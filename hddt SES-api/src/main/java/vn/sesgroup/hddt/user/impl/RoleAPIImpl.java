package vn.sesgroup.hddt.user.impl;

import java.time.LocalDateTime;
import java.util.ArrayList;
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
import vn.sesgroup.hddt.user.dao.RoleAPIDAO;
import vn.sesgroup.hddt.user.service.JPUtils;
import vn.sesgroup.hddt.user.service.TCTNService;
import vn.sesgroup.hddt.utility.Constants;
import vn.sesgroup.hddt.utility.Json;

@Repository
@Transactional
public class RoleAPIImpl extends AbstractDAO implements RoleAPIDAO {
	private static final Logger log = LogManager.getLogger(RoleAPIImpl.class);
	@Autowired ConfigConnectMongo cfg;
	@Autowired TCTNService tctnService;
	@Autowired JPUtils jpUtils;
	
	Document docUpsert = null;
	
	
	public MsgRsp list(JSONRoot jsonRoot) throws Exception {
		Msg msg = jsonRoot.getMsg();
		MsgHeader header = msg.getMsgHeader();
		MsgPage page = msg.getMsgPage();
		Object objData = msg.getObjData();
		String mst = "";
		String name = "";
		String acti = "";
		JsonNode jsonData = null;
		if (objData != null) {
			jsonData = Json.serializer().nodeFromObject(objData);
			
			mst = commons.getTextJsonNode(jsonData.at("/mst")).replaceAll("\\s", " ");
			name = commons.getTextJsonNode(jsonData.at("/name")).replaceAll("\\s", " ");
			acti = commons.getTextJsonNode(jsonData.at("/acti")).replaceAll("\\s", "");
		}
		
		Boolean isacti = true;
		if(acti.equals("true")) {
			isacti = true;
		}
		if(acti.equals("false")) {
			isacti = false;
		}
		
		if(acti.equals("DELETE")) {
			isacti = false;
		}
		MsgRsp rsp = new MsgRsp(header);
		MspResponseStatus responseStatus = null;

		ObjectId objectId = null;
		Document docTmp = null;
		List<Document> pipeline = new ArrayList<Document>();


		Document docMatch = new Document("IsDelete",
				new Document("$ne", true));
		
		if (!"".equals(mst))
			docMatch.append("UserName",
					new Document("$regex", commons.regexEscapeForMongoQuery(mst)).append("$options", "i"));
		if (!"".equals(name))
			docMatch.append("FullName",
					new Document("$regex", commons.regexEscapeForMongoQuery(name)).append("$options", "i"));
		if(!"".equals(acti))
			docMatch.append("IsActive",isacti );
		
		pipeline = new ArrayList<Document>();
		pipeline.add(new Document("$match", docMatch));		
		
		pipeline.add(new Document("$sort",
				new Document("_id", -1)					
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

				hItem = new HashMap<String, Object>();
				hItem.put("_id", objectId.toString());
				hItem.put("UserName", doc.get("UserName"));
				hItem.put("FullName", doc.get("FullName"));		
				hItem.put("IsActive", doc.get("IsActive"));		
				hItem.put("roles", doc.get("roles", ""));	
				hItem.put("Phone", doc.get("Phone", ""));
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
		List<Document> pipeline = null;
		Document docTmp = null;
		
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
	
		switch (actionCode) {
					
		case Constants.MSG_ACTION_CODE.ACTIVE:
		objectId = new ObjectId(_id);
		Document docFind = new Document("_id", objectId)
				.append("IsDelete", new Document("$ne", true))
				.append("IsActive", new Document("$ne", false))
				;
		
		pipeline = new ArrayList<Document>();
		pipeline.add(new Document("$match", docFind));
		MongoClient mongoClient = cfg.mongoClient();
		MongoCollection<Document> collection = mongoClient.getDatabase(cfg.dbName).getCollection("Users");
		      try {
		        docTmp = collection.aggregate(pipeline).allowDiskUse(true).iterator().next();    
		      } catch (Exception e) {
		        
		      }
		        
		mongoClient.close();
		
		if(docTmp==null) {
			responseStatus = new MspResponseStatus(999, "Không tìm thấy thông tin.");
			rsp.setResponseStatus(responseStatus);
			return rsp;	
		}
	
		
		options = new FindOneAndUpdateOptions();
		options.upsert(false);
		options.maxTime(5000, TimeUnit.MILLISECONDS);
		options.returnDocument(ReturnDocument.AFTER);
		
		mongoClient = cfg.mongoClient();
		collection = mongoClient.getDatabase(cfg.dbName).getCollection("Users");
		collection.findOneAndUpdate(docFind,
				new Document("$set",
						new Document("roles", "ROLE_ADMIN")					
						),
				options);
		
		  mongoClient.close();
		
		responseStatus = new MspResponseStatus(0, "SUCCESS");
		rsp.setResponseStatus(responseStatus);
		return rsp;
		
		case Constants.MSG_ACTION_CODE.DEACTIVE:
			objectId = new ObjectId(_id);
			 docFind = new Document("_id", objectId)
			.append("IsDelete", new Document("$ne", true));
			
			pipeline = new ArrayList<Document>();
			pipeline.add(new Document("$match", docFind));
			mongoClient = cfg.mongoClient();
			collection = mongoClient.getDatabase(cfg.dbName).getCollection("Users");
			  try {
			         docTmp = collection.aggregate(pipeline).allowDiskUse(true).iterator().next();      
			     } catch (Exception e) {
			        
			    }
			mongoClient.close();
			
			if(docTmp==null) {
				responseStatus = new MspResponseStatus(999, "Không tìm thấy thông tin.");
				rsp.setResponseStatus(responseStatus);
				return rsp;	
			}
			
			
			
			options = new FindOneAndUpdateOptions();
			options.upsert(false);
			options.maxTime(5000, TimeUnit.MILLISECONDS);
			options.returnDocument(ReturnDocument.AFTER);
			 
				mongoClient = cfg.mongoClient();
				collection = mongoClient.getDatabase(cfg.dbName).getCollection("Users");
				collection.findOneAndUpdate(docFind,
						new Document("$set",
								new Document("roles", "")		
								),
						options); 
				  mongoClient.close();
			
			responseStatus = new MspResponseStatus(0, "SUCCESS");
			rsp.setResponseStatus(responseStatus);
			return rsp;
			
		case Constants.MSG_ACTION_CODE.DELETE:
			objectId = null;
			
			try {
				objectId = new ObjectId(_id);
			} catch (Exception e) {
			}
			/* KIEM TRA THONG TIN HD CO TON TAI KHONG */
			docFind = new Document("IsDelete", new Document("$ne", true))
					.append("_id", objectId);

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
			
			if (null == docTmp) {
				responseStatus = new MspResponseStatus(9999, "Không tìm thấy thông tin.");
				rsp.setResponseStatus(responseStatus);
				return rsp;
			}
			
			boolean status = docTmp.get("Status",false);

			
			if(status==false) {
				//CAP NHAT TRANG THAI XOA
				options = new FindOneAndUpdateOptions();
				options.upsert(false);
				options.maxTime(5000, TimeUnit.MILLISECONDS);
				options.returnDocument(ReturnDocument.AFTER);

				mongoClient = cfg.mongoClient();
				collection = mongoClient.getDatabase(cfg.dbName).getCollection("Users");
				collection.findOneAndUpdate(docFind,
						new Document("$set", new Document("IsDelete", true).append("InfoDeleted",
								new Document("DeletedDate", LocalDateTime.now()).append("DeletedUserID", header.getUserId())
										.append("DeletedUserName", header.getUserName())
										.append("DeletedUserFullName", header.getUserFullName()))),
						options);
				  mongoClient.close();
				
				responseStatus = new MspResponseStatus(0, "SUCCESS");
				rsp.setResponseStatus(responseStatus);
				return rsp;
			}else {			
					responseStatus = new MspResponseStatus(9999, "Không thể xóa hóa khi đang kích hoạt!");
					rsp.setResponseStatus(responseStatus);
					return rsp;				
			}
			
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
	
		List<Document> pipeline = null;
		pipeline = new ArrayList<Document>();
		pipeline.add(new Document("$match", docFind));
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
