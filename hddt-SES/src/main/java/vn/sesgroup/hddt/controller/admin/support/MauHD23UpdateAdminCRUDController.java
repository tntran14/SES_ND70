package vn.sesgroup.hddt.controller.admin.support;

import java.security.Principal;
import java.time.LocalDate;
import java.time.temporal.ChronoField;
import java.util.HashMap;
import java.util.Locale;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.context.WebApplicationContext;

import com.api.message.JSONRoot;
import com.api.message.Msg;
import com.api.message.MsgRsp;
import com.api.message.MspResponseStatus;

import vn.sesgroup.hddt.controller.AbstractController;
import vn.sesgroup.hddt.dto.BaseDTO;
import vn.sesgroup.hddt.dto.CurrentUserProfile;
import vn.sesgroup.hddt.resources.RestAPIUtility;
import vn.sesgroup.hddt.utils.Constants;

@Controller
@RequestMapping({
	"/mauhd_update_admin-edit",
	"/mauhd_update_admin-check"
})
@Scope(value = WebApplicationContext.SCOPE_REQUEST)
public class MauHD23UpdateAdminCRUDController extends AbstractController{
	@Autowired RestAPIUtility restAPI;
	@Autowired RestTemplate restTemplate;

	private String _id;
//	private String NamCapNhat;
	
	@RequestMapping(value = "/checkdb", method = { RequestMethod.POST, RequestMethod.GET })
	public String checkdb(Locale locale, Principal principal, HttpServletRequest req) throws Exception {
		
		_id = commons.getParameterFromRequest(req, "_id").replaceAll("\\s", "");
		
		req.setAttribute("ListID", _id);
		return "/support-admin/mauhd_update_admin-db";		
	}
	
	
	public BaseDTO checkDataToAcceptDB(HttpServletRequest req, HttpSession session, String transaction
			, CurrentUserProfile cup) throws Exception{
		BaseDTO dto = new BaseDTO();
		dto.setErrorCode(0);
		
		_id = commons.getParameterFromRequest(req, "_id").replaceAll("\\s", "");
//		NamCapNhat = commons.getParameterFromRequest(req, "nam-cap-nhat").replaceAll("\\s", "");
		
		if("".equals(_id)) {
			dto.setErrorCode(1);
			dto.getErrorMessages().add("Không tìm thấy thông tin mẫu số hóa đơn.");
			return dto;
		}
		
//			if("".equals(NamCapNhat)) {
//				dto.setErrorCode(1);
//				dto.getErrorMessages().add("Vui lòng nhập năm cần cập nhật.");
//				return dto;
//			}
//			
//			int currentYear = LocalDate.now().get(ChronoField.YEAR);
//			
//			if(!commons.checkStringIsInt(NamCapNhat) || commons.ToNumber(NamCapNhat) > currentYear) {
//				dto.setErrorCode(1);
//				dto.getErrorMessages().add("Năm cần chuyển đổi phải là số nguyên và không được lớn hơn năm hiện tại.");
//				return dto;
//			}
				
		return dto;
	}
	
	@PostMapping(value = "/updatedb",  produces = MediaType.APPLICATION_JSON_VALUE)
	@ResponseBody
	public BaseDTO execImportData(HttpServletRequest req, HttpSession session
			, @RequestParam(value = "transaction", required = false, defaultValue = "") String transaction
			, @RequestParam(value = "tokenTransaction", required = false, defaultValue = "") String tokenTransaction) throws Exception{
		BaseDTO dtoRes = new BaseDTO();
		
		CurrentUserProfile cup = getCurrentlyAuthenticatedPrincipal();
		dtoRes = checkDataToAcceptDB(req, session, transaction, cup);
		if(0 != dtoRes.getErrorCode()) {
			dtoRes.setErrorCode(999);
			dtoRes.setResponseData(Constants.MAP_ERROR.get(999));
			return dtoRes;
		}
		
		
		dtoRes = new BaseDTO(req);
		Msg msg = dtoRes.createMsg(cup, Constants.MSG_ACTION_CODE.CREATED);
		HashMap<String, Object> hData = new HashMap<>();
		hData.put("ListID", _id);
//		hData.put("NamCapNhat", NamCapNhat);
		msg.setObjData(hData);
		JSONRoot root = new JSONRoot(msg);
		MsgRsp rsp = restAPI.callAPINormal("/mauhd_update_admin/updatedb", cup.getLoginRes().getToken(), HttpMethod.POST, root);
		
		MspResponseStatus rspStatus = rsp.getResponseStatus();
		if(rspStatus.getErrorCode() == 0) {
			dtoRes.setErrorCode(0);
			dtoRes.setResponseData("Cập nhật thông tin thành công.");
		}else {
			dtoRes.setErrorCode(rspStatus.getErrorCode());
			dtoRes.setResponseData(rspStatus.getErrorDesc());
		}
		return dtoRes;
	}
	
	
}
	