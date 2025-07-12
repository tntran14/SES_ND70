package vn.sesgroup.hddt.user.impl;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoField;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.DeliveryMode;
import javax.jms.Destination;
import javax.jms.Message;
import javax.jms.MessageProducer;
import javax.jms.Session;
import javax.jms.TextMessage;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import org.apache.commons.digester3.annotations.FromAnnotationsRuleModule;
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
import org.springframework.data.mongodb.core.query.Collation.CaseFirst;
import org.springframework.jms.core.JmsTemplate;
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
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.FindOneAndUpdateOptions;
import com.mongodb.client.model.ReturnDocument;
import com.mongodb.client.model.UpdateOptions;

import vn.sesgroup.hddt.configuration.ConfigConnectMongo;
import vn.sesgroup.hddt.dto.FileInfo;
import vn.sesgroup.hddt.dto.GetXMLInfoXMLDTO;
import vn.sesgroup.hddt.dto.MailConfig;
import vn.sesgroup.hddt.model.CT_TNCNExcelForm;
import vn.sesgroup.hddt.resources.JmsParams;
import vn.sesgroup.hddt.user.dao.AbstractDAO;
import vn.sesgroup.hddt.user.dao.CTTNCNDAO;
import vn.sesgroup.hddt.user.service.JPUtils;
import vn.sesgroup.hddt.user.service.TCTNService;
import vn.sesgroup.hddt.utility.Constants;
import vn.sesgroup.hddt.utility.Json;
import vn.sesgroup.hddt.utility.MailUtils;
import vn.sesgroup.hddt.utility.MailjetSender;
import vn.sesgroup.hddt.utility.SystemParams;
import vn.sesgroup.hddt.utility.UpdateSignedMultiBillReq;

@Repository
@Transactional
public class CTTNCNImpl extends AbstractDAO implements CTTNCNDAO {
	private static final Logger log = LogManager.getLogger(QLNVTNCNImpl.class);
	@Autowired
	ConfigConnectMongo cfg;
	@Autowired
	TCTNService tctnService;
	
	Document docUpsert = null;
	
	private MailjetSender mailJet = new MailjetSender();
	private MailUtils mailUtils = new MailUtils();
	@Autowired
	private JmsTemplate jmsTemplate;
	@Autowired
	JPUtils jpUtils;

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
				hItem.put("TNCNTime",extractFormTime(doc));
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
	
	private String extractFormTime(Document doc) {
		StringBuilder time = new StringBuilder();
		try {
			String truThang = Objects.toString(doc.get("TruThang"), "");
			String date = Objects.toString(doc.get("TuNgay"), "");
			int from = 0, to = 0;
			
			if (!date.trim().isEmpty()) {
                from = extractMonth(date);
            }

            date = Objects.toString(doc.get("DenNgay"), "");
            if (!date.trim().isEmpty()) {
                to = extractMonth(date);
            }

			if (from > 0 && to > 0) {
				if (truThang.trim().isEmpty()) {
					time.append(from).append("-").append(to).append("/")
							.append(Objects.toString(doc.get("KyBaoCao"), ""));
				} else {
					String[] truThangArray = truThang.trim().split(",");
					HashSet<Integer> excludedMonths = new HashSet<>();
					for (String month : truThangArray) {
						excludedMonths.add(Integer.parseInt(month.trim()));
					}
					for (int i = from; i <= to; i++) {
						if (!excludedMonths.contains(i)) {
							time.append(i).append(",");
						}
					}
					if (time.length() > 0 && time.charAt(time.length() - 1) == ',') {
						time.deleteCharAt(time.length() - 1);
						time.append("/").append(Objects.toString(doc.get("KyBaoCao"), ""));
					}
				}
			} else if (!truThang.trim().isEmpty()) {
				String[] truThangArray = truThang.trim().split(",");
				HashSet<Integer> excludedMonths = new HashSet<>();
				for (String month : truThangArray) {
					excludedMonths.add(Integer.parseInt(month.trim()));
				}
				for (int i = 1; i <= 12; i++) {
					if (!excludedMonths.contains(i)) {
						time.append(i).append(",");
					}
				}
				if (time.length() > 0 && time.charAt(time.length() - 1) == ',') {
					time.deleteCharAt(time.length() - 1);
					time.append("/").append(Objects.toString(doc.get("KyBaoCao"), ""));
				}
			} else {
				time.append(Objects.toString(doc.get("KyBaoCao"), ""));
			}
		} catch (Exception e) {
			System.out.println();
		}
		return time.toString();
	}
	
	private int extractMonth(String date) {
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
		LocalDate localDate = LocalDate.parse(date, formatter);
		return localDate.getMonthValue();
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
		String truthang = commons.getTextJsonNode(jsonData.at("/TruThang")).trim().replaceAll("\\s+", " ");
		String ktn = commons.getTextJsonNode(jsonData.at("/KhoanThuNhap")).trim().replaceAll("\\s+", " ");
		String tdtn = commons.getTextJsonNode(jsonData.at("/DateThuNhap")).trim().replaceAll("\\s+", " ");
		String kbh = commons.getTextJsonNode(jsonData.at("/KhoanBaoHiem")).trim().replaceAll("\\s+", " ");
		String ttnkt = commons.getTextJsonNode(jsonData.at("/TongTNKhauTru")).trim().replaceAll("\\s+", " ");
		String ttntt = commons.getTextJsonNode(jsonData.at("/TongTNTinhThue")).trim().replaceAll("\\s+", " ");
		String sttndkt = commons.getTextJsonNode(jsonData.at("/SoTienCaNhanKhauTru")).trim().replaceAll("\\s+", " ");
		String mauSoTNCN = commons.getTextJsonNode(jsonData.at("/MauSoTNCN")).trim().replaceAll("\\s+", " ");
		String sdtlh = commons.getTextJsonNode(jsonData.at("/ContactPhone")).trim().replaceAll("\\s+", " ");
		String email = commons.getTextJsonNode(jsonData.at("/ContactEmail")).trim().replaceAll("\\s+", " ");
		String emailcc = commons.getTextJsonNode(jsonData.at("/EmailCC")).trim().replaceAll("\\s+", " ");
		String kdttndkh = commons.getTextJsonNode(jsonData.at("/KhoanTuThienNhanDaoKhuyenHoc")).trim().replaceAll("\\s+", " ");
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
			String[] truThangArray = truthang.trim().split(",");
			HashSet<Integer> excludedMonths = new HashSet<>();
			for (String month : truThangArray) {
				if(!month.trim().isEmpty()) {
					excludedMonths.add(Integer.parseInt(month));					
				}
			}
			
			if (tungay.equals("") && denngay.equals("")) {
				if (!truthang.trim().isEmpty()) {
					for (int i = 1; i <= 12; i++) {
					    if (!excludedMonths.contains(i)) {
					    	thangnv += i + ",";
					    }
					}		
				} else {
					thangnv = "1,2,3,4,5,6,7,8,9,10,11,12";
				}
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
				    if (!excludedMonths.contains(i)) {
				        thangnv += i + ",";
				    }
				}
			}
			if (thangnv.endsWith(",")) {
			    thangnv = thangnv.substring(0, thangnv.length() - 1);
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
			elementSubContent.appendChild(commons.createElementWithValue(doc, "ContactPhone", sdtlh));
			elementSubContent.appendChild(commons.createElementWithValue(doc, "ContactEmail", email));
			elementSubContent.appendChild(commons.createElementWithValue(doc, "EmailCC", emailcc));
			elementSubContent.appendChild(commons.createElementWithValue(doc, "CCCD", cccd));
			elementSubContent.appendChild(commons.createElementWithValue(doc, "CCCDDATE", cccddate));
			elementSubContent.appendChild(commons.createElementWithValue(doc, "CCCDADDRESS", cccdaddress));
			elementContent.appendChild(elementSubContent);

			/* THONG TIN THUE THU NHAP CA NHAN KHAU TRU */
			elementSubContent = doc.createElement("TTTTNCNKT");
			elementSubContent.appendChild(commons.createElementWithValue(doc, "KhoanThu", ktn));
			elementSubContent.appendChild(commons.createElementWithValue(doc, "KhoanBH", kbh));
			elementSubContent.appendChild(commons.createElementWithValue(doc, "KhoanTuThienNhanDaoKhuyenHoc", kdttndkh));
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

			docUpsert = new Document("_id", objectIdTK)
					.append("IssuerId", header.getIssuerId())
					.append("SecureKey", secureKey)
					.append("MauSoHD", mauso)
					.append("Name", name)
					.append("Code", code)
					.append("Address", address)
					.append("ContactPhone", sdtlh)
					.append("ContactEmail", email)
					.append("EmailCC", emailcc)
					.append("TaxCode", taxcode)
					.append("CuTru", cutru)
					.append("KyHieu", kh)
					.append("CMND-CCCD",
							new Document("CCCD", cccd)
							.append("CCCDDATE", cccddate)
							.append("CCCDADDRESS", cccdaddress)
							.append("QuocTich", qt))
					.append("TNCNKhauTru", 
							new Document("KhoanThuNhap", ktn)
							.append("KhoanBaoHiem", kbh)
							.append("KhoanTuThienNhanDaoKhuyenHoc", kdttndkh)
							.append("TongTNKhauTru", ttnkt)
							.append("TongTNTinhThue", ttntt)
							.append("SoTienCaNhanKhauTru", sttndkt))
					.append("KyBaoCao", kibc)
					.append("TuNgay", tungay)
					.append("DenNgay", denngay)
					.append("TruThang", truthang)
					.append("DateSave", ngayluu)
					.append("Dir", dir)
					.append("FileNameXML", fileNameXML)
					.append("IsActive", true)
					.append("IsDelete", false)
					.append("SignStatus", Constants.INVOICE_SIGN_STATUS.NOSIGN)
					.append("Status", Constants.INVOICE_STATUS.CREATED)
					.append("Date", LocalDate.now().toString())
					.append("DateTime", LocalDate.now())
					.append("InfoCreated",
							new Document("CreateDate", LocalDateTime.now())
							.append("CreateUserID", header.getUserId())
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
			String[] truThangArray1 = truthang.trim().split(",");
			HashSet<Integer> excludedMonths1 = new HashSet<>();
			for (String month : truThangArray1) {
				if (!month.trim().isEmpty()) {
					excludedMonths1.add(Integer.parseInt(month));
				}
			}
			
			if (tungay.equals("") && denngay.equals("")) {
				if (!truthang.trim().isEmpty()) {
					for (int i = 1; i <= 12; i++) {
					    if (!excludedMonths1.contains(i)) {
					    	thangnv1 += i + ",";
					    }
					}		
				} else {
					thangnv = "1,2,3,4,5,6,7,8,9,10,11,12";
				}
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
				    if (!excludedMonths1.contains(i)) {
				    	thangnv1 += i + ",";
				    }
				}
			}
			if (thangnv1.endsWith(",")) {
				thangnv1 = thangnv1.substring(0, thangnv1.length() - 1);
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
			elementSubContent.appendChild(commons.createElementWithValue(doc, "ContactPhone", sdtlh));
			elementSubContent.appendChild(commons.createElementWithValue(doc, "ContactEmail", email));
			elementSubContent.appendChild(commons.createElementWithValue(doc, "EmailCC", emailcc));
			elementSubContent.appendChild(commons.createElementWithValue(doc, "CCCD", cccd));
			elementSubContent.appendChild(commons.createElementWithValue(doc, "CCCDDATE", cccddate));
			elementSubContent.appendChild(commons.createElementWithValue(doc, "CCCDADDRESS", cccdaddress));
			elementContent.appendChild(elementSubContent);

			/* THONG TIN THUE THU NHAP CA NHAN KHAU TRU */
			elementSubContent = doc.createElement("TTTTNCNKT");
			elementSubContent.appendChild(commons.createElementWithValue(doc, "KhoanThu", ktn));
			elementSubContent.appendChild(commons.createElementWithValue(doc, "KhoanBH", kbh));
			elementSubContent.appendChild(commons.createElementWithValue(doc, "KhoanTuThienNhanDaoKhuyenHoc", kdttndkh));
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
					new Document("$set",
							new Document("Name", name)
							.append("Code", code)
							.append("Address", address)
							.append("ContactPhone", sdtlh)
							.append("ContactEmail", email)
							.append("EmailCC", emailcc)
							.append("TaxCode", taxcode)
							.append("CuTru", cutru)
							.append("MauSoHD", mauso1)
							.append("CMND-CCCD",
									new Document("CCCD", cccd)
									.append("CCCDDATE", cccddate)
									.append("CCCDADDRESS", cccdaddress)
									.append("QuocTich", qt))
							.append("TNCNKhauTru", 
									new Document("KhoanThuNhap", ktn)
									.append("KhoanBaoHiem", kbh)
									.append("KhoanTuThienNhanDaoKhuyenHoc", kdttndkh)
									.append("TongTNKhauTru", ttnkt)
									.append("TongTNTinhThue", ttntt)
									.append("SoTienCaNhanKhauTru", sttndkt))
							.append("KyBaoCao", kibc)
							.append("TuNgay", tungay)
							.append("DenNgay", denngay)
							.append("TruThang", truthang)
							.append("TuNgay", tungay)
							.append("DenNgay", denngay)
							.append("DateSave", ngayluu)
							.append("Date", LocalDate.now().toString())
							.append("DateTime", LocalDate.now())
							.append("IsActive", true)
							.append("IsDelete", false)
							.append("InfoUpdated",
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
		 String mauSoTNCN = commons.getTextJsonNode(jsonData.at("/MauSoHdon")).replaceAll("\\s", "");
		// Start
		ObjectId objectId = null;
		ObjectId objectIdUser = null;
		ObjectId mstncnId = null;
		List<Document> pipeline = null;
		Iterable<Document> cursor = null;
		Iterator<Document> iter = null;

		try {
			objectId = new ObjectId(header.getIssuerId());
			mstncnId = new ObjectId(mauSoTNCN);
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
				int lastColumn = Math.max(row1.getLastCellNum(), 21);

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
			String tempSDTLH = "";
			String tempEmailLH = "";
			String tempEmailCC = "";
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
			Double tempKhoanDongTTNDKH = 0.0;
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
						tempSDTLH = ctTNCNExcelFormList.get(i).getSDTLienHe();
						tempEmailLH = ctTNCNExcelFormList.get(i).getEmailLienHe();
						tempEmailCC = ctTNCNExcelFormList.get(i).getEmailCC();
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
						tempKhoanDongTTNDKH = ctTNCNExcelFormList.get(i).getKhoanDongTTNDKH();
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
				String SDTLHForm = tempSDTLH;
				String EmailLHForm = tempEmailLH;
				String EmailCCForm = tempEmailCC;
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
				Double KhoanDongTTNDKHForm = tempKhoanDongTTNDKH;
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
									Arrays.asList(
										new Document("$match", new Document("IssuerId", header.getIssuerId())
											.append("IsDelete", new Document("$ne", true)).append("IsActive", true)
											.append("_id", mstncnId))))
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
					elementSubContent.appendChild(commons.createElementWithValue(doc, "ContactPhone", SDTLHForm));
					elementSubContent.appendChild(commons.createElementWithValue(doc, "ContactEmail", EmailLHForm));
					elementSubContent.appendChild(commons.createElementWithValue(doc, "EmailCC", EmailCCForm));
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
					String KhoanDongTTNDKH = "";
					if (KhoanDongTTNDKHForm != null) {
						KhoanDongTTNDKH = FormatNumber.format(KhoanDongTTNDKHForm);
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
					
					if (KhoanDongTTNDKH.equals("")) {
						elementSubContent.appendChild(commons.createElementWithValue(doc, "KhoanTTNDKH", ""));
					} else {
						elementSubContent
								.appendChild(commons.createElementWithValue(doc, "KhoanTTNDKH", "₫" + KhoanDongTTNDKH));
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
								.append("Code", MaForm)
								.append("Address", DCNVForm)
								.append("ContactPhone", SDTLHForm)
								.append("ContactEmail", EmailLHForm)
								.append("EmailCC", EmailCCForm)
								.append("TaxCode", MSTForm)
								.append("CuTru", CuTru).append("KyHieu", kh)
								.append("CMND-CCCD",
										new Document("CCCD", CMNDMForm)
										.append("CCCDDATE", NgayCapForm)
										.append("CCCDADDRESS", NoiCapForm)
										.append("QuocTich", QuocTichForm))
								.append("TNCNKhauTru",
										new Document("KhoanThuNhap", KhoangTNForm)
										.append("KhoanBaoHiem", KhoanDongBHBB)
										.append("KhoanTuThienNhanDaoKhuyenHoc", "₫" + KhoanDongTTNDKH)
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
								.append("Code", MaForm).append("Address", DCNVForm)
								.append("ContactPhone", SDTLHForm)
								.append("ContactEmail", EmailLHForm)
								.append("EmailCC", EmailCCForm)
								.append("TaxCode", MSTForm)
								.append("CuTru", CuTru).append("KyHieu", kh)
								.append("CMND-CCCD",
										new Document("CCCD", CMNDMForm).append("CCCDDATE", NgayCapForm)
												.append("CCCDADDRESS", NoiCapForm).append("QuocTich", QuocTichForm))
								.append("TNCNKhauTru", new Document("KhoanThuNhap", KhoangTNForm)

										.append("KhoanBaoHiem", "₫" + KhoanDongBHBB)
										.append("KhoanTuThienNhanDaoKhuyenHoc", "₫" + KhoanDongTTNDKH)
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
		
		// SDT lien he
		Cell sdtLienHe = cells.get(5);
		if (sdtLienHe != null) {
			switch (sdtLienHe.getCellType()) {
			case STRING:
				ctTNCNExcelForm.setSDTLienHe(sdtLienHe.getStringCellValue());
				break;
			case NUMERIC:
				ctTNCNExcelForm.setSDTLienHe((NumberToTextConverter.toText(sdtLienHe.getNumericCellValue())));
				break;
			case BLANK:
				break;
			default:
				break;
			}
		}
		// Email lien he
		Cell emailLienHe = cells.get(6);
		if (emailLienHe != null) {
			switch (emailLienHe.getCellType()) {
			case STRING:
				ctTNCNExcelForm.setEmailLienHe(emailLienHe.getStringCellValue());
				break;
			case NUMERIC:
				ctTNCNExcelForm.setEmailLienHe((NumberToTextConverter.toText(emailLienHe.getNumericCellValue())));
				break;
			case BLANK:
				break;
			default:
				break;
			}
		}
		
		// Email cc
		Cell emailCC = cells.get(7);
		if (emailCC != null) {
			switch (emailCC.getCellType()) {
			case STRING:
				ctTNCNExcelForm.setEmailCC(emailCC.getStringCellValue());
				break;
			case NUMERIC:
				ctTNCNExcelForm.setEmailCC((NumberToTextConverter.toText(emailCC.getNumericCellValue())));
				break;
			case BLANK:
				break;
			default:
				break;
			}
		}
				
		
		// CMND - CCCD
		Cell CMND = cells.get(8);
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
		Cell NCap = cells.get(9);
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
		Cell NoiCap = cells.get(10);
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
		Cell QuocTich = cells.get(11);
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
		Cell CNCTru = cells.get(12);
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
		Cell KBCao = cells.get(13);
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
		Cell TuNgay = cells.get(14);
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
		Cell DenNgay = cells.get(15);
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
		Cell KTNhap = cells.get(16);
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
		Cell KhoanDongBHBB = cells.get(17);
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

		// Khoan dong tu thien
		Cell KhoanDongTTNDKH = cells.get(18);
		if (KhoanDongTTNDKH != null && (KhoanDongTTNDKH.getCellType() == CellType.FORMULA)) {
			switch (KhoanDongTTNDKH.getCachedFormulaResultType()) {
			case STRING:
				ctTNCNExcelForm.setKhoanDongTTNDKH((Double.valueOf((String) KhoanDongTTNDKH.getStringCellValue())));
				break;
			case NUMERIC:
				ctTNCNExcelForm.setKhoanDongTTNDKH(KhoanDongTTNDKH.getNumericCellValue());
				break;
			case BLANK:
				break;
			default:
				break;
			}
		}
		if (KhoanDongTTNDKH != null) {
			switch (KhoanDongTTNDKH.getCellType()) {
			case STRING:
				ctTNCNExcelForm.setKhoanDongTTNDKH((Double.valueOf((String) KhoanDongTTNDKH.getStringCellValue())));
				break;
			case NUMERIC:
				ctTNCNExcelForm.setKhoanDongTTNDKH(KhoanDongTTNDKH.getNumericCellValue());
				break;
			case BLANK:
				break;
			default:
				break;
			}
		}
		// Tổng thu nhập chịu thuế phải KT
		Cell TongTNChiuthueKT = cells.get(19);
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

		Cell TongTNTThue = cells.get(20);
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
		Cell SoThueTNCNDaKT = cells.get(21);
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

	@Override
	public MsgRsp sendMail(JSONRoot jsonRoot) throws Exception {
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
		String _title = commons.getTextJsonNode(jsonData.at("/_title")).trim().replaceAll("\\s+", " ");
		String _email = commons.getTextJsonNode(jsonData.at("/_email")).trim().replaceAll("\\s+", " ");
		String _emailcc = commons.getTextJsonNode(jsonData.at("/_emailcc")).trim().replaceAll("\\s+", " ");
		String _content = commons.getTextJsonNode(jsonData.at("/_content")).trim().replaceAll("\\s+", " ");

		MsgRsp rsp = new MsgRsp(header);
		rsp.setMsgPage(page);
		MspResponseStatus responseStatus = null;

		ObjectId objectId = null;
		ObjectId objectIdIssu = null;
		try {
			objectId = new ObjectId(_id);
			objectIdIssu = new ObjectId(header.getIssuerId());
		} catch (Exception e) {
		}

		Document docFind = null;
		Document docTmp = null;
		List<Document> pipeline = null;

		try {
			docFind = new Document("IssuerId", header.getIssuerId()).append("_id", objectId);
			pipeline = new ArrayList<Document>();
			pipeline.add(new Document("$match", docFind));
			
			pipeline.add(
					new Document("$lookup",
							new Document("from", "Issuer")
									.append("pipeline",
											Arrays.asList(new Document("$match", new Document("_id", objectIdIssu)
													.append("IsDelete", new Document("$ne", true)))))
									.append("as", "Issuer")));
			pipeline.add(new Document("$unwind", new Document("path", "$Issuer").append("preserveNullAndEmptyArrays", true)));
			
			
			pipeline.add(new Document("$lookup", new Document("from", "ConfigEmail")
							.append("let", new Document("vIssuerId", "$IssuerId"))
							.append("pipeline", Arrays.asList(
												new Document("$match",new Document("$expr",
																		new Document("$eq", Arrays.asList("$IssuerId", "$$vIssuerId"))))))
							.append("as", "ConfigEmail")));
			pipeline.add(new Document("$unwind", new Document("path", "$ConfigEmail").append("preserveNullAndEmptyArrays", true)));
			
			
			pipeline.add(new Document("$lookup", new Document("from", "DMMSTNCN")
							.append("let",new Document("vIssuerId", "$IssuerId")
									.append("vMauSoHD","$MauSoHD"))
							.append("pipeline", Arrays.asList(
												new Document("$match", 
														new Document("$expr", 
																new Document("$and",Arrays.asList(
																		new Document("$eq", Arrays.asList("$$vIssuerId", "$IssuerId")),
																		new Document("$eq",Arrays.asList(new Document("$toString", "$_id"), "$$vMauSoHD")),
																		new Document("$eq", Arrays.asList("$IsDelete", false)),
							                                            new Document("$eq", Arrays.asList("$IsActive", true))
																		))))
											))
							.append("as", "DMMSTNCN")));
			pipeline.add(new Document("$unwind", new Document("path", "$DMMSTNCN").append("preserveNullAndEmptyArrays", true)));
			
			
			pipeline.add(new Document("$lookup", new Document("from", "UserConFig")
							.append("pipeline", Arrays.asList(
												new Document("$match",
															new Document("viewshd", "Y")
															.append("IssuerId", header.getIssuerId()))))
							.append("as", "UserConFig")));
			pipeline.add(new Document("$unwind", new Document("path", "$UserConFig").append("preserveNullAndEmptyArrays", true)));

			
			pipeline.add(new Document("$lookup", new Document("from", "ConfigMailJet")
							.append("pipeline", Arrays.asList(
									new Document("$match", 
											new Document("IsActive", true))))
							.append("as", "ConfigMailJet")));
			pipeline.add(new Document("$unwind", new Document("path", "$ConfigMailJet").append("preserveNullAndEmptyArrays", true)));

			
			pipeline.add(new Document("$lookup", new Document("from", "DMFooterWeb")
					.append("pipeline", Arrays.asList(
							new Document("$match", 
									new Document("IsActive", true)
									.append("IsDelete", false)),
							new Document("$project", new Document("Noidung", 1)),
							new Document("$limit", 1)))
					.append("as", "DMFooterWeb")));
			pipeline.add(new Document("$unwind", new Document("path", "$DMFooterWeb").append("preserveNullAndEmptyArrays", true)));
			
			pipeline.add(new Document("$lookup", new Document("from", "PramLink")
							.append("pipeline",Arrays.asList(
												new Document("$match",
														new Document("$expr", new Document("IsDelete", false))),
												new Document("$project", 
														new Document("_id", 1)
														.append("LinkPortal", 1))))
							.append("as", "PramLink")));
			pipeline.add(new Document("$unwind", new Document("path", "$PramLink").append("preserveNullAndEmptyArrays", true)));


			MongoClient mongoClient = cfg.mongoClient();
			MongoCollection<Document> collection = mongoClient.getDatabase(cfg.dbName).getCollection("ChungTuTNCN");
			try {
				docTmp = collection.aggregate(pipeline).allowDiskUse(true).iterator().next();
			} catch (Exception e) {
				
			}

			mongoClient.close();
		} catch (Exception e) {
			responseStatus = new MspResponseStatus(9999,
					"Cấu hình mail gửi hóa đơn không hợp lệ. Vui lòng cấu hình lại mail server!!!");
			rsp.setResponseStatus(responseStatus);
			return rsp;
		}

		if (null == docTmp) {
			responseStatus = new MspResponseStatus(9999, "Không tìm thấy thông tin hóa đơn.");
			rsp.setResponseStatus(responseStatus);
			return rsp;
		}
		String link = docTmp.getEmbedded(Arrays.asList("PramLink", "LinkPortal"), "");
		if (docTmp.get("ConfigEmail") == null) {
			responseStatus = new MspResponseStatus(9999, "Không tìm thấy thông tin email gửi.");
			rsp.setResponseStatus(responseStatus);
			return rsp;
		}

		String CheckFooterMail = docTmp.getEmbedded(Arrays.asList("UserConFig", "footermail"), "");
		if (!CheckFooterMail.equals("Y")) {
			_content = commons.decodeURIComponent(_content);
			String noidung = docTmp.getEmbedded(Arrays.asList("DMFooterWeb", "Noidung"), "");
			_content += noidung;
		} else {
			_content = commons.decodeURIComponent(_content);
		}

		String MailJet = docTmp.getEmbedded(Arrays.asList("ConfigEmail", "MailJet"), "");
		String dir = docTmp.getString("Dir");
		String statusCode = docTmp.get("SignStatus", "");
		String status = docTmp.get("Status", "");
		String soHD = commons.formatNumberBillInvoice(docTmp.get("SHDon", 0));
		String mauHD = docTmp.get("KyHieu", "");
		
		String fileNamePDF = _id + ".pdf";
		if (Constants.INVOICE_STATUS.DELETED.equals(status))
			fileNamePDF = _id + "-deleted.pdf";
		String fileName = _id + ".xml";
		String fileNameXML = _id + ".xml";
		File file = null;
		/* KIEM TRA XEM CO FILE PDF CHUA; NEU CHUA CO THI TAO FILE PDF */
		if (docTmp.get("DMMSTNCN") != null) {
			fileName = _id + ".xml";
			if ("SIGNED".equals(statusCode)) {
				fileName = _id + "_signed.xml";
			}
			
			file = new File(dir, fileName);
			if (file.exists() && file.isFile()) {
				String imgLogo = docTmp.getEmbedded(Arrays.asList("DMMSTNCN", "LoGo"), "");
				
				String kh = docTmp.get("KyHieu", "");
				String ms = docTmp.getEmbedded(Arrays.asList("DMMSTNCN", "MauSo"), "");

				org.w3c.dom.Document doc = commons.fileToDocument(file);
				
				String fileNameJP = docTmp.getEmbedded(Arrays.asList("DMMSTNCN", "FileName"), "");
				File fileJP = new File(SystemParams.DIR_E_INVOICE_TEMPLATE, fileNameJP);
				
				ByteArrayOutputStream baosPDF = null;
				baosPDF = jpUtils.viewpdfcttncn(fileJP, doc, docTmp,
						Paths.get(SystemParams.DIR_E_INVOICE_TEMPLATE, "MauSoTNCN",
								docTmp.getEmbedded(Arrays.asList("Issuer", "TaxCode"), ""), imgLogo).toString(),
						kh, ms, link, false, Constants.INVOICE_STATUS.XOABO.equals(status));
				
				if (null != baosPDF) {
					try (OutputStream fileOuputStream = new FileOutputStream(new File(dir, fileNamePDF))) {
						baosPDF.writeTo(fileOuputStream);
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}
		}
		/* END - KIEM TRA XEM CO FILE PDF CHUA; NEU CHUA CO THI TAO FILE PDF */
		List<String> listFiles = new ArrayList<>();
		List<String> listNames = new ArrayList<>();
		
		if ("SIGNED".equals(statusCode)) {
			fileNameXML = _id + "_signed.xml";
		}
		file = new File(dir, fileNameXML);
		if (file.exists() && file.isFile()) {
			listFiles.add(file.toString());
			listNames.add(mauHD + "-" + soHD + ".xml");
		}
		
		file = new File(dir, fileNamePDF);
		if (file.exists() && file.isFile()) {
			listFiles.add(file.toString());
			listNames.add(mauHD + "-" + soHD + ".pdf");
		}
		
		/* THUC HIEN GUI MAIL */
		MailConfig mailConfig = new MailConfig(docTmp.get("ConfigEmail", Document.class));
		mailConfig.setNameSend(docTmp.getEmbedded(Arrays.asList("InfoCreated", "CreateUserFullName"), ""));

		boolean boo = false;

		String email_gui = "";

		if (_email != "" || _emailcc != "") {
			if (_email != "" && _emailcc == "") {
				email_gui = _email;
			} else if (_email == "" && _emailcc != "") {
				email_gui = _emailcc;
			} else {
				email_gui = _email + "," + _emailcc;
			}
			
			// KIỂM TRA GỬI MAIL THƯỜNG HAY MAILJET
			if (MailJet.equals("Y") && !MailJet.equals("") && !MailJet.equals("N") && !email_gui.equals("")) {
				String ApiKey = docTmp.getEmbedded(Arrays.asList("ConfigMailJet", "ApiKey"), "");
				String SecretKey = docTmp.getEmbedded(Arrays.asList("ConfigMailJet", "SecretKey"), "");
				String EmailAddress = docTmp.getEmbedded(Arrays.asList("ConfigMailJet", "EmailAddress"), "");
				mailConfig.setEmailAddress(ApiKey);
				mailConfig.setEmailPassword(SecretKey);
				mailConfig.setSmtpServer(EmailAddress);
				boo = mailJet.sendMailJet(mailConfig, _title, _content, email_gui, listFiles, listNames, true);

			} else {
				boo = mailUtils.sendMail(mailConfig, _title, _content, email_gui, listFiles, listNames, true);
			}
			
			
			try {
				MongoClient mongoClient = cfg.mongoClient();
				MongoCollection<Document> collection = mongoClient.getDatabase(cfg.dbName)
						.getCollection("LogEmailUser");
				collection.insertOne(new Document("IssuerId", header.getIssuerId()).append("Title", _title)
						.append("Email", email_gui).append("IsActive", boo).append("MailCheck", boo)
						.append("IsDelete", false).append("EmailContent", _content)

				);
				mongoClient.close();

				/* LOG BAO CAO THONG KE */
				String name = docTmp.get("Name", "");
				String code = docTmp.get("Code", "");
				String address = docTmp.get("Address", "");
				String contactPhone = docTmp.get("ContactPhone", "");
				String taxCode = docTmp.get("TaxCode", "");
				String kyHieu = docTmp.get("KyHieu", "");
				String cccd = docTmp.getEmbedded(Arrays.asList("CMND-CCCD", "CCCD"), "");
				String kyBaoCao = docTmp.get("KyBaoCao", "");
				String tuNgay = docTmp.get("TuNgay", "");
				String denNgay = docTmp.get("DenNgay", "");
				int shd = docTmp.get("SHDon", 0);
				String date = docTmp.get("Date", "");

				mongoClient = cfg.mongoClient();
				collection = mongoClient.getDatabase(cfg.dbName).getCollection("BaoCaoThongKeCTTNCN");
				collection.insertOne(
						new Document("IssuerId", header.getIssuerId())
						.append("Name", name)
						.append("Code", code)
						.append("Address", address)
						.append("ContactPhone", contactPhone)
						.append("TaxCode", taxCode)
						.append("KyHieu", kyHieu)
						.append("CCCD", cccd)
						.append("KyBaoCao", kyBaoCao)
						.append("TuNgay", tuNgay)
						.append("DenNgay", denNgay)
						.append("SHDon", shd)
						.append("EmailGuiCTTNCN", email_gui)
						.append("Date", date)
						.append("IsDelete", false));
				mongoClient.close();

			} catch (Exception ex) {
			}
			
			
		}
		responseStatus = new MspResponseStatus(0, "SUCCESS");
		rsp.setResponseStatus(responseStatus);
		return rsp;
	}

	@Override
	public MsgRsp sendMailAll(JSONRoot jsonRoot) throws Exception {
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
		
		List<String> ids = null;
		try {
			ids = Json.serializer().fromJson(commons.decodeBase64ToString(commons.getTextJsonNode(jsonData.at("/_ids")).replaceAll("\\s", "")),
					new TypeReference<List<String>>() {
					});
		} catch (Exception e) {
		}

		MsgRsp rsp = new MsgRsp(header);
		rsp.setMsgPage(page);
		MspResponseStatus responseStatus = null;

		// CHECK SERVER ACTIVE MQ
		boolean checkStatusMQ = false;
		ConnectionFactory connectionFactory = null;
		Connection connection = null;
		Session session = null;
		Destination destination = null;
		MessageProducer producer = null;

		try {
			connectionFactory = jmsTemplate.getConnectionFactory();
			connection = connectionFactory.createConnection();
			session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
			destination = session.createQueue(JmsParams.QUEUE_BULK_MAIL);
			producer = session.createProducer(destination);
			producer.setDeliveryMode(DeliveryMode.PERSISTENT); // LUU DU LIEU KHI RESTART ACTIVEMQ
			checkStatusMQ = true;
		} catch (Exception e) {
			System.out.println(e);
		}
		if (!checkStatusMQ) {
			responseStatus = new MspResponseStatus(9999, "Kết nối đến Server Send Mail không thành công.");
			rsp.setResponseStatus(responseStatus);
			return rsp;
		}
		// END CHECK ACTIVE MQ

		Document docFind = null;
		Document docTmp = null;
		List<Document> pipeline = null;
	

		String uidTmp = commons.convertLocalDateTimeToString(LocalDateTime.now(),
				Constants.FORMAT_DATE.FORMAT_DATE_TIME_YYYYMMDD_HHMMSS) + "-" + commons.csRandomAlphaNumbericString(10);
		String ApiKey = "";
		String SecretKey = "";
		String EmailAddress = "";
		String MailJet = "";
		String check_mail = "";
		MailConfig mailConfig = null;
		
		ObjectId objectIdIssu = null;
		try {
			objectIdIssu = new ObjectId(header.getIssuerId());
		} catch (Exception e) {
		}
		
		String TaxCode = header.getUserName();
		String Name = header.getUserFullName();
		
		for (String id : ids) {
			ObjectId objectId = null;
			try {
				objectId = new ObjectId(id);
			} catch (Exception e) {
				continue;
			}

			docFind = new Document("IssuerId", header.getIssuerId()).append("_id", objectId);
			pipeline = new ArrayList<Document>();
			pipeline.add(new Document("$match", docFind));
			
			pipeline.add(
					new Document("$lookup",
							new Document("from", "Issuer")
									.append("pipeline",
											Arrays.asList(new Document("$match", new Document("_id", objectIdIssu)
													.append("IsDelete", new Document("$ne", true)))))
									.append("as", "Issuer")));
			pipeline.add(new Document("$unwind", new Document("path", "$Issuer").append("preserveNullAndEmptyArrays", true)));
			
			
			pipeline.add(new Document("$lookup", new Document("from", "ConfigEmail")
							.append("let", new Document("vIssuerId", "$IssuerId"))
							.append("pipeline", Arrays.asList(
												new Document("$match",new Document("$expr",
																		new Document("$eq", Arrays.asList("$IssuerId", "$$vIssuerId"))))))
							.append("as", "ConfigEmail")));
			pipeline.add(new Document("$unwind", new Document("path", "$ConfigEmail").append("preserveNullAndEmptyArrays", true)));
			
			
			pipeline.add(new Document("$lookup", new Document("from", "DMMSTNCN")
							.append("let",new Document("vIssuerId", "$IssuerId")
									.append("vMauSoHD","$MauSoHD"))
							.append("pipeline", Arrays.asList(
												new Document("$match", 
														new Document("$expr", 
																new Document("$and",Arrays.asList(
																		new Document("$eq", Arrays.asList("$$vIssuerId", "$IssuerId")),
																		new Document("$eq",Arrays.asList(new Document("$toString", "$_id"), "$$vMauSoHD")),
																		new Document("$eq", Arrays.asList("$IsDelete", false)),
							                                            new Document("$eq", Arrays.asList("$IsActive", true))
																		))))
											))
							.append("as", "DMMSTNCN")));
			pipeline.add(new Document("$unwind", new Document("path", "$DMMSTNCN").append("preserveNullAndEmptyArrays", true)));
			
			
			pipeline.add(new Document("$lookup", new Document("from", "UserConFig")
							.append("pipeline", Arrays.asList(
												new Document("$match",
															new Document("viewshd", "Y")
															.append("IssuerId", header.getIssuerId()))))
							.append("as", "UserConFig")));
			pipeline.add(new Document("$unwind", new Document("path", "$UserConFig").append("preserveNullAndEmptyArrays", true)));

			
			pipeline.add(new Document("$lookup", new Document("from", "ConfigMailJet")
							.append("pipeline", Arrays.asList(
									new Document("$match", 
											new Document("IsActive", true))))
							.append("as", "ConfigMailJet")));
			pipeline.add(new Document("$unwind", new Document("path", "$ConfigMailJet").append("preserveNullAndEmptyArrays", true)));

			
			pipeline.add(new Document("$lookup", new Document("from", "DMFooterWeb")
					.append("pipeline", Arrays.asList(
							new Document("$match", 
									new Document("IsActive", true)
									.append("IsDelete", false)),
							new Document("$project", new Document("Noidung", 1)),
							new Document("$limit", 1)))
					.append("as", "DMFooterWeb")));
			pipeline.add(new Document("$unwind", new Document("path", "$DMFooterWeb").append("preserveNullAndEmptyArrays", true)));
			
			pipeline.add(new Document("$lookup", new Document("from", "PramLink")
							.append("pipeline",Arrays.asList(
												new Document("$match",
														new Document("$expr", new Document("IsDelete", false))),
												new Document("$project", 
														new Document("_id", 1)
														.append("LinkPortal", 1))))
							.append("as", "PramLink")));
			pipeline.add(new Document("$unwind", new Document("path", "$PramLink").append("preserveNullAndEmptyArrays", true)));


			MongoClient mongoClient = cfg.mongoClient();
			MongoCollection<Document> collection = mongoClient.getDatabase(cfg.dbName).getCollection("ChungTuTNCN");
			try {
				docTmp = collection.aggregate(pipeline).allowDiskUse(true).iterator().next();
			} catch (Exception e) {
				System.out.println(e);
			}

			mongoClient.close();
			if (null == docTmp) {
				continue;
			}

			if (docTmp.get("ConfigEmail") == null) {
				continue;
			}
			
			if(!commons.isValidEmailAddress(docTmp.get("ContactEmail", ""))) {
				continue;
			}

			MailJet = docTmp.getEmbedded(Arrays.asList("ConfigEmail", "MailJet"), "");
			mailConfig = new MailConfig(docTmp.get("ConfigEmail", Document.class));
			mailConfig.setNameSend(docTmp.getEmbedded(Arrays.asList("InfoCreated", "CreateUserFullName"), ""));
			ApiKey = docTmp.getEmbedded(Arrays.asList("ConfigMailJet", "ApiKey"), "");
			SecretKey = docTmp.getEmbedded(Arrays.asList("ConfigMailJet", "SecretKey"), "");
			EmailAddress = docTmp.getEmbedded(Arrays.asList("ConfigMailJet", "EmailAddress"), "");

			if (MailJet.equals("Y") && !MailJet.equals("") && !MailJet.equals("N")) {
				check_mail = "MailJet";
			} else {
				check_mail = "MailServer";
			}

			Document docInsert = new Document("IssuerId", header.getIssuerId())
					.append("InfoServerID", uidTmp)
					.append("MailUsingType", check_mail)
					.append("TaxCode", TaxCode)
					.append("Name", Name)
					.append("Data", docTmp)
					.append("FuncSend", Constants.SendMailFromFunc.Bulk)
					.append("InfoCreated",
							new Document("CreateDate", LocalDateTime.now())
							.append("CreateUserID", header.getUserId())
							.append("CreateUserName", header.getUserName())
							.append("CreateUserFullName", header.getUserFullName()));
			mongoClient = cfg.mongoClient();
			collection = mongoClient.getDatabase(cfg.dbName).getCollection("LogBulkEMail");
			collection.insertOne(docInsert);
			mongoClient.close();

		}

		if (!MailJet.equals("") && mailConfig != null) {
			if (check_mail.equals("MailJet")) {

				MongoClient mongoClient = cfg.mongoClient();
				MongoCollection<Document> collection = mongoClient.getDatabase(cfg.dbName).getCollection("LogBulkEMailInfoServer");
				collection.insertOne(
						new Document("IssuerId", header.getIssuerId())
						.append("InfoServerID", uidTmp)
						.append("MailUsingType", "MailJet")
						.append("MailjetInfo", 
								new Document("ApiKey", ApiKey)
								.append("SecretKey", SecretKey)
								.append("EmailAddress", EmailAddress)
								.append("NameSend", mailConfig.getNameSend())
						));
				mongoClient.close();

			} else {

				boolean IsAutoSend = mailConfig.isAutoSend();
				boolean IsSSL = mailConfig.isSSL();
				boolean IsTLS = mailConfig.isTLS();

				MongoClient mongoClient = cfg.mongoClient();
				MongoCollection<Document> collection = mongoClient.getDatabase(cfg.dbName).getCollection("LogBulkEMailInfoServer");
				collection.insertOne(
						new Document("IssuerId", header.getIssuerId())
						.append("InfoServerID", uidTmp)
						.append("MailUsingType", check_mail)
						.append("MailServer",
								new Document("SmtpServer", mailConfig.getSmtpServer())
										.append("Port", mailConfig.getSmtpPort())
										.append("EmailAddress", mailConfig.getEmailAddress())
										.append("PassWord", mailConfig.getEmailPassword())
										.append("NameSend", mailConfig.getNameSend())
										.append("IsAutoSend", IsAutoSend)
										.append("IsSSL", IsSSL).append("IsTLS", IsTLS))

				);
				mongoClient.close();
			}

			/* INSERT DATA TO QUEUE */
			try {
				TextMessage objectMessage = null;
				objectMessage = session.createTextMessage(uidTmp);
				objectMessage.setStringProperty("TYPE", "CTTNCN");
				producer.send((Message) objectMessage);
			} catch (Exception e) {
			}

		}
		responseStatus = new MspResponseStatus(0, "SUCCESS");
		rsp.setResponseStatus(responseStatus);
		return rsp;
	}

	@Override
	public MsgRsp crudV1(JSONRoot jsonRoot) throws Exception {
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
		String _id_tt_dc = commons.getTextJsonNode(jsonData.at("/_id_tt_dc")).replaceAll("\\s", "");
		String _tcctu = commons.getTextJsonNode(jsonData.at("/_tcctu")).trim().replaceAll("\\s+", " ");
		String ten = commons.getTextJsonNode(jsonData.at("/Ten"));
		String code = commons.getTextJsonNode(jsonData.at("/Code")).trim().replaceAll("\\s+", " ");
		String msthue = commons.getTextJsonNode(jsonData.at("/MSThue")).trim().replaceAll("\\s+", " ");

		String dchi = commons.getTextJsonNode(jsonData.at("/DChi"));
		String qtich = commons.getTextJsonNode(jsonData.at("/QTich")).trim().replaceAll("\\s+", " ");
		String ctru = commons.getTextJsonNode(jsonData.at("/CTru")).trim().replaceAll("\\s+", " ");
		String cccdan = commons.getTextJsonNode(jsonData.at("/CCCDan")).trim().replaceAll("\\s+", " ");
		String sdthoai = commons.getTextJsonNode(jsonData.at("/SDThoai")).trim().replaceAll("\\s+", " ");
		String dctdtu = commons.getTextJsonNode(jsonData.at("/DCTDTu")).trim().replaceAll("\\s+", " ");
		String dctdtucc = commons.getTextJsonNode(jsonData.at("/DCTDTuCC")).trim().replaceAll("\\s+", " ");

		String mscttncn = commons.getTextJsonNode(jsonData.at("/MSCTu")).trim().replaceAll("\\s+", " ");
		String nam = commons.getTextJsonNode(jsonData.at("/Nam")).trim().replaceAll("\\s+", " ");
		String tthang = commons.getTextJsonNode(jsonData.at("/TThang")).trim().replaceAll("\\s+", " ");
		String dthang = commons.getTextJsonNode(jsonData.at("/DThang")).trim().replaceAll("\\s+", " ");

		String ktnhap = commons.getTextJsonNode(jsonData.at("/KTNhap")).trim().replaceAll("\\s+", " ");
		String bhiem = commons.getTextJsonNode(jsonData.at("/BHiem")).trim().replaceAll("\\s+", " ");
		String tthien = commons.getTextJsonNode(jsonData.at("/TThien")).trim().replaceAll("\\s+", " ");
		String ttncthue = commons.getTextJsonNode(jsonData.at("/TTNCThue")).trim().replaceAll("\\s+", " ");
		String ttntthue = commons.getTextJsonNode(jsonData.at("/TTNTThue")).trim().replaceAll("\\s+", " ");
		String sthue = commons.getTextJsonNode(jsonData.at("/SThue")).trim().replaceAll("\\s+", " ");

		String tctu = "Chứng từ khấu trừ thuế TNCN";
		String msctu = "";
		String khctu = "";

		ObjectId objectId = null;
		ObjectId objectIdCT = null;
		ObjectId mstncnId = null;
		ObjectId objectIdTT_DC = null;
		
		Document docFind = null;
		Document docTmp = null;
		Document docUpdate = null;
		Document docCTuTTDC = null;
		Document docTTCTLQuan = null;

		String fileNameXML = "";
		String pathDir = "";
		Path path = null;
		File file = null;

		DocumentBuilderFactory dbf = null;
		DocumentBuilder db = null;
		org.w3c.dom.Document doc = null;
		Element root = null;
		Element elementSubContent = null;
		Element elementContent = null;
		Element elementTmp = null;
		XPath xPath = null;

		boolean isSdaveFile = false;

		List<Document> pipeline = null;
		FindOneAndUpdateOptions options = null;
		
		org.w3c.dom.Document rTCTN = null;
		String maKetQua = "";
		String moTaKetQua = "";
		String codeTTTNhan = "";
		String descTTTNhan = "";

		switch (actionCode) {
		case Constants.MSG_ACTION_CODE.CREATED:
			objectId = null;
			mstncnId = null;
			objectIdTT_DC = null;
			try {
				objectId = new ObjectId(header.getIssuerId());
				mstncnId = new ObjectId(mscttncn);
				if (!"".equals(_id_tt_dc))
					objectIdTT_DC = new ObjectId(_id_tt_dc);
			} catch (Exception e) {
			}
			docFind = new Document("_id", objectId).append("IsActive", true).append("IsDelete",
					new Document("$ne", true));
			pipeline = new ArrayList<Document>();
			pipeline.add(new Document("$match", docFind));
			pipeline.add(new Document("$lookup",
					new Document("from", "DMMSTNCN")
							.append("pipeline",
									Arrays.asList(new Document("$match", new Document("IssuerId", header.getIssuerId())
											.append("IsDelete", new Document("$ne", true)).append("IsActive", true)
											.append("_id", mstncnId))))
							.append("as", "DMMSTNCN")));
			pipeline.add(new Document("$unwind",
					new Document("path", "$DMMSTNCN").append("preserveNullAndEmptyArrays", true)));
			if (!"".equals(_id_tt_dc)) {
				pipeline.add(new Document("$lookup", new Document("from", "CTTNCNhan")
						.append("pipeline", Arrays.asList(new Document("$match", new Document("_id", objectIdTT_DC)
								.append("IssuerId", header.getIssuerId())
								.append("Status",
										new Document("$in", Arrays.asList(Constants.INVOICE_STATUS.COMPLETE,
												Constants.INVOICE_STATUS.ADJUSTED, Constants.INVOICE_STATUS.REPLACED)))
								)))
						.append("as", "CTTNCNhanTTDC")));
				pipeline.add(new Document("$unwind",
						new Document("path", "$CTTNCNhanTTDC").append("preserveNullAndEmptyArrays", true)));
			}
			
			try (MongoClient mongoClient = cfg.mongoClient()) {
				MongoCollection<Document> collection = mongoClient.getDatabase(cfg.dbName).getCollection("Issuer");
				docTmp = collection.aggregate(pipeline).allowDiskUse(true).iterator().next();
			} catch (Exception e) {
			}

			if (null == docTmp) {
				responseStatus = new MspResponseStatus(9999, "Không tìm thấy thông tin người dùng.");
				rsp.setResponseStatus(responseStatus);
				return rsp;
			}
			
			if (null == docTmp.get("DMMSTNCN")) {
				responseStatus = new MspResponseStatus(9999, "Không tìm thấy thông tin mẫu chứng từ.");
				rsp.setResponseStatus(responseStatus);
				return rsp;
			}
			if (!docTmp.getEmbedded(Arrays.asList("DMMSTNCN","Nam"), "").equals(nam)) {
				responseStatus = new MspResponseStatus(9999, "Năm (Thời điểm trả thu nhập) không trùng mẫu số ký hiệu TNCN.");
				rsp.setResponseStatus(responseStatus);
				return rsp;
			}
			
			docCTuTTDC = docTmp.get("CTTNCNhanTTDC", Document.class);
			
			msctu =  docTmp.getEmbedded(Arrays.asList("DMMSTNCN","MauSo"), "");
			khctu = docTmp.getEmbedded(Arrays.asList("DMMSTNCN","KyHieu"), "")+"/"+ String.valueOf(docTmp.getEmbedded(Arrays.asList("DMMSTNCN","Nam"), "")).substring(2)+docTmp.getEmbedded(Arrays.asList("DMMSTNCN","ChungTu"), "");
		
			/* TAO FILE XML */
			objectIdCT = new ObjectId();
			path = Paths.get(SystemParams.DIR_E_INVOICE_CTTNCN, docTmp.getEmbedded(Arrays.asList("TaxCode"), ""));

			pathDir = path.toString();
			file = path.toFile();
			if (!file.exists())
				file.mkdirs();

			/* TAO FILE XML */
			fileNameXML = objectIdCT.toString() + ".xml";

			dbf = DocumentBuilderFactory.newInstance();
			db = dbf.newDocumentBuilder();
			doc = db.newDocument();
			doc.setXmlStandalone(true);

			root = doc.createElement("CTu");
			doc.appendChild(root);

			elementContent = doc.createElement("DLCTu");
			elementContent.setAttribute("Id", "data");
			root.appendChild(elementContent);

			/* THONG TIN CHUNG TO KHAI */
			elementSubContent = doc.createElement("TTChung");
			elementSubContent.appendChild(commons.createElementWithValue(doc, "PBan", SystemParams.VERSION_XML_TNCN));
			elementSubContent.appendChild(commons.createElementWithValue(doc, "TCTu", tctu));
			elementSubContent.appendChild(commons.createElementWithValue(doc, "MSCTu", msctu));
			elementSubContent.appendChild(commons.createElementWithValue(doc, "KHCTu", khctu));
			elementSubContent.appendChild(commons.createElementWithValue(doc, "SCTu", ""));
			elementSubContent.appendChild(commons.createElementWithValue(doc, "NLap", LocalDate.now().toString()));
			elementSubContent.appendChild(commons.createElementWithValue(doc, "TTKhac", ""));
			
			if (docCTuTTDC != null) {
				elementTmp = doc.createElement("TTCTLQuan");
				elementTmp.appendChild(commons.createElementWithValue(doc, "TCCTu", _tcctu));
				elementTmp.appendChild(commons.createElementWithValue(doc, "LHCTLQuan", "1"));
				elementTmp.appendChild(commons.createElementWithValue(doc, "KHMSCTCLQuan", 
						docCTuTTDC.getEmbedded(Arrays.asList("MSCTu"), "")));
				elementTmp.appendChild(commons.createElementWithValue(doc, "KHCTCLQuan",
						docCTuTTDC.getEmbedded(Arrays.asList("KHCTu"), "")));
				elementTmp.appendChild(commons.createElementWithValue(doc, "SCTCLQuan",
						String.valueOf(docCTuTTDC.getEmbedded(Arrays.asList("SCTu"), 0))));
				elementTmp.appendChild(commons.createElementWithValue(doc, "NLCTCLQuan",
						commons.convertLocalDateTimeToString(
								commons.convertDateToLocalDateTime(
										docCTuTTDC.getEmbedded(Arrays.asList("NLap"), Date.class)),
								"yyyy-MM-dd")));
				elementTmp.appendChild(commons.createElementWithValue(doc, "GChu", ""));
				elementSubContent.appendChild(elementTmp);
			}
			elementContent.appendChild(elementSubContent);

			/* TO CHUC TRA THU NHAP */
			elementSubContent = doc.createElement("NDCTu");

			elementTmp = doc.createElement("TCTTNhap");
			elementTmp.appendChild(
					commons.createElementWithValue(doc, "Ten", docTmp.getEmbedded(Arrays.asList("Name"), "")));
			elementTmp.appendChild(
					commons.createElementWithValue(doc, "MST", docTmp.getEmbedded(Arrays.asList("TaxCode"), "")));
			elementTmp.appendChild(
					commons.createElementWithValue(doc, "DChi", docTmp.getEmbedded(Arrays.asList("Address"), "")));
			elementTmp.appendChild(
					commons.createElementWithValue(doc, "SDThoai", docTmp.getEmbedded(Arrays.asList("Phone"), "")));
			elementTmp.appendChild(commons.createElementWithValue(doc, "TTKhac", ""));
			elementSubContent.appendChild(elementTmp);

			elementTmp = doc.createElement("NNT");
			elementTmp.appendChild(commons.createElementWithValue(doc, "Ten", ten));
			elementTmp.appendChild(commons.createElementWithValue(doc, "MST", msthue));
			elementTmp.appendChild(commons.createElementWithValue(doc, "DChi", dchi));
			elementTmp.appendChild(commons.createElementWithValue(doc, "QTich", qtich));
			elementTmp.appendChild(commons.createElementWithValue(doc, "CNCTru", ctru));
			elementTmp.appendChild(commons.createElementWithValue(doc, "CCCDan", cccdan));
			elementTmp.appendChild(commons.createElementWithValue(doc, "SDThoai", sdthoai));
			elementTmp.appendChild(commons.createElementWithValue(doc, "DCTDTu", dctdtu));
			elementTmp.appendChild(commons.createElementWithValue(doc, "GChu", ""));
			elementTmp.appendChild(commons.createElementWithValue(doc, "TTKhac", ""));
			elementSubContent.appendChild(elementTmp);

			elementTmp = doc.createElement("TTNCNKTru");
			elementTmp.appendChild(commons.createElementWithValue(doc, "KTNhap", ktnhap));
			elementTmp.appendChild(commons.createElementWithValue(doc, "TThang", tthang));
			elementTmp.appendChild(commons.createElementWithValue(doc, "DThang", dthang));
			elementTmp.appendChild(commons.createElementWithValue(doc, "Nam", nam));
			elementTmp.appendChild(commons.createElementWithValue(doc, "BHiem", bhiem));
			elementTmp.appendChild(commons.createElementWithValue(doc, "TThien", tthien));
			elementTmp.appendChild(commons.createElementWithValue(doc, "TTNCThue", ttncthue));
			elementTmp.appendChild(commons.createElementWithValue(doc, "TTNTThue", ttntthue));
			elementTmp.appendChild(commons.createElementWithValue(doc, "SThue", sthue));
			elementSubContent.appendChild(elementTmp);

			elementContent.appendChild(elementSubContent);

			/* END - TAO FILE XML */
			isSdaveFile = commons.docW3cToFile(doc, pathDir, fileNameXML);
			if (!isSdaveFile) {
				throw new Exception("Lưu dữ liệu không thành công.");
			}
			String secureKey = commons.csRandomNumbericString(6);
			String MTDiep = SystemParams.MSTTCGP
					+ commons.csRandomAlphaNumbericString(46 - SystemParams.MSTTCGP.length()).toUpperCase();

			Document docInsert = new Document("_id", objectIdCT).append("IssuerId", header.getIssuerId())
					.append("MauSo", mscttncn)
					.append("SecureKey", secureKey).append("MTDiep", MTDiep).append("TCTu", tctu)
					.append("MSCTu", msctu)
					.append("KHCTu", khctu)
					.append("NLap", LocalDate.now())
					.append("TTCTLQuan",
							new Document("TCCTu", _tcctu)
							.append("_id", _id_tt_dc)
							.append("LHCTLQuan", "1")
							.append("KHMSCTCLQuan", docCTuTTDC.getEmbedded(Arrays.asList("MSCTu"), ""))
							.append("KHCTCLQuan", docCTuTTDC.getEmbedded(Arrays.asList("KHCTu"), ""))
							.append("SCTCLQuan", String.valueOf(docCTuTTDC.getEmbedded(Arrays.asList("SCTu"), 0)))
							.append("NLCTCLQuan", commons.convertLocalDateTimeToString(
									commons.convertDateToLocalDateTime(
											docCTuTTDC.getEmbedded(Arrays.asList("NLap"), Date.class)),
									"yyyy-MM-dd")))
					.append("TCTTNhap",
							new Document("Ten", docTmp.getEmbedded(Arrays.asList("Name"), ""))
									.append("MST", docTmp.getEmbedded(Arrays.asList("TaxCode"), ""))
									.append("DChi", docTmp.getEmbedded(Arrays.asList("Address"), ""))
									.append("SDThoai", docTmp.getEmbedded(Arrays.asList("Phone"), "")))
					.append("NNT",
							new Document("Ten", ten).append("Code", code).append("MST", msthue).append("DChi", dchi)
									.append("QTich", qtich).append("CTru", ctru).append("CCCDan", cccdan)
									.append("SDThoai", sdthoai).append("DCTDTu", dctdtu).append("DCTDTuCC", dctdtucc))
					.append("TTNCNKTru",
							new Document("KTNhap", ktnhap).append("Nam", nam).append("TThang", tthang)
									.append("DThang", dthang).append("BHiem", bhiem).append("TThien", tthien)
									.append("TTNCThue", ttncthue).append("TTNTThue", ttntthue).append("SThue", sthue))
					.append("Dir", pathDir).append("FileNameXML", fileNameXML).append("IsActive", true)
					.append("IsDelete", false).append("SignStatus", Constants.INVOICE_SIGN_STATUS.NOSIGN)
					.append("Status", Constants.INVOICE_STATUS.CREATED).append("InfoCreated",
							new Document("CreateDate", LocalDateTime.now()).append("CreateUserID", header.getUserId())
									.append("CreateUserName", header.getUserName())
									.append("CreateUserFullName", header.getUserFullName()));

			try (MongoClient mongoClient = cfg.mongoClient()) {
				MongoCollection<Document> collection = mongoClient.getDatabase(cfg.dbName).getCollection("CTTNCNhan");
				collection.insertOne(docInsert);
				if (objectIdTT_DC != null) {
					docFind = new Document("IssuerId", header.getIssuerId()).append("IsDelete", false)
							.append("_id", objectIdTT_DC)
							.append("Status", new Document("$in", Arrays.asList(Constants.INVOICE_STATUS.COMPLETE,
									Constants.INVOICE_STATUS.ADJUSTED, Constants.INVOICE_STATUS.REPLACED)));
					options = new FindOneAndUpdateOptions();
					options.upsert(true);
					options.maxTime(5000, TimeUnit.MILLISECONDS);
					options.returnDocument(ReturnDocument.AFTER);
					String status = _tcctu.equals("1") ? Constants.INVOICE_STATUS.REPLACED:Constants.INVOICE_STATUS.ADJUSTED;
					collection.findOneAndUpdate(docFind,
							new Document("$set", new Document("Status", status)), options);		
				}
			} catch (Exception e) {
				System.out.println(e);
			}

			responseStatus = new MspResponseStatus(0, "SUCCESS");
			rsp.setResponseStatus(responseStatus);
			return rsp;
			
		case Constants.MSG_ACTION_CODE.MODIFY:
			objectId = null;
			objectIdCT = null;
			mstncnId = null;
			objectIdTT_DC = null;
			try {
				objectId = new ObjectId(header.getIssuerId());
				objectIdCT = new ObjectId(_id);
				mstncnId = new ObjectId(mscttncn);
				if (!"".equals(_id_tt_dc))
					objectIdTT_DC = new ObjectId(_id_tt_dc);
			} catch (Exception e) {
			}
			docFind = new Document("_id", objectId).append("IsActive", true).append("IsDelete",
					new Document("$ne", true));
			pipeline = new ArrayList<Document>();
			pipeline.add(new Document("$match", docFind));
			pipeline.add(
					new Document("$lookup",
							new Document("from", "CTTNCNhan")
									.append("pipeline",
											Arrays.asList(
													new Document("$match",
															new Document("_id", objectIdCT).append("IsDelete",
																	new Document("$ne", true)))))
									.append("as", "CTTNCNhan")));
			pipeline.add(new Document("$unwind",
					new Document("path", "$CTTNCNhan").append("preserveNullAndEmptyArrays", true)));
			pipeline.add(new Document("$lookup",
					new Document("from", "DMMSTNCN")
							.append("pipeline",
									Arrays.asList(new Document("$match", new Document("IssuerId", header.getIssuerId())
											.append("IsDelete", new Document("$ne", true)).append("IsActive", true)
											.append("_id", mstncnId))))
							.append("as", "DMMSTNCN")));
			pipeline.add(new Document("$unwind",
					new Document("path", "$DMMSTNCN").append("preserveNullAndEmptyArrays", true)));
			try (MongoClient mongoClient = cfg.mongoClient()) {
				MongoCollection<Document> collection = mongoClient.getDatabase(cfg.dbName).getCollection("Issuer");
				docTmp = collection.aggregate(pipeline).allowDiskUse(true).iterator().next();
			} catch (Exception e) {
			}

			if (null == docTmp) {
				responseStatus = new MspResponseStatus(9999, "Không tìm thấy thông tin người dùng.");
				rsp.setResponseStatus(responseStatus);
				return rsp;
			}
			
			if (null == docTmp.get("CTTNCNhan")) {
				responseStatus = new MspResponseStatus(9999, "Không tìm thấy thông tin chứng từ.");
				rsp.setResponseStatus(responseStatus);
				return rsp;
			}
			
			if (null == docTmp.get("DMMSTNCN")) {
				responseStatus = new MspResponseStatus(9999, "Không tìm thấy thông tin mẫu chứng từ.");
				rsp.setResponseStatus(responseStatus);
				return rsp;
			}
			
			if (!docTmp.getEmbedded(Arrays.asList("DMMSTNCN","Nam"), "").equals(nam)) {
				responseStatus = new MspResponseStatus(9999, "Năm (Thời điểm trả thu nhập) không trùng mẫu số ký hiệu TNCN.");
				rsp.setResponseStatus(responseStatus);
				return rsp;
			}
			
			docTTCTLQuan = docTmp.getEmbedded(Arrays.asList("CTTNCNhan", "TTCTLQuan"),
					Document.class);
			
			msctu =  docTmp.getEmbedded(Arrays.asList("DMMSTNCN","MauSo"), "");
			khctu = docTmp.getEmbedded(Arrays.asList("DMMSTNCN","KyHieu"), "")+"/"+ String.valueOf(docTmp.getEmbedded(Arrays.asList("DMMSTNCN","Nam"), "")).substring(2)+docTmp.getEmbedded(Arrays.asList("DMMSTNCN","ChungTu"), "");
			
			/* TAO FILE XML */
			pathDir = docTmp.getEmbedded(Arrays.asList("CTTNCNhan","Dir"), "");
			fileNameXML = docTmp.getEmbedded(Arrays.asList("CTTNCNhan","FileNameXML"), "");

			dbf = DocumentBuilderFactory.newInstance();
			db = dbf.newDocumentBuilder();
			doc = db.newDocument();
			doc.setXmlStandalone(true);

			root = doc.createElement("CTu");
			doc.appendChild(root);

			elementContent = doc.createElement("DLCTu");
			elementContent.setAttribute("Id", "data");
			root.appendChild(elementContent);

			/* THONG TIN CHUNG TO KHAI */
			elementSubContent = doc.createElement("TTChung");
			elementSubContent.appendChild(commons.createElementWithValue(doc, "PBan", SystemParams.VERSION_XML_TNCN));
			elementSubContent.appendChild(commons.createElementWithValue(doc, "TCTu", tctu));
			elementSubContent.appendChild(commons.createElementWithValue(doc, "MSCTu", msctu));
			elementSubContent.appendChild(commons.createElementWithValue(doc, "KHCTu", khctu));
			elementSubContent.appendChild(commons.createElementWithValue(doc, "SCTu", docTmp.getEmbedded(Arrays.asList("CTTNCNhan","SCTu"), "")));
			elementSubContent.appendChild(commons.createElementWithValue(doc, "NLap", LocalDate.now().toString()));
			elementSubContent.appendChild(commons.createElementWithValue(doc, "TTKhac", ""));
			if (docTTCTLQuan != null) {
				elementTmp = doc.createElement("TTCTLQuan");
				elementTmp.appendChild(commons.createElementWithValue(doc, "TCCTu", docTTCTLQuan.get("TCCTu", "")));
				elementTmp.appendChild(commons.createElementWithValue(doc, "LHCTLQuan", docTTCTLQuan.get("LHCTLQuan", "")));
				elementTmp.appendChild(commons.createElementWithValue(doc, "KHMSCTCLQuan", docTTCTLQuan.get("KHMSCTCLQuan", "")));
				elementTmp.appendChild(commons.createElementWithValue(doc, "KHCTCLQuan", docTTCTLQuan.get("KHCTCLQuan", "")));
				elementTmp.appendChild(commons.createElementWithValue(doc, "SCTCLQuan", docTTCTLQuan.get("SCTCLQuan", "")));
				elementTmp.appendChild(commons.createElementWithValue(doc, "NLCTCLQuan", docTTCTLQuan.get("NLCTCLQuan", "")));
				elementTmp.appendChild(commons.createElementWithValue(doc, "GChu", ""));
				elementSubContent.appendChild(elementTmp);
			}
			elementContent.appendChild(elementSubContent);

			/* TO CHUC TRA THU NHAP */
			elementSubContent = doc.createElement("NDCTu");

			elementTmp = doc.createElement("TCTTNhap");
			elementTmp.appendChild(
					commons.createElementWithValue(doc, "Ten", docTmp.getEmbedded(Arrays.asList("Name"), "")));
			elementTmp.appendChild(
					commons.createElementWithValue(doc, "MST", docTmp.getEmbedded(Arrays.asList("TaxCode"), "")));
			elementTmp.appendChild(
					commons.createElementWithValue(doc, "DChi", docTmp.getEmbedded(Arrays.asList("Address"), "")));
			elementTmp.appendChild(
					commons.createElementWithValue(doc, "SDThoai", docTmp.getEmbedded(Arrays.asList("Phone"), "")));
			elementTmp.appendChild(commons.createElementWithValue(doc, "TTKhac", ""));
			elementSubContent.appendChild(elementTmp);

			elementTmp = doc.createElement("NNT");
			elementTmp.appendChild(commons.createElementWithValue(doc, "Ten", ten));
			elementTmp.appendChild(commons.createElementWithValue(doc, "MST", msthue));
			elementTmp.appendChild(commons.createElementWithValue(doc, "DChi", dchi));
			elementTmp.appendChild(commons.createElementWithValue(doc, "QTich", qtich));
			elementTmp.appendChild(commons.createElementWithValue(doc, "CNCTru", ctru));
			elementTmp.appendChild(commons.createElementWithValue(doc, "CCCDan", cccdan));
			elementTmp.appendChild(commons.createElementWithValue(doc, "SDThoai", sdthoai));
			elementTmp.appendChild(commons.createElementWithValue(doc, "DCTDTu", dctdtu));
			elementTmp.appendChild(commons.createElementWithValue(doc, "GChu", ""));
			elementTmp.appendChild(commons.createElementWithValue(doc, "TTKhac", ""));
			elementSubContent.appendChild(elementTmp);

			elementTmp = doc.createElement("TTNCNKTru");
			elementTmp.appendChild(commons.createElementWithValue(doc, "KTNhap", ktnhap));
			elementTmp.appendChild(commons.createElementWithValue(doc, "TThang", tthang));
			elementTmp.appendChild(commons.createElementWithValue(doc, "DThang", dthang));
			elementTmp.appendChild(commons.createElementWithValue(doc, "Nam", nam));
			elementTmp.appendChild(commons.createElementWithValue(doc, "BHiem", bhiem));
			elementTmp.appendChild(commons.createElementWithValue(doc, "TThien", tthien));
			elementTmp.appendChild(commons.createElementWithValue(doc, "TTNCThue", ttncthue));
			elementTmp.appendChild(commons.createElementWithValue(doc, "TTNTThue", ttntthue));
			elementTmp.appendChild(commons.createElementWithValue(doc, "SThue", sthue));
			elementSubContent.appendChild(elementTmp);

			elementContent.appendChild(elementSubContent);

			/* END - TAO FILE XML */
			isSdaveFile = commons.docW3cToFile(doc, pathDir, fileNameXML);
			if (!isSdaveFile) {
				throw new Exception("Lưu dữ liệu không thành công.");
			}
			docFind = new Document("_id", objectIdCT).append("IsActive", true).append("IsDelete",
					new Document("$ne", true));
			docUpdate = new Document("NLap",  LocalDate.now())
					.append("MauSo", mscttncn)
					.append("TCTu", tctu)
					.append("MSCTu", msctu)
					.append("KHCTu", khctu)
					.append("NLap", LocalDate.now())
					.append("TTCTLQuan", docTTCTLQuan)
					.append("NNT",
							new Document("Ten", ten).append("Code", code).append("MST", msthue).append("DChi", dchi)
									.append("QTich", qtich).append("CTru", ctru).append("CCCDan", cccdan)
									.append("SDThoai", sdthoai).append("DCTDTu", dctdtu).append("DCTDTuCC", dctdtucc))
					.append("TTNCNKTru",
							new Document("KTNhap", ktnhap).append("Nam", nam).append("TThang", tthang)
									.append("DThang", dthang).append("BHiem", bhiem).append("TThien", tthien)
									.append("TTNCThue", ttncthue).append("TTNTThue", ttntthue).append("SThue", sthue))
					.append("Dir", pathDir)
					.append("FileNameXML", fileNameXML)
					.append("InfoUpdated",
							new Document("UpdatedDate", LocalDateTime.now())
							.append("UpdatedUserID", header.getUserId())
							.append("UpdatedUserName", header.getUserName())
							.append("UpdatedUserFullName", header.getUserFullName()));
			options = new FindOneAndUpdateOptions();
			options.upsert(false);
			options.maxTime(5000, TimeUnit.MILLISECONDS);
			options.returnDocument(ReturnDocument.AFTER);
			try (MongoClient mongoClient = cfg.mongoClient()) {
				MongoCollection<Document> collection = mongoClient.getDatabase(cfg.dbName).getCollection("CTTNCNhan");
				collection.findOneAndUpdate(docFind, new Document("$set", docUpdate), options);
			} catch (Exception e) {
			}

			responseStatus = new MspResponseStatus(0, "SUCCESS");
			rsp.setResponseStatus(responseStatus);
			return rsp;
		
		case Constants.MSG_ACTION_CODE.DELETE:
			objectIdCT = null;
			try {
				objectIdCT = new ObjectId(_id);
			} catch (Exception e) {
			}
			
			docFind = new Document("IsDelete", new Document("$ne", true))
					.append("_id", objectIdCT).append("IssuerId", header.getIssuerId())
					.append("SignStatus", "NOSIGN");
			
			try (MongoClient mongoClient = cfg.mongoClient()) {
				MongoCollection<Document> collection = mongoClient.getDatabase(cfg.dbName).getCollection("CTTNCNhan");
				docTmp = collection.find(docFind).allowDiskUse(true).iterator().next();	
			} catch (Exception e) {
			}
			
			if (null == docTmp) {
				responseStatus = new MspResponseStatus(9999, "Không tìm thấy thông tin chứng từ.");
				rsp.setResponseStatus(responseStatus);
				return rsp;
			}
			
			options = new FindOneAndUpdateOptions();
			options.upsert(false);
			options.maxTime(5000, TimeUnit.MILLISECONDS);
			options.returnDocument(ReturnDocument.AFTER);
			
			docUpdate = new Document("IsDelete", true)
					.append("InfoDeleted", 
							new Document("DeletedDate", LocalDateTime.now())
							.append("DeletedUserID", header.getUserId())
							.append("DeletedUserName", header.getUserName())
							.append("DeletedUserFullName", header.getUserFullName()));
					
			try (MongoClient mongoClient = cfg.mongoClient()) {
				MongoCollection<Document> collection = mongoClient.getDatabase(cfg.dbName).getCollection("CTTNCNhan");
				collection.findOneAndUpdate(docFind, new Document("$set", docUpdate), options);
			} catch (Exception e) {
			}
			
			docTTCTLQuan = docTmp.getEmbedded(Arrays.asList("TTCTLQuan"),
					Document.class);
			if (docTTCTLQuan != null && docTTCTLQuan.get("_id") != null) {
				ObjectId objectIdTTCTLQuan = new ObjectId(docTTCTLQuan.getString("_id"));
				Document find = new Document("IssuerId", header.getIssuerId()).append("IsDelete", false)
						.append("_id", objectIdTTCTLQuan).append("Status", new Document("$in",
								Arrays.asList(Constants.INVOICE_STATUS.ADJUSTED, Constants.INVOICE_STATUS.REPLACED)));

				try (MongoClient mongoClient = cfg.mongoClient()) {
					MongoCollection<Document> collection = mongoClient.getDatabase(cfg.dbName).getCollection("CTTNCNhan");
					collection.findOneAndUpdate(find,
							new Document("$set", new Document("Status", Constants.INVOICE_STATUS.COMPLETE)),
							options);
				} catch (Exception e) {
				}
			}

			
			int sctu = docTmp.getEmbedded(Arrays.asList("SCTu"), 0);
			if (sctu != 0) {
				String mstncn = docTmp.getEmbedded(Arrays.asList("MauSo"), "");
				try {
					mstncnId = new ObjectId(mstncn);
				} catch (Exception e) {
				}

				docFind = new Document("IsDelete", new Document("$ne", true)).append("_id", mstncnId);

				try (MongoClient mongoClient = cfg.mongoClient()) {
					MongoCollection<Document> collection = mongoClient.getDatabase(cfg.dbName)
							.getCollection("DMMSTNCN");
					docTmp = collection.find(docFind).allowDiskUse(true).iterator().next();
				} catch (Exception e) {
				}

				if (null == docTmp) {
					responseStatus = new MspResponseStatus(9999, "Không tìm thấy mẫu số chứng từ.");
					rsp.setResponseStatus(responseStatus);
					return rsp;
				}
				int SHDHT = docTmp.getEmbedded(Arrays.asList("SHDHT"), 0);
				int SHDCL = docTmp.getEmbedded(Arrays.asList("ConLai"), 0);

				if (sctu != SHDHT) {
					responseStatus = new MspResponseStatus(9999, "Chỉ có thể xóa số chứng từ lớn nhất!");
					rsp.setResponseStatus(responseStatus);
					return rsp;
				}
				SHDHT = SHDHT - 1;
				SHDCL = SHDCL + 1;

				options = new FindOneAndUpdateOptions();
				options.upsert(false);
				options.maxTime(5000, TimeUnit.MILLISECONDS);
				options.returnDocument(ReturnDocument.AFTER);

				try (MongoClient mongoClient = cfg.mongoClient()) {
					MongoCollection<Document> collection = mongoClient.getDatabase(cfg.dbName)
							.getCollection("DMMSTNCN");
					collection.findOneAndUpdate(docFind,
							new Document("$set",
									new Document("SHDHT", SHDHT).append("ConLai", SHDCL).append("InfoDeleted",
											new Document("DeletedDate", LocalDateTime.now())
													.append("DeletedUserID", header.getUserId())
													.append("DeletedUserName", header.getUserName())
													.append("DeletedUserFullName", header.getUserFullName()))),
							options);
				} catch (Exception e) {
				}
			}

			responseStatus = new MspResponseStatus(0, "SUCCESS");
			rsp.setResponseStatus(responseStatus);
			return rsp;

		case Constants.MSG_ACTION_CODE.SEND_CQT:
			mstncnId = null;
			try {
				mstncnId = new ObjectId(_id);
			} catch (Exception e) {
			}
			docFind = new Document("_id", mstncnId).append("IssuerId", header.getIssuerId()).append("IsActive", true)
					.append("IsDelete", new Document("$ne", true))
					.append("SignStatus", Constants.INVOICE_SIGN_STATUS.SIGNED)
					.append("Status", Constants.INVOICE_STATUS.PENDING);
			
			pipeline = new ArrayList<Document>();
			pipeline.add(new Document("$match", docFind));
			
			docTmp = null;
			try (MongoClient mongoClient = cfg.mongoClient()) {
				MongoCollection<Document> collection = mongoClient.getDatabase(cfg.dbName).getCollection("CTTNCNhan");
				docTmp = collection.aggregate(pipeline).allowDiskUse(true).iterator().next();
			} catch (Exception e) {
			}

			if (null == docTmp) {
				responseStatus = new MspResponseStatus(9999, "Không tìm thấy thông tin chứng từ.");
				rsp.setResponseStatus(responseStatus);
				return rsp;
			}
			
			String dir = docTmp.get("Dir", "");
			String mtdiep = docTmp.get("MTDiep", "");
			String fileName = _id + "_signed.xml";
			String mst = docTmp.getEmbedded(Arrays.asList("TCTTNhap", "MST"), "");
			
			file = new File(dir, fileName);
			if (!file.exists() || !file.isFile()) {
				responseStatus = new MspResponseStatus(9999, "Không tìm thấy thông tin chứng từ đã ký.");
				rsp.setResponseStatus(responseStatus);
				return rsp;
			}
			
			doc = commons.fileToDocument(file, true);
			if (null == doc) {
				responseStatus = new MspResponseStatus(9999, "Dữ liệu chứng từ đã ký không tồn tại.");
				rsp.setResponseStatus(responseStatus);
				return rsp;
			}
			
			rTCTN = tctnService.callTiepNhanThongDiep("211", mtdiep, mst, "1", doc);
			if (rTCTN == null) {
				rTCTN = tctnService.callTiepNhanThongDiep("211", mtdiep, mst, "1", doc);
			}
			if (rTCTN == null) {
				responseStatus = new MspResponseStatus(9999, "Lỗi khi gửi chứng từ đến CQT. Vui lòng liên hệ nhà cung cấp để được xử lý!");
				rsp.setResponseStatus(responseStatus);
				return rsp;
			}
			
//			fileName = _id + "_" + "tiepnhanthondiep.xml";
//			boolean boo = false;
//			try {
//				boo = commons.docW3cToFile(rTCTN, dir, fileName);
//			} catch (Exception e) {
//			}
			
			xPath = XPathFactory.newInstance().newXPath();
			Node nodeDLieu = (Node) xPath.evaluate("/TDiep", rTCTN, XPathConstants.NODE);
			
			codeTTTNhan = commons.getTextFromNodeXML(
					(Element) xPath.evaluate("DLieu/TBao/TTTNhan", nodeDLieu, XPathConstants.NODE));
			descTTTNhan = commons.getTextFromNodeXML(
					(Element) xPath.evaluate("DLieu/TBao/DSLDo/LDo/MTa", nodeDLieu, XPathConstants.NODE));

			switch (codeTTTNhan) {
			case "1":
				responseStatus = new MspResponseStatus(9999,
						"".equals(descTTTNhan) ? "Không tìm thấy tenant dữ liệu." : descTTTNhan);
				rsp.setResponseStatus(responseStatus);
				return rsp;
			case "2":
				responseStatus = new MspResponseStatus(9999,
						"".equals(descTTTNhan) ? "Mã thông điệp đã tồn tại." : descTTTNhan);
				rsp.setResponseStatus(responseStatus);
				return rsp;
			case "3":
				responseStatus = new MspResponseStatus(9999,
						"".equals(descTTTNhan) ? "Thất bại, lỗi Exception." : descTTTNhan);
				rsp.setResponseStatus(responseStatus);
				return rsp;
			default:
				break;
			}
			
			options = new FindOneAndUpdateOptions();
			options.upsert(false);
			options.maxTime(5000, TimeUnit.MILLISECONDS);
			options.returnDocument(ReturnDocument.AFTER);
			
			try (MongoClient mongoClient = cfg.mongoClient()) {
				MongoCollection<Document> collection = mongoClient.getDatabase(cfg.dbName).getCollection("CTTNCNhan");
				docTmp = collection.aggregate(pipeline).allowDiskUse(true).iterator().next();
				collection.findOneAndUpdate(docFind,
						new Document("$set", new Document("Status", Constants.INVOICE_STATUS.PROCESSING)
								.append("SendCQT_Date", LocalDateTime.now())
								.append("InfoSendCQT",
										new Document("Date", LocalDateTime.now())
										.append("UserID", header.getUserId())
										.append("UserName", header.getUserName())
										.append("UserFullName", header.getUserFullName()))),
						options);
			} catch (Exception e) {
			}

			responseStatus = new MspResponseStatus(0, "SUCCESS");
			rsp.setResponseStatus(responseStatus);
			return rsp;
		case Constants.MSG_ACTION_CODE.CHECK: // check kq từ cqt
			mstncnId = null;
			try {
				mstncnId = new ObjectId(_id);
			} catch (Exception e) {
			}
			docFind = new Document("_id", mstncnId).append("IssuerId", header.getIssuerId()).append("IsActive", true)
					.append("IsDelete", new Document("$ne", true))
					.append("SignStatus", Constants.INVOICE_SIGN_STATUS.SIGNED)
					.append("Status", new Document("$in",
							Arrays.asList(Constants.INVOICE_STATUS.ERROR_CQT, Constants.INVOICE_STATUS.PROCESSING)));

			pipeline = new ArrayList<Document>();
			pipeline.add(new Document("$match", docFind));
			
			docTmp = null;
			try (MongoClient mongoClient = cfg.mongoClient()) {
				MongoCollection<Document> collection = mongoClient.getDatabase(cfg.dbName).getCollection("CTTNCNhan");
				docTmp = collection.aggregate(pipeline).allowDiskUse(true).iterator().next();
			} catch (Exception e) {
			}

			if (null == docTmp) {
				responseStatus = new MspResponseStatus(9999, "Không tìm thấy thông tin chứng từ.");
				rsp.setResponseStatus(responseStatus);
				return rsp;
			}
			
			
			String mtd = docTmp.get("MTDiep", "");
			
			rTCTN = tctnService.callTraCuuThongDiep(mtd);
			if (rTCTN == null) {
				rTCTN = tctnService.callTraCuuThongDiep(mtd);
			}

			if (rTCTN == null) {
				responseStatus = new MspResponseStatus(9999, "Lỗi khi lấy kết quả chứng từ từ CQT. Vui lòng liên hệ nhà cung cấp để được xử lý!");
				rsp.setResponseStatus(responseStatus);
				return rsp;
			}
			
			xPath = XPathFactory.newInstance().newXPath();
			Node nodeKetQuaTraCuu = (Node) xPath.evaluate("/KetQuaTraCuu", rTCTN, XPathConstants.NODE);
			maKetQua = commons
					.getTextFromNodeXML((Element) xPath.evaluate("MaKetQua", nodeKetQuaTraCuu, XPathConstants.NODE));
			moTaKetQua = commons
					.getTextFromNodeXML((Element) xPath.evaluate("MoTaKetQua", nodeKetQuaTraCuu, XPathConstants.NODE));
			
			
			if ("2".equals(maKetQua)) {
				String dir1 = docTmp.get("Dir", "");
				String fileName1 = _id + "_signed.xml";
				String taxCode = docTmp.getEmbedded(Arrays.asList("TCTTNhap", "MST"), "");
				
				file = new File(dir1,fileName1);
				if (!file.exists() || !file.isFile()) {
					responseStatus = new MspResponseStatus(9999, "Không tìm thấy thông tin chứng từ đã ký.");
					rsp.setResponseStatus(responseStatus);
					return rsp;
				}
				
				doc = commons.fileToDocument(file, true);
				tctnService.callTiepNhanThongDiep("211", mtd, taxCode, "1", doc);
				
				rTCTN = tctnService.callTraCuuThongDiep(mtd);
				nodeKetQuaTraCuu = (Node) xPath.evaluate("/KetQuaTraCuu", rTCTN, XPathConstants.NODE);
				maKetQua = commons.getTextFromNodeXML(
						(Element) xPath.evaluate("MaKetQua", nodeKetQuaTraCuu, XPathConstants.NODE));
				moTaKetQua = commons.getTextFromNodeXML(
						(Element) xPath.evaluate("MoTaKetQua", nodeKetQuaTraCuu, XPathConstants.NODE));
				
				if ("2".equals(maKetQua)) {
					responseStatus = new MspResponseStatus(9999, "Mã giao dịch không đúng.");
					rsp.setResponseStatus(responseStatus);
					return rsp;
				}
			}
			
			Node nodeTDiep = null;
			String checkMLTDiep = "";
			String LTBao = "";
			NodeList tDiepNodes = (NodeList) xPath.evaluate("DuLieu/TDiep", nodeKetQuaTraCuu, XPathConstants.NODESET);
			for (int i = 0; i < tDiepNodes.getLength(); i++) {
				nodeTDiep = (Node) tDiepNodes.item(i);
				checkMLTDiep = commons
						.getTextFromNodeXML((Element) xPath.evaluate("TTChung/MLTDiep", nodeTDiep, XPathConstants.NODE));
				if (checkMLTDiep.equals("213")) {
					LTBao = commons.getTextFromNodeXML(
							(Element) xPath.evaluate("DLieu/TBao/DLTBao/LTBao", nodeTDiep, XPathConstants.NODE));
					break;
				}
				if (checkMLTDiep.equals("-1")) {
					break;
				}
			}
			
			if (nodeTDiep == null) {
				responseStatus = new MspResponseStatus(9999, "Chưa có kết quả trả về.");
				rsp.setResponseStatus(responseStatus);
				return rsp;
			}
			
			options = new FindOneAndUpdateOptions();
			options.upsert(false);
			options.maxTime(5000, TimeUnit.MILLISECONDS);
			options.returnDocument(ReturnDocument.AFTER);
			
			String CQT_MLTDiep = commons
					.getTextFromNodeXML((Element) xPath.evaluate("TTChung/MLTDiep", nodeTDiep, XPathConstants.NODE));
			String maLoi = "";
			String moTaLoi = "";
			if (!CQT_MLTDiep.equals("213") || !LTBao.equals("2")) {
				maLoi = commons.getTextFromNodeXML((Element) xPath.evaluate("DLieu/TBao/DLTBao/LCTu/DSCTu/CTu/DSLDo/LDo/MLoi",
						nodeTDiep, XPathConstants.NODE));
				moTaLoi = commons.getTextFromNodeXML((Element) xPath.evaluate("DLieu/TBao/DLTBao/LCTu/DSCTu/CTu/DSLDo/LDo/MTLoi",
						nodeTDiep, XPathConstants.NODE));
				
				dir = docTmp.get("Dir", "");
				fileName = _id + "_" + CQT_MLTDiep + ".xml";
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
				
				try (MongoClient mongoClient = cfg.mongoClient()) {
					MongoCollection<Document> collection = mongoClient.getDatabase(cfg.dbName).getCollection("CTTNCNhan");
					collection.findOneAndUpdate(docFind,
							new Document("$set",
									new Document("Status", Constants.INVOICE_STATUS.ERROR_CQT)
											.append("CQT_Date", LocalDate.now())
											.append("LDo", new Document("MLoi", maLoi).append("MTLoi", moTaLoi))),
							options);
				} catch (Exception e) {
				}
				responseStatus = new MspResponseStatus(0,
						"".equals(moTaLoi) ? "CQT chưa có thông báo kết quả trả về." : moTaLoi);
				rsp.setResponseStatus(responseStatus);
				return rsp;
			}
			
			dir = docTmp.get("Dir", "");
			fileName = _id + "_" + mtd + ".xml";
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
			
			try (MongoClient mongoClient = cfg.mongoClient()) {
				MongoCollection<Document> collection = mongoClient.getDatabase(cfg.dbName).getCollection("CTTNCNhan");
				collection.findOneAndUpdate(docFind,
						new Document("$set",
								new Document("Status", Constants.INVOICE_STATUS.COMPLETE)
										.append("CQT_Date", LocalDate.now())
										.append("LDo", new Document("MLoi", "").append("MTLoi", ""))),
						options);
			} catch (Exception e) {
			}
			
			// xử lý cho chứng từ điều chỉnh thay thế.... update lại trạng thái hóa đơn trước
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
	public MsgRsp listV1(JSONRoot jsonRoot) throws Exception {
		Msg msg = jsonRoot.getMsg();
		MsgHeader header = msg.getMsgHeader();
		MsgPage page = msg.getMsgPage();
		Object objData = msg.getObjData();

		MsgRsp rsp = new MsgRsp(header);
		MspResponseStatus responseStatus = null;

		String sctu = "";
		String datelap = "";
		LocalDate dateTo = null;
		Document docMatchDate = null;
		JsonNode jsonData = null;
		if (objData != null) {
			jsonData = Json.serializer().nodeFromObject(objData);
			sctu = commons.getTextJsonNode(jsonData.at("/SCTu")).trim().replaceAll("\\s+", " ");
			datelap = commons.getTextJsonNode(jsonData.at("/DateLap"));

		}
		dateTo = "".equals(datelap) || !commons.checkLocalDate(datelap, Constants.FORMAT_DATE.FORMAT_DATE_WEB) ? null
				: commons.convertStringToLocalDate(datelap, Constants.FORMAT_DATE.FORMAT_DATE_WEB);
		if (null != dateTo) {
			docMatchDate = new Document();
			if (null != dateTo)
				docMatchDate.append("$eq", dateTo);
		}

		Document docTmp = null;

		Document docMatch = new Document("IssuerId", header.getIssuerId()).append("IsDelete",
				new Document("$ne", true));
		if (!"".equals(sctu))
			docMatch.append("SCTu", commons.stringToInteger(sctu));

		if (docMatchDate != null) {
			docMatch.append("NLap", docMatchDate);
		}

		List<Document> pipeline = new ArrayList<Document>();
		pipeline.add(new Document("$match", docMatch));
		pipeline.add(new Document("$sort", new Document("SignStatus", 1).append("SCTu", -1).append("_id", -1)));
		pipeline.addAll(createFacetForSearchNotSort(page));

		try (MongoClient mongoClient = cfg.mongoClient()) {
			MongoCollection<Document> collection = mongoClient.getDatabase(cfg.dbName).getCollection("CTTNCNhan");
			docTmp = collection.aggregate(pipeline).allowDiskUse(true).iterator().next();
		} catch (Exception e) {
		}

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
				ObjectId objectId = (ObjectId) doc.get("_id");
				hItem = new HashMap<String, Object>();
				hItem.put("_id", objectId.toString());
				hItem.put("Status", doc.get("Status"));
				hItem.put("SignStatus", doc.get("SignStatus"));
				hItem.put("SCTu", doc.get("SCTu"));
				hItem.put("NLap", doc.get("NLap"));
				hItem.put("TCTTNhap", doc.get("TCTTNhap"));
				hItem.put("NNT", doc.get("NNT"));
				hItem.put("TTNCNKTru", doc.get("TTNCNKTru"));
				hItem.put("InfoCreated", doc.get("InfoCreated"));
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
	public MsgRsp detailV1(JSONRoot jsonRoot, String _id) throws Exception {
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

		try (MongoClient mongoClient = cfg.mongoClient()) {
			MongoCollection<Document> collection = mongoClient.getDatabase(cfg.dbName).getCollection("CTTNCNhan");
			docTmp = collection.find(docFind).allowDiskUse(true).iterator().next();
		} catch (Exception e) {
			// TODO: handle exception
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
	public FileInfo getFileForSignV1(JSONRoot jsonRoot) throws Exception {
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

		/* NHO KIEM TRA XEM CO HD NAO DANG KY KHONG */
		FindOneAndUpdateOptions options = null;

		Document docFind = new Document("IssuerId", header.getIssuerId())
				.append("IsDelete", new Document("$ne", true))
				.append("_id", objectId)
				.append("Status", new Document("$in", Arrays.asList(Constants.INVOICE_STATUS.CREATED, Constants.INVOICE_STATUS.PENDING)))
				.append("SignStatus", "NOSIGN");

		List<Document> pipeline = new ArrayList<Document>();
		pipeline.add(new Document("$match", docFind));

		pipeline.add(new Document("$lookup", new Document("from", "DMMSTNCN")
				.append("let", new Document("vMauSo", "$MauSo").append("vIssuerId", "$IssuerId"))
				.append("pipeline", Arrays.asList(new Document("$match", new Document("$expr", new Document("$and",
						Arrays.asList(new Document("$gt", Arrays.asList("$ConLai", 0)),
								new Document("$eq", Arrays.asList("$IsActive", true)),
								new Document("$ne", Arrays.asList("$IsDelete", true)),
								new Document("$eq", Arrays.asList(new Document("$toString", "$_id"), "$$vMauSo")),
								new Document("$eq", Arrays.asList("$IssuerId", "$$vIssuerId"))))))))
				.append("as", "DMMSTNCN")));
		pipeline.add(
				new Document("$unwind", new Document("path", "$DMMSTNCN").append("preserveNullAndEmptyArrays", true)));

		Document docTmp = null;

		try (MongoClient mongoClient = cfg.mongoClient()) {
			MongoCollection<Document> collection = mongoClient.getDatabase(cfg.dbName).getCollection("CTTNCNhan");
			docTmp = collection.aggregate(pipeline).allowDiskUse(true).iterator().next();
		} catch (Exception e) {
		}

		if (null == docTmp) {
			return fileInfo;
		}
		if (null == docTmp.get("DMMSTNCN")) {
			return fileInfo;
		}

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
		/* TAO SO Chung tu VA GHI DU LIEU VO FILE */
		
		int cur_sctu = docTmp.getEmbedded(Arrays.asList("SCTu"), 0);
		int largest_sctu = docTmp.getEmbedded(Arrays.asList("DMMSTNCN", "SHDHT"), 0);
		int getSL = docTmp.getEmbedded(Arrays.asList("DMMSTNCN", "SoLuong"), 0);
		int sctu = 0;

		if (cur_sctu == 0) {
			sctu = largest_sctu + 1;
		} else {
			sctu = cur_sctu;
		}
		int CL = getSL - sctu;

		/* DOC DU LIEU XML, VA GHI DU LIEU VO SO HD */
		org.w3c.dom.Document doc = commons.fileToDocument(file);
		XPath xPath = XPathFactory.newInstance().newXPath();
		Node nodeDLCTu = (Node) xPath.evaluate("/CTu/DLCTu[@Id='data']", doc, XPathConstants.NODE);
		Node nodeTTChung = (Node) xPath.evaluate("TTChung", nodeDLCTu, XPathConstants.NODE);

		Element elementSub = (Element) xPath.evaluate("SCTu", nodeTTChung, XPathConstants.NODE);
		if (null == elementSub) {
			elementSub = doc.createElement("SCTu");
			elementSub.setTextContent(String.valueOf(sctu));
			nodeTTChung.appendChild(elementSub);
		} else {
			elementSub.setTextContent(String.valueOf(sctu));
		}

		fileInfo.setFileName(fileName);
		fileInfo.setContentFile(commons.docW3cToByte(doc));

		/* UPDATE EINVOICE - STATUS */
		options = new FindOneAndUpdateOptions();
		options.upsert(false);
		options.maxTime(5000, TimeUnit.MILLISECONDS);
		options.returnDocument(ReturnDocument.AFTER);

		try (MongoClient mongoClient = cfg.mongoClient()) {
			MongoCollection<Document> collection = mongoClient.getDatabase(cfg.dbName).getCollection("CTTNCNhan");
			collection.findOneAndUpdate(docFind,
					new Document("$set", new Document("SCTu", sctu).append("Status", Constants.INVOICE_STATUS.PENDING)), options);	
		} catch (Exception e) {
		}
		
		if (cur_sctu == 0) {
			Document docFindMS = null;
			docFindMS = new Document("IssuerId", header.getIssuerId()).append("IsDelete", new Document("$ne", true))
					.append("IsActive", true).append("_id", objectIdMS);

			try (MongoClient mongoClient = cfg.mongoClient()) {
				MongoCollection<Document> collection = mongoClient.getDatabase(cfg.dbName).getCollection("DMMSTNCN");
				collection.findOneAndUpdate(docFindMS,
						new Document("$set", new Document("ConLai", CL).append("SHDHT", sctu)), options);	
			} catch (Exception e) {
			}
		}
		return fileInfo;
	}

	@Override
	public Object signSingleV1(InputStream is, JSONRoot jsonRoot, String _id) throws Exception {
		Msg msg = jsonRoot.getMsg();
		MsgHeader header = msg.getMsgHeader();
		MsgPage page = msg.getMsgPage();

		MsgRsp rsp = new MsgRsp(header);
		rsp.setMsgPage(page);
		MspResponseStatus responseStatus = null;

		/* DOC NOI DUNG XML DA KY */
		org.w3c.dom.Document xmlDoc = commons.inputStreamToDocument(is, false);

		int sctu = 0;

		XPath xPath = XPathFactory.newInstance().newXPath();
		Node nodeDLCTu = (Node) xPath.evaluate("/CTu/DLCTu[@Id='data']", xmlDoc, XPathConstants.NODE);
		Node nodeTTChung = (Node) xPath.evaluate("TTChung", nodeDLCTu, XPathConstants.NODE);
		sctu = commons.stringToInteger(
				commons.getTextFromNodeXML((Element) xPath.evaluate("SCTu", nodeTTChung, XPathConstants.NODE)));
		ObjectId objectId = null;
		try {
			objectId = new ObjectId(_id);
		} catch (Exception e) {
		}
		List<Document> pipeline = null;
		/* KIEM TRA THONG TIN HOP LE KHONG */
		Document docFind = new Document("IssuerId", header.getIssuerId()).append("SCTu", sctu)
				.append("Status", "PENDING").append("_id", objectId);

		pipeline = new ArrayList<Document>();
		pipeline.add(new Document("$match", docFind));
		pipeline.add(new Document("$lookup", new Document("from", "CTTNCNhan")
				.append("let", new Document("vIssuerId", "$IssuerId").append("vMauSo", "$MauSo"))
				.append("pipeline", Arrays.asList(
						new Document("$match",
								new Document("$expr", new Document("$and",
										Arrays.asList(new Document("$eq", Arrays.asList("$IssuerId", "$$vIssuerId")),
												new Document("$eq", Arrays.asList("$MauSo", "$$vMauSo")),
												new Document("$ne", Arrays.asList("$IsDelete", true)),
												new Document("$in", Arrays.asList("$Status",
														Arrays.asList("COMPLETE", "ERROR_CQT", "PROCESSING", "XOABO",
																"DELETED", "REPLACED", "ADJUSTED"))))))),
						new Document("$group",
								new Document("_id", "$MauSo").append("SCTu", new Document("$max", "$SCTu")))))
				.append("as", "CTTNCNMAXCQT")));
		pipeline.add(new Document("$unwind",
				new Document("path", "$CTTNCNMAXCQT").append("preserveNullAndEmptyArrays", true)));
		Document docTmp = null;
	
		try (MongoClient mongoClient = cfg.mongoClient()) {
			MongoCollection<Document> collection = mongoClient.getDatabase(cfg.dbName).getCollection("CTTNCNhan");
			docTmp =   collection.aggregate(pipeline).allowDiskUse(true).iterator().next();
		} catch (Exception e) {
		}

		if (null == docTmp) {
			responseStatus = new MspResponseStatus(9999, "Không tìm thấy thông tin hóa đơn.");
			rsp.setResponseStatus(responseStatus);
			return rsp;
		}

		int cur_sctu = docTmp.getEmbedded(Arrays.asList("SCTu"), 0);
		int max_sctu = 0;
		if (docTmp.get("CTTNCNMAXCQT") != null)
			max_sctu = docTmp.getEmbedded(Arrays.asList("CTTNCNMAXCQT", "SCTu"), 0);
		if (cur_sctu == 0 || cur_sctu != max_sctu + 1) {
			responseStatus = new MspResponseStatus(9999,
					"Có 1 hoặc 1 vài số hóa đơn trước đó chưa được xử lý xong.<br>Vui lòng kiểm tra lại danh sách hóa đơn.");
			rsp.setResponseStatus(responseStatus);
			return rsp;
		}

		/* LUU FILE VA CAP NHAT TRANG THAI */
		String dir = docTmp.get("Dir", "");
		String fileName = _id + "_signed.xml";
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
			objectId = new ObjectId(docTmp.getEmbedded(Arrays.asList("MauSo"), ""));
		} catch (Exception e) {
		}
		if (null == objectId) {
			throw new Exception("Không tìm thấy mẫu số hóa đơn.");
		}

		try (MongoClient mongoClient = cfg.mongoClient()) {
			MongoCollection<Document> collection = mongoClient.getDatabase(cfg.dbName).getCollection("CTTNCNhan");
			collection.findOneAndUpdate(docFind, new Document("$set",
					new Document("SignStatus", Constants.INVOICE_SIGN_STATUS.SIGNED)
					.append("InfoSigned",
							new Document("SignedDate", LocalDateTime.now())
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
	public MsgRsp history(JSONRoot jsonRoot, String _id) throws Exception {
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

		Document docFind = new Document("IssuerId", header.getIssuerId()).append("_id", objectId)
				.append("IsDelete", new Document("$ne", true))
				.append("Status", new Document("$in", Arrays.asList("ERROR_CQT", "PROCESSING", "COMPLETE")));
		
		Document docTmp = null;
		MongoClient mongoClient = cfg.mongoClient();
		MongoCollection<Document> collection = mongoClient.getDatabase(cfg.dbName).getCollection("CTTNCNhan");
		try {
			docTmp = collection.find(docFind).allowDiskUse(true).iterator().next();
		} catch (Exception e) {

		}

		mongoClient.close();

		if (null == docTmp) {
			responseStatus = new MspResponseStatus(9999, "Không tìm thấy thông tin chứng từ.");
			rsp.setResponseStatus(responseStatus);
			return rsp;
		}

		String MTDiep = docTmp.get("MTDiep", "");
		
		org.w3c.dom.Document rTCTN = tctnService.callTraCuuThongDiep(MTDiep);
		if (rTCTN == null) {
			responseStatus = new MspResponseStatus(9999, "Kết nối với TCTN không thành công.");
			rsp.setResponseStatus(responseStatus);
			return rsp;
		}

		/* LUU LAI FILE KQTN */
		String dir = docTmp.get("Dir", "");
		String fileName = _id + "_" + MTDiep + ".xml";
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

		/* DO DU LIEU TRA VE - CAP NHAT LAI KET QUA */
		XPath xPath = XPathFactory.newInstance().newXPath();
		Node nodeKetQuaTraCuu = (Node) xPath.evaluate("/KetQuaTraCuu", rTCTN, XPathConstants.NODE);
		String MaKetQua = commons
				.getTextFromNodeXML((Element) xPath.evaluate("MaKetQua", nodeKetQuaTraCuu, XPathConstants.NODE));
		String MoTaKetQua = commons
				.getTextFromNodeXML((Element) xPath.evaluate("MoTaKetQua", nodeKetQuaTraCuu, XPathConstants.NODE));

		if (!"0".equals(MaKetQua)) {
			responseStatus = new MspResponseStatus(9999, MoTaKetQua);
			rsp.setResponseStatus(responseStatus);
			return rsp;
		}

		Node nodeTDiep = null;
		String CQT_MLTDiep = "";
		int stt = 0;
		ArrayList<HashMap<String, Object>> rowsReturn = new ArrayList<HashMap<String, Object>>();
		HashMap<String, Object> hItem = null;

		for (int i = 1; i <= 20; i++) {
			if (xPath.evaluate("DuLieu/TDiep[" + i + "]", nodeKetQuaTraCuu, XPathConstants.NODE) == null)
				break;
			nodeTDiep = (Node) xPath.evaluate("DuLieu/TDiep[" + i + "]", nodeKetQuaTraCuu, XPathConstants.NODE);
			CQT_MLTDiep = commons
					.getTextFromNodeXML((Element) xPath.evaluate("TTChung/MLTDiep", nodeTDiep, XPathConstants.NODE));
			stt += 1;
			if ("-1".equals(CQT_MLTDiep)) {
				String loi = commons
						.getTextFromNodeXML((Element) xPath.evaluate("DLieu/MLoi", nodeTDiep, XPathConstants.NODE));
				String MTLoi = commons
						.getTextFromNodeXML((Element) xPath.evaluate("DLieu/MTa", nodeTDiep, XPathConstants.NODE));
				hItem = new HashMap<String, Object>();
				hItem.put("STT", stt);
				hItem.put("Date", LocalDate.now().toString());
				hItem.put("MLoi", loi);
				hItem.put("MTLoi", MTLoi);
				rowsReturn.add(hItem);
			} else if ("999".equals(CQT_MLTDiep)) {
				String loi = commons.getTextFromNodeXML(
						(Element) xPath.evaluate("DLieu/TBao/TTTNhan", nodeTDiep, XPathConstants.NODE));
				if (loi.equals("0")) {
					loi = "Đã tiếp nhận";
				} else {
					loi = "Tiếp nhận xảy ra lỗi";
				}

				hItem = new HashMap<String, Object>();
				hItem.put("STT", stt);
				hItem.put("Date", commons.getTextFromNodeXML(
						(Element) xPath.evaluate("DLieu/TBao/NNhan", nodeTDiep, XPathConstants.NODE)));
				hItem.put("MLoi", CQT_MLTDiep);
				hItem.put("MTLoi", loi);
				rowsReturn.add(hItem);

			} else if ("213".equals(CQT_MLTDiep)) {

				String LTBao = commons.getTextFromNodeXML(
						(Element) xPath.evaluate("DLieu/TBao/DLTBao/LTBao", nodeTDiep, XPathConstants.NODE));
				hItem = new HashMap<String, Object>();
					if ("2".equals(LTBao)) {
						hItem = new HashMap<String, Object>();
						hItem.put("STT", stt);
						hItem.put("Date", commons.getTextFromNodeXML(
								(Element) xPath.evaluate("DLieu/HDon/DLHDon/TTChung/NLap", nodeTDiep, XPathConstants.NODE)));
						hItem.put("MLoi", CQT_MLTDiep);
						hItem.put("MTLoi", "Đã hoàn thành");
						rowsReturn.add(hItem);
					} else {
						hItem.put("STT", stt);
						hItem.put("Date", commons.getTextFromNodeXML(
								(Element) xPath.evaluate("DLieu/TBao/DLTBao/TGGui", nodeTDiep, XPathConstants.NODE)));
						hItem.put("MLoi", commons.getTextFromNodeXML((Element) xPath
								.evaluate("DLieu/TBao/DLTBao/LCTu/DSCTu/CTu/DSLDo/LDo/MLoi", nodeTDiep, XPathConstants.NODE)));
						hItem.put("MTLoi", commons.getTextFromNodeXML((Element) xPath
								.evaluate("DLieu/TBao/DLTBao/LCTu/DSCTu/CTu/DSLDo/LDo/MTLoi", nodeTDiep, XPathConstants.NODE)));
						rowsReturn.add(hItem);
					}
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
	public MsgRsp sendMailV1(JSONRoot jsonRoot) throws Exception {
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
		String _title = commons.getTextJsonNode(jsonData.at("/_title")).trim().replaceAll("\\s+", " ");
		String _email = commons.getTextJsonNode(jsonData.at("/_email")).trim().replaceAll("\\s+", " ");
		String _emailcc = commons.getTextJsonNode(jsonData.at("/_emailcc")).trim().replaceAll("\\s+", " ");
		String _content = commons.getTextJsonNode(jsonData.at("/_content")).trim().replaceAll("\\s+", " ");

		MsgRsp rsp = new MsgRsp(header);
		rsp.setMsgPage(page);
		MspResponseStatus responseStatus = null;

		ObjectId objectId = null;
		ObjectId objectIdIssu = null;
		try {
			objectId = new ObjectId(_id);
			objectIdIssu = new ObjectId(header.getIssuerId());
		} catch (Exception e) {
		}

		Document docFind = null;
		Document docTmp = null;
		List<Document> pipeline = null;

		try {
			docFind = new Document("IssuerId", header.getIssuerId()).append("_id", objectId);
			pipeline = new ArrayList<Document>();
			pipeline.add(new Document("$match", docFind));
			
			pipeline.add(
					new Document("$lookup",
							new Document("from", "Issuer")
									.append("pipeline",
											Arrays.asList(new Document("$match", new Document("_id", objectIdIssu)
													.append("IsDelete", new Document("$ne", true)))))
									.append("as", "Issuer")));
			pipeline.add(new Document("$unwind", new Document("path", "$Issuer").append("preserveNullAndEmptyArrays", true)));
			
			
			pipeline.add(new Document("$lookup", new Document("from", "ConfigEmail")
							.append("let", new Document("vIssuerId", "$IssuerId"))
							.append("pipeline", Arrays.asList(
												new Document("$match",new Document("$expr",
																		new Document("$eq", Arrays.asList("$IssuerId", "$$vIssuerId"))))))
							.append("as", "ConfigEmail")));
			pipeline.add(new Document("$unwind", new Document("path", "$ConfigEmail").append("preserveNullAndEmptyArrays", true)));
			
			
			pipeline.add(new Document("$lookup", new Document("from", "DMMSTNCN")
							.append("let",new Document("vIssuerId", "$IssuerId")
									.append("vMauSo","$MauSo"))
							.append("pipeline", Arrays.asList(
												new Document("$match", 
														new Document("$expr", 
																new Document("$and",Arrays.asList(
																		new Document("$eq", Arrays.asList("$$vIssuerId", "$IssuerId")),
																		new Document("$eq",Arrays.asList(new Document("$toString", "$_id"), "$$vMauSo")),
																		new Document("$eq", Arrays.asList("$IsDelete", false)),
							                                            new Document("$eq", Arrays.asList("$IsActive", true))
																		))))
											))
							.append("as", "DMMSTNCN")));
			pipeline.add(new Document("$unwind", new Document("path", "$DMMSTNCN").append("preserveNullAndEmptyArrays", true)));
			
			
			pipeline.add(new Document("$lookup", new Document("from", "UserConFig")
							.append("pipeline", Arrays.asList(
												new Document("$match",
															new Document("viewshd", "Y")
															.append("IssuerId", header.getIssuerId()))))
							.append("as", "UserConFig")));
			pipeline.add(new Document("$unwind", new Document("path", "$UserConFig").append("preserveNullAndEmptyArrays", true)));

			
			pipeline.add(new Document("$lookup", new Document("from", "ConfigMailJet")
							.append("pipeline", Arrays.asList(
									new Document("$match", 
											new Document("IsActive", true))))
							.append("as", "ConfigMailJet")));
			pipeline.add(new Document("$unwind", new Document("path", "$ConfigMailJet").append("preserveNullAndEmptyArrays", true)));

			
			pipeline.add(new Document("$lookup", new Document("from", "DMFooterWeb")
					.append("pipeline", Arrays.asList(
							new Document("$match", 
									new Document("IsActive", true)
									.append("IsDelete", false)),
							new Document("$project", new Document("Noidung", 1)),
							new Document("$limit", 1)))
					.append("as", "DMFooterWeb")));
			pipeline.add(new Document("$unwind", new Document("path", "$DMFooterWeb").append("preserveNullAndEmptyArrays", true)));
			
			pipeline.add(new Document("$lookup", new Document("from", "PramLink")
							.append("pipeline",Arrays.asList(
												new Document("$match",
														new Document("$expr", new Document("IsDelete", false))),
												new Document("$project", 
														new Document("_id", 1)
														.append("LinkPortal", 1))))
							.append("as", "PramLink")));
			pipeline.add(new Document("$unwind", new Document("path", "$PramLink").append("preserveNullAndEmptyArrays", true)));


			
			try (MongoClient mongoClient = cfg.mongoClient()){
				MongoCollection<Document> collection = mongoClient.getDatabase(cfg.dbName).getCollection("CTTNCNhan");
				docTmp = collection.aggregate(pipeline).allowDiskUse(true).iterator().next();
			} catch (Exception e) {
				
			}
		} catch (Exception e) {
			responseStatus = new MspResponseStatus(9999,
					"Cấu hình mail gửi hóa đơn không hợp lệ. Vui lòng cấu hình lại mail server!!!");
			rsp.setResponseStatus(responseStatus);
			return rsp;
		}

		if (null == docTmp) {
			responseStatus = new MspResponseStatus(9999, "Không tìm thấy thông tin chứng từ.");
			rsp.setResponseStatus(responseStatus);
			return rsp;
		}
		String link = docTmp.getEmbedded(Arrays.asList("PramLink", "LinkPortal"), "");
		if (docTmp.get("ConfigEmail") == null) {
			responseStatus = new MspResponseStatus(9999, "Không tìm thấy thông tin email gửi.");
			rsp.setResponseStatus(responseStatus);
			return rsp;
		}

		String CheckFooterMail = docTmp.getEmbedded(Arrays.asList("UserConFig", "footermail"), "");
		if (!CheckFooterMail.equals("Y")) {
			_content = commons.decodeURIComponent(_content);
			String noidung = docTmp.getEmbedded(Arrays.asList("DMFooterWeb", "Noidung"), "");
			_content += noidung;
		} else {
			_content = commons.decodeURIComponent(_content);
		}

		String MailJet = docTmp.getEmbedded(Arrays.asList("ConfigEmail", "MailJet"), "");
		String dir = docTmp.getString("Dir");
		String statusCode = docTmp.get("SignStatus", "");
		String status = docTmp.get("Status", "");
		String soHD = commons.formatNumberBillInvoice(docTmp.get("SCTu", 0));
		String mauHD = docTmp.get("KHCTu", "");
		
		String fileNamePDF = _id + ".pdf";
		if (Constants.INVOICE_STATUS.DELETED.equals(status))
			fileNamePDF = _id + "-deleted.pdf";
		String fileName = _id + ".xml";
		String fileNameXML = _id + ".xml";
		File file = null;
		/* KIEM TRA XEM CO FILE PDF CHUA; NEU CHUA CO THI TAO FILE PDF */
		if (docTmp.get("DMMSTNCN") != null) {
			fileName = _id + ".xml";
			if ("SIGNED".equals(statusCode)) {
				fileName = _id + "_signed.xml";
			}
			
			file = new File(dir, fileName);
			if (file.exists() && file.isFile()) {
				String imgLogo = docTmp.getEmbedded(Arrays.asList("DMMSTNCN", "LoGo"), "");
				
				String kh = docTmp.get("KHCTu", "");
				String ms = docTmp.getEmbedded(Arrays.asList("DMMSTNCN", "MauSo"), "");

				org.w3c.dom.Document doc = commons.fileToDocument(file);
				
				String fileNameJP = docTmp.getEmbedded(Arrays.asList("DMMSTNCN", "FileName"), "");
				File fileJP = new File(SystemParams.DIR_E_INVOICE_TEMPLATE, fileNameJP);
				
				ByteArrayOutputStream baosPDF = null;
				baosPDF = jpUtils.viewpdfcttncnV1(fileJP, doc, docTmp,
						Paths.get(SystemParams.DIR_E_INVOICE_TEMPLATE, "MauSoTNCN",
								docTmp.getEmbedded(Arrays.asList("Issuer", "TaxCode"), ""), imgLogo).toString(),
						kh, ms, link, false);
				
				if (null != baosPDF) {
					try (OutputStream fileOuputStream = new FileOutputStream(new File(dir, fileNamePDF))) {
						baosPDF.writeTo(fileOuputStream);
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}
		}
		/* END - KIEM TRA XEM CO FILE PDF CHUA; NEU CHUA CO THI TAO FILE PDF */
		List<String> listFiles = new ArrayList<>();
		List<String> listNames = new ArrayList<>();
		
		if ("SIGNED".equals(statusCode)) {
			fileNameXML = _id + "_signed.xml";
		}
		file = new File(dir, fileNameXML);
		if (file.exists() && file.isFile()) {
			listFiles.add(file.toString());
			listNames.add(mauHD + "-" + soHD + ".xml");
		}
		
		file = new File(dir, fileNamePDF);
		if (file.exists() && file.isFile()) {
			listFiles.add(file.toString());
			listNames.add(mauHD + "-" + soHD + ".pdf");
		}
		
		/* THUC HIEN GUI MAIL */
		MailConfig mailConfig = new MailConfig(docTmp.get("ConfigEmail", Document.class));
		mailConfig.setNameSend(docTmp.getEmbedded(Arrays.asList("InfoCreated", "CreateUserFullName"), ""));

		boolean boo = false;

		String email_gui = "";

		if (_email != "" || _emailcc != "") {
			if (_email != "" && _emailcc == "") {
				email_gui = _email;
			} else if (_email == "" && _emailcc != "") {
				email_gui = _emailcc;
			} else {
				email_gui = _email + "," + _emailcc;
			}
			
			// KIỂM TRA GỬI MAIL THƯỜNG HAY MAILJET
			if (MailJet.equals("Y") && !MailJet.equals("") && !MailJet.equals("N") && !email_gui.equals("")) {
				String ApiKey = docTmp.getEmbedded(Arrays.asList("ConfigMailJet", "ApiKey"), "");
				String SecretKey = docTmp.getEmbedded(Arrays.asList("ConfigMailJet", "SecretKey"), "");
				String EmailAddress = docTmp.getEmbedded(Arrays.asList("ConfigMailJet", "EmailAddress"), "");
				mailConfig.setEmailAddress(ApiKey);
				mailConfig.setEmailPassword(SecretKey);
				mailConfig.setSmtpServer(EmailAddress);
				boo = mailJet.sendMailJet(mailConfig, _title, _content, email_gui, listFiles, listNames, true);

			} else {
				boo = mailUtils.sendMail(mailConfig, _title, _content, email_gui, listFiles, listNames, true);
			}
			
			
			try {
				MongoClient mongoClient = cfg.mongoClient();
				MongoCollection<Document> collection = mongoClient.getDatabase(cfg.dbName)
						.getCollection("LogEmailUser");
				collection.insertOne(new Document("IssuerId", header.getIssuerId()).append("Title", _title)
						.append("Email", email_gui).append("IsActive", boo).append("MailCheck", boo)
						.append("IsDelete", false).append("EmailContent", _content)

				);
				mongoClient.close();

				/* LOG BAO CAO THONG KE */
				String name = docTmp.get("Name", "");
				String code = docTmp.get("Code", "");
				String address = docTmp.get("Address", "");
				String contactPhone = docTmp.get("ContactPhone", "");
				String taxCode = docTmp.get("TaxCode", "");
				String kyHieu = docTmp.get("KyHieu", "");
				String cccd = docTmp.getEmbedded(Arrays.asList("CMND-CCCD", "CCCD"), "");
				String kyBaoCao = docTmp.get("KyBaoCao", "");
				String tuNgay = docTmp.get("TuNgay", "");
				String denNgay = docTmp.get("DenNgay", "");
				int shd = docTmp.get("SHDon", 0);
				String date = docTmp.get("Date", "");

				mongoClient = cfg.mongoClient();
				collection = mongoClient.getDatabase(cfg.dbName).getCollection("BaoCaoThongKeCTTNCN");
				collection.insertOne(
						new Document("IssuerId", header.getIssuerId())
						.append("Name", name)
						.append("Code", code)
						.append("Address", address)
						.append("ContactPhone", contactPhone)
						.append("TaxCode", taxCode)
						.append("KyHieu", kyHieu)
						.append("CCCD", cccd)
						.append("KyBaoCao", kyBaoCao)
						.append("TuNgay", tuNgay)
						.append("DenNgay", denNgay)
						.append("SHDon", shd)
						.append("EmailGuiCTTNCN", email_gui)
						.append("Date", date)
						.append("IsDelete", false));
				mongoClient.close();

			} catch (Exception ex) {
			}
		}
		responseStatus = new MspResponseStatus(0, "SUCCESS");
		rsp.setResponseStatus(responseStatus);
		return rsp;
	}
}
