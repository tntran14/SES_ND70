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
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import javax.xml.parsers.DocumentBuilder;
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
import com.mongodb.client.model.BulkWriteOptions;
import com.mongodb.client.model.FindOneAndUpdateOptions;
import com.mongodb.client.model.ReturnDocument;
import com.mongodb.client.model.UpdateOneModel;
import com.mongodb.client.model.UpdateOptions;
import com.mongodb.client.model.WriteModel;

import vn.sesgroup.hddt.configuration.ConfigConnectMongo;
import vn.sesgroup.hddt.dto.FileInfo;
import vn.sesgroup.hddt.dto.MailConfig;
import vn.sesgroup.hddt.user.dao.AbstractDAO;
import vn.sesgroup.hddt.user.dao.TBHDSSotMTTDAO;
import vn.sesgroup.hddt.user.service.JPUtils;
import vn.sesgroup.hddt.user.service.TCTNService;
import vn.sesgroup.hddt.utility.Constants;
import vn.sesgroup.hddt.utility.Json;
import vn.sesgroup.hddt.utility.MailUtils;
import vn.sesgroup.hddt.utility.MailjetSender;
import vn.sesgroup.hddt.utility.SystemParams;

@Repository
@Transactional
public class TBHDSSotMTTImpl extends AbstractDAO implements TBHDSSotMTTDAO{

	@Autowired ConfigConnectMongo cfg;
	@Autowired TCTNService tctnService;
	private MailUtils mailUtils = new MailUtils();
	
	private MailjetSender mailJet = new MailjetSender();
	@Autowired
	JPUtils jpUtils;

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
	
		
		String actionCode = header.getActionCode();
		String _id = commons.getTextJsonNode(jsonData.at("/_id")).replaceAll("\\s", "");
		String tinhThanh = commons.getTextJsonNode(jsonData.at("/TinhThanh")).replaceAll("\\s", "");
		String coQuanThue = commons.getTextJsonNode(jsonData.at("/CoQuanThue")).replaceAll("\\s", "");
		String loaiThongBao = commons.getTextJsonNode(jsonData.at("/LoaiThongBao")).replaceAll("\\s", "");
		String soTBcuaCQT = commons.getTextJsonNode(jsonData.at("/SoTBcuaCQT")).replaceAll("\\s", "");
		String ngayTBcuaCQT = commons.getTextJsonNode(jsonData.at("/NgayTBcuaCQT")).replaceAll("\\s", "");
		JsonNode jsonNodeDSHDon = jsonData.at("/DSHDon");

		HashMap<String, HashMap<String, String>> hEInvoice = new HashMap<String, HashMap<String,String>>();
		HashMap<String, String> hItem = null;
		HashMap<String, String> hItem02 = null;
		List<String> listMCCQT = new ArrayList<String>();
		int dem = 0;
		for(JsonNode o: jsonNodeDSHDon) {
			listMCCQT.add(commons.getTextJsonNode(o.at("/MCQTCap")));
			
			hItem = new HashMap<String, String>();
			hItem.put("TCTBao", commons.getTextJsonNode(o.at("/TCTBao")));
			hItem.put("LDo", commons.getTextJsonNode(o.at("/LDo")));
			hEInvoice.put(commons.getTextJsonNode(o.at("/MCQTCap")), hItem);
		}
		List<Object> listDSHDon = new ArrayList<Object>();
		
		MsgRsp rsp = new MsgRsp(header);
		rsp.setMsgPage(page);
		MspResponseStatus responseStatus = null;
		
		DocumentBuilderFactory dbf = null;
		DocumentBuilder db = null;
		org.w3c.dom.Document doc = null;
		Element root = null;
		Element elementContent = null;
		Element elementSubContent = null;
		Element elementTmp = null;
		
		String fileNameXML = "";
		String taxCode = "";
		String pathDir = "";
		Path path = null;
		File file = null;
		
		int stt = 0;
		ObjectId objectId = null;
		
		Document docFind = null;
		Document docTmp = null;
		Document docUpsert = null;
		Iterable<Document> cursor = null;
		Iterator<Document> iter = null;
		
		FindOneAndUpdateOptions options = null;
		List<Document> pipeline = null;
		boolean isSaveFile = false;
		String MTDiep = "";
		String MST = "";
		
		switch (actionCode) {
		
		case Constants.MSG_ACTION_CODE.CREATED:

			objectId = null;
			try {
				objectId = new ObjectId(header.getIssuerId());
			}catch(Exception e) {}
			
			pipeline = new ArrayList<Document>();
			pipeline.add(
				new Document("$match", 
					new Document("_id", objectId).append("IsActive", true).append("IsDelete", new Document("$ne", true))
				)
			);
			pipeline.add(
				new Document("$lookup", 
					new Document("from", "DMTinhThanh")
					.append("pipeline", 
						Arrays.asList(
							new Document("$match", new Document("code", tinhThanh)),
							new Document("$project", new Document("_id", 0))
						)
					)
					.append("as", "TinhThanhInfo")
				)
			);
			pipeline.add(new Document("$unwind", new Document("path", "$TinhThanhInfo").append("preserveNullAndEmptyArrays", true)));
			pipeline.add(
				new Document("$lookup", 
					new Document("from", "DMChiCucThue")
					.append("pipeline", 
						Arrays.asList(
							new Document("$match", new Document("code", coQuanThue)),
							new Document("$project", new Document("_id", 0))
						)
					)
					.append("as", "ChiCucThueInfo")
				)
			);
			pipeline.add(new Document("$unwind", new Document("path", "$ChiCucThueInfo").append("preserveNullAndEmptyArrays", true)));
			pipeline.add(
				new Document("$lookup", 
					new Document("from", "EInvoiceMTT")
					.append("pipeline", 
						Arrays.asList(
							new Document("$match", 
								new Document("IssuerId", header.getIssuerId())
								.append("HDSS.TCTBao", new Document("$ne", "1"))
								.append("EInvoiceStatus", new Document("$in", Arrays.asList("COMPLETE", "ADJUSTED")))
								.append("MCCQT", new Document("$in", listMCCQT))
							),
							new Document("$project", new Document("MCCQT", 1).append("EInvoiceDetail.TTChung", 1))
						)
					)
					.append("as", "EInvoiceMTT")
				)
			);
	
			
			MongoClient mongoClient = cfg.mongoClient();
			MongoCollection<Document> collection = mongoClient.getDatabase(cfg.dbName).getCollection("Issuer");
			      try {
			        docTmp = collection.aggregate(pipeline).allowDiskUse(true).iterator().next();    
			      } catch (Exception e) {
			        
			      }
			        
			mongoClient.close();
			
			if(null == docTmp) {
				responseStatus = new MspResponseStatus(9999, "Không tìm thấy thông tin khách hàng.");
				rsp.setResponseStatus(responseStatus);
				return rsp;
			}			
			if(docTmp.get("TinhThanhInfo") == null) {
				responseStatus = new MspResponseStatus(9999, "Không tìm thấy thông tin tỉnh/thành phố.");
				rsp.setResponseStatus(responseStatus);
				return rsp;
			}
			if(docTmp.get("ChiCucThueInfo") == null) {
				responseStatus = new MspResponseStatus(9999, "Không tìm thấy thông tin cơ quan thuế.");
				rsp.setResponseStatus(responseStatus);
				return rsp;
			}
			
			if(docTmp.get("EInvoiceMTT") == null || docTmp.getList("EInvoiceMTT", Document.class).size() == 0 || docTmp.getList("EInvoiceMTT", Document.class).size() != listMCCQT.size()) {
				responseStatus = new MspResponseStatus(9999, "Vui lòng kiểm tra lại danh sách hóa đơn.");
				rsp.setResponseStatus(responseStatus);
				return rsp;
			}
			taxCode = docTmp.getString("TaxCode");
			objectId = new ObjectId();
			
			/*LUU LAI THONG TIN HDSS*/
			path = Paths.get(SystemParams.DIR_E_INVOICE_HDSS, taxCode, String.valueOf(LocalDate.now().getYear()));
			pathDir = path.toString();
			file = path.toFile();
			if(!file.exists()) file.mkdirs();
			
//			fileNameXML = "HD-SS-" + commons.convertLocalDateTimeToString(LocalDate.now(), Constants.FORMAT_DATE.FORMAT_DATE_DB) + "-" + commons.csRandomNumbericString(5) + ".xml";
			fileNameXML = objectId.toString() + ".xml";
			
			dbf = DocumentBuilderFactory.newInstance();
			db = dbf.newDocumentBuilder();
			doc = db.newDocument();
			doc.setXmlStandalone(true);
			
			root = doc.createElement("TBao");
			doc.appendChild(root);
			
			elementContent = doc.createElement("DLTBao");
			elementContent.setAttribute("Id", "data");
			root.appendChild(elementContent);
			
			elementContent.appendChild(commons.createElementWithValue(doc, "PBan", SystemParams.VERSION_XML));
			elementContent.appendChild(commons.createElementWithValue(doc, "MSo", "04/SS-HĐĐT"));
			elementContent.appendChild(commons.createElementWithValue(doc, "Ten", "Thông báo hóa đơn điện tử có sai sót"));
			elementContent.appendChild(commons.createElementWithValue(doc, "Loai", loaiThongBao));
			if("2".equals(loaiThongBao)) {
				elementContent.appendChild(commons.createElementWithValue(doc, "So", soTBcuaCQT));
				elementContent.appendChild(commons.createElementWithValue(doc, "NTBCCQT", commons.convertLocalDateTimeToString(commons.convertStringToLocalDate(ngayTBcuaCQT, Constants.FORMAT_DATE.FORMAT_DATE_WEB), "yyyy-MM-dd")));
			}else {
			
			elementContent.appendChild(commons.createElementWithValue(doc, "NTBCCQT", ""));
			}
			elementContent.appendChild(commons.createElementWithValue(doc, "MCQT", docTmp.getEmbedded(Arrays.asList("ChiCucThueInfo", "code"), "")));
			elementContent.appendChild(commons.createElementWithValue(doc, "TCQT", docTmp.getEmbedded(Arrays.asList("ChiCucThueInfo", "name"), "")));
			elementContent.appendChild(commons.createElementWithValue(doc, "TNNT", docTmp.get("Name", "")));
			elementContent.appendChild(commons.createElementWithValue(doc, "MST", taxCode));
			elementContent.appendChild(commons.createElementWithValue(doc, "DDanh", docTmp.getEmbedded(Arrays.asList("TinhThanhInfo", "name"), "")));
			elementContent.appendChild(commons.createElementWithValue(doc, "NTBao", commons.convertLocalDateTimeToString(LocalDate.now(), "yyyy-MM-dd")));
			
			elementSubContent = doc.createElement("DSHDon");
			stt = 1;
			if(docTmp.get("EInvoiceMTT") != null){
				for(Document o: docTmp.getList("EInvoiceMTT", Document.class)) {
					hItem = hEInvoice.get(o.get("MCCQT", ""));
					
					elementTmp = doc.createElement("HDon");
					
					elementTmp.appendChild(commons.createElementWithValue(doc, "STT", String.valueOf(stt)));
					elementTmp.appendChild(commons.createElementWithValue(doc, "MCQTCap", o.get("MCCQT", "")));
					elementTmp.appendChild(commons.createElementWithValue(doc, "MCCQT", o.get("MCCQT", "")));
					elementTmp.appendChild(commons.createElementWithValue(doc, "KHMSHDon", o.getEmbedded(Arrays.asList("EInvoiceDetail", "TTChung", "KHMSHDon"), "")));
					elementTmp.appendChild(commons.createElementWithValue(doc, "KHHDon", o.getEmbedded(Arrays.asList("EInvoiceDetail", "TTChung", "KHHDon"), "")));
					elementTmp.appendChild(commons.createElementWithValue(doc, "SHDon", String.valueOf(o.getEmbedded(Arrays.asList("EInvoiceDetail", "TTChung", "SHDon"), Integer.class))));
					elementTmp.appendChild(commons.createElementWithValue(doc, "Ngay", commons.convertLocalDateTimeToString(commons.convertDateToLocalDateTime(o.getEmbedded(Arrays.asList("EInvoiceDetail", "TTChung", "NLap"), Date.class) ), "yyyy-MM-dd")));
					elementTmp.appendChild(commons.createElementWithValue(doc, "LADHDDT", "1"));
					elementTmp.appendChild(commons.createElementWithValue(doc, "TCTBao", null == hItem? "": hItem.get("TCTBao")));
					elementTmp.appendChild(commons.createElementWithValue(doc, "LDo", null == hItem? "": hItem.get("LDo")));
					
					elementSubContent.appendChild(elementTmp);
					
					hItem02 = new HashMap<String, String>();
					hItem02.put("STT", String.valueOf(stt));
					hItem02.put("MCQTCap", o.get("MCCQT", ""));
					hItem02.put("MCCQT", o.get("MCCQT", ""));
					hItem02.put("KHMSHDon", o.getEmbedded(Arrays.asList("EInvoiceDetail", "TTChung", "KHMSHDon"), ""));
					hItem02.put("KHHDon", o.getEmbedded(Arrays.asList("EInvoiceDetail", "TTChung", "KHHDon"), ""));
					hItem02.put("SHDon", String.valueOf(o.getEmbedded(Arrays.asList("EInvoiceDetail", "TTChung", "SHDon"), Integer.class)));
					hItem02.put("Ngay", commons.convertLocalDateTimeToString(commons.convertDateToLocalDateTime(o.getEmbedded(Arrays.asList("EInvoiceDetail", "TTChung", "NLap"), Date.class) ), "yyyy-MM-dd"));
					hItem02.put("LADHDDT", "1");
					hItem02.put("TCTBao", null == hItem? "": hItem.get("TCTBao"));
					hItem02.put("LDo", null == hItem? "": hItem.get("LDo"));
					
					listDSHDon.add(hItem02);
					
					stt++;
				}
			}
			
			elementContent.appendChild(elementSubContent);	
			
//			HSM 			
//			elementContent = doc.createElement("DSCKS");				
//			root.appendChild(elementContent);	
//			elementContent = doc.createElement("NNT");
//			root.appendChild(elementContent);	
			/*END - LUU LAI THONG TIN HDSS*/
			
			isSaveFile = commons.docW3cToFile(doc, pathDir, fileNameXML);
			if(!isSaveFile) {
				throw new Exception("Lưu dữ liệu không thành công.");
			}
			
			/*LUU THONG TIN HDSS VO DB*/
			docUpsert = new Document("_id", objectId)
					.append("IssuerId", header.getIssuerId())
					.append("PBan", SystemParams.VERSION_XML)
					.append("MSo", "04/SS-HĐĐT")
					.append("Ten", "Thông báo hóa đơn điện tử có sai sót")
					.append("Loai", loaiThongBao);
			if("2".equals(loaiThongBao)) {
				docUpsert.append("So", soTBcuaCQT);
				docUpsert.append("NTBCCQT", commons.convertLocalDateTimeToString(commons.convertStringToLocalDate(ngayTBcuaCQT, Constants.FORMAT_DATE.FORMAT_DATE_WEB), "yyyy-MM-dd"));
			}
			docUpsert.append("MCQT", docTmp.getEmbedded(Arrays.asList("ChiCucThueInfo", "code"), ""))
				.append("TCQT", docTmp.getEmbedded(Arrays.asList("ChiCucThueInfo", "name"), ""))
				.append("TNNT", docTmp.get("Name", ""))
				.append("MST", docTmp.get("TaxCode", ""))
				.append("DDanh", docTmp.getEmbedded(Arrays.asList("TinhThanhInfo", "name"), ""))
				.append("NTBao", commons.convertLocalDateTimeToString(LocalDate.now(), "yyyy-MM-dd"))
				.append("NTBaoDate", LocalDate.now())
				.append("DSHDon", listDSHDon)
				.append("Dir", pathDir)
				.append("FileNameXML", fileNameXML)
				.append("SignStatusCode", Constants.INVOICE_SIGN_STATUS.NOSIGN)
				.append("Status", Constants.INVOICE_STATUS.CREATED)
				.append("IsDelete", false)
				.append("TinhThanhInfo", docTmp.get("TinhThanhInfo"))
				.append("ChiCucThueInfo", docTmp.get("ChiCucThueInfo"))
				.append("InfoCreated", 
					new Document("CreateDate", LocalDateTime.now())
					.append("CreateUserID", header.getUserId())
					.append("CreateUserName", header.getUserName())
					.append("CreateUserFullName", header.getUserFullName())
				);
			
			/*END - LUU THONG TIN HDSS VO DB*/
			mongoClient = cfg.mongoClient();
			collection = mongoClient.getDatabase(cfg.dbName).getCollection("EInvoiceHDSSMTT");
			collection.insertOne(docUpsert);      
			mongoClient.close();
			
			String name_company = removeAccent(header.getUserFullName());
			DateTimeFormatter format_time = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
			LocalDateTime time_dem  = LocalDateTime.now();
			String time = time_dem.format(format_time);
			System.out.println(time +name_company+" Vua tao hoa don sai sot tu may tinh tien.");
			responseStatus = new MspResponseStatus(0, "SUCCESS");
			rsp.setResponseStatus(responseStatus);				
			return rsp;
		case Constants.MSG_ACTION_CODE.MODIFY:	
			objectId = null;
			ObjectId objectIdEInvoiceHDSSMTT = null;
			objectIdEInvoiceHDSSMTT = new ObjectId();
			objectIdEInvoiceHDSSMTT = null;
			try {
				objectId = new ObjectId(header.getIssuerId());
			}catch(Exception e) {}
			try {
				objectIdEInvoiceHDSSMTT = new ObjectId(_id);
			}catch(Exception e) {}
			pipeline = new ArrayList<Document>();
			pipeline.add(
				new Document("$match", 
					new Document("_id", objectId).append("IsActive", true).append("IsDelete", new Document("$ne", true))
				)
			);
			pipeline.add(
				new Document("$lookup", 
					new Document("from", "DMTinhThanh")
					.append("pipeline", 
						Arrays.asList(
							new Document("$match", new Document("code", tinhThanh)),
							new Document("$project", new Document("_id", 0))
						)
					)
					.append("as", "TinhThanhInfo")
				)
			);
			pipeline.add(new Document("$unwind", new Document("path", "$TinhThanhInfo").append("preserveNullAndEmptyArrays", true)));
			pipeline.add(
				new Document("$lookup", 
					new Document("from", "DMChiCucThue")
					.append("pipeline", 
						Arrays.asList(
							new Document("$match", new Document("code", coQuanThue)),
							new Document("$project", new Document("_id", 0))
						)
					)
					.append("as", "ChiCucThueInfo")
				)
			);
			pipeline.add(new Document("$unwind", new Document("path", "$ChiCucThueInfo").append("preserveNullAndEmptyArrays", true)));
			pipeline.add(
				new Document("$lookup", 
					new Document("from", "EInvoiceMTT")
					.append("pipeline", 
						Arrays.asList(
							new Document("$match", 
								new Document("IssuerId", header.getIssuerId())
//								.append("HDSS", new Document("$exists", false))
								.append("HDSS.TCTBao", new Document("$ne", "1"))
								.append("EInvoiceStatus", Constants.INVOICE_STATUS.COMPLETE)
								.append("MCCQT", new Document("$in", listMCCQT))
							),
							new Document("$project", new Document("MCCQT", 1).append("EInvoiceDetail.TTChung", 1))
						)
					)
					.append("as", "EInvoiceMTT")
				)
			);

	
			docFind = new Document("IssuerId", header.getIssuerId()).append("IsDelete", new Document("$ne", true))
					.append("_id", objectIdEInvoiceHDSSMTT);
			pipeline.add(
					new Document("$lookup", 
						new Document("from", "EInvoiceHDSSMTT")
						.append("pipeline", 
							Arrays.asList(
								new Document("$match", docFind)
//								new Document("$project", new Document("MCCQT", 1).append("EInvoiceDetail.TTChung", 1))
							)
						)
						.append("as", "EInvoiceHDSSMTT")
					)
				);
			pipeline.add(new Document("$unwind", new Document("path", "$EInvoiceHDSSMTT").append("preserveNullAndEmptyArrays", true)));
			docTmp = null;
			
			mongoClient = cfg.mongoClient();
			collection = mongoClient.getDatabase(cfg.dbName).getCollection("Issuer");
			  try {
					 docTmp = collection.aggregate(pipeline).allowDiskUse(true).iterator().next();      
				 } catch (Exception ex) {
					
				}
			mongoClient.close();
			
			if(null == docTmp) {
				responseStatus = new MspResponseStatus(9999, "Không tìm thấy thông tin khách hàng.");
				rsp.setResponseStatus(responseStatus);
				return rsp;
			}			
			if(docTmp.get("TinhThanhInfo") == null) {
				responseStatus = new MspResponseStatus(9999, "Không tìm thấy thông tin tỉnh/thành phố.");
				rsp.setResponseStatus(responseStatus);
				return rsp;
			}
			if(docTmp.get("ChiCucThueInfo") == null) {
				responseStatus = new MspResponseStatus(9999, "Không tìm thấy thông tin cơ quan thuế.");
				rsp.setResponseStatus(responseStatus);
				return rsp;
			}
			if(docTmp.get("EInvoiceMTT") == null 
					|| docTmp.getList("EInvoiceMTT", Document.class).size() == 0 
					|| docTmp.getList("EInvoiceMTT", Document.class).size() != listMCCQT.size() ) {
				responseStatus = new MspResponseStatus(9999, "Vui lòng kiểm tra lại danh sách hóa đơn.");
				rsp.setResponseStatus(responseStatus);
				return rsp;
			}
			
			taxCode = docTmp.getString("TaxCode");
			
			
			/*LUU LAI THONG TIN HDSS*/		
			pathDir = docTmp.getEmbedded(Arrays.asList("EInvoiceHDSSMTT", "Dir"), "");
			fileNameXML = docTmp.getEmbedded(Arrays.asList("EInvoiceHDSSMTT", "FileNameXML"), "");
			file = new File(pathDir);
			if(!file.exists()) file.mkdirs();
			
			dbf = DocumentBuilderFactory.newInstance();
			db = dbf.newDocumentBuilder();
			doc = db.newDocument();
			doc.setXmlStandalone(true);
			
			root = doc.createElement("TBao");
			doc.appendChild(root);
			
			elementContent = doc.createElement("DLTBao");
			elementContent.setAttribute("Id", "data");
			root.appendChild(elementContent);
			
			elementContent.appendChild(commons.createElementWithValue(doc, "PBan", SystemParams.VERSION_XML));
			elementContent.appendChild(commons.createElementWithValue(doc, "MSo", "04/SS-HĐĐT"));
			elementContent.appendChild(commons.createElementWithValue(doc, "Ten", "Thông báo hóa đơn điện tử có sai sót"));
			elementContent.appendChild(commons.createElementWithValue(doc, "Loai", loaiThongBao));
			if("2".equals(loaiThongBao)) {
				elementContent.appendChild(commons.createElementWithValue(doc, "So", soTBcuaCQT));
				elementContent.appendChild(commons.createElementWithValue(doc, "NTBCCQT", commons.convertLocalDateTimeToString(commons.convertStringToLocalDate(ngayTBcuaCQT, Constants.FORMAT_DATE.FORMAT_DATE_WEB), "yyyy-MM-dd")));
			}
			else {
				elementContent.appendChild(commons.createElementWithValue(doc, "NTBCCQT", ""));
			}
		
			elementContent.appendChild(commons.createElementWithValue(doc, "MCQT", docTmp.getEmbedded(Arrays.asList("ChiCucThueInfo", "code"), "")));
			elementContent.appendChild(commons.createElementWithValue(doc, "TCQT", docTmp.getEmbedded(Arrays.asList("ChiCucThueInfo", "name"), "")));
			elementContent.appendChild(commons.createElementWithValue(doc, "TNNT", docTmp.get("Name", "")));
			elementContent.appendChild(commons.createElementWithValue(doc, "MST", taxCode));
			elementContent.appendChild(commons.createElementWithValue(doc, "DDanh", docTmp.getEmbedded(Arrays.asList("TinhThanhInfo", "name"), "")));
			elementContent.appendChild(commons.createElementWithValue(doc, "NTBao", commons.convertLocalDateTimeToString(LocalDate.now(), "yyyy-MM-dd")));
			
			elementSubContent = doc.createElement("DSHDon");
			stt = 1;
			if(docTmp.get("EInvoiceMTT") != null){
				for(Document o: docTmp.getList("EInvoiceMTT", Document.class)) {
					hItem = hEInvoice.get(o.get("MCCQT", ""));
					
					elementTmp = doc.createElement("HDon");
					
					elementTmp.appendChild(commons.createElementWithValue(doc, "STT", String.valueOf(stt)));
					elementTmp.appendChild(commons.createElementWithValue(doc, "MCQTCap", o.get("MCCQT", "")));
					elementTmp.appendChild(commons.createElementWithValue(doc, "MCCQT", o.get("MCCQT", "")));
					elementTmp.appendChild(commons.createElementWithValue(doc, "KHMSHDon", o.getEmbedded(Arrays.asList("EInvoiceDetail", "TTChung", "KHMSHDon"), "")));
					elementTmp.appendChild(commons.createElementWithValue(doc, "KHHDon", o.getEmbedded(Arrays.asList("EInvoiceDetail", "TTChung", "KHHDon"), "")));
					elementTmp.appendChild(commons.createElementWithValue(doc, "SHDon", String.valueOf(o.getEmbedded(Arrays.asList("EInvoiceDetail", "TTChung", "SHDon"), Integer.class))));
					elementTmp.appendChild(commons.createElementWithValue(doc, "Ngay", commons.convertLocalDateTimeToString(commons.convertDateToLocalDateTime(o.getEmbedded(Arrays.asList("EInvoiceDetail", "TTChung", "NLap"), Date.class) ), "yyyy-MM-dd")));
					elementTmp.appendChild(commons.createElementWithValue(doc, "LADHDDT", "1"));
					elementTmp.appendChild(commons.createElementWithValue(doc, "TCTBao", null == hItem? "": hItem.get("TCTBao")));
					elementTmp.appendChild(commons.createElementWithValue(doc, "LDo", null == hItem? "": hItem.get("LDo")));
					
					elementSubContent.appendChild(elementTmp);
					
					hItem02 = new HashMap<String, String>();
					hItem02.put("STT", String.valueOf(stt));
					hItem02.put("MCQTCap", o.get("MCCQT", ""));
					hItem02.put("MCCQT", o.get("MCCQT", ""));
					hItem02.put("KHMSHDon", o.getEmbedded(Arrays.asList("EInvoiceDetail", "TTChung", "KHMSHDon"), ""));
					hItem02.put("KHHDon", o.getEmbedded(Arrays.asList("EInvoiceDetail", "TTChung", "KHHDon"), ""));
					hItem02.put("SHDon", String.valueOf(o.getEmbedded(Arrays.asList("EInvoiceDetail", "TTChung", "SHDon"), Integer.class)));
					hItem02.put("Ngay", commons.convertLocalDateTimeToString(commons.convertDateToLocalDateTime(o.getEmbedded(Arrays.asList("EInvoiceDetail", "TTChung", "NLap"), Date.class) ), "yyyy-MM-dd"));
					hItem02.put("LADHDDT", "1");
					hItem02.put("TCTBao", null == hItem? "": hItem.get("TCTBao"));
					hItem02.put("LDo", null == hItem? "": hItem.get("LDo"));
					
					listDSHDon.add(hItem02);
					
					stt++;
				}
			}
			
			elementContent.appendChild(elementSubContent);
			
			
//			HSM	
//			elementContent = doc.createElement("DSCKS");				
//			root.appendChild(elementContent);	
//			elementContent = doc.createElement("NNT");
//			root.appendChild(elementContent);	
			/*END - LUU LAI THONG TIN HDSS*/
			
			isSaveFile = commons.docW3cToFile(doc, pathDir, fileNameXML);
			if(!isSaveFile) {
				throw new Exception("Lưu dữ liệu không thành công.");
			}
			if("2".equals(loaiThongBao)) {
				docUpsert.append("So", soTBcuaCQT);
				docUpsert.append("NTBCCQT", commons.convertLocalDateTimeToString(commons.convertStringToLocalDate(ngayTBcuaCQT, Constants.FORMAT_DATE.FORMAT_DATE_WEB), "yyyy-MM-dd"));
			}
		
			/*END - LUU DU LIEU HD*/
			options = new FindOneAndUpdateOptions();
			options.upsert(false);
			options.maxTime(5000, TimeUnit.MILLISECONDS);
			options.returnDocument(ReturnDocument.AFTER);
			
			
			mongoClient = cfg.mongoClient();
			collection = mongoClient.getDatabase(cfg.dbName).getCollection("EInvoiceHDSSMTT");
			collection.findOneAndUpdate(docFind,
				  new Document("$set",
						  new Document("IssuerId", header.getIssuerId())
							.append("PBan", SystemParams.VERSION_XML)
							.append("MSo", "04/SS-HĐĐT")
							.append("Ten", "Thông báo hóa đơn điện tử có sai sót")
							.append("Loai", loaiThongBao)
							.append("MCQT", docTmp.getEmbedded(Arrays.asList("ChiCucThueInfo", "code"), ""))
								.append("TCQT", docTmp.getEmbedded(Arrays.asList("ChiCucThueInfo", "name"), ""))
								.append("TNNT", docTmp.get("Name", ""))
								.append("MST", docTmp.get("TaxCode", ""))
								.append("DDanh", docTmp.getEmbedded(Arrays.asList("TinhThanhInfo", "name"), ""))
								.append("NTBao", commons.convertLocalDateTimeToString(LocalDate.now(), "yyyy-MM-dd"))
								.append("NTBaoDate", LocalDate.now())
								.append("DSHDon", listDSHDon)
								.append("Dir", pathDir)
								.append("FileNameXML", fileNameXML)
								.append("SignStatusCode", Constants.INVOICE_SIGN_STATUS.NOSIGN)
								.append("Status", Constants.INVOICE_STATUS.CREATED)
								.append("IsDelete", false)
								.append("TinhThanhInfo", docTmp.get("TinhThanhInfo"))
								.append("ChiCucThueInfo", docTmp.get("ChiCucThueInfo"))	
								.append("InfoUpdated", 
										new Document("UpdatedDate", LocalDateTime.now())
										.append("UpdatedUserID", header.getUserId())
										.append("UpdatedUserName", header.getUserName())
										.append("UpdatedUserFullName", header.getUserFullName())
										)
									), 
									options
								);	 
			  mongoClient.close();
				
			format_time = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
			time_dem  = LocalDateTime.now();
			time = time_dem.format(format_time);
			name_company = removeAccent(header.getUserFullName());
			System.out.println(time +name_company+" Vua thay doi hoa don sai sot tu may tinh tien.");
			responseStatus = new MspResponseStatus(0, "SUCCESS");
			rsp.setResponseStatus(responseStatus);				
			return rsp;
		case Constants.MSG_ACTION_CODE.DELETE:
			objectId = null;
			try {
				objectId = new ObjectId(_id);
			}catch(Exception e) {}
			
			docFind = new Document("IssuerId", header.getIssuerId())
					.append("_id", objectId)
					.append("IsDelete", new Document("$ne", true))
					.append("Status", new Document("$in", Arrays.asList(Constants.INVOICE_STATUS.CREATED, Constants.INVOICE_STATUS.PENDING)));
			docTmp = null;
			
			mongoClient = cfg.mongoClient();
			collection = mongoClient.getDatabase(cfg.dbName).getCollection("EInvoiceHDSSMTT");
			  try {
				  docTmp = collection.find(docFind).allowDiskUse(true).iterator().next();      
			     } catch (Exception ex) {
			        
			    }
			mongoClient.close();
			
			if(null == docTmp) {
				responseStatus = new MspResponseStatus(9999, "Không tìm thấy thông tin thông báo.");
				rsp.setResponseStatus(responseStatus);
				return rsp;
			}

			options = new FindOneAndUpdateOptions();
			options.upsert(false);
			options.maxTime(5000, TimeUnit.MILLISECONDS);
			options.returnDocument(ReturnDocument.AFTER);
			
			
			mongoClient = cfg.mongoClient();
			collection = mongoClient.getDatabase(cfg.dbName).getCollection("EInvoiceHDSSMTT");
			collection.findOneAndUpdate(docFind,
				  new Document("$set",
						  new Document("IsDelete", true)
							.append("InfoDeleted", 
									new Document("DeletedDate", LocalDateTime.now())
										.append("DeletedUserID", header.getUserId())
										.append("DeletedUserName", header.getUserName())
										.append("DeletedUserFullName", header.getUserFullName())
									)
						)
						, options);
			  mongoClient.close();
					
			 name_company = removeAccent(header.getUserFullName());
			 
			format_time = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
			time_dem  = LocalDateTime.now();
			time = time_dem.format(format_time);
			System.out.println(time +name_company+" Vua xoa hoa don sai sot tu may tinh tien.");
			responseStatus = new MspResponseStatus(0, "SUCCESS");
			rsp.setResponseStatus(responseStatus);				
			return rsp;
		case Constants.MSG_ACTION_CODE.SEND_CQT:
			objectId = null;
			try {
				objectId = new ObjectId(_id);
			}catch(Exception e) {}
			
			docFind = new Document("IssuerId", header.getIssuerId())
					.append("_id", objectId)
					.append("IsDelete", new Document("$ne", true))
					.append("Status", Constants.INVOICE_STATUS.PENDING)
					.append("SignStatusCode", Constants.INVOICE_SIGN_STATUS.SIGNED)
					.append("NTBaoDate", LocalDate.now());
			docTmp = null;
			
			mongoClient = cfg.mongoClient();
			collection = mongoClient.getDatabase(cfg.dbName).getCollection("EInvoiceHDSSMTT");
			  try {
				  docTmp = collection.find(docFind).allowDiskUse(true).iterator().next();      
			     } catch (Exception ex) {
			        
			    }
			mongoClient.close();
			
			if(null == docTmp) {
				responseStatus = new MspResponseStatus(9999, "Không tìm thấy thông tin thông báo.");
				rsp.setResponseStatus(responseStatus);
				return rsp;
			}
			
			String dir = docTmp.get("Dir", "");
			String fileName = _id + "_signed.xml";
			file = new File(dir, fileName);
			if(!file.exists() || !file.isFile()) {
				responseStatus = new MspResponseStatus(9999, "Không tìm thấy thông tin thông báo đã ký.");
				rsp.setResponseStatus(responseStatus);
				return rsp;
			}
			doc = commons.fileToDocument(file, true);
			if(null == doc) {
				responseStatus = new MspResponseStatus(9999, "Dữ liệu thông báo đã ký không tồn tại.");
				rsp.setResponseStatus(responseStatus);
				return rsp;
			}
			org.w3c.dom.Document rTCTN = null;
			org.w3c.dom.Document rTCTN1 = null;
			MTDiep = docTmp.get("MTDiep", "");
			MST = docTmp.get("MST", "");
			XPath xPath1 = null;
			String MLoi = "";
			Node nodeKetQuaTraCuu = null;
			String MaKetQua = "";
			String CQT_MLTDiep  = "";
			String codeTTTNhan = "";
			String LTBao = "";
			String descTTTNhan="";
//			rTCTN = tctnService.callTiepNhanThongDiep("303", MTDiep, MST, "1", doc);
//			if(rTCTN == null) {
//				rTCTN = tctnService.callTiepNhanThongDiep("303", MTDiep, MST, "1", doc);
//			}
//			
//			/*DO DU LIEU TRA VE - CAP NHAT LAI KET QUA*/
//			XPath xPath = XPathFactory.newInstance().newXPath();
//			Node nodeDLHDon = (Node) xPath.evaluate("/TDiep", rTCTN, XPathConstants.NODE);
//			String codeTTTNhan = commons.getTextFromNodeXML((Element) xPath.evaluate("DLieu/TBao/TTTNhan", nodeDLHDon, XPathConstants.NODE));
//			String descTTTNhan = commons.getTextFromNodeXML((Element) xPath.evaluate("DLieu/TBao/DSLDo/LDo/MTa", nodeDLHDon, XPathConstants.NODE));
			//TRA CUU HOA DƠN TRC KHI CALL TIEP NHAN THONG DIEP
			
			 rTCTN1 = tctnService.callTraCuuThongDiep(MTDiep);
			 if (rTCTN1 == null) {			
					 rTCTN1 = tctnService.callTraCuuThongDiep(MTDiep);
				}
			 	xPath1 = XPathFactory.newInstance().newXPath();
				 nodeKetQuaTraCuu = (Node) xPath1.evaluate("/KetQuaTraCuu", rTCTN1, XPathConstants.NODE);
				 MaKetQua = commons.getTextFromNodeXML((Element) xPath1.evaluate("MaKetQua", nodeKetQuaTraCuu, XPathConstants.NODE));
				 if("2".equals(MaKetQua)) {
						rTCTN = tctnService.callTiepNhanThongDiep("303", MTDiep, MST, "1", doc);
						XPath xPath = XPathFactory.newInstance().newXPath();
						Node nodeDLHDon = (Node) xPath.evaluate("/TDiep", rTCTN, XPathConstants.NODE);
						 codeTTTNhan = commons.getTextFromNodeXML((Element) xPath.evaluate("DLieu/TBao/TTTNhan", nodeDLHDon, XPathConstants.NODE));
						 descTTTNhan = commons.getTextFromNodeXML((Element) xPath.evaluate("DLieu/TBao/DSLDo/LDo/MTa", nodeDLHDon, XPathConstants.NODE));				 
				 }else {					 								 
					 Node nodeTDiep = null;
						for(int i = 1; i<=5; i++) {
							if(xPath1.evaluate("DuLieu/TDiep[" + i + "]", nodeKetQuaTraCuu, XPathConstants.NODE) == null) break;
							nodeTDiep = (Node) xPath1.evaluate("DuLieu/TDiep[" + i + "]", nodeKetQuaTraCuu, XPathConstants.NODE);
							if(xPath1.evaluate("DLieu/TBao/DLTBao", nodeTDiep, XPathConstants.NODE) != null)
								break;
		}
					 CQT_MLTDiep = commons.getTextFromNodeXML((Element) xPath1.evaluate("TTChung/MLTDiep", nodeTDiep, XPathConstants.NODE));
						LTBao = commons.getTextFromNodeXML((Element) xPath1.evaluate("DLieu/TBao/DLTBao/LTBao", nodeTDiep, XPathConstants.NODE));
			
			if("204".equals(CQT_MLTDiep)&&"2".equals(LTBao)) {
				codeTTTNhan = MaKetQua;
			}		
		}
			switch (codeTTTNhan) {
			case "1":
				responseStatus = new MspResponseStatus(9999, "".equals(descTTTNhan)? "Không tìm thấy tenant dữ liệu.": descTTTNhan);
				rsp.setResponseStatus(responseStatus);
				return rsp;
			case "2":
				responseStatus = new MspResponseStatus(9999, "".equals(descTTTNhan)? "Mã thông điệp đã tồn tại.": descTTTNhan);
				rsp.setResponseStatus(responseStatus);
				return rsp;
			case "3":
				responseStatus = new MspResponseStatus(9999, "".equals(descTTTNhan)? "Thất bại, lỗi Exception.": descTTTNhan);
				rsp.setResponseStatus(responseStatus);
				return rsp;
			default:
				break;
			}
			
			/*CAP NHAT LAI TRANG THAI DANG CHO XU LY*/
			options = new FindOneAndUpdateOptions();
			options.upsert(false);
			options.maxTime(5000, TimeUnit.MILLISECONDS);
			options.returnDocument(ReturnDocument.AFTER);
			
			
			mongoClient = cfg.mongoClient();
			collection = mongoClient.getDatabase(cfg.dbName).getCollection("EInvoiceHDSSMTT");
			collection.findOneAndUpdate(docFind,
				new Document("$set", 
					new Document("Status", "PROCESSING")
//					.append("MTDiep", MTDiep)				
					.append("InfoSendCQT", 
						new Document("Date", LocalDateTime.now())
							.append("UserID", header.getUserId())
							.append("UserName", header.getUserName())
							.append("UserFullName", header.getUserFullName())
					)
				), 
				options
			);
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
		JsonNode jsonData = null;
		if(objData != null) {
			jsonData = Json.serializer().nodeFromObject(objData);
			mauSoHdon = commons.getTextJsonNode(jsonData.at("/MauSoHdon")).replaceAll("\\s", "");
			soHoaDon = commons.getTextJsonNode(jsonData.at("/SoHoaDon")).replaceAll("\\s", "");
			status = commons.getTextJsonNode(jsonData.at("/Status")).replaceAll("\\s", "");
			fromDate = commons.getTextJsonNode(jsonData.at("/FromDate")).replaceAll("\\s", "");
			toDate = commons.getTextJsonNode(jsonData.at("/ToDate")).replaceAll("\\s", "");
		}
		
		MsgRsp rsp = new MsgRsp(header);
		MspResponseStatus responseStatus = null;
		
		ObjectId objectId = null;
		Document docTmp = null;
		List<Document> pipeline = new ArrayList<Document>();
		
		LocalDate dateFrom = null;
		LocalDate dateTo = null;
		Document docMatchDate = null;
		
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
		 String ms = "";
		 String kh = "";
		if(!mauSoHdon.equals("")) {
			  ms = mauSoHdon.substring(0, 1);
			  kh =  mauSoHdon.substring(1);
		

		}
		Document docMatch = new Document("IssuerId", header.getIssuerId())
				.append("IsDelete", new Document("$ne", true));
		if(null != docMatchDate)
			docMatch.append("NTBaoDate", docMatchDate);
		if(!"".equals(status))
			docMatch.append("Status", commons.regexEscapeForMongoQuery(status));
		if(!"".equals(ms))
			docMatch.append("DSHDon.KHMSHDon", commons.regexEscapeForMongoQuery(ms));
		if(!"".equals(kh))
			docMatch.append("DSHDon.KHHDon", commons.regexEscapeForMongoQuery(kh));
		if(!"".equals(soHoaDon))
			docMatch.append("DSHDon.SHDon", commons.regexEscapeForMongoQuery(soHoaDon));
		pipeline = new ArrayList<Document>();
		pipeline.add(new Document("$match", docMatch));
		pipeline.add(
			new Document("$sort", 
				new Document("NTBao", -1).append("_id", -1)
			)
		);
		pipeline.addAll(createFacetForSearchNotSort(page));
		
		MongoClient mongoClient = cfg.mongoClient();
		MongoCollection<Document> collection = mongoClient.getDatabase(cfg.dbName).getCollection("EInvoiceHDSSMTT");
		      try {
		        docTmp = collection.aggregate(pipeline).allowDiskUse(true).iterator().next();    
		      } catch (Exception e) {
		        
		      }
		        
		mongoClient.close();
		
		rsp = new MsgRsp(header);
		responseStatus = null;
		if(null == docTmp) {
			responseStatus = new MspResponseStatus(9999, Constants.MAP_ERROR.get(9999));
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
			for(Document doc: rows) {
				objectId = (ObjectId) doc.get("_id");
				hItem = new HashMap<String, Object>();
				hItem.put("_id", objectId.toString());
				hItem.put("Status", doc.get("Status"));
				hItem.put("SignStatusCode", doc.get("SignStatusCode"));
				hItem.put("MSo", doc.get("MSo"));
				hItem.put("Ten", doc.get("Ten"));
				hItem.put("Loai", doc.get("Loai"));
				hItem.put("DSHDon", doc.get("DSHDon"));
				hItem.put("NTBaoDate", doc.get("NTBaoDate"));
				hItem.put("InfoCreated", doc.get("InfoCreated"));
				hItem.put("DSLoi", doc.get("DSLoi"));
				hItem.put("MTDiep", doc.get("MTDiep"));
				hItem.put("MTDTChieu", doc.get("MTDTChieu"));
				rowsReturn.add(hItem);
			}
		}
		
		DateTimeFormatter format_time = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
		LocalDateTime time_dem  = LocalDateTime.now();
		String time = time_dem.format(format_time);
		String name_company = removeAccent(header.getUserFullName());
		System.out.println(time +name_company+" Vua search hoa don sai sot tu may tinh tien.");
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
		}catch(Exception e) {}
		
		Document docFind = new Document("IssuerId", header.getIssuerId())
				.append("IsDelete", new Document("$ne", true)).append("_id", objectId);
		
		List<Document> pipeline = new ArrayList<Document>();
		
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
		MongoCollection<Document> collection = mongoClient.getDatabase(cfg.dbName).getCollection("EInvoiceHDSSMTT");
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
		
		DateTimeFormatter format_time = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
		LocalDateTime time_dem  = LocalDateTime.now();
		String time = time_dem.format(format_time);
		String name_company = removeAccent(header.getUserFullName());
		System.out.println(time +name_company+" Vua xem chi tiet hoa don sai sot tu may tinh tien.");
		rsp.setObjData(docTmp);
		responseStatus = new MspResponseStatus(0, Constants.MAP_ERROR.get(0));
		rsp.setResponseStatus(responseStatus);
		return rsp;
	}


	public FileInfo getFileForSign(JSONRoot jsonRoot) throws Exception {
		FileInfo fileInfo = new FileInfo();
		
		Msg msg = jsonRoot.getMsg();
		MsgHeader header = msg.getMsgHeader();
		Object objData = msg.getObjData();
		
		JsonNode jsonData = null;
		if(objData != null) {
			jsonData = Json.serializer().nodeFromObject(msg.getObjData());
		}else{
			throw new Exception("Lỗi dữ liệu đầu vào");
		}

		String _id = commons.getTextJsonNode(jsonData.at("/_id")).replaceAll("\\s", "");
		ObjectId objectId = null;
		try {
			objectId = new ObjectId(_id);
		}catch(Exception e) {}
		
		Document docFind = new Document("IssuerId", header.getIssuerId())
				.append("_id", objectId)
				.append("IsDelete", new Document("$ne", true))
				.append("Status", Constants.INVOICE_STATUS.CREATED)
				.append("SignStatusCode", Constants.INVOICE_SIGN_STATUS.NOSIGN)
				.append("NTBaoDate", LocalDate.now());
		List<Document> pipeline = new ArrayList<Document>();
		pipeline.add(new Document("$match", docFind));
		/*KIEM TRA DANH SACH HDSS - DANH SACH HD XEM HOP LE KHONG*/
		pipeline.add(
			new Document("$lookup", 
				new Document("from", "EInvoiceMTT")
				.append("let", 
					new Document("vIssuerId", "$IssuerId")
					.append("vDSHDon", 
						new Document("$map", 
							new Document("input", "$DSHDon")
							.append("as", "o")
							.append("in", "$$o.MCQTCap")
						)
					)
				)
				
				.append("pipeline", 
					Arrays.asList(
						new Document("$match", 
							new Document("$expr", 
								new Document("$and", 
									Arrays.asList(
										new Document("$eq", Arrays.asList("$IssuerId", "$$vIssuerId")),
										new Document("$ne", Arrays.asList(new Document("$type", "$HDSS"), "object")),
										new Document("$eq", Arrays.asList("$EInvoiceStatus", "COMPLETE")),
										new Document("$in", Arrays.asList("$MCCQT", "$$vDSHDon"))
									)
								)
							)
						),
						new Document("$project", new Document("MCCQT", 1))
					)
				)
				.append("as", "EInvoiceMTTInfo")
			)
		);

		Document docTmp = null;
		MongoClient mongoClient = cfg.mongoClient();
		MongoCollection<Document> collection = mongoClient.getDatabase(cfg.dbName).getCollection("EInvoiceHDSSMTT");
		      try {
		        docTmp = collection.aggregate(pipeline).allowDiskUse(true).iterator().next();    
		      } catch (Exception e) {
		        
		      }
		        
		mongoClient.close();
		
		if(null == docTmp) {
			return fileInfo;
		}
		
		/*AP DUNG 1 FILE TRUOC*/
		String dir = docTmp.get("Dir", "");
		String fileName = docTmp.get("FileNameXML", "");
		File file = new File(dir, fileName);
		
		if(!file.exists()) return fileInfo;
		
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
		
		/*DOC NOI DUNG XML DA KY*/
		org.w3c.dom.Document xmlDoc = commons.inputStreamToDocument(is, true);
		
		ObjectId objectId = null;
		try {
			objectId = new ObjectId(_id);
		}catch(Exception e) {}
		
		Document docFind = new Document("IssuerId", header.getIssuerId())
				.append("_id", objectId)
				.append("IsDelete", new Document("$ne", true))
				.append("Status", Constants.INVOICE_STATUS.CREATED)
				.append("SignStatusCode", Constants.INVOICE_SIGN_STATUS.NOSIGN)
				.append("NTBaoDate", LocalDate.now());
		
		Document docTmp = null;
		
		MongoClient mongoClient = cfg.mongoClient();
		MongoCollection<Document> collection = mongoClient.getDatabase(cfg.dbName).getCollection("EInvoiceHDSSMTT");
		      try {
		        docTmp = collection.find(docFind).allowDiskUse(true).iterator().next();    
		      } catch (Exception e) {
		        
		      }
		        
		mongoClient.close();
		
		if(null == docTmp) {
			responseStatus = new MspResponseStatus(9999, "Không tìm thấy thông báo hđ sai sót.");
			rsp.setResponseStatus(responseStatus);
			return rsp;
		}
		String dir = docTmp.get("Dir", "");
		String fileName = _id + "_signed.xml";
		boolean check = commons.docW3cToFile(xmlDoc, dir, fileName);
		if(!check) {
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
		
		mongoClient = cfg.mongoClient();
		collection = mongoClient.getDatabase(cfg.dbName).getCollection("EInvoiceHDSSMTT");
		collection.findOneAndUpdate(docFind,
			new Document("$set", 
				new Document("SignStatusCode", Constants.INVOICE_SIGN_STATUS.SIGNED)
				.append("Status", Constants.INVOICE_STATUS.PENDING)
				.append("MTDiep", MTDiep)
				.append("InfoSigned", 
					new Document("SignedDate", LocalDateTime.now())
						.append("SignedUserID", header.getUserId())
						.append("SignedUserName", header.getUserName())
						.append("SignedUserFullName", header.getUserFullName())
				)
			),
			options
		);
		  mongoClient.close();
		
		DateTimeFormatter format_time = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
		LocalDateTime time_dem  = LocalDateTime.now();
		String time = time_dem.format(format_time);
		String name_company = removeAccent(header.getUserFullName());
		System.out.println(time +name_company+" Vua ky thanh cong hoa don sai sot tu may tinh tien.");
		responseStatus = new MspResponseStatus(0, Constants.MAP_ERROR.get(0));
		rsp.setResponseStatus(responseStatus);
		return rsp;
	}


	@Override
	public MsgRsp refreshStatusCQT(JSONRoot jsonRoot) throws Exception {
		Msg msg = jsonRoot.getMsg();
		MsgHeader header = msg.getMsgHeader();
		MsgPage page = msg.getMsgPage();
		 String _title = "";
		 String _content = "";
		 String _email = "";
		 String MST = "";
		Object objData = msg.getObjData();
		File file = null;
		org.w3c.dom.Document doc1 = null;
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
		
		FindOneAndUpdateOptions options = null;
		
		Document docFind = new Document("IssuerId", header.getIssuerId())
				.append("_id", objectId)
				.append("IsDelete", new Document("$ne", true))
				.append("Status", new Document("$in", Arrays.asList("ERROR_CQT", "PROCESSING")))
				.append("SignStatusCode", Constants.INVOICE_SIGN_STATUS.SIGNED);
		
		Document docTmp = null;
		MongoClient mongoClient = cfg.mongoClient();
		MongoCollection<Document> collection = mongoClient.getDatabase(cfg.dbName).getCollection("EInvoiceHDSSMTT");
		      try {
		        docTmp = collection.find(docFind).allowDiskUse(true).iterator().next();    
		      } catch (Exception e) {
		        
		      }
		        
		mongoClient.close();
		
		if(null == docTmp) {
			responseStatus = new MspResponseStatus(9999, "Không tìm thấy thông tin thông báo.");
			rsp.setResponseStatus(responseStatus);
			return rsp;
		}
	
		
		
		String dir1 = docTmp.get("Dir", "");
		String fileName1 = _id + "_signed.xml";
		file = new File(dir1, fileName1);
		doc1 = commons.fileToDocument(file, true);
		
//		String MTDiep = docTmp.get("MTDiep", "");
//		org.w3c.dom.Document rTCTN = tctnService.callTraCuuThongDiep(MTDiep);
//		if(rTCTN == null) {
//			responseStatus = new MspResponseStatus(9999, "Kết nối với TCTN không thành công.");
//			rsp.setResponseStatus(responseStatus);
//			return rsp;
//		}
		
		String MTDiep = docTmp.get("MTDiep", "");
		org.w3c.dom.Document rTCTN = null;
		rTCTN = tctnService.callTraCuuThongDiep(MTDiep);
		if (rTCTN == null) {
			rTCTN = tctnService.callTraCuuThongDiep(MTDiep);
		}
		
		
		
		
		/*LUU LAI FILE KQTN*/
		String dir = docTmp.get("Dir", "");
		String fileName = _id + "_" + MTDiep + ".xml";
		boolean boo = false;
		try {
			boo = commons.docW3cToFile(rTCTN, dir, fileName);
		}catch(Exception e) {}
		if(!boo) {
			responseStatus = new MspResponseStatus(9999, "Lưu tập tin trả về từ CQT không thành công.");
			rsp.setResponseStatus(responseStatus);
			return rsp;
		}
		
		org.w3c.dom.Document rTCTN1 = null;
		
		 MST = docTmp.get("MST", "");
		/*DO DU LIEU TRA VE - CAP NHAT LAI KET QUA*/
		XPath xPath = XPathFactory.newInstance().newXPath();
		Node nodeKetQuaTraCuu = (Node) xPath.evaluate("/KetQuaTraCuu", rTCTN, XPathConstants.NODE);
		String MaKetQua = commons.getTextFromNodeXML((Element) xPath.evaluate("MaKetQua", nodeKetQuaTraCuu, XPathConstants.NODE));
		String MoTaKetQua = commons.getTextFromNodeXML((Element) xPath.evaluate("MoTaKetQua", nodeKetQuaTraCuu, XPathConstants.NODE));
		
//		if(!"0".equals(MaKetQua)) {
//			responseStatus = new MspResponseStatus(9999, MoTaKetQua);
//			rsp.setResponseStatus(responseStatus);
//			return rsp;
//		}
	
		String codeTTTNhan = "";
		String descTTTNhan="";
		if("2".equals(MaKetQua)) {			
			
			//call 3 lần mỗi lần 3s
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
				//Độ trễ
				TimeUnit.SECONDS.sleep(3);
			}
			nodeKetQuaTraCuu = (Node) xPath.evaluate("/KetQuaTraCuu", rTCTN, XPathConstants.NODE);
			MaKetQua = commons.getTextFromNodeXML((Element) xPath.evaluate("MaKetQua", nodeKetQuaTraCuu, XPathConstants.NODE));
			MoTaKetQua = commons.getTextFromNodeXML((Element) xPath.evaluate("MoTaKetQua", nodeKetQuaTraCuu, XPathConstants.NODE));
		
			if("2".equals(MaKetQua) && "Mã giao dịch không đúng".equals(MoTaKetQua)) {	
			doc1 = commons.fileToDocument(file, true);
			rTCTN = tctnService.callTiepNhanThongDiep("303", MTDiep, MST, "1", doc1);
			 xPath = XPathFactory.newInstance().newXPath();
			Node nodeDLHDon = (Node) xPath.evaluate("/TDiep", rTCTN, XPathConstants.NODE);
			 codeTTTNhan = commons.getTextFromNodeXML((Element) xPath.evaluate("DLieu/TBao/TTTNhan", nodeDLHDon, XPathConstants.NODE));
			 descTTTNhan = commons.getTextFromNodeXML((Element) xPath.evaluate("DLieu/TBao/DSLDo/LDo/MTa", nodeDLHDon, XPathConstants.NODE));				 ;
			 responseStatus = new MspResponseStatus(9999, "Mã giao dịch đang được gửi lại.");
			rsp.setResponseStatus(responseStatus);
			return rsp; 
			}	  
		}
		
		
		
		
		
		
		
		
		boolean check_ = false;
		Node nodeTDiep = null;
	    String CQT_MLTDiep = "";
	    String MLoi1 = "";
		String MTLoi1 = "";
	    String TTTNhan = "";
		String MTDTChieu1 = "";
		String LTBao = "";
		String LTBao1 = "";
		NodeList nodeList1 = null;
		String CQT_MLTDiep1 = "";
		String TTTNCCQT = "";
		
		
	    for(int i = 1; i<=20; i++) {
	      if(xPath.evaluate("DuLieu/TDiep[" + i + "]", nodeKetQuaTraCuu, XPathConstants.NODE) == null) break;
	      nodeTDiep = (Node) xPath.evaluate("DuLieu/TDiep[" + i + "]", nodeKetQuaTraCuu, XPathConstants.NODE);
	       CQT_MLTDiep = commons.getTextFromNodeXML((Element) xPath.evaluate("TTChung/MLTDiep", nodeTDiep, XPathConstants.NODE));
	       if("301".equals(CQT_MLTDiep)) {
	    	   TTTNCCQT = commons.getTextFromNodeXML((Element) xPath.evaluate("DLieu/TBao/DLTBao/DSHDon/HDon/TTTNCCQT", nodeTDiep, XPathConstants.NODE));
	          break;
	       }
	       if(CQT_MLTDiep.equals("204")) {
				check_ = true;
				MLoi1 = commons.getTextFromNodeXML((Element) xPath.evaluate("DLieu/TBao/DLTBao/KHLKhac/DSLDo/LDo/MLoi", nodeTDiep, XPathConstants.NODE));
				MTLoi1 = commons.getTextFromNodeXML((Element) xPath.evaluate("DLieu/TBao/DLTBao/KHLKhac/DSLDo/LDo/MTLoi", nodeTDiep, XPathConstants.NODE));
				
				CQT_MLTDiep1 = CQT_MLTDiep;
				MTDTChieu1 = commons
						.getTextFromNodeXML((Element) xPath.evaluate("TTChung/MTDTChieu", nodeTDiep, XPathConstants.NODE));
				LTBao1 = commons.getTextFromNodeXML((Element) xPath.evaluate("DLieu/TBao/DLTBao/LTBao", nodeTDiep, XPathConstants.NODE));
				nodeList1 = (NodeList) xPath.evaluate("DLieu/TBao/DLTBao/KHLKhac", nodeTDiep, XPathConstants.NODESET);
				
				 TTTNCCQT = commons.getTextFromNodeXML((Element) xPath.evaluate("DLieu/TBao/DLTBao/DSHDon/HDon/TTTNCCQT", nodeTDiep, XPathConstants.NODE));
	       }
	    }
		
		if(nodeTDiep == null) {
			responseStatus = new MspResponseStatus(9999, "CQT chưa có thông báo trả về.");
			rsp.setResponseStatus(responseStatus);
			return rsp;
		}
		
		String MTDTChieu = "";
		if (check_== true) {
			CQT_MLTDiep = CQT_MLTDiep1;
			MTDTChieu = MTDTChieu1;
		}
		else {
		
		CQT_MLTDiep = commons.getTextFromNodeXML((Element) xPath.evaluate("TTChung/MLTDiep", nodeTDiep, XPathConstants.NODE));
		MTDTChieu = commons
				.getTextFromNodeXML((Element) xPath.evaluate("TTChung/MTDTChieu", nodeTDiep, XPathConstants.NODE));
		}
		

		List<HashMap<String, String>> DSLoi = new ArrayList<HashMap<String,String>>();
		HashMap<String, String> hItem = new HashMap<String, String>();
		NodeList nodeList = null;
		Node node = null;		
		
		
		if("204".equals(CQT_MLTDiep)) {
			LTBao = LTBao1;
			if(!MLoi1.equals("")) {
				
				nodeList = nodeList1;
				for(int i = 0; i < nodeList.getLength(); i++) {
					node = nodeList.item(i);
					hItem = new HashMap<String, String>();
					hItem.put("MLoi", commons.getTextFromNodeXML((Element) xPath.evaluate("DSLDo/LDo/MLoi", node, XPathConstants.NODE)));
					hItem.put("MTLoi", commons.getTextFromNodeXML((Element) xPath.evaluate("DSLDo/LDo/MTLoi", node, XPathConstants.NODE)));
					DSLoi.add(hItem);
				}
				
				/*CAP NHAT TRANG THAI COMPLETE - TRANG THAI CQT*/
				options = new FindOneAndUpdateOptions();
				options.upsert(false);
				options.maxTime(5000, TimeUnit.MILLISECONDS);
				options.returnDocument(ReturnDocument.AFTER);
				
				
				mongoClient = cfg.mongoClient();
				collection = mongoClient.getDatabase(cfg.dbName).getCollection("EInvoiceHDSSMTT");
				collection.findOneAndUpdate(docFind,
					new Document("$set", 
						new Document("Status", Constants.INVOICE_STATUS.ERROR_CQT)
						.append("DSLoi", DSLoi)
					), 
					options
				);
				 mongoClient.close();
				
				responseStatus = new MspResponseStatus(0, Constants.MAP_ERROR.get(0));
				rsp.setResponseStatus(responseStatus);
				return rsp;
			}else {
			if("9".equals(LTBao)) {//TEST
			//if("2".equals(LTBao)) {//THONG BAO THANH CONG
				/*CAP NHAT LAI TRANG THAI CAC HD LIEN QUAN*/
				
				if(TTTNCCQT.equals("1")) {
				String TCTBao = "";
				Document docFilter = null;
				List<WriteModel<Document>> ous = new ArrayList<WriteModel<Document>>();
				UpdateOptions uo = new UpdateOptions();
				uo.upsert(false);
				
				for(Document doc: docTmp.getList("DSHDon", Document.class)) {
					TCTBao = doc.get("TCTBao", "");
					docFilter = new Document("IssuerId", header.getIssuerId())
							//.append("HDSS", new Document("$exists", false))
							.append("MCCQT", doc.get("MCQTCap", ""));
					if("1".equals(TCTBao)) {
						ous.add(
							new UpdateOneModel<>(
								docFilter, 
								new Document("$set", 
									new Document("EInvoiceStatus", Constants.INVOICE_STATUS.DELETED)
									.append("HDSS", 
										new Document("TCTBao", TCTBao)
										.append("TCTBaoDesc", Constants.MAP_HDSS_TCTBAO.get(TCTBao))
										.append("LDo", doc.get("LDo", ""))
									)
								)
								, 
								uo
							)
						);	
						Document docTmphd = null;
		
						mongoClient = cfg.mongoClient();
						collection = mongoClient.getDatabase(cfg.dbName).getCollection("EInvoiceMTT");
						      try {
						    	  docTmphd = collection.find(docFilter).allowDiskUse(true).iterator().next();    
						      } catch (Exception e) {
						        
						      }
						        
						mongoClient.close();
						
						if(docTmphd!=null) {
							LocalDate ngay = commons.convertStringToLocalDate(LocalDate.now().toString(), "yyyy-MM-dd");
				String shd = commons.formatNumberBillInvoice(
						docTmphd.getEmbedded(Arrays.asList("EInvoiceDetail", "TTChung", "SHDon"), 0)
					);
				String mauhd = docTmphd.getEmbedded(Arrays.asList("EInvoiceDetail", "TTChung","KHMSHDon"), "") + docTmphd.getEmbedded(Arrays.asList("EInvoiceDetail", "TTChung","KHHDon"), "");
				String _tmp = "";					
				_tmp = 	docTmphd.getEmbedded(Arrays.asList("EInvoiceDetail", "NDHDon","NMua","Ten"), "");
				String mcqt = docTmphd.get("MCCQT", "");
				_title = header.getUserFullName() + " " + "Thông báo hủy/giải trình của NNT Hóa Đơn điện tử có sai sót";		
				StringBuilder sb = new StringBuilder();
				sb.setLength(0);
				sb.append("<p><span style='font-family: Times New Roman;font-size: 13px;'>Kính gửi: <label style='font-weight: bold;'>" + ("".equals(_tmp)? "Quý khách hàng": _tmp) + "</label><o:p></o:p></span></p>\n");
				sb.append("<p><span style='font-family: Times New Roman;font-size: 13px;'>" + header.getUserFullName() + " xin trân trọng thông báo đến Quý Khách giải trình về việc hóa đơn điện tử có sai sót</span></p>\n");
				sb.append("<p><span style='font-family: Times New Roman;font-size: 13px;'><label style='font-weight: bold;'>Thông tin hóa đơn có sai sót</label></span></p>\n");
		
				sb.append("<p><span style='font-family: Times New Roman;font-size: 13px;'>1.  Số hóa đơn:  " + shd + "</span></p>\n");
				sb.append("<p><span style='font-family: Times New Roman;font-size: 13px;'>2.  Mẫu hoá đơn: " + mauhd + "</span></p>\n");
				sb.append("<p><span style='font-family: Times New Roman;font-size: 13px;'>3.  Mã của CƠ QUAN THUẾ: "+mcqt +" </span></p>\n");
				sb.append("<p><span style='font-family: Times New Roman;font-size: 13px;'>4.  Trạng thái:<label style='font-weight: bold;color:red;'>Đã xóa bỏ</label></span></p>\n");
				sb.append("<p><span style='font-family: Times New Roman;font-size: 13px;'>5.  Thời gian: "+ngay+"</span></p>\n");
				sb.append("<p><span style='font-family: Times New Roman;font-size: 13px;'>Trân trọng kính chào!</span></p>");
				sb.append("<hr style='margin: 5px 0 5px 0;'>");
				sb.append("<p style='margin-bottom: 3px;'><span style='font-family: Times New Roman;font-size: 13px;color:red;font-weight: bold;'>QUÝ KHÁCH HÀNG VUI LÒNG KHÔNG REPLY EMAIL NÀY!</span></p>");
				
				_content = 	sb.toString();
				String m1 = docTmphd.getEmbedded(Arrays.asList("EInvoiceDetail", "NDHDon","NMua","DCTDTuCC"), "");
				String m2 = docTmphd.getEmbedded(Arrays.asList("EInvoiceDetail", "NDHDon","NMua","DCTDTu"), "");
				
				
				
				//DM USER CONFIG
				Document docFindUserConfig = new Document("IssuerId", header.getIssuerId());
				Document docTmpUserConfig = null;
				
				mongoClient = cfg.mongoClient();
				collection = mongoClient.getDatabase(cfg.dbName).getCollection("UserConFig");
				  try {
					  docTmpUserConfig = collection.find(docFindUserConfig).allowDiskUse(true).iterator().next();      
					 } catch (Exception ex) {
						
					}
				mongoClient.close();

								
				
				//DM FOOTER MAIL 
				Document docFindFooter = new Document("IsActive", true).append("IsDelete", new Document("$ne", true));
				Document docTmpFooter = null;
				
				mongoClient = cfg.mongoClient();
				collection = mongoClient.getDatabase(cfg.dbName).getCollection("DMFooterWeb");
				  try {
					  docTmpFooter = collection.find(docFindFooter).allowDiskUse(true).iterator().next();      
					 } catch (Exception ex) {
						
					}
				mongoClient.close();

				//
				String CheckFooterMail = "";
				if(docTmpUserConfig ==null) {
					 CheckFooterMail = "N";					
				}else {
					 CheckFooterMail = docTmpUserConfig.get("footermail", "");
				}
			
				//
				//
				
				if(!CheckFooterMail.equals("Y")) {
					_content = commons.decodeURIComponent(_content);
					
					if(docTmpFooter==null) {
						_content = commons.decodeURIComponent(_content);	
					}else {									
					String noidung = docTmpFooter.get("Noidung", "");
					_content += noidung;
					}
				}
				else {
					_content = commons.decodeURIComponent(_content);
				}
				
				Document docFindem = new Document("IssuerId", header.getIssuerId());
				Document docTmpem = null;
				
				mongoClient = cfg.mongoClient();
				collection = mongoClient.getDatabase(cfg.dbName).getCollection("ConfigEmail");
				  try {
					  docTmpem = collection.find(docFindem).allowDiskUse(true).iterator().next();      
					 } catch (Exception ex) {
						
					}
				mongoClient.close();

				
					if(docTmpem != null) {
				MailConfig mailConfig = new MailConfig(docTmpem);
				mailConfig.setNameSend(docTmphd.getEmbedded(Arrays.asList("EInvoiceDetail", "NDHDon","NBan","Ten"), ""));
				/*THUC HIEN GUI MAIL*/
				Document docFindMailjet = new Document("IsActive", true);
				Document docTmpMailjet = null;
						
						mongoClient = cfg.mongoClient();
						collection = mongoClient.getDatabase(cfg.dbName).getCollection("ConfigMailJet");
						  try {
							  docTmpMailjet = collection.find(docFindMailjet).allowDiskUse(true).iterator().next();      
							 } catch (Exception ex) {
								
							}
						mongoClient.close();

						
						//String doctmp_id = docTmphd.getObjectId("_id").toString();
						//dir = docTmphd.getString("Dir");
						String MailJet = docTmpem.get("MailJet", "");
						String fileName_ = _id +"_signed.xml";
						File file_ = null;
						String fileNamePDF = _id + ".pdf";
						file_ = new File(dir1, fileName_);
						
						if (file_.exists() && file_.isFile()) {
							org.w3c.dom.Document doc_ = commons.fileToDocument(file_);
							String fileNameJP = "04SS.jrxml";
							int numberRowInPage = 5;
							int numberRowInPageMultiPage =  15;
							int numberCharsInRow = 50;
							String ImgLogo = "";
					        String ImgBackground = "";
							File fileJP = new File(SystemParams.DIR_E_INVOICE_TEMPLATE, fileNameJP);
							ByteArrayOutputStream baosPDF = null;
							
							baosPDF = jpUtils.print04(fileJP, doc_,docTmp, numberRowInPage, numberRowInPageMultiPage,
									numberCharsInRow, Paths.get(SystemParams.DIR_E_INVOICE_TEMPLATE, "images", (String)docTmp.getEmbedded(Arrays.asList("Issuer", "TaxCode"), ""), ImgLogo).toString(), Paths.get(SystemParams.DIR_E_INVOICE_TEMPLATE, "images", (String)docTmp.getEmbedded(Arrays.asList("Issuer", "TaxCode"), ""), ImgBackground).toString(),
									false);
							/* LUU TAP TIN PDF */
							if (null != baosPDF) {
								try (OutputStream fileOuputStream = new FileOutputStream(new File(dir, fileNamePDF))) {
									baosPDF.writeTo(fileOuputStream);
								} catch (IOException e) {
									e.printStackTrace();
								}
							}
						}
						file_ = null;
						List<String> listFiles = new ArrayList<>();
						List<String> listNames = new ArrayList<>();
						file_ = new File(dir, fileNamePDF);
						if (file_.exists() && file_.isFile()) {
							listFiles.add(file_.toString());
							listNames.add(mauhd + "-" + shd + ".pdf");
						}
						
						boolean sendmail = false;						
						
//				if(_email != "" || _email != "," ) {
					
						if(m2!="" || m1 != "") {
							
							if(m2!="" && m1 =="") {
								
								_email = m2; 	
							}
							else if(m2 == "" && m1!="") {
								_email = m1;	
							}
							else {
								_email = m1 + "," + m2;
							}	
//					if(MailJet.equals("Y")&&!MailJet.equals("")&&!MailJet.equals("N")) {				
//						//
//						String ApiKey = docTmpMailjet.getString("ApiKey");
//						String SecretKey = docTmpMailjet.getString("SecretKey");
//						String EmailAddress = docTmpMailjet.getString("EmailAddress");
//						mailConfig.setEmailAddress(ApiKey);
//						mailConfig.setEmailPassword(SecretKey);
//						mailConfig.setSmtpServer(EmailAddress);
//						 sendmail = mailJet.sendMailJet(mailConfig, _title, _content, _email, listFiles, listNames, true);				 
//						}else{
//							 sendmail = mailUtils.sendMail(mailConfig, _title, _content, _email, listFiles, listNames, true);
//						}
//					try {
//						mongoTemplate.getCollection("LogEmailUser").insertOne(
//							new Document("IssuerId", header.getIssuerId())
//							.append("Title", _title)
//							.append("Email", _email)
//							.append("IsActive", sendmail)
//							.append("MailCheck", sendmail)
//							.append("IsDelete", false)
//							.append("EmailContent", _content)
//							
//						);
//					}catch(Exception ex) {}
						
						}
				}
				
				}
				
				///END SEARCH INFORMATION OF EINVOICE
				
					}else {
						ous.add(
							new UpdateOneModel<>(
								docFilter, 
								new Document("$set", 
									new Document("HDSS", 
										new Document("TCTBao", TCTBao)
										.append("TCTBaoDesc", Constants.MAP_HDSS_TCTBAO.get(TCTBao))
										.append("LDo", doc.get("LDo", ""))
									)	
								), 
								uo
							)
						);
					}
					String lido = doc.get("LDo", "");
					String tctb = Constants.MAP_HDSS_TCTBAO.get(TCTBao);
					Document docTmphd = null;
			
					mongoClient = cfg.mongoClient();
					collection = mongoClient.getDatabase(cfg.dbName).getCollection("EInvoiceMTT");
					  try {
						  docTmphd = collection.find(docFilter).allowDiskUse(true).iterator().next();      
						 } catch (Exception ex) {
							
						}
					mongoClient.close();

			
					if(docTmphd!=null) {
						LocalDate ngay = commons.convertStringToLocalDate(LocalDate.now().toString(), "yyyy-MM-dd");
						String shd = commons.formatNumberBillInvoice(
								docTmphd.getEmbedded(Arrays.asList("EInvoiceDetail", "TTChung", "SHDon"), 0)
							);
						String mauhd = docTmphd.getEmbedded(Arrays.asList("EInvoiceDetail", "TTChung","KHMSHDon"), "") + docTmphd.getEmbedded(Arrays.asList("EInvoiceDetail", "TTChung","KHHDon"), "");
						String _tmp = "";					
						_tmp = 	docTmphd.getEmbedded(Arrays.asList("EInvoiceDetail", "NDHDon","NMua","Ten"), "");
						String mcqt = docTmphd.get("MCCQT", "");
						_title = header.getUserFullName() + " " + "Thông báo hủy/giải trình của NNT Hóa Đơn điện tử có sai sót";		
						StringBuilder sb = new StringBuilder();
						sb.setLength(0);
						sb.append("<p><span style='font-family: Times New Roman;font-size: 13px;'>Kính gửi: <label style='font-weight: bold;'>" + ("".equals(_tmp)? "Quý khách hàng": _tmp) + "</label><o:p></o:p></span></p>\n");
						sb.append("<p><span style='font-family: Times New Roman;font-size: 13px;'>" + header.getUserFullName() + " xin trân trọng thông báo đến Quý Khách giải trình về việc hóa đơn điện tử có sai sót</span></p>\n");
						sb.append("<p><span style='font-family: Times New Roman;font-size: 13px;'><label style='font-weight: bold;'>Thông tin hóa đơn có sai sót</label></span></p>\n");
				
						sb.append("<p><span style='font-family: Times New Roman;font-size: 13px;'>1.  Số hóa đơn:  " + shd + "</span></p>\n");
						sb.append("<p><span style='font-family: Times New Roman;font-size: 13px;'>2.  Mẫu hoá đơn: " + mauhd + "</span></p>\n");
						sb.append("<p><span style='font-family: Times New Roman;font-size: 13px;'>3.  Mã của CƠ QUAN THUẾ: "+mcqt +" </span></p>\n");
						sb.append("<p><span style='font-family: Times New Roman;font-size: 13px;'>4.  Trạng thái:<label style='font-weight: bold;'>"+tctb+"</label></span></p>\n");
						sb.append("<p><span style='font-family: Times New Roman;font-size: 13px;'>5.  Lí do:<label style='font-weight: bold;'>"+lido+"</label></span></p>\n");
						sb.append("<p><span style='font-family: Times New Roman;font-size: 13px;'>6.  Thời gian: "+ngay+"</span></p>\n");
						sb.append("<p><span style='font-family: Times New Roman;font-size: 13px;'>Trân trọng kính chào!</span></p>");
						sb.append("<hr style='margin: 5px 0 5px 0;'>");
						sb.append("<p style='margin-bottom: 3px;'><span style='font-family: Times New Roman;font-size: 13px;color:red;font-weight: bold;'>QUÝ KHÁCH HÀNG VUI LÒNG KHÔNG REPLY EMAIL NÀY!</span></p>");
						
						_content = 	sb.toString();
						String m1 = docTmphd.getEmbedded(Arrays.asList("EInvoiceDetail", "NDHDon","NMua","DCTDTuCC"), "");
						String m2 = docTmphd.getEmbedded(Arrays.asList("EInvoiceDetail", "NDHDon","NMua","DCTDTu"), "");

						

						//DM USER CONFIG
								Document docFindUserConfig = new Document("IssuerId", header.getIssuerId());
								Document docTmpUserConfig = null;								
								
								mongoClient = cfg.mongoClient();
								collection = mongoClient.getDatabase(cfg.dbName).getCollection("UserConFig");
								  try {
									  docTmpUserConfig = collection.find(docFindUserConfig).allowDiskUse(true).iterator().next();      
									 } catch (Exception ex) {
										
									}
								mongoClient.close();

												
								
								//DM FOOTER MAIL 
								Document docFindFooter = new Document("IsActive", true).append("IsDelete", new Document("$ne", true));
								Document docTmpFooter = null;
								
								mongoClient = cfg.mongoClient();
								collection = mongoClient.getDatabase(cfg.dbName).getCollection("DMFooterWeb");
								  try {
									  docTmpFooter = collection.find(docFindFooter).allowDiskUse(true).iterator().next();      
									 } catch (Exception ex) {
										
									}
								mongoClient.close();

								
								//
								String CheckFooterMail = "";
								if(docTmpUserConfig ==null) {
									 CheckFooterMail = "N";					
								}else {
									 CheckFooterMail = docTmpUserConfig.get("footermail", "");
								}
							
								//
								//
								
								if(!CheckFooterMail.equals("Y")) {
									_content = commons.decodeURIComponent(_content);
									
									if(docTmpFooter==null) {
										_content = commons.decodeURIComponent(_content);	
									}else {									
									String noidung = docTmpFooter.get("Noidung", "");
									_content += noidung;
									}
								}
								else {
									_content = commons.decodeURIComponent(_content);
								}
						
						
						Document docFindem = new Document("IssuerId", header.getIssuerId());
						Document docTmpem = null;
		
						mongoClient = cfg.mongoClient();
						collection = mongoClient.getDatabase(cfg.dbName).getCollection("ConfigEmail");
						  try {
							  docTmpem = collection.find(docFindem).allowDiskUse(true).iterator().next();      
							 } catch (Exception ex) {
								
							}
						mongoClient.close();

						
							if(docTmpem != null) {
						MailConfig mailConfig = new MailConfig(docTmpem);
						mailConfig.setNameSend(docTmphd.getEmbedded(Arrays.asList("EInvoiceDetail", "NDHDon","NBan","Ten"), ""));
						/*THUC HIEN GUI MAIL*/
						Document docFindMailjet = new Document("IsActive", true);
						Document docTmpMailjet = null;
								
								mongoClient = cfg.mongoClient();
								collection = mongoClient.getDatabase(cfg.dbName).getCollection("ConfigMailJet");
								  try {
									  docTmpMailjet = collection.find(docFindMailjet).allowDiskUse(true).iterator().next();      
									 } catch (Exception ex) {
										
									}
								mongoClient.close();

								
								//String doctmp_id = docTmphd.getObjectId("_id").toString();
								//dir = docTmphd.getString("Dir");
								String MailJet = docTmpem.get("MailJet", "");	
								String fileName_ = _id +"_signed.xml";
								File file_ = null;
								String fileNamePDF = _id + ".pdf";
								file_ = new File(dir1, fileName_);
								
								if (file_.exists() && file_.isFile()) {
									org.w3c.dom.Document doc_ = commons.fileToDocument(file_);
									String fileNameJP = "04SS.jrxml";
									int numberRowInPage = 5;
									int numberRowInPageMultiPage =  15;
									int numberCharsInRow = 50;
									String ImgLogo = "";
							        String ImgBackground = "";
									File fileJP = new File(SystemParams.DIR_E_INVOICE_TEMPLATE, fileNameJP);
									ByteArrayOutputStream baosPDF = null;
									
									baosPDF = jpUtils.print04(fileJP, doc_,docTmp, numberRowInPage, numberRowInPageMultiPage,
											numberCharsInRow, Paths.get(SystemParams.DIR_E_INVOICE_TEMPLATE, "images", (String)docTmp.getEmbedded(Arrays.asList("Issuer", "TaxCode"), ""), ImgLogo).toString(), Paths.get(SystemParams.DIR_E_INVOICE_TEMPLATE, "images", (String)docTmp.getEmbedded(Arrays.asList("Issuer", "TaxCode"), ""), ImgBackground).toString(),
											false);
									/* LUU TAP TIN PDF */
									if (null != baosPDF) {
										try (OutputStream fileOuputStream = new FileOutputStream(new File(dir, fileNamePDF))) {
											baosPDF.writeTo(fileOuputStream);
										} catch (IOException e) {
											e.printStackTrace();
										}
									}
								}
								file_ = null;
								List<String> listFiles = new ArrayList<>();
								List<String> listNames = new ArrayList<>();
								file_ = new File(dir, fileNamePDF);
								if (file_.exists() && file_.isFile()) {
									listFiles.add(file_.toString());
									listNames.add(mauhd + "-" + shd + ".pdf");
								}
								
								boolean sendmail = false;		
								if(m2!="" || m1 != "") {
									
									if(m2!="" && m1 =="") {
										
										_email = m2; 	
									}
									else if(m2 == "" && m1!="") {
										_email = m1;	
									}
									else {
										_email = m2 + "," + m1;
									}
							
							if(MailJet.equals("Y")&&!MailJet.equals("")&&!MailJet.equals("N")) {				
							//
							String ApiKey = docTmpMailjet.getString("ApiKey");
							String SecretKey = docTmpMailjet.getString("SecretKey");
							String EmailAddress = docTmpMailjet.getString("EmailAddress");
							mailConfig.setEmailAddress(ApiKey);
							mailConfig.setEmailPassword(SecretKey);
							mailConfig.setSmtpServer(EmailAddress);
							 sendmail = mailJet.sendMailJet(mailConfig, _title, _content, _email, listFiles, listNames, true);				 
							}else{
								 sendmail = mailUtils.sendMail(mailConfig, _title, _content, _email, listFiles, listNames, true);
							}
							try {
								mongoClient = cfg.mongoClient();
								collection = mongoClient.getDatabase(cfg.dbName).getCollection("LogEmailUser");
								collection.insertOne(new Document("IssuerId", header.getIssuerId())
										.append("Title", _title)
										.append("Email", _email)
										.append("IsActive", sendmail)
										.append("MailCheck", sendmail)
										.append("IsDelete", false)
										.append("EmailContent", _content));      
								mongoClient.close();
								
							}catch(Exception ex) {}
						}
					
							}
					}
				
				}
				
				mongoClient = cfg.mongoClient();
				collection = mongoClient.getDatabase(cfg.dbName).getCollection("EInvoiceMTT");
				collection.bulkWrite(ous,
						new BulkWriteOptions().ordered(false)
						); 
				  mongoClient.close();
			
				
				/*CAP NHAT TRANG THAI COMPLETE - TRANG THAI CQT*/
				options = new FindOneAndUpdateOptions();
				options.upsert(false);
				options.maxTime(5000, TimeUnit.MILLISECONDS);
				options.returnDocument(ReturnDocument.AFTER);
				
				mongoClient = cfg.mongoClient();
				collection = mongoClient.getDatabase(cfg.dbName).getCollection("EInvoiceHDSSMTT");
				collection.findOneAndUpdate(docFind, 
					new Document("$set", 
						new Document("Status", Constants.INVOICE_STATUS.COMPLETE)
						.append("MTDTChieu", MTDTChieu)
					), 
					options
				);
				mongoClient.close();
				} else /* if(TTTNCCQT.equals("2")) */{
					
					hItem = new HashMap<String, String>();
					hItem.put("MLoi", commons.getTextFromNodeXML((Element) xPath.evaluate("DLieu/TBao/DLTBao/DSHDon/HDon/DSLDKTNhan/LDo/MLoi", nodeTDiep, XPathConstants.NODE)));
					hItem.put("MTLoi", commons.getTextFromNodeXML((Element) xPath.evaluate("DLieu/TBao/DLTBao/DSHDon/HDon/DSLDKTNhan/LDo/MTa", nodeTDiep, XPathConstants.NODE)));
					DSLoi.add(hItem);
				
				/*CAP NHAT TRANG THAI COMPLETE - TRANG THAI CQT*/
				options = new FindOneAndUpdateOptions();
				options.upsert(false);
				options.maxTime(5000, TimeUnit.MILLISECONDS);
				options.returnDocument(ReturnDocument.AFTER);
				
				MongoClient mongoClient1 = cfg.mongoClient();
				collection = mongoClient1.getDatabase(cfg.dbName).getCollection("EInvoiceHDSSMTT");
				collection.findOneAndUpdate(docFind,		 
						new Document("$set", 
							new Document("Status", Constants.INVOICE_STATUS.ERROR_CQT)
							.append("DSLoi", DSLoi)
						), 
						options
					);
				mongoClient1.close();
				
				
				responseStatus = new MspResponseStatus(0, Constants.MAP_ERROR.get(0));
				rsp.setResponseStatus(responseStatus);
				return rsp;
				}
				responseStatus = new MspResponseStatus(0, Constants.MAP_ERROR.get(0));
				rsp.setResponseStatus(responseStatus);
				return rsp;
			}else {
				nodeList = (NodeList) xPath.evaluate("DLieu/TBao/DLTBao/KHLKhac", nodeTDiep, XPathConstants.NODESET);
				for(int i = 0; i < nodeList.getLength(); i++) {
					node = nodeList.item(i);
					hItem = new HashMap<String, String>();
					hItem.put("MLoi", commons.getTextFromNodeXML((Element) xPath.evaluate("DSLDo/LDo/MLoi", node, XPathConstants.NODE)));
					hItem.put("MTLoi", commons.getTextFromNodeXML((Element) xPath.evaluate("DSLDo/LDo/MTLoi", node, XPathConstants.NODE)));
					DSLoi.add(hItem);
				}
				
				/*CAP NHAT TRANG THAI COMPLETE - TRANG THAI CQT*/
				options = new FindOneAndUpdateOptions();
				options.upsert(false);
				options.maxTime(5000, TimeUnit.MILLISECONDS);
				options.returnDocument(ReturnDocument.AFTER);
				
				
				MongoClient mongoClient1 = cfg.mongoClient();
				collection = mongoClient1.getDatabase(cfg.dbName).getCollection("EInvoiceHDSSMTT");
				collection.findOneAndUpdate(docFind,
					new Document("$set", 
						new Document("Status", Constants.INVOICE_STATUS.ERROR_CQT)
						.append("DSLoi", DSLoi)
					), 
					options
				);
				mongoClient1.close();	
				
				responseStatus = new MspResponseStatus(0, Constants.MAP_ERROR.get(0));
				rsp.setResponseStatus(responseStatus);
				return rsp;
			}
		}
	}
		
		
		//MA 301--------------------------------
		else if("301".equals(CQT_MLTDiep)){		
			
			if(TTTNCCQT.equals("1"))
			{
			String TCTBao = "";
			Document docFilter = null;
			List<WriteModel<Document>> ous = new ArrayList<WriteModel<Document>>();
			UpdateOptions uo = new UpdateOptions();
			uo.upsert(false);
			
			for(Document doc: docTmp.getList("DSHDon", Document.class)) {
				TCTBao = doc.get("TCTBao", "");
				docFilter = new Document("IssuerId", header.getIssuerId())
						//.append("HDSS", new Document("$exists", false))
						.append("MCCQT", doc.get("MCQTCap", ""));
				if("1".equals(TCTBao)) {
					ous.add(
						new UpdateOneModel<>(
							docFilter, 
							new Document("$set", 
								new Document("EInvoiceStatus", Constants.INVOICE_STATUS.DELETED)
								.append("HDSS", 
									new Document("TCTBao", TCTBao)
									.append("TCTBaoDesc", Constants.MAP_HDSS_TCTBAO.get(TCTBao))
									.append("LDo", doc.get("LDo", ""))
								)
							)
							, 
							uo
						)
					);	
					Document docTmphd = null;
					mongoClient = cfg.mongoClient();
					collection = mongoClient.getDatabase(cfg.dbName).getCollection("EInvoiceMTT");
					  try {
						  docTmphd = collection.find(docFilter).allowDiskUse(true).iterator().next();      
						 } catch (Exception ex) {
							
						}
					mongoClient.close();
					
					
					if(docTmphd!=null) {
						LocalDate ngay = commons.convertStringToLocalDate(LocalDate.now().toString(), "yyyy-MM-dd");
			String shd = commons.formatNumberBillInvoice(
					docTmphd.getEmbedded(Arrays.asList("EInvoiceDetail", "TTChung", "SHDon"), 0)
				);
			String mauhd = docTmphd.getEmbedded(Arrays.asList("EInvoiceDetail", "TTChung","KHMSHDon"), "") + docTmphd.getEmbedded(Arrays.asList("EInvoiceDetail", "TTChung","KHHDon"), "");
			String _tmp = "";					
			_tmp = 	docTmphd.getEmbedded(Arrays.asList("EInvoiceDetail", "NDHDon","NMua","Ten"), "");
			String mcqt = docTmphd.get("MCCQT", "");
			_title = header.getUserFullName() + " " + "Thông báo hủy/giải trình của NNT Hóa Đơn điện tử có sai sót";		
			StringBuilder sb = new StringBuilder();
			sb.setLength(0);
			sb.append("<p><span style='font-family: Times New Roman;font-size: 13px;'>Kính gửi: <label style='font-weight: bold;'>" + ("".equals(_tmp)? "Quý khách hàng": _tmp) + "</label><o:p></o:p></span></p>\n");
			sb.append("<p><span style='font-family: Times New Roman;font-size: 13px;'>" + header.getUserFullName() + " xin trân trọng thông báo đến Quý Khách giải trình về việc hóa đơn điện tử có sai sót</span></p>\n");
			sb.append("<p><span style='font-family: Times New Roman;font-size: 13px;'><label style='font-weight: bold;'>Thông tin hóa đơn có sai sót</label></span></p>\n");
	
			sb.append("<p><span style='font-family: Times New Roman;font-size: 13px;'>1.  Số hóa đơn:  " + shd + "</span></p>\n");
			sb.append("<p><span style='font-family: Times New Roman;font-size: 13px;'>2.  Mẫu hoá đơn: " + mauhd + "</span></p>\n");
			sb.append("<p><span style='font-family: Times New Roman;font-size: 13px;'>3.  Mã của CƠ QUAN THUẾ: "+mcqt +" </span></p>\n");
			sb.append("<p><span style='font-family: Times New Roman;font-size: 13px;'>4.  Trạng thái:<label style='font-weight: bold;color:red;'>Đã xóa bỏ</label></span></p>\n");
			sb.append("<p><span style='font-family: Times New Roman;font-size: 13px;'>5.  Thời gian: "+ngay+"</span></p>\n");
			sb.append("<p><span style='font-family: Times New Roman;font-size: 13px;'>Trân trọng kính chào!</span></p>");
			sb.append("<hr style='margin: 5px 0 5px 0;'>");
			sb.append("<p style='margin-bottom: 3px;'><span style='font-family: Times New Roman;font-size: 13px;color:red;font-weight: bold;'>QUÝ KHÁCH HÀNG VUI LÒNG KHÔNG REPLY EMAIL NÀY!</span></p>");
			
			_content = 	sb.toString();
			String m1 = docTmphd.getEmbedded(Arrays.asList("EInvoiceDetail", "NDHDon","NMua","DCTDTuCC"), "");
			String m2 = docTmphd.getEmbedded(Arrays.asList("EInvoiceDetail", "NDHDon","NMua","DCTDTu"), "");

			

			//DM USER CONFIG
					Document docFindUserConfig = new Document("IssuerId", header.getIssuerId());
					Document docTmpUserConfig = null;
					
					mongoClient = cfg.mongoClient();
					collection = mongoClient.getDatabase(cfg.dbName).getCollection("UserConFig");
					  try {
						  docTmpUserConfig = collection.find(docFindUserConfig).allowDiskUse(true).iterator().next();      
						 } catch (Exception ex) {
							
						}
					mongoClient.close();
									
					
					//DM FOOTER MAIL 
					Document docFindFooter = new Document("IsActive", true).append("IsDelete", new Document("$ne", true));
					Document docTmpFooter = null;		
					
					mongoClient = cfg.mongoClient();
					collection = mongoClient.getDatabase(cfg.dbName).getCollection("DMFooterWeb");
					  try {
						  docTmpFooter = collection.find(docFindFooter).allowDiskUse(true).iterator().next();      
						 } catch (Exception ex) {
							
						}
					mongoClient.close();
					
					//
					String CheckFooterMail = "";
					if(docTmpUserConfig ==null) {
						 CheckFooterMail = "N";					
					}else {
						 CheckFooterMail = docTmpUserConfig.get("footermail", "");
					}
				
					//
					//
					
					if(!CheckFooterMail.equals("Y")) {
						_content = commons.decodeURIComponent(_content);
						
						if(docTmpFooter==null) {
							_content = commons.decodeURIComponent(_content);	
						}else {									
						String noidung = docTmpFooter.get("Noidung", "");
						_content += noidung;
						}
					}
					else {
						_content = commons.decodeURIComponent(_content);
					}
			
			
			Document docFindem = new Document("IssuerId", header.getIssuerId());
			Document docTmpem = null;

			mongoClient = cfg.mongoClient();
			collection = mongoClient.getDatabase(cfg.dbName).getCollection("ConfigEmail");
			  try {
				  docTmpem = collection.find(docFindem).allowDiskUse(true).iterator().next();      
				 } catch (Exception ex) {
					
				}
			mongoClient.close();
			
				if(docTmpem != null) {
			MailConfig mailConfig = new MailConfig(docTmpem);
			mailConfig.setNameSend(docTmphd.getEmbedded(Arrays.asList("EInvoiceDetail", "NDHDon","NBan","Ten"), ""));
				
			/*THUC HIEN GUI MAIL*/
			Document docFindMailjet = new Document("IsActive", true);
			Document docTmpMailjet = null;
		
					mongoClient = cfg.mongoClient();
					collection = mongoClient.getDatabase(cfg.dbName).getCollection("ConfigMailJet");
					  try {
						  docTmpMailjet = collection.find(docFindMailjet).allowDiskUse(true).iterator().next();      
						 } catch (Exception ex) {
							
						}
					mongoClient.close();
					//String doctmp_id = docTmphd.getObjectId("_id").toString();
					//dir = docTmphd.getString("Dir");
					String MailJet = docTmpem.get("MailJet", "");
					String fileName_ = _id +"_signed.xml";
					File file_ = null;
					String fileNamePDF = _id + ".pdf";
					file_ = new File(dir1, fileName_);
					
					if (file_.exists() && file_.isFile()) {
						org.w3c.dom.Document doc_ = commons.fileToDocument(file_);
						String fileNameJP = "04SS.jrxml";
						int numberRowInPage = 5;
						int numberRowInPageMultiPage =  15;
						int numberCharsInRow = 50;
						String ImgLogo = "";
				        String ImgBackground = "";
						File fileJP = new File(SystemParams.DIR_E_INVOICE_TEMPLATE, fileNameJP);
						ByteArrayOutputStream baosPDF = null;
						
						baosPDF = jpUtils.print04(fileJP, doc_,docTmp, numberRowInPage, numberRowInPageMultiPage,
								numberCharsInRow, Paths.get(SystemParams.DIR_E_INVOICE_TEMPLATE, "images", (String)docTmp.getEmbedded(Arrays.asList("Issuer", "TaxCode"), ""), ImgLogo).toString(), Paths.get(SystemParams.DIR_E_INVOICE_TEMPLATE, "images", (String)docTmp.getEmbedded(Arrays.asList("Issuer", "TaxCode"), ""), ImgBackground).toString(),
								false);
						/* LUU TAP TIN PDF */
						if (null != baosPDF) {
							try (OutputStream fileOuputStream = new FileOutputStream(new File(dir, fileNamePDF))) {
								baosPDF.writeTo(fileOuputStream);
							} catch (IOException e) {
								e.printStackTrace();
							}
						}
					}
					file_ = null;
					List<String> listFiles = new ArrayList<>();
					List<String> listNames = new ArrayList<>();
					file_ = new File(dir, fileNamePDF);
					if (file_.exists() && file_.isFile()) {
						listFiles.add(file_.toString());
						listNames.add(mauhd + "-" + shd + ".pdf");
					}
					
					boolean sendmail = false;	
					if(m2!="" || m1 != "") {
						
						if(m2!="" && m1 =="") {
							
							_email = m2; 	
						}
						else if(m2 == "" && m1!="") {
							_email = m1;	
						}
						else {
							_email = m1 + "," + m2;
						}
//				if(MailJet.equals("Y")&&!MailJet.equals("")&&!MailJet.equals("N")) {				
//					//
//					String ApiKey = docTmpMailjet.getString("ApiKey");
//					String SecretKey = docTmpMailjet.getString("SecretKey");
//					String EmailAddress = docTmpMailjet.getString("EmailAddress");
//					mailConfig.setEmailAddress(ApiKey);
//					mailConfig.setEmailPassword(SecretKey);
//					mailConfig.setSmtpServer(EmailAddress);
//					 sendmail = mailJet.sendMailJet(mailConfig, _title, _content, _email, listFiles, listNames, true);				 
//					}else{
//						 sendmail = mailUtils.sendMail(mailConfig, _title, _content, _email, listFiles, listNames, true);
//					}
//				
//				try {
//					mongoTemplate.getCollection("LogEmailUser").insertOne(
//						new Document("IssuerId", header.getIssuerId())
//						.append("Title", _title)
//						.append("Email", _email)
//						.append("IsActive", sendmail)
//						.append("MailCheck", sendmail)
//						.append("IsDelete", false)
//						.append("EmailContent", _content)
//						
//					);
//				}catch(Exception ex) {}
					}
			}
			
			}
			
			
			///END SEARCH INFORMATION OF EINVOICE
			
				}else {
					ous.add(
						new UpdateOneModel<>(
							docFilter, 
							new Document("$set", 
								new Document("HDSS", 
									new Document("TCTBao", TCTBao)
									.append("TCTBaoDesc", Constants.MAP_HDSS_TCTBAO.get(TCTBao))
									.append("LDo", doc.get("LDo", ""))
								)	
							), 
							uo
						)
					);
				}
				String lido = doc.get("LDo", "");
				String tctb = Constants.MAP_HDSS_TCTBAO.get(TCTBao);
				Document docTmphd = null;
		
				mongoClient = cfg.mongoClient();
				collection = mongoClient.getDatabase(cfg.dbName).getCollection("EInvoiceMTT");
				  try {
					  docTmphd = collection.find(docFilter).allowDiskUse(true).iterator().next();      
					 } catch (Exception ex) {
						
					}
				mongoClient.close();
			
				if(docTmphd!=null) {
					LocalDate ngay = commons.convertStringToLocalDate(LocalDate.now().toString(), "yyyy-MM-dd");
					String shd = commons.formatNumberBillInvoice(
							docTmphd.getEmbedded(Arrays.asList("EInvoiceDetail", "TTChung", "SHDon"), 0)
						);
					String mauhd = docTmphd.getEmbedded(Arrays.asList("EInvoiceDetail", "TTChung","KHMSHDon"), "") + docTmphd.getEmbedded(Arrays.asList("EInvoiceDetail", "TTChung","KHHDon"), "");
					String _tmp = "";					
					_tmp = 	docTmphd.getEmbedded(Arrays.asList("EInvoiceDetail", "NDHDon","NMua","Ten"), "");
					String mcqt = docTmphd.get("MCCQT", "");
					_title = header.getUserFullName() + " " + "Thông báo hủy/giải trình của NNT Hóa Đơn điện tử có sai sót";		
					StringBuilder sb = new StringBuilder();
					sb.setLength(0);
					sb.append("<p><span style='font-family: Times New Roman;font-size: 13px;'>Kính gửi: <label style='font-weight: bold;'>" + ("".equals(_tmp)? "Quý khách hàng": _tmp) + "</label><o:p></o:p></span></p>\n");
					sb.append("<p><span style='font-family: Times New Roman;font-size: 13px;'>" + header.getUserFullName() + " xin trân trọng thông báo đến Quý Khách giải trình về việc hóa đơn điện tử có sai sót</span></p>\n");
					sb.append("<p><span style='font-family: Times New Roman;font-size: 13px;'><label style='font-weight: bold;'>Thông tin hóa đơn có sai sót</label></span></p>\n");
			
					sb.append("<p><span style='font-family: Times New Roman;font-size: 13px;'>1.  Số hóa đơn:  " + shd + "</span></p>\n");
					sb.append("<p><span style='font-family: Times New Roman;font-size: 13px;'>2.  Mẫu hoá đơn: " + mauhd + "</span></p>\n");
					sb.append("<p><span style='font-family: Times New Roman;font-size: 13px;'>3.  Mã của CƠ QUAN THUẾ: "+mcqt +" </span></p>\n");
					sb.append("<p><span style='font-family: Times New Roman;font-size: 13px;'>4.  Trạng thái:<label style='font-weight: bold;'>"+tctb+"</label></span></p>\n");
					sb.append("<p><span style='font-family: Times New Roman;font-size: 13px;'>5.  Lí do:<label style='font-weight: bold;'>"+lido+"</label></span></p>\n");
					sb.append("<p><span style='font-family: Times New Roman;font-size: 13px;'>6.  Thời gian: "+ngay+"</span></p>\n");
					sb.append("<p><span style='font-family: Times New Roman;font-size: 13px;'>Trân trọng kính chào!</span></p>");
					sb.append("<hr style='margin: 5px 0 5px 0;'>");
					sb.append("<p style='margin-bottom: 3px;'><span style='font-family: Times New Roman;font-size: 13px;color:red;font-weight: bold;'>QUÝ KHÁCH HÀNG VUI LÒNG KHÔNG REPLY EMAIL NÀY!</span></p>");
					
					_content = 	sb.toString();
					String m1 = docTmphd.getEmbedded(Arrays.asList("EInvoiceDetail", "NDHDon","NMua","DCTDTuCC"), "");
					String m2 = docTmphd.getEmbedded(Arrays.asList("EInvoiceDetail", "NDHDon","NMua","DCTDTu"), "");

					

					//DM USER CONFIG
							Document docFindUserConfig = new Document("IssuerId", header.getIssuerId());
							Document docTmpUserConfig = null;
				
							mongoClient = cfg.mongoClient();
							collection = mongoClient.getDatabase(cfg.dbName).getCollection("UserConFig");
							  try {
								  docTmpUserConfig = collection.find(docFindUserConfig).allowDiskUse(true).iterator().next();      
								 } catch (Exception ex) {
									
								}
							mongoClient.close();				
							
							//DM FOOTER MAIL 
							Document docFindFooter = new Document("IsActive", true).append("IsDelete", new Document("$ne", true));
							Document docTmpFooter = null;
				
							mongoClient = cfg.mongoClient();
							collection = mongoClient.getDatabase(cfg.dbName).getCollection("DMFooterWeb");
							  try {
								  docTmpFooter = collection.find(docFindFooter).allowDiskUse(true).iterator().next();      
								 } catch (Exception ex) {
									
								}
							mongoClient.close();
							
							//
							String CheckFooterMail = "";
							if(docTmpUserConfig ==null) {
								 CheckFooterMail = "N";					
							}else {
								 CheckFooterMail = docTmpUserConfig.get("footermail", "");
							}
						
							//
							//
							
							if(!CheckFooterMail.equals("Y")) {
								_content = commons.decodeURIComponent(_content);
								
								if(docTmpFooter==null) {
									_content = commons.decodeURIComponent(_content);	
								}else {									
								String noidung = docTmpFooter.get("Noidung", "");
								_content += noidung;
								}
							}
							else {
								_content = commons.decodeURIComponent(_content);
							}
					
					
					Document docFindem = new Document("IssuerId", header.getIssuerId());
					Document docTmpem = null;
	
					mongoClient = cfg.mongoClient();
					collection = mongoClient.getDatabase(cfg.dbName).getCollection("ConfigEmail");
					  try {
						  docTmpem = collection.find(docFindem).allowDiskUse(true).iterator().next();      
						 } catch (Exception ex) {
							
						}
					mongoClient.close();
					
						if(docTmpem != null) {
					MailConfig mailConfig = new MailConfig(docTmpem);
					mailConfig.setNameSend(docTmphd.getEmbedded(Arrays.asList("EInvoiceDetail", "NDHDon","NBan","Ten"), ""));
					/*THUC HIEN GUI MAIL*/
					Document docFindMailjet = new Document("IsActive", true);
					Document docTmpMailjet = null;

							
							mongoClient = cfg.mongoClient();
							collection = mongoClient.getDatabase(cfg.dbName).getCollection("ConfigMailJet");
							  try {
								  docTmpMailjet = collection.find(docFindMailjet).allowDiskUse(true).iterator().next();      
								 } catch (Exception ex) {
									
								}
							mongoClient.close();
							
							//String doctmp_id = docTmphd.getObjectId("_id").toString();
							//dir = docTmphd.getString("Dir");
							String MailJet = docTmpem.get("MailJet", "");	
							String fileName_ = _id +"_signed.xml";
							File file_ = null;
							String fileNamePDF = _id + ".pdf";
							file_ = new File(dir1, fileName_);
							
							if (file_.exists() && file_.isFile()) {
								org.w3c.dom.Document doc_ = commons.fileToDocument(file_);
								String fileNameJP = "04SS.jrxml";
								int numberRowInPage = 5;
								int numberRowInPageMultiPage =  15;
								int numberCharsInRow = 50;
								String ImgLogo = "";
						        String ImgBackground = "";
								File fileJP = new File(SystemParams.DIR_E_INVOICE_TEMPLATE, fileNameJP);
								ByteArrayOutputStream baosPDF = null;
								
								baosPDF = jpUtils.print04(fileJP, doc_,docTmp, numberRowInPage, numberRowInPageMultiPage,
										numberCharsInRow, Paths.get(SystemParams.DIR_E_INVOICE_TEMPLATE, "images", (String)docTmp.getEmbedded(Arrays.asList("Issuer", "TaxCode"), ""), ImgLogo).toString(), Paths.get(SystemParams.DIR_E_INVOICE_TEMPLATE, "images", (String)docTmp.getEmbedded(Arrays.asList("Issuer", "TaxCode"), ""), ImgBackground).toString(),
										false);
								/* LUU TAP TIN PDF */
								if (null != baosPDF) {
									try (OutputStream fileOuputStream = new FileOutputStream(new File(dir, fileNamePDF))) {
										baosPDF.writeTo(fileOuputStream);
									} catch (IOException e) {
										e.printStackTrace();
									}
								}
							}
							file_ = null;
							List<String> listFiles = new ArrayList<>();
							List<String> listNames = new ArrayList<>();
							file_ = new File(dir, fileNamePDF);
							if (file_.exists() && file_.isFile()) {
								listFiles.add(file_.toString());
								listNames.add(mauhd + "-" + shd + ".pdf");
							}
							
							boolean sendmail = false;		
							if(m2!="" || m1 != "") {
								
								if(m2!="" && m1 =="") {
									
									_email = m2; 	
								}
								else if(m2 == "" && m1!="") {
									_email = m1;	
								}
								else {
									_email = m1 + "," + m2;
								}
						
						if(MailJet.equals("Y")&&!MailJet.equals("")&&!MailJet.equals("N")) {				
							//
							String ApiKey = docTmpMailjet.getString("ApiKey");
							String SecretKey = docTmpMailjet.getString("SecretKey");
							String EmailAddress = docTmpMailjet.getString("EmailAddress");
							mailConfig.setEmailAddress(ApiKey);
							mailConfig.setEmailPassword(SecretKey);
							mailConfig.setSmtpServer(EmailAddress);
							 sendmail = mailJet.sendMailJet(mailConfig, _title, _content, _email, listFiles, listNames, true);				 
						}else{
							 sendmail = mailUtils.sendMail(mailConfig, _title, _content, _email, listFiles, listNames, true);
						}
						try {
							
							mongoClient = cfg.mongoClient();
							collection = mongoClient.getDatabase(cfg.dbName).getCollection("LogEmailUser");
							collection.insertOne(new Document("IssuerId", header.getIssuerId())
									.append("Title", _title)
									.append("Email", _email)
									.append("IsActive", sendmail)
									.append("MailCheck", sendmail)
									.append("IsDelete", false)
									.append("EmailContent", _content));      
							mongoClient.close();
							
						}catch(Exception ex) {}
					}
				
						}
				}
		
			}
			
			mongoClient = cfg.mongoClient();
			collection = mongoClient.getDatabase(cfg.dbName).getCollection("EInvoiceMTT");
			collection.bulkWrite(
				ous,
				new BulkWriteOptions().ordered(false)
			);
			mongoClient.close();
			
			/*CAP NHAT TRANG THAI COMPLETE - TRANG THAI CQT*/
			options = new FindOneAndUpdateOptions();
			options.upsert(false);
			options.maxTime(5000, TimeUnit.MILLISECONDS);
			options.returnDocument(ReturnDocument.AFTER);
			
			mongoClient = cfg.mongoClient();
			collection = mongoClient.getDatabase(cfg.dbName).getCollection("EInvoiceHDSSMTT");
			collection.findOneAndUpdate(docFind,
				new Document("$set", 
					new Document("Status", Constants.INVOICE_STATUS.COMPLETE)
					.append("MTDTChieu", MTDTChieu)
				), 
				options
			);
			mongoClient.close();
			  
			responseStatus = new MspResponseStatus(0, Constants.MAP_ERROR.get(0));
			rsp.setResponseStatus(responseStatus);
			return rsp;
			
} else /* if(TTTNCCQT.equals("2")) */{
			
			hItem = new HashMap<String, String>();
			hItem.put("MLoi", commons.getTextFromNodeXML((Element) xPath.evaluate("DLieu/TBao/DLTBao/DSHDon/HDon/DSLDKTNhan/LDo/MLoi", nodeTDiep, XPathConstants.NODE)));
			hItem.put("MTLoi", commons.getTextFromNodeXML((Element) xPath.evaluate("DLieu/TBao/DLTBao/DSHDon/HDon/DSLDKTNhan/LDo/MTa", nodeTDiep, XPathConstants.NODE)));
			DSLoi.add(hItem);
		
		/*CAP NHAT TRANG THAI COMPLETE - TRANG THAI CQT*/
		options = new FindOneAndUpdateOptions();
		options.upsert(false);
		options.maxTime(5000, TimeUnit.MILLISECONDS);
		options.returnDocument(ReturnDocument.AFTER);
		
		
		MongoClient mongoClient1 = cfg.mongoClient();
		collection = mongoClient1.getDatabase(cfg.dbName).getCollection("EInvoiceHDSSMTT");
		collection.findOneAndUpdate(docFind,
				new Document("$set", 
					new Document("Status", Constants.INVOICE_STATUS.ERROR_CQT)
					.append("DSLoi", DSLoi)
				), 
				options
			);
		mongoClient1.close();

		responseStatus = new MspResponseStatus(0, Constants.MAP_ERROR.get(0));
		rsp.setResponseStatus(responseStatus);
		return rsp;
		
	}
		
		}
		else if("999".equals(CQT_MLTDiep)){	
			  TTTNhan = commons.getTextFromNodeXML((Element) xPath.evaluate("DLieu/TBao/TTTNhan", nodeTDiep, XPathConstants.NODE));
			  if(TTTNhan.equals("0")){
					responseStatus = new MspResponseStatus(9999, "CQT đã tiếp nhận.");
					rsp.setResponseStatus(responseStatus);
					return rsp;
			  }
			  else {
					responseStatus = new MspResponseStatus(9999, "Lỗi tiếp nhận dữ liệu.");
					rsp.setResponseStatus(responseStatus);
					return rsp;
			  }
		}
		else {
			responseStatus = new MspResponseStatus(9999, "CQT chưa có thông báo kết quả trả về.");
			rsp.setResponseStatus(responseStatus);
			return rsp;
		}
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
		}catch(Exception e) {}
		
		Document docFind = new Document("IssuerId", header.getIssuerId())
				.append("_id", objectId)
				.append("IsDelete", new Document("$ne", true))
				.append("Status", new Document("$in", Arrays.asList("ERROR_CQT", "PROCESSING","COMPLETE")))
				.append("SignStatusCode", Constants.INVOICE_SIGN_STATUS.SIGNED);
		
		Document docTmp = null;

		MongoClient mongoClient = cfg.mongoClient();
		MongoCollection<Document> collection = mongoClient.getDatabase(cfg.dbName).getCollection("EInvoiceHDSSMTT");
		      try {
		        docTmp = collection.find(docFind).allowDiskUse(true).iterator().next();    
		      } catch (Exception e) {
		        
		      }
		        
		mongoClient.close();
		
		if(null == docTmp) {
			responseStatus = new MspResponseStatus(9999, "Không tìm thấy thông tin thông báo.");
			rsp.setResponseStatus(responseStatus);
			return rsp;
		}
		
		String MTDiep = docTmp.get("MTDiep", "");
		org.w3c.dom.Document rTCTN = tctnService.callTraCuuThongDiep(MTDiep);
//		rTCTN = commons.stringToDocument("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?><KetQuaTraCuu><MaGD>D8DB05DDFFD8425AB665558FEF499EC7</MaGD><MaKetQua>0</MaKetQua><MoTaKetQua>CQT đã trả kết quả xử lý thông điệp</MoTaKetQua><DuLieu><TDiep><TTChung><PBan>2.0.0</PBan><MNGui>TCT</MNGui><MNNhan>V0401486901</MNNhan><MLTDiep>999</MLTDiep><MTDiep>TCT97B381D4211B4DC8ADE43A27BA72048A</MTDiep><MTDTChieu>V0401486901D8DB05DDFFD8425AB665558FEF499EC7</MTDTChieu></TTChung><DLieu><TBao><MTDiep>V0401486901D8DB05DDFFD8425AB665558FEF499EC7</MTDiep><MNGui>V0401486901</MNGui><NNhan>2022-03-22T11:50:45</NNhan><TTTNhan>0</TTTNhan></TBao></DLieu></TDiep><TDiep><TTChung><PBan>2.0.0</PBan><MNGui>TCT</MNGui><MNNhan>V0401486901</MNNhan><MLTDiep>204</MLTDiep><MTDiep>TCT193127A3E1A64029803F59BB157A4EF6</MTDiep><MTDTChieu>V0401486901D8DB05DDFFD8425AB665558FEF499EC7</MTDTChieu><MST>0301521415</MST><SLuong>1</SLuong></TTChung><DLieu><TBao><DLTBao Id=\"Id-c17ac0768edb437b9d76c965c30b3b16\"><MSo>01/TB-KTDL</MSo><Ten>Thông báo về việc kết quả kiểm tra dữ liệu hóa đơn điện tử</Ten><So>220003861409</So><DDanh>Hà Nội</DDanh><NTBao>2022-03-22</NTBao><MST>0301521415</MST><TNNT>CÔNG TY TNHH KIM LOẠI HẠNH PHÚC</TNNT><TGGui>2022-03-22T11:50:46</TGGui><LTBao>2</LTBao><CCu>Thông điệp thông báo hủy/giải trình HDDT có mã/không mã đã lập có sai sót</CCu><SLuong>1</SLuong></DLTBao><DSCKS><CQT><Signature xmlns=\"http://www.w3.org/2000/09/xmldsig#\" Id=\"Tct-71b09cd7304f4ab8ab0a74b7a164f88c\"><SignedInfo><CanonicalizationMethod Algorithm=\"http://www.w3.org/TR/2001/REC-xml-c14n-20010315\" /><SignatureMethod Algorithm=\"http://www.w3.org/2001/04/xmldsig-more#rsa-sha256\" /><Reference URI=\"#Id-c17ac0768edb437b9d76c965c30b3b16\"><DigestMethod Algorithm=\"http://www.w3.org/2001/04/xmlenc#sha256\" /><DigestValue>hpzYcBVfhJipwQN4Z++5Ya/e+0BBumAArubh0dCrSxg=</DigestValue></Reference><Reference URI=\"#SigningTime-a345d862b9c94599bfdc5c5b2bea2be1\"><DigestMethod Algorithm=\"http://www.w3.org/2001/04/xmlenc#sha256\" /><DigestValue>vVGGtDB8sQhnyxL+mnuKMA94/KqiEmVRnX/e4hE0qMI=</DigestValue></Reference></SignedInfo><SignatureValue>domvFsaa5TZdLcm9P881/2lzgviI0Pc+ZXn9kanO6CKmTCR7gwxc00XLLMw0nHG/5jJMkeW+fipNzPIzl4/YbH+XPsNc+E7OK6uJYufPLZ2NfzHKtu1otivsLbHvGVL7+feHA7tAn6eVvjCiv7YVfvyX9cE+WI0s6qVyJHDBZ7Mg3HM19H3y5VpNF4DGFD/kdLQpUXdW78UO/8ulV3yGdGV9EYckqFlptdFrbRSzlxrW5JSyk9rZ7ginJWSp5n65l6UQvu2bYKU331rcRptcZBXBu9h/1Dz7o66nU+IQn8rlkr4FFOM+NJQSxds7lIuEkYuD83q+X1k5/cu9QmRG7A==</SignatureValue><KeyInfo><X509Data><X509SubjectName>CN=TỔNG CỤC THUẾ,O=BỘ TÀI CHÍNH,L=Hà Nội,C=VN</X509SubjectName><X509Certificate>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</X509Certificate></X509Data></KeyInfo><Object Id=\"SigningTime-a345d862b9c94599bfdc5c5b2bea2be1\"><SignatureProperties><SignatureProperty Target=\"signatureProperties\"><SigningTime>2022-03-22T11:50:46</SigningTime></SignatureProperty></SignatureProperties></Object></Signature></CQT></DSCKS></TBao></DLieu></TDiep><TDiep><TTChung><PBan>2.0.0</PBan><MNGui>TCT</MNGui><MNNhan>V0401486901</MNNhan><MLTDiep>301</MLTDiep><MTDiep>TCTA2AAFCCCBD8B4292A7FEA4A1E66E595B</MTDiep><MTDTChieu>V0401486901D8DB05DDFFD8425AB665558FEF499EC7</MTDTChieu><MST>0301521415</MST><SLuong>1</SLuong></TTChung><DLieu><TBao><DLTBao Id=\"Id-2da3b60292a14702aec6cb5b276e7768\"><PBan>2.0.0</PBan><MSo>01/TB-SSĐT</MSo><Ten>Về việc tiếp nhận và kết quả xử lý về việc hóa đơn điện tử đã lập có sai sót</Ten><TCQTCTren>Cục Thuế Thành phố Hồ Chí Minh</TCQTCTren><TCQT>Chi cục Thuế Quận Gò Vấp</TCQT><TNNT>CÔNG TY TNHH KIM LOẠI HẠNH PHÚC</TNNT><MST>0316685293</MST><MGDDTu>V0401486901D8DB05DDFFD8425AB665558FEF499EC7</MGDDTu><TGNhan>2022-03-22</TGNhan><STTThe>1</STTThe><HThuc>KT.Chi cục Trưởng</HThuc><CDanh>Phó Chi cục Trưởng</CDanh></DLTBao><STBao Id=\"Id-6813e99ffeb542b1ae9c2d8bbfab068f\"><So>13657/TB-CCTGV-HDDT</So><NTBao>2022-03-25</NTBao></STBao><DSCKS><TTCQT><Signature xmlns=\"http://www.w3.org/2000/09/xmldsig#\" Id=\"TTCQT-Id-2da3b60292a14702aec6cb5b276e7768\"><SignedInfo><CanonicalizationMethod Algorithm=\"http://www.w3.org/TR/2001/REC-xml-c14n-20010315\" /><SignatureMethod Algorithm=\"http://www.w3.org/2001/04/xmldsig-more#rsa-sha256\" /><Reference URI=\"#Object-TTCQT-Id-2da3b60292a14702aec6cb5b276e7768\"><DigestMethod Algorithm=\"http://www.w3.org/2001/04/xmlenc#sha256\" /><DigestValue>N0oGHJnkn80vY+I0R6ilvQbgTHWAav1GK+v20807TyE=</DigestValue></Reference><Reference URI=\"#Id-2da3b60292a14702aec6cb5b276e7768\"><DigestMethod Algorithm=\"http://www.w3.org/2001/04/xmlenc#sha256\" /><DigestValue>fda5zcz1NSe/4c8Izv/xn9fU4hsQuVTRal+cNlsd0Zs=</DigestValue></Reference></SignedInfo><SignatureValue>UmpUOJ++XnOED7BtZqg0G6h6KpyVyQzHpnsMxntxmawwDPw5a3C5ydcV+wEj8TdEYd15t8fS1p8DNvQZfoqrziEPpbNlEXN+LuWuU0KDl5U1YjzILSOsVIanMJihAByjUzH/awXh1ycUeOMIF1jb8Rypl6mrUN+P/UkwwLM0BjOQWxZ3MuYJPDe7PBUSZqFrSoDFE6l3JOznVNu+M0eTqf2Rb2ySgg+SpSyR1l5PpSrOS1/icgRDStU8W0MWB6oV4+GpPLGfQJdNndm/yYUAyLgm+QpywzUNLEAjhbf1J5TfH+RjBaYufOJNGZcgoTw02rCJNRWZMZiy9fBWvzLGow==</SignatureValue><KeyInfo><X509Data><X509SubjectName>CN=Phó Chi cục trưởng Nguyễn Mạnh Trung, L=Hồ Chí Minh, OU=Cục Thuế thành phố Hồ Chí Minh, OU=Tổng cục Thuế, O=Bộ Tài chính, C=VN</X509SubjectName><X509Certificate>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</X509Certificate></X509Data></KeyInfo><Object Id=\"Object-TTCQT-Id-2da3b60292a14702aec6cb5b276e7768\"><SignatureProperties xmlns=\"\"><SignatureProperty Id=\"SignatureProperty-TTCQT-Id-2da3b60292a14702aec6cb5b276e7768\" Target=\"#TTCQT-Id-2da3b60292a14702aec6cb5b276e7768\"><SigningTime>2022-03-24T17:16:58</SigningTime></SignatureProperty></SignatureProperties></Object></Signature></TTCQT><CQT><Signature Id=\"CQT-Id-2da3b60292a14702aec6cb5b276e7768\" xmlns=\"http://www.w3.org/2000/09/xmldsig#\"><SignedInfo><CanonicalizationMethod Algorithm=\"http://www.w3.org/TR/2001/REC-xml-c14n-20010315\" /><SignatureMethod Algorithm=\"http://www.w3.org/2000/09/xmldsig#rsa-sha1\" /><Reference URI=\"#Object-CQT-Id-2da3b60292a14702aec6cb5b276e7768\"><DigestMethod Algorithm=\"http://www.w3.org/2000/09/xmldsig#sha1\" /><DigestValue>n2s5aNIkte2q5duZ7jTN4Vf19jY=</DigestValue></Reference><Reference URI=\"#Id-2da3b60292a14702aec6cb5b276e7768\"><DigestMethod Algorithm=\"http://www.w3.org/2000/09/xmldsig#sha1\" /><DigestValue>TEjemE/WjuxrSjYy4yYxVlP3lDw=</DigestValue></Reference><Reference URI=\"#Id-6813e99ffeb542b1ae9c2d8bbfab068f\"><DigestMethod Algorithm=\"http://www.w3.org/2000/09/xmldsig#sha1\" /><DigestValue>0CFae5LbGa1P+yjMVi8zH6j0NHs=</DigestValue></Reference></SignedInfo><SignatureValue>mMA4tHjS1AThpRFELoHY8qCEHZDLD277kB21BvcN0TaNllNjcwzcXR+xRbABFmkZ7t82eAdqYS6XibhFk4Yov0PW2rD+LYOzPbf1g+C6CJIPPZfVkCdADY6i7QAJxDQ027n03jNSvnNFTbHyavVxXxilZ0AtZhezSP8JNYIdDNWxcnnqwIIum6XldGPnEdbwI/oHtFP+1QgnZ03JabpXo3sH87o7AXi9PmTezFIWQELqdOZGI51Oy5dgArO75Rl3ek8AoDgRoycVYlpPUT7xf9DXE/gy0yotb8DJvrnooyUpmNYzv7MmSY5SxZU2VOnY6fLcMRWAM0gryyug59D4uw==</SignatureValue><KeyInfo><X509Data><X509SubjectName>CN=Chi cục Thuế Gò Vấp, L=Hồ Chí Minh, OU=Cục Thuế thành phố Hồ Chí Minh, OU=Tổng cục Thuế, O=Bộ Tài chính, C=VN</X509SubjectName><X509Certificate>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</X509Certificate></X509Data></KeyInfo><Object Id=\"Object-CQT-Id-2da3b60292a14702aec6cb5b276e7768\"><SignatureProperties xmlns=\"\"><SignatureProperty Target=\"#CQT-Id-2da3b60292a14702aec6cb5b276e7768\" Id=\"SignatureProperty-CQT-Id-2da3b60292a14702aec6cb5b276e7768\"><SigningTime>2022-03-25T08:17:07</SigningTime></SignatureProperty></SignatureProperties></Object></Signature></CQT></DSCKS></TBao></DLieu></TDiep></DuLieu></KetQuaTraCuu>");
		if(rTCTN == null) {
			responseStatus = new MspResponseStatus(9999, "Kết nối với TCTN không thành công.");
			rsp.setResponseStatus(responseStatus);
			return rsp;
		}
		
		/*LUU LAI FILE KQTN*/
		String dir = docTmp.get("Dir", "");
		String fileName = _id + "_" + MTDiep + ".xml";
		boolean boo = false;
		try {
			boo = commons.docW3cToFile(rTCTN, dir, fileName);
		}catch(Exception e) {}
		if(!boo) {
			responseStatus = new MspResponseStatus(9999, "Lưu tập tin trả về từ CQT không thành công.");
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
		
		Node nodeTDiep = null;
	    String CQT_MLTDiep = "";
	    int stt = 0;
	    NodeList nodeList = null;
		Node node = null;
	    List<Document> rows = null;
		if(docTmp.get("data") != null) {
			rows = docTmp.getList("data", Document.class);
		}
		ArrayList<HashMap<String, Object>> rowsReturn = new ArrayList<HashMap<String, Object>>();
		HashMap<String, Object> hItem = null;
	
	    for(int i = 1; i<=20; i++) {
	      if(xPath.evaluate("DuLieu/TDiep[" + i + "]", nodeKetQuaTraCuu, XPathConstants.NODE) == null) break;
	      nodeTDiep = (Node) xPath.evaluate("DuLieu/TDiep[" + i + "]", nodeKetQuaTraCuu, XPathConstants.NODE);
	       CQT_MLTDiep = commons.getTextFromNodeXML((Element) xPath.evaluate("TTChung/MLTDiep", nodeTDiep, XPathConstants.NODE));
	       stt +=1;
	   	nodeList = (NodeList) xPath.evaluate("DLieu/TBao/TTTNhan", nodeTDiep, XPathConstants.NODESET);
	       if("999".equals(CQT_MLTDiep)){	
	    	   String loi =  commons.getTextFromNodeXML((Element) xPath.evaluate("DLieu/TBao/TTTNhan", nodeTDiep, XPathConstants.NODE));
					if(loi.equals("0")) {
						loi = "Đã tiếp nhận";
					}
					else {
						loi = "Tiếp nhận xảy ra lỗi";
					}
	    	
					hItem = new HashMap<String, Object>();
					hItem.put("STT", stt);
					hItem.put("Date", commons.getTextFromNodeXML((Element) xPath.evaluate("DLieu/TBao/NNhan", nodeTDiep, XPathConstants.NODE)));
				  	hItem.put("MLoi",CQT_MLTDiep);
					hItem.put("MTLoi", loi);
					rowsReturn.add(hItem);
				
	       }
	       else  if("204".equals(CQT_MLTDiep)) {
	    	   String LTBao = commons.getTextFromNodeXML((Element) xPath.evaluate("DLieu/TBao/DLTBao/LTBao", nodeTDiep, XPathConstants.NODE));
	    	   if(LTBao.equals("2")) {
	    		   LTBao = "Dữ liệu đúng";
				}
				else {
					LTBao = "Sai dữ liệu";
					nodeList = (NodeList) xPath.evaluate("DLieu/TBao/DLTBao/LHDKMa/DSHDon/HDon", nodeTDiep, XPathConstants.NODESET);
				}
	    		hItem = new HashMap<String, Object>();
				hItem.put("STT", stt);
				hItem.put("Date", commons.getTextFromNodeXML((Element) xPath.evaluate("DLieu/TBao/DLTBao/TGGui", nodeTDiep, XPathConstants.NODE)));
				hItem.put("MLoi", commons.getTextFromNodeXML((Element) xPath.evaluate("DLieu/TBao/DLTBao/KHLKhac/DSLDo/LDo/MLoi", nodeTDiep, XPathConstants.NODE)));
				hItem.put("MTLoi", commons.getTextFromNodeXML((Element) xPath.evaluate("DLieu/TBao/DLTBao/KHLKhac/DSLDo/LDo/MTLoi", nodeTDiep, XPathConstants.NODE)));
				rowsReturn.add(hItem);
			
	       }
	       else {
	    		hItem = new HashMap<String, Object>();
					hItem.put("STT", stt);
					hItem.put("Date", commons.getTextFromNodeXML((Element) xPath.evaluate("DLieu/TBao/DLTBao/TGNhan", nodeTDiep, XPathConstants.NODE)));
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

		docFind = new Document("IssuerId", header.getIssuerId()).append("_id", objectId);
		pipeline = new ArrayList<Document>();
		pipeline.add(new Document("$match", docFind));
		pipeline.add(new Document("$lookup", new Document("from", "ConfigEmail")
				.append("let", new Document("vIssuerId", "$IssuerId"))
				.append("pipeline",
						Arrays.asList(new Document("$match",
								new Document("$expr", new Document("$eq", Arrays.asList("$IssuerId", "$$vIssuerId"))))))
				.append("as", "ConfigEmail")));
		pipeline.add(new Document("$unwind",
				new Document("path", "$ConfigEmail").append("preserveNullAndEmptyArrays", true)));
		
		pipeline.add(new Document("$lookup", new Document("from", "ConfigMailJet")
				.append("pipeline", Arrays.asList(new Document("$match", new Document("IsActive",true))))
				.append("as", "ConfigMailJet")));
		pipeline.add(new Document("$unwind",
				new Document("path", "$ConfigMailJet").append("preserveNullAndEmptyArrays", true)));
		
		pipeline.add(
				new Document("$lookup", 
					new Document("from", "DMFooterWeb")
					.append("pipeline", 
						Arrays.asList(
							new Document("$match", 
								new Document("IsActive", true).append("IsDelete", new Document("$ne", true))
							),
							new Document("$project", new Document("Noidung", 1)),
							new Document("$limit", 1)
						)
					)
					.append("as", "DMFooterWeb")
				)
			);
			pipeline.add(new Document("$unwind", new Document("path", "$DMFooterWeb").append("preserveNullAndEmptyArrays", true)));
		
		
		MongoClient mongoClient = cfg.mongoClient();
		MongoCollection<Document> collection = mongoClient.getDatabase(cfg.dbName).getCollection("EInvoiceHDSSMTT");
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

		if (docTmp.get("ConfigEmail") == null) {
			responseStatus = new MspResponseStatus(9999, "Không tìm thấy thông tin email gửi.");
			rsp.setResponseStatus(responseStatus);
			return rsp;
		}

		String CheckFooterMail = docTmp.getEmbedded(Arrays.asList("UserConFig", "footermail"), "");
		if(!CheckFooterMail.equals("Y")) {
			_content = commons.decodeURIComponent(_content);
			String noidung = docTmp.getEmbedded(Arrays.asList("DMFooterWeb", "Noidung"), "");
			_content += noidung;
		}
		else {
			_content = commons.decodeURIComponent(_content);
		}
	

		String mauHD  = "";
		String soHD = "";
		for(Document oo: docTmp.getList("DSHDon", Document.class)) {
			mauHD = oo.get("KHMSHDon", "")+ oo.get("KHHDon", "");
			soHD = oo.get("SHDon", "");
		}
	
		String MailJet = docTmp.getEmbedded(Arrays.asList("ConfigEmail", "MailJet"), "");
		String dir = docTmp.getString("Dir");
		  String fileName = _id +"_signed.xml";
			File file = null;
			String fileNamePDF = _id + ".pdf";
			file = new File(dir, fileName);
		
		/* THUC HIEN GUI MAIL */
		
		if (file.exists() && file.isFile()) {
			org.w3c.dom.Document doc = commons.fileToDocument(file);
			String fileNameJP = "04SS.jrxml";
			int numberRowInPage = 5;
			int numberRowInPageMultiPage =  15;
			int numberCharsInRow = 50;
			String ImgLogo = "";
	        String ImgBackground = "";
			File fileJP = new File(SystemParams.DIR_E_INVOICE_TEMPLATE, fileNameJP);
			ByteArrayOutputStream baosPDF = null;
			
			baosPDF = jpUtils.print04(fileJP, doc,docTmp, numberRowInPage, numberRowInPageMultiPage,
					numberCharsInRow, Paths.get(SystemParams.DIR_E_INVOICE_TEMPLATE, "images", (String)docTmp.getEmbedded(Arrays.asList("Issuer", "TaxCode"), ""), ImgLogo).toString(), Paths.get(SystemParams.DIR_E_INVOICE_TEMPLATE, "images", (String)docTmp.getEmbedded(Arrays.asList("Issuer", "TaxCode"), ""), ImgBackground).toString(),
					false);
			/* LUU TAP TIN PDF */
			if (null != baosPDF) {
				try (OutputStream fileOuputStream = new FileOutputStream(new File(dir, fileNamePDF))) {
					baosPDF.writeTo(fileOuputStream);
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		
		file = null;
		List<String> listFiles = new ArrayList<>();
		List<String> listNames = new ArrayList<>();
		file = new File(dir, fileNamePDF);
		if (file.exists() && file.isFile()) {
			listFiles.add(file.toString());
			listNames.add(mauHD + "-" + soHD + ".pdf");
		}
		boolean boo = false;
		MailConfig mailConfig = new MailConfig(docTmp.get("ConfigEmail", Document.class));
		mailConfig.setNameSend(docTmp.getEmbedded(Arrays.asList("EInvoiceDetail", "NDHDon", "NBan", "Ten"), ""));
		
		
		if (_email != "") {
			//KIỂM TRA GỬI MAIL THƯỜNG HAY MAILJET
			if(MailJet.equals("Y")&&!MailJet.equals("")&&!MailJet.equals("N")) {				
				//
				String ApiKey = docTmp.getEmbedded(Arrays.asList("ConfigMailJet", "ApiKey"), "");
				String SecretKey = docTmp.getEmbedded(Arrays.asList("ConfigMailJet", "SecretKey"), "");
				String EmailAddress = docTmp.getEmbedded(Arrays.asList("ConfigMailJet", "EmailAddress"), "");
				mailConfig.setEmailAddress(ApiKey);
				mailConfig.setEmailPassword(SecretKey);
				mailConfig.setSmtpServer(EmailAddress);
				 boo = mailJet.sendMailJet(mailConfig, _title, _content, _email, listFiles, listNames, true);				 
			}else{
				boo = mailUtils.sendMail(mailConfig, _title, _content, _email, listFiles, listNames, true);
			}
				try {
					
					mongoClient = cfg.mongoClient();
					collection = mongoClient.getDatabase(cfg.dbName).getCollection("LogEmailUser");
					collection.insertOne(new Document("IssuerId", header.getIssuerId()).append("Title", _title)
							.append("Email", _email).append("IsActive", boo).append("MailCheck", boo)
							.append("IsDelete", false).append("EmailContent", _content));      
					mongoClient.close();
					
				} catch (Exception ex) {
				}
		}
	

		responseStatus = new MspResponseStatus(0, "SUCCESS");
		rsp.setResponseStatus(responseStatus);
		return rsp;
	}

}
