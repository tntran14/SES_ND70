package vn.sesgroup.hddt.user.impl;

import java.util.ArrayList;
import java.util.Arrays;
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
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.FindOneAndUpdateOptions;
import com.mongodb.client.model.ReturnDocument;

import vn.sesgroup.hddt.configuration.ConfigConnectMongo;
import vn.sesgroup.hddt.user.dao.AbstractDAO;
import vn.sesgroup.hddt.user.dao.UserCheckDAO;
import vn.sesgroup.hddt.user.service.JPUtils;
import vn.sesgroup.hddt.user.service.TCTNService;
import vn.sesgroup.hddt.utility.Constants;
import vn.sesgroup.hddt.utility.Json;

@Repository
@Transactional
public class UserCheckImpl extends AbstractDAO implements UserCheckDAO {
	private static final Logger log = LogManager.getLogger(UserCheckImpl.class);
	@Autowired ConfigConnectMongo cfg;
	@Autowired TCTNService tctnService;

	@Autowired
	JPUtils jpUtils;
	Document docUpsert = null;
	@Override
	public MsgRsp save_user_check(JSONRoot jsonRoot) throws Exception {
		Msg msg = jsonRoot.getMsg();
		MsgHeader header = msg.getMsgHeader();
		MsgPage page = msg.getMsgPage();
		Object objData = msg.getObjData();

		JsonNode jsonData = null;
		if (objData != null) {
			jsonData = Json.serializer().nodeFromObject(msg.getObjData());
		} else {
			throw new Exception("Lỗi dữ liệu đầu vào");
		}

		String actionCode = header.getActionCode();
		String usercheck = commons.getTextJsonNode(jsonData.at("/User-check")).replaceAll("\\s", "");
		String mst = commons.getTextJsonNode(jsonData.at("/MST")).replaceAll("\\s", "");
		MsgRsp rsp = new MsgRsp(header);
		rsp.setMsgPage(page);
		MspResponseStatus responseStatus = null;
		FindOneAndUpdateOptions options = null;
		List<Document> pipeline = null;
		Document docTmp = null;		
		ObjectId objectId = null;
		

		switch (actionCode) {
		case Constants.MSG_ACTION_CODE.CREATED:

	
			/* KIEM TRA THONG TIN KHACH HANG - USERS */

			pipeline = new ArrayList<Document>();
			pipeline.add(new Document("$lookup", new Document("from", "Users")
					.append("pipeline", Arrays.asList(
			new Document("$match",
					new Document("UserName", mst)
					.append("IsDelete", new Document("$ne", true))
					.append("IsActive", true)),
			new Document("$project", new Document("_id", 1).append("IssuerId", 1))	
		))
			.append("as", "UserInfo")));
			pipeline.add(new Document("$unwind",
			new Document("path", "$UserInfo").append("preserveNullAndEmptyArrays", true)));			
			}
		pipeline.add(new Document("$lookup", new Document("from", "Users")
				.append("pipeline", Arrays.asList(
		new Document("$match",
				new Document("UserName", usercheck)
				.append("IsDelete", new Document("$ne", true))
				.append("IsActive", true)),
		new Document("$project", new Document("_id", 1))		
		
				))
				.append("as", "UserCheck")));
		pipeline.add(new Document("$unwind",
				new Document("path", "$UserCheck").append("preserveNullAndEmptyArrays", true)));
			
			MongoClient mongoClient = cfg.mongoClient();
			MongoDatabase dbmg = mongoClient.getDatabase(cfg.dbName);
			MongoCollection<Document> collection = dbmg.getCollection("Issuer");
			
			try {
				docTmp = collection.aggregate(pipeline).allowDiskUse(true).iterator().next();	
			} catch (Exception e) {
				
			}
					
			mongoClient.close();

			if (null == docTmp) {
				responseStatus = new MspResponseStatus(9999, "Không tìm thấy thông tin khách hàng.");
				rsp.setResponseStatus(responseStatus);
				return rsp;
			}
			if (docTmp.get("UserInfo") == null) {
				responseStatus = new MspResponseStatus(9999, "Mã số thuế không tồn tại trong hệ thống.");
				rsp.setResponseStatus(responseStatus);
				return rsp;
			}
			if (docTmp.get("UserCheck") == null) {
				responseStatus = new MspResponseStatus(9999, "Tài khoản USER-CHECK không đúng.");
				rsp.setResponseStatus(responseStatus);
				return rsp;
			}
			
			String IssuerId = docTmp.getEmbedded(Arrays.asList("UserInfo", "IssuerId"), "");
			String id_user_check = docTmp.getEmbedded(Arrays.asList("UserCheck", "_id"), ObjectId.class).toString();
			objectId = new ObjectId(id_user_check);
			options = new FindOneAndUpdateOptions();
			options.upsert(false);
			options.maxTime(5000, TimeUnit.MILLISECONDS);
			options.returnDocument(ReturnDocument.AFTER);
			Document docFind = new Document("_id", objectId).append("IsDelete", new Document("$ne", true));
			
			mongoClient = cfg.mongoClient();
			collection = mongoClient.getDatabase(cfg.dbName).getCollection("Users");
			collection.findOneAndUpdate(docFind,
					new Document("$set",
							new Document("IssuerId", IssuerId)),
					options);
			  mongoClient.close();

			responseStatus = new MspResponseStatus(0, "SUCCESS");
			rsp.setResponseStatus(responseStatus);
			return rsp;
	}
	


}
