package vn.sesgroup.hddt.controller.einvoice_tbn;

import java.security.Principal;
import java.time.LocalDate;
import java.time.temporal.ChronoField;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Locale;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
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
@RequestMapping("/tbhdssot_tbn")
@Scope(value = WebApplicationContext.SCOPE_REQUEST)
public class TBHDSSotTBNController extends AbstractController{
	@Autowired RestAPIUtility restAPI; 
	
	private String _id;
	private String mauSoHdon;
	private String soHoaDon;
	private String fromDate;
	private String toDate;
	private String status;
	
	

	private void LoadParameter(CurrentUserProfile cup, Locale locale, HttpServletRequest req, String action) {
		try {
			BaseDTO baseDTO = new BaseDTO(req);
			Msg msg = baseDTO.createMsg(cup, Constants.MSG_ACTION_CODE.LOAD_PARAMS);
			
			/*DANH SACH THAM SO*/
			MsgParam msgParam = null;
			MsgParams msgParams = new MsgParams();
			
			msgParam = new MsgParam();
			msgParam.setId("param01");
			msgParam.setParam("DMMauSoKyHieu");
			msgParams.getParams().add(msgParam);
			
			/*END: DANH SACH THAM SO*/
			msg.setObjData(msgParams);
			
			JSONRoot root = new JSONRoot(msg);
			MsgRsp rsp = restAPI.callAPINormal("/commons/get-full-params", cup.getLoginRes().getToken(), HttpMethod.POST, root);
			MspResponseStatus rspStatus = rsp.getResponseStatus();
			String KHHDon = "";
			if(rspStatus.getErrorCode() == 0 && rsp.getObjData() != null) {
				LinkedHashMap<String, String> hItem = null;
				
				JsonNode jsonData = Json.serializer().nodeFromObject(rsp.getObjData());
			
				if(null != jsonData.at("/param01") && jsonData.at("/param01") instanceof ArrayNode) {
					hItem = new LinkedHashMap<String, String>();
					for(JsonNode o: jsonData.at("/param01")) {													
						KHHDon = commons.getTextJsonNode(o.get("KHHDon"));	
						char words=KHHDon.charAt(KHHDon.length() - 3);
						String s=String.valueOf(words);  
						if("M".equals(s)) {
							hItem.put(commons.getTextJsonNode(o.get("_id")), commons.getTextJsonNode(o.get("KHMSHDon")) + commons.getTextJsonNode(o.get("KHHDon")));	
						}						
				}	
					req.setAttribute("map_mausokyhieu", hItem);
				}
				
			}
			
		}catch(Exception e) {}
	}
	
	
	
	@RequestMapping(value = "/init", method = {RequestMethod.POST, RequestMethod.GET})
	public String init(Locale locale, Principal principal, HttpServletRequest req) throws Exception{
		req.setAttribute("_TitleView_", Constants.PREFIX_TITLE + " - Danh sách thông báo HĐ sai sót từ bên ngoài");
		CurrentUserProfile cup = getCurrentlyAuthenticatedPrincipal();
		LoadParameter(cup, locale, req, "DETAIL");
		LocalDate now = LocalDate.now();
		req.setAttribute("FromDate", commons.convertLocalDateTimeToString(now.minusMonths(1).with(ChronoField.DAY_OF_MONTH, 1), Constants.FORMAT_DATE.FORMAT_DATE_WEB));
		req.setAttribute("ToDate", commons.convertLocalDateTimeToString(now, Constants.FORMAT_DATE.FORMAT_DATE_WEB));
		req.setAttribute("map_status", Constants.MAP_HDSS_STATUS);
		return "einvoice_tbn/tbhdssot_tbn";
	}
	
	private BaseDTO checkDataSearch(Locale locale, HttpServletRequest req, HttpSession session) {
		BaseDTO dto = new BaseDTO();
		dto.setErrorCode(0);
		mauSoHdon = commons.getParameterFromRequest(req, "mau-so-hdon").replaceAll("\\s", "");
		soHoaDon = commons.getParameterFromRequest(req, "so-hoa-don").replaceAll("\\s", "");
		fromDate = commons.getParameterFromRequest(req, "from-date").replaceAll("\\s", "");
		toDate = commons.getParameterFromRequest(req, "to-date").replaceAll("\\s", "");
		status = commons.getParameterFromRequest(req, "status").replaceAll("\\s", "");
		if(!"".equals(fromDate) && !commons.checkLocalDate(fromDate, Constants.FORMAT_DATE.FORMAT_DATE_WEB)) {
			dto.setErrorCode(1);
			dto.getErrorMessages().add("Từ ngày không đúng định dạng.");
		}
		if(!"".equals(toDate) && !commons.checkLocalDate(toDate, Constants.FORMAT_DATE.FORMAT_DATE_WEB)) {
			dto.setErrorCode(1);
			dto.getErrorMessages().add("Từ ngày không đúng định dạng.");
		}
		
		return dto;
	}
	
	@PostMapping(value = "/search",  produces = MediaType.APPLICATION_JSON_VALUE)
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
		hData.put("MauSoHdon", mauSoHdon);
		hData.put("SoHoaDon", soHoaDon);
		hData.put("FromDate", fromDate);
		hData.put("ToDate", toDate);
		hData.put("Status", status);
		msg.setObjData(hData);
		
		JSONRoot root = new JSONRoot(msg);
		MsgRsp rsp = restAPI.callAPINormal("/tbhdssot_tbn/list", cup.getLoginRes().getToken(), HttpMethod.POST, root);
		MspResponseStatus rspStatus = rsp.getResponseStatus();
		if(rspStatus.getErrorCode() == 0) {
			MsgPage page = rsp.getMsgPage();
			grid.setTotal(page.getTotalRows());
			
			StringBuilder sb = new StringBuilder();
			JsonNode jsonData = Json.serializer().nodeFromObject(rsp.getObjData());
			JsonNode rows = null;
			String tctb = "";
			HashMap<String, String> hItem = null;
			if(!jsonData.at("/rows").isMissingNode()) {
				rows = jsonData.at("/rows");
				for(JsonNode row: rows) {
					hItem = new HashMap<String, String>();
					for(JsonNode o: row.at("/DSHDon")) {
						
						tctb = commons.getTextJsonNode(o.at("/TCTBao"));
						}
						
					hItem.put("TChat", Constants.MAP_HDSS_TCTBAO.get(tctb));
					hItem.put("_id", commons.getTextJsonNode(row.at("/_id")));
					hItem.put("Status", commons.getTextJsonNode(row.at("/Status")));
					hItem.put("StatusDesc", Constants.MAP_HDSS_STATUS.get(commons.getTextJsonNode(row.at("/Status"))));
					hItem.put("MSo", commons.getTextJsonNode(row.at("/MSo")));
					hItem.put("Ten", commons.getTextJsonNode(row.at("/Ten")));
					hItem.put("LoaiTB", Constants.MAP_LOAITB_HDSS.get(commons.getTextJsonNode(row.at("/Loai"))));
					hItem.put("NTBao", 
						commons.convertLocalDateTimeToString(commons.convertLongToLocalDate(row.at("/NTBaoDate").asLong()), Constants.FORMAT_DATE.FORMAT_DATE_WEB)
					);
					hItem.put("UserCreated", commons.getTextJsonNode(row.at("/InfoCreated/CreateUserFullName")));
					
					hItem.put("SignStatusCode", commons.getTextJsonNode(row.at("/SignStatusCode")));
					hItem.put("SignStatusDesc", Constants.MAP_EINVOICE_SIGN_STATUS.get(commons.getTextJsonNode(row.at("/SignStatusCode"))));
					hItem.put("MTDiep", commons.getTextJsonNode(row.at("/MTDiep")));
					hItem.put("MTDTChieu", commons.getTextJsonNode(row.at("/MTDTChieu")));
					
					if(!row.at("/DSLoi").isMissingNode()) {
						sb.setLength(0);
						for(JsonNode o: row.at("/DSLoi")) {
							sb.append("-").append(commons.getTextJsonNode(o.at("/MTLoi")));
								
						}
						hItem.put("DSLoi", sb.toString());			
					
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
	
	public BaseDTO checkDataToRefreshStatusCqt(HttpServletRequest req, HttpSession session
			, String transaction, CurrentUserProfile cup) throws Exception{
		BaseDTO dto = new BaseDTO();
		dto.setErrorCode(0);
	
		_id = commons.getParameterFromRequest(req, "_id").replaceAll("\\s", "");
		if("".equals(_id)) {
			dto.setErrorCode(1);
			dto.getErrorMessages().add("Không tìm thấy thông tin thông báo.");
		}
		
		return dto;
	}
	
	@PostMapping(value = "/refresh-status-cqt",  produces = MediaType.APPLICATION_JSON_VALUE)
	@ResponseBody
	public BaseDTO execRefreshStatusCQT(Locale locale, HttpServletRequest req, HttpSession session
			, @RequestAttribute(name = "transaction", value = "", required = false) String transaction) throws Exception {
		BaseDTO dto = new BaseDTO();		
		
		CurrentUserProfile cup = getCurrentlyAuthenticatedPrincipal();
		dto = checkDataToRefreshStatusCqt(req, session, transaction, cup);
		if(0 != dto.getErrorCode()) {
			dto.setErrorCode(999);
			dto.setResponseData(Constants.MAP_ERROR.get(999));
			return dto;
		}

		String actionCode = Constants.MSG_ACTION_CODE.CREATED;
		dto = new BaseDTO(req);
		Msg msg = dto.createMsg(cup, actionCode);
		HashMap<String, Object> hData = new HashMap<>();
		hData.put("_id", _id);
		
		msg.setObjData(hData);
		
		JSONRoot root = new JSONRoot(msg);
		MsgRsp rsp = restAPI.callAPINormal("/tbhdssot_tbn/refresh-status-cqt", cup.getLoginRes().getToken(), HttpMethod.POST, root);
		MspResponseStatus rspStatus = rsp.getResponseStatus();
		if(rspStatus.getErrorCode() == 0) {
			dto.setErrorCode(0);
			dto.setResponseData("Lấy trạng thái thành công.");
		}else {
			dto.setErrorCode(rspStatus.getErrorCode());
			dto.setResponseData(rspStatus.getErrorDesc());
		}
		return dto;
	}
	
}
