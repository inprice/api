package io.inprice.api.scheduled;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.jdbi.v3.core.Handle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.inprice.api.app.account.AccountDao;
import io.inprice.api.app.subscription.SubscriptionDao;
import io.inprice.api.app.user.UserDao;
import io.inprice.api.consts.Global;
import io.inprice.api.email.EmailSender;
import io.inprice.api.email.EmailTemplate;
import io.inprice.api.email.TemplateRenderer;
import io.inprice.api.external.Props;
import io.inprice.common.helpers.Beans;
import io.inprice.common.helpers.Database;
import io.inprice.common.meta.AccountStatus;
import io.inprice.common.meta.SubsEvent;
import io.inprice.common.models.Account;
import io.inprice.common.models.AccountTrans;
import io.inprice.common.models.User;

/**
 * Stops accounts whose statuses are either FREE or COUPONED and subs renewal date expired.
 * Please note that stopping a regular subscriber is subject to another stopper see #SubscribedAccountStopper
 * 
 * @since 2020-10-25
 * @author mdpinar
 */
public class FreeAccountStopper implements Runnable {

  private static final Logger log = LoggerFactory.getLogger(FreeAccountStopper.class);
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
      	handle.begin();

        AccountDao accountDao = handle.attach(AccountDao.class);
        List<Account> expiredAccountList = 
          accountDao.findExpiredFreeAccountList(
            Arrays.asList(
              AccountStatus.FREE.name(),
              AccountStatus.COUPONED.name()
            )
          );

        int affected = 0;

        if (expiredAccountList != null && expiredAccountList.size() > 0) {
          UserDao userDao = handle.attach(UserDao.class);
          SubscriptionDao subscriptionDao = handle.attach(SubscriptionDao.class);

          for (Account account: expiredAccountList) {
            boolean isOK = subscriptionDao.terminate(account.getId(), AccountStatus.STOPPED.name());
            if (isOK) {

              AccountTrans trans = new AccountTrans();
              trans.setAccountId(account.getId());
              trans.setSuccessful(Boolean.TRUE);
              trans.setDescription(("End of period!"));

              if (AccountStatus.FREE.equals(account.getStatus()))
                trans.setEvent(SubsEvent.FREE_USE_STOPPED);
              else
                trans.setEvent(SubsEvent.COUPON_USE_STOPPED);
    
              isOK = subscriptionDao.insertTrans(trans, trans.getEvent().getEventDesc());
              if (isOK) {
                isOK = accountDao.insertStatusHistory(account.getId(), AccountStatus.STOPPED.name());
              }
            }

            if (isOK) {
              User user = userDao.findById(account.getId());

              String accountName = StringUtils.isNotBlank(account.getTitle()) ? account.getTitle() : account.getName();

              Map<String, Object> dataMap = new HashMap<>(3);
              dataMap.put("user", user.getEmail());
              dataMap.put("account", accountName);
              String message = templateRenderer.render(EmailTemplate.FREE_ACCOUNT_STOPPED, dataMap);
              emailSender.send(Props.APP_EMAIL_SENDER(), "Your inprice subscription is stopped.", user.getEmail(), message);

              affected++;
            }
          }
        }

        if (affected > 0) {
          log.info("{} free account in total stopped!", affected);
        } else {
          log.info("No free account to be stopped was found!");
        }
        
        if (affected > 0)
        	handle.commit();
        else
        	handle.rollback();

      } catch (Exception e) {
        log.error("Failed to trigger " + clazz , e);
      }

    } finally {
      Global.stopTask(clazz);
    }
  }

}
