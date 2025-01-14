package vn.sesgroup.hddt.user.impl;

import java.security.Key;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.crypto.spec.SecretKeySpec;
import javax.xml.bind.DatatypeConverter;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;

import io.jsonwebtoken.Header;
import io.jsonwebtoken.JwsHeader;
import io.jsonwebtoken.JwtBuilder;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import vn.sesgroup.hddt.configuration.ConfigConnectMongo;
import vn.sesgroup.hddt.user.dao.AbstractDAO;
import vn.sesgroup.hddt.user.dao.LoginDAO;
import vn.sesgroup.hddt.user.dto.IssuerInfo;
import vn.sesgroup.hddt.user.dto.LoginRes;
import vn.sesgroup.hddt.user.dto.UserLoginReq;
import vn.sesgroup.hddt.utility.Constants;

@Repository
@Transactional
public class LoginImpl extends AbstractDAO implements LoginDAO{
	private static final Logger log = LogManager.getLogger(LoginImpl.class);
	
	
	
	@Autowired ConfigConnectMongo cfg;
	
	@Transactional(rollbackFor = Exception.class)
	@Override
	public LoginRes doAuth(UserLoginReq req) throws Exception {
		LoginRes res = new LoginRes();
		
		Document docFind = new Document("UserName", commons.regexEscapeForMongoQuery(req.getUserName()))
				.append("IsDelete", new Document("$ne", true))
				.append("IsActive", true);
		List<Document> pipeline = new ArrayList<Document>();
		pipeline.add(new Document("$match", docFind));
		pipeline.add(
			new Document("$lookup", 
				new Document("from", "Issuer")
				.append("let", new Document("vIssuerId", "$IssuerId"))
				.append("pipeline", 
					Arrays.asList(
						new Document("$match", 
							new Document("$expr", 
								new Document("$and", 
									Arrays.asList(
										new Document("$ne", Arrays.asList("$IsDelete", true))
										,new Document("$ne", Arrays.asList("$IsActive", false))
										, new Document("$eq", Arrays.asList("$$vIssuerId", new Document("$toString", "$_id")))
									)
								)
							)
						)
					)
				)
				.append("as", "IssuerInfo")
			)
		);
		pipeline.add(new Document("$unwind", new Document("path", "$IssuerInfo").append("preserveNullAndEmptyArrays", true)));
		
		
		Document docTmp = null;
		Document docSub = null;
		
		MongoClient mongoClient = cfg.mongoClient();
		MongoCollection<Document> collection = mongoClient.getDatabase(cfg.dbName).getCollection("Users");
		try {
			docTmp =   collection.aggregate(pipeline).allowDiskUse(true).iterator().next();	
		} catch (Exception e) {
			// TODO: handle exception
		}
		mongoClient.close();
		
	
		
		if(docTmp == null) {
			res.setStatusCode(1);
			res.setStatusText("Người dùng không tồn tại trong hệ thống hoặc chưa được kích hoạt!!!");
			return res;
		}
		ObjectId objectId = null;
		IssuerInfo issuer = new IssuerInfo();
		
		if(null == docTmp.get("IssuerInfo")) {
			res.setStatusCode(1);
			res.setStatusText("Không tìm thấy thông tin khách hàng.");
			return res;
		}
		
		/*CHECK PASSWORD*/
		String passwordInput = commons.generateSHA(req.getUserName() + req.getPassword(), false);
		
		String pass_old = docTmp.get("Password", "");
		
		String check_pass = "";
		if(passwordInput.length()< 128) {
			check_pass = "0" + passwordInput;
		}else {
			check_pass = passwordInput;
		}
		boolean checkPassword = passwordInput.equals(pass_old);
		if(!checkPassword) {
			res = new LoginRes();
			res.setStatusCode(999);
			res.setStatusText("Mật khẩu đăng nhập không đúng.");
			return res;
		}
		/*END - CHECK PASSWORD*/
		
		if(docTmp.get("IssuerInfo") != null) {
			docSub = docTmp.get("IssuerInfo", Document.class);
			issuer = new IssuerInfo();
			issuer.set_id(docSub.getObjectId("_id").toString());
			issuer.setName(docSub.get("Name", ""));
			issuer.setAddress(docSub.get("Address", ""));
			issuer.setPhone(docSub.get("Phone", ""));
			issuer.setFax(docSub.get("Fax", ""));
			issuer.setEmail(docSub.get("Email", ""));
			issuer.setWebsite(docSub.get("Website", ""));
			issuer.setTaxCode(docSub.get("TaxCode", ""));;
			if(docSub.get("BankAccount") != null)
				issuer.setBankAccount(docSub.get("BankAccount", Object.class));
			res.setIssuerInfo(issuer);
		}
		
		
		//CHECK USER CHECK 
		String check_UserCheck = docTmp.get("UserName", "");
		String [] checkUserName = check_UserCheck.split("_");
		
		
		Document InfoIssu = null;
		if(check_UserCheck.contains("USER_CHECK")) {
			String taxCode = docSub.get("TaxCode", "");
			String Issu = docSub.getObjectId("_id").toString();
			Document findUser = new Document("UserName", taxCode)
			.append("IssuerId", Issu)
			.append("IsDelete", new Document("$ne", true)); 
			
			
			mongoClient = cfg.mongoClient();
			collection = mongoClient.getDatabase(cfg.dbName).getCollection("Users");
			try {
				InfoIssu =  collection.find(findUser).iterator().next();
			} catch (Exception e) {
				// TODO: handle exception
			}
				
			mongoClient.close();				
		}
		
		Document InfoAdmin = null;
		if(checkUserName.length > 1) {
			String taxCode = checkUserName[0];
			String Issu = docSub.getObjectId("_id").toString();
			Document findUser = new Document("UserName", taxCode)
			.append("IssuerId", Issu)
			.append("IsAdmin", true)
			.append("IsDelete", new Document("$ne", true)); 
			
			
			mongoClient = cfg.mongoClient();
			collection = mongoClient.getDatabase(cfg.dbName).getCollection("Users");
			try {
				InfoAdmin =  collection.find(findUser).iterator().next();	
			} catch (Exception e) {
				// TODO: handle exception
			}
			
			mongoClient.close();	
						
		}
	
		Document InfoUser = null;
		if(checkUserName.length > 1) {
			String taxCode = checkUserName[0];
			String Issu = docSub.getObjectId("_id").toString();
			Document findUser = new Document("UserName", taxCode)
			.append("IssuerId", Issu)
			.append("IsDelete", new Document("$ne", true)); 
			
			
//			mongoClient = cfg.mongoClient();
//			Iterable<Document> cursor1 = collection.find(findUser);
//			Iterator<Document> iter1 = cursor1.iterator();
//			mongoClient.close();
//			if(iter1.hasNext())
//				InfoUser = iter1.next();
			
			mongoClient = cfg.mongoClient();
			collection = mongoClient.getDatabase(cfg.dbName).getCollection("Users");
			try {
				InfoUser =  collection.find(findUser).iterator().next();	
			} catch (Exception e) {
				// TODO: handle exception
			}
			
			mongoClient.close();	
						
		}
		
		//END CHECK USER CHECK 
		
		
		
		
		
		res = new LoginRes();
		
		if(InfoAdmin!=null) {
			res.setIssuerId(InfoAdmin.get("IssuerId", ""));
			res.setUserId(InfoAdmin.getObjectId("_id").toString());
			res.setUserName(InfoAdmin.getString("UserName"));
			res.setPassword(InfoAdmin.get("Password", ""));
			res.setFullName(InfoAdmin.get("FullName", ""));
			res.setPhone(InfoAdmin.get("Phone", ""));
			res.setEmail(InfoAdmin.get("Email", ""));
			res.setAdIssu(InfoAdmin.get("AdIssu",false));
			res.setRoles(InfoAdmin.get("Roles", ""));
			res.setRoot(InfoAdmin.get("IsRoot", false));
			res.setKh(InfoAdmin.get("IsKH", false));
			res.setAdmin(InfoAdmin.get("IsAdmin", false));
			res.setIssuerInfo(issuer);		
		}
		
		else if(InfoIssu!=null) {
			res.setIssuerId(InfoIssu.get("IssuerId", ""));
			res.setUserId(InfoIssu.getObjectId("_id").toString());
			res.setUserName(InfoIssu.getString("UserName"));
			res.setPassword(InfoIssu.get("Password", ""));
			res.setFullName(InfoIssu.get("FullName", ""));
			res.setPhone(InfoIssu.get("Phone", ""));
			res.setEmail(InfoIssu.get("Email", ""));
			res.setAdIssu(InfoIssu.get("AdIssu",false));
			res.setRoles(InfoIssu.get("Roles", ""));
			res.setRoot(InfoIssu.get("IsRoot", false));
			res.setKh(InfoIssu.get("IsKH", false));
			res.setAdmin(InfoIssu.get("IsAdmin", false));
			res.setIssuerInfo(issuer);		
		}
		else if(InfoUser!=null) {
			res.setIssuerId(InfoUser.get("IssuerId", ""));
			res.setUserId(InfoUser.getObjectId("_id").toString());
			res.setUserName(InfoUser.getString("UserName"));
			res.setPassword(InfoUser.get("Password", ""));
			res.setFullName(InfoUser.get("FullName", ""));
			res.setPhone(InfoUser.get("Phone", ""));
			res.setEmail(InfoUser.get("Email", ""));
			res.setAdIssu(InfoUser.get("AdIssu",false));
			res.setRoles(InfoUser.get("Roles", ""));
			res.setRoot(InfoUser.get("IsRoot", false));
			res.setKh(InfoUser.get("IsKH", false));
			res.setAdmin(InfoUser.get("IsAdmin", false));
			res.setIssuerInfo(issuer);		
		}
		else {
		
		String FullNameIssuer = docTmp.getEmbedded(Arrays.asList("IssuerInfo", "Name"), "");
		String FullNameUserName = docTmp.get("FullName", "");	
			
		String FullName = FullNameUserName;
		
		if(!FullNameUserName.equals(FullNameIssuer)) {
			FullName = FullNameIssuer;
		}
		res.setIssuerId(docTmp.get("IssuerId", ""));
		res.setUserId(docTmp.getObjectId("_id").toString());
		res.setUserName(docTmp.getString("UserName"));
		res.setPassword(docTmp.get("Password", ""));
		res.setFullName(FullName);
		res.setPhone(docTmp.get("Phone", ""));
		res.setEmail(docTmp.get("Email", ""));
		res.setAdIssu(docTmp.get("AdIssu",false));
		res.setRoles(docTmp.get("Roles", ""));
		res.setRoot(docTmp.get("IsRoot", false));
		res.setKh(docTmp.get("IsKH", false));
		res.setAdmin(docTmp.get("IsAdmin", false));
		res.setIssuerInfo(issuer);	
		}
		
		
		//Check quyen
		Document docTmp1 = null;
		boolean check_role = docTmp.getBoolean("IsRole", false);
		if(check_role==true) {			
			////START
			if(null != docTmp.get("RolesRightManageInfo")) {
				String id_RolesRightManageInfo = docTmp.getEmbedded(Arrays.asList("RolesRightManageInfo", "_id"), "");
				objectId = new ObjectId(id_RolesRightManageInfo);			

				Document docFindRoleManage = new Document("IsDelete", new Document("$ne", true))
						.append("_id", objectId);
				

				mongoClient = cfg.mongoClient();
				collection = mongoClient.getDatabase(cfg.dbName).getCollection("RolesRightManage");
				try {
					docTmp1 =   collection.find(docFindRoleManage).allowDiskUse(true).iterator().next();		
				} catch (Exception e) {
					// TODO: handle exception
				}
				mongoClient.close();
				
				
	
				if (null == docTmp1) {
					res.setStatusCode(1);
					res.setStatusText("Lỗi xảy ra. Vui lòng liên hệ admin để được xử lý!!!");
					return res;
				}
				
				
				if(null != docTmp1.get("FunctionRights") && docTmp1.get("FunctionRights") instanceof ArrayList)
					res.setFullRights((ArrayList<String>) docTmp1.get("FunctionRights"));
				
				
				
			}		
			LocalDateTime dateTimeFrom = null;
			LocalDateTime dateTimeTo = null;
			LocalDateTime dateTimeNow = LocalDateTime.now();
			/*KIEM TRA NGAY HIEU LUC - NGAY HET HAN CUA USER*/
			dateTimeFrom = null;
			dateTimeTo = null;
			if(null != docTmp.get("EffectDate") && docTmp.get("EffectDate") instanceof Date)
				dateTimeFrom = commons.convertDateToLocalDateTime((Date) docTmp.get("EffectDate"));
			if(null != docTmp.get("ExpireDate") && docTmp.get("ExpireDate") instanceof Date)
				dateTimeTo = commons.convertDateToLocalDateTime((Date) docTmp.get("ExpireDate"));
			if(dateTimeFrom == null || dateTimeTo == null) {
				res = new LoginRes();
				res.setStatusCode(999);
				res.setStatusText("Ngày hiệu lực hoặc ngày hết hạn của người dùng không đúng.");
				return res;
			}else if(commons.compareLocalDateTime(dateTimeFrom, dateTimeNow) > 0 
					|| commons.compareLocalDateTime(dateTimeNow, dateTimeTo) > 0){
				res = new LoginRes();
				res.setStatusCode(999);
				res.setStatusText("Người dùng chưa đến ngày hiệu lực hoặc đã hết hạn.");
				return res;
			}
			if(!docTmp.getBoolean("IsActive").booleanValue()) {
				res = new LoginRes();
				res.setStatusCode(999);
				res.setStatusText("Người dùng chưa được kích hoạt.");
				return res;
			}
			//END
		}
		//end check quyen
		
		else {
			res.setFullRights(new ArrayList<String>());
		}
		
	
		
		///END 
	
		
	
		
		/*JWT*/
		Map<String, Object> header = new HashMap<>();
		header.put(Header.TYPE, Constants.TOKEN_TYPE);
		header.put(JwsHeader.ALGORITHM, SignatureAlgorithm.HS512);
		
		Map<String, Object> claims = new HashMap<>();
		claims.put("data01", res.getIssuerId());
		claims.put("data02", res.getUserId());
		claims.put("data03", res.getUserName());
		claims.put("data04", commons.csRandomAlphaNumbericString(32));
		
		SignatureAlgorithm signatureAlgorithm = SignatureAlgorithm.HS512;
		byte[] apiKeySecretBytes = DatatypeConverter.parseBase64Binary(Base64.getEncoder().encodeToString(Constants.JWT_SECRET.getBytes()));
		Key signingKey = new SecretKeySpec(apiKeySecretBytes, signatureAlgorithm.getJcaName());
		
		LocalDateTime now = LocalDateTime.now();
		now = now.plus(1, ChronoUnit.DAYS);
		
		JwtBuilder builder = Jwts.builder()
				.setHeader(header)
				.setId(null)
				.setClaims(claims)
				.setSubject(res.getUserId())
				.setIssuedAt(new Date())
				.setIssuer(Constants.TOKEN_ISSUER)
				.setAudience(Constants.TOKEN_AUDIENCE)
				.setExpiration(new Date(now.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()))
				.signWith(signingKey, signatureAlgorithm);
		
		String token = builder.compact();
		res.setToken(token);
		/*END - JWT*/
		res.setStatusCode(0);
		return res;
	}

}
