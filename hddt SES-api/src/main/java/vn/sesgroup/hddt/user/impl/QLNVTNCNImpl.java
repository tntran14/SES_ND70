package vn.sesgroup.hddt.user.impl;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.FormulaEvaluator;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
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
import com.mongodb.bulk.BulkWriteResult;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.BulkWriteOptions;
import com.mongodb.client.model.FindOneAndUpdateOptions;
import com.mongodb.client.model.ReturnDocument;
import com.mongodb.client.model.UpdateOneModel;
import com.mongodb.client.model.UpdateOptions;
import com.mongodb.client.model.WriteModel;

import vn.sesgroup.hddt.configuration.ConfigConnectMongo;
import vn.sesgroup.hddt.user.dao.AbstractDAO;
import vn.sesgroup.hddt.user.dao.QLNVTNCNDAO;
import vn.sesgroup.hddt.utility.Constants;
import vn.sesgroup.hddt.utility.Json;
import vn.sesgroup.hddt.utility.SystemParams;

@Repository
@Transactional
public class QLNVTNCNImpl extends AbstractDAO implements QLNVTNCNDAO{
	private static final Logger log = LogManager.getLogger(QLNVTNCNImpl.class);
	@Autowired
	ConfigConnectMongo cfg;
	
	@Override
	public MsgRsp list(JSONRoot jsonRoot) throws Exception {
		Msg msg = jsonRoot.getMsg();
		MsgHeader header = msg.getMsgHeader();
		MsgPage page = msg.getMsgPage();
		Object objData = msg.getObjData();
		
		String taxCode = "";
		String tennv = "";
		
		
		JsonNode jsonData = null;
		if(objData != null) {
			jsonData = Json.serializer().nodeFromObject(objData);
			taxCode = commons.getTextJsonNode(jsonData.at("/TaxCodeNV")).trim().replaceAll("\\s+", " ");
			tennv = commons.getTextJsonNode(jsonData.at("/TenNV"));
		
		}
		
		MsgRsp rsp = new MsgRsp(header);
		MspResponseStatus responseStatus = null;
		
		ObjectId objectId = null;
		Document docTmp = null;
		List<Document> pipeline = new ArrayList<Document>();
		
		Document docMatch = new Document("IssuerId", header.getIssuerId())
				.append("IsDelete", new Document("$ne", true));
		if(!"".equals(taxCode))
			docMatch.append("TaxCode", new Document("$regex", commons.regexEscapeForMongoQuery(taxCode)).append("$options", "i"));
		if(!"".equals(tennv))
			docMatch.append("QLNVTNCNName", new Document("$regex", commons.regexEscapeForMongoQuery(tennv)).append("$options", "i"));
		

		Document fillter = new Document("_id", 1).append("Code", 1).append("Department", 1)
				.append("Email", 1).append("EmailCC", 1)
				.append("TaxCode", 1).append("Name", 1).append("Phone", 1).append("Date", 1).append("CMND-CCCD", 1)
				.append("Address", 1).append("CuTru", 1).append("InfoCreated", 1);
		
		
		pipeline = new ArrayList<Document>();
		pipeline.add(new Document("$match", docMatch));
		pipeline.add(
			new Document("$sort", 
				new Document("Stock", 1).append("Code", 1).append("_id", 1)
			)
		);
		pipeline.add(new Document("$project", fillter));
		pipeline.addAll(createFacetForSearchNotSort(page));
		
		
		
		MongoClient mongoClient = cfg.mongoClient();
		MongoCollection<Document> collection = mongoClient.getDatabase(cfg.dbName).getCollection("TNCNStaff");
		try {
			docTmp =   collection.aggregate(pipeline).allowDiskUse(true).iterator().next();		
		} catch (Exception e) {
			// TODO: handle exception
		}
			
		mongoClient.close();
		
		
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
				hItem.put("Code", doc.get("Code"));
				hItem.put("Department", doc.get("Department"));
				hItem.put("TaxCode", doc.get("TaxCode"));
				hItem.put("Name", doc.get("Name"));
				hItem.put("Phone", doc.get("Phone"));
				hItem.put("Email", doc.get("Email"));
				hItem.put("EmailCC", doc.get("EmailCC"));
				hItem.put("Date", doc.get("Date"));
				hItem.put("CMND-CCCD", doc.get("CMND-CCCD"));
				hItem.put("Address", doc.get("Address"));
				hItem.put("CuTru", doc.get("CuTru"));
				hItem.put("InfoCreated", doc.get("InfoCreated"));
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
	public MsgRsp crud(JSONRoot jsonRoot) throws Exception {
		Msg msg = jsonRoot.getMsg();
		MsgHeader header = msg.getMsgHeader();
		MsgPage page = msg.getMsgPage();
		Object objData = msg.getObjData();
		
		JsonNode jsonData = null;
		if(objData != null) {
			jsonData = Json.serializer().nodeFromObject(msg.getObjData());
		}else{
			throw new Exception("Lỗi dữ liệu đầu vào");
		}
		
		String actionCode = header.getActionCode();
		String _id = commons.getTextJsonNode(jsonData.at("/_id")).replaceAll("\\s", "");
		String taxCode = commons.getTextJsonNode(jsonData.at("/TaxCode")).trim().replaceAll("\\s+", "").replaceAll("[+^%$#@&*]*", "").replaceAll("[a-z][A-Z]*", "");
		String code = commons.getTextJsonNode(jsonData.at("/Code")).trim().replaceAll("\\s+", " ");
		String name = commons.getTextJsonNode(jsonData.at("/Name"));
		String phone = commons.getTextJsonNode(jsonData.at("/Phone")).trim().replaceAll("\\s+", " ");
		String email = commons.getTextJsonNode(jsonData.at("/Email")).trim().replaceAll("\\s+", " ");
		String emailcc = commons.getTextJsonNode(jsonData.at("/EmailCC")).trim().replaceAll("\\s+", " ");
		String address = commons.getTextJsonNode(jsonData.at("/Address")).trim().replaceAll("\\s+", " ");
		String department = commons.getTextJsonNode(jsonData.at("/Department")).trim().replaceAll("\\s+", " ");
		String cccd = commons.getTextJsonNode(jsonData.at("/CCCD")).trim().replaceAll("\\s+", " ");
		String cccddate = commons.getTextJsonNode(jsonData.at("/CCCDDTE")).trim().replaceAll("\\s+", " ");
		String cccdaddress = commons.getTextJsonNode(jsonData.at("/CCCDADDRESS")).trim().replaceAll("\\s+", " ");
		String qt = commons.getTextJsonNode(jsonData.at("/QuocTich")).trim().replaceAll("\\s+", " ");
		String cutru = commons.getTextJsonNode(jsonData.at("/CuTru")).trim().replaceAll("\\s+", " ");
	
		MsgRsp rsp = new MsgRsp(header);
		rsp.setMsgPage(page);
		MspResponseStatus responseStatus = null;
		
		ObjectId objectId = null;
		Document docFind = null;
		Document docTmp = null;
		Document docUpsert = null;
		Document docR = null;
		
		List<Document> pipeline = null;
		FindOneAndUpdateOptions options = null;
		switch (actionCode) {
		case Constants.MSG_ACTION_CODE.CREATED:
			if("".equals(code))
				code = "MCK" + commons.convertLocalDateTimeToString(LocalDate.now(), "yyyyMMdd") + "-" + commons.csRandomAlphaNumbericString(3).toUpperCase();

			objectId = null;
			try {
				objectId = new ObjectId(header.getIssuerId());
			}catch(Exception e) {}
			docFind = new Document("_id", objectId).append("IsActive", true).append("IsDelete", new Document("$ne", true));
			pipeline = new ArrayList<Document>();
			pipeline.add(new Document("$match", docFind));
			pipeline.add(new Document("$project", new Document("_id", 1)));
			
			pipeline.add(
				new Document("$lookup", 
					new Document("from", "TNCNStaff")
					.append("let", new Document("vIssuerId", new Document("$toString", "$_id")))
					.append("pipeline", 
						Arrays.asList(
							new Document("$match", 
								new Document("$expr", 
									new Document("$and", 
										Arrays.asList(
											new Document("$eq", Arrays.asList("$IssuerId", "$$vIssuerId")),
											new Document("$ne", Arrays.asList("$IsDelete", true)),
											new Document("$or", 
												Arrays.asList(
											
													new Document("$eq", Arrays.asList("$Code", commons.regexEscapeForMongoQuery(code)))
												)
											)
										)
									)
									
								)
							),
							new Document("$project", new Document("_id", 1)),
							new Document("$limit", 1)
						)
					)
					.append("as", "TNCNStaff")
				)
			);
			pipeline.add(new Document("$unwind", new Document("path", "$TNCNStaff").append("preserveNullAndEmptyArrays", true)));
			
		
			
			MongoClient mongoClient = cfg.mongoClient();
			MongoCollection<Document> collection = mongoClient.getDatabase(cfg.dbName).getCollection("Issuer");
			try {
				docTmp =   collection.aggregate(pipeline).allowDiskUse(true).iterator().next();		
			} catch (Exception e) {
				// TODO: handle exception
			}
				
			mongoClient.close();
			
			if(null == docTmp) {
				responseStatus = new MspResponseStatus(9999, "Không tìm thấy thông tin nhân viên.");
				rsp.setResponseStatus(responseStatus);
				return rsp;
			}
			if(docTmp.get("TNCNStaff") != null) {
				responseStatus = new MspResponseStatus(9999, "MST hoặc Mã nhân viên đã tồn tại trong hệ thống.");
				rsp.setResponseStatus(responseStatus);
				return rsp;
			}
			
			docUpsert = new Document("IssuerId", header.getIssuerId())
					.append("TaxCode", taxCode)
					.append("Code", code)
					.append("Name", name)
					.append("Phone", phone)
					.append("Email", email)
					.append("EmailCC", emailcc)
					.append("Address", address)
					.append("Department", department)
					.append("CuTru", cutru)				
					.append("CMND-CCCD",
							new Document("CCCD", cccd)
							.append("CCCDDATE", cccddate)
							.append("CCCDADDRESS", cccdaddress)
							.append("QuocTich", qt))
					
					.append("IsActive", true)
					.append("IsDelete", false)
					.append("Date", LocalDateTime.now())
					.append("InfoCreated", 
						new Document("CreateDate", LocalDateTime.now())
						.append("CreateUserID", header.getUserId())
						.append("CreateUserName", header.getUserName())
						.append("CreateUserFullName", header.getUserFullName())
					);
			
		
			mongoClient = cfg.mongoClient();
			collection = mongoClient.getDatabase(cfg.dbName).getCollection("TNCNStaff");
			collection.insertOne(docUpsert);			
			mongoClient.close();
			
			
			HashMap<String, Object> hR = new HashMap<String, Object>();
			hR.put("Code", code);
			
			responseStatus = new MspResponseStatus(0, "SUCCESS");
			rsp.setResponseStatus(responseStatus);
			rsp.setObjData(hR);
			return rsp;
		case Constants.MSG_ACTION_CODE.MODIFY:
			objectId = null;
			try {
				objectId = new ObjectId(_id);
			}catch(Exception e) {}
		
			docFind = new Document("IssuerId", header.getIssuerId())
					.append("IsDelete", new Document("$ne", true))
					.append("_id", objectId);
			pipeline = new ArrayList<Document>();
			pipeline.add(new Document("$match", docFind));
			pipeline.add(new Document("$project", new Document("_id", 1)));
			
			
			 mongoClient = cfg.mongoClient();
			 collection = mongoClient.getDatabase(cfg.dbName).getCollection("TNCNStaff");
			try {
				docTmp =   collection.aggregate(pipeline).allowDiskUse(true).iterator().next();		
			} catch (Exception e) {
				// TODO: handle exception
			}
				
			mongoClient.close();
			
			if(null == docTmp) {
				responseStatus = new MspResponseStatus(9999, "Không tìm thấy thông tin khách hàng.");
				rsp.setResponseStatus(responseStatus);
				return rsp;
			}
			options = new FindOneAndUpdateOptions();
			options.upsert(false);
			options.maxTime(5000, TimeUnit.MILLISECONDS);
			options.returnDocument(ReturnDocument.AFTER);
			
			mongoClient = cfg.mongoClient();
			collection = mongoClient.getDatabase(cfg.dbName).getCollection("TNCNStaff");
			docR =	collection.findOneAndUpdate(
					docFind,
					new Document("$set", 
						new Document("TaxCode", taxCode)
						.append("Code", code)
						.append("Name", name)
						.append("Address", address)
						.append("Department", department)
						.append("CuTru", cutru)	
						.append("CMND-CCCD",
								new Document("CCCD", cccd)
								.append("CCCDDATE", cccddate)
								.append("CCCDADDRESS", cccdaddress)
								.append("QuocTich", qt))
						.append("IsActive", true)
						.append("IsDelete", false)
						.append("InfoUpdated", 
							new Document("UpdatedDate", LocalDateTime.now())
								.append("UpdatedUserID", header.getUserId())
								.append("UpdatedUserName", header.getUserName())
								.append("UpdatedUserFullName", header.getUserFullName())
						)
					),
					options
				);			
			mongoClient.close();
			
			
			responseStatus = new MspResponseStatus(0, "SUCCESS");
			rsp.setResponseStatus(responseStatus);
			rsp.setObjData(docTmp.get("_id"));
			return rsp;
		case Constants.MSG_ACTION_CODE.DELETE:
			List<ObjectId> objectIds = new ArrayList<ObjectId>();
			try {
				if(!jsonData.at("/ids").isMissingNode()) {
					for(JsonNode o: jsonData.at("/ids")) {
						try {
							objectIds.add(new ObjectId(o.asText("")));	
						}catch(Exception e) {}
					}
				}
			}catch(Exception e) {}
			
			UpdateOptions updateOptions = new UpdateOptions();
			updateOptions.upsert(false);
			
			docFind = new Document("IsDelete", new Document("$ne", true))
					.append("_id", new Document("$in", objectIds))
					.append("IssuerId", header.getIssuerId());
			
			
			mongoClient = cfg.mongoClient();
			collection = mongoClient.getDatabase(cfg.dbName).getCollection("TNCNStaff");
			collection.updateMany(
					docFind
					, new Document("$set", 
						new Document("IsDelete", true)
						.append("InfoDeleted", 
							new Document("DeletedDate", LocalDateTime.now())
								.append("DeletedUserID", header.getUserId())
								.append("DeletedUserName", header.getUserName())
								.append("DeletedUserFullName", header.getUserFullName())
							)
					)
					, updateOptions);			
			mongoClient.close();
			
			
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
		}catch(Exception e) {}
		
		Document docFind = new Document("IssuerId", header.getIssuerId())
				.append("IsDelete", new Document("$ne", true)).append("_id", objectId);
		
		Document docTmp = null;		
		MongoClient mongoClient = cfg.mongoClient();
		MongoCollection<Document> collection = mongoClient.getDatabase(cfg.dbName).getCollection("TNCNStaff");
		try {
			docTmp =   collection.find(docFind).allowDiskUse(true).iterator().next();		
		} catch (Exception e) {
			// TODO: handle exception
		}
			
		mongoClient.close();
		
		if(null == docTmp) {
			responseStatus = new MspResponseStatus(9999, Constants.MAP_ERROR.get(9999));
			rsp.setResponseStatus(responseStatus);
			return rsp;
		}
		
		rsp.setObjData(docTmp);
		responseStatus = new MspResponseStatus(0, Constants.MAP_ERROR.get(0));
		rsp.setResponseStatus(responseStatus);
		return rsp;
	}

	/*----------------------------------------------------- Start Import excel */ 
	@Override
	public MsgRsp importExcel(JSONRoot jsonRoot) throws Exception {
		Msg msg = jsonRoot.getMsg();
		MsgHeader header = msg.getMsgHeader();
		MsgPage page = msg.getMsgPage();
		Object objData = msg.getObjData();
		
		MsgRsp rsp = new MsgRsp(header);
		rsp.setMsgPage(page);
		MspResponseStatus responseStatus = null;
		
		JsonNode jsonData = null;
		if(objData != null) {
			jsonData = Json.serializer().nodeFromObject(msg.getObjData());
		}else{
			throw new Exception("Lỗi dữ liệu đầu vào");
		}
		
		String dataFileName = commons.getTextJsonNode(jsonData.at("/DataFileName")).replaceAll("\\s", "");
		Path path = Paths.get(SystemParams.DIR_TEMPORARY, header.getIssuerId(), dataFileName);
		File file = path.toFile();
		if(!(file.exists() && file.isFile())) {
			responseStatus = new MspResponseStatus(9999, "Tập tin import dữ liệu không tồn tại.");
			rsp.setResponseStatus(responseStatus);
			return rsp;
		}
		
		Workbook wb = null;
		Sheet sheet = null;
		Row row = null;
		Cell cell = null;
		try {
			wb = WorkbookFactory.create(file);
			sheet = wb.getSheetAt(0);
		}catch(Exception e) {}
		
		if(sheet == null) {
			responseStatus = new MspResponseStatus(9999, "Không tìm thấy dữ liệu import trong file dữ liệu.");
			rsp.setResponseStatus(responseStatus);
			return rsp;
		}
		
		FormulaEvaluator formulaEvaluator = wb.getCreationHelper().createFormulaEvaluator();
		
		StringBuilder sb = new StringBuilder();
		List<HashMap<String, String>> datas = new ArrayList<HashMap<String,String>>();
		HashMap<String, String> hItem = null;
		
		/*DOC FILE EXCEL - GHI DU LIEU VO LIST*/
		Iterator<Row> rows = sheet.rowIterator();
		while (rows.hasNext()) {
			row = rows.next();
			if(row.getRowNum() < 1) continue;
			
			sb.setLength(0);
			sb.append(commons.getCellValue(formulaEvaluator, row.getCell(0)));
			if("".equals(sb.toString())) continue;
			
			try {
				hItem = new HashMap<String, String>();
				hItem.put("Mst", commons.getCellValue(formulaEvaluator, row.getCell(0)));
				hItem.put("NameNV", commons.getCellValue(formulaEvaluator, row.getCell(1)));
				hItem.put("MaNV", commons.getCellValue(formulaEvaluator, row.getCell(2)));
				hItem.put("Adress", commons.getCellValue(formulaEvaluator, row.getCell(3)));
				hItem.put("Phone", commons.getCellValue(formulaEvaluator, row.getCell(4)));
				hItem.put("CCCD", commons.getCellValue(formulaEvaluator, row.getCell(5)));
				hItem.put("DateCCCD", commons.getCellValue(formulaEvaluator, row.getCell(6)));	
				hItem.put("NoiCap", commons.getCellValue(formulaEvaluator, row.getCell(7)));	
				hItem.put("QuocTich", commons.getCellValue(formulaEvaluator, row.getCell(8)));	
				hItem.put("CaNhanCuTru", commons.getCellValue(formulaEvaluator, row.getCell(9)));	
				hItem.put("Department", commons.getCellValue(formulaEvaluator, row.getCell(10)));
				datas.add(hItem);
			}catch(Exception e) {
			}
		}
		
		if(datas.size() == 0) {
			responseStatus = new MspResponseStatus(9999, "Không tồn tại dữ liệu cần import");
			rsp.setResponseStatus(responseStatus);
			return rsp;
		}		
		Document docFilter = null;

		String 	customerCode = "MCK" + commons.convertLocalDateTimeToString(LocalDate.now(), "yyyyMMdd") + "-" + commons.csRandomAlphaNumbericString(3).toUpperCase();
		List<WriteModel<Document>> ous = new ArrayList<WriteModel<Document>>();
		BulkWriteResult r = null;
		UpdateOptions uo = new UpdateOptions();
		uo.upsert(true);
	
		for(HashMap<String, String> hO: datas) {
			Document docTmpSub = new Document("CCCD",  hO.get("CCCD"))
					.append("CCCDDATE",  hO.get("DateCCCD"))
					.append("CCCDADDRESS",  hO.get("NoiCap"))
			.append("QuocTich", hO.get("QuocTich"));
			docFilter = new Document("IssuerId", header.getIssuerId()).append("TaxCode", hO.get("Mst"));
			ous.add(
				new UpdateOneModel<>(
					docFilter, 
					new Document("$set", 
							new Document("IssuerId", header.getIssuerId())
						.append("TaxCode", hO.get("Mst"))
						.append("Name", hO.get("NameNV"))
						.append("Code", hO.get("MaNV"))
						.append("Code",customerCode)
						.append("Address", hO.get("Adress"))
						.append("Phone", hO.get("Phone"))
						.append("CuTru", hO.get("CaNhanCuTru"))
						.append("CMND-CCCD", docTmpSub)
						.append("Department", hO.get("Department"))
						.append("IsDelete", false)
					).append("$setOnInsert", 
						new Document("InfoCreated", 
								new Document("CreateDate", LocalDateTime.now())
								.append("CreateUserID", header.getUserId())
								.append("CreateUserName", header.getUserName())
								.append("CreateUserFullName", header.getUserFullName())
							)
					)
					,
					uo)
			);
		}

		MongoClient mongoClient = cfg.mongoClient();
		MongoCollection<Document> collection = mongoClient.getDatabase(cfg.dbName).getCollection("TNCNStaff");
		try {
			r =   collection.bulkWrite(
					ous,
					new BulkWriteOptions().ordered(false)
				);	
		} catch (Exception e) {
			// TODO: handle exception
		}
			
		mongoClient.close();
	
		responseStatus = new MspResponseStatus(0, "SUCCESS");
		rsp.setResponseStatus(responseStatus);
		return rsp;
	}

	/*----------------------------------------------------- End Import excel */ 


}
