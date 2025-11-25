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
import java.util.List;
import java.util.Locale;
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
@RequestMapping("/call_recieve_message_statistic")
@Scope(value = WebApplicationContext.SCOPE_REQUEST)
public class CallRecieveMessageStatisticController extends AbstractController {
	private static final Logger log = LogManager.getLogger(CallRecieveMessageStatisticController.class);
	@Autowired
	RestAPIUtility restAPI;

	@RequestMapping(value = "/init", method = { RequestMethod.POST, RequestMethod.GET })
	public String init(Locale locale, Principal principal, HttpServletRequest req) throws Exception {
		req.setAttribute("_TitleView_", Constants.PREFIX_TITLE + " - Thống kê lượt gọi thông điệp đến TCTN");
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
			return "/statistic/call-receive-message-statistic";
		} else {
			return "/admin/admin";
		}

	}
	
	@SuppressWarnings("unchecked")
	@RequestMapping(value = "/search", produces = MediaType.APPLICATION_JSON_VALUE, method = RequestMethod.POST)
	@ResponseBody
	public BaseDTO execSearch(Locale locale, HttpServletRequest req, HttpSession session) throws Exception {
		JsonGridDTO grid = new JsonGridDTO();

		CurrentUserProfile cup = getCurrentlyAuthenticatedPrincipal();
		BaseDTO baseDTO = new BaseDTO(req);
		Msg msg = baseDTO.createMsg(cup, Constants.MSG_ACTION_CODE.SEARCH);

		String toDate = commons.getParameterFromRequest(req, "to-date").replaceAll("\\s", "");
		String fromDate = commons.getParameterFromRequest(req, "from-date").replaceAll("\\s", "");

		if (commons.getParameterFromRequest(req, "to-date").replaceAll("\\s", "").trim().length() == 0) {
			grid.setErrorCode(999);
			grid.setResponseData("Vui lòng nhập ngày kết thúc tìm kiếm.");
			return grid;
		}

		if (commons.getParameterFromRequest(req, "from-date").replaceAll("\\s", "").trim().length() == 0) {
			grid.setErrorCode(999);
			grid.setResponseData("Vui lòng nhập ngày bắt đầu tìm kiếm.");
			return grid;
		}
			
		HashMap<String, Object> hData = new HashMap<>();
		hData.put("ToDate", toDate);
		hData.put("FromDate", fromDate);
		msg.setObjData(hData);

		JSONRoot root = new JSONRoot(msg);
		MsgRsp rsp = restAPI.callAPINormal("/call_recieve_message_statistic/list", cup.getLoginRes().getToken(), HttpMethod.POST, root);
		MspResponseStatus rspStatus = rsp.getResponseStatus();
		if (rsp.getResponseStatus().getErrorCode() == 0) {
			MsgPage page = rsp.getMsgPage();
			grid.setTotal(page.getTotalRows());

			List<HashMap<String, Object>> dataList = new ArrayList<HashMap<String,Object>>();
			Object objData = rsp.getObjData();
			if (objData != null && objData instanceof List) {
				dataList = (List<HashMap<String, Object>>) rsp.getObjData();
			}
			HashMap<String, String> hItem = null;
			for (HashMap<String, Object> data : dataList) {
				hItem = new HashMap<String, String>();
				hItem.put("_id", Objects.toString(data.get("_id"), ""));
				hItem.put("TaxCode", Objects.toString(data.get("TaxCode"), ""));
				hItem.put("TNCN", Objects.toString(data.get("TCTN"), ""));
				hItem.put("SendDate", commons.convertLocalDateTimeToString(commons.convertLongToLocalDate(Long.parseLong(Objects.toString(data.get("SendDate"), ""))), Constants.FORMAT_DATE.FORMAT_DATE_WEB));
				hItem.put("MSKHieu", Objects.toString(data.get("KHMSHDon"), "") + Objects.toString(data.get("KHHDon"), ""));
				hItem.put("SHDon", commons.formatNumberBillInvoice(Objects.toString(data.get("SHDon"), "")));
				hItem.put("MLTDiep", Objects.toString(data.get("MLTDiep"), ""));
				hItem.put("MTDiep", Objects.toString(data.get("MTDiep"), ""));
				hItem.put("MTDTChieu", Objects.toString(data.get("MTDTChieu"), ""));
				hItem.put("Status", Constants.MAP_CALL_RECEIVED_MESSAGE_STATUS.get(Objects.toString(data.get("Code"), "")));				
				grid.getRows().add(hItem);
			}
		} else {
			grid = new JsonGridDTO();
			grid.setErrorCode(rspStatus.getErrorCode());
			grid.setResponseData(rspStatus.getErrorDesc());
		}
 		return grid;
	}

	@RequestMapping(value = {"/check-data-export" }, produces = MediaType.APPLICATION_JSON_VALUE, method = RequestMethod.POST)
	@ResponseBody
	public BaseDTO execCheckDataToExport(Locale locale, HttpServletRequest req, HttpSession session,
			@RequestParam(value = "transaction", required = false, defaultValue = "") String transaction,
			@RequestParam(value = "method", required = false, defaultValue = "") String method) throws Exception {
		String token = "";
		if (null != session.getAttribute(Constants.SESSION_TYPE.SESSION_TOKEN_EXECUTE)) {
			token = session.getAttribute(Constants.SESSION_TYPE.SESSION_TOKEN_EXECUTE).toString();
			session.removeAttribute(token);
		}
		session.removeAttribute(Constants.SESSION_TYPE.SESSION_TOKEN_EXECUTE);

		BaseDTO dto = new BaseDTO();

		token = commons.csRandomAlphaNumbericString(30);
		session.setAttribute(Constants.SESSION_TYPE.SESSION_TOKEN_EXECUTE, token);
		HashMap<String, Object> hData = new HashMap<>();
		String toDate = commons.getParameterFromRequest(req, "to-date").replaceAll("\\s", "");
		String fromDate = commons.getParameterFromRequest(req, "from-date").replaceAll("\\s", "");

		if (commons.getParameterFromRequest(req, "to-date").replaceAll("\\s", "").trim().length() == 0) {
			dto.setErrorCode(999);
			dto.setErrorDesc("Vui lòng nhập ngày kết thúc tìm kiếm.");
			return dto;
		}

		if (commons.getParameterFromRequest(req, "from-date").replaceAll("\\s", "").trim().length() == 0) {
			dto.setErrorCode(999);
			dto.setErrorDesc("Vui lòng nhập ngày bắt đầu tìm kiếm.");
			return dto;
		}
		
		hData = new HashMap<>();
		hData.put("ToDate", toDate);
		hData.put("FromDate", fromDate);

		HashMap<String, String> hInfo = new HashMap<String, String>();
		hInfo.put("TOKEN", token);
		session.setAttribute(token, hData);

		dto.setResponseData(hInfo);
		dto.setErrorCode(0);
		return dto;
	}

	@SuppressWarnings("unchecked")
	@RequestMapping(value = { "/export-excel/{token-transaction}" }, method = { RequestMethod.GET })
	public void exportExcel(Locale locale, HttpServletRequest req, HttpServletResponse resp, HttpSession session,
			@RequestAttribute(name = "method", required = false, value = "") String method,
			@PathVariable(name = "token-transaction", required = false, value = "") String tokenTransaction)
			throws Exception {
		PrintWriter writer = null;
		BaseDTO baseDTO = null;

		try {
			/* CHECK TOKEN */
			String token = session.getAttribute(Constants.SESSION_TYPE.SESSION_TOKEN_EXECUTE) == null ? ""
					: session.getAttribute(Constants.SESSION_TYPE.SESSION_TOKEN_EXECUTE).toString();
			session.removeAttribute(Constants.SESSION_TYPE.SESSION_TOKEN_EXECUTE);
			if ("".equals(token) || !tokenTransaction.equals(token)) {
				resp.setContentType("text/html; charset=utf-8");
				resp.setCharacterEncoding("UTF-8");
				resp.setHeader("success", "yes");
				writer = resp.getWriter();
				writer.write("Token giao dịch không hợp lệ.");
				writer.flush();
				writer.close();
				return;
			}
			/* END: CHECK TOKEN */
			HashMap<String, Object> hData = new HashMap<>();
			if (null != session.getAttribute(token) && session.getAttribute(token) instanceof HashMap)
				hData = (HashMap<String, Object>) session.getAttribute(token);
			session.removeAttribute(token);

			CurrentUserProfile cup = getCurrentlyAuthenticatedPrincipal();
			baseDTO = new BaseDTO(req);
			Msg msg = baseDTO.createMsg(cup, Constants.MSG_ACTION_CODE.SEARCH);

			msg.setObjData(hData);

			JSONRoot root = new JSONRoot(msg);

			String url = "/call_recieve_message_statistic/export-excel";
			vn.sesgroup.hddt.dto.FileInfo fileInfo = restAPI.callAPIGetFileInfo(url, cup.getLoginRes().getToken(),
					HttpMethod.POST, root);
			if (null != fileInfo) {
				String fileNameOut = "Thống-kê-số-lượng-thông-điệp-tới-TCTN.xlsx";

				String type = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet;charset=UTF-8";
				resp.setHeader("Content-Disposition",
						"inline; filename=" + URLEncoder
								.encode(null == fileInfo.getFileName() ? fileNameOut : fileInfo.getFileName(), "UTF-8")
								.replaceAll("\\+", "%20"));
				InputStream inputStream = new ByteArrayInputStream(fileInfo.getContentFile());
				resp.setHeader("Content-Type", type);

				resp.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
				resp.setHeader("Pragma", "no-cache");
				resp.setHeader("Expires", "0"); 

				int bufferSize = 1024;
				resp.setContentType(type);
				final byte[] buffer = new byte[bufferSize];
				int bytesRead = 0;
				OutputStream out = null;
				try {
					out = resp.getOutputStream();
					long totalWritten = 0;
					while ((bytesRead = inputStream.read(buffer)) > 0) {
						out.write(buffer, 0, bytesRead);
						totalWritten += bytesRead;
						if (totalWritten >= buffer.length) {
							out.flush();
						}
					}
				} finally {
					tryToCloseStream(out);
					tryToCloseStream(inputStream);
				}
			} else {
				resp.setContentType("text/html; charset=utf-8");
				resp.setCharacterEncoding("UTF-8");
				resp.setHeader("success", "yes");
				writer = resp.getWriter();
				writer.write("[NULL] - " + Constants.ERROR_DESCRIPTION_EXPORT_EXCEL);
				writer.close();
			}
		} catch (Exception e) {
			log.error(">>>>> An exception occurred!", e);

			resp.setContentType("text/html; charset=utf-8");
			resp.setCharacterEncoding("UTF-8");
			resp.setHeader("success", "yes");
			writer = resp.getWriter();
			writer.write("Exception - " + Constants.ERROR_DESCRIPTION_EXPORT_EXCEL);
			writer.flush();
			writer.close();
		}
	}
}
