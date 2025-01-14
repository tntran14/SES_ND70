package vn.sesgroup.hddt.user.impl;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
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
import vn.sesgroup.hddt.user.dao.QLFooterWebDao;
import vn.sesgroup.hddt.user.service.TCTNService;
import vn.sesgroup.hddt.utility.Constants;
import vn.sesgroup.hddt.utility.Json;

@Repository
@Transactional
public class QLFooterWebImpl extends AbstractDAO implements QLFooterWebDao{
	private static final Logger log = LogManager.getLogger(QLFooterWebImpl.class);
	@Autowired ConfigConnectMongo cfg;
	@Autowired TCTNService tctnService;
	

	
	@Transactional(rollbackFor = {Exception.class})
	@Override
	public MsgRsp crud(JSONRoot jsonRoot) throws Exception {
		Msg msg = jsonRoot.getMsg();
		MsgHeader header = msg.getMsgHeader();
		MsgPage page = msg.getMsgPage();
		Object objData = msg.getObjData();
		FindOneAndUpdateOptions options = null;
	
		
		JsonNode jsonData = null;
		if(objData != null) {
			jsonData = Json.serializer().nodeFromObject(msg.getObjData());
		}else{
			throw new Exception("Lỗi dữ liệu đầu vào");
		}
		
		String actionCode = header.getActionCode();
		String _id = commons.getTextJsonNode(jsonData.at("/_id")).replaceAll("\\s", "");
		String noidung = commons.getTextJsonNode(jsonData.at("/Noidung"));
		
		
	
		MsgRsp rsp = new MsgRsp(header);
		rsp.setMsgPage(page);
		MspResponseStatus responseStatus = null;
		
		ObjectId objectId = null;		
		List<Document> pipeline = null;
		Document docFind = null;
		Document docTmp = null;
		Document docUpsert = null;

		
//////////////////////////////////////////////////////////////////////////////
	
		switch (actionCode) {
		case Constants.MSG_ACTION_CODE.CREATED:
			objectId = null;
	
			try {
				objectId = new ObjectId(header.getIssuerId());
			}catch(Exception ex) {}
			
			String pathDir = "";
			pathDir = "C:/hddt-ses/server/template/file";
			/*LUU DU LIEU HD*/
			docUpsert = new Document("Noidung",noidung)
						.append("Date", LocalDate.now())
				.append("IsActive", false)
				.append("IsDelete", false)
				.append("InfoCreated", 
					new Document("CreateDate", LocalDateTime.now())
					.append("CreateUserID", header.getUserId())
					.append("CreateUserName", header.getUserName())
					.append("CreateUserFullName", header.getUserFullName())
				);
			/*END - LUU DU LIEU HD*/

			MongoClient mongoClient = cfg.mongoClient();
			MongoCollection<Document> collection = mongoClient.getDatabase(cfg.dbName).getCollection("DMFooterWeb");
			collection.insertOne(docUpsert);      
			mongoClient.close();
		
	
			responseStatus = new MspResponseStatus(0, "SUCCESS");
			rsp.setResponseStatus(responseStatus);				
			return rsp;	
///////////////////////////////////////////////////////////////////////////////////////
		case Constants.MSG_ACTION_CODE.DELETE:
			objectId = null;
			try {
				objectId = new ObjectId(_id);
			}catch(Exception ex) {}
			/*KIEM TRA THONG TIN HOPDONG CO TON TAI KHONG*/
			docFind = new Document("_id",objectId);
			docTmp = null;		
			
			pipeline = new ArrayList<Document>();
			pipeline.add(new Document("$match", docFind));
			mongoClient = cfg.mongoClient();
			collection = mongoClient.getDatabase(cfg.dbName).getCollection("DMFooterWeb");
			  try {
			         docTmp = collection.aggregate(pipeline).allowDiskUse(true).iterator().next();      
			     } catch (Exception e) {
			        
			    }
			mongoClient.close();
			
			if(null == docTmp) {
				responseStatus = new MspResponseStatus(9999, "Không tìm thấy thông tin hóa đơn.");
				rsp.setResponseStatus(responseStatus);
				return rsp;
			}

			options = new FindOneAndUpdateOptions();
			options.upsert(false);
			options.maxTime(5000, TimeUnit.MILLISECONDS);
			options.returnDocument(ReturnDocument.AFTER);
			
			mongoClient = cfg.mongoClient();
			collection = mongoClient.getDatabase(cfg.dbName).getCollection("DMFooterWeb");
			collection.findOneAndUpdate(docFind,
					new Document("$set", 
							new Document("IsDelete", true)
							.append("InfoDeleted", 
									new Document("DeletedDate", LocalDateTime.now())
										.append("DeletedUserID", header.getUserId())
										.append("DeletedUserName", header.getUserName())
										.append("DeletedUserFullName", header.getUserFullName())
									)
						)
						, options); 
			  mongoClient.close();
			
			responseStatus = new MspResponseStatus(0, "SUCCESS");
			rsp.setResponseStatus(responseStatus);				
			return rsp;
//////////////////////////////////////////////////////////////////////////////////////
		case Constants.MSG_ACTION_CODE.ACTIVE:
			objectId = null;
			try {
				objectId = new ObjectId(_id);
			}catch(Exception ex) {}
			/*KIEM TRA THONG TIN HOPDONG CO TON TAI KHONG*/
			docFind = new Document("_id",objectId);
			docTmp = null;
			
			pipeline = new ArrayList<Document>();
			pipeline.add(new Document("$match", docFind));
			mongoClient = cfg.mongoClient();
			collection = mongoClient.getDatabase(cfg.dbName).getCollection("DMFooterWeb");
			  try {
			         docTmp = collection.aggregate(pipeline).allowDiskUse(true).iterator().next();      
			     } catch (Exception e) {
			        
			    }
			mongoClient.close();
			
			if(null == docTmp) {
				responseStatus = new MspResponseStatus(9999, "Không tìm thấy thông tin hóa đơn.");
				rsp.setResponseStatus(responseStatus);
				return rsp;
			}

			options = new FindOneAndUpdateOptions();
			options.upsert(false);
			options.maxTime(5000, TimeUnit.MILLISECONDS);
			options.returnDocument(ReturnDocument.AFTER);
			
			mongoClient = cfg.mongoClient();
			collection = mongoClient.getDatabase(cfg.dbName).getCollection("DMFooterWeb");
			collection.findOneAndUpdate(docFind,
					new Document("$set", 
							new Document("IsActive", true)
							.append("InfoDeleted", 
									new Document("DeletedDate", LocalDateTime.now())
										.append("DeletedUserID", header.getUserId())
										.append("DeletedUserName", header.getUserName())
										.append("DeletedUserFullName", header.getUserFullName())
									)
						)
						, options);
			  mongoClient.close();
			
			responseStatus = new MspResponseStatus(0, "SUCCESS");
			rsp.setResponseStatus(responseStatus);				
			return rsp;
////////////////////////////////////////////////////////////////////////////////////////
		case Constants.MSG_ACTION_CODE.DEACTIVE:
			objectId = null;
			try {
				objectId = new ObjectId(_id);
			}catch(Exception ex) {}
			/*KIEM TRA THONG TIN HOPDONG CO TON TAI KHONG*/
			docFind = new Document("_id",objectId);
			docTmp = null;
		
			pipeline = new ArrayList<Document>();
			pipeline.add(new Document("$match", docFind));
			mongoClient = cfg.mongoClient();
			collection = mongoClient.getDatabase(cfg.dbName).getCollection("DMFooterWeb");
			  try {
			         docTmp = collection.aggregate(pipeline).allowDiskUse(true).iterator().next();      
			     } catch (Exception e) {
			        
			    }
			mongoClient.close();
			
			if(null == docTmp) {
				responseStatus = new MspResponseStatus(9999, "Không tìm thấy thông tin hóa đơn.");
				rsp.setResponseStatus(responseStatus);
				return rsp;
			}

			options = new FindOneAndUpdateOptions();
			options.upsert(false);
			options.maxTime(5000, TimeUnit.MILLISECONDS);
			options.returnDocument(ReturnDocument.AFTER);
			
			mongoClient = cfg.mongoClient();
			collection = mongoClient.getDatabase(cfg.dbName).getCollection("DMFooterWeb");
			collection.findOneAndUpdate(docFind,
					new Document("$set", 
							new Document("IsActive", false)
							.append("InfoDeleted", 
									new Document("DeletedDate", LocalDateTime.now())
										.append("DeletedUserID", header.getUserId())
										.append("DeletedUserName", header.getUserName())
										.append("DeletedUserFullName", header.getUserFullName())
									)
						)
						, options);
			  mongoClient.close();
			
			responseStatus = new MspResponseStatus(0, "SUCCESS");
			rsp.setResponseStatus(responseStatus);				
			return rsp;
//////////////////////////////////////////////////////////////////////////////////////
		case Constants.MSG_ACTION_CODE.MODIFY:
			try {
				objectId = new ObjectId(_id);
			}catch(Exception ex) {}
			
			/*KIEM TRA THONG TIN KHACH HANG - USER - TINH THANH - CO QUAN THUE CO TON TAI KHONG*/
			pipeline = new ArrayList<Document>();
			docFind = new Document("_id", objectId).append("IsDelete", new Document("$ne", true));
			pipeline.add(
				new Document("$lookup", 
					new Document("from", "DMFooterWeb")
					.append("pipeline", 
						Arrays.asList(
							new Document("$match", docFind)
						)
					)
					.append("as", "DMFooterWeb")
				)
			);
			pipeline.add(new Document("$unwind", new Document("path", "$DMFooterWeb").append("preserveNullAndEmptyArrays", true)));
		
			mongoClient = cfg.mongoClient();
			collection = mongoClient.getDatabase(cfg.dbName).getCollection("DMFooterWeb");
			  try {
			         docTmp = collection.aggregate(pipeline).allowDiskUse(true).iterator().next();      
			     } catch (Exception e) {
			        
			    }
			mongoClient.close();
		
			if(null == docTmp) {
				responseStatus = new MspResponseStatus(9999, "Không tìm thấy thông tin .");
				rsp.setResponseStatus(responseStatus);
				return rsp;
			}
			
			/*END - LUU DU LIEU HD*/
	

		options = new FindOneAndUpdateOptions();
		options.upsert(false);
		options.maxTime(5000, TimeUnit.MILLISECONDS);
		options.returnDocument(ReturnDocument.AFTER);
		
		mongoClient = cfg.mongoClient();
		collection = mongoClient.getDatabase(cfg.dbName).getCollection("DMFooterWeb");
		collection.findOneAndUpdate(docFind,
				new Document("$set", 
						new Document("Noidung",noidung)
						.append("Date", LocalDate.now())
				.append("IsActive", false)
				.append("IsDelete", false)
				.append("InfoUpdate", 
					new Document("UpdateDate", LocalDateTime.now())
					.append("UpdateUserID", header.getUserId())
					.append("UpdateUserName", header.getUserName())
					.append("UpdateUserFullName", header.getUserFullName())
				)
				), 
				options);
			
		  mongoClient.close();
	

			responseStatus = new MspResponseStatus(0, "SUCCESS");
			rsp.setResponseStatus(responseStatus);				
			return rsp;
		
		}
		return rsp;
	
	}


	
	@Override
	public MsgRsp list(JSONRoot jsonRoot) throws Exception {
		Msg msg = jsonRoot.getMsg();
		MsgHeader header = msg.getMsgHeader();
		MsgPage page = msg.getMsgPage();
		Object objData = msg.getObjData();
		
		 String name  = "";
		 String acti = "";
		JsonNode jsonData = null;
		if(objData != null) {
			jsonData = Json.serializer().nodeFromObject(objData);
			name = commons.getTextJsonNode(jsonData.at("/Name")).replaceAll("\\s", "");
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
		if(!"".equals(name))
			docMatch.append("Chude", commons.regexEscapeForMongoQuery(name));
		if(!"".equals(acti))
			docMatch.append("IsActive",isacti );
		
		pipeline = new ArrayList<Document>();
		pipeline.add(new Document("$match", docMatch));
		pipeline.add(
				new Document("$sort", 
					new Document("_id", -1)
				)
			);
		pipeline.addAll(createFacetForSearchNotSort(page));
		
		MongoClient mongoClient = cfg.mongoClient();
		MongoCollection<Document> collection = mongoClient.getDatabase(cfg.dbName).getCollection("DMFooterWeb");
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
				hItem.put("Noidung", doc.get("Noidung"));
				hItem.put("IsActive", doc.get("IsActive"));
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
		
		Document docFind = new Document("_id", objectId);	
		Document docTmp = null;
	
		List<Document> pipeline = null;
		pipeline = new ArrayList<Document>();
		pipeline.add(new Document("$match", docFind));
		MongoClient mongoClient = cfg.mongoClient();
		MongoCollection<Document> collection = mongoClient.getDatabase(cfg.dbName).getCollection("DMFooterWeb");
		      try {
		        docTmp = collection.aggregate(pipeline).allowDiskUse(true).iterator().next();    
		      } catch (Exception e) {
		        
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

}
