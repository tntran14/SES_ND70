package vn.sesgroup.hddt.user.impl;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
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
import vn.sesgroup.hddt.user.dao.CreateUserDAO;
import vn.sesgroup.hddt.user.service.JPUtils;
import vn.sesgroup.hddt.user.service.TCTNService;
import vn.sesgroup.hddt.utility.Constants;
import vn.sesgroup.hddt.utility.Json;
import vn.sesgroup.hddt.utility.MailUtils;

@Repository
@Transactional
public class CreateUserImpl extends AbstractDAO implements CreateUserDAO {
	private static final Logger log = LogManager.getLogger(CreateUserImpl.class);
	@Autowired
	ConfigConnectMongo cfg;
	@Autowired
	TCTNService tctnService;
	private MailUtils mailUtils = new MailUtils();
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
		String phone = "";
		String email = "";

		JsonNode jsonData = null;
		if (objData != null) {
			jsonData = Json.serializer().nodeFromObject(objData);
			
			userName = commons.getTextJsonNode(jsonData.at("/UserName")).replaceAll("\\s", "").toUpperCase();
			fullName = commons.getTextJsonNode(jsonData.at("/FullName")).trim().replaceAll("\\s+", " ");
			phone = commons.getTextJsonNode(jsonData.at("/Phone")).trim().replaceAll("\\s+", " ");
			email = commons.getTextJsonNode(jsonData.at("/Email")).trim().replaceAll("\\s+", " ");
	
		}
				
		MsgRsp rsp = new MsgRsp(header);
		MspResponseStatus responseStatus = null;

		ObjectId objectId = null;
		Document docTmp = null;
		Iterable<Document> cursor = null;
		Iterator<Document> iter = null;
		List<Document> pipeline = new ArrayList<Document>();
		Document docSubTmp = null;

		Document docMatch = new Document("IsDelete",
				new Document("$ne", true))
				.append("IssuerId", header.getIssuerId())
				.append("IsRole", true)
				;
		
		if (!"".equals(fullName))
			docMatch.append("FullName",
					new Document("$regex", commons.regexEscapeForMongoQuery(fullName)).append("$options", "i"));
		if (!"".equals(userName))
			docMatch.append("UserName",
					new Document("$regex", commons.regexEscapeForMongoQuery(userName)).append("$options", "i"));
		if (!"".equals(phone))
			docMatch.append("Phone",
					new Document("$regex", commons.regexEscapeForMongoQuery(phone)).append("$options", "i"));
		if (!"".equals(email))
			docMatch.append("Email",
					new Document("$regex", commons.regexEscapeForMongoQuery(email)).append("$options", "i"));
	
		
		Document fillter = new Document("_id", 1).append("RolesRightManageInfo", 1).append("ExpireDate", 1)
				.append("ExpireDate", 1).append("EffectDate", 1).append("InfoUpdated", 1).append("InfoCreated", 1).append("IsRoot", 1)
				.append("IsActive", 1).append("Email", 1).append("Phone", 1).append("FullName", 1).append("UserName", 1);
				
		pipeline = new ArrayList<Document>();
		pipeline.add(new Document("$match", docMatch));		
		
		pipeline.add(new Document("$sort",
				new Document("_id", -1)					
				));
		pipeline.add(new Document("$project", fillter));
		pipeline.addAll(createFacetForSearchNotSort(page));

		
		MongoClient mongoClient = cfg.mongoClient();
		MongoCollection<Document> collection = mongoClient.getDatabase(cfg.dbName).getCollection("Users");
		try {
			docTmp =   collection.aggregate(pipeline).allowDiskUse(true).iterator().next();		
		} catch (Exception e) {
			// TODO: handle exception
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

				hItem = new HashMap<String, Object>();
				hItem.put("_id", objectId.toString());
				hItem.put("UserName", doc.getString("UserName"));
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
				
				hItem.put("EffectDate", (null == doc.get("EffectDate") || !(doc.get("EffectDate") instanceof Date))?
						"":
						commons.convertLocalDateTimeToString(commons.convertDateToLocalDateTime(doc.getDate("EffectDate")), Constants.FORMAT_DATE.FORMAT_DATE_WEB)
					);
				hItem.put("ExpireDate", (null == doc.get("ExpireDate") || !(doc.get("ExpireDate") instanceof Date))?
						"":
						commons.convertLocalDateTimeToString(commons.convertDateToLocalDateTime(doc.getDate("ExpireDate")), Constants.FORMAT_DATE.FORMAT_DATE_WEB)
					);
				hItem.put("RolesRightManageInfo", doc.get("RolesRightManageInfo"));
				
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
		Iterable<Document> cursor = null;
		Iterator<Document> iter = null;
		
		Document docTmpSub = null;
		Document docRolesRightManageInfo = null;
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
		String positionName = commons.getTextJsonNode(jsonData.at("/PositionName")).replaceAll("\\s", " ");
		String roleId = commons.getTextJsonNode(jsonData.at("/RoleId")).replaceAll("\\s", "");
		String effectDate = commons.getTextJsonNode(jsonData.at("/EffectDate")).replaceAll("\\s", "");
		String expireDate = commons.getTextJsonNode(jsonData.at("/ExpireDate")).replaceAll("\\s", "");
		String hasRetired = commons.getTextJsonNode(jsonData.at("/HasRetired")).replaceAll("\\s", "");
		
		LocalDate eff = LocalDate.now();
		LocalDate exp = LocalDate.now();
		if(!"".equals(effectDate) && commons.checkLocalDate(effectDate, Constants.FORMAT_DATE.FORMAT_DATE_WEB))
			eff = commons.convertStringToLocalDate(effectDate, Constants.FORMAT_DATE.FORMAT_DATE_WEB);
		if(!"".equals(expireDate) && commons.checkLocalDate(expireDate, Constants.FORMAT_DATE.FORMAT_DATE_WEB))
			exp = commons.convertStringToLocalDate(expireDate, Constants.FORMAT_DATE.FORMAT_DATE_WEB);
		

		switch (actionCode) {
		case Constants.MSG_ACTION_CODE.CREATED:
			try {
				objectId = new ObjectId(header.getIssuerId());
			}catch(Exception e) {}
			
			pipeline = new ArrayList<Document>();
			pipeline.add(
				new Document("$match", 
					new Document("_id", objectId).append("IsActive", true)
				)
			);
			pipeline.add(
				new Document("$project", new Document("_id", new Document("$toString", "$_id")))
			);
			pipeline.add(
				new Document("$lookup", 
					new Document("from", "Users")
					.append("pipeline", 
						Arrays.asList(
							new Document("$match", 
								new Document("$expr", 
									new Document("$and", 
										Arrays.asList(
											new Document("$eq", Arrays.asList("$IssuerId", header.getIssuerId()))
											, new Document("$eq", Arrays.asList("$UserName", header.getUserName()+ "_"+ userName))
											, new Document("$ne", Arrays.asList("$IsDelete", true))
										)
									)
								)
							)
							, new Document("$project", new Document("_id", 1))
							, new Document("$limit", 1)
						)
					)
					.append("as", "UsersInfo")
				)
			);
			pipeline.add(
				new Document("$unwind", 
					new Document("path", "$UsersInfo").append("preserveNullAndEmptyArrays", true)
				)
			);
//			pipeline.add(
//				new Document("$lookup", 
//					new Document("from", "PositionsManage")
//					.append("pipeline", 
//						Arrays.asList(
//							new Document("$match", 
//								new Document("$expr", 
//									new Document("$and", 
//										Arrays.asList(
//											new Document("$eq", Arrays.asList("$AgentId", header.getAgentId()))
//											, new Document("$eq", Arrays.asList(new Document("$toString", "$_id"), positionId))
//											, new Document("$eq", Arrays.asList("$IsActive", true))
//											, new Document("$ne", Arrays.asList("$IsDelete", true))
//										)
//									)
//								)
//							)
//							, new Document("$project", new Document("_id", new Document("$toString", "$_id")).append("PositionName", 1))
//							, new Document("$limit", 1)
//						)
//					)
//					.append("as", "PositionsManageInfo")
//				)
//			);
//			pipeline.add(
//				new Document("$unwind", 
//					new Document("path", "$PositionsManageInfo").append("preserveNullAndEmptyArrays", true)
//				)
//			);
			pipeline.add(
				new Document("$lookup", 
					new Document("from", "RolesRightManage")
					.append("pipeline", 
						Arrays.asList(
							new Document("$match", 
								new Document("$expr", 
									new Document("$and", 
										Arrays.asList(
											new Document("$eq", Arrays.asList("$IssuerId", header.getIssuerId()))
											, new Document("$eq", Arrays.asList(new Document("$toString", "$_id"), roleId))
											, new Document("$eq", Arrays.asList("$IsActive", true))
											, new Document("$ne", Arrays.asList("$IsDelete", true))
										)
									)
								)
							)
							, new Document("$project", new Document("_id", new Document("$toString", "$_id")).append("RoleName", 1))
							, new Document("$limit", 1)
						)
					)
					.append("as", "RolesRightManageInfo")
				)
			);
			pipeline.add(
				new Document("$unwind", 
					new Document("path", "$RolesRightManageInfo").append("preserveNullAndEmptyArrays", true)
				)
			);
			
			docTmp = null;
			
			MongoClient mongoClient = cfg.mongoClient();
			MongoCollection<Document> collection = mongoClient.getDatabase(cfg.dbName).getCollection("Issuer");
			try {
				docTmp =   collection.aggregate(pipeline).allowDiskUse(true).iterator().next();		
			} catch (Exception e) {
				// TODO: handle exception
			}
				
			mongoClient.close();
			if(null == docTmp) {
				responseStatus = new MspResponseStatus(9999, "Khách hàng không tồn tại trong hệ thống.");
				rsp.setResponseStatus(responseStatus);
				return rsp;
			}
			if(docTmp.get("UsersInfo") != null) {
				responseStatus = new MspResponseStatus(9999, "Người dùng đã tồn tại trong hệ thống.");
				rsp.setResponseStatus(responseStatus);
				return rsp;
			}
			if(null == docTmp.get("RolesRightManageInfo")) {
				responseStatus = new MspResponseStatus(9999, "Nhóm quyền không tồn tại hoặc chưa được kích hoạt.");
				rsp.setResponseStatus(responseStatus);
				return rsp;
			}else {
				docRolesRightManageInfo = (Document) docTmp.get("RolesRightManageInfo");
			}
			
			String prefixUserID = header.getUserName();
			user = prefixUserID + "_" + userName;
//			password = commons.csRandomNumbericString(6);
//			passwordInput = commons.generateSHA(user + password, false).toUpperCase();
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
					.append("IssuerId", header.getIssuerId())
					.append("IsActive", true)
					.append("IsDelete", false)
					.append("EffectDate", LocalDateTime.of(eff, LocalTime.of(0, 0, 0)))
					.append("ExpireDate", LocalDateTime.of(exp, LocalTime.of(23, 59, 59)))
					.append("IsRoot", true)
					.append("IsRole", true)
					.append("PositionName", positionName)
					.append("RolesRightManageInfo", docRolesRightManageInfo);

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
			docFind = new Document("IssuerId", header.getIssuerId()).append("_id", objectId).append("IsDelete", new Document("$ne", true));
			
			pipeline = new ArrayList<Document>();
			pipeline.add(new Document("$match", docFind));
			pipeline.add(
				new Document("$project", new Document("_id", 1).append("IsActive", new Document("$ifNull", Arrays.asList("$IsActive", false))))
			);
//			pipeline.add(
//				new Document("$lookup", 
//					new Document("from", "PositionsManage")
//					.append("pipeline", 
//						Arrays.asList(
//							new Document("$match", 
//								new Document("$expr", 
//									new Document("$and", 
//										Arrays.asList(
//											new Document("$eq", Arrays.asList("$AgentId", header.getAgentId()))
//											, new Document("$eq", Arrays.asList(new Document("$toString", "$_id"), positionId))
//											, new Document("$eq", Arrays.asList("$IsActive", true))
//											, new Document("$ne", Arrays.asList("$IsDelete", true))
//										)
//									)
//								)
//							)
//							, new Document("$project", new Document("_id", new Document("$toString", "$_id")).append("PositionName", 1))
//							, new Document("$limit", 1)
//						)
//					)
//					.append("as", "PositionsManageInfo")
//				)
//			);
//			pipeline.add(new Document("$unwind", new Document("path", "$PositionsManageInfo").append("preserveNullAndEmptyArrays", true)));
			pipeline.add(
				new Document("$lookup", 
					new Document("from", "RolesRightManage")
					.append("pipeline", 
						Arrays.asList(
							new Document("$match", 
								new Document("$expr", 
									new Document("$and", 
										Arrays.asList(
											new Document("$eq", Arrays.asList("$IssuerId", header.getIssuerId()))
											, new Document("$eq", Arrays.asList(new Document("$toString", "$_id"), roleId))
											, new Document("$eq", Arrays.asList("$IsActive", true))
											, new Document("$ne", Arrays.asList("$IsDelete", true))
										)
									)
								)
							)
							, new Document("$project", new Document("_id", new Document("$toString", "$_id")).append("RoleName", 1))
							, new Document("$limit", 1)
						)
					)
					.append("as", "RolesRightManageInfo")
				)
			);
			pipeline.add(new Document("$unwind", new Document("path", "$RolesRightManageInfo").append("preserveNullAndEmptyArrays", true)));
			
		
			 mongoClient = cfg.mongoClient();
			 collection = mongoClient.getDatabase(cfg.dbName).getCollection("Users");
			try {
				docTmp =   collection.aggregate(pipeline).allowDiskUse(true).iterator().next();		
			} catch (Exception e) {
				// TODO: handle exception
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
			
//			if(null == docTmp.get("PositionsManageInfo")) {
//				responseStatus = new MspResponseStatus(9999, "Chức danh không tồn tại hoặc chưa được kích hoạt.");
//				rsp.setResponseStatus(responseStatus);
//				return rsp;
//			}else {
//				docPositionsManageInfo = (Document) docTmp.get("PositionsManageInfo");
//			}
			if(null == docTmp.get("RolesRightManageInfo")) {
				responseStatus = new MspResponseStatus(9999, "Nhóm quyền không tồn tại hoặc chưa được kích hoạt.");
				rsp.setResponseStatus(responseStatus);
				return rsp;
			}else {
				docRolesRightManageInfo = (Document) docTmp.get("RolesRightManageInfo");
			}
			/*END - KIEM TRA THONG TIN NHAN VIEN*/
			
			options = new FindOneAndUpdateOptions();
			options.upsert(false);
			options.maxTime(5000, TimeUnit.MILLISECONDS);
			options.returnDocument(ReturnDocument.AFTER);
			

			mongoClient = cfg.mongoClient();
			collection = mongoClient.getDatabase(cfg.dbName).getCollection("Users");
			Document docR =	collection.findOneAndUpdate(
					docFind
					, new Document("$set", 
							new Document("FullName", fullName)
								.append("Phone", phone)
								.append("Email", email)
								.append("EffectDate", LocalDateTime.of(eff, LocalTime.of(0, 0, 0)))
								.append("ExpireDate", LocalDateTime.of(exp, LocalTime.of(23, 59, 59)))
								.append("PositionName", positionName)
								.append("RolesRightManageInfo", docRolesRightManageInfo)
								.append("InfoUpdated", 
										new Document("UpdatedDate", LocalDateTime.now())
											.append("UpdatedUserID", header.getUserId())
											.append("UpdatedUserName", header.getUserName())
											.append("UpdatedUserFullName", header.getUserFullName())
										)									
								.append("UpdateDate", LocalDateTime.now())
								.append("UpdateUser", header.getUserId())
								.append("HasRetired", "Y".equals(hasRetired))
								
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

			docFind = new Document("IssuerId", header.getIssuerId())
					.append("_id", objectId)
					.append("IsDelete", new Document("$ne", true));
			docTmp = null;
		
			
			 mongoClient = cfg.mongoClient();
			 collection = mongoClient.getDatabase(cfg.dbName).getCollection("Users");
			try {
				docTmp =   collection.find(docFind).allowDiskUse(true).iterator().next();		
			} catch (Exception e) {
				// TODO: handle exception
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
				docR =  collection.findOneAndUpdate(
						docFind
						, new Document("$set", 
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
				docR =	collection.findOneAndUpdate(
						docFind
						, new Document("$set", 
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
				docR =	collection.findOneAndUpdate(
						docFind
						, new Document("$set", 
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
			docFind = new Document("IssuerId", header.getIssuerId())
					.append("_id", objectId)
					.append("IsDelete", new Document("$ne", true));

			
			docTmp = null;
	
			 mongoClient = cfg.mongoClient();
			 collection = mongoClient.getDatabase(cfg.dbName).getCollection("Users");
			 try {
					docTmp =   collection.find(docFind).allowDiskUse(true).iterator().next();			
			} catch (Exception e) {
				// TODO: handle exception
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
			docR =	collection.findOneAndUpdate(
					docFind
					, new Document("$set", 
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

		Document docFind = new Document("IsDelete", new Document("$ne", true))
				.append("IsRole", true)
				.append("_id", objectId);

		Document docTmp = null;

		MongoClient mongoClient = cfg.mongoClient();
		MongoCollection<Document> collection = mongoClient.getDatabase(cfg.dbName).getCollection("Users");
		try {
			docTmp =   collection.find(docFind).allowDiskUse(true).iterator().next();		
		} catch (Exception e) {
			// TODO: handle exception
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
