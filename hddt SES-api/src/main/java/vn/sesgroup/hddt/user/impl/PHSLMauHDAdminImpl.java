package vn.sesgroup.hddt.user.impl;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.TimeUnit;

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
import vn.sesgroup.hddt.user.dao.PHSLMauHDAdminDAO;
import vn.sesgroup.hddt.user.service.TCTNService;
import vn.sesgroup.hddt.utility.Constants;
import vn.sesgroup.hddt.utility.Json;

@Repository
@Transactional
public class PHSLMauHDAdminImpl extends AbstractDAO implements PHSLMauHDAdminDAO {
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
		MsgRsp rsp = new MsgRsp(header);
		rsp.setMsgPage(page);
		MspResponseStatus responseStatus = null;
		
		
		JsonNode jsonData = null;
		if (objData != null) {
			jsonData = Json.serializer().nodeFromObject(msg.getObjData());
		} else {
			throw new Exception("Lỗi dữ liệu đầu vào");
		}

		String actionCode = header.getActionCode();
		String _id = commons.getTextJsonNode(jsonData.at("/_id")).replaceAll("\\s", "");
		String mausohdon = commons.getTextJsonNode(jsonData.at("/mausohdon")).replaceAll("\\s", "");
		String mstkh = commons.getTextJsonNode(jsonData.at("/mstkh")).replaceAll("\\s", "");
		String quantity = commons.getTextJsonNode(jsonData.at("/quantity")).replaceAll("\\s", "");
		
		List<Document> pipeline = null;
		Document docFind = null;
		Document docTmp = null;
		Document docUpsert = null;
		FindOneAndUpdateOptions options = null;

		switch (actionCode) {
		case Constants.MSG_ACTION_CODE.CREATED:		
			docFind = new Document("TaxCode", mstkh)
					.append("IsActive", true)
					.append("IsDelete", new Document("$ne", true))
					;
			
			pipeline = new ArrayList<Document>();
			pipeline.add(new Document("$match",docFind));
			pipeline.add(new Document("$project", new Document("_id", 1).append("TaxCode", 1)));
			
			
			pipeline.add(new Document("$lookup", new Document("from", "DMMauSoKyHieu")
					.append("let", new Document("issuerIdString", new Document("$toString", "$_id")))
					.append("pipeline", Arrays.asList(
										new Document("$match", 
											new Document("$expr",
													new Document("$and", Arrays.asList(
															 new Document("$eq", Arrays.asList("$IssuerId", "$$issuerIdString")),
															 new Document("$eq", Arrays.asList("$IsActive", true)),
															 new Document("$eq", Arrays.asList("$IsDelete", false)),
															 new Document("$eq", Arrays.asList("$_id", new Document("$toObjectId", mausohdon)))
															)
															)
													)
											),
										new Document("$project", 
												new Document("_id", 1)
												.append("KHMSHDon", 1)
												.append("KHHDon", 1)
												.append("Templates.Name", 1)
												.append("SoLuong", 1)
												.append("ConLai", 1)
												.append("DenSo", 1)
												)
										)
							).append("as", "DMMauSoKyHieu")
					));
			pipeline.add(new Document("$unwind",
					new Document("path", "$DMMauSoKyHieu").append("preserveNullAndEmptyArrays", true)));
			
			
			pipeline.add(
					new Document("$lookup",new Document("from", "DMQuantity")
							.append("let", new Document("issuerIdString", new Document("$toString", "$_id")))
							.append("pipeline", Arrays.asList(
									new Document("$match", 
										new Document("$expr",
												new Document("$and", Arrays.asList(
														 new Document("$eq", Arrays.asList("$IssuerId", "$$issuerIdString")),
														 new Document("$eq", Arrays.asList("$MSKHieu", mausohdon))
														)
														)
												)
										)
									)
						).append("as", "DMQuantity")));
			pipeline.add(new Document("$unwind",
					new Document("path", "$DMQuantity").append("preserveNullAndEmptyArrays", true)));
			
			pipeline.add(
					new Document("$lookup",new Document("from", "DMDepot")
							.append("pipeline", Arrays.asList(
									new Document("$match", 
										new Document("$expr",
												new Document("$and", Arrays.asList(
														 new Document("$eq", Arrays.asList("$TaxCode", mstkh))
														)
														)
												)
										),
									new Document("$project", new Document("_id", 1).append("SLHDon", 1).append("SLHDonDD", 1).append("SLHDonCL", 1))
									)
						).append("as", "DMDepot")));
			pipeline.add(new Document("$unwind",
					new Document("path", "$DMDepot").append("preserveNullAndEmptyArrays", true)));

			try (MongoClient mongoClient = cfg.mongoClient()){
				MongoCollection<Document> collection = mongoClient.getDatabase(cfg.dbName).getCollection("Issuer");
				docTmp = collection.aggregate(pipeline).allowDiskUse(true).iterator().next();
			} catch (Exception e) {

			}
			
			if (docTmp == null) {
				responseStatus = new MspResponseStatus(9999, "Không tìm thấy thông tin khách hàng.");
				rsp.setResponseStatus(responseStatus);
				return rsp;
			}

			String KHMSHDon = docTmp.getEmbedded(Arrays.asList("DMMauSoKyHieu", "KHMSHDon"), "");
			String KHHDon = docTmp.getEmbedded(Arrays.asList("DMMauSoKyHieu", "KHHDon"), "");
			String NamePhoi = docTmp.getEmbedded(Arrays.asList("DMMauSoKyHieu", "Templates", "Name"), "");
			
			int STT = docTmp.getEmbedded(Arrays.asList("DMQuantity", "STT"), 0);
			int LanPH = docTmp.getEmbedded(Arrays.asList("DMQuantity", "LanPH"), 0);

			int SL = docTmp.getEmbedded(Arrays.asList("DMMauSoKyHieu", "SoLuong"), 0);
			int DS = docTmp.getEmbedded(Arrays.asList("DMMauSoKyHieu", "DenSo"), 0);
			int CL = docTmp.getEmbedded(Arrays.asList("DMMauSoKyHieu", "ConLai"), 0);

			int SLKho = docTmp.getEmbedded(Arrays.asList("DMDepot", "SLHDon"), 0);
			int SLKhoCL = docTmp.getEmbedded(Arrays.asList("DMDepot", "SLHDonCL"), 0);
			int SLKhoDD = docTmp.getEmbedded(Arrays.asList("DMDepot", "SLHDonDD"), 0);
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

			try (MongoClient mongoClient = cfg.mongoClient()){
				MongoCollection<Document> collection = mongoClient.getDatabase(cfg.dbName).getCollection("DMMauSoKyHieu");
				collection.findOneAndUpdate(
						new Document("_id", new ObjectId(mausohdon)),
						new Document("$set", 
								new Document("SoLuong", SLDMMS)
								.append("TuSo", 1)
								.append("DenSo", DSDMMS)
								.append("ConLai", CLDMMS)
								.append("IsActive", true)
								.append("InfoUpdated",
										new Document("UpdatedDate", LocalDateTime.now())
										.append("UpdatedUserID", header.getUserId())
										.append("UpdatedUserName", header.getUserName())
										.append("UpdatedUserFullName", header.getUserFullName()))),
						options);
				
				
				
				collection = mongoClient.getDatabase(cfg.dbName).getCollection("DMDepot");
				collection.findOneAndUpdate(
						new Document("TaxCode", mstkh), 
						new Document("$set",
								new Document("SLHDon", SLKho)
								.append("SLHDonDD", SLKhoUpdateDD)
								.append("SLHDonCL", SLKhoUpdateCL)),
						options);

				int stt1 = STT + 1;
				int lanph = LanPH + 1;
				int TuSoQuantity = SL + 1;
				
				docUpsert = new Document("IssuerId", docTmp.get("_id").toString())
						.append("STT", stt1)
						.append("LanPH", lanph)
						.append("MSKHieu", mausohdon)
						.append("KHMSHDon", KHMSHDon)
						.append("KHMSHDon", KHMSHDon)
						.append("KHHDon", KHHDon)
						.append("NamePhoi", NamePhoi)
						.append("SoLuong", SLDMMS)
						.append("TuSo", TuSoQuantity)
						.append("DenSo", DSDMMS)
						.append("GChu", "")
						.append("NLap", LocalDate.now());
				collection = mongoClient.getDatabase(cfg.dbName).getCollection("DMQuantity");
				collection.insertOne(docUpsert);
			} catch (Exception e) {
				
			}
			
			responseStatus = new MspResponseStatus(9999, "Không tìm thấy thông tin khách hàng.");
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

		String mausohd = "";
		String mstkh = "";

		JsonNode jsonData = null;

		if (objData != null) {
			jsonData = Json.serializer().nodeFromObject(objData);
			mausohd = commons.getTextJsonNode(jsonData.at("/MauSoHdon")).replaceAll("\\s", "");
			mstkh = commons.getTextJsonNode(jsonData.at("/MSTKH")).replaceAll("\\s", "");
		}

		MsgRsp rsp = new MsgRsp(header);
		MspResponseStatus responseStatus = null;

		ObjectId objectId = null;
		Document docTmp = null;
		Document docIssuer = null;
		List<Document> pipeline = new ArrayList<Document>();
		
		if (!"".equals(mstkh)) {
			try (MongoClient mongoClient = cfg.mongoClient()){
				MongoCollection<Document> collection = mongoClient.getDatabase(cfg.dbName).getCollection("Issuer");
				docIssuer = collection.find(new Document("TaxCode", mstkh)).allowDiskUse(true).iterator().next();
			} catch (Exception e) {
			}
		}
		
		Document docMatch = new Document();
		if (docIssuer != null) {
			docMatch.append("IssuerId", docIssuer.get("_id").toString());
		}
		
		if (!mausohd.equals("")) {
		     String rest = mausohd.substring(1);
		     docMatch.append("KHHDon", new Document("$regex", commons.regexEscapeForMongoQuery(rest)).append("$options", "i"));
		}
		
		
		Document fillter = new Document("_id", 1).append("KHMSHDon", 1).append("NamePhoi", 1).append("KHHDon", 1).append("IssuerId", 1)
				.append("SoLuong", 1).append("TuSo", 1).append("DenSo", 1).append("NLap", 1);

		pipeline = new ArrayList<Document>();
		pipeline.add(new Document("$match", docMatch));
		pipeline.add(new Document("$project", fillter));
		pipeline.add(new Document("$lookup", new Document("from", "Issuer")
				.append("let", new Document("issuerObjectId", new Document("$toObjectId", "$IssuerId")))
				.append("pipeline", Arrays.asList(
									new Document("$match", 
										new Document("$expr",
												new Document("$and", Arrays.asList(
														 new Document("$eq", Arrays.asList("$_id", "$$issuerObjectId"))
														)
														)
												)
										),
									new Document("$project", new Document("_id", new Document("$toString", "$_id")).append("TaxCode", 1))
									)
						).append("as", "Issuer")
				));
		pipeline.add(new Document("$unwind",
				new Document("path", "$Issuer").append("preserveNullAndEmptyArrays", true)));
		
		if (!header.getCurUserId().equals(header.getUserId())) {
			pipeline.add(new Document("$match",
					new Document("$or", Arrays.asList(new Document("Issuer.InfoCreated.CreateBySubUserID", header.getCurUserId()),
							new Document("Issuer.ManagedByUsers", new Document("$in", Arrays.asList(header.getCurUserId())))))
					));
		}
		
		pipeline.add(new Document("$sort", new Document("NLap", -1)));
		pipeline.addAll(createFacetForSearchNotSort(page));

		try (MongoClient mongoClient = cfg.mongoClient()){
			MongoCollection<Document> collection = mongoClient.getDatabase(cfg.dbName).getCollection("DMQuantity");
			docTmp = collection.aggregate(pipeline).allowDiskUse(true).iterator().next();
		} catch (Exception e) {
			System.out.println(e);
		}

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
				hItem.put("Issuer", doc.get("Issuer"));
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
		Document fillter = new Document("_id", 1).append("KHMSHDon", 1).append("NamePhoi", 1).append("KHHDon", 1).append("IssuerId", 1)
				.append("SoLuong", 1).append("TuSo", 1).append("DenSo", 1).append("NLap", 1);

		Document docTmp = null;

		ArrayList<Document> pipeline = new ArrayList<Document>();
		pipeline.add(new Document("$match", docFind));
		pipeline.add(new Document("$project", fillter));
		pipeline.add(new Document("$lookup", new Document("from", "Issuer")
				.append("let", new Document("issuerObjectId", new Document("$toObjectId", "$IssuerId")))
				.append("pipeline", Arrays.asList(
									new Document("$match", 
										new Document("$expr",
												new Document("$and", Arrays.asList(
														 new Document("$eq", Arrays.asList("$_id", "$$issuerObjectId"))
														)
														)
												)
										),
									new Document("$project", new Document("_id", new Document("$toString", "$_id")).append("TaxCode", 1))
									)
						).append("as", "Issuer")
				));
		pipeline.add(new Document("$unwind",
				new Document("path", "$Issuer").append("preserveNullAndEmptyArrays", true)));

		try (MongoClient mongoClient = cfg.mongoClient()){
			MongoCollection<Document> collection = mongoClient.getDatabase(cfg.dbName).getCollection("DMQuantity");
			docTmp = collection.aggregate(pipeline).allowDiskUse(true).iterator().next();
		} catch (Exception e) {

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
	public MsgRsp getMSHD(JSONRoot jsonRoot, String taxCode) throws Exception {
		Msg msg = jsonRoot.getMsg();
		MsgHeader header = msg.getMsgHeader();

		MsgRsp rsp = new MsgRsp(header);
		MspResponseStatus responseStatus = null;

		Document docTmp = null;
		Document docFind = new Document("TaxCode", taxCode);
		if (!header.getCurUserId().equals(header.getUserId())) {
			docFind.append("$or", Arrays.asList(
					new Document("InfoCreated.CreateBySubUserID", header.getCurUserId()),
					new Document("ManagedByUsers", new Document("$in", Arrays.asList(header.getCurUserId())))
					));
		}

		List<Document> pipeline = new ArrayList<Document>();
		
		pipeline.add(new Document("$match", docFind));
		pipeline.add(new Document("$project", new Document("_id", 1)));
		pipeline.add(new Document("$lookup", new Document("from", "DMMauSoKyHieu")
				.append("let", new Document("issuerIdString", new Document("$toString", "$_id")))
				.append("pipeline", Arrays.asList(
									new Document("$match", 
										new Document("$expr",
												new Document("$and", Arrays.asList(
														 new Document("$eq", Arrays.asList("$IssuerId", "$$issuerIdString")),
														 new Document("$eq", Arrays.asList("$IsActive", true)),
														 new Document("$eq", Arrays.asList("$IsDelete", false))
														)
														)
												)
										),
									new Document("$sort", new Document("NamPhatHanh", -1)),
									new Document("$project", new Document("_id", new Document("$toString", "$_id")).append("KHMSHDon", 1).append("KHHDon", 1).append("IssuerId", 1))
									)
						).append("as", "DMMauSoKyHieu")
				));

		try (MongoClient mongoClient = cfg.mongoClient()){
			MongoCollection<Document> collection = mongoClient.getDatabase(cfg.dbName).getCollection("Issuer");

			docTmp = collection.aggregate(pipeline).allowDiskUse(true).iterator().next();
		} catch (Exception e) {

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
