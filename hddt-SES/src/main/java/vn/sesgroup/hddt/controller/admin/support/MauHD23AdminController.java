package vn.sesgroup.hddt.controller.admin.support;

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
@RequestMapping("/mauhd_admin")
@Scope(value = WebApplicationContext.SCOPE_REQUEST)
public class MauHD23AdminController extends AbstractController{
	private static final Logger log = LogManager.getLogger(MauHD23AdminController.class);
	@Autowired RestAPIUtility restAPI; 
	
	private String _id;
	private String mst;
	private String name;
	private String mskh;
	
	@RequestMapping(value = "/init", method = {RequestMethod.POST, RequestMethod.GET})
	public String init(Locale locale, Principal principal, HttpServletRequest req) throws Exception{
		req.setAttribute("_TitleView_", Constants.PREFIX_TITLE + " - Cập nhật mẫu số theo năm");
		req.setAttribute("map_status", Constants.MAP_STATUS);
		return "support-admin/mauhd_admin";
	}
	
	private BaseDTO checkDataSearch(Locale locale, HttpServletRequest req, HttpSession session) {
		BaseDTO dto = new BaseDTO();
		dto.setErrorCode(0);
		
		mst = commons.getParameterFromRequest(req, "mst").trim().replaceAll("\\s+", "");
		mskh = commons.getParameterFromRequest(req, "mskh").trim().replaceAll("\\s+", "");
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
		hData.put("MST", mst);
		hData.put("MSKH", mskh);
		msg.setObjData(hData);
		
		JSONRoot root = new JSONRoot(msg);
		MsgRsp rsp = restAPI.callAPINormal("/mauhd_admin/list", cup.getLoginRes().getToken(), HttpMethod.POST, root);
		MspResponseStatus rspStatus = rsp.getResponseStatus();
		if(rspStatus.getErrorCode() == 0) {
			MsgPage page = rsp.getMsgPage();
			grid.setTotal(page.getTotalRows());
			
			
			req.setAttribute("total_", rspStatus.getErrorDesc());
			
			JsonNode jsonData = Json.serializer().nodeFromObject(rsp.getObjData());
			JsonNode rows = null;
			HashMap<String, String> hItem = null;
			if(!jsonData.at("/rows").isMissingNode()) {
				rows = jsonData.at("/rows");
				for(JsonNode row: rows) {
					hItem = new HashMap<String, String>();		
					
					//String check = commons.getTextJsonNode(row.at("/ActiveFlag"));
					String trangthai = "";
					String active = "";
//					if(check.equals("false")) {
//						active = "N";
//						trangthai = "Chưa kích hoạt";
//					}else {
//						active = "Y";
//						trangthai = "Đã kích hoạt";
//					}
					hItem.put("_id", commons.getTextJsonNode(row.at("/_id")));					
					hItem.put("MSHDon", commons.getTextJsonNode(row.at("/KHMSHDon"))+ commons.getTextJsonNode(row.at("/KHHDon")));
					hItem.put("SHDHT", commons.getTextJsonNode(row.at("/SHDHT")));		
					hItem.put("SoLuong",  commons.getTextJsonNode(row.at("/SoLuong")));	
					hItem.put("ConLai",  commons.getTextJsonNode(row.at("/ConLai")));
					hItem.put("DenSo",  commons.getTextJsonNode(row.at("/DenSo")));	
					hItem.put("TaxCode",  commons.getTextJsonNode(row.at("/TaxCode")));	
					hItem.put("Name",  commons.getTextJsonNode(row.at("/Name")));	
					
					hItem.put("NamPH",  commons.getTextJsonNode(row.at("/InfoPhatHanhNam23/SoLuong")));	
//					hItem.put("NamPH", 
//							"".equals(commons.getTextJsonNode(row.at("/InfoPhatHanhNam23/SoLuong")))? "":
//							commons.formatNumberBillInvoice(commons.getTextJsonNode(row.at("/InfoPhatHanhNam23/SoLuong")))
//						);
						
//					hItem.put("CreateDate", 
//							commons.convertLocalDateTimeToString(commons.convertLongToLocalDate(row.at("/InfoCreated/CreateDate").asLong()), Constants.FORMAT_DATE.FORMAT_DATE_WEB)
//						);
//					hItem.put("CreateUserFullName",commons.getTextJsonNode(row.at("/InfoCreated/CreateUserFullName")));
				
				
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
