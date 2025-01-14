package vn.sesgroup.hddt.controller.einvoice;

import java.io.File;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.multipart.MultipartFile;

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
import vn.sesgroup.hddt.dto.IssuerInfo;
import vn.sesgroup.hddt.dto.JsonGridDTO;
import vn.sesgroup.hddt.resources.APIParams;
import vn.sesgroup.hddt.resources.RestAPIUtility;
import vn.sesgroup.hddt.utils.Constants;
import vn.sesgroup.hddt.utils.Json;
import vn.sesgroup.hddt.utils.SystemParams;

@Controller
@RequestMapping({
	"/tbhdssot-cre",
	"/tbhdssot-detail",
	"/tbhdssot-edit",
	"/tbhdssot-history",
	"/tbhdssot-sign"
})
@Scope(value = WebApplicationContext.SCOPE_REQUEST)
public class TBHDSSotCRUDController extends AbstractController{
	private static final Logger log = LogManager.getLogger(TBHDSSotCRUDController.class);
	@Autowired RestAPIUtility restAPI;
	@Autowired RestTemplate restTemplate;
	
	private String errorCode;
	private String errorDesc;
	private String _id;	
	private String tinhThanh;
	private String cqtQLy;
	private String coQuanThue;
	private String loaiThongBao;
	private String soTBcuaCQT;
	private String ngayTBcuaCQT;
	private String dsHD;
	
	private void LoadParameter(CurrentUserProfile cup, Locale locale, HttpServletRequest req, String action) {
		try {
			BaseDTO baseDTO = new BaseDTO(req);
			Msg msg = baseDTO.createMsg(cup, Constants.MSG_ACTION_CODE.LOAD_PARAMS);
			
			/*DANH SACH THAM SO*/
			HashMap<String, String> hashConds = null;
			ArrayList<HashMap<String, String>> conds = null;
			MsgParam msgParam = null;
			MsgParams msgParams = new MsgParams();
			
			msgParam = new MsgParam();
			msgParam.setId("param01");
			msgParam.setParam("DMTinhThanh");
			msgParams.getParams().add(msgParam);
			
			msgParam = new MsgParam();
			msgParam.setId("param02");
			msgParam.setParam("DMChiCucThue");			
			conds = new ArrayList<>();
			hashConds = new HashMap<>();
			hashConds.put("cond", "tinhthanh_ma");
			hashConds.put("condval", null == tinhThanh? "": tinhThanh);
			conds.add(hashConds);
			msgParam.setConds(conds);			
			msgParams.getParams().add(msgParam);
			
			/*END: DANH SACH THAM SO*/
			msg.setObjData(msgParams);
			
			JSONRoot root = new JSONRoot(msg);
			MsgRsp rsp = restAPI.callAPINormal("/commons/get-full-params", cup.getLoginRes().getToken(), HttpMethod.POST, root);
			MspResponseStatus rspStatus = rsp.getResponseStatus();
			
			if(rspStatus.getErrorCode() == 0 &&
					rsp.getObjData() != null) {
				LinkedHashMap<String, String> hItem = null;
				
				JsonNode jsonData = Json.serializer().nodeFromObject(rsp.getObjData());
				if(null != jsonData.at("/param01") && jsonData.at("/param01") instanceof ArrayNode) {
					hItem = new LinkedHashMap<String, String>();
					for(JsonNode o: jsonData.at("/param01")) {
						hItem.put(commons.getTextJsonNode(o.get("code")), commons.getTextJsonNode(o.get("name")));
					}
					req.setAttribute("map_dmtinhthanh", hItem);
				}
				if(null != jsonData.at("/param02") && jsonData.at("/param02") instanceof ArrayNode) {
					hItem = new LinkedHashMap<String, String>();
					for(JsonNode o: jsonData.at("/param02")) {
						hItem.put(commons.getTextJsonNode(o.get("code")), commons.getTextJsonNode(o.get("name")));
					}
					req.setAttribute("map_cucthue", hItem);
				}
				
			}
		}catch(Exception e) {}
	}
	
	@RequestMapping(value = "/init", method = {RequestMethod.POST})
	public String init(Locale locale, HttpServletRequest req, HttpSession session
			, @RequestAttribute(name = "transaction", value = "", required = false) String transaction) throws Exception{
		errorCode = "";
		errorDesc = "";
		
		CurrentUserProfile cup = getCurrentlyAuthenticatedPrincipal();
		IssuerInfo ii = cup.getLoginRes().getIssuerInfo();
		
		req.setAttribute("tenNnt", ii.getName());
		req.setAttribute("mst", ii.getTaxCode());
		
		req.setAttribute("MSo", "04/SS-HĐĐT");
		req.setAttribute("Ten", "Thông báo hóa đơn điện tử có sai sót");
		req.setAttribute("Loai", "1");
		
		_id = commons.getParameterFromRequest(req, "_id");
		String header = "Tạo thông báo hóa đơn sai sót";
		String action = "CREATE";
		boolean isEdit = false;
		
		switch (transaction) {
		case "tbhdssot-cre":
			header = "Tạo thông báo hóa đơn sai sót";
			action = "CREATE";
			isEdit = true;
			break;
		case "tbhdssot-edit":
			header = "Thay đổi thông báo HĐ sai sót";
			action = "EDIT";
			isEdit = true;
			break;
		case "tbhdssot-detail":
			header = "Chi tiết thông báo HĐ sai sót";
			action = "DETAIL";
			isEdit = false;
			break;
		case "tbhdssot-sign":
			header = "Ký thông báo HĐ sai sót";
			action = "SIGN";
			isEdit = false;
			break;
		default:
			break;
		}
		
		if("|tbhdssot-edit|tbhdssot-sign|tbhdssot-detail|".indexOf(transaction) != -1)
			inquiry(cup, locale, req, session, _id, action);
		
		req.setAttribute("_header_", header);
		req.setAttribute("_action_", action);
		req.setAttribute("_isedit_", isEdit);
		req.setAttribute("_id", _id);

		
		if("|tbhdssot-cre|tbhdssot-edit|".indexOf(transaction) != -1)
			LoadParameter(cup, locale, req, action);
		
		req.setAttribute("map_loaitb_hdss", Constants.MAP_LOAITB_HDSS);
		
		if(!"".equals(errorDesc))
			req.setAttribute("messageError", errorDesc);
		
		return "einvoice/tbhdssot-crud";
	}
	
	
	
	@RequestMapping(value = "/history", method = {RequestMethod.POST})
	public String history(Locale locale, HttpServletRequest req, HttpSession session
			, @RequestAttribute(name = "transaction", value = "", required = false) String transaction) throws Exception{
		errorCode = "";
		errorDesc = "";
		
		CurrentUserProfile cup = getCurrentlyAuthenticatedPrincipal();
		_id = commons.getParameterFromRequest(req, "_id");
		String header = "Tra cứu lịch sử mã CQT";
		String action = "HISTORY";
		boolean isEdit = false;
		if("|tbhdssot-history|".indexOf(transaction) != -1)
			inquiryhistory(cup, locale, req, session, _id, action);
		
		req.setAttribute("_header_", header);
		req.setAttribute("_action_", action);
		req.setAttribute("_isedit_", isEdit);
		req.setAttribute("_id", _id);


		return "einvoice/einvoice-history";
	}
	
	private void inquiryhistory(CurrentUserProfile cup, Locale locale, HttpServletRequest req, HttpSession session, String _id , String action) throws Exception{
		if("".equals(_id)) {
			errorCode = "NOT FOUND";
			errorDesc = "Không tìm thấy thông tin thông báo.";
			return;
		}
		BaseDTO baseDTO = new BaseDTO(req);
		JsonGridDTO grid = new JsonGridDTO();
		Msg msg = baseDTO.createMsg(cup, Constants.MSG_ACTION_CODE.INQUIRY);
		HashMap<String, String> hData = new HashMap<>();
		msg.setObjData(hData);
		JSONRoot root = new JSONRoot(msg);
		MsgRsp rsp = restAPI.callAPINormal("/tbhdssot/history/" + _id, cup.getLoginRes().getToken(), HttpMethod.POST, root);
		MspResponseStatus rspStatus = rsp.getResponseStatus();
		if(rspStatus.getErrorCode() == 0) {
			MsgPage page = rsp.getMsgPage();
			grid.setTotal(page.getTotalRows());
			StringBuilder sb = new StringBuilder();
			JsonNode jsonData = Json.serializer().nodeFromObject(rsp.getObjData());
			JsonNode rows = null;
			HashMap<String, String> hItem = null;
			LocalDateTime localdatetime = null;
			LocalDate localdate = null;
			List<Object> dshdon = new ArrayList<Object>();
			if(!jsonData.at("/rows").isMissingNode()) {
				rows = jsonData.at("/rows");
				for(JsonNode row: rows) {
					hItem = new HashMap<String, String>();
					hItem.put("STT", commons.getTextJsonNode(row.at("/STT")));
					 localdatetime = null;
					String ngay = commons.getTextJsonNode(row.at("/Date"));
					int nngay = ngay.length();
					if(nngay == 19) {
						 localdatetime = LocalDateTime.parse(ngay);
							ngay = commons.convertLocalDateTimeToString(localdatetime, Constants.FORMAT_DATE.FORMAT_DATE_TIME_WEB);
					}
					else {
						localdate = LocalDate.parse(ngay);
						ngay = commons.convertLocalDateTimeToString(localdate, Constants.FORMAT_DATE.FORMAT_DATE_WEB);
					}
				
				
					hItem.put("Date", 
							ngay
					);
					hItem.put("MLoi", commons.getTextJsonNode(row.at("/MLoi")));
					hItem.put("MTLoi", commons.getTextJsonNode(row.at("/MTLoi")));
					dshdon.add(hItem);
				}
				req.setAttribute("DSHDon", commons.encodeStringBase64(Json.serializer().toString(dshdon)));
			}
			
		}else {
			errorDesc = rspStatus.getErrorDesc();
		}
	
	}
	
	
	
	
	
	
	
	
	
	private void inquiry(CurrentUserProfile cup, Locale locale, HttpServletRequest req, HttpSession session, String _id , String action) throws Exception{
		if("".equals(_id)) {
			errorCode = "NOT FOUND";
			errorDesc = "Không tìm thấy thông tin thông báo.";
			return;
		}
		
		BaseDTO baseDTO = new BaseDTO(req);
		Msg msg = baseDTO.createMsg(cup, Constants.MSG_ACTION_CODE.INQUIRY);
		HashMap<String, String> hData = new HashMap<>();
		msg.setObjData(hData);
		
		JSONRoot root = new JSONRoot(msg);
		MsgRsp rsp = restAPI.callAPINormal("/tbhdssot/detail/" + _id, cup.getLoginRes().getToken(), HttpMethod.POST, root);
		MspResponseStatus rspStatus = rsp.getResponseStatus();
		if(rspStatus.getErrorCode() == 0) {
			JsonNode jsonData = Json.serializer().nodeFromObject(rsp.getObjData());
			
			if("EDIT|SIGN".indexOf(action) != -1 &&
					!"CREATED".equals(commons.getTextJsonNode(jsonData.at("/Status")))) {
				errorCode = "NOT FOUND";
				errorDesc = "Trạng thái thông báo sai sót không hợp lệ.";
				return;
			}
			
			String tmp = "";
			
//			req.setAttribute("TThanhCode", commons.getTextJsonNode(jsonData.at("/TinhThanhInfo/code")));
//			req.setAttribute("TThanhName", commons.getTextJsonNode(jsonData.at("/TinhThanhInfo/name")));
//			req.setAttribute("CQThueCode", commons.getTextJsonNode(jsonData.at("/ChiCucThueInfo/code")));
//			req.setAttribute("CQThueName", commons.getTextJsonNode(jsonData.at("/ChiCucThueInfo/name")));
			
			tinhThanh = commons.getTextJsonNode(jsonData.at("/TinhThanhInfo/code"));
			req.setAttribute("TThanhCode", tinhThanh);
			req.setAttribute("TThanhName", commons.getTextJsonNode(jsonData.at("/TinhThanhInfo/name")));
			cqtQLy = commons.getTextJsonNode(jsonData.at("/ChiCucThueInfo/code"));
			req.setAttribute("CQThueCode", cqtQLy);
			req.setAttribute("CQThueName", commons.getTextJsonNode(jsonData.at("/ChiCucThueInfo/name")));
			
			req.setAttribute("Loai", commons.getTextJsonNode(jsonData.at("/Loai")));
			if("2".equals(commons.getTextJsonNode(jsonData.at("/Loai")))) {
				tmp = commons.getTextJsonNode(jsonData.at("/NTBCCQT"));
				req.setAttribute("So", commons.getTextJsonNode(jsonData.at("/So")));
				req.setAttribute("NTBCCQT", 
					!"".equals(tmp) && commons.checkLocalDate(tmp, "yyyy-MM-dd")?
					commons.convertLocalDateTimeToString(commons.convertStringToLocalDate(tmp, "yyyy-MM-dd"), Constants.FORMAT_DATE.FORMAT_DATE_WEB):
					""
				);
			}
			HashMap<String, String> hItem = null;
			List<Object> dshdon = new ArrayList<Object>();
			if(!jsonData.at("/DSHDon").isMissingNode()) {
				for(JsonNode o: jsonData.at("/DSHDon")) {
					hItem = new LinkedHashMap<String, String>();
					
					hItem.put("STT", commons.getTextJsonNode(o.at("/STT")));
					hItem.put("MSHDon", commons.getTextJsonNode(o.at("/KHMSHDon")) + commons.getTextJsonNode(o.at("/KHHDon")));
					hItem.put("SHDon", commons.formatNumberBillInvoice(commons.getTextJsonNode(o.at("/SHDon"))));
					if(!"".equals(commons.getTextJsonNode(o.at("/Ngay"))) && commons.checkLocalDate(commons.getTextJsonNode(o.at("/Ngay")), "yyyy-MM-dd")) {
						hItem.put("Ngay", 
							commons.convertLocalDateTimeToString(commons.convertStringToLocalDate(commons.getTextJsonNode(o.at("/Ngay")), "yyyy-MM-dd"), Constants.FORMAT_DATE.FORMAT_DATE_WEB)
						);
					}
					hItem.put("MCQTCap", commons.getTextJsonNode(o.at("/MCQTCap")));
					if("SIGN".equals(action) || "DETAIL".equals(action)) {
						hItem.put("LADHDDT", Constants.MAP_HDSS_LOAI_AD_HDDT.get(commons.getTextJsonNode(o.at("/LADHDDT"))));
						hItem.put("TCTBao", Constants.MAP_HDSS_TCTBAO.get(commons.getTextJsonNode(o.at("/TCTBao"))));	
					}else {
						hItem.put("LADHDDT", commons.getTextJsonNode(o.at("/LADHDDT")));
						hItem.put("TCTBao", commons.getTextJsonNode(o.at("/TCTBao")));
					}
					
					
					hItem.put("LDo", commons.getTextJsonNode(o.at("/LDo")));
					dshdon.add(hItem);
				}
			}
			req.setAttribute("DSHDon", commons.encodeStringBase64(Json.serializer().toString(dshdon)));
			
		}else {
			errorDesc = rspStatus.getErrorDesc();
		}
	}
	
	
	
	
	public BaseDTO checkDataToAccept(HttpServletRequest req, HttpSession session, String transaction
			, CurrentUserProfile cup) throws Exception{
		BaseDTO dto = new BaseDTO();
		dto.setErrorCode(0);
		
		_id = commons.getParameterFromRequest(req, "_id").replaceAll("\\s", "");
		tinhThanh = commons.getParameterFromRequest(req, "tinh-thanh").replaceAll("\\s", "");
		cqtQLy = commons.getParameterFromRequest(req, "CQTQLy").replaceAll("\\s", "");
		loaiThongBao = commons.getParameterFromRequest(req, "Loai").replaceAll("\\s", "");
		soTBcuaCQT = commons.getParameterFromRequest(req, "So").trim().replaceAll("\\s+", " ");
		ngayTBcuaCQT = commons.getParameterFromRequest(req, "NTBCCQT").replaceAll("\\s", "");
		dsHD = commons.getParameterFromRequest(req, "ds-hd").replaceAll("\\s", "");
		
		switch (transaction) {
		case "tbhdssot-cre":
			if("".equals(tinhThanh)) {
				dto.setErrorCode(1);
				dto.getErrorMessages().add("Vui lòng chọn tỉnh/thành phố.");
			}
			if("".equals(coQuanThue)) {
				dto.setErrorCode(1);
				dto.getErrorMessages().add("Vui lòng chọn cơ quan thuế.");
			}
			if("".equals(loaiThongBao) || Constants.MAP_LOAITB_HDSS.get(loaiThongBao) == null) {
				dto.setErrorCode(1);
				dto.getErrorMessages().add("Vui lòng chọn loại thông báo.");
			}
			if("2".equals(loaiThongBao)) {
				if("".equals(soTBcuaCQT)) {
					dto.setErrorCode(1);
					dto.getErrorMessages().add("Vui lòng nhập số TB của CQT");
				}
				if("".equals(ngayTBcuaCQT) || !commons.checkLocalDate(ngayTBcuaCQT, Constants.FORMAT_DATE.FORMAT_DATE_WEB)) {
					dto.setErrorCode(1);
					dto.getErrorMessages().add("Vui lòng chọn ngày TB của CQT");
				}
			}
			break;
		case "tbhdssot-edit":
			if("".equals(tinhThanh)) {
				dto.setErrorCode(1);
				dto.getErrorMessages().add("Vui lòng chọn tỉnh/thành phố.");
			}
			if("".equals(coQuanThue)) {
				dto.setErrorCode(1);
				dto.getErrorMessages().add("Vui lòng chọn cơ quan thuế.");
			}
			if("".equals(loaiThongBao) || Constants.MAP_LOAITB_HDSS.get(loaiThongBao) == null) {
				dto.setErrorCode(1);
				dto.getErrorMessages().add("Vui lòng chọn loại thông báo.");
			}
			if("2".equals(loaiThongBao)) {
				if("".equals(soTBcuaCQT)) {
					dto.setErrorCode(1);
					dto.getErrorMessages().add("Vui lòng nhập số TB của CQT");
				}
				if("".equals(ngayTBcuaCQT) || !commons.checkLocalDate(ngayTBcuaCQT, Constants.FORMAT_DATE.FORMAT_DATE_WEB)) {
					dto.setErrorCode(1);
					dto.getErrorMessages().add("Vui lòng chọn ngày TB của CQT");
				}
			}
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
		String messageConfirm = "Bạn có muốn tạo thông báo hóa đơn sai sót không?";
		switch (transaction) {
		case "tbhdssot-cre":
			messageConfirm = "Bạn có muốn tạo thông báo hóa đơn sai sót không?";
			break;
		case "tbhdssot-edit":
			messageConfirm = "Bạn có muốn thay đổi thông báo hóa đơn sai sót không?";
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

	JsonNode jsonNodeTmp = null;
		
		jsonNodeTmp = null;
		try {
			jsonNodeTmp = Json.serializer().nodeFromJson(commons.decodeBase64ToString(dsHD));
		}catch(Exception e) {
			log.error(" >>>>> An exception occurred!", e);
		}
		
		if(null == jsonNodeTmp || jsonNodeTmp.size() == 0) {
			dto.setErrorCode(999);
			dto.setResponseData("Vui lòng chọn danh sách hóa đơn để thực hiện.");
			return dto;
		}
		
		/*KIEM TRA THONG TIN*/
		boolean check = true;
		int count = 0;
		JsonNode jsonNode = null;
		while(count < jsonNodeTmp.size() && check) {
			jsonNode = jsonNodeTmp.get(count);
			if("".equals(commons.getTextJsonNode(jsonNode.at("/MCQTCap")))
				|| "|1|2|3|4|".indexOf("|" + commons.getTextJsonNode(jsonNode.at("/TCTBao")) + "|") == -1
				|| "".equals(commons.getTextJsonNode(jsonNode.at("/LDo")).trim().replaceAll("\\s+", " "))
			) {
				check = false;
				break;	
			}
			count++;
		}
		if(!check) {
			dto.setErrorCode(999);
			dto.setResponseData("Vui lòng kiểm tra lại dữ liệu hóa đơn.");
			return dto;
		}
		/*END - KIEM TRA THONG TIN*/
		
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
		JsonNode jsonNodeTmp = null;
		try {
			jsonNodeTmp = Json.serializer().nodeFromJson(commons.decodeBase64ToString(dsHD));
		}catch(Exception e) {
			log.error(" >>>>> An exception occurred!", e);
		}
		if(null == jsonNodeTmp || jsonNodeTmp.size() == 0) {
			dtoRes.setErrorCode(999);
			dtoRes.setResponseData("Vui lòng chọn từng hóa đơn để thực hiện.");
			return dtoRes;
		}
		
		/*KIEM TRA THONG TIN*/
		boolean check = true;
		int count = 0;
		JsonNode jsonNode = null;
		while(count < jsonNodeTmp.size() && check) {
			jsonNode = jsonNodeTmp.get(count);
			if("".equals(commons.getTextJsonNode(jsonNode.at("/MCQTCap")))
				|| "|1|2|3|4|".indexOf("|" + commons.getTextJsonNode(jsonNode.at("/TCTBao")) + "|") == -1
				|| "".equals(commons.getTextJsonNode(jsonNode.at("/LDo")).trim().replaceAll("\\s+", " "))
			) {
				check = false;
				break;	
			}
			count++;
		}
		if(!check) {
			dtoRes.setErrorCode(999);
			dtoRes.setResponseData("Vui lòng kiểm tra lại dữ liệu hóa đơn.");
			return dtoRes;
		}
		/*END - KIEM TRA THONG TIN*/
		
		String actionCode = Constants.MSG_ACTION_CODE.CREATED;
		switch (transaction) {
		case "tbhdssot-cre": actionCode = Constants.MSG_ACTION_CODE.CREATED; break;
		case "tbhdssot-edit": actionCode = Constants.MSG_ACTION_CODE.MODIFY; break;
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
		hData.put("TinhThanh", tinhThanh);
		hData.put("CoQuanThue", cqtQLy);
		hData.put("LoaiThongBao", loaiThongBao);
		hData.put("SoTBcuaCQT", soTBcuaCQT);
		hData.put("NgayTBcuaCQT", ngayTBcuaCQT);
		hData.put("DSHDon", jsonNodeTmp);
		
		msg.setObjData(hData);
		JSONRoot root = new JSONRoot(msg);
		MsgRsp rsp = restAPI.callAPINormal("/tbhdssot/crud", cup.getLoginRes().getToken(), HttpMethod.POST, root);
		MspResponseStatus rspStatus = rsp.getResponseStatus();
		if(rspStatus.getErrorCode() == 0) {
			dtoRes.setErrorCode(0);
			switch (transaction) {
			case "tbhdssot-cre":
				dtoRes.setResponseData("Tạo thông báo hóa đơn sai sót thành công.");
				break;
			case "tbhdssot-edit":
				dtoRes.setResponseData("Cập nhật thông tin hóa đơn sai sót thành công.");
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
	
	@RequestMapping(value = "/check-data-sign",  produces = MediaType.APPLICATION_JSON_VALUE, method = RequestMethod.POST)
	@ResponseBody
	public BaseDTO execCheckDataToSign(Locale locale, HttpServletRequest req, HttpSession session
			, @RequestAttribute(name = "transaction", value = "", required = false) String transaction) throws Exception {
		String token = "";
		if (null != session.getAttribute(Constants.SESSION_TYPE.SESSION_TOKEN_EXECUTE)) {
			token = session.getAttribute(Constants.SESSION_TYPE.SESSION_TOKEN_EXECUTE).toString();
			session.removeAttribute(token);
		}
		session.removeAttribute(Constants.SESSION_TYPE.SESSION_TOKEN_EXECUTE);
		
		CurrentUserProfile cup = getCurrentlyAuthenticatedPrincipal();
		BaseDTO dto = new BaseDTO();
		_id = commons.getParameterFromRequest(req, "_id").replaceAll("\\s", "");
		
		if(!"tbhdssot-sign".equals(transaction)) {
			dto = new BaseDTO();
			dto.setErrorCode(998);
			dto.setResponseData("Chức năng giao dịch không hợp lệ.");
			return dto;
		}
		
		if("".equals(_id)) {
			dto.setErrorCode(1);
			dto.setResponseData("Không tìm thấy thông báo hđ sai sót.");
			return dto;
		}
		
		/*LAY THONG TIN DU LIEU XML VE SERVER WEB*/
		dto = new BaseDTO(req);
		Msg msg = dto.createMsg(cup, Constants.MSG_ACTION_CODE.CREATED);
		HashMap<String, Object> hData = new HashMap<>();
		hData.put("_id", _id);
		msg.setObjData(hData);
		JSONRoot root = new JSONRoot(msg);
		
		FileInfo fileInfo = restAPI.callAPIGetFileInfo("/tbhdssot/get-file-for-sign", cup.getLoginRes().getToken(), HttpMethod.POST, root);
		if(null == fileInfo || null == fileInfo.getContentFile()) {
			dto.setErrorCode(999);
			dto.setResponseData("Không tìm thấy dữ liệu hóa đơn.");
			return dto;
		}
		
		/*THONG TIN TEN FILE*/
		token = commons.convertLocalDateTimeToString(LocalDateTime.now(), Constants.FORMAT_DATE.FORMAT_DATETIME_DB_FULL) + "-" + commons.csRandomAlphaNumbericString(5);
		token += ".xml";
		File file = new File(SystemParams.DIR_TMP_SAVE_FILES);
		file.mkdirs();
		FileUtils.writeByteArrayToFile(new File(SystemParams.DIR_TMP_SAVE_FILES, token), fileInfo.getContentFile());
		/*END - LAY THONG TIN DU LIEU XML VE SERVER WEB*/
		
		HashMap<String, String> hInfo = new HashMap<String, String>();
		hInfo.put("TOKEN", token);
		
		dto.setResponseData(hInfo);
		dto.setErrorCode(0);
		return dto;
	}
	
	public BaseDTO checkDataToSign(HttpServletRequest req, HttpSession session
			, String transaction, CurrentUserProfile cup) throws Exception{
		BaseDTO dto = new BaseDTO();
		dto.setErrorCode(0);
		
		return dto;
	}
	
	@RequestMapping(
			value = "/signFile"
			, produces = MediaType.APPLICATION_JSON_VALUE
			, method = RequestMethod.POST
			, consumes = MediaType.MULTIPART_FORM_DATA_VALUE
		)
	@ResponseBody
	public BaseDTO processSignFile(HttpServletRequest req, HttpSession session
			, @RequestAttribute(name = "transaction", value = "", required = false) String transaction
			, @RequestParam("XMLFileSigned") MultipartFile multipartFile
			, @RequestParam("certificate") String certificate
			, @RequestParam("_id") String _id
			) throws Exception{
		BaseDTO dtoRes = new BaseDTO();
		
		CurrentUserProfile cup = getCurrentlyAuthenticatedPrincipal();
		dtoRes = checkDataToSign(req, session, transaction, cup);
		if(0 != dtoRes.getErrorCode()) {
			dtoRes.setErrorCode(999);
			dtoRes.setResponseData(Constants.MAP_ERROR.get(999));
			return dtoRes;
		}
		
		dtoRes = new BaseDTO(req);
		Msg msg = dtoRes.createMsg(cup, Constants.MSG_ACTION_CODE.SIGNED);
		HashMap<String, Object> hData = new HashMap<>();
		
		msg.setObjData(hData);
		JSONRoot root = new JSONRoot(msg);
		
//		FileCopyUtils.copy(multipartFile.getBytes(), Paths.get(SystemParams.DIR_TMP_SAVE_FILES, "tmp.xml").toFile());
		
		/*CONNECT TO API*/
		HttpHeaders headers = new HttpHeaders();
		headers.setAccept(Arrays.asList(new MediaType[] { MediaType.APPLICATION_JSON }));
		headers.setContentType(MediaType.MULTIPART_FORM_DATA);
		headers.add(APIParams.API_LICENSE_KEY_NAME, APIParams.HTTP_LICENSEKEY);
		headers.add(Constants.TOKEN_HEADER, cup.getLoginRes().getToken());
		
		MultiValueMap<String, String> fileMap = null;
		HttpEntity<byte[]> fileEntity = null;
		ContentDisposition contentDisposition = null;
		
		MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
		body.add("Base64JsonRoot", commons.encodeStringBase64(Json.serializer().toString(root)));
		body.add("_id", _id);
		
		/*ADD DU LIEU XML DA KY*/
		fileMap = new LinkedMultiValueMap<String, String>();
		contentDisposition = ContentDisposition.builder("form-data").name("XMLFileSigned").filename("einvoice-signed.xml").build();
		fileMap.add(HttpHeaders.CONTENT_DISPOSITION, contentDisposition.toString());
		fileEntity = new HttpEntity<byte[]>(multipartFile.getBytes(), fileMap);
		body.add("XMLFileSigned", fileEntity);
		
		HttpEntity<MultiValueMap<String, Object>> requestBody = new HttpEntity<>(body, headers);
		String url = "/tbhdssot/sign-single";
		ResponseEntity<MsgRsp> result = restTemplate.exchange(APIParams.HTTP_URI + url, HttpMethod.POST, requestBody, MsgRsp.class);
		/*END - CONNECT TO API*/		
		if (result.getStatusCode() == org.springframework.http.HttpStatus.OK) {
			MsgRsp rsp = result.getBody();
			MspResponseStatus rspStatus = rsp.getResponseStatus();
			if(rspStatus.getErrorCode() == 0) {
				dtoRes.setErrorCode(0);
				dtoRes.setResponseData(rsp.getObjData());
			}else {
				dtoRes = new BaseDTO(rspStatus.getErrorCode(), rspStatus.getErrorDesc());
			}
		}else {
			dtoRes = new BaseDTO(result.getStatusCode().value(), "Thực hiện ký thông báo hđ sai sót không thành công.");
		}
		return dtoRes;
	}
	
}
