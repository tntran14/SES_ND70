package vn.sesgroup.hddt.controller.quantity;

import java.io.File;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.multipart.MultipartFile;

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
import vn.sesgroup.hddt.dto.FileInfo;
import vn.sesgroup.hddt.dto.IssuerInfo;
import vn.sesgroup.hddt.resources.APIParams;
import vn.sesgroup.hddt.resources.RestAPIUtility;
import vn.sesgroup.hddt.utils.Constants;
import vn.sesgroup.hddt.utils.Json;
import vn.sesgroup.hddt.utils.SystemParams;

@Controller
@RequestMapping({
	"/quantity-cre"
	, "/quantity-detail"
	, "/quantity-edit"
})
@Scope(value = WebApplicationContext.SCOPE_REQUEST)
public class QuantitysCRUDController extends AbstractController{
	private static final Logger log = LogManager.getLogger(QuantitysCRUDController.class);
	@Autowired RestAPIUtility restAPI;
	@Autowired RestTemplate restTemplate;
	
	private String errorCode;
	private String errorDesc;
	private String _id;
	private String mausohdon;
	private String trthai;
	private String quantity;

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
			msgParam.setParam("DMPaymentType");
			msgParams.getParams().add(msgParam);
			
			msgParam = new MsgParam();
			msgParam.setId("param02");
			msgParam.setParam("DMMauSoKyHieuForCreate");
			msgParams.getParams().add(msgParam);
			
			msgParam = new MsgParam();
			msgParam.setId("param03");
			msgParam.setParam("DMCurrencies");
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
						hItem.put(commons.getTextJsonNode(o.get("code")), commons.getTextJsonNode(o.get("name")));
					}
					req.setAttribute("map_paymenttype", hItem);
				}
				if(null != jsonData.at("/param02") && jsonData.at("/param02") instanceof ArrayNode) {
					hItem = new LinkedHashMap<String, String>();
					for(JsonNode o: jsonData.at("/param02")) {
					
						hItem.put(commons.getTextJsonNode(o.get("_id")), commons.getTextJsonNode(o.get("KHMSHDon")) + commons.getTextJsonNode(o.get("KHHDon")));
					
					}
					req.setAttribute("map_mausokyhieu", hItem);
				}			
			}
			
		}catch(Exception e) {}
	}
	
	@RequestMapping(value = "/init", method = {RequestMethod.POST})
	public String init(Locale locale, HttpServletRequest req, HttpSession session
			, @RequestAttribute(name = "transaction", value = "", required = false) String transaction
			, @RequestAttribute(name = "method", value = "", required = false) String method
			) throws Exception{
		errorCode = "";
		errorDesc = "";
		
		CurrentUserProfile cup = getCurrentlyAuthenticatedPrincipal();
		IssuerInfo ii = cup.getLoginRes().getIssuerInfo();
		_id = commons.getParameterFromRequest(req, "_id");
		String header = "Thêm mới thông báo phát hành";
		String action = "CREATE";
		boolean isEdit = false;
		
		switch (transaction) {
		case "quantity-cre":
			header = "Thêm mới thông báo phát hành";
			action = "CREATE";
			isEdit = true;
			break;
		case "quantity-edit":
			header = "Phát hành số lượng";
			action = "EDIT";
			isEdit = true;
			break;
		case "quantity-detail":
			header = "Chi tiết nội dung phát hành";
			action = "DETAIL";
			isEdit = false;
			break;
		
		default:
			break;
		}
		
		if("|quantity-edit|quantity-detail|".indexOf(transaction) != -1)
			inquiry(cup, locale, req, session, _id, action, transaction, method);
		if("quantity-cre".equals(transaction)) {
			req.setAttribute("NLap", commons.convertLocalDateTimeToString(LocalDate.now(), Constants.FORMAT_DATE.FORMAT_DATE_WEB));
		}
		
		req.setAttribute("_header_", header);
		req.setAttribute("_action_", action);
		req.setAttribute("_isedit_", isEdit);
		req.setAttribute("_id", _id);
		
		if("|quantity-cre|quantity-edit|".indexOf(transaction) != -1)
			LoadParameter(cup, locale, req, action);

		if(!"".equals(errorDesc))
			req.setAttribute("messageError", errorDesc);
		
		return "realease_quantity/realease_quantities-cud";
	}
	
	private void inquiry(CurrentUserProfile cup, Locale locale, HttpServletRequest req, HttpSession session, String _id , String action, String transaction, String method) throws Exception{
		if("".equals(_id)) {
			errorCode = "NOT FOUND";
			errorDesc = "Không tìm thấy thông tin hóa đơn.";
			return;
		}
		BaseDTO baseDTO = new BaseDTO(req);
		Msg msg = baseDTO.createMsg(cup, Constants.MSG_ACTION_CODE.INQUIRY);
		HashMap<String, String> hData = new HashMap<>();
		msg.setObjData(hData);
		
		JSONRoot root = new JSONRoot(msg);
		MsgRsp rsp = restAPI.callAPINormal("/quantity/detail/" + _id, cup.getLoginRes().getToken(), HttpMethod.POST, root);
		MspResponseStatus rspStatus = rsp.getResponseStatus();
		if(rspStatus.getErrorCode() == 0) {
			
			JsonNode jsonData = Json.serializer().nodeFromObject(rsp.getObjData());
//			req.setAttribute("THDon", commons.getTextJsonNode(jsonData.at("/")));
			req.setAttribute("_id", _id);
			req.setAttribute("MauSoHdon", commons.getTextJsonNode(jsonData.at("/KHMSHDon"))+ commons.getTextJsonNode(jsonData.at("/KHHDon")));
			req.setAttribute("KHMSHDon", commons.getTextJsonNode(jsonData.at("/KHMSHDon")));
			req.setAttribute("Quantity", commons.getTextJsonNode(jsonData.at("/SoLuong")));		
			req.setAttribute("NamePhoi", commons.getTextJsonNode(jsonData.at("/NamePhoi")));		
			req.setAttribute("TuSo", commons.getTextJsonNode(jsonData.at("/TuSo")));
			req.setAttribute("DenSo", commons.getTextJsonNode(jsonData.at("/DenSo")));
			req.setAttribute("NLap",  commons.convertLocalDateTimeToString(commons.convertLongToLocalDate(jsonData.at("/NLap").asLong()), Constants.FORMAT_DATE.FORMAT_DATE_WEB));
		}
		else {
			errorDesc = rspStatus.getErrorDesc();
		}
	}
	
	public BaseDTO checkDataToAccept(HttpServletRequest req, HttpSession session, String transaction
			, CurrentUserProfile cup) throws Exception{
		BaseDTO dto = new BaseDTO();
		dto.setErrorCode(0);
		
		_id = commons.getParameterFromRequest(req, "_id").replaceAll("\\s", "");
		mausohdon = commons.getParameterFromRequest(req, "mausohdon").replaceAll("\\s", "");
		trthai = commons.getParameterFromRequest(req, "trthai").replaceAll("\\s", "");
		quantity = commons.getParameterFromRequest(req, "quantity").replaceAll("\\s", "");
		
		if("quantity-edit".equals(transaction)) {
			if("".equals(_id)) {
				dto.setErrorCode(1);
				dto.getErrorMessages().add("Không tìm thấy thông tin hóa đơn.");
			}
		}
	
		switch (transaction) {
		case "quantity-cre":
		case "quantity-edit":	
			if("".equals(quantity)) {
				dto.setErrorCode(1);
				dto.getErrorMessages().add("Vui lòng nhập số lượng.");
			}
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
		String messageConfirm = "Bạn có muốn thêm mới hóa đơn không?";
		switch (transaction) {
		case "quantity-cre":
			messageConfirm = "Bạn có muốn thêm mới hóa đơn không?";
			break;
		case "quantity-edit":
			messageConfirm = "Bạn có muốn phát hành số lượng không?";
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
		
//		JsonNode jsonNodeTmp = null;
//		
//		jsonNodeTmp = null;
//		try {
//			jsonNodeTmp = Json.serializer().nodeFromJson(commons.decodeBase64ToString(dsSanPham));
//		}catch(Exception e) {
//			log.error(" >>>>> An exception occurred!", e);
//		}
//		
//		if(null == jsonNodeTmp || jsonNodeTmp.size() == 0) {
//			dto.setErrorCode(999);
//			dto.setResponseData("Hóa đơn chưa có dữ liệu hàng hóa.");
//			return dto;
//		}
		
//		/*KIEM TRA THONG TIN SAN PHAM*/
//		boolean check = true;
//		int count = 0;
//		JsonNode jsonNode = null;
//		while(count < jsonNodeTmp.size() && check) {
//			jsonNode = jsonNodeTmp.get(count);
//			if("".equals(commons.getTextJsonNode(jsonNode.at("/Feature"))) 
//					|| "".equals(commons.getTextJsonNode(jsonNode.at("/ProductName"))) 
////					|| commons.ToNumber(commons.getTextJsonNode(jsonNode.at("/Total"))) < 0
////					|| (
////							(!"4".equals(commons.getTextJsonNode(jsonNode.at("/Feature"))) && !"2".equals(commons.getTextJsonNode(jsonNode.at("/Feature"))))
////							 && commons.ToNumber(commons.getTextJsonNode(jsonNode.at("/Total"))) <= 0 
////						)
//					) {
//				check = false;
//				break;
//			}
//			count++;
//		}
//		if(!check) {
//			dto.setErrorCode(999);
//			dto.setResponseData("Vui lòng kiểm tra lại dữ liệu hàng hóa.");
//			return dto;
//		}
//		/*END - KIEM TRA THONG TIN SAN PHAM*/
		
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
//		JsonNode jsonNodeTmp = null;
//		try {
//			jsonNodeTmp = Json.serializer().nodeFromJson(commons.decodeBase64ToString(dsSanPham));
//		}catch(Exception e) {
//			log.error(" >>>>> An exception occurred!", e);
//		}
//		if(null == jsonNodeTmp || jsonNodeTmp.size() == 0) {
//			dtoRes.setErrorCode(999);
//			dtoRes.setResponseData("Hóa đơn chưa có dữ liệu hàng hóa.");
//			return dtoRes;
//		}
		
		String actionCode = Constants.MSG_ACTION_CODE.CREATED;
		switch (transaction) {
		case "quantity-cre": actionCode = Constants.MSG_ACTION_CODE.CREATED; break;
		case "quantity-edit": actionCode = Constants.MSG_ACTION_CODE.MODIFY; break;
		default:
			dtoRes = new BaseDTO();
			dtoRes.setErrorCode(998);
			dtoRes.setResponseData(Constants.MAP_ERROR.get(998));
			return dtoRes;
		}
		
		dtoRes = new BaseDTO(req);
		Msg msg = dtoRes.createMsg(cup, actionCode);
		HashMap<String, Object> hData = new HashMap<>();
		hData.put("_id", _id);	
		hData.put("mausohdon", mausohdon);	
		hData.put("trthai", trthai);	
		hData.put("quantity", quantity);	
//		hData.put("DSSanPham", jsonNodeTmp);
		
		msg.setObjData(hData);
		JSONRoot root = new JSONRoot(msg);
		MsgRsp rsp = restAPI.callAPINormal("/quantity/crud", cup.getLoginRes().getToken(), HttpMethod.POST, root);
		MspResponseStatus rspStatus = rsp.getResponseStatus();
		if(rspStatus.getErrorCode() == 0) {
			dtoRes.setErrorCode(0);
			switch (transaction) {
			case "quantity-cre":
				dtoRes.setResponseData("Thêm mới thông tin hóa đơn thành công.");
				break;
			case "quantity-edit":
				dtoRes.setResponseData("Cập nhật số lượng thành công.");
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
