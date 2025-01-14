package vn.sesgroup.hddt.controller.admin.api;

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
@RequestMapping("/role_api")
@Scope(value = WebApplicationContext.SCOPE_REQUEST)
public class SetRoleController extends AbstractController{
	private static final Logger log = LogManager.getLogger(SetRoleController.class);
	@Autowired RestAPIUtility restAPI; 
	
	private String _id;
	private String mst;
	private String name;
	private String acti;
	
	
	@RequestMapping(value = "/init", method = {RequestMethod.POST, RequestMethod.GET})
	public String init(Locale locale, Principal principal, HttpServletRequest req) throws Exception{
		req.setAttribute("_TitleView_", Constants.PREFIX_TITLE + " - Set Role API");
		req.setAttribute("map_status", Constants.MAP_STATUS);
		return "api-admin/role_api";
	}
	
	private BaseDTO checkDataSearch(Locale locale, HttpServletRequest req, HttpSession session) {
		BaseDTO dto = new BaseDTO();
		dto.setErrorCode(0);
		
		mst = commons.getParameterFromRequest(req, "mst").replaceAll("\\s", "");
		name = commons.getParameterFromRequest(req, "name").replaceAll("\\s", " ");
		acti = commons.getParameterFromRequest(req, "acti").replaceAll("\\s", "");
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
		hData.put("mst", mst);
		hData.put("name", name);
		hData.put("acti", acti);
		msg.setObjData(hData);
		
		JSONRoot root = new JSONRoot(msg);
		MsgRsp rsp = restAPI.callAPINormal("/role_api/list", cup.getLoginRes().getToken(), HttpMethod.POST, root);
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
					String set_vtro = "";
					String vt = commons.getTextJsonNode(row.at("/roles"));
					String check = "false";
					if(vt.equals("")) {
						set_vtro = "Chưa có quyền kết nối";
					}else if(vt.equals("ROLE_ADMIN")) {
						set_vtro = "Có quyền kết nối";
						check = "true";
					}else {
						set_vtro = "Chưa có quyền kết nối";
					}
					
					String trangthai = "";
					String tt =  commons.getTextJsonNode(row.at("/IsActive"));
					if(tt.equals("true")) {
						trangthai = "Hoạt động";
					}else {
						trangthai = "Không hoạt động";
					}
					
					hItem.put("_id", commons.getTextJsonNode(row.at("/_id")));
					hItem.put("UserName", commons.getTextJsonNode(row.at("/UserName")));
					hItem.put("FullName", commons.getTextJsonNode(row.at("/FullName")));
					hItem.put("Phone", commons.getTextJsonNode(row.at("/Phone")));
					hItem.put("IsActive",trangthai);
					hItem.put("roles", set_vtro);
					hItem.put("check", check);
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
