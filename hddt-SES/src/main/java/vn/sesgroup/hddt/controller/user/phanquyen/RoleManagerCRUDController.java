package vn.sesgroup.hddt.controller.user.phanquyen;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
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
	"/RolesManageCreate"
	, "/RolesManageDetail"
	, "/RolesManageEdit"
})
@Scope(value = WebApplicationContext.SCOPE_REQUEST)
public class RoleManagerCRUDController extends AbstractController{
	private static final Logger log = LogManager.getLogger(RoleManagerCRUDController.class);
	@Autowired RestAPIUtility restAPI;
	@Autowired RestTemplate restTemplate;
	
	private String errorCode;
	private String errorDesc;
	private String _id;
	private String roleId;
	private String roleName;
	private String chkActive;
	private String rightactions;
	private JsonNode jsonRightActions;
	
	
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
		String header = "Thêm mới nhóm quyền";
		String action = "CREATE";
		boolean isEdit = false;
		
		req.setAttribute("NLap", commons.convertLocalDateTimeToString(LocalDate.now(), Constants.FORMAT_DATE.FORMAT_DATE_WEB));

				
		switch (transaction) {
		case "RolesManageCreate":
			header = "Thêm mới nhóm quyền";
			action = "CREATE";
			isEdit = true;
			break;
		case "RolesManageEdit":
			header = "Thay đổi thông tin nhóm quyền";
			action = "EDIT";
			isEdit = true;
			break;
		case "RolesManageDetail":
			header = "Chi tiết nhóm quyền";
			action = "DETAIL";
			isEdit = false;
			break;
		case "RolesManageActive":
			break;
		case "RolesManageDeActive":	
			break;
		default:
			break;
		}
		
		if("|RolesManageEdit|RolesManageDetail|".indexOf(transaction) != -1
				|| "init-dc-tt".equals(method))
			inquiry(cup, locale, req, session, _id, action, transaction, method);
		if("RolesManageCreate".equals(transaction)) {
			
		
		}
		
		req.setAttribute("_header_", header);
		req.setAttribute("_action_", action);
		req.setAttribute("_isedit_", isEdit);
		req.setAttribute("_id", _id);
		
		if("|RolesManageCreate|RolesManageEdit|".indexOf(transaction) != -1)
		
		if(!"".equals(errorDesc))
			req.setAttribute("messageError", errorDesc);
		
		return "role-user/roleManagerCRUD";
	}

	private void inquiry(CurrentUserProfile cup, Locale locale, HttpServletRequest req, HttpSession session, String _id , String action, String transaction, String method) throws Exception{
		if("".equals(_id)) {
			errorCode = "NOT FOUND";
			errorDesc = "Không tìm thấy thông tin nhóm quyền.";
			return;
		}
		BaseDTO baseDTO = new BaseDTO(req);
		Msg msg = baseDTO.createMsg(cup, Constants.MSG_ACTION_CODE.INQUIRY);
		HashMap<String, String> hData = new HashMap<>();
		msg.setObjData(hData);
		
		JSONRoot root = new JSONRoot(msg);
		MsgRsp rsp = restAPI.callAPINormal("/roleManager/detail/" + _id, cup.getLoginRes().getToken(), HttpMethod.POST, root);
		MspResponseStatus rspStatus = rsp.getResponseStatus();
		if(rspStatus.getErrorCode() == 0) {
			JsonNode jsonData = Json.serializer().nodeFromObject(rsp.getObjData());
			req.setAttribute("RoleId", commons.getTextJsonNode(jsonData.at("/RoleId")));
			req.setAttribute("RoleName", commons.getTextJsonNode(jsonData.at("/RoleName")));
			req.setAttribute("ActiveFlag", jsonData.at("/ActiveFlag").asBoolean(false));
			req.setAttribute("CreateDate", commons.convertLocalDateTimeToString(commons.convertLongToLocalDate(jsonData.at("/InfoCreated/CreateDate").asLong()), Constants.FORMAT_DATE.FORMAT_DATE_WEB));
			req.setAttribute("CreateUser",  commons.getTextJsonNode(jsonData.at("/InfoCreated/CreateUserFullName")));
			
			StringJoiner sj = new StringJoiner("|", "|", "|");
			for(JsonNode o: jsonData.at("/FunctionRights")) {
				sj.add(commons.getTextJsonNode(o));
			}
			session.setAttribute(Constants.SESSION_TYPE.SESSION_FULL_MENU_RIGHTS, sj.toString());
	
		}else {
			errorDesc = rspStatus.getErrorDesc();
		}
	}
	
	public BaseDTO checkDataToAccept(HttpServletRequest req, HttpSession session, String transaction
			, CurrentUserProfile cup) throws Exception{
		BaseDTO dto = new BaseDTO();
		dto.setErrorCode(0);
		
		_id = commons.getParameterFromRequest(req, "_id").replaceAll("\\s", "");	
		roleId = commons.getParameterFromRequest(req, "roleId").replaceAll("\\s", "").toUpperCase();
		roleName = commons.getParameterFromRequest(req, "roleName").trim().replaceAll("\\s++", " ");
		chkActive = "on".equals(commons.getParameterFromRequest(req, "chkActive").replaceAll("\\s", ""))? "1": "0";
		rightactions = commons.getParameterFromRequest(req, "rightactions");
		try {
			jsonRightActions = Json.serializer().nodeFromJson(commons.decodeBase64ToString(rightactions));
		}catch(Exception e) {
			log.error(" >>>>> An exception occurred!", e);
		}
		
		if("RolesManageEdit".equals(transaction)) {
			if("".equals(_id)) {
				dto.setErrorCode(1);
				dto.getErrorMessages().add("Không tìm thấy thông tin nhóm quyền.");
			}
		}
	
		switch (transaction) {
		case "RolesManageCreate":
		case "RolesManageEdit":
			if("".equals(roleId)) {
				dto.setErrorCode(1);
				dto.getErrorMessages().add("Vui lòng nhập vào mã nhóm quyền.");
			}else if(!commons.checkStringWithRegex(Constants.REGEX_CHECK.STRING_IS_POSITIONS, roleId)) {
				dto.setErrorCode(1);
				dto.getErrorMessages().add("Mã nhóm quyền không đúng định dạng.");
			}
			if("".equals(roleName)) {
				dto.setErrorCode(1);
				dto.getErrorMessages().add("Vui lòng nhập vào tên nhóm quyền.");
			}
			break;
		case "RolesManageActive":
		case "RolesManageDeActive":
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
		String messageConfirm = "Bạn có muốn thêm mới nhóm quyền không?";
		switch (transaction) {
		case "RolesManageCreate":
			messageConfirm = "Bạn có muốn thêm mới nhóm quyền không?";
			break;
		case "RolesManageEdit":
			messageConfirm = "Bạn có muốn thay đổi thông tin nhóm quyền không?";
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
		case "RolesManageCreate": actionCode = Constants.MSG_ACTION_CODE.CREATED; break;
		case "RolesManageEdit": actionCode = Constants.MSG_ACTION_CODE.MODIFY; break;

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
		hData.put("RoleId", roleId);
		hData.put("RoleName", roleName);
		hData.put("ActiveFlag", chkActive);
		hData.put("JsonRightActions", jsonRightActions);
	
		msg.setObjData(hData);
		JSONRoot root = new JSONRoot(msg);
		MsgRsp rsp = restAPI.callAPINormal("/roleManager/crud", cup.getLoginRes().getToken(), HttpMethod.POST, root);
		MspResponseStatus rspStatus = rsp.getResponseStatus();
		if(rspStatus.getErrorCode() == 0) {
			dtoRes.setErrorCode(0);
			switch (transaction) {
			case "RolesManageCreate":
				dtoRes.setResponseData("Thêm mới thông tin nhóm quyền thành công.");
				break;
			case "RolesManageEdit":
				dtoRes.setResponseData("Cập nhật thông tin nhóm quyền thành công.");
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
	