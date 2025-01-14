package vn.sesgroup.hddt.controller.user;

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
import org.springframework.web.client.RestTemplate;
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
import vn.sesgroup.hddt.dto.IssuerInfo;
import vn.sesgroup.hddt.dto.LoginRes;
import vn.sesgroup.hddt.resources.RestAPIUtility;
import vn.sesgroup.hddt.utils.Constants;
import vn.sesgroup.hddt.utils.Json;

@Controller
@RequestMapping({
	"/changeprofile"
	, "/viewprofile"
})
@Scope(value = WebApplicationContext.SCOPE_REQUEST)
public class ProfileUser extends AbstractController{
	private static final Logger log = LogManager.getLogger(ProfileUser.class);
	@Autowired RestAPIUtility restAPI;
	@Autowired RestTemplate restTemplate;
	
	private String errorCode;
	private String errorDesc;
	private IssuerInfo isu;
	private String _id;
	private String	t ; 
	private String	n ; 
	private String	a ; 
	private String	p ;
	private String	f ; 
	private String	e ;
	private String	w ; 
	private String	ac ; 
	private String	an; 
	private String	bn;		
	private String	tinhThanh ; 
	private String	cqtQLy ; 
	private String	boss ; 
	private String	cv ; 
	private String	ng ; 
	private String	eng ; 
	private String	png; 
	private String	englh;
	private String	maCQT;
	
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
			msgParam.setId("param01");
			msgParam.setParam("DMTinhThanh");
			msgParams.getParams().add(msgParam);
			
			msgParam = new MsgParam();
			msgParam.setId("param02");
			msgParam.setParam("DMChiCucThue");			
			conds = new ArrayList<>();
			hashConds = new HashMap<>();
			hashConds.put("cond", "tinhthanh_ma");
			hashConds.put("condval", null == tinhThanh? "": tinhThanh);
			conds.add(hashConds);
			msgParam.setConds(conds);			
			msgParams.getParams().add(msgParam);
			
			/*END: DANH SACH THAM SO*/
			msg.setObjData(msgParams);
			
			JSONRoot root = new JSONRoot(msg);
			MsgRsp rsp = restAPI.callAPINormal("/commons/get-full-params", cup.getLoginRes().getToken(), HttpMethod.POST, root);
			MspResponseStatus rspStatus = rsp.getResponseStatus();
			
			if(rspStatus.getErrorCode() == 0 &&
					rsp.getObjData() != null) {
				LinkedHashMap<String, String> hItem = null;
				
				JsonNode jsonData = Json.serializer().nodeFromObject(rsp.getObjData());
				if(null != jsonData.at("/param01") && jsonData.at("/param01") instanceof ArrayNode) {
					hItem = new LinkedHashMap<String, String>();
					for(JsonNode o: jsonData.at("/param01")) {
						hItem.put(commons.getTextJsonNode(o.get("code")), commons.getTextJsonNode(o.get("name")));
					}
					req.setAttribute("map_dmtinhthanh", hItem);
				}
				if(null != jsonData.at("/param02") && jsonData.at("/param02") instanceof ArrayNode) {
					hItem = new LinkedHashMap<String, String>();
					for(JsonNode o: jsonData.at("/param02")) {
						hItem.put(commons.getTextJsonNode(o.get("code")), commons.getTextJsonNode(o.get("name")));
					}
					req.setAttribute("map_cucthue", hItem);
				}
				
			}
		}catch(Exception e) {}
	}
	

	@RequestMapping(value = {"/change"}, method = {RequestMethod.GET})
	public String change(Locale locale, HttpServletRequest req, HttpSession session
			, @RequestAttribute(name = "transaction", value = "", required = false) String transaction
			, @RequestAttribute(name = "method", value = "", required = false) String method,String action
			) throws Exception{
		errorCode = "";
		errorDesc = "";
		req.setAttribute("_TitleView_", Constants.PREFIX_TITLE + " - Thay đổi thông tin người dùng");
		CurrentUserProfile cup = getCurrentlyAuthenticatedPrincipal();
		LoginRes us = cup.getLoginRes();
		us = cup.getLoginRes();
		isu = us.getIssuerInfo();
		_id = isu.get_id();
	
		inquiry(cup, locale, req, session, _id, action);
		String header = "Thay đổi profile";
		String action1 = "EDIT";
		boolean isEdit = false;
		req.setAttribute("_header_", header);
		req.setAttribute("_action_", action1);
		req.setAttribute("_isedit_", isEdit);
		req.setAttribute("_id", _id);
		LoadParameter(cup, locale, req, action);
		return "/user/changeprofile";
	}
	@RequestMapping(value = {"/view"}, method = {RequestMethod.GET})
	public String view(Locale locale, HttpServletRequest req, HttpSession session
			, @RequestAttribute(name = "transaction", value = "", required = false) String transaction
			, @RequestAttribute(name = "method", value = "", required = false) String method,String action
			) throws Exception{
		errorCode = "";
		errorDesc = "";
		
		CurrentUserProfile cup = getCurrentlyAuthenticatedPrincipal();
		LoginRes us = cup.getLoginRes();
		us = cup.getLoginRes();
		isu = us.getIssuerInfo();
		_id = isu.get_id();
	
		inquiry(cup, locale, req, session, _id, action);
		String header = "Thay đổi profile";
		String action1 = "EDIT";
		boolean isEdit = false;
		req.setAttribute("_header_", header);
		req.setAttribute("_action_", action1);
		req.setAttribute("_isedit_", isEdit);
		req.setAttribute("_id", _id);
		LoadParameter(cup, locale, req, action);
		

		return "/user/viewprofile";
	}
	private void inquiry(CurrentUserProfile cup, Locale locale, HttpServletRequest req, HttpSession session, String _id,
			String action) throws Exception {
		BaseDTO baseDTO = new BaseDTO(req);
		Msg msg = baseDTO.createMsg(cup, Constants.MSG_ACTION_CODE.INQUIRY);
		HashMap<String, String> hData = new HashMap<>();
		msg.setObjData(hData);
		JSONRoot root = new JSONRoot(msg);
		MsgRsp rsp = restAPI.callAPINormal("/main/profile/" + _id, cup.getLoginRes().getToken(), HttpMethod.POST, root);
		MspResponseStatus rspStatus = rsp.getResponseStatus();
		if (rspStatus.getErrorCode() == 0) {
			JsonNode jsonData = Json.serializer().nodeFromObject(rsp.getObjData());
			req.setAttribute("TaxCode", commons.getTextJsonNode(jsonData.at("/TaxCode")));
			req.setAttribute("Name", commons.getTextJsonNode(jsonData.at("/Name")));
			req.setAttribute("Address", commons.getTextJsonNode(jsonData.at("/Address")));
			req.setAttribute("Phone", commons.getTextJsonNode(jsonData.at("/Phone")));
			req.setAttribute("Fax", commons.getTextJsonNode(jsonData.at("/Fax")));
			req.setAttribute("Email", commons.getTextJsonNode(jsonData.at("/Email")));
			req.setAttribute("Website", commons.getTextJsonNode(jsonData.at("/Website")));								
			req.setAttribute("MainUser", commons.getTextJsonNode(jsonData.at("/MainUser")));
			req.setAttribute("Position", commons.getTextJsonNode(jsonData.at("/Position")));
			tinhThanh = commons.getTextJsonNode(jsonData.at("/TinhThanhInfo/code"));
			req.setAttribute("TThanhCode", tinhThanh);
			req.setAttribute("TThanhName", commons.getTextJsonNode(jsonData.at("/TinhThanhInfo/name")));
			cqtQLy = commons.getTextJsonNode(jsonData.at("/ChiCucThueInfo/code"));
			req.setAttribute("CQThueCode", cqtQLy);
			req.setAttribute("CQThueName", commons.getTextJsonNode(jsonData.at("/ChiCucThueInfo/name")));
			req.setAttribute("NameUser", commons.getTextJsonNode(jsonData.at("/ContactUser/NameUser")));
			req.setAttribute("PhoneUser", commons.getTextJsonNode(jsonData.at("/ContactUser/PhoneUser")));
			req.setAttribute("EmailUser", commons.getTextJsonNode(jsonData.at("/ContactUser/EmailUser")));
			req.setAttribute("EmailUserLh", commons.getTextJsonNode(jsonData.at("/ContactUser/EmailUserLh")));
			req.setAttribute("AccountNumber", commons.getTextJsonNode(jsonData.at("/BankAccount/AccountNumber")));
			req.setAttribute("AccountName", commons.getTextJsonNode(jsonData.at("/BankAccount/AccountName")));
			req.setAttribute("BankName", commons.getTextJsonNode(jsonData.at("/BankAccount/BankName")));	
			
			req.setAttribute("MaCQT", commons.getTextJsonNode(jsonData.at("/MaCQT")));	
			
		} else {
			rspStatus.getErrorDesc();
		}
	}



	public BaseDTO checkDataToAccept(HttpServletRequest req, HttpSession session, String transaction
			, CurrentUserProfile cup) throws Exception{
		BaseDTO dto = new BaseDTO();
		dto.setErrorCode(0);	
			t = commons.getParameterFromRequest(req, "t");
			n = commons.getParameterFromRequest(req, "n");
			a = commons.getParameterFromRequest(req, "a");
			p = commons.getParameterFromRequest(req, "p");
			f = commons.getParameterFromRequest(req, "f");
			e = commons.getParameterFromRequest(req, "e");
			w = commons.getParameterFromRequest(req, "w");
			ac = commons.getParameterFromRequest(req, "ac");
			an= commons.getParameterFromRequest(req, "an");
			bn= commons.getParameterFromRequest(req, "bn");			
			tinhThanh = commons.getParameterFromRequest(req, "tinh-thanh");
			cqtQLy = commons.getParameterFromRequest(req, "CQTQLy");
			boss = commons.getParameterFromRequest(req, "boss");
			cv = commons.getParameterFromRequest(req, "cv");
			ng = commons.getParameterFromRequest(req, "ng");
			eng = commons.getParameterFromRequest(req, "eng");
			png= commons.getParameterFromRequest(req, "png");
			englh= commons.getParameterFromRequest(req, "englh");	
			maCQT= commons.getParameterFromRequest(req, "maCQT");	
				if("".equals(t)) {
					dto.setErrorCode(1);
					dto.getErrorMessages().add("Mã số thuế không được để trống.");
				}
				if("".equals(n)) {
					dto.setErrorCode(1);
					dto.getErrorMessages().add("Tên đơn vị không được để trống.");
				}
				if("".equals(a)) {
					dto.setErrorCode(1);
					dto.getErrorMessages().add("Địa chỉ không được để trống.");
				}
				if("".equals(tinhThanh)) {
					dto.setErrorCode(1);
					dto.getErrorMessages().add("Tỉnh/Thành Phố  không được để trống.");
				}
				if("".equals(cqtQLy)) {
					dto.setErrorCode(1);
					dto.getErrorMessages().add("Cơ quan thuế không được để trống.");
				}
				if(!"".equals(e) && !commons.isValidEmailAddress(e)) {
					dto.setErrorCode(1);
					dto.getErrorMessages().add("Định dạng email không đúng.");
				}
				if(!"".equals(eng) && !commons.isValidEmailAddress(eng)) {
					dto.setErrorCode(1);
					dto.getErrorMessages().add("Định dạng email không đúng.");
				}
				if(!"".equals(englh) && !commons.isValidEmailAddress(englh)) {
					dto.setErrorCode(1);
					dto.getErrorMessages().add("Định dạng email không đúng.");
				}
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
		String actionCode = Constants.MSG_ACTION_CODE.MODIFY;
		dtoRes = new BaseDTO(req);
		Msg msg = dtoRes.createMsg(cup, actionCode);
		HashMap<String, Object> hData = new HashMap<>();
		hData.put("TaxCode", t);
		hData.put("Name", n);
		hData.put("Address", a);
		hData.put("Phone", p);
		hData.put("Fax", f);
		hData.put("Email", e);
		hData.put("Website", w);
		hData.put("AccountNumber", ac);
		hData.put("AccountName", an);
		hData.put("BankName", bn);						
		hData.put("TinhThanh", tinhThanh);
		hData.put("CqtQLy", cqtQLy);
		hData.put("MainUser", boss);
		hData.put("Position", cv);
		hData.put("NameUser", ng);
		hData.put("EmailUser", eng);
		hData.put("PhoneUser", png);
		hData.put("EmailUserLh", englh);
		hData.put("MaCQT", maCQT);
		msg.setObjData(hData);
		JSONRoot root = new JSONRoot(msg);
		MsgRsp rsp = restAPI.callAPINormal("/main/crudprofile", cup.getLoginRes().getToken(), HttpMethod.POST, root);
		MspResponseStatus rspStatus = rsp.getResponseStatus();
		if(rspStatus.getErrorCode() == 0) {
			dtoRes.setErrorCode(0);
			dtoRes.setResponseData("Cập nhật thông tin khách hàng thành công.");
			
			MsgRsp rsp1 = restAPI.callAPINormal("/main/reset_issu", cup.getLoginRes().getToken(), HttpMethod.POST, root);
			MspResponseStatus rspStatus1 = rsp1.getResponseStatus();
			
			
			if(rspStatus1.getErrorCode()==0) {
				
			//	LoginRes res = new LoginRes();
				JsonNode jsonData = Json.serializer().nodeFromObject(rsp1.getObjData());
				
				IssuerInfo reset_issu = new IssuerInfo();
				
				reset_issu.set_id(rsp1.getResponseStatus().getErrorDesc());
				reset_issu.setAddress(commons.getTextJsonNode(jsonData.at("/Address")));
				reset_issu.setName(commons.getTextJsonNode(jsonData.at("/Name")));
				reset_issu.setPhone(commons.getTextJsonNode(jsonData.at("/Phone")));
				reset_issu.setEmail(commons.getTextJsonNode(jsonData.at("/Email")));	
				reset_issu.setTaxCode(commons.getTextJsonNode(jsonData.at("/TaxCode")));		
				reset_issu.setFax(commons.getTextJsonNode(jsonData.at("/Fax")));		
				//res.setIssuerInfo(reset_issu);
			
				
				cup.getLoginRes().setIssuerInfo(reset_issu);
			}
			
			
			
		}else {
			dtoRes.setErrorCode(rspStatus.getErrorCode());
			dtoRes.setResponseData(rspStatus.getErrorDesc());
		}
		return dtoRes;
	}
	


}
