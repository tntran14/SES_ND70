package vn.sesgroup.hddt.controller.tncn;

import java.security.Principal;
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
@RequestMapping("/cttncn")
@Scope(value = WebApplicationContext.SCOPE_REQUEST)
public class ChungTuControllerV1 extends AbstractController {
	private static final Logger log = LogManager.getLogger(ChungTuControllerV1.class);
	@Autowired
	RestAPIUtility restAPI;

	private String sctu;
	private String date;

	@RequestMapping(value = "/initV1", method = { RequestMethod.POST, RequestMethod.GET })
	public String init(Locale locale, Principal principal, HttpServletRequest req) throws Exception {
		req.setAttribute("_TitleView_", Constants.PREFIX_TITLE + " - Danh sách chứng từ khấu trừ thuế");
		return "tncn/cttncnV1";
	}

	private BaseDTO checkDataSearch(Locale locale, HttpServletRequest req, HttpSession session) {
		BaseDTO dto = new BaseDTO();
		dto.setErrorCode(0);
		sctu = commons.getParameterFromRequest(req, "sctu").trim().replaceAll("\\s+", " ");
		date = commons.getParameterFromRequest(req, "date").trim().replaceAll("\\s+", " ");

		return dto;
	}

	@RequestMapping(value = "/searchV1", produces = MediaType.APPLICATION_JSON_VALUE, method = RequestMethod.POST)
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
		hData.put("SCTu", sctu);
		hData.put("DateLap", date);
		msg.setObjData(hData);

		JSONRoot root = new JSONRoot(msg);
		MsgRsp rsp = restAPI.callAPINormal("/cttncn/listV1", cup.getLoginRes().getToken(), HttpMethod.POST, root);
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

					hItem.put("SCTu", commons.getTextJsonNode(row.at("/SCTu")));
					hItem.put("Status", Constants.MAP_EINVOICE_STATUS.get(commons.getTextJsonNode(row.at("/Status"))));
					hItem.put("SignStatus",
							Constants.MAP_EINVOICE_SIGN_STATUS.get(commons.getTextJsonNode(row.at("/SignStatus"))));
					hItem.put("StatusCode", commons.getTextJsonNode(row.at("/Status")));
					hItem.put("SignStatusCode", commons.getTextJsonNode(row.at("/SignStatus")));
					hItem.put("NLap",
							commons.convertLocalDateTimeToString(
									commons.convertLongToLocalDate(row.at("/NLap").asLong()),
									Constants.FORMAT_DATE.FORMAT_DATE_WEB));
					String dateSave = commons.getTextJsonNode(row.at("/TTNCNKTru/TThang")) + "-"
							+ commons.getTextJsonNode(row.at("/TTNCNKTru/DThang")) + "/"
							+ commons.getTextJsonNode(row.at("/TTNCNKTru/Nam"));
					hItem.put("DateSave", dateSave);
					hItem.put("Code", commons.getTextJsonNode(row.at("/NNT/Code")));
					hItem.put("TaxCode", commons.getTextJsonNode(row.at("/NNT/MST")));
					hItem.put("Name", commons.getTextJsonNode(row.at("/NNT/Ten")));
					hItem.put("Address", commons.getTextJsonNode(row.at("/NNT/DChi")));
					hItem.put("ErrorFromCQT", commons.getTextJsonNode(row.at("/LDo/MTLoi")));
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
