package vn.sesgroup.hddt.user.impl;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

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
import vn.sesgroup.hddt.user.dao.EInvoiceMTTResponseCQTDAO;
import vn.sesgroup.hddt.user.dao.SendMailAsyncDAO;
import vn.sesgroup.hddt.user.service.JPUtils;
import vn.sesgroup.hddt.user.service.TCTNService;
import vn.sesgroup.hddt.utility.Constants;
import vn.sesgroup.hddt.utility.Json;

@Repository
@Transactional
public class EInvoiceMTTResponseCQTImpl extends AbstractDAO implements EInvoiceMTTResponseCQTDAO {
	@Autowired ConfigConnectMongo cfg;
	
	@Autowired SendMailAsyncDAO sendMailAsyncDAO;
	@Autowired
	TCTNService tctnService;
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
//		String status = "";
		String ResponseStatus = "";
		String nbanMst = "";
		String nbanTen = "";
		
		JsonNode jsonData = null;
		if(objData != null) {
			jsonData = Json.serializer().nodeFromObject(objData);
			mauSoHdon = commons.getTextJsonNode(jsonData.at("/MauSoHdon")).replaceAll("\\s", "");
			soHoaDon = commons.getTextJsonNode(jsonData.at("/SoHoaDon")).replaceAll("\\s", "");
			fromDate = commons.getTextJsonNode(jsonData.at("/FromDate")).replaceAll("\\s", "");
			toDate = commons.getTextJsonNode(jsonData.at("/ToDate")).replaceAll("\\s", "");
//			status = commons.getTextJsonNode(jsonData.at("/Status")).replaceAll("\\s", "");
			ResponseStatus = commons.getTextJsonNode(jsonData.at("/ResponseStatus")).replaceAll("\\s", "");
			nbanMst = commons.getTextJsonNode(jsonData.at("/NbanMst")).trim().replaceAll("\\s+", " ");
			nbanTen = commons.getTextJsonNode(jsonData.at("/NbanTen")).trim().replaceAll("\\s+", " ");
		}

		MsgRsp rsp = new MsgRsp(header);
		MspResponseStatus responseStatus = null;
		Document docMatch = null;
		ObjectId objectId = null;
		Document docTmp = null;

		List<Document> pipeline = new ArrayList<Document>();
		ArrayList<HashMap<String, Object>> rowsReturn = new ArrayList<HashMap<String, Object>>();
		HashMap<String, Object> hItem = null;
		LocalDate dateFrom = null;
		LocalDate dateTo = null;
		Document docMatchDate = null;
//		int rowsign = 0;
		dateFrom =  "".equals(fromDate) || !commons.checkLocalDate(fromDate, Constants.FORMAT_DATE.FORMAT_DATE_WEB)? null: commons.convertStringToLocalDate(fromDate, Constants.FORMAT_DATE.FORMAT_DATE_WEB);
		dateTo = "".equals(toDate) || !commons.checkLocalDate(toDate, Constants.FORMAT_DATE.FORMAT_DATE_WEB)? null: commons.convertStringToLocalDate(toDate, Constants.FORMAT_DATE.FORMAT_DATE_WEB);
		if(null != dateTo)
			dateTo = dateTo.plus(1, ChronoUnit.DAYS);
		if(null != dateFrom || null != dateTo) {
			docMatchDate = new Document();
			if(null != dateFrom)
				docMatchDate.append("$gte", dateFrom);
			if(null != dateTo)
				docMatchDate.append("$lt", dateTo);
		}


		 docMatch = new Document("IssuerId", header.getIssuerId())
				 .append("IsActive", true)
					.append("IsDelete", new Document("$ne", true));
			if(!"".equals(mauSoHdon))
				docMatch.append("EInvoiceDetail.TTChung.MauSoHD", commons.regexEscapeForMongoQuery(mauSoHdon));
			if(!"".equals(soHoaDon))
				docMatch.append("EInvoiceDetail.TTChung.SHDon", commons.stringToInteger(soHoaDon));
			if(null != docMatchDate)
				docMatch.append("NLap", docMatchDate);
			if(!"".equals(ResponseStatus))
				docMatch.append("EInvoiceStatus", commons.regexEscapeForMongoQuery(ResponseStatus));			
			if(!"".equals(nbanMst))
				docMatch.append("EInvoiceDetail.NDHDon.NMua.MST", new Document("$regex", commons.regexEscapeForMongoQuery(nbanMst)).append("$options", "i"));
			if(!"".equals(nbanTen))
				docMatch.append("EInvoiceDetail.NDHDon.NMua.Ten", new Document("$regex", commons.regexEscapeForMongoQuery(nbanTen)).append("$options", "i"));
			
			pipeline = new ArrayList<Document>();
			pipeline.add(new Document("$match", docMatch));
			pipeline.add(
				new Document("$addFields", 
					new Document("SHDon", 
						new Document("$ifNull", Arrays.asList("$EInvoiceDetail.TTChung.SHDon", Integer.MAX_VALUE))
					)
				)
			);
			pipeline.add(
				new Document("$sort", 
					new Document("EInvoiceDetail.TTChung.MauSoHD", -1).append("SHDon", -1).append("_id", -1)
				)
			);
			pipeline.addAll(createFacetForSearchNotSort(page));
	
		MongoClient mongoClient = cfg.mongoClient();
		MongoCollection<Document> collection = mongoClient.getDatabase(cfg.dbName).getCollection("EInvoiceMTTSendCQT");
		      try {
		        docTmp = collection.aggregate(pipeline).allowDiskUse(true).iterator().next();    
		      } catch (Exception e) {
		        
		      }
		        
		mongoClient.close();
		
		if(null == docTmp) {
			responseStatus = new MspResponseStatus(9999, Constants.MAP_ERROR.get(9999));
			rsp.setResponseStatus(responseStatus);
			return rsp;
		}
		rsp = new MsgRsp(header);
		
		page.setTotalRows(docTmp.getInteger("total", 0));
		rsp.setMsgPage(page);
		
		
		responseStatus = null;
		List<Document> rows = null;
		if(docTmp != null) {
		if(docTmp.get("data") != null && docTmp.get("data") instanceof List) {
			rows = docTmp.getList("data", Document.class);
		}
	}
	
		
		
		if(null != rows) {
			for(Document doc: rows) {
				objectId = (ObjectId) doc.get("_id");	
				
				hItem = new HashMap<String, Object>();
				hItem.put("_id", objectId.toString());
				hItem.put("EInvoiceStatus", doc.get("EInvoiceStatus"));
//				hItem.put("SignStatusCode", doc.get("SignStatusCode"));
				hItem.put("KHHDon", doc.get("KHHDon"));
				hItem.put("KHMSHDon", doc.get("KHMSHDon"));
				hItem.put("MTDiep", doc.get("MTDiep"));
				hItem.put("SoLuong", doc.get("SoLuong"));
				hItem.put("InfoCreated", doc.get("InfoCreated"));				;
				hItem.put("NLap", doc.get("NLap"));
				hItem.put("LDo", doc.get("LDo"));
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

		Document docFind = new Document("IssuerId", header.getIssuerId()).append("IsDelete", new Document("$ne", true))
				.append("_id", objectId);

		Document docTmp = null;

		MongoClient mongoClient = cfg.mongoClient();
		MongoCollection<Document> collection = mongoClient.getDatabase(cfg.dbName).getCollection("EInvoiceMTTSendCQT");
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
		
		
		ObjectId objectId_ = (ObjectId) docTmp.get("_id");
		HashMap<String, Object> hItem = new HashMap<String, Object>();
		hItem.put("_id", objectId_.toString());
		Document docTmp1 = null;
		
		ArrayList<HashMap<String, Object>> rowsReturn = new ArrayList<HashMap<String, Object>>();
		HashMap<String, Object> hItem1 = null;
		for(Document oo: docTmp.getList("EinvoiceMTT", Document.class)) {
			hItem1 = new HashMap<String, Object>();
			String _idEInvoiceMTT = oo.get("_id", "");
			ObjectId idEInvoiceMTT = new ObjectId(_idEInvoiceMTT);
			
			Document findEInvoiceMTT = new Document("_id",idEInvoiceMTT)
					.append("IsDelete", new Document("$ne", true));
		
			mongoClient = cfg.mongoClient();
			collection = mongoClient.getDatabase(cfg.dbName).getCollection("EInvoiceMTT");
			      try {
			    	  docTmp1 = collection.find(findEInvoiceMTT).allowDiskUse(true).iterator().next();    
			      } catch (Exception e) {
			        
			      }
			        
			mongoClient.close();
	
			String KHHDon = docTmp1.getEmbedded(Arrays.asList("EInvoiceDetail","TTChung", "KHHDon"), "");
			String KHMSHDon = docTmp1.getEmbedded(Arrays.asList("EInvoiceDetail","TTChung", "KHMSHDon"), "");
			int SHDon = docTmp1.getEmbedded(Arrays.asList("EInvoiceDetail","TTChung", "SHDon"), 0);
			Date NLap = docTmp1.getEmbedded(Arrays.asList("EInvoiceDetail","TTChung", "NLap"), Date.class);
			String MaCQT = docTmp1.get("MCCQT", "");
			Double TTTTSo = docTmp1.getEmbedded(Arrays.asList("EInvoiceDetail","TToan", "TgTTTBSo"), Double.class);
			String EInvoiceStatus = docTmp1.get("EInvoiceStatus", "");
			
			hItem1.put("KHHDon", KHHDon);
			hItem1.put("KHMSHDon", KHMSHDon);
			hItem1.put("SHDon", SHDon);
			hItem1.put("NLap", NLap);
			hItem1.put("MaCQT", MaCQT);
			hItem1.put("TTTTSo", TTTTSo);
			hItem1.put("EInvoiceStatus", EInvoiceStatus);

			rowsReturn.add(hItem1);
			
			
		}
		
		HashMap<String, Object> mapDataR = new HashMap<String, Object>();
		mapDataR.put("rows", rowsReturn);
		rsp.setObjData(mapDataR);
		responseStatus = new MspResponseStatus(0, Constants.MAP_ERROR.get(0));
		rsp.setResponseStatus(responseStatus);
		return rsp;
	}

	public MsgRsp refreshStatusCQT(JSONRoot jsonRoot) throws Exception {
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

		String _id = commons.getTextJsonNode(jsonData.at("/_id")).replaceAll("\\s", "");

		MsgRsp rsp = new MsgRsp(header);
		rsp.setMsgPage(page);
		MspResponseStatus responseStatus = null;

		ObjectId objectId = null;
		try {
			objectId = new ObjectId(_id);
		} catch (Exception e) {
		}

		FindOneAndUpdateOptions options = null;
		/* KIEM TRA XEM THONG TIN TKHAI CO TON TAI KHONG */
		Document docFind = new Document("IssuerId", header.getIssuerId()).append("_id", objectId)
				.append("IsDelete", new Document("$ne",
						true))/* .append("SignStatusCode", "SIGNED") */
				.append("EInvoiceStatus", new Document("$in", Arrays.asList("ERROR_CQT", "PROCESSING")));
				
		Document docTmp = null;
		
		MongoClient mongoClient = cfg.mongoClient();
		MongoCollection<Document> collection = mongoClient.getDatabase(cfg.dbName).getCollection("EInvoiceMTTSendCQT");
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

		String MTDiep = docTmp.get("MTDiep", "");
		org.w3c.dom.Document rTCTN = null;
//		String MaKetQua = "";
//		String MoTaKetQua = "";
		
		try {
			rTCTN = tctnService.callTraCuuThongDiep(MTDiep);
		} catch (Exception e) {
		}
		
		try {
			if (rTCTN == null) {
				rTCTN = tctnService.callTraCuuThongDiep(MTDiep);
			}
		} catch (Exception e) {
		}
	
	


		/* DO DU LIEU TRA VE - CAP NHAT LAI KET QUA */
		XPath xPath = null;
		xPath = XPathFactory.newInstance().newXPath();
		Node nodeKetQuaTraCuu  = null;
		nodeKetQuaTraCuu = (Node) xPath.evaluate("/KetQuaTraCuu", rTCTN, XPathConstants.NODE);
//		MaKetQua = commons.getTextFromNodeXML((Element) xPath.evaluate("MaKetQua", nodeKetQuaTraCuu, XPathConstants.NODE));
//		MoTaKetQua = commons.getTextFromNodeXML((Element) xPath.evaluate("MoTaKetQua", nodeKetQuaTraCuu, XPathConstants.NODE));
//		org.w3c.dom.Document rTCTN1 = null;
//		org.w3c.dom.Document doc = null;
//		String MST = "";
	
//		MST = docTmp.getEmbedded(Arrays.asList("EInvoiceDetail", "NDHDon", "NBan", "MST"), "");
//	
//		Path path = Paths.get(SystemParams.DIR_E_INVOICE_DATA, MST,
//				docTmp.getEmbedded(Arrays.asList("DMMauSoKyHieu", "_id"), ObjectId.class).toString());
//		String pathDir = path.toString();
//		File file = path.toFile();
//		if (!file.exists())
//			file.mkdirs();
//		
		Node nodeTDiep = null;
		String checkMLTDiep = "";
//		boolean check_ = false;
		String MLoi = "";
		String MTLoi = "";
		String MTDTChieu = "";
		String CQT_MLTDiep = "";
		
		String MLoi1 = "";
		String MTLoi1 = "";
		String CQT_MLTDiep1 = "";
		
//		int dem = 1;
		
		for(int i = 1; i<=20; i++) {
			
			if(xPath.evaluate("DuLieu/TDiep[" + i + "]", nodeKetQuaTraCuu, XPathConstants.NODE) == null) break;		
			nodeTDiep = (Node) xPath.evaluate("DuLieu/TDiep[" + i + "]", nodeKetQuaTraCuu, XPathConstants.NODE);
			checkMLTDiep = commons.getTextFromNodeXML((Element) xPath.evaluate("TTChung/MLTDiep", nodeTDiep, XPathConstants.NODE));
			if(checkMLTDiep.equals("202"))
				break;
			if(checkMLTDiep.equals("204")) {
//				check_ = true;
//				MLoi1 = commons.getTextFromNodeXML((Element) xPath.evaluate("DLieu/TBao/DLTBao/LHDMTTien/DSLDo/LDo/MLoi", nodeTDiep, XPathConstants.NODE));
//				MTLoi1 = commons.getTextFromNodeXML((Element) xPath.evaluate("DLieu/TBao/DLTBao/LHDMTTien/DSLDo/LDo/MTLoi", nodeTDiep, XPathConstants.NODE));
//				
//				CQT_MLTDiep1 = checkMLTDiep;
				break;
	
			}
//			dem++;
		}
		
		if(nodeTDiep == null) {
			responseStatus = new MspResponseStatus(9999, "Chưa có kết quả trả về.");
			rsp.setResponseStatus(responseStatus);
			return rsp;
		}

		CQT_MLTDiep = commons.getTextFromNodeXML((Element) xPath.evaluate("TTChung/MLTDiep", nodeTDiep, XPathConstants.NODE));
		MTDTChieu = commons.getTextFromNodeXML((Element) xPath.evaluate("TTChung/MTDTChieu", nodeTDiep, XPathConstants.NODE));
		String LTBao = commons.getTextFromNodeXML((Element) xPath.evaluate("DLieu/TBao/DLTBao/LTBao", nodeTDiep, XPathConstants.NODE));
	
//		if(!CQT_MLTDiep.equals("202")) {
//		if (check_== true) {
		
		if(!LTBao.equals("2")) {
			MLoi1 = commons.getTextFromNodeXML((Element) xPath.evaluate("DLieu/TBao/DLTBao/LHDMTTien/DSLDo/LDo/MLoi", nodeTDiep, XPathConstants.NODE));
			MTLoi1 = commons.getTextFromNodeXML((Element) xPath.evaluate("DLieu/TBao/DLTBao/LHDMTTien/DSLDo/LDo/MTLoi", nodeTDiep, XPathConstants.NODE));
			MLoi = MLoi1;
			MTLoi = MTLoi1;
			/* LUU LAI FILE XML LOI */
			String dir = docTmp.get("Dir", "");
			String fileName = _id + "_" + CQT_MLTDiep1 +  "ERROR.xml";
			boolean boo = false;
			try {
				boo = commons.docW3cToFile(rTCTN, dir, fileName);
			} catch (Exception e) {
			}
			if (!boo) {
				responseStatus = new MspResponseStatus(9999, "Lưu tập tin trả về từ CQT không thành công.");
				rsp.setResponseStatus(responseStatus);
				return rsp;
			}

			/* CAP NHAT TRANG THAI COMPLETE - TRANG THAI CQT */
			
			
			
			for(Document oo: docTmp.getList("EinvoiceMTT", Document.class)) {
				String _idEInvoiceMTT = oo.get("_id", "");
				ObjectId idEInvoiceMTT = new ObjectId(_idEInvoiceMTT);
				
				Document findEInvoiceMTT = new Document("_id",idEInvoiceMTT)
						.append("IsDelete", new Document("$ne", true));
				options = new FindOneAndUpdateOptions();
				options.upsert(false);
				options.maxTime(5000, TimeUnit.MILLISECONDS);
				options.returnDocument(ReturnDocument.AFTER);
				
				mongoClient = cfg.mongoClient();
				collection = mongoClient.getDatabase(cfg.dbName).getCollection("EInvoiceMTT");
				collection.findOneAndUpdate(findEInvoiceMTT,
						new Document("$set", new Document("EInvoiceStatus", Constants.INVOICE_STATUS.ERROR_CQT)
								.append("CQT_Date", LocalDate.now())
								.append("LDo", new Document("MLoi", MLoi)
										.append("MTLoi", MTLoi))),
						options);
				 mongoClient.close();
	
			}
			options = new FindOneAndUpdateOptions();
			options.upsert(false);
			options.maxTime(5000, TimeUnit.MILLISECONDS);
			options.returnDocument(ReturnDocument.AFTER);
			
			mongoClient = cfg.mongoClient();
			collection = mongoClient.getDatabase(cfg.dbName).getCollection("EInvoiceMTTSendCQT");
			collection.findOneAndUpdate(docFind,
					new Document("$set", new Document("EInvoiceStatus", Constants.INVOICE_STATUS.ERROR_CQT)
							.append("CQT_Date", LocalDate.now())
							.append("LDo", new Document("MLoi", MLoi)
									.append("MTLoi", MTLoi))),
					options);
			 mongoClient.close();

			responseStatus = new MspResponseStatus(0,
					"".equals(MTLoi) ? "CQT chưa có thông báo kết quả trả về." : MTLoi);
			rsp.setResponseStatus(responseStatus);
			return rsp;
			
		}
//		}
		
				
		
		if ("|204|".indexOf("|" + CQT_MLTDiep + "|") == -1) {
			responseStatus = new MspResponseStatus(9999, "CQT chưa có thông báo kết quả trả về.");
			rsp.setResponseStatus(responseStatus);
			return rsp;
		}

		String MGDDTu = commons
				.getTextFromNodeXML((Element) xPath.evaluate("DLieu/TBao/DLTBao/MGDDTu", nodeTDiep, XPathConstants.NODE));

		
		String dir = docTmp.get("Dir", "");
		String fileName = _id + "_" + MGDDTu + ".xml";
		boolean boo = false;
		try {
			boo = commons.docW3cToFile(rTCTN, dir, fileName);
		} catch (Exception e) {
		}
		if (!boo) {
			responseStatus = new MspResponseStatus(9999, "Lưu tập tin trả về từ CQT không thành công.");
			rsp.setResponseStatus(responseStatus);
			return rsp;
		}
		
		
		for(Document oo: docTmp.getList("EinvoiceMTT", Document.class)) {
			String _idEInvoiceMTT = oo.get("_id", "");
			ObjectId idEInvoiceMTT = new ObjectId(_idEInvoiceMTT);
			
			Document findEInvoiceMTT = new Document("_id",idEInvoiceMTT)
					.append("IsDelete", new Document("$ne", true));
			
			options = new FindOneAndUpdateOptions();
			options.upsert(false);
			options.maxTime(5000, TimeUnit.MILLISECONDS);
			options.returnDocument(ReturnDocument.AFTER);

			mongoClient = cfg.mongoClient();
			collection = mongoClient.getDatabase(cfg.dbName).getCollection("EInvoiceMTT");
			collection.findOneAndUpdate(findEInvoiceMTT,
					new Document("$set",
							new Document("EInvoiceStatus", Constants.INVOICE_STATUS.COMPLETE)
							.append("MGDDTu", MGDDTu)
//							.append("MCCQT", maCQT)
							.append("MTDTChieu", MTDTChieu)
							.append("CQT_Date", LocalDate.now())
							.append("LDo", 
									new Document("MLoi", "").append("MTLoi", "")
								)
						), 
						options);
			  mongoClient.close();

		}
		/* CAP NHAT TRANG THAI COMPLETE - TRANG THAI CQT */
		options = new FindOneAndUpdateOptions();
		options.upsert(false);
		options.maxTime(5000, TimeUnit.MILLISECONDS);
		options.returnDocument(ReturnDocument.AFTER);

		mongoClient = cfg.mongoClient();
		collection = mongoClient.getDatabase(cfg.dbName).getCollection("EInvoiceMTTSendCQT");
		collection.findOneAndUpdate(docFind,
				new Document("$set",
						new Document("EInvoiceStatus", Constants.INVOICE_STATUS.COMPLETE)
						.append("MGDDTu", MGDDTu)
//						.append("MCCQT", maCQT)
						.append("MTDTChieu", MTDTChieu)
						.append("CQT_Date", LocalDate.now())
						.append("LDo", 
								new Document("MLoi", "").append("MTLoi", "")
							)
					), 
					options);
		  mongoClient.close();

		String iddc = "";
		try {
		 iddc = docTmp.getEmbedded(Arrays.asList("EInvoiceDetail", "TTChung", "TTHDLQuan", "_id"),"");
		} catch (Exception e) {
			 iddc = docTmp.getEmbedded(Arrays.asList("EInvoiceDetail", "TTChung", "TTHDLQuan", "_id"), ObjectId.class).toString();
		}
	
		if (!iddc.equals("")) {
		ObjectId objectIddc = null;
		try {
			objectIddc = new ObjectId(iddc);
		} catch (Exception e) {
		}	
		
		Document docFind1 = new Document("IssuerId", header.getIssuerId())
				.append("IsDelete", new Document("$ne", true)).append("_id", objectIddc)
				/* .append("SignStatusCode", Constants.INVOICE_SIGN_STATUS.SIGNED) */
				.append("EInvoiceStatus", new Document("$in", Arrays.asList(Constants.INVOICE_STATUS.COMPLETE,
						Constants.INVOICE_STATUS.ADJUSTED, Constants.INVOICE_STATUS.REPLACED)));
		options = new FindOneAndUpdateOptions();
		options.upsert(true);
		options.maxTime(5000, TimeUnit.MILLISECONDS);
		options.returnDocument(ReturnDocument.AFTER);

				if ("1".equals(docTmp.getEmbedded(Arrays.asList("EInvoiceDetail", "TTChung", "TTHDLQuan", "TCHDon"), ""))) {
					
				  	mongoClient = cfg.mongoClient();
					collection = mongoClient.getDatabase(cfg.dbName).getCollection("EInvoiceMTT");
					collection.findOneAndUpdate(docFind1,
							new Document("$set", 
									new Document("EInvoiceStatus", "REPLACED")					
								), 
								options
							);
					  mongoClient.close();
					  
				} else if ("2".equals(docTmp.getEmbedded(Arrays.asList("EInvoiceDetail", "TTChung", "TTHDLQuan", "TCHDon"), ""))) {
					
					MongoClient	mongoClient1 = cfg.mongoClient();
					collection = mongoClient1.getDatabase(cfg.dbName).getCollection("EInvoiceMTT");
					collection.findOneAndUpdate(docFind1,
							new Document("$set", 
									new Document("EInvoiceStatus", "ADJUSTED")					
								), 
								options
							);
					mongoClient1.close();
				}
		}

		responseStatus = new MspResponseStatus(0, Constants.MAP_ERROR.get(0));
		rsp.setResponseStatus(responseStatus);
		return rsp;
	}

	
}
