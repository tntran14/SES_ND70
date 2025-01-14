package vn.sesgroup.hddt.controller.issu;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.Principal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Locale;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
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
import vn.sesgroup.hddt.dto.JsonGridDTO;
import vn.sesgroup.hddt.dto.LoginRes;
import vn.sesgroup.hddt.resources.RestAPIUtility;
import vn.sesgroup.hddt.utils.Constants;
import vn.sesgroup.hddt.utils.Json;

@Controller
@RequestMapping({
	"/issu-cre"
	, "/issu-detail"
	, "/issu-edit"
	,"/issu-reset-pass"
	,"/issu-update-kh"
})
@Scope(value = WebApplicationContext.SCOPE_REQUEST)
public class IssuCRUDController extends AbstractController{
	private static final Logger log = LogManager.getLogger(IssuCRUDController.class);
	@Autowired RestAPIUtility restAPI;
	@Autowired RestTemplate restTemplate;
	
	private String errorCode;
	private String errorDesc;
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
	private String acti;

	

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
	
	@RequestMapping(value = "/init", method = {RequestMethod.POST})
	public String init(Locale locale, HttpServletRequest req, HttpSession session
			, @RequestAttribute(name = "transaction", value = "", required = false) String transaction) throws Exception{
		errorCode = "";
		errorDesc = "";
		CurrentUserProfile cup = getCurrentlyAuthenticatedPrincipal();
		_id = commons.getParameterFromRequest(req, "_id");
		String header = "Thêm mới hóa đơn";
		String action = "CREATE";
		boolean isEdit = false;

		switch (transaction) {
		case "issu-cre":
			header = "Thêm mới khách hàng";
			action = "CREATE";
			isEdit = true;
			break;
		case "issu-edit":
			header = "Thay đổi thông tin khách hàng";
			action = "EDIT";
			isEdit = true;
			break;
		case "issu-detail":
			header = "Chi tiết khách hàng";
			action = "DETAIL";
			isEdit = false;
			break;
		case "issu-reset-pass":
			action = Constants.MSG_ACTION_CODE.RESET_PASSWORD;
			header = "Reset mật khẩu khách hàng";
			isEdit = false;
			break;
		default:
			break;
		}
		
		if("|issu-edit|issu-detail|issu-reset-pass".indexOf(transaction) != -1)
		inquiry(cup, locale, req, session, _id, action);
		req.setAttribute("_header_", header);
		req.setAttribute("_action_", action);
		req.setAttribute("_isedit_", isEdit);
		req.setAttribute("_id", _id);
		LoadParameter(cup, locale, req, action);

		if(!"".equals(errorDesc))
			req.setAttribute("messageError", errorDesc);
	
		return "issu/issu-crud";
	}
	
	
	
	
	
	
	
	
	
	
	private void inquiry(CurrentUserProfile cup, Locale locale, HttpServletRequest req, HttpSession session, String _id , String action) throws Exception{
		if("".equals(_id)) {
			errorCode = "NOT FOUND";
			errorDesc = "Không tìm thấy thông tin khách hàng.";
			return;
		}
		BaseDTO baseDTO = new BaseDTO(req);
		Msg msg = baseDTO.createMsg(cup, Constants.MSG_ACTION_CODE.INQUIRY);
		HashMap<String, String> hData = new HashMap<>();
		msg.setObjData(hData);
		JSONRoot root = new JSONRoot(msg);
		MsgRsp rsp = restAPI.callAPINormal("/main/profile/" + _id, cup.getLoginRes().getToken(), HttpMethod.POST, root);
		MspResponseStatus rspStatus = rsp.getResponseStatus();
		if(rspStatus.getErrorCode() == 0) {
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
			req.setAttribute("BankName", commons.getTextJsonNode(jsonData.at("/BankAccount/BankName")));
			req.setAttribute("IsActive", commons.getTextJsonNode(jsonData.at("/IsActive")));	
			
			
			
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
			acti= commons.getParameterFromRequest(req, "acti");	
			
		
			
			 switch (transaction) {
			case "issu-cre":
			case "issu-edit":
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
				if("".equals(englh)) {
					dto.setErrorCode(1);
					dto.getErrorMessages().add("Vui lòng nhập mail nhận thông tin để nhận tài khoản.");
				}
				break;
			case "issu-reset-pass":
				if("".equals(_id)) {
					dto.setErrorCode(1);
					dto.getErrorMessages().add("Không tìm thấy ID khách hàng.");
				}
				break;
			default:
				break;
			}
		return dto;
	}
	
	
	@RequestMapping(value = "/checkuser",  produces = MediaType.APPLICATION_JSON_VALUE, method = RequestMethod.POST)
	@ResponseBody
	public BaseDTO checkuser(Locale locale, HttpServletRequest req, HttpSession session
			, @RequestAttribute(name = "transaction", required = false, value = "") String transaction) throws Exception {
		String token = "";
		if (null != session.getAttribute(Constants.SESSION_TYPE.SESSION_TOKEN_EXECUTE)) {
			token = session.getAttribute(Constants.SESSION_TYPE.SESSION_TOKEN_EXECUTE).toString();
			session.removeAttribute(token);
		}
		session.removeAttribute(Constants.SESSION_TYPE.SESSION_TOKEN_EXECUTE);
		CurrentUserProfile cup = getCurrentlyAuthenticatedPrincipal();
		BaseDTO dto = new BaseDTO();
		t = commons.getParameterFromRequest(req, "t");
		if("".equals(t)) {
			dto.setErrorCode(1);
			dto.getErrorMessages().add("Vui lòng nhập Mã Số Thuế để kiểm tra.");
		}
		else {
			String actionCode = Constants.MSG_ACTION_CODE.CHECK;
			dto = new BaseDTO(req);
			Msg msg = dto.createMsg(cup, actionCode);
			HashMap<String, Object> hData = new HashMap<>();
			hData.put("TaxCode", t);
			msg.setObjData(hData);
			JSONRoot root = new JSONRoot(msg);
		MsgRsp rsp = restAPI.callAPINormal("/issu/crud", cup.getLoginRes().getToken(), HttpMethod.POST, root);
		MspResponseStatus rspStatus = rsp.getResponseStatus();
		if(rspStatus.getErrorCode() == 0) {
			dto.getErrorMessages().add("Chưa Có Tài Khoản");
		}
		else {
			dto.setErrorCode(1);
			dto.getErrorMessages().add("Mã Số Thuế đã tồn tại tài khoản.");
		}
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
		CurrentUserProfile cup = getCurrentlyAuthenticatedPrincipal();
		BaseDTO dto = new BaseDTO();
		String messageConfirm = "Bạn có muốn thêm mới khách hàng không?";
		switch (transaction) {
		case "issu-cre":
			messageConfirm = "Bạn có muốn thêm mới khách hàng không?";
			break;
		case "issu-edit":
			messageConfirm = "Bạn có muốn thay đổi thông tin khách hàng không?";
			break;
		case "issu-reset-pass":
			messageConfirm = "Bạn có chắc chắn muốn reset mật khẩu khách hàng này không?";
			break;
		case "issu-update-kh":
			messageConfirm = "Bạn có chắc chắn muốn cập nhật lại thông tin khách hàng không?";
			break;
		default:
			dto = new BaseDTO();
			dto.setErrorCode(998);
			dto.setResponseData("Không tìm thấy chức năng giao dịch.");
			return dto;
		}
		

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

		String actionCode = Constants.MSG_ACTION_CODE.CREATED;
		switch (transaction) {
		case "issu-cre": actionCode = Constants.MSG_ACTION_CODE.CREATED; break;
		case "issu-edit": actionCode = Constants.MSG_ACTION_CODE.MODIFY; break;
		case "issu-reset-pass": actionCode = Constants.MSG_ACTION_CODE.RESET_PASSWORD;break;
		case "issu-update-kh": actionCode = Constants.MSG_ACTION_CODE.UPDATE_INFO;break;
		default:
			dtoRes = new BaseDTO();
			dtoRes.setErrorCode(998);
			dtoRes.setResponseData(Constants.MAP_ERROR.get(998));
			return dtoRes;
		}
		
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
		hData.put("IsActive", acti);

		
		msg.setObjData(hData);
		JSONRoot root = new JSONRoot(msg);
		MsgRsp rsp = restAPI.callAPINormal("/issu/crud", cup.getLoginRes().getToken(), HttpMethod.POST, root);
		MspResponseStatus rspStatus = rsp.getResponseStatus();
		if(rspStatus.getErrorCode() == 0) {
			dtoRes.setErrorCode(0);
			switch (transaction) {
			case "issu-cre":
				dtoRes.setResponseData("Thêm mới thông tin khách hàng thành công.");
				break;
			case "issu-edit":
				dtoRes.setResponseData("Cập nhật thông tin khách hàng thành công.");
				break;
			case "issu-reset-pass":
				JsonNode jsonData = Json.serializer().nodeFromObject(rsp.getObjData());
				jsonData = Json.serializer().nodeFromObject(rsp.getObjData());
				HashMap<String, Object> hR = new HashMap<String, Object>();
				hR = new HashMap<String, Object>();
				hR.put("info", "Reset mật khẩu khách hàng thành công.");
				hR.put("password", commons.getTextJsonNode(jsonData.at("/Password")));
				dtoRes.setResponseData(hR);
				break;
			case "issu-update-kh":
				dtoRes.setResponseData("Cập nhật thông tin khách hàng thành công.");
				break;
			default:
				dtoRes.setResponseData("Giao dịch thành công.");
				break;
			}
		}else {
			dtoRes.setErrorCode(rspStatus.getErrorCode());
			dtoRes.setResponseData(rspStatus.getErrorDesc());
		}
		return dtoRes;
	}

}
