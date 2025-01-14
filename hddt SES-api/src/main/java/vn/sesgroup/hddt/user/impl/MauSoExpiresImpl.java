package vn.sesgroup.hddt.user.impl;

import java.io.ByteArrayOutputStream;
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
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
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
import com.fasterxml.jackson.databind.JsonNode;
import com.mongodb.client.model.FindOneAndUpdateOptions;
import com.mongodb.client.model.ReturnDocument;

import vn.sesgroup.hddt.dto.FileInfo;
import vn.sesgroup.hddt.user.dao.AbstractDAO;
import vn.sesgroup.hddt.user.dao.MauSoExpiresDAO;
import vn.sesgroup.hddt.user.service.TCTNService;
import vn.sesgroup.hddt.utility.Constants;
import vn.sesgroup.hddt.utility.Json;

@Repository
@Transactional
public class MauSoExpiresImpl extends AbstractDAO implements MauSoExpiresDAO {
	@Autowired MongoTemplate mongoTemplate;
	@Autowired TCTNService tctnService;

	String SUMslhd = "";

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
		Iterable<Document> cursor = null;
		Iterator<Document> iter = null;
	
		String actionCode = header.getActionCode();
		String id = commons.getTextJsonNode(jsonData.at("/_id"));
		
		MsgRsp rsp = new MsgRsp(header);
		rsp.setMsgPage(page);
		MspResponseStatus responseStatus = null;
		
		List<Document> pipeline = null;
		Document docFind = null;
		Document docTmp = null;
		FindOneAndUpdateOptions options = null;
		ObjectId objectId = null;

		switch (actionCode) {
		case Constants.MSG_ACTION_CODE.ACTIVE:
			objectId = null;
		
			try {
				objectId = new ObjectId(id);
			}catch(Exception ex) {}
			
			
			/*KIEM TRA THONG TIN KHACH HANG - USER - TINH THANH - CO QUAN THUE CO TON TAI KHONG*/
			docFind = new Document("_id", objectId).append("IsActive", true).append("IsDelete", new Document("$ne", true));
			pipeline = new ArrayList<Document>();
			pipeline.add(new Document("$match", docFind));
			pipeline.add(new Document("$project", new Document("_id", 1)));
			
			cursor = mongoTemplate.getCollection("DMMauSoKyHieu").aggregate(pipeline).allowDiskUse(true);
			iter = cursor.iterator();
			if (iter.hasNext()) {
				docTmp = iter.next();
			}
			
			if(null == docTmp) {
				responseStatus = new MspResponseStatus(9999, "Không tìm thấy thông tin mẫu số.");
				rsp.setResponseStatus(responseStatus);
				return rsp;
			}
			
			options = new FindOneAndUpdateOptions();
			options.upsert(false);
			options.maxTime(5000, TimeUnit.MILLISECONDS);
			options.returnDocument(ReturnDocument.AFTER);
				
			 mongoTemplate.getCollection("DMMauSoKyHieu").findOneAndUpdate(docFind,
					new Document("$set",
							new Document("Status", true)		
							),
					options);
		
		responseStatus = new MspResponseStatus(0, "SUCCESS");
		rsp.setResponseStatus(responseStatus);
		return rsp;
		
		case Constants.MSG_ACTION_CODE.DEACTIVE:
			objectId = null;
			
			try {
				objectId = new ObjectId(id);
			}catch(Exception ex) {}
			
			
			/*KIEM TRA THONG TIN KHACH HANG - USER - TINH THANH - CO QUAN THUE CO TON TAI KHONG*/
			docFind = new Document("_id", objectId).append("IsActive", true).append("IsDelete", new Document("$ne", true));
			pipeline = new ArrayList<Document>();
			pipeline.add(new Document("$match", docFind));
			pipeline.add(new Document("$project", new Document("_id", 1)));	
			
			cursor = mongoTemplate.getCollection("DMMauSoKyHieu").aggregate(pipeline).allowDiskUse(true);
			iter = cursor.iterator();
			if (iter.hasNext()) {
				docTmp = iter.next();
			}
			
			if(null == docTmp) {
				responseStatus = new MspResponseStatus(9999, "Không tìm thấy thông tin mẫu số.");
				rsp.setResponseStatus(responseStatus);
				return rsp;
			}
			
			options = new FindOneAndUpdateOptions();
			options.upsert(false);
			options.maxTime(5000, TimeUnit.MILLISECONDS);
			options.returnDocument(ReturnDocument.AFTER);
	
			mongoTemplate.getCollection("DMMauSoKyHieu").findOneAndUpdate(docFind,
					new Document("$set",
							new Document("Status", false)		
							),
					options);
		
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
		MsgPage page = msg.getMsgPage();
		Object objData = msg.getObjData();

		String status = "";
		String mst = "";
		String tyle = "";

		Iterable<Document> cursor = null;
		Iterator<Document> iter = null;
		
		JsonNode jsonData = null;
		if (objData != null) {
			jsonData = Json.serializer().nodeFromObject(objData);
			status = commons.getTextJsonNode(jsonData.at("/Status")).replaceAll("\\s", "");
			mst = commons.getTextJsonNode(jsonData.at("/TaxCode")).replaceAll("\\s", "");
			tyle = commons.getTextJsonNode(jsonData.at("/TyLe")).replaceAll("\\s", "");
		}

		MsgRsp rsp = new MsgRsp(header);
		MspResponseStatus responseStatus = null;
		Document docTmp = null;

		List<Document> pipeline = new ArrayList<Document>();

		Document docCheck = new Document("ActiveFlag", true);
		Document docMatch = new Document("IsDelete", new Document("$ne", true)).append("NamPhatHanh", 2023);

		String issuer_ = null;
		if (!"".equals(mst)) {
			Document findIssuer = new Document("TaxCode", mst).append("IsDelete", new Document("$ne", true));		
			
			pipeline = new ArrayList<Document>();
			pipeline.add(new Document("$match", findIssuer));
			pipeline.add(new Document("$project", new Document("_id", 1)));
			
			cursor = mongoTemplate.getCollection("Issuer").aggregate(pipeline).allowDiskUse(true);
			iter = cursor.iterator();
			if (iter.hasNext()) {
				docTmp = iter.next();
			}
			
			
			if (docTmp == null) {
				responseStatus = new MspResponseStatus(9999, Constants.MAP_ERROR.get(9999));
				rsp.setResponseStatus(responseStatus);
				return rsp;
			}
			issuer_ = docTmp.getObjectId("_id").toString();
		}

		if (issuer_ != null)
			docMatch.append("IssuerId", commons.regexEscapeForMongoQuery(issuer_));

		if(!status.equals("")) {
			if(status.equals("true")) {
				docMatch.append("Status", true);
			}else {
				docMatch.append("$or", Arrays.asList(		
						new Document("Status", false),
						new Document("Status", null)				
						));
			}

		}
	
		pipeline = new ArrayList<Document>();
		pipeline.add(new Document("$match", docCheck));
		pipeline.add(new Document("$lookup", new Document("from", "DMMauSoKyHieu")
				.append("pipeline", Arrays.asList(new Document("$match", docMatch)
						,new Document("$project", new Document("_id", 1).append("IssuerId", 1).append("Status", 1)
								.append("SoLuong", 1).append("ConLai", 1).append("KHMSHDon", 1).append("KHHDon", 1)
								)
						))
				.append("as", "DMMauSoKyHieu")));
		
		cursor = mongoTemplate.getCollection("ApiLicenseKey").aggregate(pipeline).allowDiskUse(true);
		iter = cursor.iterator();
		if (iter.hasNext()) {
			docTmp = iter.next();
		}

		rsp = new MsgRsp(header);
		responseStatus = null;
		if (docTmp.getList("DMMauSoKyHieu", Document.class).size() == 0) {
			responseStatus = new MspResponseStatus(9999, Constants.MAP_ERROR.get(9999));
			rsp.setResponseStatus(responseStatus);
			return rsp;
		}

		List<Document> rows = null;
		if (docTmp.get("DMMauSoKyHieu") != null && docTmp.get("DMMauSoKyHieu") instanceof List) {
			rows = docTmp.getList("DMMauSoKyHieu", Document.class);
		}

		page.setTotalRows(rows.size());
		rsp.setMsgPage(page);

		ArrayList<HashMap<String, Object>> rowsReturn = new ArrayList<HashMap<String, Object>>();
		HashMap<String, Object> hItem = null;
		if (null != rows) {
			for (Document doc : rows) {
				String _id = doc.getObjectId("_id").toString();
				String IssuerId = doc.get("IssuerId", "");
				ObjectId issu = new ObjectId(IssuerId);
				boolean Status = doc.get("Status", false);
				String trangthai = "";
				if(Status == false) {
					trangthai = "Chưa gia hạn";
				}else {
					trangthai = "Đã gia hạn";
				}
				
				docTmp = null;

				Document findIssuer = new Document("_id", issu).append("IsDelete", new Document("$ne", true));
				
				pipeline = new ArrayList<Document>();
				pipeline.add(new Document("$match", findIssuer));
				pipeline.add(new Document("$project", new Document("_id", 1).append("TaxCode", 1).append("Name", 1)
						));
				
				cursor = mongoTemplate.getCollection("Issuer").aggregate(pipeline).allowDiskUse(true);
				iter = cursor.iterator();
				if (iter.hasNext()) {
					docTmp = iter.next();
				}

				if(docTmp == null) {
					continue;
				}
				
				String MST = docTmp.get("TaxCode", "");
				String Ten = docTmp.get("Name", "");
				int SoLuong = doc.get("SoLuong", 0);
				int ConLai = doc.get("ConLai", 0);
				int SLDD = SoLuong - ConLai;
				int tile = 0;
				if (ConLai == 0) {
					tile = 100;
				} else {
					tile = (ConLai * 100) / SoLuong;
				}

				if (!"".equals(tyle)) {
					int check_tl = Integer.parseInt(tyle);
					if (check_tl >= tile) {
					
							hItem = new HashMap<String, Object>();
							hItem.put("_id", _id);
							hItem.put("TaxCode", MST);
							hItem.put("Name", Ten);
							hItem.put("MSHDon", doc.get("KHMSHDon", "") + doc.get("KHHDon", ""));
							hItem.put("SoLuong", SoLuong);
							hItem.put("SoLuongConLai", ConLai);
							hItem.put("SoLuongDaDung", SLDD);
							hItem.put("TiLe", tile);
							hItem.put("Status", trangthai);
							rowsReturn.add(hItem);
						
					}
				} else {
					
						hItem = new HashMap<String, Object>();
						hItem.put("_id", _id);
						hItem.put("TaxCode", MST);
						hItem.put("Name", Ten);
						hItem.put("MSHDon", doc.get("KHMSHDon", "") + doc.get("KHHDon", ""));
						hItem.put("SoLuong", SoLuong);
						hItem.put("SoLuongConLai", ConLai);
						hItem.put("SoLuongDaDung", SLDD);
						hItem.put("TiLe", tile);
						hItem.put("Status", trangthai);
						rowsReturn.add(hItem);
					
				}
			}
		}

		if (rowsReturn.size() == 0) {
			responseStatus = new MspResponseStatus(9999, Constants.MAP_ERROR.get(9999));
			rsp.setResponseStatus(responseStatus);
			return rsp;
		}


		/// ===============================================================================
		ArrayList<HashMap<String, Object>> rowsReturn1 = new ArrayList<HashMap<String, Object>>();
		page.setTotalRows(rowsReturn.size());

		int pageNo = page.getPageNo();
		int size = page.getSize();
		int TotalRows = rowsReturn.size();

		int sotrang = size * (pageNo - 1);
		int dem = 0;
		for (int i = sotrang; i <= TotalRows; i++) {
			sotrang++;
			dem++;
			rowsReturn1.add(rowsReturn.get(i));

			if (dem == size || dem == TotalRows || sotrang == TotalRows) {
				break;
			}
		}

		rsp.setMsgPage(page);
		rsp.getMsgHeader().setAdmin(true);

		String SL_EXPIRES = String.valueOf(rowsReturn.size());
		responseStatus = new MspResponseStatus(0, SL_EXPIRES);
		rsp.setResponseStatus(responseStatus);
		HashMap<String, Object> mapDataR = new HashMap<String, Object>();
		mapDataR.put("rows", rowsReturn1);
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

		Document docFind = new Document("_id", objectId);
		Iterable<Document> cursor = null;
		Iterator<Document> iter = null;
		Document docTmp = null;
		
		cursor = mongoTemplate.getCollection("Contract").find(docFind).allowDiskUse(true);
		iter = cursor.iterator();
		if (iter.hasNext()) {
			docTmp = iter.next();
		}
		
		if (null == docTmp) {
			responseStatus = new MspResponseStatus(9999, Constants.MAP_ERROR.get(9999));
			rsp.setResponseStatus(responseStatus);
			return rsp;
		}

		rsp.setObjData(docTmp);
		responseStatus = new MspResponseStatus(0, Constants.MAP_ERROR.get(0));
		rsp.setResponseStatus(responseStatus);
		return rsp;
	}

	public FileInfo exportExcel(JSONRoot jsonRoot) throws Exception {
		Msg msg = jsonRoot.getMsg();
		Object objData = msg.getObjData();

		String tyle = "";
		String mst = "";
		String status = "";

		JsonNode jsonData = null;
		if (objData != null) {
			jsonData = Json.serializer().nodeFromObject(objData);
			status = commons.getTextJsonNode(jsonData.at("/Status")).replaceAll("\\s", "");
			mst = commons.getTextJsonNode(jsonData.at("/TaxCode")).replaceAll("\\s", "");
			tyle = commons.getTextJsonNode(jsonData.at("/TyLe")).replaceAll("\\s", "");
		}

		Document docTmp = null;
		List<Document> pipeline = new ArrayList<Document>();
		ByteArrayOutputStream out = null;
		SXSSFWorkbook wb = new SXSSFWorkbook(1000);
		Sheet sheet = null;
		try {
			sheet = wb.createSheet("Sheet 1");
			Row row = null;
			Cell cell = null;

			Font fontHeader = wb.createFont();
			fontHeader.setFontHeightInPoints((short) 13);
			fontHeader.setFontName("Times New Roman");
			fontHeader.setItalic(false);
			fontHeader.setBold(true);
			fontHeader.setColor(IndexedColors.WHITE.index);

			CellStyle styleHeader = null;
			styleHeader = wb.createCellStyle();
			styleHeader.setLocked(false);
			styleHeader.setAlignment(HorizontalAlignment.CENTER);
			styleHeader.setVerticalAlignment(VerticalAlignment.CENTER);

			styleHeader.setBorderBottom(BorderStyle.THIN);
			styleHeader.setBorderTop(BorderStyle.THIN);
			styleHeader.setBorderRight(BorderStyle.THIN);
			styleHeader.setBorderLeft(BorderStyle.THIN);
			styleHeader.setWrapText(true);
			styleHeader.setFont(fontHeader);

			styleHeader.setFillForegroundColor(IndexedColors.GREEN.index);
			styleHeader.setFillPattern(FillPatternType.SOLID_FOREGROUND);

			Font fontDetail = wb.createFont();
			fontDetail.setFontHeightInPoints((short) 13);
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
			styleInfoC = wb.createCellStyle();
			styleInfoC.setFont(fontDetail);
			styleInfoC.setLocked(false);
			styleInfoC.setAlignment(HorizontalAlignment.CENTER);
			styleInfoC.setVerticalAlignment(VerticalAlignment.CENTER);
			styleInfoC.setBorderBottom(BorderStyle.THIN);
			styleInfoC.setBorderTop(BorderStyle.THIN);
			styleInfoC.setBorderRight(BorderStyle.THIN);
			styleInfoC.setBorderLeft(BorderStyle.THIN);
			styleInfoC.setWrapText(true);

			CellStyle styleInfoR = null;
			styleInfoR = wb.createCellStyle();
			styleInfoR.setFont(fontDetail);
			styleInfoR.setLocked(false);
			styleInfoR.setAlignment(HorizontalAlignment.RIGHT);
			styleInfoR.setVerticalAlignment(VerticalAlignment.CENTER);
			styleInfoR.setBorderBottom(BorderStyle.THIN);
			styleInfoR.setBorderTop(BorderStyle.THIN);
			styleInfoR.setBorderRight(BorderStyle.THIN);
			styleInfoR.setBorderLeft(BorderStyle.THIN);
			styleInfoR.setAlignment(HorizontalAlignment.CENTER);
			styleInfoR.setWrapText(true);

			DataFormat format = wb.createDataFormat();
			Short df = format.getFormat(wb.createDataFormat().getFormat(wb.createDataFormat().getFormat("#,##0")));
			CellStyle cellStyleNum = wb.createCellStyle();
			cellStyleNum.setDataFormat(df);
			cellStyleNum.setBorderBottom(BorderStyle.THIN);
			cellStyleNum.setBorderTop(BorderStyle.THIN);
			cellStyleNum.setBorderRight(BorderStyle.THIN);
			cellStyleNum.setBorderLeft(BorderStyle.THIN);
			cellStyleNum.setWrapText(false);

			int countRow = 1;
			List<String> headers = Arrays.asList(new String[] { "STT", "Tên đơn vị", "Mã số thuế", "Mẫu số hóa đơn",
					"Tỷ lệ (% số lượng còn lại)", "Số lượng phát hành", "Số lượng còn lại", "Số lượng đã dùng", "Trạng thái" });

			row = sheet.getRow(0);
			if (null == row)
				row = sheet.createRow(0);
			row.setHeight((short) 500);
			for (int i = 0; i < headers.size(); i++) {
				cell = row.getCell(i);
				if (cell == null)
					cell = row.createCell(i);
				cell.setCellStyle(styleHeader);
				cell.setCellValue(headers.get(i));
				if (i == 0)
					sheet.setColumnWidth(i, 2000);
				else if (i == 2)
					sheet.setColumnWidth(i, 4500);
				else if (i == 1)
					sheet.setColumnWidth(i, 8000);
				else if (i == 4)
					sheet.setColumnWidth(i, 7500);
				else if (i == 3)
					sheet.setColumnWidth(i, 5000);
				else if (i == 5)
					sheet.setColumnWidth(i, 6000);
				else if (i == 6)
					sheet.setColumnWidth(i, 5500);
				else if (i == 7)
					sheet.setColumnWidth(i, 5000);
				else if (i == 8)
					sheet.setColumnWidth(i, 7000);
				else
					sheet.setColumnWidth(i, 3000);

			}
				int posRowData = 1;

				pipeline = new ArrayList<Document>();

				Document docCheck = new Document("ActiveFlag", true);
				Document docMatch = new Document("IsDelete", new Document("$ne", true)).append("NamPhatHanh", 2023);

				String issuer_ = null;
				if (!"".equals(mst)) {
					Document findIssuer = new Document("TaxCode", mst).append("IsDelete", new Document("$ne", true));
					
					pipeline = new ArrayList<Document>();
					pipeline.add(new Document("$match", findIssuer));
					pipeline.add(new Document("$project", new Document("_id", 1)));
					
					Iterable<Document> cursor = mongoTemplate.getCollection("Issuer").aggregate(pipeline).allowDiskUse(true);
					Iterator<Document> iter = cursor.iterator();
					if (iter.hasNext()) {
						docTmp = iter.next();
					}
					
					if (docTmp == null) {
						// DIEU KIEN CHECK SO LUONG
						row = sheet.getRow(posRowData);
						if (null == row)
							row = sheet.createRow(posRowData);
						cell = row.getCell(0);
						if (cell == null)
							cell = row.createCell(0);
						cell.setCellStyle(styleInfoR);
						cell.setCellValue(countRow);

						cell = row.getCell(1);
						if (cell == null)
							cell = row.createCell(1);
						cell.setCellStyle(styleInfoL);

						cell = row.getCell(2);
						if (cell == null)
							cell = row.createCell(2);
						cell.setCellStyle(styleInfoL);

						cell = row.getCell(3);
						if (cell == null)
							cell = row.createCell(3);
						cell.setCellStyle(styleInfoC);

						cell = row.getCell(4);
						if (cell == null)
							cell = row.createCell(4);
						cell.setCellStyle(styleInfoC);

						cell = row.getCell(5);
						if (cell == null)
							cell = row.createCell(5);
						cell.setCellStyle(styleInfoC);

						cell = row.getCell(6);
						if (cell == null)
							cell = row.createCell(6);
						cell.setCellStyle(styleInfoL);

						cell = row.getCell(7);
						if (cell == null)
							cell = row.createCell(7);
						cell.setCellStyle(styleInfoL);
						
						cell = row.getCell(8);
						if (cell == null)
							cell = row.createCell(8);
						cell.setCellStyle(styleInfoL);

						posRowData++;
						countRow++;

					}
					issuer_ = docTmp.getObjectId("_id").toString();
				}

				if (issuer_ != null)
					docMatch.append("IssuerId", commons.regexEscapeForMongoQuery(issuer_));

				
				if(!status.equals("")) {
					if(status.equals("true")) {
						docMatch.append("Status", true);
					}else {
						docMatch.append("$or", Arrays.asList(		
								new Document("Status", false),
								new Document("Status", null)				
								));
					}

				}
				
				pipeline = new ArrayList<Document>();
				pipeline.add(new Document("$match", docCheck));
				pipeline.add(new Document("$lookup",
						new Document("from", "DMMauSoKyHieu")
								.append("pipeline", Arrays.asList(new Document("$match", docMatch)
										,new Document("$project", new Document("_id", 1).append("IssuerId", 1).append("Status", 1)
												.append("SoLuong", 1).append("ConLai", 1).append("KHMSHDon", 1).append("KHHDon", 1)
												)
										))
								.append("as", "DMMauSoKyHieu")));


				Iterable<Document> cursor = mongoTemplate.getCollection("ApiLicenseKey").aggregate(pipeline).allowDiskUse(true);
				Iterator<Document> iter = cursor.iterator();
				if (iter.hasNext()) {
					docTmp = iter.next();
				}
				
				List<Document> rows = null;
				if (docTmp.get("DMMauSoKyHieu") != null && docTmp.get("DMMauSoKyHieu") instanceof List) {
					rows = docTmp.getList("DMMauSoKyHieu", Document.class);
				}
				
				if (null != rows) {
					for (Document doc : rows) {
						
						String IssuerId = doc.get("IssuerId", "");
						ObjectId issu = new ObjectId(IssuerId);
						
						boolean Status = doc.get("Status", false);
						String trangthai = "";
						if(Status == false) {
							trangthai = "Chưa gia hạn";
						}else {
							trangthai = "Đã gia hạn";
						}
		
						docTmp = null;

						Document findIssuer = new Document("_id", issu).append("IsDelete", new Document("$ne", true));
						pipeline = new ArrayList<Document>();
						pipeline.add(new Document("$match", findIssuer));
						pipeline.add(new Document("$project", new Document("_id", 1).append("TaxCode", 1).append("Name", 1)));
				
						
						Iterable<Document> cursor1 = mongoTemplate.getCollection("Issuer").aggregate(pipeline).allowDiskUse(true);
						Iterator<Document> iter1 = cursor1.iterator();
						if (iter1.hasNext()) {
							docTmp = iter1.next();
						}					

						if(docTmp == null) {
						continue;
						}
						
						String MST = docTmp.get("TaxCode", "");
						String Ten = docTmp.get("Name", "");
						
						int SoLuong = doc.get("SoLuong", 0);
						int ConLai = doc.get("ConLai", 0);
						int SLDD = SoLuong - ConLai;
						int tile = 0;
						if (ConLai == 0) {
							tile = 100;
						} else {
							tile = (ConLai * 100) / SoLuong;
						}

						if (!"".equals(tyle)) {
							int check_tl = Integer.parseInt(tyle);
							if (check_tl >= tile) {
								
									row = sheet.getRow(posRowData);
									if (null == row)
										row = sheet.createRow(posRowData);
									cell = row.getCell(0);
									if (cell == null)
										cell = row.createCell(0);
									cell.setCellStyle(styleInfoR);
									cell.setCellValue(countRow);

									cell = row.getCell(1);
									if (cell == null)
										cell = row.createCell(1);
									cell.setCellStyle(styleInfoL);
									cell.setCellValue(Ten);

									cell = row.getCell(2);
									if (cell == null)
										cell = row.createCell(2);
									cell.setCellStyle(styleInfoL);
									cell.setCellValue(MST);

									cell = row.getCell(3);
									if (cell == null)
										cell = row.createCell(3);
									cell.setCellStyle(styleInfoC);
									cell.setCellValue(doc.get("KHMSHDon", "") + doc.get("KHHDon", ""));

									cell = row.getCell(4);
									if (cell == null)
										cell = row.createCell(4);
									cell.setCellStyle(styleInfoC);
									cell.setCellValue(tile);

									cell = row.getCell(5);
									if (cell == null)
										cell = row.createCell(5);
									cell.setCellStyle(styleInfoC);
									cell.setCellValue(SoLuong);

									cell = row.getCell(6);
									if (cell == null)
										cell = row.createCell(6);
									cell.setCellStyle(styleInfoL);
									cell.setCellValue(ConLai);

									cell = row.getCell(7);
									if (cell == null)
										cell = row.createCell(7);
									cell.setCellStyle(styleInfoL);
									cell.setCellValue(SLDD);
									
									cell = row.getCell(8);
									if (cell == null)
										cell = row.createCell(8);
									cell.setCellStyle(styleInfoL);
									cell.setCellValue(trangthai);
									
									posRowData++;
									countRow++;

								
							}
						} else {
							
								row = sheet.getRow(posRowData);
								if (null == row)
									row = sheet.createRow(posRowData);
								cell = row.getCell(0);
								if (cell == null)
									cell = row.createCell(0);
								cell.setCellStyle(styleInfoR);
								cell.setCellValue(countRow);

								cell = row.getCell(1);
								if (cell == null)
									cell = row.createCell(1);
								cell.setCellStyle(styleInfoL);
								cell.setCellValue(Ten);

								cell = row.getCell(2);
								if (cell == null)
									cell = row.createCell(2);
								cell.setCellStyle(styleInfoL);
								cell.setCellValue(MST);

								cell = row.getCell(3);
								if (cell == null)
									cell = row.createCell(3);
								cell.setCellStyle(styleInfoC);
								cell.setCellValue(doc.get("KHMSHDon", "") + doc.get("KHHDon", ""));

								cell = row.getCell(4);
								if (cell == null)
									cell = row.createCell(4);
								cell.setCellStyle(styleInfoC);
								cell.setCellValue(tile);

								cell = row.getCell(5);
								if (cell == null)
									cell = row.createCell(5);
								cell.setCellStyle(styleInfoC);
								cell.setCellValue(SoLuong);

								cell = row.getCell(6);
								if (cell == null)
									cell = row.createCell(6);
								cell.setCellStyle(styleInfoL);
								cell.setCellValue(ConLai);

								cell = row.getCell(7);
								if (cell == null)
									cell = row.createCell(7);
								cell.setCellStyle(styleInfoL);
								cell.setCellValue(SLDD);
								
								cell = row.getCell(8);
								if (cell == null)
									cell = row.createCell(8);
								cell.setCellStyle(styleInfoL);
								cell.setCellValue(trangthai);
								
								posRowData++;
								countRow++;
						
						
					}
				}
			}

			out = new ByteArrayOutputStream();
			wb.write(out);

			FileInfo fileInfo = new FileInfo();
			fileInfo.setFileName("DANH-SACH-MAU-SO-KHACH-HANG.xlsx");
			fileInfo.setContentFile(out.toByteArray());
			return fileInfo;

		} catch (Exception e) {
			throw e;
		} finally {
			try {
				wb.dispose();
				wb.close();
			} catch (Exception ex) {
			}
		}
	}

}
