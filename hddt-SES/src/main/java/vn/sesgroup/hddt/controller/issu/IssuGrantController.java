package vn.sesgroup.hddt.controller.issu;


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
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;

import vn.sesgroup.hddt.controller.AbstractController;
import vn.sesgroup.hddt.dto.BaseDTO;
import vn.sesgroup.hddt.dto.CurrentUserProfile;
import vn.sesgroup.hddt.resources.RestAPIUtility;
import vn.sesgroup.hddt.utils.Constants;
import vn.sesgroup.hddt.utils.Json;
@Controller
@RequestMapping({
	"/issu-grant"
})

@Scope(value = WebApplicationContext.SCOPE_REQUEST)
public class IssuGrantController extends AbstractController{
	@Autowired RestAPIUtility restAPI;
	
	private String ids;
	private String subadmins;

	private void LoadParameter(
			CurrentUserProfile cup, 
			Locale locale,
			HttpServletRequest req, 
			String action
			) {
		try {
			BaseDTO baseDTO = new BaseDTO(req);
			Msg msg = baseDTO.createMsg(cup, Constants.MSG_ACTION_CODE.LOAD_PARAMS);

			/* DANH SACH THAM SO */
			MsgParam msgParam = null;
			MsgParams msgParams = new MsgParams();

			msgParam = new MsgParam();
			msgParam.setId("param01");
			msgParam.setParam("DMSubAdmin");
			msgParams.getParams().add(msgParam);

			/* END: DANH SACH THAM SO */
			msg.setObjData(msgParams);

			JSONRoot root = new JSONRoot(msg);
			MsgRsp rsp = restAPI.callAPINormal("/commons/get-full-params", cup.getLoginRes().getToken(),
					HttpMethod.POST, root);
			MspResponseStatus rspStatus = rsp.getResponseStatus();

			if (rspStatus.getErrorCode() == 0 && rsp.getObjData() != null) {
				LinkedHashMap<String, String> hItem = null;

				JsonNode jsonData = Json.serializer().nodeFromObject(rsp.getObjData());

				if (null != jsonData.at("/param01") && jsonData.at("/param01") instanceof ArrayNode) {
					hItem = new LinkedHashMap<String, String>();
					for (JsonNode o : jsonData.at("/param01")) {
							hItem.put(commons.getTextJsonNode(o.get("_id")), 
//									commons.getTextJsonNode(o.get("UserName")) + " - " + 
							commons.getTextJsonNode(o.get("FullName")));
					}
					req.setAttribute("map_subadmin", hItem);
				}
			}

		} catch (Exception e) {
		}
	}

	@RequestMapping(value = "/init", method = {RequestMethod.POST})
	public String init(
			Locale locale, 
			HttpServletRequest req, 
			HttpSession session,
			@RequestAttribute(name = "transaction", value = "", required = false) String transaction, 
			String action
			) throws Exception{
		CurrentUserProfile cup = getCurrentlyAuthenticatedPrincipal();
		req.setAttribute("_header_", "Gán Khách hàng cho Sub-Admin");
		LoadParameter(cup, locale, req, action);
		ids = commons.getParameterFromRequest(req, "_ids");
		req.setAttribute("IDS", ids);
		String[] listId = ids.split(",");
		
		if(listId.length == 1) {
			String id = listId[0];
			BaseDTO baseDTO = new BaseDTO(req);
			Msg msg = baseDTO.createMsg(cup, Constants.MSG_ACTION_CODE.INQUIRY);
			HashMap<String, String> hData = new HashMap<>();
			msg.setObjData(hData);
			
			JSONRoot root = new JSONRoot(msg);
			MsgRsp rsp = restAPI.callAPINormal("/main/profile/" + id, cup.getLoginRes().getToken(), HttpMethod.POST, root);
			MspResponseStatus rspStatus = rsp.getResponseStatus();
			if(rspStatus.getErrorCode() == 0) {
				JsonNode jsonData = Json.serializer().nodeFromObject(rsp.getObjData());
				ArrayList<Object> userManagedIds = new ArrayList<Object>();
				if(null != jsonData.at("/ManagedByUsers")) {
					for(JsonNode o: jsonData.at("/ManagedByUsers")) {
						userManagedIds.add(commons.getTextJsonNode(o));
					}
				}			
				req.setAttribute("users_managed", userManagedIds);
			}
		}

		return "issu/issu-grant";
	}
	
	public BaseDTO checkDataToImport(HttpServletRequest req, HttpSession session, String transaction
			, CurrentUserProfile cup) throws Exception{
		BaseDTO dto = new BaseDTO();
		dto.setErrorCode(0);
		ids = commons.getParameterFromRequest(req, "customerIds").replaceAll("\\s", "");
		subadmins = commons.getParameterFromRequest(req, "sub-admins").replaceAll("\\s", "");
		if("W10=".equals(subadmins)) {
			dto.setErrorCode(1);
			dto.getErrorMessages().add("Vui lòng chọn sub-admin.");
		}
		
		return dto;
	}
	
	@RequestMapping(value = "/check-data-grant",  produces = MediaType.APPLICATION_JSON_VALUE, method = RequestMethod.POST)
	@ResponseBody
	public BaseDTO execCheckDataToImport(
			Locale locale, 
			HttpServletRequest req, 
			HttpSession session,
			@RequestParam(value = "transaction", required = false, defaultValue = "") String transaction
			) throws Exception {
		String token = "";
		if (null != session.getAttribute(Constants.SESSION_TYPE.SESSION_TOKEN_EXECUTE)) {
			token = session.getAttribute(Constants.SESSION_TYPE.SESSION_TOKEN_EXECUTE).toString();
			session.removeAttribute(token);
		}
		session.removeAttribute(Constants.SESSION_TYPE.SESSION_TOKEN_EXECUTE);

		BaseDTO dto = new BaseDTO();	
		String messageConfirm = "Bạn có muốn thực hiện gán không?";
		CurrentUserProfile cup = getCurrentlyAuthenticatedPrincipal();
		dto = checkDataToImport(req, session, transaction, cup);
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
	
	@RequestMapping(value = "/grant", produces = MediaType.APPLICATION_JSON_VALUE, method = RequestMethod.POST)
	@ResponseBody
	public BaseDTO execImportData(
			HttpServletRequest req,
			HttpSession session,
			@RequestParam(value = "transaction", required = false, defaultValue = "") String transaction,
			@RequestParam(value = "tokenTransaction", required = false, defaultValue = "") String tokenTransaction)
			throws Exception {
		BaseDTO dtoRes = new BaseDTO();

		CurrentUserProfile cup = getCurrentlyAuthenticatedPrincipal();
		dtoRes = checkDataToImport(req, session, transaction, cup);
		if (0 != dtoRes.getErrorCode()) {
			dtoRes.setErrorCode(999);
			dtoRes.setResponseData(Constants.MAP_ERROR.get(999));
			return dtoRes;
		}

		/* CHECK TOKEN */
		String token = session.getAttribute(Constants.SESSION_TYPE.SESSION_TOKEN_EXECUTE) == null ? ""
				: session.getAttribute(Constants.SESSION_TYPE.SESSION_TOKEN_EXECUTE).toString();
		session.removeAttribute(Constants.SESSION_TYPE.SESSION_TOKEN_EXECUTE);
		if ("".equals(token) || !tokenTransaction.equals(token)) {
			dtoRes.setErrorCode(997);
			dtoRes.setResponseData(Constants.MAP_ERROR.get(997));
			return dtoRes;
		}
		/* END: CHECK TOKEN */

		dtoRes = new BaseDTO(req);
		Msg msg = dtoRes.createMsg(cup, Constants.MSG_ACTION_CODE.CREATED);
		HashMap<String, Object> hData = new HashMap<>();
		
		String subAdminsString = commons.decodeBase64ToString(subadmins);
		ObjectMapper mapper = new ObjectMapper();
		String subAdminIds = String.join(",", mapper.readValue(subAdminsString, new TypeReference<List<String>>() {}));
		
		hData.put("SUBADMINS", subAdminIds);
		hData.put("CUSTOMERS", ids);
		msg.setObjData(hData);
		JSONRoot root = new JSONRoot(msg);
		MsgRsp rsp = restAPI.callAPINormal("/issu/grant-issu-to-subadmin", cup.getLoginRes().getToken(),
				HttpMethod.POST, root);

		MspResponseStatus rspStatus = rsp.getResponseStatus();
		if (rspStatus.getErrorCode() == 0) {
			dtoRes.setErrorCode(0);
			dtoRes.setResponseData("Gán khách hàng cho Sub-Admin thành công.");
		} else {
			dtoRes.setErrorCode(rspStatus.getErrorCode());
			dtoRes.setResponseData(rspStatus.getErrorDesc());
		}
		return dtoRes;
	}

}
