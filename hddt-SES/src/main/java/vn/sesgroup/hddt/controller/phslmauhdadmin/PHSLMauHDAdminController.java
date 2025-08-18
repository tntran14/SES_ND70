package vn.sesgroup.hddt.controller.phslmauhdadmin;

import java.security.Principal;
import java.util.HashMap;
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
@RequestMapping("/phsl-mauhdadmin")
@Scope(value = WebApplicationContext.SCOPE_REQUEST)
public class PHSLMauHDAdminController extends AbstractController {
	@Autowired
	RestAPIUtility restAPI;

	private String mstkh;
	private String mausohd;

	@RequestMapping(value = "/init", method = { RequestMethod.POST, RequestMethod.GET })
	public String init(Locale locale, Principal principal, HttpServletRequest req) throws Exception {
		req.setAttribute("_TitleView_", Constants.PREFIX_TITLE + " - Danh sách phát hành mẫu số hóa đơn Khách hàng");
		return "realease_quantity_admin/realease_quantities_admin";
	}

	private BaseDTO checkDataSearch(Locale locale, HttpServletRequest req, HttpSession session) {
		BaseDTO dto = new BaseDTO();
		dto.setErrorCode(0);

		mausohd = commons.getParameterFromRequest(req, "mausohd").replaceAll("\\s", "");
		mstkh = commons.getParameterFromRequest(req, "mstkh").replaceAll("\\s", "");
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
		hData.put("MauSoHdon", mausohd);
		hData.put("MSTKH", mstkh);

		msg.setObjData(hData);

		JSONRoot root = new JSONRoot(msg);
		MsgRsp rsp = restAPI.callAPINormal("/phsl-mauhdadmin/list", cup.getLoginRes().getToken(), HttpMethod.POST,
				root);
		MspResponseStatus rspStatus = rsp.getResponseStatus();
		if (rspStatus.getErrorCode() == 0) {
			MsgPage page = rsp.getMsgPage();
			grid.setTotal(page.getTotalRows());
			JsonNode jsonData = Json.serializer().nodeFromObject(rsp.getObjData());
			JsonNode rows = null;
			HashMap<String, String> hItem = null;
			if (!jsonData.at("/rows").isMissingNode()) {
				rows = jsonData.at("/rows");
				for (JsonNode row : rows) {
					hItem = new HashMap<String, String>();

					hItem.put("_id", commons.getTextJsonNode(row.at("/_id")));

					hItem.put("KHMSHDon",
							commons.getTextJsonNode(row.at("/KHMSHDon")) + commons.getTextJsonNode(row.at("/KHHDon")));

					hItem.put("SoLuong", commons.getTextJsonNode(row.at("/SoLuong")));
					hItem.put("NamePhoi", commons.getTextJsonNode(row.at("/NamePhoi")));
					hItem.put("TuSo", commons.getTextJsonNode(row.at("/TuSo")));
					hItem.put("DenSo", commons.getTextJsonNode(row.at("/DenSo")));
					hItem.put("NLap",
							commons.convertLocalDateTimeToString(
									commons.convertLongToLocalDate(row.at("/NLap").asLong()),
									Constants.FORMAT_DATE.FORMAT_DATE_WEB));
					hItem.put("MSTKH", commons.getTextJsonNode(row.at("/Issuer/TaxCode")));
					grid.getRows().add(hItem);
				}
			}

		} else {
			grid = new JsonGridDTO();
			grid.setErrorCode(rspStatus.getErrorCode());
			grid.setResponseData(rspStatus.getErrorDesc());
		}

		return grid;
	}
}
