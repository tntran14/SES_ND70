package vn.sesgroup.hddt.user.impl;

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
import java.util.concurrent.TimeUnit;

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
import com.mongodb.client.model.UpdateOptions;

import vn.sesgroup.hddt.configuration.ConfigConnectMongo;
import vn.sesgroup.hddt.dto.FileInfo;
import vn.sesgroup.hddt.dto.MailConfig;
import vn.sesgroup.hddt.model.DSHHDVu;
import vn.sesgroup.hddt.model.EInvoicePXKDLExcelForm;
import vn.sesgroup.hddt.user.dao.AbstractDAO;
import vn.sesgroup.hddt.user.dao.AgentDAO;
import vn.sesgroup.hddt.user.service.JPUtils;
import vn.sesgroup.hddt.user.service.TCTNService;
import vn.sesgroup.hddt.utility.Constants;
import vn.sesgroup.hddt.utility.Json;
import vn.sesgroup.hddt.utility.MailUtils;
import vn.sesgroup.hddt.utility.MailjetSender;
import vn.sesgroup.hddt.utility.SystemParams;

@Repository
@Transactional
public class AgentImpl extends AbstractDAO implements AgentDAO {
	private static final Logger log = LogManager.getLogger(AgentImpl.class);
	@Autowired
	ConfigConnectMongo cfg;
	@Autowired
	TCTNService tctnService;
	@Autowired
	JPUtils jpUtils;
	private MailUtils mailUtils = new MailUtils();
	private MailjetSender mailJet = new MailjetSender();
	Document docUpsert = null;


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
		String tenLoaiHd = commons.getTextJsonNode(jsonData.at("/TenLoaiHd")).trim().replaceAll("\\s+", " ");
		String ngayLap = commons.getTextJsonNode(jsonData.at("/NgayLap")).replaceAll("\\s", "");
		String hinhThucThanhToan = commons.getTextJsonNode(jsonData.at("/HinhThucThanhToan")).replaceAll("\\s", "");
		String hinhThucThanhToanText = commons.getTextJsonNode(jsonData.at("/HinhThucThanhToanText")).trim()
				.replaceAll("\\s+", " ");
		String khMst = commons.getTextJsonNode(jsonData.at("/KhMst")).trim().replaceAll("\\s+", "")
				.replaceAll("[+^%$#@&*]*", "").replaceAll("[a-z][A-Z]*", "");

		String khHoTenNguoiVC = commons.getTextJsonNode(jsonData.at("/khHoTenNguoiVC")).trim().replaceAll("\\s+", " ");
		String khHoTenNguoiXuat = commons.getTextJsonNode(jsonData.at("/khHoTenNguoiXuat")).trim().replaceAll("\\s+",
				" ");
		String HDKTSo = commons.getTextJsonNode(jsonData.at("/HDKTSo")).trim().replaceAll("\\s+", " ");
		String HDKTNgay = commons.getTextJsonNode(jsonData.at("/HDKTNgay")).trim().replaceAll("\\s+", " ");
		String PTVChuyen = commons.getTextJsonNode(jsonData.at("/PTVChuyen")).trim().replaceAll("\\s+", " ");
		String TNDDien = commons.getTextJsonNode(jsonData.at("/TNDDien")).trim().replaceAll("\\s+", " ");
		String NDXKho = commons.getTextJsonNode(jsonData.at("/NDXKho")).trim().replaceAll("\\s+", " ");
		String DChi = commons.getTextJsonNode(jsonData.at("/Dchi")).trim().replaceAll("\\s+", " ");
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
		Document docEInvoiceTTDC = null;
		Document docFind = null;
		Document docTmp = null;
		Document docFind1 = null;
		Document docTmp1 = null;
		Document docTTHDLQuan = null;
		Document docUpsert = null;
		FindOneAndUpdateOptions options = null;

		Document docFindNmua = null;
		Document docUpsert1 = null;
		ObjectId objectNMua = null;
		ObjectId objectIdQLNMua = null;

		String nlap_shd_max = null;
		DateTimeFormatter formatter = null;

		LocalDate localDate1 = null;
		LocalDate localDate2 = null;

		
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
		String link = "";
		switch (actionCode) {
		case Constants.MSG_ACTION_CODE.CREATED:
	
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
			/* KIEM TRA THONG TIN KHACH HANG - USERS */
			try {
				if (!"".equals(_id_tt_dc))
					objectIdTT_DC = new ObjectId(_id_tt_dc);
			} catch (Exception e) {
			}
			
			Document  findInforIssuer = new Document("_id", 1)
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
			pipeline.add(new Document("$match", new Document("_id", objectId).append("IsActive", true)
					.append("IsDelete", new Document("$ne", true))));
			pipeline.add(new Document("$project", findInforIssuer));
			
			pipeline.add(new Document("$lookup", new Document("from", "Users").append("pipeline", Arrays.asList(
					new Document("$match",
							new Document("IssuerId", header.getIssuerId()).append("_id", objectIdUser)
									.append("IsActive", true).append("IsDelete", new Document("$ne", true))),
					new Document("$project", new Document("_id", 1).append("UserName", 1).append("FullName", 1)),
					new Document("$limit", 1))).append("as", "UserInfo")));
			pipeline.add(new Document("$unwind",
					new Document("path", "$UserInfo").append("preserveNullAndEmptyArrays", true)));
			pipeline.add(new Document("$lookup",
					new Document("from", "PramLink")
							.append("pipeline",
									Arrays.asList(new Document("$match",
											new Document("$expr", new Document("IsDelete", false))),
											new Document("$project", new Document("_id", 1).append("LinkPortal", 1))
											))
							.append("as", "PramLink")));
			pipeline.add(new Document("$unwind",
					new Document("path", "$PramLink").append("preserveNullAndEmptyArrays", true)));
			pipeline.add(new Document("$lookup",
					new Document("from", "DMMauSoKyHieu")
							.append("pipeline",
									Arrays.asList(new Document("$match", new Document("IssuerId", header.getIssuerId())
											.append("IsActive", true).append("IsDelete", new Document("$ne", true))
											.append("ConLai", new Document("$gt", 0)).append("_id", objectIdMSKH)),
											new Document("$project", new Document("_id", 1).append("KHMSHDon", 1).append("KHHDon", 1))
											))
							.append("as", "DMMauSoKyHieu")));
			pipeline.add(new Document("$unwind",
					new Document("path", "$DMMauSoKyHieu").append("preserveNullAndEmptyArrays", true)));
			if (!"".equals(_id_tt_dc)) {
				pipeline.add(new Document("$lookup", new Document("from", "EInvoicePXKDL")
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

			// KIEM TRA TRONG DANH SASH QUAN LY NGUOI MUA
			if (!"".equals(khMst)) {
				pipeline.add(new Document("$lookup",
						new Document("from", "QLDSNMua")
								.append("pipeline",
										Arrays.asList(new Document("$match", new Document("MST", khMst)
												.append("IsDelete", new Document("$ne", true)))))
								.append("as", "DSNMua")));
				pipeline.add(new Document("$unwind",
						new Document("path", "$DSNMua").append("preserveNullAndEmptyArrays", true)));
			}

			// CHECK NGAY LAP SO HOA DON LON NHAT
			pipeline.add(
					new Document("$lookup",
							new Document("from", "EInvoicePXKDL")
									.append("pipeline", Arrays.asList(
											new Document("$match",
													new Document("IsDelete", new Document("$ne", true))
															.append("EInvoiceDetail.TTChung.MauSoHD", mauSoHdon)),
											new Document("$group",
													new Document("_id", "$EInvoiceDetail.TTChung.MauSoHD").append(
															"SHDon",
															new Document("$max", "$EInvoiceDetail.TTChung.SHDon")))))
									.append("as", "NLap_MAX")));
			pipeline.add(new Document("$unwind",
					new Document("path", "$NLap_MAX").append("preserveNullAndEmptyArrays", true)));
			// END CHECK NGAY LAP

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
			link = docTmp.getEmbedded(Arrays.asList("PramLink", "LinkPortal"), "");
			if (docTmp.get("DMMauSoKyHieu") == null) {
				responseStatus = new MspResponseStatus(9999, "Không tìm thấy thông tin ký hiệu mẫu số.");
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
					.append("EInvoiceDetail.TTChung.MauSoHD", nl_mshd).append("IsDelete", new Document("$ne", true));

			Document docTmp2 = null;

			mongoClient = cfg.mongoClient();
			collection = mongoClient.getDatabase(cfg.dbName).getCollection("EInvoicePXKDL");
			try {
				docTmp2 = collection.find(docFindNLap).allowDiskUse(true).iterator().next();
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

			elementSubContent.appendChild(commons.createElementWithValue(doc, "PBan", SystemParams.VERSION_XML_PXK));
			elementSubContent.appendChild(commons.createElementWithValue(doc, "THDon", tenLoaiHd));
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
			// MST tổ chức cung cấp giải pháp HĐĐT
			elementSubContent.appendChild(commons.createElementWithValue(doc, "MSTTCGP", SystemParams.MSTTCGP));

			elementTmp = doc.createElement("TTKhac"); // THONG TIN KHAC
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
			// hợp đồng kinh tế số
			elementTmp.appendChild(commons.createElementWithValue(doc, "HDKTSo", HDKTSo));
			elementTmp.appendChild(
					commons.createElementWithValue(doc, "HDKTNgay", commons.convertLocalDateTimeStringToString(HDKTNgay,
							Constants.FORMAT_DATE.FORMAT_DATE_WEB, Constants.FORMAT_DATE.FORMAT_DATE_EINVOICE, false)));
			elementTmp.appendChild(commons.createElementWithValue(doc, "DChi", docTmp.get("Address", "")));
			elementTmp.appendChild(commons.createElementWithValue(doc, "HDSo", docTmp.get("HDSo", "")));
			elementTmp.appendChild(commons.createElementWithValue(doc, "HVTNXHang", khHoTenNguoiXuat));
			elementTmp.appendChild(commons.createElementWithValue(doc, "TNVChuyen", khHoTenNguoiVC));
			elementTmp.appendChild(commons.createElementWithValue(doc, "PTVChuyen", PTVChuyen));
//			elementTmp.appendChild(commons.createElementWithValue(doc, "TTKhac", ""));

			/* ADD THONG TIN TK NGAN HANG (NEU CO) */
			elementSubTmp = doc.createElement("TTKhac");
			elementSubTmp.appendChild(commons.createElementTTKhac(doc, "ComAddress", "string", DChi));
			elementSubTmp.appendChild(commons.createElementTTKhac(doc, "ComPhone", "string", docTmp.get("Phone", "")));
			elementSubTmp.appendChild(commons.createElementTTKhac(doc, "ComEmail", "string", docTmp.get("Email", "")));
			elementSubTmp.appendChild(commons.createElementTTKhac(doc, "STKNHang", "string",
					docTmp.getEmbedded(Arrays.asList("BankAccount", "AccountNumber"), "")));
			elementSubTmp.appendChild(commons.createElementTTKhac(doc, "TNHang", "string",
					docTmp.getEmbedded(Arrays.asList("BankAccount", "BankName"), "")));
			elementSubTmp.appendChild(commons.createElementTTKhac(doc, "ComFax", "string", docTmp.get("Fax", "")));
			elementSubTmp.appendChild(commons.createElementTTKhac(doc, "PostBy", "string", TNDDien));
			elementSubTmp.appendChild(commons.createElementTTKhac(doc, "PostDescription", "string", NDXKho));
			elementTmp.appendChild(elementSubTmp);
			elementSubContent.appendChild(elementTmp);

			elementTmp = doc.createElement("NMua"); // NGUOI MUA
			elementTmp.appendChild(commons.createElementWithValue(doc, "Ten", khTenDonVi));
			elementTmp.appendChild(commons.createElementWithValue(doc, "MST", khMst));
			elementTmp.appendChild(commons.createElementWithValue(doc, "DChi", khDiaChi));
			elementTmp.appendChild(commons.createElementWithValue(doc, "HVTNMHang", khHoTenNguoiXuat));

			elementSubTmp = doc.createElement("TTKhac"); // THONG TIN KHAC
			elementSubTmp.appendChild(commons.createElementTTKhac(doc, "MoveNo", "string", HDKTSo));
			elementSubTmp.appendChild(commons.createElementTTKhac(doc, "MoveDate", "string",
					commons.convertLocalDateTimeStringToString(ngayLap, Constants.FORMAT_DATE.FORMAT_DATE_WEB,
							Constants.FORMAT_DATE.FORMAT_DATE_EINVOICE, false)));
			elementSubTmp.appendChild(commons.createElementTTKhac(doc, "TransportDivice", "string", PTVChuyen));
			elementSubTmp.appendChild(commons.createElementTTKhac(doc, "TransportName", "string", khHoTenNguoiVC));
			elementTmp.appendChild(elementSubTmp);
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
						case "5":
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

						mapAmount.compute(tmp, (k, v) -> {
							return (v == null ? commons.ToNumber(commons.getTextJsonNode(o.at("/Total")))
									: v + commons.ToNumber(commons.getTextJsonNode(o.at("/Total"))));
						});
						mapVATAmount.compute(tmp, (k, v) -> {
							return (v == null ? commons.ToNumber(commons.getTextJsonNode(o.at("/VATAmount")))
									: v + commons.ToNumber(commons.getTextJsonNode(o.at("/VATAmount"))));
						});

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
						elementSubTmp.appendChild(commons.createElementWithValue(doc, "SLuong",
								commons.getTextJsonNode(o.at("/Quantity")).replaceAll(",", "")));
						elementSubTmp.appendChild(commons.createElementWithValue(doc, "DGia",
								commons.getTextJsonNode(o.at("/Price")).replaceAll(",", "")));
						elementSubTmp.appendChild(commons.createElementWithValue(doc, "ThTien",
								commons.getTextJsonNode(o.at("/Total")).replaceAll(",", "")));
						elementSubTmp.appendChild(commons.createElementWithValue(doc, "TSuat", "0%"));

						elementSubTmp01 = doc.createElement("TTKhac");
						elementSubTmp01.appendChild(commons.createElementTTKhac(doc, "SLXuat", "String",
								commons.getTextJsonNode(o.at("/Total")).replaceAll(",", "")));
						elementSubTmp01.appendChild(commons.createElementTTKhac(doc, "VATAmount", "decimal",
								commons.getTextJsonNode(o.at("/VATAmount")).replaceAll(",", "")));
						elementSubTmp01.appendChild(commons.createElementTTKhac(doc, "Amount", "decimal",
								commons.getTextJsonNode(o.at("/Amount")).replaceAll(",", "")));
						elementSubTmp.appendChild(elementSubTmp01);

						elementTmp.appendChild(elementSubTmp);

						hItem = new LinkedHashMap<String, Object>();
						hItem.put("STT", commons.getTextJsonNode(o.at("/STT")));
						hItem.put("ProductName", commons.getTextJsonNode(o.at("/ProductName")));
						hItem.put("ProductCode", commons.getTextJsonNode(o.at("/ProductCode")));
						hItem.put("Unit", commons.getTextJsonNode(o.at("/Unit")));
						hItem.put("Quantity", commons.ToNumber(commons.getTextJsonNode(o.at("/Quantity"))));
						hItem.put("Price", commons.ToNumber(commons.getTextJsonNode(o.at("/Price"))));
						hItem.put("Total", commons.ToNumber(commons.getTextJsonNode(o.at("/Total"))));
						hItem.put("Amount", commons.ToNumber(commons.getTextJsonNode(o.at("/Amount"))));
						hItem.put("Feature", commons.getTextJsonNode(o.at("/Feature")));
						listDSHHDVu.add(hItem);

					}

				}
			}
			elementSubContent.appendChild(elementTmp);

			elementTmp = doc.createElement("TToan"); // Thong tin thanh toan
			elementSubTmp = doc.createElement("THTTLTSuat");
			/* DANH SACH CAC LOAI THUE SUAT */

//			https://stackoverflow.com/questions/46898/how-do-i-efficiently-iterate-over-each-entry-in-a-java-map
			for (Map.Entry<String, Double> pair : mapVATAmount.entrySet()) {
				elementSubTmp01 = doc.createElement("LTSuat");
				elementSubTmp01.appendChild(commons.createElementWithValue(doc, "TSuat", "KCT"));
				elementSubTmp01.appendChild(commons.createElementWithValue(doc, "ThTien",
						commons.formatNumberReal(mapAmount.get(pair.getKey())).replaceAll(",", "")));
				elementSubTmp01.appendChild(commons.createElementWithValue(doc, "TThue",
						commons.formatNumberReal(mapVATAmount.get(pair.getKey())).replaceAll(",", "")));
				elementSubTmp.appendChild(elementSubTmp01);
			}
			elementTmp.appendChild(elementSubTmp);

			elementTmp.appendChild(
					commons.createElementWithValue(doc, "TgTCThue", tongTienTruocThue.replaceAll(",", "")));
			elementTmp
					.appendChild(commons.createElementWithValue(doc, "TgTThue", tongTienThueGtgt.replaceAll(",", "")));
			elementTmp.appendChild(
					commons.createElementWithValue(doc, "TTCKTMai", tongTienTruocThue.replaceAll(",", "")));
			elementTmp
					.appendChild(commons.createElementWithValue(doc, "TgTTTBSo", tongTienDaCoThue.replaceAll(",", "")));
			elementTmp.appendChild(commons.createElementWithValue(doc, "TgTTTBChu", tienBangChu));

			elementContent.appendChild(elementSubContent);
			elementSubContent.appendChild(elementTmp);

			// END - NDHDon: Nội dung hóa đơn

			isSdaveFile = commons.docW3cToFile(doc, pathDir, fileNameXML);
			if (!isSdaveFile) {
				throw new Exception("Lưu dữ liệu không thành công.");
			}
			/* END - TAO XML HOA DON */

			MTDiep = SystemParams.MSTTCGP
					+ commons.csRandomAlphaNumbericString(46 - SystemParams.MSTTCGP.length()).toUpperCase();
			/* LUU DU LIEU HD */
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
//				.append("EInvoiceNumber", null)				//PHAT SINH KHI THUC HIEN KY
					.append("EInvoiceDetail", new Document("TTChung", new Document("THDon", tenLoaiHd)
							.append("MauSoHD", mauSoHdon)
							.append("KHMSHDon", docTmp.getEmbedded(Arrays.asList("DMMauSoKyHieu", "KHMSHDon"), ""))
							.append("KHHDon", docTmp.getEmbedded(Arrays.asList("DMMauSoKyHieu", "KHHDon"), ""))
							.append("NLap",
									commons.convertStringToLocalDate(ngayLap, Constants.FORMAT_DATE.FORMAT_DATE_WEB))
							.append("DVTTe", loaiTienTt).append("TGia", tyGia).append("HTTToanCode", hinhThucThanhToan)
							.append("HTTToan", hinhThucThanhToanText).append("TTHDLQuan", docTTHDLQuan))
							.append("NDHDon", new Document("NBan", new Document("Ten", docTmp.get("Name", ""))
									.append("MST", docTmp.get("TaxCode", "")).append("HDKTSo", HDKTSo)
									.append("HDKTNgay",
											commons.convertStringToLocalDate(HDKTNgay,
													Constants.FORMAT_DATE.FORMAT_DATE_WEB))
									.append("DChi", DChi).append("SDThoai", docTmp.get("Phone", ""))
									.append("TNVChuyen", khHoTenNguoiVC).append("PTVChuyen", PTVChuyen)
									.append("TNDDien", TNDDien).append("DCTDTu", docTmp.get("Email", ""))
									.append("STKNHang",
											docTmp.getEmbedded(Arrays.asList("BankAccount", "AccountNumber"), ""))
									.append("TNHang", docTmp.getEmbedded(Arrays.asList("BankAccount", "BankName"), ""))
									.append("Fax", docTmp.get("Fax", "")).append("Website", docTmp.get("Website", "")))
									.append("NMua",
											new Document("Ten", khTenDonVi).append("MST", khMst)
													.append("DChi", khDiaChi).append("NDXKho", NDXKho)
													.append("MKHang", "").append("SDThoai", khSoDT)
													.append("DCTDTu", khEmail).append("HVTNMHang", khHoTenNguoiXuat)
													.append("STKNHang", khSoTk).append("TNHang", khTkTaiNganHang)))
							.append("DSHHDVu", listDSHHDVu)
//					.append("DSHHDVu", 
//						jsonData.at("/DSSanPham").isMissingNode()?
//						new ArrayList<Object>():
//						Json.serializer().fromNode(jsonData.at("/DSSanPham"), new TypeReference<List<?>>() {
//						})
//					)
							.append("TToan",
									new Document("TgTCThue", commons.ToNumber(tongTienTruocThue))
											.append("TgTThue", commons.ToNumber(tongTienThueGtgt))
											.append("TgTTTBSo", commons.ToNumber(tongTienDaCoThue))
											.append("TgTTTBChu", tienBangChu)))
					.append("SignStatusCode", Constants.INVOICE_SIGN_STATUS.NOSIGN)
					.append("EInvoiceStatus", Constants.INVOICE_STATUS.CREATED).append("IsDelete", false)
					.append("SecureKey", secureKey).append("Dir", pathDir).append("FileNameXML", fileNameXML)
					.append("InfoCreated",
							new Document("CreateDate", LocalDateTime.now()).append("CreateUserID", header.getUserId())
									.append("CreateUserName", header.getUserName())
									.append("CreateUserFullName", header.getUserFullName()));
			/* END - LUU DU LIEU HD */

			mongoClient = cfg.mongoClient();
			collection = mongoClient.getDatabase(cfg.dbName).getCollection("EInvoicePXKDL");
			collection.insertOne(docUpsert);
			mongoClient.close();

			// Start replace, adjusted
			/* KIEM TRA THONG TIN HD CO TON TAI KHONG */
//			Document docFind1 = new Document("IssuerId", header.getIssuerId()).append("IsDelete", new Document("$ne", true))
//					.append("_id", objectIdTT_DC).append("SignStatusCode", Constants.INVOICE_SIGN_STATUS.SIGNED).append("EInvoiceStatus",  Constants.INVOICE_STATUS.COMPLETE);
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
					collection = mongoClient.getDatabase(cfg.dbName).getCollection("EInvoicePXKDL");
					collection.findOneAndUpdate(docFind1,
							new Document("$set", new Document("EInvoiceStatus", "REPLACED")), options);
					mongoClient.close();

				} else if ("2".equals(docEInvoiceTTDC.getEmbedded(Arrays.asList("HDSS", "TCTBao"), ""))) {

					mongoClient = cfg.mongoClient();
					collection = mongoClient.getDatabase(cfg.dbName).getCollection("EInvoicePXKDL");
					collection.findOneAndUpdate(docFind1,
							new Document("$set", new Document("EInvoiceStatus", "ADJUSTED")), options);
					mongoClient.close();

				}
			}
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
							new Document("$set",
									new Document("Ten", khTenDonVi)
											.append("NLap",
													commons.convertStringToLocalDate(ngayLap,
															Constants.FORMAT_DATE.FORMAT_DATE_WEB))
											.append("MST", khMst).append("DChi", khDiaChi).append("SDThoai", phone_nm)
											.append("DCTDTu", email_nm).append("IsDelete", false)),
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
						new Document("$ne", true));

				mongoClient = cfg.mongoClient();
				collection = mongoClient.getDatabase(cfg.dbName).getCollection("DMHistoryCustomer");
				try {
					docTmp = collection.find(docFind).allowDiskUse(true).iterator().next();
				} catch (Exception e) {
					
				}
				mongoClient.close();

				if (null == docTmp) {
					objectId = null;
					objectId = new ObjectId();
					docUpsert = new Document("_id", objectId).append("IssuerId", header.getIssuerId())
							.append("TaxCode", khMst)

							.append("CompanyName", khTenDonVi)

							.append("Address", khDiaChi).append("Email", khEmail)

							.append("Phone", khSoDT).append("InfoCreated",
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
							new Document("$set", new Document("IssuerId", header.getIssuerId()).append("TaxCode", khMst)

									.append("CompanyName", khTenDonVi)

									.append("Address", khDiaChi).append("Email", khEmail)

									.append("Phone", khSoDT).append("InfoUpdated",
											new Document("UpdatedDate", LocalDateTime.now())
													.append("UpdatedUserID", header.getUserId())
													.append("UpdatedUserName", header.getUserName())
													.append("UpdatedUserFullName", header.getUserFullName()))),
							options);
					mongoClient.close();

				}
			}
			// End replace, adjusted

			DateTimeFormatter format_time = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
			LocalDateTime time_dem = LocalDateTime.now();
			String time = time_dem.format(format_time);

			String name_company = removeAccent(header.getUserFullName());
			System.out.println(time + " " + name_company + " vua tao phieu ki gui dai ly");
			responseStatus = new MspResponseStatus(0, "SUCCESS");
			rsp.setResponseStatus(responseStatus);
			return rsp;

		case Constants.MSG_ACTION_CODE.COPY:

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
			/* KIEM TRA THONG TIN KHACH HANG - USERS */

			 findInforIssuer = new Document("_id", 1)
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
			pipeline.add(new Document("$match", new Document("_id", objectId).append("IsActive", true)
					.append("IsDelete", new Document("$ne", true))));
			pipeline.add(new Document("$project", findInforIssuer));
			
			pipeline.add(new Document("$lookup", new Document("from", "Users").append("pipeline", Arrays.asList(
					new Document("$match",
							new Document("IssuerId", header.getIssuerId()).append("_id", objectIdUser)
									.append("IsActive", true).append("IsDelete", new Document("$ne", true))),
					new Document("$project", new Document("_id", 1).append("UserName", 1).append("FullName", 1)),
					new Document("$limit", 1))).append("as", "UserInfo")));
			pipeline.add(new Document("$unwind",
					new Document("path", "$UserInfo").append("preserveNullAndEmptyArrays", true)));
			pipeline.add(new Document("$lookup",
					new Document("from", "PramLink")
							.append("pipeline",
									Arrays.asList(new Document("$match",
											new Document("$expr", new Document("IsDelete", false))),											
											new Document("$project", new Document("_id", 1).append("LinkPortal", 1))
											))
							.append("as", "PramLink")));
			pipeline.add(new Document("$unwind",
					new Document("path", "$PramLink").append("preserveNullAndEmptyArrays", true)));
			pipeline.add(new Document("$lookup",
					new Document("from", "DMMauSoKyHieu")
							.append("pipeline",
									Arrays.asList(new Document("$match", new Document("IssuerId", header.getIssuerId())
											.append("IsActive", true).append("IsDelete", new Document("$ne", true))
											.append("ConLai", new Document("$gt", 0)).append("_id", objectIdMSKH)),
											new Document("$project", new Document("_id", 1).append("KHMSHDon", 1).append("KHHDon", 1))
											))
							.append("as", "DMMauSoKyHieu")));
			pipeline.add(new Document("$unwind",
					new Document("path", "$DMMauSoKyHieu").append("preserveNullAndEmptyArrays", true)));

			// DANH SACH NGUOI MUA
			pipeline.add(new Document("$lookup",
					new Document("from", "QLDSNMua")
							.append("pipeline",
									Arrays.asList(new Document("$match",
											new Document("MST", khMst).append("IsDelete", new Document("$ne", true)))))
							.append("as", "DSNMua")));
			pipeline.add(new Document("$unwind",
					new Document("path", "$DSNMua").append("preserveNullAndEmptyArrays", true)));

			// CHECK NGAY LAP SO HOA DON LON NHAT
			pipeline.add(
					new Document("$lookup",
							new Document("from", "EInvoicePXKDL")
									.append("pipeline", Arrays.asList(
											new Document("$match",
													new Document("IsDelete", new Document("$ne", true))
															.append("EInvoiceDetail.TTChung.MauSoHD", mauSoHdon)),
											new Document("$group",
													new Document("_id", "$EInvoiceDetail.TTChung.MauSoHD").append(
															"SHDon",
															new Document("$max", "$EInvoiceDetail.TTChung.SHDon")))))
									.append("as", "NLap_MAX")));
			pipeline.add(new Document("$unwind",
					new Document("path", "$NLap_MAX").append("preserveNullAndEmptyArrays", true)));
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
					.append("EInvoiceDetail.TTChung.MauSoHD", nl_mshd).append("IsDelete", new Document("$ne", true));

			docTmp2 = null;

			mongoClient = cfg.mongoClient();
			collection = mongoClient.getDatabase(cfg.dbName).getCollection("EInvoicePXKDL");
			try {
				docTmp2 = collection.find(docFindNLap).allowDiskUse(true).iterator().next();
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
			intTmp = 0;
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

			elementSubContent.appendChild(commons.createElementWithValue(doc, "PBan", SystemParams.VERSION_XML_PXK));
			elementSubContent.appendChild(commons.createElementWithValue(doc, "THDon", tenLoaiHd));
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
			// MST tổ chức cung cấp giải pháp HĐĐT
			elementSubContent.appendChild(commons.createElementWithValue(doc, "MSTTCGP", SystemParams.MSTTCGP));

			elementTmp = doc.createElement("TTKhac"); // THONG TIN KHAC
			elementTmp.appendChild(commons.createElementTTKhac(doc, "PortalLink", "string", link));
			elementTmp.appendChild(commons.createElementTTKhac(doc, "SecureKey", "string", secureKey));
			elementTmp
					.appendChild(commons.createElementTTKhac(doc, "SystemKey", "string", objectIdEInvoice.toString()));

			elementSubContent.appendChild(elementTmp);

			elementContent.appendChild(elementSubContent);
			// NDHDon: Nội dung hóa đơn
			elementSubContent = doc.createElement("NDHDon");
			elementTmp = doc.createElement("NBan"); // NGUOI BAN
			elementTmp.appendChild(commons.createElementWithValue(doc, "Ten", docTmp.get("Name", "")));
			elementTmp.appendChild(commons.createElementWithValue(doc, "MST", docTmp.get("TaxCode", "")));
			// hợp đồng kinh tế số
			elementTmp.appendChild(commons.createElementWithValue(doc, "HDKTSo", HDKTSo));
			elementTmp.appendChild(
					commons.createElementWithValue(doc, "HDKTNgay", commons.convertLocalDateTimeStringToString(HDKTNgay,
							Constants.FORMAT_DATE.FORMAT_DATE_WEB, Constants.FORMAT_DATE.FORMAT_DATE_EINVOICE, false)));
			elementTmp.appendChild(commons.createElementWithValue(doc, "DChi", docTmp.get("Address", "")));
			elementTmp.appendChild(commons.createElementWithValue(doc, "HDSo", docTmp.get("HDSo", "")));
			elementTmp.appendChild(commons.createElementWithValue(doc, "HVTNXHang", khHoTenNguoiXuat));
			elementTmp.appendChild(commons.createElementWithValue(doc, "TNVChuyen", khHoTenNguoiVC));
			elementTmp.appendChild(commons.createElementWithValue(doc, "PTVChuyen", PTVChuyen));
//			elementTmp.appendChild(commons.createElementWithValue(doc, "TTKhac", ""));

			/* ADD THONG TIN TK NGAN HANG (NEU CO) */
			elementSubTmp = doc.createElement("TTKhac");
			elementSubTmp.appendChild(commons.createElementTTKhac(doc, "ComAddress", "string", DChi));
			elementSubTmp.appendChild(commons.createElementTTKhac(doc, "ComPhone", "string", docTmp.get("Phone", "")));
			elementSubTmp.appendChild(commons.createElementTTKhac(doc, "ComEmail", "string", docTmp.get("Email", "")));
			elementSubTmp.appendChild(commons.createElementTTKhac(doc, "STKNHang", "string",
					docTmp.getEmbedded(Arrays.asList("BankAccount", "AccountNumber"), "")));
			elementSubTmp.appendChild(commons.createElementTTKhac(doc, "TNHang", "string",
					docTmp.getEmbedded(Arrays.asList("BankAccount", "BankName"), "")));
			elementSubTmp.appendChild(commons.createElementTTKhac(doc, "ComFax", "string", docTmp.get("Fax", "")));
			elementSubTmp.appendChild(commons.createElementTTKhac(doc, "PostBy", "string", TNDDien));
			elementSubTmp.appendChild(commons.createElementTTKhac(doc, "PostDescription", "string", NDXKho));
			elementSubTmp.appendChild(commons.createElementTTKhac(doc, "ComFax", "string", docTmp.get("Fax", "")));
			elementSubTmp.appendChild(commons.createElementTTKhac(doc, "PostBy", "string", TNDDien));
			elementSubTmp.appendChild(commons.createElementTTKhac(doc, "PostDescription", "string", NDXKho));
			elementTmp.appendChild(elementSubTmp);
			elementSubContent.appendChild(elementTmp);

			elementTmp = doc.createElement("NMua"); // NGUOI MUA
			elementTmp.appendChild(commons.createElementWithValue(doc, "Ten", khTenDonVi));
			elementTmp.appendChild(commons.createElementWithValue(doc, "MST", khMst));
			elementTmp.appendChild(commons.createElementWithValue(doc, "DChi", khDiaChi));
			elementTmp.appendChild(commons.createElementWithValue(doc, "HVTNMHang", khHoTenNguoiXuat));

			elementSubTmp = doc.createElement("TTKhac"); // THONG TIN KHAC
			elementSubTmp.appendChild(commons.createElementTTKhac(doc, "MoveNo", "string", HDKTSo));
			elementSubTmp.appendChild(commons.createElementTTKhac(doc, "MoveDate", "string",
					commons.convertLocalDateTimeStringToString(ngayLap, Constants.FORMAT_DATE.FORMAT_DATE_WEB,
							Constants.FORMAT_DATE.FORMAT_DATE_EINVOICE, false)));
			elementSubTmp.appendChild(commons.createElementTTKhac(doc, "TransportDivice", "string", PTVChuyen));
			elementSubTmp.appendChild(commons.createElementTTKhac(doc, "TransportName", "string", khHoTenNguoiVC));
			elementTmp.appendChild(elementSubTmp);
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
						case "5":
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

						mapAmount.compute(tmp, (k, v) -> {
							return (v == null ? commons.ToNumber(commons.getTextJsonNode(o.at("/Total")))
									: v + commons.ToNumber(commons.getTextJsonNode(o.at("/Total"))));
						});
						mapVATAmount.compute(tmp, (k, v) -> {
							return (v == null ? commons.ToNumber(commons.getTextJsonNode(o.at("/VATAmount")))
									: v + commons.ToNumber(commons.getTextJsonNode(o.at("/VATAmount"))));
						});

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
						elementSubTmp.appendChild(commons.createElementWithValue(doc, "SLuong",
								commons.getTextJsonNode(o.at("/Quantity")).replaceAll(",", "")));
						elementSubTmp.appendChild(commons.createElementWithValue(doc, "DGia",
								commons.getTextJsonNode(o.at("/Price")).replaceAll(",", "")));
						elementSubTmp.appendChild(commons.createElementWithValue(doc, "ThTien",
								commons.getTextJsonNode(o.at("/Total")).replaceAll(",", "")));
						elementSubTmp.appendChild(commons.createElementWithValue(doc, "TSuat", "0%"));

						elementSubTmp01 = doc.createElement("TTKhac");
						elementSubTmp01.appendChild(commons.createElementTTKhac(doc, "SLXuat", "String",
								commons.getTextJsonNode(o.at("/Total")).replaceAll(",", "")));
						elementSubTmp01.appendChild(commons.createElementTTKhac(doc, "VATAmount", "decimal",
								commons.getTextJsonNode(o.at("/VATAmount")).replaceAll(",", "")));
						elementSubTmp01.appendChild(commons.createElementTTKhac(doc, "Amount", "decimal",
								commons.getTextJsonNode(o.at("/Amount")).replaceAll(",", "")));
						elementSubTmp.appendChild(elementSubTmp01);

						elementTmp.appendChild(elementSubTmp);

						hItem = new LinkedHashMap<String, Object>();
						hItem.put("STT", commons.getTextJsonNode(o.at("/STT")));
						hItem.put("ProductName", commons.getTextJsonNode(o.at("/ProductName")));
						hItem.put("ProductCode", commons.getTextJsonNode(o.at("/ProductCode")));
						hItem.put("Unit", commons.getTextJsonNode(o.at("/Unit")));
						hItem.put("Quantity", commons.ToNumber(commons.getTextJsonNode(o.at("/Quantity"))));
						hItem.put("Price", commons.ToNumber(commons.getTextJsonNode(o.at("/Price"))));
						hItem.put("Total", commons.ToNumber(commons.getTextJsonNode(o.at("/Total"))));
						hItem.put("Amount", commons.ToNumber(commons.getTextJsonNode(o.at("/Amount"))));
						hItem.put("Feature", commons.getTextJsonNode(o.at("/Feature")));
						listDSHHDVu.add(hItem);

					}

				}
			}
			elementSubContent.appendChild(elementTmp);

			elementTmp = doc.createElement("TToan"); // Thong tin thanh toan
			elementSubTmp = doc.createElement("THTTLTSuat");
			/* DANH SACH CAC LOAI THUE SUAT */

//			https://stackoverflow.com/questions/46898/how-do-i-efficiently-iterate-over-each-entry-in-a-java-map
			for (Map.Entry<String, Double> pair : mapVATAmount.entrySet()) {
				elementSubTmp01 = doc.createElement("LTSuat");
				elementSubTmp01.appendChild(commons.createElementWithValue(doc, "TSuat", "KCT"));
				elementSubTmp01.appendChild(commons.createElementWithValue(doc, "ThTien",
						commons.formatNumberReal(mapAmount.get(pair.getKey())).replaceAll(",", "")));
				elementSubTmp01.appendChild(commons.createElementWithValue(doc, "TThue",
						commons.formatNumberReal(mapVATAmount.get(pair.getKey())).replaceAll(",", "")));
				elementSubTmp.appendChild(elementSubTmp01);
			}
			elementTmp.appendChild(elementSubTmp);

			elementTmp.appendChild(
					commons.createElementWithValue(doc, "TgTCThue", tongTienTruocThue.replaceAll(",", "")));
			elementTmp
					.appendChild(commons.createElementWithValue(doc, "TgTThue", tongTienThueGtgt.replaceAll(",", "")));
			elementTmp.appendChild(
					commons.createElementWithValue(doc, "TTCKTMai", tongTienTruocThue.replaceAll(",", "")));
			elementTmp
					.appendChild(commons.createElementWithValue(doc, "TgTTTBSo", tongTienDaCoThue.replaceAll(",", "")));
			elementTmp.appendChild(commons.createElementWithValue(doc, "TgTTTBChu", tienBangChu));

			elementContent.appendChild(elementSubContent);
			elementSubContent.appendChild(elementTmp);

			// END - NDHDon: Nội dung hóa đơn

			isSdaveFile = commons.docW3cToFile(doc, pathDir, fileNameXML);
			if (!isSdaveFile) {
				throw new Exception("Lưu dữ liệu không thành công.");
			}
			/* END - TAO XML HOA DON */

			MTDiep = SystemParams.MSTTCGP
					+ commons.csRandomAlphaNumbericString(46 - SystemParams.MSTTCGP.length()).toUpperCase();
			/* LUU DU LIEU HD */

			docUpsert = new Document("_id", objectIdEInvoice).append("IssuerId", header.getIssuerId())
					.append("MTDiep", MTDiep)
//				.append("EInvoiceNumber", null)				//PHAT SINH KHI THUC HIEN KY
					.append("EInvoiceDetail", new Document("TTChung", new Document("THDon", tenLoaiHd)
							.append("MauSoHD", mauSoHdon)
							.append("KHMSHDon", docTmp.getEmbedded(Arrays.asList("DMMauSoKyHieu", "KHMSHDon"), ""))
							.append("KHHDon", docTmp.getEmbedded(Arrays.asList("DMMauSoKyHieu", "KHHDon"), ""))
							.append("NLap",
									commons.convertStringToLocalDate(ngayLap, Constants.FORMAT_DATE.FORMAT_DATE_WEB))
							.append("DVTTe", loaiTienTt).append("TGia", tyGia).append("HTTToanCode", hinhThucThanhToan)
							.append("HTTToan", hinhThucThanhToanText).append("TTHDLQuan", docTTHDLQuan))
							.append("NDHDon", new Document("NBan", new Document("Ten", docTmp.get("Name", ""))
									.append("MST", docTmp.get("TaxCode", "")).append("HDKTSo", HDKTSo)
									.append("HDKTNgay",
											commons.convertStringToLocalDate(HDKTNgay,
													Constants.FORMAT_DATE.FORMAT_DATE_WEB))
									.append("DChi", DChi).append("SDThoai", docTmp.get("Phone", ""))
									.append("TNVChuyen", khHoTenNguoiVC).append("PTVChuyen", PTVChuyen)
									.append("TNDDien", TNDDien).append("DCTDTu", docTmp.get("Email", ""))
									.append("STKNHang",
											docTmp.getEmbedded(Arrays.asList("BankAccount", "AccountNumber"), ""))
									.append("TNHang", docTmp.getEmbedded(Arrays.asList("BankAccount", "BankName"), ""))
									.append("Fax", docTmp.get("Fax", "")).append("Website", docTmp.get("Website", "")))
									.append("NMua",
											new Document("Ten", khTenDonVi).append("MST", khMst)
													.append("DChi", khDiaChi).append("NDXKho", NDXKho)
													.append("MKHang", "").append("SDThoai", khSoDT)
													.append("DCTDTu", khEmail).append("HVTNMHang", khHoTenNguoiXuat)
													.append("STKNHang", khSoTk).append("TNHang", khTkTaiNganHang)))
							.append("DSHHDVu", listDSHHDVu)
//					.append("DSHHDVu", 
//						jsonData.at("/DSSanPham").isMissingNode()?
//						new ArrayList<Object>():
//						Json.serializer().fromNode(jsonData.at("/DSSanPham"), new TypeReference<List<?>>() {
//						})
//					)
							.append("TToan",
									new Document("TgTCThue", commons.ToNumber(tongTienTruocThue))
											.append("TgTThue", commons.ToNumber(tongTienThueGtgt))
											.append("TgTTTBSo", commons.ToNumber(tongTienDaCoThue))
											.append("TgTTTBChu", tienBangChu)))
					.append("SignStatusCode", Constants.INVOICE_SIGN_STATUS.NOSIGN)
					.append("EInvoiceStatus", Constants.INVOICE_STATUS.CREATED).append("IsDelete", false)
					.append("SecureKey", secureKey).append("Dir", pathDir).append("FileNameXML", fileNameXML)
					.append("InfoCreated",
							new Document("CreateDate", LocalDateTime.now()).append("CreateUserID", header.getUserId())
									.append("CreateUserName", header.getUserName())
									.append("CreateUserFullName", header.getUserFullName()));
			/* END - LUU DU LIEU HD */

			mongoClient = cfg.mongoClient();
			collection = mongoClient.getDatabase(cfg.dbName).getCollection("EInvoicePXKDL");
			collection.insertOne(docUpsert);
			mongoClient.close();

			// Start replace, adjusted
			/* KIEM TRA THONG TIN HD CO TON TAI KHONG */
			docFind1 = new Document("IssuerId", header.getIssuerId()).append("IsDelete", new Document("$ne", true))
					.append("_id", objectIdTT_DC).append("SignStatusCode", Constants.INVOICE_SIGN_STATUS.SIGNED)
					.append("EInvoiceStatus", new Document("$in", Arrays.asList(Constants.INVOICE_STATUS.COMPLETE,
							Constants.INVOICE_STATUS.ADJUSTED, Constants.INVOICE_STATUS.REPLACED)));
			options = new FindOneAndUpdateOptions();
			options.upsert(true);
			options.maxTime(5000, TimeUnit.MILLISECONDS);
			options.returnDocument(ReturnDocument.AFTER);

			// End replace, adjusted

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
							new Document("$set",
									new Document("Ten", khTenDonVi)
											.append("NLap",
													commons.convertStringToLocalDate(ngayLap,
															Constants.FORMAT_DATE.FORMAT_DATE_WEB))
											.append("MST", khMst).append("DChi", khDiaChi).append("SDThoai", phone_nm)
											.append("DCTDTu", email_nm).append("IsDelete", false)),
							options);
					mongoClient.close();

				}
				// END KIEM TRA XEM KHACH HANG DA CO TRONG DANH SACH QUAN LY
			}
			format_time = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
			time_dem = LocalDateTime.now();
			time = time_dem.format(format_time);
			name_company = removeAccent(header.getUserFullName());
			System.out.println(time + " " + name_company + " vua copy phieu ki gui dai ly");
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

			 findInforIssuer = new Document("_id", 1)
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
			 
				Document fillter = new Document("_id", 1)
						.append("Dir", 1)
						.append("FileNameXML", 1)
						.append("EInvoiceStatus", 1)
						.append("SignStatusCode", 1)
						.append("SecureKey", 1)
						.append("MCCQT", 1)
						.append("MTDiep", 1)
						.append("EInvoiceDetail", 1);
				
			pipeline = new ArrayList<Document>();
			pipeline.add(new Document("$match", new Document("_id", objectId).append("IsActive", true)
					.append("IsDelete", new Document("$ne", true))));
			pipeline.add(new Document("$project", findInforIssuer));
			
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
			pipeline.add(new Document("$lookup", new Document("from", "EInvoicePXKDL")
					.append("pipeline", Arrays.asList(new Document("$match", docFind),
					new Document("$project",fillter)	
							)).append("as", "EInvoicePXKDL")));
			pipeline.add(new Document("$unwind",
					new Document("path", "$EInvoicePXKDL").append("preserveNullAndEmptyArrays", true)));

			pipeline.add(new Document("$lookup",
					new Document("from", "PramLink")
							.append("pipeline",
									Arrays.asList(new Document("$match",
											new Document("$expr", new Document("IsDelete", false))),
											new Document("$project", new Document("_id", 1).append("LinkPortal", 1))
											))
							.append("as", "PramLink")));
			pipeline.add(new Document("$unwind",
					new Document("path", "$PramLink").append("preserveNullAndEmptyArrays", true)));
			// DANH SACH NGUOI MUA
			pipeline.add(new Document("$lookup",
					new Document("from", "QLDSNMua")
							.append("pipeline",
									Arrays.asList(new Document("$match",
											new Document("MST", khMst).append("IsDelete", new Document("$ne", true)))))
							.append("as", "DSNMua")));
			pipeline.add(new Document("$unwind",
					new Document("path", "$DSNMua").append("preserveNullAndEmptyArrays", true)));

			// CHECK NGAY LAP SO HOA DON LON NHAT
			pipeline.add(
					new Document("$lookup",
							new Document("from", "EInvoicePXKDL")
									.append("pipeline", Arrays.asList(
											new Document("$match",
													new Document("IsDelete", new Document("$ne", true))
															.append("EInvoiceDetail.TTChung.MauSoHD", mauSoHdon)),
											new Document("$group",
													new Document("_id", "$EInvoiceDetail.TTChung.MauSoHD").append(
															"SHDon",
															new Document("$max", "$EInvoiceDetail.TTChung.SHDon")))))
									.append("as", "NLap_MAX")));
			pipeline.add(new Document("$unwind",
					new Document("path", "$NLap_MAX").append("preserveNullAndEmptyArrays", true)));
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
			if (docTmp.get("UserInfo") == null) {
				responseStatus = new MspResponseStatus(9999, "Không tìm thấy thông tin người dùng.");
				rsp.setResponseStatus(responseStatus);
				return rsp;
			}
			if (docTmp.get("EInvoicePXKDL") == null) {
				responseStatus = new MspResponseStatus(9999,
						"Không tìm thấy thông tin phiếu xuất kho kiêm vận chuyển cần cập nhật.");
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
					.append("EInvoiceDetail.TTChung.MauSoHD", nl_mshd).append("IsDelete", new Document("$ne", true));

			docTmp2 = null;

			mongoClient = cfg.mongoClient();
			collection = mongoClient.getDatabase(cfg.dbName).getCollection("EInvoicePXKDL");
			try {
				docTmp2 = collection.find(docFindNLap).allowDiskUse(true).iterator().next();
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

			docTTHDLQuan = docTmp.getEmbedded(Arrays.asList("EInvoicePXKDL", "EInvoiceDetail", "TTChung", "TTHDLQuan"),
					Document.class);

			taxCode = docTmp.getString("TaxCode");
			pathDir = docTmp.getEmbedded(Arrays.asList("EInvoicePXKDL", "Dir"), "");
			fileNameXML = docTmp.getEmbedded(Arrays.asList("EInvoicePXKDL", "FileNameXML"), "");

			file = new File(pathDir);
			if (!file.exists())
				file.mkdirs();

			String KHMSHDon = docTmp
					.getEmbedded(Arrays.asList("EInvoicePXKDL", "EInvoiceDetail", "TTChung", "KHMSHDon"), "");
			String KHHDon = docTmp.getEmbedded(Arrays.asList("EInvoicePXKDL", "EInvoiceDetail", "TTChung", "KHHDon"),
					"");
			int shd = docTmp.getEmbedded(Arrays.asList("EInvoicePXKDL", "EInvoiceDetail", "TTChung", "SHDon"), 0);

			secureKey = docTmp.getEmbedded(Arrays.asList("EInvoicePXKDL", "SecureKey"), "");

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

			elementSubContent.appendChild(commons.createElementWithValue(doc, "PBan", SystemParams.VERSION_XML_PXK));
			elementSubContent.appendChild(commons.createElementWithValue(doc, "THDon", tenLoaiHd));
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
			// MST tổ chức cung cấp giải pháp HĐĐT
			elementSubContent.appendChild(commons.createElementWithValue(doc, "MSTTCGP", SystemParams.MSTTCGP));

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
			// hợp đồng kinh tế số
			elementTmp.appendChild(commons.createElementWithValue(doc, "HDKTSo", HDKTSo));
			elementTmp.appendChild(
					commons.createElementWithValue(doc, "HDKTNgay", commons.convertLocalDateTimeStringToString(HDKTNgay,
							Constants.FORMAT_DATE.FORMAT_DATE_WEB, Constants.FORMAT_DATE.FORMAT_DATE_EINVOICE, false)));
			elementTmp.appendChild(commons.createElementWithValue(doc, "DChi", docTmp.get("Address", "")));
			elementTmp.appendChild(commons.createElementWithValue(doc, "HDSo", docTmp.get("HDSo", "")));
			elementTmp.appendChild(commons.createElementWithValue(doc, "HVTNXHang", khHoTenNguoiXuat));
			elementTmp.appendChild(commons.createElementWithValue(doc, "TNVChuyen", khHoTenNguoiVC));
			elementTmp.appendChild(commons.createElementWithValue(doc, "PTVChuyen", PTVChuyen));
//			elementTmp.appendChild(commons.createElementWithValue(doc, "TTKhac", ""));

			/* ADD THONG TIN TK NGAN HANG (NEU CO) */
			elementSubTmp = doc.createElement("TTKhac");
			elementSubTmp.appendChild(commons.createElementTTKhac(doc, "ComAddress", "string", DChi));
			elementSubTmp.appendChild(commons.createElementTTKhac(doc, "ComPhone", "string", docTmp.get("Phone", "")));
			elementSubTmp.appendChild(commons.createElementTTKhac(doc, "ComEmail", "string", docTmp.get("Email", "")));
			elementSubTmp.appendChild(commons.createElementTTKhac(doc, "STKNHang", "string",
					docTmp.getEmbedded(Arrays.asList("BankAccount", "AccountNumber"), "")));
			elementSubTmp.appendChild(commons.createElementTTKhac(doc, "TNHang", "string",
					docTmp.getEmbedded(Arrays.asList("BankAccount", "BankName"), "")));
			elementSubTmp.appendChild(commons.createElementTTKhac(doc, "ComFax", "string", docTmp.get("Fax", "")));
			elementSubTmp.appendChild(commons.createElementTTKhac(doc, "PostBy", "string", TNDDien));
			elementSubTmp.appendChild(commons.createElementTTKhac(doc, "PostDescription", "string", NDXKho));
			elementSubTmp.appendChild(commons.createElementTTKhac(doc, "ComFax", "string", docTmp.get("Fax", "")));
			elementSubTmp.appendChild(commons.createElementTTKhac(doc, "PostBy", "string", TNDDien));
			elementSubTmp.appendChild(commons.createElementTTKhac(doc, "PostDescription", "string", NDXKho));
			elementTmp.appendChild(elementSubTmp);
			elementSubContent.appendChild(elementTmp);

			elementTmp = doc.createElement("NMua"); // NGUOI MUA
			elementTmp.appendChild(commons.createElementWithValue(doc, "Ten", khTenDonVi));
			elementTmp.appendChild(commons.createElementWithValue(doc, "MST", khMst));
			elementTmp.appendChild(commons.createElementWithValue(doc, "DChi", khDiaChi));
			elementTmp.appendChild(commons.createElementWithValue(doc, "HVTNMHang", khHoTenNguoiXuat));

			elementSubTmp = doc.createElement("TTKhac"); // THONG TIN KHAC
			elementSubTmp.appendChild(commons.createElementTTKhac(doc, "MoveNo", "string", HDKTSo));
			elementSubTmp.appendChild(commons.createElementTTKhac(doc, "MoveDate", "string",
					commons.convertLocalDateTimeStringToString(ngayLap, Constants.FORMAT_DATE.FORMAT_DATE_WEB,
							Constants.FORMAT_DATE.FORMAT_DATE_EINVOICE, false)));
			elementSubTmp.appendChild(commons.createElementTTKhac(doc, "TransportDivice", "string", PTVChuyen));
			elementSubTmp.appendChild(commons.createElementTTKhac(doc, "TransportName", "string", khHoTenNguoiVC));
			elementTmp.appendChild(elementSubTmp);
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
						case "5":
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

						mapAmount.compute(tmp, (k, v) -> {
							return (v == null ? commons.ToNumber(commons.getTextJsonNode(o.at("/Total")))
									: v + commons.ToNumber(commons.getTextJsonNode(o.at("/Total"))));
						});
						mapVATAmount.compute(tmp, (k, v) -> {
							return (v == null ? commons.ToNumber(commons.getTextJsonNode(o.at("/VATAmount")))
									: v + commons.ToNumber(commons.getTextJsonNode(o.at("/VATAmount"))));
						});

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
						elementSubTmp.appendChild(commons.createElementWithValue(doc, "SLuong",
								commons.getTextJsonNode(o.at("/Quantity")).replaceAll(",", "")));
						elementSubTmp.appendChild(commons.createElementWithValue(doc, "DGia",
								commons.getTextJsonNode(o.at("/Price")).replaceAll(",", "")));
						elementSubTmp.appendChild(commons.createElementWithValue(doc, "ThTien",
								commons.getTextJsonNode(o.at("/Total")).replaceAll(",", "")));
						elementSubTmp.appendChild(commons.createElementWithValue(doc, "TSuat", "0%"));

						elementSubTmp01 = doc.createElement("TTKhac");
						elementSubTmp01.appendChild(commons.createElementTTKhac(doc, "SLXuat", "String",
								commons.getTextJsonNode(o.at("/Total")).replaceAll(",", "")));
						elementSubTmp01.appendChild(commons.createElementTTKhac(doc, "VATAmount", "decimal",
								commons.getTextJsonNode(o.at("/VATAmount")).replaceAll(",", "")));
						elementSubTmp01.appendChild(commons.createElementTTKhac(doc, "Amount", "decimal",
								commons.getTextJsonNode(o.at("/Amount")).replaceAll(",", "")));
						elementSubTmp.appendChild(elementSubTmp01);

						elementTmp.appendChild(elementSubTmp);

						hItem = new LinkedHashMap<String, Object>();
						hItem.put("STT", commons.getTextJsonNode(o.at("/STT")));
						hItem.put("ProductName", commons.getTextJsonNode(o.at("/ProductName")));
						hItem.put("ProductCode", commons.getTextJsonNode(o.at("/ProductCode")));
						hItem.put("Unit", commons.getTextJsonNode(o.at("/Unit")));
						hItem.put("Quantity", commons.ToNumber(commons.getTextJsonNode(o.at("/Quantity"))));
						hItem.put("Price", commons.ToNumber(commons.getTextJsonNode(o.at("/Price"))));
						hItem.put("Total", commons.ToNumber(commons.getTextJsonNode(o.at("/Total"))));
						hItem.put("Amount", commons.ToNumber(commons.getTextJsonNode(o.at("/Amount"))));
						hItem.put("Feature", commons.getTextJsonNode(o.at("/Feature")));
						listDSHHDVu.add(hItem);

					}

				}
			}
			elementSubContent.appendChild(elementTmp);

			elementTmp = doc.createElement("TToan"); // Thong tin thanh toan
			elementSubTmp = doc.createElement("THTTLTSuat");
			/* DANH SACH CAC LOAI THUE SUAT */

//			https://stackoverflow.com/questions/46898/how-do-i-efficiently-iterate-over-each-entry-in-a-java-map
			for (Map.Entry<String, Double> pair : mapVATAmount.entrySet()) {
				elementSubTmp01 = doc.createElement("LTSuat");
				elementSubTmp01.appendChild(commons.createElementWithValue(doc, "TSuat", "KCT"));
				elementSubTmp01.appendChild(commons.createElementWithValue(doc, "ThTien",
						commons.formatNumberReal(mapAmount.get(pair.getKey())).replaceAll(",", "")));
				elementSubTmp01.appendChild(commons.createElementWithValue(doc, "TThue",
						commons.formatNumberReal(mapVATAmount.get(pair.getKey())).replaceAll(",", "")));
				elementSubTmp.appendChild(elementSubTmp01);
			}
			elementTmp.appendChild(elementSubTmp);

			elementTmp.appendChild(
					commons.createElementWithValue(doc, "TgTCThue", tongTienTruocThue.replaceAll(",", "")));
			elementTmp
					.appendChild(commons.createElementWithValue(doc, "TgTThue", tongTienThueGtgt.replaceAll(",", "")));
			elementTmp.appendChild(
					commons.createElementWithValue(doc, "TTCKTMai", tongTienTruocThue.replaceAll(",", "")));
			elementTmp
					.appendChild(commons.createElementWithValue(doc, "TgTTTBSo", tongTienDaCoThue.replaceAll(",", "")));
			elementTmp.appendChild(commons.createElementWithValue(doc, "TgTTTBChu", tienBangChu));

			elementContent.appendChild(elementSubContent);
			elementSubContent.appendChild(elementTmp);

			// END - NDHDon: Nội dung hóa đơn

			isSdaveFile = commons.docW3cToFile(doc, pathDir, fileNameXML);
			if (!isSdaveFile) {
				throw new Exception("Lưu dữ liệu không thành công.");
			}
			/* END - TAO XML HOA DON */
			if (shd == 0) {
				/* LUU DU LIEU HD */
				docUpsert = new Document("TTChung", new Document("THDon", tenLoaiHd).append("MauSoHD", mauSoHdon)
						.append("KHMSHDon", KHMSHDon).append("KHHDon", KHHDon)
						.append("NLap",
								commons.convertStringToLocalDate(ngayLap, Constants.FORMAT_DATE.FORMAT_DATE_WEB))
						.append("DVTTe", loaiTienTt).append("TGia", tyGia).append("HTTToanCode", hinhThucThanhToan)
						.append("HTTToan", hinhThucThanhToanText).append("TTHDLQuan", docTTHDLQuan))
						.append("NDHDon",
								new Document("NBan", new Document("Ten", docTmp.get("Name", ""))
										.append("MST", docTmp.get("TaxCode", "")).append("HDKTSo", HDKTSo)
										.append("HDKTNgay",
												commons.convertStringToLocalDate(HDKTNgay,
														Constants.FORMAT_DATE.FORMAT_DATE_WEB))
										.append("DChi", DChi).append("SDThoai", docTmp.get("Phone", ""))
										.append("TNVChuyen", khHoTenNguoiVC).append("PTVChuyen", PTVChuyen)
										.append("TNDDien", TNDDien).append("DCTDTu", docTmp.get("Email", ""))
										.append("STKNHang",
												docTmp.getEmbedded(Arrays.asList("BankAccount", "AccountNumber"), ""))
										.append("TNHang",
												docTmp.getEmbedded(Arrays.asList("BankAccount", "BankName"), ""))
										.append("Fax", docTmp.get("Fax", ""))
										.append("Website", docTmp.get("Website", "")))
										.append("NMua",
												new Document("Ten", khTenDonVi).append("MST", khMst)
														.append("DChi", khDiaChi).append("NDXKho", NDXKho)
														.append("MKHang", "").append("SDThoai", khSoDT)
														.append("DCTDTu", khEmail).append("HVTNMHang", khHoTenNguoiXuat)
														.append("STKNHang", khSoTk).append("TNHang", khTkTaiNganHang)))
						.append("DSHHDVu", listDSHHDVu).append("TToan",
								new Document("TgTCThue", commons.ToNumber(tongTienTruocThue))
										.append("TgTThue", commons.ToNumber(tongTienThueGtgt))
										.append("TgTTTBSo", commons.ToNumber(tongTienDaCoThue))
										.append("TgTTTBChu", tienBangChu));
				/* END - LUU DU LIEU HD */
			} else {
				/* LUU DU LIEU HD */
				docUpsert = new Document("TTChung", new Document("THDon", tenLoaiHd).append("MauSoHD", mauSoHdon)
						.append("KHMSHDon", KHMSHDon).append("KHHDon", KHHDon)
						.append("NLap",
								commons.convertStringToLocalDate(ngayLap, Constants.FORMAT_DATE.FORMAT_DATE_WEB))
						.append("DVTTe", loaiTienTt).append("TGia", tyGia).append("HTTToanCode", hinhThucThanhToan)
						.append("HTTToan", hinhThucThanhToanText).append("TTHDLQuan", docTTHDLQuan)
						.append("SHDon", shd))
						.append("NDHDon",
								new Document("NBan", new Document("Ten", docTmp.get("Name", ""))
										.append("MST", docTmp.get("TaxCode", "")).append("HDKTSo", HDKTSo)
										.append("HDKTNgay",
												commons.convertStringToLocalDate(HDKTNgay,
														Constants.FORMAT_DATE.FORMAT_DATE_WEB))
										.append("DChi", DChi).append("SDThoai", docTmp.get("Phone", ""))
										.append("TNVChuyen", khHoTenNguoiVC).append("PTVChuyen", PTVChuyen)
										.append("TNDDien", TNDDien).append("DCTDTu", docTmp.get("Email", ""))
										.append("STKNHang",
												docTmp.getEmbedded(Arrays.asList("BankAccount", "AccountNumber"), ""))
										.append("TNHang",
												docTmp.getEmbedded(Arrays.asList("BankAccount", "BankName"), ""))
										.append("Fax", docTmp.get("Fax", ""))
										.append("Website", docTmp.get("Website", "")))
										.append("NMua",
												new Document("Ten", khTenDonVi).append("MST", khMst)
														.append("DChi", khDiaChi).append("NDXKho", NDXKho)
														.append("MKHang", "").append("SDThoai", khSoDT)
														.append("DCTDTu", khEmail).append("HVTNMHang", khHoTenNguoiXuat)
														.append("STKNHang", khSoTk).append("TNHang", khTkTaiNganHang)))
						.append("DSHHDVu", listDSHHDVu).append("TToan",
								new Document("TgTCThue", commons.ToNumber(tongTienTruocThue))
										.append("TgTThue", commons.ToNumber(tongTienThueGtgt))
										.append("TgTTTBSo", commons.ToNumber(tongTienDaCoThue))
										.append("TgTTTBChu", tienBangChu));
				/* END - LUU DU LIEU HD */
			}

			options = new FindOneAndUpdateOptions();
			options.upsert(false);
			options.maxTime(5000, TimeUnit.MILLISECONDS);
			options.returnDocument(ReturnDocument.AFTER);

			mongoClient = cfg.mongoClient();
			collection = mongoClient.getDatabase(cfg.dbName).getCollection("EInvoicePXKDL");
			collection.findOneAndUpdate(docFind,
					new Document("$set", new Document("EInvoiceDetail", docUpsert).append("InfoUpdated",
							new Document("UpdatedDate", LocalDateTime.now()).append("UpdatedUserID", header.getUserId())
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
							new Document("$set",
									new Document("Ten", khTenDonVi)
											.append("NLap",
													commons.convertStringToLocalDate(ngayLap,
															Constants.FORMAT_DATE.FORMAT_DATE_WEB))
											.append("MST", khMst).append("DChi", khDiaChi).append("SDThoai", phone_nm)
											.append("DCTDTu", email_nm).append("IsDelete", false)),
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
						new Document("$ne", true));

				mongoClient = cfg.mongoClient();
				collection = mongoClient.getDatabase(cfg.dbName).getCollection("DMHistoryCustomer");
				try {
					docTmp = collection.find(docFind).allowDiskUse(true).iterator().next();
				} catch (Exception e) {
					
				}
				mongoClient.close();
				if (null == docTmp) {
					objectId = null;
					objectId = new ObjectId();
					docUpsert = new Document("_id", objectId).append("IssuerId", header.getIssuerId())
							.append("TaxCode", khMst)

							.append("CompanyName", khTenDonVi)

							.append("Address", khDiaChi).append("Email", khEmail)

							.append("Phone", khSoDT).append("InfoCreated",
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
							new Document("$set", new Document("IssuerId", header.getIssuerId()).append("TaxCode", khMst)

									.append("CompanyName", khTenDonVi)

									.append("Address", khDiaChi).append("Email", khEmail)

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
			System.out.println(time + name_company + " vua thay doi phieu ki gui dai ly");
			responseStatus = new MspResponseStatus(0, "SUCCESS");
			rsp.setResponseStatus(responseStatus);
			return rsp;
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

			docFind = new Document("IsDelete", new Document("$ne", true)).append("_id", new Document("$in", objectIds))
					.append("SignStatusCode", "NOSIGN").append("EInvoiceStatus", "CREATED")
					.append("IssuerId", header.getIssuerId());

			mongoClient = cfg.mongoClient();
			collection = mongoClient.getDatabase(cfg.dbName).getCollection("EInvoicePXKDL");
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
			try {
				objectId = new ObjectId(_id);
			} catch (Exception e) {
			}

		
			/* KIEM TRA THONG TIN HD CO TON TAI KHONG */
			docFind = new Document("IssuerId", header.getIssuerId()).append("IsDelete", new Document("$ne", true))
					.append("_id", objectId).append("SignStatusCode", Constants.INVOICE_SIGN_STATUS.NOSIGN);

			
			fillter = new Document("_id", 1)
					.append("EInvoiceDetail", 1);

			pipeline = new ArrayList<Document>();
			pipeline.add(new Document("$match",docFind));
			pipeline.add(new Document("$project", fillter));
			docTmp = null;

			mongoClient = cfg.mongoClient();
			collection = mongoClient.getDatabase(cfg.dbName).getCollection("EInvoicePXKDL");
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
			String MSKH = docTmp.getEmbedded(Arrays.asList("EInvoiceDetail", "TTChung", "MauSoHD"), "");

			int SHD = docTmp.getEmbedded(Arrays.asList("EInvoiceDetail", "TTChung", "SHDon"), 0);
			if (SHD == 0) {
				// CAP NHAT HOA DON DA XOA TRONG EINVOICE
				options = new FindOneAndUpdateOptions();
				options.upsert(false);
				options.maxTime(5000, TimeUnit.MILLISECONDS);
				options.returnDocument(ReturnDocument.AFTER);

				mongoClient = cfg.mongoClient();
				collection = mongoClient.getDatabase(cfg.dbName).getCollection("EInvoicePXKDL");
				collection.findOneAndUpdate(docFind, new Document("$set", new Document("IsDelete", true).append(
						"InfoDeleted",
						new Document("DeletedDate", LocalDateTime.now()).append("DeletedUserID", header.getUserId())
								.append("DeletedUserName", header.getUserName())
								.append("DeletedUserFullName", header.getUserFullName()))),
						options);
				mongoClient.close();

				responseStatus = new MspResponseStatus(0, "SUCCESS");
				rsp.setResponseStatus(responseStatus);
				return rsp;
			} else {
				objectIdMSKH = new ObjectId(MSKH);
				docFind1 = new Document("IssuerId", header.getIssuerId()).append("IsDelete", new Document("$ne", true))
						.append("_id", objectIdMSKH);

				 Document findMSKH = new Document("_id", 1)
							.append("SHDHT", 1)
							.append("ConLai", 1);
							
								pipeline = new ArrayList<Document>();
					pipeline.add(new Document("$match",docFind1));
					pipeline.add(new Document("$project", findMSKH));
				docTmp1 = null;

				mongoClient = cfg.mongoClient();
				collection = mongoClient.getDatabase(cfg.dbName).getCollection("DMMauSoKyHieu");
				try {
					docTmp1 = collection.aggregate(pipeline).allowDiskUse(true).iterator().next();	
				} catch (Exception e) {
					
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
					collection = mongoClient.getDatabase(cfg.dbName).getCollection("EInvoicePXKDL");
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
					System.out.println(time + " " + name_company + " vua xoa phieu ki gui dai ly");
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
		
			fillter = new Document("_id", 1)
					.append("Dir", 1)
					.append("MTDiep", 1)
					.append("EInvoiceDetail", 1);	
			
			/* KIEM TRA THONG TIN HD CO TON TAI KHONG */
			docFind = new Document("IssuerId", header.getIssuerId()).append("IsDelete", new Document("$ne", true))
					.append("_id", objectId).append("EInvoiceStatus", Constants.INVOICE_STATUS.PENDING)
					.append("SignStatusCode", Constants.INVOICE_SIGN_STATUS.SIGNED);
			pipeline = new ArrayList<Document>();
			pipeline.add(new Document("$match", docFind));
			pipeline.add(new Document("$project", fillter));
			
			pipeline.add(new Document("$lookup", new Document("from", "EInvoicePXKDL")
					.append("let",
							new Document("vIssuerId", "$IssuerId").append("vMauSo", "$EInvoiceDetail.TTChung.MauSoHD"))
					.append("pipeline", Arrays.asList(
							new Document("$match", new Document("$expr", new Document("$and",
									Arrays.asList(new Document("$eq", Arrays.asList("$IssuerId", "$$vIssuerId")),
											new Document("$eq",
													Arrays.asList("$EInvoiceDetail.TTChung.MauSoHD", "$$vMauSo")),
											new Document("$ne", Arrays.asList("$IsDelete", true)),
											new Document("$eq",
													Arrays.asList("$SignStatusCode",
															Constants.INVOICE_SIGN_STATUS.SIGNED)),
											new Document("$in",
													Arrays.asList("$EInvoiceStatus",
															Arrays.asList(Constants.INVOICE_STATUS.COMPLETE,
																	Constants.INVOICE_STATUS.ERROR_CQT))))))),
							new Document("$group",
									new Document("_id", "$EInvoiceDetail.TTChung.MauSoHD").append("SHDon",
											new Document("$max", "$EInvoiceDetail.TTChung.SHDon")))))
					.append("as", "EInvoiceMAXCQT")));
			pipeline.add(new Document("$unwind",
					new Document("path", "$EInvoiceMAXCQT").append("preserveNullAndEmptyArrays", true)));

			docTmp = null;

			mongoClient = cfg.mongoClient();
			collection = mongoClient.getDatabase(cfg.dbName).getCollection("EInvoicePXKDL");
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
//			String codeMTD = "0315382923";
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

			MTDiep = docTmp.get("MTDiep", "");
			MST = docTmp.getEmbedded(Arrays.asList("EInvoiceDetail", "NDHDon", "NBan", "MST"), "");
			rTCTN = tctnService.callTiepNhanThongDiep("200", MTDiep, MST, "1", doc);
			if (rTCTN == null) {
				MTDiep1 = docTmp.get("MTDiep", "");
				rTCTN = tctnService.callTiepNhanThongDiep("200", MTDiep1, MST, "1", doc);
			}

			if (rTCTN == null) {
				responseStatus = new MspResponseStatus(9999, "Vui lòng liên hệ nhà cung cấp để được xử lý!!!.");
				rsp.setResponseStatus(responseStatus);
				return rsp;
			}

			/* DO DU LIEU TRA VE - CAP NHAT LAI KET QUA */
			XPath xPath = XPathFactory.newInstance().newXPath();
			Node nodeDLHDon = (Node) xPath.evaluate("/TDiep", rTCTN, XPathConstants.NODE);
			codeTTTNhan = commons.getTextFromNodeXML(
					(Element) xPath.evaluate("DLieu/TBao/TTTNhan", nodeDLHDon, XPathConstants.NODE));
			descTTTNhan = commons.getTextFromNodeXML(
					(Element) xPath.evaluate("DLieu/TBao/DSLDo/LDo/MTa", nodeDLHDon, XPathConstants.NODE));
			String MaKetQua = "";
			String CQT_MLTDiep = "";
			if ("2".equals(codeTTTNhan)) {
				rTCTN1 = tctnService.callTraCuuThongDiep(MTDiep);
				XPath xPath1 = XPathFactory.newInstance().newXPath();
				Node nodeKetQuaTraCuu = (Node) xPath1.evaluate("/KetQuaTraCuu", rTCTN1, XPathConstants.NODE);
				MaKetQua = commons.getTextFromNodeXML(
						(Element) xPath1.evaluate("MaKetQua", nodeKetQuaTraCuu, XPathConstants.NODE));

				Node nodeTDiep = null;
				for (int i = 1; i <= 5; i++) {
					if (xPath.evaluate("DuLieu/TDiep[" + i + "]", nodeKetQuaTraCuu, XPathConstants.NODE) == null)
						break;
					nodeTDiep = (Node) xPath.evaluate("DuLieu/TDiep[" + i + "]", nodeKetQuaTraCuu, XPathConstants.NODE);
//							if(xPath.evaluate("DLieu/*/DSCKS", nodeTDiep, XPathConstants.NODE) != null)
					if (xPath.evaluate("DLieu/*/MCCQT", nodeTDiep, XPathConstants.NODE) != null)
						break;
				}
				CQT_MLTDiep = commons.getTextFromNodeXML(
						(Element) xPath.evaluate("TTChung/MLTDiep", nodeTDiep, XPathConstants.NODE));
			}
			if ("202".equals(CQT_MLTDiep)) {
				codeTTTNhan = MaKetQua;
			}

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

			/* CAP NHAT LAI TRANG THAI DANG CHO XU LY */
			options = new FindOneAndUpdateOptions();
			options.upsert(false);
			options.maxTime(5000, TimeUnit.MILLISECONDS);
			options.returnDocument(ReturnDocument.AFTER);

			mongoClient = cfg.mongoClient();
			collection = mongoClient.getDatabase(cfg.dbName).getCollection("EInvoicePXKDL");
			collection.findOneAndUpdate(docFind,
					new Document("$set", new Document("EInvoiceStatus", Constants.INVOICE_STATUS.PROCESSING)
							.append("SendCQT_Date", LocalDateTime.now()).append("InfoSendCQT",
									new Document("Date", LocalDateTime.now()).append("UserID", header.getUserId())
											.append("UserName", header.getUserName())
											.append("UserFullName", header.getUserFullName()))),
					options);
			mongoClient.close();

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
		}

		MsgRsp rsp = new MsgRsp(header);
		MspResponseStatus responseStatus = null;

		ObjectId objectId = null;
		Document docTmp = null;
	
		List<Document> pipeline = new ArrayList<Document>();
		Document docMatch = null;
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
		
		Document fillter = new Document("_id", 1)		
				.append("EInvoiceStatus", 1)
				.append("SignStatusCode", 1)
				.append("MCCQT", 1)
				.append("MTDiep", 1)
				.append("MTDTChieu", 1)
				.append("LDo", 1)
				.append("HDSS", 1)
				.append("SendCQT_Date", 1)
				.append("InfoCreated", 1)
				.append("CQT_Date", 1)
				.append("EInvoiceDetail", 1);
		
		if (!mstban.equals("") && !mstmua.equals("")) {
			pipeline = null;
			pipeline = new ArrayList<Document>();
			docMatch = new Document("EInvoiceDetail.NDHDon.NMua.MST", mstmua)
					.append("EInvoiceDetail.NDHDon.NBan.MST", mstban).append("IsDelete", new Document("$ne", true));
			pipeline.add(new Document("$match", docMatch));
			pipeline.add(new Document("$sort", new Document("EInvoiceDetail.TTChung.NLap", -1).append("_id", -1)));
			pipeline.add(new Document("$project", fillter));	
			pipeline.addAll(createFacetForSearchNotSort(page));

			MongoClient mongoClient = cfg.mongoClient();
			MongoCollection<Document> collection = mongoClient.getDatabase(cfg.dbName).getCollection("EInvoicePXKDL");
			try {
				docTmp = collection.aggregate(pipeline).allowDiskUse(true).iterator().next();
			} catch (Exception e) {
				
			}

			mongoClient.close();

		} else {
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
			if (!"".equals(nbanMst))
				docMatch.append("EInvoiceDetail.NDHDon.NMua.MST",
						new Document("$regex", commons.regexEscapeForMongoQuery(nbanMst)).append("$options", "i"));
			if (!"".equals(nbanTen))
				docMatch.append("EInvoiceDetail.NDHDon.NMua.Ten",
						new Document("$regex", commons.regexEscapeForMongoQuery(nbanTen)).append("$options", "i"));

			pipeline = new ArrayList<Document>();
			pipeline.add(new Document("$match", docMatch));
			pipeline.add(new Document("$addFields", new Document("SHDon",
					new Document("$ifNull", Arrays.asList("$EInvoiceDetail.TTChung.SHDon", Integer.MAX_VALUE)))));
			pipeline.add(new Document("$sort",
					new Document("EInvoiceDetail.TTChung.MauSoHD", -1).append("SHDon", -1).append("_id", -1)));
			pipeline.add(new Document("$project", fillter));	
			pipeline.addAll(createFacetForSearchNotSort(page));

			MongoClient mongoClient = cfg.mongoClient();
			MongoCollection<Document> collection = mongoClient.getDatabase(cfg.dbName).getCollection("EInvoicePXKDL");
			try {
				docTmp = collection.aggregate(pipeline).allowDiskUse(true).iterator().next();
			} catch (Exception e) {
				
			}

			mongoClient.close();

		}
		rsp = new MsgRsp(header);
		responseStatus = null;
		if (null == docTmp) {
			pipeline = null;
			pipeline = new ArrayList<Document>();
			docMatch = new Document("EInvoiceDetail.NDHDon.NMua.MST", header.getUserName()).append("IsDelete",
					new Document("$ne", true));
			pipeline.add(new Document("$match", docMatch));
			pipeline.add(new Document("$sort", new Document("EInvoiceDetail.TTChung.NLap", -1).append("_id", -1)));
			pipeline.add(new Document("$project", fillter));	
			pipeline.addAll(createFacetForSearchNotSort(page));

			MongoClient mongoClient = cfg.mongoClient();
			MongoCollection<Document> collection = mongoClient.getDatabase(cfg.dbName).getCollection("EInvoicePXKDL");
			try {
				docTmp = collection.aggregate(pipeline).allowDiskUse(true).iterator().next();
			} catch (Exception e) {
				
			}

			mongoClient.close();

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
				objectId = (ObjectId) doc.get("_id");

				hItem = new HashMap<String, Object>();
				hItem.put("_id", objectId.toString());
				hItem.put("EInvoiceStatus", doc.get("EInvoiceStatus"));
				hItem.put("SignStatusCode", doc.get("SignStatusCode"));
				hItem.put("MCCQT", doc.get("MCCQT"));
				hItem.put("EInvoiceDetail", doc.get("EInvoiceDetail"));
				hItem.put("InfoCreated", doc.get("InfoCreated"));
				hItem.put("LDo", doc.get("LDo"));
				hItem.put("MTDiep", doc.get("MTDiep"));
				hItem.put("MTDTChieu", doc.get("MTDTChieu"));
				hItem.put("HDSS", doc.get("HDSS"));
				hItem.put("SendCQT_Date", doc.get("SendCQT_Date"));
				hItem.put("CQT_Date", doc.get("CQT_Date"));
				rowsReturn.add(hItem);
			}
		}

		DateTimeFormatter format_time = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
		LocalDateTime time_dem = LocalDateTime.now();
		String time = time_dem.format(format_time);
		String name_company = removeAccent(header.getUserFullName());
		System.out.println(time + " " + name_company + " vua search phieu ki gui dai ly");
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
		pipeline.add(
				new Document("$lookup",
						new Document("from", "PramLink")
								.append("pipeline",
										Arrays.asList(new Document("$match",
												new Document("$expr", new Document("IsDelete", false)))))
								.append("as", "PramLink")));
		pipeline.add(
				new Document("$unwind", new Document("path", "$PramLink").append("preserveNullAndEmptyArrays", true)));

		Document docTmp = null;

		MongoClient mongoClient = cfg.mongoClient();
		MongoCollection<Document> collection = mongoClient.getDatabase(cfg.dbName).getCollection("EInvoicePXKDL");
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
		System.out.println(time + " " + name_company + " vua xem chi tiet phieu ki gui dai ly");
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

		Document fillter = new Document("_id", 1)
				.append("IssuerId", 1)
				.append("Dir", 1)
				.append("FileNameXML", 1)				
				.append("MCCQT", 1)
				.append("MTDiep", 1)
				.append("EInvoiceDetail", 1);
		
		Document docFind = new Document("IssuerId", header.getIssuerId()).append("IsDelete", new Document("$ne", true))
				.append("_id", objectId)
				.append("EInvoiceStatus", new Document("$in", Arrays.asList("CREATED", "PENDING")))
				.append("SignStatusCode", new Document("$in", Arrays.asList("NOSIGN", "PROCESSING")));

		List<Document> pipeline = new ArrayList<Document>();
		pipeline.add(new Document("$match", docFind));
		pipeline.add(new Document("$project", fillter));
		/* KIEM TRA THONG TIN MAU HD */
		pipeline.add(new Document("$lookup", new Document("from", "DMMauSoKyHieu")
				.append("let",
						new Document("vMauSoHD", "$EInvoiceDetail.TTChung.MauSoHD").append("vIssuerId", "$IssuerId"))
				.append("pipeline", Arrays.asList(new Document("$match", new Document("$expr", new Document("$and",
						Arrays.asList(new Document("$gt", Arrays.asList("$ConLai", 0)),
								new Document("$eq", Arrays.asList("$IsActive", true)),
								new Document("$ne", Arrays.asList("$IsDelete", true)),
								new Document("$eq", Arrays.asList(new Document("$toString", "$_id"), "$$vMauSoHD")),
								new Document("$eq", Arrays.asList("$IssuerId", "$$vIssuerId")),
								new Document("$eq", Arrays.asList("$NamPhatHanh", currentYear))))))))
				.append("as", "DMMauSoKyHieu")));
		pipeline.add(new Document("$unwind",
				new Document("path", "$DMMauSoKyHieu").append("preserveNullAndEmptyArrays", true)));
		Document docTmp = null;

		MongoClient mongoClient = cfg.mongoClient();
		MongoCollection<Document> collection = mongoClient.getDatabase(cfg.dbName).getCollection("EInvoicePXKDL");
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
		pipeline1
				.add(new Document("$lookup",
						new Document("from", "EInvoicePXKDL")
								.append("pipeline", Arrays.asList(
										new Document("$match",
												new Document("IsDelete", new Document("$ne", true))
														.append("SignStatusCode", "SIGNED")
														.append("EInvoiceDetail.TTChung.MauSoHD", mauSoHdon)),
										new Document("$group",
												new Document("_id", "$EInvoiceDetail.TTChung.MauSoHD").append("SHDon",
														new Document("$max", "$EInvoiceDetail.TTChung.SHDon")))))
								.append("as", "NLap_MAX")));
		pipeline1.add(
				new Document("$unwind", new Document("path", "$NLap_MAX").append("preserveNullAndEmptyArrays", true)));
		Document docTmp7 = null;
		mongoClient = cfg.mongoClient();
		collection = mongoClient.getDatabase(cfg.dbName).getCollection("EInvoicePXKDL");
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
					.append("EInvoiceStatus",
							new Document("$in",
									Arrays.asList(Constants.INVOICE_STATUS.COMPLETE, Constants.INVOICE_STATUS.PENDING,
											Constants.INVOICE_STATUS.ERROR_CQT, Constants.INVOICE_STATUS.PROCESSING,
											Constants.INVOICE_STATUS.ADJUSTED, Constants.INVOICE_STATUS.REPLACED)))
					.append("EInvoiceDetail.TTChung.MauSoHD", nl_mshd).append("IsDelete", new Document("$ne", true));

			Document docTmp2 = null;

			mongoClient = cfg.mongoClient();
			collection = mongoClient.getDatabase(cfg.dbName).getCollection("EInvoicePXKDL");
			try {
				docTmp2 = collection.find(docFindNLap).allowDiskUse(true).iterator().next();
			} catch (Exception e) {
				
			}
			mongoClient.close();

			if (docTmp2 != null) {
				String nlap_shd_max = commons.convertLocalDateTimeToString(
						commons.convertDateToLocalDateTime(
								docTmp2.getEmbedded(Arrays.asList("EInvoiceDetail", "TTChung", "NLap"), Date.class)),
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
///kiem tra chưa co bien nhung da co hoa don
//tim hoa don moi nhat
		if (checkshdht == 0 && checktontai > 0) {
			pipeline = null;
			Document docMatch = new Document("IssuerId", header.getIssuerId())
					.append("EInvoiceDetail.TTChung.MauSoHD", idDMMSKH).append("IsDelete", new Document("$ne", true))
					.append("EInvoiceStatus", new Document("$in",
							Arrays.asList("DELETED", "DELETE", "ERROR_CQT", "XOABO", "COMPLETE", "PROCESSING")));

			pipeline = new ArrayList<Document>();
			pipeline.add(new Document("$match", docMatch));
			pipeline.add(new Document("$addFields", new Document("SHDon",
					new Document("$ifNull", Arrays.asList("$EInvoiceDetail.TTChung.SHDon", Integer.MAX_VALUE)))));
			pipeline.add(new Document("$sort",
					new Document("EInvoiceDetail.TTChung.MauSoHD", -1).append("SHDon", -1).append("_id", -1)));
			Document docTmp1 = null;

			mongoClient = cfg.mongoClient();
			collection = mongoClient.getDatabase(cfg.dbName).getCollection("EInvoicePXKDL");
			try {
				docTmp1 = collection.aggregate(pipeline).allowDiskUse(true).iterator().next();
			} catch (Exception e) {
				
			}
			mongoClient.close();

			ktshdht = docTmp1.getEmbedded(Arrays.asList("EInvoiceDetail", "TTChung", "SHDon"), 0);
		}
/////////////////sau khi check lay ra shd lon nhat
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
		collection = mongoClient.getDatabase(cfg.dbName).getCollection("EInvoicePXKDL");
		collection.findOneAndUpdate(docFind, new Document("$set",
				new Document("EInvoiceDetail.TTChung.SHDon", eInvoiceNumber).append("EInvoiceStatus", "PENDING")),
				options);
		mongoClient.close();

		if (checkshd == 0) {
			Document docFindMS = null;
			docFindMS = new Document("IssuerId", header.getIssuerId()).append("IsDelete", new Document("$ne", true))
					.append("IsActive", true).append("_id", objectIdMS);

			mongoClient = cfg.mongoClient();
			collection = mongoClient.getDatabase(cfg.dbName).getCollection("DMMauSoKyHieu");
			collection.findOneAndUpdate(docFindMS,
					new Document("$set", new Document("ConLai", CL).append("SHDHT", sht)), options);
			mongoClient.close();

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

//		String actionCode = header.getActionCode();
		String _id = commons.getTextJsonNode(jsonData.at("/_id")).replaceAll("\\s", "");

		ObjectId objectId = null;
		try {
			objectId = new ObjectId(_id);
		} catch (Exception e) {
		}
		List<Document> pipeline = null;
		/* KIEM TRA THONG TIN HOP LE KHONG */
		Document docFind = new Document("IssuerId", header.getIssuerId()).append("IsDelete", new Document("$ne", true))
				.append("_id", objectId);
		pipeline = new ArrayList<Document>();
		pipeline.add(new Document("$match", docFind));
		pipeline.add(new Document("$project", new Document("_id", 1).append("EInvoiceDetail", 1).append("IssuerId", 1)));
		pipeline.add(new Document("$lookup", new Document("from", "EInvoicePXKDL")
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
								new Document("_id", "$EInvoiceDetail.TTChung.MauSoHD").append("SHDon",
										new Document("$max", "$EInvoiceDetail.TTChung.SHDon")))))
				.append("as", "EInvoiceMAXCQT")));
		pipeline.add(new Document("$unwind",
				new Document("path", "$EInvoiceMAXCQT").append("preserveNullAndEmptyArrays", true)));
		Document docTmp = null;
		MongoClient mongoClient = cfg.mongoClient();
		MongoCollection<Document> collection = mongoClient.getDatabase(cfg.dbName).getCollection("EInvoicePXKDL");
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
		if (invoiceNumberCurrent == 0 || invoiceNumberCurrent != maxInvoiceSendedCQT + 1) {
			responseStatus = new MspResponseStatus(9999,
					"Có 1 hoặc 1 vài số hóa đơn trước đó chưa được xử lý xong.<br>Vui lòng kiểm tra lại danh sách hóa đơn.");
			rsp.setResponseStatus(responseStatus);
			return rsp;
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
		String SigningTime = commons.getTextFromNodeXML((Element) xPath.evaluate(
				"/HDon/DSCKS/NBan/Signature/Object[@Id='SigningTime']/SignatureProperties/SignatureProperty/SigningTime",
				xmlDoc, XPathConstants.NODE));

//		LocalDate ldNLap = null;
//		LocalDate ldSigningTime = null;
//		try {
//			ldNLap = commons.convertStringToLocalDate(NLap, "yyyy-MM-dd");
//		} catch (Exception e) {
//		}
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
				.append("EInvoiceDetail.TTChung.SHDon", eInvoiceNumber).append("EInvoiceStatus", "PENDING")
				.append("_id", objectId);
		pipeline = new ArrayList<Document>();
		pipeline.add(new Document("$match", docFind));
		pipeline.add(new Document("$project", new Document("IssuerId", 1).append("EInvoiceDetail", 1)
				.append("_id", 1).append("SignStatusCode", 1).append("Dir", 1)
				));
		pipeline.add(new Document("$lookup", new Document("from", "EInvoicePXKDL")
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
								new Document("_id", "$EInvoiceDetail.TTChung.MauSoHD").append("SHDon",
										new Document("$max", "$EInvoiceDetail.TTChung.SHDon")))))
				.append("as", "EInvoiceMAXCQT")));
		pipeline.add(new Document("$unwind",
				new Document("path", "$EInvoiceMAXCQT").append("preserveNullAndEmptyArrays", true)));
		Document docTmp = null;

		MongoClient mongoClient = cfg.mongoClient();
		MongoCollection<Document> collection = mongoClient.getDatabase(cfg.dbName).getCollection("EInvoicePXKDL");
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
		if (invoiceNumberCurrent == 0 || invoiceNumberCurrent != maxInvoiceSendedCQT + 1) {
			responseStatus = new MspResponseStatus(9999,
					"Có 1 hoặc 1 vài số hóa đơn trước đó chưa được xử lý xong.<br>Vui lòng kiểm tra lại danh sách hóa đơn.");
			rsp.setResponseStatus(responseStatus);
			return rsp;
		}

		String signStatusCode = docTmp.get("SignStatusCode", "");
		if ("PROCESSING".equals(signStatusCode)) {
			responseStatus = new MspResponseStatus(9999, "Hóa đơn đã ký và đang chờ xử lý với cơ quan thuế.");
			rsp.setResponseStatus(responseStatus);
			return rsp;
		}

		/* KIEM TRA NGAY LAP TRONG HE THONG - NGAY LAP TRONG XML */
//		LocalDate ldNLapSystem = commons.convertDateToLocalDate(
//				docTmp.getEmbedded(Arrays.asList("EInvoiceDetail", "TTChung", "NLap"), Date.class));
//		if (commons.compareLocalDate(ldNLap, ldNLapSystem) != 0) {
//			responseStatus = new MspResponseStatus(9999, "Ngày lập trong hệ thống và trong dữ liệu khác nhau.");
//			rsp.setResponseStatus(responseStatus);
//			return rsp;
//		}

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
		collection = mongoClient.getDatabase(cfg.dbName).getCollection("EInvoicePXKDL");
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
		System.out.println(time + " " + name_company + " vua ky thanh cong phieu ki gui dai ly");
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
				.append("IsDelete", new Document("$ne", true)).append("SignStatusCode", "SIGNED")
				.append("EInvoiceStatus", new Document("$in", Arrays.asList("ERROR_CQT", "PROCESSING")));

		Document docTmp = null;

		MongoClient mongoClient = cfg.mongoClient();
		MongoCollection<Document> collection = mongoClient.getDatabase(cfg.dbName).getCollection("EInvoicePXKDL");
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
		rTCTN = tctnService.callTraCuuThongDiep(MTDiep);

//		rTCTN = commons.stringToDocument("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?><KetQuaTraCuu><MaKetQua>0</MaKetQua><MoTaKetQua>CQT đã trả kết quả xử lý thông điệp</MoTaKetQua><DuLieu><TDiep><TTChung><PBan>2.0.0</PBan><MNGui>TCT</MNGui><MNNhan>V0401486901</MNNhan><MLTDiep>999</MLTDiep><MTDiep>TCTAFC1603B23C84880A91527558FDD1062</MTDiep><MTDTChieu>V040148690165149975ACCF4BFFAFB5DF080E0B86E8</MTDTChieu></TTChung><DLieu><TBao><MTDiep>V040148690165149975ACCF4BFFAFB5DF080E0B86E8</MTDiep><MNGui>V0401486901</MNGui><NNhan>2021-12-15T00:12:02</NNhan><TTTNhan>0</TTTNhan></TBao></DLieu></TDiep><TDiep><TTChung><PBan>2.0.0</PBan><MNGui>TCT</MNGui><MNNhan>V0401486901</MNNhan><MLTDiep>202</MLTDiep><MTDiep>TCTAD6776E17D034CCAA607537CA16E07E2</MTDiep><MTDTChieu>V040148690165149975ACCF4BFFAFB5DF080E0B86E8</MTDTChieu><MST>0106323762</MST><SLuong>1</SLuong></TTChung><DLieu><HDon><DLHDon Id=\"data\"><TTChung><PBan>2.0.0</PBan><THDon>Hóa đơn giá trị gia tăng TT 78</THDon><KHMSHDon>1</KHMSHDon><KHHDon>C21TEE</KHHDon><SHDon>2</SHDon><MHSo /><NLap>2021-12-14</NLap><SBKe /><NBKe /><DVTTe>VND</DVTTe><TGia>1</TGia><HTTToan>Tiền mặt/Chuyển khoản</HTTToan><MSTTCGP>0315382923</MSTTCGP><MSTDVNUNLHDon /><TDVNUNLHDon /><DCDVNUNLHDon /><TTKhac><TTin><TTruong>PortalLink</TTruong><KDLieu>string</KDLieu><DLieu>http://sesgroup.vn</DLieu></TTin><TTin><TTruong>SecureKey</TTruong><KDLieu>string</KDLieu><DLieu>121200</DLieu></TTin><TTin><TTruong>SystemKey</TTruong><KDLieu>string</KDLieu><DLieu>61b8cc7e192a400fcc079600</DLieu></TTin></TTKhac></TTChung><NDHDon><NBan><Ten>MÃ SỐ THUẾ TEST 97</Ten><MST>0106323762</MST><DChi>TP.Hồ Chí Minh</DChi><SDThoai /><DCTDTu /><STKNHang /><TNHang /><Fax /><Website /><TTKhac /></NBan><NMua><Ten>cty</Ten><MST /><DChi /><MKHang /><SDThoai /><DCTDTu /><HVTNMHang>nguyen hien nang</HVTNMHang><STKNHang /><TNHang /><TTKhac /></NMua><DSHHDVu><HHDVu><TChat>1</TChat><STT>1</STT><MHHDVu /><THHDVu>san pham test</THHDVu><DVTinh /><SLuong>1</SLuong><DGia>1000000</DGia><TLCKhau /><STCKhau /><ThTien>1000000</ThTien><TSuat>10%</TSuat><TTKhac><TTin><TTruong>VATAmount</TTruong><KDLieu>decimal</KDLieu><DLieu>100000</DLieu></TTin><TTin><TTruong>Amount</TTruong><KDLieu>decimal</KDLieu><DLieu>1100000</DLieu></TTin></TTKhac></HHDVu></DSHHDVu><TToan><THTTLTSuat><LTSuat><TSuat>10%</TSuat><ThTien>1000000</ThTien><TThue>100000</TThue></LTSuat></THTTLTSuat><TgTCThue>1000000</TgTCThue><TgTThue>100000</TgTThue><DSLPhi><LPhi><TLPhi /><TPhi>0</TPhi></LPhi></DSLPhi><TTCKTMai>0</TTCKTMai><TgTTTBSo>1100000</TgTTTBSo><TgTTTBChu>Một triệu một trăm nghìn đồng.</TgTTTBChu><TTKhac /></TToan></NDHDon></DLHDon><MCCQT Id=\"Id-46005206eae1477cb7782accf90fa2fb\">006D249999BD7A4A2FA3689987BFE9C0F7</MCCQT><DSCKS><NBan><Signature xmlns=\"http://www.w3.org/2000/09/xmldsig#\" Id=\"proid\"><SignedInfo><CanonicalizationMethod Algorithm=\"http://www.w3.org/TR/2001/REC-xml-c14n-20010315\" /><SignatureMethod Algorithm=\"http://www.w3.org/2000/09/xmldsig#rsa-sha1\" /><Reference URI=\"#data\"><Transforms><Transform Algorithm=\"http://www.w3.org/2000/09/xmldsig#enveloped-signature\" /><Transform Algorithm=\"http://www.w3.org/TR/2001/REC-xml-c14n-20010315\" /></Transforms><DigestMethod Algorithm=\"http://www.w3.org/2000/09/xmldsig#sha1\" /><DigestValue>j+bt7usWOm3vVx9AiyhSmuWNNZI=</DigestValue></Reference><Reference URI=\"#SigningTime\"><Transforms><Transform Algorithm=\"http://www.w3.org/TR/2001/REC-xml-c14n-20010315\" /></Transforms><DigestMethod Algorithm=\"http://www.w3.org/2000/09/xmldsig#sha1\" /><DigestValue>W3UIWMrhcMa7hL5inxWDXwHhjR8=</DigestValue></Reference></SignedInfo><SignatureValue>QBY5NpYSqkEp8/dHZGW9Iuq0k4plUpR6A6PoKdGlnzOo2+7vxJnHvnbZR/w14RuGOvmX1wh0R38rAsYn3Y4e9wDRJeNIaiSvViL0WzMjuIFiVIhpu/W0ZvM36GTdDk9guvIeXjOOTsepaDmVy1aO+zGuT2PctgmJP/iXOzQCiotV4GwFyy2gMDsYU5NPuYVeYqqqr3rtm/D3V5GBmrlAC9bZKFEJ1ISzElOEPXlQKGc8ErXldYIeKHDFF8t6khvyBzuy/nVTHyjr7AmL2fVcmY3ibUzU0fViy8uOIkNbfZbSi5BFtitVVMOo5Lb3fwFimPDLr8JjiiRic1KIjbRZbw==</SignatureValue><KeyInfo><KeyValue><RSAKeyValue><Modulus>k0i7dlmtzCMqQm/QoKbnPRK0c7im1CiIJpAk+xRnPOZE1afDvAb5qRCV2XJm9hQMjwuuwleKOJo8XJJ5PPg0hnQb9ZYOFtxZuEbV0KU98Nyn3zG1w5ScRues49WItz32s9JuO6h/dxXIHk7qaRoAmsb6RlWEznfMiZl27RWHuUk/uwPdsdeD2YvCl16lIeb+tbLX2vafTxKk2+M+EcOfn9AOzkVvs7EKlG5eUmIgrImBd7bWwmc88ZbMc//55OczoYTDIeADg0/4X0IANL9DjbewzJ3SdN0ikKzY69Od2/2QzMVtFTsbmgNpri6PnyULa+PlN/uB8zgr/eWris78SQ==</Modulus><Exponent>AQAB</Exponent></RSAKeyValue></KeyValue><X509Data><X509IssuerSerial><X509IssuerName>C=VN, S=TP.Hồ Chí Minh, O=L.C.S CO.LTD, CN=LCS-CA G1</X509IssuerName><X509SerialNumber>111660365432937579549439403630700839553</X509SerialNumber></X509IssuerSerial><X509SubjectName>OID.0.9.2342.19200300.100.1.1=MST:0106323762, CN=MÃ SỐ THUẾ TEST 97</X509SubjectName><X509Certificate>MIIEdTCCA12gAwIBAgIQVAEBDz5oOWps6UZRr0TCgTANBgkqhkiG9w0BAQsFADBUMRIwEAYDVQQDDAlMQ1MtQ0EgRzExFTATBgNVBAoMDEwuQy5TIENPLkxURDEaMBgGA1UECAwRVFAuSOG7kyBDaMOtIE1pbmgxCzAJBgNVBAYTAlZOMB4XDTIxMTEwMzE3MDAwMFoXDTIyMTEwNDE2NTkwMFowQjEgMB4GA1UEAwwXTcODIFPhu5AgVEhV4bq+IFRFU1QgOTcxHjAcBgoJkiaJk/IsZAEBDA5NU1Q6MDEwNjMyMzc2MjCCASIwDQYJKoZIhvcNAQEBBQADggEPADCCAQoCggEBAJNIu3ZZrcwjKkJv0KCm5z0StHO4ptQoiCaQJPsUZzzmRNWnw7wG+akQldlyZvYUDI8LrsJXijiaPFySeTz4NIZ0G/WWDhbcWbhG1dClPfDcp98xtcOUnEbnrOPViLc99rPSbjuof3cVyB5O6mkaAJrG+kZVhM53zImZdu0Vh7lJP7sD3bHXg9mLwpdepSHm/rWy19r2n08SpNvjPhHDn5/QDs5Fb7OxCpRuXlJiIKyJgXe21sJnPPGWzHP/+eTnM6GEwyHgA4NP+F9CADS/Q423sMyd0nTdIpCs2OvTndv9kMzFbRU7G5oDaa4uj58lC2vj5Tf7gfM4K/3lq4rO/EkCAwEAAaOCAVMwggFPMAwGA1UdEwEB/wQCMAAwHwYDVR0jBBgwFoAUXYADrGJokVVOCpFA4VHLepzi14gwXgYIKwYBBQUHAQEEUjBQMCsGCCsGAQUFBzAChh9odHRwOi8vY3JsLmxjcy1jYS52bi9sY3MtY2EuY3J0MCEGCCsGAQUFBzABhhVodHRwOi8vb2NzcC5sY3MtY2Eudm4wGwYDVR0RBBQwEoEQZHV5cGNAdmlzbmFtLmNvbTBABgNVHSUEOTA3BggrBgEFBQcDAgYIKwYBBQUHAwQGCisGAQQBgjcKAwwGCisGAQQBgjcUAgIGCSqGSIb3LwEBBTAwBgNVHR8EKTAnMCWgI6Ahhh9odHRwOi8vY3JsLmxjcy1jYS52bi9sY3MtY2EuY3JsMB0GA1UdDgQWBBTt0dgvnsQ5PUE6RzJREZlXk6aDLTAOBgNVHQ8BAf8EBAMCBeAwDQYJKoZIhvcNAQELBQADggEBAItPk5ux4RT4O2BowpC0vfRZnDvxkt7G6HGfBC8cxLmVjqUGtR8+G6ScuR8J9OfZdF5naDnvUBqMqjcxwOiP+lZ2uBmoVe1diebRzqjZFttyDWQf0Hcjg0EOaSP89CB1Hk1PEs4sf8RWT7OA/97212tEPOkW3CiLFSOlIC8P6yPTuWyi0wIGoCJRSB/IcmD2djEv9/HkiuYSFBTuiaxaJ9WUrRCHuuaMFv3LBvtnCbog8pMOXK7qwaFlf35x6g2kPHdS72Jn0L7Q5V90iReJI2pyKyeeQ27gcViI/9IXatM95SbCCktIGhxk/vNGaC+1+uPmBM1auUDhcRmmJWNlkm8=</X509Certificate></X509Data></KeyInfo><Object Id=\"SigningTime\"><SignatureProperties Id=\"SigProps\" xmlns=\"\"><SignatureProperty Target=\"#proid\"><SigningTime xmlns=\"http://example.org/#signatureProperties\">2021-12-15T00:10:31Z</SigningTime></SignatureProperty></SignatureProperties></Object></Signature></NBan><CQT><Signature xmlns=\"http://www.w3.org/2000/09/xmldsig#\" Id=\"Tct-f2d41b7f3cdc434f8a5f7505d008092d\"><SignedInfo><CanonicalizationMethod Algorithm=\"http://www.w3.org/TR/2001/REC-xml-c14n-20010315\" /><SignatureMethod Algorithm=\"http://www.w3.org/2001/04/xmldsig-more#rsa-sha256\" /><Reference URI=\"#data\"><DigestMethod Algorithm=\"http://www.w3.org/2001/04/xmlenc#sha256\" /><DigestValue>wcJ3XO8PU3PnPqy4B3KDqohg/qIi3b/FiiJvK5akJY8=</DigestValue></Reference><Reference URI=\"#SigningTime-13f86a9edc28446c818268fb04007cef\"><DigestMethod Algorithm=\"http://www.w3.org/2001/04/xmlenc#sha256\" /><DigestValue>KhGa7oV6SzAxJ1avwO/GAsPSd8SFtS8iHz6ByAJx/L4=</DigestValue></Reference><Reference URI=\"#Id-46005206eae1477cb7782accf90fa2fb\"><DigestMethod Algorithm=\"http://www.w3.org/2001/04/xmlenc#sha256\" /><DigestValue>AMZlvmqzWaEFC7w520Psn3IMdcZDJtNB441zVtC8dlI=</DigestValue></Reference></SignedInfo><SignatureValue>iFVh5Ken6LJE60EGz2bs+0+T8hKxyJ+dA3KVaKDJmR+pndj14HUpnkse30ITmtsPH+VFkky6/YDKUlEs/+6J0cP1NRANYsW38TNSTtkW4kZspF5Cy9bpZDmRG49XaWEICVUkv1BIHI35//r7ugVYJfrRydcAIJ09xw+mcG0ZFArHMLAo9drtiVeiIks0kbqsWduSGp1yz8Al5ggYQRLlkZZUJv2SUuVoAnLRLQiSfpphmQN32skZkAHG2YvEdMdO0tev1Fjz1rbRAQobWzt5zPU9nh7ob+LT5aijMyzUc0v+OZTSwHExZoJIfiw5Hhcu0WiN8xkS6MR0jWFghFuhPQ==</SignatureValue><KeyInfo><X509Data><X509SubjectName>CN=Tổng cục thuế Test 01</X509SubjectName><X509Certificate>MIIDRzCCAi+gAwIBAgIQbYJ+PW9g1fGMCAT216XJkjANBgkqhkiG9w0BAQsFADAYMRYwFAYDVQQDDA1DWUJFUkxPVFVTLUNBMB4XDTIxMTAyMDEzNDkxM1oXDTIzMTAyMDEzNDkxM1owJjEkMCIGA1UEAwwbVOG7lW5nIGPhu6VjIHRodeG6vyBUZXN0IDAxMIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEA5WuS2+ptB81jC10L0KiQvML2ncjqvsgnkhItbpDYdTOzhphJgioOqRaGaXW55HMNRV2hRcQ96DXQVEWq72QRRA36d6lCSbeHTpieL0bfqwy+TV6qnMqntpGtFcUZLwYeBaUusQA2thGG6YHZpQLicIjYcUuvHzLt9CXWo4XJEWl2EnOORyzB4nkzbko2SGl63/qIA/VHc5Y2Cn7ykPpd+LjrP6AeE2JgmE9CIHAgwYZNZZfKWjF+64OFs/QerOOI1LqVlvx+XbbeLB5qwM38Kkc4PywyuVj5NM2kbZekPzJJRsqsMHPxI2jDILIPqvYvwhVZkKEmeukiYgdWE+8kcwIDAQABo38wfTAMBgNVHRMBAf8EAjAAMB8GA1UdIwQYMBaAFHJm6SIoV+NUQVkehr2Ptlj3B7YzMB0GA1UdJQQWMBQGCCsGAQUFBwMCBggrBgEFBQcDBDAdBgNVHQ4EFgQU9r3qRBc6SwSzJPl6txxiFrmE4UwwDgYDVR0PAQH/BAQDAgXgMA0GCSqGSIb3DQEBCwUAA4IBAQCRJURHsPjLAkNKiq+zfruPuFMMwIJu0OYm/jcBxmUsfEiNvVUOFRi/9AklZ01bmKVdpjEsP0mbh0RUk4OiK6d8spx0ShN80KjcISW/wjMaLkMgUbb2sVbVSnsMrenbpa6U1Gms5AV5NREOqKPUYNS6otNsVbkKPRDtw9KyQHC/XHydaANVsIa6LqgadVngf5F/4XnU9eYqQAzrJaHPWvWjwdkwxje8XViBEsqcTC24YfayJcZc1gkA2DIsW/2eUCmABvJi7n3VngLVxakXI6YOchsRKPmKlqZJQZ5yaYsD9eBfCbG37+J7npGMYDRQATq8PCa9CNQtPJlUdulN359b</X509Certificate></X509Data></KeyInfo><Object Id=\"SigningTime-13f86a9edc28446c818268fb04007cef\"><SignatureProperties><SignatureProperty Target=\"signatureProperties\"><SigningTime>2021-12-15T00:12:04</SigningTime></SignatureProperty></SignatureProperties></Object></Signature></CQT></DSCKS></HDon></DLieu></TDiep></DuLieu></KetQuaTraCuu>");
//		rTCTN = commons.stringToDocument("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?><KetQuaTraCuu><MaKetQua>0</MaKetQua><MoTaKetQua>CQT đã trả kết quả xử lý thông điệp</MoTaKetQua><DuLieu><TDiep><TTChung><PBan>2.0.0</PBan><MNGui>TCT</MNGui><MNNhan>V0401486901</MNNhan><MLTDiep>999</MLTDiep><MTDiep>TCTE9C0484E7A5A4E70B9D84F769D19DC76</MTDiep><MTDTChieu>V040148690106149AAB89B94875BE751E0FC2E1EACB</MTDTChieu></TTChung><DLieu><TBao><MTDiep>V040148690106149AAB89B94875BE751E0FC2E1EACB</MTDiep><MNGui>V0401486901</MNGui><NNhan>2021-12-29T09:45:30</NNhan><TTTNhan>0</TTTNhan></TBao></DLieu></TDiep><TDiep><TTChung><PBan>2.0.0</PBan><MNGui>TCT</MNGui><MNNhan>V0401486901</MNNhan><MLTDiep>204</MLTDiep><MTDiep>TCT492D38CAD70845E3B06CE7D2E2F2D536</MTDiep><MTDTChieu>V040148690106149AAB89B94875BE751E0FC2E1EACB</MTDTChieu><MST>0301521415</MST><SLuong>1</SLuong></TTChung><DLieu><TBao><DLTBao Id=\"Id-370E37D581EA48E080C00B762E76CD9E\"><PBan>2.0.0</PBan><MSo>01/TB-KTDL</MSo><Ten>Thông báo về việc kết quả kiểm tra dữ liệu hóa đơn điện tử</Ten><So>210002540595</So><DDanh>TP Hồ Chí Minh</DDanh><NTBao>2021-12-29</NTBao><MST>0301521415</MST><TNNT>CÔNG TY CỔ PHẦN TÔ THÀNH PHÁT</TNNT><TGGui>2021-12-29T09:45:30</TGGui><LTBao>1</LTBao><CCu>Thông điệp chuyển dữ liệu hóa đơn điện tử để được cấp mã đến CQT</CCu><MGDDTu>V040148690106149AAB89B94875BE751E0FC2E1EACB</MGDDTu><SLuong>1</SLuong><LCMa><DSLDo><LDo><MLoi>20001</MLoi><MTLoi>Sai định dạng dữ liệu nmdctdtu: định dạng không hợp lệ</MTLoi></LDo></DSLDo></LCMa></DLTBao><DSCKS><CQT><Signature xmlns=\"http://www.w3.org/2000/09/xmldsig#\" Id=\"Tct-8d83a782e0574cf9a340a764554ab91a\"><SignedInfo><CanonicalizationMethod Algorithm=\"http://www.w3.org/TR/2001/REC-xml-c14n-20010315\" /><SignatureMethod Algorithm=\"http://www.w3.org/2001/04/xmldsig-more#rsa-sha256\" /><Reference URI=\"#Id-370E37D581EA48E080C00B762E76CD9E\"><DigestMethod Algorithm=\"http://www.w3.org/2001/04/xmlenc#sha256\" /><DigestValue>74Gk6u5E9UA0cnEaO2ikOALhB8n4c/OOCBZue2o8qlE=</DigestValue></Reference><Reference URI=\"#SigningTime-3d0e238c1c8a42f0b5097195bce85d28\"><DigestMethod Algorithm=\"http://www.w3.org/2001/04/xmlenc#sha256\" /><DigestValue>zc6AH+Wpd2J7JugAZd49zE+ktMdwnqZGVrRanLJEVaA=</DigestValue></Reference></SignedInfo><SignatureValue>GGyDDTf7C5ZfIXyk+fSb87IqgmINS0jZzW9Aqu3OeUx368FWnp1JUbbBlfc68vWRDeQsij1uwxENxPFEQxKnBo93RkVDCeFRdlkWOPYHNQF85XwMvwEVpmvQw8lBocMfLk4K7CCz7ww6iGK/J+5l2cvaE3LSHxyHwL6HrtgSJz2RqLWlAlm3VAJbJ4EpcuOzz4sNHINGDlWoQKBa4S1K+NJAOObNxwb60VXGiMyBWbWutntlB+LduzhAGLQvyfVXDDwZICOUEXWL2C9eSZQCGCI8RFrskr7FgKb5oqvxFUn3ihh9kQsdMd9W7Cv0hFhAHCA4Puu21XM0sN+28e2HbA==</SignatureValue><KeyInfo><X509Data><X509SubjectName>CN=TỔNG CỤC THUẾ,O=BỘ TÀI CHÍNH,L=Hà Nội,C=VN</X509SubjectName><X509Certificate>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</X509Certificate></X509Data></KeyInfo><Object Id=\"SigningTime-3d0e238c1c8a42f0b5097195bce85d28\"><SignatureProperties><SignatureProperty Target=\"signatureProperties\"><SigningTime>2021-12-29T09:45:30</SigningTime></SignatureProperty></SignatureProperties></Object></Signature></CQT></DSCKS></TBao></DLieu></TDiep></DuLieu></KetQuaTraCuu>");
//		rTCTN = commons.stringToDocument("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?><KetQuaTraCuu><MaKetQua>0</MaKetQua><MoTaKetQua>CQT đã tiếp nhận thông điệp</MoTaKetQua><DuLieu><TDiep><TTChung><PBan>2.0.0</PBan><MNGui>TCT</MNGui><MNNhan>V0401486901</MNNhan><MLTDiep>202</MLTDiep><MTDiep>TCT4C5ACE1901B74662AAB2A8E2765C74F8</MTDiep><MTDTChieu>V04014869011FCE1CE41BA54914ABAB3048C8C8185D</MTDTChieu><MST>0301521415</MST><SLuong>1</SLuong></TTChung><DLieu><HDon><DLHDon Id=\"data\"><TTChung><PBan>2.0.0</PBan><THDon>Hóa đơn giá trị gia tăng TT 78</THDon><KHMSHDon>1</KHMSHDon><KHHDon>C21TBB</KHHDon><SHDon>23</SHDon><MHSo /><NLap>2021-12-29</NLap><SBKe /><NBKe /><DVTTe>VND</DVTTe><TGia>1</TGia><HTTToan>Tiền mặt</HTTToan><MSTTCGP>0315382923</MSTTCGP><MSTDVNUNLHDon /><TDVNUNLHDon /><DCDVNUNLHDon /><TTKhac><TTin><TTruong>PortalLink</TTruong><KDLieu>string</KDLieu><DLieu>www.tothanhphat.vn/ttp-einvoice/tracuuhd</DLieu></TTin><TTin><TTruong>SecureKey</TTruong><KDLieu>string</KDLieu><DLieu>467837</DLieu></TTin><TTin><TTruong>SystemKey</TTruong><KDLieu>string</KDLieu><DLieu>61cbd4150d9e5d1c1108bf8c</DLieu></TTin></TTKhac></TTChung><NDHDon><NBan><Ten>CÔNG TY CỔ PHẦN TÔ THÀNH PHÁT</Ten><MST>0301521415</MST><DChi>56 Phạm Hữu Chí, Phường 12, Quận 5, Thành phố Hồ Chí Minh</DChi><SDThoai>(028) 37.600.707</SDThoai><DCTDTu>tothanhphatbc@vnnn.vn</DCTDTu><STKNHang /><TNHang>Ngân hàng VCB - CN Bình Tây</TNHang><Fax /><Website>www.ttpvn.com</Website><TTKhac /></NBan><NMua><Ten>Cửa Hàng Phước Thịnh</Ten><MST /><DChi>119 ấp Bình Lợi, Xã Đức Tân, H.Tân Trụ, Tỉnh Long An</DChi><MKHang /><SDThoai /><DCTDTu>gachmenthanhnam@gmail.com</DCTDTu><HVTNMHang /><STKNHang /><TNHang /><TTKhac /></NMua><DSHHDVu><HHDVu><TChat>1</TChat><STT>1</STT><MHHDVu /><THHDVu>Gạch TRM 60x60 (4 Viên/Thùng) loại 1 - T61904RL1</THHDVu><DVTinh>Thùng</DVTinh><SLuong>24</SLuong><DGia>122727</DGia><TLCKhau /><STCKhau /><ThTien>2945448</ThTien><TSuat>10%</TSuat><TTKhac><TTin><TTruong>VATAmount</TTruong><KDLieu>decimal</KDLieu><DLieu>294545</DLieu></TTin><TTin><TTruong>Amount</TTruong><KDLieu>decimal</KDLieu><DLieu>3239993</DLieu></TTin></TTKhac></HHDVu></DSHHDVu><TToan><THTTLTSuat><LTSuat><TSuat>10%</TSuat><ThTien>2945448</ThTien><TThue>294545</TThue></LTSuat></THTTLTSuat><TgTCThue>2945448</TgTCThue><TgTThue>294545</TgTThue><DSLPhi><LPhi><TLPhi /><TPhi>0</TPhi></LPhi></DSLPhi><TTCKTMai>0</TTCKTMai><TgTTTBSo>3239993</TgTTTBSo><TgTTTBChu>Ba triệu hai trăm ba mươi chín nghìn chín trăm chín mươi ba đồng.</TgTTTBChu><TTKhac /></TToan></NDHDon></DLHDon><MCCQT Id=\"Id-7721300047e84265b4b873e00eaecf4a\">00852143E192414AA1BCA252DFC6E27300</MCCQT><DSCKS><NBan><Signature xmlns=\"http://www.w3.org/2000/09/xmldsig#\" Id=\"proid\"><SignedInfo><CanonicalizationMethod Algorithm=\"http://www.w3.org/TR/2001/REC-xml-c14n-20010315\" /><SignatureMethod Algorithm=\"http://www.w3.org/2000/09/xmldsig#rsa-sha1\" /><Reference URI=\"#data\"><Transforms><Transform Algorithm=\"http://www.w3.org/2000/09/xmldsig#enveloped-signature\" /><Transform Algorithm=\"http://www.w3.org/TR/2001/REC-xml-c14n-20010315\" /></Transforms><DigestMethod Algorithm=\"http://www.w3.org/2000/09/xmldsig#sha1\" /><DigestValue>JGKMwiwBDsL9C6ZNY2xXorYB7Lk=</DigestValue></Reference><Reference URI=\"#SigningTime\"><Transforms><Transform Algorithm=\"http://www.w3.org/TR/2001/REC-xml-c14n-20010315\" /></Transforms><DigestMethod Algorithm=\"http://www.w3.org/2000/09/xmldsig#sha1\" /><DigestValue>bq+mqy+vOlu79UP2OQHxjb3xbxE=</DigestValue></Reference></SignedInfo><SignatureValue>OgHw/xdR2CqhwDw2hbqttZWvRg2xtCzjFsEaikOg6nALDkg40+VfX+YafRUeqaYRNGqz5/T2TxKj0joyA95nJgQudc8odu6UQT+3MEa5GBSJWWIqKYYH75cw6gWYtLPUmLs8GSabBp6mwoylCwQjdn5bq4IwhhFS9purp7nnITI=</SignatureValue><KeyInfo><KeyValue><RSAKeyValue><Modulus>tCwXgWNBkFSOn8H8i1ngFVT62fwI2LSufN1bH1zaoiytqIA4M60A2H+iuwry3dicTdm/bZnrAY6oYY9AXPgjETWusMWzkXtmMNwFukqiJioP3IPYgZFDZts4TSXoHXwwecQBUW3sE501eb0Hm0SS0EsOfqHTymb44yz8CbvP9XU=</Modulus><Exponent>AQAB</Exponent></RSAKeyValue></KeyValue><X509Data><X509IssuerSerial><X509IssuerName>CN=SmartSign, OU=Cong ty co phan chu ky so VI NA, O=Cong ty co phan chu ky so VI NA, C=VN</X509IssuerName><X509SerialNumber>111660364806195056320980835436687974772</X509SerialNumber></X509IssuerSerial><X509SubjectName>C=VN, S=HỒ CHÍ MINH, CN=CÔNG TY CỔ PHẦN TÔ THÀNH PHÁT, OID.0.9.2342.19200300.100.1.1=MST:0301521415</X509SubjectName><X509Certificate>MIIE1zCCA7+gAwIBAgIQVAEBB1VK9cLCI9QLDebhdDANBgkqhkiG9w0BAQsFADB1MQswCQYDVQQGEwJWTjEoMCYGA1UECgwfQ29uZyB0eSBjbyBwaGFuIGNodSBreSBzbyBWSSBOQTEoMCYGA1UECwwfQ29uZyB0eSBjbyBwaGFuIGNodSBreSBzbyBWSSBOQTESMBAGA1UEAwwJU21hcnRTaWduMB4XDTIxMTIwOTA4MzgzNVoXDTI0MTIwODA4MzgzNVowdjEeMBwGCgmSJomT8ixkAQEMDk1TVDowMzAxNTIxNDE1MS4wLAYDVQQDDCVDw5RORyBUWSBD4buUIFBI4bqmTiBUw5QgVEjDgE5IIFBIw4FUMRcwFQYDVQQIDA5I4buSIENIw40gTUlOSDELMAkGA1UEBhMCVk4wgZ8wDQYJKoZIhvcNAQEBBQADgY0AMIGJAoGBALQsF4FjQZBUjp/B/ItZ4BVU+tn8CNi0rnzdWx9c2qIsraiAODOtANh/orsK8t3YnE3Zv22Z6wGOqGGPQFz4IxE1rrDFs5F7ZjDcBbpKoiYqD9yD2IGRQ2bbOE0l6B18MHnEAVFt7BOdNXm9B5tEktBLDn6h08pm+OMs/Am7z/V1AgMBAAGjggHkMIIB4DByBggrBgEFBQcBAQRmMGQwNQYIKwYBBQUHMAKGKWh0dHBzOi8vc21hcnRzaWduLmNvbS52bi9zbWFydHNpZ24yNTYuY3J0MCsGCCsGAQUFBzABhh9odHRwOi8vb2NzcDI1Ni5zbWFydHNpZ24uY29tLnZuMB0GA1UdDgQWBBQhxC7HLa/IBm21t/iI54KsPyM0aTAMBgNVHRMBAf8EAjAAMB8GA1UdIwQYMBaAFNAKWVIcyorACdIEKJLqk/vjQoYfMCgGCCsGAQUFBwEDBBwwGjAYBggrBgEFBQcLATAMBgorBgEEAYHtAwEHMIGRBgNVHSAEgYkwgYYwgYMGCisGAQQBge0DAQcwdTBKBggrBgEFBQcCAjA+HjwAVABoAGkAcwAgAGkAcwAgAGEAYwBjAHIAZQBkAGkAdABlAGQAIABjAGUAcgB0AGkAZgBpAGMAYQB0AGUwJwYIKwYBBQUHAgEWG2h0dHA6Ly9zbWFydHNpZ24uY29tLnZuL2NwczAvBgNVHR8EKDAmMCSgIqAghh5odHRwOi8vY3JsMjU2LnNtYXJ0c2lnbi5jb20udm4wDgYDVR0PAQH/BAQDAgPIMB0GA1UdJQQWMBQGCCsGAQUFBwMCBggrBgEFBQcDBDANBgkqhkiG9w0BAQsFAAOCAQEAD0NK6YluCa1oYFSXpV6ELWKKZ1HqXSto2lbVuvUSfhehUIJjDYq3Juiybn7UmvCSYqXW/9XnUHxjN6ySZswokfQlWAnae99v5RtD+gC62mOGnQ87zSQJJWaXvuOlNTpgFNR3qU/6A+vrwqRUAeRMDoKUa8QCgh2k/wDpAzl2qn3aIqoUb3S1cFDa+8VH5H00bamvvZcpIiWPHKD3B0a8Giz6Uqq6xl0YCgNSF0RnOIV7hdyt1xJMdg3yVgyWCFNIFwcW01PAL5XIROC6P4MLQZnySVRPvXoG/v/VMkvY3HKqvnPg6RlFc2XbgiGNcdeeF4ZDr4kawU5uDuEwk84vjA==</X509Certificate></X509Data></KeyInfo><Object Id=\"SigningTime\"><SignatureProperties Id=\"SigProps\" xmlns=\"\"><SignatureProperty Target=\"#proid\"><SigningTime xmlns=\"http://example.org/#signatureProperties\">2021-12-29T10:36:07Z</SigningTime></SignatureProperty></SignatureProperties></Object></Signature></NBan><CQT><Signature xmlns=\"http://www.w3.org/2000/09/xmldsig#\" Id=\"Tct-febe54fd5dbf4abb98a9c72fb2243e9b\"><SignedInfo><CanonicalizationMethod Algorithm=\"http://www.w3.org/TR/2001/REC-xml-c14n-20010315\" /><SignatureMethod Algorithm=\"http://www.w3.org/2001/04/xmldsig-more#rsa-sha256\" /><Reference URI=\"#data\"><DigestMethod Algorithm=\"http://www.w3.org/2001/04/xmlenc#sha256\" /><DigestValue>gETv9MH/DTVnDqrbghw4DZOUrHKv3PuqFc05McqMz6g=</DigestValue></Reference><Reference URI=\"#SigningTime-387d6819552141ac8bea6cc40e294454\"><DigestMethod Algorithm=\"http://www.w3.org/2001/04/xmlenc#sha256\" /><DigestValue>vxivOoUqi6RRpDZNoq42LRWyH7aAmO3O6CNgI4EJ1o8=</DigestValue></Reference><Reference URI=\"#Id-7721300047e84265b4b873e00eaecf4a\"><DigestMethod Algorithm=\"http://www.w3.org/2001/04/xmlenc#sha256\" /><DigestValue>lOsN+mYHiYd9A4vzefQzago+oRcpKlBJcEOuHCPBa7c=</DigestValue></Reference></SignedInfo><SignatureValue>E5aL0Quk/HK9dzRguJz+82cVlyN0k2iPclH8XIk8Zh6t5W35ReqDxOPkm2W+QWrwMjXv55MNacwE/ZMRdtUi9LwMhJEgk8MLIW5yfHDrWYzlLe5UdOtAXK1QU+OoF8u4aKsLpyGhAmxGYRM9Venb7ZNXa2auclsixp89fc8mQNhsphhcelTWJEI85vvEaPXX2+nRwA9YuloDfIhF6g2GWzoq4WVYMOIZ1ZxayivH1EsHC8NVJJEga+RXh0zFdia7a1a7Ky+BObvWpIaZ0fayMIecU6gCp1CH3Gm/gTPa+S5t3kKYDiabWGs8ufShnVhs8HqBzytl/FDzgYNFFAY5Zw==</SignatureValue><KeyInfo><X509Data><X509SubjectName>CN=TỔNG CỤC THUẾ,O=BỘ TÀI CHÍNH,L=Hà Nội,C=VN</X509SubjectName><X509Certificate>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</X509Certificate></X509Data></KeyInfo><Object Id=\"SigningTime-387d6819552141ac8bea6cc40e294454\"><SignatureProperties><SignatureProperty Target=\"signatureProperties\"><SigningTime>2021-12-29T12:48:41</SigningTime></SignatureProperty></SignatureProperties></Object></Signature></CQT></DSCKS></HDon></DLieu></TDiep><TDiep><TTChung><PBan>2.0.0</PBan><MNGui>TCT</MNGui><MNNhan>V0401486901</MNNhan><MLTDiep>999</MLTDiep><MTDiep>TCT018CF2522F3B411C840651DC42914588</MTDiep><MTDTChieu>V04014869011FCE1CE41BA54914ABAB3048C8C8185D</MTDTChieu></TTChung><DLieu><TBao><MTDiep>V04014869011FCE1CE41BA54914ABAB3048C8C8185D</MTDiep><MNGui>V0401486901</MNGui><NNhan>2021-12-29T12:48:41</NNhan><TTTNhan>0</TTTNhan></TBao></DLieu></TDiep></DuLieu></KetQuaTraCuu>");
//		rTCTN = commons.stringToDocument("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?><KetQuaTraCuu><MaKetQua>0</MaKetQua><MoTaKetQua>CQT đã trả kết quả xử lý thông điệp</MoTaKetQua><DuLieu><TDiep><TTChung><PBan>2.0.0</PBan><MNGui>TCT</MNGui><MNNhan>V0401486901</MNNhan><MLTDiep>999</MLTDiep><MTDiep>TCTA24C53EFF7B34EBCB1244436B95402AA</MTDiep><MTDTChieu>V0401486901A60175119CFC42658C4ED3E5FFAD1190</MTDTChieu></TTChung><DLieu><TBao><MTDiep>V0401486901A60175119CFC42658C4ED3E5FFAD1190</MTDiep><MNGui>V0401486901</MNGui><NNhan>2021-12-31T16:06:21</NNhan><TTTNhan>0</TTTNhan></TBao></DLieu></TDiep><TDiep><TTChung><PBan>2.0.0</PBan><MNGui>TCT</MNGui><MNNhan>V0401486901</MNNhan><MLTDiep>999</MLTDiep><MTDiep>TCTA24C53EFF7B34EBCB1244436B95402AA</MTDiep><MTDTChieu>V0401486901A60175119CFC42658C4ED3E5FFAD1190</MTDTChieu></TTChung><DLieu><TBao><MTDiep>V0401486901A60175119CFC42658C4ED3E5FFAD1190</MTDiep><MNGui>V0401486901</MNGui><NNhan>2021-12-31T16:06:21</NNhan><TTTNhan>0</TTTNhan></TBao></DLieu></TDiep><TDiep><TTChung><PBan>2.0.0</PBan><MNGui>TCT</MNGui><MNNhan>V0401486901</MNNhan><MLTDiep>202</MLTDiep><MTDiep>TCT072DD37657504F8D9E1658DE1D3E0D03</MTDiep><MTDTChieu>V0401486901A60175119CFC42658C4ED3E5FFAD1190</MTDTChieu><MST>0301521415</MST><SLuong>1</SLuong></TTChung><DLieu><HDon><DLHDon Id=\"data\"><TTChung><PBan>2.0.0</PBan><THDon>Hóa đơn giá trị gia tăng TT 78</THDon><KHMSHDon>1</KHMSHDon><KHHDon>C21TBB</KHHDon><SHDon>309</SHDon><MHSo /><NLap>2021-12-31</NLap><SBKe /><NBKe /><DVTTe>VND</DVTTe><TGia>1</TGia><HTTToan>Tiền mặt</HTTToan><MSTTCGP>0315382923</MSTTCGP><MSTDVNUNLHDon /><TDVNUNLHDon /><DCDVNUNLHDon /><TTKhac><TTin><TTruong>PortalLink</TTruong><KDLieu>string</KDLieu><DLieu>www.tothanhphat.vn/ttp-hddt/tracuuhd</DLieu></TTin><TTin><TTruong>SecureKey</TTruong><KDLieu>string</KDLieu><DLieu>707766</DLieu></TTin><TTin><TTruong>SystemKey</TTruong><KDLieu>string</KDLieu><DLieu>61cec2fdf4c0622340fea714</DLieu></TTin></TTKhac></TTChung><NDHDon><NBan><Ten>CÔNG TY CỔ PHẦN TÔ THÀNH PHÁT</Ten><MST>0301521415</MST><DChi>56 Phạm Hữu Chí, Phường 12, Quận 5, Thành phố Hồ Chí Minh</DChi><SDThoai>(028) 37.600.707</SDThoai><DCTDTu>tothanhphatbc@vnnn.vn</DCTDTu><STKNHang /><TNHang>Ngân hàng VCB - CN Bình Tây</TNHang><Fax /><Website>www.ttpvn.com</Website><TTKhac /></NBan><NMua><Ten>Cửa Hàng Lý Vinh</Ten><MST /><DChi>ấp Thạnh Kiết, An Thạnh Thủy, Chợ Gạo, Tiền Giang</DChi><MKHang /><SDThoai /><DCTDTu>ketoantothanhphat@gmail.com</DCTDTu><HVTNMHang /><STKNHang /><TNHang /><TTKhac /></NMua><DSHHDVu><HHDVu><TChat>1</TChat><STT>1</STT><MHHDVu /><THHDVu>Gạch TRM 40X40 (6Viên/Thùng) loại 1 - T4107RL1</THHDVu><DVTinh>Thùng</DVTinh><SLuong>160</SLuong><DGia>57273</DGia><TLCKhau /><STCKhau /><ThTien>9163680</ThTien><TSuat>10%</TSuat><TTKhac><TTin><TTruong>VATAmount</TTruong><KDLieu>decimal</KDLieu><DLieu>916368</DLieu></TTin><TTin><TTruong>Amount</TTruong><KDLieu>decimal</KDLieu><DLieu>10080048</DLieu></TTin></TTKhac></HHDVu></DSHHDVu><TToan><THTTLTSuat><LTSuat><TSuat>10%</TSuat><ThTien>9163680</ThTien><TThue>916368</TThue></LTSuat></THTTLTSuat><TgTCThue>9163680</TgTCThue><TgTThue>916368</TgTThue><DSLPhi><LPhi><TLPhi /><TPhi>0</TPhi></LPhi></DSLPhi><TTCKTMai>0</TTCKTMai><TgTTTBSo>10080048</TgTTTBSo><TgTTTBChu>Mười triệu không trăm tám mươi nghìn không trăm bốn mươi tám đồng.</TgTTTBChu><TTKhac /></TToan></NDHDon></DLHDon><MCCQT Id=\"Id-6bd8695dc07f439bb78daacc6f2ef395\">00885B0626417F476F8CE9F5539D55129E</MCCQT><DSCKS><NBan><Signature xmlns=\"http://www.w3.org/2000/09/xmldsig#\" Id=\"proid\"><SignedInfo><CanonicalizationMethod Algorithm=\"http://www.w3.org/TR/2001/REC-xml-c14n-20010315\" /><SignatureMethod Algorithm=\"http://www.w3.org/2000/09/xmldsig#rsa-sha1\" /><Reference URI=\"#data\"><Transforms><Transform Algorithm=\"http://www.w3.org/2000/09/xmldsig#enveloped-signature\" /><Transform Algorithm=\"http://www.w3.org/TR/2001/REC-xml-c14n-20010315\" /></Transforms><DigestMethod Algorithm=\"http://www.w3.org/2000/09/xmldsig#sha1\" /><DigestValue>EBW77m6XTvb8/hGklcKmCltd6lg=</DigestValue></Reference><Reference URI=\"#SigningTime\"><Transforms><Transform Algorithm=\"http://www.w3.org/TR/2001/REC-xml-c14n-20010315\" /></Transforms><DigestMethod Algorithm=\"http://www.w3.org/2000/09/xmldsig#sha1\" /><DigestValue>8dw1uLeDdl54nl16yDRj3Y+9nKo=</DigestValue></Reference></SignedInfo><SignatureValue>JTarlazBEes/pmWx80qJL1lng9DFtj/k+CxCnb4Wnw0/XmixPnkBjTL47sLbk4Zzz3FrYpMiki5rgKseKuVgb4Avmf4QvFtuDHLGnaIIxqWhLEAwcdsBtiFADUDsSPNTFjAYw5JDr6TJ5652Y6QPo03nEWbIHJDghPhoqQPF6j0=</SignatureValue><KeyInfo><KeyValue><RSAKeyValue><Modulus>tCwXgWNBkFSOn8H8i1ngFVT62fwI2LSufN1bH1zaoiytqIA4M60A2H+iuwry3dicTdm/bZnrAY6oYY9AXPgjETWusMWzkXtmMNwFukqiJioP3IPYgZFDZts4TSXoHXwwecQBUW3sE501eb0Hm0SS0EsOfqHTymb44yz8CbvP9XU=</Modulus><Exponent>AQAB</Exponent></RSAKeyValue></KeyValue><X509Data><X509IssuerSerial><X509IssuerName>CN=SmartSign, OU=Cong ty co phan chu ky so VI NA, O=Cong ty co phan chu ky so VI NA, C=VN</X509IssuerName><X509SerialNumber>111660364806195056320980835436687974772</X509SerialNumber></X509IssuerSerial><X509SubjectName>C=VN, S=HỒ CHÍ MINH, CN=CÔNG TY CỔ PHẦN TÔ THÀNH PHÁT, OID.0.9.2342.19200300.100.1.1=MST:0301521415</X509SubjectName><X509Certificate>MIIE1zCCA7+gAwIBAgIQVAEBB1VK9cLCI9QLDebhdDANBgkqhkiG9w0BAQsFADB1MQswCQYDVQQGEwJWTjEoMCYGA1UECgwfQ29uZyB0eSBjbyBwaGFuIGNodSBreSBzbyBWSSBOQTEoMCYGA1UECwwfQ29uZyB0eSBjbyBwaGFuIGNodSBreSBzbyBWSSBOQTESMBAGA1UEAwwJU21hcnRTaWduMB4XDTIxMTIwOTA4MzgzNVoXDTI0MTIwODA4MzgzNVowdjEeMBwGCgmSJomT8ixkAQEMDk1TVDowMzAxNTIxNDE1MS4wLAYDVQQDDCVDw5RORyBUWSBD4buUIFBI4bqmTiBUw5QgVEjDgE5IIFBIw4FUMRcwFQYDVQQIDA5I4buSIENIw40gTUlOSDELMAkGA1UEBhMCVk4wgZ8wDQYJKoZIhvcNAQEBBQADgY0AMIGJAoGBALQsF4FjQZBUjp/B/ItZ4BVU+tn8CNi0rnzdWx9c2qIsraiAODOtANh/orsK8t3YnE3Zv22Z6wGOqGGPQFz4IxE1rrDFs5F7ZjDcBbpKoiYqD9yD2IGRQ2bbOE0l6B18MHnEAVFt7BOdNXm9B5tEktBLDn6h08pm+OMs/Am7z/V1AgMBAAGjggHkMIIB4DByBggrBgEFBQcBAQRmMGQwNQYIKwYBBQUHMAKGKWh0dHBzOi8vc21hcnRzaWduLmNvbS52bi9zbWFydHNpZ24yNTYuY3J0MCsGCCsGAQUFBzABhh9odHRwOi8vb2NzcDI1Ni5zbWFydHNpZ24uY29tLnZuMB0GA1UdDgQWBBQhxC7HLa/IBm21t/iI54KsPyM0aTAMBgNVHRMBAf8EAjAAMB8GA1UdIwQYMBaAFNAKWVIcyorACdIEKJLqk/vjQoYfMCgGCCsGAQUFBwEDBBwwGjAYBggrBgEFBQcLATAMBgorBgEEAYHtAwEHMIGRBgNVHSAEgYkwgYYwgYMGCisGAQQBge0DAQcwdTBKBggrBgEFBQcCAjA+HjwAVABoAGkAcwAgAGkAcwAgAGEAYwBjAHIAZQBkAGkAdABlAGQAIABjAGUAcgB0AGkAZgBpAGMAYQB0AGUwJwYIKwYBBQUHAgEWG2h0dHA6Ly9zbWFydHNpZ24uY29tLnZuL2NwczAvBgNVHR8EKDAmMCSgIqAghh5odHRwOi8vY3JsMjU2LnNtYXJ0c2lnbi5jb20udm4wDgYDVR0PAQH/BAQDAgPIMB0GA1UdJQQWMBQGCCsGAQUFBwMCBggrBgEFBQcDBDANBgkqhkiG9w0BAQsFAAOCAQEAD0NK6YluCa1oYFSXpV6ELWKKZ1HqXSto2lbVuvUSfhehUIJjDYq3Juiybn7UmvCSYqXW/9XnUHxjN6ySZswokfQlWAnae99v5RtD+gC62mOGnQ87zSQJJWaXvuOlNTpgFNR3qU/6A+vrwqRUAeRMDoKUa8QCgh2k/wDpAzl2qn3aIqoUb3S1cFDa+8VH5H00bamvvZcpIiWPHKD3B0a8Giz6Uqq6xl0YCgNSF0RnOIV7hdyt1xJMdg3yVgyWCFNIFwcW01PAL5XIROC6P4MLQZnySVRPvXoG/v/VMkvY3HKqvnPg6RlFc2XbgiGNcdeeF4ZDr4kawU5uDuEwk84vjA==</X509Certificate></X509Data></KeyInfo><Object Id=\"SigningTime\"><SignatureProperties Id=\"SigProps\" xmlns=\"\"><SignatureProperty Target=\"#proid\"><SigningTime xmlns=\"http://example.org/#signatureProperties\">2021-12-31T16:06:13Z</SigningTime></SignatureProperty></SignatureProperties></Object></Signature></NBan><CQT><Signature xmlns=\"http://www.w3.org/2000/09/xmldsig#\" Id=\"Tct-a947e0b1eebf48f5ae610d759bc52ad5\"><SignedInfo><CanonicalizationMethod Algorithm=\"http://www.w3.org/TR/2001/REC-xml-c14n-20010315\" /><SignatureMethod Algorithm=\"http://www.w3.org/2001/04/xmldsig-more#rsa-sha256\" /><Reference URI=\"#data\"><DigestMethod Algorithm=\"http://www.w3.org/2001/04/xmlenc#sha256\" /><DigestValue>QT1lWPK4BhTsDCCaFi3g7qEog71LfCfYAaRusm5QQTk=</DigestValue></Reference><Reference URI=\"#SigningTime-a7f512016d334b09a8d1905e5a54aaa2\"><DigestMethod Algorithm=\"http://www.w3.org/2001/04/xmlenc#sha256\" /><DigestValue>WNQZq8Wf5oLOzvPWaC0E9yldY6I67jFpMzqjFYAhOJQ=</DigestValue></Reference><Reference URI=\"#Id-6bd8695dc07f439bb78daacc6f2ef395\"><DigestMethod Algorithm=\"http://www.w3.org/2001/04/xmlenc#sha256\" /><DigestValue>vMg04P6A356MnpTI28u8uh/+Z3ymBEzAsNlTbUF8W/4=</DigestValue></Reference></SignedInfo><SignatureValue>Z7nLk01RzPh+i7LcGK71aPOUPwm29Ep2wOIkRbzb6/wmvS42Jd8OJqS3e2AHdPeX3dqU17RPm3o6WMVziEFWXc/LRkygbndvA+8RPJ3pWhsUVHxPiz81mnd/DsXr6x7ZneHUtI6gEsLalL92rdh39pU/D0mR8xnI3cjBpW+gmvdiWt/741BiZigcxWEhVLawC8viOo+VP43sDtnQLfR85XiUbasUnRg2G+YAqllxJWQ767AwmpkPkfY7URj5WeMFjaq35OSNIYrSb0MnJXN+sd+z86zMsb45RMKX/5KGAGCAgvATQTPKlKmJfckibMkPcEraqqo428ryuVCgCgEI2w==</SignatureValue><KeyInfo><X509Data><X509SubjectName>CN=TỔNG CỤC THUẾ,O=BỘ TÀI CHÍNH,L=Hà Nội,C=VN</X509SubjectName><X509Certificate>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</X509Certificate></X509Data></KeyInfo><Object Id=\"SigningTime-a7f512016d334b09a8d1905e5a54aaa2\"><SignatureProperties><SignatureProperty Target=\"signatureProperties\"><SigningTime>2021-12-31T16:06:22</SigningTime></SignatureProperty></SignatureProperties></Object></Signature></CQT></DSCKS></HDon></DLieu></TDiep></DuLieu></KetQuaTraCuu>");
		if (rTCTN == null) {
			rTCTN = tctnService.callTraCuuThongDiep(MTDiep);
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

		if ("2".equals(MaKetQua)) {
			doc = commons.fileToDocument(file, true);
			rTCTN1 = tctnService.callTiepNhanThongDiep("200", MTDiep, MST, "1", doc);
			xPath = XPathFactory.newInstance().newXPath();
			Node nodeDLHDon = (Node) xPath.evaluate("/TDiep", rTCTN1, XPathConstants.NODE);
			MaKetQua = commons
					.getTextFromNodeXML((Element) xPath.evaluate("MaKetQua", nodeDLHDon, XPathConstants.NODE));
			MoTaKetQua = commons
					.getTextFromNodeXML((Element) xPath.evaluate("MoTaKetQua", nodeDLHDon, XPathConstants.NODE));
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

//		Node nodeTDiep = (Node) xPath.evaluate("DuLieu/TDiep[last()]", nodeKetQuaTraCuu, XPathConstants.NODE);
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
		for (int i = 1; i <= 20; i++) {

			if (xPath.evaluate("DuLieu/TDiep[" + i + "]", nodeKetQuaTraCuu, XPathConstants.NODE) == null)
				break;
			nodeTDiep = (Node) xPath.evaluate("DuLieu/TDiep[" + i + "]", nodeKetQuaTraCuu, XPathConstants.NODE);
			checkMLTDiep = commons
					.getTextFromNodeXML((Element) xPath.evaluate("TTChung/MLTDiep", nodeTDiep, XPathConstants.NODE));
			if (checkMLTDiep.equals("202"))
				break;
			if (checkMLTDiep.equals("204")) {
				check_ = true;
				MLoi1 = commons.getTextFromNodeXML((Element) xPath.evaluate("DLieu/TBao/DLTBao/LCMa/DSLDo/LDo/MLoi",
						nodeTDiep, XPathConstants.NODE));
				MTLoi1 = commons.getTextFromNodeXML((Element) xPath.evaluate("DLieu/TBao/DLTBao/LCMa/DSLDo/LDo/MTLoi",
						nodeTDiep, XPathConstants.NODE));

				CQT_MLTDiep1 = checkMLTDiep;

			}

//			if(xPath.evaluate("DLieu/*/MCCQT", nodeTDiep, XPathConstants.NODE) != null)
//				break;
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
			collection = mongoClient.getDatabase(cfg.dbName).getCollection("EInvoicePXKDL");
			collection.findOneAndUpdate(docFind,
					new Document("$set", new Document("EInvoiceStatus", Constants.INVOICE_STATUS.ERROR_CQT)
							.append("LDo", new Document("MLoi", MLoi).append("MTLoi", MTLoi))),
					options);
			mongoClient.close();

			responseStatus = new MspResponseStatus(0,
					"".equals(MTLoi) ? "CQT chưa có thông báo kết quả trả về." : MTLoi);
			rsp.setResponseStatus(responseStatus);
			return rsp;
		}
		if ("|202|".indexOf("|" + CQT_MLTDiep + "|") == -1) {
			responseStatus = new MspResponseStatus(9999, "CQT chưa có thông báo kết quả trả về.");
			rsp.setResponseStatus(responseStatus);
			return rsp;
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
		collection = mongoClient2.getDatabase(cfg.dbName).getCollection("EInvoicePXKDL");
		collection.findOneAndUpdate(docFind, new Document("$set",
				new Document("EInvoiceStatus", Constants.INVOICE_STATUS.COMPLETE).append("MCCQT", MCCQT)
						.append("MTDTChieu", MTDTChieu).append("LDo", new Document("MLoi", "").append("MTLoi", ""))),
				options);
		mongoClient2.close();

		responseStatus = new MspResponseStatus(0, Constants.MAP_ERROR.get(0));
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
				new Document("_id", objectId).append("IsActive", true).append("IsDelete", new Document("$ne", true))));
		pipeline.add(new Document("$project", findInforIssuer));
		
		pipeline.add(new Document("$lookup",
				new Document("from", "Users").append("pipeline", Arrays.asList(
						new Document("$match",
								new Document("IssuerId", header.getIssuerId()).append("_id", objectIdUser)
										.append("IsActive", true).append("IsDelete", new Document("$ne", true))),
						new Document("$project", new Document("_id", 1).append("UserName", 1).append("FullName", 1)),
						new Document("$limit", 1))).append("as", "UserInfo"))

		);
		pipeline.add(
				new Document("$unwind", new Document("path", "$UserInfo").append("preserveNullAndEmptyArrays", true)));
		pipeline.add(new Document("$lookup",
				new Document("from", "DMMauSoKyHieu").append("pipeline", Arrays.asList(
						new Document("$match",
								new Document("IssuerId", header.getIssuerId()).append("IsActive", true)
										.append("IsDelete", new Document("$ne", true))
										.append("ConLai", new Document("$gt", 0)).append("_id", objectIdMSKH)),
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
		List<EInvoicePXKDLExcelForm> eInvoicePXKDLExcelFormList = new ArrayList<>();
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
				List<Cell> cells = new ArrayList<Cell>();
				int lastColumn = Math.max(row1.getLastCellNum(), 25);

				for (int cn = 0; cn < lastColumn; cn++) {
					Cell c = row1.getCell(cn, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
					cells.add(c);
				}
				EInvoicePXKDLExcelForm eInvoicePXKDLExcelForm = extractInfoFromCell(cells);
				eInvoicePXKDLExcelFormList.add(eInvoicePXKDLExcelForm);
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
		if (eInvoicePXKDLExcelFormList != null) {
			for (int tam = 0; tam < eInvoicePXKDLExcelFormList.size(); tam++) {
				if (eInvoicePXKDLExcelFormList.get(tam).getMaHD() == null) {
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
//			String tempTenHH = "";
			String tempDChiXuat = "";
			String tempDChiNhap = "";
			String tempHDKTSo = "";
			String tempHDKTNgay = "";
			String tempPTVChuyen = "";
			String tempTenNDDien = "";
			String tempDCTDTu = "";
			String tempNDNKho = "";
			String tempNLap = "";
			String tempMSTNNhap = "";
			String tempTDVNNhap = "";
			String tempHVTNVChuyen = "";
			String tempHVTNXHang = "";
			String tempHDSo = "";
//			Double tempTgTCThue = 0.0;
//			Double tempTgTThue = 0.0;
			Double tempTTien = 0.0;
			String tempTgTTTBChu = "";
//			String tempHTTToan = "";
			List<DSHHDVu> dshhdVuList = new ArrayList<>();
			List<Object> listHHDVu = new ArrayList<Object>();
			int i = 0;
			int start = 0;
			int end = 0;
			int dem = 0;

			/* DOC FILE EXCEL - GHI DU LIEU VO LIST */
			for (; i < eInvoicePXKDLExcelFormList.size();) {
				dem = 0;
				for (int j = i; j < eInvoicePXKDLExcelFormList.size(); j++) {
					if (eInvoicePXKDLExcelFormList.get(i).getMaHD() == eInvoicePXKDLExcelFormList.get(j).getMaHD()) {
						// Xu ly
						dem++;
						start = j + 1;
						tempNLap = eInvoicePXKDLExcelFormList.get(i).getNHDon();
//						tempTenHH = eInvoicePXKDLExcelFormList.get(i).getTHHoa();
						tempDCTDTu = eInvoicePXKDLExcelFormList.get(i).getEmailKNhan();
						tempHDKTSo = eInvoicePXKDLExcelFormList.get(i).getHDKTSo();
						tempHDKTNgay = eInvoicePXKDLExcelFormList.get(i).getHDKTNgay();
						tempDChiXuat = eInvoicePXKDLExcelFormList.get(i).getXTKho();
						tempDChiNhap = eInvoicePXKDLExcelFormList.get(i).getNTkho();
						tempPTVChuyen = eInvoicePXKDLExcelFormList.get(i).getPTVChuyen();
						tempTenNDDien = eInvoicePXKDLExcelFormList.get(i).getCua();
						tempNDNKho = eInvoicePXKDLExcelFormList.get(i).getVViec();
						tempHVTNVChuyen = eInvoicePXKDLExcelFormList.get(i).getHVTNVChuyen();
						tempHVTNXHang = eInvoicePXKDLExcelFormList.get(i).getHVTNNHang();
						tempMSTNNhap = eInvoicePXKDLExcelFormList.get(i).getMSTNNhap();
						tempTDVNNhap = eInvoicePXKDLExcelFormList.get(i).getTenDV();
						// tempHDSo = eInvoicePXKDLExcelFormList.get(i).getHDso();
						tempTTien = eInvoicePXKDLExcelFormList.get(i).getTTien();
						tempTgTTTBChu = eInvoicePXKDLExcelFormList.get(i).getTBchu();
						end = j;
						if (eInvoicePXKDLExcelFormList.size() == j + 1) {
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
				String TenForm = tempTDVNNhap;
				String DCTDTuNMForm = tempDCTDTu;
				String HDKTSoForm = tempHDKTSo;
				String HDKTNgayForm = tempHDKTNgay;
				String XTKhoForm = tempDChiXuat;
				String NTKhoForm = tempDChiNhap;
				String HVTNVChuyenNMForm = tempHVTNVChuyen;
				String NLapForm = tempNLap;
				String tempHVTNXHangForm = tempHVTNXHang;
				String tempHDSoForm = tempHDSo;
				String PTVChuyenForm = tempPTVChuyen;
				String TenNDDienForm = tempTenNDDien;
				String NDNKhoForm = tempNDNKho;
				Double tempTTienForm = tempTTien;
				String MSTNhap = tempMSTNNhap;

				String tempTgTTTBChuForm = tempTgTTTBChu;

				if (checkMaHD == true) {
					if (dem > 1) {

						for (int k = i; k <= end; k++) {
							DSHHDVu dshhdVu = new DSHHDVu();
							dshhdVu.setSTT(eInvoicePXKDLExcelFormList.get(k).getSTT());
							dshhdVu.setProductName(eInvoicePXKDLExcelFormList.get(k).getTHHoa());
							dshhdVu.setProductCode(eInvoicePXKDLExcelFormList.get(k).getMaHHoa());
							dshhdVu.setUnit(eInvoicePXKDLExcelFormList.get(k).getDVTinh());
							dshhdVu.setQuantity(eInvoicePXKDLExcelFormList.get(k).getTXuat());
							dshhdVu.setPrice(eInvoicePXKDLExcelFormList.get(k).getDGia());
							dshhdVu.setTTien(eInvoicePXKDLExcelFormList.get(k).getTTien());
							dshhdVu.setTotal(eInvoicePXKDLExcelFormList.get(k).getTgTien());
							String TinhChat = eInvoicePXKDLExcelFormList.get(k).getLHHoa();
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
							hItem1.put("Unit", dshhdVu.getUnit());
							hItem1.put("Quantity", dshhdVu.getQuantity());
							hItem1.put("Price", dshhdVu.getPrice());
							hItem1.put("Amount", dshhdVu.getTTien());
							hItem1.put("Total", dshhdVu.getTotal());
							hItem1.put("Feature", dshhdVu.getFeature());
							listHHDVu.add(hItem1);

						}

						// Thông tin hóa đơn - TTChung
//						String MaHD = eInvoicePXKDLExcelFormList.get(i).getMaHD();
						String THDon = "PHIẾU XUẤT KHO HÀNG GỬI BÁN ĐẠI LÝ ĐIỆN TỬ";
						LocalDateTime NLap = LocalDateTime.now();
						String DVTTe = "VND";
						String TGia = "1";

//                        TTChung ttChung = new TTChung(MaHD, THDon, MauSoHD, KHMSHDon, KHHDon, NLap, DVTTe, TGia);

						// Thông tin người bán - Thông tin người mua - NDHDon

						String HDKTSo = HDKTSoForm;
						String HDKTNgay = HDKTNgayForm;

						String HVTNVChuyen = HVTNVChuyenNMForm;
						String HVTNXHang = tempHVTNXHangForm;
						String HDSo = tempHDSoForm;
						String NLap1 = NLapForm;
						String PTVChuyen = PTVChuyenForm;
						String TNDDien = TenNDDienForm;
						String NDNKho = NDNKhoForm;
						String XTKho = XTKhoForm;

						String TenNM = TenForm;
						String DChiNM = NTKhoForm;
						String MSTNM = MSTNhap;
						String DCTDTuNM = DCTDTuNMForm;

						String HDSoCheck = "";
						if (HDSo == null) {
							HDSoCheck = "";
						} else {
							HDSoCheck = HDSo;
						}

						String EmailNM = "";
						if (DCTDTuNM == null) {
							EmailNM = "";
						} else {
							EmailNM = DCTDTuNM;
						}
						// Thông tin thanh toán
//						Double TgTCThue = 0.0;
						Double TgTThue = 0.0;
						Double TgTTTBSo = tempTTienForm;
						String TgTTTBChu = tempTgTTTBChuForm;
						String TBChu = "";
						if (TgTTTBChu == null) {
							TBChu = "Không đồng!";
						} else {
							TBChu = TgTTTBChu;
						}

						// Một số thông tin khác
						String MTDiep = "";
						String SecureKey = "";

						// Setting
						String codeMTD = "0315382923";

						String pathDir = "";
						File file1 = null;
						Path path1 = null;
						ObjectId objectIdEInvoice = null;
						SecureKey = commons.csRandomNumbericString(6);
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

						MTDiep = codeMTD + commons.csRandomAlphaNumbericString(46 - codeMTD.length()).toUpperCase();
						SecureKey = commons.csRandomNumbericString(6);
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

						elementSubTmp = null;
						elementSubTmp01 = null;
						elementSubContent = doc.createElement("TTChung");
						elementTmp = null;

						elementSubContent
								.appendChild(commons.createElementWithValue(doc, "PBan", SystemParams.VERSION_XML_PXK));
						elementSubContent.appendChild(commons.createElementWithValue(doc, "THDon", THDon));
						elementSubContent.appendChild(commons.createElementWithValue(doc, "KHMSHDon",
								docTmp.getEmbedded(Arrays.asList("DMMauSoKyHieu", "KHMSHDon"), "")));
						elementSubContent.appendChild(commons.createElementWithValue(doc, "KHHDon",
								docTmp.getEmbedded(Arrays.asList("DMMauSoKyHieu", "KHHDon"), "")));
						elementSubContent.appendChild(commons.createElementWithValue(doc, "SHDon", "")); // SE PHAT SINH
																											// KHI KY
						// Ngày lập
						elementSubContent.appendChild(commons.createElementWithValue(doc, "NLap",
								commons.convertLocalDateTimeStringToString(NLap1, Constants.FORMAT_DATE.FORMAT_DATE_WEB,
										Constants.FORMAT_DATE.FORMAT_DATE_EINVOICE, false)));
						// Đơn vị tiền tệ
						elementSubContent.appendChild(commons.createElementWithValue(doc, "DVTTe", DVTTe));
						// Tỷ giá
						elementSubContent.appendChild(commons.createElementWithValue(doc, "TGia", TGia));
						// MST tổ chức cung cấp giải pháp HĐĐT
						elementSubContent
								.appendChild(commons.createElementWithValue(doc, "MSTTCGP", SystemParams.MSTTCGP));

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
						elementTmp.appendChild(commons.createElementWithValue(doc, "Ten", docTmp.get("Name", "")));
						elementTmp.appendChild(commons.createElementWithValue(doc, "MST", docTmp.get("TaxCode", "")));
						// lệnh điều động nội bộ
						elementTmp.appendChild(commons.createElementWithValue(doc, "HDKTSo", HDKTSo));
						elementTmp.appendChild(commons.createElementWithValue(doc, "HDKTNgay",
								commons.convertLocalDateTimeStringToString(HDKTNgay,
										Constants.FORMAT_DATE.FORMAT_DATE_WEB,
										Constants.FORMAT_DATE.FORMAT_DATE_EINVOICE, false)));
						elementTmp.appendChild(commons.createElementWithValue(doc, "DChi", docTmp.get("Address", "")));
						elementTmp.appendChild(commons.createElementWithValue(doc, "HDSo", ""));
						elementTmp.appendChild(commons.createElementWithValue(doc, "HVTNXHang", HVTNXHang));
						elementTmp.appendChild(commons.createElementWithValue(doc, "TNVChuyen", HVTNVChuyen));
						elementTmp.appendChild(commons.createElementWithValue(doc, "PTVChuyen", PTVChuyen));
//            			elementTmp.appendChild(commons.createElementWithValue(doc, "TTKhac", ""));

						/* ADD THONG TIN TK NGAN HANG (NEU CO) */
						elementSubTmp = doc.createElement("TTKhac");
						elementSubTmp.appendChild(commons.createElementTTKhac(doc, "ComAddress", "string", XTKho));
						elementSubTmp.appendChild(
								commons.createElementTTKhac(doc, "ComPhone", "string", docTmp.get("Phone", "")));
						elementSubTmp.appendChild(
								commons.createElementTTKhac(doc, "ComEmail", "string", docTmp.get("Email", "")));

						if (docTmp.get("BankAccountExt") != null
								&& docTmp.getList("BankAccountExt", Document.class).size() > 0) {
							intTmp = 1;
							for (Document oo : docTmp.getList("BankAccountExt", Document.class)) {
								elementSubTmp.appendChild(commons.createElementTTKhac(doc, "STKNHang" + intTmp,
										"string", oo.get("AccountNumber", "")));
								elementSubTmp.appendChild(commons.createElementTTKhac(doc, "TNHang" + intTmp, "string",
										oo.get("BankName", "")));
								elementTmp.appendChild(elementSubTmp);
								intTmp++;
							}
							elementTmp.appendChild(elementSubTmp);
						}
						elementSubTmp.appendChild(
								commons.createElementTTKhac(doc, "ComFax", "string", docTmp.get("Fax", "")));
						elementSubTmp.appendChild(commons.createElementTTKhac(doc, "PostBy", "string", TNDDien));
						elementSubTmp
								.appendChild(commons.createElementTTKhac(doc, "PostDescription", "string", NDNKho));
						elementTmp.appendChild(elementSubTmp);

						elementSubContent.appendChild(elementTmp);

						elementTmp = doc.createElement("NMua"); // NGUOI MUA
						elementTmp.appendChild(commons.createElementWithValue(doc, "Ten", TenNM));
						elementTmp.appendChild(commons.createElementWithValue(doc, "MST", MSTNM));
						elementTmp.appendChild(commons.createElementWithValue(doc, "DChi", DChiNM));
						elementTmp.appendChild(commons.createElementWithValue(doc, "HVTNMHang", HVTNXHang));

						elementSubTmp = doc.createElement("TTKhac"); // THONG TIN KHAC
						elementSubTmp.appendChild(commons.createElementTTKhac(doc, "MoveNo", "string", HDKTSo));
						elementSubTmp.appendChild(commons.createElementTTKhac(doc, "MoveDate", "string",
								commons.convertLocalDateTimeStringToString(HDKTNgay,
										Constants.FORMAT_DATE.FORMAT_DATE_WEB,
										Constants.FORMAT_DATE.FORMAT_DATE_EINVOICE, false)));
						elementSubTmp
								.appendChild(commons.createElementTTKhac(doc, "TransportDivice", "string", PTVChuyen));
						elementSubTmp
								.appendChild(commons.createElementTTKhac(doc, "TransportName", "string", HVTNVChuyen));
						elementTmp.appendChild(elementSubTmp);
						elementSubContent.appendChild(elementTmp);

						mapVATAmount = new LinkedHashMap<String, Double>();
						mapAmount = new LinkedHashMap<String, Double>();
						elementTmp = doc.createElement("DSHHDVu"); // HH-DV

						for (Object o : listHHDVu) {
							if (!"".equals(o.equals("/ProductName"))) {
								JsonNode h = Json.serializer().nodeFromObject(o);
								tmp = commons.getTextJsonNode(h.at("/VATRate")).replaceAll(",", "");
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
										|| "3".equals(commons.getTextJsonNode(h.at("/Feature")))) {
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
								elementSubTmp.appendChild(commons.createElementWithValue(doc, "SLuong",
										commons.getTextJsonNode(h.at("/Quantity")).replaceAll(",", "")));
								elementSubTmp.appendChild(commons.createElementWithValue(doc, "DGia",
										commons.getTextJsonNode(h.at("/Price")).replaceAll(",", "")));
								elementSubTmp.appendChild(commons.createElementWithValue(doc, "ThTien",
										commons.getTextJsonNode(h.at("/Total")).replaceAll(",", "")));
								elementSubTmp.appendChild(commons.createElementWithValue(doc, "TSuat", "0%"));

								elementSubTmp01 = doc.createElement("TTKhac");
								elementSubTmp01.appendChild(commons.createElementTTKhac(doc, "SLXuat", "String",
										commons.getTextJsonNode(h.at("/Total")).replaceAll(",", "")));
								elementSubTmp01.appendChild(commons.createElementTTKhac(doc, "VATAmount", "decimal",
										commons.getTextJsonNode(h.at("/VATAmount")).replaceAll(",", "")));
								elementSubTmp01.appendChild(commons.createElementTTKhac(doc, "Amount", "decimal",
										commons.getTextJsonNode(h.at("/Amount")).replaceAll(",", "")));
								elementSubTmp.appendChild(elementSubTmp01);

								elementTmp.appendChild(elementSubTmp);

							}
						}
						elementSubContent.appendChild(elementTmp);
						elementTmp = doc.createElement("TToan"); // Thong tin thanh toan
						elementSubTmp = doc.createElement("THTTLTSuat");
						/* DANH SACH CAC LOAI THUE SUAT */

//            			https://stackoverflow.com/questions/46898/how-do-i-efficiently-iterate-over-each-entry-in-a-java-map

						elementSubTmp01 = doc.createElement("LTSuat");
						elementSubTmp01.appendChild(commons.createElementWithValue(doc, "TSuat", "KCT"));
						elementSubTmp01.appendChild(commons.createElementWithValue(doc, "ThTien",
								commons.formatNumberReal(TgTTTBSo).replaceAll(",", "")));
						elementSubTmp01.appendChild(commons.createElementWithValue(doc, "TThue",
								commons.formatNumberReal(TgTThue).replaceAll(",", "")));
						elementSubTmp.appendChild(elementSubTmp01);

						elementTmp.appendChild(elementSubTmp);

						elementTmp.appendChild(commons.createElementWithValue(doc, "TgTCThue", "0"));
						elementTmp.appendChild(commons.createElementWithValue(doc, "TgTThue", "0"));
						elementTmp.appendChild(commons.createElementWithValue(doc, "TTCKTMai", "0"));
						elementTmp.appendChild(commons.createElementWithValue(doc, "TgTTTBSo",
								commons.formatNumberReal(TgTTTBSo).replaceAll(",", "")));
						elementTmp.appendChild(commons.createElementWithValue(doc, "TgTTTBChu", TBChu));

						elementContent.appendChild(elementSubContent);
						elementSubContent.appendChild(elementTmp);

						// END - NDHDon: Nội dung hóa đơn
						isSdaveFile = commons.docW3cToFile(doc, pathDir, fileNameXML);
						if (!isSdaveFile) {
							throw new Exception("Lưu dữ liệu không thành công.");
						}
						/* END - TAO XML HOA DON */
						// END XML"_id", objectIdEInvoice
						// lookup data
						MTDiep = SystemParams.MSTTCGP
								+ commons.csRandomAlphaNumbericString(46 - SystemParams.MSTTCGP.length()).toUpperCase();
						docUpsert = new Document("_id", objectIdEInvoice).append("IssuerId", header.getIssuerId())
								.append("MTDiep", MTDiep)
//            					.append("EInvoiceNumber", null)				//PHAT SINH KHI THUC HIEN KY
								.append("EInvoiceDetail", new Document("TTChung", new Document("THDon", THDon)
										.append("MauSoHD", mauSoHdon)
										.append("KHMSHDon",
												docTmp.getEmbedded(Arrays.asList("DMMauSoKyHieu", "KHMSHDon"), ""))
										.append("KHHDon",
												docTmp.getEmbedded(Arrays.asList("DMMauSoKyHieu", "KHHDon"), ""))
										.append("NLap", NLap).append("DVTTe", DVTTe).append("TGia", TGia)
										.append("HTTToanCode", "").append("HTTToan", ""))
										.append("NDHDon",
												new Document("NBan",
														new Document("Ten", docTmp.get("Name", ""))
																.append("MST", docTmp.get("TaxCode", ""))
																.append("HDKTSo", HDKTSo)
																.append("HDKTNgay",
																		commons.convertStringToLocalDate(HDKTNgay,
																				Constants.FORMAT_DATE.FORMAT_DATE_WEB))
																.append("DChi", XTKho)
																.append("SDThoai", docTmp.get("Phone", ""))
																.append("TNVChuyen", HVTNVChuyen)
																.append("PTVChuyen", PTVChuyen)
																.append("TNDDien", TNDDien)
																.append("DCTDTu", docTmp.get("Email", ""))
																.append("STKNHang",
																		docTmp.getEmbedded(Arrays.asList("BankAccount",
																				"AccountNumber"), ""))
																.append("TNHang",
																		docTmp.getEmbedded(Arrays.asList("BankAccount",
																				"BankName"), ""))
																.append("Fax", docTmp.get("Fax", ""))
																.append("Website", docTmp.get("Website", "")))
														.append("NMua",
																new Document("Ten", TenForm).append("MST", MSTNM)
																		.append("DChi", NTKhoForm)
																		.append("NDXKho", NDNKho).append("HDSo", "")
																		.append("MKHang", "").append("SDThoai", "")
																		.append("DCTDTu", EmailNM)
																		.append("HVTNMHang", HVTNXHang)
																		.append("STKNHang", "").append("TNHang", "")))
										.append("DSHHDVu", listHHDVu).append("TToan",
												new Document("TgTCThue", 0.0).append("TgTThue", 0.0)
														.append("TgTTTBSo", TgTTTBSo).append("TgTTTBChu", TBChu)))
								.append("SignStatusCode", Constants.INVOICE_SIGN_STATUS.NOSIGN)
								.append("EInvoiceStatus", Constants.INVOICE_STATUS.CREATED).append("IsDelete", false)
								.append("SecureKey", SecureKey).append("Dir", pathDir)
								.append("FileNameXML", fileNameXML).append("InfoCreated",
										new Document("CreateDate", LocalDateTime.now())
												.append("CreateUserID", header.getUserId())
												.append("CreateUserName", header.getUserName())
												.append("CreateUserFullName", header.getUserFullName()));
						/* END - LUU DU LIEU HD */

						mongoClient = cfg.mongoClient();
						collection = mongoClient.getDatabase(cfg.dbName).getCollection("EInvoicePXKDL");
						collection.insertOne(docUpsert);
						mongoClient.close();

						dshhdVuList.clear();
						listHHDVu.clear();
						checkMaHD = false;
					} else {
						if (dem == 1) {
							int k = i;
							DSHHDVu dshhdVu = new DSHHDVu();
							dshhdVu.setSTT(eInvoicePXKDLExcelFormList.get(k).getSTT());
							dshhdVu.setProductName(eInvoicePXKDLExcelFormList.get(k).getTHHoa());
							dshhdVu.setProductCode(eInvoicePXKDLExcelFormList.get(k).getMaHHoa());
							dshhdVu.setUnit(eInvoicePXKDLExcelFormList.get(k).getDVTinh());
							dshhdVu.setQuantity(eInvoicePXKDLExcelFormList.get(k).getTXuat());
							dshhdVu.setPrice(eInvoicePXKDLExcelFormList.get(k).getDGia());
							dshhdVu.setTTien(eInvoicePXKDLExcelFormList.get(k).getTTien());
							dshhdVu.setTotal(eInvoicePXKDLExcelFormList.get(k).getTgTien());
							String TinhChat = eInvoicePXKDLExcelFormList.get(k).getLHHoa();
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
							hItem1.put("Unit", dshhdVu.getUnit());
							hItem1.put("Quantity", dshhdVu.getQuantity());
							hItem1.put("Price", dshhdVu.getPrice());
							hItem1.put("Amount", dshhdVu.getTTien());
							hItem1.put("Total", dshhdVu.getTotal());
							hItem1.put("Feature", dshhdVu.getFeature());
							listHHDVus.add(hItem1);

							// Thông tin hóa đơn - TTChung
//							String MaHD = eInvoicePXKDLExcelFormList.get(i).getMaHD();
							String THDon = "PHIẾU XUẤT KHO HÀNG GỬI BÁN ĐẠI LÝ ĐIỆN TỬ";
//                            String MauSoHD = docTmp.getEmbedded(Arrays.asList("DMMauSoKyHieu", "_id"), ObjectId.class).toString();          
							LocalDateTime NLap = LocalDateTime.now();
							String DVTTe = "VND";
							String TGia = "1";

							// Thông tin người bán - Thông tin người mua - NDHDon
							String HDKTSo = HDKTSoForm;
							String HDKTNgay = HDKTNgayForm;
							String HVTNVChuyen = HVTNVChuyenNMForm;
							String HVTNXHang = tempHVTNXHangForm;
							String HDSo = tempHDSoForm;
							String NLap1 = NLapForm;
							String PTVChuyen = PTVChuyenForm;
							String TNDDien = TenNDDienForm;
							String NDNKho = NDNKhoForm;
							String XTKho = XTKhoForm;

							String TenNM = TenForm;
							String DChiNM = NTKhoForm;
							String MSTNM = MSTNhap;
							String DCTDTuNM = DCTDTuNMForm;
							String HDSoCheck = "";
							if (HDSo == null) {
								HDSoCheck = "";
							} else {
								HDSoCheck = HDSo;
							}
							String EmailNM = "";
							if (DCTDTuNM == null) {
								EmailNM = "";
							} else {
								EmailNM = DCTDTuNM;
							}

							// Thông tin thanh toán
//							Double TgTCThue = 0.0;
							Double TgTThue = 0.0;
							Double TgTTTBSo = tempTTienForm;
							String TgTTTBChu = tempTgTTTBChuForm;
							String TBChu = "";
							if (TgTTTBChu == null) {
								TBChu = "Không đồng!";
							} else {
								TBChu = TgTTTBChu;
							}

							// Một số thông tin khác

							String MTDiep = "";
							String SecureKey = "";
							// Setting
							String codeMTD = "0315382923";
							String pathDir = "";
							File file1 = null;
							Path path1 = null;
							ObjectId objectIdEInvoice = null;
							SecureKey = commons.csRandomNumbericString(6);
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

							MTDiep = codeMTD + commons.csRandomAlphaNumbericString(46 - codeMTD.length()).toUpperCase();
							SecureKey = commons.csRandomNumbericString(6);
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

							elementSubTmp = null;
							elementSubTmp01 = null;
							elementSubContent = doc.createElement("TTChung");
							elementTmp = null;

							elementSubContent.appendChild(
									commons.createElementWithValue(doc, "PBan", SystemParams.VERSION_XML_PXK));
							elementSubContent.appendChild(commons.createElementWithValue(doc, "THDon", THDon));
							elementSubContent.appendChild(commons.createElementWithValue(doc, "KHMSHDon",
									docTmp.getEmbedded(Arrays.asList("DMMauSoKyHieu", "KHMSHDon"), "")));
							elementSubContent.appendChild(commons.createElementWithValue(doc, "KHHDon",
									docTmp.getEmbedded(Arrays.asList("DMMauSoKyHieu", "KHHDon"), "")));
							elementSubContent.appendChild(commons.createElementWithValue(doc, "SHDon", "")); // SE PHAT
																												// SINH
																												// KHI
																												// KY
							// Ngày lập
							elementSubContent.appendChild(commons.createElementWithValue(doc, "NLap",
									commons.convertLocalDateTimeStringToString(NLap1,
											Constants.FORMAT_DATE.FORMAT_DATE_WEB,
											Constants.FORMAT_DATE.FORMAT_DATE_EINVOICE, false)));
							// Đơn vị tiền tệ
							elementSubContent.appendChild(commons.createElementWithValue(doc, "DVTTe", DVTTe));
							// Tỷ giá
							elementSubContent.appendChild(commons.createElementWithValue(doc, "TGia", TGia));
							// MST tổ chức cung cấp giải pháp HĐĐT
							elementSubContent
									.appendChild(commons.createElementWithValue(doc, "MSTTCGP", SystemParams.MSTTCGP));

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
							elementTmp.appendChild(commons.createElementWithValue(doc, "Ten", docTmp.get("Name", "")));
							elementTmp
									.appendChild(commons.createElementWithValue(doc, "MST", docTmp.get("TaxCode", "")));
							// lệnh điều động nội bộ
							elementTmp.appendChild(commons.createElementWithValue(doc, "HDKTSo", HDKTSo));
							elementTmp.appendChild(commons.createElementWithValue(doc, "HDKTNgay",
									commons.convertLocalDateTimeStringToString(HDKTNgay,
											Constants.FORMAT_DATE.FORMAT_DATE_WEB,
											Constants.FORMAT_DATE.FORMAT_DATE_EINVOICE, false)));
							elementTmp.appendChild(
									commons.createElementWithValue(doc, "DChi", docTmp.get("Address", "")));
							elementTmp.appendChild(commons.createElementWithValue(doc, "HDSo", ""));
							elementTmp.appendChild(commons.createElementWithValue(doc, "HVTNXHang", HVTNXHang));
							elementTmp.appendChild(commons.createElementWithValue(doc, "TNVChuyen", HVTNVChuyen));
							elementTmp.appendChild(commons.createElementWithValue(doc, "PTVChuyen", PTVChuyen));
//                			elementTmp.appendChild(commons.createElementWithValue(doc, "TTKhac", ""));

							/* ADD THONG TIN TK NGAN HANG (NEU CO) */
							elementSubTmp = doc.createElement("TTKhac");
							elementSubTmp.appendChild(commons.createElementTTKhac(doc, "ComAddress", "string", XTKho));
							elementSubTmp.appendChild(
									commons.createElementTTKhac(doc, "ComPhone", "string", docTmp.get("Phone", "")));
							elementSubTmp.appendChild(
									commons.createElementTTKhac(doc, "ComEmail", "string", docTmp.get("Email", "")));

							if (docTmp.get("BankAccountExt") != null
									&& docTmp.getList("BankAccountExt", Document.class).size() > 0) {
								intTmp = 1;
								for (Document oo : docTmp.getList("BankAccountExt", Document.class)) {
									elementSubTmp.appendChild(commons.createElementTTKhac(doc, "STKNHang" + intTmp,
											"string", oo.get("AccountNumber", "")));
									elementSubTmp.appendChild(commons.createElementTTKhac(doc, "TNHang" + intTmp,
											"string", oo.get("BankName", "")));
									elementTmp.appendChild(elementSubTmp);
									intTmp++;
								}
								elementTmp.appendChild(elementSubTmp);
							}
							elementSubTmp.appendChild(
									commons.createElementTTKhac(doc, "ComFax", "string", docTmp.get("Fax", "")));
							elementSubTmp.appendChild(commons.createElementTTKhac(doc, "PostBy", "string", TNDDien));
							elementSubTmp
									.appendChild(commons.createElementTTKhac(doc, "PostDescription", "string", NDNKho));
							elementTmp.appendChild(elementSubTmp);

							elementSubContent.appendChild(elementTmp);

							elementTmp = doc.createElement("NMua"); // NGUOI MUA
							elementTmp.appendChild(commons.createElementWithValue(doc, "Ten", TenNM));
							elementTmp.appendChild(commons.createElementWithValue(doc, "MST", MSTNM));
							elementTmp.appendChild(commons.createElementWithValue(doc, "DChi", DChiNM));
							elementTmp.appendChild(commons.createElementWithValue(doc, "HVTNMHang", HVTNXHang));

							elementSubTmp = doc.createElement("TTKhac"); // THONG TIN KHAC
							elementSubTmp.appendChild(commons.createElementTTKhac(doc, "MoveNo", "string", HDKTSo));
							elementSubTmp.appendChild(commons.createElementTTKhac(doc, "MoveDate", "string",
									commons.convertLocalDateTimeStringToString(HDKTNgay,
											Constants.FORMAT_DATE.FORMAT_DATE_WEB,
											Constants.FORMAT_DATE.FORMAT_DATE_EINVOICE, false)));
							elementSubTmp.appendChild(
									commons.createElementTTKhac(doc, "TransportDivice", "string", PTVChuyen));
							elementSubTmp.appendChild(
									commons.createElementTTKhac(doc, "TransportName", "string", HVTNVChuyen));
							elementTmp.appendChild(elementSubTmp);
							elementSubContent.appendChild(elementTmp);

							mapVATAmount = new LinkedHashMap<String, Double>();
							mapAmount = new LinkedHashMap<String, Double>();
							elementTmp = doc.createElement("DSHHDVu"); // HH-DV

							for (Object o : listHHDVus) {
								if (!"".equals(o.equals("/ProductName"))) {
									JsonNode h = Json.serializer().nodeFromObject(o);
									tmp = commons.getTextJsonNode(h.at("/VATRate")).replaceAll(",", "");
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
											|| "3".equals(commons.getTextJsonNode(h.at("/Feature")))) {
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
									elementSubTmp.appendChild(commons.createElementWithValue(doc, "SLuong",
											commons.getTextJsonNode(h.at("/Quantity")).replaceAll(",", "")));
									elementSubTmp.appendChild(commons.createElementWithValue(doc, "DGia",
											commons.getTextJsonNode(h.at("/Price")).replaceAll(",", "")));
									elementSubTmp.appendChild(commons.createElementWithValue(doc, "ThTien",
											commons.getTextJsonNode(h.at("/Total")).replaceAll(",", "")));
									elementSubTmp.appendChild(commons.createElementWithValue(doc, "TSuat", "0%"));

									elementSubTmp01 = doc.createElement("TTKhac");
									elementSubTmp01.appendChild(commons.createElementTTKhac(doc, "SLXuat", "String",
											commons.getTextJsonNode(h.at("/Total")).replaceAll(",", "")));
									elementSubTmp01.appendChild(commons.createElementTTKhac(doc, "VATAmount", "decimal",
											commons.getTextJsonNode(h.at("/VATAmount")).replaceAll(",", "")));
									elementSubTmp01.appendChild(commons.createElementTTKhac(doc, "Amount", "decimal",
											commons.getTextJsonNode(h.at("/Amount")).replaceAll(",", "")));
									elementSubTmp.appendChild(elementSubTmp01);

									elementTmp.appendChild(elementSubTmp);

								}
							}
							elementSubContent.appendChild(elementTmp);
							elementTmp = doc.createElement("TToan"); // Thong tin thanh toan
							elementSubTmp = doc.createElement("THTTLTSuat");
							/* DANH SACH CAC LOAI THUE SUAT */

//                			https://stackoverflow.com/questions/46898/how-do-i-efficiently-iterate-over-each-entry-in-a-java-map

							elementSubTmp01 = doc.createElement("LTSuat");
							elementSubTmp01.appendChild(commons.createElementWithValue(doc, "TSuat", "KCT"));
							elementSubTmp01.appendChild(commons.createElementWithValue(doc, "ThTien",
									commons.formatNumberReal(TgTTTBSo).replaceAll(",", "")));
							elementSubTmp01.appendChild(commons.createElementWithValue(doc, "TThue",
									commons.formatNumberReal(TgTThue).replaceAll(",", "")));
							elementSubTmp.appendChild(elementSubTmp01);

							elementTmp.appendChild(elementSubTmp);

							elementTmp.appendChild(commons.createElementWithValue(doc, "TgTCThue", "0"));
							elementTmp.appendChild(commons.createElementWithValue(doc, "TgTThue", "0"));
							elementTmp.appendChild(commons.createElementWithValue(doc, "TTCKTMai", "0"));
							elementTmp.appendChild(commons.createElementWithValue(doc, "TgTTTBSo",
									commons.formatNumberReal(TgTTTBSo).replaceAll(",", "")));
							elementTmp.appendChild(commons.createElementWithValue(doc, "TgTTTBChu", TBChu));

							elementContent.appendChild(elementSubContent);
							elementSubContent.appendChild(elementTmp);

							// END - NDHDon: Nội dung hóa đơn
							isSdaveFile = commons.docW3cToFile(doc, pathDir, fileNameXML);
							if (!isSdaveFile) {
								throw new Exception("Lưu dữ liệu không thành công.");
							}
							/* END - TAO XML HOA DON */
							// END XML"_id", objectIdEInvoice
							// lookup data
							MTDiep = SystemParams.MSTTCGP + commons
									.csRandomAlphaNumbericString(46 - SystemParams.MSTTCGP.length()).toUpperCase();
							docUpsert = new Document("_id", objectIdEInvoice).append("IssuerId", header.getIssuerId())
									.append("MTDiep", MTDiep)
//                					.append("EInvoiceNumber", null)				//PHAT SINH KHI THUC HIEN KY
									.append("EInvoiceDetail", new Document("TTChung", new Document("THDon", THDon)
											.append("MauSoHD", mauSoHdon)
											.append("KHMSHDon",
													docTmp.getEmbedded(Arrays.asList("DMMauSoKyHieu", "KHMSHDon"), ""))
											.append("KHHDon",
													docTmp.getEmbedded(Arrays.asList("DMMauSoKyHieu", "KHHDon"), ""))
											.append("NLap", NLap).append("DVTTe", DVTTe).append("TGia", TGia)
											.append("HTTToanCode", "").append("HTTToan", ""))
											.append("NDHDon",
													new Document("NBan", new Document("Ten", docTmp.get("Name", ""))
															.append("MST", docTmp.get("TaxCode", ""))
															.append("HDKTSo", HDKTSo)
															.append("HDKTNgay",
																	commons.convertStringToLocalDate(HDKTNgay,
																			Constants.FORMAT_DATE.FORMAT_DATE_WEB))
															.append("DChi", XTKho)
															.append("SDThoai", docTmp.get("Phone", ""))
															.append("TNVChuyen", HVTNVChuyen)
															.append("PTVChuyen", PTVChuyen).append("TNDDien", TNDDien)
															.append("DCTDTu", docTmp.get("Email", ""))
															.append("STKNHang",
																	docTmp.getEmbedded(Arrays.asList("BankAccount",
																			"AccountNumber"), ""))
															.append("TNHang", docTmp.getEmbedded(
																	Arrays.asList("BankAccount", "BankName"), ""))
															.append("Fax", docTmp.get("Fax", ""))
															.append("Website", docTmp.get("Website", "")))
															.append("NMua", new Document("Ten", TenNM)
																	.append("MST", MSTNM).append("DChi", NTKhoForm)
																	.append("NDXKho", NDNKho).append("HDSo", HDSoCheck)
																	.append("MKHang", "").append("SDThoai", "")
																	.append("DCTDTu", EmailNM)
																	.append("HVTNMHang", HVTNXHang)
																	.append("STKNHang", "").append("TNHang", "")))
											.append("DSHHDVu", listHHDVus).append("TToan",
													new Document("TgTCThue", 0.0).append("TgTThue", 0.0)
															.append("TgTTTBSo", TgTTTBSo).append("TgTTTBChu", TBChu)))
									.append("SignStatusCode", Constants.INVOICE_SIGN_STATUS.NOSIGN)
									.append("EInvoiceStatus", Constants.INVOICE_STATUS.CREATED)
									.append("IsDelete", false).append("SecureKey", SecureKey).append("Dir", pathDir)
									.append("FileNameXML", fileNameXML).append("InfoCreated",
											new Document("CreateDate", LocalDateTime.now())
													.append("CreateUserID", header.getUserId())
													.append("CreateUserName", header.getUserName())
													.append("CreateUserFullName", header.getUserFullName()));
							/* END - LUU DU LIEU HD */

							mongoClient = cfg.mongoClient();
							collection = mongoClient.getDatabase(cfg.dbName).getCollection("EInvoicePXKDL");
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

	private static EInvoicePXKDLExcelForm extractInfoFromCell(List<Cell> cells) {
		EInvoicePXKDLExcelForm eInvoicePXKDLExcelForm = new EInvoicePXKDLExcelForm();
		// Ma hoa don
		Cell MaHD = cells.get(0);
		if (MaHD != null) {
			switch (MaHD.getCellType()) {
			case STRING:
				eInvoicePXKDLExcelForm.setMaHD(MaHD.getStringCellValue());
				break;
			case NUMERIC:
				eInvoicePXKDLExcelForm.setMaHD((NumberToTextConverter.toText(MaHD.getNumericCellValue())));
				break;
			case BLANK:
				break;
			default:
				break;
			}
		}
		// NGAY LAP
		Cell NHDon = cells.get(1);
		if (NHDon != null) {
			switch (NHDon.getCellType()) {
			case STRING:
				eInvoicePXKDLExcelForm.setNHDon(NHDon.getStringCellValue());
				break;
			case NUMERIC:
				eInvoicePXKDLExcelForm.setNHDon((NumberToTextConverter.toText(NHDon.getNumericCellValue())));
				break;
			case BLANK:
				break;
			default:
				break;
			}
		}
		// NGUOI DIEU DONG

		Cell Cua = cells.get(2);
		if (Cua != null) {
			switch (Cua.getCellType()) {
			case STRING:
				eInvoicePXKDLExcelForm.setCua(Cua.getStringCellValue());
				break;
			case NUMERIC:
				eInvoicePXKDLExcelForm.setCua((NumberToTextConverter.toText(Cua.getNumericCellValue())));
				break;
			case BLANK:
				break;
			default:
				break;
			}
		}
		// VE VIEC
		Cell VViec = cells.get(3);
		if (VViec != null) {
			switch (VViec.getCellType()) {
			case STRING:
				eInvoicePXKDLExcelForm.setVViec(VViec.getStringCellValue());
				break;
			case NUMERIC:
				eInvoicePXKDLExcelForm.setVViec((NumberToTextConverter.toText(VViec.getNumericCellValue())));
				break;
			case BLANK:
				break;
			default:
				break;
			}

		}

		// HDKT SO
		Cell HDKTSo = cells.get(4);
		if (HDKTSo != null) {
			switch (HDKTSo.getCellType()) {
			case STRING:
				eInvoicePXKDLExcelForm.setHDKTSo(HDKTSo.getStringCellValue());
				break;
			case NUMERIC:
				eInvoicePXKDLExcelForm.setHDKTSo((NumberToTextConverter.toText(HDKTSo.getNumericCellValue())));
				break;
			case BLANK:
				break;
			default:
				break;
			}
		}

		// HDKT NGAY

		Cell HDKTNgay = cells.get(5);
		if (HDKTNgay != null) {
			switch (HDKTNgay.getCellType()) {
			case STRING:
				eInvoicePXKDLExcelForm.setHDKTNgay(HDKTNgay.getStringCellValue());
				break;
			case NUMERIC:
				eInvoicePXKDLExcelForm.setHDKTNgay((NumberToTextConverter.toText(HDKTNgay.getNumericCellValue())));
				break;
			case BLANK:
				break;
			default:
				break;
			}
		}

		// HO VA TEN NG VC
		Cell HVTNVChuyen = cells.get(6);
		if (HVTNVChuyen != null) {
			switch (HVTNVChuyen.getCellType()) {
			case STRING:
				eInvoicePXKDLExcelForm.setHVTNVChuyen(HVTNVChuyen.getStringCellValue());
				break;
			case NUMERIC:
				eInvoicePXKDLExcelForm
						.setHVTNVChuyen((NumberToTextConverter.toText(HVTNVChuyen.getNumericCellValue())));
				break;
			case BLANK:
				break;
			default:
				break;
			}

		}
		// PHUONG TIEN VC
		Cell PTVChuyen = cells.get(7);
		if (PTVChuyen != null) {
			switch (PTVChuyen.getCellType()) {
			case STRING:
				eInvoicePXKDLExcelForm.setPTVChuyen(PTVChuyen.getStringCellValue());
				break;
			case NUMERIC:
				eInvoicePXKDLExcelForm.setPTVChuyen((NumberToTextConverter.toText(PTVChuyen.getNumericCellValue())));
				break;
			case BLANK:
				break;
			default:
				break;
			}
		}

		// XUAT TAI KHO
		Cell XTKho = cells.get(8);
		if (XTKho != null) {
			switch (XTKho.getCellType()) {
			case STRING:
				eInvoicePXKDLExcelForm.setXTKho(XTKho.getStringCellValue());
				break;
			case NUMERIC:
				eInvoicePXKDLExcelForm.setXTKho((NumberToTextConverter.toText(XTKho.getNumericCellValue())));
				break;
			case BLANK:
				break;
			default:
				break;
			}
		}
		// NTkho
		Cell NTkho = cells.get(9);
		if (NTkho != null) {
			switch (NTkho.getCellType()) {
			case STRING:
				eInvoicePXKDLExcelForm.setNTkho(NTkho.getStringCellValue());
				break;
			case NUMERIC:
				eInvoicePXKDLExcelForm.setNTkho((NumberToTextConverter.toText(NTkho.getNumericCellValue())));
				break;
			case BLANK:
				break;
			default:
				break;
			}

		}

		// MST NG NHAP
		Cell MSTNNHap = cells.get(10);
		if (MSTNNHap != null) {
			switch (MSTNNHap.getCellType()) {
			case STRING:
				eInvoicePXKDLExcelForm.setMSTNNhap(MSTNNHap.getStringCellValue());
				break;
			case NUMERIC:
				eInvoicePXKDLExcelForm.setMSTNNhap((NumberToTextConverter.toText(MSTNNHap.getNumericCellValue())));
				break;
			case BLANK:
				break;
			default:
				break;
			}

		}
		// HVTNNHAP
		Cell HVTNNHang = cells.get(11);
		if (HVTNNHang != null) {
			switch (HVTNNHang.getCellType()) {
			case STRING:
				eInvoicePXKDLExcelForm.setHVTNNHang(HVTNNHang.getStringCellValue());
				break;
			case NUMERIC:
				eInvoicePXKDLExcelForm.setHVTNNHang((NumberToTextConverter.toText(HVTNNHang.getNumericCellValue())));
				break;
			case BLANK:
				break;
			default:
				break;
			}

		}
		// TEN DON VI
		Cell TenDV = cells.get(12);
		if (TenDV != null) {
			switch (TenDV.getCellType()) {
			case STRING:
				eInvoicePXKDLExcelForm.setTenDV(TenDV.getStringCellValue());
				break;
			case NUMERIC:
				eInvoicePXKDLExcelForm.setTenDV((NumberToTextConverter.toText(TenDV.getNumericCellValue())));
				break;
			case BLANK:
				break;
			default:
				break;
			}

		}
		// MAIL NHAN
		Cell EmailKNhan = cells.get(13);
		if (EmailKNhan != null) {
			switch (EmailKNhan.getCellType()) {
			case STRING:
				eInvoicePXKDLExcelForm.setEmailKNhan(EmailKNhan.getStringCellValue());
				break;
			case NUMERIC:
				eInvoicePXKDLExcelForm.setEmailKNhan((NumberToTextConverter.toText(EmailKNhan.getNumericCellValue())));
				break;
			case BLANK:
				break;
			default:
				break;
			}
		}

		// STT
		Cell STT = cells.get(14);
		if (STT != null) {
			switch (STT.getCellType()) {
			case STRING:
				eInvoicePXKDLExcelForm.setSTT(STT.getStringCellValue());
				break;
			case NUMERIC:
				eInvoicePXKDLExcelForm.setSTT((NumberToTextConverter.toText(STT.getNumericCellValue())));
				break;
			case BLANK:
				break;
			default:
				break;
			}
		}
		// TEN HH
		Cell THHoa = cells.get(15);
		if (THHoa != null) {
			switch (THHoa.getCellType()) {
			case STRING:
				eInvoicePXKDLExcelForm.setTHHoa(THHoa.getStringCellValue());
				break;
			case NUMERIC:
				break;
			case BLANK:
				break;
			default:
				break;
			}
		}
		// Don gia
		Cell MaHHoa = cells.get(16);
		if (MaHHoa != null) {
			switch (MaHHoa.getCellType()) {
			case STRING:
				eInvoicePXKDLExcelForm.setMaHHoa(MaHHoa.getStringCellValue());
				break;
			case NUMERIC:
				eInvoicePXKDLExcelForm.setMaHHoa((NumberToTextConverter.toText(MaHHoa.getNumericCellValue())));
				break;
			case BLANK:
				break;
			default:
				break;
			}
		}
		// Don vi tinh
		Cell DVTinh = cells.get(17);
		if (DVTinh != null) {
			switch (DVTinh.getCellType()) {
			case STRING:
				eInvoicePXKDLExcelForm.setDVTinh(DVTinh.getStringCellValue());
				break;
			case NUMERIC:
				eInvoicePXKDLExcelForm.setDVTinh((NumberToTextConverter.toText(DVTinh.getNumericCellValue())));
				break;
			case BLANK:
				break;
			default:
				break;
			}
		}

		// Thanh tien
		Cell TXuat = cells.get(18);
		if (TXuat != null) {
			switch (TXuat.getCellType()) {
			case STRING:
				eInvoicePXKDLExcelForm.setTXuat((Double.valueOf((String) TXuat.getStringCellValue())));
				break;
			case NUMERIC:
				eInvoicePXKDLExcelForm.setTXuat(TXuat.getNumericCellValue());
				break;
			case BLANK:
				break;
			default:
				break;
			}
		}

		// Thue suat
		Cell TNhap = cells.get(19);
		if (TNhap != null) {
			switch (TNhap.getCellType()) {
			case STRING:
				eInvoicePXKDLExcelForm.setTNhap((Double.valueOf((String) TNhap.getStringCellValue())));
				break;
			case NUMERIC:
				eInvoicePXKDLExcelForm.setTNhap(TNhap.getNumericCellValue());
				break;
			case BLANK:
				break;
			default:
				break;
			}
		}

		// Don gia
		Cell DGia = cells.get(20);
		if (DGia != null) {
			switch (DGia.getCellType()) {
			case STRING:
				eInvoicePXKDLExcelForm.setDGia((Double.valueOf((String) DGia.getStringCellValue())));
				break;
			case NUMERIC:
				eInvoicePXKDLExcelForm.setDGia(DGia.getNumericCellValue());
				break;
			case BLANK:
				break;
			default:
				break;
			}
		}
		// Thanh tien
		Cell TTien = cells.get(21);
		if (TTien != null && (TTien.getCellType() == CellType.FORMULA)) {
			switch (TTien.getCachedFormulaResultType()) {
			case STRING:
				eInvoicePXKDLExcelForm.setTTien((Double.valueOf((String) TTien.getStringCellValue())));
				break;
			case NUMERIC:
				eInvoicePXKDLExcelForm.setTTien(TTien.getNumericCellValue());
				break;
			case BLANK:
				break;
			default:
				break;
			}
		}
		if (TTien != null) {
			switch (TTien.getCellType()) {
			case STRING:
				eInvoicePXKDLExcelForm.setTTien((Double.valueOf((String) TTien.getStringCellValue())));
				break;
			case NUMERIC:
				eInvoicePXKDLExcelForm.setTTien(TTien.getNumericCellValue());
				break;
			case BLANK:
				break;
			default:
				break;
			}
		}
		Cell TgTien = cells.get(22);
		if (TgTien != null && (TgTien.getCellType() == CellType.FORMULA)) {
			switch (TgTien.getCachedFormulaResultType()) {
			case STRING:
				eInvoicePXKDLExcelForm.setTgTien((Double.valueOf((String) TgTien.getStringCellValue())));
				break;
			case NUMERIC:
				eInvoicePXKDLExcelForm.setTgTien(TgTien.getNumericCellValue());
				break;
			case BLANK:
				break;
			default:
				break;
			}
		}
		if (TgTien != null) {
			switch (TgTien.getCellType()) {
			case STRING:
				eInvoicePXKDLExcelForm.setTgTien((Double.valueOf((String) TgTien.getStringCellValue())));
				break;
			case NUMERIC:
				eInvoicePXKDLExcelForm.setTgTien(TgTien.getNumericCellValue());
				break;
			case BLANK:
				break;
			default:
				break;
			}
		}
		// Ten hang hoa
		Cell LHHoa = cells.get(23);
		if (LHHoa != null) {
			switch (LHHoa.getCellType()) {
			case STRING:
				eInvoicePXKDLExcelForm.setLHHoa(LHHoa.getStringCellValue());
				break;
			case NUMERIC:
				eInvoicePXKDLExcelForm.setLHHoa((NumberToTextConverter.toText(LHHoa.getNumericCellValue())));
				break;
			case BLANK:
				break;
			default:
				break;
			}
		}
		// Tổng tiền ghi bằng chữ
		Cell TBchu = cells.get(24);
		if (TBchu != null) {
			switch (TBchu.getCellType()) {
			case STRING:
				eInvoicePXKDLExcelForm.setTBchu(TBchu.getStringCellValue());
				break;
			case NUMERIC:
				eInvoicePXKDLExcelForm.setTBchu((NumberToTextConverter.toText(TBchu.getNumericCellValue())));
				break;
			case BLANK:
				break;
			default:
				break;
			}
		}
		// Tra ve danh sach
		return eInvoicePXKDLExcelForm;
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

//		String actionCode = header.getActionCode();
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
	

		Document fillter = new Document("_id", 1)
				.append("Dir", 1)
				.append("IssuerId", 1)
				.append("FileNameXML", 1)
				.append("EInvoiceStatus", 1)
				.append("SignStatusCode", 1)
				.append("MCCQT", 1)
				.append("HDSS", 1)
				.append("MTDiep", 1)
				.append("EInvoiceDetail", 1);
		
		docFind = new Document("IssuerId", header.getIssuerId())
				.append("MCCQT", new Document("$exists", true).append("$ne", null)).append("_id", objectId);
		pipeline = new ArrayList<Document>();
		pipeline.add(new Document("$match", docFind));
		pipeline.add(new Document("$project", fillter));
	
		pipeline.add(new Document("$lookup", new Document("from", "ConfigEmail")
				.append("let", new Document("vIssuerId", "$IssuerId"))
				.append("pipeline",
						Arrays.asList(new Document("$match",
								new Document("$expr", new Document("$eq", Arrays.asList("$IssuerId", "$$vIssuerId"))))))
				.append("as", "ConfigEmail")));
		pipeline.add(new Document("$unwind",
				new Document("path", "$ConfigEmail").append("preserveNullAndEmptyArrays", true)));
		pipeline.add(new Document("$lookup", new Document("from", "DMMauSoKyHieu")
				.append("let",
						new Document("vIssuerId", "$IssuerId").append("vMauSoHD", "$EInvoiceDetail.TTChung.MauSoHD"))
				.append("pipeline",
						Arrays.asList(new Document("$match", new Document("$expr", new Document("$and",
								Arrays.asList(new Document("$eq", Arrays.asList("$$vIssuerId", "$IssuerId")),
										new Document("$eq",
												Arrays.asList(new Document("$toString", "$_id"), "$$vMauSoHD")))))),
								new Document("$project", new Document("_id", 1).append("Templates", 1).append("Status", 1).append("SHDHT", 1).append("SoLuong", 1).append("ConLai", 1))	
								))
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

		pipeline.add(
				new Document("$lookup",
						new Document("from", "DMFooterWeb").append("pipeline",
								Arrays.asList(
										new Document("$match",
												new Document("IsActive", true).append("IsDelete",
														new Document("$ne", true))),
										new Document("$project", new Document("Noidung", 1)),
										new Document("$limit", 1)))
								.append("as", "DMFooterWeb")));
		pipeline.add(new Document("$unwind",
				new Document("path", "$DMFooterWeb").append("preserveNullAndEmptyArrays", true)));
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
		MongoCollection<Document> collection = mongoClient.getDatabase(cfg.dbName).getCollection("EInvoicePXKDL");
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
		boolean isThayThe = "3".equals(docTmp.getEmbedded(Arrays.asList("HDSS", "TCTBao"), ""));
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

				baosPDF = jpUtils.createFinalInvoiceDL(fileJP, doc,secureKey, CheckView, numberRowInPage,
						numberRowInPageMultiPage, numberCharsInRow, MST, link,
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
		// KIỂM TRA GỬI MAIL THƯỜNG HAY MAILJET

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

			if (MailJet.equals("Y") && !MailJet.equals("") && !MailJet.equals("N")) {
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

			try {

				mongoClient = cfg.mongoClient();
				collection = mongoClient.getDatabase(cfg.dbName).getCollection("LogEmailUser");
				collection.insertOne(new Document("IssuerId", header.getIssuerId()).append("Title", _title)
						.append("Email", email_gui).append("IsActive", true).append("MailCheck", boo)
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
				.append("SignStatusCode", Constants.INVOICE_SIGN_STATUS.SIGNED);

		Document docTmp = null;

		MongoClient mongoClient = cfg.mongoClient();
		MongoCollection<Document> collection = mongoClient.getDatabase(cfg.dbName).getCollection("EInvoicePXKDL");
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
//				rTCTN = commons.stringToDocument("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?><KetQuaTraCuu><MaGD>D8DB05DDFFD8425AB665558FEF499EC7</MaGD><MaKetQua>0</MaKetQua><MoTaKetQua>CQT đã trả kết quả xử lý thông điệp</MoTaKetQua><DuLieu><TDiep><TTChung><PBan>2.0.0</PBan><MNGui>TCT</MNGui><MNNhan>V0401486901</MNNhan><MLTDiep>999</MLTDiep><MTDiep>TCT97B381D4211B4DC8ADE43A27BA72048A</MTDiep><MTDTChieu>V0401486901D8DB05DDFFD8425AB665558FEF499EC7</MTDTChieu></TTChung><DLieu><TBao><MTDiep>V0401486901D8DB05DDFFD8425AB665558FEF499EC7</MTDiep><MNGui>V0401486901</MNGui><NNhan>2022-03-22T11:50:45</NNhan><TTTNhan>0</TTTNhan></TBao></DLieu></TDiep><TDiep><TTChung><PBan>2.0.0</PBan><MNGui>TCT</MNGui><MNNhan>V0401486901</MNNhan><MLTDiep>204</MLTDiep><MTDiep>TCT193127A3E1A64029803F59BB157A4EF6</MTDiep><MTDTChieu>V0401486901D8DB05DDFFD8425AB665558FEF499EC7</MTDTChieu><MST>0301521415</MST><SLuong>1</SLuong></TTChung><DLieu><TBao><DLTBao Id=\"Id-c17ac0768edb437b9d76c965c30b3b16\"><MSo>01/TB-KTDL</MSo><Ten>Thông báo về việc kết quả kiểm tra dữ liệu hóa đơn điện tử</Ten><So>220003861409</So><DDanh>Hà Nội</DDanh><NTBao>2022-03-22</NTBao><MST>0301521415</MST><TNNT>CÔNG TY TNHH KIM LOẠI HẠNH PHÚC</TNNT><TGGui>2022-03-22T11:50:46</TGGui><LTBao>2</LTBao><CCu>Thông điệp thông báo hủy/giải trình HDDT có mã/không mã đã lập có sai sót</CCu><SLuong>1</SLuong></DLTBao><DSCKS><CQT><Signature xmlns=\"http://www.w3.org/2000/09/xmldsig#\" Id=\"Tct-71b09cd7304f4ab8ab0a74b7a164f88c\"><SignedInfo><CanonicalizationMethod Algorithm=\"http://www.w3.org/TR/2001/REC-xml-c14n-20010315\" /><SignatureMethod Algorithm=\"http://www.w3.org/2001/04/xmldsig-more#rsa-sha256\" /><Reference URI=\"#Id-c17ac0768edb437b9d76c965c30b3b16\"><DigestMethod Algorithm=\"http://www.w3.org/2001/04/xmlenc#sha256\" /><DigestValue>hpzYcBVfhJipwQN4Z++5Ya/e+0BBumAArubh0dCrSxg=</DigestValue></Reference><Reference URI=\"#SigningTime-a345d862b9c94599bfdc5c5b2bea2be1\"><DigestMethod Algorithm=\"http://www.w3.org/2001/04/xmlenc#sha256\" /><DigestValue>vVGGtDB8sQhnyxL+mnuKMA94/KqiEmVRnX/e4hE0qMI=</DigestValue></Reference></SignedInfo><SignatureValue>domvFsaa5TZdLcm9P881/2lzgviI0Pc+ZXn9kanO6CKmTCR7gwxc00XLLMw0nHG/5jJMkeW+fipNzPIzl4/YbH+XPsNc+E7OK6uJYufPLZ2NfzHKtu1otivsLbHvGVL7+feHA7tAn6eVvjCiv7YVfvyX9cE+WI0s6qVyJHDBZ7Mg3HM19H3y5VpNF4DGFD/kdLQpUXdW78UO/8ulV3yGdGV9EYckqFlptdFrbRSzlxrW5JSyk9rZ7ginJWSp5n65l6UQvu2bYKU331rcRptcZBXBu9h/1Dz7o66nU+IQn8rlkr4FFOM+NJQSxds7lIuEkYuD83q+X1k5/cu9QmRG7A==</SignatureValue><KeyInfo><X509Data><X509SubjectName>CN=TỔNG CỤC THUẾ,O=BỘ TÀI CHÍNH,L=Hà Nội,C=VN</X509SubjectName><X509Certificate>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</X509Certificate></X509Data></KeyInfo><Object Id=\"SigningTime-a345d862b9c94599bfdc5c5b2bea2be1\"><SignatureProperties><SignatureProperty Target=\"signatureProperties\"><SigningTime>2022-03-22T11:50:46</SigningTime></SignatureProperty></SignatureProperties></Object></Signature></CQT></DSCKS></TBao></DLieu></TDiep><TDiep><TTChung><PBan>2.0.0</PBan><MNGui>TCT</MNGui><MNNhan>V0401486901</MNNhan><MLTDiep>301</MLTDiep><MTDiep>TCTA2AAFCCCBD8B4292A7FEA4A1E66E595B</MTDiep><MTDTChieu>V0401486901D8DB05DDFFD8425AB665558FEF499EC7</MTDTChieu><MST>0301521415</MST><SLuong>1</SLuong></TTChung><DLieu><TBao><DLTBao Id=\"Id-2da3b60292a14702aec6cb5b276e7768\"><PBan>2.0.0</PBan><MSo>01/TB-SSĐT</MSo><Ten>Về việc tiếp nhận và kết quả xử lý về việc hóa đơn điện tử đã lập có sai sót</Ten><TCQTCTren>Cục Thuế Thành phố Hồ Chí Minh</TCQTCTren><TCQT>Chi cục Thuế Quận Gò Vấp</TCQT><TNNT>CÔNG TY TNHH KIM LOẠI HẠNH PHÚC</TNNT><MST>0316685293</MST><MGDDTu>V0401486901D8DB05DDFFD8425AB665558FEF499EC7</MGDDTu><TGNhan>2022-03-22</TGNhan><STTThe>1</STTThe><HThuc>KT.Chi cục Trưởng</HThuc><CDanh>Phó Chi cục Trưởng</CDanh></DLTBao><STBao Id=\"Id-6813e99ffeb542b1ae9c2d8bbfab068f\"><So>13657/TB-CCTGV-HDDT</So><NTBao>2022-03-25</NTBao></STBao><DSCKS><TTCQT><Signature xmlns=\"http://www.w3.org/2000/09/xmldsig#\" Id=\"TTCQT-Id-2da3b60292a14702aec6cb5b276e7768\"><SignedInfo><CanonicalizationMethod Algorithm=\"http://www.w3.org/TR/2001/REC-xml-c14n-20010315\" /><SignatureMethod Algorithm=\"http://www.w3.org/2001/04/xmldsig-more#rsa-sha256\" /><Reference URI=\"#Object-TTCQT-Id-2da3b60292a14702aec6cb5b276e7768\"><DigestMethod Algorithm=\"http://www.w3.org/2001/04/xmlenc#sha256\" /><DigestValue>N0oGHJnkn80vY+I0R6ilvQbgTHWAav1GK+v20807TyE=</DigestValue></Reference><Reference URI=\"#Id-2da3b60292a14702aec6cb5b276e7768\"><DigestMethod Algorithm=\"http://www.w3.org/2001/04/xmlenc#sha256\" /><DigestValue>fda5zcz1NSe/4c8Izv/xn9fU4hsQuVTRal+cNlsd0Zs=</DigestValue></Reference></SignedInfo><SignatureValue>UmpUOJ++XnOED7BtZqg0G6h6KpyVyQzHpnsMxntxmawwDPw5a3C5ydcV+wEj8TdEYd15t8fS1p8DNvQZfoqrziEPpbNlEXN+LuWuU0KDl5U1YjzILSOsVIanMJihAByjUzH/awXh1ycUeOMIF1jb8Rypl6mrUN+P/UkwwLM0BjOQWxZ3MuYJPDe7PBUSZqFrSoDFE6l3JOznVNu+M0eTqf2Rb2ySgg+SpSyR1l5PpSrOS1/icgRDStU8W0MWB6oV4+GpPLGfQJdNndm/yYUAyLgm+QpywzUNLEAjhbf1J5TfH+RjBaYufOJNGZcgoTw02rCJNRWZMZiy9fBWvzLGow==</SignatureValue><KeyInfo><X509Data><X509SubjectName>CN=Phó Chi cục trưởng Nguyễn Mạnh Trung, L=Hồ Chí Minh, OU=Cục Thuế thành phố Hồ Chí Minh, OU=Tổng cục Thuế, O=Bộ Tài chính, C=VN</X509SubjectName><X509Certificate>MIIGDDCCBPSgAwIBAgIDa1TcMA0GCSqGSIb3DQEBCwUAMFkxCzAJBgNVBAYTAlZOMR0wGwYDVQQKDBRCYW4gQ28geWV1IENoaW5oIHBodTErMCkGA1UEAwwiQ28gcXVhbiBjaHVuZyB0aHVjIHNvIEJvIFRhaSBjaGluaDAeFw0xOTA0MDUwNzU2MTZaFw0yNjA1MTgwNzU2MTZaMIHLMQswCQYDVQQGEwJWTjEZMBcGA1UECgwQQuG7mSBUw6BpIGNow61uaDEcMBoGA1UECwwTVOG7lW5nIGPhu6VjIFRodeG6vzExMC8GA1UECwwoQ+G7pWMgVGh14bq/IHRow6BuaCBwaOG7kSBI4buTIENow60gTWluaDEXMBUGA1UEBwwOSOG7kyBDaMOtIE1pbmgxNzA1BgNVBAMMLlBow7MgQ2hpIGPhu6VjIHRyxrDhu59uZyBOZ3V54buFbiBN4bqhbmggVHJ1bmcwggEiMA0GCSqGSIb3DQEBAQUAA4IBDwAwggEKAoIBAQCya64WBOSa9oXRXEOVNf2VB8w5GCCTtAxcBgT8dORYgOQ9AfEZ2GRuImnRr3Wu0FGqhVlKE5b5h317+JAzje0wYqp1n4UnrQmQYrwTH2dOklQmDHhFYD7k+4cS6XHzqpLKMBRzZi6nG6PbFFFvztVAve3PCQ3F7DECNKoh5PDmaiRqaONskUfLkllo0erfYHTZ1BPvpDfBgcnIhJVUvdO1Rjh2gjjtjCTg4hLaFWy8JJl7M6z3IMOIMYi2xfT1urBVwqmURysmLwokT266Hk+9p1wZhSmQpv2n3QPqeH8Qu9bgVJhKBkF9N90A+pWS+ZZU2sVE5s6nuJszCjhjDEEHAgMBAAGjggJoMIICZDAJBgNVHRMEAjAAMBEGCWCGSAGG+EIBAQQEAwIFoDALBgNVHQ8EBAMCBPAwKQYDVR0lBCIwIAYIKwYBBQUHAwIGCCsGAQUFBwMEBgorBgEEAYI3FAICMB8GCWCGSAGG+EIBDQQSFhBVc2VyIFNpZ24gb2YgQlRDMB0GA1UdDgQWBBQeHp3ua90KvqdhF+NZlE++It9lezCBlQYDVR0jBIGNMIGKgBSeOJrWKZWJagV/Kv9fAZe0VzBmsqFvpG0wazELMAkGA1UEBhMCVk4xHTAbBgNVBAoMFEJhbiBDbyB5ZXUgQ2hpbmggcGh1MT0wOwYDVQQDDDRDbyBxdWFuIGNodW5nIHRodWMgc28gY2h1eWVuIGR1bmcgQ2hpbmggcGh1IChSb290Q0EpggEDMCEGA1UdEQQaMBiBFm5tdHJ1bmcuaGNtQGdkdC5nb3Yudm4wCQYDVR0SBAIwADBlBggrBgEFBQcBAQRZMFcwIgYIKwYBBQUHMAGGFmh0dHA6Ly9vY3NwLmNhLmdvdi52bi8wMQYIKwYBBQUHMAKGJWh0dHA6Ly9jYS5nb3Yudm4vcGtpL3B1Yi9jZXJ0L2J0Yy5jcnQwMwYJYIZIAYb4QgEEBCYWJGh0dHA6Ly9jYS5nb3Yudm4vcGtpL3B1Yi9jcmwvYnRjLmNybDAzBglghkgBhvhCAQMEJhYkaHR0cDovL2NhLmdvdi52bi9wa2kvcHViL2NybC9idGMuY3JsMDUGA1UdHwQuMCwwKqAooCaGJGh0dHA6Ly9jYS5nb3Yudm4vcGtpL3B1Yi9jcmwvYnRjLmNybDANBgkqhkiG9w0BAQsFAAOCAQEAD/PW3vbqzBTBdkV5e5BkQHA+v+ZYOxuGgVXyrahI4Q95z2vMWk867qjnqllkw9iM3HqwIgcxdCuMYgTABQXfqCri15dIYAYTalV3DFI4au0qEtLMEkWN9wkB/JQMITyHksvKDaR8JefCi+SQrIAmdoYp208Q0MRzwE167A/p8r7ifTdh7IUazwch1hH8sjf76fdFg+joO9wccd1DHW62OKhhGJvMwHvbWWsR3CYcH0A8CZOQ6WE/6IANrlld7mhhLCEQ4z9pZHIjkGL0AUQKQWJ6nA6sg8hruokKluI6KzSS9sNw1fsRiD9VCYhu36OSQcZatj+z8ox/k6NmTI6KAA==</X509Certificate></X509Data></KeyInfo><Object Id=\"Object-TTCQT-Id-2da3b60292a14702aec6cb5b276e7768\"><SignatureProperties xmlns=\"\"><SignatureProperty Id=\"SignatureProperty-TTCQT-Id-2da3b60292a14702aec6cb5b276e7768\" Target=\"#TTCQT-Id-2da3b60292a14702aec6cb5b276e7768\"><SigningTime>2022-03-24T17:16:58</SigningTime></SignatureProperty></SignatureProperties></Object></Signature></TTCQT><CQT><Signature Id=\"CQT-Id-2da3b60292a14702aec6cb5b276e7768\" xmlns=\"http://www.w3.org/2000/09/xmldsig#\"><SignedInfo><CanonicalizationMethod Algorithm=\"http://www.w3.org/TR/2001/REC-xml-c14n-20010315\" /><SignatureMethod Algorithm=\"http://www.w3.org/2000/09/xmldsig#rsa-sha1\" /><Reference URI=\"#Object-CQT-Id-2da3b60292a14702aec6cb5b276e7768\"><DigestMethod Algorithm=\"http://www.w3.org/2000/09/xmldsig#sha1\" /><DigestValue>n2s5aNIkte2q5duZ7jTN4Vf19jY=</DigestValue></Reference><Reference URI=\"#Id-2da3b60292a14702aec6cb5b276e7768\"><DigestMethod Algorithm=\"http://www.w3.org/2000/09/xmldsig#sha1\" /><DigestValue>TEjemE/WjuxrSjYy4yYxVlP3lDw=</DigestValue></Reference><Reference URI=\"#Id-6813e99ffeb542b1ae9c2d8bbfab068f\"><DigestMethod Algorithm=\"http://www.w3.org/2000/09/xmldsig#sha1\" /><DigestValue>0CFae5LbGa1P+yjMVi8zH6j0NHs=</DigestValue></Reference></SignedInfo><SignatureValue>mMA4tHjS1AThpRFELoHY8qCEHZDLD277kB21BvcN0TaNllNjcwzcXR+xRbABFmkZ7t82eAdqYS6XibhFk4Yov0PW2rD+LYOzPbf1g+C6CJIPPZfVkCdADY6i7QAJxDQ027n03jNSvnNFTbHyavVxXxilZ0AtZhezSP8JNYIdDNWxcnnqwIIum6XldGPnEdbwI/oHtFP+1QgnZ03JabpXo3sH87o7AXi9PmTezFIWQELqdOZGI51Oy5dgArO75Rl3ek8AoDgRoycVYlpPUT7xf9DXE/gy0yotb8DJvrnooyUpmNYzv7MmSY5SxZU2VOnY6fLcMRWAM0gryyug59D4uw==</SignatureValue><KeyInfo><X509Data><X509SubjectName>CN=Chi cục Thuế Gò Vấp, L=Hồ Chí Minh, OU=Cục Thuế thành phố Hồ Chí Minh, OU=Tổng cục Thuế, O=Bộ Tài chính, C=VN</X509SubjectName><X509Certificate>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</X509Certificate></X509Data></KeyInfo><Object Id=\"Object-CQT-Id-2da3b60292a14702aec6cb5b276e7768\"><SignatureProperties xmlns=\"\"><SignatureProperty Target=\"#CQT-Id-2da3b60292a14702aec6cb5b276e7768\" Id=\"SignatureProperty-CQT-Id-2da3b60292a14702aec6cb5b276e7768\"><SigningTime>2022-03-25T08:17:07</SigningTime></SignatureProperty></SignatureProperties></Object></Signature></CQT></DSCKS></TBao></DLieu></TDiep></DuLieu></KetQuaTraCuu>");
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
		pipeline.add(
				new Document("$match", new Document("_id", objectId).append("IsDelete", new Document("$ne", true))));

		MongoClient mongoClient = cfg.mongoClient();
		MongoCollection<Document> collection = mongoClient.getDatabase(cfg.dbName).getCollection("EInvoicePXKDL");
		try {
			docTmp = collection.aggregate(pipeline).allowDiskUse(true).iterator().next();
		} catch (Exception e) {
			
		}

		mongoClient.close();

		if (null == docTmp) {
			responseStatus = new MspResponseStatus(9999, "Không tìm thấy thông tin hóa đơn");
			rsp.setResponseStatus(responseStatus);
			return rsp;
		}
		docFind = new Document("IssuerId", header.getIssuerId()).append("IsDelete", new Document("$ne", true))
				.append("_id", objectId);
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

		options = new FindOneAndUpdateOptions();
		options.upsert(false);
		options.maxTime(5000, TimeUnit.MILLISECONDS);
		options.returnDocument(ReturnDocument.AFTER);

		mongoClient = cfg.mongoClient();
		collection = mongoClient.getDatabase(cfg.dbName).getCollection("EInvoicePXKDL");
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

		docFind = new Document("IssuerId", header.getIssuerId()).append("TaxCode", mst).append("IsDelete",
				new Document("$ne", true));

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

		docFind = new Document("IssuerId", header.getIssuerId()).append("TaxCode", mst).append("IsDelete",
				new Document("$ne", true));

		MongoClient mongoClient = cfg.mongoClient();
		MongoCollection<Document> collection = mongoClient.getDatabase(cfg.dbName).getCollection("DMCustomer");
		try {
			docTmp = collection.find(docFind).allowDiskUse(true).iterator().next();
		} catch (Exception e) {
			
		}

		mongoClient.close();
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

	@Override
	public MsgRsp getMS(JSONRoot jsonRoot) throws Exception {
		Msg msg = jsonRoot.getMsg();
		MsgHeader header = msg.getMsgHeader();
		MsgPage page = msg.getMsgPage();
		page.setSize(100);
		Object objData = msg.getObjData();
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

		Document docKHMS = new Document("IssuerId", header.getIssuerId()).append("IsDelete", new Document("$ne", true))
				.append("KHMSHDon", "6").append("IsActive", true);

		pipeline = new ArrayList<Document>();
		pipeline.add(new Document("$match", docKHMS));
		pipeline.addAll(createFacetForSearchNotSort(page));

		MongoClient mongoClient = cfg.mongoClient();
		MongoCollection<Document> collection = mongoClient.getDatabase(cfg.dbName).getCollection("DMMauSoKyHieu");
		try {
			docTmp1 = collection.aggregate(pipeline).allowDiskUse(true).iterator().next();
		} catch (Exception e) {
			
		}

		mongoClient.close();

		rsp = new MsgRsp(header);
		responseStatus = null;
		if (null == docTmp1) {
			responseStatus = new MspResponseStatus(9999, Constants.MAP_ERROR.get(9999));
			rsp.setResponseStatus(responseStatus);
			return rsp;
		}

		page.setTotalRows(docTmp1.getInteger("total", 0));
		rsp.setMsgPage(page);

		List<Document> rows = null;
		if (docTmp1.get("data") != null && docTmp1.get("data") instanceof List) {
			rows = docTmp1.getList("data", Document.class);
		}
		ObjectId objectId = null;
		String MauSo = "";
		if (null != rows) {
			for (Document doc : rows) {

				String KHHDon = doc.get("KHHDon").toString();
				char words = KHHDon.charAt(KHHDon.length() - 3);
				String s = String.valueOf(words);
				if ("B".equals(s)) {
					objectId = (ObjectId) doc.get("_id");
					MauSo = objectId.toString();

				}
			}

		}
		if (MauSo.equals("")) {
			responseStatus = new MspResponseStatus(999, "ERROR");
			rsp.setResponseStatus(responseStatus);
			return rsp;
		} else {
			responseStatus = new MspResponseStatus(0, MauSo);
			rsp.setResponseStatus(responseStatus);
			return rsp;
		}
//				}
//					
//					String MauSo = docTmp.getEmbedded(Arrays.asList("EInvoiceDetail", "TTChung", "MauSoHD"), "");			
//					
//					responseStatus = new MspResponseStatus(0, MauSo);
//					rsp.setResponseStatus(responseStatus);
//					return rsp;
//				

	}

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

		docFind = new Document("IssuerId", header.getIssuerId()).append("TaxCode", mst).append("IsDelete",
				new Document("$ne", true));

		
		MongoClient mongoClient = cfg.mongoClient();
		MongoCollection<Document> collection = mongoClient.getDatabase(cfg.dbName).getCollection("DMHistoryCustomer");
		try {
			docTmp =   collection.find(docFind).allowDiskUse(true).iterator().next();		
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

}
