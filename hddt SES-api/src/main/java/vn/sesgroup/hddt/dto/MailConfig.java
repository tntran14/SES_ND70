package vn.sesgroup.hddt.dto;

import java.io.Serializable;

import org.bson.Document;

public class MailConfig implements Serializable{
	private static final long serialVersionUID = 7944715769557320569L;
	
	private String UserName;
	private String smtpServer;
	private int smtpPort;
	private String emailAddress;
	private String emailPassword;
	private boolean isAutoSend;
	private boolean isSSL;
	private boolean isTLS;
	private String nameSend;
	public String getSmtpServer() {
		return smtpServer;
	}
	public void setSmtpServer(String smtpServer) {
		this.smtpServer = smtpServer;
	}
	public int getSmtpPort() {
		return smtpPort;
	}
	public void setSmtpPort(int smtpPort) {
		this.smtpPort = smtpPort;
	}
	public String getEmailAddress() {
		return emailAddress;
	}
	public void setEmailAddress(String emailAddress) {
		this.emailAddress = emailAddress;
	}
	public String getEmailPassword() {
		return emailPassword;
	}
	public void setEmailPassword(String emailPassword) {
		this.emailPassword = emailPassword;
	}
	public boolean isAutoSend() {
		return isAutoSend;
	}
	public void setAutoSend(boolean isAutoSend) {
		this.isAutoSend = isAutoSend;
	}
	public boolean isSSL() {
		return isSSL;
	}
	public void setSSL(boolean isSSL) {
		this.isSSL = isSSL;
	}
	public boolean isTLS() {
		return isTLS;
	}
	public void setTLS(boolean isTLS) {
		this.isTLS = isTLS;
	}
	public String getNameSend() {
		return nameSend;
	}
	public void setNameSend(String nameSend) {
		this.nameSend = nameSend;
	}
	
	public String getUserName() {
		return UserName;
	}
	public void setUserName(String userName) {
		UserName = userName;
	}
	public MailConfig() {
		super();
	}
	
	public MailConfig(String smtpServer, int smtpPort, String emailAddress, String emailPassword, boolean isAutoSend,
			boolean isSSL, boolean isTLS, String nameSend) {
		super();
		this.smtpServer = smtpServer;
		this.smtpPort = smtpPort;
		this.emailAddress = emailAddress;
		this.emailPassword = emailPassword;
		this.isAutoSend = isAutoSend;
		this.isSSL = isSSL;
		this.isTLS = isTLS;
		this.nameSend = nameSend;
	}
	
	
	
	public MailConfig(String userName, String smtpServer, int smtpPort, String emailAddress, String emailPassword,
			boolean isAutoSend, boolean isSSL, boolean isTLS, String nameSend) {
		super();
		UserName = userName;
		this.smtpServer = smtpServer;
		this.smtpPort = smtpPort;
		this.emailAddress = emailAddress;
		this.emailPassword = emailPassword;
		this.isAutoSend = isAutoSend;
		this.isSSL = isSSL;
		this.isTLS = isTLS;
		this.nameSend = nameSend;
	}
	
	public MailConfig(Document doc) {
		this.smtpServer = doc.get("SmtpServer", "");
		this.smtpPort = doc.getInteger("SmtpPort", 0);
		this.emailAddress = doc.get("EmailAddress", "");
		this.emailPassword = doc.get("EmailPassword", "");
		this.isAutoSend = doc.getBoolean("AutoSend", false);
		this.isSSL = doc.getBoolean("SSL", false);
		this.isTLS = doc.getBoolean("TLS", false);
		this.nameSend = "";
	}
	

	
}
