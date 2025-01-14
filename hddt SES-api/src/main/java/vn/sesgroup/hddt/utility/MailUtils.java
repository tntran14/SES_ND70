package vn.sesgroup.hddt.utility;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.util.Date;
import java.util.List;
import java.util.Properties;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.activation.FileDataSource;
import javax.mail.Authenticator;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

import vn.sesgroup.hddt.dto.MailConfig;

public class MailUtils {
	public boolean sendMail(MailConfig mailConfig, 
			String title, String content, String mailReceiver, 
			List<String> files, List<String> names, boolean isHtmlFormat) {
		boolean status = false;
		
		try {
			MimeMultipart multipart = new MimeMultipart();
			Properties properties = new Properties();
			properties.put("mail.smtp.host", mailConfig.getSmtpServer());
			properties.put("mail.smtp.port", mailConfig.getSmtpPort());
			properties.put("mail.smtp.user", mailConfig.getEmailAddress());
			properties.put("mail.smtp.auth", "true");
			if (mailConfig.isTLS())
				properties.put("mail.smtp.starttls.enable", "true");
			else {
				properties.put("mail.smtp.starttls.enable", "false");
			}
			if (mailConfig.isSSL()) {
				properties.put("mail.smtp.socketFactory.port", mailConfig.getSmtpPort());
				properties.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
				properties.put("mail.smtp.ssl.enable", "true");
				properties.put("mail.smtp.socketFactory.fallback", "false");
				properties.setProperty("mail.smtp.quitwait", "false");
			}
			
			Session session = Session.getInstance(properties, new Authenticator() {
				protected PasswordAuthentication getPasswordAuthentication(){
					return new PasswordAuthentication(mailConfig.getEmailAddress(), mailConfig.getEmailPassword());
				}
			});
			
			MimeMessage msg = new MimeMessage(session);
			msg.setHeader("Content-Type", "text/html; charset=UTF-8");
			msg.setSubject(title, "utf-8");
			msg.setContent(content, "charset=UTF-8");
			/*
			 * message.setFrom(new InternetAddress("Joe Smith <joe@acme.com>"));
			 * or
			 * message.setFrom(new InternetAddress("joe@acme.com", "Joe Smith"));
			 * */
//			msg.setFrom(new InternetAddress(mailConfig.getAccount()));
			msg.setFrom(new InternetAddress(mailConfig.getEmailAddress(), mailConfig.getNameSend(), "UTF-8"));
			msg.setRecipients(Message.RecipientType.TO, mailReceiver);
			msg.setSentDate(new Date());
			
			MimeBodyPart mbp = new MimeBodyPart();
			if (isHtmlFormat)
				mbp.setContent(content, "text/html; charset=UTF-8");
			else {
				mbp.setText(content, "text/plain; charset=UTF-8");
			}
			multipart.addBodyPart(mbp);
//			msg.setContent(multipart);
			
			if(null != files) {
				DataSource source = null;
				File file = null;
				int i = 0;
				for(String fileName: files){
					file = new File(fileName);
					if(file.exists() && file.isFile()) {
						mbp = new MimeBodyPart();
						source = new FileDataSource(file);
						mbp.setDataHandler(new DataHandler(source));
						mbp.setFileName(names.get(i));
						multipart.addBodyPart(mbp);
						i++;
					}
				}
			}
			
			msg.setContent(multipart);
			Transport t = session.getTransport("smtp");
			t.connect(properties.getProperty("mail.smtp.host"), properties.getProperty("mail.smtp.auth.user"),
					properties.getProperty("mail.smtp.auth.password"));

			t.sendMessage(msg, msg.getAllRecipients());
			t.close();
			
			status = true;
		}catch(NullPointerException | MessagingException | UnsupportedEncodingException e) {
			return status;
//			e.printStackTrace();
		}
		
		
		return status;
	}
}
