	package vn.sesgroup.hddt.user.impl;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

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
import com.mongodb.bulk.BulkWriteResult;
import com.mongodb.client.model.BulkWriteOptions;
import com.mongodb.client.model.FindOneAndUpdateOptions;
import com.mongodb.client.model.ReturnDocument;
import com.mongodb.client.model.UpdateOneModel;
import com.mongodb.client.model.UpdateOptions;
import com.mongodb.client.model.WriteModel;

import vn.sesgroup.hddt.dto.FileInfo;
import vn.sesgroup.hddt.user.dao.AbstractDAO;
import vn.sesgroup.hddt.user.dao.DMProductDAO;
import vn.sesgroup.hddt.utility.Constants;
import vn.sesgroup.hddt.utility.Json;
import vn.sesgroup.hddt.utility.SystemParams;

@Repository
@Transactional
public class DMProductImpl extends AbstractDAO implements DMProductDAO{
	private static final Logger log = LogManager.getLogger(DMProductImpl.class);
	@Autowired MongoTemplate mongoTemplate;
	
/*
db.getCollection('DMProduct').aggregate([
    {$match: {
        IssuerId: '61b851ebb0228bba71fca2ec', IsDelete: {$ne: true},
        Code: {$regex: '', $options: 'i'},
        Name: {$regex: '', $options: 'i'},
        Stock: 'DT1'
        }
    },
    {$sort: {'Stock': 1, Code: 1, _id: -1}}
     , {$facet: {
            meta: [{$count: 'total'}],
            data: [{$skip: 0}, {$limit: 10}]
        }
    }
    , {$unwind: '$meta'}
    , {$project:{'total':'$meta.total',data:1}}
])
 * */
	
	@Override
	public MsgRsp list(JSONRoot jsonRoot) throws Exception {
		Msg msg = jsonRoot.getMsg();
		MsgHeader header = msg.getMsgHeader();
		MsgPage page = msg.getMsgPage();
		Object objData = msg.getObjData();
		
		String code = "";
		String name = "";
		String stock = "";
		
		JsonNode jsonData = null;
		if(objData != null) {
			jsonData = Json.serializer().nodeFromObject(objData);
			code = commons.getTextJsonNode(jsonData.at("/Code")).trim().replaceAll("\\s+", " ");
			name = commons.getTextJsonNode(jsonData.at("/Name")).trim().replaceAll("\\s+", " ");
			stock = commons.getTextJsonNode(jsonData.at("/Stock")).trim().replaceAll("\\s+", " ").toUpperCase();
		}
		
		MsgRsp rsp = new MsgRsp(header);
		MspResponseStatus responseStatus = null;
		
		ObjectId objectId = null;
		Document docTmp = null;
		Iterable<Document> cursor = null;
		Iterator<Document> iter = null;
		List<Document> pipeline = new ArrayList<Document>();
		
		Document docMatch = new Document("IssuerId", header.getIssuerId())
				.append("IsDelete", new Document("$ne", true));
		if(!"".equals(code)) {
			docMatch.append("Code", new Document("$regex", commons.regexEscapeForMongoQuery(code)).append("$options", "i"));
		}
		if(!"".equals(name)) {
			docMatch.append("Name", new Document("$regex", commons.regexEscapeForMongoQuery(name)).append("$options", "i"));
		}
		if(!"".equals(stock)) {
			docMatch.append("Stock", commons.regexEscapeForMongoQuery(stock));
		}
		
		pipeline = new ArrayList<Document>();
		pipeline.add(new Document("$match", docMatch));
		pipeline.add(
			new Document("$sort", 
				new Document("Stock", 1).append("Code", 1).append("_id", -1)
			)
		);
		pipeline.addAll(createFacetForSearchNotSort(page));
		
		cursor = mongoTemplate.getCollection("DMProduct").aggregate(pipeline).allowDiskUse(true);
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
				hItem.put("Code", doc.get("Code"));
				hItem.put("Stock", doc.get("Stock"));
				hItem.put("Slsx", doc.get("Slsx"));
				hItem.put("Name", doc.get("Name"));
				hItem.put("Price", doc.get("Price"));
				hItem.put("Unit", doc.get("Unit"));
				hItem.put("VatRate", doc.get("VatRate"));
				
				hItem.put("InfoCreated", doc.get("InfoCreated"));
				hItem.put("InfoUpdated", doc.get("InfoUpdated"));
				//han su dung
				hItem.put("thdoi_tonkho", doc.get("thdoi_tonkho"));
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
		String code = commons.getTextJsonNode(jsonData.at("/code")).replaceAll("\\s", "");
		String name = commons.getTextJsonNode(jsonData.at("/name")).trim().replaceAll("\\s+", " ");
		String stock = commons.getTextJsonNode(jsonData.at("/stock")).trim().replaceAll("\\s+", " ");
		String unit = commons.getTextJsonNode(jsonData.at("/unit")).trim().replaceAll("\\s+", " ");
		String price = commons.getTextJsonNode(jsonData.at("/price")).trim().replaceAll("\\s+", " ");
		String vatRate = commons.getTextJsonNode(jsonData.at("/vatRate")).trim().replaceAll("\\s+", " ");
		String description = commons.getTextJsonNode(jsonData.at("/description")).trim().replaceAll("\\s+", " ");
		String thdoiTonkho = commons.getTextJsonNode(jsonData.at("/thdoiTonkho")).trim().replaceAll("\\s+", " ");
		String remark = commons.getTextJsonNode(jsonData.at("/remark")).trim().replaceAll("\\s+", " ");
		String slsx = commons.getTextJsonNode(jsonData.at("/slsx")).trim().replaceAll("\\s+", " ");
		String tkvt = commons.getTextJsonNode(jsonData.at("/tkvt")).trim().replaceAll("\\s+", " ");
		String tkgv = commons.getTextJsonNode(jsonData.at("/tkgv")).trim().replaceAll("\\s+", " ");
		String tkdt = commons.getTextJsonNode(jsonData.at("/tkdt")).trim().replaceAll("\\s+", " ");
		String loaivt = commons.getTextJsonNode(jsonData.at("/loaivt")).trim().replaceAll("\\s+", " ");
		String nh_vt1 = commons.getTextJsonNode(jsonData.at("/nh_vt1")).trim().replaceAll("\\s+", " ");
		String nh_vt2 = commons.getTextJsonNode(jsonData.at("/nh_vt2")).trim().replaceAll("\\s+", " ");
		String nh_vt3 = commons.getTextJsonNode(jsonData.at("/nh_vt3")).trim().replaceAll("\\s+", " ");
		String sua_tk_tonkho = commons.getTextJsonNode(jsonData.at("/sua_tk_tonkho")).trim().replaceAll("\\s+", " ");
		String cach_tinh_gia_ton = commons.getTextJsonNode(jsonData.at("/cach_tinh_gia_ton")).trim().replaceAll("\\s+", " ");
		String tk_cl_vt = commons.getTextJsonNode(jsonData.at("/tk_cl_vt")).trim().replaceAll("\\s+", " ");
		String tk_dtnb = commons.getTextJsonNode(jsonData.at("/tk_dtnb")).trim().replaceAll("\\s+", " ");
	
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
		
		Iterable<Document> cursor = null;
		Iterator<Document> iter = null;
		
		switch (actionCode) {
		case Constants.MSG_ACTION_CODE.CREATED:
/* KIEM TRA SP - KHO CO TON TAI KHONG
db.getCollection('Issuer').aggregate([
    {$match: {
            _id: ObjectId("61b851ebb0228bba71fca2ec"), IsActive: true, IsDelete: {$ne: true}
        }
    },
    {$lookup: {
            from: 'DMProduct',
            let: {vIssuerId: {$toString: '$_id'}},
            pipeline: [
                {$match: {
                        $expr: {
                            $and: [
                                {$ne: ['$IsDelete', true]},
                                {$eq: ['$IssuerId', '$$vIssuerId']},
                                {$eq: ['$Code', 'DP36772_3L1']},
                                {$eq: ['$Stock', 'KTP01']},
                            ]
                        }
                    }
                },
                {$count: 'pcount'}
            ],
            as: 'DMProduct'
        }
    },
    {$unwind: {path: '$DMProduct', preserveNullAndEmptyArrays: true}},
    {$lookup: {
            from: 'DMStock',
            let: {vIssuerId: {$toString: '$_id'}},
            pipeline: [
                {$match: {
                        $expr: {
                            $and: [
                                {$ne: ['$IsDelete', true]},
                                {$eq: ['$IssuerId', '$$vIssuerId']},
                                {$eq: ['$Code', 'KTP01']}
                            ]
                        }
                    }
                },
                {$project: {_id: 0, Code: 1, Name: 1}}
            ],
            as: 'DMStock'
        }
    },
    {$unwind: {path: '$DMStock', preserveNullAndEmptyArrays: true}},
]);
 * */
			objectId = null;
			try {
				objectId = new ObjectId(header.getIssuerId());
			}catch(Exception e) {}
			
			docFind = new Document("_id", objectId).append("IsActive", true).append("IsDelete", new Document("$ne", true));
			pipeline = new ArrayList<Document>();
			pipeline.add(new Document("$match", docFind));
			pipeline.add(
				new Document("$lookup", 
					new Document("from", "DMProduct")
					.append("let", new Document("vIssuerId", new Document("$toString", "$_id")))
					.append("pipeline", 
						Arrays.asList(
							new Document("$match", 
								new Document("$expr", 
									new Document("$and", 
										Arrays.asList(
											new Document("$ne", Arrays.asList("$IsDelete", true)),
											new Document("$eq", Arrays.asList("$IssuerId", "$$vIssuerId")),
											new Document("$eq", Arrays.asList("$Code", code)),
											new Document("$eq", Arrays.asList("$Stock", stock))
										)
									)
								)
							),
							new Document("$count", "pcount")
						)
					)
					.append("as", "DMProduct")
				)
			);
			pipeline.add(new Document("$unwind", new Document("path", "$DMProduct").append("preserveNullAndEmptyArrays", true)));
			pipeline.add(
				new Document("$lookup", 
					new Document("from", "DMStock")
					.append("let", new Document("vIssuerId", new Document("$toString", "$_id")))
					.append("pipeline", 
						Arrays.asList(
							new Document("$match", 
								new Document("$expr", 
									new Document("$and", 
										Arrays.asList(
											new Document("$ne", Arrays.asList("$IsDelete", true)),
											new Document("$eq", Arrays.asList("$IssuerId", "$$vIssuerId")),
											new Document("$eq", Arrays.asList("$Code", stock))
										)
									)
								)
							),
							new Document("$project", new Document("_id", 1).append("Code", 1).append("Name", 1))
						)
					)
					.append("as", "DMStock")
				)
			);
			pipeline.add(new Document("$unwind", new Document("path", "$DMStock").append("preserveNullAndEmptyArrays", true)));
			
			cursor = mongoTemplate.getCollection("Issuer").aggregate(pipeline);
			iter = cursor.iterator();
			if(iter.hasNext()) {
				docTmp = iter.next();
			}
			
			if(null == docTmp) {
				responseStatus = new MspResponseStatus(9999, "Không tìm thấy thông tin khách hàng.");
				rsp.setResponseStatus(responseStatus);
				return rsp;
			}
			if(docTmp.get("DMProduct") != null) {
				responseStatus = new MspResponseStatus(9999, "Sản phẩm đã tồn tại.");
				rsp.setResponseStatus(responseStatus);
				return rsp;
			}
//			if(docTmp.get("DMStock") == null) {
//				responseStatus = new MspResponseStatus(9999, "Không tìm thấy thông tin kho hàng.");
//				rsp.setResponseStatus(responseStatus);
//				return rsp;
//			}
			
			docUpsert = new Document("IssuerId", header.getIssuerId())
					.append("Code", code)
					.append("Name", name)
					.append("Stock", stock)
					.append("Slsx", slsx)
					.append("Unit", unit)
					.append("Price", commons.ToNumber(price))
					.append("VatRate", commons.ToNumber(vatRate))
					.append("Description", description)
					.append("thdoi_tonkho",thdoiTonkho)
					.append("Remark", remark)
					.append("tkvt", tkvt)
					.append("tkgv", tkgv)
					.append("tkdt", tkdt)
					.append("loaivt", loaivt)
					.append("nh_vt1", nh_vt1)
					.append("nh_vt2", nh_vt2)
					.append("nh_vt3", nh_vt3)
					.append("sua_tk_tonkho", "Y".equals(sua_tk_tonkho))
					.append("cach_tinh_gia_ton", cach_tinh_gia_ton)
					.append("tk_cl_vt", tk_cl_vt)
					.append("tk_dtnb", tk_dtnb)
					.append("InfoCreated", 
						new Document("CreateDate", LocalDateTime.now())
						.append("CreateUserID", header.getUserId())
						.append("CreateUserName", header.getUserName())
						.append("CreateUserFullName", header.getUserFullName())
					);
					
			mongoTemplate.getCollection("DMProduct").insertOne(docUpsert);
			
			responseStatus = new MspResponseStatus(0, "SUCCESS");
			rsp.setResponseStatus(responseStatus);				
			return rsp;
		case Constants.MSG_ACTION_CODE.MODIFY:
/* KIEM TRA SP - KHO CO TON TAI KHONG
ddb.getCollection('DMProduct').aggregate([
    {$match: {
           'IssuerId': '61b851ebb0228bba71fca2ec', IsDelete: {$ne: true}, _id: ObjectId("61d166fde69f8a2eb2b01e43")
        }
    },
    {$lookup: {
            from: 'DMStock',
            let: {vIssuerId: '$IssuerId'},
            pipeline: [
                {$match: {
                        $expr: {
                            $and: [
                                {$ne: ['$IsDelete', true]},
                                {$eq: ['$IssuerId', '$$vIssuerId']},
                                {$eq: ['$Code', 'KTP01']}
                            ]
                        }
                    }
                },
                {$project: {_id: 0, Code: 1, Name: 1}}
            ],
            as: 'DMStock'
        }
    },
    {$unwind: {path: '$DMStock', preserveNullAndEmptyArrays: true}},
]);
 * */
			objectId = null;
			try {
				objectId = new ObjectId(_id);
			}catch(Exception e) {}
			docFind = new Document("IssuerId", header.getIssuerId())
					.append("IsDelete", new Document("$ne", true))
					.append("_id", objectId);
			pipeline = new ArrayList<Document>();
			pipeline.add(new Document("$match", docFind));
			pipeline.add(
				new Document("$lookup", 
					new Document("from", "DMStock")
					.append("let", new Document("vIssuerId", "$IssuerId"))
					.append("pipeline", 
						Arrays.asList(
							new Document("$match", 
								new Document("$expr", 
									new Document("$and", 
										Arrays.asList(
											new Document("$ne", Arrays.asList("$IsDelete", true)),
											new Document("$eq", Arrays.asList("$IssuerId", "$$vIssuerId")),
											new Document("$eq", Arrays.asList("$Code", stock))
										)
									)
								)
							),
							new Document("$project", new Document("_id", 1).append("Code", 1).append("Name", 1))
						)
					)
					.append("as", "DMStock")
				)
			);
			pipeline.add(new Document("$unwind", new Document("path", "$DMStock").append("preserveNullAndEmptyArrays", true)));
			
			cursor = mongoTemplate.getCollection("DMProduct").aggregate(pipeline);
			iter = cursor.iterator();
			if(iter.hasNext()) {
				docTmp = iter.next();
			}
			if(null == docTmp) {
				responseStatus = new MspResponseStatus(9999, "Không tìm thấy thông tin sản phẩm.");
				rsp.setResponseStatus(responseStatus);
				return rsp;
			}
//			if(docTmp.get("DMStock") == null) {
//				responseStatus = new MspResponseStatus(9999, "Không tìm thấy thông tin kho hàng.");
//				rsp.setResponseStatus(responseStatus);
//				return rsp;
//			}
			
			options = new FindOneAndUpdateOptions();
			options.upsert(false);
			options.maxTime(5000, TimeUnit.MILLISECONDS);
			options.returnDocument(ReturnDocument.AFTER);
			
			docR = mongoTemplate.getCollection("DMProduct").findOneAndUpdate(
				docFind,
				new Document("$set", 
					new Document("Name", name)
					.append("Stock", stock)
					.append("Unit", unit)
					.append("Slsx", slsx)
					.append("Price", commons.ToNumber(price))
					.append("VatRate", commons.ToNumber(vatRate))
					.append("Description", description)
					.append("thdoi_tonkho", thdoiTonkho)
					.append("Remark", remark)
					.append("tkvt", tkvt)
					.append("tkgv", tkgv)
					.append("tkdt", tkdt)
					.append("loaivt", loaivt)
					.append("nh_vt1", nh_vt1)
					.append("nh_vt2", nh_vt2)
					.append("nh_vt3", nh_vt3)
					.append("sua_tk_tonkho", "Y".equals(sua_tk_tonkho))
					.append("cach_tinh_gia_ton", cach_tinh_gia_ton)
					.append("tk_cl_vt", tk_cl_vt)
					.append("tk_dtnb", tk_dtnb)
					.append("InfoUpdated", 
						new Document("UpdatedDate", LocalDateTime.now())
							.append("UpdatedUserID", header.getUserId())
							.append("UpdatedUserName", header.getUserName())
							.append("UpdatedUserFullName", header.getUserFullName())
					)
				),
				options
			);
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
			
			mongoTemplate.getCollection("DMProduct").updateMany(
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
			
			responseStatus = new MspResponseStatus(0, "SUCCESS");
			rsp.setResponseStatus(responseStatus);
			return rsp;			
		default:
			responseStatus = new MspResponseStatus(9998, Constants.MAP_ERROR.get(9998));
			rsp.setResponseStatus(responseStatus);
			return rsp;
		}
	}

/*
db.getCollection('DMProduct').find({
    'IssuerId': '61b851ebb0228bba71fca2ec', IsDelete: {$ne: true}, _id: ObjectId("61d166fde69f8a2eb2b01e43")
});
 * */
	
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
		Iterable<Document> cursor = mongoTemplate.getCollection("DMProduct").find(docFind);
		Iterator<Document> iter = cursor.iterator();
		if(iter.hasNext()) {
			docTmp = iter.next();
		}
		
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
			sb.append(commons.getCellValue(formulaEvaluator, row.getCell(0)).replaceAll("\\s", "").toUpperCase());
			if("".equals(sb.toString())) continue;
			
			try {
				hItem = new HashMap<String, String>();
				hItem.put("Code", sb.toString());
				hItem.put("Name", commons.getCellValue(formulaEvaluator, row.getCell(1)).trim().replaceAll("\\s+", " "));
				hItem.put("Price", commons.getCellValue(formulaEvaluator, row.getCell(2)).trim().replaceAll("\\s+", " "));
				hItem.put("Unit", commons.getCellValue(formulaEvaluator, row.getCell(3)).trim().replaceAll("\\s+", " "));
				hItem.put("Description", commons.getCellValue(formulaEvaluator, row.getCell(4)).trim().replaceAll("\\s+", " "));
				hItem.put("VatRate", commons.getCellValue(formulaEvaluator, row.getCell(5)).trim().replaceAll("\\s+", " "));
				hItem.put("Stock", commons.getCellValue(formulaEvaluator, row.getCell(6)).trim().replaceAll("\\s+", " "));
				hItem.put("Slsx", commons.getCellValue(formulaEvaluator, row.getCell(7)).trim().replaceAll("\\s+", " "));
				hItem.put("thdoi_tonkho", commons.getCellValue(formulaEvaluator, row.getCell(8)).trim().replaceAll("\\s+", " "));
				hItem.put("tkvt", commons.getCellValue(formulaEvaluator, row.getCell(9)).trim().replaceAll("\\s+", " "));
				hItem.put("tkgv", commons.getCellValue(formulaEvaluator, row.getCell(10)).trim().replaceAll("\\s+", " "));
				hItem.put("tkdt", commons.getCellValue(formulaEvaluator, row.getCell(11)).trim().replaceAll("\\s+", " "));
				hItem.put("loaivt", commons.getCellValue(formulaEvaluator, row.getCell(12)).trim().replaceAll("\\s+", " "));
				hItem.put("sua_tk_tonkho", commons.getCellValue(formulaEvaluator, row.getCell(13)).trim().replaceAll("\\s+", " "));
				hItem.put("cach_tinh_gia_ton", commons.getCellValue(formulaEvaluator, row.getCell(14)).trim().replaceAll("\\s+", " "));
				hItem.put("nh_vt1", commons.getCellValue(formulaEvaluator, row.getCell(15)).trim().replaceAll("\\s+", " "));
				hItem.put("nh_vt2", commons.getCellValue(formulaEvaluator, row.getCell(16)).trim().replaceAll("\\s+", " "));
				hItem.put("nh_vt3", commons.getCellValue(formulaEvaluator, row.getCell(17)).trim().replaceAll("\\s+", " "));
				hItem.put("tk_cl_vt", commons.getCellValue(formulaEvaluator, row.getCell(18)).trim().replaceAll("\\s+", " "));
				hItem.put("tk_dtnb", commons.getCellValue(formulaEvaluator, row.getCell(19)).trim().replaceAll("\\s+", " "));
				hItem.put("Remark", commons.getCellValue(formulaEvaluator, row.getCell(20)).trim().replaceAll("\\s+", " "));				
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
		List<WriteModel<Document>> ous = new ArrayList<WriteModel<Document>>();
		BulkWriteResult r = null;
		UpdateOptions uo = new UpdateOptions();
		uo.upsert(true);
		
		for(HashMap<String, String> hO: datas) {
			docFilter = new Document("IssuerId", header.getIssuerId()).append("IsDelete", new Document("$ne", true))
					.append("Code", hO.get("Code")).append("Stock", hO.get("Stock"));
			ous.add(
				new UpdateOneModel<>(
					docFilter, 
					new Document("$set", 
						new Document("Name", hO.get("Name"))
						.append("Code", hO.get("Code"))
						.append("Stock", hO.get("Stock"))
						.append("Slsx", hO.get("Slsx"))
						.append("Unit", hO.get("Unit"))
						.append("Price", commons.ToNumber(hO.get("Price")))
						.append("VatRate", commons.ToNumber(hO.get("VatRate")))
						.append("Description", hO.get("Description"))
						.append("thdoi_tonkho", hO.get("thdoi_tonkho"))
						.append("tkvt", hO.get("tkvt"))
						.append("tkgv", hO.get("tkgv"))
						.append("tkdt", hO.get("tkdt"))
						.append("loaivt", hO.get("loaivt"))
						.append("sua_tk_tonkho", "Y".equals(hO.get("sua_tk_tonkho")))
						.append("cach_tinh_gia_ton", hO.get("cach_tinh_gia_ton"))
						.append("nh_vt1", hO.get("nh_vt1"))
						.append("nh_vt2", hO.get("nh_vt2"))
						.append("nh_vt3", hO.get("nh_vt3"))
						.append("tk_cl_vt", hO.get("tk_cl_vt"))
						.append("tk_dtnb", hO.get("tk_dtnb"))
						.append("Remark", hO.get("Remark"))
						.append("IsDelete", false)
						.append("InfoUpdated", 
							new Document("UpdatedDate", LocalDateTime.now())
								.append("UpdatedUserID", header.getUserId())
								.append("UpdatedUserName", header.getUserName())
								.append("UpdatedUserFullName", header.getUserFullName())
						)
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
		r = mongoTemplate.getCollection("DMProduct").bulkWrite(
			ous,
			new BulkWriteOptions().ordered(false)
		);
	
		responseStatus = new MspResponseStatus(0, "SUCCESS");
		rsp.setResponseStatus(responseStatus);
		return rsp;
	}

	@Override
	public FileInfo exportExcel(JSONRoot jsonRoot) throws Exception {
		Msg msg = jsonRoot.getMsg();
		MsgHeader header = msg.getMsgHeader();
		MsgPage page = msg.getMsgPage();
		Object objData = msg.getObjData();
		
//		List<Document> pipeline = new ArrayList<Document>();
//		pipeline = buildListPipeline(objData, header);
		
		String type = "";
		String fromDate = "";
		String toDate = "";
		JsonNode jsonData = null;
		if(objData != null) {
			jsonData = Json.serializer().nodeFromObject(objData);
			
			type = commons.getTextJsonNode(jsonData.at("/Type")).replaceAll("\\s", "");
			fromDate = commons.getTextJsonNode(jsonData.at("/FromDate")).replaceAll("\\s", "");
			toDate = commons.getTextJsonNode(jsonData.at("/ToDate")).replaceAll("\\s", "");
		}
		
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
			
			List<String> headers = Arrays.asList(
				"Mã vật tư\r\n(ma_vt)"
				, "Tên vật tư\r\n(ten_vt)"
				, "Đơn vị tính\r\n(dvt)"				
				, "Kho\r\n(kho)"
				, "Đơn giá\r\n(price)"				
				, "Theo dõi tồn kho\r\n(vt_ton_kho)"
				, "Tk vật tư\r\n(tk_vt)"
				, "Tk giá vốn\r\n(tk_gv)"
				, "Tk doanh thu\r\n(tk_dt)"
				, "Tk hàng bán bị trả lại\r\n(tk_tl)"
				, "Tk sp dở dang\r\n(tk_spdd)"
				, "Loại vật tư\r\n(loai_vt)"
				, "Cho sửa tk kho\r\n(sua_tk_vt)"
				, "Tk NVL\r\n(tk_nvl)"
				, "Tk chiết khấu\r\n(tk_ck)"
				, "Tk khuyến mại\r\n(tk_km)"
				, "Mã phụ\r\n(part_no)"
				, "Tên 2\r\n(ten_vt2)"
				, "Cách tính giá tồn kho\r\n(gia_ton)"
				, "Nhóm vt 1\r\n(nh_vt1)"
				, "Nhóm vt 2\r\n(nh_vt2)"
				, "Nhóm vt 3\r\n(nh_vt3)"
				, "Số lượng tồn tối thiểu:Q\r\n(sl_min)"
				, "Số lượng tồn tối đa:Q\r\n(sl_max)"
				, "Tk chênh lệch vật tư\r\n(tk_cl_vt)"
				, "Tk doanh thu nội bộ\r\n(tk_dtnb)"
				, "Ghi chú\r\n(ghi_chu)"
				, "Số lô SX\r\n(slsx)"
				, "Mã tra cứu\r\n(ma_tra_cuu)"
			);
			row = sheet.getRow(0);
			if(null == row) row = sheet.createRow(0);
			for(int i = 0; i < headers.size(); i++) {
				cell = row.getCell(i);
				if(cell == null) cell = row.createCell(i);
				cell.setCellStyle(styleHeader);
				cell.setCellValue(headers.get(i));
				switch (i) {
				case 1:
					sheet.setColumnWidth(i, 10000);
					break;
				default:
					sheet.setColumnWidth(i, 5000);
					break;
				}
			}
			
			Document docTmp = null;
			Document docMatch = null;
			Iterable<Document> cursor = null;
			Iterator<Document> iter = null;
			
			Map<String, String> mapStock = new HashMap<String, String>();
			/*LAY DANH SACH KHO - LUU VO MAP*/
			cursor = mongoTemplate.getCollection("DMStock").find(new Document("IsDelete", new Document("$ne", true)).append("IssuerId", header.getIssuerId()));
			iter = cursor.iterator();
			while(iter.hasNext()) {
				docTmp = iter.next();
				mapStock.put(docTmp.get("Code", ""), docTmp.get("Name", ""));
			}
			
			int pos = -1;
			int offset = 2;
			
			LocalDate dateFrom = null;
			LocalDate dateTo = null;
			Document docMatchDate = null;
			
			if("DATE".equals(type)) {
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
			}
			
			docMatch = new Document("IsDelete", new Document("$ne", true)).append("IssuerId", header.getIssuerId());
			if(docMatchDate != null) {
				docMatch.append("InfoCreated.CreateDate", docMatchDate);
			}
			
			int posRowData = 1;
			cursor = mongoTemplate.getCollection("DMProduct").find(docMatch).sort(new Document("Stock", 1).append("Code", 1));
			iter = cursor.iterator();
			while(iter.hasNext()) {
				docTmp = iter.next();
				
				row = sheet.getRow(posRowData);
				if(null == row) row = sheet.createRow(posRowData);
				
				cell = row.getCell(0);
				if(cell == null) cell = row.createCell(0);
				cell.setCellStyle(styleInfoL);
				cell.setCellValue(docTmp.get("Code", ""));
				
				cell = row.getCell(1);
				if(cell == null) cell = row.createCell(1);
				cell.setCellStyle(styleInfoL);
				cell.setCellValue(docTmp.get("Name", ""));
				
				cell = row.getCell(2);
				if(cell == null) cell = row.createCell(2);
				cell.setCellStyle(styleInfoL);
				cell.setCellValue(docTmp.get("Unit", ""));
				
				cell = row.getCell(3);
				if(cell == null) cell = row.createCell(3);
				cell.setCellStyle(styleInfoL);
				cell.setCellValue(
					null == mapStock.get(docTmp.get("Stock", ""))? docTmp.get("Stock", ""): mapStock.get(docTmp.get("Stock", ""))
				);
				
				cell = row.getCell(4);
				if(cell == null) cell = row.createCell(4);
				cell.setCellStyle(styleInfoR);
				cell.setCellValue(docTmp.get("Price", 0D));
				
				cell = row.getCell(3 + offset);
				if(cell == null) cell = row.createCell(3 + offset);
				cell.setCellStyle(styleInfoL);
				cell.setCellValue(docTmp.get("cach_tinh_gia_ton", ""));
				
				cell = row.getCell(4 + offset);
				if(cell == null) cell = row.createCell(4 + offset);
				cell.setCellStyle(styleInfoL);
				cell.setCellValue(docTmp.get("tkvt", ""));
				
				cell = row.getCell(5 + offset);
				if(cell == null) cell = row.createCell(5 + offset);
				cell.setCellStyle(styleInfoL);
				cell.setCellValue(docTmp.get("tkgv", ""));
				
				cell = row.getCell(6 + offset);
				if(cell == null) cell = row.createCell(6 + offset);
				cell.setCellStyle(styleInfoL);
				cell.setCellValue(docTmp.get("tkdt", ""));
				
				cell = row.getCell(7 + offset);
				if(cell == null) cell = row.createCell(7 + offset);
				cell.setCellStyle(styleInfoL);
				cell.setCellValue("");
				
				cell = row.getCell(8 + offset);
				if(cell == null) cell = row.createCell(8 + offset);
				cell.setCellStyle(styleInfoL);
				cell.setCellValue("");
				
				cell = row.getCell(9 + offset);
				if(cell == null) cell = row.createCell(9 + offset);
				cell.setCellStyle(styleInfoL);
				cell.setCellValue(docTmp.get("loaivt", ""));
				
				cell = row.getCell(10 + offset);
				if(cell == null) cell = row.createCell(10 + offset);
				cell.setCellStyle(styleInfoL);
				cell.setCellValue(docTmp.get("loaivt", ""));
				cell.setCellValue(docTmp.get("sua_tk_tonkho") == null? "0": (docTmp.getBoolean("sua_tk_tonkho", false)? "1": "0"));
				
				cell = row.getCell(11 + offset);
				if(cell == null) cell = row.createCell(11 + offset);
				cell.setCellStyle(styleInfoL);
				cell.setCellValue("");
				
				cell = row.getCell(12 + offset);
				if(cell == null) cell = row.createCell(12 + offset);
				cell.setCellStyle(styleInfoL);
				cell.setCellValue("");
				
				cell = row.getCell(13 + offset);
				if(cell == null) cell = row.createCell(13 + offset);
				cell.setCellStyle(styleInfoL);
				cell.setCellValue("");
				
				cell = row.getCell(14 + offset);
				if(cell == null) cell = row.createCell(14 + offset);
				cell.setCellStyle(styleInfoL);
				cell.setCellValue("");
				
				cell = row.getCell(15 + offset);
				if(cell == null) cell = row.createCell(15 + offset);
				cell.setCellStyle(styleInfoL);
				cell.setCellValue("");
				
				cell = row.getCell(16 + offset);
				if(cell == null) cell = row.createCell(16 + offset);
				cell.setCellStyle(styleInfoL);
				cell.setCellValue(docTmp.get("cach_tinh_gia_ton", ""));
				
				cell = row.getCell(17 + offset);
				if(cell == null) cell = row.createCell(17 + offset);
				cell.setCellStyle(styleInfoL);
				cell.setCellValue(docTmp.get("nh_vt1", ""));
				
				cell = row.getCell(18 + offset);
				if(cell == null) cell = row.createCell(18 + offset);
				cell.setCellStyle(styleInfoL);
				cell.setCellValue(docTmp.get("nh_vt2", ""));
				
				cell = row.getCell(19 + offset);
				if(cell == null) cell = row.createCell(19 + offset);
				cell.setCellStyle(styleInfoL);
				cell.setCellValue(docTmp.get("nh_vt3", ""));
				
				cell = row.getCell(20 + offset);
				if(cell == null) cell = row.createCell(20 + offset);
				cell.setCellStyle(styleInfoL);
				cell.setCellValue("");
				
				cell = row.getCell(21 + offset);
				if(cell == null) cell = row.createCell(21 + offset);
				cell.setCellStyle(styleInfoL);
				cell.setCellValue("");
				
				cell = row.getCell(22 + offset);
				if(cell == null) cell = row.createCell(22 + offset);
				cell.setCellStyle(styleInfoL);
				cell.setCellValue(docTmp.get("tk_cl_vt", ""));
				
				cell = row.getCell(23 + offset);
				if(cell == null) cell = row.createCell(23 + offset);
				cell.setCellStyle(styleInfoL);
				cell.setCellValue(docTmp.get("tk_dtnb", ""));
				
				cell = row.getCell(24 + offset);
				if(cell == null) cell = row.createCell(24 + offset);
				cell.setCellStyle(styleInfoL);
				cell.setCellValue(docTmp.get("Remark", ""));
				
				cell = row.getCell(25 + offset);
				if(cell == null) cell = row.createCell(25 + offset);
				cell.setCellStyle(styleInfoL);
				cell.setCellValue(docTmp.get("Slsx", ""));
				
				cell = row.getCell(26 + offset);
				if(cell == null) cell = row.createCell(26 + offset);
				cell.setCellStyle(styleInfoL);
				cell.setCellValue("");
				
				
				posRowData++;
			}
			
			
			out = new ByteArrayOutputStream();
			wb.write(out);
			
			FileInfo fileInfo = new FileInfo();
			fileInfo.setFileName("DANH-MUC-SAN-PHAM.xlsx");
			fileInfo.setContentFile(out.toByteArray());			
			return fileInfo;
		}catch(Exception e) {
			throw e;
		}finally {
			try{wb.dispose(); wb.close();}catch(Exception ex){}
		}
	}

}
