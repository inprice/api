package io.inprice.scrapper.api.email;

import java.io.StringWriter;
import java.util.Locale;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateExceptionHandler;
import freemarker.template.Version;

public class TemplateRenderer {

   private static final Logger log = LoggerFactory.getLogger(TemplateRenderer.class);

   private final Configuration cfg;

   private TemplateRenderer() {
      cfg = new Configuration(new Version(2, 3, 23));
      cfg.setClassForTemplateLoading(getClass(), "/templates");

      // Some other recommended settings:
      cfg.setDefaultEncoding("UTF-8");
      cfg.setLocale(Locale.US);
      cfg.setLogTemplateExceptions(false);
      cfg.setTemplateExceptionHandler(TemplateExceptionHandler.RETHROW_HANDLER);
   }

   public String renderForgotPassword(Map<String, Object> data) {
      return render("forgot-password.html", data);
   }

   public String renderInvitationForNewUsers(Map<String, Object> data) {
      return render("invitation-for-new-users.html", data);
   }

   public String renderInvitationForExistingUsers(Map<String, Object> data) {
      return render("invitation-for-existing-users.html", data);
   }

   public String renderRegisterActivationLink(Map<String, Object> data) {
      return render("register-activation-link.html", data);
   }

   private String render(String name, Map<String, Object> data) {
      try {
         Template template = cfg.getTemplate(name);
         StringWriter sw = new StringWriter();
         template.process(data, sw);
         return sw.toString();
      } catch (Exception e) {
         log.error("Failed to render " + name, e);
      }
      return null;
   }

}
