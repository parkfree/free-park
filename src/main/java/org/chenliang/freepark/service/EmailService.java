package org.chenliang.freepark.service;

import java.io.UnsupportedEncodingException;
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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class EmailService {

  @Value("${mail.smtp.host}")
  private String smtpHost;

  @Value("${mail.smtp.port}")
  private String smtpPort;

  @Value("${mail.user}")
  private String user;

  @Value("${mail.password}")
  private String password;

  public void sendMail(String to) {
    String defaultSubject = "未缴费提醒";
    String defaultContent = "请通过微信尽快手工缴费";
    sendMail(to, defaultSubject, defaultContent);
  }

  public void sendMail(String sendTo, String subject, String content) {
    Properties properties = System.getProperties();
    properties.setProperty("mail.smtp.auth", "true");
    properties.setProperty("mail.smtp.host", smtpHost);
    properties.setProperty("mail.smtp.port", smtpPort);

    Authenticator authenticator = new Authenticator() {
      @Override
      protected PasswordAuthentication getPasswordAuthentication() {
        return new PasswordAuthentication(user, password);
      }
    };

    Session session = Session.getDefaultInstance(properties, authenticator);
    try {
      InternetAddress form = new InternetAddress(user);
      form.setPersonal("free parking");
      InternetAddress to = new InternetAddress(sendTo);

      MimeMessage message = new MimeMessage(session);
      message.setFrom(form);
      message.addRecipient(Message.RecipientType.TO, to);
      message.setSubject(subject);
      message.setContent("<h1>" + content + "</h1>", "text/html;charset=GB2312");
      Transport.send(message);
      log.info("Sent message successfully....");
    } catch (MessagingException | UnsupportedEncodingException ex) {
      log.error(ex.getLocalizedMessage(), ex);
    }
  }
}
