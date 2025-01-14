package vn.sesgroup.hddt.controller.dm;

import java.security.Principal;
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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.context.WebApplicationContext;

import com.api.message.JSONRoot;
import com.api.message.Msg;
import com.api.message.MsgPage;
import com.api.message.MsgRsp;
import com.api.message.MspResponseStatus;
import com.fasterxml.jackson.databind.JsonNode;

import vn.sesgroup.hddt.controller.AbstractController;
import vn.sesgroup.hddt.dto.BaseDTO;
import vn.sesgroup.hddt.dto.CurrentUserProfile;
import vn.sesgroup.hddt.dto.JsonGridDTO;
import vn.sesgroup.hddt.resources.RestAPIUtility;
import vn.sesgroup.hddt.utils.Constants;
import vn.sesgroup.hddt.utils.Json;

@Controller
@RequestMapping("/dmcustomer")
@Scope(value = WebApplicationContext.SCOPE_REQUEST)
public class DMCustomerController extends AbstractController{
	private static final Logger log = LogManager.getLogger(DMCustomerController.class);
	@Autowired RestAPIUtility restAPI;
	
	private String taxCode;
	private String companyName;
	private String customerName;
	
	@RequestMapping(value = "/init", method = {RequestMethod.POST, RequestMethod.GET})
	public String init(Locale locale, Principal principal, HttpServletRequest req) throws Exception{
		req.setAttribute("_TitleView_", Constants.PREFIX_TITLE + " - Danh sách khách hàng");
		
//		CurrentUserProfile cup = getCurrentlyAuthenticatedPrincipal();
//		LoadParameter(cup, locale, req, "DETAIL");
		
		return "dm/customer";
	}
	
	private BaseDTO checkDataSearch(Locale locale, HttpServletRequest req, HttpSession session) {
		BaseDTO dto = new BaseDTO();
		dto.setErrorCode(0);
		
		taxCode = commons.getParameterFromRequest(req, "tax-code").trim().replaceAll("\\s+", " ");
		companyName = commons.getParameterFromRequest(req, "company-name").trim().replaceAll("\\s+", " ");
		customerName = commons.getParameterFromRequest(req, "customer-name").trim().replaceAll("\\s+", " ");
		
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
		hData.put("TaxCode", taxCode);
		hData.put("CompanyName", companyName);
		hData.put("CustomerName", customerName);
				
		msg.setObjData(hData);
		
		JSONRoot root = new JSONRoot(msg);
		MsgRsp rsp = restAPI.callAPINormal("/dmcustomer/list", cup.getLoginRes().getToken(), HttpMethod.POST, root);
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
					
					hItem.put("_id", commons.getTextJsonNode(row.at("/_id")));
					hItem.put("TaxCode", commons.getTextJsonNode(row.at("/TaxCode")));
					hItem.put("CustomerCode", commons.getTextJsonNode(row.at("/CustomerCode")));
					hItem.put("CompanyName", commons.getTextJsonNode(row.at("/CompanyName")));
					hItem.put("CustomerName", commons.getTextJsonNode(row.at("/CustomerName")));
					hItem.put("Address", commons.getTextJsonNode(row.at("/Address")));
					hItem.put("Email", commons.getTextJsonNode(row.at("/Email")));
					hItem.put("EmailCC", commons.getTextJsonNode(row.at("/EmailCC")));
					
					hItem.put("ProvinceName", commons.getTextJsonNode(row.at("/Province/Name")));
					hItem.put("CustomerGroup1Name", commons.getTextJsonNode(row.at("/CustomerGroup1/Name")));
					hItem.put("CustomerGroup2Name", commons.getTextJsonNode(row.at("/CustomerGroup2/Name")));
					hItem.put("CustomerGroup3Name", commons.getTextJsonNode(row.at("/CustomerGroup3/Name")));
					
					hItem.put("UserCreated", commons.getTextJsonNode(row.at("/InfoCreated/CreateUserName")));
					if(!row.at("/InfoCreated/CreateDate").isMissingNode() && null != row.at("/InfoCreated/CreateDate")) {
						try {
							hItem.put("DateCreated", commons.convertLocalDateTimeToString(commons.convertLongToLocalDateTime(row.at("/InfoCreated/CreateDate").asLong(0L)), Constants.FORMAT_DATE.FORMAT_DATE_TIME_WEB));
						}catch(Exception e) {}
					}
					
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
