package io.inprice.scrapper.api.email;

import java.io.IOException;

import com.sendgrid.Method;
import com.sendgrid.Request;
import com.sendgrid.helpers.mail.Mail;
import com.sendgrid.helpers.mail.objects.Content;
import com.sendgrid.helpers.mail.objects.Email;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EmailSender {

   private static final Logger log = LoggerFactory.getLogger(EmailSender.class);

   public void send(String from, String subject, String to, String content) {
      Email emailFrom = new Email(from);
      Email emailTo = new Email(to);
      Content emailContent = new Content("text/html", content);

      Mail mail = new Mail(emailFrom, subject, emailTo, emailContent);

      //SendGrid sg = new SendGrid(Props.getAPIKey_Email());
      Request request = new Request();
      try {
         request.setMethod(Method.POST);
         request.setEndpoint("mail/send");
         request.setBody(mail.build());
         // sg.api(request);
         log.info("Email sent, to: {}, content: {}", emailTo.getEmail(), content);
      } catch (IOException e) {
         log.error("Failed to send email, to: {}, body: {}", from, content, e);
      }
   }

}
