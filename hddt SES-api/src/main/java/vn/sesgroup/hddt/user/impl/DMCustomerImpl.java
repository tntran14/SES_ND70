package vn.sesgroup.hddt.user.impl;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
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
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.FormulaEvaluator;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.api.message.JSONRoot;
import com.api.message.Msg;
import com.api.message.MsgHeader;
import com.api.message.MsgPage;
import com.api.message.MsgRsp;
import com.api.message.MspResponseStatus;
import com.fasterxml.jackson.databind.JsonNode;
import com.mongodb.bulk.BulkWriteResult;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.BulkWriteOptions;
import com.mongodb.client.model.FindOneAndUpdateOptions;
import com.mongodb.client.model.ReturnDocument;
import com.mongodb.client.model.UpdateOneModel;
import com.mongodb.client.model.UpdateOptions;
import com.mongodb.client.model.WriteModel;

import vn.sesgroup.hddt.configuration.ConfigConnectMongo;
import vn.sesgroup.hddt.dto.MailConfig;
import vn.sesgroup.hddt.user.dao.AbstractDAO;
import vn.sesgroup.hddt.user.dao.DMCustomerDAO;
import vn.sesgroup.hddt.utility.Constants;
import vn.sesgroup.hddt.utility.Json;
import vn.sesgroup.hddt.utility.MailUtils;
import vn.sesgroup.hddt.utility.SystemParams;

@Repository
@Transactional
public class DMCustomerImpl extends AbstractDAO implements DMCustomerDAO{
	private static final Logger log = LogManager.getLogger(DMCustomerImpl.class);
	@Autowired
	ConfigConnectMongo cfg;
	private MailUtils mailUtils = new MailUtils();


	
	@Override
	public MsgRsp list(JSONRoot jsonRoot) throws Exception {
		Msg msg = jsonRoot.getMsg();
		MsgHeader header = msg.getMsgHeader();
		MsgPage page = msg.getMsgPage();
		Object objData = msg.getObjData();
		
		String taxCode = "";
		String companyName = "";
		String customerName = "";
		
		JsonNode jsonData = null;
		if(objData != null) {
			jsonData = Json.serializer().nodeFromObject(objData);
			taxCode = commons.getTextJsonNode(jsonData.at("/TaxCode")).trim().replaceAll("\\s+", " ");
			companyName = commons.getTextJsonNode(jsonData.at("/CompanyName")).trim().replaceAll("\\s+", " ");
			customerName = commons.getTextJsonNode(jsonData.at("/CustomerName")).trim().replaceAll("\\s+", " ");
			
		}
		
		MsgRsp rsp = new MsgRsp(header);
		MspResponseStatus responseStatus = null;
		
		ObjectId objectId = null;
		Document docTmp = null;

		List<Document> pipeline = new ArrayList<Document>();
		
		Document docMatch = new Document("IssuerId", header.getIssuerId())
				.append("IsDelete", new Document("$ne", true));
		if(!"".equals(taxCode))
			docMatch.append("TaxCode", new Document("$regex", commons.regexEscapeForMongoQuery(taxCode)).append("$options", "i"));
		if(!"".equals(companyName))
			docMatch.append("CompanyName", new Document("$regex", commons.regexEscapeForMongoQuery(companyName)).append("$options", "i"));
		if(!"".equals(customerName))
			docMatch.append("CustomerName", new Document("$regex", commons.regexEscapeForMongoQuery(customerName)).append("$options", "i"));

		Document fillter = new Document("_id", 1).append("TaxCode", 1).append("CustomerCode", 1)
				.append("CompanyName", 1).append("CustomerName", 1).append("Address", 1).append("Email", 1).append("EmailCC", 1)
				.append("Province", 1).append("CustomerGroup1", 1).append("CustomerGroup2", 1).append("CustomerGroup3", 1).append("InfoCreated", 1);
		
		pipeline = new ArrayList<Document>();
		pipeline.add(new Document("$match", docMatch));
		pipeline.add(
			new Document("$sort", 
				new Document("Stock", 1).append("Code", 1).append("_id", -1)
			)
		);
		pipeline.add(new Document("$project", fillter));
		pipeline.addAll(createFacetForSearchNotSort(page));
		

		
		MongoClient mongoClient = cfg.mongoClient();
		MongoCollection<Document> collection = mongoClient.getDatabase(cfg.dbName).getCollection("DMCustomer");
		try {
			docTmp =   collection.aggregate(pipeline).allowDiskUse(true).iterator().next();		
		} catch (Exception e) {
			// TODO: handle exception
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
				hItem.put("CustomerCode", doc.get("CustomerCode"));
				hItem.put("CompanyName", doc.get("CompanyName"));
				hItem.put("CustomerName", doc.get("CustomerName"));
				hItem.put("Address", doc.get("Address"));
				hItem.put("Email", doc.get("Email"));
				hItem.put("EmailCC", doc.get("EmailCC"));
				hItem.put("Province", doc.get("Province"));
				hItem.put("CustomerGroup1", doc.get("CustomerGroup1"));
				hItem.put("CustomerGroup2", doc.get("CustomerGroup2"));
				hItem.put("CustomerGroup3", doc.get("CustomerGroup3"));
				
				hItem.put("InfoCreated", doc.get("InfoCreated"));
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

	@Override
	public MsgRsp crud(JSONRoot jsonRoot) throws Exception {
		Msg msg = jsonRoot.getMsg();
		MsgHeader header = msg.getMsgHeader();
		MsgPage page = msg.getMsgPage();
		Object objData = msg.getObjData();
		
		JsonNode jsonData = null;
		if(objData != null) {
			jsonData = Json.serializer().nodeFromObject(msg.getObjData());
		}else{
			throw new Exception("Lỗi dữ liệu đầu vào");
		}
		
		String actionCode = header.getActionCode();
		String _id = commons.getTextJsonNode(jsonData.at("/_id")).replaceAll("\\s", "");
		String taxCode = commons.getTextJsonNode(jsonData.at("/TaxCode")).trim().replaceAll("\\s+", "").replaceAll("[+^%$#@&*]*", "").replaceAll("[a-z][A-Z]*", "");
		String customerCode = commons.getTextJsonNode(jsonData.at("/CustomerCode")).trim().replaceAll("\\s+", " ");
		String companyName = commons.getTextJsonNode(jsonData.at("/CompanyName")).trim().replaceAll("\\s+", " ");
		String customerName = commons.getTextJsonNode(jsonData.at("/CustomerName")).trim().replaceAll("\\s+", " ");
		String address = commons.getTextJsonNode(jsonData.at("/Address")).trim().replaceAll("\\s+", " ");
		String email = commons.getTextJsonNode(jsonData.at("/Email")).trim().replaceAll("\\s+", " ");
		String emailcc = commons.getTextJsonNode(jsonData.at("/EmailCC")).trim().replaceAll("\\s+", " ");
		String Boxmail = commons.getTextJsonNode(jsonData.at("/Boxmail")).trim().replaceAll("\\s+", " ");
		
		String phone = commons.getTextJsonNode(jsonData.at("/Phone")).trim().replaceAll("\\s+", " ");
		String fax = commons.getTextJsonNode(jsonData.at("/Fax")).trim().replaceAll("\\s+", " ");
		String website = commons.getTextJsonNode(jsonData.at("/Website")).trim().replaceAll("\\s+", " ");
		String province = commons.getTextJsonNode(jsonData.at("/Province")).trim().replaceAll("\\s+", " ");
		String provinceName = commons.getTextJsonNode(jsonData.at("/ProvinceName")).trim().replaceAll("\\s+", " ");
		String roleId =  commons.getTextJsonNode(jsonData.at("/RoleId")).replaceAll("\\s", "");
		String accountNumber = commons.getTextJsonNode(jsonData.at("/AccountNumber")).trim().replaceAll("\\s+", " ");
		String accountBankName = commons.getTextJsonNode(jsonData.at("/AccountBankName")).trim().replaceAll("\\s+", " ");
		String customerGroup1 = commons.getTextJsonNode(jsonData.at("/CustomerGroup1")).trim().replaceAll("\\s+", " ");
		String customerGroup1Name = commons.getTextJsonNode(jsonData.at("/CustomerGroup1Name")).trim().replaceAll("\\s+", " ");
		
		String customerGroup2 = commons.getTextJsonNode(jsonData.at("/CustomerGroup2")).trim().replaceAll("\\s+", " ");
		String customerGroup2Name = commons.getTextJsonNode(jsonData.at("/CustomerGroup2Name")).trim().replaceAll("\\s+", " ");
		
		String customerGroup3 = commons.getTextJsonNode(jsonData.at("/CustomerGroup3")).trim().replaceAll("\\s+", " ");
		String customerGroup3Name = commons.getTextJsonNode(jsonData.at("/CustomerGroup3Name")).trim().replaceAll("\\s+", " ");
		
		String remark = commons.getTextJsonNode(jsonData.at("/Remark")).trim().replaceAll("\\s+", " ");

		
		LocalDate now = LocalDate.now();
		String fromDate = commons.convertLocalDateTimeToString(now.plusMonths(12).with(ChronoField.DAY_OF_MONTH, 1), Constants.FORMAT_DATE.FORMAT_DATE_WEB);
		String toDate =  commons.convertLocalDateTimeToString(now, Constants.FORMAT_DATE.FORMAT_DATE_WEB);
		
		LocalDate dateFrom = null;
		LocalDate dateTo =  null;
		dateFrom = "".equals(fromDate) || !commons.checkLocalDate(fromDate, Constants.FORMAT_DATE.FORMAT_DATE_WEB)
				? null
				: commons.convertStringToLocalDate(fromDate, Constants.FORMAT_DATE.FORMAT_DATE_WEB);
		dateTo = "".equals(toDate) || !commons.checkLocalDate(toDate, Constants.FORMAT_DATE.FORMAT_DATE_WEB) ? null
				: commons.convertStringToLocalDate(toDate, Constants.FORMAT_DATE.FORMAT_DATE_WEB);
		
		MsgRsp rsp = new MsgRsp(header);
		rsp.setMsgPage(page);
		MspResponseStatus responseStatus = null;
		
		ObjectId objectId = null;
		Document docFind = null;
		Document docTmp = null;
		Document docUpsert = null;
		Document docR = null;
		String link = "";
		List<Document> pipeline = null;
		FindOneAndUpdateOptions options = null;
		
		MailConfig mailConfig = new MailConfig();
		switch (actionCode) {
		case Constants.MSG_ACTION_CODE.CREATED:
			if("".equals(customerCode))
				customerCode = "MCK" + commons.convertLocalDateTimeToString(LocalDate.now(), "yyyyMMdd") + "-" + commons.csRandomAlphaNumbericString(3).toUpperCase();
		
			objectId = null;
			try {
				objectId = new ObjectId(header.getIssuerId());
			}catch(Exception e) {}
			docFind = new Document("_id", objectId).append("IsActive", true).append("IsDelete", new Document("$ne", true));
			Document docFind2 = new Document("IssuerId",  header.getIssuerId());
			pipeline = new ArrayList<Document>();
			pipeline.add(new Document("$match", docFind));
			pipeline.add(new Document("$project", new Document("_id", 1)));
			
			
			pipeline.add(
					new Document("$lookup", 
						new Document("from", "ConfigEmail")
						.append("pipeline", 
							Arrays.asList(
								new Document("$match", 
										docFind2
								)
								
							)	
						)
						.append("as", "ConfigEmail")
					)
				);
				pipeline.add(
					new Document("$unwind", new Document("path", "$ConfigEmail").append("preserveNullAndEmptyArrays", true))
				);	
		
			pipeline.add(
				new Document("$lookup", 
					new Document("from", "DMCustomer")
					.append("let", new Document("vIssuerId", new Document("$toString", "$_id")))
					.append("pipeline", 
						Arrays.asList(
							new Document("$match", 
								new Document("$expr", 
									new Document("$and", 
										Arrays.asList(
											new Document("$eq", Arrays.asList("$IssuerId", "$$vIssuerId")),
											new Document("$ne", Arrays.asList("$IsDelete", true)),
											new Document("$or", 
												Arrays.asList(
													new Document("$eq", Arrays.asList("$TaxCode", commons.regexEscapeForMongoQuery(taxCode))),
													new Document("$eq", Arrays.asList("$CustomerCode", commons.regexEscapeForMongoQuery(customerCode)))
												)
											)
										)
									)
									
								)
							),
							new Document("$project", new Document("_id", 1)),
							new Document("$limit", 1)
						)
					)
					.append("as", "DMCustomer")
				)
			);
			pipeline.add(new Document("$unwind", new Document("path", "$DMCustomer").append("preserveNullAndEmptyArrays", true)));
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
			
			
			MongoClient mongoClient = cfg.mongoClient();
			MongoCollection<Document> collection = mongoClient.getDatabase(cfg.dbName).getCollection("Issuer");
			try {
				docTmp =   collection.aggregate(pipeline).allowDiskUse(true).iterator().next();		
			} catch (Exception e) {
				// TODO: handle exception
			}
				
			mongoClient.close();
		
			if(null == docTmp) {
				responseStatus = new MspResponseStatus(9999, "Không tìm thấy thông tin khách hàng.");
				rsp.setResponseStatus(responseStatus);
				return rsp;
			}
			 link = docTmp.getEmbedded(Arrays.asList("PramLink", "LinkLogin"), "");
			if(docTmp.get("ConfigEmail") != null) {
				 mailConfig = new MailConfig(docTmp.get("ConfigEmail", Document.class));
					mailConfig.setNameSend(docTmp.getEmbedded(Arrays.asList("UserName"), ""));
			}
		
			if(docTmp.get("DMCustomer") != null) {
				responseStatus = new MspResponseStatus(9999, "MST hoặc Mã khách hàng đã tồn tại trong hệ thống.");
				rsp.setResponseStatus(responseStatus);
				return rsp;
			}
		
			
			
			docUpsert = new Document("IssuerId", header.getIssuerId())
					.append("TaxCode", taxCode)
					.append("CustomerCode", customerCode)
					.append("CompanyName", companyName)
					.append("CustomerName", customerName)
					.append("Address", address)
					.append("Email", email)
					.append("EmailCC", emailcc)
					.append("Phone", phone)
					.append("Fax", fax)
					.append("Website", website)
					.append("Province", 
						new Document("Code", province).append("Name", provinceName)
					)
					.append("AccountNumber", accountNumber)
					.append("AccountBankName", accountBankName)
					.append("CustomerGroup1", 
						new Document("Code", customerGroup1).append("Name", customerGroup1Name)
					)
					.append("CustomerGroup2", 
						new Document("Code", customerGroup2).append("Name", customerGroup2Name)
					)
					.append("CustomerGroup3", 
						new Document("Code", customerGroup3).append("Name", customerGroup3Name)
					)
					.append("Remark", remark)
					.append("IsDelete", false)
					.append("InfoCreated", 
						new Document("CreateDate", LocalDateTime.now())
						.append("CreateUserID", header.getUserId())
						.append("CreateUserName", header.getUserName())
						.append("CreateUserFullName", header.getUserFullName())
					);
			
		
			
			mongoClient = cfg.mongoClient();
			collection = mongoClient.getDatabase(cfg.dbName).getCollection("DMCustomer");
			collection.insertOne(docUpsert);			
			mongoClient.close();
			
			if( Boxmail.equals("on")) {
			if(!taxCode.equals("") &&  !taxCode.startsWith("CN") && !email.equals("")) {
				docTmp = null;
				docFind = new Document("UserName", taxCode).append("IsActive", true).append("IsDelete", new Document("$ne", true));
				pipeline = new ArrayList<Document>();
				pipeline.add(new Document("$match", docFind));
				pipeline.add(new Document("$project", new Document("_id", 1)));
				
				
				 mongoClient = cfg.mongoClient();
				 collection = mongoClient.getDatabase(cfg.dbName).getCollection("Issuer");
				 try {
						docTmp =   collection.aggregate(pipeline).allowDiskUse(true).iterator().next();			
				} catch (Exception e) {
					// TODO: handle exception
				}
				mongoClient.close();
				
				if(null != docTmp) {	
					responseStatus = new MspResponseStatus(0, "SUCCESS");
					rsp.setResponseStatus(responseStatus);
					return rsp;
				}
				String 	secureKey = commons.csRandomString(15);
				String password = commons.encryptThisString(header.getUserName()+"_"+taxCode + secureKey);	
				  int size = password.length();
			if(size < 128) {	
					  password = "0"+password;
			}
			
			docTmp = null;
			docFind = new Document("TaxCode", taxCode).append("IsActive", true).append("IsDelete", new Document("$ne", true));
			pipeline = new ArrayList<Document>();
			pipeline.add(new Document("$match", docFind));
			pipeline.add(new Document("$project", new Document("_id", 1)));
			
			 mongoClient = cfg.mongoClient();
			 collection = mongoClient.getDatabase(cfg.dbName).getCollection("Issuer");
			 try {
					docTmp =   collection.aggregate(pipeline).allowDiskUse(true).iterator().next();			
			} catch (Exception e) {
				// TODO: handle exception
			}
			mongoClient.close();

			if (null != docTmp) {
				responseStatus = new MspResponseStatus(9999, "Tài khoản đã tồn tại");
				rsp.setResponseStatus(responseStatus);
				return rsp;
			}
				Document docUpsertUser = null;
				ObjectId idUser = new ObjectId();
				ObjectId idIssu = new ObjectId();
				String agentid   = "61a328076f5d4d9a6bed2dfa";
				/*LUU DU LIEU*/
				docUpsert = new Document("_id", idIssu)
					.append("TaxCode", header.getUserName()+"_"+taxCode)
					.append("AgentId", agentid)
					.append("Name", companyName)
					.append("Address", address)
					.append("Phone",phone)
					.append("Email", email)				
					.append("IsRoot", false)
					.append("IsDelete", false)
					.append("IsUserHDDT", false)
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
				
				
				
				docUpsertUser = new Document("_id", idUser)
						.append("IssuerId", idIssu.toString())
						.append("UserName", header.getUserName()+"_"+taxCode)
						.append("Password", password)
						.append("FullName",companyName)
						.append("Phone", phone)
						.append("Email", email)
						.append("IsKH", true)
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
				
					/*THUC HIEN GUI MAIL*/
					String _title = header.getUserFullName() + " Thông báo phát hành tài khoản";
					String _tmp = companyName;
					String tk = header.getUserName()+"_"+taxCode;
					String _content;
				
					StringBuilder sb = new StringBuilder();
					sb.setLength(0);
					sb.append("<p><span style='font-family: Times New Roman;font-size: 13px;'>Kính gửi: <label style='font-weight: bold;'>" + ("".equals(_tmp)? "Quý khách hàng": _tmp) + "</label><o:p></o:p></span></p>\n");
					sb.append("<p><span style='font-family: Times New Roman;font-size: 13px;'><label style='font-weight: bold;'>" + _title + "</label> Xin chân thành cảm ơn Quý đơn vị đã tin tưởng và sử dụng sản phẩm/dịch vụ của chúng tôi!</span></p>\n");
					sb.append("<p><span style='font-family: Times New Roman;font-size: 13px;'>Chúng tôi xin gửi tài khoản  tra cứu hóa đơn điện tử đến Quý đơn vị với thông tin như sau:</span></p>\n");
					sb.append("<p><span style='font-family: Times New Roman;font-size: 13px;'>1.  Tài khoản:  " + tk + "</span></p>\n");
					sb.append("<p><span style='font-family: Times New Roman;font-size: 13px;'>2.  Mật khẩu:  " + secureKey + "</span></p>\n");
					sb.append("<p><span style='font-family: Times New Roman;font-size: 13px;'>3.  Link đăng nhập:  " + link + "</span></p>\n");
					sb.append("<p><span style='font-family: Times New Roman;font-size: 13px;'>Quý đơn vị  vui lòng bảo mật thông tin và thay đổi mật khẩu sau khi đăng nhập.</span></p>\n");
					sb.append("<p><span style='font-family: Times New Roman;font-size: 13px;'>Trân trọng kính chào!</span></p>");
					sb.append("<hr style='margin: 5px 0 5px 0;'>");
					sb.append("<p style='margin-bottom: 3px;'><span style='font-family: Times New Roman;font-size: 13px;color:red;font-weight: bold;'>QUÝ CÔNG TY VUI LÒNG KHÔNG REPLY EMAIL NÀY!</span></p>");
					 _content = sb.toString();

					boolean boo = mailUtils.sendMail(mailConfig, _title, _content, email, null, null, true);
					try {
						
						mongoClient = cfg.mongoClient();
						collection = mongoClient.getDatabase(cfg.dbName).getCollection("LogEmail");
						collection.insertOne(
								new Document("IssuerId", header.getIssuerId())
								.append("UserName", taxCode)
								.append("PassWord", secureKey)
								.append("FullName", companyName)
								.append("Date", LocalDateTime.now())
								.append("EmailLH", email)
								.append("IsActive", true)
								.append("IsDelete", false)
								.append("EmailContent", _content)
							);			
						mongoClient.close();
						
					}catch(Exception ex) {}
				
			}
			else {
				responseStatus = new MspResponseStatus(9999, "Không tạo tài khoản cho khách cá nhân");
				rsp.setResponseStatus(responseStatus);
				return rsp;
			}
			}
		
			
			
			
			
			HashMap<String, Object> hR = new HashMap<String, Object>();
			hR.put("CustomerCode", customerCode);
			
			responseStatus = new MspResponseStatus(0, "SUCCESS");
			rsp.setResponseStatus(responseStatus);
			rsp.setObjData(hR);
			return rsp;
		case Constants.MSG_ACTION_CODE.MODIFY:
			objectId = null;
			try {
				objectId = new ObjectId(_id);
			}catch(Exception e) {}
			Document docFindmail = new Document("IssuerId",  header.getIssuerId());
			docFind = new Document("IssuerId", header.getIssuerId())
					.append("IsDelete", new Document("$ne", true))
					.append("_id", objectId);
			pipeline = new ArrayList<Document>();
			pipeline.add(new Document("$match", docFind));
			pipeline.add(new Document("$project", new Document("_id", 1).append("UserName", 1).append("TaxCode", 1).append("InfoCreated", 1)));
			
			
			pipeline.add(
					new Document("$lookup", 
						new Document("from", "ConfigEmail")
						.append("pipeline", 
							Arrays.asList(
								new Document("$match", 
										docFindmail
								)
							)	
						)
						.append("as", "ConfigEmail")
					)
				);
				pipeline.add(
					new Document("$unwind", new Document("path", "$ConfigEmail").append("preserveNullAndEmptyArrays", true))
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
	
			
			 mongoClient = cfg.mongoClient();
			 collection = mongoClient.getDatabase(cfg.dbName).getCollection("DMCustomer");
			 try {
					docTmp =   collection.aggregate(pipeline).allowDiskUse(true).iterator().next();			
			} catch (Exception e) {
				// TODO: handle exception
			}
			mongoClient.close();
			
			if(null == docTmp) {
				responseStatus = new MspResponseStatus(9999, "Không tìm thấy thông tin khách hàng.");
				rsp.setResponseStatus(responseStatus);
				return rsp;
			}
			 link = docTmp.getEmbedded(Arrays.asList("PramLink", "LinkLogin"), "");
			 mailConfig = new MailConfig();
			if(docTmp.get("ConfigEmail") != null) {
				 mailConfig = new MailConfig(docTmp.get("ConfigEmail", Document.class));
					mailConfig.setNameSend(docTmp.getEmbedded(Arrays.asList("UserName"), ""));
			}
			
			String tknmua = docTmp.getEmbedded(Arrays.asList("InfoCreated", "CreateUserName"), "")+"_"+docTmp.get("TaxCode");
			Document docTmpcheckuser = null;
		Document docFindcheckuser = new Document("IsDelete", new Document("$ne", true))
					.append("UserName", tknmua);
			pipeline = new ArrayList<Document>();
			pipeline.add(new Document("$match", docFindcheckuser));
			pipeline.add(new Document("$project", new Document("_id", 1)));
			
			 mongoClient = cfg.mongoClient();
			 collection = mongoClient.getDatabase(cfg.dbName).getCollection("Users");
			 try {
				 docTmpcheckuser =   collection.aggregate(pipeline).allowDiskUse(true).iterator().next();			
			} catch (Exception e) {
				// TODO: handle exception
			}
			mongoClient.close();
			
			
			if(null == docTmpcheckuser) {
				if( Boxmail.equals("on")) {
					if(!taxCode.equals("") &&  !taxCode.startsWith("CN") && !email.equals("")) {
						docTmpcheckuser = null;
						docFindcheckuser = new Document("UserName", tknmua).append("IsActive", true).append("IsDelete", new Document("$ne", true));
						pipeline = new ArrayList<Document>();
						pipeline.add(new Document("$match", docFindcheckuser));
						pipeline.add(new Document("$project", new Document("_id", 1)));
						
						 mongoClient = cfg.mongoClient();
						 collection = mongoClient.getDatabase(cfg.dbName).getCollection("Users");
						 try {
							 docTmpcheckuser =   collection.aggregate(pipeline).allowDiskUse(true).iterator().next();			
						} catch (Exception e) {
							// TODO: handle exception
						}
						mongoClient.close();
						
						if (null != docTmpcheckuser) {
							responseStatus = new MspResponseStatus(9999, "Tài khoản đã tồn tại");
							rsp.setResponseStatus(responseStatus);
							return rsp;
						}
						String 	secureKey = commons.csRandomString(15);
						String password = commons.encryptThisString(header.getUserName()+"_"+taxCode + secureKey);	
						  int size = password.length();
					if(size < 128) {	
							  password = "0"+password;
					}
					
					docTmpcheckuser = null;
					docFindcheckuser = new Document("TaxCode", tknmua).append("IsActive", true).append("IsDelete", new Document("$ne", true));
					pipeline = new ArrayList<Document>();
					pipeline.add(new Document("$match", docFindcheckuser));
			
					
					 mongoClient = cfg.mongoClient();
					 collection = mongoClient.getDatabase(cfg.dbName).getCollection("Issuer");
					 try {
						 docTmpcheckuser =   collection.aggregate(pipeline).allowDiskUse(true).iterator().next();			
					} catch (Exception e) {
						// TODO: handle exception
					}
					mongoClient.close();
					
					
					if (null != docTmpcheckuser) {
						responseStatus = new MspResponseStatus(9999, "Tài khoản đã tồn tại");
						rsp.setResponseStatus(responseStatus);
						return rsp;
					}
						Document docUpsertUser = null;
						ObjectId idUser = new ObjectId();
						ObjectId idIssu = new ObjectId();
						String agentid   = "61a328076f5d4d9a6bed2dfa";
						/*LUU DU LIEU*/
						docUpsert = new Document("_id", idIssu)
							.append("TaxCode", header.getUserName()+"_"+taxCode)
							.append("AgentId", agentid)
							.append("Name", companyName)
							.append("Address", address)
							.append("Phone",phone)
							.append("Email", email)
						
							.append("IsRoot", false)
							.append("IsDelete", false)
						
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
						
						
						docUpsertUser = new Document("_id", idUser)
								.append("IssuerId", idIssu.toString())
								.append("UserName", header.getUserName()+"_"+taxCode)
								.append("Password", password)
								.append("FullName",companyName)
								.append("Phone", phone)
								.append("Email", email)
								.append("IsKH", true)
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
						

							/*THUC HIEN GUI MAIL*/
							String _title = header.getUserFullName() + " Thông báo phát hành tài khoản";
							String _tmp = companyName;
							String tk = header.getUserName()+"_"+taxCode;
							String _content;
						
							StringBuilder sb = new StringBuilder();
							sb.setLength(0);
							sb.append("<p><span style='font-family: Times New Roman;font-size: 13px;'>Kính gửi: <label style='font-weight: bold;'>" + ("".equals(_tmp)? "Quý khách hàng": _tmp) + "</label><o:p></o:p></span></p>\n");
							sb.append("<p><span style='font-family: Times New Roman;font-size: 13px;'><label style='font-weight: bold;'>" + _title + "</label> Xin chân thành cảm ơn Quý đơn vị đã tin tưởng và sử dụng sản phẩm/dịch vụ của chúng tôi!</span></p>\n");
							sb.append("<p><span style='font-family: Times New Roman;font-size: 13px;'>Chúng tôi xin gửi tài khoản  tra cứu hóa đơn điện tử đến Quý đơn vị với thông tin như sau:</span></p>\n");
							sb.append("<p><span style='font-family: Times New Roman;font-size: 13px;'>1.  Tài khoản:  " + tk + "</span></p>\n");
							sb.append("<p><span style='font-family: Times New Roman;font-size: 13px;'>2.  Mật khẩu:  " + secureKey + "</span></p>\n");
							sb.append("<p><span style='font-family: Times New Roman;font-size: 13px;'>3.  Link đăng nhập:  " + link + "</span></p>\n");
							sb.append("<p><span style='font-family: Times New Roman;font-size: 13px;'>Quý đơn vị  vui lòng bảo mật thông tin và thay đổi mật khẩu sau khi đăng nhập.</span></p>\n");
							sb.append("<p><span style='font-family: Times New Roman;font-size: 13px;'>Trân trọng kính chào!</span></p>");
							sb.append("<hr style='margin: 5px 0 5px 0;'>");
							sb.append("<p style='margin-bottom: 3px;'><span style='font-family: Times New Roman;font-size: 13px;color:red;font-weight: bold;'>QUÝ CÔNG TY VUI LÒNG KHÔNG REPLY EMAIL NÀY!</span></p>");
							 _content = sb.toString();

							boolean boo = mailUtils.sendMail(mailConfig, _title, _content, email, null, null, true);
							try {

								mongoClient = cfg.mongoClient();
								collection = mongoClient.getDatabase(cfg.dbName).getCollection("LogEmail");
								collection.insertOne(
										new Document("IssuerId", header.getIssuerId())
										.append("UserName", taxCode)
										.append("PassWord", secureKey)
										.append("FullName", companyName)
										.append("Date", LocalDateTime.now())
										.append("EmailLH", email)
										.append("IsActive", true)
										.append("IsDelete", false)
										.append("EmailContent", _content)
										
									);			
								mongoClient.close();
								
								
							}catch(Exception ex) {}
						
					}
					else {
						responseStatus = new MspResponseStatus(9999, "Không tạo tài khoản cho khách cá nhân");
						rsp.setResponseStatus(responseStatus);
						return rsp;
					}
					}
			}
			
			
			
	
			
			options = new FindOneAndUpdateOptions();
			options.upsert(false);
			options.maxTime(5000, TimeUnit.MILLISECONDS);
			options.returnDocument(ReturnDocument.AFTER);
			
			
			mongoClient = cfg.mongoClient();
			collection = mongoClient.getDatabase(cfg.dbName).getCollection("DMCustomer");
			docR =	collection.findOneAndUpdate(
					docFind,
					new Document("$set", 
						new Document("CompanyName", companyName)
						.append("CustomerName", customerName)
						.append("Address", address)
						.append("Email", email)
						.append("EmailCC", emailcc)
						.append("Phone", phone)
						.append("Fax", fax)
						.append("Website", website)
						.append("Province", 
							new Document("Code", province).append("Name", provinceName)
						)
						.append("AccountNumber", accountNumber)
						.append("AccountBankName", accountBankName)
						.append("CustomerGroup1", 
							new Document("Code", customerGroup1).append("Name", customerGroup1Name)
						)
						.append("CustomerGroup2", 
							new Document("Code", customerGroup2).append("Name", customerGroup2Name)
						)
						.append("CustomerGroup3", 
							new Document("Code", customerGroup3).append("Name", customerGroup3Name)
						)
						.append("Remark", remark)
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
			rsp.setObjData(docTmp.get("_id"));
			return rsp;
		case Constants.MSG_ACTION_CODE.DELETE:
			List<ObjectId> objectIds = new ArrayList<ObjectId>();
			try {
				if(!jsonData.at("/ids").isMissingNode()) {
					for(JsonNode o: jsonData.at("/ids")) {
						try {
							objectIds.add(new ObjectId(o.asText("")));	
							
						}catch(Exception e) {}
					}
				}
			}catch(Exception e) {}
			
			UpdateOptions updateOptions = new UpdateOptions();
			updateOptions.upsert(false);
			
			docFind = new Document("IsDelete", new Document("$ne", true))
					.append("_id", new Document("$in", objectIds))
					.append("IssuerId", header.getIssuerId());
			
			
			mongoClient = cfg.mongoClient();
			collection = mongoClient.getDatabase(cfg.dbName).getCollection("DMCustomer");
			collection.updateMany(
					docFind
					, new Document("$set", 
						new Document("IsDelete", true)
						.append("InfoDeleted", 
							new Document("DeletedDate", LocalDateTime.now())
								.append("DeletedUserID", header.getUserId())
								.append("DeletedUserName", header.getUserName())
								.append("DeletedUserFullName", header.getUserFullName())
							)
					)
					, updateOptions);			
			mongoClient.close();
			
			
			responseStatus = new MspResponseStatus(0, "SUCCESS");
			rsp.setResponseStatus(responseStatus);
			return rsp;
		default:
			responseStatus = new MspResponseStatus(9998, Constants.MAP_ERROR.get(9998));
			rsp.setResponseStatus(responseStatus);
			return rsp;
		}
	}

/*
db.getCollection('DMCustomer').find({
    'IssuerId': '61b851ebb0228bba71fca2ec', IsDelete: {$ne: true}, _id: ObjectId("61d166fde69f8a2eb2b01e43")
});
 * */
	
	@Override
	public MsgRsp detail(JSONRoot jsonRoot, String _id) throws Exception {
		Msg msg = jsonRoot.getMsg();
		MsgHeader header = msg.getMsgHeader();
		MsgPage page = msg.getMsgPage();
		
		MsgRsp rsp = new MsgRsp(header);
		rsp.setMsgPage(page);
		MspResponseStatus responseStatus = null;
		
		ObjectId objectId = null;
		try {
			objectId = new ObjectId(_id);
		}catch(Exception e) {}
		
		Document docFind = new Document("IssuerId", header.getIssuerId())
				.append("IsDelete", new Document("$ne", true)).append("_id", objectId);
		
		Document docTmp = null;
	
		
		MongoClient mongoClient = cfg.mongoClient();
		MongoCollection<Document> collection = mongoClient.getDatabase(cfg.dbName).getCollection("DMCustomer");
		try {
			docTmp =   collection.find(docFind).allowDiskUse(true).iterator().next();		
		} catch (Exception e) {
			// TODO: handle exception
		}
			
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

	@Override
	public MsgRsp importExcel(JSONRoot jsonRoot) throws Exception {
		Msg msg = jsonRoot.getMsg();
		MsgHeader header = msg.getMsgHeader();
		MsgPage page = msg.getMsgPage();
		Object objData = msg.getObjData();
		
		MsgRsp rsp = new MsgRsp(header);
		rsp.setMsgPage(page);
		MspResponseStatus responseStatus = null;
		
		JsonNode jsonData = null;
		if(objData != null) {
			jsonData = Json.serializer().nodeFromObject(msg.getObjData());
		}else{
			throw new Exception("Lỗi dữ liệu đầu vào");
		}
		
		String dataFileName = commons.getTextJsonNode(jsonData.at("/DataFileName")).replaceAll("\\s", "");
		Path path = Paths.get(SystemParams.DIR_TEMPORARY, header.getIssuerId(), dataFileName);
		File file = path.toFile();
		if(!(file.exists() && file.isFile())) {
			responseStatus = new MspResponseStatus(9999, "Tập tin import dữ liệu không tồn tại.");
			rsp.setResponseStatus(responseStatus);
			return rsp;
		}
		
		Workbook wb = null;
		Sheet sheet = null;
		Row row = null;
		Cell cell = null;
		try {
			wb = WorkbookFactory.create(file);
			sheet = wb.getSheetAt(0);
		}catch(Exception e) {}
		
		if(sheet == null) {
			responseStatus = new MspResponseStatus(9999, "Không tìm thấy dữ liệu import trong file dữ liệu.");
			rsp.setResponseStatus(responseStatus);
			return rsp;
		}
		
		FormulaEvaluator formulaEvaluator = wb.getCreationHelper().createFormulaEvaluator();
		
		StringBuilder sb = new StringBuilder();
		List<HashMap<String, String>> datas = new ArrayList<HashMap<String,String>>();
		HashMap<String, String> hItem = null;
		
		/*DOC FILE EXCEL - GHI DU LIEU VO LIST*/
		Iterator<Row> rows = sheet.rowIterator();
		while (rows.hasNext()) {
			row = rows.next();
			if(row.getRowNum() < 1) continue;
			
			sb.setLength(0);
			sb.append(commons.getCellValue(formulaEvaluator, row.getCell(0)));
			if("".equals(sb.toString())) continue;
			
			try {
				hItem = new HashMap<String, String>();
				hItem.put("Mst", commons.getCellValue(formulaEvaluator, row.getCell(0)));
				hItem.put("NameCty", commons.getCellValue(formulaEvaluator, row.getCell(1)));
				hItem.put("Name", commons.getCellValue(formulaEvaluator, row.getCell(2)));
				hItem.put("Adress", commons.getCellValue(formulaEvaluator, row.getCell(3)));
				hItem.put("Phone", commons.getCellValue(formulaEvaluator, row.getCell(4)).trim().replaceAll("\\s+", " "));
				hItem.put("Email", commons.getCellValue(formulaEvaluator, row.getCell(5)).trim().replaceAll("\\s+", " "));
				hItem.put("EmailCC", commons.getCellValue(formulaEvaluator, row.getCell(6)).trim().replaceAll("\\s+", " "));			
				datas.add(hItem);
			}catch(Exception e) {
			}
		}
		
		if(datas.size() == 0) {
			responseStatus = new MspResponseStatus(9999, "Không tồn tại dữ liệu cần import");
			rsp.setResponseStatus(responseStatus);
			return rsp;
		}		
		Document docFilter = null;

		String 	customerCode = "MCK" + commons.convertLocalDateTimeToString(LocalDate.now(), "yyyyMMdd") + "-" + commons.csRandomAlphaNumbericString(3).toUpperCase();
		List<WriteModel<Document>> ous = new ArrayList<WriteModel<Document>>();
		BulkWriteResult r = null;
		UpdateOptions uo = new UpdateOptions();
		uo.upsert(true);
		
		for(HashMap<String, String> hO: datas) {
			String MST = hO.get("Mst").trim().replaceAll("\\s+", "").replaceAll("[+^%$#@&*]*", "").replaceAll("[a-z][A-Z]*", "");
			docFilter = new Document("IssuerId", header.getIssuerId()).append("TaxCode", hO.get("Mst"));
			ous.add(
				new UpdateOneModel<>(
					docFilter, 
					new Document("$set", 
							new Document("IssuerId", header.getIssuerId())
						.append("TaxCode", MST)
						.append("CompanyName", hO.get("NameCty"))
						.append("CustomerName", hO.get("Name"))
						.append("CustomerCode",customerCode)
						.append("Address", hO.get("Adress"))
						.append("Phone", hO.get("Phone"))
						.append("Email", hO.get("Email"))
						.append("EmailCC", hO.get("EmailCC"))
						.append("IsDelete", false)
					).append("$setOnInsert", 
						new Document("InfoCreated", 
								new Document("CreateDate", LocalDateTime.now())
								.append("CreateUserID", header.getUserId())
								.append("CreateUserName", header.getUserName())
								.append("CreateUserFullName", header.getUserFullName())
							)
					)
					,
					uo)
			);
		}
	
		
		MongoClient mongoClient = cfg.mongoClient();
		MongoCollection<Document> collection = mongoClient.getDatabase(cfg.dbName).getCollection("DMCustomer");
		try {
			r =   collection.bulkWrite(
					ous,
					new BulkWriteOptions().ordered(false)
				);	
		} catch (Exception e) {
			// TODO: handle exception
		}
			
		mongoClient.close();
		
		responseStatus = new MspResponseStatus(0, "SUCCESS");
		rsp.setResponseStatus(responseStatus);
		return rsp;
	}


}
