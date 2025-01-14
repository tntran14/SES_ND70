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
import vn.sesgroup.hddt.user.dao.MauHDtDao;
import vn.sesgroup.hddt.user.service.TCTNService;
import vn.sesgroup.hddt.utility.Constants;
import vn.sesgroup.hddt.utility.Json;

@Repository
@Transactional
public class MauHDImpl extends AbstractDAO implements MauHDtDao {
	private static final Logger log = LogManager.getLogger(MauHDImpl.class);
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
		String loaihd = commons.getTextJsonNode(jsonData.at("/LoaiHD")).trim().replaceAll("\\s+", " ");
		String phoi = commons.getTextJsonNode(jsonData.at("/Phoi")).replaceAll("\\s", "");
		String phoitest = commons.getTextJsonNode(jsonData.at("/PhoiTest")).trim().replaceAll("\\s+", " ");
		String khhd = commons.getTextJsonNode(jsonData.at("/KiHieuHD")).replaceAll("\\s", "");
		String macqt = commons.getTextJsonNode(jsonData.at("/MaCQT")).replaceAll("\\s", "");
		String yearCreated = commons.getTextJsonNode(jsonData.at("/NamPhatHanh")).trim().replaceAll("\\s+", " ");
		String kh = commons.getTextJsonNode(jsonData.at("/KiHieu")).trim().replaceAll("\\s+", " ");
		String macty = commons.getTextJsonNode(jsonData.at("/MaCty")).trim().replaceAll("\\s+", " ");
		String r1p = commons.getTextJsonNode(jsonData.at("/RowsInPage")).trim().replaceAll("\\s+", " ");
		String rnp = commons.getTextJsonNode(jsonData.at("/RowInPageMultiPage")).trim().replaceAll("\\s+", " ");
		String CharsInRow = commons.getTextJsonNode(jsonData.at("/CharsInRow")).trim().replaceAll("\\s+", " ");
		String logo = commons.getTextJsonNode(jsonData.at("/Logo")).trim().replaceAll("\\s+", " ");
		String nen = commons.getTextJsonNode(jsonData.at("/Nen")).trim().replaceAll("\\s+", " ");
		String qa = commons.getTextJsonNode(jsonData.at("/QA")).trim().replaceAll("\\s+", " ");
		String vien = commons.getTextJsonNode(jsonData.at("/Vien")).trim().replaceAll("\\s+", " ");

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
			String KHHDon = macqt + yearCreated + kh + macty;
			docFind = new Document("_id", objectId).append("IsActive", true).append("IsDelete",
					new Document("$ne", true));
			pipeline = new ArrayList<Document>();
			pipeline.add(new Document("$match", docFind));
			pipeline.add(new Document("$lookup", new Document("from", "Users").append("pipeline", Arrays.asList(
					new Document("$match",
							new Document("IssuerId", header.getIssuerId()).append("_id", objectIdUser)
									.append("IsActive", true).append("IsDelete", new Document("$ne", true))),
					new Document("$project", new Document("_id", 1).append("UserName", 1).append("FullName", 1)),
					new Document("$limit", 1))).append("as", "UserInfo")));
			pipeline.add(new Document("$unwind",
					new Document("path", "$UserInfo").append("preserveNullAndEmptyArrays", true)));
			pipeline.add(new Document("$lookup",
					new Document("from", "DMTemplates")
							.append("pipeline", Arrays.asList(new Document("$match", new Document("Name", phoitest))))
							.append("as", "DMTemplatesInfo")));	
			pipeline.add(new Document("$unwind",
					new Document("path", "$DMTemplatesInfo").append("preserveNullAndEmptyArrays", true)));

			pipeline.add(
					new Document("$lookup", 
						new Document("from", "DMMauSoKyHieu")
						.append("pipeline", 
							Arrays.asList(
								new Document("$match", 
									new Document("IssuerId", header.getIssuerId())
									.append("KHMSHDon", loaihd)
									.append("KHHDon", KHHDon)
								)
							)
						)
						.append("as", "DMMauSoKyHieu")
					)
				);
			new Document("$project", new Document("_id", 1));	
				pipeline.add(new Document("$unwind", new Document("path", "$DMMauSoKyHieu").append("preserveNullAndEmptyArrays", true)));
				

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
			if (docTmp.get("DMMauSoKyHieu") != null) {
				responseStatus = new MspResponseStatus(9999, "Mẫu số đã tồn tại");
				rsp.setResponseStatus(responseStatus);
				return rsp;
			}
			int r1pad = Integer.parseInt(r1p);
			int rnpad = Integer.parseInt(rnp);
			int CharsInRowad = Integer.parseInt(CharsInRow);
			String nams = 20 + yearCreated;
			int namph = Integer.parseInt(nams);
			ObjectId id = new ObjectId();
			
		
			/* LUU DU LIEU HD */
			docUpsert = new Document("_id", id).append("IssuerId", header.getIssuerId()).append("SHDHT", 0).append("KHMSHDon", loaihd)
					.append("KHHDon", KHHDon)
					.append("Templates",
							new Document("Code", docTmp.getEmbedded(Arrays.asList("DMTemplatesInfo", "Code"), ""))
									.append("Name", docTmp.getEmbedded(Arrays.asList("DMTemplatesInfo", "Name"), ""))
									
									.append("FileName",
											docTmp.getEmbedded(Arrays.asList("DMTemplatesInfo", "FileName"), ""))
									.append("ImgLogo", logo).append("ImgBackground", nen).append("ImgVien", vien).append("ImgQA", qa).append("RowsInPage", r1pad)
									.append("RowInPageMultiPage", rnpad).append("CharsInRow", CharsInRowad))
					.append("IsActive", false).append("IsDelete", false).append("NamPhatHanh", namph);
			/* END - LUU DU LIEU HD */
		
			mongoClient = cfg.mongoClient();
			collection = mongoClient.getDatabase(cfg.dbName).getCollection("DMMauSoKyHieu");
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
		
			 CharsInRowad = Integer.parseInt(CharsInRow);
			docFind = new Document("_id", objectId).append("IsDelete", new Document("$ne", true));
			pipeline = new ArrayList<Document>();
			pipeline.add(new Document("$lookup", new Document("from", "DMMauSoKyHieu")
					.append("pipeline", Arrays.asList(new Document("$match", docFind))).append("as", "DMMauSoKyHieu")));
			pipeline.add(new Document("$unwind",
					new Document("path", "$DMMauSoKyHieu").append("preserveNullAndEmptyArrays", true)));
			
			
			pipeline.add(new Document("$lookup",
					new Document("from", "DMTemplates")
							.append("pipeline", Arrays.asList(new Document("$match", new Document("Name", phoitest))))
							.append("as", "DMTemplatesInfo")));
			pipeline.add(new Document("$unwind",
					new Document("path", "$DMTemplatesInfo").append("preserveNullAndEmptyArrays", true)));

		
			
			mongoClient = cfg.mongoClient();
			collection = mongoClient.getDatabase(cfg.dbName).getCollection("DMMauSoKyHieu");

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
		if(logo == "") {
			logo = 	docTmp.getEmbedded(Arrays.asList("DMMauSoKyHieu","Templates", "ImgLogo"), "");
		}
			if(nen == "") {
				nen = 	docTmp.getEmbedded(Arrays.asList("DMMauSoKyHieu","Templates", "ImgBackground"), "");
					}
			if(qa == "") {
				qa = 	docTmp.getEmbedded(Arrays.asList("DMMauSoKyHieu","Templates", "ImgQA"), "");
					}
			if(vien == "") {
				vien = 	docTmp.getEmbedded(Arrays.asList("DMMauSoKyHieu","Templates", "ImgVien"), "");
					}
			
			String KHMSHDonfindu =   docTmp.getEmbedded(Arrays.asList("DMMauSoKyHieu", "KHMSHDon"), "");
			String KHHDonfindu =   docTmp.getEmbedded(Arrays.asList("DMMauSoKyHieu", "KHHDon"), "");
			
			docFind2 = new Document("IssuerId", header.getIssuerId())
			.append("EInvoiceDetail.TTChung.KHMSHDon", KHMSHDonfindu)
			.append("IsDelete",  new Document("$ne", true))
			.append("SignStatusCode",  Constants.INVOICE_SIGN_STATUS.SIGNED)
			.append("EInvoiceDetail.TTChung.KHHDon", KHHDonfindu);

			Document docTmp2 = null;
			
				 mongoClient = cfg.mongoClient();
				collection = mongoClient.getDatabase(cfg.dbName).getCollection("EInvoice");

				try {
					docTmp2 = collection.find(docFind2).allowDiskUse(true).iterator().next();
				} catch (Exception e) {

				}
				mongoClient.close();
				
				if(docTmp2 == null && !phoitest.equals("")) {
					options = new FindOneAndUpdateOptions();
					options.upsert(false);
					options.maxTime(5000, TimeUnit.MILLISECONDS);
					options.returnDocument(ReturnDocument.AFTER);
					int r1pad1 = Integer.parseInt(r1p);
					int rnpad1 = Integer.parseInt(rnp);
					String mskh = macqt + yearCreated + kh + macty ;
				
					mongoClient = cfg.mongoClient();
					collection = mongoClient.getDatabase(cfg.dbName).getCollection("DMMauSoKyHieu");
					docR =	collection.findOneAndUpdate(
							docFind,
							new Document("$set", 
									new Document("KHHDon", mskh)
									.append("Templates", 
										new Document("Code", docTmp.getEmbedded(Arrays.asList("DMTemplatesInfo", "Code"), ""))
												.append("Name", docTmp.getEmbedded(Arrays.asList("DMTemplatesInfo", "Name"), ""))
												.append("FileName",
														docTmp.getEmbedded(Arrays.asList("DMTemplatesInfo", "FileName"), ""))
												.append("ImgLogo", logo).append("ImgBackground", nen).append("ImgVien", vien).append("ImgQA", qa).append("RowsInPage", r1pad1)										
												.append("RowInPageMultiPage", rnpad1).append("CharsInRow", CharsInRowad))
							),
						options
					);		
					mongoClient.close();
					
					responseStatus = new MspResponseStatus(0, "SUCCESS");
					rsp.setResponseStatus(responseStatus);
					return rsp;
				}
				else {
					options = new FindOneAndUpdateOptions();
					options.upsert(false);
					options.maxTime(5000, TimeUnit.MILLISECONDS);
					options.returnDocument(ReturnDocument.AFTER);
					int r1pad1 = Integer.parseInt(r1p);
					int rnpad1 = Integer.parseInt(rnp);
				
					
					mongoClient = cfg.mongoClient();
					collection = mongoClient.getDatabase(cfg.dbName).getCollection("DMMauSoKyHieu");
					docR =	collection.findOneAndUpdate(
							docFind,
							new Document("$set", 
									new Document("Templates", 
										new Document("Code", docTmp.getEmbedded(Arrays.asList("DMTemplatesInfo", "Code"), ""))
												.append("Name", docTmp.getEmbedded(Arrays.asList("DMTemplatesInfo", "Name"), ""))
												.append("FileName",
														docTmp.getEmbedded(Arrays.asList("DMTemplatesInfo", "FileName"), ""))
												.append("ImgLogo", logo).append("ImgBackground", nen).append("ImgVien", vien).append("ImgQA", qa).append("RowsInPage", r1pad1)										
												.append("RowInPageMultiPage", rnpad1).append("CharsInRow", CharsInRowad))
							),
						options
					);			
					mongoClient.close();
					
					
					responseStatus = new MspResponseStatus(0, "SUCCESS");
					rsp.setResponseStatus(responseStatus);
					return rsp;
				}



///////////////////////////////////////////////////////////////////////////////////////
		case Constants.MSG_ACTION_CODE.DELETE:
			objectId = null;
			ObjectId objectIdIsu  = null;
			objectIdIsu = new ObjectId(header.getIssuerId());
		
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
						new Document("from", "DMMauSoKyHieu")
							.append("pipeline", 
								Arrays.asList(
									new Document("$match", 
										new Document("_id", objectId)	)
							)
							)
							.append("as", "DMMauSoKyHieu")
					)
				);
				pipeline.add(new Document("$unwind", new Document("path", "$DMMauSoKyHieu").append("preserveNullAndEmptyArrays", true)));
				pipeline.add(
						new Document("$lookup", 
							new Document("from", "Issuer")
								.append("pipeline", 
									Arrays.asList(
										new Document("$match", 
											new Document("_id", objectIdIsu)	)
								)
								)
								.append("as", "Issuer")
						)
					);
				pipeline.add(new Document("$unwind", new Document("path", "$Issuer").append("preserveNullAndEmptyArrays", true)));
		
			 mongoClient = cfg.mongoClient();
			collection = mongoClient.getDatabase(cfg.dbName).getCollection("DMMauSoKyHieu");

			try {
				docTmp = collection.aggregate(pipeline).allowDiskUse(true).iterator().next();
			} catch (Exception e) {

			}
			mongoClient.close();
			
			if (null == docTmp) {
				responseStatus = new MspResponseStatus(9999, "Không tìm thấy thông tin hóa đơn.");
				rsp.setResponseStatus(responseStatus);
				return rsp;
			}
		int cl =	docTmp.getEmbedded(Arrays.asList("DMMauSoKyHieu", "ConLai"), 0);	
		int sl =	docTmp.getEmbedded(Arrays.asList("DMMauSoKyHieu", "SoLuong"), 0);	
			String taxcode =   docTmp.getEmbedded(Arrays.asList("Issuer", "TaxCode"), "");
			docFind2 = new Document("TaxCode", taxcode);
			pipeline = new ArrayList<Document>();
			pipeline.add(
					new Document("$lookup", 
						new Document("from", "DMDepot")
						.append("pipeline", 
							Arrays.asList(
								new Document("$match", 
									new Document("TaxCode",taxcode)
								)
							)
						)
						.append("as", "DMDepot")
					)
				);
			pipeline.add(new Document("$unwind", new Document("path", "$DMDepot").append("preserveNullAndEmptyArrays", true)));
			
				
				
				 mongoClient = cfg.mongoClient();
					collection = mongoClient.getDatabase(cfg.dbName).getCollection("DMDepot");

					try {
						docTmp = collection.aggregate(pipeline).allowDiskUse(true).iterator().next();
					} catch (Exception e) {

					}
					mongoClient.close();
					
					
				int slhd =  docTmp.getEmbedded(Arrays.asList("DMDepot", "SLHDon"), 0);
				int sldd = 0;
				int slcl = 0;
				int sdd = 0;
				int scl = 0;
				
				
				
				if(slhd == 0) {
					 sldd = docTmp.getEmbedded(Arrays.asList("DMDepot", "SLHDonDD"), 0);
					 slcl =  docTmp.getEmbedded(Arrays.asList("DMDepot", "SLHDonCL"), 0);
					 sdd = sl - cl;
					 scl = slcl + cl;
				}
				else {
					 sldd = docTmp.getEmbedded(Arrays.asList("DMDepot", "SLHDonDD"), 0);
					 slcl =  docTmp.getEmbedded(Arrays.asList("DMDepot", "SLHDonCL"), 0);
					 sdd = sldd - cl;
					 scl = slcl + cl;
				}
				

				
				
				if(docTmp.get("DMDepot") == null) {
					/* LUU DU LIEU HD */
					docUpsert = new Document("IssuerId", header.getIssuerId()).append("TaxCode", taxcode).append("SLHDon", 0).append("SLHDonDD", loaihd)
							.append("SLHDonDD", 0).append("SLHDonCL", 0).append("IsRoot", false).append("IsDelete", false);
					/* END - LUU DU LIEU HD */
					
					mongoClient = cfg.mongoClient();
					collection = mongoClient.getDatabase(cfg.dbName).getCollection("DMDepot");
					collection.insertOne(docUpsert);			
					mongoClient.close();
					
				}
	
				
			options = new FindOneAndUpdateOptions();
			options.upsert(false);
			options.maxTime(5000, TimeUnit.MILLISECONDS);
			options.returnDocument(ReturnDocument.AFTER);

			
			mongoClient = cfg.mongoClient();
			collection = mongoClient.getDatabase(cfg.dbName).getCollection("DMMauSoKyHieu");
			docR =	collection.findOneAndUpdate(docFind,
					new Document("$set", new Document("IsDelete", true).append("InfoDeleted",
							new Document("DeletedDate", LocalDateTime.now()).append("DeletedUserID", header.getUserId())
									.append("DeletedUserName", header.getUserName())
									.append("DeletedUserFullName", header.getUserFullName()))),
					options);			
			mongoClient.close();
			
			
			options2 = new FindOneAndUpdateOptions();
			options2.upsert(false);
			options2.maxTime(5000, TimeUnit.MILLISECONDS);
			options2.returnDocument(ReturnDocument.AFTER);
			
			if(slhd == 0) {
					mongoClient = cfg.mongoClient();
					collection = mongoClient.getDatabase(cfg.dbName).getCollection("DMDepot");
					 docRDp = collection.findOneAndUpdate(docFind2,
							new Document("$set", 
									new Document("SLHDonDD", sdd)
									.append("SLHDon",sl)
									.append("SLHDonCL",scl)
									),options2);			
					mongoClient.close();
					
			}
			else { 
					mongoClient = cfg.mongoClient();
					collection = mongoClient.getDatabase(cfg.dbName).getCollection("DMDepot");
					 docRDp = collection.findOneAndUpdate(docFind2,
								new Document("$set", 
										new Document("SLHDonDD", sdd)

										.append("SLHDonCL",scl)
										),options2);		
					mongoClient.close();		
			}

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
		
			 mongoClient = cfg.mongoClient();
			 collection = mongoClient.getDatabase(cfg.dbName).getCollection("DMMauSoKyHieu");

			try {
				docTmp = collection.find(docFind).allowDiskUse(true).iterator().next();
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
			collection = mongoClient.getDatabase(cfg.dbName).getCollection("DMMauSoKyHieu");
			docR =	collection.findOneAndUpdate(
					docFind
					, new Document("$set", 
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
		
			
			 mongoClient = cfg.mongoClient();
			collection = mongoClient.getDatabase(cfg.dbName).getCollection("DMMauSoKyHieu");

			try {
				docTmp = collection.find(docFind).allowDiskUse(true).iterator().next();
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
			collection = mongoClient.getDatabase(cfg.dbName).getCollection("DMMauSoKyHieu");
			docR = 	collection.findOneAndUpdate(
					docFind
					, new Document("$set", 
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
		
		 String loaihd = null;
		 String mausohd = null;
		
		JsonNode jsonData = null;
		if(objData != null) {
			jsonData = Json.serializer().nodeFromObject(objData);	
			loaihd = commons.getTextJsonNode(jsonData.at("/LoaiHD")).replaceAll("\\s", "");
			mausohd = commons.getTextJsonNode(jsonData.at("/MauSoHD")).replaceAll("\\s", "");
		}

		MsgRsp rsp = new MsgRsp(header);
		MspResponseStatus responseStatus = null;
		
		ObjectId objectId = null;
		Document docTmp = null;
		List<Document> pipeline = new ArrayList<Document>();
		
		
		
		Document docMatch = new Document("IssuerId", header.getIssuerId());
		if(!"".equals(loaihd))
			docMatch.append("KHMSHDon", commons.regexEscapeForMongoQuery(loaihd));
		if(!"".equals(mausohd))
			docMatch.append("KHHDon", commons.stringToInteger(mausohd));
		

		Document fillter = new Document("_id", 1).append("KHMSHDon", 1).append("KHHDon", 1)
				.append("Templates", 1).append("KHMSHDon", 1).append("NgayTao", 1).append("IsDelete", 1).append("IsActive", 1);		
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
				hItem = new HashMap<String, Object>();
				hItem.put("_id", objectId.toString());
				hItem.put("MauSo",doc.get("KHMSHDon").toString() + doc.get("KHHDon").toString());
				hItem.put("PhoiHD",doc.getEmbedded(Arrays.asList("Templates", "Name"), ""));
				hItem.put("LoaiHD", doc.get("KHMSHDon"));
				hItem.put("NgayTao", doc.get("NgayTao"));
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
		MongoCollection<Document> collection = mongoClient.getDatabase(cfg.dbName).getCollection("DMTemplates");

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
