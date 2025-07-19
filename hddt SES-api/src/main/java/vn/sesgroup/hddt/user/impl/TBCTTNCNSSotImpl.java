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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.xpath.NodeSet;
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
import vn.sesgroup.hddt.user.dao.TBCTTNCNSSotDAO;
import vn.sesgroup.hddt.user.dao.TBHDSSotDAO;
import vn.sesgroup.hddt.user.service.JPUtils;
import vn.sesgroup.hddt.user.service.TCTNService;
import vn.sesgroup.hddt.utility.Constants;
import vn.sesgroup.hddt.utility.Json;
import vn.sesgroup.hddt.utility.MailUtils;
import vn.sesgroup.hddt.utility.MailjetSender;
import vn.sesgroup.hddt.utility.SystemParams;

@Repository
@Transactional
public class TBCTTNCNSSotImpl extends AbstractDAO implements TBCTTNCNSSotDAO{
	private static final Logger log = LogManager.getLogger(TBCTTNCNSSotImpl.class);
	@Autowired TCTNService tctnService;
	private MailUtils mailUtils = new MailUtils();
	
	@Autowired ConfigConnectMongo cfg;
	
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
		JsonNode jsonNodeDSCTu = jsonData.at("/DSCTu");

		HashMap<String, HashMap<String, String>> CTDatas = new HashMap<String, HashMap<String,String>>();
		List<String> ids = new ArrayList<String>();
		for(JsonNode o: jsonNodeDSCTu) {
			ids.add(commons.getTextJsonNode(o.at("/_id")));
			
			HashMap<String, String> hItem = new HashMap<String, String>();
			hItem.put("LDo", commons.getTextJsonNode(o.at("/LDo")));
			CTDatas.put(commons.getTextJsonNode(o.at("/_id")), hItem);
		}
		List<Object> listDSCTu = new ArrayList<Object>();
		
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
		XPath xPath = null;
		
		String codeTTTNhan = "";
		String descTTTNhan = "";
		
		String fileNameXML = "";
		String taxCode = "";
		String mtdiep = "";
		String pathDir = "";
		Path path = null;
		File file = null;
		
		int stt = 0;
		ObjectId objectId = null;
		ObjectId objectIdCTSSot = null;
		
		Document docTmp = null;
		Document filter = null;
		Document docFind = null;
		FindOneAndUpdateOptions options = null;
		
		org.w3c.dom.Document rTCTN = null;
		
		List<Document> pipeline = null;
		boolean isSaveFile = false;
		
		switch (actionCode) {
		case Constants.MSG_ACTION_CODE.CREATED:
			objectId = null;
			try {
				objectId = new ObjectId(header.getIssuerId());
			}catch(Exception e) {}
			
			filter = new Document("_id", 1)
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
			pipeline.add(
				new Document("$match", 
					new Document("_id", objectId).append("IsActive", true).append("IsDelete", false)));
			pipeline.add(new Document("$project", filter));
			
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
							new Document("$match", new Document("code", coQuanThue)
									.append("IsDelete", false)
									),
							new Document("$project", new Document("_id", 0))
						)
					)
					.append("as", "ChiCucThueInfo")
				)
			);
			pipeline.add(new Document("$unwind", new Document("path", "$ChiCucThueInfo").append("preserveNullAndEmptyArrays", true)));
			pipeline.add(
				    new Document("$lookup", 
				        new Document("from", "CTTNCNhan")
				        .append("pipeline", 
				            Arrays.asList(
				                new Document("$match", 
				                    new Document("$expr", 
				                    		new Document("$and", Arrays.asList(
				                        new Document("$eq", Arrays.asList("$IssuerId", header.getIssuerId())),
				                        new Document("$ne", Arrays.asList("$CTSS.TCTBao", "1")),
				                        new Document("$in", Arrays.asList("$Status", Arrays.asList(Constants.INVOICE_STATUS.COMPLETE))),
				                        new Document("$in", Arrays.asList(new Document("$toString", "$_id"),ids)))
				                    				)
				                    			)
				                		)
				            		)
				        )
				        .append("as", "CTTNCNhan")
				    )
				);
			
			
			try (MongoClient mongoClient = cfg.mongoClient()){
				MongoCollection<Document> collection = mongoClient.getDatabase(cfg.dbName).getCollection("Issuer");
				docTmp =   collection.aggregate(pipeline).allowDiskUse(true).iterator().next();			
			} catch (Exception e) {
				
			}
							
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
			if(docTmp.get("CTTNCNhan") == null || docTmp.getList("CTTNCNhan", Document.class).size() == 0) {
				responseStatus = new MspResponseStatus(9999, "Vui lòng kiểm tra lại danh sách chứng từ.");
				rsp.setResponseStatus(responseStatus);
				return rsp;
			}
			
			taxCode = docTmp.getString("TaxCode");
			objectId = new ObjectId();
			
			/*LUU LAI THONG TIN CTSS*/
			path = Paths.get(SystemParams.DIR_E_INVOICE_CTTNCNSS, taxCode, String.valueOf(LocalDate.now().getYear()));
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
			
			elementContent.appendChild(commons.createElementWithValue(doc, "PBan", SystemParams.VERSION_XML_TNCNSS));
			elementContent.appendChild(commons.createElementWithValue(doc, "MSo", "04/SS-CTĐT"));
			elementContent.appendChild(commons.createElementWithValue(doc, "Ten", "Thông báo chứng từ điện tử đã lập sai"));
			elementContent.appendChild(commons.createElementWithValue(doc, "Loai", loaiThongBao));
			if("2".equals(loaiThongBao)) {
				elementContent.appendChild(commons.createElementWithValue(doc, "So", soTBcuaCQT));
				elementContent.appendChild(commons.createElementWithValue(doc, "NTBCCQT", commons.convertLocalDateTimeToString(commons.convertStringToLocalDate(ngayTBcuaCQT, Constants.FORMAT_DATE.FORMAT_DATE_WEB), "yyyy-MM-dd")));
			}
			elementContent.appendChild(commons.createElementWithValue(doc, "MCQT", docTmp.getEmbedded(Arrays.asList("ChiCucThueInfo", "code"), "")));
			elementContent.appendChild(commons.createElementWithValue(doc, "TCQT", docTmp.getEmbedded(Arrays.asList("ChiCucThueInfo", "name"), "")));
			elementContent.appendChild(commons.createElementWithValue(doc, "TNNT", docTmp.get("Name", "")));
			elementContent.appendChild(commons.createElementWithValue(doc, "MST", taxCode));
			elementContent.appendChild(commons.createElementWithValue(doc, "DDanh", docTmp.getEmbedded(Arrays.asList("TinhThanhInfo", "name"), "")));
			elementContent.appendChild(commons.createElementWithValue(doc, "NTBao", commons.convertLocalDateTimeToString(LocalDate.now(), "yyyy-MM-dd")));
			
			elementSubContent = doc.createElement("DSCTu");
			stt = 1;
			if(docTmp.get("CTTNCNhan") != null){
				for(Document o: docTmp.getList("CTTNCNhan", Document.class)) {
					ObjectId id = (ObjectId) o.get("_id");
					HashMap<String, String> hItem = CTDatas.get(id.toString());
					elementTmp = doc.createElement("CTu");
					
					elementTmp.appendChild(commons.createElementWithValue(doc, "STT", String.valueOf(stt++)));
					elementTmp.appendChild(commons.createElementWithValue(doc, "KHMSCTu", o.get("MSCTu", "")));
					elementTmp.appendChild(commons.createElementWithValue(doc, "KHCTu",  o.get("KHCTu", "")));
					elementTmp.appendChild(commons.createElementWithValue(doc, "SCTu", String.valueOf(o.get("SCTu", 0))));
					elementTmp.appendChild(commons.createElementWithValue(doc, "NLap", commons.convertLocalDateTimeToString(commons.convertDateToLocalDateTime(o.get("NLap", Date.class) ), "yyyy-MM-dd")));
					elementTmp.appendChild(commons.createElementWithValue(doc, "LCTDT", "1"));
					elementTmp.appendChild(commons.createElementWithValue(doc, "LDo", null == hItem? "": hItem.get("LDo")));
					elementSubContent.appendChild(elementTmp);
					
					HashMap<String, Object> hTemp = new HashMap<String, Object>();
					hTemp.put("STT", String.valueOf(stt));
					hTemp.put("_id", id.toString());
					hTemp.put("KHMSCTu", o.get("MSCTu", ""));
					hTemp.put("KHCTu", o.get("KHCTu", ""));
					hTemp.put("SCTu", String.valueOf(o.get("SCTu", 0)));
					hTemp.put("NLap", commons.convertLocalDateTimeToString(commons.convertDateToLocalDateTime(o.get("NLap", Date.class) ), "yyyy-MM-dd"));
					hTemp.put("LCTDT", "1");
					hTemp.put("LDo",  null == hItem? "": hItem.get("LDo"));
					hTemp.put("NNT", o.get("NNT"));
					listDSCTu.add(hTemp);
				}
			}
			elementContent.appendChild(elementSubContent);
			/*END - LUU LAI THONG TIN HDSS*/
			
			isSaveFile = commons.docW3cToFile(doc, pathDir, fileNameXML);
			if(!isSaveFile) {
				throw new Exception("Lưu dữ liệu không thành công.");
			}
			
			Document docInsert = new Document("_id", objectId)
					.append("IssuerId", header.getIssuerId())
					.append("PBan", SystemParams.VERSION_XML_TNCNSS)
					.append("MSo", "04/SS-CTĐT")
					.append("Ten", "Thông báo chứng từ điện tử đã lập sai")
					.append("Loai", loaiThongBao);
			if("2".equals(loaiThongBao)) {
				docInsert.append("So", soTBcuaCQT);
				docInsert.append("NTBCCQT", commons.convertLocalDateTimeToString(commons.convertStringToLocalDate(ngayTBcuaCQT, Constants.FORMAT_DATE.FORMAT_DATE_WEB), "yyyy-MM-dd"));
			}
			docInsert.append("MCQT", docTmp.getEmbedded(Arrays.asList("ChiCucThueInfo", "code"), ""))
				.append("TCQT", docTmp.getEmbedded(Arrays.asList("ChiCucThueInfo", "name"), ""))
				.append("TNNT", docTmp.get("Name", ""))
				.append("MST", docTmp.get("TaxCode", ""))
				.append("DDanh", docTmp.getEmbedded(Arrays.asList("TinhThanhInfo", "name"), ""))
				.append("NTBao", commons.convertLocalDateTimeToString(LocalDate.now(), "yyyy-MM-dd"))
				.append("NTBaoDate", LocalDate.now())
				.append("DSCTu", listDSCTu)
				.append("Dir", pathDir)
				.append("FileNameXML", fileNameXML)
				.append("SignStatus", Constants.INVOICE_SIGN_STATUS.NOSIGN)
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

			try (MongoClient mongoClient = cfg.mongoClient()) {
				MongoCollection<Document> collection = mongoClient.getDatabase(cfg.dbName).getCollection("CTTNCNhanSS");
				collection.insertOne(docInsert);
			} catch (Exception e) {
			}

			responseStatus = new MspResponseStatus(0, "SUCCESS");
			rsp.setResponseStatus(responseStatus);
			return rsp;
			
		case Constants.MSG_ACTION_CODE.MODIFY:
			objectId = null;
			objectIdCTSSot = null;
			try {
				objectId = new ObjectId(header.getIssuerId());
				objectIdCTSSot = new ObjectId(_id);
			} catch (Exception e) {
			}

			filter = new Document("_id", 1).append("TaxCode", 1).append("Name", 1).append("Address", 1)
					.append("Phone", 1).append("Fax", 1).append("Email", 1).append("Website", 1)
					.append("TinhThanhInfo", 1).append("ChiCucThueInfo", 1).append("BankAccount", 1).append("NameEN", 1)
					.append("BankAccountExt", 1);
			
			docFind = new Document("_id", objectId).append("IsActive", true).append("IsDelete", false);
			
			pipeline = new ArrayList<Document>();
			pipeline.add(new Document("$match", docFind));
			pipeline.add(new Document("$project", filter));

			pipeline.add(new Document("$lookup",
					new Document("from", "DMTinhThanh")
							.append("pipeline",
									Arrays.asList(new Document("$match", new Document("code", tinhThanh)),
											new Document("$project", new Document("_id", 0))))
							.append("as", "TinhThanhInfo")));
			pipeline.add(new Document("$unwind",
					new Document("path", "$TinhThanhInfo").append("preserveNullAndEmptyArrays", true)));
			pipeline.add(new Document("$lookup",
					new Document("from", "DMChiCucThue")
							.append("pipeline",
									Arrays.asList(
											new Document("$match",
													new Document("code", coQuanThue).append("IsDelete", false)),
											new Document("$project", new Document("_id", 0))))
							.append("as", "ChiCucThueInfo")));
			pipeline.add(new Document("$unwind",
					new Document("path", "$ChiCucThueInfo").append("preserveNullAndEmptyArrays", true)));
			pipeline.add(new Document("$lookup",
					new Document("from", "CTTNCNhan")
							.append("pipeline", Arrays.asList(new Document("$match", new Document("$expr", new Document(
									"$and",
									Arrays.asList(new Document("$eq", Arrays.asList("$IssuerId", header.getIssuerId())),
											new Document("$ne", Arrays.asList("$CTSS.TCTBao", "1")),
											new Document(
													"$in",
													Arrays.asList("$Status",
															Arrays.asList(Constants.INVOICE_STATUS.COMPLETE))),
											new Document("$in",
													Arrays.asList(new Document("$toString", "$_id"), ids))))))))
							.append("as", "CTTNCNhan")));
			docFind = new Document("IssuerId", header.getIssuerId()).append("IsDelete", false).append("_id",
					objectIdCTSSot);
			pipeline.add(new Document("$lookup", new Document("from", "CTTNCNhanSS")
					.append("pipeline", Arrays.asList(new Document("$match", docFind))).append("as", "CTTNCNhanSS")));
			pipeline.add(new Document("$unwind",
					new Document("path", "$CTTNCNhanSS").append("preserveNullAndEmptyArrays", true)));
			docTmp = null;

			try (MongoClient mongoClient = cfg.mongoClient()) {
				MongoCollection<Document> collection = mongoClient.getDatabase(cfg.dbName).getCollection("Issuer");
				docTmp = collection.aggregate(pipeline).allowDiskUse(true).iterator().next();
			} catch (Exception e) {

			}

			if (null == docTmp) {
				responseStatus = new MspResponseStatus(9999, "Không tìm thấy thông tin khách hàng.");
				rsp.setResponseStatus(responseStatus);
				return rsp;
			}
			if (docTmp.get("TinhThanhInfo") == null) {
				responseStatus = new MspResponseStatus(9999, "Không tìm thấy thông tin tỉnh/thành phố.");
				rsp.setResponseStatus(responseStatus);
				return rsp;
			}
			if (docTmp.get("ChiCucThueInfo") == null) {
				responseStatus = new MspResponseStatus(9999, "Không tìm thấy thông tin cơ quan thuế.");
				rsp.setResponseStatus(responseStatus);
				return rsp;
			}
			if (docTmp.get("CTTNCNhan") == null || docTmp.getList("CTTNCNhan", Document.class).size() == 0) {
				responseStatus = new MspResponseStatus(9999, "Vui lòng kiểm tra lại danh sách chứng từ.");
				rsp.setResponseStatus(responseStatus);
				return rsp;
			}

			taxCode = docTmp.getString("TaxCode");
			pathDir = docTmp.getEmbedded(Arrays.asList("CTTNCNhanSS", "Dir"), "");
			fileNameXML = docTmp.getEmbedded(Arrays.asList("CTTNCNhanSS", "FileNameXML"), "");
			file = new File(pathDir);
			if (!file.exists())
				file.mkdirs();

			dbf = DocumentBuilderFactory.newInstance();
			db = dbf.newDocumentBuilder();
			doc = db.newDocument();
			doc.setXmlStandalone(true);

			root = doc.createElement("TBao");
			doc.appendChild(root);

			elementContent = doc.createElement("DLTBao");
			elementContent.setAttribute("Id", "data");
			root.appendChild(elementContent);

			elementContent.appendChild(commons.createElementWithValue(doc, "PBan", SystemParams.VERSION_XML_TNCNSS));
			elementContent.appendChild(commons.createElementWithValue(doc, "MSo", "04/SS-CTĐT"));
			elementContent
					.appendChild(commons.createElementWithValue(doc, "Ten", "Thông báo chứng từ điện tử đã lập sai"));
			elementContent.appendChild(commons.createElementWithValue(doc, "Loai", loaiThongBao));
			if ("2".equals(loaiThongBao)) {
				elementContent.appendChild(commons.createElementWithValue(doc, "So", soTBcuaCQT));
				elementContent.appendChild(commons.createElementWithValue(doc, "NTBCCQT",
						commons.convertLocalDateTimeToString(
								commons.convertStringToLocalDate(ngayTBcuaCQT, Constants.FORMAT_DATE.FORMAT_DATE_WEB),
								"yyyy-MM-dd")));
			}
			elementContent.appendChild(commons.createElementWithValue(doc, "MCQT",
					docTmp.getEmbedded(Arrays.asList("ChiCucThueInfo", "code"), "")));
			elementContent.appendChild(commons.createElementWithValue(doc, "TCQT",
					docTmp.getEmbedded(Arrays.asList("ChiCucThueInfo", "name"), "")));
			elementContent.appendChild(commons.createElementWithValue(doc, "TNNT", docTmp.get("Name", "")));
			elementContent.appendChild(commons.createElementWithValue(doc, "MST", taxCode));
			elementContent.appendChild(commons.createElementWithValue(doc, "DDanh",
					docTmp.getEmbedded(Arrays.asList("TinhThanhInfo", "name"), "")));
			elementContent.appendChild(commons.createElementWithValue(doc, "NTBao",
					commons.convertLocalDateTimeToString(LocalDate.now(), "yyyy-MM-dd")));

			elementSubContent = doc.createElement("DSCTu");
			stt = 1;
			if (docTmp.get("CTTNCNhan") != null) {
				for (Document o : docTmp.getList("CTTNCNhan", Document.class)) {
					ObjectId id = (ObjectId) o.get("_id");
					HashMap<String, String> hItem = CTDatas.get(id.toString());
					elementTmp = doc.createElement("CTu");

					elementTmp.appendChild(commons.createElementWithValue(doc, "STT", String.valueOf(stt++)));
					elementTmp.appendChild(commons.createElementWithValue(doc, "KHMSCTu", o.get("MSCTu", "")));
					elementTmp.appendChild(commons.createElementWithValue(doc, "KHCTu", o.get("KHCTu", "")));
					elementTmp
							.appendChild(commons.createElementWithValue(doc, "SCTu", String.valueOf(o.get("SCTu", 0))));
					elementTmp.appendChild(
							commons.createElementWithValue(doc, "NLap", commons.convertLocalDateTimeToString(
									commons.convertDateToLocalDateTime(o.get("NLap", Date.class)), "yyyy-MM-dd")));
					elementTmp.appendChild(commons.createElementWithValue(doc, "LCTDT", "1"));
					elementTmp.appendChild(
							commons.createElementWithValue(doc, "LDo", null == hItem ? "" : hItem.get("LDo")));
					elementSubContent.appendChild(elementTmp);

					HashMap<String, String> hTemp = new HashMap<String, String>();
					hTemp.put("STT", String.valueOf(stt));
					hTemp.put("_id", id.toString());
					hTemp.put("KHMSCTu", o.get("MSCTu", ""));
					hTemp.put("KHCTu", o.get("KHCTu", ""));
					hTemp.put("SCTu", String.valueOf(o.get("SCTu", 0)));
					hTemp.put("NLap", commons.convertLocalDateTimeToString(
							commons.convertDateToLocalDateTime(o.get("NLap", Date.class)), "yyyy-MM-dd"));
					hTemp.put("LCTDT", "1");
					hTemp.put("LDo", null == hItem ? "" : hItem.get("LDo"));
					listDSCTu.add(hTemp);
				}
			}
			elementContent.appendChild(elementSubContent);
			/* END - LUU LAI THONG TIN HDSS */

			isSaveFile = commons.docW3cToFile(doc, pathDir, fileNameXML);
			if (!isSaveFile) {
				throw new Exception("Lưu dữ liệu không thành công.");
			}

			options = new FindOneAndUpdateOptions();
			options.upsert(false);
			options.maxTime(5000, TimeUnit.MILLISECONDS);
			options.returnDocument(ReturnDocument.AFTER);

			Document docUpdate = new Document("IssuerId", header.getIssuerId())
					.append("PBan", SystemParams.VERSION_XML_TNCNSS).append("MSo", "04/SS-CTĐT")
					.append("Ten", "Thông báo hóa đơn điện tử có sai sót").append("Loai", loaiThongBao);
			if ("2".equals(loaiThongBao)) {
				docUpdate.append("So", soTBcuaCQT);
				docUpdate.append("NTBCCQT",
						commons.convertLocalDateTimeToString(
								commons.convertStringToLocalDate(ngayTBcuaCQT, Constants.FORMAT_DATE.FORMAT_DATE_WEB),
								"yyyy-MM-dd"));
			}

			docUpdate.append("MCQT", docTmp.getEmbedded(Arrays.asList("ChiCucThueInfo", "code"), ""))
					.append("TCQT", docTmp.getEmbedded(Arrays.asList("ChiCucThueInfo", "name"), ""))
					.append("TNNT", docTmp.get("Name", "")).append("MST", docTmp.get("TaxCode", ""))
					.append("DDanh", docTmp.getEmbedded(Arrays.asList("TinhThanhInfo", "name"), ""))
					.append("NTBao", commons.convertLocalDateTimeToString(LocalDate.now(), "yyyy-MM-dd"))
					.append("NTBaoDate", LocalDate.now()).append("DSCTu", listDSCTu).append("Dir", pathDir)
					.append("FileNameXML", fileNameXML).append("TinhThanhInfo", docTmp.get("TinhThanhInfo"))
					.append("ChiCucThueInfo", docTmp.get("ChiCucThueInfo")).append("InfoUpdated",
							new Document("UpdatedDate", LocalDateTime.now()).append("UpdatedUserID", header.getUserId())
									.append("UpdatedUserName", header.getUserName())
									.append("UpdatedUserFullName", header.getUserFullName()));
			docFind = new Document("IssuerId", header.getIssuerId()).append("IsDelete", false).append("_id",
					objectIdCTSSot);

			try (MongoClient mongoClient = cfg.mongoClient()) {
				MongoCollection<Document> collection = mongoClient.getDatabase(cfg.dbName).getCollection("CTTNCNhanSS");
				collection.findOneAndUpdate(docFind, new Document("$set", docUpdate), options);
			} catch (Exception e) {
			}

			responseStatus = new MspResponseStatus(0, "SUCCESS");
			rsp.setResponseStatus(responseStatus);
			return rsp;
		case Constants.MSG_ACTION_CODE.DELETE:
			objectIdCTSSot = null;
			try {
				objectIdCTSSot = new ObjectId(_id);
			} catch (Exception e) {
			}

			docFind = new Document("IssuerId", header.getIssuerId()).append("_id", objectIdCTSSot)
					.append("IsDelete", false).append("Status", new Document("$in",
							Arrays.asList(Constants.INVOICE_STATUS.CREATED, Constants.INVOICE_STATUS.PENDING)));
			docTmp = null;

			try (MongoClient mongoClient = cfg.mongoClient()) {
				MongoCollection<Document> collection = mongoClient.getDatabase(cfg.dbName).getCollection("CTTNCNhanSS");
				docTmp = collection.find(docFind).allowDiskUse(true).iterator().next();
			} catch (Exception e) {
			}

			if (null == docTmp) {
				responseStatus = new MspResponseStatus(9999, "Không tìm thấy thông tin thông báo.");
				rsp.setResponseStatus(responseStatus);
				return rsp;
			}

			options = new FindOneAndUpdateOptions();
			options.upsert(false);
			options.maxTime(5000, TimeUnit.MILLISECONDS);
			options.returnDocument(ReturnDocument.AFTER);

			try (MongoClient mongoClient = cfg.mongoClient()) {
				MongoCollection<Document> collection = mongoClient.getDatabase(cfg.dbName).getCollection("CTTNCNhanSS");
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
		
		case Constants.MSG_ACTION_CODE.SEND_CQT:
			objectIdCTSSot = null;
			try {
				objectIdCTSSot = new ObjectId(_id);
			} catch (Exception e) {
			}

			docFind = new Document("IssuerId", header.getIssuerId()).append("_id", objectIdCTSSot)
					.append("IsDelete", false).append("Status", Constants.INVOICE_STATUS.PENDING)
					.append("SignStatus", Constants.INVOICE_SIGN_STATUS.SIGNED);
			docTmp = null;
			try (MongoClient mongoClient = cfg.mongoClient()) {
				MongoCollection<Document> collection = mongoClient.getDatabase(cfg.dbName).getCollection("CTTNCNhanSS");
				docTmp = collection.find(docFind).allowDiskUse(true).iterator().next();
			} catch (Exception e) {
			}

			if (null == docTmp) {
				responseStatus = new MspResponseStatus(9999, "Không tìm thấy thông tin thông báo.");
				rsp.setResponseStatus(responseStatus);
				return rsp;
			}

			mtdiep = docTmp.get("MTDiep", "");
			String mst =  docTmp.get("MST", "");
			String dir = docTmp.get("Dir", "");
			String fileName = _id + "_signed.xml";
			file = new File(dir, fileName);
			if (!file.exists() || !file.isFile()) {
				responseStatus = new MspResponseStatus(9999, "Không tìm thấy thông tin thông báo đã ký.");
				rsp.setResponseStatus(responseStatus);
				return rsp;
			}
			doc = commons.fileToDocument(file, true);
			if (null == doc) {
				responseStatus = new MspResponseStatus(9999, "Dữ liệu thông báo đã ký không tồn tại.");
				rsp.setResponseStatus(responseStatus);
				return rsp;
			}

			rTCTN = tctnService.callTiepNhanThongDiep("304", mtdiep, mst, "1", doc);
			if (rTCTN == null) {
				rTCTN = tctnService.callTiepNhanThongDiep("304", mtdiep, mst, "1", doc);
			}
			if (rTCTN == null) {
				responseStatus = new MspResponseStatus(9999,
						"Lỗi khi gửi chứng từ sai sót đến CQT. Vui lòng liên hệ nhà cung cấp để được xử lý!");
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

			codeTTTNhan = commons
					.getTextFromNodeXML((Element) xPath.evaluate("DLieu/TBao/TTTNhan", nodeDLieu, XPathConstants.NODE));
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
				MongoCollection<Document> collection = mongoClient.getDatabase(cfg.dbName).getCollection("CTTNCNhanSS");
				collection.findOneAndUpdate(docFind,
						new Document("$set",
								new Document("Status", "PROCESSING").append("InfoSendCQT",
										new Document("Date", LocalDateTime.now()).append("UserID", header.getUserId())
												.append("UserName", header.getUserName())
												.append("UserFullName", header.getUserFullName()))),
						options);
			} catch (Exception e) {
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
		String soChungTu = "";
		String fromDate = "";
		String toDate = "";
		String status = "";
		JsonNode jsonData = null;
		if(objData != null) {
			jsonData = Json.serializer().nodeFromObject(objData);
			soChungTu = commons.getTextJsonNode(jsonData.at("/SoChungTu")).replaceAll("\\s", "");
			status = commons.getTextJsonNode(jsonData.at("/Status")).replaceAll("\\s", "");
			fromDate = commons.getTextJsonNode(jsonData.at("/FromDate")).replaceAll("\\s", "");
			toDate = commons.getTextJsonNode(jsonData.at("/ToDate")).replaceAll("\\s", "");
		}
		
		MsgRsp rsp = new MsgRsp(header);
		MspResponseStatus responseStatus = null;
		
		ObjectId objectId = null;
		Document docTmp = null;
		
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

		Document docMatch = new Document("IssuerId", header.getIssuerId())
				.append("IsDelete", false);
		if(null != docMatchDate)
			docMatch.append("NTBaoDate", docMatchDate);
		if(!"".equals(status))
			docMatch.append("Status", commons.regexEscapeForMongoQuery(status));
		if(!"".equals(soChungTu))
			docMatch.append("DSCTu.SCTu", soChungTu);
		
		List<Document> pipeline = new ArrayList<Document>();
		pipeline.add(new Document("$match", docMatch));
		pipeline.add(
			new Document("$sort", 
				new Document("NTBao", -1).append("_id", -1)
			)
		);
		
		pipeline.addAll(createFacetForSearchNotSort(page));
		
		try (MongoClient mongoClient = cfg.mongoClient()){
			MongoCollection<Document> collection = mongoClient.getDatabase(cfg.dbName).getCollection("CTTNCNhanSS");
			docTmp = collection.aggregate(pipeline).allowDiskUse(true).iterator().next();	
		} catch (Exception e) {
			
		}
					
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
				hItem.put("SignStatus", doc.get("SignStatus"));
				hItem.put("MSo", doc.get("MSo"));
				hItem.put("Ten", doc.get("Ten"));
				hItem.put("Loai", doc.get("Loai"));
				hItem.put("DSCTu", doc.get("DSCTu"));
				hItem.put("NTBaoDate", doc.get("NTBaoDate"));
				hItem.put("InfoCreated", doc.get("InfoCreated"));
				hItem.put("DSLoi", doc.get("DSLoi"));
				hItem.put("MTDiep", doc.get("MTDiep"));
				hItem.put("MTDTChieu", doc.get("MTDTChieu"));
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
				.append("SignStatus", Constants.INVOICE_SIGN_STATUS.NOSIGN).append("NTBaoDate", LocalDate.now());

		List<Document> pipeline = new ArrayList<Document>();
		pipeline.add(new Document("$match", docFind));
		pipeline.add(
				new Document("$lookup",
						new Document("from", "CTTNCNhan")
								.append("let",
										new Document("vIssuerId", "$IssuerId").append("vDSCTu", new Document("$map",
												new Document("input", "$DSCTu").append("as", "o").append("in",
														"$$o._id"))))
								.append("pipeline", Arrays.asList(new Document("$match",
										new Document("$expr", new Document("$and", Arrays.asList(
												new Document("$eq", Arrays.asList("$IssuerId", "$$vIssuerId")),
												new Document("$ne",
														Arrays.asList(new Document("$type", "$HDSS"), "object")),
												new Document("$eq",
														Arrays.asList("$Status", Constants.INVOICE_STATUS.COMPLETE)),
												new Document("$in", Arrays.asList(new Document("$toString", "$_id"),
														"$$vDSCTu"))))))))
								.append("as", "CTTNCNhan")));

		Document docTmp = null;
		try (MongoClient mongoClient = cfg.mongoClient()) {
			MongoCollection<Document> collection = mongoClient.getDatabase(cfg.dbName).getCollection("CTTNCNhanSS");
			docTmp = collection.aggregate(pipeline).allowDiskUse(true).iterator().next();
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
				.append("SignStatus", Constants.INVOICE_SIGN_STATUS.NOSIGN)
				.append("NTBaoDate", LocalDate.now());
		
		Document docTmp = null;

		try (MongoClient mongoClient = cfg.mongoClient()){
			MongoCollection<Document> collection = mongoClient.getDatabase(cfg.dbName).getCollection("CTTNCNhanSS");
			docTmp = collection.find(docFind).allowDiskUse(true).iterator().next();	
		} catch (Exception e) {
			
		}
		
		if(null == docTmp) {
			responseStatus = new MspResponseStatus(9999, "Không tìm thấy thông báo chứng từ sai sót.");
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
		
		try (MongoClient mongoClient = cfg.mongoClient()){
			MongoCollection<Document> collection = mongoClient.getDatabase(cfg.dbName).getCollection("CTTNCNhanSS");
			collection.findOneAndUpdate(
					docFind, 
					new Document("$set", 
						new Document("SignStatus", Constants.INVOICE_SIGN_STATUS.SIGNED)
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
		} catch (Exception e) {
			// TODO: handle exception
		}

		responseStatus = new MspResponseStatus(0, Constants.MAP_ERROR.get(0));
		rsp.setResponseStatus(responseStatus);
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
				.append("IsDelete", false).append("_id", objectId);
		
		Document docTmp = null;

		MongoClient mongoClient = cfg.mongoClient();
		MongoCollection<Document> collection = mongoClient.getDatabase(cfg.dbName).getCollection("CTTNCNhanSS");
		
		try {
			docTmp = collection.find(docFind).allowDiskUse(true).iterator().next();				
		} catch (Exception e) {
			
		}
		
		mongoClient.close();
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
		
		FindOneAndUpdateOptions options = null;
		Document docFind = null;
		Document docTmp = null;
		
		ObjectId objectIdCTSSot = null;
		try {
			objectIdCTSSot = new ObjectId(_id);
		}catch(Exception e) {}
		
		docFind = new Document("IssuerId", header.getIssuerId()).append("_id", objectIdCTSSot)
				.append("IsDelete", false).append("Status", Constants.INVOICE_STATUS.PROCESSING)
				.append("SignStatus", Constants.INVOICE_SIGN_STATUS.SIGNED);
		
		try (MongoClient mongoClient = cfg.mongoClient()) {
			MongoCollection<Document> collection = mongoClient.getDatabase(cfg.dbName).getCollection("CTTNCNhanSS");
			docTmp = collection.find(docFind).allowDiskUse(true).iterator().next();
		} catch (Exception e) {
		}
		
		if (null == docTmp) {
			responseStatus = new MspResponseStatus(9999, "Không tìm thấy thông tin thông báo.");
			rsp.setResponseStatus(responseStatus);
			return rsp;
		}
		
		String mtdiep = docTmp.get("MTDiep", "");
		org.w3c.dom.Document rTCTN = tctnService.callTraCuuThongDiep(mtdiep);
		if (rTCTN == null) {
			rTCTN = tctnService.callTraCuuThongDiep(mtdiep);
		}
		
		if(rTCTN == null) {
			responseStatus = new MspResponseStatus(9999, "Kết nối với TCTN không thành công.");
			rsp.setResponseStatus(responseStatus);
			return rsp;
		}
			
		XPath xPath = XPathFactory.newInstance().newXPath();
		Node nodeKetQuaTraCuu = (Node) xPath.evaluate("/KetQuaTraCuu", rTCTN, XPathConstants.NODE);
		String maKetQua = commons.getTextFromNodeXML((Element) xPath.evaluate("MaKetQua", nodeKetQuaTraCuu, XPathConstants.NODE));
		String moTaKetQua = commons.getTextFromNodeXML((Element) xPath.evaluate("MoTaKetQua", nodeKetQuaTraCuu, XPathConstants.NODE));
		
		if(!"0".equals(maKetQua)) {
			responseStatus = new MspResponseStatus(9999, moTaKetQua);
			rsp.setResponseStatus(responseStatus);
			return rsp;
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
		}
		
		if(nodeTDiep == null) {
			responseStatus = new MspResponseStatus(9999, "Không đọc được kết quả tra cứu.");
			rsp.setResponseStatus(responseStatus);
			return rsp;
		}
		
		String CQT_MLTDiep = commons.getTextFromNodeXML((Element) xPath.evaluate("TTChung/MLTDiep", nodeTDiep, XPathConstants.NODE));
		String MTDTChieu = commons.getTextFromNodeXML((Element) xPath.evaluate("TTChung/MTDTChieu", nodeTDiep, XPathConstants.NODE));
		
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
		options = new FindOneAndUpdateOptions();
		options.upsert(false);
		options.maxTime(5000, TimeUnit.MILLISECONDS);
		options.returnDocument(ReturnDocument.AFTER);
		
		Document docUpdate = new Document();
		if("301".equals(CQT_MLTDiep)) {
			NodeList nodeDSLDo = (NodeList) xPath.evaluate("DLieu/TBao/DLTBao/KHLKhac/DSLDo/LDo", nodeTDiep, XPathConstants.NODESET);
			if(nodeDSLDo != null) {
				List<HashMap<String, String>> DSLoi = new ArrayList<HashMap<String,String>>();
				HashMap<String, String> hItem = new HashMap<String, String>();
				for	(int i = 0; i < nodeDSLDo.getLength(); i++) {
					hItem = new HashMap<String, String>();
					Node nodeLDo = (Node) nodeDSLDo.item(i);
					hItem.put("STT", commons.getTextFromNodeXML((Element) xPath.evaluate("STT", nodeLDo, XPathConstants.NODE)));
					hItem.put("MLoi", commons.getTextFromNodeXML((Element) xPath.evaluate("MLoi", nodeLDo, XPathConstants.NODE)));
					hItem.put("MTLoi", commons.getTextFromNodeXML((Element) xPath.evaluate("MTLoi", nodeLDo, XPathConstants.NODE)));
					DSLoi.add(hItem);
				}
				docUpdate.append("Status", Constants.INVOICE_STATUS.ERROR_CQT)
				.append("DSLoi", DSLoi);	
			}
			
			
			docUpdate.append("Status", Constants.INVOICE_STATUS.COMPLETE)
				.append("MTDTChieu", MTDTChieu);	
				
				List<WriteModel<Document>> updates = new ArrayList<WriteModel<Document>>();
				UpdateOptions udateOptions = new UpdateOptions();
				udateOptions.upsert(false);
				for (Document doc : docTmp.getList("DSCTu", Document.class)) {
					String ldo = doc.get("LDo", "");
					String id= doc.get("_id", "");
					ObjectId objectIdCTu = null;
					try {
						objectIdCTu = new ObjectId(id);
					}catch(Exception e) {}
					
					Document docFilter = new Document("IssuerId", header.getIssuerId())
							.append("_id", objectIdCTu);
					updates.add(new UpdateOneModel<>(docFilter,
							new Document("$set",
									new Document("CTSS",
											new Document("LDo", ldo))),
							udateOptions));
				}	
				
				try (MongoClient mongoClient = cfg.mongoClient()) {
					MongoCollection<Document> collection = mongoClient.getDatabase(cfg.dbName).getCollection("CTTNCNhan");
		            collection.bulkWrite(updates);	
				} catch (Exception e) {

				}
				responseStatus = new MspResponseStatus(0, "SUCCESS");
		} else { // 213 or some values else
			NodeList nodeDSLDo = (NodeList) xPath.evaluate("DLieu/TBao/DLTBao/KHLKhac/DSLDo/LDo", nodeTDiep, XPathConstants.NODESET);
			List<HashMap<String, String>> DSLoi = new ArrayList<HashMap<String,String>>();
			if(nodeDSLDo != null) {
				HashMap<String, String> hItem = new HashMap<String, String>();
				for	(int i = 0; i < nodeDSLDo.getLength(); i++) {
					hItem = new HashMap<String, String>();
					Node nodeLDo = (Node) nodeDSLDo.item(i);
					hItem.put("STT", commons.getTextFromNodeXML((Element) xPath.evaluate("STT", nodeLDo, XPathConstants.NODE)));
					hItem.put("MLoi", commons.getTextFromNodeXML((Element) xPath.evaluate("MLoi", nodeLDo, XPathConstants.NODE)));
					hItem.put("MTLoi", commons.getTextFromNodeXML((Element) xPath.evaluate("MTLoi", nodeLDo, XPathConstants.NODE)));
					DSLoi.add(hItem);
				}
			}
			docUpdate.append("Status", Constants.INVOICE_STATUS.ERROR_CQT)
			.append("DSLoi", DSLoi);
			responseStatus = new MspResponseStatus(999, "Lỗi từ CQT");
		}
		
		try (MongoClient mongoClient = cfg.mongoClient()) {
			MongoCollection<Document> collection = mongoClient.getDatabase(cfg.dbName).getCollection("CTTNCNhanSS");
			collection.findOneAndUpdate(
					docFind, 
					new Document("$set", docUpdate), 
					options
				);			
		} catch (Exception e) {
		}
		
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
		}catch(Exception e) {}
		
		Document docFind = new Document("IssuerId", header.getIssuerId())
				.append("_id", objectId)
				.append("IsDelete", new Document("$ne", true))
				.append("Status", new Document("$in", Arrays.asList("ERROR_CQT", "PROCESSING", "COMPLETE")))
				.append("SignStatus", Constants.INVOICE_SIGN_STATUS.SIGNED);
		
		Document docTmp = null;

		MongoClient mongoClient = cfg.mongoClient();
		MongoCollection<Document> collection = mongoClient.getDatabase(cfg.dbName).getCollection("CTTNCNhanSS");
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
		
		String mtdiep = docTmp.get("MTDiep", "");
		org.w3c.dom.Document rTCTN = tctnService.callTraCuuThongDiep(mtdiep);
		if(rTCTN == null) {
			responseStatus = new MspResponseStatus(9999, "Kết nối với TCTN không thành công.");
			rsp.setResponseStatus(responseStatus);
			return rsp;
		}
		
		/*LUU LAI FILE KQTN*/
		String dir = docTmp.get("Dir", "");
		String fileName = _id + "_" + mtdiep + ".xml";
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
	
		for (int i = 1; i <= 20; i++) {
			if (xPath.evaluate("DuLieu/TDiep[" + i + "]", nodeKetQuaTraCuu, XPathConstants.NODE) == null)
				break;
			nodeTDiep = (Node) xPath.evaluate("DuLieu/TDiep[" + i + "]", nodeKetQuaTraCuu, XPathConstants.NODE);
			CQT_MLTDiep = commons
					.getTextFromNodeXML((Element) xPath.evaluate("TTChung/MLTDiep", nodeTDiep, XPathConstants.NODE));
			stt += 1;
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

			} else if ("301".equals(CQT_MLTDiep)) {
				hItem = new HashMap<String, Object>();
				hItem.put("STT", stt);
				hItem.put("Date", commons.getTextFromNodeXML(
						(Element) xPath.evaluate("DLieu/TBao/DLTBao/TGNhan", nodeTDiep, XPathConstants.NODE)));
				hItem.put("MLoi", CQT_MLTDiep);
				hItem.put("MTLoi", "Đã hoàn thành");
				rowsReturn.add(hItem);
			} else {
				hItem = new HashMap<String, Object>();
				hItem.put("STT", stt);
				hItem.put("Date", commons.getTextFromNodeXML(
						(Element) xPath.evaluate("DLieu/TBao/DLTBao/TGGui", nodeTDiep, XPathConstants.NODE)));
				hItem.put("MLoi", commons.getTextFromNodeXML((Element) xPath
						.evaluate("DLieu/TBao/DLTBao/KHLKhac/DSLDo/LDo/MLoi", nodeTDiep, XPathConstants.NODE)));
				hItem.put("MTLoi", commons.getTextFromNodeXML((Element) xPath
						.evaluate("DLieu/TBao/DLTBao/KHLKhac/DSLDo/LDo/MTLoi", nodeTDiep, XPathConstants.NODE)));
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
		String _content = commons.getTextJsonNode(jsonData.at("/_content")).trim().replaceAll("\\s+", " ");

		MsgRsp rsp = new MsgRsp(header);
		rsp.setMsgPage(page);
		MspResponseStatus responseStatus = null;

		ObjectId objectId = null;
		try {
			objectId = new ObjectId(_id);
		} catch (Exception e) {
		}

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

		Document docTmp = null;
		try (MongoClient mongoClient = cfg.mongoClient()){
			MongoCollection<Document> collection = mongoClient.getDatabase(cfg.dbName).getCollection("CTTNCNhanSS");
			docTmp = collection.aggregate(pipeline).allowDiskUse(true).iterator().next();
		} catch (Exception e) {

		}
		
		if (null == docTmp) {
			responseStatus = new MspResponseStatus(9999, "Không tìm thấy thông tin chứng từ.");
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
		
		

		String khctu = "";
		String sctu = "";
		for (Document oo : docTmp.getList("DSCTu", Document.class)) {
			khctu = oo.get("KHCTu", "");
			sctu = oo.get("SCTu", "");
		}

		String MailJet = docTmp.getEmbedded(Arrays.asList("ConfigEmail", "MailJet"), "");
		String dir = docTmp.getString("Dir");
		String fileName = _id + "_signed.xml";
		String fileNamePDF = _id + ".pdf";
		File file = new File(dir, fileName);

		/* THUC HIEN GUI MAIL */

		if (file.exists() && file.isFile()) {
			org.w3c.dom.Document doc = commons.fileToDocument(file);
            String fileNameJP = "04SS-CTDT.jrxml";
            File fileJP = new File(SystemParams.DIR_E_INVOICE_TEMPLATE, fileNameJP);
			ByteArrayOutputStream baosPDF = null;

			baosPDF = jpUtils.print04CTDT(fileJP, doc);
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
			listNames.add(khctu + "-" + sctu + ".pdf");
		}
		boolean boo = false;
		MailConfig mailConfig = new MailConfig(docTmp.get("ConfigEmail", Document.class));
		
		mailConfig.setNameSend(docTmp.getEmbedded(Arrays.asList("TNNT"), ""));

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
			try (MongoClient mongoClient = cfg.mongoClient()){

				MongoCollection<Document> collection = mongoClient.getDatabase(cfg.dbName).getCollection("LogEmailUser");
				collection.insertOne(new Document("IssuerId", header.getIssuerId()).append("Title", _title)
						.append("Email", _email).append("IsActive", boo).append("MailCheck", boo)
						.append("IsDelete", false).append("EmailContent", _content)
				);
			} catch (Exception ex) {
			}
		}

		responseStatus = new MspResponseStatus(0, "SUCCESS");
		rsp.setResponseStatus(responseStatus);
		return rsp;
	}
}
