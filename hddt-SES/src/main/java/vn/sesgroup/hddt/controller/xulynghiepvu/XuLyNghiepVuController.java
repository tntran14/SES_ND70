package vn.sesgroup.hddt.controller.xulynghiepvu;

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
import vn.sesgroup.hddt.resources.RestAPIUtility;
import vn.sesgroup.hddt.utils.Constants;
import vn.sesgroup.hddt.utils.Json;

@Controller
@RequestMapping("/xuly-nghiepvu")
@Scope(value = WebApplicationContext.SCOPE_REQUEST)
public class XuLyNghiepVuController extends AbstractController{
	@Autowired RestAPIUtility restAPI; 
	
	@RequestMapping(value = "/init", method = { RequestMethod.POST, RequestMethod.GET })
	public String init(Locale locale, Principal principal, HttpServletRequest req) throws Exception {
		req.setAttribute("_TitleView_", Constants.PREFIX_TITLE + " - Cập nhật mã CQT");
		return "ad_xulynghiepvu/update_mccqt";
	}

	@RequestMapping(value = "/search", produces = MediaType.APPLICATION_JSON_VALUE, method = RequestMethod.POST)
	@ResponseBody
	public BaseDTO execSearch(Locale locale, HttpServletRequest req, HttpSession session) throws Exception {
		JsonGridDTO grid = new JsonGridDTO();

		BaseDTO baseDTO = new BaseDTO(req);
		String mtdiep = commons.getParameterFromRequest(req, "mtdiep").replaceAll("\\s", "");

		if (!"".equals(mtdiep)) {
			baseDTO.setErrorCode(1);
			baseDTO.getErrorMessages().add("Vui lòng nhập mã thông điệp.");
		}

		CurrentUserProfile cup = getCurrentlyAuthenticatedPrincipal();

		Msg msg = baseDTO.createMsg(cup, Constants.MSG_ACTION_CODE.SEARCH);

		HashMap<String, Object> hData = new HashMap<>();
		hData.put("MTDiep", mtdiep);
		msg.setObjData(hData);

		JSONRoot root = new JSONRoot(msg);
		MsgRsp rsp = restAPI.callAPINormal("/xuly-nghiepvu/get-invoices-by-mt-diep", cup.getLoginRes().getToken(),
				HttpMethod.POST, root);
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

					String status = commons.getTextJsonNode(row.at("/EInvoiceStatus"));
					String sign = commons.getTextJsonNode(row.at("/SignStatusCode"));
					if (status.equals("PENDING") && sign.equals("SIGNED")) {
						hItem.put("StatusDesc", "Chờ gửi CQT");
					} else {
						hItem.put("StatusDesc",
								Constants.MAP_EINVOICE_STATUS.get(commons.getTextJsonNode(row.at("/EInvoiceStatus"))));
					}

					hItem.put("SignStatusDesc",
							Constants.MAP_EINVOICE_SIGN_STATUS.get(commons.getTextJsonNode(row.at("/SignStatusCode"))));
					hItem.put("MCCQT", commons.getTextJsonNode(row.at("/MCCQT")));
					hItem.put("MTDTChieu", commons.getTextJsonNode(row.at("/MTDTChieu")));
					hItem.put("CQTMTLoi", commons.getTextJsonNode(row.at("/LDo/MTLoi")));
					hItem.put("MauSoHD", commons.getTextJsonNode(row.at("/EInvoiceDetail/TTChung/KHMSHDon"))
							+ commons.getTextJsonNode(row.at("/EInvoiceDetail/TTChung/KHHDon")));
					hItem.put("EInvoiceNumber",
							"".equals(commons.getTextJsonNode(row.at("/EInvoiceDetail/TTChung/SHDon"))) ? ""
									: commons.formatNumberBillInvoice(
											commons.getTextJsonNode(row.at("/EInvoiceDetail/TTChung/SHDon"))));
					hItem.put("NLap",
							commons.convertLocalDateTimeToString(
									commons.convertLongToLocalDate(row.at("/EInvoiceDetail/TTChung/NLap").asLong()),
									Constants.FORMAT_DATE.FORMAT_DATE_WEB));
					hItem.put("TaxCode", commons.getTextJsonNode(row.at("/EInvoiceDetail/NDHDon/NMua/MST")));
					hItem.put("CompanyName", commons.getTextJsonNode(row.at("/EInvoiceDetail/NDHDon/NMua/Ten")));
					hItem.put("MTDiep", commons.getTextJsonNode(row.at("/MTDiep")));
					hItem.put("UserCreated", commons.getTextJsonNode(row.at("/InfoCreated/CreateUserFullName")));
					hItem.put("SignStatusCode", commons.getTextJsonNode(row.at("/SignStatusCode")));
					hItem.put("EInvoiceStatus", commons.getTextJsonNode(row.at("/EInvoiceStatus")));
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
	
	@RequestMapping(value = "/check-data-update",  produces = MediaType.APPLICATION_JSON_VALUE, method = RequestMethod.POST)
	@ResponseBody
	public BaseDTO execCheckDataToUpdate(Locale locale, HttpServletRequest req, HttpSession session
			, @RequestAttribute(name = "transaction", required = false, value = "") String transaction) throws Exception {
		String token = "";
		if (null != session.getAttribute(Constants.SESSION_TYPE.SESSION_TOKEN_EXECUTE)) {
			token = session.getAttribute(Constants.SESSION_TYPE.SESSION_TOKEN_EXECUTE).toString();
			session.removeAttribute(token);
		}
		session.removeAttribute(Constants.SESSION_TYPE.SESSION_TOKEN_EXECUTE);
		
		BaseDTO dto = new BaseDTO();
		String messageConfirm =  "Bạn có muốn cập nhật mã cqt cho hóa đơn có mã thông điệp này không.";
			
		token = commons.csRandomAlphaNumbericString(30);
		session.setAttribute(Constants.SESSION_TYPE.SESSION_TOKEN_EXECUTE, token);
		
		HashMap<String, String> hInfo = new HashMap<String, String>();
		hInfo.put("CONFIRM", messageConfirm);
		hInfo.put("TOKEN", token);
		dto.setResponseData(hInfo);
		dto.setErrorCode(0);
		return dto;
	
	}
	
	@RequestMapping(value = "/update-data",  produces = MediaType.APPLICATION_JSON_VALUE, method = RequestMethod.POST)
	@ResponseBody
	public BaseDTO execUpdateData(Locale locale,HttpServletRequest req, HttpSession session
			, @RequestAttribute(name = "transaction", value = "", required = false) String transaction
			, @RequestParam(value = "tokenTransaction", required = false, defaultValue = "") String tokenTransaction) throws Exception{
		BaseDTO dtoRes = new BaseDTO();
		
		CurrentUserProfile cup = getCurrentlyAuthenticatedPrincipal();
		String []split_ = tokenTransaction.split(",");
		String token_ = split_[0];
		/*CHECK TOKEN*/
		String token = session.getAttribute(Constants.SESSION_TYPE.SESSION_TOKEN_EXECUTE) == null ? ""
				: session.getAttribute(Constants.SESSION_TYPE.SESSION_TOKEN_EXECUTE).toString();
		session.removeAttribute(Constants.SESSION_TYPE.SESSION_TOKEN_EXECUTE);
		if ("".equals(token) || !token_.equals(token)) {
			dtoRes.setErrorCode(1);
			dtoRes.setResponseData("Token giao dịch không hợp lệ.");
			return dtoRes;
		}
		/*END: CHECK TOKEN*/
		
		String actionCode = Constants.MSG_ACTION_CODE.CREATED;
		
		dtoRes = new BaseDTO(req);
		Msg msg = dtoRes.createMsg(cup, actionCode);
		HashMap<String, Object> hData = new HashMap<>();
		String mtdiep = commons.getParameterFromRequest(req, "mtdiep").trim().replaceAll("\\s+", "");
		String mcqt = commons.getParameterFromRequest(req, "mcqt").trim().replaceAll("\\s+", "");
		String mtc = commons.getParameterFromRequest(req, "mtc").trim().replaceAll("\\s+", "");
		hData.put("MTDiep", mtdiep);
		hData.put("MCCQT", mcqt);
		hData.put("MTDTChieu", mtc);
		msg.setObjData(hData);
		JSONRoot root = new JSONRoot(msg);
		MsgRsp rsp = restAPI.callAPINormal("/xuly-nghiepvu/update-mcqt", cup.getLoginRes().getToken(), HttpMethod.POST, root);
		MspResponseStatus rspStatus = rsp.getResponseStatus();
		if(rspStatus.getErrorCode() == 0) {
			dtoRes.setErrorCode(0);
				
		}else {
			dtoRes.setErrorCode(rspStatus.getErrorCode());
			dtoRes.setResponseData(rspStatus.getErrorDesc());
		}
		return dtoRes;
	}
}
