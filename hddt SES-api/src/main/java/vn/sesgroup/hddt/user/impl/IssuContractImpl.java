package vn.sesgroup.hddt.user.impl;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.concurrent.TimeUnit;

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
import com.mongodb.client.model.FindOneAndUpdateOptions;
import com.mongodb.client.model.ReturnDocument;
import com.mongodb.client.model.UpdateOptions;

import vn.sesgroup.hddt.user.dao.AbstractDAO;
import vn.sesgroup.hddt.user.dao.IssuContractDao;
import vn.sesgroup.hddt.user.service.TCTNService;
import vn.sesgroup.hddt.utility.Constants;
import vn.sesgroup.hddt.utility.Json;

@Repository
@Transactional
public class IssuContractImpl extends AbstractDAO implements IssuContractDao {
	@Autowired
	MongoTemplate mongoTemplate;
	@Autowired
	TCTNService tctnService;

	String SUMslhd = "";

	@Transactional(rollbackFor = { Exception.class })
	@Override
	public MsgRsp crud(JSONRoot jsonRoot) throws Exception {
		Msg msg = jsonRoot.getMsg();
		MsgHeader header = msg.getMsgHeader();
		MsgPage page = msg.getMsgPage();
		Object objData = msg.getObjData();
		List<Object> listDSHHDVu = new ArrayList<Object>();
		HashMap<String, Object> hItem = null;
		FindOneAndUpdateOptions options = null;

		JsonNode jsonData = null;
		if (objData != null) {
			jsonData = Json.serializer().nodeFromObject(msg.getObjData());
		} else {
			throw new Exception("Lỗi dữ liệu đầu vào");
		}

		String actionCode = header.getActionCode();
		String _id = commons.getTextJsonNode(jsonData.at("/_id")).replaceAll("\\s", "");
		String t = commons.getTextJsonNode(jsonData.at("/TaxCode")).trim().replaceAll("\\s+", "")
				.replaceAll("[+^%$#@&]*", "");
		String n = commons.getTextJsonNode(jsonData.at("/Name"));
		String a = commons.getTextJsonNode(jsonData.at("/Address"));
		String p = commons.getTextJsonNode(jsonData.at("/Phone")).replaceAll("\\s", "");
		String hinhThucThanhToan = commons.getTextJsonNode(jsonData.at("/HinhThucThanhToan")).replaceAll("\\s", "");
		String hinhThucThanhToanText = commons.getTextJsonNode(jsonData.at("/HinhThucThanhToanText")).trim()
				.replaceAll("\\s+", " ");
		String boss = commons.getTextJsonNode(jsonData.at("/MainUser")).trim().replaceAll("\\s+", " ");
		String shd = commons.getTextJsonNode(jsonData.at("/SoHoaDon")).trim().replaceAll("\\s+", " ");
		String slhd = commons.getTextJsonNode(jsonData.at("/SoLuongHoaDon")).trim().replaceAll("\\s+", " ");
		String ngayky = commons.getTextJsonNode(jsonData.at("/NgayKy")).trim().replaceAll("\\s+", " ");
		String ngaytt = commons.getTextJsonNode(jsonData.at("/NgayThanhToan")).trim().replaceAll("\\s+", " ");
		String gc = commons.getTextJsonNode(jsonData.at("/GhiChu")).trim().replaceAll("\\s+", " ");
		String km = commons.getTextJsonNode(jsonData.at("/KhuyenMai")).trim().replaceAll("\\s+", " ");
		String tongTienTruocThue = commons.getTextJsonNode(jsonData.at("/TongTienTruocThue")).trim().replaceAll("\\s+",
				" ");
		String tongTienThueGtgt = commons.getTextJsonNode(jsonData.at("/TongTienThueGtgt")).trim().replaceAll("\\s+",
				" ");
		String tongTienDaCoThue = commons.getTextJsonNode(jsonData.at("/TongTienDaCoThue")).trim().replaceAll("\\s+",
				" ");
		String tienBangChu = commons.getTextJsonNode(jsonData.at("/TienBangChu")).trim().replaceAll("\\s+", " ");

		String e = commons.getTextJsonNode(jsonData.at("/Email"));
		String w = commons.getTextJsonNode(jsonData.at("/Website"));
		String ac = commons.getTextJsonNode(jsonData.at("/AccountNumber"));
		String an = commons.getTextJsonNode(jsonData.at("/AccountName"));
		String bn = commons.getTextJsonNode(jsonData.at("/BankName"));
		String cv = commons.getTextJsonNode(jsonData.at("/Position"));

		MsgRsp rsp = new MsgRsp(header);
		rsp.setMsgPage(page);
		MspResponseStatus responseStatus = null;

		ObjectId objectId = null;
		ObjectId objectIdUser = null;

		List<Document> pipeline = null;

		Document docFind = null;
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
			pipeline.add(new Document("$lookup", new Document("from", "Users")
					.append("pipeline", Arrays.asList(
					new Document("$match",
							new Document("IssuerId", header.getIssuerId()).append("_id", objectIdUser)
									.append("IsActive", true)
									.append("IsDelete", new Document("$ne", true))),
					new Document("$project", new Document("_id", 1).append("UserName", 1).append("FullName", 1)),
					new Document("$limit", 1))).append("as", "UserInfo")));
			pipeline.add(new Document("$unwind",
					new Document("path", "$UserInfo").append("preserveNullAndEmptyArrays", true)));
			
			cursor = mongoTemplate.getCollection("Issuer").aggregate(pipeline);
			iter = cursor.iterator();
			int slhddb = commons.stringToInteger(slhd);
			if (iter.hasNext()) {
				docTmp = iter.next();
			}

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

			ObjectId id = new ObjectId();
			String tmp = "";
			if (!jsonData.at("/DSSanPham").isMissingNode()) {
				for (JsonNode o : jsonData.at("/DSSanPham")) {
					if (!"".equals(commons.getTextJsonNode(o.at("/ProductName")))) {
						tmp = commons.getTextJsonNode(o.at("/VATRate")).replaceAll(",", "");
						switch (tmp) {
						case "0":
						case "5":
						case "8":
						case "10":
							tmp += "%";
							break;
						case "-1":
							tmp = "KCT";
							break;
						case "-2":
							tmp = "KKKNT";
							break;
						default:
							break;
						}
					}
					hItem = new LinkedHashMap<String, Object>();
					hItem.put("STT", commons.getTextJsonNode(o.at("/STT")));
					hItem.put("ProductName", commons.getTextJsonNode(o.at("/ProductName")));
					hItem.put("Quantity", commons.ToNumber(commons.getTextJsonNode(o.at("/Quantity"))));
					hItem.put("Price", commons.ToNumber(commons.getTextJsonNode(o.at("/Price"))));
					hItem.put("Total", commons.ToNumber(commons.getTextJsonNode(o.at("/Total"))));
					hItem.put("VATRate", commons.ToNumber(commons.getTextJsonNode(o.at("/VATRate"))));
					hItem.put("VATAmount", commons.ToNumber(commons.getTextJsonNode(o.at("/VATAmount"))));
					hItem.put("Amount", commons.ToNumber(commons.getTextJsonNode(o.at("/Amount"))));
					hItem.put("Note", commons.getTextJsonNode(o.at("/Note")));
					hItem.put("CreateDate", LocalDateTime.now());
					hItem.put("CreateUserName", header.getUserName());
					listDSHHDVu.add(hItem);

				}

			}

			if (ngaytt == "") {
				ngaytt = ngayky;
			}
			/* LUU DU LIEU HD */
			docUpsert = new Document("_id", id)
					.append("Contract", new Document("SHDon", shd)
							.append("SLHDon", slhddb)
							.append("KhuyenMai", km)
							.append("GhiChu", gc)
							.append("NgayThanhToan",
									commons.convertStringToLocalDate(ngaytt, Constants.FORMAT_DATE.FORMAT_DATE_WEB))
							.append("NgayKy",
									commons.convertStringToLocalDate(ngayky, Constants.FORMAT_DATE.FORMAT_DATE_WEB))
							.append("HTTToanCode", hinhThucThanhToan).append("HTTToan", hinhThucThanhToanText))
					.append("NMUA", new Document("TaxCode", t)
							.append("Name", n)
							.append("Address", a)
							.append("Phone", p)
							.append("Email", e)
							.append("Website", w)
							.append("MainUser", boss)
							.append("Position", cv)
							.append("BankAccount",
									new Document("AccountNumber", ac).append("AccountName", an).append("BankName", bn)))
					.append("DSHHDVu", listDSHHDVu)
					.append("TToan", new Document("TgTCThue", commons.ToNumber(tongTienTruocThue))
							.append("TgTThue", commons.ToNumber(tongTienThueGtgt))
							.append("TgTTTBSo", commons.ToNumber(tongTienDaCoThue)).append("TgTTTBChu", tienBangChu))
					.append("IsActive", false)
					.append("IsDelete", false)
					.append("IsActiveApprove", false)
					.append("InfoCreated",
							new Document("CreateDate", LocalDateTime.now())
							.append("CreateUserID", header.getUserId())
									.append("CreateUserName", header.getUserName())
									.append("CreateUserFullName", header.getUserFullName()));
			/* END - LUU DU LIEU HD */
			mongoTemplate.getCollection("Contract").insertOne(docUpsert);


			responseStatus = new MspResponseStatus(0, "SUCCESS");
			rsp.setResponseStatus(responseStatus);
			return rsp;
///////////////////////////////////////////////////////////////////////////////////////
			
		case Constants.MSG_ACTION_CODE.DELETE:
			List<ObjectId> objectIds = new ArrayList<ObjectId>();
			try {
				if (!jsonData.at("/ids").isMissingNode()) {
					for (JsonNode o : jsonData.at("/ids")) {
						try {
							objectIds.add(new ObjectId(o.asText("")));
						} catch (Exception ex) {
						}
					}
				}
			} catch (Exception ex) {
			}

			UpdateOptions updateOptions = new UpdateOptions();
			updateOptions.upsert(false);

			docFind = new Document("IsDelete", new Document("$ne", true))
					.append("_id", new Document("$in", objectIds));

			mongoTemplate.getCollection("Contract").updateMany(docFind,
					new Document("$set", new Document("IsDelete", true)
							.append("InfoDeleted",
							new Document("DeletedDate", LocalDateTime.now()).append("DeletedUserID", header.getUserId())
									.append("DeletedUserName", header.getUserName())
									.append("DeletedUserFullName", header.getUserFullName()))),
					updateOptions);

			responseStatus = new MspResponseStatus(0, "SUCCESS");
			rsp.setResponseStatus(responseStatus);
			return rsp;
//////////////////////////////////////////////////////////////////////////////////////
		case Constants.MSG_ACTION_CODE.ACTIVE:
//			objectId = null;
//			try {
//				objectId = new ObjectId(_id);
//			} catch (Exception ex) {
//			}
//			/* KIEM TRA THONG TIN HOPDONG CO TON TAI KHONG */
//			docFind = new Document("_id", objectId);
//			docTmp = null;
//			cursor = mongoTemplate.getCollection("Contract").find(docFind);
//			iter = cursor.iterator();
//			if (iter.hasNext()) {
//				docTmp = iter.next();
//			}
//			if (null == docTmp) {
//				responseStatus = new MspResponseStatus(9999, "Không tìm thấy thông tin hóa đơn.");
//				rsp.setResponseStatus(responseStatus);
//				return rsp;
//			}
//
//			options = new FindOneAndUpdateOptions();
//			options.upsert(false);
//			options.maxTime(5000, TimeUnit.MILLISECONDS);
//			options.returnDocument(ReturnDocument.AFTER);
//
//			mongoTemplate.getCollection("Contract").findOneAndUpdate(docFind,
//					new Document("$set", new Document("IsActive", true).append("InfoDeleted",
//							new Document("DeletedDate", LocalDateTime.now()).append("DeletedUserID", header.getUserId())
//									.append("DeletedUserName", header.getUserName())
//									.append("DeletedUserFullName", header.getUserFullName()))),
//					options);
			
			objectIds = new ArrayList<ObjectId>();
			try {
				if (!jsonData.at("/ids").isMissingNode()) {
					for (JsonNode o : jsonData.at("/ids")) {
						try {
							objectIds.add(new ObjectId(o.asText("")));
						} catch (Exception ex) {
						}
					}
				}
			} catch (Exception ex) {
			}
			
			
			for (ObjectId objectId_ : objectIds) {
							
			docFind = new Document("_id", objectId_)
			.append("IsDelete", new Document("$ne", true));
						
			pipeline = new ArrayList<Document>();
			pipeline.add(new Document("$match", docFind));
						
			cursor = mongoTemplate.getCollection("Contract").aggregate(pipeline);
			iter = cursor.iterator();
			if (iter.hasNext()) {
				docTmp = iter.next();
			}

			if (null == docTmp) {
				responseStatus = new MspResponseStatus(9999, "Không tìm thấy thông tin hợp đồng.");
				rsp.setResponseStatus(responseStatus);
				return rsp;
			}
			
			int SLHDonCu = 0;
			boolean CheckActive = docTmp.get("CheckActive", false);
			
			if(CheckActive == true) {
				SLHDonCu = docTmp.getEmbedded(Arrays.asList("Contract", "SLHDonCu"), 0);
			}
		
			Document docTmp1 = null;
			String MST = docTmp.getEmbedded(Arrays.asList("NMUA", "TaxCode"), "");	
			int SLHDonBD = docTmp.getEmbedded(Arrays.asList("Contract", "SLHDon"), 0);
			Document findDMDepot = new Document("TaxCode", MST).append("IsDelete", new Document("$ne", true));

			Iterable<Document> cursor1 = mongoTemplate.getCollection("DMDepot").find(findDMDepot);
			Iterator<Document> iter1 = cursor1.iterator();
			if (iter1.hasNext()) {
				docTmp1 = iter1.next();
			}
			
			
			Document findIssuer = new Document("TaxCode", MST).append("IsDelete", new Document("$ne", true));
			if(SLHDonCu == 0) {
									
				if (docTmp1 == null) {
				
					ObjectId idDMDepot = new ObjectId();
					Document docUpsertUser = null;
					int slhdondd = 0;
					docUpsertUser = new Document("_id", idDMDepot)
							.append("TaxCode", MST)
							.append("SLHDon", SLHDonBD)
							.append("SLHDonDD", slhdondd)
							.append("SLHDonCL", SLHDonBD)
							.append("IsRoot", false)
							.append("IsDelete", false)
							.append("InfoCreated",
									new Document("CreateDate", LocalDateTime.now())
											.append("CreateUserID", header.getUserId())
											.append("CreateUserName", header.getUserName())
											.append("CreateUserFullName", header.getUserFullName()));
					/* END - LUU DU LIEU */
					mongoTemplate.getCollection("DMDepot").insertOne(docUpsertUser);	
			
					options = new FindOneAndUpdateOptions();
					options.upsert(false);
					options.maxTime(5000, TimeUnit.MILLISECONDS);
					options.returnDocument(ReturnDocument.AFTER);

					mongoTemplate.getCollection("Contract").findOneAndUpdate(docFind,
							new Document("$set", new Document("IsActive", true)
									.append("CheckActive", true)
									.append("InfoActived",
									new Document("ActivedDate", LocalDateTime.now())
									.append("ActivedUserID", header.getUserId())
											.append("ActivedUserName", header.getUserName())
											.append("ActivedUserFullName", header.getUserFullName()))),
							options);
			
								
			} else {
												
				int getSLHDon = docTmp1.get("SLHDon", 0);
				int getSLHDonCL = docTmp1.get("SLHDonCL", 0);			
				
				int SLHDon = getSLHDon + SLHDonBD;
				int SLHDonCL = getSLHDonCL + SLHDonBD;
				options = new FindOneAndUpdateOptions();
				options.upsert(false);
				options.maxTime(5000, TimeUnit.MILLISECONDS);
				options.returnDocument(ReturnDocument.AFTER);
				mongoTemplate.getCollection("DMDepot").findOneAndUpdate(findDMDepot,
						new Document("$set",
								new Document("SLHDon", SLHDon)
								.append("SLHDonCL", SLHDonCL)
								.append("InfoUpdated",
										new Document("UpdatedDate", LocalDateTime.now())
												.append("UpdatedUserID", header.getUserId())
												.append("UpdatedUserName", header.getUserName())
												.append("UpdatedUserFullName", header.getUserFullName()))

						), options);
				
				
				options = new FindOneAndUpdateOptions();
				options.upsert(false);
				options.maxTime(5000, TimeUnit.MILLISECONDS);
				options.returnDocument(ReturnDocument.AFTER);

				mongoTemplate.getCollection("Contract").findOneAndUpdate(docFind,
						new Document("$set", new Document("IsActive", true)
								.append("CheckActive", true)
								.append("InfoActived",
								new Document("ActivedDate", LocalDateTime.now())
								.append("ActivedUserID", header.getUserId())
										.append("ActivedUserName", header.getUserName())
										.append("ActivedUserFullName", header.getUserFullName()))),
						options);
				
			}	
				
				
				//TAI KHOAN ISSUER 
				options = new FindOneAndUpdateOptions();
				options.upsert(false);
				options.maxTime(5000, TimeUnit.MILLISECONDS);
				options.returnDocument(ReturnDocument.AFTER);

				mongoTemplate.getCollection("Issuer").findOneAndUpdate(findIssuer,
						new Document("$set", new Document("IsActive", true)
								.append("InfoActived",
								new Document("ActivedDate", LocalDateTime.now()).append("ActivedUserID", header.getUserId())
										.append("ActivedUserName", header.getUserName())
										.append("ActivedUserFullName", header.getUserFullName()))),
						options);
			
			}else {
				//CASE DA ACTIVE
				
				if (docTmp1 == null) {
					responseStatus = new MspResponseStatus(9999, "Không tìm thấy thông tin kho.");
					rsp.setResponseStatus(responseStatus);
					return rsp;
				}
				
				//HANDLE WAREHOUSE
				int getSLHDon = docTmp1.get("SLHDon", 0);
				int getSLHDonCL = docTmp1.get("SLHDonCL", 0);	
				int SLHDon = 0;
				int SLHDonCL = 0;
				
				if(SLHDonBD > SLHDonCu) {
					int getVariable = SLHDonBD - SLHDonCu;
					SLHDon = getSLHDon + getVariable;
					SLHDonCL = getSLHDonCL + getVariable;
				}else {
					int getVariable = SLHDonCu - SLHDonBD;
					
					if(getSLHDonCL < getVariable) {
						responseStatus = new MspResponseStatus(9999, "Số lượng kho không đủ để hủy kích hoạt hợp đồng!!!");
						rsp.setResponseStatus(responseStatus);
						return rsp;
					}
					SLHDon = getSLHDon - getVariable;
					SLHDonCL = getSLHDonCL - getVariable;
				}
								
				options = new FindOneAndUpdateOptions();
				options.upsert(false);
				options.maxTime(5000, TimeUnit.MILLISECONDS);
				options.returnDocument(ReturnDocument.AFTER);
				mongoTemplate.getCollection("DMDepot").findOneAndUpdate(findDMDepot,
						new Document("$set",
								new Document("SLHDon", SLHDon)
								.append("SLHDonCL", SLHDonCL)
								.append("InfoUpdated",
										new Document("UpdatedDate", LocalDateTime.now())
												.append("UpdatedUserID", header.getUserId())
												.append("UpdatedUserName", header.getUserName())
												.append("UpdatedUserFullName", header.getUserFullName()))

						), options);
				
				//END WAREHOUSE
				
				options = new FindOneAndUpdateOptions();
				options.upsert(false);
				options.maxTime(5000, TimeUnit.MILLISECONDS);
				options.returnDocument(ReturnDocument.AFTER);

				mongoTemplate.getCollection("Contract").findOneAndUpdate(docFind,
						new Document("$set", new Document("IsActive", true)
								.append("CheckActive", true)
								.append("InfoActived",
								new Document("ActivedDate", LocalDateTime.now())
								.append("ActivedUserID", header.getUserId())
										.append("ActivedUserName", header.getUserName())
										.append("ActivedUserFullName", header.getUserFullName()))),
						options);
				
				//TAI KHOAN ISSUER 
				options = new FindOneAndUpdateOptions();
				options.upsert(false);
				options.maxTime(5000, TimeUnit.MILLISECONDS);
				options.returnDocument(ReturnDocument.AFTER);

				mongoTemplate.getCollection("Issuer").findOneAndUpdate(findIssuer,
						new Document("$set", new Document("IsActive", true)
								.append("InfoActived",
								new Document("ActivedDate", LocalDateTime.now()).append("ActivedUserID", header.getUserId())
										.append("ActivedUserName", header.getUserName())
										.append("ActivedUserFullName", header.getUserFullName()))),
						options);
			}
		}			

			responseStatus = new MspResponseStatus(0, "SUCCESS");
			rsp.setResponseStatus(responseStatus);
			return rsp;
////////////////////////////////////////////////////////////////////////////////////////
		case Constants.MSG_ACTION_CODE.DEACTIVE:
//			objectId = null;
//			try {
//				objectId = new ObjectId(_id);
//			} catch (Exception ex) {
//			}
//			/* KIEM TRA THONG TIN HOPDONG CO TON TAI KHONG */
//			docFind = new Document("_id", objectId);
//			docTmp = null;
//			cursor = mongoTemplate.getCollection("Contract").find(docFind);
//			iter = cursor.iterator();
//			if (iter.hasNext()) {
//				docTmp = iter.next();
//			}
//			if (null == docTmp) {
//				responseStatus = new MspResponseStatus(9999, "Không tìm thấy thông tin hóa đơn.");
//				rsp.setResponseStatus(responseStatus);
//				return rsp;
//			}
//
//			options = new FindOneAndUpdateOptions();
//			options.upsert(false);
//			options.maxTime(5000, TimeUnit.MILLISECONDS);
//			options.returnDocument(ReturnDocument.AFTER);
//
//			mongoTemplate.getCollection("Contract").findOneAndUpdate(docFind,
//					new Document("$set", new Document("IsActive", false).append("InfoDeleted",
//							new Document("DeletedDate", LocalDateTime.now()).append("DeletedUserID", header.getUserId())
//									.append("DeletedUserName", header.getUserName())
//									.append("DeletedUserFullName", header.getUserFullName()))),
//					options);
//
//			responseStatus = new MspResponseStatus(0, "SUCCESS");
//			rsp.setResponseStatus(responseStatus);
//			return rsp;
			
			objectIds = new ArrayList<ObjectId>();
			try {
				if (!jsonData.at("/ids").isMissingNode()) {
					for (JsonNode o : jsonData.at("/ids")) {
						try {
							objectIds.add(new ObjectId(o.asText("")));
						} catch (Exception ex) {
						}
					}
				}
			} catch (Exception ex) {
			}
			
			
			for (ObjectId objectId_ : objectIds) {
							
			docFind = new Document("_id", objectId_)
			.append("IsDelete", new Document("$ne", true));
						
			pipeline = new ArrayList<Document>();
			pipeline.add(new Document("$match", docFind));
						
			cursor = mongoTemplate.getCollection("Contract").aggregate(pipeline);
			iter = cursor.iterator();
			if (iter.hasNext()) {
				docTmp = iter.next();
			}

			if (null == docTmp) {
				responseStatus = new MspResponseStatus(9999, "Không tìm thấy thông tin hợp đồng.");
				rsp.setResponseStatus(responseStatus);
				return rsp;
			}
			
			Document docTmp1 = null;
			String TaxCode = docTmp.getEmbedded(Arrays.asList("NMUA", "TaxCode"), "");			
			Document findIssuer = new Document("TaxCode", TaxCode).append("IsDelete", new Document("$ne", true));
		
			Iterable<Document> cursor1 = mongoTemplate.getCollection("Issuer").find(findIssuer).allowDiskUse(true);
			Iterator<Document> iter1 = cursor1.iterator();
			if (iter1.hasNext()) {
				docTmp1 = iter1.next();
			}
			if(docTmp1 == null) {
				responseStatus = new MspResponseStatus(9999, "Không tìm thấy thông tin khách hàng.");
				rsp.setResponseStatus(responseStatus);
				return rsp;
			}
		
			
			options = new FindOneAndUpdateOptions();
			options.upsert(false);
			options.maxTime(5000, TimeUnit.MILLISECONDS);
			options.returnDocument(ReturnDocument.AFTER);

			mongoTemplate.getCollection("Contract").findOneAndUpdate(docFind,
					new Document("$set", new Document("IsActive", false)
							.append("InfoActived",
							new Document("ActivedDate", LocalDateTime.now()).append("ActivedUserID", header.getUserId())
									.append("ActivedUserName", header.getUserName())
									.append("ActivedUserFullName", header.getUserFullName()))),
					options);
			
			//TAI KHOAN ISSUER 
			options = new FindOneAndUpdateOptions();
			options.upsert(false);
			options.maxTime(5000, TimeUnit.MILLISECONDS);
			options.returnDocument(ReturnDocument.AFTER);

			mongoTemplate.getCollection("Issuer").findOneAndUpdate(findIssuer,
					new Document("$set", new Document("IsActive", false)
							.append("InfoActived",
							new Document("ActivedDate", LocalDateTime.now()).append("ActivedUserID", header.getUserId())
									.append("ActivedUserName", header.getUserName())
									.append("ActivedUserFullName", header.getUserFullName()))),
					options);
			
		}
		
			responseStatus = new MspResponseStatus(0, "SUCCESS");
			rsp.setResponseStatus(responseStatus);
			return rsp;	
			
//////////////////////////////////////////////////////////////////////////////////////
			
		case Constants.MSG_ACTION_CODE.APPROVE:
			
			objectIds = new ArrayList<ObjectId>();
			try {
				if (!jsonData.at("/ids").isMissingNode()) {
					for (JsonNode o : jsonData.at("/ids")) {
						try {
							objectIds.add(new ObjectId(o.asText("")));
						} catch (Exception ex) {
						}
					}
				}
			} catch (Exception ex) {
			}
			
			
			for (ObjectId objectId_ : objectIds) {
							
			docFind = new Document("_id", objectId_)
			.append("IsDelete", new Document("$ne", true));
						
			pipeline = new ArrayList<Document>();
			pipeline.add(new Document("$match", docFind));
						
			cursor = mongoTemplate.getCollection("Contract").aggregate(pipeline);
			iter = cursor.iterator();
			if (iter.hasNext()) {
				docTmp = iter.next();
			}

			if (null == docTmp) {
				responseStatus = new MspResponseStatus(9999, "Không tìm thấy thông tin hợp đồng.");
				rsp.setResponseStatus(responseStatus);
				return rsp;
			}
			
			Document docTmp1 = null;
			String TaxCode = docTmp.getEmbedded(Arrays.asList("NMUA", "TaxCode"), "");			
			Document findIssuer = new Document("TaxCode", TaxCode).append("IsDelete", new Document("$ne", true));
		
			Iterable<Document> cursor1 = mongoTemplate.getCollection("Issuer").find(findIssuer).allowDiskUse(true);
			Iterator<Document> iter1 = cursor1.iterator();
			if (iter1.hasNext()) {
				docTmp1 = iter1.next();
			}
			if(docTmp1 == null) {
				responseStatus = new MspResponseStatus(9999, "Không tìm thấy thông tin khách hàng.");
				rsp.setResponseStatus(responseStatus);
				return rsp;
			}
			
			// UPDATE HUY DUYET HOA DON
			boolean IsActiveApprove = true;
			if(null != docTmp.get("IsActiveApprove") && docTmp.get("IsActiveApprove") instanceof Boolean) {
				IsActiveApprove = docTmp.getBoolean("IsActiveApprove", true);
			}
			
			if(IsActiveApprove == false) {
			options = new FindOneAndUpdateOptions();
			options.upsert(false);
			options.maxTime(5000, TimeUnit.MILLISECONDS);
			options.returnDocument(ReturnDocument.AFTER);

			mongoTemplate.getCollection("Contract").findOneAndUpdate(docFind,
					new Document("$set", new Document("IsActiveApprove", !IsActiveApprove)
							.append("InfoApproved",
							new Document("ApprovedDate", LocalDateTime.now()).append("ApprovedUserID", header.getUserId())
									.append("ApprovedUserName", header.getUserName())
									.append("ApprovedUserFullName", header.getUserFullName()))),
					options);
			
			//TAI KHOAN ISSUER 
			options = new FindOneAndUpdateOptions();
			options.upsert(false);
			options.maxTime(5000, TimeUnit.MILLISECONDS);
			options.returnDocument(ReturnDocument.AFTER);

			mongoTemplate.getCollection("Issuer").findOneAndUpdate(findIssuer,
					new Document("$set", new Document("IsActive", true)
							.append("InfoApproved",
							new Document("ApprovedDate", LocalDateTime.now()).append("ApprovedUserID", header.getUserId())
									.append("ApprovedUserName", header.getUserName())
									.append("ApprovedUserFullName", header.getUserFullName()))),
					options);
			}else {
				
				options = new FindOneAndUpdateOptions();
				options.upsert(false);
				options.maxTime(5000, TimeUnit.MILLISECONDS);
				options.returnDocument(ReturnDocument.AFTER);

				mongoTemplate.getCollection("Contract").findOneAndUpdate(docFind,
						new Document("$set", new Document("IsActiveApprove", !IsActiveApprove)
								.append("InfoApproved",
								new Document("ApprovedDate", LocalDateTime.now()).append("ApprovedUserID", header.getUserId())
										.append("ApprovedUserName", header.getUserName())
										.append("ApprovedUserFullName", header.getUserFullName()))),
						options);
				
				//TAI KHOAN ISSUER 
				options = new FindOneAndUpdateOptions();
				options.upsert(false);
				options.maxTime(5000, TimeUnit.MILLISECONDS);
				options.returnDocument(ReturnDocument.AFTER);

				mongoTemplate.getCollection("Issuer").findOneAndUpdate(findIssuer,
						new Document("$set", new Document("IsActive", false)
								.append("InfoApproved",
								new Document("ApprovedDate", LocalDateTime.now()).append("ApprovedUserID", header.getUserId())
										.append("ApprovedUserName", header.getUserName())
										.append("ApprovedUserFullName", header.getUserFullName()))),
						options);
			}
			
		}
		
			responseStatus = new MspResponseStatus(0, "SUCCESS");
			rsp.setResponseStatus(responseStatus);
			return rsp;	
		case Constants.MSG_ACTION_CODE.MODIFY:

			objectId = null;
			
			objectIdUser = null;			
			try {
				objectIdUser = new ObjectId(header.getUserId());
			} catch (Exception ex) {
			}
			try {
				objectId = new ObjectId(_id);
			} catch (Exception ex) {
			}
			
			docFind = new Document("_id", objectId)
			.append("IsDelete", new Document("$ne", true));
			
			pipeline = new ArrayList<Document>();
			pipeline.add(new Document("$match", docFind));
			cursor = mongoTemplate.getCollection("Contract").aggregate(pipeline);
			iter = cursor.iterator();
			if (iter.hasNext()) {
				docTmp = iter.next();
			}

			if (null == docTmp) {
				responseStatus = new MspResponseStatus(9999, "Không tìm thấy thông tin hợp đồng.");
				rsp.setResponseStatus(responseStatus);
				return rsp;
			}
			
			int SLHDonCu = 0;
			boolean checkActive = docTmp.get("CheckActive", false);
			if(checkActive == true) {
				SLHDonCu = docTmp.getEmbedded(Arrays.asList("Contract", "SLHDon"), 0);
			}
			
			slhddb = commons.stringToInteger(slhd);

			if (!jsonData.at("/DSSanPham").isMissingNode()) {
				for (JsonNode o : jsonData.at("/DSSanPham")) {
					if (!"".equals(commons.getTextJsonNode(o.at("/ProductName")))) {
						tmp = commons.getTextJsonNode(o.at("/VATRate")).replaceAll(",", "");
						switch (tmp) {
						case "0":
						case "5":
						case "8":
						case "10":
							tmp += "%";
							break;
						case "-1":
							tmp = "KCT";
							break;
						case "-2":
							tmp = "KKKNT";
							break;
						default:
							break;
						}
					}
					hItem = new LinkedHashMap<String, Object>();
					hItem.put("STT", commons.getTextJsonNode(o.at("/STT")));
					hItem.put("ProductName", commons.getTextJsonNode(o.at("/ProductName")));
					hItem.put("Quantity", commons.ToNumber(commons.getTextJsonNode(o.at("/Quantity"))));
					hItem.put("Price", commons.ToNumber(commons.getTextJsonNode(o.at("/Price"))));
					hItem.put("Total", commons.ToNumber(commons.getTextJsonNode(o.at("/Total"))));
					hItem.put("VATRate", commons.ToNumber(commons.getTextJsonNode(o.at("/VATRate"))));
					hItem.put("VATAmount", commons.ToNumber(commons.getTextJsonNode(o.at("/VATAmount"))));
					hItem.put("Amount", commons.ToNumber(commons.getTextJsonNode(o.at("/Amount"))));
					hItem.put("Note", commons.getTextJsonNode(o.at("/Note")));
					hItem.put("CreateDate", LocalDateTime.now());
					hItem.put("CreateUserName", header.getUserName());
					listDSHHDVu.add(hItem);

				}

			}

			/* LUU DU LIEU HD */
			docUpsert = new Document("TaxCode", t)
					.append("Name", n)
					.append("Address", a)
					.append("Phone", p)
					.append("Email", e)
					.append("Website", w)
					.append("MainUser", boss)
					.append("Position", cv)
					.append("BankAccount",
							new Document("AccountNumber", ac)
							.append("AccountName", an)
							.append("BankName", bn)

					);
			/* END - LUU DU LIEU HD */

			options = new FindOneAndUpdateOptions();
			options.upsert(false);
			options.maxTime(5000, TimeUnit.MILLISECONDS);
			options.returnDocument(ReturnDocument.AFTER);
			mongoTemplate.getCollection("Contract").findOneAndUpdate(docFind,
					new Document("$set",
							new Document("NMUA", docUpsert).append("DSHHDVu", listDSHHDVu).append("TToan",
									new Document("TgTCThue", commons.ToNumber(tongTienTruocThue))
											.append("TgTThue", commons.ToNumber(tongTienThueGtgt))
											.append("TgTTTBSo", commons.ToNumber(tongTienDaCoThue))
											.append("TgTTTBChu", tienBangChu))
									.append("InfoUpdated",
											new Document("UpdatedDate", LocalDateTime.now())
													.append("UpdatedUserID", header.getUserId())
													.append("UpdatedUserName", header.getUserName())
													.append("UpdatedUserFullName", header.getUserFullName()))
									.append("Contract", new Document("SHDon", shd)
											.append("SLHDon", slhddb)
											.append("SLHDonCu", SLHDonCu)											
											.append("KhuyenMai", km)
											.append("GhiChu", gc)
											.append("NgayThanhToan",
													commons.convertStringToLocalDate(ngaytt,
															Constants.FORMAT_DATE.FORMAT_DATE_WEB))
											.append("NgayKy",
													commons.convertStringToLocalDate(ngayky,
															Constants.FORMAT_DATE.FORMAT_DATE_WEB))
											.append("HTTToanCode", hinhThucThanhToan)
											.append("HTTToan", hinhThucThanhToanText))),
					options);

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

		String shd = "";
		String mst = "";
//		String gh = "";
		String acti = "";
		JsonNode jsonData = null;
		if (objData != null) {
			jsonData = Json.serializer().nodeFromObject(objData);
			shd = commons.getTextJsonNode(jsonData.at("/SHDon")).replaceAll("\\s", "");
			mst = commons.getTextJsonNode(jsonData.at("/TaxCode")).replaceAll("\\s", "");
			acti = commons.getTextJsonNode(jsonData.at("/IsActive")).replaceAll("\\s", "");
		}	
		
		MsgRsp rsp = new MsgRsp(header);
		MspResponseStatus responseStatus = null;

		ObjectId objectId = null;
		Document docTmp = null;
		Iterable<Document> cursor = null;
		Iterator<Document> iter = null;
		List<Document> pipeline = new ArrayList<Document>();

		Document docMatch = new Document("IsDelete", new Document("$ne", true));
		if (!"".equals(shd))
			docMatch.append("Contract.SHDon", commons.regexEscapeForMongoQuery(shd));
		if (!"".equals(mst))
			docMatch.append("NMUA.TaxCode", commons.regexEscapeForMongoQuery(mst));
		if (!"".equals(acti)) {
			if(acti.equals("true")) {
				docMatch.append("$or", Arrays.asList(		
						new Document("IsActive", true).append("IsActiveApprove", true),
						new Document("IsActive", true).append("IsActiveApprove", false),
						new Document("IsActive", false).append("IsActiveApprove", true),
						new Document("IsActive", true).append("IsActiveApprove", null)			
						));
			}else {
				docMatch.append("$or", Arrays.asList(		
						new Document("IsActive", false).append("IsActiveApprove", false),
						new Document("IsActive", false).append("IsActiveApprove", null)						
						));
			
			}
		}
	

		pipeline = new ArrayList<Document>();
		pipeline.add(new Document("$match", docMatch));
		pipeline.add(new Document("$sort", new Document("_id", -1)));
		pipeline.addAll(createFacetForSearchNotSort(page));
		cursor = mongoTemplate.getCollection("Contract").aggregate(pipeline).allowDiskUse(true);
		iter = cursor.iterator();
		if (iter.hasNext()) {
			docTmp = iter.next();
		}

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
				hItem.put("Contract", doc.get("Contract"));
				hItem.put("NMUA", doc.get("NMUA"));
				hItem.put("IsActive", doc.get("IsActive"));
				hItem.put("InfoCreated", doc.get("InfoCreated"));
				hItem.put("IsActiveApprove", doc.get("IsActiveApprove", false));
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
		Iterable<Document> cursor = mongoTemplate.getCollection("Contract").find(docFind);
		Iterator<Document> iter = cursor.iterator();
		if (iter.hasNext()) {
			docTmp = iter.next();
		}

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
	public MsgRsp checkdb(JSONRoot jsonRoot) throws Exception {

		Msg msg = jsonRoot.getMsg();
		MsgHeader header = msg.getMsgHeader();

		MsgRsp rsp = new MsgRsp(header);
		MspResponseStatus responseStatus = null;

		Document docTmp = null;
		Iterable<Document> cursor = null;
		Iterator<Document> iter = null;
		List<Document> pipeline = new ArrayList<Document>();

		Document docMatch = new Document("IsDelete", new Document("$ne", true));
		pipeline = new ArrayList<Document>();
		pipeline.add(new Document("$match", docMatch));

		/*
		 * pipeline.add(new Document("$lookup", new Document("from", "DMDepot")
		 * .append("pipeline", Arrays.asList(new Document("$match", new
		 * Document("IsDelete", new Document("$ne", true)) ))) .append("as", "DMDepot")
		 * ));
		 */

		pipeline.add(
				new Document("$lookup",
						new Document("from", "DMDepot")
								.append("pipeline",
										Arrays.asList(
												new Document("$match",
														new Document("IsDelete", new Document("$ne", true))),
												new Document("$project", new Document("TaxCode", 1))))
								.append("as", "DMDepot")));

		pipeline.add(
				new Document("$lookup",
						new Document("from", "Issuer").append("pipeline", Arrays.asList(
								new Document("$match",
										new Document("$expr", new Document("$and",
												Arrays.asList(new Document("$ne", Arrays.asList("$IsDelete", true)),
														new Document("$ne", Arrays.asList("$IsRoot", true)))))),
								new Document("$project", new Document("TaxCode", 1)))).append("as", "Issuer")));

		cursor = mongoTemplate.getCollection("DMDepot").aggregate(pipeline);
		iter = cursor.iterator();
		if (iter.hasNext()) {
			docTmp = iter.next();
		}
		List<Document> kho = null;
		if (docTmp.get("DMDepot") != null) {
			kho = docTmp.getList("DMDepot", Document.class);

		}

		List<Document> user = null;
		if (docTmp.get("Issuer") != null) {
			user = docTmp.getList("Issuer", Document.class);
		}
		docTmp.clear();
		String slql = "slql";
		String sqtt = "sltt";

		docTmp.append(slql, kho);
		docTmp.append(sqtt, user);
		rsp.setObjData(docTmp);

		responseStatus = new MspResponseStatus(0, "SUCCESS");
		rsp.setResponseStatus(responseStatus);
		return rsp;
	}

	@Override
	@Transactional(rollbackFor = {Exception.class})
	public MsgRsp updatedb(JSONRoot jsonRoot) throws Exception {
		Msg msg = jsonRoot.getMsg();
		MsgHeader header = msg.getMsgHeader();
		MsgRsp rsp = new MsgRsp(header);
		MspResponseStatus responseStatus = null;
		Document docTmp = null;
		Iterable<Document> cursor = null;
		Iterator<Document> iter = null;
		List<Document> pipeline = new ArrayList<Document>();

		Document docMatch = new Document("IsDelete", new Document("$ne", true));
		pipeline = new ArrayList<Document>();
		pipeline.add(new Document("$match", docMatch));

		pipeline.add(
				new Document("$lookup",
						new Document("from", "DMDepot")
								.append("pipeline",
										Arrays.asList(
												new Document("$match",
														new Document("IsDelete", new Document("$ne", true))),
												new Document("$project", new Document("TaxCode", 1))))
								.append("as", "DMDepot")));

		pipeline.add(
				new Document("$lookup",
						new Document("from", "Issuer").append("pipeline", Arrays.asList(
								new Document("$match",
										new Document("$expr", new Document("$and",
												Arrays.asList(new Document("$ne", Arrays.asList("$IsDelete", true)),
														new Document("$ne", Arrays.asList("$IsRoot", true)))))),
								new Document("$project", new Document("TaxCode", 1)))).append("as", "Issuer")));

		pipeline.add(new Document("$lookup",
				new Document("from", "DMMauSoKyHieu").append("pipeline",
						Arrays.asList(new Document("$match", new Document("IsDelete", new Document("$ne", true))),
								new Document("$group", new Document("_id", "$IssuerId").

										append("IssuerId", new Document("$first", "$IssuerId"))

								)

						)).append("as", "DMMauSoKyHieu")));

		pipeline.add(new Document("$lookup",
				new Document("from", "Contract").append("pipeline",
						Arrays.asList(new Document("$match", new Document("IsDelete", new Document("$ne", true))),
								new Document("$group", new Document("_id", "$NMUA.TaxCode").

										append("TaxCode", new Document("$first", "$NMUA.TaxCode"))

								)

						)).append("as", "Contract")));

		cursor = mongoTemplate.getCollection("DMDepot").aggregate(pipeline);
		iter = cursor.iterator();
		if (iter.hasNext()) {
			docTmp = iter.next();
		}

		List<Document> mskh = null;
		if (docTmp.get("DMMauSoKyHieu") != null) {
			mskh = docTmp.getList("DMMauSoKyHieu", Document.class);
		}
		List<Document> contract = null;
		if (docTmp.get("Contract") != null) {
			contract = docTmp.getList("Contract", Document.class);
		}

		/* ====== TAO HOP DONG ========= */
		String taxcode_issu = "";
		String taxcodecontract = "";
		String nameIssuer = "";
		
		/* DANH SACH HOP DONG DANG CO */
		String array_hd = "";
		for (int k = 0; k < contract.size(); k++) {
		Document checkct = null;	
		checkct = contract.get(k);	
		taxcodecontract = (String) checkct.get("TaxCode");
		if(array_hd.equals("")) {
			array_hd = taxcodecontract;
		}else {
			array_hd += ","+ taxcodecontract;
		}
	}
		
		List<Document> kho = null;
		if (docTmp.get("DMDepot") != null) {
			kho = docTmp.getList("DMDepot", Document.class);

		}

		String array_kho = "";
		//LAY MANG TAXCODE TRONG KHO 
		for (int j = 0; j < kho.size(); j++) {
			Document khocheck = null;
			khocheck = kho.get(j);
			String taxcodekho = (String) khocheck.get("TaxCode");
			
			if(array_kho.equals("")) {
				array_kho = taxcodekho;
			}else {
				array_kho += "," + taxcodekho;
			}
		}		
		// END LAY MANG TRONG KHO
		
		
		//START FOR MSKH
		for (int h = 0; h < mskh.size(); h++) {
			Document checkms = null;
			checkms = mskh.get(h);
			String isu = (String) checkms.get("IssuerId");
			ObjectId idissu = null;
			try {
				idissu = new ObjectId(isu);
			} catch (Exception e) {
			}

			Document docTmp1 = null;
			Document docFind = new Document("_id", idissu);
			Iterable<Document> cursor1 = mongoTemplate.getCollection("Issuer").find(docFind);
			Iterator<Document> iter1 = cursor1.iterator();
			if (iter1.hasNext()) {
				docTmp1 = iter1.next();
			}
			
			if(docTmp1 == null) {
				continue;
			}

			taxcode_issu = (String) docTmp1.get("TaxCode");
			nameIssuer = (String) docTmp1.get("Name");
			
			// LAY TAXCODE SO VS CONTRACT			
			boolean check_taxcode = array_hd.contains(taxcode_issu);
			
			//TAO HOP DONG CHUA CO
				if (check_taxcode != true) {				
					pipeline.clear();
					pipeline.add(
							new Document("$lookup",
									new Document("from", "DMMauSoKyHieu")
											.append("pipeline",
													Arrays.asList(
															new Document("$match", new Document("IssuerId", idissu.toString())
																	.append("IsDelete", false))															
															))												
											.append("as", "DMMauSoKyHieu")));

					Document docTmp_ = null;
					Iterable<Document> cursorMSKH = mongoTemplate.getCollection("DMMauSoKyHieu").aggregate(pipeline);
					Iterator<Document> iterMSKH = cursorMSKH.iterator();
					if (iterMSKH.hasNext()) {
						docTmp_ = iterMSKH.next();
					}

					List<Document> rows = null;
					if (docTmp_.get("DMMauSoKyHieu") != null) {
						rows = docTmp_.getList("DMMauSoKyHieu", Document.class);
					}

					// TAO HOP DONG TU DANH SACH ROWS		
					Document docUpsert = null;
					ObjectId id = null;
					id = new ObjectId();

					int SoLuongHDon = 0;
					for (int t = 0; t < rows.size(); t++) {
						Document addcontract = null;					
						addcontract = rows.get(t);
													
						int check_sl =  addcontract.get("SoLuong", 0); 
						SoLuongHDon += check_sl;
					}	
					
						String SoHoaDonHD = String.valueOf(SoLuongHDon);	
						if(SoLuongHDon > 0) {
						/* LUU DU LIEU HD */
						docUpsert = new Document("_id", id).append("Contract", new Document("SHDon", SoHoaDonHD)
								.append("SLHDon", SoLuongHDon)
								.append("GhiChu", "Đồng bộ tự tạo tay"))
								.append("NMUA", new Document("TaxCode", taxcode_issu)
										.append("Name", nameIssuer))
								.append("IsActive", true)
								.append("IsDelete", false)
								.append("InfoCreated",
										new Document("CreateDate", LocalDateTime.now())
												.append("CreateUserID", header.getUserId())
												.append("CreateUserName", header.getUserName())
												.append("CreateUserFullName", header.getUserFullName()));
						/* END - LUU DU LIEU HD */
						mongoTemplate.getCollection("Contract").insertOne(docUpsert);
						

						ObjectId idDMDepot = new ObjectId();
						Document docUpsertUser = null;
						docUpsertUser = new Document("_id", idDMDepot)
								.append("TaxCode", taxcode_issu)
								.append("SLHDon", SoLuongHDon)
								.append("SLHDonDD", SoLuongHDon)
								.append("SLHDonCL", 0)
								.append("GhiChu", "Đồng bộ tự tạo tay")
								.append("IsRoot", false)
								.append("IsDelete", false)
								.append("InfoCreated",
										new Document("CreateDate", LocalDateTime.now())
												.append("CreateUserID", header.getUserId())
												.append("CreateUserName", header.getUserName())
												.append("CreateUserFullName", header.getUserFullName()));
						/* END - LUU DU LIEU */
						mongoTemplate.getCollection("DMDepot").insertOne(docUpsertUser);
						}	
				}else {
					//DONG BO LAI HOP DONG 
					pipeline.clear();
					Document docDepot = new Document("IsDelete", new Document("$ne", true)).append("TaxCode", taxcode_issu);
					pipeline = new ArrayList<Document>();
					pipeline.add(new Document("$match", docDepot));
					pipeline.add(
							new Document("$lookup",
									new Document("from", "Contract")
											.append("pipeline",
													Arrays.asList(
															new Document("$match", new Document("NMUA.TaxCode", taxcode_issu)
																	.append("IsDelete", false))															
															))												
											.append("as", "Contract")));
					
					pipeline.add(
							new Document("$lookup",
									new Document("from", "DMMauSoKyHieu")
											.append("pipeline",
													Arrays.asList(
															new Document("$match", new Document("IssuerId", idissu.toString())
																	.append("IsDelete", false))															
															))												
											.append("as", "DMMauSoKyHieu")));

					Document docTmp_ = null;
					Iterable<Document> cursor_ = mongoTemplate.getCollection("DMDepot").aggregate(pipeline);
					Iterator<Document> iter_ = cursor_.iterator();
					if (iter_.hasNext()) {
						docTmp_ = iter_.next();
					}

					List<Document> rows = null;
					if (docTmp_.get("DMMauSoKyHieu") != null) {
						rows = docTmp_.getList("DMMauSoKyHieu", Document.class);
					}
					
					
					List<Document> rows_contract = null;
					if (docTmp_.get("Contract") != null) {
						rows_contract = docTmp_.getList("Contract", Document.class);
					}

					// TAO HOP DONG TU DANH SACH ROWS		
					Document docUpsert = null;
					ObjectId id = null;
					id = new ObjectId();

					int SoLuongHDon = 0;
					for (int t = 0; t < rows.size(); t++) {
						Document addcontract = null;					
						addcontract = rows.get(t);
													
						int check_sl =  addcontract.get("SoLuong", 0); 
						SoLuongHDon += check_sl;
					}	
					
					int soluonghd_hd = 0;
					for (int t = 0; t < rows_contract.size(); t++) {
						Document sl_contract = null;					
						sl_contract = rows_contract.get(t);
													
						int check_sl =  sl_contract.getEmbedded(Arrays.asList("Contract", "SLHDon"), 0); 
						soluonghd_hd += check_sl;
					}	
					
					
					int SoLuongKho = docTmp_.get("SLHDon", 0);
					int SoLuongKhoDaDung = docTmp_.get("SLHDonDD", 0);														
					int check_lech_hd = SoLuongHDon - SoLuongKho;
					int check_hopdong = SoLuongHDon - soluonghd_hd;
					if(check_lech_hd > 0) {	
						
						int soluongkhoUpdate  = (SoLuongHDon - SoLuongKho) + (SoLuongKho - SoLuongKhoDaDung);
						
						int SoLuongKhoDaDungUpdate = SoLuongKho + soluongkhoUpdate;
						int SoLuongKhoUpdate = SoLuongKhoDaDung + soluongkhoUpdate;
						if(check_hopdong >0) {
						
						String SoHoaDonHD = String.valueOf(soluongkhoUpdate);	
						//TAO HOP DONG - UPDATE KHO
						docUpsert = new Document("_id", id).append("Contract", new Document("SHDon", SoHoaDonHD)
								.append("SLHDon", soluongkhoUpdate)
								.append("GhiChu", "Đồng bộ tự tạo tay"))
								.append("NMUA", new Document("TaxCode", taxcode_issu)
										.append("Name", nameIssuer))
								.append("IsActive", true)
								.append("IsDelete", false)
								.append("InfoCreated",
										new Document("CreateDate", LocalDateTime.now())
												.append("CreateUserID", header.getUserId())
												.append("CreateUserName", header.getUserName())
												.append("CreateUserFullName", header.getUserFullName()));
						/* END - LUU DU LIEU HD */
						mongoTemplate.getCollection("Contract").insertOne(docUpsert);
						}
						//
						FindOneAndUpdateOptions options = null;
						options = new FindOneAndUpdateOptions();
						options.upsert(false);
						options.maxTime(5000, TimeUnit.MILLISECONDS);
						options.returnDocument(ReturnDocument.AFTER);
						
						
						mongoTemplate.getCollection("DMDepot").findOneAndUpdate(
								docDepot
							, new Document("$set", 
								new Document("SLHDon", SoLuongKhoUpdate)
								.append("SLHDonDD", SoLuongKhoDaDungUpdate)
								.append("InfoUpdated", 
										new Document("UpdatedDate", LocalDateTime.now())
											.append("SLHDon", SoLuongKho)
											.append("SLHDonDD", SoLuongKhoDaDung)
											.append("UpdatedUserID", header.getUserId())
											.append("UpdatedUserName", header.getUserName())
											.append("UpdatedUserFullName", header.getUserFullName())
										)
							)
							, options);
			
					}							
				}		

		}
		//END FOR MSKH

		/* =========== TAO KHO CHO ISSUER CHUA TON TAI ============= */
		
		

		responseStatus = new MspResponseStatus(0, "SUCCESS");
		rsp.setResponseStatus(responseStatus);
		return rsp;
	}

	///

	@Override
	public MsgRsp listcks(JSONRoot jsonRoot) throws Exception {
		Msg msg = jsonRoot.getMsg();
		MsgHeader header = msg.getMsgHeader();
		MsgPage page = msg.getMsgPage();

		MsgRsp rsp = new MsgRsp(header);
		MspResponseStatus responseStatus = null;

		ObjectId objectId = null;
		Document docTmp = null;
		Iterable<Document> cursor = null;
		Iterator<Document> iter = null;
		List<Document> pipeline = new ArrayList<Document>();

		Document docMatch = new Document("IsDelete", new Document("$ne", true));
		pipeline = new ArrayList<Document>();
		pipeline.add(new Document("$match", docMatch));
		pipeline.add(new Document("$sort", new Document("_id", -1)));
		pipeline.addAll(createFacetForSearchNotSort(page));
		cursor = mongoTemplate.getCollection("DMCTSo").aggregate(pipeline).allowDiskUse(true);
		iter = cursor.iterator();
		if (iter.hasNext()) {
			docTmp = iter.next();
		}

		rsp = new MsgRsp(header);
		responseStatus = null;
		if (null == docTmp) {
			responseStatus = new MspResponseStatus(9999, Constants.MAP_ERROR.get(9999));
			rsp.setResponseStatus(responseStatus);
			return rsp;
		}

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
				hItem.put("TenNnt", doc.get("TenNnt"));
				hItem.put("MST", doc.get("MST"));
				hItem.put("DSCTSSDung", doc.get("DSCTSSDung"));

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
	public MsgRsp listnguoimua(JSONRoot jsonRoot) throws Exception {
		Msg msg = jsonRoot.getMsg();
		MsgHeader header = msg.getMsgHeader();
		MsgPage page = msg.getMsgPage();

		MsgRsp rsp = new MsgRsp(header);
		MspResponseStatus responseStatus = null;

		ObjectId objectId = null;
		Document docTmp = null;
		Iterable<Document> cursor = null;
		Iterator<Document> iter = null;
		List<Document> pipeline = new ArrayList<Document>();

		Document docMatch = new Document("IsDelete", new Document("$ne", true));
		pipeline = new ArrayList<Document>();
		pipeline.add(new Document("$match", docMatch));
		pipeline.add(new Document("$sort", new Document("_id", -1)));
		pipeline.addAll(createFacetForSearchNotSort(page));
		cursor = mongoTemplate.getCollection("QLDSNMua").aggregate(pipeline).allowDiskUse(true);
		iter = cursor.iterator();
		if (iter.hasNext()) {
			docTmp = iter.next();
		}

		rsp = new MsgRsp(header);
		responseStatus = null;
		if (null == docTmp) {
			responseStatus = new MspResponseStatus(9999, Constants.MAP_ERROR.get(9999));
			rsp.setResponseStatus(responseStatus);
			return rsp;
		}

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
				hItem.put("Ten", doc.get("Ten"));
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

}
