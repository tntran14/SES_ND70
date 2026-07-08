package vn.sesgroup.hddt.configuration;

import javax.jms.JMSException;
import javax.jms.Message;

import org.apache.activemq.command.ActiveMQTextMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.jms.annotation.JmsListeners;
import org.springframework.stereotype.Component;

import vn.sesgroup.hddt.user.dao.JMSListenerDAO;

@Component
public class JMSListener
{
    @Autowired
    JMSListenerDAO dao;
    
    @JmsListeners({ @JmsListener(concurrency = "1-1", destination = "${jms.activemq.borker.queue.bulk.mail}", containerFactory = "queue") })
    public void receiveMessageOnQueue(final Message jmsMessage) throws JMSException {
        try {
            Thread.sleep(3000L);
        }
        catch (Exception ex) {}
        if (jmsMessage instanceof ActiveMQTextMessage) {
            final String infoServerID = ((ActiveMQTextMessage)jmsMessage).getText();
            String type = ((ActiveMQTextMessage)jmsMessage).getStringProperty("TYPE");
            if (type == null) type = "";
            try {
            	
            	switch (type) {
            	case "BBDCTT":
	            	System.out.println("Dang thuc hien gui ActiveMQ trên BBDCTT");
	            	String collection = ((ActiveMQTextMessage)jmsMessage).getStringProperty("COLLECTION");
	            	String issuerId = ((ActiveMQTextMessage)jmsMessage).getStringProperty("ISSUERID");
	            	String userId = ((ActiveMQTextMessage)jmsMessage).getStringProperty("USERID");
					this.dao.createBBDCTT(infoServerID, collection, issuerId, userId);
					break;
				case "CTTNCN":
					System.out.println("Dang thuc hien gui ActiveMQ trên CTTNCN");
					this.dao.sendMailWithQueueBulkMailOnCttncn(infoServerID);
					break;
				default:
					System.out.println("Dang thuc hien gui ActiveMQ");
					this.dao.sendMailWithQueueBulkMail(infoServerID);
					break;
				}
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }
        try {
            jmsMessage.clearBody();
        }
        catch (Exception ex2) {}
    }
}