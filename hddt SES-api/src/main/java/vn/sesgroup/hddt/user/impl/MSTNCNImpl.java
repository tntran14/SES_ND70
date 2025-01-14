package vn.sesgroup.hddt.user.impl;

import java.io.File;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
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
import com.mongodb.client.model.FindOneAndUpdateOptions;
import com.mongodb.client.model.ReturnDocument;

import vn.sesgroup.hddt.configuration.ConfigConnectMongo;
import vn.sesgroup.hddt.user.dao.AbstractDAO;
import vn.sesgroup.hddt.user.dao.MSTNCNDao;
import vn.sesgroup.hddt.user.service.TCTNService;
import vn.sesgroup.hddt.utility.Constants;
import vn.sesgroup.hddt.utility.Json;
import vn.sesgroup.hddt.utility.SystemParams;


@Repository
@Transactional
public class MSTNCNImpl extends AbstractDAO implements MSTNCNDao {
	private static final Logger log = LogManager.getLogger(MSTNCNImpl.class);
	@Autowired
	ConfigConnectMongo cfg;
	@Autowired
	TCTNService tctnService;

	@Transactional(rollbackFor = { Exception.class })
	@Override
	public MsgRsp crud(JSONRoot jsonRoot) throws Exception {
		Msg msg = jsonRoot.getMsg();
		MsgHeader header = msg.getMsgHeader();
		MsgPage page = msg.getMsgPage();
		Object objData = msg.getObjData();
		Document docR = null;
		Document docRDp = null;
		FindOneAndUpdateOptions options = null;
		FindOneAndUpdateOptions options2 = null;
		JsonNode jsonData = null;
		if (objData != null) {
			jsonData = Json.serializer().nodeFromObject(msg.getObjData());
		} else {
			throw new Exception("Lỗi dữ liệu đầu vào");
		}

		String actionCode = header.getActionCode();
		String _id = commons.getTextJsonNode(jsonData.at("/_id")).replaceAll("\\s", "");
		String khhd = commons.getTextJsonNode(jsonData.at("/KiHieuHD")).replaceAll("\\s", "");
		String macqt = "Chứng từ khấu trừ tự in(Thông tư 78/2022/TT-BTC)";
		String sl = commons.getTextJsonNode(jsonData.at("/SOLUONG"));
		String phoict = commons.getTextJsonNode(jsonData.at("/PhoiCT"));
		String phoicttext = commons.getTextJsonNode(jsonData.at("/PhoiCTText"));
		 int number = 0;
		if(!sl.equals("")) {
			  number = Integer.parseInt(sl);
		}
	   
		String yearCreated = commons.getTextJsonNode(jsonData.at("/NamPhatHanh")).trim().replaceAll("\\s+", " ");
		String macty = commons.getTextJsonNode(jsonData.at("/MaCty")).trim().replaceAll("\\s+", " ");
		String logo = commons.getTextJsonNode(jsonData.at("/Logo")).trim().replaceAll("\\s+", " ");

		MsgRsp rsp = new MsgRsp(header);
		rsp.setMsgPage(page);
		MspResponseStatus responseStatus = null;

		ObjectId objectId = null;
		ObjectId objectIdUser = null;

		List<Document> pipeline = null;

		Document docFind = null;
		Document docFind2 = null;
		Document docTmp = null;
		Document docUpsert = null;

		Iterable<Document> cursor = null;
		Iterator<Document> iter = null;

//////////////////////////////////////////////////////////////////////////////

		switch (actionCode) {
		case Constants.MSG_ACTION_CODE.CREATED:
			objectId = null;
			objectIdUser = null;
			try {
				objectId = new ObjectId(header.getIssuerId());
			} catch (Exception ex) {
			}
			try {
				objectIdUser = new ObjectId(header.getUserId());
			} catch (Exception ex) {
			}

			/*
			 * KIEM TRA THONG TIN KHACH HANG - USER - TINH THANH - CO QUAN THUE CO TON TAI
			 * KHONG
			 */
		
			docFind = new Document("_id", objectId).append("IsActive", true).append("IsDelete",
					new Document("$ne", true));
			pipeline = new ArrayList<Document>();
			pipeline.add(new Document("$match", docFind));
			new Document("$project", new Document("_id", 1));
			
			pipeline.add(new Document("$lookup", new Document("from", "Users").append("pipeline", Arrays.asList(
					new Document("$match",
							new Document("IssuerId", header.getIssuerId()).append("_id", objectIdUser)
									.append("IsActive", true).append("IsDelete", new Document("$ne", true))),
					new Document("$project", new Document("_id", 1).append("UserName", 1).append("FullName", 1)),
					new Document("$limit", 1))).append("as", "UserInfo")));
			new Document("$project", new Document("_id", 1));
			pipeline.add(new Document("$unwind",
					new Document("path", "$UserInfo").append("preserveNullAndEmptyArrays", true)));
		
			
			pipeline.add(new Document("$lookup",
					new Document("from", "DMTemplates")
							.append("pipeline", Arrays.asList(new Document("$match", new Document("Name", phoicttext))))
							.append("as", "DMTemplates")));
			new Document("$project", new Document("_id", 1).append("FileName", 1));
			pipeline.add(new Document("$unwind",
					new Document("path", "$DMTemplates").append("preserveNullAndEmptyArrays", true)));
		
			
						pipeline.add(new Document("$lookup",
					new Document("from", "DMMSTNCN")
							.append("pipeline", Arrays.asList(new Document("$match", new Document("IssuerId", header.getIssuerId()).append("KyHieu", macty))))
							.append("as", "DMTemplatesInfo")));
						new Document("$project", new Document("_id", 1));
			pipeline.add(new Document("$unwind",
					new Document("path", "$DMTemplatesInfo").append("preserveNullAndEmptyArrays", true)));

			
			MongoClient mongoClient = cfg.mongoClient();
			MongoCollection<Document> collection = mongoClient.getDatabase(cfg.dbName).getCollection("Issuer");

			try {
				docTmp = collection.aggregate(pipeline).allowDiskUse(true).iterator().next();
			} catch (Exception e) {

			}
			mongoClient.close();

			if (null == docTmp) {
				responseStatus = new MspResponseStatus(9999, "Không tìm thấy thông tin khách hàng.");
				rsp.setResponseStatus(responseStatus);
				return rsp;
			}
			if (docTmp.get("UserInfo") == null) {
				responseStatus = new MspResponseStatus(9999, "Không tìm thấy thông tin người dùng.");
				rsp.setResponseStatus(responseStatus);
				return rsp;
			}
			if (docTmp.get("DMTemplatesInfo") != null) {
				responseStatus = new MspResponseStatus(9999, "Mẫu số TNCN đã tồn tại");
				rsp.setResponseStatus(responseStatus);
				return rsp;
			}
		
			//TẠO FOLDER NEU CHUA CO
			 File directory = new File(SystemParams.DIR_E_INVOICE_TEMPLATE, "MauSoTNCN");
			    if (! directory.exists()){
			        directory.mkdir();
			    }
			//END TAO FOLDER
			
			ObjectId id = new ObjectId();
			/* LUU DU LIEU HD */
			docUpsert = new Document("_id", id).append("IssuerId", header.getIssuerId()).append("SHDHT", 0).append("Mau", macqt  )
					.append("KyHieu", macty).append("MauSo", khhd  ).append("Nam", yearCreated  ).append("ChungTu", "E" ).append("SoLuong", number )	
					.append("TuSo", 1 ).append("DenSo", number ).append("ConLai", number ).append("FileName",
							docTmp.getEmbedded(Arrays.asList("DMTemplates", "FileName"), "")).append("LoGo", logo )	
					.append("IsActive", true).append("IsDelete", false).append("InfoCreated",
							new Document("CreateDate", LocalDateTime.now()).append("CreateUserID", header.getUserId())
							.append("CreateUserName", header.getUserName())
							.append("CreateUserFullName", header.getUserFullName()));
			/* END - LUU DU LIEU HD */
		
			mongoClient = cfg.mongoClient();
			collection = mongoClient.getDatabase(cfg.dbName).getCollection("DMMSTNCN");
			collection.insertOne(docUpsert);			
			mongoClient.close();
			
			responseStatus = new MspResponseStatus(0, "SUCCESS");
			rsp.setResponseStatus(responseStatus);
			return rsp;
////////////////////////////////////////////////////////////////////////////////////////
		case Constants.MSG_ACTION_CODE.MODIFY:
		      try {
		        objectId = new ObjectId(_id);
		      } catch (Exception ex) {
		      }
		      docFind = new Document("_id", objectId).append("IssuerId", header.getIssuerId());
		      pipeline = new ArrayList<Document>();
		      pipeline.add(new Document("$match", docFind));
		      new Document("$project", new Document("_id", 1).append("LoGo", 1));
		      
		      
		      pipeline.add(new Document("$lookup",new Document("from", "DMTemplates")
		      .append("pipeline", Arrays.asList(new Document("$match", new Document("Name", phoicttext))))
		      .append("as", "DMTemplates")));
		      new Document("$project", new Document("_id", 1).append("FileName", 1));
		      pipeline.add(new Document("$unwind",new Document("path", "$DMTemplates").append("preserveNullAndEmptyArrays", true)));
		     
		      
		  	 mongoClient = cfg.mongoClient();
			 collection = mongoClient.getDatabase(cfg.dbName).getCollection("DMMSTNCN");

			try {
				docTmp = collection.aggregate(pipeline).allowDiskUse(true).iterator().next();
			} catch (Exception e) {

			}
			mongoClient.close();
			
			
		      if (null == docTmp) {
		        responseStatus = new MspResponseStatus(9999, "Không tìm thấy thông tin mẫu số TNCN.");
		        rsp.setResponseStatus(responseStatus);
		        return rsp;
		      }
		      
		      //TẠO FOLDER NEU CHUA CO
		       directory = new File(SystemParams.DIR_E_INVOICE_TEMPLATE, "MauSoTNCN");
		          if (! directory.exists()){
		              directory.mkdir();
		          }
		      //END TAO FOLDER
		    if(logo == "") {
		      logo =  (String) docTmp.get("LoGo");
		    }
		          
		          options = new FindOneAndUpdateOptions();
		          options.upsert(false);
		          options.maxTime(5000, TimeUnit.MILLISECONDS);
		          options.returnDocument(ReturnDocument.AFTER);

		      
		          
		      	mongoClient = cfg.mongoClient();
				collection = mongoClient.getDatabase(cfg.dbName).getCollection("DMMSTNCN");
				docR =	collection.findOneAndUpdate(docFind,
			              new Document("$set",
				                  new Document("IssuerId", header.getIssuerId())
				                  .append("FileName",docTmp.getEmbedded(Arrays.asList("DMTemplates", "FileName"), ""))
				                  .append("LoGo", logo )  
				                      .append("InfoUpdated",
				                          new Document("UpdatedDate", LocalDateTime.now())
				                              .append("UpdatedUserID", header.getUserId())
				                              .append("UpdatedUserName", header.getUserName())
				                              .append("UpdatedUserFullName", header.getUserFullName()))),
				              options);		
				mongoClient.close();
				

		          responseStatus = new MspResponseStatus(0, "SUCCESS");
		          rsp.setResponseStatus(responseStatus);
		          return rsp;


///////////////////////////////////////////////////////////////////////////////////////
		case Constants.MSG_ACTION_CODE.DELETE:
			try {
				objectId = new ObjectId(_id);
			} catch (Exception ex) {
			}
			docFind = new Document("_id", objectId).append("IsDelete", new Document("$ne", true));
		
			 mongoClient = cfg.mongoClient();
			 collection = mongoClient.getDatabase(cfg.dbName).getCollection("DMMSTNCN");

			try {
				docTmp = collection.find(docFind).allowDiskUse(true).iterator().next();
			} catch (Exception e) {

			}
			mongoClient.close();

			if (null == docTmp) {
				responseStatus = new MspResponseStatus(9999, "Không tìm thấy thông tin mẫu số TNCN.");
				rsp.setResponseStatus(responseStatus);
				return rsp;
			}
		if(logo == "") {
			logo =	(String) docTmp.get("LoGo");
		}
					options = new FindOneAndUpdateOptions();
					options.upsert(false);
					options.maxTime(5000, TimeUnit.MILLISECONDS);
					options.returnDocument(ReturnDocument.AFTER);
					

					mongoClient = cfg.mongoClient();
					collection = mongoClient.getDatabase(cfg.dbName).getCollection("DMMSTNCN");
					docR =	collection.findOneAndUpdate(
							docFind,				 
						 new Document("$set", 
									new Document("IsDelete", true)
									.append("Infodeleted",
											new Document("DeletedDate", LocalDateTime.now())
											.append("DeletedUserID", header.getUserId())
											.append("DeletedUserName", header.getUserName())
											.append("DeletedUserFullName", header.getUserFullName()))
							),
						 
options);		
					mongoClient.close();
					
					
					responseStatus = new MspResponseStatus(0, "SUCCESS");
					rsp.setResponseStatus(responseStatus);
					return rsp;
	}
	return rsp;

	}

	@Override
	public MsgRsp list(JSONRoot jsonRoot) throws Exception {
		Msg msg = jsonRoot.getMsg();
		MsgHeader header = msg.getMsgHeader();
		MsgPage page = msg.getMsgPage();
		Object objData = msg.getObjData();

		
		JsonNode jsonData = null;
		if(objData != null) {
			jsonData = Json.serializer().nodeFromObject(objData);	
		}

		MsgRsp rsp = new MsgRsp(header);
		MspResponseStatus responseStatus = null;
		
		ObjectId objectId = null;
		Document docTmp = null;
		List<Document> pipeline = new ArrayList<Document>();
		

		
		Document docMatch = new Document("IssuerId", header.getIssuerId());
		
		Document fillter = new Document("_id", 1).append("KyHieu", 1).append("SoLuong", 1)
				.append("LoGo", 1).append("IsDelete", 1).append("IsActive", 1);
		
		pipeline = new ArrayList<Document>();
		pipeline.add(new Document("$match", docMatch));
		pipeline.add(
				new Document("$sort", 
					new Document("_id", -1)
				)
			);
		pipeline.add(new Document("$project", fillter));
		pipeline.addAll(createFacetForSearchNotSort(page));
	
		
		MongoClient mongoClient = cfg.mongoClient();
		MongoCollection<Document> collection = mongoClient.getDatabase(cfg.dbName).getCollection("DMMSTNCN");

		try {
			docTmp = collection.aggregate(pipeline).allowDiskUse(true).iterator().next();
		} catch (Exception e) {

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
				hItem.put("KyHieu", doc.get("KyHieu"));
				hItem.put("SoLuong", doc.get("SoLuong"));
				hItem.put("LoGo", doc.get("LoGo"));
				boolean checkdelete = (boolean) doc.get("IsDelete");
				if( checkdelete == true) {
					hItem.put("IsActive","DELETE");
				}
				else {
					hItem.put("IsActive", doc.get("IsActive"));
				}
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
	
		
		MongoClient mongoClient = cfg.mongoClient();
		MongoCollection<Document> collection = mongoClient.getDatabase(cfg.dbName).getCollection("DMMSTNCN");

		try {
			docTmp = collection.find(docFind).allowDiskUse(true).iterator().next();
		} catch (Exception e) {

		}
		mongoClient.close();

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

	@Override
	public MsgRsp viewimg(JSONRoot jsonRoot, String phoi) throws Exception {
		MsgRsp rsp = new MsgRsp();
		MspResponseStatus responseStatus = null;
		Document docFind = null;
		Iterable<Document> cursor = null;
		if ("" == phoi) {
			responseStatus = new MspResponseStatus(9999, "Vui lòng chọn phôi để xem ảnh.");
			rsp.setResponseStatus(responseStatus);
			return rsp;
		}
		docFind = new Document("Name",phoi);
		Document docTmp = null;
		
		MongoClient mongoClient = cfg.mongoClient();
		MongoCollection<Document> collection = mongoClient.getDatabase(cfg.dbName).getCollection("DMTemplateCT");

		try {
			docTmp = collection.find(docFind).allowDiskUse(true).iterator().next();
		} catch (Exception e) {

		}
		mongoClient.close();
		
		
		if (null == docTmp) {
			responseStatus = new MspResponseStatus(9999, "Không tìm thấy ảnh mẫu.");
			rsp.setResponseStatus(responseStatus);
			return rsp;
		}
		String img =  docTmp.get("Images", "");
		String dir = "C:/hddt-ses/server/template/";
		File f = new File(dir,img);
		String base64Template = commons.encodeImageToBase64(f);
		rsp.setObjData(base64Template);
		return rsp;
	}

	

}
