package vn.sesgroup.hddt.user.impl;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
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
import com.mongodb.client.model.FindOneAndUpdateOptions;
import com.mongodb.client.model.ReturnDocument;

import vn.sesgroup.hddt.configuration.ConfigConnectMongo;
import vn.sesgroup.hddt.dto.FileInfo;
import vn.sesgroup.hddt.user.dao.AbstractDAO;
import vn.sesgroup.hddt.user.dao.TBHDSSotTBNDAO;
import vn.sesgroup.hddt.user.service.JPUtils;
import vn.sesgroup.hddt.user.service.TCTNService;
import vn.sesgroup.hddt.utility.Constants;
import vn.sesgroup.hddt.utility.Json;
import vn.sesgroup.hddt.utility.SystemParams;

@Repository
@Transactional
public class TBHDSSotTBNImpl extends AbstractDAO implements TBHDSSotTBNDAO{

	@Autowired ConfigConnectMongo cfg;
	@Autowired TCTNService tctnService;

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

		HashMap<String, String> hItem02 = null;
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
			
			taxCode = docTmp.getString("TaxCode");
			objectId = new ObjectId();
			
			/*LUU LAI THONG TIN HDSS*/
			path = Paths.get(SystemParams.DIR_E_INVOICE_HDSS, taxCode, String.valueOf(LocalDate.now().getYear()));
			pathDir = path.toString();
			file = path.toFile();
			if(!file.exists()) file.mkdirs();
			
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
			
			elementContent.appendChild(commons.createElementWithValue(doc, "PBan", SystemParams.VERSION_XML_HDSS));
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
			for(JsonNode o: jsonNodeDSHDon) {					
					elementTmp = doc.createElement("HDon");
					
					String MSHDon = commons.getTextJsonNode(o.at("/MSHDon")).replaceAll("\\s+", "");
					String KHMSHDon = MSHDon.substring(0, 1); 
					String KHHDon = MSHDon.substring(1); 
					String Ngay = commons.getTextJsonNode(o.at("/Ngay"));
					DateTimeFormatter inputFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");			        
				    LocalDate localDate = LocalDate.parse(Ngay, inputFormatter);			        
				    DateTimeFormatter outputFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");				        
				    String NgayHDon = localDate.format(outputFormatter);
					
					elementTmp.appendChild(commons.createElementWithValue(doc, "STT", String.valueOf(stt)));
					elementTmp.appendChild(commons.createElementWithValue(doc, "MCCQT", commons.getTextJsonNode(o.at("/MCQTCap")).replaceAll("\\s+", "").replaceAll("[^a-zA-Z0-9]", "")));
					elementTmp.appendChild(commons.createElementWithValue(doc, "KHMSHDon", KHMSHDon));
					elementTmp.appendChild(commons.createElementWithValue(doc, "KHHDon", KHHDon));
					elementTmp.appendChild(commons.createElementWithValue(doc, "SHDon", String.valueOf(commons.getTextJsonNode(o.at("/SHDon")))));
					elementTmp.appendChild(commons.createElementWithValue(doc, "Ngay", NgayHDon));
					elementTmp.appendChild(commons.createElementWithValue(doc, "LADHDDT", "1"));
					elementTmp.appendChild(commons.createElementWithValue(doc, "TCTBao", commons.getTextJsonNode(o.at("/TCTBao"))));
					elementTmp.appendChild(commons.createElementWithValue(doc, "LDo", commons.getTextJsonNode(o.at("/LDo"))));
					
					elementSubContent.appendChild(elementTmp);
					
					hItem02 = new HashMap<String, String>();
					hItem02.put("STT", String.valueOf(stt));
					hItem02.put("MCQTCap", commons.getTextJsonNode(o.at("/MCQTCap")));
					hItem02.put("KHMSHDon", KHMSHDon);
					hItem02.put("KHHDon", KHHDon);
					hItem02.put("SHDon", String.valueOf(commons.getTextJsonNode(o.at("/SHDon"))));
					hItem02.put("Ngay", NgayHDon);
					hItem02.put("LADHDDT", "1");
					hItem02.put("TCTBao", commons.getTextJsonNode(o.at("/TCTBao")));
					hItem02.put("LDo", commons.getTextJsonNode(o.at("/LDo")));
					
					listDSHDon.add(hItem02);
					
					stt++;
				}
			
			
			elementContent.appendChild(elementSubContent);	
			
			
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
			collection = mongoClient.getDatabase(cfg.dbName).getCollection("EInvoiceHDSSTBN");
			collection.insertOne(docUpsert);      
			mongoClient.close();
			
			String name_company = removeAccent(header.getUserFullName());
			DateTimeFormatter format_time = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
			LocalDateTime time_dem  = LocalDateTime.now();
			String time = time_dem.format(format_time);
			System.out.println(time +name_company+" Vua tao hoa don sai sot tu ben ngoai.");
			responseStatus = new MspResponseStatus(0, "SUCCESS");
			rsp.setResponseStatus(responseStatus);				
			return rsp;
		case Constants.MSG_ACTION_CODE.MODIFY:	
			objectId = null;
			ObjectId objectIdEInvoiceHDSSTBN = null;
			objectIdEInvoiceHDSSTBN = new ObjectId();
			objectIdEInvoiceHDSSTBN = null;
			try {
				objectId = new ObjectId(header.getIssuerId());
			}catch(Exception e) {}
			try {
				objectIdEInvoiceHDSSTBN = new ObjectId(_id);
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
			
			docFind = new Document("IssuerId", header.getIssuerId()).append("IsDelete", new Document("$ne", true))
					.append("_id", objectIdEInvoiceHDSSTBN);
			pipeline.add(
					new Document("$lookup", 
						new Document("from", "EInvoiceHDSSTBN")
						.append("pipeline", 
							Arrays.asList(
								new Document("$match", docFind)
//								new Document("$project", new Document("MCCQT", 1).append("EInvoiceDetail.TTChung", 1))
							)
						)
						.append("as", "EInvoiceHDSSTBN")
					)
				);
			pipeline.add(new Document("$unwind", new Document("path", "$EInvoiceHDSSTBN").append("preserveNullAndEmptyArrays", true)));
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
			
			taxCode = docTmp.getString("TaxCode");
			
			
			/*LUU LAI THONG TIN HDSS*/		
			pathDir = docTmp.getEmbedded(Arrays.asList("EInvoiceHDSSTBN", "Dir"), "");
			fileNameXML = docTmp.getEmbedded(Arrays.asList("EInvoiceHDSSTBN", "FileNameXML"), "");
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
			for(JsonNode o: jsonNodeDSHDon) {					
				elementTmp = doc.createElement("HDon");
				
				String MSHDon = commons.getTextJsonNode(o.at("/MSHDon")).replaceAll("\\s+", "");
				String KHMSHDon = MSHDon.substring(0, 1); 
				String KHHDon = MSHDon.substring(1); 
				String Ngay = commons.getTextJsonNode(o.at("/Ngay"));
				DateTimeFormatter inputFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");			        
			    LocalDate localDate = LocalDate.parse(Ngay, inputFormatter);			        
			    DateTimeFormatter outputFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");				        
			    String NgayHDon = localDate.format(outputFormatter);
				
				elementTmp.appendChild(commons.createElementWithValue(doc, "STT", String.valueOf(stt)));
				elementTmp.appendChild(commons.createElementWithValue(doc, "MCCQT", commons.getTextJsonNode(o.at("/MCQTCap"))));
				elementTmp.appendChild(commons.createElementWithValue(doc, "KHMSHDon", KHMSHDon));
				elementTmp.appendChild(commons.createElementWithValue(doc, "KHHDon", KHHDon));
				elementTmp.appendChild(commons.createElementWithValue(doc, "SHDon", String.valueOf(commons.getTextJsonNode(o.at("/SHDon")))));
				elementTmp.appendChild(commons.createElementWithValue(doc, "Ngay", NgayHDon));
				elementTmp.appendChild(commons.createElementWithValue(doc, "LADHDDT", "1"));
				elementTmp.appendChild(commons.createElementWithValue(doc, "TCTBao", commons.getTextJsonNode(o.at("/TCTBao"))));
				elementTmp.appendChild(commons.createElementWithValue(doc, "LDo", commons.getTextJsonNode(o.at("/LDo"))));
				
				elementSubContent.appendChild(elementTmp);
				
				hItem02 = new HashMap<String, String>();
				hItem02.put("STT", String.valueOf(stt));
				hItem02.put("MCQTCap", commons.getTextJsonNode(o.at("/MCQTCap")));
				hItem02.put("KHMSHDon", KHMSHDon);
				hItem02.put("KHHDon", KHHDon);
				hItem02.put("SHDon", String.valueOf(commons.getTextJsonNode(o.at("/SHDon"))));
				hItem02.put("Ngay", NgayHDon);
				hItem02.put("LADHDDT", "1");
				hItem02.put("TCTBao", commons.getTextJsonNode(o.at("/TCTBao")));
				hItem02.put("LDo", commons.getTextJsonNode(o.at("/LDo")));
				
				listDSHDon.add(hItem02);
				
				stt++;
			}
				
			elementContent.appendChild(elementSubContent);
			
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
			collection = mongoClient.getDatabase(cfg.dbName).getCollection("EInvoiceHDSSTBN");
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
			System.out.println(time +name_company+" Vua thay doi hoa don sai sot tu ben ngoai.");
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
			collection = mongoClient.getDatabase(cfg.dbName).getCollection("EInvoiceHDSSTBN");
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
			collection = mongoClient.getDatabase(cfg.dbName).getCollection("EInvoiceHDSSTBN");
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
			System.out.println(time +name_company+" Vua xoa hoa don sai sot tu ben ngoai.");
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
					.append("IsDelete", false)
					.append("Status", Constants.INVOICE_STATUS.PENDING)
					.append("SignStatusCode", Constants.INVOICE_SIGN_STATUS.SIGNED)
					.append("NTBaoDate", LocalDate.now());
			docTmp = null;

			
			 mongoClient = cfg.mongoClient();
			 collection = mongoClient.getDatabase(cfg.dbName).getCollection("EInvoiceHDSSTBN");
			 
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
			Node nodeKetQuaTraCuu = null;
			String MaKetQua = "";
			String CQT_MLTDiep  = "";
			String codeTTTNhan = "";
			String LTBao = "";
			String descTTTNhan="";

			 rTCTN1 = tctnService.callTraCuuThongDiep(MTDiep);
			 if (rTCTN1 == null) {			
					 rTCTN1 = tctnService.callTraCuuThongDiep(MTDiep);
				}
			 	xPath1 = XPathFactory.newInstance().newXPath();
				 nodeKetQuaTraCuu = (Node) xPath1.evaluate("/KetQuaTraCuu", rTCTN1, XPathConstants.NODE);
				 MaKetQua = commons.getTextFromNodeXML((Element) xPath1.evaluate("MaKetQua", nodeKetQuaTraCuu, XPathConstants.NODE));
				 if("2".equals(MaKetQua)) {
						rTCTN = tctnService.callTiepNhanThongDiep("300", MTDiep, MST, "1", doc);
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
			 collection = mongoClient.getDatabase(cfg.dbName).getCollection("EInvoiceHDSSTBN");
			collection.findOneAndUpdate(
					docFind, 
					new Document("$set", 
						new Document("Status", "PROCESSING")
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
		MongoCollection<Document> collection = mongoClient.getDatabase(cfg.dbName).getCollection("EInvoiceHDSSTBN");
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
		System.out.println(time +name_company+" Vua search hoa don sai sot tu ben ngoai.");
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
		MongoCollection<Document> collection = mongoClient.getDatabase(cfg.dbName).getCollection("EInvoiceHDSSTBN");
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
		System.out.println(time +name_company+" Vua xem chi tiet hoa don sai sot tu ben ngoai.");
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
				.append("IsDelete", false)
				.append("Status", Constants.INVOICE_STATUS.CREATED)
				.append("SignStatusCode", Constants.INVOICE_SIGN_STATUS.NOSIGN)
				.append("NTBaoDate", LocalDate.now());
		
		List<Document> pipeline = new ArrayList<Document>();
		pipeline.add(new Document("$match", docFind));
		
		Document docTmp = null;

		
		MongoClient mongoClient = cfg.mongoClient();
		MongoCollection<Document> collection = mongoClient.getDatabase(cfg.dbName).getCollection("EInvoiceHDSSTBN");
		
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
				.append("IsDelete", false)
				.append("Status", Constants.INVOICE_STATUS.CREATED)
				.append("SignStatusCode", Constants.INVOICE_SIGN_STATUS.NOSIGN)
				.append("NTBaoDate", LocalDate.now());
		
		Document docTmp = null;

		
		MongoClient mongoClient = cfg.mongoClient();
		MongoCollection<Document> collection = mongoClient.getDatabase(cfg.dbName).getCollection("EInvoiceHDSSTBN");
		
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
		 collection = mongoClient.getDatabase(cfg.dbName).getCollection("EInvoiceHDSSTBN");
		collection.findOneAndUpdate(
				docFind, 
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
		System.out.println(time +name_company+" Vua ky thanh cong hoa don sai sot tu ben ngoai.");
		responseStatus = new MspResponseStatus(0, Constants.MAP_ERROR.get(0));
		rsp.setResponseStatus(responseStatus);
		return rsp;
	}

	@Override
	public MsgRsp refreshStatusCQT(JSONRoot jsonRoot) throws Exception {
		Msg msg = jsonRoot.getMsg();
		MsgHeader header = msg.getMsgHeader();
		MsgPage page = msg.getMsgPage();

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
				.append("IsDelete", false)
				.append("Status", new Document("$in", Arrays.asList("ERROR_CQT", "PROCESSING")))
				.append("SignStatusCode", Constants.INVOICE_SIGN_STATUS.SIGNED);
		
		Document docTmp = null;

		
		MongoClient mongoClient = cfg.mongoClient();
		MongoCollection<Document> collection = mongoClient.getDatabase(cfg.dbName).getCollection("EInvoiceHDSSTBN");
		
		try {
			docTmp =   collection.find(docFind).allowDiskUse(true).iterator().next();	
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
		
		 MST = docTmp.get("MST", "");
		/*DO DU LIEU TRA VE - CAP NHAT LAI KET QUA*/
		XPath xPath = XPathFactory.newInstance().newXPath();
		Node nodeKetQuaTraCuu = (Node) xPath.evaluate("/KetQuaTraCuu", rTCTN, XPathConstants.NODE);
		String MaKetQua = commons.getTextFromNodeXML((Element) xPath.evaluate("MaKetQua", nodeKetQuaTraCuu, XPathConstants.NODE));
		String MoTaKetQua = commons.getTextFromNodeXML((Element) xPath.evaluate("MoTaKetQua", nodeKetQuaTraCuu, XPathConstants.NODE));

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
			rTCTN = tctnService.callTiepNhanThongDiep("300", MTDiep, MST, "1", doc1);
			 xPath = XPathFactory.newInstance().newXPath();
			 responseStatus = new MspResponseStatus(9999, "Mã giao dịch đang được gửi lại.");
			rsp.setResponseStatus(responseStatus);
			return rsp; 
			}	  
		}
		
		boolean check_ = false;
		Node nodeTDiep = null;
	    String CQT_MLTDiep = "";
	    String MLoi1 = "";
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
				 collection = mongoClient.getDatabase(cfg.dbName).getCollection("EInvoiceHDSSTBN");
				collection.findOneAndUpdate(
						docFind, 
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
				
				for(Document doc: docTmp.getList("DSHDon", Document.class)) {
					TCTBao = doc.get("TCTBao", "");
								
					String KHMSHDon = doc.get("KHMSHDon", "");
					String KHHDon = doc.get("KHHDon", "");
					String Ngay = doc.get("Ngay", "");
					SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
				    Date date = dateFormat.parse(Ngay);
											
					Document insertHD = new Document("IssuerId", header.getIssuerId())
							.append("SignStatusCode", "SIDED")
							.append("EInvoiceStatus", Constants.MAP_EInvoiceStatus_TBN.get(TCTBao))
							.append("EInvoiceDetail", new Document("TTChung", new Document("KHMSHDon", KHMSHDon)
									.append("KHHDon", KHHDon)
									.append("NLap", date)
									.append("SHDon", commons.ToNumberInt(doc.get("SHDon", "0")))
									))
							.append("IsDelete", false)
							.append("MCCQT", doc.get("MCQTCap", ""))
							.append("HDSS", new Document("TCTBao", TCTBao)
									.append("TCTBaoDesc", Constants.MAP_HDSS_TCTBAO.get(TCTBao))
									.append("LDo", doc.get("LDo", "")))
							.append("InfoCreated", 
									new Document("CreateDate", LocalDateTime.now())
									.append("CreateUserID", header.getUserId())
									.append("CreateUserName", header.getUserName())
									.append("CreateUserFullName", header.getUserFullName())
						);
					
					mongoClient = cfg.mongoClient();
					collection = mongoClient.getDatabase(cfg.dbName).getCollection("EInvoice");
					collection.insertOne(insertHD);      
					mongoClient.close();
					
				///END SEARCH INFORMATION OF EINVOICE												
			}
							
				/*CAP NHAT TRANG THAI COMPLETE - TRANG THAI CQT*/
				options = new FindOneAndUpdateOptions();
				options.upsert(false);
				options.maxTime(5000, TimeUnit.MILLISECONDS);
				options.returnDocument(ReturnDocument.AFTER);
				
			
				 mongoClient = cfg.mongoClient();
				 collection = mongoClient.getDatabase(cfg.dbName).getCollection("EInvoiceHDSSTBN");
				collection.findOneAndUpdate(
						docFind, 
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
				
				//
				
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
			 collection = mongoClient1.getDatabase(cfg.dbName).getCollection("EInvoiceHDSSTBN");
			collection.findOneAndUpdate(
					docFind, 
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
				 collection = mongoClient1.getDatabase(cfg.dbName).getCollection("EInvoiceHDSSTBN");
				collection.findOneAndUpdate(
						docFind, 
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
			
			if(TTTNCCQT.equals("1")){			
			String TCTBao = "";
			
			for(Document doc: docTmp.getList("DSHDon", Document.class)) {
				TCTBao = doc.get("TCTBao", "");
		
				String KHMSHDon = doc.get("KHMSHDon", "");
				String KHHDon = doc.get("KHHDon", "");
				String Ngay = doc.get("Ngay", "");
				SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
			    Date date = dateFormat.parse(Ngay);
										
				Document insertHD = new Document("IssuerId", header.getIssuerId())
						.append("SignStatusCode", "SIDED")
						.append("EInvoiceStatus", Constants.MAP_EInvoiceStatus_TBN.get(TCTBao))
						.append("EInvoiceDetail", new Document("TTChung", new Document("KHMSHDon", KHMSHDon)
								.append("KHHDon", KHHDon)
								.append("NLap", date)
								.append("SHDon", commons.ToNumberInt(doc.get("SHDon", "0")))
								))
						.append("IsDelete", false)
						.append("MCCQT", doc.get("MCQTCap", ""))
						.append("HDSS", new Document("TCTBao", TCTBao)
								.append("TCTBaoDesc", Constants.MAP_HDSS_TCTBAO.get(TCTBao))
								.append("LDo", doc.get("LDo", "")))
						.append("InfoCreated", 
								new Document("CreateDate", LocalDateTime.now())
								.append("CreateUserID", header.getUserId())
								.append("CreateUserName", header.getUserName())
								.append("CreateUserFullName", header.getUserFullName())
					);
				
				mongoClient = cfg.mongoClient();
				collection = mongoClient.getDatabase(cfg.dbName).getCollection("EInvoice");
				collection.insertOne(insertHD);      
				mongoClient.close();
										
			}

			/*CAP NHAT TRANG THAI COMPLETE - TRANG THAI CQT*/
			options = new FindOneAndUpdateOptions();
			options.upsert(false);
			options.maxTime(5000, TimeUnit.MILLISECONDS);
			options.returnDocument(ReturnDocument.AFTER);
			
			
			 mongoClient = cfg.mongoClient();
			 collection = mongoClient.getDatabase(cfg.dbName).getCollection("EInvoiceHDSSTBN");
			collection.findOneAndUpdate(
					docFind, 
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
		 collection = mongoClient1.getDatabase(cfg.dbName).getCollection("EInvoiceHDSSTBN");
		collection.findOneAndUpdate(
				docFind, 
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
				
	///ELSE 301
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
		MongoCollection<Document> collection = mongoClient.getDatabase(cfg.dbName).getCollection("EInvoiceHDSSTBN");
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
	 
		ArrayList<HashMap<String, Object>> rowsReturn = new ArrayList<HashMap<String, Object>>();
		HashMap<String, Object> hItem = null;
	
	    for(int i = 1; i<=20; i++) {
	      if(xPath.evaluate("DuLieu/TDiep[" + i + "]", nodeKetQuaTraCuu, XPathConstants.NODE) == null) break;
	      nodeTDiep = (Node) xPath.evaluate("DuLieu/TDiep[" + i + "]", nodeKetQuaTraCuu, XPathConstants.NODE);
	       CQT_MLTDiep = commons.getTextFromNodeXML((Element) xPath.evaluate("TTChung/MLTDiep", nodeTDiep, XPathConstants.NODE));
	       stt +=1;
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

}
