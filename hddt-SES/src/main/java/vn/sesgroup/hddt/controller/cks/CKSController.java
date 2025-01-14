package vn.sesgroup.hddt.controller.cks;

import java.security.Principal;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Locale;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bson.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.context.WebApplicationContext;

import com.api.message.JSONRoot;
import com.api.message.Msg;
import com.api.message.MsgRsp;
import com.api.message.MspResponseStatus;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;

import vn.sesgroup.hddt.controller.AbstractController;
import vn.sesgroup.hddt.dto.BaseDTO;
import vn.sesgroup.hddt.dto.CurrentUserProfile;
import vn.sesgroup.hddt.dto.JsonGridDTO;
import vn.sesgroup.hddt.resources.RestAPIUtility;
import vn.sesgroup.hddt.utils.Constants;
import vn.sesgroup.hddt.utils.Json;

@Controller
@RequestMapping("/cks")
@Scope(value = WebApplicationContext.SCOPE_REQUEST)
public class CKSController extends AbstractController{
	private static final Logger log = LogManager.getLogger(CKSController.class);
	@Autowired RestAPIUtility restAPI; 
	
	private String _id;
	
	@RequestMapping(value = "/init", method = {RequestMethod.POST, RequestMethod.GET})
	public String init(Locale locale, Principal principal, HttpServletRequest req) throws Exception{
		req.setAttribute("_TitleView_", Constants.PREFIX_TITLE + " - Danh sách chứng thư số");
		
		return "cks/cks";
	}
	
	private BaseDTO checkDataSearch(Locale locale, HttpServletRequest req, HttpSession session) {
		BaseDTO dto = new BaseDTO();
		dto.setErrorCode(0);
		
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
		
		msg.setObjData(hData);
		String checkserri  = "";
		String tungay  = "";
		String denngay  = "";
		JSONRoot root = new JSONRoot(msg);
		MsgRsp rsp = restAPI.callAPINormal("/cks/list", cup.getLoginRes().getToken(), HttpMethod.POST, root);
		MspResponseStatus rspStatus = rsp.getResponseStatus();
		if(rspStatus.getErrorCode() == 0) {
			JsonNode jsonData = Json.serializer().nodeFromObject(rsp.getObjData());
			HashMap<String, String> hItem = null;
			
			grid.setTotal(jsonData.size());
			for(JsonNode row: jsonData) {
				hItem = new HashMap<String, String>();
				
				ObjectMapper mapper = new ObjectMapper();
				ObjectReader reader = mapper.reader(JsonNode.class);
				JsonNode node = reader.readValue(row);
				
				if (!node.at("/DSCTSSDung").isMissingNode()) {
					for(JsonNode o: node.at("/DSCTSSDung")) {
						hItem = new LinkedHashMap<String, String>();
						 checkserri =  commons.getTextJsonNode(o.at("/Seri"));	
						 tungay =	commons.convertLocalDateTimeToString(commons.convertLongToLocalDate(o.at("/TNgay").asLong()), Constants.FORMAT_DATE.FORMAT_DATE_WEB);							
							 denngay =	commons.convertLocalDateTimeToString(commons.convertLongToLocalDate(o.at("/DNgay").asLong()), Constants.FORMAT_DATE.FORMAT_DATE_WEB);			
					}
				}
				
				
				
				hItem.put("Seri",checkserri);
				hItem.put("TDate",tungay);
				hItem.put("DDate", denngay);
				hItem.put("_id", commons.getTextJsonNode(row.at("/_id")));
				hItem.put("MST", commons.getTextJsonNode(row.at("/MST")));
				hItem.put("TenNnt", commons.getTextJsonNode(row.at("/TenNnt")));
				hItem.put("StatusDesc", Constants.MAP_TKHAI_STATUS.get(commons.getTextJsonNode(row.at("/Status"))));
				hItem.put("StatusCQTDesc", Constants.MAP_TKHAI_STATUS_CQT.get(commons.getTextJsonNode(row.at("/StatusCQT"))));
				hItem.put("CoQuanThue", commons.getTextJsonNode(row.at("/ChiCucThueInfo/name")));
				grid.getRows().add(hItem);
			}
			
		}else {
			grid = new JsonGridDTO();
			grid.setErrorCode(rspStatus.getErrorCode());
			grid.setResponseData(rspStatus.getErrorDesc());
		}
		return grid;
	}
	


	
}
