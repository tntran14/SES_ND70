package vn.sesgroup.hddt.controller.user;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.URLEncoder;
import java.security.Principal;
import java.time.LocalDate;
import java.time.temporal.ChronoField;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
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
import vn.sesgroup.hddt.dto.FileInfo;
import vn.sesgroup.hddt.dto.JsonGridDTO;
import vn.sesgroup.hddt.resources.RestAPIUtility;
import vn.sesgroup.hddt.utils.Constants;
import vn.sesgroup.hddt.utils.Json;

@Controller
@RequestMapping({
	"/tthdon"
})
@Scope(value = WebApplicationContext.SCOPE_REQUEST)
public class TTHDonController extends AbstractController{
	private static final Logger log = LogManager.getLogger(TTHDonController.class);
	@Autowired RestAPIUtility restAPI; 
		

	private String mtdiep;
	private String tthdon;
	private String lhd;
	
	private void LoadParameter(CurrentUserProfile cup, Locale locale, HttpServletRequest req, String action) {
		try {
			BaseDTO baseDTO = new BaseDTO(req);
			Msg msg = baseDTO.createMsg(cup, Constants.MSG_ACTION_CODE.LOAD_PARAMS);

			
			
			String[] status = { "Mới tạo", "Chưa được ký", "Đang xử lý từ CQT", "Đã phát hành" , "Lỗi từ CQT", "Đã hủy","Đã thay thế", "Đã điều chỉnh"};
			
			
				LinkedHashMap<String, String> hItem = null;

					hItem = new LinkedHashMap<String, String>();
					int dem = 0;
					for(int i=0; i<status.length;i++) {
						dem += 1; 
						String d = String.valueOf(dem);
						hItem.put(d, status[i]);
						
					}
					req.setAttribute("map_tthdon", hItem);
				
			
			
		}catch(Exception e) {}
	}
	
	@RequestMapping(value = "/init", method = {RequestMethod.POST, RequestMethod.GET})
	public String init(Locale locale, Principal principal, HttpServletRequest req) throws Exception{
		req.setAttribute("_TitleView_", Constants.PREFIX_TITLE + " - Trạng thái hóa đơn");
	
		CurrentUserProfile cup = getCurrentlyAuthenticatedPrincipal();
		LoadParameter(cup, locale, req, "");
		
		//req.setAttribute("USERS-CHECK", "USER-CHECK");
		return "user/tthdon";
	}

	
//	private BaseDTO checkDataSearch(Locale locale, HttpServletRequest req, HttpSession session) {
//		BaseDTO dto = new BaseDTO();
//		dto.setErrorCode(0);
//		
//		mtdiep = commons.getParameterFromRequest(req, "mtdiep").replaceAll("\\s", "");
//		if(!"".equals(mtdiep)) {
//			dto.setErrorCode(1);
//			dto.getErrorMessages().add("Vui lòng nhập mã thông điệp.");
//		}
//		
//		
//		return dto;
//	}
	
	@RequestMapping(value = "/search",  produces = MediaType.APPLICATION_JSON_VALUE, method = RequestMethod.POST)
	@ResponseBody
	public BaseDTO execSearch(Locale locale, HttpServletRequest req, HttpSession session) throws Exception{
		JsonGridDTO grid = new JsonGridDTO();
		
//		BaseDTO baseDTO = checkDataSearch(locale, req, session);
//		if(0 != baseDTO.getErrorCode()) {
//			grid.setErrorCode(baseDTO.getErrorCode());
//			grid.setErrorMessages(baseDTO.getErrorMessages());
//			grid.setResponseData(Constants.MAP_ERROR.get(999));
//			return grid;
//		}
		BaseDTO baseDTO = new BaseDTO(req);
		mtdiep = commons.getParameterFromRequest(req, "mtdiep").replaceAll("\\s", "");
		
		if(!"".equals(mtdiep)) {
			baseDTO.setErrorCode(1);
			baseDTO.getErrorMessages().add("Vui lòng nhập mã thông điệp.");
		}
		CurrentUserProfile cup = getCurrentlyAuthenticatedPrincipal();		
		
		Msg msg = baseDTO.createMsg(cup, Constants.MSG_ACTION_CODE.SEARCH);
		
		HashMap<String, Object> hData = new HashMap<>();
		hData.put("MTDiep", mtdiep);
		msg.setObjData(hData);
		
		JSONRoot root = new JSONRoot(msg);
		MsgRsp rsp = restAPI.callAPINormal("/tthdon/check", cup.getLoginRes().getToken(), HttpMethod.POST, root);
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
					
					hItem.put("SignStatusCode", commons.getTextJsonNode(row.at("/SignStatusCode")));
					hItem.put("SignStatusDesc", Constants.MAP_EINVOICE_SIGN_STATUS.get(commons.getTextJsonNode(row.at("/SignStatusCode"))));
					
					hItem.put("EInvoiceStatus", commons.getTextJsonNode(row.at("/EInvoiceStatus")));
					hItem.put("MCCQT", commons.getTextJsonNode(row.at("/MCCQT")));
					
					String status = commons.getTextJsonNode(row.at("/EInvoiceStatus"));
					String sign = commons.getTextJsonNode(row.at("/SignStatusCode"));
					if(status.equals("PENDING") && sign.equals("SIGNED")) {
						hItem.put("StatusDesc", "Chờ gửi CQT");
						
					}
					else {
						hItem.put("StatusDesc", Constants.MAP_EINVOICE_STATUS.get(commons.getTextJsonNode(row.at("/EInvoiceStatus"))));
						
					}
				
					
					hItem.put("CQTMTLoi", commons.getTextJsonNode(row.at("/LDo/MTLoi")));
					hItem.put("MaHD", commons.getTextJsonNode(row.at("/EInvoiceDetail/TTChung/MaHD")));
					hItem.put("MauSoHD", 
						commons.getTextJsonNode(row.at("/EInvoiceDetail/TTChung/KHMSHDon")) + commons.getTextJsonNode(row.at("/EInvoiceDetail/TTChung/KHHDon"))
					);
					hItem.put("EInvoiceNumber", 
						"".equals(commons.getTextJsonNode(row.at("/EInvoiceDetail/TTChung/SHDon")))? "":
						commons.formatNumberBillInvoice(commons.getTextJsonNode(row.at("/EInvoiceDetail/TTChung/SHDon")))
					);
					
					
					
					hItem.put("NLap", 
						commons.convertLocalDateTimeToString(commons.convertLongToLocalDate(row.at("/EInvoiceDetail/TTChung/NLap").asLong()), Constants.FORMAT_DATE.FORMAT_DATE_WEB)
					);
					
//					hItem.put("NLap", 
//							commons.convertLongToLocalDateTime(commons.convertLongToLocalDate(row.at("/EInvoiceDetail/TTChung/NLap").asLong()), Constants.FORMAT_DATE.FORMAT_DATE_WEB)
//						);
					
					
					hItem.put("TaxCode", commons.getTextJsonNode(row.at("/EInvoiceDetail/NDHDon/NMua/MST")));
					hItem.put("CompanyName", commons.getTextJsonNode(row.at("/EInvoiceDetail/NDHDon/NMua/Ten")));
					hItem.put("TgTTTBSo", 
						row.at("/EInvoiceDetail/TToan/TgTTTBSo").isMissingNode()? "":
						commons.formatNumberReal(row.at("/EInvoiceDetail/TToan/TgTTTBSo").doubleValue())
					);
					hItem.put("TgTCThue", 
						row.at("/EInvoiceDetail/TToan/TgTCThue").isMissingNode()? "":
						commons.formatNumberReal(row.at("/EInvoiceDetail/TToan/TgTCThue").doubleValue())
					);
					hItem.put("TgTThue", 
						row.at("/EInvoiceDetail/TToan/TgTThue").isMissingNode()? "":
						commons.formatNumberReal(row.at("/EInvoiceDetail/TToan/TgTThue").doubleValue())
					);
					hItem.put("HVTNMHang", commons.getTextJsonNode(row.at("/EInvoiceDetail/NDHDon/NMua/HVTNMHang")));
					hItem.put("UserCreated", commons.getTextJsonNode(row.at("/InfoCreated/CreateUserFullName")));
					hItem.put("HDSS_TCTBao", commons.getTextJsonNode(row.at("/HDSS/TCTBao")));
					hItem.put("MTDiep", commons.getTextJsonNode(row.at("/MTDiep")));
					hItem.put("MTDTChieu", commons.getTextJsonNode(row.at("/MTDTChieu")));
					String SendCQT_Date = row.at("/SendCQT_Date").toString();
					String CQT_Date = row.at("/CQT_Date").toString();
					if(!SendCQT_Date.equals("")) {
						hItem.put("SendCQT_Date", commons.convertLocalDateTimeToString(commons.convertLongToLocalDate(row.at("/SendCQT_Date").asLong()), Constants.FORMAT_DATE.FORMAT_DATE_WEB)
								);
					}
					if(!CQT_Date.equals("")) {
						hItem.put("CQT_Date", commons.convertLocalDateTimeToString(commons.convertLongToLocalDate(row.at("/CQT_Date").asLong()), Constants.FORMAT_DATE.FORMAT_DATE_WEB)
								);
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
	
	
//	@RequestMapping(value = "/check_search", method = {RequestMethod.POST, RequestMethod.GET})
//	public String check_search(Locale locale, Principal principal, HttpServletRequest req) throws Exception{	
//		CurrentUserProfile cup = getCurrentlyAuthenticatedPrincipal();
//		BaseDTO baseDTO = new BaseDTO(req);
//		LoadParameter(cup, locale, req, "");
//		Msg msg = baseDTO.createMsg(cup, Constants.MSG_ACTION_CODE.SEARCH);
//		HashMap<String, Object> hData = new HashMap<>();
//		mtdiep = commons.getParameterFromRequest(req, "mtdiep").replaceAll("\\s", "");
//		hData.put("MTDiep", mtdiep);
//		msg.setObjData(hData);
//		
//		JSONRoot root = new JSONRoot(msg);
//		MsgRsp rsp = restAPI.callAPINormal("/tthdon/check", cup.getLoginRes().getToken(), HttpMethod.POST, root);
//		MspResponseStatus rspStatus = rsp.getResponseStatus();
//		if(rspStatus.getErrorCode() == 0) {
//		req.setAttribute("LHDon", "123");
//		}
//		dto.setErrorCode(0);
//		return "user/tthdon";
//	}
	
	public BaseDTO checkDataToSave(HttpServletRequest req, HttpSession session
			, String transaction, CurrentUserProfile cup) throws Exception{
		BaseDTO dto = new BaseDTO();
		dto.setErrorCode(0);
	
	
		mtdiep = commons.getParameterFromRequest(req, "mtdiep").trim().replaceAll("\\s+", "");
		tthdon = commons.getParameterFromRequest(req, "tthdon").trim().replaceAll("\\s+", "");
	//	lhd = commons.getParameterFromRequest(req, "lhd").trim().replaceAll("\\s+", "");
		
		if("".equals(mtdiep)) {
			dto.setErrorCode(1);
			dto.getErrorMessages().add("Vui lòng nhập mã thông điệp.");
		}
		if("".equals(tthdon)) {
			dto.setErrorCode(1);
			dto.getErrorMessages().add("Vui lòng chọn trạng thái hóa đơn.");
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
		String messageConfirm =  "Bạn có muốn thay đổi trạng thái hóa đơn này không";
		switch (transaction) {
		case "tthdon":
			messageConfirm =  "Bạn có muốn thay đổi trạng thái hóa đơn này không";
			break;
			
		default:
			dto = new BaseDTO();
			dto.setErrorCode(998);
			dto.setResponseData("Không tìm thấy chức năng giao dịch.");
			return dto;
		}
		
		CurrentUserProfile cup = getCurrentlyAuthenticatedPrincipal();
		dto = checkDataToSave(req, session, transaction, cup);
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
		hInfo.put("MTDiep", mtdiep);
		hInfo.put("TTHDon", tthdon);
		//hInfo.put("LoaiHD", lhd);
		dto.setResponseData(hInfo);
		dto.setErrorCode(0);
		return dto;
	
	}
	
	@RequestMapping(value = "/save-data",  produces = MediaType.APPLICATION_JSON_VALUE, method = RequestMethod.POST)
	@ResponseBody
	public BaseDTO execSaveData(Locale locale,HttpServletRequest req, HttpSession session
			, @RequestAttribute(name = "transaction", value = "", required = false) String transaction
			, @RequestParam(value = "tokenTransaction", required = false, defaultValue = "") String tokenTransaction) throws Exception{
		BaseDTO dtoRes = new BaseDTO();
		
		CurrentUserProfile cup = getCurrentlyAuthenticatedPrincipal();
		String []split_ = tokenTransaction.split(",");
		String token_ = split_[0];
//		String mtdiep_ = split_[1];
		/*CHECK TOKEN*/
		String token = session.getAttribute(Constants.SESSION_TYPE.SESSION_TOKEN_EXECUTE) == null ? ""
				: session.getAttribute(Constants.SESSION_TYPE.SESSION_TOKEN_EXECUTE).toString();
		session.removeAttribute(Constants.SESSION_TYPE.SESSION_TOKEN_EXECUTE);
		if ("".equals(token) || !token_.equals(token)) {
			dtoRes.setErrorCode(1);
			dtoRes.setResponseData("Token giao dịch không hợp lệ.");
			return dtoRes;
		}
		/*END: CHECK TOKEN*/
		
		String actionCode = Constants.MSG_ACTION_CODE.CREATED;
		switch (transaction) {
		case "tthdon": actionCode = Constants.MSG_ACTION_CODE.CREATED; break;		
		default:
			dtoRes = new BaseDTO();
			dtoRes.setErrorCode(998);
			dtoRes.setResponseData(Constants.MAP_ERROR.get(998));
			return dtoRes;
		}
		
		dtoRes = new BaseDTO(req);
		Msg msg = dtoRes.createMsg(cup, actionCode);
		HashMap<String, Object> hData = new HashMap<>();
		dtoRes = checkDataToSave(req, session, transaction, cup);		
		hData.put("MTDiep", mtdiep);
		hData.put("TTHDon", tthdon);
		msg.setObjData(hData);
		JSONRoot root = new JSONRoot(msg);
		MsgRsp rsp = restAPI.callAPINormal("/tthdon/crud", cup.getLoginRes().getToken(), HttpMethod.POST, root);
		MspResponseStatus rspStatus = rsp.getResponseStatus();
		if(rspStatus.getErrorCode() == 0) {
			dtoRes.setErrorCode(0);
			dtoRes.setResponseData("Thay đổi trạng thái thành công");					
				
		}else {
			dtoRes.setErrorCode(rspStatus.getErrorCode());
			dtoRes.setResponseData(rspStatus.getErrorDesc());
		}
		return dtoRes;
	}
}
