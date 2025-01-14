package vn.sesgroup.hddt.user.impl;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.time.LocalDate;
import java.time.temporal.ChronoField;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
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
import com.fasterxml.jackson.databind.JsonNode;

import net.sf.jasperreports.engine.JRDataSource;
import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;
import net.sf.jasperreports.engine.export.HtmlExporter;
import net.sf.jasperreports.engine.export.JRPdfExporter;
import net.sf.jasperreports.export.Exporter;
import net.sf.jasperreports.export.SimpleExporterInput;
import net.sf.jasperreports.export.SimpleHtmlExporterOutput;
import net.sf.jasperreports.export.SimpleOutputStreamExporterOutput;
import net.sf.jasperreports.export.SimplePdfExporterConfiguration;
import vn.sesgroup.hddt.dto.FileInfo;
import vn.sesgroup.hddt.user.dao.AbstractDAO;
import vn.sesgroup.hddt.user.dao.TKTNCNDAO;
import vn.sesgroup.hddt.user.service.JPUtils;
import vn.sesgroup.hddt.user.service.TCTNService;
import vn.sesgroup.hddt.utility.Constants;
import vn.sesgroup.hddt.utility.Json;
import vn.sesgroup.hddt.utility.MailUtils;
import vn.sesgroup.hddt.utility.SystemParams;

@Repository
@Transactional
public class TKTNCNImpl extends AbstractDAO implements TKTNCNDAO {
	private static final Logger log = LogManager.getLogger(EInvoiceImpl.class);
	@Autowired
	MongoTemplate mongoTemplate;
	@Autowired
	TCTNService tctnService;
	private MailUtils mailUtils = new MailUtils();
	@Autowired
	JPUtils jpUtils;
	
	
	
	@SuppressWarnings({ "unchecked" })
	@Override
	public FileInfo viewReport(JSONRoot jsonRoot) throws Exception {
		FileInfo fileInfo = new FileInfo();
		
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
				
			HashMap<String, String> hInput = (HashMap<String, String>) msg.getObjData();
			String type = null == hInput.get("type")? "": hInput.get("type");
			String typeExport = null == hInput.get("typeExport")? "excel": hInput.get("typeExport");
			
						
			String loginRes = header.getUserName();
			switch (type) {
			case "ReportSituationUseInvoice":
				switch (typeExport) {
				case "excel":
					fileInfo = TKTNCNExcel(loginRes, jsonRoot);
					break;
				default:
					fileInfo = TKTNCNPdf(loginRes, jsonRoot);
					break;
				}				
				break;
			default:
				return fileInfo;			
			}
			return fileInfo;
		}
	

	


	private FileInfo TKTNCNExcel(String loginRes, JSONRoot jsonRoot) throws Exception{
		FileInfo fileInfo = new FileInfo();
		Iterable<Document> cursor = null;
		Iterator<Document> iter = null;
		List<Document> pipeline = new ArrayList<Document>();
			Document docTmp = null;
		try {
			Msg msg = jsonRoot.getMsg();
			MsgHeader header = msg.getMsgHeader();
			MsgPage page = msg.getMsgPage();
			Object objData = msg.getObjData();
			@SuppressWarnings("unchecked")
			HashMap<String, String> hInput = (HashMap<String, String>) msg.getObjData();									
					try {
						
						
						String quarterMonth = null == hInput.get("quarterMonth")? "": hInput.get("quarterMonth");
						String year = null == hInput.get("year")? "": hInput.get("year");
						String typeExport = null == hInput.get("typeExport")? "html": hInput.get("typeExport");
						
						String pQuarterMonth = "";
						LocalDate reportDateFrom = LocalDate.now();
						LocalDate reportDateTo = LocalDate.now();
						switch (quarterMonth) {
						case "Q1": 
							pQuarterMonth = "Quý 1";
							reportDateFrom = LocalDate.of(commons.stringToInteger(year), 1, 1);
							reportDateTo = LocalDate.of(commons.stringToInteger(year), 3, 1);
							reportDateTo = reportDateTo.with(TemporalAdjusters.lastDayOfMonth());
							break;
						case "Q2": 
							pQuarterMonth = "Quý 2";
							reportDateFrom = LocalDate.of(commons.stringToInteger(year), 4, 1);
							reportDateTo = LocalDate.of(commons.stringToInteger(year), 6, 1);
							reportDateTo = reportDateTo.with(TemporalAdjusters.lastDayOfMonth());
							break;
						case "Q3": 
							pQuarterMonth = "Quý 3";
							reportDateFrom = LocalDate.of(commons.stringToInteger(year), 7, 1);
							reportDateTo = LocalDate.of(commons.stringToInteger(year), 9, 1);
							reportDateTo = reportDateTo.with(TemporalAdjusters.lastDayOfMonth());
							break;
						case "Q4":
							pQuarterMonth = "Quý 4";
							reportDateFrom = LocalDate.of(commons.stringToInteger(year), 10, 1);
							reportDateTo = LocalDate.of(commons.stringToInteger(year), 12, 1);
							reportDateTo = reportDateTo.with(TemporalAdjusters.lastDayOfMonth());
							break;
						case "M1": 
							pQuarterMonth = "Tháng 1";
							reportDateFrom = LocalDate.of(commons.stringToInteger(year), 1, 1);
							reportDateTo = LocalDate.of(commons.stringToInteger(year), 1, 1);
							reportDateTo = reportDateTo.with(TemporalAdjusters.lastDayOfMonth());
							break;
						case "M2": 
							pQuarterMonth = "Tháng 2";
							reportDateFrom = LocalDate.of(commons.stringToInteger(year), 2, 1);
							reportDateTo = LocalDate.of(commons.stringToInteger(year), 2, 1);
							reportDateTo = reportDateTo.with(TemporalAdjusters.lastDayOfMonth());
							break;
						case "M3": 
							pQuarterMonth = "Tháng 3";
							reportDateFrom = LocalDate.of(commons.stringToInteger(year), 3, 1);
							reportDateTo = LocalDate.of(commons.stringToInteger(year), 3, 1);
							reportDateTo = reportDateTo.with(TemporalAdjusters.lastDayOfMonth());
							break;
						case "M4": 
							pQuarterMonth = "Tháng 4";
							reportDateFrom = LocalDate.of(commons.stringToInteger(year), 4, 1);
							reportDateTo = LocalDate.of(commons.stringToInteger(year), 4, 1);
							reportDateTo = reportDateTo.with(TemporalAdjusters.lastDayOfMonth());
							break;
						case "M5": 
							pQuarterMonth = "Tháng 5";
							reportDateFrom = LocalDate.of(commons.stringToInteger(year), 5, 1);
							reportDateTo = LocalDate.of(commons.stringToInteger(year), 5, 1);
							reportDateTo = reportDateTo.with(TemporalAdjusters.lastDayOfMonth());
							break;
						case "M6": 
							pQuarterMonth = "Tháng 6";
							reportDateFrom = LocalDate.of(commons.stringToInteger(year), 6, 1);
							reportDateTo = LocalDate.of(commons.stringToInteger(year), 6, 1);
							reportDateTo = reportDateTo.with(TemporalAdjusters.lastDayOfMonth());
							break;
						case "M7": 
							pQuarterMonth = "Tháng 7";
							reportDateFrom = LocalDate.of(commons.stringToInteger(year), 7, 1);
							reportDateTo = LocalDate.of(commons.stringToInteger(year), 7, 1);
							reportDateTo = reportDateTo.with(TemporalAdjusters.lastDayOfMonth());
							break;
						case "M8": 
							pQuarterMonth = "Tháng 8";
							reportDateFrom = LocalDate.of(commons.stringToInteger(year), 8, 1);
							reportDateTo = LocalDate.of(commons.stringToInteger(year), 8, 1);
							reportDateTo = reportDateTo.with(TemporalAdjusters.lastDayOfMonth());
							break;
						case "M9": 
							pQuarterMonth = "Tháng 9";
							reportDateFrom = LocalDate.of(commons.stringToInteger(year), 9, 1);
							reportDateTo = LocalDate.of(commons.stringToInteger(year), 9, 1);
							reportDateTo = reportDateTo.with(TemporalAdjusters.lastDayOfMonth());
							break;
						case "M10": 
							pQuarterMonth = "Tháng 10";
							reportDateFrom = LocalDate.of(commons.stringToInteger(year), 10, 1);
							reportDateTo = LocalDate.of(commons.stringToInteger(year), 10, 1);
							reportDateTo = reportDateTo.with(TemporalAdjusters.lastDayOfMonth());
							break;
						case "M11": 
							pQuarterMonth = "Tháng 11";
							reportDateFrom = LocalDate.of(commons.stringToInteger(year), 11, 1);
							reportDateTo = LocalDate.of(commons.stringToInteger(year), 11, 1);
							reportDateTo = reportDateTo.with(TemporalAdjusters.lastDayOfMonth());
							break;
						case "M12": 
							pQuarterMonth = "Tháng 12";
							reportDateFrom = LocalDate.of(commons.stringToInteger(year), 12, 1);
							reportDateTo = LocalDate.of(commons.stringToInteger(year), 12, 1);
							reportDateTo = reportDateTo.with(TemporalAdjusters.lastDayOfMonth());
							break;
						default:
							break;
						}
						
					//GET DATA TO DB		

						ObjectId objectId = null;
						ObjectId objectIdUser = null;			
						ObjectId objectIdEInvoice = null;
						objectId = null;
						try {
							objectId = new ObjectId(header.getIssuerId());
						} catch (Exception e) {
						}
						try {
							objectIdUser = new ObjectId(header.getUserId());
						} catch (Exception e) {
						}

						pipeline = new ArrayList<Document>();	
						pipeline.add(new Document("$match", new Document("IssuerId", header.getIssuerId()).append("IsActive", true)
								.append("IsDelete", false)
								.append("Status", new Document("$in", Arrays.asList("COMPLETE", "XOABO")))							
								.append("$and", Arrays.asList(new Document("$and", Arrays.asList(new Document("DateTime", new Document("$gte", reportDateFrom)
										.append("$lt", reportDateTo))							
												))))
								));		
						
						pipeline.add(new Document("$sort", new Document("SignStatus", 1).append("SHDon", 1).append("_id", -1)));
					
						pipeline.add(
						        new Document("$project", new Document("_id", 1).append("KyHieu", 1).append("SHDon", 1)
						            .append("DateTime", 1).append("Name", 1).append("TNCNKhauTru", 1).append("TaxCode", 1).append("Status", 1)            
						            ));
						
						File file = new File(SystemParams.DIR_E_INVOICE_TEMPLATE, Constants.TEMPLATE_FILE_NAME.EXCEL_TKCNTNT);
						if(file == null || !file.exists() || !file.isFile()) {
							fileInfo.setContentFile(null);
							return fileInfo;
						}
						ByteArrayOutputStream out = null;
						Workbook wb = null;
				    	Sheet sheet = null;
				    	Row row = null;
						Cell cell = null;
					
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
							styleInfoL.setWrapText(false);
							
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
							
							
						
							int posRowData = 3;						
							int countRow = 1;
							
							cursor = mongoTemplate.getCollection("ChungTuTNCN").aggregate(pipeline).allowDiskUse(true);
							iter = cursor.iterator();
							while(iter.hasNext()) {
								docTmp = iter.next();
			
						
								String status = "";
								//Get information of EInvoiceStatus
								row = sheet.getRow(posRowData);
								if(null == row) row = sheet.createRow(posRowData);
								
								cell = row.getCell(0);if(cell == null) cell = row.createCell(0);cell.setCellStyle(styleInfoR);
								cell.setCellValue(countRow);
								
								cell = row.getCell(1);if(cell == null) cell = row.createCell(1);cell.setCellStyle(styleInfoC);
								cell.setCellValue(
										docTmp.getEmbedded(Arrays.asList("KyHieu"), String.class)
								);
								cell = row.getCell(2);if(cell == null) cell = row.createCell(2);cell.setCellStyle(styleInfoC);
								cell.setCellValue(
										docTmp.getEmbedded(Arrays.asList("SHDon"), Integer.class)
								);
							
								
								cell = row.getCell(3);if(cell == null) cell = row.createCell(3);cell.setCellStyle(styleInfoC);
								if(null != docTmp.getEmbedded(Arrays.asList("DateTime"), Object.class) && docTmp.getEmbedded(Arrays.asList("DateTime"), Object.class) instanceof Date) {
									cell.setCellValue(commons.convertLocalDateTimeToString(commons.convertDateToLocalDateTime(docTmp.getEmbedded(Arrays.asList("DateTime"), Date.class)), Constants.FORMAT_DATE.FORMAT_DATE_WEB));
								}
								
								cell = row.getCell(4);if(cell == null) cell = row.createCell(4);cell.setCellStyle(styleInfoL);
								cell.setCellValue(docTmp.getEmbedded(Arrays.asList("Name"), ""));
								
								
								cell = row.getCell(5);if(cell == null) cell = row.createCell(5);cell.setCellStyle(styleInfoL);
								cell.setCellValue(docTmp.getEmbedded(Arrays.asList("TaxCode"), ""));
								
								
								String CNhanTNhap = docTmp.getEmbedded(Arrays.asList("TNCNKhauTru","SoTienCaNhanKhauTru"), "").replaceAll("\\₫", "");
								String CNhanTNhap0 = CNhanTNhap.replaceAll("\\,", "");
							    int number = Integer.parseInt(CNhanTNhap0);
								cell = row.getCell(6);if(cell == null) cell = row.createCell(6);cell.setCellStyle(cellStyleNum);
								cell.setCellValue(number);
							
								String TT = "";
								String TrangThai = docTmp.getEmbedded(Arrays.asList("Status"), "");
								
								if(TrangThai.equals("COMPLETE")) {
									TT = "Đã phát hành";
								}else if(TrangThai.equals("XOABO")){
									TT = "Đã xóa bỏ";
								}else {
									TT = TrangThai;
								}
								
								cell = row.getCell(7);if(cell == null) cell = row.createCell(7);cell.setCellStyle(styleInfoC);
								cell.setCellValue(TT);
							
								posRowData++;	
								countRow++;
							}
							
							
							out = new ByteArrayOutputStream();
							wb.write(out);
							
							fileInfo = new FileInfo();
							fileInfo.setFileName("Thong-Ke-Chung-Tu-Khau-Tru-Thue.xlsx");
							fileInfo.setContentFile(out.toByteArray());			
							return fileInfo;
						
					  
					}catch(Exception e) {
						e.printStackTrace();
						
						return fileInfo;
					}								
			
		}catch(Exception e) {
			throw e;
		}
	}
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	@SuppressWarnings("unchecked")
	private FileInfo TKTNCNPdf(String loginRes, JSONRoot jsonRoot) throws Exception{
		FileInfo fileInfo = new FileInfo();
		Iterable<Document> cursor = null;
		Iterator<Document> iter = null;
		List<Document> pipeline = new ArrayList<Document>();
			Document docTmp = null;
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
			
			HashMap<String, String> hInput = (HashMap<String, String>) msg.getObjData();						
				try {
						int row = 0;
						/*KIEM TRA XEM MAU REPORT CO TON TAI KHONG*/
						File f = new File(SystemParams.DIR_E_INVOICE_TEMPLATE, "ReportSituationUseInvoice.jrxml");
						if(!(f.exists() && f.isFile())) {
							return fileInfo;
						}						
						String quarterMonth = null == hInput.get("quarterMonth")? "": hInput.get("quarterMonth");
						String year = null == hInput.get("year")? "": hInput.get("year");
						String typeExport = null == hInput.get("typeExport")? "html": hInput.get("typeExport");
						
						String pQuarterMonth = "";
						LocalDate reportDateFrom = LocalDate.now();
						LocalDate reportDateTo = LocalDate.now();
						switch (quarterMonth) {
						case "Q1": 
							pQuarterMonth = "Quý 1";
							reportDateFrom = LocalDate.of(commons.stringToInteger(year), 1, 1);
							reportDateTo = LocalDate.of(commons.stringToInteger(year), 3, 1);
							reportDateTo = reportDateTo.with(TemporalAdjusters.lastDayOfMonth());
							break;
						case "Q2": 
							pQuarterMonth = "Quý 2";
							reportDateFrom = LocalDate.of(commons.stringToInteger(year), 4, 1);
							reportDateTo = LocalDate.of(commons.stringToInteger(year), 6, 1);
							reportDateTo = reportDateTo.with(TemporalAdjusters.lastDayOfMonth());
							break;
						case "Q3": 
							pQuarterMonth = "Quý 3";
							reportDateFrom = LocalDate.of(commons.stringToInteger(year), 7, 1);
							reportDateTo = LocalDate.of(commons.stringToInteger(year), 9, 1);
							reportDateTo = reportDateTo.with(TemporalAdjusters.lastDayOfMonth());
							break;
						case "Q4":
							pQuarterMonth = "Quý 4";
							reportDateFrom = LocalDate.of(commons.stringToInteger(year), 10, 1);
							reportDateTo = LocalDate.of(commons.stringToInteger(year), 12, 1);
							reportDateTo = reportDateTo.with(TemporalAdjusters.lastDayOfMonth());
							break;
						case "M1": 
							pQuarterMonth = "Tháng 1";
							reportDateFrom = LocalDate.of(commons.stringToInteger(year), 1, 1);
							reportDateTo = LocalDate.of(commons.stringToInteger(year), 1, 1);
							reportDateTo = reportDateTo.with(TemporalAdjusters.lastDayOfMonth());
							break;
						case "M2": 
							pQuarterMonth = "Tháng 2";
							reportDateFrom = LocalDate.of(commons.stringToInteger(year), 2, 1);
							reportDateTo = LocalDate.of(commons.stringToInteger(year), 2, 1);
							reportDateTo = reportDateTo.with(TemporalAdjusters.lastDayOfMonth());
							break;
						case "M3": 
							pQuarterMonth = "Tháng 3";
							reportDateFrom = LocalDate.of(commons.stringToInteger(year), 3, 1);
							reportDateTo = LocalDate.of(commons.stringToInteger(year), 3, 1);
							reportDateTo = reportDateTo.with(TemporalAdjusters.lastDayOfMonth());
							break;
						case "M4": 
							pQuarterMonth = "Tháng 4";
							reportDateFrom = LocalDate.of(commons.stringToInteger(year), 4, 1);
							reportDateTo = LocalDate.of(commons.stringToInteger(year), 4, 1);
							reportDateTo = reportDateTo.with(TemporalAdjusters.lastDayOfMonth());
							break;
						case "M5": 
							pQuarterMonth = "Tháng 5";
							reportDateFrom = LocalDate.of(commons.stringToInteger(year), 5, 1);
							reportDateTo = LocalDate.of(commons.stringToInteger(year), 5, 1);
							reportDateTo = reportDateTo.with(TemporalAdjusters.lastDayOfMonth());
							break;
						case "M6": 
							pQuarterMonth = "Tháng 6";
							reportDateFrom = LocalDate.of(commons.stringToInteger(year), 6, 1);
							reportDateTo = LocalDate.of(commons.stringToInteger(year), 6, 1);
							reportDateTo = reportDateTo.with(TemporalAdjusters.lastDayOfMonth());
							break;
						case "M7": 
							pQuarterMonth = "Tháng 7";
							reportDateFrom = LocalDate.of(commons.stringToInteger(year), 7, 1);
							reportDateTo = LocalDate.of(commons.stringToInteger(year), 7, 1);
							reportDateTo = reportDateTo.with(TemporalAdjusters.lastDayOfMonth());
							break;
						case "M8": 
							pQuarterMonth = "Tháng 8";
							reportDateFrom = LocalDate.of(commons.stringToInteger(year), 8, 1);
							reportDateTo = LocalDate.of(commons.stringToInteger(year), 8, 1);
							reportDateTo = reportDateTo.with(TemporalAdjusters.lastDayOfMonth());
							break;
						case "M9": 
							pQuarterMonth = "Tháng 9";
							reportDateFrom = LocalDate.of(commons.stringToInteger(year), 9, 1);
							reportDateTo = LocalDate.of(commons.stringToInteger(year), 9, 1);
							reportDateTo = reportDateTo.with(TemporalAdjusters.lastDayOfMonth());
							break;
						case "M10": 
							pQuarterMonth = "Tháng 10";
							reportDateFrom = LocalDate.of(commons.stringToInteger(year), 10, 1);
							reportDateTo = LocalDate.of(commons.stringToInteger(year), 10, 1);
							reportDateTo = reportDateTo.with(TemporalAdjusters.lastDayOfMonth());
							break;
						case "M11": 
							pQuarterMonth = "Tháng 11";
							reportDateFrom = LocalDate.of(commons.stringToInteger(year), 11, 1);
							reportDateTo = LocalDate.of(commons.stringToInteger(year), 11, 1);
							reportDateTo = reportDateTo.with(TemporalAdjusters.lastDayOfMonth());
							break;
						case "M12": 
							pQuarterMonth = "Tháng 12";
							reportDateFrom = LocalDate.of(commons.stringToInteger(year), 12, 1);
							reportDateTo = LocalDate.of(commons.stringToInteger(year), 12, 1);
							reportDateTo = reportDateTo.with(TemporalAdjusters.lastDayOfMonth());
							break;
						default:
							break;
						}
						ObjectId objectId = null;
						ObjectId objectIdUser = null;			
						ObjectId objectIdEInvoice = null;
						objectId = null;
						try {
							objectId = new ObjectId(header.getIssuerId());
						} catch (Exception e) {
						}
						try {
							objectIdUser = new ObjectId(header.getUserId());
						} catch (Exception e) {
						}

				
						pipeline = new ArrayList<Document>();	
						pipeline.add(new Document("$match", new Document("_id", objectId).append("IsActive", true)
								.append("IsDelete", false)));		
						
						//USER INFO 
						pipeline.add(new Document("$match", new Document("_id", objectId).append("IsActive", true)
								.append("IsDelete", false)));
						pipeline.add(new Document("$lookup", new Document("from", "Users").append("pipeline", Arrays.asList(
								new Document("$match",
										new Document("IssuerId", header.getIssuerId()).append("_id", objectIdUser)
												.append("IsActive", true).append("IsDelete", false)),
							
								new Document("$limit", 1))).append("as", "UserInfo")));
						pipeline.add(new Document("$unwind",
								new Document("path", "$UserInfo").append("preserveNullAndEmptyArrays", true)));
						//DM Depot
						pipeline.add(new Document("$lookup", new Document("from", "DMDepot").append("pipeline", Arrays.asList(
								new Document("$match",
										new Document("TaxCode", header.getUserName())
										.append("IsDelete", false))
								
							)).append("as", "DMDepot")));
						pipeline.add(new Document("$unwind",
								new Document("path", "$DMDepot").append("preserveNullAndEmptyArrays", true)));
										
						//DM Quantity
						pipeline.add(new Document("$lookup",
						new Document("from", "DMQuantity")
							.append("pipeline",
							Arrays.asList(new Document("$match", new Document("IssuerId", header.getIssuerId())
							.append("IsDelete", false)
							.append("$and", Arrays.asList(new Document("$and", Arrays.asList(new Document("NLap", new Document("$gte", reportDateFrom)
									.append("$lt", reportDateTo))							
											))))
									),																
							new Document("$project", new Document("_id", 1).append("SoLuong", 1).append("TuSo", 1).append("DenSo", 1).append("NLap", 1).append("KHMSHDon", 1).append("KHHDon", 1))
							))
							.append("as", "DMQuantity")));
							// EInvoice
						
						//DM EIvoice
						pipeline.add(new Document("$lookup",
						new Document("from", "EInvoice")
							.append("pipeline",
							Arrays.asList(new Document("$match", new Document("IssuerId", header.getIssuerId())
							.append("IsDelete", false)
							.append("SignStatusCode", Constants.INVOICE_SIGN_STATUS.SIGNED)
							.append("$and", Arrays.asList(new Document("$and", Arrays.asList(new Document("EInvoiceDetail.TTChung.NLap", new Document("$gte", reportDateFrom)
									.append("$lt", reportDateTo))								
											))))
					
									),
									new Document("$sort", new Document("EInvoiceDetail.TTChung.SHDon", 1))
//								new Document("$project", new Document("_id", 1).append("TTChung", "$EInvoiceDetail.TTChung").append("SoLuong", 1).append("NLap", 1))
							))
							.append("as", "EInvoice")));
					
						//DMMauSoKyHieu
						pipeline.add(new Document("$lookup",
								new Document("from", "DMMauSoKyHieu")
									.append("pipeline",
									Arrays.asList(new Document("$match", new Document("IssuerId", header.getIssuerId())
											.append("IsDelete", false)
											.append("IsActive", true))									
									))
									.append("as", "DMMauSoKyHieu")));
						
						cursor = mongoTemplate.getCollection("Issuer").aggregate(pipeline);
						iter = cursor.iterator();
						if (iter.hasNext()) {
							docTmp = iter.next();
						}
						
						
						///GET INFOR SOFT BY NUMBER ON THE FILE XML
						//DATA USER INFOR
						String taxCode = docTmp.getString("TaxCode");
						String Name = docTmp.getString("Name");
						String Address = docTmp.getString("Address");
						String Phone = docTmp.getString("Phone");
						String Fax = docTmp.getString("Fax");
						String Email = docTmp.getString("Email");
						
						//DATA MAU SO KY HIEU
						 String KHMSHDMSKHieu = "";
						 List<Document> MauSoKHieu = null;
						    if (docTmp.get("DMMauSoKyHieu") != null ) {
						    	MauSoKHieu = docTmp.getList("DMMauSoKyHieu", Document.class);
						    }
						//DATA EINVOICE
						    List<Document> EInvoice = null;
						    if (docTmp.get("EInvoice") != null ) {
						    	EInvoice = docTmp.getList("EInvoice", Document.class);
						    }   
						    
						//DATA DEPOT
						int SLDeport = docTmp.getEmbedded(Arrays.asList("DMDepot", "SLHDon"), 0);
						int SLHDonDDDepot = docTmp.getEmbedded(Arrays.asList("DMDepot", "SLHDonDD"), 0); 
						int SLHDonCLDepot =	docTmp.getEmbedded(Arrays.asList("DMDepot", "SLHDonCL"), 0);
						//DATA QUANTITY
						List<Document> Quantity = null;
					    if (docTmp.get("DMQuantity") != null ) {
					    	Quantity = docTmp.getList("DMQuantity", Document.class);
					    }
						int SLQuantity = 0;	
						int SLQuantity1 = 0;	
						int SLQuantity2 = 0;	
						String KHMSHDonQuantity = "";
						String TLHDon = "";
						int tongQuantity= 0;
						int tongQuantity1= 0;
						int tongQuantity2= 0;
						
						for(int i=0; i<Quantity.size();i++) {
							
							KHMSHDonQuantity = (String) Quantity.get(i).get("KHMSHDon");
							
							if(KHMSHDonQuantity.equals("1")) {
								SLQuantity = (int) Quantity.get(i).get("SoLuong");
								tongQuantity = tongQuantity + SLQuantity;
								
							}if(KHMSHDonQuantity.equals("2")) {
								SLQuantity1 = (int) Quantity.get(i).get("SoLuong");
								tongQuantity1 = tongQuantity1 + SLQuantity1;
								
							}if(KHMSHDonQuantity.equals("6")) {
								SLQuantity2 = (int) Quantity.get(i).get("SoLuong");
								tongQuantity2 = tongQuantity2 + SLQuantity2;								
							}														
						}
						
						
						///GET INFOR SOFT BY NUMBER ON THE FILE JRXML				
						//1: NAME EINVOICE
						
						
						//2: KI HIEU MAU SO HOA DON
						//3: KI HIEU HOA DON
						//4: SO TON DAU KY: TONG SO, TU SO, DEN SO, MUA PHAT HANH TRONG KY: TU SO, DEN SO
						Map<String, Object> reportParams = new HashMap<String, Object>();		
						List<HashMap<String, Object>> arrayData = new ArrayList<>();
						LocalDate localDateNLap = LocalDate.now();
						HashMap<String, Object> hItem = null;
						reportParams.put("KTTQuy", pQuarterMonth);
						reportParams.put("KTTYear", year);
						reportParams.put("TCCNTen", Name);
						reportParams.put("TCCNMST", taxCode);
						reportParams.put("TCCNDChi", Address);
						reportParams.put("BCDauKyDate", commons.formatLocalDateTimeToString(reportDateFrom, "dd/MM/yyyy"));
						reportParams.put("BCCuoiKyDate", commons.formatLocalDateTimeToString(reportDateTo, "dd/MM/yyyy"));
						reportParams.put("InvoiceDate", StringUtils.leftPad(String.valueOf(localDateNLap.get(ChronoField.DAY_OF_MONTH)), 2, "0"));
						reportParams.put("InvoiceMonth", StringUtils.leftPad(String.valueOf(localDateNLap.get(ChronoField.MONTH_OF_YEAR)), 2, "0"));
						reportParams.put("InvoiceYear", StringUtils.leftPad(String.valueOf(localDateNLap.get(ChronoField.YEAR)), 4, "0"));

						
						   for(int i=0; i<Quantity.size();i++) {
						   
						   String KHMSHDon = "";
					    	String KHHDon = "";
					    	int SLQUANTITY = 0;
					    	int TSQUANTITY = 0;
					    	int DSQUANTITY = 0;
					    	String muaTrongKy_tuSo= "";
					    	String muaTrongKy_denSo = "";
					    	int soTonMuaTrKy_ts = 0;
					    	String soTonMuaTrKy_tongSo = "";
					    	KHMSHDon = (String) Quantity.get(i).getEmbedded(Arrays.asList("KHMSHDon"), "");	
					    	if("1".equals(KHMSHDon)) {
					    		
					    	
					    	KHHDon =  (String) Quantity.get(i).getEmbedded(Arrays.asList("KHHDon"), "");	
					    	SLQUANTITY = (int) Quantity.get(i).getEmbedded(Arrays.asList("SoLuong"), 0);	
					    	TSQUANTITY = (int) Quantity.get(i).getEmbedded(Arrays.asList("TuSo"), 0);	
					    	DSQUANTITY = (int) Quantity.get(i).getEmbedded(Arrays.asList("DenSo"), 0);	
					    	
					    	
					    	muaTrongKy_tuSo = String.valueOf(TSQUANTITY);
					    	muaTrongKy_denSo = String.valueOf(DSQUANTITY);
					    	soTonMuaTrKy_ts = (DSQUANTITY - TSQUANTITY) + 1;					    					    
					    	soTonMuaTrKy_tongSo = String.valueOf(soTonMuaTrKy_ts);
							
							
								int demEInvoice = 0;
							int demDelete = 0;
							String SHDCancel = "";
							int SHDonSD = 0;
							int SHDon = 0;
							int tamSHD = 0;
							int SHDonMax = 0;
							for(int k=0;k<EInvoice.size();k++) {
								
								String KHMSHDonEInvoice = (String) EInvoice.get(k).getEmbedded(Arrays.asList("EInvoiceDetail", "TTChung", "KHMSHDon"), "");	
						    	String KHHDonEinvoice =  (String) EInvoice.get(k).getEmbedded(Arrays.asList("EInvoiceDetail", "TTChung","KHHDon"), "");	
						    	String EIvoiceStatus = (String) EInvoice.get(k).getEmbedded(Arrays.asList("EInvoiceStatus"), "");	
						    	int SHDonTam = (int) EInvoice.get(k).getEmbedded(Arrays.asList("EInvoiceDetail", "TTChung","SHDon"), 0);
						    	if(KHMSHDonEInvoice.equals(KHMSHDon)&& KHHDonEinvoice.equals(KHHDon)) {
//						    		SHDonSD = (int) EInvoice.get(k).getEmbedded(Arrays.asList("EInvoiceDetail", "TTChung","SHDon"), 0);
						    		
						    		if(SHDonMax>SHDonTam) {
						    			SHDonMax = SHDonMax;
						    		}if(SHDonMax<SHDonTam) {
						    			SHDonMax = SHDonTam;
						    		}
						    		demEInvoice++;						    	
						    	}if(KHMSHDonEInvoice.equals(KHMSHDon)&& KHHDonEinvoice.equals(KHHDon) && "DELETED".equals(EIvoiceStatus)) {
//						    		SHDon = (int) EInvoice.get(k).getEmbedded(Arrays.asList("EInvoiceDetail", "TTChung", "SHDon"), 0);	
						    		SHDCancel = SHDCancel+ SHDonTam + ";";	
						    		demDelete++;
						    	}
							}
							String tongSoSuDung_denSo = String.valueOf(SHDonMax);
							String demEInvoices = "";
							String demSDSD = "";
							int dem = 0;
							String denDele = String.valueOf(demDelete);
							if(demDelete>0) {
								dem = demEInvoice - demDelete;
								demEInvoices = String.valueOf(dem);
							}else {
								demEInvoices = String.valueOf(demEInvoice);	
							}
							demSDSD = String.valueOf(demEInvoice);
//							int demSHDon = demEInvoice - SHDonSD;
//							String TSSHD = String.valueOf(SHDonSD);
//		                	String demSHDON = String.valueOf(demSHDon);
		                	int CongSSD = (SHDonMax - TSQUANTITY)+ 1;
		                	String tongSoSuDung_cong = String.valueOf(CongSSD);
		                	int tonCKyTS = CongSSD+ 1;
		                	int tonCuoiKy_sl = (DSQUANTITY -tonCKyTS)+ 1;
		                	String SHD_Delete = "";
		                	String TStonCKy = String.valueOf(tonCKyTS);
		                	String tonCuoiKy_soLuong = String.valueOf(tonCuoiKy_sl);
		                	if(SHDCancel!="") {
		                		SHD_Delete =  SHDCancel.substring(0, SHDCancel.length() - 1);
		                	}else {
		                		SHD_Delete = "";
		                	}
		                	hItem = new HashMap<>();
		                	String STT = String.valueOf(i+1); 
							  String sTenLoaiHoaDon = "Hóa đơn giá trị gia tăng";	
							  hItem.put("STT", STT);
							  hItem.put("LHDTen",sTenLoaiHoaDon);
							  hItem.put("KHMauHDon",KHMSHDon);
							  hItem.put("KHHDon",KHHDon);
							  hItem.put("TDKMPHTongSo",soTonMuaTrKy_tongSo);
							  hItem.put("TDKTuSo","");		
							  hItem.put("TDKDenSo", "");
							  hItem.put("MPHTuSo",muaTrongKy_tuSo);
							  hItem.put("MPHDenSo",muaTrongKy_denSo);
							  hItem.put("SDXBMHTuSo",muaTrongKy_tuSo);
							  hItem.put("SDXBMHDenSo",tongSoSuDung_denSo);
							  hItem.put("SDXBMHCong",tongSoSuDung_cong);		
							  hItem.put("SLSuDung", demSDSD);
							  hItem.put("XBSLuong","");
							  hItem.put("XBSo","");
							  hItem.put("MSLuong","");
							  hItem.put("MSo","");
							  hItem.put("HSLuong",denDele);		
							  hItem.put("HSo", SHD_Delete);
							  hItem.put("TCKTuSo",TStonCKy);
							  hItem.put("TCKDenSo",muaTrongKy_denSo);
							  hItem.put("TCKSLuong",tonCuoiKy_soLuong);								
							  arrayData.add(hItem);
						   }
		}
						JRDataSource jds = null;
						jds = new JRBeanCollectionDataSource(arrayData);
						
						ByteArrayOutputStream out = new ByteArrayOutputStream();
						JasperReport jr = JasperCompileManager.compileReport(new FileInputStream(f));
						JasperPrint jp = JasperFillManager.fillReport(jr, reportParams, jds);
						Exporter exporter = null;
						switch (typeExport) {
						case "pdf":
							exporter = new JRPdfExporter();
							exporter.setExporterInput(new SimpleExporterInput(jp));
							exporter.setExporterOutput(new SimpleOutputStreamExporterOutput(out));
					        SimplePdfExporterConfiguration configuration = new SimplePdfExporterConfiguration();
					        configuration.setCreatingBatchModeBookmarks(true);
					        exporter.setConfiguration(configuration);
					        exporter.exportReport();
							break;
						default:
							exporter = new HtmlExporter();						
							exporter.setExporterOutput(new SimpleHtmlExporterOutput(out));						
							exporter.setExporterInput(new SimpleExporterInput(jp));
							exporter.exportReport();
							break;
						}
						
						fileInfo.setContentFile(out.toByteArray());										
						return fileInfo;
						  
			}catch(Exception e) {
				e.printStackTrace();
				
				return fileInfo;
			}										
		}	




}	
	
	
	
