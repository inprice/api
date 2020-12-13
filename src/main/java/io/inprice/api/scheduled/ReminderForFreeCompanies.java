package io.inprice.api.scheduled;

import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jdbi.v3.core.Handle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.inprice.api.app.company.CompanyDao;
import io.inprice.api.app.user.UserDao;
import io.inprice.api.consts.Global;
import io.inprice.api.email.EmailSender;
import io.inprice.api.email.EmailTemplate;
import io.inprice.api.email.TemplateRenderer;
import io.inprice.api.external.Props;
import io.inprice.common.helpers.Beans;
import io.inprice.common.helpers.Database;
import io.inprice.common.meta.CompanyStatus;
import io.inprice.common.models.Company;
import io.inprice.common.models.User;
import io.inprice.common.utils.DateUtils;

/**
 * Sends emails to the companies whose statuses are either FREE or COUPONED and there is less 
 * than or equal to three days to renewal date.
 * 
 * @since 2020-12-06
 * @author mdpinar
 */
public class ReminderForFreeCompanies implements Runnable {

  private static final Logger log = LoggerFactory.getLogger(ReminderForFreeCompanies.class);

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
        CompanyDao companyDao = handle.attach(CompanyDao.class);

        List<Company> aboutToExpiredCompanyList = 
          companyDao.findAboutToExpiredFreeCompanyList(
            Arrays.asList(
              CompanyStatus.FREE.name(),
              CompanyStatus.COUPONED.name()
            )
          );

        int affected = 0;

        if (aboutToExpiredCompanyList != null && aboutToExpiredCompanyList.size() > 0) {
          UserDao userDao = handle.attach(UserDao.class);

          for (Company company: aboutToExpiredCompanyList) {
            User user = userDao.findById(company.getAdminId());

            Map<String, Object> dataMap = new HashMap<>(4);
            dataMap.put("user", user.getName());
            dataMap.put("model", company.getStatus());
            dataMap.put("days", DateUtils.findDayDiff(company.getSubsRenewalAt(), new Date()));
            dataMap.put("subsRenewalAt", DateUtils.formatReverseDate(company.getSubsRenewalAt()));
            String message = templateRenderer.render(EmailTemplate.FREE_COMPANY_REMINDER, dataMap);
            emailSender.send(Props.APP_EMAIL_SENDER(), "Your subscription is about to end", user.getEmail(), message);

            affected++;
          }
        }

        if (affected > 0) {
          log.info("Reminder emails sent to {} companies which are using free or a coupon!", affected);
        } else {
          log.info("No remainder sent to free or couponed companies!");
        }
      } catch (Exception e) {
        log.error("Failed to trigger " + clazz , e);
      }
      
    } finally {
      Global.stopTask(clazz);
    }
  }

}
