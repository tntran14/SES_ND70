package vn.sesgroup.hddt.controller.admin.api;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
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
import org.springframework.web.client.RestTemplate;
import org.springframework.web.context.WebApplicationContext;

import com.api.message.JSONRoot;
import com.api.message.Msg;
import com.api.message.MsgRsp;
import com.api.message.MspResponseStatus;
import com.fasterxml.jackson.databind.JsonNode;

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
	"/session_key_cre"
	, "/session_key_detail"
	, "/session_key_edit"
})
@Scope(value = WebApplicationContext.SCOPE_REQUEST)
public class SessionKeyCRUDController extends AbstractController{
	private static final Logger log = LogManager.getLogger(SessionKeyCRUDController.class);
	@Autowired RestAPIUtility restAPI;
	@Autowired RestTemplate restTemplate;
	
	private String errorCode;
	private String errorDesc;
	private String _id;
	private String mst;
	private String name;
	private String effectDate;
	private String expireDate;
	private String IpAddress;
	
	
	@RequestMapping(value = {"/init"}, method = {RequestMethod.POST})
	public String init(Locale locale, HttpServletRequest req, HttpSession session
			, @RequestAttribute(name = "transaction", value = "", required = false) String transaction
			, @RequestAttribute(name = "method", value = "", required = false) String method
			, @RequestParam(value = "tokenTransaction", required = false, defaultValue = "") String tokenTransaction
			) throws Exception{
		errorCode = "";
		errorDesc = "";
		
		CurrentUserProfile cup = getCurrentlyAuthenticatedPrincipal();
		IssuerInfo ii = cup.getLoginRes().getIssuerInfo();
		
		String mst = "";
		String name = "";
		if(!tokenTransaction.equals("")) {
			String[] split =tokenTransaction.split(",");
			mst = split[0];
			name = split[1]; 
		}
		
		LoginRes issu = cup.getLoginRes();
		String _idsu = issu.getIssuerId();

		BaseDTO baseDTO = new BaseDTO(req);
		Msg msg = baseDTO.createMsg(cup, Constants.MSG_ACTION_CODE.INQUIRY);
		HashMap<String, String> hData = new HashMap<>();
		msg.setObjData(hData);

		JSONRoot root = new JSONRoot(msg);
		
		_id = commons.getParameterFromRequest(req, "_id");
		String header = "Thêm mới session key";
		String action = "CREATE";
		boolean isEdit = false;
		
		req.setAttribute("NLap", commons.convertLocalDateTimeToString(LocalDate.now(), Constants.FORMAT_DATE.FORMAT_DATE_WEB));

				
		switch (transaction) {
		case "session_key_cre":
			header = "Thêm mới session key";
			action = "CREATE";
			isEdit = true;
			break;
		case "session_key_edit":
			header = "Thay đổi thông tin session key";
			action = "EDIT";
			isEdit = true;
			break;
		case "session_key_detail":
			header = "Chi tiết session key";
			action = "DETAIL";
			isEdit = false;
			break;

		default:
			break;
		}
		
		if("|session_key_edit|session_key_detail|".indexOf(transaction) != -1
				|| "init-dc-tt".equals(method))
			inquiry(cup, locale, req, session, _id, action, transaction, method);
		if("session_key_cre".equals(transaction)) {
			
			LocalDate effectDate = LocalDate.now();
			LocalDate expireDate = effectDate.plus(1, ChronoUnit.YEARS);
			
		req.setAttribute("SessionKey",	"automatically generated");
		req.setAttribute("EffectDate", commons.convertLocalDateTimeToString(effectDate, Constants.FORMAT_DATE.FORMAT_DATE_WEB));
		req.setAttribute("ExpireDate", commons.convertLocalDateTimeToString(expireDate, Constants.FORMAT_DATE.FORMAT_DATE_WEB));
		
		}
		
		req.setAttribute("_header_", header);
		req.setAttribute("_action_", action);
		req.setAttribute("_isedit_", isEdit);
		req.setAttribute("_id", _id);
		
		if("|session_key_cre|session_key_edit|".indexOf(transaction) != -1)
		
		if(!"".equals(errorDesc))
			req.setAttribute("messageError", errorDesc);
		
		
		if(!mst.equals("undefined")&&!mst.equals("")) {
			req.setAttribute("TaxCode", mst);
		}
		
		if(!name.equals("undefined")&&!name.equals("")) {
			req.setAttribute("Name", name);
		}
		
		return "api-admin/session_key_crud";
	}

	private void inquiry(CurrentUserProfile cup, Locale locale, HttpServletRequest req, HttpSession session, String _id , String action, String transaction, String method) throws Exception{
		if("".equals(_id)) {
			errorCode = "NOT FOUND";
			errorDesc = "Không tìm thấy thông tin session key.";
			return;
		}
		BaseDTO baseDTO = new BaseDTO(req);
		Msg msg = baseDTO.createMsg(cup, Constants.MSG_ACTION_CODE.INQUIRY);
		HashMap<String, String> hData = new HashMap<>();
		msg.setObjData(hData);
		
		JSONRoot root = new JSONRoot(msg);
		MsgRsp rsp = restAPI.callAPINormal("/session_key/detail/" + _id, cup.getLoginRes().getToken(), HttpMethod.POST, root);
		MspResponseStatus rspStatus = rsp.getResponseStatus();
		if(rspStatus.getErrorCode() == 0) {
			JsonNode jsonData = Json.serializer().nodeFromObject(rsp.getObjData());
			
			
			req.setAttribute("TaxCode", commons.getTextJsonNode(jsonData.at("/TaxCode")));
			req.setAttribute("SessionKey", commons.getTextJsonNode(jsonData.at("/SessionKey")));
			req.setAttribute("EffectDate", commons.convertLocalDateTimeToString(commons.convertLongToLocalDate(jsonData.at("/EffectDate").asLong()), Constants.FORMAT_DATE.FORMAT_DATE_WEB));
			req.setAttribute("ExpireDate", commons.convertLocalDateTimeToString(commons.convertLongToLocalDate(jsonData.at("/ExpireDate").asLong()), Constants.FORMAT_DATE.FORMAT_DATE_WEB));
			req.setAttribute("Name", commons.getTextJsonNode(jsonData.at("/Name")));
			req.setAttribute("IpAddress", commons.getTextJsonNode(jsonData.at("/IpAddress")));

	
		}else {
			errorDesc = rspStatus.getErrorDesc();
		}
	}
	
	public BaseDTO checkDataToAccept(HttpServletRequest req, HttpSession session, String transaction
			, CurrentUserProfile cup) throws Exception{
		BaseDTO dto = new BaseDTO();
		dto.setErrorCode(0);
		
		_id = commons.getParameterFromRequest(req, "_id").replaceAll("\\s", "");
		mst = commons.getParameterFromRequest(req, "mst").replaceAll("\\s", "");
		name = commons.getParameterFromRequest(req, "name");
		effectDate = commons.getParameterFromRequest(req, "effectDate").replaceAll("\\s", "");
		expireDate = commons.getParameterFromRequest(req, "expireDate").trim().replaceAll("\\s+", "");
		IpAddress = commons.getParameterFromRequest(req, "IpAddress").trim().replaceAll("\\s+", "");
		
		if("session_key_edit".equals(transaction)) {
			if("".equals(_id)) {
				dto.setErrorCode(1);
				dto.getErrorMessages().add("Không tìm thấy thông tin session key.");
			}
		}
	
		switch (transaction) {
		case "session_key_cre":
		case "session_key_copy":
		case "session_key_edit":
			if("".equals(mst)) {
				dto.setErrorCode(1);
				dto.getErrorMessages().add("Vui lòng chọn mẫu số session key.");
			}
			if("".equals(name)) {
				dto.setErrorCode(1);
				dto.getErrorMessages().add("Vui lòng chọn tên đơn vị.");
			}
			if("".equals(effectDate)) {
				dto.setErrorCode(1);
				dto.getErrorMessages().add("Vui lòng chọn ngày session key.");
			}else if(!commons.checkLocalDate(effectDate, Constants.FORMAT_DATE.FORMAT_DATE_WEB)) {
				dto.setErrorCode(1);
				dto.getErrorMessages().add("Ngày không đúng định dạng.");
			}
			else if(commons.compareLocalDate(commons.convertStringToLocalDate(effectDate, Constants.FORMAT_DATE.FORMAT_DATE_WEB), LocalDate.now()) < 0) {
				dto.setErrorCode(1);
				dto.getErrorMessages().add("Ngày hiệu lực session key không được nhỏ hơn ngày hiện tại.");
			}
			if("".equals(expireDate)) {
				dto.setErrorCode(1);
				dto.getErrorMessages().add("Vui lòng chọn ngày session key.");
			}else if(!commons.checkLocalDate(expireDate, Constants.FORMAT_DATE.FORMAT_DATE_WEB)) {
				dto.setErrorCode(1);
				dto.getErrorMessages().add("Ngày không đúng định dạng.");
			}
			else if(commons.compareLocalDate(commons.convertStringToLocalDate(expireDate, Constants.FORMAT_DATE.FORMAT_DATE_WEB), LocalDate.now()) < 0) {
				dto.setErrorCode(1);
				dto.getErrorMessages().add("Ngày hết hiệu lực session key không được nhỏ hơn ngày hiện tại.");
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
		String messageConfirm = "Bạn có muốn thêm mới session key không?";
		switch (transaction) {
		case "session_key_cre":
			messageConfirm = "Bạn có muốn thêm mới session key không?";
			break;
		case "session_key_edit":
			messageConfirm = "Bạn có muốn thay đổi thông tin session key không?";
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
		case "session_key_cre": actionCode = Constants.MSG_ACTION_CODE.CREATED; break;
		case "session_key_edit": actionCode = Constants.MSG_ACTION_CODE.MODIFY; break;

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
		hData.put("TaxCode", mst);
		hData.put("Name", name);
		hData.put("EffectDate", effectDate);
		hData.put("ExpireDate", expireDate);
		hData.put("IpAddress", IpAddress);
		
		msg.setObjData(hData);
		JSONRoot root = new JSONRoot(msg);
		MsgRsp rsp = restAPI.callAPINormal("/session_key/crud", cup.getLoginRes().getToken(), HttpMethod.POST, root);
		MspResponseStatus rspStatus = rsp.getResponseStatus();
		if(rspStatus.getErrorCode() == 0) {
			dtoRes.setErrorCode(0);
			switch (transaction) {
			case "session_key_cre":
				dtoRes.setResponseData("Thêm mới thông tin session key thành công.");
				break;
			case "session_key_edit":
				dtoRes.setResponseData("Cập nhật thông tin session key thành công.");
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
	