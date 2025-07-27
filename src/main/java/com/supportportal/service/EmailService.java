package com.supportportal.service;

import java.util.Properties;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import org.springframework.stereotype.Service;

import jakarta.websocket.Session;

import static com.supportportal.constant.EmailConstant.*;

@Service
public class EmailService {

	private Message createEmail(String firstName,String password, String email) throws MessagingException {
		Message message = new MimeMessage(getEmailSession());
		message.setFrom(new InternetAddress(FROM_EMAIL));
		message.setRecipients(Message.RecipientType.TO, null);
		return null;
		
	}
	
	private Session getEmailSession() {
		Properties properties = System.getProperties();
		properties.put(SMTP_HOST, GMAIL_SMTP_SERVER);
		properties.put(SMTP_AUTH, true);
		properties.put(SMTP_PORT, DEFAULT_PORT);
		properties.put(SMTP_STARTTLS_ENABLE, true);
		properties.put(SMTP_STARTTLS_REQUIRED, true);
		
		return Session.getInstance(properties,null);
	}
}
 