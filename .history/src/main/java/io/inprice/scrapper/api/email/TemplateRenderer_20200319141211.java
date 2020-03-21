package io.inprice.scrapper.api.email;

import freemarker.template.*;

import java.io.IOException;
import java.io.StringWriter;
import java.util.Locale;
import java.util.Map;

public class TemplateRenderer {

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

   public String renderForgotPassword(Map<String, Object> data) throws IOException, TemplateException {
      return render("forgot-password.html", data);
   }

   public String renderInvitationForNewUsers(Map<String, Object> data) throws IOException, TemplateException {
      return render("invitation-for-new-users.html", data);
   }

   public String renderInvitationForExistingUser(Map<String, Object> data) throws IOException, TemplateException {
      return render("invitation-for-existing-users.html", data);
   }

   private String render(String name, Map<String, Object> data) throws IOException, TemplateException {
      Template template = cfg.getTemplate(name);
      StringWriter sw = new StringWriter();
      template.process(data, sw);
      return sw.toString();
   }

}
