package vn.sesgroup.hddt.user.impl;

import java.io.File;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

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
import vn.sesgroup.hddt.user.dao.ChangeMTDiepDAO;
import vn.sesgroup.hddt.user.dao.SendMailAsyncDAO;
import vn.sesgroup.hddt.user.service.JPUtils;
import vn.sesgroup.hddt.user.service.TCTNService;
import vn.sesgroup.hddt.utility.Constants;
import vn.sesgroup.hddt.utility.Json;
import vn.sesgroup.hddt.utility.MailUtils;
import vn.sesgroup.hddt.utility.SystemParams;

@Repository
@Transactional
public class ChangeMTDiepImpl extends AbstractDAO implements ChangeMTDiepDAO {
	private static final Logger log = LogManager.getLogger(ChangeMTDiepImpl.class);
	@Autowired
	ConfigConnectMongo cfg;
	@Autowired SendMailAsyncDAO sendMailAsyncDAO;
	@Autowired
	TCTNService tctnService;
	private MailUtils mailUtils = new MailUtils();
	@Autowired
	JPUtils jpUtils;
	Document docUpsert = null;


	
	@Override
	public MsgRsp list(JSONRoot jsonRoot) throws Exception {
		Msg msg = jsonRoot.getMsg();
		MsgHeader header = msg.getMsgHeader();
		MsgPage page = msg.getMsgPage();
		Object objData = msg.getObjData();

		String mauSoHdon = "";
		String soHoaDon = "";
		String fromDate = "";
		String toDate = "";
		String status = "";
		String signStatus = "";
		String nbanMst = "";
		String nbanTen = "";
		String maHoaDon = "";
		JsonNode jsonData = null;
		if (objData != null) {
			jsonData = Json.serializer().nodeFromObject(objData);

			mauSoHdon = commons.getTextJsonNode(jsonData.at("/MauSoHdon")).replaceAll("\\s", "");
			soHoaDon = commons.getTextJsonNode(jsonData.at("/SoHoaDon")).replaceAll("\\s", "");
			fromDate = commons.getTextJsonNode(jsonData.at("/FromDate")).replaceAll("\\s", "");
			toDate = commons.getTextJsonNode(jsonData.at("/ToDate")).replaceAll("\\s", "");
			status = commons.getTextJsonNode(jsonData.at("/Status")).replaceAll("\\s", "");
			signStatus = commons.getTextJsonNode(jsonData.at("/SignStatus")).replaceAll("\\s", "");
			nbanMst = commons.getTextJsonNode(jsonData.at("/NbanMst")).trim().replaceAll("\\s+", " ");
			nbanTen = commons.getTextJsonNode(jsonData.at("/NbanTen")).trim().replaceAll("\\s+", " ");
			maHoaDon = commons.getTextJsonNode(jsonData.at("/MaHDon")).trim().replaceAll("\\s+", " ");
		}

		MsgRsp rsp = new MsgRsp(header);
		MspResponseStatus responseStatus = null;

		ObjectId objectId = null;
		Iterator<Document> iter = null;
		List<Document> pipeline = new ArrayList<Document>();
		List<Document> docsTmp = new ArrayList<Document>();

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

		Document baseCondition = new Document("IssuerId", header.getIssuerId())
		        .append("IsDelete", new Document("$ne", true));

		List<Document> orConditions = Arrays.asList(
		        new Document("EInvoiceStatus", "PROCESSING"),
		        new Document("$and", Arrays.asList(
		                new Document("EInvoiceStatus", "ERROR_CQT"),
		                new Document("LDo.MLoi", 1002)
		        ))
		);

		Document docMatch = new Document("$and", Arrays.asList(
		        baseCondition,
		        new Document("$or", orConditions)
		));
		if (!"".equals(mauSoHdon))
			docMatch.append("EInvoiceDetail.TTChung.MauSoHD", commons.regexEscapeForMongoQuery(mauSoHdon));
		if (!"".equals(soHoaDon))
			docMatch.append("EInvoiceDetail.TTChung.SHDon", commons.stringToInteger(soHoaDon));
		if (null != docMatchDate)
			docMatch.append("EInvoiceDetail.TTChung.NLap", docMatchDate);
		if (!"".equals(status))
			docMatch.append("EInvoiceStatus", commons.regexEscapeForMongoQuery(status));
		if (!"".equals(signStatus))
			docMatch.append("SignStatusCode", commons.regexEscapeForMongoQuery(signStatus));
		if (!"".equals(nbanMst))
			docMatch.append("EInvoiceDetail.NDHDon.NMua.MST",
					new Document("$regex", commons.regexEscapeForMongoQuery(nbanMst)).append("$options", "i"));
		if (!"".equals(nbanTen)) {
			docMatch.append("$or",
					Arrays.asList(
							new Document("EInvoiceDetail.NDHDon.NMua.Ten",
									new Document("$regex", commons.regexEscapeForMongoQuery(nbanTen)).append("$options",
											"i")),
							new Document("EInvoiceDetail.NDHDon.NMua.HVTNMHang",
									new Document("$regex", commons.regexEscapeForMongoQuery(nbanTen)).append("$options",
											"i"))));
//			docMatch.append("EInvoiceDetail.NDHDon.NMua.Ten", new Document("$regex", commons.regexEscapeForMongoQuery(nbanTen)).append("$options", "i"));
		}
		if (!"".equals(maHoaDon))
			docMatch.append("EInvoiceDetail.TTChung.MaHD", commons.regexEscapeForMongoQuery(maHoaDon));

	
		Document fillter = new Document("_id", 1).append("EInvoiceStatus", 1).append("SignStatusCode", 1)
				.append("MCCQT", 1).append("MTDiep", 1).append("EInvoiceDetail", 1).append("InfoCreated", 1).append("LDo", 1)
				.append("HDSS", 1).append("MTDiep", 1).append("MTDTChieu", 1);
				
		
		pipeline = new ArrayList<Document>();
		pipeline.add(new Document("$match", docMatch));
		pipeline.add(new Document("$addFields", new Document("SHDon",
				new Document("$ifNull", Arrays.asList("$EInvoiceDetail.TTChung.SHDon", Integer.MAX_VALUE)))));
		pipeline.add(new Document("$sort",
				new Document("EInvoiceDetail.TTChung.MauSoHD", -1).append("SHDon", -1).append("_id", -1)));
		pipeline.add(new Document("$project", fillter));
//		pipeline.addAll(createFacetForSearchNotSort(page));

		try (MongoClient mongoClient = cfg.mongoClient()) {
			MongoCollection<Document> collection = mongoClient.getDatabase(cfg.dbName).getCollection("EInvoice");
			iter = collection.aggregate(pipeline).allowDiskUse(true).iterator();
			while (iter.hasNext()) {
				docsTmp.add(iter.next());
			}
			
			iter = null;
			collection = mongoClient.getDatabase(cfg.dbName).getCollection("EInvoiceBH");
			iter = collection.aggregate(pipeline).allowDiskUse(true).iterator();
			while (iter.hasNext()) {
				docsTmp.add(iter.next());
			}
			
			iter = null;
			collection = mongoClient.getDatabase(cfg.dbName).getCollection("EInvoicePXK");
			iter = collection.aggregate(pipeline).allowDiskUse(true).iterator();
			while (iter.hasNext()) {
				docsTmp.add(iter.next());
			}
			
			iter = null;
			collection = mongoClient.getDatabase(cfg.dbName).getCollection("EInvoicePXKDL");
			iter = collection.aggregate(pipeline).allowDiskUse(true).iterator();
			while (iter.hasNext()) {
				docsTmp.add(iter.next());
			}
			
			
			iter = null;
			collection = mongoClient.getDatabase(cfg.dbName).getCollection("EInvoiceMTT");
			iter = collection.aggregate(pipeline).allowDiskUse(true).iterator();
			while (iter.hasNext()) {
				docsTmp.add(iter.next());
			}
			
			
		} catch (Exception e) {
		}
		
		rsp = new MsgRsp(header);
		
		ArrayList<HashMap<String, Object>> rowsReturn = new ArrayList<HashMap<String, Object>>();
		HashMap<String, Object> hItem = null;
		int totalRows = docsTmp.size();
		if (totalRows > 0) {
			for (Document doc : docsTmp) {
				objectId = (ObjectId) doc.get("_id");

				hItem = new HashMap<String, Object>();
				hItem.put("_id", objectId.toString());
				hItem.put("EInvoiceStatus", doc.get("EInvoiceStatus"));
				hItem.put("SignStatusCode", doc.get("SignStatusCode"));
				hItem.put("MCCQT", doc.get("MCCQT"));
				hItem.put("MTDiep", doc.get("MTDiep"));
				hItem.put("EInvoiceDetail", doc.get("EInvoiceDetail"));
				hItem.put("InfoCreated", doc.get("InfoCreated"));
				hItem.put("LDo", doc.get("LDo"));
				hItem.put("HDSS", doc.get("HDSS"));
				hItem.put("MTDiep", doc.get("MTDiep"));
				hItem.put("MTDTChieu", doc.get("MTDTChieu"));
				
				rowsReturn.add(hItem);
			}
		}
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
	public MsgRsp change(JSONRoot jsonRoot, String _id) throws Exception {
		ObjectId objectId = null;
		try {
			objectId = new ObjectId(_id);
		} catch (Exception e) {
		}
		Document docFind = null;
		Msg msg = jsonRoot.getMsg();
		MsgHeader header = msg.getMsgHeader();
		MsgRsp rsp = new MsgRsp(header);
		MspResponseStatus responseStatus = null;
		List<Object> oldMTDs = new ArrayList<Object>();
		HashMap<String, Object> hItem = null;
		String collectionName = "";
		Document docTmp = null;
		List<Document> pipeline = null;
		pipeline = new ArrayList<Document>();
		pipeline.add(
				new Document("$match", new Document("_id", objectId).append("IsDelete", new Document("$ne", true))));

		try (MongoClient mongoClient = cfg.mongoClient()) {
			collectionName = "EInvoice";
			MongoCollection<Document> collection = mongoClient.getDatabase(cfg.dbName).getCollection(collectionName);
			Iterator<Document> iter = collection.aggregate(pipeline).allowDiskUse(true).iterator();
			if (iter.hasNext()) {
				docTmp = iter.next();
			}

			if (null == docTmp) {
				collectionName = "EInvoiceBH";
				collection = mongoClient.getDatabase(cfg.dbName).getCollection(collectionName);
				iter = collection.aggregate(pipeline).allowDiskUse(true).iterator();
				if (iter.hasNext()) {
					docTmp = iter.next();
				}
			}

			if (null == docTmp) {
				collectionName = "EInvoicePXK";
				collection = mongoClient.getDatabase(cfg.dbName).getCollection(collectionName);
				iter = collection.aggregate(pipeline).allowDiskUse(true).iterator();
				if (iter.hasNext()) {
					docTmp = iter.next();
				}
			}

			if (null == docTmp) {
				collectionName = "EInvoicePXKDL";
				collection = mongoClient.getDatabase(cfg.dbName).getCollection(collectionName);
				iter = collection.aggregate(pipeline).allowDiskUse(true).iterator();
				if (iter.hasNext()) {
					docTmp = iter.next();
				}
			}

			if (null == docTmp) {
				collectionName = "EInvoiceMTT";
				collection = mongoClient.getDatabase(cfg.dbName).getCollection(collectionName);
				iter = collection.aggregate(pipeline).allowDiskUse(true).iterator();
				if (iter.hasNext()) {
					docTmp = iter.next();
				}
			}
		} catch (Exception e) {
			System.out.println(e);

		}
		
		if (null == docTmp) {
			responseStatus = new MspResponseStatus(9999, "Không tìm thấy thông tin hóa đơn");
			rsp.setResponseStatus(responseStatus);
			return rsp;
		}
		
		String mtdiepcu = docTmp.get("MTDiep", "");
		String MTDiep = SystemParams.MSTTCGP
				+ commons.csRandomAlphaNumbericString(46 - SystemParams.MSTTCGP.length()).toUpperCase();
	
		String MSKH = docTmp.getEmbedded(Arrays.asList("EInvoiceDetail", "TTChung", "KHMSHDon"), "")
				+ docTmp.getEmbedded(Arrays.asList("EInvoiceDetail", "TTChung", "KHHDon"), "");
		Integer SHD = docTmp.getEmbedded(Arrays.asList("EInvoiceDetail", "TTChung", "SHDon"), 0);
		
		ArrayList<HashMap<String, Object>> rowsReturn = new ArrayList<HashMap<String, Object>>();
		List<Document> oldMTDDoc = docTmp.getList("MTDiepCU", Document.class);
		if (oldMTDDoc != null && oldMTDDoc instanceof List) {
			for (Document doc: oldMTDDoc) {
				hItem = new LinkedHashMap<String, Object>();
				hItem.put("Date", doc.get("Date"));
				hItem.put("MTDiep", doc.get("MTDiep"));
				oldMTDs.add(hItem);
			}
		}
		hItem = new LinkedHashMap<String, Object>();
		hItem.put("Date", LocalDateTime.now());
		hItem.put("MTDiep", mtdiepcu);
		oldMTDs.add(hItem);
		
		hItem = new HashMap<String, Object>();
		hItem.put("SHD", SHD);
		hItem.put("MS", MSKH);
		hItem.put("MTDiep", MTDiep);
		hItem.put("MTDiepCU", mtdiepcu);
		rowsReturn.add(hItem);

		FindOneAndUpdateOptions options = new FindOneAndUpdateOptions();
		options.upsert(false);
		options.maxTime(5000, TimeUnit.MILLISECONDS);
		options.returnDocument(ReturnDocument.AFTER);

		docFind = new Document("IssuerId", header.getIssuerId()).append("IsDelete", new Document("$ne", true))
				.append("_id", objectId);
		try (MongoClient mongoClient = cfg.mongoClient()) {
			MongoCollection<Document> collection = mongoClient.getDatabase(cfg.dbName).getCollection(collectionName);
			collection.findOneAndUpdate(
					docFind, 
					new Document("$set",
							new Document("MTDiep", MTDiep)
							.append("EInvoiceStatus", Constants.INVOICE_STATUS.PENDING)
							.append("MTDiepCU", oldMTDs)),
					options);
		}
		
		if (collectionName.equals("EInvoiceMTT")) {
			String dir = docTmp.get("Dir", "");
			String stringId = docTmp.get("_id", ObjectId.class).toString();
			String signStatusCode = docTmp.get("SignStatusCode", "");
			String fileName = stringId + ".xml";
			if (Constants.INVOICE_SIGN_STATUS.SIGNED.equals(signStatusCode)) {
				fileName = stringId + "_signed.xml";
			}
			File file = new File(dir, fileName);

			if (!file.exists()) {
				responseStatus = new MspResponseStatus(999, "Cập nhật Mã thông điệp ở file xml thất bại.");
				rsp.setResponseStatus(responseStatus);
			}
			
			org.w3c.dom.Document doc = commons.fileToDocument(file);
			XPath xPath = XPathFactory.newInstance().newXPath();
			Node nodeTTChung = (Node) xPath.evaluate("/TDiep/TTChung", doc, XPathConstants.NODE);
			Element MTDiepElement = (Element) xPath.evaluate("MTDiep", nodeTTChung, XPathConstants.NODE);
			if (null == MTDiepElement) {
				MTDiepElement = doc.createElement("MTDiep");
				MTDiepElement.setTextContent(MTDiep);
				nodeTTChung.appendChild(MTDiepElement);
			} else {
				MTDiepElement.setTextContent(MTDiep);
			}
			
			boolean isSdaveFile = commons.docW3cToFile(doc, dir, fileName);
			if (!isSdaveFile) {
				throw new Exception("Lưu dữ liệu xml mới không thành công.");
			}
		}
		
		responseStatus = new MspResponseStatus(0, "SUCCESS");
		rsp.setResponseStatus(responseStatus);
		HashMap<String, Object> mapDataR = new HashMap<String, Object>();
		mapDataR.put("rows", rowsReturn);
		rsp.setObjData(mapDataR);
		return rsp;
	}

}
