package vn.sesgroup.hddt.user.impl;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.DataFormat;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.streaming.SXSSFCell;
import org.apache.poi.xssf.streaming.SXSSFRow;
import org.apache.poi.xssf.streaming.SXSSFSheet;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.bson.Document;
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
import vn.sesgroup.hddt.user.dao.SendMailHDDAO;
import vn.sesgroup.hddt.user.service.TCTNService;
import vn.sesgroup.hddt.utility.Constants;
import vn.sesgroup.hddt.utility.Json;
import vn.sesgroup.hddt.utility.SystemParams;

@Repository
@Transactional
public class SendMailHDImpl extends AbstractDAO implements SendMailHDDAO {

	@Autowired MongoTemplate mongoTemplate;
	@Autowired TCTNService tctnService;

	String SUMslhd = "";

	
	private List<Document> buildListPipeline(Object objData, MsgHeader header) throws Exception{
		List<Document> pipeline = null;
		String fromDate = "";
		String toDate = "";
		
		JsonNode jsonData = null;
		if(objData != null) {
			jsonData = Json.serializer().nodeFromObject(objData);		
			fromDate = commons.getTextJsonNode(jsonData.at("/FromDate")).replaceAll("\\s", "");
			toDate = commons.getTextJsonNode(jsonData.at("/ToDate")).replaceAll("\\s", "");
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
		
		Document docMatch = new Document("IsDelete", new Document("$ne", true))				
				.append("SignStatusCode", Constants.INVOICE_SIGN_STATUS.SIGNED)
				.append("EInvoiceStatus",
						new Document("$in", Arrays.asList(Constants.INVOICE_STATUS.COMPLETE,
								Constants.INVOICE_STATUS.ADJUSTED, Constants.INVOICE_STATUS.REPLACED)))
				
				.append("$expr", new Document("$and", Arrays.asList(		
						new Document("$ne", Arrays.asList("$EInvoiceDetail.NDHDon.NMua.MST", "")),
						new Document("$ne", Arrays.asList("$EInvoiceDetail.NDHDon.NMua.MST", null))					
						)));
				;
								
				if(docMatchDate !=null)
				docMatch.append("EInvoiceDetail.TTChung.NLap", docMatchDate);
		
		pipeline = new ArrayList<Document>();
		pipeline.add(new Document("$match", docMatch));
	
		pipeline.add(
			new Document("$sort", 
					new Document("EInvoiceDetail.TTChung.NLap", -1)
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
		Iterable<Document> cursor = null;
		Iterator<Document> iter = null;

		MsgRsp rsp = new MsgRsp(header);
		MspResponseStatus responseStatus = null;

		Document docTmp = null;
		List<Document> pipeline = new ArrayList<Document>();

		
		pipeline = buildListPipeline(objData, header);
		pipeline.addAll(createFacetForSearchNotSort(page));
		
		cursor = mongoTemplate.getCollection("EInvoice").aggregate(pipeline).allowDiskUse(true);
		iter = cursor.iterator();
		if (iter.hasNext()) {
			docTmp = iter.next();
		}

		rsp = new MsgRsp(header);
		responseStatus = null;
		if (docTmp == null) {
			responseStatus = new MspResponseStatus(9999, Constants.MAP_ERROR.get(9999));
			rsp.setResponseStatus(responseStatus);
			return rsp;
		}

		page.setTotalRows(docTmp.getInteger("total", 0));
		rsp.setMsgPage(page);

		List<Document> rows = null;
		if (docTmp != null && docTmp.get("data") instanceof List) {
			rows = docTmp.getList("data", Document.class);
		}

		ArrayList<HashMap<String, Object>> rowsReturn = new ArrayList<HashMap<String, Object>>();
		HashMap<String, Object> hItem = null;
		if (null != rows) {
			for (Document doc : rows) {				
						hItem = new HashMap<String, Object>();
						hItem.put("EInvoiceStatus", doc.get("EInvoiceStatus"));
						hItem.put("SignStatusCode", doc.get("SignStatusCode"));
						hItem.put("IssuerId", doc.get("IssuerId"));
						hItem.put("EInvoiceDetail", doc.get("EInvoiceDetail"));
						rowsReturn.add(hItem);			
				}
		}

		if (rowsReturn.size() == 0) {
			responseStatus = new MspResponseStatus(9999, Constants.MAP_ERROR.get(9999));
			rsp.setResponseStatus(responseStatus);
			return rsp;
		}

		responseStatus = new MspResponseStatus(0, "SUCCESS");
		rsp.setResponseStatus(responseStatus);
		HashMap<String, Object> mapDataR = new HashMap<String, Object>();
		mapDataR.put("rows", rowsReturn);
		rsp.setObjData(mapDataR);
		return rsp;
	}

	
	public FileInfo exportExcel(JSONRoot jsonRoot) throws Exception {
		FileInfo fileInfo = new FileInfo();
		Msg msg = jsonRoot.getMsg();
		MsgHeader header = msg.getMsgHeader();
		Object objData = msg.getObjData();

		Iterable<Document> cursor = null;
		Iterator<Document> iter = null;
		
		List<Document> pipeline = new ArrayList<Document>();
		pipeline = buildListPipeline(objData, header);
		
		File file = new File(SystemParams.DIR_E_INVOICE_TEMPLATE, Constants.TEMPLATE_FILE_NAME.EXCEL_SENDMAILHD_CTIET);
		if(file == null || !file.exists() || !file.isFile()) {
			fileInfo.setContentFile(null);
			return fileInfo;
		}

		 String excelFilePath = SystemParams.DIR_E_INVOICE_TEMPLATE+"/"+Constants.TEMPLATE_FILE_NAME.EXCEL_SENDMAILHD_CTIET;
		 FileInputStream inputStream = new FileInputStream(new File(excelFilePath));
	     Workbook tmpWb = getWorkbook(inputStream, excelFilePath);

	     SXSSFWorkbook wb = new SXSSFWorkbook((XSSFWorkbook) tmpWb);
	        
		ByteArrayOutputStream out = null;
//		SXSSFWorkbook wb = new SXSSFWorkbook();
		SXSSFSheet sheet = null;
		SXSSFRow row = null;
		SXSSFCell cell = null;
		
		try {
//			wb = (SXSSFWorkbook) WorkbookFactory.create(new FileInputStream(file));
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
			cellStyleNum.setFont(fontDetail);
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
			Document docTmp = null;
			Document docEInvoiceDetail = null;
		
			int countRow = 1;
					
			cursor = mongoTemplate.getCollection("EInvoice").aggregate(pipeline).allowDiskUse(true);
			iter = cursor.iterator();	
		
			while(iter.hasNext()) {
				docTmp = iter.next();
				docEInvoiceDetail = docTmp.get("EInvoiceDetail", Document.class);
				row = sheet.getRow(posRowData);
				if(null == row) row = sheet.createRow(posRowData);
				
				cell = row.getCell(0);if(cell == null) cell = row.createCell(0);cell.setCellStyle(styleInfoR);
				cell.setCellValue(countRow);
				
				cell = row.getCell(1);if(cell == null) cell = row.createCell(1);cell.setCellStyle(styleInfoC);
				cell.setCellValue(
					docEInvoiceDetail.getEmbedded(Arrays.asList("NDHDon", "NBan", "Ten"), String.class)
				);

				cell = row.getCell(2);if(cell == null) cell = row.createCell(2);cell.setCellStyle(styleInfoC);
				cell.setCellValue(
						docEInvoiceDetail.getEmbedded(Arrays.asList("NDHDon", "NBan", "MST"), String.class)
					);
				
				cell = row.getCell(3);if(cell == null) cell = row.createCell(3);cell.setCellStyle(styleInfoL);
				cell.setCellValue(docEInvoiceDetail.getEmbedded(Arrays.asList("NDHDon", "NBan", "DChi"), ""));
				
				cell = row.getCell(4);if(cell == null) cell = row.createCell(4);cell.setCellStyle(styleInfoL);
				cell.setCellValue(docEInvoiceDetail.getEmbedded(Arrays.asList("NDHDon", "NBan", "DCTDTu"), ""));
				
				cell = row.getCell(5);if(cell == null) cell = row.createCell(5);cell.setCellStyle(styleInfoL);
				cell.setCellValue(docEInvoiceDetail.getEmbedded(Arrays.asList("NDHDon", "NBan", "SDThoai"), ""));
				
				cell = row.getCell(6);if(cell == null) cell = row.createCell(6);cell.setCellStyle(styleInfoL);
				cell.setCellValue(docEInvoiceDetail.getEmbedded(Arrays.asList("NDHDon", "NMua", "Ten"), ""));
				
				cell = row.getCell(7);if(cell == null) cell = row.createCell(7);cell.setCellStyle(styleInfoL);
				cell.setCellValue(docEInvoiceDetail.getEmbedded(Arrays.asList("NDHDon", "NMua", "MST"), ""));
				
				cell = row.getCell(8);if(cell == null) cell = row.createCell(8);cell.setCellStyle(styleInfoL);
				cell.setCellValue(docEInvoiceDetail.getEmbedded(Arrays.asList("NDHDon", "NMua", "DChi"), ""));
				
				cell = row.getCell(9);if(cell == null) cell = row.createCell(9);cell.setCellStyle(styleInfoL);
				cell.setCellValue(docEInvoiceDetail.getEmbedded(Arrays.asList("NDHDon", "NMua", "DCTDTu"), ""));
				
				cell = row.getCell(10);if(cell == null) cell = row.createCell(10);cell.setCellStyle(styleInfoL);
				cell.setCellValue(docEInvoiceDetail.getEmbedded(Arrays.asList("NDHDon", "NMua", "SDThoai"), ""));
				
				cell = row.getCell(11);if(cell == null) cell = row.createCell(11);cell.setCellStyle(styleInfoC);
				
				Object shdon = docEInvoiceDetail.getEmbedded(Arrays.asList("TTChung", "SHDon"), Object.class);
				
				Class<?> objClass = shdon.getClass();
				
				if(objClass == Double.class) {
					cell.setCellValue(
							docEInvoiceDetail.getEmbedded(Arrays.asList("TTChung", "SHDon"), Object.class) == null? "": 
							commons.formatNumberReal(docEInvoiceDetail.getEmbedded(Arrays.asList("TTChung", "SHDon"), Double.class))
						);
				}else {
					cell.setCellValue(
							docEInvoiceDetail.getEmbedded(Arrays.asList("TTChung", "SHDon"), Object.class) == null? "": 
							commons.formatNumberReal(docEInvoiceDetail.getEmbedded(Arrays.asList("TTChung", "SHDon"), Integer.class))
						);
				}
				

				//ngay phat hanh
				cell = row.getCell(12);if(cell == null) cell = row.createCell(12);cell.setCellStyle(styleInfoC);
				if(null != docEInvoiceDetail.getEmbedded(Arrays.asList("TTChung", "NLap"), Object.class) && docEInvoiceDetail.getEmbedded(Arrays.asList("TTChung", "NLap"), Object.class) instanceof Date) {
					cell.setCellValue(commons.convertLocalDateTimeToString(commons.convertDateToLocalDateTime(docEInvoiceDetail.getEmbedded(Arrays.asList("TTChung", "NLap"), Date.class)), Constants.FORMAT_DATE.FORMAT_DATE_WEB));
				}
				
				//tong tien
				cell = row.getCell(13);if(cell == null) cell = row.createCell(13);cell.setCellStyle(cellStyleNum);
				cell.setCellValue(
					docEInvoiceDetail.getEmbedded(Arrays.asList("TToan", "TgTTTBSo"), Object.class) == null? 0:
					docEInvoiceDetail.getEmbedded(Arrays.asList("TToan", "TgTTTBSo"), Double.class)
				);
				
				//ngay gui hoa don
				cell = row.getCell(14);if(cell == null) cell = row.createCell(14);cell.setCellStyle(styleInfoC);
				if(null != docTmp.get("CQT_Date", Object.class) && docTmp.get("CQT_Date", Object.class) instanceof Date) {
					cell.setCellValue(commons.convertLocalDateTimeToString(commons.convertDateToLocalDateTime(docTmp.get("CQT_Date", Date.class)), Constants.FORMAT_DATE.FORMAT_DATE_WEB));
				}
				
				countRow++;
				posRowData++;
			}
			
			
			out = new ByteArrayOutputStream();
			wb.write(out);
			
			fileInfo = new FileInfo();
			fileInfo.setFileName("chi-tiet-danh-sach-gui-mai-hoa-don.xlsx");
			fileInfo.setContentFile(out.toByteArray());			
			return fileInfo;
		}catch(Exception e) {
			throw e;
		}finally {
			try{wb.close();}catch(Exception ex){}
		}
	}

	
    private Workbook getWorkbook(FileInputStream inputStream, String excelFilePath)
            throws IOException {
        Workbook workbook = null;

        if (excelFilePath.endsWith("xlsx")) {
            workbook = new XSSFWorkbook(inputStream);
        } else if (excelFilePath.endsWith("xls")) {
            workbook = new HSSFWorkbook(inputStream);
        } else {
            throw new IllegalArgumentException("File không đúng định dạng");
        }

        return workbook;
    }
}
