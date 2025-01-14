package vn.sesgroup.hddt.user.impl;

import java.io.File;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
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
import vn.sesgroup.hddt.user.dao.CKSDAO;
import vn.sesgroup.hddt.user.service.TCTNService;
import vn.sesgroup.hddt.utility.Constants;
import vn.sesgroup.hddt.utility.Json;

@Repository
@Transactional
public class CKSImpl extends AbstractDAO implements CKSDAO {
	private static final Logger log = LogManager.getLogger(CKSImpl.class);
	@Autowired
	ConfigConnectMongo cfg;
	@Autowired
	TCTNService tctnService;

	@Transactional(rollbackFor = { Exception.class })
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
		String tenNnt = commons.getTextJsonNode(jsonData.at("/TenNnt")).trim().replaceAll("\\s+", " ");
		String mauSo = commons.getTextJsonNode(jsonData.at("/MauSo")).replaceAll("\\s", "");
		String ten = commons.getTextJsonNode(jsonData.at("/Ten")).trim().replaceAll("\\s+", " ");
		String mst = commons.getTextJsonNode(jsonData.at("/Mst")).replaceAll("\\s", "");
		String tinhThanh = commons.getTextJsonNode(jsonData.at("/TinhThanh")).replaceAll("\\s", "");
		String cqtQLy = commons.getTextJsonNode(jsonData.at("/CqtQLy")).replaceAll("\\s", "");
		String nLap = commons.getTextJsonNode(jsonData.at("/NLap")).trim().replaceAll("\\s+", " ");
		List<Object> rowDSCTSSDung = new ArrayList<Object>();

		MsgRsp rsp = new MsgRsp(header);
		rsp.setMsgPage(page);
		MspResponseStatus responseStatus = null;

		List<Document> pipeline = null;
		Document docFind = null;
		Document docTmp = null;

		Document docUpsert = null;

		String taxCode = "";

		Path path = null;
		File file = null;

		ObjectId objectId = null;
		ObjectId objectIdUser = null;
		ObjectId objectIdTK = null;
		ObjectId objectIdDMCTS = null;
		HashMap<String, Object> hO = null;
		String cks = "";
		if (!jsonData.at("/DSCTSSDung").isMissingNode()) {
			for (JsonNode o : jsonData.at("/DSCTSSDung")) {
				hO = new LinkedHashMap<String, Object>();
				hO.put("TTChuc", commons.getTextJsonNode(o.at("/TTChuc")));
				hO.put("Seri", commons.getTextJsonNode(o.at("/Seri")));
				cks = commons.getTextJsonNode(o.at("/Seri")).trim().replaceAll("\\s+", "");
				hO.put("TNgay", commons.convertStringToLocalDateTime(commons.getTextJsonNode(o.at("/TNgay")),
						Constants.FORMAT_DATE.FORMAT_DATE_TIME_WEB));
				hO.put("DNgay", commons.convertStringToLocalDateTime(commons.getTextJsonNode(o.at("/DNgay")),
						Constants.FORMAT_DATE.FORMAT_DATE_TIME_WEB));
				rowDSCTSSDung.add(hO);

			}
		}
		FindOneAndUpdateOptions options = null;
		Document docR = null;

		switch (actionCode) {
		case Constants.MSG_ACTION_CODE.CREATED:

			objectId = null;
			objectIdUser = null;
			try {
				objectId = new ObjectId(header.getIssuerId());
			} catch (Exception e) {
			}
			try {
				objectIdUser = new ObjectId(header.getUserId());
			} catch (Exception e) {
			}

			/*
			 * KIEM TRA THONG TIN KHACH HANG - USER - TINH THANH - CO QUAN THUE CO TON TAI
			 * KHONG
			 */
			docFind = new Document("_id", objectId).append("IsActive", true).append("IsDelete",
					new Document("$ne", true));
			pipeline = new ArrayList<Document>();
			pipeline.add(new Document("$match", docFind));
			pipeline.add(new Document("$lookup", new Document("from", "Users").append("pipeline", Arrays.asList(
					new Document("$match",
							new Document("IssuerId", header.getIssuerId()).append("_id", objectIdUser)
									.append("IsActive", true).append("IsDelete", new Document("$ne", true))),
					new Document("$project", new Document("_id", 1).append("UserName", 1).append("FullName", 1)),
					new Document("$limit", 1))).append("as", "UserInfo")));
			pipeline.add(new Document("$unwind",
					new Document("path", "$UserInfo").append("preserveNullAndEmptyArrays", true)));
			pipeline.add(new Document("$lookup",
					new Document("from", "DMCTSo").append("pipeline",
							Arrays.asList(
									new Document("$match",
											new Document("IssuerId", header.getIssuerId()).append("IsDelete",
													new Document("$ne", true))),
									new Document("$sort", new Document("NLap", -1).append("_id", -1)),
									new Document("$limit", 1)))
							.append("as", "DMCTSo")));
			pipeline.add(new Document("$unwind",
					new Document("path", "$DMCTSo").append("preserveNullAndEmptyArrays", true)));
			pipeline.add(new Document("$lookup", new Document("from", "DMTinhThanh")
					.append("pipeline", Arrays.asList(
							new Document("$match",
									new Document("IsDelete", new Document("$ne", true)).append("code",
											commons.regexEscapeForMongoQuery(tinhThanh))),
							new Document("$project", new Document("_id", 0).append("code", 1).append("name", 1))))
					.append("as", "DMTinhThanhInfo")));
			pipeline.add(new Document("$unwind",
					new Document("path", "$DMTinhThanhInfo").append("preserveNullAndEmptyArrays", true)));
			pipeline.add(new Document("$lookup", new Document("from", "DMChiCucThue")
					.append("pipeline", Arrays.asList(
							new Document("$match",
									new Document("IsDelete", new Document("$ne", true)).append("code",
											commons.regexEscapeForMongoQuery(cqtQLy))),
							new Document("$project", new Document("_id", 0).append("code", 1).append("name", 1))))
					.append("as", "DMChiCucThueInfo")));
			pipeline.add(new Document("$unwind",
					new Document("path", "$DMChiCucThueInfo").append("preserveNullAndEmptyArrays", true)));

			MongoClient mongoClient = cfg.mongoClient();
			MongoCollection<Document> collection = mongoClient.getDatabase(cfg.dbName).getCollection("Issuer");
			try {
				docTmp = collection.aggregate(pipeline).allowDiskUse(true).iterator().next();
			} catch (Exception e) {
				// TODO: handle exception
			}

			mongoClient.close();

			if (null == docTmp) {
				responseStatus = new MspResponseStatus(9999, "Không tìm thấy thông tin khách hàng.");
				rsp.setResponseStatus(responseStatus);
				return rsp;
			}
			if (docTmp.get("UserInfo") == null) {
				responseStatus = new MspResponseStatus(9999, "Không tìm thấy thông tin người dùng.");
				rsp.setResponseStatus(responseStatus);
				return rsp;
			}

			if (docTmp.get("DMTinhThanhInfo") == null || docTmp.get("DMChiCucThueInfo") == null) {
				responseStatus = new MspResponseStatus(9999, "Vui lòng kiểm tra lại tỉnh/thành phố và cơ quan thuế.");
				rsp.setResponseStatus(responseStatus);
				return rsp;
			}

			taxCode = docTmp.getString("TaxCode");

			rsp.setObjData(docTmp.getEmbedded(Arrays.asList("DMCTSo"), ""));

			JsonNode jsonData123 = Json.serializer().nodeFromObject(rsp.getObjData());

			HashMap<String, String> hItem = null;
			boolean ccheck = false;
			if (!jsonData.at("/DSCTSSDung").isMissingNode()) {
				for (JsonNode o : jsonData123.at("/DSCTSSDung")) {
					hItem = new LinkedHashMap<String, String>();
					String checkserri = commons.getTextJsonNode(o.at("/Seri"));
					if (checkserri.equals(cks)) {
						ccheck = true;
						break;
					}

				}
			}

			String TenNNT = docTmp.get("Name", "");

			/* LUU DU LIEU */
			docUpsert = new Document("IssuerId", header.getIssuerId())

					.append("TenNnt", TenNNT).append("MST", mst).append("TinhThanhInfo", docTmp.get("DMTinhThanhInfo"))
					.append("ChiCucThueInfo", docTmp.get("DMChiCucThueInfo"))

					.append("NLap", commons.convertStringToLocalDate(nLap, Constants.FORMAT_DATE.FORMAT_DATE_WEB))

					.append("DSCTSSDung", rowDSCTSSDung).append("Status", Constants.INVOICE_STATUS.TK_CREATED)
					.append("IsDelete", false).append("IsActive", true).append("InfoCreated",
							new Document("CreateDate", LocalDateTime.now()).append("CreateUserID", header.getUserId())
									.append("CreateUserName", header.getUserName())
									.append("CreateUserFullName", header.getUserFullName()));
			/* END - LUU DU LIEU */

			mongoClient = cfg.mongoClient();
			collection = mongoClient.getDatabase(cfg.dbName).getCollection("DMCTSo");
			collection.insertOne(docUpsert);
			mongoClient.close();

			responseStatus = new MspResponseStatus(0, "SUCCESS");
			rsp.setResponseStatus(responseStatus);
			return rsp;
		case Constants.MSG_ACTION_CODE.MODIFY:
			objectId = null;
			objectIdUser = null;
			try {
				objectId = new ObjectId(header.getIssuerId());
			} catch (Exception e) {
			}
			try {
				objectIdUser = new ObjectId(header.getUserId());
			} catch (Exception e) {
			}
			try {
				objectIdTK = new ObjectId(_id);
			} catch (Exception e) {
			}

			/*
			 * KIEM TRA THONG TIN KHACH HANG - USER - TINH THANH - CO QUAN THUE CO TON TAI
			 * KHONG
			 */
			docFind = new Document("_id", objectId).append("IsActive", true).append("IsDelete",
					new Document("$ne", true));
			pipeline = new ArrayList<Document>();
			pipeline.add(new Document("$match", docFind));
			pipeline.add(new Document("$lookup", new Document("from", "Users").append("pipeline", Arrays.asList(
					new Document("$match",
							new Document("IssuerId", header.getIssuerId()).append("_id", objectIdUser)
									.append("IsActive", true).append("IsDelete", new Document("$ne", true))),
					new Document("$project", new Document("_id", 1).append("UserName", 1).append("FullName", 1)),
					new Document("$limit", 1))).append("as", "UserInfo")));
			pipeline.add(new Document("$unwind",
					new Document("path", "$UserInfo").append("preserveNullAndEmptyArrays", true)));
			pipeline.add(new Document("$lookup", new Document("from", "DMTinhThanh")
					.append("pipeline", Arrays.asList(
							new Document("$match",
									new Document("IsDelete", new Document("$ne", true)).append("code",
											commons.regexEscapeForMongoQuery(tinhThanh))),
							new Document("$project", new Document("_id", 0).append("code", 1).append("name", 1))))
					.append("as", "DMTinhThanhInfo")));
			pipeline.add(new Document("$unwind",
					new Document("path", "$DMTinhThanhInfo").append("preserveNullAndEmptyArrays", true)));
			pipeline.add(new Document("$lookup", new Document("from", "DMChiCucThue")
					.append("pipeline", Arrays.asList(
							new Document("$match",
									new Document("IsDelete", new Document("$ne", true)).append("code",
											commons.regexEscapeForMongoQuery(cqtQLy))),
							new Document("$project", new Document("_id", 0).append("code", 1).append("name", 1))))
					.append("as", "DMChiCucThueInfo")));
			pipeline.add(new Document("$unwind",
					new Document("path", "$DMChiCucThueInfo").append("preserveNullAndEmptyArrays", true)));

			docFind = new Document("IssuerId", header.getIssuerId()).append("IsDelete", new Document("$ne", true))
					.append("Status", Constants.INVOICE_STATUS.CREATED).append("_id", objectIdTK);
			pipeline.add(new Document("$lookup",
					new Document("from", "DMCTSo").append("pipeline", Arrays.asList(new Document("$match", docFind)
					)).append("as", "DMCTSo")));
			pipeline.add(new Document("$unwind",
					new Document("path", "$DMCTSo").append("preserveNullAndEmptyArrays", true)));

			mongoClient = cfg.mongoClient();
			collection = mongoClient.getDatabase(cfg.dbName).getCollection("Issuer");
			try {
				docTmp = collection.aggregate(pipeline).allowDiskUse(true).iterator().next();
			} catch (Exception e) {
				// TODO: handle exception
			}
			mongoClient.close();

			if (null == docTmp) {
				responseStatus = new MspResponseStatus(9999, "Không tìm thấy thông tin khách hàng.");
				rsp.setResponseStatus(responseStatus);
				return rsp;
			}
			if (docTmp.get("UserInfo") == null) {
				responseStatus = new MspResponseStatus(9999, "Không tìm thấy thông tin người dùng.");
				rsp.setResponseStatus(responseStatus);
				return rsp;
			}

			if (docTmp.get("DMTinhThanhInfo") == null || docTmp.get("DMChiCucThueInfo") == null) {
				responseStatus = new MspResponseStatus(9999, "Vui lòng kiểm tra lại tỉnh/thành phố và cơ quan thuế.");
				rsp.setResponseStatus(responseStatus);
				return rsp;
			}

			if (docTmp.get("DMCTSo") == null) {
				responseStatus = new MspResponseStatus(9999, "Vui lòng kiểm tra lại thông tin chứng thư số.");
				rsp.setResponseStatus(responseStatus);
				return rsp;
			}

			taxCode = docTmp.getString("TaxCode");

			options = new FindOneAndUpdateOptions();
			options.upsert(false);
			options.maxTime(5000, TimeUnit.MILLISECONDS);
			options.returnDocument(ReturnDocument.AFTER);

			mongoClient = cfg.mongoClient();
			collection = mongoClient.getDatabase(cfg.dbName).getCollection("DMCTSo");
			docR = collection.findOneAndUpdate(docFind, new Document("$set", new Document("TenNnt", tenNnt)
					.append("MSo", mauSo).append("Ten", ten)

					.append("MST", mst).append("TinhThanhInfo", docTmp.get("DMTinhThanhInfo"))
					.append("ChiCucThueInfo", docTmp.get("DMChiCucThueInfo"))

					.append("NLap", commons.convertStringToLocalDate(nLap, Constants.FORMAT_DATE.FORMAT_DATE_WEB))

					.append("DSCTSSDung", rowDSCTSSDung).append("Status", Constants.INVOICE_STATUS.TK_CREATED)
					.append("IsDelete", false).append("IsActive", true).append("InfoUpdated",
							new Document("UpdatedDate", LocalDateTime.now()).append("UpdatedUserID", header.getUserId())
									.append("UpdatedUserName", header.getUserName())
									.append("UpdatedUserFullName", header.getUserFullName()))),
					options);

			mongoClient.close();

			responseStatus = new MspResponseStatus(0, "SUCCESS");
			rsp.setResponseStatus(responseStatus);
			return rsp;

		case Constants.MSG_ACTION_CODE.DELETE:
			objectId = null;
			try {
				objectId = new ObjectId(_id);
			} catch (Exception e) {
			}

			docFind = new Document("IssuerId", header.getIssuerId()).append("_id", objectId)
					.append("IsDelete", new Document("$ne", true)).append("Status", "CREATED");

			docTmp = null;

			mongoClient = cfg.mongoClient();
			collection = mongoClient.getDatabase(cfg.dbName).getCollection("DMCTSo");
			try {
				docTmp = collection.find(docFind).allowDiskUse(true).iterator().next();
			} catch (Exception e) {
				// TODO: handle exception
			}
			mongoClient.close();

			if (null == docTmp) {
				responseStatus = new MspResponseStatus(9999, "Không tìm thấy thông tin tờ khai.");
				rsp.setResponseStatus(responseStatus);
				return rsp;
			}

			options = new FindOneAndUpdateOptions();
			options.upsert(false);
			options.maxTime(5000, TimeUnit.MILLISECONDS);
			options.returnDocument(ReturnDocument.AFTER);

			mongoClient = cfg.mongoClient();
			collection = mongoClient.getDatabase(cfg.dbName).getCollection("DMCTSo");
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
		default:
			responseStatus = new MspResponseStatus(9998, Constants.MAP_ERROR.get(9998));
			rsp.setResponseStatus(responseStatus);
			return rsp;
		}

	}

	@Override
	public MsgRsp list(JSONRoot jsonRoot) throws Exception {
		Msg msg = jsonRoot.getMsg();
		MsgHeader header = msg.getMsgHeader();
		MsgPage page = msg.getMsgPage();
		Object objData = msg.getObjData();

		MsgRsp rsp = new MsgRsp(header);
		MspResponseStatus responseStatus = null;

//		ObjectId objectId = null;
//		Document docTmp = null;
		Iterator<Document> iter = null;
		List<Document> pipeline = new ArrayList<Document>();

		List<Document> rows = new ArrayList<Document>();

		Document docMatch = new Document("IssuerId", header.getIssuerId()).append("IsDelete",
				new Document("$ne", true));

		Document fillter = new Document("_id", 1).append("DSCTSSDung", 1).append("MST", 1).append("TenNnt", 1)
				.append("Status", 1).append("StatusCQT", 1).append("ChiCucThueInfo", 1);

		pipeline = new ArrayList<Document>();
		pipeline.add(new Document("$match", docMatch));
		pipeline.add(new Document("$sort", new Document("_id", -1)));
		pipeline.add(new Document("$set", new Document("_id", new Document("$toString", "$_id"))));
		pipeline.add(new Document("$project", fillter));

		MongoClient mongoClient = cfg.mongoClient();
		MongoCollection<Document> collection = mongoClient.getDatabase(cfg.dbName).getCollection("DMCTSo");
		iter = collection.aggregate(pipeline).allowDiskUse(true).iterator();
		mongoClient.close();
		while (iter.hasNext()) {
			rows.add(iter.next());
		}

		rsp = new MsgRsp(header);
		responseStatus = null;

		responseStatus = new MspResponseStatus(0, "SUCCESS");
		rsp.setResponseStatus(responseStatus);
		rsp.setObjData(rows);
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

		MongoClient mongoClient = cfg.mongoClient();
		MongoCollection<Document> collection = mongoClient.getDatabase(cfg.dbName).getCollection("DMCTSo");
		try {
			docTmp = collection.find(docFind).allowDiskUse(true).iterator().next();
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

	@Override
	public MsgRsp check(JSONRoot jsonRoot, String _id) throws Exception {
		Msg msg = jsonRoot.getMsg();
		MsgHeader header = msg.getMsgHeader();
		MsgPage page = msg.getMsgPage();
		List<Document> pipeline = new ArrayList<Document>();
		MsgRsp rsp = new MsgRsp(header);
		rsp.setMsgPage(page);
		MspResponseStatus responseStatus = null;
		Document docTmp = null;

		Document docUpsert = null;
		ObjectId objectIdUser = null;

		Object objData = msg.getObjData();

		JsonNode jsonData = null;
		if (objData != null) {
			jsonData = Json.serializer().nodeFromObject(msg.getObjData());
		} else {
			throw new Exception("Lỗi dữ liệu đầu vào");
		}

		try {
			objectIdUser = new ObjectId(header.getIssuerId());
		} catch (Exception e) {
		}

		pipeline = new ArrayList<Document>();
		pipeline.add(new Document("$match",
				new Document("_id", objectIdUser).append("IsDelete", new Document("$ne", true))));
		pipeline.add(
				new Document("$lookup",
						new Document("from", "DMCTSo")
								.append("pipeline",
										Arrays.asList(
												new Document("$match", new Document("IssuerId", header.getIssuerId())
														// .append("DSCTSSDung.Seri", _id)

														.append("$or",
																Arrays.asList(new Document("IsActive", true),
																		new Document("IsActive", null)))
														.append("IsDelete", new Document("$ne", true))),
												new Document("$sort", new Document("NLap", -1).append("_id", -1)),
												new Document("$limit", 1)))
								.append("as", "DMCTSo")));
		pipeline.add(
				new Document("$unwind", new Document("path", "$DMCTSo").append("preserveNullAndEmptyArrays", true)));

		pipeline.add(new Document("$lookup", new Document("from", "DMTKhai")
				.append("pipeline", Arrays.asList(new Document("$match", new Document("IssuerId", header.getIssuerId())
						// .append("DSCTSSDung.Seri", _id)

						.append("IsDelete", new Document("$ne", true))),
						new Document("$sort", new Document("NLap", -1).append("_id", -1)), new Document("$limit", 1)))
				.append("as", "DMTKhai")));
		pipeline.add(
				new Document("$unwind", new Document("path", "$DMTKhai").append("preserveNullAndEmptyArrays", true)));

		MongoClient mongoClient = cfg.mongoClient();
		MongoCollection<Document> collection = mongoClient.getDatabase(cfg.dbName).getCollection("Issuer");
		try {
			docTmp = collection.aggregate(pipeline).allowDiskUse(true).iterator().next();
		} catch (Exception e) {
			// TODO: handle exception
		}

		mongoClient.close();

		if (docTmp.getEmbedded(Arrays.asList("DMCTSo"), "") == "") {

			if (docTmp.getEmbedded(Arrays.asList("DMTKhai"), "") != "") {

				String status = docTmp.getEmbedded(Arrays.asList("DMTKhai", "Status"), "");
				if (status.equals("PROCESSING") || status.equals("COMPLETE")) {

					rsp.setObjData(docTmp.getEmbedded(Arrays.asList("DMTKhai"), ""));
					responseStatus = new MspResponseStatus(0, Constants.MAP_ERROR.get(0));
					rsp.setResponseStatus(responseStatus);
				} else {
					responseStatus = new MspResponseStatus(301, Constants.MAP_ERROR.get(0));
					rsp.setResponseStatus(responseStatus);
				}

			} else {

				responseStatus = new MspResponseStatus(300, Constants.MAP_ERROR.get(0));
				rsp.setResponseStatus(responseStatus);
			}
		} else {

			rsp.setObjData(docTmp.getEmbedded(Arrays.asList("DMCTSo"), ""));
			responseStatus = new MspResponseStatus(0, Constants.MAP_ERROR.get(0));
			rsp.setResponseStatus(responseStatus);
		}
//	}else {
//			rsp.setObjData(docTmp.getEmbedded(Arrays.asList("CHECK_DMCTSo"), ""));
//			responseStatus = new MspResponseStatus(0, Constants.MAP_ERROR.get(0));
//			rsp.setResponseStatus(responseStatus);
//		}

		return rsp;
	}
}
