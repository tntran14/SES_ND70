package vn.sesgroup.hddt.controller.cks;

import java.io.File;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
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
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.context.WebApplicationContext;

import com.api.message.JSONRoot;
import com.api.message.Msg;
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
import vn.sesgroup.hddt.resources.RestAPIUtility;
import vn.sesgroup.hddt.utils.Constants;
import vn.sesgroup.hddt.utils.Json;
import vn.sesgroup.hddt.utils.SystemParams;

@Controller
@RequestMapping({
	"/cks-cre"
	, "/cks-edit"
	, "/cks-detail"
	, "/cks-sign"
	, "/cks-del"
})
@Scope(value = WebApplicationContext.SCOPE_REQUEST)
public class CKSCRUDControlller extends AbstractController{
	private static final Logger log = LogManager.getLogger(CKSCRUDControlller.class);
	@Autowired RestAPIUtility restAPI;
	@Autowired RestTemplate restTemplate;
	
	private String errorCode;
	private String errorDesc;
	private String _id;
	private String tenNnt;
	private String mauSo;
	private String ten;
	private String mst;
	private String tinhThanh;
	private String cqtQLy;

	private String nLap;
	
	private String dsctsSDung;
	
	private String hThuc;
	
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
		
		LocalDate now = LocalDate.now();
		ten = "Chứng thư số đăng ký/thay đổi thông tin sử dụng hóa đơn điện tử";
		nLap = commons.convertLocalDateTimeToString(now, Constants.FORMAT_DATE.FORMAT_DATE_WEB);
		
		req.setAttribute("tenNnt", ii.getName());
		req.setAttribute("ten", ten);
		req.setAttribute("nLap", nLap);
		req.setAttribute("mst", ii.getTaxCode());
		

		
		_id = commons.getParameterFromRequest(req, "_id");
		String header = "Đăng ký chứng thư số";
		String action = "CREATE";
		boolean isEdit = false;
		
		switch (transaction) {
		case "cks-cre":
			header = "Đăng ký chứng thư số";
			action = "CREATE";
			isEdit = true;
			break;
		case "cks-edit":
			header = "Thay đổi thông tin chứng thư số";
			action = "EDIT";
			isEdit = true;
			break;
		case "cks-detail":
			header = "Chi tiết chứng thư số";
			action = "DETAIL";
			isEdit = false;
			break;
		case "cks-sign":
			header = "Ký chứng thư số";
			action = "SIGN";
			isEdit = false;
			break;
		default:
			break;
		}
		
		if("|cks-detail|cks-sign|cks-edit|".indexOf(transaction) != -1)
			inquiry(cup, locale, req, session, _id, action);
		
		req.setAttribute("_header_", header);
		req.setAttribute("_action_", action);
		req.setAttribute("_isedit_", isEdit);
		req.setAttribute("_id", _id);
		
		LoadParameter(cup, locale, req, action);
		
		if(!"".equals(errorDesc))
			req.setAttribute("messageError", errorDesc);
		
		return "cks/cks-crud";
	}
	
	private void inquiry(CurrentUserProfile cup, Locale locale, HttpServletRequest req, HttpSession session, String _id , String action) throws Exception{
		if("".equals(_id)) {
			errorCode = "NOT FOUND";
			errorDesc = "Không tìm thấy thông tin chứng thư số.";
			return;
		}
		BaseDTO baseDTO = new BaseDTO(req);
		Msg msg = baseDTO.createMsg(cup, Constants.MSG_ACTION_CODE.INQUIRY);
		HashMap<String, String> hData = new HashMap<>();
		msg.setObjData(hData);
		
		JSONRoot root = new JSONRoot(msg);
		MsgRsp rsp = restAPI.callAPINormal("/cks/detail/" + _id, cup.getLoginRes().getToken(), HttpMethod.POST, root);
		MspResponseStatus rspStatus = rsp.getResponseStatus();
		if(rspStatus.getErrorCode() == 0) {
			JsonNode jsonData = Json.serializer().nodeFromObject(rsp.getObjData());
			
			if("EDIT|SIGN".indexOf(action) != -1 &&
					!"CREATED".equals(commons.getTextJsonNode(jsonData.at("/Status")))) {
				errorCode = "NOT FOUND";
				errorDesc = "Trạng thái chứng thư số không hợp lệ.";
				return;
			}
			
			req.setAttribute("tenNnt", commons.getTextJsonNode(jsonData.at("/TenNnt")));
			req.setAttribute("MSo", commons.getTextJsonNode(jsonData.at("/MSo")));
			req.setAttribute("ten", commons.getTextJsonNode(jsonData.at("/Ten")));
			req.setAttribute("mst", commons.getTextJsonNode(jsonData.at("/MST")));
	
			
			tinhThanh = commons.getTextJsonNode(jsonData.at("/TinhThanhInfo/code"));
			req.setAttribute("TThanhCode", tinhThanh);
			req.setAttribute("TThanhName", commons.getTextJsonNode(jsonData.at("/TinhThanhInfo/name")));
			cqtQLy = commons.getTextJsonNode(jsonData.at("/ChiCucThueInfo/code"));
			req.setAttribute("CQThueCode", cqtQLy);
			req.setAttribute("CQThueName", commons.getTextJsonNode(jsonData.at("/ChiCucThueInfo/name")));
			
			req.setAttribute("nLap", commons.convertLocalDateTimeToString(commons.convertLongToLocalDate(jsonData.at("/NLap").asLong()), Constants.FORMAT_DATE.FORMAT_DATE_WEB));
			
		
			HashMap<String, String> hItem = null;
			List<Object> prds = new ArrayList<Object>();
			if(!jsonData.at("/DSCTSSDung").isMissingNode()) {
				for(JsonNode o: jsonData.at("/DSCTSSDung")) {
					hItem = new LinkedHashMap<String, String>();
					hItem.put("TTChuc", commons.getTextJsonNode(o.at("/TTChuc")));
					hItem.put("Seri", commons.getTextJsonNode(o.at("/Seri")));
					hItem.put("TNgay", commons.convertLocalDateTimeToString(commons.convertLongToLocalDateTime(o.at("/TNgay").asLong()), Constants.FORMAT_DATE.FORMAT_DATE_TIME_WEB));
					hItem.put("DNgay", commons.convertLocalDateTimeToString(commons.convertLongToLocalDateTime(o.at("/DNgay").asLong()), Constants.FORMAT_DATE.FORMAT_DATE_TIME_WEB));
					if("EDIT".equals(action))
						hItem.put("HThuc", commons.getTextJsonNode(o.at("/HThuc")));
					else
						hItem.put("HThuc", Constants.MAP_TKHAI_HTHUC.get(commons.getTextJsonNode(o.at("/HThuc"))));
					
					prds.add(hItem);
				}
			}
			req.setAttribute("DSCTSSDung", Json.serializer().toString(prds));
		}else {
			errorDesc = rspStatus.getErrorDesc();
		}
	}
	
	public BaseDTO checkDataToAccept(HttpServletRequest req, HttpSession session, String transaction
			, CurrentUserProfile cup) throws Exception{
		BaseDTO dto = new BaseDTO();
		dto.setErrorCode(0);
		
		_id = commons.getParameterFromRequest(req, "_id").replaceAll("\\s", "");
		tenNnt = commons.getParameterFromRequest(req, "ten-nnt").trim().replaceAll("\\s+", " ");
		mauSo = commons.getParameterFromRequest(req, "mau-so").replaceAll("\\s", "");
		ten = commons.getParameterFromRequest(req, "ten").trim().replaceAll("\\s+", " ");
		mst = commons.getParameterFromRequest(req, "mst").replaceAll("\\s", "");
		tinhThanh = commons.getParameterFromRequest(req, "tinh-thanh").replaceAll("\\s", "");
		cqtQLy = commons.getParameterFromRequest(req, "CQTQLy").replaceAll("\\s", "");
	
		nLap = commons.getParameterFromRequest(req, "NLap").replaceAll("\\s", "");
	
		dsctsSDung = commons.getParameterFromRequest(req, "DSCTSSDung").replaceAll("\\s", "");
	
		switch (transaction) {
		case "cks-cre":
		case "cks-edit":
			
			if("".equals(mst)) {
				dto.setErrorCode(1);
				dto.getErrorMessages().add("Vui lòng nhập mã số thuế.");
			}
			if("W10=".equals(dsctsSDung)) {
				dto.setErrorCode(1);
				dto.getErrorMessages().add("Vui lòng chọn chứng thư số.");
			}
		
			if("".equals(tinhThanh)) {
				dto.setErrorCode(1);
				dto.getErrorMessages().add("Vui lòng chọn tỉnh/thành phố.");
			}
			if("".equals(cqtQLy)) {
				dto.setErrorCode(1);
				dto.getErrorMessages().add("Vui lòng chọn cơ quan thuế quản lý.");
			}
			
			if("".equals(nLap)) {
				dto.setErrorCode(1);
				dto.getErrorMessages().add("Vui lòng nhập ngày lập.");
			}else if(!commons.checkLocalDate(nLap, Constants.FORMAT_DATE.FORMAT_DATE_WEB)) {
				dto.setErrorCode(1);
				dto.getErrorMessages().add("Ngày lập không đúng định dạng.");
			}
			
			
			break;
		
		
		case "cks-del":
			if("".equals(_id)) {
				dto.setErrorCode(1);
				dto.getErrorMessages().add("Không tìm thấy thông tin chứng thư số.");
			}
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
		String messageConfirm = "Bạn có muốn đăng ký chứng thư số không?";
		switch (transaction) {
		case "cks-cre":
			messageConfirm = "Bạn có muốn đăng ký chứng thư số không?";
			break;
		case "cks-edit":
			messageConfirm = "Bạn có muốn thay đổi chứng thư số không?";
			break;
		case "cks-del":
			messageConfirm = "Bạn có muốn xóa chứng thư số này không?";
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
			jsonNodeTmp = Json.serializer().nodeFromJson(commons.decodeBase64ToString(dsctsSDung));
		}catch(Exception e) {
			log.error(" >>>>> An exception occurred!", e);
		}
		
		String actionCode = Constants.MSG_ACTION_CODE.CREATED;
		switch (transaction) {
		case "cks-cre": actionCode = Constants.MSG_ACTION_CODE.CREATED; break;
		case "cks-edit": actionCode = Constants.MSG_ACTION_CODE.MODIFY; break;
		case "cks-del": actionCode = Constants.MSG_ACTION_CODE.DELETE; break;
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
		hData.put("TenNnt", tenNnt);
		hData.put("MauSo", mauSo);
		hData.put("Ten", ten);
		hData.put("Mst", mst);
		hData.put("TinhThanh", tinhThanh);
		hData.put("CqtQLy", cqtQLy);
		hData.put("NLap", nLap);
		hData.put("DSCTSSDung", jsonNodeTmp);
		msg.setObjData(hData);
		
		JSONRoot root = new JSONRoot(msg);
		MsgRsp rsp = restAPI.callAPINormal("/cks/crud", cup.getLoginRes().getToken(), HttpMethod.POST, root);
		MspResponseStatus rspStatus = rsp.getResponseStatus();
		if(rspStatus.getErrorCode() == 0) {
			dtoRes.setErrorCode(0);
			switch (transaction) {
			case "cks-cre": dtoRes.setResponseData("Đăng ký chứng thư số thành công.");break;
			case "cks-edit": dtoRes.setResponseData("Thay đổi chứng thư số thành công.");break;
			case "cks-del": dtoRes.setResponseData("Xóa chứng thư số thành công.");break;
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
		
		if("".equals(_id)) {
			dto.setErrorCode(1);
			dto.setResponseData("Không tìm thấy chứng thư số cần ký.");
			return dto;
		}
		
		/*LAY THONG TIN DU LIEU XML VE SERVER WEB*/
		dto = new BaseDTO(req);
		Msg msg = dto.createMsg(cup, Constants.MSG_ACTION_CODE.CREATED);
		HashMap<String, Object> hData = new HashMap<>();
		hData.put("_id", _id);
		msg.setObjData(hData);
		JSONRoot root = new JSONRoot(msg);
		
		FileInfo fileInfo = restAPI.callAPIGetFileInfo("/tkhai/get-file-for-sign", cup.getLoginRes().getToken(), HttpMethod.POST, root);
		if(null == fileInfo.getContentFile()) {
			dto.setErrorCode(999);
			dto.setResponseData("Không tìm thấy dữ liệu chứng thư số.");
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
	

}
