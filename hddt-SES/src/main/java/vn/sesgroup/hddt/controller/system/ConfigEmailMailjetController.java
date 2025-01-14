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
import vn.sesgroup.hddt.resources.RestAPIUtility;
import vn.sesgroup.hddt.utils.Constants;
import vn.sesgroup.hddt.utils.Json;

@Controller
@RequestMapping("/config-email-mailjet")
@Scope(value = WebApplicationContext.SCOPE_REQUEST)
public class ConfigEmailMailjetController extends AbstractController{
	private static final Logger log = LogManager.getLogger(ConfigEmailMailjetController.class);
	@Autowired RestAPIUtility restAPI; 
	
	private String errorCode;
	private String errorDesc;
	private String nch;
	private String md;
	private String lm;
	private String ak;
	private String sk;
	private String ng;
	private String am;

	
	@RequestMapping(value = "/init", method = {RequestMethod.POST, RequestMethod.GET})
	public String init(Locale locale, Principal principal, HttpServletRequest req
			, HttpSession session ) throws Exception{
		req.setAttribute("_TitleView_", Constants.PREFIX_TITLE + " - Cấu hình email server");
		CurrentUserProfile cup = getCurrentlyAuthenticatedPrincipal();
		
		req.setAttribute("nch", "vdc-service vdc.net.vn - Mailjet");
		req.setAttribute("lm", "Mailjet");
		inquiry(cup, locale, req, session, "");
		
		return "system/config-email-mailjet";
	}
	
	private void inquiry(CurrentUserProfile cup, Locale locale, HttpServletRequest req, HttpSession session, String action) throws Exception{
		BaseDTO baseDTO = new BaseDTO(req);
		Msg msg = baseDTO.createMsg(cup, Constants.MSG_ACTION_CODE.INQUIRY);
		HashMap<String, String> hData = new HashMap<>();
		msg.setObjData(hData);
		
		JSONRoot root = new JSONRoot(msg);
		MsgRsp rsp = restAPI.callAPINormal("/config-email-mailjet/detail", cup.getLoginRes().getToken(), HttpMethod.POST, root);
		MspResponseStatus rspStatus = rsp.getResponseStatus();
		if(rspStatus.getErrorCode() == 0) {
			JsonNode jsonData = Json.serializer().nodeFromObject(rsp.getObjData());
			
			req.setAttribute("nch", commons.getTextJsonNode(jsonData.at("/NameConfig")));
			req.setAttribute("md", commons.getTextJsonNode(jsonData.at("/Default")));
			req.setAttribute("lm", commons.getTextJsonNode(jsonData.at("/LoaiMail")));
			req.setAttribute("ak", commons.getTextJsonNode(jsonData.at("/ApiKey")));
			req.setAttribute("sk", commons.getTextJsonNode(jsonData.at("/SecretKey")));
			req.setAttribute("ng", commons.getTextJsonNode(jsonData.at("/NameSend")));
			req.setAttribute("am", commons.getTextJsonNode(jsonData.at("/EmailAddress")));
		}
	}
	
	public BaseDTO checkDataToAccept(HttpServletRequest req, HttpSession session, String transaction
			, CurrentUserProfile cup) throws Exception{
		BaseDTO dto = new BaseDTO();
		dto.setErrorCode(0);
		
		nch = commons.getParameterFromRequest(req, "nch").replaceAll("\\s", "");
		md = commons.getParameterFromRequest(req, "md").replaceAll("\\s", "");
		lm = commons.getParameterFromRequest(req, "lm").replaceAll("\\s", "");
		ak = commons.getParameterFromRequest(req, "ak").replaceAll("\\s", "");
		sk = commons.getParameterFromRequest(req, "sk").replaceAll("\\s", "");
		ng = commons.getParameterFromRequest(req, "ng").replaceAll("\\s", "");
		am = commons.getParameterFromRequest(req, "am").replaceAll("\\s", "").toLowerCase();


		if("".equals(nch)) {
			dto.setErrorCode(1);
			dto.getErrorMessages().add("Vui lòng nhập vào tên cấu hình.");
		}
		if("".equals(ng)) {
			dto.setErrorCode(1);
			dto.getErrorMessages().add("Vui lòng nhập vào tên người gửi.");
		}
		if("".equals(ak)) {
			dto.setErrorCode(1);
			dto.getErrorMessages().add("Vui lòng nhập vào Api key.");
		}
		if("".equals(sk)) {
			dto.setErrorCode(1);
			dto.getErrorMessages().add("Vui lòng nhập vào SecretKey.");
		}
		if("".equals(am)) {
			dto.setErrorCode(1);
			dto.getErrorMessages().add("Vui lòng nhập vào địa chỉ email gửi.");
		}else if(!commons.isValidEmailAddress(am)) {
			dto.setErrorCode(1);
			dto.getErrorMessages().add("Địa chỉ email gửi không đúng định dạng.");
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
		String messageConfirm = "Bạn có muốn cấu hình email mailjet không?";
		
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
		hData.put("NameConfig", nch);
		hData.put("Default", md);
		hData.put("LoaiMail", lm);
		hData.put("ApiKey", ak);
		hData.put("SecretKey", sk);
		hData.put("NameSend", ng);
		hData.put("EmailAddress", am);

		msg.setObjData(hData);
		JSONRoot root = new JSONRoot(msg);
		MsgRsp rsp = restAPI.callAPINormal("/config-email-mailjet/crud", cup.getLoginRes().getToken(), HttpMethod.POST, root);
		MspResponseStatus rspStatus = rsp.getResponseStatus();
		if(rspStatus.getErrorCode() == 0) {
			dtoRes.setErrorCode(0);
			dtoRes.setResponseData("Cấu hình email server thành công.");
		}else {
			dtoRes.setErrorCode(rspStatus.getErrorCode());
			dtoRes.setResponseData(rspStatus.getErrorDesc());
		}
		return dtoRes;
	}
	
}
