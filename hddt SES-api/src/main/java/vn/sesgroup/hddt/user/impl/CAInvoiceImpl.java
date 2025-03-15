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
import vn.sesgroup.hddt.user.dao.CAInvoiceDAO;
import vn.sesgroup.hddt.user.service.TCTNService;
import vn.sesgroup.hddt.utility.Constants;
import vn.sesgroup.hddt.utility.Json;

@Repository
@Transactional
public class CAInvoiceImpl extends AbstractDAO implements CAInvoiceDAO {
    @Autowired
    MongoTemplate mongoTemplate;
    @Autowired
    TCTNService tctnService;

    String SUMslhd = "";

    @Override
    public MsgRsp list(JSONRoot jsonRoot) throws Exception {
        Msg msg = jsonRoot.getMsg();
        MsgHeader header = msg.getMsgHeader();
        MsgPage page = msg.getMsgPage();
        Object objData = msg.getObjData();

        String name = "";
        String mst = "";
        String toDate = "";
        String fromDate = "";
        JsonNode jsonData = null;
        if (objData != null) {
            jsonData = Json.serializer().nodeFromObject(objData);
            name = commons.getTextJsonNode(jsonData.at("/Name")).replaceAll("\\s+", " ");
            mst = commons.getTextJsonNode(jsonData.at("/TaxCode")).replaceAll("\\s+", "");
            toDate = commons.getTextJsonNode(jsonData.at("/ToDate")).replaceAll("\\s", "");
            fromDate = commons.getTextJsonNode(jsonData.at("/FromDate")).replaceAll("\\s", "");
        }

        MsgRsp rsp = new MsgRsp(header);
        MspResponseStatus responseStatus = null;

        Document docTmp = null;
        Iterable<Document> cursor = null;
        Iterator<Document> iter = null;
        List<Document> pipeline = new ArrayList<Document>();


        pipeline = new ArrayList<Document>();
        pipeline.add(new Document("$match", new Document("ActiveFlag", true)));

        Document docMatch = new Document("IsDelete", new Document("$ne", true));
        buildDocMatch(name, mst, toDate, fromDate, docMatch);


        pipeline = new ArrayList<Document>();
        pipeline.add(new Document("$match", docMatch));
        pipeline.add(new Document("$match", new Document("TenNnt", new Document("$ne", null).append("$ne", ""))));
        //3. sort CA nhỏ -> Lớn
    	pipeline.add(
				  new Document("$sort", 
				    new Document("DSCTSSDung.DNgay", -1) 
				  )  
				);
    	pipeline.add(new Document("$group", new Document("_id", "$MST")
    		    .append("document", new Document("$first", "$$ROOT"))));

    	pipeline.add(new Document("$replaceRoot", new Document("newRoot", "$document")));
        pipeline.addAll(createFacetForSearchNotSort(page));
        cursor = mongoTemplate.getCollection("DMCTSo").aggregate(pipeline).allowDiskUse(true);
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
                ObjectId objectIdCTS = null;
                objectIdCTS = (ObjectId) doc.get("_id");
                List<Document> rows1 = null;
                if (doc.get("DSCTSSDung") != null && doc.get("DSCTSSDung") instanceof List) {
                    rows1 = doc.getList("DSCTSSDung", Document.class);
                }
                hItem = new HashMap<String, Object>();
                hItem.put("_id", objectIdCTS.toString());
                hItem.put("TenNnt", doc.get("TenNnt"));
                hItem.put("MST", doc.get("MST"));
                if (null != rows1) {
                    for (Document doc1 : rows1) {
                        hItem.put("TenNCC", doc1.get("TTChuc"));
                        hItem.put("TuNgay", doc1.get("TNgay"));
                        hItem.put("DenNgay", doc1.get("DNgay"));
                        rowsReturn.add(hItem);
                    }
                }
            }
        }

        String CKS_EXPIRES = String.valueOf(docTmp.getInteger("total", 0));
        responseStatus = new MspResponseStatus(0, CKS_EXPIRES);
        rsp.setResponseStatus(responseStatus);
    	HashMap<String, Object> mapDataR = new HashMap<String, Object>();
		mapDataR.put("rows", rowsReturn);
		rsp.setObjData(mapDataR);
		return rsp;
    }


    public FileInfo exportExcel(JSONRoot jsonRoot) throws Exception {
        Msg msg = jsonRoot.getMsg();
        Object objData = msg.getObjData();

        String name = "";
        String mst = "";
        String toDate = "";
        String fromDate = "";
        JsonNode jsonData = null;
        if (objData != null) {
            jsonData = Json.serializer().nodeFromObject(objData);
            name = commons.getTextJsonNode(jsonData.at("/Name")).replaceAll("\\s+", " ");
            mst = commons.getTextJsonNode(jsonData.at("/TaxCode")).replaceAll("\\s", "");
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
            fontHeader.setColor(IndexedColors.WHITE.index);

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
            dateCellStyle.setAlignment(HorizontalAlignment.CENTER); // Căn giữa ngang
            dateCellStyle.setVerticalAlignment(VerticalAlignment.CENTER);
            setStyleInfo(dateCellStyle);
            
            setCellStyle(wb);
            int countRow =1;
			List<String> headers = Arrays.asList(
					new String[] { "STT", "Khách hàng", "Mã số thuế", "Ngày hết hạn", "Ngày bắt đầu", "Nhà cung cấp" });
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
                else if (i == 1)
                    sheet.setColumnWidth(i, 8000);
                else if (i == 2)
                    sheet.setColumnWidth(i, 6000);
                else if (i == 3)
                    sheet.setColumnWidth(i, 5000);
                else if (i == 4)
                    sheet.setColumnWidth(i, 5000);
                else if (i == 5)
                    sheet.setColumnWidth(i, 10000);
                else
                    sheet.setColumnWidth(i, 3000);
            }

            int posRowData = 1;

            Document docMatch = new Document("IsDelete", new Document("$ne", true));
            buildDocMatch(name, mst, toDate, fromDate, docMatch);
            pipeline = new ArrayList<>();

            pipeline.add(new Document("$match", docMatch));
            pipeline.add(new Document("$match", new Document("TenNnt", new Document("$ne", null).append("$ne", ""))));
			pipeline.add(new Document("$sort", new Document("DSCTSSDung.DNgay", -1)));
			pipeline.add(new Document("$group",
					new Document("_id", "$MST").append("document", new Document("$first", "$$ROOT"))));

			pipeline.add(new Document("$replaceRoot", new Document("newRoot", "$document")));
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
                    List<Document> rows1 = null;
                    if (doc.get("DSCTSSDung") != null && doc.get("DSCTSSDung") instanceof List) {
                        rows1 = doc.getList("DSCTSSDung", Document.class);
                    }
                    String TenNnt = (String) doc.get("TenNnt");
                    String MST = (String) doc.get("MST");

                    if (null != rows1) {
                        for (Document doc1 : rows1) {
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
                            cell.setCellValue(TenNnt);

                            cell = row.getCell(2);
                            if (cell == null)
                                cell = row.createCell(2);
                            cell.setCellStyle(styleInfoL);
                            cell.setCellValue(MST);

                            cell = row.getCell(3);
                            if (cell == null)
                                cell = row.createCell(3);
                            cell.setCellStyle(dateCellStyle);
                            cell.setCellValue(commons.convertDateToLocalDateTime(doc1.get("DNgay", Date.class)));
                            
                            cell = row.getCell(4);
                            if (cell == null)
                                cell = row.createCell(4);
                            cell.setCellStyle(dateCellStyle);
                            cell.setCellValue(commons.convertDateToLocalDateTime(doc1.get("TNgay", Date.class)));

                            cell = row.getCell(5);
                            if (cell == null)
                                cell = row.createCell(5);
                            cell.setCellStyle(styleInfoL);
                            cell.setCellValue(doc1.get("TTChuc", ""));

                            posRowData++;
                            countRow++;

                        }
                    }

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

    private void buildDocMatch(String name, String mst, String toDate, String fromDate, Document docMatch) {
//        Document docMatchDateTN = null;
        Document docMatchDateDN = null;
        LocalDate dateTo = null;
        LocalDate dateFrom = null;
        dateTo = "".equals(toDate) || !commons.checkLocalDate(toDate, Constants.FORMAT_DATE.FORMAT_DATE_WEB) ?
                null : commons.convertStringToLocalDate(toDate, Constants.FORMAT_DATE.FORMAT_DATE_WEB);
        dateFrom = "".equals(fromDate) || !commons.checkLocalDate(fromDate, Constants.FORMAT_DATE.FORMAT_DATE_WEB) ?
                null : commons.convertStringToLocalDate(fromDate, Constants.FORMAT_DATE.FORMAT_DATE_WEB);
        if (null != dateFrom || null != dateTo) {
//            docMatchDateTN = new Document();
            docMatchDateDN = new Document();
            if (null != dateFrom)
            	docMatchDateDN.append("$gte", dateFrom);
            if (null != dateTo)
                docMatchDateDN.append("$lte", dateTo);
        }

        if (!"".equals(name))
            docMatch.append("TenNnt", new Document("$regex", commons.regexEscapeForMongoQuery(name)).append("$options", "i"));

        if (!"".equals(mst))
            docMatch.append("MST", commons.regexEscapeForMongoQuery(mst));
//        if (null != docMatchDateTN)
//            docMatch.append("DSCTSSDung.TNgay", docMatchDateTN);
        if (null != docMatchDateDN)
            docMatch.append("DSCTSSDung.DNgay", docMatchDateDN);
    }

}
