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
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import java.util.regex.Pattern;
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
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.ss.util.NumberToTextConverter;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
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
import vn.sesgroup.hddt.model.DSHHDVu;
import vn.sesgroup.hddt.model.EInvoiceExcelForm;
import vn.sesgroup.hddt.model.EInvoiceExcelFormMISA;
import vn.sesgroup.hddt.resources.JmsParams;
import vn.sesgroup.hddt.user.dao.AbstractDAO;
import vn.sesgroup.hddt.user.dao.EInvoiceDAO;
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
public class EInvoiceImpl extends AbstractDAO implements EInvoiceDAO {
	private static final Logger log = LogManager.getLogger(EInvoiceImpl.class);
	private final ConcurrentHashMap<String, ReentrantLock> issuerLocks_sign = new ConcurrentHashMap<>();
	@Autowired
	ConfigConnectMongo cfg;

	@Autowired
	private JmsTemplate jmsTemplate;

	@Autowired
	SendMailAsyncDAO sendMailAsyncDAO;
	@Autowired
	TCTNService tctnService;
	private MailUtils mailUtils = new MailUtils();

	private MailjetSender mailJet = new MailjetSender();
	@Autowired
	JPUtils jpUtils;
	Document docUpsert = null;

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
		List<Object> listVAT = new ArrayList<Object>();
		HashMap<String, Object> hItemVAT = null;
		String actionCode = header.getActionCode();
		String _id = commons.getTextJsonNode(jsonData.at("/_id")).replaceAll("\\s", "");
		String _id_tt_dc = commons.getTextJsonNode(jsonData.at("/_id_tt_dc")).replaceAll("\\s", "");
		String _tchdon = commons.getTextJsonNode(jsonData.at("/_tchdon")).trim().replaceAll("\\s+", " ");
		String mauSoHdon = commons.getTextJsonNode(jsonData.at("/MauSoHdon")).replaceAll("\\s", "");
		String maHoadon = commons.getTextJsonNode(jsonData.at("/MaHoaDon")).trim().replaceAll("\\s+", " ");
		String madvqhns = commons.getTextJsonNode(jsonData.at("/MaDVQHNS")).trim().replaceAll("\\s+", " ");
		String _token = commons.getTextJsonNode(jsonData.at("/_token"));
		String tenLoaiHd = commons.getTextJsonNode(jsonData.at("/TenLoaiHd")).trim().replaceAll("\\s+", " ");
		String ngayLap = commons.getTextJsonNode(jsonData.at("/NgayLap")).replaceAll("\\s", "");
		String hinhThucThanhToan = commons.getTextJsonNode(jsonData.at("/HinhThucThanhToan")).replaceAll("\\s", "");
		String hinhThucThanhToanText = commons.getTextJsonNode(jsonData.at("/HinhThucThanhToanText")).trim()
				.replaceAll("\\s+", " ");
//		String maCuaHang = commons.getTextJsonNode(jsonData.at("/MaCuaHang")).trim().replaceAll("\\s+", " ");
//		String tenCuaHang = commons.getTextJsonNode(jsonData.at("/TenCuaHang")).trim().replaceAll("\\s+", " ");
		
//		String chkXuatTheoLoaiTienTt = commons.getTextJsonNode(jsonData.at("/ChkXuatTheoLoaiTienTt")).replaceAll("\\s", "");
		String khMst = commons.getTextJsonNode(jsonData.at("/KhMst")).trim().replaceAll("\\s+", "")
				.replaceAll("[+^%$#@&*]*", "").replaceAll("[a-z][A-Z]*", "");
		String khMKHang = commons.getTextJsonNode(jsonData.at("/KhMKHang")).trim().replaceAll("\\s+", " ");
		String khHoTenNguoiMua = commons.getTextJsonNode(jsonData.at("/KhHoTenNguoiMua")).trim().replaceAll("\\s+",
				" ");
		String khTenDonVi = commons.getTextJsonNode(jsonData.at("/KhTenDonVi")).trim().replaceAll("\\s+", " ");
		String khDiaChi = commons.getTextJsonNode(jsonData.at("/KhDiaChi")).trim().replaceAll("\\s+", " ");
		String khEmail = commons.getTextJsonNode(jsonData.at("/KhEmail")).trim().replaceAll("\\s+", " ");
		String khEmailCC = commons.getTextJsonNode(jsonData.at("/KhEmailCC")).trim().replaceAll("\\s+", " ");
		String khSoDT = commons.getTextJsonNode(jsonData.at("/KhSoDt")).trim().replaceAll("\\s+", " ");
		String khSoTk = commons.getTextJsonNode(jsonData.at("/KhSoTk")).trim().replaceAll("\\s+", " ");
		String khTkTaiNganHang = commons.getTextJsonNode(jsonData.at("/KhTkTaiNganHang")).trim().replaceAll("\\s+",
				" ");
		String khSoCCCD = commons.getTextJsonNode(jsonData.at("/KhSoCCCD")).trim().replaceAll("\\s+", "");
		String khSoHochieu = commons.getTextJsonNode(jsonData.at("/KhSoHoChieu")).trim().replaceAll("\\s+", "");
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
		String checkProductExtension = commons.getTextJsonNode(jsonData.at("/checkProductExtension")).trim()
				.replaceAll("\\s+", " ");
		String paramUSD = commons.getTextJsonNode(jsonData.at("/ParamUSD")).trim().replaceAll("\\s+", "");
		
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
		Document docFindNmua = null;
		Document docUpsert1 = null;
		ObjectId objectNMua = null;
		ObjectId objectIdQLNMua = null;

		String taxCode = "";
		HashMap<String, Double> mapVATAmount = null;
		HashMap<String, Double> mapAmount = null;
		List<Object> listDSHHDVu = new ArrayList<Object>();
		HashMap<String, Object> hItem = null;

		String MTDiep = "";
		String MST = "";

		String nlap_shd_max = null;
		DateTimeFormatter formatter = null;

		LocalDate localDate1 = null;
		LocalDate localDate2 = null;

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

			Document findInforIssuer = new Document("_id", 1).append("TaxCode", 1).append("Name", 1)
					.append("Address", 1).append("Phone", 1).append("Fax", 1).append("Email", 1).append("Website", 1)
					.append("TinhThanhInfo", 1).append("ChiCucThueInfo", 1).append("BankAccount", 1).append("NameEN", 1)
					.append("BankAccountExt", 1).append("HDDCKPTQuan", 1);

			pipeline = new ArrayList<Document>();
			pipeline.add(new Document("$match",
					new Document("_id", objectId).append("IsActive", true).append("IsDelete", false)));
			pipeline.add(new Document("$project", findInforIssuer));

			pipeline.add(new Document("$lookup", new Document("from", "Users").append("pipeline", Arrays.asList(
					new Document("$match",
							new Document("IssuerId", header.getIssuerId()).append("_id", objectIdUser)
									.append("IsActive", true).append("IsDelete", false)),
					new Document("$project", new Document("_id", 1).append("UserName", 1).append("FullName", 1)),
					new Document("$limit", 1))).append("as", "UserInfo")));
			pipeline.add(new Document("$unwind",
					new Document("path", "$UserInfo").append("preserveNullAndEmptyArrays", true)));

			pipeline.add(new Document("$lookup", new Document("from", "DMMauSoKyHieu")
					.append("pipeline", Arrays.asList(
							new Document(
									"$match",
									new Document("IssuerId", header.getIssuerId()).append("IsActive", true)
											.append("IsDelete", false).append("ConLai", new Document("$gt", 0))
											.append("_id", objectIdMSKH)),
							new Document("$project", new Document("_id", 1).append("KHMSHDon", 1).append("KHHDon", 1))))
					.append("as", "DMMauSoKyHieu")));

			pipeline.add(new Document("$unwind",
					new Document("path", "$DMMauSoKyHieu").append("preserveNullAndEmptyArrays", true)));
			if (!"".equals(_id_tt_dc)) {
				pipeline.add(new Document("$lookup", new Document("from", "EInvoice")
						.append("pipeline", Arrays.asList(new Document("$match", new Document("_id", objectIdTT_DC)
								.append("IssuerId", header.getIssuerId())
								.append("EInvoiceStatus",
										new Document("$in", Arrays.asList(Constants.INVOICE_STATUS.COMPLETE,
												Constants.INVOICE_STATUS.ADJUSTED, Constants.INVOICE_STATUS.REPLACED)))
								.append("MCCQT", new Document("$exists", true))
//								.append("HDSS.TCTBao", new Document("$in", Arrays.asList("2", "3")))
								),
								new Document("$project",
										new Document("TT_DC", "$EInvoiceDetail.TTChung").append("HDSS", 1))))
						.append("as", "EInvoiceTTDC")));
				pipeline.add(new Document("$unwind",
						new Document("path", "$EInvoiceTTDC").append("preserveNullAndEmptyArrays", true)));
			}
			if (!"".equals(maHoadon)) {
				// CHECK MHDon
				pipeline.add(
						new Document("$lookup",
								new Document("from", "EInvoice")
										.append("pipeline",
												Arrays.asList(
														new Document("$match",
																new Document("EInvoiceDetail.TTChung.MaHD", maHoadon)
																		.append("EInvoiceDetail.TTChung.MauSoHD",
																				mauSoHdon)
																		.append("IssuerId", header.getIssuerId())
																		.append("IsDelete", false)),
														new Document("$project", new Document("_id", 1))))
										.append("as", "CheckMHDon")));
				pipeline.add(new Document("$unwind",
						new Document("path", "$CheckMHDon").append("preserveNullAndEmptyArrays", true)));
			}

			// KIEM TRA TRONG DANH SASH QUAN LY NGUOI MUA
			if (!"".equals(khMst)) {
				pipeline.add(new Document("$lookup",
						new Document("from", "QLDSNMua")
								.append("pipeline",
										Arrays.asList(new Document("$match",
												new Document("MST", khMst).append("IsDelete", false))))
								.append("as", "DSNMua")));
				pipeline.add(new Document("$unwind",
						new Document("path", "$DSNMua").append("preserveNullAndEmptyArrays", true)));
			}
			// CHECK NGAY LAP SO HOA DON LON NHAT
			pipeline.add(new Document("$lookup",
					new Document("from", "EInvoice")
							.append("pipeline", Arrays.asList(
									new Document("$match",
											new Document("IsDelete", false).append("EInvoiceDetail.TTChung.MauSoHD",
													mauSoHdon)),
									new Document("$group",
											new Document("_id", "$EInvoiceDetail.TTChung.MauSoHD").append("SHDon",
													new Document("$max", "$EInvoiceDetail.TTChung.SHDon")))))
							.append("as", "NLap_MAX")));
			pipeline.add(new Document("$unwind",
					new Document("path", "$NLap_MAX").append("preserveNullAndEmptyArrays", true)));
			// END CHECK NGAY LAP
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
			String link = docTmp.getEmbedded(Arrays.asList("PramLink", "LinkPortal"), "");
			if (docTmp.get("UserInfo") == null) {
				responseStatus = new MspResponseStatus(9999, "Không tìm thấy thông tin người dùng.");
				rsp.setResponseStatus(responseStatus);
				return rsp;
			}
			if (docTmp.get("DMMauSoKyHieu") == null) {
				responseStatus = new MspResponseStatus(9999, "Hết số lượng hóa đơn.");
				rsp.setResponseStatus(responseStatus);
				return rsp;
			}
			if (docTmp.get("CheckMHDon") != null) {
				responseStatus = new MspResponseStatus(9999, "Mã hóa đơn đã tồn tại.");
				rsp.setResponseStatus(responseStatus);
				return rsp;
			}

			// LAY NGAY LAP SO HOA DON
			String nl_mshd = docTmp.getEmbedded(Arrays.asList("NLap_MAX", "_id"), "");
			int nl_shdon = docTmp.getEmbedded(Arrays.asList("NLap_MAX", "SHDon"), 0);

			Document docFindNLap = new Document("IssuerId", header.getIssuerId())
					.append("EInvoiceDetail.TTChung.SHDon", nl_shdon)
					.append("SignStatusCode", Constants.INVOICE_SIGN_STATUS.SIGNED)
					.append("EInvoiceStatus",
							new Document("$in",
									Arrays.asList(Constants.INVOICE_STATUS.COMPLETE, Constants.INVOICE_STATUS.PENDING,
											Constants.INVOICE_STATUS.ERROR_CQT, Constants.INVOICE_STATUS.PROCESSING,
											Constants.INVOICE_STATUS.ADJUSTED, Constants.INVOICE_STATUS.REPLACED)))
					.append("EInvoiceDetail.TTChung.MauSoHD", nl_mshd).append("IsDelete", false);

			Document docTmp2 = null;


			mongoClient = cfg.mongoClient();
			collection = mongoClient.getDatabase(cfg.dbName).getCollection("EInvoice");
			try {
				docTmp2 = collection.find(docFindNLap).iterator().next();
			} catch (Exception e) {

			}
			mongoClient.close();

			if (docTmp2 != null) {
				nlap_shd_max = commons.convertLocalDateTimeToString(
						commons.convertDateToLocalDateTime(
								docTmp2.getEmbedded(Arrays.asList("EInvoiceDetail", "TTChung", "NLap"), Date.class)),
						"dd/MM/yyyy");

				formatter = DateTimeFormatter.ofPattern("d/MM/yyyy");
				localDate1 = LocalDate.parse(ngayLap, formatter);
				localDate2 = LocalDate.parse(nlap_shd_max, formatter);

				if (localDate1.compareTo(localDate2) < 0) {
					responseStatus = new MspResponseStatus(9999, "Ngày lập hóa đơn không được nhỏ hơn ngày "
							+ nlap_shd_max + " của hóa đơn trước đó. Vui lòng chọn lại ngày lập.");
					rsp.setResponseStatus(responseStatus);
					return rsp;
				}
			}

			// END LAY NGAY LAP

			docEInvoiceTTDC = docTmp.get("EInvoiceTTDC", Document.class);

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
			doc.appendChild(root);

			elementContent = doc.createElement("DLHDon");
			elementContent.setAttribute("Id", "data");
			root.appendChild(elementContent);

			elementSubTmp = null;
			elementSubTmp01 = null;
			elementSubContent = doc.createElement("TTChung");
			elementTmp = null;

			elementSubContent.appendChild(commons.createElementWithValue(doc, "PBan", SystemParams.VERSION_XML));
			elementSubContent.appendChild(commons.createElementWithValue(doc, "THDon", tenLoaiHd));
			elementSubContent.appendChild(commons.createElementWithValue(doc, "KHMSHDon",
					docTmp.getEmbedded(Arrays.asList("DMMauSoKyHieu", "KHMSHDon"), "")));
			elementSubContent.appendChild(commons.createElementWithValue(doc, "KHHDon",
					docTmp.getEmbedded(Arrays.asList("DMMauSoKyHieu", "KHHDon"), "")));
			elementSubContent.appendChild(commons.createElementWithValue(doc, "SHDon", "")); // SE PHAT SINH KHI KY
			// Mã hồ sơ:
			elementSubContent.appendChild(commons.createElementWithValue(doc, "MHSo", ""));
			// Ngày lập
			elementSubContent.appendChild(
					commons.createElementWithValue(doc, "NLap", commons.convertLocalDateTimeStringToString(ngayLap,
							Constants.FORMAT_DATE.FORMAT_DATE_WEB, Constants.FORMAT_DATE.FORMAT_DATE_EINVOICE, false)));
			if(docTmp.get("HDDCKPTQuan") != null) {
				elementSubContent.appendChild(commons.createElementWithValue(doc, "HDDCKPTQuan", docTmp.get("HDDCKPTQuan", "")));	
			}
			// Số bảng kê
			elementSubContent.appendChild(commons.createElementWithValue(doc, "SBKe", ""));
			// Ngày bảng kê
			elementSubContent.appendChild(commons.createElementWithValue(doc, "NBKe", ""));
			// Đơn vị tiền tệ
			elementSubContent.appendChild(commons.createElementWithValue(doc, "DVTTe", loaiTienTt));
			// Tỷ giá
			elementSubContent.appendChild(commons.createElementWithValue(doc, "TGia", tyGia));
			// Hình thức thanh toán
			elementSubContent.appendChild(commons.createElementWithValue(doc, "HTTToan", hinhThucThanhToanText));
			// MST tổ chức cung cấp giải pháp HĐĐT
			elementSubContent.appendChild(commons.createElementWithValue(doc, "MSTTCGP", SystemParams.MSTTCGP));
			// MST đơn vị nhận ủy nhiệm lập hóa đơn
			elementSubContent.appendChild(commons.createElementWithValue(doc, "MSTDVNUNLHDon", ""));
			// Tên đơn vị nhận ủy nhiệm lập hóa đơn
			elementSubContent.appendChild(commons.createElementWithValue(doc, "TDVNUNLHDon", ""));
			// Địa chỉ đơn vị nhận ủy nhiệm lập hóa đơn
			elementSubContent.appendChild(commons.createElementWithValue(doc, "DCDVNUNLHDon", ""));

			elementTmp = doc.createElement("TTKhac"); // THONG TIN KHAC
			elementTmp.appendChild(commons.createElementTTKhac(doc, "PortalLink", "string", link));
			elementTmp.appendChild(commons.createElementTTKhac(doc, "SecureKey", "string", secureKey));
			elementTmp
					.appendChild(commons.createElementTTKhac(doc, "SystemKey", "string", objectIdEInvoice.toString()));
			elementSubContent.appendChild(elementTmp);

			if (docEInvoiceTTDC != null) {
				elementTmp = doc.createElement("TTHDLQuan");
				elementTmp.appendChild(commons.createElementWithValue(doc, "TCHDon", _tchdon));
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
//						elementTmp.appendChild(commons.createElementWithValue(doc, "TTKhac", ""));

			elementSubTmp = doc.createElement("TTKhac");
			elementSubTmp.appendChild(commons.createElementTTKhac(doc, "TenEN", "string", docTmp.get("NameEN", "")));
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
			elementTmp.appendChild(commons.createElementWithValue(doc, "MDVQHNSach", madvqhns));
			elementTmp.appendChild(commons.createElementWithValue(doc, "MKHang", khMKHang));
			elementTmp.appendChild(commons.createElementWithValue(doc, "SDThoai", khSoDT));
			elementTmp.appendChild(commons.createElementWithValue(doc, "CCCDan", khSoCCCD));
			elementTmp.appendChild(commons.createElementWithValue(doc, "SHChieu", khSoHochieu));
			elementTmp.appendChild(commons.createElementWithValue(doc, "DCTDTu", khEmail));
			elementTmp.appendChild(commons.createElementWithValue(doc, "HVTNMHang", khHoTenNguoiMua));
			elementTmp.appendChild(commons.createElementWithValue(doc, "STKNHang", khSoTk));
			elementTmp.appendChild(commons.createElementWithValue(doc, "TNHang", khTkTaiNganHang));
			elementTmp.appendChild(commons.createElementWithValue(doc, "TTKhac", ""));
			elementSubContent.appendChild(elementTmp);

			mapVATAmount = new LinkedHashMap<String, Double>();
			mapAmount = new LinkedHashMap<String, Double>();
			elementTmp = doc.createElement("DSHHDVu"); // HH-DV

			boolean check_vat = false;
			String vat_ = "";
			String VAT_ = "";
			String check_feature = "";
			double chietKhauTMTotal	= 0.0;
			if (!jsonData.at("/DSSanPham").isMissingNode()) {

				for (JsonNode o : jsonData.at("/DSSanPham")) {
					String feature = commons.getTextJsonNode(o.at("/Feature"));
					if (!feature.equals("4")) {
						vat_ = commons.getTextJsonNode(o.at("/VATRate")).replaceAll(",", "");
						if (VAT_.equals("")) {
							VAT_ = vat_;
						} else {
							if (!VAT_.equals(vat_)) {
								check_vat = true;
								break;
							}
						}
						check_feature = feature;
					}

				}

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
						Boolean slo1 = false;
						Boolean hsd1 = false;
						String slo = commons.getTextJsonNode(o.at("/SLo"));
						String hsd = commons.getTextJsonNode(o.at("/HanSD"));
						if ("".equals(slo)) {
							slo1 = true;
						}
						if ("".equals(hsd)) {
							hsd1 = true;
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
						if (slo1 == false) {
							elementSubTmp.appendChild(
									commons.createElementWithValue(doc, "SLo", commons.getTextJsonNode(o.at("/SLo"))));
						}
						if (hsd1 == false) {
							elementSubTmp.appendChild(commons.createElementWithValue(doc, "HanSD",
									commons.getTextJsonNode(o.at("/HanSD"))));
						}
						elementSubTmp.appendChild(
								commons.createElementWithValue(doc, "DVTinh", commons.getTextJsonNode(o.at("/Unit"))));
//						if (!("2".equals(commons.getTextJsonNode(o.at("/Feature"))))) { // ||
						// "4".equals(commons.getTextJsonNode(o.at("/Feature")))
						elementSubTmp.appendChild(commons.createElementWithValue(doc, "SLuong",
								commons.getTextJsonNode(o.at("/Quantity")).replaceAll(",", "")));
						elementSubTmp.appendChild(commons.createElementWithValue(doc, "DGia",
								commons.getTextJsonNode(o.at("/Price")).replaceAll(",", "")));
						elementSubTmp.appendChild(commons.createElementWithValue(doc, "TLCKhau", ""));
						elementSubTmp.appendChild(commons.createElementWithValue(doc, "STCKhau", ""));
						elementSubTmp.appendChild(commons.createElementWithValue(doc, "ThTien",
								commons.getTextJsonNode(o.at("/Total")).replaceAll(",", "")));

						if (!tmp.equals("-1")) {
							elementSubTmp.appendChild(commons.createElementWithValue(doc, "TSuat", tmp));
						}

//						}

						elementSubTmp01 = doc.createElement("TTKhac");
						elementSubTmp01.appendChild(commons.createElementTTKhac(doc, "VATAmount", "decimal",
								commons.getTextJsonNode(o.at("/VATAmount")).replaceAll(",", "")));
						elementSubTmp01.appendChild(commons.createElementTTKhac(doc, "Amount", "decimal",
								commons.getTextJsonNode(o.at("/Amount")).replaceAll(",", "")));
						elementSubTmp.appendChild(elementSubTmp01);

						elementTmp.appendChild(elementSubTmp);

						double a = commons.ToNumber(commons.getTextJsonNode(o.at("/VATRate")));

						hItem = new LinkedHashMap<String, Object>();
						hItem.put("STT", commons.getTextJsonNode(o.at("/STT")));
						hItem.put("ProductName", commons.getTextJsonNode(o.at("/ProductName")));
						hItem.put("ProductCode", commons.getTextJsonNode(o.at("/ProductCode")));
						hItem.put("SLo", commons.getTextJsonNode(o.at("/SLo")));
						hItem.put("HanSD", commons.getTextJsonNode(o.at("/HanSD")));
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

					}
					if ("3".equals(commons.getTextJsonNode(o.at("/Feature")))) {
						chietKhauTMTotal += commons.ToNumber(commons.getTextJsonNode(o.at("/Total")));
					}
				}
			}
			elementSubContent.appendChild(elementTmp);

			elementTmp = doc.createElement("TToan"); // Thong tin thanh toan
			elementSubTmp = doc.createElement("THTTLTSuat");
			/* DANH SACH CAC LOAI THUE SUAT */

//						https://stackoverflow.com/questions/46898/how-do-i-efficiently-iterate-over-each-entry-in-a-java-map

			if (check_vat == true) {
				for (Map.Entry<String, Double> pair : mapVATAmount.entrySet()) {
					if (null != pair.getKey() && !"".equals(pair.getKey())) {
						hItemVAT = new LinkedHashMap<String, Object>();
						if (!pair.getKey().equals("-1")) {
							elementSubTmp01 = doc.createElement("LTSuat");
							elementSubTmp01.appendChild(commons.createElementWithValue(doc, "TSuat", pair.getKey()));
							elementSubTmp01.appendChild(commons.createElementWithValue(doc, "ThTien",
									commons.formatNumberReal(mapAmount.get(pair.getKey())).replaceAll(",", "")));
							hItemVAT.put("VatName", pair.getKey().replace("%", ""));
							hItemVAT.put("VATAmount",
									commons.formatNumberReal(mapAmount.get(pair.getKey())).replaceAll(",", ""));
							if (loaiTienTt.equals("VND")) {
								elementSubTmp01.appendChild(commons.createElementWithValue(doc, "TThue",
										String.format("%.0f", mapVATAmount.get(pair.getKey()))));
								hItemVAT.put("Amount", String.format("%.0f", mapVATAmount.get(pair.getKey())));
							} else {
								elementSubTmp01.appendChild(commons.createElementWithValue(doc, "TThue",
										String.format("%.2f", mapVATAmount.get(pair.getKey()))));
								hItemVAT.put("Amount", String.format("%.2f", mapVATAmount.get(pair.getKey())));
							}

							elementSubTmp.appendChild(elementSubTmp01);
							listVAT.add(hItemVAT);
						}
					}
				}

			} else {
				if (!VAT_.equals("") && !check_feature.equals("2")) {
					switch (VAT_) {
					case "0":
						VAT_ = "-1";
						break;
					case "1":
						VAT_ = "0%";
						break;

					case "5":
					case "8":
					case "10":
						VAT_ += "%";
						break;
					case "-1":
						VAT_ = "KCT";
						break;
					case "-2":
						VAT_ = "KKKNT";
						break;
					default:
						break;
					}

					elementSubTmp01 = doc.createElement("LTSuat");
					elementSubTmp01.appendChild(commons.createElementWithValue(doc, "TSuat", VAT_));
					elementSubTmp01.appendChild(
							commons.createElementWithValue(doc, "ThTien", tongTienTruocThue.replaceAll(",", "")));
					elementSubTmp01.appendChild(
							commons.createElementWithValue(doc, "TThue", tongTienThueGtgt.replaceAll(",", "")));
					elementSubTmp.appendChild(elementSubTmp01);
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

			elementTmp.appendChild(commons.createElementWithValue(doc, "TTCKTMai", String.valueOf(chietKhauTMTotal)));
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
						.append("TCHDon", _tchdon)
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
					.append("MTDiep", MTDiep).append("DSVAT", listVAT)
//							.append("EInvoiceNumber", null)				//PHAT SINH KHI THUC HIEN KY
					.append("EInvoiceDetail", new Document("TTChung", new Document("THDon", tenLoaiHd)
							.append("MaHD", maHoadon).append("MauSoHD", mauSoHdon)
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
											new Document("Ten", khTenDonVi).append("MST", khMst).append("MDVQHNSach", madvqhns)
													.append("DChi", khDiaChi).append("MKHang", khMKHang)
													.append("SDThoai", khSoDT)
													.append("CCCDan", khSoCCCD)
													.append("SHChieu", khSoHochieu)
													.append("DCTDTu", khEmail)
													.append("DCTDTuCC", khEmailCC).append("HVTNMHang", khHoTenNguoiMua)
													.append("STKNHang", khSoTk).append("TNHang", khTkTaiNganHang)))
							.append("DSHHDVu", listDSHHDVu).append("TToan",
									new Document("TgTCThue", commons.ToNumber(tongTienTruocThue))
											.append("TgTThue", commons.ToNumber(tongTienThueGtgt))
											.append("TgTTTBSo", commons.ToNumber(tongTienDaCoThue))
											.append("TgTQDoi", commons.ToNumber(tongTienQuyDoi))
											.append("TgTTTBChu", tienBangChu)
											.append("ParamUSD", paramUSD)		
									))
					.append("SignStatusCode", Constants.INVOICE_SIGN_STATUS.NOSIGN)
					.append("EInvoiceStatus", Constants.INVOICE_STATUS.CREATED).append("IsDelete", false)
					.append("checkProductExtension", checkProductExtension).append("SecureKey", secureKey)
					.append("Dir", pathDir).append("FileNameXML", fileNameXML).append("InfoCreated",
							new Document("CreateDate", LocalDateTime.now()).append("CreateUserID", header.getUserId())
									.append("CreateUserName", header.getUserName())
									.append("CreateUserFullName", header.getUserFullName()));
			/* END - LUU DU LIEU HD */
			
			 mongoClient = cfg.mongoClient();
				collection = mongoClient.getDatabase(cfg.dbName).getCollection("EInvoice");
				collection.insertOne(docUpsert);			
				mongoClient.close();
				
		

			// START REPLACE, ADJUSTED

			/* KIEM TRA THONG TIN HD CO TON TAI KHONG */
			docFind1 = new Document("IssuerId", header.getIssuerId()).append("IsDelete", false)
					.append("_id", objectIdTT_DC)
				//	.append("SignStatusCode", Constants.INVOICE_SIGN_STATUS.SIGNED)
					.append("EInvoiceStatus", new Document("$in", Arrays.asList(Constants.INVOICE_STATUS.COMPLETE,
							Constants.INVOICE_STATUS.ADJUSTED, Constants.INVOICE_STATUS.REPLACED)));
			options = new FindOneAndUpdateOptions();
			options.upsert(true);
			options.maxTime(5000, TimeUnit.MILLISECONDS);
			options.returnDocument(ReturnDocument.AFTER);

//			if (docEInvoiceTTDC != null) {
//				if ("1".equals(_tchdon)) {
//
//					 mongoClient = cfg.mongoClient();
//					 collection = mongoClient.getDatabase(cfg.dbName).getCollection("EInvoice");
//					collection.findOneAndUpdate(docFind1,
//							new Document("$set", new Document("EInvoiceStatus", "REPLACED")), options);		
//					mongoClient.close();
//					
//				} else if ("2".equals(_tchdon)) {
//					 mongoClient = cfg.mongoClient();
//					 collection = mongoClient.getDatabase(cfg.dbName).getCollection("EInvoice");
//					collection.findOneAndUpdate(docFind1,
//							new Document("$set", new Document("EInvoiceStatus", "ADJUSTED")), options);
//					mongoClient.close();
//					
//				}
//			}

			// END REPLACE, ADJUSTED
			// KIEM TRA XEM KHACH HANG DA CO TRONG DANH SACH QUAN LY
			if (!khMst.equals("")) {
				if (docTmp.get("DSNMua") == null) {
					objectIdQLNMua = new ObjectId();
					docUpsert1 = new Document("_id", objectIdQLNMua)
							.append("NLap",
									commons.convertStringToLocalDate(ngayLap, Constants.FORMAT_DATE.FORMAT_DATE_WEB))
							.append("Ten", khTenDonVi).append("MST", khMst).append("DChi", khDiaChi)
							.append("SDThoai", khSoDT).append("DCTDTu", khEmail).append("IsDelete", false);
					/* END - LUU DU LIEU HD */
					 mongoClient = cfg.mongoClient();
						collection = mongoClient.getDatabase(cfg.dbName).getCollection("QLDSNMua");
						collection.insertOne(docUpsert1);			
						mongoClient.close();
						
	
				} else {

					String phone_nm = "";
					String email_nm = "";
					String phone = docTmp.getEmbedded(Arrays.asList("DSNMua", "SDThoai"), "");
					String email = docTmp.getEmbedded(Arrays.asList("DSNMua", "DCTDTu"), "");

					boolean isPhone = phone.contains(khSoDT);
					boolean isEmail = email.contains(khEmail);

					if (phone.equals("") || phone.isEmpty()) {
						phone_nm = khSoDT;
					} else {
						if (isPhone == true) {
							phone_nm = phone;
						} else {
							phone_nm = phone + "," + khSoDT;
						}
					}

					if (email.equals("") || email.isEmpty()) {
						email_nm = khEmail;
					} else {
						if (isEmail == true) {
							email_nm = email;
						} else {
							email_nm = email + "," + khEmail;
						}
					}

					String id_nm = docTmp.getEmbedded(Arrays.asList("DSNMua", "_id"), ObjectId.class).toString();
					objectNMua = new ObjectId(id_nm);
					docFindNmua = new Document("_id", objectNMua);

					options = new FindOneAndUpdateOptions();
					options.upsert(false);
					options.maxTime(5000, TimeUnit.MILLISECONDS);
					options.returnDocument(ReturnDocument.AFTER);

		
					
					 mongoClient = cfg.mongoClient();
					 collection = mongoClient.getDatabase(cfg.dbName).getCollection("QLDSNMua");
					collection.findOneAndUpdate(docFindNmua,
							new Document("$set", new Document("Ten", khTenDonVi)
									.append("NLap",
											commons.convertStringToLocalDate(ngayLap,
													Constants.FORMAT_DATE.FORMAT_DATE_WEB))
									.append("MST", khMst).append("DChi", khDiaChi).append("SDThoai", phone_nm)
									.append("DCTDTu", email_nm)

									.append("IsDelete", false)),
							options);
					mongoClient.close();
					
					
				}
				// END KIEM TRA XEM KHACH HANG DA CO TRONG DANH SACH QUAN LY
			}

			/* KIEM TRA THONG TIN KHACH HANG - USERS */
			if (!khMst.equals("")) {
				docFind = null;
		
				docTmp = null;
				docFind = new Document("IssuerId", header.getIssuerId()).append("TaxCode", khMst).append("IsDelete",
						false);

				 mongoClient = cfg.mongoClient();
				 collection = mongoClient.getDatabase(cfg.dbName).getCollection("DMHistoryCustomer");
				try {
					docTmp =   collection.find(docFind).iterator().next();	
				} catch (Exception e) {
					// TODO: handle exception
				}
						
				mongoClient.close();
				
				
				

				if (null == docTmp) {
					objectId = null;
					objectId = new ObjectId();
					docUpsert = new Document("_id", objectId).append("IssuerId", header.getIssuerId())
							.append("TaxCode", khMst).append("CustomerCode", khMKHang).append("CompanyName", khTenDonVi)
							.append("CustomerName", khHoTenNguoiMua).append("Address", khDiaChi)
							.append("Email", khEmail).append("EmailCC", khEmailCC).append("Phone", khSoDT)
							.append("InfoCreated",
									new Document("CreateDate", LocalDateTime.now())
											.append("CreateUserID", header.getUserId())
											.append("CreateUserName", header.getUserName())
											.append("CreateUserFullName", header.getUserFullName()));

				
					 mongoClient = cfg.mongoClient();
						collection = mongoClient.getDatabase(cfg.dbName).getCollection("DMHistoryCustomer");
						collection.insertOne(docUpsert);			
						mongoClient.close();
						
						
					responseStatus = new MspResponseStatus(0, "SUCCESS");
					rsp.setResponseStatus(responseStatus);
					return rsp;
				} else {
					options = new FindOneAndUpdateOptions();
					options.upsert(false);
					options.maxTime(5000, TimeUnit.MILLISECONDS);
					options.returnDocument(ReturnDocument.AFTER);
			
					 mongoClient = cfg.mongoClient();
					 collection = mongoClient.getDatabase(cfg.dbName).getCollection("DMHistoryCustomer");
					collection.findOneAndUpdate(docFind,
							new Document("$set",
									new Document("IssuerId", header.getIssuerId()).append("TaxCode", khMst)
											.append("CustomerCode", khMKHang).append("CompanyName", khTenDonVi)
											.append("CustomerName", khHoTenNguoiMua).append("Address", khDiaChi)
											.append("Email", khEmail).append("EmailCC", khEmailCC)
											.append("Phone", khSoDT).append("InfoUpdated",
													new Document("UpdatedDate", LocalDateTime.now())
															.append("UpdatedUserID", header.getUserId())
															.append("UpdatedUserName", header.getUserName())
															.append("UpdatedUserFullName", header.getUserFullName()))),
							options);	
					mongoClient.close();
				}
			}

			DateTimeFormatter format_time = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
			LocalDateTime time_dem = LocalDateTime.now();
			String time = time_dem.format(format_time);
			String name_company = removeAccent(header.getUserFullName());
			System.out.println(time + " " + name_company + " vua tao hoa don VAT");
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

			findInforIssuer = new Document("_id", 1).append("TaxCode", 1).append("Name", 1).append("Address", 1)
					.append("Phone", 1).append("Fax", 1).append("Email", 1).append("Website", 1)
					.append("TinhThanhInfo", 1).append("ChiCucThueInfo", 1).append("BankAccount", 1).append("NameEN", 1)
					.append("BankAccountExt", 1);

			Document fillter = new Document("_id", 1).append("Dir", 1).append("FileNameXML", 1)
					.append("EInvoiceStatus", 1).append("SignStatusCode", 1).append("MCCQT", 1).append("MTDiep", 1).append("SecureKey", 1)
					.append("EInvoiceDetail", 1);

			pipeline = new ArrayList<Document>();
			pipeline.add(new Document("$match",
					new Document("_id", objectId).append("IsActive", true).append("IsDelete", false)));
			pipeline.add(new Document("$project", findInforIssuer));

			pipeline.add(new Document("$lookup", new Document("from", "Users").append("pipeline", Arrays.asList(
					new Document("$match",
							new Document("IssuerId", header.getIssuerId()).append("_id", objectIdUser)
									.append("IsActive", true).append("IsDelete", false)),
					new Document("$project", new Document("_id", 1).append("UserName", 1).append("FullName", 1)),
					new Document("$limit", 1))).append("as", "UserInfo")));
			pipeline.add(new Document("$unwind",
					new Document("path", "$UserInfo").append("preserveNullAndEmptyArrays", true)));

			docFind = new Document("IssuerId", header.getIssuerId()).append("IsDelete", false)
					.append("SignStatusCode", Constants.INVOICE_SIGN_STATUS.NOSIGN).append("_id", objectIdEInvoice);
			pipeline.add(new Document("$lookup",
					new Document("from", "EInvoice")
							.append("pipeline",
									Arrays.asList(new Document("$match", docFind), new Document("$project", fillter)))
							.append("as", "EInvoice")));
			pipeline.add(new Document("$unwind",
					new Document("path", "$EInvoice").append("preserveNullAndEmptyArrays", true)));

			pipeline.add(new Document("$lookup",
					new Document("from", "QLDSNMua")
							.append("pipeline",
									Arrays.asList(new Document("$match",
											new Document("MST", khMst).append("IsDelete", false))))
							.append("as", "DSNMua")));
			pipeline.add(new Document("$unwind",
					new Document("path", "$DSNMua").append("preserveNullAndEmptyArrays", true)));

			// CHECK NGAY LAP SO HOA DON LON NHAT
			pipeline.add(new Document("$lookup",
					new Document("from", "EInvoice")
							.append("pipeline", Arrays.asList(
									new Document("$match",
											new Document("IsDelete", false).append("EInvoiceDetail.TTChung.MauSoHD",
													mauSoHdon)),
									new Document("$group",
											new Document("_id", "$EInvoiceDetail.TTChung.MauSoHD").append("SHDon",
													new Document("$max", "$EInvoiceDetail.TTChung.SHDon")))))
							.append("as", "NLap_MAX")));
			pipeline.add(new Document("$unwind",
					new Document("path", "$NLap_MAX").append("preserveNullAndEmptyArrays", true)));
			// END CHECK NGAY LAP
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
			} catch (Exception e) {

			}

			mongoClient.close();
			if (null == docTmp) {
				responseStatus = new MspResponseStatus(9999, "Không tìm thấy thông tin khách hàng.");
				rsp.setResponseStatus(responseStatus);
				return rsp;
			}
			link = docTmp.getEmbedded(Arrays.asList("PramLink", "LinkPortal"), "");
			if (docTmp.get("UserInfo") == null) {
				responseStatus = new MspResponseStatus(9999, "Không tìm thấy thông tin người dùng.");
				rsp.setResponseStatus(responseStatus);
				return rsp;
			}
			if (docTmp.get("EInvoice") == null) {
				responseStatus = new MspResponseStatus(9999, "Không tìm thấy thông tin hóa đơn cần cập nhật.");
				rsp.setResponseStatus(responseStatus);
				return rsp;
			}

			// LAY NGAY LAP SO HOA DON
			nl_mshd = docTmp.getEmbedded(Arrays.asList("NLap_MAX", "_id"), "");
			nl_shdon = docTmp.getEmbedded(Arrays.asList("NLap_MAX", "SHDon"), 0);

			docFindNLap = new Document("IssuerId", header.getIssuerId())
					.append("EInvoiceDetail.TTChung.SHDon", nl_shdon)
					.append("SignStatusCode", Constants.INVOICE_SIGN_STATUS.SIGNED)
					.append("EInvoiceStatus",
							new Document("$in",
									Arrays.asList(Constants.INVOICE_STATUS.COMPLETE, Constants.INVOICE_STATUS.PENDING,
											Constants.INVOICE_STATUS.ERROR_CQT, Constants.INVOICE_STATUS.PROCESSING,
											Constants.INVOICE_STATUS.ADJUSTED, Constants.INVOICE_STATUS.REPLACED)))
					.append("EInvoiceDetail.TTChung.MauSoHD", nl_mshd).append("IsDelete", false);

			docTmp2 = null;
			mongoClient = cfg.mongoClient();
			collection = mongoClient.getDatabase(cfg.dbName).getCollection("EInvoice");
			try {
				docTmp2 = collection.find(docFindNLap).iterator().next();
			} catch (Exception e) {

			}

			mongoClient.close();

			if (docTmp2 != null) {
				nlap_shd_max = commons.convertLocalDateTimeToString(
						commons.convertDateToLocalDateTime(
								docTmp2.getEmbedded(Arrays.asList("EInvoiceDetail", "TTChung", "NLap"), Date.class)),
						"dd/MM/yyyy");

				formatter = DateTimeFormatter.ofPattern("d/MM/yyyy");
				localDate1 = LocalDate.parse(ngayLap, formatter);
				localDate2 = LocalDate.parse(nlap_shd_max, formatter);

				if (localDate1.compareTo(localDate2) < 0) {
					responseStatus = new MspResponseStatus(9999, "Ngày lập hóa đơn không được nhỏ hơn ngày "
							+ nlap_shd_max + " của hóa đơn trước đó. Vui lòng chọn lại ngày lập.");
					rsp.setResponseStatus(responseStatus);
					return rsp;
				}
			}

			// END LAY NGAY LAP

			docTTHDLQuan = docTmp.getEmbedded(Arrays.asList("EInvoice", "EInvoiceDetail", "TTChung", "TTHDLQuan"),
					Document.class);

			taxCode = docTmp.getString("TaxCode");
			pathDir = docTmp.getEmbedded(Arrays.asList("EInvoice", "Dir"), "");
			fileNameXML = docTmp.getEmbedded(Arrays.asList("EInvoice", "FileNameXML"), "");

			file = new File(pathDir);
			if (!file.exists())
				file.mkdirs();

			String KHMSHDon = docTmp.getEmbedded(Arrays.asList("EInvoice", "EInvoiceDetail", "TTChung", "KHMSHDon"),
					"");
			String KHHDon = docTmp.getEmbedded(Arrays.asList("EInvoice", "EInvoiceDetail", "TTChung", "KHHDon"), "");

			int shd = docTmp.getEmbedded(Arrays.asList("EInvoice", "EInvoiceDetail", "TTChung", "SHDon"), 0);

			secureKey = docTmp.getEmbedded(Arrays.asList("EInvoice", "SecureKey"), "");

			/* TAO XML HOA DON */
			dbf = DocumentBuilderFactory.newInstance();
			db = dbf.newDocumentBuilder();
			doc = db.newDocument();
			doc.setXmlStandalone(true);

			root = doc.createElement("HDon");
			doc.appendChild(root);

			elementContent = doc.createElement("DLHDon");
			elementContent.setAttribute("Id", "data");
			root.appendChild(elementContent);

			elementSubTmp = null;
			elementSubTmp01 = null;
			elementSubContent = doc.createElement("TTChung");
			elementTmp = null;

			elementSubContent.appendChild(commons.createElementWithValue(doc, "PBan", SystemParams.VERSION_XML));
			elementSubContent.appendChild(commons.createElementWithValue(doc, "THDon", tenLoaiHd));
			elementSubContent.appendChild(commons.createElementWithValue(doc, "KHMSHDon", KHMSHDon));
			elementSubContent.appendChild(commons.createElementWithValue(doc, "KHHDon", KHHDon));
			elementSubContent.appendChild(commons.createElementWithValue(doc, "SHDon", "")); // SE PHAT SINH KHI KY
			// Mã hồ sơ:
			elementSubContent.appendChild(commons.createElementWithValue(doc, "MHSo", ""));
			// Ngày lập
			elementSubContent.appendChild(
					commons.createElementWithValue(doc, "NLap", commons.convertLocalDateTimeStringToString(ngayLap,
							Constants.FORMAT_DATE.FORMAT_DATE_WEB, Constants.FORMAT_DATE.FORMAT_DATE_EINVOICE, false)));
			// Số bảng kê
			elementSubContent.appendChild(commons.createElementWithValue(doc, "SBKe", ""));
			// Ngày bảng kê
			elementSubContent.appendChild(commons.createElementWithValue(doc, "NBKe", ""));
			// Đơn vị tiền tệ
			elementSubContent.appendChild(commons.createElementWithValue(doc, "DVTTe", loaiTienTt));
			// Tỷ giá
			elementSubContent.appendChild(commons.createElementWithValue(doc, "TGia", tyGia));
			// Hình thức thanh toán
			elementSubContent.appendChild(commons.createElementWithValue(doc, "HTTToan", hinhThucThanhToanText));
			// MST tổ chức cung cấp giải pháp HĐĐT
			elementSubContent.appendChild(commons.createElementWithValue(doc, "MSTTCGP", SystemParams.MSTTCGP));
			// MST đơn vị nhận ủy nhiệm lập hóa đơn
			elementSubContent.appendChild(commons.createElementWithValue(doc, "MSTDVNUNLHDon", ""));
			// Tên đơn vị nhận ủy nhiệm lập hóa đơn
			elementSubContent.appendChild(commons.createElementWithValue(doc, "TDVNUNLHDon", ""));
			// Địa chỉ đơn vị nhận ủy nhiệm lập hóa đơn
			elementSubContent.appendChild(commons.createElementWithValue(doc, "DCDVNUNLHDon", ""));

			elementTmp = doc.createElement("TTKhac"); // THONG TIN KHAC
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
			elementSubTmp.appendChild(commons.createElementTTKhac(doc, "TenEN", "string", docTmp.get("NameEN", "")));
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
			elementTmp.appendChild(commons.createElementWithValue(doc, "MDVQHNSach", madvqhns));
			elementTmp.appendChild(commons.createElementWithValue(doc, "DChi", khDiaChi));
			elementTmp.appendChild(commons.createElementWithValue(doc, "MKHang", khMKHang));
			elementTmp.appendChild(commons.createElementWithValue(doc, "SDThoai", khSoDT));
			elementTmp.appendChild(commons.createElementWithValue(doc, "CCCDan", khSoCCCD));
			elementTmp.appendChild(commons.createElementWithValue(doc, "SHChieu", khSoHochieu));
			elementTmp.appendChild(commons.createElementWithValue(doc, "DCTDTu", khEmail));
			elementTmp.appendChild(commons.createElementWithValue(doc, "HVTNMHang", khHoTenNguoiMua));
			elementTmp.appendChild(commons.createElementWithValue(doc, "STKNHang", khSoTk));
			elementTmp.appendChild(commons.createElementWithValue(doc, "TNHang", khTkTaiNganHang));
			elementTmp.appendChild(commons.createElementWithValue(doc, "TTKhac", ""));
			elementSubContent.appendChild(elementTmp);

			mapVATAmount = new LinkedHashMap<String, Double>();
			mapAmount = new LinkedHashMap<String, Double>();
			elementTmp = doc.createElement("DSHHDVu"); // HH-DV
			check_vat = false;
			vat_ = "";
			VAT_ = "";
			check_feature = "";
			chietKhauTMTotal = 0.0;
			if (!jsonData.at("/DSSanPham").isMissingNode()) {

				for (JsonNode o : jsonData.at("/DSSanPham")) {
					String feature = commons.getTextJsonNode(o.at("/Feature"));
					if (!feature.equals("4")) {
						vat_ = commons.getTextJsonNode(o.at("/VATRate")).replaceAll(",", "");
						if (VAT_.equals("")) {
							VAT_ = vat_;
						} else {
							if (!VAT_.equals(vat_)) {
								check_vat = true;
								break;
							}
						}
						check_feature = feature;
					}
				}

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
						Boolean slo1 = false;
						Boolean hsd1 = false;
						String slo = commons.getTextJsonNode(o.at("/SLo"));
						String hsd = commons.getTextJsonNode(o.at("/HanSD"));
						if ("".equals(slo)) {
							slo1 = true;
						}
						if ("".equals(hsd)) {
							hsd1 = true;
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
						if (slo1 == false) {
							elementSubTmp.appendChild(
									commons.createElementWithValue(doc, "SLo", commons.getTextJsonNode(o.at("/SLo"))));
						}
						if (hsd1 == false) {
							elementSubTmp.appendChild(commons.createElementWithValue(doc, "HanSD",
									commons.getTextJsonNode(o.at("/HanSD"))));
						}
						elementSubTmp.appendChild(
								commons.createElementWithValue(doc, "DVTinh", commons.getTextJsonNode(o.at("/Unit"))));

//						if (!("2".equals(commons.getTextJsonNode(o.at("/Feature"))))) { // ||
						// "4".equals(commons.getTextJsonNode(o.at("/Feature")))
						elementSubTmp.appendChild(commons.createElementWithValue(doc, "SLuong",
								commons.getTextJsonNode(o.at("/Quantity")).replaceAll(",", "")));
						elementSubTmp.appendChild(commons.createElementWithValue(doc, "DGia",
								commons.getTextJsonNode(o.at("/Price")).replaceAll(",", "")));
						elementSubTmp.appendChild(commons.createElementWithValue(doc, "TLCKhau", ""));
						elementSubTmp.appendChild(commons.createElementWithValue(doc, "STCKhau", ""));
						elementSubTmp.appendChild(commons.createElementWithValue(doc, "ThTien",
								commons.getTextJsonNode(o.at("/Total")).replaceAll(",", "")));
						if (!tmp.equals("-1")) {
							elementSubTmp.appendChild(commons.createElementWithValue(doc, "TSuat", tmp));
						}
//						}
//									else if("4".equals(commons.getTextJsonNode(o.at("/Feature")))) {
//										elementSubTmp.appendChild(commons.createElementWithValue(doc, "TSuat", tmp));
//									}

						elementSubTmp01 = doc.createElement("TTKhac");
						elementSubTmp01.appendChild(commons.createElementTTKhac(doc, "VATAmount", "decimal",
								commons.getTextJsonNode(o.at("/VATAmount")).replaceAll(",", "")));
						elementSubTmp01.appendChild(commons.createElementTTKhac(doc, "Amount", "decimal",
								commons.getTextJsonNode(o.at("/Amount")).replaceAll(",", "")));
						elementSubTmp.appendChild(elementSubTmp01);

						elementTmp.appendChild(elementSubTmp);

						double a = commons.ToNumber(commons.getTextJsonNode(o.at("/VATRate")));

						hItem = new LinkedHashMap<String, Object>();
						hItem.put("STT", commons.getTextJsonNode(o.at("/STT")));
						hItem.put("ProductName", commons.getTextJsonNode(o.at("/ProductName")));
						hItem.put("ProductCode", commons.getTextJsonNode(o.at("/ProductCode")));
						hItem.put("SLo", commons.getTextJsonNode(o.at("/SLo")));
						hItem.put("HanSD", commons.getTextJsonNode(o.at("/HanSD")));
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

					}
					if ("3".equals(commons.getTextJsonNode(o.at("/Feature")))) {
						chietKhauTMTotal += commons.ToNumber(commons.getTextJsonNode(o.at("/Total")));
					}
				}
			}
			elementSubContent.appendChild(elementTmp);

			elementTmp = doc.createElement("TToan"); // Thong tin thanh toan
			elementSubTmp = doc.createElement("THTTLTSuat");
			/* DANH SACH CAC LOAI THUE SUAT */
//						https://stackoverflow.com/questions/46898/how-do-i-efficiently-iterate-over-each-entry-in-a-java-map

			if (check_vat == true) {

				for (Map.Entry<String, Double> pair : mapVATAmount.entrySet()) {
					if (null != pair.getKey() && !"".equals(pair.getKey())) {
						hItemVAT = new LinkedHashMap<String, Object>();
						if (!pair.getKey().equals("-1")) {
							elementSubTmp01 = doc.createElement("LTSuat");
							elementSubTmp01.appendChild(commons.createElementWithValue(doc, "TSuat", pair.getKey()));
							elementSubTmp01.appendChild(commons.createElementWithValue(doc, "ThTien",
									commons.formatNumberReal(mapAmount.get(pair.getKey())).replaceAll(",", "")));
							hItemVAT.put("VatName", pair.getKey().replace("%", ""));
							hItemVAT.put("VATAmount",
									commons.formatNumberReal(mapAmount.get(pair.getKey())).replaceAll(",", ""));

							if (loaiTienTt.equals("VND")) {
								elementSubTmp01.appendChild(commons.createElementWithValue(doc, "TThue",
										String.format("%.0f", mapVATAmount.get(pair.getKey()))));
								hItemVAT.put("Amount", String.format("%.0f", mapVATAmount.get(pair.getKey())));

							} else {
//						elementSubTmp01.appendChild(commons.createElementWithValue(doc, "TThue",String.valueOf(mapVATAmount.get(pair.getKey()))));	
								elementSubTmp01.appendChild(commons.createElementWithValue(doc, "TThue",
										String.format("%.2f", mapVATAmount.get(pair.getKey()))));
								hItemVAT.put("Amount", String.format("%.2f", mapVATAmount.get(pair.getKey())));
							}
							elementSubTmp.appendChild(elementSubTmp01);
							listVAT.add(hItemVAT);
						}

					}
				}

			} else {
				if (!VAT_.equals("") && !check_feature.equals("2")) {
					switch (VAT_) {
					case "0":
						VAT_ = "-1";
						break;
					case "1":
						VAT_ = "0%";
						break;

					case "5":
					case "8":
					case "10":
						VAT_ += "%";
						break;
					case "-1":
						VAT_ = "KCT";
						break;
					case "-2":
						VAT_ = "KKKNT";
						break;
					default:
						break;
					}

					elementSubTmp01 = doc.createElement("LTSuat");
					elementSubTmp01.appendChild(commons.createElementWithValue(doc, "TSuat", VAT_));
					elementSubTmp01.appendChild(
							commons.createElementWithValue(doc, "ThTien", tongTienTruocThue.replaceAll(",", "")));
					elementSubTmp01.appendChild(
							commons.createElementWithValue(doc, "TThue", tongTienThueGtgt.replaceAll(",", "")));
					elementSubTmp.appendChild(elementSubTmp01);
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

			elementTmp.appendChild(commons.createElementWithValue(doc, "TTCKTMai", String.valueOf(chietKhauTMTotal)));
			elementTmp
					.appendChild(commons.createElementWithValue(doc, "TgTTTBSo", tongTienDaCoThue.replaceAll(",", "")));
			elementTmp.appendChild(commons.createElementWithValue(doc, "TgTTTBChu", tienBangChu));
			elementSubTmp = doc.createElement("TTKhac");
			if (!"VND".equals(loaiTienTt)) {
				elementSubTmp.appendChild(
						commons.createElementTTKhac(doc, "TgTQDoi", "decimal", tongTienQuyDoi.replaceAll(",", "")));
			}
			elementTmp.appendChild(elementSubTmp);

			elementSubContent.appendChild(elementTmp);

			elementContent.appendChild(elementSubContent);
			// END - NDHDon: Nội dung hóa đơn

			isSdaveFile = commons.docW3cToFile(doc, pathDir, fileNameXML);
			if (!isSdaveFile) {
				throw new Exception("Lưu dữ liệu không thành công.");
			}
			/* END - TAO XML HOA DON */
			if (shd == 0) {
				/* LUU DU LIEU HD */
				docUpsert = new Document("TTChung", new Document("THDon", tenLoaiHd).append("MaHD", maHoadon)
						.append("MauSoHD", mauSoHdon).append("KHMSHDon", KHMSHDon).append("KHHDon", KHHDon)
						.append("NLap",
								commons.convertStringToLocalDate(ngayLap, Constants.FORMAT_DATE.FORMAT_DATE_WEB))

						.append("DVTTe", loaiTienTt).append("TGia", tyGia).append("HTTToanCode", hinhThucThanhToan)
						.append("HTTToan", hinhThucThanhToanText).append("TTHDLQuan", docTTHDLQuan))
						.append("NDHDon", new Document("NBan", new Document("Ten", docTmp.get("Name", ""))
								.append("MST", docTmp.get("TaxCode", "")).append("DChi", docTmp.get("Address", ""))
								.append("SDThoai", docTmp.get("Phone", "")).append("DCTDTu", docTmp.get("Email", ""))
								.append("STKNHang",
										docTmp.getEmbedded(Arrays.asList("BankAccount", "AccountNumber"), ""))
								.append("TNHang", docTmp.getEmbedded(Arrays.asList("BankAccount", "BankName"), ""))
								.append("Fax", docTmp.get("Fax", "")).append("Website", docTmp.get("Website", "")))
								.append("NMua",
										new Document("Ten", khTenDonVi).append("MST", khMst).append("MDVQHNSach", madvqhns)
										.append("DChi", khDiaChi)
												.append("MKHang", khMKHang).append("SDThoai", khSoDT)
												.append("CCCDan", khSoCCCD)
												.append("SHChieu", khSoHochieu)
												.append("DCTDTu", khEmail).append("DCTDTuCC", khEmailCC)
												.append("HVTNMHang", khHoTenNguoiMua).append("STKNHang", khSoTk)
												.append("TNHang", khTkTaiNganHang)))
						.append("DSHHDVu", listDSHHDVu)

						.append("TToan", new Document("TgTCThue", commons.ToNumber(tongTienTruocThue))
								.append("TgTThue", commons.ToNumber(tongTienThueGtgt))
								.append("TgTTTBSo", commons.ToNumber(tongTienDaCoThue))
								.append("TgTQDoi", commons.ToNumber(tongTienQuyDoi))
								.append("TgTTTBChu", tienBangChu)
								.append("ParamUSD", paramUSD)		
								);
				/* END - LUU DU LIEU HD */
			} else {
				/* LUU DU LIEU HD */
				docUpsert = new Document("TTChung", new Document("THDon", tenLoaiHd).append("MaHD", maHoadon)
						.append("MauSoHD", mauSoHdon).append("KHMSHDon", KHMSHDon).append("KHHDon", KHHDon)
						.append("NLap",
								commons.convertStringToLocalDate(ngayLap, Constants.FORMAT_DATE.FORMAT_DATE_WEB))
						.append("DVTTe", loaiTienTt).append("TGia", tyGia).append("HTTToanCode", hinhThucThanhToan)
						.append("HTTToan", hinhThucThanhToanText).append("TTHDLQuan", docTTHDLQuan)
						.append("SHDon", shd))
						.append("NDHDon", new Document("NBan", new Document("Ten", docTmp.get("Name", ""))
								.append("MST", docTmp.get("TaxCode", "")).append("DChi", docTmp.get("Address", ""))
								.append("SDThoai", docTmp.get("Phone", "")).append("DCTDTu", docTmp.get("Email", ""))
								.append("STKNHang",
										docTmp.getEmbedded(Arrays.asList("BankAccount", "AccountNumber"), ""))
								.append("TNHang", docTmp.getEmbedded(Arrays.asList("BankAccount", "BankName"), ""))
								.append("Fax", docTmp.get("Fax", "")).append("Website", docTmp.get("Website", "")))
								.append("NMua",
										new Document("Ten", khTenDonVi).append("MST", khMst).append("MDVQHNSach", madvqhns)
												.append("DChi", khDiaChi)
												.append("MKHang", khMKHang).append("SDThoai", khSoDT)
												.append("CCCDan", khSoCCCD)
												.append("SHChieu", khSoHochieu)
												.append("DCTDTu", khEmail).append("DCTDTuCC", khEmailCC)
												.append("HVTNMHang", khHoTenNguoiMua).append("STKNHang", khSoTk)
												.append("TNHang", khTkTaiNganHang)))
						.append("DSHHDVu", listDSHHDVu)
						.append("TToan", new Document("TgTCThue", commons.ToNumber(tongTienTruocThue))
								.append("TgTThue", commons.ToNumber(tongTienThueGtgt))
								.append("TgTTTBSo", commons.ToNumber(tongTienDaCoThue))
								.append("TgTQDoi", commons.ToNumber(tongTienQuyDoi))
								.append("TgTTTBChu", tienBangChu)
								.append("ParamUSD", paramUSD)		
								);
				/* END - LUU DU LIEU HD */
			}
			options = new FindOneAndUpdateOptions();
			options.upsert(false);
			options.maxTime(5000, TimeUnit.MILLISECONDS);
			options.returnDocument(ReturnDocument.AFTER);


			
			 mongoClient = cfg.mongoClient();
			 collection = mongoClient.getDatabase(cfg.dbName).getCollection("EInvoice");
			collection.findOneAndUpdate(docFind,
					new Document("$set",
							new Document("EInvoiceDetail", docUpsert).append("DSVAT", listVAT)
									.append("checkProductExtension", checkProductExtension).append("InfoUpdated",
											new Document("UpdatedDate", LocalDateTime.now())
													.append("UpdatedUserID", header.getUserId())
													.append("UpdatedUserName", header.getUserName())
													.append("UpdatedUserFullName", header.getUserFullName()))),
					options);	
			mongoClient.close();
			

			// KIEM TRA XEM KHACH HANG DA CO TRONG DANH SACH QUAN LY
			if (!khMst.equals("")) {
				if (docTmp.get("DSNMua") == null) {
					objectIdQLNMua = new ObjectId();
					docUpsert1 = new Document("_id", objectIdQLNMua)
							.append("NLap",
									commons.convertStringToLocalDate(ngayLap, Constants.FORMAT_DATE.FORMAT_DATE_WEB))
							.append("Ten", khTenDonVi).append("MST", khMst).append("DChi", khDiaChi)
							.append("SDThoai", khSoDT).append("DCTDTu", khEmail)

							.append("IsDelete", false);
					/* END - LUU DU LIEU HD */
					
					 mongoClient = cfg.mongoClient();
						collection = mongoClient.getDatabase(cfg.dbName).getCollection("QLDSNMua");
						collection.insertOne(docUpsert1);			
						mongoClient.close();
						
						
				} else {

					String phone_nm = "";
					String email_nm = "";
					String phone = docTmp.getEmbedded(Arrays.asList("DSNMua", "SDThoai"), "");
					String email = docTmp.getEmbedded(Arrays.asList("DSNMua", "DCTDTu"), "");

					boolean isPhone = phone.contains(khSoDT);
					boolean isEmail = email.contains(khEmail);

					if (phone.equals("") || phone.isEmpty()) {
						phone_nm = khSoDT;
					} else {
						if (isPhone == true) {
							phone_nm = phone;
						} else {
							phone_nm = phone + "," + khSoDT;
						}
					}

					if (email.equals("") || email.isEmpty()) {
						email_nm = khEmail;
					} else {
						if (isEmail == true) {
							email_nm = email;
						} else {
							email_nm = email + "," + khEmail;
						}
					}

					String id_nm = docTmp.getEmbedded(Arrays.asList("DSNMua", "_id"), ObjectId.class).toString();
					objectNMua = new ObjectId(id_nm);
					docFindNmua = new Document("_id", objectNMua);
					options = new FindOneAndUpdateOptions();
					options.upsert(false);
					options.maxTime(5000, TimeUnit.MILLISECONDS);
					options.returnDocument(ReturnDocument.AFTER);

					
					 mongoClient = cfg.mongoClient();
					 collection = mongoClient.getDatabase(cfg.dbName).getCollection("QLDSNMua");
					collection.findOneAndUpdate(docFindNmua,
							new Document("$set", new Document("Ten", khTenDonVi)
									.append("NLap",
											commons.convertStringToLocalDate(ngayLap,
													Constants.FORMAT_DATE.FORMAT_DATE_WEB))
									.append("MST", khMst).append("DChi", khDiaChi).append("SDThoai", phone_nm)
									.append("DCTDTu", email_nm)

									.append("IsDelete", false)),
							options);	
					mongoClient.close();
				}
				// END KIEM TRA XEM KHACH HANG DA CO TRONG DANH SACH QUAN LY
			}

			/* KIEM TRA THONG TIN KHACH HANG - USERS */
			if (!khMst.equals("")) {
				docFind = null;
			
				docTmp = null;
				docFind = new Document("IssuerId", header.getIssuerId()).append("TaxCode", khMst).append("IsDelete",
						false);


				mongoClient = cfg.mongoClient();
				collection = mongoClient.getDatabase(cfg.dbName).getCollection("DMHistoryCustomer");
				try {
					docTmp2 = collection.find(docFind).iterator().next();
				} catch (Exception e) {

				}

				mongoClient.close();

				if (null == docTmp) {
					objectId = null;
					objectId = new ObjectId();
					docUpsert = new Document("_id", objectId).append("IssuerId", header.getIssuerId())
							.append("TaxCode", khMst).append("CustomerCode", khMKHang).append("CompanyName", khTenDonVi)
							.append("CustomerName", khHoTenNguoiMua).append("Address", khDiaChi)
							.append("Email", khEmail).append("EmailCC", khEmailCC).append("Phone", khSoDT)
							.append("InfoCreated",
									new Document("CreateDate", LocalDateTime.now())
											.append("CreateUserID", header.getUserId())
											.append("CreateUserName", header.getUserName())
											.append("CreateUserFullName", header.getUserFullName()));

					
					 mongoClient = cfg.mongoClient();
						collection = mongoClient.getDatabase(cfg.dbName).getCollection("DMHistoryCustomer");
						collection.insertOne(docUpsert);			
						mongoClient.close();
						
						
					responseStatus = new MspResponseStatus(0, "SUCCESS");
					rsp.setResponseStatus(responseStatus);
					return rsp;
				} else {
					options = new FindOneAndUpdateOptions();
					options.upsert(false);
					options.maxTime(5000, TimeUnit.MILLISECONDS);
					options.returnDocument(ReturnDocument.AFTER);

					
					 mongoClient = cfg.mongoClient();
					 collection = mongoClient.getDatabase(cfg.dbName).getCollection("DMHistoryCustomer");
					collection.findOneAndUpdate(docFind,
							new Document("$set",
									new Document("IssuerId", header.getIssuerId()).append("TaxCode", khMst)
											.append("CustomerCode", khMKHang).append("CompanyName", khTenDonVi)
											.append("CustomerName", khHoTenNguoiMua).append("Address", khDiaChi)
											.append("Email", khEmail).append("EmailCC", khEmailCC)
											.append("Phone", khSoDT).append("InfoUpdated",
													new Document("UpdatedDate", LocalDateTime.now())
															.append("UpdatedUserID", header.getUserId())
															.append("UpdatedUserName", header.getUserName())
															.append("UpdatedUserFullName", header.getUserFullName()))),
							options);
					mongoClient.close();
				}
			}

			format_time = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
			time_dem = LocalDateTime.now();
			time = time_dem.format(format_time);
			name_company = removeAccent(header.getUserFullName());
			System.out.println(time + " " + name_company + " vua thay doi hoa don VAT");
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

			findInforIssuer = new Document("_id", 1).append("TaxCode", 1).append("Name", 1).append("Address", 1)
					.append("Phone", 1).append("Fax", 1).append("Email", 1).append("Website", 1)
					.append("TinhThanhInfo", 1).append("ChiCucThueInfo", 1).append("BankAccount", 1).append("NameEN", 1)
					.append("BankAccountExt", 1);

			fillter = new Document("_id", 1).append("Dir", 1).append("FileNameXML", 1).append("EInvoiceStatus", 1)
					.append("SignStatusCode", 1).append("MCCQT", 1).append("MTDiep", 1).append("EInvoiceDetail", 1);

			pipeline = new ArrayList<Document>();
			pipeline.add(new Document("$match",
					new Document("_id", objectId).append("IsActive", true).append("IsDelete", false)));
			pipeline.add(new Document("$project", findInforIssuer));

			pipeline.add(new Document("$lookup", new Document("from", "Users").append("pipeline", Arrays.asList(
					new Document("$match",
							new Document("IssuerId", header.getIssuerId()).append("_id", objectIdUser)
									.append("IsActive", true).append("IsDelete", false)),
					new Document("$project", new Document("_id", 1).append("UserName", 1).append("FullName", 1)),
					new Document("$limit", 1))).append("as", "UserInfo")));
			pipeline.add(new Document("$unwind",
					new Document("path", "$UserInfo").append("preserveNullAndEmptyArrays", true)));
			pipeline.add(new Document("$lookup", new Document("from", "DMMauSoKyHieu")
					.append("pipeline", Arrays.asList(
							new Document(
									"$match",
									new Document("IssuerId", header.getIssuerId()).append("IsActive", true)
											.append("IsDelete", false).append("ConLai", new Document("$gt", 0))
											.append("_id", objectIdMSKH)),
							new Document("$project", new Document("_id", 1).append("KHMSHDon", 1).append("KHHDon", 1))))
					.append("as", "DMMauSoKyHieu")));
			pipeline.add(new Document("$unwind",
					new Document("path", "$DMMauSoKyHieu").append("preserveNullAndEmptyArrays", true)));

			docFind = new Document("IssuerId", header.getIssuerId()).append("IsDelete", false).append("_id",
					objectIdEInvoice);
			pipeline.add(new Document("$lookup",
					new Document("from", "EInvoice")
							.append("pipeline",
									Arrays.asList(new Document("$match", docFind), new Document("$project", fillter)))
							.append("as", "EInvoice")));
			pipeline.add(new Document("$unwind",
					new Document("path", "$EInvoice").append("preserveNullAndEmptyArrays", true)));
			if (!"".equals(maHoadon)) {
				// Check MHDon
				pipeline.add(
						new Document("$lookup",
								new Document("from", "EInvoice").append("pipeline", Arrays.asList(new Document("$match",
										new Document("EInvoiceDetail.TTChung.MaHD", maHoadon)
												.append("EInvoiceDetail.TTChung.MauSoHD", mauSoHdon)
												.append("IssuerId", header.getIssuerId()).append("IsDelete", false)
//														.append("EInvoiceDetail.TTChung.MauSoHD", mauSoHdon)
								))).append("as", "CheckMHDon")));
				pipeline.add(new Document("$unwind",
						new Document("path", "$CheckMHDon").append("preserveNullAndEmptyArrays", true)));

			}

			pipeline.add(new Document("$lookup",
					new Document("from", "QLDSNMua")
							.append("pipeline",
									Arrays.asList(new Document("$match",
											new Document("MST", khMst).append("IsDelete", false))))
							.append("as", "DSNMua")));
			pipeline.add(new Document("$unwind",
					new Document("path", "$DSNMua").append("preserveNullAndEmptyArrays", true)));

			// CHECK NGAY LAP SO HOA DON LON NHAT
			pipeline.add(new Document("$lookup",
					new Document("from", "EInvoice")
							.append("pipeline", Arrays.asList(
									new Document("$match",
											new Document("IsDelete", false).append("EInvoiceDetail.TTChung.MauSoHD",
													mauSoHdon)),
									new Document("$group",
											new Document("_id", "$EInvoiceDetail.TTChung.MauSoHD").append("SHDon",
													new Document("$max", "$EInvoiceDetail.TTChung.SHDon")))))
							.append("as", "NLap_MAX")));
			pipeline.add(new Document("$unwind",
					new Document("path", "$NLap_MAX").append("preserveNullAndEmptyArrays", true)));
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
			// END CHECK NGAY LAP

			mongoClient = cfg.mongoClient();
			collection = mongoClient.getDatabase(cfg.dbName).getCollection("Issuer");
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
			link = docTmp.getEmbedded(Arrays.asList("PramLink", "LinkPortal"), "");
			if (docTmp.get("UserInfo") == null) {
				responseStatus = new MspResponseStatus(9999, "Không tìm thấy thông tin người dùng.");
				rsp.setResponseStatus(responseStatus);
				return rsp;
			}
			if (docTmp.get("EInvoice") == null) {
				responseStatus = new MspResponseStatus(9999, "Không tìm thấy thông tin hóa đơn cần cập nhật.");
				rsp.setResponseStatus(responseStatus);
				return rsp;
			}
			if (docTmp.get("CheckMHDon") != null) {
				responseStatus = new MspResponseStatus(9999, "Mã hóa đơn đã tồn tại.");
				rsp.setResponseStatus(responseStatus);
				return rsp;
			}
			if (docTmp.get("DMMauSoKyHieu") == null) {
				responseStatus = new MspResponseStatus(9999, "Hết số lượng hóa đơn.");
				rsp.setResponseStatus(responseStatus);
				return rsp;
			}

			// LAY NGAY LAP SO HOA DON
			nl_mshd = docTmp.getEmbedded(Arrays.asList("NLap_MAX", "_id"), "");
			nl_shdon = docTmp.getEmbedded(Arrays.asList("NLap_MAX", "SHDon"), 0);

			docFindNLap = new Document("IssuerId", header.getIssuerId())
					.append("EInvoiceDetail.TTChung.SHDon", nl_shdon)
					.append("SignStatusCode", Constants.INVOICE_SIGN_STATUS.SIGNED)
					.append("EInvoiceStatus",
							new Document("$in",
									Arrays.asList(Constants.INVOICE_STATUS.COMPLETE, Constants.INVOICE_STATUS.PENDING,
											Constants.INVOICE_STATUS.ERROR_CQT, Constants.INVOICE_STATUS.PROCESSING,
											Constants.INVOICE_STATUS.ADJUSTED, Constants.INVOICE_STATUS.REPLACED)))
					.append("EInvoiceDetail.TTChung.MauSoHD", nl_mshd).append("IsDelete", false);

			docTmp2 = null;

			mongoClient = cfg.mongoClient();
			collection = mongoClient.getDatabase(cfg.dbName).getCollection("EInvoice");
			try {
				docTmp2 = collection.find(docFindNLap).iterator().next();
			} catch (Exception e) {

			}

			mongoClient.close();

			if (docTmp2 != null) {
				nlap_shd_max = commons.convertLocalDateTimeToString(
						commons.convertDateToLocalDateTime(
								docTmp2.getEmbedded(Arrays.asList("EInvoiceDetail", "TTChung", "NLap"), Date.class)),
						"dd/MM/yyyy");

				formatter = DateTimeFormatter.ofPattern("d/MM/yyyy");
				localDate1 = LocalDate.parse(ngayLap, formatter);
				localDate2 = LocalDate.parse(nlap_shd_max, formatter);

				if (localDate1.compareTo(localDate2) < 0) {
					responseStatus = new MspResponseStatus(9999, "Ngày lập hóa đơn không được nhỏ hơn ngày "
							+ nlap_shd_max + " của hóa đơn trước đó. Vui lòng chọn lại ngày lập.");
					rsp.setResponseStatus(responseStatus);
					return rsp;
				}
			}
			// END LAY NGAY LAP

			docTTHDLQuan = docTmp.getEmbedded(Arrays.asList("EInvoice", "EInvoiceDetail", "TTChung", "TTHDLQuan"),
					Document.class);

			taxCode = docTmp.getString("TaxCode");

			String KHMSHDon1 = docTmp.getEmbedded(Arrays.asList("EInvoice", "EInvoiceDetail", "TTChung", "KHMSHDon"),
					"");

			String KHHDon1 = docTmp.getEmbedded(Arrays.asList("DMMauSoKyHieu", "KHHDon"), "");
			String MaHD = docTmp.getEmbedded(Arrays.asList("EInvoice", "EInvoiceDetail", "TTChung", "MaHD"), "");
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
			doc.appendChild(root);

			elementContent = doc.createElement("DLHDon");
			elementContent.setAttribute("Id", "data");
			root.appendChild(elementContent);

			elementSubTmp = null;
			elementSubTmp01 = null;
			elementSubContent = doc.createElement("TTChung");
			elementTmp = null;

			elementSubContent.appendChild(commons.createElementWithValue(doc, "PBan", SystemParams.VERSION_XML));
			elementSubContent.appendChild(commons.createElementWithValue(doc, "THDon", tenLoaiHd));
			elementSubContent.appendChild(commons.createElementWithValue(doc, "KHMSHDon", KHMSHDon1));
			elementSubContent.appendChild(commons.createElementWithValue(doc, "KHHDon", KHHDon1));
			elementSubContent.appendChild(commons.createElementWithValue(doc, "SHDon", "")); // SE PHAT SINH KHI KY
			// Mã hồ sơ:
			elementSubContent.appendChild(commons.createElementWithValue(doc, "MHSo", ""));
			// Ngày lập
			elementSubContent.appendChild(
					commons.createElementWithValue(doc, "NLap", commons.convertLocalDateTimeStringToString(ngayLap,
							Constants.FORMAT_DATE.FORMAT_DATE_WEB, Constants.FORMAT_DATE.FORMAT_DATE_EINVOICE, false)));
			// Số bảng kê
			elementSubContent.appendChild(commons.createElementWithValue(doc, "SBKe", ""));
			// Ngày bảng kê
			elementSubContent.appendChild(commons.createElementWithValue(doc, "NBKe", ""));
			// Đơn vị tiền tệ
			elementSubContent.appendChild(commons.createElementWithValue(doc, "DVTTe", loaiTienTt));
			// Tỷ giá
			elementSubContent.appendChild(commons.createElementWithValue(doc, "TGia", tyGia));
			// Hình thức thanh toán
			elementSubContent.appendChild(commons.createElementWithValue(doc, "HTTToan", hinhThucThanhToanText));
			// MST tổ chức cung cấp giải pháp HĐĐT
			elementSubContent.appendChild(commons.createElementWithValue(doc, "MSTTCGP", SystemParams.MSTTCGP));
			// MST đơn vị nhận ủy nhiệm lập hóa đơn
			elementSubContent.appendChild(commons.createElementWithValue(doc, "MSTDVNUNLHDon", ""));
			// Tên đơn vị nhận ủy nhiệm lập hóa đơn
			elementSubContent.appendChild(commons.createElementWithValue(doc, "TDVNUNLHDon", ""));
			// Địa chỉ đơn vị nhận ủy nhiệm lập hóa đơn
			elementSubContent.appendChild(commons.createElementWithValue(doc, "DCDVNUNLHDon", ""));

			elementTmp = doc.createElement("TTKhac"); // THONG TIN KHAC
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
			elementSubTmp.appendChild(commons.createElementTTKhac(doc, "TenEN", "string", docTmp.get("NameEN", "")));
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
			elementTmp.appendChild(commons.createElementWithValue(doc, "MDVQHNSach", madvqhns));
			elementTmp.appendChild(commons.createElementWithValue(doc, "DChi", khDiaChi));
			elementTmp.appendChild(commons.createElementWithValue(doc, "MKHang", khMKHang));
			elementTmp.appendChild(commons.createElementWithValue(doc, "SDThoai", khSoDT));
			elementTmp.appendChild(commons.createElementWithValue(doc, "CCCDan", khSoCCCD));
			elementTmp.appendChild(commons.createElementWithValue(doc, "SHChieu", khSoHochieu));
			elementTmp.appendChild(commons.createElementWithValue(doc, "DCTDTu", khEmail));
			elementTmp.appendChild(commons.createElementWithValue(doc, "HVTNMHang", khHoTenNguoiMua));
			elementTmp.appendChild(commons.createElementWithValue(doc, "STKNHang", khSoTk));
			elementTmp.appendChild(commons.createElementWithValue(doc, "TNHang", khTkTaiNganHang));
			elementTmp.appendChild(commons.createElementWithValue(doc, "TTKhac", ""));
			elementSubContent.appendChild(elementTmp);

			mapVATAmount = new LinkedHashMap<String, Double>();
			mapAmount = new LinkedHashMap<String, Double>();
			elementTmp = doc.createElement("DSHHDVu"); // HH-DV

			check_vat = false;
			vat_ = "";
			VAT_ = "";
			check_feature = "";
			chietKhauTMTotal = 0.0;
			if (!jsonData.at("/DSSanPham").isMissingNode()) {

				for (JsonNode o : jsonData.at("/DSSanPham")) {
					String feature = commons.getTextJsonNode(o.at("/Feature"));
					if (!feature.equals("4")) {
						vat_ = commons.getTextJsonNode(o.at("/VATRate")).replaceAll(",", "");
						if (VAT_.equals("")) {
							VAT_ = vat_;
						} else {
							if (!VAT_.equals(vat_)) {
								check_vat = true;
								break;
							}
						}
						check_feature = feature;
					}
				}

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
						Boolean slo1 = false;
						Boolean hsd1 = false;
						String slo = commons.getTextJsonNode(o.at("/SLo"));
						String hsd = commons.getTextJsonNode(o.at("/HanSD"));
						if ("".equals(slo)) {
							slo1 = true;
						}
						if ("".equals(hsd)) {
							hsd1 = true;
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
						if (slo1 == false) {
							elementSubTmp.appendChild(
									commons.createElementWithValue(doc, "SLo", commons.getTextJsonNode(o.at("/SLo"))));
						}
						if (hsd1 == false) {
							elementSubTmp.appendChild(commons.createElementWithValue(doc, "HanSD",
									commons.getTextJsonNode(o.at("/HanSD"))));
						}
						elementSubTmp.appendChild(
								commons.createElementWithValue(doc, "DVTinh", commons.getTextJsonNode(o.at("/Unit"))));

//						if (!("2".equals(commons.getTextJsonNode(o.at("/Feature"))))) { // ||
						// "4".equals(commons.getTextJsonNode(o.at("/Feature")))
						elementSubTmp.appendChild(commons.createElementWithValue(doc, "SLuong",
								commons.getTextJsonNode(o.at("/Quantity")).replaceAll(",", "")));
						elementSubTmp.appendChild(commons.createElementWithValue(doc, "DGia",
								commons.getTextJsonNode(o.at("/Price")).replaceAll(",", "")));
						elementSubTmp.appendChild(commons.createElementWithValue(doc, "TLCKhau", ""));
						elementSubTmp.appendChild(commons.createElementWithValue(doc, "STCKhau", ""));
						elementSubTmp.appendChild(commons.createElementWithValue(doc, "ThTien",
								commons.getTextJsonNode(o.at("/Total")).replaceAll(",", "")));
						if (!tmp.equals("-1")) {
							elementSubTmp.appendChild(commons.createElementWithValue(doc, "TSuat", tmp));
						}
//						}
//									else if("4".equals(commons.getTextJsonNode(o.at("/Feature")))) {
//										elementSubTmp.appendChild(commons.createElementWithValue(doc, "TSuat", tmp));
//									}

						elementSubTmp01 = doc.createElement("TTKhac");
						elementSubTmp01.appendChild(commons.createElementTTKhac(doc, "VATAmount", "decimal",
								commons.getTextJsonNode(o.at("/VATAmount")).replaceAll(",", "")));
						elementSubTmp01.appendChild(commons.createElementTTKhac(doc, "Amount", "decimal",
								commons.getTextJsonNode(o.at("/Amount")).replaceAll(",", "")));
						elementSubTmp.appendChild(elementSubTmp01);

						elementTmp.appendChild(elementSubTmp);

						double a = commons.ToNumber(commons.getTextJsonNode(o.at("/VATRate")));

						hItem = new LinkedHashMap<String, Object>();
						hItem.put("STT", commons.getTextJsonNode(o.at("/STT")));
						hItem.put("ProductName", commons.getTextJsonNode(o.at("/ProductName")));
						hItem.put("ProductCode", commons.getTextJsonNode(o.at("/ProductCode")));
						hItem.put("SLo", commons.getTextJsonNode(o.at("/SLo")));
						hItem.put("HanSD", commons.getTextJsonNode(o.at("/HanSD")));
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

					}
					if ("3".equals(commons.getTextJsonNode(o.at("/Feature")))) {
						chietKhauTMTotal += commons.ToNumber(commons.getTextJsonNode(o.at("/Total")));
					}
				}
			}
			elementSubContent.appendChild(elementTmp);

			elementTmp = doc.createElement("TToan"); // Thong tin thanh toan
			elementSubTmp = doc.createElement("THTTLTSuat");
			/* DANH SACH CAC LOAI THUE SUAT */

//						https://stackoverflow.com/questions/46898/how-do-i-efficiently-iterate-over-each-entry-in-a-java-map

			if (check_vat == true) {

				for (Map.Entry<String, Double> pair : mapVATAmount.entrySet()) {
					if (null != pair.getKey() && !"".equals(pair.getKey())) {
						hItemVAT = new LinkedHashMap<String, Object>();
						if (!pair.getKey().equals("-1")) {
							elementSubTmp01 = doc.createElement("LTSuat");
							elementSubTmp01.appendChild(commons.createElementWithValue(doc, "TSuat", pair.getKey()));
							elementSubTmp01.appendChild(commons.createElementWithValue(doc, "ThTien",
									commons.formatNumberReal(mapAmount.get(pair.getKey())).replaceAll(",", "")));
							hItemVAT.put("VatName", pair.getKey().replace("%", ""));
							hItemVAT.put("VATAmount",
									commons.formatNumberReal(mapAmount.get(pair.getKey())).replaceAll(",", ""));

							if (loaiTienTt.equals("VND")) {
								elementSubTmp01.appendChild(commons.createElementWithValue(doc, "TThue",
										String.format("%.0f", mapVATAmount.get(pair.getKey()))));
								hItemVAT.put("Amount", String.format("%.0f", mapVATAmount.get(pair.getKey())));
							} else {
//						elementSubTmp01.appendChild(commons.createElementWithValue(doc, "TThue",String.valueOf(mapVATAmount.get(pair.getKey()))));	
								elementSubTmp01.appendChild(commons.createElementWithValue(doc, "TThue",
										String.format("%.2f", mapVATAmount.get(pair.getKey()))));
								hItemVAT.put("Amount", String.format("%.2f", mapVATAmount.get(pair.getKey())));
							}

							elementSubTmp.appendChild(elementSubTmp01);
							listVAT.add(hItemVAT);
						}
					}
				}
			} else {

				if (!VAT_.equals("") && !check_feature.equals("2")) {

					switch (VAT_) {
					case "0":
						VAT_ = "-1";
						break;
					case "1":
						VAT_ = "0%";
						break;

					case "5":
					case "8":
					case "10":
						VAT_ += "%";
						break;
					case "-1":
						VAT_ = "KCT";
						break;
					case "-2":
						VAT_ = "KKKNT";
						break;
					default:
						break;
					}

					elementSubTmp01 = doc.createElement("LTSuat");
					elementSubTmp01.appendChild(commons.createElementWithValue(doc, "TSuat", VAT_));
					elementSubTmp01.appendChild(
							commons.createElementWithValue(doc, "ThTien", tongTienTruocThue.replaceAll(",", "")));
					elementSubTmp01.appendChild(
							commons.createElementWithValue(doc, "TThue", tongTienThueGtgt.replaceAll(",", "")));
					elementSubTmp.appendChild(elementSubTmp01);
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

			elementTmp.appendChild(commons.createElementWithValue(doc, "TTCKTMai", String.valueOf(chietKhauTMTotal)));
			elementTmp
					.appendChild(commons.createElementWithValue(doc, "TgTTTBSo", tongTienDaCoThue.replaceAll(",", "")));
			elementTmp.appendChild(commons.createElementWithValue(doc, "TgTTTBChu", tienBangChu));
			elementSubTmp = doc.createElement("TTKhac");
			if (!"VND".equals(loaiTienTt)) {
				elementSubTmp.appendChild(
						commons.createElementTTKhac(doc, "TgTQDoi", "decimal", tongTienQuyDoi.replaceAll(",", "")));
			}
			elementTmp.appendChild(elementSubTmp);

			elementSubContent.appendChild(elementTmp);

			elementContent.appendChild(elementSubContent);
			// END - NDHDon: Nội dung hóa đơn

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
					.append("MTDiep", MTDiep).append("DSVAT", listVAT)
//							.append("EInvoiceNumber", null)				//PHAT SINH KHI THUC HIEN KY
					.append("EInvoiceDetail", new Document("TTChung", new Document("THDon", tenLoaiHd)
							.append("MaHD", maHoadon).append("MauSoHD", mauSoHdon)
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
											new Document("Ten", khTenDonVi).append("MST", khMst).append("MDVQHNSach", madvqhns)
													.append("DChi", khDiaChi).append("MKHang", khMKHang)
													.append("SDThoai", khSoDT)
													.append("CCCDan", khSoCCCD)
													.append("SHChieu", khSoHochieu)
													.append("DCTDTu", khEmail)
													.append("DCTDTuCC", khEmailCC).append("HVTNMHang", khHoTenNguoiMua)
													.append("STKNHang", khSoTk).append("TNHang", khTkTaiNganHang)))
							.append("DSHHDVu", listDSHHDVu)
//								.append("DSHHDVu", 
//									jsonData.at("/DSSanPham").isMissingNode()?
//									new ArrayList<Object>():
//									Json.serializer().fromNode(jsonData.at("/DSSanPham"), new TypeReference<List<?>>() {
//									})
//								)
							.append("TToan",
									new Document("TgTCThue", commons.ToNumber(tongTienTruocThue))
											.append("TgTThue", commons.ToNumber(tongTienThueGtgt))
											.append("TgTTTBSo", commons.ToNumber(tongTienDaCoThue))
											.append("TgTQDoi", commons.ToNumber(tongTienQuyDoi))
											.append("TgTTTBChu", tienBangChu)
											.append("ParamUSD", paramUSD)
									))
					.append("SignStatusCode", Constants.INVOICE_SIGN_STATUS.NOSIGN)
					.append("EInvoiceStatus", Constants.INVOICE_STATUS.CREATED).append("IsDelete", false)
					.append("SecureKey", secureKey).append("Dir", pathDir).append("FileNameXML", fileNameXML)
					.append("checkProductExtension", checkProductExtension).append("InfoCreated",
							new Document("CreateDate", LocalDateTime.now()).append("CreateUserID", header.getUserId())
									.append("CreateUserName", header.getUserName())
									.append("CreateUserFullName", header.getUserFullName()));
			/* END - LUU DU LIEU HD */

			// KIEM TRA XEM KHACH HANG DA CO TRONG DANH SACH QUAN LY
			if (!khMst.equals("")) {
				if (docTmp.get("DSNMua") == null) {
					objectIdQLNMua = new ObjectId();
					docUpsert1 = new Document("_id", objectIdQLNMua)
							.append("NLap",
									commons.convertStringToLocalDate(ngayLap, Constants.FORMAT_DATE.FORMAT_DATE_WEB))
							.append("Ten", khTenDonVi).append("MST", khMst).append("DChi", khDiaChi)
							.append("SDThoai", khSoDT).append("DCTDTu", khEmail)

							.append("IsDelete", false);
					/* END - LUU DU LIEU HD */
				
					 mongoClient = cfg.mongoClient();
						collection = mongoClient.getDatabase(cfg.dbName).getCollection("QLDSNMua");
						collection.insertOne(docUpsert1);			
						mongoClient.close();
						
						
				} else {

					String phone_nm = "";
					String email_nm = "";
					String phone = docTmp.getEmbedded(Arrays.asList("DSNMua", "SDThoai"), "");
					String email = docTmp.getEmbedded(Arrays.asList("DSNMua", "DCTDTu"), "");

					boolean isPhone = phone.contains(khSoDT);
					boolean isEmail = email.contains(khEmail);

					if (phone.equals("") || phone.isEmpty()) {
						phone_nm = khSoDT;
					} else {
						if (isPhone == true) {
							phone_nm = phone;
						} else {
							phone_nm = phone + "," + khSoDT;
						}
					}

					if (email.equals("") || email.isEmpty()) {
						email_nm = khEmail;
					} else {
						if (isEmail == true) {
							email_nm = email;
						} else {
							email_nm = email + "," + khEmail;
						}
					}

					options = new FindOneAndUpdateOptions();
					options.upsert(false);
					options.maxTime(5000, TimeUnit.MILLISECONDS);
					options.returnDocument(ReturnDocument.AFTER);

					String id_nm = docTmp.getEmbedded(Arrays.asList("DSNMua", "_id"), ObjectId.class).toString();
					objectNMua = new ObjectId(id_nm);
					docFindNmua = new Document("_id", objectNMua);
					
					 mongoClient = cfg.mongoClient();
					 collection = mongoClient.getDatabase(cfg.dbName).getCollection("QLDSNMua");
					collection.findOneAndUpdate(docFindNmua,
							new Document("$set", new Document("Ten", khTenDonVi)
									.append("NLap",
											commons.convertStringToLocalDate(ngayLap,
													Constants.FORMAT_DATE.FORMAT_DATE_WEB))
									.append("MST", khMst).append("DChi", khDiaChi).append("SDThoai", phone_nm)
									.append("DCTDTu", email_nm)

									.append("IsDelete", false)),
							options);	
					mongoClient.close();
				}
				// END KIEM TRA XEM KHACH HANG DA CO TRONG DANH SACH QUAN LY
			}

	
			
			 mongoClient = cfg.mongoClient();
				collection = mongoClient.getDatabase(cfg.dbName).getCollection("EInvoice");
				collection.insertOne(docUpsert);			
				mongoClient.close();
				
				

			format_time = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
			time_dem = LocalDateTime.now();
			time = time_dem.format(format_time);
			name_company = removeAccent(header.getUserFullName());
			System.out.println(time + " " + name_company + " vua copy hoa don VAT");
			responseStatus = new MspResponseStatus(0, "SUCCESS");
			rsp.setResponseStatus(responseStatus);
			return rsp;

		//////////////////////////////////////////////////////////////////////////////////////////////////////////////

		case Constants.MSG_ACTION_CODE.DELETEALL:
			List<ObjectId> objectIds = new ArrayList<ObjectId>();
			try {
				if (!jsonData.at("/ids").isMissingNode()) {
					for (JsonNode o : jsonData.at("/ids")) {
						try {
							objectIds.add(new ObjectId(o.asText("")));
						} catch (Exception e) {
						}
					}
				}
			} catch (Exception e) {
			}

			UpdateOptions updateOptions = new UpdateOptions();
			updateOptions.upsert(false);

			docFind = new Document("IsDelete", false).append("_id", new Document("$in", objectIds))
					.append("SignStatusCode", "NOSIGN").append("EInvoiceStatus", "CREATED")
					.append("IssuerId", header.getIssuerId());


			 mongoClient = cfg.mongoClient();
			 collection = mongoClient.getDatabase(cfg.dbName).getCollection("EInvoice");
			collection.updateMany(docFind,
					new Document("$set", new Document("IsDelete", true).append("InfoDeleted",
							new Document("DeletedDate", LocalDateTime.now()).append("DeletedUserID", header.getUserId())
									.append("DeletedUserName", header.getUserName())
									.append("DeletedUserFullName", header.getUserFullName()))),
					updateOptions);	
			mongoClient.close();
			responseStatus = new MspResponseStatus(0, "SUCCESS");
			rsp.setResponseStatus(responseStatus);
			return rsp;

		case Constants.MSG_ACTION_CODE.DELETE:
			objectId = null;
			objectIdMSKH = null;
			try {
				objectId = new ObjectId(_id);
			} catch (Exception e) {
			}

			/* KIEM TRA THONG TIN HD CO TON TAI KHONG */
			docFind = new Document("IssuerId", header.getIssuerId()).append("IsDelete", false).append("_id", objectId)
					.append("EInvoiceStatus", new Document("$in", Arrays.asList("CREATED", "PENDING")));
			docTmp = null;

			fillter = new Document("_id", 1).append("EInvoiceDetail", 1);

			pipeline = new ArrayList<Document>();
			pipeline.add(new Document("$match", docFind));
			pipeline.add(new Document("$project", fillter));

			mongoClient = cfg.mongoClient();
			collection = mongoClient.getDatabase(cfg.dbName).getCollection("EInvoice");
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

//			docTTHDLQuan = docTmp.getEmbedded(Arrays.asList("EInvoiceDetail", "TTChung", "TTHDLQuan"),
//					Document.class);
//			if (docTTHDLQuan != null && docTTHDLQuan.get("_id") != null) {
//				options = new FindOneAndUpdateOptions();
//				options.upsert(false);
//				options.maxTime(5000, TimeUnit.MILLISECONDS);
//				options.returnDocument(ReturnDocument.AFTER);
//				ObjectId objectIdTTHDLQuan = new ObjectId(docTTHDLQuan.getString("_id"));
//				Document find = new Document("IssuerId", header.getIssuerId()).append("IsDelete", false)
//						.append("_id", objectIdTTHDLQuan).append("EInvoiceStatus", new Document("$in",
//								Arrays.asList(Constants.INVOICE_STATUS.ADJUSTED, Constants.INVOICE_STATUS.REPLACED)));
//
//				mongoClient = cfg.mongoClient();
//				collection = mongoClient.getDatabase(cfg.dbName).getCollection("EInvoice");
//
//				Document dooo = collection.find(find).allowDiskUse(true).iterator().next();
//				collection.findOneAndUpdate(find,
//						new Document("$set", new Document("EInvoiceStatus", Constants.INVOICE_STATUS.COMPLETE)),
//						options);
//
//				mongoClient.close();
//			}


			String MSKH = docTmp.getEmbedded(Arrays.asList("EInvoiceDetail", "TTChung", "MauSoHD"), "");

			int SHD = docTmp.getEmbedded(Arrays.asList("EInvoiceDetail", "TTChung", "SHDon"), 0);
			if (SHD == 0) {
				// CAP NHAT HOA DON DA XOA TRONG EINVOICE
				options = new FindOneAndUpdateOptions();
				options.upsert(false);
				options.maxTime(5000, TimeUnit.MILLISECONDS);
				options.returnDocument(ReturnDocument.AFTER);

				 mongoClient = cfg.mongoClient();
				 collection = mongoClient.getDatabase(cfg.dbName).getCollection("EInvoice");
				collection.findOneAndUpdate(docFind,
						new Document("$set",
								new Document("IsDelete", true).append("InfoDeleted",
										new Document("DeletedDate", LocalDateTime.now())
												.append("DeletedUserID", header.getUserId())
												.append("DeletedUserName", header.getUserName())
												.append("DeletedUserFullName", header.getUserFullName()))),
						options);	
				mongoClient.close();
				
				

				responseStatus = new MspResponseStatus(0, "SUCCESS");
				rsp.setResponseStatus(responseStatus);
				return rsp;
			} else {
				objectIdMSKH = new ObjectId(MSKH);
				docFind1 = new Document("IssuerId", header.getIssuerId()).append("IsDelete", false).append("_id",
						objectIdMSKH);

				Document findMSKH = new Document("_id", 1).append("SHDHT", 1).append("ConLai", 1);

				pipeline = new ArrayList<Document>();
				pipeline.add(new Document("$match", docFind1));
				pipeline.add(new Document("$project", findMSKH));

				mongoClient = cfg.mongoClient();
				collection = mongoClient.getDatabase(cfg.dbName).getCollection("DMMauSoKyHieu");
				try {
					docTmp1 = collection.aggregate(pipeline).allowDiskUse(true).iterator().next();
				} catch (Exception e) {

				}

				mongoClient.close();

				if (null == docTmp) {
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
					 collection = mongoClient.getDatabase(cfg.dbName).getCollection("EInvoice");
					collection.findOneAndUpdate(docFind,
							new Document("$set",
									new Document("IsDelete", true).append("InfoDeleted",
											new Document("DeletedDate", LocalDateTime.now())
													.append("DeletedUserID", header.getUserId())
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
					format_time = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
					time_dem = LocalDateTime.now();
					time = time_dem.format(format_time);
					name_company = removeAccent(header.getUserFullName());
					System.out.println(time + " " + name_company + " vua xoa hoa don VAT");
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


		case Constants.MSG_ACTION_CODE.SEND_CQT:
			objectId = null;
			try {
				objectId = new ObjectId(_id);
			} catch (Exception e) {
			}
			/* KIEM TRA THONG TIN HD CO TON TAI KHONG */
			docFind = new Document("IssuerId", header.getIssuerId()).append("IsDelete", false).append("_id", objectId)
					.append("EInvoiceStatus", Constants.INVOICE_STATUS.PENDING)
					.append("SignStatusCode", Constants.INVOICE_SIGN_STATUS.SIGNED);

			fillter = new Document("_id", 1).append("Dir", 1).append("MTDiep", 1).append("EInvoiceDetail", 1);

			pipeline = new ArrayList<Document>();
			pipeline.add(new Document("$match", docFind));
			pipeline.add(new Document("$project", fillter));
			pipeline.add(new Document("$lookup", new Document("from", "EInvoice")
					.append("let",
							new Document("vIssuerId", "$IssuerId").append("vMauSo", "$EInvoiceDetail.TTChung.MauSoHD"))
					.append("pipeline", Arrays.asList(
							new Document("$match", new Document("$expr", new Document("$and", Arrays.asList(
									new Document("$eq", Arrays.asList("$IssuerId", "$$vIssuerId")),
									new Document("$eq", Arrays.asList("$EInvoiceDetail.TTChung.MauSoHD", "$$vMauSo")),
									new Document("$ne", Arrays.asList("$IsDelete", true)),
									new Document("$eq",
											Arrays.asList("$SignStatusCode", Constants.INVOICE_SIGN_STATUS.SIGNED)),
									new Document("$in", Arrays.asList("$EInvoiceStatus",
											Arrays.asList(Constants.INVOICE_STATUS.COMPLETE,
													Constants.INVOICE_STATUS.ERROR_CQT,
													Constants.INVOICE_STATUS.DELETED, Constants.INVOICE_STATUS.REPLACED,
													Constants.INVOICE_STATUS.ADJUSTED))))))),
							new Document("$group",
									new Document("_id", "$EInvoiceDetail.TTChung.MauSoHD").append("SHDon",
											new Document("$max", "$EInvoiceDetail.TTChung.SHDon")))))
					.append("as", "EInvoiceMAXCQT")));
			pipeline.add(new Document("$unwind",
					new Document("path", "$EInvoiceMAXCQT").append("preserveNullAndEmptyArrays", true)));

			docTmp = null;

			mongoClient = cfg.mongoClient();
			collection = mongoClient.getDatabase(cfg.dbName).getCollection("EInvoice");
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

			int invoiceNumberCurrent = docTmp.getEmbedded(Arrays.asList("EInvoiceDetail", "TTChung", "SHDon"), 0);
			int maxInvoiceSendedCQT = 0;
			if (docTmp.get("EInvoiceMAXCQT") != null)
				maxInvoiceSendedCQT = docTmp.getEmbedded(Arrays.asList("EInvoiceMAXCQT", "SHDon"), 0);
//			if(invoiceNumberCurrent == 0 || invoiceNumberCurrent != maxInvoiceSendedCQT + 1) {
//				responseStatus = new MspResponseStatus(9999, "Có 1 hoặc 1 vài số hóa đơn trước đó chưa được xử lý xong.<br>Vui lòng kiểm tra lại danh sách hóa đơn.");
//				rsp.setResponseStatus(responseStatus);
//				return rsp;
//			}
			org.w3c.dom.Document rTCTN = null;
			org.w3c.dom.Document rTCTN1 = null;
			String codeMTD = "0315382923";
			String MTDiep1 = "";
			String codeTTTNhan = "";
			String descTTTNhan = "";
			String dir = docTmp.get("Dir", "");
			String fileName = _id + "_signed.xml";
			file = new File(dir, fileName);
			if (!file.exists() || !file.isFile()) {
				responseStatus = new MspResponseStatus(9999, "Không tìm thấy thông tin hóa đơn đã ký.");
				rsp.setResponseStatus(responseStatus);
				return rsp;
			}

			doc = commons.fileToDocument(file, true);
			if (null == doc) {
				responseStatus = new MspResponseStatus(9999, "Dữ liệu hóa đơn đã ký không tồn tại.");
				rsp.setResponseStatus(responseStatus);
				return rsp;
			}

			String MaKetQua = "";
			String CQT_MLTDiep = "";
			String MLoi = "";
			Node nodeKetQuaTraCuu = null;
			MTDiep = docTmp.get("MTDiep", "");
			MST = docTmp.getEmbedded(Arrays.asList("EInvoiceDetail", "NDHDon", "NBan", "MST"), "");

			// TRA CUU HOA DƠN TRC KHI CALL TIEP NHAN THONG DIEP

			format_time = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
			time_dem = LocalDateTime.now();
			time = time_dem.format(format_time);
			name_company = removeAccent(header.getUserFullName());
			System.out.println(time + " " + name_company + " vua TRA CUU HOA DƠN " + invoiceNumberCurrent
					+ " TRUOC KHI CALL TIEP NHAN THONG DIEP");
			try {
				rTCTN1 = tctnService.callTraCuuThongDiep(MTDiep);
			} catch (Exception e) {
				System.out.println(time + " " + "Loi callTraCuuThongDiep" + "  " + MTDiep);
			}

			if (rTCTN1 == null) {

				try {
					MTDiep1 = docTmp.get("MTDiep", "");
					rTCTN1 = tctnService.callTraCuuThongDiep(MTDiep);
				} catch (Exception e) {
					System.out.println(time + " " + "Loi callTraCuuThongDiep" + "  " + MTDiep);
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
					rTCTN = tctnService.callTiepNhanThongDiep("200", MTDiep, MST, "1", doc);
				} catch (Exception e) {
					System.out.println(time + " " + "Loi call Tiep Nhan Thong Diep  " + MTDiep);
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
//							if(xPath.evaluate("DLieu/*/DSCKS", nodeTDiep, XPathConstants.NODE) != null)
					if (xPath1.evaluate("DLieu/*/MCCQT", nodeTDiep, XPathConstants.NODE) != null)
						break;
				}
				CQT_MLTDiep = commons.getTextFromNodeXML(
						(Element) xPath1.evaluate("TTChung/MLTDiep", nodeTDiep, XPathConstants.NODE));

				if ("202".equals(CQT_MLTDiep)) {
					codeTTTNhan = MaKetQua;
				}
				if ("204".equals(CQT_MLTDiep)) {
					MLoi = commons.getTextFromNodeXML((Element) xPath1.evaluate("DLieu/TBao/DLTBao/LCMa/DSLDo/LDo/MLoi",
							nodeTDiep, XPathConstants.NODE));
					responseStatus = new MspResponseStatus(9999, MLoi);
					rsp.setResponseStatus(responseStatus);
					return rsp;
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
				 collection = mongoClient.getDatabase(cfg.dbName).getCollection("EInvoice");
				collection.findOneAndUpdate(docFind,
						new Document("$set", new Document("EInvoiceStatus", Constants.INVOICE_STATUS.PROCESSING)
								.append("SendCQT_Date", LocalDateTime.now()).append("InfoSendCQT",
										new Document("Date", LocalDateTime.now()).append("UserID", header.getUserId())
												.append("UserName", header.getUserName())
												.append("UserFullName", header.getUserFullName()))),
						options);	
				mongoClient.close();
				
				System.out.println(time + " " + "Luu hoa don THANH CONG  " + "  " + MTDiep);
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
			List<String> ids = null;
			List<String> listIDSendMail = new ArrayList<>();
			MailConfig mailConfig = null;
			try {
				ids = Json.serializer().fromJson(commons.decodeBase64ToString(_token),
						new TypeReference<List<String>>() {
						});
			} catch (Exception e) {
			}
			int count = 0;
			String hdError = "";

			for (int h = 0; h < ids.size(); h++) {
				_id = ids.get(h);
				try {
					objectId = new ObjectId(_id);
				} catch (Exception e) {
				}
				/* KIEM TRA THONG TIN HD CO TON TAI KHONG */
				docFind = new Document("IssuerId", header.getIssuerId()).append("IsDelete", false)
						.append("_id", objectId).append("EInvoiceStatus", Constants.INVOICE_STATUS.PENDING)
						.append("SignStatusCode", Constants.INVOICE_SIGN_STATUS.SIGNED);
				pipeline = new ArrayList<Document>();
				pipeline.add(new Document("$match", docFind));
				pipeline.add(new Document("$project",
						new Document("_id", 1).append("Dir", 1).append("MTDiep", 1).append("EInvoiceDetail", 1)));
				pipeline.add(new Document("$lookup", new Document("from", "ConfigEmail")
						.append("let", new Document("vIssuerId", "$IssuerId"))
						.append("pipeline", Arrays.asList(new Document("$match",
								new Document("$expr", new Document("$eq", Arrays.asList("$IssuerId", "$$vIssuerId"))))
//							new Document("$project", new Document("InfoCreated", -1).append("InfoUpdated", -1))
						)).append("as", "ConfigEmail")));
				pipeline.add(new Document("$unwind",
						new Document("path", "$ConfigEmail").append("preserveNullAndEmptyArrays", true)));


				mongoClient = cfg.mongoClient();
				collection = mongoClient.getDatabase(cfg.dbName).getCollection("EInvoice");
				try {
					docTmp = collection.aggregate(pipeline).allowDiskUse(true).iterator().next();
				} catch (Exception e) {

				}

				mongoClient.close();

				if (null == docTmp) {
					count += 1;
					break;
				}
				if (docTmp.get("ConfigEmail") != null && mailConfig == null) {
					mailConfig = new MailConfig(docTmp.get("ConfigEmail", Document.class));
					mailConfig.setNameSend(
							docTmp.getEmbedded(Arrays.asList("EInvoiceDetail", "NDHDon", "NBan", "Ten"), ""));
				}
				invoiceNumberCurrent = docTmp.getEmbedded(Arrays.asList("EInvoiceDetail", "TTChung", "SHDon"), 0);
				maxInvoiceSendedCQT = 0;
				if (docTmp.get("EInvoiceMAXCQT") != null)
					maxInvoiceSendedCQT = docTmp.getEmbedded(Arrays.asList("EInvoiceMAXCQT", "SHDon"), 0);
				rTCTN = null;
				rTCTN1 = null;
				codeMTD = "0315382923";
				MTDiep1 = "";
				codeTTTNhan = "";
				descTTTNhan = "";
				dir = docTmp.get("Dir", "");
				fileName = _id + "_signed.xml";
				file = new File(dir, fileName);
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

				MaKetQua = "";
				CQT_MLTDiep = "";
				xPath1 = null;
				MLoi = "";
				nodeKetQuaTraCuu = null;
				MTDiep = docTmp.get("MTDiep", "");
				MST = docTmp.getEmbedded(Arrays.asList("EInvoiceDetail", "NDHDon", "NBan", "MST"), "");

				// TRA CUU HOA DƠN TRC KHI CALL TIEP NHAN THONG DIEP
				rTCTN1 = tctnService.callTraCuuThongDiep(MTDiep);
				if (rTCTN1 == null) {
					MTDiep1 = docTmp.get("MTDiep", "");
					rTCTN1 = tctnService.callTraCuuThongDiep(MTDiep);
				}
				xPath1 = XPathFactory.newInstance().newXPath();
				nodeKetQuaTraCuu = (Node) xPath1.evaluate("/KetQuaTraCuu", rTCTN1, XPathConstants.NODE);
				MaKetQua = commons.getTextFromNodeXML(
						(Element) xPath1.evaluate("MaKetQua", nodeKetQuaTraCuu, XPathConstants.NODE));
				if ("2".equals(MaKetQua)) {
					rTCTN = tctnService.callTiepNhanThongDiep("200", MTDiep, MST, "1", doc);
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
//							if(xPath.evaluate("DLieu/*/DSCKS", nodeTDiep, XPathConstants.NODE) != null)
						if (xPath1.evaluate("DLieu/*/MCCQT", nodeTDiep, XPathConstants.NODE) != null)
							break;
					}
					CQT_MLTDiep = commons.getTextFromNodeXML(
							(Element) xPath1.evaluate("TTChung/MLTDiep", nodeTDiep, XPathConstants.NODE));

					if ("202".equals(CQT_MLTDiep)) {
						codeTTTNhan = MaKetQua;
					}
					if ("204".equals(CQT_MLTDiep)) {
						count += 1;
						hdError += invoiceNumberCurrent + ",";
						break;
					}
				}
				switch (codeTTTNhan) {
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
				// listIDSendMail.add(_id);
				/* CAP NHAT LAI TRANG THAI DANG CHO XU LY */
				options = new FindOneAndUpdateOptions();
				options.upsert(false);
				options.maxTime(5000, TimeUnit.MILLISECONDS);
				options.returnDocument(ReturnDocument.AFTER);

		
				
				 mongoClient = cfg.mongoClient();
				 collection = mongoClient.getDatabase(cfg.dbName).getCollection("EInvoice");
				collection.findOneAndUpdate(docFind, new Document("$set",
						new Document("EInvoiceStatus", Constants.INVOICE_STATUS.PROCESSING).append("InfoSendCQT",
								new Document("Date", LocalDateTime.now()).append("UserID", header.getUserId())
										.append("UserName", header.getUserName())
										.append("UserFullName", header.getUserFullName()))),
						options);	
				mongoClient.close();
				
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
		String nbanMst = "";
		String nbanTen = "";
		String nameProd = "";
		String nmuaTen = "";

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
			nameProd = commons.getTextJsonNode(jsonData.at("/NameProd")).trim().replaceAll("\\s+", " ");
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
		String mstban = "";
		String mstmua = "";
		try {
			String[] words = header.getUserName().split("_");
			mstban = words[0];
			mstmua = words[1];
		} catch (Exception e) {

		}

		Document fillter = new Document("_id", 1).append("EInvoiceStatus", 1).append("SignStatusCode", 1)
				.append("MCCQT", 1).append("MTDiep", 1).append("MTDTChieu", 1).append("LDo", 1).append("HDSS", 1)
				.append("SendCQT_Date", 1).append("InfoCreated", 1).append("CQT_Date", 1).append("EInvoiceDetail", 1)
				.append("ReplacedBy", 1).append("AdjustedBy", 1);

		if (!mstban.equals("") && !mstmua.equals("")) {
			pipeline = null;
			pipeline = new ArrayList<Document>();
			docMatch = new Document("EInvoiceDetail.NDHDon.NMua.MST", mstmua)
					.append("EInvoiceDetail.NDHDon.NBan.MST", mstban).append("IsDelete", false);
			pipeline.add(new Document("$match", docMatch));
			pipeline.add(new Document("$sort", new Document("EInvoiceDetail.TTChung.NLap", -1).append("_id", -1)));
			pipeline.add(new Document("$project", fillter));

			pipeline.addAll(createFacetForSearchNotSort(page));


			MongoClient mongoClient = cfg.mongoClient();
			MongoCollection<Document> collection = mongoClient.getDatabase(cfg.dbName).getCollection("EInvoice");
			try {
				docTmp = collection.aggregate(pipeline).allowDiskUse(true).iterator().next();
			} catch (Exception e) {

			}

			mongoClient.close();
		} else {
			docMatch = new Document("IssuerId", header.getIssuerId()).append("IsDelete", false);
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
			if (!"".equals(nbanTen))
				docMatch.append("EInvoiceDetail.NDHDon.NMua.Ten",
						new Document("$regex", commons.regexEscapeForMongoQuery(nbanTen)).append("$options", "i"));
			if (!"".equals(nmuaTen))
				docMatch.append("EInvoiceDetail.NDHDon.NMua.HVTNMHang",
						new Document("$regex", commons.regexEscapeForMongoQuery(nmuaTen)).append("$options", "i"));
			
			
			if(!"".equals(nameProd))			
				docMatch.append("EInvoiceDetail.DSHHDVu.ProductName",
						new Document("$regex", commons.regexEscapeForMongoQuery(nameProd)).append("$options", "i"));
								
			pipeline = new ArrayList<Document>();
			pipeline.add(new Document("$match", docMatch));
			pipeline.add(new Document("$addFields", new Document("SHDon",
					new Document("$ifNull", Arrays.asList("$EInvoiceDetail.TTChung.SHDon", Integer.MAX_VALUE)))));
			pipeline.add(new Document("$sort", new Document("SignStatusCode", 1)
					.append("EInvoiceDetail.TTChung.MauSoHD", -1).append("SHDon", -1).append("_id", -1)));

			pipeline.add(new Document("$project", fillter));

			pipeline.addAll(createFacetForSearchNotSort(page));


			MongoClient mongoClient = cfg.mongoClient();
			MongoCollection<Document> collection = mongoClient.getDatabase(cfg.dbName).getCollection("EInvoice");
			try {
				docTmp = collection.aggregate(pipeline).allowDiskUse(true).iterator().next();
			} catch (Exception e) {

			}

			mongoClient.close();

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

		rowsReturn = new ArrayList<HashMap<String, Object>>();
		hItem = null;
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
				hItem.put("ReplacedBy", doc.get("ReplacedBy"));
				hItem.put("AdjustedBy", doc.get("AdjustedBy"));
				rowsReturn.add(hItem);
			}
		}

		DateTimeFormatter format_time = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
		LocalDateTime time_dem = LocalDateTime.now();
		String time = time_dem.format(format_time);
		String name_company = removeAccent(header.getUserFullName());
		System.out.println(time + " " + name_company + " vua search hoa don VAT");
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

		Document docFind = new Document("IssuerId", header.getIssuerId()).append("IsDelete", false).append("_id",
				objectId);

		Document fillter = new Document("_id", 1).append("EInvoiceStatus", 1).append("SignStatusCode", 1)
				.append("MCCQT", 1).append("MTDiep", 1).append("EInvoiceDetail", 1).append("HDSS", 1)
				.append("SecureKey", 1)
		        .append("InfoCreated", 1)
		        .append("CQT_Date", 1)
		        .append("LDo", 1);

		pipeline = new ArrayList<Document>();
		pipeline.add(new Document("$match", docFind));
		pipeline.add(new Document("$project", fillter));
		pipeline.add(new Document("$lookup", new Document("from", "PramLink")
				.append("pipeline",
						Arrays.asList(new Document("$match", new Document("$expr", new Document("IsDelete", false))),
								new Document("$project", new Document("_id", 1).append("LinkPortal", 1))))
				.append("as", "PramLink")));
		pipeline.add(
				new Document("$unwind", new Document("path", "$PramLink").append("preserveNullAndEmptyArrays", true)));

		Document docTmp = null;

		MongoClient mongoClient = cfg.mongoClient();
		MongoCollection<Document> collection = mongoClient.getDatabase(cfg.dbName).getCollection("EInvoice");
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

		DateTimeFormatter format_time = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
		LocalDateTime time_dem = LocalDateTime.now();
		String time = time_dem.format(format_time);
		String name_company = removeAccent(header.getUserFullName());
		System.out.println(time + " " + name_company + " vua xem chi tiet hoa don VAT");
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

		String issuerId = header.getIssuerId();
		issuerLocks_sign.putIfAbsent(issuerId, new ReentrantLock());
		ReentrantLock lock = issuerLocks_sign.get(issuerId);

		if (lock.tryLock()) {
			try {
				/* NHO KIEM TRA XEM CO HD NAO DANG KY KHONG */
				FindOneAndUpdateOptions options = null;

				Document docFind = new Document("IssuerId", header.getIssuerId()).append("IsDelete", false)
						.append("_id", objectId)
						.append("EInvoiceStatus", new Document("$in", Arrays.asList("CREATED", "PENDING")))
						.append("SignStatusCode", new Document("$in", Arrays.asList("NOSIGN", "PROCESSING")));

				Document fillter = new Document("_id", 1).append("IssuerId", 1).append("Dir", 1)
						.append("FileNameXML", 1).append("MCCQT", 1).append("MTDiep", 1).append("EInvoiceDetail", 1);

				List<Document> pipeline = new ArrayList<Document>();
				pipeline.add(new Document("$match", docFind));
				pipeline.add(new Document("$project", fillter));
				/* KIEM TRA THONG TIN MAU HD */
				pipeline.add(
						new Document("$lookup",
								new Document("from", "DMMauSoKyHieu")
										.append("let",
												new Document("vMauSoHD", "$EInvoiceDetail.TTChung.MauSoHD")
														.append("vIssuerId", "$IssuerId"))
										.append("pipeline",
												Arrays.asList(
														new Document("$match", new Document("$expr",
																new Document("$and", Arrays.asList(
																		new Document("$gt",
																				Arrays.asList("$ConLai", 0)),
																		new Document(
																				"$eq",
																				Arrays.asList("$IsActive", true)),
																		new Document(
																				"$ne",
																				Arrays.asList("$IsDelete", true)),
																		new Document("$eq",
																				Arrays.asList(new Document("$toString",
																						"$_id"), "$$vMauSoHD")),
																		new Document(
																				"$eq",
																				Arrays.asList("$IssuerId",
																						"$$vIssuerId")),
																		new Document("$eq",
																				Arrays.asList("$NamPhatHanh",
																						currentYear)))))),
														new Document("$project",
																new Document("_id", 1).append("Status", 1)
																		.append("SHDHT", 1).append("SoLuong", 1)
																		.append("ConLai", 1))

												)).append("as", "DMMauSoKyHieu")));
				pipeline.add(new Document("$unwind",
						new Document("path", "$DMMauSoKyHieu").append("preserveNullAndEmptyArrays", true)));
				Document docTmp = null;

				pipeline.add(new Document("$lookup",
						new Document("from", "EInvoice")
								.append("pipeline", Arrays.asList(
										new Document("$match",
												new Document("IsDelete", false).append("EInvoiceDetail.TTChung.MauSoHD",
														"$$vMauSoHD")),
										new Document("$group",
												new Document("_id", "$EInvoiceDetail.TTChung.MauSoHD").append("SHDon",
														new Document("$max", "$EInvoiceDetail.TTChung.SHDon")))))
								.append("as", "NLap_MAX")));
				pipeline.add(new Document("$unwind",
						new Document("path", "$NLap_MAX").append("preserveNullAndEmptyArrays", true)));

				MongoClient mongoClient = cfg.mongoClient();
				MongoCollection<Document> collection = mongoClient.getDatabase(cfg.dbName).getCollection("EInvoice");
				try {
					docTmp = collection.aggregate(pipeline).allowDiskUse(true).iterator().next();
				} catch (Exception e) {

				}

				mongoClient.close();

				if (null == docTmp) {
					return fileInfo;
				}
				if (null == docTmp.get("DMMauSoKyHieu")) {
					fileInfo.setCheck("Hết số hóa đơn. Vui lòng kiểm tra lại!!!");
					return fileInfo;
				}

				// CHECK STATUS CO DC ACTIVE CHUA
				boolean check_active = docTmp.getEmbedded(Arrays.asList("DMMauSoKyHieu", "Status"), false);
				if (check_active == true) {
					fileInfo.setCheck("Mẫu hóa đơn đang được admin xử lý. Vui lòng chờ trong giây lát!!!");
					return fileInfo;
				}
				// END CHECK STATUS

				String mauSoHdon = docTmp.getEmbedded(Arrays.asList("DMMauSoKyHieu", "_id"), ObjectId.class).toString();

				String ngayLap = commons.convertLocalDateTimeToString(
						commons.convertDateToLocalDateTime(
								docTmp.getEmbedded(Arrays.asList("EInvoiceDetail", "TTChung", "NLap"), Date.class)),
						"dd/MM/yyyy");

				// CHECK NGAY LAP SO HOA DON LON NHAT
				List<Document> pipeline1 = null;
				pipeline1 = new ArrayList<Document>();
				pipeline1.add(new Document("$match", docFind));
				pipeline1.add(new Document("$lookup",
						new Document("from", "EInvoice")
								.append("pipeline", Arrays.asList(
										new Document("$match",
												new Document("IsDelete", false).append("SignStatusCode", "SIGNED")
														.append("EInvoiceDetail.TTChung.MauSoHD", mauSoHdon)),
										new Document("$group",
												new Document("_id", "$EInvoiceDetail.TTChung.MauSoHD").append("SHDon",
														new Document("$max", "$EInvoiceDetail.TTChung.SHDon")))))
								.append("as", "NLap_MAX")));
				pipeline1.add(new Document("$unwind",
						new Document("path", "$NLap_MAX").append("preserveNullAndEmptyArrays", true)));
				Document docTmp7 = null;

				mongoClient = cfg.mongoClient();
				collection = mongoClient.getDatabase(cfg.dbName).getCollection("EInvoice");
				try {
					docTmp7 = collection.aggregate(pipeline1).allowDiskUse(true).iterator().next();
				} catch (Exception e) {

				}

				mongoClient.close();
				// END CHECK NGAY LAP

				if (docTmp7 != null) {
					// LAY NGAY LAP SO HOA DON
					String nl_mshd = docTmp7.getEmbedded(Arrays.asList("NLap_MAX", "_id"), "");
					int nl_shdon = docTmp7.getEmbedded(Arrays.asList("NLap_MAX", "SHDon"), 0);

					Document docFindNLap = new Document("IssuerId", header.getIssuerId())
							.append("EInvoiceDetail.TTChung.SHDon", nl_shdon)
							.append("SignStatusCode", Constants.INVOICE_SIGN_STATUS.SIGNED)
							.append("EInvoiceStatus", new Document("$in",
									Arrays.asList(Constants.INVOICE_STATUS.COMPLETE, Constants.INVOICE_STATUS.PENDING,
											Constants.INVOICE_STATUS.ERROR_CQT, Constants.INVOICE_STATUS.PROCESSING,
											Constants.INVOICE_STATUS.ADJUSTED, Constants.INVOICE_STATUS.REPLACED)))
							.append("EInvoiceDetail.TTChung.MauSoHD", nl_mshd).append("IsDelete", false);

					Document docTmp2 = null;

					mongoClient = cfg.mongoClient();
					collection = mongoClient.getDatabase(cfg.dbName).getCollection("EInvoice");
					try {
						docTmp2 = collection.find(docFindNLap).allowDiskUse(true).iterator().next();
					} catch (Exception e) {

					}

					mongoClient.close();

					if (docTmp2 != null) {
						String nlap_shd_max = commons.convertLocalDateTimeToString(
								commons.convertDateToLocalDateTime(docTmp2
										.getEmbedded(Arrays.asList("EInvoiceDetail", "TTChung", "NLap"), Date.class)),
								"dd/MM/yyyy");

						DateTimeFormatter formatter = DateTimeFormatter.ofPattern("d/MM/yyyy");
						LocalDate localDate1 = LocalDate.parse(ngayLap, formatter);
						LocalDate localDate2 = LocalDate.parse(nlap_shd_max, formatter);
						LocalDate NHT = LocalDate.now();
						if (localDate1.compareTo(localDate2) < 0) {
							fileInfo.setCheck("Ngày lập hóa đơn không được nhỏ hơn ngày " + nlap_shd_max
									+ " của hóa đơn trước đó. Vui lòng chọn lại ngày lập.");
							return fileInfo;
						}
						if (localDate1.compareTo(NHT) > 0) {
							fileInfo.setCheck(
									"Ngày lập của hóa đơn đang ký không được lớn hơn ngày hiện tại. vui lòng chọn lại ngày lập.");
							return fileInfo;
						}

					}
				}
				// CHECK TRUONG HOP CHUA CO SO HOA DON
				DateTimeFormatter formatter = DateTimeFormatter.ofPattern("d/MM/yyyy");
				LocalDate localDate1 = LocalDate.parse(ngayLap, formatter);
				LocalDate NHT = LocalDate.now();
				if (localDate1.compareTo(NHT) > 0) {
					fileInfo.setCheck(
							"Ngày lập của hóa đơn đang ký không được lớn hơn ngày hiện tại. vui lòng chọn lại ngày lập.");
					return fileInfo;
				}
				// END LAY NGAY LAP

				/* AP DUNG 1 FILE TRUOC */
				String dir = docTmp.get("Dir", "");
				String fileName = docTmp.get("FileNameXML", "");
				File file = new File(dir, fileName);

				if (!file.exists())
					return fileInfo;
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
							.append("EInvoiceDetail.TTChung.MauSoHD", idDMMSKH).append("IsDelete", false)
							.append("EInvoiceStatus", new Document("$in", Arrays.asList("DELETED", "DELETE",
									"ERROR_CQT", "XOABO", "COMPLETE", "PROCESSING")));

					pipeline = new ArrayList<Document>();
					pipeline.add(new Document("$match", docMatch));
					pipeline.add(new Document("$addFields", new Document("SHDon", new Document("$ifNull",
							Arrays.asList("$EInvoiceDetail.TTChung.SHDon", Integer.MAX_VALUE)))));
					pipeline.add(new Document("$sort",
							new Document("EInvoiceDetail.TTChung.MauSoHD", -1).append("SHDon", -1).append("_id", -1)));
					Document docTmp1 = null;

					mongoClient = cfg.mongoClient();
					collection = mongoClient.getDatabase(cfg.dbName).getCollection("EInvoice");
					try {
						docTmp1 = collection.aggregate(pipeline).allowDiskUse(true).iterator().next();
					} catch (Exception e) {

					}

					mongoClient.close();

					ktshdht = docTmp1.getEmbedded(Arrays.asList("EInvoiceDetail", "TTChung", "SHDon"), 0);
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
				collection = mongoClient.getDatabase(cfg.dbName).getCollection("EInvoice");
				collection.findOneAndUpdate(docFind,
						new Document("$set", new Document("EInvoiceDetail.TTChung.SHDon", eInvoiceNumber)
								.append("EInvoiceStatus", "PENDING")),
						options);
				mongoClient.close();
				if (checkshd == 0) {
					Document docFindMS = null;
					docFindMS = new Document("IssuerId", header.getIssuerId()).append("IsDelete", false)
							.append("IsActive", true).append("_id", objectIdMS);
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
			fileInfo.setCheck("Tồn tại user đang thực hiện Ký, vui lòng thử lại.");
			return fileInfo;
		}
		return fileInfo;
	}

	// CHECK SHD
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
		pipeline.add(new Document("$lookup", new Document("from", "EInvoice")
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

		pipeline.add(new Document("$lookup", new Document("from", "EInvoice")
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
		MongoCollection<Document> collection = mongoClient.getDatabase(cfg.dbName).getCollection("EInvoice");

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
	// END CHECK SHD

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

		/*
		 * KIEM TRA THONG TIN FILE DA KY DOC DU LIEU VA LUU VO THU MUC TUONG UNG
		 */

		// ==> PROCESSING

		int eInvoiceNumber = 0;

		XPath xPath = XPathFactory.newInstance().newXPath();
		Node nodeDLHDon = (Node) xPath.evaluate("/HDon/DLHDon[@Id='data']", xmlDoc, XPathConstants.NODE);
		Node nodeTmp = null;
		nodeTmp = (Node) xPath.evaluate("TTChung", nodeDLHDon, XPathConstants.NODE);

		eInvoiceNumber = commons.stringToInteger(
				commons.getTextFromNodeXML((Element) xPath.evaluate("SHDon", nodeTmp, XPathConstants.NODE)));
		String keySystem = "";

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
//			String SigningTime = commons.getTextFromNodeXML((Element) xPath.evaluate(
//					"/HDon/DSCKS/NBan/Signature/Object[@Id='SigningTime']/SignatureProperties/SignatureProperty/SigningTime",
//					xmlDoc, XPathConstants.NODE));

		LocalDate ldNLap = null;
//			LocalDate ldSigningTime = null;
		try {
			ldNLap = commons.convertStringToLocalDate(NLap, "yyyy-MM-dd");
		} catch (Exception e) {
		}
//			if (SigningTime.length() > 10) {
//				ldSigningTime = commons.convertStringToLocalDate(SigningTime.substring(0, 10), "yyyy-MM-dd");
//			}
//

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
		pipeline.add(new Document("$project", new Document("IssuerId", 1).append("EInvoiceDetail", 1).append("_id", 1)
				.append("SignStatusCode", 1).append("Dir", 1)));
//		pipeline.add(new Document("$lookup", new Document("from", "EInvoice")
//				.append("let",
//						new Document("vIssuerId", "$IssuerId").append("vMauSo", "$EInvoiceDetail.TTChung.MauSoHD"))
//				.append("pipeline", Arrays.asList(
//						new Document("$match",
//								new Document("$expr", new Document("$and",
//										Arrays.asList(new Document("$eq", Arrays.asList("$IssuerId", "$$vIssuerId")),
//												new Document("$eq",
//														Arrays.asList("$EInvoiceDetail.TTChung.MauSoHD", "$$vMauSo")),
//												new Document("$ne", Arrays.asList("$IsDelete", true)),
//												new Document("$in", Arrays.asList("$EInvoiceStatus",
//														Arrays.asList("COMPLETE", "ERROR_CQT", "PROCESSING", "XOABO",
//																"DELETED", "REPLACED", "ADJUSTED"))))))),
//						new Document("$group",
//								new Document("_id", "$EInvoiceDetail.TTChung.MauSoHD").append("SHDon",
//										new Document("$max", "$EInvoiceDetail.TTChung.SHDon")))))
//				.append("as", "EInvoiceMAXCQT")));
//		pipeline.add(new Document("$unwind",
//				new Document("path", "$EInvoiceMAXCQT").append("preserveNullAndEmptyArrays", true)));
		Document docTmp = null;


		MongoClient mongoClient = cfg.mongoClient();
		MongoCollection<Document> collection = mongoClient.getDatabase(cfg.dbName).getCollection("EInvoice");
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

//		int invoiceNumberCurrent = docTmp.getEmbedded(Arrays.asList("EInvoiceDetail", "TTChung", "SHDon"), 0);
//		int maxInvoiceSendedCQT = 0;
//		if (docTmp.get("EInvoiceMAXCQT") != null)
//			maxInvoiceSendedCQT = docTmp.getEmbedded(Arrays.asList("EInvoiceMAXCQT", "SHDon"), 0);
//		if (invoiceNumberCurrent == 0 || invoiceNumberCurrent != maxInvoiceSendedCQT + 1) {
//			// responseStatus = new MspResponseStatus(9999, "Có 1 hoặc 1 vài số hóa đơn
//			// trước đó chưa được xử lý xong.<br>Vui lòng kiểm tra lại danh sách hóa đơn.");
//			responseStatus = new MspResponseStatus(9999, "Vui lòng ký từ hóa đơn: " + (maxInvoiceSendedCQT + 1));
//			rsp.setResponseStatus(responseStatus);
//			return rsp;
//		}

		String signStatusCode = docTmp.get("SignStatusCode", "");
		if ("PROCESSING".equals(signStatusCode)) {
			responseStatus = new MspResponseStatus(9999, "Hóa đơn đã ký và đang chờ xử lý với cơ quan thuế.");
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
		 collection = mongoClient.getDatabase(cfg.dbName).getCollection("EInvoice");
		collection.findOneAndUpdate(docFind,
				new Document("$set", new Document("SignStatusCode", "SIGNED").append("InfoSigned",
						new Document("SignedDate", LocalDateTime.now()).append("SignedUserID", header.getUserId())
								.append("SignedUserName", header.getUserName())
								.append("SignedUserFullName", header.getUserFullName()))),
				options);
		mongoClient.close();
		

		DateTimeFormatter format_time = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
		LocalDateTime time_dem = LocalDateTime.now();
		String time = time_dem.format(format_time);
		String name_company = removeAccent(header.getUserFullName());
		System.out.println(time + " " + name_company + " vua ky thanh cong hoa don VAT");
		responseStatus = new MspResponseStatus(0, Constants.MAP_ERROR.get(0));
		rsp.setResponseStatus(responseStatus);
		return rsp;
	}

	@SuppressWarnings("unused")
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
				.append("IsDelete", false).append("SignStatusCode", "SIGNED")
				.append("EInvoiceStatus", new Document("$in", Arrays.asList("ERROR_CQT", "PROCESSING")));

		Document docTmp = null;


		MongoClient mongoClient = cfg.mongoClient();
		MongoCollection<Document> collection = mongoClient.getDatabase(cfg.dbName).getCollection("EInvoice");
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
			DateTimeFormatter format_time = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
			LocalDateTime time_dem = LocalDateTime.now();
			String time = time_dem.format(format_time);
			System.out.println(time + " " + "Loi callTraCuuThongDiep  " + MTDiep);
		}

		try {
			if (rTCTN == null) {
				rTCTN = tctnService.callTraCuuThongDiep(MTDiep);
			}
		} catch (Exception e) {
			DateTimeFormatter format_time = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
			LocalDateTime time_dem = LocalDateTime.now();
			String time = time_dem.format(format_time);
			System.out.println(time + " " + "Loi callTraCuuThongDiep  " + MTDiep);
		}

		if (rTCTN == null) {
			responseStatus = new MspResponseStatus(9999, "Vui lòng liên hệ nhà cung cấp để được xử lý!!!.");
			rsp.setResponseStatus(responseStatus);
			return rsp;
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
		String fileName1 = _id + "_signed.xml";
		file = new File(dir1, fileName1);
		MST = docTmp.getEmbedded(Arrays.asList("EInvoiceDetail", "NDHDon", "NBan", "MST"), "");
		
		log.info(LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")) 
	    		+ "Call tracuuthongdiep1 - MaMTDiep: "+MTDiep);
		
		if ("2".equals(MaKetQua)) {

			// call 3 lần mỗi lần 3s
			if (rTCTN == null) {
				rTCTN = tctnService.callTraCuuThongDiep(MTDiep);
				TimeUnit.SECONDS.sleep(3);
				DateTimeFormatter format_time = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
				LocalDateTime time_dem = LocalDateTime.now();
				String time = time_dem.format(format_time);
				System.out.println(time + " " + "callTraCuuThongDiep lan 1  " + MTDiep);
			}
			if (rTCTN == null) {
				rTCTN = tctnService.callTraCuuThongDiep(MTDiep);
				TimeUnit.SECONDS.sleep(3);
				DateTimeFormatter format_time = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
				LocalDateTime time_dem = LocalDateTime.now();
				String time = time_dem.format(format_time);
				System.out.println(time + " " + "callTraCuuThongDiep lan 2 " + MTDiep);
			}
			if (rTCTN == null) {
				rTCTN = tctnService.callTraCuuThongDiep(MTDiep);
				// Độ trễ
				TimeUnit.SECONDS.sleep(3);
				DateTimeFormatter format_time = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
				LocalDateTime time_dem = LocalDateTime.now();
				String time = time_dem.format(format_time);
				System.out.println(time + " " + "callTraCuuThongDiep lan 3  " + MTDiep);
			}
			nodeKetQuaTraCuu = (Node) xPath.evaluate("/KetQuaTraCuu", rTCTN, XPathConstants.NODE);
			MaKetQua = commons
					.getTextFromNodeXML((Element) xPath.evaluate("MaKetQua", nodeKetQuaTraCuu, XPathConstants.NODE));
			MoTaKetQua = commons
					.getTextFromNodeXML((Element) xPath.evaluate("MoTaKetQua", nodeKetQuaTraCuu, XPathConstants.NODE));

			if ("2".equals(MaKetQua) && "Mã giao dịch không đúng".equals(MoTaKetQua)) {
				doc = commons.fileToDocument(file, true);

				try {
					rTCTN1 = tctnService.callTiepNhanThongDiep("200", MTDiep, MST, "1", doc);
				} catch (Exception e) {
					DateTimeFormatter format_time = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
					LocalDateTime time_dem = LocalDateTime.now();
					String time = time_dem.format(format_time);
					System.out.println(time + " " + "callTiepNhanThongDiep   " + MTDiep);
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
		
		
		
		log.info(LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")) 
	    		+ "Call tracuuthongdiep ss - MaMTDiep: "+MTDiep);
		
		
		Node nodeTDiep = null;
		String checkMLTDiep = "";
		boolean check_ = false;
		String MLoi = "";
		String MTLoi = "";
		String MTDTChieu = "";
		String CQT_MLTDiep = "";

		String MLoi1 = "";
		String MTLoi1 = "";
		String CQT_MLTDiep1 = "";
		Node nodeTemp = null;
		NodeList tDiepNodes = (NodeList) xPath.evaluate("DuLieu/TDiep", nodeKetQuaTraCuu, XPathConstants.NODESET);
		for (int i = 0; i < tDiepNodes.getLength(); i++) {
			nodeTemp = (Node) tDiepNodes.item(i);
			checkMLTDiep = commons
					.getTextFromNodeXML((Element) xPath.evaluate("TTChung/MLTDiep", nodeTemp, XPathConstants.NODE));
			if (xPath.evaluate("DLieu/*/MCCQT", nodeTemp, XPathConstants.NODE) != null && checkMLTDiep.equals("202")) {
				nodeTDiep = nodeTemp;
				break;
			}
		}

		if (nodeTDiep == null) {
			for (int i = 0; i < tDiepNodes.getLength(); i++) {
				nodeTDiep = (Node) tDiepNodes.item(i);
				checkMLTDiep = commons.getTextFromNodeXML(
						(Element) xPath.evaluate("TTChung/MLTDiep", nodeTDiep, XPathConstants.NODE));
				if (checkMLTDiep.equals("202")) {
					break;
				}
				if (checkMLTDiep.equals("204")) {
					check_ = true;
					MLoi1 = commons.getTextFromNodeXML((Element) xPath.evaluate("DLieu/TBao/DLTBao/LCMa/DSLDo/LDo/MLoi",
							nodeTDiep, XPathConstants.NODE));
					MTLoi1 = commons.getTextFromNodeXML((Element) xPath
							.evaluate("DLieu/TBao/DLTBao/LCMa/DSLDo/LDo/MTLoi", nodeTDiep, XPathConstants.NODE));
					CQT_MLTDiep1 = checkMLTDiep;
					break;
				}
				if (checkMLTDiep.equals("-1")) {
					check_ = true;
					MLoi1 = commons
							.getTextFromNodeXML((Element) xPath.evaluate("DLieu/MLoi", nodeTDiep, XPathConstants.NODE));
					MTLoi1 = commons
							.getTextFromNodeXML((Element) xPath.evaluate("DLieu/MTa", nodeTDiep, XPathConstants.NODE));
					CQT_MLTDiep1 = checkMLTDiep;
					break;
				}
			}
		}
//		for (int i = 1; i <= 20; i++) {
//
//			if (xPath.evaluate("DuLieu/TDiep[" + i + "]", nodeKetQuaTraCuu, XPathConstants.NODE) == null)
//				break;
//			nodeTDiep = (Node) xPath.evaluate("DuLieu/TDiep[" + i + "]", nodeKetQuaTraCuu, XPathConstants.NODE);
//			checkMLTDiep = commons
//					.getTextFromNodeXML((Element) xPath.evaluate("TTChung/MLTDiep", nodeTDiep, XPathConstants.NODE));
//			if (checkMLTDiep.equals("202")) {
//				break;
//			}
//			if (checkMLTDiep.equals("204")) {
//				check_ = true;
//				MLoi1 = commons.getTextFromNodeXML((Element) xPath.evaluate("DLieu/TBao/DLTBao/LCMa/DSLDo/LDo/MLoi",
//						nodeTDiep, XPathConstants.NODE));
//				MTLoi1 = commons.getTextFromNodeXML((Element) xPath.evaluate("DLieu/TBao/DLTBao/LCMa/DSLDo/LDo/MTLoi",
//						nodeTDiep, XPathConstants.NODE));
//				CQT_MLTDiep1 = checkMLTDiep;
//				break;
//			}
//			if (checkMLTDiep.equals("-1")) {
//				check_ = true;
//				MLoi1 = commons
//						.getTextFromNodeXML((Element) xPath.evaluate("DLieu/MLoi", nodeTDiep, XPathConstants.NODE));
//				MTLoi1 = commons
//						.getTextFromNodeXML((Element) xPath.evaluate("DLieu/MTa", nodeTDiep, XPathConstants.NODE));
//				CQT_MLTDiep1 = checkMLTDiep;
//				break;
//			}
//
//		}

		if (nodeTDiep == null) {
			responseStatus = new MspResponseStatus(9999, "Chưa có kết quả trả về.");
			rsp.setResponseStatus(responseStatus);
			return rsp;
		}
		
		CQT_MLTDiep = commons
				.getTextFromNodeXML((Element) xPath.evaluate("TTChung/MLTDiep", nodeTDiep, XPathConstants.NODE));
		MTDTChieu = commons
				.getTextFromNodeXML((Element) xPath.evaluate("TTChung/MTDTChieu", nodeTDiep, XPathConstants.NODE));
		
		
		log.info(LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")) 
	    		+ "Get tracuuthongdiep - MaMTDiep: "+MTDiep+" CQT_MLTDiep: "+CQT_MLTDiep+" MTDTChieu: " + MTDTChieu);
		
		
		
		if (!CQT_MLTDiep.equals("202")) {
			if (check_ == true) {
				MLoi = MLoi1;
				MTLoi = MTLoi1;
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

				 mongoClient = cfg.mongoClient();
				 collection = mongoClient.getDatabase(cfg.dbName).getCollection("EInvoice");
				collection.findOneAndUpdate(docFind,
						new Document("$set",
								new Document("EInvoiceStatus", Constants.INVOICE_STATUS.ERROR_CQT)
										.append("CQT_Date", LocalDate.now())
										.append("LDo", new Document("MLoi", MLoi).append("MTLoi", MTLoi))),
						options);
				mongoClient.close();
				
				responseStatus = new MspResponseStatus(999,
						"".equals(MTLoi) ? "CQT chưa có thông báo kết quả trả về." : MTLoi);
				rsp.setResponseStatus(responseStatus);
				return rsp;
			}
		}

		String MCCQT = commons
				.getTextFromNodeXML((Element) xPath.evaluate("DLieu/HDon/MCCQT", nodeTDiep, XPathConstants.NODE));
		
		

//		log.info(LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")) 
//	    		+ "Get mccqttracuuthongdiep - MaMTDiep: "+MTDiep+" MCCQT: "+(MCCQT == null || MCCQT.trim().isEmpty() ? "_Rong_":MCCQT)
//	    		+" CQT_MLTDiep: "+CQT_MLTDiep+" MTDTChieu: " + MTDTChieu);
//		
//		
//		
		if (MCCQT == null || MCCQT.trim().isEmpty() || "".equals(MCCQT.trim())) {
//		    System.out.println(LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")) 
//		    		+ "MCCQT khong co du lieu sau khi call:"+MCCQT);
//		    
//		    log.info(LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")) 
//		    		+ "MCCQT khong co du lieu sau khi call:"+MCCQT);
		    
		    
			responseStatus = new MspResponseStatus(999,"CQT chưa cấp mã, vui lòng lấy lại mã sau!");
			rsp.setResponseStatus(responseStatus);
			return rsp;
		}  
//		else {
//		    System.out.println(LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")) 
//		    		+ "Ma thong diep: "+MTDiep+" MCCQT co du lieu: " + MCCQT);
//		    
//		    
//		    log.info(LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")) 
//		    		+ "Ma thong diep: "+MTDiep+" MCCQT co du lieu: " + MCCQT);
//		    
//		    
//		}

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

	
		MongoClient mongoClient2 = cfg.mongoClient();
		

		MongoCollection<Document> collection2 =  mongoClient2.getDatabase(cfg.dbName).getCollection("EInvoice");
		collection2.findOneAndUpdate(docFind,
				new Document("$set",
						new Document("EInvoiceStatus", Constants.INVOICE_STATUS.COMPLETE).append("MCCQT", MCCQT)
								.append("MTDTChieu", MTDTChieu).append("CQT_Date", LocalDate.now())
								.append("LDo", new Document("MLoi", "").append("MTLoi", ""))),
				options);
		mongoClient2.close();
//		System.out.println(LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")) + "id hoa don: "+_id);
//		System.out.println(LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")) + "vua update status tobe complete "+MCCQT);
//		log.info(LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")) + "id hoa don: "+_id);
//
//		log.info(LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")) + "vua update status tobe complete ");
//		log.info("MCCQT: "+MCCQT + " MTDTChieu: "+MTDTChieu);
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
			ObjectId byId =  docTmp.getEmbedded(Arrays.asList("_id"), ObjectId.class);
			Document docTTHDLQuan = new Document("_id", byId != null ? byId.toHexString():"")
					.append("TCHDon", docTmp.getEmbedded(Arrays.asList("EInvoiceDetail", "TTChung", "TTHDLQuan", "TCHDon"), ""))
					.append("LHDCLQuan", "1")
					.append("KHMSHDCLQuan", docTmp.getEmbedded(Arrays.asList("EInvoiceDetail", "TTChung","KHMSHDon"), ""))
					.append("KHHDCLQuan", docTmp.getEmbedded(Arrays.asList("EInvoiceDetail", "TTChung", "KHHDon"), ""))
					.append("SHDCLQuan",
							String.valueOf(docTmp.getEmbedded(Arrays.asList("EInvoiceDetail", "TTChung", "SHDon"), 0)))
					.append("NLHDCLQuan", commons.convertLocalDateTimeToString(
							commons.convertDateToLocalDateTime(
									docTmp.getEmbedded(Arrays.asList("EInvoiceDetail", "TTChung", "NLap"), Date.class)),
							"yyyy-MM-dd"))
					.append("GChu", "");
			
			Document docFind1 = new Document("IssuerId", header.getIssuerId()).append("IsDelete", false)
					.append("_id", objectIddc).append("SignStatusCode", Constants.INVOICE_SIGN_STATUS.SIGNED)
					.append("EInvoiceStatus", new Document("$in", Arrays.asList(Constants.INVOICE_STATUS.COMPLETE,
							Constants.INVOICE_STATUS.ADJUSTED, Constants.INVOICE_STATUS.REPLACED)));
			options = new FindOneAndUpdateOptions();
			options.upsert(true);
			options.maxTime(5000, TimeUnit.MILLISECONDS);
			options.returnDocument(ReturnDocument.AFTER);

			if ("1".equals(docTmp.getEmbedded(Arrays.asList("EInvoiceDetail", "TTChung", "TTHDLQuan", "TCHDon"), ""))) {
				 mongoClient2 = cfg.mongoClient();
				 collection = mongoClient2.getDatabase(cfg.dbName).getCollection("EInvoice");
				collection.findOneAndUpdate(docFind1,
						new Document("$set", new Document("EInvoiceStatus", "REPLACED").append("ReplacedBy", docTTHDLQuan)), options);
				mongoClient2.close();
				
				
			} else if ("2".equals(
					docTmp.getEmbedded(Arrays.asList("EInvoiceDetail", "TTChung", "TTHDLQuan", "TCHDon"), ""))) {			
				MongoClient mongoClient3 = cfg.mongoClient();
				 collection = mongoClient3.getDatabase(cfg.dbName).getCollection("EInvoice");
				collection.findOneAndUpdate(docFind1,
						new Document("$set", new Document("EInvoiceStatus", "ADJUSTED")), options);
				collection.findOneAndUpdate(docFind1,
			            new Document("$push", new Document("AdjustedBy", docTTHDLQuan)), options);
				mongoClient3.close();
			}
		}
		DateTimeFormatter format_time2 = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
		LocalDateTime time_dem2 = LocalDateTime.now();
		String time2 = time_dem2.format(format_time2);
		String name_company = removeAccent(header.getUserFullName());
		System.out.println(time2 + " " + name_company + " vua lay ma CQT hoa don VAT");
		responseStatus = new MspResponseStatus(0, Constants.MAP_ERROR.get(0));
		rsp.setResponseStatus(responseStatus);
		return rsp;
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
			MongoCollection<Document> collection = mongoClient.getDatabase(cfg.dbName).getCollection("EInvoice");
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

		String ParamUSD = docTmp.getEmbedded(Arrays.asList("EInvoiceDetail", "TToan", "ParamUSD"), "");
				
		String fileNameXML = _id + "_" + MCCQT + ".xml";
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
				fileName = _id + "_" + MCCQT + ".xml";

				String fileName_ = _id + "_" + MCCQT + "_" + SoHDon + ".xml";
				/* CHECK MCCQT GET DATA XML */

				File file_xml = new File(dir, fileName);

				org.w3c.dom.Document doc_xml = commons.fileToDocument(file_xml);

				XPath xPath_xml = XPathFactory.newInstance().newXPath();
				Node nodeHDon = null;
				for (int J = 1; J <= 20; J++) {
					nodeHDon = (Node) xPath_xml.evaluate("/KetQuaTraCuu/DuLieu/TDiep[" + J + "]/DLieu/HDon", doc_xml,
							XPathConstants.NODE);
					if (nodeHDon != null)
						break;
				}
				/* FILE CHUA DUOC DUI DEN CO QUAN THUE */
				if (null == nodeHDon) {
					nodeHDon = (Node) xPath_xml.evaluate("/HDon", doc_xml, XPathConstants.NODE);
				}

				DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
				DocumentBuilder builder = factory.newDocumentBuilder();
				org.w3c.dom.Document rTCTN_HDON = builder.newDocument();
				rTCTN_HDON.appendChild(rTCTN_HDON.importNode(nodeHDon, true));
				/* END CHECK GET DATA IN RESULT IN FILE XML TO TAX */
				boolean boo_ = false;
				boo_ = commons.docW3cToFile(rTCTN_HDON, dir, fileName_);

				if (boo_ == true) {
					fileName = fileName_;
					fileNameXML = fileName_;
				}
				/* END CHECK MCCQT GET DATA XML */

			} else {
				if ("SIGNED".equals(signStatusCode)) {
					fileName = _id + "_signed.xml";
				}
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

				baosPDF = jpUtils.createFinalInvoice(fileJP, doc, secureKey, CheckView, numberRowInPage, numberRowInPageMultiPage,
						numberCharsInRow, MST, link, ParamUSD,
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
				MongoCollection<Document> collection = mongoClient.getDatabase(cfg.dbName).getCollection("LogEmailUser");
					collection.insertOne(new Document("IssuerId", header.getIssuerId()).append("Title", _title)
							.append("Email", email_gui).append("IsActive", boo).append("MailCheck", boo)
							.append("IsDelete", false).append("EmailContent", _content)

					);			
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

	/*----------------------------------------------------- Start Import excel */
	@SuppressWarnings({ "unlikely-arg-type", "unused" })
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

		//
		objectId = null;
		objectIdUser = null;
		objectIdMSKH = null;

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


		Document findInforIssuer = new Document("_id", 1)
				.append("TaxCode", 1)
				.append("Name", 1)
				.append("Address", 1)
				.append("Phone", 1)
				.append("Fax", 1)
				.append("Email", 1)
				.append("Website", 1)
				.append("TinhThanhInfo", 1)
				.append("ChiCucThueInfo", 1)
				.append("BankAccount", 1)
				.append("NameEN", 1)
				.append("BankAccountExt", 1);
		
		pipeline = new ArrayList<Document>();
		pipeline.add(new Document("$match",
				new Document("_id", objectId).append("IsActive", true).append("IsDelete", false)));
		pipeline.add(new Document("$project" ,findInforIssuer));

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
		pipeline.add(
				new Document("$lookup",
						new Document("from", "PramLink")
								.append("pipeline",
										Arrays.asList(new Document("$match",
												new Document("$expr", new Document("IsDelete", false))),
												new Document("$project", new Document("_id", 1).append("LinkPortal", 1))
												))
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
		List<EInvoiceExcelForm> eInvoiceExcelFormList = new ArrayList<>();
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
				int lastColumn = Math.max(row1.getLastCellNum(), 30);

				for (int cn = 0; cn < lastColumn; cn++) {
					Cell c = row1.getCell(cn, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
					cells.add(c);
				}
				EInvoiceExcelForm eInvoiceExcelForm = extractInfoFromCell(cells);
				eInvoiceExcelFormList.add(eInvoiceExcelForm);
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
		if (eInvoiceExcelFormList != null) {
			for (int tam = 0; tam < eInvoiceExcelFormList.size(); tam++) {
				if (eInvoiceExcelFormList.get(tam).getMaHD() == null) {
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
			String tempTen = "";
			String tempMST = "";
			String tempDChi = "";
			String tempSDThoai = "";
			String tempDCTDTu = "";
			String tempMAKH = "";
			String tempSTKNHang = "";
			String tempTNHang = "";
			String tempHVTNMHang = "";
			Double tempTgTCThue = 0.0;
			Double tempTgTThue = 0.0;
			Double tempTgTTTBSo = 0.0;
			String tempTgTTTBChu = "";
			String tempTyGia = "";
			String tempHTTToan = "";
			List<DSHHDVu> dshhdVuList = new ArrayList<>();
			List<Object> listHHDVu = new ArrayList<Object>();
			int i = 0;
			int start = 0;
			int end = 0;
			int dem = 0;

			/* DOC FILE EXCEL - GHI DU LIEU VO LIST */
			for (; i < eInvoiceExcelFormList.size();) {
				dem = 0;
				for (int j = i; j < eInvoiceExcelFormList.size(); j++) {
					if (eInvoiceExcelFormList.get(i).getMaHD() == eInvoiceExcelFormList.get(j).getMaHD()) {
						// Xu ly
						dem++;
						start = j + 1;

						tempTen = eInvoiceExcelFormList.get(i).getTenDonVi();
						tempMST = eInvoiceExcelFormList.get(i).getMaSoThue();
						tempDChi = eInvoiceExcelFormList.get(i).getDiaChiKhachHang();
						tempSDThoai = eInvoiceExcelFormList.get(i).getSDTKhachHang();
						tempDCTDTu = eInvoiceExcelFormList.get(i).getMailKhachHang();
						tempMAKH = eInvoiceExcelFormList.get(i).getMaKH();
						tempSTKNHang = eInvoiceExcelFormList.get(i).getSoTaiKhoan();
						tempTNHang = eInvoiceExcelFormList.get(i).getTenNganHang();
						tempHVTNMHang = eInvoiceExcelFormList.get(i).getTenNguoiMua();
						tempTgTCThue = eInvoiceExcelFormList.get(i).getTongTienTruocThue();
						tempTgTThue = eInvoiceExcelFormList.get(i).getTTTGTGT();
						tempTgTTTBSo = eInvoiceExcelFormList.get(i).getTTDCTGTGT();
						tempTgTTTBChu = eInvoiceExcelFormList.get(i).getTTBangChu();
						tempTyGia = eInvoiceExcelFormList.get(i).getTyGia();
						tempHTTToan = eInvoiceExcelFormList.get(i).getHinhThucThanhToan();
						end = j;
						if (eInvoiceExcelFormList.size() == j + 1) {
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
				String TenForm = tempTen;
				String MaSoThueForm = tempMST;
				String DChiNMForm = tempDChi;
				String SDThoaiNMForm = tempSDThoai;
				String DCTDTuNMForm = tempDCTDTu;
				String MaKHNMForm = tempMAKH;
				String STKNHangNMForm = tempSTKNHang;
				String TNHangNMForm = tempTNHang;
				String HVTNMHangNMForm = tempHVTNMHang;
				Double tempTgTCThueForm = tempTgTCThue;
				Double tempTgTThueForm = tempTgTThue;
				Double tempTgTTTBSoForm = tempTgTTTBSo;
				String tempTgTTTBChuForm = tempTgTTTBChu;
				String TyGiaForm = tempTyGia;
				String HTTToanForm = tempHTTToan;

				if (checkMaHD == true) {
					if (dem > 1) {

						for (int k = i; k <= end; k++) {
							DSHHDVu dshhdVu = new DSHHDVu();
							dshhdVu.setSTT(eInvoiceExcelFormList.get(k).getSTT());
							dshhdVu.setProductName(eInvoiceExcelFormList.get(k).getTenHangHoa());
							dshhdVu.setProductCode(eInvoiceExcelFormList.get(k).getMaHangHoa());
							dshhdVu.setSLo(eInvoiceExcelFormList.get(k).getSLo());
							dshhdVu.setHanSD(eInvoiceExcelFormList.get(k).getHanSD());
							dshhdVu.setUnit(eInvoiceExcelFormList.get(k).getDonViTinh());
							dshhdVu.setQuantity(eInvoiceExcelFormList.get(k).getSoLuong());
							dshhdVu.setPrice(eInvoiceExcelFormList.get(k).getDonGia());
							dshhdVu.setTotal(eInvoiceExcelFormList.get(k).getThanhTien());
							dshhdVu.setVATRate(eInvoiceExcelFormList.get(k).getThueSuat());
							dshhdVu.setVATAmount(eInvoiceExcelFormList.get(k).getTienThue());
							dshhdVu.setAmount(eInvoiceExcelFormList.get(k).getTongTien());
							String TinhChat = eInvoiceExcelFormList.get(k).getTinhChat();
							switch (TinhChat) {
							case "1":
								dshhdVu.setFeature("1");
								break;
							case "2":
								dshhdVu.setFeature("2");
								break;
							case "3":
								dshhdVu.setFeature("3");
								break;
							case "4":
								dshhdVu.setFeature("4");
								break;
							default:
								break;
							}
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

						// docTmp.getEmbedded(Arrays.asList("DMMauSoKyHieu", "_id"),
						// ObjectId.class).toString()
						// Thông tin hóa đơn - TTChung
						String MaHD = eInvoiceExcelFormList.get(i).getMaHD();
						String THDon = "Hóa đơn giá trị gia tăng TT 78";
//                        String MauSoHD = docTmp.getEmbedded(Arrays.asList("DMMauSoKyHieu", "_id"), ObjectId.class).toString();
						String MauSoHD = mauSoHdon;
						String KHMSHDon = docTmp.getEmbedded(Arrays.asList("DMMauSoKyHieu", "KHMSHDon"), "").toString();
						String KHHDon = docTmp.getEmbedded(Arrays.asList("DMMauSoKyHieu", "KHHDon"), "").toString();
						LocalDateTime NLap = LocalDateTime.now();
						String DVTTe = eInvoiceExcelFormList.get(i).getLoaiTien();
						String TGia = TyGiaForm;
						String HTTToanCode = "";
						String HTTToan1 = "";
						switch (HTTToanForm) {
						case "1":
							HTTToanCode = "1";
							HTTToan1 = "Tiền mặt";
							break;
						case "2":
							HTTToanCode = "2";
							HTTToan1 = "Chuyển khoản";
							break;
						case "3":
							HTTToanCode = "3";
							HTTToan1 = "Tiền mặt/Chuyển khoản";
							break;
						case "4":
							HTTToanCode = "4";
							HTTToan1 = "Đối trừ công nợ";
							break;
						case "5":
							HTTToanCode = "5";
							HTTToan1 = "Không thu tiền";
							break;
						default:
							break;
						}
						String HTTToan = HTTToan1;
//						TTChung ttChung = new TTChung(MaHD, THDon, MauSoHD, KHMSHDon, KHHDon, NLap, DVTTe, TGia,
//								HTTToanCode, HTTToan);

						// Thông tin người bán - Thông tin người mua - NDHDon
						String Ten = docTmp.getEmbedded(Arrays.asList("Name"), "").toString();
						String MST = docTmp.getEmbedded(Arrays.asList("TaxCode"), "").toString();
						String DChi = docTmp.getEmbedded(Arrays.asList("Address"), "").toString();
						String SDThoai = docTmp.getEmbedded(Arrays.asList("Phone"), "").toString();
						String DCTDTu = "";
						String STKNHang = docTmp.getEmbedded(Arrays.asList("BankAccount", "AccountNumber"), "")
								.toString();
						String TNHang = docTmp.getEmbedded(Arrays.asList("BankAccount", "BankName"), "").toString();
						String Fax = docTmp.getEmbedded(Arrays.asList("Fax"), "").toString();
						String Website = docTmp.getEmbedded(Arrays.asList("Website"), "").toString();

						String TenNM = TenForm;
						String MSTNM = MaSoThueForm;
						String DChiNM = DChiNMForm;
						String SDThoaiNM = SDThoaiNMForm;
						String DCTDTuNM = DCTDTuNMForm;
						String MaKHangNM = MaKHNMForm;
						String STKNHangNM = STKNHangNMForm;
						String TNHangNM = TNHangNMForm;
						String HVTNMHangNM = HVTNMHangNMForm;

//						NBan nBan = new NBan(Ten, MST, DChi, SDThoai, DCTDTu, STKNHang, TNHang, Fax, Website);
//						NMua nMua = new NMua(TenNM, MSTNM, DChiNM, SDThoaiNM, DCTDTuNM, MaKHangNM, STKNHangNM, TNHangNM,
//								HVTNMHangNM);
//						NDHDon ndhDon = new NDHDon(nBan, nMua);

						// Thông tin thanh toán
						Double TgTCThue = tempTgTCThueForm;
						Double TgTThue = tempTgTThueForm;
						Double TgTTTBSo = tempTgTTTBSoForm;
						String TgTTTBChu = tempTgTTTBChuForm;
//						TToan tToan = new TToan(TgTCThue, TgTThue, TgTTTBSo, TgTTTBChu);

						String SignStatusCode = "NOSIGN";
						String EInvoiceStatus = "CREATED";
						String MTDiep = "";
						String SecureKey = "";
						String Dir = "";

//                        
//                        //
//                    	objectIdEInvoice = new ObjectId();
//            			path = Paths.get(SystemParams.DIR_E_INVOICE_DATA, taxCode, docTmp.getEmbedded(Arrays.asList("DMMauSoKyHieu", "_id"), ObjectId.class).toString());
//            			pathDir = path.toString();
//            			file = path.toFile();
//            			if(!file.exists()) file.mkdirs();
//            			/*TAO XML HOA DON*/
//            			fileNameXML = objectIdEInvoice.toString() + ".xml";
//                        //

						// Setting
						String codeMTD = "0315382923";
						String FileNameXML = "";
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

						Dir = pathDir;
						MTDiep = codeMTD + commons.csRandomAlphaNumbericString(46 - codeMTD.length()).toUpperCase();
						SecureKey = commons.csRandomNumbericString(6);
						// XML
						FileNameXML = fileNameXML;
						dbf = DocumentBuilderFactory.newInstance();
						db = dbf.newDocumentBuilder();
						doc = db.newDocument();
						doc.setXmlStandalone(true);

						root = doc.createElement("HDon");
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
																											// KHI KY
						elementSubContent.appendChild(commons.createElementWithValue(doc, "MHSo", ""));
						// Ngày lập
						elementSubContent.appendChild(
								commons.createElementWithValue(doc, "NLap", NLap.format(DateTimeFormatter.ISO_DATE)));
						// Số bảng kê
						elementSubContent.appendChild(commons.createElementWithValue(doc, "SBKe", ""));
						// Ngày bảng kê
						elementSubContent.appendChild(commons.createElementWithValue(doc, "NBKe", ""));
						// Đơn vị tiền tệ
						elementSubContent.appendChild(commons.createElementWithValue(doc, "DVTTe", DVTTe));
						// Tỷ giá
						elementSubContent.appendChild(commons.createElementWithValue(doc, "TGia", TGia));
						// Hình thức thanh toán
						elementSubContent.appendChild(commons.createElementWithValue(doc, "HTTToan", HTTToan));
						// MST tổ chức cung cấp giải pháp HĐĐT
						elementSubContent
								.appendChild(commons.createElementWithValue(doc, "MSTTCGP", SystemParams.MSTTCGP));
						// MST đơn vị nhận ủy nhiệm lập hóa đơn
						elementSubContent.appendChild(commons.createElementWithValue(doc, "MSTDVNUNLHDon", ""));
						// Tên đơn vị nhận ủy nhiệm lập hóa đơn
						elementSubContent.appendChild(commons.createElementWithValue(doc, "TDVNUNLHDon", ""));
						// Địa chỉ đơn vị nhận ủy nhiệm lập hóa đơn
						elementSubContent.appendChild(commons.createElementWithValue(doc, "DCDVNUNLHDon", ""));
						elementTmp = doc.createElement("TTKhac"); // THONG TIN KHAC
						elementTmp.appendChild(commons.createElementTTKhac(doc, "PortalLink", "string", link));
						elementTmp.appendChild(commons.createElementTTKhac(doc, "SecureKey", "string", SecureKey));
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
//            			elementTmp.appendChild(commons.createElementWithValue(doc, "TTKhac", ""));

						/* ADD THONG TIN TK NGAN HANG (NEU CO) */
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
						elementTmp.appendChild(commons.createElementWithValue(doc, "Ten", TenNM));
						elementTmp.appendChild(commons.createElementWithValue(doc, "MST", MSTNM));
						elementTmp.appendChild(commons.createElementWithValue(doc, "DChi", DChiNM));
						elementTmp.appendChild(commons.createElementWithValue(doc, "MKHang", MaKHangNM));
						elementTmp.appendChild(commons.createElementWithValue(doc, "SDThoai", SDThoaiNM));
						elementTmp.appendChild(commons.createElementWithValue(doc, "DCTDTu", DCTDTuNM));
						elementTmp.appendChild(commons.createElementWithValue(doc, "HVTNMHang", HVTNMHangNM));
						elementTmp.appendChild(commons.createElementWithValue(doc, "STKNHang", STKNHangNM));
						elementTmp.appendChild(commons.createElementWithValue(doc, "TNHang", TNHangNM));
						elementTmp.appendChild(commons.createElementWithValue(doc, "TTKhac", ""));
						elementSubContent.appendChild(elementTmp);

						mapVATAmount = new LinkedHashMap<String, Double>();
						mapAmount = new LinkedHashMap<String, Double>();
						elementTmp = doc.createElement("DSHHDVu"); // HH-DV
						// dshhdVuList, listHHDVu, \VATRate,
						// Json.serializer().nodeFromObject(msg.getObjData());

						for (Object o : listHHDVu) {
							if (!"".equals(o.equals("/ProductName"))) {
								JsonNode h = Json.serializer().nodeFromObject(o);
								tmp = commons.formatNumberReal(commons.getTextJsonNode(h.at("/VATRate")))
										.replaceAll(",", "");
								switch (tmp) {
								case "0":
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
								Boolean slo1 = false;
								Boolean hsd1 = false;
								String slo = commons.getTextJsonNode(h.at("/SLo"));
								String hsd = commons.getTextJsonNode(h.at("/HanSD"));
								if ("".equals(slo)) {
									slo1 = true;
								}
								if ("".equals(hsd)) {
									hsd1 = true;
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
								if (slo1 == false) {
									elementSubTmp.appendChild(commons.createElementWithValue(doc, "SLo",
											commons.getTextJsonNode(h.at("/SLo"))));
								}
								if (hsd1 == false) {
									elementSubTmp.appendChild(commons.createElementWithValue(doc, "HanSD",
											commons.getTextJsonNode(h.at("/HanSD"))));
								}
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
									elementSubTmp.appendChild(commons.createElementWithValue(doc, "TSuat", tmp));
								}

								elementSubTmp01 = doc.createElement("TTKhac");
								elementSubTmp01.appendChild(commons.createElementTTKhac(doc, "VATAmount", "decimal",
										commons.formatNumberReal(commons.getTextJsonNode(h.at("/VATAmount")))
												.replaceAll(",", "")));
								elementSubTmp01.appendChild(commons.createElementTTKhac(doc, "Amount", "decimal",
										commons.formatNumberReal(commons.getTextJsonNode(h.at("/Amount")))
												.replaceAll(",", "")));
								elementSubTmp.appendChild(elementSubTmp01);
								elementTmp.appendChild(elementSubTmp);

							}
						}
						elementSubContent.appendChild(elementTmp);

						elementTmp = doc.createElement("TToan"); // Thong tin thanh toan
						elementSubTmp = doc.createElement("THTTLTSuat");
						/* DANH SACH CAC LOAI THUE SUAT */

						// https: //
						// stackoverflow.com/questions/46898/how-do-i-efficiently-iterate-over-each-entry-in-a-java-map
						for (Map.Entry<String, Double> pair : mapVATAmount.entrySet()) {
							if (null != pair.getKey() && !"".equals(pair.getKey())) {
								elementSubTmp01 = doc.createElement("LTSuat");
								elementSubTmp01
										.appendChild(commons.createElementWithValue(doc, "TSuat", pair.getKey()));
								elementSubTmp01.appendChild(commons.createElementWithValue(doc, "ThTien",
										commons.formatNumberReal(mapAmount.get(pair.getKey())).replaceAll(",", "")));
								elementSubTmp01.appendChild(commons.createElementWithValue(doc, "TThue",
										String.format("%.0f", mapVATAmount.get(pair.getKey()))));
								elementSubTmp.appendChild(elementSubTmp01);
							}
						}
						elementTmp.appendChild(elementSubTmp);
////            			https://stackoverflow.com/questions/46898/how-do-i-efficiently-iterate-over-each-entry-in-a-java-map
//            			elementSubTmp01 = doc.createElement("LTSuat");
//    					elementSubTmp01.appendChild(commons.createElementWithValue(doc, "TSuat", commons.formatNumberReal(tmp).replaceAll(",", "") + "%"));
//    					elementSubTmp01.appendChild(commons.createElementWithValue(doc, "ThTien", commons.formatNumberReal(TgTTTBSo).replaceAll(",", "")));
//    					elementSubTmp01.appendChild(commons.createElementWithValue(doc, "TThue", commons.formatNumberReal(TgTThue).replaceAll(",", "")));
//    					elementSubTmp.appendChild(elementSubTmp01);	
//            			elementTmp.appendChild(elementSubTmp);

						elementTmp.appendChild(commons.createElementWithValue(doc, "TgTCThue",
								commons.formatNumberReal(TgTCThue).replaceAll(",", "")));
						elementTmp.appendChild(commons.createElementWithValue(doc, "TgTThue",
								commons.formatNumberReal(TgTThue).replaceAll(",", "")));

						elementSubTmp = doc.createElement("DSLPhi");
						elementSubTmp01 = doc.createElement("LPhi");
						elementSubTmp01.appendChild(commons.createElementWithValue(doc, "TLPhi", ""));
						elementSubTmp01.appendChild(commons.createElementWithValue(doc, "TPhi", "0"));
						elementSubTmp.appendChild(elementSubTmp01);

						elementTmp.appendChild(elementSubTmp);

						elementTmp.appendChild(commons.createElementWithValue(doc, "TTCKTMai", "0"));
						elementTmp.appendChild(commons.createElementWithValue(doc, "TgTTTBSo",
								commons.formatNumberReal(TgTTTBSo).replaceAll(",", "")));
						elementTmp.appendChild(commons.createElementWithValue(doc, "TgTTTBChu", TgTTTBChu));
						elementSubTmp = doc.createElement("TTKhac");
						elementTmp.appendChild(elementSubTmp);

						elementSubContent.appendChild(elementTmp);

						elementContent.appendChild(elementSubContent);
						// END - NDHDon: Nội dung hóa đơn

						isSdaveFile = commons.docW3cToFile(doc, Dir, FileNameXML);
						if (!isSdaveFile) {
							throw new Exception("Lưu dữ liệu không thành công.");
						}
						/* END - TAO XML HOA DON */
						// END XML"_id", objectIdEInvoice
						// lookup data
						docUpsert = new Document("_id", objectIdEInvoice).append("IssuerId", header.getIssuerId())
								.append("MTDiep", MTDiep)
								.append("EInvoiceDetail", new Document("TTChung",
										new Document("THDon", THDon).append("MaHD", MaHD).append("MauSoHD", MauSoHD)
												.append("KHMSHDon", KHMSHDon).append("KHHDon", KHHDon)
												.append("NLap", NLap).append("DVTTe", DVTTe).append("TGia", TGia)
												.append("HTTToanCode", HTTToanCode).append("HTTToan", HTTToan))
										.append("NDHDon",
												new Document("NBan",
														new Document("Ten", Ten).append("MST", MST).append("DChi", DChi)
																.append("SDThoai", SDThoai).append("DCTDTu", DCTDTu)
																.append("STKNHang", STKNHang).append("TNHang", TNHang)
																.append("Fax", Fax).append("Website", Website))
														.append("NMua", new Document("Ten", TenNM).append("MST", MSTNM)
																.append("DChi", DChiNM).append("MKHang", MaKHangNM)
																.append("SDThoai", SDThoaiNM).append("DCTDTu", DCTDTuNM)
																.append("HVTNMHang", HVTNMHangNM)
																.append("STKNHang", STKNHangNM)
																.append("TNHang", TNHangNM)))

										.append("DSHHDVu", listHHDVu).append("TToan",
												new Document("TgTCThue", TgTCThue).append("TgTThue", TgTThue)
														.append("TgTTTBSo", TgTTTBSo).append("TgTTTBChu", TgTTTBChu)))
								.append("SignStatusCode", SignStatusCode).append("EInvoiceStatus", EInvoiceStatus)
								.append("IsDelete", false).append("SecureKey", SecureKey).append("Dir", Dir)
								.append("FileNameXML", FileNameXML).append("InfoCreated",
										new Document("CreateDate", LocalDateTime.now())
												.append("CreateUserID", header.getUserId())
												.append("CreateUserName", header.getUserName())
												.append("CreateUserFullName", header.getUserFullName()));
					
						 mongoClient = cfg.mongoClient();
						 collection = mongoClient.getDatabase(cfg.dbName).getCollection("EInvoice");
						collection.insertOne(docUpsert);
						mongoClient.close();
						
						dshhdVuList.clear();
						listHHDVu.clear();
						checkMaHD = false;
					} else {
						if (dem == 1) {
							int k = i;
							DSHHDVu dshhdVu = new DSHHDVu();
							dshhdVu.setSTT(eInvoiceExcelFormList.get(k).getSTT());
							dshhdVu.setProductName(eInvoiceExcelFormList.get(k).getTenHangHoa());
							dshhdVu.setProductCode(eInvoiceExcelFormList.get(k).getMaHangHoa());
							dshhdVu.setSLo(eInvoiceExcelFormList.get(k).getSLo());
							dshhdVu.setHanSD(eInvoiceExcelFormList.get(k).getHanSD());
							dshhdVu.setUnit(eInvoiceExcelFormList.get(k).getDonViTinh());
							dshhdVu.setQuantity(eInvoiceExcelFormList.get(k).getSoLuong());
							dshhdVu.setPrice(eInvoiceExcelFormList.get(k).getDonGia());
							dshhdVu.setTotal(eInvoiceExcelFormList.get(k).getThanhTien());
							dshhdVu.setVATRate(eInvoiceExcelFormList.get(k).getThueSuat());
							dshhdVu.setVATAmount(eInvoiceExcelFormList.get(k).getTienThue());
							dshhdVu.setAmount(eInvoiceExcelFormList.get(k).getTongTien());
							String TinhChat = eInvoiceExcelFormList.get(k).getTinhChat();
							switch (TinhChat) {
							case "1":
								dshhdVu.setFeature("1");
								break;
							case "2":
								dshhdVu.setFeature("2");
								break;
							case "3":
								dshhdVu.setFeature("3");
								break;
							case "4":
								dshhdVu.setFeature("4");
								break;
							default:
								break;
							}
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

							// Thông tin hóa đơn - TTChung
							String MaHD = eInvoiceExcelFormList.get(i).getMaHD();
							String THDon = "Hóa đơn giá trị gia tăng TT 78";
//                            String MauSoHD = "62610fc6dd79cb7e4571890f";
//                            String MauSoHD = docTmp.getEmbedded(Arrays.asList("DMMauSoKyHieu", "_id"), ObjectId.class).toString();
							String MauSoHD = mauSoHdon;
							String KHMSHDon = docTmp.getEmbedded(Arrays.asList("DMMauSoKyHieu", "KHMSHDon"), "")
									.toString();
							String KHHDon = docTmp.getEmbedded(Arrays.asList("DMMauSoKyHieu", "KHHDon"), "").toString();
							LocalDateTime NLap = LocalDateTime.now();
							String DVTTe = eInvoiceExcelFormList.get(i).getLoaiTien();
							String TGia = TyGiaForm;
							String HTTToanCode = "";
							String HTTToan1 = "";
							switch (HTTToanForm) {
							case "1":
								HTTToanCode = "1";
								HTTToan1 = "Tiền mặt";
								break;
							case "2":
								HTTToanCode = "2";
								HTTToan1 = "Chuyển khoản";
								break;
							case "3":
								HTTToanCode = "3";
								HTTToan1 = "Tiền mặt/Chuyển khoản";
								break;
							case "4":
								HTTToanCode = "4";
								HTTToan1 = "Đối trừ công nợ";
								break;
							case "5":
								HTTToanCode = "5";
								HTTToan1 = "Không thu tiền";
								break;
							default:
								break;
							}
							String HTTToan = HTTToan1;
//                            TTChung ttChung = new TTChung(MaHD,MaKH,THDon, MauSoHD, KHMSHDon, KHHDon, NLap, DVTTe, TGia, HTTToanCode, HTTToan);

							// Thông tin người bán - Thông tin người mua - NDHDon
							String Ten = docTmp.getEmbedded(Arrays.asList("Name"), "").toString();
							String MST = docTmp.getEmbedded(Arrays.asList("TaxCode"), "").toString();
							String DChi = docTmp.getEmbedded(Arrays.asList("Address"), "").toString();
//							String MKHang = "";
							String SDThoai = docTmp.getEmbedded(Arrays.asList("Phone"), "").toString();
							String DCTDTu = "";
//							String HVTNMHang = "";
							String STKNHang = docTmp.getEmbedded(Arrays.asList("BankAccount", "AccountNumber"), "")
									.toString();
							String TNHang = docTmp.getEmbedded(Arrays.asList("BankAccount", "BankName"), "").toString();
							String Fax = docTmp.getEmbedded(Arrays.asList("Fax"), "").toString();
							String Website = docTmp.getEmbedded(Arrays.asList("Website"), "").toString();

							String TenNM = TenForm;
							String MSTNM = MaSoThueForm;
							String DChiNM = DChiNMForm;
							String SDThoaiNM = SDThoaiNMForm;
							String DCTDTuNM = DCTDTuNMForm;
							String MaKHangNM = MaKHNMForm;
							String STKNHangNM = STKNHangNMForm;
							String TNHangNM = TNHangNMForm;
							String HVTNMHangNM = HVTNMHangNMForm;

							// NBan nBan = new NBan(Ten, MST, DChi, SDThoai, DCTDTu, STKNHang, TNHang, Fax,
							// Website);
//                            NMua nMua = new NMua(TenNM, MSTNM, DChiNM, SDThoaiNM, DCTDTuNM,MaKHang, STKNHangNM, TNHangNM, HVTNMHangNM);

//                            NDHDon ndhDon = new NDHDon(nBan, nMua);

							// Thông tin thanh toán
							Double TgTCThue = tempTgTCThueForm;
							Double TgTThue = tempTgTThueForm;
							Double TgTTTBSo = tempTgTTTBSoForm;
							String TgTTTBChu = tempTgTTTBChuForm;
//							TToan tToan = new TToan(TgTCThue, TgTThue, TgTTTBSo, TgTTTBChu);

							// Một số thông tin khác
							String SignStatusCode = "NOSIGN";
							String EInvoiceStatus = "CREATED";
							Boolean IsDelete = false;
							String MTDiep = "";
							String SecureKey = "";
							String Dir = "";

							// Setting
							String codeMTD = "0315382923";
							String FileNameXML = "";
							String pathDir = "";
							File file1 = null;
							Path path1 = null;
							ObjectId objectIdEInvoice = null;
							String fileNameXML = "";
							objectIdEInvoice = new ObjectId();
							String taxCode = "";
							taxCode = docTmp.getString("TaxCode");
							path1 = Paths.get(SystemParams.DIR_E_INVOICE_DATA, taxCode, docTmp
									.getEmbedded(Arrays.asList("DMMauSoKyHieu", "_id"), ObjectId.class).toString());
							pathDir = path1.toString();
							file1 = path1.toFile();
							if (!file1.exists())
								file1.mkdirs();
							fileNameXML = objectIdEInvoice.toString() + ".xml";
							pathDir = path1.toString();
							file1 = path1.toFile();
							if (!file1.exists())
								file1.mkdirs();
							Dir = pathDir;
							MTDiep = codeMTD + commons.csRandomAlphaNumbericString(46 - codeMTD.length()).toUpperCase();
							SecureKey = commons.csRandomNumbericString(6);
							// XML
							FileNameXML = fileNameXML;
							dbf = DocumentBuilderFactory.newInstance();
							db = dbf.newDocumentBuilder();
							doc = db.newDocument();
							doc.setXmlStandalone(true);

							root = doc.createElement("HDon");
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
																												// SINH
																												// KHI
																												// KY
							elementSubContent.appendChild(commons.createElementWithValue(doc, "MHSo", ""));
							// Ngày lập
							elementSubContent.appendChild(commons.createElementWithValue(doc, "NLap",
									NLap.format(DateTimeFormatter.ISO_DATE)));
							// Số bảng kê
							elementSubContent.appendChild(commons.createElementWithValue(doc, "SBKe", ""));
							// Ngày bảng kê
							elementSubContent.appendChild(commons.createElementWithValue(doc, "NBKe", ""));
							// Đơn vị tiền tệ
							elementSubContent.appendChild(commons.createElementWithValue(doc, "DVTTe", DVTTe));
							// Tỷ giá
							elementSubContent.appendChild(commons.createElementWithValue(doc, "TGia", TGia));
							// Hình thức thanh toán
							elementSubContent.appendChild(commons.createElementWithValue(doc, "HTTToan", HTTToan));
							// MST tổ chức cung cấp giải pháp HĐĐT
							elementSubContent
									.appendChild(commons.createElementWithValue(doc, "MSTTCGP", SystemParams.MSTTCGP));
							// MST đơn vị nhận ủy nhiệm lập hóa đơn
							elementSubContent.appendChild(commons.createElementWithValue(doc, "MSTDVNUNLHDon", ""));
							// Tên đơn vị nhận ủy nhiệm lập hóa đơn
							elementSubContent.appendChild(commons.createElementWithValue(doc, "TDVNUNLHDon", ""));
							// Địa chỉ đơn vị nhận ủy nhiệm lập hóa đơn
							elementSubContent.appendChild(commons.createElementWithValue(doc, "DCDVNUNLHDon", ""));
							elementTmp = doc.createElement("TTKhac"); // THONG TIN KHAC
							elementTmp.appendChild(commons.createElementTTKhac(doc, "PortalLink", "string", link));
							elementTmp.appendChild(commons.createElementTTKhac(doc, "SecureKey", "string", SecureKey));
							elementTmp.appendChild(commons.createElementTTKhac(doc, "SystemKey", "string",
									objectIdEInvoice.toString()));

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
//                			elementTmp.appendChild(commons.createElementWithValue(doc, "TTKhac", ""));

							/* ADD THONG TIN TK NGAN HANG (NEU CO) */
							elementSubTmp = doc.createElement("TTKhac");
							elementSubTmp.appendChild(
									commons.createElementTTKhac(doc, "TenEN", "string", docTmp.get("NameEN", "")));

							elementSubTmp.appendChild(
									commons.createElementTTKhac(doc, "STKNHang" + intTmp, "string", STKNHang));
							elementSubTmp
									.appendChild(commons.createElementTTKhac(doc, "TNHang" + intTmp, "string", TNHang));

							elementTmp.appendChild(elementSubTmp);
							elementSubContent.appendChild(elementTmp);

							elementTmp = doc.createElement("NMua"); // NGUOI MUA
							elementTmp.appendChild(commons.createElementWithValue(doc, "Ten", TenNM));
							elementTmp.appendChild(commons.createElementWithValue(doc, "MST", MSTNM));
							elementTmp.appendChild(commons.createElementWithValue(doc, "DChi", DChiNM));
							elementTmp.appendChild(commons.createElementWithValue(doc, "MKHang", MaKHangNM));
							elementTmp.appendChild(commons.createElementWithValue(doc, "SDThoai", SDThoaiNM));
							elementTmp.appendChild(commons.createElementWithValue(doc, "DCTDTu", DCTDTuNM));
							elementTmp.appendChild(commons.createElementWithValue(doc, "HVTNMHang", HVTNMHangNM));
							elementTmp.appendChild(commons.createElementWithValue(doc, "STKNHang", STKNHangNM));
							elementTmp.appendChild(commons.createElementWithValue(doc, "TNHang", TNHangNM));
							elementTmp.appendChild(commons.createElementWithValue(doc, "TTKhac", ""));
							elementSubContent.appendChild(elementTmp);

							mapVATAmount = new LinkedHashMap<String, Double>();
							mapAmount = new LinkedHashMap<String, Double>();
							elementTmp = doc.createElement("DSHHDVu"); // HH-DV
							// dshhdVuList, listHHDVu, \VATRate,
							// Json.serializer().nodeFromObject(msg.getObjData());

							for (Object o : listHHDVus) {
								if (!"".equals(o.equals("/ProductName"))) {
									JsonNode h = Json.serializer().nodeFromObject(o);
									tmp = commons.formatNumberReal(commons.getTextJsonNode(h.at("/VATRate")))
											.replaceAll(",", "");
									switch (tmp) {
									case "0":
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
											return (v == null ? commons
													.ToNumber(commons.getTextJsonNode(h.at("/Total")))
													* ("3".equals(commons.getTextJsonNode(h.at("/Feature"))) ? -1 : 1)
													: v + commons.ToNumber(commons.getTextJsonNode(h.at("/Total")))
															* ("3".equals(commons.getTextJsonNode(h.at("/Feature")))
																	? -1
																	: 1));
										});
										mapVATAmount.compute(tmp, (f, v) -> {
											return (v == null ? commons
													.ToNumber(commons.getTextJsonNode(h.at("/VATAmount")))
													* ("3".equals(commons.getTextJsonNode(h.at("/Feature"))) ? -1 : 1)
													: v + commons.ToNumber(commons.getTextJsonNode(h.at("/VATAmount")))
															* ("3".equals(commons.getTextJsonNode(h.at("/Feature")))
																	? -1
																	: 1));
										});
									}
									Boolean slo1 = false;
									Boolean hsd1 = false;
									String slo = commons.getTextJsonNode(h.at("/SLo"));
									String hsd = commons.getTextJsonNode(h.at("/HanSD"));
									if ("".equals(slo)) {
										slo1 = true;
									}
									if ("".equals(hsd)) {
										hsd1 = true;
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
									if (slo1 == false) {
										elementSubTmp.appendChild(commons.createElementWithValue(doc, "SLo",
												commons.getTextJsonNode(h.at("/SLo"))));
									}
									if (hsd1 == false) {
										elementSubTmp.appendChild(commons.createElementWithValue(doc, "HanSD",
												commons.getTextJsonNode(h.at("/HanSD"))));
									}
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
										String thtien = commons.getTextJsonNode(h.at("/Total")).replaceAll(",", "");
										elementSubTmp.appendChild(commons.createElementWithValue(doc, "ThTien",
												commons.formatNumberReal(thtien).replaceAll(",", "")));
										elementSubTmp.appendChild(commons.createElementWithValue(doc, "TSuat", tmp));
									}

									elementSubTmp01 = doc.createElement("TTKhac");
									elementSubTmp01.appendChild(commons.createElementTTKhac(doc, "VATAmount", "decimal",
											commons.formatNumberReal(commons.getTextJsonNode(h.at("/VATAmount")))
													.replaceAll(",", "")));
									elementSubTmp01.appendChild(commons.createElementTTKhac(doc, "Amount", "decimal",
											commons.formatNumberReal(commons.getTextJsonNode(h.at("/Amount")))
													.replaceAll(",", "")));
									elementSubTmp.appendChild(elementSubTmp01);
									elementTmp.appendChild(elementSubTmp);

								}
							}
							elementSubContent.appendChild(elementTmp);

							elementTmp = doc.createElement("TToan"); // Thong tin thanh toan
							elementSubTmp = doc.createElement("THTTLTSuat");

							/* DANH SACH CAC LOAI THUE SUAT */

							// https://stackoverflow.com/questions/46898/how-do-i-efficiently-iterate-over-each-entry-in-a-java-map
							for (Map.Entry<String, Double> pair : mapVATAmount.entrySet()) {
								if (null != pair.getKey() && !"".equals(pair.getKey())) {
									elementSubTmp01 = doc.createElement("LTSuat");
									elementSubTmp01
											.appendChild(commons.createElementWithValue(doc, "TSuat", pair.getKey()));
									elementSubTmp01.appendChild(commons.createElementWithValue(doc, "ThTien", commons
											.formatNumberReal(mapAmount.get(pair.getKey())).replaceAll(",", "")));
									elementSubTmp01.appendChild(commons.createElementWithValue(doc, "TThue",
											String.format("%.0f", mapVATAmount.get(pair.getKey()))));
									elementSubTmp.appendChild(elementSubTmp01);
								}
							}
							elementTmp.appendChild(elementSubTmp);
//                			/*DANH SACH CAC LOAI THUE SUAT*/
//                			
////                			https://stackoverflow.com/questions/46898/how-do-i-efficiently-iterate-over-each-entry-in-a-java-map
//                			elementSubTmp01 = doc.createElement("LTSuat");
//        					elementSubTmp01.appendChild(commons.createElementWithValue(doc, "TSuat", commons.formatNumberReal(tmp).replaceAll(",", "") + "%"));
//        					elementSubTmp01.appendChild(commons.createElementWithValue(doc, "ThTien", commons.formatNumberReal(TgTTTBSo).replaceAll(",", "")));
//        					elementSubTmp01.appendChild(commons.createElementWithValue(doc, "TThue", commons.formatNumberReal(TgTThue).replaceAll(",", "")));
//        					elementSubTmp.appendChild(elementSubTmp01);	
//                			elementTmp.appendChild(elementSubTmp);
//                			
							elementTmp.appendChild(commons.createElementWithValue(doc, "TgTCThue",
									commons.formatNumberReal(TgTCThue).replaceAll(",", "")));
							elementTmp.appendChild(commons.createElementWithValue(doc, "TgTThue",
									commons.formatNumberReal(TgTThue).replaceAll(",", "")));

							elementSubTmp = doc.createElement("DSLPhi");
							elementSubTmp01 = doc.createElement("LPhi");
							elementSubTmp01.appendChild(commons.createElementWithValue(doc, "TLPhi", ""));
							elementSubTmp01.appendChild(commons.createElementWithValue(doc, "TPhi", "0"));
							elementSubTmp.appendChild(elementSubTmp01);

							elementTmp.appendChild(elementSubTmp);

							elementTmp.appendChild(commons.createElementWithValue(doc, "TTCKTMai", "0"));
							elementTmp.appendChild(commons.createElementWithValue(doc, "TgTTTBSo",
									commons.formatNumberReal(TgTTTBSo).replaceAll(",", "")));
							elementTmp.appendChild(commons.createElementWithValue(doc, "TgTTTBChu", TgTTTBChu));
							elementSubTmp = doc.createElement("TTKhac");
							elementTmp.appendChild(elementSubTmp);

							elementSubContent.appendChild(elementTmp);

							elementContent.appendChild(elementSubContent);
							// END - NDHDon: Nội dung hóa đơn

							isSdaveFile = commons.docW3cToFile(doc, Dir, FileNameXML);
							if (!isSdaveFile) {
								throw new Exception("Lưu dữ liệu không thành công.");
							}
							/* END - TAO XML HOA DON */
							// END XML
							// lookup data
							docUpsert = new Document("_id", objectIdEInvoice).append("IssuerId", header.getIssuerId())
									.append("MTDiep", MTDiep)
									.append("EInvoiceDetail", new Document("TTChung",
											new Document("THDon", THDon).append("MaHD", MaHD).append("MauSoHD", MauSoHD)
													.append("KHMSHDon", KHMSHDon).append("KHHDon", KHHDon)
													.append("NLap", NLap).append("DVTTe", DVTTe).append("TGia", TGia)
													.append("HTTToanCode", HTTToanCode).append("HTTToan", HTTToan))
											.append("NDHDon", new Document("NBan",
													new Document("Ten", Ten).append("MST", MST).append("DChi", DChi)
															.append("SDThoai", SDThoai).append("DCTDTu", DCTDTu)
															.append("STKNHang", STKNHang).append("TNHang", TNHang)
															.append("Fax", Fax).append("Website", Website))
													.append("NMua", new Document("Ten", TenNM).append("MST", MSTNM)
															.append("DChi", DChiNM).append("MKHang", MaKHangNM)
															.append("SDThoai", SDThoaiNM).append("DCTDTu", DCTDTuNM)
															.append("HVTNMHang", HVTNMHangNM)
															.append("STKNHang", STKNHangNM).append("TNHang", TNHangNM)))

											.append("DSHHDVu", listHHDVus).append("TToan",
													new Document("TgTCThue", TgTCThue).append("TgTThue", TgTThue)
															.append("TgTTTBSo", TgTTTBSo)
															.append("TgTTTBChu", TgTTTBChu)))
									.append("SignStatusCode", SignStatusCode).append("EInvoiceStatus", EInvoiceStatus)
									.append("IsDelete", IsDelete).append("SecureKey", SecureKey).append("Dir", Dir)
									.append("FileNameXML", FileNameXML).append("InfoCreated",
											new Document("CreateDate", LocalDateTime.now())
													.append("CreateUserID", header.getUserId())
													.append("CreateUserName", header.getUserName())
													.append("CreateUserFullName", header.getUserFullName()));
						
							 mongoClient = cfg.mongoClient();
							 collection = mongoClient.getDatabase(cfg.dbName).getCollection("EInvoice");
							collection.insertOne(docUpsert);
							mongoClient.close();
							
							
							dshhdVuList.clear();
							listHHDVus.clear();
							checkMaHD = false;
						}
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

	/*----------------------------------------------------- End Import excel */
	private static EInvoiceExcelForm extractInfoFromCell(List<Cell> cells) {
		EInvoiceExcelForm eInvoiceExcelForm = new EInvoiceExcelForm();
		// Ma hoa don
		Cell MaHD = cells.get(0);
		if (MaHD != null) {
			switch (MaHD.getCellType()) {
			case STRING:
				eInvoiceExcelForm.setMaHD(MaHD.getStringCellValue());
				break;
			case NUMERIC:
				eInvoiceExcelForm.setMaHD((NumberToTextConverter.toText(MaHD.getNumericCellValue())));
				break;
			case BLANK:
				break;
			default:
				break;
			}
		}
		// Ma so thue
		Cell MaSoThue = cells.get(1);
		if (MaSoThue != null) {
			switch (MaSoThue.getCellType()) {
			case STRING:
				eInvoiceExcelForm.setMaSoThue(MaSoThue.getStringCellValue());
				break;
			case NUMERIC:
				eInvoiceExcelForm.setMaSoThue((NumberToTextConverter.toText(MaSoThue.getNumericCellValue())));
				break;
			case BLANK:
				break;
			default:
				break;
			}
		}
		// Ten nguoi mua
		Cell TenNguoiMua = cells.get(2);
		if (TenNguoiMua != null) {
			switch (TenNguoiMua.getCellType()) {
			case STRING:
				eInvoiceExcelForm.setTenNguoiMua(TenNguoiMua.getStringCellValue());
				break;
			case NUMERIC:
				eInvoiceExcelForm.setTenNguoiMua((NumberToTextConverter.toText(TenNguoiMua.getNumericCellValue())));
				break;
			case BLANK:
				break;
			default:
				break;
			}
		}
		// Ten don vi
		Cell TenDonVi = cells.get(3);
		if (TenDonVi != null) {
			switch (TenDonVi.getCellType()) {
			case STRING:
				eInvoiceExcelForm.setTenDonVi(TenDonVi.getStringCellValue());
				break;
			case NUMERIC:
				eInvoiceExcelForm.setTenDonVi((NumberToTextConverter.toText(TenDonVi.getNumericCellValue())));
				break;
			case BLANK:
				break;
			default:
				break;
			}
		}
		// Dia chi khach hang
		Cell DiaChiKhachHang = cells.get(4);
		if (DiaChiKhachHang != null) {
			switch (DiaChiKhachHang.getCellType()) {
			case STRING:
				eInvoiceExcelForm.setDiaChiKhachHang(DiaChiKhachHang.getStringCellValue());
				break;
			case NUMERIC:
				eInvoiceExcelForm
						.setDiaChiKhachHang((NumberToTextConverter.toText(DiaChiKhachHang.getNumericCellValue())));
				break;
			case BLANK:
				break;
			default:
				break;
			}

		}
		// Mail khach hang
		Cell MailKhachHang = cells.get(5);
		if (MailKhachHang != null) {
			switch (MailKhachHang.getCellType()) {
			case STRING:
				eInvoiceExcelForm.setMailKhachHang(MailKhachHang.getStringCellValue());
				break;
			case NUMERIC:
				eInvoiceExcelForm.setMailKhachHang((NumberToTextConverter.toText(MailKhachHang.getNumericCellValue())));
				break;
			case BLANK:
				break;
			default:
				break;
			}

		}
		// SDT khach hang
		Cell SDTKhachHang = cells.get(6);
		if (SDTKhachHang != null) {
			switch (SDTKhachHang.getCellType()) {
			case STRING:
				eInvoiceExcelForm.setSDTKhachHang(SDTKhachHang.getStringCellValue());
				break;
			case NUMERIC:
				eInvoiceExcelForm.setSDTKhachHang((NumberToTextConverter.toText(SDTKhachHang.getNumericCellValue())));
				break;
			case BLANK:
				break;
			default:
				break;
			}
		}
		// So tai khoan khach hang
		Cell SoTaiKhoan = cells.get(7);
		if (SoTaiKhoan != null) {
			switch (SoTaiKhoan.getCellType()) {
			case STRING:
				eInvoiceExcelForm.setSoTaiKhoan(SoTaiKhoan.getStringCellValue());
				break;
			case NUMERIC:
				eInvoiceExcelForm.setSoTaiKhoan((NumberToTextConverter.toText(SoTaiKhoan.getNumericCellValue())));
				break;
			case BLANK:
				break;
			default:
				break;
			}
		}
		// Ten ngan hang
		Cell TenNganHang = cells.get(8);
		if (TenNganHang != null) {
			switch (TenNganHang.getCellType()) {
			case STRING:
				eInvoiceExcelForm.setTenNganHang(TenNganHang.getStringCellValue());
				break;
			case NUMERIC:
				eInvoiceExcelForm.setTenNganHang((NumberToTextConverter.toText(TenNganHang.getNumericCellValue())));
				break;
			case BLANK:
				break;
			default:
				break;
			}

		}
		// hinh thuc thanh toan
		Cell HinhThucThanhToan = cells.get(9);
		if (HinhThucThanhToan != null) {
			switch (HinhThucThanhToan.getCellType()) {
			case STRING:
				eInvoiceExcelForm.setHinhThucThanhToan(HinhThucThanhToan.getStringCellValue());
				break;
			case NUMERIC:
				eInvoiceExcelForm
						.setHinhThucThanhToan((NumberToTextConverter.toText(HinhThucThanhToan.getNumericCellValue())));
				break;
			case BLANK:
				break;
			default:
				break;
			}
		}
		// Loai tien
		Cell LoaiTien = cells.get(10);
		if (LoaiTien != null) {
			switch (LoaiTien.getCellType()) {
			case STRING:
				eInvoiceExcelForm.setLoaiTien(LoaiTien.getStringCellValue());
				break;
			case NUMERIC:
				eInvoiceExcelForm.setLoaiTien((NumberToTextConverter.toText(LoaiTien.getNumericCellValue())));
				break;
			case BLANK:
				break;
			default:
				break;
			}

		}
		// STT San pham
		Cell STT = cells.get(11);
		if (STT != null) {
			switch (STT.getCellType()) {
			case STRING:
				eInvoiceExcelForm.setSTT(STT.getStringCellValue());
				break;
			case NUMERIC:
				eInvoiceExcelForm.setSTT((NumberToTextConverter.toText(STT.getNumericCellValue())));
				break;
			case BLANK:
				break;
			default:
				break;
			}
		}
		// Ten hang hoa
		Cell TenHangHoa = cells.get(12);
		if (TenHangHoa != null) {
			switch (TenHangHoa.getCellType()) {
			case STRING:
				eInvoiceExcelForm.setTenHangHoa(TenHangHoa.getStringCellValue());
				break;
			case NUMERIC:
				eInvoiceExcelForm.setTenHangHoa((NumberToTextConverter.toText(TenHangHoa.getNumericCellValue())));
				break;
			case BLANK:
				break;
			default:
				break;
			}
		}
		// Ma hang hoa
		Cell MaHangHoa = cells.get(13);
		if (MaHangHoa != null) {
			switch (MaHangHoa.getCellType()) {
			case STRING:
				eInvoiceExcelForm.setMaHangHoa(MaHangHoa.getStringCellValue());
				break;
			case NUMERIC:
				eInvoiceExcelForm.setMaHangHoa((NumberToTextConverter.toText(MaHangHoa.getNumericCellValue())));
				break;
			case BLANK:
				break;
			default:
				break;
			}
		}
		// Ma hang hoa
		Cell SLo = cells.get(14);
		if (SLo != null) {
			switch (SLo.getCellType()) {
			case STRING:
				eInvoiceExcelForm.setSLo(SLo.getStringCellValue());
				break;
			case NUMERIC:
				eInvoiceExcelForm.setSLo((NumberToTextConverter.toText(SLo.getNumericCellValue())));
				break;
			case BLANK:
				break;
			default:
				break;
			}
		}
		// Ma hang hoa
		Cell HanSD = cells.get(15);
		if (HanSD != null) {
			switch (HanSD.getCellType()) {
			case STRING:
				eInvoiceExcelForm.setHanSD(HanSD.getStringCellValue());
				break;
			case NUMERIC:
				eInvoiceExcelForm.setHanSD((NumberToTextConverter.toText(HanSD.getNumericCellValue())));
				break;
			case BLANK:
				break;
			default:
				break;
			}
		}
		// Don vi tinh
		Cell DonViTinh = cells.get(16);
		if (DonViTinh != null) {
			switch (DonViTinh.getCellType()) {
			case STRING:
				eInvoiceExcelForm.setDonViTinh(DonViTinh.getStringCellValue());
				break;
			case NUMERIC:
				eInvoiceExcelForm.setDonViTinh((NumberToTextConverter.toText(DonViTinh.getNumericCellValue())));
				break;
			case BLANK:
				break;
			default:
				break;
			}
		}
		// so luong
		Cell SoLuong = cells.get(17);
		if (SoLuong != null) {
			switch (SoLuong.getCellType()) {
			case STRING:
				eInvoiceExcelForm.setSoLuong((Double.valueOf((String) SoLuong.getStringCellValue())));
				break;
			case NUMERIC:
				eInvoiceExcelForm.setSoLuong(SoLuong.getNumericCellValue());
				break;
			case BLANK:
				break;
			default:
				break;
			}
		}
		// Don gia
		Cell DonGia = cells.get(18);
		if (DonGia != null) {
			switch (DonGia.getCellType()) {
			case STRING:
				eInvoiceExcelForm.setDonGia((Double.valueOf((String) DonGia.getStringCellValue())));
				break;
			case NUMERIC:
				eInvoiceExcelForm.setDonGia(DonGia.getNumericCellValue());
				break;
			case BLANK:
				break;
			default:
				break;
			}
		}
		// Thanh tien
		Cell ThanhTien = cells.get(19);
		if (ThanhTien != null && (ThanhTien.getCellType() == CellType.FORMULA)) {
			switch (ThanhTien.getCachedFormulaResultType()) {
			case STRING:
				eInvoiceExcelForm.setThanhTien((Double.valueOf((String) ThanhTien.getStringCellValue())));
				break;
			case NUMERIC:
				eInvoiceExcelForm.setThanhTien(ThanhTien.getNumericCellValue());
				break;
			case BLANK:
				break;
			default:
				break;
			}
		}
		if (ThanhTien != null) {
			switch (ThanhTien.getCellType()) {
			case STRING:
				eInvoiceExcelForm.setThanhTien((Double.valueOf((String) ThanhTien.getStringCellValue())));
				break;
			case NUMERIC:
				eInvoiceExcelForm.setThanhTien(ThanhTien.getNumericCellValue());
				break;
			case BLANK:
				break;
			default:
				break;
			}
		}
		// Thue suat
		Cell ThueSuat = cells.get(20);
		if (ThueSuat != null) {
			switch (ThueSuat.getCellType()) {
			case STRING:
				eInvoiceExcelForm.setThueSuat((Double.valueOf((String) ThueSuat.getStringCellValue())));
				break;
			case NUMERIC:
				eInvoiceExcelForm.setThueSuat(ThueSuat.getNumericCellValue());
				break;
			case BLANK:
				break;
			default:
				break;
			}
		}
		// Tien thue
		Cell TienThue = cells.get(21);
		if (TienThue != null && (TienThue.getCellType() == CellType.FORMULA)) {
			switch (TienThue.getCachedFormulaResultType()) {
			case STRING:
				eInvoiceExcelForm.setTienThue((Double.valueOf((String) TienThue.getStringCellValue())));
				break;
			case NUMERIC:
				eInvoiceExcelForm.setTienThue(TienThue.getNumericCellValue());
				break;
			case BLANK:
				break;
			default:
				break;
			}
		}
		if (TienThue != null) {
			switch (TienThue.getCellType()) {
			case STRING:
				eInvoiceExcelForm.setTienThue((Double.valueOf((String) TienThue.getStringCellValue())));
				break;
			case NUMERIC:
				eInvoiceExcelForm.setTienThue(TienThue.getNumericCellValue());
				break;
			case BLANK:
				break;
			default:
				break;
			}
		}
		// tong tien
		Cell TongTien = cells.get(22);
		if (TongTien != null && (TongTien.getCellType() == CellType.FORMULA)) {
			switch (TongTien.getCachedFormulaResultType()) {
			case STRING:
				eInvoiceExcelForm.setTongTien((Double.valueOf((String) TongTien.getStringCellValue())));
				break;
			case NUMERIC:
				eInvoiceExcelForm.setTongTien(TongTien.getNumericCellValue());
				break;
			case BLANK:
				break;
			default:
				break;
			}
		}
		if (TongTien != null) {
			switch (TongTien.getCellType()) {
			case STRING:
				eInvoiceExcelForm.setTongTien((Double.valueOf((String) TongTien.getStringCellValue())));
				break;
			case NUMERIC:
				eInvoiceExcelForm.setTongTien(TongTien.getNumericCellValue());
				break;
			case BLANK:
				break;
			default:
				break;
			}
		}
		// Thanh tien
		Cell TinhChat = cells.get(23);
		if (TinhChat != null) {
			switch (TinhChat.getCellType()) {
			case STRING:
				eInvoiceExcelForm.setTinhChat(TinhChat.getStringCellValue());
				break;
			case NUMERIC:
				eInvoiceExcelForm.setTinhChat((NumberToTextConverter.toText(TinhChat.getNumericCellValue())));
				break;
			case BLANK:
				break;
			default:
				break;
			}
		}
		// Tong tien truoc thue
		Cell TongTienTruocThue = cells.get(24);
		if (TongTienTruocThue != null && (TongTienTruocThue.getCellType() == CellType.FORMULA)) {
			switch (TongTienTruocThue.getCachedFormulaResultType()) {
			case STRING:
				eInvoiceExcelForm
						.setTongTienTruocThue((Double.valueOf((String) TongTienTruocThue.getStringCellValue())));
				break;
			case NUMERIC:
				eInvoiceExcelForm.setTongTienTruocThue(TongTienTruocThue.getNumericCellValue());
				break;
			case BLANK:
				break;
			default:
				break;
			}
		}
		if (TongTienTruocThue != null) {
			switch (TongTienTruocThue.getCellType()) {
			case STRING:
				eInvoiceExcelForm
						.setTongTienTruocThue((Double.valueOf((String) TongTienTruocThue.getStringCellValue())));
				break;
			case NUMERIC:
				eInvoiceExcelForm.setTongTienTruocThue(TongTienTruocThue.getNumericCellValue());
				break;
			case BLANK:
				break;
			default:
				break;
			}
		}
		// ty gia
		Cell TyGia = cells.get(25);
		if (TyGia != null) {
			switch (TyGia.getCellType()) {
			case STRING:
				eInvoiceExcelForm.setTyGia(TyGia.getStringCellValue());
				break;
			case NUMERIC:
				eInvoiceExcelForm.setTyGia((NumberToTextConverter.toText(TyGia.getNumericCellValue())));
				break;
			case BLANK:
				break;
			default:
				break;
			}
		}
		// Tổng tiền thuế GTGT
		Cell TTTGTGT = cells.get(26);
		if (TTTGTGT != null && (TTTGTGT.getCellType() == CellType.FORMULA)) {
			switch (TTTGTGT.getCachedFormulaResultType()) {
			case STRING:
				eInvoiceExcelForm.setTTTGTGT((Double.valueOf((String) TTTGTGT.getStringCellValue())));
				break;
			case NUMERIC:
				eInvoiceExcelForm.setTTTGTGT(TTTGTGT.getNumericCellValue());
				break;
			case BLANK:
				break;
			default:
				break;
			}
		}
		if (TTTGTGT != null) {
			switch (TTTGTGT.getCellType()) {
			case STRING:
				eInvoiceExcelForm.setTTTGTGT((Double.valueOf((String) TTTGTGT.getStringCellValue())));
				break;
			case NUMERIC:
				eInvoiceExcelForm.setTTTGTGT(TTTGTGT.getNumericCellValue());
				break;
			case BLANK:
				break;
			default:
				break;
			}
		}
		// Tổng tiền thuế đã có thuế GTGT
		Cell TTDCTGTGT = cells.get(27);
		if (TTDCTGTGT != null && (TTDCTGTGT.getCellType() == CellType.FORMULA)) {
			switch (TTDCTGTGT.getCachedFormulaResultType()) {
			case STRING:
				eInvoiceExcelForm.setTTDCTGTGT((Double.valueOf((String) TTDCTGTGT.getStringCellValue())));
				break;
			case NUMERIC:
				eInvoiceExcelForm.setTTDCTGTGT(TTDCTGTGT.getNumericCellValue());
				break;
			case BLANK:
				break;
			default:
				break;
			}
		}
		if (TTDCTGTGT != null) {
			switch (TTDCTGTGT.getCellType()) {
			case STRING:
				eInvoiceExcelForm.setTTDCTGTGT((Double.valueOf((String) TTDCTGTGT.getStringCellValue())));
				break;
			case NUMERIC:
				eInvoiceExcelForm.setTTDCTGTGT(TTDCTGTGT.getNumericCellValue());
				break;
			case BLANK:
				break;
			default:
				break;
			}
		}
		// Tổng tiền ghi bằng chữ
		Cell TTBangChu = cells.get(28);
		if (TTBangChu != null) {
			switch (TTBangChu.getCellType()) {
			case STRING:
				eInvoiceExcelForm.setTTBangChu(TTBangChu.getStringCellValue());
				break;
			case NUMERIC:
				eInvoiceExcelForm.setTTBangChu((NumberToTextConverter.toText(TTBangChu.getNumericCellValue())));
				break;
			case BLANK:
				break;
			default:
				break;
			}
		}

		// Thanh tien
		Cell MaKH = cells.get(29);
		if (MaKH != null) {
			switch (MaKH.getCellType()) {
			case STRING:
				eInvoiceExcelForm.setMaKH(MaKH.getStringCellValue());
				break;
			case NUMERIC:
				eInvoiceExcelForm.setMaKH((NumberToTextConverter.toText(MaKH.getNumericCellValue())));
				break;
			case BLANK:
				break;
			default:
				break;
			}
		}
		// Tra ve danh sach
		return eInvoiceExcelForm;
	}

	/*----------------------------------------------------- Start Import excel */
	@SuppressWarnings({ "unlikely-arg-type", "unused" })
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
		//
		objectId = null;
		objectIdUser = null;
		objectIdMSKH = null;
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

		Document findInforIssuer = new Document("_id", 1)
				.append("TaxCode", 1)
				.append("Name", 1)
				.append("Address", 1)
				.append("Phone", 1)
				.append("Fax", 1)
				.append("Email", 1)
				.append("Website", 1)
				.append("TinhThanhInfo", 1)
				.append("ChiCucThueInfo", 1)
				.append("BankAccount", 1)
				.append("NameEN", 1)
				.append("BankAccountExt", 1);
		
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
		pipeline.add(
				new Document("$lookup",
						new Document("from", "PramLink")
								.append("pipeline",
										Arrays.asList(new Document("$match",
												new Document("$expr", new Document("IsDelete", false))),
												new Document("$project", new Document("_id", 1).append("LinkPortal", 1))
												))
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
		List<EInvoiceExcelForm> eInvoiceExcelFormList = new ArrayList<>();
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
				int lastColumn = Math.max(row1.getLastCellNum(), 26);

				for (int cn = 0; cn < lastColumn; cn++) {
					Cell c = row1.getCell(cn, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
					cells.add(c);
				}
				EInvoiceExcelForm eInvoiceExcelForm = extractInfoFromCellAuto(cells);
				eInvoiceExcelFormList.add(eInvoiceExcelForm);
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
		if (eInvoiceExcelFormList != null) {
			for (int tam = 0; tam < eInvoiceExcelFormList.size(); tam++) {
				if (eInvoiceExcelFormList.get(tam).getMaHD() == null) {
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
			String tempTen = "";
			String tempMST = "";
			String tempMDVQHNS = "";
			String tempDChi = "";
			String tempSDThoai = "";
			String tempSCCCDan = "";
			String tempSHChieu = "";
			String tempDCTDTu = "";
			String tempMAKH = "";
			String tempSTKNHang = "";
			String tempTNHang = "";
			String tempHVTNMHang = "";
//			Double tempTgTCThue = 0.0;
//			Double tempTgTThue = 0.0;
//			Double tempTgTTTBSo = 0.0;
//			String tempTgTTTBChu = "";
			String tempTyGia = "";
			String tempHTTToan = "";
			List<DSHHDVu> dshhdVuList = new ArrayList<>();
			List<Object> listHHDVu = new ArrayList<Object>();
			int i = 0;
			int start = 0;
			int end = 0;
			int dem = 0;

			/* DOC FILE EXCEL - GHI DU LIEU VO LIST */
			for (; i < eInvoiceExcelFormList.size();) {
				
				dem = 0;
				for (int j = i; j < eInvoiceExcelFormList.size(); j++) {
					if (eInvoiceExcelFormList.get(i).getMaHD().equals(eInvoiceExcelFormList.get(j).getMaHD())) {
						// Xu ly
						dem++;
						start = j + 1;

						tempTen = eInvoiceExcelFormList.get(i).getTenDonVi();
						tempMST = eInvoiceExcelFormList.get(i).getMaSoThue();
						tempMDVQHNS = eInvoiceExcelFormList.get(i).getMaDVQHNS();
						tempDChi = eInvoiceExcelFormList.get(i).getDiaChiKhachHang();
						tempSDThoai = eInvoiceExcelFormList.get(i).getSDTKhachHang();
						tempSCCCDan = eInvoiceExcelFormList.get(i).getSoCCCD();
						tempSHChieu = eInvoiceExcelFormList.get(i).getSoHoChieu();
						tempDCTDTu = eInvoiceExcelFormList.get(i).getMailKhachHang();
						tempSTKNHang = eInvoiceExcelFormList.get(i).getSoTaiKhoan();
						tempTNHang = eInvoiceExcelFormList.get(i).getTenNganHang();
						tempHVTNMHang = eInvoiceExcelFormList.get(i).getTenNguoiMua();
						tempHTTToan = eInvoiceExcelFormList.get(i).getHinhThucThanhToan();
						tempTyGia = eInvoiceExcelFormList.get(i).getTyGia();
						end = j;
						if (eInvoiceExcelFormList.size() == j + 1) {
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
				String TenForm = tempTen;
				String MaSoThueForm = tempMST;
				String MaDVQHNSForm = tempMDVQHNS;
				String DChiNMForm = tempDChi;
				String SDThoaiNMForm = tempSDThoai;
				String SCCCDanNMForm = tempSCCCDan;
				String SHChieuNMForm = tempSHChieu;
				String DCTDTuNMForm = tempDCTDTu;
				String MaKHNMForm = tempMAKH;
				String STKNHangNMForm = tempSTKNHang;
				String TNHangNMForm = tempTNHang;
				String HVTNMHangNMForm = tempHVTNMHang;
//				Double tempTgTCThueForm = tempTgTCThue;
//				Double tempTgTThueForm = tempTgTThue;
//				Double tempTgTTTBSoForm = tempTgTTTBSo;
//				String tempTgTTTBChuForm = tempTgTTTBChu;
				String TyGiaForm = tempTyGia;
				String HTTToanForm = tempHTTToan;

				if (checkMaHD == true) {
					if (dem > 1) {

						int a = 1;
						int stt = 0;
						int TT = 0;
						Double tong = 0.0;
						Double tthue = 0.0;
						Double Tongtien = 0.0;
						Double TongTienTThue = 0.0;
						Double TongTienThue = 0.0;
						Double TTDCThue = 0.0;
						Double Total = 0.0;
						Double TongTienChietKhauTM = 0.0;
						for (int k = i; k <= end; k++) {
							DSHHDVu dshhdVu = new DSHHDVu();
							TT = stt + a;
							String STT = String.valueOf(TT);
							tong = eInvoiceExcelFormList.get(k).getThanhTien();
							tthue = eInvoiceExcelFormList.get(k).getTienThue();
							Total = tong + tthue;
							Tongtien = Tongtien + Total;
							TongTienTThue = TongTienTThue + tong;
							TongTienThue = TongTienThue + tthue;
							dshhdVu.setSTT(STT);
							dshhdVu.setProductName(eInvoiceExcelFormList.get(k).getTenHangHoa());
							dshhdVu.setProductCode(eInvoiceExcelFormList.get(k).getMaHangHoa());
							dshhdVu.setSLo(eInvoiceExcelFormList.get(k).getSLo());
							dshhdVu.setHanSD(eInvoiceExcelFormList.get(k).getHanSD());
							dshhdVu.setUnit(eInvoiceExcelFormList.get(k).getDonViTinh());
							dshhdVu.setQuantity(eInvoiceExcelFormList.get(k).getSoLuong());
							dshhdVu.setPrice(eInvoiceExcelFormList.get(k).getDonGia());
							dshhdVu.setTotal(eInvoiceExcelFormList.get(k).getThanhTien());
							dshhdVu.setVATRate(eInvoiceExcelFormList.get(k).getThueSuat());
							dshhdVu.setVATAmount(eInvoiceExcelFormList.get(k).getTienThue());
							dshhdVu.setAmount(Tongtien);
							String TinhChat = eInvoiceExcelFormList.get(k).getTinhChat();
							switch (TinhChat) {
							case "1":
								dshhdVu.setFeature("1");
								break;
							case "2":
								dshhdVu.setFeature("2");
								break;
							case "3":
								dshhdVu.setFeature("3");
								break;
							case "4":
								dshhdVu.setFeature("4");
								break;
							default:
								break;
							}
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
							stt++;
							Tongtien = 0.0;

						}
						// Clear BIEN GAN STT
						stt = stt - (end + 1);

						String TTTien = "";
						Double TTT = 0.0;
						String TTTTien = "";
						Double TTTT = 0.0;
						TTTTien = String.format("%.0f", TongTienTThue);
						TTTT = Double.parseDouble(TTTTien);
						TTTien = String.format("%.0f", TongTienThue);
						TTT = Double.parseDouble(TTTien);

						TTDCThue = TTTT + TTT;
						String TTBChu = commons.formatNumberReal(TTDCThue).replaceAll(",", "");
						String TTBCHU = ChuyenSangChu(TTBChu);
						String TongTienBangchu = TTBCHU.substring(0, 1).toUpperCase() + TTBCHU.substring(1) + ".";
//	            	        System.out.println(currencyFormat(tien));

						// Thông tin hóa đơn - TTChung
						String MaHD = eInvoiceExcelFormList.get(i).getMaHD();
						String THDon = "Hóa đơn giá trị gia tăng TT 78";
//	                        String MauSoHD = docTmp.getEmbedded(Arrays.asList("DMMauSoKyHieu", "_id"), ObjectId.class).toString();
						String MauSoHD = mauSoHdon;
						String KHMSHDon = docTmp.getEmbedded(Arrays.asList("DMMauSoKyHieu", "KHMSHDon"), "").toString();
						String KHHDon = docTmp.getEmbedded(Arrays.asList("DMMauSoKyHieu", "KHHDon"), "").toString();
						LocalDateTime NLap = LocalDateTime.now();
						String DVTTe = eInvoiceExcelFormList.get(i).getLoaiTien();
						String TGia = TyGiaForm;
						String HTTToanCode = "";
						String HTTToan1 = "";
						switch (HTTToanForm) {
						case "1":
							HTTToanCode = "1";
							HTTToan1 = "Tiền mặt";
							break;
						case "2":
							HTTToanCode = "2";
							HTTToan1 = "Chuyển khoản";
							break;
						case "3":
							HTTToanCode = "3";
							HTTToan1 = "Tiền mặt/Chuyển khoản";
							break;
						case "4":
							HTTToanCode = "4";
							HTTToan1 = "Đối trừ công nợ";
							break;
						case "5":
							HTTToanCode = "5";
							HTTToan1 = "Không thu tiền";
							break;
						default:
							break;
						}
						String HTTToan = HTTToan1;
//						TTChung ttChung = new TTChung(MaHD, THDon, MauSoHD, KHMSHDon, KHHDon, NLap, DVTTe, TGia,
//								HTTToanCode, HTTToan);

						// Thông tin người bán - Thông tin người mua - NDHDon
						String Ten = docTmp.getEmbedded(Arrays.asList("Name"), "").toString();
						String MST = docTmp.getEmbedded(Arrays.asList("TaxCode"), "").toString();
						String DChi = docTmp.getEmbedded(Arrays.asList("Address"), "").toString();
//						String MKHang = "";
						String SDThoai = docTmp.getEmbedded(Arrays.asList("Phone"), "").toString();
						String DCTDTu = "";
//						String HVTNMHang = "";
						String STKNHang = docTmp.getEmbedded(Arrays.asList("BankAccount", "AccountNumber"), "")
								.toString();
						String TNHang = docTmp.getEmbedded(Arrays.asList("BankAccount", "BankName"), "").toString();
						String Fax = docTmp.getEmbedded(Arrays.asList("Fax"), "").toString();
						String Website = docTmp.getEmbedded(Arrays.asList("Website"), "").toString();

						String TenNM = TenForm;
						String MSTNM = MaSoThueForm;
						String MDVQHNSNM = MaDVQHNSForm;
						String DChiNM = DChiNMForm;
						String SDThoaiNM = SDThoaiNMForm;
						String SCCCDanNM = SCCCDanNMForm;
						String SHChieuNM = SHChieuNMForm;
						String DCTDTuNM = DCTDTuNMForm;
						String MaKHangNM = MaKHNMForm;
						String STKNHangNM = STKNHangNMForm;
						String TNHangNM = TNHangNMForm;
						String HVTNMHangNM = HVTNMHangNMForm;

//						NBan nBan = new NBan(Ten, MST, DChi, SDThoai, DCTDTu, STKNHang, TNHang, Fax, Website);
//						NMua nMua = new NMua(TenNM, MSTNM, DChiNM, SDThoaiNM, DCTDTuNM, MaKHangNM, STKNHangNM, TNHangNM,
//								HVTNMHangNM);
//						NDHDon ndhDon = new NDHDon(nBan, nMua);

						// Thông tin thanh toán
//						Double TgTCThue = tempTgTCThueForm;
//						Double TgTThue = tempTgTThueForm;
//						Double TgTTTBSo = tempTgTTTBSoForm;
//						String TgTTTBChu = tempTgTTTBChuForm;
//						TToan tToan = new TToan(TgTCThue, TgTThue, TgTTTBSo, TgTTTBChu);

						// Một số thông tin khác
						String SignStatusCode = "NOSIGN";
						String EInvoiceStatus = "CREATED";
						String MTDiep = "";
						String SecureKey = "";
						String Dir = "";

//	                        
//	                        //
//	                    	objectIdEInvoice = new ObjectId();
//	            			path = Paths.get(SystemParams.DIR_E_INVOICE_DATA, taxCode, docTmp.getEmbedded(Arrays.asList("DMMauSoKyHieu", "_id"), ObjectId.class).toString());
//	            			pathDir = path.toString();
//	            			file = path.toFile();
//	            			if(!file.exists()) file.mkdirs();
//	            			/*TAO XML HOA DON*/
//	            			fileNameXML = objectIdEInvoice.toString() + ".xml";
//	                        //

						// Setting
						String codeMTD = "0315382923";
						String FileNameXML = "";
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

						Dir = pathDir;
						MTDiep = codeMTD + commons.csRandomAlphaNumbericString(46 - codeMTD.length()).toUpperCase();
						SecureKey = commons.csRandomNumbericString(6);
						// XML
						FileNameXML = fileNameXML;
						dbf = DocumentBuilderFactory.newInstance();
						db = dbf.newDocumentBuilder();
						doc = db.newDocument();
						doc.setXmlStandalone(true);

						root = doc.createElement("HDon");
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
																											// KHI KY
						elementSubContent.appendChild(commons.createElementWithValue(doc, "MHSo", ""));
						// Ngày lập
						elementSubContent.appendChild(
								commons.createElementWithValue(doc, "NLap", NLap.format(DateTimeFormatter.ISO_DATE)));
						// Số bảng kê
						elementSubContent.appendChild(commons.createElementWithValue(doc, "SBKe", ""));
						// Ngày bảng kê
						elementSubContent.appendChild(commons.createElementWithValue(doc, "NBKe", ""));
						// Đơn vị tiền tệ
						elementSubContent.appendChild(commons.createElementWithValue(doc, "DVTTe", DVTTe));
						// Tỷ giá
						elementSubContent.appendChild(commons.createElementWithValue(doc, "TGia", TGia));
						// Hình thức thanh toán
						elementSubContent.appendChild(commons.createElementWithValue(doc, "HTTToan", HTTToan));
						// MST tổ chức cung cấp giải pháp HĐĐT
						elementSubContent
								.appendChild(commons.createElementWithValue(doc, "MSTTCGP", SystemParams.MSTTCGP));
						// MST đơn vị nhận ủy nhiệm lập hóa đơn
						elementSubContent.appendChild(commons.createElementWithValue(doc, "MSTDVNUNLHDon", ""));
						// Tên đơn vị nhận ủy nhiệm lập hóa đơn
						elementSubContent.appendChild(commons.createElementWithValue(doc, "TDVNUNLHDon", ""));
						// Địa chỉ đơn vị nhận ủy nhiệm lập hóa đơn
						elementSubContent.appendChild(commons.createElementWithValue(doc, "DCDVNUNLHDon", ""));
						elementTmp = doc.createElement("TTKhac"); // THONG TIN KHAC
						elementTmp.appendChild(commons.createElementTTKhac(doc, "PortalLink", "string", link));
						elementTmp.appendChild(commons.createElementTTKhac(doc, "SecureKey", "string", SecureKey));
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
//	            			elementTmp.appendChild(commons.createElementWithValue(doc, "TTKhac", ""));

						/* ADD THONG TIN TK NGAN HANG (NEU CO) */
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
						elementTmp.appendChild(commons.createElementWithValue(doc, "Ten", TenNM));
						elementTmp.appendChild(commons.createElementWithValue(doc, "MST", MSTNM));
						elementTmp.appendChild(commons.createElementWithValue(doc, "MDVQHNSach", MDVQHNSNM));
						elementTmp.appendChild(commons.createElementWithValue(doc, "DChi", DChiNM));
						elementTmp.appendChild(commons.createElementWithValue(doc, "MKHang", MaKHangNM));
						elementTmp.appendChild(commons.createElementWithValue(doc, "SDThoai", SDThoaiNM));
						elementTmp.appendChild(commons.createElementWithValue(doc, "CCCDan", SCCCDanNMForm));
						elementTmp.appendChild(commons.createElementWithValue(doc, "SHChieu", SHChieuNMForm));
						elementTmp.appendChild(commons.createElementWithValue(doc, "DCTDTu", DCTDTuNM));
						elementTmp.appendChild(commons.createElementWithValue(doc, "HVTNMHang", HVTNMHangNM));
						elementTmp.appendChild(commons.createElementWithValue(doc, "STKNHang", STKNHangNM));
						elementTmp.appendChild(commons.createElementWithValue(doc, "TNHang", TNHangNM));
						elementTmp.appendChild(commons.createElementWithValue(doc, "TTKhac", ""));
						elementSubContent.appendChild(elementTmp);

						mapVATAmount = new LinkedHashMap<String, Double>();
						mapAmount = new LinkedHashMap<String, Double>();
						elementTmp = doc.createElement("DSHHDVu"); // HH-DV
						// dshhdVuList, listHHDVu, \VATRate,
						// Json.serializer().nodeFromObject(msg.getObjData());

						for (Object o : listHHDVu) {
							JsonNode h = Json.serializer().nodeFromObject(o);
							if (!"".equals(commons.getTextJsonNode(h.at("/ProductName")).trim())) {
								tmp = commons.formatNumberReal(commons.getTextJsonNode(h.at("/VATRate")))
										.replaceAll(",", "");
								switch (tmp) {
								case "0":
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
								Boolean slo1 = false;
								Boolean hsd1 = false;
								String slo = commons.getTextJsonNode(h.at("/SLo"));
								String hsd = commons.getTextJsonNode(h.at("/HanSD"));
								if ("".equals(slo)) {
									slo1 = true;
								}
								if ("".equals(hsd)) {
									hsd1 = true;
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
								if (slo1 == false) {
									elementSubTmp.appendChild(commons.createElementWithValue(doc, "SLo",
											commons.getTextJsonNode(h.at("/SLo"))));
								}
								if (hsd1 == false) {
									elementSubTmp.appendChild(commons.createElementWithValue(doc, "HanSD",
											commons.getTextJsonNode(h.at("/HanSD"))));
								}

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
									elementSubTmp.appendChild(commons.createElementWithValue(doc, "TSuat", tmp));
								}

								elementSubTmp01 = doc.createElement("TTKhac");
								elementSubTmp01.appendChild(commons.createElementTTKhac(doc, "VATAmount", "decimal",
										commons.formatNumberReal(commons.getTextJsonNode(h.at("/VATAmount")))
												.replaceAll(",", "")));
								elementSubTmp01.appendChild(commons.createElementTTKhac(doc, "Amount", "decimal",
										commons.formatNumberReal(commons.getTextJsonNode(h.at("/Amount")))
												.replaceAll(",", "")));
								elementSubTmp.appendChild(elementSubTmp01);
								elementTmp.appendChild(elementSubTmp);

							}
							if ("3".equals(commons.getTextJsonNode(h.at("/Feature")))) {
								TongTienChietKhauTM += commons.ToNumber(commons.getTextJsonNode(h.at("/Total")));
							}
							
						}
						elementSubContent.appendChild(elementTmp);

						elementTmp = doc.createElement("TToan"); // Thong tin thanh toan
						elementSubTmp = doc.createElement("THTTLTSuat");
						/* DANH SACH CAC LOAI THUE SUAT */

						// https: //
						// stackoverflow.com/questions/46898/how-do-i-efficiently-iterate-over-each-entry-in-a-java-map
						for (Map.Entry<String, Double> pair : mapVATAmount.entrySet()) {
							if (null != pair.getKey() && !"".equals(pair.getKey())) {
								elementSubTmp01 = doc.createElement("LTSuat");
								elementSubTmp01
										.appendChild(commons.createElementWithValue(doc, "TSuat", pair.getKey()));
								elementSubTmp01.appendChild(commons.createElementWithValue(doc, "ThTien",
										commons.formatNumberReal(mapAmount.get(pair.getKey())).replaceAll(",", "")));
								elementSubTmp01.appendChild(commons.createElementWithValue(doc, "TThue",
										String.format("%.0f", mapVATAmount.get(pair.getKey()))));
								elementSubTmp.appendChild(elementSubTmp01);
							}
						}
						elementTmp.appendChild(elementSubTmp);
////	            			https://stackoverflow.com/questions/46898/how-do-i-efficiently-iterate-over-each-entry-in-a-java-map
//	            			elementSubTmp01 = doc.createElement("LTSuat");
//	    					elementSubTmp01.appendChild(commons.createElementWithValue(doc, "TSuat", commons.formatNumberReal(tmp).replaceAll(",", "") + "%"));
//	    					elementSubTmp01.appendChild(commons.createElementWithValue(doc, "ThTien", commons.formatNumberReal(TgTTTBSo).replaceAll(",", "")));
//	    					elementSubTmp01.appendChild(commons.createElementWithValue(doc, "TThue", commons.formatNumberReal(TgTThue).replaceAll(",", "")));
//	    					elementSubTmp.appendChild(elementSubTmp01);	
//	            			elementTmp.appendChild(elementSubTmp);

						elementTmp.appendChild(commons.createElementWithValue(doc, "TgTCThue",
								commons.formatNumberReal(TTTT).replaceAll(",", "")));
						elementTmp.appendChild(commons.createElementWithValue(doc, "TgTThue",
								commons.formatNumberReal(TTT).replaceAll(",", "")));

						elementSubTmp = doc.createElement("DSLPhi");
						elementSubTmp01 = doc.createElement("LPhi");
						elementSubTmp01.appendChild(commons.createElementWithValue(doc, "TLPhi", ""));
						elementSubTmp01.appendChild(commons.createElementWithValue(doc, "TPhi", "0"));
						elementSubTmp.appendChild(elementSubTmp01);

						elementTmp.appendChild(elementSubTmp);

						elementTmp.appendChild(commons.createElementWithValue(doc, "TTCKTMai", String.valueOf(TongTienChietKhauTM)));
						elementTmp.appendChild(commons.createElementWithValue(doc, "TgTTTBSo",
								commons.formatNumberReal(TTDCThue).replaceAll(",", "")));
						elementTmp.appendChild(commons.createElementWithValue(doc, "TgTTTBChu", TongTienBangchu));
						elementSubTmp = doc.createElement("TTKhac");
						elementTmp.appendChild(elementSubTmp);

						elementSubContent.appendChild(elementTmp);

						elementContent.appendChild(elementSubContent);
						// END - NDHDon: Nội dung hóa đơn

						isSdaveFile = commons.docW3cToFile(doc, Dir, FileNameXML);
						if (!isSdaveFile) {
							throw new Exception("Lưu dữ liệu không thành công.");
						}
						/* END - TAO XML HOA DON */
						// END XML"_id", objectIdEInvoice
						// lookup data
						docUpsert = new Document("_id", objectIdEInvoice).append("IssuerId", header.getIssuerId())
								.append("MTDiep", MTDiep)
								.append("EInvoiceDetail", new Document("TTChung",
										new Document("THDon", THDon).append("MaHD", MaHD).append("MauSoHD", MauSoHD)
												.append("KHMSHDon", KHMSHDon).append("KHHDon", KHHDon)
												.append("NLap", NLap).append("DVTTe", DVTTe).append("TGia", TGia)
												.append("HTTToanCode", HTTToanCode).append("HTTToan", HTTToan))
										.append("NDHDon",
												new Document("NBan",
														new Document("Ten", Ten).append("MST", MST).append("DChi", DChi)
																.append("SDThoai", SDThoai).append("DCTDTu", DCTDTu)
																.append("STKNHang", STKNHang).append("TNHang", TNHang)
																.append("Fax", Fax).append("Website", Website))
														.append("NMua", new Document("Ten", TenNM).append("MST", MSTNM)
																.append("MDVQHNSach", MDVQHNSNM)
																.append("DChi", DChiNM).append("MKHang", MaKHangNM)
																.append("SDThoai", SDThoaiNM)
																.append("CCCDan", SCCCDanNM)
																.append("SHChieu", SHChieuNM)
																.append("DCTDTu", DCTDTuNM)
																.append("HVTNMHang", HVTNMHangNM)
																.append("STKNHang", STKNHangNM)
																.append("TNHang", TNHangNM)))

										.append("DSHHDVu", listHHDVu)
										.append("TToan", new Document("TgTCThue", TTTT).append("TgTThue", TTT)
												.append("TgTTTBSo", TTDCThue).append("TgTTTBChu", TongTienBangchu)))
								.append("SignStatusCode", SignStatusCode).append("EInvoiceStatus", EInvoiceStatus)
								.append("IsDelete", false).append("SecureKey", SecureKey).append("Dir", Dir)
								.append("FileNameXML", FileNameXML).append("InfoCreated",
										new Document("CreateDate", LocalDateTime.now())
												.append("CreateUserID", header.getUserId())
												.append("CreateUserName", header.getUserName())
												.append("CreateUserFullName", header.getUserFullName()));
					
						 mongoClient = cfg.mongoClient();
						 collection = mongoClient.getDatabase(cfg.dbName).getCollection("EInvoice");
						collection.insertOne(docUpsert);
						mongoClient.close();
						
						
						dshhdVuList.clear();
						listHHDVu.clear();
						checkMaHD = false;
					} else {
						int a = 1;
						int stt = 0;
						int TT = 0;
						Double tong = 0.0;
						Double tthue = 0.0;
						Double Tongtien = 0.0;
						Double TongTienTThue = 0.0;
						Double TongTienThue = 0.0;
						Double TTDCThue = 0.0;
						Double Total = 0.0;
						Double TongTienChietKhauTM = 0.0;
						if (dem == 1) {
							int k = i;
							DSHHDVu dshhdVu = new DSHHDVu();
							TT = stt + a;
							String STT = String.valueOf(TT);
							tong = eInvoiceExcelFormList.get(k).getThanhTien();
							tthue = eInvoiceExcelFormList.get(k).getTienThue();
							Total = tong + tthue;
							Tongtien = Tongtien + Total;
							TongTienTThue = TongTienTThue + tong;
							TongTienThue = TongTienThue + tthue;
							dshhdVu.setSTT(STT);
							dshhdVu.setProductName(eInvoiceExcelFormList.get(k).getTenHangHoa());
							dshhdVu.setProductCode(eInvoiceExcelFormList.get(k).getMaHangHoa());
							dshhdVu.setSLo(eInvoiceExcelFormList.get(k).getSLo());
							dshhdVu.setHanSD(eInvoiceExcelFormList.get(k).getHanSD());
							dshhdVu.setUnit(eInvoiceExcelFormList.get(k).getDonViTinh());
							dshhdVu.setQuantity(eInvoiceExcelFormList.get(k).getSoLuong());
							dshhdVu.setPrice(eInvoiceExcelFormList.get(k).getDonGia());
							dshhdVu.setTotal(eInvoiceExcelFormList.get(k).getThanhTien());
							dshhdVu.setVATRate(eInvoiceExcelFormList.get(k).getThueSuat());
							dshhdVu.setVATAmount(eInvoiceExcelFormList.get(k).getTienThue());
							dshhdVu.setAmount(Tongtien);
							String TinhChat = eInvoiceExcelFormList.get(k).getTinhChat();
							switch (TinhChat) {
							case "1":
								dshhdVu.setFeature("1");
								break;
							case "2":
								dshhdVu.setFeature("2");
								break;
							case "3":
								dshhdVu.setFeature("3");
								break;
							case "4":
								dshhdVu.setFeature("4");
								break;
							default:
								break;
							}
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

							String TTTien = "";
							Double TTT = 0.0;
							String TTTTien = "";
							Double TTTT = 0.0;
							TTTTien = String.format("%.0f", TongTienTThue);
							TTTT = Double.parseDouble(TTTTien);
							TTTien = String.format("%.0f", TongTienThue);
							TTT = Double.parseDouble(TTTien);

							TTDCThue = TTTT + TTT;
							String TTBChu = commons.formatNumberReal(TTDCThue).replaceAll(",", "");
							String TTBCHU = ChuyenSangChu(TTBChu);
							String TongTienBangchu = TTBCHU.substring(0, 1).toUpperCase() + TTBCHU.substring(1) + ".";
							// Thông tin hóa đơn - TTChung
							String MaHD = eInvoiceExcelFormList.get(i).getMaHD();
							String THDon = "Hóa đơn giá trị gia tăng TT 78";
//	                            String MauSoHD = "62610fc6dd79cb7e4571890f";
//	                            String MauSoHD = docTmp.getEmbedded(Arrays.asList("DMMauSoKyHieu", "_id"), ObjectId.class).toString();
							String MauSoHD = mauSoHdon;
							String KHMSHDon = docTmp.getEmbedded(Arrays.asList("DMMauSoKyHieu", "KHMSHDon"), "")
									.toString();
							String KHHDon = docTmp.getEmbedded(Arrays.asList("DMMauSoKyHieu", "KHHDon"), "").toString();
							LocalDateTime NLap = LocalDateTime.now();
							String DVTTe = eInvoiceExcelFormList.get(i).getLoaiTien();
							String TGia = TyGiaForm;
							String HTTToanCode = "";
							String HTTToan1 = "";
							switch (HTTToanForm) {
							case "1":
								HTTToanCode = "1";
								HTTToan1 = "Tiền mặt";
								break;
							case "2":
								HTTToanCode = "2";
								HTTToan1 = "Chuyển khoản";
								break;
							case "3":
								HTTToanCode = "3";
								HTTToan1 = "Tiền mặt/Chuyển khoản";
								break;
							case "4":
								HTTToanCode = "4";
								HTTToan1 = "Đối trừ công nợ";
								break;
							case "5":
								HTTToanCode = "5";
								HTTToan1 = "Không thu tiền";
								break;
							default:
								break;
							}
							String HTTToan = HTTToan1;
//	                            TTChung ttChung = new TTChung(MaHD,MaKH,THDon, MauSoHD, KHMSHDon, KHHDon, NLap, DVTTe, TGia, HTTToanCode, HTTToan);

							// Thông tin người bán - Thông tin người mua - NDHDon
							String Ten = docTmp.getEmbedded(Arrays.asList("Name"), "").toString();
							String MST = docTmp.getEmbedded(Arrays.asList("TaxCode"), "").toString();
							String DChi = docTmp.getEmbedded(Arrays.asList("Address"), "").toString();
							String MKHang = "";
							String SDThoai = docTmp.getEmbedded(Arrays.asList("Phone"), "").toString();
							String DCTDTu = "";
							String HVTNMHang = "";
							String STKNHang = docTmp.getEmbedded(Arrays.asList("BankAccount", "AccountNumber"), "")
									.toString();
							String TNHang = docTmp.getEmbedded(Arrays.asList("BankAccount", "BankName"), "").toString();
							String Fax = docTmp.getEmbedded(Arrays.asList("Fax"), "").toString();
							String Website = docTmp.getEmbedded(Arrays.asList("Website"), "").toString();

							String TenNM = TenForm;
							String MSTNM = MaSoThueForm;
							String MDVQHNSNM = MaDVQHNSForm;
							String DChiNM = DChiNMForm;
							String SDThoaiNM = SDThoaiNMForm;
							String SCCCDanNM = SCCCDanNMForm;
							String SHChieuNM = SHChieuNMForm;
							String DCTDTuNM = DCTDTuNMForm;
							String MaKHangNM = MaKHNMForm;
							String STKNHangNM = STKNHangNMForm;
							String TNHangNM = TNHangNMForm;
							String HVTNMHangNM = HVTNMHangNMForm;

//							NBan nBan = new NBan(Ten, MST, DChi, SDThoai, DCTDTu, STKNHang, TNHang, Fax, Website);
//	                            NMua nMua = new NMua(TenNM, MSTNM, DChiNM, SDThoaiNM, DCTDTuNM,MaKHang, STKNHangNM, TNHangNM, HVTNMHangNM);

//	                            NDHDon ndhDon = new NDHDon(nBan, nMua);

							// Thông tin thanh toán
//							Double TgTCThue = tempTgTCThueForm;
//							Double TgTThue = tempTgTThueForm;
//							Double TgTTTBSo = tempTgTTTBSoForm;
//							String TgTTTBChu = tempTgTTTBChuForm;
//							TToan tToan = new TToan(TgTCThue, TgTThue, TgTTTBSo, TgTTTBChu);

							// Một số thông tin khác
							String SignStatusCode = "NOSIGN";
							String EInvoiceStatus = "CREATED";
							String MTDiep = "";
							String SecureKey = "";
							String Dir = "";

							// Setting
							String codeMTD = "0315382923";
							String FileNameXML = "";
							String pathDir = "";
							File file1 = null;
							Path path1 = null;
							ObjectId objectIdEInvoice = null;
							String fileNameXML = "";
							objectIdEInvoice = new ObjectId();
							String taxCode = "";
							taxCode = docTmp.getString("TaxCode");
							path1 = Paths.get(SystemParams.DIR_E_INVOICE_DATA, taxCode, docTmp
									.getEmbedded(Arrays.asList("DMMauSoKyHieu", "_id"), ObjectId.class).toString());
							pathDir = path1.toString();
							file1 = path1.toFile();
							if (!file1.exists())
								file1.mkdirs();
							fileNameXML = objectIdEInvoice.toString() + ".xml";
							pathDir = path1.toString();
							file1 = path1.toFile();
							if (!file1.exists())
								file1.mkdirs();
							Dir = pathDir;
							MTDiep = codeMTD + commons.csRandomAlphaNumbericString(46 - codeMTD.length()).toUpperCase();
							SecureKey = commons.csRandomNumbericString(6);
							// XML
							FileNameXML = fileNameXML;
							dbf = DocumentBuilderFactory.newInstance();
							db = dbf.newDocumentBuilder();
							doc = db.newDocument();
							doc.setXmlStandalone(true);

							root = doc.createElement("HDon");
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
																												// SINH
																												// KHI
																												// KY
							elementSubContent.appendChild(commons.createElementWithValue(doc, "MHSo", ""));
							// Ngày lập
							elementSubContent.appendChild(commons.createElementWithValue(doc, "NLap",
									NLap.format(DateTimeFormatter.ISO_DATE)));
							// Số bảng kê
							elementSubContent.appendChild(commons.createElementWithValue(doc, "SBKe", ""));
							// Ngày bảng kê
							elementSubContent.appendChild(commons.createElementWithValue(doc, "NBKe", ""));
							// Đơn vị tiền tệ
							elementSubContent.appendChild(commons.createElementWithValue(doc, "DVTTe", DVTTe));
							// Tỷ giá
							elementSubContent.appendChild(commons.createElementWithValue(doc, "TGia", TGia));
							// Hình thức thanh toán
							elementSubContent.appendChild(commons.createElementWithValue(doc, "HTTToan", HTTToan));
							// MST tổ chức cung cấp giải pháp HĐĐT
							elementSubContent
									.appendChild(commons.createElementWithValue(doc, "MSTTCGP", SystemParams.MSTTCGP));
							// MST đơn vị nhận ủy nhiệm lập hóa đơn
							elementSubContent.appendChild(commons.createElementWithValue(doc, "MSTDVNUNLHDon", ""));
							// Tên đơn vị nhận ủy nhiệm lập hóa đơn
							elementSubContent.appendChild(commons.createElementWithValue(doc, "TDVNUNLHDon", ""));
							// Địa chỉ đơn vị nhận ủy nhiệm lập hóa đơn
							elementSubContent.appendChild(commons.createElementWithValue(doc, "DCDVNUNLHDon", ""));
							elementTmp = doc.createElement("TTKhac"); // THONG TIN KHAC
							elementTmp.appendChild(commons.createElementTTKhac(doc, "PortalLink", "string", link));
							elementTmp.appendChild(commons.createElementTTKhac(doc, "SecureKey", "string", SecureKey));
							elementTmp.appendChild(commons.createElementTTKhac(doc, "SystemKey", "string",
									objectIdEInvoice.toString()));

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
//	                			elementTmp.appendChild(commons.createElementWithValue(doc, "TTKhac", ""));

							/* ADD THONG TIN TK NGAN HANG (NEU CO) */
							elementSubTmp = doc.createElement("TTKhac");
							elementSubTmp.appendChild(
									commons.createElementTTKhac(doc, "TenEN", "string", docTmp.get("NameEN", "")));

							elementSubTmp.appendChild(
									commons.createElementTTKhac(doc, "STKNHang" + intTmp, "string", STKNHang));
							elementSubTmp
									.appendChild(commons.createElementTTKhac(doc, "TNHang" + intTmp, "string", TNHang));

							elementTmp.appendChild(elementSubTmp);
							elementSubContent.appendChild(elementTmp);

							elementTmp = doc.createElement("NMua"); // NGUOI MUA
							elementTmp.appendChild(commons.createElementWithValue(doc, "Ten", TenNM));
							elementTmp.appendChild(commons.createElementWithValue(doc, "MST", MSTNM));
							elementTmp.appendChild(commons.createElementWithValue(doc, "MSVQHNSNM", MDVQHNSNM));
							elementTmp.appendChild(commons.createElementWithValue(doc, "DChi", DChiNM));
							elementTmp.appendChild(commons.createElementWithValue(doc, "MKHang", MaKHangNM));
							elementTmp.appendChild(commons.createElementWithValue(doc, "SDThoai", SDThoaiNM));
							elementTmp.appendChild(commons.createElementWithValue(doc, "CCCDan", SCCCDanNMForm));
							elementTmp.appendChild(commons.createElementWithValue(doc, "SHChieu", SHChieuNMForm));
							elementTmp.appendChild(commons.createElementWithValue(doc, "DCTDTu", DCTDTuNM));
							elementTmp.appendChild(commons.createElementWithValue(doc, "HVTNMHang", HVTNMHangNM));
							elementTmp.appendChild(commons.createElementWithValue(doc, "STKNHang", STKNHangNM));
							elementTmp.appendChild(commons.createElementWithValue(doc, "TNHang", TNHangNM));
							elementTmp.appendChild(commons.createElementWithValue(doc, "TTKhac", ""));
							elementSubContent.appendChild(elementTmp);

							mapVATAmount = new LinkedHashMap<String, Double>();
							mapAmount = new LinkedHashMap<String, Double>();
							elementTmp = doc.createElement("DSHHDVu"); // HH-DV
							// dshhdVuList, listHHDVu, \VATRate,
							// Json.serializer().nodeFromObject(msg.getObjData());

							for (Object o : listHHDVus) {
								JsonNode h = Json.serializer().nodeFromObject(o);
								if (!"".equals(commons.getTextJsonNode(h.at("/ProductName")).trim())) {
									tmp = commons.formatNumberReal(commons.getTextJsonNode(h.at("/VATRate")))
											.replaceAll(",", "");
									switch (tmp) {
									case "0":
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
											return (v == null ? commons
													.ToNumber(commons.getTextJsonNode(h.at("/Total")))
													* ("3".equals(commons.getTextJsonNode(h.at("/Feature"))) ? -1 : 1)
													: v + commons.ToNumber(commons.getTextJsonNode(h.at("/Total")))
															* ("3".equals(commons.getTextJsonNode(h.at("/Feature")))
																	? -1
																	: 1));
										});
										mapVATAmount.compute(tmp, (f, v) -> {
											return (v == null ? commons
													.ToNumber(commons.getTextJsonNode(h.at("/VATAmount")))
													* ("3".equals(commons.getTextJsonNode(h.at("/Feature"))) ? -1 : 1)
													: v + commons.ToNumber(commons.getTextJsonNode(h.at("/VATAmount")))
															* ("3".equals(commons.getTextJsonNode(h.at("/Feature")))
																	? -1
																	: 1));
										});
									}
									Boolean slo1 = false;
									Boolean hsd1 = false;
									String slo = commons.getTextJsonNode(h.at("/SLo"));
									String hsd = commons.getTextJsonNode(h.at("/HanSD"));
									if ("".equals(slo)) {
										slo1 = true;
									}
									if ("".equals(hsd)) {
										hsd1 = true;
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
									if (slo1 == false) {
										elementSubTmp.appendChild(commons.createElementWithValue(doc, "SLo",
												commons.getTextJsonNode(h.at("/SLo"))));
									}
									if (hsd1 == false) {
										elementSubTmp.appendChild(commons.createElementWithValue(doc, "HanSD",
												commons.getTextJsonNode(h.at("/HanSD"))));
									}
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
										String thtien = commons.getTextJsonNode(h.at("/Total")).replaceAll(",", "");
										elementSubTmp.appendChild(commons.createElementWithValue(doc, "ThTien",
												commons.formatNumberReal(thtien).replaceAll(",", "")));
										elementSubTmp.appendChild(commons.createElementWithValue(doc, "TSuat", tmp));
									}

									elementSubTmp01 = doc.createElement("TTKhac");
									elementSubTmp01.appendChild(commons.createElementTTKhac(doc, "VATAmount", "decimal",
											commons.formatNumberReal(commons.getTextJsonNode(h.at("/VATAmount")))
													.replaceAll(",", "")));
									elementSubTmp01.appendChild(commons.createElementTTKhac(doc, "Amount", "decimal",
											commons.formatNumberReal(commons.getTextJsonNode(h.at("/Amount")))
													.replaceAll(",", "")));
									elementSubTmp.appendChild(elementSubTmp01);
									elementTmp.appendChild(elementSubTmp);

								}
								if ("3".equals(commons.getTextJsonNode(h.at("/Feature")))) {
									TongTienChietKhauTM += commons.ToNumber(commons.getTextJsonNode(h.at("/Total")));
								}
							}
							elementSubContent.appendChild(elementTmp);

							elementTmp = doc.createElement("TToan"); // Thong tin thanh toan
							elementSubTmp = doc.createElement("THTTLTSuat");

							/* DANH SACH CAC LOAI THUE SUAT */

							// https://stackoverflow.com/questions/46898/how-do-i-efficiently-iterate-over-each-entry-in-a-java-map
							for (Map.Entry<String, Double> pair : mapVATAmount.entrySet()) {
								if (null != pair.getKey() && !"".equals(pair.getKey())) {
									elementSubTmp01 = doc.createElement("LTSuat");
									elementSubTmp01
											.appendChild(commons.createElementWithValue(doc, "TSuat", pair.getKey()));
									elementSubTmp01.appendChild(commons.createElementWithValue(doc, "ThTien", commons
											.formatNumberReal(mapAmount.get(pair.getKey())).replaceAll(",", "")));
									elementSubTmp01.appendChild(commons.createElementWithValue(doc, "TThue",
											String.format("%.0f", mapVATAmount.get(pair.getKey()))));
									elementSubTmp.appendChild(elementSubTmp01);
								}
							}
							elementTmp.appendChild(elementSubTmp);
//	                			/*DANH SACH CAC LOAI THUE SUAT*/
//	                			
////	                			https://stackoverflow.com/questions/46898/how-do-i-efficiently-iterate-over-each-entry-in-a-java-map
//	                			elementSubTmp01 = doc.createElement("LTSuat");
//	        					elementSubTmp01.appendChild(commons.createElementWithValue(doc, "TSuat", commons.formatNumberReal(tmp).replaceAll(",", "") + "%"));
//	        					elementSubTmp01.appendChild(commons.createElementWithValue(doc, "ThTien", commons.formatNumberReal(TgTTTBSo).replaceAll(",", "")));
//	        					elementSubTmp01.appendChild(commons.createElementWithValue(doc, "TThue", commons.formatNumberReal(TgTThue).replaceAll(",", "")));
//	        					elementSubTmp.appendChild(elementSubTmp01);	
//	                			elementTmp.appendChild(elementSubTmp);
//	                			
							elementTmp.appendChild(commons.createElementWithValue(doc, "TgTCThue",
									commons.formatNumberReal(TTTT).replaceAll(",", "")));
							elementTmp.appendChild(commons.createElementWithValue(doc, "TgTThue",
									commons.formatNumberReal(TTT).replaceAll(",", "")));

							elementSubTmp = doc.createElement("DSLPhi");
							elementSubTmp01 = doc.createElement("LPhi");
							elementSubTmp01.appendChild(commons.createElementWithValue(doc, "TLPhi", ""));
							elementSubTmp01.appendChild(commons.createElementWithValue(doc, "TPhi", "0"));
							elementSubTmp.appendChild(elementSubTmp01);

							elementTmp.appendChild(elementSubTmp);

							elementTmp.appendChild(commons.createElementWithValue(doc, "TTCKTMai", String.valueOf(TongTienChietKhauTM)));
							elementTmp.appendChild(commons.createElementWithValue(doc, "TgTTTBSo",
									commons.formatNumberReal(TTDCThue).replaceAll(",", "")));
							elementTmp.appendChild(commons.createElementWithValue(doc, "TgTTTBChu", TongTienBangchu));
							elementSubTmp = doc.createElement("TTKhac");
							elementTmp.appendChild(elementSubTmp);

							elementSubContent.appendChild(elementTmp);

							elementContent.appendChild(elementSubContent);
							// END - NDHDon: Nội dung hóa đơn

							isSdaveFile = commons.docW3cToFile(doc, Dir, FileNameXML);
							if (!isSdaveFile) {
								throw new Exception("Lưu dữ liệu không thành công.");
							}
							/* END - TAO XML HOA DON */
							// END XML
							// lookup data
							docUpsert = new Document("_id", objectIdEInvoice).append("IssuerId", header.getIssuerId())
									.append("MTDiep", MTDiep)
									.append("EInvoiceDetail", new Document("TTChung",
											new Document("THDon", THDon).append("MaHD", MaHD).append("MauSoHD", MauSoHD)
													.append("KHMSHDon", KHMSHDon).append("KHHDon", KHHDon)
													.append("NLap", NLap).append("DVTTe", DVTTe).append("TGia", TGia)
													.append("HTTToanCode", HTTToanCode).append("HTTToan", HTTToan))
											.append("NDHDon", new Document("NBan",
													new Document("Ten", Ten).append("MST", MST).append("DChi", DChi)
															.append("SDThoai", SDThoai).append("DCTDTu", DCTDTu)
															.append("STKNHang", STKNHang).append("TNHang", TNHang)
															.append("Fax", Fax).append("Website", Website))
													.append("NMua", new Document("Ten", TenNM).append("MST", MSTNM)
															.append("MDVQHNSach", MDVQHNSNM)
															.append("DChi", DChiNM).append("MKHang", MaKHangNM)
															.append("SDThoai", SDThoaiNM)
															.append("CCCDan", SCCCDanNM)
															.append("SHChieu", SHChieuNM)
															.append("DCTDTu", DCTDTuNM)
															.append("HVTNMHang", HVTNMHangNM)
															.append("STKNHang", STKNHangNM).append("TNHang", TNHangNM)))

											.append("DSHHDVu", listHHDVus)
											.append("TToan", new Document("TgTCThue", TTTT).append("TgTThue", TTT)
													.append("TgTTTBSo", TTDCThue).append("TgTTTBChu", TongTienBangchu)))
									.append("SignStatusCode", SignStatusCode).append("EInvoiceStatus", EInvoiceStatus)
									.append("IsDelete", false).append("SecureKey", SecureKey).append("Dir", Dir)
									.append("FileNameXML", FileNameXML).append("InfoCreated",
											new Document("CreateDate", LocalDateTime.now())
													.append("CreateUserID", header.getUserId())
													.append("CreateUserName", header.getUserName())
													.append("CreateUserFullName", header.getUserFullName()));
							
							 mongoClient = cfg.mongoClient();
							 collection = mongoClient.getDatabase(cfg.dbName).getCollection("EInvoice");
							collection.insertOne(docUpsert);
							mongoClient.close();
							
							
							dshhdVuList.clear();
							listHHDVus.clear();
							checkMaHD = false;
						}
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

	// HAM DEM SO O TRONG FILE EXCEL AUTO
	private static EInvoiceExcelForm extractInfoFromCellAuto(List<Cell> cells) {
		EInvoiceExcelForm eInvoiceExcelForm = new EInvoiceExcelForm();
		// Ma hoa don
		Cell MaHD = cells.get(0);
		if (MaHD != null) {
			switch (MaHD.getCellType()) {
			case STRING:
				eInvoiceExcelForm.setMaHD(MaHD.getStringCellValue());
				break;
			case NUMERIC:
				eInvoiceExcelForm.setMaHD((NumberToTextConverter.toText(MaHD.getNumericCellValue())));
				break;
			case BLANK:
				break;
			default:
				break;
			}
		}
		// Ma so thue
		Cell MaSoThue = cells.get(1);
		if (MaSoThue != null) {
			switch (MaSoThue.getCellType()) {
			case STRING:
				eInvoiceExcelForm.setMaSoThue(MaSoThue.getStringCellValue());
				break;
			case NUMERIC:
				eInvoiceExcelForm.setMaSoThue((NumberToTextConverter.toText(MaSoThue.getNumericCellValue())));
				break;
			case BLANK:
				break;
			default:
				break;
			}
		}
		// Ten nguoi mua
		Cell TenNguoiMua = cells.get(2);
		if (TenNguoiMua != null && (TenNguoiMua.getCellType() == CellType.FORMULA)) {
			switch (TenNguoiMua.getCachedFormulaResultType()) {
			case STRING:
				eInvoiceExcelForm.setTenNguoiMua(TenNguoiMua.getStringCellValue());
				break;
			case NUMERIC:
				eInvoiceExcelForm.setTenNguoiMua((NumberToTextConverter.toText(TenNguoiMua.getNumericCellValue())));
				break;
			case BLANK:
				break;
			default:
				break;
			}
		}
		if (TenNguoiMua != null) {
			switch (TenNguoiMua.getCellType()) {
			case STRING:
				eInvoiceExcelForm.setTenNguoiMua(TenNguoiMua.getStringCellValue());
				break;
			case NUMERIC:
				eInvoiceExcelForm.setTenNguoiMua((NumberToTextConverter.toText(TenNguoiMua.getNumericCellValue())));
				break;
			case BLANK:
				break;
			default:
				break;
			}
		}
		// Ten don vi
		Cell TenDonVi = cells.get(3);
		if (TenDonVi != null) {
			switch (TenDonVi.getCellType()) {
			case STRING:
				eInvoiceExcelForm.setTenDonVi(TenDonVi.getStringCellValue());
				break;
			case NUMERIC:
				eInvoiceExcelForm.setTenDonVi((NumberToTextConverter.toText(TenDonVi.getNumericCellValue())));
				break;
			case BLANK:
				break;
			default:
				break;
			}
		}
		// Ma DVQHNS
		Cell MaDVQHNS = cells.get(4);
		if (MaDVQHNS != null) {
			switch (MaDVQHNS.getCellType()) {
			case STRING:
				eInvoiceExcelForm.setMaDVQHNS(MaDVQHNS.getStringCellValue());
				break;
			case NUMERIC:
				eInvoiceExcelForm.setMaDVQHNS((NumberToTextConverter.toText(MaDVQHNS.getNumericCellValue())));
				break;
			case BLANK:
				break;
			default:
				break;
			}
		}
		// Dia chi khach hang
		Cell DiaChiKhachHang = cells.get(5);
		if (DiaChiKhachHang != null) {
			switch (DiaChiKhachHang.getCellType()) {
			case STRING:
				eInvoiceExcelForm.setDiaChiKhachHang(DiaChiKhachHang.getStringCellValue());
				break;
			case NUMERIC:
				eInvoiceExcelForm
						.setDiaChiKhachHang((NumberToTextConverter.toText(DiaChiKhachHang.getNumericCellValue())));
				break;
			case BLANK:
				break;
			default:
				break;
			}

		}
		// Mail khach hang
		Cell MailKhachHang = cells.get(6);
		if (MailKhachHang != null) {
			switch (MailKhachHang.getCellType()) {
			case STRING:
				eInvoiceExcelForm.setMailKhachHang(MailKhachHang.getStringCellValue());
				break;
			case NUMERIC:
				eInvoiceExcelForm.setMailKhachHang((NumberToTextConverter.toText(MailKhachHang.getNumericCellValue())));
				break;
			case BLANK:
				break;
			default:
				break;
			}

		}
		// SDT khach hang
		Cell SDTKhachHang = cells.get(7);
		if (SDTKhachHang != null) {
			switch (SDTKhachHang.getCellType()) {
			case STRING:
				eInvoiceExcelForm.setSDTKhachHang(SDTKhachHang.getStringCellValue());
				break;
			case NUMERIC:
				eInvoiceExcelForm.setSDTKhachHang((NumberToTextConverter.toText(SDTKhachHang.getNumericCellValue())));
				break;
			case BLANK:
				break;
			default:
				break;
			}
		}
		// So CCCD
		Cell SoCCCD = cells.get(8);
		if (SoCCCD != null) {
			switch (SoCCCD.getCellType()) {
			case STRING:
				eInvoiceExcelForm.setSoCCCD(SoCCCD.getStringCellValue());
				break;
			case NUMERIC:
				eInvoiceExcelForm.setSoCCCD((NumberToTextConverter.toText(SoCCCD.getNumericCellValue())));
				break;
			case BLANK:
				break;
			default:
				break;
			}
		}
		// So Ho chieu
		Cell SoHoChieu= cells.get(9);
		if (SoHoChieu != null) {
			switch (SoHoChieu.getCellType()) {
			case STRING:
				eInvoiceExcelForm.setSoHoChieu(SoHoChieu.getStringCellValue());
				break;
			case NUMERIC:
				eInvoiceExcelForm.setSoHoChieu((NumberToTextConverter.toText(SoHoChieu.getNumericCellValue())));
				break;
			case BLANK:
				break;
			default:
				break;
			}
		}
		
		
		// So tai khoan khach hang
		Cell SoTaiKhoan = cells.get(10);
		if (SoTaiKhoan != null) {
			switch (SoTaiKhoan.getCellType()) {
			case STRING:
				eInvoiceExcelForm.setSoTaiKhoan(SoTaiKhoan.getStringCellValue());
				break;
			case NUMERIC:
				eInvoiceExcelForm.setSoTaiKhoan((NumberToTextConverter.toText(SoTaiKhoan.getNumericCellValue())));
				break;
			case BLANK:
				break;
			default:
				break;
			}
		}
		// Ten ngan hang
		Cell TenNganHang = cells.get(11);
		if (TenNganHang != null) {
			switch (TenNganHang.getCellType()) {
			case STRING:
				eInvoiceExcelForm.setTenNganHang(TenNganHang.getStringCellValue());
				break;
			case NUMERIC:
				eInvoiceExcelForm.setTenNganHang((NumberToTextConverter.toText(TenNganHang.getNumericCellValue())));
				break;
			case BLANK:
				break;
			default:
				break;
			}

		}
		// hinh thuc thanh toan
		Cell HinhThucThanhToan = cells.get(12);
		if (HinhThucThanhToan != null) {
			switch (HinhThucThanhToan.getCellType()) {
			case STRING:
				eInvoiceExcelForm.setHinhThucThanhToan(HinhThucThanhToan.getStringCellValue());
				break;
			case NUMERIC:
				eInvoiceExcelForm
						.setHinhThucThanhToan((NumberToTextConverter.toText(HinhThucThanhToan.getNumericCellValue())));
				break;
			case BLANK:
				break;
			default:
				break;
			}
		}
		// Loai tien
		Cell LoaiTien = cells.get(13);
		if (LoaiTien != null) {
			switch (LoaiTien.getCellType()) {
			case STRING:
				eInvoiceExcelForm.setLoaiTien(LoaiTien.getStringCellValue());
				break;
			case NUMERIC:
				eInvoiceExcelForm.setLoaiTien((NumberToTextConverter.toText(LoaiTien.getNumericCellValue())));
				break;
			case BLANK:
				break;
			default:
				break;
			}

		}
		// Ten hang hoa
		Cell TenHangHoa = cells.get(14);
		if (TenHangHoa != null) {
			switch (TenHangHoa.getCellType()) {
			case STRING:
				eInvoiceExcelForm.setTenHangHoa(TenHangHoa.getStringCellValue());
				break;
			case NUMERIC:
				eInvoiceExcelForm.setTenHangHoa((NumberToTextConverter.toText(TenHangHoa.getNumericCellValue())));
				break;
			case BLANK:
				break;
			default:
				break;
			}
		}
		// Ma hang hoa
		Cell MaHangHoa = cells.get(15);
		if (MaHangHoa != null) {
			switch (MaHangHoa.getCellType()) {
			case STRING:
				eInvoiceExcelForm.setMaHangHoa(MaHangHoa.getStringCellValue());
				break;
			case NUMERIC:
				eInvoiceExcelForm.setMaHangHoa((NumberToTextConverter.toText(MaHangHoa.getNumericCellValue())));
				break;
			case BLANK:
				break;
			default:
				break;
			}
		}
		// So lo
		Cell SLo = cells.get(16);
		if (SLo != null) {
			switch (SLo.getCellType()) {
			case STRING:
				eInvoiceExcelForm.setSLo(SLo.getStringCellValue());
				break;
			case NUMERIC:
				eInvoiceExcelForm.setSLo((NumberToTextConverter.toText(SLo.getNumericCellValue())));
				break;
			case BLANK:
				break;
			default:
				break;
			}
		}
		// han su dung
		Cell HanSD = cells.get(17);
		if (HanSD != null) {
			switch (HanSD.getCellType()) {
			case STRING:
				eInvoiceExcelForm.setHanSD(HanSD.getStringCellValue());
				break;
			case NUMERIC:
				eInvoiceExcelForm.setHanSD((NumberToTextConverter.toText(HanSD.getNumericCellValue())));
				break;
			case BLANK:
				break;
			default:
				break;
			}
		}
		// Don vi tinh
		Cell DonViTinh = cells.get(18);
		if (DonViTinh != null) {
			switch (DonViTinh.getCellType()) {
			case STRING:
				eInvoiceExcelForm.setDonViTinh(DonViTinh.getStringCellValue());
				break;
			case NUMERIC:
				eInvoiceExcelForm.setDonViTinh((NumberToTextConverter.toText(DonViTinh.getNumericCellValue())));
				break;
			case BLANK:
				break;
			default:
				break;
			}
		}
		// so luong
		Cell SoLuong = cells.get(19);
		if (SoLuong != null) {
			switch (SoLuong.getCellType()) {
			case STRING:
				eInvoiceExcelForm.setSoLuong((Double.valueOf((String) SoLuong.getStringCellValue())));
				break;
			case NUMERIC:
				eInvoiceExcelForm.setSoLuong(SoLuong.getNumericCellValue());
				break;
			case BLANK:
				break;
			default:
				break;
			}
		}
		// Don gia
		Cell DonGia = cells.get(20);
		if (DonGia != null) {
			switch (DonGia.getCellType()) {
			case STRING:
				eInvoiceExcelForm.setDonGia((Double.valueOf((String) DonGia.getStringCellValue())));
				break;
			case NUMERIC:
				eInvoiceExcelForm.setDonGia(DonGia.getNumericCellValue());
				break;
			case BLANK:
				break;
			default:
				break;
			}
		}
		// Thanh tien
		Cell ThanhTien = cells.get(21);
		if (ThanhTien != null && (ThanhTien.getCellType() == CellType.FORMULA)) {
			switch (ThanhTien.getCachedFormulaResultType()) {
			case STRING:
				eInvoiceExcelForm.setThanhTien((Double.valueOf((String) ThanhTien.getStringCellValue())));
				break;
			case NUMERIC:
				eInvoiceExcelForm.setThanhTien(ThanhTien.getNumericCellValue());
				break;
			case BLANK:
				break;
			default:
				break;
			}
		}
		if (ThanhTien != null) {
			switch (ThanhTien.getCellType()) {
			case STRING:
				eInvoiceExcelForm.setThanhTien((Double.valueOf((String) ThanhTien.getStringCellValue())));
				break;
			case NUMERIC:
				eInvoiceExcelForm.setThanhTien(ThanhTien.getNumericCellValue());
				break;
			case BLANK:
				break;
			default:
				break;
			}
		}
		// Thue suat
		Cell ThueSuat = cells.get(22);
		if (ThueSuat != null) {
			switch (ThueSuat.getCellType()) {
			case STRING:
				eInvoiceExcelForm.setThueSuat((Double.valueOf((String) ThueSuat.getStringCellValue())));
				break;
			case NUMERIC:
				eInvoiceExcelForm.setThueSuat(ThueSuat.getNumericCellValue());
				break;
			case BLANK:
				break;
			default:
				break;
			}
		}
		// Tien thue
		Cell TienThue = cells.get(23);
		if (TienThue != null && (TienThue.getCellType() == CellType.FORMULA)) {
			switch (TienThue.getCachedFormulaResultType()) {
			case STRING:
				eInvoiceExcelForm.setTienThue((Double.valueOf((String) TienThue.getStringCellValue())));
				break;
			case NUMERIC:
				eInvoiceExcelForm.setTienThue(TienThue.getNumericCellValue());
				break;
			case BLANK:
				break;
			default:
				break;
			}
		}
		if (TienThue != null) {
			switch (TienThue.getCellType()) {
			case STRING:
				eInvoiceExcelForm.setTienThue((Double.valueOf((String) TienThue.getStringCellValue())));
				break;
			case NUMERIC:
				eInvoiceExcelForm.setTienThue(TienThue.getNumericCellValue());
				break;
			case BLANK:
				break;
			default:
				break;
			}
		}

		// Tinh chat
		Cell TinhChat = cells.get(24);
		if (TinhChat != null) {
			switch (TinhChat.getCellType()) {
			case STRING:
				eInvoiceExcelForm.setTinhChat(TinhChat.getStringCellValue());
				break;
			case NUMERIC:
				eInvoiceExcelForm.setTinhChat((NumberToTextConverter.toText(TinhChat.getNumericCellValue())));
				break;
			case BLANK:
				break;
			default:
				break;
			}
		}

		// ty gia
		Cell TyGia = cells.get(25);
		if (TyGia != null) {
			switch (TyGia.getCellType()) {
			case STRING:
				eInvoiceExcelForm.setTyGia(TyGia.getStringCellValue());
				break;
			case NUMERIC:
				eInvoiceExcelForm.setTyGia((NumberToTextConverter.toText(TyGia.getNumericCellValue())));
				break;
			case BLANK:
				break;
			default:
				break;
			}
		}
		// Tra ve danh sach
		return eInvoiceExcelForm;
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
				.append("IsDelete", false)
				.append("EInvoiceStatus", new Document("$in", Arrays.asList("ERROR_CQT", "PROCESSING", "COMPLETE")))
				.append("SignStatusCode", Constants.INVOICE_SIGN_STATUS.SIGNED);

		Document docTmp = null;
		
		MongoClient mongoClient = cfg.mongoClient();
		MongoCollection<Document> collection = mongoClient.getDatabase(cfg.dbName).getCollection("EInvoice");
		try {
			docTmp =   collection.find(docFind).allowDiskUse(true).iterator().next();	
		} catch (Exception e) {
			// TODO: handle exception
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
			nodeList = (NodeList) xPath.evaluate("DLieu/TBao/TTTNhan", nodeTDiep, XPathConstants.NODESET);
			if ("999".equals(CQT_MLTDiep)) {
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
				hItem = new HashMap<String, Object>();
				hItem.put("STT", stt);
				hItem.put("Date", commons.getTextFromNodeXML(
						(Element) xPath.evaluate("DLieu/TBao/DLTBao/TGGui", nodeTDiep, XPathConstants.NODE)));
				hItem.put("MLoi", commons.getTextFromNodeXML((Element) xPath
						.evaluate("DLieu/TBao/DLTBao/LCMa/DSLDo/LDo/MLoi", nodeTDiep, XPathConstants.NODE)));
				hItem.put("MTLoi", commons.getTextFromNodeXML((Element) xPath
						.evaluate("DLieu/TBao/DLTBao/LCMa/DSLDo/LDo/MTLoi", nodeTDiep, XPathConstants.NODE)));
				rowsReturn.add(hItem);

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
				if(objData != null) {
					jsonData = Json.serializer().nodeFromObject(msg.getObjData());
				}else{
					throw new Exception("Lỗi dữ liệu đầu vào");
				}
				int shd = 0;
				int shdky = 0;
				 
					List<Long> billNumbers = new ArrayList<Long>();
					FindOneAndUpdateOptions options = null;
					int eInvoiceNumber = 0;
					Document docFind  = null;	
					int checkshd = 0;
					String IDMS="";
					ObjectId objectIdMS = null;
					String IdMauSo = "";
					String SLHDon = commons.getTextJsonNode(jsonData.at("/soLuong")).replaceAll("\\s", "0");
					int Soluong = Integer.parseInt(SLHDon);
					//CHECK SL CON LAI DE KY HOA DON
				//	int dem = ids.size();
					//END CHECK SL CON LAI
					
					for(JsonNode o: jsonData.at("/id")) {					  
						String _id = commons.getTextJsonNode(o);	
					int currentYear = LocalDate.now().get(ChronoField.YEAR);
					ObjectId objectId = null;
					try {
						objectId = new ObjectId(_id);
					}catch(Exception e) {}
					
					/*NHO KIEM TRA XEM CO HD NAO DANG KY KHONG*/
					
					Document fillter = new Document("_id", 1)
							.append("IssuerId", 1)
							.append("Dir", 1)
							.append("FileNameXML", 1)				
							.append("MCCQT", 1)
							.append("MTDiep", 1)
							.append("EInvoiceDetail", 1);
					
					 docFind = new Document("IssuerId", header.getIssuerId())
							.append("IsDelete", new Document("$ne", true)).append("_id", objectId)
							.append("EInvoiceStatus", new Document("$in", Arrays.asList("CREATED", "PENDING")))
							.append("SignStatusCode", new Document("$in", Arrays.asList("NOSIGN", "PROCESSING")));
					List<Document> pipeline = new ArrayList<Document>();
					pipeline.add(new Document("$match", docFind));
					pipeline.add(new Document("$project", fillter));
					
					
					/*KIEM TRA THONG TIN MAU HD*/
					pipeline.add(
						new Document("$lookup", 
							new Document("from", "DMMauSoKyHieu")
							.append("let", new Document("vMauSoHD", "$EInvoiceDetail.TTChung.MauSoHD").append("vIssuerId", "$IssuerId"))
							.append("pipeline", 
								Arrays.asList(
									new Document("$match",
										new Document("$expr", 
											new Document("$and", 
												Arrays.asList(
													new Document("$gt", Arrays.asList("$ConLai", 0)),
													new Document("$eq", Arrays.asList("$IsActive", true)),
													new Document("$ne", Arrays.asList("$IsDelete", true)),
													new Document("$eq", Arrays.asList(new Document("$toString", "$_id"), "$$vMauSoHD")),
													new Document("$eq", Arrays.asList("$IssuerId", "$$vIssuerId")),
													new Document("$eq", Arrays.asList("$NamPhatHanh", currentYear))
												)
											)
										)
									),
									new Document("$project", new Document("_id", 1).append("Status", 1).append("SHDHT", 1).append("SoLuong", 1).append("ConLai", 1).append("TuSo", 1))
								)
							)
							.append("as", "DMMauSoKyHieu")
						)
					);
					pipeline.add(new Document("$unwind", new Document("path", "$DMMauSoKyHieu").append("preserveNullAndEmptyArrays", true)));
					
					Document docTmp = null;
				
					
					MongoClient mongoClient = cfg.mongoClient();
					MongoCollection<Document> collection = mongoClient.getDatabase(cfg.dbName).getCollection("EInvoice");
					try {
						docTmp =   collection.aggregate(pipeline).allowDiskUse(true).iterator().next();		
					} catch (Exception e) {
						// TODO: handle exception
					}
							
					mongoClient.close();
					
					if(null == docTmp) {
						return fileInfo;
					}
					if(null == docTmp.get("DMMauSoKyHieu")) {
						return fileInfo;
					}
					
					
					
			
					IdMauSo = docTmp.getEmbedded(Arrays.asList("DMMauSoKyHieu", "_id"), ObjectId.class).toString();
					try {
						objectIdMS = new ObjectId(IdMauSo);
					}catch(Exception e) {}								
					if(IDMS == "") {
						IDMS = IdMauSo;
					}
					if(IDMS != "" && !IDMS.equals(IdMauSo)) {
						fileInfo.setFormIssueInvoiceID(IDMS);
						fileInfo.setCheck("error");
						return fileInfo;
					}
				
					
					int CheckSLConlai = docTmp.getEmbedded(Arrays.asList("DMMauSoKyHieu", "ConLai"), 0);
					int checkSL = CheckSLConlai - Soluong;
					if(checkSL<0) {
						fileInfo.setCheck("Not Enough");
						return fileInfo;	
					}
				}

				String issuerId = header.getIssuerId();
				issuerLocks_sign.putIfAbsent(issuerId, new ReentrantLock());
				ReentrantLock lock = issuerLocks_sign.get(issuerId);

				if (lock.tryLock()) {
					try {
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

							Document fillter = new Document("_id", 1).append("IssuerId", 1).append("Dir", 1)
									.append("FileNameXML", 1).append("MCCQT", 1).append("MTDiep", 1)
									.append("EInvoiceDetail", 1);

							docFind = new Document("IssuerId", header.getIssuerId())
									.append("IsDelete", new Document("$ne", true)).append("_id", objectId)
									.append("EInvoiceStatus", new Document("$in", Arrays.asList("CREATED", "PENDING")))
									.append("SignStatusCode",
											new Document("$in", Arrays.asList("NOSIGN", "PROCESSING")));

							List<Document> pipeline = new ArrayList<Document>();
							pipeline.add(new Document("$match", docFind));
							pipeline.add(new Document("$project", fillter));
							/* KIEM TRA THONG TIN MAU HD */
							pipeline.add(new Document("$lookup", new Document("from", "DMMauSoKyHieu").append("let",
									new Document("vMauSoHD", "$EInvoiceDetail.TTChung.MauSoHD").append("vIssuerId",
											"$IssuerId"))
									.append("pipeline", Arrays.asList(
											new Document("$match", new Document("$expr", new Document("$and",
													Arrays.asList(new Document("$gt", Arrays.asList("$ConLai", 0)),
															new Document("$eq", Arrays.asList("$IsActive", true)),
															new Document("$ne", Arrays.asList("$IsDelete", true)),
															new Document("$eq",
																	Arrays.asList(new Document("$toString", "$_id"),
																			"$$vMauSoHD")),
															new Document("$eq",
																	Arrays.asList("$IssuerId", "$$vIssuerId")),
															new Document("$eq",
																	Arrays.asList("$NamPhatHanh", currentYear)))))),
											new Document("$project",
													new Document("_id", 1).append("Status", 1).append("SHDHT", 1)
															.append("SoLuong", 1).append("ConLai", 1).append("TuSo", 1))

									)).append("as", "DMMauSoKyHieu")));
							pipeline.add(new Document("$unwind",
									new Document("path", "$DMMauSoKyHieu").append("preserveNullAndEmptyArrays", true)));

							Document docTmp = null;

							MongoClient mongoClient = cfg.mongoClient();
							MongoCollection<Document> collection = mongoClient.getDatabase(cfg.dbName)
									.getCollection("EInvoice");
							try {
								docTmp = collection.aggregate(pipeline).allowDiskUse(true).iterator().next();
							} catch (Exception e) {
								// TODO: handle exception
							}

							mongoClient.close();

							if (null == docTmp) {
								return fileInfo;
							}
							docFind2 = new Document("IssuerId", header.getIssuerId())
									.append("IsDelete", new Document("$ne", true)).append("_id", objectIdMS);
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

							int shdcheck = docTmp.getEmbedded(Arrays.asList("EInvoiceDetail", "TTChung", "SHDon"), 0);
							if (shdcheck > 0) {
								eInvoiceNumber = shdcheck;

							} else {
								eInvoiceNumber = docTmp.getEmbedded(Arrays.asList("DMMauSoKyHieu", "SoLuong"), 0)
										- docTmp.getEmbedded(Arrays.asList("DMMauSoKyHieu", "ConLai"), 0)
										+ docTmp.getEmbedded(Arrays.asList("DMMauSoKyHieu", "TuSo"), 0);
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
							Node nodeDLHDon = (Node) xPath.evaluate("/HDon/DLHDon[@Id='data']", doc,
									XPathConstants.NODE);

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
							collection = mongoClient.getDatabase(cfg.dbName).getCollection("EInvoice");
							collection.findOneAndUpdate(docFind,
									new Document("$set", new Document("EInvoiceDetail.TTChung.SHDon", eInvoiceNumber)
											.append("EInvoiceStatus", "PENDING")

									), options);
							mongoClient.close();

							if (shdky != 0) {

								mongoClient = cfg.mongoClient();
								collection = mongoClient.getDatabase(cfg.dbName).getCollection("DMMauSoKyHieu");
								collection.findOneAndUpdate(docFind2,
										new Document("$inc", new Document("ConLai", -1).append("SHDHT", +1)),

										options);
								mongoClient.close();
							}
						}
					} finally {
						lock.unlock();
					}
				} else {
					fileInfo.setCheck("LOCK");
					return fileInfo;
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
//			String folderTmp = commons.csRandomAlphaNumbericString(15);
		String taxCode = input.getTaxcode();

		// CHECK USER CON
		String[] split_mst = taxCode.split("_");
		int dem_mst = split_mst.length;
		if (dem_mst == 2) {
			taxCode = split_mst[0];
		}
//			else {
//				taxCode = taxCode;
//			}
		// END CHECK USER CON

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
//			String SigningTime = commons.getTextFromNodeXML((Element) xPath.evaluate(
//					"/HDon/DSCKS/NBan/Signature/Object[@Id='SigningTime']/SignatureProperties/SignatureProperty/SigningTime",
//					xmlDoc, XPathConstants.NODE));

			LocalDate ldNLap = null;
//			LocalDate ldSigningTime = null;
			try {
				ldNLap = commons.convertStringToLocalDate(NLap, "yyyy-MM-dd");
			} catch (Exception e) {
			}
//			if (SigningTime.length() > 10) {
//				ldSigningTime = commons.convertStringToLocalDate(SigningTime.substring(0, 10), "yyyy-MM-dd");
//			}
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
			Document docFind = new Document("IssuerId", header.getIssuerId()).append("IsDelete", false)
					.append("EInvoiceDetail.TTChung.SHDon", eInvoiceNumber).append("EInvoiceStatus", "PENDING")
					.append("_id", objectId);
			pipeline = new ArrayList<Document>();
			pipeline.add(new Document("$match", docFind));
			pipeline.add(new Document("$lookup",
					new Document("from", "EInvoice").append("pipeline",
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
			MongoCollection<Document> collection = mongoClient.getDatabase(cfg.dbName).getCollection("EInvoice");
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
			 collection = mongoClient.getDatabase(cfg.dbName).getCollection("EInvoice");
			collection.findOneAndUpdate(docFind,
					new Document("$set", new Document("SignStatusCode", "SIGNED").append("InfoSigned",
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
	public MsgRsp change(JSONRoot jsonRoot, String _id) throws Exception {
		ObjectId objectId = null;
		try {
			objectId = new ObjectId(_id);
		} catch (Exception e) {
		}
		Document docFind = null;
		Msg msg = jsonRoot.getMsg();
		MsgHeader header = msg.getMsgHeader();
		MsgPage page = msg.getMsgPage();
		Object objData = msg.getObjData();
		MsgRsp rsp = new MsgRsp(header);
		MspResponseStatus responseStatus = null;

		Document docTmp = null;
		List<Document> pipeline = null;
		pipeline = new ArrayList<Document>();
		pipeline.add(new Document("$match", new Document("_id", objectId).append("IsDelete", false)));
		pipeline.add(new Document("$project", new Document("_id", 1).append("EInvoiceDetail", 1).append("MTDiep", 1)));

	
		MongoClient mongoClient = cfg.mongoClient();
		MongoCollection<Document> collection =  mongoClient.getDatabase(cfg.dbName).getCollection("EInvoice");
		try {
			docTmp =   collection.aggregate(pipeline).allowDiskUse(true).iterator().next();		
		} catch (Exception e) {
			// TODO: handle exception
		}
		mongoClient.close();

		if (null == docTmp) {
			responseStatus = new MspResponseStatus(9999, "Không tìm thấy thông tin hóa đơn");
			rsp.setResponseStatus(responseStatus);
			return rsp;
		}
		docFind = new Document("IssuerId", header.getIssuerId()).append("IsDelete", false).append("_id", objectId);
		ArrayList<HashMap<String, Object>> rowsReturn = new ArrayList<HashMap<String, Object>>();
		String MTDiep = docTmp.get("MTDiep", "");
		String mtdiepcu = MTDiep;
		MTDiep = SystemParams.MSTTCGP
				+ commons.csRandomAlphaNumbericString(46 - SystemParams.MSTTCGP.length()).toUpperCase();
		FindOneAndUpdateOptions options = null;

		String MSKH = docTmp.getEmbedded(Arrays.asList("EInvoiceDetail", "TTChung", "KHMSHDon"), "")
				+ docTmp.getEmbedded(Arrays.asList("EInvoiceDetail", "TTChung", "KHHDon"), "");
		Integer SHD = docTmp.getEmbedded(Arrays.asList("EInvoiceDetail", "TTChung", "SHDon"), 0);

		HashMap<String, Object> hItem = null;
		hItem = new HashMap<String, Object>();
		hItem.put("SHD", SHD);
		hItem.put("MS", MSKH);
		hItem.put("MTDiep", MTDiep);
		hItem.put("MTDiepCU", mtdiepcu);
		rowsReturn.add(hItem);

//		Document docR = null;

		options = new FindOneAndUpdateOptions();
		options.upsert(false);
		options.maxTime(5000, TimeUnit.MILLISECONDS);
		options.returnDocument(ReturnDocument.AFTER);


		 mongoClient = cfg.mongoClient();
			collection = mongoClient.getDatabase(cfg.dbName).getCollection("EInvoice");
			collection.findOneAndUpdate(docFind,
					new Document("$set", new Document("MTDiep", MTDiep).append("MTDiepCU", mtdiepcu)), options);		
			mongoClient.close();
			
		responseStatus = new MspResponseStatus(0, "SUCCESS");
		rsp.setResponseStatus(responseStatus);
		HashMap<String, Object> mapDataR = new HashMap<String, Object>();
		mapDataR.put("rows", rowsReturn);
		rsp.setObjData(mapDataR);
		return rsp;
	}

	@Override
	public MsgRsp checkMST(JSONRoot jsonRoot) throws Exception {
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

//		String actionCode = header.getActionCode();
		String mst = commons.getTextJsonNode(jsonData.at("/MST")).replaceAll("\\s", "");

		MsgRsp rsp = new MsgRsp(header);
		rsp.setMsgPage(page);
		MspResponseStatus responseStatus = null;

		Document docFind = null;
		Document docTmp = null;

		/* KIEM TRA THONG TIN KHACH HANG - USERS */

		docFind = new Document("IssuerId", header.getIssuerId()).append("TaxCode", mst).append("IsDelete", new Document("$ne", true));
		MongoClient mongoClient = cfg.mongoClient();
		MongoCollection<Document> collection = mongoClient.getDatabase(cfg.dbName).getCollection("DMCustomer");
		try {
			docTmp = collection.find(docFind).allowDiskUse(true).iterator().next();
		} catch (Exception e) {

		}

		mongoClient.close();
		if (null == docTmp) {
			responseStatus = new MspResponseStatus(9999, "Không tìm thấy thông tin khách hàng.");
			rsp.setResponseStatus(responseStatus);
			return rsp;
		}

		String ma_kh = docTmp.getString("CustomerCode");
		String hvtnmh = docTmp.getString("CustomerName");
		String tendv = docTmp.getString("CompanyName");
		String dchi = docTmp.getString("Address");
		String email = docTmp.getString("Email");
		String sdt = docTmp.getString("Phone");
		String emailcc = docTmp.getString("EmailCC");

		String TH = ma_kh + ";" + hvtnmh + ";" + tendv + ";" + emailcc + ";" + email + ";" + sdt + ";" + dchi;
		responseStatus = new MspResponseStatus(0, TH);
		rsp.setResponseStatus(responseStatus);
		return rsp;

	}

	@Override
	public MsgRsp saveNMua(JSONRoot jsonRoot) throws Exception {
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
		FindOneAndUpdateOptions options = null;
//		String actionCode = header.getActionCode();
		String mst = commons.getTextJsonNode(jsonData.at("/MST")).trim().replaceAll("\\s+", "")
				.replaceAll("[+^%$#@&*]*", "").replaceAll("[a-z][A-Z]*", "");
		String mkh = commons.getTextJsonNode(jsonData.at("/MKH")).replaceAll("\\s", "");
		String hvtnm = commons.getTextJsonNode(jsonData.at("/HVTNM")).replaceAll("\\s", " ");
		String tdv = commons.getTextJsonNode(jsonData.at("/TDV")).replaceAll("\\s", " ");
		String dchi = commons.getTextJsonNode(jsonData.at("/DCHI")).replaceAll("\\s", " ");
		String email = commons.getTextJsonNode(jsonData.at("/EMAIL")).replaceAll("\\s", "");
		String emailcc = commons.getTextJsonNode(jsonData.at("/EMAILCC")).replaceAll("\\s", "");
		String sdt = commons.getTextJsonNode(jsonData.at("/SDT")).replaceAll("\\s", "");

		MsgRsp rsp = new MsgRsp(header);
		rsp.setMsgPage(page);
		MspResponseStatus responseStatus = null;

		Document docFind = null;
		Document docTmp = null;


		/* KIEM TRA THONG TIN KHACH HANG - USERS */

		docFind = new Document("IssuerId", header.getIssuerId()).append("TaxCode", mst)	.append("IsDelete", new Document("$ne", true));
		MongoClient mongoClient = cfg.mongoClient();
		MongoCollection<Document> collection = mongoClient.getDatabase(cfg.dbName).getCollection("DMCustomer");
		try {
			docTmp =   collection.find(docFind).allowDiskUse(true).iterator().next();
		} catch (Exception e) {
			// TODO: handle exception
		}
					
		mongoClient.close();
		if("".equals(mkh))
		{
			mkh = "MCK" + commons.convertLocalDateTimeToString(LocalDate.now(), "yyyyMMdd") + "-" + commons.csRandomAlphaNumbericString(3).toUpperCase();
		}
		if (null == docTmp) {
			ObjectId objectId = null;
			objectId = new ObjectId();
			docUpsert = new Document("_id", objectId).append("IssuerId", header.getIssuerId()).append("TaxCode", mst)
					.append("CustomerCode", mkh).append("CompanyName", tdv).append("CustomerName", hvtnm)
					.append("Address", dchi).append("Email", email).append("EmailCC", emailcc).append("Phone", sdt)
					.append("InfoCreated",
							new Document("CreateDate", LocalDateTime.now()).append("CreateUserID", header.getUserId())
									.append("CreateUserName", header.getUserName())
									.append("CreateUserFullName", header.getUserFullName()));

	
			 mongoClient = cfg.mongoClient();
				collection = mongoClient.getDatabase(cfg.dbName).getCollection("DMCustomer");
				collection.insertOne(docUpsert);			
				mongoClient.close();
				
			responseStatus = new MspResponseStatus(0, "SUCCESS");
			rsp.setResponseStatus(responseStatus);
			return rsp;
		} else {
			options = new FindOneAndUpdateOptions();
			options.upsert(false);
			options.maxTime(5000, TimeUnit.MILLISECONDS);
			options.returnDocument(ReturnDocument.AFTER);

	
			
			MongoClient mongoClient2 = cfg.mongoClient();
				collection = mongoClient2.getDatabase(cfg.dbName).getCollection("DMCustomer");
				collection.findOneAndUpdate(docFind,
						new Document("$set",
								new Document("IssuerId", header.getIssuerId()).append("TaxCode", mst)
										.append("CustomerCode", mkh).append("CompanyName", tdv)
										.append("CustomerName", hvtnm).append("Address", dchi).append("Email", email)
										.append("EmailCC", emailcc).append("Phone", sdt).append("InfoUpdated",
												new Document("UpdatedDate", LocalDateTime.now())
														.append("UpdatedUserID", header.getUserId())
														.append("UpdatedUserName", header.getUserName())
														.append("UpdatedUserFullName", header.getUserFullName()))),
						options);		
				mongoClient2.close();
				
			responseStatus = new MspResponseStatus(0, "SUCCESS");
			rsp.setResponseStatus(responseStatus);
			return rsp;
		}

	}

	///
	@Override
	public MsgRsp getMS(JSONRoot jsonRoot) throws Exception {
		Msg msg = jsonRoot.getMsg();
		MsgHeader header = msg.getMsgHeader();
		MsgPage page = msg.getMsgPage();
		Object objData = msg.getObjData();
		Document docTmp = null;
		Document docTmp1 = null;
		JsonNode jsonData = null;
		if (objData != null) {
			jsonData = Json.serializer().nodeFromObject(msg.getObjData());
		} else {
			throw new Exception("Lỗi dữ liệu đầu vào");
		}

		MsgRsp rsp = new MsgRsp(header);
		rsp.setMsgPage(page);
		MspResponseStatus responseStatus = null;

		List<Document> pipeline = null;

		/* KIEM TRA THONG TIN KHACH HANG - USERS */
		Document docMatch = new Document("IsDelete", false).append("IssuerId", header.getIssuerId());
		pipeline = new ArrayList<Document>();

		pipeline.add(new Document("$match", docMatch));
		pipeline.add(new Document("$sort", new Document("_id", -1)));
		pipeline.add(new Document("$project", new Document("_id", 1).append("EInvoiceDetail", 1)));


		MongoClient mongoClient = cfg.mongoClient();
		MongoCollection<Document> collection = mongoClient.getDatabase(cfg.dbName).getCollection("EInvoice");
		try {
			docTmp = collection.aggregate(pipeline).allowDiskUse(true).iterator().next();
		} catch (Exception e) {

		}

		mongoClient.close();
		if (null == docTmp) {
			// KIEM TRA TRONG MAU SO KY HIEU

			Document docKHMS = new Document("IssuerId", header.getIssuerId()).append("IsDelete", false)
					.append("KHMSHDon", "1").append("IsActive", true);

			 mongoClient = cfg.mongoClient();
			 collection = mongoClient.getDatabase(cfg.dbName).getCollection("DMMauSoKyHieu");
			try {
				docTmp1 =   collection.find(docKHMS).limit(1).allowDiskUse(true).iterator().next();		
			} catch (Exception e) {
				// TODO: handle exception
			}
				
			mongoClient.close();
			if (docTmp1 == null) {
				responseStatus = new MspResponseStatus(9999, "Không tìm thấy thông tin hóa đơn.");
				rsp.setResponseStatus(responseStatus);
				return rsp;
			}

			String MauSo = docTmp1.getObjectId("_id").toString();
			responseStatus = new MspResponseStatus(0, MauSo);
			rsp.setResponseStatus(responseStatus);
			return rsp;
		}

		String MauSo = docTmp.getEmbedded(Arrays.asList("EInvoiceDetail", "TTChung", "MauSoHD"), "");

		responseStatus = new MspResponseStatus(0, MauSo);
		rsp.setResponseStatus(responseStatus);
		return rsp;

	}

	//// LAY MA CO QUAN THUE HANG LOAT

	@Transactional(rollbackFor = { Exception.class })
	@Override
	public MsgRsp refreshAllStatusCQT(JSONRoot jsonRoot) throws Exception {
		Msg msg = jsonRoot.getMsg();
		MsgHeader header = msg.getMsgHeader();
		MsgPage page = msg.getMsgPage();

		Object objData = msg.getObjData();

		List<String> ids = null;

//		String listSendMail = "";
		List<String> listMail_ = new ArrayList<String>();
		JsonNode jsonData = null;
		if (objData != null) {
			jsonData = Json.serializer().nodeFromObject(msg.getObjData());
		} else {
			throw new Exception("Lỗi dữ liệu đầu vào");
		}

		String _id = "";
		String _ListId = commons.getTextJsonNode(jsonData.at("/_id")).replaceAll("\\s", "");
		ids = null;
		try {
			ids = Json.serializer().fromJson(commons.decodeBase64ToString(_ListId), new TypeReference<List<String>>() {
			});
		} catch (Exception e) {
		}

		MsgRsp rsp = new MsgRsp(header);
		rsp.setMsgPage(page);
		MspResponseStatus responseStatus = null;

		for (int t = 0; t < ids.size(); t++) {
			_id = ids.get(t);
			ObjectId objectId = null;
			try {
				objectId = new ObjectId(_id);
			} catch (Exception e) {
			}

//		try {
			FindOneAndUpdateOptions options = null;
			/* KIEM TRA XEM THONG TIN TKHAI CO TON TAI KHONG */
			Document docFind = new Document("IssuerId", header.getIssuerId()).append("_id", objectId)
					.append("IsDelete", false).append("SignStatusCode", "SIGNED")
					.append("EInvoiceStatus", new Document("$in", Arrays.asList("ERROR_CQT", "PROCESSING")));

			Document docTmp = null;


			MongoClient mongoClient = cfg.mongoClient();
			MongoCollection<Document> collection = mongoClient.getDatabase(cfg.dbName).getCollection("EInvoice");
			try {
				docTmp = collection.find(docFind).allowDiskUse(true).iterator().next();
			} catch (Exception e) {

			}

			mongoClient.close();

			if (null == docTmp) {
				break;
			}

			String MTDiep = docTmp.get("MTDiep", "");
			org.w3c.dom.Document rTCTN = null;
			String MaKetQua = "";
			String MoTaKetQua = "";
			rTCTN = tctnService.callTraCuuThongDiep(MTDiep);
			if (rTCTN == null) {
				rTCTN = tctnService.callTraCuuThongDiep(MTDiep);
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
			String fileName1 = _id + "_signed.xml";
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
				MaKetQua = commons.getTextFromNodeXML(
						(Element) xPath.evaluate("MaKetQua", nodeKetQuaTraCuu, XPathConstants.NODE));
				MoTaKetQua = commons.getTextFromNodeXML(
						(Element) xPath.evaluate("MoTaKetQua", nodeKetQuaTraCuu, XPathConstants.NODE));

				if ("2".equals(MaKetQua) && "Mã giao dịch không đúng".equals(MoTaKetQua)) {
					doc = commons.fileToDocument(file, true);
					rTCTN1 = tctnService.callTiepNhanThongDiep("200", MTDiep, MST, "1", doc);
					xPath = XPathFactory.newInstance().newXPath();
					Node nodeDLHDon = (Node) xPath.evaluate("/TDiep", rTCTN1, XPathConstants.NODE);
					MaKetQua = commons
							.getTextFromNodeXML((Element) xPath.evaluate("MaKetQua", nodeDLHDon, XPathConstants.NODE));
					MoTaKetQua = commons.getTextFromNodeXML(
							(Element) xPath.evaluate("MoTaKetQua", nodeDLHDon, XPathConstants.NODE));
				}

				if ("".equals(MaKetQua)) {
					rTCTN = tctnService.callTraCuuThongDiep(MTDiep);
					nodeKetQuaTraCuu = (Node) xPath.evaluate("/KetQuaTraCuu", rTCTN, XPathConstants.NODE);
					MaKetQua = commons.getTextFromNodeXML(
							(Element) xPath.evaluate("MaKetQua", nodeKetQuaTraCuu, XPathConstants.NODE));
					MoTaKetQua = commons.getTextFromNodeXML(
							(Element) xPath.evaluate("MoTaKetQua", nodeKetQuaTraCuu, XPathConstants.NODE));
				} else {
					continue;
				}
			}

			Node nodeTDiep = null;
			String checkMLTDiep = "";
			boolean check_ = false;
			String MLoi = "";
			String MTLoi = "";
			String MTDTChieu = "";
			String CQT_MLTDiep = "";

			String MLoi1 = "";
			String MTLoi1 = "";
			String CQT_MLTDiep1 = "";
			
			Node nodeTemp = null;
			NodeList tDiepNodes = (NodeList) xPath.evaluate("DuLieu/TDiep", nodeKetQuaTraCuu, XPathConstants.NODESET);
			for (int i = 0; i < tDiepNodes.getLength(); i++) {
				nodeTemp = (Node) tDiepNodes.item(i);
				checkMLTDiep = commons.getTextFromNodeXML(
						(Element) xPath.evaluate("TTChung/MLTDiep", nodeTemp, XPathConstants.NODE));
				if (xPath.evaluate("DLieu/*/MCCQT", nodeTemp, XPathConstants.NODE) != null
						&& checkMLTDiep.equals("202")) {
					nodeTDiep = nodeTemp;
					break;
				}
			}
			if (nodeTDiep == null) {
				for (int i = 0; i < tDiepNodes.getLength(); i++) {
					nodeTDiep = (Node) tDiepNodes.item(i);
					checkMLTDiep = commons.getTextFromNodeXML(
							(Element) xPath.evaluate("TTChung/MLTDiep", nodeTDiep, XPathConstants.NODE));
					if (checkMLTDiep.equals("202"))
						break;
					if (checkMLTDiep.equals("204")) {
						check_ = true;
						MLoi1 = commons.getTextFromNodeXML((Element) xPath
								.evaluate("DLieu/TBao/DLTBao/LCMa/DSLDo/LDo/MLoi", nodeTDiep, XPathConstants.NODE));
						MTLoi1 = commons.getTextFromNodeXML((Element) xPath
								.evaluate("DLieu/TBao/DLTBao/LCMa/DSLDo/LDo/MTLoi", nodeTDiep, XPathConstants.NODE));

						CQT_MLTDiep1 = checkMLTDiep;
						break;
					}
					if (checkMLTDiep.equals("-1")) {
						check_ = true;
						MLoi1 = commons.getTextFromNodeXML(
								(Element) xPath.evaluate("DLieu/MLoi", nodeTDiep, XPathConstants.NODE));
						MTLoi1 = commons.getTextFromNodeXML(
								(Element) xPath.evaluate("DLieu/MTa", nodeTDiep, XPathConstants.NODE));
						CQT_MLTDiep1 = checkMLTDiep;
						break;
					}
				}
			}
			
//			for (int i = 1; i <= 20; i++) {
//
//				if (xPath.evaluate("DuLieu/TDiep[" + i + "]", nodeKetQuaTraCuu, XPathConstants.NODE) == null)
//					break;
//				nodeTDiep = (Node) xPath.evaluate("DuLieu/TDiep[" + i + "]", nodeKetQuaTraCuu, XPathConstants.NODE);
//				checkMLTDiep = commons.getTextFromNodeXML(
//						(Element) xPath.evaluate("TTChung/MLTDiep", nodeTDiep, XPathConstants.NODE));
//				if (checkMLTDiep.equals("202"))
//					break;
//				if (checkMLTDiep.equals("204")) {
//					check_ = true;
//					MLoi1 = commons.getTextFromNodeXML((Element) xPath.evaluate("DLieu/TBao/DLTBao/LCMa/DSLDo/LDo/MLoi",
//							nodeTDiep, XPathConstants.NODE));
//					MTLoi1 = commons.getTextFromNodeXML((Element) xPath
//							.evaluate("DLieu/TBao/DLTBao/LCMa/DSLDo/LDo/MTLoi", nodeTDiep, XPathConstants.NODE));
//
//					CQT_MLTDiep1 = checkMLTDiep;
//
//				}
//
//			}

			if (nodeTDiep == null) {
				continue;
			}

			CQT_MLTDiep = commons
					.getTextFromNodeXML((Element) xPath.evaluate("TTChung/MLTDiep", nodeTDiep, XPathConstants.NODE));
			MTDTChieu = commons
					.getTextFromNodeXML((Element) xPath.evaluate("TTChung/MTDTChieu", nodeTDiep, XPathConstants.NODE));

			if (check_ == true) {
				MLoi = MLoi1;
				MTLoi = MTLoi1;
				/* LUU LAI FILE XML LOI */
				String dir = docTmp.get("Dir", "");
				String fileName = _id + "_" + CQT_MLTDiep1 + ".xml";
				boolean boo = false;
				try {
					boo = commons.docW3cToFile(rTCTN, dir, fileName);
				} catch (Exception e) {
				}
				if (!boo) {
					continue;
				}

				/* CAP NHAT TRANG THAI COMPLETE - TRANG THAI CQT */
				options = new FindOneAndUpdateOptions();
				options.upsert(false);
				options.maxTime(5000, TimeUnit.MILLISECONDS);
				options.returnDocument(ReturnDocument.AFTER);

	
				 mongoClient = cfg.mongoClient();
					collection = mongoClient.getDatabase(cfg.dbName).getCollection("EInvoice");
					collection.findOneAndUpdate(docFind,
							new Document("$set",
									new Document("EInvoiceStatus", Constants.INVOICE_STATUS.ERROR_CQT)
											.append("CQT_Date", LocalDate.now())
											.append("LDo", new Document("MLoi", MLoi).append("MTLoi", MTLoi))),
							options);		
					mongoClient.close();

//			responseStatus = new MspResponseStatus(0,
//					"".equals(MTLoi) ? "CQT chưa có thông báo kết quả trả về." : MTLoi);
//			rsp.setResponseStatus(responseStatus);
//			return rsp;
			}
			if ("|202|".indexOf("|" + CQT_MLTDiep + "|") == -1) {
				continue;
			}

			String MCCQT = commons
					.getTextFromNodeXML((Element) xPath.evaluate("DLieu/HDon/MCCQT", nodeTDiep, XPathConstants.NODE));

			String dir = docTmp.get("Dir", "");
			String fileName = _id + "_" + MCCQT + ".xml";
			boolean boo = false;
			try {
				boo = commons.docW3cToFile(rTCTN, dir, fileName);
			} catch (Exception e) {
			}
			if (!boo) {
				continue;
			}

			/* CAP NHAT TRANG THAI COMPLETE - TRANG THAI CQT */
			options = new FindOneAndUpdateOptions();
			options.upsert(false);
			options.maxTime(5000, TimeUnit.MILLISECONDS);
			options.returnDocument(ReturnDocument.AFTER);


			 mongoClient = cfg.mongoClient();
				collection = mongoClient.getDatabase(cfg.dbName).getCollection("EInvoice");
				collection.findOneAndUpdate(docFind,
						new Document("$set",
								new Document("EInvoiceStatus", Constants.INVOICE_STATUS.COMPLETE).append("MCCQT", MCCQT)
										.append("MTDTChieu", MTDTChieu).append("CQT_Date", LocalDate.now())
										.append("LDo", new Document("MLoi", "").append("MTLoi", ""))),
						options);		
				mongoClient.close();
				
			// LƯU ID DE CHECK MAIL CHO HOA DON DA PHAT HANH
//		if(listSendMail.equals("")) {
//			listSendMail = _id;
//		}
//		else {
//			listSendMail = listSendMail + _id + ",";	
//		}		

			listMail_.add(_id);
			//
			String iddc = docTmp.getEmbedded(Arrays.asList("EInvoiceDetail", "TTChung", "TTHDLQuan", "_id"), "");
			if (!iddc.equals("")) {
				ObjectId objectIddc = null;
				try {
					objectIddc = new ObjectId(iddc);
				} catch (Exception e) {
				}
	            ObjectId byId =  docTmp.getEmbedded(Arrays.asList("_id"), ObjectId.class);
	            Document docTTHDLQuan = new Document("_id", byId != null ? byId.toHexString():"")
	                    .append("TCHDon", docTmp.getEmbedded(Arrays.asList("EInvoiceDetail", "TTChung", "TTHDLQuan", "TCHDon"), ""))
	                    .append("LHDCLQuan", "1")
	                    .append("KHMSHDCLQuan", docTmp.getEmbedded(Arrays.asList("EInvoiceDetail", "TTChung","KHMSHDon"), ""))
	                    .append("KHHDCLQuan", docTmp.getEmbedded(Arrays.asList("EInvoiceDetail", "TTChung", "KHHDon"), ""))
	                    .append("SHDCLQuan",
	                            String.valueOf(docTmp.getEmbedded(Arrays.asList("EInvoiceDetail", "TTChung", "SHDon"), 0)))
	                    .append("NLHDCLQuan", commons.convertLocalDateTimeToString(
	                            commons.convertDateToLocalDateTime(
	                                    docTmp.getEmbedded(Arrays.asList("EInvoiceDetail", "TTChung", "NLap"), Date.class)),
	                            "yyyy-MM-dd"))
	                    .append("GChu", "");
	            

				Document docFind1 = new Document("IssuerId", header.getIssuerId()).append("IsDelete", false)
						.append("_id", objectIddc).append("SignStatusCode", Constants.INVOICE_SIGN_STATUS.SIGNED)
						.append("EInvoiceStatus", new Document("$in", Arrays.asList(Constants.INVOICE_STATUS.COMPLETE,
								Constants.INVOICE_STATUS.ADJUSTED, Constants.INVOICE_STATUS.REPLACED)));
				options = new FindOneAndUpdateOptions();
				options.upsert(true);
				options.maxTime(5000, TimeUnit.MILLISECONDS);
				options.returnDocument(ReturnDocument.AFTER);

				if ("1".equals(
						docTmp.getEmbedded(Arrays.asList("EInvoiceDetail", "TTChung", "TTHDLQuan", "TCHDon"), ""))) {
				
					 mongoClient = cfg.mongoClient();
						collection = mongoClient.getDatabase(cfg.dbName).getCollection("EInvoice");
						collection.findOneAndUpdate(docFind1,
//								new Document("$set", new Document("EInvoiceStatus", "REPLACED")), options);		
		                        new Document("$set", new Document("EInvoiceStatus", "REPLACED").append("ReplacedBy", docTTHDLQuan)), options);
						mongoClient.close();
						
						
				} else if ("2".equals(
						docTmp.getEmbedded(Arrays.asList("EInvoiceDetail", "TTChung", "TTHDLQuan", "TCHDon"), ""))) {
			
					MongoClient mongoClient2 = cfg.mongoClient();
						collection = mongoClient2.getDatabase(cfg.dbName).getCollection("EInvoice");
						collection.findOneAndUpdate(docFind1,
								new Document("$set", new Document("EInvoiceStatus", "ADJUSTED")), options);	
		                collection.findOneAndUpdate(docFind1,
		                        new Document("$push", new Document("AdjustedBy", docTTHDLQuan)), options);
						mongoClient2.close();
						
				}
			}
// 		}catch(Exception ex) {}
		}

		// String a = listSendMail;

		responseStatus = new MspResponseStatus(0, listMail_.toString());
		rsp.setResponseStatus(responseStatus);
		return rsp;
	}

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
		String _id = "";
		String _ListId = commons.getTextJsonNode(jsonData.at("/_id")).replaceAll("\\s", "");

		byte[] decodedBytes = Base64.getDecoder().decode(_ListId);
		String decodedString = new String(decodedBytes);
		String check = decodedString.trim().replace("[", "").trim().replace("]", "");

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
//		MapMessage mapMessage = null;	

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
	
		String[] split_ = check.split(",");

		String uidTmp = commons.convertLocalDateTimeToString(LocalDateTime.now(),
				Constants.FORMAT_DATE.FORMAT_DATE_TIME_YYYYMMDD_HHMMSS) + "-" + commons.csRandomAlphaNumbericString(10);
		String ApiKey = "";
		String NBanTen = "";
		String SecretKey = "";
		String EmailAddress = "";
		String MailJet = "";
		String check_mail = "";
		MailConfig mailConfig = null;
		for (String a : split_) {

			_id = a.trim().replace(" ", "").trim().replaceAll("\"", "");
			ObjectId objectId = null;
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


			MongoClient mongoClient = cfg.mongoClient();
			MongoCollection<Document> collection = mongoClient.getDatabase(cfg.dbName).getCollection("EInvoice");
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
			
			if(!commons.isValidEmailAddress(docTmp.getEmbedded(Arrays.asList("EInvoiceDetail", "NDHDon", "NMua", "DCTDTu"), ""))) {
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

			if(!NBanTen.equals("")) {
			if (MailJet.equals("Y") && !MailJet.equals("") && !MailJet.equals("N")  ) {
				check_mail = "MailJet";
			} else {
				check_mail = "MailServer";
			}

			Document docInsert = new Document("IssuerId", header.getIssuerId()).append("InfoServerID", uidTmp)
					.append("MailUsingType", check_mail).append("TaxCode", TaxCode).append("Name", Name)
					.append("Data", docTmp).append("FuncSend", Constants.SendMailFromFunc.Bulk).append("InfoCreated",
							new Document("CreateDate", LocalDateTime.now()).append("CreateUserID", header.getUserId())
									.append("CreateUserName", header.getUserName())
									.append("CreateUserFullName", header.getUserFullName()));
			 mongoClient = cfg.mongoClient();
				collection = mongoClient.getDatabase(cfg.dbName).getCollection("LogBulkEMail");
				collection.insertOne(docInsert);			
				mongoClient.close();
				
		}}

		if (!MailJet.equals("") && mailConfig != null && !NBanTen.equals("")) {

			if (check_mail.equals("MailJet")) {

				
				MongoClient mongoClient = cfg.mongoClient();
				MongoCollection<Document> collection = mongoClient.getDatabase(cfg.dbName).getCollection("LogBulkEMailInfoServer");
					collection.insertOne(new Document("IssuerId", header.getIssuerId()).append("InfoServerID", uidTmp)
							.append("MailUsingType", "MailJet").append("MailjetInfo",
									new Document("ApiKey", ApiKey).append("SecretKey", SecretKey)
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
					collection.insertOne(new Document("IssuerId", header.getIssuerId()).append("InfoServerID", uidTmp)
							.append("MailUsingType", check_mail)
							.append("MailServer", new Document("SmtpServer", mailConfig.getSmtpServer())
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
		responseStatus = new MspResponseStatus(0, "SUCCESS");
		rsp.setResponseStatus(responseStatus);
		return rsp;
	}

	@Override
	public MsgRsp checkHistoryMST(JSONRoot jsonRoot) throws Exception {
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

//		String actionCode = header.getActionCode();
		String mst = commons.getTextJsonNode(jsonData.at("/MST")).replaceAll("\\s", "");

		MsgRsp rsp = new MsgRsp(header);
		rsp.setMsgPage(page);
		MspResponseStatus responseStatus = null;

		Document docFind = null;
		Document docTmp = null;

		/* KIEM TRA THONG TIN KHACH HANG - USERS */

		docFind = new Document("IssuerId", header.getIssuerId()).append("TaxCode", mst).append("IsDelete", false);

		MongoClient mongoClient = cfg.mongoClient();
		MongoCollection<Document> collection = mongoClient.getDatabase(cfg.dbName).getCollection("DMHistoryCustomer");
		try {
			docTmp = collection.find(docFind).allowDiskUse(true).iterator().next();
		} catch (Exception e) {

		}

		mongoClient.close();

		if (null == docTmp) {
			responseStatus = new MspResponseStatus(9999, "Không tìm thấy thông tin khách hàng.");
			rsp.setResponseStatus(responseStatus);
			return rsp;
		}

		String ma_kh = docTmp.getString("CustomerCode");
		String hvtnmh = docTmp.getString("CustomerName");
		String tendv = docTmp.getString("CompanyName");
		String dchi = docTmp.getString("Address");
		String email = docTmp.getString("Email");
		String sdt = docTmp.getString("Phone");
		String emailcc = docTmp.getString("EmailCC");

		String TH = ma_kh + ";" + hvtnmh + ";" + tendv + ";" + emailcc + ";" + email + ";" + sdt + ";" + dchi;
		responseStatus = new MspResponseStatus(0, TH);
		rsp.setResponseStatus(responseStatus);
		return rsp;

	}

	/*------------ Start Import excel */
	@SuppressWarnings({ "unlikely-arg-type", "unused" })
	@Override
	public MsgRsp importExcelMisa(JSONRoot jsonRoot) throws Exception {
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
		objectId = null;
		objectIdUser = null;
		objectIdMSKH = null;
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

		Document findInforIssuer = new Document("_id", 1)
				.append("TaxCode", 1)
				.append("Name", 1)
				.append("Address", 1)
				.append("Phone", 1)
				.append("Fax", 1)
				.append("Email", 1)
				.append("Website", 1)
				.append("TinhThanhInfo", 1)
				.append("ChiCucThueInfo", 1)
				.append("BankAccount", 1)
				.append("NameEN", 1)
				.append("BankAccountExt", 1);
		
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
		List<EInvoiceExcelFormMISA> eInvoiceExcelFormList = new ArrayList<>();
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
				int lastColumn = Math.max(row1.getLastCellNum(), 13);

				for (int cn = 0; cn < lastColumn; cn++) {
					Cell c = row1.getCell(cn, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
					cells.add(c);
				}
				EInvoiceExcelFormMISA eInvoiceExcelForm = extractInfoFromCellMISA(cells);
				eInvoiceExcelFormList.add(eInvoiceExcelForm);

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
		if (eInvoiceExcelFormList != null) {
			for (int tam = 0; tam < eInvoiceExcelFormList.size(); tam++) {
				if (eInvoiceExcelFormList.get(tam).getSHD() == null) {
					checkNullMaHD = true;
				}
			}
			if (checkNullMaHD == true) {
				responseStatus = new MspResponseStatus(999,
						"Import không thành công. \r\n" + "Hãy kiểm tra lại cột số hoá đơn trong file excel.");
				rsp.setResponseStatus(responseStatus);
				return rsp;
			}
			String tempTen = "";
			String tempMST = "";
			String tempDChi = "";
			String tempSDThoai = "";
			String tempDCTDTu = "";
			String tempMAKH = "";
			String tempSTKNHang = "";
			String tempTNHang = "";
			String tempHVTNMHang = "";
//			Double tempTgTCThue = 0.0;
//			Double tempTgTThue = 0.0;
//			Double tempTgTTTBSo = 0.0;
//			String tempTgTTTBChu = "";
//			String tempTyGia = "";
//			String tempHTTToan = "";
			List<DSHHDVu> dshhdVuList = new ArrayList<>();
			List<Object> listHHDVu = new ArrayList<Object>();
			int i = 0;
			int start = 0;
			int end = 0;
			int dem = 0;

			/* DOC FILE EXCEL - GHI DU LIEU VO LIST */
			for (; i < eInvoiceExcelFormList.size();) {
				dem = 0;
				for (int j = i; j < eInvoiceExcelFormList.size(); j++) {
					if (eInvoiceExcelFormList.get(i).getSHD() == eInvoiceExcelFormList.get(j).getSHD()) {
						dem++;
						start = j + 1;
						tempHVTNMHang = "";
						tempMST = "";
						tempTen = "";
						if (eInvoiceExcelFormList.get(i).getMST() == null) {
							tempHVTNMHang = eInvoiceExcelFormList.get(i).getTenNguoiMua();
						} else {
							tempMST = eInvoiceExcelFormList.get(i).getMST();
							tempTen = eInvoiceExcelFormList.get(i).getTenNguoiMua();
						}

						tempDChi = eInvoiceExcelFormList.get(i).getAddress();
//						tempHTTToan = eInvoiceExcelFormList.get(i).getHinhThucThanhToan();
//						tempTyGia = eInvoiceExcelFormList.get(i).getTyGia();
						end = j;
						if (eInvoiceExcelFormList.size() == j + 1) {
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
				String TenForm = tempTen;
				String MaSoThueForm = tempMST;
				String DChiNMForm = tempDChi;
				String SDThoaiNMForm = tempSDThoai;
				String DCTDTuNMForm = tempDCTDTu;
				String MaKHNMForm = tempMAKH;
				String STKNHangNMForm = tempSTKNHang;
				String TNHangNMForm = tempTNHang;
				String HVTNMHangNMForm = tempHVTNMHang;
//				Double tempTgTCThueForm = tempTgTCThue;
//				Double tempTgTThueForm = tempTgTThue;
//				Double tempTgTTTBSoForm = tempTgTTTBSo;
//				String tempTgTTTBChuForm = tempTgTTTBChu;
//				String TyGiaForm = tempTyGia;
				// String HTTToanForm = tempHTTToan;
				String HTTToanForm = "3";
				if (checkMaHD == true) {
					if (dem > 1) {

						int a = 1;
						int stt = 0;
						int TT = 0;
						Double tong = 0.0;
						Double tthue = 0.0;
						Double Tongtien = 0.0;
						Double TongTienTThue = 0.0;
						Double TongTienThue = 0.0;
						Double TTDCThue = 0.0;
						Double Total = 0.0;
						for (int k = i; k <= end; k++) {
							DSHHDVu dshhdVu = new DSHHDVu();
							TT = stt + a;
							String STT = String.valueOf(TT);
							tong = eInvoiceExcelFormList.get(k).getThanhTien();
							tthue = eInvoiceExcelFormList.get(k).getTienThue();
							Total = tong + tthue;
							Tongtien = Tongtien + Total;
							TongTienTThue = TongTienTThue + tong;
							TongTienThue = TongTienThue + tthue;
							dshhdVu.setSTT(STT);
							dshhdVu.setProductName(eInvoiceExcelFormList.get(k).getTenHangHoa());
							dshhdVu.setProductCode(eInvoiceExcelFormList.get(k).getMaHangHoa());
							dshhdVu.setUnit(eInvoiceExcelFormList.get(k).getDonViTinh());
							dshhdVu.setQuantity(eInvoiceExcelFormList.get(k).getSoLuong());
							dshhdVu.setPrice(eInvoiceExcelFormList.get(k).getDonGia());
							dshhdVu.setTotal(eInvoiceExcelFormList.get(k).getThanhTien());
							dshhdVu.setVATRate(eInvoiceExcelFormList.get(k).getThueSuat());
							dshhdVu.setVATAmount(eInvoiceExcelFormList.get(k).getTienThue());
							dshhdVu.setAmount(Tongtien);
							// String TinhChat = eInvoiceExcelFormList.get(k).getTinhChat();
							if (Tongtien == 0.0) {
								dshhdVu.setFeature("4");
							} else {
								dshhdVu.setFeature("1");
							}

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
							double checkVAT = (dshhdVu.getVATAmount() * 100) / dshhdVu.getTotal();
							double result = Math.round(checkVAT);
							hItem1.put("VATRate", result);
							hItem1.put("VATAmount", dshhdVu.getVATAmount());
							hItem1.put("Amount", dshhdVu.getAmount());
							hItem1.put("Feature", dshhdVu.getFeature());
							listHHDVu.add(hItem1);
							stt++;
							Tongtien = 0.0;

						}
						// Clear BIEN GAN STT
						stt = stt - (end + 1);

						String TTTien = "";
						Double TTT = 0.0;
						String TTTTien = "";
						Double TTTT = 0.0;
						TTTTien = String.format("%.0f", TongTienTThue);
						TTTT = Double.parseDouble(TTTTien);
						TTTien = String.format("%.0f", TongTienThue);
						TTT = Double.parseDouble(TTTien);

						TTDCThue = TTTT + TTT;
						String TTBChu = commons.formatNumberReal(TTDCThue).replaceAll(",", "");
						String TTBCHU = ChuyenSangChu(TTBChu);
						String TongTienBangchu = TTBCHU.substring(0, 1).toUpperCase() + TTBCHU.substring(1) + ".";

						// Thông tin hóa đơn - TTChung
						String MaHD = eInvoiceExcelFormList.get(i).getSHD();
						String THDon = "Hóa đơn giá trị gia tăng TT 78";
						String MauSoHD = mauSoHdon;
						String KHMSHDon = docTmp.getEmbedded(Arrays.asList("DMMauSoKyHieu", "KHMSHDon"), "").toString();
						String KHHDon = docTmp.getEmbedded(Arrays.asList("DMMauSoKyHieu", "KHHDon"), "").toString();
						LocalDateTime NLap = LocalDateTime.now();
//						String DVTTe = eInvoiceExcelFormList.get(i).getLoaiTien();
//						String TGia = TyGiaForm;
						String DVTTe = "VND";
						String TGia = "1";
						String HTTToanCode = "";
						String HTTToan1 = "";
						switch (HTTToanForm) {
						case "1":
							HTTToanCode = "1";
							HTTToan1 = "Tiền mặt";
							break;
						case "2":
							HTTToanCode = "2";
							HTTToan1 = "Chuyển khoản";
							break;
						case "3":
							HTTToanCode = "3";
							HTTToan1 = "Tiền mặt/Chuyển khoản";
							break;
						case "4":
							HTTToanCode = "4";
							HTTToan1 = "Đối trừ công nợ";
							break;
						case "5":
							HTTToanCode = "5";
							HTTToan1 = "Không thu tiền";
							break;
						default:
							break;
						}
						String HTTToan = HTTToan1;

						// Thông tin người bán - Thông tin người mua - NDHDon
						String Ten = docTmp.getEmbedded(Arrays.asList("Name"), "").toString();
						String MST = docTmp.getEmbedded(Arrays.asList("TaxCode"), "").toString();
						String DChi = docTmp.getEmbedded(Arrays.asList("Address"), "").toString();

						String SDThoai = docTmp.getEmbedded(Arrays.asList("Phone"), "").toString();
						String DCTDTu = "";

						String STKNHang = docTmp.getEmbedded(Arrays.asList("BankAccount", "AccountNumber"), "")
								.toString();
						String TNHang = docTmp.getEmbedded(Arrays.asList("BankAccount", "BankName"), "").toString();
						String Fax = docTmp.getEmbedded(Arrays.asList("Fax"), "").toString();
						String Website = docTmp.getEmbedded(Arrays.asList("Website"), "").toString();

						String TenNM = TenForm;
						String MSTNM = MaSoThueForm;
						String DChiNM = DChiNMForm;
						String SDThoaiNM = SDThoaiNMForm;
						String DCTDTuNM = DCTDTuNMForm;
						String MaKHangNM = MaKHNMForm;
						String STKNHangNM = STKNHangNMForm;
						String TNHangNM = TNHangNMForm;
						String HVTNMHangNM = HVTNMHangNMForm;

						// Một số thông tin khác
						String SignStatusCode = "NOSIGN";
						String EInvoiceStatus = "CREATED";
						String MTDiep = "";
						String SecureKey = "";
						String Dir = "";
						// Setting
						String codeMTD = "0315382923";
						String FileNameXML = "";
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

						Dir = pathDir;
						MTDiep = codeMTD + commons.csRandomAlphaNumbericString(46 - codeMTD.length()).toUpperCase();
						SecureKey = commons.csRandomNumbericString(6);
						// XML
						FileNameXML = fileNameXML;
						dbf = DocumentBuilderFactory.newInstance();
						db = dbf.newDocumentBuilder();
						doc = db.newDocument();
						doc.setXmlStandalone(true);

						root = doc.createElement("HDon");
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
																											// KHI KY
						elementSubContent.appendChild(commons.createElementWithValue(doc, "MHSo", ""));
						// Ngày lập
						elementSubContent.appendChild(
								commons.createElementWithValue(doc, "NLap", NLap.format(DateTimeFormatter.ISO_DATE)));
						// Số bảng kê
						elementSubContent.appendChild(commons.createElementWithValue(doc, "SBKe", ""));
						// Ngày bảng kê
						elementSubContent.appendChild(commons.createElementWithValue(doc, "NBKe", ""));
						// Đơn vị tiền tệ
						elementSubContent.appendChild(commons.createElementWithValue(doc, "DVTTe", DVTTe));
						// Tỷ giá
						elementSubContent.appendChild(commons.createElementWithValue(doc, "TGia", TGia));
						// Hình thức thanh toán
						elementSubContent.appendChild(commons.createElementWithValue(doc, "HTTToan", HTTToan));
						// MST tổ chức cung cấp giải pháp HĐĐT
						elementSubContent
								.appendChild(commons.createElementWithValue(doc, "MSTTCGP", SystemParams.MSTTCGP));
						// MST đơn vị nhận ủy nhiệm lập hóa đơn
						elementSubContent.appendChild(commons.createElementWithValue(doc, "MSTDVNUNLHDon", ""));
						// Tên đơn vị nhận ủy nhiệm lập hóa đơn
						elementSubContent.appendChild(commons.createElementWithValue(doc, "TDVNUNLHDon", ""));
						// Địa chỉ đơn vị nhận ủy nhiệm lập hóa đơn
						elementSubContent.appendChild(commons.createElementWithValue(doc, "DCDVNUNLHDon", ""));
						elementTmp = doc.createElement("TTKhac"); // THONG TIN KHAC
						elementTmp.appendChild(commons.createElementTTKhac(doc, "PortalLink", "string", link));
						elementTmp.appendChild(commons.createElementTTKhac(doc, "SecureKey", "string", SecureKey));
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
//	            			elementTmp.appendChild(commons.createElementWithValue(doc, "TTKhac", ""));

						/* ADD THONG TIN TK NGAN HANG (NEU CO) */
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
						elementTmp.appendChild(commons.createElementWithValue(doc, "Ten", TenNM));
						elementTmp.appendChild(commons.createElementWithValue(doc, "MST", MSTNM));
						elementTmp.appendChild(commons.createElementWithValue(doc, "DChi", DChiNM));
						elementTmp.appendChild(commons.createElementWithValue(doc, "MKHang", MaKHangNM));
						elementTmp.appendChild(commons.createElementWithValue(doc, "SDThoai", SDThoaiNM));
						elementTmp.appendChild(commons.createElementWithValue(doc, "DCTDTu", DCTDTuNM));
						elementTmp.appendChild(commons.createElementWithValue(doc, "HVTNMHang", HVTNMHangNM));
						elementTmp.appendChild(commons.createElementWithValue(doc, "STKNHang", STKNHangNM));
						elementTmp.appendChild(commons.createElementWithValue(doc, "TNHang", TNHangNM));
						elementTmp.appendChild(commons.createElementWithValue(doc, "TTKhac", ""));
						elementSubContent.appendChild(elementTmp);

						mapVATAmount = new LinkedHashMap<String, Double>();
						mapAmount = new LinkedHashMap<String, Double>();
						elementTmp = doc.createElement("DSHHDVu"); // HH-DV
						// dshhdVuList, listHHDVu, \VATRate,
						// Json.serializer().nodeFromObject(msg.getObjData());

						for (Object o : listHHDVu) {
							if (!"".equals(o.equals("/ProductName"))) {
								JsonNode h = Json.serializer().nodeFromObject(o);
								tmp = commons.formatNumberReal(commons.getTextJsonNode(h.at("/VATRate")))
										.replaceAll(",", "");
								switch (tmp) {
								case "0":
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
								Boolean slo1 = false;
								Boolean hsd1 = false;
								String slo = commons.getTextJsonNode(h.at("/SLo"));
								String hsd = commons.getTextJsonNode(h.at("/HanSD"));
								if ("".equals(slo)) {
									slo1 = true;
								}
								if ("".equals(hsd)) {
									hsd1 = true;
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
								if (slo1 == false) {
									elementSubTmp.appendChild(commons.createElementWithValue(doc, "SLo",
											commons.getTextJsonNode(h.at("/SLo"))));
								}
								if (hsd1 == false) {
									elementSubTmp.appendChild(commons.createElementWithValue(doc, "HanSD",
											commons.getTextJsonNode(h.at("/HanSD"))));
								}

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
									elementSubTmp.appendChild(commons.createElementWithValue(doc, "TSuat", tmp));
								}

								elementSubTmp01 = doc.createElement("TTKhac");
								elementSubTmp01.appendChild(commons.createElementTTKhac(doc, "VATAmount", "decimal",
										commons.formatNumberReal(commons.getTextJsonNode(h.at("/VATAmount")))
												.replaceAll(",", "")));
								elementSubTmp01.appendChild(commons.createElementTTKhac(doc, "Amount", "decimal",
										commons.formatNumberReal(commons.getTextJsonNode(h.at("/Amount")))
												.replaceAll(",", "")));
								elementSubTmp.appendChild(elementSubTmp01);
								elementTmp.appendChild(elementSubTmp);

							}
						}
						elementSubContent.appendChild(elementTmp);

						elementTmp = doc.createElement("TToan"); // Thong tin thanh toan
						elementSubTmp = doc.createElement("THTTLTSuat");
						/* DANH SACH CAC LOAI THUE SUAT */

						// https: //
						// stackoverflow.com/questions/46898/how-do-i-efficiently-iterate-over-each-entry-in-a-java-map
						for (Map.Entry<String, Double> pair : mapVATAmount.entrySet()) {
							if (null != pair.getKey() && !"".equals(pair.getKey())) {
								elementSubTmp01 = doc.createElement("LTSuat");
								elementSubTmp01
										.appendChild(commons.createElementWithValue(doc, "TSuat", pair.getKey()));
								elementSubTmp01.appendChild(commons.createElementWithValue(doc, "ThTien",
										commons.formatNumberReal(mapAmount.get(pair.getKey())).replaceAll(",", "")));
								elementSubTmp01.appendChild(commons.createElementWithValue(doc, "TThue",
										String.format("%.0f", mapVATAmount.get(pair.getKey()))));
								elementSubTmp.appendChild(elementSubTmp01);
							}
						}
						elementTmp.appendChild(elementSubTmp);

						elementTmp.appendChild(commons.createElementWithValue(doc, "TgTCThue",
								commons.formatNumberReal(TTTT).replaceAll(",", "")));
						elementTmp.appendChild(commons.createElementWithValue(doc, "TgTThue",
								commons.formatNumberReal(TTT).replaceAll(",", "")));

						elementSubTmp = doc.createElement("DSLPhi");
						elementSubTmp01 = doc.createElement("LPhi");
						elementSubTmp01.appendChild(commons.createElementWithValue(doc, "TLPhi", ""));
						elementSubTmp01.appendChild(commons.createElementWithValue(doc, "TPhi", "0"));
						elementSubTmp.appendChild(elementSubTmp01);

						elementTmp.appendChild(elementSubTmp);

						elementTmp.appendChild(commons.createElementWithValue(doc, "TTCKTMai", "0"));
						elementTmp.appendChild(commons.createElementWithValue(doc, "TgTTTBSo",
								commons.formatNumberReal(TTDCThue).replaceAll(",", "")));
						elementTmp.appendChild(commons.createElementWithValue(doc, "TgTTTBChu", TongTienBangchu));
						elementSubTmp = doc.createElement("TTKhac");
						elementTmp.appendChild(elementSubTmp);

						elementSubContent.appendChild(elementTmp);

						elementContent.appendChild(elementSubContent);
						// END - NDHDon: Nội dung hóa đơn

						isSdaveFile = commons.docW3cToFile(doc, Dir, FileNameXML);
						if (!isSdaveFile) {
							throw new Exception("Lưu dữ liệu không thành công.");
						}
						/* END - TAO XML HOA DON */
						// END XML"_id", objectIdEInvoice
						// lookup data
						docUpsert = new Document("_id", objectIdEInvoice).append("IssuerId", header.getIssuerId())
								.append("MTDiep", MTDiep)
								.append("EInvoiceDetail", new Document("TTChung",
										new Document("THDon", THDon).append("MaHD", MaHD).append("MauSoHD", MauSoHD)
												.append("KHMSHDon", KHMSHDon).append("KHHDon", KHHDon)
												.append("NLap", NLap).append("DVTTe", DVTTe).append("TGia", TGia)
												.append("HTTToanCode", HTTToanCode).append("HTTToan", HTTToan))
										.append("NDHDon",
												new Document("NBan",
														new Document("Ten", Ten).append("MST", MST).append("DChi", DChi)
																.append("SDThoai", SDThoai).append("DCTDTu", DCTDTu)
																.append("STKNHang", STKNHang).append("TNHang", TNHang)
																.append("Fax", Fax).append("Website", Website))
														.append("NMua", new Document("Ten", TenNM).append("MST", MSTNM)
																.append("DChi", DChiNM).append("MKHang", MaKHangNM)
																.append("SDThoai", SDThoaiNM).append("DCTDTu", DCTDTuNM)
																.append("HVTNMHang", HVTNMHangNM)
																.append("STKNHang", STKNHangNM)
																.append("TNHang", TNHangNM)))

										.append("DSHHDVu", listHHDVu)
										.append("TToan", new Document("TgTCThue", TTTT).append("TgTThue", TTT)
												.append("TgTTTBSo", TTDCThue).append("TgTTTBChu", TongTienBangchu)))
								.append("SignStatusCode", SignStatusCode).append("EInvoiceStatus", EInvoiceStatus)
								.append("IsDelete", false).append("SecureKey", SecureKey).append("Dir", Dir)
								.append("FileNameXML", FileNameXML).append("InfoCreated",
										new Document("CreateDate", LocalDateTime.now())
												.append("CreateUserID", header.getUserId())
												.append("CreateUserName", header.getUserName())
												.append("CreateUserFullName", header.getUserFullName()));
					
						 mongoClient = cfg.mongoClient();
							collection = mongoClient.getDatabase(cfg.dbName).getCollection("EInvoice");
							collection.insertOne(docUpsert);			
							mongoClient.close();
						dshhdVuList.clear();
						listHHDVu.clear();
						checkMaHD = false;
					} else {
						int a = 1;
						int stt = 0;
						int TT = 0;
						Double tong = 0.0;
						Double tthue = 0.0;
						Double Tongtien = 0.0;
						Double TongTienTThue = 0.0;
						Double TongTienThue = 0.0;
						Double TTDCThue = 0.0;
						Double Total = 0.0;
						if (dem == 1) {
							int k = i;
							DSHHDVu dshhdVu = new DSHHDVu();
							TT = stt + a;
							String STT = String.valueOf(TT);
							tong = eInvoiceExcelFormList.get(k).getThanhTien();
							tthue = eInvoiceExcelFormList.get(k).getTienThue();
							Total = tong + tthue;
							Tongtien = Tongtien + Total;
							TongTienTThue = TongTienTThue + tong;
							TongTienThue = TongTienThue + tthue;
							dshhdVu.setSTT(STT);
							dshhdVu.setProductName(eInvoiceExcelFormList.get(k).getTenHangHoa());
							dshhdVu.setProductCode(eInvoiceExcelFormList.get(k).getMaHangHoa());
							dshhdVu.setUnit(eInvoiceExcelFormList.get(k).getDonViTinh());
							dshhdVu.setQuantity(eInvoiceExcelFormList.get(k).getSoLuong());
							dshhdVu.setPrice(eInvoiceExcelFormList.get(k).getDonGia());
							dshhdVu.setTotal(eInvoiceExcelFormList.get(k).getThanhTien());
							dshhdVu.setVATRate(eInvoiceExcelFormList.get(k).getThueSuat());
							dshhdVu.setVATAmount(eInvoiceExcelFormList.get(k).getTienThue());
							dshhdVu.setAmount(Tongtien);

							if (Tongtien == 0.0) {
								dshhdVu.setFeature("4");
							} else {
								dshhdVu.setFeature("1");
							}

							dshhdVuList.add(dshhdVu);
							List<Object> listHHDVus = new ArrayList<Object>();
							HashMap<String, Object> hItem1 = null;
							hItem1 = new LinkedHashMap<String, Object>();
							hItem1.put("STT", dshhdVu.getSTT());
							hItem1.put("ProductName", dshhdVu.getProductName());
							hItem1.put("ProductCode", dshhdVu.getProductCode());
							hItem1.put("Unit", dshhdVu.getUnit());
							hItem1.put("Quantity", dshhdVu.getQuantity());
							hItem1.put("Price", dshhdVu.getPrice());
							hItem1.put("Total", dshhdVu.getTotal());

							double checkVAT = (dshhdVu.getVATAmount() * 100) / dshhdVu.getTotal();
							double result = Math.round(checkVAT);
							hItem1.put("VATRate", result);
							hItem1.put("VATAmount", dshhdVu.getVATAmount());
							hItem1.put("Amount", dshhdVu.getAmount());
							hItem1.put("Feature", dshhdVu.getFeature());
							listHHDVus.add(hItem1);

							String TTTien = "";
							Double TTT = 0.0;
							String TTTTien = "";
							Double TTTT = 0.0;
							TTTTien = String.format("%.0f", TongTienTThue);
							TTTT = Double.parseDouble(TTTTien);
							TTTien = String.format("%.0f", TongTienThue);
							TTT = Double.parseDouble(TTTien);

							TTDCThue = TTTT + TTT;
							String TTBChu = commons.formatNumberReal(TTDCThue).replaceAll(",", "");
							String TTBCHU = ChuyenSangChu(TTBChu);
							String TongTienBangchu = TTBCHU.substring(0, 1).toUpperCase() + TTBCHU.substring(1) + ".";
							// Thông tin hóa đơn - TTChung
							String MaHD = eInvoiceExcelFormList.get(i).getSHD();
							String THDon = "Hóa đơn giá trị gia tăng TT 78";
							String MauSoHD = mauSoHdon;
							String KHMSHDon = docTmp.getEmbedded(Arrays.asList("DMMauSoKyHieu", "KHMSHDon"), "")
									.toString();
							String KHHDon = docTmp.getEmbedded(Arrays.asList("DMMauSoKyHieu", "KHHDon"), "").toString();
							LocalDateTime NLap = LocalDateTime.now();
//							String DVTTe = eInvoiceExcelFormList.get(i).getLoaiTien();
//							String TGia = TyGiaForm;
							String DVTTe = "VND";
							String TGia = "1";
							String HTTToanCode = "";
							String HTTToan1 = "";
							switch (HTTToanForm) {
							case "1":
								HTTToanCode = "1";
								HTTToan1 = "Tiền mặt";
								break;
							case "2":
								HTTToanCode = "2";
								HTTToan1 = "Chuyển khoản";
								break;
							case "3":
								HTTToanCode = "3";
								HTTToan1 = "Tiền mặt/Chuyển khoản";
								break;
							case "4":
								HTTToanCode = "4";
								HTTToan1 = "Đối trừ công nợ";
								break;
							case "5":
								HTTToanCode = "5";
								HTTToan1 = "Không thu tiền";
								break;
							default:
								break;
							}
							String HTTToan = HTTToan1;
//	                            TTChung ttChung = new TTChung(MaHD,MaKH,THDon, MauSoHD, KHMSHDon, KHHDon, NLap, DVTTe, TGia, HTTToanCode, HTTToan);

							// Thông tin người bán - Thông tin người mua - NDHDon
							String Ten = docTmp.getEmbedded(Arrays.asList("Name"), "").toString();
							String MST = docTmp.getEmbedded(Arrays.asList("TaxCode"), "").toString();
							String DChi = docTmp.getEmbedded(Arrays.asList("Address"), "").toString();
//							String MKHang = "";
							String SDThoai = docTmp.getEmbedded(Arrays.asList("Phone"), "").toString();
							String DCTDTu = "";
//							String HVTNMHang = "";
							String STKNHang = docTmp.getEmbedded(Arrays.asList("BankAccount", "AccountNumber"), "")
									.toString();
							String TNHang = docTmp.getEmbedded(Arrays.asList("BankAccount", "BankName"), "").toString();
							String Fax = docTmp.getEmbedded(Arrays.asList("Fax"), "").toString();
							String Website = docTmp.getEmbedded(Arrays.asList("Website"), "").toString();

							String TenNM = TenForm;
							String MSTNM = MaSoThueForm;
							String DChiNM = DChiNMForm;
							String SDThoaiNM = SDThoaiNMForm;
							String DCTDTuNM = DCTDTuNMForm;
							String MaKHangNM = MaKHNMForm;
							String STKNHangNM = STKNHangNMForm;
							String TNHangNM = TNHangNMForm;
							String HVTNMHangNM = HVTNMHangNMForm;

//							NBan nBan = new NBan(Ten, MST, DChi, SDThoai, DCTDTu, STKNHang, TNHang, Fax, Website);
//	                            NMua nMua = new NMua(TenNM, MSTNM, DChiNM, SDThoaiNM, DCTDTuNM,MaKHang, STKNHangNM, TNHangNM, HVTNMHangNM);

//	                            NDHDon ndhDon = new NDHDon(nBan, nMua);

							// Thông tin thanh toán
//							Double TgTCThue = tempTgTCThueForm;
//							Double TgTThue = tempTgTThueForm;
//							Double TgTTTBSo = tempTgTTTBSoForm;
//							String TgTTTBChu = tempTgTTTBChuForm;
//							TToan tToan = new TToan(TgTCThue, TgTThue, TgTTTBSo, TgTTTBChu);

							// Một số thông tin khác
							String SignStatusCode = "NOSIGN";

							String EInvoiceStatus = "CREATED";

							Boolean IsDelete = false;

							String MTDiep = "";

							String SecureKey = "";

							String Dir = "";

							// Setting
							String codeMTD = "0315382923";
							String FileNameXML = "";
							String pathDir = "";
							File file1 = null;
							Path path1 = null;
							ObjectId objectIdEInvoice = null;
							String fileNameXML = "";
							objectIdEInvoice = new ObjectId();
							String taxCode = "";
							taxCode = docTmp.getString("TaxCode");
							path1 = Paths.get(SystemParams.DIR_E_INVOICE_DATA, taxCode, docTmp
									.getEmbedded(Arrays.asList("DMMauSoKyHieu", "_id"), ObjectId.class).toString());
							pathDir = path1.toString();
							file1 = path1.toFile();
							if (!file1.exists())
								file1.mkdirs();
							fileNameXML = objectIdEInvoice.toString() + ".xml";
							pathDir = path1.toString();
							file1 = path1.toFile();
							if (!file1.exists())
								file1.mkdirs();
							Dir = pathDir;
							MTDiep = codeMTD + commons.csRandomAlphaNumbericString(46 - codeMTD.length()).toUpperCase();
							SecureKey = commons.csRandomNumbericString(6);
							// XML
							FileNameXML = fileNameXML;
							dbf = DocumentBuilderFactory.newInstance();
							db = dbf.newDocumentBuilder();
							doc = db.newDocument();
							doc.setXmlStandalone(true);

							root = doc.createElement("HDon");
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
																												// SINH
																												// KHI
																												// KY
							elementSubContent.appendChild(commons.createElementWithValue(doc, "MHSo", ""));
							// Ngày lập
							elementSubContent.appendChild(commons.createElementWithValue(doc, "NLap",
									NLap.format(DateTimeFormatter.ISO_DATE)));
							// Số bảng kê
							elementSubContent.appendChild(commons.createElementWithValue(doc, "SBKe", ""));
							// Ngày bảng kê
							elementSubContent.appendChild(commons.createElementWithValue(doc, "NBKe", ""));
							// Đơn vị tiền tệ
							elementSubContent.appendChild(commons.createElementWithValue(doc, "DVTTe", DVTTe));
							// Tỷ giá
							elementSubContent.appendChild(commons.createElementWithValue(doc, "TGia", TGia));
							// Hình thức thanh toán
							elementSubContent.appendChild(commons.createElementWithValue(doc, "HTTToan", HTTToan));
							// MST tổ chức cung cấp giải pháp HĐĐT
							elementSubContent
									.appendChild(commons.createElementWithValue(doc, "MSTTCGP", SystemParams.MSTTCGP));
							// MST đơn vị nhận ủy nhiệm lập hóa đơn
							elementSubContent.appendChild(commons.createElementWithValue(doc, "MSTDVNUNLHDon", ""));
							// Tên đơn vị nhận ủy nhiệm lập hóa đơn
							elementSubContent.appendChild(commons.createElementWithValue(doc, "TDVNUNLHDon", ""));
							// Địa chỉ đơn vị nhận ủy nhiệm lập hóa đơn
							elementSubContent.appendChild(commons.createElementWithValue(doc, "DCDVNUNLHDon", ""));
							elementTmp = doc.createElement("TTKhac"); // THONG TIN KHAC
							elementTmp.appendChild(commons.createElementTTKhac(doc, "PortalLink", "string", link));
							elementTmp.appendChild(commons.createElementTTKhac(doc, "SecureKey", "string", SecureKey));
							elementTmp.appendChild(commons.createElementTTKhac(doc, "SystemKey", "string",
									objectIdEInvoice.toString()));

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
//	                			elementTmp.appendChild(commons.createElementWithValue(doc, "TTKhac", ""));

							/* ADD THONG TIN TK NGAN HANG (NEU CO) */
							elementSubTmp = doc.createElement("TTKhac");
							elementSubTmp.appendChild(
									commons.createElementTTKhac(doc, "TenEN", "string", docTmp.get("NameEN", "")));

							elementSubTmp.appendChild(
									commons.createElementTTKhac(doc, "STKNHang" + intTmp, "string", STKNHang));
							elementSubTmp
									.appendChild(commons.createElementTTKhac(doc, "TNHang" + intTmp, "string", TNHang));

							elementTmp.appendChild(elementSubTmp);
							elementSubContent.appendChild(elementTmp);

							elementTmp = doc.createElement("NMua"); // NGUOI MUA
							elementTmp.appendChild(commons.createElementWithValue(doc, "Ten", TenNM));
							elementTmp.appendChild(commons.createElementWithValue(doc, "MST", MSTNM));
							elementTmp.appendChild(commons.createElementWithValue(doc, "DChi", DChiNM));
							elementTmp.appendChild(commons.createElementWithValue(doc, "MKHang", MaKHangNM));
							elementTmp.appendChild(commons.createElementWithValue(doc, "SDThoai", SDThoaiNM));
							elementTmp.appendChild(commons.createElementWithValue(doc, "DCTDTu", DCTDTuNM));
							elementTmp.appendChild(commons.createElementWithValue(doc, "HVTNMHang", HVTNMHangNM));
							elementTmp.appendChild(commons.createElementWithValue(doc, "STKNHang", STKNHangNM));
							elementTmp.appendChild(commons.createElementWithValue(doc, "TNHang", TNHangNM));
							elementTmp.appendChild(commons.createElementWithValue(doc, "TTKhac", ""));
							elementSubContent.appendChild(elementTmp);

							mapVATAmount = new LinkedHashMap<String, Double>();
							mapAmount = new LinkedHashMap<String, Double>();
							elementTmp = doc.createElement("DSHHDVu"); // HH-DV
							// dshhdVuList, listHHDVu, \VATRate,
							// Json.serializer().nodeFromObject(msg.getObjData());

							for (Object o : listHHDVus) {
								if (!"".equals(o.equals("/ProductName"))) {
									JsonNode h = Json.serializer().nodeFromObject(o);
									tmp = commons.formatNumberReal(commons.getTextJsonNode(h.at("/VATRate")))
											.replaceAll(",", "");
									switch (tmp) {
									case "0":
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
											return (v == null ? commons
													.ToNumber(commons.getTextJsonNode(h.at("/Total")))
													* ("3".equals(commons.getTextJsonNode(h.at("/Feature"))) ? -1 : 1)
													: v + commons.ToNumber(commons.getTextJsonNode(h.at("/Total")))
															* ("3".equals(commons.getTextJsonNode(h.at("/Feature")))
																	? -1
																	: 1));
										});
										mapVATAmount.compute(tmp, (f, v) -> {
											return (v == null ? commons
													.ToNumber(commons.getTextJsonNode(h.at("/VATAmount")))
													* ("3".equals(commons.getTextJsonNode(h.at("/Feature"))) ? -1 : 1)
													: v + commons.ToNumber(commons.getTextJsonNode(h.at("/VATAmount")))
															* ("3".equals(commons.getTextJsonNode(h.at("/Feature")))
																	? -1
																	: 1));
										});
									}
									Boolean slo1 = false;
									Boolean hsd1 = false;
									String slo = commons.getTextJsonNode(h.at("/SLo"));
									String hsd = commons.getTextJsonNode(h.at("/HanSD"));
									if ("".equals(slo)) {
										slo1 = true;
									}
									if ("".equals(hsd)) {
										hsd1 = true;
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
									if (slo1 == false) {
										elementSubTmp.appendChild(commons.createElementWithValue(doc, "SLo",
												commons.getTextJsonNode(h.at("/SLo"))));
									}
									if (hsd1 == false) {
										elementSubTmp.appendChild(commons.createElementWithValue(doc, "HanSD",
												commons.getTextJsonNode(h.at("/HanSD"))));
									}
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
										String thtien = commons.getTextJsonNode(h.at("/Total")).replaceAll(",", "");
										elementSubTmp.appendChild(commons.createElementWithValue(doc, "ThTien",
												commons.formatNumberReal(thtien).replaceAll(",", "")));
										elementSubTmp.appendChild(commons.createElementWithValue(doc, "TSuat", tmp));
									}

									elementSubTmp01 = doc.createElement("TTKhac");
									elementSubTmp01.appendChild(commons.createElementTTKhac(doc, "VATAmount", "decimal",
											commons.formatNumberReal(commons.getTextJsonNode(h.at("/VATAmount")))
													.replaceAll(",", "")));
									elementSubTmp01.appendChild(commons.createElementTTKhac(doc, "Amount", "decimal",
											commons.formatNumberReal(commons.getTextJsonNode(h.at("/Amount")))
													.replaceAll(",", "")));
									elementSubTmp.appendChild(elementSubTmp01);
									elementTmp.appendChild(elementSubTmp);

								}
							}
							elementSubContent.appendChild(elementTmp);

							elementTmp = doc.createElement("TToan"); // Thong tin thanh toan
							elementSubTmp = doc.createElement("THTTLTSuat");

							/* DANH SACH CAC LOAI THUE SUAT */

							// https://stackoverflow.com/questions/46898/how-do-i-efficiently-iterate-over-each-entry-in-a-java-map
							for (Map.Entry<String, Double> pair : mapVATAmount.entrySet()) {
								if (null != pair.getKey() && !"".equals(pair.getKey())) {
									elementSubTmp01 = doc.createElement("LTSuat");
									elementSubTmp01
											.appendChild(commons.createElementWithValue(doc, "TSuat", pair.getKey()));
									elementSubTmp01.appendChild(commons.createElementWithValue(doc, "ThTien", commons
											.formatNumberReal(mapAmount.get(pair.getKey())).replaceAll(",", "")));
									elementSubTmp01.appendChild(commons.createElementWithValue(doc, "TThue",
											String.format("%.0f", mapVATAmount.get(pair.getKey()))));
									elementSubTmp.appendChild(elementSubTmp01);
								}
							}
							elementTmp.appendChild(elementSubTmp);
							elementTmp.appendChild(commons.createElementWithValue(doc, "TgTCThue",
									commons.formatNumberReal(TTTT).replaceAll(",", "")));
							elementTmp.appendChild(commons.createElementWithValue(doc, "TgTThue",
									commons.formatNumberReal(TTT).replaceAll(",", "")));

							elementSubTmp = doc.createElement("DSLPhi");
							elementSubTmp01 = doc.createElement("LPhi");
							elementSubTmp01.appendChild(commons.createElementWithValue(doc, "TLPhi", ""));
							elementSubTmp01.appendChild(commons.createElementWithValue(doc, "TPhi", "0"));
							elementSubTmp.appendChild(elementSubTmp01);

							elementTmp.appendChild(elementSubTmp);

							elementTmp.appendChild(commons.createElementWithValue(doc, "TTCKTMai", "0"));
							elementTmp.appendChild(commons.createElementWithValue(doc, "TgTTTBSo",
									commons.formatNumberReal(TTDCThue).replaceAll(",", "")));
							elementTmp.appendChild(commons.createElementWithValue(doc, "TgTTTBChu", TongTienBangchu));
							elementSubTmp = doc.createElement("TTKhac");
							elementTmp.appendChild(elementSubTmp);

							elementSubContent.appendChild(elementTmp);

							elementContent.appendChild(elementSubContent);
							// END - NDHDon: Nội dung hóa đơn

							isSdaveFile = commons.docW3cToFile(doc, Dir, FileNameXML);
							if (!isSdaveFile) {
								throw new Exception("Lưu dữ liệu không thành công.");
							}
							/* END - TAO XML HOA DON */
							// END XML
							// lookup data
							docUpsert = new Document("_id", objectIdEInvoice).append("IssuerId", header.getIssuerId())
									.append("MTDiep", MTDiep)
									.append("EInvoiceDetail", new Document("TTChung",
											new Document("THDon", THDon).append("MaHD", MaHD).append("MauSoHD", MauSoHD)
													.append("KHMSHDon", KHMSHDon).append("KHHDon", KHHDon)
													.append("NLap", NLap).append("DVTTe", DVTTe).append("TGia", TGia)
													.append("HTTToanCode", HTTToanCode).append("HTTToan", HTTToan))
											.append("NDHDon", new Document("NBan",
													new Document("Ten", Ten).append("MST", MST).append("DChi", DChi)
															.append("SDThoai", SDThoai).append("DCTDTu", DCTDTu)
															.append("STKNHang", STKNHang).append("TNHang", TNHang)
															.append("Fax", Fax).append("Website", Website))
													.append("NMua", new Document("Ten", TenNM).append("MST", MSTNM)
															.append("DChi", DChiNM).append("MKHang", MaKHangNM)
															.append("SDThoai", SDThoaiNM).append("DCTDTu", DCTDTuNM)
															.append("HVTNMHang", HVTNMHangNM)
															.append("STKNHang", STKNHangNM).append("TNHang", TNHangNM)))

											.append("DSHHDVu", listHHDVus)
											.append("TToan", new Document("TgTCThue", TTTT).append("TgTThue", TTT)
													.append("TgTTTBSo", TTDCThue).append("TgTTTBChu", TongTienBangchu)))
									.append("SignStatusCode", SignStatusCode).append("EInvoiceStatus", EInvoiceStatus)
									.append("IsDelete", IsDelete).append("SecureKey", SecureKey).append("Dir", Dir)
									.append("FileNameXML", FileNameXML).append("InfoCreated",
											new Document("CreateDate", LocalDateTime.now())
													.append("CreateUserID", header.getUserId())
													.append("CreateUserName", header.getUserName())
													.append("CreateUserFullName", header.getUserFullName()));
			
							 mongoClient = cfg.mongoClient();
								collection = mongoClient.getDatabase(cfg.dbName).getCollection("EInvoice");
								collection.insertOne(docUpsert);			
								mongoClient.close();
							dshhdVuList.clear();
							listHHDVus.clear();
							checkMaHD = false;
						}
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

	// HAM DEM SO O TRONG FILE EXCEL AUTO

	private static EInvoiceExcelFormMISA extractInfoFromCellMISA(List<Cell> cells) {
		EInvoiceExcelFormMISA eInvoiceExcelForm = new EInvoiceExcelFormMISA();
		// han su dung
		Cell DateHD = cells.get(0);
		if (DateHD != null) {
			switch (DateHD.getCellType()) {
			case STRING:
				eInvoiceExcelForm.setDateHD(DateHD.getStringCellValue());
				break;
			case NUMERIC:
				eInvoiceExcelForm.setDateHD((NumberToTextConverter.toText(DateHD.getNumericCellValue())));
				break;
			case BLANK:
				break;
			default:
				break;
			}
		}
		// So hoa don
		Cell SHD = cells.get(1);
		if (SHD != null) {
			switch (SHD.getCellType()) {
			case STRING:
				eInvoiceExcelForm.setSHD(SHD.getStringCellValue());
				break;
			case NUMERIC:
				eInvoiceExcelForm.setSHD((NumberToTextConverter.toText(SHD.getNumericCellValue())));
				break;
			case BLANK:
				break;
			default:
				break;
			}
		}
		// Ten nguoi mua
		Cell TenNguoiMua = cells.get(2);
		if (TenNguoiMua != null) {
			switch (TenNguoiMua.getCellType()) {
			case STRING:
				eInvoiceExcelForm.setTenNguoiMua(TenNguoiMua.getStringCellValue());
				break;
			case NUMERIC:
				eInvoiceExcelForm.setTenNguoiMua((NumberToTextConverter.toText(TenNguoiMua.getNumericCellValue())));
				break;
			case BLANK:
				break;
			default:
				break;
			}
		}

		// mst nguoi mua
		Cell MstNguoiMua = cells.get(3);
		if (MstNguoiMua != null) {
			switch (MstNguoiMua.getCellType()) {

			case STRING:
				eInvoiceExcelForm.setMST(MstNguoiMua.getStringCellValue());
				break;
			case NUMERIC:
				eInvoiceExcelForm.setMST((NumberToTextConverter.toText(MstNguoiMua.getNumericCellValue())));
				break;
			case BLANK:
				break;
			default:
				break;
			}
		}

		// dia chi nguoi mua
		Cell DiaChiNguoiMua = cells.get(4);
		if (DiaChiNguoiMua != null) {
			switch (DiaChiNguoiMua.getCellType()) {
			case STRING:
				eInvoiceExcelForm.setAddress(DiaChiNguoiMua.getStringCellValue());
				break;
			case NUMERIC:
				eInvoiceExcelForm.setAddress((NumberToTextConverter.toText(DiaChiNguoiMua.getNumericCellValue())));
				break;
			case BLANK:
				break;
			default:
				break;
			}
		}
		// Ma hang hoa
		Cell MaHangHoa = cells.get(5);
		if (MaHangHoa != null) {
			switch (MaHangHoa.getCellType()) {
			case STRING:
				eInvoiceExcelForm.setMaHangHoa(MaHangHoa.getStringCellValue());
				break;
			case NUMERIC:
				eInvoiceExcelForm.setMaHangHoa((NumberToTextConverter.toText(MaHangHoa.getNumericCellValue())));
				break;
			case BLANK:
				break;
			default:
				break;
			}
		}
		// Ten hang hoa
		Cell TenHangHoa = cells.get(6);
		if (TenHangHoa != null) {
			switch (TenHangHoa.getCellType()) {
			case STRING:
				eInvoiceExcelForm.setTenHangHoa(TenHangHoa.getStringCellValue());
				break;
			case NUMERIC:
				eInvoiceExcelForm.setTenHangHoa((NumberToTextConverter.toText(TenHangHoa.getNumericCellValue())));
				break;
			case BLANK:
				break;
			default:
				break;
			}
		}
		// Don vi tinh
		Cell DonViTinh = cells.get(7);
		if (DonViTinh != null) {
			switch (DonViTinh.getCellType()) {
			case STRING:
				eInvoiceExcelForm.setDonViTinh(DonViTinh.getStringCellValue());
				break;
			case NUMERIC:
				eInvoiceExcelForm.setDonViTinh((NumberToTextConverter.toText(DonViTinh.getNumericCellValue())));
				break;
			case BLANK:
				break;
			default:
				break;
			}
		}
		// so luong
		Cell SoLuong = cells.get(8);
		if (SoLuong != null) {
			switch (SoLuong.getCellType()) {
			case STRING:
				eInvoiceExcelForm.setSoLuong((Double.valueOf((String) SoLuong.getStringCellValue())));
				break;
			case NUMERIC:
				eInvoiceExcelForm.setSoLuong(SoLuong.getNumericCellValue());
				break;
			case BLANK:
				break;
			default:
				break;
			}
		}
		// Don gia
		Cell DonGia = cells.get(9);
		if (DonGia != null) {
			switch (DonGia.getCellType()) {
			case STRING:
				eInvoiceExcelForm.setDonGia((Double.valueOf((String) DonGia.getStringCellValue())));
				break;
			case NUMERIC:
				eInvoiceExcelForm.setDonGia(DonGia.getNumericCellValue());
				break;
			case BLANK:
				break;
			default:
				break;
			}
		}

		// Thanh tien
		Cell ThanhTien = cells.get(11);
		if (ThanhTien != null && (ThanhTien.getCellType() == CellType.FORMULA)) {
			switch (ThanhTien.getCachedFormulaResultType()) {
			case STRING:
				eInvoiceExcelForm.setThanhTien((Double.valueOf((String) ThanhTien.getStringCellValue())));
				break;
			case NUMERIC:
				eInvoiceExcelForm.setThanhTien(ThanhTien.getNumericCellValue());
				break;
			case BLANK:
				break;
			default:
				break;
			}
		}
		if (ThanhTien != null) {
			switch (ThanhTien.getCellType()) {
			case STRING:
				eInvoiceExcelForm.setThanhTien((Double.valueOf((String) ThanhTien.getStringCellValue())));
				break;
			case NUMERIC:
				eInvoiceExcelForm.setThanhTien(ThanhTien.getNumericCellValue());
				break;
			case BLANK:
				break;
			default:
				break;
			}
		}
		// Tien thue
		Cell TienThue = cells.get(10);
		if (TienThue != null && (TienThue.getCellType() == CellType.FORMULA)) {
			switch (TienThue.getCachedFormulaResultType()) {
			case STRING:
				eInvoiceExcelForm.setTienThue((Double.valueOf((String) TienThue.getStringCellValue())));
				break;
			case NUMERIC:
				eInvoiceExcelForm.setTienThue(TienThue.getNumericCellValue());
				break;
			case BLANK:
				break;
			default:
				break;
			}
		}
		if (TienThue != null) {
			switch (TienThue.getCellType()) {
			case STRING:
				eInvoiceExcelForm.setTienThue((Double.valueOf((String) TienThue.getStringCellValue())));
				break;
			case NUMERIC:
				eInvoiceExcelForm.setTienThue(TienThue.getNumericCellValue());
				break;
			case BLANK:
				break;
			default:
				break;
			}
		}

		// Tong tien
		Cell Tongtien = cells.get(12);
		if (Tongtien != null && (Tongtien.getCellType() == CellType.FORMULA)) {
			switch (Tongtien.getCachedFormulaResultType()) {
			case STRING:
				eInvoiceExcelForm.setTongTien((Double.valueOf((String) Tongtien.getStringCellValue())));
				break;
			case NUMERIC:
				eInvoiceExcelForm.setTongTien(Tongtien.getNumericCellValue());
				break;
			case BLANK:
				break;
			default:
				break;
			}
		}
		if (Tongtien != null) {
			switch (Tongtien.getCellType()) {
			case STRING:
				eInvoiceExcelForm.setTongTien((Double.valueOf((String) Tongtien.getStringCellValue())));
				break;
			case NUMERIC:
				eInvoiceExcelForm.setTongTien(Tongtien.getNumericCellValue());
				break;
			case BLANK:
				break;
			default:
				break;
			}
		}
		// Tra ve danh sach
		return eInvoiceExcelForm;
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
		for (JsonNode o : jsonData.at("/id")) {
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
						new Document("from", "EInvoice")
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
		pipeline.add(new Document("$lookup", new Document("from", "EInvoice")
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
		pipeline.add(new Document("$lookup", new Document("from", "EInvoice")
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
		pipeline.add(new Document("$lookup", new Document("from", "EInvoice")
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
		MongoCollection<Document> collection = mongoClient.getDatabase(cfg.dbName).getCollection("EInvoice");

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
}
