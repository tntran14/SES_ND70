package vn.sesgroup.hddt.controller.statistics;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.URLEncoder;
import java.security.Principal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

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
import com.api.message.MsgRsp;
import com.api.message.MspResponseStatus;
import com.fasterxml.jackson.databind.JsonNode;

import vn.sesgroup.hddt.controller.AbstractController;
import vn.sesgroup.hddt.dto.BaseDTO;
import vn.sesgroup.hddt.dto.CurrentUserProfile;
import vn.sesgroup.hddt.dto.JsonGridDTO;
import vn.sesgroup.hddt.dto.LoginRes;
import vn.sesgroup.hddt.resources.RestAPIUtility;
import vn.sesgroup.hddt.utils.Constants;
import vn.sesgroup.hddt.utils.Json;

@Controller
@RequestMapping("/statistic-tcgp-tctn")
@Scope(value = WebApplicationContext.SCOPE_REQUEST)
public class StatisticTcgpTctnController extends AbstractController {
	private static final Logger log = LogManager.getLogger(StatisticTcgpTctnController.class);
	@Autowired
	RestAPIUtility restAPI;

	@RequestMapping(value = "/init", method = { RequestMethod.POST, RequestMethod.GET })
	public String init(Locale locale, Principal principal, HttpServletRequest req) throws Exception {
		req.setAttribute("_TitleView_", Constants.PREFIX_TITLE + " - Thống kê TCGP/TCTN");
		CurrentUserProfile cup = getCurrentlyAuthenticatedPrincipal();
		LoginRes issu = cup.getLoginRes();
		String _id = issu.getIssuerId();

		BaseDTO baseDTO = new BaseDTO(req);
		Msg msg = baseDTO.createMsg(cup, Constants.MSG_ACTION_CODE.INQUIRY);
		HashMap<String, String> hData = new HashMap<>();
		msg.setObjData(hData);
		JSONRoot root = new JSONRoot(msg);
		MsgRsp rsp = restAPI.callAPINormal("/main/profile/" + _id, cup.getLoginRes().getToken(), HttpMethod.POST, root);
		JsonNode jsonData = Json.serializer().nodeFromObject(rsp.getObjData());
		String isroot = commons.getTextJsonNode(jsonData.at("/IsRoot"));

		LocalDate now = LocalDate.now();
		req.setAttribute("ToDate", commons.convertLocalDateTimeToString(now, Constants.FORMAT_DATE.FORMAT_DATE_WEB));
		req.setAttribute("FromDate", commons.convertLocalDateTimeToString(now.minusMonths(1), Constants.FORMAT_DATE.FORMAT_DATE_WEB));

		if ("true".equals(isroot) && issu.isRoot() == true && issu.isAdmin() == true) {
			return "/statistic/statistic-tcgp-tctn";
		} else {
			return "/admin/admin";
		}

	}
	
	@RequestMapping(value = "/search", produces = MediaType.APPLICATION_JSON_VALUE, method = RequestMethod.POST)
	@ResponseBody
	public BaseDTO execSearch(Locale locale, HttpServletRequest req, HttpSession session) throws Exception {
		JsonGridDTO grid = new JsonGridDTO();

		CurrentUserProfile cup = getCurrentlyAuthenticatedPrincipal();
		BaseDTO baseDTO = new BaseDTO(req);
		Msg msg = baseDTO.createMsg(cup, Constants.MSG_ACTION_CODE.SEARCH);

		String msthue = commons.getParameterFromRequest(req, "msthue").replaceAll("\\s", "");
		String tkhang = commons.getParameterFromRequest(req, "tkhang").replaceAll("\\s", " ");

		HashMap<String, Object> hData = new HashMap<>();
		hData.put("MSThue", msthue);
		hData.put("TKHang", tkhang);
		msg.setObjData(hData);

		JSONRoot root = new JSONRoot(msg);
		MsgRsp rsp = restAPI.callAPINormal("/statistic-tcgp-tctn/list", cup.getLoginRes().getToken(), HttpMethod.POST,
				root);
		MspResponseStatus rspStatus = rsp.getResponseStatus();
		if (rsp.getResponseStatus().getErrorCode() == 0) {
			grid.setTotal(rsp.getMsgPage().getTotalRows());

			JsonNode jsonDatas = Json.serializer().nodeFromObject(rsp.getObjData());
			HashMap<String, String> hItem = null;
			StringBuilder sb = new StringBuilder();
			for (JsonNode data : jsonDatas) {
				hItem = new HashMap<String, String>();
				hItem.put("_id", commons.getTextJsonNode(data.at("/_id")));
				hItem.put("TaxCode", commons.getTextJsonNode(data.get("MST")));
				hItem.put("CompanyName", commons.getTextJsonNode(data.get("TenNnt")));
				JsonNode dstctn = data.get("DSTCTNhan");
				sb.setLength(0);
				if (dstctn != null) {
					for (JsonNode tncn : dstctn) {
						sb.append(commons.getTextJsonNode(tncn.get("MSTTCTNhan")));
						sb.append("-");
						sb.append(commons.getTextJsonNode(tncn.get("TTCTNhan")));
						sb.append("</br>");
					}
					if (sb.length() > 0)
						sb.setLength(sb.length() - 5);
				} else {
					sb.append("0401486901-CÔNG TY CỔ PHẦN THƯƠNG MẠI VISNAM");
				}
				hItem.put("TCTN", sb.toString());
				JsonNode dstcgp = data.get("DSTCGPhap");
				sb.setLength(0);
				if (dstcgp != null) {
					for (JsonNode tngp : dstcgp) {
						sb.append(commons.getTextJsonNode(tngp.get("MSTTCGPhap")));
						sb.append("-");
						sb.append(commons.getTextJsonNode(tngp.get("TTCGPhap")));
						sb.append("</br>");
					}
					if (sb.length() > 0)
						sb.setLength(sb.length() - 5);
				} else {
					sb.append("0315382923-CÔNG TY TNHH SES GROUP");
				}
				hItem.put("TCGP", sb.toString());
				grid.getRows().add(hItem);
			}
		} else {
			grid = new JsonGridDTO();
			grid.setErrorCode(rspStatus.getErrorCode());
			grid.setResponseData(rspStatus.getErrorDesc());
		}
		return grid;
	}
}
