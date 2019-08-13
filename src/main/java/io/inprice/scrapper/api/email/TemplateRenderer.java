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
        Template template = cfg.getTemplate("forgot-password.html");
        StringWriter sw = new StringWriter();
        template.process(data, sw);
        return sw.toString();
    }

}
