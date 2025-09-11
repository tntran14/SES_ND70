package vn.sesgroup.hddt.user.impl;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.Normalizer;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
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
import vn.sesgroup.hddt.user.dao.TKhaiDAO;
import vn.sesgroup.hddt.user.service.TCTNService;
import vn.sesgroup.hddt.utility.Constants;
import vn.sesgroup.hddt.utility.Json;
import vn.sesgroup.hddt.utility.SystemParams;

@Repository
@Transactional
public class TKhaiImpl extends AbstractDAO implements TKhaiDAO{
	private static final Logger log = LogManager.getLogger(TKhaiImpl.class);

	@Autowired
	ConfigConnectMongo cfg;
	
	@Autowired TCTNService tctnService;	

	//CHUYEN DOI TIENG VIET CÓ DAU THANH KHONG CO DAU
	public String removeAccent(String s) {
		  
		  String temp = Normalizer.normalize(s, Normalizer.Form.NFD);
		  Pattern pattern = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");
		  return pattern.matcher(temp).replaceAll("");
		 }
	
	@Transactional(rollbackFor = {Exception.class})
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
		String tenNnt = commons.getTextJsonNode(jsonData.at("/TenNnt")).trim().replaceAll("\\s+", " ");
		String mauSo = commons.getTextJsonNode(jsonData.at("/MauSo")).replaceAll("\\s", "");
		String ten = commons.getTextJsonNode(jsonData.at("/Ten")).trim().replaceAll("\\s+", " ");
		String hThuc = commons.getTextJsonNode(jsonData.at("/HThuc")).trim().replaceAll("\\s+", " ");
//		String hThuc = "1";							//1: Đăng ký mới, 2:Thay đổi thông tin
		String mst = commons.getTextJsonNode(jsonData.at("/Mst")).replaceAll("\\s", "");
		String tinhThanh = commons.getTextJsonNode(jsonData.at("/TinhThanh")).replaceAll("\\s", "");
		String cqtQLy = commons.getTextJsonNode(jsonData.at("/CqtQLy")).replaceAll("\\s", "");
		String nlHe = commons.getTextJsonNode(jsonData.at("/NLHe")).trim().replaceAll("\\s+", " ");
		String dcLHe = commons.getTextJsonNode(jsonData.at("/DCLHe")).trim().replaceAll("\\s+", " ");
		String dcCTDTu = commons.getTextJsonNode(jsonData.at("/DCCTDTu")).trim().replaceAll("\\s+", " ");
		String dtLHe = commons.getTextJsonNode(jsonData.at("/DTLHe")).trim().replaceAll("\\s+", " ");
		String nLap = commons.getTextJsonNode(jsonData.at("/NLap")).trim().replaceAll("\\s+", " ");
		String htHDon = commons.getTextJsonNode(jsonData.at("/HTHDon")).trim().replaceAll("\\s+", " ");
		String pthuc = commons.getTextJsonNode(jsonData.at("/PThuc")).trim().replaceAll("\\s+", " ");
		String CMMTT = commons.getTextJsonNode(jsonData.at("/CMMTT")).trim().replaceAll("\\s+", " ");
		String lhdSDung_HDGTGT = commons.getTextJsonNode(jsonData.at("/LHDSDung_HDGTGT")).trim().replaceAll("\\s+", " ");
		String lhdsDung_HDBHang = commons.getTextJsonNode(jsonData.at("/LHDSDung_HDBHang")).trim().replaceAll("\\s+", " ");
		String lhdsDung_HDBTSCong = commons.getTextJsonNode(jsonData.at("/LHDSDung_HDBTSCong")).trim().replaceAll("\\s+", " ");
		String lhdsDung_HDBHDTQGia = commons.getTextJsonNode(jsonData.at("/LHDSDung_HDBHDTQGia")).trim().replaceAll("\\s+", " ");
		String lhdsDung_HDKhac = commons.getTextJsonNode(jsonData.at("/LHDSDung_HDKhac")).trim().replaceAll("\\s+", " ");
		String lhdsDung_CTu = commons.getTextJsonNode(jsonData.at("/LHDSDung_CTu")).trim().replaceAll("\\s+", " ");
		List<Object> rowDSCTSSDung = new ArrayList<Object>();
		
		MsgRsp rsp = new MsgRsp(header);
		rsp.setMsgPage(page);
		MspResponseStatus responseStatus = null;
		
		List<Document> pipeline = null;
		Document docFind = null;
		Document docTmp = null;
		Document docTmp1 = null;
		Document docUpsert = null;
		Document docUpsert1 = null;
		String taxCode = "";
		String fileNameXML = "";
		String pathDir = "";
		Path path = null;
		File file = null;
		
		ObjectId objectId = null;
		ObjectId objectIdUser = null;
		ObjectId objectIdTK = null;
		ObjectId objectIdDMCTS = null;
		HashMap<String, Object> hO = null;
		
		FindOneAndUpdateOptions options = null;
		Document docR = null;
		Document docR1 = null;

		List<Document> docs = null;
		Document docEInvoiceTTDC = null;
		Document docTTHDLQuan = null;
		
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
			try {
				objectId = new ObjectId(header.getIssuerId());
			}catch(Exception e) {}
			try {
				objectIdUser = new ObjectId(header.getUserId());
			}catch(Exception e) {}
			
			// Kiem tra da co chu ky so chua
			pipeline = new ArrayList<Document>();
			Date currentDate = new Date(); 
			docFind = new Document("IssuerId", header.getIssuerId())
					.append("IsDelete",new Document("$ne", true))
					.append("DSCTSSDung.DNgay", new Document("$gte", currentDate));  
					;
			pipeline.add(new Document("$match", docFind));

			System.out.println();
			MongoClient mongoClient = cfg.mongoClient();
			MongoCollection<Document> collection = mongoClient.getDatabase(cfg.dbName).getCollection("DMCTSo");
			docTmp = null;
			try {
				docTmp = collection.aggregate(pipeline).allowDiskUse(true).iterator().next();
			} catch (Exception e) {

			}
			mongoClient.close();
			if(null == docTmp) {
				responseStatus = new MspResponseStatus(9999, "Quý khách vui lòng vào menu Hệ Thống/Chứng thư số để thêm chữ ký số vào hệ thống trước khi Đăng ký tờ khai 01.");
				rsp.setResponseStatus(responseStatus);
				return rsp;
			}

			/*KIEM TRA THONG TIN KHACH HANG - USER - TINH THANH - CO QUAN THUE CO TON TAI KHONG*/
			docFind = new Document("_id", objectId).append("IsActive", true).append("IsDelete", new Document("$ne", true));
			pipeline = new ArrayList<Document>();
			pipeline.add(new Document("$match", docFind));
			pipeline.add(
				new Document("$lookup", 
					new Document("from", "Users")
					.append("pipeline", 
						Arrays.asList(
							new Document("$match", 
								new Document("IssuerId", header.getIssuerId()).append("_id", objectIdUser)
								.append("IsActive", true).append("IsDelete", new Document("$ne", true))
							),
							new Document("$project", new Document("_id", 1).append("UserName", 1).append("FullName", 1)),
							new Document("$limit", 1)
						)
					)
					.append("as", "UserInfo")
				)
			);
			pipeline.add(new Document("$unwind", new Document("path", "$UserInfo").append("preserveNullAndEmptyArrays", true)));
			pipeline.add(
				new Document("$lookup", 
					new Document("from", "DMTinhThanh")
					.append("pipeline", 
						Arrays.asList(
							new Document("$match", new Document("IsDelete", new Document("$ne", true)).append("code", commons.regexEscapeForMongoQuery(tinhThanh))),
							new Document("$project", new Document("_id", 0).append("code", 1).append("name", 1))
						)
					)
					.append("as", "DMTinhThanhInfo")
				)
			);
			pipeline.add(new Document("$unwind", new Document("path", "$DMTinhThanhInfo").append("preserveNullAndEmptyArrays", true)));
			pipeline.add(
				new Document("$lookup", 
					new Document("from", "DMChiCucThue")
					.append("pipeline", 
						Arrays.asList(
							new Document("$match", new Document("IsDelete", new Document("$ne", true)).append("code", commons.regexEscapeForMongoQuery(cqtQLy))),
							new Document("$project", new Document("_id", 0).append("code", 1).append("name", 1))
						)
					)
					.append("as", "DMChiCucThueInfo")
				)
			);
			pipeline.add(new Document("$unwind", new Document("path", "$DMChiCucThueInfo").append("preserveNullAndEmptyArrays", true)));
				
		
			
			mongoClient = cfg.mongoClient();
			collection = mongoClient.getDatabase(cfg.dbName).getCollection("Issuer");
			docTmp = null;
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
			if(docTmp.get("UserInfo") == null) {
				responseStatus = new MspResponseStatus(9999, "Không tìm thấy thông tin người dùng.");
				rsp.setResponseStatus(responseStatus);
				return rsp;
			}
			
			if(docTmp.get("DMTinhThanhInfo") == null || docTmp.get("DMChiCucThueInfo") == null) {
				responseStatus = new MspResponseStatus(9999, "Vui lòng kiểm tra lại tỉnh/thành phố và cơ quan thuế.");
				rsp.setResponseStatus(responseStatus);
				return rsp;
			}
			
			taxCode = docTmp.getString("TaxCode");
			
			/*TAO FILE XML*/
			objectIdTK = new ObjectId();
			objectIdDMCTS = new ObjectId();
			path = Paths.get(SystemParams.DIR_E_INVOICE_TKHAI, taxCode);
			pathDir = path.toString();
			file = path.toFile();
			if(!file.exists()) file.mkdirs();
			
			/*TAO FILE XML*/
			fileNameXML = objectIdTK.toString() + ".xml";
			
			 dbf = DocumentBuilderFactory.newInstance();
			 db = dbf.newDocumentBuilder();
			 doc = db.newDocument();
			doc.setXmlStandalone(true);
			
			root = doc.createElement("TKhai");
			doc.appendChild(root);
			
			 elementTmp = null;
			 elementSubTmp = null;
			 elementContent = doc.createElement("DLTKhai");
			elementContent.setAttribute("Id", "data");
			root.appendChild(elementContent);
			
			/*THONG TIN CHUNG TO KHAI*/
			elementSubContent = doc.createElement("TTChung");
			elementSubContent.appendChild(commons.createElementWithValue(doc, "PBan",  SystemParams.VERSION_XML_TOKHAI));
			elementSubContent.appendChild(commons.createElementWithValue(doc, "MSo", mauSo));
			elementSubContent.appendChild(commons.createElementWithValue(doc, "Ten", ten));
			elementSubContent.appendChild(commons.createElementWithValue(doc, "HThuc", hThuc));
			elementSubContent.appendChild(commons.createElementWithValue(doc, "TNNT", tenNnt));
			elementSubContent.appendChild(commons.createElementWithValue(doc, "MST", mst));
			elementSubContent.appendChild(commons.createElementWithValue(doc, "CQTQLy", docTmp.getEmbedded(Arrays.asList("DMChiCucThueInfo", "name"), "")));
			elementSubContent.appendChild(commons.createElementWithValue(doc, "MCQTQLy", cqtQLy));
			elementSubContent.appendChild(commons.createElementWithValue(doc, "NLHe", nlHe));
			elementSubContent.appendChild(commons.createElementWithValue(doc, "DCLHe", dcLHe));
			elementSubContent.appendChild(commons.createElementWithValue(doc, "DCTDTu", dcCTDTu));
			elementSubContent.appendChild(commons.createElementWithValue(doc, "DTLHe", dtLHe));
			elementSubContent.appendChild(commons.createElementWithValue(doc, "DDanh", docTmp.getEmbedded(Arrays.asList("DMTinhThanhInfo", "name"), "")));
			elementSubContent.appendChild(commons.createElementWithValue(doc, "NLap", commons.convertLocalDateTimeStringToString(nLap, Constants.FORMAT_DATE.FORMAT_DATE_WEB, Constants.FORMAT_DATE.FORMAT_DATE_EINVOICE)));
			elementContent.appendChild(elementSubContent);
			
			/*NOI DUNG CHI TIET TO KHAI*/
			elementSubContent = doc.createElement("NDTKhai");
			/*HINH THUC HOA DON AP DUNG*/
			elementTmp = doc.createElement("HTHDon");
			elementTmp.appendChild(commons.createElementWithValue(doc, "CMa", "CMa".equals(htHDon)? "1": "0"));
			if(CMMTT.equals("on")) {
				elementTmp.appendChild(commons.createElementWithValue(doc, "CMTMTTien", "on".equals(CMMTT)? "1": "0"));
			}
			elementTmp.appendChild(commons.createElementWithValue(doc, "KCMa", "KCMa".equals(htHDon)? "1": "0"));
			elementSubContent.appendChild(elementTmp);
			/*HINH THUC GUI DU LIEU HDDT - FIX GIA TRI*/
			elementTmp = doc.createElement("HTGDLHDDT");
			elementTmp.appendChild(commons.createElementWithValue(doc, "NNTDBKKhan", "0"));
			elementTmp.appendChild(commons.createElementWithValue(doc, "NNTKTDNUBND", "0"));
			elementTmp.appendChild(commons.createElementWithValue(doc, "CDLTTDCQT", "0"));
			elementTmp.appendChild(commons.createElementWithValue(doc, "CDLQTCTN", "0"));
			elementSubContent.appendChild(elementTmp);
			/*PHUONG THUC CHUYEN DU LIEU*/
			elementTmp = doc.createElement("PThuc");
			elementTmp.appendChild(commons.createElementWithValue(doc, "CDDu", "CDDu".equals(pthuc)? "1": "0"));
			elementTmp.appendChild(commons.createElementWithValue(doc, "CBTHop", "CBTHop".equals(pthuc)? "1": "0"));
			elementSubContent.appendChild(elementTmp);
			/*LOAI HD SU DUNG*/
			elementTmp = doc.createElement("LHDSDung");
			elementTmp.appendChild(commons.createElementWithValue(doc, "HDGTGT", "on".equals(lhdSDung_HDGTGT)? "1": "0"));
			elementTmp.appendChild(commons.createElementWithValue(doc, "HDBHang", "on".equals(lhdsDung_HDBHang)? "1": "0"));
			elementTmp.appendChild(commons.createElementWithValue(doc, "HDBTSCong", "on".equals(lhdsDung_HDBTSCong)? "1": "0"));
			elementTmp.appendChild(commons.createElementWithValue(doc, "HDBHDTQGia", "on".equals(lhdsDung_HDBHDTQGia)? "1": "0"));
			elementTmp.appendChild(commons.createElementWithValue(doc, "HDKhac", "on".equals(lhdsDung_HDKhac)? "1": "0"));
			elementTmp.appendChild(commons.createElementWithValue(doc, "CTu", "on".equals(lhdsDung_CTu)? "1": "0"));
			elementSubContent.appendChild(elementTmp);
			/*DANH SACH CHUNG THU SO*/
			elementTmp = doc.createElement("DSCTSSDung");
			if(!jsonData.at("/DSCTSSDung").isMissingNode()) {
				for(JsonNode o: jsonData.at("/DSCTSSDung")) {
					String tn = commons.getTextJsonNode(o.at("/TNgay"));
					String[] words2 = tn.split(" ");
					String[] words = words2[1].split(":");
					String h =  words[0];
					if(h.length() < 2)
					{
						h = "0"+h;
					}
					String m =  words[1];
					if(m.length() < 2)
					{
						m = "0"+m;
					}
					 String s = words[2];
					 if(s.length() < 2)
						{
							s = "0"+s;
						}
					 tn = words2[0]+" "+h+":"+m+":"+s;
					String dn = commons.getTextJsonNode(o.at("/DNgay"));
					String[] words21 = dn.split(" ");
					String[] words1 = words21[1].split(":");
					String h1 =  words1[0];
					if(h1.length() < 2)
					{
						h1 = "0"+h1;
					}
					String m1 =  words1[1];
					if(m1.length() < 2)
					{
						m1 = "0"+m1;
					}
					 String s1 = words1[2];
					 if(s1.length() < 2)
						{
							s1 = "0"+s1;
						}
					 dn = words21[0]+" "+h1+":"+m1+":"+s1;
					
					 DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
					 LocalDateTime dateTime = LocalDateTime.parse(tn, formatter);
					 DateTimeFormatter formatter1 = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
					 LocalDateTime dateTime1 = LocalDateTime.parse(dn, formatter1);
					elementSubTmp = doc.createElement("CTS");
					elementSubTmp.appendChild(commons.createElementWithValue(doc, "TTChuc", commons.getTextJsonNode(o.at("/TTChuc"))));
					elementSubTmp.appendChild(commons.createElementWithValue(doc, "Seri", commons.getTextJsonNode(o.at("/Seri"))));
					elementSubTmp.appendChild(commons.createElementWithValue(doc, "TNgay", commons.convertLocalDateTimeToString(dateTime, Constants.FORMAT_DATE.FORMAT_DATETIME_EINVOICE)));
					elementSubTmp.appendChild(commons.createElementWithValue(doc, "DNgay", commons.convertLocalDateTimeToString(dateTime1, Constants.FORMAT_DATE.FORMAT_DATETIME_EINVOICE)));
				
					elementSubTmp.appendChild(commons.createElementWithValue(doc, "HThuc", commons.getTextJsonNode(o.at("/HThuc"))));
					elementTmp.appendChild(elementSubTmp);
					/*
					 * <TNgay>2021-11-04T00:00:00</TNgay> <DNgay>2022-11-04T23:59:00</DNgay>
					 */
					
					
					/*
					 * "TNgay" : ISODate("2021-11-04T00:00:00.000Z"), "DNgay" :
					 * ISODate("2022-11-04T23:59:00.000Z"),
					 */
					hO = new LinkedHashMap<String, Object>();
					hO.put("TTChuc", commons.getTextJsonNode(o.at("/TTChuc")));
					hO.put("Seri", commons.getTextJsonNode(o.at("/Seri")));
					hO.put("TNgay", dateTime);
					hO.put("DNgay",dateTime1);
					hO.put("HThuc", commons.getTextJsonNode(o.at("/HThuc")));
					rowDSCTSSDung.add(hO);
					
				}
			}
			elementSubContent.appendChild(elementTmp);
			
			elementContent.appendChild(elementSubContent);
			/*END - TAO FILE XML*/
			 isSdaveFile = commons.docW3cToFile(doc, pathDir, fileNameXML);
			if(!isSdaveFile) {
				throw new Exception("Lưu dữ liệu không thành công.");
			}
			
			String MTDiep = SystemParams.MSTTCGP + commons.csRandomAlphaNumbericString(46 - SystemParams.MSTTCGP.length()).toUpperCase();
			/*LUU DU LIEU*/
			docUpsert = new Document("_id", objectIdTK)
				.append("IssuerId", header.getIssuerId())
				.append("MTDiep", MTDiep)
				.append("TenNnt", tenNnt)
				.append("MSo", mauSo)
				.append("Ten", ten)
				.append("HThuc", hThuc)
				.append("MST", mst)
				.append("TinhThanhInfo", docTmp.get("DMTinhThanhInfo"))
				.append("ChiCucThueInfo", docTmp.get("DMChiCucThueInfo"))
				.append("NLHe", nlHe)
				.append("DCLHe", dcLHe)
				.append("DCTDTu", dcCTDTu)
				.append("DTLHe", dtLHe)
				.append("NLap", commons.convertStringToLocalDate(nLap, Constants.FORMAT_DATE.FORMAT_DATE_WEB))				
				.append("HTHDon", htHDon)
				.append("PThuc", pthuc)		
				.append("CMMTTien",  "on".equals(CMMTT)? "1": "0")		
				
				.append("LHDSDung", 
					new Document("HDGTGT", "on".equals(lhdSDung_HDGTGT)? "1": "0")
						.append("HDBHang", "on".equals(lhdsDung_HDBHang)? "1": "0")
						.append("HDBTSCong", "on".equals(lhdsDung_HDBTSCong)? "1": "0")
						.append("HDBHDTQGia", "on".equals(lhdsDung_HDBHDTQGia)? "1": "0")
						.append("HDKhac", "on".equals(lhdsDung_HDKhac)? "1": "0")
						.append("CTu", "on".equals(lhdsDung_CTu)? "1": "0")
				)
				.append("DSCTSSDung", rowDSCTSSDung)
				.append("Status", Constants.INVOICE_STATUS.TK_CREATED)
				.append("IsDelete", false)
				.append("Dir", pathDir)
				.append("FileNameXML", fileNameXML)
				.append("InfoCreated", 
					new Document("CreateDate", LocalDateTime.now())
					.append("CreateUserID", header.getUserId())
					.append("CreateUserName", header.getUserName())
					.append("CreateUserFullName", header.getUserFullName())
				);
			/*END - LUU DU LIEU*/
		
			 mongoClient = cfg.mongoClient();
				collection = mongoClient.getDatabase(cfg.dbName).getCollection("DMTKhai");
				collection.insertOne(docUpsert);
				mongoClient.close();
			
			
			DateTimeFormatter format_time = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
			LocalDateTime time_dem  = LocalDateTime.now();
			String time = time_dem.format(format_time);
				String name_company = removeAccent(header.getUserFullName());
				System.out.println(time +name_company+" Vua tao to khai");
			responseStatus = new MspResponseStatus(0, "SUCCESS");
			rsp.setResponseStatus(responseStatus);				
			return rsp;
		case Constants.MSG_ACTION_CODE.COPY:
			objectId = null;
			objectIdUser = null;
			try {
				objectId = new ObjectId(header.getIssuerId());
			}catch(Exception e) {}
			try {
				objectIdUser = new ObjectId(header.getUserId());
			}catch(Exception e) {}
			
			/*KIEM TRA THONG TIN KHACH HANG - USER - TINH THANH - CO QUAN THUE CO TON TAI KHONG*/
			docFind = new Document("_id", objectId).append("IsActive", true).append("IsDelete", new Document("$ne", true));
			pipeline = new ArrayList<Document>();
			pipeline.add(new Document("$match", docFind));
			pipeline.add(
				new Document("$lookup", 
					new Document("from", "Users")
					.append("pipeline", 
						Arrays.asList(
							new Document("$match", 
								new Document("IssuerId", header.getIssuerId()).append("_id", objectIdUser)
								.append("IsActive", true).append("IsDelete", new Document("$ne", true))
							),
							new Document("$project", new Document("_id", 1).append("UserName", 1).append("FullName", 1)),
							new Document("$limit", 1)
						)
					)
					.append("as", "UserInfo")
				)
			);
			pipeline.add(new Document("$unwind", new Document("path", "$UserInfo").append("preserveNullAndEmptyArrays", true)));
			pipeline.add(
				new Document("$lookup", 
					new Document("from", "DMTinhThanh")
					.append("pipeline", 
						Arrays.asList(
							new Document("$match", new Document("IsDelete", new Document("$ne", true)).append("code", commons.regexEscapeForMongoQuery(tinhThanh))),
							new Document("$project", new Document("_id", 0).append("code", 1).append("name", 1))
						)
					)
					.append("as", "DMTinhThanhInfo")
				)
			);
			pipeline.add(new Document("$unwind", new Document("path", "$DMTinhThanhInfo").append("preserveNullAndEmptyArrays", true)));
			pipeline.add(
				new Document("$lookup", 
					new Document("from", "DMChiCucThue")
					.append("pipeline", 
						Arrays.asList(
							new Document("$match", new Document("IsDelete", new Document("$ne", true)).append("code", commons.regexEscapeForMongoQuery(cqtQLy))),
							new Document("$project", new Document("_id", 0).append("code", 1).append("name", 1))
						)
					)
					.append("as", "DMChiCucThueInfo")
				)
			);
			pipeline.add(new Document("$unwind", new Document("path", "$DMChiCucThueInfo").append("preserveNullAndEmptyArrays", true)));
				
		
			
			mongoClient = cfg.mongoClient();
			collection = mongoClient.getDatabase(cfg.dbName).getCollection("Issuer");

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
			if(docTmp.get("UserInfo") == null) {
				responseStatus = new MspResponseStatus(9999, "Không tìm thấy thông tin người dùng.");
				rsp.setResponseStatus(responseStatus);
				return rsp;
			}
			
			if(docTmp.get("DMTinhThanhInfo") == null || docTmp.get("DMChiCucThueInfo") == null) {
				responseStatus = new MspResponseStatus(9999, "Vui lòng kiểm tra lại tỉnh/thành phố và cơ quan thuế.");
				rsp.setResponseStatus(responseStatus);
				return rsp;
			}
			
			taxCode = docTmp.getString("TaxCode");
			
			/*TAO FILE XML*/
			objectIdTK = new ObjectId();
			objectIdDMCTS = new ObjectId();
			path = Paths.get(SystemParams.DIR_E_INVOICE_TKHAI, taxCode);
			pathDir = path.toString();
			file = path.toFile();
			if(!file.exists()) file.mkdirs();
			
			/*TAO FILE XML*/
			fileNameXML = objectIdTK.toString() + ".xml";
			
			 dbf = DocumentBuilderFactory.newInstance();
			 db = dbf.newDocumentBuilder();
			 doc = db.newDocument();
			doc.setXmlStandalone(true);
			
			root = doc.createElement("TKhai");
			doc.appendChild(root);
			
			 elementTmp = null;
			 elementSubTmp = null;
			 elementContent = doc.createElement("DLTKhai");
			elementContent.setAttribute("Id", "data");
			root.appendChild(elementContent);
			
			/*THONG TIN CHUNG TO KHAI*/
			elementSubContent = doc.createElement("TTChung");
			elementSubContent.appendChild(commons.createElementWithValue(doc, "PBan",  SystemParams.VERSION_XML_TOKHAI));
			elementSubContent.appendChild(commons.createElementWithValue(doc, "MSo", mauSo));
			elementSubContent.appendChild(commons.createElementWithValue(doc, "Ten", ten));
			elementSubContent.appendChild(commons.createElementWithValue(doc, "HThuc", hThuc));
			elementSubContent.appendChild(commons.createElementWithValue(doc, "TNNT", tenNnt));
			elementSubContent.appendChild(commons.createElementWithValue(doc, "MST", mst));
			elementSubContent.appendChild(commons.createElementWithValue(doc, "CQTQLy", docTmp.getEmbedded(Arrays.asList("DMChiCucThueInfo", "name"), "")));
			elementSubContent.appendChild(commons.createElementWithValue(doc, "MCQTQLy", cqtQLy));
			elementSubContent.appendChild(commons.createElementWithValue(doc, "NLHe", nlHe));
			elementSubContent.appendChild(commons.createElementWithValue(doc, "DCLHe", dcLHe));
			elementSubContent.appendChild(commons.createElementWithValue(doc, "DCTDTu", dcCTDTu));
			elementSubContent.appendChild(commons.createElementWithValue(doc, "DTLHe", dtLHe));
			elementSubContent.appendChild(commons.createElementWithValue(doc, "DDanh", docTmp.getEmbedded(Arrays.asList("DMTinhThanhInfo", "name"), "")));
			elementSubContent.appendChild(commons.createElementWithValue(doc, "NLap", commons.convertLocalDateTimeStringToString(nLap, Constants.FORMAT_DATE.FORMAT_DATE_WEB, Constants.FORMAT_DATE.FORMAT_DATE_EINVOICE)));
			elementContent.appendChild(elementSubContent);
			
			/*NOI DUNG CHI TIET TO KHAI*/
			elementSubContent = doc.createElement("NDTKhai");
			/*HINH THUC HOA DON AP DUNG*/
			elementTmp = doc.createElement("HTHDon");
			elementTmp.appendChild(commons.createElementWithValue(doc, "CMa", "CMa".equals(htHDon)? "1": "0"));
			if(CMMTT.equals("on")) {
				elementTmp.appendChild(commons.createElementWithValue(doc, "CMTMTTien", "on".equals(CMMTT)? "1": "0"));
			}
			elementTmp.appendChild(commons.createElementWithValue(doc, "KCMa", "KCMa".equals(htHDon)? "1": "0"));
			elementSubContent.appendChild(elementTmp);
			/*HINH THUC GUI DU LIEU HDDT - FIX GIA TRI*/
			elementTmp = doc.createElement("HTGDLHDDT");
			elementTmp.appendChild(commons.createElementWithValue(doc, "NNTDBKKhan", "0"));
			elementTmp.appendChild(commons.createElementWithValue(doc, "NNTKTDNUBND", "0"));
			elementTmp.appendChild(commons.createElementWithValue(doc, "CDLTTDCQT", "0"));
			elementTmp.appendChild(commons.createElementWithValue(doc, "CDLQTCTN", "0"));
			elementSubContent.appendChild(elementTmp);
			/*PHUONG THUC CHUYEN DU LIEU*/
			elementTmp = doc.createElement("PThuc");
			elementTmp.appendChild(commons.createElementWithValue(doc, "CDDu", "CDDu".equals(pthuc)? "1": "0"));
			elementTmp.appendChild(commons.createElementWithValue(doc, "CBTHop", "CBTHop".equals(pthuc)? "1": "0"));
			elementSubContent.appendChild(elementTmp);
			/*LOAI HD SU DUNG*/
			elementTmp = doc.createElement("LHDSDung");
			elementTmp.appendChild(commons.createElementWithValue(doc, "HDGTGT", "on".equals(lhdSDung_HDGTGT)? "1": "0"));
			elementTmp.appendChild(commons.createElementWithValue(doc, "HDBHang", "on".equals(lhdsDung_HDBHang)? "1": "0"));
			elementTmp.appendChild(commons.createElementWithValue(doc, "HDBTSCong", "on".equals(lhdsDung_HDBTSCong)? "1": "0"));
			elementTmp.appendChild(commons.createElementWithValue(doc, "HDBHDTQGia", "on".equals(lhdsDung_HDBHDTQGia)? "1": "0"));
			elementTmp.appendChild(commons.createElementWithValue(doc, "HDKhac", "on".equals(lhdsDung_HDKhac)? "1": "0"));
			elementTmp.appendChild(commons.createElementWithValue(doc, "CTu", "on".equals(lhdsDung_CTu)? "1": "0"));
			elementSubContent.appendChild(elementTmp);
			/*DANH SACH CHUNG THU SO*/
			elementTmp = doc.createElement("DSCTSSDung");
			if(!jsonData.at("/DSCTSSDung").isMissingNode()) {
				for(JsonNode o: jsonData.at("/DSCTSSDung")) {
					String tn = commons.getTextJsonNode(o.at("/TNgay"));
					String[] words2 = tn.split(" ");
					String[] words = words2[1].split(":");
					String h =  words[0];
					if(h.length() < 2)
					{
						h = "0"+h;
					}
					String m =  words[1];
					if(m.length() < 2)
					{
						m = "0"+m;
					}
					 String s = words[2];
					 if(s.length() < 2)
						{
							s = "0"+s;
						}
					 tn = words2[0]+" "+h+":"+m+":"+s;
					String dn = commons.getTextJsonNode(o.at("/DNgay"));
					String[] words21 = dn.split(" ");
					String[] words1 = words21[1].split(":");
					String h1 =  words1[0];
					if(h1.length() < 2)
					{
						h1 = "0"+h1;
					}
					String m1 =  words1[1];
					if(m1.length() < 2)
					{
						m1 = "0"+m1;
					}
					 String s1 = words1[2];
					 if(s1.length() < 2)
						{
							s1 = "0"+s1;
						}
					 dn = words21[0]+" "+h1+":"+m1+":"+s1;
					
					 DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
					 LocalDateTime dateTime = LocalDateTime.parse(tn, formatter);
					 DateTimeFormatter formatter1 = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
					 LocalDateTime dateTime1 = LocalDateTime.parse(dn, formatter1);
					elementSubTmp = doc.createElement("CTS");
					elementSubTmp.appendChild(commons.createElementWithValue(doc, "TTChuc", commons.getTextJsonNode(o.at("/TTChuc"))));
					elementSubTmp.appendChild(commons.createElementWithValue(doc, "Seri", commons.getTextJsonNode(o.at("/Seri"))));
					elementSubTmp.appendChild(commons.createElementWithValue(doc, "TNgay", commons.convertLocalDateTimeToString(dateTime, Constants.FORMAT_DATE.FORMAT_DATETIME_EINVOICE)));
					elementSubTmp.appendChild(commons.createElementWithValue(doc, "DNgay", commons.convertLocalDateTimeToString(dateTime1, Constants.FORMAT_DATE.FORMAT_DATETIME_EINVOICE)));
				
					elementSubTmp.appendChild(commons.createElementWithValue(doc, "HThuc", commons.getTextJsonNode(o.at("/HThuc"))));
					elementTmp.appendChild(elementSubTmp);
					/*
					 * <TNgay>2021-11-04T00:00:00</TNgay> <DNgay>2022-11-04T23:59:00</DNgay>
					 */
					
					
					/*
					 * "TNgay" : ISODate("2021-11-04T00:00:00.000Z"), "DNgay" :
					 * ISODate("2022-11-04T23:59:00.000Z"),
					 */
					hO = new LinkedHashMap<String, Object>();
					hO.put("TTChuc", commons.getTextJsonNode(o.at("/TTChuc")));
					hO.put("Seri", commons.getTextJsonNode(o.at("/Seri")));
					hO.put("TNgay", dateTime);
					hO.put("DNgay",dateTime1);
					hO.put("HThuc", commons.getTextJsonNode(o.at("/HThuc")));
					rowDSCTSSDung.add(hO);
					
				}
			}
			elementSubContent.appendChild(elementTmp);
			
			elementContent.appendChild(elementSubContent);
			/*END - TAO FILE XML*/
			 isSdaveFile = commons.docW3cToFile(doc, pathDir, fileNameXML);
			if(!isSdaveFile) {
				throw new Exception("Lưu dữ liệu không thành công.");
			}
			
			MTDiep = SystemParams.MSTTCGP + commons.csRandomAlphaNumbericString(46 - SystemParams.MSTTCGP.length()).toUpperCase();
			/*LUU DU LIEU*/
			docUpsert = new Document("_id", objectIdTK)
				.append("IssuerId", header.getIssuerId())
				.append("MTDiep", MTDiep)
				.append("TenNnt", tenNnt)
				.append("MSo", mauSo)
				.append("Ten", ten)
				.append("HThuc", hThuc)
				.append("MST", mst)
				.append("TinhThanhInfo", docTmp.get("DMTinhThanhInfo"))
				.append("ChiCucThueInfo", docTmp.get("DMChiCucThueInfo"))
				.append("NLHe", nlHe)
				.append("DCLHe", dcLHe)
				.append("DCTDTu", dcCTDTu)
				.append("DTLHe", dtLHe)
				.append("NLap", commons.convertStringToLocalDate(nLap, Constants.FORMAT_DATE.FORMAT_DATE_WEB))				
				.append("HTHDon", htHDon)
				.append("PThuc", pthuc)		
				.append("CMMTTien",  "on".equals(CMMTT)? "1": "0")		
				
				.append("LHDSDung", 
					new Document("HDGTGT", "on".equals(lhdSDung_HDGTGT)? "1": "0")
						.append("HDBHang", "on".equals(lhdsDung_HDBHang)? "1": "0")
						.append("HDBTSCong", "on".equals(lhdsDung_HDBTSCong)? "1": "0")
						.append("HDBHDTQGia", "on".equals(lhdsDung_HDBHDTQGia)? "1": "0")
						.append("HDKhac", "on".equals(lhdsDung_HDKhac)? "1": "0")
						.append("CTu", "on".equals(lhdsDung_CTu)? "1": "0")
				)
				.append("DSCTSSDung", rowDSCTSSDung)
				.append("Status", Constants.INVOICE_STATUS.TK_CREATED)
				.append("IsDelete", false)
				.append("Dir", pathDir)
				.append("FileNameXML", fileNameXML)
				.append("InfoCreated", 
					new Document("CreateDate", LocalDateTime.now())
					.append("CreateUserID", header.getUserId())
					.append("CreateUserName", header.getUserName())
					.append("CreateUserFullName", header.getUserFullName())
				);
			/*END - LUU DU LIEU*/
		
			 mongoClient = cfg.mongoClient();
				collection = mongoClient.getDatabase(cfg.dbName).getCollection("DMTKhai");
				collection.insertOne(docUpsert);
				mongoClient.close();
			
			
			format_time = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
			time_dem  = LocalDateTime.now();
			time = time_dem.format(format_time);
			name_company = removeAccent(header.getUserFullName());
			System.out.println(time +name_company+" Vua copy to khai");
			responseStatus = new MspResponseStatus(0, "SUCCESS");
			rsp.setResponseStatus(responseStatus);				
			return rsp;
		case Constants.MSG_ACTION_CODE.MODIFY:
			objectId = null;
			objectIdUser = null;
			try {
				objectId = new ObjectId(header.getIssuerId());
			}catch(Exception e) {}
			try {
				objectIdUser = new ObjectId(header.getUserId());
			}catch(Exception e) {}
			try {
				objectIdTK = new ObjectId(_id);
			}catch(Exception e) {}
			
			/*KIEM TRA THONG TIN KHACH HANG - USER - TINH THANH - CO QUAN THUE CO TON TAI KHONG*/
			docFind = new Document("_id", objectId).append("IsActive", true).append("IsDelete", new Document("$ne", true));
			pipeline = new ArrayList<Document>();
			pipeline.add(new Document("$match", docFind));
			pipeline.add(
				new Document("$lookup", 
					new Document("from", "Users")
					.append("pipeline", 
						Arrays.asList(
							new Document("$match", 
								new Document("IssuerId", header.getIssuerId()).append("_id", objectIdUser)
								.append("IsActive", true).append("IsDelete", new Document("$ne", true))
							),
							new Document("$project", new Document("_id", 1).append("UserName", 1).append("FullName", 1)),
							new Document("$limit", 1)
						)
					)
					.append("as", "UserInfo")
				)
			);
			pipeline.add(new Document("$unwind", new Document("path", "$UserInfo").append("preserveNullAndEmptyArrays", true)));
			pipeline.add(
				new Document("$lookup", 
					new Document("from", "DMTinhThanh")
					.append("pipeline", 
						Arrays.asList(
							new Document("$match", new Document("IsDelete", new Document("$ne", true)).append("code", commons.regexEscapeForMongoQuery(tinhThanh))),
							new Document("$project", new Document("_id", 0).append("code", 1).append("name", 1))
						)
					)
					.append("as", "DMTinhThanhInfo")
				)
			);
			pipeline.add(new Document("$unwind", new Document("path", "$DMTinhThanhInfo").append("preserveNullAndEmptyArrays", true)));
			pipeline.add(
				new Document("$lookup", 
					new Document("from", "DMChiCucThue")
					.append("pipeline", 
						Arrays.asList(
							new Document("$match", new Document("IsDelete", new Document("$ne", true)).append("code", commons.regexEscapeForMongoQuery(cqtQLy))),
							new Document("$project", new Document("_id", 0).append("code", 1).append("name", 1))
						)
					)
					.append("as", "DMChiCucThueInfo")
				)
			);
			pipeline.add(new Document("$unwind", new Document("path", "$DMChiCucThueInfo").append("preserveNullAndEmptyArrays", true)));
				
			docFind = new Document("IssuerId", header.getIssuerId()).append("IsDelete", new Document("$ne", true))
					.append("Status", Constants.INVOICE_STATUS.CREATED)		
					.append("_id", objectIdTK);
			pipeline.add(
				new Document("$lookup", 
					new Document("from", "DMTKhai")
					.append("pipeline", 
						Arrays.asList(
							new Document("$match", docFind)
						)
					)
					.append("as", "DMTKhai")
				)
			);
			
			pipeline.add(new Document("$unwind", new Document("path", "$DMTKhai").append("preserveNullAndEmptyArrays", true)));
			

		
			
			 mongoClient = cfg.mongoClient();
			 collection = mongoClient.getDatabase(cfg.dbName).getCollection("Issuer");

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
			if(docTmp.get("UserInfo") == null) {
				responseStatus = new MspResponseStatus(9999, "Không tìm thấy thông tin người dùng.");
				rsp.setResponseStatus(responseStatus);
				return rsp;
			}
			
			if(docTmp.get("DMTinhThanhInfo") == null || docTmp.get("DMChiCucThueInfo") == null) {
				responseStatus = new MspResponseStatus(9999, "Vui lòng kiểm tra lại tỉnh/thành phố và cơ quan thuế.");
				rsp.setResponseStatus(responseStatus);
				return rsp;
			}
			if(docTmp.get("DMTKhai") == null) {
				responseStatus = new MspResponseStatus(9999, "Vui lòng kiểm tra lại thông tin tờ khai 01.");
				rsp.setResponseStatus(responseStatus);
				return rsp;
			}


		
			taxCode = docTmp.getString("TaxCode");
			
			/*TAO FILE XML*/
			objectIdTK = new ObjectId();
			path = Paths.get(SystemParams.DIR_E_INVOICE_TKHAI, taxCode);
			pathDir = docTmp.getEmbedded(Arrays.asList("DMTKhai", "Dir"), "");
			fileNameXML =docTmp.getEmbedded(Arrays.asList("DMTKhai", "FileNameXML"), "");
			file = new File(pathDir);
			if(!file.exists()) file.mkdirs();
			
			/*TAO FILE XML*/
			
			
			 dbf = DocumentBuilderFactory.newInstance();
			 db = dbf.newDocumentBuilder();
			 doc = db.newDocument();
			doc.setXmlStandalone(true);
			
			 root = doc.createElement("TKhai");
			doc.appendChild(root);
			
			 elementTmp = null;
			 elementSubTmp = null;
			 elementContent = doc.createElement("DLTKhai");
			elementContent.setAttribute("Id", "data");
			root.appendChild(elementContent);
			
			/*THONG TIN CHUNG TO KHAI*/
			elementSubContent = doc.createElement("TTChung");
			elementSubContent.appendChild(commons.createElementWithValue(doc, "PBan", SystemParams.VERSION_XML_TOKHAI));
			elementSubContent.appendChild(commons.createElementWithValue(doc, "MSo", mauSo));
			elementSubContent.appendChild(commons.createElementWithValue(doc, "Ten", ten));
			elementSubContent.appendChild(commons.createElementWithValue(doc, "HThuc", hThuc));
			elementSubContent.appendChild(commons.createElementWithValue(doc, "TNNT", tenNnt));
			elementSubContent.appendChild(commons.createElementWithValue(doc, "MST", mst));
			elementSubContent.appendChild(commons.createElementWithValue(doc, "CQTQLy", docTmp.getEmbedded(Arrays.asList("DMChiCucThueInfo", "name"), "")));
			elementSubContent.appendChild(commons.createElementWithValue(doc, "MCQTQLy", cqtQLy));
			elementSubContent.appendChild(commons.createElementWithValue(doc, "NLHe", nlHe));
			elementSubContent.appendChild(commons.createElementWithValue(doc, "DCLHe", dcLHe));
			elementSubContent.appendChild(commons.createElementWithValue(doc, "DCTDTu", dcCTDTu));
			elementSubContent.appendChild(commons.createElementWithValue(doc, "DTLHe", dtLHe));
			elementSubContent.appendChild(commons.createElementWithValue(doc, "DDanh", docTmp.getEmbedded(Arrays.asList("DMTinhThanhInfo", "name"), "")));
			elementSubContent.appendChild(commons.createElementWithValue(doc, "NLap", commons.convertLocalDateTimeStringToString(nLap, Constants.FORMAT_DATE.FORMAT_DATE_WEB, Constants.FORMAT_DATE.FORMAT_DATE_EINVOICE)));
			elementContent.appendChild(elementSubContent);
			
			/*NOI DUNG CHI TIET TO KHAI*/
			elementSubContent = doc.createElement("NDTKhai");
			/*HINH THUC HOA DON AP DUNG*/
			elementTmp = doc.createElement("HTHDon");
			elementTmp.appendChild(commons.createElementWithValue(doc, "CMa", "CMa".equals(htHDon)? "1": "0"));
			if(CMMTT.equals("on")) {
				elementTmp.appendChild(commons.createElementWithValue(doc, "CMTMTTien", "on".equals(CMMTT)? "1": "0"));
			}
		
			elementTmp.appendChild(commons.createElementWithValue(doc, "KCMa", "KCMa".equals(htHDon)? "1": "0"));
			elementSubContent.appendChild(elementTmp);
			/*HINH THUC GUI DU LIEU HDDT - FIX GIA TRI*/
			elementTmp = doc.createElement("HTGDLHDDT");
			elementTmp.appendChild(commons.createElementWithValue(doc, "NNTDBKKhan", "0"));
			elementTmp.appendChild(commons.createElementWithValue(doc, "NNTKTDNUBND", "0"));
			elementTmp.appendChild(commons.createElementWithValue(doc, "CDLTTDCQT", "0"));
			elementTmp.appendChild(commons.createElementWithValue(doc, "CDLQTCTN", "0"));
			elementSubContent.appendChild(elementTmp);
			/*PHUONG THUC CHUYEN DU LIEU*/
			elementTmp = doc.createElement("PThuc");
			elementTmp.appendChild(commons.createElementWithValue(doc, "CDDu", "CDDu".equals(pthuc)? "1": "0"));
			elementTmp.appendChild(commons.createElementWithValue(doc, "CBTHop", "CBTHop".equals(pthuc)? "1": "0"));
			elementSubContent.appendChild(elementTmp);
			/*LOAI HD SU DUNG*/
			elementTmp = doc.createElement("LHDSDung");
			elementTmp.appendChild(commons.createElementWithValue(doc, "HDGTGT", "on".equals(lhdSDung_HDGTGT)? "1": "0"));
			elementTmp.appendChild(commons.createElementWithValue(doc, "HDBHang", "on".equals(lhdsDung_HDBHang)? "1": "0"));
			elementTmp.appendChild(commons.createElementWithValue(doc, "HDBTSCong", "on".equals(lhdsDung_HDBTSCong)? "1": "0"));
			elementTmp.appendChild(commons.createElementWithValue(doc, "HDBHDTQGia", "on".equals(lhdsDung_HDBHDTQGia)? "1": "0"));
			elementTmp.appendChild(commons.createElementWithValue(doc, "HDKhac", "on".equals(lhdsDung_HDKhac)? "1": "0"));
			elementTmp.appendChild(commons.createElementWithValue(doc, "CTu", "on".equals(lhdsDung_CTu)? "1": "0"));
			elementSubContent.appendChild(elementTmp);
			/*DANH SACH CHUNG THU SO*/
			elementTmp = doc.createElement("DSCTSSDung");
			if(!jsonData.at("/DSCTSSDung").isMissingNode()) {
				for(JsonNode o: jsonData.at("/DSCTSSDung")) {
					String tn = commons.getTextJsonNode(o.at("/TNgay"));
					String[] words2 = tn.split(" ");
					String[] words = words2[1].split(":");
					String h =  words[0];
					if(h.length() < 2)
					{
						h = "0"+h;
					}
					String m =  words[1];
					if(m.length() < 2)
					{
						m = "0"+m;
					}
					 String s = words[2];
					 if(s.length() < 2)
						{
							s = "0"+s;
						}
					 tn = words2[0]+" "+h+":"+m+":"+s;
					String dn = commons.getTextJsonNode(o.at("/DNgay"));
					String[] words21 = dn.split(" ");
					String[] words1 = words21[1].split(":");
					String h1 =  words1[0];
					if(h1.length() < 2)
					{
						h1 = "0"+h1;
					}
					String m1 =  words1[1];
					if(m1.length() < 2)
					{
						m1 = "0"+m1;
					}
					 String s1 = words1[2];
					 if(s1.length() < 2)
						{
							s1 = "0"+s1;
						}
					 dn = words21[0]+" "+h1+":"+m1+":"+s1;
					
					 DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
					 LocalDateTime dateTime = LocalDateTime.parse(tn, formatter);
					 DateTimeFormatter formatter1 = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
					 LocalDateTime dateTime1 = LocalDateTime.parse(dn, formatter1);
					elementSubTmp = doc.createElement("CTS");
					elementSubTmp.appendChild(commons.createElementWithValue(doc, "TTChuc", commons.getTextJsonNode(o.at("/TTChuc"))));
					elementSubTmp.appendChild(commons.createElementWithValue(doc, "Seri", commons.getTextJsonNode(o.at("/Seri"))));
					elementSubTmp.appendChild(commons.createElementWithValue(doc, "TNgay", commons.convertLocalDateTimeToString(dateTime, Constants.FORMAT_DATE.FORMAT_DATETIME_EINVOICE)));
					elementSubTmp.appendChild(commons.createElementWithValue(doc, "DNgay", commons.convertLocalDateTimeToString(dateTime1, Constants.FORMAT_DATE.FORMAT_DATETIME_EINVOICE)));
				
					elementSubTmp.appendChild(commons.createElementWithValue(doc, "HThuc", commons.getTextJsonNode(o.at("/HThuc"))));
					elementTmp.appendChild(elementSubTmp);
					/*
					 * <TNgay>2021-11-04T00:00:00</TNgay> <DNgay>2022-11-04T23:59:00</DNgay>
					 */
					
					
					/*
					 * "TNgay" : ISODate("2021-11-04T00:00:00.000Z"), "DNgay" :
					 * ISODate("2022-11-04T23:59:00.000Z"),
					 */
					hO = new LinkedHashMap<String, Object>();
					hO.put("TTChuc", commons.getTextJsonNode(o.at("/TTChuc")));
					hO.put("Seri", commons.getTextJsonNode(o.at("/Seri")));
					hO.put("TNgay", dateTime);
					hO.put("DNgay",dateTime1);
					hO.put("HThuc", commons.getTextJsonNode(o.at("/HThuc")));
					rowDSCTSSDung.add(hO);
					
				}
			}
			elementSubContent.appendChild(elementTmp);
			
			elementContent.appendChild(elementSubContent);
			/*END - TAO FILE XML*/
			isSdaveFile = commons.docW3cToFile(doc, pathDir, fileNameXML);
			if(!isSdaveFile) {
				throw new Exception("Lưu dữ liệu không thành công.");
			}
			
			MTDiep = SystemParams.MSTTCGP + commons.csRandomAlphaNumbericString(46 - SystemParams.MSTTCGP.length()).toUpperCase();
			/*LUU DU LIEU*/
			docUpsert = new Document("IssuerId", header.getIssuerId())
				.append("MTDiep", MTDiep)
				.append("TenNnt", tenNnt)
				.append("MSo", mauSo)
				.append("Ten", ten)
				.append("HThuc", hThuc)
				.append("MST", mst)
				.append("TinhThanhInfo", docTmp.get("DMTinhThanhInfo"))
				.append("ChiCucThueInfo", docTmp.get("DMChiCucThueInfo"))
				.append("NLHe", nlHe)
				.append("DCLHe", dcLHe)
				.append("DCTDTu", dcCTDTu)
				.append("DTLHe", dtLHe)
				.append("NLap", commons.convertStringToLocalDate(nLap, Constants.FORMAT_DATE.FORMAT_DATE_WEB))				
				.append("HTHDon", htHDon)
				.append("PThuc", pthuc)	
			
				.append("LHDSDung", 
					new Document("HDGTGT", "on".equals(lhdSDung_HDGTGT)? "1": "0")
						.append("HDBHang", "on".equals(lhdsDung_HDBHang)? "1": "0")
						.append("HDBTSCong", "on".equals(lhdsDung_HDBTSCong)? "1": "0")
						.append("HDBHDTQGia", "on".equals(lhdsDung_HDBHDTQGia)? "1": "0")
						.append("HDKhac", "on".equals(lhdsDung_HDKhac)? "1": "0")
						.append("CTu", "on".equals(lhdsDung_CTu)? "1": "0")
				)
				.append("DSCTSSDung", rowDSCTSSDung)
				.append("Status", Constants.INVOICE_STATUS.TK_CREATED)
				.append("IsDelete", false)
				.append("Dir", pathDir)
				.append("FileNameXML", fileNameXML)				
				;
			
			
			///
			String TKhai_id = docTmp.getEmbedded(Arrays.asList("DMCTSo", "TKhaiId"), "");
			docUpsert1 = new Document("TKhaiId", TKhai_id)					
					.append("IssuerId", header.getIssuerId())				
					.append("TenNnt", tenNnt)
					.append("MST", mst)									
					.append("NDKy", commons.convertStringToLocalDate(nLap, Constants.FORMAT_DATE.FORMAT_DATE_WEB))									
					.append("DSCTSSDung", rowDSCTSSDung)				
					.append("IsDelete", false)				
					.append("InfoCreated", 
						new Document("CreateDate", LocalDateTime.now())
						.append("CreateUserID", header.getUserId())
						.append("CreateUserName", header.getUserName())
						.append("CreateUserFullName", header.getUserFullName())
					);
			
			
			/*END - LUU DU LIEU*/
			
			
			
			options = new FindOneAndUpdateOptions();
			options.upsert(false);
			options.maxTime(5000, TimeUnit.MILLISECONDS);
			options.returnDocument(ReturnDocument.AFTER);
			

			 mongoClient = cfg.mongoClient();
				collection = mongoClient.getDatabase(cfg.dbName).getCollection("DMTKhai");
				docR =		collection.findOneAndUpdate(
						docFind, 
						new Document("$set", 
								new Document("MTDiep", MTDiep)
								.append("TenNnt", tenNnt)
								.append("MSo", mauSo)
								.append("Ten", ten)
								.append("HThuc", hThuc)
								.append("MST", mst)
								.append("TinhThanhInfo", docTmp.get("DMTinhThanhInfo"))
								.append("ChiCucThueInfo", docTmp.get("DMChiCucThueInfo"))
								.append("NLHe", nlHe)
								.append("DCLHe", dcLHe)
								.append("DCTDTu", dcCTDTu)
								.append("DTLHe", dtLHe)
								.append("NLap", commons.convertStringToLocalDate(nLap, Constants.FORMAT_DATE.FORMAT_DATE_WEB))				
								.append("HTHDon", htHDon)
								.append("CMMTTien",  "on".equals(CMMTT)? "1": "0")		
								.append("PThuc", pthuc)				
								.append("LHDSDung", 
									new Document("HDGTGT", "on".equals(lhdSDung_HDGTGT)? "1": "0")
										.append("HDBHang", "on".equals(lhdsDung_HDBHang)? "1": "0")
										.append("HDBTSCong", "on".equals(lhdsDung_HDBTSCong)? "1": "0")
										.append("HDBHDTQGia", "on".equals(lhdsDung_HDBHDTQGia)? "1": "0")
										.append("HDKhac", "on".equals(lhdsDung_HDKhac)? "1": "0")
										.append("CTu", "on".equals(lhdsDung_CTu)? "1": "0")
								)
								.append("DSCTSSDung", rowDSCTSSDung)
								.append("Status", Constants.INVOICE_STATUS.TK_CREATED)
								.append("IsDelete", false)
								.append("Dir", pathDir)
								.append("FileNameXML", fileNameXML)									
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
			System.out.println(time +name_company+" Vừa thay doi to khai");
			responseStatus = new MspResponseStatus(0, "SUCCESS");
			rsp.setResponseStatus(responseStatus);				
			return rsp;	
			
		case Constants.MSG_ACTION_CODE.DELETE:
			objectId = null;
			try {
				objectId = new ObjectId(_id);
			}catch(Exception e) {}
			
			docFind = new Document("IssuerId", header.getIssuerId())
					.append("_id", objectId).append("IsDelete", new Document("$ne", true))
					.append("Status", "CREATED");
			
		
			docTmp = null;
			docTmp1 = null;
	
	
			 mongoClient = cfg.mongoClient();
			 collection = mongoClient.getDatabase(cfg.dbName).getCollection("DMTKhai");

			try {
				docTmp = collection.find(docFind).allowDiskUse(true).iterator().next();
			} catch (Exception e) {

			}
			mongoClient.close();

			if(null == docTmp) {
				responseStatus = new MspResponseStatus(9999, "Không tìm thấy thông tin tờ khai.");
				rsp.setResponseStatus(responseStatus);
				return rsp;
			}

			options = new FindOneAndUpdateOptions();
			options.upsert(false);
			options.maxTime(5000, TimeUnit.MILLISECONDS);
			options.returnDocument(ReturnDocument.AFTER);
		
			 mongoClient = cfg.mongoClient();
				collection = mongoClient.getDatabase(cfg.dbName).getCollection("DMTKhai");
				collection.findOneAndUpdate(
						docFind
						, new Document("$set", 
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
			
			format_time = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");			
			time_dem  = LocalDateTime.now();			
			time = time_dem.format(format_time);
			
			name_company = removeAccent(header.getUserFullName());
			System.out.println(time+name_company+" Vua xoa to khai");
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
	public MsgRsp list(JSONRoot jsonRoot) throws Exception {
		Msg msg = jsonRoot.getMsg();
		MsgHeader header = msg.getMsgHeader();

		
		MsgRsp rsp = new MsgRsp(header);
		MspResponseStatus responseStatus = null;
		
		Iterator<Document> iter = null;
		List<Document> pipeline = new ArrayList<Document>();
		
		List<Document> rows = new ArrayList<Document>();
		
		Document docMatch = new Document("IssuerId", header.getIssuerId()).append("IsDelete", new Document("$ne", true));
		
		Document fillter = new Document("_id", 1).append("MST", 1).append("TenNnt", 1)
				.append("MTDiep", 1).append("MSo", 1).append("Status", 1).append("ChiCucThueInfo", 1).append("StatusCQT", 1)
				.append("LDo", 1).append("Ten", 1).append("NLap", 1);
		
		pipeline = new ArrayList<Document>();
		pipeline.add(new Document("$match", docMatch));
		pipeline.add(new Document("$sort", new Document("_id", -1)));
		pipeline.add(new Document("$project", fillter));

		pipeline.add(
			new Document("$set", 
				new Document("_id", new Document("$toString", "$_id"))
			)
		);
		
	
	
		MongoClient mongoClient = cfg.mongoClient();
		MongoCollection<Document> collection = mongoClient.getDatabase(cfg.dbName).getCollection("DMTKhai");
		iter =  collection.aggregate(pipeline).allowDiskUse(true).iterator();
		mongoClient.close();
		pipeline.clear();
		while(iter.hasNext()) {
			rows.add(iter.next());
		}
	
		
		
		rsp = new MsgRsp(header);
		responseStatus = null;
		
		responseStatus = new MspResponseStatus(0, "SUCCESS");
		rsp.setResponseStatus(responseStatus);
		rsp.setObjData(rows);
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
		
		Document docTmp = null;
	
		
		MongoClient mongoClient = cfg.mongoClient();
		MongoCollection<Document> collection = mongoClient.getDatabase(cfg.dbName).getCollection("DMTKhai");

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
	public FileInfo getFileForSign(JSONRoot jsonRoot) throws Exception {
		FileInfo fileInfo = new FileInfo();
		
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
		
		ObjectId objectId = null;
		try {
			objectId = new ObjectId(_id);
		}catch(Exception e) {}
		
		Document docFind = new Document("IssuerId", header.getIssuerId())
				.append("_id", objectId).append("IsDelete", new Document("$ne", true))
				.append("Status", "CREATED");
		
		Document docTmp = null;
		
	
		
		MongoClient mongoClient = cfg.mongoClient();
		MongoCollection<Document> collection = mongoClient.getDatabase(cfg.dbName).getCollection("DMTKhai");

		try {
			docTmp = collection.find(docFind).allowDiskUse(true).iterator().next();
		} catch (Exception e) {

		}
		mongoClient.close();
		
		
		if(null == docTmp) {
			return fileInfo;
		}
		
		String dir = docTmp.get("Dir", "");
		String fileName = docTmp.get("FileNameXML", "");
		File file = new File(dir, fileName);
		
		if(!file.exists()) return fileInfo;
		
		fileInfo.setFileName(fileName);
		fileInfo.setContentFile(FileUtils.readFileToByteArray(file));		
		return fileInfo;
	}

	@Override
	public MsgRsp signSingle(InputStream is, JSONRoot jsonRoot, String _id) throws Exception {
		Msg msg = jsonRoot.getMsg();
		MsgHeader header = msg.getMsgHeader();
		MsgPage page = msg.getMsgPage();
		Object objData = msg.getObjData();
		
		MsgRsp rsp = new MsgRsp(header);
		rsp.setMsgPage(page);
		MspResponseStatus responseStatus = null;
		
		/*DOC NOI DUNG XML DA KY*/
		org.w3c.dom.Document xmlDoc = commons.inputStreamToDocument(is, true);
		
		ObjectId objectId = null;
		try {
			objectId = new ObjectId(_id);
		}catch(Exception e) {}
		
		/*KIEM TRA XEM THONG TIN TKHAI CO TON TAI KHONG*/
		Document docFind = new Document("IssuerId", header.getIssuerId())
				.append("_id", objectId).append("IsDelete", new Document("$ne", true))
				.append("Status", "CREATED");
		
		Document docTmp = null;
	
		MongoClient mongoClient = cfg.mongoClient();
		MongoCollection<Document> collection = mongoClient.getDatabase(cfg.dbName).getCollection("DMTKhai");

		try {
			docTmp = collection.find(docFind).allowDiskUse(true).iterator().next();
		} catch (Exception e) {

		}
		mongoClient.close();
		
		
		if(null == docTmp) {
			responseStatus = new MspResponseStatus(9999, "Không tìm thấy thông tin tờ khai.");
			rsp.setResponseStatus(responseStatus);
			return rsp;
		}
		
		/*LUU FILE VA CAP NHAT TRANG THAI*/
		String dir = docTmp.get("Dir", "");
		String fileName = _id + "_processing.xml";
		boolean check = commons.docW3cToFile(xmlDoc, dir, fileName);
		if(!check) {
			responseStatus = new MspResponseStatus(9999, "Lưu tập tin đã ký không thành công.");
			rsp.setResponseStatus(responseStatus);
			return rsp;
		}
		
		/*KET NOI VA DAY HD DEN CHUC TRUYEN NHAN*/
		org.w3c.dom.Document rTCTN = null;
		String MTDiep = docTmp.get("MTDiep", "");
		String MST = docTmp.get("MST", "");
		String TenNNT = docTmp.get("Name", "");
		/*END - KET NOI VA DAY HD DEN CHUC TRUYEN NHAN*/
		
		rTCTN = tctnService.callTiepNhanThongDiep("100", MTDiep, MST, "1", commons.fileToDocument(new File(dir, fileName), true));
		if(rTCTN == null) {
			rTCTN = tctnService.callTiepNhanThongDiep("100", MTDiep, MST, "1", commons.fileToDocument(new File(dir, fileName), true));
		}
		
		/*DO DU LIEU TRA VE - CAP NHAT LAI KET QUA*/
		XPath xPath = XPathFactory.newInstance().newXPath();
		Node nodeDLHDon = (Node) xPath.evaluate("/TDiep", rTCTN, XPathConstants.NODE);
		String codeTTTNhan = "3";
		codeTTTNhan = commons.getTextFromNodeXML((Element) xPath.evaluate("DLieu/TBao/TTTNhan", nodeDLHDon, XPathConstants.NODE));
		
		switch (codeTTTNhan) {
		case "1":
			responseStatus = new MspResponseStatus(9999, "Không tìm thấy tenant dữ liệu.");
			rsp.setResponseStatus(responseStatus);
			return rsp;
		case "2":
			responseStatus = new MspResponseStatus(9999, "Mã thông điệp đã tồn tại.");
			rsp.setResponseStatus(responseStatus);
			return rsp;
		case "3":
			responseStatus = new MspResponseStatus(9999, "Thất bại, lỗi Exception.");
			rsp.setResponseStatus(responseStatus);
			return rsp;
		default:
			break;
		}
		
		
		LocalDate localDate = LocalDate.now();
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
		String Nlap = localDate.format(formatter);
		
		/*CAP NHAT LAI TRANG THAI DANG CHO XU LY*/
		FindOneAndUpdateOptions options = new FindOneAndUpdateOptions();
		options.upsert(false);
		options.maxTime(5000, TimeUnit.MILLISECONDS);
		options.returnDocument(ReturnDocument.AFTER);
		
	
		
		
		 mongoClient = cfg.mongoClient();
			collection = mongoClient.getDatabase(cfg.dbName).getCollection("DMTKhai");
			collection.findOneAndUpdate(
					docFind, 
					new Document("$set", 
						new Document("Status", "PROCESSING")
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
		

		//UPDATE ACTIVE DMCTSO
		//CAP NHAT TRANG THAI HOAT DONG TRONG DMCTSO
		HashMap<String, Object> hItem = null;
		List<Object> listCert = new ArrayList<Object>();
		hItem = new LinkedHashMap<String, Object>();
		List<Document> rows = null;
		rows = docTmp.getList("DSCTSSDung", Document.class);
		for(Document o: rows) {
			hItem = new LinkedHashMap<String, Object>();
			hItem.put("TTChuc", o.get("TTChuc"));
			hItem.put("Seri", o.get("Seri"));
			hItem.put("TNgay", o.get("TNgay"));
			hItem.put("DNgay", o.get("DNgay"));
			hItem.put("HThuc",  o.get("HThuc"));	
			listCert.add(hItem);
		}
		
		Document docUpsert = new Document("IssuerId", header.getIssuerId())
				
				.append("TenNnt", TenNNT)
				.append("MST", MST)
				.append("TinhThanhInfo", docTmp.get("TinhThanhInfo"))
				.append("ChiCucThueInfo", docTmp.get("ChiCucThueInfo"))
			
				.append("NLap", commons.convertStringToLocalDate(Nlap, Constants.FORMAT_DATE.FORMAT_DATE_WEB))				
					
				.append("DSCTSSDung", listCert)
				.append("Status", Constants.INVOICE_STATUS.TK_CREATED)
				.append("IsDelete", false)
				.append("IsActive", true)
				.append("InfoCreated", 
					new Document("CreateDate", LocalDateTime.now())
					.append("CreateUserID", header.getUserId())
					.append("CreateUserName", header.getUserName())
					.append("CreateUserFullName", header.getUserFullName())
				);
			/*END - LUU DU LIEU*/
	
			 mongoClient = cfg.mongoClient();
				collection = mongoClient.getDatabase(cfg.dbName).getCollection("DMCTSo");
//				collection.insertOne(docUpsert);
				mongoClient.close();
			//END DMCTSo
			
		DateTimeFormatter format_time = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
		LocalDateTime time_dem  = LocalDateTime.now();
		String time = time_dem.format(format_time);	
			
		String name_company = removeAccent(header.getUserFullName());
		System.out.println(time +name_company+" Vua ky to khai");
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
		
		String actionCode = header.getActionCode();
		String _id = commons.getTextJsonNode(jsonData.at("/_id")).replaceAll("\\s", "");
		
		MsgRsp rsp = new MsgRsp(header);
		rsp.setMsgPage(page);
		MspResponseStatus responseStatus = null;
		
		ObjectId objectId = null;
		try {
			objectId = new ObjectId(_id);
		}catch(Exception e) {}
		
		/*KIEM TRA XEM THONG TIN TKHAI CO TON TAI KHONG*/
		Document docFind = new Document("IssuerId", header.getIssuerId())
				.append("_id", objectId).append("IsDelete", new Document("$ne", true))
				.append("Status", "PROCESSING");
		
		Document docTmp = null;
	
		
		MongoClient mongoClient = cfg.mongoClient();
		MongoCollection<Document> collection = mongoClient.getDatabase(cfg.dbName).getCollection("DMTKhai");

		try {
			docTmp = collection.find(docFind).allowDiskUse(true).iterator().next();
		} catch (Exception e) {

		}
		mongoClient.close();
		
		if(null == docTmp) {
			responseStatus = new MspResponseStatus(9999, "Không tìm thấy thông tin tờ khai.");
			rsp.setResponseStatus(responseStatus);
			return rsp;
		}
		
		String MTDiep = docTmp.get("MTDiep", "");
//		MTDiep = "0315382923GRXAUSQANBYZDBBSWYZ1X5SBQB89UROOTGUX";
		
//		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
//        DocumentBuilder db = dbf.newDocumentBuilder();        
//        org.w3c.dom.Document doc = db.newDocument();
//		doc.setXmlStandalone(true);		
//		Element root = doc.createElement("MTDiep");
//		doc.appendChild(commons.createElementWithValue(doc, "MTDiep", MTDiep));
		
		org.w3c.dom.Document rTCTN = tctnService.callTraCuuThongDiep(MTDiep);
//		rTCTN = commons.stringToDocument("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?><KetQuaTraCuu><MaKetQua>0</MaKetQua><MoTaKetQua>CQT đã trả kết quả xử lý thông điệp</MoTaKetQua><DuLieu><TDiep><TTChung><PBan>2.0.0</PBan><MNGui>TCT</MNGui><MNNhan>V0401486901</MNNhan><MLTDiep>999</MLTDiep><MTDiep>TCT9CCBD05C4DAF43E787B29CF15C055F7B</MTDiep><MTDTChieu>V040148690141B924C263C34C13B5EF49E1DDB10CA5</MTDTChieu></TTChung><DLieu><TBao><MTDiep>V040148690141B924C263C34C13B5EF49E1DDB10CA5</MTDiep><MNGui>V0401486901</MNGui><NNhan>2022-01-12T16:24:22</NNhan><TTTNhan>0</TTTNhan></TBao></DLieu></TDiep><TDiep><TTChung><PBan>2.0.0</PBan><MNGui>TCT</MNGui><MNNhan>V0401486901</MNNhan><MLTDiep>102</MLTDiep><MTDiep>TCT41DB272DE3264AC9B841546900B450A2</MTDiep><MTDTChieu>V040148690141B924C263C34C13B5EF49E1DDB10CA5</MTDTChieu><MST>0301521415</MST><SLuong>1</SLuong></TTChung><DLieu><TBao><DLTBao Id=\"Id-BAAA7B96E1FA43B1BA2F1074BA91159E\"><PBan>2.0.0</PBan><MSo>01/TB-TNĐT</MSo><Ten>Về việc không tiếp nhận tờ khai đăng ký sử dụng HĐĐT theo quy định tại Nghị định 123/2020/NĐ-CP</Ten><So>70100220000002867</So><DDanh>...</DDanh><NTBao>2022-01-12</NTBao><MST>0301521415</MST><TNNT>CÔNG TY CỔ PHẦN TÔ THÀNH PHÁT</TNNT><TTKhai>Tờ khai đăng ký/thay đổi thông tin sử dụng hóa đơn điện tử</TTKhai><MGDDTu>V040148690141B924C263C34C13B5EF49E1DDB10CA5</MGDDTu><TGGui>2022-01-12T16:24:22</TGGui><THop>2</THop><TGNhan>2022-01-12T16:24:22</TGNhan><DSLDKCNhan><LDo><MLoi>9992</MLoi><MTa>Đã tồn tại tờ khai đăng ký sử dụng hóa đơn điện tử khác của người nộp thuế đã được cơ quan thuế chấp nhận</MTa></LDo></DSLDKCNhan></DLTBao><DSCKS><CQT><Signature xmlns=\"http://www.w3.org/2000/09/xmldsig#\" Id=\"Tct-5223b2f9e70e458b8bb3eef420b4c0bb\"><SignedInfo><CanonicalizationMethod Algorithm=\"http://www.w3.org/TR/2001/REC-xml-c14n-20010315\"/><SignatureMethod Algorithm=\"http://www.w3.org/2001/04/xmldsig-more#rsa-sha256\"/><Reference URI=\"#Id-BAAA7B96E1FA43B1BA2F1074BA91159E\"><DigestMethod Algorithm=\"http://www.w3.org/2001/04/xmlenc#sha256\"/><DigestValue>9Y0V1zk1JJghi5/THFzOzfAhgKk0LLK964UTrEUq7+c=</DigestValue></Reference><Reference URI=\"#SigningTime-613d60a8d4e34d079888ffde907ebc8d\"><DigestMethod Algorithm=\"http://www.w3.org/2001/04/xmlenc#sha256\"/><DigestValue>4MY0Rh4I8Bn3SW7zXYSSympZTRzjOC0OvvkzJdyEqz4=</DigestValue></Reference></SignedInfo><SignatureValue>JOoO3RE8hz+oG+WQlYB7SpCb1ASpBCOZ8OUSBCwswvImOrwNkIVme5IAE+a3m5TF/Lf44qH4Wbn7jtwV4BQ6WtJ5yfoxWiqF4vWO+5pVqANvLizVOrco3v7vEzxVjYGT2ns7HkTnX4A3/ayahC5cL6kMGxxS/33eigYoBbCrqOBkrYQqUyQHjhtXCXnJVnZa5hfIjF+DP+OkGsvB48rZtldMBcX11c8GHTPJUMNh4z+46OFfol3sykVRkfdskkgz6KWgpgikafSIEqo3jOcVuK8wsoO4rofid2FGcwQsElm6z5c5EgxOFrXGk0I5x1lZrc6b8VXipFN4dHh0mSlLPA==</SignatureValue><KeyInfo><X509Data><X509SubjectName>CN=TỔNG CỤC THUẾ,O=BỘ TÀI CHÍNH,L=Hà Nội,C=VN</X509SubjectName><X509Certificate>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</X509Certificate></X509Data></KeyInfo><Object Id=\"SigningTime-613d60a8d4e34d079888ffde907ebc8d\"><SignatureProperties><SignatureProperty Target=\"signatureProperties\"><SigningTime>2022-01-12T16:24:22</SigningTime></SignatureProperty></SignatureProperties></Object></Signature></CQT></DSCKS></TBao></DLieu></TDiep></DuLieu></KetQuaTraCuu>");
//		rTCTN = commons.stringToDocument("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?><KetQuaTraCuu><MaKetQua>0</MaKetQua><MoTaKetQua>CQT đã trả kết quả xử lý thông điệp</MoTaKetQua><DuLieu><TDiep><TTChung><PBan>2.0.0</PBan><MNGui>TCT</MNGui><MNNhan>V0401486901</MNNhan><MLTDiep>999</MLTDiep><MTDiep>TCTCB8DB34A7ACF481190D7089F71EA6395</MTDiep><MTDTChieu>V0401486901CB76C5C14099451E81164B104AB41E27</MTDTChieu></TTChung><DLieu><TBao><MTDiep>V0401486901CB76C5C14099451E81164B104AB41E27</MTDiep><MNGui>V0401486901</MNGui><NNhan>2022-01-14T16:35:56</NNhan><TTTNhan>0</TTTNhan></TBao></DLieu></TDiep><TDiep><TTChung><PBan>2.0.0</PBan><MNGui>TCT</MNGui><MNNhan>V0401486901</MNNhan><MLTDiep>102</MLTDiep><MTDiep>TCT8FE695FBC6FE45B9871DC11242F682C9</MTDiep><MTDTChieu>V0401486901CB76C5C14099451E81164B104AB41E27</MTDTChieu><MST>0301521415</MST><SLuong>1</SLuong></TTChung><DLieu><TBao><DLTBao Id=\"Id-CC6FDDA410CD46EBAA073364067AAA8F\"><PBan>2.0.0</PBan><MSo>01/TB-TNĐT</MSo><Ten>Về việc tiếp nhận tờ khai thay đổi thông tin sử dụng HĐĐT theo quy định tại Nghị định 123/2020/NĐ-CP</Ten><So>7901220000011805</So><DDanh>Hà Nội</DDanh><NTBao>2022-01-14</NTBao><MST>0301521415</MST><TNNT>CÔNG TY CỔ PHẦN TÔ THÀNH PHÁT</TNNT><TTKhai>Tờ khai đăng ký/thay đổi thông tin sử dụng hóa đơn điện tử</TTKhai><MGDDTu>V0401486901CB76C5C14099451E81164B104AB41E27</MGDDTu><TGGui>2022-01-14T16:35:56</TGGui><THop>3</THop><TGNhan>2022-01-14T16:35:56</TGNhan></DLTBao><DSCKS><CQT><Signature xmlns=\"http://www.w3.org/2000/09/xmldsig#\" Id=\"Tct-3a6c42cebb564f50b0eadc7afce4ed9c\"><SignedInfo><CanonicalizationMethod Algorithm=\"http://www.w3.org/TR/2001/REC-xml-c14n-20010315\" /><SignatureMethod Algorithm=\"http://www.w3.org/2001/04/xmldsig-more#rsa-sha256\" /><Reference URI=\"#Id-CC6FDDA410CD46EBAA073364067AAA8F\"><DigestMethod Algorithm=\"http://www.w3.org/2001/04/xmlenc#sha256\" /><DigestValue>yXy9lktFHfBPi73p7gvdX1UvXSRlumziJNF8MahRJgU=</DigestValue></Reference><Reference URI=\"#SigningTime-7723dfe24af64ee9bef5fa4dfdcbc8b0\"><DigestMethod Algorithm=\"http://www.w3.org/2001/04/xmlenc#sha256\" /><DigestValue>Efu8kkQKEtySg40lRxH9mJU+w26iQPVfLcmh8XId0hM=</DigestValue></Reference></SignedInfo><SignatureValue>mGnAh6ICysY9wzJbILHPoDzdf8CszKSGcLCzh9fC/rqMNKl27xqGse0VSMbmhLhAsS9FQvFvOaz7g6wVU7s2NGFcZSOGd/+j0ds02WpAs7cb7kkIFvz6P0iJwUMM7NpwHWkkeN4Y5heiwYxtQwTVtzU+bBby9XgZdZgUWVKQgg+/QYG5sBfsvz/+sFz138aLEMNCXeVXdRbXIppBig/haWY5O4AGKEC75Vy3TbHVFAKbfIqXhhc9kczXxah+hoq7KQiCfihpq/JnkktIW+xsUjFxfBeprH6Q2PQX4Lg4FKEpXwHaQNp/tokWLetjFMPudQXvQK2cW9rNvq8wc2WF7w==</SignatureValue><KeyInfo><X509Data><X509SubjectName>CN=TỔNG CỤC THUẾ,O=BỘ TÀI CHÍNH,L=Hà Nội,C=VN</X509SubjectName><X509Certificate>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</X509Certificate></X509Data></KeyInfo><Object Id=\"SigningTime-7723dfe24af64ee9bef5fa4dfdcbc8b0\"><SignatureProperties><SignatureProperty Target=\"signatureProperties\"><SigningTime>2022-01-14T16:35:56</SigningTime></SignatureProperty></SignatureProperties></Object></Signature></CQT></DSCKS></TBao></DLieu></TDiep></DuLieu></KetQuaTraCuu>");
//		rTCTN = commons.stringToDocument("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?><KetQuaTraCuu><MaGD>8A5A73DF108D4181965B5854E76D39E2</MaGD><MaKetQua>0</MaKetQua><MoTaKetQua>CQT đã trả kết quả xử lý thông điệp</MoTaKetQua><DuLieu><TDiep><TTChung><PBan>2.0.0</PBan><MNGui>TCT</MNGui><MNNhan>V0401486901</MNNhan><MLTDiep>999</MLTDiep><MTDiep>TCT1296B10E8DE04A30A51F286CFA5AB692</MTDiep><MTDTChieu>V04014869018A5A73DF108D4181965B5854E76D39E2</MTDTChieu></TTChung><DLieu><TBao><MTDiep>V04014869018A5A73DF108D4181965B5854E76D39E2</MTDiep><MNGui>V0401486901</MNGui><NNhan>2022-01-17T10:22:22</NNhan><TTTNhan>0</TTTNhan></TBao></DLieu></TDiep><TDiep><TTChung><PBan>2.0.0</PBan><MNGui>TCT</MNGui><MNNhan>V0401486901</MNNhan><MLTDiep>102</MLTDiep><MTDiep>TCTF0335C05A4A04783ABD9BB1D8A117AB4</MTDiep><MTDTChieu>V04014869018A5A73DF108D4181965B5854E76D39E2</MTDTChieu><MST>0314080292</MST><SLuong>1</SLuong></TTChung><DLieu><TBao><DLTBao Id=\"Id-D9DB164BC5534A1D8976ECDC51BED093\"><PBan>2.0.0</PBan><MSo>01/TB-TNĐT</MSo><Ten>Về việc tiếp nhận tờ khai đăng ký sử dụng HĐĐT theo quy định tại Nghị định 123/2020/NĐ-CP</Ten><So>7922220000007366</So><DDanh>Hà Nội</DDanh><NTBao>2022-01-17</NTBao><MST>0314080292</MST><TNNT>TRẦN THỊ LÊ</TNNT><TTKhai>Tờ khai đăng ký/thay đổi thông tin sử dụng hóa đơn điện tử</TTKhai><MGDDTu>V04014869018A5A73DF108D4181965B5854E76D39E2</MGDDTu><TGGui>2022-01-17T10:22:23</TGGui><THop>1</THop><TGNhan>2022-01-17T10:22:23</TGNhan></DLTBao><DSCKS><CQT><Signature xmlns=\"http://www.w3.org/2000/09/xmldsig#\" Id=\"Tct-b98915423c264660b67be063faf11748\"><SignedInfo><CanonicalizationMethod Algorithm=\"http://www.w3.org/TR/2001/REC-xml-c14n-20010315\" /><SignatureMethod Algorithm=\"http://www.w3.org/2001/04/xmldsig-more#rsa-sha256\" /><Reference URI=\"#Id-D9DB164BC5534A1D8976ECDC51BED093\"><DigestMethod Algorithm=\"http://www.w3.org/2001/04/xmlenc#sha256\" /><DigestValue>SPHDQKMzfdwyi8bSbk+Rp0m49RWJ1GPle/0FlphVC3c=</DigestValue></Reference><Reference URI=\"#SigningTime-baf6e6a8e98d4e31b9a65e7854081817\"><DigestMethod Algorithm=\"http://www.w3.org/2001/04/xmlenc#sha256\" /><DigestValue>MxOjOMU1Fyg9N24T0JM7uhtfDbaOq2C0A98oNoH0Q1E=</DigestValue></Reference></SignedInfo><SignatureValue>HrhctUjlqhyDZ0qNruFyDma4ZtcLFh355WFK8sD6b3kwPA/w4TCYzQImttSSBycHd+2X47VIR3fdugkJzuz46Vd6wAFp5Sq2B/cw/jPQZui+Ex0KKb9Awd640q8jFZfuU8627+zUADoThfYaITHKPv9helxUBfBPgZTIYln7B8VhJSku1eLpRjmtdsQOWDQwqGJSNLr9IL02r+v3Qc+Pzn5WnUgJx1N7NFdYDg1V+E/T9WxcGFeCyIX0a8i7Zt6xWifzgP9pDHUBQNNeEp9Ufl/abVJlJCvz7gZyKSHPT+vX2SJ5MCyi9bFOMqzPl02whvUl8TE0PsBdLly2o5PSVw==</SignatureValue><KeyInfo><X509Data><X509SubjectName>CN=TỔNG CỤC THUẾ,O=BỘ TÀI CHÍNH,L=Hà Nội,C=VN</X509SubjectName><X509Certificate>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</X509Certificate></X509Data></KeyInfo><Object Id=\"SigningTime-baf6e6a8e98d4e31b9a65e7854081817\"><SignatureProperties><SignatureProperty Target=\"signatureProperties\"><SigningTime>2022-01-17T10:22:23</SigningTime></SignatureProperty></SignatureProperties></Object></Signature></CQT></DSCKS></TBao></DLieu></TDiep><TDiep><TTChung><PBan>2.0.0</PBan><MNGui>TCT</MNGui><MNNhan>V0401486901</MNNhan><MLTDiep>103</MLTDiep><MTDiep>TCTD3F4BD1CFE724D41B1691FB5B1BCF383</MTDiep><MTDTChieu>V04014869018A5A73DF108D4181965B5854E76D39E2</MTDTChieu><MST>0314080292</MST><SLuong>1</SLuong></TTChung><DLieu><TBao><DLTBao Id=\"Id-182587d9d657481eb4a7d09383c94d9a\"><PBan>2.0.0</PBan><MSo>01/TB-ĐKĐT</MSo><Ten>Thông báo về việc không chấp nhận đăng ký sử dụng hóa đơn điện tử</Ten><DDanh>Huyện Hóc Môn</DDanh><TCQTCTren>Cục Thuế Thành phố Hồ Chí Minh</TCQTCTren><TCQT>Chi cục Thuế khu vực Quận 12 - huyện Hóc Môn</TCQT><MST>0314080292</MST><TNNT>TRẦN THỊ LÊ</TNNT><Ngay>2022-01-17T07:00:00</Ngay><HTDKy>1</HTDKy><TTXNCQT>2</TTXNCQT><HThuc>KT. Chi Cục Trưởng</HThuc><CDanh>Phó Chi Cục Trưởng</CDanh><DSLDKCNhan><LDo><MLoi>2005</MLoi><MTa>NNT chọn loại hóa đơn sử dụng chưa phù hợp với phương pháp tính thuế đã đăng ký với cơ quan thuế</MTa></LDo><LDo><MLoi>2007</MLoi><MTa>Ghi chú: liên hệ đội kê khai (ĐT: 38917432) để đối chiếu điều chỉnh trường hợp đăng ký ban đầu sai phương pháp tính thuế</MTa></LDo></DSLDKCNhan></DLTBao><STBao Id=\"Id-75bb7dbf51804f538df5f5362ed27f11\"><So>1169TB-CCTKVQ12HM-HĐĐT</So><NTBao>2022-01-17T15:31:57</NTBao></STBao><DSCKS><TTCQT><Signature xmlns=\"http://www.w3.org/2000/09/xmldsig#\" Id=\"TTCQT-Id-182587d9d657481eb4a7d09383c94d9a\"><SignedInfo><CanonicalizationMethod Algorithm=\"http://www.w3.org/TR/2001/REC-xml-c14n-20010315\" /><SignatureMethod Algorithm=\"http://www.w3.org/2000/09/xmldsig#rsa-sha1\" /><Reference URI=\"#Object-TTCQT-Id-182587d9d657481eb4a7d09383c94d9a\"><DigestMethod Algorithm=\"http://www.w3.org/2000/09/xmldsig#sha1\" /><DigestValue>jm+3AmlhEyJrgzLDPEy0v95u+Ss=</DigestValue></Reference><Reference URI=\"#Id-182587d9d657481eb4a7d09383c94d9a\"><DigestMethod Algorithm=\"http://www.w3.org/2000/09/xmldsig#sha1\" /><DigestValue>3YlQs4OxeVPwHF6dzOM7+bcr7kY=</DigestValue></Reference></SignedInfo><SignatureValue>ovsj3SApWg8GVdrWZWTxcLjofFWFVG67MwFnkmxEFfRbRBU1Fp9MyR9d8L/u9M7f5ypDLFUu8jUvkDVFIA3VbVwUYhMdLU5XV0jrUkWyaExm450hLZSKpnKQdHCAMkujx6kNaNUAV5KJZ5gnZ/lv/FaSbqxHwVJyzB/Yxouqi1FzYQNWepw/5kUhLaQpxO/l+kkOJf3Plz/dhOG6WtRNBolNxMPs1T96b+ZUcfps7NqPfNYXgd3dz22w2JyhwQNZmj5EAE3Va+ezmCZ3FgjnZL4vEoX1ZQu5EO1zUFZpxGVeWz8N4/KTa5CU4Gew1cCbYEXRQJjf6c3+9tjb7K180Q==</SignatureValue><KeyInfo><X509Data><X509SubjectName>CN=Trần Việt Thắng, L=Hồ Chí Minh, OU=Cục Thuế thành phố Hồ Chí Minh, OU=Tổng cục Thuế, O=Bộ Tài chính, C=VN</X509SubjectName><X509Certificate>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</X509Certificate></X509Data></KeyInfo><Object Id=\"Object-TTCQT-Id-182587d9d657481eb4a7d09383c94d9a\"><SignatureProperties xmlns=\"\"><SignatureProperty Id=\"SignatureProperty-TTCQT-Id-182587d9d657481eb4a7d09383c94d9a\" Target=\"#TTCQT-Id-182587d9d657481eb4a7d09383c94d9a\"><SigningTime>2022-01-17T15:24:10</SigningTime></SignatureProperty></SignatureProperties></Object></Signature></TTCQT><CQT><Signature Id=\"CQT-Id-182587d9d657481eb4a7d09383c94d9a\" xmlns=\"http://www.w3.org/2000/09/xmldsig#\"><SignedInfo><CanonicalizationMethod Algorithm=\"http://www.w3.org/TR/2001/REC-xml-c14n-20010315\" /><SignatureMethod Algorithm=\"http://www.w3.org/2001/04/xmldsig-more#rsa-sha256\" /><Reference URI=\"#Object-CQT-Id-182587d9d657481eb4a7d09383c94d9a\"><DigestMethod Algorithm=\"http://www.w3.org/2001/04/xmlenc#sha256\" /><DigestValue>zGl0fpnJUfgdAUJv6J7kNLSeN+pgVunKyoLUtxpDorU=</DigestValue></Reference><Reference URI=\"#Id-182587d9d657481eb4a7d09383c94d9a\"><DigestMethod Algorithm=\"http://www.w3.org/2001/04/xmlenc#sha256\" /><DigestValue>msU2MwCtqmhqvRJNjuecHzU1bB7UGH/HmuKcUJ5iUNM=</DigestValue></Reference><Reference URI=\"#Id-75bb7dbf51804f538df5f5362ed27f11\"><DigestMethod Algorithm=\"http://www.w3.org/2001/04/xmlenc#sha256\" /><DigestValue>M7t1LRsrS87Tz8LItJ6DB3IGV0DOu6DINrGdtkUDwMo=</DigestValue></Reference></SignedInfo><SignatureValue>bhOXTqkDp5m27QaAp0W8wYjucQ2n+g+vHt/Np7hovn4Jg15AhvGngDIcWQuviV/xzCWqV93tfDCoZMXGKqCYA9K8tlDZFf3Qe0pwV4/KMKVLqyvVOKBINqv9lbkyChwE0ZrOmJxiyDlP348nCsk8UB4XZkOMth0/Q9tb7QW2+Ns6au5Wlpe94Z+N0LcSnd3RT7pZbiCGnAfPnc4aibZB7QYr0imcJXRmJeDqS0BMciRY0fbDRYRwWWYW1VOsNpWzrBHOOh14RNAIhe+JTWdkpmNJiVArfYeSwxwJreHBtJHV0vGhXDQAPAUnsRKOmJuvzKB/pnR47eXwbA6TnCfLSA==</SignatureValue><KeyInfo><X509Data><X509SubjectName>OID.0.9.2342.19200300.100.1.1=MST:0301519977-029, CN=Chi cục thuế Khu vực Quận 12 - Huyện Hóc Môn, L=Hà Nội, OU=Cục thuế Thành phố Hồ Chí Minh, OU=Tổng Cục thuế, O=Bộ Tài chính, C=VN</X509SubjectName><X509Certificate>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</X509Certificate></X509Data></KeyInfo><Object Id=\"Object-CQT-Id-182587d9d657481eb4a7d09383c94d9a\"><SignatureProperties xmlns=\"\"><SignatureProperty Target=\"#CQT-Id-182587d9d657481eb4a7d09383c94d9a\" Id=\"SignatureProperty-CQT-Id-182587d9d657481eb4a7d09383c94d9a\"><SigningTime>2022-01-17T15:32:23</SigningTime></SignatureProperty></SignatureProperties></Object></Signature></CQT></DSCKS></TBao></DLieu></TDiep></DuLieu></KetQuaTraCuu>");
//		rTCTN = commons.stringToDocument("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?><KetQuaTraCuu><MaGD>48366DC9B0F64D438770C141719975D2</MaGD><MaKetQua>0</MaKetQua><MoTaKetQua>CQT đã trả kết quả xử lý thông điệp</MoTaKetQua><DuLieu><TDiep><TTChung><PBan>2.0.0</PBan><MNGui>TCT</MNGui><MNNhan>V0401486901</MNNhan><MLTDiep>999</MLTDiep><MTDiep>TCT1E28023993C6406582FDEE4C6EE41029</MTDiep><MTDTChieu>V040148690148366DC9B0F64D438770C141719975D2</MTDTChieu></TTChung><DLieu><TBao><MTDiep>V040148690148366DC9B0F64D438770C141719975D2</MTDiep><MNGui>V0401486901</MNGui><NNhan>2022-01-19T14:43:21</NNhan><TTTNhan>0</TTTNhan></TBao></DLieu></TDiep><TDiep><TTChung><PBan>2.0.0</PBan><MNGui>TCT</MNGui><MNNhan>V0401486901</MNNhan><MLTDiep>102</MLTDiep><MTDiep>TCTC959CE2D80324C4083B7563BCCAFD890</MTDiep><MTDTChieu>V040148690148366DC9B0F64D438770C141719975D2</MTDTChieu><MST>0314326450</MST><SLuong>1</SLuong></TTChung><DLieu><TBao><DLTBao Id=\"Id-5D7289F78582460EA5AD090E1396F3E6\"><PBan>2.0.0</PBan><MSo>01/TB-TNĐT</MSo><Ten>Về việc tiếp nhận tờ khai đăng ký sử dụng HĐĐT theo quy định tại Nghị định 123/2020/NĐ-CP</Ten><So>7926220000003830</So><DDanh>Hà Nội</DDanh><NTBao>2022-01-19</NTBao><MST>0314326450</MST><TNNT>CÔNG TY TNHH THƯƠNG MẠI DỊCH VỤ VẬT LIỆU XÂY DỰNG CÔNG NGHỆ CAO</TNNT><TTKhai>Tờ khai đăng ký/thay đổi thông tin sử dụng hóa đơn điện tử</TTKhai><MGDDTu>V040148690148366DC9B0F64D438770C141719975D2</MGDDTu><TGGui>2022-01-19T14:43:21</TGGui><THop>1</THop><TGNhan>2022-01-19T14:43:21</TGNhan></DLTBao><DSCKS><CQT><Signature xmlns=\"http://www.w3.org/2000/09/xmldsig#\" Id=\"Tct-06c98483c3a4424b8a821713c373f557\"><SignedInfo><CanonicalizationMethod Algorithm=\"http://www.w3.org/TR/2001/REC-xml-c14n-20010315\" /><SignatureMethod Algorithm=\"http://www.w3.org/2001/04/xmldsig-more#rsa-sha256\" /><Reference URI=\"#Id-5D7289F78582460EA5AD090E1396F3E6\"><DigestMethod Algorithm=\"http://www.w3.org/2001/04/xmlenc#sha256\" /><DigestValue>CoDQ8HdXRC9Mu7wAq3Ci4qrcCCLqxORXL1AW9xWOEO4=</DigestValue></Reference><Reference URI=\"#SigningTime-5aefb7585fba4cacbc2c864867628d28\"><DigestMethod Algorithm=\"http://www.w3.org/2001/04/xmlenc#sha256\" /><DigestValue>6awuEZZKBRZ2a1Caqx2OgUdZJoWw/2kL9aLUuHtXTHU=</DigestValue></Reference></SignedInfo><SignatureValue>ndO8Pr9Dapj4HDeQWbG06oxGnv4Q3IspeGXze4I7JSiJkOFGNbV/Dr26ofeTLyaz4mA7NXJl0qftvldaRBawKTRAR0WDMXZlQmRYvYaBV9pqwO4AARLft73cDW0v3WeMbgbdZM6nI9BczpgoM42uHtH4zZfSqv+918ahItYX8Y0PXq/ChATCr278UqsYzmjyt7/sUKRs7wgv1s0KxZKCHA5lqyVsWVh/Rs/OVjgWQc8EW+JXAVX0fuYYzKgq7TTM/xUlkYPcKAoDD8yle7OvtsCFTe2S8/ehQK1E7CXLQiBpRa7r6Anp8nyretOc1vY2wGQe8OWNG5+iGjzleF5Qrw==</SignatureValue><KeyInfo><X509Data><X509SubjectName>CN=TỔNG CỤC THUẾ,O=BỘ TÀI CHÍNH,L=Hà Nội,C=VN</X509SubjectName><X509Certificate>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</X509Certificate></X509Data></KeyInfo><Object Id=\"SigningTime-5aefb7585fba4cacbc2c864867628d28\"><SignatureProperties><SignatureProperty Target=\"signatureProperties\"><SigningTime>2022-01-19T14:43:21</SigningTime></SignatureProperty></SignatureProperties></Object></Signature></CQT></DSCKS></TBao></DLieu></TDiep><TDiep><TTChung><PBan>2.0.0</PBan><MNGui>TCT</MNGui><MNNhan>V0401486901</MNNhan><MLTDiep>103</MLTDiep><MTDiep>TCT383841A58ABC42339B28300B609C4156</MTDiep><MTDTChieu>V040148690148366DC9B0F64D438770C141719975D2</MTDTChieu><MST>0314326450</MST><SLuong>1</SLuong></TTChung><DLieu><TBao><DLTBao Id=\"Id-e602c6eecc414bd6ae9ba0d16ba8a5e9\"><PBan>2.0.0</PBan><MSo>01/TB-ĐKĐT</MSo><Ten>Thông báo về việc không chấp nhận đăng ký sử dụng hóa đơn điện tử</Ten><DDanh>Thành Phố Thủ Đức</DDanh><TCQTCTren>Cục Thuế Thành phố Hồ Chí Minh</TCQTCTren><TCQT>Chi cục Thuế Thành phố Thủ Đức</TCQT><MST>0314326450</MST><TNNT>CÔNG TY TNHH THƯƠNG MẠI DỊCH VỤ VẬT LIỆU XÂY DỰNG CÔNG NGHỆ CAO</TNNT><Ngay>2022-01-19T07:00:00</Ngay><HTDKy>1</HTDKy><TTXNCQT>2</TTXNCQT><HThuc /><CDanh>Phó Chi Cục Trưởng</CDanh><DSLDKCNhan><LDo><MLoi>2004</MLoi><MTa>NNT phải đăng ký phương thức chuyển dữ liệu là Chuyển đầy đủ nội dung từng hóa đơn</MTa></LDo></DSLDKCNhan></DLTBao><STBao Id=\"Id-c3a83deb3c63480abbbd43f0b8bc768a\"><So>7562TB-CCTTPTĐ</So><NTBao>2022-01-19T16:41:57</NTBao></STBao><DSCKS><TTCQT><Signature xmlns=\"http://www.w3.org/2000/09/xmldsig#\" Id=\"TTCQT-Id-e602c6eecc414bd6ae9ba0d16ba8a5e9\"><SignedInfo><CanonicalizationMethod Algorithm=\"http://www.w3.org/TR/2001/REC-xml-c14n-20010315\" /><SignatureMethod Algorithm=\"http://www.w3.org/2000/09/xmldsig#rsa-sha1\" /><Reference URI=\"#Object-TTCQT-Id-e602c6eecc414bd6ae9ba0d16ba8a5e9\"><DigestMethod Algorithm=\"http://www.w3.org/2000/09/xmldsig#sha1\" /><DigestValue>LEzr/HKPRZN5ypXpIHi24xKmiDU=</DigestValue></Reference><Reference URI=\"#Id-e602c6eecc414bd6ae9ba0d16ba8a5e9\"><DigestMethod Algorithm=\"http://www.w3.org/2000/09/xmldsig#sha1\" /><DigestValue>GICxayuyGwriSzuI66zLIEMFiKk=</DigestValue></Reference></SignedInfo><SignatureValue>SXTGPFg1puh+WRlVAbpUExj12NVj2BzrFBNJB7Mb0Q53pGTDBxda4UZXQmW6dGp/eRrgKAHYDtYyX1iNa1rUSlDo3dj++CRyoro9hi9GUWYyFZ7avrOm71YzDdSFPUEx/KHAJgaTm/RZsI1V9HBzFzEeFXhK6MuXUuFzqsdyEXSEF0e8dEo03dt6kKQT1N1OtCYeiQJ9ALvkd1XdjYLQjR0wEqup8UmuX9+Dv1HnQR0oE0umBKoO3YCTgu2tpm0WySXpOkGW06QIaODrmFEx9uMjGjOIrtKNePfQvNSzBYPpuAzK2AxQJxspGyGD8n7PxdQ2It6CotdDqOsa56MBgg==</SignatureValue><KeyInfo><X509Data><X509SubjectName>CN=Trần Thị Hoàng Dung, L=Hồ Chí Minh, OU=Cục Thuế thành phố Hồ Chí Minh, OU=Tổng cục Thuế, O=Bộ Tài chính, C=VN</X509SubjectName><X509Certificate>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</X509Certificate></X509Data></KeyInfo><Object Id=\"Object-TTCQT-Id-e602c6eecc414bd6ae9ba0d16ba8a5e9\"><SignatureProperties xmlns=\"\"><SignatureProperty Id=\"SignatureProperty-TTCQT-Id-e602c6eecc414bd6ae9ba0d16ba8a5e9\" Target=\"#TTCQT-Id-e602c6eecc414bd6ae9ba0d16ba8a5e9\"><SigningTime>2022-01-19T16:12:29</SigningTime></SignatureProperty></SignatureProperties></Object></Signature></TTCQT><CQT><Signature Id=\"CQT-Id-e602c6eecc414bd6ae9ba0d16ba8a5e9\" xmlns=\"http://www.w3.org/2000/09/xmldsig#\"><SignedInfo><CanonicalizationMethod Algorithm=\"http://www.w3.org/TR/2001/REC-xml-c14n-20010315\" /><SignatureMethod Algorithm=\"http://www.w3.org/2001/04/xmldsig-more#rsa-sha256\" /><Reference URI=\"#Object-CQT-Id-e602c6eecc414bd6ae9ba0d16ba8a5e9\"><DigestMethod Algorithm=\"http://www.w3.org/2001/04/xmlenc#sha256\" /><DigestValue>MYgwK9pRQEkzavDfsQQCnqi90VffJI/t2z1c6M03SUE=</DigestValue></Reference><Reference URI=\"#Id-e602c6eecc414bd6ae9ba0d16ba8a5e9\"><DigestMethod Algorithm=\"http://www.w3.org/2001/04/xmlenc#sha256\" /><DigestValue>SM40Oo3ac3YYqdPfS6ncIfPIT2Nstrw+f/cITkgjQCQ=</DigestValue></Reference><Reference URI=\"#Id-c3a83deb3c63480abbbd43f0b8bc768a\"><DigestMethod Algorithm=\"http://www.w3.org/2001/04/xmlenc#sha256\" /><DigestValue>1asYpggHOoUGocdLkRjY7Rdv9c2pOlnyw+qpfBsCeYw=</DigestValue></Reference></SignedInfo><SignatureValue>R5LvFAKLQAXNC7pUsh8ABQ+ntCph+CH/QpoU2JQVaS4qXcpO6M52yO6e3z8ioil7JDFhs7XX8VkLShBmzNLiB4zyvTkXbowsm9l24f/xp1XVOl/x18rHHOvs1tlF+IvW4MNW4mSTej3TM3YA8WSK2D4ae51LqhJc+AtSEN4/3Da2D6Y57msjspO+Z0r3jYS/1wfQe4Gll41f5uTeYFM9faPDpF07TFgZ4E+TYruleiKTerdLql3y2ff98w739cjEYb6LaMYWJdS5M33fw4h9iqfs2MdG2xn/9dUZGD+HRcckEeF6Nz1fJk92H2WYmCAHJTXRvny8SXxpjPVhFZG4cg==</SignatureValue><KeyInfo><X509Data><X509SubjectName>CN=CHI CỤC THUẾ THÀNH PHỐ THỦ ĐỨC, OU=CỤC THUẾ THÀNH PHỐ HỒ CHÍ MINH, OU=TỔNG CỤC THUẾ, O=BỘ TÀI CHÍNH, L=Hồ Chí Minh, C=VN</X509SubjectName><X509Certificate>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</X509Certificate></X509Data></KeyInfo><Object Id=\"Object-CQT-Id-e602c6eecc414bd6ae9ba0d16ba8a5e9\"><SignatureProperties xmlns=\"\"><SignatureProperty Target=\"#CQT-Id-e602c6eecc414bd6ae9ba0d16ba8a5e9\" Id=\"SignatureProperty-CQT-Id-e602c6eecc414bd6ae9ba0d16ba8a5e9\"><SigningTime>2022-01-19T16:39:46</SigningTime></SignatureProperty></SignatureProperties></Object></Signature></CQT></DSCKS></TBao></DLieu></TDiep></DuLieu></KetQuaTraCuu>");
		if(rTCTN == null) {
			responseStatus = new MspResponseStatus(9999, "Kết nối với TCTN không thành công.");
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
		
		Node nodeTDiep = (Node) xPath.evaluate("DuLieu/TDiep[last()]", nodeKetQuaTraCuu, XPathConstants.NODE);
		if(nodeTDiep == null) {
			responseStatus = new MspResponseStatus(9999, "Không đọc được kết quả tra cứu.");
			rsp.setResponseStatus(responseStatus);
			return rsp;
		}
		String CQT_MLTDiep = commons.getTextFromNodeXML((Element) xPath.evaluate("TTChung/MLTDiep", nodeTDiep, XPathConstants.NODE));
		if("|102|103|".indexOf("|" + CQT_MLTDiep + "|") == -1) {
			responseStatus = new MspResponseStatus(9999, "CQT chưa có thông báo kết quả trả về.");
			rsp.setResponseStatus(responseStatus);
			return rsp;
		}
		
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
		
		String MLoi = "";
		String MTa = "";
		Node nodeTmp = null;
		/*CAP NHAT TRANG THAI COMPLETE - TRANG THAI CQT*/
		FindOneAndUpdateOptions options = new FindOneAndUpdateOptions();
		options.upsert(false);
		options.maxTime(5000, TimeUnit.MILLISECONDS);
		options.returnDocument(ReturnDocument.AFTER);
		
		Document docUpdate = new Document("StatusCQT", CQT_MLTDiep);
		if("103".equals(CQT_MLTDiep)) {
			/*LAY DANH SACH LOI (NEU CO)*/
			NodeList nodeListDSLDKCNhan = (NodeList) xPath.evaluate("DLieu/TBao/DLTBao/DSLDKCNhan/LDo", nodeTDiep, XPathConstants.NODESET) ;
			if(null == nodeListDSLDKCNhan || nodeListDSLDKCNhan.getLength() == 0) {
				docUpdate.append("Status", "COMPLETE");	
			}else {
				List<Document> DSLDKCNhan = new ArrayList<Document>();
				for(int i = 0; i < nodeListDSLDKCNhan.getLength(); i++) {
					nodeTmp = nodeListDSLDKCNhan.item(i);
					if("".equals(MLoi)) {
						MLoi = commons.getTextFromNodeXML((Element) xPath.evaluate("MLoi", nodeTmp, XPathConstants.NODE));
						MTa = commons.getTextFromNodeXML((Element) xPath.evaluate("MTa", nodeTmp, XPathConstants.NODE));
					}
					DSLDKCNhan.add(
						new Document("MLoi", commons.getTextFromNodeXML((Element) xPath.evaluate("MLoi", nodeTmp, XPathConstants.NODE)))
						.append("MTa", commons.getTextFromNodeXML((Element) xPath.evaluate("MTa", nodeTmp, XPathConstants.NODE)))
					);
				}
				docUpdate.append("Status", Constants.INVOICE_STATUS.ERROR_CQT)
				.append("LDo", 
					new Document("MLoi", MLoi).append("MTa", MTa)
				)
				.append("DSLDKCNhan", DSLDKCNhan);
			}
			
		}else {
			MLoi = commons.getTextFromNodeXML((Element) xPath.evaluate("DLieu/TBao/DLTBao/DSLDKCNhan/LDo/MLoi", nodeTDiep, XPathConstants.NODE));
			MTa = commons.getTextFromNodeXML((Element) xPath.evaluate("DLieu/TBao/DLTBao/DSLDKCNhan/LDo/MTa", nodeTDiep, XPathConstants.NODE));
			
			if("".equals(MLoi)) {
				responseStatus = new MspResponseStatus(9999, "CQT chưa có thông báo kết quả trả về.");
				rsp.setResponseStatus(responseStatus);
				return rsp;
//				throw new Exception("CQT chưa có thông báo kết quả trả về.");
			}
			
			docUpdate.append("Status", Constants.INVOICE_STATUS.ERROR_CQT)
				.append("LDo", 
					new Document("MLoi", MLoi).append("MTa", MTa)
				);
			
		}
	
		 mongoClient = cfg.mongoClient();
			collection = mongoClient.getDatabase(cfg.dbName).getCollection("DMTKhai");
			collection.findOneAndUpdate(
					docFind, 
					new Document("$set", docUpdate), 
					options
				);			
			mongoClient.close();
			
			
		responseStatus = new MspResponseStatus(0, Constants.MAP_ERROR.get(0));
		rsp.setResponseStatus(responseStatus);
		return rsp;
	}

	@Transactional(rollbackFor = {Exception.class})
	@Override
	public MsgRsp crudV1(JSONRoot jsonRoot) throws Exception {
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
		String tenNnt = commons.getTextJsonNode(jsonData.at("/TenNnt")).trim().replaceAll("\\s+", " ");
		String mauSo = commons.getTextJsonNode(jsonData.at("/MauSo")).replaceAll("\\s", "");
		String ten = commons.getTextJsonNode(jsonData.at("/Ten")).trim().replaceAll("\\s+", " ");
		String hThuc = commons.getTextJsonNode(jsonData.at("/HThuc")).trim().replaceAll("\\s+", " ");
		String mst = commons.getTextJsonNode(jsonData.at("/Mst")).replaceAll("\\s", "");
		String tinhThanh = commons.getTextJsonNode(jsonData.at("/TinhThanh")).replaceAll("\\s", "");
		String cqtQLy = commons.getTextJsonNode(jsonData.at("/CqtQLy")).replaceAll("\\s", "");
		String nlHe = commons.getTextJsonNode(jsonData.at("/NLHe")).trim().replaceAll("\\s+", " ");
		String dcLHe = commons.getTextJsonNode(jsonData.at("/DCLHe")).trim().replaceAll("\\s+", " ");
		String dcCTDTu = commons.getTextJsonNode(jsonData.at("/DCCTDTu")).trim().replaceAll("\\s+", " ");
		String dtLHe = commons.getTextJsonNode(jsonData.at("/DTLHe")).trim().replaceAll("\\s+", " ");
		String nLap = commons.getTextJsonNode(jsonData.at("/NLap")).trim().replaceAll("\\s+", " ");
		
		String tnddpLuat = commons.getTextJsonNode(jsonData.at("/TNDDPLuat")).trim().replaceAll("\\s+", " ");
		String dtddpLuat = commons.getTextJsonNode(jsonData.at("/DTDDPLuat")).trim().replaceAll("\\s+", " ");
		String cccDan = commons.getTextJsonNode(jsonData.at("/CCCDan")).trim().replaceAll("\\s+", " ");
		String shChieu = commons.getTextJsonNode(jsonData.at("/SHChieu")).trim().replaceAll("\\s+", " ");
		String nsddpLuat = commons.getTextJsonNode(jsonData.at("/NSDDPLuat")).trim().replaceAll("\\s+", " ");
		String gTinh = commons.getTextJsonNode(jsonData.at("/GTinh")).trim().replaceAll("\\s+", " ");
		
		String htHDon = commons.getTextJsonNode(jsonData.at("/HTHDon")).trim().replaceAll("\\s+", " ");
		String pthuc = commons.getTextJsonNode(jsonData.at("/PThuc")).trim().replaceAll("\\s+", " ");
		String CMMTT = commons.getTextJsonNode(jsonData.at("/CMMTT")).trim().replaceAll("\\s+", " ");
		String lhdSDung_HDGTGT = commons.getTextJsonNode(jsonData.at("/LHDSDung_HDGTGT")).trim().replaceAll("\\s+", " ");
		String lhdsDung_HDBHang = commons.getTextJsonNode(jsonData.at("/LHDSDung_HDBHang")).trim().replaceAll("\\s+", " ");
		String lhdsDung_HDBTSCong = commons.getTextJsonNode(jsonData.at("/LHDSDung_HDBTSCong")).trim().replaceAll("\\s+", " ");
		String lhdsDung_HDBHDTQGia = commons.getTextJsonNode(jsonData.at("/LHDSDung_HDBHDTQGia")).trim().replaceAll("\\s+", " ");
		String lhdsDung_HDKhac = commons.getTextJsonNode(jsonData.at("/LHDSDung_HDKhac")).trim().replaceAll("\\s+", " ");
		String lhdsDung_CTu = commons.getTextJsonNode(jsonData.at("/LHDSDung_CTu")).trim().replaceAll("\\s+", " ");
		
		String lhdsDung_HDGTGTTHBLai = commons.getTextJsonNode(jsonData.at("/LHDSDung_HDGTGTTHBLai")).trim().replaceAll("\\s+", " ");
		String lhdsDung_HDBHTHBLai = commons.getTextJsonNode(jsonData.at("/LHDSDung_HDBHTHBLai")).trim().replaceAll("\\s+", " ");
		String lhdsDung_HDTMai = commons.getTextJsonNode(jsonData.at("/LHDSDung_HDTMai")).trim().replaceAll("\\s+", " ");
		
		List<Object> rowDSCTSSDung = new ArrayList<Object>();
		List<Object> rowDSTCGPhap = new ArrayList<Object>();
		List<Object> rowDSTCTNhan = new ArrayList<Object>();
		MsgRsp rsp = new MsgRsp(header);
		rsp.setMsgPage(page);
		MspResponseStatus responseStatus = null;
		
		List<Document> pipeline = null;
		Document docFind = null;
		Document docTmp = null;
		Document docUpsert = null;
		String taxCode = "";
		String fileNameXML = "";
		String pathDir = "";
		Path path = null;
		File file = null;
		
		ObjectId objectId = null;
		ObjectId objectIdUser = null;
		ObjectId objectIdTK = null;
		HashMap<String, Object> hO = null;
		
		FindOneAndUpdateOptions options = null;

		DocumentBuilderFactory dbf = null;
		DocumentBuilder db = null;
		org.w3c.dom.Document doc = null;
		Element root = null;
		
		Element elementContent = null;
		
		Element elementSubTmp = null;
		
		Element elementSubContent = null;
		Element elementTmp = null;
		boolean isSdaveFile = false;
		switch (actionCode) {
		case Constants.MSG_ACTION_CODE.CREATED:
			objectId = null;
			objectIdUser = null;
			try {
				objectId = new ObjectId(header.getIssuerId());
			}catch(Exception e) {}
			try {
				objectIdUser = new ObjectId(header.getUserId());
			}catch(Exception e) {}
			
			// Kiem tra da co chu ky so chua
			pipeline = new ArrayList<Document>();
			Date currentDate = new Date(); 
			docFind = new Document("IssuerId", header.getIssuerId())
					.append("IsDelete",new Document("$ne", true))
					.append("DSCTSSDung.DNgay", new Document("$gte", currentDate));  
					;
			pipeline.add(new Document("$match", docFind));
			
			MongoClient mongoClient = cfg.mongoClient();
			MongoCollection<Document> collection = mongoClient.getDatabase(cfg.dbName).getCollection("DMCTSo");
			docTmp = null;
			try {
				docTmp = collection.aggregate(pipeline).allowDiskUse(true).iterator().next();
			} catch (Exception e) {

			}
			mongoClient.close();
			if(null == docTmp) {
				responseStatus = new MspResponseStatus(9999, "Quý khách vui lòng vào menu Hệ Thống/Chứng thư số để thêm chữ ký số vào hệ thống trước khi Đăng ký tờ khai 01.");
				rsp.setResponseStatus(responseStatus);
				return rsp;
			}

			/*KIEM TRA THONG TIN KHACH HANG - USER - TINH THANH - CO QUAN THUE CO TON TAI KHONG*/
			docFind = new Document("_id", objectId).append("IsActive", true).append("IsDelete", new Document("$ne", true));
			pipeline = new ArrayList<Document>();
			pipeline.add(new Document("$match", docFind));
			pipeline.add(
				new Document("$lookup", 
					new Document("from", "Users")
					.append("pipeline", 
						Arrays.asList(
							new Document("$match", 
								new Document("IssuerId", header.getIssuerId()).append("_id", objectIdUser)
								.append("IsActive", true).append("IsDelete", new Document("$ne", true))
							),
							new Document("$project", new Document("_id", 1).append("UserName", 1).append("FullName", 1)),
							new Document("$limit", 1)
						)
					)
					.append("as", "UserInfo")
				)
			);
			pipeline.add(new Document("$unwind", new Document("path", "$UserInfo").append("preserveNullAndEmptyArrays", true)));
			pipeline.add(
				new Document("$lookup", 
					new Document("from", "DMTinhThanh")
					.append("pipeline", 
						Arrays.asList(
							new Document("$match", new Document("IsDelete", new Document("$ne", true)).append("code", commons.regexEscapeForMongoQuery(tinhThanh))),
							new Document("$project", new Document("_id", 0).append("code", 1).append("name", 1))
						)
					)
					.append("as", "DMTinhThanhInfo")
				)
			);
			pipeline.add(new Document("$unwind", new Document("path", "$DMTinhThanhInfo").append("preserveNullAndEmptyArrays", true)));
			pipeline.add(
				new Document("$lookup", 
					new Document("from", "DMChiCucThue")
					.append("pipeline", 
						Arrays.asList(
							new Document("$match", new Document("IsDelete", new Document("$ne", true)).append("code", commons.regexEscapeForMongoQuery(cqtQLy))),
							new Document("$project", new Document("_id", 0).append("code", 1).append("name", 1).append("note", 1))
						)
					)
					.append("as", "DMChiCucThueInfo")
				)
			);
			pipeline.add(new Document("$unwind", new Document("path", "$DMChiCucThueInfo").append("preserveNullAndEmptyArrays", true)));
				
			mongoClient = cfg.mongoClient();
			collection = mongoClient.getDatabase(cfg.dbName).getCollection("Issuer");
			docTmp = null;
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
			if(docTmp.get("UserInfo") == null) {
				responseStatus = new MspResponseStatus(9999, "Không tìm thấy thông tin người dùng.");
				rsp.setResponseStatus(responseStatus);
				return rsp;
			}
			
			if(docTmp.get("DMTinhThanhInfo") == null || docTmp.get("DMChiCucThueInfo") == null) {
				responseStatus = new MspResponseStatus(9999, "Vui lòng kiểm tra lại tỉnh/thành phố và cơ quan thuế.");
				rsp.setResponseStatus(responseStatus);
				return rsp;
			}
			
			taxCode = docTmp.getString("TaxCode");
			
			/*TAO FILE XML*/
			objectIdTK = new ObjectId();
			path = Paths.get(SystemParams.DIR_E_INVOICE_TKHAI, taxCode);
			pathDir = path.toString();
			file = path.toFile();
			if(!file.exists()) file.mkdirs();
			
			/*TAO FILE XML*/
			fileNameXML = objectIdTK.toString() + ".xml";
			
			dbf = DocumentBuilderFactory.newInstance();
			db = dbf.newDocumentBuilder();
			doc = db.newDocument();
			doc.setXmlStandalone(true);
			
			root = doc.createElement("TKhai");
			doc.appendChild(root);
			
			elementTmp = null;
			elementSubTmp = null;
			elementContent = doc.createElement("DLTKhai");
			elementContent.setAttribute("Id", "data");
			root.appendChild(elementContent);
			
			/*THONG TIN CHUNG TO KHAI*/
			elementSubContent = doc.createElement("TTChung");
			elementSubContent.appendChild(commons.createElementWithValue(doc, "PBan",  SystemParams.VERSION_XML_TOKHAI));
			elementSubContent.appendChild(commons.createElementWithValue(doc, "MSo", mauSo));
			elementSubContent.appendChild(commons.createElementWithValue(doc, "Ten", ten));
			elementSubContent.appendChild(commons.createElementWithValue(doc, "HThuc", hThuc));
			elementSubContent.appendChild(commons.createElementWithValue(doc, "TNNT", tenNnt));
			elementSubContent.appendChild(commons.createElementWithValue(doc, "MST", mst));
			elementSubContent.appendChild(commons.createElementWithValue(doc, "CQTQLy", docTmp.getEmbedded(Arrays.asList("DMChiCucThueInfo", "name"), "")));
			elementSubContent.appendChild(commons.createElementWithValue(doc, "MCQTQLy", cqtQLy));
			elementSubContent.appendChild(commons.createElementWithValue(doc, "TNDDPLuat", tnddpLuat));
			elementSubContent.appendChild(commons.createElementWithValue(doc, "DTDDPLuat", dtddpLuat));
			elementSubContent.appendChild(commons.createElementWithValue(doc, "CCCDan", cccDan));
			elementSubContent.appendChild(commons.createElementWithValue(doc, "SHChieu", shChieu));
			elementSubContent.appendChild(commons.createElementWithValue(doc, "NSDDPLuat", commons.convertLocalDateTimeStringToString(nsddpLuat, Constants.FORMAT_DATE.FORMAT_DATE_WEB, Constants.FORMAT_DATE.FORMAT_DATE_EINVOICE)));
			elementSubContent.appendChild(commons.createElementWithValue(doc, "GTinh", gTinh));
			elementSubContent.appendChild(commons.createElementWithValue(doc, "DCLHe", dcLHe));
			elementSubContent.appendChild(commons.createElementWithValue(doc, "DCTDTu", dcCTDTu));
			elementSubContent.appendChild(commons.createElementWithValue(doc, "NLHe", nlHe));
			elementSubContent.appendChild(commons.createElementWithValue(doc, "DTLHe", dtLHe));
			elementSubContent.appendChild(commons.createElementWithValue(doc, "DDanh", docTmp.getEmbedded(Arrays.asList("DMTinhThanhInfo", "name"), "")));
			elementSubContent.appendChild(commons.createElementWithValue(doc, "NLap", commons.convertLocalDateTimeStringToString(nLap, Constants.FORMAT_DATE.FORMAT_DATE_WEB, Constants.FORMAT_DATE.FORMAT_DATE_EINVOICE)));
			elementContent.appendChild(elementSubContent);
			
			/*NOI DUNG CHI TIET TO KHAI*/
			elementSubContent = doc.createElement("NDTKhai");
			/*HINH THUC HOA DON AP DUNG*/
			elementTmp = doc.createElement("HTHDon");
			elementTmp.appendChild(commons.createElementWithValue(doc, "CMa", "CMa".equals(htHDon)? "1": "0"));
			elementTmp.appendChild(commons.createElementWithValue(doc, "CMTMTTien", "on".equals(CMMTT)? "1": "0"));
			elementTmp.appendChild(commons.createElementWithValue(doc, "KCMa", "KCMa".equals(htHDon)? "1": "0"));
			elementSubContent.appendChild(elementTmp);
			/*HINH THUC GUI DU LIEU HDDT - FIX GIA TRI*/
			elementTmp = doc.createElement("HTGDLHDDT");
			elementTmp.appendChild(commons.createElementWithValue(doc, "NNTDBKKhan", "0"));
			elementTmp.appendChild(commons.createElementWithValue(doc, "NNTKTDNUBND", "0"));
			elementTmp.appendChild(commons.createElementWithValue(doc, "CQXLTSCong", "0"));
			elementTmp.appendChild(commons.createElementWithValue(doc, "CDLTTDCQT", "0"));
			elementTmp.appendChild(commons.createElementWithValue(doc, "CDLQTCTN", "0"));
			elementSubContent.appendChild(elementTmp);
			/*PHUONG THUC CHUYEN DU LIEU*/
			elementTmp = doc.createElement("PThuc");
			elementTmp.appendChild(commons.createElementWithValue(doc, "CDDu", "CDDu".equals(pthuc)? "1": "0"));
			elementTmp.appendChild(commons.createElementWithValue(doc, "CBTHop", "CBTHop".equals(pthuc)? "1": "0"));
			elementSubContent.appendChild(elementTmp);
			/*LOAI HD SU DUNG*/
			elementTmp = doc.createElement("LHDSDung");
			elementTmp.appendChild(commons.createElementWithValue(doc, "HDGTGT", "on".equals(lhdSDung_HDGTGT)? "1": "0"));
			elementTmp.appendChild(commons.createElementWithValue(doc, "HDGTGTTHBLai", "on".equals(lhdsDung_HDGTGTTHBLai)? "1": "0"));
			elementTmp.appendChild(commons.createElementWithValue(doc, "HDBHang", "on".equals(lhdsDung_HDBHang)? "1": "0"));
			elementTmp.appendChild(commons.createElementWithValue(doc, "HDBHTHBLai", "on".equals(lhdsDung_HDBHTHBLai)? "1": "0"));
			elementTmp.appendChild(commons.createElementWithValue(doc, "HDTMai", "on".equals(lhdsDung_HDTMai)? "1": "0"));
			elementTmp.appendChild(commons.createElementWithValue(doc, "HDBTSCong", "on".equals(lhdsDung_HDBTSCong)? "1": "0"));
			elementTmp.appendChild(commons.createElementWithValue(doc, "HDBHDTQGia", "on".equals(lhdsDung_HDBHDTQGia)? "1": "0"));
			elementTmp.appendChild(commons.createElementWithValue(doc, "HDKhac", "on".equals(lhdsDung_HDKhac)? "1": "0"));
			elementTmp.appendChild(commons.createElementWithValue(doc, "CTu", "on".equals(lhdsDung_CTu)? "1": "0"));
			elementSubContent.appendChild(elementTmp);
			/*DANH SACH CHUNG THU SO*/
			elementTmp = doc.createElement("DSCTSSDung");
			if(!jsonData.at("/DSCTSSDung").isMissingNode()) {
				for(JsonNode o: jsonData.at("/DSCTSSDung")) {
					String tn = commons.getTextJsonNode(o.at("/TNgay"));
					String[] words2 = tn.split(" ");
					String[] words = words2[1].split(":");
					String h =  words[0];
					if(h.length() < 2)
					{
						h = "0"+h;
					}
					String m =  words[1];
					if(m.length() < 2)
					{
						m = "0"+m;
					}
					 String s = words[2];
					 if(s.length() < 2)
						{
							s = "0"+s;
						}
					 tn = words2[0]+" "+h+":"+m+":"+s;
					String dn = commons.getTextJsonNode(o.at("/DNgay"));
					String[] words21 = dn.split(" ");
					String[] words1 = words21[1].split(":");
					String h1 =  words1[0];
					if(h1.length() < 2)
					{
						h1 = "0"+h1;
					}
					String m1 =  words1[1];
					if(m1.length() < 2)
					{
						m1 = "0"+m1;
					}
					 String s1 = words1[2];
					 if(s1.length() < 2)
						{
							s1 = "0"+s1;
						}
					 dn = words21[0]+" "+h1+":"+m1+":"+s1;
					
					 DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
					 LocalDateTime dateTime = LocalDateTime.parse(tn, formatter);
					 DateTimeFormatter formatter1 = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
					 LocalDateTime dateTime1 = LocalDateTime.parse(dn, formatter1);
					elementSubTmp = doc.createElement("CTS");
					elementSubTmp.appendChild(commons.createElementWithValue(doc, "TTChuc", commons.getTextJsonNode(o.at("/TTChuc"))));
					elementSubTmp.appendChild(commons.createElementWithValue(doc, "Seri", commons.getTextJsonNode(o.at("/Seri"))));
					elementSubTmp.appendChild(commons.createElementWithValue(doc, "TNgay", commons.convertLocalDateTimeToString(dateTime, Constants.FORMAT_DATE.FORMAT_DATETIME_EINVOICE)));
					elementSubTmp.appendChild(commons.createElementWithValue(doc, "DNgay", commons.convertLocalDateTimeToString(dateTime1, Constants.FORMAT_DATE.FORMAT_DATETIME_EINVOICE)));
				
					elementSubTmp.appendChild(commons.createElementWithValue(doc, "HThuc", commons.getTextJsonNode(o.at("/HThuc"))));
					elementTmp.appendChild(elementSubTmp);
					/*
					 * <TNgay>2021-11-04T00:00:00</TNgay> <DNgay>2022-11-04T23:59:00</DNgay>
					 */
					
					
					/*
					 * "TNgay" : ISODate("2021-11-04T00:00:00.000Z"), "DNgay" :
					 * ISODate("2022-11-04T23:59:00.000Z"),
					 */
					hO = new LinkedHashMap<String, Object>();
					hO.put("TTChuc", commons.getTextJsonNode(o.at("/TTChuc")));
					hO.put("Seri", commons.getTextJsonNode(o.at("/Seri")));
					hO.put("TNgay", dateTime);
					hO.put("DNgay",dateTime1);
					hO.put("HThuc", commons.getTextJsonNode(o.at("/HThuc")));
					rowDSCTSSDung.add(hO);
					
				}
			}
			elementSubContent.appendChild(elementTmp);
			
			/*TO CHUC CUNG CAP DICH VU*/
			elementTmp = doc.createElement("TTTCGP");
			if (!jsonData.at("/DSTCGPhap").isMissingNode()) {
				for (JsonNode data : jsonData.at("/DSTCGPhap")) {
					String ttcgp = commons.getTextJsonNode(data.at("/TTCGPhap"));
					String msttcgp = commons.getTextJsonNode(data.at("/MSTTCGPhap"));
					if (ttcgp.trim().isEmpty() || msttcgp.trim().isEmpty()) continue;
					elementSubTmp = doc.createElement("TCGP");
					elementSubTmp.appendChild(commons.createElementWithValue(doc, "TTCGP", ttcgp));
					elementSubTmp.appendChild(commons.createElementWithValue(doc, "MSTTCGP", msttcgp));
					elementSubTmp.appendChild(commons.createElementWithValue(doc, "TNgay", commons.convertLocalDateTimeToString(LocalDate.now(), "yyyy-MM-dd")));
					elementTmp.appendChild(elementSubTmp);
					
					hO = new LinkedHashMap<String, Object>();
					hO.put("MSTTCGPhap", msttcgp);
					hO.put("TTCGPhap", ttcgp);
					rowDSTCGPhap.add(hO);
				}
			}
			elementSubContent.appendChild(elementTmp);
			
			/*TO CHUC TRUYEN NHAN*/
			elementTmp = doc.createElement("TTTCTN");
			if (!jsonData.at("/DSTCTNhan").isMissingNode()) {
				for (JsonNode data : jsonData.at("/DSTCTNhan")) {
					String ttctn = commons.getTextJsonNode(data.at("/TTCTNhan"));
					String msttctn = commons.getTextJsonNode(data.at("/MSTTCTNhan"));
					if (ttctn.trim().isEmpty() || msttctn.trim().isEmpty()) continue;
					elementSubTmp = doc.createElement("TCTN");
					elementSubTmp.appendChild(commons.createElementWithValue(doc, "TTCTN", ttctn));
					elementSubTmp.appendChild(commons.createElementWithValue(doc, "MSTTCTN", msttctn));
					elementSubTmp.appendChild(commons.createElementWithValue(doc, "TNgay", commons.convertLocalDateTimeToString(LocalDate.now(), "yyyy-MM-dd")));
					elementTmp.appendChild(elementSubTmp);
					
					hO = new LinkedHashMap<String, Object>();
					hO.put("MSTTCTNhan", msttctn);
					hO.put("TTCTNhan", ttctn);
					rowDSTCTNhan.add(hO);
				}
			}
			
			elementSubContent.appendChild(elementTmp);
			
			elementContent.appendChild(elementSubContent);
			/*END - TAO FILE XML*/
			 isSdaveFile = commons.docW3cToFile(doc, pathDir, fileNameXML);
			if(!isSdaveFile) {
				throw new Exception("Lưu dữ liệu không thành công.");
			}
			
			String MTDiep = SystemParams.MSTTCGP + commons.csRandomAlphaNumbericString(46 - SystemParams.MSTTCGP.length()).toUpperCase();
			/*LUU DU LIEU*/
			docUpsert = new Document("_id", objectIdTK)
				.append("IssuerId", header.getIssuerId())
				.append("MTDiep", MTDiep)
				.append("TenNnt", tenNnt)
				.append("MSo", mauSo)
				.append("Ten", ten)
				.append("HThuc", hThuc)
				.append("MST", mst)
				.append("TinhThanhInfo", docTmp.get("DMTinhThanhInfo"))
				.append("ChiCucThueInfo", docTmp.get("DMChiCucThueInfo"))
				.append("NLHe", nlHe)
				.append("DCLHe", dcLHe)
				.append("DCTDTu", dcCTDTu)
				.append("DTLHe", dtLHe)
				.append("NLap", commons.convertStringToLocalDate(nLap, Constants.FORMAT_DATE.FORMAT_DATE_WEB))		
				.append("TNDDPLuat", tnddpLuat)
				.append("DTDDPLuat", dtddpLuat)
				.append("CCCDan", cccDan)
				.append("SHChieu", shChieu)
				.append("NSDDPLuat", commons.convertStringToLocalDate(nsddpLuat, Constants.FORMAT_DATE.FORMAT_DATE_WEB))
				.append("GTinh", gTinh)
				.append("HTHDon", htHDon)
				.append("PThuc", pthuc)		
				.append("CMMTTien",  "on".equals(CMMTT)? "1": "0")		
				.append("LHDSDung", 
					new Document("HDGTGT", "on".equals(lhdSDung_HDGTGT)? "1": "0")
						.append("HDGTGTTHBLai", "on".equals(lhdsDung_HDGTGTTHBLai)? "1": "0")
						.append("HDBHang", "on".equals(lhdsDung_HDBHang)? "1": "0")
						.append("HDBHTHBLai", "on".equals(lhdsDung_HDBHTHBLai)? "1": "0")
						.append("HDTMai", "on".equals(lhdsDung_HDTMai)? "1": "0")
						.append("HDBTSCong", "on".equals(lhdsDung_HDBTSCong)? "1": "0")
						.append("HDBHDTQGia", "on".equals(lhdsDung_HDBHDTQGia)? "1": "0")
						.append("HDKhac", "on".equals(lhdsDung_HDKhac)? "1": "0")
						.append("CTu", "on".equals(lhdsDung_CTu)? "1": "0")
				)
				.append("DSCTSSDung", rowDSCTSSDung)
				.append("DSTCGPhap", rowDSTCGPhap)
				.append("DSTCTNhan", rowDSTCTNhan)
				.append("Status", Constants.INVOICE_STATUS.TK_CREATED)
				.append("IsDelete", false)
				.append("Dir", pathDir)
				.append("FileNameXML", fileNameXML)
				.append("InfoCreated", 
					new Document("CreateDate", LocalDateTime.now())
					.append("CreateUserID", header.getUserId())
					.append("CreateUserName", header.getUserName())
					.append("CreateUserFullName", header.getUserFullName())
				);
			/*END - LUU DU LIEU*/
		
			mongoClient = cfg.mongoClient();
			collection = mongoClient.getDatabase(cfg.dbName).getCollection("DMTKhai");
			collection.insertOne(docUpsert);
			mongoClient.close();
			
			
			DateTimeFormatter format_time = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
			LocalDateTime time_dem = LocalDateTime.now();
			String time = time_dem.format(format_time);
			String name_company = removeAccent(header.getUserFullName());
			System.out.println(time + name_company + " Vua tao to khai");
			responseStatus = new MspResponseStatus(0, "SUCCESS");
			rsp.setResponseStatus(responseStatus);			
			return rsp;
		case Constants.MSG_ACTION_CODE.COPY:
			objectId = null;
			objectIdUser = null;
			try {
				objectId = new ObjectId(header.getIssuerId());
			}catch(Exception e) {}
			try {
				objectIdUser = new ObjectId(header.getUserId());
			}catch(Exception e) {}
			
			/*KIEM TRA THONG TIN KHACH HANG - USER - TINH THANH - CO QUAN THUE CO TON TAI KHONG*/
			docFind = new Document("_id", objectId).append("IsActive", true).append("IsDelete", new Document("$ne", true));
			pipeline = new ArrayList<Document>();
			pipeline.add(new Document("$match", docFind));
			pipeline.add(
				new Document("$lookup", 
					new Document("from", "Users")
					.append("pipeline", 
						Arrays.asList(
							new Document("$match", 
								new Document("IssuerId", header.getIssuerId()).append("_id", objectIdUser)
								.append("IsActive", true).append("IsDelete", new Document("$ne", true))
							),
							new Document("$project", new Document("_id", 1).append("UserName", 1).append("FullName", 1)),
							new Document("$limit", 1)
						)
					)
					.append("as", "UserInfo")
				)
			);
			pipeline.add(new Document("$unwind", new Document("path", "$UserInfo").append("preserveNullAndEmptyArrays", true)));
			pipeline.add(
				new Document("$lookup", 
					new Document("from", "DMTinhThanh")
					.append("pipeline", 
						Arrays.asList(
							new Document("$match", new Document("IsDelete", new Document("$ne", true)).append("code", commons.regexEscapeForMongoQuery(tinhThanh))),
							new Document("$project", new Document("_id", 0).append("code", 1).append("name", 1))
						)
					)
					.append("as", "DMTinhThanhInfo")
				)
			);
			pipeline.add(new Document("$unwind", new Document("path", "$DMTinhThanhInfo").append("preserveNullAndEmptyArrays", true)));
			pipeline.add(
				new Document("$lookup", 
					new Document("from", "DMChiCucThue")
					.append("pipeline", 
						Arrays.asList(
							new Document("$match", new Document("IsDelete", new Document("$ne", true)).append("code", commons.regexEscapeForMongoQuery(cqtQLy))),
							new Document("$project", new Document("_id", 0).append("code", 1).append("name", 1).append("note", 1))
						)
					)
					.append("as", "DMChiCucThueInfo")
				)
			);
			pipeline.add(new Document("$unwind", new Document("path", "$DMChiCucThueInfo").append("preserveNullAndEmptyArrays", true)));
				
			mongoClient = cfg.mongoClient();
			collection = mongoClient.getDatabase(cfg.dbName).getCollection("Issuer");
			docTmp = null;
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
			if(docTmp.get("UserInfo") == null) {
				responseStatus = new MspResponseStatus(9999, "Không tìm thấy thông tin người dùng.");
				rsp.setResponseStatus(responseStatus);
				return rsp;
			}
			
			if(docTmp.get("DMTinhThanhInfo") == null || docTmp.get("DMChiCucThueInfo") == null) {
				responseStatus = new MspResponseStatus(9999, "Vui lòng kiểm tra lại tỉnh/thành phố và cơ quan thuế.");
				rsp.setResponseStatus(responseStatus);
				return rsp;
			}
			
			taxCode = docTmp.getString("TaxCode");
			
			/*TAO FILE XML*/
			objectIdTK = new ObjectId();
			path = Paths.get(SystemParams.DIR_E_INVOICE_TKHAI, taxCode);
			pathDir = path.toString();
			file = path.toFile();
			if(!file.exists()) file.mkdirs();
			
			/*TAO FILE XML*/
			fileNameXML = objectIdTK.toString() + ".xml";

			dbf = DocumentBuilderFactory.newInstance();
			db = dbf.newDocumentBuilder();
			doc = db.newDocument();
			doc.setXmlStandalone(true);
			
			root = doc.createElement("TKhai");
			doc.appendChild(root);
			
			 elementTmp = null;
			 elementSubTmp = null;
			 elementContent = doc.createElement("DLTKhai");
			elementContent.setAttribute("Id", "data");
			root.appendChild(elementContent);
			
			/*THONG TIN CHUNG TO KHAI*/
			elementSubContent = doc.createElement("TTChung");
			elementSubContent.appendChild(commons.createElementWithValue(doc, "PBan",  SystemParams.VERSION_XML_TOKHAI));
			elementSubContent.appendChild(commons.createElementWithValue(doc, "MSo", mauSo));
			elementSubContent.appendChild(commons.createElementWithValue(doc, "Ten", ten));
			elementSubContent.appendChild(commons.createElementWithValue(doc, "HThuc", hThuc));
			elementSubContent.appendChild(commons.createElementWithValue(doc, "TNNT", tenNnt));
			elementSubContent.appendChild(commons.createElementWithValue(doc, "MST", mst));
			elementSubContent.appendChild(commons.createElementWithValue(doc, "CQTQLy", docTmp.getEmbedded(Arrays.asList("DMChiCucThueInfo", "name"), "")));
			elementSubContent.appendChild(commons.createElementWithValue(doc, "MCQTQLy", cqtQLy));
			elementSubContent.appendChild(commons.createElementWithValue(doc, "TNDDPLuat", tnddpLuat));
			elementSubContent.appendChild(commons.createElementWithValue(doc, "DTDDPLuat", dtddpLuat));
			elementSubContent.appendChild(commons.createElementWithValue(doc, "CCCDan", cccDan));
			elementSubContent.appendChild(commons.createElementWithValue(doc, "SHChieu", shChieu));
			elementSubContent.appendChild(commons.createElementWithValue(doc, "NSDDPLuat", commons.convertLocalDateTimeStringToString(nsddpLuat, Constants.FORMAT_DATE.FORMAT_DATE_WEB, Constants.FORMAT_DATE.FORMAT_DATE_EINVOICE)));
			elementSubContent.appendChild(commons.createElementWithValue(doc, "GTinh", gTinh));
			elementSubContent.appendChild(commons.createElementWithValue(doc, "DCLHe", dcLHe));
			elementSubContent.appendChild(commons.createElementWithValue(doc, "DCTDTu", dcCTDTu));
			elementSubContent.appendChild(commons.createElementWithValue(doc, "NLHe", nlHe));
			elementSubContent.appendChild(commons.createElementWithValue(doc, "DTLHe", dtLHe));
			elementSubContent.appendChild(commons.createElementWithValue(doc, "DDanh", docTmp.getEmbedded(Arrays.asList("DMTinhThanhInfo", "name"), "")));
			elementSubContent.appendChild(commons.createElementWithValue(doc, "NLap", commons.convertLocalDateTimeStringToString(nLap, Constants.FORMAT_DATE.FORMAT_DATE_WEB, Constants.FORMAT_DATE.FORMAT_DATE_EINVOICE)));
			elementContent.appendChild(elementSubContent);
			
			/*NOI DUNG CHI TIET TO KHAI*/
			elementSubContent = doc.createElement("NDTKhai");
			/*HINH THUC HOA DON AP DUNG*/
			elementTmp = doc.createElement("HTHDon");
			elementTmp.appendChild(commons.createElementWithValue(doc, "CMa", "CMa".equals(htHDon)? "1": "0"));
			elementTmp.appendChild(commons.createElementWithValue(doc, "CMTMTTien", "on".equals(CMMTT)? "1": "0"));
			elementTmp.appendChild(commons.createElementWithValue(doc, "KCMa", "KCMa".equals(htHDon)? "1": "0"));
			elementSubContent.appendChild(elementTmp);
			/*HINH THUC GUI DU LIEU HDDT - FIX GIA TRI*/
			elementTmp = doc.createElement("HTGDLHDDT");
			elementTmp.appendChild(commons.createElementWithValue(doc, "NNTDBKKhan", "0"));
			elementTmp.appendChild(commons.createElementWithValue(doc, "NNTKTDNUBND", "0"));
			elementTmp.appendChild(commons.createElementWithValue(doc, "CQXLTSCong", "0"));
			elementTmp.appendChild(commons.createElementWithValue(doc, "CDLTTDCQT", "0"));
			elementTmp.appendChild(commons.createElementWithValue(doc, "CDLQTCTN", "0"));
			elementSubContent.appendChild(elementTmp);
			/*PHUONG THUC CHUYEN DU LIEU*/
			elementTmp = doc.createElement("PThuc");
			elementTmp.appendChild(commons.createElementWithValue(doc, "CDDu", "CDDu".equals(pthuc)? "1": "0"));
			elementTmp.appendChild(commons.createElementWithValue(doc, "CBTHop", "CBTHop".equals(pthuc)? "1": "0"));
			elementSubContent.appendChild(elementTmp);
			/*LOAI HD SU DUNG*/
			elementTmp = doc.createElement("LHDSDung");
			elementTmp.appendChild(commons.createElementWithValue(doc, "HDGTGT", "on".equals(lhdSDung_HDGTGT)? "1": "0"));
			elementTmp.appendChild(commons.createElementWithValue(doc, "HDGTGTTHBLai", "on".equals(lhdsDung_HDGTGTTHBLai)? "1": "0"));
			elementTmp.appendChild(commons.createElementWithValue(doc, "HDBHang", "on".equals(lhdsDung_HDBHang)? "1": "0"));
			elementTmp.appendChild(commons.createElementWithValue(doc, "HDBHTHBLai", "on".equals(lhdsDung_HDBHTHBLai)? "1": "0"));
			elementTmp.appendChild(commons.createElementWithValue(doc, "HDTMai", "on".equals(lhdsDung_HDTMai)? "1": "0"));
			elementTmp.appendChild(commons.createElementWithValue(doc, "HDBTSCong", "on".equals(lhdsDung_HDBTSCong)? "1": "0"));
			elementTmp.appendChild(commons.createElementWithValue(doc, "HDBHDTQGia", "on".equals(lhdsDung_HDBHDTQGia)? "1": "0"));
			elementTmp.appendChild(commons.createElementWithValue(doc, "HDKhac", "on".equals(lhdsDung_HDKhac)? "1": "0"));
			elementTmp.appendChild(commons.createElementWithValue(doc, "CTu", "on".equals(lhdsDung_CTu)? "1": "0"));
			elementSubContent.appendChild(elementTmp);
			/*DANH SACH CHUNG THU SO*/
			elementTmp = doc.createElement("DSCTSSDung");
			if(!jsonData.at("/DSCTSSDung").isMissingNode()) {
				for(JsonNode o: jsonData.at("/DSCTSSDung")) {
					String tn = commons.getTextJsonNode(o.at("/TNgay"));
					String[] words2 = tn.split(" ");
					String[] words = words2[1].split(":");
					String h =  words[0];
					if(h.length() < 2)
					{
						h = "0"+h;
					}
					String m =  words[1];
					if(m.length() < 2)
					{
						m = "0"+m;
					}
					 String s = words[2];
					 if(s.length() < 2)
						{
							s = "0"+s;
						}
					 tn = words2[0]+" "+h+":"+m+":"+s;
					String dn = commons.getTextJsonNode(o.at("/DNgay"));
					String[] words21 = dn.split(" ");
					String[] words1 = words21[1].split(":");
					String h1 =  words1[0];
					if(h1.length() < 2)
					{
						h1 = "0"+h1;
					}
					String m1 =  words1[1];
					if(m1.length() < 2)
					{
						m1 = "0"+m1;
					}
					 String s1 = words1[2];
					 if(s1.length() < 2)
						{
							s1 = "0"+s1;
						}
					 dn = words21[0]+" "+h1+":"+m1+":"+s1;
					
					 DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
					 LocalDateTime dateTime = LocalDateTime.parse(tn, formatter);
					 DateTimeFormatter formatter1 = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
					 LocalDateTime dateTime1 = LocalDateTime.parse(dn, formatter1);
					elementSubTmp = doc.createElement("CTS");
					elementSubTmp.appendChild(commons.createElementWithValue(doc, "TTChuc", commons.getTextJsonNode(o.at("/TTChuc"))));
					elementSubTmp.appendChild(commons.createElementWithValue(doc, "Seri", commons.getTextJsonNode(o.at("/Seri"))));
					elementSubTmp.appendChild(commons.createElementWithValue(doc, "TNgay", commons.convertLocalDateTimeToString(dateTime, Constants.FORMAT_DATE.FORMAT_DATETIME_EINVOICE)));
					elementSubTmp.appendChild(commons.createElementWithValue(doc, "DNgay", commons.convertLocalDateTimeToString(dateTime1, Constants.FORMAT_DATE.FORMAT_DATETIME_EINVOICE)));
				
					elementSubTmp.appendChild(commons.createElementWithValue(doc, "HThuc", commons.getTextJsonNode(o.at("/HThuc"))));
					elementTmp.appendChild(elementSubTmp);
					/*
					 * <TNgay>2021-11-04T00:00:00</TNgay> <DNgay>2022-11-04T23:59:00</DNgay>
					 */
					
					
					/*
					 * "TNgay" : ISODate("2021-11-04T00:00:00.000Z"), "DNgay" :
					 * ISODate("2022-11-04T23:59:00.000Z"),
					 */
					hO = new LinkedHashMap<String, Object>();
					hO.put("TTChuc", commons.getTextJsonNode(o.at("/TTChuc")));
					hO.put("Seri", commons.getTextJsonNode(o.at("/Seri")));
					hO.put("TNgay", dateTime);
					hO.put("DNgay",dateTime1);
					hO.put("HThuc", commons.getTextJsonNode(o.at("/HThuc")));
					rowDSCTSSDung.add(hO);
					
				}
			}
			elementSubContent.appendChild(elementTmp);
			
			/*TO CHUC CUNG CAP DICH VU*/
			elementTmp = doc.createElement("TTTCGP");
			if (!jsonData.at("/DSTCGPhap").isMissingNode()) {
				for (JsonNode data : jsonData.at("/DSTCGPhap")) {
					String ttcgp = commons.getTextJsonNode(data.at("/TTCGPhap"));
					String msttcgp = commons.getTextJsonNode(data.at("/MSTTCGPhap"));
					if (ttcgp.trim().isEmpty() || msttcgp.trim().isEmpty()) continue;
					elementSubTmp = doc.createElement("TCGP");
					elementSubTmp.appendChild(commons.createElementWithValue(doc, "TTCGP", ttcgp));
					elementSubTmp.appendChild(commons.createElementWithValue(doc, "MSTTCGP", msttcgp));
					elementSubTmp.appendChild(commons.createElementWithValue(doc, "TNgay", commons.convertLocalDateTimeToString(LocalDate.now(), "yyyy-MM-dd")));
					elementTmp.appendChild(elementSubTmp);
					
					hO = new LinkedHashMap<String, Object>();
					hO.put("MSTTCGPhap", msttcgp);
					hO.put("TTCGPhap", ttcgp);
					rowDSTCGPhap.add(hO);
				}
			}
			elementSubContent.appendChild(elementTmp);
			
			/*TO CHUC TRUYEN NHAN*/
			elementTmp = doc.createElement("TTTCTN");
			if (!jsonData.at("/DSTCTNhan").isMissingNode()) {
				for (JsonNode data : jsonData.at("/DSTCTNhan")) {
					String ttctn = commons.getTextJsonNode(data.at("/TTCTNhan"));
					String msttctn = commons.getTextJsonNode(data.at("/MSTTCTNhan"));
					if (ttctn.trim().isEmpty() || msttctn.trim().isEmpty()) continue;
					elementSubTmp = doc.createElement("TCTN");
					elementSubTmp.appendChild(commons.createElementWithValue(doc, "TTCTN", ttctn));
					elementSubTmp.appendChild(commons.createElementWithValue(doc, "MSTTCTN", msttctn));
					elementSubTmp.appendChild(commons.createElementWithValue(doc, "TNgay", commons.convertLocalDateTimeToString(LocalDate.now(), "yyyy-MM-dd")));
					elementTmp.appendChild(elementSubTmp);
					
					hO = new LinkedHashMap<String, Object>();
					hO.put("MSTTCTNhan", msttctn);
					hO.put("TTCTNhan", ttctn);
					rowDSTCTNhan.add(hO);
				}
			}
			
			elementSubContent.appendChild(elementTmp);
			
			elementContent.appendChild(elementSubContent);
			/*END - TAO FILE XML*/
			 isSdaveFile = commons.docW3cToFile(doc, pathDir, fileNameXML);
			if(!isSdaveFile) {
				throw new Exception("Lưu dữ liệu không thành công.");
			}
			
			MTDiep = SystemParams.MSTTCGP + commons.csRandomAlphaNumbericString(46 - SystemParams.MSTTCGP.length()).toUpperCase();
			/*LUU DU LIEU*/
			docUpsert = new Document("_id", objectIdTK)
				.append("IssuerId", header.getIssuerId())
				.append("MTDiep", MTDiep)
				.append("TenNnt", tenNnt)
				.append("MSo", mauSo)
				.append("Ten", ten)
				.append("HThuc", hThuc)
				.append("MST", mst)
				.append("TinhThanhInfo", docTmp.get("DMTinhThanhInfo"))
				.append("ChiCucThueInfo", docTmp.get("DMChiCucThueInfo"))
				.append("NLHe", nlHe)
				.append("DCLHe", dcLHe)
				.append("DCTDTu", dcCTDTu)
				.append("DTLHe", dtLHe)
				.append("NLap", commons.convertStringToLocalDate(nLap, Constants.FORMAT_DATE.FORMAT_DATE_WEB))	
				.append("TNDDPLuat", tnddpLuat)
				.append("DTDDPLuat", dtddpLuat)
				.append("CCCDan", cccDan)
				.append("SHChieu", shChieu)
				.append("NSDDPLuat", commons.convertStringToLocalDate(nsddpLuat, Constants.FORMAT_DATE.FORMAT_DATE_WEB))
				.append("GTinh", gTinh)
				.append("HTHDon", htHDon)
				.append("PThuc", pthuc)		
				.append("CMMTTien",  "on".equals(CMMTT)? "1": "0")		
				
				.append("LHDSDung", 
					new Document("HDGTGT", "on".equals(lhdSDung_HDGTGT)? "1": "0")
						.append("HDGTGTTHBLai", "on".equals(lhdsDung_HDGTGTTHBLai)? "1": "0")
						.append("HDBHang", "on".equals(lhdsDung_HDBHang)? "1": "0")
						.append("HDBHTHBLai", "on".equals(lhdsDung_HDBHTHBLai)? "1": "0")
						.append("HDTMai", "on".equals(lhdsDung_HDTMai)? "1": "0")
						.append("HDBTSCong", "on".equals(lhdsDung_HDBTSCong)? "1": "0")
						.append("HDBHDTQGia", "on".equals(lhdsDung_HDBHDTQGia)? "1": "0")
						.append("HDKhac", "on".equals(lhdsDung_HDKhac)? "1": "0")
						.append("CTu", "on".equals(lhdsDung_CTu)? "1": "0")
				)
				.append("DSCTSSDung", rowDSCTSSDung)
				.append("DSTCGPhap", rowDSTCGPhap)
				.append("DSTCTNhan", rowDSTCTNhan)
				.append("Status", Constants.INVOICE_STATUS.TK_CREATED)
				.append("IsDelete", false)
				.append("Dir", pathDir)
				.append("FileNameXML", fileNameXML)
				.append("InfoCreated", 
					new Document("CreateDate", LocalDateTime.now())
					.append("CreateUserID", header.getUserId())
					.append("CreateUserName", header.getUserName())
					.append("CreateUserFullName", header.getUserFullName())
				);
			/*END - LUU DU LIEU*/
		
			mongoClient = cfg.mongoClient();
			collection = mongoClient.getDatabase(cfg.dbName).getCollection("DMTKhai");
			collection.insertOne(docUpsert);
			mongoClient.close();
			
			format_time = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
			time_dem  = LocalDateTime.now();
			time = time_dem.format(format_time);
			name_company = removeAccent(header.getUserFullName());
			System.out.println(time +name_company+" Vua copy to khai");
			responseStatus = new MspResponseStatus(0, "SUCCESS");
			rsp.setResponseStatus(responseStatus);				
			return rsp;
		case Constants.MSG_ACTION_CODE.MODIFY:
			objectId = null;
			objectIdUser = null;
			try {
				objectId = new ObjectId(header.getIssuerId());
			}catch(Exception e) {}
			try {
				objectIdUser = new ObjectId(header.getUserId());
			}catch(Exception e) {}
			try {
				objectIdTK = new ObjectId(_id);
			}catch(Exception e) {}
			
			/*KIEM TRA THONG TIN KHACH HANG - USER - TINH THANH - CO QUAN THUE CO TON TAI KHONG*/
			docFind = new Document("_id", objectId).append("IsActive", true).append("IsDelete", new Document("$ne", true));
			pipeline = new ArrayList<Document>();
			pipeline.add(new Document("$match", docFind));
			pipeline.add(
				new Document("$lookup", 
					new Document("from", "Users")
					.append("pipeline", 
						Arrays.asList(
							new Document("$match", 
								new Document("IssuerId", header.getIssuerId()).append("_id", objectIdUser)
								.append("IsActive", true).append("IsDelete", new Document("$ne", true))
							),
							new Document("$project", new Document("_id", 1).append("UserName", 1).append("FullName", 1)),
							new Document("$limit", 1)
						)
					)
					.append("as", "UserInfo")
				)
			);
			pipeline.add(new Document("$unwind", new Document("path", "$UserInfo").append("preserveNullAndEmptyArrays", true)));
			pipeline.add(
				new Document("$lookup", 
					new Document("from", "DMTinhThanh")
					.append("pipeline", 
						Arrays.asList(
							new Document("$match", new Document("IsDelete", new Document("$ne", true)).append("code", commons.regexEscapeForMongoQuery(tinhThanh))),
							new Document("$project", new Document("_id", 0).append("code", 1).append("name", 1))
						)
					)
					.append("as", "DMTinhThanhInfo")
				)
			);
			pipeline.add(new Document("$unwind", new Document("path", "$DMTinhThanhInfo").append("preserveNullAndEmptyArrays", true)));
			pipeline.add(
				new Document("$lookup", 
					new Document("from", "DMChiCucThue")
					.append("pipeline", 
						Arrays.asList(
							new Document("$match", new Document("IsDelete", new Document("$ne", true)).append("code", commons.regexEscapeForMongoQuery(cqtQLy))),
							new Document("$project", new Document("_id", 0).append("code", 1).append("name", 1).append("note", 1))
						)
					)
					.append("as", "DMChiCucThueInfo")
				)
			);
			pipeline.add(new Document("$unwind", new Document("path", "$DMChiCucThueInfo").append("preserveNullAndEmptyArrays", true)));
				
			docFind = new Document("IssuerId", header.getIssuerId()).append("IsDelete", new Document("$ne", true))
					.append("Status", Constants.INVOICE_STATUS.CREATED)		
					.append("_id", objectIdTK);
			pipeline.add(
				new Document("$lookup", 
					new Document("from", "DMTKhai")
					.append("pipeline", 
						Arrays.asList(
							new Document("$match", docFind)
						)
					)
					.append("as", "DMTKhai")
				)
			);
			
			pipeline.add(new Document("$unwind", new Document("path", "$DMTKhai").append("preserveNullAndEmptyArrays", true)));
			

		
			
			 mongoClient = cfg.mongoClient();
			 collection = mongoClient.getDatabase(cfg.dbName).getCollection("Issuer");

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
			if(docTmp.get("UserInfo") == null) {
				responseStatus = new MspResponseStatus(9999, "Không tìm thấy thông tin người dùng.");
				rsp.setResponseStatus(responseStatus);
				return rsp;
			}
			
			if(docTmp.get("DMTinhThanhInfo") == null || docTmp.get("DMChiCucThueInfo") == null) {
				responseStatus = new MspResponseStatus(9999, "Vui lòng kiểm tra lại tỉnh/thành phố và cơ quan thuế.");
				rsp.setResponseStatus(responseStatus);
				return rsp;
			}
			if(docTmp.get("DMTKhai") == null) {
				responseStatus = new MspResponseStatus(9999, "Vui lòng kiểm tra lại thông tin tờ khai 01.");
				rsp.setResponseStatus(responseStatus);
				return rsp;
			}


		
			taxCode = docTmp.getString("TaxCode");
			
			/*TAO FILE XML*/
			path = Paths.get(SystemParams.DIR_E_INVOICE_TKHAI, taxCode);
			pathDir = docTmp.getEmbedded(Arrays.asList("DMTKhai", "Dir"), "");
			fileNameXML =docTmp.getEmbedded(Arrays.asList("DMTKhai", "FileNameXML"), "");
			file = new File(pathDir);
			if(!file.exists()) file.mkdirs();
			/*TAO FILE XML*/
			
			dbf = DocumentBuilderFactory.newInstance();
			db = dbf.newDocumentBuilder();
			doc = db.newDocument();
			doc.setXmlStandalone(true);
			
			root = doc.createElement("TKhai");
			doc.appendChild(root);

			elementTmp = null;
			elementSubTmp = null;
			elementContent = doc.createElement("DLTKhai");
			elementContent.setAttribute("Id", "data");
			root.appendChild(elementContent);
			
			/*THONG TIN CHUNG TO KHAI*/
			elementSubContent = doc.createElement("TTChung");
			elementSubContent.appendChild(commons.createElementWithValue(doc, "PBan",  SystemParams.VERSION_XML_TOKHAI));
			elementSubContent.appendChild(commons.createElementWithValue(doc, "MSo", mauSo));
			elementSubContent.appendChild(commons.createElementWithValue(doc, "Ten", ten));
			elementSubContent.appendChild(commons.createElementWithValue(doc, "HThuc", hThuc));
			elementSubContent.appendChild(commons.createElementWithValue(doc, "TNNT", tenNnt));
			elementSubContent.appendChild(commons.createElementWithValue(doc, "MST", mst));
			elementSubContent.appendChild(commons.createElementWithValue(doc, "CQTQLy", docTmp.getEmbedded(Arrays.asList("DMChiCucThueInfo", "name"), "")));
			elementSubContent.appendChild(commons.createElementWithValue(doc, "MCQTQLy", cqtQLy));
			elementSubContent.appendChild(commons.createElementWithValue(doc, "TNDDPLuat", tnddpLuat));
			elementSubContent.appendChild(commons.createElementWithValue(doc, "DTDDPLuat", dtddpLuat));
			elementSubContent.appendChild(commons.createElementWithValue(doc, "CCCDan", cccDan));
			elementSubContent.appendChild(commons.createElementWithValue(doc, "SHChieu", shChieu));
			elementSubContent.appendChild(commons.createElementWithValue(doc, "NSDDPLuat", commons.convertLocalDateTimeStringToString(nsddpLuat, Constants.FORMAT_DATE.FORMAT_DATE_WEB, Constants.FORMAT_DATE.FORMAT_DATE_EINVOICE)));
			elementSubContent.appendChild(commons.createElementWithValue(doc, "GTinh", gTinh));
			elementSubContent.appendChild(commons.createElementWithValue(doc, "DCLHe", dcLHe));
			elementSubContent.appendChild(commons.createElementWithValue(doc, "DCTDTu", dcCTDTu));
			elementSubContent.appendChild(commons.createElementWithValue(doc, "NLHe", nlHe));
			elementSubContent.appendChild(commons.createElementWithValue(doc, "DTLHe", dtLHe));
			elementSubContent.appendChild(commons.createElementWithValue(doc, "DDanh", docTmp.getEmbedded(Arrays.asList("DMTinhThanhInfo", "name"), "")));
			elementSubContent.appendChild(commons.createElementWithValue(doc, "NLap", commons.convertLocalDateTimeStringToString(nLap, Constants.FORMAT_DATE.FORMAT_DATE_WEB, Constants.FORMAT_DATE.FORMAT_DATE_EINVOICE)));
			elementContent.appendChild(elementSubContent);
			
			/*NOI DUNG CHI TIET TO KHAI*/
			elementSubContent = doc.createElement("NDTKhai");
			/*HINH THUC HOA DON AP DUNG*/
			elementTmp = doc.createElement("HTHDon");
			elementTmp.appendChild(commons.createElementWithValue(doc, "CMa", "CMa".equals(htHDon)? "1": "0"));
			elementTmp.appendChild(commons.createElementWithValue(doc, "CMTMTTien", "on".equals(CMMTT)? "1": "0"));
			elementTmp.appendChild(commons.createElementWithValue(doc, "KCMa", "KCMa".equals(htHDon)? "1": "0"));
			elementSubContent.appendChild(elementTmp);
			/*HINH THUC GUI DU LIEU HDDT - FIX GIA TRI*/
			elementTmp = doc.createElement("HTGDLHDDT");
			elementTmp.appendChild(commons.createElementWithValue(doc, "NNTDBKKhan", "0"));
			elementTmp.appendChild(commons.createElementWithValue(doc, "NNTKTDNUBND", "0"));
			elementTmp.appendChild(commons.createElementWithValue(doc, "CQXLTSCong", "0"));
			elementTmp.appendChild(commons.createElementWithValue(doc, "CDLTTDCQT", "0"));
			elementTmp.appendChild(commons.createElementWithValue(doc, "CDLQTCTN", "0"));
			elementSubContent.appendChild(elementTmp);
			/*PHUONG THUC CHUYEN DU LIEU*/
			elementTmp = doc.createElement("PThuc");
			elementTmp.appendChild(commons.createElementWithValue(doc, "CDDu", "CDDu".equals(pthuc)? "1": "0"));
			elementTmp.appendChild(commons.createElementWithValue(doc, "CBTHop", "CBTHop".equals(pthuc)? "1": "0"));
			elementSubContent.appendChild(elementTmp);
			/*LOAI HD SU DUNG*/
			elementTmp = doc.createElement("LHDSDung");
			elementTmp.appendChild(commons.createElementWithValue(doc, "HDGTGT", "on".equals(lhdSDung_HDGTGT)? "1": "0"));
			elementTmp.appendChild(commons.createElementWithValue(doc, "HDGTGTTHBLai", "on".equals(lhdsDung_HDGTGTTHBLai)? "1": "0"));
			elementTmp.appendChild(commons.createElementWithValue(doc, "HDBHang", "on".equals(lhdsDung_HDBHang)? "1": "0"));
			elementTmp.appendChild(commons.createElementWithValue(doc, "HDBHTHBLai", "on".equals(lhdsDung_HDBHTHBLai)? "1": "0"));
			elementTmp.appendChild(commons.createElementWithValue(doc, "HDTMai", "on".equals(lhdsDung_HDTMai)? "1": "0"));
			elementTmp.appendChild(commons.createElementWithValue(doc, "HDBTSCong", "on".equals(lhdsDung_HDBTSCong)? "1": "0"));
			elementTmp.appendChild(commons.createElementWithValue(doc, "HDBHDTQGia", "on".equals(lhdsDung_HDBHDTQGia)? "1": "0"));
			elementTmp.appendChild(commons.createElementWithValue(doc, "HDKhac", "on".equals(lhdsDung_HDKhac)? "1": "0"));
			elementTmp.appendChild(commons.createElementWithValue(doc, "CTu", "on".equals(lhdsDung_CTu)? "1": "0"));
			elementSubContent.appendChild(elementTmp);
			/*DANH SACH CHUNG THU SO*/
			elementTmp = doc.createElement("DSCTSSDung");
			if (!jsonData.at("/DSCTSSDung").isMissingNode()) {
				for (JsonNode o : jsonData.at("/DSCTSSDung")) {
					String tn = commons.getTextJsonNode(o.at("/TNgay"));
					String[] words2 = tn.split(" ");
					String[] words = words2[1].split(":");
					String h = words[0];
					if (h.length() < 2) {
						h = "0" + h;
					}
					String m = words[1];
					if (m.length() < 2) {
						m = "0" + m;
					}
					String s = words[2];
					if (s.length() < 2) {
						s = "0" + s;
					}
					tn = words2[0] + " " + h + ":" + m + ":" + s;
					String dn = commons.getTextJsonNode(o.at("/DNgay"));
					String[] words21 = dn.split(" ");
					String[] words1 = words21[1].split(":");
					String h1 = words1[0];
					if (h1.length() < 2) {
						h1 = "0" + h1;
					}
					String m1 = words1[1];
					if (m1.length() < 2) {
						m1 = "0" + m1;
					}
					String s1 = words1[2];
					if (s1.length() < 2) {
						s1 = "0" + s1;
					}
					dn = words21[0] + " " + h1 + ":" + m1 + ":" + s1;

					DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
					LocalDateTime dateTime = LocalDateTime.parse(tn, formatter);
					DateTimeFormatter formatter1 = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
					LocalDateTime dateTime1 = LocalDateTime.parse(dn, formatter1);
					elementSubTmp = doc.createElement("CTS");
					elementSubTmp.appendChild(
							commons.createElementWithValue(doc, "TTChuc", commons.getTextJsonNode(o.at("/TTChuc"))));
					elementSubTmp.appendChild(
							commons.createElementWithValue(doc, "Seri", commons.getTextJsonNode(o.at("/Seri"))));
					elementSubTmp.appendChild(commons.createElementWithValue(doc, "TNgay", commons
							.convertLocalDateTimeToString(dateTime, Constants.FORMAT_DATE.FORMAT_DATETIME_EINVOICE)));
					elementSubTmp.appendChild(commons.createElementWithValue(doc, "DNgay", commons
							.convertLocalDateTimeToString(dateTime1, Constants.FORMAT_DATE.FORMAT_DATETIME_EINVOICE)));

					elementSubTmp.appendChild(
							commons.createElementWithValue(doc, "HThuc", commons.getTextJsonNode(o.at("/HThuc"))));
					elementTmp.appendChild(elementSubTmp);

					hO = new LinkedHashMap<String, Object>();
					hO.put("TTChuc", commons.getTextJsonNode(o.at("/TTChuc")));
					hO.put("Seri", commons.getTextJsonNode(o.at("/Seri")));
					hO.put("TNgay", dateTime);
					hO.put("DNgay", dateTime1);
					hO.put("HThuc", commons.getTextJsonNode(o.at("/HThuc")));
					rowDSCTSSDung.add(hO);

				}
			}
			elementSubContent.appendChild(elementTmp);
			
			/*TO CHUC CUNG CAP DICH VU*/
			elementTmp = doc.createElement("TTTCGP");
			if (!jsonData.at("/DSTCGPhap").isMissingNode()) {
				for (JsonNode data : jsonData.at("/DSTCGPhap")) {
					String ttcgp = commons.getTextJsonNode(data.at("/TTCGPhap"));
					String msttcgp = commons.getTextJsonNode(data.at("/MSTTCGPhap"));
					if (ttcgp.trim().isEmpty() || msttcgp.trim().isEmpty()) continue;
					elementSubTmp = doc.createElement("TCGP");
					elementSubTmp.appendChild(commons.createElementWithValue(doc, "TTCGP", ttcgp));
					elementSubTmp.appendChild(commons.createElementWithValue(doc, "MSTTCGP", msttcgp));
					elementSubTmp.appendChild(commons.createElementWithValue(doc, "TNgay", commons.convertLocalDateTimeToString(LocalDate.now(), "yyyy-MM-dd")));
					elementTmp.appendChild(elementSubTmp);
					
					hO = new LinkedHashMap<String, Object>();
					hO.put("MSTTCGPhap", msttcgp);
					hO.put("TTCGPhap", ttcgp);
					rowDSTCGPhap.add(hO);
				}
			}
			elementSubContent.appendChild(elementTmp);
			
			/*TO CHUC TRUYEN NHAN*/
			elementTmp = doc.createElement("TTTCTN");
			if (!jsonData.at("/DSTCTNhan").isMissingNode()) {
				for (JsonNode data : jsonData.at("/DSTCTNhan")) {
					String ttctn = commons.getTextJsonNode(data.at("/TTCTNhan"));
					String msttctn = commons.getTextJsonNode(data.at("/MSTTCTNhan"));
					if (ttctn.trim().isEmpty() || msttctn.trim().isEmpty()) continue;
					elementSubTmp = doc.createElement("TCTN");
					elementSubTmp.appendChild(commons.createElementWithValue(doc, "TTCTN", ttctn));
					elementSubTmp.appendChild(commons.createElementWithValue(doc, "MSTTCTN", msttctn));
					elementSubTmp.appendChild(commons.createElementWithValue(doc, "TNgay", commons.convertLocalDateTimeToString(LocalDate.now(), "yyyy-MM-dd")));
					elementTmp.appendChild(elementSubTmp);
					
					hO = new LinkedHashMap<String, Object>();
					hO.put("MSTTCTNhan", msttctn);
					hO.put("TTCTNhan", ttctn);
					rowDSTCTNhan.add(hO);
				}
			}
			
			elementSubContent.appendChild(elementTmp);
			
			elementContent.appendChild(elementSubContent);
			/*END - TAO FILE XML*/
			isSdaveFile = commons.docW3cToFile(doc, pathDir, fileNameXML);
			if(!isSdaveFile) {
				throw new Exception("Lưu dữ liệu không thành công.");
			}
			
			MTDiep = SystemParams.MSTTCGP + commons.csRandomAlphaNumbericString(46 - SystemParams.MSTTCGP.length()).toUpperCase();
			
			/*LUU DU LIEU*/
			docUpsert = new Document("_id", objectIdTK)
					.append("IssuerId", header.getIssuerId())
					.append("MTDiep", MTDiep)
					.append("TenNnt", tenNnt)
					.append("MSo", mauSo)
					.append("Ten", ten)
					.append("HThuc", hThuc)
					.append("MST", mst)
					.append("TinhThanhInfo", docTmp.get("DMTinhThanhInfo"))
					.append("ChiCucThueInfo", docTmp.get("DMChiCucThueInfo"))
					.append("NLHe", nlHe)
					.append("DCLHe", dcLHe)
					.append("DCTDTu", dcCTDTu)
					.append("DTLHe", dtLHe)
					.append("NLap", commons.convertStringToLocalDate(nLap, Constants.FORMAT_DATE.FORMAT_DATE_WEB))	
					.append("TNDDPLuat", tnddpLuat)
					.append("DTDDPLuat", dtddpLuat)
					.append("CCCDan", cccDan)
					.append("SHChieu", shChieu)
					.append("NSDDPLuat", commons.convertStringToLocalDate(nsddpLuat, Constants.FORMAT_DATE.FORMAT_DATE_WEB))
					.append("GTinh", gTinh)
					.append("HTHDon", htHDon)
					.append("PThuc", pthuc)		
					.append("CMMTTien",  "on".equals(CMMTT)? "1": "0")		
					
					.append("LHDSDung", 
						new Document("HDGTGT", "on".equals(lhdSDung_HDGTGT)? "1": "0")
							.append("HDGTGTTHBLai", "on".equals(lhdsDung_HDGTGTTHBLai)? "1": "0")
							.append("HDBHang", "on".equals(lhdsDung_HDBHang)? "1": "0")
							.append("HDBHTHBLai", "on".equals(lhdsDung_HDBHTHBLai)? "1": "0")
							.append("HDTMai", "on".equals(lhdsDung_HDTMai)? "1": "0")
							.append("HDBTSCong", "on".equals(lhdsDung_HDBTSCong)? "1": "0")
							.append("HDBHDTQGia", "on".equals(lhdsDung_HDBHDTQGia)? "1": "0")
							.append("HDKhac", "on".equals(lhdsDung_HDKhac)? "1": "0")
							.append("CTu", "on".equals(lhdsDung_CTu)? "1": "0")
					)
					.append("DSCTSSDung", rowDSCTSSDung)
					.append("DSTCGPhap", rowDSTCGPhap)
					.append("DSTCTNhan", rowDSTCTNhan)
					.append("Status", Constants.INVOICE_STATUS.TK_CREATED)
					.append("IsDelete", false)
					.append("Dir", pathDir)
					.append("FileNameXML", fileNameXML)
					.append("InfoUpdated", 
							new Document("UpdatedDate", LocalDateTime.now())
							.append("UpdatedUserID", header.getUserId())
							.append("UpdatedUserName", header.getUserName())
							.append("UpdatedUserFullName", header.getUserFullName())
					);
			
			/*END - LUU DU LIEU*/
			
			options = new FindOneAndUpdateOptions();
			options.upsert(false);
			options.maxTime(5000, TimeUnit.MILLISECONDS);
			options.returnDocument(ReturnDocument.AFTER);
			

			mongoClient = cfg.mongoClient();
			collection = mongoClient.getDatabase(cfg.dbName).getCollection("DMTKhai");
			collection.findOneAndUpdate(docFind, new Document("$set", docUpsert), options);
			mongoClient.close();
			
			format_time = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");			
			time_dem  = LocalDateTime.now();			
			time = time_dem.format(format_time);
			name_company = removeAccent(header.getUserFullName());
			System.out.println(time +name_company+" Vua thay doi to khai");
			responseStatus = new MspResponseStatus(0, "SUCCESS");
			rsp.setResponseStatus(responseStatus);				
			return rsp;	
			
		case Constants.MSG_ACTION_CODE.DELETE:
			objectId = null;
			try {
				objectId = new ObjectId(_id);
			}catch(Exception e) {}
			
			docFind = new Document("IssuerId", header.getIssuerId())
					.append("_id", objectId).append("IsDelete", new Document("$ne", true))
					.append("Status", "CREATED");

			mongoClient = cfg.mongoClient();
			collection = mongoClient.getDatabase(cfg.dbName).getCollection("DMTKhai");
			docTmp = null;
			try {
				docTmp = collection.find(docFind).allowDiskUse(true).iterator().next();
			} catch (Exception e) {

			}
			mongoClient.close();

			if(null == docTmp) {
				responseStatus = new MspResponseStatus(9999, "Không tìm thấy thông tin tờ khai.");
				rsp.setResponseStatus(responseStatus);
				return rsp;
			}

			options = new FindOneAndUpdateOptions();
			options.upsert(false);
			options.maxTime(5000, TimeUnit.MILLISECONDS);
			options.returnDocument(ReturnDocument.AFTER);
		
			mongoClient = cfg.mongoClient();
			collection = mongoClient.getDatabase(cfg.dbName).getCollection("DMTKhai");
			collection.findOneAndUpdate(docFind,
					new Document("$set", new Document("IsDelete", true).append("InfoDeleted",
							new Document("DeletedDate", LocalDateTime.now()).append("DeletedUserID", header.getUserId())
									.append("DeletedUserName", header.getUserName())
									.append("DeletedUserFullName", header.getUserFullName()))),
					options);
			mongoClient.close();

			format_time = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");			
			time_dem  = LocalDateTime.now();			
			time = time_dem.format(format_time);
			
			name_company = removeAccent(header.getUserFullName());
			System.out.println(time+name_company+" Vua xoa to khai");
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
		}catch(Exception e) {}
		
		Document docFind = new Document("IssuerId", header.getIssuerId())
				.append("IsDelete", new Document("$ne", true)).append("_id", objectId);
		
		Document docTmp = null;
	
		
		MongoClient mongoClient = cfg.mongoClient();
		MongoCollection<Document> collection = mongoClient.getDatabase(cfg.dbName).getCollection("DMTKhai");

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
	public MsgRsp signSingleV1(InputStream is, JSONRoot jsonRoot, String _id) throws Exception {
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
		
		/*KIEM TRA XEM THONG TIN TKHAI CO TON TAI KHONG*/
		Document docFind = new Document("IssuerId", header.getIssuerId())
				.append("_id", objectId).append("IsDelete", new Document("$ne", true))
				.append("Status", "CREATED");
		
		Document docTmp = null;
	
		MongoClient mongoClient = cfg.mongoClient();
		MongoCollection<Document> collection = mongoClient.getDatabase(cfg.dbName).getCollection("DMTKhai");

		try {
			docTmp = collection.find(docFind).allowDiskUse(true).iterator().next();
		} catch (Exception e) {

		}
		mongoClient.close();
		
		
		if(null == docTmp) {
			responseStatus = new MspResponseStatus(9999, "Không tìm thấy thông tin tờ khai.");
			rsp.setResponseStatus(responseStatus);
			return rsp;
		}
		
		/*LUU FILE VA CAP NHAT TRANG THAI*/
		String dir = docTmp.get("Dir", "");
		String fileName = _id + "_processing.xml";
		boolean check = commons.docW3cToFile(xmlDoc, dir, fileName);
		if(!check) {
			responseStatus = new MspResponseStatus(9999, "Lưu tập tin đã ký không thành công.");
			rsp.setResponseStatus(responseStatus);
			return rsp;
		}
		
		/*KET NOI VA DAY HD DEN CHUC TRUYEN NHAN*/
		org.w3c.dom.Document rTCTN = null;
		String MTDiep = docTmp.get("MTDiep", "");
		String MST = docTmp.get("MST", "");
		/*END - KET NOI VA DAY HD DEN CHUC TRUYEN NHAN*/
		
		rTCTN = tctnService.callTiepNhanThongDiep("100", MTDiep, MST, "1", commons.fileToDocument(new File(dir, fileName), true));
		if(rTCTN == null) {
			rTCTN = tctnService.callTiepNhanThongDiep("100", MTDiep, MST, "1", commons.fileToDocument(new File(dir, fileName), true));
		}
		
		/*DO DU LIEU TRA VE - CAP NHAT LAI KET QUA*/
		XPath xPath = XPathFactory.newInstance().newXPath();
		Node nodeDLHDon = (Node) xPath.evaluate("/TDiep", rTCTN, XPathConstants.NODE);
		String codeTTTNhan = "3";
		codeTTTNhan = commons.getTextFromNodeXML((Element) xPath.evaluate("DLieu/TBao/TTTNhan", nodeDLHDon, XPathConstants.NODE));
		
//		if (codeTTTNhan.equals("1")) {
//			responseStatus = new MspResponseStatus(9999, "Trạng thái tiếp nhận lỗi.");
//			rsp.setResponseStatus(responseStatus);
//			return rsp;
//		}
		switch (codeTTTNhan) {
		case "1":
			responseStatus = new MspResponseStatus(9999, "Không tìm thấy tenant dữ liệu.");
			rsp.setResponseStatus(responseStatus);
			return rsp;
		case "2":
			responseStatus = new MspResponseStatus(9999, "Mã thông điệp đã tồn tại.");
			rsp.setResponseStatus(responseStatus);
			return rsp;
		case "3":
			responseStatus = new MspResponseStatus(9999, "Thất bại, lỗi Exception.");
			rsp.setResponseStatus(responseStatus);
			return rsp;
		default:
			break;
		}

		/*CAP NHAT LAI TRANG THAI DANG CHO XU LY*/
		FindOneAndUpdateOptions options = new FindOneAndUpdateOptions();
		options.upsert(false);
		options.maxTime(5000, TimeUnit.MILLISECONDS);
		options.returnDocument(ReturnDocument.AFTER);

		mongoClient = cfg.mongoClient();
		collection = mongoClient.getDatabase(cfg.dbName).getCollection("DMTKhai");
		collection.findOneAndUpdate(docFind,
				new Document("$set", 
						new Document("Status", "PROCESSING")
						.append("InfoSigned", new Document("SignedDate", LocalDateTime.now())
											.append("SignedUserID", header.getUserId())
											.append("SignedUserName", header.getUserName())
											.append("SignedUserFullName", header.getUserFullName()))),
				options);
		mongoClient.close();
					
		DateTimeFormatter format_time = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
		LocalDateTime time_dem  = LocalDateTime.now();
		String time = time_dem.format(format_time);	
			
		String name_company = removeAccent(header.getUserFullName());
		System.out.println(time +name_company+" Vua ky to khai");
		responseStatus = new MspResponseStatus(0, Constants.MAP_ERROR.get(0));
		rsp.setResponseStatus(responseStatus);
		return rsp;
	}

	@Override
	public MsgRsp refreshStatusCQTV1(JSONRoot jsonRoot) throws Exception {
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
		
		ObjectId objectId = null;
		try {
			objectId = new ObjectId(_id);
		}catch(Exception e) {}
		
		/*KIEM TRA XEM THONG TIN TKHAI CO TON TAI KHONG*/
		Document docFind = new Document("IssuerId", header.getIssuerId())
				.append("_id", objectId).append("IsDelete", new Document("$ne", true))
				.append("Status", "PROCESSING");
		
		Document docTmp = null;
	
		
		MongoClient mongoClient = cfg.mongoClient();
		MongoCollection<Document> collection = mongoClient.getDatabase(cfg.dbName).getCollection("DMTKhai");

		try {
			docTmp = collection.find(docFind).allowDiskUse(true).iterator().next();
		} catch (Exception e) {

		}
		mongoClient.close();
		
		if(null == docTmp) {
			responseStatus = new MspResponseStatus(9999, "Không tìm thấy thông tin tờ khai.");
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
		
		Node nodeTDiep = (Node) xPath.evaluate("DuLieu/TDiep[last()]", nodeKetQuaTraCuu, XPathConstants.NODE);
		if(nodeTDiep == null) {
			responseStatus = new MspResponseStatus(9999, "Không đọc được kết quả tra cứu.");
			rsp.setResponseStatus(responseStatus);
			return rsp;
		}
		String CQT_MLTDiep = commons.getTextFromNodeXML((Element) xPath.evaluate("TTChung/MLTDiep", nodeTDiep, XPathConstants.NODE));
		if("|102|103|".indexOf("|" + CQT_MLTDiep + "|") == -1) {
			responseStatus = new MspResponseStatus(9999, "CQT chưa có thông báo kết quả trả về.");
			rsp.setResponseStatus(responseStatus);
			return rsp;
		}
		
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
		
		String MLoi = "";
		String MTa = "";
		Node nodeTmp = null;
		/*CAP NHAT TRANG THAI COMPLETE - TRANG THAI CQT*/
		FindOneAndUpdateOptions options = new FindOneAndUpdateOptions();
		options.upsert(false);
		options.maxTime(5000, TimeUnit.MILLISECONDS);
		options.returnDocument(ReturnDocument.AFTER);
		
		Document docUpdate = new Document("StatusCQT", CQT_MLTDiep);
		if("103".equals(CQT_MLTDiep)) {
			/*LAY DANH SACH LOI (NEU CO)*/
			NodeList nodeListDSLDKCNhan = (NodeList) xPath.evaluate("DLieu/TBao/DLTBao/DSLDKCNhan/LDo", nodeTDiep, XPathConstants.NODESET) ;
			if(null == nodeListDSLDKCNhan || nodeListDSLDKCNhan.getLength() == 0) {
				docUpdate.append("Status", "COMPLETE");	
			}else {
				List<Document> DSLDKCNhan = new ArrayList<Document>();
				for(int i = 0; i < nodeListDSLDKCNhan.getLength(); i++) {
					nodeTmp = nodeListDSLDKCNhan.item(i);
					if("".equals(MLoi)) {
						MLoi = commons.getTextFromNodeXML((Element) xPath.evaluate("MLoi", nodeTmp, XPathConstants.NODE));
						MTa = commons.getTextFromNodeXML((Element) xPath.evaluate("MTa", nodeTmp, XPathConstants.NODE));
					}
					DSLDKCNhan.add(
						new Document("MLoi", commons.getTextFromNodeXML((Element) xPath.evaluate("MLoi", nodeTmp, XPathConstants.NODE)))
						.append("MTa", commons.getTextFromNodeXML((Element) xPath.evaluate("MTa", nodeTmp, XPathConstants.NODE)))
					);
				}
				docUpdate.append("Status", Constants.INVOICE_STATUS.ERROR_CQT)
				.append("LDo", 
					new Document("MLoi", MLoi).append("MTa", MTa)
				)
				.append("DSLDKCNhan", DSLDKCNhan);
			}
			
		}else {
			MLoi = commons.getTextFromNodeXML((Element) xPath.evaluate("DLieu/TBao/DLTBao/DSLDKCNhan/LDo/MLoi", nodeTDiep, XPathConstants.NODE));
			MTa = commons.getTextFromNodeXML((Element) xPath.evaluate("DLieu/TBao/DLTBao/DSLDKCNhan/LDo/MTa", nodeTDiep, XPathConstants.NODE));
			
			if("".equals(MLoi)) {
				responseStatus = new MspResponseStatus(9999, "CQT chưa có thông báo kết quả trả về.");
				rsp.setResponseStatus(responseStatus);
				return rsp;
			}
			
			docUpdate.append("Status", Constants.INVOICE_STATUS.ERROR_CQT)
				.append("LDo", 
					new Document("MLoi", MLoi).append("MTa", MTa)
				);
			
		}
	
		 mongoClient = cfg.mongoClient();
			collection = mongoClient.getDatabase(cfg.dbName).getCollection("DMTKhai");
			collection.findOneAndUpdate(
					docFind, 
					new Document("$set", docUpdate), 
					options
				);			
			mongoClient.close();
			
			
		responseStatus = new MspResponseStatus(0, Constants.MAP_ERROR.get(0));
		rsp.setResponseStatus(responseStatus);
		return rsp;
	}	
}
