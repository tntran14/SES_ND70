package vn.sesgroup.hddt.user.impl;

import java.time.LocalDateTime;
import java.util.Iterator;
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
import vn.sesgroup.hddt.user.dao.ConfigEmailServerDAO;
import vn.sesgroup.hddt.utility.Constants;
import vn.sesgroup.hddt.utility.Json;

@Repository
@Transactional
public class ConfigEmailServerImpl extends AbstractDAO implements ConfigEmailServerDAO{
	private static final Logger log = LogManager.getLogger(ConfigEmailServerImpl.class);
	@Autowired
	ConfigConnectMongo cfg;
	
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
		String _id = commons.getTextJsonNode(jsonData.at("/_id")).replaceAll("\\s", "");
		String checkAutoSend = commons.getTextJsonNode(jsonData.at("/CheckAutoSend")).replaceAll("\\s", "");
		String checkSSL = commons.getTextJsonNode(jsonData.at("/CheckSSL")).replaceAll("\\s", "");
		String checkTLS = commons.getTextJsonNode(jsonData.at("/CheckTLS")).replaceAll("\\s", "");
		String smtpServer = commons.getTextJsonNode(jsonData.at("/SmtpServer")).replaceAll("\\s", "");
		String smtpPort = commons.getTextJsonNode(jsonData.at("/SmtpPort")).replaceAll("\\s", "");
		String emailAddress = commons.getTextJsonNode(jsonData.at("/EmailAddress")).replaceAll("\\s", "");
		String emailPassword = commons.getTextJsonNode(jsonData.at("/EmailPassword"));
		
		String mail = commons.getTextJsonNode(jsonData.at("/Mail"));
		String mailjet = "";
		String maill = "";
		if(mail.equals("Y")) {
			maill = "Y";
			mailjet = "N";
		}else {
			maill = "N";
			mailjet = "Y";
		}
		
		MsgRsp rsp = new MsgRsp(header);
		rsp.setMsgPage(page);
		MspResponseStatus responseStatus = null;
		
		Document docFind = null;
		Document docTmp = null;
		Document docUpsert = null;
		
		FindOneAndUpdateOptions options = null;
		/*KIEM TRA XEM THONG TIN ISSUER DA CO CHUA*/
		docFind = new Document("IssuerId", header.getIssuerId());
		

		MongoClient mongoClient = cfg.mongoClient();
		MongoCollection<Document> collection = mongoClient.getDatabase(cfg.dbName).getCollection("ConfigEmail");
		try {
			docTmp =   collection.find(docFind).allowDiskUse(true).iterator().next();		
		} catch (Exception e) {
			// TODO: handle exception
		}
			
		mongoClient.close();
		if(docTmp == null) {		//THEM MOI
			docUpsert = new Document("IssuerId", header.getIssuerId())
				.append("SmtpServer", smtpServer)
				.append("SmtpPort", commons.stringToInteger(smtpPort))
				.append("EmailAddress", emailAddress)
				.append("EmailPassword", emailPassword)
				.append("AutoSend", "Y".equals(checkAutoSend))
				.append("SSL", "Y".equals(checkSSL))
				.append("TLS", "Y".equals(checkTLS))
				.append("Mail",maill)
				.append("MailJet",mailjet)
				.append("InfoCreated", 
					new Document("CreateDate", LocalDateTime.now())
					.append("CreateUserID", header.getUserId())
					.append("CreateUserName", header.getUserName())
					.append("CreateUserFullName", header.getUserFullName())
				);
	
			mongoClient = cfg.mongoClient();
			collection = mongoClient.getDatabase(cfg.dbName).getCollection("ConfigEmail");
			collection.insertOne(docUpsert);			
			mongoClient.close();
		}else {						//UPDATE			
			options = new FindOneAndUpdateOptions();
			options.upsert(false);
			options.maxTime(5000, TimeUnit.MILLISECONDS);
			options.returnDocument(ReturnDocument.AFTER);
			
			docUpsert = new Document("SmtpServer", smtpServer)
				.append("SmtpPort", commons.stringToInteger(smtpPort))
				.append("EmailAddress", emailAddress)
				.append("AutoSend", "Y".equals(checkAutoSend))
				.append("SSL", "Y".equals(checkSSL))
				.append("TLS", "Y".equals(checkTLS))
				.append("Mail",maill)
				.append("MailJet",mailjet)
				.append("InfoUpdated", 
					new Document("UpdatedDate", LocalDateTime.now())
						.append("UpdatedUserID", header.getUserId())
						.append("UpdatedUserName", header.getUserName())
						.append("UpdatedUserFullName", header.getUserFullName())
				);
			if(!"".equals(emailPassword)) {
				docUpsert.append("EmailPassword", emailPassword);
			}
			
			MongoClient mongoClient2 = cfg.mongoClient();
			collection = mongoClient2.getDatabase(cfg.dbName).getCollection("ConfigEmail");
			collection.findOneAndUpdate(
					docFind, 
					new Document("$set", docUpsert),
					options
				);		
			mongoClient2.close();
			
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
		
	
		MongoClient mongoClient = cfg.mongoClient();
		MongoCollection<Document> collection = mongoClient.getDatabase(cfg.dbName).getCollection("ConfigEmail");
		try {
			docTmp =   collection.find(docFind).allowDiskUse(true).iterator().next();		
		} catch (Exception e) {
			// TODO: handle exception
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
