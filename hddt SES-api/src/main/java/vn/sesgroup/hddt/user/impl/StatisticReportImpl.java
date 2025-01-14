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
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;

import vn.sesgroup.hddt.configuration.ConfigConnectMongo;
import vn.sesgroup.hddt.dto.FileInfo;
import vn.sesgroup.hddt.user.dao.AbstractDAO;
import vn.sesgroup.hddt.user.dao.StatisticReportDAO;
import vn.sesgroup.hddt.user.service.TCTNService;
import vn.sesgroup.hddt.utility.Constants;
import vn.sesgroup.hddt.utility.Json;
import vn.sesgroup.hddt.utility.SystemParams;

@Repository
@Transactional
public class StatisticReportImpl extends AbstractDAO implements StatisticReportDAO {
	@Autowired ConfigConnectMongo cfg;
	@Autowired TCTNService tctnService;
	@Autowired MongoTemplate mongoTemplate;
	String SUMslhd = "";

	
	private List<Document> buildListPipeline(Object objData, MsgHeader header) throws Exception{
		List<Document> pipeline = null;
		String fromDate = "";
		String toDate = "";
		String MSTNBan = "";
		String MSTNMua = "";
		String Email = "";
		String Total = "";
		
		JsonNode jsonData = null;
		if(objData != null) {
			jsonData = Json.serializer().nodeFromObject(objData);		
			fromDate = commons.getTextJsonNode(jsonData.at("/FromDate")).replaceAll("\\s", "");
			toDate = commons.getTextJsonNode(jsonData.at("/ToDate")).replaceAll("\\s", "");
			MSTNBan = commons.getTextJsonNode(jsonData.at("/MSTNBan")).replaceAll("\\s", "");
			MSTNMua = commons.getTextJsonNode(jsonData.at("/MSTNMua")).replaceAll("\\s", "");
			Email = commons.getTextJsonNode(jsonData.at("/Email")).replaceAll("\\s", "");
			Total = commons.getTextJsonNode(jsonData.at("/Total")).replaceAll("\\s", "");
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
		
		Document docMatch = new Document("IsDelete", false);
								
		if(docMatchDate !=null)
			docMatch.append("NgayPhatHanh", docMatchDate);
		if(!MSTNBan.equals(""))
			docMatch.append("MSTNBan",
					new Document("$regex", commons.regexEscapeForMongoQuery(MSTNBan)).append("$options", "i"));
		if(!MSTNMua.equals(""))
			docMatch.append("MSTNMua",
					new Document("$regex", commons.regexEscapeForMongoQuery(MSTNMua)).append("$options", "i"));
		
		if(!Email.equals(""))
			docMatch.append("EmailGuiHoaDon",
					new Document("$regex", commons.regexEscapeForMongoQuery(Email)).append("$options", "i"));
		
		if(!Total.equals("")) {
			String cleanTotal = Total.replaceAll(",", ""); 
			double convertedTotal = Double.parseDouble(cleanTotal);
			docMatch.append("$expr", new Document("$and", Arrays.asList(		
					new Document("$lte", Arrays.asList("$TongTien", convertedTotal))
					)));
			
		}
		
		pipeline = new ArrayList<Document>();
		pipeline.add(new Document("$match", docMatch));
	
		pipeline.add(
			new Document("$sort", 
					new Document("NgayPhatHanh", -1)
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

		Document docTmp = null;
		List<Document> pipeline = new ArrayList<Document>();
		
		pipeline = buildListPipeline(objData, header);
		pipeline.addAll(createFacetForSearchNotSort(page));
		
		MongoClient mongoClient = cfg.mongoClient();
		MongoCollection<Document> collection = mongoClient.getDatabase(cfg.dbName).getCollection("BaoCaoThongKe");
		      try {
		        docTmp = collection.aggregate(pipeline).allowDiskUse(true).iterator().next();    
		      } catch (Exception e) {
		        
		      }
		        
		mongoClient.close();

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
					hItem.put("TenNBan",doc.get("TenNBan"));
					hItem.put("MSTNBan", doc.get("MSTNBan"));
					hItem.put("TenNMua", doc.get("TenNMua"));
					hItem.put("MSTNMua", doc.get("MSTNMua"));
					hItem.put("SHDon",doc.get("SHDon"));
					hItem.put("TongTien", doc.get("TongTien"));
					hItem.put("NgayPhatHanh", doc.get("NgayPhatHanh"));
					hItem.put("EmailGuiHoaDon", doc.get("EmailGuiHoaDon"));
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

		List<Document> pipeline = new ArrayList<Document>();
		pipeline = buildListPipeline(objData, header);
		
		File file = new File(SystemParams.DIR_E_INVOICE_TEMPLATE, Constants.TEMPLATE_FILE_NAME.EXCEL_BAOCAOTHONGKE);
		if(file == null || !file.exists() || !file.isFile()) {
			fileInfo.setContentFile(null);
			return fileInfo;
		}

		 String excelFilePath = SystemParams.DIR_E_INVOICE_TEMPLATE+"/"+Constants.TEMPLATE_FILE_NAME.EXCEL_BAOCAOTHONGKE;
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
			
			Iterable<Document> cursor = null;
			Iterator<Document> iter = null;
			int posRowData = 2;
			Document docTmp = null;
			int countRow = 1;
					
				cursor = mongoTemplate.getCollection("BaoCaoThongKe").aggregate(pipeline).allowDiskUse(true);
				iter = cursor.iterator();
				while(iter.hasNext()) {
				docTmp = iter.next();
				row = sheet.getRow(posRowData);
				if(null == row) row = sheet.createRow(posRowData);
				
				cell = row.getCell(0);if(cell == null) cell = row.createCell(0);cell.setCellStyle(styleInfoR);
				cell.setCellValue(countRow);
				
				cell = row.getCell(1);if(cell == null) cell = row.createCell(1);cell.setCellStyle(styleInfoC);
				cell.setCellValue(docTmp.get("TenNBan", ""));

				cell = row.getCell(2);if(cell == null) cell = row.createCell(2);cell.setCellStyle(styleInfoC);
				cell.setCellValue(docTmp.get("MSTNBan", ""));
				
				cell = row.getCell(3);if(cell == null) cell = row.createCell(3);cell.setCellStyle(styleInfoL);
				cell.setCellValue(docTmp.get("DchiNBan", ""));	
				
				cell = row.getCell(4);if(cell == null) cell = row.createCell(4);cell.setCellStyle(styleInfoL);
				cell.setCellValue(docTmp.get("SDTNBan", ""));				
				
				cell = row.getCell(5);if(cell == null) cell = row.createCell(5);cell.setCellStyle(styleInfoL);
				cell.setCellValue(docTmp.get("EmailNBan", ""));				
				
				cell = row.getCell(6);if(cell == null) cell = row.createCell(6);cell.setCellStyle(styleInfoL);
				cell.setCellValue(docTmp.get("TenNMua", ""));	
				
				cell = row.getCell(7);if(cell == null) cell = row.createCell(7);cell.setCellStyle(styleInfoL);
				cell.setCellValue(docTmp.get("MSTNMua", ""));
				
				cell = row.getCell(8);if(cell == null) cell = row.createCell(8);cell.setCellStyle(styleInfoL);
				cell.setCellValue(docTmp.get("DchiNMua", ""));
				
				cell = row.getCell(9);if(cell == null) cell = row.createCell(9);cell.setCellStyle(styleInfoL);
				cell.setCellValue(docTmp.get("SDTNMua", ""));
				
				cell = row.getCell(10);if(cell == null) cell = row.createCell(10);cell.setCellStyle(styleInfoL);
				cell.setCellValue(docTmp.get("EmailNMua", ""));
				
				cell = row.getCell(11);if(cell == null) cell = row.createCell(11);cell.setCellStyle(styleInfoL);
				cell.setCellValue(docTmp.get("EmailCCNMua", ""));
				
				cell = row.getCell(12);if(cell == null) cell = row.createCell(12);cell.setCellStyle(styleInfoC);
				Object shdon = docTmp.get("SHDon", Object.class);
				
				Class<?> objClass = shdon.getClass();
				
				if(objClass == Double.class) {
					cell.setCellValue(
							docTmp.get("SHDon", Object.class) == null? "": 
							commons.formatNumberReal(docTmp.get("SHDon", Double.class))
						);
				}else {
					cell.setCellValue(
							docTmp.get("SHDon", Object.class) == null? "": 
							commons.formatNumberReal(docTmp.get("SHDon", Integer.class))
						);
				}
				

				//ngay phat hanh
				cell = row.getCell(13);if(cell == null) cell = row.createCell(13);cell.setCellStyle(styleInfoC);
				if(null != docTmp.get("NgayPhatHanh", Object.class) && docTmp.get("NgayPhatHanh", Object.class) instanceof Date) {
					cell.setCellValue(commons.convertLocalDateTimeToString(commons.convertDateToLocalDateTime(docTmp.get("NgayPhatHanh", Date.class)), Constants.FORMAT_DATE.FORMAT_DATE_WEB));
				}
				
				//tong tien
				cell = row.getCell(14);if(cell == null) cell = row.createCell(14);cell.setCellStyle(cellStyleNum);
				cell.setCellValue(
						docTmp.get("TongTien", Object.class) == null? 0: docTmp.get("TongTien", Double.class)
				);
				
				//ngay gui hoa don
				cell = row.getCell(15);if(cell == null) cell = row.createCell(15);cell.setCellStyle(styleInfoL);
				cell.setCellValue(docTmp.get("EmailGuiHoaDon", ""));
				
				countRow++;
				posRowData++;
			}
		
			
			out = new ByteArrayOutputStream();
			wb.write(out);
			
			fileInfo = new FileInfo();
			fileInfo.setFileName("bao-cao-thong-ke.xlsx");
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
    
    
    
	@Override
	public MsgRsp backupData(JSONRoot jsonRoot) throws Exception {
		Msg msg = jsonRoot.getMsg();
		MsgHeader header = msg.getMsgHeader();
		MsgRsp rsp = new MsgRsp(header);
		MspResponseStatus responseStatus = null;

		Document docTmp = null;
		List<Document> pipeline = new ArrayList<Document>();
		Iterable<Document> cursor = null;
		Iterator<Document> iter = null;
		
		 pipeline = new ArrayList<Document>();
	        pipeline.add(new Document("$match", new Document("ActiveFlag", true)));
	        pipeline.add(new Document("$lookup", new Document("from", "LogEmailUser")
	                .append("pipeline", Arrays.asList(new Document("$match", new Document("IsDelete", false)))).append("as", "LogEmailUser")));

	        cursor = mongoTemplate.getCollection("ApiLicenseKey").aggregate(pipeline).allowDiskUse(true);
	        iter = cursor.iterator();
	        if (iter.hasNext()) {
	            docTmp = iter.next();
	        }

		rsp = new MsgRsp(header);
		responseStatus = null;

		if (docTmp.getList("LogEmailUser", Document.class).size() == 0) {
	            responseStatus = new MspResponseStatus(9999, Constants.MAP_ERROR.get(9999));
	            rsp.setResponseStatus(responseStatus);
	            return rsp;
	        }

	    List<Document> rows = null;
        if (docTmp.get("LogEmailUser") != null && docTmp.get("LogEmailUser") instanceof List) {
            rows = docTmp.getList("LogEmailUser", Document.class);
        }

		if (null != rows) {
			for (Document doc : rows) {						
				String SHDonOriginal = doc.get("Title").toString();
				String split[] = SHDonOriginal.split("Số HĐ ");
				if(split.length < 2) {
					continue;
				}
				
				String SHDonString = split[1].replace(" (No reply)", "");
					
				int SHDonInt = Integer.parseInt(SHDonString);
				
				String EmailContent = doc.get("EmailContent").toString();
				String spllitMSKH[] = EmailContent.split("Mẫu hoá đơn: ");
				String MSKH = spllitMSKH[1].substring(0, 7);
																
				String KHMSHDon = MSKH.substring(0, 1);
				String KHHDon = MSKH.substring(1);
				
				String EmailGuiHoaDon = doc.get("Email", "");
				String IssuerId = doc.get("IssuerId", "");
						
				Document findEInvoice = new Document("EInvoiceDetail.TTChung.SHDon", SHDonInt).append("EInvoiceDetail.TTChung.KHMSHDon", KHMSHDon).append("EInvoiceDetail.TTChung.KHHDon", KHHDon).append("IsDelete", false);
				
				if(KHMSHDon.equals("1")) {
					cursor = mongoTemplate.getCollection("EInvoice").find(findEInvoice);
				}else if(KHMSHDon.equals("2")) {
					cursor = mongoTemplate.getCollection("EInvoiceBH").find(findEInvoice);
				}else if(KHMSHDon.equals("6")) {
					String check = KHHDon.substring(0, 3);
					if(check.equals("T")) {
						cursor = mongoTemplate.getCollection("EInvoicePXK").find(findEInvoice);
					}else {
						cursor = mongoTemplate.getCollection("EInvoicePXKDL").find(findEInvoice);
					}												
				}
				
				docTmp = null;
				iter = cursor.iterator();
				if (iter.hasNext()) {
					docTmp = iter.next();
				}
				
				if(docTmp == null) {
					continue;
				}
				
				String TenNBan = docTmp.getEmbedded(Arrays.asList("EInvoiceDetail", "NDHDon", "NBan", "Ten"), "");
				String MSTNBan = docTmp.getEmbedded(Arrays.asList("EInvoiceDetail", "NDHDon", "NBan", "MST"), "");
				String DchiNBan = docTmp.getEmbedded(Arrays.asList("EInvoiceDetail", "NDHDon", "NBan", "DChi"), "");
				String SDTNBan = docTmp.getEmbedded(Arrays.asList("EInvoiceDetail", "NDHDon", "NBan", "SDThoai"), "");
				String EmailNBan = docTmp.getEmbedded(Arrays.asList("EInvoiceDetail", "NDHDon", "NBan", "DCTDTu"), "");
				String TenNMua = docTmp.getEmbedded(Arrays.asList("EInvoiceDetail", "NDHDon", "NMua", "Ten"), "");
				String MSTNMua = docTmp.getEmbedded(Arrays.asList("EInvoiceDetail", "NDHDon", "NMua", "MST"), "");
				String DchiNMua = docTmp.getEmbedded(Arrays.asList("EInvoiceDetail", "NDHDon", "NMua", "DChi"), "");
				String SDTNMua = docTmp.getEmbedded(Arrays.asList("EInvoiceDetail", "NDHDon", "NMua", "SDThoai"), "");
				String EmailNMua = docTmp.getEmbedded(Arrays.asList("EInvoiceDetail", "NDHDon", "NMua", "DCTDTu"), "");
				String EmailCCNMua = docTmp.getEmbedded(Arrays.asList("EInvoiceDetail", "NDHDon", "NMua", "DCTDTuCC"), "");				
				double TongTien = docTmp.getEmbedded(Arrays.asList("EInvoiceDetail","TToan", "TgTTTBSo"), 0.0);
				Date NgayPhatHanh = docTmp.getEmbedded(Arrays.asList("EInvoiceDetail", "TTChung", "NLap"),  Date.class);
				
				if(NgayPhatHanh == null) {
					continue;
				}
				
				Document docInsert = new Document("IssuerId", IssuerId)
						.append("TenNBan", TenNBan)					
						.append("MSTNBan", MSTNBan)
						.append("DchiNBan", DchiNBan)
						.append("SDTNBan", SDTNBan)
						.append("EmailNBan", EmailNBan)
						.append("TenNMua", TenNMua)
						.append("MSTNMua", MSTNMua)
						.append("DchiNMua", DchiNMua)
						.append("SDTNMua", SDTNMua)
						.append("EmailNMua", EmailNMua)
						.append("EmailCCNMua", EmailCCNMua)
						.append("SHDon", SHDonInt)
						.append("EmailGuiHoaDon", EmailGuiHoaDon)
						.append("TongTien", TongTien)
						.append("NgayPhatHanh", NgayPhatHanh)
						.append("IsDelete", false);
				
				mongoTemplate.getCollection("BaoCaoThongKe").insertOne(docInsert);
				
				}
		}

		responseStatus = new MspResponseStatus(0, "SUCCESS");
		rsp.setResponseStatus(responseStatus);
		return rsp;
	}

}
