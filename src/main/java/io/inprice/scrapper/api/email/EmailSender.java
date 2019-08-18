package io.inprice.scrapper.api.email;

import com.sendgrid.Method;
import com.sendgrid.Request;
import com.sendgrid.SendGrid;
import com.sendgrid.helpers.mail.Mail;
import com.sendgrid.helpers.mail.objects.Content;
import com.sendgrid.helpers.mail.objects.Email;
import io.inprice.scrapper.api.config.Properties;
import io.inprice.scrapper.api.framework.Beans;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class EmailSender {

    private static final Logger log = LoggerFactory.getLogger(EmailSender.class);

    private final Properties properties = Beans.getSingleton(Properties.class);

    public void send(String from, String subject, String to, String content) {
        Email emailFrom = new Email(from);
        Email emailTo = new Email(to);
        Content emailContent = new Content("text/html; charset=utf-8", content);

        Mail mail = new Mail(emailFrom, subject, emailTo, emailContent);

        SendGrid sg = new SendGrid(properties.getEmail_APIKey());
        Request request = new Request();
        try {
            request.setMethod(Method.POST);
            request.setEndpoint("mail/send");
            request.setBody(mail.build());
            sg.api(request);
        } catch (IOException e) {
            log.error("Failed to send email, to: {}, body: {}", from, content);
        }
    }

}
