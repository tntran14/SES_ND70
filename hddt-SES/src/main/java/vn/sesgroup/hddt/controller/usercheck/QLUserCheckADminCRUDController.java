package vn.sesgroup.hddt.controller.usercheck;

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
	"/qlUserCheckAdminCre"
	,"/qlUserCheckAdminDetail"
	,"/qlUserCheckAdminEdit"
	,"/qlUserCheckAdminActive"
	,"/qlUserCheckAdminDeActive"
	,"/qlUserCheckAdminDelete"
	,"/qlUserCheckAdminResetPassword"
})
@Scope(value = WebApplicationContext.SCOPE_REQUEST)
public class QLUserCheckADminCRUDController extends AbstractController{
	private static final Logger log = LogManager.getLogger(QLUserCheckADminCRUDController.class);
	@Autowired RestAPIUtility restAPI;
	@Autowired RestTemplate restTemplate;
	
	private String errorCode;
	private String errorDesc;
	private String _id;
	private boolean activeFlag;
	private String userName;
	private String fullName;
	private String phone;
	private String email;
//	private String effectDate;
//	private String expireDate;

	
	
	
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
		
		
		LoginRes issu = cup.getLoginRes();
		String _idsu = issu.getIssuerId();

		BaseDTO baseDTO = new BaseDTO(req);
		Msg msg = baseDTO.createMsg(cup, Constants.MSG_ACTION_CODE.INQUIRY);
		HashMap<String, String> hData = new HashMap<>();
		msg.setObjData(hData);

		JSONRoot root = new JSONRoot(msg);
		
		_id = commons.getParameterFromRequest(req, "_id");
		String header = "Thêm mới user check";
		String action = "CREATE";
		boolean isEdit = false;
		
		req.setAttribute("NLap", commons.convertLocalDateTimeToString(LocalDate.now(), Constants.FORMAT_DATE.FORMAT_DATE_WEB));

		LocalDate now = LocalDate.now();
		req.setAttribute("EffectDate", commons.convertLocalDateTimeToString(now, Constants.FORMAT_DATE.FORMAT_DATE_WEB));
		req.setAttribute("ExpireDate", commons.convertLocalDateTimeToString(now.plus(12, ChronoUnit.MONTHS), Constants.FORMAT_DATE.FORMAT_DATE_WEB));
			
		switch (transaction) {
		case "qlUserCheckAdminCre":
			header = "Thêm mới user check";
			action = "CREATE";
			isEdit = true;
			break;
		case "qlUserCheckAdminEdit":
			header = "Thay đổi thông tin user check";
			action = "EDIT";
			isEdit = true;
			break;
		case "qlUserCheckAdminDetail":
			header = "Chi tiết user check";
			action = "DETAIL";
			isEdit = false;
			break;
			
		case "qlUserCheckAdminResetPassword":
			action = Constants.MSG_ACTION_CODE.RESET_PASSWORD;
			header = "Reset mật khẩu user check";
			isEdit = false;			
			break;
		case "qlUserCheckAdminActive":
			break;
		case "qlUserCheckAdminDeActive":	
			break;
		default:
			break;
		}
		
		if("|qlUserCheckAdminEdit|qlUserCheckAdminDetail|qlUserCheckAdminResetPassword|qlUserCheckAdminActive|qlUserCheckAdminDeActive|".indexOf(transaction) != -1
				|| "init-dc-tt".equals(method))
			inquiry(cup, locale, req, session, _id, action, transaction, method);
		if("qlUserCheckAdminCreate".equals(transaction)) {
			
		
		}

		req.setAttribute("_header_", header);
		req.setAttribute("_action_", action);
		req.setAttribute("_isedit_", isEdit);
		req.setAttribute("_id", _id);
		
		req.setAttribute("PrefixUserID", "USER_CHECK_");
		
		if("|qlUserCheckAdminCreate|qlUserCheckAdminEdit|".indexOf(transaction) != -1)
		
		if(!"".equals(errorDesc))
			req.setAttribute("messageError", errorDesc);
		
		return "user_check/qlUserCheckAdminCRUD";
	}

	private void inquiry(CurrentUserProfile cup, Locale locale, HttpServletRequest req, HttpSession session, String _id , String action, String transaction, String method) throws Exception{
		if("".equals(_id)) {
			errorCode = "NOT FOUND";
			errorDesc = "Không tìm thấy thông tin user check.";
			return;
		}
		BaseDTO baseDTO = new BaseDTO(req);
		Msg msg = baseDTO.createMsg(cup, Constants.MSG_ACTION_CODE.INQUIRY);
		HashMap<String, String> hData = new HashMap<>();
		msg.setObjData(hData);
		
		JSONRoot root = new JSONRoot(msg);
		MsgRsp rsp = restAPI.callAPINormal("/qlUserCheckAdmin/detail/" + _id, cup.getLoginRes().getToken(), HttpMethod.POST, root);
		MspResponseStatus rspStatus = rsp.getResponseStatus();
		if(rspStatus.getErrorCode() == 0) {
			JsonNode jsonData = Json.serializer().nodeFromObject(rsp.getObjData());
			req.setAttribute("UserName", commons.getTextJsonNode(jsonData.at("/UserName")));
			req.setAttribute("FullName", commons.getTextJsonNode(jsonData.at("/FullName")));
			req.setAttribute("Phone", commons.getTextJsonNode(jsonData.at("/Phone")));
			req.setAttribute("Email", commons.getTextJsonNode(jsonData.at("/Email")));
			
//			req.setAttribute("EffectDate",commons.convertLocalDateTimeToString(commons.convertLongToLocalDate(jsonData.at("/EffectDate").asLong()), Constants.FORMAT_DATE.FORMAT_DATE_WEB));
//			req.setAttribute("ExpireDate", commons.convertLocalDateTimeToString(commons.convertLongToLocalDate(jsonData.at("/ExpireDate").asLong()), Constants.FORMAT_DATE.FORMAT_DATE_WEB));
//			
	
		}else {
			errorDesc = rspStatus.getErrorDesc();
		}
	}
	
	public BaseDTO checkDataToAccept(HttpServletRequest req, HttpSession session, String transaction
			, CurrentUserProfile cup) throws Exception{
		BaseDTO dto = new BaseDTO();
		dto.setErrorCode(0);
		
		_id = commons.getParameterFromRequest(req, "_id").replaceAll("\\s", "");	
		userName = commons.getParameterFromRequest(req, "userName").replaceAll("\\s", "").toUpperCase();
		fullName = commons.getParameterFromRequest(req, "fullName").trim().replaceAll("\\s+", " ");
		phone = commons.getParameterFromRequest(req, "phone").trim().replaceAll("\\s+", " ");
		email = commons.getParameterFromRequest(req, "email").trim().replaceAll("\\s+", " ");		
//		effectDate = commons.getParameterFromRequest(req, "effectDate").replaceAll("\\s", "");
//		expireDate = commons.getParameterFromRequest(req, "expireDate").replaceAll("\\s", "");
		
				
		if("qlUserCheckAdminEdit".equals(transaction)) {
			if("".equals(_id)) {
				dto.setErrorCode(1);
				dto.getErrorMessages().add("Không tìm thấy thông tin user check.");
			}
		}
		LocalDate fromDate = null;
		LocalDate toDate = null;
		switch (transaction) {
		case "qlUserCheckAdminCre":
			if("".equals(userName)) {
				dto.setErrorCode(1);
				dto.getErrorMessages().add("Vui lòng nhập vào mã user check.");
			}else if(!commons.checkStringWithRegex(Constants.REGEX_CHECK.STRING_IS_USERNAME, userName)) {
				dto.setErrorCode(1);
				dto.getErrorMessages().add("Mã không đúng định dạng.");
			}
			
		
//			if("".equals(effectDate)) {
//				dto.setErrorCode(1);
//				dto.getErrorMessages().add("Vui lòng chọn ngày hiệu lực.");
//			}else if(!commons.checkLocalDate(effectDate, Constants.FORMAT_DATE.FORMAT_DATE_WEB)) {
//				dto.setErrorCode(1);
//				dto.getErrorMessages().add("Ngày hiệu lực không đúng định dạng.");
//			}
//			if("".equals(expireDate)) {
//				dto.setErrorCode(1);
//				dto.getErrorMessages().add("Vui lòng chọn ngày hết hạn.");
//			}else if(!commons.checkLocalDate(expireDate, Constants.FORMAT_DATE.FORMAT_DATE_WEB)) {
//				dto.setErrorCode(1);
//				dto.getErrorMessages().add("Ngày hết hạn không đúng định dạng.");
//			}
//			
//			fromDate = null;
//			toDate = null;			
//			if(dto.getErrorCode() == 0) {
//				fromDate = commons.convertStringToLocalDate(effectDate, Constants.FORMAT_DATE.FORMAT_DATE_WEB);
//				toDate = commons.convertStringToLocalDate(expireDate, Constants.FORMAT_DATE.FORMAT_DATE_WEB);
//				if(commons.compareLocalDate(fromDate, toDate) > 0) {
//					dto.setErrorCode(1);
//					dto.getErrorMessages().add("Vui lòng chọn ngày hết hạn lớn hơn ngày hiệu lực.");
//				}
//			}
			break;
						
		case "qlUserCheckAdminEdit":
			if("".equals(_id)) {
				dto.setErrorCode(1);
				dto.getErrorMessages().add("Không tìm thấy ID user check.");
			}			
			
//			if("".equals(effectDate)) {
//				dto.setErrorCode(1);
//				dto.getErrorMessages().add("Vui lòng chọn ngày hiệu lực.");
//			}else if(!commons.checkLocalDate(effectDate, Constants.FORMAT_DATE.FORMAT_DATE_WEB)) {
//				dto.setErrorCode(1);
//				dto.getErrorMessages().add("Ngày hiệu lực không đúng định dạng.");
//			}
//			if("".equals(expireDate)) {
//				dto.setErrorCode(1);
//				dto.getErrorMessages().add("Vui lòng chọn ngày hết hạn.");
//			}else if(!commons.checkLocalDate(expireDate, Constants.FORMAT_DATE.FORMAT_DATE_WEB)) {
//				dto.setErrorCode(1);
//				dto.getErrorMessages().add("Ngày hết hạn không đúng định dạng.");
//			}
//			
//			fromDate = null;
//			toDate = null;			
//			if(dto.getErrorCode() == 0) {
//				fromDate = commons.convertStringToLocalDate(effectDate, Constants.FORMAT_DATE.FORMAT_DATE_WEB);
//				toDate = commons.convertStringToLocalDate(expireDate, Constants.FORMAT_DATE.FORMAT_DATE_WEB);
//				if(commons.compareLocalDate(fromDate, toDate) > 0) {
//					dto.setErrorCode(1);
//					dto.getErrorMessages().add("Vui lòng chọn ngày hết hạn lớn hơn ngày hiệu lực.");
//				}
//			}
			break;
		case "qlUserCheckAdminActive":
		case "qlUserCheckAdminDeActive":
		case "qlUserCheckAdminResetPassword":
		case "qlUserCheckAdminDelete":
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
		String messageConfirm = "Bạn có muốn thêm mới không?";
		switch (transaction) {
		case "qlUserCheckAdminCre":
			messageConfirm = "Bạn có muốn thêm mới không?";
			break;
		case "qlUserCheckAdminEdit":
			messageConfirm = "Bạn có muốn thay đổi thông tin không?";
			break;
		case "qlUserCheckAdminActive":
			messageConfirm = "Bạn có chắc chắn muốn kích hoạt này không?";
			break;
		case "qlUserCheckAdminDeActive":
			messageConfirm = "Bạn có chắc chắn muốn hủy kích hoạt này không?";
			break;
		case "qlUserCheckAdminDelete":
			messageConfirm = "Bạn có chắc chắn muốn xóa này không?";
			break;
		case "qlUserCheckAdminResetPassword":
			messageConfirm = "Bạn có chắc chắn muốn reset mật khẩu này không?";
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
		case "qlUserCheckAdminCre": actionCode = Constants.MSG_ACTION_CODE.CREATED; break;
		case "qlUserCheckAdminEdit": actionCode = Constants.MSG_ACTION_CODE.MODIFY; break;
		case "qlUserCheckAdminActive": actionCode = Constants.MSG_ACTION_CODE.ACTIVE; break;
		case "qlUserCheckAdminDeActive": actionCode = Constants.MSG_ACTION_CODE.DEACTIVE;break;
		case "qlUserCheckAdminDelete": actionCode = Constants.MSG_ACTION_CODE.DELETE;break;		
		case "qlUserCheckAdminResetPassword": actionCode = Constants.MSG_ACTION_CODE.RESET_PASSWORD;break;
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
		hData.put("UserName", userName);
		hData.put("FullName", fullName);
		hData.put("Phone", phone);
		hData.put("Email", email);
//		hData.put("EffectDate", effectDate);
//		hData.put("ExpireDate", expireDate);

		msg.setObjData(hData);
		JSONRoot root = new JSONRoot(msg);
		MsgRsp rsp = restAPI.callAPINormal("/qlUserCheckAdmin/crud", cup.getLoginRes().getToken(), HttpMethod.POST, root);
		MspResponseStatus rspStatus = rsp.getResponseStatus();
		if(rspStatus.getErrorCode() == 0) {
			JsonNode jsonData = null;
			HashMap<String, Object> hR = null;
			dtoRes.setErrorCode(0);
			switch (transaction) {
			case "qlUserCheckAdminCre":
				jsonData = Json.serializer().nodeFromObject(rsp.getObjData());
				
				hR = new HashMap<String, Object>();
				hR.put("info", "Thêm mới thành công.");
				hR.put("password", commons.getTextJsonNode(jsonData.at("/Password")));
				hR.put("userName", commons.getTextJsonNode(jsonData.at("/UserName")));
				
				dtoRes.setResponseData(hR);
				break;
			case "qlUserCheckAdminEdit":
				dtoRes.setResponseData("Thay đổi thông tin thành công.");
				break;
			case "qlUserCheckAdminActive":
				dtoRes.setResponseData("Kích hoạt thông tin thành công.");
				break;
			case "qlUserCheckAdminDeActive":
				dtoRes.setResponseData("Hủy kích hoạt thông tin thành công.");
				break;
			case "qlUserCheckAdminDelete":
				dtoRes.setResponseData("Xóa thông tin thành công.");
				break;
			case "qlUserCheckAdminResetPassword":
				jsonData = Json.serializer().nodeFromObject(rsp.getObjData());
				
				hR = new HashMap<String, Object>();
				hR.put("info", "Reset mật khẩu thành công.");
				hR.put("password", commons.getTextJsonNode(jsonData.at("/Password")));
				
				dtoRes.setResponseData(hR);
				break;
			}
		}else {
			dtoRes.setErrorCode(rspStatus.getErrorCode());
			dtoRes.setResponseData(rspStatus.getErrorDesc());
		}
		return dtoRes;
	}
	
}
	