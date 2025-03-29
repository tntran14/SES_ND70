package vn.sesgroup.hddt.user.impl;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoField;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.ss.util.NumberToTextConverter;
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
import com.mongodb.client.model.UpdateOptions;

import vn.sesgroup.hddt.configuration.ConfigConnectMongo;
import vn.sesgroup.hddt.dto.FileInfo;
import vn.sesgroup.hddt.dto.GetXMLInfoXMLDTO;
import vn.sesgroup.hddt.model.CT_TNCNExcelForm;
import vn.sesgroup.hddt.user.dao.AbstractDAO;
import vn.sesgroup.hddt.user.dao.CTTNCNDAO;
import vn.sesgroup.hddt.utility.Constants;
import vn.sesgroup.hddt.utility.Json;
import vn.sesgroup.hddt.utility.SystemParams;
import vn.sesgroup.hddt.utility.UpdateSignedMultiBillReq;

@Repository
@Transactional
public class CTTNCNImpl extends AbstractDAO implements CTTNCNDAO {
	private static final Logger log = LogManager.getLogger(QLNVTNCNImpl.class);
	@Autowired
	ConfigConnectMongo cfg;

	Document docUpsert = null;

	@Override
	public MsgRsp list(JSONRoot jsonRoot) throws Exception {
		Msg msg = jsonRoot.getMsg();
		MsgHeader header = msg.getMsgHeader();
		MsgPage page = msg.getMsgPage();
		Object objData = msg.getObjData();

		String shd = "";
		String datelap = "";
		LocalDate dateTo = null;
		Document docMatchDate = null;
		JsonNode jsonData = null;
		if (objData != null) {
			jsonData = Json.serializer().nodeFromObject(objData);
			shd = commons.getTextJsonNode(jsonData.at("/SHD")).trim().replaceAll("\\s+", " ");
			datelap = commons.getTextJsonNode(jsonData.at("/DateLap"));

		}
		dateTo = "".equals(datelap) || !commons.checkLocalDate(datelap, Constants.FORMAT_DATE.FORMAT_DATE_WEB)? null: commons.convertStringToLocalDate(datelap, Constants.FORMAT_DATE.FORMAT_DATE_WEB);
		if(null != dateTo) {
			docMatchDate = new Document();
			if(null != dateTo)
				docMatchDate.append("$eq", dateTo);
		}
		MsgRsp rsp = new MsgRsp(header);
		MspResponseStatus responseStatus = null;

		ObjectId objectId = null;
		Document docTmp = null;
		Iterable<Document> cursor = null;
		Iterator<Document> iter = null;
		List<Document> pipeline = new ArrayList<Document>();

		Document docMatch = new Document("IssuerId", header.getIssuerId()).append("IsDelete",
				new Document("$ne", true));
		if (!"".equals(shd))
			docMatch.append("SHDon", commons.stringToInteger(shd));
		
		if (docMatchDate != null) {
			docMatch.append("DateTime", docMatchDate);
			}
		pipeline = new ArrayList<Document>();
		pipeline.add(new Document("$match", docMatch));
		pipeline.add(new Document("$sort", new Document("SignStatus", 1).append("SHDon", -1).append("_id", -1)));
		pipeline.addAll(createFacetForSearchNotSort(page));

	
		
		MongoClient mongoClient = cfg.mongoClient();
		MongoCollection<Document> collection = mongoClient.getDatabase(cfg.dbName).getCollection("ChungTuTNCN");
		try {
			docTmp =   collection.aggregate(pipeline).allowDiskUse(true).iterator().next();		
		} catch (Exception e) {
			// TODO: handle exception
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
				hItem.put("_id", objectId.toString());

				hItem.put("Status", doc.get("Status"));
				hItem.put("SignStatus", doc.get("SignStatus"));
				hItem.put("SHDon", doc.get("SHDon"));
				hItem.put("DateSave", doc.get("DateSave"));
				hItem.put("DateLap", doc.get("DateLap"));
				hItem.put("DateTime", doc.get("DateTime"));
				hItem.put("Date", doc.get("Date"));
				hItem.put("Code", doc.get("Code"));
				hItem.put("TaxCode", doc.get("TaxCode"));
				hItem.put("Name", doc.get("Name"));
				hItem.put("Address", doc.get("Address"));
				hItem.put("InfoCreated", doc.get("InfoCreated"));
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
		String name = commons.getTextJsonNode(jsonData.at("/Name"));
		String code = commons.getTextJsonNode(jsonData.at("/Code")).trim().replaceAll("\\s+", " ");
		String address = commons.getTextJsonNode(jsonData.at("/Address"));
		String taxcode = commons.getTextJsonNode(jsonData.at("/Taxcode")).trim().replaceAll("\\s+", " ");
		String cutru = commons.getTextJsonNode(jsonData.at("/CuTru")).trim().replaceAll("\\s+", " ");
		String cccd = commons.getTextJsonNode(jsonData.at("/CCCD")).trim().replaceAll("\\s+", " ");
		String qt = commons.getTextJsonNode(jsonData.at("/QuocTich")).trim().replaceAll("\\s+", " ");
		String cccddate = commons.getTextJsonNode(jsonData.at("/CCCDDATE")).trim().replaceAll("\\s+", " ");
		String cccdaddress = commons.getTextJsonNode(jsonData.at("/CCCDADDRESS")).trim().replaceAll("\\s+", " ");
		String kibc = commons.getTextJsonNode(jsonData.at("/KyBaoCao")).trim().replaceAll("\\s+", " ");
		String tungay = commons.getTextJsonNode(jsonData.at("/TuNgay")).trim().replaceAll("\\s+", " ");
		String denngay = commons.getTextJsonNode(jsonData.at("/DenNgay")).trim().replaceAll("\\s+", " ");
		String ktn = commons.getTextJsonNode(jsonData.at("/KhoanThuNhap")).trim().replaceAll("\\s+", " ");
		String tdtn = commons.getTextJsonNode(jsonData.at("/DateThuNhap")).trim().replaceAll("\\s+", " ");
		String kbh = commons.getTextJsonNode(jsonData.at("/KhoanBaoHiem")).trim().replaceAll("\\s+", " ");
		String ttnkt = commons.getTextJsonNode(jsonData.at("/TongTNKhauTru")).trim().replaceAll("\\s+", " ");
		String ttntt = commons.getTextJsonNode(jsonData.at("/TongTNTinhThue")).trim().replaceAll("\\s+", " ");
		String sttndkt = commons.getTextJsonNode(jsonData.at("/SoTienCaNhanKhauTru")).trim().replaceAll("\\s+", " ");
		String mauSoTNCN = commons.getTextJsonNode(jsonData.at("/MauSoTNCN")).trim().replaceAll("\\s+", " ");
		MsgRsp rsp = new MsgRsp(header);
		rsp.setMsgPage(page);
		MspResponseStatus responseStatus = null;

		ObjectId objectId = null;
		ObjectId mstncnId = null;
		Document docFind = null;
		Document docTmp = null;
		Document docUpsert = null;
		Document docR = null;
		List<Document> pipeline = null;
		FindOneAndUpdateOptions options = null;
		Iterable<Document> cursor = null;
		Iterator<Document> iter = null;
		String ngayt = "";
		String ngayd = "";
		String[] words;
		String nam = "";
		String thang = "";
		int number = 0;
		String[] words1;
		String nam1 = "";
		String thang1 = "";
		int number1 = 0;
		String ngayluu = "";

		String fileNameXML = "";
		String pathDir = "";
		Path path = null;
		File file = null;

		ObjectId objectIdUser = null;
		ObjectId objectIdTK = null;
		ObjectId objectIdDMCTS = null;
		HashMap<String, Object> hO = null;
		Element elementSubTmp = null;
		Element elementSubTmp01 = null;
		Element elementSubContent = null;
		Element elementTmp = null;
		boolean isSdaveFile = false;
		DocumentBuilderFactory dbf = null;
		DocumentBuilder db = null;
		org.w3c.dom.Document doc = null;
		Element root = null;

		Element elementContent = null;

		switch (actionCode) {
		case Constants.MSG_ACTION_CODE.CREATED:
			objectId = null;
			mstncnId = null;
			try {
				objectId = new ObjectId(header.getIssuerId());
				mstncnId = new ObjectId(mauSoTNCN);
			} catch (Exception e) {
			}
			docFind = new Document("_id", objectId).append("IsActive", true).append("IsDelete",
					new Document("$ne", true));
			pipeline = new ArrayList<Document>();
			pipeline.add(new Document("$match", docFind));
			pipeline.add(new Document("$lookup", new Document("from", "ChungTuTNCN")
					.append("let", new Document("vIssuerId", new Document("$toString", "$_id")))
					.append("pipeline",
							Arrays.asList(new Document("$match", new Document("$expr", new Document("$and",
									Arrays.asList(new Document("$eq", Arrays.asList("$IssuerId", "$$vIssuerId")),
											new Document("$ne", Arrays.asList("$IsDelete", true)),
											new Document("$or", Arrays.asList(new Document("$eq",
													Arrays.asList("$Name", commons.regexEscapeForMongoQuery(name)))))))

							)), new Document("$limit", 1))).append("as", "ChungTuTNCN")));
			pipeline.add(new Document("$unwind",
					new Document("path", "$ChungTuTNCN").append("preserveNullAndEmptyArrays", true)));

			pipeline.add(new Document("$lookup",
					new Document("from", "TNCNStaff")
							.append("pipeline",
									Arrays.asList(new Document("$match",
											new Document("IssuerId", header.getIssuerId()).append("Name", name))))
							.append("as", "TNCNStaff")));
			pipeline.add(new Document("$unwind",
					new Document("path", "$TNCNStaff").append("preserveNullAndEmptyArrays", true)));

			pipeline.add(new Document("$lookup",
					new Document("from", "DMMSTNCN")
							.append("pipeline",
									Arrays.asList(new Document("$match", new Document("IssuerId", header.getIssuerId())
											.append("IsDelete", new Document("$ne", true)).append("IsActive", true)
											.append("_id", mstncnId))))
							.append("as", "DMMSTNCN")));
			pipeline.add(new Document("$unwind",
					new Document("path", "$DMMSTNCN").append("preserveNullAndEmptyArrays", true)));

			
			
			MongoClient mongoClient = cfg.mongoClient();
			MongoCollection<Document> collection = mongoClient.getDatabase(cfg.dbName).getCollection("Issuer");
			try {
				docTmp =   collection.aggregate(pipeline).allowDiskUse(true).iterator().next();		
			} catch (Exception e) {
				// TODO: handle exception
			}
				
			mongoClient.close();

			if (null == docTmp) {
				responseStatus = new MspResponseStatus(9999, "Không tìm thấy thông tin.");
				rsp.setResponseStatus(responseStatus);
				return rsp;
			}
			String mauso = docTmp.getEmbedded(Arrays.asList("DMMSTNCN", "_id"), ObjectId.class).toString();

			/// CHECK TU NGAY DEN NGAY

			String thangnv = "";

			if (tungay.equals("") && denngay.equals("")) {
				thangnv = "1,2,3,4,5,6,7,8,9,10,11,12";
				ngayluu = kibc;
			} else {
				ngayt = tungay.toString();
				ngayd = denngay.toString();

				words = ngayt.split("/");
				nam = words[2];
				thang = words[1];
				number = Integer.parseInt(thang);

				words1 = ngayd.split("/");
				nam1 = words1[2];
				thang1 = words1[1];
				number1 = Integer.parseInt(thang1);

//				ngayluu = number + "-" + number1 + "/" + kibc;
				ngayluu = number + "-" + number1 + "/" + nam1;

				words = tungay.split("/");
				nam = words[2];
				thang = words[1];
				int tuthang = Integer.parseInt(thang);

				words = denngay.split("/");
				nam = words[2];
				thang = words[1];
				int denthang = Integer.parseInt(thang);

				for (int i = tuthang; i <= denthang; i++) {
					if (i == denthang) {
						thangnv += i;
					} else {
						thangnv += i + ",";
					}

				}

			}

			// END CHECK TU NGAY DEN NGAY

			/* TAO FILE XML */
			objectIdTK = new ObjectId();
			path = Paths.get(SystemParams.DIR_E_INVOICE_CTTNCN, docTmp.getEmbedded(Arrays.asList("TaxCode"), ""));

			pathDir = path.toString();
			String dir = pathDir;
			file = path.toFile();
			if (!file.exists())
				file.mkdirs();
			String kh = docTmp.getEmbedded(Arrays.asList("DMMSTNCN", "KyHieu"), "") + "/"
					+ docTmp.getEmbedded(Arrays.asList("DMMSTNCN", "Nam"), "") + "/"
					+ docTmp.getEmbedded(Arrays.asList("DMMSTNCN", "ChungTu"), "");

			/* TAO FILE XML */
			fileNameXML = objectIdTK.toString() + ".xml";

			dbf = DocumentBuilderFactory.newInstance();
			db = dbf.newDocumentBuilder();
			doc = db.newDocument();
			doc.setXmlStandalone(true);

			root = doc.createElement("HDon");
			doc.appendChild(root);

			elementContent = doc.createElement("DLHDon");
			elementContent.setAttribute("Id", "data");
			root.appendChild(elementContent);

			/* THONG TIN CHUNG TO KHAI */
			elementSubContent = doc.createElement("TTChung");
			elementSubContent.appendChild(commons.createElementWithValue(doc, "PBan", "2.0.0"));
			elementSubContent.appendChild(commons.createElementWithValue(doc, "MSo",
					docTmp.getEmbedded(Arrays.asList("DMMSTNCN", "MauSo"), "")));
			elementSubContent.appendChild(commons.createElementWithValue(doc, "KyHieu", kh));
			elementSubContent.appendChild(commons.createElementWithValue(doc, "ID", objectIdTK.toString()));
			elementSubContent.appendChild(commons.createElementWithValue(doc, "SHDon", "")); // SE PHAT SINH KHI KY

			elementContent.appendChild(elementSubContent);

			/* TO CHUC TRA THU NHAP */
			elementSubContent = doc.createElement("TCTTN");
			elementSubContent.appendChild(
					commons.createElementWithValue(doc, "Name", docTmp.getEmbedded(Arrays.asList("Name"), "")));
			elementSubContent.appendChild(
					commons.createElementWithValue(doc, "TaxCode", docTmp.getEmbedded(Arrays.asList("TaxCode"), "")));
			elementSubContent.appendChild(
					commons.createElementWithValue(doc, "Address", docTmp.getEmbedded(Arrays.asList("Address"), "")));
			elementSubContent.appendChild(
					commons.createElementWithValue(doc, "Phone", docTmp.getEmbedded(Arrays.asList("Phone"), "")));
			elementContent.appendChild(elementSubContent);

			/* THONG TIN NOP THUE */
			elementSubContent = doc.createElement("TTNT");
			elementSubContent.appendChild(commons.createElementWithValue(doc, "Name", name));
			elementSubContent.appendChild(commons.createElementWithValue(doc, "TaxCode", taxcode));
			elementSubContent.appendChild(commons.createElementWithValue(doc, "QuocTich", qt));
			elementSubContent.appendChild(commons.createElementWithValue(doc, "CuTru", cutru));
			elementSubContent.appendChild(commons.createElementWithValue(doc, "Address", address));
			elementSubContent.appendChild(commons.createElementWithValue(doc, "CCCD", cccd));
			elementSubContent.appendChild(commons.createElementWithValue(doc, "CCCDDATE", cccddate));
			elementSubContent.appendChild(commons.createElementWithValue(doc, "CCCDADDRESS", cccdaddress));
			elementContent.appendChild(elementSubContent);

			/* THONG TIN THUE THU NHAP CA NHAN KHAU TRU */
			elementSubContent = doc.createElement("TTTTNCNKT");
			elementSubContent.appendChild(commons.createElementWithValue(doc, "KhoanThu", ktn));
			elementSubContent.appendChild(commons.createElementWithValue(doc, "KhoanBH", kbh));
			elementSubContent.appendChild(commons.createElementWithValue(doc, "ThoiDiemTraTNMonth", thangnv));
			elementSubContent.appendChild(commons.createElementWithValue(doc, "ThoiDiemTraTNYear", kibc));
			elementSubContent.appendChild(commons.createElementWithValue(doc, "TongThuKhauTru", ttnkt));
			elementSubContent.appendChild(commons.createElementWithValue(doc, "TongThuThue", ttntt));
			elementSubContent.appendChild(commons.createElementWithValue(doc, "SoThueKhauTru", sttndkt));
			elementContent.appendChild(elementSubContent);

			/* END - TAO FILE XML */
			isSdaveFile = commons.docW3cToFile(doc, pathDir, fileNameXML);
			if (!isSdaveFile) {
				throw new Exception("Lưu dữ liệu không thành công.");
			}
			String secureKey = commons.csRandomNumbericString(6);

			docUpsert = new Document("_id", objectIdTK).append("IssuerId", header.getIssuerId())
					.append("SecureKey", secureKey).append("MauSoHD", mauso).append("Name", name).append("Code", code)
					.append("Address", address).append("TaxCode", taxcode).append("CuTru", cutru).append("KyHieu", kh)
					.append("CMND-CCCD",
							new Document("CCCD", cccd).append("CCCDDATE", cccddate).append("CCCDADDRESS", cccdaddress)
									.append("QuocTich", qt))
					.append("TNCNKhauTru", new Document("KhoanThuNhap", ktn)

							.append("KhoanBaoHiem", kbh).append("TongTNKhauTru", ttnkt).append("TongTNTinhThue", ttntt)
							.append("SoTienCaNhanKhauTru", sttndkt))
					.append("KyBaoCao", kibc).append("TuNgay", tungay).append("DenNgay", denngay)
					.append("DateSave", ngayluu).append("Dir", dir).append("FileNameXML", fileNameXML)
					.append("IsActive", true).append("IsDelete", false)
					.append("SignStatus", Constants.INVOICE_SIGN_STATUS.NOSIGN)
					.append("Status", Constants.INVOICE_STATUS.CREATED).append("Date", LocalDate.now().toString())
					.append("DateTime", LocalDate.now()).append("InfoCreated",
							new Document("CreateDate", LocalDateTime.now()).append("CreateUserID", header.getUserId())
									.append("CreateUserName", header.getUserName())
									.append("CreateUserFullName", header.getUserFullName()));

		
			mongoClient = cfg.mongoClient();
			collection = mongoClient.getDatabase(cfg.dbName).getCollection("ChungTuTNCN");
			collection.insertOne(docUpsert);			
			mongoClient.close();
			
			
			responseStatus = new MspResponseStatus(0, "SUCCESS");
			rsp.setResponseStatus(responseStatus);
			return rsp;

		case Constants.MSG_ACTION_CODE.MODIFY:
			objectId = null;
			mstncnId = null;
			try {
				objectId = new ObjectId(_id);
				mstncnId = new ObjectId(mauSoTNCN);
			} catch (Exception e) {
			}
			ObjectId objectIdIssu = null;
			try {
				objectIdIssu = new ObjectId(header.getIssuerId());
			} catch (Exception e) {
			}

			docFind = new Document("IssuerId", header.getIssuerId()).append("IsDelete", new Document("$ne", true))
					.append("_id", objectId);

			pipeline = new ArrayList<Document>();
			pipeline.add(new Document("$match", docFind));
			pipeline.add(
					new Document("$lookup",
							new Document("from", "Issuer")
									.append("pipeline",
											Arrays.asList(
													new Document("$match",
															new Document("_id", objectIdIssu).append("IsDelete",
																	new Document("$ne", true)))))
									.append("as", "Issuer")));
			pipeline.add(new Document("$unwind",
					new Document("path", "$Issuer").append("preserveNullAndEmptyArrays", true)));

			pipeline.add(new Document("$lookup",
					new Document("from", "DMMSTNCN")
							.append("pipeline",
									Arrays.asList(new Document("$match", 
											new Document("IssuerId", header.getIssuerId())
											.append("IsDelete", new Document("$ne", true))
											.append("_id", mstncnId))))
							.append("as", "DMMSTNCN")));
			pipeline.add(new Document("$unwind",
					new Document("path", "$DMMSTNCN").append("preserveNullAndEmptyArrays", true)));

		
			
			 mongoClient = cfg.mongoClient();
			 collection = mongoClient.getDatabase(cfg.dbName).getCollection("ChungTuTNCN");
			 try {
					docTmp =   collection.aggregate(pipeline).allowDiskUse(true).iterator().next();			
			} catch (Exception e) {
				// TODO: handle exception
			}
			mongoClient.close();
			
			
			if (null == docTmp) {
				responseStatus = new MspResponseStatus(9999, "Không tìm thấy thông tin chứng từ.");
				rsp.setResponseStatus(responseStatus);
				return rsp;
			}
			
			String mauso1 = docTmp.getEmbedded(Arrays.asList("DMMSTNCN", "_id"), ObjectId.class).toString();
			// int shd = docTmp.getEmbedded(Arrays.asList("SHDon"), 0);
			// int shd = docTmp.getInteger("SHDon");

			String thangnv1 = "";

			if (tungay.equals("") && denngay.equals("")) {
				thangnv = "1,2,3,4,5,6,7,8,9,10,11,12";
				ngayluu = kibc;
			} else {
				ngayt = tungay.toString();
				ngayd = denngay.toString();

				words = ngayt.split("/");
				nam = words[2];
				thang = words[1];
				number = Integer.parseInt(thang);

				words1 = ngayd.split("/");
				nam1 = words1[2];
				thang1 = words1[1];
				number1 = Integer.parseInt(thang1);

//				ngayluu = number + "-" + number1 + "/" + kibc;
				ngayluu = number + "-" + number1 + "/" + nam1;
				
				words = tungay.split("/");
				nam = words[2];
				thang = words[1];
				int tuthang1 = Integer.parseInt(thang);

				words = denngay.split("/");
				nam = words[2];
				thang = words[1];
				int denthang1 = Integer.parseInt(thang);

				thangnv1 = "";

				for (int i = tuthang1; i <= denthang1; i++) {
					if (i == denthang1) {
						thangnv1 += i;
					} else {
						thangnv1 += i + ",";
					}

				}
			}
			/* TAO FILE XML */
			objectIdTK = objectId;
			path = Paths.get(SystemParams.DIR_E_INVOICE_CTTNCN,
					docTmp.getEmbedded(Arrays.asList("Issuer", "TaxCode"), ""));
			pathDir = path.toString();
			file = path.toFile();
			if (!file.exists())
				file.mkdirs();
			String kh1 = docTmp.getEmbedded(Arrays.asList("DMMSTNCN", "KyHieu"), "") + "/"
					+ docTmp.getEmbedded(Arrays.asList("DMMSTNCN", "Nam"), "") + "/"
					+ docTmp.getEmbedded(Arrays.asList("DMMSTNCN", "ChungTu"), "");

			/* TAO FILE XML */
			fileNameXML = objectIdTK.toString() + ".xml";

			dbf = DocumentBuilderFactory.newInstance();
			db = dbf.newDocumentBuilder();
			doc = db.newDocument();
			doc.setXmlStandalone(true);

			root = doc.createElement("HDon");
			doc.appendChild(root);

			elementContent = doc.createElement("DLHDon");
			elementContent.setAttribute("Id", "data");
			root.appendChild(elementContent);

			/* THONG TIN CHUNG TO KHAI */
			elementSubContent = doc.createElement("TTChung");
			elementSubContent.appendChild(commons.createElementWithValue(doc, "PBan", "2.0.0"));
			elementSubContent.appendChild(commons.createElementWithValue(doc, "MSo",
					docTmp.getEmbedded(Arrays.asList("DMMSTNCN", "MauSo"), "")));
			elementSubContent.appendChild(commons.createElementWithValue(doc, "KyHieu", kh1));
			elementSubContent.appendChild(commons.createElementWithValue(doc, "ID", objectIdTK.toString()));
			elementSubContent.appendChild(commons.createElementWithValue(doc, "SHDon", "")); // SE PHAT SINH KHI KY
			elementContent.appendChild(elementSubContent);

			/* TO CHUC TRA THU NHAP */
			elementSubContent = doc.createElement("TCTTN");
			elementSubContent.appendChild(commons.createElementWithValue(doc, "Name",
					docTmp.getEmbedded(Arrays.asList("Issuer", "Name"), "")));
			elementSubContent.appendChild(commons.createElementWithValue(doc, "TaxCode",
					docTmp.getEmbedded(Arrays.asList("Issuer", "TaxCode"), "")));
			elementSubContent.appendChild(commons.createElementWithValue(doc, "Address",
					docTmp.getEmbedded(Arrays.asList("Issuer", "Address"), "")));
			elementSubContent.appendChild(commons.createElementWithValue(doc, "Phone",
					docTmp.getEmbedded(Arrays.asList("Issuer", "Phone"), "")));
			elementContent.appendChild(elementSubContent);

			/* THONG TIN NOP THUE */
			elementSubContent = doc.createElement("TTNT");
			elementSubContent.appendChild(commons.createElementWithValue(doc, "Name", name));
			elementSubContent.appendChild(commons.createElementWithValue(doc, "TaxCode", taxcode));
			elementSubContent.appendChild(commons.createElementWithValue(doc, "QuocTich", qt));
			elementSubContent.appendChild(commons.createElementWithValue(doc, "CuTru", cutru));
			elementSubContent.appendChild(commons.createElementWithValue(doc, "Address", address));
			elementSubContent.appendChild(commons.createElementWithValue(doc, "CCCD", cccd));
			elementSubContent.appendChild(commons.createElementWithValue(doc, "CCCDDATE", cccddate));
			elementSubContent.appendChild(commons.createElementWithValue(doc, "CCCDADDRESS", cccdaddress));
			elementContent.appendChild(elementSubContent);

			/* THONG TIN THUE THU NHAP CA NHAN KHAU TRU */
			elementSubContent = doc.createElement("TTTTNCNKT");
			elementSubContent.appendChild(commons.createElementWithValue(doc, "KhoanThu", ktn));
			elementSubContent.appendChild(commons.createElementWithValue(doc, "KhoanBH", kbh));
			elementSubContent.appendChild(commons.createElementWithValue(doc, "ThoiDiemTraTNMonth", thangnv1));
			elementSubContent.appendChild(commons.createElementWithValue(doc, "ThoiDiemTraTNYear", kibc));
			elementSubContent.appendChild(commons.createElementWithValue(doc, "TongThuKhauTru", ttnkt));
			elementSubContent.appendChild(commons.createElementWithValue(doc, "TongThuThue", ttntt));
			elementSubContent.appendChild(commons.createElementWithValue(doc, "SoThueKhauTru", sttndkt));
			elementContent.appendChild(elementSubContent);

			/* END - TAO FILE XML */
			isSdaveFile = commons.docW3cToFile(doc, pathDir, fileNameXML);
			if (!isSdaveFile) {
				throw new Exception("Lưu dữ liệu không thành công.");
			}

			options = new FindOneAndUpdateOptions();
			options.upsert(false);
			options.maxTime(5000, TimeUnit.MILLISECONDS);
			options.returnDocument(ReturnDocument.AFTER);

			
			mongoClient = cfg.mongoClient();
			collection = mongoClient.getDatabase(cfg.dbName).getCollection("ChungTuTNCN");
			docR = collection.findOneAndUpdate(docFind,
					new Document("$set", new Document("Name", name).append("Code", code)
							.append("Address", address).append("TaxCode", taxcode).append("CuTru", cutru)
							.append("MauSoHD", mauso1)
							
							.append("CMND-CCCD",
									new Document("CCCD", cccd).append("CCCDDATE", cccddate)
											.append("CCCDADDRESS", cccdaddress).append("QuocTich", qt))
							.append("TNCNKhauTru", new Document("KhoanThuNhap", ktn)

									.append("KhoanBaoHiem", kbh).append("TongTNKhauTru", ttnkt)
									.append("TongTNTinhThue", ttntt).append("SoTienCaNhanKhauTru", sttndkt))
							.append("KyBaoCao", kibc).append("TuNgay", tungay).append("DenNgay", denngay)
							.append("TuNgay", tungay).append("DenNgay", denngay).append("DateSave", ngayluu)
							.append("Date", LocalDate.now().toString())
							.append("DateTime", LocalDate.now())
							.append("IsActive", true).append("IsDelete", false).append("InfoUpdated",
									new Document("UpdatedDate", LocalDateTime.now())
											.append("UpdatedUserID", header.getUserId())
											.append("UpdatedUserName", header.getUserName())
											.append("UpdatedUserFullName", header.getUserFullName()))),
					options);	
			mongoClient.close();
			
			
		
			responseStatus = new MspResponseStatus(0, "SUCCESS");
			rsp.setResponseStatus(responseStatus);
			rsp.setObjData(docTmp.get("_id"));
			return rsp;
		case Constants.MSG_ACTION_CODE.DELETE:
			List<ObjectId> objectIds = new ArrayList<ObjectId>();

			try {
				if (!jsonData.at("/ids").isMissingNode()) {
					for (JsonNode o : jsonData.at("/ids")) {
						try {
							objectIds.add(new ObjectId(o.asText("")));

							ObjectId objectIdtncn = null;
							objectIdtncn = new ObjectId(o.asText(""));

							UpdateOptions updateOptions = new UpdateOptions();
							updateOptions.upsert(false);

							Document docFindid = new Document("IsDelete", new Document("$ne", true))
									.append("_id", objectIdtncn).append("IssuerId", header.getIssuerId())
									.append("SignStatus", "NOSIGN");

					
							 mongoClient = cfg.mongoClient();
							 collection = mongoClient.getDatabase(cfg.dbName).getCollection("ChungTuTNCN");
							 try {
									docTmp =   collection.find(docFindid).allowDiskUse(true).iterator().next();			
							} catch (Exception e) {
								// TODO: handle exception
							}
							mongoClient.close();
							
							if (null == docTmp) {
								responseStatus = new MspResponseStatus(9999, "Không tìm thấy thông tin chứng từ.");
								rsp.setResponseStatus(responseStatus);
								return rsp;
							}
							int SHD = docTmp.getEmbedded(Arrays.asList("SHDon"), 0);
							String MSKH = docTmp.getEmbedded(Arrays.asList("MauSoHD"), "");
							try {
								objectIdtncn = new ObjectId(MSKH);
							} catch (Exception e) {
							}
							if (SHD == 0) {
								mongoClient = cfg.mongoClient();
								collection = mongoClient.getDatabase(cfg.dbName).getCollection("ChungTuTNCN");
								collection.updateMany(docFindid,
										new Document("$set", new Document("IsDelete", true).append("InfoDeleted",
												new Document("DeletedDate", LocalDateTime.now())
														.append("DeletedUserID", header.getUserId())
														.append("DeletedUserName", header.getUserName())
														.append("DeletedUserFullName", header.getUserFullName()))),
										updateOptions);		
								mongoClient.close();
								
								
							} else {

								mongoClient = cfg.mongoClient();
								collection = mongoClient.getDatabase(cfg.dbName).getCollection("ChungTuTNCN");
								collection.updateMany(docFindid,
										new Document("$set", new Document("IsDelete", true).append("InfoDeleted",
												new Document("DeletedDate", LocalDateTime.now())
														.append("DeletedUserID", header.getUserId())
														.append("DeletedUserName", header.getUserName())
														.append("DeletedUserFullName", header.getUserFullName()))),
										updateOptions);			
								mongoClient.close();
								
								
								docFind = new Document("IsDelete", new Document("$ne", true)).append("_id",
										objectIdtncn);
							
								
								 mongoClient = cfg.mongoClient();
								 collection = mongoClient.getDatabase(cfg.dbName).getCollection("DMMSTNCN");
								 try {
										docTmp =   collection.find(docFind).allowDiskUse(true).iterator().next();			
								} catch (Exception e) {
									// TODO: handle exception
								}
								mongoClient.close();
								
								
								if (null == docTmp) {
									responseStatus = new MspResponseStatus(9999, "Không tìm thấy mẫu số chứng từ.");
									rsp.setResponseStatus(responseStatus);
									return rsp;
								}
								int SHDHT = docTmp.getEmbedded(Arrays.asList("SHDHT"), 0);
								int SHDCL = docTmp.getEmbedded(Arrays.asList("ConLai"), 0);

								if (SHD != SHDHT) {
									responseStatus = new MspResponseStatus(9999,
											"Chỉ có thể xóa số chứng từ lớn nhất!");
									rsp.setResponseStatus(responseStatus);
									return rsp;
								}
								SHDHT = SHDHT - 1;
								SHDCL = SHDCL + 1;

								options = new FindOneAndUpdateOptions();
								options.upsert(false);
								options.maxTime(5000, TimeUnit.MILLISECONDS);
								options.returnDocument(ReturnDocument.AFTER);

							
								mongoClient = cfg.mongoClient();
								collection = mongoClient.getDatabase(cfg.dbName).getCollection("DMMSTNCN");
								docR = collection.findOneAndUpdate(docFind,
										new Document("$set", new Document("SHDHT", SHDHT).append("ConLai", SHDCL)
												.append("InfoDeleted", new Document("DeletedDate", LocalDateTime.now())
														.append("DeletedUserID", header.getUserId())
														.append("DeletedUserName", header.getUserName())
														.append("DeletedUserFullName", header.getUserFullName()))),
										options);		
								mongoClient.close();
								

							}

						} catch (Exception e) {
						}
					}
				}
			} catch (Exception e) {
			}

			responseStatus = new MspResponseStatus(0, "SUCCESS");
			rsp.setResponseStatus(responseStatus);
			return rsp;

		case Constants.MSG_ACTION_CODE.XoaBo:
			objectId = null;
			try {
				objectId = new ObjectId(_id);
			} catch (Exception e) {
			}

			/* KIEM TRA THONG TIN HD CO TON TAI KHONG */
			docFind = new Document("IssuerId", header.getIssuerId()).append("IsDelete", new Document("$ne", true))
					.append("_id", objectId).append("Status", "COMPLETE");
			docTmp = null;
		
			 mongoClient = cfg.mongoClient();
			 collection = mongoClient.getDatabase(cfg.dbName).getCollection("ChungTuTNCN");
			 try {
					docTmp =   collection.find(docFind).allowDiskUse(true).iterator().next();			
			} catch (Exception e) {
				// TODO: handle exception
			}
			mongoClient.close();
			
			if (null == docTmp) {
				responseStatus = new MspResponseStatus(9999, "Không tìm thấy thông tin chứng từ.");
				rsp.setResponseStatus(responseStatus);
				return rsp;
			}

			// CAP NHAT HOA DON DA XOA TRONG EINVOICE
			options = new FindOneAndUpdateOptions();
			options.upsert(false);
			options.maxTime(5000, TimeUnit.MILLISECONDS);
			options.returnDocument(ReturnDocument.AFTER);

		
			mongoClient = cfg.mongoClient();
			collection = mongoClient.getDatabase(cfg.dbName).getCollection("ChungTuTNCN");
			docR = collection.findOneAndUpdate(docFind,
					new Document("$set", new Document("Status", "XOABO").append("InfoXoaBo",
							new Document("XoaBoDate", LocalDateTime.now()).append("XoaBoUserID", header.getUserId())
									.append("XoaBoName", header.getUserName())
									.append("XoaBoUserFullName", header.getUserFullName()))),
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

	/*
	 * db.getCollection('DMCustomer').find({ 'IssuerId': '61b851ebb0228bba71fca2ec',
	 * IsDelete: {$ne: true}, _id: ObjectId("61d166fde69f8a2eb2b01e43") });
	 */

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
		MongoCollection<Document> collection = mongoClient.getDatabase(cfg.dbName).getCollection("ChungTuTNCN");
		try {
			docTmp =   collection.find(docFind).allowDiskUse(true).iterator().next();		
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
	public FileInfo getFileForSign(JSONRoot jsonRoot) throws Exception {
		FileInfo fileInfo = new FileInfo();

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

		int currentYear = LocalDate.now().get(ChronoField.YEAR);
		ObjectId objectId = null;
		try {
			objectId = new ObjectId(_id);
		} catch (Exception e) {
		}

		/* NHO KIEM TRA XEM CO HD NAO DANG KY KHONG */
		FindOneAndUpdateOptions options = null;

		Document docFind = new Document("IssuerId", header.getIssuerId()).append("IsDelete", new Document("$ne", true))
				.append("_id", objectId).append("Status", new Document("$in", Arrays.asList("CREATED", "PENDING")))
				.append("SignStatus", "NOSIGN");

		List<Document> pipeline = new ArrayList<Document>();
		pipeline.add(new Document("$match", docFind));

		/* KIEM TRA THONG TIN MAU HD */
		pipeline.add(new Document("$lookup", new Document("from", "DMMSTNCN")
				.append("let", new Document("vMauSoHD", "$MauSoHD").append("vIssuerId", "$IssuerId"))
				.append("pipeline", Arrays.asList(new Document("$match", new Document("$expr", new Document("$and",
						Arrays.asList(new Document("$gt", Arrays.asList("$ConLai", 0)),
								new Document("$eq", Arrays.asList("$IsActive", true)),
								new Document("$ne", Arrays.asList("$IsDelete", true)),
								new Document("$eq", Arrays.asList(new Document("$toString", "$_id"), "$$vMauSoHD")),
								new Document("$eq", Arrays.asList("$IssuerId", "$$vIssuerId"))))))))
				.append("as", "DMMSTNCN")));
		pipeline.add(
				new Document("$unwind", new Document("path", "$DMMSTNCN").append("preserveNullAndEmptyArrays", true)));

		Document docTmp = null;
		
		MongoClient mongoClient = cfg.mongoClient();
		MongoCollection<Document> collection = mongoClient.getDatabase(cfg.dbName).getCollection("ChungTuTNCN");
		try {
			docTmp =   collection.aggregate(pipeline).allowDiskUse(true).iterator().next();		
		} catch (Exception e) {
			// TODO: handle exception
		}
			
		mongoClient.close();
		if (null == docTmp) {
			return fileInfo;
		}
		if (null == docTmp.get("DMMSTNCN")) {
			return fileInfo;
		}

		/* AP DUNG 1 FILE TRUOC */
		String dir = docTmp.get("Dir", "");
		String fileName = docTmp.get("FileNameXML", "");
		File file = new File(dir, fileName);

		if (!file.exists())
			return fileInfo;
		String idDMMSKH = docTmp.getEmbedded(Arrays.asList("DMMSTNCN", "_id"), ObjectId.class).toString();
		ObjectId objectIdMS = null;
		try {
			objectIdMS = new ObjectId(idDMMSKH);
		} catch (Exception e) {
		}
		/* TAO SO HD VA GHI DU LIEU VO FILE */
		int checkshd = docTmp.getEmbedded(Arrays.asList("SHDon"), 0);
		int checkshdht = docTmp.getEmbedded(Arrays.asList("DMMSTNCN", "SHDHT"), 0);
		int sl = docTmp.getEmbedded(Arrays.asList("DMMSTNCN", "SoLuong"), 0);
		int cl = docTmp.getEmbedded(Arrays.asList("DMMSTNCN", "ConLai"), 0);
		int checktontai = sl - cl;
		int ktshdht = 0;
///kiem tra chưa co bien nhung da co hoa don
//tim hoa don moi nhat
		if (checkshdht == 0 && checktontai > 0) {
			pipeline = null;
			Document docMatch = new Document("IssuerId", header.getIssuerId()).append("MauSoHD", idDMMSKH)
					.append("IsDelete", new Document("$ne", true));

			pipeline = new ArrayList<Document>();
			pipeline.add(new Document("$match", docMatch));
			pipeline.add(new Document("$addFields",
					new Document("SHDon", new Document("$ifNull", Arrays.asList("SHDon", Integer.MAX_VALUE)))));
			pipeline.add(new Document("$sort", new Document("MauSoHD", -1).append("SHDon", -1).append("_id", -1)));
			Document docTmp1 = null;
			
			
			 mongoClient = cfg.mongoClient();
			 collection = mongoClient.getDatabase(cfg.dbName).getCollection("ChungTuTNCN");
			try {
				docTmp1 =   collection.aggregate(pipeline).allowDiskUse(true).iterator().next();		
			} catch (Exception e) {
				// TODO: handle exception
			}
				
			mongoClient.close();
			
			ktshdht = docTmp1.getEmbedded(Arrays.asList("SHDon"), 0);
		}
/////////////////sau khi check lay ra shd lon nhat
		if (ktshdht > 0) {
			checkshdht = ktshdht;
		}

		int getSL = docTmp.getEmbedded(Arrays.asList("DMMSTNCN", "SoLuong"), 0);
		int eInvoiceNumber = 0;

		if (checkshd == 0) {
			eInvoiceNumber = checkshdht + 1;
		} else {
			eInvoiceNumber = checkshd;
		}
		int sht = checkshdht + 1;
		int CL = getSL - sht;

		/* DOC DU LIEU XML, VA GHI DU LIEU VO SO HD */
		org.w3c.dom.Document doc = commons.fileToDocument(file);
		XPath xPath = XPathFactory.newInstance().newXPath();
		Node nodeDLHDon = (Node) xPath.evaluate("/HDon/DLHDon[@Id='data']", doc, XPathConstants.NODE);
		Node nodeTmp = null;
		nodeTmp = (Node) xPath.evaluate("TTChung", nodeDLHDon, XPathConstants.NODE);

		Element elementSub = (Element) xPath.evaluate("SHDon", nodeTmp, XPathConstants.NODE);
		if (null == elementSub) {
			elementSub = doc.createElement("SHDon");
			elementSub.setTextContent(String.valueOf(eInvoiceNumber));
			nodeTmp.appendChild(elementSub);
		} else {
			elementSub.setTextContent(String.valueOf(eInvoiceNumber));
		}

		fileInfo.setFileName(fileName);
		fileInfo.setContentFile(commons.docW3cToByte(doc));

		/* UPDATE EINVOICE - STATUS */
		options = new FindOneAndUpdateOptions();
		options.upsert(false);
		options.maxTime(5000, TimeUnit.MILLISECONDS);
		options.returnDocument(ReturnDocument.AFTER);


		mongoClient = cfg.mongoClient();
		collection = mongoClient.getDatabase(cfg.dbName).getCollection("ChungTuTNCN");
		collection.findOneAndUpdate(docFind,
				new Document("$set", new Document("SHDon", eInvoiceNumber).append("Status", "PENDING")), options);	
		mongoClient.close();
		
		
		if (checkshd == 0) {
			Document docFindMS = null;
			docFindMS = new Document("IssuerId", header.getIssuerId()).append("IsDelete", new Document("$ne", true))
					.append("IsActive", true).append("_id", objectIdMS);

			
			mongoClient = cfg.mongoClient();
			collection = mongoClient.getDatabase(cfg.dbName).getCollection("DMMSTNCN");
			collection.findOneAndUpdate(docFindMS,
					new Document("$set", new Document("ConLai", CL).append("SHDHT", sht)), options);			
			mongoClient.close();
			
			
		}
		return fileInfo;
	}

	@Override
	public MsgRsp signSingle(InputStream is, JSONRoot jsonRoot) throws Exception {
		Msg msg = jsonRoot.getMsg();
		MsgHeader header = msg.getMsgHeader();
		MsgPage page = msg.getMsgPage();
		Object objData = msg.getObjData();

		MsgRsp rsp = new MsgRsp(header);
		rsp.setMsgPage(page);
		MspResponseStatus responseStatus = null;

		/* DOC NOI DUNG XML DA KY */
		org.w3c.dom.Document xmlDoc = commons.inputStreamToDocument(is, false);

		int eInvoiceNumber = 0;

		XPath xPath = XPathFactory.newInstance().newXPath();
		Node nodeDLHDon = (Node) xPath.evaluate("/HDon/DLHDon[@Id='data']", xmlDoc, XPathConstants.NODE);
		Node nodeTmp = null;
		nodeTmp = (Node) xPath.evaluate("TTChung", nodeDLHDon, XPathConstants.NODE);

		eInvoiceNumber = commons.stringToInteger(
				commons.getTextFromNodeXML((Element) xPath.evaluate("SHDon", nodeTmp, XPathConstants.NODE)));
		String keySystem = commons.getTextFromNodeXML((Element) xPath.evaluate("ID", nodeTmp, XPathConstants.NODE));

		String key = "";

		ObjectId objectId = null;
		try {
			objectId = new ObjectId(keySystem);
		} catch (Exception e) {
		}
		List<Document> pipeline = null;
		/* KIEM TRA THONG TIN HOP LE KHONG */
		Document docFind = new Document("IssuerId", header.getIssuerId()).append("SHDon", eInvoiceNumber)
				.append("Status", "PENDING").append("_id", objectId);

		pipeline = new ArrayList<Document>();
		pipeline.add(new Document("$match", docFind));
		pipeline.add(new Document("$lookup", new Document("from", "ChungTuTNCN")
				.append("let", new Document("vIssuerId", "$IssuerId").append("vMauSoHD", "$MauSoHD"))
				.append("pipeline", Arrays.asList(
						new Document("$match",
								new Document("$expr", new Document("$and",
										Arrays.asList(new Document("$eq", Arrays.asList("$IssuerId", "$$vIssuerId")),
												new Document("$eq", Arrays.asList("$MauSoHD", "$$vMauSoHD")),
												new Document("$ne", Arrays.asList("$IsDelete", true)),
												new Document("$in", Arrays.asList("$Status",
														Arrays.asList("COMPLETE", "ERROR_CQT", "PROCESSING", "XOABO",
																"DELETED", "REPLACED", "ADJUSTED"))))))),
						new Document("$group",
								new Document("_id", "$MauSoHD").append("SHDon", new Document("$max", "$SHDon")))))
				.append("as", "EInvoiceMAXCQT")));
		pipeline.add(new Document("$unwind",
				new Document("path", "$EInvoiceMAXCQT").append("preserveNullAndEmptyArrays", true)));
		Document docTmp = null;
	
		MongoClient mongoClient = cfg.mongoClient();
		MongoCollection<Document> collection = mongoClient.getDatabase(cfg.dbName).getCollection("ChungTuTNCN");
		try {
			docTmp =   collection.aggregate(pipeline).allowDiskUse(true).iterator().next();		
		} catch (Exception e) {
			// TODO: handle exception
		}
			
		mongoClient.close();
		

		if (null == docTmp) {
			responseStatus = new MspResponseStatus(9999, "Không tìm thấy thông tin hóa đơn.");
			rsp.setResponseStatus(responseStatus);
			return rsp;
		}

		int invoiceNumberCurrent = docTmp.getEmbedded(Arrays.asList("SHDon"), 0);
		int maxInvoiceSendedCQT = 0;
		if (docTmp.get("EInvoiceMAXCQT") != null)
			maxInvoiceSendedCQT = docTmp.getEmbedded(Arrays.asList("EInvoiceMAXCQT", "SHDon"), 0);
		if (invoiceNumberCurrent == 0 || invoiceNumberCurrent != maxInvoiceSendedCQT + 1) {
			responseStatus = new MspResponseStatus(9999,
					"Có 1 hoặc 1 vài số hóa đơn trước đó chưa được xử lý xong.<br>Vui lòng kiểm tra lại danh sách hóa đơn.");
			rsp.setResponseStatus(responseStatus);
			return rsp;
		}

		/* LUU FILE VA CAP NHAT TRANG THAI */
		String dir = docTmp.get("Dir", "");
		String fileName = keySystem + "_signed.xml";
		boolean check = commons.docW3cToFile(xmlDoc, dir, fileName);
		if (!check) {
			responseStatus = new MspResponseStatus(9999, "Lưu tập tin đã ký không thành công.");
			rsp.setResponseStatus(responseStatus);
			return rsp;
		}

		/* CAP NHAT DB */
		FindOneAndUpdateOptions options = new FindOneAndUpdateOptions();
		options.upsert(false);
		options.maxTime(5000, TimeUnit.MILLISECONDS);
		options.returnDocument(ReturnDocument.AFTER);

		objectId = null;
		try {
			objectId = new ObjectId(docTmp.getEmbedded(Arrays.asList("MauSoHD"), ""));
		} catch (Exception e) {
		}
		if (null == objectId) {
			throw new Exception("Không tìm thấy mẫu số hóa đơn.");
		}
		
		mongoClient = cfg.mongoClient();
		collection = mongoClient.getDatabase(cfg.dbName).getCollection("ChungTuTNCN");
		collection.findOneAndUpdate(docFind, new Document("$set",
				new Document("SignStatus", "SIGNED").append("Status", "COMPLETE").append("InfoSigned",
						new Document("SignedDate", LocalDateTime.now()).append("SignedUserID", header.getUserId())
								.append("SignedUserName", header.getUserName())
								.append("SignedUserFullName", header.getUserFullName()))),
				options);		
		mongoClient.close();
		
		
		responseStatus = new MspResponseStatus(0, Constants.MAP_ERROR.get(0));
		rsp.setResponseStatus(responseStatus);
		return rsp;
	}

	/*----------------------------------------------------- Start Import excel */

	@Transactional(rollbackFor = { Exception.class })
	@Override
	public MsgRsp importExcel(JSONRoot jsonRoot) throws Exception {
		Msg msg = jsonRoot.getMsg();
		MsgHeader header = msg.getMsgHeader();
		MsgPage page = msg.getMsgPage();
		Object objData = msg.getObjData();

		MsgRsp rsp = new MsgRsp(header);
		rsp.setMsgPage(page);
		MspResponseStatus responseStatus = null;

		// XML
		Document docTmp = null;
		DocumentBuilderFactory dbf = null;
		DocumentBuilder db = null;
		org.w3c.dom.Document doc = null;
		Element root = null;

		Element elementContent = null;

		Element elementSubTmp = null;
		Element elementSubTmp01 = null;
		Element elementSubContent = null;
		Element elementTmp = null;
		boolean isSdaveFile = false;
		int intTmp = 0;
		HashMap<String, Double> mapVATAmount = null;
		HashMap<String, Double> mapAmount = null;
		String tmp = "";
		HashMap<String, Object> hItem = null;
		// END XML

		JsonNode jsonData = null;
		if (objData != null) {
			jsonData = Json.serializer().nodeFromObject(msg.getObjData());
		} else {
			throw new Exception("Lỗi dữ liệu đầu vào");
		}

		String dataFileName = commons.getTextJsonNode(jsonData.at("/DataFileName")).replaceAll("\\s", "");
		// String mauSoHdon =
		// commons.getTextJsonNode(jsonData.at("/MauSoHdon")).replaceAll("\\s", "");
		// Start
		ObjectId objectId = null;
		ObjectId objectIdUser = null;
		List<Document> pipeline = null;
		Iterable<Document> cursor = null;
		Iterator<Document> iter = null;

		//
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
		pipeline.add(new Document("$match",
				new Document("_id", objectId).append("IsActive", true).append("IsDelete", new Document("$ne", true))));
		pipeline.add(new Document("$lookup",
				new Document("from", "Users").append("pipeline", Arrays.asList(
						new Document("$match",
								new Document("IssuerId", header.getIssuerId()).append("_id", objectIdUser)
										.append("IsActive", true).append("IsDelete", new Document("$ne", true))),
						new Document("$project", new Document("_id", 1).append("UserName", 1).append("FullName", 1)),
						new Document("$limit", 1))).append("as", "UserInfo"))

		);

		
		
		MongoClient mongoClient = cfg.mongoClient();
		MongoCollection<Document> collection = mongoClient.getDatabase(cfg.dbName).getCollection("Issuer");
		try {
			docTmp =   collection.aggregate(pipeline).allowDiskUse(true).iterator().next();		
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

		/* END XU LY LAY ID CỦA MAU SO KI HIEU */

		Path path = Paths.get(SystemParams.DIR_TEMPORARY, header.getIssuerId(), dataFileName);
		File file = path.toFile();
		if (!(file.exists() && file.isFile())) {
			responseStatus = new MspResponseStatus(9999, "Tập tin import dữ liệu không tồn tại.");
			rsp.setResponseStatus(responseStatus);
			return rsp;
		}
		List<CT_TNCNExcelForm> ctTNCNExcelFormList = new ArrayList<>();
		Workbook wb = null;
		Sheet sheet = null;
		try {
			wb = WorkbookFactory.create(file);
			sheet = wb.getSheetAt(0);
			boolean skipHeader = true;
			for (Row row1 : sheet) {
				if (skipHeader) {
					skipHeader = false;
					continue;
				}
				
				Cell firstCell = row1.getCell(0, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
				if (firstCell != null) {
				    String cellValue = commons.getCellValueAsString(firstCell);
				    if ("END".equalsIgnoreCase(cellValue.trim())) {
				        break; 
				    }
				}
				
				List<Cell> cells = new ArrayList<Cell>();
				int lastColumn = Math.max(row1.getLastCellNum(), 17);

				for (int cn = 0; cn < lastColumn; cn++) {
					Cell c = row1.getCell(cn, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
					cells.add(c);
				}
				CT_TNCNExcelForm ctTNCNExcelForm = extractInfoFromCell(cells);
				ctTNCNExcelFormList.add(ctTNCNExcelForm);
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (wb != null) {
				try {
					wb.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}

		boolean checkMaCT = false;
		boolean checkNullMaCT = false;
		if (ctTNCNExcelFormList != null) {
			for (int tam = 0; tam < ctTNCNExcelFormList.size(); tam++) {
				if (ctTNCNExcelFormList.get(tam).getMaCT() == null) {
					checkNullMaCT = true;
				}
			}
			if (checkNullMaCT == true) {
				responseStatus = new MspResponseStatus(999,
						"Import không thành công. \r\n" + "Hãy kiểm tra lại file excel. \r\n"
								+ "Không được chứa các dòng thừa và phải chuẩn theo mẫu.");
				rsp.setResponseStatus(responseStatus);
				return rsp;
			}
			String tempMSTNV = "";
			String tempTenNV = "";
			String tempMaNV = "";
			String tempDChiNV = "";
			String tempCMND = "";
			String tempNgayCap = "";
			String tempNoiCap = "";
			String tempQuocTich = "";
			String tempCaNhanCuTru = "";
			String tempKyBaoCao = "";
			String tempTuNgay = "";
			String tempDenNgay = "";
			String tempKhoanThuNhap = "";
			Double tempKhoanDongBHBB = 0.0;
			Double tempTongTNChiuThuePhaiKT = 0.0;
			Double tempTongThuNhapTinhThue = 0.0;
			Double tempSoThueTNCNDaKT = 0.0;

			int i = 0;
			int start = 0;
			int end = 0;
			int dem = 0;

			/* DOC FILE EXCEL - GHI DU LIEU VO LIST */
			for (; i < ctTNCNExcelFormList.size();) {
				dem = 0;
				for (int j = i; j < ctTNCNExcelFormList.size(); j++) {
					if (ctTNCNExcelFormList.get(i).getMaCT() == ctTNCNExcelFormList.get(j).getMaCT()) {
						// Xu ly
						dem++;
						start = j + 1;
						tempMSTNV = ctTNCNExcelFormList.get(i).getMST();
						tempTenNV = ctTNCNExcelFormList.get(i).getTenNV();
						tempMaNV = ctTNCNExcelFormList.get(i).getMaNV();
						tempDChiNV = ctTNCNExcelFormList.get(i).getDiaChiNV();
						tempCMND = ctTNCNExcelFormList.get(i).getCMND();
						tempNgayCap = ctTNCNExcelFormList.get(i).getNgayCap();
						tempNoiCap = ctTNCNExcelFormList.get(i).getNoiCap();
						tempQuocTich = ctTNCNExcelFormList.get(i).getQuocTich();
						tempCaNhanCuTru = ctTNCNExcelFormList.get(i).getCaNhanCuTru();
						tempKyBaoCao = ctTNCNExcelFormList.get(i).getKyBaoCao();
						tempTuNgay = ctTNCNExcelFormList.get(i).getTuNgay();
						tempDenNgay = ctTNCNExcelFormList.get(i).getDenNgay();
						tempKhoanThuNhap = ctTNCNExcelFormList.get(i).getKhoanTN();
						tempKhoanDongBHBB = ctTNCNExcelFormList.get(i).getKhoanDongBHBB();
						tempTongTNChiuThuePhaiKT = ctTNCNExcelFormList.get(i).getTongTNChiuThuePhaiKT();
						tempTongThuNhapTinhThue = ctTNCNExcelFormList.get(i).getTongThuNhapTinhThue();
						tempSoThueTNCNDaKT = ctTNCNExcelFormList.get(i).getSoThueTNCNDaKT();

						end = j;
						if (ctTNCNExcelFormList.size() == j + 1) {
							checkMaCT = true;
						}
					} else {
						checkMaCT = true;
					}

				}
				if (dem == 1) {
					end = i;
					checkMaCT = true;
				}
				String MSTForm = tempMSTNV;
				String TenForm = tempTenNV;
				String MaForm = tempMaNV;
				String DCNVForm = tempDChiNV;
				String CMNDMForm = tempCMND;
				String NgayCapForm = tempNgayCap;
				String NoiCapForm = tempNoiCap;
				String QuocTichForm = tempQuocTich;
				String CaNhanCuTruMForm = tempCaNhanCuTru;
				String KyBaoCaoForm = tempKyBaoCao;
				String TuNgayForm = tempTuNgay;
				String DenNgayForm = tempDenNgay;
				String KhoangTNForm = tempKhoanThuNhap;
				Double KhoanDongBHBBForm = tempKhoanDongBHBB;
				Double TongTNChiuThuePhaiKTForm = tempTongTNChiuThuePhaiKT;
				Double TongThuNhapTinhThueForm = tempTongThuNhapTinhThue;
				Double SoThueTNCNDaKTForm = tempSoThueTNCNDaKT;

				String CuTru = "KCT";
				if (CaNhanCuTruMForm.equals("1")) {
					CuTru = "CCT";
				}
				
				String thangnv = "";

				String ngayt = "";
				String ngayd = "";
				String[] words;
				String nam = "";
				String thang = "";
				int number = 0;
				String[] words1;
				String nam1 = "";
				String thang1 = "";
				int number1 = 0;
				String ngayluu = "";

				try {
				if (TuNgayForm == null && DenNgayForm == null) {
					thangnv = "1,2,3,4,5,6,7,8,9,10,11,12";
					int currentYear = LocalDate.now().get(ChronoField.YEAR);
					ngayluu = String.valueOf(currentYear);
				} else {
				
				ngayt = TuNgayForm.toString();
				ngayd = DenNgayForm.toString();

				words = ngayt.split("/");
				nam = words[2];
				thang = words[1];
				number = Integer.parseInt(thang);

				words1 = ngayd.split("/");
				nam1 = words1[2];
				thang1 = words1[1];
				number1 = Integer.parseInt(thang1);

//				ngayluu = number + "-" + number1 + "/" + KyBaoCaoForm;
				ngayluu = number + "-" + number1 + "/" + nam1;
				words = TuNgayForm.split("/");
				nam = words[2];
				thang = words[1];
				int tuthang = Integer.parseInt(thang);

				words = DenNgayForm.split("/");
				nam = words[2];
				thang = words[1];
				int denthang = Integer.parseInt(thang);

				
					for (int d = tuthang; d <= denthang; d++) {
						if (d == denthang) {
							thangnv += d;
						} else {
							thangnv += d + ",";
						}

					}
				}
				}catch (Exception e) {
					responseStatus = new MspResponseStatus(999,
							"Import không thành công.");
					rsp.setResponseStatus(responseStatus);
					return rsp;
				}
				if (checkMaCT == true) {
					Document docFind = null;
					objectId = null;
					try {
						objectId = new ObjectId(header.getIssuerId());
					} catch (Exception e) {
					}
					docFind = new Document("_id", objectId).append("IsActive", true).append("IsDelete",
							new Document("$ne", true));
					pipeline = new ArrayList<Document>();
					pipeline.add(new Document("$match", docFind));
					pipeline.add(new Document("$lookup", new Document("from", "ChungTuTNCN")
							.append("let", new Document("vIssuerId", new Document("$toString", "$_id")))
							.append("pipeline", Arrays.asList(new Document("$match", new Document("$expr", new Document(
									"$and",
									Arrays.asList(new Document("$eq", Arrays.asList("$IssuerId", "$$vIssuerId")),
											new Document("$ne", Arrays.asList("$IsDelete", true)),
											new Document("$or", Arrays.asList(new Document("$eq", Arrays.asList("$Name",
													commons.regexEscapeForMongoQuery(TenForm)))))))

							)), new Document("$limit", 1))).append("as", "ChungTuTNCN")));
					pipeline.add(new Document("$unwind",
							new Document("path", "$ChungTuTNCN").append("preserveNullAndEmptyArrays", true)));

					pipeline.add(
							new Document("$lookup",
									new Document("from", "TNCNStaff")
											.append("pipeline",
													Arrays.asList(new Document("$match",
															new Document("IssuerId", header.getIssuerId())
																	.append("Name", TenForm))))
											.append("as", "TNCNStaff")));
					pipeline.add(new Document("$unwind",
							new Document("path", "$TNCNStaff").append("preserveNullAndEmptyArrays", true)));

					pipeline.add(new Document("$lookup", new Document("from", "DMMSTNCN")
							.append("pipeline",
									Arrays.asList(new Document("$match", new Document("IssuerId", header.getIssuerId())
											.append("IsDelete", new Document("$ne", true)).append("IsActive", true))))
							.append("as", "DMMSTNCN")));
					pipeline.add(new Document("$unwind",
							new Document("path", "$DMMSTNCN").append("preserveNullAndEmptyArrays", true)));

				
					
					 mongoClient = cfg.mongoClient();
					 collection = mongoClient.getDatabase(cfg.dbName).getCollection("Issuer");
					 try {
							docTmp =   collection.aggregate(pipeline).allowDiskUse(true).iterator().next();			
					} catch (Exception e) {
						// TODO: handle exception
					}
					mongoClient.close();

					if (null == docTmp) {
						responseStatus = new MspResponseStatus(9999, "Không tìm thấy thông tin nhân viên.");
						rsp.setResponseStatus(responseStatus);
						return rsp;
					}
					String mauso = docTmp.getEmbedded(Arrays.asList("DMMSTNCN", "_id"), ObjectId.class).toString();

					int k = i;
					// Thông tin CT
					String Dir = "";
					String pathDir = "";
					ObjectId objectIdTK = null;
					/* TAO FILE XML */
					objectIdTK = new ObjectId();
					path = Paths.get(SystemParams.DIR_E_INVOICE_CTTNCN,
							docTmp.getEmbedded(Arrays.asList("TaxCode"), ""));
					pathDir = path.toString();
					file = path.toFile();
					if (!file.exists())
						file.mkdirs();
					String kh = docTmp.getEmbedded(Arrays.asList("DMMSTNCN", "KyHieu"), "") + "/"
							+ docTmp.getEmbedded(Arrays.asList("DMMSTNCN", "Nam"), "") + "/"
							+ docTmp.getEmbedded(Arrays.asList("DMMSTNCN", "ChungTu"), "");

					/* TAO FILE XML */
					String fileNameXML = objectIdTK.toString() + ".xml";
					Dir = pathDir;
					// XML

					dbf = DocumentBuilderFactory.newInstance();
					db = dbf.newDocumentBuilder();
					doc = db.newDocument();
					doc.setXmlStandalone(true);

					root = doc.createElement("HDon");
					doc.appendChild(root);

					elementContent = doc.createElement("DLHDon");
					elementContent.setAttribute("Id", "data");
					root.appendChild(elementContent);

					/* THONG TIN CHUNG TO KHAI */
					elementSubContent = doc.createElement("TTChung");
					elementSubContent.appendChild(commons.createElementWithValue(doc, "PBan", "2.0.0"));
					elementSubContent.appendChild(commons.createElementWithValue(doc, "MSo",
							docTmp.getEmbedded(Arrays.asList("DMMSTNCN", "MauSo"), "")));
					elementSubContent.appendChild(commons.createElementWithValue(doc, "KyHieu", kh));
					elementSubContent.appendChild(commons.createElementWithValue(doc, "ID", objectIdTK.toString()));
					elementSubContent.appendChild(commons.createElementWithValue(doc, "SHDon", "")); // SE PHAT SINH KHI
																										// KY

					elementContent.appendChild(elementSubContent);

					/* TO CHUC TRA THU NHAP */
					elementSubContent = doc.createElement("TCTTN");
					elementSubContent.appendChild(
							commons.createElementWithValue(doc, "Name", docTmp.getEmbedded(Arrays.asList("Name"), "")));
					elementSubContent.appendChild(commons.createElementWithValue(doc, "TaxCode",
							docTmp.getEmbedded(Arrays.asList("TaxCode"), "")));
					elementSubContent.appendChild(commons.createElementWithValue(doc, "Address",
							docTmp.getEmbedded(Arrays.asList("Address"), "")));
					elementSubContent.appendChild(commons.createElementWithValue(doc, "Phone",
							docTmp.getEmbedded(Arrays.asList("Phone"), "")));
					elementContent.appendChild(elementSubContent);

					/* THONG TIN NOP THUE */
					elementSubContent = doc.createElement("TTNT");
					elementSubContent.appendChild(commons.createElementWithValue(doc, "Name", TenForm));
					elementSubContent.appendChild(commons.createElementWithValue(doc, "TaxCode", MSTForm));
					elementSubContent.appendChild(commons.createElementWithValue(doc, "QuocTich", QuocTichForm));
					elementSubContent.appendChild(commons.createElementWithValue(doc, "CuTru", CuTru));
					elementSubContent.appendChild(commons.createElementWithValue(doc, "Address", DCNVForm));
					elementSubContent.appendChild(commons.createElementWithValue(doc, "CCCD", CMNDMForm));
					elementSubContent.appendChild(commons.createElementWithValue(doc, "CCCDDATE", NgayCapForm));
					elementSubContent.appendChild(commons.createElementWithValue(doc, "CCCDADDRESS", NoiCapForm));
					elementContent.appendChild(elementSubContent);

					NumberFormat FormatNumber = NumberFormat.getInstance(new Locale("en", "US"));

					String TongTNChiuThuePhaiKT = FormatNumber.format(TongTNChiuThuePhaiKTForm);
					String KhoanDongBHBB = "";
					if (KhoanDongBHBBForm != null) {
						KhoanDongBHBB = FormatNumber.format(KhoanDongBHBBForm);
					}

					String TongThuNhapTinhThue = FormatNumber.format(TongThuNhapTinhThueForm);
					String SoThueTNCNDaKT = FormatNumber.format(SoThueTNCNDaKTForm);
					/* THONG TIN THUE THU NHAP CA NHAN KHAU TRU */
					elementSubContent = doc.createElement("TTTTNCNKT");
					elementSubContent.appendChild(commons.createElementWithValue(doc, "KhoanThu", KhoangTNForm));
					if (KhoanDongBHBB.equals("")) {
						elementSubContent.appendChild(commons.createElementWithValue(doc, "KhoanBH", ""));
					} else {
						elementSubContent
								.appendChild(commons.createElementWithValue(doc, "KhoanBH", "₫" + KhoanDongBHBB));
					}

					elementSubContent.appendChild(commons.createElementWithValue(doc, "ThoiDiemTraTNMonth", thangnv));
					elementSubContent
							.appendChild(commons.createElementWithValue(doc, "ThoiDiemTraTNYear", KyBaoCaoForm));
					elementSubContent.appendChild(
							commons.createElementWithValue(doc, "TongThuKhauTru", "₫" + TongTNChiuThuePhaiKT));
					elementSubContent
							.appendChild(commons.createElementWithValue(doc, "TongThuThue", "₫" + TongThuNhapTinhThue));
					elementSubContent
							.appendChild(commons.createElementWithValue(doc, "SoThueKhauTru", "₫" + SoThueTNCNDaKT));
					elementContent.appendChild(elementSubContent);

					// END
					isSdaveFile = commons.docW3cToFile(doc, pathDir, fileNameXML);
					if (!isSdaveFile) {
						throw new Exception("Lưu dữ liệu không thành công.");
					}
					/* END - TAO XML HOA DON */

					String secureKey = commons.csRandomNumbericString(6);

					if (KhoanDongBHBB.equals("")) {

						docUpsert = new Document("_id", objectIdTK).append("IssuerId", header.getIssuerId())
								.append("SecureKey", secureKey).append("MauSoHD", mauso).append("Name", TenForm)
								.append("Code", MaForm).append("Address", DCNVForm).append("TaxCode", MSTForm)
								.append("CuTru", CuTru).append("KyHieu", kh)
								.append("CMND-CCCD",
										new Document("CCCD", CMNDMForm).append("CCCDDATE", NgayCapForm)
												.append("CCCDADDRESS", NoiCapForm).append("QuocTich", QuocTichForm))
								.append("TNCNKhauTru", new Document("KhoanThuNhap", KhoangTNForm)

										.append("KhoanBaoHiem", KhoanDongBHBB)
										.append("TongTNKhauTru", "₫" + TongTNChiuThuePhaiKT)
										.append("TongTNTinhThue", "₫" + TongThuNhapTinhThue)
										.append("SoTienCaNhanKhauTru", "₫" + SoThueTNCNDaKT))
								.append("KyBaoCao", KyBaoCaoForm).append("TuNgay", TuNgayForm)
								.append("DenNgay", DenNgayForm).append("DateSave", ngayluu).append("Dir", Dir)
								.append("FileNameXML", fileNameXML).append("IsActive", true).append("IsDelete", false)
								.append("SignStatus", Constants.INVOICE_SIGN_STATUS.NOSIGN)
								.append("Status", Constants.INVOICE_STATUS.CREATED)
								.append("Date", LocalDate.now().toString()).append("DateTime", LocalDate.now())
								.append("InfoCreated",
										new Document("CreateDate", LocalDateTime.now())
												.append("CreateUserID", header.getUserId())
												.append("CreateUserName", header.getUserName())
												.append("CreateUserFullName", header.getUserFullName()));

					
						mongoClient = cfg.mongoClient();
						collection = mongoClient.getDatabase(cfg.dbName).getCollection("ChungTuTNCN");
						collection.insertOne(docUpsert);			
						mongoClient.close();
						
						
					} else {
						docUpsert = new Document("_id", objectIdTK).append("IssuerId", header.getIssuerId())
								.append("SecureKey", secureKey).append("MauSoHD", mauso).append("Name", TenForm)
								.append("Code", MaForm).append("Address", DCNVForm).append("TaxCode", MSTForm)
								.append("CuTru", CuTru).append("KyHieu", kh)
								.append("CMND-CCCD",
										new Document("CCCD", CMNDMForm).append("CCCDDATE", NgayCapForm)
												.append("CCCDADDRESS", NoiCapForm).append("QuocTich", QuocTichForm))
								.append("TNCNKhauTru", new Document("KhoanThuNhap", KhoangTNForm)

										.append("KhoanBaoHiem", "₫" + KhoanDongBHBB)
										.append("TongTNKhauTru", "₫" + TongTNChiuThuePhaiKT)
										.append("TongTNTinhThue", "₫" + TongThuNhapTinhThue)
										.append("SoTienCaNhanKhauTru", "₫" + SoThueTNCNDaKT))
								.append("KyBaoCao", KyBaoCaoForm).append("TuNgay", TuNgayForm)
								.append("DenNgay", DenNgayForm).append("DateSave", ngayluu).append("Dir", Dir)
								.append("FileNameXML", fileNameXML).append("IsActive", true).append("IsDelete", false)
								.append("SignStatus", Constants.INVOICE_SIGN_STATUS.NOSIGN)
								.append("Status", Constants.INVOICE_STATUS.CREATED)
								.append("Date", LocalDate.now().toString()).append("DateTime", LocalDate.now())
								.append("InfoCreated",
										new Document("CreateDate", LocalDateTime.now())
												.append("CreateUserID", header.getUserId())
												.append("CreateUserName", header.getUserName())
												.append("CreateUserFullName", header.getUserFullName()));

						MongoClient mongoClient2 = cfg.mongoClient();
						collection = mongoClient2.getDatabase(cfg.dbName).getCollection("ChungTuTNCN");
						collection.insertOne(docUpsert);			
						mongoClient2.close();
						
					}
					checkMaCT = false;
				}
				i = start;
				if (dem == 0) {
					break;
				}

			}
			responseStatus = new MspResponseStatus(0, "Thêm thông tin thàng công.");
			rsp.setResponseStatus(responseStatus);

		} else {
			responseStatus = new MspResponseStatus(999, "Không thành công");
			rsp.setResponseStatus(responseStatus);
			return rsp;
		}
		return rsp;
	}

	/*----------------------------------------------------- End Import excel */

	private static CT_TNCNExcelForm extractInfoFromCell(List<Cell> cells) {
		CT_TNCNExcelForm ctTNCNExcelForm = new CT_TNCNExcelForm();
		// Ma chung tu
		Cell MaCT = cells.get(0);
		if (MaCT != null) {
			switch (MaCT.getCellType()) {
			case STRING:
				ctTNCNExcelForm.setMaCT(MaCT.getStringCellValue());
				break;
			case NUMERIC:
				ctTNCNExcelForm.setMaCT((NumberToTextConverter.toText(MaCT.getNumericCellValue())));
				break;
			case BLANK:
				break;
			default:
				break;
			}
		}
		// Ma so thue
		Cell MST = cells.get(1);
		if (MST != null) {
			switch (MST.getCellType()) {
			case STRING:
				ctTNCNExcelForm.setMST(MST.getStringCellValue());
				break;
			case NUMERIC:
				ctTNCNExcelForm.setMST((NumberToTextConverter.toText(MST.getNumericCellValue())));
				break;
			case BLANK:
				break;
			default:
				break;
			}
		}
		// Ten nhan vien
		Cell Ten = cells.get(2);
		if (Ten != null) {
			switch (Ten.getCellType()) {
			case STRING:
				ctTNCNExcelForm.setTenNV(Ten.getStringCellValue());
				break;
			case NUMERIC:
				ctTNCNExcelForm.setTenNV((NumberToTextConverter.toText(Ten.getNumericCellValue())));
				break;
			case BLANK:
				break;
			default:
				break;
			}
		}
		// Ma nhan vien
		Cell MaNV = cells.get(3);
		if (MaNV != null) {
			switch (MaNV.getCellType()) {
			case STRING:
				ctTNCNExcelForm.setMaNV(MaNV.getStringCellValue());
				break;
			case NUMERIC:
				ctTNCNExcelForm.setMaNV((NumberToTextConverter.toText(MaNV.getNumericCellValue())));
				break;
			case BLANK:
				break;
			default:
				break;
			}
		}
		// Dia chi nhan vien
		Cell DiaChiNV = cells.get(4);
		if (DiaChiNV != null) {
			switch (DiaChiNV.getCellType()) {
			case STRING:
				ctTNCNExcelForm.setDiaChiNV(DiaChiNV.getStringCellValue());
				break;
			case NUMERIC:
				ctTNCNExcelForm.setDiaChiNV((NumberToTextConverter.toText(DiaChiNV.getNumericCellValue())));
				break;
			case BLANK:
				break;
			default:
				break;
			}

		}
		// CMND - CCCD
		Cell CMND = cells.get(5);
		if (CMND != null) {
			switch (CMND.getCellType()) {
			case STRING:
				ctTNCNExcelForm.setCMND(CMND.getStringCellValue());
				break;
			case NUMERIC:
				ctTNCNExcelForm.setCMND((NumberToTextConverter.toText(CMND.getNumericCellValue())));
				break;
			case BLANK:
				break;
			default:
				break;
			}

		}
		// Ngày cấp
		Cell NCap = cells.get(6);
		if (NCap != null) {
			switch (NCap.getCellType()) {
			case STRING:
				ctTNCNExcelForm.setNgayCap(NCap.getStringCellValue());
				break;
			case NUMERIC:
				ctTNCNExcelForm.setNgayCap((NumberToTextConverter.toText(NCap.getNumericCellValue())));
				break;
			case BLANK:
				break;
			default:
				break;
			}
		}
		// Nơi cấp
		Cell NoiCap = cells.get(7);
		if (NoiCap != null) {
			switch (NoiCap.getCellType()) {
			case STRING:
				ctTNCNExcelForm.setNoiCap(NoiCap.getStringCellValue());
				break;
			case NUMERIC:
				ctTNCNExcelForm.setNoiCap((NumberToTextConverter.toText(NoiCap.getNumericCellValue())));
				break;
			case BLANK:
				break;
			default:
				break;
			}
		}
		// Quốc tịch
		Cell QuocTich = cells.get(8);
		if (QuocTich != null) {
			switch (QuocTich.getCellType()) {
			case STRING:
				ctTNCNExcelForm.setQuocTich(QuocTich.getStringCellValue());
				break;
			case NUMERIC:
				ctTNCNExcelForm.setQuocTich((NumberToTextConverter.toText(QuocTich.getNumericCellValue())));
				break;
			case BLANK:
				break;
			default:
				break;
			}

		}
		// Cá nhân cư trú
		Cell CNCTru = cells.get(9);
		if (CNCTru != null) {
			switch (CNCTru.getCellType()) {
			case STRING:
				ctTNCNExcelForm.setCaNhanCuTru(CNCTru.getStringCellValue());
				break;
			case NUMERIC:
				ctTNCNExcelForm.setCaNhanCuTru((NumberToTextConverter.toText(CNCTru.getNumericCellValue())));
				break;
			case BLANK:
				break;
			default:
				break;
			}
		}
		// Kỳ báo cáo
		Cell KBCao = cells.get(10);
		if (KBCao != null) {
			switch (KBCao.getCellType()) {
			case STRING:
				ctTNCNExcelForm.setKyBaoCao(KBCao.getStringCellValue());
				break;
			case NUMERIC:
				ctTNCNExcelForm.setKyBaoCao((NumberToTextConverter.toText(KBCao.getNumericCellValue())));
				break;
			case BLANK:
				break;
			default:
				break;
			}

		}
		// Từ ngày
		Cell TuNgay = cells.get(11);
		if (TuNgay != null) {
			switch (TuNgay.getCellType()) {
			case STRING:
				ctTNCNExcelForm.setTuNgay(TuNgay.getStringCellValue());
				break;
			case NUMERIC:
				ctTNCNExcelForm.setTuNgay((NumberToTextConverter.toText(TuNgay.getNumericCellValue())));
				break;
			case BLANK:
				break;
			default:
				break;
			}
		}

		// Đến ngày
		Cell DenNgay = cells.get(12);
		if (DenNgay != null) {
			switch (DenNgay.getCellType()) {
			case STRING:
				ctTNCNExcelForm.setDenNgay(DenNgay.getStringCellValue());
				break;
			case NUMERIC:
				ctTNCNExcelForm.setDenNgay((NumberToTextConverter.toText(DenNgay.getNumericCellValue())));
				break;
			case BLANK:
				break;
			default:
				break;
			}
		}
		// Khoan thu nhap
		Cell KTNhap = cells.get(13);
		if (KTNhap != null) {
			switch (KTNhap.getCellType()) {
			case STRING:
				ctTNCNExcelForm.setKhoanTN(KTNhap.getStringCellValue());
				break;
			case NUMERIC:
				break;
			case BLANK:
				break;
			default:
				break;
			}
		}
		// Khoan dong bao hiem
		Cell KhoanDongBHBB = cells.get(14);
		if (KhoanDongBHBB != null && (KhoanDongBHBB.getCellType() == CellType.FORMULA)) {
			switch (KhoanDongBHBB.getCachedFormulaResultType()) {
			case STRING:
				ctTNCNExcelForm.setKhoanDongBHBB((Double.valueOf((String) KhoanDongBHBB.getStringCellValue())));
				break;
			case NUMERIC:
				ctTNCNExcelForm.setKhoanDongBHBB(KhoanDongBHBB.getNumericCellValue());
				break;
			case BLANK:
				break;
			default:
				break;
			}
		}
		if (KhoanDongBHBB != null) {
			switch (KhoanDongBHBB.getCellType()) {
			case STRING:
				ctTNCNExcelForm.setKhoanDongBHBB((Double.valueOf((String) KhoanDongBHBB.getStringCellValue())));
				break;
			case NUMERIC:
				ctTNCNExcelForm.setKhoanDongBHBB(KhoanDongBHBB.getNumericCellValue());
				break;
			case BLANK:
				break;
			default:
				break;
			}
		}

		// Tổng thu nhập chịu thuế phải KT
		Cell TongTNChiuthueKT = cells.get(15);
		if (TongTNChiuthueKT != null && (TongTNChiuthueKT.getCellType() == CellType.FORMULA)) {
			switch (TongTNChiuthueKT.getCachedFormulaResultType()) {
			case STRING:
				ctTNCNExcelForm
						.setTongTNChiuThuePhaiKT((Double.valueOf((String) TongTNChiuthueKT.getStringCellValue())));
				break;
			case NUMERIC:
				ctTNCNExcelForm.setTongTNChiuThuePhaiKT(TongTNChiuthueKT.getNumericCellValue());
				break;
			case BLANK:
				break;
			default:
				break;
			}
		}
		if (TongTNChiuthueKT != null) {
			switch (TongTNChiuthueKT.getCellType()) {
			case STRING:
				ctTNCNExcelForm
						.setTongTNChiuThuePhaiKT((Double.valueOf((String) TongTNChiuthueKT.getStringCellValue())));
				break;
			case NUMERIC:
				ctTNCNExcelForm.setTongTNChiuThuePhaiKT(TongTNChiuthueKT.getNumericCellValue());
				break;
			case BLANK:
				break;
			default:
				break;
			}
		}

		// Tổng thu nhập tính thuế

		Cell TongTNTThue = cells.get(16);
		if (TongTNTThue != null && (TongTNTThue.getCellType() == CellType.FORMULA)) {
			switch (TongTNTThue.getCachedFormulaResultType()) {
			case STRING:
				ctTNCNExcelForm.setTongThuNhapTinhThue((Double.valueOf((String) TongTNTThue.getStringCellValue())));
				break;
			case NUMERIC:
				ctTNCNExcelForm.setTongThuNhapTinhThue(TongTNTThue.getNumericCellValue());
				break;
			case BLANK:
				break;
			default:
				break;
			}
		}
		if (TongTNTThue != null) {
			switch (TongTNTThue.getCellType()) {
			case STRING:
				ctTNCNExcelForm.setTongThuNhapTinhThue((Double.valueOf((String) TongTNTThue.getStringCellValue())));
				break;
			case NUMERIC:
				ctTNCNExcelForm.setTongThuNhapTinhThue(TongTNTThue.getNumericCellValue());
				break;
			case BLANK:
				break;
			default:
				break;
			}
		}
		// Số thuế TNCN đã KT
		Cell SoThueTNCNDaKT = cells.get(17);
		if (SoThueTNCNDaKT != null && (SoThueTNCNDaKT.getCellType() == CellType.FORMULA)) {
			switch (SoThueTNCNDaKT.getCachedFormulaResultType()) {
			case STRING:
				ctTNCNExcelForm.setSoThueTNCNDaKT((Double.valueOf((String) SoThueTNCNDaKT.getStringCellValue())));
				break;
			case NUMERIC:
				ctTNCNExcelForm.setSoThueTNCNDaKT(SoThueTNCNDaKT.getNumericCellValue());
				break;
			case BLANK:
				break;
			default:
				break;
			}
		}
		if (SoThueTNCNDaKT != null) {
			switch (SoThueTNCNDaKT.getCellType()) {
			case STRING:
				ctTNCNExcelForm.setSoThueTNCNDaKT((Double.valueOf((String) SoThueTNCNDaKT.getStringCellValue())));
				break;
			case NUMERIC:
				ctTNCNExcelForm.setSoThueTNCNDaKT(SoThueTNCNDaKT.getNumericCellValue());
				break;
			case BLANK:
				break;
			default:
				break;
			}
		}
		// Tra ve danh sach
		return ctTNCNExcelForm;
	}

	@Override
	public FileInfo getFileForSignAll(JSONRoot jsonRoot) throws Exception {
		FileInfo fileInfo = new FileInfo();

		Msg msg = jsonRoot.getMsg();
		MsgHeader header = msg.getMsgHeader();
		MsgPage page = msg.getMsgPage();
		Object objData = msg.getObjData();
		Document docFind2 = null;
		JsonNode jsonData = null;
		List<GetXMLInfoXMLDTO> arrFileInfos = new ArrayList<>();
		if (objData != null) {
			jsonData = Json.serializer().nodeFromObject(msg.getObjData());
		} else {
			throw new Exception("Lỗi dữ liệu đầu vào");
		}
		int shd = 0;
		int shdky = 0;
		List<String> testList = new ArrayList<>();

		List<Long> billNumbers = new ArrayList<Long>();
		FindOneAndUpdateOptions options = null;
		int eInvoiceNumber = 0;
		Document docFind = null;
		Document docFind1 = null;
		int checkshd = 0;
		String IDMS = "";
		ObjectId objectIdMS = null;
		String IdMauSo = "";
		String SLHDon = commons.getTextJsonNode(jsonData.at("/soLuong")).replaceAll("\\s", "0");
		int Soluong = Integer.parseInt(SLHDon);
		// CHECK SL CON LAI DE KY HOA DON
		// int dem = ids.size();
		// END CHECK SL CON LAI

		for (JsonNode o : jsonData.at("/id")) {
			String _id = commons.getTextJsonNode(o);
			ObjectId objectId = null;
			try {
				objectId = new ObjectId(_id);
			} catch (Exception e) {
			}

			/* NHO KIEM TRA XEM CO HD NAO DANG KY KHONG */

			docFind = new Document("IssuerId", header.getIssuerId()).append("IsDelete", new Document("$ne", true))
					.append("_id", objectId).append("Status", new Document("$in", Arrays.asList("CREATED", "PENDING")))
					.append("SignStatus", new Document("$in", Arrays.asList("NOSIGN", "PROCESSING")));
			List<Document> pipeline = new ArrayList<Document>();
			pipeline.add(new Document("$match", docFind));
			/* KIEM TRA THONG TIN MAU HD */
			pipeline.add(new Document("$lookup", new Document("from", "DMMSTNCN")
					.append("let", new Document("vMauSoHD", "$MauSoHD").append("vIssuerId", "$IssuerId"))
					.append("pipeline", Arrays.asList(new Document("$match", new Document("$expr", new Document("$and",
							Arrays.asList(new Document("$gt", Arrays.asList("$ConLai", 0)),
									new Document("$eq", Arrays.asList("$IsActive", true)),
									new Document("$ne", Arrays.asList("$IsDelete", true)),
									new Document("$eq", Arrays.asList(new Document("$toString", "$_id"), "$$vMauSoHD")),
									new Document("$eq", Arrays.asList("$IssuerId", "$$vIssuerId"))))))))
					.append("as", "DMMSTNCN")));
			pipeline.add(new Document("$unwind",
					new Document("path", "$DMMSTNCN").append("preserveNullAndEmptyArrays", true)));

			Document docTmp = null;
		
			MongoClient mongoClient = cfg.mongoClient();
			MongoCollection<Document> collection = mongoClient.getDatabase(cfg.dbName).getCollection("ChungTuTNCN");
			try {
				docTmp =   collection.aggregate(pipeline).allowDiskUse(true).iterator().next();		
			} catch (Exception e) {
				// TODO: handle exception
			}
				
			mongoClient.close();
			
			if (null == docTmp) {
				fileInfo.setCheck("Not CT");
				return fileInfo;
			}
			if (null == docTmp.get("DMMSTNCN")) {
				fileInfo.setCheck("Not MS");
				return fileInfo;
			}

			IdMauSo = docTmp.getEmbedded(Arrays.asList("DMMSTNCN", "_id"), ObjectId.class).toString();
			try {
				objectIdMS = new ObjectId(IdMauSo);
			} catch (Exception e) {
			}
			if (IDMS == "") {
				IDMS = IdMauSo;
			}
			if (IDMS != "" && !IDMS.equals(IdMauSo)) {
				fileInfo.setFormIssueInvoiceID(IDMS);
				fileInfo.setCheck("error");
				return fileInfo;
			}

			int CheckSLConlai = docTmp.getEmbedded(Arrays.asList("DMMSTNCN", "ConLai"), 0);
			int checkSL = CheckSLConlai - Soluong;
			if (checkSL < 0) {
				fileInfo.setCheck("Not Enough");
				return fileInfo;
			}
		}

		for (JsonNode o : jsonData.at("/id")) {
			String _id = commons.getTextJsonNode(o);
			int currentYear = LocalDate.now().get(ChronoField.YEAR);
			shdky = 0;
			ObjectId objectId = null;
			try {
				objectId = new ObjectId(_id);
			} catch (Exception e) {
			}

			/* NHO KIEM TRA XEM CO HD NAO DANG KY KHONG */

			docFind = new Document("IssuerId", header.getIssuerId()).append("IsDelete", new Document("$ne", true))
					.append("_id", objectId).append("Status", new Document("$in", Arrays.asList("CREATED", "PENDING")))
					.append("SignStatus", new Document("$in", Arrays.asList("NOSIGN", "PROCESSING")));

			List<Document> pipeline = new ArrayList<Document>();
			pipeline.add(new Document("$match", docFind));
			/* KIEM TRA THONG TIN MAU HD */
			pipeline.add(new Document("$lookup", new Document("from", "DMMSTNCN")
					.append("let", new Document("vMauSoHD", "$MauSoHD").append("vIssuerId", "$IssuerId"))
					.append("pipeline", Arrays.asList(new Document("$match", new Document("$expr", new Document("$and",
							Arrays.asList(new Document("$gt", Arrays.asList("$ConLai", 0)),
									new Document("$eq", Arrays.asList("$IsActive", true)),
									new Document("$ne", Arrays.asList("$IsDelete", true)),
									new Document("$eq", Arrays.asList(new Document("$toString", "$_id"), "$$vMauSoHD")),
									new Document("$eq", Arrays.asList("$IssuerId", "$$vIssuerId"))))))))
					.append("as", "DMMSTNCN")));
			pipeline.add(new Document("$unwind",
					new Document("path", "$DMMSTNCN").append("preserveNullAndEmptyArrays", true)));

			Document docTmp = null;
			
			MongoClient mongoClient = cfg.mongoClient();
			MongoCollection<Document> collection = mongoClient.getDatabase(cfg.dbName).getCollection("ChungTuTNCN");
			try {
				docTmp =   collection.aggregate(pipeline).allowDiskUse(true).iterator().next();		
			} catch (Exception e) {
				// TODO: handle exception
			}
				
			mongoClient.close();
			
			
			if (null == docTmp) {
				return fileInfo;
			}
			docFind2 = new Document("IssuerId", header.getIssuerId()).append("IsDelete", new Document("$ne", true))
					.append("_id", objectIdMS);
			if (IDMS == "") {
				IDMS = IdMauSo;
			}
			if (IDMS != "" && !IDMS.equals(IdMauSo)) {
				fileInfo.setFormIssueInvoiceID(IDMS);
				fileInfo.setCheck("error");
				return fileInfo;
			}
			try {
				objectIdMS = new ObjectId(IdMauSo);
			} catch (Exception e) {
			}

			/* AP DUNG 1 FILE TRUOC */
			String dir = docTmp.get("Dir", "");
			String fileName = docTmp.get("FileNameXML", "");
			File file = new File(dir, fileName);

			if (!file.exists())
				return fileInfo;

			/* TAO SO HD VA GHI DU LIEU VO FILE */

			int shdcheck = docTmp.get("SHDon", 0);
			if (shdcheck > 0) {
				eInvoiceNumber = shdcheck;

			} else {
				eInvoiceNumber = docTmp.getEmbedded(Arrays.asList("DMMSTNCN", "SoLuong"), 0)
						- docTmp.getEmbedded(Arrays.asList("DMMSTNCN", "ConLai"), 0)
						+ docTmp.getEmbedded(Arrays.asList("DMMSTNCN", "TuSo"), 0);
				shdky += 1;
				// eInvoiceNumber = eInvoiceNumber + shd;
				// shd = shd +1 ;
			}
			checkshd = eInvoiceNumber;
			/* DOC DU LIEU XML, VA GHI DU LIEU VO SO HD */
			billNumbers.add((long) eInvoiceNumber);
			/* DOC DU LIEU XML, VA GHI DU LIEU VO SO HD */
			org.w3c.dom.Document doc = commons.fileToDocument(file);
			XPath xPath = XPathFactory.newInstance().newXPath();
			Node nodeDLHDon = (Node) xPath.evaluate("/HDon/DLHDon[@Id='data']", doc, XPathConstants.NODE);
			Node nodeTmp = null;
			nodeTmp = (Node) xPath.evaluate("TTChung", nodeDLHDon, XPathConstants.NODE);

			Element elementSub = (Element) xPath.evaluate("SHDon", nodeTmp, XPathConstants.NODE);
			if (null == elementSub) {
				elementSub = doc.createElement("SHDon");
				elementSub.setTextContent(String.valueOf(eInvoiceNumber));
				nodeTmp.appendChild(elementSub);
			} else {
				elementSub.setTextContent(String.valueOf(eInvoiceNumber));
			}
			fileInfo.setFileName(fileName);
			fileInfo.setContentFile(commons.docW3cToByte(doc));

			GetXMLInfoXMLDTO getXMLInfoXMLDTO = null;
			getXMLInfoXMLDTO = new GetXMLInfoXMLDTO();
			getXMLInfoXMLDTO.setFileName(file.getName());
			getXMLInfoXMLDTO.setFileData(commons.docW3cToByte(doc));
			getXMLInfoXMLDTO.setShd(eInvoiceNumber);
			getXMLInfoXMLDTO.setIDMSHDon(IdMauSo);
			arrFileInfos.add(getXMLInfoXMLDTO);

			/* UPDATE EINVOICE - STATUS */
			options = new FindOneAndUpdateOptions();
			options.upsert(false);
			options.maxTime(5000, TimeUnit.MILLISECONDS);
			options.returnDocument(ReturnDocument.AFTER);

			
			mongoClient = cfg.mongoClient();
			collection = mongoClient.getDatabase(cfg.dbName).getCollection("ChungTuTNCN");
			collection.findOneAndUpdate(docFind,
					new Document("$set", new Document("SHDon", eInvoiceNumber).append("Status", "PENDING")), options);		
			mongoClient.close();
			
			
			if (shdky != 0) {
				mongoClient = cfg.mongoClient();
				collection = mongoClient.getDatabase(cfg.dbName).getCollection("DMMSTNCN");
				collection.findOneAndUpdate(docFind2,
						new Document("$inc", new Document("ConLai", -1).append("SHDHT", +1)),
						options);			
				mongoClient.close();
				
			}

		}

		fileInfo.setCheck("");
		fileInfo.setFormIssueInvoiceID(IDMS);
		fileInfo.setNumbers(billNumbers);
		fileInfo.setArrFileInfos(arrFileInfos);
		return fileInfo;
	}

	@Override
	public Object signAll(UpdateSignedMultiBillReq input, JSONRoot jsonRoot) throws Exception {
		Msg msg = jsonRoot.getMsg();
		MsgHeader header = msg.getMsgHeader();
		MsgPage page = msg.getMsgPage();
		Object objData = msg.getObjData();
		MsgRsp rsp = new MsgRsp(header);
		rsp.setMsgPage(page);
		MspResponseStatus responseStatus = null;
		List<String> DsFile = new ArrayList<>();
		int count = 0;
		String hdError = "";
		String mauso = input.getFormIssueInvoiceID();
		// GIAI NEN FILE ZIP
		String folderTmp = commons.csRandomAlphaNumbericString(15);
		String taxCode = input.getTaxcode();

		// CHECK USER CON
		String[] split_mst = taxCode.split("_");
		int dem_mst = split_mst.length;
		if (dem_mst == 2) {
			taxCode = split_mst[0];
		} else {
			taxCode = taxCode;
		}
		// END CHECK USER CON

		Path path = Paths.get(SystemParams.DIR_E_INVOICE_CTTNCN, taxCode);
		String urlPath = path.toString();
		File file = path.toFile();
		if (!file.exists())
			file.mkdirs();
		byte[] buffer = new byte[1024];
		ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(input.getFileData()));
		ZipEntry ze = zis.getNextEntry();
		while (ze != null) {
			if (!ze.isDirectory()) {
				String tenfileky = ze.getName().replaceAll("\\.xml", "");
				File newFile = new File(urlPath, tenfileky + "_signed.xml");
				DsFile.add(tenfileky);
				new File(newFile.getParent()).mkdirs();
				FileOutputStream fos = new FileOutputStream(newFile);
				int len;
				while ((len = zis.read(buffer)) > 0) {
					fos.write(buffer, 0, len);
				}
				fos.flush();
				fos.close();
			}
			ze = zis.getNextEntry();
		}
		zis.close();

		for (int k = 0; k < DsFile.size(); k++) {
			String fileName = DsFile.get(k) + "_signed.xml";
			File fileKy = new File(urlPath, fileName);
			if (!fileKy.exists() || !fileKy.isFile()) {
				return new FileInfo();
			}

			org.w3c.dom.Document xmlDoc = commons.fileToDocument(fileKy);
			int eInvoiceNumber = 0;

			XPath xPath = XPathFactory.newInstance().newXPath();
			Node nodeDLHDon = (Node) xPath.evaluate("/HDon/DLHDon[@Id='data']", xmlDoc, XPathConstants.NODE);
			Node nodeTmp = null;
			nodeTmp = (Node) xPath.evaluate("TTChung", nodeDLHDon, XPathConstants.NODE);

			eInvoiceNumber = commons.stringToInteger(
					commons.getTextFromNodeXML((Element) xPath.evaluate("SHDon", nodeTmp, XPathConstants.NODE)));
			String keySystem = "";

			keySystem = commons.getTextFromNodeXML((Element) xPath.evaluate("ID", nodeTmp, XPathConstants.NODE));

			/* LAY THONG TIN NGAY LAP - NGAY KY TRONG FILE XML */
			String NLap = commons.getTextFromNodeXML((Element) xPath.evaluate("NLap", nodeTmp, XPathConstants.NODE));
			String SigningTime = commons.getTextFromNodeXML((Element) xPath.evaluate(
					"/HDon/DSCKS/NBan/Signature/Object[@Id='SigningTime']/SignatureProperties/SignatureProperty/SigningTime",
					xmlDoc, XPathConstants.NODE));

			LocalDate ldNLap = null;
			LocalDate ldSigningTime = null;
			try {
				ldNLap = commons.convertStringToLocalDate(NLap, "yyyy-MM-dd");
			} catch (Exception e) {
			}
			if (SigningTime.length() > 10) {
				ldSigningTime = commons.convertStringToLocalDate(SigningTime.substring(0, 10), "yyyy-MM-dd");
			}
//			if (commons.compareLocalDate(ldNLap, ldSigningTime) != 0) {
//			count +=1;
//			hdError += eInvoiceNumber+",";
//			break;
//			}

			ObjectId objectId = null;
			try {
				objectId = new ObjectId(keySystem);
			} catch (Exception e) {
			}
			List<Document> pipeline = null;
			/* KIEM TRA THONG TIN HOP LE KHONG */
			Document docFind = new Document("IssuerId", header.getIssuerId()).append("SHDon", eInvoiceNumber)
					.append("Status", "PENDING").append("_id", objectId);

			pipeline = new ArrayList<Document>();
			pipeline.add(new Document("$match", docFind));
			pipeline.add(new Document("$lookup",
					new Document("from", "ChungTuTNCN").append("pipeline",
							Arrays.asList(new Document("$match",
									new Document("IssuerId", header.getIssuerId()).append("MauSoHD", mauso)
											.append("SHDon", eInvoiceNumber).append("IsActive", true)
											.append("IsDelete", new Document("$ne", true)))

							)).append("as", "CTCNTNInfo")));
			pipeline.add(new Document("$unwind",
					new Document("path", "$EInvoiceInfo").append("preserveNullAndEmptyArrays", true)));
			Document docTmp = null;
			
			MongoClient mongoClient = cfg.mongoClient();
			MongoCollection<Document> collection = mongoClient.getDatabase(cfg.dbName).getCollection("ChungTuTNCN");
			try {
				docTmp =   collection.aggregate(pipeline).allowDiskUse(true).iterator().next();		
			} catch (Exception e) {
				// TODO: handle exception
			}
				
			mongoClient.close();

			if (null == docTmp) {
				count += 1;
				hdError += eInvoiceNumber + ",";
				break;
			}
//			if(docTmp.get("CTCNTNInfo") != null) {
//				count +=1;
//				hdError += eInvoiceNumber+",";
//				break;
//			}

			String signStatusCode = docTmp.get("SignStatus", "");
			if ("PROCESSING".equals(signStatusCode)) {
				count += 1;
				hdError += eInvoiceNumber + ",";
				break;
			}

//			/* KIEM TRA NGAY LAP TRONG HE THONG - NGAY LAP TRONG XML */
//			LocalDate ldNLapSystem = commons.convertDateToLocalDate(
//					docTmp.getEmbedded(Arrays.asList("DateTime"), Date.class));
//			if (commons.compareLocalDate(ldNLap, ldNLapSystem) != 0) {
//				count +=1;
//				hdError += eInvoiceNumber+",";
//				break;
//			}

			/* CAP NHAT DB */
			FindOneAndUpdateOptions options = new FindOneAndUpdateOptions();
			options.upsert(false);
			options.maxTime(5000, TimeUnit.MILLISECONDS);
			options.returnDocument(ReturnDocument.AFTER);
			objectId = null;
			try {
				objectId = new ObjectId(docTmp.get("MauSoHD", ""));
			} catch (Exception e) {
			}
			if (null == objectId) {
				throw new Exception("Không tìm thấy mẫu số hóa đơn.");
			}

		
			mongoClient = cfg.mongoClient();
			collection = mongoClient.getDatabase(cfg.dbName).getCollection("ChungTuTNCN");
			collection.findOneAndUpdate(docFind, new Document("$set",
					new Document("Status", "COMPLETE").append("SignStatus", "SIGNED").append("InfoSigned",
							new Document("SignedDate", LocalDateTime.now()).append("SignedUserID", header.getUserId())
									.append("SignedUserName", header.getUserName())
									.append("SignedUserFullName", header.getUserFullName()))),
					options);		
			mongoClient.close();
			
			
		}

		if (count != 0) {
			responseStatus = new MspResponseStatus(9999, count + "Hóa đơn ký không thành công là :" + hdError);
			rsp.setResponseStatus(responseStatus);
			return rsp;
		} else {
			responseStatus = new MspResponseStatus(0, Constants.MAP_ERROR.get(0));
			rsp.setResponseStatus(responseStatus);
			return rsp;
		}

	}

}
