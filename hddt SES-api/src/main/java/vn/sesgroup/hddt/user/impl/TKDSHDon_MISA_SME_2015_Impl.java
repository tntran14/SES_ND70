package vn.sesgroup.hddt.user.impl;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
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
import com.fasterxml.jackson.databind.JsonNode;

import vn.sesgroup.hddt.dto.FileInfo;
import vn.sesgroup.hddt.user.dao.AbstractDAO;
import vn.sesgroup.hddt.user.dao.TKDSHDon_MISA_SME_2015_DAO;
import vn.sesgroup.hddt.utility.Constants;
import vn.sesgroup.hddt.utility.Json;
import vn.sesgroup.hddt.utility.SystemParams;

@Repository
@Transactional
public class TKDSHDon_MISA_SME_2015_Impl extends AbstractDAO implements TKDSHDon_MISA_SME_2015_DAO{
	private static final Logger log = LogManager.getLogger(TKDSHDon_MISA_SME_2015_Impl.class);
	@Autowired MongoTemplate mongoTemplate;
	
	private List<Document> buildListPipeline(Object objData, MsgHeader header) throws Exception{
		List<Document> pipeline = null;
		String mauSoHdon = "";
		String soHoaDon = "";
		String fromDate = "";
		String toDate = "";
		String nmuaMst = "";
		String nmuaTen = "";
		
		JsonNode jsonData = null;
		if(objData != null) {
			jsonData = Json.serializer().nodeFromObject(objData);
			
			mauSoHdon = commons.getTextJsonNode(jsonData.at("/MauSoHdon")).replaceAll("\\s", "");
			soHoaDon = commons.getTextJsonNode(jsonData.at("/SoHoaDon")).replaceAll("\\s", "");
			fromDate = commons.getTextJsonNode(jsonData.at("/FromDate")).replaceAll("\\s", "");
			toDate = commons.getTextJsonNode(jsonData.at("/ToDate")).replaceAll("\\s", "");
			nmuaMst = commons.getTextJsonNode(jsonData.at("/NmuaMst")).trim().replaceAll("\\s+", "");
			nmuaTen = commons.getTextJsonNode(jsonData.at("/NmuaTen")).trim().replaceAll("\\s+", " ");
		}
		
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
		docMatch.append("EInvoiceStatus", new Document("$in", Arrays.asList(Constants.INVOICE_STATUS.DELETED,Constants.INVOICE_STATUS.ERROR_CQT,Constants.INVOICE_STATUS.COMPLETE,Constants.INVOICE_STATUS.REPLACED, Constants.INVOICE_STATUS.ADJUSTED )));
		
		if(!"".equals(mauSoHdon))
			docMatch.append("EInvoiceDetail.TTChung.MauSoHD", commons.regexEscapeForMongoQuery(mauSoHdon));
		if(!"".equals(soHoaDon))
			docMatch.append("EInvoiceDetail.TTChung.SHDon", commons.stringToInteger(soHoaDon));
		if(null != docMatchDate)
			docMatch.append("EInvoiceDetail.TTChung.NLap", docMatchDate);
		
		if(!"".equals(nmuaMst))
			docMatch.append("EInvoiceDetail.NDHDon.NMua.MST", new Document("$regex", commons.regexEscapeForMongoQuery(nmuaMst)).append("$options", "i"));
		if(!"".equals(nmuaTen)) {
			docMatch.append("$or", 
				Arrays.asList(
					new Document("EInvoiceDetail.NDHDon.NMua.Ten", new Document("$regex", commons.regexEscapeForMongoQuery(nmuaTen)).append("$options", "i")),
					new Document("EInvoiceDetail.NDHDon.NMua.HVTNMHang", new Document("$regex", commons.regexEscapeForMongoQuery(nmuaTen)).append("$options", "i"))
				)
			);
		}
		
		pipeline = new ArrayList<Document>();
		pipeline.add(new Document("$match", docMatch));
		pipeline.add(
			new Document("$addFields", 
				new Document("SHDon", 
					new Document("$ifNull", Arrays.asList("$EInvoiceDetail.TTChung.SHDon", Integer.MAX_VALUE))
				)
			)
		);
		pipeline.add(
			new Document("$sort", 
				new Document("EInvoiceDetail.TTChung.MauSoHD", -1).append("SHDon", 1).append("_id", -1)
			)
		);
		
		return pipeline;
	}
	
	@Override
	public MsgRsp list(JSONRoot jsonRoot) throws Exception {
		Msg msg = jsonRoot.getMsg();
		MsgHeader header = msg.getMsgHeader();
		MsgPage page = msg.getMsgPage();
		Object objData = msg.getObjData();
		
		MsgRsp rsp = new MsgRsp(header);
		MspResponseStatus responseStatus = null;
		
		ObjectId objectId = null;
		Document docTmp = null;
		Iterable<Document> cursor = null;
		Iterator<Document> iter = null;
		List<Document> pipeline = new ArrayList<Document>();
		
		pipeline = buildListPipeline(objData, header);
		pipeline.addAll(createFacetForSearchNotSort(page));
		
		cursor = mongoTemplate.getCollection("EInvoice").aggregate(pipeline).allowDiskUse(true);
		iter = cursor.iterator();
		if(iter.hasNext()) {
			docTmp = iter.next();
		}
		
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
				hItem.put("EInvoiceStatus", doc.get("EInvoiceStatus"));
				hItem.put("SignStatusCode", doc.get("SignStatusCode"));
				hItem.put("MCCQT", doc.get("MCCQT"));
				
				hItem.put("EInvoiceDetail", doc.get("EInvoiceDetail"));
				hItem.put("InfoCreated", doc.get("InfoCreated"));
				hItem.put("LDo", doc.get("LDo"));
				
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
	public FileInfo exportExcelDSHDVAT(JSONRoot jsonRoot) throws Exception {
		FileInfo fileInfo = new FileInfo();
		
		Msg msg = jsonRoot.getMsg();
		MsgHeader header = msg.getMsgHeader();
		MsgPage page = msg.getMsgPage();
		Object objData = msg.getObjData();
		
		List<Document> pipeline = new ArrayList<Document>();
		pipeline = buildListPipeline(objData, header);
		
		File file = new File(SystemParams.DIR_E_INVOICE_TEMPLATE, Constants.TEMPLATE_FILE_NAME.EXCEL_TKDSHD_MISA_SME_2015);
		if(file == null || !file.exists() || !file.isFile()) {
			fileInfo.setContentFile(null);
			return fileInfo;
		}
		
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
			
			Iterable<Document> cursor = null;
			Iterator<Document> iter = null;
			int posRowData = 1;
			Document docTmp = null;
			Document docEInvoiceDetail = null;
			String docEInvoiceStatus = null;
			int countRow = 1;
			
			cursor = mongoTemplate.getCollection("EInvoice").aggregate(pipeline).allowDiskUse(true);
			iter = cursor.iterator();
			while(iter.hasNext()) {
				docTmp = iter.next();
				docEInvoiceDetail = docTmp.get("EInvoiceDetail", Document.class);
				//Get information of EInvoiceStatus
				String status = "";
				docEInvoiceStatus = docTmp.get("EInvoiceStatus", "");
				if("ERROR_CQT".equals(docEInvoiceStatus)) {
					status = "Lỗi CQT";
				}else if("REPLACED".equals(docEInvoiceStatus)) {
					status = "Đã thay thế";
				}else if("ADJUSTED".equals(docEInvoiceStatus)) {
					status = "Đã điều chỉnh";
				}else if("DELETED".equals(docEInvoiceStatus)) {
					status = "Đã xóa bỏ";
				}
				else {
					status = "Đã phát hành";
				}
				//Get information of EInvoiceStatus
				row = sheet.getRow(posRowData);
				if(null == row) row = sheet.createRow(posRowData);
				
				cell = row.getCell(0);if(cell == null) cell = row.createCell(0);cell.setCellStyle(styleInfoR);
				cell.setCellValue("");
				
				
				cell = row.getCell(1);if(cell == null) cell = row.createCell(1);cell.setCellStyle(styleInfoC);
				cell.setCellValue("");

				cell = row.getCell(2);if(cell == null) cell = row.createCell(2);cell.setCellStyle(styleInfoC);
				cell.setCellValue("");
				
				cell = row.getCell(3);if(cell == null) cell = row.createCell(3);cell.setCellStyle(styleInfoC);
				cell.setCellValue("");
				
				cell = row.getCell(4);if(cell == null) cell = row.createCell(4);cell.setCellStyle(styleInfoC);
				cell.setCellValue("");
				
				cell = row.getCell(5);if(cell == null) cell = row.createCell(5);cell.setCellStyle(styleInfoC);
				cell.setCellValue("");
			
				cell = row.getCell(6);if(cell == null) cell = row.createCell(6);cell.setCellStyle(styleInfoC);
				cell.setCellValue("");
				
				cell = row.getCell(7);if(cell == null) cell = row.createCell(7);cell.setCellStyle(styleInfoC);
				cell.setCellValue("");
				
				cell = row.getCell(8);if(cell == null) cell = row.createCell(8);cell.setCellStyle(styleInfoC);
				cell.setCellValue("");
				
				cell = row.getCell(9);if(cell == null) cell = row.createCell(9);cell.setCellStyle(styleInfoC);
				cell.setCellValue("");
				
				cell = row.getCell(10);if(cell == null) cell = row.createCell(10);cell.setCellStyle(styleInfoC);
				cell.setCellValue("");
				
//				
//				cell = row.getCell(11);if(cell == null) cell = row.createCell(11);cell.setCellStyle(styleInfoC);
//				cell.setCellValue(
//						docEInvoiceDetail.getEmbedded(Arrays.asList("TTChung", "KHMSHDon"), String.class)
//						+ docEInvoiceDetail.getEmbedded(Arrays.asList("TTChung", "KHHDon"), String.class)
//					);
//
//				cell = row.getCell(12);if(cell == null) cell = row.createCell(12);cell.setCellStyle(styleInfoC);
//				cell.setCellValue(
//						 docEInvoiceDetail.getEmbedded(Arrays.asList("TTChung", "KHHDon"), String.class)
//					);
//				
//				cell = row.getCell(13);if(cell == null) cell = row.createCell(13);cell.setCellStyle(styleInfoC);
//				cell.setCellValue(
//						docEInvoiceDetail.getEmbedded(Arrays.asList("TTChung", "SHDon"), Object.class) == null? "": 
//						commons.formatNumberBillInvoice(docEInvoiceDetail.getEmbedded(Arrays.asList("TTChung", "SHDon"), Integer.class))
//					);
//				
//				
//				cell = row.getCell(14);if(cell == null) cell = row.createCell(14);cell.setCellStyle(styleInfoC);
//				if(null != docEInvoiceDetail.getEmbedded(Arrays.asList("TTChung", "NLap"), Object.class) && docEInvoiceDetail.getEmbedded(Arrays.asList("TTChung", "NLap"), Object.class) instanceof Date) {
//					cell.setCellValue(commons.convertLocalDateTimeToString(commons.convertDateToLocalDateTime(docEInvoiceDetail.getEmbedded(Arrays.asList("TTChung", "NLap"), Date.class)), Constants.FORMAT_DATE.FORMAT_DATE_WEB));
//				}
				
				cell = row.getCell(15);if(cell == null) cell = row.createCell(15);cell.setCellStyle(styleInfoC);
				cell.setCellValue("");
			
				cell = row.getCell(16);if(cell == null) cell = row.createCell(16);cell.setCellStyle(styleInfoC);
				cell.setCellValue("");
				
				cell = row.getCell(17);if(cell == null) cell = row.createCell(17);cell.setCellStyle(styleInfoC);
				cell.setCellValue("");
				
				cell = row.getCell(18);if(cell == null) cell = row.createCell(18);cell.setCellStyle(styleInfoC);
				cell.setCellValue("");
				
				cell = row.getCell(19);if(cell == null) cell = row.createCell(19);cell.setCellStyle(styleInfoC);
				cell.setCellValue("");
				
				cell = row.getCell(20);if(cell == null) cell = row.createCell(20);cell.setCellStyle(styleInfoC);
				cell.setCellValue("");
				
				cell = row.getCell(21);if(cell == null) cell = row.createCell(21);cell.setCellStyle(styleInfoC);
				cell.setCellValue("");

				cell = row.getCell(22);if(cell == null) cell = row.createCell(22);cell.setCellStyle(styleInfoC);
				cell.setCellValue("");
				
				cell = row.getCell(23);if(cell == null) cell = row.createCell(23);cell.setCellStyle(styleInfoC);
				cell.setCellValue("");
				
				cell = row.getCell(24);if(cell == null) cell = row.createCell(24);cell.setCellStyle(styleInfoC);
				cell.setCellValue("");
				
				cell = row.getCell(25);if(cell == null) cell = row.createCell(25);cell.setCellStyle(styleInfoC);
				cell.setCellValue("");
			
				cell = row.getCell(26);if(cell == null) cell = row.createCell(26);cell.setCellStyle(styleInfoC);
				cell.setCellValue("");
				
				cell = row.getCell(27);if(cell == null) cell = row.createCell(27);cell.setCellStyle(styleInfoC);
				cell.setCellValue("");
				
				cell = row.getCell(28);if(cell == null) cell = row.createCell(28);cell.setCellStyle(styleInfoC);
				cell.setCellValue("");
				
				cell = row.getCell(29);if(cell == null) cell = row.createCell(29);cell.setCellStyle(styleInfoC);
				cell.setCellValue("");
				
				cell = row.getCell(30);if(cell == null) cell = row.createCell(30);cell.setCellStyle(styleInfoC);
				cell.setCellValue("");
				
				cell = row.getCell(31);if(cell == null) cell = row.createCell(31);cell.setCellStyle(styleInfoC);
				cell.setCellValue("");

				cell = row.getCell(32);if(cell == null) cell = row.createCell(32);cell.setCellStyle(styleInfoC);
				cell.setCellValue("");
				
				cell = row.getCell(33);if(cell == null) cell = row.createCell(33);cell.setCellStyle(styleInfoC);
				cell.setCellValue("");
				
				cell = row.getCell(34);if(cell == null) cell = row.createCell(34);cell.setCellStyle(styleInfoC);
				cell.setCellValue("");
				
				cell = row.getCell(35);if(cell == null) cell = row.createCell(35);cell.setCellStyle(styleInfoC);
				cell.setCellValue("");
			
				cell = row.getCell(36);if(cell == null) cell = row.createCell(36);cell.setCellStyle(styleInfoC);
				cell.setCellValue("");
				
				cell = row.getCell(37);if(cell == null) cell = row.createCell(37);cell.setCellStyle(styleInfoC);
				cell.setCellValue("");
				
				cell = row.getCell(38);if(cell == null) cell = row.createCell(38);cell.setCellStyle(styleInfoC);
				cell.setCellValue("");
				
				cell = row.getCell(39);if(cell == null) cell = row.createCell(39);cell.setCellStyle(styleInfoC);
				cell.setCellValue("");
				
				cell = row.getCell(40);if(cell == null) cell = row.createCell(40);cell.setCellStyle(styleInfoC);
				cell.setCellValue("");
				
				cell = row.getCell(41);if(cell == null) cell = row.createCell(41);cell.setCellStyle(styleInfoC);
				cell.setCellValue("");

				cell = row.getCell(42);if(cell == null) cell = row.createCell(42);cell.setCellStyle(styleInfoC);
				cell.setCellValue("");
				
				cell = row.getCell(43);if(cell == null) cell = row.createCell(43);cell.setCellStyle(styleInfoC);
				cell.setCellValue("");
				
				cell = row.getCell(44);if(cell == null) cell = row.createCell(44);cell.setCellStyle(cellStyleNum);
				cell.setCellValue(
					docEInvoiceDetail.getEmbedded(Arrays.asList("TToan", "TgTThue"), Object.class) == null? 0:
					docEInvoiceDetail.getEmbedded(Arrays.asList("TToan", "TgTThue"), Double.class)
				);
				
				cell = row.getCell(45);if(cell == null) cell = row.createCell(45);cell.setCellStyle(styleInfoC);
				cell.setCellValue("");
			
				cell = row.getCell(46);if(cell == null) cell = row.createCell(46);cell.setCellStyle(styleInfoC);
				cell.setCellValue("");
				
				cell = row.getCell(47);if(cell == null) cell = row.createCell(47);cell.setCellStyle(styleInfoC);
				cell.setCellValue("");
				
				cell = row.getCell(47);if(cell == null) cell = row.createCell(47);cell.setCellStyle(styleInfoC);
				cell.setCellValue("");
				
				cell = row.getCell(48);if(cell == null) cell = row.createCell(48);cell.setCellStyle(styleInfoC);
				cell.setCellValue("");
				
				cell = row.getCell(49);if(cell == null) cell = row.createCell(49);cell.setCellStyle(styleInfoC);
				cell.setCellValue("");
				
				cell = row.getCell(50);if(cell == null) cell = row.createCell(50);cell.setCellStyle(styleInfoC);
				cell.setCellValue("");
				
				cell = row.getCell(51);if(cell == null) cell = row.createCell(51);cell.setCellStyle(styleInfoC);
				cell.setCellValue("");
				
				cell = row.getCell(52);if(cell == null) cell = row.createCell(52);cell.setCellStyle(styleInfoC);
				cell.setCellValue("");
				
				
				if(docEInvoiceDetail.get("DSHHDVu") != null && docEInvoiceDetail.getList("DSHHDVu", Document.class).size() > 0) {
					for(Document o: docEInvoiceDetail.getList("DSHHDVu", Document.class)) {
						row = sheet.getRow(posRowData);
						if(null == row) row = sheet.createRow(posRowData);
					
						
						cell = row.getCell(11);if(cell == null) cell = row.createCell(11);cell.setCellStyle(styleInfoC);
						cell.setCellValue(
								docEInvoiceDetail.getEmbedded(Arrays.asList("TTChung", "KHMSHDon"), String.class)
//								+ docEInvoiceDetail.getEmbedded(Arrays.asList("TTChung", "KHHDon"), String.class)
							);

						cell = row.getCell(12);if(cell == null) cell = row.createCell(12);cell.setCellStyle(styleInfoC);
						cell.setCellValue(
								 docEInvoiceDetail.getEmbedded(Arrays.asList("TTChung", "KHHDon"), String.class)
							);
						
						cell = row.getCell(13);if(cell == null) cell = row.createCell(13);cell.setCellStyle(styleInfoC);
						cell.setCellValue(
								docEInvoiceDetail.getEmbedded(Arrays.asList("TTChung", "SHDon"), Object.class) == null? "": 
								commons.formatNumberBillInvoice(docEInvoiceDetail.getEmbedded(Arrays.asList("TTChung", "SHDon"), Integer.class))
							);
						
						
						cell = row.getCell(14);if(cell == null) cell = row.createCell(14);cell.setCellStyle(styleInfoC);
						if(null != docEInvoiceDetail.getEmbedded(Arrays.asList("TTChung", "NLap"), Object.class) && docEInvoiceDetail.getEmbedded(Arrays.asList("TTChung", "NLap"), Object.class) instanceof Date) {
							cell.setCellValue(commons.convertLocalDateTimeToString(commons.convertDateToLocalDateTime(docEInvoiceDetail.getEmbedded(Arrays.asList("TTChung", "NLap"), Date.class)), Constants.FORMAT_DATE.FORMAT_DATE_WEB));
						}
						
						cell = row.getCell(24);if(cell == null) cell = row.createCell(24);cell.setCellStyle(styleInfoL);
						cell.setCellValue(o.get("ProductCode", ""));
					
						cell = row.getCell(25);if(cell == null) cell = row.createCell(25);cell.setCellStyle(styleInfoL);
						cell.setCellValue(o.get("ProductName", ""));
						
						cell = row.getCell(29);if(cell == null) cell = row.createCell(29);cell.setCellStyle(styleInfoL);
						cell.setCellValue(o.get("Unit", ""));
						
						cell = row.getCell(30);if(cell == null) cell = row.createCell(30);cell.setCellStyle(cellStyleNum);
						cell.setCellValue(o.get("Quantity", Object.class) == null? 0:o.get("Quantity", Double.class));
						
						cell = row.getCell(32);if(cell == null) cell = row.createCell(32);cell.setCellStyle(cellStyleNum);
						cell.setCellValue(o.get("Price", Object.class) == null? 0:o.get("Price", Double.class));
						
						
						String feature = o.get("Feature", "");
						
						if(feature.equals("3")) {
							double total = o.get("Total", Object.class) == null? 0:o.get("Total", Double.class) ;
							
							String check_total = commons.formatNumberReal(total);
							String total_ =  "(" + check_total + ")";
							cell = row.getCell(33);if(cell == null) cell = row.createCell(33);cell.setCellStyle(cellStyleCheck);
							cell.setCellValue(total_);
						}else {
							cell = row.getCell(33);if(cell == null) cell = row.createCell(33);cell.setCellStyle(cellStyleNum);
							cell.setCellValue(o.get("Total", Object.class) == null? 0:o.get("Total", Double.class));
						}
						
						
						cell = row.getCell(43);if(cell == null) cell = row.createCell(43);cell.setCellStyle(cellStyleNum);
						
						
						Double VATRate = o.get("VATRate", 0D);
						String TSuat = "";
						if(VATRate == -1) {
							TSuat = "KCT";
						}
						else if(VATRate == -2) {
							TSuat = "KKKNT";
						}else {							
							TSuat = String.valueOf(VATRate.intValue());
						}
						
						cell.setCellValue(TSuat);
						
						
						
						
					posRowData++;	
				}
			}else {
				posRowData++;	
			}
				
				countRow++;
			}
			
			
			out = new ByteArrayOutputStream();
			wb.write(out);
			
			fileInfo = new FileInfo();
			fileInfo.setFileName("Thong-ke-VAT-MISA_SME_2015.xlsx");
			fileInfo.setContentFile(out.toByteArray());			
			return fileInfo;
		}catch(Exception e) {
			throw e;
		}finally {
			try{wb.close();}catch(Exception ex){}
		}
	}


}
