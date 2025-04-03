package vn.sesgroup.hddt.controller.tncn;

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
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.context.WebApplicationContext;

import com.api.message.JSONRoot;
import com.api.message.Msg;
import com.api.message.MsgRsp;
import com.api.message.MspResponseStatus;
import com.fasterxml.jackson.databind.JsonNode;

import vn.sesgroup.hddt.controller.AbstractController;
import vn.sesgroup.hddt.controller.einvoice.EinvoiceSendMailController;
import vn.sesgroup.hddt.dto.BaseDTO;
import vn.sesgroup.hddt.dto.CurrentUserProfile;
import vn.sesgroup.hddt.dto.IssuerInfo;
import vn.sesgroup.hddt.resources.RestAPIUtility;
import vn.sesgroup.hddt.utils.Constants;
import vn.sesgroup.hddt.utils.Json;

@Controller
@RequestMapping({ "/cttncn-send-mail" })
@Scope(value = WebApplicationContext.SCOPE_REQUEST)
public class ChungTuSendMailController extends AbstractController {
	private static final Logger log = LogManager.getLogger(EinvoiceSendMailController.class);
	@Autowired
	RestAPIUtility restAPI;

	private String errorCode;
	private String errorDesc;
	private String _id;
	private String _title;
	private String _email;
	private String _content;

	@RequestMapping(value = "/init", method = { RequestMethod.POST })
	public String init(Locale locale, HttpServletRequest req, HttpSession session,
			@RequestAttribute(name = "transaction", value = "", required = false) String transaction) throws Exception {
		req.setAttribute("_header_", "Gửi thông tin hóa đơn");

		errorCode = "";
		errorDesc = "";

		CurrentUserProfile cup = getCurrentlyAuthenticatedPrincipal();
		_id = commons.getParameterFromRequest(req, "_id");
		inquiry(cup, locale, req, session, _id);

		if (!"".equals(errorCode))
			req.setAttribute("messageError", errorDesc);

		return "tncn/cttncn-send-mail";
	}

	private void inquiry(CurrentUserProfile cup, Locale locale, HttpServletRequest req, HttpSession session, String _id)
			throws Exception {
		if ("".equals(_id)) {
			errorCode = "404";
			errorDesc = "Không tìm thấy thông tin hóa đơn.";
			return;
		}
		BaseDTO baseDTO = new BaseDTO(req);
		Msg msg = baseDTO.createMsg(cup, Constants.MSG_ACTION_CODE.INQUIRY);
		HashMap<String, String> hData = new HashMap<>();
		msg.setObjData(hData);

		JSONRoot root = new JSONRoot(msg);
		MsgRsp rsp = restAPI.callAPINormal("/cttncn/detail/" + _id, cup.getLoginRes().getToken(), HttpMethod.POST,
				root);
		MspResponseStatus rspStatus = rsp.getResponseStatus();
		if (rspStatus.getErrorCode() == 0) {
			IssuerInfo ii = cup.getLoginRes().getIssuerInfo();

			JsonNode jsonData = Json.serializer().nodeFromObject(rsp.getObjData());

			StringBuffer title = new StringBuffer();
			String emailReceive = "";
			String tmp = "";

			title.append(ii.getTaxCode());
			title.append(" ");
			title.append(ii.getName());
			title.append(" Thông báo phát hành Chứng từ khấu trừ thuế");

			tmp = commons.getTextJsonNode(jsonData.at("/TaxCode"));
			if (!"".equals(tmp)) {
				title.append(" ");
				title.append(tmp);
			}
			tmp = commons.getTextJsonNode(jsonData.at("/Name"));
			if (!"".equals(tmp)) {
				title.append(" ");
				title.append(tmp);
			}
			title.append(" - Số Chứng từ ");

			if (!jsonData.at("/SHDon").isMissingNode())
				title.append(jsonData.at("/SHDon").doubleValue());
			title.append(" (No reply)");

			tmp = commons.getTextJsonNode(jsonData.at("/Name")).toUpperCase();
			StringBuilder sb = new StringBuilder();
			sb.setLength(0);
			sb.append("<p><span style='font-family: Times New Roman;font-size: 13px;'>Kính gửi: <label>"
					+ ("".equals(tmp) ? "Nhân viên" : tmp) + "</label><o:p></o:p></span></p>\n");
			sb.append("<p><span style='font-family: Times New Roman;font-size: 13px;'><label>" + ii.getName()
					+ "</label> xin gửi " + ("".equals(tmp) ? "Nhân viên" : tmp)
					+ " chứng từ khấu trừ thuế TNCN theo file đính kèm.</span></p>\n");
			sb.append("<p><span style='font-family: Times New Roman;font-size: 13px;'>Trân trọng!</span></p>");

			sb.append("<hr style='margin: 5px 0 5px 0;'>");
			sb.append(
					"<p style='margin-bottom: 3px;'><span style='font-family: Times New Roman;font-size: 13px;color:red;font-weight: bold;'>NHÂN VIÊN VUI LÒNG KHÔNG REPLY EMAIL NÀY!</span></p>");
			sb.append(
					"<p style='margin-bottom: 0px;'><span style='font-family: Times New Roman;font-size: 13px;'><label style='font-weight: bold;'>"
							+ ii.getName().toUpperCase() + "</label><o:p></o:p></span></p>");
			sb.append("<p><span style='font-family: Times New Roman;font-size: 13px;'>" + ii.getAddress()
					+ "</span></p>\n");

			req.setAttribute("_id", _id);
			req.setAttribute("Title", title.toString());
			req.setAttribute("EmailReceive", emailReceive);
			req.setAttribute("EmailContent", sb.toString());
		} else {
			errorCode = "505";
			errorDesc = rspStatus.getErrorDesc();
		}
	}

	private BaseDTO checkDataToSend(HttpServletRequest req, HttpSession session, String transaction,
			CurrentUserProfile cup) throws Exception {
		BaseDTO dto = new BaseDTO();
		dto.setErrorCode(0);

		_id = commons.getParameterFromRequest(req, "_id").replaceAll("\\s", "");
		_title = commons.getParameterFromRequest(req, "_title").trim().replaceAll("\\s+", " ").toUpperCase();
		String mailcc = commons.getParameterFromRequest(req, "_emailcc").trim().replaceAll("\\s+", " ");
		if (mailcc != "") {
			_email = commons.getParameterFromRequest(req, "_email").trim().replaceAll("\\s+", " ") + "," + mailcc;
		} else {
			_email = commons.getParameterFromRequest(req, "_email").trim().replaceAll("\\s+", " ");
		}

		_content = commons.getParameterFromRequest(req, "_content").trim().replaceAll("\\s+", " ");

		if ("".equals(_id)) {
			dto.setErrorCode(1);
			dto.getErrorMessages().add("Không tìm thấy thông tin hóa đơn.");
		}
		if ("".equals(_title)) {
			dto.setErrorCode(1);
			dto.getErrorMessages().add("Vui lòng nhập vào tiêu đề email.");
		}
		if ("".equals(_email)) {
			dto.setErrorCode(1);
			dto.getErrorMessages().add("Vui lòng nhập vào email nhận.");
		}
		if ("".equals(_content)) {
			dto.setErrorCode(1);
			dto.getErrorMessages().add("Vui lòng nhập vào nội dung gửi mail.");
		}
		return dto;
	}

	@RequestMapping(value = "/check-data-send", produces = MediaType.APPLICATION_JSON_VALUE, method = RequestMethod.POST)
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
		String messageConfirm = "Bạn có muốn thực hiện gửi mail không?";

		CurrentUserProfile cup = getCurrentlyAuthenticatedPrincipal();
		dto = checkDataToSend(req, session, transaction, cup);
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

	@RequestMapping(value = "/send-mail", produces = MediaType.APPLICATION_JSON_VALUE, method = RequestMethod.POST)
	@ResponseBody
	public BaseDTO execSendMail(HttpServletRequest req, HttpSession session,
			@RequestAttribute(name = "transaction", value = "", required = false) String transaction,
			@RequestParam(value = "tokenTransaction", required = false, defaultValue = "") String tokenTransaction)
			throws Exception {
		BaseDTO dtoRes = new BaseDTO();

		CurrentUserProfile cup = getCurrentlyAuthenticatedPrincipal();
		dtoRes = checkDataToSend(req, session, transaction, cup);
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
		/* END: CHECK TOKEN */

		String actionCode = Constants.MSG_ACTION_CODE.CREATED;

		dtoRes = new BaseDTO(req);
		Msg msg = dtoRes.createMsg(cup, actionCode);
		HashMap<String, Object> hData = new HashMap<>();
		hData.put("_id", _id);
		hData.put("_title", _title);
		hData.put("_email", _email);
		hData.put("_content", _content);

		msg.setObjData(hData);
		JSONRoot root = new JSONRoot(msg);
		MsgRsp rsp = restAPI.callAPINormal("/cttncn/send-mail", cup.getLoginRes().getToken(), HttpMethod.POST, root);
		MspResponseStatus rspStatus = rsp.getResponseStatus();
		if (rspStatus.getErrorCode() == 0) {
			dtoRes.setErrorCode(0);
			dtoRes.setResponseData("Gửi email hóa đơn thành công.");
		} else {
			dtoRes.setErrorCode(rspStatus.getErrorCode());
			dtoRes.setResponseData(rspStatus.getErrorDesc());
		}

		return dtoRes;
	}
}
