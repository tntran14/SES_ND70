package vn.sesgroup.hddt.controller.xulyhoadon;

import java.security.Principal;
import java.time.LocalDate;
import java.time.temporal.ChronoField;
import java.util.HashMap;
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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
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
import vn.sesgroup.hddt.resources.RestAPIUtility;
import vn.sesgroup.hddt.utils.Constants;
import vn.sesgroup.hddt.utils.Json;

@Controller
@RequestMapping("/lbbdctt")
@Scope(value = WebApplicationContext.SCOPE_REQUEST)
public class LBBDCTTheController extends AbstractController {
	private static final Logger log = LogManager.getLogger(LBBDCTTheController.class);
	@Autowired
	RestAPIUtility restAPI;

	private String fromDate;
	private String toDate;
	private String status;

	@RequestMapping(value = "/init", method = { RequestMethod.POST, RequestMethod.GET })
	public String init(Locale locale, Principal principal, HttpServletRequest req) throws Exception {
		req.setAttribute("_TitleView_", Constants.PREFIX_TITLE + " - Danh sách thông báo HĐ sai sót");
		LocalDate now = LocalDate.now();
		req.setAttribute("FromDate", commons.convertLocalDateTimeToString(
				now.minusMonths(2).with(ChronoField.DAY_OF_MONTH, 1), Constants.FORMAT_DATE.FORMAT_DATE_WEB));
		req.setAttribute("ToDate", commons.convertLocalDateTimeToString(now, Constants.FORMAT_DATE.FORMAT_DATE_WEB));
		req.setAttribute("map_status", Constants.MAP_BBDCTT_STATUS);
		return "bbdctthe/bbdctthe";
	}

	private BaseDTO checkDataSearch(Locale locale, HttpServletRequest req, HttpSession session) {
		BaseDTO dto = new BaseDTO();
		dto.setErrorCode(0);
		fromDate = commons.getParameterFromRequest(req, "from-date").replaceAll("\\s", "");
		toDate = commons.getParameterFromRequest(req, "to-date").replaceAll("\\s", "");
		status = commons.getParameterFromRequest(req, "status").replaceAll("\\s", "");
		if (!"".equals(fromDate) && !commons.checkLocalDate(fromDate, Constants.FORMAT_DATE.FORMAT_DATE_WEB)) {
			dto.setErrorCode(1);
			dto.getErrorMessages().add("Từ ngày không đúng định dạng.");
		}
		if (!"".equals(toDate) && !commons.checkLocalDate(toDate, Constants.FORMAT_DATE.FORMAT_DATE_WEB)) {
			dto.setErrorCode(1);
			dto.getErrorMessages().add("Từ ngày không đúng định dạng.");
		}

		return dto;
	}

	@RequestMapping(value = "/search", produces = MediaType.APPLICATION_JSON_VALUE, method = RequestMethod.POST)
	@ResponseBody
	public BaseDTO execSearch(Locale locale, HttpServletRequest req, HttpSession session) throws Exception {
		JsonGridDTO grid = new JsonGridDTO();

		BaseDTO baseDTO = checkDataSearch(locale, req, session);
		if (0 != baseDTO.getErrorCode()) {
			grid.setErrorCode(baseDTO.getErrorCode());
			grid.setErrorMessages(baseDTO.getErrorMessages());
			grid.setResponseData(Constants.MAP_ERROR.get(999));
			return grid;
		}

		CurrentUserProfile cup = getCurrentlyAuthenticatedPrincipal();
		baseDTO = new BaseDTO(req);
		Msg msg = baseDTO.createMsg(cup, Constants.MSG_ACTION_CODE.SEARCH);

		HashMap<String, Object> hData = new HashMap<>();
		hData.put("FromDate", fromDate);
		hData.put("ToDate", toDate);
		hData.put("Status", status);
		msg.setObjData(hData);

		JSONRoot root = new JSONRoot(msg);
		MsgRsp rsp = restAPI.callAPINormal("/lbbdctt/list", cup.getLoginRes().getToken(), HttpMethod.POST, root);
		MspResponseStatus rspStatus = rsp.getResponseStatus();
		if (rspStatus.getErrorCode() == 0) {
			MsgPage page = rsp.getMsgPage();
			grid.setTotal(page.getTotalRows());
			JsonNode jsonDatas = Json.serializer().nodeFromObject(rsp.getObjData());
			HashMap<String, String> hItem = null;
			for (JsonNode jsonData : jsonDatas) {
				hItem = new HashMap<String, String>();
				hItem.put("_id", commons.getTextJsonNode(jsonData.at("/_id")));
				hItem.put("StatusDesc",
						Constants.MAP_BBDCTT_STATUS.get(commons.getTextJsonNode(jsonData.at("/Status"))));
				hItem.put("SignStatusDesc", Constants.MAP_EINVOICE_SIGN_STATUS
						.get(commons.getTextJsonNode(jsonData.at("/SignStatusCode"))));
				hItem.put("ClientSignStatusDesc", Constants.MAP_EINVOICE_SIGN_STATUS
						.get(commons.getTextJsonNode(jsonData.at("/ClientSignStatusCode"))));
				hItem.put("Status", commons.getTextJsonNode(jsonData.at("/Status")));
				hItem.put("SignStatus", commons.getTextJsonNode(jsonData.at("/SignStatusCode")));
				hItem.put("ClientSignStatus", commons.getTextJsonNode(jsonData.at("/ClientSignStatusCode")));
				hItem.put("LoaiBB", commons.getTextJsonNode(jsonData.at("/Ten")));
				hItem.put("SBBan", commons.getTextJsonNode(jsonData.at("/SBBan")));
				hItem.put("NLap",
						commons.convertLocalDateTimeStringToString(commons.getTextJsonNode(jsonData.at("/NLap")),
								"yyyy-MM-dd", Constants.FORMAT_DATE.FORMAT_DATE_WEB));
				hItem.put("NDSai", commons.getTextJsonNode(jsonData.at("/NDSai")));
				hItem.put("MTDiep", commons.getTextJsonNode(jsonData.at("/MTDiep")));
				hItem.put("SecureKey", commons.getTextJsonNode(jsonData.at("/SecureKey")));
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
