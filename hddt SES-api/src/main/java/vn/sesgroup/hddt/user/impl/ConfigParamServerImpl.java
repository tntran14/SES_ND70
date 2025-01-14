package vn.sesgroup.hddt.user.impl;

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
import vn.sesgroup.hddt.user.dao.ConfigParamDAO;
import vn.sesgroup.hddt.utility.Constants;
import vn.sesgroup.hddt.utility.Json;

@Repository
@Transactional
public class ConfigParamServerImpl extends AbstractDAO implements ConfigParamDAO{
	private static final Logger log = LogManager.getLogger(ConfigParamServerImpl.class);
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
		String VND = commons.getTextJsonNode(jsonData.at("/VND")).replaceAll("\\s", "");
		String USD = commons.getTextJsonNode(jsonData.at("/USD"));
		String viewshd = commons.getTextJsonNode(jsonData.at("/viewshd"));
		String viewmoney = commons.getTextJsonNode(jsonData.at("/viewmoney"));
		String namecd = commons.getTextJsonNode(jsonData.at("/namecd"));
		String footermail = commons.getTextJsonNode(jsonData.at("/footermail"));		
		String tax_invoice = commons.getTextJsonNode(jsonData.at("/TaxInvoice"));
		
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
		
	
		
		MongoClient mongoClient = cfg.mongoClient();
		MongoCollection<Document> collection = mongoClient.getDatabase(cfg.dbName).getCollection("UserConFig");
		try {
			docTmp =   collection.find(docFind).allowDiskUse(true).iterator().next();		
		} catch (Exception e) {
			// TODO: handle exception
		}
			
		mongoClient.close();
		
	
		if(docTmp == null) {		//THEM MOI
			docUpsert = new Document("IssuerId", header.getIssuerId())
				.append("VND", VND)
				.append("USD",USD)
				.append("footermail",footermail)
				.append("viewshd",viewshd)
				.append("viewmoney",viewmoney)
			.append("NameCD",namecd)
			.append("TaxInvoice", tax_invoice);
		
		
			mongoClient = cfg.mongoClient();
			collection = mongoClient.getDatabase(cfg.dbName).getCollection("UserConFig");
			collection.insertOne(docUpsert);			
			mongoClient.close();
			
			
		}else {						//UPDATE			
			options = new FindOneAndUpdateOptions();
			options.upsert(false);
			options.maxTime(5000, TimeUnit.MILLISECONDS);
			options.returnDocument(ReturnDocument.AFTER);
			
			docUpsert = new Document("USD", USD)
					.append("VND", VND)
					.append("footermail",footermail)
					.append("viewshd",viewshd)
					.append("viewmoney",viewmoney)
					.append("NameCD",namecd)
					.append("TaxInvoice", tax_invoice)
					;
		
			
			MongoClient mongoClient2 = cfg.mongoClient();
			collection = mongoClient2.getDatabase(cfg.dbName).getCollection("UserConFig");
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

	@Override
	public MsgRsp check(JSONRoot jsonRoot) throws Exception {
		Msg msg = jsonRoot.getMsg();
		MsgHeader header = msg.getMsgHeader();
		MsgPage page = msg.getMsgPage();

		MsgRsp rsp = new MsgRsp(header);
		rsp.setMsgPage(page);
		MspResponseStatus responseStatus = null;

	

		Document docFind = new Document("IssuerId", header.getIssuerId());

		Document docTmp = null;
	
		MongoClient mongoClient = cfg.mongoClient();
		MongoCollection<Document> collection = mongoClient.getDatabase(cfg.dbName).getCollection("UserConFig");
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
