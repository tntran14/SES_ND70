package vn.sesgroup.hddt.utility;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;

import com.mailjet.client.ClientOptions;
import com.mailjet.client.MailjetClient;
import com.mailjet.client.MailjetRequest;
import com.mailjet.client.MailjetResponse;
import com.mailjet.client.errors.MailjetException;
import com.mailjet.client.errors.MailjetSocketTimeoutException;
import com.mailjet.client.resource.Emailv31;

import vn.sesgroup.hddt.dto.MailConfig;
public class MailjetSender {
	
  public boolean sendMailJet(MailConfig mailConfig, 
			String title, String content, String mailReceiver, 
			List<String> files, List<String> names, boolean isHtmlFormat) throws MailjetException, MailjetSocketTimeoutException {
	
	 boolean status = false;
	MailjetClient client;
    MailjetRequest request;
    MailjetResponse response;
    String fileData = "";
    String fileData1 = "";
    int dem_name = names.size();
    String name1 = "";
    String name2 = "";
    String name1_check = names.get(0);
    //NEU FILE CHI CO 11 PHAI CHECK XEM NO LA PDF HAY LA XML
    String[] split_name = name1_check.split("\\.");
    String check = split_name[1];
    
    
    String []mail_ = mailReceiver.split(",");
	List<String> listMail = new ArrayList<>();
    for(int dem1 =0; dem1<mail_.length;dem1++) {
    	listMail.add(mail_[dem1]);
    }
    
    try {   
    //ĐEM SO LUONG FILE DAU VAO  	
    	if(dem_name==1 && check.equals("pdf")) {
       	  name1 = names.get(0);
       	  if(null != files) {
      		File file = null;
      		int i = 0;
      		for(String fileName: files){
      			file = new File(fileName);
      			if(file.exists() && file.isFile()) {   				
      				java.nio.file.Path pdfPath = java.nio.file.Paths.get(fileName);
      				    byte[] filecontent = java.nio.file.Files.readAllBytes(pdfPath);     				  
      				    	  fileData = com.mailjet.client.Base64.encode(filecontent);     				     				  
      				    i++;
      			}
      		}		
      	}
       	 
       	  //XU LY MAIL
       	  for(int i=0; i<listMail.size(); i++) {
       		  
       		 client = new MailjetClient(mailConfig.getEmailAddress(), mailConfig.getEmailPassword(), new ClientOptions("v3.1"));
             request = new MailjetRequest(Emailv31.resource)
             .property(Emailv31.MESSAGES, new JSONArray()
             .put(new JSONObject()
             .put(Emailv31.Message.FROM, new JSONObject()
            .put("Email",mailConfig.getSmtpServer())
             .put("Name", mailConfig.getNameSend()))
       
            	  .put(Emailv31.Message.TO, new JSONArray()
            			  
            		         .put(new JSONObject()
            		         .put("Email", listMail.get(i)))
            		       )                              
             .put(Emailv31.Message.SUBJECT,  title)
             .put(Emailv31.Message.HTMLPART, content)
             
             .put(Emailv31.Message.ATTACHMENTS, new JSONArray()
                   .put(new JSONObject().put("ContentType", "application/pdf")
                                    .put("Filename", name1)
                                    .put("Base64Content", fileData)))
             ));  
             response = client.post(request);
             System.out.println(response.getStatus());
             System.out.println(response.getData());
       	  }      
    	}
    	
    	else if(dem_name==1 && check.equals("xml")) {
         	  name1 = names.get(0);
         	  if(null != files) {
        		File file = null;
        		int i = 0;
        		for(String fileName: files){
        			file = new File(fileName);
        			if(file.exists() && file.isFile()) {   				
        				java.nio.file.Path pdfPath = java.nio.file.Paths.get(fileName);
        				    byte[] filecontent = java.nio.file.Files.readAllBytes(pdfPath);     				  
        				    	  fileData = com.mailjet.client.Base64.encode(filecontent);     				     				  
        				    i++;
        			}
        		}		
        	}
         	 
         	  //XU LY MAIL
         	  for(int i=0; i<listMail.size(); i++) {
         		  
         		 client = new MailjetClient(mailConfig.getEmailAddress(), mailConfig.getEmailPassword(), new ClientOptions("v3.1"));
               request = new MailjetRequest(Emailv31.resource)
               .property(Emailv31.MESSAGES, new JSONArray()
               .put(new JSONObject()
               .put(Emailv31.Message.FROM, new JSONObject()
              .put("Email",mailConfig.getSmtpServer())
               .put("Name", mailConfig.getNameSend()))
         
              	  .put(Emailv31.Message.TO, new JSONArray()
              			  
              		         .put(new JSONObject()
              		         .put("Email", listMail.get(i)))
              		       )                              
               .put(Emailv31.Message.SUBJECT,  title)
               .put(Emailv31.Message.HTMLPART, content)
               
               .put(Emailv31.Message.ATTACHMENTS, new JSONArray()
                     .put(new JSONObject().put("ContentType", "application/xml")
                                      .put("Filename", name1)
                                      .put("Base64Content", fileData)))
               ));  
               response = client.post(request);
               System.out.println(response.getStatus());
               System.out.println(response.getData());
         	  }      
      	}
    	else {
    	 name1 = names.get(0);
         name2 = names.get(1);
    	   if(null != files) {
        		File file = null;
        		int i = 0;
        		for(String fileName: files){
        			file = new File(fileName);
        			if(file.exists() && file.isFile()) {   				
        				java.nio.file.Path pdfPath = java.nio.file.Paths.get(fileName);
        				    byte[] filecontent = java.nio.file.Files.readAllBytes(pdfPath);
        				    if(fileData.equals("")) {
        				    	  fileData = com.mailjet.client.Base64.encode(filecontent);
        				    }else {
        				    	  fileData1 = com.mailjet.client.Base64.encode(filecontent);
        				    }   				  
        				    i++;
        			}
        		}		
        	}
            
    	   
    	   for(int i=0; i<listMail.size(); i++) {
    	   
    	   client = new MailjetClient(mailConfig.getEmailAddress(), mailConfig.getEmailPassword(), new ClientOptions("v3.1"));
           request = new MailjetRequest(Emailv31.resource)
           .property(Emailv31.MESSAGES, new JSONArray()
           .put(new JSONObject()
           .put(Emailv31.Message.FROM, new JSONObject()
           .put("Email",mailConfig.getSmtpServer())
           .put("Name", mailConfig.getNameSend()))
     
          	  .put(Emailv31.Message.TO, new JSONArray()
          			  
          		         .put(new JSONObject()
          		         .put("Email", listMail.get(i)))          	              		
          		       )                              
                .put(Emailv31.Message.SUBJECT,  title)
                .put(Emailv31.Message.HTMLPART, content)         
                .put(Emailv31.Message.ATTACHMENTS, new JSONArray()
                      .put(new JSONObject().put("ContentType", "application/xml")
                                       .put("Filename", name1)
                                       .put("Base64Content", fileData))   
                        .put(new JSONObject().put("ContentType", "application/pdf")
                                       .put("Filename", name2)
                                       .put("Base64Content", fileData1)))
                ));
                response = client.post(request);
                System.out.println(response.getStatus());
                System.out.println(response.getData());
       }
    	   
      }
          
       
    //END ĐEM SO LUONG FILE DAU VAO  	
    	
    status = true;
    
	}catch(Exception e) {
		  return status;
//		e.printStackTrace();
	}
    return status;
  }
}