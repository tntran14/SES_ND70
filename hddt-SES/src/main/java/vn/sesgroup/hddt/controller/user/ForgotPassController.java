package vn.sesgroup.hddt.controller.user;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.URLEncoder;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Locale;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.xml.bind.DatatypeConverter;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.context.WebApplicationContext;

import com.api.message.JSONRoot;
import com.api.message.Msg;
import com.api.message.MsgRsp;
import com.api.message.MspResponseStatus;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;

import vn.sesgroup.hddt.controller.AbstractController;
import vn.sesgroup.hddt.dto.BaseDTO;
import vn.sesgroup.hddt.dto.CurrentUserProfile;
import vn.sesgroup.hddt.dto.FileInfo;
import vn.sesgroup.hddt.dto.IssuerInfo;
import vn.sesgroup.hddt.resources.RestAPIUtility;
import vn.sesgroup.hddt.utils.Commons;
import vn.sesgroup.hddt.utils.Constants;
import vn.sesgroup.hddt.utils.Json;

@Controller
@Scope(value = WebApplicationContext.SCOPE_REQUEST)
@RequestMapping({
	"/forgotpass"
})
public class ForgotPassController extends AbstractController{
	private static final Logger log = LogManager.getLogger(ForgotPassController.class);
	@Autowired RestAPIUtility restAPI;
	@Autowired RestTemplate restTemplate;
	
	private String rp_username;
	private String rp_email;
	private String codeCaptcha;
	private String tk_username;
	private String certificate;
	
	@RequestMapping(value = {"/", ""})
	public String forgotpass(Model model, HttpServletRequest req) throws Exception {
		req.setAttribute("_header", Constants.PREFIX_TITLE + " - QUÊN MẬT KHẨU");
		BaseDTO dtoRes = new BaseDTO();
		Msg msg = dtoRes.createMsgPass();
		HashMap<String, String> hInput = new HashMap<>();
		msg.setObjData(hInput);
		JSONRoot root = new JSONRoot(msg);
		MsgRsp rsp = restAPI.callAPIPass("/forgotpass/dl", HttpMethod.POST, root);
		MspResponseStatus rspStatus = rsp.getResponseStatus();
		JsonNode rows = null;
		if(rspStatus.getErrorCode() == 0) {
			JsonNode jsonData = Json.serializer().nodeFromObject(rsp.getObjData());
			if(!jsonData.at("/rows").isMissingNode()) {
				rows = jsonData.at("/rows");
			for(JsonNode row: rows) {
				req.setAttribute("LOGO", commons.getTextJsonNode(row.at("/LOGO")));
				req.setAttribute("PHONE", commons.getTextJsonNode(row.at("/PHONE")));
				req.setAttribute("EMAIL", commons.getTextJsonNode(row.at("/EMAIL")));
			}
			}
		}
		return "user/forgetpass";
	}
	
	public BaseDTO checkDataToAccept(Locale locale, HttpServletRequest req, HttpSession session) throws Exception{
		BaseDTO dto = new BaseDTO();
		dto.setErrorCode(0);
		
		rp_username = null == req.getParameter("rp_username")? "": req.getParameter("rp_username").replaceAll("\\s", "");
		rp_email = null == req.getParameter("rp_email")? "": req.getParameter("rp_email").replaceAll("\\s", "");	
		codeCaptcha = null == req.getParameter("codeCaptcha")? "": req.getParameter("codeCaptcha").trim();

		if("".equals(rp_username)) {
			dto.setErrorCode(1);
			dto.getErrorMessages().add("Vui lòng nhập vào tên đăng nhập.");
		}
		if("".equals(rp_email)) {
			dto.setErrorCode(1);
			dto.getErrorMessages().add("Vui lòng nhập vào email người dùng.");
		}
		if(dto.getErrorCode() == 0) {
			String captchaSession = null == session.getAttribute(Constants.SESSION_CAPTCHA)? "": (String) session.getAttribute(Constants.SESSION_CAPTCHA);
			if("".equals(codeCaptcha) || !codeCaptcha.equals(captchaSession)) {
				dto.setErrorCode(1);
				dto.setResponseData("Mã captcha không đúng. Vui lòng nhập lại.");
			}
		}
		
		return dto;
	}
	
	
	
	@RequestMapping(value = "/checkDataToSend",  produces = MediaType.APPLICATION_JSON_VALUE, method = RequestMethod.POST)
	@ResponseBody
	public BaseDTO execCheckDataToSave(Locale locale, HttpServletRequest req, HttpSession session){
		BaseDTO dto = new BaseDTO();
		try {
			dto = checkDataToAccept(locale, req, session);
			if(0 != dto.getErrorCode()) {
				if(null != dto.getResponseData() && !"".equals(dto.getResponseData())) {
				}else {
					dto.setErrorCode(999);
					dto.setResponseData(Constants.MAP_ERROR.get(999));
					return dto;
				}
				
				return dto;
			}
			
			dto.setErrorCode(0);
		}catch(Exception e) {
			dto.setErrorCode(999);
			dto.setResponseData(Constants.MAP_ERROR.get(999));
			return dto;
		}
		
		return dto;
	}
	
	
	
	@RequestMapping(value = {"/getToken"},  produces = MediaType.APPLICATION_JSON_VALUE, method = {RequestMethod.POST})
	@ResponseBody
	public BaseDTO execGetToken(Locale locale, HttpServletRequest req, HttpSession session) {
				
		BaseDTO dtoRes = new BaseDTO();		
			try {		
			dtoRes = checkDataToAccept(locale, req, session);
			if(0 != dtoRes.getErrorCode()) {
				if(null != dtoRes.getResponseData() && !"".equals(dtoRes.getResponseData())) {
				}else {
					dtoRes.setErrorCode(999);
					dtoRes.setResponseData(Constants.MAP_ERROR.get(999));
					return dtoRes;
				}
				return dtoRes;
			}

		dtoRes = new BaseDTO(req);
		Msg msg = dtoRes.createMsgPass();
		HashMap<String, String> hData = new HashMap<>();
		hData.put("Username", rp_username);
		hData.put("Email", rp_email);

		msg.setObjData(hData);
		JSONRoot root = new JSONRoot(msg);
		MsgRsp rsp = restAPI.callAPIPass("/forgotpass/crud", HttpMethod.POST, root);
		MspResponseStatus rspStatus = rsp.getResponseStatus();
		if(rspStatus.getErrorCode() == 0) {
			dtoRes.setErrorCode(0);			
			dtoRes.setResponseData(rspStatus.getErrorDesc());
		}else {
			dtoRes.setErrorCode(rspStatus.getErrorCode());
			dtoRes.setResponseData(rspStatus.getErrorDesc());
		}
		}catch(Exception e) {
			dtoRes.setErrorCode(999);
			dtoRes.setResponseData(Constants.MAP_ERROR.get(999));
			return dtoRes;
		}
		return dtoRes;	
	}
	
	@RequestMapping(value = "/confirmPassword", method = {RequestMethod.POST})
	public String confirmPassword(Locale locale, HttpServletRequest req, HttpSession session) {
		req.setAttribute("_header_", "Xác nhận lấy lại mật khẩu");
		req.setAttribute("tokenConfirm", req.getParameter("tokenConfirm"));		
		return "user/resetPasswordConfirm";
	}
	
	private String otpConfirm;
	private String tokenConfirm;
	
	public BaseDTO checkDataToConfirm(Locale locale, HttpServletRequest req, HttpSession session) throws Exception{
		BaseDTO dto = new BaseDTO();
		dto.setErrorCode(0);
		
		otpConfirm = null == req.getParameter("otpConfirm")? "": req.getParameter("otpConfirm").replaceAll("\\s", "");
		tokenConfirm = null == req.getParameter("tokenConfirm")? "": req.getParameter("tokenConfirm").replaceAll("\\s", "");
		
		if("".equals(otpConfirm)) {
			dto.setErrorCode(1);
			dto.getErrorMessages().add("Vui lòng nhập vào mã xác thực.");
		}
		if("".equals(tokenConfirm)) {
			dto.setErrorCode(1);
			dto.getErrorMessages().add("Token xác nhận mật khẩu không hợp lệ.");
		}
		
		return dto;
	}
	
	@RequestMapping(value = "/checkDataToConfirm",  produces = MediaType.APPLICATION_JSON_VALUE, method = RequestMethod.POST)
	@ResponseBody
	public BaseDTO execCheckDataToConfirm(Locale locale, HttpServletRequest req, HttpSession session) {
		BaseDTO dto = new BaseDTO();
		try {
			dto = checkDataToConfirm(locale, req, session);
			if(0 != dto.getErrorCode()) {
				if(null != dto.getResponseData() && !"".equals(dto.getResponseData())) {
				}else {
					dto.setErrorCode(999);
					dto.setResponseData(Constants.MAP_ERROR.get(999));
					return dto;
				}
				
				return dto;
			}
			
			dto.setErrorCode(0);
		}catch(Exception e) {
			dto.setErrorCode(999);
			dto.setResponseData(Constants.MAP_ERROR.get(999));
			return dto;
		}
		return dto;
	}
	
	@RequestMapping(value = "/getPassword",  produces = MediaType.APPLICATION_JSON_VALUE, method = RequestMethod.POST)
	@ResponseBody
	public BaseDTO execGetPassword(Locale locale, HttpServletRequest req, HttpSession session) {
		BaseDTO dtoRes = new BaseDTO();
		try {
			dtoRes = checkDataToConfirm(locale, req, session);
			if(0 != dtoRes.getErrorCode()) {
				if(null != dtoRes.getResponseData() && !"".equals(dtoRes.getResponseData())) {
				}else {
					dtoRes.setErrorCode(999);
					dtoRes.setResponseData(Constants.MAP_ERROR.get(999));
					return dtoRes;
				}
				return dtoRes;
			}
			
			dtoRes = new BaseDTO(req);
			Msg msg = dtoRes.createMsgPass();

			HashMap<String, String> hInput = new HashMap<>();
			hInput.put("token", tokenConfirm);
			hInput.put("otp", otpConfirm);

			msg.setObjData(hInput);
			JSONRoot root = new JSONRoot(msg);
			MsgRsp rsp = restAPI.callAPIPass("/forgotpass/check-token", HttpMethod.POST, root);
			MspResponseStatus rspStatus = rsp.getResponseStatus();
			if(rspStatus.getErrorCode() == 0) {
				dtoRes.setErrorCode(0);			
				dtoRes.setResponseData("Lấy mật khẩu thành công. <br/>Vui lòng kiểm tra Email để lấy mật khẩu mới.");
			}else {
				dtoRes.setErrorCode(rspStatus.getErrorCode());
				dtoRes.setResponseData(rspStatus.getErrorDesc());
			}					
			
		}catch(Exception e) {
			dtoRes.setErrorCode(999);
			dtoRes.setResponseData(Constants.MAP_ERROR.get(999));
			return dtoRes;
		}
		return dtoRes;
	}
	/*TRUONG HOP DUNG TOKEN*/
	public BaseDTO checkDataToAcceptTK(Locale locale, HttpServletRequest req, HttpSession session) throws Exception{
		BaseDTO dto = new BaseDTO();
		dto.setErrorCode(0);
		
		tk_username = null == req.getParameter("tk_username")? "": req.getParameter("tk_username").replaceAll("\\s", "").toUpperCase();
		codeCaptcha = null == req.getParameter("codeCaptcha")? "": req.getParameter("codeCaptcha").trim();

		if("".equals(tk_username)) {
			dto.setErrorCode(1);
			dto.getErrorMessages().add("Vui lòng nhập vào tên đăng nhập.");
		}
		
		if(dto.getErrorCode() == 0) {
			String captchaSession = null == session.getAttribute(Constants.SESSION_CAPTCHA)? "": (String) session.getAttribute(Constants.SESSION_CAPTCHA);
			session.removeAttribute(Constants.SESSION_CAPTCHA);
			if("".equals(codeCaptcha) || !codeCaptcha.equals(captchaSession)) {
				dto.setErrorCode(1);
				dto.setResponseData("Mã kiểm tra không đúng. Vui lòng nhập lại mã kiểm tra.");
			}
		}
		
		return dto;
	}
	@RequestMapping(value = "/checkDataToSendTK",  produces = MediaType.APPLICATION_JSON_VALUE, method = RequestMethod.POST)
	@ResponseBody
	public BaseDTO execCheckDataToSaveTK(Locale locale, HttpServletRequest req, HttpSession session) {
		BaseDTO dto = new BaseDTO();
		try {
			dto = checkDataToAcceptTK(locale, req, session);
			if(0 != dto.getErrorCode()) {
				if(null != dto.getResponseData() && !"".equals(dto.getResponseData())) {
				}else {
					dto.setResponseData("Thông tin nhập vào chưa đúng. Vui lòng kiểm tra lại.");	
				}
				
				return dto;
			}
			
			String token = commons.csRandomAlphaNumbericString(50);
			session.setAttribute(Constants.SESSION_TYPE.SESSION_TOKEN_EXECUTE, token);
			dto.setResponseData(token);
			dto.setErrorCode(0);
		}catch(Exception e) {
			dto.setErrorCode(1);
			dto.setResponseData("Lỗi đã xảy ra.");
		}
		return dto;
	}
	
	@RequestMapping(value = "/changePassTK",  produces = MediaType.APPLICATION_JSON_VALUE, method = RequestMethod.POST)
	@ResponseBody
	public BaseDTO execChangePassTK(Locale locale, HttpServletRequest req, HttpSession session) throws Exception{
		BaseDTO dtoRes = new BaseDTO();
		
//		dtoRes = checkDataToAcceptTK(locale, req, session);
		tk_username = null == req.getParameter("tk_username")? "": req.getParameter("tk_username").replaceAll("\\s", "").toUpperCase();
		certificate = commons.getParameterFromRequest(req, "certificate").replace("@", "+");
		if("".equals(tk_username)) {
			dtoRes.setErrorCode(1);
			dtoRes.getErrorMessages().add("Vui lòng nhập vào tên đăng nhập.");
		}
		if("".equals(certificate)) {
			dtoRes.setErrorCode(1);
			dtoRes.getErrorMessages().add("Không tìm thấy thông tin chứng thư số.");
		}
		
		if(0 != dtoRes.getErrorCode()) {
			dtoRes.setResponseData("Thông tin nhập vào chưa đúng. Vui lòng kiểm tra lại.");
			return dtoRes;
		}
		
		String vToken = null == req.getParameter("vToken")? "": req.getParameter("vToken");
		String vTokenSession = null == session.getAttribute(Constants.SESSION_TYPE.SESSION_TOKEN_EXECUTE)? "": session.getAttribute(Constants.SESSION_TYPE.SESSION_TOKEN_EXECUTE).toString();
		session.removeAttribute(Constants.SESSION_TYPE.SESSION_TOKEN_EXECUTE);
		if("".equals(vToken) || !vToken.equals(vTokenSession)) {
			dtoRes.setErrorCode(1);
			dtoRes.setResponseData("Phiên giao dịch không hợp lệ.");
			return dtoRes;
		}
//		//LAY THONG TIN CERT - KIEM TRA SAU
//				CertificateFactory certificateFactory = null;
//				InputStream in = null;
//				X509Certificate x509Cert = null;
//				
//				String cert= null;
//				
//				certificateFactory = CertificateFactory.getInstance("X.509");
//				cert = certificate.replaceAll("@", "+");
//				in = new ByteArrayInputStream(DatatypeConverter.parseBase64Binary(cert));
//				x509Cert = (X509Certificate) certificateFactory.generateCertificate(in);
//				
//				String serialNumber = x509Cert.getSerialNumber().toString(16);
//				if(serialNumber.length() % 2 == 1)
//					serialNumber = "0" + serialNumber;						
//		///
		dtoRes = new BaseDTO(req);
		Msg msg = dtoRes.createMsgPass();

		HashMap<String, String> hInput = new HashMap<>();
		hInput.put("tk_username", tk_username);
//		hInput.put("cert", serialNumber);
		hInput.put("certificate", certificate);

		msg.setObjData(hInput);
		JSONRoot root = new JSONRoot(msg);
		MsgRsp rsp = restAPI.callAPIPass("/forgotpass/confirmResetPasswordWithToken", HttpMethod.POST, root);
		MspResponseStatus rspStatus = rsp.getResponseStatus();
		if(0 == rspStatus.getErrorCode()) {
			dtoRes.setErrorCode(0);
			HashMap<String, String> hR = new HashMap<String, String>();
			hR.put("info", "Lấy mật khẩu thành công. Vui lòng copy mật khẩu mới để thực hiện giao dịch.");
			hR.put("secureKey", rspStatus.getErrorDesc());
			dtoRes.setResponseData(hR);
		}else {
			dtoRes.setErrorCode(1);
			dtoRes.setResponseData(rspStatus.getErrorDesc());
			return dtoRes;
		}
		
		return dtoRes;
	}
	
}
