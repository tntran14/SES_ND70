package vn.sesgroup.hddt.user.impl;

import java.io.File;
import java.time.LocalDate;
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
import vn.sesgroup.hddt.dto.MailConfig;
import vn.sesgroup.hddt.dto.SignTypeInfo;
import vn.sesgroup.hddt.user.dao.AbstractDAO;
import vn.sesgroup.hddt.user.dao.ForgetPassDao;
import vn.sesgroup.hddt.user.service.JPUtils;
import vn.sesgroup.hddt.utility.Constants;
import vn.sesgroup.hddt.utility.Json;
import vn.sesgroup.hddt.utility.MailUtils;

@Repository
@Transactional
public class ForgetPassImpl extends AbstractDAO implements ForgetPassDao{
	private static final Logger log = LogManager.getLogger(ForgetPassImpl.class);
	
	@Autowired MongoTemplate mongoTemplate;
	@Autowired ConfigConnectMongo cfg;
	
	private MailUtils mailUtils = new MailUtils();
	@Autowired JPUtils jpUtils;
	@Override
	public MsgRsp crud(JSONRoot jsonRoot) throws Exception {
		Msg msg = jsonRoot.getMsg();
		MsgHeader header = msg.getMsgHeader();
		MsgPage page = msg.getMsgPage();
		Object objData = msg.getObjData();
		Iterable<Document> cursor = null;
		Iterator<Document> iter = null;
		Iterable<Document> cursor1 = null;
		Iterator<Document> iter1 = null;
		List<Document> pipeline = null;
		Document docR = null;
		Document docFind = null;
		Document docTmp = null;
		Document docTmp1 = null;
		JsonNode jsonData = null;
		MsgRsp rsp = new MsgRsp(header);
		rsp.setMsgPage(page);
		MspResponseStatus responseStatus = null;
		
		
		
		if (objData != null) {
			jsonData = Json.serializer().nodeFromObject(msg.getObjData());
		} else {
			throw new Exception("Lỗi dữ liệu đầu vào");
		}

		String ten = commons.getTextJsonNode(jsonData.at("/Username"));
		String mail = commons.getTextJsonNode(jsonData.at("/Email"));
		if ("" == ten) {
			responseStatus = new MspResponseStatus(9999, "Tài khoản không được để trống.");
			rsp.setResponseStatus(responseStatus);
			return rsp;
		}
		if(!"".equals(mail) && !commons.isValidEmailAddress(mail)) {
			responseStatus = new MspResponseStatus(9999, "Vui lòng nhập mail đúng định dạng.");
			rsp.setResponseStatus(responseStatus);
			return rsp;
		}
		docFind = new Document("TaxCode",ten).append("ContactUser.EmailUserLh",mail); 
		 docTmp = null;
		cursor = mongoTemplate.getCollection("Issuer").find(docFind);
		 iter = cursor.iterator();
		if (iter.hasNext()) {
			docTmp = iter.next();
		}
		if (null == docTmp) {
			responseStatus = new MspResponseStatus(9999, "Thông tin nhập vào chưa đúng. Vui lòng nhập lại thông tin chính xác!");
			rsp.setResponseStatus(responseStatus);
			return rsp;
		}
		
		ObjectId idissu  = 	(ObjectId) docTmp.get("_id");
		String token = commons.csRandomAlphaNumbericString(50);
		String otp = commons.csRandomNumbericString(4);
		
		
		/*INSERT DATA IN SYSTEM-GETPASSWORD*/
		ObjectId objectId = null;
		objectId = new ObjectId();
		Document docUpsert = new Document("_id", objectId)
				.append("UserName", ten)
				.append("Email", mail)
				.append("Type", "USER")
				.append("IsActive", false)
				.append("Token", token)
				.append("OTP", otp)								
				.append("IssuerId", idissu.toString());				
		
		mongoTemplate.getCollection("system_getpassword").insertOne(docUpsert);
		/* END - INSERT DATA IN SYSTEM-GETPASSWORD */
		
		
		/*CAU HINH MAIL GUI*/
		ObjectId idissu1  = (ObjectId) docTmp.get("_id");		
		docFind = null;
		docFind = new Document("IssuerId",idissu1.toString()).append("UserName",ten); 
		 docTmp = null;
		 pipeline = new ArrayList<Document>();
			pipeline.add(new Document("$match", docFind));
			pipeline.add(
					new Document("$lookup", 
						new Document("from", "PramLink")
						.append("pipeline", 
							Arrays.asList(
								new Document("$match", 
									new Document("$expr", 
											new Document("IsDelete", false)
									)
								)
							)	
						)
						.append("as", "PramLink")
					)
				);
				pipeline.add(
					new Document("$unwind", new Document("path", "$PramLink").append("preserveNullAndEmptyArrays", true))
				);	
		 
		cursor = mongoTemplate.getCollection("Users").aggregate(pipeline);
		
		iter = cursor.iterator();
		
		if(iter.hasNext()) {
			docTmp = iter.next();
		}		
		
		if (null == docTmp) {
			responseStatus = new MspResponseStatus(9999, "Không tìm thấy thông tin khách hàng");
			rsp.setResponseStatus(responseStatus);
			return rsp;
		}
		
		
		cursor1 = mongoTemplate.getCollection("ConfigEmailAdmin").find().limit(1);
		iter1 = cursor1.iterator();
		if(iter1.hasNext()) {
			docTmp1 = iter1.next();
		}
		
//		String 	secureKey = commons.csRandomString(15);
//		String pass = commons.encryptThisString(ten + secureKey);	
//		
//		FindOneAndUpdateOptions options = null;
//		options = new FindOneAndUpdateOptions();
//		options.upsert(false);
//		options.maxTime(5000, TimeUnit.MILLISECONDS);
//		options.returnDocument(ReturnDocument.AFTER);
//	
//		docR = mongoTemplate.getCollection("Users").findOneAndUpdate(
//				docFind,
//				new Document("$set", 
//						new Document("Password", pass)
//				),
//			options
//		);
//		
		String Fullname = docTmp.getString("FullName");
		
		MailConfig mailConfig = new MailConfig(docTmp1);
 		mailConfig.setNameSend(docTmp.getEmbedded(Arrays.asList("UserName"), ""));

		/*THUC HIEN GUI MAIL*/
		String _title =  "Mã xác thực xác nhận quên mật khẩu " + ten;
		String _tmp = "";
		String tk = ten;
		String _content;
		
		String link = docTmp.getEmbedded(Arrays.asList("PramLink", "LinkLogin"), "");
		StringBuilder message = new StringBuilder();
		message.setLength(0);
		message.append("<p><span style='font-family: times new roman;font-size: 13px;'>Kính gửi: <label style='font-weight: bold;'>" + ("".equals(Fullname)? ten: Fullname) + "</label><o:p></o:p></span></p>\n");
		message.append("<p><span style='font-family: times new roman;font-size: 13px;'>Bạn vừa yêu cầu mã xác thực lấy lại mật khẩu.</span></p>\n");
		message.append("<p><span style='font-family: times new roman;font-size: 13px;'>Vui lòng nhập mã xác thực <strong>" + otp + "</strong> để lấy lại mật khẩu mới.</span></p>\n");
		message.append("<p><span style='font-family: times new roman;font-size: 13px;'>Trân trọng!</span></p><br />\n");
		message.append("<p style='margin-bottom: 3px;'><span style='font-family: Times New Roman;font-size: 13px;color:red;font-weight: bold;'>QUÝ CÔNG TY VUI LÒNG KHÔNG REPLY EMAIL NÀY!</span></p>");
		_content = message.toString();
		boolean boo = mailUtils.sendMail(mailConfig, _title, _content, mail, null, null, true);		
		responseStatus = new MspResponseStatus(0, token);
		rsp.setResponseStatus(responseStatus);
		return rsp;
	}
	@Override
	public MsgRsp check_token(JSONRoot jsonRoot) throws Exception {
		MsgRsp res = new MsgRsp();
		
		Msg msg = jsonRoot.getMsg();
		MsgHeader header = msg.getMsgHeader();
		MsgPage page = msg.getMsgPage();
		Object objData = msg.getObjData();
		Iterable<Document> cursor = null;
		Iterator<Document> iter = null;
		List<Document> pipeline = null;
		Document docR = null;
		Document docFind = null;
		Document docTmp = null;
		JsonNode jsonData = null;
		Iterable<Document> cursor1 = null;
		Iterator<Document> iter1 = null;
		Document docTmp1 = null;
		MsgRsp rsp = new MsgRsp(header);
		rsp.setMsgPage(page);
		MspResponseStatus responseStatus = null;

		if (objData != null) {
			jsonData = Json.serializer().nodeFromObject(msg.getObjData());
		} else {
			throw new Exception("Lỗi dữ liệu đầu vào");
		}
		String token = commons.getTextJsonNode(jsonData.at("/token"));
		String otp = commons.getTextJsonNode(jsonData.at("/otp"));					
		String sysOtp = "";
		String sysUserName = "";
		String sysFullName = "";
		String sysEmail = "";
		String IssuID = "";
		int sysErrorsCount = -1;
		boolean sysIsActive = false;
		long sysSecondCount = 0L;
		long agentId = 0L;
		
		docFind = new Document("Token",token).append("OTP", otp); 
		 docTmp = null;
		cursor = mongoTemplate.getCollection("system_getpassword").find(docFind);
		 iter = cursor.iterator();
		if (iter.hasNext()) {
			docTmp = iter.next();
		}
		if (null == docTmp) {
			responseStatus = new MspResponseStatus(9999, "Thông tin nhập vào chưa đúng. Vui lòng nhập lại OTP đúng!");
			rsp.setResponseStatus(responseStatus);
			return rsp;
		}
//		TIMESTAMPDIFF(SECOND, s.SystemDate, NOW())
		sysOtp = docTmp.getString("otp");
		sysUserName = docTmp.getString("UserName");
		sysEmail = docTmp.getString("Email");
		sysIsActive = docTmp.getBoolean("IsActive");
		IssuID = docTmp.getString("IssuerId");
//		sysSecondCount = docTmp.getLong("SecondCount");
		
		
		
		
		/*THUC HIEN RESET MAT KHAU*/
		String password = "";
		String 	secureKey = commons.csRandomString(15);
		String pass = commons.encryptThisString(sysUserName + secureKey);	
		int size = pass.length();
		if(size < 128) {
		password = "0"+ pass;
		}else {
		password = pass;
		}
		/*CAP NHAT THONG TIN MAT KHAU*/
		FindOneAndUpdateOptions options = null;
		options = new FindOneAndUpdateOptions();
		options.upsert(false);
		options.maxTime(5000, TimeUnit.MILLISECONDS);
		options.returnDocument(ReturnDocument.AFTER);
		
		Document docFind1 = new Document("IssuerId",IssuID).append("UserName",sysUserName); 
		docR = mongoTemplate.getCollection("Users").findOneAndUpdate(
				docFind1,
				new Document("$set", 
						new Document("Password", password)
				),
			options
		);
		
		/*CAP NHAT TRANG THAI TOKEN DA THUC HIEN*/
		FindOneAndUpdateOptions options1 = null;
		options1 = new FindOneAndUpdateOptions();
		options1.upsert(false);
		options1.maxTime(5000, TimeUnit.MILLISECONDS);
		options1.returnDocument(ReturnDocument.AFTER);
		
		Document docFind2 = new Document("Email",sysEmail).append("UserName",sysUserName); 
		docR = mongoTemplate.getCollection("system_getpassword").findOneAndUpdate(
				docFind2,
				new Document("$set", 
						new Document("IsActive", true)
				),
			options1
		);
		/*CAU HINH MAIL GUI*/
		ObjectId idissu1  = (ObjectId) docTmp.get("_id");		
		docFind = null;	
		 docTmp = null;
		 pipeline = new ArrayList<Document>();
			pipeline.add(new Document("$match", docFind1));
			pipeline.add(
					new Document("$lookup", 
						new Document("from", "PramLink")
						.append("pipeline", 
							Arrays.asList(
								new Document("$match", 
									new Document("$expr", 
											new Document("IsDelete", false)
									)
								)
							)	
						)
						.append("as", "PramLink")
					)
				);
				pipeline.add(
					new Document("$unwind", new Document("path", "$PramLink").append("preserveNullAndEmptyArrays", true))
				);	
		 
		 
		
		cursor = mongoTemplate.getCollection("Users").aggregate(pipeline);
		iter = cursor.iterator();
		if(iter.hasNext()) {
			docTmp = iter.next();
		}		
		
		if (null == docTmp) {
			responseStatus = new MspResponseStatus(9999, "Không tìm thấy thông tin khách hàng");
			rsp.setResponseStatus(responseStatus);
			return rsp;
		}
		

		cursor1 = mongoTemplate.getCollection("ConfigEmailAdmin").find().limit(1);
		iter1 = cursor1.iterator();
		if(iter1.hasNext()) {
			docTmp1 = iter1.next();
		}
		MailConfig mailConfig = new MailConfig(docTmp1);
		mailConfig.setNameSend(docTmp.getEmbedded(Arrays.asList("UserName"), ""));
		String link = docTmp.getEmbedded(Arrays.asList("PramLink", "LinkLogin"), "");
		/*THUC HIEN GUI MAIL*/
		String _title =  " Thông báo cấp lại mật khẩu";
		String _tmp = "";
		String tk = sysUserName;
		String _content;
	
		StringBuilder sb = new StringBuilder();
		sb.setLength(0);
		sb.append("<p><span style='font-family: Times New Roman;font-size: 13px;'>Kính gửi: <label style='font-weight: bold;'>" + ("".equals(_tmp)? "Quý khách hàng": _tmp) + "</label><o:p></o:p></span></p>\n");
		sb.append("<p><span style='font-family: Times New Roman;font-size: 13px;'><label style='font-weight: bold;'>" + _title + "</label> Xin chân thành cảm ơn Quý đơn vị đã tin tưởng và sử dụng sản phẩm/dịch vụ của chúng tôi!</span></p>\n");
		sb.append("<p><span style='font-family: Times New Roman;font-size: 13px;'>Chúng tôi vừa cập nhật lại mật khẩu của Quý đơn vị với thông tin như sau:</span></p>\n");
		sb.append("<p><span style='font-family: Times New Roman;font-size: 13px;'>1.  Tài khoản:  " + tk + "</span></p>\n");
		sb.append("<p><span style='font-family: Times New Roman;font-size: 13px;'>2.  Mật khẩu:  " + secureKey + "</span></p>\n");
		sb.append("<p><span style='font-family: Times New Roman;font-size: 13px;'>3.  Link đăng nhập:  " + link + "</span></p>\n");
		sb.append("<p><span style='font-family: Times New Roman;font-size: 13px;'>Quý đơn vị  vui lòng bảo mật thông tin và thay đổi mật khẩu sau khi đăng nhập.</span></p>\n");
		sb.append("<p><span style='font-family: Times New Roman;font-size: 13px;'>Trân trọng kính chào!</span></p>");
		sb.append("<hr style='margin: 5px 0 5px 0;'>");
		sb.append("<p style='margin-bottom: 3px;'><span style='font-family: Times New Roman;font-size: 13px;color:red;font-weight: bold;'>QUÝ CÔNG TY VUI LÒNG KHÔNG REPLY EMAIL NÀY!</span></p>");
		 _content = sb.toString();
		boolean boo = mailUtils.sendMail(mailConfig, _title, _content, sysEmail, null, null, true);

		responseStatus = new MspResponseStatus(0, "SUCCESS");
		rsp.setResponseStatus(responseStatus);
		return rsp;
	}
	@Override
	public MsgRsp using_token(JSONRoot jsonRoot) throws Exception {
		MsgRsp res = new MsgRsp();
		
		Msg msg = jsonRoot.getMsg();
		MsgHeader header = msg.getMsgHeader();
		MsgPage page = msg.getMsgPage();
		Object objData = msg.getObjData();
		Iterable<Document> cursor = null;
		Iterator<Document> iter = null;
		List<Document> pipeline = null;
		Document docR = null;
		Document docFind = null;
		Document docFind1 = null;
		Document docTmp = null;
		JsonNode jsonData = null;
		Iterable<Document> cursor1 = null;
		Iterator<Document> iter1 = null;
		Document docTmp1 = null;
		MsgRsp rsp = new MsgRsp(header);
		rsp.setMsgPage(page);
		MspResponseStatus responseStatus = null;
		try {
		if (objData != null) {
			jsonData = Json.serializer().nodeFromObject(msg.getObjData());
			String tk_username = commons.getTextJsonNode(jsonData.at("/tk_username"));
//			String cert = commons.getTextJsonNode(jsonData.at("/cert"));
			String certificate = commons.getTextJsonNode(jsonData.at("/certificate"));
			if ("" == certificate) {
				responseStatus = new MspResponseStatus(9999, "Chứng thư số không được rỗng.");
				rsp.setResponseStatus(responseStatus);
				return rsp;
			}
//			docFind = new Document("DSCTSSDung.Seri", new Document("$in", Arrays.asList(cert)));
//			 pipeline = new ArrayList<Document>();
//			 pipeline.add(new Document("$match", new Document("UserName", tk_username)));		 			 
//				pipeline.add(new Document("$lookup",
//						new Document("from", "DMCTSo").append("pipeline", Arrays.asList(new Document("$match", docFind)
//						)).append("as", "DMCTSo")));
//				pipeline.add(new Document("$unwind",
//						new Document("path", "$DMCTSo").append("preserveNullAndEmptyArrays", true)));
//				
//				
//				cursor = mongoTemplate.getCollection("Users").aggregate(pipeline);
//			
//			//cursor = mongoTemplate.getCollection("DMCTSo").find(docFind).limit(1);
//			 iter = cursor.iterator();
//			if (iter.hasNext()) {
//				docTmp = iter.next();
//			}
//			if (null == docTmp) {
//				responseStatus = new MspResponseStatus(9999, "Thông tin chứng thư số không hợp lệ!");
//				rsp.setResponseStatus(responseStatus);
//				return rsp;
//			}		
//			Document docTTHDLQuan = null;
//			docTTHDLQuan = docTmp.get("DMCTSo", Document.class);
//			ObjectMapper mapper = new ObjectMapper();
//			ObjectReader reader = mapper.reader(JsonNode.class);
//			JsonNode node = reader.readValue(docTTHDLQuan.toJson());
//			String HThuc = "";
//			String TNgay1 = "";
//			String DNgay1 = "";
//			if (!node.at("/DSCTSSDung").isMissingNode()) {
//				for (JsonNode o : node.at("/DSCTSSDung")) {
//					HThuc = commons.getTextJsonNode(o.at("/HThuc")).replaceAll(",", "");
//					for (JsonNode n : o.at("/TNgay")) {
//						TNgay1 = n.toString();
//					}
//					for (JsonNode k : o.at("/DNgay")) {
//						DNgay1 = k.toString();
//					}
//				}}
			
//			LocalDateTime TNgay = commons.convertStringToLocalDateTime("2022-11-04",Constants.FORMAT_DATE.FORMAT_DATE_WEB);
//			String DNgay = commons.convertLocalDateTimeToString(commons.convertDateToLocalDateTime(docTmp.getEmbedded(Arrays.asList("DMCTSo","DSCTSSDung", "DNgay"), Date.class)),"yyyy-MM-dd");
//			LocalDate now = LocalDate.now();
//			LocalDate from = commons.convertStringToLocalDate("2022/11/04", Constants.FORMAT_DATE.FORMAT_DATE_DB);
//			LocalDate to = commons.convertStringToLocalDate(DNgay1, Constants.FORMAT_DATE.FORMAT_DATE_DB);
//			
			

			SignTypeInfo signTypeInfo = commons.parserCert(certificate);
			if(null == signTypeInfo) {
				responseStatus = new MspResponseStatus(9999, "Thông tin chứng thư số không hợp lệ.");
				rsp.setResponseStatus(responseStatus);
				return rsp;
			}
			
			LocalDate now = LocalDate.now();
			LocalDate from = commons.formatStringToLocalDate(signTypeInfo.getIssuedDate(), Constants.FORMAT_DATE.FORMAT_DATE_DB);
			LocalDate to = commons.formatStringToLocalDate(signTypeInfo.getExpireDate(), Constants.FORMAT_DATE.FORMAT_DATE_DB);
			if(commons.compareLocalDate(now, from) < 0) {
				responseStatus = new MspResponseStatus(9999, "Chứng thư số không hợp lệ.");
				rsp.setResponseStatus(responseStatus);
				return rsp;
			}
			if(commons.compareLocalDate(to, now) < 0) {
				responseStatus = new MspResponseStatus(9999, "Chứng thư số đã hết hạn.");
				rsp.setResponseStatus(responseStatus);
				return rsp;
			}

			
			docFind1 = new Document("MST", tk_username);
			cursor1 = mongoTemplate.getCollection("DMCTSo").find(docFind1);
			 iter1 = cursor1.iterator();
			if (iter1.hasNext()) {
				docTmp1 = iter1.next();
			}
			if (null == docTmp1) {
				responseStatus = new MspResponseStatus(9999, "Không tìm thấy người dùng trong hệ thống!");
				rsp.setResponseStatus(responseStatus);
				return rsp;
			}
			
			String MST = docTmp1.getString("MST");
			if(!MST.equals(signTypeInfo.getTaxCode())) {
				responseStatus = new MspResponseStatus(9999, "Mã số thuế khách hàng không hợp lệ!");
				rsp.setResponseStatus(responseStatus);
				return rsp;	
			}
			
			/*THUC HIEN RESET MAT KHAU*/
			String password = ""; 
			String 	secureKey = commons.csRandomString(15);
			String pass = commons.encryptThisString(tk_username + secureKey);	
			int size = pass.length();
			if(size < 128) {
			password = "0"+ pass;
			}else {
			password = pass;
			}
			
			
			
			/*CAP NHAT THONG TIN MAT KHAU*/
			FindOneAndUpdateOptions options = null;
			options = new FindOneAndUpdateOptions();
			options.upsert(false);
			options.maxTime(5000, TimeUnit.MILLISECONDS);
			options.returnDocument(ReturnDocument.AFTER);
			
			Document docFind2 = new Document("UserName",tk_username); 
			docR = mongoTemplate.getCollection("Users").findOneAndUpdate(
					docFind2,
					new Document("$set", 
							new Document("Password", password)
					),
				options
			);
			responseStatus = new MspResponseStatus(0, secureKey);
			rsp.setResponseStatus(responseStatus);
	
			return rsp;
		}
		else {
			throw new Exception("Lỗi dữ liệu đầu vào");
		}
		}catch(Exception e) {
			responseStatus = new MspResponseStatus(9999, "Có lỗi trong quá trình xử lý!");
			rsp.setResponseStatus(responseStatus);
			return rsp;
		}
	}
	@Override
	public MsgRsp list(JSONRoot jsonRoot) throws Exception {
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
		

		
		
		Document docMatch = new Document("IsDelete",new Document("$ne", true));
		pipeline = new ArrayList<Document>();
		pipeline.add(new Document("$match", docMatch));
		pipeline.add(
				new Document("$sort", 
					new Document("_id", -1)
				)
			);
		
		
		
		MongoClient mongoClient = cfg.mongoClient();
		MongoCollection<Document> collection = mongoClient.getDatabase(cfg.dbName).getCollection("DMTTWeb");
		try {
			docTmp =   collection.aggregate(pipeline).allowDiskUse(true).iterator().next();	
		} catch (Exception e) {
			// TODO: handle exception
		}
		mongoClient.close();
		
		
		rsp = new MsgRsp(header);
		responseStatus = null;
		if(null == docTmp) {
			responseStatus = new MspResponseStatus(9999, Constants.MAP_ERROR.get(9999));
			rsp.setResponseStatus(responseStatus);
			return rsp;
		}
		String img =  docTmp.get("logo", "");
		String dir = "C:/hddt-ses/server/template/images/";
		File f = new File(dir,img);
		String base64Template = commons.encodeImageToBase64(f);
		ArrayList<HashMap<String, Object>> rowsReturn = new ArrayList<HashMap<String, Object>>();
		HashMap<String, Object> hItem = null;
		if(null != docTmp) {
				hItem = new HashMap<String, Object>();
				hItem.put("LOGO", base64Template);
				hItem.put("PHONE", docTmp.get("phone"));
				hItem.put("EMAIL", docTmp.get("mail"));
				rowsReturn.add(hItem);
		}
		
		HashMap<String, Object> mapDataR = new HashMap<String, Object>();
		mapDataR.put("rows", rowsReturn);
		responseStatus = new MspResponseStatus(0, "SUCCESS");
		rsp.setResponseStatus(responseStatus);
		rsp.setObjData(mapDataR);
		return rsp;
	}
	@Override
	public MsgRsp left(JSONRoot jsonRoot) throws Exception {
		Msg msg = jsonRoot.getMsg();
		MsgHeader header = msg.getMsgHeader();
		MsgPage page = msg.getMsgPage();
		page.setSize(3);
		MsgRsp rsp = new MsgRsp(header);
		MspResponseStatus responseStatus = null;
		
		ObjectId objectId = null;
		Document docTmp = null;
		Iterable<Document> cursor = null;
		Iterator<Document> iter = null;
		List<Document> pipeline = new ArrayList<Document>();
		

		Document docMatch =new Document("IsDelete",false).append("HT", "L").append("IsActive", true);
		pipeline = new ArrayList<Document>();
		pipeline.add(new Document("$match", docMatch));
		pipeline.add(new Document("$sort",
				new Document("_id", -1)));
		pipeline.addAll(createFacetForSearchNotSort(page));

		
		
		MongoClient mongoClient = cfg.mongoClient();
		    MongoCollection<Document> collection = mongoClient.getDatabase(cfg.dbName).getCollection("DMInforWeb");
		    try {
		        docTmp = collection.aggregate(pipeline).allowDiskUse(true).iterator().next();
			} catch (Exception e) {
				// TODO: handle exception
			}
		mongoClient.close();
	
		rsp = new MsgRsp(header);
		responseStatus = null;
		if(null == docTmp) {
			responseStatus = new MspResponseStatus(9999, Constants.MAP_ERROR.get(9999));
			rsp.setResponseStatus(responseStatus);
			return rsp;
		}
		List<Document> rows = null;
		if (docTmp.get("data") != null && docTmp.get("data") instanceof List) {
			rows = docTmp.getList("data", Document.class);
			docTmp = null;
		}
		ArrayList<HashMap<String, Object>> rowsReturn = new ArrayList<HashMap<String, Object>>();
		HashMap<String, Object> hItem = null;
		if (null != rows) {
			for (Document doc : rows) {
				objectId = (ObjectId) doc.get("_id");

				hItem = new HashMap<String, Object>();
				hItem.put("_id", objectId.toString());
				hItem.put("Chude", doc.get("Chude"));
				hItem.put("Tieude", doc.get("Tieude"));
				hItem.put("Noidung", doc.get("Noidung"));
				hItem.put("Link", doc.get("Link"));
				rowsReturn.add(hItem);
			}
		}
		rows = null;
		HashMap<String, Object> mapDataR = new HashMap<String, Object>();
		mapDataR.put("rows", rowsReturn);
		rowsReturn = null;
		responseStatus = new MspResponseStatus(0, "SUCCESS");
		rsp.setResponseStatus(responseStatus);
		rsp.setObjData(mapDataR);
		mapDataR = null;
		return rsp;
	}
	@Override
	public MsgRsp right(JSONRoot jsonRoot) throws Exception {
		Msg msg = jsonRoot.getMsg();
		MsgHeader header = msg.getMsgHeader();
		MsgPage page = msg.getMsgPage();
		page.setSize(3);
		MsgRsp rsp = new MsgRsp(header);
		MspResponseStatus responseStatus = null;
		
		ObjectId objectId = null;
		Document docTmp = null;
		List<Document> pipeline = new ArrayList<Document>();
		

		Document docMatch =new Document("IsDelete",false).append("HT", "R").append("IsActive", true);
		pipeline = new ArrayList<Document>();
		pipeline.add(new Document("$match", docMatch));
		pipeline.add(new Document("$sort",
				new Document("_id", -1)));
		pipeline.addAll(createFacetForSearchNotSort(page));

		
		MongoClient mongoClient = cfg.mongoClient();
		MongoCollection<Document> collection = mongoClient.getDatabase(cfg.dbName).getCollection("DMInforWeb");
		try {
			docTmp =   collection.aggregate(pipeline).allowDiskUse(true).iterator().next();		
		} catch (Exception e) {
			// TODO: handle exception
		}
	
		mongoClient.close();
		rsp = new MsgRsp(header);
		responseStatus = null;
		if(null == docTmp) {
			responseStatus = new MspResponseStatus(9999, Constants.MAP_ERROR.get(9999));
			rsp.setResponseStatus(responseStatus);
			return rsp;
		}
		List<Document> rows = null;
		if (docTmp.get("data") != null && docTmp.get("data") instanceof List) {
			rows = docTmp.getList("data", Document.class);
			docTmp = null;
		}
		ArrayList<HashMap<String, Object>> rowsReturn = new ArrayList<HashMap<String, Object>>();
		HashMap<String, Object> hItem = null;
		if (null != rows) {
			for (Document doc : rows) {
				objectId = (ObjectId) doc.get("_id");

				hItem = new HashMap<String, Object>();
				hItem.put("_id", objectId.toString());
				hItem.put("Chude", doc.get("Chude"));
				hItem.put("Noidung", doc.get("Noidung"));
				hItem.put("Tieude", doc.get("Tieude"));
				rowsReturn.add(hItem);
			}
		}
		rows = null;
		HashMap<String, Object> mapDataR = new HashMap<String, Object>();
		mapDataR.put("rows", rowsReturn);
		rowsReturn = null;
		responseStatus = new MspResponseStatus(0, "SUCCESS");
		rsp.setResponseStatus(responseStatus);
		rsp.setObjData(mapDataR);
		mapDataR = null;
		return rsp;
	}

}
