package vn.sesgroup.hddt.user.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

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

import vn.sesgroup.hddt.user.dao.AbstractDAO;
import vn.sesgroup.hddt.user.dao.StatisticTCGPTCTNDAO;
import vn.sesgroup.hddt.utility.Constants;
import vn.sesgroup.hddt.utility.Json;

@Repository
@Transactional
public class StatisticTCGPTNCNImpl extends AbstractDAO implements StatisticTCGPTCTNDAO {
	@Autowired
	MongoTemplate mongoTemplate;

	private void buildDocMatch(String taxCode, String customerName, Document docMatch) {
		if (taxCode != null && taxCode.trim().length() > 0) {
			docMatch.append("MST", taxCode);
		}

		if (customerName != null && customerName.trim().length() > 0) {
			docMatch.append("TenNnt", new Document("$regex", customerName).append("$options", "i"));
		}
		docMatch.append("Status", Constants.INVOICE_STATUS.COMPLETE);
	}

	@Override
	public MsgRsp list(JSONRoot jsonRoot) throws Exception {
		Msg msg = jsonRoot.getMsg();
		MsgHeader header = msg.getMsgHeader();
		MsgPage page = msg.getMsgPage();
		Object objData = msg.getObjData();

		String msthue = "";
		String tkhang = "";
		JsonNode jsonData = null;
		if (objData != null) {
			jsonData = Json.serializer().nodeFromObject(objData);
			msthue = commons.getTextJsonNode(jsonData.at("/MSThue")).replaceAll("\\s", "");
			tkhang = commons.getTextJsonNode(jsonData.at("/TKHang")).replaceAll("\\s", " ");
		}

		MsgRsp rsp = new MsgRsp(header);
		MspResponseStatus responseStatus = null;

		Iterator<Document> iter = null;
		List<Document> docs = new ArrayList<Document>();

		Document docMatch = new Document();
		buildDocMatch(msthue, tkhang, docMatch);

		List<Document> pipeline = new ArrayList<Document>();
		pipeline.add(new Document("$match", docMatch));
		pipeline.add(new Document("$project", new Document("IssuerId", 1).append("MST", 1).append("TenNnt", 1)
				.append("DSTCGPhap", 1).append("DSTCTNhan", 1).append("Status", 1)));

		pipeline.add(new Document("$sort", new Document("statusPriority", -1).append("InfoCreated.CreateDate", -1)));
		pipeline.add(
				new Document("$group", new Document("_id", new Document("IssuerId", "$IssuerId").append("MST", "$MST"))
						.append("doc", new Document("$first", "$$ROOT"))));
		try {
			iter = mongoTemplate.getCollection("DMTKhai").aggregate(pipeline).allowDiskUse(true).iterator();
			while (iter.hasNext()) {
				docs.add(iter.next());
			}
		} catch (Exception e) {
		}

		if (docs.size() == 0) {
			responseStatus = new MspResponseStatus(999, Constants.MAP_ERROR.get(999));
			rsp.setResponseStatus(responseStatus);
			return rsp;
		}
		page.setTotalRows(docs.size());
		rsp.setMsgPage(page);

		List<HashMap<String, Object>> response = new ArrayList<HashMap<String, Object>>();
		HashMap<String, Object> resItem = null;
		ObjectId objectId = null;
		for (Document doc : docs) {
			Document docc = (Document) doc.get("doc");
			resItem = new HashMap<String, Object>();
			
			objectId = (ObjectId) docc.get("_id");
			resItem.put("_id", objectId.toString());
			resItem.put("IssuerId", docc.get("IssuerId", String.class));
			resItem.put("TenNnt", docc.get("TenNnt", String.class));
			resItem.put("MST", docc.get("MST", String.class));
			resItem.put("DSTCGPhap", docc.get("DSTCGPhap", Object.class));
			resItem.put("DSTCTNhan", docc.get("DSTCTNhan", Object.class));
			response.add(resItem);
		}

		responseStatus = new MspResponseStatus(0, "Lấy thông tin thành công.");
		rsp.setResponseStatus(responseStatus);
		rsp.setObjData(response);
		return rsp;
	}
}
