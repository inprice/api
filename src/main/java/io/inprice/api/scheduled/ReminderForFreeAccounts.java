package io.inprice.api.scheduled;

import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jdbi.v3.core.Handle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.inprice.api.app.account.AccountDao;
import io.inprice.api.app.user.UserDao;
import io.inprice.api.consts.Global;
import io.inprice.api.email.EmailSender;
import io.inprice.api.email.EmailTemplate;
import io.inprice.api.email.TemplateRenderer;
import io.inprice.api.external.Props;
import io.inprice.common.helpers.Beans;
import io.inprice.common.helpers.Database;
import io.inprice.common.meta.AccountStatus;
import io.inprice.common.models.Account;
import io.inprice.common.models.User;
import io.inprice.common.utils.DateUtils;

/**
 * Sends emails to the accounts whose statuses are either FREE or COUPONED and there is less 
 * than or equal to three days to renewal date.
 * 
 * @since 2020-12-06
 * @author mdpinar
 */
public class ReminderForFreeAccounts implements Runnable {

  private static final Logger log = LoggerFactory.getLogger(ReminderForFreeAccounts.class);

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
        AccountDao accountDao = handle.attach(AccountDao.class);

        List<Account> aboutToExpiredAccountList = 
          accountDao.findAboutToExpiredFreeAccountList(
            Arrays.asList(
              AccountStatus.FREE.name(),
              AccountStatus.COUPONED.name()
            )
          );

        int affected = 0;

        if (aboutToExpiredAccountList != null && aboutToExpiredAccountList.size() > 0) {
          UserDao userDao = handle.attach(UserDao.class);

          for (Account account: aboutToExpiredAccountList) {
            User user = userDao.findById(account.getAdminId());

            Map<String, Object> dataMap = new HashMap<>(4);
            dataMap.put("user", user.getName());
            dataMap.put("model", account.getStatus());
            dataMap.put("days", DateUtils.findDayDiff(account.getSubsRenewalAt(), new Date()));
            dataMap.put("subsRenewalAt", DateUtils.formatReverseDate(account.getSubsRenewalAt()));
            String message = templateRenderer.render(EmailTemplate.FREE_ACCOUNT_REMINDER, dataMap);
            emailSender.send(Props.APP_EMAIL_SENDER, "Your subscription is about to end", user.getEmail(), message);

            affected++;
          }
        }

        if (affected > 0) {
          log.info("Reminder emails sent to {} accounts which are using free or a coupon!", affected);
        } else {
          log.info("No remainder sent to free or couponed accounts!");
        }
      } catch (Exception e) {
        log.error("Failed to trigger " + clazz , e);
      }
      
    } finally {
      Global.stopTask(clazz);
    }
  }

}
