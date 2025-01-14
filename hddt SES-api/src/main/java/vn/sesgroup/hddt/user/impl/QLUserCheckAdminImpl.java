package vn.sesgroup.hddt.user.impl;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
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
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.FindOneAndUpdateOptions;
import com.mongodb.client.model.ReturnDocument;

import vn.sesgroup.hddt.configuration.ConfigConnectMongo;
import vn.sesgroup.hddt.user.dao.AbstractDAO;
import vn.sesgroup.hddt.user.dao.QLUserCheckAdminDAO;
import vn.sesgroup.hddt.user.service.JPUtils;
import vn.sesgroup.hddt.user.service.TCTNService;
import vn.sesgroup.hddt.utility.Constants;
import vn.sesgroup.hddt.utility.Json;

@Repository
@Transactional
public class QLUserCheckAdminImpl extends AbstractDAO implements QLUserCheckAdminDAO {
	private static final Logger log = LogManager.getLogger(QLUserCheckAdminImpl.class);
	@Autowired ConfigConnectMongo cfg;
	@Autowired TCTNService tctnService;

	@Autowired
	JPUtils jpUtils;
	Document docUpsert = null;
	
public MsgRsp list(JSONRoot jsonRoot) throws Exception {
		Msg msg = jsonRoot.getMsg();
		MsgHeader header = msg.getMsgHeader();
		MsgPage page = msg.getMsgPage();
		Object objData = msg.getObjData();
		
		String userName = "";
		String fullName = "";

		JsonNode jsonData = null;
		if (objData != null) {
			jsonData = Json.serializer().nodeFromObject(objData);
			
			userName = commons.getTextJsonNode(jsonData.at("/UserName")).replaceAll("\\s", "").toUpperCase();
			fullName = commons.getTextJsonNode(jsonData.at("/FullName")).trim().replaceAll("\\s+", " ");
	
		}
				
		MsgRsp rsp = new MsgRsp(header);
		MspResponseStatus responseStatus = null;

		ObjectId objectId = null;
		Document docTmp = null;
		List<Document> pipeline = new ArrayList<Document>();
		Document docSubTmp = null;

		Document docMatch = new Document("IsDelete",
				new Document("$ne", true))
//				.append("IssuerId", header.getIssuerId())
				.append("IsCheck", true);
		
		if (!"".equals(fullName))
			docMatch.append("FullName",
					new Document("$regex", commons.regexEscapeForMongoQuery(fullName)).append("$options", "i"));
		if (!"".equals(userName))
			docMatch.append("UserName",
					new Document("$regex", commons.regexEscapeForMongoQuery(userName)).append("$options", "i"));
	
		pipeline = new ArrayList<Document>();
		pipeline.add(new Document("$match", docMatch));		
		
		pipeline.add(new Document("$sort",
				new Document("_id", -1)					
				));
		
		pipeline.add(new Document("$project",
				new Document("_id", -1).append("IssuerId", 1).append("UserName", 1).append("FullName", 1).append("Phone", 1)			
				.append("Email", 1).append("IsActive", 1).append("IsRoot", 1).append("InfoCreated", 1).append("InfoUpdated", 1)
				));
		
		pipeline.addAll(createFacetForSearchNotSort(page));
		
		MongoClient mongoClient = cfg.mongoClient();
		MongoCollection<Document> collection = mongoClient.getDatabase(cfg.dbName).getCollection("Users");
		      try {
		        docTmp = collection.aggregate(pipeline).allowDiskUse(true).iterator().next();    
		      } catch (Exception e) {
		        
		      }
		        
		mongoClient.close();
		
		rsp = new MsgRsp(header);
		responseStatus = null;
		
		if (null == docTmp) {
			responseStatus = new MspResponseStatus(9999, Constants.MAP_ERROR.get(9999));
			rsp.setResponseStatus(responseStatus);
			return rsp;
		}
		
		page.setTotalRows(docTmp.getInteger("total", 0));
		rsp.setMsgPage(page);

		ArrayList<HashMap<String, Object>> rowsReturn = new ArrayList<HashMap<String, Object>>();
		
		List<Document> rows = null;
		
		if(docTmp!=null) {
		if (docTmp.get("data") != null && docTmp.get("data") instanceof List) {
			rows = docTmp.getList("data", Document.class);
		}


		HashMap<String, Object> hItem = null;
		if (null != rows) {
			for (Document doc : rows) {
				objectId = (ObjectId) doc.get("_id");

				String issuer = doc.get("IssuerId", "");
				
				
				String NameIssuer = "";
				if(!issuer.equals("")) {
					ObjectId objectIssu = new ObjectId(issuer);		
					docTmp = null;
					Document findIssuer = new Document("_id" , objectIssu).append("IsDelete", new Document("$ne", true));
					
					pipeline = new ArrayList<Document>();
					pipeline.add(new Document("$match", findIssuer));
					pipeline.add(new Document("$project", new Document("_id", 1).append("Name", 1)));
					
					mongoClient = cfg.mongoClient();
					collection = mongoClient.getDatabase(cfg.dbName).getCollection("Issuer");
					  try {
					         docTmp = collection.aggregate(pipeline).allowDiskUse(true).iterator().next();      
					     } catch (Exception e) {
					        
					    }
					mongoClient.close();
					
					NameIssuer = docTmp.get("Name", "");
				}

				hItem = new HashMap<String, Object>();
				hItem.put("_id", objectId.toString());
				hItem.put("UserName", doc.getString("UserName"));
				
				hItem.put("NameIssuer", NameIssuer);
				
				
				hItem.put("FullName", doc.getString("FullName"));
				hItem.put("Phone", doc.getString("Phone"));
				hItem.put("Email", doc.getString("Email"));
				hItem.put("IsActive", doc.getBoolean("IsActive", false));					
				hItem.put("IsRoot", doc.getBoolean("IsRoot", false));
				
				if(null != doc.get("InfoCreated") && doc.get("InfoCreated") instanceof Document) {
					docSubTmp = (Document) doc.get("InfoCreated");
					hItem.put("CreateDate", (null == docSubTmp.get("CreateDate") || !(docSubTmp.get("CreateDate") instanceof Date))?
							"":
							commons.convertLocalDateTimeToString(commons.convertDateToLocalDateTime(docSubTmp.getDate("CreateDate")), Constants.FORMAT_DATE.FORMAT_DATE_TIME_WEB)
						);
					hItem.put("CreateUserFullName", null == docSubTmp.get("CreateUserFullName")? "": docSubTmp.getString("CreateUserFullName"));
				}
				if(null != doc.get("InfoUpdated") && doc.get("InfoUpdated") instanceof Document) {
					docSubTmp = (Document) doc.get("InfoUpdated");
					hItem.put("UpdatedDate", (null == docSubTmp.get("UpdatedDate") || !(docSubTmp.get("UpdatedDate") instanceof Date))?
							"":
							commons.convertLocalDateTimeToString(commons.convertDateToLocalDateTime(docSubTmp.getDate("UpdatedDate")), Constants.FORMAT_DATE.FORMAT_DATE_TIME_WEB)
						);
					hItem.put("UpdatedUserFullName", null == docSubTmp.get("UpdatedUserFullName")? "": docSubTmp.getString("UpdatedUserFullName"));
				}

				rowsReturn.add(hItem);
			}
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
		ObjectId objectId = null;
		FindOneAndUpdateOptions options = null;		
		Document docTmp = null;
		Document docTmpSub = null;
		String user = "";
		String password = "";
		String passwordInput = "";
		
		Document docFind = null;
		
		boolean IsActive = false;
		List<Document> pipeline = null;
		
		JsonNode jsonData = null;
		if (objData != null) {
			jsonData = Json.serializer().nodeFromObject(msg.getObjData());
		} else {
			throw new Exception("Lỗi dữ liệu đầu vào");
		}
		
		MsgRsp rsp = new MsgRsp(header);
		rsp.setMsgPage(page);
		MspResponseStatus responseStatus = null;

		String actionCode = header.getActionCode();
		
		String _id = commons.getTextJsonNode(jsonData.at("/_id")).replaceAll("\\s", "");	
		String userName = commons.getTextJsonNode(jsonData.at("/UserName")).trim().replaceAll("\\s+", " ").toUpperCase();
		String fullName = commons.getTextJsonNode(jsonData.at("/FullName")).trim().replaceAll("\\s+", " ");
		String phone = commons.getTextJsonNode(jsonData.at("/Phone")).trim().replaceAll("\\s+", " ");
		String email = commons.getTextJsonNode(jsonData.at("/Email")).trim().replaceAll("\\s+", " ");		
		
		switch (actionCode) {
		case Constants.MSG_ACTION_CODE.CREATED:
			try {
				objectId = new ObjectId(header.getIssuerId());
			}catch(Exception e) {}
			
			pipeline = new ArrayList<Document>();
			pipeline.add(
				new Document("$match", 
					new Document("UserName", "USER_CHECK"+ "_"+ userName)
					.append("IsDelete", new Document("$ne", true))
				)
			);
			pipeline.add(new Document("$project", new Document("_id",1)));

			docTmp = null;
			
			MongoClient mongoClient = cfg.mongoClient();
			MongoCollection<Document> collection = mongoClient.getDatabase(cfg.dbName).getCollection("Users");
			      try {
			        docTmp = collection.aggregate(pipeline).allowDiskUse(true).iterator().next();    
			      } catch (Exception e) {
			        
			      }
			        
			mongoClient.close();
			
			if(null != docTmp) {
				responseStatus = new MspResponseStatus(9999, "Tên đăng nhập đã tồn tại.");
				rsp.setResponseStatus(responseStatus);
				return rsp;
			}

			
			String prefixUserID = "USER_CHECK";
			user = prefixUserID + "_" + userName;
			password = commons.csRandomNumbericString(8);
			
			passwordInput = commons.encryptThisString(user + password);	
			int size = passwordInput.length();
		if(size < 128) {
				password = commons.csRandomString(8);
				passwordInput = commons.encryptThisString(user + password);	
		}
		  int size2 = passwordInput.length();
			if(size2 < 128) {
				password = commons.csRandomString(8);
				passwordInput = commons.encryptThisString(user + password);	
			}
			 
			docTmp = new Document("UserName", user)
					.append("Password", passwordInput)
					.append("FullName", fullName)
					.append("Phone", phone)
					.append("Email", email)
					.append("IssuerId", "")
					.append("IsActive", true)
					.append("IsDelete", false)
					.append("IsCheck", true);
		
			docTmpSub = new Document("CreateDate", LocalDateTime.now())
					.append("CreateUserID", header.getUserId())
					.append("CreateUserName", header.getUserName())
					.append("CreateUserFullName", header.getUserFullName());
			docTmp.append("InfoCreated", docTmpSub);
			
			mongoClient = cfg.mongoClient();
			collection = mongoClient.getDatabase(cfg.dbName).getCollection("Users");
			collection.insertOne(docTmp);      
			mongoClient.close();
			
			objectId = (ObjectId) docTmp.get("_id");
			HashMap<String, String> hR = new HashMap<String, String>();
			hR.put("_id", objectId.toString());
			hR.put("Password", password);
			hR.put("UserName", user);
			
			responseStatus = new MspResponseStatus(0, "SUCCESS");
			rsp.setResponseStatus(responseStatus);
			rsp.setObjData(hR);
			return rsp;
	
		case Constants.MSG_ACTION_CODE.MODIFY:
			
			objectId = null;
			try {
				objectId = new ObjectId(_id);
			}catch(Exception e) {}
			docFind = new Document("_id", objectId).append("IsDelete", new Document("$ne", true));
			
			pipeline = new ArrayList<Document>();
			pipeline.add(new Document("$match", docFind));
			pipeline.add(
				new Document("$project", new Document("_id", 1).append("IsActive", new Document("$ifNull", Arrays.asList("$IsActive", false))))
			);
			
			mongoClient = cfg.mongoClient();
			collection = mongoClient.getDatabase(cfg.dbName).getCollection("Users");
			  try {
			         docTmp = collection.aggregate(pipeline).allowDiskUse(true).iterator().next();      
			     } catch (Exception e) {
			        
			    }
			mongoClient.close();
			
			if(null == docTmp){
				responseStatus = new MspResponseStatus(99999, "Người dùng không tồn tại trong hệ thống.");
				rsp.setResponseStatus(responseStatus);
				return rsp;
			}
			IsActive = false;
			if(null != docTmp.get("IsActive") && docTmp.get("IsActive") instanceof Boolean) {
				IsActive = docTmp.getBoolean("IsActive", false);
			}
			if(IsActive) {
				responseStatus = new MspResponseStatus(99999, "Người dùng đã được kích hoạt, không được phép thay đổi.");
				rsp.setResponseStatus(responseStatus);
				return rsp;
			}
			

			/*END - KIEM TRA THONG TIN NHAN VIEN*/
			
			options = new FindOneAndUpdateOptions();
			options.upsert(false);
			options.maxTime(5000, TimeUnit.MILLISECONDS);
			options.returnDocument(ReturnDocument.AFTER);		
			
			mongoClient = cfg.mongoClient();
			collection = mongoClient.getDatabase(cfg.dbName).getCollection("Users");
			collection.findOneAndUpdate(docFind,
					new Document("$set", 
							new Document("FullName", fullName)
								.append("Phone", phone)
								.append("Email", email)
								.append("InfoUpdated", 
										new Document("UpdatedDate", LocalDateTime.now())
											.append("UpdatedUserID", header.getUserId())
											.append("UpdatedUserName", header.getUserName())
											.append("UpdatedUserFullName", header.getUserFullName())
										)									
								.append("UpdateDate", LocalDateTime.now())
								.append("UpdateUser", header.getUserId())
							
								
							)
					, options);
			  mongoClient.close();
			
			responseStatus = new MspResponseStatus(0, "SUCCESS");
			rsp.setResponseStatus(responseStatus);
			rsp.setObjData(docTmp.get("_id"));
			return rsp;
		
		case Constants.MSG_ACTION_CODE.ACTIVE:
		case Constants.MSG_ACTION_CODE.DELETE:
		case Constants.MSG_ACTION_CODE.RESET_PASSWORD:
			objectId = null;
			try {
				objectId = new ObjectId(_id);
			}catch(Exception e) {}
			/*KIEM TRA ID NHAN VIEN*/

			docFind = new Document("_id", objectId)
					.append("IsDelete", new Document("$ne", true));
			docTmp = null;
			
			pipeline = new ArrayList<Document>();
			pipeline.add(new Document("$match", docFind));
			pipeline.add(new Document("$project", new Document("_id", 1).append("IsActive", 1).append("UserName", 1)));
			
			mongoClient = cfg.mongoClient();
			collection = mongoClient.getDatabase(cfg.dbName).getCollection("Users");
			  try {
			         docTmp = collection.aggregate(pipeline).allowDiskUse(true).iterator().next();      
			     } catch (Exception e) {
			        
			    }
			mongoClient.close();
			
			if(null == docTmp){
				responseStatus = new MspResponseStatus(99999, "Người dùng không tồn tại trong hệ thống.");
				rsp.setResponseStatus(responseStatus);
				return rsp;
			}
			
			IsActive = false;
			if(null != docTmp.get("IsActive") && docTmp.get("IsActive") instanceof Boolean) {
				IsActive = docTmp.getBoolean("IsActive", false);
			}
			if(IsActive) {
				responseStatus = new MspResponseStatus(99999, "Người dùng đã được kích hoạt, không được phép thực hiện.");
				rsp.setResponseStatus(responseStatus);
				return rsp;
			}
			
			/*END - KIEM TRA ID NHAN VIEN*/
			options = new FindOneAndUpdateOptions();
			options.upsert(false);
			options.maxTime(5000, TimeUnit.MILLISECONDS);
			options.returnDocument(ReturnDocument.AFTER);
			
			if(Constants.MSG_ACTION_CODE.ACTIVE.equals(actionCode)) {
							
				mongoClient = cfg.mongoClient();
				collection = mongoClient.getDatabase(cfg.dbName).getCollection("Users");
				collection.findOneAndUpdate(docFind,
						new Document("$set", 
								new Document("IsActive", true)				
								.append("InfoActived", 
										new Document("ActivedDate", LocalDateTime.now())
											.append("ActivedUserID", header.getUserId())
											.append("ActivedUserName", header.getUserName())
											.append("ActivedUserFullName", header.getUserFullName())
										)
								.append("ActiveDate", LocalDateTime.now())
								.append("ActiveUser", header.getUserId())
								)
						, options);
				  mongoClient.close();
				
			}else if(Constants.MSG_ACTION_CODE.DELETE.equals(actionCode)){
				
				mongoClient = cfg.mongoClient();
				collection = mongoClient.getDatabase(cfg.dbName).getCollection("Users");
				collection.findOneAndUpdate(docFind,
						new Document("$set", 
								new Document("IsDelete", true)
								.append("InfoDeleted", 
										new Document("DeletedDate", LocalDateTime.now())
											.append("DeletedUserID", header.getUserId())
											.append("DeletedUserName", header.getUserName())
											.append("DeletedUserFullName", header.getUserFullName())
										)
								.append("DeleteDate", LocalDateTime.now())
								.append("DeleteUser", header.getUserId())
								)
						, options);
				  mongoClient.close();
			}else if(Constants.MSG_ACTION_CODE.RESET_PASSWORD.equals(actionCode)){
				//oldPasswordInSystem = docTmp.getString("Password");
				user = docTmp.getString("UserName");
				password = commons.csRandomNumbericString(8);
				
				passwordInput = commons.encryptThisString(user + password);	
				   size = passwordInput.length();
			if(size < 128) {
					password = commons.csRandomString(8);
					passwordInput = commons.encryptThisString(user + password);	
			}
			   size2 = passwordInput.length();
				if(size2 < 128) {
					password = commons.csRandomString(8);
					passwordInput = commons.encryptThisString(user + password);	
				}
				
				mongoClient = cfg.mongoClient();
				collection = mongoClient.getDatabase(cfg.dbName).getCollection("Users");
				collection.findOneAndUpdate(docFind,
						new Document("$set", 
								new Document("Password", passwordInput)
								.append("InfoResetPass", 
										new Document("ResetPassDate", LocalDateTime.now())
											.append("ResetPassUserID", header.getUserId())
											.append("ResetPassUserName", header.getUserName())
											.append("ResetPassUserFullName", header.getUserFullName())
										)
								)
						, options);
				  mongoClient.close();
				
				hR = new HashMap<String, String>();
				hR.put("_id", objectId.toString());
				hR.put("Password", password);
				
				responseStatus = new MspResponseStatus(0, "SUCCESS");
				rsp.setResponseStatus(responseStatus);
				rsp.setObjData(hR);
				return rsp;
			}
			
			responseStatus = new MspResponseStatus(0, "SUCCESS");
			rsp.setResponseStatus(responseStatus);
			rsp.setObjData(docTmp.get("_id"));
			return rsp;
		case Constants.MSG_ACTION_CODE.DEACTIVE:
			objectId = null;
			try {
				objectId = new ObjectId(_id);
			}catch(Exception e) {}
			/*KIEM TRA ID NHAN VIEN*/
			docFind = new Document("_id", objectId)
					.append("IsDelete", new Document("$ne", true));	
			docTmp = null;
		
			pipeline = new ArrayList<Document>();
			pipeline.add(new Document("$match", docFind));
			
			mongoClient = cfg.mongoClient();
			collection = mongoClient.getDatabase(cfg.dbName).getCollection("Users");
			  try {
			         docTmp = collection.aggregate(pipeline).allowDiskUse(true).iterator().next();      
			     } catch (Exception e) {
			        
			    }
			mongoClient.close();
			
			if(null == docTmp){
				responseStatus = new MspResponseStatus(99999, "Người dùng không tồn tại trong hệ thống.");
				rsp.setResponseStatus(responseStatus);
				return rsp;
			}
			IsActive = false;
			if(null != docTmp.get("IsActive") && docTmp.get("IsActive") instanceof Boolean) {
				IsActive = docTmp.getBoolean("IsActive", false);
			}
			if(!IsActive) {
				responseStatus = new MspResponseStatus(99999, "Người dùng chưa được kích hoạt, không được phép thực hiện.");
				rsp.setResponseStatus(responseStatus);
				return rsp;
			}
			/*END - KIEM TRA ID NHAN VIEN*/
			options = new FindOneAndUpdateOptions();
			options.upsert(false);
			options.maxTime(5000, TimeUnit.MILLISECONDS);
			options.returnDocument(ReturnDocument.AFTER);
			
			mongoClient = cfg.mongoClient();
			collection = mongoClient.getDatabase(cfg.dbName).getCollection("Users");
			collection.findOneAndUpdate(docFind,
					new Document("$set", 
							new Document("IsActive", false)
							.append("InfoDeActived", 
									new Document("DeActivedDate", LocalDateTime.now())
										.append("DeActivedUserID", header.getUserId())
										.append("DeActivedUserName", header.getUserName())
										.append("DeActivedUserFullName", header.getUserFullName())
									)
							.append("DeActiveDate", LocalDateTime.now())
							.append("DeActiveUser", header.getUserId())
							)
					, options);
			  mongoClient.close();
			
			responseStatus = new MspResponseStatus(0, "SUCCESS");
			rsp.setResponseStatus(responseStatus);
			rsp.setObjData(docTmp.get("_id"));
			return rsp;
			
			
		default:
			responseStatus = new MspResponseStatus(9998, Constants.MAP_ERROR.get(9998));
			rsp.setResponseStatus(responseStatus);
			return rsp;
		}
	}
	
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
		} catch (Exception e) {
		}

		List<Document> pipeline = new ArrayList<Document>();
		
		Document docFind = new Document("IsDelete", new Document("$ne", true))
				.append("IsCheck", true)				
				.append("_id", objectId);

		
		pipeline = new ArrayList<Document>();
		pipeline.add(new Document("$match", docFind));
		Document docTmp = null;
		
		MongoClient mongoClient = cfg.mongoClient();
		MongoCollection<Document> collection = mongoClient.getDatabase(cfg.dbName).getCollection("Users");
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

}
