package vn.sesgroup.hddt.controller.admin.support;

import java.security.Principal;
import java.time.LocalDate;
import java.time.temporal.ChronoField;
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
	"/mauhd_admin-edit",
	"/mauhd_admin-check"
})
@Scope(value = WebApplicationContext.SCOPE_REQUEST)
public class MauHD23AdminCRUDController extends AbstractController{
	private static final Logger log = LogManager.getLogger(MauHD23AdminCRUDController.class);
	@Autowired RestAPIUtility restAPI;
	@Autowired RestTemplate restTemplate;
	
	private String errorCode;
	private String errorDesc;
	private String _id;
	private String NamCanChuyenDoi;
	private String NamChuyenDoi;
	
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
		String header = "Thêm mới mẫu số 23";
		String action = "CREATE";
		boolean isEdit = false;
		
		req.setAttribute("NLap", commons.convertLocalDateTimeToString(LocalDate.now(), Constants.FORMAT_DATE.FORMAT_DATE_WEB));

				
		switch (transaction) {
		case "mauhd_admin-edit":
			header = "Thay đổi thông tin mẫu số 23";
			action = "EDIT";
			isEdit = true;
			break;
		default:
			break;
		}
		
		
		req.setAttribute("_header_", header);
		req.setAttribute("_action_", action);
		req.setAttribute("_isedit_", isEdit);
		req.setAttribute("_id", _id);
		
		if("|mauhd_admin-edit|".indexOf(transaction) != -1)
		
		if(!"".equals(errorDesc))
			req.setAttribute("messageError", errorDesc);

		
		return "support-admin/support-crud";
	}

	public BaseDTO checkDataToAccept(HttpServletRequest req, HttpSession session, String transaction
			, CurrentUserProfile cup) throws Exception{
		BaseDTO dto = new BaseDTO();
		dto.setErrorCode(0);
		
		_id = commons.getParameterFromRequest(req, "_id").replaceAll("\\s", "");
		switch (transaction) {
			
		case "mauhd_admin-edit":	
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
		String messageConfirm = "Bạn có muốn thêm mới mẫu số 23 không?";
		switch (transaction) {
		case "mauhd_admin-edit":
			messageConfirm = "Bạn có muốn thay đổi thông tin mẫu số 23 không?";
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
		case "mauhd_admin-edit": actionCode = Constants.MSG_ACTION_CODE.MODIFY; break;
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
	
		msg.setObjData(hData);
		JSONRoot root = new JSONRoot(msg);
		MsgRsp rsp = restAPI.callAPINormal("/mauhd_admin/crud", cup.getLoginRes().getToken(), HttpMethod.POST, root);
		MspResponseStatus rspStatus = rsp.getResponseStatus();
		if(rspStatus.getErrorCode() == 0) {
			dtoRes.setErrorCode(0);
			switch (transaction) {
			case "mauhd_admin-edit":
				dtoRes.setResponseData("Cập nhật thông tin mẫu số 23 thành công.");
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
	
	
	
	
	@RequestMapping(value = "/showCheck", method = { RequestMethod.POST, RequestMethod.GET })
	public String checkdb(Locale locale, Principal principal, HttpServletRequest req) throws Exception {
//		CurrentUserProfile cup = getCurrentlyAuthenticatedPrincipal();
//		LoginRes issu = cup.getLoginRes();
//
//		BaseDTO baseDTO = new BaseDTO(req);
//		Msg msg = baseDTO.createMsg(cup, Constants.MSG_ACTION_CODE.INQUIRY);
//		HashMap<String, String> hData = new HashMap<>();
//		msg.setObjData(hData);
//		JSONRoot root = new JSONRoot(msg);
//		MsgRsp rsp = restAPI.callAPINormal("/mauhd_admin/checkdb", cup.getLoginRes().getToken(), HttpMethod.POST, root);
//		MspResponseStatus rspStatus = rsp.getResponseStatus();
//		if (rspStatus.getErrorCode() == 0) {
//			//JsonNode jsonData = Json.serializer().nodeFromObject(rsp.getObjData());
//		
//			String SLMSHT = rspStatus.getErrorDesc();
//			
//			req.setAttribute("SLMSHT", SLMSHT);
//					
//	
//		} else {
//			rspStatus.getErrorDesc();
//		}

		return "/support-admin/mauhd_admin-db";
		
	}
	
	public BaseDTO checkDataToAcceptDB(HttpServletRequest req, HttpSession session, String transaction
			, CurrentUserProfile cup) throws Exception{
		BaseDTO dto = new BaseDTO();
		dto.setErrorCode(0);
		
		NamCanChuyenDoi = commons.getParameterFromRequest(req, "nam-can-chuyen-doi").replaceAll("\\s", "");
		NamChuyenDoi = commons.getParameterFromRequest(req, "nam-chuyen-doi").replaceAll("\\s", "");
		
			if("".equals(NamCanChuyenDoi)) {
				dto.setErrorCode(1);
				dto.getErrorMessages().add("Vui lòng nhập năm cần chuyển đổi.");
				return dto;
			}
			
			int currentYear = LocalDate.now().get(ChronoField.YEAR);
			
			if(!commons.checkStringIsInt(NamCanChuyenDoi) || commons.ToNumber(NamCanChuyenDoi) > currentYear) {
				dto.setErrorCode(1);
				dto.getErrorMessages().add("Năm cần chuyển đổi phải là số nguyên và không được lớn hơn năm hiện tại.");
				return dto;
			}
			
			if("".equals(NamChuyenDoi)) {
				dto.setErrorCode(1);
				dto.getErrorMessages().add("Vui lòng nhập năm chuyển đổi.");
				return dto;
			}
		
			if(!commons.checkStringIsInt(NamChuyenDoi) || commons.ToNumber(NamChuyenDoi) <= commons.ToNumber(NamCanChuyenDoi)) {
				dto.setErrorCode(1);
				dto.getErrorMessages().add("Năm cần chuyển đổi phải là số nguyên và không được bé hơn hoặc bằng năm cần chuyển đổi.");
				return dto;
			}
			
			double checkNam = commons.ToNumber(NamChuyenDoi) - 1;
			
			if(checkNam != commons.ToNumber(NamCanChuyenDoi)) {
				dto.setErrorCode(1);
				dto.getErrorMessages().add("Vui lòng nhập năm liền kề.");
				return dto;
			}
			
		return dto;
	}
	
	@RequestMapping(value = "/checkdb",  produces = MediaType.APPLICATION_JSON_VALUE, method = RequestMethod.POST)
	@ResponseBody
	public BaseDTO checkdb(Locale locale, HttpServletRequest req, HttpSession session
			, @RequestAttribute(name = "transaction", required = false, value = "") String transaction) throws Exception {
		String token = "";
		if (null != session.getAttribute(Constants.SESSION_TYPE.SESSION_TOKEN_EXECUTE)) {
			token = session.getAttribute(Constants.SESSION_TYPE.SESSION_TOKEN_EXECUTE).toString();
			session.removeAttribute(token);
		}
		session.removeAttribute(Constants.SESSION_TYPE.SESSION_TOKEN_EXECUTE);
		
		
		CurrentUserProfile cup = getCurrentlyAuthenticatedPrincipal();		
		
		BaseDTO dto = new BaseDTO();
		String messageConfirm = "Bạn có muốn cập nhật mẫu số hóa đơn không?";
		
		dto = checkDataToAcceptDB(req, session, transaction, cup);
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
	
	
	@RequestMapping(value = "/updatedb",  produces = MediaType.APPLICATION_JSON_VALUE, method = RequestMethod.POST)
	@ResponseBody
	public BaseDTO updatedb(HttpServletRequest req, HttpSession session
			, @RequestParam(value = "transaction", required = false, defaultValue = "") String transaction
			, @RequestParam(value = "tokenTransaction", required = false, defaultValue = "") String tokenTransaction) throws Exception{
		BaseDTO dtoRes = new BaseDTO();
		
		CurrentUserProfile cup = getCurrentlyAuthenticatedPrincipal();
		dtoRes = checkDataToAcceptDB(req, session, transaction, cup);
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
		
		String actionCode = Constants.MSG_ACTION_CODE.MODIFY;
	
		dtoRes = new BaseDTO(req);
		Msg msg = dtoRes.createMsg(cup, actionCode);
		HashMap<String, Object> hData = new HashMap<>();

		hData.put("NamCanChuyenDoi", NamCanChuyenDoi);
		hData.put("NamChuyenDoi", NamChuyenDoi);
		
		
		msg.setObjData(hData);
		JSONRoot root = new JSONRoot(msg);
		MsgRsp rsp = restAPI.callAPINormal("/mauhd_admin/updateDBTheoNam", cup.getLoginRes().getToken(), HttpMethod.POST, root);
		MspResponseStatus rspStatus = rsp.getResponseStatus();
		if(rspStatus.getErrorCode() == 0) {
			dtoRes.setErrorCode(0);
			dtoRes.setResponseData("Cập nhật thông tin thành công.");
		}else {
			dtoRes.setErrorCode(rspStatus.getErrorCode());
			dtoRes.setResponseData(rspStatus.getErrorDesc());
		}
		return dtoRes;

	}
	
	
}
	