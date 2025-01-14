package vn.sesgroup.hddt.controller.user.phanquyen;

import java.security.Principal;
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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.context.WebApplicationContext;

import com.api.message.JSONRoot;
import com.api.message.Msg;
import com.api.message.MsgPage;
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
import vn.sesgroup.hddt.resources.RestAPIUtility;
import vn.sesgroup.hddt.utils.Constants;
import vn.sesgroup.hddt.utils.Json;

@Controller
@RequestMapping("/createUser")
@Scope(value = WebApplicationContext.SCOPE_REQUEST)
public class CreateUserController extends AbstractController{
	private static final Logger log = LogManager.getLogger(CreateUserController.class);
	@Autowired RestAPIUtility restAPI; 
	
	private String _id;
	private String userName;
	private String fullName;
	private String phone;
	private String email;
	
	
	@RequestMapping(value = "/init", method = {RequestMethod.POST, RequestMethod.GET})
	public String init(Locale locale, Principal principal, HttpServletRequest req) throws Exception{
		req.setAttribute("_TitleView_", Constants.PREFIX_TITLE + " - Danh sách tài khoản người dùng");
		return "role-user/createUser";
	}
	
	private BaseDTO checkDataSearch(Locale locale, HttpServletRequest req, HttpSession session) {
		BaseDTO dto = new BaseDTO();
		dto.setErrorCode(0);
		
		userName = commons.getParameterFromRequest(req, "userName").replaceAll("\\s", "").toUpperCase();
		fullName = commons.getParameterFromRequest(req, "fullName").trim().replaceAll("\\s+", " ");
		phone = commons.getParameterFromRequest(req, "phone").trim().replaceAll("\\s+", " ");
		email = commons.getParameterFromRequest(req, "email").replaceAll("\\s", "");
		return dto;
	}
	
	@RequestMapping(value = "/search",  produces = MediaType.APPLICATION_JSON_VALUE, method = RequestMethod.POST)
	@ResponseBody
	public BaseDTO execSearch(Locale locale, HttpServletRequest req, HttpSession session) throws Exception{
		JsonGridDTO grid = new JsonGridDTO();
		
		BaseDTO baseDTO = checkDataSearch(locale, req, session);
		if(0 != baseDTO.getErrorCode()) {
			grid.setErrorCode(baseDTO.getErrorCode());
			grid.setErrorMessages(baseDTO.getErrorMessages());
			grid.setResponseData(Constants.MAP_ERROR.get(999));
			return grid;
		}
		
		CurrentUserProfile cup = getCurrentlyAuthenticatedPrincipal();		
		baseDTO = new BaseDTO(req);
		Msg msg = baseDTO.createMsg(cup, Constants.MSG_ACTION_CODE.SEARCH);
		
		HashMap<String, Object> hData = new HashMap<>();

		hData.put("UserName", userName);
		hData.put("FullName", fullName);
		hData.put("Phone", phone);
		hData.put("Email", email);

		msg.setObjData(hData);
		
		JSONRoot root = new JSONRoot(msg);
		MsgRsp rsp = restAPI.callAPINormal("/createUser/list", cup.getLoginRes().getToken(), HttpMethod.POST, root);
		MspResponseStatus rspStatus = rsp.getResponseStatus();
		if(rspStatus.getErrorCode() == 0) {
			MsgPage page = rsp.getMsgPage();
			grid.setTotal(page.getTotalRows());
			
			JsonNode jsonData = Json.serializer().nodeFromObject(rsp.getObjData());
			JsonNode rows = null;
			HashMap<String, String> hItem = null;
			if(!jsonData.at("/rows").isMissingNode()) {
				rows = jsonData.at("/rows");
				for(JsonNode row: rows) {
					hItem = new HashMap<String, String>();		
					
					String trangthai = "";
					String is_root = "";
					String active = "";
					boolean ActiveFlag = row.at("/IsActive").asBoolean();
					boolean IsRoleRoot = row.at("/IsRoleRoot").asBoolean();
					if(ActiveFlag == true) {
						trangthai = "Đã kích hoạt";
						active = "1";
					}else {
						trangthai = "Chưa kích hoạt";
						active = "0";
					}
					if(IsRoleRoot == true) {
						is_root = "1";
				
					}else {
						is_root = "0";
				
					}
					
					
					hItem.put("_id", commons.getTextJsonNode(row.at("/_id")));
					hItem.put("userName", commons.getTextJsonNode(row.at("/UserName")));
					hItem.put("fullName", commons.getTextJsonNode(row.at("/FullName")));
					hItem.put("phone", commons.getTextJsonNode(row.at("/Phone")));
					hItem.put("email", commons.getTextJsonNode(row.at("/Email")));				
					hItem.put("activeDeactiveDesc", trangthai);
					hItem.put("active", active);
					hItem.put("isRoot", is_root);
					hItem.put("createDate", commons.getTextJsonNode(row.at("/CreateDate")));
					hItem.put("createUser", commons.getTextJsonNode(row.at("/CreateUserFullName")));
					hItem.put("updateDate", commons.getTextJsonNode(row.at("/UpdatedDate")));
					hItem.put("updateUser", commons.getTextJsonNode(row.at("/UpdatedUserFullName")));
					
					hItem.put("effectDate", commons.getTextJsonNode(row.at("/EffectDate")));
					hItem.put("expireDate", commons.getTextJsonNode(row.at("/ExpireDate")));
					
					hItem.put("roleRightName", commons.getTextJsonNode(row.at("/RolesRightManageInfo/RoleName")));

					
					grid.getRows().add(hItem);
				}
			}
			
		}else {
			grid = new JsonGridDTO();
			grid.setErrorCode(rspStatus.getErrorCode());
			grid.setResponseData(rspStatus.getErrorDesc());
		}
		
		return grid;
	}
	

	

}
