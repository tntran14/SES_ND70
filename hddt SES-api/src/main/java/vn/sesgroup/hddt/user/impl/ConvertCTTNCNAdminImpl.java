package vn.sesgroup.hddt.user.impl;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
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
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.FindOneAndUpdateOptions;
import com.mongodb.client.model.ReturnDocument;

import vn.sesgroup.hddt.configuration.ConfigConnectMongo;
import vn.sesgroup.hddt.user.dao.AbstractDAO;
import vn.sesgroup.hddt.user.dao.ConvertCTTNCNAdminDAO;
import vn.sesgroup.hddt.user.service.TCTNService;
import vn.sesgroup.hddt.utility.Constants;
import vn.sesgroup.hddt.utility.Json;

@Repository
public class ConvertCTTNCNAdminImpl extends AbstractDAO implements ConvertCTTNCNAdminDAO{
	@Autowired ConfigConnectMongo cfg;
	@Autowired TCTNService tctnService;
	@Autowired MongoTemplate mongoTemplate;

	@Override
	public MsgRsp list(JSONRoot jsonRoot) throws Exception {
		Msg msg = jsonRoot.getMsg();
		MsgHeader header = msg.getMsgHeader();
		MsgPage page = msg.getMsgPage();
		Object objData = msg.getObjData();
		
		MsgRsp rsp = new MsgRsp(header);
		MspResponseStatus responseStatus = null;

		String mst = "";
		JsonNode jsonData = null;
		if (objData != null) {
			jsonData = Json.serializer().nodeFromObject(objData);
			mst = commons.getTextJsonNode(jsonData.at("/MST")).replaceAll("\\s", "");
		}

		ObjectId objectId = null;
		Document docTmp = null;
		Document docFind = null;
		List<Document> pipeline = new ArrayList<Document>();
		
		String issuerId = "";
		
		if (!mst.equals("")) {
			docFind = new Document("TaxCode", mst).append("IsDelete", new Document("$ne", true));

			pipeline.clear();
			pipeline.add(new Document("$match", docFind));
			docTmp = mongoTemplate.getCollection("Issuer").aggregate(pipeline).allowDiskUse(true).iterator().next();
			if (docTmp == null) {
				responseStatus = new MspResponseStatus(9999, "Không tìm thấy thông tin mã số thuế.");
				rsp.setResponseStatus(responseStatus);
				return rsp;
			}
			issuerId = docTmp.getObjectId("_id").toString();
		}

		docFind = new Document("IsDelete", new Document("$ne", true)).append("IsActive", true);

		if (!"".equals(issuerId))
			docFind.append("IssuerId", commons.regexEscapeForMongoQuery(issuerId));
		pipeline = new ArrayList<Document>();
		pipeline.add(new Document("$match", docFind));
		pipeline.add(new Document("$sort", new Document("_id", -1)));
		pipeline.addAll(createFacetForSearchNotSort(page));

		docTmp = mongoTemplate.getCollection("DMMSTNCN").aggregate(pipeline).allowDiskUse(true).iterator().next();

		if (null == docTmp) {
			responseStatus = new MspResponseStatus(9999, Constants.MAP_ERROR.get(9999));
			rsp.setResponseStatus(responseStatus);
			return rsp;
		}

		page.setTotalRows(docTmp.getInteger("total", 0));
		rsp.setMsgPage(page);

		List<Document> rows = null;
		if (docTmp.get("data") != null && docTmp.get("data") instanceof List) {
			rows = docTmp.getList("data", Document.class);
		}

		ArrayList<HashMap<String, Object>> rowsReturn = new ArrayList<HashMap<String, Object>>();
		HashMap<String, Object> hItem = null;
		if (null != rows) {
			for (Document doc : rows) {
				objectId = (ObjectId) doc.get("_id");

				issuerId = doc.get("IssuerId").toString();
				docFind = new Document("_id",  new ObjectId(issuerId)).append("IsDelete", false);
				pipeline.clear();
				pipeline.add(new Document("$match", docFind));
				try (MongoClient mongoClient = cfg.mongoClient()){
					MongoCollection<Document> collection = mongoClient.getDatabase(cfg.dbName).getCollection("Issuer");
					docTmp = collection.aggregate(pipeline).allowDiskUse(true).iterator().next();
				} catch (Exception e) {
				}

				hItem = new HashMap<String, Object>();
				hItem.put("_id", objectId.toString());
				hItem.put("SoLuong", doc.get("SoLuong"));
				hItem.put("ConLai", doc.get("ConLai"));
				hItem.put("KyHieu", doc.get("KyHieu"));
				hItem.put("MauSo", doc.get("MauSo"));
				hItem.put("Nam", doc.get("Nam"));
				hItem.put("ChungTu", doc.get("ChungTu"));
				hItem.put("TaxCode", docTmp.get("TaxCode"));
				hItem.put("Name", docTmp.get("Name"));
				rowsReturn.add(hItem);
			}
		}

		responseStatus = new MspResponseStatus(0,  Constants.MAP_ERROR.get(0));
		rsp.setResponseStatus(responseStatus);

		HashMap<String, Object> mapDataR = new HashMap<String, Object>();
		mapDataR.put("rows", rowsReturn);
		rsp.setObjData(mapDataR);
		return rsp;
	}
	
	@Transactional
	@Override
	public MsgRsp convertByYear(JSONRoot jsonRoot) throws Exception {
		Msg msg = jsonRoot.getMsg();
		MsgHeader header = msg.getMsgHeader();
		MsgRsp rsp = new MsgRsp(header);
		MspResponseStatus responseStatus = null;

		Object objData = msg.getObjData();
		JsonNode jsonData = null;
		if (objData != null) {
			jsonData = Json.serializer().nodeFromObject(msg.getObjData());
		} else {
			throw new Exception("Lỗi dữ liệu đầu vào");
		}
		
		String fromYear = commons.getTextJsonNode(jsonData.at("/NamCanChuyenDoi")).replaceAll("\\s", "");
		String becomeYear = commons.getTextJsonNode(jsonData.at("/NamChuyenDoi")).trim().replaceAll("\\s", "");
		
		List<Document> docTmps = new ArrayList<Document>();
		List<Document> pipeline = new ArrayList<Document>();
		FindOneAndUpdateOptions options = null;
		Set<String> set = new HashSet<String>();
		
		Document docMatch = new Document("IsActive", true).append("IsDelete", false);

		Iterator<Document> iter = mongoTemplate.getCollection("DMMSTNCN").find(docMatch.append("Nam", becomeYear)).iterator();	
		while (iter.hasNext()) {
			set.add(iter.next().get("IssuerId",""));
		}
		
		pipeline.add(new Document("$match", docMatch.append("Nam", fromYear).append("KyHieu", "CT")));
		iter = mongoTemplate.getCollection("DMMSTNCN").aggregate(pipeline).iterator();
		while (iter.hasNext()) {
			docTmps.add(iter.next());
		}
		
		if (docTmps.size() == 0 ) {			
			responseStatus = new MspResponseStatus(999, "Không tìm thấy thông tin mẫu chứng từ tncn năm cần chuyển đổi. Vui lòng kiểm tra lại!!!");
			rsp.setResponseStatus(responseStatus);
			return rsp;
		}
		
		ObjectId oldObjectId= null;
		Document docUpdate = null;
		options = new FindOneAndUpdateOptions();
		options.upsert(false);
		options.maxTime(5000, TimeUnit.MILLISECONDS);
		options.returnDocument(ReturnDocument.AFTER);	
		
		for (Document doc: docTmps) {
			oldObjectId = (ObjectId) doc.get("_id", ObjectId.class);
			doc.remove("_id");
			doc.remove("InfoDeleted");
			doc.remove("InfoUpdated");
			int oldQuantity = doc.get("SoLuong",0);
			int oldCurrentInvoiceNumber = doc.get("SHDHT",0);
			int oldRemmainInvoiceNumber = doc.get("ConLai",0);
			
			if (oldRemmainInvoiceNumber == 0 || set.contains(doc.get("IssuerId",""))) continue; 
			
			doc
			.append("Nam", becomeYear)
			.append("SoLuong", oldRemmainInvoiceNumber)
			.append("DenSo", oldRemmainInvoiceNumber)
			.append("ConLai", oldRemmainInvoiceNumber)
			.append("SHDHT", 0)
			.append("InfoCreated",
					new Document("CreateDate", LocalDateTime.now())
					.append("CreateUserID", header.getUserId())
					.append("CreateUserName", header.getUserName())
					.append("CreateUserFullName", header.getUserFullName()));
			
			mongoTemplate.getCollection("DMMSTNCN").insertOne(doc);
			
			docMatch = new Document("_id", oldObjectId).append("IsDelete", false).append("IsActive", true);
			docUpdate = new Document("SHDHT", oldQuantity)
					.append("ConLai", 0)
					.append("IsActive", false)
					.append("InfoPhatHanhNam" + fromYear,
							new Document("UpdatedDate", LocalDateTime.now())
									.append("SLDaDung", oldCurrentInvoiceNumber)
									.append("SLConLai", oldRemmainInvoiceNumber)
									.append("UpdatedUserID", header.getUserId())
									.append("UpdatedUserName", header.getUserName())
									.append("UpdatedUserFullName", header.getUserFullName()));
			
			mongoTemplate.getCollection("DMMSTNCN").findOneAndUpdate(
					docMatch,
					new Document("$set",docUpdate),
					options);
		}
		responseStatus = new MspResponseStatus(0, "SUCCESS");
		rsp.setResponseStatus(responseStatus);
		return rsp;
	}
}
