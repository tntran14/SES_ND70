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
import org.springframework.web.bind.annotation.RequestAttribute;
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
@RequestMapping({"/convert-cttncn-edit"})
@Scope(value = WebApplicationContext.SCOPE_REQUEST)
public class ConvertCTTNCNAdminCRUDController extends AbstractController{
	@Autowired RestAPIUtility restAPI;
	@Autowired RestTemplate restTemplate;
	
	private String NamCanChuyenDoi;
	private String NamChuyenDoi;
	
	@RequestMapping(value = "/init", method = { RequestMethod.POST, RequestMethod.GET })
	public String init(Locale locale, Principal principal, HttpServletRequest req) throws Exception {
		return "/support-admin/convert_cttncn_edit_admin";
	}
	
	public BaseDTO checkDataToAccept(HttpServletRequest req, HttpSession session, String transaction,
			CurrentUserProfile cup) throws Exception {
		BaseDTO dto = new BaseDTO();
		dto.setErrorCode(0);

		NamCanChuyenDoi = commons.getParameterFromRequest(req, "nam-can-chuyen-doi").replaceAll("\\s", "");
		NamChuyenDoi = commons.getParameterFromRequest(req, "nam-chuyen-doi").replaceAll("\\s", "");

		if ("".equals(NamCanChuyenDoi)) {
			dto.setErrorCode(1);
			dto.getErrorMessages().add("Vui lòng nhập năm cần chuyển đổi.");
			return dto;
		}

		int currentYear = LocalDate.now().get(ChronoField.YEAR);

		if (!commons.checkStringIsInt(NamCanChuyenDoi) || commons.ToNumber(NamCanChuyenDoi) > currentYear) {
			dto.setErrorCode(1);
			dto.getErrorMessages().add("Năm cần chuyển đổi phải là số nguyên và không được lớn hơn năm hiện tại.");
			return dto;
		}

		if ("".equals(NamChuyenDoi)) {
			dto.setErrorCode(1);
			dto.getErrorMessages().add("Vui lòng nhập năm chuyển đổi.");
			return dto;
		}

		if (!commons.checkStringIsInt(NamChuyenDoi)
				|| commons.ToNumber(NamChuyenDoi) <= commons.ToNumber(NamCanChuyenDoi)) {
			dto.setErrorCode(1);
			dto.getErrorMessages()
					.add("Năm cần chuyển đổi phải là số nguyên và không được bé hơn hoặc bằng năm cần chuyển đổi.");
			return dto;
		}

		double checkNam = commons.ToNumber(NamChuyenDoi) - 1;

		if (checkNam != commons.ToNumber(NamCanChuyenDoi)) {
			dto.setErrorCode(1);
			dto.getErrorMessages().add("Vui lòng nhập năm liền kề.");
			return dto;
		}

		return dto;
	}
	
	@RequestMapping(value = "/check",  produces = MediaType.APPLICATION_JSON_VALUE, method = RequestMethod.POST)
	@ResponseBody
	public BaseDTO check(Locale locale, HttpServletRequest req, HttpSession session
			, @RequestAttribute(name = "transaction", required = false, value = "") String transaction) throws Exception {
		String token = "";
		if (null != session.getAttribute(Constants.SESSION_TYPE.SESSION_TOKEN_EXECUTE)) {
			token = session.getAttribute(Constants.SESSION_TYPE.SESSION_TOKEN_EXECUTE).toString();
			session.removeAttribute(token);
		}
		session.removeAttribute(Constants.SESSION_TYPE.SESSION_TOKEN_EXECUTE);

		CurrentUserProfile cup = getCurrentlyAuthenticatedPrincipal();
		String messageConfirm = "Bạn có muốn cập nhật mẫu số chứng từ TNCN không?";

		BaseDTO dto = checkDataToAccept(req, session, transaction, cup);
		if (0 != dto.getErrorCode()) {
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
	
	@RequestMapping(value = "/update",  produces = MediaType.APPLICATION_JSON_VALUE, method = RequestMethod.POST)
	@ResponseBody
	public BaseDTO update(HttpServletRequest req, HttpSession session
			, @RequestParam(value = "transaction", required = false, defaultValue = "") String transaction
			, @RequestParam(value = "tokenTransaction", required = false, defaultValue = "") String tokenTransaction) throws Exception{
		CurrentUserProfile cup = getCurrentlyAuthenticatedPrincipal();
		BaseDTO dtoRes = checkDataToAccept(req, session, transaction, cup);
		if (0 != dtoRes.getErrorCode()) {
			dtoRes.setErrorCode(999);
			dtoRes.setResponseData(Constants.MAP_ERROR.get(999));
			return dtoRes;
		}

		/* CHECK TOKEN */
		String token = session.getAttribute(Constants.SESSION_TYPE.SESSION_TOKEN_EXECUTE) == null ? ""
				: session.getAttribute(Constants.SESSION_TYPE.SESSION_TOKEN_EXECUTE).toString();
		session.removeAttribute(Constants.SESSION_TYPE.SESSION_TOKEN_EXECUTE);
		if ("".equals(token) || !tokenTransaction.equals(token)) {
			dtoRes.setErrorCode(1);
			dtoRes.setResponseData("Token giao dịch không hợp lệ.");
			return dtoRes;
		}

		String actionCode = Constants.MSG_ACTION_CODE.MODIFY;

		dtoRes = new BaseDTO(req);
		Msg msg = dtoRes.createMsg(cup, actionCode);
		HashMap<String, Object> hData = new HashMap<>();
		hData.put("NamCanChuyenDoi", NamCanChuyenDoi);
		hData.put("NamChuyenDoi", NamChuyenDoi);

		msg.setObjData(hData);
		JSONRoot root = new JSONRoot(msg);
		MsgRsp rsp = restAPI.callAPINormal("/convert-cttncn-admin/convertByYear", cup.getLoginRes().getToken(),
				HttpMethod.POST, root);
		MspResponseStatus rspStatus = rsp.getResponseStatus();
		if (rspStatus.getErrorCode() == 0) {
			dtoRes.setErrorCode(0);
			dtoRes.setResponseData("Cập nhật thông tin thành công.");
		} else {
			dtoRes.setErrorCode(rspStatus.getErrorCode());
			dtoRes.setResponseData(rspStatus.getErrorDesc());
		}
		return dtoRes;
	}
}
	