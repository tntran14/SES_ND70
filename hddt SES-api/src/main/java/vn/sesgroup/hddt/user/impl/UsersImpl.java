package vn.sesgroup.hddt.user.impl;

import java.util.ArrayList;
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
import vn.sesgroup.hddt.user.dao.UserDAO;
import vn.sesgroup.hddt.user.service.TCTNService;
import vn.sesgroup.hddt.utility.Constants;
import vn.sesgroup.hddt.utility.Json;

@Repository
@Transactional
public class UsersImpl extends AbstractDAO implements UserDAO {
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
		String serverinput = commons.getTextJsonNode(jsonData.at("/Password"));
		
		String mkc2 = "";
		if(serverinput.length() < 128) {
			 mkc2 = 0 + serverinput;
		}
		else {
			mkc2 = 	serverinput;
		}
		MsgRsp rsp = new MsgRsp(header);
		rsp.setMsgPage(page);
		MspResponseStatus responseStatus = null;
		Document docUpsert = null;
		FindOneAndUpdateOptions options = null;
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
				objectId = new ObjectId(header.getUserId());
			}catch(Exception e) {}
			docFind = new Document("IssuerId", header.getIssuerId())
					.append("IsDelete", new Document("$ne", true))
					.append("_id", objectId);
			pipeline = new ArrayList<Document>();
			pipeline.add(new Document("$match", docFind));
		
			
			cursor = mongoTemplate.getCollection("Users").aggregate(pipeline);
			iter = cursor.iterator();
			if(iter.hasNext()) {
				docTmp = iter.next();
			}
			
		
			
			options = new FindOneAndUpdateOptions();
			options.upsert(false);
			options.maxTime(5000, TimeUnit.MILLISECONDS);
			options.returnDocument(ReturnDocument.AFTER);
			
			docR = mongoTemplate.getCollection("Users").findOneAndUpdate(
				docFind,
				new Document("$set", 
					new Document("UserName", header.getUserName())
					.append("Password", mkc2)
					.append("FullName", header.getUserFullName())
				),
				options
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

		Document docFind = new Document("IssuerId", header.getIssuerId()).append("IsDelete", new Document("$ne", true))
				.append("_id", objectId);

		Document docTmp = null;
		Iterable<Document> cursor = mongoTemplate.getCollection("Users").find(docFind);
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

}
