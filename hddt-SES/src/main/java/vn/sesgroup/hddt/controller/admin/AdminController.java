package vn.sesgroup.hddt.controller.admin;

import java.util.HashMap;
import java.util.Locale;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.api.message.JSONRoot;
import com.api.message.Msg;
import com.api.message.MsgRsp;
import com.api.message.MspResponseStatus;
import com.fasterxml.jackson.databind.JsonNode;

import cn.apiclub.captcha.Captcha;
import vn.sesgroup.hddt.controller.AbstractController;
import vn.sesgroup.hddt.controller.einvoice.EInvoiceCRUDController;
import vn.sesgroup.hddt.dto.BaseDTO;
import vn.sesgroup.hddt.dto.CurrentUserProfile;
import vn.sesgroup.hddt.dto.LoginRes;
import vn.sesgroup.hddt.model.Users;
import vn.sesgroup.hddt.resources.RestAPIUtility;
import vn.sesgroup.hddt.utils.CaptchaUtil;
import vn.sesgroup.hddt.utils.Constants;
import vn.sesgroup.hddt.utils.Json;

@Controller
@RequestMapping(value = { "/changepassAdmin" })
public class AdminController extends AbstractController {
	@Autowired
	RestAPIUtility restAPI;
	private static final Logger log = LogManager.getLogger(EInvoiceCRUDController.class);
	private String _id;
	private String userName;
	private String serverinput;
	private String pw;
	private String pw1;
	private String pw2;
	private String errorDesc;
	private int kt = 0;
	@GetMapping("/init")
	public String init(Users users, LoginRes us, HttpServletRequest req, Locale locale, String action
			, @RequestAttribute(name = "transaction", value = "", required = false) String transaction
			, @RequestAttribute(name = "method", value = "", required = false) String method
			,HttpSession session) throws Exception {
		req.setAttribute("_TitleView_", Constants.PREFIX_TITLE + " - Thay đổi mật khẩu");
		CurrentUserProfile cup = getCurrentlyAuthenticatedPrincipal();
		us = cup.getLoginRes();
		req.setAttribute("userName", cup.getUsername());
		us.setPassword(cup.getPassword());
		_id = us.getUserId();
		kt=0;

		String header = "Thay đổi password";
		String action1 = "EDIT";
		boolean isEdit = false;
		req.setAttribute("_header_", header);
		req.setAttribute("_action_", action1);
		req.setAttribute("_isedit_", isEdit);
		req.setAttribute("_id", _id);
		return "/admin/changepass";
	}

	@RequestMapping(value = "/change", method = RequestMethod.POST
			, produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_FORM_URLENCODED_VALUE})
	@ResponseBody
	public BaseDTO changepass(Model model, Users users, LoginRes us, Authentication authentication, HttpServletRequest req,HttpServletResponse response, HttpSession session, RedirectAttributes redirectAttributes) throws Exception {
		CurrentUserProfile cup = getCurrentlyAuthenticatedPrincipal();
	
		BaseDTO dtoRes = new BaseDTO();
		String captcha = commons.getParameterFromRequest(req, "j_captcha");
		String captchaSession = null == session.getAttribute(Constants.SESSION_CAPTCHA)? "": session.getAttribute(Constants.SESSION_CAPTCHA).toString();
		session.removeAttribute(Constants.SESSION_CAPTCHA);
		
		us = cup.getLoginRes();
		req.setAttribute("userName", cup.getUsername());
		us.setPassword(cup.getPassword());
		if (kt > 3) {
			Authentication auth = SecurityContextHolder.getContext().getAuthentication();

			SecurityContextHolder.getContext().setAuthentication(null);
			new SecurityContextLogoutHandler().logout(req, response, auth);

			redirectAttributes.addFlashAttribute("logout", "true");
			
			dtoRes = new BaseDTO(909, Constants.MAP_ERROR.get(1));
			return dtoRes;
		} else {
			if(captchaSession.equals(captcha)) {
				userName = cup.getUsername();
				pw = commons.getParameterFromRequest(req, "pw");
				pw1 = commons.getParameterFromRequest(req, "pw1");
				pw2 = commons.getParameterFromRequest(req, "pw2");
				String pass = commons.generateSHA(cup.getUsername() + cup.getPassword(), false);
				String pwuserinput = commons.encryptThisString(userName + pw);
				String mkc2 = "";
				if(pwuserinput.length() < 128) {
					 mkc2 = 0 + pwuserinput;
				}
				else {
					mkc2 = 	pwuserinput;
				}
				boolean checkPassword = pass.equals(mkc2);
				if (!checkPassword) {
					kt += 1;
					dtoRes = new BaseDTO(999, "Mật khẩu cũ không đúng!");
		
				} else {
					if (!pw1.equals(pw2)) {
						kt += 1;
						dtoRes = new BaseDTO(999, "Xác nhận mật khẩu không trùng khớp!");
				
					} else {
						String serverinput = commons.encryptThisString(userName + pw1);	
						
						String actionCode = Constants.MSG_ACTION_CODE.MODIFY;
						dtoRes = new BaseDTO(req);
						Msg msg = dtoRes.createMsg(cup, actionCode);
						HashMap<String, Object> hData = new HashMap<>();
						hData.put("_id", _id);
						hData.put("UserName", userName);
						hData.put("Password", serverinput);
						msg.setObjData(hData);
						JSONRoot root = new JSONRoot(msg);
						MsgRsp rsp = restAPI.callAPINormal("/main/admin/crud", cup.getLoginRes().getToken(), HttpMethod.POST, root);
						MspResponseStatus rspStatus = rsp.getResponseStatus();
						if (rspStatus.getErrorCode() == 0) {
							dtoRes.setErrorCode(0);
							Authentication auth = SecurityContextHolder.getContext().getAuthentication();

							SecurityContextHolder.getContext().setAuthentication(null);
							new SecurityContextLogoutHandler().logout(req, response, auth);

							redirectAttributes.addFlashAttribute("logout", "true");
							dtoRes = new BaseDTO(0, Constants.MAP_ERROR.get(0));
						
						} else {
							model.addAttribute("message", "Error!");
							dtoRes = new BaseDTO(999, Constants.MAP_ERROR.get(1));
							
						}
					
					}
				}

				return dtoRes;
			} else {

				kt += 1;
				dtoRes = new BaseDTO(999, "Lỗi Captcha không đúng!");
				return dtoRes;
				
			}

		}
	}
	
	@RequestMapping(value = { "/logout" })
	public String logoutPage(HttpServletRequest request, HttpServletResponse response, Model model,
			RedirectAttributes redirectAttributes) {
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();

		SecurityContextHolder.getContext().setAuthentication(null);
		new SecurityContextLogoutHandler().logout(request, response, auth);

		redirectAttributes.addFlashAttribute("logout", "true");
		return "redirect:/login";

	}


}