package vn.sesgroup.hddt.controller.admin.access;

import java.security.Principal;
import java.util.HashMap;
import java.util.Locale;

import javax.servlet.http.HttpServletRequest;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.context.WebApplicationContext;

import com.api.message.JSONRoot;
import com.api.message.Msg;
import com.api.message.MsgRsp;
import com.fasterxml.jackson.databind.JsonNode;

import vn.sesgroup.hddt.configuration.SessionListener;
import vn.sesgroup.hddt.controller.AbstractController;
import vn.sesgroup.hddt.dto.BaseDTO;
import vn.sesgroup.hddt.dto.CurrentUserProfile;
import vn.sesgroup.hddt.resources.RestAPIUtility;
import vn.sesgroup.hddt.utils.Constants;
import vn.sesgroup.hddt.utils.Json;

@Controller
@RequestMapping({ "/sl-access-users" })
@Scope(value = WebApplicationContext.SCOPE_REQUEST)
public class AccessQuantityOfCustomersController extends AbstractController {
	@Autowired
	RestAPIUtility restAPI;

	@RequestMapping(value = "/init", method = { RequestMethod.POST, RequestMethod.GET })
	public String init(Locale locale, Principal principal, HttpServletRequest req) throws Exception {
		req.setAttribute("_TitleView_", Constants.PREFIX_TITLE + " - Số lượng khách hàng truy cập");

		CurrentUserProfile cup = getCurrentlyAuthenticatedPrincipal();
		JSONRoot root = new JSONRoot();
		BaseDTO baseDTO =null;
		baseDTO = new BaseDTO(req);
		Msg msg = baseDTO.createMsg(cup, Constants.MSG_ACTION_CODE.SEARCH);
		
		int activeUsers = SessionListener.getActiveSessions();
		HashMap<String, Object> hData = new HashMap<>();
		
		
		hData.put("ActiveUsers", activeUsers);
		msg.setObjData(hData);
		
		req.setAttribute("ActiveUsers", activeUsers);

		return "access/ds-access-users";
	}
}
