package vn.sesgroup.hddt.controller.common;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoField;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import javax.imageio.ImageIO;
import javax.security.auth.x500.X500Principal;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.xml.bind.DatatypeConverter;

import org.apache.commons.io.FilenameUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.poi.util.IOUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.FileCopyUtils;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;

import com.api.message.JSONRoot;
import com.api.message.Msg;
import com.api.message.MsgPage;
import com.api.message.MsgParam;
import com.api.message.MsgParams;
import com.api.message.MsgRsp;
import com.api.message.MspResponseStatus;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;

import vn.sesgroup.hddt.controller.AbstractController;
import vn.sesgroup.hddt.dto.BaseDTO;
import vn.sesgroup.hddt.dto.CurrentUserProfile;
import vn.sesgroup.hddt.dto.FileInfo;
import vn.sesgroup.hddt.dto.JsonGridDTO;
import vn.sesgroup.hddt.dto.LoginRes;
import vn.sesgroup.hddt.dto.SignTypeInfo;
import vn.sesgroup.hddt.resources.RestAPIUtility;
import vn.sesgroup.hddt.utils.Constants;
import vn.sesgroup.hddt.utils.Json;
import vn.sesgroup.hddt.utils.SystemParams;

@Controller
@RequestMapping(value = {"/"})
@Scope(value = WebApplicationContext.SCOPE_REQUEST)
public class CommonController extends AbstractController{
	private static final Logger log = LogManager.getLogger(CommonController.class);
	@Autowired RestAPIUtility restAPI;
	private String _token;
	private List<String> ids = null;
	
	@RequestMapping(value = "/common/get-mau-so",  produces = MediaType.APPLICATION_JSON_VALUE, method = RequestMethod.POST)
	@ResponseBody
	public List<?> loai_hd(Locale locale, HttpServletRequest req, HttpSession session
			, @RequestParam(name = "loai_hd", defaultValue = "", required = true) String loai_hd) throws Exception{
		List<Object> rows = new ArrayList<Object>();
		
		try {
			CurrentUserProfile cup = getCurrentlyAuthenticatedPrincipal();
			
			BaseDTO baseDTO = new BaseDTO(req);
			Msg msg = baseDTO.createMsg(cup, Constants.MSG_ACTION_CODE.LOAD_PARAMS);
			
			/*DANH SACH THAM SO*/
			HashMap<String, String> hashConds = null;
			ArrayList<HashMap<String, String>> conds = null;
			MsgParam msgParam = null;
			MsgParams msgParams = new MsgParams();
			
			msgParam = new MsgParam();
			msgParam.setId("param01");
			msgParam.setParam("DMMauSoKyHieuForCreate");
			msgParams.getParams().add(msgParam);
			
			msg.setObjData(msgParams);
			
			JSONRoot root = new JSONRoot(msg);
			MsgRsp rsp = restAPI.callAPINormal("/commons/get-full-params", cup.getLoginRes().getToken(), HttpMethod.POST, root);
			MspResponseStatus rspStatus = rsp.getResponseStatus();
			if(rspStatus.getErrorCode() == 0 &&
					rsp.getObjData() != null) {
				JsonNode jsonData = Json.serializer().nodeFromObject(rsp.getObjData());
				if(null != jsonData.at("/param01") && jsonData.at("/param01") instanceof ArrayNode) {
					for(JsonNode o: jsonData.at("/param01")) {
						if(loai_hd.equals(commons.getTextJsonNode(o.get("KHMSHDon")))){
					String	KHHDon = commons.getTextJsonNode(o.get("KHHDon"));	
						char words=KHHDon.charAt(KHHDon.length() - 3);
						String s=String.valueOf(words);  
						if("M".equals(s)) {
						rows.add(
							new LinkedHashMap<String, String>(){
								private static final long serialVersionUID = 5320109478345573105L;
								{put("_id", commons.getTextJsonNode(o.at("/_id")));}
								{put("name",commons.getTextJsonNode(o.get("KHMSHDon")) + commons.getTextJsonNode(o.get("KHHDon")));}
							}
						);
						}
						}
					}
				}
				
				

				
			}
		}catch(Exception e) {
			rows = new ArrayList<Object>();
		}
		
		return rows;
	}
	
	
	@RequestMapping(value = "/common/generatingCaptcha", method = {RequestMethod.POST})
	public ResponseEntity<?> genCaptcha(HttpServletRequest req) throws Exception{
		HttpHeaders httpHeaders = new HttpHeaders();
		httpHeaders.setContentType(org.springframework.http.MediaType.TEXT_PLAIN);

		String captcha = commons.csRandomNumbericString(6);
		req.getSession(true).setAttribute(Constants.SESSION_CAPTCHA, captcha);

		BufferedImage bufferedImage = new BufferedImage(150, 45, BufferedImage.TYPE_INT_RGB);

		Graphics2D graphics = bufferedImage.createGraphics();

		graphics.setColor(Color.WHITE);
		graphics.fillRect(0, 0, 150, 45);
		graphics.setColor(Color.CYAN);
		graphics.drawRoundRect(0, 0, 149, 44, 10, 10);

		graphics.setColor(Color.GREEN);
		graphics.drawRoundRect(-10, 20, 140, 25, 40, 10);
		graphics.setColor(Color.MAGENTA);
		graphics.drawRoundRect(30, 0, 130, 15, 10, 90);
		
		Color color = null;
		for (int i = 1; i <= captcha.length(); i++) {
			color = new Color(12, 14, 63);
			graphics.setFont(new Font("Courier New", Font.BOLD, 30));		//Arial	Font.PLAIN
			graphics.setColor(color);
			graphics.drawString(String.valueOf(captcha.charAt(i - 1)), (i - 1) * 25, 35);
		}
		graphics.dispose();
		
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		ImageIO.write(bufferedImage, "jpg", baos);
		baos.flush();
		byte[] imageInByteArray = baos.toByteArray();
		baos.close();
		String b64 = javax.xml.bind.DatatypeConverter.printBase64Binary(imageInByteArray);

		return new ResponseEntity<byte[]>(b64.getBytes("utf-8"), httpHeaders, HttpStatus.OK);
	}
	
	
	
	
	
	@RequestMapping(value = "/common/check_finish", method = {RequestMethod.POST})
	@ResponseBody
	public BaseDTO check_finish(Locale locale, HttpServletRequest req, HttpSession session
			) throws Exception{
		BaseDTO dtoRes = new BaseDTO();
		dtoRes = new BaseDTO(req);	
		String file = "";
		String cks = "";
		file = commons.getParameterFromRequest(req, "SerialNumber");
		cks = commons.getParameterFromRequest(req, "xmlFile");
		try {
		 HttpClient client = HttpClient.newHttpClient();
	        HttpRequest request = HttpRequest.newBuilder()
	                .uri(URI.create("http://localhost:11284/signXML"))
					/* .POST(HttpRequest.BodyPublishers.ofString(getFormDataAsString(formData))) */
	                .build();
	        HttpResponse<String> response = client.send(request,
	                HttpResponse.BodyHandlers.ofString());
	        if(response.body().equals("")) {
	        	dtoRes.setErrorCode(999);
	    		return dtoRes;
	        }
	        
			HashMap<String, String> hR = new HashMap<String, String>();
			hR.put("cert", response.body());				
			dtoRes.setErrorCode(0);
			dtoRes.setResponseData(hR);
		}catch (Exception e) {
			dtoRes.setErrorCode(999);
		}
			return dtoRes;
	}
	

	
	@RequestMapping(value = "/common/openplugin", method = {RequestMethod.POST})
    @ResponseBody
	public BaseDTO openplugin(String targetURL, String urlParameters) throws Exception{
		  BaseDTO dtoRes = new BaseDTO();
		   HashMap<String, String> hR = new HashMap<String, String>();
		  
		   URL urlForGetRequest = new URL("http://localhost:11284/getCert");
		    String readLine = null;
		    HttpURLConnection conection = (HttpURLConnection) urlForGetRequest.openConnection();
		    conection.setRequestMethod("GET");
		    int responseCode = conection.getResponseCode();


		    if (responseCode == HttpURLConnection.HTTP_OK) {
		        BufferedReader in = new BufferedReader(
		            new InputStreamReader(conection.getInputStream()));
		        StringBuffer response = new StringBuffer();
		        while ((readLine = in .readLine()) != null) {
		            response.append(readLine);
		        } in .close();
		     
		          hR.put("cert", response.toString());
		          if(response.toString().equals("")) {
		            dtoRes.setResponseData(hR);
		           	  return dtoRes;
		      
		    } else {
		    	dtoRes.setErrorCode(0);
	            dtoRes.setResponseData(hR);
	           	  return dtoRes;
		    }
		  
          }
      	  return dtoRes;
		}
	
	
	@RequestMapping(value = {"/common/check-cert", "/common/check-cert-full"},  produces = MediaType.APPLICATION_JSON_VALUE, method = RequestMethod.POST)
	@ResponseBody
	public BaseDTO execCheckCert(Locale locale, HttpServletRequest req, HttpSession session
			, @RequestParam(value = "cert", required = false, defaultValue = "") String cert
			, @RequestAttribute(name = "method", value = "") String method
		) throws Exception {
		BaseDTO dtoRes = new BaseDTO();
		
//		System.out.println("method = " + method);
//		System.out.println(req.getRequestURI());
		
		if("".equals(cert)) {
			dtoRes.setErrorCode(1);
			dtoRes.setResponseData("Chứng thư số không hợp lệ (CKS Rỗng)");
			return dtoRes;
		}
		
		//LAY THONG TIN CERT - KIEM TRA SAU
				CertificateFactory certificateFactory = null;
				InputStream in = null;
				X509Certificate x509Cert = null;
				
				certificateFactory = CertificateFactory.getInstance("X.509");
				cert = cert.replaceAll("@", "+");
				in = new ByteArrayInputStream(DatatypeConverter.parseBase64Binary(cert));
				x509Cert = (X509Certificate) certificateFactory.generateCertificate(in);	
				String serialNumber = x509Cert.getSerialNumber().toString(16);
				SignTypeInfo signTypeInfo = commons.parserCert(cert);
				 String mst = signTypeInfo.getTaxCode();
				 
				 
		if(serialNumber.length() <32)
		{
			serialNumber = "0" +serialNumber;
		}
		
		CurrentUserProfile cup = getCurrentlyAuthenticatedPrincipal();
		LoginRes issu = cup.getLoginRes();
		String mstLogin = issu.getUserName();
		
		String []split_mst =  mstLogin.split("_");
		int dem_mst = split_mst.length;
		if(dem_mst==2) {
			mstLogin = split_mst[0];
		}else {
			mstLogin = mstLogin;
		}
		
		if(!mstLogin.equals(mst)) {
			dtoRes.setErrorCode(1);
			dtoRes.setResponseData("Mã số thuế không khớp với chứng thư số");
			return dtoRes;
		}
		
		BaseDTO baseDTO = new BaseDTO(req);
		Msg msg = baseDTO.createMsg(cup, Constants.MSG_ACTION_CODE.INQUIRY);
		HashMap<String, String> hData = new HashMap<>();
		hData.put("MSTCKS", mst);
		msg.setObjData(hData);
		
		JSONRoot root = new JSONRoot(msg);
		MsgRsp rsp = restAPI.callAPINormal("/cks/check/" + serialNumber, cup.getLoginRes().getToken(), HttpMethod.POST, root);
		MspResponseStatus rspStatus = rsp.getResponseStatus();
		if(rspStatus.getErrorCode() == 0) {
			JsonNode jsonData = Json.serializer().nodeFromObject(rsp.getObjData());
		
		HashMap<String, String> hItem = null;
		
		if(serialNumber.length() % 2 == 1)
			serialNumber = "0" + serialNumber;
		
	boolean ccheck = false;
		if(!jsonData.at("/DSCTSSDung").isMissingNode()) {
			for(JsonNode o: jsonData.at("/DSCTSSDung")) {
				hItem = new LinkedHashMap<String, String>();
				String checkserri =  commons.getTextJsonNode(o.at("/Seri"));
				String isActive =  commons.getTextJsonNode(jsonData.at("/IsActive"));
				if(checkserri.length() <32)
				{
					checkserri = "0" +checkserri;
				}
			if(checkserri.equals(serialNumber)) {
				ccheck = true;
				break;
			}			
			if(isActive.equals("false")){
				ccheck = false;
				break;
			}
		}
	}
	if(ccheck != true) {
		dtoRes.setErrorCode(1);
		dtoRes.setResponseData("Chứng thư số không hợp lệ (không tìm thấy CKS).");
		return dtoRes;
	}
		}
		
		else if(rspStatus.getErrorCode() == 300) {
			dtoRes.setErrorCode(0);
			HashMap<String, String> hR = new LinkedHashMap<String, String>();
			hR.put("check", "300");
			hR.put("TTChuc", commons.getAttributeCertificate(x509Cert.getIssuerDN().toString(), "O"));
			hR.put("Seri", serialNumber);
			hR.put("TNgay", commons.convertLocalDateTimeToString(commons.convertDateGMT7ToLocalDateTime(x509Cert.getNotBefore()), Constants.FORMAT_DATE.FORMAT_DATE_TIME_WEB));
			hR.put("DNgay", commons.convertLocalDateTimeToString(commons.convertDateGMT7ToLocalDateTime(x509Cert.getNotAfter()), Constants.FORMAT_DATE.FORMAT_DATE_TIME_WEB));
			dtoRes.setResponseData(hR);
			return dtoRes;
		}
		else if(rspStatus.getErrorCode() == 301) {
			dtoRes.setErrorCode(0);
			HashMap<String, String> hR = new LinkedHashMap<String, String>();
			hR.put("check", "300");
			hR.put("Seri", serialNumber);
			dtoRes.setResponseData(hR);
			return dtoRes;
		}
		else {
			dtoRes.setErrorCode(1);
			dtoRes.setResponseData("Chứng thư số không hợp lệ (Vui lòng kiểm tra CKS đã đăng ký thành công chưa).");
			return dtoRes;
		}
		

		
		dtoRes.setErrorCode(0);
		if("check-cert-full".equals(method)) {
			HashMap<String, String> hR = new LinkedHashMap<String, String>();
			hR.put("TTChuc", commons.getAttributeCertificate(x509Cert.getIssuerDN().toString(), "O"));
			hR.put("Seri", serialNumber);
			hR.put("TNgay", commons.convertLocalDateTimeToString(commons.convertDateGMT7ToLocalDateTime(x509Cert.getNotBefore()), Constants.FORMAT_DATE.FORMAT_DATE_TIME_WEB));
			hR.put("DNgay", commons.convertLocalDateTimeToString(commons.convertDateGMT7ToLocalDateTime(x509Cert.getNotAfter()), Constants.FORMAT_DATE.FORMAT_DATE_TIME_WEB));
			dtoRes.setResponseData(hR);
		}else {
			HashMap<String, String> hR = new LinkedHashMap<String, String>();
			hR.put("Seri", serialNumber);
			hR.put("DateTime",  LocalDateTime.now().toString());
		dtoRes.setResponseData(hR);
		}
		return dtoRes;
	}
	
	
	
	
	
	
	
	
	@RequestMapping(value = {"/common/check-certcks"},  produces = MediaType.APPLICATION_JSON_VALUE, method = RequestMethod.POST)
	@ResponseBody
	public BaseDTO execCheckCertcks(Locale locale, HttpServletRequest req, HttpSession session
			, @RequestParam(value = "cert", required = false, defaultValue = "") String cert
			, @RequestAttribute(name = "method", value = "") String method
		) throws Exception {
		BaseDTO dtoRes = new BaseDTO();
		
//		System.out.println("method = " + method);
//		System.out.println(req.getRequestURI());
		
		if("".equals(cert)) {
			dtoRes.setErrorCode(1);
			dtoRes.setResponseData("Chứng thư số không hợp lệ.");
			return dtoRes;
		}
		
		//LAY THONG TIN CERT - KIEM TRA SAU
		CertificateFactory certificateFactory = null;
		InputStream in = null;
		X509Certificate x509Cert = null;
		
		certificateFactory = CertificateFactory.getInstance("X.509");
		cert = cert.replaceAll("@", "+");
		in = new ByteArrayInputStream(DatatypeConverter.parseBase64Binary(cert));
		x509Cert = (X509Certificate) certificateFactory.generateCertificate(in);
		
		String serialNumber = x509Cert.getSerialNumber().toString(16);

		
		if(serialNumber.length() % 2 == 1)
			serialNumber = "0" + serialNumber;
		
		dtoRes.setErrorCode(0);
		if("check-certcks".equals(method)) {
			HashMap<String, String> hR = new LinkedHashMap<String, String>();
			hR.put("TTChuc", commons.getAttributeCertificate(x509Cert.getIssuerDN().toString(), "O"));
			hR.put("Seri", serialNumber);
			hR.put("TNgay", commons.convertLocalDateTimeToString(commons.convertDateGMT7ToLocalDateTime(x509Cert.getNotBefore()), Constants.FORMAT_DATE.FORMAT_DATE_TIME_WEB));
			hR.put("DNgay", commons.convertLocalDateTimeToString(commons.convertDateGMT7ToLocalDateTime(x509Cert.getNotAfter()), Constants.FORMAT_DATE.FORMAT_DATE_TIME_WEB));
			dtoRes.setResponseData(hR);
		}else {
			dtoRes.setResponseData(serialNumber);	
		}
		return dtoRes;
	}

	
	
	
	
	
	
	
	@RequestMapping(value = "/common/get-file-to-sign/{file-name:.+}", method = RequestMethod.GET)
	public void getFileToSign(HttpServletRequest req, HttpServletResponse resp, HttpSession session
			, @PathVariable(name = "file-name", required = true) String fileName) throws Exception{
		PrintWriter writer = null;
		
		String[] info = fileName.split("-");
		if(info.length != 2) {
			resp.setContentType("text/html; charset=utf-8");
			resp.setCharacterEncoding("UTF-8");
			resp.setHeader("success", "yes");
			resp.setStatus(HttpStatus.NOT_FOUND.value());
			writer = resp.getWriter();
			writer.write(HttpStatus.NOT_FOUND.getReasonPhrase());
			writer.close();
			return;
		}
		
		LocalDateTime now = LocalDateTime.now();
		LocalDateTime time = null;
		try {
			time = commons.convertStringToLocalDateTime(info[0], Constants.FORMAT_DATE.FORMAT_DATETIME_DB_FULL);
		}catch(Exception e) {}
		long diffTime = ChronoUnit.SECONDS.between(time, now);
		if(diffTime > SystemParams.TIME_OUT_DOWNLOAD_FILE) {
			resp.setContentType("text/html; charset=utf-8");
	        resp.setCharacterEncoding("UTF-8");
	        resp.setHeader("success", "yes");
	        resp.setStatus(HttpStatus.FORBIDDEN.value());
	        writer = resp.getWriter();
	        writer.write(HttpStatus.FORBIDDEN.getReasonPhrase());
	        writer.close();
	        return;
		}
		
		Path path = Paths.get(SystemParams.DIR_TMP_SAVE_FILES, fileName);
		File file = path.toFile();
		if(!file.exists() || !file.isFile()) {
			resp.setContentType("text/html; charset=utf-8");
	        resp.setCharacterEncoding("UTF-8");
	        resp.setHeader("success", "yes");
	        resp.setStatus(HttpStatus.NOT_FOUND.value());
	        writer = resp.getWriter();
	        writer.write(HttpStatus.NOT_FOUND.getReasonPhrase());
	        writer.close();
	        return;
		}
		
		InputStream is = new FileInputStream(file);
		resp.setContentType("application/force-download");
	    resp.setHeader("Content-Disposition", "attachment; filename=" + fileName + "");
	    int read=0;
	    byte[] bytes = new byte[SystemParams.BUFFER_SIZE];
	    OutputStream os = resp.getOutputStream();

	    while((read = is.read(bytes))!= -1){
	        os.write(bytes, 0, read);
	    }
	    os.flush();
	    os.close(); 
		is.close();
		
//		resp.setContentType("text/html; charset=utf-8");
//		resp.setCharacterEncoding("UTF-8");
//		resp.setHeader("success", "yes");
//		writer = resp.getWriter();
//		writer.write(String.valueOf(diffTime));
//		writer.close();
		return;
	}


	
	@RequestMapping(value = "/common/get-chi-cuc-thue",  produces = MediaType.APPLICATION_JSON_VALUE, method = RequestMethod.POST)
	@ResponseBody
	public List<?> getDistricts(Locale locale, HttpServletRequest req, HttpSession session
			, @RequestParam(name = "tinhthanh_ma", defaultValue = "", required = true) String tinhthanh_ma) throws Exception{
		List<Object> rows = new ArrayList<Object>();
		
		try {
			CurrentUserProfile cup = getCurrentlyAuthenticatedPrincipal();
			
			BaseDTO baseDTO = new BaseDTO(req);
			Msg msg = baseDTO.createMsg(cup, Constants.MSG_ACTION_CODE.LOAD_PARAMS);
			
			/*DANH SACH THAM SO*/
			HashMap<String, String> hashConds = null;
			ArrayList<HashMap<String, String>> conds = null;
			MsgParam msgParam = null;
			MsgParams msgParams = new MsgParams();
			
			msgParam = new MsgParam();
			msgParam.setId("param01");
			msgParam.setParam("DMChiCucThue");			
			conds = new ArrayList<>();
			hashConds = new HashMap<>();
			hashConds.put("cond", "tinhthanh_ma");
			hashConds.put("condval", null == tinhthanh_ma? "": tinhthanh_ma);
			conds.add(hashConds);
			msgParam.setConds(conds);			
			msgParams.getParams().add(msgParam);
			
			msg.setObjData(msgParams);
			
			JSONRoot root = new JSONRoot(msg);
			MsgRsp rsp = restAPI.callAPINormal("/commons/get-full-params", cup.getLoginRes().getToken(), HttpMethod.POST, root);
			MspResponseStatus rspStatus = rsp.getResponseStatus();
			if(rspStatus.getErrorCode() == 0 &&
					rsp.getObjData() != null) {
				JsonNode jsonData = Json.serializer().nodeFromObject(rsp.getObjData());
				if(null != jsonData.at("/param01") && jsonData.at("/param01") instanceof ArrayNode) {
					for(JsonNode o: jsonData.at("/param01")) {
						rows.add(
							new LinkedHashMap<String, String>(){
								private static final long serialVersionUID = 5320109478345573105L;
								{put("code", commons.getTextJsonNode(o.at("/code")));}
								{put("name", commons.getTextJsonNode(o.at("/name")));}
							}
						);
					}
				}
			}
		}catch(Exception e) {
			rows = new ArrayList<Object>();
		}
		
		return rows;
	}
	
	@RequestMapping(value = "/common/get-phoi",  produces = MediaType.APPLICATION_JSON_VALUE, method = RequestMethod.POST)
	@ResponseBody	
	public List<?> getPhoi(Locale locale, HttpServletRequest req, HttpSession session
			, @RequestParam(name = "loaihd_ma", defaultValue = "", required = true) String loaihd_ma) throws Exception{
		List<Object> rows = new ArrayList<Object>();
		
		try {
			CurrentUserProfile cup = getCurrentlyAuthenticatedPrincipal();
			
			BaseDTO baseDTO = new BaseDTO(req);
			Msg msg = baseDTO.createMsg(cup, Constants.MSG_ACTION_CODE.LOAD_PARAMS);
			
			/*DANH SACH THAM SO*/
			HashMap<String, String> hashConds = null;
			ArrayList<HashMap<String, String>> conds = null;
			MsgParam msgParam = null;
			MsgParams msgParams = new MsgParams();
			
			msgParam = new MsgParam();
			msgParam.setId("param01");
			msgParam.setParam("DMTemplates");			
			conds = new ArrayList<>();
			hashConds = new HashMap<>();
			hashConds.put("cond", "loaihd_ma");
			hashConds.put("condval", null == loaihd_ma? "": loaihd_ma);
			conds.add(hashConds);
			msgParam.setConds(conds);			
			msgParams.getParams().add(msgParam);
			
			msg.setObjData(msgParams);
			
			JSONRoot root = new JSONRoot(msg);
			MsgRsp rsp = restAPI.callAPINormal("/commons/get-full-params", cup.getLoginRes().getToken(), HttpMethod.POST, root);
			MspResponseStatus rspStatus = rsp.getResponseStatus();
			if(rspStatus.getErrorCode() == 0 &&
					rsp.getObjData() != null) {
				JsonNode jsonData = Json.serializer().nodeFromObject(rsp.getObjData());
				if(null != jsonData.at("/param01") && jsonData.at("/param01") instanceof ArrayNode) {
					for(JsonNode o: jsonData.at("/param01")) {
						rows.add(
							new LinkedHashMap<String, String>(){
								private static final long serialVersionUID = 5320109478345573105L;
								{put("Images", commons.getTextJsonNode(o.at("/Images")));}
								{put("Name", commons.getTextJsonNode(o.at("/Name")));}
							}
						);
					}
				}
			}
		}catch(Exception e) {
			rows = new ArrayList<Object>();
		}
		
		return rows;
	}
	

	@RequestMapping(
			value = {"/common/viewpdf/{_id}", "/common/viewpdf-convert/{_id}"}
			, method = { RequestMethod.POST, RequestMethod.GET }
		)
		public void viewpdf(Locale locale, HttpServletRequest req, HttpServletResponse resp, HttpSession session
				, @PathVariable(value = "_id") String _id
			) throws Exception {
			PrintWriter writer = null;
			
			String uri = req.getRequestURI();
			boolean isConvert = uri.contains("viewpdf-convert");
			
			_id = _id.replaceAll("\\s", "");
			if("".equals(_id) || null == _id) {
				resp.setContentType("text/html; charset=utf-8");
		        resp.setCharacterEncoding("UTF-8");
		        resp.setHeader("success", "yes");
		        writer = resp.getWriter();
		        writer.write("Không tìm thấy thông tin mẫu hóa đơn.");
		        writer.flush();
		        writer.close();
		        return;
			}
			
			CurrentUserProfile cup = getCurrentlyAuthenticatedPrincipal();
			BaseDTO dto = new BaseDTO();
			
			dto = new BaseDTO(req);
			Msg msg = dto.createMsg(cup, Constants.MSG_ACTION_CODE.CREATED);
			HashMap<String, Object> hData = new HashMap<>();
			hData.put("_id", _id);
			hData.put("IsConvert", isConvert? "Y": "N");
			
			msg.setObjData(hData);
			JSONRoot root = new JSONRoot(msg);
			
			FileInfo fileInfo = restAPI.callAPIGetFileInfo("/commons/viewpdf", cup.getLoginRes().getToken(), HttpMethod.POST, root);
			if(null == fileInfo || null == fileInfo.getContentFile()) {
				resp.setContentType("text/html; charset=utf-8");
		        resp.setCharacterEncoding("UTF-8");
		        resp.setHeader("success", "yes");
		        writer = resp.getWriter();
		        writer.write("Xem mẫu hóa đơn không thành công.");
		        writer.flush();
		        writer.close();
		        return;
			}
			
			String type = "application/pdf";
			InputStream inputStream = new ByteArrayInputStream(fileInfo.getContentFile());
			resp.setHeader("Content-Type", type);
			
			resp.setHeader("Cache-Control", "no-cache, no-store, must-revalidate"); // HTTP 1.1.
			resp.setHeader("Pragma", "no-cache"); // HTTP 1.0.
			resp.setHeader("Expires", "0"); // Proxies.
			
			int bufferSize = 1024;
			resp.setContentType(type);
	        final byte[] buffer = new byte[bufferSize];
	        int bytesRead = 0;
	        OutputStream out = null;
	        try {
	            out = resp.getOutputStream();
	            long totalWritten = 0;
	            while ((bytesRead = inputStream.read(buffer)) > 0) {
	                out.write(buffer, 0, bytesRead);
	                totalWritten += bytesRead;
	                if (totalWritten >= buffer.length) {
	                    out.flush();
	                }
	            }
	        } finally {
	            tryToCloseStream(out);
	            tryToCloseStream(inputStream);
	        }
	     
		}	
	
	
	

	@RequestMapping(
			value = {"/common/viewpdftncn/{_id}", "/common/viewpdftncn-convert/{_id}"}
			, method = { RequestMethod.POST, RequestMethod.GET }
		)
		public void viewpdftncn(Locale locale, HttpServletRequest req, HttpServletResponse resp, HttpSession session
				, @PathVariable(value = "_id") String _id
			) throws Exception {
			PrintWriter writer = null;
			
			String uri = req.getRequestURI();
			boolean isConvert = uri.contains("viewpdf-convert");
			
			_id = _id.replaceAll("\\s", "");
			if("".equals(_id) || null == _id) {
				resp.setContentType("text/html; charset=utf-8");
		        resp.setCharacterEncoding("UTF-8");
		        resp.setHeader("success", "yes");
		        writer = resp.getWriter();
		        writer.write("Không tìm thấy thông tin mẫu hóa đơn.");
		        writer.flush();
		        writer.close();
		        return;
			}
			
			CurrentUserProfile cup = getCurrentlyAuthenticatedPrincipal();
			BaseDTO dto = new BaseDTO();
			
			dto = new BaseDTO(req);
			Msg msg = dto.createMsg(cup, Constants.MSG_ACTION_CODE.CREATED);
			HashMap<String, Object> hData = new HashMap<>();
			hData.put("_id", _id);
			hData.put("IsConvert", isConvert? "Y": "N");
			
			msg.setObjData(hData);
			JSONRoot root = new JSONRoot(msg);
			
			FileInfo fileInfo = restAPI.callAPIGetFileInfo("/commons/viewpdftncn", cup.getLoginRes().getToken(), HttpMethod.POST, root);
			if(null == fileInfo || null == fileInfo.getContentFile()) {
				resp.setContentType("text/html; charset=utf-8");
		        resp.setCharacterEncoding("UTF-8");
		        resp.setHeader("success", "yes");
		        writer = resp.getWriter();
		        writer.write("Xem mẫu hóa đơn không thành công.");
		        writer.flush();
		        writer.close();
		        return;
			}
			
			String type = "application/pdf";
			InputStream inputStream = new ByteArrayInputStream(fileInfo.getContentFile());
			resp.setHeader("Content-Type", type);
			
			resp.setHeader("Cache-Control", "no-cache, no-store, must-revalidate"); // HTTP 1.1.
			resp.setHeader("Pragma", "no-cache"); // HTTP 1.0.
			resp.setHeader("Expires", "0"); // Proxies.
			
			int bufferSize = 1024;
			resp.setContentType(type);
	        final byte[] buffer = new byte[bufferSize];
	        int bytesRead = 0;
	        OutputStream out = null;
	        try {
	            out = resp.getOutputStream();
	            long totalWritten = 0;
	            while ((bytesRead = inputStream.read(buffer)) > 0) {
	                out.write(buffer, 0, bytesRead);
	                totalWritten += bytesRead;
	                if (totalWritten >= buffer.length) {
	                    out.flush();
	                }
	            }
	        } finally {
	            tryToCloseStream(out);
	            tryToCloseStream(inputStream);
	        }
	     
		}	
	
	
	

	@RequestMapping(
			value = {"/common/viewpdfcttncn/{_id}", "/common/viewpdfcttncn-convert/{_id}"}
			, method = { RequestMethod.POST, RequestMethod.GET }
		)
		public void viewpdfcttncn(Locale locale, HttpServletRequest req, HttpServletResponse resp, HttpSession session
				, @PathVariable(value = "_id") String _id
			) throws Exception {
			PrintWriter writer = null;
			
			String uri = req.getRequestURI();
			boolean isConvert = uri.contains("viewpdf-convert");
			
			_id = _id.replaceAll("\\s", "");
			if("".equals(_id) || null == _id) {
				resp.setContentType("text/html; charset=utf-8");
		        resp.setCharacterEncoding("UTF-8");
		        resp.setHeader("success", "yes");
		        writer = resp.getWriter();
		        writer.write("Không tìm thấy thông tin mẫu hóa đơn.");
		        writer.flush();
		        writer.close();
		        return;
			}
			
			CurrentUserProfile cup = getCurrentlyAuthenticatedPrincipal();
			BaseDTO dto = new BaseDTO();
			
			dto = new BaseDTO(req);
			Msg msg = dto.createMsg(cup, Constants.MSG_ACTION_CODE.CREATED);
			HashMap<String, Object> hData = new HashMap<>();
			hData.put("_id", _id);
			hData.put("IsConvert", isConvert? "Y": "N");
			
			msg.setObjData(hData);
			JSONRoot root = new JSONRoot(msg);
			
			FileInfo fileInfo = restAPI.callAPIGetFileInfo("/commons/viewpdfcttncn", cup.getLoginRes().getToken(), HttpMethod.POST, root);
			if(null == fileInfo || null == fileInfo.getContentFile()) {
				resp.setContentType("text/html; charset=utf-8");
		        resp.setCharacterEncoding("UTF-8");
		        resp.setHeader("success", "yes");
		        writer = resp.getWriter();
		        writer.write("Xem mẫu hóa đơn không thành công.");
		        writer.flush();
		        writer.close();
		        return;
			}
			
			String type = "application/pdf";
			InputStream inputStream = new ByteArrayInputStream(fileInfo.getContentFile());
			resp.setHeader("Content-Type", type);
			
			resp.setHeader("Cache-Control", "no-cache, no-store, must-revalidate"); // HTTP 1.1.
			resp.setHeader("Pragma", "no-cache"); // HTTP 1.0.
			resp.setHeader("Expires", "0"); // Proxies.
			
			int bufferSize = 1024;
			resp.setContentType(type);
	        final byte[] buffer = new byte[bufferSize];
	        int bytesRead = 0;
	        OutputStream out = null;
	        try {
	            out = resp.getOutputStream();
	            long totalWritten = 0;
	            while ((bytesRead = inputStream.read(buffer)) > 0) {
	                out.write(buffer, 0, bytesRead);
	                totalWritten += bytesRead;
	                if (totalWritten >= buffer.length) {
	                    out.flush();
	                }
	            }
	        } finally {
	            tryToCloseStream(out);
	            tryToCloseStream(inputStream);
	        }
	     
		}	
	
	
	
	
	
	
	
	@RequestMapping(
			value = {"/common/print04/{_id}", "/common/print04-convert/{_id}"}
			, method = { RequestMethod.POST, RequestMethod.GET }
		)
		public void print04(Locale locale, HttpServletRequest req, HttpServletResponse resp, HttpSession session
				, @PathVariable(value = "_id") String _id
			) throws Exception {
			PrintWriter writer = null;
			
			String uri = req.getRequestURI();
			boolean isConvert = uri.contains("print04-convert");
			
			_id = _id.replaceAll("\\s", "");
			if("".equals(_id) || null == _id) {
				resp.setContentType("text/html; charset=utf-8");
		        resp.setCharacterEncoding("UTF-8");
		        resp.setHeader("success", "yes");
		        writer = resp.getWriter();
		        writer.write("Không tìm thấy thông tin mẫu hóa đơn sai sót.");
		        writer.flush();
		        writer.close();
		        return;
			}
			
			CurrentUserProfile cup = getCurrentlyAuthenticatedPrincipal();
			BaseDTO dto = new BaseDTO();
			
			dto = new BaseDTO(req);
			Msg msg = dto.createMsg(cup, Constants.MSG_ACTION_CODE.CREATED);
			HashMap<String, Object> hData = new HashMap<>();
			hData.put("_id", _id);
			hData.put("IsConvert", isConvert? "Y": "N");
			
			msg.setObjData(hData);
			JSONRoot root = new JSONRoot(msg);
			
			FileInfo fileInfo = restAPI.callAPIGetFileInfo("/commons/print04", cup.getLoginRes().getToken(), HttpMethod.POST, root);
			if(null == fileInfo || null == fileInfo.getContentFile()) {
				resp.setContentType("text/html; charset=utf-8");
		        resp.setCharacterEncoding("UTF-8");
		        resp.setHeader("success", "yes");
		        writer = resp.getWriter();
		        writer.write("Xem mẫu hóa đơn sai sót không thành công.");
		        writer.flush();
		        writer.close();
		        return;
			}
			
			String type = "application/pdf";
			InputStream inputStream = new ByteArrayInputStream(fileInfo.getContentFile());
			resp.setHeader("Content-Type", type);
			
			resp.setHeader("Cache-Control", "no-cache, no-store, must-revalidate"); // HTTP 1.1.
			resp.setHeader("Pragma", "no-cache"); // HTTP 1.0.
			resp.setHeader("Expires", "0"); // Proxies.
			
			int bufferSize = 1024;
			resp.setContentType(type);
	        final byte[] buffer = new byte[bufferSize];
	        int bytesRead = 0;
	        OutputStream out = null;
	        try {
	            out = resp.getOutputStream();
	            long totalWritten = 0;
	            while ((bytesRead = inputStream.read(buffer)) > 0) {
	                out.write(buffer, 0, bytesRead);
	                totalWritten += bytesRead;
	                if (totalWritten >= buffer.length) {
	                    out.flush();
	                }
	            }
	        } finally {
	            tryToCloseStream(out);
	            tryToCloseStream(inputStream);
	        }
	     
		}	
	@RequestMapping(
		value = {"/common/print-einvoice/{_id}", "/common/print-einvoice-convert/{_id}"}
		, method = { RequestMethod.POST, RequestMethod.GET }
	)
	public void printEInvoice(Locale locale, HttpServletRequest req, HttpServletResponse resp, HttpSession session
			, @PathVariable(value = "_id") String _id
		) throws Exception {
		PrintWriter writer = null;
		
		try {
		String uri = req.getRequestURI();
		boolean isConvert = uri.contains("print-einvoice-convert");
		
		_id = _id.replaceAll("\\s", "");
		if("".equals(_id) || null == _id) {
			resp.setContentType("text/html; charset=utf-8");
	        resp.setCharacterEncoding("UTF-8");
	        resp.setHeader("success", "yes");
	        writer = resp.getWriter();
	        writer.write("Không tìm thấy thông tin hóa đơn.");
	        writer.flush();
	        writer.close();
	        return;
		}
		
		CurrentUserProfile cup = getCurrentlyAuthenticatedPrincipal();
		BaseDTO dto = new BaseDTO();
		dto = new BaseDTO(req);
		Msg msg = dto.createMsg(cup, Constants.MSG_ACTION_CODE.CREATED);
		HashMap<String, Object> hData = new HashMap<>();
		hData.put("_id", _id);
		hData.put("IsConvert", isConvert? "Y": "N");
		
		msg.setObjData(hData);
		JSONRoot root = new JSONRoot(msg);
		
		FileInfo fileInfo = restAPI.callAPIGetFileInfo("/commons/print-einvoice", cup.getLoginRes().getToken(), HttpMethod.POST, root);
		if(null == fileInfo || null == fileInfo.getContentFile()) {
			resp.setContentType("text/html; charset=utf-8");
	        resp.setCharacterEncoding("UTF-8");
	        resp.setHeader("success", "yes");
	        writer = resp.getWriter();
	        writer.write("In hóa đơn không thành công.");
	        writer.flush();
	        writer.close();
	        return;
		}
		
		String type = "application/pdf";
		InputStream inputStream = new ByteArrayInputStream(fileInfo.getContentFile());
		resp.setHeader("Content-Type", type);
		
		resp.setHeader("Cache-Control", "no-cache, no-store, must-revalidate"); // HTTP 1.1.
		resp.setHeader("Pragma", "no-cache"); // HTTP 1.0.
		resp.setHeader("Expires", "0"); // Proxies.
		
		int bufferSize = 1024;
		resp.setContentType(type);
        final byte[] buffer = new byte[bufferSize];
        int bytesRead = 0;
        OutputStream out = null;
        try {
            out = resp.getOutputStream();
            long totalWritten = 0;
            while ((bytesRead = inputStream.read(buffer)) > 0) {
                out.write(buffer, 0, bytesRead);
                totalWritten += bytesRead;
                if (totalWritten >= buffer.length) {
                    out.flush();
                }
            }
        } finally {
            tryToCloseStream(out);
            tryToCloseStream(inputStream);
        }
        
		}catch (Exception e) {
			resp.setContentType("text/html; charset=utf-8");
	        resp.setCharacterEncoding("UTF-8");
	        resp.setHeader("success", "yes");
	        writer = resp.getWriter();
	        writer.write("Không tìm thấy thông tin hóa đơn. Vui lòng thử lại!!!");
	        writer.flush();
	        writer.close();
	        return;
		}
	}

	
	
	
	
	
	
	@RequestMapping(value = {"/common/print-einvoiceAll/{_id}", "/common/print-einvoiceCD/{_id}"}, method = {RequestMethod.POST, RequestMethod.GET})
	public void printEInvoiceAll(Locale locale, HttpServletRequest req, HttpServletResponse resp, HttpSession session, @PathVariable(value = "_id") String _id) throws Exception {
	    PrintWriter writer = null;
	    FileInfo fileInfo = null;
	    String uri = req.getRequestURI();
	    boolean isConvert = uri.contains("print-einvoiceCD");

	    try {
	        _id = _id.replaceAll("\\s", "");
	        if ("".equals(_id) || null == _id) {
	            resp.setContentType("text/html; charset=utf-8");
	            resp.setCharacterEncoding("UTF-8");
	            resp.setHeader("success", "yes");
	            writer = resp.getWriter();
	            writer.write("Không tìm thấy thông tin hóa đơn.");
	            writer.flush();
	            return;
	        }

	        CurrentUserProfile cup = getCurrentlyAuthenticatedPrincipal();
	        BaseDTO dto = new BaseDTO();
	        dto = new BaseDTO(req);
	        Msg msg = dto.createMsg(cup, Constants.MSG_ACTION_CODE.CREATED);
	        HashMap<String, Object> hData = new HashMap<>();
	        hData.put("_token", _id);
	        hData.put("IsConvert", isConvert ? "Y" : "N");

	        msg.setObjData(hData);
	        JSONRoot root = new JSONRoot(msg);
	        long startTime = System.currentTimeMillis();
	        fileInfo = restAPI.callAPIGetFilePDF("/commons/print-einvoiceAll", cup.getLoginRes().getToken(), HttpMethod.POST, root);
	        long endTime = System.currentTimeMillis();
	        long totalTime = (endTime - startTime) / 1000; // Chia cho 1000 để đổi sang giây
	        System.out.println("Thoi gian thuc hien view PDF hang loat: " + totalTime + " giay");

	        if (null == fileInfo || null == fileInfo.getContentFile()) {
	            resp.setContentType("text/html; charset=utf-8");
	            resp.setCharacterEncoding("UTF-8");
	            resp.setHeader("success", "yes");
	            writer = resp.getWriter();
	            writer.write("In hóa đơn không thành công.");
	            writer.flush();
	            return;
	        }

	        String type = "application/pdf";
	        InputStream inputStream = new ByteArrayInputStream(fileInfo.getContentFile());
	        resp.setHeader("Content-Type", type);
	        resp.setHeader("Cache-Control", "no-cache, no-store, must-revalidate"); // HTTP 1.1.
	        resp.setHeader("Pragma", "no-cache"); // HTTP 1.0.
	        resp.setHeader("Expires", "0"); // Proxies.
	        int bufferSize = 1024;
	        resp.setContentType(type);
	        final byte[] buffer = new byte[bufferSize];
	        int bytesRead;
	        OutputStream out = null;
	        try {
	            out = resp.getOutputStream();
	            while ((bytesRead = inputStream.read(buffer)) > 0) {
	                out.write(buffer, 0, bytesRead);
	            }
	        } finally {
	            if (out != null) {
	                out.flush();
	                out.close();
	            }
	            if (inputStream != null) {
	                inputStream.close();
	            }
	        }
	    } catch (Exception e) {
	        resp.setContentType("text/html; charset=utf-8");
	        resp.setCharacterEncoding("UTF-8");
	        resp.setHeader("success", "yes");
	        writer = resp.getWriter();
	        writer.write("Không tìm thấy thông tin hóa đơn. Vui lòng thử lại!!!");
	        writer.flush();
	    }
	}

	
	
	@RequestMapping(
		value = {"/common/einvoiceXml/{_id}"}
		, method = { RequestMethod.POST, RequestMethod.GET }
	)
	public void einvoiceXml(Locale locale, HttpServletRequest req, HttpServletResponse resp, HttpSession session
			, @PathVariable(value = "_id") String _id
			) throws Exception {
		
	
		PrintWriter writer = null;
		FileInfo fileInfo = null;
		
		try {
		String uri = req.getRequestURI();
		boolean isConvert = uri.contains("print-einvoice-convert");
		_token = _id;
		
		if("".equals(_id) || null == _id) {
			resp.setContentType("text/html; charset=utf-8");
	        resp.setCharacterEncoding("UTF-8");
	        resp.setHeader("success", "yes");
	        writer = resp.getWriter();
	        writer.write("Không tìm thấy thông tin hóa đơn.");
	        writer.flush();
	        writer.close();
	        return;
		}
		
		CurrentUserProfile cup = getCurrentlyAuthenticatedPrincipal();
		BaseDTO dto = new BaseDTO();
		dto = new BaseDTO(req);
		Msg msg = dto.createMsg(cup, Constants.MSG_ACTION_CODE.CREATED);
		HashMap<String, Object> hData = new HashMap<>();
		hData.put("_token", _token);
		hData.put("IsConvert", isConvert? "Y": "N");
		
		msg.setObjData(hData);
		JSONRoot root = new JSONRoot(msg);
		
		 fileInfo = restAPI.callAPIGetFileInfo("/commons/einvoiceXml", cup.getLoginRes().getToken(), HttpMethod.POST, root);
		if(null == fileInfo || null == fileInfo.getContentFile()) {
			resp.setContentType("text/html; charset=utf-8");
	        resp.setCharacterEncoding("UTF-8");
	        resp.setHeader("success", "yes");
	        writer = resp.getWriter();
	        writer.write("Xuất XML không thành công.");
	        writer.flush();
	        writer.close();
	        return;
		}
		
		String type = "application/octet-stream";
		InputStream inputStream = new ByteArrayInputStream(fileInfo.getContentFile());
		resp.setHeader("Content-Type", type);
		resp.setHeader("Content-Disposition", "inline; filename=" + fileInfo.getFileName());
		resp.setHeader("Cache-Control", "no-cache, no-store, must-revalidate"); // HTTP 1.1.
		resp.setHeader("Pragma", "no-cache"); // HTTP 1.0.
		resp.setHeader("Expires", "0"); // Proxies.
		int bufferSize = 1024;
		

		resp.setContentType(type);
        final byte[] buffer = new byte[bufferSize];
        int bytesRead = 0;
        OutputStream out = null;
        try {
            out = resp.getOutputStream();
            long totalWritten = 0;
            while ((bytesRead = inputStream.read(buffer)) > 0) {
                out.write(buffer, 0, bytesRead);
                totalWritten += bytesRead;
                if (totalWritten >= buffer.length) {
                    out.flush();
                }
            }
        } finally {
            tryToCloseStream(out);
            tryToCloseStream(inputStream);
        }  
		}catch (Exception e) {
			resp.setContentType("text/html; charset=utf-8");
	        resp.setCharacterEncoding("UTF-8");
	        resp.setHeader("success", "yes");
	        writer = resp.getWriter();
	        writer.write("Không tìm thấy thông tin hóa đơn. Vui lòng thử lại!!!");
	        writer.flush();
	        writer.close();
	        return;
		}
	}


	
	@RequestMapping(value = "/common/auto-complete-products",  produces = MediaType.APPLICATION_JSON_VALUE, method = RequestMethod.POST)
	@ResponseBody
	public List<?> autoCompleteProducts(Locale locale, HttpServletRequest req, HttpSession session
			, @RequestParam(name = "q", defaultValue = "", required = true) String dataInput) throws Exception{
		List<Object> rows = new ArrayList<Object>();
		
		try {
			CurrentUserProfile cup = getCurrentlyAuthenticatedPrincipal();		
			BaseDTO baseDTO = new BaseDTO(req);
			Msg msg = baseDTO.createMsg(cup, Constants.MSG_ACTION_CODE.SEARCH);
			
			HashMap<String, Object> hData = new HashMap<>();
			hData.put("DataInput", dataInput);
			msg.setObjData(hData);
			
			JSONRoot root = new JSONRoot(msg);
			MsgRsp rsp = restAPI.callAPINormal("/commons/auto-complete-products", cup.getLoginRes().getToken(), HttpMethod.POST, root);
			MspResponseStatus rspStatus = rsp.getResponseStatus();
			if(rspStatus.getErrorCode() == 0) {
				JsonNode jsonData = Json.serializer().nodeFromObject(rsp.getObjData());
				HashMap<String, Object> hItem = null;
				for(JsonNode row: jsonData) {
					hItem = new HashMap<String, Object>();
					hItem.put("Code", commons.getTextJsonNode(row.at("/Code")));
					hItem.put("Name", commons.getTextJsonNode(row.at("/Name")));
					if(!row.at("/Price").isMissingNode())
						hItem.put("Price", commons.formatNumberReal(row.at("/Price").doubleValue()));
					if(!row.at("/VatRate").isMissingNode())
						hItem.put("VatRate", commons.formatNumberReal(row.at("/VatRate").doubleValue()));
					hItem.put("Unit", commons.getTextJsonNode(row.at("/Unit")));
					hItem.put("Stock", commons.getTextJsonNode(row.at("/Stock")));
					
					rows.add(hItem);
				}
			}
		}catch(Exception e) {
			rows = new ArrayList<Object>();
		}
		
		return rows;
	}
	
	@RequestMapping(value = "/common/processUploadFile", produces = MediaType.APPLICATION_JSON_VALUE,
			method = RequestMethod.POST, consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	@ResponseBody
	public BaseDTO processUploadFile(HttpServletRequest req, 
			HttpSession session, MultipartHttpServletRequest multipartHttpServletRequest) throws Exception{
		BaseDTO dtoRes = new BaseDTO();
		CurrentUserProfile cup = getCurrentlyAuthenticatedPrincipal();
		
		byte[] data = null;
		String originalFilename = "";
		
		Map<String, MultipartFile> fileMap = multipartHttpServletRequest.getFileMap();
		for (MultipartFile multipartFile : fileMap.values()) {
			originalFilename = multipartFile.getOriginalFilename();
			data = multipartFile.getBytes();
			break;
		}
		
		if(data == null) {
			dtoRes = new BaseDTO(1, "Dữ liệu upload không tồn tại.");
			return dtoRes;
		}
		
		MsgRsp rsp = restAPI.callAPIUploadTmpFile("/commons/processUploadTmpFile", cup.getLoginRes().getToken()
				, HttpMethod.POST, cup.getLoginRes().getIssuerId()
				, originalFilename, data);
		
		MspResponseStatus rspStatus = rsp.getResponseStatus();
		if(rspStatus.getErrorCode() == 0) {
			dtoRes.setErrorCode(0);
			dtoRes.setResponseData(rsp.getObjData());
		}else {
			dtoRes.setErrorCode(rspStatus.getErrorCode());
			dtoRes.setResponseData(rspStatus.getErrorDesc());
		}
		return dtoRes;
	}
	
	private String taxCode;
	private String companyName;
	private String customerName;
	private String mstnv;
	private String tennv;
	private String mauSoHdon;
	private String soHoaDon;
	private String fromDate;
	private String toDate;
	private String nbanMst;
	private String nbanTen;
	
	

	
	
	
	
	
	
	
	@RequestMapping(value = "/common/show-search-nv", method = {RequestMethod.POST})
	public String showSearchNV(Locale locale, HttpServletRequest req, HttpSession session) throws Exception{
		req.setAttribute("_header_", "Danh sách nhân viên");
		return "common/search-nv";
	}
	@RequestMapping(value = "/common/search-nv",  produces = MediaType.APPLICATION_JSON_VALUE, method = RequestMethod.POST)
	@ResponseBody
	public BaseDTO execSearchNV(Locale locale, HttpServletRequest req, HttpSession session) throws Exception{
		JsonGridDTO grid = new JsonGridDTO();
		
		BaseDTO baseDTO = checkDataSearchNV(locale, req, session);
		if(0 != baseDTO.getErrorCode()) {
			grid.setErrorCode(baseDTO.getErrorCode());
			grid.setErrorMessages(baseDTO.getErrorMessages());
			grid.setResponseData(Constants.MAP_ERROR.get(999));
			return grid;
		}
		
		CurrentUserProfile cup = getCurrentlyAuthenticatedPrincipal();		
		baseDTO = new BaseDTO(req);
		Msg msg = baseDTO.createMsg(cup, Constants.MSG_ACTION_CODE.SEARCH);
		
		HashMap<String, Object> hData = new HashMap<>();
		hData.put("TaxCode", mstnv);
		hData.put("Name", tennv);
		
				
		msg.setObjData(hData);
		
		JSONRoot root = new JSONRoot(msg);
		MsgRsp rsp = restAPI.callAPINormal("/qlnvtncn/list", cup.getLoginRes().getToken(), HttpMethod.POST, root);
		MspResponseStatus rspStatus = rsp.getResponseStatus();
		if(rspStatus.getErrorCode() == 0) {
			MsgPage page = rsp.getMsgPage();
			grid.setTotal(page.getTotalRows());
			
			JsonNode jsonData = Json.serializer().nodeFromObject(rsp.getObjData());
			JsonNode rows = null;
			String[] arrTmp = null;
			HashMap<String, String> hItem = null;
			if(!jsonData.at("/rows").isMissingNode()) {
				rows = jsonData.at("/rows");
				for(JsonNode row: rows) {
					hItem = new HashMap<String, String>();
					hItem.put("_id", commons.getTextJsonNode(row.at("/_id")));
					hItem.put("Code", commons.getTextJsonNode(row.at("/Code")));
					hItem.put("Department", commons.getTextJsonNode(row.at("/Department")));
					hItem.put("TaxCode", commons.getTextJsonNode(row.at("/TaxCode")));
					
					hItem.put("CCCD", commons.getTextJsonNode(row.at("/CMND-CCCD/CCCD")));
					hItem.put("CCCDDATE", commons.getTextJsonNode(row.at("/CMND-CCCD/CCCDDATE")));
					hItem.put("CCCDADDRESS", commons.getTextJsonNode(row.at("/CMND-CCCD/CCCDADDRESS")));
					hItem.put("QuocTich", commons.getTextJsonNode(row.at("/CMND-CCCD/QuocTich")));
					hItem.put("CuTru", commons.getTextJsonNode(row.at("/CuTru")));
					
					hItem.put("Name", commons.getTextJsonNode(row.at("/Name")));
					
					hItem.put("Date", 
							commons.convertLocalDateTimeToString(commons.convertLongToLocalDate(row.at("/Date").asLong()), Constants.FORMAT_DATE.FORMAT_DATE_WEB)
						);
					hItem.put("CreateDate", commons.getTextJsonNode(row.at("/CreateDate")));
					hItem.put("Address", commons.getTextJsonNode(row.at("/Address")));
			
					hItem.put("UserCreated", commons.getTextJsonNode(row.at("/InfoCreated/CreateUserFullName")));
					if(!row.at("/InfoCreated/CreateDate").isMissingNode() && null != row.at("/InfoCreated/CreateDate")) {
						try {
							hItem.put("DateCreated", commons.convertLocalDateTimeToString(commons.convertLongToLocalDateTime(row.at("/InfoCreated/CreateDate").asLong(0L)), Constants.FORMAT_DATE.FORMAT_DATE_TIME_WEB));
						}catch(Exception e) {}
					}
					
					grid.getRows().add(hItem);
				}
			}
			
		}else {
			grid = new JsonGridDTO();
			grid.setErrorCode(rspStatus.getErrorCode());
			grid.setResponseData(rspStatus.getErrorDesc());
		}
		return grid;
	}
	private BaseDTO checkDataSearchCustomer(Locale locale, HttpServletRequest req, HttpSession session) {
		BaseDTO dto = new BaseDTO();
		dto.setErrorCode(0);
		
		taxCode = commons.getParameterFromRequest(req, "tax-code").trim().replaceAll("\\s+", " ");
		companyName = commons.getParameterFromRequest(req, "company-name").trim().replaceAll("\\s+", " ");
		customerName = commons.getParameterFromRequest(req, "customer-name").trim().replaceAll("\\s+", " ");
		
		return dto;
	}
	private BaseDTO checkDataSearchNV(Locale locale, HttpServletRequest req, HttpSession session) {
		BaseDTO dto = new BaseDTO();
		dto.setErrorCode(0);
		
		mstnv = commons.getParameterFromRequest(req, "tax-code").trim().replaceAll("\\s+", " ");
		tennv = commons.getParameterFromRequest(req, "qlnvtncn-name").trim().replaceAll("\\s+", " ");
		
		return dto;
	}

	
	@RequestMapping(value = "/common/show-search-customerol", method = {RequestMethod.POST})
    @ResponseBody
    public BaseDTO showSearchCustomerol(Locale locale, HttpServletRequest req, HttpSession session) throws Exception{
      BaseDTO dtoRes = new BaseDTO();
      dtoRes = new BaseDTO(req);
      String mst  = commons.getParameterFromRequest(req, "kh-mst").replaceAll("\\s", "");
       HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://thongtindoanhnghiep.co/api/company/"+mst))
                    .build();
            HttpResponse<String> response = client.send(request,
                    HttpResponse.BodyHandlers.ofString());
           ObjectMapper mapper = new ObjectMapper();
           JsonNode jsonData = mapper.readTree(response.body());
          String title = commons.getTextJsonNode(jsonData.at("/Title"));
          if(!title.equals(""))
          {
          String diaChiCongTy = commons.getTextJsonNode(jsonData.at("/DiaChiCongTy"));
          String chuSoHuu = commons.getTextJsonNode(jsonData.at("/ChuSoHuu"));
        HashMap<String, String> hR = new HashMap<String, String>();
        hR.put("Title", title);
        hR.put("DiaChiCongTy",diaChiCongTy);
        hR.put("ChuSoHuu",chuSoHuu);
        dtoRes.setErrorCode(0);
        dtoRes.setResponseData(hR);
        return dtoRes;
          }
          else {
        	  dtoRes.setErrorCode(9000);
        	  return dtoRes;
          }

    }
	
	

	
	@RequestMapping(value = "/common/show-search-customer", method = {RequestMethod.POST})
	public String showSearchCustomer(Locale locale, HttpServletRequest req, HttpSession session) throws Exception{
		req.setAttribute("_header_", "Danh sách khách hàng");
		return "common/search-customer";
	}
	
	
	@RequestMapping(value = "/common/show-search-customer-update", method = {RequestMethod.POST})
	public String showSearchCustomerUpdate(Locale locale, HttpServletRequest req, HttpSession session) throws Exception{
		req.setAttribute("_header_", "Danh sách khách hàng");
		return "common/search-customer-update";
	}
	
	
	@RequestMapping(value = "/common/search-customer",  produces = MediaType.APPLICATION_JSON_VALUE, method = RequestMethod.POST)
	@ResponseBody
	public BaseDTO execSearchCustomer(Locale locale, HttpServletRequest req, HttpSession session) throws Exception{
		JsonGridDTO grid = new JsonGridDTO();
		
		BaseDTO baseDTO = checkDataSearchCustomer(locale, req, session);
		if(0 != baseDTO.getErrorCode()) {
			grid.setErrorCode(baseDTO.getErrorCode());
			grid.setErrorMessages(baseDTO.getErrorMessages());
			grid.setResponseData(Constants.MAP_ERROR.get(999));
			return grid;
		}
		
		CurrentUserProfile cup = getCurrentlyAuthenticatedPrincipal();		
		baseDTO = new BaseDTO(req);
		Msg msg = baseDTO.createMsg(cup, Constants.MSG_ACTION_CODE.SEARCH);
		
		HashMap<String, Object> hData = new HashMap<>();
		hData.put("TaxCode", taxCode);
		hData.put("CompanyName", companyName);
		hData.put("CustomerName", customerName);
				
		msg.setObjData(hData);
		
		JSONRoot root = new JSONRoot(msg);
		MsgRsp rsp = restAPI.callAPINormal("/commons/list-search-customer", cup.getLoginRes().getToken(), HttpMethod.POST, root);
		MspResponseStatus rspStatus = rsp.getResponseStatus();
		if(rspStatus.getErrorCode() == 0) {
			MsgPage page = rsp.getMsgPage();
			grid.setTotal(page.getTotalRows());
			
			JsonNode jsonData = Json.serializer().nodeFromObject(rsp.getObjData());
			JsonNode rows = null;
			String[] arrTmp = null;
			HashMap<String, String> hItem = null;
			if(!jsonData.at("/rows").isMissingNode()) {
				rows = jsonData.at("/rows");
				for(JsonNode row: rows) {
					hItem = new HashMap<String, String>();
					
					hItem.put("_id", commons.getTextJsonNode(row.at("/_id")));
					hItem.put("TaxCode", commons.getTextJsonNode(row.at("/TaxCode")));
					hItem.put("CustomerCode", commons.getTextJsonNode(row.at("/CustomerCode")));
					hItem.put("CompanyName", commons.getTextJsonNode(row.at("/CompanyName")));
					hItem.put("CustomerName", commons.getTextJsonNode(row.at("/CustomerName")));
					hItem.put("Address", commons.getTextJsonNode(row.at("/Address")));
					
					arrTmp = commons.getTextJsonNode(row.at("/Email")).replaceAll(";", ",").split(",");
					hItem.put("Email",
						arrTmp.length > 0? arrTmp[arrTmp.length - 1]: ""
					);
					
					hItem.put("EmailCC", commons.getTextJsonNode(row.at("/EmailCC")));
					hItem.put("Phone", commons.getTextJsonNode(row.at("/Phone")));
					hItem.put("AccountNumber", commons.getTextJsonNode(row.at("/AccountNumber")));
					hItem.put("AccountBankName", commons.getTextJsonNode(row.at("/AccountBankName")));
					
					hItem.put("ProvinceName", commons.getTextJsonNode(row.at("/Province/Name")));
					hItem.put("CustomerGroup1Name", commons.getTextJsonNode(row.at("/CustomerGroup1/Name")));
					hItem.put("CustomerGroup2Name", commons.getTextJsonNode(row.at("/CustomerGroup2/Name")));
					hItem.put("CustomerGroup3Name", commons.getTextJsonNode(row.at("/CustomerGroup3/Name")));
					
					hItem.put("UserCreated", commons.getTextJsonNode(row.at("/InfoCreated/CreateUserName")));
					if(!row.at("/InfoCreated/CreateDate").isMissingNode() && null != row.at("/InfoCreated/CreateDate")) {
						try {
							hItem.put("DateCreated", commons.convertLocalDateTimeToString(commons.convertLongToLocalDateTime(row.at("/InfoCreated/CreateDate").asLong(0L)), Constants.FORMAT_DATE.FORMAT_DATE_TIME_WEB));
						}catch(Exception e) {}
					}
						
					grid.getRows().add(hItem);
				}
			}
			
		}else {
			grid = new JsonGridDTO();
			grid.setErrorCode(rspStatus.getErrorCode());
			grid.setResponseData(rspStatus.getErrorDesc());
		}
		
		return grid;
	}
	
	


	@RequestMapping(value = "/common/search-customer-update",  produces = MediaType.APPLICATION_JSON_VALUE, method = RequestMethod.POST)
	@ResponseBody
	public BaseDTO execSearchCustomerUpdate(Locale locale, HttpServletRequest req, HttpSession session) throws Exception{
		JsonGridDTO grid = new JsonGridDTO();
		
		BaseDTO baseDTO = checkDataSearchCustomer(locale, req, session);
		if(0 != baseDTO.getErrorCode()) {
			grid.setErrorCode(baseDTO.getErrorCode());
			grid.setErrorMessages(baseDTO.getErrorMessages());
			grid.setResponseData(Constants.MAP_ERROR.get(999));
			return grid;
		}
		
		CurrentUserProfile cup = getCurrentlyAuthenticatedPrincipal();		
		baseDTO = new BaseDTO(req);
		Msg msg = baseDTO.createMsg(cup, Constants.MSG_ACTION_CODE.SEARCH);
		
		HashMap<String, Object> hData = new HashMap<>();
		hData.put("TaxCode", taxCode);
		hData.put("CompanyName", companyName);
		hData.put("CustomerName", customerName);
				
		msg.setObjData(hData);
		
		JSONRoot root = new JSONRoot(msg);
		MsgRsp rsp = restAPI.callAPINormal("/commons/list-search-customer-update", cup.getLoginRes().getToken(), HttpMethod.POST, root);
		MspResponseStatus rspStatus = rsp.getResponseStatus();
		if(rspStatus.getErrorCode() == 0) {
			MsgPage page = rsp.getMsgPage();
			grid.setTotal(page.getTotalRows());
			
			JsonNode jsonData = Json.serializer().nodeFromObject(rsp.getObjData());
			JsonNode rows = null;
			String[] arrTmp = null;
			HashMap<String, String> hItem = null;
			if(!jsonData.at("/rows").isMissingNode()) {
				rows = jsonData.at("/rows");
				for(JsonNode row: rows) {
					hItem = new HashMap<String, String>();
					
					hItem.put("_id", commons.getTextJsonNode(row.at("/_id")));
					hItem.put("TaxCode", commons.getTextJsonNode(row.at("/TaxCode")));
					hItem.put("CustomerCode", commons.getTextJsonNode(row.at("/CustomerCode")));
					hItem.put("CompanyName", commons.getTextJsonNode(row.at("/CompanyName")));
					hItem.put("CustomerName", commons.getTextJsonNode(row.at("/CustomerName")));
					hItem.put("Address", commons.getTextJsonNode(row.at("/Address")));
					
					arrTmp = commons.getTextJsonNode(row.at("/Email")).replaceAll(";", ",").split(",");
					hItem.put("Email",
						arrTmp.length > 0? arrTmp[arrTmp.length - 1]: ""
					);
					
					hItem.put("EmailCC", commons.getTextJsonNode(row.at("/EmailCC")));
					hItem.put("Phone", commons.getTextJsonNode(row.at("/Phone")));
					hItem.put("AccountNumber", commons.getTextJsonNode(row.at("/AccountNumber")));
					hItem.put("AccountBankName", commons.getTextJsonNode(row.at("/AccountBankName")));
					
					hItem.put("ProvinceName", commons.getTextJsonNode(row.at("/Province/Name")));
					hItem.put("CustomerGroup1Name", commons.getTextJsonNode(row.at("/CustomerGroup1/Name")));
					hItem.put("CustomerGroup2Name", commons.getTextJsonNode(row.at("/CustomerGroup2/Name")));
					hItem.put("CustomerGroup3Name", commons.getTextJsonNode(row.at("/CustomerGroup3/Name")));
					
					hItem.put("UserCreated", commons.getTextJsonNode(row.at("/InfoCreated/CreateUserName")));
					if(!row.at("/InfoCreated/CreateDate").isMissingNode() && null != row.at("/InfoCreated/CreateDate")) {
						try {
							hItem.put("DateCreated", commons.convertLocalDateTimeToString(commons.convertLongToLocalDateTime(row.at("/InfoCreated/CreateDate").asLong(0L)), Constants.FORMAT_DATE.FORMAT_DATE_TIME_WEB));
						}catch(Exception e) {}
					}
						
					grid.getRows().add(hItem);
				}
			}
			
		}else {
			grid = new JsonGridDTO();
			grid.setErrorCode(rspStatus.getErrorCode());
			grid.setResponseData(rspStatus.getErrorDesc());
		}
		
		return grid;
	}
	
	


	private void LoadParameterFor_DSHDDKy(CurrentUserProfile cup, Locale locale, HttpServletRequest req, String action) {
		try {
			BaseDTO baseDTO = new BaseDTO(req);
			Msg msg = baseDTO.createMsg(cup, Constants.MSG_ACTION_CODE.LOAD_PARAMS);
			
			/*DANH SACH THAM SO*/
			HashMap<String, String> hashConds = null;
			ArrayList<HashMap<String, String>> conds = null;
			MsgParam msgParam = null;
			MsgParams msgParams = new MsgParams();
			
			msgParam = new MsgParam();
			msgParam.setId("param01");
			msgParam.setParam("DMMauSoKyHieu");
			msgParams.getParams().add(msgParam);
			
			/*END: DANH SACH THAM SO*/
			msg.setObjData(msgParams);
			
			JSONRoot root = new JSONRoot(msg);
			MsgRsp rsp = restAPI.callAPINormal("/commons/get-full-params", cup.getLoginRes().getToken(), HttpMethod.POST, root);
			MspResponseStatus rspStatus = rsp.getResponseStatus();
			
			if(rspStatus.getErrorCode() == 0 && rsp.getObjData() != null) {
				LinkedHashMap<String, String> hItem = null;
				
				JsonNode jsonData = Json.serializer().nodeFromObject(rsp.getObjData());
			
				if(null != jsonData.at("/param01") && jsonData.at("/param01") instanceof ArrayNode) {
					hItem = new LinkedHashMap<String, String>();
					for(JsonNode o: jsonData.at("/param01")) {
						hItem.put(commons.getTextJsonNode(o.get("_id")), commons.getTextJsonNode(o.get("KHMSHDon")) + commons.getTextJsonNode(o.get("KHHDon")));
					}
					req.setAttribute("map_mausokyhieu", hItem);
				}
				
			}
			
		}catch(Exception e) {}
	}
	
	@RequestMapping(value = {
			"/common/show-search-dshddky"
	}, method = {RequestMethod.POST})
	public String showSearchDSHDDKy(Locale locale, HttpServletRequest req, HttpSession session) throws Exception{
		req.setAttribute("_header_", "Danh sách hóa đơn đã ký");
		
		LocalDate now = LocalDate.now();
		req.setAttribute("FromDate", commons.convertLocalDateTimeToString(now.with(ChronoField.DAY_OF_MONTH, 1), Constants.FORMAT_DATE.FORMAT_DATE_WEB));
		req.setAttribute("ToDate", commons.convertLocalDateTimeToString(now, Constants.FORMAT_DATE.FORMAT_DATE_WEB));
		
		CurrentUserProfile cup = getCurrentlyAuthenticatedPrincipal();

		LoadParameterFor_DSHDDKy(cup, locale, req, "DETAIL");
		return "common/search-dshddky";
	}
	
	private BaseDTO checkDataSearchEInvoiceSigned(Locale locale, HttpServletRequest req, HttpSession session) {
		BaseDTO dto = new BaseDTO();
		dto.setErrorCode(0);
		
		mauSoHdon = commons.getParameterFromRequest(req, "mau-so-hdon").replaceAll("\\s", "");
		soHoaDon = commons.getParameterFromRequest(req, "so-hoa-don").replaceAll("\\s", "");
		fromDate = commons.getParameterFromRequest(req, "from-date").replaceAll("\\s", "");
		toDate = commons.getParameterFromRequest(req, "to-date").replaceAll("\\s", "");
		nbanMst = commons.getParameterFromRequest(req, "nban-mst").replaceAll("\\s", "");
		nbanTen = commons.getParameterFromRequest(req, "nban-ten").replaceAll("\\s", "");
		
		if(!"".equals(fromDate) && !commons.checkLocalDate(fromDate, Constants.FORMAT_DATE.FORMAT_DATE_WEB)) {
			dto.setErrorCode(1);
			dto.getErrorMessages().add("Từ ngày không đúng định dạng.");
		}
		if(!"".equals(toDate) && !commons.checkLocalDate(toDate, Constants.FORMAT_DATE.FORMAT_DATE_WEB)) {
			dto.setErrorCode(1);
			dto.getErrorMessages().add("Từ ngày không đúng định dạng.");
		}
		
		return dto;
	}
	
	@RequestMapping(value = "/common/list-einvoice-signed",  produces = MediaType.APPLICATION_JSON_VALUE, method = RequestMethod.POST)
	@ResponseBody
	public BaseDTO listEInvoiceSigned(Locale locale, HttpServletRequest req, HttpSession session) throws Exception{
		JsonGridDTO grid = new JsonGridDTO();
		
		try {
		BaseDTO baseDTO = checkDataSearchEInvoiceSigned(locale, req, session);
		if(0 != baseDTO.getErrorCode()) {
			grid.setErrorCode(baseDTO.getErrorCode());
			grid.setErrorMessages(baseDTO.getErrorMessages());
			grid.setResponseData(Constants.MAP_ERROR.get(999));
			return grid;
		}
		
		CurrentUserProfile cup = getCurrentlyAuthenticatedPrincipal();		
		baseDTO = new BaseDTO(req);
		Msg msg = baseDTO.createMsg(cup, Constants.MSG_ACTION_CODE.SEARCH);
		
		HashMap<String, Object> hData = new HashMap<>();
		hData.put("MauSoHdon", mauSoHdon);
		hData.put("SoHoaDon", soHoaDon);
		hData.put("FromDate", fromDate);
		hData.put("ToDate", toDate);
		hData.put("NbanMst", nbanMst);
		hData.put("NbanTen", nbanTen);
				
		msg.setObjData(hData);
		
		JSONRoot root = new JSONRoot(msg);
		MsgRsp rsp = restAPI.callAPINormal("/commons/list-einvoice-signed", cup.getLoginRes().getToken(), HttpMethod.POST, root);
		MspResponseStatus rspStatus = rsp.getResponseStatus();
		if(rspStatus.getErrorCode() == 0) {
			MsgPage page = rsp.getMsgPage();
			grid.setTotal(page.getTotalRows());
			
			JsonNode jsonData = Json.serializer().nodeFromObject(rsp.getObjData());
			JsonNode rows = null;
			HashMap<String, String> hItem = null;
			if(!jsonData.at("/rows").isMissingNode()) {
				rows = jsonData.at("/rows");
				for(JsonNode row: rows) {
					hItem = new HashMap<String, String>();
					
					hItem.put("_id", commons.getTextJsonNode(row.at("/_id")));
					
					hItem.put("SignStatusCode", commons.getTextJsonNode(row.at("/SignStatusCode")));
					hItem.put("SignStatusDesc", Constants.MAP_EINVOICE_SIGN_STATUS.get(commons.getTextJsonNode(row.at("/SignStatusCode"))));
					
					hItem.put("EInvoiceStatus", commons.getTextJsonNode(row.at("/EInvoiceStatus")));
					hItem.put("MCCQT", commons.getTextJsonNode(row.at("/MCCQT")));
					hItem.put("StatusDesc", Constants.MAP_EINVOICE_STATUS.get(commons.getTextJsonNode(row.at("/EInvoiceStatus"))));
					hItem.put("CQTMTLoi", commons.getTextJsonNode(row.at("/LDo/MTLoi")));
					
					hItem.put("MauSoHD", 
						commons.getTextJsonNode(row.at("/EInvoiceDetail/TTChung/KHMSHDon")) + commons.getTextJsonNode(row.at("/EInvoiceDetail/TTChung/KHHDon"))
					);
					hItem.put("EInvoiceNumber", 
						"".equals(commons.getTextJsonNode(row.at("/EInvoiceDetail/TTChung/SHDon")))? "":
						commons.formatNumberBillInvoice(commons.getTextJsonNode(row.at("/EInvoiceDetail/TTChung/SHDon")))
					);
					hItem.put("NLap", 
						commons.convertLocalDateTimeToString(commons.convertLongToLocalDate(row.at("/EInvoiceDetail/TTChung/NLap").asLong()), Constants.FORMAT_DATE.FORMAT_DATE_WEB)
					);
					hItem.put("TaxCode", commons.getTextJsonNode(row.at("/EInvoiceDetail/NDHDon/NMua/MST")));
					hItem.put("CompanyName", commons.getTextJsonNode(row.at("/EInvoiceDetail/NDHDon/NMua/Ten")));
					hItem.put("TgTTTBSo", 
						row.at("/EInvoiceDetail/TToan/TgTTTBSo").isMissingNode()? "":
						commons.formatNumberReal(row.at("/EInvoiceDetail/TToan/TgTTTBSo").doubleValue())
					);
					hItem.put("TgTCThue", 
						row.at("/EInvoiceDetail/TToan/TgTCThue").isMissingNode()? "":
						commons.formatNumberReal(row.at("/EInvoiceDetail/TToan/TgTCThue").doubleValue())
					);
					hItem.put("TgTThue", 
						row.at("/EInvoiceDetail/TToan/TgTThue").isMissingNode()? "":
						commons.formatNumberReal(row.at("/EInvoiceDetail/TToan/TgTThue").doubleValue())
					);
					hItem.put("HVTNMHang", commons.getTextJsonNode(row.at("/EInvoiceDetail/NDHDon/NMua/HVTNMHang")));
					hItem.put("UserCreated", commons.getTextJsonNode(row.at("/InfoCreated/CreateUserFullName")));
					
					grid.getRows().add(hItem);
				}
			}
		}else {
			grid = new JsonGridDTO();
			grid.setErrorCode(rspStatus.getErrorCode());
			grid.setResponseData(rspStatus.getErrorDesc());
		}
		
		}catch (Exception e) {
			grid = new JsonGridDTO();
			grid.setErrorCode(999);
			grid.setResponseData("Không tìm thấy thông tin. Vui lòng thử lại!!!");
		}
		return grid;
	}
	
	private static final int BUFFER_SIZE = 1028;
	@RequestMapping(value = "/common/download-plugin", produces = MediaType.APPLICATION_JSON_VALUE, method = RequestMethod.GET)
	public void execDownloadPlugin(HttpServletRequest req, HttpServletResponse resp) throws Exception {
		PrintWriter writer = null;
		
		File file = new File(SystemParams.DIR_TEMPLATE_FILES, Constants.FILE_DOWNLOAD_TEMPLATE.SIGN_PLUGIN);
		if(!file.exists() || !file.isFile()) {
			resp.setContentType("text/html; charset=utf-8");
			resp.setCharacterEncoding("UTF-8");
			resp.setHeader("success", "yes");
			writer = resp.getWriter();
			writer.write("Tập tin không tồn tại.");
			writer.close();
			return;
		}
		
		InputStream is = new FileInputStream(file);
	    resp.setHeader("Content-Disposition", "attachment; filename=" + Constants.FILE_DOWNLOAD_TEMPLATE.SIGN_PLUGIN + "");
	    int read=0;
	    byte[] bytes = new byte[BUFFER_SIZE];
	    OutputStream os = resp.getOutputStream();

	    while((read = is.read(bytes))!= -1){
	        os.write(bytes, 0, read);
	    }
	    os.flush();
	    os.close(); 
		is.close();
	}
	
	
	//Print PXK
	@RequestMapping(
			value = {"/common/print-export/{_id}", "/common/print-export-convert/{_id}"}
			, method = { RequestMethod.POST, RequestMethod.GET }
		)
		public void printExport(Locale locale, HttpServletRequest req, HttpServletResponse resp, HttpSession session
				, @PathVariable(value = "_id") String _id
			) throws Exception {
			PrintWriter writer = null;
			
			try {
			String uri = req.getRequestURI();
			boolean isConvert = uri.contains("print-export-convert");
			
			_id = _id.replaceAll("\\s", "");
			if("".equals(_id) || null == _id) {
				resp.setContentType("text/html; charset=utf-8");
		        resp.setCharacterEncoding("UTF-8");
		        resp.setHeader("success", "yes");
		        writer = resp.getWriter();
		        writer.write("Không tìm thấy thông tin hóa đơn.");
		        writer.flush();
		        writer.close();
		        return;
			}
			
			CurrentUserProfile cup = getCurrentlyAuthenticatedPrincipal();
			BaseDTO dto = new BaseDTO();
			
			dto = new BaseDTO(req);
			Msg msg = dto.createMsg(cup, Constants.MSG_ACTION_CODE.CREATED);
			HashMap<String, Object> hData = new HashMap<>();
			hData.put("_id", _id);
			hData.put("IsConvert", isConvert? "Y": "N");
			
			msg.setObjData(hData);
			JSONRoot root = new JSONRoot(msg);
			
			FileInfo fileInfo = restAPI.callAPIGetFileInfo("/commons/print-export", cup.getLoginRes().getToken(), HttpMethod.POST, root);
			if(null == fileInfo || null == fileInfo.getContentFile()) {
				resp.setContentType("text/html; charset=utf-8");
		        resp.setCharacterEncoding("UTF-8");
		        resp.setHeader("success", "yes");
		        writer = resp.getWriter();
		        writer.write("In hóa đơn không thành công.");
		        writer.flush();
		        writer.close();
		        return;
			}
			
			String type = "application/pdf";
			InputStream inputStream = new ByteArrayInputStream(fileInfo.getContentFile());
			resp.setHeader("Content-Type", type);
			
			resp.setHeader("Cache-Control", "no-cache, no-store, must-revalidate"); // HTTP 1.1.
			resp.setHeader("Pragma", "no-cache"); // HTTP 1.0.
			resp.setHeader("Expires", "0"); // Proxies.
			int bufferSize = 1024;
			resp.setContentType(type);
	        final byte[] buffer = new byte[bufferSize];
	        int bytesRead = 0;
	        OutputStream out = null;
	        try {
	            out = resp.getOutputStream();
	            long totalWritten = 0;
	            while ((bytesRead = inputStream.read(buffer)) > 0) {
	                out.write(buffer, 0, bytesRead);
	                totalWritten += bytesRead;
	                if (totalWritten >= buffer.length) {
	                    out.flush();
	                }
	            }
	        } finally {
	            tryToCloseStream(out);
	            tryToCloseStream(inputStream);
	        }
			}catch (Exception e) {
				resp.setContentType("text/html; charset=utf-8");
		        resp.setCharacterEncoding("UTF-8");
		        resp.setHeader("success", "yes");
		        writer = resp.getWriter();
		        writer.write("Không tìm thấy thông tin hóa đơn. Vui lòng thử lại!!!");
		        writer.flush();
		        writer.close();
		        return;
			}
		}
	
	
	
	
	//Print PXK DL
		@RequestMapping(
				value = {"/common/print-agent/{_id}", "/common/print-agent-convert/{_id}"}
				, method = { RequestMethod.POST, RequestMethod.GET }
			)
			public void printAgent(Locale locale, HttpServletRequest req, HttpServletResponse resp, HttpSession session
					, @PathVariable(value = "_id") String _id
				) throws Exception {
				PrintWriter writer = null;
				try {
				String uri = req.getRequestURI();
				boolean isConvert = uri.contains("print-agent-convert");
				
				_id = _id.replaceAll("\\s", "");
				if("".equals(_id) || null == _id) {
					resp.setContentType("text/html; charset=utf-8");
			        resp.setCharacterEncoding("UTF-8");
			        resp.setHeader("success", "yes");
			        writer = resp.getWriter();
			        writer.write("Không tìm thấy thông tin hóa đơn.");
			        writer.flush();
			        writer.close();
			        return;
				}
				
				CurrentUserProfile cup = getCurrentlyAuthenticatedPrincipal();
				BaseDTO dto = new BaseDTO();
				
				dto = new BaseDTO(req);
				Msg msg = dto.createMsg(cup, Constants.MSG_ACTION_CODE.CREATED);
				HashMap<String, Object> hData = new HashMap<>();
				hData.put("_id", _id);
				hData.put("IsConvert", isConvert? "Y": "N");
				
				msg.setObjData(hData);
				JSONRoot root = new JSONRoot(msg);
				
				FileInfo fileInfo = restAPI.callAPIGetFileInfo("/commons/print-agent", cup.getLoginRes().getToken(), HttpMethod.POST, root);
				if(null == fileInfo || null == fileInfo.getContentFile()) {
					resp.setContentType("text/html; charset=utf-8");
			        resp.setCharacterEncoding("UTF-8");
			        resp.setHeader("success", "yes");
			        writer = resp.getWriter();
			        writer.write("In hóa đơn không thành công.");
			        writer.flush();
			        writer.close();
			        return;
				}
				
				String type = "application/pdf";
				InputStream inputStream = new ByteArrayInputStream(fileInfo.getContentFile());
				resp.setHeader("Content-Type", type);
				
				resp.setHeader("Cache-Control", "no-cache, no-store, must-revalidate"); // HTTP 1.1.
				resp.setHeader("Pragma", "no-cache"); // HTTP 1.0.
				resp.setHeader("Expires", "0"); // Proxies.
				int bufferSize = 1024;
				resp.setContentType(type);
		        final byte[] buffer = new byte[bufferSize];
		        int bytesRead = 0;
		        OutputStream out = null;
		        try {
		            out = resp.getOutputStream();
		            long totalWritten = 0;
		            while ((bytesRead = inputStream.read(buffer)) > 0) {
		                out.write(buffer, 0, bytesRead);
		                totalWritten += bytesRead;
		                if (totalWritten >= buffer.length) {
		                    out.flush();
		                }
		            }
		        } finally {
		            tryToCloseStream(out);
		            tryToCloseStream(inputStream);
		        }
				}catch (Exception e) {
					resp.setContentType("text/html; charset=utf-8");
			        resp.setCharacterEncoding("UTF-8");
			        resp.setHeader("success", "yes");
			        writer = resp.getWriter();
			        writer.write("Không tìm thấy thông tin hóa đơn. Vui lòng thử lại!!!");
			        writer.flush();
			        writer.close();
			        return;
				}
			}
	
		@RequestMapping(
				value = {"/common/print-einvoice1/{_id}", "/common/print-einvoice1-convert/{_id}"}
				, method = { RequestMethod.POST, RequestMethod.GET }
			)
			public void printEInvoiceBH(Locale locale, HttpServletRequest req, HttpServletResponse resp, HttpSession session
					, @PathVariable(value = "_id") String _id
				) throws Exception {
				PrintWriter writer = null;
				try {
				String uri = req.getRequestURI();
				boolean isConvert = uri.contains("print-einvoice1-convert");
				
				_id = _id.replaceAll("\\s", "");
				if("".equals(_id) || null == _id) {
					resp.setContentType("text/html; charset=utf-8");
			        resp.setCharacterEncoding("UTF-8");
			        resp.setHeader("success", "yes");
			        writer = resp.getWriter();
			        writer.write("Không tìm thấy thông tin hóa đơn.");
			        writer.flush();
			        writer.close();
			        return;
				}
				
				CurrentUserProfile cup = getCurrentlyAuthenticatedPrincipal();
				BaseDTO dto = new BaseDTO();
				
				dto = new BaseDTO(req);
				Msg msg = dto.createMsg(cup, Constants.MSG_ACTION_CODE.CREATED);
				HashMap<String, Object> hData = new HashMap<>();
				hData.put("_id", _id);
				hData.put("IsConvert", isConvert? "Y": "N");
				
				msg.setObjData(hData);
				JSONRoot root = new JSONRoot(msg);
				
				FileInfo fileInfo = restAPI.callAPIGetFileInfo("/commons/print-einvoice1", cup.getLoginRes().getToken(), HttpMethod.POST, root);
				if(null == fileInfo || null == fileInfo.getContentFile()) {
					resp.setContentType("text/html; charset=utf-8");
			        resp.setCharacterEncoding("UTF-8");
			        resp.setHeader("success", "yes");
			        writer = resp.getWriter();
			        writer.write("In hóa đơn không thành công.");
			        writer.flush();
			        writer.close();
			        return;
				}
				
				String type = "application/pdf";
				InputStream inputStream = new ByteArrayInputStream(fileInfo.getContentFile());
				resp.setHeader("Content-Type", type);
				
				resp.setHeader("Cache-Control", "no-cache, no-store, must-revalidate"); // HTTP 1.1.
				resp.setHeader("Pragma", "no-cache"); // HTTP 1.0.
				resp.setHeader("Expires", "0"); // Proxies.
				
				int bufferSize = 1024;
//				String mimetype = "application/pdf";
				resp.setContentType(type);
		        final byte[] buffer = new byte[bufferSize];
		        int bytesRead = 0;
		        OutputStream out = null;
		        try {
		            out = resp.getOutputStream();
		            long totalWritten = 0;
		            while ((bytesRead = inputStream.read(buffer)) > 0) {
		                out.write(buffer, 0, bytesRead);
		                totalWritten += bytesRead;
		                if (totalWritten >= buffer.length) {
		                    out.flush();
		                }
		            }
		        } finally {
		            tryToCloseStream(out);
		            tryToCloseStream(inputStream);
		        }
				}catch (Exception e) {
					resp.setContentType("text/html; charset=utf-8");
			        resp.setCharacterEncoding("UTF-8");
			        resp.setHeader("success", "yes");
			        writer = resp.getWriter();
			        writer.write("Không tìm thấy thông tin hóa đơn. Vui lòng thử lại!!!");
			        writer.flush();
			        writer.close();
			        return;
				}
}
		
		
		@RequestMapping(
				value = {"common/getXml/{token}"}
				, method = { RequestMethod.POST, RequestMethod.GET }
			)
			public void getXml(Locale locale, HttpServletRequest req, HttpServletResponse resp, HttpSession session
					, @PathVariable(value = "token") String token
					) throws Exception {
		
				PrintWriter writer = null;
				FileInfo fileInfo = null;
				try {
				String uri = req.getRequestURI();
				boolean isConvert = uri.contains("print-einvoice-convert");
				
				if("".equals(token) || null == token) {
					resp.setContentType("text/html; charset=utf-8");
			        resp.setCharacterEncoding("UTF-8");
			        resp.setHeader("success", "yes");
			        writer = resp.getWriter();
			        writer.write("Không tìm thấy thông tin hóa đơn.");
			        writer.flush();
			        writer.close();
			        return;
				}
				
				CurrentUserProfile cup = getCurrentlyAuthenticatedPrincipal();
				BaseDTO dto = new BaseDTO();
				dto = new BaseDTO(req);
				Msg msg = dto.createMsg(cup, Constants.MSG_ACTION_CODE.CREATED);
				HashMap<String, Object> hData = new HashMap<>();
				hData.put("_token", token);
				hData.put("IsConvert", isConvert? "Y": "N");
				
				msg.setObjData(hData);
				JSONRoot root = new JSONRoot(msg);
				
				 fileInfo = restAPI.callAPIGetFileInfo("/commons/getXml", cup.getLoginRes().getToken(), HttpMethod.POST, root);
				if(null == fileInfo || null == fileInfo.getContentFile()) {
					resp.setContentType("text/html; charset=utf-8");
			        resp.setCharacterEncoding("UTF-8");
			        resp.setHeader("success", "yes");
			        writer = resp.getWriter();
			        writer.write("Xuất XML không thành công.");
			        writer.flush();
			        writer.close();
			        return;
				}
				
				String type = "application/octet-stream";
				InputStream inputStream = new ByteArrayInputStream(fileInfo.getContentFile());
				resp.setHeader("Content-Type", type);
				resp.setHeader("Content-Disposition", "inline; filename=" + fileInfo.getFileName());
				resp.setHeader("Cache-Control", "no-cache, no-store, must-revalidate"); // HTTP 1.1.
				resp.setHeader("Pragma", "no-cache"); // HTTP 1.0.
				resp.setHeader("Expires", "0"); // Proxies.
				int bufferSize = 1024;
				

				resp.setContentType(type);
		        final byte[] buffer = new byte[bufferSize];
		        int bytesRead = 0;
		        OutputStream out = null;
		        try {
		            out = resp.getOutputStream();
		            long totalWritten = 0;
		            while ((bytesRead = inputStream.read(buffer)) > 0) {
		                out.write(buffer, 0, bytesRead);
		                totalWritten += bytesRead;
		                if (totalWritten >= buffer.length) {
		                    out.flush();
		                }
		            }
		        } finally {
		            tryToCloseStream(out);
		            tryToCloseStream(inputStream);
		        }  
				}catch (Exception e) {
					resp.setContentType("text/html; charset=utf-8");
			        resp.setCharacterEncoding("UTF-8");
			        resp.setHeader("success", "yes");
			        writer = resp.getWriter();
			        writer.write("Không tìm thấy thông tin hóa đơn. Vui lòng thử lại!!!");
			        writer.flush();
			        writer.close();
			        return;
				}
			}
	
		
		
		@RequestMapping(
				value = {"/common/einvoice1Xml/{_id}"}
				, method = { RequestMethod.POST, RequestMethod.GET }
			)
			public void einvoice1Xml(Locale locale, HttpServletRequest req, HttpServletResponse resp, HttpSession session
					, @PathVariable(value = "_id") String _id
					) throws Exception {
				
			
				PrintWriter writer = null;
				FileInfo fileInfo = null;
				try {
				String uri = req.getRequestURI();
				boolean isConvert = uri.contains("print-einvoice-convert");
				_token = _id;
				
				if("".equals(_id) || null == _id) {
					resp.setContentType("text/html; charset=utf-8");
			        resp.setCharacterEncoding("UTF-8");
			        resp.setHeader("success", "yes");
			        writer = resp.getWriter();
			        writer.write("Không tìm thấy thông tin hóa đơn.");
			        writer.flush();
			        writer.close();
			        return;
				}
				
				CurrentUserProfile cup = getCurrentlyAuthenticatedPrincipal();
				BaseDTO dto = new BaseDTO();
				dto = new BaseDTO(req);
				Msg msg = dto.createMsg(cup, Constants.MSG_ACTION_CODE.CREATED);
				HashMap<String, Object> hData = new HashMap<>();
				hData.put("_token", _token);
				hData.put("IsConvert", isConvert? "Y": "N");
				
				msg.setObjData(hData);
				JSONRoot root = new JSONRoot(msg);
				
				 fileInfo = restAPI.callAPIGetFileInfo("/commons/einvoice1Xml", cup.getLoginRes().getToken(), HttpMethod.POST, root);
				if(null == fileInfo || null == fileInfo.getContentFile()) {
					resp.setContentType("text/html; charset=utf-8");
			        resp.setCharacterEncoding("UTF-8");
			        resp.setHeader("success", "yes");
			        writer = resp.getWriter();
			        writer.write("Xuất XML không thành công.");
			        writer.flush();
			        writer.close();
			        return;
				}
				
				String type = "application/octet-stream";
				InputStream inputStream = new ByteArrayInputStream(fileInfo.getContentFile());
				resp.setHeader("Content-Type", type);
				resp.setHeader("Content-Disposition", "inline; filename=" + fileInfo.getFileName());
				resp.setHeader("Cache-Control", "no-cache, no-store, must-revalidate"); // HTTP 1.1.
				resp.setHeader("Pragma", "no-cache"); // HTTP 1.0.
				resp.setHeader("Expires", "0"); // Proxies.
				int bufferSize = 1024;
				

				resp.setContentType(type);
		        final byte[] buffer = new byte[bufferSize];
		        int bytesRead = 0;
		        OutputStream out = null;
		        try {
		            out = resp.getOutputStream();
		            long totalWritten = 0;
		            while ((bytesRead = inputStream.read(buffer)) > 0) {
		                out.write(buffer, 0, bytesRead);
		                totalWritten += bytesRead;
		                if (totalWritten >= buffer.length) {
		                    out.flush();
		                }
		            }
		        } finally {
		            tryToCloseStream(out);
		            tryToCloseStream(inputStream);
		        } 
		        
				}catch (Exception e) {
					resp.setContentType("text/html; charset=utf-8");
			        resp.setCharacterEncoding("UTF-8");
			        resp.setHeader("success", "yes");
			        writer = resp.getWriter();
			        writer.write("Không tìm thấy thông tin hóa đơn. Vui lòng thử lại!!!");
			        writer.flush();
			        writer.close();
			        return;
				}
			}
	
		
		@RequestMapping(
				value = {"/common/print-einvoice1All/{_id}","/common/print-einvoice1CD/{_id}"}
				, method = { RequestMethod.POST, RequestMethod.GET }
			)
			public void printEInvoice1All(Locale locale, HttpServletRequest req, HttpServletResponse resp, HttpSession session
					, @PathVariable(value = "_id") String _id
					) throws Exception {
				
			
				PrintWriter writer = null;
				FileInfo fileInfo = null;
				try {
				String uri = req.getRequestURI();
				boolean isConvert = uri.contains("print-einvoice1CD");
//				_token = _id;
//				ids = null;
//				try {
//					ids = Json.serializer().fromJson(commons.decodeBase64ToString(_token), new TypeReference<List<String>>() {
//					});
//				}catch(Exception e) {}
			
				_id = _id.replaceAll("\\s", "");
				if("".equals(_id) || null == _id) {
					resp.setContentType("text/html; charset=utf-8");
			        resp.setCharacterEncoding("UTF-8");
			        resp.setHeader("success", "yes");
			        writer = resp.getWriter();
			        writer.write("Không tìm thấy thông tin hóa đơn.");
			        writer.flush();
			        writer.close();
			        return;
				}
				
				CurrentUserProfile cup = getCurrentlyAuthenticatedPrincipal();
				BaseDTO dto = new BaseDTO();
				dto = new BaseDTO(req);
				Msg msg = dto.createMsg(cup, Constants.MSG_ACTION_CODE.CREATED);
				HashMap<String, Object> hData = new HashMap<>();
				hData.put("_token", _id);
				hData.put("IsConvert", isConvert? "Y": "N");
				
				msg.setObjData(hData);
				JSONRoot root = new JSONRoot(msg);
				
				 fileInfo = restAPI.callAPIGetFileInfo("/commons/print-einvoice1All", cup.getLoginRes().getToken(), HttpMethod.POST, root);
				if(null == fileInfo || null == fileInfo.getContentFile()) {
					resp.setContentType("text/html; charset=utf-8");
			        resp.setCharacterEncoding("UTF-8");
			        resp.setHeader("success", "yes");
			        writer = resp.getWriter();
			        writer.write("In hóa đơn không thành công.");
			        writer.flush();
			        writer.close();
			        return;
				}
				
//				String type = "application/pdf";
				String type = "application/octet-stream";
				InputStream inputStream = new ByteArrayInputStream(fileInfo.getContentFile());
				resp.setHeader("Content-Type", type);
				resp.setHeader("Content-Disposition", "inline; filename=" + fileInfo.getFileName());
				resp.setHeader("Cache-Control", "no-cache, no-store, must-revalidate"); // HTTP 1.1.
				resp.setHeader("Pragma", "no-cache"); // HTTP 1.0.
				resp.setHeader("Expires", "0"); // Proxies.
				int bufferSize = 1024;
				resp.setContentType(type);
		        final byte[] buffer = new byte[bufferSize];
		        int bytesRead = 0;
		        OutputStream out = null;
		        try {
		            out = resp.getOutputStream();
		            long totalWritten = 0;
		            while ((bytesRead = inputStream.read(buffer)) > 0) {
		                out.write(buffer, 0, bytesRead);
		                totalWritten += bytesRead;
		                if (totalWritten >= buffer.length) {
		                    out.flush();
		                }
		            }
		        } finally {
		            tryToCloseStream(out);
		            tryToCloseStream(inputStream);
		        }   
				}catch (Exception e) {
					resp.setContentType("text/html; charset=utf-8");
			        resp.setCharacterEncoding("UTF-8");
			        resp.setHeader("success", "yes");
			        writer = resp.getWriter();
			        writer.write("Không tìm thấy thông tin hóa đơn. Vui lòng thử lại!!!");
			        writer.flush();
			        writer.close();
			        return;
				}
			}
		
		
		
		
		///
		
		@RequestMapping(
				value = {"/common/downLoadFile/{_id}"}
				, method = { RequestMethod.POST, RequestMethod.GET }
			)
			public void downLoadFile(Locale locale, HttpServletRequest req, HttpServletResponse resp, HttpSession session
					, @PathVariable(value = "_id") String _id
					) throws Exception {
			
				PrintWriter writer = null;
				FileInfo fileInfo = null;
				try {
				String uri = req.getRequestURI();
				boolean isConvert = uri.contains("print-einvoice-convert");
				_token = _id;
				
				if("".equals(_id) || null == _id) {
					resp.setContentType("text/html; charset=utf-8");
			        resp.setCharacterEncoding("UTF-8");
			        resp.setHeader("success", "yes");
			        writer = resp.getWriter();
			        writer.write("Không tìm thấy thông tin hóa đơn.");
			        writer.flush();
			        writer.close();
			        return;
				}
				
				CurrentUserProfile cup = getCurrentlyAuthenticatedPrincipal();
				BaseDTO dto = new BaseDTO();
				dto = new BaseDTO(req);
				Msg msg = dto.createMsg(cup, Constants.MSG_ACTION_CODE.CREATED);
				HashMap<String, Object> hData = new HashMap<>();
				hData.put("_token", _token);
				hData.put("IsConvert", isConvert? "Y": "N");
				
				msg.setObjData(hData);
				JSONRoot root = new JSONRoot(msg);
				
				 fileInfo = restAPI.callAPIGetFileInfo("/commons/downLoadFile", cup.getLoginRes().getToken(), HttpMethod.POST, root);
				if(null == fileInfo || null == fileInfo.getContentFile()) {
					resp.setContentType("text/html; charset=utf-8");
			        resp.setCharacterEncoding("UTF-8");
			        resp.setHeader("success", "yes");
			        writer = resp.getWriter();
			        writer.write("Xuất file không thành công.");
			        writer.flush();
			        writer.close();
			        return;
				}
				
				String type = "application/octet-stream";
				InputStream inputStream = new ByteArrayInputStream(fileInfo.getContentFile());
				resp.setHeader("Content-Type", type);
				resp.setHeader("Content-Disposition", "inline; filename=" + fileInfo.getFileName());
				resp.setHeader("Cache-Control", "no-cache, no-store, must-revalidate"); // HTTP 1.1.
				resp.setHeader("Pragma", "no-cache"); // HTTP 1.0.
				resp.setHeader("Expires", "0"); // Proxies.
				int bufferSize = 1024;
				

				resp.setContentType(type);
		        final byte[] buffer = new byte[bufferSize];
		        int bytesRead = 0;
		        OutputStream out = null;
		        try {
		            out = resp.getOutputStream();
		            long totalWritten = 0;
		            while ((bytesRead = inputStream.read(buffer)) > 0) {
		                out.write(buffer, 0, bytesRead);
		                totalWritten += bytesRead;
		                if (totalWritten >= buffer.length) {
		                    out.flush();
		                }
		            }
		        } finally {
		            tryToCloseStream(out);
		            tryToCloseStream(inputStream);
		        }  
		        String name = fileInfo.getFileName();
		        //String temporary ="C:\\hddt-ses\\server\\temporary";	       
		        File file = new File(SystemParams.DIR_E_INVOICE_TEMPORARY+"/"+ name);
		        file.delete();
				}catch (Exception e) {
					resp.setContentType("text/html; charset=utf-8");
			        resp.setCharacterEncoding("UTF-8");
			        resp.setHeader("success", "yes");
			        writer = resp.getWriter();
			        writer.write("Không tìm thấy thông tin hóa đơn. Vui lòng thử lại!!!");
			        writer.flush();
			        writer.close();
			        return;
				}
			}
		
		
		
		
		
		////
		
		
		@RequestMapping(
				value = {"common/getXmlThue/{token}"}
				, method = { RequestMethod.POST, RequestMethod.GET }
			)
			public void getXmlThue(Locale locale, HttpServletRequest req, HttpServletResponse resp, HttpSession session
					, @PathVariable(value = "token") String token
					) throws Exception {
		
				PrintWriter writer = null;
				FileInfo fileInfo = null;
				try {
				String uri = req.getRequestURI();
				boolean isConvert = uri.contains("print-einvoice-convert");
				
				if("".equals(token) || null == token) {
					resp.setContentType("text/html; charset=utf-8");
			        resp.setCharacterEncoding("UTF-8");
			        resp.setHeader("success", "yes");
			        writer = resp.getWriter();
			        writer.write("Không tìm thấy thông tin.");
			        writer.flush();
			        writer.close();
			        return;
				}
				
				CurrentUserProfile cup = getCurrentlyAuthenticatedPrincipal();
				BaseDTO dto = new BaseDTO();
				dto = new BaseDTO(req);
				Msg msg = dto.createMsg(cup, Constants.MSG_ACTION_CODE.CREATED);
				HashMap<String, Object> hData = new HashMap<>();
				hData.put("_token", token);
				hData.put("IsConvert", isConvert? "Y": "N");
				
				msg.setObjData(hData);
				JSONRoot root = new JSONRoot(msg);
				
				 fileInfo = restAPI.callAPIGetFileInfo("/commons/getXmlThue", cup.getLoginRes().getToken(), HttpMethod.POST, root);
				if(null == fileInfo || null == fileInfo.getContentFile()) {
					resp.setContentType("text/html; charset=utf-8");
			        resp.setCharacterEncoding("UTF-8");
			        resp.setHeader("success", "yes");
			        writer = resp.getWriter();
			        writer.write("Xuất XML không thành công.");
			        writer.flush();
			        writer.close();
			        return;
				}
				
				String type = "application/octet-stream";
				InputStream inputStream = new ByteArrayInputStream(fileInfo.getContentFile());
				resp.setHeader("Content-Type", type);
				resp.setHeader("Content-Disposition", "inline; filename=" + fileInfo.getFileName());
				resp.setHeader("Cache-Control", "no-cache, no-store, must-revalidate"); // HTTP 1.1.
				resp.setHeader("Pragma", "no-cache"); // HTTP 1.0.
				resp.setHeader("Expires", "0"); // Proxies.
				int bufferSize = 1024;
				

				resp.setContentType(type);
		        final byte[] buffer = new byte[bufferSize];
		        int bytesRead = 0;
		        OutputStream out = null;
		        try {
		            out = resp.getOutputStream();
		            long totalWritten = 0;
		            while ((bytesRead = inputStream.read(buffer)) > 0) {
		                out.write(buffer, 0, bytesRead);
		                totalWritten += bytesRead;
		                if (totalWritten >= buffer.length) {
		                    out.flush();
		                }
		            }
		        } finally {
		            tryToCloseStream(out);
		            tryToCloseStream(inputStream);
		        }   
		        
		        String name = fileInfo.getFileName();      
		        File file = new File(SystemParams.DIR_E_INVOICE_TEMPORARY+"/"+ name);
		        file.delete();
		        File file1 = new File(SystemParams.DIR_E_INVOICE_DATA+"/"+ name);
		        file1.delete();
				}catch (Exception e) {
					resp.setContentType("text/html; charset=utf-8");
			        resp.setCharacterEncoding("UTF-8");
			        resp.setHeader("success", "yes");
			        writer = resp.getWriter();
			        writer.write("Không tìm thấy thông tin hóa đơn. Vui lòng thử lại!!!");
			        writer.flush();
			        writer.close();
			        return;
				}
			}
		
		///PHAN QUYEN
		
				@SuppressWarnings("unchecked")
				@RequestMapping(value = "/common/getFullRightAdmin",  produces = MediaType.APPLICATION_JSON_VALUE, method = RequestMethod.POST)
				@ResponseBody
				public List<?> getFullRightAdmin(Locale locale, HttpServletRequest req, HttpSession session) throws Exception{
					CurrentUserProfile cup = getCurrentlyAuthenticatedPrincipal();
					
					BaseDTO baseDTO = new BaseDTO(req);
					Msg msg = baseDTO.createMsg(cup, Constants.MSG_ACTION_CODE.INQUIRY);
					HashMap<String, String> hData = new HashMap<>();
					msg.setObjData(hData);
					
					JSONRoot root = new JSONRoot(msg);
					MsgRsp rsp = restAPI.callAPINormal("/commons/getFullRightAdmin" , cup.getLoginRes().getToken(), HttpMethod.POST, root);
					MspResponseStatus rspStatus = rsp.getResponseStatus();
					if(rspStatus.getErrorCode() == 0
							&& rsp.getObjData() instanceof ArrayList) {
//						String action = null == session.getAttribute(Constants.SESSION_TYPE.SESSION_FORM_ACTION)? "": session.getAttribute(Constants.SESSION_TYPE.SESSION_FORM_ACTION).toString();
						String rights = null == session.getAttribute(Constants.SESSION_TYPE.SESSION_FULL_MENU_RIGHTS)? "": session.getAttribute(Constants.SESSION_TYPE.SESSION_FULL_MENU_RIGHTS).toString();
//						session.removeAttribute(Constants.SESSION_TYPE.SESSION_FORM_ACTION);
						session.removeAttribute(Constants.SESSION_TYPE.SESSION_FULL_MENU_RIGHTS);
						ArrayList<HashMap<String, Object>> arrayFullRights = (ArrayList<HashMap<String, Object>>) rsp.getObjData();
						for(HashMap<String, Object> obj: arrayFullRights) {
//							obj.put("chkDisabled", "|DETAIL|ACTIVE|".contains("|" + action + "|"));
							if(obj.get("action") != null) {
								obj.put("checked", rights.indexOf("|" + obj.get("action").toString() + "|") != -1);	
							}
						}
						return arrayFullRights;
					}else {
						return Arrays.asList();	
					}
					
				}
				
				
				@RequestMapping(value = "/common/processUploadFileTmp", produces = MediaType.APPLICATION_JSON_VALUE,
						method = RequestMethod.POST, consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
				@ResponseBody
				public BaseDTO processUploadFileTmp(HttpServletRequest req, 
						HttpSession session, MultipartHttpServletRequest multipartHttpServletRequest) throws Exception{
					BaseDTO dtoRes = new BaseDTO();
					CurrentUserProfile cup = getCurrentlyAuthenticatedPrincipal();
					
					byte[] data = null;
					String originalFilename = "";
					
					Map<String, MultipartFile> fileMap = multipartHttpServletRequest.getFileMap();
					for (MultipartFile multipartFile : fileMap.values()) {
						originalFilename = multipartFile.getOriginalFilename();
						data = multipartFile.getBytes();
						break;
					}
					
					if(data == null) {
						dtoRes = new BaseDTO(1, "Dữ liệu upload không tồn tại.");
						return dtoRes;
					}
					
					/*KIEM TRA THU MUC TMP; NEU CHUA CO THI TAO*/
					File file = new File(SystemParams.DIR_E_INVOICE_TEMPORARY);
					if(!file.exists()) file.mkdirs();
					
					String extensionFile = FilenameUtils.getExtension(originalFilename);
					String fileNameSystem = UUID.randomUUID() + "-" + commons.csRandomAlphaNumbericString(5) + "." + extensionFile;
					
					FileCopyUtils.copy(data, new FileOutputStream(new File(SystemParams.DIR_E_INVOICE_TEMPORARY, fileNameSystem)));
					
					HashMap<String, String> map = new HashMap<String, String>();
					map.put("SystemFilename", fileNameSystem);
					map.put("OriginalFilename", originalFilename);
					dtoRes.setResponseData(map);
					return dtoRes;
				}
				
				
				@RequestMapping(value = "/common/get-file/{name:.+}/{file-name:.+}", method = RequestMethod.GET)
				public void getFile(HttpServletRequest req, HttpServletResponse resp, HttpSession session
						, @PathVariable(name = "file-name", required = true) String fileName
						, @PathVariable(name = "name", required = true) String name
						) throws Exception{
					PrintWriter writer = null;
							
					Path path = Paths.get(SystemParams.DIR_E_INVOICE_DATA+ "/support", fileName);
					File file = path.toFile();
					if(!file.exists() || !file.isFile()) {
						resp.setContentType("text/html; charset=utf-8");
				        resp.setCharacterEncoding("UTF-8");
				        resp.setHeader("success", "yes");
				        resp.setStatus(HttpStatus.NOT_FOUND.value());
				        writer = resp.getWriter();
				        writer.write(HttpStatus.NOT_FOUND.getReasonPhrase());
				        writer.close();
				        return;
					}
					
					InputStream is = new FileInputStream(file);
					resp.setContentType("application/force-download");
				    resp.setHeader("Content-Disposition", "attachment; filename=" + name + "");
				    int read=0;
				    byte[] bytes = new byte[SystemParams.BUFFER_SIZE];
				    OutputStream os = resp.getOutputStream();

				    while((read = is.read(bytes))!= -1){
				        os.write(bytes, 0, read);
				    }
				    os.flush();
				    os.close(); 
					is.close();
					
					
					//  String name = fileInfo.getFileName();      
				        File file_delete = new File(SystemParams.DIR_E_INVOICE_TEMPORARY +"/"+ fileName);
				        file_delete.delete();
				       
					return;
					
					
				}	
				
				
				
				
				
				@RequestMapping(
						value = {"/common/exportXml/{_id}"}
						, method = { RequestMethod.POST, RequestMethod.GET }
					)
					public void exportXml(Locale locale, HttpServletRequest req, HttpServletResponse resp, HttpSession session
							, @PathVariable(value = "_id") String _id
							) throws Exception {
						
					
						PrintWriter writer = null;
						FileInfo fileInfo = null;
						try {
						String uri = req.getRequestURI();
						boolean isConvert = uri.contains("print-export-convert");
						_token = _id;
						
						if("".equals(_id) || null == _id) {
							resp.setContentType("text/html; charset=utf-8");
					        resp.setCharacterEncoding("UTF-8");
					        resp.setHeader("success", "yes");
					        writer = resp.getWriter();
					        writer.write("Không tìm thấy thông tin hóa đơn.");
					        writer.flush();
					        writer.close();
					        return;
						}
						
						CurrentUserProfile cup = getCurrentlyAuthenticatedPrincipal();
						BaseDTO dto = new BaseDTO();
						dto = new BaseDTO(req);
						Msg msg = dto.createMsg(cup, Constants.MSG_ACTION_CODE.CREATED);
						HashMap<String, Object> hData = new HashMap<>();
						hData.put("_token", _token);
						hData.put("IsConvert", isConvert? "Y": "N");
						
						msg.setObjData(hData);
						JSONRoot root = new JSONRoot(msg);
						
						 fileInfo = restAPI.callAPIGetFileInfo("/commons/exportXml", cup.getLoginRes().getToken(), HttpMethod.POST, root);
						if(null == fileInfo || null == fileInfo.getContentFile()) {
							resp.setContentType("text/html; charset=utf-8");
					        resp.setCharacterEncoding("UTF-8");
					        resp.setHeader("success", "yes");
					        writer = resp.getWriter();
					        writer.write("Xuất XML không thành công.");
					        writer.flush();
					        writer.close();
					        return;
						}
						
						String type = "application/octet-stream";
						InputStream inputStream = new ByteArrayInputStream(fileInfo.getContentFile());
						resp.setHeader("Content-Type", type);
						resp.setHeader("Content-Disposition", "inline; filename=" + fileInfo.getFileName());
						resp.setHeader("Cache-Control", "no-cache, no-store, must-revalidate"); // HTTP 1.1.
						resp.setHeader("Pragma", "no-cache"); // HTTP 1.0.
						resp.setHeader("Expires", "0"); // Proxies.
						int bufferSize = 1024;
						

						resp.setContentType(type);
				        final byte[] buffer = new byte[bufferSize];
				        int bytesRead = 0;
				        OutputStream out = null;
				        try {
				            out = resp.getOutputStream();
				            long totalWritten = 0;
				            while ((bytesRead = inputStream.read(buffer)) > 0) {
				                out.write(buffer, 0, bytesRead);
				                totalWritten += bytesRead;
				                if (totalWritten >= buffer.length) {
				                    out.flush();
				                }
				            }
				        } finally {
				            tryToCloseStream(out);
				            tryToCloseStream(inputStream);
				        }   
						}catch (Exception e) {
							resp.setContentType("text/html; charset=utf-8");
					        resp.setCharacterEncoding("UTF-8");
					        resp.setHeader("success", "yes");
					        writer = resp.getWriter();
					        writer.write("Không tìm thấy thông tin hóa đơn. Vui lòng thử lại!!!");
					        writer.flush();
					        writer.close();
					        return;
						}
					}
					
					//IN PDF HANG LOAT
				
				@RequestMapping(
						value = {"/common/exportPDF/{_id}"}
						, method = { RequestMethod.POST, RequestMethod.GET }
					)
					public void export_PRINT_PDF(Locale locale, HttpServletRequest req, HttpServletResponse resp, HttpSession session
							, @PathVariable(value = "_id") String _id
							) throws Exception {
						
					
						PrintWriter writer = null;
						FileInfo fileInfo = null;
						try {
						String uri = req.getRequestURI();
						boolean isConvert = uri.contains("print-export-convert");
						_token = _id;
						
						if("".equals(_id) || null == _id) {
							resp.setContentType("text/html; charset=utf-8");
					        resp.setCharacterEncoding("UTF-8");
					        resp.setHeader("success", "yes");
					        writer = resp.getWriter();
					        writer.write("Không tìm thấy thông tin hóa đơn.");
					        writer.flush();
					        writer.close();
					        return;
						}
						
						CurrentUserProfile cup = getCurrentlyAuthenticatedPrincipal();
						BaseDTO dto = new BaseDTO();
						dto = new BaseDTO(req);
						Msg msg = dto.createMsg(cup, Constants.MSG_ACTION_CODE.CREATED);
						HashMap<String, Object> hData = new HashMap<>();
						hData.put("_token", _token);
						hData.put("IsConvert", isConvert? "Y": "N");
						
						msg.setObjData(hData);
						JSONRoot root = new JSONRoot(msg);
						
						 fileInfo = restAPI.callAPIGetFileInfo("/commons/exportPDF", cup.getLoginRes().getToken(), HttpMethod.POST, root);
						if(null == fileInfo || null == fileInfo.getContentFile()) {
							resp.setContentType("text/html; charset=utf-8");
					        resp.setCharacterEncoding("UTF-8");
					        resp.setHeader("success", "yes");
					        writer = resp.getWriter();
					        writer.write("Xuất PDF không thành công.");
					        writer.flush();
					        writer.close();
					        return;
						}
						
						String type = "application/octet-stream";
						InputStream inputStream = new ByteArrayInputStream(fileInfo.getContentFile());
						resp.setHeader("Content-Type", type);
						resp.setHeader("Content-Disposition", "inline; filename=" + fileInfo.getFileName());
						resp.setHeader("Cache-Control", "no-cache, no-store, must-revalidate"); // HTTP 1.1.
						resp.setHeader("Pragma", "no-cache"); // HTTP 1.0.
						resp.setHeader("Expires", "0"); // Proxies.
						int bufferSize = 1024;
						

						resp.setContentType(type);
				        final byte[] buffer = new byte[bufferSize];
				        int bytesRead = 0;
				        OutputStream out = null;
				        try {
				            out = resp.getOutputStream();
				            long totalWritten = 0;
				            while ((bytesRead = inputStream.read(buffer)) > 0) {
				                out.write(buffer, 0, bytesRead);
				                totalWritten += bytesRead;
				                if (totalWritten >= buffer.length) {
				                    out.flush();
				                }
				            }
				        } finally {
				            tryToCloseStream(out);
				            tryToCloseStream(inputStream);
				        }   
						}catch (Exception e) {
							resp.setContentType("text/html; charset=utf-8");
					        resp.setCharacterEncoding("UTF-8");
					        resp.setHeader("success", "yes");
					        writer = resp.getWriter();
					        writer.write("Không tìm thấy thông tin hóa đơn. Vui lòng thử lại!!!");
					        writer.flush();
					        writer.close();
					        return;
						}
					}
				
				
				@RequestMapping(
						value = {"/common/print-exportAll/{_id}","/common/print-exportCD/{_id}"}
						, method = { RequestMethod.POST, RequestMethod.GET }
					)
					public void printExportAll(Locale locale, HttpServletRequest req, HttpServletResponse resp, HttpSession session
							, @PathVariable(value = "_id") String _id
							) throws Exception {
						
					
						PrintWriter writer = null;
						FileInfo fileInfo = null;
						try {
						String uri = req.getRequestURI();
						boolean isConvert = uri.contains("print-exportCD");
//						_token = _id;
//						ids = null;
//						try {
//							ids = Json.serializer().fromJson(commons.decodeBase64ToString(_token), new TypeReference<List<String>>() {
//							});
//						}catch(Exception e) {}
					
						_id = _id.replaceAll("\\s", "");
						if("".equals(_id) || null == _id) {
							resp.setContentType("text/html; charset=utf-8");
					        resp.setCharacterEncoding("UTF-8");
					        resp.setHeader("success", "yes");
					        writer = resp.getWriter();
					        writer.write("Không tìm thấy thông tin hóa đơn.");
					        writer.flush();
					        writer.close();
					        return;
						}
						
						CurrentUserProfile cup = getCurrentlyAuthenticatedPrincipal();
						BaseDTO dto = new BaseDTO();
						dto = new BaseDTO(req);
						Msg msg = dto.createMsg(cup, Constants.MSG_ACTION_CODE.CREATED);
						HashMap<String, Object> hData = new HashMap<>();
						hData.put("_token", _id);
						hData.put("IsConvert", isConvert? "Y": "N");
						
						msg.setObjData(hData);
						JSONRoot root = new JSONRoot(msg);
						
						 fileInfo = restAPI.callAPIGetFileInfo("/commons/print-exportAll", cup.getLoginRes().getToken(), HttpMethod.POST, root);
						if(null == fileInfo || null == fileInfo.getContentFile()) {
							resp.setContentType("text/html; charset=utf-8");
					        resp.setCharacterEncoding("UTF-8");
					        resp.setHeader("success", "yes");
					        writer = resp.getWriter();
					        writer.write("In hóa đơn không thành công.");
					        writer.flush();
					        writer.close();
					        return;
						}
						
						String type = "application/octet-stream";
//						String type = "application/pdf";
						InputStream inputStream = new ByteArrayInputStream(fileInfo.getContentFile());
						resp.setHeader("Content-Type", type);
						resp.setHeader("Content-Disposition", "inline; filename=" + fileInfo.getFileName());
						resp.setHeader("Cache-Control", "no-cache, no-store, must-revalidate"); // HTTP 1.1.
						resp.setHeader("Pragma", "no-cache"); // HTTP 1.0.
						resp.setHeader("Expires", "0"); // Proxies.
						int bufferSize = 1024;
						resp.setContentType(type);
				        final byte[] buffer = new byte[bufferSize];
				        int bytesRead = 0;
				        OutputStream out = null;
				        try {
				            out = resp.getOutputStream();
				            long totalWritten = 0;
				            while ((bytesRead = inputStream.read(buffer)) > 0) {
				                out.write(buffer, 0, bytesRead);
				                totalWritten += bytesRead;
				                if (totalWritten >= buffer.length) {
				                    out.flush();
				                }
				            }
				        } finally {
				            tryToCloseStream(out);
				            tryToCloseStream(inputStream);
				        }   
				        
						}catch (Exception e) {
							resp.setContentType("text/html; charset=utf-8");
					        resp.setCharacterEncoding("UTF-8");
					        resp.setHeader("success", "yes");
					        writer = resp.getWriter();
					        writer.write("Không tìm thấy thông tin hóa đơn. Vui lòng thử lại!!!");
					        writer.flush();
					        writer.close();
					        return;
						}
				}
	///PHAN QUYEN ADMIN
				
				@SuppressWarnings("unchecked")
				@RequestMapping(value = "/common/getFullRightAdminManager",  produces = MediaType.APPLICATION_JSON_VALUE, method = RequestMethod.POST)
				@ResponseBody
				public List<?> getFullRightAdminManager(Locale locale, HttpServletRequest req, HttpSession session) throws Exception{
					CurrentUserProfile cup = getCurrentlyAuthenticatedPrincipal();
					
					BaseDTO baseDTO = new BaseDTO(req);
					Msg msg = baseDTO.createMsg(cup, Constants.MSG_ACTION_CODE.INQUIRY);
					HashMap<String, String> hData = new HashMap<>();
					msg.setObjData(hData);
					
					JSONRoot root = new JSONRoot(msg);
					MsgRsp rsp = restAPI.callAPINormal("/commons/getFullRightAdminManager" , cup.getLoginRes().getToken(), HttpMethod.POST, root);
					MspResponseStatus rspStatus = rsp.getResponseStatus();
					if(rspStatus.getErrorCode() == 0
							&& rsp.getObjData() instanceof ArrayList) {
//						String action = null == session.getAttribute(Constants.SESSION_TYPE.SESSION_FORM_ACTION)? "": session.getAttribute(Constants.SESSION_TYPE.SESSION_FORM_ACTION).toString();
						String rights = null == session.getAttribute(Constants.SESSION_TYPE.SESSION_FULL_MENU_RIGHTS)? "": session.getAttribute(Constants.SESSION_TYPE.SESSION_FULL_MENU_RIGHTS).toString();
//						session.removeAttribute(Constants.SESSION_TYPE.SESSION_FORM_ACTION);
						session.removeAttribute(Constants.SESSION_TYPE.SESSION_FULL_MENU_RIGHTS);
						ArrayList<HashMap<String, Object>> arrayFullRights = (ArrayList<HashMap<String, Object>>) rsp.getObjData();
						for(HashMap<String, Object> obj: arrayFullRights) {
//							obj.put("chkDisabled", "|DETAIL|ACTIVE|".contains("|" + action + "|"));
							if(obj.get("action") != null) {
								obj.put("checked", rights.indexOf("|" + obj.get("action").toString() + "|") != -1);	
							}
						}
						return arrayFullRights;
					}else {
						return Arrays.asList();	
					}
					
				}
				
				///
				
				
				@Transactional(rollbackFor = {Exception.class})
				@RequestMapping(value = "/common/detailMaSoThue",  produces = MediaType.APPLICATION_JSON_VALUE, method = RequestMethod.POST)
				@ResponseBody
				public BaseDTO execDetailMaSoThue(HttpServletRequest req, HttpSession session
						, @RequestParam(value = "mst", required = false, defaultValue = "") String taxCode ) throws Exception{
					BaseDTO dtoRes = new BaseDTO();
					
					try {
					taxCode = taxCode.replaceAll("\\s", "");
					if("".equals(taxCode)) {
						dtoRes.setErrorCode(1);
						dtoRes.setResponseData("Mã số thuế không được rỗng.");
						return dtoRes;
					}
					
					
					CurrentUserProfile cup = getCurrentlyAuthenticatedPrincipal();
					BaseDTO baseDTO = new BaseDTO(req);
					Msg msg = baseDTO.createMsg(cup, Constants.MSG_ACTION_CODE.LOAD_PARAMS);
					HashMap<String, String> hData = new HashMap<>();
					hData.put("MST", taxCode);
					msg.setObjData(hData);
					
					JSONRoot root = new JSONRoot(msg);					
					MsgRsp rsp = restAPI.callAPINormal("/commons/detailMaSoThue", cup.getLoginRes().getToken(), HttpMethod.POST, root);
					MspResponseStatus rspStatus = rsp.getResponseStatus();
					if(rspStatus.getErrorCode() == 0) {				
						JsonNode jsonData = Json.serializer().nodeFromObject(rsp.getObjData());
						HashMap<String, String> hR = new HashMap<String, String>();						
						hR.put("ten_cong_ty", commons.getTextJsonNode(jsonData.at("/ten_cong_ty")));
						hR.put("dia_chi", commons.getTextJsonNode(jsonData.at("/dia_chi")));		
						dtoRes.setErrorCode(0);
						dtoRes.setResponseData(hR);
					}else {
						dtoRes.setErrorCode(999);
					}

					}catch (Exception e) {
						dtoRes.setErrorCode(999);
					}
					return dtoRes;
				}
					
				
				@RequestMapping(
						value = {"/common/view-pdf-tiepnhan/{_id}", "/common/view-pdf-tiepnhan-convert/{_id}"}
						, method = { RequestMethod.POST, RequestMethod.GET }
					)
				public void viewPdfTiepnhan(Locale locale, HttpServletRequest req, HttpServletResponse resp, HttpSession session
						, @PathVariable(value = "_id") String _id
					) throws Exception {
					PrintWriter writer = null;
					
					try {
					String uri = req.getRequestURI();
					boolean isConvert = uri.contains("print04-convert");
					
					_id = _id.replaceAll("\\s", "");
					if("".equals(_id) || null == _id) {
						resp.setContentType("text/html; charset=utf-8");
				        resp.setCharacterEncoding("UTF-8");
				        resp.setHeader("success", "yes");
				        writer = resp.getWriter();
				        writer.write("Không tìm thấy thông tin.");
				        writer.flush();
				        writer.close();
				        return;
					}
					
					CurrentUserProfile cup = getCurrentlyAuthenticatedPrincipal();
					BaseDTO dto = new BaseDTO();
					
					dto = new BaseDTO(req);
					Msg msg = dto.createMsg(cup, Constants.MSG_ACTION_CODE.CREATED);
					HashMap<String, Object> hData = new HashMap<>();
					hData.put("_id", _id);
					hData.put("IsConvert", isConvert? "Y": "N");
					
					msg.setObjData(hData);
					JSONRoot root = new JSONRoot(msg);
					
					FileInfo fileInfo = restAPI.callAPIGetFileInfo("/commons/view-pdf-tiepnhan", cup.getLoginRes().getToken(), HttpMethod.POST, root);
					if(null == fileInfo || null == fileInfo.getContentFile()) {
						resp.setContentType("text/html; charset=utf-8");
				        resp.setCharacterEncoding("UTF-8");
				        resp.setHeader("success", "yes");
				        writer = resp.getWriter();
				        writer.write("Xem mẫu hóa đơn sai sót không thành công.");
				        writer.flush();
				        writer.close();
				        return;
					}
					
					String type = "application/pdf";
					InputStream inputStream = new ByteArrayInputStream(fileInfo.getContentFile());
					resp.setHeader("Content-Type", type);
					
					resp.setHeader("Cache-Control", "no-cache, no-store, must-revalidate"); // HTTP 1.1.
					resp.setHeader("Pragma", "no-cache"); // HTTP 1.0.
					resp.setHeader("Expires", "0"); // Proxies.
					
					int bufferSize = 1024;
					resp.setContentType(type);
			        final byte[] buffer = new byte[bufferSize];
			        int bytesRead = 0;
			        OutputStream out = null;
			        try {
			            out = resp.getOutputStream();
			            long totalWritten = 0;
			            while ((bytesRead = inputStream.read(buffer)) > 0) {
			                out.write(buffer, 0, bytesRead);
			                totalWritten += bytesRead;
			                if (totalWritten >= buffer.length) {
			                    out.flush();
			                }
			            }
			        } finally {
			            tryToCloseStream(out);
			            tryToCloseStream(inputStream);
			        }
					}catch (Exception e) {
						resp.setContentType("text/html; charset=utf-8");
				        resp.setCharacterEncoding("UTF-8");
				        resp.setHeader("success", "yes");
				        writer = resp.getWriter();
				        writer.write("Không tìm thấy thông tin hóa đơn. Vui lòng thử lại!!!");
				        writer.flush();
				        writer.close();
				        return;
					}
				}
				
				
				
				@RequestMapping(
						value = {"/common/cttncnXml/{_id}"}
						, method = { RequestMethod.POST, RequestMethod.GET }
					)
					public void cttncnXml(Locale locale, HttpServletRequest req, HttpServletResponse resp, HttpSession session
							, @PathVariable(value = "_id") String _id
							) throws Exception {
					
						PrintWriter writer = null;
						FileInfo fileInfo = null;
						try {
						String uri = req.getRequestURI();
						boolean isConvert = uri.contains("print-cttncn-convert");
						_token = _id;
						
						if("".equals(_id) || null == _id) {
							resp.setContentType("text/html; charset=utf-8");
					        resp.setCharacterEncoding("UTF-8");
					        resp.setHeader("success", "yes");
					        writer = resp.getWriter();
					        writer.write("Không tìm thấy thông tin chứng từ.");
					        writer.flush();
					        writer.close();
					        return;
						}
						
						CurrentUserProfile cup = getCurrentlyAuthenticatedPrincipal();
						BaseDTO dto = new BaseDTO();
						dto = new BaseDTO(req);
						Msg msg = dto.createMsg(cup, Constants.MSG_ACTION_CODE.CREATED);
						HashMap<String, Object> hData = new HashMap<>();
						hData.put("_token", _token);
						hData.put("IsConvert", isConvert? "Y": "N");
						
						msg.setObjData(hData);
						JSONRoot root = new JSONRoot(msg);
						
						 fileInfo = restAPI.callAPIGetFileInfo("/commons/cttncnXml", cup.getLoginRes().getToken(), HttpMethod.POST, root);
						if(null == fileInfo || null == fileInfo.getContentFile()) {
							resp.setContentType("text/html; charset=utf-8");
					        resp.setCharacterEncoding("UTF-8");
					        resp.setHeader("success", "yes");
					        writer = resp.getWriter();
					        writer.write("Xuất XML không thành công.");
					        writer.flush();
					        writer.close();
					        return;
						}
						
						String type = "application/octet-stream";
						InputStream inputStream = new ByteArrayInputStream(fileInfo.getContentFile());
						resp.setHeader("Content-Type", type);
						resp.setHeader("Content-Disposition", "inline; filename=" + fileInfo.getFileName());
						resp.setHeader("Cache-Control", "no-cache, no-store, must-revalidate"); // HTTP 1.1.
						resp.setHeader("Pragma", "no-cache"); // HTTP 1.0.
						resp.setHeader("Expires", "0"); // Proxies.
						int bufferSize = 1024;
						

						resp.setContentType(type);
				        final byte[] buffer = new byte[bufferSize];
				        int bytesRead = 0;
				        OutputStream out = null;
				        try {
				            out = resp.getOutputStream();
				            long totalWritten = 0;
				            while ((bytesRead = inputStream.read(buffer)) > 0) {
				                out.write(buffer, 0, bytesRead);
				                totalWritten += bytesRead;
				                if (totalWritten >= buffer.length) {
				                    out.flush();
				                }
				            }
				        } finally {
				            tryToCloseStream(out);
				            tryToCloseStream(inputStream);
				        } 
				        
						}catch (Exception e) {
							resp.setContentType("text/html; charset=utf-8");
					        resp.setCharacterEncoding("UTF-8");
					        resp.setHeader("success", "yes");
					        writer = resp.getWriter();
					        writer.write("Không tìm thấy thông tin hóa đơn. Vui lòng thử lại!!!");
					        writer.flush();
					        writer.close();
					        return;
						}
					}

				
				@RequestMapping(value = "/common/saveDataBas64", produces = MediaType.APPLICATION_JSON_VALUE, method = {RequestMethod.POST})
				@ResponseBody
				public BaseDTO saveDataBas64(Locale locale, HttpServletRequest req, HttpSession session
						) throws Exception{
					
					
					BaseDTO dtoRes = new BaseDTO();
					try {
					dtoRes = new BaseDTO(req);	
					String ArrayData = "";	
					ArrayData = commons.getParameterFromRequest(req, "ArrayData").replaceAll("\\s", "");
					CurrentUserProfile cup = getCurrentlyAuthenticatedPrincipal();
					BaseDTO baseDTO = new BaseDTO(req);
					Msg msg = baseDTO.createMsg(cup, Constants.MSG_ACTION_CODE.INQUIRY);
					HashMap<String, String> hData = new HashMap<>();
					hData.put("ArrayData", ArrayData);
					msg.setObjData(hData);
					
					JSONRoot root = new JSONRoot(msg);
					MsgRsp rsp = restAPI.callAPINormal("/commons/saveDataToBase64", cup.getLoginRes().getToken(), HttpMethod.POST, root);
					MspResponseStatus rspStatus = rsp.getResponseStatus();
					if(rspStatus.getErrorCode() == 0) {						
						dtoRes.setErrorCode(0);
						HashMap<String, Object> hR = new HashMap<String, Object>();
//						String uuid = UUID.randomUUID().toString().replaceAll("-", "").toUpperCase();					
						hR.put("ID", rspStatus.getErrorDesc());	
//						hR.put("UUID", uuid);	
						dtoRes.setResponseData(hR);
					}else {
						dtoRes.setErrorCode(999);
						dtoRes.setResponseData("Tối đa là 50 hóa đơn. Vui lòng chọn lại !!!");
					}
					}catch (Exception e) {
						dtoRes.setErrorCode(999);
					}

					return dtoRes;
				}
				
				
				
				/* MAY TINH TIEN */
				
				
				@RequestMapping(
						value = {"/common/print04_mtt/{_id}", "/common/print04_mtt-convert/{_id}"}
						, method = { RequestMethod.POST, RequestMethod.GET }
					)
				public void print04_mtt(Locale locale, HttpServletRequest req, HttpServletResponse resp, HttpSession session
						, @PathVariable(value = "_id") String _id
					) throws Exception {
					PrintWriter writer = null;
					
					String uri = req.getRequestURI();
					boolean isConvert = uri.contains("print04-convert");
					
					_id = _id.replaceAll("\\s", "");
					if("".equals(_id) || null == _id) {
						resp.setContentType("text/html; charset=utf-8");
				        resp.setCharacterEncoding("UTF-8");
				        resp.setHeader("success", "yes");
				        writer = resp.getWriter();
				        writer.write("Không tìm thấy thông tin mẫu hóa đơn sai sót.");
				        writer.flush();
				        writer.close();
				        return;
					}
					
					CurrentUserProfile cup = getCurrentlyAuthenticatedPrincipal();
					BaseDTO dto = new BaseDTO();
					
					dto = new BaseDTO(req);
					Msg msg = dto.createMsg(cup, Constants.MSG_ACTION_CODE.CREATED);
					HashMap<String, Object> hData = new HashMap<>();
					hData.put("_id", _id);
					hData.put("IsConvert", isConvert? "Y": "N");
					
					msg.setObjData(hData);
					JSONRoot root = new JSONRoot(msg);
					
					FileInfo fileInfo = restAPI.callAPIGetFileInfo("/commons/print04_mtt", cup.getLoginRes().getToken(), HttpMethod.POST, root);
					if(null == fileInfo || null == fileInfo.getContentFile()) {
						resp.setContentType("text/html; charset=utf-8");
				        resp.setCharacterEncoding("UTF-8");
				        resp.setHeader("success", "yes");
				        writer = resp.getWriter();
				        writer.write("Xem mẫu hóa đơn sai sót không thành công.");
				        writer.flush();
				        writer.close();
				        return;
					}
					
					String type = "application/pdf";
					InputStream inputStream = new ByteArrayInputStream(fileInfo.getContentFile());
					resp.setHeader("Content-Type", type);
					
					resp.setHeader("Cache-Control", "no-cache, no-store, must-revalidate"); // HTTP 1.1.
					resp.setHeader("Pragma", "no-cache"); // HTTP 1.0.
					resp.setHeader("Expires", "0"); // Proxies.
					
					int bufferSize = 1024;
					resp.setContentType(type);
			        final byte[] buffer = new byte[bufferSize];
			        int bytesRead = 0;
			        OutputStream out = null;
			        try {
			            out = resp.getOutputStream();
			            long totalWritten = 0;
			            while ((bytesRead = inputStream.read(buffer)) > 0) {
			                out.write(buffer, 0, bytesRead);
			                totalWritten += bytesRead;
			                if (totalWritten >= buffer.length) {
			                    out.flush();
			                }
			            }
			        } finally {
			            tryToCloseStream(out);
			            tryToCloseStream(inputStream);
			        }
			     
				}	
				
				@RequestMapping(
						value = {"/common/print-einvoice_mtt/{_id}", "/common/print-einvoice_mtt-convert/{_id}"}
						, method = { RequestMethod.POST, RequestMethod.GET }
					)
					public void printEInvoiceMTT(Locale locale, HttpServletRequest req, HttpServletResponse resp, HttpSession session
							, @PathVariable(value = "_id") String _id
						) throws Exception {
						PrintWriter writer = null;
						
						String uri = req.getRequestURI();
						boolean isConvert = uri.contains("print-einvoice_mtt-convert");
						
						_id = _id.replaceAll("\\s", "");
						if("".equals(_id) || null == _id) {
							resp.setContentType("text/html; charset=utf-8");
					        resp.setCharacterEncoding("UTF-8");
					        resp.setHeader("success", "yes");
					        writer = resp.getWriter();
					        writer.write("Không tìm thấy thông tin hóa đơn.");
					        writer.flush();
					        writer.close();
					        return;
						}
						
						CurrentUserProfile cup = getCurrentlyAuthenticatedPrincipal();
						BaseDTO dto = new BaseDTO();
						dto = new BaseDTO(req);
						Msg msg = dto.createMsg(cup, Constants.MSG_ACTION_CODE.CREATED);
						HashMap<String, Object> hData = new HashMap<>();
						hData.put("_id", _id);
						hData.put("IsConvert", isConvert? "Y": "N");
						
						msg.setObjData(hData);
						JSONRoot root = new JSONRoot(msg);
						
						FileInfo fileInfo = restAPI.callAPIGetFileInfo("/commons/print-einvoice_mtt", cup.getLoginRes().getToken(), HttpMethod.POST, root);
						if(null == fileInfo || null == fileInfo.getContentFile()) {
							resp.setContentType("text/html; charset=utf-8");
					        resp.setCharacterEncoding("UTF-8");
					        resp.setHeader("success", "yes");
					        writer = resp.getWriter();
					        writer.write("In hóa đơn không thành công.");
					        writer.flush();
					        writer.close();
					        return;
						}
						
						String type = "application/pdf";
						InputStream inputStream = new ByteArrayInputStream(fileInfo.getContentFile());
						resp.setHeader("Content-Type", type);
						
						resp.setHeader("Cache-Control", "no-cache, no-store, must-revalidate"); // HTTP 1.1.
						resp.setHeader("Pragma", "no-cache"); // HTTP 1.0.
						resp.setHeader("Expires", "0"); // Proxies.
						
						int bufferSize = 1024;
						resp.setContentType(type);
				        final byte[] buffer = new byte[bufferSize];
				        int bytesRead = 0;
				        OutputStream out = null;
				        try {
				            out = resp.getOutputStream();
				            long totalWritten = 0;
				            while ((bytesRead = inputStream.read(buffer)) > 0) {
				                out.write(buffer, 0, bytesRead);
				                totalWritten += bytesRead;
				                if (totalWritten >= buffer.length) {
				                    out.flush();
				                }
				            }
				        } finally {
				            tryToCloseStream(out);
				            tryToCloseStream(inputStream);
				        }
				        
					}

	///check cts EINVOICE MTT
				@RequestMapping(value = {"/common/check-cert_mtt", "/common/check-cert_mtt-full"},  produces = MediaType.APPLICATION_JSON_VALUE, method = RequestMethod.POST)
				@ResponseBody
				public BaseDTO execCheckCertMTT(Locale locale, HttpServletRequest req, HttpSession session
						, @RequestParam(value = "cert", required = false, defaultValue = "") String cert
						, @RequestAttribute(name = "method", value = "") String method
					) throws Exception {
					BaseDTO dtoRes = new BaseDTO();
					
//					System.out.println("method = " + method);
//					System.out.println(req.getRequestURI());
					
					if("".equals(cert)) {
						dtoRes.setErrorCode(1);
						dtoRes.setResponseData("Chứng thư số không hợp lệ (CKS Rỗng)");
						return dtoRes;
					}
					
					//LAY THONG TIN CERT - KIEM TRA SAU
							CertificateFactory certificateFactory = null;
							InputStream in = null;
							X509Certificate x509Cert = null;
							
							certificateFactory = CertificateFactory.getInstance("X.509");
							cert = cert.replaceAll("@", "+");
							in = new ByteArrayInputStream(DatatypeConverter.parseBase64Binary(cert));
							x509Cert = (X509Certificate) certificateFactory.generateCertificate(in);	
							String serialNumber = x509Cert.getSerialNumber().toString(16);
							SignTypeInfo signTypeInfo = commons.parserCert(cert);
							 String mst = signTypeInfo.getTaxCode();
							 
							 
					if(serialNumber.length() <32)
					{
						serialNumber = "0" +serialNumber;
					}
					
					CurrentUserProfile cup = getCurrentlyAuthenticatedPrincipal();
					LoginRes issu = cup.getLoginRes();
					String mstLogin = issu.getUserName();
					
					String []split_mst =  mstLogin.split("_");
					int dem_mst = split_mst.length;
					if(dem_mst==2) {
						mstLogin = split_mst[0];
					}else {
						mstLogin = mstLogin;
					}
					
					if(!mstLogin.equals(mst)) {
						dtoRes.setErrorCode(1);
						dtoRes.setResponseData("Mã số thuế không khớp với chứng thư số");
						return dtoRes;
					}
					
					BaseDTO baseDTO = new BaseDTO(req);
					Msg msg = baseDTO.createMsg(cup, Constants.MSG_ACTION_CODE.INQUIRY);
					HashMap<String, String> hData = new HashMap<>();
					hData.put("MSTCKS", mst);
					msg.setObjData(hData);
					
					JSONRoot root = new JSONRoot(msg);
					MsgRsp rsp = restAPI.callAPINormal("/cks/check/" + serialNumber, cup.getLoginRes().getToken(), HttpMethod.POST, root);
					MspResponseStatus rspStatus = rsp.getResponseStatus();
					if(rspStatus.getErrorCode() == 0) {
						JsonNode jsonData = Json.serializer().nodeFromObject(rsp.getObjData());
										
					if(serialNumber.length() % 2 == 1)
						serialNumber = "0" + serialNumber;
					
				boolean ccheck = false;
					if(!jsonData.at("/DSCTSSDung").isMissingNode()) {
						for(JsonNode o: jsonData.at("/DSCTSSDung")) {
							String checkserri =  commons.getTextJsonNode(o.at("/Seri"));
							String isActive =  commons.getTextJsonNode(jsonData.at("/IsActive"));
							if(checkserri.length() <32)
							{
								checkserri = "0" +checkserri;
							}
						if(checkserri.equals(serialNumber)) {
							ccheck = true;
							break;
						}			
						if(isActive.equals("false")){
							ccheck = false;
							break;
						}
					}
				}
				if(ccheck != true) {
					dtoRes.setErrorCode(1);
					dtoRes.setResponseData("Chứng thư số không hợp lệ (không tìm thấy CKS).");
					return dtoRes;
				}
					}
					
					else if(rspStatus.getErrorCode() == 300) {
						dtoRes.setErrorCode(0);
						HashMap<String, String> hR = new LinkedHashMap<String, String>();
						hR.put("check", "300");
						hR.put("TTChuc", commons.getAttributeCertificate(x509Cert.getIssuerDN().toString(), "O"));
						hR.put("Seri", serialNumber);
						hR.put("TNgay", commons.convertLocalDateTimeToString(commons.convertDateGMT7ToLocalDateTime(x509Cert.getNotBefore()), Constants.FORMAT_DATE.FORMAT_DATE_TIME_WEB));
						hR.put("DNgay", commons.convertLocalDateTimeToString(commons.convertDateGMT7ToLocalDateTime(x509Cert.getNotAfter()), Constants.FORMAT_DATE.FORMAT_DATE_TIME_WEB));
						dtoRes.setResponseData(hR);
						return dtoRes;
					}
					else if(rspStatus.getErrorCode() == 301) {
						dtoRes.setErrorCode(0);
						HashMap<String, String> hR = new LinkedHashMap<String, String>();
						hR.put("check", "300");
						hR.put("Seri", serialNumber);
						dtoRes.setResponseData(hR);
						return dtoRes;
					}
					else {
						dtoRes.setErrorCode(1);
						dtoRes.setResponseData("Chứng thư số không hợp lệ (Vui lòng kiểm tra CKS đã đăng ký thành công chưa).");
						return dtoRes;
					}
					

					
					dtoRes.setErrorCode(0);
					if("check-cert-full".equals(method)) {
						HashMap<String, String> hR = new LinkedHashMap<String, String>();
						hR.put("TTChuc", commons.getAttributeCertificate(x509Cert.getIssuerDN().toString(), "O"));
						hR.put("Seri", serialNumber);
						hR.put("TNgay", commons.convertLocalDateTimeToString(commons.convertDateGMT7ToLocalDateTime(x509Cert.getNotBefore()), Constants.FORMAT_DATE.FORMAT_DATE_TIME_WEB));
						hR.put("DNgay", commons.convertLocalDateTimeToString(commons.convertDateGMT7ToLocalDateTime(x509Cert.getNotAfter()), Constants.FORMAT_DATE.FORMAT_DATE_TIME_WEB));
						dtoRes.setResponseData(hR);
					}else {
						HashMap<String, String> hR = new LinkedHashMap<String, String>();
						hR.put("Seri", serialNumber);
					dtoRes.setResponseData(hR);
					}
					return dtoRes;
				}
				
				
				
				
				
				
				
				
				
				
				
				
				//////////SHOW EINVOICE MTT
				private void LoadParameterFor_DSHDDKyMTT(CurrentUserProfile cup, Locale locale, HttpServletRequest req, String action) {
					try {
						BaseDTO baseDTO = new BaseDTO(req);
						Msg msg = baseDTO.createMsg(cup, Constants.MSG_ACTION_CODE.LOAD_PARAMS);
						
						/*DANH SACH THAM SO*/
						MsgParam msgParam = null;
						MsgParams msgParams = new MsgParams();
						
						msgParam = new MsgParam();
						msgParam.setId("param01");
						msgParam.setParam("DMMauSoKyHieu");
						msgParams.getParams().add(msgParam);
						
						/*END: DANH SACH THAM SO*/
						msg.setObjData(msgParams);
						
						JSONRoot root = new JSONRoot(msg);
						MsgRsp rsp = restAPI.callAPINormal("/commons/get-full-params", cup.getLoginRes().getToken(), HttpMethod.POST, root);
						MspResponseStatus rspStatus = rsp.getResponseStatus();
						
						if(rspStatus.getErrorCode() == 0 && rsp.getObjData() != null) {
							LinkedHashMap<String, String> hItem = null;
							
							JsonNode jsonData = Json.serializer().nodeFromObject(rsp.getObjData());
						String KHHDon = "";
							if(null != jsonData.at("/param01") && jsonData.at("/param01") instanceof ArrayNode) {
								hItem = new LinkedHashMap<String, String>();
								for(JsonNode o: jsonData.at("/param01")) {													
									KHHDon = commons.getTextJsonNode(o.get("KHHDon"));	
									char words=KHHDon.charAt(KHHDon.length() - 3);
									String s=String.valueOf(words);  
									if("M".equals(s)) {
										hItem.put(commons.getTextJsonNode(o.get("_id")), commons.getTextJsonNode(o.get("KHMSHDon")) + commons.getTextJsonNode(o.get("KHHDon")));	
									}						
							}
								req.setAttribute("map_mausokyhieu", hItem);
							}
							
						}
						
					}catch(Exception e) {}
				}
				
				
				@RequestMapping(value = {
						"/common/show-search_mtt-dshddky"
				}, method = {RequestMethod.POST})
				public String showSearchMTTDSHDDKy(Locale locale, HttpServletRequest req, HttpSession session) throws Exception{
					req.setAttribute("_header_", "Danh sách hóa đơn đã ký");
					
					LocalDate now = LocalDate.now();
					req.setAttribute("FromDate", commons.convertLocalDateTimeToString(now.with(ChronoField.DAY_OF_MONTH, 1), Constants.FORMAT_DATE.FORMAT_DATE_WEB));
					req.setAttribute("ToDate", commons.convertLocalDateTimeToString(now, Constants.FORMAT_DATE.FORMAT_DATE_WEB));
					
					CurrentUserProfile cup = getCurrentlyAuthenticatedPrincipal();

					LoadParameterFor_DSHDDKyMTT(cup, locale, req, "DETAIL");
					return "common/search_mtt-dshddky";
				}
				
				
				@RequestMapping(value = "/common/list-einvoice_mtt-signed",  produces = MediaType.APPLICATION_JSON_VALUE, method = RequestMethod.POST)
				@ResponseBody
				public BaseDTO listEInvoiceMTTSigned(Locale locale, HttpServletRequest req, HttpSession session) throws Exception{
					JsonGridDTO grid = new JsonGridDTO();
					
					BaseDTO baseDTO = checkDataSearchEInvoiceSigned(locale, req, session);
					if(0 != baseDTO.getErrorCode()) {
						grid.setErrorCode(baseDTO.getErrorCode());
						grid.setErrorMessages(baseDTO.getErrorMessages());
						grid.setResponseData(Constants.MAP_ERROR.get(999));
						return grid;
					}
					
					CurrentUserProfile cup = getCurrentlyAuthenticatedPrincipal();		
					baseDTO = new BaseDTO(req);
					Msg msg = baseDTO.createMsg(cup, Constants.MSG_ACTION_CODE.SEARCH);
					
					HashMap<String, Object> hData = new HashMap<>();
					hData.put("MauSoHdon", mauSoHdon);
					hData.put("SoHoaDon", soHoaDon);
					hData.put("FromDate", fromDate);
					hData.put("ToDate", toDate);
					hData.put("NbanMst", nbanMst);
					hData.put("NbanTen", nbanTen);
							
					msg.setObjData(hData);
					
					JSONRoot root = new JSONRoot(msg);
					MsgRsp rsp = restAPI.callAPINormal("/commons/list-einvoice_mtt-signed", cup.getLoginRes().getToken(), HttpMethod.POST, root);
					MspResponseStatus rspStatus = rsp.getResponseStatus();
					if(rspStatus.getErrorCode() == 0) {
						MsgPage page = rsp.getMsgPage();
						grid.setTotal(page.getTotalRows());
						
						JsonNode jsonData = Json.serializer().nodeFromObject(rsp.getObjData());
						JsonNode rows = null;
						HashMap<String, String> hItem = null;
						if(!jsonData.at("/rows").isMissingNode()) {
							rows = jsonData.at("/rows");
							for(JsonNode row: rows) {
								hItem = new HashMap<String, String>();
								
								hItem.put("_id", commons.getTextJsonNode(row.at("/_id")));
								
								hItem.put("SignStatusCode", commons.getTextJsonNode(row.at("/SignStatusCode")));
								hItem.put("SignStatusDesc", Constants.MAP_EINVOICE_SIGN_STATUS.get(commons.getTextJsonNode(row.at("/SignStatusCode"))));
								
								hItem.put("EInvoiceStatus", commons.getTextJsonNode(row.at("/EInvoiceStatus")));
								hItem.put("MCCQT", commons.getTextJsonNode(row.at("/MCCQT")));
								hItem.put("StatusDesc", Constants.MAP_EINVOICE_STATUS.get(commons.getTextJsonNode(row.at("/EInvoiceStatus"))));
								hItem.put("CQTMTLoi", commons.getTextJsonNode(row.at("/LDo/MTLoi")));
								
								hItem.put("MauSoHD", 
									commons.getTextJsonNode(row.at("/EInvoiceDetail/TTChung/KHMSHDon")) + commons.getTextJsonNode(row.at("/EInvoiceDetail/TTChung/KHHDon"))
								);
								hItem.put("EInvoiceNumber", 
									"".equals(commons.getTextJsonNode(row.at("/EInvoiceDetail/TTChung/SHDon")))? "":
									commons.formatNumberBillInvoice(commons.getTextJsonNode(row.at("/EInvoiceDetail/TTChung/SHDon")))
								);
								hItem.put("NLap", 
									commons.convertLocalDateTimeToString(commons.convertLongToLocalDate(row.at("/EInvoiceDetail/TTChung/NLap").asLong()), Constants.FORMAT_DATE.FORMAT_DATE_WEB)
								);
								hItem.put("TaxCode", commons.getTextJsonNode(row.at("/EInvoiceDetail/NDHDon/NMua/MST")));
								hItem.put("CompanyName", commons.getTextJsonNode(row.at("/EInvoiceDetail/NDHDon/NMua/Ten")));
								hItem.put("TgTTTBSo", 
									row.at("/EInvoiceDetail/TToan/TgTTTBSo").isMissingNode()? "":
									commons.formatNumberReal(row.at("/EInvoiceDetail/TToan/TgTTTBSo").doubleValue())
								);
								hItem.put("TgTCThue", 
									row.at("/EInvoiceDetail/TToan/TgTCThue").isMissingNode()? "":
									commons.formatNumberReal(row.at("/EInvoiceDetail/TToan/TgTCThue").doubleValue())
								);
								hItem.put("TgTThue", 
									row.at("/EInvoiceDetail/TToan/TgTThue").isMissingNode()? "":
									commons.formatNumberReal(row.at("/EInvoiceDetail/TToan/TgTThue").doubleValue())
								);
								hItem.put("HVTNMHang", commons.getTextJsonNode(row.at("/EInvoiceDetail/NDHDon/NMua/HVTNMHang")));
								hItem.put("UserCreated", commons.getTextJsonNode(row.at("/InfoCreated/CreateUserFullName")));
								
								grid.getRows().add(hItem);
							}
						}
					}else {
						grid = new JsonGridDTO();
						grid.setErrorCode(rspStatus.getErrorCode());
						grid.setResponseData(rspStatus.getErrorDesc());
					}
					
					return grid;
				}

}