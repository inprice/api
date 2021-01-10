package io.inprice.api.email;

import java.io.StringReader;
import java.io.StringWriter;
import java.util.Locale;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import freemarker.template.Configuration;
import freemarker.template.DefaultObjectWrapper;
import freemarker.template.Template;
import freemarker.template.TemplateExceptionHandler;
import io.inprice.api.utils.CssInliner;

public class TemplateRenderer {

  private static final Logger log = LoggerFactory.getLogger(TemplateRenderer.class);

  private final Configuration cfg;

  private TemplateRenderer() {
    cfg = new Configuration(Configuration.VERSION_2_3_23);
    cfg.setObjectWrapper(new DefaultObjectWrapper(Configuration.VERSION_2_3_23));

    // Some other recommended settings:
    cfg.setDefaultEncoding("UTF-8");
    cfg.setLocale(Locale.US);
    cfg.setLogTemplateExceptions(false);
    cfg.setTemplateExceptionHandler(TemplateExceptionHandler.RETHROW_HANDLER);
  }

  public String render(EmailTemplate emailTemplate, Map<String, Object> data) {
    return render(emailTemplate.name(), CssInliner.inlinedEmailTemplate(emailTemplate), data);
  }

  private String render(String name, String content, Map<String, Object> data) {
    try {
      Template template = new Template(name, new StringReader(content), cfg);
      StringWriter sw = new StringWriter();
      template.process(data, sw);
      return sw.toString();
    } catch (Exception e) {
      log.error("Failed to render " + name, e);
    }
    return null;
  }

}
