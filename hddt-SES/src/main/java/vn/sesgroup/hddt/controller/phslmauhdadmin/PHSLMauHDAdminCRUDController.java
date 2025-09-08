package vn.sesgroup.hddt.controller.phslmauhdadmin;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
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
import com.fasterxml.jackson.databind.JsonNode;

import vn.sesgroup.hddt.controller.AbstractController;
import vn.sesgroup.hddt.dto.BaseDTO;
import vn.sesgroup.hddt.dto.CurrentUserProfile;
import vn.sesgroup.hddt.resources.RestAPIUtility;
import vn.sesgroup.hddt.utils.Constants;
import vn.sesgroup.hddt.utils.Json;

@Controller
@RequestMapping({
	"/phsl-mauhdadmin-cre"
	, "/phsl-mauhdadmin-detail"
	, "/phsl-mauhdadmin-edit"
})
@Scope(value = WebApplicationContext.SCOPE_REQUEST)
public class PHSLMauHDAdminCRUDController extends AbstractController{
	@Autowired RestAPIUtility restAPI;
	@Autowired RestTemplate restTemplate;
	
	private String errorDesc;
	private String _id;
	private String mstkh;
	private String mausohdon;
	private String quantity;
	
	@RequestMapping(value = "/init", method = { RequestMethod.POST })
	public String init(Locale locale, HttpServletRequest req, HttpSession session,
			@RequestAttribute(name = "transaction", value = "", required = false) String transaction,
			@RequestAttribute(name = "method", value = "", required = false) String method) throws Exception {
		errorDesc = "";

		CurrentUserProfile cup = getCurrentlyAuthenticatedPrincipal();
		_id = commons.getParameterFromRequest(req, "_id");
		String header = "Thêm mới thông báo phát hành";
		String action = "CREATE";
		boolean isEdit = false;

		switch (transaction) {
		case "phsl-mauhdadmin-cre":
			header = "Thêm mới thông báo phát hành";
			action = "CREATE";
			isEdit = true;
			break;
		case "phsl-mauhdadmin-edit":
			header = "Phát hành số lượng";
			action = "EDIT";
			isEdit = true;
			break;
		case "phsl-mauhdadmin-detail":
			header = "Chi tiết nội dung phát hành";
			action = "DETAIL";
			isEdit = false;
			break;

		default:
			break;
		}

		if ("|phsl-mauhdadmin-edit|phsl-mauhdadmin-detail|".indexOf(transaction) != -1)
			inquiry(cup, locale, req, session, _id, action, transaction, method);
		if ("phsl-mauhdadmin-cre".equals(transaction)) {
			req.setAttribute("NLap",
					commons.convertLocalDateTimeToString(LocalDate.now(), Constants.FORMAT_DATE.FORMAT_DATE_WEB));
		}

		req.setAttribute("_header_", header);
		req.setAttribute("_action_", action);
		req.setAttribute("_isedit_", isEdit);
		req.setAttribute("_id", _id);

		if (!"".equals(errorDesc))
			req.setAttribute("messageError", errorDesc);

		return "realease_quantity_admin/realease_quantities_admin-crud";
	}

	private void inquiry(CurrentUserProfile cup, Locale locale, HttpServletRequest req, HttpSession session, String _id,
			String action, String transaction, String method) throws Exception {
		if ("".equals(_id)) {
			errorDesc = "Không tìm thấy thông tin hóa đơn.";
			return;
		}
		BaseDTO baseDTO = new BaseDTO(req);
		Msg msg = baseDTO.createMsg(cup, Constants.MSG_ACTION_CODE.INQUIRY);
		HashMap<String, String> hData = new HashMap<>();
		msg.setObjData(hData);

		JSONRoot root = new JSONRoot(msg);
		MsgRsp rsp = restAPI.callAPINormal("/phsl-mauhdadmin/detail/" + _id, cup.getLoginRes().getToken(),
				HttpMethod.POST, root);
		MspResponseStatus rspStatus = rsp.getResponseStatus();
		if (rspStatus.getErrorCode() == 0) {

			JsonNode jsonData = Json.serializer().nodeFromObject(rsp.getObjData());
//			req.setAttribute("THDon", commons.getTextJsonNode(jsonData.at("/")));
			req.setAttribute("_id", _id);
			req.setAttribute("MauSoHdon", commons.getTextJsonNode(jsonData.at("/KHMSHDon"))
					+ commons.getTextJsonNode(jsonData.at("/KHHDon")));
			req.setAttribute("KHMSHDon", commons.getTextJsonNode(jsonData.at("/KHMSHDon")));
			req.setAttribute("Quantity", commons.getTextJsonNode(jsonData.at("/SoLuong")));
			req.setAttribute("NamePhoi", commons.getTextJsonNode(jsonData.at("/NamePhoi")));
			req.setAttribute("TuSo", commons.getTextJsonNode(jsonData.at("/TuSo")));
			req.setAttribute("DenSo", commons.getTextJsonNode(jsonData.at("/DenSo")));
			req.setAttribute("NLap",
					commons.convertLocalDateTimeToString(commons.convertLongToLocalDate(jsonData.at("/NLap").asLong()),
							Constants.FORMAT_DATE.FORMAT_DATE_WEB));
			req.setAttribute("MSTKH", commons.getTextJsonNode(jsonData.at("/Issuer/TaxCode")));
		} else {
			errorDesc = rspStatus.getErrorDesc();
		}
	}

	public BaseDTO checkDataToAccept(HttpServletRequest req, HttpSession session, String transaction,
			CurrentUserProfile cup) throws Exception {
		BaseDTO dto = new BaseDTO();
		dto.setErrorCode(0);

		_id = commons.getParameterFromRequest(req, "_id").replaceAll("\\s", "");
		mstkh = commons.getParameterFromRequest(req, "mstkh").replaceAll("\\s", "");
		mausohdon = commons.getParameterFromRequest(req, "mausohdon").replaceAll("\\s", "");
		quantity = commons.getParameterFromRequest(req, "quantity").replaceAll("\\s", "");

		if ("phsl-mauhdadmin-edit".equals(transaction)) {
			if ("".equals(_id)) {
				dto.setErrorCode(1);
				dto.getErrorMessages().add("Không tìm thấy thông tin hóa đơn.");
			}
		}

		switch (transaction) {
		case "phsl-mauhdadmin-cre":
		case "phsl-mauhdadmin-edit":
			if ("".equals(quantity)) {
				dto.setErrorCode(1);
				dto.getErrorMessages().add("Vui lòng nhập số lượng.");
			}
			if ("".equals(mstkh)) {
				dto.setErrorCode(1);
				dto.getErrorMessages().add("Vui lòng nhập MST khách hàng.");
			}
			if ("".equals(mausohdon)) {
				dto.setErrorCode(1);
				dto.getErrorMessages().add("Vui lòng chọn Mẫu số.");
			}
			break;
		default:
			break;
		}

		return dto;
	}

	@RequestMapping(value = "/check-data-save", produces = MediaType.APPLICATION_JSON_VALUE, method = RequestMethod.POST)
	@ResponseBody
	public BaseDTO execCheckDataToSave(Locale locale, HttpServletRequest req, HttpSession session,
			@RequestAttribute(name = "transaction", required = false, value = "") String transaction) throws Exception {
		String token = "";
		if (null != session.getAttribute(Constants.SESSION_TYPE.SESSION_TOKEN_EXECUTE)) {
			token = session.getAttribute(Constants.SESSION_TYPE.SESSION_TOKEN_EXECUTE).toString();
			session.removeAttribute(token);
		}
		session.removeAttribute(Constants.SESSION_TYPE.SESSION_TOKEN_EXECUTE);

		BaseDTO dto = new BaseDTO();
		String messageConfirm = "Bạn có muốn thêm mới hóa đơn không?";
		switch (transaction) {
		case "phsl-mauhdadmin-cre":
			messageConfirm = "Bạn có muốn thêm mới hóa đơn không?";
			break;
		case "phsl-mauhdadmin-edit":
			messageConfirm = "Bạn có muốn phát hành số lượng không?";
			break;
		default:
			dto = new BaseDTO();
			dto.setErrorCode(998);
			dto.setResponseData("Không tìm thấy chức năng giao dịch.");
			return dto;
		}

		CurrentUserProfile cup = getCurrentlyAuthenticatedPrincipal();
		dto = checkDataToAccept(req, session, transaction, cup);
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

	@RequestMapping(value = "/save-data", produces = MediaType.APPLICATION_JSON_VALUE, method = RequestMethod.POST)
	@ResponseBody
	public BaseDTO execSaveData(HttpServletRequest req, HttpSession session,
			@RequestAttribute(name = "transaction", value = "", required = false) String transaction,
			@RequestParam(value = "tokenTransaction", required = false, defaultValue = "") String tokenTransaction)
			throws Exception {
		BaseDTO dtoRes = new BaseDTO();

		CurrentUserProfile cup = getCurrentlyAuthenticatedPrincipal();
		dtoRes = checkDataToAccept(req, session, transaction, cup);
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

		String actionCode = Constants.MSG_ACTION_CODE.CREATED;
		switch (transaction) {
		case "phsl-mauhdadmin-cre":
			actionCode = Constants.MSG_ACTION_CODE.CREATED;
			break;
		case "phsl-mauhdadmin-edit":
			actionCode = Constants.MSG_ACTION_CODE.MODIFY;
			break;
		default:
			dtoRes = new BaseDTO();
			dtoRes.setErrorCode(998);
			dtoRes.setResponseData(Constants.MAP_ERROR.get(998));
			return dtoRes;
		}

		dtoRes = new BaseDTO(req);
		Msg msg = dtoRes.createMsg(cup, actionCode);
		HashMap<String, Object> hData = new HashMap<>();
		hData.put("_id", _id);
		hData.put("mstkh", mstkh);
		hData.put("mausohdon", mausohdon);
		hData.put("quantity", quantity);

		msg.setObjData(hData);
		JSONRoot root = new JSONRoot(msg);
		MsgRsp rsp = restAPI.callAPINormal("/phsl-mauhdadmin/crud", cup.getLoginRes().getToken(), HttpMethod.POST,
				root);
		MspResponseStatus rspStatus = rsp.getResponseStatus();
		if (rspStatus.getErrorCode() == 0) {
			dtoRes.setErrorCode(0);
			switch (transaction) {
			case "phsl-mauhdadmin-cre":
				dtoRes.setResponseData("Thêm mới thông tin hóa đơn thành công.");
				break;
			case "phsl-mauhdadmin-edit":
				dtoRes.setResponseData("Cập nhật số lượng thành công.");
				break;
			default:
				dtoRes.setResponseData("Giao dịch thành công.");
				break;
			}
		} else {
			dtoRes.setErrorCode(rspStatus.getErrorCode());
			dtoRes.setResponseData(rspStatus.getErrorDesc());
		}
		return dtoRes;
	}

	@RequestMapping(value = "/get-mshd", produces = MediaType.APPLICATION_JSON_VALUE, method = RequestMethod.POST)
	@ResponseBody
	public List<?> getPhoi(Locale locale, HttpServletRequest req, HttpSession session,
			@RequestParam(name = "mstkh", defaultValue = "", required = true) String mstkh) throws Exception {
		List<Object> rows = new ArrayList<Object>();
		CurrentUserProfile cup = getCurrentlyAuthenticatedPrincipal();
		try {
			BaseDTO baseDTO = new BaseDTO(req);
			Msg msg = baseDTO.createMsg(cup, Constants.MSG_ACTION_CODE.INQUIRY);
			HashMap<String, String> hData = new HashMap<>();
			msg.setObjData(hData);

			JSONRoot root = new JSONRoot(msg);
			MsgRsp rsp = restAPI.callAPINormal("/phsl-mauhdadmin/getMSHD/" + mstkh, cup.getLoginRes().getToken(),
					HttpMethod.POST, root);
			MspResponseStatus rspStatus = rsp.getResponseStatus();

			if (rspStatus.getErrorCode() == 0 && rsp.getObjData() != null) {
				JsonNode jsonData = Json.serializer().nodeFromObject(rsp.getObjData());
				System.out.println();
				for (JsonNode row : jsonData.at("/DMMauSoKyHieu")) {
					rows.add(new LinkedHashMap<String, String>() {
						private static final long serialVersionUID = 5320109478345573105L;
						{
							put("Code", commons.getTextJsonNode(row.at("/_id")));
						}
						{
							put("Name", commons.getTextJsonNode(row.at("/KHMSHDon"))
									+ commons.getTextJsonNode(row.at("/KHHDon")));
						}
					});
				}
			}
		} catch (Exception e) {
			rows = new ArrayList<Object>();
		}

		return rows;
	}

}
