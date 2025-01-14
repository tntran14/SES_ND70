package vn.sesgroup.hddt.user.impl;

import java.io.File;
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
import vn.sesgroup.hddt.user.dao.PhoiHDonDao;
import vn.sesgroup.hddt.user.service.TCTNService;
import vn.sesgroup.hddt.utility.Constants;
import vn.sesgroup.hddt.utility.Json;

@Repository
@Transactional
public class QLPhoiHDonImpl extends AbstractDAO implements PhoiHDonDao {
	private static final Logger log = LogManager.getLogger(QLPhoiHDonImpl.class);
	@Autowired ConfigConnectMongo cfg;
	@Autowired TCTNService tctnService;


	@Transactional(rollbackFor = { Exception.class })
	@Override
	public MsgRsp crud(JSONRoot jsonRoot) throws Exception {
		Msg msg = jsonRoot.getMsg();
		MsgHeader header = msg.getMsgHeader();
		MsgPage page = msg.getMsgPage();
		Object objData = msg.getObjData();
		MsgRsp rsp = new MsgRsp(header);
		rsp.setMsgPage(page);
		MspResponseStatus responseStatus = null;
		FindOneAndUpdateOptions options = null;

		JsonNode jsonData = null;
		if (objData != null) {
			jsonData = Json.serializer().nodeFromObject(msg.getObjData());
		} else {
			throw new Exception("Lỗi dữ liệu đầu vào");
		}

		String actionCode = header.getActionCode();
		String _id = commons.getTextJsonNode(jsonData.at("/_id")).replaceAll("\\s", "");
		String loaihd = commons.getTextJsonNode(jsonData.at("/LoaiHD")).trim().replaceAll("\\s+", " ");
		String name = commons.getTextJsonNode(jsonData.at("/Name"));
		String FileNameSystem = commons.getTextJsonNode(jsonData.at("/FileNameSystem"));
		String ImgFileNameSystem = commons.getTextJsonNode(jsonData.at("/ImgFileNameSystem"));
		
		String phanloai = commons.getTextJsonNode(jsonData.at("/PhanLoai")).trim().replaceAll("\\s+", " ");
		String dactinh = commons.getTextJsonNode(jsonData.at("/DacTinh")).trim().replaceAll("\\s+", " ");
		String mota = commons.getTextJsonNode(jsonData.at("/MoTa")).trim().replaceAll("\\s+", " ");
		String ghichu = commons.getTextJsonNode(jsonData.at("/GhiChu")).trim().replaceAll("\\s+", " ");
		
		String code  = "";
		try {
			if(FileNameSystem != "") {
				String[] parts = FileNameSystem.split("j");
				String part1 = parts[0];
				String part2 = parts[1];
				 code = part1;
			}
		} catch (Exception e) {
			responseStatus = new MspResponseStatus(9999," Lỗi cắt chuỗi");
			rsp.setResponseStatus(responseStatus);
		}

	
		
	

		ObjectId objectId = null;
		List<Document> pipeline = null;
		Document docFind = null;
		Document docTmp = null;
		Document docUpsert = null;

//////////////////////////////////////////////////////////////////////////////

		switch (actionCode) {
		case Constants.MSG_ACTION_CODE.CREATED:
		break;
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
			new Document("from", "DMTemplates")
			.append("pipeline", 
				Arrays.asList(
					new Document("$match", 
						new Document("_id", objectId)
					),
					new Document("$project", new Document("_id", 1))
				)
			)
			.append("as", "DMTemplates")
		)
	);
	pipeline.add(new Document("$unwind", new Document("path", "$DMTemplates").append("preserveNullAndEmptyArrays", true)));

			MongoClient mongoClient = cfg.mongoClient();
			MongoCollection<Document> collection = mongoClient.getDatabase(cfg.dbName).getCollection("DMTemplates");
			      try {
			        docTmp = collection.aggregate(pipeline).allowDiskUse(true).iterator().next();    
			      } catch (Exception e) {
			        
			      }
			        
			mongoClient.close();

			if (null == docTmp) {
				responseStatus = new MspResponseStatus(9999, "Không tìm thấy thông tin mẫu số.");
				rsp.setResponseStatus(responseStatus);
				return rsp;
			}

			
			options = new FindOneAndUpdateOptions();
			options.upsert(false);
			options.maxTime(5000, TimeUnit.MILLISECONDS);
			options.returnDocument(ReturnDocument.AFTER);
			
			
			if("".equals(FileNameSystem) && "".equals(ImgFileNameSystem)) {
				docUpsert = new Document("Name", name)
						.append("PhanLoai", phanloai)
						.append("DacTinh", dactinh)
						.append("MoTa", mota)
						.append("GhiChu", ghichu);																	
				
				mongoClient = cfg.mongoClient();
				collection = mongoClient.getDatabase(cfg.dbName).getCollection("DMTemplates");
				collection.findOneAndUpdate(docFind,
						new Document("$set", docUpsert)
						,
						options);
				  mongoClient.close();
				  
				responseStatus = new MspResponseStatus(0, "SUCCESS");
				rsp.setResponseStatus(responseStatus);
				return rsp;	
			}
			
			if("".equals(FileNameSystem)) {
				docUpsert = new Document("Images", ImgFileNameSystem)
						.append("Name", name)
						.append("PhanLoai", phanloai)
						.append("DacTinh", dactinh)
						.append("MoTa", mota)
						.append("GhiChu", ghichu);			
				
				mongoClient = cfg.mongoClient();
				collection = mongoClient.getDatabase(cfg.dbName).getCollection("DMTemplates");
				collection.findOneAndUpdate(docFind,
						new Document("$set", docUpsert)						
						,options);
					
				  mongoClient.close();
				  
			}else {
				docUpsert = new Document("FileName", FileNameSystem)
						.append("Name", name)
						.append("PhanLoai", phanloai)
						.append("DacTinh", dactinh)
						.append("MoTa", mota)
						.append("GhiChu", ghichu);		
				
				if(!"".equals(code)) {
					docUpsert.append("Code", code);
				}
				if(!"".equals(ImgFileNameSystem)) {
					docUpsert.append("Images", ImgFileNameSystem);
				}
				
				
				mongoClient = cfg.mongoClient();
				collection = mongoClient.getDatabase(cfg.dbName).getCollection("DMTemplates");
				collection.findOneAndUpdate(docFind,
						new Document("$set", docUpsert),
						options
					);
				  mongoClient.close();
			}

			responseStatus = new MspResponseStatus(0, "SUCCESS");
			rsp.setResponseStatus(responseStatus);
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
						new Document("from", "DMTemplates")
							.append("pipeline", 
								Arrays.asList(
									new Document("$match", 
										new Document("_id", objectId))
									, new Document("$project", new Document("_id", 1).append("Name", 1).append("FileName", 1))
							)
							)
							.append("as", "DMTemplates")
					)
				);
				pipeline.add(new Document("$unwind", new Document("path", "$DMTemplates").append("preserveNullAndEmptyArrays", true)));
			
			mongoClient = cfg.mongoClient();
			collection = mongoClient.getDatabase(cfg.dbName).getCollection("DMTemplates");
			  try {
			         docTmp = collection.aggregate(pipeline).allowDiskUse(true).iterator().next();      
			     } catch (Exception e) {
			        
			    }
			mongoClient.close();
			
			if (null == docTmp) {
				responseStatus = new MspResponseStatus(9999, "Không tìm thấy thông tin mẫu hóa đơn.");
				rsp.setResponseStatus(responseStatus);
				return rsp;
			}
String Name =   docTmp.getEmbedded(Arrays.asList("DMTemplates", "Name"), "");
String FileName =   docTmp.getEmbedded(Arrays.asList("DMTemplates", "FileName"), "");
			pipeline = new ArrayList<Document>();
			pipeline.add(
					new Document("$lookup", 
						new Document("from", "DMMauSoKyHieu")
						.append("pipeline", 
							Arrays.asList(
								new Document("$match", 
									new Document("Templates.Name", Name)
									.append("Templates.FileName", FileName)

								)
							)
						)
						.append("as", "DMMauSoKyHieu")
					)
				);
			
				pipeline.add(new Document("$unwind", new Document("path", "$DMMauSoKyHieu").append("preserveNullAndEmptyArrays", true)));
				
				mongoClient = cfg.mongoClient();
				collection = mongoClient.getDatabase(cfg.dbName).getCollection("DMMauSoKyHieu");
				  try {
				         docTmp = collection.aggregate(pipeline).allowDiskUse(true).iterator().next();      
				     } catch (Exception e) {
				        
				    }
				mongoClient.close();
				
				if(docTmp.get("DMMauSoKyHieu") != null) {
					responseStatus = new MspResponseStatus(9999, "Mẫu đã tạo hóa đơn không được xóa");
					rsp.setResponseStatus(responseStatus);
					return rsp;
				}
					
			options = new FindOneAndUpdateOptions();
			options.upsert(false);
			options.maxTime(5000, TimeUnit.MILLISECONDS);
			options.returnDocument(ReturnDocument.AFTER);
			
			mongoClient = cfg.mongoClient();
			collection = mongoClient.getDatabase(cfg.dbName).getCollection("DMTemplates");
			collection.findOneAndUpdate(docFind,
					new Document("$set", new Document("IsDelete", true).append("InfoDeleted",
							new Document("DeletedDate", LocalDateTime.now()).append("DeletedUserID", header.getUserId())
									.append("DeletedUserName", header.getUserName())
									.append("DeletedUserFullName", header.getUserFullName()))),
					options);
			  mongoClient.close();

			responseStatus = new MspResponseStatus(0, "SUCCESS");
			rsp.setResponseStatus(responseStatus);
			return rsp;
////////////////////////////////////////////////////////////////////////////////////////
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
			pipeline.add(new Document("$project", new Document("_id", 1)));
			
			mongoClient = cfg.mongoClient();
			collection = mongoClient.getDatabase(cfg.dbName).getCollection("DMTemplates");
			  try {
			         docTmp = collection.aggregate(pipeline).allowDiskUse(true).iterator().next();      
			     } catch (Exception e) {
			        
			    }
			mongoClient.close();
			
			if(null == docTmp) {
				responseStatus = new MspResponseStatus(9999, "Không tìm thấy thông tin mẫu hóa đơn.");
				rsp.setResponseStatus(responseStatus);
				return rsp;
			}

			options = new FindOneAndUpdateOptions();
			options.upsert(false);
			options.maxTime(5000, TimeUnit.MILLISECONDS);
			options.returnDocument(ReturnDocument.AFTER);
			
			mongoClient = cfg.mongoClient();
			collection = mongoClient.getDatabase(cfg.dbName).getCollection("DMTemplates");
			collection.findOneAndUpdate(docFind,
					new Document("$set", 
							new Document("IsActive", true)
							.append("InfoActive", 
									new Document("ActiveDate", LocalDateTime.now())
										.append("ActiveUserID", header.getUserId())
										.append("ActiveUserName", header.getUserName())
										.append("ActiveUserFullName", header.getUserFullName())
									)
						)
						, options);
			  mongoClient.close();
			
			responseStatus = new MspResponseStatus(0, "SUCCESS");
			rsp.setResponseStatus(responseStatus);				
			return rsp;
//////////////////////////////////////////////////////////////////////////////////////////
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
			pipeline.add(new Document("$project", new Document("_id", 1)));
			
			mongoClient = cfg.mongoClient();
			collection = mongoClient.getDatabase(cfg.dbName).getCollection("DMTemplates");
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
			collection = mongoClient.getDatabase(cfg.dbName).getCollection("DMTemplates");
			collection.findOneAndUpdate(docFind,
					new Document("$set", 
							new Document("IsActive", false)
							.append("InfoActive", 
									new Document("ActiveDate", LocalDateTime.now())
										.append("ActiveUserID", header.getUserId())
										.append("ActiveUserName", header.getUserName())
										.append("ActiveUserFullName", header.getUserFullName())
									)
						)
						, options);
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

		 String name = null;
		
		JsonNode jsonData = null;
		if(objData != null) {
			jsonData = Json.serializer().nodeFromObject(objData);	
			name = commons.getTextJsonNode(jsonData.at("/Name")).replaceAll("\\s+", " ");
		}

		MsgRsp rsp = new MsgRsp(header);
		MspResponseStatus responseStatus = null;
		
		ObjectId objectId = null;
		Document docTmp = null;
		Document docTmp1 = null;
		Document docTmp2 = null;
		List<Document> pipeline = new ArrayList<Document>();
		
		
		
		Document docMatch = new Document("IsDelete",new Document("$ne", true));
		if(!"".equals(name))
			docMatch.append("Name",  new Document("$regex", commons.regexEscapeForMongoQuery(name)).append("$options", "i"));
		
		
		pipeline = new ArrayList<Document>();
		pipeline.add(new Document("$match", docMatch));
		pipeline.add(
				new Document("$sort", 
					new Document("_id", -1)
				)
			);
		
		pipeline.add(
				new Document("$project", 
					new Document("_id", -1).append("loaihd_ma", 1).append("Code", 1).append("Name", 1).append("Images", 1).append("FileName", 1)
					.append("Images", 1).append("PhanLoai", 1).append("DanhSach", 1).append("DacTinhPhoi", 1).append("MoTa", 1).append("GhiChu", 1).append("IsActive", 1).append("InfoCreated", 1)
				)
			);

		pipeline.addAll(createFacetForSearchNotSort(page));
		
		MongoClient mongoClient = cfg.mongoClient();
		MongoCollection<Document> collection = mongoClient.getDatabase(cfg.dbName).getCollection("DMTemplates");
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
				
				String filename = doc.get("FileName", "");
							
				Document doc_fileName = new Document("IsDelete",
						new Document("$ne", true))
						.append("Templates.FileName",filename);
				
				
				pipeline = new ArrayList<Document>();
				pipeline.add(new Document("$match", doc_fileName));
				pipeline.add(new Document("$project", new Document("_id", 1).append("IssuerId", 1)));
				pipeline.addAll(createFacetForSearchNotSort(page));
			
				mongoClient = cfg.mongoClient();
				collection = mongoClient.getDatabase(cfg.dbName).getCollection("DMMauSoKyHieu");
				  try {
					  docTmp1 = collection.aggregate(pipeline).allowDiskUse(true).iterator().next();      
				     } catch (Exception e) {
				        
				    }
				mongoClient.close();
				
				if(docTmp1 == null) {
				continue;
				}
				
				List<Document> rows1 = null;
				if(docTmp1.get("data") != null && docTmp1.get("data") instanceof List) {
					rows1 = docTmp1.getList("data", Document.class);
				}

				String taxcode = "";
				if(null != rows1) {
					for(Document doc1: rows1) {								
						
						String issuerId = doc1.get("IssuerId", "");
												
						//FIND MST
						ObjectId id_issuer = new ObjectId(issuerId);						
						Document find_issuer = new Document("_id", id_issuer);
			
						pipeline = new ArrayList<Document>();
						pipeline.add(new Document("$match", find_issuer));
						pipeline.add(new Document("$project", new Document("_id", 1).append("TaxCode", 1)));
						mongoClient = cfg.mongoClient();
						collection = mongoClient.getDatabase(cfg.dbName).getCollection("Issuer");
						  try {
							  docTmp2 = collection.aggregate(pipeline).allowDiskUse(true).iterator().next();      
						     } catch (Exception e) {
						        
						    }
						mongoClient.close();
						
						String taxcode1 = docTmp2.get("TaxCode", "");
						if(taxcode.equals("")) {
							taxcode = taxcode1;
						}else {
						taxcode = taxcode + "," + taxcode1; 
						}
					}
				}
				

				
				hItem.put("_id", objectId.toString());
				hItem.put("loaihd_ma", doc.get("loaihd_ma", ""));
				hItem.put("Code", doc.get("Code", ""));
				hItem.put("Name", doc.get("Name", ""));
				hItem.put("Images", doc.get("Images", ""));				
				hItem.put("FileName", doc.get("FileName", ""));
				hItem.put("Images", doc.get("Images"));
				hItem.put("PhanLoai", doc.get("PhanLoai", ""));
				
				hItem.put("DanhSach", taxcode);
				
				hItem.put("DacTinhPhoi", doc.get("DacTinhPhoi", ""));
				hItem.put("MoTa", doc.get("MoTa", ""));
				hItem.put("GhiChu", doc.get("GhiChu", ""));
				hItem.put("IsActive", doc.get("IsActive"));
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
		MongoCollection<Document> collection = mongoClient.getDatabase(cfg.dbName).getCollection("DMTemplates");
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

	@Override
	public MsgRsp viewimg(JSONRoot jsonRoot, String _id) throws Exception {
		MsgRsp rsp = new MsgRsp();
		MspResponseStatus responseStatus = null;
		Document docFind = null;

		ObjectId objectId = null;
		try {
			objectId = new ObjectId(_id);
		} catch (Exception e) {
		}

		 docFind = new Document("_id", objectId);
		 Document docTmp = null;

		List<Document> pipeline = null;
		pipeline = new ArrayList<Document>();
		pipeline.add(new Document("$match", docFind));
		
		MongoClient mongoClient = cfg.mongoClient();
		MongoCollection<Document> collection = mongoClient.getDatabase(cfg.dbName).getCollection("Issuer");
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
		
		String phoi = docTmp.get("Name", "");
		
		docFind = new Document("Name",phoi);
		Document docTmp1 = null;
		
		pipeline = new ArrayList<Document>();
		pipeline.add(new Document("$match", docFind));
		
		mongoClient = cfg.mongoClient();
		collection = mongoClient.getDatabase(cfg.dbName).getCollection("DMTemplates");
		  try {
			  docTmp1 = collection.aggregate(pipeline).allowDiskUse(true).iterator().next();      
		     } catch (Exception e) {
		        
		    }
		mongoClient.close();
		
		if (null == docTmp1) {
			responseStatus = new MspResponseStatus(9999, "Không tìm thấy ảnh mẫu.");
			rsp.setResponseStatus(responseStatus);
			return rsp;
		}
		String img =  docTmp1.get("Images", "");
		String dir = "C:/hddt-ses/server/template/";
		File f = new File(dir,img);
		String base64Template = commons.encodeImageToBase64(f);
		rsp.setObjData(base64Template);
		return rsp;
	}

}
