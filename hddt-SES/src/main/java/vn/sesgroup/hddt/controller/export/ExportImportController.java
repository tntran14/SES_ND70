package vn.sesgroup.hddt.controller.export;


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
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
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
import vn.sesgroup.hddt.resources.RestAPIUtility;
import vn.sesgroup.hddt.utils.Constants;
import vn.sesgroup.hddt.utils.Json;
@Controller
@RequestMapping({
	"/export-import"
})

@Scope(value = WebApplicationContext.SCOPE_REQUEST)
public class ExportImportController extends AbstractController{
	private static final Logger log = LogManager.getLogger(ExportImportController.class);
	@Autowired RestAPIUtility restAPI;
	private String loaiTienTt;

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
			msgParam.setParam("DMPaymentType");
			msgParams.getParams().add(msgParam);
			
			msgParam = new MsgParam();
			msgParam.setId("param02");
			msgParam.setParam("DMMauSoKyHieuForCreate");
			msgParams.getParams().add(msgParam);
			
			msgParam = new MsgParam();
			msgParam.setId("param03");
			msgParam.setParam("DMCurrencies");
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
						hItem.put(commons.getTextJsonNode(o.get("code")), commons.getTextJsonNode(o.get("name")));
					}
					req.setAttribute("map_paymenttype", hItem);
				}
				if(null != jsonData.at("/param02") && jsonData.at("/param02") instanceof ArrayNode) {
					hItem = new LinkedHashMap<String, String>();
					for(JsonNode o: jsonData.at("/param02")) {
						if("6".equals(commons.getTextJsonNode(o.get("KHMSHDon")))){									
							KHHDon = commons.getTextJsonNode(o.get("KHHDon"));	
							char words=KHHDon.charAt(KHHDon.length() - 3);
							String s=String.valueOf(words);  
							if("N".equals(s)) {
								hItem.put(commons.getTextJsonNode(o.get("_id")), commons.getTextJsonNode(o.get("KHMSHDon")) + commons.getTextJsonNode(o.get("KHHDon")));	
							}
						}
					}
					req.setAttribute("map_mausokyhieu", hItem);
				}
				if(null != jsonData.at("/param03") && jsonData.at("/param03") instanceof ArrayNode) {
					hItem = new LinkedHashMap<String, String>();
					for(JsonNode o: jsonData.at("/param03")) {
						hItem.put(commons.getTextJsonNode(o.get("code")), commons.getTextJsonNode(o.get("code")));
						if(action.equals("CREATE") && null != o.get("IsDefault") && o.get("IsDefault").asBoolean(false)) {
							loaiTienTt = commons.getTextJsonNode(o.get("code"));
							req.setAttribute("DVTTe", loaiTienTt);
						}
					}
					req.setAttribute("map_currencies", hItem);
				}
			}
			
		}catch(Exception e) {}
	}
	@RequestMapping(value = "/init", method = {RequestMethod.POST})
	public String init(Locale locale, HttpServletRequest req, HttpSession session
			, @RequestAttribute(name = "transaction", value = "", required = false) String transaction, String action) throws Exception{
		CurrentUserProfile cup = getCurrentlyAuthenticatedPrincipal();
		req.setAttribute("_header_", "Import pxk kiêm vận chuyển nội bộ ");
		LoadParameter(cup, locale, req, action);
		return "export/export-import";
	}
	private String mauSoHdon;
	private String dataFileName;
	
	public BaseDTO checkDataToImport(HttpServletRequest req, HttpSession session, String transaction
			, CurrentUserProfile cup) throws Exception{
		BaseDTO dto = new BaseDTO();
		dto.setErrorCode(0);
		mauSoHdon = commons.getParameterFromRequest(req, "mau-so-hdon").replaceAll("\\s", "");
		dataFileName = commons.getParameterFromRequest(req, "dataFileName").replaceAll("\\s", "");
		if("".equals(dataFileName)) {
			dto.setErrorCode(1);
			dto.getErrorMessages().add("Vui lòng chọn tập tin chứa dữ liệu.");
		}if("".equals(mauSoHdon)) {
			dto.setErrorCode(1);
			dto.getErrorMessages().add("Vui lòng chọn mẫu số hóa đơn.");
		}
		
		return dto;
	}
	
	@RequestMapping(value = "/check-data-import",  produces = MediaType.APPLICATION_JSON_VALUE, method = RequestMethod.POST)
	@ResponseBody
	public BaseDTO execCheckDataToImport(Locale locale, HttpServletRequest req, HttpSession session
			, @RequestParam(value = "transaction", required = false, defaultValue = "") String transaction) throws Exception {
		String token = "";
		if (null != session.getAttribute(Constants.SESSION_TYPE.SESSION_TOKEN_EXECUTE)) {
			token = session.getAttribute(Constants.SESSION_TYPE.SESSION_TOKEN_EXECUTE).toString();
			session.removeAttribute(token);
		}
		session.removeAttribute(Constants.SESSION_TYPE.SESSION_TOKEN_EXECUTE);

		BaseDTO dto = new BaseDTO();	
		String messageConfirm = "Bạn có muốn thực hiện import không?";
		CurrentUserProfile cup = getCurrentlyAuthenticatedPrincipal();
		dto = checkDataToImport(req, session, transaction, cup);
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
	
	@RequestMapping(value = "/import",  produces = MediaType.APPLICATION_JSON_VALUE, method = RequestMethod.POST)
	@ResponseBody
	public BaseDTO execImportData(HttpServletRequest req, HttpSession session
			, @RequestParam(value = "transaction", required = false, defaultValue = "") String transaction
			, @RequestParam(value = "tokenTransaction", required = false, defaultValue = "") String tokenTransaction) throws Exception{
		BaseDTO dtoRes = new BaseDTO();
		
		CurrentUserProfile cup = getCurrentlyAuthenticatedPrincipal();
		dtoRes = checkDataToImport(req, session, transaction, cup);
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
			dtoRes.setErrorCode(997);
			dtoRes.setResponseData(Constants.MAP_ERROR.get(997));
			return dtoRes;
		}
		/*END: CHECK TOKEN*/
		
		dtoRes = new BaseDTO(req);
		Msg msg = dtoRes.createMsg(cup, Constants.MSG_ACTION_CODE.CREATED);
		HashMap<String, Object> hData = new HashMap<>();
		hData.put("DataFileName", dataFileName);
		hData.put("MauSoHdon", mauSoHdon);
		msg.setObjData(hData);
		JSONRoot root = new JSONRoot(msg);
		MsgRsp rsp = restAPI.callAPINormal("/export/import-data", cup.getLoginRes().getToken(), HttpMethod.POST, root);
		
		MspResponseStatus rspStatus = rsp.getResponseStatus();
		if(rspStatus.getErrorCode() == 0) {
			dtoRes.setErrorCode(0);
			dtoRes.setResponseData("Import thông tin sản phẩm thành công.");
		}else {
			dtoRes.setErrorCode(rspStatus.getErrorCode());
			dtoRes.setResponseData(rspStatus.getErrorDesc());
		}
		return dtoRes;
	}

}
