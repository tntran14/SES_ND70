package vn.sesgroup.hddt.user.impl;

import java.time.LocalDateTime;
import java.util.Iterator;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bson.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.api.message.JSONRoot;
import com.api.message.Msg;
import com.api.message.MsgHeader;
import com.api.message.MsgPage;
import com.api.message.MsgRsp;
import com.api.message.MspResponseStatus;
import com.fasterxml.jackson.databind.JsonNode;
import com.mongodb.client.model.FindOneAndUpdateOptions;
import com.mongodb.client.model.ReturnDocument;

import vn.sesgroup.hddt.user.dao.AbstractDAO;
import vn.sesgroup.hddt.user.dao.ConfigEmailMailJetDAO;
import vn.sesgroup.hddt.utility.Constants;
import vn.sesgroup.hddt.utility.Json;

@Repository
@Transactional
public class ConfigEmailMailJetImpl extends AbstractDAO implements ConfigEmailMailJetDAO{
	private static final Logger log = LogManager.getLogger(ConfigEmailMailJetImpl.class);
	@Autowired MongoTemplate mongoTemplate;
	
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
		String nch = commons.getTextJsonNode(jsonData.at("/NameConfig")).replaceAll("\\s", "");
		String md = commons.getTextJsonNode(jsonData.at("/Default")).replaceAll("\\s", "");
		String lm = commons.getTextJsonNode(jsonData.at("/LoaiMail")).replaceAll("\\s", "");
		String ak = commons.getTextJsonNode(jsonData.at("/ApiKey")).replaceAll("\\s", "");
		String sk = commons.getTextJsonNode(jsonData.at("/SecretKey")).replaceAll("\\s", "");
		String ng = commons.getTextJsonNode(jsonData.at("/NameSend")).replaceAll("\\s", "");
		String am = commons.getTextJsonNode(jsonData.at("/EmailAddress")).replaceAll("\\s", "");

		
		
		MsgRsp rsp = new MsgRsp(header);
		rsp.setMsgPage(page);
		MspResponseStatus responseStatus = null;
		
		Document docFind = null;
		Document docTmp = null;
		Document docUpsert = null;
		
		FindOneAndUpdateOptions options = null;
		Iterable<Document> cursor = null;
		Iterator<Document> iter = null;
		/*KIEM TRA XEM THONG TIN ISSUER DA CO CHUA*/
		docFind = new Document("IssuerId", header.getIssuerId());
		cursor = mongoTemplate.getCollection("ConfigEmailMailjet").find(docFind);
		iter = cursor.iterator();
		if(iter.hasNext()) {
			docTmp = iter.next();
		}
		
		if(docTmp == null) {		//THEM MOI
			docUpsert = new Document("IssuerId", header.getIssuerId())
				.append("NameConfig", nch)
				.append("Default", md)
				.append("LoaiMail", lm)
				.append("ApiKey", ak)
				.append("SecretKey",sk)
				.append("NameSend",ng)
				.append("EmailAddress",am)
				.append("InfoCreated", 
					new Document("CreateDate", LocalDateTime.now())
					.append("CreateUserID", header.getUserId())
					.append("CreateUserName", header.getUserName())
					.append("CreateUserFullName", header.getUserFullName())
				);
			mongoTemplate.getCollection("ConfigEmailMailjet").insertOne(docUpsert);	
		}else {						//UPDATE			
			options = new FindOneAndUpdateOptions();
			options.upsert(false);
			options.maxTime(5000, TimeUnit.MILLISECONDS);
			options.returnDocument(ReturnDocument.AFTER);
			
			docUpsert = new Document("NameConfig", nch)
					.append("Default", md)
					.append("LoaiMail", lm)
					.append("ApiKey", ak)
					.append("SecretKey",sk)
					.append("NameSend",ng)
					.append("EmailAddress",am)
				.append("InfoUpdated", 
					new Document("UpdatedDate", LocalDateTime.now())
						.append("UpdatedUserID", header.getUserId())
						.append("UpdatedUserName", header.getUserName())
						.append("UpdatedUserFullName", header.getUserFullName())
				);
		
			mongoTemplate.getCollection("ConfigEmailMailjet").findOneAndUpdate(
				docFind, 
				new Document("$set", docUpsert),
				options
			);
		}
		
		responseStatus = new MspResponseStatus(0, "SUCCESS");
		rsp.setResponseStatus(responseStatus);
		return rsp;
	}

/*
db.getCollection('ConfigEmail').find({IssuerId: '61b851ebb0228bba71fca2ec'})
 * */
	
	@Override
	public MsgRsp detail(JSONRoot jsonRoot) throws Exception {
		Msg msg = jsonRoot.getMsg();
		MsgHeader header = msg.getMsgHeader();
		MsgPage page = msg.getMsgPage();
		
		MsgRsp rsp = new MsgRsp(header);
		rsp.setMsgPage(page);
		MspResponseStatus responseStatus = null;
		
		Document docTmp = null;
		Document docFind = new Document("IssuerId", header.getIssuerId());
		
		Iterable<Document> cursor = mongoTemplate.getCollection("ConfigEmailMailjet").find(docFind);
		Iterator<Document> iter = cursor.iterator();
		if(iter.hasNext()) {
			docTmp = iter.next();
		}
		
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
