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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.bson.Document;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import org.w3c.dom.Element;

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
import vn.sesgroup.hddt.dto.MailConfig;
import vn.sesgroup.hddt.user.dao.AbstractDAO;
import vn.sesgroup.hddt.user.dao.LBBDCTTheDAO;
import vn.sesgroup.hddt.user.service.JPUtils;
import vn.sesgroup.hddt.utility.Json;
import vn.sesgroup.hddt.utility.MailUtils;
import vn.sesgroup.hddt.utility.MailjetSender;
import vn.sesgroup.hddt.utility.SystemParams;
import vn.sesgroup.hddt.utility.Constants;

@Repository
@Transactional
public class LBBDCTTheImpl extends AbstractDAO implements LBBDCTTheDAO {
	@Autowired ConfigConnectMongo cfg;
	@Autowired JPUtils jpUtils;

	private MailUtils mailUtils = new MailUtils();
	private MailjetSender mailJet = new MailjetSender();
	
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
		if(objData != null) {
			jsonData = Json.serializer().nodeFromObject(msg.getObjData());
		}else{
			throw new Exception("Lỗi dữ liệu đầu vào");
		}
	
		String actionCode = header.getActionCode();
		String _id = commons.getTextJsonNode(jsonData.at("/_id")).replaceAll("\\s", "");
		
		String nb_mst = commons.getTextJsonNode(jsonData.at("/NB_MSThue")).replaceAll("\\s", "");
		String nb_dvbh = commons.getTextJsonNode(jsonData.at("/NB_DVBHang")).replaceAll("\\s+", " ");
		String nb_dc = commons.getTextJsonNode(jsonData.at("/NB_DChi")).replaceAll("\\s+", " ");
		String nb_dd = commons.getTextJsonNode(jsonData.at("/NB_DDien")).replaceAll("\\s+", " ");
		String nb_cv = commons.getTextJsonNode(jsonData.at("/NB_CVu")).replaceAll("\\s+", " ");
		String nb_email_receive  = commons.getTextJsonNode(jsonData.at("/Email_Recive")).replaceAll("\\s", "");
		
		String nm_mst = commons.getTextJsonNode(jsonData.at("/NM_MSThue")).replaceAll("\\s", "");
		String nm_dvmh = commons.getTextJsonNode(jsonData.at("/NM_DVMHang")).replaceAll("\\s+", " ");
		String nm_dc = commons.getTextJsonNode(jsonData.at("/NM_DChi")).replaceAll("\\s+", " ");
		String nm_dd = commons.getTextJsonNode(jsonData.at("/NM_DDien")).replaceAll("\\s+", " ");
		String nm_cv = commons.getTextJsonNode(jsonData.at("/NM_CVu")).replaceAll("\\s+", " ");
		String nm_email_send  = commons.getTextJsonNode(jsonData.at("/Email_Send")).replaceAll("\\s", "");
		
		String loaibb = commons.getTextJsonNode(jsonData.at("/LBBan")).replaceAll("\\s", "");
		String sbban = commons.getTextJsonNode(jsonData.at("/SBBan")).replaceAll("\\s", "");
		String ndsai = commons.getTextJsonNode(jsonData.at("/NDSai")).replaceAll("\\s+", " ");
		String nddung = commons.getTextJsonNode(jsonData.at("/NDDung")).replaceAll("\\s+", " ");
		String ndtnhat = commons.getTextJsonNode(jsonData.at("/NDTNhat")).replaceAll("\\s+", " ");
		String tenbb = loaibb.equals("1") ? "Biên bản thay thế" : "Biên bản điều chỉnh";
		
		JsonNode jsonNodeHDon = jsonData.at("/HDon");
		JsonNode jsonNodeHDNew = jsonData.at("/HDNew");
		String old_mccqt = commons.getTextJsonNode(jsonNodeHDon.at("/MCQTCap"));
		int old_shdon = 0;
		if(!"".equals(commons.getTextJsonNode(jsonNodeHDon.at("/SHDon")))) {
			old_shdon = Integer.parseInt(commons.getTextJsonNode(jsonNodeHDon.at("/SHDon")));
		}
		String old_khhdon = commons.getTextJsonNode(jsonNodeHDon.at("/MSHDon")).replaceFirst("^\\d", "");
		
		String new_mccqt = commons.getTextJsonNode(jsonNodeHDNew.at("/MCQTCap"));
		int new_shdon = 0;
		if (!"".equals(commons.getTextJsonNode(jsonNodeHDNew.at("/SHDon")))) {
			new_shdon = Integer.parseInt(commons.getTextJsonNode(jsonNodeHDNew.at("/SHDon")));
		}
		String new_khhdon = commons.getTextJsonNode(jsonNodeHDNew.at("/MSHDon")).replaceFirst("^\\d", "");
		
		ObjectId objectId = null;
		Document docFind = null;
		Document docFind1 = null;
		Document docFind2 = null;
		Document filter = null;
		Document filter1 = null;
		Document filter2 = null;
		Document docUpdate = null;
		Document oldInvoiceDoc = null;
		Document newInvoiceDoc = null;
				
		String fileNameXML = "";
		String taxCode = "";
		String pathDir = "";
		String secureKey = "";
		Path path = null;
		File file = null;
		
		DocumentBuilderFactory dbf = null;
		DocumentBuilder db = null;
		org.w3c.dom.Document doc = null;
		Element root = null;
		Element elementContent = null;
		Element elementSubContent = null;
		
		boolean isSaveFile = false;
		String oldCollectionName = "";
		String newCollectionName = "";
		
		Document oldDoc = null;
		Document newDoc = null;
		
		FindOneAndUpdateOptions options = null;
		
		DateTimeFormatter format_time = null;
		LocalDateTime time_dem  = null;
		String time = null;
		String name_company = "";
		
		switch (actionCode) {
		case Constants.MSG_ACTION_CODE.CREATED:
			try {
				objectId = new ObjectId(header.getIssuerId());
			} catch (Exception e) {
			}

			docFind = new Document("_id", objectId).append("IsActive", true).append("IsDelete", false);
			
			filter = new Document("_id", 1).append("TaxCode", 1).append("Name", 1).append("Address", 1)
					.append("Phone", 1).append("Fax", 1).append("Email", 1).append("Website", 1)
					.append("TinhThanhInfo", 1).append("ChiCucThueInfo", 1).append("BankAccount", 1).append("NameEN", 1)
					.append("BankAccountExt", 1);
			
			docFind1 = new Document("IssuerId", header.getIssuerId())
					.append("HDSS.TCTBao", new Document("$ne", "1"))
					.append("EInvoiceStatus",
							new Document("$in",
									Arrays.asList("COMPLETE", "ADJUSTED", "REPLACED")))
					.append("MCCQT", old_mccqt)
					.append("EInvoiceDetail.TTChung.SHDon", old_shdon)
					.append("EInvoiceDetail.TTChung.KHHDon", old_khhdon);

			filter1 = new Document("MCCQT", 1)
					.append("EInvoiceDetail", 1);

			oldInvoiceDoc = getDocuments(docFind, filter, docFind1, filter1, null, null);
			
			if (oldInvoiceDoc == null) {
				responseStatus = new MspResponseStatus(9999, "Không tìm thấy thông tin khách hàng.");
				rsp.setResponseStatus(responseStatus);
				return rsp;
			}
			
			oldCollectionName = "";
			if (oldInvoiceDoc.get("EInvoice") != null) {
				oldCollectionName = "EInvoice";
			}
			if (oldInvoiceDoc.get("EInvoicePXK") != null) {
				oldCollectionName = "EInvoicePXK";
			}
			if (oldInvoiceDoc.get("EInvoiceBH") != null) {
				oldCollectionName = "EInvoiceBH";
			}
			if (oldInvoiceDoc.get("EInvoicePXKDL") != null) {
				oldCollectionName = "EInvoicePXKDL";
			}
			if (oldInvoiceDoc.get("EInvoiceMTT") != null) {
				oldCollectionName = "EInvoiceMTT";
			}
			
			if (oldCollectionName.equals("")) {
				responseStatus = new MspResponseStatus(9999, "Không tìm thấy thông tin hóa đơn cần điều chỉnh/thay thế.");
				rsp.setResponseStatus(responseStatus);
				return rsp;
			}
			
			docFind1 = new Document("IssuerId", header.getIssuerId())
					.append("HDSS.TCTBao", new Document("$ne", "1"))
					.append("EInvoiceStatus",
							new Document("$in",
									Arrays.asList("COMPLETE", "ADJUSTED", "REPLACED")))
					.append("MCCQT", new_mccqt)
					.append("EInvoiceDetail.TTChung.SHDon", new_shdon)
					.append("EInvoiceDetail.TTChung.KHHDon", new_khhdon);
			
			newInvoiceDoc = getDocuments(docFind, filter, docFind1, filter1, null, null);
			
			if (newInvoiceDoc == null) {
				responseStatus = new MspResponseStatus(9999, "Không tìm thấy thông tin khách hàng.");
				rsp.setResponseStatus(responseStatus);
				return rsp;
			}
			
			newCollectionName = "";
			if (newInvoiceDoc.get("EInvoice") != null) {
				newCollectionName = "EInvoice";
			}
			if (newInvoiceDoc.get("EInvoicePXK") != null) {
				newCollectionName = "EInvoicePXK";
			}
			if (newInvoiceDoc.get("EInvoiceBH") != null) {
				newCollectionName = "EInvoiceBH";
			}
			if (newInvoiceDoc.get("EInvoicePXKDL") != null) {
				newCollectionName = "EInvoicePXKDL";
			}
			if (newInvoiceDoc.get("EInvoiceMTT") != null) {
				newCollectionName = "EInvoiceMTT";
			}

			if (newCollectionName.equals("")) {
				responseStatus = new MspResponseStatus(9999, "Không tìm thấy thông tin hóa đơn mới.");
				rsp.setResponseStatus(responseStatus);
				return rsp;
			}

			if (!newCollectionName.equals(oldCollectionName)) {
				responseStatus = new MspResponseStatus(9999,
						"Hóa đơn cần điều chỉnh/thay thế và hóa đơn mới không cùng loại.");
				rsp.setResponseStatus(responseStatus);
				return rsp;
			}

			taxCode = oldInvoiceDoc.getString("TaxCode");
			secureKey = commons.csRandomNumbericString(6);
			objectId = new ObjectId();
			path = Paths.get(SystemParams.DIR_E_INVOICE_BBDCTT, taxCode, String.valueOf(LocalDate.now().getYear()));
			pathDir = path.toString();
			file = path.toFile();
			if (!file.exists())
				file.mkdirs();
			fileNameXML = objectId.toString() + ".xml";

			oldDoc = oldInvoiceDoc.get(oldCollectionName, Document.class);
			newDoc = newInvoiceDoc.get(newCollectionName, Document.class);

			dbf = DocumentBuilderFactory.newInstance();
			db = dbf.newDocumentBuilder();
			doc = db.newDocument();
			doc.setXmlStandalone(true);

			root = doc.createElement("BBDCTThe");
			doc.appendChild(root);

			elementContent = doc.createElement("DLBBDCTThe");
			elementContent.setAttribute("Id", "data");
			root.appendChild(elementContent);
			
			elementContent.appendChild(commons.createElementWithValue(doc, "PBan", SystemParams.VERSION_XML_HDSS));
			elementContent.appendChild(commons.createElementWithValue(doc, "Ten", tenbb));
			elementContent.appendChild(commons.createElementWithValue(doc, "Loai", loaibb));
			elementContent.appendChild(commons.createElementWithValue(doc, "SBBan", sbban));
			elementContent.appendChild(commons.createElementWithValue(doc, "NLap", LocalDate.now().toString()));
			elementContent.appendChild(commons.createElementWithValue(doc, "NDSai", ndsai));
			elementContent.appendChild(commons.createElementWithValue(doc, "NDDung", nddung));
			elementContent.appendChild(commons.createElementWithValue(doc, "NDTNhat", ndtnhat));
			elementContent.appendChild(commons.createElementWithValue(doc, "SecureKey", secureKey));

			elementSubContent = doc.createElement("TTNBan");
			elementSubContent.appendChild(commons.createElementWithValue(doc, "MSThue", nb_mst));
			elementSubContent.appendChild(commons.createElementWithValue(doc, "DVBHang", nb_dvbh));
			elementSubContent.appendChild(commons.createElementWithValue(doc, "DChi", nb_dc));
			elementSubContent.appendChild(commons.createElementWithValue(doc, "DDien", nb_dd));
			elementSubContent.appendChild(commons.createElementWithValue(doc, "CVu", nb_cv));
			elementSubContent.appendChild(commons.createElementWithValue(doc, "EReceive", nb_email_receive));

			elementContent.appendChild(elementSubContent);

			elementSubContent = doc.createElement("TTNMua");
			elementSubContent.appendChild(commons.createElementWithValue(doc, "MSThue", nm_mst));
			elementSubContent.appendChild(commons.createElementWithValue(doc, "DVMHang", nm_dvmh));
			elementSubContent.appendChild(commons.createElementWithValue(doc, "DChi", nm_dc));
			elementSubContent.appendChild(commons.createElementWithValue(doc, "DDien", nm_dd));
			elementSubContent.appendChild(commons.createElementWithValue(doc, "CVu", nm_cv));
			elementSubContent.appendChild(commons.createElementWithValue(doc, "ESend", nm_email_send));

			elementContent.appendChild(elementSubContent);

			elementSubContent = doc.createElement("HDSSot");
			elementSubContent.appendChild(commons.createElementWithValue(doc, "THDon",
					oldDoc.getEmbedded(Arrays.asList("EInvoiceDetail", "TTChung", "THDon"), "")));
			elementSubContent.appendChild(commons.createElementWithValue(doc, "MaHD",
					oldDoc.getEmbedded(Arrays.asList("EInvoiceDetail", "TTChung", "MaHD"), "")));
			elementSubContent.appendChild(commons.createElementWithValue(doc, "MauSoHD",
					oldDoc.getEmbedded(Arrays.asList("EInvoiceDetail", "TTChung", "MauSoHD"), "")));
			elementSubContent.appendChild(commons.createElementWithValue(doc, "KHMSHDon",
					oldDoc.getEmbedded(Arrays.asList("EInvoiceDetail", "TTChung", "KHMSHDon"), "")));
			elementSubContent.appendChild(commons.createElementWithValue(doc, "KHHDon",
					oldDoc.getEmbedded(Arrays.asList("EInvoiceDetail", "TTChung", "KHHDon"), "")));
			elementSubContent.appendChild(commons.createElementWithValue(doc, "DVTTe",
					oldDoc.getEmbedded(Arrays.asList("EInvoiceDetail", "TTChung", "DVTTe"), "")));
			elementSubContent.appendChild(commons.createElementWithValue(doc, "NLap",
					commons.convertLocalDateTimeToString(
							commons.convertDateToLocalDateTime(
									oldDoc.getEmbedded(Arrays.asList("EInvoiceDetail", "TTChung", "NLap"), Date.class)),
							"yyyy-MM-dd")));
			elementSubContent.appendChild(commons.createElementWithValue(doc, "SHDon",
					String.valueOf(oldDoc.getEmbedded(Arrays.asList("EInvoiceDetail", "TTChung", "SHDon"), ""))));
			elementSubContent.appendChild(commons.createElementWithValue(doc, "TNMua",
					oldDoc.getEmbedded(Arrays.asList("EInvoiceDetail", "NDHDon", "NMua", "Ten"), "")));
			elementSubContent.appendChild(commons.createElementWithValue(doc, "TgTCThue",
					String.valueOf(oldDoc.getEmbedded(Arrays.asList("EInvoiceDetail", "TToan", "TgTCThue"), ""))));
			elementSubContent.appendChild(commons.createElementWithValue(doc, "TgTThue",
					String.valueOf(oldDoc.getEmbedded(Arrays.asList("EInvoiceDetail", "TToan", "TgTThue"), ""))));
			elementSubContent.appendChild(commons.createElementWithValue(doc, "TgTTTBSo",
					String.valueOf(oldDoc.getEmbedded(Arrays.asList("EInvoiceDetail", "TToan", "TgTTTBSo"), ""))));
			elementSubContent.appendChild(commons.createElementWithValue(doc, "TgTTTBChu",
					String.valueOf(oldDoc.getEmbedded(Arrays.asList("EInvoiceDetail", "TToan", "TgTTTBChu"), ""))));
			elementSubContent.appendChild(
					commons.createElementWithValue(doc, "MCCQT", oldDoc.getEmbedded(Arrays.asList("MCCQT"), "")));

			elementContent.appendChild(elementSubContent);

			elementSubContent = doc.createElement("HDDCTThe");
			elementSubContent.appendChild(commons.createElementWithValue(doc, "THDon",
					newDoc.getEmbedded(Arrays.asList("EInvoiceDetail", "TTChung", "THDon"), "")));
			elementSubContent.appendChild(commons.createElementWithValue(doc, "MaHD",
					newDoc.getEmbedded(Arrays.asList("EInvoiceDetail", "TTChung", "MaHD"), "")));
			elementSubContent.appendChild(commons.createElementWithValue(doc, "MauSoHD",
					newDoc.getEmbedded(Arrays.asList("EInvoiceDetail", "TTChung", "MauSoHD"), "")));
			elementSubContent.appendChild(commons.createElementWithValue(doc, "KHMSHDon",
					newDoc.getEmbedded(Arrays.asList("EInvoiceDetail", "TTChung", "KHMSHDon"), "")));
			elementSubContent.appendChild(commons.createElementWithValue(doc, "KHHDon",
					newDoc.getEmbedded(Arrays.asList("EInvoiceDetail", "TTChung", "KHHDon"), "")));
			elementSubContent.appendChild(commons.createElementWithValue(doc, "DVTTe",
					newDoc.getEmbedded(Arrays.asList("EInvoiceDetail", "TTChung", "DVTTe"), "")));
			elementSubContent.appendChild(commons.createElementWithValue(doc, "NLap",
					commons.convertLocalDateTimeToString(
							commons.convertDateToLocalDateTime(
									newDoc.getEmbedded(Arrays.asList("EInvoiceDetail", "TTChung", "NLap"), Date.class)),
							"yyyy-MM-dd")));
			elementSubContent.appendChild(commons.createElementWithValue(doc, "SHDon",
					String.valueOf(newDoc.getEmbedded(Arrays.asList("EInvoiceDetail", "TTChung", "SHDon"), ""))));
			elementSubContent.appendChild(commons.createElementWithValue(doc, "TNMua",
					newDoc.getEmbedded(Arrays.asList("EInvoiceDetail", "NDHDon", "NMua", "Ten"), "")));
			elementSubContent.appendChild(commons.createElementWithValue(doc, "TgTCThue",
					String.valueOf(newDoc.getEmbedded(Arrays.asList("EInvoiceDetail", "TToan", "TgTCThue"), ""))));
			elementSubContent.appendChild(commons.createElementWithValue(doc, "TgTThue",
					String.valueOf(newDoc.getEmbedded(Arrays.asList("EInvoiceDetail", "TToan", "TgTThue"), ""))));
			elementSubContent.appendChild(commons.createElementWithValue(doc, "TgTTTBSo",
					String.valueOf(newDoc.getEmbedded(Arrays.asList("EInvoiceDetail", "TToan", "TgTTTBSo"), ""))));
			elementSubContent.appendChild(commons.createElementWithValue(doc, "TgTTTBChu",
					String.valueOf(newDoc.getEmbedded(Arrays.asList("EInvoiceDetail", "TToan", "TgTTTBChu"), ""))));
			elementSubContent.appendChild(
					commons.createElementWithValue(doc, "MCCQT", newDoc.getEmbedded(Arrays.asList("MCCQT"), "")));
			elementContent.appendChild(elementSubContent);

			isSaveFile = commons.docW3cToFile(doc, pathDir, fileNameXML);
			if (!isSaveFile) {
				throw new Exception("Lưu dữ liệu biên bản điều chỉnh/thay thế không thành công.");
			}
			
			Document docInsert = new Document();
			docInsert
				.append("_id", objectId)
				.append("IssuerId", header.getIssuerId())
				.append("PBan", SystemParams.VERSION_XML_HDSS)
				.append("Ten", tenbb)
				.append("Loai", loaibb)
				.append("SBBan", sbban)
				.append("NLap", LocalDate.now().toString())
				.append("NDSai", ndsai)
				.append("NDDung", nddung)
				.append("NDTNhat", ndtnhat)
				.append("TTNBan", 
						new Document()
						.append("MSThue", nb_mst)
						.append("DVBHang", nb_dvbh)
						.append("DChi", nb_dc)
						.append("DDien", nb_mst)
						.append("CVu", nb_cv)
						.append("EReceive", nb_email_receive)
						)
				.append("TTNMua", 
						new Document()
						.append("MSThue", nm_mst)
						.append("DVMHang", nm_dvmh)
						.append("DChi", nm_dc)
						.append("DDien", nm_dd)
						.append("CVu", nm_cv)
						.append("ESend", nm_email_send)
						)
				.append("HDSSot", 
						new Document()
						.append("THDon", oldDoc.getEmbedded(Arrays.asList("EInvoiceDetail", "TTChung", "THDon"), ""))
						.append("MaHD", oldDoc.getEmbedded(Arrays.asList("EInvoiceDetail", "TTChung", "MaHD"), ""))
						.append("MauSoHD", oldDoc.getEmbedded(Arrays.asList("EInvoiceDetail", "TTChung", "MauSoHD"), ""))
						.append("KHMSHDon", oldDoc.getEmbedded(Arrays.asList("EInvoiceDetail", "TTChung", "KHMSHDon"), ""))
						.append("KHHDon", oldDoc.getEmbedded(Arrays.asList("EInvoiceDetail", "TTChung", "KHHDon"), ""))
						.append("DVTTe", oldDoc.getEmbedded(Arrays.asList("EInvoiceDetail", "TTChung", "DVTTe"), ""))
						.append("NLap", oldDoc.getEmbedded(Arrays.asList("EInvoiceDetail", "TTChung", "NLap"), Date.class))
						.append("SHDon", oldDoc.getEmbedded(Arrays.asList("EInvoiceDetail", "TTChung", "SHDon"), ""))
						.append("TNMua", oldDoc.getEmbedded(Arrays.asList("EInvoiceDetail", "NDHDon", "NMua", "Ten"), ""))
						.append("TgTCThue", oldDoc.getEmbedded(Arrays.asList("EInvoiceDetail", "TToan", "TgTCThue"), ""))
						.append("TgTThue", oldDoc.getEmbedded(Arrays.asList("EInvoiceDetail", "TToan", "TgTThue"), ""))
						.append("TgTTTBSo", oldDoc.getEmbedded(Arrays.asList("EInvoiceDetail", "TToan", "TgTTTBSo"), ""))
						.append("TgTTTBChu", oldDoc.getEmbedded(Arrays.asList("EInvoiceDetail", "TToan", "TgTTTBChu"), ""))
						.append("MCCQT", oldDoc.getEmbedded(Arrays.asList("MCCQT"), ""))
						)
				.append("HDDCTThe", 
						new Document()
						.append("THDon", newDoc.getEmbedded(Arrays.asList("EInvoiceDetail", "TTChung", "THDon"), ""))
						.append("MaHD", newDoc.getEmbedded(Arrays.asList("EInvoiceDetail", "TTChung", "MaHD"), ""))
						.append("MauSoHD", newDoc.getEmbedded(Arrays.asList("EInvoiceDetail", "TTChung", "MauSoHD"), ""))
						.append("KHMSHDon", newDoc.getEmbedded(Arrays.asList("EInvoiceDetail", "TTChung", "KHMSHDon"), ""))
						.append("KHHDon", newDoc.getEmbedded(Arrays.asList("EInvoiceDetail", "TTChung", "KHHDon"), ""))
						.append("DVTTe", newDoc.getEmbedded(Arrays.asList("EInvoiceDetail", "TTChung", "DVTTe"), ""))
						.append("NLap", newDoc.getEmbedded(Arrays.asList("EInvoiceDetail", "TTChung", "NLap"), Date.class))
						.append("SHDon", newDoc.getEmbedded(Arrays.asList("EInvoiceDetail", "TTChung", "SHDon"), ""))
						.append("TNMua", newDoc.getEmbedded(Arrays.asList("EInvoiceDetail", "NDHDon", "NMua", "Ten"), ""))
						.append("TgTCThue", newDoc.getEmbedded(Arrays.asList("EInvoiceDetail", "TToan", "TgTCThue"), ""))
						.append("TgTThue", newDoc.getEmbedded(Arrays.asList("EInvoiceDetail", "TToan", "TgTThue"), ""))
						.append("TgTTTBSo", newDoc.getEmbedded(Arrays.asList("EInvoiceDetail", "TToan", "TgTTTBSo"), ""))
						.append("TgTTTBChu", newDoc.getEmbedded(Arrays.asList("EInvoiceDetail", "TToan", "TgTTTBChu"), ""))
						.append("MCCQT", newDoc.getEmbedded(Arrays.asList("MCCQT"), ""))
						)

				.append("SignStatusCode", Constants.INVOICE_SIGN_STATUS.NOSIGN)
				.append("ClientSignStatusCode", Constants.INVOICE_SIGN_STATUS.NOSIGN)
				.append("Status", Constants.INVOICE_STATUS.CREATED)
				.append("IsDelete", false)
				.append("SecureKey", secureKey)
				.append("Dir", pathDir)
				.append("FileNameXML", fileNameXML)
				.append("InfoCreated", 
						new Document()
						.append("CreateDate", LocalDateTime.now())
						.append("CreateUserID", header.getUserId())
						.append("CreateUserName", header.getUserName())
						.append("CreateUserFullName", header.getUserFullName())
					)
				;
			try (MongoClient mongoClient = cfg.mongoClient()){
				MongoCollection<Document> collection = mongoClient.getDatabase(cfg.dbName).getCollection("EInvoiceBBDCTT");
				collection.insertOne(docInsert);
			} catch (Exception e) {
				e.printStackTrace();
			}
			format_time = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
			time_dem = LocalDateTime.now();
			time = time_dem.format(format_time);
			name_company = removeAccent(header.getUserFullName());
			System.out.println(time + name_company + " Vua lap bien ban dieu chinh thay the");
			responseStatus = new MspResponseStatus(0, "SUCCESS");
			rsp.setResponseStatus(responseStatus);
			return rsp;
			
		case Constants.MSG_ACTION_CODE.MODIFY:	
			ObjectId objectIdEInvoiceBBDCTT = null;
			try {
				objectId = new ObjectId(header.getIssuerId());
				objectIdEInvoiceBBDCTT = new ObjectId(_id);
			} catch (Exception e) {
			}
			
			docFind = new Document("_id", objectId).append("IsActive", true).append("IsDelete", false);
			
			filter = new Document("_id", 1).append("TaxCode", 1).append("Name", 1).append("Address", 1)
					.append("Phone", 1).append("Fax", 1).append("Email", 1).append("Website", 1)
					.append("TinhThanhInfo", 1).append("ChiCucThueInfo", 1).append("BankAccount", 1).append("NameEN", 1)
					.append("BankAccountExt", 1);
			
			docFind1 = new Document("IssuerId", header.getIssuerId())
					.append("HDSS.TCTBao", new Document("$ne", "1"))
					.append("EInvoiceStatus",
							new Document("$in",
									Arrays.asList("COMPLETE", "ADJUSTED","REPLACED")))
					.append("MCCQT", old_mccqt)
					.append("EInvoiceDetail.TTChung.SHDon", old_shdon)
					.append("EInvoiceDetail.TTChung.KHHDon", old_khhdon);

			filter1 = new Document("MCCQT", 1)
					.append("EInvoiceDetail", 1);

			oldInvoiceDoc = getDocuments(docFind, filter, docFind1, filter1, null, null);
			
			if (oldInvoiceDoc == null) {
				responseStatus = new MspResponseStatus(9999, "Không tìm thấy thông tin khách hàng.");
				rsp.setResponseStatus(responseStatus);
				return rsp;
			}
			
			oldCollectionName = "";
			if (oldInvoiceDoc.get("EInvoice") != null) {
				oldCollectionName = "EInvoice";
			}
			if (oldInvoiceDoc.get("EInvoicePXK") != null) {
				oldCollectionName = "EInvoicePXK";
			}
			if (oldInvoiceDoc.get("EInvoiceBH") != null) {
				oldCollectionName = "EInvoiceBH";
			}
			if (oldInvoiceDoc.get("EInvoicePXKDL") != null) {
				oldCollectionName = "EInvoicePXKDL";
			}
			if (oldInvoiceDoc.get("EInvoiceMTT") != null) {
				oldCollectionName = "EInvoiceMTT";
			}
			
			if (oldCollectionName.equals("")) {
				responseStatus = new MspResponseStatus(9999, "Không tìm thấy thông tin hóa đơn cần điều chỉnh/thay thế.");
				rsp.setResponseStatus(responseStatus);
				return rsp;
			}
			
			docFind1 = new Document("IssuerId", header.getIssuerId())
					.append("HDSS.TCTBao", new Document("$ne", "1"))
					.append("EInvoiceStatus",
							new Document("$in",
									Arrays.asList("COMPLETE", "ADJUSTED","REPLACED")))
					.append("MCCQT", new_mccqt)
					.append("EInvoiceDetail.TTChung.SHDon", new_shdon)
					.append("EInvoiceDetail.TTChung.KHHDon", new_khhdon);
			
			docFind2 = new Document("IssuerId", header.getIssuerId())
					.append("IsDelete", false)
					.append("SignStatusCode", Constants.INVOICE_SIGN_STATUS.NOSIGN)
					.append("Status", Constants.INVOICE_STATUS.CREATED)
					.append("_id", objectIdEInvoiceBBDCTT);
	
			filter2 = new Document("Dir", 1)
					.append("FileNameXML", 1)
					.append("SecureKey", 1);
			
			newInvoiceDoc = getDocuments(docFind, filter, docFind1, filter1, docFind2, filter2);
			
			if (newInvoiceDoc == null) {
				responseStatus = new MspResponseStatus(9999, "Không tìm thấy thông tin khách hàng.");
				rsp.setResponseStatus(responseStatus);
				return rsp;
			}
			
			newCollectionName = "";
			if (newInvoiceDoc.get("EInvoice") != null) {
				newCollectionName = "EInvoice";
			}
			if (newInvoiceDoc.get("EInvoicePXK") != null) {
				newCollectionName = "EInvoicePXK";
			}
			if (newInvoiceDoc.get("EInvoiceBH") != null) {
				newCollectionName = "EInvoiceBH";
			}
			if (newInvoiceDoc.get("EInvoicePXKDL") != null) {
				newCollectionName = "EInvoicePXKDL";
			}
			if (newInvoiceDoc.get("EInvoiceMTT") != null) {
				newCollectionName = "EInvoiceMTT";
			}
			
			if (newInvoiceDoc.get("EInvoiceBBDCTT") == null) {
				responseStatus = new MspResponseStatus(9999, "Biên bản điều chỉnh thay thế có trạng thái không hợp lệ.");
				rsp.setResponseStatus(responseStatus);
				return rsp;
			}

			if (newCollectionName.equals("")) {
				responseStatus = new MspResponseStatus(9999, "Không tìm thấy thông tin hóa đơn mới.");
				rsp.setResponseStatus(responseStatus);
				return rsp;
			}

			if (!newCollectionName.equals(oldCollectionName)) {
				responseStatus = new MspResponseStatus(9999,
						"Hóa đơn cần điều chỉnh/thay thế và hóa đơn mới không cùng loại.");
				rsp.setResponseStatus(responseStatus);
				return rsp;
			}
			secureKey = newInvoiceDoc.getEmbedded(Arrays.asList("EInvoiceBBDCTT", "SecureKey"), "");
			pathDir = newInvoiceDoc.getEmbedded(Arrays.asList("EInvoiceBBDCTT", "Dir"), "");
			fileNameXML = newInvoiceDoc.getEmbedded(Arrays.asList("EInvoiceBBDCTT", "FileNameXML"), "");
			file = new File(pathDir);
			if(!file.exists()) file.mkdirs();
			
			oldDoc = oldInvoiceDoc.get(oldCollectionName, Document.class);
			newDoc = newInvoiceDoc.get(newCollectionName, Document.class);
			
			dbf = DocumentBuilderFactory.newInstance();
			db = dbf.newDocumentBuilder();
			doc = db.newDocument();
			doc.setXmlStandalone(true);

			root = doc.createElement("BBDCTThe");
			doc.appendChild(root);

			elementContent = doc.createElement("DLBBDCTThe");
			elementContent.setAttribute("Id", "data");
			root.appendChild(elementContent);
			
			elementContent.appendChild(commons.createElementWithValue(doc, "PBan", SystemParams.VERSION_XML_HDSS));
			elementContent.appendChild(commons.createElementWithValue(doc, "Ten", tenbb));
			elementContent.appendChild(commons.createElementWithValue(doc, "Loai", loaibb));
			elementContent.appendChild(commons.createElementWithValue(doc, "SBBan", sbban));
			elementContent.appendChild(commons.createElementWithValue(doc, "NLap", LocalDate.now().toString()));
			elementContent.appendChild(commons.createElementWithValue(doc, "NDSai", ndsai));
			elementContent.appendChild(commons.createElementWithValue(doc, "NDDung", nddung));
			elementContent.appendChild(commons.createElementWithValue(doc, "NDTNhat", ndtnhat));
			elementContent.appendChild(commons.createElementWithValue(doc, "SecureKey", secureKey));

			elementSubContent = doc.createElement("TTNBan");
			elementSubContent.appendChild(commons.createElementWithValue(doc, "MSThue", nb_mst));
			elementSubContent.appendChild(commons.createElementWithValue(doc, "DVBHang", nb_dvbh));
			elementSubContent.appendChild(commons.createElementWithValue(doc, "DChi", nb_dc));
			elementSubContent.appendChild(commons.createElementWithValue(doc, "DDien", nb_dd));
			elementSubContent.appendChild(commons.createElementWithValue(doc, "CVu", nb_cv));
			elementSubContent.appendChild(commons.createElementWithValue(doc, "EReceive", nb_email_receive));

			elementContent.appendChild(elementSubContent);

			elementSubContent = doc.createElement("TTNMua");
			elementSubContent.appendChild(commons.createElementWithValue(doc, "MSThue", nm_mst));
			elementSubContent.appendChild(commons.createElementWithValue(doc, "DVMHang", nm_dvmh));
			elementSubContent.appendChild(commons.createElementWithValue(doc, "DChi", nm_dc));
			elementSubContent.appendChild(commons.createElementWithValue(doc, "DDien", nm_dd));
			elementSubContent.appendChild(commons.createElementWithValue(doc, "CVu", nm_cv));
			elementSubContent.appendChild(commons.createElementWithValue(doc, "ESend", nm_email_send));

			elementContent.appendChild(elementSubContent);

			elementSubContent = doc.createElement("HDSSot");
			elementSubContent.appendChild(commons.createElementWithValue(doc, "THDon",
					oldDoc.getEmbedded(Arrays.asList("EInvoiceDetail", "TTChung", "THDon"), "")));
			elementSubContent.appendChild(commons.createElementWithValue(doc, "MaHD",
					oldDoc.getEmbedded(Arrays.asList("EInvoiceDetail", "TTChung", "MaHD"), "")));
			elementSubContent.appendChild(commons.createElementWithValue(doc, "MauSoHD",
					oldDoc.getEmbedded(Arrays.asList("EInvoiceDetail", "TTChung", "MauSoHD"), "")));
			elementSubContent.appendChild(commons.createElementWithValue(doc, "KHMSHDon",
					oldDoc.getEmbedded(Arrays.asList("EInvoiceDetail", "TTChung", "KHMSHDon"), "")));
			elementSubContent.appendChild(commons.createElementWithValue(doc, "KHHDon",
					oldDoc.getEmbedded(Arrays.asList("EInvoiceDetail", "TTChung", "KHHDon"), "")));
			elementSubContent.appendChild(commons.createElementWithValue(doc, "DVTTe",
					oldDoc.getEmbedded(Arrays.asList("EInvoiceDetail", "TTChung", "DVTTe"), "")));
			elementSubContent.appendChild(commons.createElementWithValue(doc, "NLap",
					commons.convertLocalDateTimeToString(
							commons.convertDateToLocalDateTime(
									oldDoc.getEmbedded(Arrays.asList("EInvoiceDetail", "TTChung", "NLap"), Date.class)),
							"yyyy-MM-dd")));
			elementSubContent.appendChild(commons.createElementWithValue(doc, "SHDon",
					String.valueOf(oldDoc.getEmbedded(Arrays.asList("EInvoiceDetail", "TTChung", "SHDon"), ""))));
			elementSubContent.appendChild(commons.createElementWithValue(doc, "TNMua",
					oldDoc.getEmbedded(Arrays.asList("EInvoiceDetail", "NDHDon", "NMua", "Ten"), "")));
			elementSubContent.appendChild(commons.createElementWithValue(doc, "TgTCThue",
					String.valueOf(oldDoc.getEmbedded(Arrays.asList("EInvoiceDetail", "TToan", "TgTCThue"), ""))));
			elementSubContent.appendChild(commons.createElementWithValue(doc, "TgTThue",
					String.valueOf(oldDoc.getEmbedded(Arrays.asList("EInvoiceDetail", "TToan", "TgTThue"), ""))));
			elementSubContent.appendChild(commons.createElementWithValue(doc, "TgTTTBSo",
					String.valueOf(oldDoc.getEmbedded(Arrays.asList("EInvoiceDetail", "TToan", "TgTTTBSo"), ""))));
			elementSubContent.appendChild(commons.createElementWithValue(doc, "TgTTTBChu",
					String.valueOf(oldDoc.getEmbedded(Arrays.asList("EInvoiceDetail", "TToan", "TgTTTBChu"), ""))));
			elementSubContent.appendChild(
					commons.createElementWithValue(doc, "MCCQT", oldDoc.getEmbedded(Arrays.asList("MCCQT"), "")));

			elementContent.appendChild(elementSubContent);

			elementSubContent = doc.createElement("HDDCTThe");
			elementSubContent.appendChild(commons.createElementWithValue(doc, "THDon",
					newDoc.getEmbedded(Arrays.asList("EInvoiceDetail", "TTChung", "THDon"), "")));
			elementSubContent.appendChild(commons.createElementWithValue(doc, "MaHD",
					newDoc.getEmbedded(Arrays.asList("EInvoiceDetail", "TTChung", "MaHD"), "")));
			elementSubContent.appendChild(commons.createElementWithValue(doc, "MauSoHD",
					newDoc.getEmbedded(Arrays.asList("EInvoiceDetail", "TTChung", "MauSoHD"), "")));
			elementSubContent.appendChild(commons.createElementWithValue(doc, "KHMSHDon",
					newDoc.getEmbedded(Arrays.asList("EInvoiceDetail", "TTChung", "KHMSHDon"), "")));
			elementSubContent.appendChild(commons.createElementWithValue(doc, "KHHDon",
					newDoc.getEmbedded(Arrays.asList("EInvoiceDetail", "TTChung", "KHHDon"), "")));
			elementSubContent.appendChild(commons.createElementWithValue(doc, "DVTTe",
					newDoc.getEmbedded(Arrays.asList("EInvoiceDetail", "TTChung", "DVTTe"), "")));
			elementSubContent.appendChild(commons.createElementWithValue(doc, "NLap",
					commons.convertLocalDateTimeToString(
							commons.convertDateToLocalDateTime(
									newDoc.getEmbedded(Arrays.asList("EInvoiceDetail", "TTChung", "NLap"), Date.class)),
							"yyyy-MM-dd")));
			elementSubContent.appendChild(commons.createElementWithValue(doc, "SHDon",
					String.valueOf(newDoc.getEmbedded(Arrays.asList("EInvoiceDetail", "TTChung", "SHDon"), ""))));
			elementSubContent.appendChild(commons.createElementWithValue(doc, "TNMua",
					newDoc.getEmbedded(Arrays.asList("EInvoiceDetail", "NDHDon", "NMua", "Ten"), "")));
			elementSubContent.appendChild(commons.createElementWithValue(doc, "TgTCThue",
					String.valueOf(newDoc.getEmbedded(Arrays.asList("EInvoiceDetail", "TToan", "TgTCThue"), ""))));
			elementSubContent.appendChild(commons.createElementWithValue(doc, "TgTThue",
					String.valueOf(newDoc.getEmbedded(Arrays.asList("EInvoiceDetail", "TToan", "TgTThue"), ""))));
			elementSubContent.appendChild(commons.createElementWithValue(doc, "TgTTTBSo",
					String.valueOf(newDoc.getEmbedded(Arrays.asList("EInvoiceDetail", "TToan", "TgTTTBSo"), ""))));
			elementSubContent.appendChild(commons.createElementWithValue(doc, "TgTTTBChu",
					String.valueOf(newDoc.getEmbedded(Arrays.asList("EInvoiceDetail", "TToan", "TgTTTBChu"), ""))));
			elementSubContent.appendChild(
					commons.createElementWithValue(doc, "MCCQT", newDoc.getEmbedded(Arrays.asList("MCCQT"), "")));
			elementContent.appendChild(elementSubContent);

			isSaveFile = commons.docW3cToFile(doc, pathDir, fileNameXML);
			if (!isSaveFile) {
				throw new Exception("Lưu dữ liệu biên bản điều chỉnh/thay thế không thành công.");
			}
			
			options = new FindOneAndUpdateOptions();
			options.upsert(false);
			options.maxTime(5000, TimeUnit.MILLISECONDS);
			options.returnDocument(ReturnDocument.AFTER);
			
			docFind = new Document("IssuerId", header.getIssuerId()).append("IsDelete", false)
					.append("_id", objectIdEInvoiceBBDCTT);
			docUpdate = new Document();
			docUpdate.append("IssuerId", header.getIssuerId())
			.append("Ten", tenbb)
			.append("Loai", loaibb)
			.append("SBBan", sbban)
			.append("NLap", LocalDate.now().toString())
			.append("NDSai", ndsai)
			.append("NDDung", nddung)
			.append("NDTNhat", ndtnhat)
			.append("TTNBan", 
					new Document()
					.append("MSThue", nb_mst)
					.append("DVBHang", nb_dvbh)
					.append("DChi", nb_dc)
					.append("DDien", nb_mst)
					.append("CVu", nb_cv)
					.append("EReceive", nb_email_receive)
					)
			.append("TTNMua", 
					new Document()
					.append("MSThue", nm_mst)
					.append("DVMHang", nm_dvmh)
					.append("DChi", nm_dc)
					.append("DDien", nm_dd)
					.append("CVu", nm_cv)
					.append("ESend", nm_email_send)
					)
			.append("HDSSot", 
					new Document()
					.append("THDon", oldDoc.getEmbedded(Arrays.asList("EInvoiceDetail", "TTChung", "THDon"), ""))
					.append("MaHD", oldDoc.getEmbedded(Arrays.asList("EInvoiceDetail", "TTChung", "MaHD"), ""))
					.append("MauSoHD", oldDoc.getEmbedded(Arrays.asList("EInvoiceDetail", "TTChung", "MauSoHD"), ""))
					.append("KHMSHDon", oldDoc.getEmbedded(Arrays.asList("EInvoiceDetail", "TTChung", "KHMSHDon"), ""))
					.append("KHHDon", oldDoc.getEmbedded(Arrays.asList("EInvoiceDetail", "TTChung", "KHHDon"), ""))
					.append("DVTTe", oldDoc.getEmbedded(Arrays.asList("EInvoiceDetail", "TTChung", "DVTTe"), ""))
					.append("NLap", oldDoc.getEmbedded(Arrays.asList("EInvoiceDetail", "TTChung", "NLap"), Date.class))
					.append("SHDon", oldDoc.getEmbedded(Arrays.asList("EInvoiceDetail", "TTChung", "SHDon"), ""))
					.append("TNMua", oldDoc.getEmbedded(Arrays.asList("EInvoiceDetail", "NDHDon", "NMua", "Ten"), ""))
					.append("TgTCThue", oldDoc.getEmbedded(Arrays.asList("EInvoiceDetail", "TToan", "TgTCThue"), ""))
					.append("TgTThue", oldDoc.getEmbedded(Arrays.asList("EInvoiceDetail", "TToan", "TgTThue"), ""))
					.append("TgTTTBSo", oldDoc.getEmbedded(Arrays.asList("EInvoiceDetail", "TToan", "TgTTTBSo"), ""))
					.append("TgTTTBChu", oldDoc.getEmbedded(Arrays.asList("EInvoiceDetail", "TToan", "TgTTTBChu"), ""))
					.append("MCCQT", oldDoc.getEmbedded(Arrays.asList("MCCQT"), ""))
					)
			.append("HDDCTThe", 
					new Document()
					.append("THDon", newDoc.getEmbedded(Arrays.asList("EInvoiceDetail", "TTChung", "THDon"), ""))
					.append("MaHD", newDoc.getEmbedded(Arrays.asList("EInvoiceDetail", "TTChung", "MaHD"), ""))
					.append("MauSoHD", newDoc.getEmbedded(Arrays.asList("EInvoiceDetail", "TTChung", "MauSoHD"), ""))
					.append("KHMSHDon", newDoc.getEmbedded(Arrays.asList("EInvoiceDetail", "TTChung", "KHMSHDon"), ""))
					.append("KHHDon", newDoc.getEmbedded(Arrays.asList("EInvoiceDetail", "TTChung", "KHHDon"), ""))
					.append("DVTTe", newDoc.getEmbedded(Arrays.asList("EInvoiceDetail", "TTChung", "DVTTe"), ""))
					.append("NLap", newDoc.getEmbedded(Arrays.asList("EInvoiceDetail", "TTChung", "NLap"), Date.class))
					.append("SHDon", newDoc.getEmbedded(Arrays.asList("EInvoiceDetail", "TTChung", "SHDon"), ""))
					.append("TNMua", newDoc.getEmbedded(Arrays.asList("EInvoiceDetail", "NDHDon", "NMua", "Ten"), ""))
					.append("TgTCThue", newDoc.getEmbedded(Arrays.asList("EInvoiceDetail", "TToan", "TgTCThue"), ""))
					.append("TgTThue", newDoc.getEmbedded(Arrays.asList("EInvoiceDetail", "TToan", "TgTThue"), ""))
					.append("TgTTTBSo", newDoc.getEmbedded(Arrays.asList("EInvoiceDetail", "TToan", "TgTTTBSo"), ""))
					.append("TgTTTBChu", newDoc.getEmbedded(Arrays.asList("EInvoiceDetail", "TToan", "TgTTTBChu"), ""))
					.append("MCCQT", newDoc.getEmbedded(Arrays.asList("MCCQT"), ""))
					)
			.append("InfoUpdated", 
					new Document("UpdatedDate", LocalDateTime.now())
					.append("UpdatedUserID", header.getUserId())
					.append("UpdatedUserName", header.getUserName())
					.append("UpdatedUserFullName", header.getUserFullName())
					);
			
			try (MongoClient mongoClient = cfg.mongoClient()){
				MongoCollection<Document> collection = mongoClient.getDatabase(cfg.dbName).getCollection("EInvoiceBBDCTT");
				collection.findOneAndUpdate(docFind, new Document("$set", docUpdate), options);
			} catch (Exception e) {
				e.printStackTrace();
			}
			
			format_time = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
			time_dem = LocalDateTime.now();
			time = time_dem.format(format_time);
			name_company = removeAccent(header.getUserFullName());
			System.out.println(time + name_company + " Vua thay doi bien ban dieu chinh thay the");
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
			docFind = new Document("IssuerId", header.getIssuerId())
					.append("IsDelete", false)
					.append("_id", objectId)
					.append("SignStatusCode", Constants.INVOICE_SIGN_STATUS.NOSIGN)
					.append("Status", Constants.INVOICE_STATUS.CREATED);
			Document docTmp = null;
			filter = new Document("_id", 1);

			List<Document> pipeline = new ArrayList<Document>();
			pipeline.add(new Document("$match", docFind));
			pipeline.add(new Document("$project", filter));

			try (MongoClient mongoClient = cfg.mongoClient()) {
				MongoCollection<Document> collection = mongoClient.getDatabase(cfg.dbName).getCollection("EInvoiceBBDCTT");
				docTmp = collection.aggregate(pipeline).allowDiskUse(true).iterator().next();
			} catch (Exception e) {
				e.printStackTrace();
			}

			if (docTmp == null) {
				responseStatus = new MspResponseStatus(9999, "Không tìm thấy thông tin biên bản.");
				rsp.setResponseStatus(responseStatus);
				return rsp;
			}

			options = new FindOneAndUpdateOptions();
			options.upsert(false);
			options.maxTime(5000, TimeUnit.MILLISECONDS);
			options.returnDocument(ReturnDocument.AFTER);

			docUpdate = new Document();
			docUpdate.append("IsDelete", true)
			.append("InfoDeleted",
					new Document("DeletedDate", LocalDateTime.now())
					.append("DeletedUserID", header.getUserId())
					.append("DeletedUserName", header.getUserName())
					.append("DeletedUserFullName", header.getUserFullName()));

			try (MongoClient mongoClient = cfg.mongoClient()) {
				MongoCollection<Document> collection = mongoClient.getDatabase(cfg.dbName)
						.getCollection("EInvoiceBBDCTT");
				collection.findOneAndUpdate(docFind, new Document("$set", docUpdate), options);
			} catch (Exception e) {
				e.printStackTrace();
			}

			format_time = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
			time_dem = LocalDateTime.now();
			time = time_dem.format(format_time);
			name_company = removeAccent(header.getUserFullName());
			System.out.println(time + name_company + " Vua thay xóa bien ban dieu chinh thay the");
			responseStatus = new MspResponseStatus(0, "SUCCESS");
			rsp.setResponseStatus(responseStatus);
			return rsp;
		}
		
		return rsp;
	}
	
	private Document getDocuments(Document docFind, Document filter, Document docFind1, Document filter1, Document docFind2, Document filter2) {
		List<Document> pipeline = null;
		Document docTmp = null;

		pipeline = new ArrayList<Document>();
		pipeline.add(new Document("$match", docFind));
		pipeline.add(new Document("$project", filter));

		pipeline.add(new Document("$lookup",
				new Document("from", "EInvoice")
						.append("pipeline",
								Arrays.asList(new Document("$match", docFind1), new Document("$project", filter1)))
						.append("as", "EInvoice")));
		pipeline.add(
				new Document("$unwind", new Document("path", "$EInvoice").append("preserveNullAndEmptyArrays", true)));

		pipeline.add(new Document("$lookup",
				new Document("from", "EInvoicePXK")
						.append("pipeline",
								Arrays.asList(new Document("$match", docFind1), new Document("$project", filter1)))
						.append("as", "EInvoicePXK")));
		pipeline.add(new Document("$unwind",
				new Document("path", "$EInvoicePXK").append("preserveNullAndEmptyArrays", true)));
		pipeline.add(new Document("$lookup",
				new Document("from", "EInvoiceBH")
						.append("pipeline",
								Arrays.asList(new Document("$match", docFind1), new Document("$project", filter1)))
						.append("as", "EInvoiceBH")));
		pipeline.add(new Document("$unwind",
				new Document("path", "$EInvoiceBH").append("preserveNullAndEmptyArrays", true)));

		pipeline.add(new Document("$lookup",
				new Document("from", "EInvoicePXKDL")
						.append("pipeline",
								Arrays.asList(new Document("$match", docFind1), new Document("$project", filter1)))
						.append("as", "EInvoicePXKDL")));
		pipeline.add(new Document("$unwind",
				new Document("path", "$EInvoicePXKDL").append("preserveNullAndEmptyArrays", true)));

		pipeline.add(new Document("$lookup",
				new Document("from", "EInvoiceMTT")
						.append("pipeline",
								Arrays.asList(new Document("$match", docFind1), new Document("$project", filter1)))
						.append("as", "EInvoiceMTT")));
		pipeline.add(new Document("$unwind",
				new Document("path", "$EInvoiceMTT").append("preserveNullAndEmptyArrays", true)));

		if (docFind2 != null && filter2 != null) {
			pipeline.add(new Document("$lookup",
					new Document("from", "EInvoiceBBDCTT")
							.append("pipeline",
									Arrays.asList(new Document("$match", docFind2), new Document("$project", filter2)))
							.append("as", "EInvoiceBBDCTT")));
			pipeline.add(new Document("$unwind",
					new Document("path", "$EInvoiceBBDCTT").append("preserveNullAndEmptyArrays", true)));
		}
		
		try (MongoClient mongoClient = cfg.mongoClient()) {
			MongoCollection<Document> collection = mongoClient.getDatabase(cfg.dbName).getCollection("Issuer");
			docTmp = collection.aggregate(pipeline).allowDiskUse(true).iterator().next();
		} catch (Exception e) {

		}

		return docTmp;
	}

	@Override
	public MsgRsp list(JSONRoot jsonRoot) throws Exception {
		Msg msg = jsonRoot.getMsg();
		MsgHeader header = msg.getMsgHeader();
		MsgPage page = msg.getMsgPage();
		Object objData = msg.getObjData();
		
		MsgRsp rsp = new MsgRsp(header);
		rsp.setMsgPage(page);
		MspResponseStatus responseStatus = null;
		
		JsonNode jsonData = null;
		if(objData != null) {
			jsonData = Json.serializer().nodeFromObject(msg.getObjData());
		}else{
			throw new Exception("Lỗi dữ liệu đầu vào");
		}
		
		String status = commons.getTextJsonNode(jsonData.at("/Status")).replaceAll("\\s", "");
		String fromDate = commons.getTextJsonNode(jsonData.at("/FromDate")).replaceAll("\\s", "");
		String toDate = commons.getTextJsonNode(jsonData.at("/ToDate")).replaceAll("\\s", "");
		Document docMatchDate = null;
		
		Document docMatch = new Document("IssuerId", header.getIssuerId())
				.append("IsDelete", false);
		
		if(fromDate.length() > 0 || fromDate.length() > 0) {
			docMatchDate = new Document();
			if(fromDate.length() > 0)
				docMatchDate.append("$gte", formatDate(fromDate));
			if( fromDate.length() > 0)
				docMatchDate.append("$lte", formatDate(toDate));
			docMatch.append("NLap", docMatchDate);
		}

		if(!"".equals(status))
			docMatch.append("Status", commons.regexEscapeForMongoQuery(status));

		List<Document> pipeline = new ArrayList<Document>();
		pipeline.add(new Document("$match", docMatch));
		pipeline.add(
				new Document("$project", 
					new Document("Status", 1)
					.append("_id", 1)
					.append("Ten", 1)
					.append("SBBan", 1)
					.append("NLap", 1)
					.append("NDSai", 1)
					.append("NDDung", 1)
					.append("NDTNhat", 1)
					.append("SignStatusCode", 1)
					.append("ClientSignStatusCode", 1)
					.append("IsDelete", 1)
					.append("SecureKey", 1)
				)
			);
		page.setFieldSort("NLap");
		page.setTypeSort(-1);
		pipeline.addAll(createFacetForSearch(page));
		Document docTmp = null;
		try (MongoClient mongoClient = cfg.mongoClient()) {
			MongoCollection<Document> collection = mongoClient.getDatabase(cfg.dbName).getCollection("EInvoiceBBDCTT");
			docTmp = collection.aggregate(pipeline).allowDiskUse(true).iterator().next();	
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		if(null == docTmp) {
			responseStatus = new MspResponseStatus(9999, "Không tìm thấy dữ liệu.");
			rsp.setResponseStatus(responseStatus);
			return rsp;
		}
		
		page.setTotalRows(docTmp.getInteger("total", 0));
		rsp.setMsgPage(page);
		
		List<Document> rows = null;
		if(docTmp.get("data") != null && docTmp.get("data") instanceof List) {
			rows = docTmp.getList("data", Document.class);
		}
		
		ArrayList<HashMap<String, Object>> rowsReturn = new ArrayList<HashMap<String, Object>>();
		HashMap<String, Object> hItem = null;
		if(null != rows) {
			ObjectId objectId = null;
			for(Document doc: rows) {
				objectId = (ObjectId) doc.get("_id");
				hItem = new HashMap<String, Object>();
				hItem.put("_id", objectId.toString());
				hItem.put("Ten", doc.get("Ten"));
				hItem.put("SBBan", doc.get("SBBan"));
				hItem.put("NLap", doc.get("NLap"));
				hItem.put("NDSai", doc.get("NDSai"));
				hItem.put("NDDung", doc.get("NDDung"));
				hItem.put("NDTNhat", doc.get("NDTNhat"));
				hItem.put("SignStatusCode", doc.get("SignStatusCode"));
				hItem.put("ClientSignStatusCode", doc.get("ClientSignStatusCode"));
				hItem.put("Status", doc.get("Status"));
				hItem.put("IsDelete", doc.get("IsDelete"));
				hItem.put("SecureKey", doc.get("SecureKey"));
				rowsReturn.add(hItem);
			}
		}
		rsp.setObjData(rowsReturn);
		
		DateTimeFormatter format_time = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
		LocalDateTime time_dem  = LocalDateTime.now();
		String time = time_dem.format(format_time);
		String name_company = removeAccent(header.getUserFullName());
		System.out.println(time +name_company+" Vua search bien ban dieu chinh thay the");
		responseStatus = new MspResponseStatus(0, "SUCCESS");
		rsp.setResponseStatus(responseStatus);
		return rsp;
	}
	private String formatDate(String input) {
		DateTimeFormatter inputFormatter = DateTimeFormatter.ofPattern(Constants.FORMAT_DATE.FORMAT_DATE_WEB);
        DateTimeFormatter outputFormatter = DateTimeFormatter.ofPattern(Constants.FORMAT_DATE.FORMAT_DATE_EINVOICE);
        LocalDate date = LocalDate.parse(input, inputFormatter);
        return date.format(outputFormatter);
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
				.append("IsDelete", false).append("_id", objectId);
		Document docTmp = null;
		try (MongoClient mongoClient = cfg.mongoClient();){
			MongoCollection<Document> collection = mongoClient.getDatabase(cfg.dbName).getCollection("EInvoiceBBDCTT");
			docTmp = collection.find(docFind).allowDiskUse(true).iterator().next();	
		} catch (Exception e) {
		}
		
		if (docTmp == null) {
			responseStatus = new MspResponseStatus(9999, Constants.MAP_ERROR.get(9999));
			rsp.setResponseStatus(responseStatus);
			return rsp;
		}
		
		DateTimeFormatter format_time = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
		LocalDateTime time_dem  = LocalDateTime.now();
		String time = time_dem.format(format_time); 
		
		String name_company = removeAccent(header.getUserFullName());
		System.out.println(time +name_company+" Vua xem chi tiet bien ban dieu chinh thay the");
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
				.append("IsDelete", false).append("Status", Constants.INVOICE_STATUS.CREATED)
				.append("SignStatusCode", Constants.INVOICE_SIGN_STATUS.NOSIGN);

		List<Document> pipeline = new ArrayList<Document>();
		pipeline.add(new Document("$match", docFind));

		Document docTmp = null;

		MongoClient mongoClient = cfg.mongoClient();
		MongoCollection<Document> collection = mongoClient.getDatabase(cfg.dbName).getCollection("EInvoiceBBDCTT");

		try {
			docTmp = collection.aggregate(pipeline).allowDiskUse(true).iterator().next();
		} catch (Exception e) {

		}

		mongoClient.close();

		if (null == docTmp) {
			return fileInfo;
		}

		String dir = docTmp.get("Dir", "");
		String fileName = docTmp.get("FileNameXML", "");
		File file = new File(dir, fileName);

		if (!file.exists())
			return fileInfo;

		fileInfo.setFileName(fileName);
		fileInfo.setContentFile(commons.getBytesDataFromFile(file));
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

		/* DOC NOI DUNG XML DA KY */
		org.w3c.dom.Document xmlDoc = commons.inputStreamToDocument(is, true);

		ObjectId objectId = null;
		try {
			objectId = new ObjectId(_id);
		} catch (Exception e) {
		}

		Document docFind = new Document("IssuerId", header.getIssuerId()).append("_id", objectId)
				.append("IsDelete", false).append("Status", Constants.INVOICE_STATUS.CREATED)
				.append("SignStatusCode", Constants.INVOICE_SIGN_STATUS.NOSIGN);

		Document docTmp = null;
		try (MongoClient mongoClient = cfg.mongoClient()){
			MongoCollection<Document> collection = mongoClient.getDatabase(cfg.dbName).getCollection("EInvoiceBBDCTT");
			docTmp = collection.find(docFind).allowDiskUse(true).iterator().next();
		} catch (Exception e) {
		}

		if (null == docTmp) {
			responseStatus = new MspResponseStatus(9999, "Không tìm thấy biên bản điều chỉnh thay thế.");
			rsp.setResponseStatus(responseStatus);
			return rsp;
		}
		String dir = docTmp.get("Dir", "");
		String fileName = _id + "_signed.xml";
		boolean check = commons.docW3cToFile(xmlDoc, dir, fileName);
		if (!check) {
			responseStatus = new MspResponseStatus(9999, "Lưu tập tin đã ký không thành công.");
			rsp.setResponseStatus(responseStatus);
			return rsp;
		}
		String uuid = UUID.randomUUID().toString().replaceAll("-", "").toUpperCase();
		String MTDiep = SystemParams.MSTTCGP
				+ commons.convertLocalDateTimeToString(LocalDateTime.now(), "yyyyMMddHHmmssSSS")
				+ uuid.substring(0, 19);

		FindOneAndUpdateOptions options = new FindOneAndUpdateOptions();
		options.upsert(false);
		options.maxTime(5000, TimeUnit.MILLISECONDS);
		options.returnDocument(ReturnDocument.AFTER);

		try (MongoClient mongoClient = cfg.mongoClient()){
			MongoCollection<Document> collection = mongoClient.getDatabase(cfg.dbName).getCollection("EInvoiceBBDCTT");
			docTmp = collection.find(docFind).allowDiskUse(true).iterator().next();
			collection.findOneAndUpdate(docFind,
					new Document("$set",
							new Document("SignStatusCode", Constants.INVOICE_SIGN_STATUS.SIGNED)
									.append("Status", Constants.INVOICE_STATUS.PENDING)
									.append("MTDiep", MTDiep)
									.append("InfoSigned",
											new Document("SignedDate", LocalDateTime.now())
													.append("SignedUserID", header.getUserId())
													.append("SignedUserName", header.getUserName())
													.append("SignedUserFullName", header.getUserFullName()))),
					options);
		} catch (Exception e) {
		}

		DateTimeFormatter format_time = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
		LocalDateTime time_dem = LocalDateTime.now();
		String time = time_dem.format(format_time);
		String name_company = removeAccent(header.getUserFullName());
		System.out.println(time + name_company + " Vua ky thanh cong bien ban dieu chinh thay the");
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
		String _content = commons.getTextJsonNode(jsonData.at("/_content")).trim().replaceAll("\\s+", " ");

		MsgRsp rsp = new MsgRsp(header);
		rsp.setMsgPage(page);
		MspResponseStatus responseStatus = null;

		ObjectId objectId = null;
		try {
			objectId = new ObjectId(_id);
		} catch (Exception e) {
		}

		Document docTmp = null;
		Document docFind = new Document("IssuerId", header.getIssuerId()).append("_id", objectId);

		List<Document> pipeline = new ArrayList<Document>();
		pipeline.add(new Document("$match", docFind));

		pipeline.add(new Document("$lookup", new Document("from", "ConfigEmail")
				.append("let", new Document("vIssuerId", "$IssuerId"))
				.append("pipeline",
						Arrays.asList(new Document("$match",
								new Document("$expr", new Document("$eq", Arrays.asList("$IssuerId", "$$vIssuerId"))))))
				.append("as", "ConfigEmail")));
		pipeline.add(new Document("$unwind",
				new Document("path", "$ConfigEmail").append("preserveNullAndEmptyArrays", true)));

		pipeline.add(new Document("$lookup",
				new Document("from", "ConfigMailJet")
						.append("pipeline", Arrays.asList(new Document("$match", new Document("IsActive", true))))
						.append("as", "ConfigMailJet")));
		pipeline.add(new Document("$unwind",
				new Document("path", "$ConfigMailJet").append("preserveNullAndEmptyArrays", true)));

		pipeline.add(new Document("$lookup", new Document("from", "DMFooterWeb")
				.append("pipeline",
						Arrays.asList(new Document("$match", new Document("IsActive", true).append("IsDelete", false)),
								new Document("$project", new Document("Noidung", 1)), new Document("$limit", 1)))
				.append("as", "DMFooterWeb")));
		pipeline.add(new Document("$unwind",
				new Document("path", "$DMFooterWeb").append("preserveNullAndEmptyArrays", true)));

		try (MongoClient mongoClient = cfg.mongoClient()) {
			MongoCollection<Document> collection = mongoClient.getDatabase(cfg.dbName).getCollection("EInvoiceBBDCTT");
			docTmp = collection.aggregate(pipeline).allowDiskUse(true).iterator().next();
		} catch (Exception e) {

		}

		if (null == docTmp) {
			responseStatus = new MspResponseStatus(9999, "Không tìm thấy thông tin hóa đơn.");
			rsp.setResponseStatus(responseStatus);
			return rsp;
		}

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
		String signStatusCode = docTmp.get("SignStatusCode", "");
		String status = docTmp.get("Status", "");
		String mtdiep = docTmp.get("MTDiep", "");
		String loai = docTmp.get("Loai", "");
		String fileName = _id + ".xml";
		String fileNamePDF = _id + ".pdf";
		String dir = docTmp.get("Dir", "");
		if ("SIGNED".equals(signStatusCode)) {
			fileName = _id + "_signed.xml";
			if ("COMPLETE".equals(status)) {
				fileName = _id + "_" + mtdiep + "_signed.xml";
			}
		}

		File file = new File(dir, fileName);
		if (file.exists() && file.isFile()) {
			org.w3c.dom.Document doc = commons.fileToDocument(file);
			String fileNameJP = "BIEN-BAN-DIEU-CHINH-THAY-THE.jrxml";
			File fileJP = new File(SystemParams.DIR_E_INVOICE_TEMPLATE, fileNameJP);
			ByteArrayOutputStream baosPDF = this.jpUtils.printbb(fileJP, doc, "1".equals(loai));
			/* LUU TAP TIN PDF */
			if (null != baosPDF) {
				try (OutputStream fileOuputStream = new FileOutputStream(new File(dir, fileNamePDF))) {
					baosPDF.writeTo(fileOuputStream);
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}

		List<String> listFiles = new ArrayList<>();
		List<String> listNames = new ArrayList<>();
		file = new File(dir, fileNamePDF);
		if (file.exists() && file.isFile()) {
			listFiles.add(file.toString());
			listNames.add("bien-ban-dieu-chinh-thay-the-hoa-don-sai-sot.pdf");
		}
		boolean boo = false;
		MailConfig mailConfig = new MailConfig(docTmp.get("ConfigEmail", Document.class));
		mailConfig.setNameSend(docTmp.getEmbedded(Arrays.asList("EInvoiceDetail", "NDHDon", "NBan", "Ten"), ""));

		if (_email != "") {
			// KIỂM TRA GỬI MAIL THƯỜNG HAY MAILJET
			if (MailJet.equals("Y") && !MailJet.equals("") && !MailJet.equals("N")) {
				//
				String ApiKey = docTmp.getEmbedded(Arrays.asList("ConfigMailJet", "ApiKey"), "");
				String SecretKey = docTmp.getEmbedded(Arrays.asList("ConfigMailJet", "SecretKey"), "");
				String EmailAddress = docTmp.getEmbedded(Arrays.asList("ConfigMailJet", "EmailAddress"), "");
				mailConfig.setEmailAddress(ApiKey);
				mailConfig.setEmailPassword(SecretKey);
				mailConfig.setSmtpServer(EmailAddress);
				boo = mailJet.sendMailJet(mailConfig, _title, _content, _email, listFiles, listNames, true);
			} else {
				boo = mailUtils.sendMail(mailConfig, _title, _content, _email, listFiles, listNames, true);
			}
			try (MongoClient mongoClient = cfg.mongoClient()) {

				MongoCollection<Document> collection = mongoClient.getDatabase(cfg.dbName)
						.getCollection("LogEmailUser");
				collection.insertOne(new Document("IssuerId", header.getIssuerId()).append("Title", _title)
						.append("Email", _email).append("IsActive", boo).append("MailCheck", boo)
						.append("IsDelete", false).append("EmailContent", _content));

				if (boo) {
					FindOneAndUpdateOptions options = new FindOneAndUpdateOptions();
					options.upsert(false);
					options.maxTime(5000, TimeUnit.MILLISECONDS);
					options.returnDocument(ReturnDocument.AFTER);
					MongoCollection<Document> collection1 = mongoClient.getDatabase(cfg.dbName)
							.getCollection("EInvoiceBBDCTT");
					docTmp = collection1.find(docFind).allowDiskUse(true).iterator().next();
					collection1.findOneAndUpdate(docFind,
							new Document("$set",
									new Document("Status", Constants.INVOICE_STATUS.PROCESSING)
									.append("InfoSendMail",
											new Document("SignedDate", LocalDateTime.now())
													.append("SignedUserID", header.getUserId())
													.append("SignedUserName", header.getUserName())
													.append("SignedUserFullName", header.getUserFullName()))),
							options);
				}
			} catch (Exception ex) {
			}
		}

		responseStatus = new MspResponseStatus(0, "SUCCESS");
		rsp.setResponseStatus(responseStatus);
		return rsp;
	}
}
