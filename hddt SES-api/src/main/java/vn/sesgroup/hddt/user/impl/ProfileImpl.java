package vn.sesgroup.hddt.user.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.bson.Document;
import org.bson.types.ObjectId;
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
import vn.sesgroup.hddt.user.dao.ProfileDAO;
import vn.sesgroup.hddt.user.service.TCTNService;
import vn.sesgroup.hddt.utility.Constants;
import vn.sesgroup.hddt.utility.Json;

@Repository
@Transactional
public class ProfileImpl extends AbstractDAO implements ProfileDAO {
	@Autowired
	MongoTemplate mongoTemplate;
	@Autowired
	TCTNService tctnService;

	@Override
	public MsgRsp crud(JSONRoot jsonRoot) throws Exception {
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
		String t = commons.getTextJsonNode(jsonData.at("/TaxCode"));
		String n = commons.getTextJsonNode(jsonData.at("/Name"));
		String a = commons.getTextJsonNode(jsonData.at("/Address"));
		String p = commons.getTextJsonNode(jsonData.at("/Phone"));
		String f = commons.getTextJsonNode(jsonData.at("/Fax"));
		String e = commons.getTextJsonNode(jsonData.at("/Email"));
		String w = commons.getTextJsonNode(jsonData.at("/Website"));
		String ac = commons.getTextJsonNode(jsonData.at("/AccountNumber"));
		String an = commons.getTextJsonNode(jsonData.at("/AccountName"));
		String bn = commons.getTextJsonNode(jsonData.at("/BankName"));
		String boss = commons.getTextJsonNode(jsonData.at("/MainUser"));
		String cv = commons.getTextJsonNode(jsonData.at("/Position"));
		String ng = commons.getTextJsonNode(jsonData.at("/NameUser"));
		String eng = commons.getTextJsonNode(jsonData.at("/EmailUser"));
		String png = commons.getTextJsonNode(jsonData.at("/PhoneUser"));
		String englh = commons.getTextJsonNode(jsonData.at("/EmailUserLh"));
		String maCQT = commons.getTextJsonNode(jsonData.at("/MaCQT"));

		String tinhThanh = commons.getTextJsonNode(jsonData.at("/TinhThanh"));
		String cqtQLy = commons.getTextJsonNode(jsonData.at("/CqtQLy"));
	
		MsgRsp rsp = new MsgRsp(header);
		rsp.setMsgPage(page);
		MspResponseStatus responseStatus = null;
		Document docUpsert = null;
		FindOneAndUpdateOptions options = null;
		FindOneAndUpdateOptions options1 = null;
		Document docR = null;
		Document docFind = null;
		ObjectId objectId = null;
		List<Document> pipeline = null;
		Iterable<Document> cursor = null;
		Iterator<Document> iter = null;
		Document docTmp = null;
		
		
		
		
		
		
		
		switch (actionCode) {
		case Constants.MSG_ACTION_CODE.MODIFY:
			objectId = null;
			try {
				objectId = new ObjectId(header.getIssuerId());
			}catch(Exception ex) {}
			docFind = new Document("_id", header.getIssuerId())
					.append("IsDelete", new Document("$ne", true))
					.append("_id", objectId);
			pipeline = new ArrayList<Document>();
			pipeline.add(new Document("$match", docFind));
		
			pipeline.add(
					new Document("$lookup", 
						new Document("from", "DMTinhThanh")
						.append("pipeline", 
							Arrays.asList(
								new Document("$match", new Document("IsDelete", new Document("$ne", true)).append("code", commons.regexEscapeForMongoQuery(tinhThanh))),
								new Document("$project", new Document("_id", 0).append("code", 1).append("name", 1))
							)
						)
						.append("as", "DMTinhThanhInfo")
					)
				);
				pipeline.add(new Document("$unwind", new Document("path", "$DMTinhThanhInfo").append("preserveNullAndEmptyArrays", true)));
				pipeline.add(
					new Document("$lookup", 
						new Document("from", "DMChiCucThue")
						.append("pipeline", 
							Arrays.asList(
								new Document("$match", new Document("IsDelete", new Document("$ne", true)).append("code", commons.regexEscapeForMongoQuery(cqtQLy))),
								new Document("$project", new Document("_id", 0).append("code", 1).append("name", 1))
							)
						)
						.append("as", "DMChiCucThueInfo")
					)
				);
				pipeline.add(new Document("$unwind", new Document("path", "$DMChiCucThueInfo").append("preserveNullAndEmptyArrays", true)));
					
			cursor = mongoTemplate.getCollection("Issuer").aggregate(pipeline);
			iter = cursor.iterator();
			if(iter.hasNext()) {
				docTmp = iter.next();
			}
			
		
			
			options = new FindOneAndUpdateOptions();
			options.upsert(false);
			options.maxTime(5000, TimeUnit.MILLISECONDS);
			options.returnDocument(ReturnDocument.AFTER);
			
	
			docR = mongoTemplate.getCollection("Issuer").findOneAndUpdate(
					docFind,
					new Document("$set", 
						new Document("TaxCode", t)
						.append("Name", n)
						.append("Address", a)
						.append("Phone", p)
						.append("Fax", f)
						.append("Email", e)
						.append("Website", w)	
						.append("MaCQT", maCQT)
						
						.append("TinhThanhInfo", docTmp.get("DMTinhThanhInfo"))
						.append("ChiCucThueInfo", docTmp.get("DMChiCucThueInfo"))
						.append("MainUser", boss)
						.append("Position", cv)
						.append("BankAccount", 
								new Document("AccountNumber",ac)
									.append("AccountName", an)
									.append("BankName", bn)
							)
						.append("ContactUser", 
								new Document("NameUser",ng)
									.append("PhoneUser", png)
									.append("EmailUser", eng)
									.append("EmailUserLh", englh)
							)
					),
					options
				);
			
			
			//CẬP NHẬT USER
			
			options1 = new FindOneAndUpdateOptions();
			options1.upsert(false);
			options1.maxTime(5000, TimeUnit.MILLISECONDS);
			options1.returnDocument(ReturnDocument.AFTER);
			
			ObjectId id_user = new ObjectId(header.getUserId());
			Document findUser = new Document("_id", id_user)
					.append("IsDelete", false)
					.append("IssuerId", header.getIssuerId());
			Document docR1 = mongoTemplate.getCollection("Users").findOneAndUpdate(
					findUser,
					new Document("$set", 
						new Document("FullName", n)		
						.append("Phone", p)					
						.append("Email", e)
					),
					options1
				);
			
			
			
			
			/* CAP NHAT LAI TRANG THAI DANG CHO XU LY */
			responseStatus = new MspResponseStatus(0, "SUCCESS");
			rsp.setResponseStatus(responseStatus);
			return rsp;
		}
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
		} catch (Exception e) {
		}

		Document docFind = new Document("_id", header.getIssuerId()).append("IsDelete", new Document("$ne", true))
				.append("_id", objectId);

		Document docTmp = null;
		Iterable<Document> cursor = mongoTemplate.getCollection("Issuer").find(docFind);
		Iterator<Document> iter = cursor.iterator();
		if (iter.hasNext()) {
			docTmp = iter.next();
		}

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

	@Override
	public MsgRsp issu(JSONRoot jsonRoot, String _id) throws Exception {
		Msg msg = jsonRoot.getMsg();
		MsgHeader header = msg.getMsgHeader();
		MsgPage page = msg.getMsgPage();

		MsgRsp rsp = new MsgRsp(header);
		rsp.setMsgPage(page);
		MspResponseStatus responseStatus = null;

		String taxcode = _id;
		

		Document docFind = new Document("TaxCode", taxcode).append("IsDelete", new Document("$ne", true));


		Document docTmp = null;
		Iterable<Document> cursor = mongoTemplate.getCollection("Issuer").find(docFind);
		Iterator<Document> iter = cursor.iterator();
		if (iter.hasNext()) {
			docTmp = iter.next();
		}

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

	@Override
	public MsgRsp infoConfig(JSONRoot jsonRoot) throws Exception {
		// TODO Auto-generated method stub
		Msg msg = jsonRoot.getMsg();
		MsgHeader header = msg.getMsgHeader();
		MsgPage page = msg.getMsgPage();

		MsgRsp rsp = new MsgRsp(header);
		rsp.setMsgPage(page);
		MspResponseStatus responseStatus = null;

		Document docFind = new Document("IsDelete", new Document("$ne", true));


		Document docTmp = null;
		Iterable<Document> cursor = mongoTemplate.getCollection("ConfigInfoAdmin").find(docFind);
		Iterator<Document> iter = cursor.iterator();
		if (iter.hasNext()) {
			docTmp = iter.next();
		}

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

	
	
	public MsgRsp reset_issu(JSONRoot jsonRoot) throws Exception {
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
		String t = commons.getTextJsonNode(jsonData.at("/TaxCode"));		
		MsgRsp rsp = new MsgRsp(header);
		rsp.setMsgPage(page);
		MspResponseStatus responseStatus = null;
		Iterable<Document> cursor = null;
		Iterator<Document> iter = null;
		Document docTmp = null;
		
				
		Document findIssu = new Document("TaxCode", t).append("IsActive", true).append("IsDelete", new Document("$ne", true));
		cursor = mongoTemplate.getCollection("Issuer").find(findIssu);
		iter = cursor.iterator();
		if (iter.hasNext()) {
			docTmp = iter.next();
		}			
		
		ObjectId id_issu = docTmp.getObjectId("_id");
			responseStatus = new MspResponseStatus(0, id_issu.toString());
			rsp.setResponseStatus(responseStatus);
			rsp.setObjData(docTmp);
			return rsp;	
	}

}
