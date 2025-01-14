package vn.sesgroup.hddt.controller.admin.phanquyen;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.StringJoiner;

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
import com.fasterxml.jackson.core.type.TypeReference;
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
	"/createUserAdminCre"
	,"/createUserAdminDetail"
	,"/createUserAdminEdit"
	,"/createUserAdminActive"
	,"/createUserAdminDeActive"
	,"/createUserAdminDelete"
	,"/createUserAdminResetPassword"
})
@Scope(value = WebApplicationContext.SCOPE_REQUEST)
public class CreateUserADminCRUDController extends AbstractController{
	private static final Logger log = LogManager.getLogger(CreateUserADminCRUDController.class);
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
	private String chucDanh;
	private String roleId;
	private String effectDate;
	private String expireDate;
	private String hasRetired;
	
	
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
			msgParam.setParam("RolesRightManage");
			msgParams.getParams().add(msgParam);
			
			msg.setObjData(msgParams);
			
			JSONRoot root = new JSONRoot(msg);
			MsgRsp rsp = restAPI.callAPINormal("/commons/get-full-params", cup.getLoginRes().getToken(), HttpMethod.POST, root);
			MspResponseStatus rspStatus = rsp.getResponseStatus();
			
			if(rspStatus.getErrorCode() == 0 && rsp.getObjData() != null) {
				LinkedHashMap<String, String> hItem = null;
				ArrayList<Object> arrTmp = null;
				
				
				JsonNode jsonData = Json.serializer().nodeFromObject(rsp.getObjData());
				if(null != jsonData.at("/param01")) {
					arrTmp = new ArrayList<Object>();
					for(JsonNode o: jsonData.at("/param01")) {
						arrTmp.add(Json.serializer().fromNode(o, new TypeReference<HashMap<String, String>>() {
						}));	
					}
					req.setAttribute("RolesRightList", arrTmp);
				}
			
				
			
			}
			
		}catch(Exception e) {}
	}
	
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
		String header = "Thêm mới người dùng";
		String action = "CREATE";
		boolean isEdit = false;
		
		req.setAttribute("NLap", commons.convertLocalDateTimeToString(LocalDate.now(), Constants.FORMAT_DATE.FORMAT_DATE_WEB));

		LocalDate now = LocalDate.now();
		req.setAttribute("EffectDate", commons.convertLocalDateTimeToString(now, Constants.FORMAT_DATE.FORMAT_DATE_WEB));
		req.setAttribute("ExpireDate", commons.convertLocalDateTimeToString(now.plus(12, ChronoUnit.MONTHS), Constants.FORMAT_DATE.FORMAT_DATE_WEB));
			
		switch (transaction) {
		case "createUserAdminCre":
			header = "Thêm mới người dùng";
			action = "CREATE";
			isEdit = true;
			break;
		case "createUserAdminEdit":
			header = "Thay đổi thông tin người dùng";
			action = "EDIT";
			isEdit = true;
			break;
		case "createUserAdminDetail":
			header = "Chi tiết người dùng";
			action = "DETAIL";
			isEdit = false;
			break;
			
		case "createUserAdminResetPassword":
			action = Constants.MSG_ACTION_CODE.RESET_PASSWORD;
			header = "Reset mật khẩu người dùng";
			isEdit = false;			
			break;
		case "createUserAdminActive":
			break;
		case "createUserAdminDeActive":	
			break;
		default:
			break;
		}
		
		if("|createUserAdminEdit|createUserAdminDetail|createUserAdminResetPassword|createUserAdminActive|createUserAdminDeActive|".indexOf(transaction) != -1
				|| "init-dc-tt".equals(method))
			inquiry(cup, locale, req, session, _id, action, transaction, method);
		if("createUserAdminCreate".equals(transaction)) {
			
		
		}
		LoadParameter(cup, locale, req, action);
		req.setAttribute("_header_", header);
		req.setAttribute("_action_", action);
		req.setAttribute("_isedit_", isEdit);
		req.setAttribute("_id", _id);
		
		req.setAttribute("PrefixUserID", cup.getLoginRes().getUserName() + "_");
		
		if("|createUserAdminCreate|createUserAdminEdit|".indexOf(transaction) != -1)
		
		if(!"".equals(errorDesc))
			req.setAttribute("messageError", errorDesc);
		
		return "role-user-admin/createUserAdminCRUD";
	}

	private void inquiry(CurrentUserProfile cup, Locale locale, HttpServletRequest req, HttpSession session, String _id , String action, String transaction, String method) throws Exception{
		if("".equals(_id)) {
			errorCode = "NOT FOUND";
			errorDesc = "Không tìm thấy thông tin người dùng.";
			return;
		}
		BaseDTO baseDTO = new BaseDTO(req);
		Msg msg = baseDTO.createMsg(cup, Constants.MSG_ACTION_CODE.INQUIRY);
		HashMap<String, String> hData = new HashMap<>();
		msg.setObjData(hData);
		
		JSONRoot root = new JSONRoot(msg);
		MsgRsp rsp = restAPI.callAPINormal("/createUserAdmin/detail/" + _id, cup.getLoginRes().getToken(), HttpMethod.POST, root);
		MspResponseStatus rspStatus = rsp.getResponseStatus();
		if(rspStatus.getErrorCode() == 0) {
			JsonNode jsonData = Json.serializer().nodeFromObject(rsp.getObjData());
			req.setAttribute("UserName", commons.getTextJsonNode(jsonData.at("/UserName")));
			req.setAttribute("FullName", commons.getTextJsonNode(jsonData.at("/FullName")));
			req.setAttribute("Phone", commons.getTextJsonNode(jsonData.at("/Phone")));
			req.setAttribute("Email", commons.getTextJsonNode(jsonData.at("/Email")));
			
			req.setAttribute("EffectDate",commons.convertLocalDateTimeToString(commons.convertLongToLocalDate(jsonData.at("/EffectDate").asLong()), Constants.FORMAT_DATE.FORMAT_DATE_WEB));
			req.setAttribute("ExpireDate", commons.convertLocalDateTimeToString(commons.convertLongToLocalDate(jsonData.at("/ExpireDate").asLong()), Constants.FORMAT_DATE.FORMAT_DATE_WEB));
			
			req.setAttribute("PositionName", commons.getTextJsonNode(jsonData.at("/PositionName")));
			req.setAttribute("RolesRightId", commons.getTextJsonNode(jsonData.at("/RolesRightManageInfo/_id")));
			req.setAttribute("RolesRightName", commons.getTextJsonNode(jsonData.at("/RolesRightManageInfo/RoleName")));
			System.out.println(jsonData.at("/HasRetired"));
			req.setAttribute("HasRetired", jsonData.at("/HasRetired").asBoolean());
	
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
		chucDanh = commons.getParameterFromRequest(req, "chuc-danh").replaceAll("\\s", " ");
		roleId = commons.getParameterFromRequest(req, "roleId").replaceAll("\\s", "");
		effectDate = commons.getParameterFromRequest(req, "effectDate").replaceAll("\\s", "");
		expireDate = commons.getParameterFromRequest(req, "expireDate").replaceAll("\\s", "");
		hasRetired = commons.getParameterFromRequest(req, "hasRetired").replaceAll("\\s", "");
		
		
		if("createUserAdminEdit".equals(transaction)) {
			if("".equals(_id)) {
				dto.setErrorCode(1);
				dto.getErrorMessages().add("Không tìm thấy thông tin người dùng.");
			}
		}
		LocalDate fromDate = null;
		LocalDate toDate = null;
		switch (transaction) {
		case "createUserAdminCre":
			if("".equals(userName)) {
				dto.setErrorCode(1);
				dto.getErrorMessages().add("Vui lòng nhập vào mã người dùng.");
			}else if(!commons.checkStringWithRegex(Constants.REGEX_CHECK.STRING_IS_USERNAME, userName)) {
				dto.setErrorCode(1);
				dto.getErrorMessages().add("Mã người dùng không đúng định dạng.");
			}
			if("".equals(fullName)) {
				dto.setErrorCode(1);
				dto.getErrorMessages().add("Vui lòng nhập vào tên người dùng.");
			}
			if("".equals(chucDanh)) {
				dto.setErrorCode(1);
				dto.getErrorMessages().add("Vui lòng nhập chức danh người dùng.");
			}
			if("".equals(roleId)) {
				dto.setErrorCode(1);
				dto.getErrorMessages().add("Vui lòng chọn nhóm quyền người dùng.");
			}
			if("".equals(effectDate)) {
				dto.setErrorCode(1);
				dto.getErrorMessages().add("Vui lòng chọn ngày hiệu lực.");
			}else if(!commons.checkLocalDate(effectDate, Constants.FORMAT_DATE.FORMAT_DATE_WEB)) {
				dto.setErrorCode(1);
				dto.getErrorMessages().add("Ngày hiệu lực không đúng định dạng.");
			}
			if("".equals(expireDate)) {
				dto.setErrorCode(1);
				dto.getErrorMessages().add("Vui lòng chọn ngày hết hạn.");
			}else if(!commons.checkLocalDate(expireDate, Constants.FORMAT_DATE.FORMAT_DATE_WEB)) {
				dto.setErrorCode(1);
				dto.getErrorMessages().add("Ngày hết hạn không đúng định dạng.");
			}
			
			fromDate = null;
			toDate = null;			
			if(dto.getErrorCode() == 0) {
				fromDate = commons.convertStringToLocalDate(effectDate, Constants.FORMAT_DATE.FORMAT_DATE_WEB);
				toDate = commons.convertStringToLocalDate(expireDate, Constants.FORMAT_DATE.FORMAT_DATE_WEB);
				if(commons.compareLocalDate(fromDate, toDate) > 0) {
					dto.setErrorCode(1);
					dto.getErrorMessages().add("Vui lòng chọn ngày hết hạn lớn hơn ngày hiệu lực.");
				}
			}
			break;
						
		case "createUserAdminEdit":
			if("".equals(_id)) {
				dto.setErrorCode(1);
				dto.getErrorMessages().add("Không tìm thấy ID người dùng.");
			}
			if("".equals(fullName)) {
				dto.setErrorCode(1);
				dto.getErrorMessages().add("Vui lòng nhập vào tên người dùng.");
			}
			if("".equals(chucDanh)) {
				dto.setErrorCode(1);
				dto.getErrorMessages().add("Vui lòng nhập chức danh người dùng.");
			}
			if("".equals(roleId)) {
				dto.setErrorCode(1);
				dto.getErrorMessages().add("Vui lòng chọn nhóm quyền người dùng.");
			}
			if("".equals(effectDate)) {
				dto.setErrorCode(1);
				dto.getErrorMessages().add("Vui lòng chọn ngày hiệu lực.");
			}else if(!commons.checkLocalDate(effectDate, Constants.FORMAT_DATE.FORMAT_DATE_WEB)) {
				dto.setErrorCode(1);
				dto.getErrorMessages().add("Ngày hiệu lực không đúng định dạng.");
			}
			if("".equals(expireDate)) {
				dto.setErrorCode(1);
				dto.getErrorMessages().add("Vui lòng chọn ngày hết hạn.");
			}else if(!commons.checkLocalDate(expireDate, Constants.FORMAT_DATE.FORMAT_DATE_WEB)) {
				dto.setErrorCode(1);
				dto.getErrorMessages().add("Ngày hết hạn không đúng định dạng.");
			}
			
			fromDate = null;
			toDate = null;			
			if(dto.getErrorCode() == 0) {
				fromDate = commons.convertStringToLocalDate(effectDate, Constants.FORMAT_DATE.FORMAT_DATE_WEB);
				toDate = commons.convertStringToLocalDate(expireDate, Constants.FORMAT_DATE.FORMAT_DATE_WEB);
				if(commons.compareLocalDate(fromDate, toDate) > 0) {
					dto.setErrorCode(1);
					dto.getErrorMessages().add("Vui lòng chọn ngày hết hạn lớn hơn ngày hiệu lực.");
				}
			}
			break;
		case "createUserAdminActive":
		case "createUserAdminDeActive":
		case "createUserAdminResetPassword":
		case "createUserAdminDelete":
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
		String messageConfirm = "Bạn có muốn thêm mới người dùng không?";
		switch (transaction) {
		case "createUserAdminCre":
			messageConfirm = "Bạn có muốn thêm mới người dùng không?";
			break;
		case "createUserAdminEdit":
			messageConfirm = "Bạn có muốn thay đổi thông tin người dùng không?";
			break;
		case "createUserAdminActive":
			messageConfirm = "Bạn có chắc chắn muốn kích hoạt người dùng này không?";
			break;
		case "createUserAdminDeActive":
			messageConfirm = "Bạn có chắc chắn muốn hủy kích hoạt người dùng này không?";
			break;
		case "createUserAdminDelete":
			messageConfirm = "Bạn có chắc chắn muốn xóa người dùng này không?";
			break;
		case "createUserAdminResetPassword":
			messageConfirm = "Bạn có chắc chắn muốn reset mật khẩu người dùng này không?";
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
		case "createUserAdminCre": actionCode = Constants.MSG_ACTION_CODE.CREATED; break;
		case "createUserAdminEdit": actionCode = Constants.MSG_ACTION_CODE.MODIFY; break;
		case "createUserAdminActive": actionCode = Constants.MSG_ACTION_CODE.ACTIVE; break;
		case "createUserAdminDeActive": actionCode = Constants.MSG_ACTION_CODE.DEACTIVE;break;
		case "createUserAdminDelete": actionCode = Constants.MSG_ACTION_CODE.DELETE;break;		
		case "createUserAdminResetPassword": actionCode = Constants.MSG_ACTION_CODE.RESET_PASSWORD;break;
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
		hData.put("PositionName", chucDanh);
		hData.put("RoleId", roleId);
		hData.put("EffectDate", effectDate);
		hData.put("ExpireDate", expireDate);
		hData.put("HasRetired", hasRetired);
	
		msg.setObjData(hData);
		JSONRoot root = new JSONRoot(msg);
		MsgRsp rsp = restAPI.callAPINormal("/createUserAdmin/crud", cup.getLoginRes().getToken(), HttpMethod.POST, root);
		MspResponseStatus rspStatus = rsp.getResponseStatus();
		if(rspStatus.getErrorCode() == 0) {
			JsonNode jsonData = null;
			HashMap<String, Object> hR = null;
			dtoRes.setErrorCode(0);
			switch (transaction) {
			case "createUserAdminCre":
				jsonData = Json.serializer().nodeFromObject(rsp.getObjData());
				
				hR = new HashMap<String, Object>();
				hR.put("info", "Thêm mới người dùng thành công.");
				hR.put("password", commons.getTextJsonNode(jsonData.at("/Password")));
				hR.put("userName", commons.getTextJsonNode(jsonData.at("/UserName")));
				
				dtoRes.setResponseData(hR);
				break;
			case "createUserAdminEdit":
				dtoRes.setResponseData("Thay đổi thông tin người dùng thành công.");
				break;
			case "createUserAdminActive":
				dtoRes.setResponseData("Kích hoạt thông tin người dùng thành công.");
				break;
			case "createUserAdminDeActive":
				dtoRes.setResponseData("Hủy kích hoạt thông tin người dùng thành công.");
				break;
			case "createUserAdminDelete":
				dtoRes.setResponseData("Xóa thông tin người dùng thành công.");
				break;
			case "createUserAdminResetPassword":
				jsonData = Json.serializer().nodeFromObject(rsp.getObjData());
				
				hR = new HashMap<String, Object>();
				hR.put("info", "Reset mật khẩu người dùng thành công.");
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
	