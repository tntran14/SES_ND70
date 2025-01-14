package vn.sesgroup.hddt.user.impl;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoField;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.api.message.JSONRoot;
import com.api.message.Msg;
import com.api.message.MsgHeader;
import com.api.message.MsgPage;
import com.api.message.MsgRsp;
import com.api.message.MspResponseStatus;
import com.fasterxml.jackson.databind.JsonNode;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.FindOneAndUpdateOptions;
import com.mongodb.client.model.ReturnDocument;

import vn.sesgroup.hddt.configuration.ConfigConnectMongo;
import vn.sesgroup.hddt.dto.MailConfig;
import vn.sesgroup.hddt.user.dao.AbstractDAO;
import vn.sesgroup.hddt.user.dao.IssuDao;
import vn.sesgroup.hddt.user.service.TCTNService;
import vn.sesgroup.hddt.utility.Constants;
import vn.sesgroup.hddt.utility.Json;
import vn.sesgroup.hddt.utility.MailUtils;

@Repository
@Transactional
public class IssuImpl extends AbstractDAO implements IssuDao{
	private static final Logger log = LogManager.getLogger(IssuImpl.class);
//	@Autowired MongoTemplate mongoTemplate;
	private MailUtils mailUtils = new MailUtils();
	@Autowired TCTNService tctnService;
	@Autowired ConfigConnectMongo cfg;
	
	@Value("${spring.data.mongodb.application.name}")
	private String applicationName;
	
	@Value("${spring.data.mongodb.database}")
	private String dbName;

	@Value("${spring.data.mongodb.host}")
	private String dbHost;

	@Value("${spring.data.mongodb.port}")
	private int dbPort;
	
	@Value("${spring.data.mongodb.username}")
	private String userName;
	
	@Value("${spring.data.mongodb.password}")
	private String password;

	@Override
	public MsgRsp list(JSONRoot jsonRoot) throws Exception {
		Msg msg = jsonRoot.getMsg();
		MsgHeader header = msg.getMsgHeader();
		MsgPage page = msg.getMsgPage();
		Object objData = msg.getObjData();
	
		String t = "";
		String n = "";
		String a = "";
		String p = "";
		String acti = "";
		
		JsonNode jsonData = null;
		if(objData != null) {
			jsonData = Json.serializer().nodeFromObject(objData);
			t = commons.getTextJsonNode(jsonData.at("/TaxCode")).replaceAll("\\s", "");
			n = commons.getTextJsonNode(jsonData.at("/Name")).replaceAll("\\s", " ");
			a = commons.getTextJsonNode(jsonData.at("/Address")).replaceAll("\\s", "");
			p = commons.getTextJsonNode(jsonData.at("/Phone")).replaceAll("\\s", "");
			acti = commons.getTextJsonNode(jsonData.at("/IsActive")).replaceAll("\\s", "");
		}
		int abcs = commons.stringToInteger(acti);
		Boolean isacti = true;
		if(abcs == 1) {
			isacti = true;
		}
		if(abcs == 2) {
			isacti = false;
		}
		MsgRsp rsp = new MsgRsp(header);
		MspResponseStatus responseStatus = null;
		
		ObjectId objectId = null;
		Document docTmp = null;

		List<Document> pipeline = new ArrayList<Document>();
		
		Document docMatch = new Document("IsDelete",new Document("$ne", true));	
		   //2. Lấy list khách hàng
//		String userName = header.getUserName();	
//		String [] split = userName.split("_");
//		
//		String UserFullName =  header.getUserName();
//		if(split.length>1) {
//			UserFullName = split[0];
//		}
//		
//		docMatch.append("$or", Arrays.asList(		
//				new Document("InfoCreated.CreateUserName", UserFullName),
//				new Document("InfoCreated", null)				
//				));
		
		if(!"".equals(t))
			docMatch.append("TaxCode", commons.regexEscapeForMongoQuery(t));
		if(!"".equals(n))
		docMatch.append("Name",
				new Document("$regex", commons.regexEscapeForMongoQuery(n)).append("$options", "i"));
		if(!"".equals(a))
			docMatch.append("Address", commons.regexEscapeForMongoQuery(a));
		if(!"".equals(p))
			docMatch.append("Phone", commons.regexEscapeForMongoQuery(p));
		if(!"".equals(acti))
			docMatch.append("IsActive",isacti );
		
		
		pipeline = new ArrayList<Document>();
		pipeline.add(new Document("$match", docMatch));
		pipeline.add(
				new Document("$sort", 
					new Document("_id", -1)
				)
			);
		
		pipeline.add(
				new Document("$project", 
					new Document("_id", 1).append("TaxCode", 1).append("Name", 1).append("Address", 1).append("Phone", 1)
					.append("IsActive", 1).append("InfoCreated", 1)
				)
			);
		
		pipeline.addAll(createFacetForSearchNotSort(page));
		
		MongoClient mongoClient = cfg.mongoClient();
	    MongoCollection<Document> collection = mongoClient.getDatabase(cfg.dbName).getCollection("Issuer");
	      try {
	        docTmp = collection.aggregate(pipeline).allowDiskUse(true).iterator().next();    
	      } catch (Exception e) {
	        
	      }
	        
	      mongoClient.close();
		
		rsp = new MsgRsp(header);
		responseStatus = null;
		if(null == docTmp) {
			responseStatus = new MspResponseStatus(9999, Constants.MAP_ERROR.get(9999));
			rsp.setResponseStatus(responseStatus);
			return rsp;
		}
		
		page.setTotalRows(docTmp.getInteger("total", 0));
		rsp.setMsgPage(page);
		
		List<Document> rows = null;
		if(docTmp.get("data") != null && docTmp.get("data") instanceof List) {
			rows = docTmp.getList("data", Document.class);
		}
		
		ArrayList<HashMap<String, Object>> rowsReturn = new ArrayList<HashMap<String, Object>>();
		HashMap<String, Object> hItem = null;
		if(null != rows) {
			for(Document doc: rows) {
				objectId = (ObjectId) doc.get("_id");
				
				hItem = new HashMap<String, Object>();
				hItem.put("_id", objectId.toString());
				hItem.put("TaxCode", doc.get("TaxCode"));
				hItem.put("Name", doc.get("Name"));
				hItem.put("Address", doc.get("Address"));
				hItem.put("Phone", doc.get("Phone"));
				hItem.put("IsActive", doc.get("IsActive"));
				hItem.put("InfoCreated", doc.get("InfoCreated",""));				
				rowsReturn.add(hItem);
			}
		}
		responseStatus = new MspResponseStatus(0, "SUCCESS");
		rsp.setResponseStatus(responseStatus);
		
		HashMap<String, Object> mapDataR = new HashMap<String, Object>();
		mapDataR.put("rows", rowsReturn);
		rsp.setObjData(mapDataR);
		return rsp;
	}

	@Transactional(rollbackFor = {Exception.class})
	@Override
	public MsgRsp crud(JSONRoot jsonRoot) throws Exception {
		Msg msg = jsonRoot.getMsg();
		MsgHeader header = msg.getMsgHeader();
		MsgPage page = msg.getMsgPage();
		Object objData = msg.getObjData();
		Boolean quyen = false ;
		JsonNode jsonData = null;
		if(objData != null) {
			jsonData = Json.serializer().nodeFromObject(msg.getObjData());
		}else{
			throw new Exception("Lỗi dữ liệu đầu vào");
		}
		String agentId   = "61a328076f5d4d9a6bed2dfa";
		String actionCode = header.getActionCode();
		String t = commons.getTextJsonNode(jsonData.at("/TaxCode")).trim().replaceAll("\\s+", "").replaceAll("[+^%$#@&*]*", "").replaceAll("[a-z][A-Z]*", "");
		String n = commons.getTextJsonNode(jsonData.at("/Name"));
		String a = commons.getTextJsonNode(jsonData.at("/Address"));
		String p = commons.getTextJsonNode(jsonData.at("/Phone")).trim().replaceAll("\\s+", "");
		String f = commons.getTextJsonNode(jsonData.at("/Fax"));
		String e = commons.getTextJsonNode(jsonData.at("/Email"));
		String w = commons.getTextJsonNode(jsonData.at("/Website"));
		String ac = commons.getTextJsonNode(jsonData.at("/AccountNumber"));
		String an = commons.getTextJsonNode(jsonData.at("/AccountName"));
		String bn = commons.getTextJsonNode(jsonData.at("/BankName"));
		String boss = commons.getTextJsonNode(jsonData.at("/MainUser"));
		String cv = commons.getTextJsonNode(jsonData.at("/Position"));
		String ng = commons.getTextJsonNode(jsonData.at("/NameUser"));
		String eng = commons.getTextJsonNode(jsonData.at("/EmailUser"));
		String png = commons.getTextJsonNode(jsonData.at("/PhoneUser"));
		String englh = commons.getTextJsonNode(jsonData.at("/EmailUserLh"));
		String tinhThanh = commons.getTextJsonNode(jsonData.at("/TinhThanh"));
		String cqtQLy = commons.getTextJsonNode(jsonData.at("/CqtQLy"));
		String acti = commons.getTextJsonNode(jsonData.at("/IsActive"));
		String 	secureKey = commons.csRandomString(15);
		
		
		MsgRsp rsp = new MsgRsp(header);
		rsp.setMsgPage(page);
		MspResponseStatus responseStatus = null;
		
		List<Document> pipeline = null;
		Document docFind = null;
		Document docTmp = null;
		Document docUpsert = null;
		Document docUpsertUser = null;

		FindOneAndUpdateOptions options = null;

		HashMap<String, String> hR = new HashMap<String, String>();
		
		ObjectId objectId = null;
		ObjectId objectIdUser = null;

		switch (actionCode) {
		case Constants.MSG_ACTION_CODE.CREATED:
			objectId = null;
			objectIdUser = null;
			try {
				objectId = new ObjectId(header.getIssuerId());
			}catch(Exception ex) {}
			try {
				objectIdUser = new ObjectId(header.getUserId());
			}catch(Exception ex) {}
			
			/*KIEM TRA THONG TIN KHACH HANG - USER - TINH THANH - CO QUAN THUE CO TON TAI KHONG*/
			docFind = new Document("_id", objectId).append("IsActive", true).append("IsDelete", new Document("$ne", true));
			pipeline = new ArrayList<Document>();
			pipeline.add(new Document("$match", docFind));
			pipeline.add(new Document("$project", new Document("_id", 1).append("TaxCode", 1).append("Name", 1).append("Address", 1).append("Phone", 1).append("Email", 1)));
			pipeline.add(
					new Document("$lookup", 
						new Document("from", "ConfigEmailAdmin")
						.append("pipeline", 
							Arrays.asList(
								new Document("$match", 
									new Document("$expr", 
											new Document("IssuerId", header.getIssuerId())
									)
								),
								new Document("$project", new Document("_id", 1).append("SmtpServer", 1).append("SmtpPort", 1).append("EmailAddress", 1)
										.append("EmailPassword", 1).append("AutoSend", 1).append("SSL", 1).append("TLS", 1)
										)
							)	
						)
						.append("as", "ConfigEmailAdmin")
					)
				);
				pipeline.add(
					new Document("$unwind", new Document("path", "$ConfigEmailAdmin").append("preserveNullAndEmptyArrays", true))
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
									),
									new Document("$project", new Document("_id", 1).append("LinkLogin", 1))
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
					new Document("from", "Users")
					.append("pipeline", 
						Arrays.asList(
							new Document("$match", 
								new Document("IssuerId", header.getIssuerId()).append("_id", objectIdUser)
								.append("IsActive", true).append("IsDelete", new Document("$ne", true))
							),
							new Document("$project", new Document("_id", 1).append("UserName", 1).append("FullName", 1)),
							new Document("$limit", 1)
						)
					)
					.append("as", "UserInfo")
				)
			);
			pipeline.add(new Document("$unwind", new Document("path", "$UserInfo").append("preserveNullAndEmptyArrays", true)));
			pipeline.add(
				new Document("$lookup", 
					new Document("from", "DMTinhThanh")
					.append("pipeline", 
						Arrays.asList(
							new Document("$match", new Document("IsDelete", new Document("$ne", true)).append("code", commons.regexEscapeForMongoQuery(tinhThanh))),
							new Document("$project", new Document("_id", 0).append("code", 1).append("name", 1))
						)
					)
					.append("as", "DMTinhThanhInfo")
				)
			);
			pipeline.add(new Document("$unwind", new Document("path", "$DMTinhThanhInfo").append("preserveNullAndEmptyArrays", true)));
			pipeline.add(
				new Document("$lookup", 
					new Document("from", "DMChiCucThue")
					.append("pipeline", 
						Arrays.asList(
							new Document("$match", new Document("IsDelete", new Document("$ne", true)).append("code", commons.regexEscapeForMongoQuery(cqtQLy))),
							new Document("$project", new Document("_id", 0).append("code", 1).append("name", 1))
						)
					)
					.append("as", "DMChiCucThueInfo")
				)
			);
			pipeline.add(new Document("$unwind", new Document("path", "$DMChiCucThueInfo").append("preserveNullAndEmptyArrays", true)));
			
			MongoClient mongoClient = cfg.mongoClient();
			MongoCollection<Document> collection = mongoClient.getDatabase(cfg.dbName).getCollection("Issuer");
			      try {
			        docTmp = collection.aggregate(pipeline).allowDiskUse(true).iterator().next();    
			      } catch (Exception ex) {
			        
			      }
			        
			 mongoClient.close();
			
			
			if(null == docTmp) {
				responseStatus = new MspResponseStatus(9999, "Không tìm thấy thông tin khách hàng.");
				rsp.setResponseStatus(responseStatus);
				return rsp;
			}
			if(docTmp.get("ConfigEmailAdmin") == null) {
				responseStatus = new MspResponseStatus(9999, "Chưa cấu hình Mail server.");
				rsp.setResponseStatus(responseStatus);
				return rsp;
			}
			if(docTmp.get("UserInfo") == null) {
				responseStatus = new MspResponseStatus(9999, "Không tìm thấy thông tin người dùng.");
				rsp.setResponseStatus(responseStatus);
				return rsp;
			}
			if(docTmp.get("DMTinhThanhInfo") == null || docTmp.get("DMChiCucThueInfo") == null) {
				responseStatus = new MspResponseStatus(9999, "Vui lòng kiểm tra lại tỉnh/thành phố và cơ quan thuế.");
				rsp.setResponseStatus(responseStatus);
				return rsp;
			}
			if(t.length()<10 || t.length()>14) {
				responseStatus = new MspResponseStatus(9999, "Mã số thuế có độ dài tối đa là 14 ký tự và ít nhất là 10 ký tự. Không có các ký tự đặt biệt và chữ cái. Vui lòng kiểm tra lại.");
				rsp.setResponseStatus(responseStatus);
				return rsp;
			}

			
//			taxCode = docTmp.getString("TaxCode");
			int abc = commons.stringToInteger(acti);
			if(abc == 1) {
				quyen = true;
			}
			if(abc == 2) {
				quyen = false;
			}

			ObjectId idIssu = new ObjectId();
			/*LUU DU LIEU*/
			docUpsert = new Document("_id", idIssu)
				.append("TaxCode", t)
				.append("agentId", agentId)
				.append("Name", n)
				.append("Address", a)
				.append("Phone",p)
				.append("Fax", f)
				.append("Email", e)
				.append("Website", w)
				.append("TinhThanhInfo", docTmp.get("DMTinhThanhInfo"))
				.append("ChiCucThueInfo", docTmp.get("DMChiCucThueInfo"))
				.append("MainUser", boss)
				.append("Position", cv)			
				.append("ContactUser",  
							new Document("NameUser", ng)
							.append("EmailUser", eng)			
							.append("PhoneUser", png)
							.append("EmailUserLh", englh)				
				
						)
			.append("BankAccount", 
							new Document("AccountNumber", ac)
							.append("AccountName", an)
							.append("BankName", bn)
						)
				.append("IsActive", quyen)
				.append("IsRoot", false)
				.append("IsDelete", false)
				.append("IsUserHDDT", true)
				.append("InfoCreated", 
						new Document("CreateDate", LocalDateTime.now())
						.append("CreateUserID", header.getUserId())
						.append("CreateUserName", header.getUserName())
						.append("CreateUserFullName", header.getUserFullName())
					);
			/*END - LUU DU LIEU*/
			
			 mongoClient = cfg.mongoClient();
		        collection = mongoClient.getDatabase(cfg.dbName).getCollection("Issuer");
		        collection.insertOne(docUpsert);      
		        mongoClient.close();
			
			//SET MAIL SERVER
			Document docUpsertmaill = null;
			docUpsertmaill = new Document("IssuerId", idIssu.toString())
					.append("Mail", "N")
					.append("MailJet", "Y")
					.append("InfoCreated", 
							new Document("CreateDate", LocalDateTime.now())
							.append("CreateUserID", header.getUserId())
							.append("CreateUserName", header.getUserName())
							.append("CreateUserFullName", header.getUserFullName())
						);
			
				/*END - LUU DU LIEU*/
				
				mongoClient = cfg.mongoClient();
				collection = mongoClient.getDatabase(cfg.dbName).getCollection("ConfigEmail");
				collection.insertOne(docUpsertmaill);      
				mongoClient.close();
		
			String password = commons.encryptThisString(t + secureKey);	
			  int size = password.length();
		if(size < 128) {
				  secureKey = commons.csRandomString(15);
				  password = commons.encryptThisString(t + secureKey);	
		}
		  int size2 = password.length();
			if(size2 < 128) {
					  secureKey = commons.csRandomString(15);
					  password = commons.encryptThisString(t + secureKey);	
			}
			ObjectId idUser = new ObjectId();
			docUpsertUser = new Document("_id", idUser)
					.append("IssuerId", idIssu.toString())
					.append("UserName", t)
					.append("Password", password)
					.append("FullName",n)
					.append("Phone", p)
					.append("Email", e)
					.append("IsRoot", true)
					.append("IsAdmin", false)
					.append("IsActive", true)
					.append("IsDelete", false)
					.append("InfoCreated", 
							new Document("CreateDate", LocalDateTime.now())
							.append("CreateUserID", header.getUserId())
							.append("CreateUserName", header.getUserName())
							.append("CreateUserFullName", header.getUserFullName())
						);
				/*END - LUU DU LIEU*/
				mongoClient = cfg.mongoClient();
				collection = mongoClient.getDatabase(cfg.dbName).getCollection("Users");
				collection.insertOne(docUpsertUser);      
				mongoClient.close();
			
	if(docUpsertUser != null)	
	{
	MailConfig mailConfig = new MailConfig(docTmp.get("ConfigEmailAdmin", Document.class));
	mailConfig.setNameSend(docTmp.getEmbedded(Arrays.asList("UserName"), ""));
	String link = docTmp.getEmbedded(Arrays.asList("PramLink", "LinkLogin"), "");
	/*THUC HIEN GUI MAIL*/
	String _title = header.getUserFullName() + " Thông báo phát hành tài khoản";
	String _tmp = n;
	String tk = t;
	String _content;
	
	StringBuilder sb = new StringBuilder();
	sb.setLength(0);
	sb.append("<p><span style='font-family: Times New Roman;font-size: 13px;'>Kính gửi: <label style='font-weight: bold;'>" + ("".equals(_tmp)? "Quý khách hàng": _tmp) + "</label><o:p></o:p></span></p>\n");
	sb.append("<p><span style='font-family: Times New Roman;font-size: 13px;'><label style='font-weight: bold;'>" + _title + "</label> Xin chân thành cảm ơn Quý đơn vị đã tin tưởng và sử dụng sản phẩm/dịch vụ của chúng tôi!</span></p>\n");
	sb.append("<p><span style='font-family: Times New Roman;font-size: 13px;'>Chúng tôi vừa phát hành tài khoản đến Quý đơn vị với thông tin như sau:</span></p>\n");
	sb.append("<p><span style='font-family: Times New Roman;font-size: 13px;'>1.  Tài khoản:  " + tk + "</span></p>\n");
	sb.append("<p><span style='font-family: Times New Roman;font-size: 13px;'>2.  Mật khẩu:  " + secureKey + "</span></p>\n");
	sb.append("<p><span style='font-family: Times New Roman;font-size: 13px;'>3.  Link đăng nhập:  " + link + "</span></p>\n");
	sb.append("<p><span style='font-family: Times New Roman;font-size: 13px;'>Quý đơn vị  vui lòng bảo mật thông tin và thay đổi mật khẩu sau khi đăng nhập.</span></p>\n");
	sb.append("<p><span style='font-family: Times New Roman;font-size: 13px;'>Trân trọng kính chào!</span></p>");
	sb.append("<hr style='margin: 5px 0 5px 0;'>");
	sb.append("<p style='margin-bottom: 3px;'><span style='font-family: Times New Roman;font-size: 13px;color:red;font-weight: bold;'>QUÝ CÔNG TY VUI LÒNG KHÔNG REPLY EMAIL NÀY!</span></p>");
	 _content = sb.toString();
	
	
	
	boolean boo = mailUtils.sendMail(mailConfig, _title, _content, englh, null, null, true);
	try {	
		mongoClient = cfg.mongoClient();
		collection = mongoClient.getDatabase(cfg.dbName).getCollection("LogEmail");
		collection.insertOne(new Document("IssuerId", header.getIssuerId())
				.append("UserName", t)
				.append("PassWord", secureKey)
				.append("FullName", n)
				.append("Date", LocalDateTime.now())
				.append("EmailLH", englh)
				.append("IsActive", true)
				.append("IsDelete", false)
				.append("EmailContent", _content)
				
			);      
		mongoClient.close();
		
	}catch(Exception ex) {}}
	else {
		responseStatus = new MspResponseStatus(9999, "Lỗi tạo tài khoản");
		rsp.setResponseStatus(responseStatus);	
	}

///*END - INSERT LOG MAIL*/
			
			
			responseStatus = new MspResponseStatus(0, "SUCCESS");
			rsp.setResponseStatus(responseStatus);				
			return rsp;
			
			
			
	///RESET PASSWORD 
		case Constants.MSG_ACTION_CODE.RESET_PASSWORD:	
			objectId = null;
			objectIdUser = null;
			try {
				objectId = new ObjectId(header.getIssuerId());
			}catch(Exception ex) {}
		
			pipeline = new ArrayList<Document>();
			pipeline.add(
				new Document("$match", 
					new Document("_id", objectId).append("IsActive", true).append("IsDelete", new Document("$ne", true))
				)
			);
			pipeline.add(new Document("$project", new Document("_id", 1).append("TaxCode", 1).append("Name", 1).append("Address", 1).append("Phone", 1).append("Email", 1)));
			pipeline.add(
				new Document("$lookup", 
					new Document("from", "Users")
					.append("pipeline", 
						Arrays.asList(
							new Document("$match", 
								new Document("UserName", t)
								.append("IsActive", true).append("IsDelete", new Document("$ne", true))
							),
							new Document("$project", new Document("_id", 1).append("UserName", 1).append("FullName", 1)),
							new Document("$limit", 1)
						)
					)
					.append("as", "UserInfo")
				)
			);
			pipeline.add(
					new Document("$unwind", new Document("path", "$UserInfo").append("preserveNullAndEmptyArrays", true))
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
								),
								new Document("$project", new Document("_id", 1).append("LinkLogin", 1))
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
						new Document("from", "ConfigEmailAdmin")
						.append("pipeline", 
							Arrays.asList(
								new Document("$match", 
									new Document("$expr", 
											new Document("IssuerId", header.getIssuerId())
									)
								),
								new Document("$project", new Document("_id", 1).append("SmtpServer", 1).append("SmtpPort", 1).append("EmailAddress", 1)
										.append("EmailPassword", 1).append("AutoSend", 1).append("SSL", 1).append("TLS", 1)
										)
							)	
						)
						.append("as", "ConfigEmailAdmin")
					)
				);
				pipeline.add(
					new Document("$unwind", new Document("path", "$ConfigEmailAdmin").append("preserveNullAndEmptyArrays", true))
				);	
		

			docFind = new Document("TaxCode", t).append("IsDelete", new Document("$ne", true));
			pipeline.add(
				new Document("$lookup", 
					new Document("from", "Issuer")
					.append("pipeline", 
						Arrays.asList(
							new Document("$match", docFind),
							new Document("$project", new Document("_id", 1))
						)
					)
					.append("as", "Issuer")
				)
			);
			pipeline.add(new Document("$unwind", new Document("path", "$Issuer").append("preserveNullAndEmptyArrays", true)));
			
			mongoClient = cfg.mongoClient();
			collection = mongoClient.getDatabase(cfg.dbName).getCollection("Issuer");
			      try {
			        docTmp = collection.aggregate(pipeline).allowDiskUse(true).iterator().next();    
			      } catch (Exception ex) {
			        
			      }
			        
			mongoClient.close();
			
			if(null == docTmp) {
				responseStatus = new MspResponseStatus(9999, "Không tìm thấy thông tin khách hàng.");
				rsp.setResponseStatus(responseStatus);
				return rsp;
			}
			if(docTmp.get("UserInfo") == null) {
				responseStatus = new MspResponseStatus(9999, "Không tìm thấy thông tin khách hàng.");
				rsp.setResponseStatus(responseStatus);
				return rsp;
			}
			if(docTmp.get("Issuer") == null) {
				responseStatus = new MspResponseStatus(9999, "Không tìm thấy thông tin khách hàng cần cập nhật.");
				rsp.setResponseStatus(responseStatus);
				return rsp;
			}
			
			
			if(docTmp.get("ConfigEmailAdmin") == null) {
			responseStatus = new MspResponseStatus(9999, "Chưa cấu hình Mail server.");
			rsp.setResponseStatus(responseStatus);
			return rsp;
		}
		
			
			// LAY THONG TIN CAP NHAT
			String id_user = docTmp.getEmbedded(Arrays.asList("UserInfo", "_id"), ObjectId.class).toString();	
			ObjectId objectId_User = new ObjectId(id_user);
			//CAP NHAT MAT KHAU
		
			String password_update = commons.encryptThisString(t + secureKey);	
			  int size_update = password_update.length();
		if(size_update < 128) {
				  secureKey = commons.csRandomString(15);
				  password_update = commons.encryptThisString(t + secureKey);	
		}
		  int size2_update = password_update.length();
			if(size2_update < 128) {
					  secureKey = commons.csRandomString(15);
					  password_update = commons.encryptThisString(t + secureKey);	
			}
		
			hR = new HashMap<String, String>();
			hR.put("_id", objectId.toString());
			hR.put("Password", secureKey);
			
			Document findUser = new Document("_id", objectId_User).append("IsDelete", new Document("$ne", true)).append("IsActive", true);
			
			options = new FindOneAndUpdateOptions();
			options.upsert(true);
			options.maxTime(5000, TimeUnit.MILLISECONDS);
			options.returnDocument(ReturnDocument.AFTER);
					
			mongoClient = cfg.mongoClient();
		    collection = mongoClient.getDatabase(cfg.dbName).getCollection("Users");
		    collection.findOneAndUpdate(findUser,
		    		 new Document("$set", 
								new Document("Password", password_update)
								.append("InfoResetPass", 
										new Document("ResetPassDate", LocalDateTime.now())
											.append("ResetPassUserID", header.getUserId())
											.append("ResetPassUserName", header.getUserName())
											.append("ResetPassUserFullName", header.getUserFullName())
										)
								)
						, options);
		      mongoClient.close();
		      
			//GUI MAI			
			//GHI LOG
			MailConfig mailConfig = new MailConfig(docTmp.get("ConfigEmailAdmin", Document.class));
			mailConfig.setNameSend(docTmp.getEmbedded(Arrays.asList("UserName"), ""));
			
			/*THUC HIEN GUI MAIL*/
			String _title = header.getUserFullName() + " Thông báo phát hành tài khoản";
			String _tmp = n;
			String tk = t;
			String _content;
			String link = docTmp.getEmbedded(Arrays.asList("PramLink", "LinkLogin"), "");
			StringBuilder sb = new StringBuilder();
			sb.setLength(0);
			sb.append("<p><span style='font-family: Times New Roman;font-size: 13px;'>Kính gửi: <label style='font-weight: bold;'>" + ("".equals(_tmp)? "Quý khách hàng": _tmp) + "</label><o:p></o:p></span></p>\n");
			sb.append("<p><span style='font-family: Times New Roman;font-size: 13px;'><label style='font-weight: bold;'>" + _title + "</label> Xin chân thành cảm ơn Quý đơn vị đã tin tưởng và sử dụng sản phẩm/dịch vụ của chúng tôi!</span></p>\n");
			sb.append("<p><span style='font-family: Times New Roman;font-size: 13px;'>Chúng tôi vừa phát hành tài khoản đến Quý đơn vị với thông tin như sau:</span></p>\n");
			sb.append("<p><span style='font-family: Times New Roman;font-size: 13px;'>1.  Tài khoản:  " + tk + "</span></p>\n");
			sb.append("<p><span style='font-family: Times New Roman;font-size: 13px;'>2.  Mật khẩu:  " + secureKey + "</span></p>\n");
			sb.append("<p><span style='font-family: Times New Roman;font-size: 13px;'>3.  Link đăng nhập:  " + link + "</span></p>\n");
			sb.append("<p><span style='font-family: Times New Roman;font-size: 13px;'>Quý đơn vị  vui lòng bảo mật thông tin và thay đổi mật khẩu sau khi đăng nhập.</span></p>\n");
			sb.append("<p><span style='font-family: Times New Roman;font-size: 13px;'>Trân trọng kính chào!</span></p>");
			sb.append("<hr style='margin: 5px 0 5px 0;'>");
			sb.append("<p style='margin-bottom: 3px;'><span style='font-family: Times New Roman;font-size: 13px;color:red;font-weight: bold;'>QUÝ CÔNG TY VUI LÒNG KHÔNG REPLY EMAIL NÀY!</span></p>");
			 _content = sb.toString();
			
			
			
			boolean boo = mailUtils.sendMail(mailConfig, _title, _content, englh, null, null, true);
			try {
			
				mongoClient = cfg.mongoClient();
				collection = mongoClient.getDatabase(cfg.dbName).getCollection("LogEmail");
				collection.insertOne(new Document("IssuerId", header.getIssuerId())
						.append("UserName", t)
						.append("PassWord", secureKey)
						.append("FullName", n)
						.append("Date", LocalDateTime.now())
						.append("EmailLH", englh)
						.append("IsActive", true)
						.append("IsDelete", false)
						.append("EmailContent", _content)
						
					);      
				mongoClient.close();
				
			}catch(Exception ex) {}
	
		responseStatus = new MspResponseStatus(0, "SUCCESS");
		rsp.setResponseStatus(responseStatus);
		rsp.setObjData(hR);
		return rsp;	
			
	//END RESET PASSWORD		
//////////////////////////////////////////////////////////////////////////////////////////////////////////
		case Constants.MSG_ACTION_CODE.CHECK:
			/*KIEM TRA THONG TIN KHACH HANG - USER - TINH THANH - CO QUAN THUE CO TON TAI KHONG*/
			docFind = new Document("_id", objectId).append("IsActive", true).append("IsDelete", new Document("$ne", true));
			pipeline = new ArrayList<Document>();
			pipeline.add(
				new Document("$lookup", 
					new Document("from", "Users")
					.append("pipeline", 
						Arrays.asList(
							new Document("$match", 
								new Document("UserName",t)
								.append("IsActive", true).append("IsDelete", new Document("$ne", true))
							),
							new Document("$project", new Document("_id", 1).append("UserName", 1).append("FullName", 1)),
							new Document("$limit", 1)
						)
					)
					.append("as", "UserInfo")
				)
			);
			pipeline.add(new Document("$unwind", new Document("path", "$UserInfo").append("preserveNullAndEmptyArrays", true)));
			
			mongoClient = cfg.mongoClient();
			collection = mongoClient.getDatabase(cfg.dbName).getCollection("Issuer");
			  try {
			         docTmp = collection.aggregate(pipeline).allowDiskUse(true).iterator().next();      
			     } catch (Exception ex) {
			        
			    }
			mongoClient.close();
			
			if(docTmp.get("UserInfo") != null) {
				responseStatus = new MspResponseStatus(9999, "Tài khoản đã tồn tại");
				rsp.setResponseStatus(responseStatus);
				return rsp;
			}
			else {
			responseStatus = new MspResponseStatus(0, "SUCCESS");
			rsp.setResponseStatus(responseStatus);				
			return rsp;		
			}
//////////////////////////////////////////////////////////////////////////////////////////////////////////			
		case Constants.MSG_ACTION_CODE.MODIFY:
			objectId = null;
			objectIdUser = null;

			try {
				objectId = new ObjectId(header.getIssuerId());
			}catch(Exception ex) {}
			try {
				objectIdUser = new ObjectId(header.getUserId());
			}catch(Exception ex) {}
					
			pipeline = new ArrayList<Document>();
			pipeline.add(
				new Document("$match", 
					new Document("_id", objectId).append("IsActive", true).append("IsDelete", new Document("$ne", true))
				)
			);
			pipeline.add(new Document("$project", new Document("_id", 1).append("TaxCode", 1).append("Name", 1).append("Address", 1).append("Phone", 1).append("Email", 1)));
			
			pipeline.add(
				new Document("$lookup", 
					new Document("from", "Users")
					.append("pipeline", 
						Arrays.asList(
							new Document("$match", 
								new Document("IssuerId", header.getIssuerId()).append("_id", objectIdUser)
								.append("IsActive", true).append("IsDelete", new Document("$ne", true))
							),
							new Document("$project", new Document("_id", 1).append("UserName", 1).append("FullName", 1)),
							new Document("$limit", 1)
						)
					)
					.append("as", "UserInfo")
				)
			);
			pipeline.add(new Document("$unwind", new Document("path", "$UserInfo").append("preserveNullAndEmptyArrays", true)));
			pipeline.add(
					new Document("$lookup", 
						new Document("from", "DMTinhThanh")
						.append("pipeline", 
							Arrays.asList(
								new Document("$match", new Document("IsDelete", new Document("$ne", true)).append("code", commons.regexEscapeForMongoQuery(tinhThanh))),
								new Document("$project", new Document("_id", 0).append("code", 1).append("name", 1))
							)
						)
						.append("as", "DMTinhThanhInfo")
					)
				);
				pipeline.add(new Document("$unwind", new Document("path", "$DMTinhThanhInfo").append("preserveNullAndEmptyArrays", true)));
				pipeline.add(
					new Document("$lookup", 
						new Document("from", "DMChiCucThue")
						.append("pipeline", 
							Arrays.asList(
								new Document("$match", new Document("IsDelete", new Document("$ne", true)).append("code", commons.regexEscapeForMongoQuery(cqtQLy))),
								new Document("$project", new Document("_id", 0).append("code", 1).append("name", 1))
							)
						)
						.append("as", "DMChiCucThueInfo")
					)
				);
				pipeline.add(new Document("$unwind", new Document("path", "$DMChiCucThueInfo").append("preserveNullAndEmptyArrays", true)));
					
			docFind = new Document("TaxCode", t).append("IsDelete", new Document("$ne", true));
			pipeline.add(
				new Document("$lookup", 
					new Document("from", "Issuer")
					.append("pipeline", 
						Arrays.asList(
							new Document("$match", docFind),
							new Document("$project", new Document("_id", 1))
						)
					)
					.append("as", "Issuer")
				)
			);
			pipeline.add(new Document("$unwind", new Document("path", "$Issuer").append("preserveNullAndEmptyArrays", true)));
			
			mongoClient = cfg.mongoClient();
			collection = mongoClient.getDatabase(cfg.dbName).getCollection("Issuer");
			  try {
			        docTmp = collection.aggregate(pipeline).allowDiskUse(true).iterator().next();      
			     } catch (Exception ex) {
			        
			    }
			mongoClient.close();
			
			if(null == docTmp) {
				responseStatus = new MspResponseStatus(9999, "Không tìm thấy thông tin khách hàng.");
				rsp.setResponseStatus(responseStatus);
				return rsp;
			}
			if(docTmp.get("UserInfo") == null) {
				responseStatus = new MspResponseStatus(9999, "Không tìm thấy thông tin người dùng.");
				rsp.setResponseStatus(responseStatus);
				return rsp;
			}
			if(docTmp.get("Issuer") == null) {
				responseStatus = new MspResponseStatus(9999, "Không tìm thấy thông tin hóa đơn cần cập nhật.");
				rsp.setResponseStatus(responseStatus);
				return rsp;
			}
			

			int abc2 = commons.stringToInteger(acti);
			if(abc2 == 1) {
				quyen = true;
			}
			if(abc2 == 2) {
				quyen = false;
			}
		
			options = new FindOneAndUpdateOptions();
			options.upsert(false);
			options.maxTime(5000, TimeUnit.MILLISECONDS);
			options.returnDocument(ReturnDocument.AFTER);
			Object tt  =  docTmp.get("DMTinhThanhInfo");
			Object th  =  docTmp.get("DMChiCucThueInfo");
				
			mongoClient = cfg.mongoClient();
		    collection = mongoClient.getDatabase(cfg.dbName).getCollection("Issuer");
		    collection.findOneAndUpdate(docFind,
		    		new Document("$set", 
							new Document("TaxCode", t)
							.append("agentId", agentId)
							.append("Name", n)
							.append("Address", a)
							.append("Phone", p)
							.append("Fax", f)
							.append("Email", e)
							.append("Website", w)			
							.append("TinhThanhInfo", tt)
							.append("ChiCucThueInfo", th)
							.append("MainUser", boss)
							.append("Position", cv)
							.append("IsActive", quyen)
							.append("BankAccount", 
									new Document("AccountNumber",ac)
										.append("AccountName", an)
										.append("BankName", bn)
								)
							.append("ContactUser", 
									new Document("NameUser",ng)
										.append("PhoneUser", png)
										.append("EmailUser", eng)
										.append("EmailUserLh", englh)
								)
							.append("InfoUpdated", 
									new Document("UpdatedDate", LocalDateTime.now())
										.append("UpdatedUserID", header.getUserId())
										.append("UpdatedUserName", header.getUserName())
										.append("UpdatedUserFullName", header.getUserFullName())
								)
						),
					options
				); 
		      mongoClient.close();
			
			responseStatus = new MspResponseStatus(0, "SUCCESS");
			rsp.setResponseStatus(responseStatus);				
			return rsp;		
		case Constants.MSG_ACTION_CODE.UPDATE_INFO:
		
			Document docTmp1 = null;
			Document docTmp2 = null;
			/*CAP NHAT LAI THONG TIN KHACH HANG HDDT VA NGUOI MUA*/
			Iterator<Document> iter = null;
			List<Document> pipeline1 = new ArrayList<Document>();
			
			Document docMatch = new Document("IsDelete",new Document("$ne", true));				
			pipeline1 = new ArrayList<Document>();
			pipeline1.add(new Document("$match", docMatch));
			pipeline1.add(new Document("$project", new Document("_id", 1).append("InfoCreated", 1)));
			
			mongoClient = cfg.mongoClient();
			collection = mongoClient.getDatabase(cfg.dbName).getCollection("Issuer");
			
			try {
				iter = collection.aggregate(pipeline1).allowDiskUse(true).iterator();
			} catch (Exception ex) {

			}
			mongoClient.close();
			
			while(iter.hasNext()) {
				try {
					docTmp1 = iter.next();
				} catch (Exception ex) {
					
				}
			
				ObjectId id_issu = docTmp1.getObjectId("_id");				
				Object createdInfo = docTmp1.get("InfoCreated", "");
				if(createdInfo.equals("")) {
					options = new FindOneAndUpdateOptions();
					options.upsert(true);
					options.maxTime(5000, TimeUnit.MILLISECONDS);
					options.returnDocument(ReturnDocument.AFTER);
					
					Document docFind1 = new Document("_id", id_issu).append("IsDelete", new Document("$ne", true));
					
					mongoClient = cfg.mongoClient();
				    collection = mongoClient.getDatabase(cfg.dbName).getCollection("Issuer");
				    
				    collection.findOneAndUpdate(docFind1,
				    		new Document("$set",
									new Document("IsUserHDDT", true)							
									.append("InfoCreated", 
											new Document("CreateDate", LocalDateTime.now())
											.append("CreateUserID", header.getUserId())
											.append("CreateUserName", header.getUserName())
											.append("CreateUserFullName", header.getUserFullName()))
											
									),
							options); 
				      mongoClient.close();
				      
				}else {
					String idUSer = docTmp1.getEmbedded(Arrays.asList("InfoCreated", "CreateUserID"), "");
					
					ObjectId ID_USER = new ObjectId(idUSer);
					Document findUserById = new Document("_id", ID_USER).append("IsDelete", new Document("$ne", true));
										
					mongoClient = cfg.mongoClient();
					collection = mongoClient.getDatabase(cfg.dbName).getCollection("Users");
					  try {
						  docTmp2 = collection.find(findUserById).allowDiskUse(true).iterator().next();      
					     } catch (Exception ex) {
					        
					    }
					mongoClient.close();
					
					boolean checkAdmin = docTmp2.getBoolean("IsAdmin", false);
					if(checkAdmin==true) {
						options = new FindOneAndUpdateOptions();
						options.upsert(true);
						options.maxTime(5000, TimeUnit.MILLISECONDS);
						options.returnDocument(ReturnDocument.AFTER);
						
						Document docFind1 = new Document("_id", id_issu).append("IsDelete", new Document("$ne", true));
						
						mongoClient = cfg.mongoClient();
					    collection = mongoClient.getDatabase(cfg.dbName).getCollection("Issuer");
					    collection.findOneAndUpdate(docFind1,
					    		new Document("$set",
										new Document("IsUserHDDT", true)											
										),
								options);	
					      mongoClient.close();
					      
					}else {
						options = new FindOneAndUpdateOptions();
						options.upsert(true);
						options.maxTime(5000, TimeUnit.MILLISECONDS);
						options.returnDocument(ReturnDocument.AFTER);
						
						Document docFind1 = new Document("_id", id_issu).append("IsDelete", new Document("$ne", true));	
						
						mongoClient = cfg.mongoClient();
					    collection = mongoClient.getDatabase(cfg.dbName).getCollection("Issuer");
					    collection.findOneAndUpdate(docFind1,
					    		new Document("$set",
										new Document("IsUserHDDT", false)											
										),
								options);
					      mongoClient.close();
					}
				}
			
			}
			mongoClient.close();
			responseStatus = new MspResponseStatus(0, "SUCCESS");
			rsp.setResponseStatus(responseStatus);				
			return rsp;		
			}
		return rsp;
	
	}

	@Override
	public MsgRsp param(JSONRoot jsonRoot, String _id) {
			Msg msg = jsonRoot.getMsg();
			MsgHeader header = msg.getMsgHeader();
			MsgPage page = msg.getMsgPage();

			MsgRsp rsp = new MsgRsp(header);
			rsp.setMsgPage(page);
			MspResponseStatus responseStatus = null;
			List<Document> pipeline = null;
			Document docTmp = null;
		
			pipeline = new ArrayList<Document>();
			pipeline.add(
					new Document("$lookup", 
						new Document("from", "UserRounding")
						.append("pipeline", 
							Arrays.asList(
								new Document("$match", 
									new Document("IssuerId", header.getIssuerId())
								)
							)
						)
						.append("as", "UserRounding")
					)
				);
			
			pipeline.add(new Document("$unwind", new Document("path", "$UserRounding").append("preserveNullAndEmptyArrays", true)));
						
			MongoClient mongoClient = cfg.mongoClient();
			MongoCollection<Document> collection = mongoClient.getDatabase(cfg.dbName).getCollection("UserRounding");
			      try {
			        docTmp = collection.aggregate(pipeline).allowDiskUse(true).iterator().next();    
			      } catch (Exception e) {
			        
			      }
			        
			mongoClient.close();
			
			if (null == docTmp) {
				responseStatus = new MspResponseStatus(9999, Constants.MAP_ERROR.get(9999));
				rsp.setResponseStatus(responseStatus);
				return rsp;
			}

			rsp.setObjData(docTmp);
			responseStatus = new MspResponseStatus(0, Constants.MAP_ERROR.get(0));
			rsp.setResponseStatus(responseStatus);
			return rsp;
		}

	@Override
	public MsgRsp mskh(JSONRoot jsonRoot, String _id) throws Exception {	
		MsgRsp rsp = new MsgRsp();
		MspResponseStatus responseStatus = null;
		List<Document> pipeline = null;
		Document docTmp = null;

		int currentYear = LocalDate.now().get(ChronoField.YEAR);
		int lastYear = currentYear - 1;
		
			pipeline = new ArrayList<Document>();
			pipeline.add(
				new Document("$lookup", 
					new Document("from", "DMMauSoKyHieu")
					.append("pipeline", 
						Arrays.asList(
							new Document("$match", 
								new Document("IssuerId",_id)
								.append("IsDelete", new Document("$ne", true))
								.append("$expr", new Document("$and", Arrays.asList(		
										new Document("$gte", Arrays.asList("$NamPhatHanh", lastYear))					
										)))
									)
						)
					)
					.append("as", "DMMauSoKyHieu")
				)
			);

			MongoClient mongoClient = cfg.mongoClient();
			MongoCollection<Document> collection = mongoClient.getDatabase(cfg.dbName).getCollection("DMMauSoKyHieu");
			docTmp =   collection.aggregate(pipeline).allowDiskUse(true).iterator().next();				
			mongoClient.close();
			if(null == docTmp) {
				responseStatus = new MspResponseStatus(9999, Constants.MAP_ERROR.get(9999));
				rsp.setResponseStatus(responseStatus);
				return rsp;
			}
			
			rsp.setObjData(docTmp);
			responseStatus = new MspResponseStatus(0, Constants.MAP_ERROR.get(0));
			rsp.setResponseStatus(responseStatus);
			return rsp;
		}
		

}
