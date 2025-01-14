package vn.sesgroup.hddt.controller.system;

import java.security.Principal;
import java.util.HashMap;
import java.util.Locale;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.context.WebApplicationContext;

import com.api.message.JSONRoot;
import com.api.message.Msg;
import com.api.message.MsgRsp;
import com.api.message.MspResponseStatus;
import com.fasterxml.jackson.databind.JsonNode;
import com.lowagie.text.Header;

import vn.sesgroup.hddt.controller.AbstractController;
import vn.sesgroup.hddt.dto.BaseDTO;
import vn.sesgroup.hddt.dto.CurrentUserProfile;
import vn.sesgroup.hddt.dto.LoginRes;
import vn.sesgroup.hddt.resources.RestAPIUtility;
import vn.sesgroup.hddt.utils.Constants;
import vn.sesgroup.hddt.utils.Json;

@Controller
@RequestMapping("/config-email-server-admin")
@Scope(value = WebApplicationContext.SCOPE_REQUEST)
public class ConfigEmailServerAdminController extends AbstractController{
	private static final Logger log = LogManager.getLogger(ConfigEmailServerAdminController.class);
	@Autowired RestAPIUtility restAPI; 
	
	private String errorCode;
	private String errorDesc;
	private String _id;
	private String checkAutoSend;
	private String checkSSL;
	private String checkTLS;
	private String smtpServer;
	private String smtpPort;
	private String emailAddress;
	private String emailPassword;
	
	@RequestMapping(value = "/init", method = {RequestMethod.POST, RequestMethod.GET})
	public String init(Locale locale, Principal principal, HttpServletRequest req
			, HttpSession session ) throws Exception{
		req.setAttribute("_TitleView_", Constants.PREFIX_TITLE + " - Cấu hình email server Admin");
		CurrentUserProfile cup = getCurrentlyAuthenticatedPrincipal();
		
		req.setAttribute("SmtpPort", "465");
		
		inquiry(cup, locale, req, session, "");
		LoginRes issu = cup.getLoginRes();
		String _id = issu.getIssuerId();
		
		BaseDTO baseDTO = new BaseDTO(req);
		Msg msg = baseDTO.createMsg(cup, Constants.MSG_ACTION_CODE.INQUIRY);
		HashMap<String, String> hData = new HashMap<>();
		msg.setObjData(hData);
		JSONRoot root = new JSONRoot(msg);
		MsgRsp rsp = restAPI.callAPINormal("/main/profile/" + _id, cup.getLoginRes().getToken(), HttpMethod.POST, root);
		JsonNode jsonData = Json.serializer().nodeFromObject(rsp.getObjData());
		String isroot = commons.getTextJsonNode(jsonData.at("/IsRoot"));
		
		if("true".equals(isroot) && issu.isRoot() == true && issu.isAdmin() == true)
		{	return "system/config-email-server-admin";}
		else {
			return "/admin/admin";
		}
	
	}
	
	private void inquiry(CurrentUserProfile cup, Locale locale, HttpServletRequest req, HttpSession session, String action) throws Exception{
		BaseDTO baseDTO = new BaseDTO(req);
		Msg msg = baseDTO.createMsg(cup, Constants.MSG_ACTION_CODE.INQUIRY);
		HashMap<String, String> hData = new HashMap<>();
		msg.setObjData(hData);
		
		JSONRoot root = new JSONRoot(msg);
		MsgRsp rsp = restAPI.callAPINormal("/config-email-server-admin/detail", cup.getLoginRes().getToken(), HttpMethod.POST, root);
		MspResponseStatus rspStatus = rsp.getResponseStatus();
		if(rspStatus.getErrorCode() == 0) {
			JsonNode jsonData = Json.serializer().nodeFromObject(rsp.getObjData());
			
			if(!jsonData.at("/AutoSend").isMissingNode())
				req.setAttribute("CheckAutoSend", jsonData.at("/AutoSend").asBoolean(false));
			if(!jsonData.at("/SSL").isMissingNode())
				req.setAttribute("CheckSSL", jsonData.at("/SSL").asBoolean(false));
			if(!jsonData.at("/TLS").isMissingNode())
				req.setAttribute("CheckTLS", jsonData.at("/TLS").asBoolean(false));
			req.setAttribute("SmtpServer", commons.getTextJsonNode(jsonData.at("/SmtpServer")));
			req.setAttribute("SmtpPort", commons.getTextJsonNode(jsonData.at("/SmtpPort")));
			req.setAttribute("EmailAddress", commons.getTextJsonNode(jsonData.at("/EmailAddress")));
			
			req.setAttribute("_id", commons.getTextJsonNode(jsonData.at("/IssuerId")));
		}
	}
	
	public BaseDTO checkDataToAccept(HttpServletRequest req, HttpSession session, String transaction
			, CurrentUserProfile cup) throws Exception{
		BaseDTO dto = new BaseDTO();
		dto.setErrorCode(0);
		
		_id = commons.getParameterFromRequest(req, "_id").replaceAll("\\s", "");
		checkAutoSend = commons.getParameterFromRequest(req, "check-auto-send").replaceAll("\\s", "");
		checkSSL = commons.getParameterFromRequest(req, "check-ssl").replaceAll("\\s", "");
		checkTLS = commons.getParameterFromRequest(req, "check-tls").replaceAll("\\s", "");
		smtpServer = commons.getParameterFromRequest(req, "smtp-server").replaceAll("\\s", "");
		smtpPort = commons.getParameterFromRequest(req, "smtp-port").replaceAll("\\s", "");
		emailAddress = commons.getParameterFromRequest(req, "email-address").replaceAll("\\s", "").toLowerCase();
		emailPassword = commons.getParameterFromRequest(req, "email-password");
		
		if("".equals(smtpServer)) {
			dto.setErrorCode(1);
			dto.getErrorMessages().add("Vui lòng nhập vào địa chỉ SMTP Server.");
		}
		if("".equals(smtpPort) || !commons.checkStringIsInt(smtpPort) || commons.stringToInteger(smtpPort) <= 0) {
			dto.setErrorCode(1);
			dto.getErrorMessages().add("Vui lòng kiểm tra lại Port SMTP Server.");
		}
		if("".equals(emailAddress)) {
			dto.setErrorCode(1);
			dto.getErrorMessages().add("Vui lòng nhập vào địa chỉ email gửi.");
		}else if(!commons.isValidEmailAddress(emailAddress)) {
			dto.setErrorCode(1);
			dto.getErrorMessages().add("Địa chỉ email gửi không đúng định dạng.");
		}
		if("".equals(_id) && "".equals(emailPassword)) {
			dto.setErrorCode(1);
			dto.getErrorMessages().add("Vui lòng nhập vào mật khẩu email gửi.");
		}
		
		return dto;
	}
	
	@RequestMapping(value = "/check-data-save",  produces = MediaType.APPLICATION_JSON_VALUE, method = RequestMethod.POST)
	@ResponseBody
	public BaseDTO execCheckDataToSave(Locale locale, HttpServletRequest req, HttpSession session
			, @RequestAttribute(name = "transaction", required = false, value = "") String transaction) throws Exception {
		String token = "";
		if (null != session.getAttribute(Constants.SESSION_TYPE.SESSION_TOKEN_EXECUTE)) {
			token = session.getAttribute(Constants.SESSION_TYPE.SESSION_TOKEN_EXECUTE).toString();
			session.removeAttribute(token);
		}
		session.removeAttribute(Constants.SESSION_TYPE.SESSION_TOKEN_EXECUTE);
		
		BaseDTO dto = new BaseDTO();
		String messageConfirm = "Bạn có muốn cấu hình email server không?";
		
		CurrentUserProfile cup = getCurrentlyAuthenticatedPrincipal();
		dto = checkDataToAccept(req, session, transaction, cup);
		if(0 != dto.getErrorCode()) {
			dto.setErrorCode(999);
			dto.setResponseData(Constants.MAP_ERROR.get(999));
			return dto;
		}
		
		token = commons.csRandomAlphaNumbericString(30);
		session.setAttribute(Constants.SESSION_TYPE.SESSION_TOKEN_EXECUTE, token);
		
		HashMap<String, String> hInfo = new HashMap<String, String>();
		hInfo.put("CONFIRM", messageConfirm);
		hInfo.put("TOKEN", token);
		
		dto.setResponseData(hInfo);
		dto.setErrorCode(0);
		return dto;
	}
	
	@RequestMapping(value = "/save-data",  produces = MediaType.APPLICATION_JSON_VALUE, method = RequestMethod.POST)
	@ResponseBody
	public BaseDTO execSaveData(HttpServletRequest req, HttpSession session
			, @RequestAttribute(name = "transaction", value = "", required = false) String transaction
			, @RequestParam(value = "tokenTransaction", required = false, defaultValue = "") String tokenTransaction) throws Exception{
		BaseDTO dtoRes = new BaseDTO();
		
		CurrentUserProfile cup = getCurrentlyAuthenticatedPrincipal();
		dtoRes = checkDataToAccept(req, session, transaction, cup);
		if(0 != dtoRes.getErrorCode()) {
			dtoRes.setErrorCode(999);
			dtoRes.setResponseData(Constants.MAP_ERROR.get(999));
			return dtoRes;
		}
		
		/*CHECK TOKEN*/
		String token = session.getAttribute(Constants.SESSION_TYPE.SESSION_TOKEN_EXECUTE) == null ? ""
				: session.getAttribute(Constants.SESSION_TYPE.SESSION_TOKEN_EXECUTE).toString();
		session.removeAttribute(Constants.SESSION_TYPE.SESSION_TOKEN_EXECUTE);
		if ("".equals(token) || !tokenTransaction.equals(token)) {
			dtoRes.setErrorCode(1);
			dtoRes.setResponseData("Token giao dịch không hợp lệ.");
			return dtoRes;
		}
		/*END: CHECK TOKEN*/
		
		String actionCode = Constants.MSG_ACTION_CODE.CREATED;
		dtoRes = new BaseDTO(req);
		Msg msg = dtoRes.createMsg(cup, actionCode);
		HashMap<String, Object> hData = new HashMap<>();
		hData.put("_id", _id);
		hData.put("CheckAutoSend", checkAutoSend);
		hData.put("CheckSSL", checkSSL);
		hData.put("CheckTLS", checkTLS);
		hData.put("SmtpServer", smtpServer);
		hData.put("SmtpPort", smtpPort);
		hData.put("EmailAddress", emailAddress);
		hData.put("EmailPassword", emailPassword);
		
		msg.setObjData(hData);
		JSONRoot root = new JSONRoot(msg);
		MsgRsp rsp = restAPI.callAPINormal("/config-email-server-admin/crud", cup.getLoginRes().getToken(), HttpMethod.POST, root);
		MspResponseStatus rspStatus = rsp.getResponseStatus();
		if(rspStatus.getErrorCode() == 0) {
			dtoRes.setErrorCode(0);
			dtoRes.setResponseData("Cấu hình email server admin thành công.");
		}else {
			dtoRes.setErrorCode(rspStatus.getErrorCode());
			dtoRes.setResponseData(rspStatus.getErrorDesc());
		}
		return dtoRes;
	}
	
}
