package vn.sesgroup.hddt.controller.xulynghiepvu;

import java.security.Principal;
import java.util.Locale;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

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
import com.api.message.MsgRsp;
import com.api.message.MspResponseStatus;

import vn.sesgroup.hddt.controller.AbstractController;
import vn.sesgroup.hddt.dto.BaseDTO;
import vn.sesgroup.hddt.dto.CurrentUserProfile;
import vn.sesgroup.hddt.resources.RestAPIUtility;
import vn.sesgroup.hddt.utils.Constants;

@Controller
@RequestMapping("/tracuuthongdiep")
@Scope(value = WebApplicationContext.SCOPE_REQUEST)
public class TraCuuThongDiepController extends AbstractController{
	@Autowired RestAPIUtility restAPI; 
	
	@RequestMapping(value = "/init", method = { RequestMethod.POST, RequestMethod.GET })
	public String init(Locale locale, Principal principal, HttpServletRequest req) throws Exception {
		req.setAttribute("_TitleView_", Constants.PREFIX_TITLE + " - Tra cứu thông điệp");
		return "ad_xulynghiepvu/tracuuthongdiep";
	}

	@RequestMapping(value = "/execute", produces = MediaType.APPLICATION_JSON_VALUE, method = RequestMethod.POST)
	@ResponseBody
	public BaseDTO execSearch(Locale locale, HttpServletRequest req, HttpSession session) throws Exception {
		BaseDTO dtoRes = new BaseDTO(req);
		String mtdiep = commons.getParameterFromRequest(req, "mtdiep").replaceAll("\\s", "");

		if ("".equals(mtdiep.trim())) {
			dtoRes.setErrorCode(1);
			dtoRes.getErrorMessages().add("Vui lòng nhập mã thông điệp.");
			return dtoRes;
		}

		CurrentUserProfile cup = getCurrentlyAuthenticatedPrincipal();
		Msg msg = dtoRes.createMsg(cup, Constants.MSG_ACTION_CODE.SEARCH);
		JSONRoot root = new JSONRoot(msg);
		MsgRsp rsp = restAPI.callAPINormal("/tracuuthongdiep/" + mtdiep, cup.getLoginRes().getToken(), HttpMethod.GET,
				root);
		MspResponseStatus rspStatus = rsp.getResponseStatus();
		if (rspStatus.getErrorCode() == 0) {
			dtoRes.setErrorCode(0);
			dtoRes.setResponseData(rsp.getObjData());
		} else {
			dtoRes.setErrorCode(rspStatus.getErrorCode());
			dtoRes.setResponseData(rspStatus.getErrorDesc());
		}
		
		return dtoRes;
	}
}
