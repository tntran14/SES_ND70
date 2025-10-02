package vn.sesgroup.hddt.user.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
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
import vn.sesgroup.hddt.user.dao.TTHDonDAO;
import vn.sesgroup.hddt.user.service.JPUtils;
import vn.sesgroup.hddt.user.service.TCTNService;
import vn.sesgroup.hddt.utility.Constants;
import vn.sesgroup.hddt.utility.Json;

@Repository
@Transactional
public class TTHDonImpl extends AbstractDAO implements TTHDonDAO {
	private static final Logger log = LogManager.getLogger(TTHDonImpl.class);
	@Autowired ConfigConnectMongo cfg;
	@Autowired TCTNService tctnService;

	@Autowired
	JPUtils jpUtils;
	Document docUpsert = null;
	@Override
	public MsgRsp check(JSONRoot jsonRoot) throws Exception {
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

		MsgRsp rsp = new MsgRsp(header);
		rsp.setMsgPage(page);
		MspResponseStatus responseStatus = null;

		String mtdiep = commons.getTextJsonNode(jsonData.at("/MTDiep")).replaceAll("\\s", "");
		ObjectId objectId = null;

		List<Document> pipeline = new ArrayList<Document>();

		Document docMatch = new Document("MTDiep", mtdiep).append("IsDelete", new Document("$ne", true));
		Document docFilter = new Document("_id", 1)
							.append("EInvoiceDetail.TTChung", 1)
							.append("EInvoiceDetail.NDHDon.NMua", 1)
							.append("SignStatusCode", 1)
							.append("EInvoiceStatus", 1)
							.append("MTDTChieu", 1)
							.append("MTDiep", 1);
		pipeline = new ArrayList<Document>();
		pipeline.add(new Document("$match", docMatch));
		pipeline.add(new Document("$project", docFilter));
		List<Document> docTmps = new ArrayList<Document>();
		Iterator<Document> iter = null;
		String collectionName = "";

		try (MongoClient mongoClient = cfg.mongoClient()) {
			collectionName = "EInvoice";
			MongoCollection<Document> collection = mongoClient.getDatabase(cfg.dbName).getCollection(collectionName);
			iter = collection.aggregate(pipeline).allowDiskUse(true).iterator();
			while (iter.hasNext()) {
				docTmps.add(iter.next());
			}

			iter = null;
			collectionName = "EInvoiceBH";
			collection = mongoClient.getDatabase(cfg.dbName).getCollection(collectionName);
			iter = collection.aggregate(pipeline).allowDiskUse(true).iterator();
			while (iter.hasNext()) {
				docTmps.add(iter.next());
			}

			iter = null;
			collectionName = "EInvoicePXK";
			collection = mongoClient.getDatabase(cfg.dbName).getCollection(collectionName);
			iter = collection.aggregate(pipeline).allowDiskUse(true).iterator();
			while (iter.hasNext()) {
				docTmps.add(iter.next());
			}

			iter = null;
			collectionName = "EInvoicePXKDL";
			collection = mongoClient.getDatabase(cfg.dbName).getCollection(collectionName);
			iter = collection.aggregate(pipeline).allowDiskUse(true).iterator();
			while (iter.hasNext()) {
				docTmps.add(iter.next());
			}

			iter = null;
			collectionName = "EInvoiceMTT";
			collection = mongoClient.getDatabase(cfg.dbName).getCollection(collectionName);
			iter = collection.aggregate(pipeline).allowDiskUse(true).iterator();
			while (iter.hasNext()) {
				docTmps.add(iter.next());
			}

		} catch (Exception e) {
		}
		
		if (docTmps.size() < 1) {
			responseStatus = new MspResponseStatus(999, "Không tìm thấy hóa đơn.");
			rsp.setResponseStatus(responseStatus);
			return rsp;
		}
		
		ArrayList<HashMap<String, Object>> rowsReturn = new ArrayList<HashMap<String, Object>>();
		HashMap<String, Object> hItem = null;

		for (Document doc : docTmps) {
			objectId = (ObjectId) doc.get("_id");

			hItem = new HashMap<String, Object>();
			hItem.put("_id", objectId.toString());
			hItem.put("EInvoiceStatus", doc.get("EInvoiceStatus"));
			hItem.put("SignStatusCode", doc.get("SignStatusCode"));
			hItem.put("MTDiep", doc.get("MTDiep"));
			hItem.put("EInvoiceDetail", doc.get("EInvoiceDetail"));
			hItem.put("MTDiep", doc.get("MTDiep"));
			hItem.put("MTDTChieu", doc.get("MTDTChieu"));

			rowsReturn.add(hItem);
		}
		
		int totalRows = docTmps.size();
		page.setTotalRows(totalRows);
		rsp.setMsgPage(page);
		responseStatus = new MspResponseStatus(0, "SUCCESS");
		rsp.setResponseStatus(responseStatus);
		
		int pageNo = page.getPageNo();     
		int pageSize = page.getSize();

		// Calculate start and end indexes
		int fromIndex = (pageNo - 1) * pageSize;
		int toIndex = Math.min(fromIndex + pageSize, totalRows);
		List<HashMap<String, Object>> results = new ArrayList<>();
		if (fromIndex < totalRows) {
			results = rowsReturn.subList(fromIndex, toIndex);
		}

		HashMap<String, Object> mapDataR = new HashMap<String, Object>();
		mapDataR.put("rows", results);
		rsp.setObjData(mapDataR);
		return rsp;
	}
	
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

		MsgRsp rsp = new MsgRsp(header);
		rsp.setMsgPage(page);
		MspResponseStatus responseStatus = null;

		String mtdiep = commons.getTextJsonNode(jsonData.at("/MTDiep")).replaceAll("\\s", "");
		String tthd = commons.getTextJsonNode(jsonData.at("/TTHDon")).replaceAll("\\s", "");

		// SEARCH
		List<Document> pipeline = new ArrayList<Document>();
		Document docMatch = new Document("MTDiep", mtdiep).append("IsDelete", new Document("$ne", true));

		pipeline = new ArrayList<Document>();
		pipeline.add(new Document("$match", docMatch));
		pipeline.addAll(createFacetForSearchNotSort(page));

		Document docTmp = null;
		String collectionName = "";
		Iterator<Document> iter = null;
		
		Document docFind = new Document("MTDiep", mtdiep).append("IsDelete", new Document("$ne", true));

		try (MongoClient mongoClient = cfg.mongoClient()) {
			collectionName = "EInvoice";
			MongoCollection<Document> collection = mongoClient.getDatabase(cfg.dbName).getCollection(collectionName);
			iter = collection.find(docFind).allowDiskUse(true).iterator();
			if (iter.hasNext()) {
				docTmp = iter.next();
			}

			if (docTmp == null) {
				collectionName = "EInvoiceBH";
				collection = mongoClient.getDatabase(cfg.dbName).getCollection(collectionName);
				iter = collection.find(docFind).allowDiskUse(true).iterator();
				if (iter.hasNext()) {
					docTmp = iter.next();
				}
			}

			if (docTmp == null) {
				collectionName = "EInvoicePXK";
				collection = mongoClient.getDatabase(cfg.dbName).getCollection(collectionName);
				iter = collection.find(docFind).allowDiskUse(true).iterator();
				if (iter.hasNext()) {
					docTmp = iter.next();
				}
			}
			if (docTmp == null) {
				collectionName = "EInvoicePXKDL";
				collection = mongoClient.getDatabase(cfg.dbName).getCollection(collectionName);
				iter = collection.find(docFind).allowDiskUse(true).iterator();
				if (iter.hasNext()) {
					docTmp = iter.next();
				}
			}
			if (docTmp == null) {
				collectionName = "EInvoiceMTT";
				collection = mongoClient.getDatabase(cfg.dbName).getCollection(collectionName);
				iter = collection.find(docFind).allowDiskUse(true).iterator();
				if (iter.hasNext()) {
					docTmp = iter.next();
				}
			}
		} catch (Exception e) {

		}

		if (docTmp == null) {
			responseStatus = new MspResponseStatus(999, "Không tìm thấy hóa đơn");
			rsp.setResponseStatus(responseStatus);
			return rsp;
		}

		FindOneAndUpdateOptions options = new FindOneAndUpdateOptions();
		options.upsert(false);
		options.maxTime(5000, TimeUnit.MILLISECONDS);
		options.returnDocument(ReturnDocument.AFTER);

		try (MongoClient mongoClient = cfg.mongoClient()) {
			MongoCollection<Document> collection = mongoClient.getDatabase(cfg.dbName).getCollection(collectionName);
			collection.findOneAndUpdate(
					docFind, 
					new Document("$set", new Document("EInvoiceStatus", tthd)), 
					options);
		} catch (Exception e) {
			responseStatus = new MspResponseStatus(999, "Lỗi ngoại lệ");
			rsp.setResponseStatus(responseStatus);
			return rsp;
		}

		responseStatus = new MspResponseStatus(0, "SUCCESS");
		rsp.setResponseStatus(responseStatus);
		return rsp;
	}
}
