package vn.sesgroup.hddt.controller.support;

import java.security.Principal;
import java.time.LocalDate;
import java.time.temporal.ChronoField;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
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
import vn.sesgroup.hddt.dto.JsonGridDTO;
import vn.sesgroup.hddt.dto.LoginRes;
import vn.sesgroup.hddt.resources.RestAPIUtility;
import vn.sesgroup.hddt.utils.Constants;
import vn.sesgroup.hddt.utils.Json;

@Controller
@RequestMapping("/support")
@Scope(value = WebApplicationContext.SCOPE_REQUEST)
public class SupportController extends AbstractController{
	private static final Logger log = LogManager.getLogger(SupportController.class);
	@Autowired RestAPIUtility restAPI; 
	

	@RequestMapping(value = "/init", method = {RequestMethod.POST, RequestMethod.GET})
	public String init(Locale locale, Principal principal, HttpServletRequest req) throws Exception{
		req.setAttribute("_TitleView_", Constants.PREFIX_TITLE + " - Phần mềm hỗ trợ");
			
		HashMap<String, Object> hTmp = null;
		CurrentUserProfile cup = getCurrentlyAuthenticatedPrincipal();
		List<HashMap<String, Object>> rowsMGW = null;
		
		JSONRoot root = new JSONRoot();
		BaseDTO dtoRes = new BaseDTO();
		Msg msg = dtoRes.createMsgPass();
		HashMap<String, String> hInput = new HashMap<>();
		msg.setObjData(hInput);
		 root = new JSONRoot(msg);
		//MsgRsp rsp = restAPI.callAPIPass("/forgotpass/dl", HttpMethod.POST, root);
		MsgRsp rsp = restAPI.callAPIPass("/support/getList", HttpMethod.POST, root);
		MspResponseStatus rspStatus = rsp.getResponseStatus();

		
//	
//		JSONRoot root = new JSONRoot();
//		BaseDTO dtoRes = new BaseDTO();
//		Msg msg = dtoRes.createMsgPass();
//		HashMap<String, String> hInput = new HashMap<>();
//		msg.setObjData(hInput);
//		 root = new JSONRoot(msg);
//		MsgRsp rsp = restAPI.callAPIPass("/support/getList", HttpMethod.POST, root);
//		MspResponseStatus rspStatus = rsp.getResponseStatus();
		JsonNode rows = null;
		HashMap<String, String> hItem = null;
		if(rspStatus.getErrorCode() == 0) {
			JsonNode jsonData = Json.serializer().nodeFromObject(rsp.getObjData());
			if(!jsonData.at("/rows").isMissingNode()) {
				rows = jsonData.at("/rows");
				rowsMGW = new ArrayList<HashMap<String,Object>>();
			for(JsonNode row: rows) {
			hTmp = new HashMap<String, Object>();
			String name_file = commons.getTextJsonNode(row.at("/ImageLogo"));	
			hTmp.put("Title", commons.getTextJsonNode(row.at("/Title")));
			hTmp.put("Content", commons.decodeURIComponent(commons.getTextJsonNode(row.at("/Content"))));
			hTmp.put("SummaryContent", commons.getTextJsonNode(row.at("/SummaryContent")));					
			hTmp.put("ImageLogo", commons.getTextJsonNode(row.at("/ImageLogo")));
			hTmp.put("ImageLogoOriginalFilename", commons.getTextJsonNode(row.at("/ImageLogoOriginalFilename")));
			rowsMGW.add(hTmp);
			}
			req.setAttribute("_support", rowsMGW);
		}	
		}
		return "support/support";
	}	
}
