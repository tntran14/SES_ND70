package vn.sesgroup.hddt.controller.einvoice_mtt;

import java.security.Principal;
import java.time.LocalDate;
import java.time.temporal.ChronoField;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

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
import com.api.message.MsgPage;
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
import vn.sesgroup.hddt.dto.FileInfo;
import vn.sesgroup.hddt.dto.JsonGridDTO;
import vn.sesgroup.hddt.resources.RestAPIUtility;
import vn.sesgroup.hddt.utils.Constants;
import vn.sesgroup.hddt.utils.Json;

@Controller
@RequestMapping("/einvoice_mtt")
@Scope(value = WebApplicationContext.SCOPE_REQUEST)
public class EInvoiceMTTController extends AbstractController{
	@Autowired RestAPIUtility restAPI; 
	
	private String _id;
	private String mauSoHdon;
	private String soHoaDon;
	private String mtdiep;
	private String fromDate;
	private String toDate;
	private String status;
	private String signStatus;
	private String emailStatus;
	private String SendCQTStatus;
	private String nbanMst;
	private String nbanTen;
	private String maHDon;
	private String maCQT;
	private String khName;
	private String MST;
	private String nmuaTen;
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
			
			if(rspStatus.getErrorCode() == 0 && rsp.getObjData() != null) {
				LinkedHashMap<String, String> hItem = null;
				
				JsonNode jsonData = Json.serializer().nodeFromObject(rsp.getObjData());
				String KHHDon = "";
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
		req.setAttribute("_TitleView_", Constants.PREFIX_TITLE + " - Danh sách hóa đơn điện tử từ máy tính tiền");
		
		CurrentUserProfile cup = getCurrentlyAuthenticatedPrincipal();

		LoadParameter(cup, locale, req, "DETAIL");
		
		LocalDate now = LocalDate.now();

		req.setAttribute("FromDate", commons.convertLocalDateTimeToString(now.minusMonths(1).with(ChronoField.DAY_OF_MONTH, 1), Constants.FORMAT_DATE.FORMAT_DATE_WEB));
		req.setAttribute("ToDate", commons.convertLocalDateTimeToString(now, Constants.FORMAT_DATE.FORMAT_DATE_WEB));	
		req.setAttribute("map_status", Constants.MAP_EINVOICE_STATUS);
		req.setAttribute("map_sign_status", Constants.MAP_EINVOICE_SIGN_STATUS);
		req.setAttribute("map_email_status", Constants.MAP_EINVOICE_EMAIL_STATUS);
		req.setAttribute("map_sendCQT_status", Constants.MAP_EINVOICE_SENDCQT_STATUS);
		req.setAttribute("map_status_sendcqt", Constants.MAP_STATUS_SENDCQT);
		return "einvoice_mtt/einvoice_mtt";
	}
	
	private BaseDTO checkDataSearch(Locale locale, HttpServletRequest req, HttpSession session) {
		BaseDTO dto = new BaseDTO();
		dto.setErrorCode(0);
		
		mauSoHdon = commons.getParameterFromRequest(req, "mau-so-hdon").replaceAll("\\s", "");
		soHoaDon = commons.getParameterFromRequest(req, "so-hoa-don").replaceAll("\\s", "");
		mtdiep = commons.getParameterFromRequest(req, "mtdiep").replaceAll("\\s", "");
		fromDate = commons.getParameterFromRequest(req, "from-date").replaceAll("\\s", "");
		toDate = commons.getParameterFromRequest(req, "to-date").replaceAll("\\s", "");
		status = commons.getParameterFromRequest(req, "status").replaceAll("\\s", "");
		signStatus = commons.getParameterFromRequest(req, "sign-status").replaceAll("\\s", "");
		emailStatus = commons.getParameterFromRequest(req, "email-status").replaceAll("\\s", "");
		SendCQTStatus = commons.getParameterFromRequest(req, "send-cqt-status").replaceAll("\\s", "");
		nbanMst = commons.getParameterFromRequest(req, "nban-mst").replaceAll("\\s+", "");
		nbanTen = commons.getParameterFromRequest(req, "nban-ten").replaceAll("\\s+", " ");
		maHDon = commons.getParameterFromRequest(req, "ma-hoa-don").replaceAll("\\s", "");
		maCQT = commons.getParameterFromRequest(req, "mcqt").replaceAll("\\s", "");
//		khName = commons.getParameterFromRequest(req, "kh_name").replaceAll("\\s+", " ");
//		MST = commons.getParameterFromRequest(req, "mst").replaceAll("\\s+", "");
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
		hData.put("MauSoHdon", mauSoHdon);
		hData.put("SoHoaDon", soHoaDon);
		hData.put("MTDiep", mtdiep);
		hData.put("FromDate", fromDate);
		hData.put("ToDate", toDate);
		hData.put("Status", status);
		hData.put("SignStatus", signStatus);
		hData.put("EmailStatus", emailStatus);
		hData.put("SendCQTStatus", SendCQTStatus);
		hData.put("NbanMST", nbanMst);
		hData.put("NbanTen", nbanTen);
		hData.put("MaHDon", maHDon);	
		hData.put("MaCQT", maCQT);	
		hData.put("NameKH", khName);
		hData.put("MST", MST);
		hData.put("NmuaTen", nmuaTen);
		msg.setObjData(hData);
		
		JSONRoot root = new JSONRoot(msg);
		MsgRsp rsp = restAPI.callAPINormal("/einvoice_mtt/list", cup.getLoginRes().getToken(), HttpMethod.POST, root);
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
					if(status.equals("PENDING") && sign.equals("SIGNED") ||status.equals("PENDING") && sign.equals("NOSIGN")) {
						hItem.put("StatusDesc", "Chưa gửi CQT");
					}
						else if(status.equals("PROCESSING")) {
							hItem.put("StatusDesc", "Gửi CQT thành công");
						
					}else if(status.equals("COMPLETE")) {
						hItem.put("StatusDesc", "CQT chấp nhận");
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
//							commons.convertLongToLocalDateTime(commons.convertLongToLocalDate(row.at("/einvoice_mttDetail/TTChung/NLap").asLong()), Constants.FORMAT_DATE.FORMAT_DATE_WEB)
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
					
					String publish = commons.getTextJsonNode(row.at("/PublishStatus"));
					if(publish.equals("true")) {
						hItem.put("PublishStatus", "Đã phát hành");
						
					}
					else {
						hItem.put("PublishStatus", "Chưa phát hành");
						
					}
					String sendCQT = commons.getTextJsonNode(row.at("/SendCQTStatus"));
					if(sendCQT.equals("true")) {
						hItem.put("SendCQTStatus", "Đã gửi thuế");
						
					}
					else {
						hItem.put("SendCQTStatus", "Chưa gửi thuế");
						
					}
					
					String emailStatus = commons.getTextJsonNode(row.at("/EmailStatus"));
					if(emailStatus.equals("true")) {
						hItem.put("EmailStatus", "Đã gửi");
						
					}
					else {
						hItem.put("EmailStatus", "Chưa gửi");
					}

					hItem.put("MST", commons.getTextJsonNode(row.at("/EInvoiceDetail/NDHDon/NMua/MST")));
					hItem.put("NameKH", commons.getTextJsonNode(row.at("/EInvoiceDetail/NDHDon/NMua/Ten")));
					hItem.put("NameNM", commons.getTextJsonNode(row.at("/EInvoiceDetail/NDHDon/NMua/HVTNMHang")));
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
			dto.getErrorMessages().add("Không tìm thấy thông tin hóa đơn.");
		}
		
		return dto;
	}
	
	@RequestMapping(value = "/refresh-status-cqt",  produces = MediaType.APPLICATION_JSON_VALUE, method = RequestMethod.POST)
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
		MsgRsp rsp = restAPI.callAPINormal("/einvoice_mtt/refresh-status-cqt", cup.getLoginRes().getToken(), HttpMethod.POST, root);
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
	@RequestMapping(value = "/delete_HD",  produces = MediaType.APPLICATION_JSON_VALUE, method = RequestMethod.POST)
	  @ResponseBody
	  public BaseDTO delete_HD(Locale locale, HttpServletRequest req, HttpSession session
	      , @RequestAttribute(name = "transaction", value = "", required = false) String transaction) throws Exception {
	    BaseDTO dto = new BaseDTO();    
	    
	    CurrentUserProfile cup = getCurrentlyAuthenticatedPrincipal();
	    dto = checkDataToRefreshStatusCqt(req, session, transaction, cup);
	    if(0 != dto.getErrorCode()) {
	      dto.setErrorCode(999);
	      dto.setResponseData(Constants.MAP_ERROR.get(999));
	      return dto;
	    }

	    String actionCode = Constants.MSG_ACTION_CODE.XoaBo;
	    dto = new BaseDTO(req);
	    Msg msg = dto.createMsg(cup, actionCode);
	    HashMap<String, Object> hData = new HashMap<>();
	    hData.put("_id", _id);
	    
	    msg.setObjData(hData);
	    
	    JSONRoot root = new JSONRoot(msg);
	    MsgRsp rsp = restAPI.callAPINormal("/einvoice_mtt/delete_HD", cup.getLoginRes().getToken(), HttpMethod.POST, root);
	    MspResponseStatus rspStatus = rsp.getResponseStatus();
	    if(rspStatus.getErrorCode() == 0) {
	      dto.setErrorCode(0);
	      dto.setResponseData("Xóa bỏ thành công.");
	    }else {
	      dto.setErrorCode(rspStatus.getErrorCode());
	      dto.setResponseData(rspStatus.getErrorDesc());
	    }
	    return dto;
	  }
	
	
	
	
	//
	
	public BaseDTO checkDataToPublish(HttpServletRequest req, HttpSession session
			, String transaction, CurrentUserProfile cup) throws Exception{
		BaseDTO dto = new BaseDTO();
		dto.setErrorCode(0);
	
		_id = commons.getParameterFromRequest(req, "_id").replaceAll("\\s", "");
		if("".equals(_id)) {
			dto.setErrorCode(1);
			dto.getErrorMessages().add("Không tìm thấy thông tin hóa đơn.");
		}
		
		return dto;
	}
	@RequestMapping(value = "/publish_hd",  produces = MediaType.APPLICATION_JSON_VALUE, method = RequestMethod.POST)
	@ResponseBody
	public BaseDTO execPublishHD(Locale locale, HttpServletRequest req, HttpSession session
			, @RequestAttribute(name = "transaction", value = "", required = false) String transaction) throws Exception {
		BaseDTO dto = new BaseDTO();		
		
		CurrentUserProfile cup = getCurrentlyAuthenticatedPrincipal();
		dto = checkDataToPublish(req, session, transaction, cup);
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
		MsgRsp rsp = restAPI.callAPINormal("/einvoice_mtt/publish_hd", cup.getLoginRes().getToken(), HttpMethod.POST, root);
		MspResponseStatus rspStatus = rsp.getResponseStatus();
		if(rspStatus.getErrorCode() == 0) {
//			String sendMail = rsp.getResponseStatus().getErrorDesc();			
			
//			String encodedString = Base64.getEncoder().encodeToString(sendMail.getBytes());
			
//			HashMap<String, String> hInfo = new HashMap<String, String>();
//			hInfo.put("IDSendMail", encodedString);
//			
//			dto.setResponseData(hInfo);
			dto.setErrorCode(0);
		}else {
			dto.setErrorCode(rspStatus.getErrorCode());
			dto.setResponseData(rspStatus.getErrorDesc());
		}
		return dto;
	}
	
	@RequestMapping(value = "/publish-hd-list",  produces = MediaType.APPLICATION_JSON_VALUE, method = RequestMethod.POST)
	@ResponseBody
	public BaseDTO publishHDList(Locale locale, HttpServletRequest req, HttpSession session
			, @RequestAttribute(name = "transaction", value = "", required = false) String transaction) throws Exception {
		
		String token = "";
		if (null != session.getAttribute(Constants.SESSION_TYPE.SESSION_TOKEN_EXECUTE)) {
			token = session.getAttribute(Constants.SESSION_TYPE.SESSION_TOKEN_EXECUTE).toString();
			session.removeAttribute(token);
		}
		token = commons.csRandomAlphaNumbericString(5);
		session.removeAttribute(Constants.SESSION_TYPE.SESSION_TOKEN_EXECUTE);
		
		List<String> ids = new ArrayList<String>();
		String _ids = commons.getParameterFromRequest(req, "_ids").replaceAll("\\s", "");
		try {
			ids = Json.serializer().fromJson(commons.decodeBase64ToString(_ids), new TypeReference<List<String>>() {});
		} catch (Exception e) {
			
		}

		CurrentUserProfile cup = getCurrentlyAuthenticatedPrincipal();
		BaseDTO dto = new BaseDTO(req);
		
		Msg msg = dto.createMsg(cup, Constants.MSG_ACTION_CODE.SIGNALL);
		HashMap<String, Object> hData = new HashMap<>();
		hData.put("ids", ids);
		hData.put("SL", ids.size());
		msg.setObjData(hData);
		
		JSONRoot root = new JSONRoot(msg);
		MsgRsp rsp = restAPI.callAPINormal("/einvoice_mtt/publish-hd-list", cup.getLoginRes().getToken(), HttpMethod.POST, root);
		MspResponseStatus rspStatus = rsp.getResponseStatus();
		if(rspStatus.getErrorCode() != 0) {			
			dto.setErrorCode(rspStatus.getErrorCode());
			dto.setResponseData(rspStatus.getErrorDesc());
			return dto;
		}
		return dto;
	}
	
	
	public BaseDTO checkDataToRefreshAllStatusCqt(HttpServletRequest req, HttpSession session
			, String transaction, CurrentUserProfile cup) throws Exception{
		BaseDTO dto = new BaseDTO();
		dto.setErrorCode(0);
	
		_id = commons.getParameterFromRequest(req, "_token").replaceAll("\\s", "");
		if("".equals(_id)) {
			dto.setErrorCode(1);
			dto.getErrorMessages().add("Không tìm thấy thông tin hóa đơn.");
		}
		
		return dto;
	}
	@RequestMapping(value = "/refreshAll-status-cqt",  produces = MediaType.APPLICATION_JSON_VALUE, method = RequestMethod.POST)
	@ResponseBody
	public BaseDTO execRefreshAllStatusCQT(Locale locale, HttpServletRequest req, HttpSession session
			, @RequestAttribute(name = "transaction", value = "", required = false) String transaction) throws Exception {
		BaseDTO dto = new BaseDTO();		
		
		CurrentUserProfile cup = getCurrentlyAuthenticatedPrincipal();
		dto = checkDataToRefreshAllStatusCqt(req, session, transaction, cup);
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
		MsgRsp rsp = restAPI.callAPINormal("/einvoice/refreshAll-status-cqt", cup.getLoginRes().getToken(), HttpMethod.POST, root);
		MspResponseStatus rspStatus = rsp.getResponseStatus();
		if(rspStatus.getErrorCode() == 0) {
			String sendMail = rsp.getResponseStatus().getErrorDesc();			
			
			String encodedString = Base64.getEncoder().encodeToString(sendMail.getBytes());
			
			HashMap<String, String> hInfo = new HashMap<String, String>();
			hInfo.put("IDSendMail", encodedString);
			
			dto.setResponseData(hInfo);
			dto.setErrorCode(0);
		}else {
			dto.setErrorCode(rspStatus.getErrorCode());
			dto.setResponseData(rspStatus.getErrorDesc());
		}
		return dto;
	}
	
	
	@RequestMapping(value = "/createTDSendTax",  produces = MediaType.APPLICATION_JSON_VALUE, method = RequestMethod.POST)
	@ResponseBody
	public BaseDTO createTDSendTax(Locale locale, HttpServletRequest req, HttpSession session
			, @RequestAttribute(name = "transaction", value = "", required = false) String transaction) throws Exception {
		BaseDTO dto = new BaseDTO();		
		
		CurrentUserProfile cup = getCurrentlyAuthenticatedPrincipal();
		dto = checkDataToPublish(req, session, transaction, cup);
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
		MsgRsp rsp = restAPI.callAPINormal("/einvoice_mtt/createTDSendTax", cup.getLoginRes().getToken(), HttpMethod.POST, root);
		MspResponseStatus rspStatus = rsp.getResponseStatus();
		if(rspStatus.getErrorCode() == 0) {
			dto.setErrorCode(0);
		}else {
			dto.setErrorCode(rspStatus.getErrorCode());
			dto.setResponseData(rspStatus.getErrorDesc());
		}
		return dto;
	}
	
	@RequestMapping(value = "/create-td-send-tax-list",  produces = MediaType.APPLICATION_JSON_VALUE, method = RequestMethod.POST)
	@ResponseBody
	public BaseDTO createTDSendTaxList(Locale locale, HttpServletRequest req, HttpSession session
			, @RequestAttribute(name = "transaction", value = "", required = false) String transaction) throws Exception {
		List<String> ids = new ArrayList<String>();
		String _ids = commons.getParameterFromRequest(req, "_ids").replaceAll("\\s", "");
		try {
			ids = Json.serializer().fromJson(commons.decodeBase64ToString(_ids), new TypeReference<List<String>>() {});
		} catch (Exception e) {
			
		}

		CurrentUserProfile cup = getCurrentlyAuthenticatedPrincipal();
		BaseDTO dto = new BaseDTO(req);
		
		Msg msg = dto.createMsg(cup, Constants.MSG_ACTION_CODE.SIGNALL);
		HashMap<String, Object> hData = new HashMap<>();
		hData.put("ids", ids);
		hData.put("SL", ids.size());
		msg.setObjData(hData);
		
		JSONRoot root = new JSONRoot(msg);
		MsgRsp rsp = restAPI.callAPINormal("/einvoice_mtt/create-td-send-tax-list", cup.getLoginRes().getToken(), HttpMethod.POST, root);
		MspResponseStatus rspStatus = rsp.getResponseStatus();
		if(rspStatus.getErrorCode() != 0) {			
			dto.setErrorCode(rspStatus.getErrorCode());
			dto.setResponseData(rspStatus.getErrorDesc());
			return dto;
		}
		return dto;
	}
	
}
