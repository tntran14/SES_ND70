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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoField;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
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
import org.springframework.data.mongodb.core.MongoTemplate;
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
import com.fasterxml.jackson.databind.JsonNode;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.FindOneAndUpdateOptions;
import com.mongodb.client.model.ReturnDocument;

import vn.sesgroup.hddt.configuration.ConfigConnectMongo;
import vn.sesgroup.hddt.dto.FileInfo;
import vn.sesgroup.hddt.dto.GetXMLInfoXMLDTO;
import vn.sesgroup.hddt.dto.MailConfig;
import vn.sesgroup.hddt.model.DSHHDVu;
import vn.sesgroup.hddt.model.EInvoiceExcelForm;
import vn.sesgroup.hddt.model.EInvoiceMTTExcelForm;
import vn.sesgroup.hddt.resources.JmsParams;
import vn.sesgroup.hddt.user.dao.AbstractDAO;
import vn.sesgroup.hddt.user.dao.EInvoiceMTTDAO;
import vn.sesgroup.hddt.user.dao.SendMailAsyncDAO;
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
public class EInvoiceMTTImpl extends AbstractDAO implements EInvoiceMTTDAO {
    private static final Logger logger = LogManager.getLogger(EInvoiceMTTImpl.class);
    private final ConcurrentHashMap<String, ReentrantLock> issuerLocks_sign = new ConcurrentHashMap<>();

	@Autowired
	MongoTemplate mongoTemplate;

	@Autowired
	ConfigConnectMongo cfg;

	@Autowired
	private JmsTemplate jmsTemplate;

	@Autowired
	SendMailAsyncDAO sendMailAsyncDAO;
	
	@Autowired
	TCTNService tctnService;
	
	@Autowired
	JPUtils jpUtils;
	Document docUpsert = null;

	private MailUtils mailUtils = new MailUtils();

	private MailjetSender mailJet = new MailjetSender();

	@SuppressWarnings("unused")
	@Transactional(rollbackFor = { Exception.class })
//	@Transactional(propagation = Propagation.REQUIRED, rollbackFor = Exception.class)
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
		String _id_tt_dc = commons.getTextJsonNode(jsonData.at("/_id_tt_dc")).replaceAll("\\s", "");
		String mauSoHdon = commons.getTextJsonNode(jsonData.at("/MauSoHdon")).replaceAll("\\s", "");

		String loaiHoaDon = commons.getTextJsonNode(jsonData.at("/LoaiHoaDon")).replaceAll("\\s", "");
		String loaiHoaDonText = commons.getTextJsonNode(jsonData.at("/LoaiHoaDonText")).trim().replaceAll("\\s+", " ");

//		String maHoadon = commons.getTextJsonNode(jsonData.at("/MaHoaDon")).trim().replaceAll("\\s+", " ");
//		String _token = commons.getTextJsonNode(jsonData.at("/_token"));
//		String tenLoaiHd = commons.getTextJsonNode(jsonData.at("/TenLoaiHd")).trim().replaceAll("\\s+", " ");
		
		String ngayLap = commons.getTextJsonNode(jsonData.at("/NgayLap")).replaceAll("\\s", "");
		String hinhThucThanhToan = commons.getTextJsonNode(jsonData.at("/HinhThucThanhToan")).replaceAll("\\s", "");
		String hinhThucThanhToanText = commons.getTextJsonNode(jsonData.at("/HinhThucThanhToanText")).trim()
				.replaceAll("\\s+", " ");
		String khMst = commons.getTextJsonNode(jsonData.at("/KhMst")).trim().replaceAll("\\s+", "")
				.replaceAll("[+^%$#@&*]*", "").replaceAll("[a-z][A-Z]*", "");
//		String khMKHang = commons.getTextJsonNode(jsonData.at("/KhMKHang")).trim().replaceAll("\\s+", " ");

		String khCCCDan = commons.getTextJsonNode(jsonData.at("/KhCCCDan")).trim().replaceAll("\\s+", " ");

		String khHoTenNguoiMua = commons.getTextJsonNode(jsonData.at("/KhHoTenNguoiMua")).trim().replaceAll("\\s+",
				" ");
		String khTenDonVi = commons.getTextJsonNode(jsonData.at("/KhTenDonVi")).trim().replaceAll("\\s+", " ");
		String khDiaChi = commons.getTextJsonNode(jsonData.at("/KhDiaChi")).trim().replaceAll("\\s+", " ");
		String khEmail = commons.getTextJsonNode(jsonData.at("/KhEmail")).trim().replaceAll("\\s+", " ");
		String khSoDT = commons.getTextJsonNode(jsonData.at("/KhSoDt")).trim().replaceAll("\\s+", " ");
		String khSoTk = commons.getTextJsonNode(jsonData.at("/KhSoTk")).trim().replaceAll("\\s+", " ");
		String khTkTaiNganHang = commons.getTextJsonNode(jsonData.at("/KhTkTaiNganHang")).trim().replaceAll("\\s+",
				" ");
		String tongTienTruocThue = commons.getTextJsonNode(jsonData.at("/TongTienTruocThue")).trim().replaceAll("\\s+",
				" ");
		String loaiTienTt = commons.getTextJsonNode(jsonData.at("/LoaiTienTt")).trim().replaceAll("\\s+", " ");
		String tyGia = commons.getTextJsonNode(jsonData.at("/TyGia")).trim().replaceAll("\\s+", " ");

		String tongTienThueGtgt = commons.getTextJsonNode(jsonData.at("/TongTienThueGtgt")).trim().replaceAll("\\s+",
				" ");
		String tongTienDaCoThue = commons.getTextJsonNode(jsonData.at("/TongTienDaCoThue")).trim().replaceAll("\\s+",
				" ");
		String tongTienQuyDoi = commons.getTextJsonNode(jsonData.at("/TongTienQuyDoi")).trim().replaceAll("\\s+", " ");
		String tienBangChu = commons.getTextJsonNode(jsonData.at("/TienBangChu")).trim().replaceAll("\\s+", " ");

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
		Document docFind = null;
		Document docTmp = null;
		Document docFind1 = null;
		Document docTmp1 = null;
		Document docUpsert = null;
		FindOneAndUpdateOptions options = null;
		Document docEInvoiceTTDC = null;
		Document docTTHDLQuan = null;

		String secureKey = "";
		String fileNameXML = "";
		String pathDir = "";
		Path path = null;
		File file = null;

		String taxCode = "";
		HashMap<String, Double> mapVATAmount = null;
		HashMap<String, Double> mapAmount = null;
		List<Object> listDSHHDVu = new ArrayList<Object>();
		HashMap<String, Object> hItem = null;

		String MTDiep = "";
		String MST = "";

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
				objectIdMSKH = new ObjectId(mauSoHdon);
			} catch (Exception e) {
			}

			try {
				if (!"".equals(_id_tt_dc))
					objectIdTT_DC = new ObjectId(_id_tt_dc);
			} catch (Exception e) {
			}
			/* KIEM TRA THONG TIN KHACH HANG - USERS */

			pipeline = new ArrayList<Document>();
			pipeline.add(new Document("$match", new Document("_id", objectId).append("IsActive", true)
					.append("IsDelete", new Document("$ne", true))));

			pipeline.add(new Document("$lookup", new Document("from", "Users").append("pipeline", Arrays.asList(
					new Document("$match",
							new Document("IssuerId", header.getIssuerId()).append("_id", objectIdUser)
									.append("IsActive", true).append("IsDelete", new Document("$ne", true))),

					new Document("$project", new Document("_id", 1).append("UserName", 1).append("FullName", 1)),
					new Document("$limit", 1))).append("as", "UserInfo")));
			pipeline.add(new Document("$unwind",
					new Document("path", "$UserInfo").append("preserveNullAndEmptyArrays", true)));

			pipeline.add(new Document("$lookup", new Document("from", "DMMauSoKyHieu")
					.append("pipeline", Arrays.asList(new Document("$match",
							new Document("IssuerId", header.getIssuerId()).append("IsActive", true)
									.append("IsDelete", new Document("$ne", true)).append("_id", objectIdMSKH))))
					.append("as", "DMMauSoKyHieu")));
			pipeline.add(new Document("$unwind",
					new Document("path", "$DMMauSoKyHieu").append("preserveNullAndEmptyArrays", true)));

			if (!"".equals(_id_tt_dc)) {
				pipeline.add(new Document("$lookup", new Document("from", "EInvoiceMTT")
						.append("pipeline", Arrays.asList(new Document("$match", new Document("_id", objectIdTT_DC)
								.append("IssuerId", header.getIssuerId())
								.append("EInvoiceStatus",
										new Document("$in", Arrays.asList(Constants.INVOICE_STATUS.COMPLETE,
												Constants.INVOICE_STATUS.ADJUSTED, Constants.INVOICE_STATUS.REPLACED)))
								.append("MCCQT", new Document("$exists", true))
								.append("HDSS.TCTBao", new Document("$in", Arrays.asList("2", "3")))),
								new Document("$project",
										new Document("TT_DC", "$EInvoiceDetail.TTChung").append("HDSS", 1))))
						.append("as", "EInvoiceTTDC")));
				pipeline.add(new Document("$unwind",
						new Document("path", "$EInvoiceTTDC").append("preserveNullAndEmptyArrays", true)));
			}

			pipeline.add(new Document("$lookup",
					new Document("from", "PramLink")
							.append("pipeline",
									Arrays.asList(
											new Document("$match",
													new Document("$expr", new Document("IsDelete", false))),
											new Document("$project", new Document("_id", 1).append("LinkPortal", 1))))
							.append("as", "PramLink")));
			pipeline.add(new Document("$unwind",
					new Document("path", "$PramLink").append("preserveNullAndEmptyArrays", true)));

			MongoClient mongoClient = cfg.mongoClient();
			MongoCollection<Document> collection = mongoClient.getDatabase(cfg.dbName).getCollection("Issuer");
			try {
				docTmp = collection.aggregate(pipeline).allowDiskUse(true).iterator().next();
			} catch (Exception e) {

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
			if (docTmp.get("DMMauSoKyHieu") == null) {
				responseStatus = new MspResponseStatus(9999, "Không tìm thấy thông tin mẫu số.");
				rsp.setResponseStatus(responseStatus);
				return rsp;
			}

			docEInvoiceTTDC = docTmp.get("EInvoiceTTDC", Document.class);
			String link = docTmp.getEmbedded(Arrays.asList("PramLink", "LinkPortal"), "");
			int intTmp = 0;
			taxCode = docTmp.getString("TaxCode");

			secureKey = commons.csRandomNumbericString(6);
			objectIdEInvoice = new ObjectId();
			path = Paths.get(SystemParams.DIR_E_INVOICE_DATA, taxCode,
					docTmp.getEmbedded(Arrays.asList("DMMauSoKyHieu", "_id"), ObjectId.class).toString());
			pathDir = path.toString();
			file = path.toFile();
			if (!file.exists())
				file.mkdirs();
			/* TAO XML HOA DON */
			fileNameXML = objectIdEInvoice.toString() + ".xml";

			dbf = DocumentBuilderFactory.newInstance();
			db = dbf.newDocumentBuilder();
			doc = db.newDocument();
			doc.setXmlStandalone(true);

			root = doc.createElement("HDon");
			root.setAttribute("Id", "DLieu0");
			doc.appendChild(root);

			elementContent = doc.createElement("DLHDon");
			elementContent.setAttribute("Id", "data");
			root.appendChild(elementContent);

			elementSubTmp = null;
			elementSubTmp01 = null;
			elementSubContent = doc.createElement("TTChung");
			elementTmp = null;

			elementSubContent.appendChild(commons.createElementWithValue(doc, "PBan", SystemParams.VERSION_XML));
			elementSubContent.appendChild(commons.createElementWithValue(doc, "THDon", loaiHoaDonText));
			elementSubContent.appendChild(commons.createElementWithValue(doc, "KHMSHDon",
					docTmp.getEmbedded(Arrays.asList("DMMauSoKyHieu", "KHMSHDon"), "")));
			elementSubContent.appendChild(commons.createElementWithValue(doc, "KHHDon",
					docTmp.getEmbedded(Arrays.asList("DMMauSoKyHieu", "KHHDon"), "")));
			elementSubContent.appendChild(commons.createElementWithValue(doc, "SHDon", "")); // SE PHAT SINH KHI KY
			// Ngày lập
			elementSubContent.appendChild(
					commons.createElementWithValue(doc, "NLap", commons.convertLocalDateTimeStringToString(ngayLap,
							Constants.FORMAT_DATE.FORMAT_DATE_WEB, Constants.FORMAT_DATE.FORMAT_DATE_EINVOICE, false)));

			// Đơn vị tiền tệ
			elementSubContent.appendChild(commons.createElementWithValue(doc, "DVTTe", loaiTienTt));
			// Tỷ giá
			elementSubContent.appendChild(commons.createElementWithValue(doc, "TGia", tyGia));
			// Hình thức thanh toán
			elementSubContent.appendChild(commons.createElementWithValue(doc, "HTTToan", hinhThucThanhToanText));
			// MST tổ chức cung cấp giải pháp HĐĐT
			elementSubContent.appendChild(commons.createElementWithValue(doc, "MSTTCGP", SystemParams.MSTTCGP));
////			ghi chu 
//			elementSubContent.appendChild(commons.createElementWithValue(doc, "GChu", ""));

			elementTmp = doc.createElement("TTKhac"); // THONG TIN KHAC
			elementTmp.appendChild(commons.createElementTTKhac(doc, "HoaDon_Loai", "string", loaiHoaDon));
			elementTmp.appendChild(commons.createElementTTKhac(doc, "PortalLink", "string", link));
			elementTmp.appendChild(commons.createElementTTKhac(doc, "SecureKey", "string", secureKey));
			elementTmp
					.appendChild(commons.createElementTTKhac(doc, "SystemKey", "string", objectIdEInvoice.toString()));
			elementSubContent.appendChild(elementTmp);

			if (docEInvoiceTTDC != null) {
				elementTmp = doc.createElement("TTHDLQuan");
				elementTmp.appendChild(commons.createElementWithValue(doc, "TCHDon",
						"3".equals(docEInvoiceTTDC.getEmbedded(Arrays.asList("HDSS", "TCTBao"), "")) ? "1" : "2"));
				elementTmp.appendChild(commons.createElementWithValue(doc, "LHDCLQuan", "1"));
				elementTmp.appendChild(commons.createElementWithValue(doc, "KHMSHDCLQuan",
						docEInvoiceTTDC.getEmbedded(Arrays.asList("TT_DC", "KHMSHDon"), "")));
				elementTmp.appendChild(commons.createElementWithValue(doc, "KHHDCLQuan",
						docEInvoiceTTDC.getEmbedded(Arrays.asList("TT_DC", "KHHDon"), "")));
				elementTmp.appendChild(commons.createElementWithValue(doc, "SHDCLQuan",
						String.valueOf(docEInvoiceTTDC.getEmbedded(Arrays.asList("TT_DC", "SHDon"), 0))));
				elementTmp.appendChild(commons.createElementWithValue(doc, "NLHDCLQuan",
						commons.convertLocalDateTimeToString(
								commons.convertDateToLocalDateTime(
										docEInvoiceTTDC.getEmbedded(Arrays.asList("TT_DC", "NLap"), Date.class)),
								"yyyy-MM-dd")));
				elementTmp.appendChild(commons.createElementWithValue(doc, "GChu", ""));
				elementSubContent.appendChild(elementTmp);
			}

			elementContent.appendChild(elementSubContent);

			// NDHDon: Nội dung hóa đơn
			elementSubContent = doc.createElement("NDHDon");
			elementTmp = doc.createElement("NBan"); // NGUOI BAN
			elementTmp.appendChild(commons.createElementWithValue(doc, "Ten", docTmp.get("Name", "")));
			elementTmp.appendChild(commons.createElementWithValue(doc, "MST", docTmp.get("TaxCode", "")));
			elementTmp.appendChild(commons.createElementWithValue(doc, "DChi", docTmp.get("Address", "")));
			elementTmp.appendChild(commons.createElementWithValue(doc, "SDThoai", docTmp.get("Phone", "")));

			elementTmp.appendChild(commons.createElementWithValue(doc, "DCTDTu", docTmp.get("Email", "")));
			elementTmp.appendChild(commons.createElementWithValue(doc, "STKNHang",
					docTmp.getEmbedded(Arrays.asList("BankAccount", "AccountNumber"), "")));
			elementTmp.appendChild(commons.createElementWithValue(doc, "TNHang",
					docTmp.getEmbedded(Arrays.asList("BankAccount", "BankName"), "")));

			elementTmp.appendChild(commons.createElementWithValue(doc, "Fax", docTmp.get("Fax", "")));
			elementTmp.appendChild(commons.createElementWithValue(doc, "Website", docTmp.get("Website", "")));
//			elementTmp.appendChild(commons.createElementWithValue(doc, "TTKhac", ""));

			elementSubTmp = doc.createElement("TTKhac");

			if (!docTmp.get("NameEN", "").equals("")) {
				elementSubTmp
						.appendChild(commons.createElementTTKhac(doc, "TenEN", "string", docTmp.get("NameEN", "")));
			}
			elementTmp.appendChild(elementSubTmp);

			/* ADD THONG TIN TK NGAN HANG (NEU CO) */
			if (docTmp.get("BankAccountExt") != null && docTmp.getList("BankAccountExt", Document.class).size() > 0) {
				intTmp = 1;
//							elementTmp = doc.createElement("TTKhac");
//							elementSubTmp = doc.createElement("TTKhac");
				for (Document oo : docTmp.getList("BankAccountExt", Document.class)) {
					elementSubTmp.appendChild(commons.createElementTTKhac(doc, "STKNHang" + intTmp, "string",
							oo.get("AccountNumber", "")));
					elementSubTmp.appendChild(
							commons.createElementTTKhac(doc, "TNHang" + intTmp, "string", oo.get("BankName", "")));
					elementTmp.appendChild(elementSubTmp);
					intTmp++;
				}
			}
			elementSubContent.appendChild(elementTmp);

			elementTmp = doc.createElement("NMua"); // NGUOI MUA
			elementTmp.appendChild(commons.createElementWithValue(doc, "Ten", khTenDonVi));
			elementTmp.appendChild(commons.createElementWithValue(doc, "MST", khMst));
			elementTmp.appendChild(commons.createElementWithValue(doc, "DChi", khDiaChi));
			elementTmp.appendChild(commons.createElementWithValue(doc, "SDThoai", khSoDT));
			elementTmp.appendChild(commons.createElementWithValue(doc, "CCCDan", khCCCDan));
			elementTmp.appendChild(commons.createElementWithValue(doc, "DCTDTu", khEmail));
			elementTmp.appendChild(commons.createElementWithValue(doc, "HVTNMHang", khHoTenNguoiMua));
			elementTmp.appendChild(commons.createElementWithValue(doc, "STKNHang", khSoTk));
			elementTmp.appendChild(commons.createElementWithValue(doc, "TNHang", khTkTaiNganHang));
			elementTmp.appendChild(commons.createElementWithValue(doc, "TTKhac", ""));

			elementSubContent.appendChild(elementTmp);

			mapVATAmount = new LinkedHashMap<String, Double>();
			mapAmount = new LinkedHashMap<String, Double>();
			elementTmp = doc.createElement("DSHHDVu"); // HH-DV
			if (!jsonData.at("/DSSanPham").isMissingNode()) {
				for (JsonNode o : jsonData.at("/DSSanPham")) {
					if (!"".equals(commons.getTextJsonNode(o.at("/ProductName")))) {
						tmp = commons.getTextJsonNode(o.at("/VATRate")).replaceAll(",", "");
						switch (tmp) {
						case "0":
							tmp = "-1";
							break;
						case "1":
							tmp = "0%";
							break;
						case "5":
						case "8":
						case "10":
							tmp += "%";
							break;
						case "-1":
							tmp = "KCT";
							break;
						case "-2":
							tmp = "KKKNT";
							break;
						default:
							break;
						}

//									if(!("2".equals(commons.getTextJsonNode(o.at("/Feature"))))) {		// || "4".equals(commons.getTextJsonNode(o.at("/Feature")))
						if ("1".equals(commons.getTextJsonNode(o.at("/Feature")))
								|| "3".equals(commons.getTextJsonNode(o.at("/Feature")))
								|| "4".equals(commons.getTextJsonNode(o.at("/Feature")))) {
							mapAmount.compute(tmp, (k, v) -> {
								return (v == null
										? commons.ToNumber(commons.getTextJsonNode(o.at("/Total")))
												* ("3".equals(commons.getTextJsonNode(o.at("/Feature"))) ? -1 : 1)
										: v + commons.ToNumber(commons.getTextJsonNode(o.at("/Total")))
												* ("3".equals(commons.getTextJsonNode(o.at("/Feature"))) ? -1 : 1));
							});
							mapVATAmount.compute(tmp, (k, v) -> {
								return (v == null
										? commons.ToNumber(commons.getTextJsonNode(o.at("/VATAmount")))
												* ("3".equals(commons.getTextJsonNode(o.at("/Feature"))) ? -1 : 1)
										: v + commons.ToNumber(commons.getTextJsonNode(o.at("/VATAmount")))
												* ("3".equals(commons.getTextJsonNode(o.at("/Feature"))) ? -1 : 1));
							});
						}

						elementSubTmp = doc.createElement("HHDVu");
						elementSubTmp.appendChild(commons.createElementWithValue(doc, "TChat",
								commons.getTextJsonNode(o.at("/Feature"))));
						elementSubTmp.appendChild(
								commons.createElementWithValue(doc, "STT", commons.getTextJsonNode(o.at("/STT"))));
						elementSubTmp.appendChild(commons.createElementWithValue(doc, "MHHDVu",
								commons.getTextJsonNode(o.at("/ProductCode"))));
						elementSubTmp.appendChild(commons.createElementWithValue(doc, "THHDVu",
								commons.getTextJsonNode(o.at("/ProductName"))));

						elementSubTmp.appendChild(
								commons.createElementWithValue(doc, "DVTinh", commons.getTextJsonNode(o.at("/Unit"))));
						if (!("2".equals(commons.getTextJsonNode(o.at("/Feature"))))) { // ||
																						// "4".equals(commons.getTextJsonNode(o.at("/Feature")))
							elementSubTmp.appendChild(commons.createElementWithValue(doc, "SLuong",
									commons.getTextJsonNode(o.at("/Quantity")).replaceAll(",", "")));
							elementSubTmp.appendChild(commons.createElementWithValue(doc, "DGia",
									commons.getTextJsonNode(o.at("/Price")).replaceAll(",", "")));
							elementSubTmp.appendChild(commons.createElementWithValue(doc, "TLCKhau", ""));
							elementSubTmp.appendChild(commons.createElementWithValue(doc, "STCKhau", ""));
							elementSubTmp.appendChild(commons.createElementWithValue(doc, "ThTien",
									commons.getTextJsonNode(o.at("/Total")).replaceAll(",", "")));

							if (!tmp.equals("-1") && !loaiHoaDon.equals("2")) {
								elementSubTmp.appendChild(commons.createElementWithValue(doc, "TSuat", tmp));
							}

						}
						if (!loaiHoaDon.equals("2")) {
							elementSubTmp01 = doc.createElement("TTKhac");

							if (commons.getTextJsonNode(o.at("/VATAmount")).replaceAll(",", "").equals("")) {
								elementSubTmp01
										.appendChild(commons.createElementTTKhac(doc, "VATAmount", "decimal", "0"));
							} else {
								elementSubTmp01.appendChild(commons.createElementTTKhac(doc, "VATAmount", "decimal",
										commons.getTextJsonNode(o.at("/VATAmount")).replaceAll(",", "")));
							}

							elementSubTmp01.appendChild(commons.createElementTTKhac(doc, "Amount", "decimal",
									commons.getTextJsonNode(o.at("/Amount")).replaceAll(",", "")));
							elementSubTmp.appendChild(elementSubTmp01);

							elementTmp.appendChild(elementSubTmp);
							double a = commons.ToNumber(commons.getTextJsonNode(o.at("/VATRate")));

							hItem = new LinkedHashMap<String, Object>();
							hItem.put("STT", commons.getTextJsonNode(o.at("/STT")));
							hItem.put("ProductName", commons.getTextJsonNode(o.at("/ProductName")));
							hItem.put("ProductCode", commons.getTextJsonNode(o.at("/ProductCode")));
							hItem.put("Unit", commons.getTextJsonNode(o.at("/Unit")));
							hItem.put("Quantity", commons.ToNumber(commons.getTextJsonNode(o.at("/Quantity"))));
							hItem.put("Price", commons.ToNumber(commons.getTextJsonNode(o.at("/Price"))));
							hItem.put("Total", commons.ToNumber(commons.getTextJsonNode(o.at("/Total"))));
							double vatRate = 0.0;
							if (a == 0.0) {

							} else if (a == 1.0) {
								hItem.put("VATRate", vatRate);
							} else {
								hItem.put("VATRate", commons.ToNumber(commons.getTextJsonNode(o.at("/VATRate"))));
							}

							hItem.put("VATAmount", commons.ToNumber(commons.getTextJsonNode(o.at("/VATAmount"))));
							hItem.put("Amount", commons.ToNumber(commons.getTextJsonNode(o.at("/Amount"))));
							hItem.put("Feature", commons.getTextJsonNode(o.at("/Feature")));
							listDSHHDVu.add(hItem);
						} else {
							elementSubTmp01 = doc.createElement("TTKhac");
//							elementSubTmp01.appendChild(commons.createElementTTKhac(doc, "VATAmount", "decimal",
//									commons.getTextJsonNode(o.at("/VATAmount")).replaceAll(",", "")));
//							
							if (commons.getTextJsonNode(o.at("/VATAmount")).replaceAll(",", "").equals("")) {
								elementSubTmp01
										.appendChild(commons.createElementTTKhac(doc, "VATAmount", "decimal", "0"));
							} else {
								elementSubTmp01.appendChild(commons.createElementTTKhac(doc, "VATAmount", "decimal",
										commons.getTextJsonNode(o.at("/VATAmount")).replaceAll(",", "")));
							}
							
							elementSubTmp01.appendChild(commons.createElementTTKhac(doc, "Amount", "decimal",
									commons.getTextJsonNode(o.at("/Amount")).replaceAll(",", "")));
							elementSubTmp01.appendChild(commons.createElementTTKhac(doc, "Stt", "numeric",
									commons.getTextJsonNode(o.at("/STT"))));
							elementSubTmp.appendChild(elementSubTmp01);

							elementTmp.appendChild(elementSubTmp);
							hItem = new LinkedHashMap<String, Object>();
							hItem.put("STT", commons.getTextJsonNode(o.at("/STT")));
							hItem.put("ProductName", commons.getTextJsonNode(o.at("/ProductName")));
							hItem.put("Unit", commons.getTextJsonNode(o.at("/Unit")));
							hItem.put("Quantity", commons.ToNumber(commons.getTextJsonNode(o.at("/Quantity"))));
							hItem.put("Price", commons.ToNumber(commons.getTextJsonNode(o.at("/Price"))));
							hItem.put("Total", commons.ToNumber(commons.getTextJsonNode(o.at("/Total"))));
							hItem.put("VATRate", null);
							hItem.put("VATAmount", null);
							hItem.put("Amount", commons.ToNumber(commons.getTextJsonNode(o.at("/Amount"))));
							hItem.put("Feature", commons.getTextJsonNode(o.at("/Feature")));
							listDSHHDVu.add(hItem);
						}

					

					}

				}
			}
			
			
			if (!loaiHoaDon.equals("2")) {
			elementSubContent.appendChild(elementTmp);
			elementTmp = doc.createElement("TToan"); // Thong tin thanh toan
			elementSubTmp = doc.createElement("THTTLTSuat");
			/* DANH SACH CAC LOAI THUE SUAT */
			for (Map.Entry<String, Double> pair : mapVATAmount.entrySet()) {
				if (null != pair.getKey() && !"".equals(pair.getKey())) {
					if (!pair.getKey().equals("-1")) {
						elementSubTmp01 = doc.createElement("LTSuat");
						elementSubTmp01.appendChild(commons.createElementWithValue(doc, "TSuat", pair.getKey()));
						elementSubTmp01.appendChild(commons.createElementWithValue(doc, "ThTien",
								commons.formatNumberReal(mapAmount.get(pair.getKey())).replaceAll(",", "")));

						if (loaiTienTt.equals("VND")) {
							elementSubTmp01.appendChild(commons.createElementWithValue(doc, "TThue",
									String.format("%.0f", mapVATAmount.get(pair.getKey()))));
						} else {
//						elementSubTmp01.appendChild(commons.createElementWithValue(doc, "TThue",String.valueOf(mapVATAmount.get(pair.getKey()))));	
							elementSubTmp01.appendChild(commons.createElementWithValue(doc, "TThue",
									String.format("%.2f", mapVATAmount.get(pair.getKey()))));
						}

						elementSubTmp.appendChild(elementSubTmp01);
					}
				}
			}
			elementTmp.appendChild(elementSubTmp);
			elementTmp.appendChild(
					commons.createElementWithValue(doc, "TgTCThue", tongTienTruocThue.replaceAll(",", "")));
			elementTmp
					.appendChild(commons.createElementWithValue(doc, "TgTThue", tongTienThueGtgt.replaceAll(",", "")));

			elementSubTmp = doc.createElement("DSLPhi");
			elementSubTmp01 = doc.createElement("LPhi");
			elementSubTmp01.appendChild(commons.createElementWithValue(doc, "TLPhi", ""));
			elementSubTmp01.appendChild(commons.createElementWithValue(doc, "TPhi", "0"));
			elementSubTmp.appendChild(elementSubTmp01);

			elementTmp.appendChild(elementSubTmp);

			elementTmp.appendChild(commons.createElementWithValue(doc, "TTCKTMai", "0"));
			elementTmp
					.appendChild(commons.createElementWithValue(doc, "TgTTTBSo", tongTienDaCoThue.replaceAll(",", "")));
			elementTmp.appendChild(commons.createElementWithValue(doc, "TgTTTBChu", tienBangChu));
			elementSubTmp = doc.createElement("TTKhac");
			if (!"VND".equals(loaiTienTt)) {
				elementSubTmp.appendChild(
						commons.createElementTTKhac(doc, "TgTQDoi", "decimal", tongTienQuyDoi.replaceAll(",", "")));
			}

//						elementSubTmp.appendChild(commons.createElementWithValue(doc, "TgTQDoi", tongTienQuyDoi.replaceAll(",", "")));
			elementTmp.appendChild(elementSubTmp);

			elementSubContent.appendChild(elementTmp);

			elementContent.appendChild(elementSubContent);
			}
			else {
				elementSubContent.appendChild(elementTmp);
				elementTmp = doc.createElement("TToan"); // Thong tin thanh toan
				elementTmp.appendChild(
						commons.createElementWithValue(doc, "TgTCThue", tongTienTruocThue.replaceAll(",", "")));
				elementTmp
						.appendChild(commons.createElementWithValue(doc, "TgTThue", tongTienThueGtgt.replaceAll(",", "")));
				elementTmp.appendChild(commons.createElementWithValue(doc, "TTCKTMai", "0"));
				elementTmp
						.appendChild(commons.createElementWithValue(doc, "TgTTTBSo", tongTienDaCoThue.replaceAll(",", "")));
				elementTmp.appendChild(commons.createElementWithValue(doc, "TgTTTBChu", tienBangChu));
				elementSubContent.appendChild(elementTmp);
				elementContent.appendChild(elementSubContent);
			}
			
			
			

			

			// END - NDHDon: Nội dung hóa đơn

			isSdaveFile = commons.docW3cToFile(doc, pathDir, fileNameXML);
			if (!isSdaveFile) {
				throw new Exception("Lưu dữ liệu không thành công.");
			}
			/* END - TAO XML HOA DON */

//						MTDiep = SystemParams.MSTTCGP + commons.csRandomAlphaNumbericString(46 - SystemParams.MSTTCGP.length()).toUpperCase();
			String uuid = UUID.randomUUID().toString().replaceAll("-", "").toUpperCase();
			MTDiep = SystemParams.MSTTCGP
					+ commons.convertLocalDateTimeToString(LocalDateTime.now(), "yyyyMMddHHmmssSSS")
					+ uuid.substring(0, 19);

			/* LUU DU LIEU HD */
			docTTHDLQuan = null;
			if (docEInvoiceTTDC != null) {
				docTTHDLQuan = new Document("_id", _id_tt_dc)
						.append("TCHDon",
								"3".equals(docEInvoiceTTDC.getEmbedded(Arrays.asList("HDSS", "TCTBao"), "")) ? "1"
										: "2")
						.append("LHDCLQuan", "1")
						.append("KHMSHDCLQuan", docEInvoiceTTDC.getEmbedded(Arrays.asList("TT_DC", "KHMSHDon"), ""))
						.append("KHHDCLQuan", docEInvoiceTTDC.getEmbedded(Arrays.asList("TT_DC", "KHHDon"), ""))
						.append("SHDCLQuan",
								String.valueOf(docEInvoiceTTDC.getEmbedded(Arrays.asList("TT_DC", "SHDon"), 0)))
						.append("NLHDCLQuan", commons.convertLocalDateTimeToString(
								commons.convertDateToLocalDateTime(
										docEInvoiceTTDC.getEmbedded(Arrays.asList("TT_DC", "NLap"), Date.class)),
								"yyyy-MM-dd"))
						.append("GChu", "");
			}

			docUpsert = new Document("_id", objectIdEInvoice).append("IssuerId", header.getIssuerId())
					.append("MTDiep", MTDiep)
//							.append("EInvoiceNumber", null)				//PHAT SINH KHI THUC HIEN KY
					.append("EInvoiceDetail", new Document("TTChung", new Document("THDon", loaiHoaDonText)
//							.append("MaHD", maHoadon)
							.append("LoaiHD", loaiHoaDon).append("MauSoHD", mauSoHdon)
							.append("KHMSHDon", docTmp.getEmbedded(Arrays.asList("DMMauSoKyHieu", "KHMSHDon"), ""))
							.append("KHHDon", docTmp.getEmbedded(Arrays.asList("DMMauSoKyHieu", "KHHDon"), ""))
							.append("NLap",
									commons.convertStringToLocalDate(ngayLap, Constants.FORMAT_DATE.FORMAT_DATE_WEB))
							.append("DVTTe", loaiTienTt).append("TGia", tyGia).append("HTTToanCode", hinhThucThanhToan)
							.append("HTTToan", hinhThucThanhToanText).append("TTHDLQuan", docTTHDLQuan))
							.append("NDHDon", new Document("NBan", new Document("Ten", docTmp.get("Name", ""))
									.append("MST", docTmp.get("TaxCode", "")).append("DChi", docTmp.get("Address", ""))
									.append("SDThoai", docTmp.get("Phone", ""))
									.append("DCTDTu", docTmp.get("Email", ""))
									.append("STKNHang",
											docTmp.getEmbedded(Arrays.asList("BankAccount", "AccountNumber"), ""))
									.append("TNHang", docTmp.getEmbedded(Arrays.asList("BankAccount", "BankName"), ""))
									.append("Fax", docTmp.get("Fax", "")).append("Website", docTmp.get("Website", "")))
									.append("NMua",
											new Document("Ten", khTenDonVi).append("MST", khMst)
													.append("DChi", khDiaChi).append("SDThoai", khSoDT)
													.append("CCCDan", khCCCDan).append("DCTDTu", khEmail)
													.append("HVTNMHang", khHoTenNguoiMua).append("STKNHang", khSoTk)
													.append("TNHang", khTkTaiNganHang)))
							.append("DSHHDVu", listDSHHDVu)

							.append("TToan",
									new Document("TgTCThue", commons.ToNumber(tongTienTruocThue))
											.append("TgTThue", commons.ToNumber(tongTienThueGtgt))
											.append("TgTTTBSo", commons.ToNumber(tongTienDaCoThue))
											.append("TgTQDoi", commons.ToNumber(tongTienQuyDoi))
											.append("TgTTTBChu", tienBangChu)))
					.append("SignStatusCode", Constants.INVOICE_SIGN_STATUS.NOSIGN)
					.append("EInvoiceStatus", Constants.INVOICE_STATUS.CREATED).append("IsDelete", false)
					.append("SecureKey", secureKey).append("Dir", pathDir).append("FileNameXML", fileNameXML)
					.append("PublishStatus", false).append("InfoCreated",
							new Document("CreateDate", LocalDateTime.now()).append("CreateUserID", header.getUserId())
									.append("CreateUserName", header.getUserName())
									.append("CreateUserFullName", header.getUserFullName()));
			/* END - LUU DU LIEU HD */

			mongoClient = cfg.mongoClient();
			collection = mongoClient.getDatabase(cfg.dbName).getCollection("EInvoiceMTT");
			collection.insertOne(docUpsert);
			mongoClient.close();
			// Start replace, adjusted

			docFind1 = new Document("IssuerId", header.getIssuerId()).append("IsDelete", new Document("$ne", true))
					.append("_id", objectIdTT_DC).append("SignStatusCode", Constants.INVOICE_SIGN_STATUS.SIGNED)
					.append("EInvoiceStatus", new Document("$in", Arrays.asList(Constants.INVOICE_STATUS.COMPLETE,
							Constants.INVOICE_STATUS.ADJUSTED, Constants.INVOICE_STATUS.REPLACED)));
			options = new FindOneAndUpdateOptions();
			options.upsert(true);
			options.maxTime(5000, TimeUnit.MILLISECONDS);
			options.returnDocument(ReturnDocument.AFTER);

			if (docEInvoiceTTDC != null) {
				if ("3".equals(docEInvoiceTTDC.getEmbedded(Arrays.asList("HDSS", "TCTBao"), ""))) {

					mongoClient = cfg.mongoClient();
					collection = mongoClient.getDatabase(cfg.dbName).getCollection("EInvoiceMTT");
					collection.findOneAndUpdate(docFind1,
							new Document("$set", new Document("EInvoiceStatus", "REPLACED")), options);
					mongoClient.close();

				} else if ("2".equals(docEInvoiceTTDC.getEmbedded(Arrays.asList("HDSS", "TCTBao"), ""))) {

					mongoClient = cfg.mongoClient();
					collection = mongoClient.getDatabase(cfg.dbName).getCollection("EInvoiceMTT");
					collection.findOneAndUpdate(docFind1,
							new Document("$set", new Document("EInvoiceStatus", "ADJUSTED")), options);
					mongoClient.close();

				}

			}

			// End replace, adjusted
			String name_company = removeAccent(header.getUserFullName());
			DateTimeFormatter format_time = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
			LocalDateTime time_dem = LocalDateTime.now();
			String time = time_dem.format(format_time);
			System.out.println(time + name_company + " Vua tao hoa don tu may tinh tien.");

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

			pipeline.add(new Document("$lookup", new Document("from", "Users").append("pipeline", Arrays.asList(
					new Document("$match",
							new Document("IssuerId", header.getIssuerId()).append("_id", objectIdUser)
									.append("IsActive", true).append("IsDelete", new Document("$ne", true))),
					new Document("$project", new Document("_id", 1).append("UserName", 1).append("FullName", 1)),
					new Document("$limit", 1))).append("as", "UserInfo")));
			pipeline.add(new Document("$unwind",
					new Document("path", "$UserInfo").append("preserveNullAndEmptyArrays", true)));

			docFind = new Document("IssuerId", header.getIssuerId()).append("IsDelete", new Document("$ne", true))

					.append("SignStatusCode", Constants.INVOICE_SIGN_STATUS.NOSIGN).append("_id", objectIdEInvoice);
			pipeline.add(new Document("$lookup",
					new Document("from", "EInvoiceMTT").append("pipeline", Arrays.asList(new Document("$match", docFind)

					)).append("as", "EInvoiceMTT")));
			pipeline.add(new Document("$unwind",
					new Document("path", "$EInvoiceMTT").append("preserveNullAndEmptyArrays", true)));

			pipeline.add(new Document("$lookup",
					new Document("from", "PramLink")
							.append("pipeline",
									Arrays.asList(
											new Document("$match",
													new Document("$expr", new Document("IsDelete", false))),
											new Document("$project", new Document("_id", 1).append("LinkPortal", 1))))
							.append("as", "PramLink")));
			pipeline.add(new Document("$unwind",
					new Document("path", "$PramLink").append("preserveNullAndEmptyArrays", true)));

			mongoClient = cfg.mongoClient();
			collection = mongoClient.getDatabase(cfg.dbName).getCollection("Issuer");
			try {
				docTmp = collection.aggregate(pipeline).allowDiskUse(true).iterator().next();
			} catch (Exception ex) {

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
			if (docTmp.get("EInvoiceMTT") == null) {
				responseStatus = new MspResponseStatus(9999, "Không tìm thấy thông tin hóa đơn cần cập nhật.");
				rsp.setResponseStatus(responseStatus);
				return rsp;
			}

			link = docTmp.getEmbedded(Arrays.asList("PramLink", "LinkPortal"), "");

			docTTHDLQuan = docTmp.getEmbedded(Arrays.asList("EInvoiceMTT", "EInvoiceDetail", "TTChung", "TTHDLQuan"),
					Document.class);

			taxCode = docTmp.getString("TaxCode");
			pathDir = docTmp.getEmbedded(Arrays.asList("EInvoiceMTT", "Dir"), "");
			fileNameXML = docTmp.getEmbedded(Arrays.asList("EInvoiceMTT", "FileNameXML"), "");

			file = new File(pathDir);
			if (!file.exists())
				file.mkdirs();

			String KHMSHDon = docTmp.getEmbedded(Arrays.asList("EInvoiceMTT", "EInvoiceDetail", "TTChung", "KHMSHDon"),
					"");
			String KHHDon = docTmp.getEmbedded(Arrays.asList("EInvoiceMTT", "EInvoiceDetail", "TTChung", "KHHDon"), "");

//			int shd = docTmp.getEmbedded(Arrays.asList("EInvoiceMTT", "EInvoiceDetail", "TTChung", "SHDon"), 0);

			secureKey = docTmp.getEmbedded(Arrays.asList("EInvoiceMTT", "SecureKey"), "");

			/* TAO XML HOA DON */
			dbf = DocumentBuilderFactory.newInstance();
			db = dbf.newDocumentBuilder();
			doc = db.newDocument();
			doc.setXmlStandalone(true);

			root = doc.createElement("HDon");
			root.setAttribute("Id", "DLieu0");
			doc.appendChild(root);

			elementContent = doc.createElement("DLHDon");
			elementContent.setAttribute("Id", "data");
			root.appendChild(elementContent);

			elementSubTmp = null;
			elementSubTmp01 = null;
			elementSubContent = doc.createElement("TTChung");
			elementTmp = null;

			elementSubContent.appendChild(commons.createElementWithValue(doc, "PBan", SystemParams.VERSION_XML));
			elementSubContent.appendChild(commons.createElementWithValue(doc, "THDon", loaiHoaDonText));
			elementSubContent.appendChild(commons.createElementWithValue(doc, "KHMSHDon", KHMSHDon));
			elementSubContent.appendChild(commons.createElementWithValue(doc, "KHHDon", KHHDon));
			elementSubContent.appendChild(commons.createElementWithValue(doc, "SHDon", "")); // SE PHAT SINH KHI KY
			// Ngày lập
			elementSubContent.appendChild(
					commons.createElementWithValue(doc, "NLap", commons.convertLocalDateTimeStringToString(ngayLap,
							Constants.FORMAT_DATE.FORMAT_DATE_WEB, Constants.FORMAT_DATE.FORMAT_DATE_EINVOICE, false)));

			// Đơn vị tiền tệ
			elementSubContent.appendChild(commons.createElementWithValue(doc, "DVTTe", loaiTienTt));
			// Tỷ giá
			elementSubContent.appendChild(commons.createElementWithValue(doc, "TGia", tyGia));
			// Hình thức thanh toán
			elementSubContent.appendChild(commons.createElementWithValue(doc, "HTTToan", hinhThucThanhToanText));
			// MST tổ chức cung cấp giải pháp HĐĐT
			elementSubContent.appendChild(commons.createElementWithValue(doc, "MSTTCGP", SystemParams.MSTTCGP));

			elementTmp = doc.createElement("TTKhac"); // THONG TIN KHAC
			elementTmp.appendChild(commons.createElementTTKhac(doc, "HoaDon_Loai", "string", loaiHoaDon));
			elementTmp.appendChild(commons.createElementTTKhac(doc, "PortalLink", "string", link));
			elementTmp.appendChild(commons.createElementTTKhac(doc, "SecureKey", "string", secureKey));
			elementTmp
					.appendChild(commons.createElementTTKhac(doc, "SystemKey", "string", objectIdEInvoice.toString()));
			elementSubContent.appendChild(elementTmp);

			if (docTTHDLQuan != null) {
				elementTmp = doc.createElement("TTHDLQuan");
				elementTmp.appendChild(commons.createElementWithValue(doc, "TCHDon", docTTHDLQuan.get("TCHDon", "")));
				elementTmp.appendChild(
						commons.createElementWithValue(doc, "LHDCLQuan", docTTHDLQuan.get("LHDCLQuan", "")));
				elementTmp.appendChild(
						commons.createElementWithValue(doc, "KHMSHDCLQuan", docTTHDLQuan.get("KHMSHDCLQuan", "")));
				elementTmp.appendChild(
						commons.createElementWithValue(doc, "KHHDCLQuan", docTTHDLQuan.get("KHHDCLQuan", "")));
				elementTmp.appendChild(
						commons.createElementWithValue(doc, "SHDCLQuan", docTTHDLQuan.get("SHDCLQuan", "")));
				elementTmp.appendChild(
						commons.createElementWithValue(doc, "NLHDCLQuan", docTTHDLQuan.get("NLHDCLQuan", "")));
				elementTmp.appendChild(commons.createElementWithValue(doc, "GChu", ""));
				elementSubContent.appendChild(elementTmp);
			}

			elementContent.appendChild(elementSubContent);

			// NDHDon: Nội dung hóa đơn
			elementSubContent = doc.createElement("NDHDon");
			elementTmp = doc.createElement("NBan"); // NGUOI BAN
			elementTmp.appendChild(commons.createElementWithValue(doc, "Ten", docTmp.get("Name", "")));
			elementTmp.appendChild(commons.createElementWithValue(doc, "MST", docTmp.get("TaxCode", "")));
			elementTmp.appendChild(commons.createElementWithValue(doc, "DChi", docTmp.get("Address", "")));
			elementTmp.appendChild(commons.createElementWithValue(doc, "SDThoai", docTmp.get("Phone", "")));
			elementTmp.appendChild(commons.createElementWithValue(doc, "DCTDTu", docTmp.get("Email", "")));
			elementTmp.appendChild(commons.createElementWithValue(doc, "STKNHang",
					docTmp.getEmbedded(Arrays.asList("BankAccount", "AccountNumber"), "")));
			elementTmp.appendChild(commons.createElementWithValue(doc, "TNHang",
					docTmp.getEmbedded(Arrays.asList("BankAccount", "BankName"), "")));
			elementTmp.appendChild(commons.createElementWithValue(doc, "Fax", docTmp.get("Fax", "")));
			elementTmp.appendChild(commons.createElementWithValue(doc, "Website", docTmp.get("Website", "")));

			elementSubTmp = doc.createElement("TTKhac");
			if (!docTmp.get("NameEN", "").equals("")) {
				elementSubTmp
						.appendChild(commons.createElementTTKhac(doc, "TenEN", "string", docTmp.get("NameEN", "")));
			}
			elementTmp.appendChild(elementSubTmp);

			/* ADD THONG TIN TK NGAN HANG (NEU CO) */
			if (docTmp.get("BankAccountExt") != null && docTmp.getList("BankAccountExt", Document.class).size() > 0) {
				intTmp = 1;
//							elementTmp = doc.createElement("TTKhac");
//							elementSubTmp = doc.createElement("TTKhac");
				for (Document oo : docTmp.getList("BankAccountExt", Document.class)) {
					elementSubTmp.appendChild(commons.createElementTTKhac(doc, "STKNHang" + intTmp, "string",
							oo.get("AccountNumber", "")));
					elementSubTmp.appendChild(
							commons.createElementTTKhac(doc, "TNHang" + intTmp, "string", oo.get("BankName", "")));
					elementTmp.appendChild(elementSubTmp);
					intTmp++;
				}
			}
			elementSubContent.appendChild(elementTmp);

			elementTmp = doc.createElement("NMua"); // NGUOI MUA
			elementTmp.appendChild(commons.createElementWithValue(doc, "Ten", khTenDonVi));
			elementTmp.appendChild(commons.createElementWithValue(doc, "MST", khMst));
			elementTmp.appendChild(commons.createElementWithValue(doc, "DChi", khDiaChi));
			elementTmp.appendChild(commons.createElementWithValue(doc, "SDThoai", khSoDT));
			elementTmp.appendChild(commons.createElementWithValue(doc, "CCCDan", khCCCDan));
			elementTmp.appendChild(commons.createElementWithValue(doc, "DCTDTu", khEmail));
			elementTmp.appendChild(commons.createElementWithValue(doc, "HVTNMHang", khHoTenNguoiMua));
			elementTmp.appendChild(commons.createElementWithValue(doc, "STKNHang", khSoTk));
			elementTmp.appendChild(commons.createElementWithValue(doc, "TNHang", khTkTaiNganHang));
			elementTmp.appendChild(commons.createElementWithValue(doc, "TTKhac", ""));

			elementSubContent.appendChild(elementTmp);

			mapVATAmount = new LinkedHashMap<String, Double>();
			mapAmount = new LinkedHashMap<String, Double>();
			elementTmp = doc.createElement("DSHHDVu"); // HH-DV
			if (!jsonData.at("/DSSanPham").isMissingNode()) {
				for (JsonNode o : jsonData.at("/DSSanPham")) {
					if (!"".equals(commons.getTextJsonNode(o.at("/ProductName")))) {
						tmp = commons.getTextJsonNode(o.at("/VATRate")).replaceAll(",", "");
						switch (tmp) {
						case "0":
							tmp = "-1";
							break;
						case "1":
							tmp = "0%";
							break;
						case "5":
						case "8":
						case "10":
							tmp += "%";
							break;
						case "-1":
							tmp = "KCT";
							break;
						case "-2":
							tmp = "KKKNT";
							break;
						default:
							break;
						}
//									if(!("2".equals(commons.getTextJsonNode(o.at("/Feature"))))) {	// || "4".equals(commons.getTextJsonNode(o.at("/Feature")))
						if ("1".equals(commons.getTextJsonNode(o.at("/Feature")))
								|| "3".equals(commons.getTextJsonNode(o.at("/Feature")))
								|| "4".equals(commons.getTextJsonNode(o.at("/Feature")))) {
							mapAmount.compute(tmp, (k, v) -> {
								return (v == null
										? commons.ToNumber(commons.getTextJsonNode(o.at("/Total")))
												* ("3".equals(commons.getTextJsonNode(o.at("/Feature"))) ? -1 : 1)
										: v + commons.ToNumber(commons.getTextJsonNode(o.at("/Total")))
												* ("3".equals(commons.getTextJsonNode(o.at("/Feature"))) ? -1 : 1));
							});
							mapVATAmount.compute(tmp, (k, v) -> {
								return (v == null
										? commons.ToNumber(commons.getTextJsonNode(o.at("/VATAmount")))
												* ("3".equals(commons.getTextJsonNode(o.at("/Feature"))) ? -1 : 1)
										: v + commons.ToNumber(commons.getTextJsonNode(o.at("/VATAmount")))
												* ("3".equals(commons.getTextJsonNode(o.at("/Feature"))) ? -1 : 1));
							});
						}

						elementSubTmp = doc.createElement("HHDVu");
						elementSubTmp.appendChild(commons.createElementWithValue(doc, "TChat",
								commons.getTextJsonNode(o.at("/Feature"))));
						elementSubTmp.appendChild(
								commons.createElementWithValue(doc, "STT", commons.getTextJsonNode(o.at("/STT"))));
						elementSubTmp.appendChild(commons.createElementWithValue(doc, "MHHDVu",
								commons.getTextJsonNode(o.at("/ProductCode"))));
						elementSubTmp.appendChild(commons.createElementWithValue(doc, "THHDVu",
								commons.getTextJsonNode(o.at("/ProductName"))));

						elementSubTmp.appendChild(
								commons.createElementWithValue(doc, "DVTinh", commons.getTextJsonNode(o.at("/Unit"))));

						if (!("2".equals(commons.getTextJsonNode(o.at("/Feature"))))) { // ||
																						// "4".equals(commons.getTextJsonNode(o.at("/Feature")))
							elementSubTmp.appendChild(commons.createElementWithValue(doc, "SLuong",
									commons.getTextJsonNode(o.at("/Quantity")).replaceAll(",", "")));
							elementSubTmp.appendChild(commons.createElementWithValue(doc, "DGia",
									commons.getTextJsonNode(o.at("/Price")).replaceAll(",", "")));
							elementSubTmp.appendChild(commons.createElementWithValue(doc, "TLCKhau", ""));
							elementSubTmp.appendChild(commons.createElementWithValue(doc, "STCKhau", ""));
							elementSubTmp.appendChild(commons.createElementWithValue(doc, "ThTien",
									commons.getTextJsonNode(o.at("/Total")).replaceAll(",", "")));
							if (!tmp.equals("-1") && !loaiHoaDon.equals("2")) {
								elementSubTmp.appendChild(commons.createElementWithValue(doc, "TSuat", tmp));
							}
						}

						
						
						if (!loaiHoaDon.equals("2")) {
							elementSubTmp01 = doc.createElement("TTKhac");

							if (commons.getTextJsonNode(o.at("/VATAmount")).replaceAll(",", "").equals("")) {
								elementSubTmp01
										.appendChild(commons.createElementTTKhac(doc, "VATAmount", "decimal", "0"));
							} else {
								elementSubTmp01.appendChild(commons.createElementTTKhac(doc, "VATAmount", "decimal",
										commons.getTextJsonNode(o.at("/VATAmount")).replaceAll(",", "")));
							}

							elementSubTmp01.appendChild(commons.createElementTTKhac(doc, "Amount", "decimal",
									commons.getTextJsonNode(o.at("/Amount")).replaceAll(",", "")));
							elementSubTmp.appendChild(elementSubTmp01);

							elementTmp.appendChild(elementSubTmp);
							double a = commons.ToNumber(commons.getTextJsonNode(o.at("/VATRate")));

							hItem = new LinkedHashMap<String, Object>();
							hItem.put("STT", commons.getTextJsonNode(o.at("/STT")));
							hItem.put("ProductName", commons.getTextJsonNode(o.at("/ProductName")));
							hItem.put("ProductCode", commons.getTextJsonNode(o.at("/ProductCode")));
							hItem.put("Unit", commons.getTextJsonNode(o.at("/Unit")));
							hItem.put("Quantity", commons.ToNumber(commons.getTextJsonNode(o.at("/Quantity"))));
							hItem.put("Price", commons.ToNumber(commons.getTextJsonNode(o.at("/Price"))));
							hItem.put("Total", commons.ToNumber(commons.getTextJsonNode(o.at("/Total"))));
							double vatRate = 0.0;
							if (a == 0.0) {

							} else if (a == 1.0) {
								hItem.put("VATRate", vatRate);
							} else {
								hItem.put("VATRate", commons.ToNumber(commons.getTextJsonNode(o.at("/VATRate"))));
							}

							hItem.put("VATAmount", commons.ToNumber(commons.getTextJsonNode(o.at("/VATAmount"))));
							hItem.put("Amount", commons.ToNumber(commons.getTextJsonNode(o.at("/Amount"))));
							hItem.put("Feature", commons.getTextJsonNode(o.at("/Feature")));
							listDSHHDVu.add(hItem);
						} else {
							elementSubTmp01 = doc.createElement("TTKhac");
//							elementSubTmp01.appendChild(commons.createElementTTKhac(doc, "VATAmount", "decimal",
//									commons.getTextJsonNode(o.at("/VATAmount")).replaceAll(",", "")));
//							
							if (commons.getTextJsonNode(o.at("/VATAmount")).replaceAll(",", "").equals("")) {
								elementSubTmp01
										.appendChild(commons.createElementTTKhac(doc, "VATAmount", "decimal", "0"));
							} else {
								elementSubTmp01.appendChild(commons.createElementTTKhac(doc, "VATAmount", "decimal",
										commons.getTextJsonNode(o.at("/VATAmount")).replaceAll(",", "")));
							}
							
							elementSubTmp01.appendChild(commons.createElementTTKhac(doc, "Amount", "decimal",
									commons.getTextJsonNode(o.at("/Amount")).replaceAll(",", "")));
							elementSubTmp01.appendChild(commons.createElementTTKhac(doc, "Stt", "numeric",
									commons.getTextJsonNode(o.at("/STT"))));
							elementSubTmp.appendChild(elementSubTmp01);

							elementTmp.appendChild(elementSubTmp);
							hItem = new LinkedHashMap<String, Object>();
							hItem.put("STT", commons.getTextJsonNode(o.at("/STT")));
							hItem.put("ProductName", commons.getTextJsonNode(o.at("/ProductName")));
							hItem.put("Unit", commons.getTextJsonNode(o.at("/Unit")));
							hItem.put("Quantity", commons.ToNumber(commons.getTextJsonNode(o.at("/Quantity"))));
							hItem.put("Price", commons.ToNumber(commons.getTextJsonNode(o.at("/Price"))));
							hItem.put("Total", commons.ToNumber(commons.getTextJsonNode(o.at("/Total"))));
							hItem.put("VATRate", null);
							hItem.put("VATAmount", null);
							hItem.put("Amount", commons.ToNumber(commons.getTextJsonNode(o.at("/Amount"))));
							hItem.put("Feature", commons.getTextJsonNode(o.at("/Feature")));
							listDSHHDVu.add(hItem);
						}

						
						
						
						
						

					}

				}
			}
			
			
			
			if (!loaiHoaDon.equals("2")) {
			elementSubContent.appendChild(elementTmp);
			elementTmp = doc.createElement("TToan"); // Thong tin thanh toan
			elementSubTmp = doc.createElement("THTTLTSuat");
			/* DANH SACH CAC LOAI THUE SUAT */
			for (Map.Entry<String, Double> pair : mapVATAmount.entrySet()) {
				if (null != pair.getKey() && !"".equals(pair.getKey())) {
					if (!pair.getKey().equals("-1")) {
						elementSubTmp01 = doc.createElement("LTSuat");
						elementSubTmp01.appendChild(commons.createElementWithValue(doc, "TSuat", pair.getKey()));
						elementSubTmp01.appendChild(commons.createElementWithValue(doc, "ThTien",
								commons.formatNumberReal(mapAmount.get(pair.getKey())).replaceAll(",", "")));

						if (loaiTienTt.equals("VND")) {
							elementSubTmp01.appendChild(commons.createElementWithValue(doc, "TThue",
									String.format("%.0f", mapVATAmount.get(pair.getKey()))));
						} else {
//						elementSubTmp01.appendChild(commons.createElementWithValue(doc, "TThue",String.valueOf(mapVATAmount.get(pair.getKey()))));	
							elementSubTmp01.appendChild(commons.createElementWithValue(doc, "TThue",
									String.format("%.2f", mapVATAmount.get(pair.getKey()))));
						}

						elementSubTmp.appendChild(elementSubTmp01);
					}
				}
			}
			elementTmp.appendChild(elementSubTmp);
			elementTmp.appendChild(
					commons.createElementWithValue(doc, "TgTCThue", tongTienTruocThue.replaceAll(",", "")));
			elementTmp
					.appendChild(commons.createElementWithValue(doc, "TgTThue", tongTienThueGtgt.replaceAll(",", "")));

			elementSubTmp = doc.createElement("DSLPhi");
			elementSubTmp01 = doc.createElement("LPhi");
			elementSubTmp01.appendChild(commons.createElementWithValue(doc, "TLPhi", ""));
			elementSubTmp01.appendChild(commons.createElementWithValue(doc, "TPhi", "0"));
			elementSubTmp.appendChild(elementSubTmp01);

			elementTmp.appendChild(elementSubTmp);

			elementTmp.appendChild(commons.createElementWithValue(doc, "TTCKTMai", "0"));
			elementTmp
					.appendChild(commons.createElementWithValue(doc, "TgTTTBSo", tongTienDaCoThue.replaceAll(",", "")));
			elementTmp.appendChild(commons.createElementWithValue(doc, "TgTTTBChu", tienBangChu));
			elementSubTmp = doc.createElement("TTKhac");
			if (!"VND".equals(loaiTienTt)) {
				elementSubTmp.appendChild(
						commons.createElementTTKhac(doc, "TgTQDoi", "decimal", tongTienQuyDoi.replaceAll(",", "")));
			}

//						elementSubTmp.appendChild(commons.createElementWithValue(doc, "TgTQDoi", tongTienQuyDoi.replaceAll(",", "")));
			elementTmp.appendChild(elementSubTmp);

			elementSubContent.appendChild(elementTmp);

			elementContent.appendChild(elementSubContent);
			}
			else {
				elementSubContent.appendChild(elementTmp);
				elementTmp = doc.createElement("TToan"); // Thong tin thanh toan
				elementTmp.appendChild(
						commons.createElementWithValue(doc, "TgTCThue", tongTienTruocThue.replaceAll(",", "")));
				elementTmp
						.appendChild(commons.createElementWithValue(doc, "TgTThue", tongTienThueGtgt.replaceAll(",", "")));
				elementTmp.appendChild(commons.createElementWithValue(doc, "TTCKTMai", "0"));
				elementTmp
						.appendChild(commons.createElementWithValue(doc, "TgTTTBSo", tongTienDaCoThue.replaceAll(",", "")));
				elementTmp.appendChild(commons.createElementWithValue(doc, "TgTTTBChu", tienBangChu));
				elementSubContent.appendChild(elementTmp);
				elementContent.appendChild(elementSubContent);
			}
			
			

			isSdaveFile = commons.docW3cToFile(doc, pathDir, fileNameXML);
			if (!isSdaveFile) {
				throw new Exception("Lưu dữ liệu không thành công.");
			}
			/* END - TAO XML HOA DON */

			/* LUU DU LIEU HD */
			docUpsert = new Document("TTChung", new Document("THDon", loaiHoaDonText).append("LoaiHD", loaiHoaDon)
					.append("MauSoHD", mauSoHdon).append("KHMSHDon", KHMSHDon).append("KHHDon", KHHDon)
					.append("NLap", commons.convertStringToLocalDate(ngayLap, Constants.FORMAT_DATE.FORMAT_DATE_WEB))
					.append("DVTTe", loaiTienTt).append("TGia", tyGia).append("HTTToanCode", hinhThucThanhToan)
					.append("HTTToan", hinhThucThanhToanText).append("TTHDLQuan", docTTHDLQuan))
					.append("NDHDon", new Document("NBan", new Document("Ten", docTmp.get("Name", ""))
							.append("MST", docTmp.get("TaxCode", "")).append("DChi", docTmp.get("Address", ""))
							.append("SDThoai", docTmp.get("Phone", "")).append("DCTDTu", docTmp.get("Email", ""))
							.append("STKNHang", docTmp.getEmbedded(Arrays.asList("BankAccount", "AccountNumber"), ""))
							.append("TNHang", docTmp.getEmbedded(Arrays.asList("BankAccount", "BankName"), ""))
							.append("Fax", docTmp.get("Fax", "")).append("Website", docTmp.get("Website", "")))
							.append("NMua",
									new Document("Ten", khTenDonVi).append("MST", khMst).append("DChi", khDiaChi)
//									.append("MKHang", khMKHang)
											.append("CCCDan", khCCCDan).append("SDThoai", khSoDT)
											.append("DCTDTu", khEmail).append("HVTNMHang", khHoTenNguoiMua)
											.append("STKNHang", khSoTk).append("TNHang", khTkTaiNganHang)))
					.append("DSHHDVu", listDSHHDVu)
					.append("TToan", new Document("TgTCThue", commons.ToNumber(tongTienTruocThue))
							.append("TgTThue", commons.ToNumber(tongTienThueGtgt))
							.append("TgTTTBSo", commons.ToNumber(tongTienDaCoThue))
							.append("TgTQDoi", commons.ToNumber(tongTienQuyDoi)).append("TgTTTBChu", tienBangChu));
			/* END - LUU DU LIEU HD */

			options = new FindOneAndUpdateOptions();
			options.upsert(false);
			options.maxTime(5000, TimeUnit.MILLISECONDS);
			options.returnDocument(ReturnDocument.AFTER);

			mongoClient = cfg.mongoClient();
			collection = mongoClient.getDatabase(cfg.dbName).getCollection("EInvoiceMTT");
			collection.findOneAndUpdate(docFind, new Document("$set",
					new Document("EInvoiceDetail", docUpsert).append("PublishStatus", false).append("InfoUpdated",
							new Document("UpdatedDate", LocalDateTime.now()).append("UpdatedUserID", header.getUserId())
									.append("UpdatedUserName", header.getUserName())
									.append("UpdatedUserFullName", header.getUserFullName()))),
					options);
			mongoClient.close();

			name_company = removeAccent(header.getUserFullName());
			format_time = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
			time_dem = LocalDateTime.now();
			time = time_dem.format(format_time);
			System.out.println(time + name_company + " Vua thay doi hoa don tu may tinh tien.");

			responseStatus = new MspResponseStatus(0, "SUCCESS");
			rsp.setResponseStatus(responseStatus);
			return rsp;

		//////////////////////////////////////////////////////////////////////////////////////////////////////////////
		case Constants.MSG_ACTION_CODE.COPY:

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
			try {
				objectIdMSKH = new ObjectId(mauSoHdon);
			} catch (Exception e) {
			}
			pipeline = new ArrayList<Document>();
			pipeline.add(new Document("$match", new Document("_id", objectId).append("IsActive", true)
					.append("IsDelete", new Document("$ne", true))));

			pipeline.add(new Document("$lookup", new Document("from", "Users").append("pipeline", Arrays.asList(
					new Document("$match",
							new Document("IssuerId", header.getIssuerId()).append("_id", objectIdUser)
									.append("IsActive", true).append("IsDelete", new Document("$ne", true))),
					new Document("$project", new Document("_id", 1).append("UserName", 1).append("FullName", 1)),
					new Document("$limit", 1))).append("as", "UserInfo")));
			pipeline.add(new Document("$unwind",
					new Document("path", "$UserInfo").append("preserveNullAndEmptyArrays", true)));

			pipeline.add(new Document("$lookup", new Document("from", "DMMauSoKyHieu")
					.append("pipeline", Arrays.asList(new Document("$match",
							new Document("IssuerId", header.getIssuerId()).append("IsActive", true)
									.append("IsDelete", new Document("$ne", true)).append("_id", objectIdMSKH))))
					.append("as", "DMMauSoKyHieu")));
			pipeline.add(new Document("$unwind",
					new Document("path", "$DMMauSoKyHieu").append("preserveNullAndEmptyArrays", true)));

			docFind = new Document("IssuerId", header.getIssuerId()).append("IsDelete", new Document("$ne", true))
					.append("_id", objectIdEInvoice);
			pipeline.add(new Document("$lookup",
					new Document("from", "EInvoiceMTT").append("pipeline", Arrays.asList(new Document("$match", docFind)

					)).append("as", "EInvoiceMTT")));
			pipeline.add(new Document("$unwind",
					new Document("path", "$EInvoiceMTT").append("preserveNullAndEmptyArrays", true)));

			pipeline.add(new Document("$lookup",
					new Document("from", "PramLink")
							.append("pipeline",
									Arrays.asList(
											new Document("$match",
													new Document("$expr", new Document("IsDelete", false))),
											new Document("$project", new Document("_id", 1).append("LinkPortal", 1))))
							.append("as", "PramLink")));
			pipeline.add(new Document("$unwind",
					new Document("path", "$PramLink").append("preserveNullAndEmptyArrays", true)));

			mongoClient = cfg.mongoClient();
			collection = mongoClient.getDatabase(cfg.dbName).getCollection("Issuer");
			try {
				docTmp = collection.aggregate(pipeline).allowDiskUse(true).iterator().next();
			} catch (Exception ex) {

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
			if (docTmp.get("EInvoiceMTT") == null) {
				responseStatus = new MspResponseStatus(9999, "Không tìm thấy thông tin hóa đơn cần cập nhật.");
				rsp.setResponseStatus(responseStatus);
				return rsp;
			}
			if (docTmp.get("DMMauSoKyHieu") == null) {
				responseStatus = new MspResponseStatus(9999, "Không tìm thấy thông tin ký hiệu mẫu số.");
				rsp.setResponseStatus(responseStatus);
				return rsp;
			}

			docTTHDLQuan = docTmp.getEmbedded(Arrays.asList("EInvoiceMTT", "EInvoiceDetail", "TTChung", "TTHDLQuan"),
					Document.class);

			taxCode = docTmp.getString("TaxCode");

			String KHMSHDon1 = docTmp.getEmbedded(Arrays.asList("EInvoiceMTT", "EInvoiceDetail", "TTChung", "KHMSHDon"),
					"");

			link = docTmp.getEmbedded(Arrays.asList("PramLink", "LinkPortal"), "");

			String KHHDon1 = docTmp.getEmbedded(Arrays.asList("DMMauSoKyHieu", "KHHDon"), "");
//			String MaHD = docTmp.getEmbedded(Arrays.asList("EInvoiceMTT", "EInvoiceDetail", "TTChung", "MaHD"), "");
			secureKey = commons.csRandomNumbericString(6);
			objectIdEInvoice = new ObjectId();
			path = Paths.get(SystemParams.DIR_E_INVOICE_DATA, taxCode,
					docTmp.getEmbedded(Arrays.asList("DMMauSoKyHieu", "_id"), ObjectId.class).toString());
			pathDir = path.toString();
			file = path.toFile();
			if (!file.exists())
				file.mkdirs();
			/* TAO XML HOA DON */
			fileNameXML = objectIdEInvoice.toString() + ".xml";

			dbf = DocumentBuilderFactory.newInstance();
			db = dbf.newDocumentBuilder();
			doc = db.newDocument();
			doc.setXmlStandalone(true);

			root = doc.createElement("HDon");
			root.setAttribute("Id", "DLieu0");
			doc.appendChild(root);

			elementContent = doc.createElement("DLHDon");
			elementContent.setAttribute("Id", "data");
			root.appendChild(elementContent);

			elementSubTmp = null;
			elementSubTmp01 = null;
			elementSubContent = doc.createElement("TTChung");
			elementTmp = null;

			elementSubContent.appendChild(commons.createElementWithValue(doc, "PBan", SystemParams.VERSION_XML));
			elementSubContent.appendChild(commons.createElementWithValue(doc, "THDon", loaiHoaDonText));
			elementSubContent.appendChild(commons.createElementWithValue(doc, "KHMSHDon", KHMSHDon1));
			elementSubContent.appendChild(commons.createElementWithValue(doc, "KHHDon", KHHDon1));
			elementSubContent.appendChild(commons.createElementWithValue(doc, "SHDon", "")); // SE PHAT SINH KHI KY

			// Ngày lập
			elementSubContent.appendChild(
					commons.createElementWithValue(doc, "NLap", commons.convertLocalDateTimeStringToString(ngayLap,
							Constants.FORMAT_DATE.FORMAT_DATE_WEB, Constants.FORMAT_DATE.FORMAT_DATE_EINVOICE, false)));

			// Đơn vị tiền tệ
			elementSubContent.appendChild(commons.createElementWithValue(doc, "DVTTe", loaiTienTt));
			// Tỷ giá
			elementSubContent.appendChild(commons.createElementWithValue(doc, "TGia", tyGia));
			// Hình thức thanh toán
			elementSubContent.appendChild(commons.createElementWithValue(doc, "HTTToan", hinhThucThanhToanText));
			// MST tổ chức cung cấp giải pháp HĐĐT
			elementSubContent.appendChild(commons.createElementWithValue(doc, "MSTTCGP", SystemParams.MSTTCGP));

			elementTmp = doc.createElement("TTKhac"); // THONG TIN KHAC
			elementTmp.appendChild(commons.createElementTTKhac(doc, "HoaDon_Loai", "string", loaiHoaDon));
			elementTmp.appendChild(commons.createElementTTKhac(doc, "PortalLink", "string", link));
			elementTmp.appendChild(commons.createElementTTKhac(doc, "SecureKey", "string", secureKey));
			elementTmp
					.appendChild(commons.createElementTTKhac(doc, "SystemKey", "string", objectIdEInvoice.toString()));
			elementSubContent.appendChild(elementTmp);

			if (docTTHDLQuan != null) {
				elementTmp = doc.createElement("TTHDLQuan");
				elementTmp.appendChild(commons.createElementWithValue(doc, "TCHDon", docTTHDLQuan.get("TCHDon", "")));
				elementTmp.appendChild(
						commons.createElementWithValue(doc, "LHDCLQuan", docTTHDLQuan.get("LHDCLQuan", "")));
				elementTmp.appendChild(
						commons.createElementWithValue(doc, "KHMSHDCLQuan", docTTHDLQuan.get("KHMSHDCLQuan", "")));
				elementTmp.appendChild(
						commons.createElementWithValue(doc, "KHHDCLQuan", docTTHDLQuan.get("KHHDCLQuan", "")));
				elementTmp.appendChild(
						commons.createElementWithValue(doc, "SHDCLQuan", docTTHDLQuan.get("SHDCLQuan", "")));
				elementTmp.appendChild(
						commons.createElementWithValue(doc, "NLHDCLQuan", docTTHDLQuan.get("NLHDCLQuan", "")));
				elementTmp.appendChild(commons.createElementWithValue(doc, "GChu", ""));
				elementSubContent.appendChild(elementTmp);
			}

			elementContent.appendChild(elementSubContent);

			// NDHDon: Nội dung hóa đơn
			elementSubContent = doc.createElement("NDHDon");
			elementTmp = doc.createElement("NBan"); // NGUOI BAN
			elementTmp.appendChild(commons.createElementWithValue(doc, "Ten", docTmp.get("Name", "")));
			elementTmp.appendChild(commons.createElementWithValue(doc, "MST", docTmp.get("TaxCode", "")));
			elementTmp.appendChild(commons.createElementWithValue(doc, "DChi", docTmp.get("Address", "")));
			elementTmp.appendChild(commons.createElementWithValue(doc, "SDThoai", docTmp.get("Phone", "")));
			elementTmp.appendChild(commons.createElementWithValue(doc, "DCTDTu", docTmp.get("Email", "")));
			elementTmp.appendChild(commons.createElementWithValue(doc, "STKNHang",
					docTmp.getEmbedded(Arrays.asList("BankAccount", "AccountNumber"), "")));
			elementTmp.appendChild(commons.createElementWithValue(doc, "TNHang",
					docTmp.getEmbedded(Arrays.asList("BankAccount", "BankName"), "")));
			elementTmp.appendChild(commons.createElementWithValue(doc, "Fax", docTmp.get("Fax", "")));
			elementTmp.appendChild(commons.createElementWithValue(doc, "Website", docTmp.get("Website", "")));

			elementSubTmp = doc.createElement("TTKhac");
			if (!docTmp.get("NameEN", "").equals("")) {
				elementSubTmp
						.appendChild(commons.createElementTTKhac(doc, "TenEN", "string", docTmp.get("NameEN", "")));
			}
			elementTmp.appendChild(elementSubTmp);

			/* ADD THONG TIN TK NGAN HANG (NEU CO) */
			if (docTmp.get("BankAccountExt") != null && docTmp.getList("BankAccountExt", Document.class).size() > 0) {
				intTmp = 1;
//							elementTmp = doc.createElement("TTKhac");
//							elementSubTmp = doc.createElement("TTKhac");
				for (Document oo : docTmp.getList("BankAccountExt", Document.class)) {
					elementSubTmp.appendChild(commons.createElementTTKhac(doc, "STKNHang" + intTmp, "string",
							oo.get("AccountNumber", "")));
					elementSubTmp.appendChild(
							commons.createElementTTKhac(doc, "TNHang" + intTmp, "string", oo.get("BankName", "")));
					elementTmp.appendChild(elementSubTmp);
					intTmp++;
				}
			}
			elementSubContent.appendChild(elementTmp);

			elementTmp = doc.createElement("NMua"); // NGUOI MUA
			elementTmp.appendChild(commons.createElementWithValue(doc, "Ten", khTenDonVi));
			elementTmp.appendChild(commons.createElementWithValue(doc, "MST", khMst));
			elementTmp.appendChild(commons.createElementWithValue(doc, "DChi", khDiaChi));
			elementTmp.appendChild(commons.createElementWithValue(doc, "SDThoai", khSoDT));
			elementTmp.appendChild(commons.createElementWithValue(doc, "CCCDan", khCCCDan));

			elementTmp.appendChild(commons.createElementWithValue(doc, "DCTDTu", khEmail));
			elementTmp.appendChild(commons.createElementWithValue(doc, "HVTNMHang", khHoTenNguoiMua));
			elementTmp.appendChild(commons.createElementWithValue(doc, "STKNHang", khSoTk));
			elementTmp.appendChild(commons.createElementWithValue(doc, "TNHang", khTkTaiNganHang));
			elementTmp.appendChild(commons.createElementWithValue(doc, "TTKhac", ""));

			elementSubContent.appendChild(elementTmp);

			mapVATAmount = new LinkedHashMap<String, Double>();
			mapAmount = new LinkedHashMap<String, Double>();
			elementTmp = doc.createElement("DSHHDVu"); // HH-DV
			if (!jsonData.at("/DSSanPham").isMissingNode()) {
				for (JsonNode o : jsonData.at("/DSSanPham")) {
					if (!"".equals(commons.getTextJsonNode(o.at("/ProductName")))) {
						tmp = commons.getTextJsonNode(o.at("/VATRate")).replaceAll(",", "");
						switch (tmp) {
						case "0":
							tmp = "-1";
							break;
						case "1":
							tmp = "0%";
							break;

						case "5":
						case "8":
						case "10":
							tmp += "%";
							break;
						case "-1":
							tmp = "KCT";
							break;
						case "-2":
							tmp = "KKKNT";
							break;
						default:
							break;
						}
//									if(!("2".equals(commons.getTextJsonNode(o.at("/Feature"))))) {	// || "4".equals(commons.getTextJsonNode(o.at("/Feature")))
						if ("1".equals(commons.getTextJsonNode(o.at("/Feature")))
								|| "3".equals(commons.getTextJsonNode(o.at("/Feature")))
								|| "4".equals(commons.getTextJsonNode(o.at("/Feature")))) {
							mapAmount.compute(tmp, (k, v) -> {
								return (v == null
										? commons.ToNumber(commons.getTextJsonNode(o.at("/Total")))
												* ("3".equals(commons.getTextJsonNode(o.at("/Feature"))) ? -1 : 1)
										: v + commons.ToNumber(commons.getTextJsonNode(o.at("/Total")))
												* ("3".equals(commons.getTextJsonNode(o.at("/Feature"))) ? -1 : 1));
							});
							mapVATAmount.compute(tmp, (k, v) -> {
								return (v == null
										? commons.ToNumber(commons.getTextJsonNode(o.at("/VATAmount")))
												* ("3".equals(commons.getTextJsonNode(o.at("/Feature"))) ? -1 : 1)
										: v + commons.ToNumber(commons.getTextJsonNode(o.at("/VATAmount")))
												* ("3".equals(commons.getTextJsonNode(o.at("/Feature"))) ? -1 : 1));
							});
						}

						elementSubTmp = doc.createElement("HHDVu");
						elementSubTmp.appendChild(commons.createElementWithValue(doc, "TChat",
								commons.getTextJsonNode(o.at("/Feature"))));
						elementSubTmp.appendChild(
								commons.createElementWithValue(doc, "STT", commons.getTextJsonNode(o.at("/STT"))));
						elementSubTmp.appendChild(commons.createElementWithValue(doc, "MHHDVu",
								commons.getTextJsonNode(o.at("/ProductCode"))));
						elementSubTmp.appendChild(commons.createElementWithValue(doc, "THHDVu",
								commons.getTextJsonNode(o.at("/ProductName"))));

						elementSubTmp.appendChild(
								commons.createElementWithValue(doc, "DVTinh", commons.getTextJsonNode(o.at("/Unit"))));

						if (!("2".equals(commons.getTextJsonNode(o.at("/Feature"))))) { // ||
																						// "4".equals(commons.getTextJsonNode(o.at("/Feature")))
							elementSubTmp.appendChild(commons.createElementWithValue(doc, "SLuong",
									commons.getTextJsonNode(o.at("/Quantity")).replaceAll(",", "")));
							elementSubTmp.appendChild(commons.createElementWithValue(doc, "DGia",
									commons.getTextJsonNode(o.at("/Price")).replaceAll(",", "")));
							elementSubTmp.appendChild(commons.createElementWithValue(doc, "TLCKhau", ""));
							elementSubTmp.appendChild(commons.createElementWithValue(doc, "STCKhau", ""));
							elementSubTmp.appendChild(commons.createElementWithValue(doc, "ThTien",
									commons.getTextJsonNode(o.at("/Total")).replaceAll(",", "")));
							if (!tmp.equals("-1") && !loaiHoaDon.equals("2")) {
								elementSubTmp.appendChild(commons.createElementWithValue(doc, "TSuat", tmp));
							}
						}

						if (!loaiHoaDon.equals("2")) {
							elementSubTmp01 = doc.createElement("TTKhac");

							if (commons.getTextJsonNode(o.at("/VATAmount")).replaceAll(",", "").equals("")) {
								elementSubTmp01
										.appendChild(commons.createElementTTKhac(doc, "VATAmount", "decimal", "0"));
							} else {
								elementSubTmp01.appendChild(commons.createElementTTKhac(doc, "VATAmount", "decimal",
										commons.getTextJsonNode(o.at("/VATAmount")).replaceAll(",", "")));
							}

							elementSubTmp01.appendChild(commons.createElementTTKhac(doc, "Amount", "decimal",
									commons.getTextJsonNode(o.at("/Amount")).replaceAll(",", "")));
							elementSubTmp.appendChild(elementSubTmp01);

							elementTmp.appendChild(elementSubTmp);
							double a = commons.ToNumber(commons.getTextJsonNode(o.at("/VATRate")));

							hItem = new LinkedHashMap<String, Object>();
							hItem.put("STT", commons.getTextJsonNode(o.at("/STT")));
							hItem.put("ProductName", commons.getTextJsonNode(o.at("/ProductName")));
							hItem.put("ProductCode", commons.getTextJsonNode(o.at("/ProductCode")));
							hItem.put("Unit", commons.getTextJsonNode(o.at("/Unit")));
							hItem.put("Quantity", commons.ToNumber(commons.getTextJsonNode(o.at("/Quantity"))));
							hItem.put("Price", commons.ToNumber(commons.getTextJsonNode(o.at("/Price"))));
							hItem.put("Total", commons.ToNumber(commons.getTextJsonNode(o.at("/Total"))));
							double vatRate = 0.0;
							if (a == 0.0) {

							} else if (a == 1.0) {
								hItem.put("VATRate", vatRate);
							} else {
								hItem.put("VATRate", commons.ToNumber(commons.getTextJsonNode(o.at("/VATRate"))));
							}

							hItem.put("VATAmount", commons.ToNumber(commons.getTextJsonNode(o.at("/VATAmount"))));
							hItem.put("Amount", commons.ToNumber(commons.getTextJsonNode(o.at("/Amount"))));
							hItem.put("Feature", commons.getTextJsonNode(o.at("/Feature")));
							listDSHHDVu.add(hItem);
						} else {
							elementSubTmp01 = doc.createElement("TTKhac");
//							elementSubTmp01.appendChild(commons.createElementTTKhac(doc, "VATAmount", "decimal",
//									commons.getTextJsonNode(o.at("/VATAmount")).replaceAll(",", "")));
//							
							if (commons.getTextJsonNode(o.at("/VATAmount")).replaceAll(",", "").equals("")) {
								elementSubTmp01
										.appendChild(commons.createElementTTKhac(doc, "VATAmount", "decimal", "0"));
							} else {
								elementSubTmp01.appendChild(commons.createElementTTKhac(doc, "VATAmount", "decimal",
										commons.getTextJsonNode(o.at("/VATAmount")).replaceAll(",", "")));
							}
							
							elementSubTmp01.appendChild(commons.createElementTTKhac(doc, "Amount", "decimal",
									commons.getTextJsonNode(o.at("/Amount")).replaceAll(",", "")));
							elementSubTmp01.appendChild(commons.createElementTTKhac(doc, "Stt", "numeric",
									commons.getTextJsonNode(o.at("/STT"))));
							elementSubTmp.appendChild(elementSubTmp01);

							elementTmp.appendChild(elementSubTmp);
							hItem = new LinkedHashMap<String, Object>();
							hItem.put("STT", commons.getTextJsonNode(o.at("/STT")));
							hItem.put("ProductName", commons.getTextJsonNode(o.at("/ProductName")));
							hItem.put("Unit", commons.getTextJsonNode(o.at("/Unit")));
							hItem.put("Quantity", commons.ToNumber(commons.getTextJsonNode(o.at("/Quantity"))));
							hItem.put("Price", commons.ToNumber(commons.getTextJsonNode(o.at("/Price"))));
							hItem.put("Total", commons.ToNumber(commons.getTextJsonNode(o.at("/Total"))));
							hItem.put("VATRate", null);
							hItem.put("VATAmount", null);
							hItem.put("Amount", commons.ToNumber(commons.getTextJsonNode(o.at("/Amount"))));
							hItem.put("Feature", commons.getTextJsonNode(o.at("/Feature")));
							listDSHHDVu.add(hItem);
						}

					
					}

				}
			}
		
			
			
			if (!loaiHoaDon.equals("2")) {
			elementSubContent.appendChild(elementTmp);
			elementTmp = doc.createElement("TToan"); // Thong tin thanh toan
			elementSubTmp = doc.createElement("THTTLTSuat");
			/* DANH SACH CAC LOAI THUE SUAT */
			for (Map.Entry<String, Double> pair : mapVATAmount.entrySet()) {
				if (null != pair.getKey() && !"".equals(pair.getKey())) {
					if (!pair.getKey().equals("-1")) {
						elementSubTmp01 = doc.createElement("LTSuat");
						elementSubTmp01.appendChild(commons.createElementWithValue(doc, "TSuat", pair.getKey()));
						elementSubTmp01.appendChild(commons.createElementWithValue(doc, "ThTien",
								commons.formatNumberReal(mapAmount.get(pair.getKey())).replaceAll(",", "")));

						if (loaiTienTt.equals("VND")) {
							elementSubTmp01.appendChild(commons.createElementWithValue(doc, "TThue",
									String.format("%.0f", mapVATAmount.get(pair.getKey()))));
						} else {
//						elementSubTmp01.appendChild(commons.createElementWithValue(doc, "TThue",String.valueOf(mapVATAmount.get(pair.getKey()))));	
							elementSubTmp01.appendChild(commons.createElementWithValue(doc, "TThue",
									String.format("%.2f", mapVATAmount.get(pair.getKey()))));
						}

						elementSubTmp.appendChild(elementSubTmp01);
					}
				}
			}
			elementTmp.appendChild(elementSubTmp);
			elementTmp.appendChild(
					commons.createElementWithValue(doc, "TgTCThue", tongTienTruocThue.replaceAll(",", "")));
			elementTmp
					.appendChild(commons.createElementWithValue(doc, "TgTThue", tongTienThueGtgt.replaceAll(",", "")));

			elementSubTmp = doc.createElement("DSLPhi");
			elementSubTmp01 = doc.createElement("LPhi");
			elementSubTmp01.appendChild(commons.createElementWithValue(doc, "TLPhi", ""));
			elementSubTmp01.appendChild(commons.createElementWithValue(doc, "TPhi", "0"));
			elementSubTmp.appendChild(elementSubTmp01);

			elementTmp.appendChild(elementSubTmp);

			elementTmp.appendChild(commons.createElementWithValue(doc, "TTCKTMai", "0"));
			elementTmp
					.appendChild(commons.createElementWithValue(doc, "TgTTTBSo", tongTienDaCoThue.replaceAll(",", "")));
			elementTmp.appendChild(commons.createElementWithValue(doc, "TgTTTBChu", tienBangChu));
			elementSubTmp = doc.createElement("TTKhac");
			if (!"VND".equals(loaiTienTt)) {
				elementSubTmp.appendChild(
						commons.createElementTTKhac(doc, "TgTQDoi", "decimal", tongTienQuyDoi.replaceAll(",", "")));
			}

//						elementSubTmp.appendChild(commons.createElementWithValue(doc, "TgTQDoi", tongTienQuyDoi.replaceAll(",", "")));
			elementTmp.appendChild(elementSubTmp);

			elementSubContent.appendChild(elementTmp);

			elementContent.appendChild(elementSubContent);
			}
			else {
				elementSubContent.appendChild(elementTmp);
				elementTmp = doc.createElement("TToan"); // Thong tin thanh toan
				elementTmp.appendChild(
						commons.createElementWithValue(doc, "TgTCThue", tongTienTruocThue.replaceAll(",", "")));
				elementTmp
						.appendChild(commons.createElementWithValue(doc, "TgTThue", tongTienThueGtgt.replaceAll(",", "")));
				elementTmp.appendChild(commons.createElementWithValue(doc, "TTCKTMai", "0"));
				elementTmp
						.appendChild(commons.createElementWithValue(doc, "TgTTTBSo", tongTienDaCoThue.replaceAll(",", "")));
				elementTmp.appendChild(commons.createElementWithValue(doc, "TgTTTBChu", tienBangChu));
				elementSubContent.appendChild(elementTmp);
				elementContent.appendChild(elementSubContent);
			}
			
			
			

			isSdaveFile = commons.docW3cToFile(doc, pathDir, fileNameXML);
			if (!isSdaveFile) {
				throw new Exception("Lưu dữ liệu không thành công.");
			}
			/* END - TAO XML HOA DON */

//						MTDiep = SystemParams.MSTTCGP + commons.csRandomAlphaNumbericString(46 - SystemParams.MSTTCGP.length()).toUpperCase();
			String uuid1 = UUID.randomUUID().toString().replaceAll("-", "").toUpperCase();
			MTDiep = SystemParams.MSTTCGP
					+ commons.convertLocalDateTimeToString(LocalDateTime.now(), "yyyyMMddHHmmssSSS")
					+ uuid1.substring(0, 19);

			/* LUU DU LIEU HD */
			docTTHDLQuan = null;
			if (docEInvoiceTTDC != null) {
				docTTHDLQuan = new Document("_id", _id_tt_dc)
						.append("TCHDon",
								"3".equals(docEInvoiceTTDC.getEmbedded(Arrays.asList("HDSS", "TCTBao"), "")) ? "1"
										: "2")
						.append("LHDCLQuan", "1")
						.append("KHMSHDCLQuan", docEInvoiceTTDC.getEmbedded(Arrays.asList("TT_DC", "KHMSHDon"), ""))
						.append("KHHDCLQuan", docEInvoiceTTDC.getEmbedded(Arrays.asList("TT_DC", "KHHDon"), ""))
						.append("SHDCLQuan",
								String.valueOf(docEInvoiceTTDC.getEmbedded(Arrays.asList("TT_DC", "SHDon"), 0)))
						.append("NLHDCLQuan", commons.convertLocalDateTimeToString(
								commons.convertDateToLocalDateTime(
										docEInvoiceTTDC.getEmbedded(Arrays.asList("TT_DC", "NLap"), Date.class)),
								"yyyy-MM-dd"))
						.append("GChu", "");
			}
			docUpsert = new Document("_id", objectIdEInvoice).append("IssuerId", header.getIssuerId())
					.append("MTDiep", MTDiep)
//							.append("EInvoiceNumber", null)				//PHAT SINH KHI THUC HIEN KY
					.append("EInvoiceDetail", new Document("TTChung", new Document("THDon", loaiHoaDonText)
//							.append("MaHD", maHoadon)
							.append("LoaiHD", loaiHoaDon).append("MauSoHD", mauSoHdon)
							.append("KHMSHDon", docTmp.getEmbedded(Arrays.asList("DMMauSoKyHieu", "KHMSHDon"), ""))
							.append("KHHDon", docTmp.getEmbedded(Arrays.asList("DMMauSoKyHieu", "KHHDon"), ""))
							.append("NLap",
									commons.convertStringToLocalDate(ngayLap, Constants.FORMAT_DATE.FORMAT_DATE_WEB))
							.append("DVTTe", loaiTienTt).append("TGia", tyGia).append("HTTToanCode", hinhThucThanhToan)
							.append("HTTToan", hinhThucThanhToanText).append("TTHDLQuan", docTTHDLQuan))
							.append("NDHDon", new Document("NBan", new Document("Ten", docTmp.get("Name", ""))
									.append("MST", docTmp.get("TaxCode", "")).append("DChi", docTmp.get("Address", ""))
									.append("SDThoai", docTmp.get("Phone", ""))
									.append("DCTDTu", docTmp.get("Email", ""))
									.append("STKNHang",
											docTmp.getEmbedded(Arrays.asList("BankAccount", "AccountNumber"), ""))
									.append("TNHang", docTmp.getEmbedded(Arrays.asList("BankAccount", "BankName"), ""))
									.append("Fax", docTmp.get("Fax", "")).append("Website", docTmp.get("Website", "")))
									.append("NMua", new Document("Ten", khTenDonVi).append("MST", khMst)
											.append("DChi", khDiaChi)
//													.append("MKHang", khMKHang)
											.append("CCCDan", khCCCDan).append("SDThoai", khSoDT)
											.append("DCTDTu", khEmail).append("HVTNMHang", khHoTenNguoiMua)
											.append("STKNHang", khSoTk).append("TNHang", khTkTaiNganHang)))
							.append("DSHHDVu", listDSHHDVu).append("TToan",
									new Document("TgTCThue", commons.ToNumber(tongTienTruocThue))
											.append("TgTThue", commons.ToNumber(tongTienThueGtgt))
											.append("TgTTTBSo", commons.ToNumber(tongTienDaCoThue))
											.append("TgTQDoi", commons.ToNumber(tongTienQuyDoi))
											.append("TgTTTBChu", tienBangChu)))
					.append("SignStatusCode", Constants.INVOICE_SIGN_STATUS.NOSIGN)
					.append("EInvoiceStatus", Constants.INVOICE_STATUS.CREATED).append("PublishStatus", false)
					.append("IsDelete", false).append("SecureKey", secureKey).append("Dir", pathDir)
					.append("FileNameXML", fileNameXML).append("InfoCreated",
							new Document("CreateDate", LocalDateTime.now()).append("CreateUserID", header.getUserId())
									.append("CreateUserName", header.getUserName())
									.append("CreateUserFullName", header.getUserFullName()));
			/* END - LUU DU LIEU HD */

			mongoClient = cfg.mongoClient();
			collection = mongoClient.getDatabase(cfg.dbName).getCollection("EInvoiceMTT");
			collection.insertOne(docUpsert);
			mongoClient.close();

			name_company = removeAccent(header.getUserFullName());
			format_time = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
			time_dem = LocalDateTime.now();
			time = time_dem.format(format_time);
			System.out.println(time + name_company + " Vua copy hoa don tu may tinh tien.");
			responseStatus = new MspResponseStatus(0, "SUCCESS");
			rsp.setResponseStatus(responseStatus);
			return rsp;

		//////////////////////////////////////////////////////////////////////////////////////////////////////////////

		case Constants.MSG_ACTION_CODE.DELETE:
			objectId = null;
			objectIdMSKH = null;
			try {
				objectId = new ObjectId(_id);
			} catch (Exception e) {
			}

			/* KIEM TRA THONG TIN HD CO TON TAI KHONG */
			docFind = new Document("IssuerId", header.getIssuerId()).append("IsDelete", new Document("$ne", true))
					.append("_id", objectId);

			docTmp = null;

			mongoClient = cfg.mongoClient();
			collection = mongoClient.getDatabase(cfg.dbName).getCollection("EInvoiceMTT");
			try {
				docTmp = collection.find(docFind).allowDiskUse(true).iterator().next();
			} catch (Exception ex) {

			}
			mongoClient.close();

			if (null == docTmp) {
				responseStatus = new MspResponseStatus(9999, "Không tìm thấy thông tin hóa đơn.");
				rsp.setResponseStatus(responseStatus);
				return rsp;
			}
			String MSKH = docTmp.getEmbedded(Arrays.asList("EInvoiceDetail", "TTChung", "MauSoHD"), "");

			int SHD = docTmp.getEmbedded(Arrays.asList("EInvoiceDetail", "TTChung", "SHDon"), 0);
			if (SHD == 0) {
				// CAP NHAT HOA DON DA XOA TRONG EINVOICE
				options = new FindOneAndUpdateOptions();
				options.upsert(false);
				options.maxTime(5000, TimeUnit.MILLISECONDS);
				options.returnDocument(ReturnDocument.AFTER);

				mongoClient = cfg.mongoClient();
				collection = mongoClient.getDatabase(cfg.dbName).getCollection("EInvoiceMTT");
				collection.findOneAndUpdate(docFind, new Document("$set", new Document("IsDelete", true).append(
						"InfoDeleted",
						new Document("DeletedDate", LocalDateTime.now()).append("DeletedUserID", header.getUserId())
								.append("DeletedUserName", header.getUserName())
								.append("DeletedUserFullName", header.getUserFullName()))),
						options);
				mongoClient.close();

				name_company = removeAccent(header.getUserFullName());
				format_time = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
				time_dem = LocalDateTime.now();
				time = time_dem.format(format_time);
				System.out.println(time + name_company + " Vua xoa hoa don tu may tinh tien.");
				responseStatus = new MspResponseStatus(0, "SUCCESS");
				rsp.setResponseStatus(responseStatus);
				return rsp;
			} else {
				objectIdMSKH = new ObjectId(MSKH);
				docFind1 = new Document("IssuerId", header.getIssuerId()).append("IsDelete", new Document("$ne", true))
						.append("_id", objectIdMSKH);

				docTmp1 = null;

				mongoClient = cfg.mongoClient();
				collection = mongoClient.getDatabase(cfg.dbName).getCollection("DMMauSoKyHieu");
				try {
					docTmp1 = collection.find(docFind1).allowDiskUse(true).iterator().next();
				} catch (Exception ex) {

				}
				mongoClient.close();

				if (null == docTmp1) {
					responseStatus = new MspResponseStatus(9999, "Không tìm thấy thông tin mẫu số ký hiệu.");
					rsp.setResponseStatus(responseStatus);
					return rsp;
				}
				int SHDHT = docTmp1.getInteger("SHDHT");
				int SHDCL = docTmp1.getInteger("ConLai");
				if (SHD == SHDHT) {
					// CAP NHAT HOA DON DA XOA TRONG EINVOICE

					int SHDHT_ = SHDHT - 1;
					int SHDCL_ = SHDCL + 1;
					options = new FindOneAndUpdateOptions();
					options.upsert(false);
					options.maxTime(5000, TimeUnit.MILLISECONDS);
					options.returnDocument(ReturnDocument.AFTER);
					// CAP NHAT EINVOICE

					mongoClient = cfg.mongoClient();
					collection = mongoClient.getDatabase(cfg.dbName).getCollection("EInvoiceMTT");
					collection.findOneAndUpdate(docFind, new Document("$set", new Document("IsDelete", true).append(
							"InfoDeleted",
							new Document("DeletedDate", LocalDateTime.now()).append("DeletedUserID", header.getUserId())
									.append("DeletedUserName", header.getUserName())
									.append("DeletedUserFullName", header.getUserFullName()))),
							options);
					mongoClient.close();
					// CAP NHAT MAU SO KY HIEU TRA VE SO HOA DON

					mongoClient = cfg.mongoClient();
					collection = mongoClient.getDatabase(cfg.dbName).getCollection("DMMauSoKyHieu");
					collection.findOneAndUpdate(docFind1,
							new Document("$set", new Document("ConLai", SHDCL_).append("SHDHT", SHDHT_)), options);
					mongoClient.close();

					name_company = removeAccent(header.getUserFullName());
					format_time = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
					time_dem = LocalDateTime.now();
					time = time_dem.format(format_time);
					System.out.println(time + name_company + " Vua tao hoa don tu may tinh tien.");
					responseStatus = new MspResponseStatus(0, "SUCCESS");
					rsp.setResponseStatus(responseStatus);
					return rsp;
				} else {
					responseStatus = new MspResponseStatus(9999,
							"Không thể xóa hóa đơn này. Chỉ có thể xóa số hóa đơn lớn nhất!");
					rsp.setResponseStatus(responseStatus);
					return rsp;
				}
			}

			// TRONG EINVOICE LẤY ĐƯỢC SỐ HÓA ĐƠN HIỆN TẠI ĐỂ CHECK TRONG BẢNG MẪU SỐ KÍ
			// HIỆU

			// DỰA VÀO HÓA ĐƠN LẤY RA ĐƯỢC MẪU SỐ KÍ HIỆU VÀ CHECK TRONG BẢNG MẪU SỐ KÍ HIỆU
			// LẤY RA SHDHT

			// SO SÁNH 2 SỐ HÓA ĐƠN NẾU BẰNG NHAU THÌ CÓ THỂ XÓA. NGƯỢC LẠI THÌ KHÔNG THỂ
			// XÓA

			// NẾU XÓA SỐ HÓA ĐƠN THÌ CẬP NHẬT LẠI MẪU SỐ KÍ HIỆU SỐ LƯỢNG CÒN LẠI VÀ TỔNG
			// SỐ LƯỢNG ĐÃ SỬ DỤNG

		case Constants.MSG_ACTION_CODE.SEND_CQT:
			objectId = null;
			try {
				objectId = new ObjectId(_id);
			} catch (Exception e) {
			}
			/* KIEM TRA THONG TIN HD CO TON TAI KHONG */
			docFind = new Document("IssuerId", header.getIssuerId()).append("IsDelete", new Document("$ne", true))
					.append("_id", objectId).append("EInvoiceStatus", Constants.INVOICE_STATUS.PENDING);
			pipeline = new ArrayList<Document>();
			pipeline.add(new Document("$match", docFind));
			docTmp = null;

			mongoClient = cfg.mongoClient();
			collection = mongoClient.getDatabase(cfg.dbName).getCollection("EInvoiceMTT");
			try {
				docTmp = collection.aggregate(pipeline).allowDiskUse(true).iterator().next();
			} catch (Exception ex) {

			}
			mongoClient.close();

			if (null == docTmp) {
				responseStatus = new MspResponseStatus(9999, "Không tìm thấy thông tin hóa đơn.");
				rsp.setResponseStatus(responseStatus);
				return rsp;
			}

			String dir = docTmp.get("Dir", "");

			String fileName = "";
//			String check_file_signed = docTmp.get("SignStatusCode", "NOSIGN");					
//				fileName = _id + "_pending.xml";
			fileName = _id + "_signed.xml";

			file = new File(dir, fileName);
			if (!file.exists() || !file.isFile()) {
				responseStatus = new MspResponseStatus(9999, "Không tìm thấy thông tin hóa đơn đã ký!!!");
				rsp.setResponseStatus(responseStatus);
				return rsp;
			}

			doc = commons.fileToDocument(file, true);

			if (null == doc) {
				responseStatus = new MspResponseStatus(9999, "Dữ liệu hóa đơn không tồn tại.");
				rsp.setResponseStatus(responseStatus);
				return rsp;
			}

			org.w3c.dom.Document rTCTN = null;
			org.w3c.dom.Document rTCTN1 = null;
			String codeTTTNhan = "";
			String descTTTNhan = "";
			String MaKetQua = "";
			String CQT_MLTDiep = "";
			String MLoi = "";
			Node nodeKetQuaTraCuu = null;

			MST = docTmp.getEmbedded(Arrays.asList("EInvoiceDetail", "NDHDon", "NBan", "MST"), "");
			MTDiep = docTmp.get("MTDiep", "");

			// TRA CUU HOA DƠN TRC KHI CALL TIEP NHAN THONG DIEP

			try {
				rTCTN1 = tctnService.callTraCuuThongDiep(MTDiep);
			} catch (Exception e) {
			}

			if (rTCTN1 == null) {

				try {
					rTCTN1 = tctnService.callTraCuuThongDiep(MTDiep);
				} catch (Exception e) {
				}
			}

			if (rTCTN1 == null) {
				responseStatus = new MspResponseStatus(9999, "Vui lòng liên hệ nhà cung cấp để được xử lý!!!.");
				rsp.setResponseStatus(responseStatus);
				return rsp;
			}

			XPath xPath1 = XPathFactory.newInstance().newXPath();
			nodeKetQuaTraCuu = (Node) xPath1.evaluate("/KetQuaTraCuu", rTCTN1, XPathConstants.NODE);
			MaKetQua = commons
					.getTextFromNodeXML((Element) xPath1.evaluate("MaKetQua", nodeKetQuaTraCuu, XPathConstants.NODE));
			if ("2".equals(MaKetQua)) {

				try {
					rTCTN = tctnService.callTiepNhanThongDiepMTT(doc);
				} catch (Exception e) {
				}
				XPath xPath = XPathFactory.newInstance().newXPath();
				Node nodeDLHDon = (Node) xPath.evaluate("/TDiep", rTCTN, XPathConstants.NODE);
				codeTTTNhan = commons.getTextFromNodeXML(
						(Element) xPath.evaluate("DLieu/TBao/TTTNhan", nodeDLHDon, XPathConstants.NODE));
				descTTTNhan = commons.getTextFromNodeXML(
						(Element) xPath.evaluate("DLieu/TBao/DSLDo/LDo/MTa", nodeDLHDon, XPathConstants.NODE));
			} else {
				Node nodeTDiep = null;
				for (int i = 1; i <= 5; i++) {
					if (xPath1.evaluate("DuLieu/TDiep[" + i + "]", nodeKetQuaTraCuu, XPathConstants.NODE) == null)
						break;
					nodeTDiep = (Node) xPath1.evaluate("DuLieu/TDiep[" + i + "]", nodeKetQuaTraCuu,
							XPathConstants.NODE);
//										if(xPath.evaluate("DLieu/*/DSCKS", nodeTDiep, XPathConstants.NODE) != null)
					if (xPath1.evaluate("DLieu/*/MCCQT", nodeTDiep, XPathConstants.NODE) != null)
						break;
				}
				CQT_MLTDiep = commons.getTextFromNodeXML(
						(Element) xPath1.evaluate("TTChung/MLTDiep", nodeTDiep, XPathConstants.NODE));

				if ("204".equals(CQT_MLTDiep)) {
					String LTBao = commons.getTextFromNodeXML(
							(Element) xPath1.evaluate("DLieu/TBao/DLTBao/LTBao", nodeTDiep, XPathConstants.NODE));
					if (!LTBao.equals("2")) {
						MLoi = commons.getTextFromNodeXML((Element) xPath1.evaluate(
								"DLieu/TBao/DLTBao/LHDMTTien/DSLDo/LDo/MLoi", nodeTDiep, XPathConstants.NODE));
						responseStatus = new MspResponseStatus(9999, MLoi);
						rsp.setResponseStatus(responseStatus);
						return rsp;
					}
				}
			}
			switch (codeTTTNhan) {
			case "1":
				responseStatus = new MspResponseStatus(9999,
						"".equals(descTTTNhan) ? "Không tìm thấy tenant dữ liệu." : descTTTNhan);
				rsp.setResponseStatus(responseStatus);
				return rsp;
			case "2":
				responseStatus = new MspResponseStatus(9999, "Mã thông điệp đã tồn tại.");
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
			/* CAP NHAT LAI TRANG THAI DANG CHO XU LY */

			try {
				options = new FindOneAndUpdateOptions();
				options.upsert(false);
				options.maxTime(5000, TimeUnit.MILLISECONDS);
				options.returnDocument(ReturnDocument.AFTER);

				mongoClient = cfg.mongoClient();
				collection = mongoClient.getDatabase(cfg.dbName).getCollection("EInvoiceMTT");
				collection.findOneAndUpdate(docFind,
						new Document("$set", new Document("EInvoiceStatus", Constants.INVOICE_STATUS.PROCESSING)
								.append("SendCQT_Date", LocalDateTime.now()).append("InfoSendCQT",
										new Document("Date", LocalDateTime.now()).append("UserID", header.getUserId())
												.append("UserName", header.getUserName())
												.append("UserFullName", header.getUserFullName()))),
						options);
				mongoClient.close();

				name_company = removeAccent(header.getUserFullName());
				format_time = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
				time_dem = LocalDateTime.now();
				time = time_dem.format(format_time);
				System.out.println(time + name_company + " Vua gui CQT hoa don tu may tinh tien.");
				responseStatus = new MspResponseStatus(0, Constants.MAP_ERROR.get(0));
				rsp.setResponseStatus(responseStatus);
				return rsp;
			} catch (Exception e) {
				responseStatus = new MspResponseStatus(9999, "Vui lòng gửi lấy mã hóa đơn lại.");
				rsp.setResponseStatus(responseStatus);
				return rsp;
			}
			
		case Constants.MSG_ACTION_CODE.SEND_CQTALL:
			objectId = null;
			String id = "";
			int count = 0;
			String hdError = "";
			for (JsonNode o : jsonData.at("/_ids")) {
				id = commons.getTextJsonNode(o);
				try {
					objectId = new ObjectId(id);
				} catch (Exception e) {
				}

				docFind = new Document("IssuerId", header.getIssuerId()).append("IsDelete", false)
						.append("_id", objectId).append("EInvoiceStatus", Constants.INVOICE_STATUS.PENDING)
						.append("SignStatusCode", Constants.INVOICE_SIGN_STATUS.SIGNED);

				pipeline = new ArrayList<Document>();
				pipeline.add(new Document("$match", docFind));
				pipeline.add(new Document("$project",
						new Document("_id", 1).append("Dir", 1).append("MTDiep", 1).append("EInvoiceDetail", 1)));
				docTmp = null;

				mongoClient = cfg.mongoClient();
				collection = mongoClient.getDatabase(cfg.dbName).getCollection("EInvoiceMTT");
				try {
					docTmp = collection.aggregate(pipeline).allowDiskUse(true).iterator().next();
				} catch (Exception ex) {

				}
				mongoClient.close();

				if (null == docTmp) {
					count++;
					break;
				}
				int invoiceNumberCurrent = docTmp.getEmbedded(Arrays.asList("EInvoiceDetail", "TTChung", "SHDon"), 0);
				String dira = docTmp.get("Dir", "");

				String fileNamea = "";
				fileNamea = id + "_signed.xml";

				file = new File(dira, fileNamea);
				if (!file.exists() || !file.isFile()) {
					count += 1;
					hdError += invoiceNumberCurrent + ",";
					break;
				}

				doc = commons.fileToDocument(file, true);
				if (null == doc) {
					count += 1;
					hdError += invoiceNumberCurrent + ",";
					break;
				}

				org.w3c.dom.Document rTCTNa = null;
				org.w3c.dom.Document rTCTN1a = null;
				String codeTTTNhana = "";
				String MaKetQuaa = "";
				String CQT_MLTDiepa = "";
				Node nodeKetQuaTraCuua = null;
				MST = docTmp.getEmbedded(Arrays.asList("EInvoiceDetail", "NDHDon", "NBan", "MST"), "");
				MTDiep = docTmp.get("MTDiep", "");

				try {
					rTCTN1a = tctnService.callTraCuuThongDiep(MTDiep);
				} catch (Exception e) {
				}

				if (rTCTN1a == null) {
					try {
						rTCTN1a = tctnService.callTraCuuThongDiep(MTDiep);
					} catch (Exception e) {
					}
				}

				XPath xPath11 = XPathFactory.newInstance().newXPath();
				nodeKetQuaTraCuua = (Node) xPath11.evaluate("/KetQuaTraCuu", rTCTN1a, XPathConstants.NODE);
				MaKetQuaa = commons.getTextFromNodeXML(
						(Element) xPath11.evaluate("MaKetQua", nodeKetQuaTraCuua, XPathConstants.NODE));

				if ("2".equals(MaKetQuaa)) {
					try {
						rTCTNa = tctnService.callTiepNhanThongDiepMTT(doc);
					} catch (Exception e) {
					}
					XPath xPath = XPathFactory.newInstance().newXPath();
					Node nodeDLHDon = (Node) xPath.evaluate("/TDiep", rTCTNa, XPathConstants.NODE);
					codeTTTNhana = commons.getTextFromNodeXML(
							(Element) xPath.evaluate("DLieu/TBao/TTTNhan", nodeDLHDon, XPathConstants.NODE));
				} else {
					Node nodeTDiep = null;
					for (int i = 1; i <= 5; i++) {
						if (xPath11.evaluate("DuLieu/TDiep[" + i + "]", nodeKetQuaTraCuua, XPathConstants.NODE) == null)
							break;
						nodeTDiep = (Node) xPath11.evaluate("DuLieu/TDiep[" + i + "]", nodeKetQuaTraCuua,
								XPathConstants.NODE);
						if (xPath11.evaluate("DLieu/*/MCCQT", nodeTDiep, XPathConstants.NODE) != null)
							break;
					}
					CQT_MLTDiepa = commons.getTextFromNodeXML(
							(Element) xPath11.evaluate("TTChung/MLTDiep", nodeTDiep, XPathConstants.NODE));

					if ("202".equals(CQT_MLTDiepa)) {
						codeTTTNhan = MaKetQuaa;
					}
					if ("204".equals(CQT_MLTDiepa)) {
						count += 1;
						hdError += invoiceNumberCurrent + ",";
						break;
					}
				}

				switch (codeTTTNhana) {
				case "1":
					count += 1;
					hdError += invoiceNumberCurrent + ",";
					break;
				case "2":
					count += 1;
					hdError += invoiceNumberCurrent + ",";
					break;
				case "3":
					count += 1;
					hdError += invoiceNumberCurrent + ",";
					break;
				default:

					break;
				}

				options = new FindOneAndUpdateOptions();
				options.upsert(false);
				options.maxTime(5000, TimeUnit.MILLISECONDS);
				options.returnDocument(ReturnDocument.AFTER);

				mongoClient = cfg.mongoClient();
				collection = mongoClient.getDatabase(cfg.dbName).getCollection("EInvoiceMTT");
				collection.findOneAndUpdate(docFind,
						new Document("$set", new Document("EInvoiceStatus", Constants.INVOICE_STATUS.PROCESSING)
								.append("SendCQT_Date", LocalDateTime.now()).append("InfoSendCQT",
										new Document("Date", LocalDateTime.now()).append("UserID", header.getUserId())
												.append("UserName", header.getUserName())
												.append("UserFullName", header.getUserFullName()))),
						options);
				mongoClient.close();

				name_company = removeAccent(header.getUserFullName());
				format_time = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
				time_dem = LocalDateTime.now();
				time = time_dem.format(format_time);
				System.out.println(time + name_company + " Vua gui CQT hoa don tu may tinh tien.");
			}
			if (count != 0) {
				responseStatus = new MspResponseStatus(9999,
						count + " Hóa đơn gửi cqt không thành công là : " + hdError);
				rsp.setResponseStatus(responseStatus);
				return rsp;
			}
			responseStatus = new MspResponseStatus(0, Constants.MAP_ERROR.get(0));
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

		String mauSoHdon = "";
		String soHoaDon = "";
		String fromDate = "";
		String toDate = "";
		String status = "";
		String signStatus = "";
		String emailStatus = "";
		String sendCQTStatus = "";
		String nbanMST = "";
		String nbanTen = "";
		String maCQT = "";
//		String nameKH = "";
//		String mst = "";
		String nmuaTen = "";

		JsonNode jsonData = null;
		if (objData != null) {
			jsonData = Json.serializer().nodeFromObject(objData);
			mauSoHdon = commons.getTextJsonNode(jsonData.at("/MauSoHdon")).replaceAll("\\s", "");
			soHoaDon = commons.getTextJsonNode(jsonData.at("/SoHoaDon")).replaceAll("\\s", "");
			fromDate = commons.getTextJsonNode(jsonData.at("/FromDate")).replaceAll("\\s", "");
			toDate = commons.getTextJsonNode(jsonData.at("/ToDate")).replaceAll("\\s", "");
			status = commons.getTextJsonNode(jsonData.at("/Status")).replaceAll("\\s", "");
			emailStatus = commons.getTextJsonNode(jsonData.at("/EmailStatus")).replaceAll("\\s", "");
			sendCQTStatus = commons.getTextJsonNode(jsonData.at("/SendCQTStatus")).replaceAll("\\s", "");
			signStatus = commons.getTextJsonNode(jsonData.at("/SignStatus")).replaceAll("\\s", "");
			nbanMST = commons.getTextJsonNode(jsonData.at("/NbanMST")).trim().replaceAll("\\s+", "");
			nbanTen = commons.getTextJsonNode(jsonData.at("/NbanTen")).trim().replaceAll("\\s+", " ");
			maCQT = commons.getTextJsonNode(jsonData.at("/MaCQT")).trim().replaceAll("\\s+", "");
//			nameKH = commons.getTextJsonNode(jsonData.at("/NameKH")).trim().replaceAll("\\s+", " ");
//			mst = commons.getTextJsonNode(jsonData.at("/MST")).trim().replaceAll("\\s+", "");
			nmuaTen = commons.getTextJsonNode(jsonData.at("/NmuaTen")).trim().replaceAll("\\s+", " ");
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

		docMatch = new Document("IssuerId", header.getIssuerId()).append("IsDelete", new Document("$ne", true));
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
		if (!"".equals(nbanMST))
			docMatch.append("EInvoiceDetail.NDHDon.NMua.MST",
					new Document("$regex", commons.regexEscapeForMongoQuery(nbanMST)).append("$options", "i"));
		if (!"".equals(nbanTen))
			docMatch.append("EInvoiceDetail.NDHDon.NMua.Ten",
					new Document("$regex", commons.regexEscapeForMongoQuery(nbanTen)).append("$options", "i"));
		if (!"".equals(nmuaTen))
			docMatch.append("EInvoiceDetail.NDHDon.NBan.Ten",
					new Document("$regex", commons.regexEscapeForMongoQuery(nmuaTen)).append("$options", "i"));
		

		

		if (!"".equals(emailStatus)) {
			if (emailStatus.equals("true")) {
				docMatch.append("EmailStatus", true);
			} else {
				docMatch.append("EmailStatus", false);
				docMatch.append("EmailStatus", new Document("$in", Arrays.asList(false, null)));
			}
		}

		if (!"".equals(sendCQTStatus)) {
			if (sendCQTStatus.equals("true")) {
				docMatch.append("SendCQTStatus", true);
			} else {
				docMatch.append("SendCQTStatus", new Document("$in", Arrays.asList(false, null)));
			}
		}

		if (!"".equals(maCQT))
			docMatch.append("MCCQT", maCQT);

		pipeline = new ArrayList<Document>();
		pipeline.add(new Document("$match", docMatch));
		pipeline.add(new Document("$addFields", new Document("SHDon",
				new Document("$ifNull", Arrays.asList("$EInvoiceDetail.TTChung.SHDon", Integer.MAX_VALUE)))));
		pipeline.add(new Document("$sort",
				new Document("EInvoiceDetail.TTChung.MauSoHD", -1).append("SHDon", -1).append("_id", -1)));
		pipeline.addAll(createFacetForSearchNotSort(page));

		MongoClient mongoClient = cfg.mongoClient();
		MongoCollection<Document> collection = mongoClient.getDatabase(cfg.dbName).getCollection("EInvoiceMTT");
		try {
			docTmp = collection.aggregate(pipeline).allowDiskUse(true).iterator().next();
		} catch (Exception e) {

		}

		mongoClient.close();

		if (null == docTmp) {
			responseStatus = new MspResponseStatus(9999, Constants.MAP_ERROR.get(9999));
			rsp.setResponseStatus(responseStatus);
			return rsp;
		}
		rsp = new MsgRsp(header);

		page.setTotalRows(docTmp.getInteger("total", 0));
		rsp.setMsgPage(page);

		responseStatus = null;
		List<Document> rows = null;
		if (docTmp != null) {
			if (docTmp.get("data") != null && docTmp.get("data") instanceof List) {
				rows = docTmp.getList("data", Document.class);
			}
		}

		if (null != rows) {
			for (Document doc : rows) {
				objectId = (ObjectId) doc.get("_id");

				hItem = new HashMap<String, Object>();
				hItem.put("_id", objectId.toString());
				hItem.put("EInvoiceStatus", doc.get("EInvoiceStatus"));
				hItem.put("SignStatusCode", doc.get("SignStatusCode"));
				hItem.put("MCCQT", doc.get("MCCQT"));
				hItem.put("MTDiep", doc.get("MTDiep"));
				hItem.put("MTDTChieu", doc.get("MTDTChieu"));
				hItem.put("EInvoiceDetail", doc.get("EInvoiceDetail"));
				hItem.put("InfoCreated", doc.get("InfoCreated"));
				hItem.put("LDo", doc.get("LDo"));
				hItem.put("HDSS", doc.get("HDSS"));
				hItem.put("SendCQT_Date", doc.get("SendCQT_Date"));
				hItem.put("CQT_Date", doc.get("CQT_Date"));
				hItem.put("EmailStatus", doc.get("EmailStatus"));
				hItem.put("PublishStatus", doc.get("PublishStatus"));
				hItem.put("SendCQTStatus", doc.get("SendCQTStatus"));

				rowsReturn.add(hItem);
			}
		}

		String name_company = removeAccent(header.getUserFullName());
		DateTimeFormatter format_time = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
		LocalDateTime time_dem = LocalDateTime.now();
		String time = time_dem.format(format_time);
		System.out.println(time + name_company + " Vua search hoa don tu may tinh tien.");

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

		List<Document> pipeline = new ArrayList<Document>();

		Document docFind = new Document("IssuerId", header.getIssuerId()).append("IsDelete", new Document("$ne", true))
				.append("_id", objectId);

		pipeline = new ArrayList<Document>();
		pipeline.add(new Document("$match", docFind));

		pipeline.add(new Document("$lookup", new Document("from", "PramLink")
				.append("pipeline",
						Arrays.asList(new Document("$match", new Document("$expr", new Document("IsDelete", false))),
								new Document("$project", new Document("_id", 1).append("LinkPortal", 1))))
				.append("as", "PramLink")));
		pipeline.add(
				new Document("$unwind", new Document("path", "$PramLink").append("preserveNullAndEmptyArrays", true)));

		Document docTmp = null;

		MongoClient mongoClient = cfg.mongoClient();
		MongoCollection<Document> collection = mongoClient.getDatabase(cfg.dbName).getCollection("EInvoiceMTT");
		try {
			docTmp = collection.aggregate(pipeline).allowDiskUse(true).iterator().next();
		} catch (Exception e) {

		}

		mongoClient.close();

		if (null == docTmp) {
			responseStatus = new MspResponseStatus(9999, Constants.MAP_ERROR.get(9999));
			rsp.setResponseStatus(responseStatus);
			return rsp;
		}

		String name_company = removeAccent(header.getUserFullName());
		DateTimeFormatter format_time = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
		LocalDateTime time_dem = LocalDateTime.now();
		String time = time_dem.format(format_time);
		System.out.println(time + name_company + " Vua xem chi tiet hoa don tu may tinh tien.");

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

		ObjectId objectId = null;
		String _id = commons.getTextJsonNode(jsonData.at("/_id")).replaceAll("\\s", "");

		int currentYear = LocalDate.now().get(ChronoField.YEAR);
		try {
			objectId = new ObjectId(_id);
		} catch (Exception e) {
		}

		/* NHO KIEM TRA XEM CO HD NAO DANG KY KHONG */
		Document docFind = new Document("IssuerId", header.getIssuerId()).append("IsDelete", new Document("$ne", true))
				.append("_id", objectId).append("EInvoiceStatus", "PENDING").append("SignStatusCode", "NOSIGN");

		List<Document> pipeline = new ArrayList<Document>();
		pipeline.add(new Document("$match", docFind));
		/* KIEM TRA THONG TIN MAU HD */
		pipeline.add(new Document("$lookup", new Document("from", "DMMauSoKyHieu")
				.append("let",
						new Document("vMauSoHD", "$EInvoiceDetail.TTChung.MauSoHD").append("vIssuerId", "$IssuerId"))
				.append("pipeline", Arrays.asList(new Document("$match", new Document("$expr", new Document("$and",
						Arrays.asList(new Document("$eq", Arrays.asList("$IsActive", true)),
								new Document("$ne", Arrays.asList("$IsDelete", true)),
								new Document("$eq", Arrays.asList(new Document("$toString", "$_id"), "$$vMauSoHD")),
								new Document("$eq", Arrays.asList("$IssuerId", "$$vIssuerId")),
								new Document("$eq", Arrays.asList("$NamPhatHanh", currentYear))))))))
				.append("as", "DMMauSoKyHieu")));
		pipeline.add(new Document("$unwind",
				new Document("path", "$DMMauSoKyHieu").append("preserveNullAndEmptyArrays", true)));
		Document docTmp = null;

		MongoClient mongoClient = cfg.mongoClient();
		MongoCollection<Document> collection = mongoClient.getDatabase(cfg.dbName).getCollection("EInvoiceMTT");
		try {
			docTmp = collection.aggregate(pipeline).allowDiskUse(true).iterator().next();
		} catch (Exception e) {

		}

		mongoClient.close();

		if (null == docTmp) {
			return fileInfo;
		}

		// CHECK STATUS CO DC ACTIVE CHUA
		boolean check_active = docTmp.getEmbedded(Arrays.asList("DMMauSoKyHieu", "Status"), false);
		if (check_active == true) {
			fileInfo.setCheck("Mẫu hóa đơn đang được admin xử lý. Vui lòng chờ trong giây lát!!!");
			return fileInfo;
		}
		// END CHECK STATUS

		/* AP DUNG 1 FILE TRUOC */
		String dir = docTmp.get("Dir", "");
		String fileName = docTmp.get("FileNameXML", "");

		String fileNamePending = _id + "_preparingSendCQT.xml";
		File file = new File(dir, fileNamePending);

		if (!file.exists())
			return fileInfo;

		/* DOC DU LIEU XML, VA GHI DU LIEU VO SO HD */
		org.w3c.dom.Document doc = commons.fileToDocument(file);
		fileInfo.setFileName(fileName);
		fileInfo.setContentFile(commons.docW3cToByte(doc));

//		
//		/* UPDATE EINVOICE - STATUS */
//		options = new FindOneAndUpdateOptions();
//		options.upsert(false);
//		options.maxTime(5000, TimeUnit.MILLISECONDS);
//		options.returnDocument(ReturnDocument.AFTER);
//
//		mongoTemplate.getCollection("EInvoiceMTT").findOneAndUpdate(docFind, new Document("$set",
//				new Document("EInvoiceStatus", "PENDING")),
//				options);
//		
		return fileInfo;
	}

	@Override
	public MsgRsp signSingle(InputStream is, JSONRoot jsonRoot) throws Exception {
		Msg msg = jsonRoot.getMsg();
		MsgHeader header = msg.getMsgHeader();
		MsgPage page = msg.getMsgPage();

		MsgRsp rsp = new MsgRsp(header);
		rsp.setMsgPage(page);
		MspResponseStatus responseStatus = null;

		/* DOC NOI DUNG XML DA KY */
		org.w3c.dom.Document xmlDoc = commons.inputStreamToDocument(is, false);
		/*
		 * KIEM TRA THONG TIN FILE DA KY DOC DU LIEU VA LUU VO THU MUC TUONG UNG
		 */
		// ==> PROCESSING

		XPath xPath = XPathFactory.newInstance().newXPath();
		Node nodeDLHDon = (Node) xPath.evaluate("/TDiep/DLieu/HDon/DLHDon[@Id='data']", xmlDoc, XPathConstants.NODE);
		Node nodeTmp = null;
		nodeTmp = (Node) xPath.evaluate("TTChung", nodeDLHDon, XPathConstants.NODE);

		String keySystem = "";
		int eInvoiceNumber = 0;
		eInvoiceNumber = commons.stringToInteger(
				commons.getTextFromNodeXML((Element) xPath.evaluate("SHDon", nodeTmp, XPathConstants.NODE)));

		String key = "";
		NodeList nodeList = (NodeList) xPath.evaluate("TTKhac/TTin", nodeTmp, XPathConstants.NODESET);
		for (int i = 0; i < nodeList.getLength(); i++) {
			key = commons
					.getTextFromNodeXML((Element) xPath.evaluate("TTruong", nodeList.item(i), XPathConstants.NODE));
			if ("SystemKey".equals(key)) {
				keySystem = commons
						.getTextFromNodeXML((Element) xPath.evaluate("DLieu", nodeList.item(i), XPathConstants.NODE));
				break;
			}
		}

		/* LAY THONG TIN NGAY LAP - NGAY KY TRONG FILE XML */
		String NLap = commons.getTextFromNodeXML((Element) xPath.evaluate("NLap", nodeTmp, XPathConstants.NODE));
//		String SigningTime = commons.getTextFromNodeXML((Element) xPath.evaluate(
//				"/HDon/DSCKS/NBan/Signature/Object[@Id='SigningTime']/SignatureProperties/SignatureProperty/SigningTime",
//				xmlDoc, XPathConstants.NODE));

		LocalDate ldNLap = null;
//		LocalDate ldSigningTime = null;
		try {
			ldNLap = commons.convertStringToLocalDate(NLap, "yyyy-MM-dd");
		} catch (Exception e) {
		}
//		if (SigningTime.length() > 10) {
//			ldSigningTime = commons.convertStringToLocalDate(SigningTime.substring(0, 10), "yyyy-MM-dd");
//		}
//		if (commons.compareLocalDate(ldNLap, ldSigningTime) != 0) {
//			responseStatus = new MspResponseStatus(9999, "Ngày lập và ngày ký dữ liệu không trùng nhau.");
//			rsp.setResponseStatus(responseStatus);
//			return rsp;
//		}

		ObjectId objectId = null;
		try {
			objectId = new ObjectId(keySystem);
		} catch (Exception e) {
		}
		List<Document> pipeline = null;
		/* KIEM TRA THONG TIN HOP LE KHONG */
		Document docFind = new Document("IssuerId", header.getIssuerId()).append("IsDelete", new Document("$ne", true))
				.append("EInvoiceDetail.TTChung.SHDon", eInvoiceNumber)
				.append("EInvoiceStatus", "PENDING").append("_id", objectId);
		pipeline = new ArrayList<Document>();
		pipeline.add(new Document("$match", docFind));
		Document docTmp = null;

		MongoClient mongoClient = cfg.mongoClient();
		MongoCollection<Document> collection = mongoClient.getDatabase(cfg.dbName).getCollection("EInvoiceMTT");
		try {
			docTmp = collection.aggregate(pipeline).allowDiskUse(true).iterator().next();
		} catch (Exception e) {

		}

		mongoClient.close();

		if (null == docTmp) {
			responseStatus = new MspResponseStatus(9999, "Không tìm thấy thông tin hóa đơn.");
			rsp.setResponseStatus(responseStatus);
			return rsp;
		}

		String signStatusCode = docTmp.get("SignStatusCode", "");
		if ("PROCESSING".equals(signStatusCode)) {
			responseStatus = new MspResponseStatus(9999, "Hóa đơn đã ký.");
			rsp.setResponseStatus(responseStatus);
			return rsp;
		}

		/* KIEM TRA NGAY LAP TRONG HE THONG - NGAY LAP TRONG XML */
		LocalDate ldNLapSystem = commons.convertDateToLocalDate(
				docTmp.getEmbedded(Arrays.asList("EInvoiceDetail", "TTChung", "NLap"), Date.class));
		if (commons.compareLocalDate(ldNLap, ldNLapSystem) != 0) {
			responseStatus = new MspResponseStatus(9999, "Ngày lập trong hệ thống và trong dữ liệu khác nhau.");
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
			objectId = new ObjectId(docTmp.getEmbedded(Arrays.asList("EInvoiceDetail", "TTChung", "MauSoHD"), ""));
		} catch (Exception e) {
		}
		if (null == objectId) {
			throw new Exception("Không tìm thấy mẫu số hóa đơn.");
		}

		mongoClient = cfg.mongoClient();
		collection = mongoClient.getDatabase(cfg.dbName).getCollection("EInvoiceMTT");
		collection.findOneAndUpdate(docFind,
				new Document("$set", new Document("SignStatusCode", "SIGNED").append("InfoSigned",
						new Document("SignedDate", LocalDateTime.now()).append("SignedUserID", header.getUserId())
								.append("SignedUserName", header.getUserName())
								.append("SignedUserFullName", header.getUserFullName()))),
				options);
		mongoClient.close();

		String name_company = removeAccent(header.getUserFullName());
		DateTimeFormatter format_time = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
		LocalDateTime time_dem = LocalDateTime.now();
		String time = time_dem.format(format_time);
		System.out.println(time + name_company + " Vua ky hoa don tu may tinh tien.");

		responseStatus = new MspResponseStatus(0, Constants.MAP_ERROR.get(0));
		rsp.setResponseStatus(responseStatus);
		return rsp;
	}

	/* REFRESH CQT */

	@Override
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
				.append("IsDelete", new Document("$ne", true))/* .append("SignStatusCode", "SIGNED") */
				.append("EInvoiceStatus", new Document("$in", Arrays.asList("ERROR_CQT", "PROCESSING")));

		Document docTmp = null;

		MongoClient mongoClient = cfg.mongoClient();
		MongoCollection<Document> collection = mongoClient.getDatabase(cfg.dbName).getCollection("EInvoiceMTT");
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
		String MaKetQua = "";
		String MoTaKetQua = "";

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
		Node nodeKetQuaTraCuu = null;
		nodeKetQuaTraCuu = (Node) xPath.evaluate("/KetQuaTraCuu", rTCTN, XPathConstants.NODE);
		MaKetQua = commons
				.getTextFromNodeXML((Element) xPath.evaluate("MaKetQua", nodeKetQuaTraCuu, XPathConstants.NODE));
		MoTaKetQua = commons
				.getTextFromNodeXML((Element) xPath.evaluate("MoTaKetQua", nodeKetQuaTraCuu, XPathConstants.NODE));
		org.w3c.dom.Document rTCTN1 = null;
		org.w3c.dom.Document doc = null;
		String MST = "";
		File file = null;
		String dir1 = docTmp.get("Dir", "");
		String signStatusCode = docTmp.get("SignStatusCode", "NOSIGN");
		String fileName1 = "";
		if (signStatusCode.equals("SIGNED")) {
			fileName1 = _id + "_signed.xml";
		} else {
			fileName1 = _id + "_pending.xml";
		}

		file = new File(dir1, fileName1);
		MST = docTmp.getEmbedded(Arrays.asList("EInvoiceDetail", "NDHDon", "NBan", "MST"), "");

		if ("2".equals(MaKetQua)) {

			// call 3 lần mỗi lần 3s
			if (rTCTN == null) {
				rTCTN = tctnService.callTraCuuThongDiep(MTDiep);
				TimeUnit.SECONDS.sleep(3);

			}
			if (rTCTN == null) {
				rTCTN = tctnService.callTraCuuThongDiep(MTDiep);
				TimeUnit.SECONDS.sleep(3);

			}
			if (rTCTN == null) {
				rTCTN = tctnService.callTraCuuThongDiep(MTDiep);
				// Độ trễ
				TimeUnit.SECONDS.sleep(3);

			}
			nodeKetQuaTraCuu = (Node) xPath.evaluate("/KetQuaTraCuu", rTCTN, XPathConstants.NODE);
			MaKetQua = commons
					.getTextFromNodeXML((Element) xPath.evaluate("MaKetQua", nodeKetQuaTraCuu, XPathConstants.NODE));
			MoTaKetQua = commons
					.getTextFromNodeXML((Element) xPath.evaluate("MoTaKetQua", nodeKetQuaTraCuu, XPathConstants.NODE));

			if ("2".equals(MaKetQua) && "Mã giao dịch không đúng".equals(MoTaKetQua)) {
				doc = commons.fileToDocument(file, true);

				try {
					rTCTN1 = tctnService.callTiepNhanThongDiepMTT(doc);
				} catch (Exception e) {
				}
				xPath = XPathFactory.newInstance().newXPath();
				Node nodeDLHDon = (Node) xPath.evaluate("/TDiep", rTCTN1, XPathConstants.NODE);
				MaKetQua = commons
						.getTextFromNodeXML((Element) xPath.evaluate("MaKetQua", nodeDLHDon, XPathConstants.NODE));
				MoTaKetQua = commons
						.getTextFromNodeXML((Element) xPath.evaluate("MoTaKetQua", nodeDLHDon, XPathConstants.NODE));
			}

			if ("".equals(MaKetQua)) {
				rTCTN = tctnService.callTraCuuThongDiep(MTDiep);
				nodeKetQuaTraCuu = (Node) xPath.evaluate("/KetQuaTraCuu", rTCTN, XPathConstants.NODE);
				MaKetQua = commons.getTextFromNodeXML(
						(Element) xPath.evaluate("MaKetQua", nodeKetQuaTraCuu, XPathConstants.NODE));
				MoTaKetQua = commons.getTextFromNodeXML(
						(Element) xPath.evaluate("MoTaKetQua", nodeKetQuaTraCuu, XPathConstants.NODE));
			} else {
				responseStatus = new MspResponseStatus(9999, "Mã giao dịch không đúng.");
				rsp.setResponseStatus(responseStatus);
				return rsp;
			}
		}

		Node nodeTDiep = null;
		String checkMLTDiep = "";
		String MLoi = "";
		String MTLoi = "";
		String MTDTChieu = "";
		String CQT_MLTDiep = "";

		String CQT_MLTDiep1 = "";
		String LTBao = "";

		for (int i = 1; i <= 20; i++) {

			if (xPath.evaluate("DuLieu/TDiep[" + i + "]", nodeKetQuaTraCuu, XPathConstants.NODE) == null)
				break;
			nodeTDiep = (Node) xPath.evaluate("DuLieu/TDiep[" + i + "]", nodeKetQuaTraCuu, XPathConstants.NODE);
			checkMLTDiep = commons
					.getTextFromNodeXML((Element) xPath.evaluate("TTChung/MLTDiep", nodeTDiep, XPathConstants.NODE));

			if (checkMLTDiep.equals("204")) {
				LTBao = commons.getTextFromNodeXML(
						(Element) xPath.evaluate("DLieu/TBao/DLTBao/LTBao", nodeTDiep, XPathConstants.NODE));
				break;
			}
		}

		if (nodeTDiep == null) {
			responseStatus = new MspResponseStatus(9999, "Chưa có kết quả trả về.");
			rsp.setResponseStatus(responseStatus);
			return rsp;
		}

		CQT_MLTDiep = commons
				.getTextFromNodeXML((Element) xPath.evaluate("TTChung/MLTDiep", nodeTDiep, XPathConstants.NODE));
		MTDTChieu = commons
				.getTextFromNodeXML((Element) xPath.evaluate("TTChung/MTDTChieu", nodeTDiep, XPathConstants.NODE));

		if (CQT_MLTDiep.equals("-1")) {

			MLoi = commons.getTextFromNodeXML((Element) xPath.evaluate("DLieu/MLoi", nodeTDiep, XPathConstants.NODE));
			MTLoi = commons.getTextFromNodeXML((Element) xPath.evaluate("DLieu/MTa", nodeTDiep, XPathConstants.NODE));
			/* CAP NHAT - TRANG THAI CQT */
			options = new FindOneAndUpdateOptions();
			options.upsert(false);
			options.maxTime(5000, TimeUnit.MILLISECONDS);
			options.returnDocument(ReturnDocument.AFTER);

			mongoClient = cfg.mongoClient();
			collection = mongoClient.getDatabase(cfg.dbName).getCollection("EInvoiceMTT");
			collection.findOneAndUpdate(docFind,
					new Document("$set",
							new Document("EInvoiceStatus", Constants.INVOICE_STATUS.ERROR_CQT)
									.append("CQT_Date", LocalDate.now())
									.append("LDo", new Document("MLoi", MLoi).append("MTLoi", MTLoi))),
					options);
			mongoClient.close();

			responseStatus = new MspResponseStatus(0,
					"".equals(MTLoi) ? "CQT chưa có thông báo kết quả trả về." : MTLoi);
			rsp.setResponseStatus(responseStatus);
			return rsp;
		}

		if (!LTBao.equals("2")) {
			MLoi = commons.getTextFromNodeXML((Element) xPath.evaluate("DLieu/TBao/DLTBao/LHDMTTien/DSLDo/LDo/MLoi",
					nodeTDiep, XPathConstants.NODE));
			MTLoi = commons.getTextFromNodeXML((Element) xPath.evaluate("DLieu/TBao/DLTBao/LHDMTTien/DSLDo/LDo/MTLoi",
					nodeTDiep, XPathConstants.NODE));
			/* LUU LAI FILE XML LOI */
			String dir = docTmp.get("Dir", "");
			String fileName = _id + "_" + CQT_MLTDiep1 + ".xml";
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
			options = new FindOneAndUpdateOptions();
			options.upsert(false);
			options.maxTime(5000, TimeUnit.MILLISECONDS);
			options.returnDocument(ReturnDocument.AFTER);

			MongoClient mongoClient1 = cfg.mongoClient();
			collection = mongoClient1.getDatabase(cfg.dbName).getCollection("EInvoiceMTT");
			collection.findOneAndUpdate(docFind,
					new Document("$set",
							new Document("EInvoiceStatus", Constants.INVOICE_STATUS.ERROR_CQT)
									.append("CQT_Date", LocalDate.now())
									.append("LDo", new Document("MLoi", MLoi).append("MTLoi", MTLoi))),
					options);
			mongoClient1.close();

			responseStatus = new MspResponseStatus(0,
					"".equals(MTLoi) ? "CQT chưa có thông báo kết quả trả về." : MTLoi);
			rsp.setResponseStatus(responseStatus);
			return rsp;

		}

		if ("|204|".indexOf("|" + CQT_MLTDiep + "|") == -1) {
			responseStatus = new MspResponseStatus(9999, "CQT chưa có thông báo kết quả trả về.");
			rsp.setResponseStatus(responseStatus);
			return rsp;
		}

		String MCCQT = docTmp.get("MCCQT", "").replaceAll("-", "_");

		String dir = docTmp.get("Dir", "");
		String fileName = _id + "_" + MCCQT + ".xml";
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
		options = new FindOneAndUpdateOptions();
		options.upsert(false);
		options.maxTime(5000, TimeUnit.MILLISECONDS);
		options.returnDocument(ReturnDocument.AFTER);

		MongoClient mongoClient1 = cfg.mongoClient();
		collection = mongoClient1.getDatabase(cfg.dbName).getCollection("EInvoiceMTT");
		collection.findOneAndUpdate(docFind,
				new Document("$set",
						new Document("EInvoiceStatus", Constants.INVOICE_STATUS.COMPLETE).append("MTDTChieu", MTDTChieu)
								.append("CQT_Date", LocalDate.now())
								.append("LDo", new Document("MLoi", "").append("MTLoi", ""))),
				options);
		mongoClient1.close();

		String iddc = "";
		try {
			iddc = docTmp.getEmbedded(Arrays.asList("EInvoiceDetail", "TTChung", "TTHDLQuan", "_id"), "");
		} catch (Exception e) {
			iddc = docTmp.getEmbedded(Arrays.asList("EInvoiceDetail", "TTChung", "TTHDLQuan", "_id"), ObjectId.class)
					.toString();
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

				MongoClient mongoClient3 = cfg.mongoClient();
				collection = mongoClient3.getDatabase(cfg.dbName).getCollection("EInvoiceMTT");
				collection.findOneAndUpdate(docFind1, new Document("$set", new Document("EInvoiceStatus", "REPLACED")),
						options);
				mongoClient3.close();

			} else if ("2".equals(
					docTmp.getEmbedded(Arrays.asList("EInvoiceDetail", "TTChung", "TTHDLQuan", "TCHDon"), ""))) {

				MongoClient mongoClient2 = cfg.mongoClient();
				collection = mongoClient2.getDatabase(cfg.dbName).getCollection("EInvoiceMTT");
				collection.findOneAndUpdate(docFind1, new Document("$set", new Document("EInvoiceStatus", "ADJUSTED")),
						options);
				mongoClient2.close();

			}
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
				.append("EInvoiceStatus", new Document("$in", Arrays.asList("ERROR_CQT", "PROCESSING", "COMPLETE")))
		/* .append("SignStatusCode", Constants.INVOICE_SIGN_STATUS.SIGNED) */;

		Document docTmp = null;

		MongoClient mongoClient = cfg.mongoClient();
		MongoCollection<Document> collection = mongoClient.getDatabase(cfg.dbName).getCollection("EInvoiceMTT");
		try {
			docTmp = collection.find(docFind).allowDiskUse(true).iterator().next();
		} catch (Exception e) {

		}

		mongoClient.close();

		if (null == docTmp) {
			responseStatus = new MspResponseStatus(9999, "Không tìm thấy thông tin thông báo.");
			rsp.setResponseStatus(responseStatus);
			return rsp;
		}

		String MTDiep = docTmp.get("MTDiep", "");
		org.w3c.dom.Document rTCTN = tctnService.callTraCuuThongDiep(MTDiep);
//			rTCTN = commons.stringToDocument("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?><KetQuaTraCuu><MaGD>D8DB05DDFFD8425AB665558FEF499EC7</MaGD><MaKetQua>0</MaKetQua><MoTaKetQua>CQT đã trả kết quả xử lý thông điệp</MoTaKetQua><DuLieu><TDiep><TTChung><PBan>2.0.0</PBan><MNGui>TCT</MNGui><MNNhan>V0401486901</MNNhan><MLTDiep>999</MLTDiep><MTDiep>TCT97B381D4211B4DC8ADE43A27BA72048A</MTDiep><MTDTChieu>V0401486901D8DB05DDFFD8425AB665558FEF499EC7</MTDTChieu></TTChung><DLieu><TBao><MTDiep>V0401486901D8DB05DDFFD8425AB665558FEF499EC7</MTDiep><MNGui>V0401486901</MNGui><NNhan>2022-03-22T11:50:45</NNhan><TTTNhan>0</TTTNhan></TBao></DLieu></TDiep><TDiep><TTChung><PBan>2.0.0</PBan><MNGui>TCT</MNGui><MNNhan>V0401486901</MNNhan><MLTDiep>204</MLTDiep><MTDiep>TCT193127A3E1A64029803F59BB157A4EF6</MTDiep><MTDTChieu>V0401486901D8DB05DDFFD8425AB665558FEF499EC7</MTDTChieu><MST>0301521415</MST><SLuong>1</SLuong></TTChung><DLieu><TBao><DLTBao Id=\"Id-c17ac0768edb437b9d76c965c30b3b16\"><MSo>01/TB-KTDL</MSo><Ten>Thông báo về việc kết quả kiểm tra dữ liệu hóa đơn điện tử</Ten><So>220003861409</So><DDanh>Hà Nội</DDanh><NTBao>2022-03-22</NTBao><MST>0301521415</MST><TNNT>CÔNG TY TNHH KIM LOẠI HẠNH PHÚC</TNNT><TGGui>2022-03-22T11:50:46</TGGui><LTBao>2</LTBao><CCu>Thông điệp thông báo hủy/giải trình HDDT có mã/không mã đã lập có sai sót</CCu><SLuong>1</SLuong></DLTBao><DSCKS><CQT><Signature xmlns=\"http://www.w3.org/2000/09/xmldsig#\" Id=\"Tct-71b09cd7304f4ab8ab0a74b7a164f88c\"><SignedInfo><CanonicalizationMethod Algorithm=\"http://www.w3.org/TR/2001/REC-xml-c14n-20010315\" /><SignatureMethod Algorithm=\"http://www.w3.org/2001/04/xmldsig-more#rsa-sha256\" /><Reference URI=\"#Id-c17ac0768edb437b9d76c965c30b3b16\"><DigestMethod Algorithm=\"http://www.w3.org/2001/04/xmlenc#sha256\" /><DigestValue>hpzYcBVfhJipwQN4Z++5Ya/e+0BBumAArubh0dCrSxg=</DigestValue></Reference><Reference URI=\"#SigningTime-a345d862b9c94599bfdc5c5b2bea2be1\"><DigestMethod Algorithm=\"http://www.w3.org/2001/04/xmlenc#sha256\" /><DigestValue>vVGGtDB8sQhnyxL+mnuKMA94/KqiEmVRnX/e4hE0qMI=</DigestValue></Reference></SignedInfo><SignatureValue>domvFsaa5TZdLcm9P881/2lzgviI0Pc+ZXn9kanO6CKmTCR7gwxc00XLLMw0nHG/5jJMkeW+fipNzPIzl4/YbH+XPsNc+E7OK6uJYufPLZ2NfzHKtu1otivsLbHvGVL7+feHA7tAn6eVvjCiv7YVfvyX9cE+WI0s6qVyJHDBZ7Mg3HM19H3y5VpNF4DGFD/kdLQpUXdW78UO/8ulV3yGdGV9EYckqFlptdFrbRSzlxrW5JSyk9rZ7ginJWSp5n65l6UQvu2bYKU331rcRptcZBXBu9h/1Dz7o66nU+IQn8rlkr4FFOM+NJQSxds7lIuEkYuD83q+X1k5/cu9QmRG7A==</SignatureValue><KeyInfo><X509Data><X509SubjectName>CN=TỔNG CỤC THUẾ,O=BỘ TÀI CHÍNH,L=Hà Nội,C=VN</X509SubjectName><X509Certificate>MIIF3jCCA8agAwIBAgIIdVgJVfyB1qcwDQYJKoZIhvcNAQELBQAwaTELMAkGA1UEBhMCVk4xIzAhBgNVBAoMGkJhbiBDxqEgeeG6v3UgQ2jDrW5oIHBo4bunMTUwMwYDVQQDDCxDQSBwaOG7pWMgduG7pSBjw6FjIGPGoSBxdWFuIE5ow6Agbsaw4bubYyBHMjAeFw0yMTExMTcwODA5MDBaFw0yNjExMTYwODA5MDBaMFoxCzAJBgNVBAYTAlZOMRIwEAYDVQQHDAlIw6AgTuG7mWkxGTAXBgNVBAoMEELhu5ggVMOASSBDSMONTkgxHDAaBgNVBAMME1Thu5RORyBD4bukQyBUSFXhur4wggEiMA0GCSqGSIb3DQEBAQUAA4IBDwAwggEKAoIBAQC7nHwdxstlexFCsMFhLZMy6TZR1Epb0NfhhDTcjbG/a7fbGrvjxZHPXHtSNnnkpT3Pnl2SR870TFSCojqw6grK1NPsxzBA6RW5VMdmTjq7t/cGOz1TdXr9o+b1bXApmB0o5j1W+0IJS6ZC2BEGonfPbnKk6PyFvaPLgvutqT2GOzdtJ1kGXDN84bANDEJc6ltKSW7HIiDgm01BqkdxEJCQ12EW9HbMjioWllkyRxEIWXURV0+MgIFYn1vRFB129q+SIu8f98w1gvA258HVQ/rBCEaKe8a7MMt9/qUlh1HI+qLq4SWRZ7BMmUQ4GK4S/Ia6JqTK9hU+pxY9CfGb+OIrAgMBAAGjggGXMIIBkzAMBgNVHRMBAf8EAjAAMB8GA1UdIwQYMBaAFFC+z6C96g+fTpEooyrrfO7wL8i6MGYGCCsGAQUFBwEBBFowWDAzBggrBgEFBQcwAoYnaHR0cDovL2NhLmdvdi52bi9wa2kvcHViL2NydC9jcGNhZzIuY3J0MCEGCCsGAQUFBzABhhVodHRwOi8vb2NzcC5jYS5nb3Yudm4wGgYDVR0RBBMwEYEPY250dEBnZHQuZ292LnZuMEoGA1UdIARDMEEwPwYIYIVAAQEBAQEwMzAxBggrBgEFBQcCARYlaHR0cHM6Ly9jYS5nb3Yudm4vcGtpL3B1Yi9wb2xpY2llcy9DUDApBgNVHSUEIjAgBggrBgEFBQcDAgYIKwYBBQUHAwQGCisGAQQBgjcUAgIwOAYDVR0fBDEwLzAtoCugKYYnaHR0cDovL2NhLmdvdi52bi9wa2kvcHViL2NybC9jcGNhZzIuY3JsMB0GA1UdDgQWBBQniiN6yqEnrwg8ldrPZoxUEI409DAOBgNVHQ8BAf8EBAMCBPAwDQYJKoZIhvcNAQELBQADggIBAF6Kki2pZb2cTPkHHPMi6FudEc9vTRrDZy4sh5xleTZM9SaV0OYz1CcgSQXforxajSf7+cyhRmiK6z4mdqv3w5mO+ESPDGFn/24L70iRp7LQtzoDWdN9reObuxfhDBLozBlTEJeka4ZCAo3CMn1Ldj5EtOrrUNvkt1waPk8eRrqjosRXzyw9/p5D5DaNr1cjrVe9mVoM7IHlFQ+7OtaRnEesjInqk25Xoj+kxRLFquZt9ZjcAWIdsgrkM/0OcsCtjtxvZtxUWufwM862BBNuDnHb9sDkSmF5LQ4FloFRQIeNJddOfgmgaY8UX37kmQAqv7KdmW2eClG93mPv3RsQhZPnTgQVmDgXr61OnAPdqTEpuJY1yXiei4Lwcultl9IuyeQjXDx8xE/oaQ2ubC8YXBNDRupggyfAoloUFXf/21uZQ8Rpm/P5TS1eOiDcA/ZVu9NBNyopwbrRpIHlgbf+IzQTun6ShkNZcbxlYfzDPBRa6jEkpDgjh/rXQqnYwBGzuY1UDNitHrX7gIPCnhmz6lL6x0kc6+V2+Bu+8IWw1Y+pRVMiNlVx39KtDKpssabt/HRoI92UHd5GyEGTSbzkztUFDieCs/KPtFZEsg30RnkHirk/k5nHRyAgpNtOdWWGFmh3QAaj0cZ9MZKJwLMzG2/QCOSaaP//1CGE2fM8hcgK</X509Certificate></X509Data></KeyInfo><Object Id=\"SigningTime-a345d862b9c94599bfdc5c5b2bea2be1\"><SignatureProperties><SignatureProperty Target=\"signatureProperties\"><SigningTime>2022-03-22T11:50:46</SigningTime></SignatureProperty></SignatureProperties></Object></Signature></CQT></DSCKS></TBao></DLieu></TDiep><TDiep><TTChung><PBan>2.0.0</PBan><MNGui>TCT</MNGui><MNNhan>V0401486901</MNNhan><MLTDiep>301</MLTDiep><MTDiep>TCTA2AAFCCCBD8B4292A7FEA4A1E66E595B</MTDiep><MTDTChieu>V0401486901D8DB05DDFFD8425AB665558FEF499EC7</MTDTChieu><MST>0301521415</MST><SLuong>1</SLuong></TTChung><DLieu><TBao><DLTBao Id=\"Id-2da3b60292a14702aec6cb5b276e7768\"><PBan>2.0.0</PBan><MSo>01/TB-SSĐT</MSo><Ten>Về việc tiếp nhận và kết quả xử lý về việc hóa đơn điện tử đã lập có sai sót</Ten><TCQTCTren>Cục Thuế Thành phố Hồ Chí Minh</TCQTCTren><TCQT>Chi cục Thuế Quận Gò Vấp</TCQT><TNNT>CÔNG TY TNHH KIM LOẠI HẠNH PHÚC</TNNT><MST>0316685293</MST><MGDDTu>V0401486901D8DB05DDFFD8425AB665558FEF499EC7</MGDDTu><TGNhan>2022-03-22</TGNhan><STTThe>1</STTThe><HThuc>KT.Chi cục Trưởng</HThuc><CDanh>Phó Chi cục Trưởng</CDanh></DLTBao><STBao Id=\"Id-6813e99ffeb542b1ae9c2d8bbfab068f\"><So>13657/TB-CCTGV-HDDT</So><NTBao>2022-03-25</NTBao></STBao><DSCKS><TTCQT><Signature xmlns=\"http://www.w3.org/2000/09/xmldsig#\" Id=\"TTCQT-Id-2da3b60292a14702aec6cb5b276e7768\"><SignedInfo><CanonicalizationMethod Algorithm=\"http://www.w3.org/TR/2001/REC-xml-c14n-20010315\" /><SignatureMethod Algorithm=\"http://www.w3.org/2001/04/xmldsig-more#rsa-sha256\" /><Reference URI=\"#Object-TTCQT-Id-2da3b60292a14702aec6cb5b276e7768\"><DigestMethod Algorithm=\"http://www.w3.org/2001/04/xmlenc#sha256\" /><DigestValue>N0oGHJnkn80vY+I0R6ilvQbgTHWAav1GK+v20807TyE=</DigestValue></Reference><Reference URI=\"#Id-2da3b60292a14702aec6cb5b276e7768\"><DigestMethod Algorithm=\"http://www.w3.org/2001/04/xmlenc#sha256\" /><DigestValue>fda5zcz1NSe/4c8Izv/xn9fU4hsQuVTRal+cNlsd0Zs=</DigestValue></Reference></SignedInfo><SignatureValue>UmpUOJ++XnOED7BtZqg0G6h6KpyVyQzHpnsMxntxmawwDPw5a3C5ydcV+wEj8TdEYd15t8fS1p8DNvQZfoqrziEPpbNlEXN+LuWuU0KDl5U1YjzILSOsVIanMJihAByjUzH/awXh1ycUeOMIF1jb8Rypl6mrUN+P/UkwwLM0BjOQWxZ3MuYJPDe7PBUSZqFrSoDFE6l3JOznVNu+M0eTqf2Rb2ySgg+SpSyR1l5PpSrOS1/icgRDStU8W0MWB6oV4+GpPLGfQJdNndm/yYUAyLgm+QpywzUNLEAjhbf1J5TfH+RjBaYufOJNGZcgoTw02rCJNRWZMZiy9fBWvzLGow==</SignatureValue><KeyInfo><X509Data><X509SubjectName>CN=Phó Chi cục trưởng Nguyễn Mạnh Trung, L=Hồ Chí Minh, OU=Cục Thuế thành phố Hồ Chí Minh, OU=Tổng cục Thuế, O=Bộ Tài chính, C=VN</X509SubjectName><X509Certificate>MIIGDDCCBPSgAwIBAgIDa1TcMA0GCSqGSIb3DQEBCwUAMFkxCzAJBgNVBAYTAlZOMR0wGwYDVQQKDBRCYW4gQ28geWV1IENoaW5oIHBodTErMCkGA1UEAwwiQ28gcXVhbiBjaHVuZyB0aHVjIHNvIEJvIFRhaSBjaGluaDAeFw0xOTA0MDUwNzU2MTZaFw0yNjA1MTgwNzU2MTZaMIHLMQswCQYDVQQGEwJWTjEZMBcGA1UECgwQQuG7mSBUw6BpIGNow61uaDEcMBoGA1UECwwTVOG7lW5nIGPhu6VjIFRodeG6vzExMC8GA1UECwwoQ+G7pWMgVGh14bq/IHRow6BuaCBwaOG7kSBI4buTIENow60gTWluaDEXMBUGA1UEBwwOSOG7kyBDaMOtIE1pbmgxNzA1BgNVBAMMLlBow7MgQ2hpIGPhu6VjIHRyxrDhu59uZyBOZ3V54buFbiBN4bqhbmggVHJ1bmcwggEiMA0GCSqGSIb3DQEBAQUAA4IBDwAwggEKAoIBAQCya64WBOSa9oXRXEOVNf2VB8w5GCCTtAxcBgT8dORYgOQ9AfEZ2GRuImnRr3Wu0FGqhVlKE5b5h317+JAzje0wYqp1n4UnrQmQYrwTH2dOklQmDHhFYD7k+4cS6XHzqpLKMBRzZi6nG6PbFFFvztVAve3PCQ3F7DECNKoh5PDmaiRqaONskUfLkllo0erfYHTZ1BPvpDfBgcnIhJVUvdO1Rjh2gjjtjCTg4hLaFWy8JJl7M6z3IMOIMYi2xfT1urBVwqmURysmLwokT266Hk+9p1wZhSmQpv2n3QPqeH8Qu9bgVJhKBkF9N90A+pWS+ZZU2sVE5s6nuJszCjhjDEEHAgMBAAGjggJoMIICZDAJBgNVHRMEAjAAMBEGCWCGSAGG+EIBAQQEAwIFoDALBgNVHQ8EBAMCBPAwKQYDVR0lBCIwIAYIKwYBBQUHAwIGCCsGAQUFBwMEBgorBgEEAYI3FAICMB8GCWCGSAGG+EIBDQQSFhBVc2VyIFNpZ24gb2YgQlRDMB0GA1UdDgQWBBQeHp3ua90KvqdhF+NZlE++It9lezCBlQYDVR0jBIGNMIGKgBSeOJrWKZWJagV/Kv9fAZe0VzBmsqFvpG0wazELMAkGA1UEBhMCVk4xHTAbBgNVBAoMFEJhbiBDbyB5ZXUgQ2hpbmggcGh1MT0wOwYDVQQDDDRDbyBxdWFuIGNodW5nIHRodWMgc28gY2h1eWVuIGR1bmcgQ2hpbmggcGh1IChSb290Q0EpggEDMCEGA1UdEQQaMBiBFm5tdHJ1bmcuaGNtQGdkdC5nb3Yudm4wCQYDVR0SBAIwADBlBggrBgEFBQcBAQRZMFcwIgYIKwYBBQUHMAGGFmh0dHA6Ly9vY3NwLmNhLmdvdi52bi8wMQYIKwYBBQUHMAKGJWh0dHA6Ly9jYS5nb3Yudm4vcGtpL3B1Yi9jZXJ0L2J0Yy5jcnQwMwYJYIZIAYb4QgEEBCYWJGh0dHA6Ly9jYS5nb3Yudm4vcGtpL3B1Yi9jcmwvYnRjLmNybDAzBglghkgBhvhCAQMEJhYkaHR0cDovL2NhLmdvdi52bi9wa2kvcHViL2NybC9idGMuY3JsMDUGA1UdHwQuMCwwKqAooCaGJGh0dHA6Ly9jYS5nb3Yudm4vcGtpL3B1Yi9jcmwvYnRjLmNybDANBgkqhkiG9w0BAQsFAAOCAQEAD/PW3vbqzBTBdkV5e5BkQHA+v+ZYOxuGgVXyrahI4Q95z2vMWk867qjnqllkw9iM3HqwIgcxdCuMYgTABQXfqCri15dIYAYTalV3DFI4au0qEtLMEkWN9wkB/JQMITyHksvKDaR8JefCi+SQrIAmdoYp208Q0MRzwE167A/p8r7ifTdh7IUazwch1hH8sjf76fdFg+joO9wccd1DHW62OKhhGJvMwHvbWWsR3CYcH0A8CZOQ6WE/6IANrlld7mhhLCEQ4z9pZHIjkGL0AUQKQWJ6nA6sg8hruokKluI6KzSS9sNw1fsRiD9VCYhu36OSQcZatj+z8ox/k6NmTI6KAA==</X509Certificate></X509Data></KeyInfo><Object Id=\"Object-TTCQT-Id-2da3b60292a14702aec6cb5b276e7768\"><SignatureProperties xmlns=\"\"><SignatureProperty Id=\"SignatureProperty-TTCQT-Id-2da3b60292a14702aec6cb5b276e7768\" Target=\"#TTCQT-Id-2da3b60292a14702aec6cb5b276e7768\"><SigningTime>2022-03-24T17:16:58</SigningTime></SignatureProperty></SignatureProperties></Object></Signature></TTCQT><CQT><Signature Id=\"CQT-Id-2da3b60292a14702aec6cb5b276e7768\" xmlns=\"http://www.w3.org/2000/09/xmldsig#\"><SignedInfo><CanonicalizationMethod Algorithm=\"http://www.w3.org/TR/2001/REC-xml-c14n-20010315\" /><SignatureMethod Algorithm=\"http://www.w3.org/2000/09/xmldsig#rsa-sha1\" /><Reference URI=\"#Object-CQT-Id-2da3b60292a14702aec6cb5b276e7768\"><DigestMethod Algorithm=\"http://www.w3.org/2000/09/xmldsig#sha1\" /><DigestValue>n2s5aNIkte2q5duZ7jTN4Vf19jY=</DigestValue></Reference><Reference URI=\"#Id-2da3b60292a14702aec6cb5b276e7768\"><DigestMethod Algorithm=\"http://www.w3.org/2000/09/xmldsig#sha1\" /><DigestValue>TEjemE/WjuxrSjYy4yYxVlP3lDw=</DigestValue></Reference><Reference URI=\"#Id-6813e99ffeb542b1ae9c2d8bbfab068f\"><DigestMethod Algorithm=\"http://www.w3.org/2000/09/xmldsig#sha1\" /><DigestValue>0CFae5LbGa1P+yjMVi8zH6j0NHs=</DigestValue></Reference></SignedInfo><SignatureValue>mMA4tHjS1AThpRFELoHY8qCEHZDLD277kB21BvcN0TaNllNjcwzcXR+xRbABFmkZ7t82eAdqYS6XibhFk4Yov0PW2rD+LYOzPbf1g+C6CJIPPZfVkCdADY6i7QAJxDQ027n03jNSvnNFTbHyavVxXxilZ0AtZhezSP8JNYIdDNWxcnnqwIIum6XldGPnEdbwI/oHtFP+1QgnZ03JabpXo3sH87o7AXi9PmTezFIWQELqdOZGI51Oy5dgArO75Rl3ek8AoDgRoycVYlpPUT7xf9DXE/gy0yotb8DJvrnooyUpmNYzv7MmSY5SxZU2VOnY6fLcMRWAM0gryyug59D4uw==</SignatureValue><KeyInfo><X509Data><X509SubjectName>CN=Chi cục Thuế Gò Vấp, L=Hồ Chí Minh, OU=Cục Thuế thành phố Hồ Chí Minh, OU=Tổng cục Thuế, O=Bộ Tài chính, C=VN</X509SubjectName><X509Certificate>MIIGFTCCBP2gAwIBAgIDay7xMA0GCSqGSIb3DQEBBQUAMFkxCzAJBgNVBAYTAlZOMR0wGwYDVQQKDBRCYW4gQ28geWV1IENoaW5oIHBodTErMCkGA1UEAwwiQ28gcXVhbiBjaHVuZyB0aHVjIHNvIEJvIFRhaSBjaGluaDAeFw0xNjA1MjgwMzA4MzBaFw0yNjA1MjYwMzA4MzBaMIG3MQswCQYDVQQGEwJWTjEZMBcGA1UECgwQQuG7mSBUw6BpIGNow61uaDEcMBoGA1UECwwTVOG7lW5nIGPhu6VjIFRodeG6vzExMC8GA1UECwwoQ+G7pWMgVGh14bq/IHRow6BuaCBwaOG7kSBI4buTIENow60gTWluaDEXMBUGA1UEBwwOSOG7kyBDaMOtIE1pbmgxIzAhBgNVBAMMGkNoaSBj4bulYyBUaHXhur8gR8OyIFbhuqVwMIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAsoQCzxWagmcf9VVMw4no0Y/gdJ6bOqapbryoCR0+cgV7urXeFVw7j8/nIfS/S3GKJonFdt8/AXy8yTczjBPyfFaVhsTBO80v5w86Rk+uh+lrApW18yK1yBi2HnhNvD5iNbYiT5Z7lN9kmROmWvCrkospU+KBZZ/QL5P4TPFAZVnsnpnvSy/KXPrroARs3e/uCNZgccKBoKNIlxNuY6FumfXkj0RJqgLF04oDY+cr4K1naX2eho2qOYo1FUEpEuOBM1om25DnI5TehoBPa8/ieRuSxP3B2oyp6oCRewydSFYXTfW0AJE4dhkRerLJbz8H0J8cZYRfnBarRizHYqqEHwIDAQABo4IChTCCAoEwCQYDVR0TBAIwADARBglghkgBhvhCAQEEBAMCBaAwCwYDVR0PBAQDAgTwMCkGA1UdJQQiMCAGCCsGAQUFBwMCBggrBgEFBQcDBAYKKwYBBAGCNxQCAjAfBglghkgBhvhCAQ0EEhYQVXNlciBTaWduIG9mIEJUQzAdBgNVHQ4EFgQURDVlb3eibfzyP+9fjDiSZRJjFGcwgZUGA1UdIwSBjTCBioAUnjia1imViWoFfyr/XwGXtFcwZrKhb6RtMGsxCzAJBgNVBAYTAlZOMR0wGwYDVQQKDBRCYW4gQ28geWV1IENoaW5oIHBodTE9MDsGA1UEAww0Q28gcXVhbiBjaHVuZyB0aHVjIHNvIGNodXllbiBkdW5nIENoaW5oIHBodSAoUm9vdENBKYIBAzAhBgNVHREEGjAYgRZIY19ndmFwLmhjbUBnZHQuZ292LnZuMAkGA1UdEgQCMAAwXwYIKwYBBQUHAQEEUzBRMB8GCCsGAQUFBzABhhNodHRwOi8vb2NzcC5jYS5idGMvMC4GCCsGAQUFBzAChiJodHRwOi8vY2EuYnRjL3BraS9wdWIvY2VydC9idGMuY3J0MDAGCWCGSAGG+EIBBAQjFiFodHRwOi8vY2EuYnRjL3BraS9wdWIvY3JsL2J0Yy5jcmwwMAYJYIZIAYb4QgEDBCMWIWh0dHA6Ly9jYS5idGMvcGtpL3B1Yi9jcmwvYnRjLmNybDBeBgNVHR8EVzBVMCegJaAjhiFodHRwOi8vY2EuYnRjL3BraS9wdWIvY3JsL2J0Yy5jcmwwKqAooCaGJGh0dHA6Ly9jYS5nb3Yudm4vcGtpL3B1Yi9jcmwvYnRjLmNybDANBgkqhkiG9w0BAQUFAAOCAQEAkE65aC22UBHZOmwY4ejCN3Tv3idRBn/0bgeaFFAu+rECCTu+hM99bOerFAxGxXJQ4+CxIWe1APTfguaag2RqDQlVQy3J1//sUfCLZHIGenjrizpq/fpYHvfE5U7uasQQAPIYyAhCCkkvYU1q3wgpY/ql9KOwg8sFcRXe36daPu7lthjkeVkHOvClbf6hh3Wf500zA3hnu6JlbXhw4ll9TP6ZR0VfC3huQMrafoQaZkx8r1xp4N26GOkCeSkFNGQ8TWwtyu0lJUhTadZRQA9lwcAPIdRwcDRIdqNcab/gFTdGFtOA6vUjd1EiGj8QoJxAtJKUMsBYVX8CKO9bGa6f7Q==</X509Certificate></X509Data></KeyInfo><Object Id=\"Object-CQT-Id-2da3b60292a14702aec6cb5b276e7768\"><SignatureProperties xmlns=\"\"><SignatureProperty Target=\"#CQT-Id-2da3b60292a14702aec6cb5b276e7768\" Id=\"SignatureProperty-CQT-Id-2da3b60292a14702aec6cb5b276e7768\"><SigningTime>2022-03-25T08:17:07</SigningTime></SignatureProperty></SignatureProperties></Object></Signature></CQT></DSCKS></TBao></DLieu></TDiep></DuLieu></KetQuaTraCuu>");
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
		NodeList nodeList = null;
		Node node = null;
		List<Document> rows = null;
		if (docTmp.get("data") != null) {
			rows = docTmp.getList("data", Document.class);
		}
		ArrayList<HashMap<String, Object>> rowsReturn = new ArrayList<HashMap<String, Object>>();
		HashMap<String, Object> hItem = null;

		for (int i = 1; i <= 20; i++) {
			if (xPath.evaluate("DuLieu/TDiep[" + i + "]", nodeKetQuaTraCuu, XPathConstants.NODE) == null)
				break;
			nodeTDiep = (Node) xPath.evaluate("DuLieu/TDiep[" + i + "]", nodeKetQuaTraCuu, XPathConstants.NODE);
			CQT_MLTDiep = commons
					.getTextFromNodeXML((Element) xPath.evaluate("TTChung/MLTDiep", nodeTDiep, XPathConstants.NODE));
			stt += 1;
//		   	nodeList = (NodeList) xPath.evaluate("DLieu/TBao/TTTNhan", nodeTDiep, XPathConstants.NODESET);
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

			} else if ("204".equals(CQT_MLTDiep)) {

				String LTBao = commons.getTextFromNodeXML(
						(Element) xPath.evaluate("DLieu/TBao/DLTBao/LTBao", nodeTDiep, XPathConstants.NODE));
				hItem = new HashMap<String, Object>();

				if (LTBao.equals("2")) {
					hItem.put("STT", stt);
					hItem.put("Date", commons.getTextFromNodeXML(
							(Element) xPath.evaluate("DLieu/TBao/DLTBao/TGGui", nodeTDiep, XPathConstants.NODE)));
					hItem.put("MLoi", commons.getTextFromNodeXML((Element) xPath
							.evaluate("DLieu/TBao/DLTBao/LHDMTTien/DSLDo/LDo/MLoi", nodeTDiep, XPathConstants.NODE)));
					hItem.put("MTLoi", commons.getTextFromNodeXML((Element) xPath
							.evaluate("DLieu/TBao/DLTBao/LHDMTTien/DSLDo/LDo/MTLoi", nodeTDiep, XPathConstants.NODE)));
					rowsReturn.add(hItem);
				} else {
					hItem.put("STT", stt);
					hItem.put("Date", commons.getTextFromNodeXML(
							(Element) xPath.evaluate("DLieu/TBao/DLTBao/TGGui", nodeTDiep, XPathConstants.NODE)));
					hItem.put("MLoi", commons.getTextFromNodeXML((Element) xPath
							.evaluate("DLieu/TBao/DLTBao/LHDMTTien/DSLDo/LDo/MLoi", nodeTDiep, XPathConstants.NODE)));
					hItem.put("MTLoi", commons.getTextFromNodeXML((Element) xPath
							.evaluate("DLieu/TBao/DLTBao/LHDMTTien/DSLDo/LDo/MTLoi", nodeTDiep, XPathConstants.NODE)));
					rowsReturn.add(hItem);
				}

			} else {
				hItem = new HashMap<String, Object>();
				hItem.put("STT", stt);
				hItem.put("Date", commons.getTextFromNodeXML(
						(Element) xPath.evaluate("DLieu/HDon/DLHDon/TTChung/NLap", nodeTDiep, XPathConstants.NODE)));
				hItem.put("MLoi", CQT_MLTDiep);
				hItem.put("MTLoi", "Đã hoàn thành");
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

	/* PUBLISH HD */
	public MsgRsp publishHD(JSONRoot jsonRoot) throws Exception {
		Msg msg = jsonRoot.getMsg();
		MsgHeader header = msg.getMsgHeader();
		MsgPage page = msg.getMsgPage();

		Object objData = msg.getObjData();

		List<Document> pipeline = null;
		Document docFind = null;
		Document docTmp = null;
		FindOneAndUpdateOptions options = null;

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
		ObjectId objectIssuerId = null;
		try {
			objectId = new ObjectId(_id);
		} catch (Exception e) {
		}
		try {
			objectIssuerId = new ObjectId(header.getIssuerId());
		} catch (Exception e) {
		}
		int currentYear = LocalDate.now().get(ChronoField.YEAR);

		String issuerId = header.getIssuerId();
		issuerLocks_sign.putIfAbsent(issuerId, new ReentrantLock());
		ReentrantLock lock = issuerLocks_sign.get(issuerId);

		if (lock.tryLock()) {
			try {
				/* KIEM TRA THONG TIN HD CO TON TAI KHONG */
				docFind = new Document("IssuerId", header.getIssuerId()).append("IsDelete", new Document("$ne", true))
						.append("_id", objectId);
				pipeline = new ArrayList<Document>();
				pipeline.add(new Document("$match", docFind));

				//////////////////////////////

				pipeline.add(new Document("$lookup", new Document("from", "DMMauSoKyHieu")
						.append("let",
								new Document("vMauSoHD", "$EInvoiceDetail.TTChung.MauSoHD").append("vIssuerId",
										"$IssuerId"))
						.append("pipeline",
								Arrays.asList(new Document("$match", new Document("$expr", new Document("$and",
										Arrays.asList(new Document("$gt", Arrays.asList("$ConLai", 0)),
												new Document("$eq", Arrays.asList("$IsActive", true)),
												new Document("$ne", Arrays.asList("$IsDelete", true)),
												new Document("$eq",
														Arrays.asList(new Document("$toString", "$_id"), "$$vMauSoHD")),
												new Document("$eq", Arrays.asList("$IssuerId", "$$vIssuerId")),
												new Document("$eq", Arrays.asList("$NamPhatHanh", currentYear))))))))
						.append("as", "DMMauSoKyHieu")));
				pipeline.add(new Document("$unwind",
						new Document("path", "$DMMauSoKyHieu").append("preserveNullAndEmptyArrays", true)));

				pipeline.add(new Document("$lookup",
						new Document("from", "EInvoiceMTT")
								.append("pipeline", Arrays.asList(
										new Document("$match",
												new Document("IsDelete", new Document("$ne", true))
														.append("EInvoiceDetail.TTChung.MauSoHD", "$$vMauSoHD")),
										new Document("$group",
												new Document("_id", "$EInvoiceDetail.TTChung.MauSoHD").append("SHDon",
														new Document("$max", "$EInvoiceDetail.TTChung.SHDon")))))
								.append("as", "NLap_MAX")));
				pipeline.add(new Document("$unwind",
						new Document("path", "$NLap_MAX").append("preserveNullAndEmptyArrays", true)));

				docTmp = null;

				MongoClient mongoClient = cfg.mongoClient();
				MongoCollection<Document> collection = mongoClient.getDatabase(cfg.dbName).getCollection("EInvoiceMTT");
				try {
					docTmp = collection.aggregate(pipeline).allowDiskUse(true).iterator().next();
				} catch (Exception e) {

				}

				mongoClient.close();

				if (null == docTmp) {
					responseStatus = new MspResponseStatus(9999, "Không tìm thấy thông tin hóa đơn.");
					rsp.setResponseStatus(responseStatus);
					return rsp;
				}
				if (null == docTmp.get("DMMauSoKyHieu")) {
					responseStatus = new MspResponseStatus(9999, "Hết số hóa đơn.");
					rsp.setResponseStatus(responseStatus);
					return rsp;
				}

				// CHECK STATUS CO DC ACTIVE CHUA
				boolean check_active = docTmp.getEmbedded(Arrays.asList("DMMauSoKyHieu", "Status"), false);
				if (check_active == true) {
					responseStatus = new MspResponseStatus(9999,
							"Mẫu hóa đơn đang được admin xử lý. Vui lòng chờ trong giây lát!!!");
					rsp.setResponseStatus(responseStatus);
					return rsp;
				}
				// END CHECK STATUS
				String mauSoHdon_ = docTmp.getEmbedded(Arrays.asList("DMMauSoKyHieu", "_id"), ObjectId.class)
						.toString();

				String ngayLap_ = commons.convertLocalDateTimeToString(
						commons.convertDateToLocalDateTime(
								docTmp.getEmbedded(Arrays.asList("EInvoiceDetail", "TTChung", "NLap"), Date.class)),
						"dd/MM/yyyy");

				// CHECK NGAY LAP SO HOA DON LON NHAT
				List<Document> pipeline1 = null;
				pipeline1 = new ArrayList<Document>();
				pipeline1.add(new Document("$match", docFind));
				pipeline1.add(new Document("$lookup",
						new Document("from", "EInvoiceMTT")
								.append("pipeline", Arrays.asList(
										new Document("$match", new Document("IsDelete", new Document("$ne", true))
												/* .append("SignStatusCode", "SIGNED") */
												.append("EInvoiceDetail.TTChung.MauSoHD", mauSoHdon_)),
										new Document("$group",
												new Document("_id", "$EInvoiceDetail.TTChung.MauSoHD").append("SHDon",
														new Document("$max", "$EInvoiceDetail.TTChung.SHDon")))))
								.append("as", "NLap_MAX")));
				pipeline1.add(new Document("$unwind",
						new Document("path", "$NLap_MAX").append("preserveNullAndEmptyArrays", true)));

				Document docTmp7 = null;

				mongoClient = cfg.mongoClient();
				collection = mongoClient.getDatabase(cfg.dbName).getCollection("EInvoiceMTT");
				try {
					docTmp7 = collection.aggregate(pipeline1).allowDiskUse(true).iterator().next();
				} catch (Exception ex) {

				}
				mongoClient.close();
				// END CHECK NGAY LAP

				if (docTmp7 != null) {
					// LAY NGAY LAP SO HOA DON
					String nl_mshd = docTmp7.getEmbedded(Arrays.asList("NLap_MAX", "_id"), "");
					int nl_shdon = docTmp7.getEmbedded(Arrays.asList("NLap_MAX", "SHDon"), 0);

					Document docFindNLap = new Document("IssuerId", header.getIssuerId())
							.append("EInvoiceDetail.TTChung.SHDon", nl_shdon)
							/* .append("SignStatusCode", Constants.INVOICE_SIGN_STATUS.SIGNED) */
							.append("EInvoiceStatus", new Document("$in",
									Arrays.asList(Constants.INVOICE_STATUS.COMPLETE, Constants.INVOICE_STATUS.PENDING,
											Constants.INVOICE_STATUS.ERROR_CQT, Constants.INVOICE_STATUS.PROCESSING,
											Constants.INVOICE_STATUS.ADJUSTED, Constants.INVOICE_STATUS.REPLACED)))
							.append("EInvoiceDetail.TTChung.MauSoHD", nl_mshd)
							.append("IsDelete", new Document("$ne", true));

					Document docTmp2 = null;

					mongoClient = cfg.mongoClient();
					collection = mongoClient.getDatabase(cfg.dbName).getCollection("EInvoiceMTT");
					try {
						docTmp2 = collection.find(docFindNLap).allowDiskUse(true).iterator().next();
					} catch (Exception ex) {

					}
					mongoClient.close();

					if (docTmp2 != null) {
						String nlap_shd_max_ = commons.convertLocalDateTimeToString(
								commons.convertDateToLocalDateTime(docTmp2
										.getEmbedded(Arrays.asList("EInvoiceDetail", "TTChung", "NLap"), Date.class)),
								"dd/MM/yyyy");

						DateTimeFormatter formatter_ = DateTimeFormatter.ofPattern("d/MM/yyyy");
						LocalDate localDate1_ = LocalDate.parse(ngayLap_, formatter_);
						LocalDate localDate2_ = LocalDate.parse(nlap_shd_max_, formatter_);
						LocalDate NHT = LocalDate.now();
						if (localDate1_.compareTo(localDate2_) < 0) {
							responseStatus = new MspResponseStatus(9999, "Ngày lập hóa đơn không được nhỏ hơn ngày "
									+ nlap_shd_max_ + " của hóa đơn trước đó. Vui lòng chọn lại ngày lập.");
							rsp.setResponseStatus(responseStatus);
							return rsp;
						}
						if (localDate1_.compareTo(NHT) > 0) {
							responseStatus = new MspResponseStatus(9999,
									"Ngày lập của hóa đơn đang ký không được lớn hơn ngày hiện tại. vui lòng chọn lại ngày lập.");
							rsp.setResponseStatus(responseStatus);
							return rsp;

						}

					}
				}
				// CHECK TRUONG HOP CHUA CO SO HOA DON
				DateTimeFormatter formatter_ = DateTimeFormatter.ofPattern("d/MM/yyyy");
				LocalDate localDate1_ = LocalDate.parse(ngayLap_, formatter_);
				LocalDate NHT = LocalDate.now();
				if (localDate1_.compareTo(NHT) > 0) {
					responseStatus = new MspResponseStatus(9999,
							"Ngày lập của hóa đơn đang ký không được lớn hơn ngày hiện tại. vui lòng chọn lại ngày lập.");
					rsp.setResponseStatus(responseStatus);
					return rsp;
				}
				// END LAY NGAY LAP

				/* AP DUNG 1 FILE TRUOC */
				String dir = docTmp.get("Dir", "");
				String fileName = "";
				String check_file_signed = docTmp.get("SignStatusCode", "NOSIGN");
				String id_ = docTmp.getObjectId("_id").toString();
				String LoaiHD = docTmp.getEmbedded(Arrays.asList("EInvoiceDetail", "TTChung", "LoaiHD"), "");

				if (check_file_signed.equals("SIGNED")) {
					fileName = id_ + "_signed.xml";
				} else {
					fileName = id_ + ".xml";
				}

				File file_ = new File(dir, fileName);

				if (!file_.exists()) {
					responseStatus = new MspResponseStatus(9999, "Tập tin không tồn tại.");
					rsp.setResponseStatus(responseStatus);
					return rsp;
				}

				String idDMMSKH = docTmp.getEmbedded(Arrays.asList("DMMauSoKyHieu", "_id"), ObjectId.class).toString();
				ObjectId objectIdMS = null;
				try {
					objectIdMS = new ObjectId(idDMMSKH);
				} catch (Exception e) {
				}
				/* TAO SO HD VA GHI DU LIEU VO FILE */
				int checkshd = docTmp.getEmbedded(Arrays.asList("EInvoiceDetail", "TTChung", "SHDon"), 0);
				int checkshdht = docTmp.getEmbedded(Arrays.asList("DMMauSoKyHieu", "SHDHT"), 0);
				int sl = docTmp.getEmbedded(Arrays.asList("DMMauSoKyHieu", "SoLuong"), 0);
				int cl = docTmp.getEmbedded(Arrays.asList("DMMauSoKyHieu", "ConLai"), 0);
				int checktontai = sl - cl;
				int ktshdht = 0;
				/// kiem tra chưa co bien nhung da co hoa don
				// tim hoa don moi nhat
				if (checkshdht == 0 && checktontai > 0) {
					pipeline = null;
					Document docMatch = new Document("IssuerId", header.getIssuerId())
							.append("EInvoiceDetail.TTChung.MauSoHD", idDMMSKH)
							.append("IsDelete", new Document("$ne", true))
							.append("EInvoiceStatus", new Document("$in", Arrays.asList("DELETED", "DELETE",
									"ERROR_CQT", "XOABO", "COMPLETE", "PROCESSING")));

					pipeline = new ArrayList<Document>();
					pipeline.add(new Document("$match", docMatch));
					pipeline.add(new Document("$addFields", new Document("SHDon", new Document("$ifNull",
							Arrays.asList("$EInvoiceDetail.TTChung.SHDon", Integer.MAX_VALUE)))));
					pipeline.add(new Document("$sort",
							new Document("EInvoiceDetail.TTChung.MauSoHD", -1).append("SHDon", -1).append("_id", -1)));
					Document docTmp1_ = null;

					mongoClient = cfg.mongoClient();
					collection = mongoClient.getDatabase(cfg.dbName).getCollection("EInvoiceMTT");
					try {
						docTmp1_ = collection.aggregate(pipeline).allowDiskUse(true).iterator().next();
					} catch (Exception ex) {

					}
					mongoClient.close();

					ktshdht = docTmp1_.getEmbedded(Arrays.asList("EInvoiceDetail", "TTChung", "SHDon"), 0);
				}
				///////////////// sau khi check lay ra shd lon nhat
				if (ktshdht > 0) {
					checkshdht = ktshdht;
				}

				int getSL = docTmp.getEmbedded(Arrays.asList("DMMauSoKyHieu", "SoLuong"), 0);
				int eInvoiceNumber = 0;

				if (checkshd == 0) {
					eInvoiceNumber = checkshdht + 1;
				} else {
					eInvoiceNumber = checkshd;
				}
				int sht = checkshdht + 1;
				int CL = getSL - sht;

				/* DOC DU LIEU XML, VA GHI DU LIEU VO SO HD VA MA CQT */

				org.w3c.dom.Document doc1 = commons.fileToDocument(file_);
				XPath xPath = XPathFactory.newInstance().newXPath();
				Node nodeDLHDon = (Node) xPath.evaluate("/HDon/DLHDon[@Id='data']", doc1, XPathConstants.NODE);
				Node nodeMaCQT = (Node) xPath.evaluate("/HDon", doc1, XPathConstants.NODE);
				Node nodeTmp = null;
				nodeTmp = (Node) xPath.evaluate("TTChung", nodeDLHDon, XPathConstants.NODE);
				Element elementSub = (Element) xPath.evaluate("SHDon", nodeTmp, XPathConstants.NODE);
				if (null == elementSub) {
					elementSub = doc1.createElement("SHDon");
					elementSub.setTextContent(String.valueOf(eInvoiceNumber));
					nodeTmp.appendChild(elementSub);
				} else {
					elementSub.setTextContent(String.valueOf(eInvoiceNumber));
				}

				Document docTmp1 = null;
				Document findUser = new Document("_id", objectIssuerId).append("IsActive", true).append("IsDelete",
						new Document("$ne", true));

				mongoClient = cfg.mongoClient();
				collection = mongoClient.getDatabase(cfg.dbName).getCollection("Issuer");
				try {
					docTmp1 = collection.find(findUser).allowDiskUse(true).iterator().next();
				} catch (Exception ex) {

				}
				mongoClient.close();

				if (null == docTmp1) {
					responseStatus = new MspResponseStatus(9999, "Không tìm thấy thông tin người dùng.");
					rsp.setResponseStatus(responseStatus);
					return rsp;
				}

				String MaCQT = docTmp1.get("MaCQT", "");

				if (MaCQT.equals("")) {
					responseStatus = new MspResponseStatus(9999, "Vui lòng cập nhật mã doanh nghiệp!!!");
					rsp.setResponseStatus(responseStatus);
					return rsp;
				}

				/* CHECK HOA DON DA CO MA CQT CHUA */
				String MCCQT = docTmp.get("MCCQT", "");

				if (MCCQT.equals("")) {
					/* TAO prefixUserID CAP NHAT SO LogNextSequence DB */
					String prefixUserID = "";

					mongoClient = cfg.mongoClient();

					int seq = getValueForNextSequenceAdmin(mongoTemplate, header.getIssuerId(),
							Constants.NEXT_SEQUENCE.EINVOICEMTT, MaCQT);
					prefixUserID = StringUtils.leftPad(String.valueOf(seq), 11, "0");

					// GHEP MA CQT

					String NgayLap = LocalDate.now().toString();

					String[] split_year = NgayLap.split("-");

					String year_ = split_year[0];

					String year = year_.substring(2, 4);

					String maCQT = "M" + LoaiHD + "-" + year + "-" + MaCQT + "-" + prefixUserID;

					Element elementSub_cqt = (Element) xPath.evaluate("MCCQT", nodeMaCQT, XPathConstants.NODE);
					if (null == elementSub_cqt) {
						elementSub_cqt = doc1.createElement("MCCQT");
						elementSub_cqt.setTextContent(String.valueOf(maCQT));
						nodeMaCQT.appendChild(elementSub_cqt);
					} else {
						elementSub_cqt.setTextContent(String.valueOf(maCQT));
					}

					FileInfo fileInfo = new FileInfo();
					fileInfo.setFileName(fileName);
					fileInfo.setContentFile(commons.docW3cToByte(doc1));

					// SAVE FILE
					String filename_publish = _id + "_pending.xml";
					File file_publish = new File(dir, filename_publish);
					FileUtils.writeByteArrayToFile(new File(file_publish.toString()), fileInfo.getContentFile());

					/* UPDATE EINVOICE - STATUS */
					options = new FindOneAndUpdateOptions();
					options.upsert(false);
					options.maxTime(5000, TimeUnit.MILLISECONDS);
					options.returnDocument(ReturnDocument.AFTER);

					mongoClient = cfg.mongoClient();
					collection = mongoClient.getDatabase(cfg.dbName).getCollection("EInvoiceMTT");
					collection.findOneAndUpdate(docFind,
							new Document("$set",
									new Document("EInvoiceDetail.TTChung.SHDon", eInvoiceNumber).append("MCCQT", maCQT)
											.append("PublishStatus", true).append("EInvoiceStatus", "PENDING")),
							options);
					mongoClient.close();

				}
				if (checkshd == 0) {
					Document docFindMS = null;
					docFindMS = new Document("IssuerId", header.getIssuerId())
							.append("IsDelete", new Document("$ne", true)).append("IsActive", true)
							.append("_id", objectIdMS);

					mongoClient = cfg.mongoClient();
					collection = mongoClient.getDatabase(cfg.dbName).getCollection("DMMauSoKyHieu");
					collection.findOneAndUpdate(docFindMS,
							new Document("$set", new Document("ConLai", CL).append("SHDHT", sht)), options);
					mongoClient.close();
				}
			} finally {
				lock.unlock();
			}
		} else {
			responseStatus = new MspResponseStatus(9999, "Tồn tại user đang thực hiện Ký, vui lòng thử lại.");
			rsp.setResponseStatus(responseStatus);
			return rsp;
		}

		responseStatus = new MspResponseStatus(0, "SUCCESS");
		rsp.setResponseStatus(responseStatus);
		return rsp;
	}

	/* CREATE THONG DIEP TO SIGN SEND THUE */

	public MsgRsp createTDSendTax(JSONRoot jsonRoot) throws Exception {
		Msg msg = jsonRoot.getMsg();
		MsgHeader header = msg.getMsgHeader();
		MsgPage page = msg.getMsgPage();

		Object objData = msg.getObjData();

		List<Document> pipeline = null;
		Document docFind = null;
		Document docTmp = null;
		org.w3c.dom.Document doc = null;
		List<org.w3c.dom.Document> ListDoc = new ArrayList<>();
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

//		int check_soluong = 0;
//		
//		String mauSoHdon = "";
//		String KHHDon = "";
//		String KHMSHDon = "";
//		for(int t = 0; t< ids.size(); t++ ) {
//			_id = ids.get(t) ;
		ObjectId objectId = null;
		try {
			objectId = new ObjectId(_id);
		} catch (Exception e) {
		}

		/* KIEM TRA THONG TIN HD CO TON TAI KHONG */
		docFind = new Document("IssuerId", header.getIssuerId()).append("IsDelete", new Document("$ne", true))
				.append("_id", objectId);
		pipeline = new ArrayList<Document>();
		pipeline.add(new Document("$match", docFind));

		docTmp = null;

		MongoClient mongoClient = cfg.mongoClient();
		MongoCollection<Document> collection = mongoClient.getDatabase(cfg.dbName).getCollection("EInvoiceMTT");
		try {
			docTmp = collection.aggregate(pipeline).allowDiskUse(true).iterator().next();
		} catch (Exception e) {

		}

		mongoClient.close();

		if (null == docTmp) {
			responseStatus = new MspResponseStatus(9999, "Không tìm thấy thông tin hóa đơn!!!");
			rsp.setResponseStatus(responseStatus);
			return rsp;
		}
		File file = null;
		String dir = docTmp.get("Dir", "");

		String fileName = "";
//		mauSoHdon = docTmp.getEmbedded(Arrays.asList("EInvoiceDetail","TTChung", "MauSoHD"), "");		
//		KHHDon = docTmp.getEmbedded(Arrays.asList("EInvoiceDetail","TTChung", "KHHDon"), "");
//		KHMSHDon = docTmp.getEmbedded(Arrays.asList("EInvoiceDetail","TTChung", "KHMSHDon"), "");

		fileName = _id + "_pending.xml";

		file = new File(dir, fileName);
		if (!file.exists() || !file.isFile()) {
			responseStatus = new MspResponseStatus(9999, "Không tìm thấy thông tin hóa đơn!!!");
			rsp.setResponseStatus(responseStatus);
			return rsp;
		}

		doc = commons.fileToDocument(file, true);
		if (null == doc) {
			responseStatus = new MspResponseStatus(9999, "Không tìm thấy thông tin hóa đơn!!!");
			rsp.setResponseStatus(responseStatus);
			return rsp;
		}

		ListDoc.add(doc);
//		check_soluong++;
//	}
//	//end for 

//		String soluong = String.valueOf(check_soluong);
		String MTDiep = docTmp.get("MTDiep", "");
		String MST = docTmp.getEmbedded(Arrays.asList("EInvoiceDetail", "NDHDon", "NBan", "MST"), "");
		//
		/// TẠO THÔNG ĐIỆP

		/* TAO XML THONG DIEP GUI DI */
		DocumentBuilderFactory dbf1 = DocumentBuilderFactory.newInstance();
		DocumentBuilder db1 = dbf1.newDocumentBuilder();

		org.w3c.dom.Document doc1 = db1.newDocument();
		doc1.setXmlStandalone(true);

		Element root1 = doc1.createElement("TDiep");
		doc1.appendChild(root1);

		Element elementContent1 = doc1.createElement("TTChung");

		elementContent1
				.appendChild(commons.createElementWithValue(doc1, "PBan", Constants.TDiep_TTChung_TCTN_VISNAM.PBan1));
		elementContent1.appendChild(commons.createElementWithValue(doc1, "MNGui", SystemParams.MSTTCGP));
		elementContent1.appendChild(commons.createElementWithValue(doc1, "MNNhan", SystemParams.MSTDVTN));
		elementContent1.appendChild(commons.createElementWithValue(doc1, "MLTDiep", "206"));
		elementContent1.appendChild(commons.createElementWithValue(doc1, "MTDiep", MTDiep));
		elementContent1.appendChild(commons.createElementWithValue(doc1, "MTDTChieu", ""));
		elementContent1.appendChild(commons.createElementWithValue(doc1, "MST", MST)); // MA SO THUE NGUOI NOP THUE
		elementContent1.appendChild(commons.createElementWithValue(doc1, "SLuong", "1"));
		root1.appendChild(elementContent1);

		elementContent1 = doc1.createElement("DLieu");
		for (org.w3c.dom.Document oo : ListDoc) {
			Element elem01 = oo.getDocumentElement();
			Node copiedRoot = doc1.importNode(elem01, true);
			elementContent1.appendChild(copiedRoot);
			root1.appendChild(elementContent1);
		}

		boolean boo = false;
		String fileName_TD = _id + "_preparingSendCQT.xml";

		try {
			boo = commons.docW3cToFile(doc1, dir, fileName_TD);
		} catch (Exception e) {
		}
		if (!boo) {
			responseStatus = new MspResponseStatus(9999, "Lưu tập tin gửi thuế không thành công.");
			rsp.setResponseStatus(responseStatus);
			return rsp;
		}

		/* CAP NHAT LAI TRANG THAI DANG CHO XU LY */
		FindOneAndUpdateOptions options = null;
		options = new FindOneAndUpdateOptions();
		options.upsert(false);
		options.maxTime(5000, TimeUnit.MILLISECONDS);
		options.returnDocument(ReturnDocument.AFTER);

		mongoClient = cfg.mongoClient();
		collection = mongoClient.getDatabase(cfg.dbName).getCollection("EInvoiceMTT");
		collection.findOneAndUpdate(docFind,
				new Document("$set",
						new Document("CreateTDStatus", true).append("CreateTDStatus_Date", LocalDateTime.now()).append(
								"InfoCreateTDStatus",
								new Document("Date", LocalDateTime.now()).append("UserID", header.getUserId())
										.append("UserName", header.getUserName())
										.append("UserFullName", header.getUserFullName()))),
				options);
		mongoClient.close();

		responseStatus = new MspResponseStatus(0, "SUCCESS");
		rsp.setResponseStatus(responseStatus);
		return rsp;
	}

//	
//	public MsgRsp list_send_cqt(JSONRoot jsonRoot) throws Exception {
//		Msg msg = jsonRoot.getMsg();
//		MsgHeader header = msg.getMsgHeader();
//		MsgPage page = msg.getMsgPage();
//		Object objData = msg.getObjData();
//		
//		String mauSoHdon = "";
//		String soHoaDon = "";
//		String date = "";
//		String sendCQTStatus = "";
//
//		
//		JsonNode jsonData = null;
//		if(objData != null) {
//			jsonData = Json.serializer().nodeFromObject(objData);
//			mauSoHdon = commons.getTextJsonNode(jsonData.at("/MauSoHdon")).replaceAll("\\s", "");
//			soHoaDon = commons.getTextJsonNode(jsonData.at("/SoHoaDon")).replaceAll("\\s", "");
//			date = commons.getTextJsonNode(jsonData.at("/Date")).replaceAll("\\s", "");
////			status = commons.getTextJsonNode(jsonData.at("/Status")).replaceAll("\\s", "");
////			signStatus = commons.getTextJsonNode(jsonData.at("/SignStatus")).replaceAll("\\s", "");
//			sendCQTStatus = commons.getTextJsonNode(jsonData.at("/SendCQTStatus")).replaceAll("\\s", "");
////			nbanMst = commons.getTextJsonNode(jsonData.at("/NbanMst")).trim().replaceAll("\\s+", " ");
////			nbanTen = commons.getTextJsonNode(jsonData.at("/NbanTen")).trim().replaceAll("\\s+", " ");
//		}
//
//		MsgRsp rsp = new MsgRsp(header);
//		MspResponseStatus responseStatus = null;
//		Document docMatch = null;
//		ObjectId objectId = null;
//		Document docTmp = null;
//		List<Document> pipeline = new ArrayList<Document>();
//		ArrayList<HashMap<String, Object>> rowsReturn = new ArrayList<HashMap<String, Object>>();
//		HashMap<String, Object> hItem = null;
////		LocalDate dateFrom = null;
//		LocalDate date_ = null;
////		Document docMatchDate = null;
////		int rowsign = 0;
//		date_ =  "".equals(date) || !commons.checkLocalDate(date, Constants.FORMAT_DATE.FORMAT_DATE_WEB)? null: commons.convertStringToLocalDate(date, Constants.FORMAT_DATE.FORMAT_DATE_WEB);
//	
//
//		 docMatch = new Document("IssuerId", header.getIssuerId())
//					.append("IsDelete", new Document("$ne", true));
//			if(!"".equals(mauSoHdon))
//				docMatch.append("EInvoiceDetail.TTChung.MauSoHD", commons.regexEscapeForMongoQuery(mauSoHdon));
//			if(!"".equals(soHoaDon))
//				docMatch.append("EInvoiceDetail.TTChung.SHDon", commons.stringToInteger(soHoaDon));
//			
//				docMatch.append("EInvoiceDetail.TTChung.NLap", date_);
//				
//				
//				docMatch.append("EInvoiceStatus", new Document("$in", Arrays.asList("PENDING", "PROCESSING", "COMPLETE", "ERROR_CQT")));
//				
//				if(!"".equals(sendCQTStatus)) {
//					if(sendCQTStatus.equals("true")) {					
//						docMatch.append("SendCQTStatus", true);
//					}else {
//						docMatch.append("SendCQTStatus", new Document("$in", Arrays.asList(false, null)));
//					}
//				}
//				
//			pipeline = new ArrayList<Document>();
//			pipeline.add(new Document("$match", docMatch));
//			pipeline.add(
//				new Document("$addFields", 
//					new Document("SHDon", 
//						new Document("$ifNull", Arrays.asList("$EInvoiceDetail.TTChung.SHDon", Integer.MAX_VALUE))
//					)
//				)
//			);
//			pipeline.add(
//				new Document("$sort", 
//					new Document("EInvoiceDetail.TTChung.MauSoHD", -1).append("SHDon", -1).append("_id", -1)
//				)
//			);
//			pipeline.addAll(createFacetForSearchNotSort(page));
//		
//		MongoClient mongoClient = cfg.mongoClient();
//		MongoCollection<Document> collection = mongoClient.getDatabase(cfg.dbName).getCollection("EInvoiceMTT");
//		      try {
//		        docTmp = collection.aggregate(pipeline).allowDiskUse(true).iterator().next();    
//		      } catch (Exception e) {
//		        
//		      }
//		        
//		mongoClient.close();
//		
//		if(null == docTmp) {
//			responseStatus = new MspResponseStatus(9999, Constants.MAP_ERROR.get(9999));
//			rsp.setResponseStatus(responseStatus);
//			return rsp;
//		}
//		rsp = new MsgRsp(header);
//		
//		page.setTotalRows(docTmp.getInteger("total", 0));
//		rsp.setMsgPage(page);
//		
//		
//		responseStatus = null;
//		List<Document> rows = null;
//		if(docTmp != null) {
//		if(docTmp.get("data") != null && docTmp.get("data") instanceof List) {
//			rows = docTmp.getList("data", Document.class);
//		}
//	}
//	
//		
//		
//		if(null != rows) {
//			for(Document doc: rows) {
//				objectId = (ObjectId) doc.get("_id");
//				
//				hItem = new HashMap<String, Object>();
//				hItem.put("_id", objectId.toString());
//				hItem.put("EInvoiceStatus", doc.get("EInvoiceStatus"));
//				hItem.put("SignStatusCode", doc.get("SignStatusCode"));
//				hItem.put("MCCQT", doc.get("MCCQT"));
//				hItem.put("MTDiep", doc.get("MTDiep"));
//				hItem.put("MTDTChieu", doc.get("MTDTChieu"));
//				hItem.put("EInvoiceDetail", doc.get("EInvoiceDetail"));
//				hItem.put("InfoCreated", doc.get("InfoCreated"));
//				hItem.put("LDo", doc.get("LDo"));			
//				hItem.put("HDSS", doc.get("HDSS"));				
//				hItem.put("SendCQT_Date", doc.get("SendCQT_Date"));
//				hItem.put("CQT_Date", doc.get("CQT_Date"));
//				hItem.put("SendCQTStatus", doc.get("SendCQTStatus"));
//				rowsReturn.add(hItem);
//			}
//		}
//		
//		responseStatus = new MspResponseStatus(0, "SUCCESS");
//		rsp.setResponseStatus(responseStatus);
//		
//		HashMap<String, Object> mapDataR = new HashMap<String, Object>();
//		mapDataR.put("rows", rowsReturn);
//		rsp.setObjData(mapDataR);
//		return rsp;
//	}
//

//	/* SEND CQT */
//	
//	public MsgRsp sendCQTMTT(JSONRoot jsonRoot) throws Exception {
//		Msg msg = jsonRoot.getMsg();
//		MsgHeader header = msg.getMsgHeader();
//		MsgPage page = msg.getMsgPage();
//		Object objData = msg.getObjData();
//
//		JsonNode jsonData = null;
//		if (objData != null) {
//			jsonData = Json.serializer().nodeFromObject(msg.getObjData());
//		} else {
//			throw new Exception("Lỗi dữ liệu đầu vào");
//		}
//
//		
//		String _id = commons.getTextJsonNode(jsonData.at("/_id")).replaceAll("\\s", "");
//		
//		MsgRsp rsp = new MsgRsp(header);
//		rsp.setMsgPage(page);
//		MspResponseStatus responseStatus = null;
//
//		ObjectId objectId = null;
//		List<Document> pipeline = null;
//		Document docFind = null;
//		Document docTmp = null;
//		FindOneAndUpdateOptions options = null;
//		File file = null;
//		String MTDiep = "";
//		String MST = "";
//		org.w3c.dom.Document doc = null;
//			objectId = null;
//			try {
//				objectId = new ObjectId(_id);
//			} catch (Exception e) {
//			}
//			/* KIEM TRA THONG TIN HD CO TON TAI KHONG */
//			docFind = new Document("IssuerId", header.getIssuerId()).append("IsDelete", new Document("$ne", true))
//					.append("_id", objectId)/* .append("EInvoiceStatus", Constants.INVOICE_STATUS.PENDING) */
//			/* .append("SignStatusCode", Constants.INVOICE_SIGN_STATUS.SIGNED) */;
//			pipeline = new ArrayList<Document>();
//			pipeline.add(new Document("$match", docFind));
//			pipeline.add(new Document("$lookup", new Document("from", "EInvoiceMTT")
//					.append("let",
//							new Document("vIssuerId", "$IssuerId").append("vMauSo", "$EInvoiceDetail.TTChung.MauSoHD"))
//					.append("pipeline", Arrays.asList(
//							new Document("$match", new Document("$expr", new Document("$and", Arrays.asList(
//									new Document("$eq", Arrays.asList("$IssuerId", "$$vIssuerId")),
//									new Document("$eq", Arrays.asList("$EInvoiceDetail.TTChung.MauSoHD", "$$vMauSo")),
//									new Document("$ne", Arrays.asList("$IsDelete", true)),
////									new Document("$eq",
////											Arrays.asList("$SignStatusCode", Constants.INVOICE_SIGN_STATUS.SIGNED)),
//									new Document("$in", Arrays.asList("$EInvoiceStatus",
//											Arrays.asList(Constants.INVOICE_STATUS.COMPLETE,
//													Constants.INVOICE_STATUS.ERROR_CQT,
//													Constants.INVOICE_STATUS.DELETED, Constants.INVOICE_STATUS.REPLACED,
//													Constants.INVOICE_STATUS.ADJUSTED))))))),
//							new Document("$group",
//									new Document("_id", "$EInvoiceDetail.TTChung.MauSoHD").append("SHDon",
//											new Document("$max", "$EInvoiceDetail.TTChung.SHDon")))))
//					.append("as", "EInvoiceMAXCQT")));
//			pipeline.add(new Document("$unwind",
//					new Document("path", "$EInvoiceMAXCQT").append("preserveNullAndEmptyArrays", true)));
//
//			docTmp = null;
////			org.w3c.dom.Document rTCTN = null;
//			
//			MongoClient mongoClient = cfg.mongoClient();
//			MongoCollection<Document> collection = mongoClient.getDatabase(cfg.dbName).getCollection("EInvoiceMTT");
//			      try {
//			        docTmp = collection.aggregate(pipeline).allowDiskUse(true).iterator().next();    
//			      } catch (Exception e) {
//			        
//			      }
//			        
//			mongoClient.close();
//			
//			if (null == docTmp) {
//				responseStatus = new MspResponseStatus(9999, "Không tìm thấy thông tin hóa đơn.");
//				rsp.setResponseStatus(responseStatus);
//				return rsp;
//			}
//
//
//			org.w3c.dom.Document rTCTN = null;
//			org.w3c.dom.Document rTCTN1 = null;
//			String codeTTTNhan = "";
//			String descTTTNhan="";
//			String dir = docTmp.get("Dir", "");
//						
//			String fileName = "";
//			String check_file_signed = docTmp.get("SignStatusCode", "NOSIGN");
//					
//			if(check_file_signed.equals("SIGNED")) {
//				fileName = _id + "_signed.xml";
//			}else {
//				fileName = _id + "_pending.xml";
//			}
//
//			file = new File(dir, fileName);
//			if (!file.exists() || !file.isFile()) {
//				responseStatus = new MspResponseStatus(9999, "Không tìm thấy thông tin hóa đơn.");
//				rsp.setResponseStatus(responseStatus);
//				return rsp;
//			}
//
//			doc = commons.fileToDocument(file, true);
//			if (null == doc) {
//				responseStatus = new MspResponseStatus(9999, "Dữ liệu hóa đơn không tồn tại.");
//				rsp.setResponseStatus(responseStatus);
//				return rsp;
//			}
//
////			String MTDiep1 = "";
//			String MaKetQua = "";
//			String CQT_MLTDiep  = "";
//			XPath xPath1 = null;
//			String MLoi = "";
//			Node nodeKetQuaTraCuu = null;
//			MTDiep = docTmp.get("MTDiep", "");
//			MST = docTmp.getEmbedded(Arrays.asList("EInvoiceDetail", "NDHDon", "NBan", "MST"), "");
//			
//			//TRA CUU HOA DƠN TRC KHI CALL TIEP NHAN THONG DIEP
//		
//			
//			try {
//				rTCTN1 = tctnService.callTraCuuThongDiep(MTDiep);
//			} catch (Exception e) {
//
//			}
//		
//			 if (rTCTN1 == null) {
//				
//					 try {
////							MTDiep1 = docTmp.get("MTDiep", "");
//							 rTCTN1 = tctnService.callTraCuuThongDiep(MTDiep);
//						} catch (Exception e) {
//
//						}	 
//			 }
//			 	xPath1 = XPathFactory.newInstance().newXPath();
//				 nodeKetQuaTraCuu = (Node) xPath1.evaluate("/KetQuaTraCuu", rTCTN1, XPathConstants.NODE);
//				 MaKetQua = commons.getTextFromNodeXML((Element) xPath1.evaluate("MaKetQua", nodeKetQuaTraCuu, XPathConstants.NODE));
//				 if("2".equals(MaKetQua)) {
//					
//						try {
//							rTCTN = tctnService.callTiepNhanThongDiep("200", MTDiep, MST, "1", doc);	
//						} catch (Exception e) {
//				
//						}
//						XPath xPath = XPathFactory.newInstance().newXPath();
//						Node nodeDLHDon = (Node) xPath.evaluate("/TDiep", rTCTN, XPathConstants.NODE);
//						 codeTTTNhan = commons.getTextFromNodeXML((Element) xPath.evaluate("DLieu/TBao/TTTNhan", nodeDLHDon, XPathConstants.NODE));
//						 descTTTNhan = commons.getTextFromNodeXML((Element) xPath.evaluate("DLieu/TBao/DSLDo/LDo/MTa", nodeDLHDon, XPathConstants.NODE));				 
//				 }else {					 								 
//					 Node nodeTDiep = null;
//						for (int i = 1; i <= 5; i++) {
//							if (xPath1.evaluate("DuLieu/TDiep[" + i + "]", nodeKetQuaTraCuu, XPathConstants.NODE) == null)
//								break;
//							nodeTDiep = (Node) xPath1.evaluate("DuLieu/TDiep[" + i + "]", nodeKetQuaTraCuu, XPathConstants.NODE);
////							if(xPath.evaluate("DLieu/*/DSCKS", nodeTDiep, XPathConstants.NODE) != null)
//							if (xPath1.evaluate("DLieu/*/MCCQT", nodeTDiep, XPathConstants.NODE) != null)
//								break;
//						}
//					 CQT_MLTDiep = commons.getTextFromNodeXML((Element) xPath1.evaluate("TTChung/MLTDiep", nodeTDiep, XPathConstants.NODE));
//					
//			
//			if("202".equals(CQT_MLTDiep)) {
//				codeTTTNhan = MaKetQua;
//			}if("204".equals(CQT_MLTDiep)){
//				 MLoi = commons.getTextFromNodeXML((Element) xPath1.evaluate("DLieu/TBao/DLTBao/LCMa/DSLDo/LDo/MLoi", nodeTDiep, XPathConstants.NODE));					
//				 responseStatus = new MspResponseStatus(9999, MLoi);
//					rsp.setResponseStatus(responseStatus);
//					return rsp;
//			}			
//		}
//			switch (codeTTTNhan) {
//			case "1":
//				responseStatus = new MspResponseStatus(9999,
//						"".equals(descTTTNhan) ? "Không tìm thấy tenant dữ liệu." : descTTTNhan);
//				rsp.setResponseStatus(responseStatus);
//				return rsp;
//			case "2":
//				responseStatus = new MspResponseStatus(9999, "Mã thông điệp đã tồn tại.");
//				rsp.setResponseStatus(responseStatus);
//				return rsp;
//			case "3":
//				responseStatus = new MspResponseStatus(9999,
//						"".equals(descTTTNhan) ? "Thất bại, lỗi Exception." : descTTTNhan);
//				rsp.setResponseStatus(responseStatus);
//				return rsp;
//			default:
//				
//				break;
//			}
//			/* CAP NHAT LAI TRANG THAI DANG CHO XU LY */
//		
//		try {
//			options = new FindOneAndUpdateOptions();
//			options.upsert(false);
//			options.maxTime(5000, TimeUnit.MILLISECONDS);
//			options.returnDocument(ReturnDocument.AFTER);
//			
//			mongoClient = cfg.mongoClient();
//			collection = mongoClient.getDatabase(cfg.dbName).getCollection("EInvoiceMTT");
//			collection.findOneAndUpdate(docFind,
//					new Document("$set",
//							new Document("EInvoiceStatus", Constants.INVOICE_STATUS.PROCESSING)
//							.append("SendCQT_Date", LocalDateTime.now())
//							.append("InfoSendCQT",
//									new Document("Date", LocalDateTime.now()).append("UserID", header.getUserId())
//											.append("UserName", header.getUserName())
//											.append("UserFullName", header.getUserFullName()))),
//					options);
//			 mongoClient.close();
//			
//			responseStatus = new MspResponseStatus(0, Constants.MAP_ERROR.get(0));
//			rsp.setResponseStatus(responseStatus);
//			return rsp;
//		} catch (Exception e) {
//			responseStatus = new MspResponseStatus(9999, "Vui lòng gửi lấy mã hóa đơn lại.");
//			rsp.setResponseStatus(responseStatus);
//			return rsp;
//		}
//	
//	
//	}
//

//	public MsgRsp sendListCQT(JSONRoot jsonRoot) throws Exception {
//		Msg msg = jsonRoot.getMsg();
//		MsgHeader header = msg.getMsgHeader();
//		MsgPage page = msg.getMsgPage();
//
//		Object objData = msg.getObjData();
//
//		List<String> ids = null;
//		List<Document> pipeline = null;
//		Document docFind = null;
//		Document docTmp = null;
//		org.w3c.dom.Document doc = null;
//		List<org.w3c.dom.Document> ListDoc = new ArrayList<>();
//		JsonNode jsonData = null;
//		if (objData != null) {
//			jsonData = Json.serializer().nodeFromObject(msg.getObjData());
//		} else {
//			throw new Exception("Lỗi dữ liệu đầu vào");
//		}
//		
//		String _id = "";
//		String _ListId = commons.getTextJsonNode(jsonData.at("/_id")).replaceAll("\\s", "");
//		ids = null;
//		try {
//			ids = Json.serializer().fromJson(commons.decodeBase64ToString(_ListId), new TypeReference<List<String>>() {
//			});
//		}catch(Exception e) {}
//		
//
//		MsgRsp rsp = new MsgRsp(header);
//		rsp.setMsgPage(page);
//		MspResponseStatus responseStatus = null;
//		
////		org.w3c.dom.Document rTCTN = null;
////		String codeTTTNhan = "";
////		String descTTTNhan="";
//		int check_soluong = 0;
//		
//		String mauSoHdon = "";
//		String KHHDon = "";
//		String KHMSHDon = "";
//		for(int t = 0; t< ids.size(); t++ ) {
//			_id = ids.get(t) ;
//		ObjectId objectId = null;
//		try {
//			objectId = new ObjectId(_id);
//		} catch (Exception e) {
//		}
//		
//		/* KIEM TRA THONG TIN HD CO TON TAI KHONG */
//		docFind = new Document("IssuerId", header.getIssuerId())
//				.append("IsDelete", new Document("$ne", true))
//				.append("_id", objectId);
//		pipeline = new ArrayList<Document>();
//		pipeline.add(new Document("$match", docFind));
//
//		docTmp = null;
//		
//		MongoClient mongoClient = cfg.mongoClient();
//		MongoCollection<Document> collection = mongoClient.getDatabase(cfg.dbName).getCollection("EInvoiceMTT");
//		      try {
//		        docTmp = collection.aggregate(pipeline).allowDiskUse(true).iterator().next();    
//		      } catch (Exception e) {
//		        
//		      }
//		        
//		mongoClient.close();
//		
//		if (null == docTmp) {
//			continue;
//		}
//		File file = null;
//		String dir = docTmp.get("Dir", "");
//					
//		String fileName = "";
//		String check_file_signed = docTmp.get("SignStatusCode", "NOSIGN");
//		mauSoHdon = docTmp.getEmbedded(Arrays.asList("EInvoiceDetail","TTChung", "MauSoHD"), "");		
//		KHHDon = docTmp.getEmbedded(Arrays.asList("EInvoiceDetail","TTChung", "KHHDon"), "");
//		KHMSHDon = docTmp.getEmbedded(Arrays.asList("EInvoiceDetail","TTChung", "KHMSHDon"), "");
//		
//		if(check_file_signed.equals("SIGNED")) {
//			fileName = _id + "_signed.xml";
//		}else {
//			fileName = _id + ".xml";
//		}
//
//		file = new File(dir, fileName);
//		if (!file.exists() || !file.isFile()) {
//			continue;
//		}
//
//		doc = commons.fileToDocument(file, true);
//		if (null == doc) {
//			continue;
//		}
//
//		ListDoc.add(doc);	
//		check_soluong++;
//	}
//	//end for 
//		
//		String soluong = String.valueOf(check_soluong);
//		String MTDiep = "";
//		String uuid = UUID.randomUUID().toString().replaceAll("-", "").toUpperCase();
//		MTDiep = SystemParams.MSTTCGP+ commons.convertLocalDateTimeToString(LocalDateTime.now(), "yyyyMMddHHmmssSSS")+ uuid.substring(0, 19);
//		String MST = docTmp.getEmbedded(Arrays.asList("EInvoiceDetail", "NDHDon", "NBan", "MST"), "");
//		//					
//		///TẠO THÔNG ĐIỆP
//		
//		
//		/*TAO XML THONG DIEP GUI DI*/
//		DocumentBuilderFactory dbf1 = DocumentBuilderFactory.newInstance();
//        DocumentBuilder db1 = dbf1.newDocumentBuilder();
//        
//        org.w3c.dom.Document doc1 = db1.newDocument();
//		doc1.setXmlStandalone(true);
//		
//		Element root1 = doc1.createElement("TDiep");
//		doc1.appendChild(root1);
//		
//		Element elementContent1 = doc1.createElement("TTChung");
//		
//		elementContent1.appendChild(commons.createElementWithValue(doc1, "PBan", Constants.TDiep_TTChung_TCTN_VISNAM.PBan));
//		elementContent1.appendChild(commons.createElementWithValue(doc1, "MNGui", SystemParams.MSTTCGP));
//		elementContent1.appendChild(commons.createElementWithValue(doc1, "MNNhan", SystemParams.MSTDVTN));
//		elementContent1.appendChild(commons.createElementWithValue(doc1, "MLTDiep", "200"));
//		elementContent1.appendChild(commons.createElementWithValue(doc1, "MTDiep", MTDiep));
//		elementContent1.appendChild(commons.createElementWithValue(doc1, "MTDTChieu", ""));
//		elementContent1.appendChild(commons.createElementWithValue(doc1, "MST", MST));		//MA SO THUE NGUOI NOP THUE
//		elementContent1.appendChild(commons.createElementWithValue(doc1, "SLuong", soluong));		
//		root1.appendChild(elementContent1);
//		
//		elementContent1 = doc1.createElement("DLieu");			
//		for(org.w3c.dom.Document oo : ListDoc) {
//			Element elem01 = oo.getDocumentElement();
//			Node copiedRoot = doc1.importNode(elem01, true);
//			elementContent1.appendChild(copiedRoot);
//			root1.appendChild(elementContent1);
//		}
//	
//					
//		///
//		boolean boo = false;
//		
//		String data = commons.docW3cToString(doc1);
//		
//		System.out.println(data);
//		ObjectId id_sendCQT = new ObjectId();
//		
//		String fileName_send_thue = id_sendCQT + "_preparingSendCQT.xml";
//		
//		
//		Path path = Paths.get(SystemParams.DIR_E_INVOICE_DATA, MST, mauSoHdon.toString());
//		String pathDir = path.toString();
//		
//		String dir = pathDir;
//		try {
//			boo = commons.docW3cToFile(doc1, dir, fileName_send_thue);
//		} catch (Exception e) {
//		}
//		if (!boo) {
//			responseStatus = new MspResponseStatus(9999, "Lưu tập tin gửi thuế không thành công.");
//			rsp.setResponseStatus(responseStatus);
//			return rsp;
//		}
//			
//		/* CAP NHAT LAI TRANG THAI DANG CHO XU LY */
//		HashMap<String, Object> hItem = null;
//		List<Object> listEInvoiceMTT = new ArrayList<Object>();
//		for(int h = 0; h< ids.size(); h++ ) {
//			_id = ids.get(h) ;	
//			hItem = new LinkedHashMap<String, Object>();
//			hItem.put("_id", _id);
//			listEInvoiceMTT.add(hItem);
//		}
//	
//		
//		/* THEM MOI BAN GHI CHO COLECTION EInvoiceMTTSendCQT */
//
//		
//		Document UpdateEInvoiceMTTSendCQT = new Document("_id", id_sendCQT)
//				.append("IssuerId", header.getIssuerId())
//				.append("MTDiep", MTDiep)
//				.append("SoLuong", soluong)
//				.append("MauSoHD", mauSoHdon)
//				.append("KHHDon", KHHDon)
//				.append("KHMSHDon", KHMSHDon)
//				.append("Dir", dir)
//				.append("EInvoiceStatus", "PENDING")
//				.append("EinvoiceMTT", listEInvoiceMTT)
//						.append("NLap", LocalDateTime.now())	
//				.append("IsActive", false)	
//				.append("IsDelete", false)	
//				.append("InfoCreated",
//						new Document("CreateDate", LocalDateTime.now()).append("CreateUserID", header.getUserId())
//								.append("CreateUserName", header.getUserName())
//								.append("CreateUserFullName", header.getUserFullName()));
//		/* END - LUU DU LIEU HD */
//		MongoClient mongoClient = cfg.mongoClient();
//		MongoCollection<Document> collection = mongoClient.getDatabase(cfg.dbName).getCollection("EInvoiceMTTSendCQT");
//		collection.insertOne(UpdateEInvoiceMTTSendCQT);      
//		mongoClient.close();
//		
//		responseStatus = new MspResponseStatus(0, id_sendCQT.toString());
//		rsp.setResponseStatus(responseStatus);
//		return rsp;
//	}
//		
//	
//	public MsgRsp list_einvoice_mtt_send(JSONRoot jsonRoot) throws Exception {
//		Msg msg = jsonRoot.getMsg();
//		MsgHeader header = msg.getMsgHeader();
//		MsgPage page = msg.getMsgPage();
//		Object objData = msg.getObjData();
//		
//		String date = "";
//		
//		JsonNode jsonData = null;
//		if(objData != null) {
//			jsonData = Json.serializer().nodeFromObject(objData);
//			
//			date = commons.getTextJsonNode(jsonData.at("/Date")).replaceAll("\\s", "");
//			
//		}
//
//		MsgRsp rsp = new MsgRsp(header);
//		MspResponseStatus responseStatus = null;
//		Document docMatch = null;
//		Document docMatch1 = null;
//		Document docMatch2 = null;
//		Document docMatch3 = null;
//
//		Document docTmp = null;
//		Document docTmp1 = null;
//		Document docTmp2 = null;
//		Document docTmp3 = null;
//
//		List<Document> pipeline = new ArrayList<Document>();
//		List<Document> pipeline1 = new ArrayList<Document>();
//		List<Document> pipeline2 = new ArrayList<Document>();
//		List<Document> pipeline3 = new ArrayList<Document>();
//
//		LocalDate date_ = null;
//		
//		
//		int tong_hd = 0;
//		int tong_hople = 0;
//		int tong_k_hople = 0;
//		int tong_chuagui = 0;
//
//		date_ =  "".equals(date) || !commons.checkLocalDate(date, Constants.FORMAT_DATE.FORMAT_DATE_WEB)? null: commons.convertStringToLocalDate(date, Constants.FORMAT_DATE.FORMAT_DATE_WEB);
//	
//		 docMatch = new Document("IssuerId", header.getIssuerId())
//					.append("EInvoiceStatus", new Document("$in", Arrays.asList("PENDING", "PROCESSING", "COMPLETE", "ERROR_CQT")))
//					.append("IsDelete", new Document("$ne", true));
//			
//			if(null != date_)
//				docMatch.append("EInvoiceDetail.TTChung.NLap", date_);
//			
//			
//			pipeline = new ArrayList<Document>();
//			pipeline.add(new Document("$match", docMatch));
//			pipeline.add(
//				new Document("$addFields", 
//					new Document("SHDon", 
//						new Document("$ifNull", Arrays.asList("$EInvoiceDetail.TTChung.SHDon", Integer.MAX_VALUE))
//					)
//				)
//			);
//			pipeline.add(
//				new Document("$sort", 
//					new Document("EInvoiceDetail.TTChung.MauSoHD", -1).append("SHDon", -1).append("_id", -1)
//				)
//			);
//			pipeline.addAll(createFacetForSearchNotSort(page));
//		
//		MongoClient mongoClient = cfg.mongoClient();
//		MongoCollection<Document> collection = mongoClient.getDatabase(cfg.dbName).getCollection("EInvoiceMTT");
//		      try {
//		        docTmp = collection.aggregate(pipeline).allowDiskUse(true).iterator().next();    
//		      } catch (Exception e) {
//		        
//		      }
//		        
//		mongoClient.close();
//		      
//		
//		if(null == docTmp) {		
//			
//			responseStatus = new MspResponseStatus(9999, date);		 			 		 
//			rsp.setResponseStatus(responseStatus);		
//			return rsp;
//		}
//		
//		
//		
//		/////////////// LAY DANH SACH HOA DON DA GUI
//		 docMatch1 = new Document("IssuerId", header.getIssuerId())
//					.append("EInvoiceStatus", new Document("$in", Arrays.asList("PROCESSING", "COMPLETE")))
//					.append("IsDelete", new Document("$ne", true))
//					.append("EInvoiceDetail.TTChung.NLap", date_);
//						
//			pipeline1 = new ArrayList<Document>();
//			pipeline1.add(new Document("$match", docMatch1));
//			pipeline1.addAll(createFacetForSearchNotSort(page));
//		
//		mongoClient = cfg.mongoClient();
//		collection = mongoClient.getDatabase(cfg.dbName).getCollection("EInvoiceMTT");
//		  try {
//			  docTmp1 = collection.aggregate(pipeline1).allowDiskUse(true).iterator().next();      
//			 } catch (Exception ex) {
//				
//			}
//		mongoClient.close();
//		
//		if(null == docTmp1) {
//			tong_hople = 0;
//		}else {
//			tong_hople = docTmp1.getInteger("total", 0);
//		}
//		
//		
//		// DANH SACH HOA DON CHUA GUI THUE
//		 docMatch2 = new Document("IssuerId", header.getIssuerId())
//					.append("EInvoiceStatus","PENDING")
//					.append("IsDelete", new Document("$ne", true))
//					.append("EInvoiceDetail.TTChung.NLap", date_);
//			
//			pipeline2 = new ArrayList<Document>();
//			pipeline2.add(new Document("$match", docMatch2));
//			pipeline2.addAll(createFacetForSearchNotSort(page));
//		
//		
//		mongoClient = cfg.mongoClient();
//		collection = mongoClient.getDatabase(cfg.dbName).getCollection("EInvoiceMTT");
//		  try {
//			  docTmp2 = collection.aggregate(pipeline2).allowDiskUse(true).iterator().next();      
//			 } catch (Exception ex) {
//				
//			}
//		mongoClient.close();
//		
//		if(null == docTmp2) {
//			tong_chuagui = 0;
//		}else {
//			tong_chuagui = docTmp2.getInteger("total", 0);
//		}
//				
//		// DANH SACH HOA DON LOI 
//		 docMatch3 = new Document("IssuerId", header.getIssuerId())
//					.append("EInvoiceStatus","ERROR_CQT")
//					.append("IsDelete", new Document("$ne", true))
//					.append("EInvoiceDetail.TTChung.NLap", date_);
//		
//			pipeline3 = new ArrayList<Document>();
//			pipeline3.add(new Document("$match", docMatch3));			
//			pipeline3.addAll(createFacetForSearchNotSort(page));
//
//		mongoClient = cfg.mongoClient();
//		collection = mongoClient.getDatabase(cfg.dbName).getCollection("EInvoiceMTT");
//		  try {
//			  docTmp3 = collection.aggregate(pipeline3).allowDiskUse(true).iterator().next();      
//			 } catch (Exception ex) {
//				
//			}
//		mongoClient.close();
//		
//		if(null == docTmp3) {
//			tong_k_hople = 0;
//		}else {
//			tong_k_hople = docTmp3.getInteger("total", 0);
//		}	
//		//////////
//		rsp = new MsgRsp(header);
//		
//		//LAY TONG HOA DON	
//		page.setTotalRows(docTmp.getInteger("total", 0));
//		rsp.setMsgPage(page);
//		
//		tong_hd = docTmp.getInteger("total", 0);
//		
//		responseStatus = null;
//		
//
//		String hd = String.valueOf(tong_hd);
//		String hople = String.valueOf(tong_hople);
//		String khonghople = String.valueOf(tong_k_hople);
//		String chuagui = String.valueOf(tong_chuagui);
//
//		responseStatus = new MspResponseStatus(0, date+"," +hd+ ","+ hople + ","+ khonghople + ","+ chuagui);
//		rsp.setResponseStatus(responseStatus);	
//		return rsp;
//	}
//	
//	public MsgRsp einvoice_mtt_list_sendAll(JSONRoot jsonRoot) throws Exception {
//		Msg msg = jsonRoot.getMsg();
//		MsgHeader header = msg.getMsgHeader();
//		Object objData = msg.getObjData();
//		
//		String date = "";
//		
//		JsonNode jsonData = null;
//		if(objData != null) {
//			jsonData = Json.serializer().nodeFromObject(objData);
//			
//			date = commons.getTextJsonNode(jsonData.at("/Date")).replaceAll("\\s", "");
//			
//		}
//
//		MsgRsp rsp = new MsgRsp(header);
//		MspResponseStatus responseStatus = null;
//		Document docMatch = null;
//
//		Document docTmp = null;
//
//		Iterator<Document> iter = null;
//		List<Document> pipeline = new ArrayList<Document>();
//
////		org.w3c.dom.Document rTCTN = null;
////		String codeTTTNhan = "";
////		String descTTTNhan="";
//		int check_soluong = 0;
//		LocalDate date_ = null;
//
//		org.w3c.dom.Document doc = null;
//		String mauSoHdon = "";
//		String KHHDon = "";
//		String KHMSHDon = "";
//		
//		List<org.w3c.dom.Document> ListDoc = new ArrayList<>();
//
//		date_ =  "".equals(date) || !commons.checkLocalDate(date, Constants.FORMAT_DATE.FORMAT_DATE_WEB)? null: commons.convertStringToLocalDate(date, Constants.FORMAT_DATE.FORMAT_DATE_WEB);
//	
//		 docMatch = new Document("IssuerId", header.getIssuerId())
//					.append("EInvoiceStatus", "PENDING")
//					.append("IsDelete", new Document("$ne", true))
//					.append("EInvoiceDetail.TTChung.NLap", date_);
//			
//			
//			pipeline = new ArrayList<Document>();
//			pipeline.add(new Document("$match", docMatch));
//			pipeline.add(
//				new Document("$addFields", 
//					new Document("SHDon", 
//						new Document("$ifNull", Arrays.asList("$EInvoiceDetail.TTChung.SHDon", Integer.MAX_VALUE))
//					)
//				)
//			);
//			pipeline.add(
//				new Document("$sort", 
//					new Document("EInvoiceDetail.TTChung.MauSoHD", -1).append("SHDon", -1).append("_id", -1)
//				)
//			);
//		
//		MongoClient mongoClient = cfg.mongoClient();
//		MongoCollection<Document> collection = mongoClient.getDatabase(cfg.dbName).getCollection("EInvoiceMTT");
//		iter =  collection.aggregate(pipeline).allowDiskUse(true).iterator();
//		mongoClient.close();
//		pipeline.clear();
//		
//		HashMap<String, Object> hItem = null;
//		List<Object> listEInvoiceMTT = new ArrayList<Object>();
//		
//		while(iter.hasNext()) {
//			docTmp = iter.next();
//		
//			String _id = docTmp.getObjectId("_id").toString();
//			
//			File file = null;
//			String dir = docTmp.get("Dir", "");
//						
//			String fileName = "";
//			String check_file_signed = docTmp.get("SignStatusCode", "NOSIGN");
//			mauSoHdon = docTmp.getEmbedded(Arrays.asList("EInvoiceDetail","TTChung", "MauSoHD"), "");	
//			KHHDon = docTmp.getEmbedded(Arrays.asList("EInvoiceDetail","TTChung", "KHHDon"), "");
//			KHMSHDon = docTmp.getEmbedded(Arrays.asList("EInvoiceDetail","TTChung", "KHMSHDon"), "");
//			
//			if(check_file_signed.equals("SIGNED")) {
//				fileName = _id + "_signed.xml";
//			}else {
//				fileName = _id + ".xml";
//			}
//
//			file = new File(dir, fileName);
//			if (!file.exists() || !file.isFile()) {
//				continue;
//			}
//
//			doc = commons.fileToDocument(file, true);
//			if (null == doc) {
//				continue;
//			}
//
//			ListDoc.add(doc);	
//			check_soluong++;
//			
//			hItem = new LinkedHashMap<String, Object>();
//			hItem.put("_id", _id);
//			listEInvoiceMTT.add(hItem);
//		}
//				
//		
//		String soluong = String.valueOf(check_soluong);
//		String MTDiep = "";
//		String uuid = UUID.randomUUID().toString().replaceAll("-", "").toUpperCase();
//		MTDiep = SystemParams.MSTTCGP+ commons.convertLocalDateTimeToString(LocalDateTime.now(), "yyyyMMddHHmmssSSS")+ uuid.substring(0, 19);
//		String MST = docTmp.getEmbedded(Arrays.asList("EInvoiceDetail", "NDHDon", "NBan", "MST"), "");
//		//					
//		///TẠO THÔNG ĐIỆP
//		
//		
//		/*TAO XML THONG DIEP GUI DI*/
//		DocumentBuilderFactory dbf1 = DocumentBuilderFactory.newInstance();
//        DocumentBuilder db1 = dbf1.newDocumentBuilder();
//        
//        org.w3c.dom.Document doc1 = db1.newDocument();
//		doc1.setXmlStandalone(true);
//		
//		Element root1 = doc1.createElement("TDiep");
//		doc1.appendChild(root1);
//		
//		Element elementContent1 = doc1.createElement("TTChung");
//		
//		elementContent1.appendChild(commons.createElementWithValue(doc1, "PBan", Constants.TDiep_TTChung_TCTN_VISNAM.PBan));
//		elementContent1.appendChild(commons.createElementWithValue(doc1, "MNGui", SystemParams.MSTTCGP));
//		elementContent1.appendChild(commons.createElementWithValue(doc1, "MNNhan", SystemParams.MSTDVTN));
//		elementContent1.appendChild(commons.createElementWithValue(doc1, "MLTDiep", "200"));
//		elementContent1.appendChild(commons.createElementWithValue(doc1, "MTDiep", MTDiep));
//		elementContent1.appendChild(commons.createElementWithValue(doc1, "MTDTChieu", ""));
//		elementContent1.appendChild(commons.createElementWithValue(doc1, "MST", MST));		//MA SO THUE NGUOI NOP THUE
//		elementContent1.appendChild(commons.createElementWithValue(doc1, "SLuong", soluong));		
//		root1.appendChild(elementContent1);
//		
//		elementContent1 = doc1.createElement("DLieu");			
//		for(org.w3c.dom.Document oo : ListDoc) {
//			Element elem01 = oo.getDocumentElement();
//			Node copiedRoot = doc1.importNode(elem01, true);
//			elementContent1.appendChild(copiedRoot);
//			root1.appendChild(elementContent1);
//		}
//			
//		
//	///
//	boolean boo = false;
//	
//	String data = commons.docW3cToString(doc1);
//	
//	System.out.println(data);
//	ObjectId id_sendCQT = new ObjectId();
//	
//	String fileName_send_thue = id_sendCQT + "_preparingSendCQT.xml";
//	
//	
//	Path path = Paths.get(SystemParams.DIR_E_INVOICE_DATA, MST, mauSoHdon.toString());
//	String pathDir = path.toString();
//	
//	String dir = pathDir;
//	try {
//		boo = commons.docW3cToFile(doc1, dir, fileName_send_thue);
//	} catch (Exception e) {
//	}
//	if (!boo) {
//		responseStatus = new MspResponseStatus(9999, "Lưu tập tin gửi thuế không thành công.");
//		rsp.setResponseStatus(responseStatus);
//		return rsp;
//	}
//		
//	/* CAP NHAT LAI TRANG THAI DANG CHO XU LY */
//
//
//	
//	/* THEM MOI BAN GHI CHO COLECTION EInvoiceMTTSendCQT */
//
//	
//	Document UpdateEInvoiceMTTSendCQT = new Document("_id", id_sendCQT)
//			.append("IssuerId", header.getIssuerId())
//			.append("MTDiep", MTDiep)
//			.append("SoLuong", soluong)
//			.append("MauSoHD", mauSoHdon)
//			.append("KHHDon", KHHDon)
//			.append("KHMSHDon", KHMSHDon)
//			.append("Dir", dir)
//			.append("EInvoiceStatus", "PENDING")
//			.append("EinvoiceMTT", listEInvoiceMTT)
//					.append("NLap", LocalDateTime.now())	
//			.append("IsActive", false)	
//			.append("IsDelete", false)	
//			.append("InfoCreated",
//					new Document("CreateDate", LocalDateTime.now()).append("CreateUserID", header.getUserId())
//							.append("CreateUserName", header.getUserName())
//							.append("CreateUserFullName", header.getUserFullName()));
//	/* END - LUU DU LIEU HD */			
//
//	mongoClient = cfg.mongoClient();
//	collection = mongoClient.getDatabase(cfg.dbName).getCollection("EInvoiceMTTSendCQT");
//	collection.insertOne(UpdateEInvoiceMTTSendCQT);      
//	mongoClient.close();
//	
//	responseStatus = new MspResponseStatus(0,  id_sendCQT.toString());
//	rsp.setResponseStatus(responseStatus);
//	return rsp;
//	
//	}
//
//	
//	

//	public FileInfo getFileForSignMTT(JSONRoot jsonRoot) throws Exception {
//		FileInfo fileInfo = new FileInfo();
//
//		Msg msg = jsonRoot.getMsg();
//		MsgHeader header = msg.getMsgHeader();
//		Object objData = msg.getObjData();
//
//		JsonNode jsonData = null;
//		if (objData != null) {
//			jsonData = Json.serializer().nodeFromObject(msg.getObjData());
//		} else {
//			throw new Exception("Lỗi dữ liệu đầu vào");
//		}
//
//		String _id = commons.getTextJsonNode(jsonData.at("/_id")).replaceAll("\\s", "");
//
////		int currentYear = LocalDate.now().get(ChronoField.YEAR);
//		ObjectId objectId = null;
//		try {
//			objectId = new ObjectId(_id);
//		} catch (Exception e) {
//		}
//
//		/* NHO KIEM TRA XEM CO HD NAO DANG KY KHONG */
//
//		Document docFind = new Document("IssuerId", header.getIssuerId()).append("IsDelete", new Document("$ne", true))
//				.append("_id", objectId)
//				.append("EInvoiceStatus", new Document("$in", Arrays.asList("CREATED", "PENDING")))
//				.append("SignStatusCode", new Document("$in", Arrays.asList("NOSIGN", "PROCESSING")));
//
//		List<Document> pipeline = new ArrayList<Document>();
//		pipeline.add(new Document("$match", docFind));
//		Document docTmp = null;
//		
//		MongoClient mongoClient = cfg.mongoClient();
//		MongoCollection<Document> collection = mongoClient.getDatabase(cfg.dbName).getCollection("EInvoiceMTT");
//		      try {
//		        docTmp = collection.aggregate(pipeline).allowDiskUse(true).iterator().next();    
//		      } catch (Exception e) {
//		        
//		      }
//		        
//		mongoClient.close();
//		
//		if (null == docTmp) {
//			return fileInfo;
//		}
//		
//		
//		/* AP DUNG 1 FILE TRUOC */
//		String dir = docTmp.get("Dir", "");
//		String fileName = _id + "_preparingSendCQT.xml";
//		File file = new File(dir, fileName);
//		
//		if (!file.exists())
//			return fileInfo;
//		
//		/* DOC DU LIEU XML, VA GHI DU LIEU VO SO HD */
//		org.w3c.dom.Document doc = commons.fileToDocument(file);	
//		fileInfo.setFileName(fileName);
//		fileInfo.setContentFile(commons.docW3cToByte(doc));
//				
//		return fileInfo;
//	}
//		
//		
//	@Override
//	public MsgRsp signSingleAndSendCQTMTT(InputStream is, JSONRoot jsonRoot) throws Exception {
//		Msg msg = jsonRoot.getMsg();
//		MsgHeader header = msg.getMsgHeader();
//		MsgPage page = msg.getMsgPage();
//		Object objData = msg.getObjData();
//	
//		JsonNode jsonData = null;
//		if (objData != null) {
//			jsonData = Json.serializer().nodeFromObject(msg.getObjData());
//		} else {
//			throw new Exception("Lỗi dữ liệu đầu vào");
//		}
//
//		String _id = commons.getTextJsonNode(jsonData.at("/_id")).replaceAll("\\s", "");
//		
//		MsgRsp rsp = new MsgRsp(header);
//		rsp.setMsgPage(page);
//		MspResponseStatus responseStatus = null;
//
//		/* DOC NOI DUNG XML DA KY */
//		org.w3c.dom.Document xmlDoc = commons.inputStreamToDocument(is, false);
//
//		ObjectId objectId = null;
//		try {
//			objectId = new ObjectId(_id);
//		} catch (Exception e) {
//		}
//		List<Document> pipeline = null;
//		/* KIEM TRA THONG TIN HOP LE KHONG */
//		Document docFind = new Document("IssuerId", header.getIssuerId()).append("IsDelete", new Document("$ne", true))
//				.append("EInvoiceStatus", "PENDING")
//				.append("_id", objectId);
//		pipeline = new ArrayList<Document>();
//		pipeline.add(new Document("$match", docFind));
//		Document docTmp = null;		
//		
//		MongoClient mongoClient = cfg.mongoClient();
//		MongoCollection<Document> collection = mongoClient.getDatabase(cfg.dbName).getCollection("EInvoiceMTT");
//		      try {
//		        docTmp = collection.aggregate(pipeline).allowDiskUse(true).iterator().next();    
//		      } catch (Exception e) {
//		        
//		      }
//		        
//		mongoClient.close();
//
//		if (null == docTmp) {
//			responseStatus = new MspResponseStatus(9999, "Không tìm thấy thông tin hóa đơn.");
//			rsp.setResponseStatus(responseStatus);
//			return rsp;
//		}
//
//		/* LUU FILE VA CAP NHAT TRANG THAI */
//		String dir = docTmp.get("Dir", "");
////		String fileName = keySystem + "_sendCQT.xml";
//		String fileName = _id + "_sendCQT.xml";
//		boolean check = commons.docW3cToFile(xmlDoc, dir, fileName);
//		if (!check) {
//			responseStatus = new MspResponseStatus(9999, "Lưu tập tin đã ký không thành công.");
//			rsp.setResponseStatus(responseStatus);
//			return rsp;
//		}
//
//		/* GỬI CƠ QUAN THUẾ */
//		
//		String MTDiep = "";
//		MTDiep = docTmp.get("MTDiep", "");
//		String MTDiep1 = "";
//
//		String codeTTTNhan = "";
//		String descTTTNhan="";
//		org.w3c.dom.Document rTCTN = null;
//		HashMap<String, Object> hItem = null;
//		FindOneAndUpdateOptions options = null;
//	//	List<org.w3c.dom.Document> ListDoc = new ArrayList<>();
//		
//		//LAY XML VA MST GUI THUE
//		
//		//	ListDoc.add(xmlDoc);
//		
//			String MST = docTmp.getEmbedded(Arrays.asList("EInvoiceDetail", "NDHDon", "NBan", "MST"), "");
//			String mauSoHdon = docTmp.getEmbedded(Arrays.asList("EInvoiceDetail","TTChung", "MauSoHD"), "");
//			String KHMSHDon_ = docTmp.getEmbedded(Arrays.asList("EInvoiceDetail","TTChung", "KHMSHDon"), "");
//			String KHHDon_ = docTmp.getEmbedded(Arrays.asList("EInvoiceDetail","TTChung", "KHHDon"), "");
//		
//					try {
//						rTCTN = tctnService.callTiepNhanListThongDiep("200", MTDiep, MST, "1", xmlDoc);	
//					} catch (Exception e) {
//
//					}
//					
//					if(rTCTN == null) {
//						MTDiep1 = docTmp.get("MTDiep", "");
//						rTCTN = tctnService.callTiepNhanListThongDiep("200", MTDiep1, MST, "1", xmlDoc);	
//					}
//					XPath xPath = XPathFactory.newInstance().newXPath();
//					Node nodeDLHDon = (Node) xPath.evaluate("/TDiep", rTCTN, XPathConstants.NODE);
//					 codeTTTNhan = commons.getTextFromNodeXML((Element) xPath.evaluate("DLieu/TBao/TTTNhan", nodeDLHDon, XPathConstants.NODE));
//					 descTTTNhan = commons.getTextFromNodeXML((Element) xPath.evaluate("DLieu/TBao/DSLDo/LDo/MTa", nodeDLHDon, XPathConstants.NODE));				 
//		
//		switch (codeTTTNhan) {
//		case "1":
//			responseStatus = new MspResponseStatus(9999,
//					"".equals(descTTTNhan) ? "Không tìm thấy tenant dữ liệu." : descTTTNhan);
//			rsp.setResponseStatus(responseStatus);
//			return rsp;
//		case "2":
//			responseStatus = new MspResponseStatus(9999, "Mã thông điệp đã tồn tại.");
//			rsp.setResponseStatus(responseStatus);
//			return rsp;
//		case "3":
//			responseStatus = new MspResponseStatus(9999,
//					"".equals(descTTTNhan) ? "Thất bại, lỗi Exception." : descTTTNhan);
//			rsp.setResponseStatus(responseStatus);
//			return rsp;
//		default:
//			
//			break;
//		}
//		/* CAP NHAT LAI TRANG THAI DANG CHO XU LY */
//	
//		options = new FindOneAndUpdateOptions();
//		options.upsert(false);
//		options.maxTime(5000, TimeUnit.MILLISECONDS);
//		options.returnDocument(ReturnDocument.AFTER);
//		
//		mongoClient = cfg.mongoClient();
//		collection = mongoClient.getDatabase(cfg.dbName).getCollection("EInvoiceMTT");
//		collection.findOneAndUpdate(docFind,
//				new Document("$set",
//						new Document("EInvoiceStatus", Constants.INVOICE_STATUS.PROCESSING)
//						.append("SendCQTStatus", true)
//						.append("SendCQT_Date", LocalDateTime.now())
//						.append("InfoSendCQT",
//								new Document("Date", LocalDateTime.now()).append("UserID", header.getUserId())
//										.append("UserName", header.getUserName())
//										.append("UserFullName", header.getUserFullName()))),
//				options);
//		  mongoClient.close();
//
//		
//		
//		/* THEM MOI BAN GHI CHO COLECTION EInvoiceMTTSendCQT */
//		List<Object> listEInvoiceMTT = new ArrayList<Object>();
//		hItem = new LinkedHashMap<String, Object>();
//		hItem.put("_id", _id);
//		listEInvoiceMTT.add(hItem);
//		
//		ObjectId idEInvoiceMTTSendCQT = new ObjectId();
//		Document UpdateEInvoiceMTTSendCQT = new Document("_id", idEInvoiceMTTSendCQT)
//				.append("IssuerId", header.getIssuerId())
//				.append("MTDiep", MTDiep)
//				.append("SoLuong", "1")
//				.append("MauSoHD", mauSoHdon)
//				.append("KHHDon", KHHDon_)
//				.append("KHMSHDon", KHMSHDon_)
//				.append("EInvoiceStatus", Constants.INVOICE_STATUS.PROCESSING)
//				.append("EinvoiceMTT", listEInvoiceMTT)
//						.append("NLap", LocalDateTime.now())			
//				.append("IsDelete", false)	
//				.append("IsActive", true)	
//				.append("InfoCreated",
//						new Document("CreateDate", LocalDateTime.now()).append("CreateUserID", header.getUserId())
//								.append("CreateUserName", header.getUserName())
//								.append("CreateUserFullName", header.getUserFullName()));
//		/* END - LUU DU LIEU HD */
//
//		mongoClient = cfg.mongoClient();
//		collection = mongoClient.getDatabase(cfg.dbName).getCollection("EInvoiceMTTSendCQT");
//		collection.insertOne(UpdateEInvoiceMTTSendCQT);      
//		mongoClient.close();
//
//		responseStatus = new MspResponseStatus(0, Constants.MAP_ERROR.get(0));
//		rsp.setResponseStatus(responseStatus);
//		return rsp;
//	}
//	
//	
//	public FileInfo getFileForSignALLMTT(JSONRoot jsonRoot) throws Exception {
//		FileInfo fileInfo = new FileInfo();
//
//		Msg msg = jsonRoot.getMsg();
//		MsgHeader header = msg.getMsgHeader();
//		Object objData = msg.getObjData();
//
//		JsonNode jsonData = null;
//		if (objData != null) {
//			jsonData = Json.serializer().nodeFromObject(msg.getObjData());
//		} else {
//			throw new Exception("Lỗi dữ liệu đầu vào");
//		}
//
//		String _id = commons.getTextJsonNode(jsonData.at("/_id")).replaceAll("\\s", "");
//
////		int currentYear = LocalDate.now().get(ChronoField.YEAR);
//		ObjectId objectId = null;
//		try {
//			objectId = new ObjectId(_id);
//		} catch (Exception e) {
//		}
//
//		/* NHO KIEM TRA XEM CO HD NAO DANG KY KHONG */
//
//		Document docFind = new Document("IssuerId", header.getIssuerId()).append("IsDelete", new Document("$ne", true))
//				.append("_id", objectId)
//				.append("EInvoiceStatus", "PENDING");
//
//		List<Document> pipeline = new ArrayList<Document>();
//		pipeline.add(new Document("$match", docFind));
//		Document docTmp = null;
//
//		MongoClient mongoClient = cfg.mongoClient();
//		MongoCollection<Document> collection = mongoClient.getDatabase(cfg.dbName).getCollection("EInvoiceMTTSendCQT");
//		      try {
//		        docTmp = collection.aggregate(pipeline).allowDiskUse(true).iterator().next();    
//		      } catch (Exception e) {
//		        
//		      }
//		        
//		mongoClient.close();
//		
//		if (null == docTmp) {
//			return fileInfo;
//		}
//		
//		
//		/* AP DUNG 1 FILE TRUOC */
//		String dir = docTmp.get("Dir", "");
//		String fileName = _id + "_preparingSendCQT.xml";
//		File file = new File(dir, fileName);
//		
//		if (!file.exists())
//			return fileInfo;
//		
//		/* DOC DU LIEU XML, VA GHI DU LIEU VO SO HD */
//		org.w3c.dom.Document doc = commons.fileToDocument(file);	
//		fileInfo.setFileName(fileName);
//		fileInfo.setContentFile(commons.docW3cToByte(doc));
//				
//		return fileInfo;
//	}
//		
//		
//	@Override
//	public MsgRsp signSingleAndSendALLCQTMTT(InputStream is, JSONRoot jsonRoot) throws Exception {
//		Msg msg = jsonRoot.getMsg();
//		MsgHeader header = msg.getMsgHeader();
//		MsgPage page = msg.getMsgPage();
//		Object objData = msg.getObjData();
//	
//		JsonNode jsonData = null;
//		if (objData != null) {
//			jsonData = Json.serializer().nodeFromObject(msg.getObjData());
//		} else {
//			throw new Exception("Lỗi dữ liệu đầu vào");
//		}
//
//		String _id = commons.getTextJsonNode(jsonData.at("/_id")).replaceAll("\\s", "");
//		
//		MsgRsp rsp = new MsgRsp(header);
//		rsp.setMsgPage(page);
//		MspResponseStatus responseStatus = null;
//
//		/* DOC NOI DUNG XML DA KY */
//		org.w3c.dom.Document xmlDoc = commons.inputStreamToDocument(is, false);
//
//		ObjectId objectId = null;
//		try {
//			objectId = new ObjectId(_id);
//		} catch (Exception e) {
//		}
//		List<Document> pipeline = null;
//		/* KIEM TRA THONG TIN HOP LE KHONG */
//		Document docFind = new Document("IssuerId", header.getIssuerId()).append("IsDelete", new Document("$ne", true))
//	//			.append("EInvoiceStatus", "PENDING")
//				.append("_id", objectId);
//		pipeline = new ArrayList<Document>();
//		pipeline.add(new Document("$match", docFind));
//		Document docTmp = null;		
//		
//		MongoClient mongoClient = cfg.mongoClient();
//		MongoCollection<Document> collection = mongoClient.getDatabase(cfg.dbName).getCollection("EInvoiceMTTSendCQT");
//		      try {
//		        docTmp = collection.aggregate(pipeline).allowDiskUse(true).iterator().next();    
//		      } catch (Exception e) {
//		        
//		      }
//		        
//		mongoClient.close();
//
//		if (null == docTmp) {
//			responseStatus = new MspResponseStatus(9999, "Không tìm thấy thông tin hóa đơn.");
//			rsp.setResponseStatus(responseStatus);
//			return rsp;
//		}
//
//		/* LUU FILE VA CAP NHAT TRANG THAI */
//		String dir = docTmp.get("Dir", "");
////		String fileName = keySystem + "_sendCQT.xml";
//		String fileName = _id + "_sendCQT.xml";
//		boolean check = commons.docW3cToFile(xmlDoc, dir, fileName);
//		if (!check) {
//			responseStatus = new MspResponseStatus(9999, "Lưu tập tin đã ký không thành công.");
//			rsp.setResponseStatus(responseStatus);
//			return rsp;
//		}
//
//		/* GỬI CƠ QUAN THUẾ */
//		
//		String MTDiep = "";
//		MTDiep = docTmp.get("MTDiep", "");
//		String MTDiep1 = "";
//
//		String codeTTTNhan = "";
//		String descTTTNhan="";
//		org.w3c.dom.Document rTCTN = null;
//		FindOneAndUpdateOptions options = null;
//	//	List<org.w3c.dom.Document> ListDoc = new ArrayList<>();
//		
//		//LAY XML VA MST GUI THUE
//		
//		//	ListDoc.add(xmlDoc);
//			
//			String MST = docTmp.get("MST", "");
////			String mauSoHdon = docTmp.get("MauSoHD", "");
////			String KHMSHDon_ = docTmp.get("KHMSHDon", "");
////			String KHHDon_ = docTmp.get("KHHDon", "");
//			String SoLuong = docTmp.get("SoLuong", "");
//			
//					try {
//						rTCTN = tctnService.callTiepNhanListThongDiep("200", MTDiep, MST, SoLuong, xmlDoc);	
//					} catch (Exception e) {
//
//					}
//					
//					if(rTCTN == null) {
//						MTDiep1 = docTmp.get("MTDiep", "");
//						rTCTN = tctnService.callTiepNhanListThongDiep("200", MTDiep1, MST, SoLuong, xmlDoc);	
//					}
//					XPath xPath = XPathFactory.newInstance().newXPath();
//					Node nodeDLHDon = (Node) xPath.evaluate("/TDiep", rTCTN, XPathConstants.NODE);
//					 codeTTTNhan = commons.getTextFromNodeXML((Element) xPath.evaluate("DLieu/TBao/TTTNhan", nodeDLHDon, XPathConstants.NODE));
//					 descTTTNhan = commons.getTextFromNodeXML((Element) xPath.evaluate("DLieu/TBao/DSLDo/LDo/MTa", nodeDLHDon, XPathConstants.NODE));				 
//		
//		switch (codeTTTNhan) {
//		case "1":
//			responseStatus = new MspResponseStatus(9999,
//					"".equals(descTTTNhan) ? "Không tìm thấy tenant dữ liệu." : descTTTNhan);
//			rsp.setResponseStatus(responseStatus);
//			return rsp;
//		case "2":
//			responseStatus = new MspResponseStatus(9999, "Mã thông điệp đã tồn tại.");
//			rsp.setResponseStatus(responseStatus);
//			return rsp;
//		case "3":
//			responseStatus = new MspResponseStatus(9999,
//					"".equals(descTTTNhan) ? "Thất bại, lỗi Exception." : descTTTNhan);
//			rsp.setResponseStatus(responseStatus);
//			return rsp;
//		default:
//			
//			break;
//		}
//		/* CAP NHAT LAI TRANG THAI DANG CHO XU LY EINVOICEMTT*/
//		for(Document oo: docTmp.getList("EinvoiceMTT", Document.class)) {
//			String _idEInvoiceMTT = oo.get("_id", "");
//			ObjectId idEInvoiceMTT = new ObjectId(_idEInvoiceMTT);
//			
//			Document findEInvoiceMTT = new Document("_id",idEInvoiceMTT)
//					.append("IsDelete", new Document("$ne", true));
//		
//			options = new FindOneAndUpdateOptions();
//			options.upsert(false);
//			options.maxTime(5000, TimeUnit.MILLISECONDS);
//			options.returnDocument(ReturnDocument.AFTER);
//			
//			mongoClient = cfg.mongoClient();
//			collection = mongoClient.getDatabase(cfg.dbName).getCollection("EInvoiceMTT");
//			collection.findOneAndUpdate(findEInvoiceMTT,
//					new Document("$set",
//							new Document("EInvoiceStatus", Constants.INVOICE_STATUS.PROCESSING)
//							.append("SendCQTStatus", true)
//							.append("SendCQT_Date", LocalDateTime.now())
//							.append("InfoSendCQT",
//									new Document("Date", LocalDateTime.now()).append("UserID", header.getUserId())
//											.append("UserName", header.getUserName())
//											.append("UserFullName", header.getUserFullName()))),
//					options);
//			mongoClient.close();
//		}
//		
//
//				
//		
//		/* CAP NHAT TRANG THAI DANG CHO XU LY EInvoiceMTTSendCQT */
//		
//		
//		options = new FindOneAndUpdateOptions();
//		options.upsert(false);
//		options.maxTime(5000, TimeUnit.MILLISECONDS);
//		options.returnDocument(ReturnDocument.AFTER);
//		
//		mongoClient = cfg.mongoClient();
//		collection = mongoClient.getDatabase(cfg.dbName).getCollection("EInvoiceMTTSendCQT");
//		collection.findOneAndUpdate(docFind,
//				new Document("$set",
//						new Document("EInvoiceStatus", Constants.INVOICE_STATUS.PROCESSING)
//						.append("SendCQTStatus", true)
//						.append("IsActive", true)
//						.append("SendCQT_Date", LocalDateTime.now())
//						.append("InfoSendCQT",
//								new Document("Date", LocalDateTime.now()).append("UserID", header.getUserId())
//										.append("UserName", header.getUserName())
//										.append("UserFullName", header.getUserFullName()))),
//				options);
//		mongoClient.close();
//		
//
//		responseStatus = new MspResponseStatus(0, Constants.MAP_ERROR.get(0));
//		rsp.setResponseStatus(responseStatus);
//		return rsp;
//	}
//	

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
		try {
			objectId = new ObjectId(_id);
		} catch (Exception e) {
		}

		Document docFind = null;
		Document docTmp = null;
		List<Document> pipeline = null;

		try {

			Document fillter = new Document("_id", 1).append("Dir", 1).append("IssuerId", 1).append("FileNameXML", 1)
					.append("EInvoiceStatus", 1).append("SignStatusCode", 1).append("MCCQT", 1).append("HDSS", 1)
					.append("MTDiep", 1).append("EInvoiceDetail", 1).append("SecureKey", 1);

			docFind = new Document("IssuerId", header.getIssuerId())
					.append("MCCQT", new Document("$exists", true).append("$ne", null)).append("_id", objectId);
			pipeline = new ArrayList<Document>();
			pipeline.add(new Document("$match", docFind));
			pipeline.add(new Document("$project", fillter));

			pipeline.add(
					new Document("$lookup",
							new Document("from", "ConfigEmail").append("let", new Document("vIssuerId", "$IssuerId"))
									.append("pipeline", Arrays.asList(new Document("$match",
											new Document("$expr",
													new Document("$eq", Arrays.asList("$IssuerId", "$$vIssuerId"))))))
									.append("as", "ConfigEmail")));
			pipeline.add(new Document("$unwind",
					new Document("path", "$ConfigEmail").append("preserveNullAndEmptyArrays", true)));

			pipeline.add(new Document("$lookup", new Document("from", "DMMauSoKyHieu")
					.append("let",
							new Document("vIssuerId", "$IssuerId").append("vMauSoHD",
									"$EInvoiceDetail.TTChung.MauSoHD"))
					.append("pipeline", Arrays.asList(
							new Document("$match", new Document("$expr", new Document("$and",
									Arrays.asList(new Document("$eq", Arrays.asList("$$vIssuerId", "$IssuerId")),
											new Document("$eq",
													Arrays.asList(new Document("$toString", "$_id"), "$$vMauSoHD")))))),
							new Document("$project",
									new Document("_id", 1).append("Templates", 1).append("Status", 1).append("SHDHT", 1)
											.append("SoLuong", 1).append("ConLai", 1))))
					.append("as", "DMMauSoKyHieu")));
			pipeline.add(new Document("$unwind",
					new Document("path", "$DMMauSoKyHieu").append("preserveNullAndEmptyArrays", true)));
			pipeline.add(new Document("$lookup",
					new Document("from", "UserConFig")
							.append("pipeline",
									Arrays.asList(new Document("$match",
											new Document("viewshd", "Y").append("IssuerId", header.getIssuerId()))))
							.append("as", "UserConFig")));
			pipeline.add(new Document("$unwind",
					new Document("path", "$UserConFig").append("preserveNullAndEmptyArrays", true)));

			pipeline.add(new Document("$lookup",
					new Document("from", "ConfigMailJet")
							.append("pipeline", Arrays.asList(new Document("$match", new Document("IsActive", true))))
							.append("as", "ConfigMailJet")));
			pipeline.add(new Document("$unwind",
					new Document("path", "$ConfigMailJet").append("preserveNullAndEmptyArrays", true)));

			pipeline.add(new Document("$lookup", new Document("from", "DMFooterWeb").append("pipeline",
					Arrays.asList(new Document("$match", new Document("IsActive", true).append("IsDelete", false)),
							new Document("$project", new Document("Noidung", 1)), new Document("$limit", 1)))
					.append("as", "DMFooterWeb")));
			pipeline.add(new Document("$unwind",
					new Document("path", "$DMFooterWeb").append("preserveNullAndEmptyArrays", true)));
			pipeline.add(new Document("$lookup",
					new Document("from", "PramLink")
							.append("pipeline",
									Arrays.asList(
											new Document("$match",
													new Document("$expr", new Document("IsDelete", false))),
											new Document("$project", new Document("_id", 1).append("LinkPortal", 1))))
							.append("as", "PramLink")));
			pipeline.add(new Document("$unwind",
					new Document("path", "$PramLink").append("preserveNullAndEmptyArrays", true)));

			MongoClient mongoClient = cfg.mongoClient();
			MongoCollection<Document> collection = mongoClient.getDatabase(cfg.dbName).getCollection("EInvoiceMTT");
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
		String mauHD = docTmp.getEmbedded(Arrays.asList("EInvoiceDetail", "TTChung", "KHMSHDon"), "")
				+ docTmp.getEmbedded(Arrays.asList("EInvoiceDetail", "TTChung", "KHHDon"), "");
		String soHD = commons
				.formatNumberBillInvoice(docTmp.getEmbedded(Arrays.asList("EInvoiceDetail", "TTChung", "SHDon"), 0));

		boolean isDieuChinh = "2".equals(docTmp.getEmbedded(Arrays.asList("HDSS", "TCTBao"), ""));
		boolean isThayThe = false;
		String check_status = docTmp.get("EInvoiceStatus", "");
		if (check_status.equals("REPLACED")) {
			isThayThe = "3".equals(docTmp.getEmbedded(Arrays.asList("HDSS", "TCTBao"), ""));
		}

		String CheckView = docTmp.getEmbedded(Arrays.asList("UserConFig", "viewshd"), "");

		String MailJet = docTmp.getEmbedded(Arrays.asList("ConfigEmail", "MailJet"), "");

		String dir = docTmp.getString("Dir");
		String signStatusCode = docTmp.get("SignStatusCode", "");
		String eInvoiceStatus = docTmp.get("EInvoiceStatus", "");
		String MCCQT = docTmp.get("MCCQT", "");
		String secureKey = docTmp.get("SecureKey", "");
		String fileName = _id + ".xml";
		File file = null;

		int SoHDon = docTmp.getEmbedded(Arrays.asList("EInvoiceDetail", "TTChung", "SHDon"), 0);

		String fileNameXML = _id + "_preparingSendCQT.xml";
		String fileNamePDF = _id + ".pdf";
		if (Constants.INVOICE_STATUS.DELETED.equals(eInvoiceStatus))
			fileNamePDF = _id + "-deleted.pdf";
		/* KIEM TRA XEM CO FILE PDF CHUA; NEU CHUA CO THI TAO FILE PDF */
		if (docTmp.get("DMMauSoKyHieu") != null) {
			String MST = docTmp.getEmbedded(Arrays.asList("EInvoiceDetail", "NDHDon", "NBan", "MST"), "");
			String ImgLogo = docTmp.getEmbedded(Arrays.asList("DMMauSoKyHieu", "Templates", "ImgLogo"), "");
			String ImgBackground = docTmp.getEmbedded(Arrays.asList("DMMauSoKyHieu", "Templates", "ImgBackground"), "");
			String ImgQA = docTmp.getEmbedded(Arrays.asList("DMMauSoKyHieu", "Templates", "ImgQA"), "");
			String ImgVien = docTmp.getEmbedded(Arrays.asList("DMMauSoKyHieu", "Templates", "ImgVien"), "");

			fileName = _id + ".xml";
			if ("SIGNED".equals(signStatusCode) && !"".equals(MCCQT)) {
				fileName = _id + "_signed.xml";
				fileNameXML = _id + "_signed.xml";
			} else {
				fileName = _id + "_preparingSendCQT.xml";
			}
			file = new File(dir, fileName);
			if (file.exists() && file.isFile()) {
				org.w3c.dom.Document doc = commons.fileToDocument(file);
				String fileNameJP = docTmp.getEmbedded(Arrays.asList("DMMauSoKyHieu", "Templates", "FileName"), "");
				int numberRowInPage = docTmp.getEmbedded(Arrays.asList("DMMauSoKyHieu", "Templates", "RowsInPage"), 20);
				int numberRowInPageMultiPage = docTmp
						.getEmbedded(Arrays.asList("DMMauSoKyHieu", "Templates", "RowInPageMultiPage"), 26);
				int numberCharsInRow = docTmp.getEmbedded(Arrays.asList("DMMauSoKyHieu", "Templates", "CharsInRow"),
						50);

				File fileJP = new File(SystemParams.DIR_E_INVOICE_TEMPLATE, fileNameJP);

				ByteArrayOutputStream baosPDF = null;

				baosPDF = jpUtils.createFinalInvoiceMTT(fileJP, doc, CheckView, link, eInvoiceStatus, signStatusCode,
						numberRowInPage, numberRowInPageMultiPage, numberCharsInRow, MST,
						Paths.get(SystemParams.DIR_E_INVOICE_TEMPLATE, "images", MST, ImgLogo).toString(),
						Paths.get(SystemParams.DIR_E_INVOICE_TEMPLATE, "images", MST, ImgBackground).toString(),
						Paths.get(SystemParams.DIR_E_INVOICE_TEMPLATE, "images", MST, ImgQA).toString(),
						Paths.get(SystemParams.DIR_E_INVOICE_TEMPLATE, "images", MST, ImgVien).toString(), false,
						Constants.INVOICE_STATUS.DELETED.equals(eInvoiceStatus), isThayThe, isDieuChinh);
				/* LUU TAP TIN PDF */
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

		file = null;
		List<String> listFiles = new ArrayList<>();
		List<String> listNames = new ArrayList<>();
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

		MailConfig mailConfig = new MailConfig(docTmp.get("ConfigEmail", Document.class));
		mailConfig.setNameSend(docTmp.getEmbedded(Arrays.asList("EInvoiceDetail", "NDHDon", "NBan", "Ten"), ""));
		/* THUC HIEN GUI MAIL */
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
				//
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
			// END KIỂM TRA GỬI MAIL
			try {
				MongoClient mongoClient = cfg.mongoClient();
				MongoCollection<Document> collection = mongoClient.getDatabase(cfg.dbName)
						.getCollection("LogEmailUser");
				collection.insertOne(new Document("IssuerId", header.getIssuerId()).append("Title", _title)
						.append("Email", email_gui).append("IsActive", boo).append("MailCheck", boo)
						.append("IsDelete", false).append("EmailContent", _content)

				);
				mongoClient.close();

				/* CAP NHAT LAI TRANG THAI DANG CHO XU LY */
				FindOneAndUpdateOptions options = null;
				options = new FindOneAndUpdateOptions();
				options.upsert(false);
				options.maxTime(5000, TimeUnit.MILLISECONDS);
				options.returnDocument(ReturnDocument.AFTER);

				mongoClient = cfg.mongoClient();
				collection = mongoClient.getDatabase(cfg.dbName).getCollection("EInvoiceMTT");
				collection.findOneAndUpdate(docFind,
						new Document("$set",
								new Document("EmailStatus", boo).append("InfoEmailStatus",
										new Document("Date", LocalDateTime.now()).append("UserID", header.getUserId())
												.append("UserName", header.getUserName())
												.append("UserFullName", header.getUserFullName()))),
						options);
				mongoClient.close();

				
	/* LOG BAO CAO THONG KE */
				
				String TenNBan = docTmp.getEmbedded(Arrays.asList("EInvoiceDetail", "NDHDon", "NBan", "Ten"), "");
				String MSTNBan = docTmp.getEmbedded(Arrays.asList("EInvoiceDetail", "NDHDon", "NBan", "MST"), "");
				String DchiNBan = docTmp.getEmbedded(Arrays.asList("EInvoiceDetail", "NDHDon", "NBan", "DChi"), "");
				String SDTNBan = docTmp.getEmbedded(Arrays.asList("EInvoiceDetail", "NDHDon", "NBan", "SDThoai"), "");
				String EmailNBan = docTmp.getEmbedded(Arrays.asList("EInvoiceDetail", "NDHDon", "NBan", "DCTDTu"), "");
				String TenNMua = docTmp.getEmbedded(Arrays.asList("EInvoiceDetail", "NDHDon", "NMua", "Ten"), "");
				String MSTNMua = docTmp.getEmbedded(Arrays.asList("EInvoiceDetail", "NDHDon", "NMua", "MST"), "");
				String DchiNMua = docTmp.getEmbedded(Arrays.asList("EInvoiceDetail", "NDHDon", "NMua", "DChi"), "");
				String SDTNMua = docTmp.getEmbedded(Arrays.asList("EInvoiceDetail", "NDHDon", "NMua", "SDThoai"), "");
				String EmailNMua = docTmp.getEmbedded(Arrays.asList("EInvoiceDetail", "NDHDon", "NMua", "DCTDTu"), "");
				String EmailCCNMua = docTmp.getEmbedded(Arrays.asList("EInvoiceDetail", "NDHDon", "NMua", "DCTDTuCC"), "");
				int soHDon = docTmp.getEmbedded(Arrays.asList("EInvoiceDetail", "TTChung", "SHDon"), 0);
				double TongTien = docTmp.getEmbedded(Arrays.asList("EInvoiceDetail", "TToan", "TgTTTBSo"), 0.0);
				Date NgayPhatHanh = docTmp.getEmbedded(Arrays.asList("EInvoiceDetail", "TTChung", "NLap"), Date.class);
				
				mongoClient = cfg.mongoClient();
				collection = mongoClient.getDatabase(cfg.dbName).getCollection("BaoCaoThongKe");
					collection.insertOne(new Document("IssuerId", header.getIssuerId())
							.append("TenNBan", TenNBan)
							.append("MSTNBan", MSTNBan)
							.append("DchiNBan", DchiNBan)
							.append("SDTNBan", SDTNBan)
							.append("EmailNBan", EmailNBan)
							.append("TenNMua", TenNMua)
							.append("MSTNMua", MSTNMua)
							.append("DchiNMua", DchiNMua)
							.append("SDTNMua", SDTNMua)
							.append("EmailNMua", EmailNMua)
							.append("EmailCCNMua", EmailCCNMua)
							.append("SHDon", soHDon)
							.append("EmailGuiHoaDon", email_gui)
							.append("TongTien", TongTien)
							.append("NgayPhatHanh", NgayPhatHanh)
							.append("IsDelete", false)
					);			
					mongoClient.close();
					
			} catch (Exception ex) {
			}

		}

		responseStatus = new MspResponseStatus(0, "SUCCESS");
		rsp.setResponseStatus(responseStatus);
		return rsp;
	}
	
	@Override
	public MsgRsp importExcelAuto(JSONRoot jsonRoot) throws Exception {
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
		// END XML

		JsonNode jsonData = null;
		if (objData != null) {
			jsonData = Json.serializer().nodeFromObject(msg.getObjData());
		} else {
			throw new Exception("Lỗi dữ liệu đầu vào");
		}

		String dataFileName = commons.getTextJsonNode(jsonData.at("/DataFileName")).replaceAll("\\s", "");
		String mauSoHdon = commons.getTextJsonNode(jsonData.at("/MauSoHdon")).replaceAll("\\s", "");
		// Start
		ObjectId objectId = null;
		ObjectId objectIdUser = null;
		ObjectId objectIdMSKH = null;
		List<Document> pipeline = null;
		try {
			objectId = new ObjectId(header.getIssuerId());
		} catch (Exception e) {
		}
		try {
			objectIdUser = new ObjectId(header.getUserId());
		} catch (Exception e) {
		}
		try {
			objectIdMSKH = new ObjectId(mauSoHdon);
		} catch (Exception e) {
		}

		/* XU LY LAY ID CỦA MAU SO KI HIEU */

		Document findInforIssuer = new Document("_id", 1).append("TaxCode", 1).append("Name", 1).append("Address", 1)
				.append("Phone", 1).append("Fax", 1).append("Email", 1).append("Website", 1).append("TinhThanhInfo", 1)
				.append("ChiCucThueInfo", 1).append("BankAccount", 1).append("NameEN", 1).append("BankAccountExt", 1);

		pipeline = new ArrayList<Document>();
		pipeline.add(new Document("$match",
				new Document("_id", objectId).append("IsActive", true).append("IsDelete", false)));
		pipeline.add(new Document("$project", findInforIssuer));
		pipeline.add(new Document("$lookup",
				new Document("from", "Users").append("pipeline", Arrays.asList(
						new Document("$match",
								new Document("IssuerId", header.getIssuerId()).append("_id", objectIdUser)
										.append("IsActive", true).append("IsDelete", false)),
						new Document("$project", new Document("_id", 1).append("UserName", 1).append("FullName", 1)),
						new Document("$limit", 1))).append("as", "UserInfo"))

		);
		pipeline.add(
				new Document("$unwind", new Document("path", "$UserInfo").append("preserveNullAndEmptyArrays", true)));
		pipeline.add(new Document("$lookup",
				new Document("from", "DMMauSoKyHieu").append("pipeline", Arrays.asList(
						new Document("$match",
								new Document("IssuerId", header.getIssuerId()).append("IsActive", true)
										.append("IsDelete", false).append("ConLai", new Document("$gt", 0))
										.append("_id", objectIdMSKH)),
						new Document("$project", new Document("_id", 1).append("KHMSHDon", 1).append("KHHDon", 1)),
						new Document("$limit", 1))).append("as", "DMMauSoKyHieu")));
		pipeline.add(new Document("$unwind",
				new Document("path", "$DMMauSoKyHieu").append("preserveNullAndEmptyArrays", true)));
		pipeline.add(new Document("$lookup", new Document("from", "PramLink")
				.append("pipeline",
						Arrays.asList(new Document("$match", new Document("$expr", new Document("IsDelete", false))),
								new Document("$project", new Document("_id", 1).append("LinkPortal", 1))))
				.append("as", "PramLink")));
		pipeline.add(
				new Document("$unwind", new Document("path", "$PramLink").append("preserveNullAndEmptyArrays", true)));
		MongoClient mongoClient = cfg.mongoClient();
		MongoCollection<Document> collection = mongoClient.getDatabase(cfg.dbName).getCollection("Issuer");
		try {
			docTmp = collection.aggregate(pipeline).allowDiskUse(true).iterator().next();
		} catch (Exception e) {

		}

		mongoClient.close();
		if (null == docTmp) {
			responseStatus = new MspResponseStatus(9999, "Không tìm thấy thông tin khách hàng.");
			rsp.setResponseStatus(responseStatus);
			return rsp;
		}
		String link = docTmp.getEmbedded(Arrays.asList("PramLink", "LinkPortal"), "");
		if (docTmp.get("UserInfo") == null) {
			responseStatus = new MspResponseStatus(9999, "Không tìm thấy thông tin người dùng.");
			rsp.setResponseStatus(responseStatus);
			return rsp;
		}
		if (docTmp.get("DMMauSoKyHieu") == null) {
			responseStatus = new MspResponseStatus(9999, "Không tìm thấy thông tin ký hiệu mẫu số.");
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
		List<EInvoiceMTTExcelForm> eInvoiceMTTExcelFormList = new ArrayList<>();
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
				int lastColumn = Math.max(row1.getLastCellNum(), 24);

				for (int cn = 0; cn < lastColumn; cn++) {
					Cell c = row1.getCell(cn, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
					cells.add(c);
				}
				EInvoiceMTTExcelForm eInvoiceExcelForm = extractInfoFromCellAuto(cells);
				eInvoiceMTTExcelFormList.add(eInvoiceExcelForm);
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

		boolean checkMaHD = false;
		boolean checkNullMaHD = false;
		if (eInvoiceMTTExcelFormList != null) {
			for (int tam = 0; tam < eInvoiceMTTExcelFormList.size(); tam++) {
				if (eInvoiceMTTExcelFormList.get(tam).getMaHD() == null) {
					checkNullMaHD = true;
				}
			}
			if (checkNullMaHD == true) {
				responseStatus = new MspResponseStatus(999,
						"Import không thành công. \r\n" + "Hãy kiểm tra lại file excel. \r\n"
								+ "Không được chứa các dòng thừa và phải chuẩn theo mẫu.");
				rsp.setResponseStatus(responseStatus);
				return rsp;
			}

			String tempMST = "";
			String tempTenNM = "";
			String tempCccd = "";
			String tempTenDonVi = "";
			String tempDChiKH = "";
			String tempMailKH = "";
			String tempSdtKH = "";
			String tempStkNH = "";
			String tempTenNH = "";
			String tempLoaiHoaDon = "";
			String tempHinhThucThanhToan = "";
			String tempLoaiTien = "";
			String tempTyGia = "";
			List<DSHHDVu> dshhdVuList = new ArrayList<>();
			List<Object> listHHDVu = new ArrayList<Object>();
			int i = 0;
			int start = 0;
			int end = 0;
			int dem = 0;

			/* DOC FILE EXCEL - GHI DU LIEU VO LIST */
			for (; i < eInvoiceMTTExcelFormList.size();) {
				dem = 0;

				// lay thong tin
				tempMST = eInvoiceMTTExcelFormList.get(i).getMaSoThue();
				tempTenNM = eInvoiceMTTExcelFormList.get(i).getTenNguoiMua();
				tempCccd = eInvoiceMTTExcelFormList.get(i).getCccd();
				tempTenDonVi = eInvoiceMTTExcelFormList.get(i).getTenDonVi();
				tempDChiKH = eInvoiceMTTExcelFormList.get(i).getDiaChiKhachHang();
				tempMailKH = eInvoiceMTTExcelFormList.get(i).getMailKhachHang();
				tempSdtKH = eInvoiceMTTExcelFormList.get(i).getSdtKhachHang();
				tempStkNH = eInvoiceMTTExcelFormList.get(i).getSoTaiKhoan();
				tempTenNH = eInvoiceMTTExcelFormList.get(i).getTenNganHang();
				tempLoaiHoaDon = eInvoiceMTTExcelFormList.get(i).getLoaiHoaDon();
				tempHinhThucThanhToan = eInvoiceMTTExcelFormList.get(i).getHinhThucThanhToan();
				tempLoaiTien = eInvoiceMTTExcelFormList.get(i).getLoaiTien();
				tempTyGia = eInvoiceMTTExcelFormList.get(i).getTyGia();

				for (int j = i; j < eInvoiceMTTExcelFormList.size(); j++) {
					if (eInvoiceMTTExcelFormList.get(i).getMaHD() == eInvoiceMTTExcelFormList.get(j).getMaHD()) {
						dem++;
						start = j + 1;
						end = j;
						if (eInvoiceMTTExcelFormList.size() == j + 1) {
							checkMaHD = true;
						}
					} else {
						checkMaHD = true;
					}

				}

				if (dem == 1) {
					end = i;
					checkMaHD = true;
				}

				if (checkMaHD == true) {
					if (dem > 1) {
						int stt = 1;

						Double tongTienThue = 0.0;
						Double tongThanhTien = 0.0;
						Double total = 0.0;
						for (int k = i; k <= end; k++) {
							DSHHDVu dshhdVu = new DSHHDVu();
							String STT = String.valueOf(stt++);
							Double thanhTien = eInvoiceMTTExcelFormList.get(k).getThanhTien();
							Double thueSuat = eInvoiceMTTExcelFormList.get(k).getThueSuat();
							Double tienThue = eInvoiceMTTExcelFormList.get(k).getTienThue();
							Double tongtien = eInvoiceMTTExcelFormList.get(k).getTongTien();

							tongTienThue += tienThue;
							tongThanhTien += thanhTien;
							total += tongtien;

							dshhdVu.setSTT(STT);
							dshhdVu.setProductName(eInvoiceMTTExcelFormList.get(k).getTenHangHoa());
							dshhdVu.setProductCode(eInvoiceMTTExcelFormList.get(k).getMaHangHoa());
							dshhdVu.setUnit(eInvoiceMTTExcelFormList.get(k).getDonViTinh());
							dshhdVu.setQuantity(eInvoiceMTTExcelFormList.get(k).getSoLuong());
							dshhdVu.setPrice(eInvoiceMTTExcelFormList.get(k).getDonGia());
							dshhdVu.setTotal(thanhTien);
							dshhdVu.setVATRate(thueSuat);
							dshhdVu.setVATAmount(tienThue);
							dshhdVu.setAmount(tongtien);
							dshhdVu.setFeature(eInvoiceMTTExcelFormList.get(k).getTinhChat());

							dshhdVuList.add(dshhdVu);

							HashMap<String, Object> hItem1 = null;
							hItem1 = new LinkedHashMap<String, Object>();
							hItem1.put("STT", dshhdVu.getSTT());
							hItem1.put("ProductName", dshhdVu.getProductName());
							hItem1.put("ProductCode", dshhdVu.getProductCode());
							hItem1.put("SLo", dshhdVu.getSLo());
							hItem1.put("HanSD", dshhdVu.getHanSD());
							hItem1.put("Unit", dshhdVu.getUnit());
							hItem1.put("Quantity", dshhdVu.getQuantity());
							hItem1.put("Price", dshhdVu.getPrice());
							hItem1.put("Total", dshhdVu.getTotal());
							hItem1.put("VATRate", dshhdVu.getVATRate());
							hItem1.put("VATAmount", dshhdVu.getVATAmount());
							hItem1.put("Amount", dshhdVu.getAmount());
							hItem1.put("Feature", dshhdVu.getFeature());

							listHHDVu.add(hItem1);
						}
						// Clear BIEN GAN STT
						stt = stt - (end + 1);

						String TTBChu = commons.formatNumberReal(total).replaceAll(",", "");
						String TTBCHU = ChuyenSangChu(TTBChu);
						String tongTienBangchu = TTBCHU.substring(0, 1).toUpperCase() + TTBCHU.substring(1) + ".";

						// Thông tin hóa đơn - TTChung
						String MaHD = eInvoiceMTTExcelFormList.get(i).getMaHD();
						String THDon = Integer.parseInt(tempLoaiHoaDon) == 1 ? "Hóa đơn giá trị gia tăng TT 78"
								: "Hóa đơn bán hàng";
						String MauSoHD = mauSoHdon;
						String KHMSHDon = docTmp.getEmbedded(Arrays.asList("DMMauSoKyHieu", "KHMSHDon"), "").toString();
						String KHHDon = docTmp.getEmbedded(Arrays.asList("DMMauSoKyHieu", "KHHDon"), "").toString();
						LocalDateTime NLap = LocalDateTime.now();
						String HTTToanCode = "";
						String HTTToan = "";
						switch (tempHinhThucThanhToan) {
						case "1":
							HTTToanCode = "1";
							HTTToan = "Tiền mặt";
							break;
						case "2":
							HTTToanCode = "2";
							HTTToan = "Chuyển khoản";
							break;
						case "3":
							HTTToanCode = "3";
							HTTToan = "Tiền mặt/Chuyển khoản";
							break;
						case "4":
							HTTToanCode = "4";
							HTTToan = "Đối trừ công nợ";
							break;
						case "5":
							HTTToanCode = "5";
							HTTToan = "Không thu tiền";
							break;
						default:
							break;
						}

						// Thông tin người bán - Thông tin người mua - NDHDon
						String Ten = docTmp.getEmbedded(Arrays.asList("Name"), "").toString();
						String MST = docTmp.getEmbedded(Arrays.asList("TaxCode"), "").toString();
						String DChi = docTmp.getEmbedded(Arrays.asList("Address"), "").toString();
						String SDThoai = docTmp.getEmbedded(Arrays.asList("Phone"), "").toString();
						String DCTDTu = docTmp.getEmbedded(Arrays.asList("Email"), "").toString();
						String STKNHang = docTmp.getEmbedded(Arrays.asList("BankAccount", "AccountNumber"), "")
								.toString();
						String TNHang = docTmp.getEmbedded(Arrays.asList("BankAccount", "BankName"), "").toString();
						String Fax = docTmp.getEmbedded(Arrays.asList("Fax"), "").toString();
						String Website = docTmp.getEmbedded(Arrays.asList("Website"), "").toString();

						// Một số thông tin khác
						String MTDiep = "";
						String secureKey = "";

						// Setting
						String pathDir = "";
						File file1 = null;
						Path path1 = null;
						ObjectId objectIdEInvoice = null;
						String fileNameXML = "";
						objectIdEInvoice = new ObjectId();
						String taxCode = "";
						taxCode = docTmp.getString("TaxCode");
						path1 = Paths.get(SystemParams.DIR_E_INVOICE_DATA, taxCode,
								docTmp.getEmbedded(Arrays.asList("DMMauSoKyHieu", "_id"), ObjectId.class).toString());
						pathDir = path1.toString();
						file1 = path1.toFile();
						if (!file1.exists())
							file1.mkdirs();
						fileNameXML = objectIdEInvoice.toString() + ".xml";

						secureKey = commons.csRandomNumbericString(6);

						// XML
						dbf = DocumentBuilderFactory.newInstance();
						db = dbf.newDocumentBuilder();
						doc = db.newDocument();
						doc.setXmlStandalone(true);

						root = doc.createElement("HDon");
						root.setAttribute("Id", "DLieu0");
						doc.appendChild(root);

						elementContent = doc.createElement("DLHDon");
						elementContent.setAttribute("Id", "data");
						root.appendChild(elementContent);

						elementSubTmp = null;
						elementSubTmp01 = null;
						elementSubContent = doc.createElement("TTChung");
						elementTmp = null;

						elementSubContent
								.appendChild(commons.createElementWithValue(doc, "PBan", SystemParams.VERSION_XML));
						elementSubContent.appendChild(commons.createElementWithValue(doc, "THDon", THDon));
						elementSubContent.appendChild(commons.createElementWithValue(doc, "KHMSHDon", KHMSHDon));
						elementSubContent.appendChild(commons.createElementWithValue(doc, "KHHDon", KHHDon));
						elementSubContent.appendChild(commons.createElementWithValue(doc, "SHDon", "")); // SE PHAT SINH

						// Ngày lập
						elementSubContent.appendChild(
								commons.createElementWithValue(doc, "NLap", NLap.format(DateTimeFormatter.ISO_DATE)));

						// Đơn vị tiền tệ
						elementSubContent.appendChild(commons.createElementWithValue(doc, "DVTTe", tempLoaiTien));

						// Tỷ giá
						elementSubContent.appendChild(commons.createElementWithValue(doc, "TGia", tempTyGia));

						// Hình thức thanh toán
						elementSubContent.appendChild(commons.createElementWithValue(doc, "HTTToan", HTTToan));

						// MST tổ chức cung cấp giải pháp HĐĐT
						elementSubContent
								.appendChild(commons.createElementWithValue(doc, "MSTTCGP", SystemParams.MSTTCGP));

						elementTmp = doc.createElement("TTKhac"); // THONG TIN KHAC
						elementTmp
								.appendChild(commons.createElementTTKhac(doc, "HoaDon_Loai", "string", tempLoaiHoaDon));
						elementTmp.appendChild(commons.createElementTTKhac(doc, "PortalLink", "string", link));
						elementTmp.appendChild(commons.createElementTTKhac(doc, "SecureKey", "string", secureKey));
						elementTmp.appendChild(
								commons.createElementTTKhac(doc, "SystemKey", "string", objectIdEInvoice.toString()));
						elementSubContent.appendChild(elementTmp);

						elementContent.appendChild(elementSubContent);

						// NDHDon: Nội dung hóa đơn
						elementSubContent = doc.createElement("NDHDon");
						elementTmp = doc.createElement("NBan"); // NGUOI BAN
						elementTmp.appendChild(commons.createElementWithValue(doc, "Ten", Ten));
						elementTmp.appendChild(commons.createElementWithValue(doc, "MST", MST));
						elementTmp.appendChild(commons.createElementWithValue(doc, "DChi", DChi));
						elementTmp.appendChild(commons.createElementWithValue(doc, "SDThoai", SDThoai));
						elementTmp.appendChild(commons.createElementWithValue(doc, "DCTDTu", DCTDTu));
						elementTmp.appendChild(commons.createElementWithValue(doc, "STKNHang", STKNHang));
						elementTmp.appendChild(commons.createElementWithValue(doc, "TNHang", TNHang));
						elementTmp.appendChild(commons.createElementWithValue(doc, "Fax", Fax));
						elementTmp.appendChild(commons.createElementWithValue(doc, "Website", Website));

						// Thong tin khac
						elementSubTmp = doc.createElement("TTKhac");
						elementSubTmp.appendChild(
								commons.createElementTTKhac(doc, "TenEN", "string", docTmp.get("NameEN", "")));

						elementSubTmp
								.appendChild(commons.createElementTTKhac(doc, "STKNHang" + intTmp, "string", STKNHang));
						elementSubTmp
								.appendChild(commons.createElementTTKhac(doc, "TNHang" + intTmp, "string", TNHang));

						elementTmp.appendChild(elementSubTmp);
						elementSubContent.appendChild(elementTmp);

						elementTmp = doc.createElement("NMua"); // NGUOI MUA
						elementTmp.appendChild(commons.createElementWithValue(doc, "Ten", tempTenDonVi));
						elementTmp.appendChild(commons.createElementWithValue(doc, "MST", tempMST));
						elementTmp.appendChild(commons.createElementWithValue(doc, "DChi", tempDChiKH));
						elementTmp.appendChild(commons.createElementWithValue(doc, "SDThoai", tempSdtKH));
						elementTmp.appendChild(commons.createElementWithValue(doc, "DCTDTu", tempMailKH));
						elementTmp.appendChild(commons.createElementWithValue(doc, "HVTNMHang", tempTenNM));
						elementTmp.appendChild(commons.createElementWithValue(doc, "STKNHang", tempStkNH));
						elementTmp.appendChild(commons.createElementWithValue(doc, "TNHang", tempTenNH));
						elementTmp.appendChild(commons.createElementWithValue(doc, "TTKhac", ""));
						elementSubContent.appendChild(elementTmp);

						mapVATAmount = new LinkedHashMap<String, Double>();
						mapAmount = new LinkedHashMap<String, Double>();
						elementTmp = doc.createElement("DSHHDVu"); // HH-DV

						for (Object o : listHHDVu) {
							if (!"".equals(o.equals("/ProductName"))) {
								JsonNode h = Json.serializer().nodeFromObject(o);
								tmp = commons.formatNumberReal(commons.getTextJsonNode(h.at("/VATRate")))
										.replaceAll(",", "");
								switch (tmp) {
								case "0":
									tmp = "-1";
									break;
								case "1":
									tmp = "0%";
									break;
								case "5":
								case "8":
								case "10":
									tmp += "%";
									break;
								case "-1":
									tmp = "KCT";
									break;
								case "-2":
									tmp = "KKKNT";
									break;
								default:
									break;
								}

								if ("1".equals(commons.getTextJsonNode(h.at("/Feature")))
										|| "3".equals(commons.getTextJsonNode(h.at("/Feature")))
										|| "4".equals(commons.getTextJsonNode(h.at("/Feature")))) {
									mapAmount.compute(tmp, (k, v) -> {
										return (v == null ? commons.ToNumber(commons.getTextJsonNode(h.at("/Total")))
												* ("3".equals(commons.getTextJsonNode(h.at("/Feature"))) ? -1 : 1)
												: v + commons.ToNumber(commons.getTextJsonNode(h.at("/Total")))
														* ("3".equals(commons.getTextJsonNode(h.at("/Feature"))) ? -1
																: 1));
									});
									mapVATAmount.compute(tmp, (k, v) -> {
										return (v == null
												? commons.ToNumber(commons.getTextJsonNode(h.at("/VATAmount")))
														* ("3".equals(commons.getTextJsonNode(h.at("/Feature"))) ? -1
																: 1)
												: v + commons.ToNumber(commons.getTextJsonNode(h.at("/VATAmount")))
														* ("3".equals(commons.getTextJsonNode(h.at("/Feature"))) ? -1
																: 1));
									});
								}

								elementSubTmp = doc.createElement("HHDVu");
								elementSubTmp.appendChild(commons.createElementWithValue(doc, "TChat",
										commons.getTextJsonNode(h.at("/Feature"))));
								elementSubTmp.appendChild(commons.createElementWithValue(doc, "STT",
										commons.getTextJsonNode(h.at("/STT"))));
								elementSubTmp.appendChild(commons.createElementWithValue(doc, "MHHDVu",
										commons.getTextJsonNode(h.at("/ProductCode"))));
								elementSubTmp.appendChild(commons.createElementWithValue(doc, "THHDVu",
										commons.getTextJsonNode(h.at("/ProductName"))));

								elementSubTmp.appendChild(commons.createElementWithValue(doc, "DVTinh",
										commons.getTextJsonNode(h.at("/Unit"))));

								if (!("2".equals(commons.getTextJsonNode(h.at("/Feature"))))) { // ||
																								// "4".equals(commons.getTextJsonNode(o.at("/Feature")))
									elementSubTmp.appendChild(commons.createElementWithValue(doc, "SLuong",
											commons.formatNumberReal(commons.getTextJsonNode(h.at("/Quantity")))
													.replaceAll(",", "")));
									elementSubTmp.appendChild(commons.createElementWithValue(doc, "DGia",
											commons.formatNumberReal(commons.getTextJsonNode(h.at("/Price")))
													.replaceAll(",", "")));
									elementSubTmp.appendChild(commons.createElementWithValue(doc, "TLCKhau", ""));
									elementSubTmp.appendChild(commons.createElementWithValue(doc, "STCKhau", ""));
									elementSubTmp.appendChild(commons.createElementWithValue(doc, "ThTien",
											commons.formatNumberReal(
													commons.getTextJsonNode(h.at("/Total")).replaceAll(",", ""))
													.replaceAll(",", "")));
									if (!tmp.equals("-1") && !tempLoaiHoaDon.equals("2")) {
										elementSubTmp.appendChild(commons.createElementWithValue(doc, "TSuat", tmp));
									}
								}

								if (!tempLoaiHoaDon.equals("2")) {
									elementSubTmp01 = doc.createElement("TTKhac");

									if (commons.getTextJsonNode(h.at("/VATAmount")).replaceAll(",", "").equals("")) {
										elementSubTmp01.appendChild(
												commons.createElementTTKhac(doc, "VATAmount", "decimal", "0"));
									} else {
										elementSubTmp01.appendChild(commons.createElementTTKhac(doc, "VATAmount",
												"decimal",
												commons.getTextJsonNode(h.at("/VATAmount")).replaceAll(",", "")));
									}

									elementSubTmp01.appendChild(commons.createElementTTKhac(doc, "Amount", "decimal",
											commons.getTextJsonNode(h.at("/Amount")).replaceAll(",", "")));

									elementSubTmp.appendChild(elementSubTmp01);
									elementTmp.appendChild(elementSubTmp);
								} else {
									elementSubTmp01 = doc.createElement("TTKhac");

									if (commons.getTextJsonNode(h.at("/VATAmount")).replaceAll(",", "").equals("")) {
										elementSubTmp01.appendChild(
												commons.createElementTTKhac(doc, "VATAmount", "decimal", "0"));
									} else {
										elementSubTmp01.appendChild(commons.createElementTTKhac(doc, "VATAmount",
												"decimal",
												commons.getTextJsonNode(h.at("/VATAmount")).replaceAll(",", "")));
									}

									elementSubTmp01.appendChild(commons.createElementTTKhac(doc, "Amount", "decimal",
											commons.getTextJsonNode(h.at("/Amount")).replaceAll(",", "")));
									elementSubTmp01.appendChild(commons.createElementTTKhac(doc, "Stt", "numeric",
											commons.getTextJsonNode(h.at("/STT"))));

									elementSubTmp.appendChild(elementSubTmp01);
									elementTmp.appendChild(elementSubTmp);
								}
							}
						}

						if (!tempLoaiHoaDon.equals("2")) {
							elementSubContent.appendChild(elementTmp);

							// Thong tin thanh toan
							elementTmp = doc.createElement("TToan");
							elementSubTmp = doc.createElement("THTTLTSuat");

							/* DANH SACH CAC LOAI THUE SUAT */
							for (Map.Entry<String, Double> pair : mapVATAmount.entrySet()) {
								if (null != pair.getKey() && !"".equals(pair.getKey())) {
									if (!pair.getKey().equals("-1")) {
										elementSubTmp01 = doc.createElement("LTSuat");
										elementSubTmp01.appendChild(
												commons.createElementWithValue(doc, "TSuat", pair.getKey()));
										elementSubTmp01.appendChild(commons.createElementWithValue(doc, "ThTien",
												commons.formatNumberReal(mapAmount.get(pair.getKey())).replaceAll(",",
														"")));

										if (tempLoaiTien.equals("VND")) {
											elementSubTmp01.appendChild(commons.createElementWithValue(doc, "TThue",
													String.format("%.0f", mapVATAmount.get(pair.getKey()))));
										} else {
											elementSubTmp01.appendChild(commons.createElementWithValue(doc, "TThue",
													String.format("%.2f", mapVATAmount.get(pair.getKey()))));
										}
										elementSubTmp.appendChild(elementSubTmp01);
									}
								}
							}
							elementTmp.appendChild(elementSubTmp);
							elementTmp.appendChild(commons.createElementWithValue(doc, "TgTCThue",
									commons.formatNumberReal(tongThanhTien).replaceAll(",", "")));
							elementTmp.appendChild(commons.createElementWithValue(doc, "TgTThue",
									commons.formatNumberReal(tongTienThue).replaceAll(",", "")));

							elementSubTmp = doc.createElement("DSLPhi");
							elementSubTmp01 = doc.createElement("LPhi");
							elementSubTmp01.appendChild(commons.createElementWithValue(doc, "TLPhi", ""));
							elementSubTmp01.appendChild(commons.createElementWithValue(doc, "TPhi", "0"));
							elementSubTmp.appendChild(elementSubTmp01);

							elementTmp.appendChild(elementSubTmp);
							elementTmp.appendChild(commons.createElementWithValue(doc, "TTCKTMai", "0"));
							elementTmp.appendChild(commons.createElementWithValue(doc, "TgTTTBSo",
									commons.formatNumberReal(total).replaceAll(",", "")));
							elementTmp.appendChild(commons.createElementWithValue(doc, "TgTTTBChu", tongTienBangchu));

							elementSubTmp = doc.createElement("TTKhac");

							elementTmp.appendChild(elementSubTmp);
							elementSubContent.appendChild(elementTmp);
							elementContent.appendChild(elementSubContent);
						} else {
							elementSubContent.appendChild(elementTmp);

							// Thong tin thanh toan
							elementTmp = doc.createElement("TToan");
							elementTmp.appendChild(commons.createElementWithValue(doc, "TgTCThue",
									commons.formatNumberReal(tongThanhTien).replaceAll(",", "")));
							elementTmp.appendChild(commons.createElementWithValue(doc, "TgTThue",
									commons.formatNumberReal(tongTienThue).replaceAll(",", "")));
							elementTmp.appendChild(commons.createElementWithValue(doc, "TTCKTMai", "0"));
							elementTmp.appendChild(commons.createElementWithValue(doc, "TgTTTBSo",
									commons.formatNumberReal(total).replaceAll(",", "")));
							elementTmp.appendChild(commons.createElementWithValue(doc, "TgTTTBChu", tongTienBangchu));
							elementSubContent.appendChild(elementTmp);
							elementContent.appendChild(elementSubContent);
						}

						// END - NDHDon: Nội dung hóa đơn

						isSdaveFile = commons.docW3cToFile(doc, pathDir, fileNameXML);
						if (!isSdaveFile) {
							throw new Exception("Lưu dữ liệu không thành công.");
						}
						/* END - TAO XML HOA DON */
						String uuid = UUID.randomUUID().toString().replaceAll("-", "").toUpperCase();
						MTDiep = SystemParams.MSTTCGP
								+ commons.convertLocalDateTimeToString(LocalDateTime.now(), "yyyyMMddHHmmssSSS")
								+ uuid.substring(0, 19);

						// lookup data
						docUpsert = new Document("_id", objectIdEInvoice).append("IssuerId", header.getIssuerId())
								.append("MTDiep", MTDiep)
								.append("EInvoiceDetail", new Document("TTChung",
										new Document("THDon", THDon)
										.append("MaHD", MaHD)
										.append("LoaiHD", tempLoaiHoaDon)
										.append("MauSoHD", MauSoHD)
										.append("KHMSHDon", KHMSHDon)
										.append("KHHDon", KHHDon)
										.append("NLap", NLap)
										.append("DVTTe", tempLoaiTien)
										.append("TGia", tempTyGia)
										.append("HTTToanCode", HTTToanCode)
										.append("HTTToan", HTTToan))
										.append("NDHDon",
												new Document("NBan",
														new Document("Ten", Ten)
														.append("MST", MST)
														.append("DChi", DChi)
														.append("SDThoai", SDThoai)
														.append("DCTDTu", DCTDTu)
														.append("STKNHang", STKNHang)
														.append("TNHang", TNHang)
														.append("Fax", Fax)
														.append("Website", Website))
												.append("NMua", new Document("Ten", tempTenDonVi)
																.append("MST", "")
																.append("DChi", tempDChiKH)
																.append("MKHang", "")
																.append("SDThoai", tempSdtKH)
																.append("CCCDan", tempCccd)
																.append("DCTDTu", tempMailKH)
																.append("HVTNMHang", tempTenNM)
																.append("STKNHang", tempStkNH)
																.append("TNHang", tempTenNH)))

										.append("DSHHDVu",listHHDVu)
										.append("TToan",
												new Document("TgTCThue", tongThanhTien)
												.append("TgTThue", tongTienThue)
												.append("TgTTTBSo", total)
												.append("TgTTTBChu", tongTienBangchu)))
								.append("SignStatusCode", Constants.INVOICE_SIGN_STATUS.NOSIGN)
								.append("EInvoiceStatus", Constants.INVOICE_STATUS.CREATED)
								.append("IsDelete", false)
								.append("SecureKey", secureKey)
								.append("Dir", pathDir)
								.append("FileNameXML", fileNameXML)
								.append("InfoCreated",
										new Document("CreateDate", LocalDateTime.now())
										.append("CreateUserID", header.getUserId())
										.append("CreateUserName", header.getUserName())
										.append("CreateUserFullName", header.getUserFullName()));

						mongoClient = cfg.mongoClient();
						collection = mongoClient.getDatabase(cfg.dbName).getCollection("EInvoiceMTT");
						collection.insertOne(docUpsert);
						mongoClient.close();

						dshhdVuList.clear();
						listHHDVu.clear();
						checkMaHD = false;
					} else if (dem == 1) {
						int stt = 1;
						int k = i;
						DSHHDVu dshhdVu = new DSHHDVu();
						String STT = String.valueOf(stt);
						Double thanhTien = eInvoiceMTTExcelFormList.get(k).getThanhTien();
						Double tienThue = eInvoiceMTTExcelFormList.get(k).getTienThue();
						Double tongTien = eInvoiceMTTExcelFormList.get(k).getTongTien();

						dshhdVu.setSTT(STT);
						dshhdVu.setProductName(eInvoiceMTTExcelFormList.get(k).getTenHangHoa());
						dshhdVu.setProductCode(eInvoiceMTTExcelFormList.get(k).getMaHangHoa());
						dshhdVu.setUnit(eInvoiceMTTExcelFormList.get(k).getDonViTinh());
						dshhdVu.setQuantity(eInvoiceMTTExcelFormList.get(k).getSoLuong());
						dshhdVu.setPrice(eInvoiceMTTExcelFormList.get(k).getDonGia());
						dshhdVu.setTotal(eInvoiceMTTExcelFormList.get(k).getThanhTien());
						dshhdVu.setVATRate(eInvoiceMTTExcelFormList.get(k).getThueSuat());
						dshhdVu.setVATAmount(eInvoiceMTTExcelFormList.get(k).getTienThue());
						dshhdVu.setAmount(tongTien);
						dshhdVu.setFeature(eInvoiceMTTExcelFormList.get(k).getTinhChat());

						dshhdVuList.add(dshhdVu);

						List<Object> listHHDVus = new ArrayList<Object>();
						HashMap<String, Object> hItem1 = null;
						hItem1 = new LinkedHashMap<String, Object>();
						hItem1.put("STT", dshhdVu.getSTT());
						hItem1.put("ProductName", dshhdVu.getProductName());
						hItem1.put("ProductCode", dshhdVu.getProductCode());
						hItem1.put("SLo", dshhdVu.getSLo());
						hItem1.put("HanSD", dshhdVu.getHanSD());
						hItem1.put("Unit", dshhdVu.getUnit());
						hItem1.put("Quantity", dshhdVu.getQuantity());
						hItem1.put("Price", dshhdVu.getPrice());
						hItem1.put("Total", dshhdVu.getTotal());
						hItem1.put("VATRate", dshhdVu.getVATRate());
						hItem1.put("VATAmount", dshhdVu.getVATAmount());
						hItem1.put("Amount", dshhdVu.getAmount());
						hItem1.put("Feature", dshhdVu.getFeature());
						listHHDVus.add(hItem1);

						String TTBChu = commons.formatNumberReal(tongTien).replaceAll(",", "");
						String TTBCHU = ChuyenSangChu(TTBChu);
						String tongTienBangchu = TTBCHU.substring(0, 1).toUpperCase() + TTBCHU.substring(1) + ".";
						// Thông tin hóa đơn - TTChung
						String MaHD = eInvoiceMTTExcelFormList.get(i).getMaHD();
						String THDon = Integer.parseInt(tempLoaiHoaDon) == 1 ? "Hóa đơn giá trị gia tăng TT 78"
								: "Hóa đơn bán hàng";
						String MauSoHD = mauSoHdon;
						String KHMSHDon = docTmp.getEmbedded(Arrays.asList("DMMauSoKyHieu", "KHMSHDon"), "").toString();
						String KHHDon = docTmp.getEmbedded(Arrays.asList("DMMauSoKyHieu", "KHHDon"), "").toString();
						LocalDateTime NLap = LocalDateTime.now();
						String HTTToanCode = "";
						String HTTToan = "";
						switch (tempHinhThucThanhToan) {
						case "1":
							HTTToanCode = "1";
							HTTToan = "Tiền mặt";
							break;
						case "2":
							HTTToanCode = "2";
							HTTToan = "Chuyển khoản";
							break;
						case "3":
							HTTToanCode = "3";
							HTTToan = "Tiền mặt/Chuyển khoản";
							break;
						case "4":
							HTTToanCode = "4";
							HTTToan = "Đối trừ công nợ";
							break;
						case "5":
							HTTToanCode = "5";
							HTTToan = "Không thu tiền";
							break;
						default:
							break;
						}

						// Thông tin người bán - Thông tin người mua - NDHDon
						String Ten = docTmp.getEmbedded(Arrays.asList("Name"), "").toString();
						String MST = docTmp.getEmbedded(Arrays.asList("TaxCode"), "").toString();
						String DChi = docTmp.getEmbedded(Arrays.asList("Address"), "").toString();
						String SDThoai = docTmp.getEmbedded(Arrays.asList("Phone"), "").toString();
						String DCTDTu = docTmp.getEmbedded(Arrays.asList("Email"), "").toString();
						String STKNHang = docTmp.getEmbedded(Arrays.asList("BankAccount", "AccountNumber"), "")
								.toString();
						String TNHang = docTmp.getEmbedded(Arrays.asList("BankAccount", "BankName"), "").toString();
						String Fax = docTmp.getEmbedded(Arrays.asList("Fax"), "").toString();
						String Website = docTmp.getEmbedded(Arrays.asList("Website"), "").toString();

						// Một số thông tin khác
						String MTDiep = "";
						String secureKey = "";

						// Setting
						String pathDir = "";
						File file1 = null;
						Path path1 = null;
						ObjectId objectIdEInvoice = null;
						String fileNameXML = "";
						objectIdEInvoice = new ObjectId();
						String taxCode = "";
						taxCode = docTmp.getString("TaxCode");
						path1 = Paths.get(SystemParams.DIR_E_INVOICE_DATA, taxCode,
								docTmp.getEmbedded(Arrays.asList("DMMauSoKyHieu", "_id"), ObjectId.class).toString());
						pathDir = path1.toString();
						file1 = path1.toFile();
						if (!file1.exists())
							file1.mkdirs();
						fileNameXML = objectIdEInvoice.toString() + ".xml";
						secureKey = commons.csRandomNumbericString(6);

						// XML
						dbf = DocumentBuilderFactory.newInstance();
						db = dbf.newDocumentBuilder();
						doc = db.newDocument();
						doc.setXmlStandalone(true);

						root = doc.createElement("HDon");
						root.setAttribute("Id", "DLieu0");
						doc.appendChild(root);

						elementContent = doc.createElement("DLHDon");
						elementContent.setAttribute("Id", "data");
						root.appendChild(elementContent);

						elementSubTmp = null;
						elementSubTmp01 = null;
						elementSubContent = doc.createElement("TTChung");
						elementTmp = null;

						elementSubContent
								.appendChild(commons.createElementWithValue(doc, "PBan", SystemParams.VERSION_XML));
						elementSubContent.appendChild(commons.createElementWithValue(doc, "THDon", THDon));
						elementSubContent.appendChild(commons.createElementWithValue(doc, "KHMSHDon", KHMSHDon));
						elementSubContent.appendChild(commons.createElementWithValue(doc, "KHHDon", KHHDon));
						elementSubContent.appendChild(commons.createElementWithValue(doc, "SHDon", "")); // SE PHAT

						// Ngày lập
						elementSubContent.appendChild(
								commons.createElementWithValue(doc, "NLap", NLap.format(DateTimeFormatter.ISO_DATE)));

						// Đơn vị tiền tệ
						elementSubContent.appendChild(commons.createElementWithValue(doc, "DVTTe", tempLoaiTien));

						// Tỷ giá
						elementSubContent.appendChild(commons.createElementWithValue(doc, "TGia", tempTyGia));

						// Hình thức thanh toán
						elementSubContent.appendChild(commons.createElementWithValue(doc, "HTTToan", HTTToan));
						// MST tổ chức cung cấp giải pháp HĐĐT
						elementSubContent
								.appendChild(commons.createElementWithValue(doc, "MSTTCGP", SystemParams.MSTTCGP));

						// THONG TIN KHAC

						elementTmp = doc.createElement("TTKhac");
						elementTmp
								.appendChild(commons.createElementTTKhac(doc, "HoaDon_Loai", "string", tempLoaiHoaDon));
						elementTmp.appendChild(commons.createElementTTKhac(doc, "PortalLink", "string", link));
						elementTmp.appendChild(commons.createElementTTKhac(doc, "SecureKey", "string", secureKey));
						elementTmp.appendChild(
								commons.createElementTTKhac(doc, "SystemKey", "string", objectIdEInvoice.toString()));

						elementSubContent.appendChild(elementTmp);
						elementContent.appendChild(elementSubContent);

						// NDHDon: Nội dung hóa đơn
						elementSubContent = doc.createElement("NDHDon");
						// NGUOI BAN
						elementTmp = doc.createElement("NBan");
						elementTmp.appendChild(commons.createElementWithValue(doc, "Ten", Ten));
						elementTmp.appendChild(commons.createElementWithValue(doc, "MST", MST));
						elementTmp.appendChild(commons.createElementWithValue(doc, "DChi", DChi));
						elementTmp.appendChild(commons.createElementWithValue(doc, "SDThoai", SDThoai));
						elementTmp.appendChild(commons.createElementWithValue(doc, "DCTDTu", DCTDTu));
						elementTmp.appendChild(commons.createElementWithValue(doc, "STKNHang", STKNHang));
						elementTmp.appendChild(commons.createElementWithValue(doc, "TNHang", TNHang));
						elementTmp.appendChild(commons.createElementWithValue(doc, "Fax", Fax));
						elementTmp.appendChild(commons.createElementWithValue(doc, "Website", Website));

						/* ADD THONG TIN KHAC */
						elementSubTmp = doc.createElement("TTKhac");
						elementSubTmp.appendChild(
								commons.createElementTTKhac(doc, "TenEN", "string", docTmp.get("NameEN", "")));

						elementSubTmp
								.appendChild(commons.createElementTTKhac(doc, "STKNHang" + intTmp, "string", STKNHang));
						elementSubTmp
								.appendChild(commons.createElementTTKhac(doc, "TNHang" + intTmp, "string", TNHang));

						elementTmp.appendChild(elementSubTmp);
						elementSubContent.appendChild(elementTmp);

						// NGUOI MUA
						elementTmp = doc.createElement("NMua");
						elementTmp.appendChild(commons.createElementWithValue(doc, "Ten", tempTenDonVi));
						elementTmp.appendChild(commons.createElementWithValue(doc, "MST", tempMST));
						elementTmp.appendChild(commons.createElementWithValue(doc, "DChi", tempDChiKH));
						elementTmp.appendChild(commons.createElementWithValue(doc, "SDThoai", tempSdtKH));
						elementTmp.appendChild(commons.createElementWithValue(doc, "DCTDTu", tempMailKH));
						elementTmp.appendChild(commons.createElementWithValue(doc, "HVTNMHang", tempTenNM));
						elementTmp.appendChild(commons.createElementWithValue(doc, "STKNHang", tempStkNH));
						elementTmp.appendChild(commons.createElementWithValue(doc, "TNHang", tempTenNH));
						elementTmp.appendChild(commons.createElementWithValue(doc, "TTKhac", ""));
						elementSubContent.appendChild(elementTmp);

						mapVATAmount = new LinkedHashMap<String, Double>();
						mapAmount = new LinkedHashMap<String, Double>();
						elementTmp = doc.createElement("DSHHDVu"); // HH-DV

						for (Object o : listHHDVus) {
							if (!"".equals(o.equals("/ProductName"))) {
								JsonNode h = Json.serializer().nodeFromObject(o);
								tmp = commons.formatNumberReal(commons.getTextJsonNode(h.at("/VATRate")))
										.replaceAll(",", "");
								switch (tmp) {
								case "0":
									tmp = "-1";
									break;
								case "1":
									tmp = "0%";
									break;
								case "5":
								case "8":
								case "10":
									tmp += "%";
									break;
								case "-1":
									tmp = "KCT";
									break;
								case "-2":
									tmp = "KKKNT";
									break;
								default:
									break;
								}

								if ("1".equals(commons.getTextJsonNode(h.at("/Feature")))
										|| "3".equals(commons.getTextJsonNode(h.at("/Feature")))
										|| "4".equals(commons.getTextJsonNode(h.at("/Feature")))) {
									mapAmount.compute(tmp, (f, v) -> {
										return (v == null ? commons.ToNumber(commons.getTextJsonNode(h.at("/Total")))
												* ("3".equals(commons.getTextJsonNode(h.at("/Feature"))) ? -1 : 1)
												: v + commons.ToNumber(commons.getTextJsonNode(h.at("/Total")))
														* ("3".equals(commons.getTextJsonNode(h.at("/Feature"))) ? -1
																: 1));
									});
									mapVATAmount.compute(tmp, (f, v) -> {
										return (v == null
												? commons.ToNumber(commons.getTextJsonNode(h.at("/VATAmount")))
														* ("3".equals(commons.getTextJsonNode(h.at("/Feature"))) ? -1
																: 1)
												: v + commons.ToNumber(commons.getTextJsonNode(h.at("/VATAmount")))
														* ("3".equals(commons.getTextJsonNode(h.at("/Feature"))) ? -1
																: 1));
									});
								}

								elementSubTmp = doc.createElement("HHDVu");
								elementSubTmp.appendChild(commons.createElementWithValue(doc, "TChat",
										commons.getTextJsonNode(h.at("/Feature"))));
								elementSubTmp.appendChild(commons.createElementWithValue(doc, "STT",
										commons.getTextJsonNode(h.at("/STT"))));
								elementSubTmp.appendChild(commons.createElementWithValue(doc, "MHHDVu",
										commons.getTextJsonNode(h.at("/ProductCode"))));
								elementSubTmp.appendChild(commons.createElementWithValue(doc, "THHDVu",
										commons.getTextJsonNode(h.at("/ProductName"))));

								elementSubTmp.appendChild(commons.createElementWithValue(doc, "DVTinh",
										commons.getTextJsonNode(h.at("/Unit"))));

								if (!("2".equals(commons.getTextJsonNode(h.at("/Feature"))))) { // ||
																								// "4".equals(commons.getTextJsonNode(o.at("/Feature")))
									elementSubTmp.appendChild(commons.createElementWithValue(doc, "SLuong",
											commons.formatNumberReal(commons.getTextJsonNode(h.at("/Quantity")))
													.replaceAll(",", "")));
									elementSubTmp.appendChild(commons.createElementWithValue(doc, "DGia",
											commons.formatNumberReal(commons.getTextJsonNode(h.at("/Price")))
													.replaceAll(",", "")));
									elementSubTmp.appendChild(commons.createElementWithValue(doc, "TLCKhau", ""));
									elementSubTmp.appendChild(commons.createElementWithValue(doc, "STCKhau", ""));
									elementSubTmp.appendChild(commons.createElementWithValue(doc, "ThTien",
											commons.formatNumberReal(
													commons.getTextJsonNode(h.at("/Total")).replaceAll(",", ""))
													.replaceAll(",", "")));
									if (!tmp.equals("-1") && !tempLoaiHoaDon.equals("2")) {
										elementSubTmp.appendChild(commons.createElementWithValue(doc, "TSuat", tmp));
									}
								}

								if (!tempLoaiHoaDon.equals("2")) {
									elementSubTmp01 = doc.createElement("TTKhac");

									if (commons.getTextJsonNode(h.at("/VATAmount")).replaceAll(",", "").equals("")) {
										elementSubTmp01.appendChild(
												commons.createElementTTKhac(doc, "VATAmount", "decimal", "0"));
									} else {
										elementSubTmp01.appendChild(commons.createElementTTKhac(doc, "VATAmount",
												"decimal",
												commons.getTextJsonNode(h.at("/VATAmount")).replaceAll(",", "")));
									}

									elementSubTmp01.appendChild(commons.createElementTTKhac(doc, "Amount", "decimal",
											commons.getTextJsonNode(h.at("/Amount")).replaceAll(",", "")));

									elementSubTmp.appendChild(elementSubTmp01);
									elementTmp.appendChild(elementSubTmp);
								} else {
									elementSubTmp01 = doc.createElement("TTKhac");

									if (commons.getTextJsonNode(h.at("/VATAmount")).replaceAll(",", "").equals("")) {
										elementSubTmp01.appendChild(
												commons.createElementTTKhac(doc, "VATAmount", "decimal", "0"));
									} else {
										elementSubTmp01.appendChild(commons.createElementTTKhac(doc, "VATAmount",
												"decimal",
												commons.getTextJsonNode(h.at("/VATAmount")).replaceAll(",", "")));
									}

									elementSubTmp01.appendChild(commons.createElementTTKhac(doc, "Amount", "decimal",
											commons.getTextJsonNode(h.at("/Amount")).replaceAll(",", "")));
									elementSubTmp01.appendChild(commons.createElementTTKhac(doc, "Stt", "numeric",
											commons.getTextJsonNode(h.at("/STT"))));

									elementSubTmp.appendChild(elementSubTmp01);
									elementTmp.appendChild(elementSubTmp);
								}
							}
						}
						if (!tempLoaiHoaDon.equals("2")) {
							elementSubContent.appendChild(elementTmp);

							// Thong tin thanh toan
							elementTmp = doc.createElement("TToan");
							elementSubTmp = doc.createElement("THTTLTSuat");

							/* DANH SACH CAC LOAI THUE SUAT */
							for (Map.Entry<String, Double> pair : mapVATAmount.entrySet()) {
								if (null != pair.getKey() && !"".equals(pair.getKey())) {
									if (!pair.getKey().equals("-1")) {
										elementSubTmp01 = doc.createElement("LTSuat");
										elementSubTmp01.appendChild(
												commons.createElementWithValue(doc, "TSuat", pair.getKey()));
										elementSubTmp01.appendChild(commons.createElementWithValue(doc, "ThTien",
												commons.formatNumberReal(mapAmount.get(pair.getKey())).replaceAll(",",
														"")));

										if (tempLoaiTien.equals("VND")) {
											elementSubTmp01.appendChild(commons.createElementWithValue(doc, "TThue",
													String.format("%.0f", mapVATAmount.get(pair.getKey()))));
										} else {
											elementSubTmp01.appendChild(commons.createElementWithValue(doc, "TThue",
													String.format("%.2f", mapVATAmount.get(pair.getKey()))));
										}
										elementSubTmp.appendChild(elementSubTmp01);
									}
								}
							}
							elementTmp.appendChild(elementSubTmp);
							elementTmp.appendChild(commons.createElementWithValue(doc, "TgTCThue",
									commons.formatNumberReal(thanhTien).replaceAll(",", "")));
							elementTmp.appendChild(commons.createElementWithValue(doc, "TgTThue",
									commons.formatNumberReal(tienThue).replaceAll(",", "")));

							elementSubTmp = doc.createElement("DSLPhi");
							elementSubTmp01 = doc.createElement("LPhi");
							elementSubTmp01.appendChild(commons.createElementWithValue(doc, "TLPhi", ""));
							elementSubTmp01.appendChild(commons.createElementWithValue(doc, "TPhi", "0"));
							elementSubTmp.appendChild(elementSubTmp01);

							elementTmp.appendChild(elementSubTmp);
							elementTmp.appendChild(commons.createElementWithValue(doc, "TTCKTMai", "0"));
							elementTmp.appendChild(commons.createElementWithValue(doc, "TgTTTBSo",
									commons.formatNumberReal(tongTien).replaceAll(",", "")));
							elementTmp.appendChild(commons.createElementWithValue(doc, "TgTTTBChu", tongTienBangchu));

							elementSubTmp = doc.createElement("TTKhac");

							elementTmp.appendChild(elementSubTmp);
							elementSubContent.appendChild(elementTmp);
							elementContent.appendChild(elementSubContent);
						} else {
							elementSubContent.appendChild(elementTmp);

							// Thong tin thanh toan
							elementTmp = doc.createElement("TToan");
							elementTmp.appendChild(commons.createElementWithValue(doc, "TgTCThue",
									commons.formatNumberReal(thanhTien).replaceAll(",", "")));
							elementTmp.appendChild(commons.createElementWithValue(doc, "TgTThue",
									commons.formatNumberReal(tienThue).replaceAll(",", "")));
							elementTmp.appendChild(commons.createElementWithValue(doc, "TTCKTMai", "0"));
							elementTmp.appendChild(commons.createElementWithValue(doc, "TgTTTBSo",
									commons.formatNumberReal(tongTien).replaceAll(",", "")));
							elementTmp.appendChild(commons.createElementWithValue(doc, "TgTTTBChu", tongTienBangchu));
							elementSubContent.appendChild(elementTmp);
							elementContent.appendChild(elementSubContent);
						}
						// END - NDHDon: Nội dung hóa đơn

						isSdaveFile = commons.docW3cToFile(doc, pathDir, fileNameXML);
						if (!isSdaveFile) {
							throw new Exception("Lưu dữ liệu không thành công.");
						}
						/* END - TAO XML HOA DON */

						String uuid = UUID.randomUUID().toString().replaceAll("-", "").toUpperCase();
						MTDiep = SystemParams.MSTTCGP
								+ commons.convertLocalDateTimeToString(LocalDateTime.now(), "yyyyMMddHHmmssSSS")
								+ uuid.substring(0, 19);

						// lookup data
						docUpsert = new Document("_id", objectIdEInvoice).append("IssuerId", header.getIssuerId())
									.append("MTDiep", MTDiep)
									.append("EInvoiceDetail", new Document("TTChung",
											new Document("THDon", THDon)
											.append("MaHD", MaHD)
											.append("LoaiHD", tempLoaiHoaDon)
											.append("MauSoHD", MauSoHD)
											.append("KHMSHDon", KHMSHDon)
											.append("KHHDon", KHHDon)
											.append("NLap", NLap)
											.append("DVTTe", tempLoaiTien)
											.append("TGia", tempTyGia)
											.append("HTTToanCode", HTTToanCode)
											.append("HTTToan", HTTToan))
											.append("NDHDon",
													new Document("NBan",
															new Document("Ten", Ten)
															.append("MST", MST)
															.append("DChi", DChi)
															.append("SDThoai", SDThoai)
															.append("DCTDTu", DCTDTu)
															.append("STKNHang", STKNHang)
															.append("TNHang", TNHang)
															.append("Fax", Fax)
															.append("Website", Website))
													.append("NMua", new Document("Ten", tempTenDonVi)
																	.append("MST", "")
																	.append("DChi", tempDChiKH)
																	.append("MKHang", "")
																	.append("SDThoai", tempSdtKH)
																	.append("CCCDan", tempCccd)
																	.append("DCTDTu", tempMailKH)
																	.append("HVTNMHang", tempTenNM)
																	.append("STKNHang", tempStkNH)
																	.append("TNHang", tempTenNH)))

											.append("DSHHDVu",listHHDVus)
											.append("TToan",
													new Document("TgTCThue", thanhTien)
													.append("TgTThue", tienThue)
													.append("TgTTTBSo", tongTien)
													.append("TgTTTBChu", tongTienBangchu)))
									.append("SignStatusCode", Constants.INVOICE_SIGN_STATUS.NOSIGN)
									.append("EInvoiceStatus", Constants.INVOICE_STATUS.CREATED)
									.append("IsDelete", false)
									.append("SecureKey", secureKey)
									.append("Dir", pathDir)
									.append("FileNameXML", fileNameXML)
									.append("InfoCreated",
											new Document("CreateDate", LocalDateTime.now())
											.append("CreateUserID", header.getUserId())
											.append("CreateUserName", header.getUserName())
											.append("CreateUserFullName", header.getUserFullName()));

							mongoClient = cfg.mongoClient();
							collection = mongoClient.getDatabase(cfg.dbName).getCollection("EInvoiceMTT");
							collection.insertOne(docUpsert);
							mongoClient.close();

							dshhdVuList.clear();
							listHHDVus.clear();
							checkMaHD = false;
					}
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

	// Ham lay gia tri cac o trong excel file
	private static EInvoiceMTTExcelForm extractInfoFromCellAuto(List<Cell> cells) {
		EInvoiceMTTExcelForm eInvoiceMTTExcelForm = new EInvoiceMTTExcelForm();
		// Ma hoa don
		Cell maHD = cells.get(0);
		if (maHD != null) {
			switch (maHD.getCellType()) {
			case STRING:
				eInvoiceMTTExcelForm.setMaHD(maHD.getStringCellValue().trim());
				break;
			case NUMERIC:
				eInvoiceMTTExcelForm.setMaHD((NumberToTextConverter.toText(maHD.getNumericCellValue())));
				break;
			case BLANK:
				break;
			default:
				break;
			}
		}
		// Ma so thue
		Cell maSoThue = cells.get(1);
		if (maSoThue != null) {
			switch (maSoThue.getCellType()) {
			case STRING:
				eInvoiceMTTExcelForm.setMaSoThue(maSoThue.getStringCellValue().trim());
				break;
			case BLANK:
				break;
			default:
				break;
			}
		}
		// Ten nguoi mua
		Cell tenNguoiMua = cells.get(2);
		if (tenNguoiMua != null) {
			switch (tenNguoiMua.getCellType()) {
			case STRING:
				eInvoiceMTTExcelForm.setTenNguoiMua(tenNguoiMua.getStringCellValue().trim());
				break;
			case BLANK:
				break;
			default:
				break;
			}
		}
		// CCCD
		Cell cccd = cells.get(3);
		if (cccd != null) {
			switch (cccd.getCellType()) {
			case STRING:
				eInvoiceMTTExcelForm.setCccd(cccd.getStringCellValue().trim());
				break;
			case BLANK:
				break;
			default:
				break;
			}
		}

		// Ten don vi
		Cell tenDonvi = cells.get(4);
		if (tenDonvi != null) {
			switch (tenDonvi.getCellType()) {
			case STRING:
				eInvoiceMTTExcelForm.setTenDonVi(tenDonvi.getStringCellValue().trim());
				break;
			case BLANK:
				break;
			default:
				break;
			}
		}

		// Dia chi khach hang
		Cell diaChiKhachHang = cells.get(5);
		if (diaChiKhachHang != null) {
			switch (diaChiKhachHang.getCellType()) {
			case STRING:
				eInvoiceMTTExcelForm.setDiaChiKhachHang(diaChiKhachHang.getStringCellValue().trim());
				break;
			case BLANK:
				break;
			default:
				break;
			}

		}
		// Mail khach hang
		Cell mailKhachHang = cells.get(6);
		if (mailKhachHang != null) {
			switch (mailKhachHang.getCellType()) {
			case STRING:
				eInvoiceMTTExcelForm.setMailKhachHang(mailKhachHang.getStringCellValue().trim());
				break;
			case NUMERIC:
				eInvoiceMTTExcelForm
						.setMailKhachHang((NumberToTextConverter.toText(mailKhachHang.getNumericCellValue())));
				break;
			case BLANK:
				break;
			default:
				break;
			}

		}
		// SDT khach hang
		Cell sdtKhachHang = cells.get(7);
		if (sdtKhachHang != null) {
			switch (sdtKhachHang.getCellType()) {
			case STRING:
				eInvoiceMTTExcelForm.setSdtKhachHang(sdtKhachHang.getStringCellValue().trim());
				break;
			case BLANK:
				break;
			default:
				break;
			}
		}
		// So tai khoan khach hang
		Cell soTaiKhoan = cells.get(8);
		if (soTaiKhoan != null) {
			switch (soTaiKhoan.getCellType()) {
			case STRING:
				eInvoiceMTTExcelForm.setSoTaiKhoan(soTaiKhoan.getStringCellValue().trim());
				break;
			case BLANK:
				break;
			default:
				break;
			}
		}
		// Ten ngan hang
		Cell tenNganHang = cells.get(9);
		if (tenNganHang != null) {
			switch (tenNganHang.getCellType()) {
			case STRING:
				eInvoiceMTTExcelForm.setTenNganHang(tenNganHang.getStringCellValue().trim());
				break;
			case NUMERIC:
				eInvoiceMTTExcelForm.setTenNganHang((NumberToTextConverter.toText(tenNganHang.getNumericCellValue())));
				break;
			case BLANK:
				break;
			default:
				break;
			}

		}

		// Loai hoa don
		Cell loaiHoaDon = cells.get(10);
		if (loaiHoaDon != null) {
			switch (loaiHoaDon.getCellType()) {
			case STRING:
				eInvoiceMTTExcelForm.setLoaiHoaDon(loaiHoaDon.getStringCellValue().trim());
				break;
			case NUMERIC:
				eInvoiceMTTExcelForm.setLoaiHoaDon((NumberToTextConverter.toText(loaiHoaDon.getNumericCellValue())));
				break;
			case BLANK:
				break;
			default:
				break;
			}

		}

		// hinh thuc thanh toan
		Cell hinhThucThanhToan = cells.get(11);
		if (hinhThucThanhToan != null) {
			switch (hinhThucThanhToan.getCellType()) {
			case STRING:
				eInvoiceMTTExcelForm.setHinhThucThanhToan(hinhThucThanhToan.getStringCellValue().trim());
				break;
			case NUMERIC:
				eInvoiceMTTExcelForm
						.setHinhThucThanhToan((NumberToTextConverter.toText(hinhThucThanhToan.getNumericCellValue())));
				break;
			case BLANK:
				break;
			default:
				break;
			}
		}
		// Loai tien
		Cell loaiTien = cells.get(12);
		if (loaiTien != null) {
			switch (loaiTien.getCellType()) {
			case STRING:
				eInvoiceMTTExcelForm.setLoaiTien(loaiTien.getStringCellValue().trim());
				break;
			case BLANK:
				break;
			default:
				break;
			}

		}
		// Ten hang hoa
		Cell tenHangHoa = cells.get(13);
		if (tenHangHoa != null) {
			switch (tenHangHoa.getCellType()) {
			case STRING:
				eInvoiceMTTExcelForm.setTenHangHoa(tenHangHoa.getStringCellValue().trim());
				break;
			case BLANK:
				break;
			default:
				break;
			}
		}
		// Ma hang hoa
		Cell maHangHoa = cells.get(14);
		if (maHangHoa != null) {
			switch (maHangHoa.getCellType()) {
			case STRING:
				eInvoiceMTTExcelForm.setMaHangHoa(maHangHoa.getStringCellValue().trim());
				break;
			case BLANK:
				break;
			default:
				break;
			}
		}
		// Don vi tinh
		Cell donViTinh = cells.get(15);
		if (donViTinh != null) {
			switch (donViTinh.getCellType()) {
			case STRING:
				eInvoiceMTTExcelForm.setDonViTinh(donViTinh.getStringCellValue().trim());
				break;
			case BLANK:
				break;
			default:
				break;
			}
		}

		// So luong
		Cell soLuong = cells.get(16);
		if (soLuong != null) {
			switch (soLuong.getCellType()) {
			case STRING:
				eInvoiceMTTExcelForm.setSoLuong((Double.valueOf((String) soLuong.getStringCellValue().trim())));
				break;
			case NUMERIC:
				eInvoiceMTTExcelForm.setSoLuong(soLuong.getNumericCellValue());
				break;
			case BLANK:
				break;
			default:
				break;
			}
		}
		// Don gia
		Cell donGia = cells.get(17);
		if (donGia != null) {
			switch (donGia.getCellType()) {
			case STRING:
				eInvoiceMTTExcelForm.setDonGia((Double.valueOf((String) donGia.getStringCellValue().trim())));
				break;
			case NUMERIC:
				eInvoiceMTTExcelForm.setDonGia(donGia.getNumericCellValue());
				break;
			case BLANK:
				break;
			default:
				break;
			}
		}

		// Thanh tien
		Cell thanhTien = cells.get(18);
		if (thanhTien != null) {
			if (thanhTien.getCellType() == CellType.FORMULA) {
				switch (thanhTien.getCachedFormulaResultType()) {
				case STRING:
					eInvoiceMTTExcelForm.setThanhTien((Double.valueOf((String) thanhTien.getStringCellValue().trim())));
					break;
				case NUMERIC:
					eInvoiceMTTExcelForm.setThanhTien(thanhTien.getNumericCellValue());
					break;
				case BLANK:
					break;
				default:
					break;
				}
			} else {
				switch (thanhTien.getCellType()) {
				case STRING:
					eInvoiceMTTExcelForm.setThanhTien((Double.valueOf((String) thanhTien.getStringCellValue().trim())));
					break;
				case NUMERIC:
					eInvoiceMTTExcelForm.setThanhTien(thanhTien.getNumericCellValue());
					break;
				case BLANK:
					break;
				default:
					break;
				}
			}
		}

		// Thue suat
		Cell thueSuat = cells.get(19);
		if (thueSuat != null) {
			switch (thueSuat.getCellType()) {
			case STRING:
				eInvoiceMTTExcelForm.setThueSuat((Double.valueOf((String) thueSuat.getStringCellValue().trim())));
				break;
			case NUMERIC:
				eInvoiceMTTExcelForm.setThueSuat(thueSuat.getNumericCellValue());
				break;
			case BLANK:
				break;
			default:
				break;
			}
		}
		// Tien thue
		Cell tienThue = cells.get(20);
		if (tienThue != null) {
			if (tienThue.getCellType() == CellType.FORMULA) {
				switch (tienThue.getCachedFormulaResultType()) {
				case STRING:
					eInvoiceMTTExcelForm.setTienThue((Double.valueOf((String) tienThue.getStringCellValue().trim())));
					break;
				case NUMERIC:
					eInvoiceMTTExcelForm.setTienThue(tienThue.getNumericCellValue());
					break;
				case BLANK:
					break;
				default:
					break;
				}
			} else {
				switch (tienThue.getCellType()) {
				case STRING:
					eInvoiceMTTExcelForm.setTienThue((Double.valueOf((String) tienThue.getStringCellValue().trim())));
					break;
				case NUMERIC:
					eInvoiceMTTExcelForm.setTienThue(tienThue.getNumericCellValue());
					break;
				case BLANK:
					break;
				default:
					break;
				}
			}
		}

		// Tinh chat
		Cell tinhChat = cells.get(21);
		if (tinhChat != null) {
			switch (tinhChat.getCellType()) {
			case STRING:
				eInvoiceMTTExcelForm.setTinhChat(tinhChat.getStringCellValue().trim());
				break;
			case NUMERIC:
				eInvoiceMTTExcelForm.setTinhChat((NumberToTextConverter.toText(tinhChat.getNumericCellValue())));
				break;
			case BLANK:
				break;
			default:
				break;
			}
		}

		// ty gia
		Cell tyGia = cells.get(22);
		if (tyGia != null) {
			switch (tyGia.getCellType()) {
			case STRING:
				eInvoiceMTTExcelForm.setTyGia(tyGia.getStringCellValue().trim());
				break;
			case NUMERIC:
				eInvoiceMTTExcelForm.setTyGia((NumberToTextConverter.toText(tyGia.getNumericCellValue())));
				break;
			case BLANK:
				break;
			default:
				break;
			}
		}

		// Tong tien
		Cell tongTien = cells.get(23);
		if (tongTien != null) {
			if (tongTien.getCellType() == CellType.FORMULA) {
				switch (tongTien.getCachedFormulaResultType()) {
				case STRING:
					eInvoiceMTTExcelForm.setTongTien((Double.valueOf((String) tongTien.getStringCellValue().trim())));
					break;
				case NUMERIC:
					eInvoiceMTTExcelForm.setTongTien(tongTien.getNumericCellValue());
					break;
				case BLANK:
					break;
				default:
					break;
				}
			} else {
				switch (tongTien.getCellType()) {
				case STRING:
					eInvoiceMTTExcelForm.setTongTien((Double.valueOf((String) tongTien.getStringCellValue().trim())));
					break;
				case NUMERIC:
					eInvoiceMTTExcelForm.setTongTien(tongTien.getNumericCellValue());
					break;
				case BLANK:
					break;
				default:
					break;
				}
			}
		}

		// Tra giá trị của 1 row
		return eInvoiceMTTExcelForm;
	}

	@Override
	public MsgRsp checkSHD(JSONRoot jsonRoot) throws Exception {
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

		String _id = commons.getTextJsonNode(jsonData.at("/_id")).replaceAll("\\s", "");

		ObjectId objectId = null;
		try {
			objectId = new ObjectId(_id);
		} catch (Exception e) {
		}
		List<Document> pipeline = null;
		/* KIEM TRA THONG TIN HOP LE KHONG */
		Document docFind = new Document("IssuerId", header.getIssuerId()).append("IsDelete", false).append("_id",
				objectId);
		pipeline = new ArrayList<Document>();
		pipeline.add(new Document("$match", docFind));
		pipeline.add(
				new Document("$project", new Document("_id", 1).append("EInvoiceDetail", 1).append("IssuerId", 1)));
		pipeline.add(new Document("$lookup", new Document("from", "EInvoiceMTT")
				.append("let",
						new Document("vIssuerId", "$IssuerId").append("vMauSo", "$EInvoiceDetail.TTChung.MauSoHD"))
				.append("pipeline", Arrays.asList(
						new Document("$match",
								new Document("$expr", new Document("$and",
										Arrays.asList(new Document("$eq", Arrays.asList("$IssuerId", "$$vIssuerId")),
												new Document("$eq",
														Arrays.asList("$EInvoiceDetail.TTChung.MauSoHD", "$$vMauSo")),
												new Document("$ne", Arrays.asList("$IsDelete", true)),
												new Document("$in", Arrays.asList("$EInvoiceStatus",
														Arrays.asList("COMPLETE", "ERROR_CQT", "PROCESSING", "XOABO",
																"DELETED", "REPLACED", "ADJUSTED"))))))),
						new Document("$group",
								new Document("_id", "$EInvoiceDetail.TTChung.MauSoHD").append("RecentNlap",
										new Document("$max", "$EInvoiceDetail.TTChung.NLap")))))
				.append("as", "MaxNlapInfo")));

		pipeline.add(new Document("$lookup", new Document("from", "EInvoiceMTT")
				.append("let",
						new Document("vIssuerId", "$IssuerId").append("vMauSo", "$EInvoiceDetail.TTChung.MauSoHD"))
				.append("pipeline", Arrays.asList(
						new Document("$match", new Document("$expr", new Document("$and",
								Arrays.asList(new Document("$eq", Arrays.asList("$IssuerId", "$$vIssuerId")),
										new Document("$eq",
												Arrays.asList("$EInvoiceDetail.TTChung.MauSoHD", "$$vMauSo")),
										new Document("$ne", Arrays.asList("$IsDelete", true)),
										new Document("$not",
												new Document("$in", Arrays.asList("$EInvoiceStatus",
														Arrays.asList("COMPLETE", "ERROR_CQT", "PROCESSING", "XOABO",
																"DELETED", "REPLACED", "ADJUSTED")))))))),
						new Document("$group",
								new Document("_id", "$EInvoiceDetail.TTChung.MauSoHD").append("MinSHDon",
										new Document("$min", "$EInvoiceDetail.TTChung.SHDon")))))
				.append("as", "MinSHDonInfo")));
		pipeline.add(new Document("$unwind",
				new Document("path", "$MinSHDonInfo").append("preserveNullAndEmptyArrays", true)));
		pipeline.add(new Document("$unwind",
				new Document("path", "$MaxNlapInfo").append("preserveNullAndEmptyArrays", true)));
		Document docTmp = null;

		MongoClient mongoClient = cfg.mongoClient();
		MongoCollection<Document> collection = mongoClient.getDatabase(cfg.dbName).getCollection("EInvoiceMTT");

		try {
			docTmp = collection.aggregate(pipeline).allowDiskUse(true).iterator().next();
		} catch (Exception e) {

		}

		mongoClient.close();
		if (null == docTmp) {
			responseStatus = new MspResponseStatus(9999, "Không tìm thấy thông tin hóa đơn.");
			rsp.setResponseStatus(responseStatus);
			return rsp;
		}

		// lấy SHDon của hd hiện tại
		int currentInvoiceNumber = docTmp.getEmbedded(Arrays.asList("EInvoiceDetail", "TTChung", "SHDon"), 0);

		// lấy ngày lập của hóa đơn hiện tại
		LocalDate currentNLap = commons.convertDateToLocalDate(
				docTmp.getEmbedded(Arrays.asList("EInvoiceDetail", "TTChung", "NLap"), Date.class));

		// lấy SHDon nhở nhất trong tập SHDon chưa phát hành và đã có SHDon
		int minInvoiceNumber = 0;
		if (docTmp.get("MinSHDonInfo") != null)
			minInvoiceNumber = docTmp.getEmbedded(Arrays.asList("MinSHDonInfo", "MinSHDon"), 0);

		// kiểm tra SHDon hiện tại có phải là số hóa đơn nhỏ nhất trong tập SHDon chưa
		// phát hành và đã có SHDon không
		if (currentInvoiceNumber != minInvoiceNumber) {
			rsp.setResponseStatus(new MspResponseStatus(9999, "Vui lòng ký hóa đơn có Số hóa đơn nhỏ nhất là: "
					+ commons.formatNumberBillInvoice(minInvoiceNumber)));
			return rsp;
		}

		// lấy ngày lập gần nhất
		LocalDate latestNLap = null;
		if (docTmp.get("MaxNlapInfo") != null) {
			latestNLap = commons
					.convertDateToLocalDate(docTmp.getEmbedded(Arrays.asList("MaxNlapInfo", "RecentNlap"), Date.class));
		}

		if (latestNLap != null) {
			if (currentNLap == null) {
				rsp.setResponseStatus(new MspResponseStatus(9999, "Hóa đơn không có Ngày phát hành"));
				return rsp;
			}
			if (commons.compareLocalDate(currentNLap, latestNLap) < 0) {
				rsp.setResponseStatus(new MspResponseStatus(9999,
						"Vui lòng ký hóa đơn có Ngày phát hành lớn hơn hoặc bằng ngày " + latestNLap));
				return rsp;
			}
		}
		responseStatus = new MspResponseStatus(0, "SUCCESS");
		rsp.setResponseStatus(responseStatus);
		return rsp;
	}

	@Override
	public MsgRsp publishHDList(JSONRoot jsonRoot) throws Exception {
		Msg msg = jsonRoot.getMsg();
		MsgHeader header = msg.getMsgHeader();
		MsgPage page = msg.getMsgPage();
		Object objData = msg.getObjData();

		JsonNode jsonData = null;
		if (objData != null) {
			jsonData = Json.serializer().nodeFromObject(objData);
		} else {
			throw new Exception("Lỗi dữ liệu đầu vào");
		}

		MsgRsp rsp = new MsgRsp(header);
		rsp.setMsgPage(page);

		String soLuongHD = commons.getTextJsonNode(jsonData.at("/SL")).replaceAll("\\s", "0");

		ObjectId objectIssuerId = null;
		try {
			objectIssuerId = new ObjectId(header.getIssuerId());
		} catch (Exception e) {
		}

		String issuerId = header.getIssuerId();
		issuerLocks_sign.putIfAbsent(issuerId, new ReentrantLock());
		ReentrantLock lock = issuerLocks_sign.get(issuerId);

		if (lock.tryLock()) {
			try {
				ObjectId objectId = null;
				Document fillter = null;
				Document docFind = null;
				Document docTmp = null;
				FindOneAndUpdateOptions options = null;
				List<Document> pipeline = null;
				for (JsonNode o : jsonData.at("/ids")) {
					String id = commons.getTextJsonNode(o);
					try {
						objectId = new ObjectId(id);
					} catch (Exception e) {

					}

					docFind = new Document("IssuerId", header.getIssuerId())
							.append("IsDelete", new Document("$ne", true)).append("_id", objectId)
							.append("EInvoiceStatus", new Document("$in", Arrays.asList("CREATED", "PENDING")))
							.append("SignStatusCode", new Document("$in", Arrays.asList("NOSIGN", "PROCESSING")));
					fillter = new Document("_id", 1).append("EInvoiceDetail", 1).append("IssuerId", 1).append("Dir", 1)
							.append("SignStatusCode", 1).append("MCCQT", 1);

					pipeline = new ArrayList<Document>();
					pipeline.add(new Document("$match", docFind));
					pipeline.add(new Document("$project", fillter));

					// check info MSHD
					pipeline.add(new Document("$lookup", new Document("from", "DMMauSoKyHieu")
							.append("let",
									new Document("vMauSoHD", "$EInvoiceDetail.TTChung.MauSoHD").append("vIssuerId",
											"$IssuerId"))
							.append("pipeline", Arrays.asList(new Document("$match", new Document("$expr",
									new Document("$and", Arrays.asList(new Document("$gt", Arrays.asList("$ConLai", 0)),
											new Document("$eq", Arrays.asList("$IsActive", true)),
											new Document("$ne", Arrays.asList("$IsDelete", true)),
											new Document("$eq",
													Arrays.asList(new Document("$toString", "$_id"), "$$vMauSoHD")),
											new Document("$eq", Arrays.asList("$IssuerId", "$$vIssuerId"))))))))
							.append("as", "DMMauSoKyHieu")));
					pipeline.add(new Document("$unwind",
							new Document("path", "$DMMauSoKyHieu").append("preserveNullAndEmptyArrays", true)));

					MongoClient mongoClient = cfg.mongoClient();
					MongoCollection<Document> collection = mongoClient.getDatabase(cfg.dbName)
							.getCollection("EInvoiceMTT");
					try {
						docTmp = collection.aggregate(pipeline).allowDiskUse(true).iterator().next();
					} catch (Exception e) {
						logger.error("Error when access mongoDB: ", e.getMessage(), e);
					}
					mongoClient.close();

					if (null == docTmp) {
						rsp.setResponseStatus(new MspResponseStatus(9999, "Không tìm thấy thông tin hóa đơn."));
						return rsp;
					}
					if (null == docTmp.get("DMMauSoKyHieu")) {
						rsp.setResponseStatus(new MspResponseStatus(9999, "Hết số hóa đơn."));
						return rsp;
					}

					String mauSoHD = docTmp.getEmbedded(Arrays.asList("EInvoiceDetail", "TTChung", "MauSoHD"),
							String.class);
					String vIssuerId = header.getIssuerId();
					// get max release date and max shd signed

					pipeline = new ArrayList<Document>();
					pipeline.add(new Document("$match",
							new Document("IssuerId", vIssuerId).append("EInvoiceDetail.TTChung.MauSoHD", mauSoHD)
									.append("IsDelete", new Document("$ne", true))
									.append("EInvoiceStatus", new Document("$in", Arrays.asList("COMPLETE", "ERROR_CQT",
											"PROCESSING", "XOABO", "DELETED", "REPLACED", "ADJUSTED")))));
					pipeline.add(new Document("$group", new Document("_id", "$EInvoiceDetail.TTChung.MauSoHD")
							.append("RecentNlap", new Document("$max", "$EInvoiceDetail.TTChung.NLap"))));

					Document docInfo = null;
					mongoClient = cfg.mongoClient();
					collection = mongoClient.getDatabase(cfg.dbName).getCollection("EInvoiceMTT");
					try {
						docInfo = collection.aggregate(pipeline).allowDiskUse(true).iterator().next();
					} catch (Exception e) {
						logger.error("Error when access mongoDB: ", e.getMessage(), e);
					}
					mongoClient.close();

					// check date of invoice
					LocalDate currentLocalDate = LocalDate.now();
					LocalDate latestNLap = null;

					if (docInfo != null && docInfo.get("RecentNlap") != null) {
						latestNLap = commons
								.convertDateToLocalDate(docInfo.getEmbedded(Arrays.asList("RecentNlap"), Date.class));
					}

					LocalDate localDateOfInvoice = null;
					if (docTmp.get("EInvoiceDetail") != null) {
						localDateOfInvoice = commons.convertDateToLocalDate(
								docTmp.getEmbedded(Arrays.asList("EInvoiceDetail", "TTChung", "NLap"), Date.class));
					}

					if (latestNLap != null) {
						if (localDateOfInvoice.compareTo(latestNLap) < 0) {
							rsp.setResponseStatus(new MspResponseStatus(9999,
									"Tồn tại hóa đơn có ngày phát hành nhỏ hơn ngày " + latestNLap));
							return rsp;
						}

					}
					if (localDateOfInvoice.compareTo(currentLocalDate) > 0) {
						rsp.setResponseStatus(new MspResponseStatus(9999,
								"Ngày phát hành của hóa đơn không được lớn hơn ngày hiện tại."));
						return rsp;
					}

					pipeline = new ArrayList<Document>();
					pipeline.add(new Document("$match",
							new Document("$expr",
									new Document("$and",
											Arrays.asList(new Document("$eq", Arrays.asList("$IssuerId", vIssuerId)),
													new Document("$eq",
															Arrays.asList("$EInvoiceDetail.TTChung.MauSoHD", mauSoHD)),
													new Document("$ne", Arrays.asList("$IsDelete", true)),
													new Document("$in",
															Arrays.asList("$EInvoiceStatus",
																	Arrays.asList("COMPLETE", "ERROR_CQT", "PROCESSING",
																			"PENDING", "XOABO", "DELETED", "REPLACED",
																			"ADJUSTED"))))))));
					pipeline.add(new Document("$group", new Document("_id", "$EInvoiceDetail.TTChung.MauSoHD")
							.append("MaxSHDon", new Document("$max", "$EInvoiceDetail.TTChung.SHDon"))));

					Document docInfo1 = null;
					mongoClient = cfg.mongoClient();
					collection = mongoClient.getDatabase(cfg.dbName).getCollection("EInvoiceMTT");
					try {
						docInfo1 = collection.aggregate(pipeline).allowDiskUse(true).iterator().next();
					} catch (Exception e) {
						logger.error("Error when access mongoDB: ", e.getMessage(), e);
					}
					mongoClient.close();

					// check shd of invoice
					String dir = docTmp.get("Dir", "");
					String fileName = "";
					String check_file_signed = docTmp.get("SignStatusCode", "NOSIGN");
					String idOfInvoice = docTmp.getObjectId("_id").toString();
					String loaiHD = docTmp.getEmbedded(Arrays.asList("EInvoiceDetail", "TTChung", "LoaiHD"), "");

					if (check_file_signed.equals("SIGNED")) {
						fileName = idOfInvoice + "_signed.xml";
					} else {
						fileName = idOfInvoice + ".xml";
					}

					File file = new File(dir, fileName);
					if (!file.exists()) {
						rsp.setResponseStatus(new MspResponseStatus(9999, "Tập tin không tồn tại."));
						return rsp;
					}

					int shdOfCurrentInvoice = docTmp.getEmbedded(Arrays.asList("EInvoiceDetail", "TTChung", "SHDon"),
							0);
					int invoiceNumber = 0;
					if (shdOfCurrentInvoice == 0) {
						int maxInvoiceNumber = 0;
						if (docInfo1.get("MaxSHDon") != null)
							maxInvoiceNumber = docInfo1.getEmbedded(Arrays.asList("MaxSHDon"), 0);
						invoiceNumber = maxInvoiceNumber + 1;
					}

					/* DOC DU LIEU XML, VA GHI DU LIEU VO SO HD VA MA CQT */
					org.w3c.dom.Document doc = commons.fileToDocument(file);
					XPath xPath = XPathFactory.newInstance().newXPath();
					Node nodeDLHDon = (Node) xPath.evaluate("/HDon/DLHDon[@Id='data']", doc, XPathConstants.NODE);
					Node nodeMaCQT = (Node) xPath.evaluate("/HDon", doc, XPathConstants.NODE);
					Node nodeTmp = null;
					nodeTmp = (Node) xPath.evaluate("TTChung", nodeDLHDon, XPathConstants.NODE);
					Element elementSub = (Element) xPath.evaluate("SHDon", nodeTmp, XPathConstants.NODE);
					if (null == elementSub) {
						elementSub = doc.createElement("SHDon");
						elementSub.setTextContent(String.valueOf(invoiceNumber));
						nodeTmp.appendChild(elementSub);
					} else {
						elementSub.setTextContent(String.valueOf(invoiceNumber));
					}

					Document docIssuerInfo = null;
					Document findUser = new Document("_id", objectIssuerId).append("IsActive", true).append("IsDelete",
							new Document("$ne", true));

					mongoClient = cfg.mongoClient();
					collection = mongoClient.getDatabase(cfg.dbName).getCollection("Issuer");
					try {
						docIssuerInfo = collection.find(findUser).allowDiskUse(true).iterator().next();
					} catch (Exception ex) {

					}
					mongoClient.close();

					if (null == docIssuerInfo) {
						rsp.setResponseStatus(new MspResponseStatus(9999, "Không tìm thấy thông tin người dùng."));
						return rsp;
					}

					String MaCQT = docIssuerInfo.get("MaCQT", "");
					if (MaCQT.equals("")) {
						rsp.setResponseStatus(new MspResponseStatus(9999, "Vui lòng cập nhật mã doanh nghiệp!!!"));
						return rsp;
					}

					/* CHECK HOA DON DA CO MA CQT CHUA */
					String MCCQT = docTmp.get("MCCQT", "");

					if (MCCQT.equals("")) {
						/* TAO prefixUserID CAP NHAT SO LogNextSequence DB */
						String prefixUserID = "";

						mongoClient = cfg.mongoClient();

						int seq = getValueForNextSequenceAdmin(mongoTemplate, header.getIssuerId(),
								Constants.NEXT_SEQUENCE.EINVOICEMTT, MaCQT);
						prefixUserID = StringUtils.leftPad(String.valueOf(seq), 11, "0");

						// GHEP MA CQT

						String NgayLap = LocalDate.now().toString();

						String[] split_year = NgayLap.split("-");

						String year_ = split_year[0];

						String year = year_.substring(2, 4);

						String maCQT = "M" + loaiHD + "-" + year + "-" + MaCQT + "-" + prefixUserID;

						Element elementSub_cqt = (Element) xPath.evaluate("MCCQT", nodeMaCQT, XPathConstants.NODE);
						if (null == elementSub_cqt) {
							elementSub_cqt = doc.createElement("MCCQT");
							elementSub_cqt.setTextContent(String.valueOf(maCQT));
							nodeMaCQT.appendChild(elementSub_cqt);
						} else {
							elementSub_cqt.setTextContent(String.valueOf(maCQT));
						}

						FileInfo fileInfo = new FileInfo();
						fileInfo.setFileName(fileName);
						fileInfo.setContentFile(commons.docW3cToByte(doc));

						// SAVE FILE
						String filename_publish = idOfInvoice + "_pending.xml";
						File file_publish = new File(dir, filename_publish);
						FileUtils.writeByteArrayToFile(new File(file_publish.toString()), fileInfo.getContentFile());

						/* UPDATE EINVOICE - STATUS */
						options = new FindOneAndUpdateOptions();
						options.upsert(false);
						options.maxTime(5000, TimeUnit.MILLISECONDS);
						options.returnDocument(ReturnDocument.AFTER);

						mongoClient = cfg.mongoClient();
						collection = mongoClient.getDatabase(cfg.dbName).getCollection("EInvoiceMTT");
						collection.findOneAndUpdate(docFind,
								new Document("$set",
										new Document("EInvoiceDetail.TTChung.SHDon", invoiceNumber)
												.append("MCCQT", maCQT).append("PublishStatus", true)
												.append("EInvoiceStatus", "PENDING")),
								options);
						mongoClient.close();

						// Update DMMauSoKyHieu
						if (shdOfCurrentInvoice == 0) {
							String idDMMSKH = docTmp.getEmbedded(Arrays.asList("DMMauSoKyHieu", "_id"), ObjectId.class)
									.toString();
							int getSL = docTmp.getEmbedded(Arrays.asList("DMMauSoKyHieu", "SoLuong"), 0);
							int CL = getSL - invoiceNumber;
							ObjectId objectIdMS = null;
							try {
								objectIdMS = new ObjectId(idDMMSKH);
							} catch (Exception e) {
							}
							Document docFindMS = null;
							docFindMS = new Document("IssuerId", header.getIssuerId())
									.append("IsDelete", new Document("$ne", true)).append("IsActive", true)
									.append("_id", objectIdMS);

							mongoClient = cfg.mongoClient();
							collection = mongoClient.getDatabase(cfg.dbName).getCollection("DMMauSoKyHieu");
							collection.findOneAndUpdate(docFindMS,
									new Document("$set", new Document("ConLai", CL).append("SHDHT", invoiceNumber)),
									options);
							mongoClient.close();
						}
					}
				}
			} finally {
				lock.unlock();
			}
		} else {
			rsp.setResponseStatus(new MspResponseStatus(9999, "Tồn tại user đang thực hiện Ký, vui lòng thử lại."));
			return rsp;
		}

		rsp.setResponseStatus(new MspResponseStatus(0, "SUCCESS"));
		return rsp;
	}

	@Override
	public MsgRsp createTDSendTaxList(JSONRoot jsonRoot) throws Exception {
		Msg msg = jsonRoot.getMsg();
		MsgHeader header = msg.getMsgHeader();
		MsgPage page = msg.getMsgPage();
		Object objData = msg.getObjData();

		JsonNode jsonData = null;
		if (objData != null) {
			jsonData = Json.serializer().nodeFromObject(objData);
		} else {
			throw new Exception("Lỗi dữ liệu đầu vào");
		}

		MsgRsp rsp = new MsgRsp(header);
		rsp.setMsgPage(page);

		String soLuongHD = commons.getTextJsonNode(jsonData.at("/SL")).replaceAll("\\s", "0");

		ObjectId objectId = null;
		Document docFind = null;
		Document docTmp = null;
		org.w3c.dom.Document doc = null;
		List<Document> pipeline = null;
		for (JsonNode o : jsonData.at("/ids")) {
			String id = commons.getTextJsonNode(o);
			try {
				objectId = new ObjectId(id);
			} catch (Exception e) {
			}

			/* KIEM TRA THONG TIN HD CO TON TAI KHONG */
			docFind = new Document("IssuerId", header.getIssuerId()).append("IsDelete", new Document("$ne", true))
					.append("_id", objectId);
			pipeline = new ArrayList<Document>();
			pipeline.add(new Document("$match", docFind));

			docTmp = null;
			MongoClient mongoClient = cfg.mongoClient();
			MongoCollection<Document> collection = mongoClient.getDatabase(cfg.dbName).getCollection("EInvoiceMTT");
			try {
				docTmp = collection.aggregate(pipeline).allowDiskUse(true).iterator().next();
			} catch (Exception e) {

			}
			mongoClient.close();

			if (docTmp == null) {
				rsp.setResponseStatus(new MspResponseStatus(9999, "Không tìm thấy thông tin hóa đơn!!!"));
				return rsp;
			}

			File file = null;
			String dir = docTmp.get("Dir", "");
			String fileName = id + "_pending.xml";

			file = new File(dir, fileName);
			if (!file.exists() || !file.isFile()) {
				rsp.setResponseStatus(new MspResponseStatus(9999, "Không tìm thấy file thông tin hóa đơn!!!"));
				return rsp;
			}

			doc = null;
			doc = commons.fileToDocument(file, true);
			if (null == doc) {
				rsp.setResponseStatus(new MspResponseStatus(9999, "Không tìm thấy file thông tin hóa đơn!!!"));
				return rsp;
			}

			String MTDiep = docTmp.get("MTDiep", "");
			String MST = docTmp.getEmbedded(Arrays.asList("EInvoiceDetail", "NDHDon", "NBan", "MST"), "");

			/* TAO XML THONG DIEP GUI DI */
			DocumentBuilderFactory dbf1 = DocumentBuilderFactory.newInstance();
			DocumentBuilder db1 = dbf1.newDocumentBuilder();

			org.w3c.dom.Document docTD = db1.newDocument();
			docTD.setXmlStandalone(true);

			Element root = docTD.createElement("TDiep");
			docTD.appendChild(root);

			Element elementContent = docTD.createElement("TTChung");

			elementContent.appendChild(
					commons.createElementWithValue(docTD, "PBan", Constants.TDiep_TTChung_TCTN_VISNAM.PBan1));
			elementContent.appendChild(commons.createElementWithValue(docTD, "MNGui", SystemParams.MSTTCGP));
			elementContent.appendChild(commons.createElementWithValue(docTD, "MNNhan", SystemParams.MSTDVTN));
			elementContent.appendChild(commons.createElementWithValue(docTD, "MLTDiep", "206"));
			elementContent.appendChild(commons.createElementWithValue(docTD, "MTDiep", MTDiep));
			elementContent.appendChild(commons.createElementWithValue(docTD, "MTDTChieu", ""));
			elementContent.appendChild(commons.createElementWithValue(docTD, "MST", MST));
			elementContent.appendChild(commons.createElementWithValue(docTD, "SLuong", "1"));
			root.appendChild(elementContent);

			elementContent = docTD.createElement("DLieu");
			Element docElement = doc.getDocumentElement();
			Node copiedRoot = docTD.importNode(docElement, true);
			elementContent.appendChild(copiedRoot);
			root.appendChild(elementContent);

			boolean isSaved = false;
			String fileName_TD = id + "_preparingSendCQT.xml";

			try {
				isSaved = commons.docW3cToFile(docTD, dir, fileName_TD);
			} catch (Exception e) {
			}
			if (!isSaved) {
				rsp.setResponseStatus(new MspResponseStatus(9999, "Lưu tập tin gửi thuế không thành công."));
				return rsp;
			}
			/* CAP NHAT LAI TRANG THAI DANG CHO XU LY */
			FindOneAndUpdateOptions options = null;
			options = new FindOneAndUpdateOptions();
			options.upsert(false);
			options.maxTime(5000, TimeUnit.MILLISECONDS);
			options.returnDocument(ReturnDocument.AFTER);

			mongoClient = cfg.mongoClient();
			collection = mongoClient.getDatabase(cfg.dbName).getCollection("EInvoiceMTT");
			collection.findOneAndUpdate(docFind,
					new Document("$set", new Document("CreateTDStatus", true)
							.append("CreateTDStatus_Date", LocalDateTime.now()).append("InfoCreateTDStatus",
									new Document("Date", LocalDateTime.now()).append("UserID", header.getUserId())
											.append("UserName", header.getUserName())
											.append("UserFullName", header.getUserFullName()))),
					options);
			mongoClient.close();

		}

		rsp.setResponseStatus(new MspResponseStatus(0, "SUCCESS"));
		return rsp;
	}

	@Override
	public FileInfo getFileForSignAll(JSONRoot jsonRoot) throws Exception {
		FileInfo fileInfo = new FileInfo();
		Msg msg = jsonRoot.getMsg();
		MsgHeader header = msg.getMsgHeader();
		Object objData = msg.getObjData();

		JsonNode jsonData = null;
		if (objData != null) {
			jsonData = Json.serializer().nodeFromObject(objData);
		} else {
			throw new Exception("Lỗi dữ liệu đầu vào");
		}
		List<GetXMLInfoXMLDTO> arrFileInfos = new ArrayList<>();
		List<Long> invoiceNumbers = new ArrayList<Long>();
		ObjectId objectId = null;
		Document fillter = null;
		Document docFind = null;
		Document docTmp = null;
		List<Document> pipeline = null;
		String idMauSoKyHieu = "";
		for (JsonNode o : jsonData.at("/ids")) {
			String id = commons.getTextJsonNode(o);
			try {
				objectId = new ObjectId(id);
			} catch (Exception e) {
			}

			docFind = new Document("IssuerId", header.getIssuerId()).append("IsDelete", new Document("$ne", true))
					.append("_id", objectId)
					.append("EInvoiceStatus", new Document("$in", Arrays.asList("CREATED", "PENDING")))
					.append("SignStatusCode", new Document("$in", Arrays.asList("NOSIGN", "PROCESSING")));
			fillter = new Document("_id", 1).append("EInvoiceDetail", 1).append("IssuerId", 1).append("Dir", 1)
					.append("SignStatusCode", 1).append("MCCQT", 1).append("FileNameXML", 1);

			pipeline = new ArrayList<Document>();
			pipeline.add(new Document("$match", docFind));
			pipeline.add(new Document("$project", fillter));

			// check info MSHD
			pipeline.add(new Document("$lookup", new Document("from", "DMMauSoKyHieu")
					.append("let",
							new Document("vMauSoHD", "$EInvoiceDetail.TTChung.MauSoHD").append("vIssuerId",
									"$IssuerId"))
					.append("pipeline", Arrays.asList(new Document("$match", new Document("$expr", new Document("$and",
							Arrays.asList(new Document("$gt", Arrays.asList("$ConLai", 0)),
									new Document("$eq", Arrays.asList("$IsActive", true)),
									new Document("$ne", Arrays.asList("$IsDelete", true)),
									new Document("$eq", Arrays.asList(new Document("$toString", "$_id"), "$$vMauSoHD")),
									new Document("$eq", Arrays.asList("$IssuerId", "$$vIssuerId"))))))))
					.append("as", "DMMauSoKyHieu")));
			pipeline.add(new Document("$unwind",
					new Document("path", "$DMMauSoKyHieu").append("preserveNullAndEmptyArrays", true)));

			MongoClient mongoClient = cfg.mongoClient();
			MongoCollection<Document> collection = mongoClient.getDatabase(cfg.dbName).getCollection("EInvoiceMTT");
			docTmp=null;
			try {
				docTmp = collection.aggregate(pipeline).allowDiskUse(true).iterator().next();
			} catch (Exception e) {
				logger.error("Error when access mongoDB: ", e.getMessage(), e);
			}
			mongoClient.close();

			if (null == docTmp) {
				fileInfo.setCheck("Không tìm thấy thông tin hóa đơn.");
				return fileInfo;
			}

			String idMauSo = docTmp.getEmbedded(Arrays.asList("DMMauSoKyHieu", "_id"), ObjectId.class).toString();

			if (idMauSoKyHieu == "")
				idMauSoKyHieu = idMauSo;

			if (idMauSoKyHieu != "" && !idMauSoKyHieu.equals(idMauSo)) {
				fileInfo.setCheck("Ký hàng loạt chỉ hỗ trợ kí cùng 1 mẫu số.");
				return fileInfo;
			}

			int invoiceNumber = docTmp.getEmbedded(Arrays.asList("EInvoiceDetail", "TTChung", "SHDon"), 0);
			invoiceNumbers.add((long) invoiceNumber);

			String dir = docTmp.get("Dir", "");
			String fileName = docTmp.get("FileNameXML", "");

			String fileNamePending = id + "_preparingSendCQT.xml";
			File file = new File(dir, fileNamePending);

			if (!file.exists()) {
				fileInfo.setCheck("File gửi cơ quan thuế không tồn tại.");
				return fileInfo;
			}
			org.w3c.dom.Document doc = commons.fileToDocument(file);
			GetXMLInfoXMLDTO getXMLInfoXMLDTO = null;
			getXMLInfoXMLDTO = new GetXMLInfoXMLDTO();
			getXMLInfoXMLDTO.setFileName(fileName);
			getXMLInfoXMLDTO.setFileData(commons.docW3cToByte(doc));
			getXMLInfoXMLDTO.setShd(invoiceNumber);
			getXMLInfoXMLDTO.setIDMSHDon(idMauSoKyHieu);
			arrFileInfos.add(getXMLInfoXMLDTO);

		}

		fileInfo.setCheck("");
		fileInfo.setFormIssueInvoiceID(idMauSoKyHieu);
		fileInfo.setNumbers(invoiceNumbers);
		fileInfo.setArrFileInfos(arrFileInfos);

		return fileInfo;
	}

	@Override
	public MsgRsp checkSHDList(JSONRoot jsonRoot) throws Exception {
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

		// get ids of shd
		List<ObjectId> objectIds = new ArrayList<>();
		for (JsonNode o : jsonData.at("/ids")) {
			String _id = commons.getTextJsonNode(o);
			try {
				objectIds.add(new ObjectId(_id));
			} catch (Exception e) {
			}
		}

		if (objectIds.isEmpty()) {
			rsp.setResponseStatus(new MspResponseStatus(9999, "Lỗi khi lấy dữ liệu đầu vào"));
			return rsp;
		}

		// get info to check
		List<Document> pipeline = null;
		Document docFind = new Document("IssuerId", header.getIssuerId()).append("IsDelete", false).append("_id",
				objectIds.get(0));
		pipeline = new ArrayList<Document>();
		pipeline.add(new Document("$match", docFind));
		pipeline.add(
				new Document("$project", new Document("_id", 1).append("EInvoiceDetail", 1).append("IssuerId", 1)));

		// get release data of invoices
		pipeline.add(
				new Document("$lookup",
						new Document("from", "EInvoiceMTT")
								.append("let",
										new Document("ids", objectIds))
								.append("pipeline", Arrays.asList(
										new Document("$match",
												new Document("$expr",
														new Document("$in", Arrays.asList("$_id", "$$ids")))),
										new Document("$project", new Document("EInvoiceDetail", 1).append("_id", 1))))
								.append("as", "InvoiceDetailList")));
		// get missing invoices
		pipeline.add(new Document("$addFields",
				new Document("minSHDon", new Document("$min", "$InvoiceDetailList.EInvoiceDetail.TTChung.SHDon"))
						.append("maxSHDon", new Document("$max", "$InvoiceDetailList.EInvoiceDetail.TTChung.SHDon"))));
		pipeline.add(new Document("$lookup", new Document("from", "EInvoiceMTT")
				.append("let",
						new Document("vIssuerId", "$IssuerId").append("vMauSo", "$EInvoiceDetail.TTChung.MauSoHD")
								.append("minSHD", "$minSHDon").append("maxSHD", "$maxSHDon")
								.append("existingSHD", "$InvoiceDetailList.EInvoiceDetail.TTChung.SHDon"))
				.append("pipeline", Arrays.asList(
						new Document("$match", new Document("$expr", new Document("$and", Arrays.asList(
								new Document("$eq", Arrays.asList("$IssuerId", "$$vIssuerId")),
								new Document("$eq", Arrays.asList("$EInvoiceDetail.TTChung.MauSoHD", "$$vMauSo")),
								new Document("$ne", Arrays.asList("$IsDelete", true)),
								new Document("$gte", Arrays.asList("$EInvoiceDetail.TTChung.SHDon", "$$minSHD")),
								new Document("$lte", Arrays.asList("$EInvoiceDetail.TTChung.SHDon", "$$maxSHD")),
								new Document("$not",
										new Document("$in",
												Arrays.asList("$EInvoiceDetail.TTChung.SHDon", "$$existingSHD"))),
								new Document("$not",
										new Document("$in",
												Arrays.asList("$EInvoiceStatus",
														Arrays.asList("COMPLETE", "ERROR_CQT", "PROCESSING", "XOABO",
																"DELETED", "REPLACED", "ADJUSTED")))))))),
						new Document("$project", new Document("EInvoiceDetail", 1).append("_id", 1))))
				.append("as", "MissingInvoices")));

		// get the largest release date of the invoice signed
		pipeline.add(new Document("$lookup", new Document("from", "EInvoiceMTT")
				.append("let",
						new Document("vIssuerId", "$IssuerId").append("vMauSo", "$EInvoiceDetail.TTChung.MauSoHD"))
				.append("pipeline", Arrays.asList(
						new Document("$match",
								new Document("$expr", new Document("$and",
										Arrays.asList(new Document("$eq", Arrays.asList("$IssuerId", "$$vIssuerId")),
												new Document("$eq",
														Arrays.asList("$EInvoiceDetail.TTChung.MauSoHD", "$$vMauSo")),
												new Document("$ne", Arrays.asList("$IsDelete", true)),
												new Document("$in", Arrays.asList("$EInvoiceStatus",
														Arrays.asList("COMPLETE", "ERROR_CQT", "PROCESSING", "XOABO",
																"DELETED", "REPLACED", "ADJUSTED"))))))),
						new Document("$group",
								new Document("_id", "$EInvoiceDetail.TTChung.MauSoHD").append("RecentNlap",
										new Document("$max", "$EInvoiceDetail.TTChung.NLap")))))
				.append("as", "MaxNlapInfo")));

		// get the smallest invoice number
		pipeline.add(new Document("$lookup", new Document("from", "EInvoiceMTT")
				.append("let",
						new Document("vIssuerId", "$IssuerId").append("vMauSo", "$EInvoiceDetail.TTChung.MauSoHD"))
				.append("pipeline", Arrays.asList(
						new Document("$match", new Document("$expr", new Document("$and",
								Arrays.asList(new Document("$eq", Arrays.asList("$IssuerId", "$$vIssuerId")),
										new Document("$eq",
												Arrays.asList("$EInvoiceDetail.TTChung.MauSoHD", "$$vMauSo")),
										new Document("$ne", Arrays.asList("$IsDelete", true)),
										new Document("$not",
												new Document("$in", Arrays.asList("$EInvoiceStatus",
														Arrays.asList("COMPLETE", "ERROR_CQT", "PROCESSING", "XOABO",
																"DELETED", "REPLACED", "ADJUSTED")))))))),
						new Document("$group",
								new Document("_id", "$EInvoiceDetail.TTChung.MauSoHD").append("MinSHDon",
										new Document("$min", "$EInvoiceDetail.TTChung.SHDon")))))
				.append("as", "MinSHDonInfo")));
		pipeline.add(new Document("$unwind",
				new Document("path", "$MinSHDonInfo").append("preserveNullAndEmptyArrays", true)));
		pipeline.add(new Document("$unwind",
				new Document("path", "$MaxNlapInfo").append("preserveNullAndEmptyArrays", true)));

		Document docTmp = null;
		MongoClient mongoClient = cfg.mongoClient();
		MongoCollection<Document> collection = mongoClient.getDatabase(cfg.dbName).getCollection("EInvoiceMTT");

		try {
			docTmp = collection.aggregate(pipeline).allowDiskUse(true).iterator().next();
		} catch (Exception e) {

		}

		mongoClient.close();
		if (null == docTmp) {
			responseStatus = new MspResponseStatus(9999, "Không tìm thấy thông tin hóa đơn.");
			rsp.setResponseStatus(responseStatus);
			return rsp;
		}

		int minInvoiceNumber = 0;
		if (docTmp.get("MinSHDonInfo") != null) {
			minInvoiceNumber = docTmp.getEmbedded(Arrays.asList("MinSHDonInfo", "MinSHDon"), 0);
		}

		LocalDate latestNLap = null;
		if (docTmp.get("MaxNlapInfo") != null) {
			latestNLap = commons
					.convertDateToLocalDate(docTmp.getEmbedded(Arrays.asList("MaxNlapInfo", "RecentNlap"), Date.class));
		}

		List<Document> invoiceDetailList = new ArrayList<>();
		if (docTmp.containsKey("InvoiceDetailList") && docTmp.get("InvoiceDetailList") instanceof List) {
			invoiceDetailList = docTmp.getList("InvoiceDetailList", Document.class);
		}
		Map<Integer, LocalDate> tempMap = new HashMap<Integer, LocalDate>();
		if (invoiceDetailList != null) {
			int shd = 0;
			LocalDate date = null;
			for (Document doc : invoiceDetailList) {
				shd = doc.getEmbedded(Arrays.asList("EInvoiceDetail", "TTChung", "SHDon"), 0);
				date = commons.convertDateToLocalDate(
						doc.getEmbedded(Arrays.asList("EInvoiceDetail", "TTChung", "NLap"), Date.class));
				tempMap.put(shd, date);
			}

			// check not exist smallest invoice number of invoice set have number
			if (!tempMap.containsKey(minInvoiceNumber)) {
				rsp.setResponseStatus(new MspResponseStatus(9999,
						"Ký các hóa đơn không thành công. Vui lòng ký trước hoặc ký bao gồm hóa đơn "
								+ commons.formatNumberBillInvoice(minInvoiceNumber)));
				return rsp;
			}
		} else {
			rsp.setResponseStatus(new MspResponseStatus(9999, "Không tìm thấy thông tin các hóa đơn."));
			return rsp;
		}

		// check not missing invoice in invoice set from smallest invoice number to
		// largest invoice number
		List<Document> missingInvoices = new ArrayList<Document>();
		if (docTmp.containsKey("MissingInvoices") && docTmp.get("MissingInvoices") instanceof List) {
			missingInvoices = docTmp.getList("MissingInvoices", Document.class);
		}

		StringBuffer sb = new StringBuffer();
		if (missingInvoices.size() > 0) {
			for (Document doc : missingInvoices) {
				sb.append(commons.formatNumberBillInvoice(
						doc.getEmbedded(Arrays.asList("EInvoiceDetail", "TTChung", "SHDon"), 0)));
				sb.append(" ");
			}
			rsp.setResponseStatus(new MspResponseStatus(9999,
					"Ký các hóa đơn không thành công. Vui lòng ký các hóa đơn liền kề. ( thiếu hóa đơn " + sb.toString()
							+ ")"));
			return rsp;
		}

		// check contain release date < lastest release date
		sb = new StringBuffer();
		if (latestNLap != null) {
			for (Map.Entry<Integer, LocalDate> entry : tempMap.entrySet()) {
				if (commons.compareLocalDate(entry.getValue(), latestNLap) < 0) {
					sb.append(commons.formatNumberBillInvoice(entry.getKey()));
					sb.append("");
				}
			}
		}

		if (sb.length() > 0) {
			rsp.setResponseStatus(new MspResponseStatus(9999, "Tồn tại hóa đơn " + sb.toString()
					+ "có ngày phát hành nhỏ hơn ngày phát hành của hóa đơn đã ký gần nhất " + latestNLap));
			return rsp;
		}

		responseStatus = new MspResponseStatus(0, "SUCCESS");
		rsp.setResponseStatus(responseStatus);
		return rsp;
	}

	@Override
	public MsgRsp signAll(UpdateSignedMultiBillReq input, JSONRoot jsonRoot) throws Exception {
		Msg msg = jsonRoot.getMsg();
		MsgHeader header = msg.getMsgHeader();
		MsgPage page = msg.getMsgPage();
		MsgRsp rsp = new MsgRsp(header);
		rsp.setMsgPage(page);
		MspResponseStatus responseStatus = null;

		List<String> DsFile = new ArrayList<>();

		int count = 0;
		String hdError = "";
		String mauso = input.getFormIssueInvoiceID();
		String taxCode = input.getTaxcode();
		String[] split_mst = taxCode.split("_");
		int dem_mst = split_mst.length;
		if (dem_mst == 2) {
			taxCode = split_mst[0];
		}

		Path path = Paths.get(SystemParams.DIR_E_INVOICE_DATA, taxCode, mauso);
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
				responseStatus = new MspResponseStatus(9999, "Không tìm thấy file hóa đơn thực hiện ký.");
				rsp.setResponseStatus(responseStatus);
				return rsp;
			}

			org.w3c.dom.Document xmlDoc = commons.fileToDocument(fileKy);
			int eInvoiceNumber = 0;
			String keySystem = "";
			XPath xPath = XPathFactory.newInstance().newXPath();
			Node nodeDLHDon = (Node) xPath.evaluate("/TDiep/DLieu/HDon/DLHDon[@Id='data']", xmlDoc,
					XPathConstants.NODE);
			Node nodeTmp = (Node) xPath.evaluate("TTChung", nodeDLHDon, XPathConstants.NODE);

			eInvoiceNumber = commons.stringToInteger(
					commons.getTextFromNodeXML((Element) xPath.evaluate("SHDon", nodeTmp, XPathConstants.NODE)));

			String key = "";
			NodeList nodeList = (NodeList) xPath.evaluate("TTKhac/TTin", nodeTmp, XPathConstants.NODESET);
			for (int i = 0; i < nodeList.getLength(); i++) {
				key = commons
						.getTextFromNodeXML((Element) xPath.evaluate("TTruong", nodeList.item(i), XPathConstants.NODE));
				if ("SystemKey".equals(key)) {
					keySystem = commons.getTextFromNodeXML(
							(Element) xPath.evaluate("DLieu", nodeList.item(i), XPathConstants.NODE));
					break;
				}
			}

			/* LAY THONG TIN NGAY LAP - NGAY KY TRONG FILE XML */
			String NLap = commons.getTextFromNodeXML((Element) xPath.evaluate("NLap", nodeTmp, XPathConstants.NODE));

			LocalDate ldNLap = null;
			try {
				ldNLap = commons.convertStringToLocalDate(NLap, "yyyy-MM-dd");
			} catch (Exception e) {
			}

			ObjectId objectId = null;
			try {
				objectId = new ObjectId(keySystem);
			} catch (Exception e) {
			}
			List<Document> pipeline = null;
			/* KIEM TRA THONG TIN HOP LE KHONG */
			Document docFind = new Document("IssuerId", header.getIssuerId()).append("IsDelete", false)
					.append("EInvoiceDetail.TTChung.SHDon", eInvoiceNumber).append("EInvoiceStatus", "PENDING")
					.append("_id", objectId);
			pipeline = new ArrayList<Document>();
			pipeline.add(new Document("$match", docFind));
			pipeline.add(new Document("$lookup",
					new Document("from", "EInvoiceMTT").append("pipeline",
							Arrays.asList(new Document("$match",
									new Document("IssuerId", header.getIssuerId())
											.append("EInvoiceDetail.TTChung.MauSoHD", mauso)
											.append("EInvoiceDetail.TTChung.SHDon", eInvoiceNumber)
											.append("IsActive", true).append("IsDelete", false))

							)).append("as", "EInvoiceInfo")));
			pipeline.add(new Document("$unwind",
					new Document("path", "$EInvoiceInfo").append("preserveNullAndEmptyArrays", true)));
			Document docTmp = null;
			MongoClient mongoClient = cfg.mongoClient();
			MongoCollection<Document> collection = mongoClient.getDatabase(cfg.dbName).getCollection("EInvoiceMTT");
			try {
				docTmp = collection.aggregate(pipeline).allowDiskUse(true).iterator().next();
			} catch (Exception e) {

			}

			mongoClient.close();
			if (null == docTmp) {
				count += 1;
				hdError += eInvoiceNumber + ",";
				break;
			}
			if (docTmp.get("EInvoiceInfo") != null) {
				count += 1;
				hdError += eInvoiceNumber + ",";
				break;
			}

			String signStatusCode = docTmp.get("SignStatusCode", "");
			if ("PROCESSING".equals(signStatusCode)) {
				count += 1;
				hdError += eInvoiceNumber + ",";
				break;
			}

			/* KIEM TRA NGAY LAP TRONG HE THONG - NGAY LAP TRONG XML */
			LocalDate ldNLapSystem = commons.convertDateToLocalDate(
					docTmp.getEmbedded(Arrays.asList("EInvoiceDetail", "TTChung", "NLap"), Date.class));
			if (commons.compareLocalDate(ldNLap, ldNLapSystem) != 0) {
				count += 1;
				hdError += eInvoiceNumber + ",";
				break;
			}

			/* CAP NHAT DB */
			FindOneAndUpdateOptions options = new FindOneAndUpdateOptions();
			options.upsert(false);
			options.maxTime(5000, TimeUnit.MILLISECONDS);
			options.returnDocument(ReturnDocument.AFTER);
			objectId = null;
			try {
				objectId = new ObjectId(docTmp.getEmbedded(Arrays.asList("EInvoiceDetail", "TTChung", "MauSoHD"), ""));
			} catch (Exception e) {
			}
			if (null == objectId) {
				throw new Exception("Không tìm thấy mẫu số hóa đơn.");
			}

			mongoClient = cfg.mongoClient();
			collection = mongoClient.getDatabase(cfg.dbName).getCollection("EInvoiceMTT");
			collection.findOneAndUpdate(docFind,
					new Document("$set", new Document("SignStatusCode", "SIGNED").append("InfoSigned",
							new Document("SignedDate", LocalDateTime.now()).append("SignedUserID", header.getUserId())
									.append("SignedUserName", header.getUserName())
									.append("SignedUserFullName", header.getUserFullName()))),
					options);
			mongoClient.close();
		}
		if (count != 0) {
			responseStatus = new MspResponseStatus(9999, count + " Hóa đơn ký không thành công là : " + hdError);
			rsp.setResponseStatus(responseStatus);
			return rsp;
		}
		responseStatus = new MspResponseStatus(0, Constants.MAP_ERROR.get(0));
		rsp.setResponseStatus(responseStatus);
		return rsp;
	}

	@Override
	public MsgRsp sendMailAll(JSONRoot jsonRoot) throws Exception {
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
		String NBanTen = "";
		String SecretKey = "";
		String EmailAddress = "";
		String MailJet = "";
		String check_mail = "";
		MailConfig mailConfig = null;

		ObjectId objectId = null;
		for (JsonNode o : jsonData.at("/_ids")) {
			String _id = commons.getTextJsonNode(o);
			objectId = null;
			try {
				objectId = new ObjectId(_id);
			} catch (Exception e) {
				continue;
			}

			docFind = new Document("IssuerId", header.getIssuerId())
					.append("MCCQT", new Document("$exists", true).append("$ne", null)).append("_id", objectId);

			pipeline = new ArrayList<Document>();
			pipeline.add(new Document("$match", docFind));

			pipeline.add(
					new Document("$lookup",
							new Document("from", "ConfigEmail").append("let", new Document("vIssuerId", "$IssuerId"))
									.append("pipeline", Arrays.asList(new Document("$match",
											new Document("$expr",
													new Document("$eq", Arrays.asList("$IssuerId", "$$vIssuerId"))))))
									.append("as", "ConfigEmail")));
			pipeline.add(new Document("$unwind",
					new Document("path", "$ConfigEmail").append("preserveNullAndEmptyArrays", true)));

			pipeline.add(new Document("$lookup", new Document("from", "DMMauSoKyHieu")
					.append("let",
							new Document("vIssuerId", "$IssuerId").append("vMauSoHD",
									"$EInvoiceDetail.TTChung.MauSoHD"))
					.append("pipeline", Arrays.asList(new Document("$match", new Document("$expr", new Document("$and",
							Arrays.asList(new Document("$eq", Arrays.asList("$$vIssuerId", "$IssuerId")),
									new Document("$eq",
											Arrays.asList(new Document("$toString", "$_id"), "$$vMauSoHD"))))))))
					.append("as", "DMMauSoKyHieu")));
			pipeline.add(new Document("$unwind",
					new Document("path", "$DMMauSoKyHieu").append("preserveNullAndEmptyArrays", true)));

			pipeline.add(new Document("$lookup",
					new Document("from", "UserConFig")
							.append("pipeline",
									Arrays.asList(new Document("$match",
											new Document("viewshd", "Y").append("IssuerId", header.getIssuerId()))))
							.append("as", "UserConFig")));
			pipeline.add(new Document("$unwind",
					new Document("path", "$UserConFig").append("preserveNullAndEmptyArrays", true)));

			pipeline.add(new Document("$lookup",
					new Document("from", "ConfigMailJet")
							.append("pipeline", Arrays.asList(new Document("$match", new Document("IsActive", true))))
							.append("as", "ConfigMailJet")));
			pipeline.add(new Document("$unwind",
					new Document("path", "$ConfigMailJet").append("preserveNullAndEmptyArrays", true)));

			pipeline.add(new Document("$lookup", new Document("from", "DMFooterWeb").append("pipeline",
					Arrays.asList(new Document("$match", new Document("IsActive", true).append("IsDelete", false)),
							new Document("$project", new Document("Noidung", 1)), new Document("$limit", 1)))
					.append("as", "DMFooterWeb")));
			pipeline.add(new Document("$unwind",
					new Document("path", "$DMFooterWeb").append("preserveNullAndEmptyArrays", true)));

			docTmp = null;
			MongoClient mongoClient = cfg.mongoClient();
			MongoCollection<Document> collection = mongoClient.getDatabase(cfg.dbName).getCollection("EInvoiceMTT");
			try {
				docTmp = collection.aggregate(pipeline).allowDiskUse(true).iterator().next();
			} catch (Exception e) {

			}

			mongoClient.close();
			if (null == docTmp) {
				continue;
			}

			if (docTmp.get("ConfigEmail") == null) {
				continue;
			}
			
			if(!commons.isValidEmailAddress(docTmp.getEmbedded(Arrays.asList("EInvoiceDetail", "NDHDon", "NMua", "DCTDTu"), ""))){
				continue;
			}

			String TaxCode = header.getUserName();
			String Name = header.getUserFullName();
			NBanTen = docTmp.getEmbedded(Arrays.asList("EInvoiceDetail", "NDHDon", "NBan", "Ten"), "");
			MailJet = docTmp.getEmbedded(Arrays.asList("ConfigEmail", "MailJet"), "");

			mailConfig = new MailConfig(docTmp.get("ConfigEmail", Document.class));
			mailConfig.setNameSend(NBanTen);

			ApiKey = docTmp.getEmbedded(Arrays.asList("ConfigMailJet", "ApiKey"), "");
			SecretKey = docTmp.getEmbedded(Arrays.asList("ConfigMailJet", "SecretKey"), "");
			EmailAddress = docTmp.getEmbedded(Arrays.asList("ConfigMailJet", "EmailAddress"), "");

			if (!NBanTen.equals("")) {
				if (MailJet.equals("Y") && !MailJet.equals("") && !MailJet.equals("N")) {
					check_mail = "MailJet";
				} else {
					check_mail = "MailServer";
				}

				Document docInsert = new Document("IssuerId", header.getIssuerId()).append("InfoServerID", uidTmp)
						.append("MailUsingType", check_mail).append("TaxCode", TaxCode).append("Name", Name)
						.append("Data", docTmp).append("FuncSend", Constants.SendMailFromFunc.Bulk)
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
		}
		if (!MailJet.equals("") && mailConfig != null && !NBanTen.equals("")) {

			if (check_mail.equals("MailJet")) {

				MongoClient mongoClient = cfg.mongoClient();
				MongoCollection<Document> collection = mongoClient.getDatabase(cfg.dbName)
						.getCollection("LogBulkEMailInfoServer");
				collection.insertOne(new Document("IssuerId", header.getIssuerId()).append("InfoServerID", uidTmp)
						.append("MailUsingType", "MailJet")
						.append("MailjetInfo", new Document("ApiKey", ApiKey).append("SecretKey", SecretKey)
								.append("EmailAddress", EmailAddress).append("NameSend", mailConfig.getNameSend())

						));
				mongoClient.close();

			} else {

				boolean IsAutoSend = mailConfig.isAutoSend();
				boolean IsSSL = mailConfig.isSSL();
				boolean IsTLS = mailConfig.isTLS();

				MongoClient mongoClient = cfg.mongoClient();
				MongoCollection<Document> collection = mongoClient.getDatabase(cfg.dbName)
						.getCollection("LogBulkEMailInfoServer");
				collection.insertOne(new Document("IssuerId", header.getIssuerId()).append("InfoServerID", uidTmp)
						.append("MailUsingType", check_mail).append("MailServer",
								new Document("SmtpServer", mailConfig.getSmtpServer())
										.append("Port", mailConfig.getSmtpPort())
										.append("EmailAddress", mailConfig.getEmailAddress())
										.append("PassWord", mailConfig.getEmailPassword())
										.append("NameSend", mailConfig.getNameSend()).append("IsAutoSend", IsAutoSend)
										.append("IsSSL", IsSSL).append("IsTLS", IsTLS))

				);
				mongoClient.close();
			}

			/* INSERT DATA TO QUEUE */
			try {
				TextMessage objectMessage = null;
				objectMessage = session.createTextMessage(uidTmp);
				producer.send((Message) objectMessage);
			} catch (Exception e) {
			}
		}
		responseStatus = new MspResponseStatus(0, Constants.MAP_ERROR.get(0));
		rsp.setResponseStatus(responseStatus);
		return rsp;
	}
}
