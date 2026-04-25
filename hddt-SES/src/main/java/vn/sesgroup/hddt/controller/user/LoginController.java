package vn.sesgroup.hddt.controller.user;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bson.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.api.message.JSONRoot;
import com.api.message.Msg;
import com.api.message.MsgRsp;
import com.api.message.MspResponseStatus;
import com.fasterxml.jackson.databind.JsonNode;

import vn.sesgroup.hddt.controller.AbstractController;
import vn.sesgroup.hddt.dto.BaseDTO;
import vn.sesgroup.hddt.dto.CurrentUserProfile;
import vn.sesgroup.hddt.dto.CurrentUserRole;
import vn.sesgroup.hddt.dto.LoginRes;
import vn.sesgroup.hddt.dto.req.UserLoginReq;
import vn.sesgroup.hddt.resources.RestAPIUtility;
import vn.sesgroup.hddt.utils.Constants;
import vn.sesgroup.hddt.utils.Json;

@Controller
@Scope(value = WebApplicationContext.SCOPE_REQUEST)
@RequestMapping("/")
public class LoginController extends AbstractController{
	@Autowired private RestAPIUtility restAPIUtility;
	private static final Logger log = LogManager.getLogger(LoginController.class);
	@Autowired RestAPIUtility restAPI;	
	
	
	@RequestMapping(value = { "/", "/login" }, method = { RequestMethod.GET, RequestMethod.POST })
	public String login(Model model, HttpServletRequest request, HttpSession session,
			@ModelAttribute("logout") String isLogout, @ModelAttribute("message") String message) throws Exception {
		HashMap<String, Object> hTmp = null;
		Document r10 = null;
		String messageLogin = "";
		List<HashMap<String, Object>> rowsL = null;
		if ("true".equals(isLogout)) {
			messageLogin = "Bạn vừa đăng xuất. Vui lòng đăng nhập lại để thực hiện giao dịch.";
		} else if (null != message && !"".equals(message)) {
			messageLogin = message;
		}
		if (!"".equals(messageLogin)) {
			request.setAttribute("messageLogin", messageLogin);
		}
		request.setAttribute("_header", Constants.PREFIX_TITLE + " - Đăng nhập");

		BaseDTO dtoRes = new BaseDTO();
		Msg msg = dtoRes.createMsgPass();
		HashMap<String, String> hInput = new HashMap<>();
		msg.setObjData(hInput);
		JSONRoot root = new JSONRoot(msg);
		MsgRsp rsp = restAPI.callAPIPass("/forgotpass/dl", HttpMethod.POST, root);
		MspResponseStatus rspStatus = rsp.getResponseStatus();
		JsonNode rows = null;
		if (rspStatus.getErrorCode() == 0) {
			JsonNode jsonData = Json.serializer().nodeFromObject(rsp.getObjData());
			if (!jsonData.at("/rows").isMissingNode()) {
				rows = jsonData.at("/rows");
				for (JsonNode row : rows) {
					request.setAttribute("LOGO", commons.getTextJsonNode(row.at("/LOGO")));
					request.setAttribute("PHONE", commons.getTextJsonNode(row.at("/PHONE")));
					request.setAttribute("EMAIL", commons.getTextJsonNode(row.at("/EMAIL")));
				}
			}
		}

		dtoRes = new BaseDTO(request);
		msg = dtoRes.createMsgPass();
		hInput = new HashMap<>();
		msg.setObjData(hInput);
		root = new JSONRoot(msg);
		rsp = restAPI.callAPIPass("/forgotpass/left", HttpMethod.POST, root);
		rspStatus = rsp.getResponseStatus();
		rows = null;

		if (rspStatus.getErrorCode() == 0) {
			JsonNode jsonData = Json.serializer().nodeFromObject(rsp.getObjData());
			if (!jsonData.at("/rows").isMissingNode()) {
				rows = jsonData.at("/rows");
				rowsL = new ArrayList<HashMap<String, Object>>();
				for (JsonNode row : rows) {
					hTmp = new HashMap<String, Object>();
					hTmp.put("Chude", commons.getTextJsonNode(row.at("/Chude")));
					hTmp.put("Tieude", commons.getTextJsonNode(row.at("/Tieude")));
					hTmp.put("Link", commons.getTextJsonNode(row.at("/Link")));
					hTmp.put("Noidung", commons.getTextJsonNode(row.at("/Noidung")));
					hTmp.put("_id", commons.getTextJsonNode(row.at("/_id")));
					rowsL.add(hTmp);
				}
				request.setAttribute("TinWebL", rowsL);
			}
		}

		dtoRes = new BaseDTO(request);
		msg = dtoRes.createMsgPass();
		hInput = new HashMap<>();
		msg.setObjData(hInput);
		root = new JSONRoot(msg);
		rsp = restAPI.callAPIPass("/forgotpass/right", HttpMethod.POST, root);
		rspStatus = rsp.getResponseStatus();
		rows = null;
		if (rspStatus.getErrorCode() == 0) {
			JsonNode jsonData = Json.serializer().nodeFromObject(rsp.getObjData());
			if (!jsonData.at("/rows").isMissingNode()) {
				rows = jsonData.at("/rows");
				rowsL = new ArrayList<HashMap<String, Object>>();
				for (JsonNode row : rows) {
					hTmp = new HashMap<String, Object>();
					hTmp.put("Chude", commons.getTextJsonNode(row.at("/Chude")));
					hTmp.put("Tieude", commons.getTextJsonNode(row.at("/Tieude")));
					hTmp.put("Noidung", commons.getTextJsonNode(row.at("/Noidung")));
					request.setAttribute("Content", commons.getTextJsonNode(row.at("/Noidung")));
					hTmp.put("_id", commons.getTextJsonNode(row.at("/_id")));
					rowsL.add(hTmp);
				}
				request.setAttribute("TinWebR", rowsL);
			}
		}
		return "user/login";
	}
	
	@RequestMapping("favicon.ico")
	public String favicon() {
        return "forward:/static/images/ses.jpg";
    }
	
	@RequestMapping(value = "/authenticate", method = RequestMethod.POST
			, produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_FORM_URLENCODED_VALUE})
	@ResponseBody
	public BaseDTO executeLogin(HttpServletRequest req, HttpServletResponse resp, HttpSession session
			, RedirectAttributes redirectAttributes) throws Exception{
		BaseDTO dtoRes = new BaseDTO();
		
		String userName = commons.getParameterFromRequest(req, "j_username").toUpperCase().replaceAll("\\s", "");
		String password = commons.getParameterFromRequest(req, "j_password");
		String captcha = commons.getParameterFromRequest(req, "j_captcha");
		
		String captchaSession = null == session.getAttribute(Constants.SESSION_CAPTCHA)? "": session.getAttribute(Constants.SESSION_CAPTCHA).toString();
		session.removeAttribute(Constants.SESSION_CAPTCHA);
		if("".equals(userName) || "".equals(password)) {
			dtoRes = new BaseDTO(1, Constants.MAP_ERROR.get(1));
			return dtoRes;
		}
		
		if(!captchaSession.equals(captcha)) {
			dtoRes = new BaseDTO(1, Constants.MAP_ERROR.get(2));
			return dtoRes;
		}
		
		Authentication auth = null;
		CurrentUserProfile cup = new CurrentUserProfile();
		
		List<CurrentUserRole> grantedAuthoritiesList = new ArrayList<CurrentUserRole>();
        CurrentUserRole role = new CurrentUserRole();
        
        UserLoginReq userLoginReq = new UserLoginReq(
        		userName, 
        		password, 
        		null != req.getHeader("X-Forwarded-For")? req.getHeader("X-Forwarded-For"): req.getRemoteAddr(),
				req.getHeader("User-Agent")
    		);
		
        LoginRes res = restAPIUtility.callAPILogin("/auth", HttpMethod.POST, userLoginReq);
        if(0 == res.getStatusCode()) {
        	StringBuilder sbRights = new StringBuilder("|");
        	StringBuilder rightForRootAgent = new StringBuilder("tkhai|");
       		if(res.isKh() == true) {
       			sbRights.append("einvoices|einvoice-pdfAll|einvoice-xml|");
            	sbRights.append("einvoices1|einvoice1-pdfAll|einvoice1-xml|");
            	sbRights.append("agent|");
            	sbRights.append("export|export-xml|export-print-pdfAll|export-pdfAll|");       	
            	
       		}
        	else if(res.getFullRights() != null) {
				for(String s: res.getFullRights()) {
					if(rightForRootAgent.toString().indexOf("|" + s + "|") == -1) {
						sbRights.append(s).append("|");	
					}					
				}
			} else {
        	sbRights.append("einvoices|einvoice-sendAll-cqt|einvoice-history|einvoice-change|einvoice-signAll|einvoice-send-cqtAll|einvoice-cre|einvoice-edit|einvoice-del|einvoice-deleteAll|einvoice-import|einvoice-detail|einvoice-signAll|einvoice-pdfAll|einvoice-xml|einvoice-sign|einvoice-copy|einvoice-send-cqt|einvoice-send-mail|einvoice-send-emailauto|einvoice-import-auto|einvoice-import-misa|viewmail|einvoice_check_mst|einvoice_save_nmua|einvoice-pdfCD|einvoice-cre-dc-tt|einvoice-refreshAll|einvoice-sendAll-emailauto|einvoice-send-cqt|einvoice-sendMailAll|");
        	sbRights.append("einvoices1|einvoice1-history|einvoice1-change|einvoice1-send-emailauto|einvoice1-cre|einvoice1-edit|einvoice1-copy|einvoice1-del|einvoice1-detail|einvoice1-sign|einvoice1-send-cqt|einvoice1-send-mail|einvoice1-import-auto|einvoice1_check_mst|einvoice1_save_nmua|einvoice1-pdfAll|einvoice1-xml|einvoice1-cre-dc-tt|");
        	sbRights.append("agent|agent-history|agent-change|agent-send-emailauto|agent-cre|agent-copy|agent-edit|agent-del|agent-detail|agent-sign|agent-send-cqt|agent-send-mail|agent-import|agent_check_mst|agent_save_nmua|agent_online_mst|agent-cre-dc-tt|agent-deleteAll|agent-import|");
        	sbRights.append("export|export-history|export-change|export-send-emailauto|export-cre|export-edit|export-del|export-detail|export-sign|export-copy|export-send-cqt|export-send-mail|export-import|export_check_mst|export_save_nmua|export-cre-dc-tt|export-xml|export-print-pdfAll|export-del|export-deleteAll|export-pdfAll|export-pdfCD|");       	
        	sbRights.append("issu|issu-cre|issu-edit|issu-del|issu-detail|issu-contract|issu-send-cqt|issu-send-mail|issu-reset-pass|issu-update-kh|");
        	sbRights.append("issu-contract|issu-contract-cre|issu-contract-db|issu-contract-del|issu-contract-detail|issu-contract-edit|issu-contract-approve|issu-contract-active|issu-contract-deactive|");
        	sbRights.append("tkhai|tkhai-cre|tkhai-edit|tkhai-del|tkhai-detail|tkhai-sign|tkhai-copy|tkhaiV1|tkhai-creV1|tkhai-editV1|tkhai-delV1|tkhai-detailV1|tkhai-signV1|tkhai-copyV1|");
        	sbRights.append("mstncn|mstncn-cre|mstncn-edit|mstncn-del|mstncn-detail|mstncn-sign|");
        	sbRights.append("tkctu|tkctu-cre|tkctu-edit|tkctu-del|tkctu-detail|tkctu-sign|tkctu-copy|");
        	sbRights.append("cttncn|cttncn-cre|cttncn-edit|cttncn-del|cttncn-detail|cttncn-sign|cttncn-import|cttncn-signAll|cttncn-xoabo|cttncn-xml|cttncn-send-mail|cttncn-pdfAll|");
        	sbRights.append("cttncn-creV1|cttncn-editV1|cttncn-delV1|cttncn-detailV1|cttncn-signV1|cttncn-importV1|cttncn-signAllV1|cttncn-xoaboV1|cttncn-xmlV1|cttncn-send-mailV1|cttncn-pdfAllV1|cttncn-historyV1|cttncn-cre-dc-tt|");
        	sbRights.append("hdsduser|hdsduser-detail|");
        	sbRights.append("qlnvtncn|qlnvtncn-cre|qlnvtncn-edit|qlnvtncn-del|qlnvtncn-detail|");
        	sbRights.append("ql-link|ql-link-cre|ql-link-edit|ql-link-del|ql-link-detail|ql-link-active|ql-link-deactive|");
        	sbRights.append("ql-footerweb|ql-footerweb-cre|ql-footerweb-edit|ql-footerweb-del|ql-footerweb-detail|ql-footerweb-active|ql-footerweb-deactive|");
        	sbRights.append("ql-notification|ql-notification-cre|ql-notification-edit|ql-notification-del|ql-notification-detail|ql-notification-active|ql-notification-deactive|");
        	sbRights.append("ql-tinh|ql-tinh-cre|ql-tinh-edit|ql-tinh-del|ql-tinh-detail|");
        	sbRights.append("ql-tinweb|ql-tinweb-cre|ql-tinweb-edit|ql-tinweb-del|ql-tinweb-detail|ql-tinweb-active|ql-tinweb-deactive|");
        	sbRights.append("ql-ttweb|ql-ttweb-cre|ql-ttweb-edit|ql-ttweb-del|ql-ttweb-detail|");
        	sbRights.append("ql-cqt|ql-cqt-cre|ql-cqt-edit|ql-cqt-del|ql-cqt-detail|");
        	sbRights.append("ql-mstk|ql-mstk-cre|ql-mstk-edit|ql-mstk-del|ql-mstk-detail|");
        	sbRights.append("ql-httk|ql-httk-cre|ql-httk-edit|ql-httk-del|ql-httk-detail|");
        	sbRights.append("ql-lhd|ql-lhd-cre|ql-lhd-edit|ql-lhd-del|ql-lhd-detail|");
        	sbRights.append("tktncn|tktncn-excel|tktncn-pdf|");
        	sbRights.append("mauhd|mauhd-cre|mauhd-edit|mauhd-del|mauhd-detail|mauhd-active|mauhd-deactive|");
        	sbRights.append("hdsd|hdsd-cre|hdsd-edit|hdsd-del|hdsd-detail|hdsd-delete|hdsd|hdsd-active|hdsd-deactive|");
        	sbRights.append("dmproduct|dmproduct-import|dmproduct-exp|dmproduct-cre|dmproduct-edit|dmproduct-del|dmproduct-detail|");
        	sbRights.append("dmcustomer|dmcustomer-import|dmcustomer-cre|dmcustomer-edit|dmcustomer-del|dmcustomer-detail|dmcustomer-export|");
        	sbRights.append("tkdshdon|tkdshdon-export-excel-fast|tkdshdon-export-excel-detail|tkdshdon-export-excel-general|");
        	sbRights.append("change-mtdiep|");
        	sbRights.append("report_situation|report_situation-html|report_situation-pdf|report_situation-xml|");
        	sbRights.append("tkdshdonPXKDL|tkdshdonPXKDL-export-excel-fast|tkdshdonPXKDL-export-excel-detail|");
        	sbRights.append("tkdshdonPXK|tkdshdonPXK-export-excel-fast|tkdshdonPXK-export-excel-detail|");
            sbRights.append("tkdshdonBH|tkdshdonBH-export-excel-fast|tkdshdonBH-export-excel-detail|tkdshdonBH-export-excel-general|");
            sbRights.append("user_check|");
        	sbRights.append("qly-mauhd|qly-mauhd-cre|qly-mauhd-edit|qly-mauhd-del|qly-mauhd-detail|qly-mauhd-active|qly-mauhd-deactive|");  
        	sbRights.append("xuly-nghiepvu|");
        	sbRights.append("cks|cks-cre|cks-edit|cks-del|cks-detail|");  
        	sbRights.append("qly-mauct|qly-mauct-cre|qly-mauct-edit|qly-mauct-del|qly-mauct-detail|qly-mauct-active|qly-mauct-deactive|");  
        	sbRights.append("quantitys|quantity-cre|quantity-edit|quantity-del|quantity-detail|quantity-active|quantity-deactive|");	
        	sbRights.append("config-email-server|config-email-mailjet|tra-cuu-mail|tra-cuu-mail-user|config-email-server-admin|config-param|");
        	sbRights.append("tbhdssot|tbhdssot-history|tbhdssot-cre|tbhdssot-detail|tbhdssot-edit|tbhdssot-del|tbhdssot-sign|");
        	sbRights.append("lbbdctt|lbbdctt-cre|lbbdctt-detail|lbbdctt-edit|lbbdctt-del|lbbdctt-sign|lbbdctt-sendMail|");
        	sbRights.append("tbhdssot-send-cqt|tbhdssot-del|tbhdssot-send-mail|");
        	sbRights.append("cttncnss|cttncnss-history|cttncnss-cre|cttncnss-detail|cttncnss-edit|cttncnss-del|cttncnss-sign|cttncnss-send-cqt|cttncnss-refresh|cttncnss-send-mail|");
        	sbRights.append("introduce|");
        	sbRights.append("support|");
        	sbRights.append("config-mailjet|");
        	sbRights.append("qlxml|");
        	sbRights.append("xml_thue|");
        	sbRights.append("dsnmua|dsnmua-export|");
        	sbRights.append("tthdon|tthdon-edit|");
        	sbRights.append("thhd|thhd-edit|thhd-active|");
        	sbRights.append("ql-phoihd|qly-phoihd-edit|qly-phoihd-detail|qly-phoihd-active|qly-phoihd-deactive|qly-phoihd-del|qly-phoihd-view|");
        	sbRights.append("ql-mauhdadmin|ql-mauhdadmin-cre|ql-mauhdadmin-edit|ql-mauhdadmin-detail|ql-mauhdadmin-active|ql-mauhdadmin-deactive|ql-mauhdadmin-del|ql-mauhdadmin-view|");
        	sbRights.append("phsl-mauhdadmin|phsl-mauhdadmin-cre|phsl-mauhdadmin-edit|phsl-mauhdadmin-detail|");	
        	sbRights.append("change_color|color-cre|color-detail|color-edit|color-active|color-del|");
        	sbRights.append("role_api|set_api_active|set_api_deactive|");
         	sbRights.append("session_key|session_key_check|session_key_cre|session_key_edit|session_key_detail|session_key_del|");
        	sbRights.append("roleRightManager|RolesRightManageCreate|RolesRightManageDetail|RolesRightManageEdit|RolesRightManageActive|RolesRightManageDeActive|");
         	sbRights.append("roleManager|RolesManageCreate|RolesManageDetail|RolesManageEdit|RolesManageActive|RolesManageDeActive|");          	
         	sbRights.append("createUser|createUserCre|createUserDetail|createUserEdit|createUserActive|createUserDeActive|createUserResetPassword|createUserDelete|");
         	sbRights.append("support_admin|support-cre|support-detail|support-edit|support-del|support-active|support-deactive|");
           	sbRights.append("mauhd_admin|mauhd_admin-edit|mauhd_admin-check|");
         	sbRights.append("einvoice_mtt|einvoice_mtt-cre|einvoice_mtt-detail|einvoice_mtt-edit|einvoice_mtt-sign|einvoice_mtt-copy|einvoice_mtt-cre-dc-tt|einvoice_mtt-import-auto|einvoice_mtt-signAll|einvoice_mtt-xml|");
         	sbRights.append("dm-lhd|dm-lhd-cre|dm-lhd-detail|dm-lhd-edit|dm-lhd-del|");
         	sbRights.append("tbhdssot_mtt|tbhdssot_mtt-history|tbhdssot_mtt-cre|tbhdssot_mtt-detail|tbhdssot_mtt-edit|tbhdssot_mtt-del|tbhdssot_mtt-sign|tbhdssot_mtt-send-cqt|tbhdssot_mtt-send-mail|");
        	sbRights.append("tkdsmtt|tkdsmtt-export-excel-detail|tkdsmtt-export-excel-general|");
        	sbRights.append("issu-contract-expires|issu-contract-expires-export|issu-contract-expires-active|issu-contract-expires-deactive|");
        	sbRights.append("roleManagerAdmin|RolesManageAdminCreate|RolesManageAdminDetail|RolesManageAdminEdit|RolesManageAdminActive|RolesManageAdminDeActive|");          	
         	sbRights.append("createUserAdmin|createUserAdminCre|createUserAdminDetail|createUserAdminEdit|createUserAdminActive|createUserAdminDeActive|createUserAdminResetPassword|createUserAdminDelete|");
          	sbRights.append("changeprofile|viewprofile|");
        	sbRights.append("changepass|");
        	sbRights.append("changepassAdmin|");
        	sbRights.append("ca_invoice|ca_invoice-export|");
        	sbRights.append("mauhd_update_admin|mauhd_update_admin-edit|mauhd_update_admin-check|mauhd_update_admin-export|");
         	sbRights.append("mauso-expires|mauso-expires-export|mauso-expires-active|mauso-expires-deactive|");
         	sbRights.append("tax-code-search-statistics|tax-code-search-statistics-export|");
         	sbRights.append("client-signature-info|client-signature-info-export|");
           	sbRights.append("khxhd|khxhd-export|");
           	sbRights.append("ql-user-check|qlUserCheckAdminCre|qlUserCheckAdminDetail|qlUserCheckAdminEdit|qlUserCheckAdminActive|qlUserCheckAdminDeActive|qlUserCheckAdminResetPassword|qlUserCheckAdminDelete|");
           	sbRights.append("misaSME2015|misaSME2015-export-excel|");
           	sbRights.append("tax_invoice|tax_invoice-cre|tax_invoice-detail|tax_invoice-edit|tax_invoice-del|tax_invoice-active|tax_invoice-deActive|tax_invoice-default|");
        	sbRights.append("email-send-hd|email-send-hd-export|");
           	sbRights.append("sl-access-users|");       	
           	sbRights.append("tbhdssot_mtt|tbhdssot_mtt-history|tbhdssot_mtt-cre|tbhdssot_mtt-detail|tbhdssot_mtt-edit|tbhdssot_mtt-del|tbhdssot_mtt-sign|tbhdssot_mtt-send-cqt|tbhdssot_mtt-send-mail|");
        	sbRights.append("tkdsmtt|tkdsmtt-export-excel-detail|tkdsmtt-export-excel-general|");   	
        	sbRights.append("einvoice_mtt-del|einvoice_mtt-send-cqt|einvoice_mtt-history|einvoice_mtt-publish|einvoice_mtt_list|einvoice_mtt_list-send|einvoice_mtt_list-sendAll|einvoice_mtt-send-mail|");        	
        	sbRights.append("einvoice_mtt_cqt|einvoice_mtt_cqt-detail|einvoice_mtt-send-emailauto|einvoice_mtt-send-cqt|einvoice_mtt-sendAll-email-auto|einvoice_mtt-send-cqtAll|");
          	sbRights.append("statistic-report|statistic-report-export|statistic-report-backup|");
          	sbRights.append("tbhdssot_tbn|tbhdssot_tbn-history|tbhdssot_tbn-cre|tbhdssot_tbn-detail|tbhdssot_tbn-edit|tbhdssot_tbn-del|tbhdssot_tbn-sign|tbhdssot_tbn-send-cqt|tbhdssot_tbn-send-mail|");
          	sbRights.append("param-admin|");
        	sbRights.append("qlnvtncn-import|dmproduct-copy|einvoice-pdfAll1|cttncn-xmlV1|cttncn-importV1|cttncn-pdfAllV1|cttncn-send-mail-allV1|cttncn-signAllV1|cttncn-refreshAllFromCQTV1|cttncn-sendAllToCQTV1|");
			sbRights.append("issu-grant|issu-ungrant|einvoice-cre-dctt-tbn|statistic-tcgp-tctn|call_recieve_message_statistic|call_recieve_message_statistic-export|lbbdctt-cre-invs|lbbdctt-edit-invs|");
			sbRights.append("convert-cttncn-edit|convert-cttncn|einvoice_mtt-pdfAll|einvoice1-pdfCD|issu-contract-export|");
			}
        	cup.setUsername(userName);
        	cup.setPassword(password);
        	cup.setLoginRes(res);
        	cup.setAllRights(sbRights.toString());
        	
        
            grantedAuthoritiesList.add(role);	            
            cup.setAuthorities(grantedAuthoritiesList);
            
            auth = new UsernamePasswordAuthenticationToken(cup, password, grantedAuthoritiesList);
            
            SecurityContext sc = new SecurityContextImpl();
            sc.setAuthentication(auth);
            SecurityContextHolder.setContext(sc);
            
            session.setMaxInactiveInterval(60 * 60 * 2);
			
			dtoRes = new BaseDTO(0, Constants.MAP_ERROR.get(0));
        }else {
        	dtoRes = new BaseDTO(res.getStatusCode(), res.getStatusText());
        }
		return dtoRes;
	}
	
	@RequestMapping(value = {"/logout"})
	public String logoutPage(HttpServletRequest request, HttpServletResponse response, Model model, RedirectAttributes redirectAttributes) {
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		
		String pattern = (String) request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE);
		
		SecurityContextHolder.getContext().setAuthentication(null);
		new SecurityContextLogoutHandler().logout(request, response, auth);
		
		redirectAttributes.addFlashAttribute("logout", "true");
		return "redirect:/login";
			
	}
	
}
