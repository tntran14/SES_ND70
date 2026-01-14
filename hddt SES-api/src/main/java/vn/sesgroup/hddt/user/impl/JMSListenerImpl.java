package vn.sesgroup.hddt.user.impl;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import org.bson.Document;
import org.bson.types.ObjectId;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import org.w3c.dom.Node;

import com.fasterxml.jackson.databind.JsonNode;
import com.mailjet.client.ClientOptions;
import com.mailjet.client.MailjetClient;
import com.mailjet.client.MailjetRequest;
import com.mailjet.client.MailjetResponse;
import com.mailjet.client.resource.Emailv31;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.FindOneAndUpdateOptions;
import com.mongodb.client.model.ReturnDocument;

import vn.sesgroup.hddt.configuration.ConfigConnectMongo;
import vn.sesgroup.hddt.dto.MailConfig;
import vn.sesgroup.hddt.user.dao.AbstractDAO;
import vn.sesgroup.hddt.user.dao.JMSListenerDAO;
import vn.sesgroup.hddt.user.service.JPUtils;
import vn.sesgroup.hddt.utility.Commons;
import vn.sesgroup.hddt.utility.Constants;
import vn.sesgroup.hddt.utility.Json;
import vn.sesgroup.hddt.utility.MailUtils;
import vn.sesgroup.hddt.utility.MailjetSender;
import vn.sesgroup.hddt.utility.SystemParams;

@Repository
@Transactional
public class JMSListenerImpl extends AbstractDAO implements JMSListenerDAO {
	
	@Autowired ConfigConnectMongo cfg;
	
	Commons commons = new Commons();
	
	@Autowired
	JPUtils jpUtils;
	
	private MailUtils mailUtils = new MailUtils();
	private MailjetSender mailJet = new MailjetSender();
	
	@Override
	public void sendMailWithQueueBulkMail(String infoServerID) throws Exception {
		List<Document> pipeline = new ArrayList<Document>();
		pipeline.add(
			new Document("$match", new Document("InfoServerID", infoServerID))
		);
		pipeline.add(
			new Document("$lookup", 
				new Document("from", "LogBulkEMail")
				.append("pipeline", 
					Arrays.asList(
						new Document("$match", new Document("InfoServerID", infoServerID))
						
					)
				)
				.append("as", "LogBulkEMail")
			)
		);
		pipeline.add(
				new Document("$lookup", 
					new Document("from", "PramLink")
					.append("pipeline", 
						Arrays.asList(
							new Document("$match", 
								new Document("$expr", 
										new Document("IsDelete", false)
								)
							)
						)	
					)
					.append("as", "PramLink")
				)
			);
			pipeline.add(
				new Document("$unwind", new Document("path", "$PramLink").append("preserveNullAndEmptyArrays", true))
			);	
			pipeline.add(
					new Document("$lookup", 
						new Document("from", "PramLink")
						.append("pipeline", 
							Arrays.asList(
								new Document("$match", 
									new Document("$expr", 
											new Document("IsDelete", false)
									)
								)
							)	
						)
						.append("as", "PramLink")
					)
				);
				pipeline.add(
					new Document("$unwind", new Document("path", "$PramLink").append("preserveNullAndEmptyArrays", true))
				);

		Document docTmp = null;
		MongoClient mongoClient = cfg.mongoClient();
		MongoCollection<Document> collection = mongoClient.getDatabase(cfg.dbName).getCollection("LogBulkEMailInfoServer");
		try {
			docTmp =   collection.aggregate(pipeline).allowDiskUse(true).iterator().next();	
		} catch (Exception e) {
			// TODO: handle exception
		}
		mongoClient.close();
		
		
		if(docTmp == null) return;
		
		String Check_Mail = docTmp.get("MailUsingType", "");
		String 	link = docTmp.getEmbedded(Arrays.asList("PramLink", "LinkPortal"), "");

		String title = "";
		String _tmp = "";
		String emailReceive = "";
		String _email="";
		String _emailcc="";
		String _title = "";
		String _content ="";
		String email_gui = "";
		 	link = docTmp.getEmbedded(Arrays.asList("PramLink", "LinkPortal"), "");
	
		
		if(Check_Mail.equals("MailJet")) {
						
		//LAY CAU HINH MAILJET	
		JSONObject fromEmail = new JSONObject()
				.put("Email", docTmp.getEmbedded(Arrays.asList("MailjetInfo", "EmailAddress"), ""))
				.put("Name", docTmp.getEmbedded(Arrays.asList("MailjetInfo", "NameSend"), ""));
		String ApiKey = docTmp.getEmbedded(Arrays.asList("MailjetInfo", "ApiKey"), "");
		String SecretKey = docTmp.getEmbedded(Arrays.asList("MailjetInfo", "SecretKey"), "");
		//END LAY CAU HINH MAILJET
		
		
		//LAY DANH SACH LogBulkEMail		
		List<Document> toEmails = docTmp.getList("LogBulkEMail", Document.class);
		
		///////////////////
		if(null == toEmails || toEmails.size() == 0) return;
		

		for(Document o: toEmails) {
			ObjectId id_log = null;
			ObjectId objectIdMail = o.get("_id", ObjectId.class);
			
			ObjectId objectId = o.getEmbedded(Arrays.asList("Data","_id"), ObjectId.class);
			String _id = objectId.toString();
			String TaxCode = o.getEmbedded(Arrays.asList("TaxCode"), "");
			String Name = o.getEmbedded(Arrays.asList("Name"), "");
			String MST_NMua = o.getEmbedded(Arrays.asList("Data","EInvoiceDetail", "NDHDon", "NMua", "MST"), "");
			String Ten_NMua = o.getEmbedded(Arrays.asList("Data","EInvoiceDetail", "NDHDon", "NMua", "Ten"), "");
			String HVTNMHang = o.getEmbedded(Arrays.asList("Data","EInvoiceDetail", "NDHDon", "NMua", "HVTNMHang"), "");		
			int SHDon = o.getEmbedded(Arrays.asList("Data","EInvoiceDetail", "TTChung", "SHDon"), 0);			
			String Email = o.getEmbedded(Arrays.asList("Data","EInvoiceDetail", "NDHDon", "NMua", "DCTDTu"), "");
			String EmailCC = o.getEmbedded(Arrays.asList("Data","EInvoiceDetail", "NDHDon", "NMua", "DCTDTuCC"), "");			
			String SecureKey = o.getEmbedded(Arrays.asList("Data","SecureKey"), "");						
			String KHMSHDon =  o.getEmbedded(Arrays.asList("Data","EInvoiceDetail", "TTChung", "KHMSHDon"), "");
			String KHHDon =  o.getEmbedded(Arrays.asList("Data","EInvoiceDetail", "TTChung", "KHHDon"), "");
			String MSHDon = KHMSHDon + KHHDon;		
			String DChi =  o.getEmbedded(Arrays.asList("Data","EInvoiceDetail", "NDHDon", "NBan", "DChi"), "");
			String IssuerId = o.get("IssuerId", "");
			
			if(Email.equals("") && EmailCC.equals("")) {
				continue;
			}
			
			
			title = TaxCode + " " + Name + " Thông báo phát hành Hóa đơn điện tử";
			
			_tmp = MST_NMua;
			if(!"".equals(_tmp))
				title += " " + _tmp;
			_tmp = Ten_NMua;
			if("".equals(_tmp))
				_tmp = HVTNMHang;
			if(!"".equals(_tmp))
				title += " " + _tmp;
			_tmp = " - Số HĐ ";
			if(SHDon != 0)
				_tmp += commons.formatNumberBillInvoice(SHDon);
			_tmp += " (No reply)";
			title += _tmp;
			
			emailReceive = Email;
			
			_tmp = Ten_NMua.toUpperCase();
			if("".equals(_tmp))
				_tmp = HVTNMHang.toUpperCase();
			
			String url =link;
			String secretCode = SecureKey;
			
			StringBuilder sb = new StringBuilder();
			sb.setLength(0);
			sb.append("<p><span style='font-family: Times New Roman;font-size: 13px;'>Kính gửi: <label style='font-weight: bold;'>" + ("".equals(_tmp)? "Quý khách hàng": _tmp) + "</label><o:p></o:p></span></p>\n");
			sb.append("<p><span style='font-family: Times New Roman;font-size: 13px;'><label style='font-weight: bold;'>" + Name + "</label> xin chân thành cảm ơn Quý đơn vị đã tin tưởng và sử dụng sản phẩm/dịch vụ của chúng tôi!</span></p>\n");
			sb.append("<p><span style='font-family: Times New Roman;font-size: 13px;'>Chúng tôi vừa phát hành hóa đơn điện tử đến Quý đơn vị với thông tin như sau:</span></p>\n");
			
			_tmp = "";
			if(SHDon != 0)
				_tmp += commons.formatNumberBillInvoice(SHDon);
			
			sb.append("<p><span style='font-family: Times New Roman;font-size: 13px;'>1.  Số hoá đơn:  " + _tmp + "</span></p>\n");
			sb.append("<p><span style='font-family: Times New Roman;font-size: 13px;'>2.  Mẫu hoá đơn: " + (MSHDon) + "</span></p>\n");
			sb.append("<p><span style='font-family: Times New Roman;font-size: 13px;'>3.  Link truy cập: <a target='_blank' href='" + url + "'>" + url + "</a></span></p>\n");
			sb.append("<p><span style='font-family: Times New Roman;font-size: 13px;'>4.  Mã bảo mật: <a target='_blank' href='" + url + "'>" + secretCode + "</a></span></p>\n");
			sb.append("<p><span style='font-family: Times New Roman;font-size: 13px;'>Quý đơn vị  vui lòng kiểm tra lại thông tin và lưu trữ hoá đơn.</span></p>\n");
			sb.append("<p><span style='font-family: Times New Roman;font-size: 13px;'>Trân trọng kính chào!</span></p>");

			sb.append("<hr style='margin: 5px 0 5px 0;'>");
			sb.append("<p style='margin-bottom: 3px;'><span style='font-family: Times New Roman;font-size: 13px;color:red;font-weight: bold;'>QUÝ CÔNG TY VUI LÒNG KHÔNG REPLY EMAIL NÀY!</span></p>");
			sb.append("<p style='margin-bottom: 0px;'><span style='font-family: Times New Roman;font-size: 13px;'><label style='font-weight: bold;'>" + Name.toUpperCase() + "</label><o:p></o:p></span></p>");
			sb.append("<p><span style='font-family: Times New Roman;font-size: 13px;'>" + DChi + "</span></p>\n");
			
			
			_email=emailReceive = Email;
			_emailcc=emailReceive = EmailCC;
			_title = title;
			_content = sb.toString();
	         
			String CheckFooterMail = o.getEmbedded(Arrays.asList("Data","UserConFig", "footermail"), "");
			if(!CheckFooterMail.equals("Y")) {
				_content = commons.decodeURIComponent(_content);
				String noidung = o.getEmbedded(Arrays.asList("Data","DMFooterWeb", "Noidung"), "");
				_content += noidung;
			}
			else {
				_content = commons.decodeURIComponent(_content);
			}
			String mauHD = o.getEmbedded(Arrays.asList("Data","EInvoiceDetail", "TTChung", "KHMSHDon"), "")
					+ o.getEmbedded(Arrays.asList("Data","EInvoiceDetail", "TTChung", "KHHDon"), "");
			String soHD = commons
					.formatNumberBillInvoice(o.getEmbedded(Arrays.asList("Data","EInvoiceDetail", "TTChung", "SHDon"), 0));

			boolean isDieuChinh = "2".equals(o.getEmbedded(Arrays.asList("Data","HDSS", "TCTBao"), ""));
			boolean isThayThe = false;
			String check_status = docTmp.get("EInvoiceStatus", "");
			if(check_status.equals("REPLACED"))	{
				isThayThe = "3".equals(docTmp.getEmbedded(Arrays.asList("HDSS", "TCTBao"), ""));
			}
			
			String CheckView = o.getEmbedded(Arrays.asList("Data","UserConFig", "viewshd"), "");
			String readMoneyInEnglish = o.getEmbedded(Arrays.asList("Data", "UserConFig", "ReadMoneyInEnglish"), "");	
			String dir = o.getEmbedded(Arrays.asList("Data","Dir"), "");
			String signStatusCode = o.getEmbedded(Arrays.asList("Data","SignStatusCode"), "");
			String eInvoiceStatus = o.getEmbedded(Arrays.asList("Data","EInvoiceStatus"), "");
			String MCCQT = o.getEmbedded(Arrays.asList("Data","MCCQT"), "");
			String secureKey = o.getEmbedded(Arrays.asList("Data","SecureKey"), "");
			String fileName = _id + ".xml";
			File file = null;

			
			int SoHDon = docTmp.getEmbedded(Arrays.asList("EInvoiceDetail", "TTChung", "SHDon"), 0);
			
			
			String fileNameXML = _id + "_" + MCCQT + ".xml";
			String fileNamePDF = _id + ".pdf";
			if (Constants.INVOICE_STATUS.DELETED.equals(eInvoiceStatus))
				fileNamePDF = _id + "-deleted.pdf";
			/* KIEM TRA XEM CO FILE PDF CHUA; NEU CHUA CO THI TAO FILE PDF */
			if (o.getEmbedded(Arrays.asList("Data","DMMauSoKyHieu"), "") != null) {
				String MST = o.getEmbedded(Arrays.asList("Data","EInvoiceDetail", "NDHDon", "NBan", "MST"), "");
				String ImgLogo = o.getEmbedded(Arrays.asList("Data","DMMauSoKyHieu", "Templates", "ImgLogo"), "");
				String ImgBackground = o.getEmbedded(Arrays.asList("Data","DMMauSoKyHieu", "Templates", "ImgBackground"), "");
				String ImgQA = o.getEmbedded(Arrays.asList("Data","DMMauSoKyHieu", "Templates", "ImgQA"), "");
				String ImgVien = o.getEmbedded(Arrays.asList("Data","DMMauSoKyHieu", "Templates", "ImgVien"), "");
				String ParamUSD = o.getEmbedded(Arrays.asList("EInvoiceDetail", "TToan", "ParamUSD"), "");

				fileName = _id + ".xml";
				if ("SIGNED".equals(signStatusCode)  && !"".equals(MCCQT)) {
					fileName = _id + "_" + MCCQT + ".xml";
					String fileName_ = _id + "_" + MCCQT + "_" + SoHDon + ".xml";
					/*	CHECK MCCQT GET DATA XML */
						
						File file_xml = new File(dir, fileName);
						
						org.w3c.dom.Document doc_xml = commons.fileToDocument(file_xml);
						
						XPath xPath_xml = XPathFactory.newInstance().newXPath();
						Node nodeHDon = null;
						for(int J = 1; J<= 20; J++) {
							nodeHDon = (Node) xPath_xml.evaluate("/KetQuaTraCuu/DuLieu/TDiep[" + J + "]/DLieu/HDon", doc_xml, XPathConstants.NODE);
							if(nodeHDon != null) break;
						}
						/*FILE CHUA DUOC DUI DEN CO QUAN THUE*/
						if(null == nodeHDon) {
							nodeHDon = (Node) xPath_xml.evaluate("/HDon", doc_xml, XPathConstants.NODE);
						}							
						
						DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
						DocumentBuilder builder = factory.newDocumentBuilder();
						org.w3c.dom.Document rTCTN_HDON = builder.newDocument();
						rTCTN_HDON.appendChild(rTCTN_HDON.importNode(nodeHDon, true));
						/* END CHECK GET DATA IN RESULT IN FILE XML TO TAX */
						boolean boo_ = false;
						boo_ = commons.docW3cToFile(rTCTN_HDON, dir, fileName_);
						
						if(boo_ == true) {
							fileName = fileName_;
							fileNameXML = fileName_;
						}
					/* END CHECK MCCQT GET DATA XML */
					
				} else {
					if ("SIGNED".equals(signStatusCode)) {
						fileName = _id + "_signed.xml";
					}
				}
				file = new File(dir, fileName);
				if (file.exists() && file.isFile()) {
					org.w3c.dom.Document doc = commons.fileToDocument(file);
					String fileNameJP = o.getEmbedded(Arrays.asList("Data","DMMauSoKyHieu", "Templates", "FileName"), "");
					int numberRowInPage = o.getEmbedded(Arrays.asList("Data","DMMauSoKyHieu", "Templates", "RowsInPage"), 20);
					int numberRowInPageMultiPage = o
							.getEmbedded(Arrays.asList("Data","DMMauSoKyHieu", "Templates", "RowInPageMultiPage"), 26);
					int numberCharsInRow = o.getEmbedded(Arrays.asList("Data","DMMauSoKyHieu", "Templates", "CharsInRow"),
							50);

					File fileJP = new File(SystemParams.DIR_E_INVOICE_TEMPLATE, fileNameJP);

					ByteArrayOutputStream baosPDF = null;

					baosPDF = jpUtils.createFinalInvoice(fileJP, doc, secureKey, CheckView, readMoneyInEnglish, numberRowInPage, numberRowInPageMultiPage,
							numberCharsInRow, MST,link, ParamUSD,
							Paths.get(SystemParams.DIR_E_INVOICE_TEMPLATE, "images", MST, ImgLogo).toString(),
							Paths.get(SystemParams.DIR_E_INVOICE_TEMPLATE, "images", MST, ImgBackground).toString(),
							Paths.get(SystemParams.DIR_E_INVOICE_TEMPLATE, "images", MST, ImgQA ).toString(),
							Paths.get(SystemParams.DIR_E_INVOICE_TEMPLATE, "images", MST, ImgVien ).toString(),
							false,Constants.INVOICE_STATUS.DELETED.equals(eInvoiceStatus), isThayThe, isDieuChinh, false);
					/* LUU TAP TIN PDF */
					if (null != baosPDF) {
						try (OutputStream fileOuputStream = new FileOutputStream(new File(dir, fileNamePDF))) {
							baosPDF.writeTo(fileOuputStream);
						} catch (IOException e) {
							continue;
						}
					}
				}
			}
			/* END - KIEM TRA XEM CO FILE PDF CHUA; NEU CHUA CO THI TAO FILE PDF */

			file = null;
			List<String> listFiles = new ArrayList<>();
			List<String> listNames = new ArrayList<>();
			file = new File(dir, fileNameXML);
			if (file.exists() && file.isFile()) {
				listFiles.add(file.toString());
				listNames.add(mauHD + "-" + soHD + ".xml");
			}
			file = new File(dir, fileNamePDF);
			if (file.exists() && file.isFile()) {
				listFiles.add(file.toString());
				listNames.add(mauHD + "-" + soHD + ".pdf");
			}
			
			/* THUC HIEN GUI MAIL */
			boolean boo = false;
				
				if(!_email.equals("")) {
					
					email_gui = _email; 	
				}
				if( !_emailcc.equals("")) {
					email_gui += "," + _emailcc;	
				}	
			
				
				
			//List<String> arrayName = o.get("EmailListName", List.class);
			String name1 = listNames.get(0);
	        String name2 = listNames.get(1);
	        String fileData = "";
	        String fileData1 = "";

	       // List<String> arrayFiles = o.get("EmailListFile", List.class);
	    	   if(null != listFiles) {
	        		File file1 = null;
	        		int i = 0;
	        		for(String fileName1: listFiles){
	        			file1 = new File(fileName1);
	        			if(file1.exists() && file1.isFile()) {   				
	        				java.nio.file.Path path = java.nio.file.Paths.get(fileName1);
	        				    byte[] filecontent = java.nio.file.Files.readAllBytes(path);
	        				    if (i == 0)
									fileData = com.mailjet.client.Base64.encode(filecontent); // XML 1
								if (i == 1)
									fileData1 = com.mailjet.client.Base64.encode(filecontent); // PDF		  
	        				    i++;
	        			}
	        		}		
	        	}
			
	    	   String[] arrayMail = email_gui.split(","); 
	    	   
	    	   MailjetClient client;
	    	   MailjetRequest request;
	    	   MailjetResponse response;
				
	    	   for(String mail: arrayMail) {
	    			String status = "";
	    		//  if(commons.checkStringWithRegex(Constants.REGEX_CHECK.STRING_IS_EMAIL, mail)) {							
				 client = new MailjetClient(ApiKey, SecretKey, new ClientOptions("v3.1"));
		           request = new MailjetRequest(Emailv31.resource)
		           .property(Emailv31.MESSAGES, new JSONArray()
		           .put(new JSONObject()
		           .put(Emailv31.Message.FROM, fromEmail)		     
		          	  .put(Emailv31.Message.TO, new JSONArray()		          			  
		          		         .put(new JSONObject()
		          		         .put("Email", mail))    	              		
		          		       )                              
		                .put(Emailv31.Message.SUBJECT,  _title)
		                .put(Emailv31.Message.HTMLPART, _content)         
		                .put(Emailv31.Message.ATTACHMENTS, new JSONArray()
		                      .put(new JSONObject().put("ContentType", "application/xml")
		                                       .put("Filename", name1)
		                                       .put("Base64Content", fileData))   
		                        .put(new JSONObject().put("ContentType", "application/pdf")
		                                       .put("Filename", name2)
		                                       .put("Base64Content", fileData1))
		                        )
		                ));
		             response = client.post(request);
		             // GHI LOG KET QUA GUI MAIL
		        	/*CAP NHAT MESSAGE ID THEO MAIL THEO infoServerID + ID*/
		   			JsonNode jsonNode = null;
		   		
		   			try {
		   				jsonNode = Json.serializer().nodeFromJson(response.getData().toString());
		   				for(JsonNode h: jsonNode) {
		   					status = h.get("Status").asText();
		   				}		   						  
		   			}catch(Exception ex) {}
		   	
		   			
		   	//GHI LOG 
		   	Document findLogBulkEMail = new Document("InfoServerID", infoServerID)
		   			.append("_id", objectIdMail); 		
		   			
			FindOneAndUpdateOptions options = null;
			options = new FindOneAndUpdateOptions();
			options.upsert(true);
			options.maxTime(5000, TimeUnit.MILLISECONDS);
			options.returnDocument(ReturnDocument.AFTER);
			
			
			
	
			 mongoClient = cfg.mongoClient();
			 collection = mongoClient.getDatabase(cfg.dbName).getCollection("LogBulkEMail");
			collection.findOneAndUpdate(findLogBulkEMail,
							new Document("$set",
									new Document("Status", status)),							
																
							options);		
			mongoClient.close();

			
						
			/////END KIỂM TRA GỬI MAIL
	    		  
	    		  if(status.equals("success")) {
	    			  id_log = new ObjectId();
		    			try {						
							Document LogEmail = new Document("_id", id_log)
									.append("IssuerId", IssuerId)
									.append("Title", _title)
									.append("Email", email_gui)
									.append("IsActive", true)
									.append("MailCheck", true)
									.append("IsDelete", false)
									.append("EmailContent", _content);

							//CONNECT WITH MONGODB
							
							
						
							
							 mongoClient = cfg.mongoClient();
							collection = mongoClient.getDatabase(cfg.dbName).getCollection("LogEmailUser");
							collection.insertOne(LogEmail);			
							mongoClient.close();
																		
						} catch (Exception ex) {
						}  
		    			
		    			System.out.println("Gui mail tu nha cung cap den "+ email_gui + " THANH CONG");
	    		  }else {
	    			  id_log = new ObjectId();
		    			try {						
							Document LogEmail = new Document("_id", id_log)
									.append("IssuerId", IssuerId)
									.append("Title", _title)
									.append("Email", email_gui)
									.append("IsActive", false)
									.append("MailCheck", false)
									.append("IsDelete", false)
									.append("EmailContent", _content);

							//CONNECT WITH MONGODB
							MongoClient mongoClient2 = cfg.mongoClient();
							collection = mongoClient2.getDatabase(cfg.dbName).getCollection("LogEmailUser");
							collection.insertOne(LogEmail);	
							mongoClient2.close();
																		
						} catch (Exception ex) {
						}    
		    			System.out.println("Gui mail tu nha cung cap den "+ email_gui + " THAT BAI");
	    		  }
	    			
			
			
		
			
					
	   //END FOR MAIL 	   
	  }    		
	}		

		}else {
			 MailConfig mailConfig = null;
			//XU LY BANG CAU HINH MAIL THUONG 
			String SmtpServer = docTmp.getEmbedded(Arrays.asList("MailServer", "SmtpServer"), "");
			int Port = docTmp.getEmbedded(Arrays.asList("MailServer", "Port"), 0);
			String EmailAddress = docTmp.getEmbedded(Arrays.asList("MailServer", "EmailAddress"), "");
			String PassWord = docTmp.getEmbedded(Arrays.asList("MailServer", "PassWord"), "");
			String NameSend = docTmp.getEmbedded(Arrays.asList("MailServer", "NameSend"), "");
			boolean IsAutoSend = docTmp.getEmbedded(Arrays.asList("MailServer", "IsAutoSend"), false);
			boolean IsSSL = docTmp.getEmbedded(Arrays.asList("MailServer", "IsSSL"), false);
			boolean IsTLS = docTmp.getEmbedded(Arrays.asList("MailServer", "IsTLS"), false);

			//
			//LAY DANH SACH LogBulkEMail		
			List<Document> toEmails = docTmp.getList("LogBulkEMail", Document.class);
			
			///////////////////
			if(null == toEmails || toEmails.size() == 0) return;
			
			List<Document> docEmails = new ArrayList<Document>();
			for(Document o: toEmails) {
				ObjectId id_log = null;			
				ObjectId objectIdMail = o.get("_id", ObjectId.class);
				ObjectId objectId = o.getEmbedded(Arrays.asList("Data","_id"), ObjectId.class);
				String _id = objectId.toString();
				String TaxCode = o.getEmbedded(Arrays.asList("TaxCode"), "");
				String Name = o.getEmbedded(Arrays.asList("Name"), "");
				String MST_NMua = o.getEmbedded(Arrays.asList("Data","EInvoiceDetail", "NDHDon", "NMua", "MST"), "");
				String Ten_NMua = o.getEmbedded(Arrays.asList("Data","EInvoiceDetail", "NDHDon", "NMua", "Ten"), "");
				String HVTNMHang = o.getEmbedded(Arrays.asList("Data","EInvoiceDetail", "NDHDon", "NMua", "HVTNMHang"), "");		
				int SHDon = o.getEmbedded(Arrays.asList("Data","EInvoiceDetail", "TTChung", "SHDon"), 0);			
				String Email = o.getEmbedded(Arrays.asList("Data","EInvoiceDetail", "NDHDon", "NMua", "DCTDTu"), "");
				String EmailCC = o.getEmbedded(Arrays.asList("Data","EInvoiceDetail", "NDHDon", "NMua", "DCTDTuCC"), "");			
				String SecureKey = o.getEmbedded(Arrays.asList("Data","SecureKey"), "");						
				String KHMSHDon =  o.getEmbedded(Arrays.asList("Data","EInvoiceDetail", "TTChung", "KHMSHDon"), "");
				String KHHDon =  o.getEmbedded(Arrays.asList("Data","EInvoiceDetail", "TTChung", "KHHDon"), "");
				String MSHDon = KHMSHDon + KHHDon;		
				String DChi =  o.getEmbedded(Arrays.asList("Data","EInvoiceDetail", "NDHDon", "NBan", "DChi"), "");
				String IssuerId = o.get("IssuerId", "");
				
				if(Email.equals("") && EmailCC.equals("")) {
					continue;
				}
				
				
				title = TaxCode + " " + Name + " Thông báo phát hành Hóa đơn điện tử";
				
				_tmp = MST_NMua;
				if(!"".equals(_tmp))
					title += " " + _tmp;
				_tmp = Ten_NMua;
				if("".equals(_tmp))
					_tmp = HVTNMHang;
				if(!"".equals(_tmp))
					title += " " + _tmp;
				_tmp = " - Số HĐ ";
				if(SHDon != 0)				
				_tmp += commons.formatNumberBillInvoice(SHDon);
				_tmp += " (No reply)";
				title += _tmp;
				
				emailReceive = Email;
				
				_tmp = Ten_NMua.toUpperCase();
				if("".equals(_tmp))
					_tmp = HVTNMHang.toUpperCase();
				
				String url =link;
				String secretCode = SecureKey;
				
				StringBuilder sb = new StringBuilder();
				sb.setLength(0);
				sb.append("<p><span style='font-family: Times New Roman;font-size: 13px;'>Kính gửi: <label style='font-weight: bold;'>" + ("".equals(_tmp)? "Quý khách hàng": _tmp) + "</label><o:p></o:p></span></p>\n");
				sb.append("<p><span style='font-family: Times New Roman;font-size: 13px;'><label style='font-weight: bold;'>" + Name + "</label> xin chân thành cảm ơn Quý đơn vị đã tin tưởng và sử dụng sản phẩm/dịch vụ của chúng tôi!</span></p>\n");
				sb.append("<p><span style='font-family: Times New Roman;font-size: 13px;'>Chúng tôi vừa phát hành hóa đơn điện tử đến Quý đơn vị với thông tin như sau:</span></p>\n");
				
				_tmp = "";
				if(SHDon != 0)
					_tmp += "000000"+ SHDon;
				
				sb.append("<p><span style='font-family: Times New Roman;font-size: 13px;'>1.  Số hoá đơn:  " + _tmp + "</span></p>\n");
				sb.append("<p><span style='font-family: Times New Roman;font-size: 13px;'>2.  Mẫu hoá đơn: " + (MSHDon) + "</span></p>\n");
				sb.append("<p><span style='font-family: Times New Roman;font-size: 13px;'>3.  Link truy cập: <a target='_blank' href='" + url + "'>" + url + "</a></span></p>\n");
				sb.append("<p><span style='font-family: Times New Roman;font-size: 13px;'>4.  Mã bảo mật: <a target='_blank' href='" + url + "'>" + secretCode + "</a></span></p>\n");
				sb.append("<p><span style='font-family: Times New Roman;font-size: 13px;'>Quý đơn vị  vui lòng kiểm tra lại thông tin và lưu trữ hoá đơn.</span></p>\n");
				sb.append("<p><span style='font-family: Times New Roman;font-size: 13px;'>Trân trọng kính chào!</span></p>");

				sb.append("<hr style='margin: 5px 0 5px 0;'>");
				sb.append("<p style='margin-bottom: 3px;'><span style='font-family: Times New Roman;font-size: 13px;color:red;font-weight: bold;'>QUÝ CÔNG TY VUI LÒNG KHÔNG REPLY EMAIL NÀY!</span></p>");
				sb.append("<p style='margin-bottom: 0px;'><span style='font-family: Times New Roman;font-size: 13px;'><label style='font-weight: bold;'>" + Name.toUpperCase() + "</label><o:p></o:p></span></p>");
				sb.append("<p><span style='font-family: Times New Roman;font-size: 13px;'>" + DChi + "</span></p>\n");
				
				
				_email=emailReceive = Email;
				_emailcc=emailReceive = EmailCC;
				_title = title;
				_content = sb.toString();
		         
				String CheckFooterMail = o.getEmbedded(Arrays.asList("Data","UserConFig", "footermail"), "");
				if(!CheckFooterMail.equals("Y")) {
					_content = commons.decodeURIComponent(_content);
					String noidung = o.getEmbedded(Arrays.asList("Data","DMFooterWeb", "Noidung"), "");
					_content += noidung;
				}
				else {
					_content = commons.decodeURIComponent(_content);
				}
				String mauHD = o.getEmbedded(Arrays.asList("Data","EInvoiceDetail", "TTChung", "KHMSHDon"), "")
						+ o.getEmbedded(Arrays.asList("Data","EInvoiceDetail", "TTChung", "KHHDon"), "");
				String soHD = commons
						.formatNumberBillInvoice(o.getEmbedded(Arrays.asList("Data","EInvoiceDetail", "TTChung", "SHDon"), 0));

				boolean isDieuChinh = "2".equals(o.getEmbedded(Arrays.asList("Data","HDSS", "TCTBao"), ""));
				boolean isThayThe = false;
				String check_status = docTmp.get("EInvoiceStatus", "");
				if(check_status.equals("REPLACED"))	{
					isThayThe = "3".equals(docTmp.getEmbedded(Arrays.asList("HDSS", "TCTBao"), ""));
				}
				
				String CheckView = o.getEmbedded(Arrays.asList("Data","UserConFig", "viewshd"), "");
				String readMoneyInEnglish = o.getEmbedded(Arrays.asList("Data", "UserConFig", "ReadMoneyInEnglish"), "");	
				String dir = o.getEmbedded(Arrays.asList("Data","Dir"), "");
				String signStatusCode = o.getEmbedded(Arrays.asList("Data","SignStatusCode"), "");
				String eInvoiceStatus = o.getEmbedded(Arrays.asList("Data","EInvoiceStatus"), "");
				String MCCQT = o.getEmbedded(Arrays.asList("Data","MCCQT"), "");
				String secureKey = o.getEmbedded(Arrays.asList("Data","SecureKey"), "");
				String fileName = _id + ".xml";
				File file = null;

				String fileNameXML = _id + "_" + MCCQT + ".xml";
				String fileNamePDF = _id + ".pdf";
				if (Constants.INVOICE_STATUS.DELETED.equals(eInvoiceStatus))
					fileNamePDF = _id + "-deleted.pdf";
				/* KIEM TRA XEM CO FILE PDF CHUA; NEU CHUA CO THI TAO FILE PDF */
				if (o.getEmbedded(Arrays.asList("Data","DMMauSoKyHieu"), "") != null) {
					String MST = o.getEmbedded(Arrays.asList("Data","EInvoiceDetail", "NDHDon", "NBan", "MST"), "");
					String ImgLogo = o.getEmbedded(Arrays.asList("Data","DMMauSoKyHieu", "Templates", "ImgLogo"), "");
					String ImgBackground = o.getEmbedded(Arrays.asList("Data","DMMauSoKyHieu", "Templates", "ImgBackground"), "");
					String ImgQA = o.getEmbedded(Arrays.asList("Data","DMMauSoKyHieu", "Templates", "ImgQA"), "");
					String ImgVien = o.getEmbedded(Arrays.asList("Data","DMMauSoKyHieu", "Templates", "ImgVien"), "");
					String ParamUSD = o.getEmbedded(Arrays.asList("EInvoiceDetail", "TToan", "ParamUSD"), "");

					fileName = _id + ".xml";
					if ("SIGNED".equals(signStatusCode)  && !"".equals(MCCQT)) {
						fileName = _id + "_" + MCCQT + ".xml";
					} else {
						if ("SIGNED".equals(signStatusCode)) {
							fileName = _id + "_signed.xml";
						}
					}
					file = new File(dir, fileName);
					if (file.exists() && file.isFile()) {
						org.w3c.dom.Document doc = commons.fileToDocument(file);
						String fileNameJP = o.getEmbedded(Arrays.asList("Data","DMMauSoKyHieu", "Templates", "FileName"), "");
						int numberRowInPage = o.getEmbedded(Arrays.asList("Data","DMMauSoKyHieu", "Templates", "RowsInPage"), 20);
						int numberRowInPageMultiPage = o
								.getEmbedded(Arrays.asList("Data","DMMauSoKyHieu", "Templates", "RowInPageMultiPage"), 26);
						int numberCharsInRow = o.getEmbedded(Arrays.asList("Data","DMMauSoKyHieu", "Templates", "CharsInRow"),
								50);

						File fileJP = new File(SystemParams.DIR_E_INVOICE_TEMPLATE, fileNameJP);

						ByteArrayOutputStream baosPDF = null;

						baosPDF = jpUtils.createFinalInvoice(fileJP, doc, secureKey, CheckView, readMoneyInEnglish, numberRowInPage, numberRowInPageMultiPage,
								numberCharsInRow, MST,link, ParamUSD,
								Paths.get(SystemParams.DIR_E_INVOICE_TEMPLATE, "images", MST, ImgLogo).toString(),
								Paths.get(SystemParams.DIR_E_INVOICE_TEMPLATE, "images", MST, ImgBackground).toString(),
								Paths.get(SystemParams.DIR_E_INVOICE_TEMPLATE, "images", MST, ImgQA ).toString(),
								Paths.get(SystemParams.DIR_E_INVOICE_TEMPLATE, "images", MST, ImgVien ).toString(),
								false,Constants.INVOICE_STATUS.DELETED.equals(eInvoiceStatus), isThayThe, isDieuChinh, false);
						/* LUU TAP TIN PDF */
						if (null != baosPDF) {
							try (OutputStream fileOuputStream = new FileOutputStream(new File(dir, fileNamePDF))) {
								baosPDF.writeTo(fileOuputStream);
							} catch (IOException e) {
								continue;
							}
						}
					}
				}
				/* END - KIEM TRA XEM CO FILE PDF CHUA; NEU CHUA CO THI TAO FILE PDF */

				file = null;
				List<String> listFiles = new ArrayList<>();
				List<String> listNames = new ArrayList<>();
				file = new File(dir, fileNameXML);
				if (file.exists() && file.isFile()) {
					listFiles.add(file.toString());
					listNames.add(mauHD + "-" + soHD + ".xml");
				}
				file = new File(dir, fileNamePDF);
				if (file.exists() && file.isFile()) {
					listFiles.add(file.toString());
					listNames.add(mauHD + "-" + soHD + ".pdf");
				}
				/* THUC HIEN GUI MAIL */
				boolean boo = false;
				
					if(!_email.equals("")) {
						
						email_gui = _email; 	
					}
					if( !_emailcc.equals("")) {
						email_gui += "," + _emailcc;	
					}	
				
		    	   //GUI MAIL
					mailConfig = new MailConfig();
					
					 mailConfig.setEmailAddress(EmailAddress);
					 mailConfig.setEmailPassword(PassWord);
					 mailConfig.setSmtpPort(Port);
					 mailConfig.setSmtpServer(SmtpServer);
					 mailConfig.setNameSend(NameSend);
					 mailConfig.setAutoSend(IsAutoSend);
					 mailConfig.setSSL(IsSSL);
					 mailConfig.setTLS(IsTLS);
				
					boo = mailUtils.sendMail(mailConfig, _title, _content, email_gui, listFiles, listNames, true);
						
					
				
				   	
					
					id_log = new ObjectId();													
						Document LogEmail = new Document("_id", id_log)
								.append("IssuerId", IssuerId)
								.append("Title", _title)
								.append("Email", email_gui)
								.append("IsActive", boo)
								.append("MailCheck", boo)
								.append("IsDelete", false)
								.append("EmailContent", _content);
						
						
						//CONNECT WITH MONGODB
						
						 mongoClient = cfg.mongoClient();
						collection = mongoClient.getDatabase(cfg.dbName).getCollection("LogEmailUser");
						collection.insertOne(LogEmail);	
						mongoClient.close();
						

						if(boo == true) {
							//GHI LOG 
						   	Document findLogBulkEMail = new Document("InfoServerID", infoServerID)
						   			.append("_id", objectIdMail); 		
						   			
							FindOneAndUpdateOptions options = null;
							options = new FindOneAndUpdateOptions();
							options.upsert(true);
							options.maxTime(5000, TimeUnit.MILLISECONDS);
							options.returnDocument(ReturnDocument.AFTER);
							
							
							 mongoClient = cfg.mongoClient();
							 collection = mongoClient.getDatabase(cfg.dbName).getCollection("LogBulkEMail");
							collection.findOneAndUpdate(findLogBulkEMail,
											new Document("$set",
													new Document("Status", "success")),							
																				
											options);
							mongoClient.close();
							System.out.println("Gui Email Server den " +email_gui + " THANH CONG");
						}else {
							
							//GHI LOG 
						   	Document findLogBulkEMail = new Document("InfoServerID", infoServerID)
						   			.append("_id", objectIdMail); 		
						   			
							FindOneAndUpdateOptions options = null;
							options = new FindOneAndUpdateOptions();
							options.upsert(true);
							options.maxTime(5000, TimeUnit.MILLISECONDS);
							options.returnDocument(ReturnDocument.AFTER);
							
							
							MongoClient  mongoClient2 = cfg.mongoClient();
								collection = mongoClient2.getDatabase(cfg.dbName).getCollection("LogBulkEMail");;
							collection.findOneAndUpdate(findLogBulkEMail,
											new Document("$set",
													new Document("Status", "error")),							
																				
											options);
							mongoClient2.close();
							System.out.println("Gui Email Server den " +email_gui +" THAT BAI");
						}	
			}	
					
		}
		
	}




	@Override
	public void sendMailWithQueueBulkMailOnCttncn(String infoServerID) throws Exception {
		List<Document> pipeline = new ArrayList<Document>();
		pipeline.add(new Document("$match", new Document("InfoServerID", infoServerID)));
		pipeline.add(
				new Document("$lookup",
						new Document("from", "LogBulkEMail")
								.append("pipeline",
										Arrays.asList(
												new Document("$match", new Document("InfoServerID", infoServerID))))
								.append("as", "LogBulkEMail")));
		pipeline.add(
				new Document("$lookup",
						new Document("from", "PramLink")
								.append("pipeline",
										Arrays.asList(new Document("$match",
												new Document("$expr", new Document("IsDelete", false)))))
								.append("as", "PramLink")));
		pipeline.add(
				new Document("$unwind", new Document("path", "$PramLink").append("preserveNullAndEmptyArrays", true)));

		Document docTmp = null;
		try (MongoClient mongoClient = cfg.mongoClient()){
			MongoCollection<Document> collection = mongoClient.getDatabase(cfg.dbName).getCollection("LogBulkEMailInfoServer");

			docTmp = collection.aggregate(pipeline).allowDiskUse(true).iterator().next();	
		} catch (Exception e) {
			// TODO: handle exception
		}		
		
		if(docTmp == null) return;
		
		String mailType = docTmp.get("MailUsingType", "");
		String link = docTmp.getEmbedded(Arrays.asList("PramLink", "LinkPortal"), "");

		StringBuilder title = null;
		String tmp = "";
		String email="";
		String emailcc="";
		StringBuilder content = null;
		String emailReceives = "";
		File file = null;
		
		// LAY DANH SACH LogBulkEMail
		List<Document> toEmails = docTmp.getList("LogBulkEMail", Document.class);
		if (null == toEmails || toEmails.size() == 0)
			return;

		for (Document toEmail : toEmails) {
			title = new StringBuilder();
			tmp = "";
			email="";
			emailcc="";
			content = new StringBuilder();
			emailReceives = "";
			
			// set title
			title.append(toEmail.get("TaxCode", ""));
			title.append(" ");
			title.append(toEmail.get("Name", ""));
			title.append(" Thông báo phát hành Chứng từ khấu trừ thuế");
			tmp = toEmail.getEmbedded(Arrays.asList("Data","NNT", "MST"), "");
			if (!"".equals(tmp)) {
				title.append(" ");
				title.append(tmp);
			}
			tmp = toEmail.getEmbedded(Arrays.asList("Data","NNT", "Ten"), "");
			if (!"".equals(tmp)) {
				title.append(" ");
				title.append(tmp);
			}
			
			tmp = String.valueOf(toEmail.getEmbedded(Arrays.asList("Data","SCTu"), ""));
			if (!"".equals(tmp)) {
				title.append(" - Số Chứng từ ");
				title.append(tmp);
				title.append(" (No reply)");
			}
			//end set title
			
			// set content
			content.setLength(0);
			tmp = toEmail.getEmbedded(Arrays.asList("Data", "NNT", "Ten"), "");
			content.append("<p><span style='font-family: Times New Roman;font-size: 13px;'>Kính gửi: <label>"
					+ ("".equals(tmp) ? "Nhân viên" : tmp) + "</label><o:p></o:p></span></p>\n");
			content.append("<p><span style='font-family: Times New Roman;font-size: 13px;'><label>"
					+ toEmail.get("Name", "") + "</label> xin gửi " + ("".equals(tmp) ? "Nhân viên" : tmp)
					+ " chứng từ khấu trừ thuế TNCN theo file đính kèm.</span></p>\n");
			content.append("<p><span style='font-family: Times New Roman;font-size: 13px;'>Trân trọng!</span></p>");
			content.append("<hr style='margin: 5px 0 5px 0;'>");
			content.append(
					"<p style='margin-bottom: 3px;'><span style='font-family: Times New Roman;font-size: 13px;color:red;font-weight: bold;'>NHÂN VIÊN VUI LÒNG KHÔNG REPLY EMAIL NÀY!</span></p>");
			content.append(
					"<p style='margin-bottom: 0px;'><span style='font-family: Times New Roman;font-size: 13px;'><label style='font-weight: bold;'>"
							+ toEmail.get("Name", "").toUpperCase() + "</label><o:p></o:p></span></p>");
			content.append("<p><span style='font-family: Times New Roman;font-size: 13px;'>"
					+ toEmail.getEmbedded(Arrays.asList("Data", "TCTTNhap", "DChi"), "") + "</span></p>\n");

			String checkFooterMail = docTmp.getEmbedded(Arrays.asList("Data", "UserConFig", "footermail"), "");
			if (!checkFooterMail.equals("Y")) {
				String noidung = docTmp.getEmbedded(Arrays.asList("Data", "DMFooterWeb", "Noidung"), "");
				content.append(noidung);
			} 
			// end set content
			
			// set email
			email=toEmail.getEmbedded(Arrays.asList("Data", "NNT", "DCTDTu"), "");
			if (email.trim().isEmpty()) break;
			
			emailReceives= email;
			emailcc=toEmail.getEmbedded(Arrays.asList("Data", "NNT", "DCTDTuCC"), "");
			if (!emailcc.trim().isEmpty()) {
				emailReceives+=","+emailcc;
			}
			// end set email
			
			// check contained pdf
			String id = toEmail.getEmbedded(Arrays.asList("Data", "_id"), ObjectId.class).toString();
			String fileName = id + ".xml";
			String fileNamePDF = id + ".pdf";
			
			String statusCode = toEmail.getEmbedded(Arrays.asList("Data", "SignStatus"), "");
			String dir = toEmail.getEmbedded(Arrays.asList("Data","Dir"), "");
			if ("SIGNED".equals(statusCode)) {
				fileName = id + "_signed.xml";
			}
			
			file = new File(dir, fileName);
			if (file.exists() && file.isFile()) {
				String imgLogo = toEmail.getEmbedded(Arrays.asList("Data", "DMMSTNCN","LoGo"), "");
				String kh = toEmail.getEmbedded(Arrays.asList("Data","KHCTu"), "");
				String ms = toEmail.getEmbedded(Arrays.asList("Data", "DMMSTNCN", "MauSo"), "");

				org.w3c.dom.Document doc = commons.fileToDocument(file);
				
				String fileNameJP = toEmail.getEmbedded(Arrays.asList("Data", "DMMSTNCN", "FileName"), "");
				File fileJP = new File(SystemParams.DIR_E_INVOICE_TEMPLATE, fileNameJP);
				ByteArrayOutputStream baosPDF = null;
				baosPDF = jpUtils.viewpdfcttncnV1(
						fileJP, 
						doc, 
						toEmail.get("Data", Document.class),
						Paths.get(SystemParams.DIR_E_INVOICE_TEMPLATE, "MauSoTNCN", toEmail.get("TaxCode", ""), imgLogo).toString(),
						kh, 
						ms,
						link, 
						false);

				if (null != baosPDF) {
					try (OutputStream fileOuputStream = new FileOutputStream(new File(dir, fileNamePDF))) {
						baosPDF.writeTo(fileOuputStream);
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}
			// end check contained pdf
			
			String khctu = toEmail.getEmbedded(Arrays.asList("Data", "KHCTu"), "");
			String sctu = String.valueOf(toEmail.getEmbedded(Arrays.asList("Data","SCTu"), ""));
			List<String> listFiles = new ArrayList<>();
			List<String> listNames = new ArrayList<>();
			
			file = new File(dir, fileName);
			if (file.exists() && file.isFile()) {
				listFiles.add(file.toString());
				listNames.add(khctu + "-" + sctu + ".xml");
			}
			
			file = new File(dir, fileNamePDF);
			if (file.exists() && file.isFile()) {
				listFiles.add(file.toString());
				listNames.add(khctu + "-" + sctu + ".pdf");
			}
			
			// send mail
			boolean boo = false;
			MailConfig mailConfig = new MailConfig(toEmail.getEmbedded(Arrays.asList("Data","ConfigEmail"), Document.class));
			if (mailType.equals("MailJet")) {
				mailConfig.setEmailAddress(toEmail.getEmbedded(Arrays.asList("Data","ConfigMailJet","ApiKey"), ""));
				mailConfig.setEmailPassword(toEmail.getEmbedded(Arrays.asList("Data","ConfigMailJet","SecretKey"), ""));
				mailConfig.setSmtpServer(toEmail.getEmbedded(Arrays.asList("Data","ConfigMailJet","EmailAddress"), ""));
				boo = mailJet.sendMailJet(mailConfig, title.toString(), content.toString(), emailReceives, listFiles, listNames, true);
			} else {
				boo = mailUtils.sendMail(mailConfig, title.toString(), content.toString(), emailReceives, listFiles, listNames, true);
			}
			
			FindOneAndUpdateOptions options = null;
			options = new FindOneAndUpdateOptions();
			options.upsert(true);
			options.maxTime(5000, TimeUnit.MILLISECONDS);
			options.returnDocument(ReturnDocument.AFTER);
					
			try (MongoClient mongoClient = cfg.mongoClient()) {
				MongoCollection<Document> collection = null;
				if(boo == false) {
					collection = mongoClient.getDatabase(cfg.dbName).getCollection("LogBulkEMail");
					collection.findOneAndUpdate(new Document("InfoServerID", infoServerID)
				   			.append("_id", toEmail.get("_id", ObjectId.class)),
							new Document("$set",
									new Document("Status", "error")),						
							options);
				} else {
					Document docInsert = null;
					collection = mongoClient.getDatabase(cfg.dbName).getCollection("LogEmailUser");
					docInsert = new Document("IssuerId", toEmail.get("IssuerId",""))
							.append("Title", title.toString())
							.append("Email", emailReceives)
							.append("IsActive", boo)
							.append("MailCheck", boo)
							.append("IsDelete", false)
							.append("EmailContent", content.toString());
					collection.insertOne(docInsert);

					/* LOG BAO CAO THONG KE */
					collection = mongoClient.getDatabase(cfg.dbName).getCollection("BaoCaoThongKeCTTNCN");
					docInsert = new Document("IssuerId", toEmail.get("IssuerId",""))
							.append("Name", toEmail.getEmbedded(Arrays.asList("Data", "NNT", "Ten"), ""))
							.append("Code", toEmail.getEmbedded(Arrays.asList("Data", "NNT", "Code"), ""))
							.append("Address", toEmail.getEmbedded(Arrays.asList("Data", "NNT", "DChi"), ""))
							.append("ContactPhone", toEmail.getEmbedded(Arrays.asList("Data", "NNT", "SDThoai"), ""))
							.append("TaxCode", toEmail.getEmbedded(Arrays.asList("Data", "NNT", "MST"), ""))
							.append("KyHieu", toEmail.getEmbedded(Arrays.asList("Data", "KHCTu"), ""))
							.append("CCCD", toEmail.getEmbedded(Arrays.asList("Data", "NNT", "CCCDan"), ""))
							.append("KyBaoCao", toEmail.getEmbedded(Arrays.asList("Data", "TTNCNKTru", "Nam"), ""))
							.append("TThang", toEmail.getEmbedded(Arrays.asList("Data", "TTNCNKTru", "TThang"), ""))
							.append("DThang", toEmail.getEmbedded(Arrays.asList("Data", "TTNCNKTru", "DThang"), ""))
							.append("SCTu", toEmail.getEmbedded(Arrays.asList("Data","SCTu"), 0))
							.append("EmailGuiCTTNCN", emailReceives)
							.append("IsDelete", false);
					collection.insertOne(docInsert);
					
					collection = mongoClient.getDatabase(cfg.dbName).getCollection("LogBulkEMail");
					collection.findOneAndUpdate(new Document("InfoServerID", infoServerID)
				   			.append("_id", toEmail.get("_id", ObjectId.class)),
							new Document("$set",
									new Document("Status", "success")),				
							options);
				}
			} catch (Exception e) {
				
			}
		}
	}
}
