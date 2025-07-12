package vn.sesgroup.hddt.user.impl;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import org.apache.commons.io.FileUtils;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

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
import vn.sesgroup.hddt.dto.FileInfo;
import vn.sesgroup.hddt.user.dao.AbstractDAO;
import vn.sesgroup.hddt.user.dao.TKCTuDAO;
import vn.sesgroup.hddt.user.service.TCTNService;
import vn.sesgroup.hddt.utility.Constants;
import vn.sesgroup.hddt.utility.Json;
import vn.sesgroup.hddt.utility.SystemParams;

@Repository
@Transactional
public class TKCTuImpl extends AbstractDAO implements TKCTuDAO{
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
		if(objData != null) {
			jsonData = Json.serializer().nodeFromObject(msg.getObjData());
		}else{
			throw new Exception("Lỗi dữ liệu đầu vào");
		}
		
		MsgRsp rsp = new MsgRsp(header);
		rsp.setMsgPage(page);
		MspResponseStatus responseStatus = null;
		
		
		String actionCode = header.getActionCode();
		String _id = commons.getTextJsonNode(jsonData.at("/_id")).replaceAll("\\s", "");
		String mst = commons.getTextJsonNode(jsonData.at("/Mst")).replaceAll("\\s", "");
		String tennnt = commons.getTextJsonNode(jsonData.at("/TenNNT")).trim().replaceAll("\\s+", " ");
		String nlhe = commons.getTextJsonNode(jsonData.at("/NLHe")).trim().replaceAll("\\s+", " ");
		String dclhe = commons.getTextJsonNode(jsonData.at("/DCLHe")).trim().replaceAll("\\s+", " ");
		String dctdtu = commons.getTextJsonNode(jsonData.at("/DCTDTu")).trim().replaceAll("\\s+", " ");
		String dtlhe = commons.getTextJsonNode(jsonData.at("/DTLHe")).trim().replaceAll("\\s+", " ");
		
		String mauSo = commons.getTextJsonNode(jsonData.at("/MauSo")).replaceAll("\\s", "");
		String ten = commons.getTextJsonNode(jsonData.at("/Ten")).trim().replaceAll("\\s+", " ");
		String tinhThanh = commons.getTextJsonNode(jsonData.at("/TinhThanh")).replaceAll("\\s", "");
		String cqtqly = commons.getTextJsonNode(jsonData.at("/CQTQLy")).replaceAll("\\s", "");
		String nlap = commons.getTextJsonNode(jsonData.at("/NLap")).trim().replaceAll("\\s+", " ");
		String hthuc = commons.getTextJsonNode(jsonData.at("/HThuc")).trim().replaceAll("\\s+", " ");

		String tccnnphanh = commons.getTextJsonNode(jsonData.at("/TCCNPHanh")).trim().replaceAll("\\s+", " ");
		String cqtphanh = commons.getTextJsonNode(jsonData.at("/CQTPHanh")).trim().replaceAll("\\s+", " ");
		
		String pthuc = commons.getTextJsonNode(jsonData.at("/PThuc")).trim().replaceAll("\\s+", " ");

		String lctsdung_TNCNhan = commons.getTextJsonNode(jsonData.at("/LCTSDung_TNCNhan")).trim().replaceAll("\\s+", " ");
		String lctsdung_TMDTu = commons.getTextJsonNode(jsonData.at("/LCTSDung_TMDTu")).trim().replaceAll("\\s+", " ");
		String lctsdung_BLKIMGia = commons.getTextJsonNode(jsonData.at("/LCTSDung_BLKIMGia")).trim().replaceAll("\\s+", " ");
		String lctsdung_BLIMGia = commons.getTextJsonNode(jsonData.at("/LCTSDung_BLIMGia")).trim().replaceAll("\\s+", " ");
		String lctsdung_BLCTT50 = commons.getTextJsonNode(jsonData.at("/LCTSDung_BLCTT50")).trim().replaceAll("\\s+", " ");
		List<Object> rowDSCTSSDung = new ArrayList<Object>();
		
		
		List<Document> pipeline = null;
		Document docFind = null;
		Document docTmp = null;
		FindOneAndUpdateOptions options = null;
		
		ObjectId objectId = null;
		ObjectId objectIdUser = null;
		ObjectId objectIdTK = null;

		DocumentBuilderFactory dbf = null;
		DocumentBuilder db = null;
		org.w3c.dom.Document doc = null;
		Element root = null;
		
		Element elementContent = null;
		Element elementSubContent = null;
		Element elementTmp = null;
		Element elementSubTmp = null;
		HashMap<String, Object> hO = null;

		String fileNameXML = "";
		String pathDir = "";
		Path path = null;
		File file = null;
		
		String taxCode = "";
		boolean isSaveFile = false;
		
		MongoCollection<Document> collection = null;
		
		switch (actionCode) {
		case Constants.MSG_ACTION_CODE.CREATED:
		case Constants.MSG_ACTION_CODE.COPY:
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

			pipeline = new ArrayList<Document>();
			Date currentDate = new Date();
			docFind = new Document("IssuerId", header.getIssuerId()).append("IsDelete", new Document("$ne", true))
					.append("DSCTSSDung.DNgay", new Document("$gte", currentDate));
			pipeline.add(new Document("$match", docFind));
			docTmp = null;
			try (MongoClient mongoClient = cfg.mongoClient()) {
				collection = mongoClient.getDatabase(cfg.dbName).getCollection("DMCTSo");
				docTmp = collection.aggregate(pipeline).allowDiskUse(true).iterator().next();
			} catch (Exception e) {

			}

			if (null == docTmp) {
				responseStatus = new MspResponseStatus(9999,
						"Quý khách vui lòng vào menu Hệ Thống/Chứng thư số để thêm chữ ký số vào hệ thống trước khi Đăng ký tờ khai chứng từ.");
				rsp.setResponseStatus(responseStatus);
				return rsp;
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
											commons.regexEscapeForMongoQuery(cqtqly))),
							new Document("$project", new Document("_id", 0).append("code", 1).append("name", 1).append("note", 1))))
					.append("as", "DMChiCucThueInfo")));
			pipeline.add(new Document("$unwind",
					new Document("path", "$DMChiCucThueInfo").append("preserveNullAndEmptyArrays", true)));
			docTmp = null;
			try (MongoClient mongoClient = cfg.mongoClient()) {
				collection = mongoClient.getDatabase(cfg.dbName).getCollection("Issuer");
				docTmp = collection.aggregate(pipeline).allowDiskUse(true).iterator().next();
			} catch (Exception e) {

			}

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

			objectIdTK = new ObjectId();
			path = Paths.get(SystemParams.DIR_E_INVOICE_TKHAI, taxCode, "chungtu");
			pathDir = path.toString();
			file = path.toFile();
			if (!file.exists())
				file.mkdirs();

			/* TAO FILE XML */
			fileNameXML = objectIdTK.toString() + ".xml";

			dbf = DocumentBuilderFactory.newInstance();
			db = dbf.newDocumentBuilder();
			doc = db.newDocument();
			doc.setXmlStandalone(true);

			root = doc.createElement("TKhai");
			doc.appendChild(root);

			elementContent = doc.createElement("DLTKhai");
			elementContent.setAttribute("Id", "data");
			root.appendChild(elementContent);

			/* THONG TIN CHUNG TO KHAI */
			elementSubContent = doc.createElement("TTChung");
			elementSubContent.appendChild(commons.createElementWithValue(doc, "PBan", SystemParams.VERSION_XML_TOKHAICHUNGTU));
			elementSubContent.appendChild(commons.createElementWithValue(doc, "MSo", mauSo));
			elementSubContent.appendChild(commons.createElementWithValue(doc, "Ten", ten));
			elementSubContent.appendChild(commons.createElementWithValue(doc, "HThuc", hthuc));
			elementSubContent.appendChild(commons.createElementWithValue(doc, "TNNT", tennnt));
			elementSubContent.appendChild(commons.createElementWithValue(doc, "MST", mst));
			elementSubContent.appendChild(commons.createElementWithValue(doc, "CQTQLy",
					docTmp.getEmbedded(Arrays.asList("DMChiCucThueInfo", "name"), "")));
			elementSubContent.appendChild(commons.createElementWithValue(doc, "MCQTQLy", cqtqly));
			elementSubContent.appendChild(commons.createElementWithValue(doc, "NLHe", nlhe));
			elementSubContent.appendChild(commons.createElementWithValue(doc, "DCLHe", dclhe));
			elementSubContent.appendChild(commons.createElementWithValue(doc, "DCTDTu", dctdtu));
			elementSubContent.appendChild(commons.createElementWithValue(doc, "DTLHe", dtlhe));
			elementSubContent.appendChild(commons.createElementWithValue(doc, "DDanh",
					docTmp.getEmbedded(Arrays.asList("DMTinhThanhInfo", "name"), "")));
			elementSubContent.appendChild(
					commons.createElementWithValue(doc, "NLap", commons.convertLocalDateTimeStringToString(nlap,
							Constants.FORMAT_DATE.FORMAT_DATE_WEB, Constants.FORMAT_DATE.FORMAT_DATE_EINVOICE)));
			elementContent.appendChild(elementSubContent);

			elementSubContent = doc.createElement("NDTKhai");

			elementTmp = doc.createElement("DTPHanh");
			elementTmp.appendChild(
					commons.createElementWithValue(doc, "TCCNPHanh", tccnnphanh.trim().equals("on") ? "1" : "0"));
			elementTmp.appendChild(
					commons.createElementWithValue(doc, "CQTPHanh", cqtphanh.trim().equals("on") ? "1" : "0"));
			elementSubContent.appendChild(elementTmp);

			elementTmp = doc.createElement("LHSDung");
			elementTmp.appendChild(
					commons.createElementWithValue(doc, "CTTNCNhan", lctsdung_TNCNhan.trim().equals("on") ? "1" : "0"));
			elementTmp.appendChild(
					commons.createElementWithValue(doc, "CTKTTTMDTu", lctsdung_TMDTu.trim().equals("on") ? "1" : "0"));
			elementTmp.appendChild(commons.createElementWithValue(doc, "BLTPLPKIn",
					lctsdung_BLKIMGia.trim().equals("on") ? "1" : "0"));
			elementTmp.appendChild(
					commons.createElementWithValue(doc, "BLTPLPIn", lctsdung_BLIMGia.trim().equals("on") ? "1" : "0"));
			elementTmp.appendChild(
					commons.createElementWithValue(doc, "BLTTPLPhi", lctsdung_BLCTT50.trim().equals("on") ? "1" : "0"));
			elementSubContent.appendChild(elementTmp);

			elementTmp = doc.createElement("HTGDLCTDT");
			elementTmp.appendChild(
					commons.createElementWithValue(doc, "CDLQCCQT", pthuc.trim().equals("HTGDLieu_CTTDT") ? "1" : "0"));
			elementTmp.appendChild(
					commons.createElementWithValue(doc, "CDLQTCTN", pthuc.trim().equals("HTGDLieu_TNCN") ? "1" : "0"));
			elementTmp.appendChild(commons.createElementWithValue(doc, "CDLQTCTNUT",
					pthuc.trim().equals("HTGDLieu_TNCNYT") ? "1" : "0"));
			elementSubContent.appendChild(elementTmp);

			elementTmp = doc.createElement("DSCTSSDung");
			if (!jsonData.at("/DSCTSSDung").isMissingNode()) {
				for (JsonNode o : jsonData.at("/DSCTSSDung")) {
					String tn = commons.getTextJsonNode(o.at("/TNgay"));
					String[] words2 = tn.split(" ");
					String[] words = words2[1].split(":");
					String h = words[0];
					if (h.length() < 2) {
						h = "0" + h;
					}
					String m = words[1];
					if (m.length() < 2) {
						m = "0" + m;
					}
					String s = words[2];
					if (s.length() < 2) {
						s = "0" + s;
					}
					tn = words2[0] + " " + h + ":" + m + ":" + s;
					String dn = commons.getTextJsonNode(o.at("/DNgay"));
					String[] words21 = dn.split(" ");
					String[] words1 = words21[1].split(":");
					String h1 = words1[0];
					if (h1.length() < 2) {
						h1 = "0" + h1;
					}
					String m1 = words1[1];
					if (m1.length() < 2) {
						m1 = "0" + m1;
					}
					String s1 = words1[2];
					if (s1.length() < 2) {
						s1 = "0" + s1;
					}
					dn = words21[0] + " " + h1 + ":" + m1 + ":" + s1;

					DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
					LocalDateTime dateTime = LocalDateTime.parse(tn, formatter);
					DateTimeFormatter formatter1 = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
					LocalDateTime dateTime1 = LocalDateTime.parse(dn, formatter1);
					elementSubTmp = doc.createElement("CTS");
					elementSubTmp.appendChild(
							commons.createElementWithValue(doc, "TTChuc", commons.getTextJsonNode(o.at("/TTChuc"))));
					elementSubTmp.appendChild(
							commons.createElementWithValue(doc, "Seri", commons.getTextJsonNode(o.at("/Seri"))));
					elementSubTmp.appendChild(commons.createElementWithValue(doc, "TNgay", commons
							.convertLocalDateTimeToString(dateTime, Constants.FORMAT_DATE.FORMAT_DATETIME_EINVOICE)));
					elementSubTmp.appendChild(commons.createElementWithValue(doc, "DNgay", commons
							.convertLocalDateTimeToString(dateTime1, Constants.FORMAT_DATE.FORMAT_DATETIME_EINVOICE)));
					elementSubTmp.appendChild(
							commons.createElementWithValue(doc, "HThuc", commons.getTextJsonNode(o.at("/HThuc"))));
					elementTmp.appendChild(elementSubTmp);

					hO = new LinkedHashMap<String, Object>();
					hO.put("TTChuc", commons.getTextJsonNode(o.at("/TTChuc")));
					hO.put("Seri", commons.getTextJsonNode(o.at("/Seri")));
					hO.put("TNgay", dateTime);
					hO.put("DNgay", dateTime1);
					hO.put("HThuc", commons.getTextJsonNode(o.at("/HThuc")));
					rowDSCTSSDung.add(hO);

				}
			}
			elementSubContent.appendChild(elementTmp);
			elementContent.appendChild(elementSubContent);

			isSaveFile = commons.docW3cToFile(doc, pathDir, fileNameXML);
			if (!isSaveFile) {
				throw new Exception("Lưu dữ liệu không thành công.");
			}

			String MTDiep = SystemParams.MSTTCGP
					+ commons.csRandomAlphaNumbericString(46 - SystemParams.MSTTCGP.length()).toUpperCase();
			/* LUU DU LIEU */
			Document docInsert = new Document("_id", objectIdTK).append("IssuerId", header.getIssuerId())
					.append("MTDiep", MTDiep).append("MSo", mauSo).append("Ten", ten).append("HThuc", hthuc)
					.append("TNNT", tennnt).append("MST", mst).append("TinhThanhInfo", docTmp.get("DMTinhThanhInfo"))
					.append("ChiCucThueInfo", docTmp.get("DMChiCucThueInfo")).append("NLHe", nlhe)
					.append("DCLHe", dclhe).append("DCTDTu", dctdtu).append("DTLHe", dtlhe)
					.append("NLap", commons.convertStringToLocalDate(nlap, Constants.FORMAT_DATE.FORMAT_DATE_WEB))

					.append("PThuc", pthuc)
					.append("DTPHanh",
							new Document("TCCNPHanh", "on".equals(tccnnphanh) ? "1" : "0").append("CQTPHanh",
									"on".equals(cqtphanh) ? "1" : "0"))
					.append("LHSDung",
							new Document("CTTNCNhan", "on".equals(lctsdung_TNCNhan) ? "1" : "0")
									.append("CTKTTTMDTu", "on".equals(lctsdung_TMDTu) ? "1" : "0")
									.append("BLTPLPKIn", "on".equals(lctsdung_BLKIMGia) ? "1" : "0")
									.append("BLTPLPIn", "on".equals(lctsdung_BLIMGia) ? "1" : "0")
									.append("BLTTPLPhi", "on".equals(lctsdung_BLCTT50) ? "1" : "0"))
					.append("DSCTSSDung", rowDSCTSSDung).append("Status", Constants.INVOICE_STATUS.TK_CREATED)
					.append("IsDelete", false).append("Dir", pathDir).append("FileNameXML", fileNameXML)
					.append("InfoCreated",
							new Document("CreateDate", LocalDateTime.now()).append("CreateUserID", header.getUserId())
									.append("CreateUserName", header.getUserName())
									.append("CreateUserFullName", header.getUserFullName()));
			/* END - LUU DU LIEU */
			try (MongoClient mongoClient = cfg.mongoClient()) {
				collection = mongoClient.getDatabase(cfg.dbName).getCollection("DMTKCTu");
				collection.insertOne(docInsert);
			} catch (Exception e) {

			}
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
											commons.regexEscapeForMongoQuery(cqtqly))),
							new Document("$project", new Document("_id", 0).append("code", 1).append("name", 1).append("note", 1))))
					.append("as", "DMChiCucThueInfo")));
			pipeline.add(new Document("$unwind",
					new Document("path", "$DMChiCucThueInfo").append("preserveNullAndEmptyArrays", true)));

			docFind = new Document("IssuerId", header.getIssuerId()).append("IsDelete", new Document("$ne", true))
					.append("Status", Constants.INVOICE_STATUS.CREATED).append("_id", objectIdTK);
			pipeline.add(new Document("$lookup", new Document("from", "DMTKCTu")
					.append("pipeline", Arrays.asList(new Document("$match", docFind))).append("as", "DMTKCTu")));

			pipeline.add(new Document("$unwind",
					new Document("path", "$DMTKCTu").append("preserveNullAndEmptyArrays", true)));

			try (MongoClient mongoClient = cfg.mongoClient()) {
				collection = mongoClient.getDatabase(cfg.dbName).getCollection("Issuer");
				docTmp = collection.aggregate(pipeline).allowDiskUse(true).iterator().next();
			} catch (Exception e) {

			}

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
			if (docTmp.get("DMTKCTu") == null) {
				responseStatus = new MspResponseStatus(9999, "Vui lòng kiểm tra lại thông tin tờ khai chứng từ.");
				rsp.setResponseStatus(responseStatus);
				return rsp;
			}

			taxCode = docTmp.getString("TaxCode");

			path = Paths.get(SystemParams.DIR_E_INVOICE_TKHAI, taxCode, "chungtu");
			pathDir = path.toString();
			file = path.toFile();
			if (!file.exists())
				file.mkdirs();

			/* TAO FILE XML */
			fileNameXML = docTmp.getEmbedded(Arrays.asList("DMTKCTu", "FileNameXML"), "");

			dbf = DocumentBuilderFactory.newInstance();
			db = dbf.newDocumentBuilder();
			doc = db.newDocument();
			doc.setXmlStandalone(true);

			root = doc.createElement("TKhai");
			doc.appendChild(root);

			elementContent = doc.createElement("DLTKhai");
			elementContent.setAttribute("Id", "data");
			root.appendChild(elementContent);

			/* THONG TIN CHUNG TO KHAI */
			elementSubContent = doc.createElement("TTChung");
			elementSubContent.appendChild(commons.createElementWithValue(doc, "PBan", SystemParams.VERSION_XML_TOKHAICHUNGTU));
			elementSubContent.appendChild(commons.createElementWithValue(doc, "MSo", mauSo));
			elementSubContent.appendChild(commons.createElementWithValue(doc, "Ten", ten));
			elementSubContent.appendChild(commons.createElementWithValue(doc, "HThuc", hthuc));
			elementSubContent.appendChild(commons.createElementWithValue(doc, "TNNT", tennnt));
			elementSubContent.appendChild(commons.createElementWithValue(doc, "MST", mst));
			elementSubContent.appendChild(commons.createElementWithValue(doc, "CQTQLy",
					docTmp.getEmbedded(Arrays.asList("DMChiCucThueInfo", "name"), "")));
			elementSubContent.appendChild(commons.createElementWithValue(doc, "MCQTQLy", cqtqly));
			elementSubContent.appendChild(commons.createElementWithValue(doc, "NLHe", nlhe));
			elementSubContent.appendChild(commons.createElementWithValue(doc, "DCLHe", dclhe));
			elementSubContent.appendChild(commons.createElementWithValue(doc, "DCTDTu", dctdtu));
			elementSubContent.appendChild(commons.createElementWithValue(doc, "DTLHe", dtlhe));
			elementSubContent.appendChild(commons.createElementWithValue(doc, "DDanh",
					docTmp.getEmbedded(Arrays.asList("DMTinhThanhInfo", "name"), "")));
			elementSubContent.appendChild(
					commons.createElementWithValue(doc, "NLap", commons.convertLocalDateTimeStringToString(nlap,
							Constants.FORMAT_DATE.FORMAT_DATE_WEB, Constants.FORMAT_DATE.FORMAT_DATE_EINVOICE)));
			elementContent.appendChild(elementSubContent);

			elementSubContent = doc.createElement("NDTKhai");

			elementTmp = doc.createElement("DTPHanh");
			elementTmp.appendChild(
					commons.createElementWithValue(doc, "TCCNPHanh", tccnnphanh.trim().equals("on") ? "1" : "0"));
			elementTmp.appendChild(
					commons.createElementWithValue(doc, "CQTPHanh", cqtphanh.trim().equals("on") ? "1" : "0"));
			elementSubContent.appendChild(elementTmp);

			elementTmp = doc.createElement("LHSDung");
			elementTmp.appendChild(
					commons.createElementWithValue(doc, "CTTNCNhan", lctsdung_TNCNhan.trim().equals("on") ? "1" : "0"));
			elementTmp.appendChild(
					commons.createElementWithValue(doc, "CTKTTTMDTu", lctsdung_TMDTu.trim().equals("on") ? "1" : "0"));
			elementTmp.appendChild(commons.createElementWithValue(doc, "BLTPLPKIn",
					lctsdung_BLKIMGia.trim().equals("on") ? "1" : "0"));
			elementTmp.appendChild(
					commons.createElementWithValue(doc, "BLTPLPIn", lctsdung_BLIMGia.trim().equals("on") ? "1" : "0"));
			elementTmp.appendChild(
					commons.createElementWithValue(doc, "BLTTPLPhi", lctsdung_BLCTT50.trim().equals("on") ? "1" : "0"));
			elementSubContent.appendChild(elementTmp);

			elementTmp = doc.createElement("HTGDLCTDT");
			elementTmp.appendChild(
					commons.createElementWithValue(doc, "CDLQCCQT", pthuc.trim().equals("HTGDLieu_CTTDT") ? "1" : "0"));
			elementTmp.appendChild(
					commons.createElementWithValue(doc, "CDLQTCTN", pthuc.trim().equals("HTGDLieu_TNCN") ? "1" : "0"));
			elementTmp.appendChild(commons.createElementWithValue(doc, "CDLQTCTNUT",
					pthuc.trim().equals("HTGDLieu_TNCNYT") ? "1" : "0"));
			elementSubContent.appendChild(elementTmp);

			elementTmp = doc.createElement("DSCTSSDung");
			if (!jsonData.at("/DSCTSSDung").isMissingNode()) {
				for (JsonNode o : jsonData.at("/DSCTSSDung")) {
					String tn = commons.getTextJsonNode(o.at("/TNgay"));
					String[] words2 = tn.split(" ");
					String[] words = words2[1].split(":");
					String h = words[0];
					if (h.length() < 2) {
						h = "0" + h;
					}
					String m = words[1];
					if (m.length() < 2) {
						m = "0" + m;
					}
					String s = words[2];
					if (s.length() < 2) {
						s = "0" + s;
					}
					tn = words2[0] + " " + h + ":" + m + ":" + s;
					String dn = commons.getTextJsonNode(o.at("/DNgay"));
					String[] words21 = dn.split(" ");
					String[] words1 = words21[1].split(":");
					String h1 = words1[0];
					if (h1.length() < 2) {
						h1 = "0" + h1;
					}
					String m1 = words1[1];
					if (m1.length() < 2) {
						m1 = "0" + m1;
					}
					String s1 = words1[2];
					if (s1.length() < 2) {
						s1 = "0" + s1;
					}
					dn = words21[0] + " " + h1 + ":" + m1 + ":" + s1;

					DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
					LocalDateTime dateTime = LocalDateTime.parse(tn, formatter);
					DateTimeFormatter formatter1 = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
					LocalDateTime dateTime1 = LocalDateTime.parse(dn, formatter1);
					elementSubTmp = doc.createElement("CTS");
					elementSubTmp.appendChild(
							commons.createElementWithValue(doc, "TTChuc", commons.getTextJsonNode(o.at("/TTChuc"))));
					elementSubTmp.appendChild(
							commons.createElementWithValue(doc, "Seri", commons.getTextJsonNode(o.at("/Seri"))));
					elementSubTmp.appendChild(commons.createElementWithValue(doc, "TNgay", commons
							.convertLocalDateTimeToString(dateTime, Constants.FORMAT_DATE.FORMAT_DATETIME_EINVOICE)));
					elementSubTmp.appendChild(commons.createElementWithValue(doc, "DNgay", commons
							.convertLocalDateTimeToString(dateTime1, Constants.FORMAT_DATE.FORMAT_DATETIME_EINVOICE)));
					elementSubTmp.appendChild(
							commons.createElementWithValue(doc, "HThuc", commons.getTextJsonNode(o.at("/HThuc"))));
					elementTmp.appendChild(elementSubTmp);

					hO = new LinkedHashMap<String, Object>();
					hO.put("TTChuc", commons.getTextJsonNode(o.at("/TTChuc")));
					hO.put("Seri", commons.getTextJsonNode(o.at("/Seri")));
					hO.put("TNgay", dateTime);
					hO.put("DNgay", dateTime1);
					hO.put("HThuc", commons.getTextJsonNode(o.at("/HThuc")));
					rowDSCTSSDung.add(hO);

				}
			}
			elementSubContent.appendChild(elementTmp);
			elementContent.appendChild(elementSubContent);

			isSaveFile = commons.docW3cToFile(doc, pathDir, fileNameXML);
			if (!isSaveFile) {
				throw new Exception("Lưu dữ liệu không thành công.");
			}

			options = new FindOneAndUpdateOptions();
			options.upsert(false);
			options.maxTime(5000, TimeUnit.MILLISECONDS);
			options.returnDocument(ReturnDocument.AFTER);

			Document docUpdate = new Document().append("IssuerId", header.getIssuerId()).append("MSo", mauSo)
					.append("Ten", ten).append("HThuc", hthuc).append("TNNT", tennnt).append("MST", mst)
					.append("TinhThanhInfo", docTmp.get("DMTinhThanhInfo"))
					.append("ChiCucThueInfo", docTmp.get("DMChiCucThueInfo")).append("NLHe", nlhe)
					.append("DCLHe", dclhe).append("DCTDTu", dctdtu).append("DTLHe", dtlhe)
					.append("NLap", commons.convertStringToLocalDate(nlap, Constants.FORMAT_DATE.FORMAT_DATE_WEB))
					.append("PThuc", pthuc)
					.append("DTPHanh",
							new Document("TCCNPHanh", "on".equals(tccnnphanh) ? "1" : "0").append("CQTPHanh",
									"on".equals(cqtphanh) ? "1" : "0"))
					.append("LHSDung",
							new Document("CTTNCNhan", "on".equals(lctsdung_TNCNhan) ? "1" : "0")
									.append("CTKTTTMDTu", "on".equals(lctsdung_TMDTu) ? "1" : "0")
									.append("BLTPLPKIn", "on".equals(lctsdung_BLKIMGia) ? "1" : "0")
									.append("BLTPLPIn", "on".equals(lctsdung_BLIMGia) ? "1" : "0")
									.append("BLTTPLPhi", "on".equals(lctsdung_BLCTT50) ? "1" : "0"))
					.append("DSCTSSDung", rowDSCTSSDung).append("Status", Constants.INVOICE_STATUS.TK_CREATED)
					.append("IsDelete", false).append("Dir", pathDir).append("FileNameXML", fileNameXML)
					.append("InfoUpdated",
							new Document("UpdatedDate", LocalDateTime.now()).append("UpdatedUserID", header.getUserId())
									.append("UpdatedUserName", header.getUserName())
									.append("UpdatedUserFullName", header.getUserFullName()));

			try (MongoClient mongoClient = cfg.mongoClient()) {
				collection = mongoClient.getDatabase(cfg.dbName).getCollection("DMTKCTu");
				collection.findOneAndUpdate(docFind, new Document("$set", docUpdate), options);
			} catch (Exception e) {
			}
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
					.append("IsDelete", false).append("Status", "CREATED");

			docTmp = null;
			try (MongoClient mongoClient = cfg.mongoClient()) {
				collection = mongoClient.getDatabase(cfg.dbName).getCollection("DMTKCTu");
				docTmp = collection.find(docFind).allowDiskUse(true).iterator().next();
			} catch (Exception e) {

			}

			if (null == docTmp) {
				responseStatus = new MspResponseStatus(9999, "Không tìm thấy thông tin tờ khai chứng từ.");
				rsp.setResponseStatus(responseStatus);
				return rsp;
			}

			options = new FindOneAndUpdateOptions();
			options.upsert(false);
			options.maxTime(5000, TimeUnit.MILLISECONDS);
			options.returnDocument(ReturnDocument.AFTER);
			try (MongoClient mongoClient = cfg.mongoClient()) {
				collection = mongoClient.getDatabase(cfg.dbName).getCollection("DMTKCTu");
				collection.findOneAndUpdate(docFind, new Document("$set", new Document("IsDelete", true).append(
						"InfoDeleted",
						new Document("DeletedDate", LocalDateTime.now()).append("DeletedUserID", header.getUserId())
								.append("DeletedUserName", header.getUserName())
								.append("DeletedUserFullName", header.getUserFullName()))),
						options);
			} catch (Exception e) {

			}
			responseStatus = new MspResponseStatus(0, "SUCCESS");
			rsp.setResponseStatus(responseStatus);
			return rsp;
		default:
			break;
		}

		responseStatus = new MspResponseStatus(0, "SUCCESS");
		rsp.setResponseStatus(responseStatus);
		return rsp;
	}

	@Override
	public MsgRsp list(JSONRoot jsonRoot) throws Exception {
		Msg msg = jsonRoot.getMsg();
		MsgHeader header = msg.getMsgHeader();
		MsgRsp rsp = new MsgRsp(header);
		MspResponseStatus responseStatus = null;
		
		Iterator<Document> iter = null;
		List<Document> pipeline = new ArrayList<Document>();
		
		List<Document> rows = new ArrayList<Document>();
		
		Document docMatch = new Document("IssuerId", header.getIssuerId()).append("IsDelete", new Document("$ne", true));
		
		Document fillter = new Document("_id", 1).append("MST", 1).append("TNNT", 1)
				.append("MTDiep", 1).append("MSo", 1).append("Status", 1).append("ChiCucThueInfo", 1).append("StatusCQT", 1)
				.append("LDo", 1).append("Ten", 1).append("NLap", 1);
		
		pipeline = new ArrayList<Document>();
		pipeline.add(new Document("$match", docMatch));
		pipeline.add(new Document("$sort", new Document("_id", -1)));
		pipeline.add(new Document("$project", fillter));

		pipeline.add(
			new Document("$set", 
				new Document("_id", new Document("$toString", "$_id"))
			)
		);
		
		MongoClient mongoClient = cfg.mongoClient();
		MongoCollection<Document> collection = mongoClient.getDatabase(cfg.dbName).getCollection("DMTKCTu");
		iter =  collection.aggregate(pipeline).allowDiskUse(true).iterator();
		mongoClient.close();
		pipeline.clear();
		while(iter.hasNext()) {
			rows.add(iter.next());
		}
	
		rsp = new MsgRsp(header);
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
		}catch(Exception e) {}
		
		Document docFind = new Document("IssuerId", header.getIssuerId())
				.append("IsDelete", new Document("$ne", true)).append("_id", objectId);
		
		Document docTmp = null;
		try (MongoClient mongoClient = cfg.mongoClient()) {
			MongoCollection<Document> collection = mongoClient.getDatabase(cfg.dbName).getCollection("DMTKCTu");
			docTmp = collection.find(docFind).allowDiskUse(true).iterator().next();
		} catch (Exception e) {

		}
		
		if(null == docTmp) {
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
	public FileInfo getFileForSign(JSONRoot jsonRoot) throws Exception {
		FileInfo fileInfo = new FileInfo();
		Msg msg = jsonRoot.getMsg();
		MsgHeader header = msg.getMsgHeader();
		Object objData = msg.getObjData();

		JsonNode jsonData = null;
		if (objData != null) {
			jsonData = Json.serializer().nodeFromObject(msg.getObjData());
		} else {
			throw new Exception("Lỗi dữ liệu đầu vào");
		}

		String _id = commons.getTextJsonNode(jsonData.at("/_id")).replaceAll("\\s", "");

		ObjectId objectId = null;
		try {
			objectId = new ObjectId(_id);
		} catch (Exception e) {
		}

		Document docFind = new Document("IssuerId", header.getIssuerId()).append("_id", objectId)
				.append("IsDelete", new Document("$ne", true)).append("Status", "CREATED");

		Document docTmp = null;
		try (MongoClient mongoClient = cfg.mongoClient()) {
			MongoCollection<Document> collection = mongoClient.getDatabase(cfg.dbName).getCollection("DMTKCTu");
			docTmp = collection.find(docFind).allowDiskUse(true).iterator().next();
		} catch (Exception e) {

		}
		
		if (null == docTmp) {
			return fileInfo;
		}

		String dir = docTmp.get("Dir", "");
		String fileName = docTmp.get("FileNameXML", "");
		File file = new File(dir, fileName);

		if (!file.exists())
			return fileInfo;

		fileInfo.setFileName(fileName);
		fileInfo.setContentFile(FileUtils.readFileToByteArray(file));
		return fileInfo;
	}

	@Override
	public MsgRsp signSingle(InputStream is, JSONRoot jsonRoot, String _id) throws Exception {
		Msg msg = jsonRoot.getMsg();
		MsgHeader header = msg.getMsgHeader();
		MsgPage page = msg.getMsgPage();
		
		MsgRsp rsp = new MsgRsp(header);
		rsp.setMsgPage(page);
		MspResponseStatus responseStatus = null;
		
		/*DOC NOI DUNG XML DA KY*/
		org.w3c.dom.Document xmlDoc = commons.inputStreamToDocument(is, true);
		
		ObjectId objectId = null;
		try {
			objectId = new ObjectId(_id);
		}catch(Exception e) {}
		
		/*KIEM TRA XEM THONG TIN TKHAI CO TON TAI KHONG*/
		Document docFind = new Document("IssuerId", header.getIssuerId())
				.append("_id", objectId).append("IsDelete", new Document("$ne", true))
				.append("Status", "CREATED");
		
		Document docTmp = null;

		try (MongoClient mongoClient = cfg.mongoClient()) {
			MongoCollection<Document> collection = mongoClient.getDatabase(cfg.dbName).getCollection("DMTKCTu");
			docTmp = collection.find(docFind).allowDiskUse(true).iterator().next();
		} catch (Exception e) {

		}
		
		if(null == docTmp) {
			responseStatus = new MspResponseStatus(9999, "Không tìm thấy thông tin tờ khai.");
			rsp.setResponseStatus(responseStatus);
			return rsp;
		}
		
		/*LUU FILE VA CAP NHAT TRANG THAI*/
		String dir = docTmp.get("Dir", "");
		String fileName = _id + "_processing.xml";
		boolean check = commons.docW3cToFile(xmlDoc, dir, fileName);
		if(!check) {
			responseStatus = new MspResponseStatus(9999, "Lưu tập tin đã ký không thành công.");
			rsp.setResponseStatus(responseStatus);
			return rsp;
		}
		
		/*KET NOI VA DAY HD DEN CHUC TRUYEN NHAN*/
		org.w3c.dom.Document rTCTN = null;
		String MTDiep = docTmp.get("MTDiep", "");
		String MST = docTmp.get("MST", "");
		/*END - KET NOI VA DAY HD DEN CHUC TRUYEN NHAN*/
		
		rTCTN = tctnService.callTiepNhanThongDiep("109", MTDiep, MST, "1", commons.fileToDocument(new File(dir, fileName), true));
		if(rTCTN == null) {
			rTCTN = tctnService.callTiepNhanThongDiep("109", MTDiep, MST, "1", commons.fileToDocument(new File(dir, fileName), true));
		}
		
		/*DO DU LIEU TRA VE - CAP NHAT LAI KET QUA*/
		XPath xPath = XPathFactory.newInstance().newXPath();
		Node nodeDLHDon = (Node) xPath.evaluate("/TDiep", rTCTN, XPathConstants.NODE);
		String codeTTTNhan = "3";
		codeTTTNhan = commons.getTextFromNodeXML((Element) xPath.evaluate("DLieu/TBao/TTTNhan", nodeDLHDon, XPathConstants.NODE));
		
		switch (codeTTTNhan) {
		case "1":
			responseStatus = new MspResponseStatus(9999, "Không tìm thấy tenant dữ liệu.");
			rsp.setResponseStatus(responseStatus);
			return rsp;
		case "2":
			responseStatus = new MspResponseStatus(9999, "Mã thông điệp đã tồn tại.");
			rsp.setResponseStatus(responseStatus);
			return rsp;
		case "3":
			responseStatus = new MspResponseStatus(9999, "Thất bại, lỗi Exception.");
			rsp.setResponseStatus(responseStatus);
			return rsp;
		default:
			break;
		}

		/*CAP NHAT LAI TRANG THAI DANG CHO XU LY*/
		FindOneAndUpdateOptions options = new FindOneAndUpdateOptions();
		options.upsert(false);
		options.maxTime(5000, TimeUnit.MILLISECONDS);
		options.returnDocument(ReturnDocument.AFTER);

		try (MongoClient mongoClient = cfg.mongoClient()) {
			MongoCollection<Document> collection = mongoClient.getDatabase(cfg.dbName).getCollection("DMTKCTu");
			collection.findOneAndUpdate(docFind,
					new Document("$set", 
							new Document("Status", "PROCESSING")
							.append("InfoSigned", new Document("SignedDate", LocalDateTime.now())
												.append("SignedUserID", header.getUserId())
												.append("SignedUserName", header.getUserName())
												.append("SignedUserFullName", header.getUserFullName()))),
					options);
		} catch (Exception e) {
		}
		responseStatus = new MspResponseStatus(0, Constants.MAP_ERROR.get(0));
		rsp.setResponseStatus(responseStatus);
		return rsp;
	}

	@Override
	public MsgRsp refreshStatusCQT(JSONRoot jsonRoot) throws Exception {
		Msg msg = jsonRoot.getMsg();
		MsgHeader header = msg.getMsgHeader();
		MsgPage page = msg.getMsgPage();
		
		Object objData = msg.getObjData();
		
		JsonNode jsonData = null;
		if(objData != null) {
			jsonData = Json.serializer().nodeFromObject(msg.getObjData());
		}else{
			throw new Exception("Lỗi dữ liệu đầu vào");
		}
		
		String _id = commons.getTextJsonNode(jsonData.at("/_id")).replaceAll("\\s", "");
		
		MsgRsp rsp = new MsgRsp(header);
		rsp.setMsgPage(page);
		MspResponseStatus responseStatus = null;
		
		ObjectId objectId = null;
		try {
			objectId = new ObjectId(_id);
		}catch(Exception e) {}
		
		/*KIEM TRA XEM THONG TIN TKHAI CO TON TAI KHONG*/
		Document docFind = new Document("IssuerId", header.getIssuerId())
				.append("_id", objectId).append("IsDelete", new Document("$ne", true))
				.append("Status", "PROCESSING");
		
		Document docTmp = null;
		try (MongoClient mongoClient = cfg.mongoClient()) {
			MongoCollection<Document> collection = mongoClient.getDatabase(cfg.dbName).getCollection("DMTKCTu");
			docTmp = collection.find(docFind).allowDiskUse(true).iterator().next();
		} catch (Exception e) {

		}

		if(null == docTmp) {
			responseStatus = new MspResponseStatus(9999, "Không tìm thấy thông tin tờ khai.");
			rsp.setResponseStatus(responseStatus);
			return rsp;
		}
		
		String MTDiep = docTmp.get("MTDiep", "");
		
		org.w3c.dom.Document rTCTN = tctnService.callTraCuuThongDiep(MTDiep);
		if(rTCTN == null) {
			responseStatus = new MspResponseStatus(9999, "Kết nối với TCTN không thành công.");
			rsp.setResponseStatus(responseStatus);
			return rsp;
		}
		
		/*DO DU LIEU TRA VE - CAP NHAT LAI KET QUA*/
		XPath xPath = XPathFactory.newInstance().newXPath();
		Node nodeKetQuaTraCuu = (Node) xPath.evaluate("/KetQuaTraCuu", rTCTN, XPathConstants.NODE);
		String MaKetQua = commons.getTextFromNodeXML((Element) xPath.evaluate("MaKetQua", nodeKetQuaTraCuu, XPathConstants.NODE));
		String MoTaKetQua = commons.getTextFromNodeXML((Element) xPath.evaluate("MoTaKetQua", nodeKetQuaTraCuu, XPathConstants.NODE));
		
		if(!"0".equals(MaKetQua)) {
			responseStatus = new MspResponseStatus(9999, MoTaKetQua);
			rsp.setResponseStatus(responseStatus);
			return rsp;
		}
		
		Node nodeTDiep = (Node) xPath.evaluate("DuLieu/TDiep[last()]", nodeKetQuaTraCuu, XPathConstants.NODE);
		if(nodeTDiep == null) {
			responseStatus = new MspResponseStatus(9999, "Không đọc được kết quả tra cứu.");
			rsp.setResponseStatus(responseStatus);
			return rsp;
		}
		String CQT_MLTDiep = commons.getTextFromNodeXML((Element) xPath.evaluate("TTChung/MLTDiep", nodeTDiep, XPathConstants.NODE));
		if("|110|111|".indexOf("|" + CQT_MLTDiep + "|") == -1) {
			responseStatus = new MspResponseStatus(9999, "CQT chưa có thông báo kết quả trả về.");
			rsp.setResponseStatus(responseStatus);
			return rsp;
		}
		
		String dir = docTmp.get("Dir", "");
		String fileName = _id + "_" + CQT_MLTDiep + ".xml";
		boolean boo = false;
		try {
			boo = commons.docW3cToFile(rTCTN, dir, fileName);
		}catch(Exception e) {}
		if(!boo) {
			responseStatus = new MspResponseStatus(9999, "Lưu tập tin trả về từ CQT không thành công.");
			rsp.setResponseStatus(responseStatus);
			return rsp;
		}
		
		String MLoi = "";
		String MTa = "";
		Node nodeTmp = null;
		/*CAP NHAT TRANG THAI COMPLETE - TRANG THAI CQT*/
		FindOneAndUpdateOptions options = new FindOneAndUpdateOptions();
		options.upsert(false);
		options.maxTime(5000, TimeUnit.MILLISECONDS);
		options.returnDocument(ReturnDocument.AFTER);
		
		Document docUpdate = new Document("StatusCQT", CQT_MLTDiep);
		if("111".equals(CQT_MLTDiep)) {
			/*LAY DANH SACH LOI (NEU CO)*/
			NodeList nodeListDSLDKCNhan = (NodeList) xPath.evaluate("DLieu/TBao/DLTBao/DSLDKCNhan/LDo", nodeTDiep, XPathConstants.NODESET) ;
			if(null == nodeListDSLDKCNhan || nodeListDSLDKCNhan.getLength() == 0) {
				docUpdate.append("Status", "COMPLETE");	
			}else {
				List<Document> DSLDKCNhan = new ArrayList<Document>();
				for(int i = 0; i < nodeListDSLDKCNhan.getLength(); i++) {
					nodeTmp = nodeListDSLDKCNhan.item(i);
					if("".equals(MLoi)) {
						MLoi = commons.getTextFromNodeXML((Element) xPath.evaluate("MLoi", nodeTmp, XPathConstants.NODE));
						MTa = commons.getTextFromNodeXML((Element) xPath.evaluate("MTa", nodeTmp, XPathConstants.NODE));
					}
					DSLDKCNhan.add(
						new Document("MLoi", commons.getTextFromNodeXML((Element) xPath.evaluate("MLoi", nodeTmp, XPathConstants.NODE)))
						.append("MTa", commons.getTextFromNodeXML((Element) xPath.evaluate("MTa", nodeTmp, XPathConstants.NODE)))
					);
				}
				docUpdate.append("Status", Constants.INVOICE_STATUS.ERROR_CQT)
				.append("LDo", 
					new Document("MLoi", MLoi).append("MTa", MTa)
				)
				.append("DSLDKCNhan", DSLDKCNhan);
			}
			
		}else {
			MLoi = commons.getTextFromNodeXML((Element) xPath.evaluate("DLieu/TBao/DLTBao/DSLDKCNhan/LDo/MLoi", nodeTDiep, XPathConstants.NODE));
			MTa = commons.getTextFromNodeXML((Element) xPath.evaluate("DLieu/TBao/DLTBao/DSLDKCNhan/LDo/MTa", nodeTDiep, XPathConstants.NODE));
			
			if("".equals(MLoi)) {
				responseStatus = new MspResponseStatus(9999, "CQT chưa có thông báo kết quả trả về.");
				rsp.setResponseStatus(responseStatus);
				return rsp;
			}
			
			docUpdate.append("Status", Constants.INVOICE_STATUS.ERROR_CQT)
				.append("LDo", 
					new Document("MLoi", MLoi).append("MTa", MTa)
				);
			
		}
	
		try (MongoClient mongoClient = cfg.mongoClient()) {
			MongoCollection<Document> collection = mongoClient.getDatabase(cfg.dbName).getCollection("DMTKCTu");
			collection.findOneAndUpdate(
					docFind, 
					new Document("$set", docUpdate), 
					options
				);			
		} catch (Exception e) {

		}
	
		responseStatus = new MspResponseStatus(0, Constants.MAP_ERROR.get(0));
		rsp.setResponseStatus(responseStatus);
		return rsp;
	}	
}
