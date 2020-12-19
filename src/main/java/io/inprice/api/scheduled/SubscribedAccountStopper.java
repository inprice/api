package io.inprice.api.scheduled;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.stripe.model.Subscription;

import org.jdbi.v3.core.Handle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.inprice.api.app.account.AccountDao;
import io.inprice.api.app.account.mapper.AccountInfo;
import io.inprice.api.app.subscription.SubscriptionDao;
import io.inprice.api.consts.Global;
import io.inprice.api.email.EmailSender;
import io.inprice.api.email.EmailTemplate;
import io.inprice.api.email.TemplateRenderer;
import io.inprice.api.external.Props;
import io.inprice.common.helpers.Beans;
import io.inprice.common.helpers.Database;
import io.inprice.common.meta.AccountStatus;
import io.inprice.common.meta.SubsEvent;
import io.inprice.common.models.AccountTrans;

/**
 * Stops SUBSCRIBED accounts after four days later from their subs renewal date expired.
 * Normally, StripeService in api project will handle this properly. 
 * However, a communication problem with stripe may occur and we do not want to miss an expired account.
 * 
 * @since 2020-12-06
 * @author mdpinar
 */
public class SubscribedAccountStopper implements Runnable {

  private static final Logger log = LoggerFactory.getLogger(SubscribedAccountStopper.class);
  private final String clazz = getClass().getSimpleName();

  private final EmailSender emailSender = Beans.getSingleton(EmailSender.class);
  private final TemplateRenderer templateRenderer = Beans.getSingleton(TemplateRenderer.class);

  @Override
  public void run() {
    if (Global.isTaskRunning(clazz)) {
      log.warn(clazz + " is already triggered!");
      return;
    }

    try {
      Global.startTask(clazz);

      log.info(clazz + " is triggered.");
      try (Handle handle = Database.getHandle()) {
        handle.inTransaction(transactional -> {
          AccountDao accountDao = transactional.attach(AccountDao.class);

          List<AccountInfo> expiredAccountList = accountDao.findExpiredSubscriberAccountList();
          int affected = 0;

          if (expiredAccountList != null && expiredAccountList.size() > 0) {
            for (AccountInfo cinfo: expiredAccountList) {

              //we need to cancel stripe first
              try {
                Subscription subscription = Subscription.retrieve(cinfo.getCustId());
                Subscription subsResult = subscription.cancel();
                if (subsResult != null && subsResult.getStatus().equals("canceled")) {
                  log.info("Stopping subscription: {} stopped!", cinfo.getName());
                } else if (subsResult != null) {
                  log.warn("Stopping subscription: Unexpected subs status: {}", subsResult.getStatus());
                } else {
                  log.error("Stopping subscription: subsResult is null!");
                }
              } catch (Exception e) {
                log.error("Stopping subscription: failed " + cinfo.getName(), e);
              }

              SubscriptionDao subscriptionDao = transactional.attach(SubscriptionDao.class);

              //then account can be cancellable
              boolean isOK = subscriptionDao.terminate(cinfo.getId(), AccountStatus.STOPPED.name());

              AccountTrans trans = new AccountTrans();
              trans.setAccountId(cinfo.getId());
              trans.setEvent(SubsEvent.SUBSCRIPTION_STOPPED);
              trans.setSuccessful(Boolean.TRUE);
              trans.setDescription(("Stopped! Final payment failed."));

              isOK = subscriptionDao.insertTrans(trans, trans.getEvent().getEventDesc());
              if (isOK) {
                isOK = accountDao.insertStatusHistory(cinfo.getId(), AccountStatus.STOPPED.name());
              }

              if (isOK) {
                Map<String, Object> dataMap = new HashMap<>(1);
                dataMap.put("user", cinfo.getEmail());
                String message = templateRenderer.render(EmailTemplate.SUBSCRIPTION_STOPPED, dataMap);
                emailSender.send(Props.APP_EMAIL_SENDER(), "The last notification for your subscription to inprice.", cinfo.getEmail(), message);

                affected++;
              }
            }
          }

          if (affected > 0) {
            log.info("{} subscribed account in total stopped!", affected);
          } else {
            log.info("No subscribed account to be stopped was found!");
          }
          return (affected > 0);
        });
      } catch (Exception e) {
        log.error("Failed to trigger " + clazz , e);
      }
      
    } finally {
      Global.stopTask(clazz);
    }
  }

}
