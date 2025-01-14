package vn.sesgroup.hddt.user.impl;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.DataFormat;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.api.message.JSONRoot;
import com.api.message.Msg;
import com.api.message.MsgHeader;
import com.api.message.MsgPage;
import com.api.message.MsgRsp;
import com.api.message.MspResponseStatus;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.mongodb.client.model.FindOneAndUpdateOptions;
import com.mongodb.client.model.ReturnDocument;

import vn.sesgroup.hddt.dto.FileInfo;
import vn.sesgroup.hddt.user.dao.AbstractDAO;
import vn.sesgroup.hddt.user.dao.MauHD23UpdateAdminDAO;
import vn.sesgroup.hddt.user.service.TCTNService;
import vn.sesgroup.hddt.utility.Constants;
import vn.sesgroup.hddt.utility.Json;
import vn.sesgroup.hddt.utility.SystemParams;

@Repository
@Transactional
public class MauHD23UpdateAdminImpl extends AbstractDAO implements MauHD23UpdateAdminDAO {
	@Autowired TCTNService tctnService;
	@Autowired MongoTemplate mongoTemplate;

	public MsgRsp list(JSONRoot jsonRoot) throws Exception {
		Msg msg = jsonRoot.getMsg();
		MsgHeader header = msg.getMsgHeader();
		MsgPage page = msg.getMsgPage();
		Object objData = msg.getObjData();

		ObjectId objectId = null;
		Document docTmp = null;
		Iterable<Document> cursor = null;
		Iterator<Document> iter = null;
		Iterable<Document> cursor1 = null;
		Iterator<Document> iter1 = null;
		Iterable<Document> cursor3 = null;
		Iterator<Document> iter3 = null;
		
		List<Document> pipeline = new ArrayList<Document>();

		String mst = "";
		String mskhieu = "";
		String name = "";
		String status = "";

		JsonNode jsonData = null;
		if (objData != null) {
			jsonData = Json.serializer().nodeFromObject(objData);
			mst = commons.getTextJsonNode(jsonData.at("/MST")).replaceAll("\\s", "");
			mskhieu = commons.getTextJsonNode(jsonData.at("/MSKH")).replaceAll("\\s", "");
			name = commons.getTextJsonNode(jsonData.at("/Name")).replaceAll("\\s+", " ");
			status = commons.getTextJsonNode(jsonData.at("/Status")).replaceAll("\\s+", "");
		}

		MsgRsp rsp = new MsgRsp(header);
		MspResponseStatus responseStatus = null;

		ObjectId issu = null;
		String issuerId = null;
		if (!mst.equals("") || !name.equals("")) {
			Document docTmp3 = null;
			Document findIssuer = new Document("IsDelete", new Document("$ne", true));

			if (!mst.equals(""))
				findIssuer.append("TaxCode", mst);

			if (!name.equals(""))
				findIssuer.append("Name",
						new Document("$regex", commons.regexEscapeForMongoQuery(name)).append("$options", "i"));
			
			cursor3 = mongoTemplate.getCollection("Issuer").find(findIssuer).allowDiskUse(true);
			iter3 = cursor3.iterator();
			if (iter3.hasNext()) {
				docTmp3 = iter3.next();
			}
			
			if (docTmp3 == null) {
				responseStatus = new MspResponseStatus(9999, Constants.MAP_ERROR.get(9999));
				rsp.setResponseStatus(responseStatus);
				return rsp;
			}

			issu = docTmp3.getObjectId("_id");
			issuerId = issu.toString();
		}

		Document docMSKH = new Document("IsDelete", new Document("$ne", true)).append("IsActive", true);

		if (issuerId != null)
			docMSKH.append("IssuerId", issuerId);
		
		if (!mskhieu.equals(""))
			docMSKH.append("KHHDon", mskhieu);
		
		if(!status.equals("")) {
			if(status.equals("true")) {
				docMSKH.append("InvoiceStatus", true);
			}else {
				docMSKH.append("InvoiceStatus", new Document("$ne", true));			
			}
		}

		pipeline.add(new Document("$match", docMSKH));
		pipeline.add(new Document("$sort", new Document("NamPhatHanh", -1)));
		pipeline.addAll(createFacetForSearchNotSort(page));
		
		cursor = mongoTemplate.getCollection("DMMauSoKyHieu").aggregate(pipeline).allowDiskUse(true);
		iter = cursor.iterator();
		if (iter.hasNext()) {
			docTmp = iter.next();
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

		List<Document> mskh = null;
		if (docTmp.get("data") != null) {
			mskh = docTmp.getList("data", Document.class);
		}

		List<Document> rows = null;
		if (mskh != null && docTmp.get("data") instanceof List) {
			rows = docTmp.getList("data", Document.class);
		}

		ArrayList<HashMap<String, Object>> rowsReturn = new ArrayList<HashMap<String, Object>>();
		HashMap<String, Object> hItem = null;
		if (null != rows) {
			for (Document doc : rows) {
				objectId = (ObjectId) doc.get("_id");
				Document docTmp1 = null;

				String issuerid = doc.get("IssuerId").toString();
				ObjectId id_issuer = new ObjectId(issuerid);

				// FIND ISSUERID

				Document findIssuer = new Document("_id", id_issuer).append("IsDelete", false);			
				pipeline = new ArrayList<Document>();
				pipeline.add(new Document("$match", findIssuer));
				
				cursor1 = mongoTemplate.getCollection("Issuer").aggregate(pipeline).allowDiskUse(true);
				iter1 = cursor1.iterator();
				if (iter1.hasNext()) {
					docTmp1 = iter1.next();
				}

				String TaxCode = "";
				String Ten = "";
				if (docTmp1 == null) {
					TaxCode = "";
					Ten = "";

				} else {
					TaxCode = docTmp1.get("TaxCode", "");
					Ten = docTmp1.get("Name", "");
				}

				int SLMauMoi = doc.get("SoLuong", 0);
				String KHMSHDonMoi = doc.get("KHMSHDon", "");
				String KHHDonMoi = doc.get("KHHDon", "");
				int NamPhatHanhMoi = doc.get("NamPhatHanh", 0);
				boolean check_SLCL = false;
				
				Document docTmp2 = null;
				int SLLech = 0;
				int NamPhatHanhCu = NamPhatHanhMoi - 1;
				String KHMSHDonCu = KHMSHDonMoi;
				String namPhatHanhCuString = String.valueOf(NamPhatHanhCu);
				String kyTuCuoiNamCu = namPhatHanhCuString.substring(namPhatHanhCuString.length() - 2);				
				String KHHDonCu = KHHDonMoi.substring(0, 1) + kyTuCuoiNamCu + KHHDonMoi.substring(3);
				
				Document findOLDMSKH = new Document("KHMSHDon", KHMSHDonCu)
						.append("KHHDon", KHHDonCu)
						.append("NamPhatHanh", NamPhatHanhCu)
						.append("IssuerId", issuerid)
						.append("IsDelete", new Document("$ne", true));
	
				Iterable<Document> cursor2 = mongoTemplate.getCollection("DMMauSoKyHieu").find(findOLDMSKH);
				Iterator<Document> iter2 = cursor2.iterator();
				if (iter2.hasNext()) {
					docTmp2 = iter2.next();
				}
				
				if (docTmp2 == null) {
					SLLech = 0;
				}else {
					int SLConLaiNamCu = docTmp2.get("ConLai", 0);	
					if(SLConLaiNamCu == 0) {
						check_SLCL = true;
					}
					SLLech = SLMauMoi - SLConLaiNamCu;
				}
				
				hItem = new HashMap<String, Object>();
				hItem.put("_id", objectId.toString());
				hItem.put("SHDHT", doc.get("SHDHT"));
				hItem.put("KHMSHDon", doc.get("KHMSHDon"));
				hItem.put("KHHDon", doc.get("KHHDon"));
				hItem.put("SoLuong", doc.get("SoLuong"));
				hItem.put("ConLai", doc.get("ConLai"));
				hItem.put("TaxCode", TaxCode);
				hItem.put("Name", Ten);
				hItem.put("SoLuongLech", SLLech);
				hItem.put("InvoiceStatus", doc.get("InvoiceStatus"));
				if(check_SLCL == true && SLLech > 0) {
					hItem.put("CheckInvoice", true);
				}else {
					hItem.put("CheckInvoice", false);
				}
				hItem.put("SoLuongNotEnough", doc.get("SoLuongNotEnough"));
				
				rowsReturn.add(hItem);					
			}
		}
		rsp.getMsgHeader().setAdmin(true);

		responseStatus = new MspResponseStatus(0, "SUCCESS");
		rsp.setResponseStatus(responseStatus);
		HashMap<String, Object> mapDataR = new HashMap<String, Object>();
		mapDataR.put("rows", rowsReturn);
		rsp.setObjData(mapDataR);
		return rsp;
	}

	@Override
	public MsgRsp checkdb(JSONRoot jsonRoot) throws Exception {

		Msg msg = jsonRoot.getMsg();
		MsgHeader header = msg.getMsgHeader();
		MsgRsp rsp = new MsgRsp(header);
		MspResponseStatus responseStatus = null;

		Document docTmp = null;
		List<Document> pipeline = new ArrayList<Document>();
		Iterable<Document> cursor = null;
		Iterator<Document> iter = null;

		Document docMatch = new Document("ActiveFlag", new Document("$ne", false));
		pipeline.add(new Document("$match", docMatch));

		pipeline.add(new Document("$lookup", new Document("from", "DMMauSoKyHieu")
				.append("pipeline", Arrays.asList(new Document("$match",
						new Document("IsDelete", new Document("$ne", true)).append("IsActive", true)
								.append("NamPhatHanh", 2022).append("InfoPhatHanhNam23", new Document("$ne", null)))))
				.append("as", "DMMauSoKyHieu")));
		
		cursor = mongoTemplate.getCollection("ApiLicenseKey").aggregate(pipeline).allowDiskUse(true);
		iter = cursor.iterator();
		if (iter.hasNext()) {
			docTmp = iter.next();
		}
		
		
		List<Document> mskh = null;
		if (docTmp.get("DMMauSoKyHieu") != null) {
			mskh = docTmp.getList("DMMauSoKyHieu", Document.class);
		}

		int dem = mskh.size();

		int dem_ms_update = 0;
		for (int i = 0; i < dem; i++) {
			Document checkms = null;
			checkms = mskh.get(i);
			int SoLuongConLai = checkms.get("ConLai", 0);
			int SoLuongConLaiNam23 = checkms.getEmbedded(Arrays.asList("InfoPhatHanhNam23", "SoLuong"), 0);
			int check = SoLuongConLaiNam23 - SoLuongConLai;
			if (check > 0) {
				dem_ms_update++;
			}

		}
		rsp = new MsgRsp(header);
		responseStatus = null;

		int total = dem;

		String SLMSHT = String.valueOf(total);
		String SLMSCN = String.valueOf(dem_ms_update);

		responseStatus = new MspResponseStatus(0, SLMSHT + "," + SLMSCN);
		rsp.setResponseStatus(responseStatus);
		return rsp;
	}

	@Override
	public MsgRsp updatedb(JSONRoot jsonRoot) throws Exception {
		Msg msg = jsonRoot.getMsg();
		MsgHeader header = msg.getMsgHeader();
		MsgRsp rsp = new MsgRsp(header);
		Object objData = msg.getObjData();
		MspResponseStatus responseStatus = null;
		Document docTmp = null;
		FindOneAndUpdateOptions options = null;
		Iterable<Document> cursor = null;
		Iterator<Document> iter = null;
		
		List<String> ids = null;
		JsonNode jsonData = null;
		if (objData != null) {
			jsonData = Json.serializer().nodeFromObject(msg.getObjData());
		} else {
			throw new Exception("Lỗi dữ liệu đầu vào");
		}
		
		String ListID = commons.getTextJsonNode(jsonData.at("/ListID")).replaceAll("\\s", "");
		
		ids = null;
		try {
			ids = Json.serializer().fromJson(commons.decodeBase64ToString(ListID), new TypeReference<List<String>>() {
			});
		} catch (Exception e) {
		}
		
		ObjectId objectId = null;
		for(int i = 0 ; i < ids.size() ; ++i) {
			
			String data = ids.get(i);
			String split [] = data.split(";");
			String _id = split[0];
			String SLLech = split[1];
			String Check = split[2];
			
			
			if(Check.equals("true")) {
				ObjectId issuerID = null;
				objectId = new ObjectId(_id);
				Document findMSKH = new Document("_id", objectId).append("IsDelete", new Document("$ne", true));			
				
				cursor = mongoTemplate.getCollection("DMMauSoKyHieu").find(findMSKH).allowDiskUse(true);
				iter = cursor.iterator();
				if (iter.hasNext()) {
					docTmp = iter.next();
				}
				
				if(docTmp == null) {
					continue;
				}
				
				String issuerId = docTmp.get("IssuerId", "");
				issuerID = new ObjectId(issuerId);
				Document findIssuer = new Document("_id", issuerID).append("IsDelete", new Document("$ne", true));
				
				Document docTmp1 = null;
				Iterable<Document> cursor1 = mongoTemplate.getCollection("Issuer").find(findIssuer).allowDiskUse(true);
				Iterator<Document> iter1 = cursor1.iterator();
				if (iter1.hasNext()) {
					docTmp1 = iter1.next();
				}
				
				if(docTmp1 == null) {
					continue;
				}
				
				String TaxCode = docTmp1.get("TaxCode", "");
				String Name = docTmp1.get("Name", "");
				
				int SoLuongLech = Integer.parseInt(SLLech);
				
				
				/* INSERT CONTRACT */
				Document insertContract =  new Document("Contract", new Document("SHDon", SLLech)
								.append("SLHDon", SoLuongLech)
								.append("SLHDonCu", SoLuongLech)
								.append("GhiChu", "")
							)
						.append("NMUA", new Document("TaxCode", TaxCode)
								.append("Name", Name))				
						.append("IsActive", true)
						.append("IsDelete", false)
//						.append("IsActiveApprove", true)
						.append("CheckActive", true)
						.append("InfoCreated",
								new Document("CreateDate", LocalDateTime.now())
								.append("CreateUserID", header.getUserId())
										.append("CreateUserName", header.getUserName())
										.append("CreateUserFullName", header.getUserFullName()));
			
				mongoTemplate.getCollection("Contract").insertOne(insertContract);

				/* UPDATE DMDEPOT */
				
				Document findKho = new Document("TaxCode", TaxCode)
						.append("IsDelete", new Document("$ne", true));
				Document docTmp2 = null;
		
				Iterable<Document> cursor2 = mongoTemplate.getCollection("DMDepot").find(findKho).allowDiskUse(true);
				Iterator<Document> iter2 = cursor2.iterator();
				if (iter2.hasNext()) {
					docTmp2 = iter2.next();
				}
				
				if (docTmp2 == null) {
					mongoTemplate.getCollection("DMDepot").insertOne(new Document("TaxCode", TaxCode)
							.append("SLHDon", SoLuongLech)
							.append("SLHDonDD", SoLuongLech)
							.append("SLHDonCL", 0)
							.append("IsRoot", false)
							.append("IsDelete", false)
							.append("InfoCreated",
									new Document("CreateDate", LocalDateTime.now())
											.append("CreateUserID", header.getUserId())
											.append("CreateUserName", header.getUserName())
											.append("CreateUserFullName", header.getUserFullName()))
							);	
				}else {
				
				
				int SLHDon = docTmp2.getInteger("SLHDon", 0);
				int SLHDonDD = docTmp2.getInteger("SLHDonDD", 0);

				int update_SLHDon_kho = SLHDon + SoLuongLech;
				int update_SLHDonDD_kho = SLHDonDD + SoLuongLech;
				
				options = new FindOneAndUpdateOptions();
				options.upsert(false);
				options.maxTime(5000, TimeUnit.MILLISECONDS);
				options.returnDocument(ReturnDocument.AFTER);
				
				mongoTemplate.getCollection("DMDepot").findOneAndUpdate(findKho,
						new Document("$set", new Document("SLHDon", update_SLHDon_kho)
								.append("SLHDonDD", update_SLHDonDD_kho)
								.append("InfoUpdated",
										new Document("UpdatedDate", LocalDateTime.now())
												.append("SoLuong", SoLuongLech)
												.append("UpdatedUserID", header.getUserId())
												.append("UpdatedUserName", header.getUserName())
												.append("UpdatedUserFullName", header.getUserFullName()))),
						options); 
				}
				
				options = new FindOneAndUpdateOptions();
				options.upsert(false);
				options.maxTime(5000, TimeUnit.MILLISECONDS);
				options.returnDocument(ReturnDocument.AFTER);
				
				 mongoTemplate.getCollection("DMMauSoKyHieu").findOneAndUpdate(findMSKH,
						new Document("$set", new Document("InvoiceStatus", true)				
								.append("InfoUpdateSoLuongLech",
										new Document("UpdatedInvoiceStatus", LocalDateTime.now())
												.append("UpdatedInvoiceStatusUserID", header.getUserId())
												.append("UpdatedInvoiceStatusUserName", header.getUserName())
												.append("UpdatedInvoiceStatusUserFullName", header.getUserFullName()))),
						options);
				
			}else {
				objectId = new ObjectId(_id);
				Document findMSKH = new Document("_id", objectId).append("IsDelete", new Document("$ne", true));
				
				cursor = mongoTemplate.getCollection("DMMauSoKyHieu").find(findMSKH).allowDiskUse(true);
				iter = cursor.iterator();
				if (iter.hasNext()) {
					docTmp = iter.next();
				}
				
				if(docTmp == null) {
					continue;
				}
				
				int SoLuongLech = Integer.parseInt(SLLech);
				int SLPhatHanh = docTmp.get("SoLuong", 0);
				int SLConLai = docTmp.get("ConLai", 0);
				
				
				if(SLConLai < SoLuongLech) {
					
					options = new FindOneAndUpdateOptions();
					options.upsert(false);
					options.maxTime(5000, TimeUnit.MILLISECONDS);
					options.returnDocument(ReturnDocument.AFTER);
					
					 mongoTemplate.getCollection("DMMauSoKyHieu").findOneAndUpdate(findMSKH,
							new Document("$set", new Document("SoLuongNotEnough", true)
									.append("InfoUpdateSoLuongNotEnough",
											new Document("UpdatedSoLuongNotEnough", LocalDateTime.now())
													.append("UpdatedSoLuongNotEnoughserID", header.getUserId())
													.append("UpdatedSoLuongNotEnoughUserName", header.getUserName())
													.append("UpdatedSoLuongNotEnoughUserFullName", header.getUserFullName()))),
							options);	
					 
				}else {
				
				int SLPhatHanhUpdate = SLPhatHanh - SoLuongLech;
				int SLConLaiUpdate = SLConLai - SoLuongLech;
				// CAP NHAT SO HOA DON
				options = new FindOneAndUpdateOptions();
				options.upsert(false);
				options.maxTime(5000, TimeUnit.MILLISECONDS);
				options.returnDocument(ReturnDocument.AFTER);
				
				 mongoTemplate.getCollection("DMMauSoKyHieu").findOneAndUpdate(findMSKH,
						new Document("$set", new Document("SoLuong", SLPhatHanhUpdate)
								.append("DenSo", SLPhatHanhUpdate)
								.append("ConLai", SLConLaiUpdate)
								.append("InvoiceStatus", true)
								.append("InfoUpdateSoLuongLech",
										new Document("UpdatedSoLuongLech", LocalDateTime.now())
												.append("SoLuong", SLConLaiUpdate)
												.append("UpdatedSoLuongLechserID", header.getUserId())
												.append("UpdatedSoLuongLechUserName", header.getUserName())
												.append("UpdatedSoLuongLechUserFullName", header.getUserFullName()))),
						options);	
			}
		}
			
	}
		
		
		responseStatus = new MspResponseStatus(0, "SUCCESS");
		rsp.setResponseStatus(responseStatus);
		return rsp;
	}

	public FileInfo exportExcel(JSONRoot jsonRoot) throws Exception {
		FileInfo fileInfo = new FileInfo();
		Msg msg = jsonRoot.getMsg();
		Object objData = msg.getObjData();

		String mst = "";
		String mskhieu = "";
		String name = "";
		String status = "";

		JsonNode jsonData = null;
		if (objData != null) {
			jsonData = Json.serializer().nodeFromObject(objData);
			mst = commons.getTextJsonNode(jsonData.at("/MST")).replaceAll("\\s", "");
			name = commons.getTextJsonNode(jsonData.at("/Name")).replaceAll("\\s+", " ");
			mskhieu = commons.getTextJsonNode(jsonData.at("/MSKH")).replaceAll("\\s", "");
			status = commons.getTextJsonNode(jsonData.at("/Status")).replaceAll("\\s", "");
		}

		File file = new File(SystemParams.DIR_E_INVOICE_TEMPLATE, Constants.TEMPLATE_FILE_NAME.EXCEL_TK_LECH_SHD);
		if(file == null || !file.exists() || !file.isFile()) {
			fileInfo.setContentFile(null);
			return fileInfo;
		}
		
		Iterable<Document> cursor = null;
		Iterator<Document> iter = null;
		
		Iterable<Document> cursor1 = null;
		Iterator<Document> iter1 = null;
		
		Iterable<Document> cursor3 = null;
		Iterator<Document> iter3 = null;
		
		List<Document> pipeline = new ArrayList<Document>();
		Document docTmp = null;		
		ByteArrayOutputStream out = null;
		Workbook wb = null;
    	Sheet sheet = null;
    	Row row = null;
		Cell cell = null;
		

		try {
			wb = WorkbookFactory.create(new FileInputStream(file));
			sheet = wb.getSheetAt(0);
			Font fontDetail = wb.createFont();
			fontDetail.setFontHeightInPoints((short)10);
			fontDetail.setFontName("Times New Roman");
			fontDetail.setItalic(false);
			
			CellStyle styleInfoL = null;
			styleInfoL = wb.createCellStyle();
			styleInfoL.setFont(fontDetail);
			styleInfoL.setLocked(false);
			styleInfoL.setAlignment(HorizontalAlignment.LEFT);
			styleInfoL.setVerticalAlignment(VerticalAlignment.CENTER);
			styleInfoL.setBorderBottom(BorderStyle.THIN);
			styleInfoL.setBorderTop(BorderStyle.THIN);
			styleInfoL.setBorderRight(BorderStyle.THIN);
			styleInfoL.setBorderLeft(BorderStyle.THIN);
			styleInfoL.setWrapText(true);
			
			CellStyle styleInfoC = null;
			styleInfoC  = wb.createCellStyle();
			styleInfoC.setFont(fontDetail);
			styleInfoC.setLocked(false);
			styleInfoC.setAlignment(HorizontalAlignment.CENTER);
			styleInfoC.setVerticalAlignment(VerticalAlignment.CENTER);
			styleInfoC.setBorderBottom(BorderStyle.THIN);
			styleInfoC.setBorderTop(BorderStyle.THIN);
			styleInfoC.setBorderRight(BorderStyle.THIN);
			styleInfoC.setBorderLeft(BorderStyle.THIN);
			styleInfoC.setWrapText(false);
			
			CellStyle styleInfoR = null;
			styleInfoR  = wb.createCellStyle();
			styleInfoR.setFont(fontDetail);
			styleInfoR.setLocked(false);
			styleInfoR.setAlignment(HorizontalAlignment.RIGHT);
			styleInfoR.setVerticalAlignment(VerticalAlignment.CENTER);								
			styleInfoR.setBorderBottom(BorderStyle.THIN);
			styleInfoR.setBorderTop(BorderStyle.THIN);
			styleInfoR.setBorderRight(BorderStyle.THIN);
			styleInfoR.setBorderLeft(BorderStyle.THIN);
			styleInfoR.setWrapText(false);
			
			DataFormat format = wb.createDataFormat();
			Short df = format.getFormat(wb.createDataFormat().getFormat(wb.createDataFormat().getFormat("#,##0")));
			CellStyle cellStyleNum = wb.createCellStyle();
			cellStyleNum.setDataFormat(df);							
			cellStyleNum.setBorderBottom(BorderStyle.THIN);
			cellStyleNum.setBorderTop(BorderStyle.THIN);
			cellStyleNum.setBorderRight(BorderStyle.THIN);
			cellStyleNum.setBorderLeft(BorderStyle.THIN);			
			cellStyleNum.setWrapText(false);
			
			
			
			CellStyle cellStyleCheck = wb.createCellStyle();
			cellStyleCheck.setDataFormat(df);							
			cellStyleCheck.setBorderBottom(BorderStyle.THIN);
			cellStyleCheck.setBorderTop(BorderStyle.THIN);
			cellStyleCheck.setBorderRight(BorderStyle.THIN);
			cellStyleCheck.setBorderLeft(BorderStyle.THIN);	
			cellStyleCheck.setAlignment(HorizontalAlignment.RIGHT);
			cellStyleCheck.setVerticalAlignment(VerticalAlignment.CENTER);
			//styleInfoR.setVerticalAlignment(VerticalAlignment.);	
			cellStyleCheck.setWrapText(false);
			
			int posRowData = 2;

			int countRow = 1;
			
			ObjectId issu = null;
			String issuerId = null;
			if (!mst.equals("") || !name.equals("")) {
				Document docTmp3 = null;
				Document findIssuer = new Document("IsDelete", new Document("$ne", true));

				if (!mst.equals(""))
					findIssuer.append("TaxCode", mst);

				if (!name.equals(""))
					findIssuer.append("Name",
							new Document("$regex", commons.regexEscapeForMongoQuery(name)).append("$options", "i"));
				
				cursor3 = mongoTemplate.getCollection("Issuer").find(findIssuer).allowDiskUse(true);
				iter3 = cursor3.iterator();
				if (iter3.hasNext()) {
					docTmp3 = iter3.next();
				}
				
				if (docTmp3 == null) {
					row = sheet.getRow(posRowData);
					if (null == row) row = sheet.createRow(posRowData);

					cell = row.getCell(0);
					if (cell == null) cell = row.createCell(0);
					cell.setCellStyle(styleInfoR);

					cell = row.getCell(1);
					if (cell == null) cell = row.createCell(1);
					cell.setCellStyle(styleInfoL);

					cell = row.getCell(2);
					if (cell == null) cell = row.createCell(2);
					cell.setCellStyle(styleInfoL);

					cell = row.getCell(3);
					if (cell == null) cell = row.createCell(3);
					cell.setCellStyle(styleInfoC);

					cell = row.getCell(4);
					if (cell == null) cell = row.createCell(4);
					cell.setCellStyle(styleInfoC);
					
					cell = row.getCell(5);
					if (cell == null) cell = row.createCell(5);
					cell.setCellStyle(styleInfoC);
					
					cell = row.getCell(6);
					if (cell == null) cell = row.createCell(6);
					cell.setCellStyle(styleInfoC);
					
					cell = row.getCell(7);
					if (cell == null) cell = row.createCell(7);
					cell.setCellStyle(styleInfoC);
					
					cell = row.getCell(8);
					if (cell == null) cell = row.createCell(8);
					cell.setCellStyle(styleInfoC);

					out = new ByteArrayOutputStream();
					wb.write(out);

					fileInfo = new FileInfo();
					fileInfo.setFileName("DANH-SACH-MAU-SO-LECH-SHD.xlsx");
					fileInfo.setContentFile(out.toByteArray());
					return fileInfo;
				}

				issu = docTmp3.getObjectId("_id");
				issuerId = issu.toString();
			}
			
			Document docMSKH = new Document("IsDelete", new Document("$ne", true)).append("IsActive", true);

			if (issuerId != null)
				docMSKH.append("IssuerId", issuerId);
			
			if (!mskhieu.equals(""))
				docMSKH.append("KHHDon", mskhieu);
			
			if(!status.equals("")) {
				if(status.equals("true")) {
					docMSKH.append("InvoiceStatus", true);
				}else {
					docMSKH.append("InvoiceStatus", new Document("$ne", true));			
				}
			}
			
			pipeline.add(new Document("$match", docMSKH));
			pipeline.add(new Document("$sort", new Document("NamPhatHanh", -1)));
			cursor = mongoTemplate.getCollection("DMMauSoKyHieu").aggregate(pipeline).allowDiskUse(true);
			iter = cursor.iterator();
			while(iter.hasNext()) {
				docTmp = iter.next();
				
				Document docTmp1 = null;

				String issuerid = docTmp.get("IssuerId", "").toString();
				ObjectId id_issuer = new ObjectId(issuerid);

				// FIND ISSUERID

				Document findIssuer = new Document("_id", id_issuer).append("IsDelete", false);			
				pipeline = new ArrayList<Document>();
				pipeline.add(new Document("$match", findIssuer));
				
				cursor1 = mongoTemplate.getCollection("Issuer").aggregate(pipeline).allowDiskUse(true);
				iter1 = cursor1.iterator();
				if (iter1.hasNext()) {
					docTmp1 = iter1.next();
				}

				String TaxCode = "";
				String Ten = "";
				if (docTmp1 == null) {
					TaxCode = "";
					Ten = "";

				} else {
					TaxCode = docTmp1.get("TaxCode", "");
					Ten = docTmp1.get("Name", "");
				}
				
				int SLMauMoi = docTmp.get("SoLuong", 0);
				String KHMSHDonMoi = docTmp.get("KHMSHDon", "");
				String KHHDonMoi = docTmp.get("KHHDon", "");
				int NamPhatHanhMoi = docTmp.get("NamPhatHanh", 0);
				
				Document docTmp2 = null;
				int SLLech = 0;
				int NamPhatHanhCu = NamPhatHanhMoi - 1;
				String KHMSHDonCu = KHMSHDonMoi;
				String namPhatHanhCuString = String.valueOf(NamPhatHanhCu);
				String kyTuCuoiNamCu = namPhatHanhCuString.substring(namPhatHanhCuString.length() - 2);				
				String KHHDonCu = KHHDonMoi.substring(0, 1) + kyTuCuoiNamCu + KHHDonMoi.substring(3);
				
				Document findOLDMSKH = new Document("KHMSHDon", KHMSHDonCu)
						.append("KHHDon", KHHDonCu)
						.append("NamPhatHanh", NamPhatHanhCu)
						.append("IssuerId", issuerid)
						.append("IsDelete", new Document("$ne", true));
	
				Iterable<Document> cursor2 = mongoTemplate.getCollection("DMMauSoKyHieu").find(findOLDMSKH);
				Iterator<Document> iter2 = cursor2.iterator();
				if (iter2.hasNext()) {
					docTmp2 = iter2.next();
				}
				
				if (docTmp2 == null) {
					SLLech = 0;
				}else {
					int SLConLaiNamCu = docTmp2.get("ConLai", 0);	
				
					SLLech = SLMauMoi - SLConLaiNamCu;
				}
				
				String InvoiceStatus = "";
				if(docTmp.get("InvoiceStatus", false) == true) {
					InvoiceStatus = "Đã cập nhật";
				}else {
					InvoiceStatus = "Chưa cập nhật";
				}
			
				String SoLuongNotEnough = "";
				if(docTmp.get("SoLuongNotEnough", false) == true) {
					SoLuongNotEnough = "Số lượng không đủ";
				}
				
				row = sheet.getRow(posRowData);
				if(null == row) row = sheet.createRow(posRowData);
				
				cell = row.getCell(0);if(cell == null) cell = row.createCell(0);cell.setCellStyle(styleInfoR);
				cell.setCellValue(countRow);
				
				cell = row.getCell(1);if(cell == null) cell = row.createCell(1);cell.setCellStyle(styleInfoC);
				cell.setCellValue(TaxCode);

				cell = row.getCell(2);if(cell == null) cell = row.createCell(2);cell.setCellStyle(styleInfoC);
				cell.setCellValue(Ten);
				
				cell = row.getCell(3);if(cell == null) cell = row.createCell(3);cell.setCellStyle(styleInfoL);
				cell.setCellValue(docTmp.get("KHMSHDon", "") + docTmp.get("KHHDon", ""));
				
				cell = row.getCell(4);if(cell == null) cell = row.createCell(4);cell.setCellStyle(styleInfoL);
				cell.setCellValue(docTmp.get("SoLuong", 0));
				
				cell = row.getCell(5);if(cell == null) cell = row.createCell(5);cell.setCellStyle(styleInfoL);
				cell.setCellValue(docTmp.get("ConLai", 0));
				
				cell = row.getCell(6);if(cell == null) cell = row.createCell(6);cell.setCellStyle(styleInfoL);
				cell.setCellValue(SLLech);
				
				cell = row.getCell(7);if(cell == null) cell = row.createCell(7);cell.setCellStyle(styleInfoL);
				cell.setCellValue(InvoiceStatus);
				
				cell = row.getCell(8);if(cell == null) cell = row.createCell(8);cell.setCellStyle(styleInfoL);
				cell.setCellValue(SoLuongNotEnough);
			
				posRowData++;	
				countRow++;
			}
			
			
			out = new ByteArrayOutputStream();
			wb.write(out);
			fileInfo.setFileName("DANH-SACH-MAU-SO-LECH-SHD.xlsx");
			fileInfo.setContentFile(out.toByteArray());
			return fileInfo;
		} catch (Exception e) {
			throw e;
		} finally {
			try{wb.close();}catch(Exception ex){}
		}
	}

}
