package vn.sesgroup.hddt.user.impl;

import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CreationHelper;
import org.apache.poi.ss.usermodel.DataFormat;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.xssf.streaming.SXSSFCell;
import org.apache.poi.xssf.streaming.SXSSFRow;
import org.apache.poi.xssf.streaming.SXSSFSheet;
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
import vn.sesgroup.hddt.user.dao.TaxCodeSearchStatisticsDAO;
import vn.sesgroup.hddt.utility.Constants;
import vn.sesgroup.hddt.utility.Json;

@Repository
@Transactional
public class TaxCodeSearchStatisticsImpl extends AbstractDAO implements TaxCodeSearchStatisticsDAO {
	@Autowired
	MongoTemplate mongoTemplate;

	private void buildDocMatch(String toDate, String fromDate, Document docMatch) {
		Document docMatchDateDN = null;
		LocalDate dateTo = null;
		LocalDate dateFrom = null;
		dateTo = "".equals(toDate) || !commons.checkLocalDate(toDate, Constants.FORMAT_DATE.FORMAT_DATE_WEB) ? null
				: commons.convertStringToLocalDate(toDate, Constants.FORMAT_DATE.FORMAT_DATE_WEB);
		dateFrom = "".equals(fromDate) || !commons.checkLocalDate(fromDate, Constants.FORMAT_DATE.FORMAT_DATE_WEB)
				? null
				: commons.convertStringToLocalDate(fromDate, Constants.FORMAT_DATE.FORMAT_DATE_WEB);
		if (null != dateFrom || null != dateTo) {
			docMatchDateDN = new Document();
			if (null != dateFrom)
				docMatchDateDN.append("$gte", dateFrom);
			if (null != dateTo)
				docMatchDateDN.append("$lte", dateTo);
		}

		if (null != docMatchDateDN)
			docMatch.append("SearchDate", docMatchDateDN);
	}

	@Override
	public MsgRsp list(JSONRoot jsonRoot) throws Exception {
		Msg msg = jsonRoot.getMsg();
		MsgHeader header = msg.getMsgHeader();
		MsgPage page = msg.getMsgPage();
		Object objData = msg.getObjData();

		String toDate = "";
		String fromDate = "";
		JsonNode jsonData = null;
		if (objData != null) {
			jsonData = Json.serializer().nodeFromObject(objData);
			toDate = commons.getTextJsonNode(jsonData.at("/ToDate")).replaceAll("\\s", "");
			fromDate = commons.getTextJsonNode(jsonData.at("/FromDate")).replaceAll("\\s", "");
		}

		MsgRsp rsp = new MsgRsp(header);
		MspResponseStatus responseStatus = null;
		
		Iterator<Document> iter = null;
		Document docTmp = null;
		List<Document> docs = null;

		Document docMatch = new Document("IsDelete", new Document("$ne", true));
		buildDocMatch(toDate, fromDate, docMatch);

		List<Document> pipeline = new ArrayList<Document>();
		pipeline.add(new Document("$match", docMatch));
		
		page.setFieldSort("SearchDate");
		page.setTypeSort(1);
		pipeline.addAll(createFacetForSearch(page));
		iter = mongoTemplate.getCollection("TaxCodeSearchStatistics").aggregate(pipeline).allowDiskUse(true).iterator();
		if (iter.hasNext()) {
			docTmp = iter.next();
		}
		
		if (docTmp == null) {
			responseStatus = new MspResponseStatus(999, Constants.MAP_ERROR.get(999));
			rsp.setResponseStatus(responseStatus);
			return rsp;
		}

		page.setTotalRows(docTmp.getInteger("total", 0));
		rsp.setMsgPage(page);

		
		if (docTmp != null && docTmp.get("data") instanceof List) {
			docs = docTmp.getList("data", Document.class);
		}
		List<HashMap<String, Object>> response = new ArrayList<HashMap<String, Object>>();
		
		if (null != docs) {
			HashMap<String, Object> resItem = null;
			ObjectId objectIdCTS = null;
			
			for (Document doc : docs) {
				resItem = new HashMap<String, Object>();
				
				objectIdCTS = (ObjectId) doc.get("_id");
				resItem.put("_id", objectIdCTS.toString());
				resItem.put("IssuerName", doc.get("IssuerName") != null ? doc.get("IssuerName") : "");
				resItem.put("IssuerTaxCode", doc.get("IssuerTaxCode") != null ? doc.get("IssuerTaxCode") : "");
				resItem.put("SearchDate", doc.get("SearchDate") != null ? doc.get("SearchDate") : "");
				resItem.put("TaxCodeSearched", doc.get("TaxCode") != null ? doc.get("TaxCode") : "");
				resItem.put("CompanyNameSearched", doc.get("CompanyName") != null ? doc.get("CompanyName") : "");
				resItem.put("Address", doc.get("Address") != null ? doc.get("Address") : "");
				resItem.put("Email", doc.get("Email") != null ? doc.get("Email") : "");
				resItem.put("Phone", doc.get("Phone") != null ? doc.get("Phone") : "");
				response.add(resItem);
			}
		}

		responseStatus = new MspResponseStatus(0, "Lấy thông tin thành công.");
		rsp.setResponseStatus(responseStatus);
		rsp.setObjData(response);
		return rsp;
	}

	@Override
	public FileInfo exportExcel(JSONRoot jsonRoot) throws Exception {
		Msg msg = jsonRoot.getMsg();
        Object objData = msg.getObjData();

        String toDate = "";
        String fromDate = "";
        if (objData != null) {
        	JsonNode jsonData = Json.serializer().nodeFromObject(objData);
            toDate = commons.getTextJsonNode(jsonData.at("/ToDate")).replaceAll("\\s", "");
            fromDate = commons.getTextJsonNode(jsonData.at("/FromDate")).replaceAll("\\s", "");
        }

        Document docTmp = null;
        Iterator<Document> iter = null;
        List<Document> pipeline = new ArrayList<>();
        List<Document> docs = new ArrayList<>();

        ByteArrayOutputStream out = null;
        SXSSFWorkbook wb = new SXSSFWorkbook();
        SXSSFSheet sheet = null;
        try {
            sheet = wb.createSheet("Sheet 1");
            SXSSFRow row = null;
            SXSSFCell cell = null;

            Font fontHeader = wb.createFont();
            fontHeader.setFontHeightInPoints((short) 13);
            fontHeader.setFontName("Times New Roman");
            fontHeader.setItalic(false);
            fontHeader.setBold(true);
            fontHeader.setColor(IndexedColors.BLACK.index);

            CellStyle styleHeader = null;
            styleHeader = wb.createCellStyle();
            styleHeader.setLocked(false);
            setStyleInfo(styleHeader);
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
            setStyleInfo(styleInfoL);
            styleInfoL.setAlignment(HorizontalAlignment.LEFT);

            CellStyle styleInfoC = null;
            styleInfoC = wb.createCellStyle();
            styleInfoC.setFont(fontDetail);
            styleInfoC.setLocked(false);
            setStyleInfo(styleInfoC);

            CellStyle styleInfoR = null;
            styleInfoR = wb.createCellStyle();
            styleInfoR.setFont(fontDetail);
            styleInfoR.setLocked(false);
            setStyleInfo(styleInfoR);
            
            CreationHelper createHelper = wb.getCreationHelper();
            CellStyle dateCellStyle = wb.createCellStyle();
            dateCellStyle.setDataFormat(createHelper.createDataFormat().getFormat(Constants.FORMAT_DATE.FORMAT_DATE_WEB));
            dateCellStyle.setAlignment(HorizontalAlignment.CENTER); 
            dateCellStyle.setVerticalAlignment(VerticalAlignment.CENTER);
            setStyleInfo(dateCellStyle);
            
            setCellStyle(wb);
            int countRow =1;
            
			List<String> headers = Arrays.asList(
					new String[] { "STT", "Tên issuer", "MST issuer", "Ngày tìm kiếm", "MST tìm", "Tên công ty",
							"Địa chỉ", "Email", "Điện thoại"});
			row = sheet.getRow(1);
			if (null == row)
				row = sheet.createRow(1);
			row.setHeight((short) 500);
			for (int i = 0; i < headers.size(); i++) {
				cell = row.getCell(i);
				if (cell == null)
					cell = row.createCell(i);
				cell.setCellStyle(styleHeader);
				cell.setCellValue(headers.get(i));
				switch (i) {
				case 0:
					sheet.setColumnWidth(i, 2000);
					break;
				case 1:
				case 5:
				case 6:
					sheet.setColumnWidth(i, 10000);
					break;
				default:
					 sheet.setColumnWidth(i, 5000);
					break;
				}
            }

            int posRowData = 2;

            Document docMatch = new Document("IsDelete", new Document("$ne", true));
    		buildDocMatch(toDate, fromDate, docMatch);

    		pipeline.add(new Document("$match", docMatch));
    		
    		iter = mongoTemplate.getCollection("TaxCodeSearchStatistics").aggregate(pipeline).allowDiskUse(true).iterator();
			while (iter.hasNext()) {
				docTmp = iter.next();
				if (docTmp != null) {
					docs.add(docTmp);
				}
			}

			if (null != docs) {
				row = sheet.getRow(0);
				if (null == row)
					row = sheet.createRow(0);
				cell = row.getCell(2);
				if (cell == null)
					cell = row.createCell(2);
				cell.setCellValue("Tổng");
				
				cell = row.getCell(3);
				if (cell == null)
					cell = row.createCell(3);
				cell.setCellValue(docs.size());
				
				String issuerName = null;
				String issuerTaxCode = null;
				Date date = null;
				String taxCode = null;
				String companyName = null;
				String address = null;
				String email = null;
				String phone = null;
				for (Document doc : docs) {
					issuerName = Objects.toString(doc.get("IssuerName"),"");
					issuerTaxCode = Objects.toString(doc.get("IssuerTaxCode"),"");
					date = doc.getDate("SearchDate");
					taxCode = Objects.toString(doc.get("TaxCode"),"");
					companyName = Objects.toString(doc.get("CompanyName"),"");
					address = Objects.toString(doc.get("Address"),"");
					email = Objects.toString(doc.get("Email"),"");
					phone = Objects.toString(doc.get("Phone"),"");
					
					row = sheet.getRow(posRowData);
					if (null == row)
						row = sheet.createRow(posRowData);

					cell = row.getCell(0);
					if (cell == null)
						cell = row.createCell(0);
					cell.setCellStyle(styleInfoC);
					cell.setCellValue(countRow);

					cell = row.getCell(1);
					if (cell == null)
						cell = row.createCell(1);
					cell.setCellStyle(styleInfoL);
					cell.setCellValue(issuerName);

					cell = row.getCell(2);
					if (cell == null)
						cell = row.createCell(2);
					cell.setCellStyle(styleInfoL);
					cell.setCellValue(issuerTaxCode);

					cell = row.getCell(3);
					if (cell == null)
						cell = row.createCell(3);
					cell.setCellStyle(dateCellStyle);
					if (date != null) {
						cell.setCellValue(commons.convertDateToLocalDateTime(date));
					}

					cell = row.getCell(4);
					if (cell == null)
						cell = row.createCell(4);
					cell.setCellStyle(styleInfoL);
					cell.setCellValue(taxCode);

					cell = row.getCell(5);
					if (cell == null)
						cell = row.createCell(5);
					cell.setCellStyle(styleInfoL);
					cell.setCellValue(companyName);
					
					cell = row.getCell(6);
					if (cell == null)
						cell = row.createCell(6);
					cell.setCellStyle(styleInfoL);
					cell.setCellValue(address);
					
					cell = row.getCell(7);
					if (cell == null)
						cell = row.createCell(7);
					cell.setCellStyle(styleInfoL);
					cell.setCellValue(email);

					cell = row.getCell(8);
					if (cell == null)
						cell = row.createCell(8);
					cell.setCellStyle(styleInfoL);
					cell.setCellValue(phone);
					posRowData++;
					countRow++;
                }
            }
            out = new ByteArrayOutputStream();
            wb.write(out);
            FileInfo fileInfo = new FileInfo();
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

    static void setStyleInfo(CellStyle styleInfoR) {
        styleInfoR.setAlignment(HorizontalAlignment.CENTER);
        styleInfoR.setVerticalAlignment(VerticalAlignment.CENTER);
        styleInfoR.setBorderBottom(BorderStyle.THIN);
        styleInfoR.setBorderTop(BorderStyle.THIN);
        styleInfoR.setBorderRight(BorderStyle.THIN);
        styleInfoR.setBorderLeft(BorderStyle.THIN);
        styleInfoR.setWrapText(true);
    }

    static void setCellStyle(SXSSFWorkbook wb) {
        DataFormat format = wb.createDataFormat();
        Short df = format.getFormat(wb.createDataFormat().getFormat(wb.createDataFormat().getFormat("#,##0")));
        CellStyle cellStyleNum = wb.createCellStyle();
        cellStyleNum.setDataFormat(df);
        cellStyleNum.setBorderBottom(BorderStyle.THIN);
        cellStyleNum.setBorderTop(BorderStyle.THIN);
        cellStyleNum.setBorderRight(BorderStyle.THIN);
        cellStyleNum.setBorderLeft(BorderStyle.THIN);
        cellStyleNum.setWrapText(false);
    }
}
