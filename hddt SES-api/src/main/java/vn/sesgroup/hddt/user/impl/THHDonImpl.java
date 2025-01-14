package vn.sesgroup.hddt.user.impl;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
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
import vn.sesgroup.hddt.user.dao.THHDonDAO;
import vn.sesgroup.hddt.user.service.JPUtils;
import vn.sesgroup.hddt.user.service.TCTNService;
import vn.sesgroup.hddt.utility.Constants;
import vn.sesgroup.hddt.utility.Json;

@Repository
@Transactional
public class THHDonImpl extends AbstractDAO implements THHDonDAO {
	private static final Logger log = LogManager.getLogger(THHDonImpl.class);
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

		String mst = commons.getTextJsonNode(jsonData.at("/MST")).replaceAll("\\s", "");
		ObjectId objectId = null;
		Document docTmp = null;
		Document docTmp1 = null;

		List<Document> pipeline = new ArrayList<Document>();

		//CHECK ISSUER TON TAI
 		Document docMatch = new Document("TaxCode", mst)
				.append("IsDelete", new Document("$ne", true));		
		
		MongoClient mongoClient = cfg.mongoClient();
		MongoCollection<Document> collection = mongoClient.getDatabase(cfg.dbName).getCollection("Issuer");
		      try {
		        docTmp = collection.find(docMatch).allowDiskUse(true).iterator().next();    
		      } catch (Exception e) {
		        
		      }
		        
		mongoClient.close();
		
		if(docTmp==null) {
			responseStatus = new MspResponseStatus(999, "Mã số thuế không tồn tại.");
			rsp.setResponseStatus(responseStatus);
			return rsp;	
		}	
		//END CHECK ISSUER TON TAI
		
		//CHECK MAU SO KY HIEU DUA VAO ISSUER 
		String issuer_id= docTmp.getObjectId("_id").toString();
		
		Document docMSKH = new Document("IssuerId", issuer_id)
				.append("IsDelete", new Document("$ne", true));
		
		
		pipeline = new ArrayList<Document>();
		pipeline.add(new Document("$match", docMSKH));
		pipeline.add(new Document("$project", new Document("_id", 1).append("KHMSHDon", 1).append("KHHDon", 1)
				.append("SoLuong", 1).append("TuSo", 1).append("DenSo", 1).append("ConLai", 1)
				.append("Status", 1).append("SHDTH", 1)
				));	
		
		pipeline.addAll(createFacetForSearchNotSort(page));
		
		Iterator<Document> iter1 = null;
		
		mongoClient = cfg.mongoClient();
		collection = mongoClient.getDatabase(cfg.dbName).getCollection("DMMauSoKyHieu");
		
		try {
			iter1 = collection.aggregate(pipeline).allowDiskUse(true).iterator();
		} catch (Exception e) {

		}
		mongoClient.close();
		
		if (iter1.hasNext()) {
			docTmp1 = iter1.next();		
			
			page.setTotalRows(docTmp1.getInteger("total", 0));
			rsp.setMsgPage(page);
			List<Document> rows = null;
			if (docTmp1.get("data") != null && docTmp1.get("data") instanceof List) {
				rows = docTmp1.getList("data", Document.class);
			}
			
			ArrayList<HashMap<String, Object>> rowsReturn = new ArrayList<HashMap<String, Object>>();
			HashMap<String, Object> hItem = null;
			if (null != rows) {
				for (Document doc : rows) {
					int tong = doc.getInteger("SoLuong", 0);
					int conlai = doc.getInteger("ConLai", 0);
					int dadung = tong - conlai;
					objectId = (ObjectId) doc.get("_id");
					hItem = new HashMap<String, Object>();
					hItem.put("_id", objectId.toString());
					hItem.put("KHMSHDon", doc.get("KHMSHDon"));
					hItem.put("KHHDon", doc.get("KHHDon"));
					hItem.put("SoLuong", tong);
					hItem.put("TuSo", doc.get("TuSo"));
					hItem.put("DenSo", doc.get("DenSo"));
					hItem.put("ConLai",conlai);		
					hItem.put("DaDung", dadung);	
					hItem.put("Status", doc.get("Status"));
					hItem.put("SHDTH", doc.get("SHDTH"));
					rowsReturn.add(hItem);
				}
			}
			responseStatus = new MspResponseStatus(0, "SUCCESS");			
			rsp.setResponseStatus(responseStatus);

			HashMap<String, Object> mapDataR = new HashMap<String, Object>();
			mapDataR.put("rows", rowsReturn);
			rsp.setObjData(mapDataR);
			return rsp;
		//END CHECK MAU SO KY HIEU DUA VAO ISSUER
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
		ObjectId objectId = null;
		FindOneAndUpdateOptions options = null;
		FindOneAndUpdateOptions options1 = null;
		FindOneAndUpdateOptions options2 = null;
		List<Document> pipeline = null;
		Document docTmp = null;
		
		JsonNode jsonData = null;
		if (objData != null) {
			jsonData = Json.serializer().nodeFromObject(msg.getObjData());
		} else {
			throw new Exception("Lỗi dữ liệu đầu vào");
		}
		
		MsgRsp rsp = new MsgRsp(header);
		rsp.setMsgPage(page);
		MspResponseStatus responseStatus = null;

		String actionCode = header.getActionCode();
		
		String _id = commons.getTextJsonNode(jsonData.at("/_id")).replaceAll("\\s", "");
		String shd_th = commons.getTextJsonNode(jsonData.at("/SHD_TH")).replaceAll("\\s", "");
		
		objectId = new ObjectId(_id);
		
		switch (actionCode) {
		case Constants.MSG_ACTION_CODE.MODIFY:
			
		Document docFind = new Document("_id", objectId)
				.append("IsDelete", new Document("$ne", true));
		
		MongoClient mongoClient = cfg.mongoClient();
		MongoCollection<Document> collection = mongoClient.getDatabase(cfg.dbName).getCollection("DMMauSoKyHieu");
		      try {
		        docTmp = collection.find(docFind).allowDiskUse(true).iterator().next();    
		      } catch (Exception e) {
		        
		      }
		        
		mongoClient.close();
		
		if(docTmp==null) {
			responseStatus = new MspResponseStatus(999, "Không tìm thấy mẫu số ký hiệu.");
			rsp.setResponseStatus(responseStatus);
			return rsp;	
		}
		
		
		int SHDTH = Integer.parseInt(shd_th);
		String issuer_id = docTmp.getString("IssuerId");
		ObjectId id_taxcode = new ObjectId(issuer_id);
		Document findTaxcode = new Document("_id", id_taxcode);
		
		String KHHDon = docTmp.get("KHHDon", "");
		String KHMSHDon = docTmp.get("KHMSHDon", "");		
		String namePhoi = docTmp.getEmbedded(Arrays.asList("Templates", "Name"), "");			
		int SL_MS = docTmp.get("SoLuong", 0);
		int TS_MS = docTmp.get("TuSo", 0);
		int DS_MS = docTmp.get("DenSo", 0);
		int CL_MS = docTmp.get("ConLai", 0);
		
		
		int SL_QUANTITY = SL_MS - SHDTH;
		int TS_QUANTITY = TS_MS;
		int DS_QUANTITY = DS_MS - SHDTH;
		
		
		if(SHDTH>CL_MS) {
			responseStatus = new MspResponseStatus(999, "Số hóa đơn không đủ để thu hồi.");
			rsp.setResponseStatus(responseStatus);
			return rsp;
		}
		
		//TRU TRONG QUANTITY
		Document docTmp2 = null;
		pipeline = new ArrayList<Document>();
		pipeline.add(new Document("$match",findTaxcode));
		pipeline.add(new Document("$lookup",
				new Document("from", "DMQuantity").append("pipeline", Arrays.asList(
						new Document("$match",
								new Document("IssuerId", issuer_id)
								.append("KHMSHDon", KHMSHDon)
								.append("KHHDon", KHHDon)
								),
						new Document("$sort", new Document("NLap", -1).append("_id",-1)),
						new Document("$project", new Document("_id", 1).append("SoLuong", 1).append("DenSo", 1)),
						new Document("$limit", 1)))
				.append("as", "DMQuantity"))
		);
		pipeline.add(
				new Document("$unwind", new Document("path", "$DMQuantity").append("preserveNullAndEmptyArrays", true)));
			
		mongoClient = cfg.mongoClient();
		collection = mongoClient.getDatabase(cfg.dbName).getCollection("Issuer");
		  try {
			  docTmp2 = collection.aggregate(pipeline).allowDiskUse(true).iterator().next();      
		     } catch (Exception e) {
		        
		    }
		mongoClient.close();
		
		String id_issuer = docTmp2.get("_id", ObjectId.class).toString();
		
		if (docTmp.get("DMQuantity") == null) {
			ObjectId id_quantity = new ObjectId();
				
			 LocalDate date = LocalDate.now();
			 DateTimeFormatter formatters = DateTimeFormatter.ofPattern("dd/MM/yyyy");
			 String ngayLap = date.format(formatters);
						
			Document docQuantity = new Document("_id", id_quantity)
					.append("IssuerId", id_issuer)
					.append("STT", "1")					
					.append("LanPH", "1")       
					.append("MSKHieu", _id)
					.append("KHMSHDon", KHMSHDon)
					.append("KHHDon", KHHDon)
					.append("NamePhoi", namePhoi)
					.append("SoLuong", SL_QUANTITY)
					.append("TuSo", TS_QUANTITY)
					.append("DenSo", DS_QUANTITY)
					.append("GChu", "")
					.append("NLap", commons.convertStringToLocalDate(ngayLap, Constants.FORMAT_DATE.FORMAT_DATE_WEB));
			/* END - LUU DU LIEU HD */
	
			mongoClient = cfg.mongoClient();
			collection = mongoClient.getDatabase(cfg.dbName).getCollection("DMQuantity");
			collection.insertOne(docQuantity);      
			mongoClient.close();
			
		}else {
			ObjectId id_quantity = docTmp2.getEmbedded(Arrays.asList("DMQuantity", "_id"), ObjectId.class);
			int SL_Quantity =  docTmp2.getEmbedded(Arrays.asList("DMQuantity", "SoLuong"), 0);
			int DS_Quantity =  docTmp2.getEmbedded(Arrays.asList("DMQuantity", "DenSo"), 0);
			
			int SL_DMQUANTITY = SL_Quantity - SHDTH;
			int DS_DMQUANTITY = DS_Quantity	- SHDTH;	
			
			options1 = new FindOneAndUpdateOptions();
			options1.upsert(false);
			options1.maxTime(5000, TimeUnit.MILLISECONDS);
			options1.returnDocument(ReturnDocument.AFTER);

			Document find_quantity = new Document("_id", id_quantity);
			
			mongoClient = cfg.mongoClient();
			collection = mongoClient.getDatabase(cfg.dbName).getCollection("DMQuantity");
			collection.findOneAndUpdate(find_quantity,
					new Document("$set",
							new Document("SoLuong", SL_DMQUANTITY)	
							.append("DenSo", DS_DMQUANTITY)
							
							),
					options1);	
			  mongoClient.close();
		}
		
		
		
		//END QUANTITY
		
		//TRA VE KHO
		Document docTmp3 = null;	
		
		mongoClient = cfg.mongoClient();
		collection = mongoClient.getDatabase(cfg.dbName).getCollection("Issuer");
		  try {
			  docTmp3 = collection.find(findTaxcode).allowDiskUse(true).iterator().next();      
		     } catch (Exception e) {
		        
		    }
		mongoClient.close();
		
		String taxcode = docTmp3.get("TaxCode", "");
		
				Document docTmp4 = null;			
				Document findKho = new Document("TaxCode",taxcode);
				
				mongoClient = cfg.mongoClient();
				collection = mongoClient.getDatabase(cfg.dbName).getCollection("DMDepot");
				  try {
					  docTmp4 = collection.find(findKho).allowDiskUse(true).iterator().next();      
				     } catch (Exception e) {
				        
				    }
				mongoClient.close();
				
				
				if(docTmp4 == null) {
					ObjectId id_kho = new ObjectId();
					Document docKho = new Document("_id", id_kho)
							.append("TaxCode", taxcode)
							.append("SLHDon", SHDTH)					
							.append("SLHDonDD", 0)
							.append("SLHDonCL", SHDTH)
							.append("SHDTH", SHDTH)
							.append("IsRoot", false)
							.append("IsDelete", false)
							.append("InfoCreated",
									new Document("CreateDate", LocalDateTime.now()).append("CreateUserID", header.getUserId())
											.append("CreateUserName", header.getUserName())
											.append("CreateUserFullName", header.getUserFullName()));
						
					/* END - LUU DU LIEU HD */
			
					mongoClient = cfg.mongoClient();
					collection = mongoClient.getDatabase(cfg.dbName).getCollection("DMDepot");
					collection.insertOne(docKho);      
					mongoClient.close();
					
				}else {
//					int SLHDon =  docTmp4.get("SLHDon",0);
					int SLHDonCL =  docTmp4.get("SLHDonCL", 0);
//					int SLHDonTH =  docTmp4.get("SHDTH", 0);
					int SLHDonDD =  docTmp4.get("SLHDonDD", 0);
					
//					int SLHDon_KHO = SLHDon + SHDTH;
					int SLHDonCL_KHO = SLHDonCL	+ SHDTH;	
					int SLHDonDD_KHO = SLHDonDD - SHDTH;
					
					options2 = new FindOneAndUpdateOptions();
					options2.upsert(false);
					options2.maxTime(5000, TimeUnit.MILLISECONDS);
					options2.returnDocument(ReturnDocument.AFTER);

					mongoClient = cfg.mongoClient();
					collection = mongoClient.getDatabase(cfg.dbName).getCollection("DMDepot");
					collection.findOneAndUpdate(findKho,
							new Document("$set",
									new Document("SLHDonCL", SLHDonCL_KHO)	
									.append("SLHDonDD", SLHDonDD_KHO)
									),
							options2);
					  mongoClient.close();
						
				}
								
				//END TRU KHO
		
		int SHD_DATH = docTmp.get("SHDTH", 0);
		
		
		//SET QUANTITY
		int TSL = SL_MS - SHDTH;
		int CL = CL_MS - SHDTH;
		int DS = DS_MS - SHDTH;
		int TH = SHD_DATH + SHDTH;
		
		options = new FindOneAndUpdateOptions();
		options.upsert(false);
		options.maxTime(5000, TimeUnit.MILLISECONDS);
		options.returnDocument(ReturnDocument.AFTER);
		
		mongoClient = cfg.mongoClient();
		collection = mongoClient.getDatabase(cfg.dbName).getCollection("DMMauSoKyHieu");
		collection.findOneAndUpdate(docFind,
				new Document("$set",
						new Document("SoLuong", TSL)	
						.append("ConLai",CL)															
						.append("DenSo", DS)
						.append("SHDTH", TH)	
						),
				options);
		  mongoClient.close();
		  
		responseStatus = new MspResponseStatus(0, "SUCCESS");
		rsp.setResponseStatus(responseStatus);
		return rsp;
		
		case Constants.MSG_ACTION_CODE.ACTIVE:
			
		 docFind = new Document("_id", objectId)
		.append("IsDelete", new Document("$ne", true));
		
		mongoClient = cfg.mongoClient();
		collection = mongoClient.getDatabase(cfg.dbName).getCollection("DMMauSoKyHieu");
		  try {
		         docTmp = collection.find(docFind).allowDiskUse(true).iterator().next();      
		     } catch (Exception e) {
		        
		    }
		mongoClient.close();
		
		if(docTmp==null) {
			responseStatus = new MspResponseStatus(999, "Không tìm thấy mẫu số ký hiệu.");
			rsp.setResponseStatus(responseStatus);
			return rsp;	
		}
		
		boolean isActive = false;
		if(null != docTmp.get("Status") && docTmp.get("Status") instanceof Boolean) {
			isActive = docTmp.getBoolean("Status", false);
		}
		
		options = new FindOneAndUpdateOptions();
		options.upsert(false);
		options.maxTime(5000, TimeUnit.MILLISECONDS);
		options.returnDocument(ReturnDocument.AFTER);
		 
		 mongoClient = cfg.mongoClient();
			collection = mongoClient.getDatabase(cfg.dbName).getCollection("DMMauSoKyHieu");
			collection.findOneAndUpdate(docFind,
					new Document("$set",
							new Document("Status", !isActive)	
							),
					options);
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
		} catch (Exception e) {
		}

		Document docFind = new Document("IsDelete", new Document("$ne", true))
				.append("_id", objectId);

		Document docTmp = null;
	
		MongoClient mongoClient = cfg.mongoClient();
		MongoCollection<Document> collection = mongoClient.getDatabase(cfg.dbName).getCollection("DMMauSoKyHieu");
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

}
