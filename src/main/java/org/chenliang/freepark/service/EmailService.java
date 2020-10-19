package org.chenliang.freepark.service;

import java.util.Properties;
import javax.mail.Authenticator;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class EmailService {

  public void sendMail(String to) {
    String defaultMessage = "请缴费";
    sendMail(to, defaultMessage);
  }

  public void sendMail(String sendTo, String messageContent) {
    Properties properties = System.getProperties();
    properties.setProperty("mail.smtp.auth", "true");
    properties.setProperty("mail.smtp.host", "smtp.qq.com");
    properties.setProperty("mail.smtp.port", "587");
    properties.setProperty("mail.user", "532213748@qq.com");
    properties.setProperty("mail.password", "zbkinnaeggwvbhah");

    Authenticator authenticator = new Authenticator() {
      @Override
      protected PasswordAuthentication getPasswordAuthentication() {
        String userName = properties.getProperty("mail.user");
        String password = properties.getProperty("mail.password");
        return new PasswordAuthentication(userName, password);
      }
    };

    Session session = Session.getDefaultInstance(properties, authenticator);
    try {
      InternetAddress form = new InternetAddress(properties.getProperty("mail.user"));
      InternetAddress to = new InternetAddress(sendTo);

      MimeMessage message = new MimeMessage(session);
      message.setFrom(form);
      message.addRecipient(Message.RecipientType.TO, to);
      message.setSubject("This is the Subject Line!");
      message.setContent("<h1>" + messageContent + "</h1>", "text/html");
      Transport.send(message);
      log.info("Sent message successfully....");
    } catch (MessagingException ex) {
      log.error(ex.getLocalizedMessage(), ex);
    }
  }
}
