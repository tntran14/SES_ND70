package vn.sesgroup.hddt.user.impl;

import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
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
import com.mongodb.client.model.FindOneAndUpdateOptions;
import com.mongodb.client.model.ReturnDocument;

import vn.sesgroup.hddt.dto.FileInfo;
import vn.sesgroup.hddt.user.dao.AbstractDAO;
import vn.sesgroup.hddt.user.dao.IssuContractExpiresDAO;
import vn.sesgroup.hddt.user.service.TCTNService;
import vn.sesgroup.hddt.utility.Constants;
import vn.sesgroup.hddt.utility.Json;

@Repository
@Transactional
public class IssuContractExpiresImpl extends AbstractDAO implements IssuContractExpiresDAO {
    private static final Logger log = LogManager.getLogger(IssuContractExpiresImpl.class);
    @Autowired
    MongoTemplate mongoTemplate;
    @Autowired
    TCTNService tctnService;

    String SUMslhd = "";

    public MsgRsp crud(JSONRoot jsonRoot) throws Exception {
        Msg msg = jsonRoot.getMsg();
        MsgHeader header = msg.getMsgHeader();
        MsgPage page = msg.getMsgPage();
        Object objData = msg.getObjData();
        Boolean quyen = false;
        JsonNode jsonData = null;
        if (objData != null) {
            jsonData = Json.serializer().nodeFromObject(msg.getObjData());
        } else {
            throw new Exception("Lỗi dữ liệu đầu vào");
        }

        String actionCode = header.getActionCode();
        String id = commons.getTextJsonNode(jsonData.at("/_id"));

        MsgRsp rsp = new MsgRsp(header);
        rsp.setMsgPage(page);
        MspResponseStatus responseStatus = null;

        List<Document> pipeline = null;
        Document docFind = null;
        Document docTmp = null;
        Document docUpsert = null;
        Document docUpsertUser = null;
        Iterable<Document> cursor = null;
        Iterator<Document> iter = null;
        FindOneAndUpdateOptions options = null;
        String taxCode = "";
        HashMap<String, String> hR = new HashMap<String, String>();

        ObjectId objectId = null;

        switch (actionCode) {
            case Constants.MSG_ACTION_CODE.ACTIVE:
                objectId = null;

                try {
                    objectId = new ObjectId(id);
                } catch (Exception ex) {
                }


                /*KIEM TRA THONG TIN KHACH HANG - USER - TINH THANH - CO QUAN THUE CO TON TAI KHONG*/
                docFind = new Document("_id", objectId).append("IsActive", true).append("IsDelete", new Document("$ne", true));
                pipeline = new ArrayList<Document>();
                pipeline.add(new Document("$match", docFind));
                cursor = mongoTemplate.getCollection("Issuer").aggregate(pipeline);
                iter = cursor.iterator();
                if (iter.hasNext()) {
                    docTmp = iter.next();
                }

                if (null == docTmp) {
                    responseStatus = new MspResponseStatus(9999, "Không tìm thấy thông tin khách hàng.");
                    rsp.setResponseStatus(responseStatus);
                    return rsp;
                }

                options = new FindOneAndUpdateOptions();
                options.upsert(false);
                options.maxTime(5000, TimeUnit.MILLISECONDS);
                options.returnDocument(ReturnDocument.AFTER);

                Document docR = mongoTemplate.getCollection("Issuer").findOneAndUpdate(docFind,
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
                } catch (Exception ex) {
                }


                /*KIEM TRA THONG TIN KHACH HANG - USER - TINH THANH - CO QUAN THUE CO TON TAI KHONG*/
                docFind = new Document("_id", objectId).append("IsActive", true).append("IsDelete", new Document("$ne", true));
                pipeline = new ArrayList<Document>();
                pipeline.add(new Document("$match", docFind));
                cursor = mongoTemplate.getCollection("Issuer").aggregate(pipeline);
                iter = cursor.iterator();
                if (iter.hasNext()) {
                    docTmp = iter.next();
                }

                if (null == docTmp) {
                    responseStatus = new MspResponseStatus(9999, "Không tìm thấy thông tin khách hàng.");
                    rsp.setResponseStatus(responseStatus);
                    return rsp;
                }

                options = new FindOneAndUpdateOptions();
                options.upsert(false);
                options.maxTime(5000, TimeUnit.MILLISECONDS);
                options.returnDocument(ReturnDocument.AFTER);

                docR = mongoTemplate.getCollection("Issuer").findOneAndUpdate(docFind,
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

        String soluong = "";
        String mst = "";
        String gh = "";
        String tyle = "";

        JsonNode jsonData = null;
        if (objData != null) {
            jsonData = Json.serializer().nodeFromObject(objData);
            soluong = commons.getTextJsonNode(jsonData.at("/SoLuong")).replaceAll("\\s", "");
            mst = commons.getTextJsonNode(jsonData.at("/TaxCode")).replaceAll("\\s", "");
            tyle = commons.getTextJsonNode(jsonData.at("/TyLe")).replaceAll("\\s", "");
        }

        MsgRsp rsp = new MsgRsp(header);
        MspResponseStatus responseStatus = null;
        ObjectId objectId = null;
        Document docTmp = null;
        Iterable<Document> cursor = null;
        Iterator<Document> iter = null;
        List<Document> pipeline = new ArrayList<Document>();
        Document docMatch = new Document("IsDelete", new Document("$ne", true));
        if (!"".equals(mst))
            docMatch.append("TaxCode", commons.regexEscapeForMongoQuery(mst));

        pipeline = new ArrayList<Document>();
        
        pipeline.add(new Document("$lookup", new Document("from", "Issuer")
                .append("localField", "TaxCode")
                .append("foreignField", "TaxCode")
                .append("as", "issuer")));

        pipeline.add(new Document("$unwind", "$issuer"));
        pipeline.add(new Document("$lookup", new Document("from", "Contract")
                .append("localField", "TaxCode")
                .append("foreignField", "NMUA.TaxCode")
                .append("as", "contract")));
        // Thêm các pha $lookup và $group khác vào pipeline tại đây
        pipeline.add(new Document("$lookup", new Document("from", "EInvoice")
                .append("let", new Document("issuerId", new Document("$toString", "$issuer._id")))
                .append("pipeline", Arrays.asList(
                        new Document("$match", new Document("$expr",
                                new Document("$eq", Arrays.asList("$$issuerId", "$IssuerId")))),
                        new Document("$match", new Document("SignStatusCode", "SIGNED")),
                        new Document("$group", new Document("_id", null)
                                .append("count", new Document("$sum", 1)))
                ))
                .append("as", "EInvoice")));
        
        pipeline.add(new Document("$lookup", new Document("from", "EInvoiceBH")
                .append("let", new Document("issuerId", new Document("$toString", "$issuer._id")))
                .append("pipeline", Arrays.asList(
                        new Document("$match", new Document("$expr",
                                new Document("$eq", Arrays.asList("$$issuerId", "$IssuerId")))),
                        new Document("$match", new Document("SignStatusCode", "SIGNED")),
                        new Document("$group", new Document("_id", null)
                                .append("count", new Document("$sum", 1)))
                ))
                .append("as", "EInvoiceBH")));

        pipeline.add(new Document("$lookup", new Document("from", "EInvoicePXKDL")
                .append("let", new Document("issuerId", new Document("$toString", "$issuer._id")))
                .append("pipeline", Arrays.asList(
                        new Document("$match", new Document("$expr",
                                new Document("$eq", Arrays.asList("$$issuerId", "$IssuerId")))),
                        new Document("$match", new Document("SignStatusCode", "SIGNED")),
                        new Document("$group", new Document("_id", null)
                                .append("count", new Document("$sum", 1)))
                ))
                .append("as", "EInvoicePXKDL")));

        pipeline.add(new Document("$lookup", new Document("from", "EInvoicePXK")
                .append("let", new Document("issuerId", new Document("$toString", "$issuer._id")))
                .append("pipeline", Arrays.asList(
                        new Document("$match", new Document("$expr",
                                new Document("$eq", Arrays.asList("$$issuerId", "$IssuerId")))),
                        new Document("$match", new Document("SignStatusCode", "SIGNED")),
                        new Document("$group", new Document("_id", null)
                                .append("count", new Document("$sum", 1)))
                ))
                .append("as", "EInvoicePXK")));

        pipeline.add(new Document("$lookup", new Document("from", "EInvoiceMTT")
                .append("let", new Document("issuerId", new Document("$toString", "$issuer._id")))
                .append("pipeline", Arrays.asList(
                        new Document("$match", new Document("$expr",
                                new Document("$eq", Arrays.asList("$$issuerId", "$IssuerId")))),
                        new Document("$match", new Document("SignStatusCode", "SIGNED")),
                        new Document("$group", new Document("_id", null)
                                .append("count", new Document("$sum", 1)))
                ))
                .append("as", "EInvoiceMTT")));
        pipeline.add(new Document("$project", new Document("_id", 1)
                .append("TaxCode", 1)
                .append("SLHDon", 1)
                .append("issuer", "$issuer.Name")
                .append("contract", new Document("$map", new Document("input", "$contract")
                        .append("as", "item")
                        .append("in", new Document("Contract", "$$item.Contract.SHDon"))))
                .append("EInvoice", new Document("$arrayElemAt", Arrays.asList("$EInvoice.count", 0)))
                .append("EInvoiceBH", new Document("$arrayElemAt", Arrays.asList("$EInvoiceBH.count", 0)))
                .append("EInvoicePXKDL", new Document("$arrayElemAt", Arrays.asList("$EInvoicePXKDL.count", 0)))
                .append("EInvoicePXK", new Document("$arrayElemAt", Arrays.asList("$EInvoicePXK.count", 0)))
                .append("EInvoiceMTT", new Document("$arrayElemAt", Arrays.asList("$EInvoiceMTT.count", 0)))));

        pipeline.add(new Document("$addFields", new Document("TotalInvoices", new Document("$toInt", new Document("$sum", Arrays.asList("$EInvoice", "$EInvoiceBH", "$EInvoicePXKDL", "$EInvoicePXK", "$EInvoiceMTT"))))));

        
        pipeline.add(new Document("$addFields", new Document("RemainingRatio", 
        		   new Document("$toInt", 
        		     new Document("$multiply",  
        		       Arrays.asList(
        		         new Document("$subtract", Arrays.asList(1, 
        		           new Document("$divide", Arrays.asList("$TotalInvoices", "$SLHDon"))
        		         )), 100
        		       )
        		     )
        		   ) 
        		 )));
        
        pipeline.add(new Document("$addFields", 
        		  new Document("RemainingRatioInt",  
        		    new Document("$toInt",
        		      new Document("$multiply",
        		         Arrays.asList(
        		           new Document("$subtract", 
        		             Arrays.asList(
        		               1, 
        		               new Document("$divide", 
        		                 Arrays.asList("$TotalInvoices", "$SLHDon")
        		               )
        		             )
        		           ),
        		           100
        		         )  
        		      )
        		    )
        		  )
        		));
        
        pipeline.addAll(createFacetForSearchNotSort(page));

        cursor = mongoTemplate.getCollection("DMDepot").aggregate(pipeline).allowDiskUse(true);
        iter = cursor.iterator();
        if (iter.hasNext()) {
            docTmp = iter.next();
        }
//Đã lấy được dữ liệu theo trang bắt đầu xử lý
    	page.setTotalRows(docTmp.getInteger("total", 0));
		rsp.setMsgPage(page);

		List<Document> rows = null;
		if (docTmp.get("data") != null && docTmp.get("data") instanceof List) {
			rows = docTmp.getList("data", Document.class);
		}
		ArrayList<HashMap<String, Object>> rowsReturn = new ArrayList<HashMap<String, Object>>();
		HashMap<String, Object> hItem = null;
		if (null != rows) {
			for (Document doc : rows) {
				objectId = (ObjectId) doc.get("_id");
		        int SoLuongConLai = (int) doc.get("SLHDon") - (int) doc.get("TotalInvoices");
		        String SoHDong = "";
		        List<Document> contractList = (List<Document>) doc.get("contract");
		        List<String> contractValues = new ArrayList<>();
		        for (Document contractDoc : contractList) {
		        	SoHDong += contractDoc.getString("Contract")+",";
		        }
		        
		        
				hItem = new HashMap<String, Object>();
				hItem.put("_id", objectId.toString());
				hItem.put("TaxCode", doc.get("TaxCode"));
				hItem.put("Name", doc.get("issuer"));
				hItem.put("SoHDong",SoHDong);
				hItem.put("SoLuongDaCap", doc.get("SLHDon"));
				hItem.put("SoLuongPH", doc.get("TotalInvoices"));
				hItem.put("SoLuongConLai",SoLuongConLai );
				hItem.put("SoLuongDaDung", doc.get("TotalInvoices"));
				hItem.put("TiLe", doc.get("RemainingRatio"));
				hItem.put("Status", doc.get("Status"));
				rowsReturn.add(hItem);
			}
		}

        responseStatus = new MspResponseStatus(0, "SUCCESS");
        rsp.setResponseStatus(responseStatus);
        rsp.setMsgPage(page);
        rsp.getMsgHeader().setAdmin(true);
        HashMap<String, Object> mapDataR = new HashMap<String, Object>();
        mapDataR.put("rows", rowsReturn);
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

        Document docTmp = null;
        Iterable<Document> cursor = mongoTemplate.getCollection("Contract").find(docFind);
        Iterator<Document> iter = cursor.iterator();
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
        MsgHeader header = msg.getMsgHeader();
        MsgPage page = msg.getMsgPage();
        Object objData = msg.getObjData();

        String tyle = "";
        String type = "";
        String fromDate = "";
        String toDate = "";

        JsonNode jsonData = null;
        if (objData != null) {
            jsonData = Json.serializer().nodeFromObject(objData);
            type = commons.getTextJsonNode(jsonData.at("/Type")).replaceAll("\\s", "");
            fromDate = commons.getTextJsonNode(jsonData.at("/FromDate")).replaceAll("\\s", "");
            toDate = commons.getTextJsonNode(jsonData.at("/ToDate")).replaceAll("\\s", "");
        }

        List<Document> rows3 = null;
        ObjectId objectId = null;
        Document docTmp = null;
        Document docTmp1 = null;
        Document docTmp2 = null;
        Iterable<Document> cursor = null;
        Iterator<Document> iter = null;
        Iterable<Document> cursor1 = null;
        Iterator<Document> iter1 = null;
        List<Document> pipeline = new ArrayList<Document>();
        List<Document> pipeline1 = new ArrayList<Document>();
        List<Document> pipeline2 = new ArrayList<Document>();

        ByteArrayOutputStream out = null;
        SXSSFWorkbook wb = new SXSSFWorkbook(1000);
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
            List<String> headers_week = Arrays.asList(new String[]{"STT", "Tên đơn vị", "Mã số thuế", "Số hợp đồng",
                    "Còn lại", "SL sử dụng HD theo tuần", "Từ ngày", "Đến ngày",
                    "Thời gian liên hệ (Tuần)", "Trạng thái"});
            List<String> headers_month = Arrays.asList(new String[]{"STT", "Tên đơn vị", "Mã số thuế", "Số hợp đồng",
                    "Còn lại", "SL sử dụng HD theo tháng", "Từ ngày", "Đến ngày",
                    "Thời gian liên hệ (Tháng)", "Trạng thái"});

            if (type.equals("WEEK")) {
                calcWidthHeight(sheet, styleHeader, headers_week, 8000, 7500);

            } else {
                calcWidthHeight(sheet, styleHeader, headers_month, 7500, 8000);

            }

            int posRowData = 1;

            Document docMatch = new Document("IsDelete", new Document("$ne", true));
            pipeline = new ArrayList<Document>();
            pipeline.add(new Document("$match", docMatch));

            pipeline.add(new Document("$sort", new Document("_id", -1)));

            cursor = mongoTemplate.getCollection("DMDepot").aggregate(pipeline).allowDiskUse(true);
            iter = cursor.iterator();
            while (iter.hasNext()) {
                docTmp = iter.next();

                String TaxCode = docTmp.get("TaxCode", "");
                int soLuongDaCap = docTmp.get("SLHDon", 0);
                int soLuongCLKho = docTmp.get("SLHDonCL", 0);
                int soLuongPH = 0;
                int soLuongConLai = 0;
                int soLuongDaDung = 0;
                Document findIssuer_ = new Document("TaxCode", TaxCode).append("IsDelete", new Document("$ne", true));

                pipeline1 = new ArrayList<Document>();
                pipeline1.add(new Document("$match", findIssuer_));
                pipeline1.add(new Document("$lookup", new Document("from", "Contract")
                        .append("pipeline",
                                Arrays.asList(new Document("$match", new Document("NMUA.TaxCode", TaxCode)
                                        .append("IsDelete", new Document("$ne", true)).append("IsActive", true))))
                        .append("as", "Contract")));

                cursor1 = mongoTemplate.getCollection("Issuer").aggregate(pipeline1);

                iter1 = cursor1.iterator();
                if (iter1.hasNext()) {
                    docTmp1 = iter1.next();
                }

                ObjectId IssuerId = docTmp1.get("_id", ObjectId.class);
                String IssuerIdString = IssuerId.toString();
                List<Document> Contract = null;
                if (docTmp1.get("Contract") != null) {
                    Contract = docTmp1.getList("Contract", Document.class);
                }
                String MST = "";
                String Ten = "";
                String soHopDong = "";
                String SoHD = "";
                // LAY THONG TIN SO HOP DONG
                for (Document doc1 : Contract) {
                    MST = doc1.getEmbedded(Arrays.asList("NMUA", "TaxCode"), "");
                    Ten = doc1.getEmbedded(Arrays.asList("NMUA", "Name"), "");
                    SoHD = doc1.getEmbedded(Arrays.asList("Contract", "SHDon"), "");
                    if (soHopDong.equals("")) {
                        soHopDong = SoHD;
                    } else {
                        soHopDong += "," + SoHD;
                    }
                }

                String FromDateWeek = "";
                String ToDateWeek = "";

                LocalDate dateFrom = null;
                LocalDate dateTo = null;
                Document docMatchDate = null;

                if ("MONTH".equals(type)) {
                    dateFrom = "".equals(fromDate)
                            || !commons.checkLocalDate(fromDate, Constants.FORMAT_DATE.FORMAT_DATE_WEB) ? null
                            : commons.convertStringToLocalDate(fromDate, Constants.FORMAT_DATE.FORMAT_DATE_WEB);
                    dateTo = "".equals(toDate) || !commons.checkLocalDate(toDate, Constants.FORMAT_DATE.FORMAT_DATE_WEB)
                            ? null
                            : commons.convertStringToLocalDate(toDate, Constants.FORMAT_DATE.FORMAT_DATE_WEB);
                    if (null != dateTo)
                        dateTo = dateTo.plus(1, ChronoUnit.DAYS);
                    if (null != dateFrom || null != dateTo) {
                        docMatchDate = new Document();
                        if (null != dateFrom)
                            docMatchDate.append("$gte", dateFrom);
                        if (null != dateTo)
                            docMatchDate.append("$lt", dateTo);
                    }
                } else if ("WEEK".equals(type)) {

                    LocalDate now = LocalDate.now();

                    ToDateWeek = commons.convertLocalDateTimeToString(now.minus(1, ChronoUnit.DAYS), Constants.FORMAT_DATE.FORMAT_DATE_WEB);
                    FromDateWeek = commons.convertLocalDateTimeToString(now.minus(8, ChronoUnit.DAYS), Constants.FORMAT_DATE.FORMAT_DATE_WEB);

                    dateFrom = "".equals(FromDateWeek)
                            || !commons.checkLocalDate(FromDateWeek, Constants.FORMAT_DATE.FORMAT_DATE_WEB) ? null
                            : commons.convertStringToLocalDate(FromDateWeek, Constants.FORMAT_DATE.FORMAT_DATE_WEB);
                    dateTo = "".equals(ToDateWeek) || !commons.checkLocalDate(ToDateWeek, Constants.FORMAT_DATE.FORMAT_DATE_WEB)
                            ? null
                            : commons.convertStringToLocalDate(ToDateWeek, Constants.FORMAT_DATE.FORMAT_DATE_WEB);
                    if (null != dateFrom || null != dateTo) {
                        docMatchDate = new Document();
                        if (null != dateFrom)
                            docMatchDate.append("$gte", dateFrom);
                        if (null != dateTo)
                            docMatchDate.append("$lt", dateTo);
                    }
                }

                if (docMatchDate != null) {
                    docMatch.append("EInvoiceDetail.TTChung.NLap", docMatchDate);
                }
                // LAY THONG TIN MAU SO KY HIEU
                pipeline2 = getPipelines(pipeline, IssuerId, IssuerIdString);

                pipeline2.add(new Document("$lookup",
                        new Document("from", "EInvoice")
                                .append("pipeline",
                                        Arrays.asList(new Document("$match",
                                                new Document("IsDelete", new Document("$ne", true))
                                                        .append("EInvoiceDetail.TTChung.NLap", docMatchDate)
                                                        .append("SignStatusCode", "SIGNED")
                                                        .append("IssuerId", IssuerIdString))))
                                .append("as", "EInvoice")));

                pipeline.add(new Document("$unwind",
                        new Document("path", "$EInvoice").append("preserveNullAndEmptyArrays", true)));

                pipeline2.add(new Document("$lookup",
                        new Document("from", "EInvoiceBH")
                                .append("pipeline",
                                        Arrays.asList(new Document("$match",
                                                new Document("IsDelete", new Document("$ne", true))
                                                        .append("EInvoiceDetail.TTChung.NLap", docMatchDate)
                                                        .append("SignStatusCode", "SIGNED")
                                                        .append("IssuerId", IssuerIdString))))
                                .append("as", "EInvoiceBH")));

                pipeline.add(new Document("$unwind",
                        new Document("path", "$EInvoiceBH").append("preserveNullAndEmptyArrays", true)));

                pipeline2.add(new Document("$lookup",
                        new Document("from", "EInvoicePXK")
                                .append("pipeline",
                                        Arrays.asList(new Document("$match",
                                                new Document("IsDelete", new Document("$ne", true))
                                                        .append("EInvoiceDetail.TTChung.NLap", docMatchDate)
                                                        .append("SignStatusCode", "SIGNED")
                                                        .append("IssuerId", IssuerIdString))))
                                .append("as", "EInvoicePXK")));

                pipeline.add(new Document("$unwind",
                        new Document("path", "$EInvoicePXK").append("preserveNullAndEmptyArrays", true)));

                pipeline2.add(new Document("$lookup",
                        new Document("from", "EInvoicePXKDL")
                                .append("pipeline",
                                        Arrays.asList(new Document("$match",
                                                new Document("IsDelete", new Document("$ne", true))
                                                        .append("EInvoiceDetail.TTChung.NLap", docMatchDate)
                                                        .append("SignStatusCode", "SIGNED")
                                                        .append("IssuerId", IssuerIdString))))
                                .append("as", "EInvoicePXKDL")));

                pipeline.add(new Document("$unwind",
                        new Document("path", "$EInvoicePXKDL").append("preserveNullAndEmptyArrays", true)));

                docTmp2 = getDocTmp(docTmp2, pipeline2);


                int slEinvoice = docTmp2.getList("EInvoice", Document.class).size();
                int slEinvoiceBH = docTmp2.getList("EInvoiceBH", Document.class).size();
                int slEinvoicePXK = docTmp2.getList("EInvoicePXK", Document.class).size();
                int slEinvoicePXKDL = docTmp2.getList("EInvoicePXKDL", Document.class).size();
                String tgSD = "";

                int slSD = slEinvoice + slEinvoiceBH + slEinvoicePXK + slEinvoicePXKDL;

                boolean Status = docTmp2.get("Status", false);
                String trangthai = "";
                if (!Status) {
                    trangthai = "Chưa gia hạn";
                } else {
                    trangthai = "Đã gia hạn";
                }

                if (docTmp2.getList("DMMauSoKyHieu", Document.class).size() == 0) {

                    // DIEU KIEN CHECK SO LUONG
                    setCellData(row, sheet, posRowData, cell, styleInfoR, countRow, styleInfoL, Ten, MST, styleInfoC
                            , SoHD, 100, slSD, type, FromDateWeek, fromDate, ToDateWeek, toDate, "Không Xác Định", trangthai);
                    posRowData++;
                    countRow++;
                }

                List<Document> DMMSKHieu = null;
                if (docTmp2.getList("DMMauSoKyHieu", Document.class).size() > 0) {
                    DMMSKHieu = docTmp2.getList("DMMauSoKyHieu", Document.class);

                    for (Document doc2 : DMMSKHieu) {

                        int slph = doc2.get("SoLuong", 0);
                        int slcl = doc2.get("ConLai", 0);
                        int dadung = slph - slcl;
                        soLuongPH += slph;
                        soLuongConLai += slcl;
                        soLuongDaDung += dadung;

                    }
                }

                if (docTmp2.getList("DMMauSoKyHieu", Document.class).size() > 0) {


                    int soluong_chuaph = 0;
                    soluong_chuaph = soLuongDaCap - soLuongPH;
                    int tile = 0;
                    if (soLuongPH == 0) {
                        tile = 100;
                        soLuongConLai = soluong_chuaph;
                    } else {
                        tile = ((soLuongConLai + soluong_chuaph) * 100) / soLuongDaCap;
                        soLuongConLai += soluong_chuaph;
                    }


                    float check_count = 0;
                    if (slSD == 0) {
                        tgSD = "Không xác định";
                    } else {

                        check_count = (float) soLuongConLai / (float) slSD;
                        tgSD = String.valueOf(String.format("%.2f", check_count));


                    }
                    setCellData(row, sheet, posRowData, cell, styleInfoR, countRow, styleInfoL, Ten, MST, styleInfoC
                            , SoHD, soLuongConLai, slSD, type, FromDateWeek, fromDate, ToDateWeek, toDate, tgSD, trangthai);
                    posRowData++;
                    countRow++;
                }
            }

            out = new ByteArrayOutputStream();
            wb.write(out);
                FileInfo fileInfo = new FileInfo();
            fileInfo.setFileName("DANH-SACH-HOP-DONG-HET-HAN.xlsx");
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

    private void setCellData(SXSSFRow row, SXSSFSheet sheet, int posRowData, SXSSFCell cell, CellStyle styleInfoR, int countRow, CellStyle styleInfoL,
                             String Ten, String MST, CellStyle styleInfoC, String SoHD, int soLuongConLai, int slSD, String type, String FromDateWeek, String fromDate,
                             String ToDateWeek, String toDate, String tgSD, String trangthai) {
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
        cell.setCellValue(SoHD);

        cell = row.getCell(4);
        if (cell == null)
            cell = row.createCell(4);
        cell.setCellStyle(styleInfoL);
        cell.setCellValue(soLuongConLai);

        cell = row.getCell(5);
        if (cell == null)
            cell = row.createCell(5);
        cell.setCellStyle(styleInfoC);
        cell.setCellValue(slSD);

        if (type.equals("WEEK")) {
            cell = row.getCell(6);
            if (cell == null) cell = row.createCell(6);
            cell.setCellStyle(styleInfoL);
            cell.setCellValue(FromDateWeek);
        } else {
            cell = row.getCell(6);
            if (cell == null) cell = row.createCell(6);
            cell.setCellStyle(styleInfoL);
            cell.setCellValue(fromDate);
        }

        if (type.equals("WEEK")) {
            cell = row.getCell(7);
            if (cell == null) cell = row.createCell(7);
            cell.setCellStyle(styleInfoL);
            cell.setCellValue(ToDateWeek);
        } else {
            cell = row.getCell(7);
            if (cell == null) cell = row.createCell(7);
            cell.setCellStyle(styleInfoL);
            cell.setCellValue(toDate);
        }

        cell = row.getCell(8);
        if (cell == null)
            cell = row.createCell(8);
        cell.setCellStyle(styleInfoL);
        cell.setCellValue(tgSD);

        cell = row.getCell(9);
        if (cell == null)
            cell = row.createCell(9);
        cell.setCellStyle(styleInfoL);
        cell.setCellValue(trangthai);

    }

    private Document getDocTmp(Document docTmp2, List<Document> pipeline2) {
        Iterable<Document> cursor2 = null;
        Iterator<Document> iter2 = null;
        cursor2 = mongoTemplate.getCollection("Issuer").aggregate(pipeline2);
        iter2 = cursor2.iterator();
        if (iter2.hasNext()) {
            docTmp2 = iter2.next();
        }
        return docTmp2;
    }

    private List<Document> getPipelines(List<Document> pipeline, ObjectId issuerId, String issuerIdString) {
        List<Document> pipeline2;
        Document findIssuer = new Document("_id", issuerId)
                .append("IsDelete", new Document("$ne", true));
        pipeline2 = new ArrayList<Document>();
        pipeline2.add(new Document("$match", findIssuer));
        pipeline2.add(new Document("$lookup",
                new Document("from", "DMMauSoKyHieu")
                        .append("pipeline",
                                Arrays.asList(new Document("$match",
                                        new Document("IsDelete", new Document("$ne", true))
                                                .append("IsActive", true)
                                                .append("NamPhatHanh", 2023)
                                                .append("IssuerId", issuerIdString))))
                        .append("as", "DMMauSoKyHieu")));

        pipeline.add(new Document("$unwind",
                new Document("path", "$DMMauSoKyHieu").append("preserveNullAndEmptyArrays", true)));
        return pipeline2;
    }

    private void calcWidthHeight(Sheet sheet, CellStyle styleHeader, List<String> headers_week, int i2, int i3) {
        Row row;
        Cell cell;
        row = sheet.getRow(0);
        if (null == row)
            row = sheet.createRow(0);
        row.setHeight((short) 500);
        for (int i = 0; i < headers_week.size(); i++) {
            cell = row.getCell(i);
            if (cell == null)
                cell = row.createCell(i);
            cell.setCellStyle(styleHeader);
            cell.setCellValue(headers_week.get(i));
            if (i == 0)
                sheet.setColumnWidth(i, 2000);
            else if (i == 2)
                sheet.setColumnWidth(i, 4500);
            else if (i == 1)
                sheet.setColumnWidth(i, i2);
            else if (i == 3)
                sheet.setColumnWidth(i, 4000);
            else if (i == 4)
                sheet.setColumnWidth(i, 3500);
            else if (i == 5)
                sheet.setColumnWidth(i, i3);
            else if (i == 6)
                sheet.setColumnWidth(i, 3500);
            else if (i == 7)
                sheet.setColumnWidth(i, 3500);
            else if (i == 8)
                sheet.setColumnWidth(i, 7000);
            else
                sheet.setColumnWidth(i, 5000);
        }
    }

    private void buildHItem(ArrayList<HashMap<String, Object>> rowsReturn, int soLuongDaCap, int soLuongConLai, int soLuongPH, int soLuongDaDung, String MST, String ten, String soHopDong, String _id, String trangthai, int tile) {
        HashMap<String, Object> hItem;
        hItem = new HashMap<>();
        hItem.put("_id", _id);
        hItem.put("TaxCode", MST);
        hItem.put("Name", ten);
        hItem.put("SoHDong", soHopDong);
        hItem.put("SoLuongDaCap", soLuongDaCap);
        hItem.put("SoLuongPH", soLuongPH);
        hItem.put("SoLuongConLai", soLuongConLai);
        hItem.put("SoLuongDaDung", soLuongDaDung);
        hItem.put("TiLe", tile);
        hItem.put("Status", trangthai);
        rowsReturn.add(hItem);
    }

}
