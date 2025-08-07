package vn.sesgroup.hddt.user.impl;

import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.time.ZoneId;
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
import vn.sesgroup.hddt.user.dao.ClientSignatureInfoDAO;
import vn.sesgroup.hddt.utility.Constants;
import vn.sesgroup.hddt.utility.Json;

@Repository
@Transactional
public class ClientSignatureInfoImpl extends AbstractDAO implements ClientSignatureInfoDAO {
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
				docMatchDateDN.append("$lt", dateTo.plusDays(1));
		}

		if (null != docMatchDateDN)
			docMatch.append("InfoCreated.CreateDate", docMatchDateDN);
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

		Document docMatch = new Document();
		buildDocMatch(toDate, fromDate, docMatch);

		List<Document> pipeline = new ArrayList<Document>();
		pipeline.add(new Document("$match", docMatch));
		
		page.setFieldSort("InfoCreated.CreateDate");
		page.setTypeSort(-1);
		pipeline.addAll(createFacetForSearch(page));
		iter = mongoTemplate.getCollection("ClientSignatureInfo").aggregate(pipeline).allowDiskUse(true).iterator();
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
			ObjectId objectId = null;
			
			for (Document doc : docs) {
				resItem = new HashMap<String, Object>();
				
				objectId = (ObjectId) doc.get("_id");
				resItem.put("_id", objectId.toString());
				resItem.put("SerialNumber", doc.get("SerialNumber") != null ? doc.get("SerialNumber") : "");
				resItem.put("IssuerDN", doc.get("IssuerDN") != null ? doc.get("IssuerDN") : "");
				resItem.put("SubjectDN", doc.get("SubjectDN") != null ? doc.get("SubjectDN") : "");
				resItem.put("ValidFrom", doc.get("ValidFrom") != null ? doc.get("ValidFrom") : "");
				resItem.put("ValidTo", doc.get("ValidTo") != null ? doc.get("ValidTo") : "");
				resItem.put("ClientOf", doc.get("ClientOf") != null ? doc.get("ClientOf") : "");
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
        MsgPage page = msg.getMsgPage();
        
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
            int countRow = 1;
            
			List<String> headers = Arrays.asList(
					new String[] { "STT", "Doanh nghiệp", "Nhà cung cấp", "Từ ngày", "Đến ngày", "Serial",
							"Doanh nghiệp quan hệ"});
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
				switch (i) {
				case 0:
					sheet.setColumnWidth(i, 2000);
					break;
				case 1:
				case 2:
				case 6:
					sheet.setColumnWidth(i, 15000);
					break;
				case 5:
					sheet.setColumnWidth(i, 11000);
					break;
				default:
					 sheet.setColumnWidth(i, 5000);
					break;
				}
            }

            int posRowData = 1;

            Document docMatch = new Document();
    		buildDocMatch(toDate, fromDate, docMatch);
 
    		iter = mongoTemplate.getCollection("ClientSignatureInfo").aggregate(pipeline).allowDiskUse(true).iterator();
    		while (iter.hasNext()) {
				docTmp = iter.next();
				if (docTmp != null) {
					docs.add(docTmp);
				}
			}

			if (null != docs) {
				String issuerDN = null;
				String subjectDN = null;
				Date validFrom = null;
				Date validTo = null;
				String serialNumber = null;
				String clientOf = null;
				for (Document doc : docs) {
					issuerDN = doc.get("IssuerDN","");
					subjectDN = doc.get("SubjectDN","");
					validFrom = doc.getDate("ValidFrom");
					validTo = doc.getDate("ValidTo");
					serialNumber = doc.get("SerialNumber","");
					clientOf = doc.getEmbedded(Arrays.asList("ClientOf","UserName"),"") + " - " +doc.getEmbedded(Arrays.asList("ClientOf","UserFullName"),"");
					
					
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
					cell.setCellValue(subjectDN);

					cell = row.getCell(2);
					if (cell == null)
						cell = row.createCell(2);
					cell.setCellStyle(styleInfoL);
					cell.setCellValue(issuerDN);

					cell = row.getCell(3);
					if (cell == null)
						cell = row.createCell(3);
					cell.setCellStyle(dateCellStyle);
					if (validFrom != null) {
						cell.setCellValue(commons.convertDateToLocalDateTime(validFrom));
					}

					cell = row.getCell(4);
					if (cell == null)
						cell = row.createCell(4);
					cell.setCellStyle(dateCellStyle);
					if (validTo != null) {
						cell.setCellValue(commons.convertDateToLocalDateTime(validTo));
					}

					cell = row.getCell(5);
					if (cell == null)
						cell = row.createCell(5);
					cell.setCellStyle(styleInfoC);
					cell.setCellValue(serialNumber);
					
					cell = row.getCell(6);
					if (cell == null)
						cell = row.createCell(6);
					cell.setCellStyle(styleInfoL);
					cell.setCellValue(clientOf);
					
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
