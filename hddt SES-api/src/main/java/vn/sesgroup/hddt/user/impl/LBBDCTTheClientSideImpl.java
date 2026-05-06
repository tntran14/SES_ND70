package vn.sesgroup.hddt.user.impl;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Paths;
import java.security.Principal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilderFactory;
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
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.FindOneAndUpdateOptions;
import com.mongodb.client.model.ReturnDocument;

import vn.sesgroup.hddt.configuration.ConfigConnectMongo;
import vn.sesgroup.hddt.dto.FileInfo;
import vn.sesgroup.hddt.dto.MailConfig;
import vn.sesgroup.hddt.dto.SignTypeInfo;
import vn.sesgroup.hddt.user.dao.AbstractDAO;
import vn.sesgroup.hddt.user.dao.LBBDCTTheClientSideDAO;
import vn.sesgroup.hddt.user.service.JPUtils;
import vn.sesgroup.hddt.utility.Json;
import vn.sesgroup.hddt.utility.MailjetSender;
import vn.sesgroup.hddt.utility.SystemParams;
import vn.sesgroup.hddt.utility.Constants;

@Repository
@Transactional
public class LBBDCTTheClientSideImpl extends AbstractDAO implements LBBDCTTheClientSideDAO {
	@Autowired
	ConfigConnectMongo cfg;
	@Autowired
	JPUtils jpUtils;

	private MailjetSender mailJet = new MailjetSender();

	@Override
	public MsgRsp detail(JSONRoot jsonRoot) throws Exception {
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

		String secureKey = commons.getTextJsonNode(jsonData.at("/secureKey")).replaceAll("\\s", "");
		String mtdiep = commons.getTextJsonNode(jsonData.at("/mtdiep")).replaceAll("\\s+", "");
		Document docFind = new Document("SecureKey", secureKey)
				.append("MTDiep", mtdiep)
				.append("IsDelete", false)
				.append("SignStatusCode", Constants.INVOICE_SIGN_STATUS.SIGNED)
				.append("Status", new Document("$in", 
						Arrays.asList(Constants.INVOICE_STATUS.PENDING,
						Constants.INVOICE_STATUS.PROCESSING, 
						Constants.INVOICE_STATUS.COMPLETE))
						);
		Document docTmp = null;
		try (MongoClient mongoClient = cfg.mongoClient();) {
			MongoCollection<Document> collection = mongoClient.getDatabase(cfg.dbName).getCollection("EInvoiceBBDCTT");
			docTmp = collection.find(docFind).allowDiskUse(true).iterator().next();
		} catch (Exception e) {
		}

		if (docTmp == null) {
			responseStatus = new MspResponseStatus(9999, Constants.MAP_ERROR.get(9999));
			rsp.setResponseStatus(responseStatus);
			return rsp;
		}

		HashMap<String, Object> hdata = new HashMap<String, Object>();
		ObjectId objectId = (ObjectId) docTmp.get("_id");
		hdata.put("_id", objectId.toString());
		hdata.put("ClientSignStatusCode", docTmp.get("ClientSignStatusCode"));
		hdata.put("Ten", docTmp.get("Ten"));
		hdata.put("SBBan", docTmp.get("SBBan"));
		hdata.put("NLap", docTmp.get("NLap"));
		hdata.put("NDSai", docTmp.get("NDSai"));
		rsp.setObjData(hdata);
		responseStatus = new MspResponseStatus(0, Constants.MAP_ERROR.get(0));
		rsp.setResponseStatus(responseStatus);
		return rsp;
	}

	@Override
	public FileInfo getFileForSign(JSONRoot jsonRoot) throws Exception {
		FileInfo fileInfo = new FileInfo();
		Msg msg = jsonRoot.getMsg();
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

		Document docFind = new Document("_id", objectId).append("IsDelete", false)
				.append("Status", new Document("$in", Arrays.asList(Constants.INVOICE_STATUS.PENDING, Constants.INVOICE_STATUS.PROCESSING)))
				.append("SignStatusCode", Constants.INVOICE_SIGN_STATUS.SIGNED)
				.append("ClientSignStatusCode", Constants.INVOICE_SIGN_STATUS.NOSIGN);

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
		String fileName = _id + "_signed.xml";
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

		Document docFind = new Document("_id", objectId).append("IsDelete", false)
				.append("Status",new Document("$in", Arrays.asList(Constants.INVOICE_STATUS.PENDING, Constants.INVOICE_STATUS.PROCESSING)))
				.append("SignStatusCode", Constants.INVOICE_SIGN_STATUS.SIGNED)
				.append("ClientSignStatusCode", Constants.INVOICE_SIGN_STATUS.NOSIGN);

		Document docTmp = null;
		try (MongoClient mongoClient = cfg.mongoClient()) {
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
		String mtdiep = docTmp.get("MTDiep", "");
		String fileName = _id + "_" + mtdiep + "_signed.xml";
		boolean check = commons.docW3cToFile(xmlDoc, dir, fileName);
		if (!check) {
			responseStatus = new MspResponseStatus(9999, "Lưu tập tin đã ký không thành công.");
			rsp.setResponseStatus(responseStatus);
			return rsp;
		}

		FindOneAndUpdateOptions options = new FindOneAndUpdateOptions();
		options.upsert(false);
		options.maxTime(5000, TimeUnit.MILLISECONDS);
		options.returnDocument(ReturnDocument.AFTER);

		try (MongoClient mongoClient = cfg.mongoClient()) {
			MongoCollection<Document> collection = mongoClient.getDatabase(cfg.dbName).getCollection("EInvoiceBBDCTT");
			docTmp = collection.find(docFind).allowDiskUse(true).iterator().next();
			collection.findOneAndUpdate(docFind,
					new Document("$set",
							new Document("ClientSignStatusCode", Constants.INVOICE_SIGN_STATUS.SIGNED)
									.append("Status", Constants.INVOICE_STATUS.COMPLETE)
									.append("ClientInfoSigned", new Document("SignedDate", LocalDateTime.now()))),
					options);
		} catch (Exception e) {
		}
		
		org.w3c.dom.Document doc = commons.fileToDocument(new File(dir, fileName), false);

		XPath xPath = XPathFactory.newInstance().newXPath();
		Node nodeDSCKS = (Node) xPath.evaluate("/BBDCTThe/DSCKS/NNT", doc, XPathConstants.NODE);
		Node nodeSignature = null;
		if (null != nodeDSCKS)
			nodeSignature = (Node) xPath.evaluate("Signature", nodeDSCKS, XPathConstants.NODE);
		if (null != nodeSignature) {
			String x509Certificate = commons.getTextFromNodeXML(
					(Element) xPath.evaluate("KeyInfo/X509Data/X509Certificate", nodeSignature, XPathConstants.NODE));
			byte[] certBytes = Base64.getDecoder().decode(x509Certificate);
			java.security.cert.CertificateFactory cf = java.security.cert.CertificateFactory.getInstance("X.509");
			java.security.cert.X509Certificate cert = (java.security.cert.X509Certificate) cf
					.generateCertificate(new java.io.ByteArrayInputStream(certBytes));

			String serialNumber = cert.getSerialNumber().toString(16);
			docFind = new Document("SerialNumber", serialNumber);
			Document existDoc = null;
			try (MongoClient mongoClient = cfg.mongoClient()) {
				MongoCollection<Document> collection = mongoClient.getDatabase(cfg.dbName)
						.getCollection("ClientSignatureInfo");
				existDoc = collection.find(docFind).first();
			} catch (Exception e) {
			}

			if (existDoc == null) {
				String version = String.valueOf(cert.getVersion());
				String sigAlgName = cert.getSigAlgName();
				String issuerDN = cert.getIssuerDN().toString();
				String subjectDN = cert.getSubjectDN().toString();
				LocalDate validFrom = commons.dateToLocalDate(cert.getNotBefore());
				LocalDate validTo = commons.dateToLocalDate(cert.getNotAfter());

				Document docInsert = new Document("SerialNumber", serialNumber)
										.append("Version", version)
										.append("SigAlgName", sigAlgName)
										.append("IssuerDN", issuerDN)
										.append("SubjectDN", subjectDN)
										.append("ValidFrom", validFrom)
										.append("ValidTo", validTo)
										.append("ClientOf", 
												new Document("UserID", docTmp.getEmbedded(Arrays.asList("InfoCreated", "CreateUserID"),""))
												.append("UserName",  docTmp.getEmbedded(Arrays.asList("InfoCreated", "CreateUserName"),""))
												.append("UserFullName",  docTmp.getEmbedded(Arrays.asList("InfoCreated", "CreateUserFullName"),""))
												)
										.append("InfoCreated", new Document("CreateDate", LocalDateTime.now()));

				try (MongoClient mongoClient = cfg.mongoClient()) {
					MongoCollection<Document> collection = mongoClient.getDatabase(cfg.dbName)
							.getCollection("ClientSignatureInfo");
					collection.insertOne(docInsert);
				} catch (Exception e) {
				}

			}
		}
		
		responseStatus = new MspResponseStatus(0, Constants.MAP_ERROR.get(0));
		rsp.setResponseStatus(responseStatus);
		return rsp;
	}

	@Override
	public FileInfo printbb(JSONRoot jsonRoot) throws Exception {
		FileInfo fileInfo = new FileInfo();
		Msg msg = jsonRoot.getMsg();
		MsgHeader header = msg.getMsgHeader();
		Object objData = msg.getObjData();

		if (objData == null) {
			return new FileInfo();
		}

		JsonNode jsonData = Json.serializer().nodeFromObject(msg.getObjData());
		String _id = this.commons.getTextJsonNode(jsonData.at("/_id")).replaceAll("\\s", "");

		ObjectId objectId = null;
		ObjectId objectIdIssu = null;
		try {
			objectId = new ObjectId(_id);
			objectIdIssu = new ObjectId(header.getIssuerId());
		} catch (Exception var29) {
		}

		List<Document> pipeline = new ArrayList<>();
		pipeline.add(
				new Document("$match", (new Document("_id", objectId)).append("IsDelete", new Document("$ne", true))));
		Document docTmp = null;
		try (MongoClient mongoClient = cfg.mongoClient()) {
			MongoCollection<Document> collection = mongoClient.getDatabase(cfg.dbName).getCollection("EInvoiceBBDCTT");
			docTmp = collection.aggregate(pipeline).allowDiskUse(true).iterator().next();
		} catch (Exception e) {
		}
		if (docTmp == null) {
			return fileInfo;
		}

		String signStatusCode = docTmp.get("SignStatusCode", "");
		String status = docTmp.get("Status", "");
		String mtdiep = docTmp.get("MTDiep", "");
		String loai = docTmp.get("Loai", "");
		String fileName = _id + ".xml";
		String dir = docTmp.get("Dir", "");
		boolean isMultiInvoice = docTmp.getBoolean("IsMultiInvoice", false);
		if ("SIGNED".equals(signStatusCode)) {
			fileName = _id + "_signed.xml";
			if ("COMPLETE".equals(status)) {
				fileName = _id + "_" + mtdiep + "_signed.xml";
			}
		}
		
		String fileNameJP = isMultiInvoice ? "BIEN-BAN-DIEU-CHINH-THAY-THE-MULTI.jrxml" : "BIEN-BAN-DIEU-CHINH-THAY-THE.jrxml";
		List<Document> invoicesReturn = new ArrayList<Document>();
		if (isMultiInvoice && 
				Constants.TAXCODE_USING_RETURN_REPORT.contains(docTmp.getEmbedded(Arrays.asList("TTNBan", "MSThue"), ""))) {
			fileNameJP ="BIEN-BAN-DIEU-CHINH-THAY-THE-MULTI-101.jrxml";

			List<ObjectId> ids = new ArrayList<ObjectId>();
			List<Document> dshDon = docTmp.getList("DSHDon", Document.class);
			for (Document hdPair: dshDon) {
					String id = hdPair.getEmbedded(Arrays.asList("HDDCTThe","_id"),"");
					ids.add(new ObjectId(id));		
			}
			
			Document docFind = new Document("_id", new Document("$in", ids)).append("IsDelete", false).append("IssuerId", docTmp.get("IssuerId", ""));
			Document projection = new Document("EInvoiceDetail.DSHHDVu", 1).append("EInvoiceDetail.TTChung", 1);
			docTmp = null;
			try (MongoClient mongoClient = cfg.mongoClient()) {
				MongoDatabase database = mongoClient.getDatabase(cfg.dbName);
				Iterable<Document> cursor = database.getCollection("EInvoice").find(docFind).projection(projection).allowDiskUse(true);
				Iterator<Document> iter = cursor.iterator();
				while (iter.hasNext()) {
					invoicesReturn.add(iter.next());
				}
				if (invoicesReturn.size() == 0) {
					cursor = database.getCollection("EInvoice").find(docFind).allowDiskUse(true);
					iter = cursor.iterator();
					while (iter.hasNext()) {
						invoicesReturn.add(iter.next());
					}
				}
				if (invoicesReturn.size() == 0) {
					cursor = database.getCollection("EInvoiceBH").find(docFind).allowDiskUse(true);
					iter = cursor.iterator();
					while (iter.hasNext()) {
						invoicesReturn.add(iter.next());
					}
				}
				if (invoicesReturn.size() == 0) {
					cursor = database.getCollection("EInvoiceMTT").find(docFind).allowDiskUse(true);
					iter = cursor.iterator();
					while (iter.hasNext()) {
						invoicesReturn.add(iter.next());
					}
				}
			} catch (Exception e) {
			}
		}

		File file = new File(dir, fileName);
		if (file.exists() && file.isFile()) {
			org.w3c.dom.Document doc = this.commons.fileToDocument(file);
			File fileJP = new File(SystemParams.DIR_E_INVOICE_TEMPLATE, fileNameJP);
			ByteArrayOutputStream baosPDF = isMultiInvoice
					? this.jpUtils.printbb_multi(fileJP, doc, "1".equals(loai), invoicesReturn)
					: this.jpUtils.printbb(fileJP, doc, "1".equals(loai));
			fileInfo.setFileName("printbb.pdf");
			fileInfo.setContentFile(baosPDF.toByteArray());
			return fileInfo;
		} else {
			return new FileInfo();
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

		MsgRsp rsp = new MsgRsp(header);
		rsp.setMsgPage(page);
		MspResponseStatus responseStatus = null;

		ObjectId objectId = null;
		try {
			objectId = new ObjectId(_id);
		} catch (Exception e) {
		}

		Document docTmp = null;
		Document docFind = new Document("_id", objectId);

		List<Document> pipeline = new ArrayList<Document>();
		pipeline.add(new Document("$match", docFind));

		pipeline.add(new Document("$lookup",
				new Document("from", "ConfigMailJet")
						.append("pipeline", Arrays.asList(new Document("$match", new Document("IsActive", true))))
						.append("as", "ConfigMailJet")));
		pipeline.add(new Document("$unwind",
				new Document("path", "$ConfigMailJet").append("preserveNullAndEmptyArrays", true)));

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

		if (docTmp.get("ConfigMailJet") == null) {
			responseStatus = new MspResponseStatus(9999, "Không tìm thấy thông tin email gửi.");
			rsp.setResponseStatus(responseStatus);
			return rsp;
		}

		StringBuilder title = new StringBuilder();
		String dvmh = docTmp.getEmbedded(Arrays.asList("TTNMua", "DVMHang"), "");
		String nmTaxCode = docTmp.getEmbedded(Arrays.asList("TTNMua", "MSThue"), "");
		String khmshdon = docTmp.getEmbedded(Arrays.asList("HDSSot", "KHMSHDon"), "");
		String khhdon = docTmp.getEmbedded(Arrays.asList("HDSSot", "KHHDon"), "");
		String mccqt = docTmp.getEmbedded(Arrays.asList("HDSSot", "MCCQT"), "");
		String nlap = commons.convertLocalDateTimeToString(
				commons.convertDateToLocalDateTime(docTmp.getEmbedded(Arrays.asList("HDSSot", "NLap"), Date.class)),
				"dd/MM/yyyy");
		int shd = docTmp.getEmbedded(Arrays.asList("HDSSot", "SHDon"), 0);
		title.append(nmTaxCode);
		title.append(" ");
		title.append(dvmh);
		title.append(" Thông báo đã ký biên bản điều chỉnh thay thế hóa đơn điện tử có sai sót");
		title.append(" - Số HĐ ");
		title.append(shd);
		title.append(" (No reply)");

		String email = docTmp.getEmbedded(Arrays.asList("TTNBan", "EReceive"), "");
		StringBuilder content = new StringBuilder();
		content.setLength(0);
		content.append(
				"<p><span style='font-family: Times New Roman;font-size: 13px;'>Kính gửi: <label style='font-weight: bold;'>"
						+ ("".equals(dvmh) ? "Quý bán hàng" : dvmh) + "</label><o:p></o:p></span></p>\n");
		content.append("<p><span style='font-family: Times New Roman;font-size: 13px;'>" + dvmh
				+ " xin thông báo về việc ĐÃ KÝ biên bản điều chỉnh thay thế hóa đơn điện tử có sai sót</span></p>\n");
		content.append(
				"<p><span style='font-family: Times New Roman;font-size: 13px;'><label style='font-weight: bold;'>Thông tin hóa đơn có sai sót:</label></span></p>\n");

		content.append("<p><span style='font-family: Times New Roman;font-size: 13px;'>1.  Số hóa đơn:  " + shd
				+ "</span></p>\n");

		content.append("<p><span style='font-family: Times New Roman;font-size: 13px;'>2.  Mẫu hoá đơn: " + khmshdon
				+ khhdon + "</span></p>\n");

		content.append("<p><span style='font-family: Times New Roman;font-size: 13px;'>3.  Mã của CƠ QUAN THUẾ: "
				+ mccqt + " </span></p>\n");

		content.append("<p><span style='font-family: Times New Roman;font-size: 13px;'>5.  Ngày lập: " + nlap
				+ "</span></p>\n");

		content.append(
				"<p><span style='font-family: Times New Roman;font-size: 13px;'>Trân trọng kính chào!</span></p>");
		content.append("<hr style='margin: 5px 0 5px 0;'>");
		content.append(
				"<p style='margin-bottom: 3px;'><span style='font-family: Times New Roman;font-size: 13px;color:red;font-weight: bold;'>VUI LÒNG KHÔNG REPLY EMAIL NÀY!</span></p>");

		String signStatusCode = docTmp.get("SignStatusCode", "");
		String status = docTmp.get("Status", "");
		String mtdiep = docTmp.get("MTDiep", "");
		String loai = docTmp.get("Loai", "");
		String fileName = _id + ".xml";
		String fileNamePDF = _id + "client.pdf";
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
		String ApiKey = docTmp.getEmbedded(Arrays.asList("ConfigMailJet", "ApiKey"), "");
		String SecretKey = docTmp.getEmbedded(Arrays.asList("ConfigMailJet", "SecretKey"), "");
		String EmailAddress = docTmp.getEmbedded(Arrays.asList("ConfigMailJet", "EmailAddress"), "");

		MailConfig mailConfig = new MailConfig();
		mailConfig.setNameSend(dvmh);
		mailConfig.setEmailAddress(ApiKey);
		mailConfig.setEmailPassword(SecretKey);
		mailConfig.setSmtpServer(EmailAddress);

		boo = mailJet.sendMailJet(mailConfig, title.toString(), content.toString(), email, listFiles, listNames, true);

		try (MongoClient mongoClient = cfg.mongoClient()) {
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
								new Document("InfoSendMail", new Document("SendedDate", LocalDateTime.now()))),
						options);
			}
		} catch (Exception ex) {
		}

		responseStatus = new MspResponseStatus(0, "SUCCESS");
		rsp.setResponseStatus(responseStatus);
		return rsp;
	}
}
