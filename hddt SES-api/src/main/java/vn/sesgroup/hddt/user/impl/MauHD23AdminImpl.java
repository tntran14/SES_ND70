package vn.sesgroup.hddt.user.impl;

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
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.FindOneAndUpdateOptions;
import com.mongodb.client.model.ReturnDocument;

import vn.sesgroup.hddt.configuration.ConfigConnectMongo;
import vn.sesgroup.hddt.user.dao.AbstractDAO;
import vn.sesgroup.hddt.user.dao.MauHD23AdminDAO;
import vn.sesgroup.hddt.user.service.TCTNService;
import vn.sesgroup.hddt.utility.Constants;
import vn.sesgroup.hddt.utility.Json;

@Repository
@Transactional
public class MauHD23AdminImpl extends AbstractDAO implements MauHD23AdminDAO{
	private static final Logger log = LogManager.getLogger(MauHD23AdminImpl.class);
	@Autowired ConfigConnectMongo cfg;
	@Autowired TCTNService tctnService;
	@Autowired MongoTemplate mongoTemplate;

	
	@Transactional(rollbackFor = {Exception.class})
	@Override
	public MsgRsp crud(JSONRoot jsonRoot) throws Exception {
		Msg msg = jsonRoot.getMsg();
		MsgHeader header = msg.getMsgHeader();
		MsgPage page = msg.getMsgPage();
		Object objData = msg.getObjData();
		FindOneAndUpdateOptions options = null;
	
		
		JsonNode jsonData = null;
		if(objData != null) {
			jsonData = Json.serializer().nodeFromObject(msg.getObjData());
		}else{
			throw new Exception("Lỗi dữ liệu đầu vào");
		}
		
		String actionCode = header.getActionCode();
		String _id = commons.getTextJsonNode(jsonData.at("/_id")).replaceAll("\\s", "");
		String chude = commons.getTextJsonNode(jsonData.at("/Chude"));
		String file = commons.getTextJsonNode(jsonData.at("/File"));
		String tieude = commons.getTextJsonNode(jsonData.at("/Tieude"));
		String noidung = commons.getTextJsonNode(jsonData.at("/Noidung"));
		
		
	
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
			}catch(Exception ex) {}
			
			String pathDir = "";
			pathDir = "C:/hddt-ses/server/template/file";
			/*LUU DU LIEU HD*/
			docUpsert = new Document("Chude", chude)
						.append("File",file)
						.append("Tieude",tieude)
						.append("Noidung",noidung)
						.append("Dir", pathDir)
						.append("Date", LocalDate.now())
				.append("IsActive", false)
				.append("IsDelete", false)
				.append("InfoCreated", 
					new Document("CreateDate", LocalDateTime.now())
					.append("CreateUserID", header.getUserId())
					.append("CreateUserName", header.getUserName())
					.append("CreateUserFullName", header.getUserFullName())
				);
			/*END - LUU DU LIEU HD*/
			
			MongoClient mongoClient = cfg.mongoClient();
			MongoCollection<Document> collection = mongoClient.getDatabase(cfg.dbName).getCollection("HuongDanSD");
			collection.insertOne(docUpsert);      
			mongoClient.close();
	
			responseStatus = new MspResponseStatus(0, "SUCCESS");
			rsp.setResponseStatus(responseStatus);				
			return rsp;	
///////////////////////////////////////////////////////////////////////////////////////
		case Constants.MSG_ACTION_CODE.DELETE:
			objectId = null;
			try {
				objectId = new ObjectId(_id);
			}catch(Exception ex) {}
			/*KIEM TRA THONG TIN HOPDONG CO TON TAI KHONG*/
			docFind = new Document("_id",objectId);
			docTmp = null;
			
			pipeline = new ArrayList<Document>();
			pipeline.add(new Document("$match", docFind));
			mongoClient = cfg.mongoClient();
			collection = mongoClient.getDatabase(cfg.dbName).getCollection("HuongDanSD");
			      try {
			        docTmp = collection.aggregate(pipeline).allowDiskUse(true).iterator().next();    
			      } catch (Exception e) {
			        
			      }
			        
			mongoClient.close();
			
			if(null == docTmp) {
				responseStatus = new MspResponseStatus(9999, "Không tìm thấy thông tin hóa đơn.");
				rsp.setResponseStatus(responseStatus);
				return rsp;
			}

			options = new FindOneAndUpdateOptions();
			options.upsert(false);
			options.maxTime(5000, TimeUnit.MILLISECONDS);
			options.returnDocument(ReturnDocument.AFTER);
						
			mongoClient = cfg.mongoClient();
			collection = mongoClient.getDatabase(cfg.dbName).getCollection("HuongDanSD");
			collection.findOneAndUpdate(docFind,
					new Document("$set", 
							new Document("IsDelete", true)
							.append("InfoDeleted", 
									new Document("DeletedDate", LocalDateTime.now())
										.append("DeletedUserID", header.getUserId())
										.append("DeletedUserName", header.getUserName())
										.append("DeletedUserFullName", header.getUserFullName())
									)
						)
						, options);
			  mongoClient.close();
			
			responseStatus = new MspResponseStatus(0, "SUCCESS");
			rsp.setResponseStatus(responseStatus);				
			return rsp;
//////////////////////////////////////////////////////////////////////////////////////
		case Constants.MSG_ACTION_CODE.ACTIVE:
			objectId = null;
			try {
				objectId = new ObjectId(_id);
			}catch(Exception ex) {}
			/*KIEM TRA THONG TIN HOPDONG CO TON TAI KHONG*/
			docFind = new Document("_id",objectId);
			docTmp = null;
			
			pipeline = new ArrayList<Document>();
			pipeline.add(new Document("$match", docFind));
			mongoClient = cfg.mongoClient();
			collection = mongoClient.getDatabase(cfg.dbName).getCollection("HuongDanSD");
			      try {
			        docTmp = collection.aggregate(pipeline).allowDiskUse(true).iterator().next();    
			      } catch (Exception e) {
			        
			      }
			        
			mongoClient.close();
			
			if(null == docTmp) {
				responseStatus = new MspResponseStatus(9999, "Không tìm thấy thông tin hóa đơn.");
				rsp.setResponseStatus(responseStatus);
				return rsp;
			}

			options = new FindOneAndUpdateOptions();
			options.upsert(false);
			options.maxTime(5000, TimeUnit.MILLISECONDS);
			options.returnDocument(ReturnDocument.AFTER);
			
			mongoClient = cfg.mongoClient();
			collection = mongoClient.getDatabase(cfg.dbName).getCollection("HuongDanSD");
			collection.findOneAndUpdate(docFind,
					new Document("$set", 
							new Document("IsActive", true)
							.append("InfoDeleted", 
									new Document("DeletedDate", LocalDateTime.now())
										.append("DeletedUserID", header.getUserId())
										.append("DeletedUserName", header.getUserName())
										.append("DeletedUserFullName", header.getUserFullName())
									)
						)
						, options);
			  mongoClient.close();
			
			responseStatus = new MspResponseStatus(0, "SUCCESS");
			rsp.setResponseStatus(responseStatus);				
			return rsp;
////////////////////////////////////////////////////////////////////////////////////////
		case Constants.MSG_ACTION_CODE.DEACTIVE:
			objectId = null;
			try {
				objectId = new ObjectId(_id);
			}catch(Exception ex) {}
			/*KIEM TRA THONG TIN HOPDONG CO TON TAI KHONG*/
			docFind = new Document("_id",objectId);
			docTmp = null;

			pipeline = new ArrayList<Document>();
			pipeline.add(new Document("$match", docFind));
			mongoClient = cfg.mongoClient();
			collection = mongoClient.getDatabase(cfg.dbName).getCollection("HuongDanSD");
			  try {
			         docTmp = collection.aggregate(pipeline).allowDiskUse(true).iterator().next();      
			     } catch (Exception e) {
			        
			    }
			mongoClient.close();
			
			if(null == docTmp) {
				responseStatus = new MspResponseStatus(9999, "Không tìm thấy thông tin hóa đơn.");
				rsp.setResponseStatus(responseStatus);
				return rsp;
			}

			options = new FindOneAndUpdateOptions();
			options.upsert(false);
			options.maxTime(5000, TimeUnit.MILLISECONDS);
			options.returnDocument(ReturnDocument.AFTER);
			
			mongoClient = cfg.mongoClient();
			collection = mongoClient.getDatabase(cfg.dbName).getCollection("HuongDanSD");
			collection.findOneAndUpdate(docFind,
					new Document("$set", 
							new Document("IsActive", false)
							.append("InfoDeleted", 
									new Document("DeletedDate", LocalDateTime.now())
										.append("DeletedUserID", header.getUserId())
										.append("DeletedUserName", header.getUserName())
										.append("DeletedUserFullName", header.getUserFullName())
									)
						)
						, options);
			  mongoClient.close();
			
			responseStatus = new MspResponseStatus(0, "SUCCESS");
			rsp.setResponseStatus(responseStatus);				
			return rsp;
//////////////////////////////////////////////////////////////////////////////////////
		case Constants.MSG_ACTION_CODE.MODIFY:
			try {
				objectId = new ObjectId(_id);
			}catch(Exception ex) {}
			
			/*KIEM TRA THONG TIN KHACH HANG - USER - TINH THANH - CO QUAN THUE CO TON TAI KHONG*/
			pipeline = new ArrayList<Document>();
			docFind = new Document("_id", objectId).append("IsDelete", new Document("$ne", true));
			pipeline.add(
				new Document("$lookup", 
					new Document("from", "HuongDanSD")
					.append("pipeline", 
						Arrays.asList(
							new Document("$match", docFind)
						)
					)
					.append("as", "HuongDanSD")
				)
			);
			pipeline.add(new Document("$unwind", new Document("path", "$HuongDanSD").append("preserveNullAndEmptyArrays", true)));
			
			mongoClient = cfg.mongoClient();
			collection = mongoClient.getDatabase(cfg.dbName).getCollection("HuongDanSD");
			  try {
			         docTmp = collection.aggregate(pipeline).allowDiskUse(true).iterator().next();      
			     } catch (Exception e) {
			        
			    }
			mongoClient.close();
		
			if(null == docTmp) {
				responseStatus = new MspResponseStatus(9999, "Không tìm thấy thông tin .");
				rsp.setResponseStatus(responseStatus);
				return rsp;
			}
			
			/*END - LUU DU LIEU HD*/
	if(file != "") {
		options = new FindOneAndUpdateOptions();
		options.upsert(false);
		options.maxTime(5000, TimeUnit.MILLISECONDS);
		options.returnDocument(ReturnDocument.AFTER);
		
		mongoClient = cfg.mongoClient();
		collection = mongoClient.getDatabase(cfg.dbName).getCollection("HuongDanSD");
		collection.findOneAndUpdate(docFind,
				new Document("$set", 
						new Document("Chude", chude)
						.append("File",file)
						.append("Tieude",tieude)
						.append("Noidung",noidung)
						.append("Date", LocalDate.now())
				.append("IsActive", false)
				.append("IsDelete", false)
				.append("InfoUpdate", 
					new Document("UpdateDate", LocalDateTime.now())
					.append("UpdateUserID", header.getUserId())
					.append("UpdateUserName", header.getUserName())
					.append("UpdateUserFullName", header.getUserFullName())
				)
				), 
				options);
		  mongoClient.close();
			}
	else {
		options = new FindOneAndUpdateOptions();
		options.upsert(false);
		options.maxTime(5000, TimeUnit.MILLISECONDS);
		options.returnDocument(ReturnDocument.AFTER);	
		
		mongoClient = cfg.mongoClient();
		collection = mongoClient.getDatabase(cfg.dbName).getCollection("HuongDanSD");
		collection.findOneAndUpdate(docFind,
				new Document("$set", 
						new Document("Chude", chude)
				
						.append("Tieude",tieude)
						.append("Noidung",noidung)
						.append("Date", LocalDate.now())
				.append("IsActive", false)
				.append("IsDelete", false)
				.append("InfoUpdate", 
					new Document("UpdateDate", LocalDateTime.now())
					.append("UpdateUserID", header.getUserId())
					.append("UpdateUserName", header.getUserName())
					.append("UpdateUserFullName", header.getUserFullName())
				)
				), 
				options
			);
			
		  mongoClient.close();
	}
			
		

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
		
		ObjectId objectId = null;
		Document docTmp = null;
		List<Document> pipeline = new ArrayList<Document>();
		
		 String mst = "";
		 String name = "";
		 String mskh = "";
		 
		JsonNode jsonData = null;
		if(objData != null) {
			jsonData = Json.serializer().nodeFromObject(objData);	
			mst = commons.getTextJsonNode(jsonData.at("/MST")).replaceAll("\\s", "");
			mskh = commons.getTextJsonNode(jsonData.at("/MSKH")).replaceAll("\\s", "");
		}
		
		MsgRsp rsp = new MsgRsp(header);
		MspResponseStatus responseStatus = null;
		
		ObjectId issu = null;
		String issuerId = null;
		if(!mst.equals("")) {
			Document docTmp3 = null;
			Document findIssuer = new Document("TaxCode", mst).append("IsDelete", new Document("$ne", true));
			
			
			pipeline = new ArrayList<Document>();
			pipeline.add(new Document("$match", findIssuer));
			MongoClient mongoClient = cfg.mongoClient();
			MongoCollection<Document> collection = mongoClient.getDatabase(cfg.dbName).getCollection("Issuer");
			      try {
			    	  docTmp3 = collection.aggregate(pipeline).allowDiskUse(true).iterator().next();    
			      } catch (Exception e) {
			        
			      }
			        
			mongoClient.close();
			
			if(docTmp3==null) {
				responseStatus = new MspResponseStatus(9999, Constants.MAP_ERROR.get(9999));
				rsp.setResponseStatus(responseStatus);
				return rsp;
			}
			
			issu = docTmp3.getObjectId("_id");
			issuerId = issu.toString();
		}
		
		Document docMatch = new Document("IsDelete",new Document("$ne", true)).append("IsActive", true);
		
		if(!"".equals(mst))
			docMatch.append("IssuerId", commons.regexEscapeForMongoQuery(issuerId));
		if(!"".equals(mskh)) {
			docMatch.append("KHHDon", commons.regexEscapeForMongoQuery(mskh));
		}
		pipeline = new ArrayList<Document>();
		pipeline.add(new Document("$match", docMatch));
		pipeline.add(
				new Document("$sort", 
					new Document("_id", -1)
				)
			);
		pipeline.addAll(createFacetForSearchNotSort(page));

		MongoClient mongoClient = cfg.mongoClient();
		MongoCollection<Document> collection = mongoClient.getDatabase(cfg.dbName).getCollection("DMMauSoKyHieu");
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
				Document docTmp1 = null;
				
				String issuerid = doc.get("IssuerId").toString();
				ObjectId id_issuer = new ObjectId(issuerid);
				
				//Find ISSUERID
				
				Document findIssuer = new Document("_id", id_issuer).append("IsDelete", false);
				
				pipeline = new ArrayList<Document>();
				pipeline.add(new Document("$match", findIssuer));
				mongoClient = cfg.mongoClient();
				collection = mongoClient.getDatabase(cfg.dbName).getCollection("Issuer");
				      try {
				    	  docTmp1 = collection.aggregate(pipeline).allowDiskUse(true).iterator().next();    
				      } catch (Exception e) {
				        
				      }
				        
				mongoClient.close();
				
				String TaxCode = "";
				String Ten = "";
				if(docTmp1==null) {
					 TaxCode = "";
					 Ten = "";
					 
				}else {
				 TaxCode = docTmp1.get("TaxCode", "");
				 Ten = docTmp1.get("Name", "");
				}
				hItem = new HashMap<String, Object>();
				hItem.put("_id", objectId.toString());
				hItem.put("SHDHT", doc.get("SHDHT"));
				hItem.put("KHMSHDon", doc.get("KHMSHDon"));
				hItem.put("KHHDon", doc.get("KHHDon"));
				hItem.put("SoLuong", doc.get("SoLuong"));
				hItem.put("ConLai", doc.get("ConLai"));
				hItem.put("TaxCode",TaxCode);
				hItem.put("Name",Ten);
				hItem.put("DenSo", doc.get("DenSo"));			
				hItem.put("InfoPhatHanhNam23", doc.get("InfoPhatHanhNam23"));			
				rowsReturn.add(hItem);
			}
		}
		
		int total_ = docTmp.getInteger("total", 0);
		
		String check_total = String.valueOf(total_);
		
		responseStatus = new MspResponseStatus(0, check_total);
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
		}catch(Exception e) {}
		
		Document docFind = new Document("_id", objectId);
		Document docTmp = null;
		
		List<Document> pipeline = null;
		pipeline = new ArrayList<Document>();
		pipeline.add(new Document("$match", docFind));
		MongoClient mongoClient = cfg.mongoClient();
		MongoCollection<Document> collection = mongoClient.getDatabase(cfg.dbName).getCollection("HuongDanSD");
		      try {
		        docTmp = collection.aggregate(pipeline).allowDiskUse(true).iterator().next();    
		      } catch (Exception e) {
		        
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

	

	@Override
	public MsgRsp checkdb(JSONRoot jsonRoot) throws Exception {

		Msg msg = jsonRoot.getMsg();
		MsgHeader header = msg.getMsgHeader();
		MsgPage page = msg.getMsgPage();				
		MsgRsp rsp = new MsgRsp(header);
		MspResponseStatus responseStatus = null;

		Document docTmp = null;
		List<Document> pipeline = new ArrayList<Document>();
		
		Document docMatch = new Document("IsDelete",new Document("$ne", true)).append("IsActive", true);		
		pipeline = new ArrayList<Document>();
		pipeline.add(new Document("$match", docMatch));
		pipeline.add(
				new Document("$sort", 
					new Document("_id", -1)
				)
			);
		pipeline.addAll(createFacetForSearchNotSort(page));
		
		MongoClient mongoClient = cfg.mongoClient();
		MongoCollection<Document> collection = mongoClient.getDatabase(cfg.dbName).getCollection("DMMauSoKyHieu");
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
		
		int total = docTmp.getInteger("total", 0);
		
		String SLMSHT  = String.valueOf(total);
		
		responseStatus = new MspResponseStatus(0, SLMSHT);
		rsp.setResponseStatus(responseStatus);
		return rsp;
	}



	@Override
	public MsgRsp updatedb(JSONRoot jsonRoot) throws Exception {
		Msg msg = jsonRoot.getMsg();
		MsgHeader header = msg.getMsgHeader();
		MsgRsp rsp = new MsgRsp(header);
		MspResponseStatus responseStatus = null;
		Document docTmp = null;
		List<Document> pipeline = new ArrayList<Document>();
		FindOneAndUpdateOptions options = null;
		
		Document docMatch = new Document("ActiveFlag",new Document("$ne", false));
		pipeline = new ArrayList<Document>();
		pipeline.add(new Document("$match", docMatch));
		
		pipeline.add(new Document("$lookup", new Document("from", "DMMauSoKyHieu")
				.append("pipeline", Arrays.asList(
				new Document("$match",
						new Document("IsDelete", new Document("$ne", true)).append("IsActive", true))				
						)).append("as", "DMMauSoKyHieu")));
		
		
		MongoClient mongoClient = cfg.mongoClient();
		MongoCollection<Document> collection = mongoClient.getDatabase(cfg.dbName).getCollection("ApiLicenseKey");
		      try {
		        docTmp = collection.aggregate(pipeline).allowDiskUse(true).iterator().next();    
		      } catch (Exception e) {
		        
		      }
		        
		mongoClient.close();
			
		
		List<Document> mskh = null;
		if (docTmp.get("DMMauSoKyHieu") != null ) {
			mskh = docTmp.getList("DMMauSoKyHieu", Document.class);
		}
		
		int dem = mskh.size();
		ObjectId objectId = null;
			for(int i=0;i<dem;i++) {
				
				Document  checkms = null;
				checkms = mskh.get(i);
				
				ObjectId id_new = new ObjectId();
				
				objectId = (ObjectId) checkms.get("_id", ObjectId.class);
		
				String issuerid = checkms.get("IssuerId","");
				String KHMSHDon = checkms.get("KHMSHDon", "");
				String KHHDon_goc = checkms.get("KHHDon","");
				
				String KHHDon = KHHDon_goc.trim().replace("22", "23");
				int ConLai = checkms.getInteger("ConLai", 0);
				int DenSo = checkms.getInteger("DenSo", 0);
				int SoLuong = checkms.getInteger("SoLuong", 0);
				int namphathanh = 2023;
				
				Document templates = checkms.get("Templates", Document.class);
				String NamePhoi = checkms.getEmbedded(Arrays.asList("Templates", "Name"),"");	
				
				
				
				//UPDATE LOG NAM PHAT HANH
				Document findMSKH = new Document("_id", objectId).append("IsDelete", new Document("$ne", true));
				
				options = new FindOneAndUpdateOptions();
				options.upsert(false);
				options.maxTime(5000, TimeUnit.MILLISECONDS);
				options.returnDocument(ReturnDocument.AFTER);				
				
				mongoClient = cfg.mongoClient();
				collection = mongoClient.getDatabase(cfg.dbName).getCollection("DMMauSoKyHieu");
				collection.findOneAndUpdate(findMSKH,
						new Document("$set",
								new Document("InfoPhatHanhNam23",
								new Document("UpdatedDate", LocalDateTime.now())
												.append("SoLuong", ConLai)
												.append("UpdatedUserID", header.getUserId())
												.append("UpdatedUserName", header.getUserName())
												.append("UpdatedUserFullName", header.getUserFullName()))),
						options);
				  mongoClient.close();
				//END UPDATE
				
				//SAVE MSKH
				
				Document docUpsert1 = new Document("_id", id_new)
						.append("IssuerId", issuerid)
						.append("KHMSHDon", KHMSHDon)
						.append("KHHDon", KHHDon)
						.append("Templates", templates)
						.append("IsActive", true)
						.append("IsDelete", false)
						.append("NamPhatHanh", namphathanh)
						.append("SoLuong", ConLai)
						.append("TuSo", 1)
						.append("DenSo", ConLai)
						.append("ConLai", ConLai)
						.append("InfoCreated", 
						new Document("CreateDate", LocalDateTime.now())
						.append("CreateUserID", header.getUserId())
						.append("CreateUserName", header.getUserName())
						.append("CreateUserFullName", header.getUserFullName())
					);
				
				mongoClient = cfg.mongoClient();
				collection = mongoClient.getDatabase(cfg.dbName).getCollection("DMMauSoKyHieu");
				collection.insertOne(docUpsert1);      
				mongoClient.close();
				
				/* END - SAVE MSKH */
				
				//SAVE COLECTION QUANTITY 
				String MSSKHieu = id_new.toString();
				Document docUpsert = new Document("IssuerId", issuerid)
						.append("STT", 1)
						.append("LanPH", 1)
						.append("MSKHieu", MSSKHieu)
						.append("KHMSHDon", KHMSHDon)
						.append("KHHDon", KHHDon)
						.append("NamePhoi", NamePhoi)
						.append("SoLuong", SoLuong)
						.append("TuSo", 1)
						.append("DenSo", DenSo)
						.append("GChu", "")
						.append("NLap", LocalDate.now());
				/* END - LUU DU LIEU HD */

				mongoClient = cfg.mongoClient();
				collection = mongoClient.getDatabase(cfg.dbName).getCollection("DMQuantity");
				collection.insertOne(docUpsert);      
				mongoClient.close();
				
				//END SAVE QUANTITY
			
				
			}
		
		responseStatus = new MspResponseStatus(0, "SUCCESS");
		rsp.setResponseStatus(responseStatus);
		return rsp;
	}

	public MsgRsp updateDBTheoNam(JSONRoot jsonRoot) throws Exception {
		Msg msg = jsonRoot.getMsg();
		MsgHeader header = msg.getMsgHeader();
		MsgRsp rsp = new MsgRsp(header);
		MspResponseStatus responseStatus = null;
		Document docTmp = null;
		List<Document> pipeline = new ArrayList<Document>();
		FindOneAndUpdateOptions options = null;
		
		Iterable<Document> cursor = null;
		Iterator<Document> iter = null;
		
		Object objData = msg.getObjData();

		JsonNode jsonData = null;
		if (objData != null) {
			jsonData = Json.serializer().nodeFromObject(msg.getObjData());
		} else {
			throw new Exception("Lỗi dữ liệu đầu vào");
		}
		
		String NamCanChuyenDoi = commons.getTextJsonNode(jsonData.at("/NamCanChuyenDoi")).replaceAll("\\s", "");
		String NamChuyenDoi = commons.getTextJsonNode(jsonData.at("/NamChuyenDoi")).trim().replaceAll("\\s", "");
		
		int CheckNamChuyenDoi = commons.ToNumberInt(NamChuyenDoi);
		int checkNamLienTruoc = CheckNamChuyenDoi - 1;
		
		int CheckNamCanChuyenDoi = commons.ToNumberInt(NamCanChuyenDoi);
		int checkNamLienSau = CheckNamCanChuyenDoi + 1;
		
		Document docMatch = new Document("ActiveFlag",new Document("$ne", false));
		pipeline = new ArrayList<Document>();
		pipeline.add(new Document("$match", docMatch));
		
		pipeline.add(new Document("$lookup", new Document("from", "DMMauSoKyHieu")
				.append("pipeline", Arrays.asList(
				new Document("$match",
						new Document("IsDelete", new Document("$ne", true))
						.append("IsActive", true)
						.append("NamPhatHanh", commons.ToNumberInt(NamCanChuyenDoi))
						)				
						)).append("as", "DMMauSoKyHieu")));
		
		
		//CHECK NAM LIEN TRUOC CHUYEN DOI 
		pipeline.add(new Document("$lookup", new Document("from", "DMMauSoKyHieu")
				.append("pipeline", Arrays.asList(
				new Document("$match",
						new Document("IsDelete", new Document("$ne", true))
						.append("IsActive", true)
						.append("NamPhatHanh", checkNamLienTruoc)
						),
						new Document("$count", "pcount")
						)).append("as", "DMMauSoKyHieuNamLienTruoc")));
		
		
		//CHECK NAM LIEN SAU CAN CHUYEN DOI 
		pipeline.add(new Document("$lookup", new Document("from", "DMMauSoKyHieu")
				.append("pipeline", Arrays.asList(
				new Document("$match",
						new Document("IsDelete", new Document("$ne", true))
						.append("IsActive", true)
						.append("NamPhatHanh", checkNamLienSau)
						),
						new Document("$count", "pcount")
						)).append("as", "DMMauSoKyHieuNamLienSau")));
		
		//CHECK NAM CAN CHUYEN 
		pipeline.add(new Document("$lookup", new Document("from", "DMMauSoKyHieu")
				.append("pipeline", Arrays.asList(
				new Document("$match",
						new Document("IsDelete", new Document("$ne", true))
						.append("IsActive", true)
						.append("NamPhatHanh", commons.ToNumberInt(NamChuyenDoi))
						),
				new Document("$count", "pcount")
						)).append("as", "DMMauSoKyHieuNamChuyenDoi")));
		
		cursor = mongoTemplate.getCollection("ApiLicenseKey").aggregate(pipeline);
		iter = cursor.iterator();
		if (iter.hasNext()) {
			docTmp = iter.next();
		}
		
//		MongoClient mongoClient = cfg.mongoClient();
//		MongoCollection<Document> collection = mongoClient.getDatabase(cfg.dbName).getCollection("ApiLicenseKey");
//		      try {
//		        docTmp = collection.aggregate(pipeline).allowDiskUse(true).iterator().next();    
//		      } catch (Exception e) {
//		        
//		      }
//		        
//		mongoClient.close();
		
		if (docTmp.getList("DMMauSoKyHieu",  Document.class).size() == 0 ) {			
			responseStatus = new MspResponseStatus(999, "Không tìm thấy thông tin mẫu số năm cần chuyển đổi. Vui lòng kiểm tra lại!!!");
			rsp.setResponseStatus(responseStatus);
			return rsp;
		}
		
		if(docTmp.getList("DMMauSoKyHieuNamLienTruoc", Document.class).size() == 0) {
			responseStatus = new MspResponseStatus(999, "Năm chuyển đổi không hợp lệ!!!");
			rsp.setResponseStatus(responseStatus);
			return rsp;			
		}
		
		if(docTmp.getList("DMMauSoKyHieuNamLienSau", Document.class).size() != 0) {
			responseStatus = new MspResponseStatus(999, "Năm cần chuyển đổi đã được cập nhật!!!");
			rsp.setResponseStatus(responseStatus);
			return rsp;			
		}
		
		if(docTmp.getList("DMMauSoKyHieuNamChuyenDoi", Document.class).size() != 0) {
			responseStatus = new MspResponseStatus(999, "Năm chuyển đổi đã được cập nhật. Vui lòng kiểm tra lại!!!");
			rsp.setResponseStatus(responseStatus);
			return rsp;
		}
		
	
		List<Document> mskh = null;
		if (docTmp.get("DMMauSoKyHieu") != null ) {
			mskh = docTmp.getList("DMMauSoKyHieu", Document.class);
		}
		
		
		String kyTuCuoiNamCanChuyenDoi = NamCanChuyenDoi.substring(NamCanChuyenDoi.length() - 2);
		String kyTuCuoiNamChuyenDoi = NamChuyenDoi.substring(NamChuyenDoi.length() - 2);
		
		ObjectId objectId = null;
			for(int i=0; i< mskh.size() ;i++) {
				
				Document checkms = null;
				checkms = mskh.get(i);
				
				ObjectId id_new = new ObjectId();
				
				objectId = (ObjectId) checkms.get("_id", ObjectId.class);
		
				String issuerId = checkms.get("IssuerId","");
				String KHMSHDon = checkms.get("KHMSHDon", "");
				String KHHDon_goc = checkms.get("KHHDon","");
			
				
				String KHHDon = KHHDon_goc.trim().replace(kyTuCuoiNamCanChuyenDoi, kyTuCuoiNamChuyenDoi);
				int ConLai = checkms.getInteger("ConLai", 0);
//				int DenSo = checkms.getInteger("DenSo", 0);
				int SoLuong = checkms.getInteger("SoLuong", 0);
				
				Document templates = checkms.get("Templates", Document.class);
				String NamePhoi = checkms.getEmbedded(Arrays.asList("Templates", "Name"),"");	
						
				//SAVE MSKH
				
				Document docUpsert1 = new Document("_id", id_new)
						.append("IssuerId", issuerId)
						.append("KHMSHDon", KHMSHDon)
						.append("KHHDon", KHHDon)
						.append("Templates", templates)
						.append("IsActive", true)
						.append("IsDelete", false)
						.append("NamPhatHanh", commons.ToNumberInt(NamChuyenDoi))
						.append("SoLuong", ConLai)
						.append("TuSo", 1)
						.append("DenSo", ConLai)
						.append("ConLai", ConLai)
						.append("SHDHT", 0)
						.append("InfoCreated", 
						new Document("CreateDate", LocalDateTime.now())
						.append("CreateUserID", header.getUserId())
						.append("CreateUserName", header.getUserName())
						.append("CreateUserFullName", header.getUserFullName())
					);
				
//				mongoClient = cfg.mongoClient();
//				collection = mongoClient.getDatabase(cfg.dbName).getCollection("DMMauSoKyHieu");
//				collection.insertOne(docUpsert1);      
//				mongoClient.close();
				
				options = new FindOneAndUpdateOptions();
				options.upsert(false);
				options.maxTime(5000, TimeUnit.MILLISECONDS);
				options.returnDocument(ReturnDocument.AFTER);
				mongoTemplate.getCollection("DMMauSoKyHieu").insertOne(docUpsert1);
				
				/* END - SAVE MSKH */
				
				//UPDATE LOG NAM PHAT HANH
				Document findMSKH = new Document("_id", objectId).append("IsDelete", new Document("$ne", true));
				
				String InfoPhatHanhTheoNam = "InfoPhatHanhNam" + kyTuCuoiNamChuyenDoi;
				
				options = new FindOneAndUpdateOptions();
				options.upsert(false);
				options.maxTime(5000, TimeUnit.MILLISECONDS);
				options.returnDocument(ReturnDocument.AFTER);				
				
//				int SLconLaiHDonCu = SoLuong - ConLai;
				
				mongoTemplate.getCollection("DMMauSoKyHieu").findOneAndUpdate(findMSKH,
						new Document("$set",
//								new Document("SoLuong", SLconLaiHDonCu)
//								.append("DenSo", SLconLaiHDonCu)
//								.append("SHDHT", SLconLaiHDonCu)
//								.append("ConLai", 0)
								new Document(InfoPhatHanhTheoNam,
								new Document("UpdatedDate", LocalDateTime.now())
//												.append("SoLuongDaDung", SLconLaiHDonCu)
												.append("SoLuongConLai", ConLai)
												.append("UpdatedUserID", header.getUserId())
												.append("UpdatedUserName", header.getUserName())
												.append("UpdatedUserFullName", header.getUserFullName()))),
						options);
				
//				mongoClient = cfg.mongoClient();
//				collection = mongoClient.getDatabase(cfg.dbName).getCollection("DMMauSoKyHieu");
//				collection.findOneAndUpdate(findMSKH,
//						new Document("$set",
//								new Document("ConLai", 0)
//								.append("SHDHT", SoLuong)
//								.append(InfoPhatHanhTheoNam,
//								new Document("UpdatedDate", LocalDateTime.now())
//												.append("SoLuong", ConLai)
//												.append("UpdatedUserID", header.getUserId())
//												.append("UpdatedUserName", header.getUserName())
//												.append("UpdatedUserFullName", header.getUserFullName()))),
//						options);
//				  mongoClient.close();
				//END UPDATE
				
			
				
				//SAVE COLECTION QUANTITY 
				String MSSKHieu = id_new.toString();
				Document docUpsert = new Document("IssuerId", issuerId)
						.append("STT", 1)
						.append("LanPH", 1)
						.append("MSKHieu", MSSKHieu)
						.append("KHMSHDon", KHMSHDon)
						.append("KHHDon", KHHDon)
						.append("NamePhoi", NamePhoi)
						.append("SoLuong", ConLai)
						.append("TuSo", 1)
						.append("DenSo", ConLai)
						.append("GChu", "")
						.append("NLap", LocalDate.now());
				/* END - LUU DU LIEU HD */

//				mongoClient = cfg.mongoClient();
//				collection = mongoClient.getDatabase(cfg.dbName).getCollection("DMQuantity");
//				collection.insertOne(docUpsert);      
//				mongoClient.close();
				
				
				options = new FindOneAndUpdateOptions();
				options.upsert(false);
				options.maxTime(5000, TimeUnit.MILLISECONDS);
				options.returnDocument(ReturnDocument.AFTER);
				mongoTemplate.getCollection("DMQuantity").insertOne(docUpsert);
				
				//END SAVE QUANTITY				
			}
		
		responseStatus = new MspResponseStatus(0, "SUCCESS");
		rsp.setResponseStatus(responseStatus);
		return rsp;
	}

}
