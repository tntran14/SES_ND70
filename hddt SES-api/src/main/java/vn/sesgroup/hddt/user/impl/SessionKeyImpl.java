package vn.sesgroup.hddt.user.impl;

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
import vn.sesgroup.hddt.user.dao.SessionKeyDAO;
import vn.sesgroup.hddt.user.service.JPUtils;
import vn.sesgroup.hddt.user.service.TCTNService;
import vn.sesgroup.hddt.utility.Constants;
import vn.sesgroup.hddt.utility.Json;

@Repository
@Transactional
public class SessionKeyImpl extends AbstractDAO implements SessionKeyDAO {
	private static final Logger log = LogManager.getLogger(SessionKeyImpl.class);
	@Autowired ConfigConnectMongo cfg;
	@Autowired TCTNService tctnService;
	@Autowired JPUtils jpUtils;
	
	public MsgRsp list(JSONRoot jsonRoot) throws Exception {
		Msg msg = jsonRoot.getMsg();
		MsgHeader header = msg.getMsgHeader();
		MsgPage page = msg.getMsgPage();
		Object objData = msg.getObjData();

		String mst = "";
		String name = "";
		JsonNode jsonData = null;
		if (objData != null) {
			jsonData = Json.serializer().nodeFromObject(objData);

			mst = commons.getTextJsonNode(jsonData.at("/mst")).replaceAll("\\s", "");
			name = commons.getTextJsonNode(jsonData.at("/name")).replaceAll("\\s", " ");
		}

		MsgRsp rsp = new MsgRsp(header);
		MspResponseStatus responseStatus = null;

		ObjectId objectId = null;
		Document docTmp = null;
		List<Document> pipeline = new ArrayList<Document>();


		Document docMatch = new Document("IsDelete",
				new Document("$ne", true));
		if (!"".equals(mst))
			docMatch.append("TaxCode",
					new Document("$regex", commons.regexEscapeForMongoQuery(mst)).append("$options", "i"));
		if (!"".equals(name))
			docMatch.append("Name",
					new Document("$regex", commons.regexEscapeForMongoQuery(name)).append("$options", "i"));
		
		
		
		pipeline = new ArrayList<Document>();
		pipeline.add(new Document("$match", docMatch));		
		pipeline.add(new Document("$sort",
				new Document("_id", -1)					
				));
		
		pipeline.addAll(createFacetForSearchNotSort(page));
		
		MongoClient mongoClient = cfg.mongoClient();
		MongoCollection<Document> collection = mongoClient.getDatabase(cfg.dbName).getCollection("UsersSessionKey");
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
				hItem.put("TaxCode", doc.get("TaxCode"));
				hItem.put("SessionKey", doc.get("SessionKey"));		
				hItem.put("EffectDate", doc.get("EffectDate"));			
				hItem.put("ExpireDate", doc.get("ExpireDate"));		
				hItem.put("Name", doc.get("Name"));	
				hItem.put("IpAddress", doc.get("IpAddress"));	
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
		FindOneAndUpdateOptions options2 = null;
		List<Document> pipeline = null;
		Document docTmp = null;
		Document docTmp1 = null;

		
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
		String mst = commons.getTextJsonNode(jsonData.at("/TaxCode")).replaceAll("\\s", "");
		String name = commons.getTextJsonNode(jsonData.at("/Name"));
		String IpAddress = commons.getTextJsonNode(jsonData.at("/IpAddress")).replaceAll("\\s", "");
		String effectDate = commons.getTextJsonNode(jsonData.at("/EffectDate")).replaceAll("\\s", "");
		String expireDate = commons.getTextJsonNode(jsonData.at("/ExpireDate")).replaceAll("\\s", "");
		String userSession = commons.csRandomAlphaNumbericString(50);
			
		switch (actionCode) {
		

		case Constants.MSG_ACTION_CODE.CREATED:		
			objectId = new ObjectId();
			
			Document findMST = new Document("TaxCode", mst)
			.append("IsActive",  new Document("$ne", false))
			.append("IsDelete",  new Document("$ne", true));
			
			pipeline = new ArrayList<Document>();
			pipeline.add(new Document("$match", findMST));
			
			MongoClient mongoClient = cfg.mongoClient();
			MongoCollection<Document> collection = mongoClient.getDatabase(cfg.dbName).getCollection("Issuer");
			      try {
			        docTmp = collection.aggregate(pipeline).allowDiskUse(true).iterator().next();    
			      } catch (Exception e) {
			        
			      }
			        
			mongoClient.close();
			
			if(docTmp==null) {
				responseStatus = new MspResponseStatus(999, "Mã số thuế không tồn tại.");
				rsp.setResponseStatus(responseStatus);
				return rsp;	
			}	
			

			Document findUser = new Document("UserName", mst)
			.append("IsActive",  new Document("$ne", false))
			.append("IsDelete",  new Document("$ne", true));
			
			pipeline = new ArrayList<Document>();
			pipeline.add(new Document("$match", findUser));
			mongoClient = cfg.mongoClient();
			collection = mongoClient.getDatabase(cfg.dbName).getCollection("Users");
			  try {
				  docTmp1 = collection.aggregate(pipeline).allowDiskUse(true).iterator().next();      
			     } catch (Exception e) {
			        
			    }
			mongoClient.close();
			
			String full_name = docTmp1.get("FullName", "");
			String IssuerId = docTmp1.get("IssuerId", "");			

			Document docUpsert1 = new Document("_id", objectId)
					.append("IssuerId",IssuerId)
					.append("TaxCode", mst)
					.append("Name", full_name)	
					.append("IpAddress", IpAddress)	
					.append("SessionKey", userSession)
					.append("EffectDate", commons.convertStringToLocalDate(effectDate, Constants.FORMAT_DATE.FORMAT_DATE_WEB))
					.append("ExpireDate", commons.convertStringToLocalDate(expireDate, Constants.FORMAT_DATE.FORMAT_DATE_WEB))
					.append("IsDelete", false);
					
				/*END - LUU DU LIEU*/

				mongoClient = cfg.mongoClient();
				collection = mongoClient.getDatabase(cfg.dbName).getCollection("UsersSessionKey");
				collection.insertOne(docUpsert1);      
				mongoClient.close();
			
			responseStatus = new MspResponseStatus(0, "SUCCESS");
			rsp.setResponseStatus(responseStatus);
			return rsp;
		
		case Constants.MSG_ACTION_CODE.MODIFY:
			
		objectId = new ObjectId(_id);
		Document docFind = new Document("_id", objectId)
				.append("IsDelete", new Document("$ne", true));
		
		pipeline = new ArrayList<Document>();
		pipeline.add(new Document("$match", docFind));
		mongoClient = cfg.mongoClient();
		collection = mongoClient.getDatabase(cfg.dbName).getCollection("UsersSessionKey");
		  try {
			  docTmp1 = collection.aggregate(pipeline).allowDiskUse(true).iterator().next();      
		     } catch (Exception e) {
		        
		    }
		mongoClient.close();
		
		if(docTmp1==null) {
			responseStatus = new MspResponseStatus(999, "Không tìm thấy thông tin.");
			rsp.setResponseStatus(responseStatus);
			return rsp;	
		}
			
		
		String taxcode = docTmp1.get("TaxCode", "");
		//String name = docTmp1.get("Name", "");
		String SessionKey = docTmp1.get("SessionKey", "");			
					options2 = new FindOneAndUpdateOptions();
					options2.upsert(false);
					options2.maxTime(5000, TimeUnit.MILLISECONDS);
					options2.returnDocument(ReturnDocument.AFTER);
						
					mongoClient = cfg.mongoClient();
					collection = mongoClient.getDatabase(cfg.dbName).getCollection("UsersSessionKey");
					collection.findOneAndUpdate(docFind,
							new Document("$set",
									new Document("TaxCode", taxcode)	
									.append("Name", name)	
									.append("IpAddress", IpAddress)	
									.append("SessionKey", SessionKey)
									.append("EffectDate", commons.convertStringToLocalDate(effectDate, Constants.FORMAT_DATE.FORMAT_DATE_WEB))
									.append("ExpireDate", commons.convertStringToLocalDate(expireDate, Constants.FORMAT_DATE.FORMAT_DATE_WEB))
									),
							options2);
					  mongoClient.close();
		

		responseStatus = new MspResponseStatus(0, "SUCCESS");
		rsp.setResponseStatus(responseStatus);
		return rsp;
		
		case Constants.MSG_ACTION_CODE.DELETE:
		objectId = new ObjectId(_id);	
		 docFind = new Document("_id", objectId)
		.append("IsDelete", new Document("$ne", true));
		
		pipeline = new ArrayList<Document>();
		pipeline.add(new Document("$match", docFind));
		mongoClient = cfg.mongoClient();
		collection = mongoClient.getDatabase(cfg.dbName).getCollection("UsersSessionKey");
		      try {
		        docTmp = collection.aggregate(pipeline).allowDiskUse(true).iterator().next();    
		      } catch (Exception e) {
		        
		      }
		        
		mongoClient.close();
		
		if(docTmp==null) {
			responseStatus = new MspResponseStatus(999, "Không tìm thấy mẫu số ký hiệu.");
			rsp.setResponseStatus(responseStatus);
			return rsp;	
		}
		
		
		options = new FindOneAndUpdateOptions();
		options.upsert(false);
		options.maxTime(5000, TimeUnit.MILLISECONDS);
		options.returnDocument(ReturnDocument.AFTER);

		
		mongoClient = cfg.mongoClient();
		collection = mongoClient.getDatabase(cfg.dbName).getCollection("UsersSessionKey");
		collection.findOneAndUpdate(docFind,
				new Document("$set",
						new Document("IsDelete", true)	
						),
				options);
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
		MongoCollection<Document> collection = mongoClient.getDatabase(cfg.dbName).getCollection("UsersSessionKey");
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

	
	///
	
	@Override
	public MsgRsp check(JSONRoot jsonRoot) throws Exception {
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
		
		String mst = commons.getTextJsonNode(jsonData.at("/MST")).replaceAll("\\s", "");
		
	
		MsgRsp rsp = new MsgRsp(header);
		rsp.setMsgPage(page);
		MspResponseStatus responseStatus = null;
		Document docTmp = null;
		Document docTmp1 = null;
		List<Document> pipeline = null;
		
		String actionCode = header.getActionCode();
		switch (actionCode) {
		case Constants.MSG_ACTION_CODE.CREATED:
			pipeline = new ArrayList<Document>();
			
			Document findMST = new Document("TaxCode", mst).append("IsActive", new Document("$ne", false)).append("IsDelete", new Document("$ne", true));
				
			pipeline = new ArrayList<Document>();
			pipeline.add(new Document("$match", findMST));
			MongoClient mongoClient = cfg.mongoClient();
			MongoCollection<Document> collection = mongoClient.getDatabase(cfg.dbName).getCollection("Issuer");
			      try {
			        docTmp = collection.aggregate(pipeline).allowDiskUse(true).iterator().next();    
			      } catch (Exception e) {
			        
			      }
			        
			mongoClient.close();
			
			if(null == docTmp) {
				responseStatus = new MspResponseStatus(9999, "Không tìm thấy thông tin khách hàng.");
				rsp.setResponseStatus(responseStatus);
				return rsp;
			}
			
			
			//CHECK TON TAI
			Document findSessionKey = new Document("TaxCode", mst).append("IsDelete", new Document("$ne", true));
			pipeline = new ArrayList<Document>();
			pipeline.add(new Document("$match", findSessionKey));
			mongoClient = cfg.mongoClient();
			collection = mongoClient.getDatabase(cfg.dbName).getCollection("UsersSessionKey");
			      try {
			    	  docTmp1 = collection.aggregate(pipeline).allowDiskUse(true).iterator().next();    
			      } catch (Exception e) {
			        
			      }
			        
			mongoClient.close();
			
			if(null != docTmp1) {
				responseStatus = new MspResponseStatus(9999, "Mã số thuế đã có session key. vui lòng kiểm tra lại!!!");
				rsp.setResponseStatus(responseStatus);
				return rsp;
			}
			
			//END CHECK TON TAI
			
			
			
			String name = docTmp.get("Name","");
			String th = "";
			th = mst +"," +name;	
			
			responseStatus = new MspResponseStatus(0, th);
			rsp.setResponseStatus(responseStatus);
			
			return rsp;
			
			
		default:
			responseStatus = new MspResponseStatus(9998, Constants.MAP_ERROR.get(9998));
			rsp.setResponseStatus(responseStatus);
			return rsp;
		}
	}
	
	
	//
	
}
