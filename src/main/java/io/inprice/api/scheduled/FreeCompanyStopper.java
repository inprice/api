package io.inprice.api.scheduled;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.jdbi.v3.core.Handle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.inprice.api.app.company.CompanyDao;
import io.inprice.api.app.subscription.SubscriptionDao;
import io.inprice.api.app.user.UserDao;
import io.inprice.api.consts.Global;
import io.inprice.api.email.EmailSender;
import io.inprice.api.email.EmailTemplate;
import io.inprice.api.email.TemplateRenderer;
import io.inprice.api.external.Props;
import io.inprice.common.helpers.Beans;
import io.inprice.common.helpers.Database;
import io.inprice.common.meta.CompanyStatus;
import io.inprice.common.meta.SubsEvent;
import io.inprice.common.models.Company;
import io.inprice.common.models.CompanyTrans;
import io.inprice.common.models.User;

/**
 * Stops companies whose statuses are either FREE or COUPONED and subs renewal date expired.
 * Please note that stopping a regular subscriber is subject to another stopper see #SubscribedCompanyStopper
 * 
 * @since 2020-10-25
 * @author mdpinar
 */
public class FreeCompanyStopper implements Runnable {

  private static final Logger log = LoggerFactory.getLogger(FreeCompanyStopper.class);
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

          List<Company> expiredCompanyList = 
            companyDao.findExpiredFreeCompanyList(
              Arrays.asList(
                CompanyStatus.FREE.name(),
                CompanyStatus.COUPONED.name()
              )
            );

          int affected = 0;

          if (expiredCompanyList != null && expiredCompanyList.size() > 0) {
            UserDao userDao = transactional.attach(UserDao.class);
            SubscriptionDao subscriptionDao = transactional.attach(SubscriptionDao.class);

            for (Company company: expiredCompanyList) {
              boolean isOK = subscriptionDao.terminate(company.getId(), CompanyStatus.STOPPED.name());
              if (isOK) {

                CompanyTrans trans = new CompanyTrans();
                trans.setCompanyId(company.getId());
                trans.setSuccessful(Boolean.TRUE);
                trans.setDescription(("End of period!"));

                if (CompanyStatus.FREE.equals(company.getStatus()))
                  trans.setEvent(SubsEvent.FREE_USE_STOPPED);
                else
                  trans.setEvent(SubsEvent.COUPON_USE_STOPPED);
      
                isOK = subscriptionDao.insertTrans(trans, trans.getEvent().getEventDesc());
                if (isOK) {
                  isOK = companyDao.insertStatusHistory(company.getId(), CompanyStatus.STOPPED.name());
                }
              }

              if (isOK) {
                User user = userDao.findById(company.getId());

                String companyName = StringUtils.isNotBlank(company.getTitle()) ? company.getTitle() : company.getName();

                Map<String, Object> dataMap = new HashMap<>(3);
                dataMap.put("user", user.getEmail());
                dataMap.put("company", companyName);
                String message = templateRenderer.render(EmailTemplate.FREE_COMPANY_CANCELLED, dataMap);
                emailSender.send(Props.APP_EMAIL_SENDER(), "inprice subscription is cancelled.", user.getEmail(), message);

                affected++;
              }
            }
          }

          if (affected > 0) {
            log.info("{} free company in total stopped!", affected);
          } else {
            log.info("No free company to be stopped was found!");
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
