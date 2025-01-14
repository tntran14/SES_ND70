package vn.sesgroup.hddt.user.impl;

import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

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
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
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
import vn.sesgroup.hddt.user.dao.KHXuatHDonAdminDAO;
import vn.sesgroup.hddt.user.service.TCTNService;
import vn.sesgroup.hddt.utility.Constants;
import vn.sesgroup.hddt.utility.Json;

@Repository
@Transactional
public class KHXuatHDonAdminImpl extends AbstractDAO implements KHXuatHDonAdminDAO {
	private static final Logger log = LogManager.getLogger(KHXuatHDonAdminImpl.class);
	@Autowired ConfigConnectMongo cfg;
	@Autowired TCTNService tctnService;

	public MsgRsp list(JSONRoot jsonRoot) throws Exception {
		Msg msg = jsonRoot.getMsg();
		MsgHeader header = msg.getMsgHeader();
		MsgPage page = msg.getMsgPage();
		Object objData = msg.getObjData();

		ObjectId objectId = null;
		Document docTmp = null;
		List<Document> pipeline = new ArrayList<Document>();

		String mst = "";
		String fromDate = "";
		String toDate = "";
		String name = "";

		JsonNode jsonData = null;
		if (objData != null) {
			jsonData = Json.serializer().nodeFromObject(objData);
			mst = commons.getTextJsonNode(jsonData.at("/MST")).replaceAll("\\s", "");
			fromDate = commons.getTextJsonNode(jsonData.at("/FromDate")).replaceAll("\\s", "");
			toDate = commons.getTextJsonNode(jsonData.at("/ToDate")).replaceAll("\\s", "");
			name = commons.getTextJsonNode(jsonData.at("/Name")).replaceAll("\\s+", " ");
		}

		MsgRsp rsp = new MsgRsp(header);
		MspResponseStatus responseStatus = null;

		LocalDate dateFrom = null;
		LocalDate dateTo = null;
		Document docMatchDate = null;

		dateFrom = "".equals(fromDate) || !commons.checkLocalDate(fromDate, Constants.FORMAT_DATE.FORMAT_DATE_WEB)
				? null
				: commons.convertStringToLocalDate(fromDate, Constants.FORMAT_DATE.FORMAT_DATE_WEB);
		dateTo = "".equals(toDate) || !commons.checkLocalDate(toDate, Constants.FORMAT_DATE.FORMAT_DATE_WEB) ? null
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

		Document docMatch = new Document("IsDelete", new Document("$ne", true));
		if (!mst.equals(""))
			docMatch.append("EInvoiceDetail.NDHDon.NBan.MST", mst);

		if (!name.equals(""))
			docMatch.append("EInvoiceDetail.NDHDon.NBan.Ten",
					new Document("$regex", commons.regexEscapeForMongoQuery(name)).append("$options", "i"));
		if (null != docMatchDate)
			docMatch.append("EInvoiceDetail.TTChung.NLap", docMatchDate);

		pipeline = new ArrayList<Document>();
		pipeline.add(new Document("$match", new Document("ActiveFlag", true)));
		pipeline.add(new Document("$lookup", new Document("from", "EInvoice").append("pipeline",
				Arrays.asList(new Document("$match", docMatch), new Document("$group", new Document("_id", "$IssuerId").

						append("IssuerId", new Document("$first", "$IssuerId"))

				)

				)).append("as", "EInvoice")));
		
		MongoClient mongoClient = cfg.mongoClient();
		MongoCollection<Document> collection = mongoClient.getDatabase(cfg.dbName).getCollection("ApiLicenseKey");
		      try {
		        docTmp = collection.aggregate(pipeline).allowDiskUse(true).iterator().next();    
		      } catch (Exception e) {
		        
		      }
		        
		mongoClient.close();

		if (docTmp.get("EInvoice") == null) {
			responseStatus = new MspResponseStatus(9999, Constants.MAP_ERROR.get(9999));
			rsp.setResponseStatus(responseStatus);
			return rsp;
		}

		rsp = new MsgRsp(header);
		responseStatus = null;
		List<Document> data = null;
		if (docTmp.get("EInvoice") != null) {
			data = docTmp.getList("EInvoice", Document.class);
		}

		List<Document> rows = null;
		if (data != null && docTmp.get("EInvoice") instanceof List) {
			rows = docTmp.getList("EInvoice", Document.class);
		}

		ArrayList<HashMap<String, Object>> rowsReturn = new ArrayList<HashMap<String, Object>>();
		HashMap<String, Object> hItem = null;
		if (null != rows) {
			for (Document doc : rows) {
				Document docTmp1 = null;
				String issuerid = doc.get("IssuerId").toString();
				ObjectId id_issuer = new ObjectId(issuerid);

				// Find ISSUERID
				Document findIssuer = new Document("_id", id_issuer).append("IsDelete", false);
				
				mongoClient = cfg.mongoClient();
				collection = mongoClient.getDatabase(cfg.dbName).getCollection("Issuer");
				  try {
					  docTmp1 = collection.find(findIssuer).allowDiskUse(true).iterator().next();      
				     } catch (Exception e) {
				        
				    }
				mongoClient.close();
				
				
				try {
					objectId = docTmp1.getObjectId("_id");
					String TaxCode = docTmp1.get("TaxCode", "");
					String Ten = docTmp1.get("Name", "");
					String Address = docTmp1.get("Address", "");
					String Email = docTmp1.get("Email", "");

					hItem = new HashMap<String, Object>();
					hItem.put("_id", objectId.toString());
					hItem.put("MST", TaxCode);
					hItem.put("Name", Ten);
					hItem.put("Address", Address);
					hItem.put("Email", Email);

					rowsReturn.add(hItem);

				} catch (Exception e) {
					continue;
				}
			}
		}

		if (rowsReturn.size() == 0) {
			responseStatus = new MspResponseStatus(9999, Constants.MAP_ERROR.get(9999));
			rsp.setResponseStatus(responseStatus);
			return rsp;
		}

		ArrayList<HashMap<String, Object>> rowsReturn1 = new ArrayList<HashMap<String, Object>>();
		page.setTotalRows(rowsReturn.size());
		int pageNo = commons.calcPageNo(page.getPageNo());
		int size = commons.calcPageSize(page.getSize());
		int TotalRows = commons.calcPageSize(rowsReturn.size());

		int sotrang = size * (pageNo - 1);
		int dem = 0;
		for (int i = sotrang; i <= TotalRows; i++) {
			sotrang++;
			dem++;
			rowsReturn1.add(rowsReturn.get(i));

			if (dem == size || dem == TotalRows || sotrang == TotalRows) {
				break;
			}
		}
		rsp.setMsgPage(page);
		rsp.getMsgHeader().setAdmin(true);

		responseStatus = new MspResponseStatus(0, "SUCCESS");
		rsp.setResponseStatus(responseStatus);
		HashMap<String, Object> mapDataR = new HashMap<String, Object>();
		mapDataR.put("rows", rowsReturn1);
		rsp.setObjData(mapDataR);
		return rsp;
	}

	public FileInfo exportExcel(JSONRoot jsonRoot) throws Exception {
		Msg msg = jsonRoot.getMsg();
		MsgHeader header = msg.getMsgHeader();
		MsgPage page = msg.getMsgPage();
		Object objData = msg.getObjData();

		String mst = "";
		String fromDate = "";
		String toDate = "";
		String name = "";

		JsonNode jsonData = null;
		if (objData != null) {
			jsonData = Json.serializer().nodeFromObject(objData);
			mst = commons.getTextJsonNode(jsonData.at("/MST")).replaceAll("\\s", "");
			name = commons.getTextJsonNode(jsonData.at("/Name")).replaceAll("\\s+", " ");
			fromDate = commons.getTextJsonNode(jsonData.at("/FromDate")).replaceAll("\\s", "");
			toDate = commons.getTextJsonNode(jsonData.at("/ToDate")).replaceAll("\\s", "");
		}

		List<Document> pipeline = new ArrayList<Document>();
		Document docTmp = null;
		Document docTmp1 = null;
		ByteArrayOutputStream out = null;
		SXSSFWorkbook wb = new SXSSFWorkbook(1000);
		Sheet sheet = null;
		try {
			sheet = wb.createSheet("Sheet 1");
			Row row = null;
			Cell cell = null;

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
			styleInfoR.setAlignment(HorizontalAlignment.CENTER);
			styleInfoR.setVerticalAlignment(VerticalAlignment.CENTER);
			styleInfoR.setBorderBottom(BorderStyle.THIN);
			styleInfoR.setBorderTop(BorderStyle.THIN);
			styleInfoR.setBorderRight(BorderStyle.THIN);
			styleInfoR.setBorderLeft(BorderStyle.THIN);
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
			List<String> headers = Arrays
					.asList(new String[] { "STT", "Mã số thuế", "Tên đơn vị", "Địa chỉ", "Email" });
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
					sheet.setColumnWidth(i, 5500);
				else if (i == 2)
					sheet.setColumnWidth(i, 10000);
				else if (i == 3)
					sheet.setColumnWidth(i, 10000);
				else if (i == 4)
					sheet.setColumnWidth(i, 7500);
				else
					sheet.setColumnWidth(i, 3000);
			}

			int posRowData = 1;

			LocalDate dateFrom = null;
			LocalDate dateTo = null;
			Document docMatchDate = null;

			dateFrom = "".equals(fromDate) || !commons.checkLocalDate(fromDate, Constants.FORMAT_DATE.FORMAT_DATE_WEB)
					? null
					: commons.convertStringToLocalDate(fromDate, Constants.FORMAT_DATE.FORMAT_DATE_WEB);
			dateTo = "".equals(toDate) || !commons.checkLocalDate(toDate, Constants.FORMAT_DATE.FORMAT_DATE_WEB) ? null
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

			Document docMatch = new Document("IsDelete", new Document("$ne", true));
			if (!mst.equals(""))
				docMatch.append("EInvoiceDetail.NDHDon.NBan.MST", mst);

			if (!name.equals(""))
				docMatch.append("EInvoiceDetail.NDHDon.NBan.Ten",
						new Document("$regex", commons.regexEscapeForMongoQuery(name)).append("$options", "i"));
			if (null != docMatchDate)
				docMatch.append("EInvoiceDetail.TTChung.NLap", docMatchDate);

			pipeline = new ArrayList<Document>();
			pipeline.add(new Document("$match", new Document("ActiveFlag", true)));
			pipeline.add(new Document("$lookup", new Document("from", "EInvoice").append("pipeline", Arrays
					.asList(new Document("$match", docMatch), new Document("$group", new Document("_id", "$IssuerId").

							append("IssuerId", new Document("$first", "$IssuerId"))

					)

					)).append("as", "EInvoice")));

			
			MongoClient mongoClient = cfg.mongoClient();
			MongoCollection<Document> collection = mongoClient.getDatabase(cfg.dbName).getCollection("ApiLicenseKey");
			      try {
			        docTmp = collection.aggregate(pipeline).allowDiskUse(true).iterator().next();    
			      } catch (Exception e) {
			        
			      }
			        
			mongoClient.close();

			if (docTmp.get("EInvoice") == null) {
				row = sheet.getRow(posRowData);
				if (null == row)
					row = sheet.createRow(posRowData);

				cell = row.getCell(0);
				if (cell == null)
					cell = row.createCell(0);
				cell.setCellStyle(styleInfoR);

				cell = row.getCell(1);
				if (cell == null)
					cell = row.createCell(1);
				cell.setCellStyle(styleInfoL);

				cell = row.getCell(2);
				if (cell == null)
					cell = row.createCell(2);
				cell.setCellStyle(styleInfoL);

				cell = row.getCell(3);
				if (cell == null)
					cell = row.createCell(3);
				cell.setCellStyle(styleInfoC);

				cell = row.getCell(4);
				if (cell == null)
					cell = row.createCell(4);
				cell.setCellStyle(styleInfoC);

				out = new ByteArrayOutputStream();
				wb.write(out);

				FileInfo fileInfo = new FileInfo();
				fileInfo.setFileName("DANH-SACH-KHACH-HANG-XUAT-HD.xlsx");
				fileInfo.setContentFile(out.toByteArray());
				return fileInfo;

			}

			List<Document> data = null;
			if (docTmp.get("EInvoice") != null) {
				data = docTmp.getList("EInvoice", Document.class);
			}

			List<Document> rows = null;
			if (data != null && docTmp.get("EInvoice") instanceof List) {
				rows = docTmp.getList("EInvoice", Document.class);
			}

			if (null != rows) {
				for (Document doc : rows) {
					docTmp1 = null;
					String issuerid = doc.get("IssuerId").toString();
					ObjectId id_issuer = new ObjectId(issuerid);

					// Find ISSUERID
					Document findIssuer = new Document("_id", id_issuer).append("IsDelete", false);				
					mongoClient = cfg.mongoClient();
					collection = mongoClient.getDatabase(cfg.dbName).getCollection("Issuer");
					      try {
					    	  docTmp1 = collection.find(findIssuer).allowDiskUse(true).iterator().next();    
					      } catch (Exception e) {
					        
					      }
					        
					mongoClient.close();
					
					try {
						String TaxCode = docTmp1.get("TaxCode", "");
						String Ten = docTmp1.get("Name", "");
						String Address = docTmp1.get("Address", "");
						String Email = docTmp1.get("Email", "");

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
						cell.setCellValue(TaxCode);

						cell = row.getCell(2);
						if (cell == null)
							cell = row.createCell(2);
						cell.setCellStyle(styleInfoL);
						cell.setCellValue(Ten);

						cell = row.getCell(3);
						if (cell == null)
							cell = row.createCell(3);
						cell.setCellStyle(styleInfoC);
						cell.setCellValue(Address);

						cell = row.getCell(4);
						if (cell == null)
							cell = row.createCell(4);
						cell.setCellStyle(styleInfoC);
						cell.setCellValue(Email);

						posRowData++;
						countRow++;

					} catch (Exception e) {
						continue;
					}
				}
			}

			out = new ByteArrayOutputStream();
			wb.write(out);

			FileInfo fileInfo = new FileInfo();
			fileInfo.setFileName("DANH-SACH-KHACH-HANG-XUAT-HD.xlsx");
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

}
