package vn.sesgroup.hddt.user.impl;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
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
import vn.sesgroup.hddt.user.dao.QuantityDAO;
import vn.sesgroup.hddt.user.service.TCTNService;
import vn.sesgroup.hddt.utility.Constants;
import vn.sesgroup.hddt.utility.Json;

@Repository
@Transactional
public class QuantityImpl extends AbstractDAO implements QuantityDAO {
	private static final Logger log = LogManager.getLogger(TBHDSSotImpl.class);
	@Autowired
	ConfigConnectMongo cfg;
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
		String _id = commons.getTextJsonNode(jsonData.at("/_id")).replaceAll("\\s", "");
		String mausohdon = commons.getTextJsonNode(jsonData.at("/mausohdon")).replaceAll("\\s", "");
//		String khhdon = commons.getTextJsonNode(jsonData.at("/khhdon")).replaceAll("\\s", "");
		String trthai = commons.getTextJsonNode(jsonData.at("/trthai")).replaceAll("\\s", "");
		String quantity = commons.getTextJsonNode(jsonData.at("/quantity")).replaceAll("\\s", "");

		String tmp = "";

		MsgRsp rsp = new MsgRsp(header);
		rsp.setMsgPage(page);
		MspResponseStatus responseStatus = null;

		ObjectId objectId = null;
		ObjectId objectIdUser = null;
		ObjectId objectIdMSKH = null;
		ObjectId objectIdEInvoice = null;
		ObjectId objectIdTT_DC = null;

		List<Document> pipeline = null;
		Document docR = null;
		Document docFind = null;
		Document docFind1 = null;
		Document docTmp = null;
		Document docTmp1 = null;
		Document docUpsert = null;
		FindOneAndUpdateOptions options = null;
		List<Document> docs = null;

		Iterable<Document> cursor = null;
		Iterator<Document> iter = null;
		Iterable<Document> cursor1 = null;
		Iterator<Document> iter1 = null;
		switch (actionCode) {
		case Constants.MSG_ACTION_CODE.CREATED:
			objectId = null;
			objectIdUser = null;
			objectIdMSKH = null;
			objectIdTT_DC = null;
			try {
				objectId = new ObjectId(header.getIssuerId());
			} catch (Exception e) {
			}
			try {
				objectIdUser = new ObjectId(header.getUserId());
			} catch (Exception e) {
			}
			try {
				objectIdMSKH = new ObjectId(mausohdon);
			} catch (Exception e) {
			}
			pipeline = new ArrayList<Document>();
			pipeline.add(new Document("$match", new Document("_id", objectId).append("IsActive", true)
					.append("IsDelete", new Document("$ne", true))));
			new Document("$project", new Document("_id", 1).append("TaxCode", 1));
			
			pipeline.add(new Document("$lookup", new Document("from", "Users").append("pipeline", Arrays.asList(
					new Document("$match",
							new Document("IssuerId", header.getIssuerId()).append("_id", objectIdUser)
									.append("IsActive", true).append("IsDelete", new Document("$ne", true))),
					new Document("$project", new Document("_id", 1).append("UserName", 1).append("FullName", 1)),
					new Document("$limit", 1))).append("as", "UserInfo")));
			pipeline.add(new Document("$unwind",
					new Document("path", "$UserInfo").append("preserveNullAndEmptyArrays", true)));

			docFind = new Document("_id", objectIdMSKH);
			pipeline.add(new Document("$lookup", new Document("from", "DMMauSoKyHieu")
					.append("pipeline", Arrays.asList(new Document("$match", docFind))).append("as", "DMMauSoKyHieu")));
			new Document("$project", new Document("_id", 1).append("KHMSHDon", 1).append("KHHDon", 1)
					.append("Templates", 1).append("SoLuong", 1)
					.append("DenSo", 1).append("ConLai", 1));
			pipeline.add(new Document("$unwind",
					new Document("path", "$DMMauSoKyHieu").append("preserveNullAndEmptyArrays", true)));

			pipeline.add(
					new Document("$lookup",
							new Document("from", "DMQuantity")
									.append("pipeline",
											Arrays.asList(
													new Document("$match",
															new Document("IssuerId", header.getIssuerId())
																	.append("MSKHieu", mausohdon))))
									.append("as", "DMQuantity")));
			new Document("$project", new Document("_id", 1).append("STT", 1).append("LanPH", 1));
			pipeline.add(new Document("$unwind",
					new Document("path", "$DMQuantity").append("preserveNullAndEmptyArrays", true)));

			MongoClient mongoClient = cfg.mongoClient();
			MongoCollection<Document> collection = mongoClient.getDatabase(cfg.dbName).getCollection("Issuer");

			try {
				docTmp = collection.aggregate(pipeline).allowDiskUse(true).iterator().next();
			} catch (Exception e) {

			}
			mongoClient.close();

			String taxCode = "";
			taxCode = docTmp.getString("TaxCode");

			pipeline = new ArrayList<Document>();
			pipeline.add(new Document("$lookup", new Document("from", "DMDepot")
					.append("pipeline", Arrays.asList(new Document("$match", new Document("TaxCode", taxCode)),
							new Document("$project",
									new Document("_id", 1).append("SLHDon", 1).append("SLHDonDD", 1)
											.append("SLHDonCL", 1).append("TaxCode", 1)),
							new Document("$limit", 1)))
					.append("as", "DMDepotInfos")));
			pipeline.add(new Document("$unwind",
					new Document("path", "$DMDepotInfos").append("preserveNullAndEmptyArrays", true)));

			mongoClient = cfg.mongoClient();
			collection = mongoClient.getDatabase(cfg.dbName).getCollection("Issuer");

			try {
				docTmp1 = collection.aggregate(pipeline).allowDiskUse(true).iterator().next();
			} catch (Exception e) {

			}
			mongoClient.close();

			String KHMSHDon = docTmp.getEmbedded(Arrays.asList("DMMauSoKyHieu", "KHMSHDon"), "");
			String KHHDon = docTmp.getEmbedded(Arrays.asList("DMMauSoKyHieu", "KHHDon"), "");
			String NamePhoi = docTmp.getEmbedded(Arrays.asList("DMMauSoKyHieu", "Templates", "Name"), "");
			int STT = docTmp.getEmbedded(Arrays.asList("DMQuantity", "STT"), 0);
			int LanPH = docTmp.getEmbedded(Arrays.asList("DMQuantity", "LanPH"), 0);

			int SL = docTmp.getEmbedded(Arrays.asList("DMMauSoKyHieu", "SoLuong"), 0);
			int DS = docTmp.getEmbedded(Arrays.asList("DMMauSoKyHieu", "DenSo"), 0);
			int CL = docTmp.getEmbedded(Arrays.asList("DMMauSoKyHieu", "ConLai"), 0);

			int SLKho = docTmp1.getEmbedded(Arrays.asList("DMDepotInfos", "SLHDon"), 0);
			int SLKhoCL = docTmp1.getEmbedded(Arrays.asList("DMDepotInfos", "SLHDonCL"), 0);
			int SLKhoDD = docTmp1.getEmbedded(Arrays.asList("DMDepotInfos", "SLHDonDD"), 0);
			int SL_nhap = Integer.parseInt(quantity);
			if (SL_nhap > SLKhoCL) {
				responseStatus = new MspResponseStatus(9999, "Số lượng không đủ để phát hành.");
				rsp.setResponseStatus(responseStatus);
				return rsp;
			}
			int SLDMMS = SL + SL_nhap;
			int DSDMMS = DS + SL_nhap;
			int CLDMMS = CL + SL_nhap;
			int SLKhoUpdateDD = SLKhoDD + SL_nhap;
			int SLKhoUpdateCL = SLKhoCL - SL_nhap;

			options = new FindOneAndUpdateOptions();
			options.upsert(false);
			options.maxTime(5000, TimeUnit.MILLISECONDS);
			options.returnDocument(ReturnDocument.AFTER);

			mongoClient = cfg.mongoClient();
			collection = mongoClient.getDatabase(cfg.dbName).getCollection("DMMauSoKyHieu");
			docR = collection.findOneAndUpdate(docFind, new Document("$set", new Document("SoLuong", SLDMMS)
					.append("TuSo", 1).append("DenSo", DSDMMS).append("ConLai", CLDMMS)

					.append("IsActive", true).append("InfoUpdated",
							new Document("UpdatedDate", LocalDateTime.now()).append("UpdatedUserID", header.getUserId())
									.append("UpdatedUserName", header.getUserName())
									.append("UpdatedUserFullName", header.getUserFullName()))),
					options);
			mongoClient.close();

			// update kho
			docFind1 = new Document("TaxCode", taxCode);
			mongoClient = cfg.mongoClient();
			collection = mongoClient.getDatabase(cfg.dbName).getCollection("DMDepot");
			docR = collection.findOneAndUpdate(docFind1, new Document("$set",
					new Document("SLHDon", SLKho).append("SLHDonDD", SLKhoUpdateDD).append("SLHDonCL", SLKhoUpdateCL)),
					options);

			mongoClient.close();

			int stt1 = STT + 1;
			int lanph = LanPH + 1;
			int TuSoQuantity = SL + 1;
			docUpsert = new Document("IssuerId", header.getIssuerId()).append("STT", stt1).append("LanPH", lanph)
					.append("MSKHieu", mausohdon).append("KHMSHDon", KHMSHDon).append("KHMSHDon", KHMSHDon)
					.append("KHHDon", KHHDon).append("NamePhoi", NamePhoi).append("SoLuong", SLDMMS)
					.append("TuSo", TuSoQuantity).append("DenSo", DSDMMS).append("GChu", "")
					.append("NLap", LocalDate.now());
			/* END - LUU DU LIEU HD */

			mongoClient = cfg.mongoClient();
			collection = mongoClient.getDatabase(cfg.dbName).getCollection("DMQuantity");
			collection.insertOne(docUpsert);
			mongoClient.close();
			responseStatus = new MspResponseStatus(0, "SUCCESS");
			rsp.setResponseStatus(responseStatus);
			return rsp;
		case Constants.MSG_ACTION_CODE.MODIFY:
			objectId = null;
			objectIdUser = null;
			objectIdEInvoice = null;
			try {
				objectId = new ObjectId(header.getIssuerId());
			} catch (Exception e) {
			}
			try {
				objectIdUser = new ObjectId(header.getUserId());
			} catch (Exception e) {
			}
			try {
				objectIdEInvoice = new ObjectId(_id);
			} catch (Exception e) {
			}

			pipeline = new ArrayList<Document>();
			pipeline.add(new Document("$match", new Document("_id", objectId).append("IsActive", true)
					.append("IsDelete", new Document("$ne", true))));
			new Document("$project", new Document("_id", 1).append("TaxCode", 1));
			
			pipeline.add(new Document("$lookup", new Document("from", "Users").append("pipeline", Arrays.asList(
					new Document("$match",
							new Document("IssuerId", header.getIssuerId()).append("_id", objectIdUser)
									.append("IsActive", true).append("IsDelete", new Document("$ne", true))),
					new Document("$project", new Document("_id", 1).append("UserName", 1).append("FullName", 1)),
					new Document("$limit", 1))).append("as", "UserInfo")));
			pipeline.add(new Document("$unwind",
					new Document("path", "$UserInfo").append("preserveNullAndEmptyArrays", true)));

			
			docFind = new Document("_id", objectIdEInvoice);
			pipeline.add(new Document("$lookup", new Document("from", "DMMauSoKyHieu")
					.append("pipeline", Arrays.asList(new Document("$match", docFind))).append("as", "DMMauSoKyHieu")));
			new Document("$project", new Document("_id", 1));
			pipeline.add(new Document("$unwind",
					new Document("path", "$DMMauSoKyHieu").append("preserveNullAndEmptyArrays", true)));

			
			mongoClient = cfg.mongoClient();
			collection = mongoClient.getDatabase(cfg.dbName).getCollection("Issuer");

			try {
				docTmp = collection.aggregate(pipeline).allowDiskUse(true).iterator().next();
			} catch (Exception e) {

			}
			mongoClient.close();

			taxCode = docTmp.getString("TaxCode");

			pipeline = new ArrayList<Document>();
			pipeline.add(new Document("$lookup", new Document("from", "DMDepot")
					.append("pipeline", Arrays.asList(new Document("$match", new Document("TaxCode", taxCode)),
							new Document("$project",
									new Document("_id", 1).append("SLHDon", 1).append("SLHDonDD", 1)
											.append("SLHDonCL", 1).append("TaxCode", 1)),
							new Document("$limit", 1)))
					.append("as", "DMDepotInfos")));
			pipeline.add(new Document("$unwind",
					new Document("path", "$DMDepotInfos").append("preserveNullAndEmptyArrays", true)));

			mongoClient = cfg.mongoClient();
			collection = mongoClient.getDatabase(cfg.dbName).getCollection("Issuer");
			try {
				docTmp1 = collection.aggregate(pipeline).allowDiskUse(true).iterator().next();
			} catch (Exception e) {
				// TODO: handle exception
			}
			mongoClient.close();

			int SL_nhap1 = Integer.parseInt(quantity);
			if (docTmp.get("UserInfo") == null) {
				responseStatus = new MspResponseStatus(9999, "Không tìm thấy thông tin người dùng.");
				rsp.setResponseStatus(responseStatus);
				return rsp;
			}

			options = new FindOneAndUpdateOptions();
			options.upsert(false);
			options.maxTime(5000, TimeUnit.MILLISECONDS);
			options.returnDocument(ReturnDocument.AFTER);

			mongoClient = cfg.mongoClient();
			collection = mongoClient.getDatabase(cfg.dbName).getCollection("DMMauSoKyHieu");
			docR = collection.findOneAndUpdate(docFind, new Document("$set", new Document("SoLuong", quantity)
					.append("TuSo", 1).append("DenSo", quantity).append("ConLai", quantity).append("InfoUpdated",
							new Document("UpdatedDate", LocalDateTime.now()).append("UpdatedUserID", header.getUserId())
									.append("UpdatedUserName", header.getUserName())
									.append("UpdatedUserFullName", header.getUserFullName()))),
					options);
			mongoClient.close();

			responseStatus = new MspResponseStatus(0, "SUCCESS");
			rsp.setResponseStatus(responseStatus);
			return rsp;
///////////////////////////////////////////////////////////////////////////////////////
		case Constants.MSG_ACTION_CODE.DELETE:
			objectId = null;
			try {
				objectId = new ObjectId(_id);
			} catch (Exception ex) {
			}
			/* KIEM TRA THONG TIN HOPDONG CO TON TAI KHONG */
			docFind = new Document("_id", objectId);
			docTmp = null;

			mongoClient = cfg.mongoClient();
			collection = mongoClient.getDatabase(cfg.dbName).getCollection("DMMauSoKyHieu");
			try {
				docTmp = collection.find(docFind).allowDiskUse(true).iterator().next();
			} catch (Exception e) {
				// TODO: handle exception
			}
			mongoClient.close();

			if (null == docTmp) {
				responseStatus = new MspResponseStatus(9999, "Không tìm thấy thông tin hóa đơn.");
				rsp.setResponseStatus(responseStatus);
				return rsp;
			}

			options = new FindOneAndUpdateOptions();
			options.upsert(false);
			options.maxTime(5000, TimeUnit.MILLISECONDS);
			options.returnDocument(ReturnDocument.AFTER);

			mongoClient = cfg.mongoClient();
			collection = mongoClient.getDatabase(cfg.dbName).getCollection("DMMauSoKyHieu");
			docR = collection.findOneAndUpdate(docFind,
					new Document("$set", new Document("IsDelete", true).append("InfoDeleted",
							new Document("DeletedDate", LocalDateTime.now()).append("DeletedUserID", header.getUserId())
									.append("DeletedUserName", header.getUserName())
									.append("DeletedUserFullName", header.getUserFullName()))),
					options);
			mongoClient.close();

			responseStatus = new MspResponseStatus(0, "SUCCESS");
			rsp.setResponseStatus(responseStatus);
			return rsp;
////////////////////////////////////////////////////////////////////////////////////////
		case Constants.MSG_ACTION_CODE.ACTIVE:
			objectId = null;
			try {
				objectId = new ObjectId(_id);
			} catch (Exception ex) {
			}
			/* KIEM TRA THONG TIN HOPDONG CO TON TAI KHONG */
			docFind = new Document("_id", objectId);
			docTmp = null;

			mongoClient = cfg.mongoClient();
			collection = mongoClient.getDatabase(cfg.dbName).getCollection("DMMauSoKyHieu");

			try {
				docTmp = collection.find(docFind).allowDiskUse(true).iterator().next();
			} catch (Exception e) {

			}
			mongoClient.close();

			if (null == docTmp) {
				responseStatus = new MspResponseStatus(9999, "Không tìm thấy thông tin hóa đơn.");
				rsp.setResponseStatus(responseStatus);
				return rsp;
			}

			options = new FindOneAndUpdateOptions();
			options.upsert(false);
			options.maxTime(5000, TimeUnit.MILLISECONDS);
			options.returnDocument(ReturnDocument.AFTER);

			mongoClient = cfg.mongoClient();
			collection = mongoClient.getDatabase(cfg.dbName).getCollection("DMMauSoKyHieu");
			docR = collection.findOneAndUpdate(docFind,
					new Document("$set", new Document("IsActive", true).append("InfoDeleted",
							new Document("DeletedDate", LocalDateTime.now()).append("DeletedUserID", header.getUserId())
									.append("DeletedUserName", header.getUserName())
									.append("DeletedUserFullName", header.getUserFullName()))),
					options);
			mongoClient.close();

			responseStatus = new MspResponseStatus(0, "SUCCESS");
			rsp.setResponseStatus(responseStatus);
			return rsp;
//////////////////////////////////////////////////////////////////////////////////////////
		case Constants.MSG_ACTION_CODE.DEACTIVE:
			objectId = null;
			try {
				objectId = new ObjectId(_id);
			} catch (Exception ex) {
			}
			/* KIEM TRA THONG TIN HOPDONG CO TON TAI KHONG */
			docFind = new Document("_id", objectId);
			docTmp = null;

			mongoClient = cfg.mongoClient();
			collection = mongoClient.getDatabase(cfg.dbName).getCollection("DMMauSoKyHieu");

			try {
				docTmp = collection.find(docFind).allowDiskUse(true).iterator().next();
			} catch (Exception e) {

			}
			mongoClient.close();
			if (null == docTmp) {
				responseStatus = new MspResponseStatus(9999, "Không tìm thấy thông tin hóa đơn.");
				rsp.setResponseStatus(responseStatus);
				return rsp;
			}

			options = new FindOneAndUpdateOptions();
			options.upsert(false);
			options.maxTime(5000, TimeUnit.MILLISECONDS);
			options.returnDocument(ReturnDocument.AFTER);

			mongoClient = cfg.mongoClient();
			collection = mongoClient.getDatabase(cfg.dbName).getCollection("DMMauSoKyHieu");
			docR = collection.findOneAndUpdate(docFind,
					new Document("$set", new Document("IsActive", false).append("InfoDeleted",
							new Document("DeletedDate", LocalDateTime.now()).append("DeletedUserID", header.getUserId())
									.append("DeletedUserName", header.getUserName())
									.append("DeletedUserFullName", header.getUserFullName()))),
					options);
			mongoClient.close();

			responseStatus = new MspResponseStatus(0, "SUCCESS");
			rsp.setResponseStatus(responseStatus);
			return rsp;

		}
		return rsp;

	}

	public MsgRsp list(JSONRoot jsonRoot) throws Exception {
		Msg msg = jsonRoot.getMsg();
		MsgHeader header = msg.getMsgHeader();
		MsgPage page = msg.getMsgPage();
		Object objData = msg.getObjData();

		String mauSoHdon = "";
		String fromDate = "";
		String toDate = "";

		JsonNode jsonData = null;

		if (objData != null) {
			jsonData = Json.serializer().nodeFromObject(objData);
			mauSoHdon = commons.getTextJsonNode(jsonData.at("/MauSoHdon")).replaceAll("\\s", "");
			fromDate = commons.getTextJsonNode(jsonData.at("/FromDate")).replaceAll("\\s", "");
			toDate = commons.getTextJsonNode(jsonData.at("/ToDate")).replaceAll("\\s", "");
		}

		MsgRsp rsp = new MsgRsp(header);
		MspResponseStatus responseStatus = null;

		ObjectId objectId = null;
		Document docTmp = null;
		Iterable<Document> cursor = null;
		Iterator<Document> iter = null;
		List<Document> pipeline = new ArrayList<Document>();

		LocalDate dateFrom = null;
		LocalDate dateTo = null;
		Document docMatchDate = null;

		dateFrom = "".equals(fromDate) || !commons.checkLocalDate(fromDate, Constants.FORMAT_DATE.FORMAT_DATE_WEB)
				? null
				: commons.convertStringToLocalDate(fromDate, Constants.FORMAT_DATE.FORMAT_DATE_WEB);
		dateTo = "".equals(toDate) || !commons.checkLocalDate(toDate, Constants.FORMAT_DATE.FORMAT_DATE_WEB) ? null
				: commons.convertStringToLocalDate(toDate, Constants.FORMAT_DATE.FORMAT_DATE_WEB);
		if (null != dateTo)
			dateTo = dateTo.plus(1, ChronoUnit.DAYS);
		if (null != dateFrom || null != dateTo) {
			docMatchDate = new Document();
			if (null != dateFrom)
				docMatchDate.append("$gte", dateFrom);
			if (null != dateTo)
				docMatchDate.append("$lt", dateTo);
		}
		Document docMatch = new Document("IssuerId", header.getIssuerId());
		if (!"".equals(mauSoHdon))
			docMatch.append("MSKHieu", commons.regexEscapeForMongoQuery(mauSoHdon));
		Document fillter = new Document("_id", 1).append("KHMSHDon", 1).append("NamePhoi", 1).append("KHHDon", 1)
				.append("SoLuong", 1).append("TuSo", 1).append("DenSo", 1).append("NLap", 1);

		pipeline = new ArrayList<Document>();
		pipeline.add(new Document("$match", docMatch));
		pipeline.add(new Document("$sort", new Document("NLap", -1)));
		pipeline.add(new Document("$project", fillter));
		pipeline.addAll(createFacetForSearchNotSort(page));

		MongoClient mongoClient = cfg.mongoClient();
		MongoCollection<Document> collection = mongoClient.getDatabase(cfg.dbName).getCollection("DMQuantity");

		try {
			docTmp = collection.aggregate(pipeline).allowDiskUse(true).iterator().next();
		} catch (Exception e) {

		}
		mongoClient.close();

		rsp = new MsgRsp(header);
		responseStatus = null;
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
				hItem = new HashMap<String, Object>();
				hItem = new HashMap<String, Object>();
				hItem.put("_id", objectId.toString());
				hItem.put("KHMSHDon", doc.get("KHMSHDon"));
				hItem.put("NamePhoi", doc.get("NamePhoi"));
				hItem.put("KHHDon", doc.get("KHHDon"));
				hItem.put("SoLuong", doc.get("SoLuong"));
				hItem.put("TuSo", doc.get("TuSo"));
				hItem.put("DenSo", doc.get("DenSo"));
				hItem.put("NLap", doc.get("NLap"));
				rowsReturn.add(hItem);
			}
		}
		responseStatus = new MspResponseStatus(0, "SUCCESS");
		rsp.setResponseStatus(responseStatus);

		HashMap<String, Object> mapDataR = new HashMap<String, Object>();
		mapDataR.put("rows", rowsReturn);
		rsp.setObjData(mapDataR);
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

		Document docFind = new Document("_id", objectId);
		Document docTmp = null;

		MongoClient mongoClient = cfg.mongoClient();
		MongoCollection<Document> collection = mongoClient.getDatabase(cfg.dbName).getCollection("DMQuantity");

		try {
			docTmp = collection.find(docFind).allowDiskUse(true).iterator().next();
		} catch (Exception e) {

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
