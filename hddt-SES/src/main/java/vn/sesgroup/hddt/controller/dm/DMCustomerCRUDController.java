package vn.sesgroup.hddt.controller.dm;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
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
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;

import vn.sesgroup.hddt.controller.AbstractController;
import vn.sesgroup.hddt.dto.BaseDTO;
import vn.sesgroup.hddt.dto.CurrentUserProfile;
import vn.sesgroup.hddt.resources.RestAPIUtility;
import vn.sesgroup.hddt.utils.Constants;
import vn.sesgroup.hddt.utils.Json;
@Controller
@RequestMapping({
	"/dmcustomer-cre",
	"/dmcustomer-detail",
	"/dmcustomer-edit",
	"/dmcustomer-del"
})
@Scope(value = WebApplicationContext.SCOPE_REQUEST)
public class DMCustomerCRUDController extends AbstractController{
	private static final Logger log = LogManager.getLogger(DMCustomerCRUDController.class);
	@Autowired RestAPIUtility restAPI;
	
	private String errorCode;
	private String errorDesc;
	private String _id;
	private String taxCode;
	private String customerCode;
	private String companyName;
	private String customerName;
	private String address;
	private String email;
	private String emailcc;
	private String LHDSDung_HDBTSCong;
	private String phone;
	private String fax;
	private String website;
	private String province;
	private String provinceName;	
	private String accountNumber;
	private String accountBankName;
	private String customerGroup1;
	private String customerGroup1Name;
	private String customerGroup2;
	private String customerGroup2Name;
	private String customerGroup3;
	private String customerGroup3Name;
	private String remark;
	private String roleId;
	private String _token;
	private List<String> ids = null;
	
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
			msgParam.setParam("DMCustomerGroup");
			msgParams.getParams().add(msgParam);
			
			msgParam = new MsgParam();
			msgParam.setId("param03");
			msgParam.setParam("RolesRightManage");
			msgParams.getParams().add(msgParam);
			
			/*END: DANH SACH THAM SO*/
			msg.setObjData(msgParams);
			
			JSONRoot root = new JSONRoot(msg);
			MsgRsp rsp = restAPI.callAPINormal("/commons/get-full-params", cup.getLoginRes().getToken(), HttpMethod.POST, root);
			MspResponseStatus rspStatus = rsp.getResponseStatus();
			
			if(rspStatus.getErrorCode() == 0 &&
					rsp.getObjData() != null) {
				LinkedHashMap<String, String> hItem = null;
				ArrayList<Object> arrTmp = null;
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
						hItem.put(commons.getTextJsonNode(o.get("Code")), commons.getTextJsonNode(o.get("Name")));
					}
					req.setAttribute("map_customergroup", hItem);
				}
				
				if(null != jsonData.at("/param03")) {
					arrTmp = new ArrayList<Object>();
					for(JsonNode o: jsonData.at("/param03")) {
						arrTmp.add(Json.serializer().fromNode(o, new TypeReference<HashMap<String, String>>() {
						}));	
					}
					req.setAttribute("RolesRightList", arrTmp);
				}
				
			}
		}catch(Exception e) {}
	}
	
	@RequestMapping(value = "/init", method = {RequestMethod.POST})
	public String init(Locale locale, HttpServletRequest req, HttpSession session
			, @RequestAttribute(name = "transaction", value = "", required = false) String transaction) throws Exception{
		errorCode = "";
		errorDesc = "";
		
		_id = commons.getParameterFromRequest(req, "_id");
		CurrentUserProfile cup = getCurrentlyAuthenticatedPrincipal();
		
		String header = "Thêm mới khách hàng";
		String action = "CREATE";
		boolean isEdit = false;
		
		switch (transaction) {
		case "dmcustomer-cre":
			header = "Thêm mới khách hàng";
			action = "CREATE";
			isEdit = true;
			break;
		case "dmcustomer-detail":
			header = "Chi tiết thông tin khách hàng";
			action = "DETAIL";
			isEdit = false;
			break;
		case "dmcustomer-edit":
			header = "Thay đổi thông tin khách hàng";
			action = "EDIT";
			isEdit = true;
			break;
		default:
			break;
		}
		
		if("|dmcustomer-detail|dmcustomer-edit|".indexOf(transaction) != -1) {
			inquiry(cup, locale, req, session, _id, action);
		}
		
		req.setAttribute("_header_", header);
		req.setAttribute("_action_", action);
		req.setAttribute("_id", _id);
		req.setAttribute("_isedit_", isEdit);
		
		LoadParameter(cup, locale, req, action);
		
		if(!"".equals(errorDesc))
			req.setAttribute("messageError", errorDesc);
		
		return "dm/customer-crud";
	}
	
	private void inquiry(CurrentUserProfile cup, Locale locale, HttpServletRequest req, HttpSession session, String _id , String action) throws Exception{
		if("".equals(_id)) {
			errorCode = "NOT FOUND";
			errorDesc = "Không tìm thấy thông tin sản phẩm.";
			return;
		}
		BaseDTO baseDTO = new BaseDTO(req);
		Msg msg = baseDTO.createMsg(cup, Constants.MSG_ACTION_CODE.INQUIRY);
		HashMap<String, String> hData = new HashMap<>();
		msg.setObjData(hData);
		
		JSONRoot root = new JSONRoot(msg);
		MsgRsp rsp = restAPI.callAPINormal("/dmcustomer/detail/" + _id, cup.getLoginRes().getToken(), HttpMethod.POST, root);
		MspResponseStatus rspStatus = rsp.getResponseStatus();
		if(rspStatus.getErrorCode() == 0) {
			JsonNode jsonData = Json.serializer().nodeFromObject(rsp.getObjData());
			
			req.setAttribute("TaxCode", commons.getTextJsonNode(jsonData.at("/TaxCode")));
			req.setAttribute("CustomerCode", commons.getTextJsonNode(jsonData.at("/CustomerCode")));
			req.setAttribute("CompanyName", commons.getTextJsonNode(jsonData.at("/CompanyName")));
			req.setAttribute("CustomerName", commons.getTextJsonNode(jsonData.at("/CustomerName")));
			req.setAttribute("Address", commons.getTextJsonNode(jsonData.at("/Address")));
			req.setAttribute("Email", commons.getTextJsonNode(jsonData.at("/Email")));
			req.setAttribute("EmailCC", commons.getTextJsonNode(jsonData.at("/EmailCC")));
			req.setAttribute("LHDSDung_HDBTSCong", commons.getTextJsonNode(jsonData.at("/LHDSDung_HDBTSCong")));
			req.setAttribute("Phone", commons.getTextJsonNode(jsonData.at("/Phone")));
			req.setAttribute("Fax", commons.getTextJsonNode(jsonData.at("/Fax")));
			req.setAttribute("Website", commons.getTextJsonNode(jsonData.at("/Website")));
			req.setAttribute("TThanhCode", commons.getTextJsonNode(jsonData.at("/Province/Code")));
			req.setAttribute("AccountNumber", commons.getTextJsonNode(jsonData.at("/AccountNumber")));
			req.setAttribute("AccountBankName", commons.getTextJsonNode(jsonData.at("/AccountBankName")));
			req.setAttribute("CustomerGroup1", commons.getTextJsonNode(jsonData.at("/CustomerGroup1/Code")));
			req.setAttribute("CustomerGroup2", commons.getTextJsonNode(jsonData.at("/CustomerGroup2/Code")));
			req.setAttribute("CustomerGroup3", commons.getTextJsonNode(jsonData.at("/CustomerGroup3/Code")));
			req.setAttribute("Remark", commons.getTextJsonNode(jsonData.at("/Remark")));			
		}else {
			errorDesc = rspStatus.getErrorDesc();
		}
	}
	
	public BaseDTO checkDataToAccept(HttpServletRequest req, HttpSession session, String transaction
			, CurrentUserProfile cup) throws Exception{
		BaseDTO dto = new BaseDTO();
		dto.setErrorCode(0);
		
		_id = commons.getParameterFromRequest(req, "_id").replaceAll("\\s", "");
		taxCode = commons.getParameterFromRequest(req, "tax-code").replaceAll("\\s", "").toUpperCase();
		customerCode = commons.getParameterFromRequest(req, "customer-code").replaceAll("\\s", "").toUpperCase();
		companyName = commons.getParameterFromRequest(req, "company-name").trim().replaceAll("\\s+", " ");
		customerName = commons.getParameterFromRequest(req, "customer-name").trim().replaceAll("\\s+", " ");
		address = commons.getParameterFromRequest(req, "address").trim().replaceAll("\\s+", " ");
		email = commons.getParameterFromRequest(req, "email").trim().replaceAll("\\s+", " ");
		emailcc = commons.getParameterFromRequest(req, "emailcc").trim().replaceAll("\\s+", " ");
		LHDSDung_HDBTSCong = commons.getParameterFromRequest(req, "LHDSDung_HDBTSCong").trim().replaceAll("\\s+", " ");
		
		phone = commons.getParameterFromRequest(req, "phone").trim().replaceAll("\\s+", " ");
		fax = commons.getParameterFromRequest(req, "fax").trim().replaceAll("\\s+", " ");
		website = commons.getParameterFromRequest(req, "website").trim().replaceAll("\\s+", " ");
		province = commons.getParameterFromRequest(req, "province").trim().replaceAll("\\s+", " ");
		provinceName = commons.getParameterFromRequest(req, "province-name").trim().replaceAll("\\s+", " ");
		roleId = commons.getParameterFromRequest(req, "roleId").replaceAll("\\s", "");
		accountNumber = commons.getParameterFromRequest(req, "account-number").trim().replaceAll("\\s+", " ");
		accountBankName = commons.getParameterFromRequest(req, "account-bank-name").trim().replaceAll("\\s+", " ");
		customerGroup1 = commons.getParameterFromRequest(req, "customer-group-1").trim().replaceAll("\\s+", " ");
		customerGroup1Name = commons.getParameterFromRequest(req, "customer-group-1-name").trim().replaceAll("\\s+", " ");
		
		customerGroup2 = commons.getParameterFromRequest(req, "customer-group-2").trim().replaceAll("\\s+", " ");
		customerGroup2Name = commons.getParameterFromRequest(req, "customer-group-2-name").trim().replaceAll("\\s+", " ");
		
		customerGroup3 = commons.getParameterFromRequest(req, "customer-group-3").trim().replaceAll("\\s+", " ");
		customerGroup3Name = commons.getParameterFromRequest(req, "customer-group-3-name").trim().replaceAll("\\s+", " ");
		
		remark = commons.getParameterFromRequest(req, "remark").trim().replaceAll("\\s+", " ");
		
		if("dmcustomer-edit".equals(transaction)) {
			if("".equals(_id)) {
				dto.setErrorCode(1);
				dto.getErrorMessages().add("Không tìm thấy thông tin khách hàng.");
			}
		}
		switch (transaction) {
		case "dmcustomer-cre":
		case "dmcustomer-edit":
			if("".equals(taxCode)) {
				dto.setErrorCode(1);
				dto.getErrorMessages().add("Vui lòng nhập vào MST khách hàng.");
			}
//			if("".equals(companyName)) {
//				dto.setErrorCode(1);
//				dto.getErrorMessages().add("Vui lòng nhập vào tên đơn vị.");
//			}
//			if("".equals(customerName)) {
//				dto.setErrorCode(1);
//				dto.getErrorMessages().add("Vui lòng nhập vào tên người mua hàng.");
//			}
			if("".equals(address)) {
				dto.setErrorCode(1);
				dto.getErrorMessages().add("Vui lòng nhập vào địa chỉ khách hàng.");
			}
			break;
		case "dmcustomer-del":
			_token = commons.getParameterFromRequest(req, "_token").replaceAll("\\s", "");
			ids = null;
			try {
				ids = Json.serializer().fromJson(commons.decodeBase64ToString(_token), new TypeReference<List<String>>() {
				});
			}catch(Exception e) {}
			break;
		default:
			break;
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
		String messageConfirm = "Bạn có muốn thêm mới khách hàng không?";
		switch (transaction) {
		case "dmcustomer-cre":
			messageConfirm = "Bạn có muốn thêm mới khách hàng không?";
			break;
		case "dmcustomer-edit":
			messageConfirm = "Bạn có muốn thay đổi thông tin khách hàng không?";
			break;
		case "dmcustomer-del":
			messageConfirm = "Bạn có muốn xóa danh sách khách hàng không?";
			break;
		default:
			dto = new BaseDTO();
			dto.setErrorCode(998);
			dto.setResponseData("Không tìm thấy chức năng giao dịch.");
			return dto;
		}
		
		CurrentUserProfile cup = getCurrentlyAuthenticatedPrincipal();
		dto = checkDataToAccept(req, session, transaction, cup);
		if(0 != dto.getErrorCode()) {
			dto.setErrorCode(999);
			dto.setResponseData(Constants.MAP_ERROR.get(999));
			return dto;
		}
		
		if("dmcustomer-del".equals(transaction)) {
			if(null == ids || ids.size() == 0) {
				dto.setErrorCode(999);
				dto.setResponseData("Không tìm thấy danh sách khách hàng cần xóa.");
				return dto;
			}
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
		switch (transaction) {
		case "dmcustomer-cre": actionCode = Constants.MSG_ACTION_CODE.CREATED; break;
		case "dmcustomer-edit": actionCode = Constants.MSG_ACTION_CODE.MODIFY; break;
		case "dmcustomer-del": actionCode = Constants.MSG_ACTION_CODE.DELETE; break;
		default:
			dtoRes = new BaseDTO();
			dtoRes.setErrorCode(998);
			dtoRes.setResponseData(Constants.MAP_ERROR.get(998));
			return dtoRes;
		}
		
		dtoRes = new BaseDTO(req);
		Msg msg = dtoRes.createMsg(cup, actionCode);
		HashMap<String, Object> hData = new HashMap<>();
		switch (transaction) {
		case "dmcustomer-del":
			hData.put("ids", ids);
			break;
		default:
			hData.put("_id", _id);
			hData.put("TaxCode", taxCode);
			hData.put("CustomerCode", customerCode);
			hData.put("CompanyName", companyName);
			hData.put("CustomerName", customerName);
			hData.put("Address", address);
			hData.put("Email", email);
			hData.put("EmailCC", emailcc);
			hData.put("Boxmail", LHDSDung_HDBTSCong);
			
			hData.put("RoleId", roleId);
			hData.put("Phone", phone);
			hData.put("Fax", fax);
			hData.put("Website", website);
			hData.put("Province", province);
			hData.put("ProvinceName", provinceName);
			hData.put("AccountNumber", accountNumber);
			hData.put("AccountBankName", accountBankName);
			hData.put("CustomerGroup1", customerGroup1);
			hData.put("CustomerGroup1Name", customerGroup1Name);
			hData.put("CustomerGroup2", customerGroup2);
			hData.put("CustomerGroup2Name", customerGroup2Name);
			hData.put("CustomerGroup3", customerGroup3);
			hData.put("CustomerGroup3Name", customerGroup3Name);
			hData.put("Remark", remark);
			break;
		}
		
		msg.setObjData(hData);
		JSONRoot root = new JSONRoot(msg);
		MsgRsp rsp = restAPI.callAPINormal("/dmcustomer/crud", cup.getLoginRes().getToken(), HttpMethod.POST, root);
		MspResponseStatus rspStatus = rsp.getResponseStatus();
		if(rspStatus.getErrorCode() == 0) {
			JsonNode jsonData = Json.serializer().nodeFromObject(rsp.getObjData());
			HashMap<String, Object> hR = new HashMap<String, Object>();
			
			dtoRes.setErrorCode(0);
			switch (transaction) {
			case "dmcustomer-cre":
				hR.put("info", "Thêm mới thông tin khách hàng thành công.");
				hR.put("CustomerCode", commons.getTextJsonNode(jsonData.at("/CustomerCode")));
				dtoRes.setResponseData(hR);
				break;
			case "dmcustomer-edit":
				dtoRes.setResponseData("Cập nhật thông tin khách hàng thành công.");
				break;
			case "dmcustomer-del":
				dtoRes.setResponseData("Xóa danh sách khách hàng thành công.");
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
