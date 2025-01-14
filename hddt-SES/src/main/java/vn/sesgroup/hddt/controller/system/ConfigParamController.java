package vn.sesgroup.hddt.controller.system;

import java.security.Principal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
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
import com.api.message.MsgParam;
import com.api.message.MsgParams;
import com.api.message.MsgRsp;
import com.api.message.MspResponseStatus;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;

import vn.sesgroup.hddt.controller.AbstractController;
import vn.sesgroup.hddt.dto.BaseDTO;
import vn.sesgroup.hddt.dto.CurrentUserProfile;
import vn.sesgroup.hddt.resources.RestAPIUtility;
import vn.sesgroup.hddt.utils.Constants;
import vn.sesgroup.hddt.utils.Json;

@Controller
@RequestMapping("/config-param")
@Scope(value = WebApplicationContext.SCOPE_REQUEST)
public class ConfigParamController extends AbstractController{
	private static final Logger log = LogManager.getLogger(ConfigParamController.class);
	@Autowired RestAPIUtility restAPI; 
	
	private String _id;
	private String VND;
	private String USD;
	private String viewshd;
	private String viewmoney;
	private String namecd;
	private String footermail;
	private String tax_invoice;

	private void LoadParameter(CurrentUserProfile cup, Locale locale, HttpServletRequest req, String action) {
		try {
			BaseDTO baseDTO = new BaseDTO(req);
			Msg msg = baseDTO.createMsg(cup, Constants.MSG_ACTION_CODE.LOAD_PARAMS);
			
			/*DANH SACH THAM SO*/
			HashMap<String, String> hashConds = null;
			ArrayList<HashMap<String, String>> conds = null;
			MsgParam msgParam = null;
			MsgParams msgParams = new MsgParams();
						
			msgParam = new MsgParam();
			msgParam.setId("param05");
			msgParam.setParam("TaxInvoice");
			msgParams.getParams().add(msgParam);
			
			
			/*END: DANH SACH THAM SO*/
			msg.setObjData(msgParams);
			
			JSONRoot root = new JSONRoot(msg);
			MsgRsp rsp = restAPI.callAPINormal("/commons/get-full-params", cup.getLoginRes().getToken(), HttpMethod.POST, root);
			MspResponseStatus rspStatus = rsp.getResponseStatus();
			
			
			if(rspStatus.getErrorCode() == 0 && rsp.getObjData() != null) {
				LinkedHashMap<String, String> hItem = null;
				LinkedHashMap<String, String> hItem1 = null;
				ArrayList<HashMap<String, String>> rows = null;
				ArrayList<HashMap<String, String>> rows1 = null;
				
			
				JsonNode jsonData = Json.serializer().nodeFromObject(rsp.getObjData());
				
				String TaxInvoice = "";
				String defaultTaxAdmin = "";
				String _taxInvoice = "";
				//boolean check_tax = false;
				if(null != jsonData.at("/param05") && jsonData.at("/param05") instanceof ArrayNode) {
				//	rows = new ArrayList<HashMap<String,String>>();
					hItem = new LinkedHashMap<String, String>();
					for(JsonNode o: jsonData.at("/param05")) {
						defaultTaxAdmin =  commons.getTextJsonNode(o.at("/DefaultAdmin"));
						if(defaultTaxAdmin.equals("true")) {
							_taxInvoice = commons.getTextJsonNode(o.at("/Code"));
						}
						hItem.put(commons.getTextJsonNode(o.get("Code")), commons.getTextJsonNode(o.get("Name")));
				//		rows.add(hItem);
				}		
					req.setAttribute("map_tax_invoice", hItem);
				}
				
				req.setAttribute("DefaultTaxAdmin", _taxInvoice);
			}
			
		}catch(Exception e) {}
	}
	
	
	
	@SuppressWarnings("unlikely-arg-type")
	@RequestMapping(value = "/init", method = {RequestMethod.POST, RequestMethod.GET})
	public String init(Locale locale, Principal principal, HttpServletRequest req
			, HttpSession session ) throws Exception{
		req.setAttribute("_TitleView_", Constants.PREFIX_TITLE + " - Cấu hình Tham số");
		CurrentUserProfile cup = getCurrentlyAuthenticatedPrincipal();	
		
		LoadParameter(cup, locale, req, "");
		
		BaseDTO baseDTO = new BaseDTO(req);
		Msg msg = baseDTO.createMsg(cup, Constants.MSG_ACTION_CODE.INQUIRY);
		HashMap<String, String> hData = new HashMap<>();
		msg.setObjData(hData);
		JSONRoot root = new JSONRoot(msg);
		MsgRsp rsp = restAPI.callAPINormal("/config-param/check", cup.getLoginRes().getToken(), HttpMethod.POST, root);
		MspResponseStatus rspStatus = rsp.getResponseStatus();
		if(rspStatus.getErrorCode() == 0) {
			JsonNode jsonData = Json.serializer().nodeFromObject(rsp.getObjData());
			req.setAttribute("NameCD", commons.getTextJsonNode(jsonData.at("/NameCD")));
			req.setAttribute("VND", commons.getTextJsonNode(jsonData.at("/VND")));
			req.setAttribute("USD", commons.getTextJsonNode(jsonData.at("/USD")));
			
			
			String check_tax = commons.getTextJsonNode(jsonData.at("/TaxInvoice"));
			
			if(!check_tax.equals("")) {
				req.setAttribute("DefaultTaxAdmin", check_tax);
			}
			
			
			String shd = jsonData.at("/viewshd").toString();	
			String viewmoney = jsonData.at("/viewmoney").toString();
			String mail = jsonData.at("/footermail").toString();
		
			int index1 = shd.charAt(1);
			char check =(char) index1;
			String s=String.valueOf(check);  
		

				if(s.equals("Y")) {
					req.setAttribute("CheckSHD", true);
			
				}
				else {
					req.setAttribute("CheckSHD", false);
				
				}
				//FOOTER MAIL
				if(!mail.equals("")) {
					int index2 = mail.charAt(1);
					char check2 =	(char) index2;

					String s2=String.valueOf(check2);  
						if(s2.equals("Y")) {
							req.setAttribute("CheckMail", true);
					
						}
						else {
							req.setAttribute("CheckMail", false);
						
						}
				}	
				//view money
				if(!viewmoney.equals("")) {
					int index2 = viewmoney.charAt(1);
					char check2 =	(char) index2;

					String s2=String.valueOf(check2);  
						if(s2.equals("Y")) {
							req.setAttribute("ViewMoney", true);
					
						}
						else {
							req.setAttribute("ViewMoney", false);
						
						}
				}	
				
			}
			return "system/config-param";
	}

	public BaseDTO checkDataToAccept(HttpServletRequest req, HttpSession session, String transaction
			, CurrentUserProfile cup) throws Exception{
		BaseDTO dto = new BaseDTO();
		dto.setErrorCode(0);	
		_id = commons.getParameterFromRequest(req, "_id").replaceAll("\\s", "");
		VND = commons.getParameterFromRequest(req, "vnd").replaceAll("\\s", "");
		USD = commons.getParameterFromRequest(req, "usd");
		viewshd = commons.getParameterFromRequest(req, "view-shd");
		viewmoney = commons.getParameterFromRequest(req, "viewmoney");
		namecd = commons.getParameterFromRequest(req, "namecd");
		footermail = commons.getParameterFromRequest(req, "footermail");
		tax_invoice = commons.getParameterFromRequest(req, "tax_invoice");
				
		
		if(!commons.checkStringIsInt(VND)) {
			dto.setErrorCode(1);
			dto.getErrorMessages().add("Đơn vị tiền tệ phải là số nguyên.");
		}
		if(!commons.checkStringIsInt(USD)) {
			dto.setErrorCode(1);
			dto.getErrorMessages().add("Đơn vị tiền tệ phải là số nguyên.");
		}
		
		int Check_VND = Integer.parseInt(VND);
		int Check_USD = Integer.parseInt(USD);
		
		if(Check_VND < 0 || Check_VND > 8) {
			dto.setErrorCode(1);
			dto.getErrorMessages().add("Đơn vị tiền tệ VND phải hợp lệ.");
		}
		
		if(Check_USD < 0 || Check_USD > 8) {
			dto.setErrorCode(1);
			dto.getErrorMessages().add("Đơn vị tiền tệ USD phải hợp lệ.");
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
		String messageConfirm = "Xác nhận lưu dữ liệu?";
		
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
		
		String actionCode = Constants.MSG_ACTION_CODE.CREATEDPARAM;
		dtoRes = new BaseDTO(req);
		Msg msg = dtoRes.createMsg(cup, actionCode);
		HashMap<String, Object> hData = new HashMap<>();
		hData.put("_id", _id);
		hData.put("VND", VND);
		hData.put("USD", USD);
		hData.put("viewshd", viewshd);
		hData.put("viewmoney", viewmoney);
		hData.put("namecd", namecd);
		hData.put("footermail", footermail);
		hData.put("TaxInvoice", tax_invoice);
		
		msg.setObjData(hData);
		JSONRoot root = new JSONRoot(msg);
		MsgRsp rsp = restAPI.callAPINormal("/config-param/crud", cup.getLoginRes().getToken(), HttpMethod.POST, root);
		MspResponseStatus rspStatus = rsp.getResponseStatus();
		if(rspStatus.getErrorCode() == 0) {
			dtoRes.setErrorCode(0);
			dtoRes.setResponseData("Thành công.");
		}else {
			dtoRes.setErrorCode(rspStatus.getErrorCode());
			dtoRes.setResponseData(rspStatus.getErrorDesc());
		}
		return dtoRes;
	}
	
}
