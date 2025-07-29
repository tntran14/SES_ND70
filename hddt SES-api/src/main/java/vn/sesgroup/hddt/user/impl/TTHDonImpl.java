package vn.sesgroup.hddt.user.impl;

import java.util.ArrayList;
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
import vn.sesgroup.hddt.user.dao.TTHDonDAO;
import vn.sesgroup.hddt.user.service.JPUtils;
import vn.sesgroup.hddt.user.service.TCTNService;
import vn.sesgroup.hddt.utility.Json;

@Repository
@Transactional
public class TTHDonImpl extends AbstractDAO implements TTHDonDAO {
	private static final Logger log = LogManager.getLogger(TTHDonImpl.class);
	@Autowired ConfigConnectMongo cfg;
	@Autowired TCTNService tctnService;

	@Autowired
	JPUtils jpUtils;
	Document docUpsert = null;
	@Override
	public MsgRsp check(JSONRoot jsonRoot) throws Exception {
		Msg msg = jsonRoot.getMsg();
		MsgHeader header = msg.getMsgHeader();
		MsgPage page = msg.getMsgPage();

		Object objData = msg.getObjData();

		JsonNode jsonData = null;
		if (objData != null) {
			jsonData = Json.serializer().nodeFromObject(msg.getObjData());
		} else {
			throw new Exception("Lỗi dữ liệu đầu vào");
		}

		MsgRsp rsp = new MsgRsp(header);
		rsp.setMsgPage(page);
		MspResponseStatus responseStatus = null;

		String mtdiep = commons.getTextJsonNode(jsonData.at("/MTDiep")).replaceAll("\\s", "");
		ObjectId objectId = null;

		List<Document> pipeline = new ArrayList<Document>();

		Document docMatch = new Document("MTDiep", mtdiep).append("IsDelete", new Document("$ne", true));
		Document docFilter = new Document("_id", 1).append("EInvoiceDetail.TTChung", 1)
				.append("EInvoiceDetail.NDHDon.NMua", 1).append("SignStatusCode", 1).append("EInvoiceStatus", 1)
				.append("MTDTChieu", 1).append("MTDiep", 1);
		pipeline = new ArrayList<Document>();
		pipeline.add(new Document("$match", docMatch));
		pipeline.add(new Document("$project", docFilter));
		pipeline.addAll(createFacetForSearchNotSort(page));

		Document docTmp = null;
		
		Iterator<Document> iter = null;

		try (MongoClient mongoClient = cfg.mongoClient()) {
			MongoCollection<Document> collection = mongoClient.getDatabase(cfg.dbName).getCollection("EInvoice");
			iter = collection.aggregate(pipeline).allowDiskUse(true).iterator();
		} catch (Exception e) {
		}

		if (iter.hasNext()) {
			docTmp = iter.next();

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
					
					hItem = new HashMap<String, Object>();
					hItem.put("_id", objectId.toString());
					hItem.put("EInvoiceStatus", doc.get("EInvoiceStatus"));
					hItem.put("SignStatusCode", doc.get("SignStatusCode"));
					hItem.put("MTDiep", doc.get("MTDiep"));
					hItem.put("EInvoiceDetail", doc.get("EInvoiceDetail"));
					hItem.put("MTDiep", doc.get("MTDiep"));
					hItem.put("MTDTChieu", doc.get("MTDTChieu"));
					
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

		docTmp = null;
		iter = null;
		try (MongoClient mongoClient = cfg.mongoClient()) {
			MongoCollection<Document> collection = mongoClient.getDatabase(cfg.dbName).getCollection("EInvoiceBH");
			iter = collection.aggregate(pipeline).allowDiskUse(true).iterator();
		} catch (Exception e) {
		}

		if (iter.hasNext()) {
			docTmp = iter.next();
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

					hItem = new HashMap<String, Object>();
					hItem.put("_id", objectId.toString());
					hItem.put("EInvoiceStatus", doc.get("EInvoiceStatus"));
					hItem.put("SignStatusCode", doc.get("SignStatusCode"));
					hItem.put("MTDiep", doc.get("MTDiep"));
					hItem.put("EInvoiceDetail", doc.get("EInvoiceDetail"));
					hItem.put("MTDiep", doc.get("MTDiep"));
					hItem.put("MTDTChieu", doc.get("MTDTChieu"));

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

		docTmp = null;
		iter = null;
		try (MongoClient mongoClient = cfg.mongoClient()) {
			MongoCollection<Document> collection = mongoClient.getDatabase(cfg.dbName).getCollection("EInvoicePXK");
			iter = collection.aggregate(pipeline).allowDiskUse(true).iterator();
		} catch (Exception e) {
		}
		if (iter.hasNext()) {
			docTmp = iter.next();
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

					hItem = new HashMap<String, Object>();
					hItem.put("_id", objectId.toString());
					hItem.put("EInvoiceStatus", doc.get("EInvoiceStatus"));
					hItem.put("SignStatusCode", doc.get("SignStatusCode"));
					hItem.put("MTDiep", doc.get("MTDiep"));
					hItem.put("EInvoiceDetail", doc.get("EInvoiceDetail"));
					hItem.put("MTDiep", doc.get("MTDiep"));
					hItem.put("MTDTChieu", doc.get("MTDTChieu"));

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

		docTmp = null;
		iter = null;
		try (MongoClient mongoClient = cfg.mongoClient()) {
			MongoCollection<Document> collection = mongoClient.getDatabase(cfg.dbName).getCollection("EInvoicePXKDL");
			iter = collection.aggregate(pipeline).allowDiskUse(true).iterator();
		} catch (Exception e) {
		}
		if (iter.hasNext()) {
			docTmp = iter.next();
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

					hItem = new HashMap<String, Object>();
					hItem.put("_id", objectId.toString());
					hItem.put("EInvoiceStatus", doc.get("EInvoiceStatus"));
					hItem.put("SignStatusCode", doc.get("SignStatusCode"));
					hItem.put("MTDiep", doc.get("MTDiep"));
					hItem.put("EInvoiceDetail", doc.get("EInvoiceDetail"));
					hItem.put("MTDiep", doc.get("MTDiep"));
					hItem.put("MTDTChieu", doc.get("MTDTChieu"));

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

		responseStatus = new MspResponseStatus(999, "Không tìm thấy hóa đơn.");
		rsp.setResponseStatus(responseStatus);
		return rsp;
	}
	
	
	@Override
	public MsgRsp crud(JSONRoot jsonRoot) throws Exception {
		Msg msg = jsonRoot.getMsg();
		MsgHeader header = msg.getMsgHeader();
		MsgPage page = msg.getMsgPage();
	
		Object objData = msg.getObjData();
		FindOneAndUpdateOptions options = null;
		JsonNode jsonData = null;
		if (objData != null) {
			jsonData = Json.serializer().nodeFromObject(msg.getObjData());
		} else {
			throw new Exception("Lỗi dữ liệu đầu vào");
		}
		
		MsgRsp rsp = new MsgRsp(header);
		rsp.setMsgPage(page);
		MspResponseStatus responseStatus = null;
		
		String mtdiep = commons.getTextJsonNode(jsonData.at("/MTDiep")).replaceAll("\\s", "");
		String TTHDon = commons.getTextJsonNode(jsonData.at("/TTHDon")).replaceAll("\\s", "");
		String tthd = "";
		
		//SEARCH  
		List<Document> pipeline = new ArrayList<Document>();
		Document docMatch = new Document("MTDiep", mtdiep)
				.append("IsDelete", new Document("$ne", true));
		
		pipeline = new ArrayList<Document>();
		pipeline.add(new Document("$match", docMatch));
		pipeline.addAll(createFacetForSearchNotSort(page));

		
		Document docTmp1 = null;	
		Document docTmp2 = null;
		Document docTmp3 = null;
		Document docTmp4 = null;
		
		Document docFind = new Document("MTDiep", mtdiep)
				.append("IsDelete", new Document("$ne", true));
		 
		 MongoClient mongoClient = cfg.mongoClient();
		 MongoCollection<Document> collection = mongoClient.getDatabase(cfg.dbName).getCollection("EInvoice");
		       try {
		    	   docTmp1 = collection.aggregate(pipeline).allowDiskUse(true).iterator().next();    
		       } catch (Exception e) {
		         
		       }
		         
		 mongoClient.close(); 
		 
		 mongoClient = cfg.mongoClient();
		 collection = mongoClient.getDatabase(cfg.dbName).getCollection("EInvoiceBH");
		   try {
			   docTmp2 = collection.aggregate(pipeline).allowDiskUse(true).iterator().next();      
		      } catch (Exception e) {
		         
		     }
		 mongoClient.close();
		 
		 mongoClient = cfg.mongoClient();
		 collection = mongoClient.getDatabase(cfg.dbName).getCollection("EInvoicePXK");
		   try {
		          docTmp3 = collection.aggregate(pipeline).allowDiskUse(true).iterator().next();      
		      } catch (Exception e) {
		         
		     }
		 mongoClient.close();
		 
		 mongoClient = cfg.mongoClient();
		 collection = mongoClient.getDatabase(cfg.dbName).getCollection("EInvoicePXKDL");
		   try {
		          docTmp4 = collection.aggregate(pipeline).allowDiskUse(true).iterator().next();      
		      } catch (Exception e) {
		         
		     }
		 mongoClient.close();
		 
		
		if(TTHDon.equals("1")) {
			tthd = "CREATED";
		}else if(TTHDon.equals("2")) {
			tthd = "PENDING";
		}else if(TTHDon.equals("3")) {
			tthd = "PROCESSING";
		}else if(TTHDon.equals("4")) {
			tthd = "COMPLETE";
	
		}else if(TTHDon.equals("5")) {
			tthd ="ERROR_CQT";
		}else if(TTHDon.equals("6")) {
			tthd = "DELETED";
		}else if(TTHDon.equals("7")) {
			tthd = "REPLACED";
		}else if(TTHDon.equals("8")) {
			tthd = "ADJUSTED";
		}
		

		try {
		
		if(docTmp1!=null) {
			options = new FindOneAndUpdateOptions();
			options.upsert(false);
			options.maxTime(5000, TimeUnit.MILLISECONDS);
			options.returnDocument(ReturnDocument.AFTER);
			
			mongoClient = cfg.mongoClient();
			collection = mongoClient.getDatabase(cfg.dbName).getCollection("EInvoice");
			collection.findOneAndUpdate(docFind,
					new Document("$set", new Document("EInvoiceStatus", tthd)
							),
			options);
			  mongoClient.close();

			
			responseStatus = new MspResponseStatus(0, "SUCCESS");
			rsp.setResponseStatus(responseStatus);
			return rsp;
			
		}else if(docTmp2 !=null){
			options = new FindOneAndUpdateOptions();
			options.upsert(false);
			options.maxTime(5000, TimeUnit.MILLISECONDS);
			options.returnDocument(ReturnDocument.AFTER);

			MongoClient mongoClient1 = cfg.mongoClient();
			collection = mongoClient1.getDatabase(cfg.dbName).getCollection("EInvoiceBH");
			collection.findOneAndUpdate(docFind,
					new Document("$set", new Document("EInvoiceStatus", tthd)
							),
			options);
			  mongoClient1.close();
			
			responseStatus = new MspResponseStatus(0, "SUCCESS");
			rsp.setResponseStatus(responseStatus);
	
			return rsp;
		}else if(docTmp3!=null){
			options = new FindOneAndUpdateOptions();
			options.upsert(false);
			options.maxTime(5000, TimeUnit.MILLISECONDS);
			options.returnDocument(ReturnDocument.AFTER);
			
			MongoClient mongoClient1 = cfg.mongoClient();
			collection = mongoClient1.getDatabase(cfg.dbName).getCollection("EInvoicePXK");
			collection.findOneAndUpdate(docFind,
					new Document("$set", new Document("EInvoiceStatus", tthd)
							),
			options);
			  mongoClient1.close();

			responseStatus = new MspResponseStatus(0, "SUCCESS");
			rsp.setResponseStatus(responseStatus);
			return rsp;
		}else if(docTmp4!=null){
			options = new FindOneAndUpdateOptions();
			options.upsert(false);
			options.maxTime(5000, TimeUnit.MILLISECONDS);
			options.returnDocument(ReturnDocument.AFTER);

			MongoClient mongoClient1 = cfg.mongoClient();
			collection = mongoClient1.getDatabase(cfg.dbName).getCollection("EInvoicePXKDL");
			collection.findOneAndUpdate(docFind,
					new Document("$set", new Document("EInvoiceStatus", tthd)
							),
			options);
			  mongoClient1.close();
			  
			responseStatus = new MspResponseStatus(0, "SUCCESS");
			rsp.setResponseStatus(responseStatus);
			return rsp;
		}else {
			responseStatus = new MspResponseStatus(999, "Không tìm thấy hóa đơn");
			rsp.setResponseStatus(responseStatus);
			return rsp;
		}
	}catch (Exception e) {
		responseStatus = new MspResponseStatus(999, "Lỗi ngoại lệ");
		rsp.setResponseStatus(responseStatus);
		return rsp;
	}

		
	}
	
	
}
