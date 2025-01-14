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
import vn.sesgroup.hddt.user.dao.ColorDAO;
import vn.sesgroup.hddt.user.service.JPUtils;
import vn.sesgroup.hddt.user.service.TCTNService;
import vn.sesgroup.hddt.utility.Constants;
import vn.sesgroup.hddt.utility.Json;

@Repository
@Transactional
public class DMColorImpl extends AbstractDAO implements ColorDAO {
	private static final Logger log = LogManager.getLogger(DMColorImpl.class);
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

		String name = "";
		JsonNode jsonData = null;
		if (objData != null) {
			jsonData = Json.serializer().nodeFromObject(objData);
			name = commons.getTextJsonNode(jsonData.at("/Name")).replaceAll("\\s", " ");
		}

		MsgRsp rsp = new MsgRsp(header);
		MspResponseStatus responseStatus = null;

		ObjectId objectId = null;
		Document docTmp = null;
		List<Document> pipeline = new ArrayList<Document>();


		Document docMatch = new Document("IsDelete",
				new Document("$ne", true));
		if (!"".equals(name))
			docMatch.append("Name", commons.regexEscapeForMongoQuery(name));
		
		
		pipeline = new ArrayList<Document>();
		pipeline.add(new Document("$match", docMatch));		
		
		pipeline.add(new Document("$sort",
				new Document("_id", -1)					
				));	
		pipeline.add(new Document("$project", new Document("_id", 1).append("Name", 1).append("Color", 1).append("Status", 1)));
		pipeline.addAll(createFacetForSearchNotSort(page));
		
		MongoClient mongoClient = cfg.mongoClient();
		MongoCollection<Document> collection = mongoClient.getDatabase(cfg.dbName).getCollection("DMColor");
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
				hItem.put("Name", doc.get("Name"));
				hItem.put("Color", doc.get("Color"));		
				hItem.put("Status", doc.get("Status"));		
				
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
		String name = commons.getTextJsonNode(jsonData.at("/Name")).replaceAll("\\s", " ");
		String color = commons.getTextJsonNode(jsonData.at("/Color")).replaceAll("\\s", "");
		String lbt = commons.getTextJsonNode(jsonData.at("/LBT")).replaceAll("\\s", " ");
		String lbtcode = commons.getTextJsonNode(jsonData.at("/LBTCode")).replaceAll("\\s", "");
		
		switch (actionCode) {
		
		case Constants.MSG_ACTION_CODE.CREATED:
		
			objectId = new ObjectId();
			Document docUpsert = new Document("_id", objectId)
					.append("Name", name)
					.append("Color", color)	
					.append("LBT", lbt)
					.append("LBTCode", lbtcode)	
					.append("IsDelete", false)
					.append("Status", false);
			/* END - LUU DU LIEU HD */

			MongoClient mongoClient = cfg.mongoClient();
			MongoCollection<Document> collection = mongoClient.getDatabase(cfg.dbName).getCollection("DMColor");
			collection.insertOne(docUpsert);      
			mongoClient.close();
			
			responseStatus = new MspResponseStatus(0, "SUCCESS");
			rsp.setResponseStatus(responseStatus);
			return rsp;
			
		case Constants.MSG_ACTION_CODE.MODIFY:
		objectId = new ObjectId(_id);
		Document docFind = new Document("_id", objectId)
				.append("IsDelete", new Document("$ne", true));
		
		mongoClient = cfg.mongoClient();
		collection = mongoClient.getDatabase(cfg.dbName).getCollection("DMColor");
		  try {
		         docTmp = collection.find(docFind).allowDiskUse(true).iterator().next();      
		     } catch (Exception e) {
		        
		    }
		mongoClient.close();
		
		if(docTmp==null) {
			responseStatus = new MspResponseStatus(999, "Không tìm thấy thông tin.");
			rsp.setResponseStatus(responseStatus);
			return rsp;	
		}
			
		ObjectId id_color = docTmp.getObjectId("_id");			
		Document findColor = new Document("_id", id_color).append("IsDelete",  new Document("$ne", true));

		options = new FindOneAndUpdateOptions();
		options.upsert(false);
		options.maxTime(5000, TimeUnit.MILLISECONDS);
		options.returnDocument(ReturnDocument.AFTER);
		
		mongoClient = cfg.mongoClient();
		collection = mongoClient.getDatabase(cfg.dbName).getCollection("DMColor");
		collection.findOneAndUpdate(findColor,
				new Document("$set",
						new Document("Name", name)
						.append("Color", color)
						.append("LBT", lbt)
						.append("LBTCode", lbtcode)	
						),
				options);
		  mongoClient.close();
		
		responseStatus = new MspResponseStatus(0, "SUCCESS");
		rsp.setResponseStatus(responseStatus);
		return rsp;
		
		case Constants.MSG_ACTION_CODE.ACTIVE:
			objectId = new ObjectId(_id);
			 docFind = new Document("_id", objectId)
			.append("IsDelete", new Document("$ne", true));
			
			mongoClient = cfg.mongoClient();
			collection = mongoClient.getDatabase(cfg.dbName).getCollection("DMColor");
			  try {
			         docTmp = collection.find(docFind).allowDiskUse(true).iterator().next();      
			     } catch (Exception e) {
			        
			    }
			mongoClient.close();
			
			if(docTmp==null) {
				responseStatus = new MspResponseStatus(999, "Không tìm thấy thông tin.");
				rsp.setResponseStatus(responseStatus);
				return rsp;	
			}
			
			String code = docTmp.get("LBTCode", "");
			
			//KIEM TRA MA CODE DA TON TAI VA DC KICH HOAT CHUA
			
			Document docTmp2 = null;
			Document find_Active = new Document("IsDelete", new Document("$ne", true))
					.append("LBTCode", code)
					.append("Status", new Document("$ne", false));
			
			
			mongoClient = cfg.mongoClient();
			collection = mongoClient.getDatabase(cfg.dbName).getCollection("DMColor");
			  try {
				  docTmp2 = collection.find(find_Active).allowDiskUse(true).iterator().next();      
			     } catch (Exception e) {
			        
			    }
			mongoClient.close();
			
			if(docTmp2!=null) {				
				String check_id = docTmp2.getObjectId("_id").toString();		
				
				if(!check_id.equals(_id))
				{
				responseStatus = new MspResponseStatus(999, "Chỉ được kích hoạt 1 loại nút 1 lần. Vui lòng kiểm tra lại dữ liệu!!!");
				rsp.setResponseStatus(responseStatus);
				return rsp;	
				}
			}
			// END KIEM TRA TRANG THAI ACTIVE
			boolean isActive = false;
			if(null != docTmp.get("Status") && docTmp.get("Status") instanceof Boolean) {
				isActive = docTmp.getBoolean("Status", false);
			}
			
			options = new FindOneAndUpdateOptions();
			options.upsert(false);
			options.maxTime(5000, TimeUnit.MILLISECONDS);
			options.returnDocument(ReturnDocument.AFTER);
			 
				mongoClient = cfg.mongoClient();
				collection = mongoClient.getDatabase(cfg.dbName).getCollection("DMColor");
				collection.findOneAndUpdate(docFind,
						new Document("$set",
								new Document("Status", !isActive)	
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
			mongoClient = cfg.mongoClient();
			collection = mongoClient.getDatabase(cfg.dbName).getCollection("DMColor");
			  try {
			         docTmp = collection.find(docFind).allowDiskUse(true).iterator().next();      
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
				collection = mongoClient.getDatabase(cfg.dbName).getCollection("DMColor");
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
		MongoClient mongoClient = cfg.mongoClient();
		MongoCollection<Document> collection = mongoClient.getDatabase(cfg.dbName).getCollection("DMColor");
		      try {
		        docTmp = collection.find(docFind).allowDiskUse(true).iterator().next();    
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
