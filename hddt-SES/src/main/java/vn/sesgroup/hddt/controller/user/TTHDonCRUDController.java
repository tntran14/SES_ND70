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
	 "/tthdon-edit"
})
@Scope(value = WebApplicationContext.SCOPE_REQUEST)
public class TTHDonCRUDController extends AbstractController{
	private static final Logger log = LogManager.getLogger(TTHDonCRUDController.class);
	@Autowired RestAPIUtility restAPI; 
		

	private String tthdon;
	private String _id;
	private String lhd;

	
	private void LoadParameter(CurrentUserProfile cup, Locale locale, HttpServletRequest req, String action) {
		try {
			BaseDTO baseDTO = new BaseDTO(req);
			Msg msg = baseDTO.createMsg(cup, Constants.MSG_ACTION_CODE.LOAD_PARAMS);

			
			
			String[] status = { "CREATED", "PENDING", "PROCESSING", "COMPLETE" , "ERROR_CQT", "DELETED", "REPLACED", "ADJUSTED"};
			
			
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
	
	
	@RequestMapping(value = "/init_edit", method = {RequestMethod.POST, RequestMethod.GET})
	public String init_edit(Locale locale, Principal principal, HttpServletRequest req
			, @RequestParam(value = "hdon", required = false, defaultValue = "") String hdon
			) throws Exception{
		CurrentUserProfile cup = getCurrentlyAuthenticatedPrincipal();
		req.setAttribute("_TitleView_", Constants.PREFIX_TITLE + " - Trạng thái hóa đơn");
			
		LoadParameter(cup, locale, req, "");
		String[] hoadon = hdon.split(",");
		req.setAttribute("_id", hoadon[0]);
		req.setAttribute("TTHDon", hoadon[1]);
		req.setAttribute("LoaiHD", hoadon[2]);
		
		return "user/tthdon_edit";
	}
	
	public BaseDTO checkDataToSave(HttpServletRequest req, HttpSession session
			, String transaction, CurrentUserProfile cup) throws Exception{
		BaseDTO dto = new BaseDTO();
		dto.setErrorCode(0);
	
		_id = commons.getParameterFromRequest(req, "_id").trim().replaceAll("\\s+", "");
		tthdon = commons.getParameterFromRequest(req, "tthdon").trim().replaceAll("\\s+", "");
		lhd = commons.getParameterFromRequest(req, "lhd").trim().replaceAll("\\s+", "");
		
		if("".equals(tthdon)) {
			dto.setErrorCode(1);
			dto.getErrorMessages().add("Vui lòng chọn trạng thái hóa đơn.");
		}
		
		return dto;
	}
	
	
	@RequestMapping(value = "/check-data",  produces = MediaType.APPLICATION_JSON_VALUE, method = RequestMethod.POST)
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
	
		
		CurrentUserProfile cup = getCurrentlyAuthenticatedPrincipal();
		dto = checkDataToSave(req, session, transaction, cup);
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
		hData.put("TTHDon", tthdon);
		hData.put("LoaiHD", lhd);
		
		msg.setObjData(hData);
		JSONRoot root = new JSONRoot(msg);
		MsgRsp rsp = restAPI.callAPINormal("/tthdon/crud", cup.getLoginRes().getToken(), HttpMethod.POST, root);
		MspResponseStatus rspStatus = rsp.getResponseStatus();
		if(rspStatus.getErrorCode() == 0) {
			dto.setErrorCode(0);
			dto.setResponseData("Thay đổi trạng thái thành công");					
				
		}else {
			dto.setErrorCode(rspStatus.getErrorCode());
			dto.setResponseData(rspStatus.getErrorDesc());
		}
		return dto;
	
	}
	
}
