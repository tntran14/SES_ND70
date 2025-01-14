package vn.sesgroup.hddt.user.impl;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bson.Document;
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
import vn.sesgroup.hddt.user.dao.QLLinkDao;
import vn.sesgroup.hddt.utility.Constants;
import vn.sesgroup.hddt.utility.Json;

@Repository
@Transactional
public class QLLinkImpl extends AbstractDAO implements QLLinkDao{
	private static final Logger log = LogManager.getLogger(QLLinkImpl.class);
	@Autowired ConfigConnectMongo cfg;
	
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
		
		String _id = commons.getTextJsonNode(jsonData.at("/_id")).replaceAll("\\s", "");
		String LinkLogin = commons.getTextJsonNode(jsonData.at("/LinkLogin"));
		String LinkPortal = commons.getTextJsonNode(jsonData.at("/LinkPortal"));
	
	
		MsgRsp rsp = new MsgRsp(header);
		rsp.setMsgPage(page);
		MspResponseStatus responseStatus = null;
		
		Document docFind = null;
		Document docTmp = null;
		Document docUpsert = null;
		
		FindOneAndUpdateOptions options = null;
		/*KIEM TRA XEM THONG TIN ISSUER DA CO CHUA*/
		docFind = new Document("IsDelete",false);
		
		List<Document> pipeline = null;
		pipeline = new ArrayList<Document>();
		pipeline.add(new Document("$match", docFind));
		MongoClient mongoClient = cfg.mongoClient();
		MongoCollection<Document> collection = mongoClient.getDatabase(cfg.dbName).getCollection("PramLink");
		      try {
		        docTmp = collection.aggregate(pipeline).allowDiskUse(true).iterator().next();    
		      } catch (Exception e) {
		        
		      }
		        
		mongoClient.close();
		
		if(docTmp == null) {		//THEM MOI
			docUpsert = new Document("IssuerId", header.getIssuerId())
				.append("LinkLogin", LinkLogin)
				.append("LinkPortal",LinkPortal)				
				.append("InfoCreated", 
					new Document("CreateDate", LocalDateTime.now())
					.append("CreateUserID", header.getUserId())
					.append("CreateUserName", header.getUserName())
					.append("CreateUserFullName", header.getUserFullName())
				);

			mongoClient = cfg.mongoClient();
			collection = mongoClient.getDatabase(cfg.dbName).getCollection("PramLink");
			collection.insertOne(docUpsert);      
			mongoClient.close();
			
		}else {						//UPDATE			
			options = new FindOneAndUpdateOptions();
			options.upsert(false);
			options.maxTime(5000, TimeUnit.MILLISECONDS);
			options.returnDocument(ReturnDocument.AFTER);
			
			docUpsert = new Document("LinkLogin", LinkLogin)
				.append("LinkPortal", LinkPortal)			
				.append("InfoUpdated", 
					new Document("UpdatedDate", LocalDateTime.now())
						.append("UpdatedUserID", header.getUserId())
						.append("UpdatedUserName", header.getUserName())
						.append("UpdatedUserFullName", header.getUserFullName())
				);
	
			MongoClient mongoClient1 = cfg.mongoClient();
			collection = mongoClient1.getDatabase(cfg.dbName).getCollection("PramLink");
			collection.findOneAndUpdate(docFind,
					new Document("$set", docUpsert),
					options
				); 
			mongoClient1.close();
		}
		
		responseStatus = new MspResponseStatus(0, "SUCCESS");
		rsp.setResponseStatus(responseStatus);
		return rsp;
	}


	@Override
	public MsgRsp detail(JSONRoot jsonRoot) throws Exception {
		Msg msg = jsonRoot.getMsg();
		MsgHeader header = msg.getMsgHeader();
		MsgPage page = msg.getMsgPage();
		
		MsgRsp rsp = new MsgRsp(header);
		rsp.setMsgPage(page);
		MspResponseStatus responseStatus = null;
		
		Document docTmp = null;
		Document docFind = new Document("IsDelete", false);
		
		List<Document> pipeline = null;
		pipeline = new ArrayList<Document>();
		pipeline.add(new Document("$match", docFind));
		MongoClient mongoClient = cfg.mongoClient();
		MongoCollection<Document> collection = mongoClient.getDatabase(cfg.dbName).getCollection("PramLink");
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
