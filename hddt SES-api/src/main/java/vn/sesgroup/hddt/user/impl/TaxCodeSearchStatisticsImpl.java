package vn.sesgroup.hddt.user.impl;

import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

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
        Iterable<Document> cursor = null;
        Iterator<Document> iter = null;
        List<Document> pipeline = new ArrayList<>();

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
				case 5:
				case 6:
					sheet.setColumnWidth(i, 10000);
					break;
				default:
					 sheet.setColumnWidth(i, 5000);
					break;
				}
            }

            int posRowData = 1;

            Document docMatch = new Document("IsDelete", new Document("$ne", true));
            buildDocMatch(toDate, fromDate, docMatch);
            pipeline = new ArrayList<>();

            pipeline.add(new Document("$match", docMatch));
            pipeline.add(new Document("$match", new Document("TenNnt", new Document("$ne", null).append("$ne", ""))));
			pipeline.add(new Document("$sort", new Document("DSCTSSDung.DNgay", 1)));
			pipeline.add(new Document("$group",
					new Document("_id", "$MST").append("document", new Document("$first", "$$ROOT"))));
			pipeline.add(new Document("$replaceRoot", new Document("newRoot", "$document")));
			pipeline.add(new Document("$lookup",
	    		    new Document("from", "Issuer")
	    		        .append("let", new Document("issuerIdStr", "$IssuerId"))
	    		        .append("pipeline", Arrays.asList(
	    		            new Document("$match",
	    		                new Document("$expr",
	    		                    new Document("$eq", Arrays.asList(
	    		                        new Document("$toString", "$_id"),
	    		                        "$$issuerIdStr"
	    		                    ))
	    		                )
	    		            ) ,
	    		            new Document("$project", new Document("_id", 1).append("Phone", 1).append("Email", 1).append("ContactUser", 1))
	    		        ))
	    		        .append("as", "IssuerInfo")
	    		));
	    	pipeline.add(new Document("$lookup",
	    		    new Document("from", "DMTKhai")
	    		        .append("let", new Document("issuerIdStr", "$IssuerId"))
	    		        .append("pipeline", Arrays.asList(
	    		            new Document("$match",
	    		                new Document("$expr",
	    		                    new Document("$eq", Arrays.asList("$IssuerId", "$$issuerIdStr")
	    		                    		)
	    		                )
	    		            ),
	    		            new Document("$sort", new Document("InfoCreated.CreateDate", -1)),
	    		            new Document("$limit", 1),
	    		            new Document("$project", new Document("_id", 1) .append("DCTDTu", 1).append("DTLHe", 1) .append("NLHe", 1))
	    		        ))
	    		        .append("as", "DMTKhaiInfo")
	    		));
            cursor = mongoTemplate.getCollection("DMCTSo").aggregate(pipeline).allowDiskUse(true);
            iter = cursor.iterator();
            List<Document> rows = new ArrayList<>();

            while (iter.hasNext()) {
                docTmp = iter.next();
                if (docTmp != null) {
                    rows.add(docTmp);
                }
            }

			if (null != rows) {
				for (Document doc : rows) {
					String tenNnt = (String) doc.get("TenNnt");
					String taxCode = (String) doc.get("MST");
					List<Document> temp = null;
					if (doc.get("DSCTSSDung") != null && doc.get("DSCTSSDung") instanceof List) {
						temp = doc.getList("DSCTSSDung", Document.class);
					}
					Date fDate = (Date) temp.get(0).get("TNgay");
					Date tDate = (Date) temp.get(0).get("DNgay");
					String ncc = (String) temp.get(0).get("TTChuc");

					if (doc.get("IssuerInfo") != null && doc.get("IssuerInfo") instanceof List) {
						temp = doc.getList("IssuerInfo", Document.class);
					}
					String issuerPhone = (String) temp.get(0).getString("Phone");
					String issuerEmail = (String) temp.get(0).getString("Email");
					String nameUser = temp.get(0).getEmbedded(Arrays.asList("ContactUser","NameUser"), String.class);
                    String emailUser = temp.get(0).getEmbedded(Arrays.asList("ContactUser","EmailUser"), String.class);
                    String phoneUser = temp.get(0).getEmbedded(Arrays.asList("ContactUser","PhoneUser"), String.class);
                    String mailUserLh = temp.get(0).getEmbedded(Arrays.asList("ContactUser","EmailUserLh"), String.class);

					if (doc.get("DMTKhaiInfo") != null && doc.get("DMTKhaiInfo") instanceof List) {
						temp = doc.getList("DMTKhaiInfo", Document.class);
					}
					String tkhaiPhone = (String) temp.get(0).getString("DTLHe");
					String tkhaiEmail = (String) temp.get(0).getString("DCTDTu");
					String tkhaiName = (String) temp.get(0).getString("NLHe");

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
					cell.setCellValue(tenNnt);

					cell = row.getCell(2);
					if (cell == null)
						cell = row.createCell(2);
					cell.setCellStyle(styleInfoL);
					cell.setCellValue(taxCode);

					cell = row.getCell(3);
					if (cell == null)
						cell = row.createCell(3);
					cell.setCellStyle(dateCellStyle);
					cell.setCellValue(commons.convertDateToLocalDateTime(tDate));

					cell = row.getCell(4);
					if (cell == null)
						cell = row.createCell(4);
					cell.setCellStyle(dateCellStyle);
					cell.setCellValue(commons.convertDateToLocalDateTime(fDate));

					cell = row.getCell(5);
					if (cell == null)
						cell = row.createCell(5);
					cell.setCellStyle(styleInfoL);
					cell.setCellValue(ncc);
					
					cell = row.getCell(6);
					if (cell == null)
						cell = row.createCell(6);
					cell.setCellStyle(styleInfoC);
					cell.setCellValue(issuerPhone);
					
					cell = row.getCell(7);
					if (cell == null)
						cell = row.createCell(7);
					cell.setCellStyle(styleInfoL);
					cell.setCellValue(issuerEmail);

					cell = row.getCell(8);
					if (cell == null)
						cell = row.createCell(8);
					cell.setCellStyle(styleInfoL);
					cell.setCellValue(nameUser);
					
					cell = row.getCell(9);
					if (cell == null)
						cell = row.createCell(9);
					cell.setCellStyle(styleInfoL);
					cell.setCellValue(emailUser);
					
					cell = row.getCell(10);
					if (cell == null)
						cell = row.createCell(10);
					cell.setCellStyle(styleInfoL);
					cell.setCellValue(phoneUser);
					
					cell = row.getCell(11);
					if (cell == null)
						cell = row.createCell(11);
					cell.setCellStyle(styleInfoL);
					cell.setCellValue(mailUserLh);
					
					cell = row.getCell(12);
					if (cell == null)
						cell = row.createCell(12);
					cell.setCellStyle(styleInfoL);
					cell.setCellValue(tkhaiName);
					
					cell = row.getCell(13);
					if (cell == null)
						cell = row.createCell(13);
					cell.setCellStyle(styleInfoL);
					cell.setCellValue(tkhaiEmail);
					
					cell = row.getCell(14);
					if (cell == null)
						cell = row.createCell(14);
					cell.setCellStyle(styleInfoC);
					cell.setCellValue(tkhaiPhone);

					posRowData++;
					countRow++;
                }
            }
            out = new ByteArrayOutputStream();
            wb.write(out);
            FileInfo fileInfo = new FileInfo();
            fileInfo.setFileName("DANH-SACH-CA-INVOICE.xlsx");
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
