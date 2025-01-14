package vn.sesgroup.hddt.user.impl;

import java.io.ByteArrayOutputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
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
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.FindOneAndUpdateOptions;
import com.mongodb.client.model.ReturnDocument;

import vn.sesgroup.hddt.configuration.ConfigConnectMongo;
import vn.sesgroup.hddt.dto.FileInfo;
import vn.sesgroup.hddt.user.dao.AbstractDAO;
import vn.sesgroup.hddt.user.dao.DSNMuaDAO;
import vn.sesgroup.hddt.user.service.JPUtils;
import vn.sesgroup.hddt.user.service.TCTNService;
import vn.sesgroup.hddt.utility.Constants;
import vn.sesgroup.hddt.utility.Json;

@Repository
@Transactional
public class DSNMuaImpl extends AbstractDAO implements DSNMuaDAO {
	private static final Logger log = LogManager.getLogger(DSNMuaImpl.class);
	@Autowired ConfigConnectMongo cfg;
	@Autowired TCTNService tctnService;
	@Autowired
	JPUtils jpUtils;
	Document docUpsert = null;
	@Override
	public MsgRsp crud(JSONRoot jsonRoot) throws Exception {
		Msg msg = jsonRoot.getMsg();
		MsgHeader header = msg.getMsgHeader();
		MsgPage page = msg.getMsgPage();
		page.setSize(1000000);
		Object objData = msg.getObjData();
		ObjectId objectIdQLNMua = null;
		JsonNode jsonData = null;
		if (objData != null) {
			jsonData = Json.serializer().nodeFromObject(msg.getObjData());
		} else {
			throw new Exception("Lỗi dữ liệu đầu vào");
		}

		String actionCode = header.getActionCode();
		String fromDate = commons.getTextJsonNode(jsonData.at("/TuNgay")).replaceAll("\\s", "");
		String toDate = commons.getTextJsonNode(jsonData.at("/DenNgay")).replaceAll("\\s", "");
    	String phone_nm = "";
		String email_nm = "";
		
		MsgRsp rsp = new MsgRsp(header);
		MspResponseStatus responseStatus = null;

		FindOneAndUpdateOptions options = null;

		List<Document> pipeline = new ArrayList<Document>();
		List<Document> pipeline1 = new ArrayList<Document>();
		List<Document> pipeline2 = new ArrayList<Document>();
		List<Document> pipeline3 = new ArrayList<Document>();
		Document docTmp = null;	
		Document docTmp1 = null;	
		Document docTmp2 = null;	
		Document docTmp3 = null;	
		Document docUpsert1 = null;
		LocalDate dateFrom = null;
		LocalDate dateTo = null;
		Document docMatchDate = null;
		ObjectId objectNMua = null;
		Document docFindNmua = null;
		List<Document> rows = null;
		List<Document> rows1 = null;
		List<Document> rows2 = null;
		List<Document> rows3 = null;
		switch (actionCode) {
		case Constants.MSG_ACTION_CODE.CREATED:			

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
		}
			/* KIEM TRA THONG TIN KHACH HANG - USERS */

			Document docMatch = new Document("IsDelete",new Document("$ne", true));			
//			if (null != docMatchDate)
//				docMatch.append("EInvoiceDetail.TTChung.NLap", docMatchDate);						
			docMatch.append("$expr", new Document("$and", Arrays.asList(		
					new Document("$ne", Arrays.asList("$EInvoiceDetail.NDHDon.NMua.MST", "")),
					new Document("$ne", Arrays.asList("$EInvoiceDetail.NDHDon.NMua.MST", null))					
					)));
			
		
			//EINVOICE
			pipeline = new ArrayList<Document>();
			pipeline.add(new Document("$match", docMatch));
			pipeline.add(new Document("$sort",
					new Document("EInvoiceDetail.TTChung.MauSoHD", -1).append("SHDon", -1).append("_id", -1)));			
			pipeline.addAll(createFacetForSearchNotSort(page));
			
			MongoClient mongoClient = cfg.mongoClient();
			MongoDatabase dbmg = mongoClient.getDatabase(cfg.dbName);
			MongoCollection<Document> collection = dbmg.getCollection("EInvoice");
			
			try {
				docTmp = collection.aggregate(pipeline).allowDiskUse(true).iterator().next();	
			} catch (Exception e) {
				
			}
					
			mongoClient.close();
			
			if (null == docTmp) {
				responseStatus = new MspResponseStatus(9999, "Không tìm thấy thông tin hóa đơn VAT.");
				rsp.setResponseStatus(responseStatus);
				return rsp;
			}
			
			rsp = new MsgRsp(header);
			page.setSize(docTmp.getInteger("total", 0));
			page.setTotalRows(docTmp.getInteger("total", 0));
			rsp.setMsgPage(page);
			
		
			if (docTmp.get("data") != null && docTmp.get("data") instanceof List) {
				rows = docTmp.getList("data", Document.class);
			}

			if (null != rows) {
				for (Document doc : rows) {

					String nlap = commons.convertLocalDateTimeToString(
							commons.convertDateToLocalDateTime(
							doc.getEmbedded(Arrays.asList("EInvoiceDetail","TTChung", "NLap"), Date.class)),"dd/MM/yyyy");
					String mst = doc.getEmbedded(Arrays.asList("EInvoiceDetail","NDHDon", "NMua", "MST"), "");
					String ten = doc.getEmbedded(Arrays.asList("EInvoiceDetail","NDHDon", "NMua", "Ten"), "");
					String dchi = doc.getEmbedded(Arrays.asList("EInvoiceDetail","NDHDon", "NMua", "DChi"), "");
					String sdt = doc.getEmbedded(Arrays.asList("EInvoiceDetail","NDHDon", "NMua", "SDThoai"), "");
					String email = doc.getEmbedded(Arrays.asList("EInvoiceDetail","NDHDon", "NMua", "DCTDTu"), "");
				
					//KIEM TRA TON TAI TRONG BANG DANH SACH NGUOI MUA
					
					Document r = null;
				    
				    Document docFind = new Document("MST", mst);				    
				    mongoClient = cfg.mongoClient();
				    collection = mongoClient.getDatabase(cfg.dbName).getCollection("QLDSNMua");
				      try {
				             r = collection.find(docFind).allowDiskUse(true).iterator().next();      
				         } catch (Exception e) {
				            
				        }
				    mongoClient.close();
				    
				    //NEU KO TON TAI THI TIEN HANH INSERT VAO COLLECTION QLDSNMua
				    if (null == r) {
				    	objectIdQLNMua = new ObjectId();
						docUpsert1 = new Document("_id", objectIdQLNMua)
								.append("NLap",
										commons.convertStringToLocalDate(nlap, Constants.FORMAT_DATE.FORMAT_DATE_WEB))						
								.append("Ten", ten)
								.append("MST", mst)
								.append("DChi", dchi)
								.append("SDThoai", sdt)
								.append("DCTDTu", email)
								.append("IsDelete", false);
						
						mongoClient = cfg.mongoClient();
						collection = mongoClient.getDatabase(cfg.dbName).getCollection("QLDSNMua");
						collection.insertOne(docUpsert1);      
						mongoClient.close();
					}
				    //NEU DA TON TAI THI TIEN HANH SO SANH
				    else {
				    	//KIEM TRA SO DIEN THOAI VA EMAIL DE CAP NHAT THEM VAO
						String phone_ = r.getString("SDThoai");
						String email_ = r.getString("DCTDTu");
						boolean isPhone = phone_.contains(sdt);
						boolean isEmail = email_.contains(email);

						if(phone_.equals("")|| phone_.isEmpty()) {
							phone_nm = sdt;
						}else {
						 if(isPhone==true) {
						phone_nm = phone_;
						}
						 else {
						phone_nm = phone_ + "," + sdt;
						}					
					}
					
						if(email_.equals("")|| email_.isEmpty()) {
							email_nm = email;
						}else {
						 if(isEmail==true) {
							 email_nm = email_;
						}
						 else {
							 email_nm = email_ + "," + email;
						}					
					}
						//TIEN HANH UPDATE VÀO COLLECTION
						 String id_nm = r.get("_id").toString();	
							objectNMua = new ObjectId(id_nm);
							docFindNmua = new Document("_id", objectNMua);
							options = new FindOneAndUpdateOptions();
							options.upsert(false);
							options.maxTime(5000, TimeUnit.MILLISECONDS);
							options.returnDocument(ReturnDocument.AFTER);						
							
							MongoClient mongoClient1 = cfg.mongoClient();
							collection = mongoClient1.getDatabase(cfg.dbName).getCollection("QLDSNMua");
							collection.findOneAndUpdate(docFindNmua,
									new Document("$set",
											new Document("Ten", ten)	
											.append("NLap",
													commons.convertStringToLocalDate(nlap, Constants.FORMAT_DATE.FORMAT_DATE_WEB))															
											.append("MST", mst)
											.append("DChi", dchi)
											.append("SDThoai", phone_nm)
											.append("DCTDTu", email_nm)
											.append("IsDelete", false)	
											),
									options);
							mongoClient1.close();
				  //END VONG LAP INSERT VA UPDATE
				    }
				  //END VONG LAP ROWS 
				}
				
			}
		
			//END EINVOICE
			
			//EINVOICE BAN HANG
			pipeline1 = new ArrayList<Document>();
			pipeline1.add(new Document("$match", docMatch));
			pipeline1.add(new Document("$sort",
					new Document("EInvoiceDetail.TTChung.MauSoHD", -1).append("SHDon", -1).append("_id", -1)));			
			pipeline1.addAll(createFacetForSearchNotSort(page));
					
//			cursor1 = mongoTemplate.getCollection("EInvoiceBH").aggregate(pipeline1).allowDiskUse(true);
//			iter1 = cursor1.iterator();
//			if (iter1.hasNext()) {
//				docTmp1 = iter1.next();
//			}
			
			mongoClient = cfg.mongoClient();
			collection = mongoClient.getDatabase(cfg.dbName).getCollection("EInvoiceBH");
			      try {
			    	  docTmp1 = collection.aggregate(pipeline1).allowDiskUse(true).iterator().next();    
			      } catch (Exception e) {
			        
			      }
			        
			mongoClient.close();
			
			if (null == docTmp1) {
				responseStatus = new MspResponseStatus(9999, "Không tìm thấy thông tin hóa đơn bán hàng.");
				rsp.setResponseStatus(responseStatus);
				return rsp;
			}
			rsp = new MsgRsp(header);
			page.setSize(docTmp1.getInteger("total", 0));
			page.setTotalRows(docTmp1.getInteger("total", 0));
			rsp.setMsgPage(page);
			
		
			if (docTmp1.get("data") != null && docTmp1.get("data") instanceof List) {
				rows1 = docTmp1.getList("data", Document.class);
			}

			if (null != rows1) {
				for (Document doc : rows1) {

					String nlap = commons.convertLocalDateTimeToString(
							commons.convertDateToLocalDateTime(
							doc.getEmbedded(Arrays.asList("EInvoiceDetail","TTChung", "NLap"), Date.class)),"dd/MM/yyyy");
					String mst = doc.getEmbedded(Arrays.asList("EInvoiceDetail","NDHDon", "NMua", "MST"), "");
					String ten = doc.getEmbedded(Arrays.asList("EInvoiceDetail","NDHDon", "NMua", "Ten"), "");
					String dchi = doc.getEmbedded(Arrays.asList("EInvoiceDetail","NDHDon", "NMua", "DChi"), "");
					String sdt = doc.getEmbedded(Arrays.asList("EInvoiceDetail","NDHDon", "NMua", "SDThoai"), "");
					String email = doc.getEmbedded(Arrays.asList("EInvoiceDetail","NDHDon", "NMua", "DCTDTu"), "");
				
					//KIEM TRA TON TAI TRONG BANG DANH SACH NGUOI MUA
					
					Document r = null;
				    
				    Document docFind = new Document("MST", mst);
				    
//				    Iterable<Document> cursor4 = mongoTemplate.getCollection("QLDSNMua").find(docFind);
//				    Iterator<Document> iter4 = cursor4.iterator();
//				    if(iter4.hasNext()) {
//				      r = iter4.next();
//				    }
				    
				    mongoClient = cfg.mongoClient();
				    collection = mongoClient.getDatabase(cfg.dbName).getCollection("QLDSNMua");
				      try {
				             r = collection.find(docFind).allowDiskUse(true).iterator().next();      
				         } catch (Exception e) {
				            
				        }
				    mongoClient.close();
				    
				    //NEU KO TON TAI THI TIEN HANH INSERT VAO COLLECTION QLDSNMua
				    if (null == r) {
				    	objectIdQLNMua = new ObjectId();
						docUpsert1 = new Document("_id", objectIdQLNMua)
								.append("NLap",
										commons.convertStringToLocalDate(nlap, Constants.FORMAT_DATE.FORMAT_DATE_WEB))						
								.append("Ten", ten)
								.append("MST", mst)
								.append("DChi", dchi)
								.append("SDThoai", sdt)
								.append("DCTDTu", email)
								.append("IsDelete", false);

						mongoClient = cfg.mongoClient();
						collection = mongoClient.getDatabase(cfg.dbName).getCollection("QLDSNMua");
						collection.insertOne(docUpsert1);      
						mongoClient.close();
					}
				    //NEU DA TON TAI THI TIEN HANH SO SANH
				    else {
				    	//KIEM TRA SO DIEN THOAI VA EMAIL DE CAP NHAT THEM VAO
						String phone_ = r.getString("SDThoai");
						String email_ = r.getString("DCTDTu");
						boolean isPhone = phone_.contains(sdt);
						boolean isEmail = email_.contains(email);

						if(phone_.equals("")|| phone_.isEmpty()) {
							phone_nm = sdt;
						}else {
						 if(isPhone==true) {
						phone_nm = phone_;
						}
						 else {
						phone_nm = phone_ + "," + sdt;
						}					
					}
					
						if(email_.equals("")|| email_.isEmpty()) {
							email_nm = email;
						}else {
						 if(isEmail==true) {
							 email_nm = email_;
						}
						 else {
							 email_nm = email_ + "," + email;
						}					
					}
						//TIEN HANH UPDATE VÀO COLLECTION
						 String id_nm = r.get("_id").toString();	
							objectNMua = new ObjectId(id_nm);
							docFindNmua = new Document("_id", objectNMua);
							options = new FindOneAndUpdateOptions();
							options.upsert(false);
							options.maxTime(5000, TimeUnit.MILLISECONDS);
							options.returnDocument(ReturnDocument.AFTER);
													
							MongoClient mongoClient1 = cfg.mongoClient();
							collection = mongoClient1.getDatabase(cfg.dbName).getCollection("QLDSNMua");
							collection.findOneAndUpdate(docFindNmua,
									new Document("$set",
											new Document("Ten", ten)	
											.append("NLap",
													commons.convertStringToLocalDate(nlap, Constants.FORMAT_DATE.FORMAT_DATE_WEB))															
											.append("MST", mst)
											.append("DChi", dchi)
											.append("SDThoai", phone_nm)
											.append("DCTDTu", email_nm)
											.append("IsDelete", false)	
											),
									options);
							mongoClient1.close();
				  //END VONG LAP INSERT VA UPDATE
				    }
				  //END VONG LAP ROWS 
				}
			}
			
			//END EINVOICE BAN HANG
			
			//EINVOICE PXK
			pipeline2 = new ArrayList<Document>();
			pipeline2.add(new Document("$match", docMatch));
			pipeline2.add(new Document("$sort",
					new Document("EInvoiceDetail.TTChung.MauSoHD", -1).append("SHDon", -1).append("_id", -1)));			
			pipeline2.addAll(createFacetForSearchNotSort(page));
			
			mongoClient = cfg.mongoClient();
			collection = mongoClient.getDatabase(cfg.dbName).getCollection("EInvoicePXK");
			  try {
				  docTmp2 = collection.aggregate(pipeline2).allowDiskUse(true).iterator().next();      
			     } catch (Exception e) {
			        
			    }
			mongoClient.close();
			
			if (null == docTmp2) {
				responseStatus = new MspResponseStatus(9999, "Không tìm thấy thông tin hóa đơn phiếu xuất kho.");
				rsp.setResponseStatus(responseStatus);
				return rsp;
			}
			rsp = new MsgRsp(header);
			page.setSize(docTmp2.getInteger("total", 0));
			page.setTotalRows(docTmp2.getInteger("total", 0));
			rsp.setMsgPage(page);
			
		
			if (docTmp2.get("data") != null && docTmp2.get("data") instanceof List) {
				rows2 = docTmp2.getList("data", Document.class);
			}

			if (null != rows2) {
				for (Document doc : rows2) {

					String nlap = commons.convertLocalDateTimeToString(
							commons.convertDateToLocalDateTime(
							doc.getEmbedded(Arrays.asList("EInvoiceDetail","TTChung", "NLap"), Date.class)),"dd/MM/yyyy");
					String mst = doc.getEmbedded(Arrays.asList("EInvoiceDetail","NDHDon", "NMua", "MST"), "");
					String ten = doc.getEmbedded(Arrays.asList("EInvoiceDetail","NDHDon", "NMua", "Ten"), "");
					String dchi = doc.getEmbedded(Arrays.asList("EInvoiceDetail","NDHDon", "NMua", "DChi"), "");
					String sdt = doc.getEmbedded(Arrays.asList("EInvoiceDetail","NDHDon", "NMua", "SDThoai"), "");
					String email = doc.getEmbedded(Arrays.asList("EInvoiceDetail","NDHDon", "NMua", "DCTDTu"), "");
				
					//KIEM TRA TON TAI TRONG BANG DANH SACH NGUOI MUA
					
					Document r = null;			    
				    Document docFind = new Document("MST", mst);
				    				    
				    mongoClient = cfg.mongoClient();
				    collection = mongoClient.getDatabase(cfg.dbName).getCollection("QLDSNMua");
				      try {
				             r = collection.find(docFind).allowDiskUse(true).iterator().next();      
				         } catch (Exception e) {
				            
				        }
				    mongoClient.close();
				    
				    //NEU KO TON TAI THI TIEN HANH INSERT VAO COLLECTION QLDSNMua
				    if (null == r) {
				    	objectIdQLNMua = new ObjectId();
						docUpsert1 = new Document("_id", objectIdQLNMua)
								.append("NLap",
										commons.convertStringToLocalDate(nlap, Constants.FORMAT_DATE.FORMAT_DATE_WEB))						
								.append("Ten", ten)
								.append("MST", mst)
								.append("DChi", dchi)
								.append("SDThoai", sdt)
								.append("DCTDTu", email)
								.append("IsDelete", false);
			
						mongoClient = cfg.mongoClient();
						collection = mongoClient.getDatabase(cfg.dbName).getCollection("QLDSNMua");
						collection.insertOne(docUpsert1);      
						mongoClient.close();
					}
				    //NEU DA TON TAI THI TIEN HANH SO SANH
				    else {
				    	//KIEM TRA SO DIEN THOAI VA EMAIL DE CAP NHAT THEM VAO
						String phone_ = r.getString("SDThoai");
						String email_ = r.getString("DCTDTu");
						boolean isPhone = phone_.contains(sdt);
						boolean isEmail = email_.contains(email);

						if(phone_.equals("")|| phone_.isEmpty()) {
							phone_nm = sdt;
						}else {
						 if(isPhone==true) {
						phone_nm = phone_;
						}
						 else {
						phone_nm = phone_ + "," + sdt;
						}					
					}
					
						if(email_.equals("")|| email_.isEmpty()) {
							email_nm = email;
						}else {
						 if(isEmail==true) {
							 email_nm = email_;
						}
						 else {
							 email_nm = email_ + "," + email;
						}					
					}
						//TIEN HANH UPDATE VÀO COLLECTION
						 String id_nm = r.get("_id").toString();	
							objectNMua = new ObjectId(id_nm);
							docFindNmua = new Document("_id", objectNMua);
							options = new FindOneAndUpdateOptions();
							options.upsert(false);
							options.maxTime(5000, TimeUnit.MILLISECONDS);
							options.returnDocument(ReturnDocument.AFTER);
							
							MongoClient mongoClient1 = cfg.mongoClient();
							collection = mongoClient1.getDatabase(cfg.dbName).getCollection("QLDSNMua");
							collection.findOneAndUpdate(docFindNmua,
									new Document("$set",
											new Document("Ten", ten)	
											.append("NLap",
													commons.convertStringToLocalDate(nlap, Constants.FORMAT_DATE.FORMAT_DATE_WEB))															
											.append("MST", mst)
											.append("DChi", dchi)
											.append("SDThoai", phone_nm)
											.append("DCTDTu", email_nm)
											.append("IsDelete", false)	
											),
									options);
							mongoClient1.close();
				  //END VONG LAP INSERT VA UPDATE
				    }
				  //END VONG LAP ROWS 
				}
			}
			//END EINVOICE PXK
			
			//EINVOICE DLY
			pipeline3 = new ArrayList<Document>();
			pipeline3.add(new Document("$match", docMatch));
			pipeline3.add(new Document("$sort",
					new Document("EInvoiceDetail.TTChung.MauSoHD", -1).append("SHDon", -1).append("_id", -1)));			
			pipeline3.addAll(createFacetForSearchNotSort(page));
					
			
			mongoClient = cfg.mongoClient();
			collection = mongoClient.getDatabase(cfg.dbName).getCollection("EInvoicePXKDL");
			  try {
				  docTmp3 = collection.aggregate(pipeline3).allowDiskUse(true).iterator().next();      
			     } catch (Exception e) {
			        
			    }
			mongoClient.close();
			
			if (null == docTmp3) {
				responseStatus = new MspResponseStatus(9999, "Không tìm thấy thông tin hóa đơn phiếu xuất kho ký gửi đại lý.");
				rsp.setResponseStatus(responseStatus);
				return rsp;
			}
			rsp = new MsgRsp(header);
			page.setSize(docTmp3.getInteger("total", 0));
			page.setTotalRows(docTmp3.getInteger("total", 0));
			rsp.setMsgPage(page);
			
		
			if (docTmp3.get("data") != null && docTmp3.get("data") instanceof List) {
				rows3 = docTmp3.getList("data", Document.class);
			}

			if (null != rows3) {
				for (Document doc : rows3) {

					String nlap = commons.convertLocalDateTimeToString(
							commons.convertDateToLocalDateTime(
							doc.getEmbedded(Arrays.asList("EInvoiceDetail","TTChung", "NLap"), Date.class)),"dd/MM/yyyy");
					String mst = doc.getEmbedded(Arrays.asList("EInvoiceDetail","NDHDon", "NMua", "MST"), "");
					String ten = doc.getEmbedded(Arrays.asList("EInvoiceDetail","NDHDon", "NMua", "Ten"), "");
					String dchi = doc.getEmbedded(Arrays.asList("EInvoiceDetail","NDHDon", "NMua", "DChi"), "");
					String sdt = doc.getEmbedded(Arrays.asList("EInvoiceDetail","NDHDon", "NMua", "SDThoai"), "");
					String email = doc.getEmbedded(Arrays.asList("EInvoiceDetail","NDHDon", "NMua", "DCTDTu"), "");
				
					//KIEM TRA TON TAI TRONG BANG DANH SACH NGUOI MUA
					
					Document r = null;
				    Document docFind = new Document("MST", mst);
				    
				    mongoClient = cfg.mongoClient();
				    collection = mongoClient.getDatabase(cfg.dbName).getCollection("QLDSNMua");
				      try {
				             r = collection.find(docFind).allowDiskUse(true).iterator().next();      
				         } catch (Exception e) {
				            
				        }
				    mongoClient.close();
				    
				    //NEU KO TON TAI THI TIEN HANH INSERT VAO COLLECTION QLDSNMua
				    if (null == r) {
				    	objectIdQLNMua = new ObjectId();
						docUpsert1 = new Document("_id", objectIdQLNMua)
								.append("NLap",
										commons.convertStringToLocalDate(nlap, Constants.FORMAT_DATE.FORMAT_DATE_WEB))						
								.append("Ten", ten)
								.append("MST", mst)
								.append("DChi", dchi)
								.append("SDThoai", sdt)
								.append("DCTDTu", email)
								.append("IsDelete", false);
	
						mongoClient = cfg.mongoClient();
						collection = mongoClient.getDatabase(cfg.dbName).getCollection("QLDSNMua");
						collection.insertOne(docUpsert1);      
						mongoClient.close();
					}
				    //NEU DA TON TAI THI TIEN HANH SO SANH
				    else {
				    	//KIEM TRA SO DIEN THOAI VA EMAIL DE CAP NHAT THEM VAO
						String phone_ = r.getString("SDThoai");
						String email_ = r.getString("DCTDTu");
						boolean isPhone = phone_.contains(sdt);
						boolean isEmail = email_.contains(email);

						if(phone_.equals("")|| phone_.isEmpty()) {
							phone_nm = sdt;
						}else {
						 if(isPhone==true) {
						phone_nm = phone_;
						}
						 else {
						phone_nm = phone_ + "," + sdt;
						}					
					}
					
						if(email_.equals("")|| email_.isEmpty()) {
							email_nm = email;
						}else {
						 if(isEmail==true) {
							 email_nm = email_;
						}
						 else {
							 email_nm = email_ + "," + email;
						}					
					}
						//TIEN HANH UPDATE VÀO COLLECTION
						 String id_nm = r.get("_id").toString();	
							objectNMua = new ObjectId(id_nm);
							docFindNmua = new Document("_id", objectNMua);
							options = new FindOneAndUpdateOptions();
							options.upsert(false);
							options.maxTime(5000, TimeUnit.MILLISECONDS);
							options.returnDocument(ReturnDocument.AFTER);
							
							MongoClient mongoClient1 = cfg.mongoClient();
							collection = mongoClient1.getDatabase(cfg.dbName).getCollection("QLDSNMua");
							collection.findOneAndUpdate(docFindNmua,
									new Document("$set",
											new Document("Ten", ten)	
											.append("NLap",
													commons.convertStringToLocalDate(nlap, Constants.FORMAT_DATE.FORMAT_DATE_WEB))															
											.append("MST", mst)
											.append("DChi", dchi)
											.append("SDThoai", phone_nm)
											.append("DCTDTu", email_nm)
											.append("IsDelete", false)	
											),
									options); 
							mongoClient1.close();
				  //END VONG LAP INSERT VA UPDATE
				    }
				  //END VONG LAP ROWS 
				}
			}
			//END EINVOICE DLY
			
			
		
			responseStatus = new MspResponseStatus(0, "SUCCESS");
			rsp.setResponseStatus(responseStatus);
			return rsp;
	}
	
	
	//List file to export
		private List<Document> buildListPipeline1(Object objData, MsgHeader header) throws Exception{
			
			List<Document> pipeline1 = null;		
			Document docMatch = new Document("IsDelete", new Document("$ne", true));

			pipeline1 = new ArrayList<Document>();
			pipeline1.add(new Document("$match", docMatch));	
			pipeline1.add(new Document("$project", new Document("_id", 1).append("Ten", 1).append("NLap", 1).append("MST", 1).append("DChi", 1)
					.append("SDThoai", 1).append("DCTDTu", 1)
					));	

			return pipeline1;
		}
		//END List file to export
	
	
	@Override
	public FileInfo exportExcel(JSONRoot jsonRoot) throws Exception {
		Msg msg = jsonRoot.getMsg();
		MsgHeader header = msg.getMsgHeader();
		MsgPage page = msg.getMsgPage();
		page.setSize(1000000);
		Object objData = msg.getObjData();
		List<Document> rows3 = null;
		List<Document> pipeline1 = new ArrayList<Document>();
		pipeline1 = buildListPipeline1(objData, header);
		
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
			styleInfoL.setWrapText(true);
			
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
			styleInfoC.setWrapText(true);
			
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
			
			List<String> headers = Arrays.asList(new String[] {
					"Mã số thuế\r\n(mst)" 
					, "Tên\r\n(ten)" 
					, "Ngày lập\r\n(nlap)"
					, "Địa chỉ\r\n(d_chi)"
					, "Số điện thoại\r\n(so_dt)"
					, "Email\r\n(email)"					
					});
			row = sheet.getRow(0);
			if(null == row) row = sheet.createRow(0);
			row.setHeight((short) 500);
			for(int i = 0; i < headers.size(); i++) {
				cell = row.getCell(i);
				if(cell == null) cell = row.createCell(i);
				cell.setCellStyle(styleHeader);
				cell.setCellValue(headers.get(i));
				if(i == 0)
					sheet.setColumnWidth(i, 6000);
				else if(i == 2)
					sheet.setColumnWidth(i, 6000);
				else if(i==5)
					sheet.setColumnWidth(i, 10000);
				else
					sheet.setColumnWidth(i, 8000);
			}
			
			Document docTmp = null;			
			int posRowData = 1;
			
			pipeline1.addAll(createFacetForSearchNotSort(page));	
			
			MongoClient mongoClient = cfg.mongoClient();
			MongoCollection<Document> collection = mongoClient.getDatabase(cfg.dbName).getCollection("QLDSNMua");
			      try {
			        docTmp = collection.aggregate(pipeline1).allowDiskUse(true).iterator().next();    
			      } catch (Exception e) {
			        
			      }
			        
			mongoClient.close();
			
			if (docTmp.get("data") != null && docTmp.get("data") instanceof List) {
				rows3 = docTmp.getList("data", Document.class);
			}

			if (null != rows3) {
				for (Document doc : rows3) {
					Date nlap = doc.getDate("NLap");
					DateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy");  
					String NLap = dateFormat.format(nlap);  
					row = sheet.getRow(posRowData);
					if(null == row) row = sheet.createRow(posRowData);
									
						cell = row.getCell(0);if(cell == null) cell = row.createCell(0);cell.setCellStyle(styleInfoL);
						cell.setCellValue(doc.getString("MST"));
						
						cell = row.getCell(1);if(cell == null) cell = row.createCell(1);cell.setCellStyle(styleInfoL);
						cell.setCellValue(doc.getString("Ten"));
						
						cell = row.getCell(2);if(cell == null) cell = row.createCell(2);cell.setCellStyle(styleInfoC);				
						cell.setCellValue(NLap);
												
						cell = row.getCell(3);if(cell == null) cell = row.createCell(3);cell.setCellStyle(styleInfoL);
						cell.setCellValue(doc.getString("DChi"));
						
						cell = row.getCell(4);if(cell == null) cell = row.createCell(4);cell.setCellStyle(styleInfoL);
						cell.setCellValue(doc.getString("SDThoai"));
						
						cell = row.getCell(5);if(cell == null) cell = row.createCell(5);cell.setCellStyle(styleInfoL);
						cell.setCellValue(doc.getString("DCTDTu"));										
						
						posRowData++;					
				}
				
			}
			
			
			out = new ByteArrayOutputStream();
			wb.write(out);
			
			FileInfo fileInfo = new FileInfo();
			fileInfo.setFileName("DANH-SACH-KHACH-HANG.xlsx");
			fileInfo.setContentFile(out.toByteArray());			
			return fileInfo;
		}catch(Exception e) {
			throw e;
		}finally {
			try{wb.dispose(); wb.close();}catch(Exception ex){}
		}
	}

	
	@Override
	public MsgRsp list(JSONRoot jsonRoot) throws Exception {
		Msg msg = jsonRoot.getMsg();
		MsgHeader header = msg.getMsgHeader();
		MsgPage page = msg.getMsgPage();
		Object objData = msg.getObjData();
		String fromDate = "";
		String toDate = "";
		JsonNode jsonData = null;
		if (objData != null) {
			jsonData = Json.serializer().nodeFromObject(objData);

			
			fromDate = commons.getTextJsonNode(jsonData.at("/FromDate")).replaceAll("\\s", "");
			toDate = commons.getTextJsonNode(jsonData.at("/ToDate")).replaceAll("\\s", "");
		}

		MsgRsp rsp = new MsgRsp(header);
		MspResponseStatus responseStatus = null;

		ObjectId objectId = null;
		Document docTmp = null;
		List<Document> pipeline = new ArrayList<Document>();

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

		Document docMatch = new Document("IsDelete",
				new Document("$ne", true));
		if (null != docMatchDate)
			docMatch.append("NLap", docMatchDate);
	
		pipeline = new ArrayList<Document>();
		pipeline.add(new Document("$match", docMatch));
		pipeline.add(new Document("$project", new Document("_id", 1).append("Ten", 1).append("NLap", 1).append("MST", 1).append("DChi", 1)
				.append("SDThoai", 1).append("DCTDTu", 1)
				));
			
		pipeline.addAll(createFacetForSearchNotSort(page));
		
		MongoClient mongoClient = cfg.mongoClient();
		MongoCollection<Document> collection = mongoClient.getDatabase(cfg.dbName).getCollection("QLDSNMua");
		      try {
		        docTmp = collection.aggregate(pipeline).allowDiskUse(true).iterator().next();    
		      } catch (Exception e) {
		        
		      }
		        
		mongoClient.close();

		rsp = new MsgRsp(header);
		responseStatus = null;
		if (null == docTmp) {
			responseStatus = new MspResponseStatus(9999, Constants.MAP_ERROR.get(9999));
			rsp.setResponseStatus(responseStatus);
			return rsp;
		}

		page.setTotalRows(docTmp.getInteger("total", 0));
		rsp.setMsgPage(page);
		rsp.getMsgHeader().setAdmin(true);

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
				hItem.put("Ten", doc.get("Ten"));
				hItem.put("NLap", doc.get("NLap"));
				hItem.put("MST", doc.get("MST"));
				hItem.put("DChi", doc.get("DChi"));
				hItem.put("SDThoai", doc.get("SDThoai"));
				hItem.put("DCTDTu", doc.get("DCTDTu"));
				
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

		Document docFind = new Document("IsDelete", new Document("$ne", true))
				.append("_id", objectId);

		Document docTmp = null;	
		MongoClient mongoClient = cfg.mongoClient();
		MongoCollection<Document> collection = mongoClient.getDatabase(cfg.dbName).getCollection("QLDSNMua");
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
