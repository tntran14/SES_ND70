package vn.sesgroup.hddt.user.impl;

import java.time.LocalDateTime;
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
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.FindOneAndUpdateOptions;
import com.mongodb.client.model.ReturnDocument;

import vn.sesgroup.hddt.configuration.ConfigConnectMongo;
import vn.sesgroup.hddt.user.dao.AbstractDAO;
import vn.sesgroup.hddt.user.dao.RoleManagerUserDAO;
import vn.sesgroup.hddt.user.service.JPUtils;
import vn.sesgroup.hddt.user.service.TCTNService;
import vn.sesgroup.hddt.utility.Constants;
import vn.sesgroup.hddt.utility.Json;
import vn.sesgroup.hddt.utility.MailUtils;

@Repository
@Transactional
public class RoleManagerUserImpl extends AbstractDAO implements RoleManagerUserDAO {
	private static final Logger log = LogManager.getLogger(RoleManagerUserImpl.class);
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
		
		String roleName = "";

		JsonNode jsonData = null;
		if (objData != null) {
			jsonData = Json.serializer().nodeFromObject(objData);
			
			roleName = commons.getTextJsonNode(jsonData.at("/RoleName")).replaceAll("\\s", " ");
	
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
				.append("IssuerId", header.getIssuerId());
		
		if (!"".equals(roleName))
			docMatch.append("RoleName",
					new Document("$regex", commons.regexEscapeForMongoQuery(roleName)).append("$options", "i"));
	
		

		
		Document fillter = new Document("_id", 1).append("InfoUpdated", 1).append("InfoCreated", 1)
				.append("IsRoleRoot", 1).append("IsActive", 1).append("FunctionRights", 1).append("RoleName", 1).append("RoleId", 1);
		
		
		pipeline = new ArrayList<Document>();
		pipeline.add(new Document("$match", docMatch));		
		
		pipeline.add(new Document("$sort",
				new Document("_id", -1)					
				));
		pipeline.add(new Document("$project", fillter));
		pipeline.addAll(createFacetForSearchNotSort(page));
	
		MongoClient mongoClient = cfg.mongoClient();
		MongoCollection<Document> collection = mongoClient.getDatabase(cfg.dbName).getCollection("RolesRightManage");
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
				hItem.put("RoleId", doc.get("RoleId"));
				hItem.put("RoleName", doc.get("RoleName"));		
				hItem.put("IsActive", doc.get("IsActive"));		
				hItem.put("NumFunctionRights", 
						null != doc.get("FunctionRights") && doc.get("FunctionRights") instanceof List?
								doc.getList("FunctionRights", Object.class).size() : 0
						);	
				hItem.put("IsActive", doc.getBoolean("IsActive", false));
				hItem.put("IsRoleRoot", doc.getBoolean("IsRoleRoot", false));
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
		Iterable<Document> cursor = null;
		Iterator<Document> iter = null;
		Document docInsert = null;
		List<Document> rightsAgent = null;
		List<String> rightForAgent = null;
		
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
		String roleId = commons.getTextJsonNode(jsonData.at("/RoleId")).replaceAll("\\s", "").toUpperCase();
		String roleName = commons.getTextJsonNode(jsonData.at("/RoleName")).trim().replaceAll("\\s+", " ");
		List<String> arrayRightActions = new ArrayList<String>();
		if(!actionCode.equals("ACTIVE") && !actionCode.equals("DEACTIVE") && !actionCode.equals("DELETE")) {		
		try {
			arrayRightActions = Json.serializer().fromNode(jsonData.at("/JsonRightActions"), new TypeReference<ArrayList<String>>() {
			});
		}catch(Exception ex) {
			log.error(" >>>>> An exception occurred!", ex);
		}
		}
		
//		objectId = new ObjectId(_id);
//		try {
//			objectId = new ObjectId(header.getIssuerId());
//		}catch(Exception e) {}
//		
		switch (actionCode) {
		case Constants.MSG_ACTION_CODE.CREATED:
			objectId = new ObjectId(header.getIssuerId());
			pipeline = new ArrayList<Document>();
			pipeline.add(
				new Document("$match", new Document("_id", objectId).append("IsDelete", new Document("$ne", true)).append("IsActive", true))
			);			
			pipeline.add(
				new Document("$lookup", 
					new Document("from", "RolesRightManage")
					.append("let",
							new Document("vIssuerId", new Document("$toString", "$_id")))
					.append("pipeline", 
						Arrays.asList(
							new Document("$match", 
								new Document("$expr", 
									new Document("$and", 
										Arrays.asList(
											new Document("$eq", Arrays.asList("$IssuerId", "$$vIssuerId"))
											,
											new Document("$eq", Arrays.asList("$RoleId", roleId))
										)
									)
								)
							)
							, new Document("$count", "count")
						)
					)
					.append("as", "RolesRightManageInfo")
				)
			);
			pipeline.add(new Document("$unwind", new Document("path", "$RolesRightManageInfo").append("preserveNullAndEmptyArrays", true)));
				
			pipeline.add(
					new Document("$lookup", 
						new Document("from", "FullParamsSystems")
						.append("pipeline", 
							Arrays.asList(
								new Document("$match", 
									new Document("$expr", 
										new Document("$and", 
											Arrays.asList(
//												new Document("$eq", Arrays.asList("$$vAgentParentID", null))
//												,
												new Document("$eq", Arrays.asList("$k", "FULL-RIGHT-ADMIN"))
											)
										)
									)
								)
							)
						)
						.append("as", "FullRightAdmin")
					)
				);
				pipeline.add(new Document("$unwind", new Document("path", "$FullRightAdmin").append("preserveNullAndEmptyArrays", true)));
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
					responseStatus = new MspResponseStatus(99999, "Khách hàng không tồn tại trong hệ thống.");
					rsp.setResponseStatus(responseStatus);
					return rsp;
				}
				/*KIEM TRA NHOM QUYEN CO TON TAI KHONG*/
				int rightExists = docTmp.getEmbedded(Arrays.asList("RolesRightManageInfo", "count"), 0);
				if(rightExists > 0) {
					responseStatus = new MspResponseStatus(99999, "Nhóm quyền đã tồn tại trong hệ thống.");
					rsp.setResponseStatus(responseStatus);
					return rsp;
				}
				
				rightsAgent = null;
				if(null == docTmp.get("AgentParentID")) {
					rightsAgent = docTmp.getEmbedded(Arrays.asList("FullRightAdmin", "v"), List.class);	
				}
				if(rightsAgent == null || rightsAgent.size() == 0) {
					responseStatus = new MspResponseStatus(9999, "Không tìm thấy thông tin quyền đại lý.");
					rsp.setResponseStatus(responseStatus);
					return rsp;
				}
				rightForAgent = new ArrayList<String>();
				for(Document o: rightsAgent) {
					if(null != o.get("action") && arrayRightActions.contains(o.getString("action"))) {
						rightForAgent.add(o.getString("action"));
					}
				}
				
				/*INSERT*/
				docInsert = new Document("IssuerId", header.getIssuerId())
					.append("RoleId", roleId)
					.append("RoleName", roleName)
					.append("FunctionRights", rightForAgent)
					.append("IsActive", true)
					.append("IsRoleRoot", false)
					.append("InfoCreated", 
						new Document("CreateDate", LocalDateTime.now())
							.append("CreateUserID", header.getUserId())
							.append("CreateUserName", header.getUserName())
							.append("CreateUserFullName", header.getUserFullName())
					);
			
				mongoClient = cfg.mongoClient();
				collection = mongoClient.getDatabase(cfg.dbName).getCollection("RolesRightManage");
				collection.insertOne(docInsert);			
				mongoClient.close();
				
		responseStatus = new MspResponseStatus(0, "SUCCESS");
		rsp.setResponseStatus(responseStatus);
		return rsp;
		case Constants.MSG_ACTION_CODE.MODIFY:
			
			objectId = null;
			try {
				objectId = new ObjectId(_id);
			}catch(Exception e) {}
		
			
			pipeline = new ArrayList<Document>();
			pipeline.add(
				new Document("$match", 
					new Document("IssuerId", header.getIssuerId()).append("_id", objectId).append("IsDelete", new Document("$ne", true))
				)
			);
			pipeline.add(
					new Document("$lookup", 
						new Document("let", new Document("vAgentParentID", "$AgentInfo.AgentParentID"))
						.append("from", "FullParamsSystems")
						.append("pipeline", 
							Arrays.asList(
								new Document("$match", 
									new Document("$expr", 
										new Document("$and", 
											Arrays.asList(
//												new Document("$eq", Arrays.asList("$$vAgentParentID", null))
//												,
												new Document("$eq", Arrays.asList("$k", "FULL-RIGHT-ADMIN"))
											)
										)
									)
								)
							)
						)
						.append("as", "FullRightAdmin")
					)
				);
				pipeline.add(new Document("$unwind", new Document("path", "$FullRightAdmin").append("preserveNullAndEmptyArrays", true)));
				docTmp = null;
			
				 mongoClient = cfg.mongoClient();
				 collection = mongoClient.getDatabase(cfg.dbName).getCollection("RolesRightManage");
				try {
					docTmp =   collection.aggregate(pipeline).allowDiskUse(true).iterator().next();		
				} catch (Exception e) {
					// TODO: handle exception
				}
					
				mongoClient.close();
				
				if(null == docTmp) {
					responseStatus = new MspResponseStatus(99999, "Nhóm quyền không tồn tại trong hệ thống.");
					rsp.setResponseStatus(responseStatus);
					return rsp;
				}
				IsActive = docTmp.getBoolean("IsActive", false);
				if(IsActive) {
					responseStatus = new MspResponseStatus(9999, "Nhóm quyền đã được kích hoạt.");
					rsp.setResponseStatus(responseStatus);
					return rsp;
				}
				
				rightsAgent = null;
				if(null == docTmp.get("AgentParentID")) {
					rightsAgent = docTmp.getEmbedded(Arrays.asList("FullRightAdmin", "v"), List.class);	
				}
				if(rightsAgent == null || rightsAgent.size() == 0) {
					responseStatus = new MspResponseStatus(9999, "Không tìm thấy thông tin quyền đại lý.");
					rsp.setResponseStatus(responseStatus);
					return rsp;
				}
				rightForAgent = new ArrayList<String>();
				for(Document o: rightsAgent) {
					if(null != o.get("action") && arrayRightActions.contains(o.getString("action"))) {
						rightForAgent.add(o.getString("action"));
					}
				}
				objectId = null;
				try {
					objectId = new ObjectId(_id);
				}catch(Exception e) {}
				options = new FindOneAndUpdateOptions();
				options.upsert(false);
				options.maxTime(5000, TimeUnit.MILLISECONDS);
				options.returnDocument(ReturnDocument.AFTER);
				
				mongoClient = cfg.mongoClient();
				collection = mongoClient.getDatabase(cfg.dbName).getCollection("RolesRightManage");
				Document docR =	collection.findOneAndUpdate(
						new Document("IssuerId", header.getIssuerId()).append("_id", objectId)
						, new Document("$set", 
							new Document("RoleName", roleName)
							.append("FunctionRights", rightForAgent)
							.append("InfoUpdated", 
									new Document("UpdatedDate", LocalDateTime.now())
										.append("UpdatedUserID", header.getUserId())
										.append("UpdatedUserName", header.getUserName())
										.append("UpdatedUserFullName", header.getUserFullName())
									)
						),
						options);		
				mongoClient.close();
				
					
		responseStatus = new MspResponseStatus(0, "SUCCESS");
		rsp.setResponseStatus(responseStatus);
		return rsp;
		
		case Constants.MSG_ACTION_CODE.ACTIVE:
		case Constants.MSG_ACTION_CODE.DEACTIVE:
			/*KIEM TRA XEM _ID CO TON TAI KHONG*/
			if(!"".equals(_id)) {
				try {
					objectId = new ObjectId(_id);
				}catch(Exception e) {}
			}
			Document docFind = new Document("IssuerId", header.getIssuerId()).append("_id", objectId).append("IsDelete", new Document("$ne", true));
		
			
			 mongoClient = cfg.mongoClient();
			 collection = mongoClient.getDatabase(cfg.dbName).getCollection("RolesRightManage");
			 try {
					docTmp =   collection.find(docFind).allowDiskUse(true).iterator().next();			
			} catch (Exception e) {
				// TODO: handle exception
			}
			mongoClient.close();
			
			if(null == docTmp){
				responseStatus = new MspResponseStatus(16, Constants.MAP_ERROR.get(16));
				rsp.setResponseStatus(responseStatus);
				return rsp;
			}

			
			options = new FindOneAndUpdateOptions();
			options.upsert(false);
			options.maxTime(5000, TimeUnit.MILLISECONDS);
			options.returnDocument(ReturnDocument.AFTER);
			if(actionCode.equals(Constants.MSG_ACTION_CODE.ACTIVE)) {
				
				mongoClient = cfg.mongoClient();
				collection = mongoClient.getDatabase(cfg.dbName).getCollection("RolesRightManage");
				docR = 	collection.findOneAndUpdate(
						docFind
						, new Document("$set", 
							new Document("IsActive", true)
							.append("InfoActive", 
									new Document("ActiveDate", LocalDateTime.now())
										.append("ActiveUserID", header.getUserId())
										.append("ActiveUserName", header.getUserName())
										.append("ActiveUserFullName", header.getUserFullName())
									)
						),
						options);		
				mongoClient.close();
				
			}else {
				mongoClient = cfg.mongoClient();
				collection = mongoClient.getDatabase(cfg.dbName).getCollection("RolesRightManage");
				docR =	collection.findOneAndUpdate(
						docFind
						, new Document("$set", 
							new Document("IsActive", false)
							.append("InfoDeActive", 
									new Document("DeActiveDate", LocalDateTime.now())
										.append("DeActiveUserID", header.getUserId())
										.append("DeActiveUserName", header.getUserName())
										.append("DeActiveUserFullName", header.getUserFullName())
									)
						),
						options);			
				mongoClient.close();
				
			}

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
				.append("_id", objectId);

		Document docTmp = null;

		MongoClient mongoClient = cfg.mongoClient();
		MongoCollection<Document> collection = mongoClient.getDatabase(cfg.dbName).getCollection("RolesRightManage");
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
