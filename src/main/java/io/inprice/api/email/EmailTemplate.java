package io.inprice.api.email;

/**
 * Refers to email template files under resource/templates folder.
 * 
 * @since 2020-12-06
 * @author mdpinar
 */
public enum EmailTemplate {
  
  FORGOT_PASSWORD("forgot-password"),
  REGISTER_ACTIVATION_LINK("register-activation-link"),
  INVITATION_FOR_NEW_USERS("invitation-for-new-users"),
  INVITATION_FOR_EXISTING_USERS("invitation-for-existing-users"),

  PAYMENT_FAILED_FIRST_TIME("payment-failed-first-time"),
  PAYMENT_FAILED_HAS_MORE_DAYS("payment-failed-has-more-days"),
  PAYMENT_FAILED_LAST_TIME("payment-failed-last-time"),

  SUBSCRIPTION_STARTED("subscription-started"),
  SUBSCRIPTION_RENEWAL("subscription-renewal"),
  SUBSCRIPTION_CANCELLED("subscription-cancelled"),
  SUBSCRIPTION_STOPPED("subscription-stopped"),

  REMINDER_FOR_FREE_COMPANIES("reminder-for-free-companies");

  private String fileName;

  private EmailTemplate(String fileName) {
    this.fileName = fileName + ".html";
  }

  public String getFileName() {
    return fileName;
  }

}
