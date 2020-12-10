package io.inprice.api.scheduled;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jdbi.v3.core.Handle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.inprice.api.app.company.CompanyDao;
import io.inprice.api.app.company.mapper.CompanyIdUserEmail;
import io.inprice.api.app.subscription.SubscriptionDao;
import io.inprice.api.consts.Global;
import io.inprice.api.email.EmailSender;
import io.inprice.api.email.EmailTemplate;
import io.inprice.api.email.TemplateRenderer;
import io.inprice.api.external.Props;
import io.inprice.common.helpers.Beans;
import io.inprice.common.helpers.Database;
import io.inprice.common.meta.CompanyStatus;
import io.inprice.common.meta.SubsEvent;
import io.inprice.common.models.CompanyTrans;

/**
 * Stops SUBSCRIBED companies after four days later from their subs renewal date expired.
 * Normally, StripeService in api project will handle this properly. 
 * However, a communication problem with stripe may occur and we do not want to miss an expired company.
 * 
 * @since 2020-12-06
 * @author mdpinar
 */
public class SubscribedCompanyStopper implements Runnable {

  private static final Logger log = LoggerFactory.getLogger(SubscribedCompanyStopper.class);
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
          CompanyDao companyDao = transactional.attach(CompanyDao.class);

          List<CompanyIdUserEmail> expiredCompanyList = companyDao.findExpiredSubscribedCompanysEmailList();
          int affected = 0;

          if (expiredCompanyList != null && expiredCompanyList.size() > 0) {
            for (CompanyIdUserEmail idAndEmail: expiredCompanyList) {
              boolean isOK = companyDao.stopCompany(idAndEmail.getCompanyId(), CompanyStatus.STOPPED.name());

              CompanyTrans trans = new CompanyTrans();
              trans.setCompanyId(idAndEmail.getCompanyId());
              trans.setEvent(SubsEvent.SUBSCRIPTION_STOPPED);
              trans.setSuccessful(Boolean.TRUE);
              trans.setDescription(("Stopped due to payment problem."));

              SubscriptionDao subscriptionDao = transactional.attach(SubscriptionDao.class);
              isOK = subscriptionDao.insertTrans(trans, trans.getEvent().name());
              if (isOK) {
                isOK = companyDao.insertStatusHistory(idAndEmail.getCompanyId(), CompanyStatus.STOPPED.name());
              }

              if (isOK) {
                Map<String, Object> dataMap = new HashMap<>(1);
                dataMap.put("user", idAndEmail.getEmail());
                String message = templateRenderer.render(EmailTemplate.SUBSCRIPTION_STOPPED, dataMap);
                emailSender.send(Props.APP_EMAIL_SENDER(), "The last notification for your subscription to inprice.", idAndEmail.getEmail(), message);

                affected++;
              }
            }
          }

          if (affected > 0) {
            log.info("{} subscribed company in total stopped!", affected);
          } else {
            log.info("No subscribed company to be stopped was found!");
          }
          return (affected > 0);
        });
      }
      
    } finally {
      Global.stopTask(clazz);
    }
  }

}
