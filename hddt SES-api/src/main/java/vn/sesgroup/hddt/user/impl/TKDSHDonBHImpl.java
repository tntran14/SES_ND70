package vn.sesgroup.hddt.user.impl;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.time.LocalDate;
import java.time.temporal.ChronoField;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.DataFormat;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.FormulaEvaluator;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.ss.util.CellRangeAddress;
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

import vn.sesgroup.hddt.dto.FileInfo;
import vn.sesgroup.hddt.user.dao.AbstractDAO;
import vn.sesgroup.hddt.user.dao.TKDSHDonBHDAO;
import vn.sesgroup.hddt.utility.Constants;
import vn.sesgroup.hddt.utility.Json;
import vn.sesgroup.hddt.utility.SystemParams;

@Repository
@Transactional
public class TKDSHDonBHImpl extends AbstractDAO implements TKDSHDonBHDAO{
	private static final Logger log = LogManager.getLogger(TKDSHDonBHImpl.class);
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
	
	//List file to export
	private List<Document> buildListPipeline1(Object objData, MsgHeader header) throws Exception{
		List<Document> pipeline1 = null;
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
			nmuaMst = commons.getTextJsonNode(jsonData.at("/NmuaMst")).trim().replaceAll("\\s+", " ");
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
		docMatch.append("EInvoiceStatus", new Document("$in", Arrays.asList(Constants.INVOICE_STATUS.COMPLETE, Constants.INVOICE_STATUS.ADJUSTED )));
		
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
		
		pipeline1 = new ArrayList<Document>();
		pipeline1.add(new Document("$match", docMatch));
		pipeline1.add(
			new Document("$addFields", 
				new Document("SHDon", 
					new Document("$ifNull", Arrays.asList("$EInvoiceDetail.TTChung.SHDon", Integer.MAX_VALUE))
				)
			)
		);
		pipeline1.add(
			new Document("$sort", 
				new Document("EInvoiceDetail.TTChung.MauSoHD", -1).append("SHDon", 1).append("_id", -1)
			)
		);
		
		return pipeline1;
	}
	//END List file to export
	
	
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
		
		cursor = mongoTemplate.getCollection("EInvoiceBH").aggregate(pipeline).allowDiskUse(true);
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
	public FileInfo exportExcelToFAST(JSONRoot jsonRoot) throws Exception {
		Msg msg = jsonRoot.getMsg();
		MsgHeader header = msg.getMsgHeader();
		MsgPage page = msg.getMsgPage();
		Object objData = msg.getObjData();
		
		List<Document> pipeline1 = new ArrayList<Document>();
		pipeline1 = buildListPipeline1(objData, header);
		
		ByteArrayOutputStream out = null;
		SXSSFWorkbook wb = new SXSSFWorkbook(1000);
		Sheet sheet = null;
		try {
			sheet = wb.createSheet("Sheet 1");
			Row row = null;
			Cell cell = null;
			
			Font fontHeader = wb.createFont();
			fontHeader.setFontHeightInPoints((short) 10);
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
			
			List<String> headers = Arrays.asList(new String[] {
					"Mã khách hàng\r\n(ma_kh)" 
					, "Người mua hàng\r\n(ong_ba)" 
					, "Quyển sổ\r\n(ma_qs)"
					, "Số seri\r\n(so_seri)"
					, "Số chứng từ\r\n(so_ct)"
					, "Ngày chứng từ\r\n(ngay_ct)"
					, "Số lượng:Q\r\n(so_luong)"
					, "Giá bán n.tệ:P1\r\n(gia_nt2)"
					, "Tiền bán n.tệ:N1\r\n(tien_nt2)"
					, "Giá vốn n.tệ:P1\r\n(gia_nt)"
					, "Tiền vốn n.tệ:N1\r\n(tien_nt)"
					, "Mã n.tệ\r\n(ma_nt)"
					, "Tỷ giá:R\r\n(ty_gia)"
					, "Giá bán:P0\r\n(gia2)"
					, "Tiền bán:N0\r\n(tien2)"
					, "Giá vốn:P0\r\n(gia)"
					, "Tiền vốn:N0\r\n(tien)"
					, "Tỷ lệ chiết khấu\r\n(tl_ck)"
					, "Tiền chiết khấu n.tệ:N1\r\n(tien_ck_nt)"		//HIDDEN
//					, "Tiền chiết khấu n.tệ:N1\r\n(tien_ck_nt)"
					, "Tiền chiết khấu:N0\r\n(tien_ck)"
					, "Mã thuế\r\n(ma_thue)"
					, "Thuế suất\r\n(thue_suat)"
					, "Tiền thuế n.tệ:N1\r\n(tien_thue_nt)"
					, "Tiền thuế:N0\r\n(tien_thue)"
					, "Mã nx (Tk nợ)\r\n(ma_nx)"
					, "Tk doanh thu\r\n(tk_dt)"
					, "Tk vật tư\r\n(tk_vt)"
					, "Tk giá vốn\r\n(tk_gv)"
					, "Tk chiết khấu\r\n(tk_ck)"
					, "Tài khoản thuế\r\n(tk_thue_co)"
					, "Mã kho\r\n(ma_kho)"
					, "Mã vật tư\r\n(ma_vt)"
					, "Diễn giải\r\n(dien_giai)"
					, "Hạn thanh toán:N\r\n(han_tt)"
					, "Loại hoá đơn\r\n(ma_gd)"
					, "Mã dự án\r\n(ma_vv_i)"
					, "Mã phí\r\n(ma_phi_i)"
					, "Khuyến mại\r\n(km_ck)"
					, "Tk cp km\r\n(tk_km_i)"
					, "Mã ĐVCS\r\n(ma_dvcs)"
					, "Hình thức TT\r\n(ht_tt)"
					});
			row = sheet.getRow(0);
			if(null == row) row = sheet.createRow(0);
			row.setHeight((short) 500);
			for(int i = 0; i < headers.size(); i++) {
				cell = row.getCell(i);
				if(cell == null) cell = row.createCell(i);
				cell.setCellStyle(styleHeader);
				cell.setCellValue(headers.get(i));
				if(i == 18)
					sheet.setColumnWidth(i, 0);
				else
					sheet.setColumnWidth(i, 5000);
			}
			Iterable<Document> cursor = null;
			Iterator<Document> iter = null;
			
			Document docTmp = null;
			Document docPrd = null;
			Document docEInvoiceDetail = null;
			String tmp = "";
			String prdCode = "";
			int pos = -1;
			
			Map<String, Document> mapPrd = new HashMap<String, Document>();
			/*LAY THONG TIN SP*/
			cursor = mongoTemplate.getCollection("DMProduct").find(
					new Document("IsDelete", false)
				)
				.projection(
					new Document("_id", 0).append("Code", 1).append("tkdt", 1).append("tkvt", 1).append("tkgv", 1)
				);
			iter = cursor.iterator();
			while(iter.hasNext()) {
				docTmp = iter.next();
				tmp = docTmp.getString("Code");
				docTmp.remove("Code");
				
				mapPrd.put(tmp, docTmp);
			}
			/*END - LAY THONG TIN SP*/
			
			int posRowData = 1;
			cursor = mongoTemplate.getCollection("EInvoiceBH").aggregate(pipeline1).allowDiskUse(true);
			iter = cursor.iterator();
			while(iter.hasNext()) {
				docTmp = iter.next();
				docEInvoiceDetail = docTmp.get("EInvoiceDetail", Document.class);
				if(docEInvoiceDetail.get("DSHHDVu") != null) {
					for(Document oo: docEInvoiceDetail.getList("DSHHDVu", Document.class)) {
						row = sheet.getRow(posRowData);
						if(null == row) row = sheet.createRow(posRowData);
						
						prdCode = "";
						if(!"".equals(oo.get("ProductCode", "")))
							prdCode	= oo.get("ProductCode", "");
						else {
							tmp = oo.get("ProductName", "");
							pos = tmp.lastIndexOf("-");
							if(pos != -1)
								prdCode = tmp.substring(pos + 1).replaceAll("\\s", "");
						}
						docPrd = null;
						docPrd = mapPrd.get(prdCode);
						
						cell = row.getCell(0);if(cell == null) cell = row.createCell(0);cell.setCellStyle(styleInfoL);
						cell.setCellValue(docEInvoiceDetail.getEmbedded(Arrays.asList("NDHDon", "NMua", "MKHang"), ""));
						
						cell = row.getCell(1);if(cell == null) cell = row.createCell(1);cell.setCellStyle(styleInfoL);
						cell.setCellValue(docEInvoiceDetail.getEmbedded(Arrays.asList("NDHDon", "NMua", "Ten"), ""));
						
						cell = row.getCell(2);if(cell == null) cell = row.createCell(2); cell.setCellStyle(styleInfoL);
						
						cell = row.getCell(3);if(cell == null) cell = row.createCell(3); cell.setCellStyle(styleInfoL);
						cell.setCellValue(
							docEInvoiceDetail.getEmbedded(Arrays.asList("TTChung", "KHMSHDon"), "") + 
							docEInvoiceDetail.getEmbedded(Arrays.asList("TTChung", "KHHDon"), "")
						);
						
						cell = row.getCell(4);if(cell == null) cell = row.createCell(4); cell.setCellStyle(styleInfoR);
						cell.setCellValue(docEInvoiceDetail.getEmbedded(Arrays.asList("TTChung", "SHDon"), new Integer(0)));
						
						cell = row.getCell(5);if(cell == null) cell = row.createCell(5); cell.setCellStyle(styleInfoC);
						if(null != docEInvoiceDetail.getEmbedded(Arrays.asList("TTChung", "NLap"), Object.class) && docEInvoiceDetail.getEmbedded(Arrays.asList("TTChung", "NLap"), Object.class) instanceof Date) {
							cell.setCellValue(commons.convertLocalDateTimeToString(commons.convertDateToLocalDateTime(docEInvoiceDetail.getEmbedded(Arrays.asList("TTChung", "NLap"), Date.class)), Constants.FORMAT_DATE.FORMAT_DATE_WEB));
						}
						
						cell = row.getCell(6);if(cell == null) cell = row.createCell(6); cell.setCellStyle(styleInfoR);
						cell.setCellValue(oo.get("Quantity", 0D));
						
						cell = row.getCell(7);if(cell == null) cell = row.createCell(7); cell.setCellStyle(styleInfoL);
						cell = row.getCell(8);if(cell == null) cell = row.createCell(8); cell.setCellStyle(styleInfoL);
						cell = row.getCell(9);if(cell == null) cell = row.createCell(9); cell.setCellStyle(styleInfoL);
						cell = row.getCell(10);if(cell == null) cell = row.createCell(10); cell.setCellStyle(styleInfoL);
						cell = row.getCell(11);if(cell == null) cell = row.createCell(11); cell.setCellStyle(styleInfoL);
						cell = row.getCell(12);if(cell == null) cell = row.createCell(12); cell.setCellStyle(styleInfoL);
						
						cell = row.getCell(13);if(cell == null) cell = row.createCell(13); cell.setCellStyle(styleInfoR);
						cell.setCellValue(oo.get("Price", 0D));
						
						cell = row.getCell(14);if(cell == null) cell = row.createCell(14); cell.setCellStyle(styleInfoR);
						cell.setCellValue(oo.get("Total", 0D));
						
						cell = row.getCell(15);if(cell == null) cell = row.createCell(15); cell.setCellStyle(styleInfoL);
						cell = row.getCell(16);if(cell == null) cell = row.createCell(16); cell.setCellStyle(styleInfoL);
						cell = row.getCell(17);if(cell == null) cell = row.createCell(17); cell.setCellStyle(styleInfoL);
						cell = row.getCell(18);if(cell == null) cell = row.createCell(18); cell.setCellStyle(styleInfoL);
						cell = row.getCell(19);if(cell == null) cell = row.createCell(19); cell.setCellStyle(styleInfoL);
						
						cell = row.getCell(20);if(cell == null) cell = row.createCell(20); cell.setCellStyle(styleInfoR);
						cell.setCellValue(oo.get("VATRate", 0D));
						
						cell = row.getCell(21);if(cell == null) cell = row.createCell(21); cell.setCellStyle(styleInfoR);
						cell.setCellValue(oo.get("VATRate", 0D));
						
						cell = row.getCell(22);if(cell == null) cell = row.createCell(22); cell.setCellStyle(styleInfoL);
						
						cell = row.getCell(23);if(cell == null) cell = row.createCell(23); cell.setCellStyle(styleInfoR);
						cell.setCellValue(oo.get("VATAmount", 0D));
						
						cell = row.getCell(24);if(cell == null) cell = row.createCell(24); cell.setCellStyle(styleInfoR);
						cell.setCellValue("131111");
						
						cell = row.getCell(25);if(cell == null) cell = row.createCell(25); cell.setCellStyle(styleInfoL);
						cell.setCellValue(docPrd == null? "": docPrd.getString("tkdt"));
						
						cell = row.getCell(26);if(cell == null) cell = row.createCell(26); cell.setCellStyle(styleInfoL);
						cell.setCellValue(docPrd == null? "": docPrd.getString("tkvt"));
						
						cell = row.getCell(27);if(cell == null) cell = row.createCell(27); cell.setCellStyle(styleInfoL);
						cell.setCellValue(docPrd == null? "": docPrd.getString("tkgv"));
						
						cell = row.getCell(28);if(cell == null) cell = row.createCell(28); cell.setCellStyle(styleInfoL);
						
						cell = row.getCell(29);if(cell == null) cell = row.createCell(29); cell.setCellStyle(styleInfoR);
						cell.setCellValue("33311");
						
						cell = row.getCell(30);if(cell == null) cell = row.createCell(30); cell.setCellStyle(styleInfoL);
						
						cell = row.getCell(31);if(cell == null) cell = row.createCell(31); cell.setCellStyle(styleInfoL);
						cell.setCellValue(oo.get("ProductName", ""));
//						tmp = oo.get("ProductName", "");
//						pos = tmp.lastIndexOf("-");
//						if(pos != -1) {
//							cell.setCellValue(tmp.substring(pos + 1).replaceAll("\\s", ""));
//						}

						cell = row.getCell(32);if(cell == null) cell = row.createCell(32); cell.setCellStyle(styleInfoL);
						cell.setCellValue("Bán Hàng");
						
						cell = row.getCell(33);if(cell == null) cell = row.createCell(33); cell.setCellStyle(styleInfoL);
						cell = row.getCell(34);if(cell == null) cell = row.createCell(34); cell.setCellStyle(styleInfoL);
						cell = row.getCell(35);if(cell == null) cell = row.createCell(35); cell.setCellStyle(styleInfoL);
						cell = row.getCell(36);if(cell == null) cell = row.createCell(36); cell.setCellStyle(styleInfoL);
						cell = row.getCell(37);if(cell == null) cell = row.createCell(37); cell.setCellStyle(styleInfoL);
						cell = row.getCell(38);if(cell == null) cell = row.createCell(38); cell.setCellStyle(styleInfoL);
						cell = row.getCell(39);if(cell == null) cell = row.createCell(39); cell.setCellStyle(styleInfoL);
						
						cell = row.getCell(40);if(cell == null) cell = row.createCell(40); cell.setCellStyle(styleInfoL);
						cell.setCellValue(docEInvoiceDetail.getEmbedded(Arrays.asList("TTChung", "HTTToan"), ""));
						
						posRowData++;
					}
				}
				
			}
			
			
			out = new ByteArrayOutputStream();
			wb.write(out);
			
			FileInfo fileInfo = new FileInfo();
			fileInfo.setFileName("DANH-SACH-HDBH-DANG-SD.xlsx");
			fileInfo.setContentFile(out.toByteArray());			
			return fileInfo;
		}catch(Exception e) {
			throw e;
		}finally {
			try{wb.dispose(); wb.close();}catch(Exception ex){}
		}
	}

	@Override
	public FileInfo exportExcelDSHDCTiet(JSONRoot jsonRoot) throws Exception {
		FileInfo fileInfo = new FileInfo();
		
		Msg msg = jsonRoot.getMsg();
		MsgHeader header = msg.getMsgHeader();
		MsgPage page = msg.getMsgPage();
		Object objData = msg.getObjData();
		
		List<Document> pipeline = new ArrayList<Document>();
		pipeline = buildListPipeline(objData, header);
		
		File file = new File(SystemParams.DIR_E_INVOICE_TEMPLATE, Constants.TEMPLATE_FILE_NAME.EXCEL_TKDSHDBH_CTIET);
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
			int posRowData = 2;
			Document docTmp = null;
			Document docEInvoiceDetail = null;
			String docEInvoiceStatus = null;
			int countRow = 1;
			
			cursor = mongoTemplate.getCollection("EInvoiceBH").aggregate(pipeline).allowDiskUse(true);
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
				cell.setCellValue(countRow);
				
				cell = row.getCell(1);if(cell == null) cell = row.createCell(1);cell.setCellStyle(styleInfoC);
				cell.setCellValue(
					docEInvoiceDetail.getEmbedded(Arrays.asList("TTChung", "KHMSHDon"), String.class)
					+ docEInvoiceDetail.getEmbedded(Arrays.asList("TTChung", "KHHDon"), String.class)
				);

				cell = row.getCell(2);if(cell == null) cell = row.createCell(2);cell.setCellStyle(styleInfoC);
				cell.setCellValue(
					docEInvoiceDetail.getEmbedded(Arrays.asList("TTChung", "SHDon"), Object.class) == null? "": 
					commons.formatNumberBillInvoice(docEInvoiceDetail.getEmbedded(Arrays.asList("TTChung", "SHDon"), Integer.class))
				);
				
				cell = row.getCell(3);if(cell == null) cell = row.createCell(3);cell.setCellStyle(styleInfoL);
				cell.setCellValue(docEInvoiceDetail.getEmbedded(Arrays.asList("NDHDon", "NMua", "MST"), ""));
				
				cell = row.getCell(4);if(cell == null) cell = row.createCell(4);cell.setCellStyle(styleInfoL);
				cell.setCellValue(docEInvoiceDetail.getEmbedded(Arrays.asList("NDHDon", "NMua", "HVTNMHang"), ""));
				
				cell = row.getCell(5);if(cell == null) cell = row.createCell(5);cell.setCellStyle(styleInfoL);
				cell.setCellValue(docEInvoiceDetail.getEmbedded(Arrays.asList("NDHDon", "NMua", "Ten"), ""));
				
				cell = row.getCell(6);if(cell == null) cell = row.createCell(6);cell.setCellStyle(styleInfoL);
				cell.setCellValue(docEInvoiceDetail.getEmbedded(Arrays.asList("NDHDon", "NMua", "DCTDTu"), ""));
				
				cell = row.getCell(7);if(cell == null) cell = row.createCell(7);cell.setCellStyle(styleInfoC);
				if(null != docEInvoiceDetail.getEmbedded(Arrays.asList("TTChung", "NLap"), Object.class) && docEInvoiceDetail.getEmbedded(Arrays.asList("TTChung", "NLap"), Object.class) instanceof Date) {
					cell.setCellValue(commons.convertLocalDateTimeToString(commons.convertDateToLocalDateTime(docEInvoiceDetail.getEmbedded(Arrays.asList("TTChung", "NLap"), Date.class)), Constants.FORMAT_DATE.FORMAT_DATE_WEB));
				}
				cell = row.getCell(8);if(cell == null) cell = row.createCell(8);cell.setCellStyle(styleInfoL);
				cell.setCellValue(status);
				
				cell = row.getCell(9);if(cell == null) cell = row.createCell(9);cell.setCellStyle(styleInfoL);
				cell.setCellValue(docEInvoiceDetail.getEmbedded(Arrays.asList("NDHDon", "NMua", "DChi"), ""));
				
				cell = row.getCell(10);if(cell == null) cell = row.createCell(10);cell.setCellStyle(styleInfoL);
				cell.setCellValue(docEInvoiceDetail.getEmbedded(Arrays.asList("TTChung", "HTTToan"), ""));
				
				cell = row.getCell(11);if(cell == null) cell = row.createCell(11);cell.setCellStyle(styleInfoL);
				cell.setCellValue(docEInvoiceDetail.getEmbedded(Arrays.asList("NDHDon", "NMua", "STKNHang"), ""));
				
				cell = row.getCell(20);if(cell == null) cell = row.createCell(20);cell.setCellStyle(cellStyleNum);
				cell.setCellValue(
					docEInvoiceDetail.getEmbedded(Arrays.asList("TToan", "TgTCThue"), Object.class) == null? 0:
					docEInvoiceDetail.getEmbedded(Arrays.asList("TToan", "TgTCThue"), Double.class)
				);
				
				cell = row.getCell(21);if(cell == null) cell = row.createCell(21);cell.setCellStyle(cellStyleNum);
				cell.setCellValue(
					docEInvoiceDetail.getEmbedded(Arrays.asList("TToan", "TgTThue"), Object.class) == null? 0:
					docEInvoiceDetail.getEmbedded(Arrays.asList("TToan", "TgTThue"), Double.class)
				);
				
				cell = row.getCell(22);if(cell == null) cell = row.createCell(22);cell.setCellStyle(cellStyleNum);
				cell.setCellValue(
					docEInvoiceDetail.getEmbedded(Arrays.asList("TToan", "TgTTTBSo"), Object.class) == null? 0:
					docEInvoiceDetail.getEmbedded(Arrays.asList("TToan", "TgTTTBSo"), Double.class)
				);
				
				cell = row.getCell(23);if(cell == null) cell = row.createCell(23);cell.setCellStyle(styleInfoL);
				cell.setCellValue(docEInvoiceDetail.getEmbedded(Arrays.asList("TToan", "TgTTTBChu"), ""));
				
				if(docEInvoiceDetail.get("DSHHDVu") != null && docEInvoiceDetail.getList("DSHHDVu", Document.class).size() > 0) {
					for(Document o: docEInvoiceDetail.getList("DSHHDVu", Document.class)) {
						row = sheet.getRow(posRowData);
						if(null == row) row = sheet.createRow(posRowData);
						
						cell = row.getCell(12);if(cell == null) cell = row.createCell(12);cell.setCellStyle(styleInfoL);
						cell.setCellValue("");
						
						cell = row.getCell(13);if(cell == null) cell = row.createCell(13);cell.setCellStyle(styleInfoL);
						cell.setCellValue(o.get("ProductName", ""));
						
						cell = row.getCell(14);if(cell == null) cell = row.createCell(14);cell.setCellStyle(styleInfoL);
						cell.setCellValue(o.get("Unit", ""));
						
						cell = row.getCell(15);if(cell == null) cell = row.createCell(15);cell.setCellStyle(cellStyleNum);
						cell.setCellValue(o.get("Quantity", Object.class) == null? 0:o.get("Quantity", Double.class));
						
						cell = row.getCell(16);if(cell == null) cell = row.createCell(16);cell.setCellStyle(cellStyleNum);
						cell.setCellValue(o.get("Price", Object.class) == null? 0:o.get("Price", Double.class));
						
						
						
						String feature = o.get("Feature", "");
						
				
						
						if(feature.equals("3")) {
							double total = o.get("Total", Object.class) == null? 0:o.get("Total", Double.class) ;
							
							String check_total = commons.formatNumberReal(total);
							String total_ =  "(" + check_total + ")";
							cell = row.getCell(17);if(cell == null) cell = row.createCell(17);cell.setCellStyle(cellStyleCheck);
							cell.setCellValue(total_);
						}else {
							cell = row.getCell(17);if(cell == null) cell = row.createCell(17);cell.setCellStyle(cellStyleNum);
							cell.setCellValue(o.get("Total", Object.class) == null? 0:o.get("Total", Double.class));
						}
						
						cell = row.getCell(18);if(cell == null) cell = row.createCell(18);cell.setCellStyle(cellStyleNum);
						cell.setCellValue(o.get("VATRate", Object.class) == null? 0:o.get("VATRate", Double.class));
						
						
						if(feature.equals("3")) {
							
							double VATAmount = o.get("VATAmount", Object.class) == null? 0:o.get("VATAmount", Double.class);
							String check_VATAmount = commons.formatNumberReal(VATAmount);				
							String VATAmount_ =  "(" + check_VATAmount + ")";
							cell = row.getCell(19);if(cell == null) cell = row.createCell(19);cell.setCellStyle(cellStyleCheck);							
							cell.setCellValue(VATAmount_);
							
						}else {
							cell = row.getCell(19);if(cell == null) cell = row.createCell(19);cell.setCellStyle(cellStyleNum);
							cell.setCellValue(o.get("VATAmount", Object.class) == null? 0:o.get("VATAmount", Double.class));
						}
						
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
			fileInfo.setFileName("chi-tiet-danh-sach-hoa-don-ban-hang.xlsx");
			fileInfo.setContentFile(out.toByteArray());			
			return fileInfo;
		}catch(Exception e) {
			throw e;
		}finally {
			try{wb.close();}catch(Exception ex){}
		}
	}

	@Override
	public FileInfo exportExceGeneral(JSONRoot jsonRoot) throws Exception {
		FileInfo fileInfo = new FileInfo();
		
		Msg msg = jsonRoot.getMsg();
		MsgHeader header = msg.getMsgHeader();
		MsgPage page = msg.getMsgPage();
		Object objData = msg.getObjData();
		
		String fromDate = "";
		String toDate = "";
		JsonNode jsonData = null;
		if(objData != null) {
			jsonData = Json.serializer().nodeFromObject(objData);
			fromDate = commons.getTextJsonNode(jsonData.at("/FromDate")).replaceAll("\\s", "");
			toDate = commons.getTextJsonNode(jsonData.at("/ToDate")).replaceAll("\\s", "");
		}
		
		List<Document> pipeline = new ArrayList<Document>();
		pipeline = buildListPipeline(objData, header);
		
		File file = new File(SystemParams.DIR_E_INVOICE_TEMPLATE, Constants.TEMPLATE_FILE_NAME.EXCEL_TKDSHDBH_GENERAL);
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
			
//			Font fontDetail = wb.createFont();
//			fontDetail.setFontHeightInPoints((short)10);
//			fontDetail.setFontName("Times New Roman");
//			fontDetail.setItalic(false);
//			
//			CellStyle styleInfoL = null;
//			styleInfoL = wb.createCellStyle();
//			styleInfoL.setFont(fontDetail);
//			styleInfoL.setLocked(false);
//			styleInfoL.setAlignment(HorizontalAlignment.LEFT);
//			styleInfoL.setVerticalAlignment(VerticalAlignment.CENTER);
//			styleInfoL.setBorderBottom(BorderStyle.THIN);
//			styleInfoL.setBorderTop(BorderStyle.THIN);
//			styleInfoL.setBorderRight(BorderStyle.THIN);
//			styleInfoL.setBorderLeft(BorderStyle.THIN);
//			styleInfoL.setWrapText(false);
//			
//			CellStyle styleInfoC = null;
//			styleInfoC  = wb.createCellStyle();
//			styleInfoC.setFont(fontDetail);
//			styleInfoC.setLocked(false);
//			styleInfoC.setAlignment(HorizontalAlignment.CENTER);
//			styleInfoC.setVerticalAlignment(VerticalAlignment.CENTER);
//			styleInfoC.setBorderBottom(BorderStyle.THIN);
//			styleInfoC.setBorderTop(BorderStyle.THIN);
//			styleInfoC.setBorderRight(BorderStyle.THIN);
//			styleInfoC.setBorderLeft(BorderStyle.THIN);
//			styleInfoC.setWrapText(false);
//			
//			CellStyle styleInfoR = null;
//			styleInfoR  = wb.createCellStyle();
//			styleInfoR.setFont(fontDetail);
//			styleInfoR.setLocked(false);
//			styleInfoR.setAlignment(HorizontalAlignment.RIGHT);
//			styleInfoR.setVerticalAlignment(VerticalAlignment.CENTER);								
//			styleInfoR.setBorderBottom(BorderStyle.THIN);
//			styleInfoR.setBorderTop(BorderStyle.THIN);
//			styleInfoR.setBorderRight(BorderStyle.THIN);
//			styleInfoR.setBorderLeft(BorderStyle.THIN);
//			styleInfoR.setWrapText(false);
//			
//			DataFormat format = wb.createDataFormat();
//			Short df = format.getFormat(wb.createDataFormat().getFormat(wb.createDataFormat().getFormat("#,##0")));
//			CellStyle cellStyleNum = wb.createCellStyle();
//			cellStyleNum.setDataFormat(df);							
//			cellStyleNum.setBorderBottom(BorderStyle.THIN);
//			cellStyleNum.setBorderTop(BorderStyle.THIN);
//			cellStyleNum.setBorderRight(BorderStyle.THIN);
//			cellStyleNum.setBorderLeft(BorderStyle.THIN);			
//			cellStyleNum.setWrapText(false);
			
			ObjectId objectId = null;
			Iterable<Document> cursor = null;
			Iterator<Document> iter = null;
			int posRowData = 2;
			Document docTmp = null;
			Document docEInvoiceDetail = null;
			Document docEInvoiceData = null;
			int countRow = 1;
			
			row = sheet.getRow(2);
			if(null == row) row = sheet.createRow(2);
			cell = row.getCell(3);
			if(cell == null) cell = row.getCell(3);
			cell.setCellValue("[1] Kỳ tính thuế: " + fromDate + " đến " + toDate);
			
			try {
				objectId = new ObjectId(header.getIssuerId());
			}catch(Exception e) {}
			/*LAY THONG TIN KHACH HANG*/
			cursor = mongoTemplate.getCollection("Issuer").find(new Document("_id", objectId));
			iter = cursor.iterator();
			if(iter.hasNext()) {
				docTmp = iter.next();
				
				row = sheet.getRow(3);
				cell = row.getCell(1);
				if(cell == null) cell = row.getCell(1);
				cell.setCellValue("[02] Tên người nộp thuế: " + docTmp.get("Name", ""));
				
				row = sheet.getRow(4);
				cell = row.getCell(1);
				if(cell == null) cell = row.getCell(1);
				cell.setCellValue("[03] Mã số thuế: " + docTmp.get("TaxCode", ""));
			}
			
			List<HashMap<String, Object>> arrayData = new ArrayList<>();
			HashMap<String, Object> hItem = null;
			double tax = -999D;
			double taxTrThue = -999D;
			double taxThue = -999D;
			
			List<Document> prds = null;
			int pos = 0;
			StringJoiner sj10 = null;
			StringJoiner sj8 = null;
			StringJoiner sj5 = null;
			StringJoiner sj0 = null;
			StringJoiner sjKCT = null;
			
			double totalBeforTax = 0D;
			double totalTax = 0D;
		
//			pipeline.addAll(createFacetForSearchNotSort(page));
			cursor = mongoTemplate.getCollection("EInvoiceBH").aggregate(pipeline).allowDiskUse(true);
//			page.setTotalRows(docTmp.getInteger("total", 0));
		
			iter = cursor.iterator();
			while(iter.hasNext()) {
				docTmp = iter.next();
				
				
				docEInvoiceDetail = docTmp.get("EInvoiceDetail", Document.class);
				prds = docEInvoiceDetail.get("DSHHDVu", List.class);
				
				tax = 0D;
				taxTrThue = 0D;
				taxThue = 0D;
				pos = 0;
				totalBeforTax = 0D;
				totalTax = 0D;
						
		
				sj0 = new StringJoiner(", ");
			
				hItem = new HashMap<>();

		
				Double tax0 = 0.0;
		
				
		
				
				Double taxTotal0 = 0.0;
				Double taxVATAmount0 = 0.0;
		
				
		
				int dem0 = 0;
				int demKCT = 0;
				if(null != prds) {
				
					for(Document doc: prds){
						if(pos == 0) {
							tax = doc.get("VATRate", 0D);	
							taxTrThue = doc.get("Total", 0D);
							
							
							taxThue = doc.get("VATAmount", 0D);
//							String a =  commons.formatNumberReal(taxTrThue);
						
							 if(tax==0) {
								tax0 = tax;
								sj0.add(doc.get("ProductName", ""));	
								if(doc.get("Feature").equals("3")) {
									taxTotal0 = taxTotal0 - taxTrThue;
								}
								else {
									taxTotal0 += taxTrThue;
								}
							
								taxVATAmount0 += taxThue;
								dem0++;
							}
						
					}	
							
					}
					pos++;
					
				}
				
				// TIEN THUE CUA HOA DON THAY THE VA LOICQT = 0 AND GHI CHU TRANG THAI O MUC GHI CHU
				String Ghichu= (String) docTmp.get("EInvoiceStatus");
				if("REPLACED".equals(docTmp.get("EInvoiceStatus"))) {

					taxTotal0 = 0.0;
					taxVATAmount0= 0.0;
					Ghichu = "Đã thay thế";
    			}
				else if("DELETED".equals(docTmp.get("EInvoiceStatus"))){

					taxTotal0 = 0.0;
					taxVATAmount0= 0.0;
				
					Ghichu = "Đã hủy";
    			}
				else if("ERROR_CQT".equals(docTmp.get("EInvoiceStatus"))){

					taxTotal0 = 0.0;
					taxVATAmount0= 0.0;
				
					Ghichu = "Lỗi CQT";
    			}else if("ADJUSTED".equals(docTmp.get("EInvoiceStatus"))){    				
					Ghichu = "Đã điều chỉnh";
    			}else {  					
					Ghichu = "Đang sử dụng";
    			}
			if (tax0==0) {
						
						hItem.put("Tax", tax0);
						hItem.put("Col02", docEInvoiceDetail.getEmbedded(Arrays.asList("TTChung", "KHMSHDon"), "") + docEInvoiceDetail.getEmbedded(Arrays.asList("TTChung", "KHHDon"), ""));
						hItem.put("Col03", docEInvoiceDetail.getEmbedded(Arrays.asList("TTChung", "SHDon"), 0));
						hItem.put("Col04", 
							null == docEInvoiceDetail.get("TTChung", "NLap") || !(docEInvoiceDetail.getEmbedded(Arrays.asList("TTChung", "NLap"), Object.class) instanceof Date)?
							"":
							commons.convertLocalDateTimeToString(commons.convertDateToLocalDateTime(docEInvoiceDetail.getEmbedded(Arrays.asList("TTChung", "NLap"), Date.class)), Constants.FORMAT_DATE.FORMAT_DATE_WEB)
						);
						hItem.put("Col05", docEInvoiceDetail.getEmbedded(Arrays.asList("NDHDon", "NMua", "HVTNMHang"), ""));
						hItem.put("Col06", docEInvoiceDetail.getEmbedded(Arrays.asList("NDHDon", "NMua", "MST"), ""));
						hItem.put("Col07", docEInvoiceDetail.getEmbedded(Arrays.asList("NDHDon", "NMua", "Ten"), ""));
						hItem.put("Col08", sj0.toString());	///LSI SP

						hItem.put("Col09", taxTotal0);
						hItem.put("Col10", taxVATAmount0);
						hItem.put("Col11", Ghichu);
						arrayData.add(hItem);
						}
					
					else if (tax0==0 ){	
					HashMap<String, Object> hItem1 = null;
            		hItem1 = new LinkedHashMap<String, Object>();
            		HashMap<String, Object> hItem2 = null;
            		hItem2 = new LinkedHashMap<String, Object>();
            		HashMap<String, Object> hItem3 = null;
            		hItem3 = new LinkedHashMap<String, Object>();
              		HashMap<String, Object> hItem4 = null;
            		hItem4 = new LinkedHashMap<String, Object>();
            	
						if(dem0>0) {
							
							hItem3.put("Tax", tax0);
							hItem3.put("Col02", docEInvoiceDetail.getEmbedded(Arrays.asList("TTChung", "KHMSHDon"), "") + docEInvoiceDetail.getEmbedded(Arrays.asList("TTChung", "KHHDon"), ""));
							hItem3.put("Col03", docEInvoiceDetail.getEmbedded(Arrays.asList("TTChung", "SHDon"), 0));
							hItem3.put("Col04", 
								null == docEInvoiceDetail.get("TTChung", "NLap") || !(docEInvoiceDetail.getEmbedded(Arrays.asList("TTChung", "NLap"), Object.class) instanceof Date)?
								"":
								commons.convertLocalDateTimeToString(commons.convertDateToLocalDateTime(docEInvoiceDetail.getEmbedded(Arrays.asList("TTChung", "NLap"), Date.class)), Constants.FORMAT_DATE.FORMAT_DATE_WEB)
							);
							hItem3.put("Col05", docEInvoiceDetail.getEmbedded(Arrays.asList("NDHDon", "NMua", "HVTNMHang"), ""));
							hItem3.put("Col06", docEInvoiceDetail.getEmbedded(Arrays.asList("NDHDon", "NMua", "MST"), ""));
							hItem3.put("Col07", docEInvoiceDetail.getEmbedded(Arrays.asList("NDHDon", "NMua", "Ten"), ""));
							hItem3.put("Col08", sj0.toString());	///LSI SP

							hItem3.put("Col09", taxTotal0);
							hItem3.put("Col10", taxVATAmount0);
							hItem3.put("Col11", Ghichu);
							arrayData.add(hItem3);
						}	
						
				}				
			}
			
			/*SORT ARRAY*/
			Comparator<HashMap<String, Object>> valueComparator = (e1, e2) -> ((Double) e1.get("Tax")).compareTo((Double) e2.get("Tax"));
			arrayData.sort(valueComparator);
			/*END SORT ARRAY*/
			
			/*FONT - STYLE*/
			Font font = wb.createFont();
			font.setFontHeightInPoints((short) 11);
			font.setFontName("Times New Roman");
			font.setItalic(false);
			font.setBold(true);
			
			CellStyle styleHeaderTaxInfo = null;
			styleHeaderTaxInfo  = wb.createCellStyle();
			styleHeaderTaxInfo.setFont(font);
			styleHeaderTaxInfo.setLocked(false);
			styleHeaderTaxInfo.setAlignment(HorizontalAlignment.CENTER);
			styleHeaderTaxInfo.setVerticalAlignment(VerticalAlignment.CENTER);
			styleHeaderTaxInfo.setWrapText(false);
			styleHeaderTaxInfo.setBorderTop(BorderStyle.THIN);
			styleHeaderTaxInfo.setBorderRight(BorderStyle.THIN);
			styleHeaderTaxInfo.setBorderBottom(BorderStyle.THIN);
			styleHeaderTaxInfo.setBorderLeft(BorderStyle.THIN);
			
			font = wb.createFont();
			font.setFontHeightInPoints((short) 10);
			font.setFontName("Times New Roman");
			font.setItalic(false);
			font.setBold(false);
			
			CellStyle styleData = null;
			styleData  = wb.createCellStyle();
			styleData.setFont(font);
			styleData.setLocked(false);
//			styleData.setAlignment(HorizontalAlignment.LEFT);
			styleData.setVerticalAlignment(VerticalAlignment.CENTER);
			styleData.setWrapText(true);
			styleData.setBorderTop(BorderStyle.THIN);
			styleData.setBorderRight(BorderStyle.THIN);
			styleData.setBorderBottom(BorderStyle.THIN);
			styleData.setBorderLeft(BorderStyle.THIN);
			
			DataFormat format = wb.createDataFormat();
			Short df = format.getFormat(wb.createDataFormat().getFormat(wb.createDataFormat().getFormat("#,##0;(#,##0)")));
			CellStyle cellStyleNum = wb.createCellStyle();
			cellStyleNum.setDataFormat(df);							
			cellStyleNum.setBorderBottom(BorderStyle.THIN);
			cellStyleNum.setBorderTop(BorderStyle.THIN);
			cellStyleNum.setBorderRight(BorderStyle.THIN);
			cellStyleNum.setBorderLeft(BorderStyle.THIN);			
			cellStyleNum.setWrapText(true);
			cellStyleNum.setAlignment(HorizontalAlignment.RIGHT);
			cellStyleNum.setVerticalAlignment(VerticalAlignment.CENTER);
			
			int posRow = 10;
			double taxTmp = -999;
			tax = -999;
			int seq = 0;
			String tmp = "";
			int startRow = -1;
			
			double total = 0;
			double totalHasVAT = 0;
			double totalVATAmount = 0;
			
			for(HashMap<String, Object> o: arrayData) {
				taxTmp = (Double) o.get("Tax");
				if(tax != taxTmp) {
					/*THEM DONG TONG KHI THAY DOI LOAI THUE*/
					if(seq > 0) {
						row = sheet.getRow(posRow);
						if(null == row) row = sheet.createRow(posRow);
						
						cell = row.getCell(1);
						if(cell == null) cell = row.createCell(1);
						cell.setCellValue("Tổng cộng");
						cell.setCellStyle(styleHeaderTaxInfo);
						
						for(int ii = 2; ii <= 11; ii++) {
							cell = row.getCell(ii);
							if(cell == null) cell = row.createCell(ii);
							cell.setCellStyle(styleHeaderTaxInfo);
						}
						
						sheet.addMergedRegion(new CellRangeAddress(posRow, posRow, 1, 8));
						
						cell = row.getCell(9);
						if(cell == null) cell = row.createCell(9);
						cell.setCellStyle(styleHeaderTaxInfo);
						cell.setCellFormula("SUM(J" + (posRow - startRow + 1) + ":J" + posRow + ")");
						cell.setCellStyle(cellStyleNum);
						
						cell = row.getCell(10);
						if(cell == null) cell = row.createCell(10);
						cell.setCellStyle(styleHeaderTaxInfo);
						cell.setCellFormula("SUM(K" + (posRow - startRow + 1) + ":K" + posRow + ")");
						cell.setCellStyle(cellStyleNum);
						
						posRow++;
					}
					
					seq++;
					tax = taxTmp;
					startRow = 1;
					
				
						tmp = "Danh sách hàng hoá, dịch vụ";
				
					
					/*WRITE HEADER TAX*/
					row = sheet.getRow(posRow);
					if(null == row) row = sheet.createRow(posRow);
					cell = row.getCell(1);
					if(cell == null) cell = row.createCell(1);
					cell.setCellValue(tmp);
					cell.setCellStyle(styleHeaderTaxInfo);
					
					for(int ii = 2; ii <= 11; ii++) {
						cell = row.getCell(ii);
						if(cell == null) cell = row.createCell(ii);
						cell.setCellStyle(styleHeaderTaxInfo);
					}
					
					sheet.addMergedRegion(new CellRangeAddress(posRow, posRow, 1, 11));
					
					posRow++;
				}else {
					startRow++;
				}
				
				/*GHI THONG TIN HOA DON*/
				row = sheet.getRow(posRow);
				if(null == row) row = sheet.createRow(posRow);
				cell = row.getCell(1);
				if(cell == null) cell = row.createCell(1);
				cell.setCellValue(startRow);
				cell.setCellStyle(styleData);
				
				for(int ii = 2; ii <= 11; ii++) {
					tmp = "Col" + StringUtils.leftPad(String.valueOf(ii), 2, "0");
					switch (ii) {
					case 2:
					case 3:
					case 4:
					case 5:
					case 6:
					case 7:
					case 8:
					case 11:
						cell = row.getCell(ii);
						if(cell == null) cell = row.createCell(ii);
						cell.setCellValue(o.get(tmp).toString());
						cell.setCellStyle(styleData);
						break;
					case 9:
					case 10:
						cell = row.getCell(ii);
						if(cell == null) cell = row.createCell(ii);
						cell.setCellValue(null == o.get(tmp)? 0: (double) o.get(tmp));
						cell.setCellStyle(cellStyleNum);
						break;
					default:
						break;
					}
				}
				
				/*GHI THONG TIN HOA DON*/
				total += null == o.get("Col09")? 0: (double) o.get("Col09");
				if(tax >= 0) {
					totalHasVAT += null == o.get("Col09")? 0: (double) o.get("Col09");
					totalVATAmount += null == o.get("Col10")? 0: (double) o.get("Col10");
				}
//				if(posRow == 127)
//					System.out.println(posRow);
				posRow++;
			}
			/*XU LY DONG TONG CUOI CUNG*/
			row = sheet.getRow(posRow);
			if(null == row) row = sheet.createRow(posRow);
			
			cell = row.getCell(1);
			if(cell == null) cell = row.createCell(1);
			cell.setCellValue("Tổng cộng");
			cell.setCellStyle(styleHeaderTaxInfo);
			
			for(int ii = 2; ii <= 11; ii++) {
				cell = row.getCell(ii);
				if(cell == null) cell = row.createCell(ii);
				cell.setCellStyle(styleHeaderTaxInfo);
			}
			
			sheet.addMergedRegion(new CellRangeAddress(posRow, posRow, 1, 8));
			
			cell = row.getCell(9);
			if(cell == null) cell = row.createCell(9);
			cell.setCellStyle(styleHeaderTaxInfo);
			cell.setCellFormula("SUM(J" + (posRow - startRow + 1) + ":J" + posRow + ")");
			cell.setCellStyle(cellStyleNum);
			
			cell = row.getCell(10);
			if(cell == null) cell = row.createCell(10);
			cell.setCellStyle(styleHeaderTaxInfo);
			cell.setCellFormula("SUM(K" + (posRow - startRow + 1) + ":K" + posRow + ")");
			cell.setCellStyle(cellStyleNum);
			/*END - XU LY DONG TONG CUOI CUNG*/
			
			font = wb.createFont();
			font.setFontHeightInPoints((short) 10);
			font.setFontName("Times New Roman");
			font.setItalic(false);
			font.setBold(true);

			CellStyle cellStyleNumFooter = wb.createCellStyle();	
			cellStyleNumFooter.setFont(font);
			cellStyleNumFooter.setWrapText(true);
//			cellStyleNumFooter.setAlignment(HorizontalAlignment.LEFT);
			cellStyleNumFooter.setVerticalAlignment(VerticalAlignment.CENTER);
			
			posRow++;
			row = sheet.getRow(posRow);
			if(null == row) row = sheet.createRow(posRow);
			for(int ii = 1; ii <= 11; ii++) {
				cell = row.getCell(ii);
				if(cell == null) cell = row.createCell(ii);
				cell.setCellStyle(cellStyleNumFooter);
			}
			
			cell = row.getCell(1);
			if(cell == null) cell = row.createCell(1);
			cell.setCellValue("Tổng doanh thu hàng hóa, dịch vụ bán ra (*): ");
			sheet.addMergedRegion(new CellRangeAddress(posRow, posRow, 1, 6));
			
			cell = row.getCell(7);
			if(cell == null) cell = row.createCell(7);
			cell.setCellValue(total);
			
			posRow++;
			row = sheet.getRow(posRow);
			if(null == row) row = sheet.createRow(posRow);
			for(int ii = 1; ii <= 11; ii++) {
				cell = row.getCell(ii);
				if(cell == null) cell = row.createCell(ii);
				cell.setCellStyle(cellStyleNumFooter);
			}
			
//			cell = row.getCell(1);
//			if(cell == null) cell = row.createCell(1);
//			cell.setCellValue("Tổng thuế GTGT của hàng hóa, dịch vụ bán ra (***):");
//			sheet.addMergedRegion(new CellRangeAddress(posRow, posRow, 1, 6));
//			
//			cell = row.getCell(7);
//			if(cell == null) cell = row.createCell(7);
//			cell.setCellValue(totalHasVAT);
//			
//			posRow++;
//			row = sheet.getRow(posRow);
//			if(null == row) row = sheet.createRow(posRow);
//			for(int ii = 1; ii <= 11; ii++) {
//				cell = row.getCell(ii);
//				if(cell == null) cell = row.createCell(ii);
//				cell.setCellStyle(cellStyleNumFooter);
//			}
//			
//			cell = row.getCell(1);
//			if(cell == null) cell = row.createCell(1);
//			cell.setCellValue("Tổng thuế GTGT của hàng hóa, dịch vụ bán ra (***):");
//			sheet.addMergedRegion(new CellRangeAddress(posRow, posRow, 1, 6));
//			
//			cell = row.getCell(7);
//			if(cell == null) cell = row.createCell(7);
//			cell.setCellValue(totalVATAmount);
			
			FormulaEvaluator evaluator = wb.getCreationHelper().createFormulaEvaluator();
			evaluator.evaluateAll();		//TINH GIA TRI LAI CAC CONG THUC
			
			/****************************************************************/
			font = wb.createFont();
			font.setFontHeightInPoints((short) 10);
			font.setFontName("Times New Roman");
			font.setItalic(true);
			font.setBold(false);

			cellStyleNumFooter = wb.createCellStyle();	
			cellStyleNumFooter.setFont(font);
			cellStyleNumFooter.setWrapText(true);
			cellStyleNumFooter.setAlignment(HorizontalAlignment.CENTER);
			cellStyleNumFooter.setVerticalAlignment(VerticalAlignment.CENTER);
			
			LocalDate now = LocalDate.now();
			
			posRow++;
			row = sheet.getRow(posRow);
			if(null == row) row = sheet.createRow(posRow);
			cell = row.getCell(8);
			if(cell == null) cell = row.createCell(8);
			cell.setCellValue(
					String.format("Ngày %s tháng %s năm %s"
					, StringUtils.leftPad(String.valueOf(now.get(ChronoField.DAY_OF_MONTH)), 2, "0")
					, StringUtils.leftPad(String.valueOf(now.get(ChronoField.MONTH_OF_YEAR)), 2, "0")
					, StringUtils.leftPad(String.valueOf(now.get(ChronoField.YEAR)), 4, "0")
					)
				);
			cell.setCellStyle(cellStyleNumFooter);
			sheet.addMergedRegion(new CellRangeAddress(posRow, posRow, 8, 10));
			
			font = wb.createFont();
			font.setFontHeightInPoints((short) 10);
			font.setFontName("Times New Roman");
			font.setItalic(false);
			font.setBold(true);

			cellStyleNumFooter = wb.createCellStyle();	
			cellStyleNumFooter.setFont(font);
			cellStyleNumFooter.setWrapText(true);
			cellStyleNumFooter.setAlignment(HorizontalAlignment.CENTER);
			cellStyleNumFooter.setVerticalAlignment(VerticalAlignment.CENTER);

			posRow++;
			row = sheet.getRow(posRow);
			if(null == row) row = sheet.createRow(posRow);
			row.setHeight((short) 800);
			
			cell = row.getCell(2);
			if(cell == null) cell = row.createCell(2);
			cell.setCellValue("NHÂN VIÊN ĐẠI LÝ THUẾ");
			cell.setCellStyle(cellStyleNumFooter);
			sheet.addMergedRegion(new CellRangeAddress(posRow, posRow, 2, 4));						
			cell = row.getCell(8);
			if(cell == null) cell = row.createCell(8);
			cell.setCellValue("NGƯỜI NỘP THUẾ hoặc\r\n" + 
					"ĐẠI DIỆN HỢP PHÁP CỦA NGƯỜI NỘP THUẾ");
			cell.setCellStyle(cellStyleNumFooter);
			sheet.addMergedRegion(new CellRangeAddress(posRow, posRow, 8, 10));
			
			font = wb.createFont();
			font.setFontHeightInPoints((short) 10);
			font.setFontName("Times New Roman");
			font.setItalic(true);
			font.setBold(false);

			cellStyleNumFooter = wb.createCellStyle();	
			cellStyleNumFooter.setFont(font);
			cellStyleNumFooter.setWrapText(true);
			cellStyleNumFooter.setAlignment(HorizontalAlignment.CENTER);
			cellStyleNumFooter.setVerticalAlignment(VerticalAlignment.TOP);
			
			posRow++;
			row = sheet.getRow(posRow);
			if(null == row) row = sheet.createRow(posRow);
			row.setHeight((short) 1500);
			
			cell = row.getCell(2);
			if(cell == null) cell = row.createCell(2);
			cell.setCellValue("Họ và tên:\r\n" + 
					"Chứng chỉ hành nghề số:");
			cell.setCellStyle(cellStyleNumFooter);
			sheet.addMergedRegion(new CellRangeAddress(posRow, posRow, 2, 4));
			cell = row.getCell(8);
			if(cell == null) cell = row.createCell(8);
			cell.setCellValue("Ký, ghi rõ họ tên; chức vụ và đóng dấu (nếu có)");
			cell.setCellStyle(cellStyleNumFooter);
			sheet.addMergedRegion(new CellRangeAddress(posRow, posRow, 8, 10));
			
			font = wb.createFont();
			font.setFontHeightInPoints((short) 10);
			font.setFontName("Times New Roman");
			font.setItalic(false);
			font.setBold(true);
			font.setUnderline(org.apache.poi.ss.usermodel.Font.U_SINGLE);

			cellStyleNumFooter = wb.createCellStyle();	
			cellStyleNumFooter.setFont(font);
			cellStyleNumFooter.setWrapText(true);
			cellStyleNumFooter.setAlignment(HorizontalAlignment.LEFT);
			cellStyleNumFooter.setVerticalAlignment(VerticalAlignment.CENTER);

			posRow++;
			row = sheet.getRow(posRow);
			if(null == row) row = sheet.createRow(posRow);
			row.setHeight((short) 300);
			
//			cell = row.getCell(1);
//			if(cell == null) cell = row.createCell(1);
//			cell.setCellValue("Ghi chú:");
//			cell.setCellStyle(cellStyleNumFooter);
//			sheet.addMergedRegion(new CellRangeAddress(posRow, posRow, 1, 11));
			
			font = wb.createFont();
			font.setFontHeightInPoints((short) 10);
			font.setFontName("Times New Roman");
			font.setItalic(false);
			font.setBold(false);

			cellStyleNumFooter = wb.createCellStyle();	
			cellStyleNumFooter.setFont(font);
			cellStyleNumFooter.setWrapText(true);
			cellStyleNumFooter.setAlignment(HorizontalAlignment.LEFT);
			cellStyleNumFooter.setVerticalAlignment(VerticalAlignment.CENTER);

			posRow++;
			row = sheet.getRow(posRow);
			if(null == row) row = sheet.createRow(posRow);
			row.setHeight((short) 1000);
			
//			cell = row.getCell(1);
//			if(cell == null) cell = row.createCell(1);
//			cell.setCellValue("(*) Tổng doanh thu hàng hóa, dịch vụ bán ra là tổng cộng số liệu tại cột 8 của dòng tổng của tất cả các chỉ tiêu.\r\n" + 
//					"(**) Tổng doanh thu hàng hóa, dịch vụ bán ra chịu thuế GTGT là tổng cộng số liệu tại cột 8 của dòng tổng của các chỉ tiêu chịu thuế.\r\n" + 
//					"(***) Tổng số thuế GTGT của hàng hóa, dịch vụ bán ra là tổng cộng số liệu tại cột 9 của dòng tổng của các chỉ tiêu chịu thuế.");
//			cell.setCellStyle(cellStyleNumFooter);	
//			sheet.addMergedRegion(new CellRangeAddress(posRow, posRow, 1, 11));
			
			out = new ByteArrayOutputStream();
			wb.write(out);
			
			fileInfo = new FileInfo();
			fileInfo.setFileName(Constants.TEMPLATE_FILE_NAME.EXCEL_TKDSHDBH_GENERAL);
			fileInfo.setContentFile(out.toByteArray());			
			return fileInfo;
		}catch(Exception e) {
			throw e;
		}finally {
			try{wb.close();}catch(Exception ex){}
		}
	}
}
