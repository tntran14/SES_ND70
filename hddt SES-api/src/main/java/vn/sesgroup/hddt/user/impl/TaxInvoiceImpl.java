package vn.sesgroup.hddt.user.impl;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
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
import vn.sesgroup.hddt.user.dao.TaxInvoiceDAO;
import vn.sesgroup.hddt.user.service.TCTNService;
import vn.sesgroup.hddt.utility.Constants;
import vn.sesgroup.hddt.utility.Json;

@Repository
@Transactional
public class TaxInvoiceImpl extends AbstractDAO implements TaxInvoiceDAO {
	private static final Logger log = LogManager.getLogger(TaxInvoiceImpl.class);
	@Autowired ConfigConnectMongo cfg;
	@Autowired TCTNService tctnService;

	@Transactional(rollbackFor = { Exception.class })
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

		String actionCode = header.getActionCode();
		String _id = commons.getTextJsonNode(jsonData.at("/_id")).replaceAll("\\s", "");
		String code = commons.getTextJsonNode(jsonData.at("/code")).trim().replaceAll("\\s+", "");
		String name = commons.getTextJsonNode(jsonData.at("/name")).trim().replaceAll("\\s+", " ");
		String stt = commons.getTextJsonNode(jsonData.at("/stt")).trim().replaceAll("\\s+", "");

		MsgRsp rsp = new MsgRsp(header);
		rsp.setMsgPage(page);
		MspResponseStatus responseStatus = null;

		ObjectId objectId = null;
		List<Document> pipeline = null;
		Document docFind = null;
		Document docTmp = null;
		Document docUpsert = null;

//////////////////////////////////////////////////////////////////////////////

		switch (actionCode) {
		case Constants.MSG_ACTION_CODE.CREATED:
			objectId = null;
			try {
				objectId = new ObjectId(header.getIssuerId());
			} catch (Exception ex) {
			}
		

			/*
			 * KIEM TRA THONG TIN KHACH HANG - USER - TINH THANH - CO QUAN THUE CO TON TAI
			 * KHONG
			 */
		
			docFind = new Document("code", code);
			pipeline = new ArrayList<Document>();
			pipeline.add(new Document("$match", docFind));
			
			MongoClient mongoClient = cfg.mongoClient();
			MongoCollection<Document> collection = mongoClient.getDatabase(cfg.dbName).getCollection("TaxInvoice");
			      try {
			        docTmp = collection.aggregate(pipeline).allowDiskUse(true).iterator().next();    
			      } catch (Exception e) {
			        
			      }
			        
			mongoClient.close();

			if (null != docTmp) {
				responseStatus = new MspResponseStatus(9999, "Đã tồn tại giá trị thuế.");
				rsp.setResponseStatus(responseStatus);
				return rsp;
			}
			
			/* LUU DU LIEU HD */
			docUpsert = new Document("Code", code)
					.append("Name", name)
					.append("STT", stt)
					.append("IsDelete", false);
			/* END - LUU DU LIEU HD */

			mongoClient = cfg.mongoClient();
			collection = mongoClient.getDatabase(cfg.dbName).getCollection("TaxInvoice");
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
		

			docFind = new Document("_id", objectId);
			pipeline = new ArrayList<Document>();
			pipeline.add(
					new Document("$lookup", 
						new Document("from", "TaxInvoice")
						.append("pipeline", 
							Arrays.asList(
								new Document("$match", 
									new Document("_id", objectId)
								)
							)
						)
						.append("as", "TaxInvoice")
					)
				);
				pipeline.add(new Document("$unwind", new Document("path", "$TaxInvoice").append("preserveNullAndEmptyArrays", true)));
			

			mongoClient = cfg.mongoClient();
			collection = mongoClient.getDatabase(cfg.dbName).getCollection("TaxInvoice");
			  try {
			         docTmp = collection.aggregate(pipeline).allowDiskUse(true).iterator().next();      
			     } catch (Exception e) {
			        
			    }
			mongoClient.close();
			
			if (null == docTmp) {
				responseStatus = new MspResponseStatus(9999, "Không tìm thấy thông tin.");
				rsp.setResponseStatus(responseStatus);
				return rsp;
			}
	

					options = new FindOneAndUpdateOptions();
					options.upsert(false);
					options.maxTime(5000, TimeUnit.MILLISECONDS);
					options.returnDocument(ReturnDocument.AFTER);				
					
					mongoClient = cfg.mongoClient();
					collection = mongoClient.getDatabase(cfg.dbName).getCollection("TaxInvoice");
					collection.findOneAndUpdate(docFind,
							new Document("$set", 
									new Document("Name", name)
									.append("Code", code)	
									.append("STT", stt)
							),
						options
					);
					  mongoClient.close();
					
					responseStatus = new MspResponseStatus(0, "SUCCESS");
					rsp.setResponseStatus(responseStatus);
					return rsp;
				

		case Constants.MSG_ACTION_CODE.ACTIVE:
		case Constants.MSG_ACTION_CODE.DEACTIVE:
			/*KIEM TRA XEM _ID CO TON TAI KHONG*/
			if(!"".equals(_id)) {
				try {
					objectId = new ObjectId(_id);
				}catch(Exception e) {}
			}
			docFind = new Document("_id", objectId).append("IsDelete", new Document("$ne", true));
					
			pipeline = new ArrayList<Document>();
			pipeline.add(new Document("$match", docFind));
			mongoClient = cfg.mongoClient();
			collection = mongoClient.getDatabase(cfg.dbName).getCollection("TaxInvoice");
			  try {
			         docTmp = collection.aggregate(pipeline).allowDiskUse(true).iterator().next();      
			     } catch (Exception e) {
			        
			    }
			mongoClient.close();
			
			if(null == docTmp){
				responseStatus = new MspResponseStatus(16, Constants.MAP_ERROR.get(16));
				rsp.setResponseStatus(responseStatus);
				return rsp;
			}

			
			options = new FindOneAndUpdateOptions();
			options.upsert(false);
			options.maxTime(5000, TimeUnit.MILLISECONDS);
			options.returnDocument(ReturnDocument.AFTER);
			if(actionCode.equals(Constants.MSG_ACTION_CODE.ACTIVE)) {
					
				mongoClient = cfg.mongoClient();
				collection = mongoClient.getDatabase(cfg.dbName).getCollection("TaxInvoice");
				collection.findOneAndUpdate(docFind,
						new Document("$set", 
								new Document("IsActive", true)
								.append("InfoActive", 
										new Document("ActiveDate", LocalDateTime.now())
											.append("ActiveUserID", header.getUserId())
											.append("ActiveUserName", header.getUserName())
											.append("ActiveUserFullName", header.getUserFullName())
										)
							),
							options);
				  mongoClient.close();
			}else {
			
				mongoClient = cfg.mongoClient();
				collection = mongoClient.getDatabase(cfg.dbName).getCollection("TaxInvoice");
				collection.findOneAndUpdate(docFind,
						new Document("$set", 
								new Document("IsActive", false)
								.append("InfoDeActive", 
										new Document("DeActiveDate", LocalDateTime.now())
											.append("DeActiveUserID", header.getUserId())
											.append("DeActiveUserName", header.getUserName())
											.append("DeActiveUserFullName", header.getUserFullName())
										)
							),
							options);
				  mongoClient.close();
			}

			responseStatus = new MspResponseStatus(0, "SUCCESS");
			rsp.setResponseStatus(responseStatus);
			rsp.setObjData(docTmp.get("_id"));
			return rsp;
		case Constants.MSG_ACTION_CODE.APPROVE:
			/*KIEM TRA XEM _ID CO TON TAI KHONG*/
			if(!"".equals(_id)) {
				try {
					objectId = new ObjectId(_id);
				}catch(Exception e) {}
			}
			docFind = new Document("_id", objectId).append("IsDelete", new Document("$ne", true)).append("IsActive", true);
					
			pipeline = new ArrayList<Document>();
			pipeline.add(new Document("$match", docFind));
			mongoClient = cfg.mongoClient();
			collection = mongoClient.getDatabase(cfg.dbName).getCollection("TaxInvoice");
			  try {
			         docTmp = collection.aggregate(pipeline).allowDiskUse(true).iterator().next();      
			     } catch (Exception e) {
			        
			    }
			mongoClient.close();
			
			if(null == docTmp){
				responseStatus = new MspResponseStatus(16, Constants.MAP_ERROR.get(16));
				rsp.setResponseStatus(responseStatus);
				return rsp;
			}

			boolean activeFlag = false;
			if(null != docTmp.get("DefaultAdmin") && docTmp.get("DefaultAdmin") instanceof Boolean) {
				activeFlag = docTmp.getBoolean("DefaultAdmin", false);
			}
			
			if(activeFlag ==false) {
				
				Document docTmp1 = null;
				Document checkDoc = new Document("IsDelete", new Document("$ne", true)).append("IsActive", true).append("DefaultAdmin", true);
		
				pipeline = new ArrayList<Document>();
				pipeline.add(new Document("$match", checkDoc));
				mongoClient = cfg.mongoClient();
				collection = mongoClient.getDatabase(cfg.dbName).getCollection("TaxInvoice");
				  try {
					  docTmp1 = collection.aggregate(pipeline).allowDiskUse(true).iterator().next();      
				     } catch (Exception e) {
				        
				    }
				mongoClient.close();
				
				if(docTmp1 != null) {
					responseStatus = new MspResponseStatus(999, "Tham số mặc định là duy nhất. Vui lòng hủy tham số trước!!!");
					rsp.setResponseStatus(responseStatus);
					return rsp;
				}
			}
			
			
			options = new FindOneAndUpdateOptions();
			options.upsert(false);
			options.maxTime(5000, TimeUnit.MILLISECONDS);
			options.returnDocument(ReturnDocument.AFTER);		
				
			mongoClient = cfg.mongoClient();
			collection = mongoClient.getDatabase(cfg.dbName).getCollection("TaxInvoice");
			collection.findOneAndUpdate(docFind,
					new Document("$set", 
							new Document("DefaultAdmin", !activeFlag)
							.append("InfoDefaultAdmin", 
									new Document("DefaultAdminDate", LocalDateTime.now())
										.append("DefaultAdminUserID", header.getUserId())
										.append("DefaultAdminUserName", header.getUserName())
										.append("DefaultAdminUserFullName", header.getUserFullName())
									)
						),
						options);
			  mongoClient.close();
		

			responseStatus = new MspResponseStatus(0, "SUCCESS");
			rsp.setResponseStatus(responseStatus);
			rsp.setObjData(docTmp.get("_id"));
			return rsp;

///////////////////////////////////////////////////////////////////////////////////////
		case Constants.MSG_ACTION_CODE.DELETE:
			objectId = null;
			
			try {
				objectId = new ObjectId(_id);
			} catch (Exception ex) {
			}
			/* KIEM TRA THONG TIN HOPDONG CO TON TAI KHONG */
			docFind = new Document("_id", objectId);
		
			docTmp = null;
			pipeline = new ArrayList<Document>();
			pipeline.add(
					new Document("$lookup", 
						new Document("from", "TaxInvoice")
							.append("pipeline", 
								Arrays.asList(
									new Document("$match", 
										new Document("_id", objectId)	)
							)
							)
							.append("as", "TaxInvoice")
					)
				);
				pipeline.add(new Document("$unwind", new Document("path", "$TaxInvoice").append("preserveNullAndEmptyArrays", true)));
			
			mongoClient = cfg.mongoClient();
			collection = mongoClient.getDatabase(cfg.dbName).getCollection("TaxInvoice");
			  try {
			         docTmp = collection.aggregate(pipeline).allowDiskUse(true).iterator().next();      
			     } catch (Exception e) {
			        
			    }
			mongoClient.close();
			
			if (null == docTmp) {
				responseStatus = new MspResponseStatus(9999, "Không tìm thấy thông tin .");
				rsp.setResponseStatus(responseStatus);
				return rsp;
			}
	
				
			options = new FindOneAndUpdateOptions();
			options.upsert(false);
			options.maxTime(5000, TimeUnit.MILLISECONDS);
			options.returnDocument(ReturnDocument.AFTER);
			
			mongoClient = cfg.mongoClient();
			collection = mongoClient.getDatabase(cfg.dbName).getCollection("TaxInvoice");
			collection.findOneAndUpdate(docFind,
					new Document("$set", new Document("IsDelete", true)),
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
		
		 String code = null;
		 String name = null;
		
		JsonNode jsonData = null;
		if(objData != null) {
			jsonData = Json.serializer().nodeFromObject(objData);	
			 code = commons.getTextJsonNode(jsonData.at("/code")).replaceAll("\\s", "");
			 	name = commons.getTextJsonNode(jsonData.at("/name"));
		}

		MsgRsp rsp = new MsgRsp(header);
		MspResponseStatus responseStatus = null;
		
		ObjectId objectId = null;
		Document docTmp = null;
		List<Document> pipeline = new ArrayList<Document>();
		
		
		
		Document docMatch = new Document("IsDelete", new Document("$ne", true));
		if(!"".equals(code))
			docMatch.append("Code", commons.regexEscapeForMongoQuery(code));
		if(!"".equals(name))
			docMatch.append("Name", commons.regexEscapeForMongoQuery(name));
	
		
		pipeline = new ArrayList<Document>();
		pipeline.add(new Document("$match", docMatch));
		pipeline.add(
				new Document("$sort", 
					new Document("_id", -1)
				)
			);
		pipeline.addAll(createFacetForSearchNotSort(page));
		
		MongoClient mongoClient = cfg.mongoClient();
		MongoCollection<Document> collection = mongoClient.getDatabase(cfg.dbName).getCollection("TaxInvoice");
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
				hItem.put("Code", doc.get("Code"));
				hItem.put("Name", doc.get("Name"));
				hItem.put("STT", doc.get("STT"));
				hItem.put("IsActive", doc.get("IsActive"));		
				hItem.put("DefaultAdmin", doc.get("DefaultAdmin"));		
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

		List<Document> pipeline = null;
		pipeline = new ArrayList<Document>();
		pipeline.add(new Document("$match", docFind));
		
		MongoClient mongoClient = cfg.mongoClient();
		MongoCollection<Document> collection = mongoClient.getDatabase(cfg.dbName).getCollection("TaxInvoice");
		      try {
		        docTmp = collection.aggregate(pipeline).allowDiskUse(true).iterator().next();    
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

	
}
