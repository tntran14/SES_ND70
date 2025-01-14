package vn.sesgroup.hddt.controller.user;

import java.security.Principal;
import java.util.HashMap;
import java.util.Locale;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.context.WebApplicationContext;

import vn.sesgroup.hddt.controller.AbstractController;
import vn.sesgroup.hddt.dto.BaseDTO;
import vn.sesgroup.hddt.dto.CurrentUserProfile;
import vn.sesgroup.hddt.resources.RestAPIUtility;
import vn.sesgroup.hddt.utils.Constants;

@Controller
@RequestMapping("/qlxml")
@Scope(value = WebApplicationContext.SCOPE_REQUEST)
public class QLXMLController extends AbstractController{
	private static final Logger log = LogManager.getLogger(QLXMLController.class);
	@Autowired RestAPIUtility restAPI; 
		
	private String mst;
	private String khhd;
	private String shd;
	private String mtd;
	

	
	
	@RequestMapping(value = "/init", method = {RequestMethod.POST, RequestMethod.GET})
	public String init(Locale locale, Principal principal, HttpServletRequest req) throws Exception{
		req.setAttribute("_TitleView_", Constants.PREFIX_TITLE + " - Lấy XML");
		
//		CurrentUserProfile cup = getCurrentlyAuthenticatedPrincipal();
		
		return "user/qlxml";
	}
	
	
	public BaseDTO checkDataToSave(HttpServletRequest req, HttpSession session
			, String transaction, CurrentUserProfile cup) throws Exception{
		BaseDTO dto = new BaseDTO();
		dto.setErrorCode(0);
	
		mst = commons.getParameterFromRequest(req, "mst").replaceAll("\\s", "");
		khhd = commons.getParameterFromRequest(req, "khhd").trim().replaceAll("\\s+", "");
		shd = commons.getParameterFromRequest(req, "shd").trim().replaceAll("\\s+", "");
		mtd = commons.getParameterFromRequest(req, "mtd").trim().replaceAll("\\s+", "");
		if("".equals(mst)) {
			dto.setErrorCode(1);
			dto.getErrorMessages().add("Vui lòng nhập mã số thuế.");
		}
		if("".equals(khhd)) {
			dto.setErrorCode(1);
			dto.getErrorMessages().add("Vui lòng nhập ký hiệu hóa đơn.");
		}
		if("".equals(shd)) {
			dto.setErrorCode(1);
			dto.getErrorMessages().add("Vui lòng nhập số hóa đơn.");
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
		String messageConfirm =  "Bạn có muốn lấy xml này không";
		switch (transaction) {
		case "qlxml":
			messageConfirm = "Bạn có muốn lấy xml này không?";
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
		hInfo.put("mst", mst);
		hInfo.put("khhd", khhd);
		hInfo.put("shd", shd);
		hInfo.put("mtd", mtd);
		dto.setResponseData(hInfo);
		dto.setErrorCode(0);
		return dto;
	
	}

}
