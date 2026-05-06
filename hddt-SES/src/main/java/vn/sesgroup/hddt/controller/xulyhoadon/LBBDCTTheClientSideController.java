package vn.sesgroup.hddt.controller.xulyhoadon;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.Principal;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Locale;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.xml.bind.DatatypeConverter;

import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.multipart.MultipartFile;

import com.api.message.JSONRoot;
import com.api.message.Msg;
import com.api.message.MsgPage;
import com.api.message.MsgRsp;
import com.api.message.MspResponseStatus;
import com.fasterxml.jackson.databind.JsonNode;

import vn.sesgroup.hddt.controller.AbstractController;
import vn.sesgroup.hddt.dto.BaseDTO;
import vn.sesgroup.hddt.dto.FileInfo;
import vn.sesgroup.hddt.dto.JsonGridDTO;
import vn.sesgroup.hddt.resources.APIParams;
import vn.sesgroup.hddt.resources.RestAPIUtility;
import vn.sesgroup.hddt.utils.Constants;
import vn.sesgroup.hddt.utils.Json;
import vn.sesgroup.hddt.utils.SystemParams;

@Controller
@RequestMapping("/lbbdctt-client")
@Scope(value = WebApplicationContext.SCOPE_REQUEST)
public class LBBDCTTheClientSideController extends AbstractController {
	private static final Logger log = LogManager.getLogger(LBBDCTTheClientSideController.class);
	@Autowired
	RestAPIUtility restAPI;
	@Autowired
	RestTemplate restTemplate;

	@RequestMapping(value = "/{secureKey}/{mtdiep}/init", method = { RequestMethod.POST, RequestMethod.GET })
	public String init(Locale locale, Principal principal, HttpServletRequest req,
			@PathVariable(value = "secureKey") String secureKey, @PathVariable(value = "mtdiep") String mtdiep) throws Exception {
		req.setAttribute("_TitleView_",
				Constants.PREFIX_TITLE + " - Biên bản điều chỉnh/thay thế của khách hàng");
		req.setAttribute("_mtdiep", mtdiep);
		req.setAttribute("_secureKey", secureKey);
		return "bbdctthe/bbdctthe-client";
	}

	@RequestMapping(value = "/search", produces = MediaType.APPLICATION_JSON_VALUE, method = RequestMethod.POST)
	@ResponseBody
	public BaseDTO execSearch(Locale locale, HttpServletRequest req, HttpSession session) throws Exception {
		JsonGridDTO grid = new JsonGridDTO();
		BaseDTO baseDTO = new BaseDTO(req);

		Msg msg = baseDTO.createMsgPass();
		String mtdiep = commons.getParameterFromRequest(req, "_mtdiep").replaceAll("\\s", "");
		String secureKey = commons.getParameterFromRequest(req, "_secureKey").replaceAll("\\s", "");

		HashMap<String, Object> hData = new HashMap<>();
		hData.put("secureKey", secureKey);
		hData.put("mtdiep", mtdiep);
		msg.setObjData(hData);

		JSONRoot root = new JSONRoot(msg);
		MsgRsp rsp = restAPI.callAPIPNotAuth("/lbbdctt-client/detail", HttpMethod.POST, root);
		MspResponseStatus rspStatus = rsp.getResponseStatus();
		if (rspStatus.getErrorCode() == 0) {
			MsgPage page = rsp.getMsgPage();
			grid.setTotal(page.getTotalRows());
			JsonNode jsonData = Json.serializer().nodeFromObject(rsp.getObjData());
			HashMap<String, String> hItem = null;
			hItem = new HashMap<String, String>();
			hItem.put("_id", commons.getTextJsonNode(jsonData.at("/_id")));
			hItem.put("ClientSignStatusDesc", Constants.MAP_EINVOICE_SIGN_STATUS
					.get(commons.getTextJsonNode(jsonData.at("/ClientSignStatusCode"))));
			hItem.put("ClientSignStatus", commons.getTextJsonNode(jsonData.at("/ClientSignStatusCode")));
			hItem.put("LoaiBB", commons.getTextJsonNode(jsonData.at("/Ten")));
			hItem.put("SBBan", commons.getTextJsonNode(jsonData.at("/SBBan")));
			hItem.put("NLap", commons.convertLocalDateTimeStringToString(commons.getTextJsonNode(jsonData.at("/NLap")),
					"yyyy-MM-dd", Constants.FORMAT_DATE.FORMAT_DATE_WEB));
			hItem.put("NDSai", commons.getTextJsonNode(jsonData.at("/NDSai")));
			grid.getRows().add(hItem);
		} else {
			grid = new JsonGridDTO();
			grid.setErrorCode(rspStatus.getErrorCode());
			grid.setResponseData(rspStatus.getErrorDesc());
		}

		return grid;
	}

	@RequestMapping(value = { "/printbb/{_id}" }, method = { RequestMethod.POST, RequestMethod.GET })
	public void printbb(Locale locale, HttpServletRequest req, HttpServletResponse resp, HttpSession session,
			@PathVariable(value = "_id") String _id) throws Exception {
		PrintWriter writer = null;

		_id = _id.replaceAll("\\s", "");
		if ("".equals(_id) || null == _id) {
			resp.setContentType("text/html; charset=utf-8");
			resp.setCharacterEncoding("UTF-8");
			resp.setHeader("success", "yes");
			writer = resp.getWriter();
			writer.write("Không tìm thấy thông tin biên bản điều chỉnh thay thế.");
			writer.flush();
			writer.close();
			return;
		}

		BaseDTO dto = new BaseDTO(req);
		Msg msg = dto.createMsgPass();
		HashMap<String, Object> hData = new HashMap<>();
		hData.put("_id", _id);

		msg.setObjData(hData);
		JSONRoot root = new JSONRoot(msg);

		FileInfo fileInfo = restAPI.callAPIGetFileInfoNotAuth("/lbbdctt-client/printbb", HttpMethod.POST, root);
		if (null == fileInfo || null == fileInfo.getContentFile()) {
			resp.setContentType("text/html; charset=utf-8");
			resp.setCharacterEncoding("UTF-8");
			resp.setHeader("success", "yes");
			writer = resp.getWriter();
			writer.write("Xem biên bản điều chỉnh thay thế không thành công.");
			writer.flush();
			writer.close();
			return;
		}

		String type = "application/pdf";
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

	}

	@RequestMapping(value = "/check-data-sign", produces = MediaType.APPLICATION_JSON_VALUE, method = RequestMethod.POST)
	@ResponseBody
	public BaseDTO execCheckDataToSign(Locale locale, HttpServletRequest req, HttpSession session) throws Exception {
		String token = "";
		if (null != session.getAttribute(Constants.SESSION_TYPE.SESSION_TOKEN_EXECUTE)) {
			token = session.getAttribute(Constants.SESSION_TYPE.SESSION_TOKEN_EXECUTE).toString();
			session.removeAttribute(token);
		}
		session.removeAttribute(Constants.SESSION_TYPE.SESSION_TOKEN_EXECUTE);

		BaseDTO dto = new BaseDTO();
		String _id = commons.getParameterFromRequest(req, "_id").replaceAll("\\s", "");

		if ("".equals(_id)) {
			dto.setErrorCode(1);
			dto.setResponseData("Không tìm thấy thông báo hđ sai sót.");
			return dto;
		}

		/* LAY THONG TIN DU LIEU XML VE SERVER WEB */
		dto = new BaseDTO(req);
		Msg msg = dto.createMsgPass();
		HashMap<String, Object> hData = new HashMap<>();
		hData.put("_id", _id);
		msg.setObjData(hData);
		JSONRoot root = new JSONRoot(msg);

		FileInfo fileInfo = restAPI.callAPIGetFileInfoNotAuth("/lbbdctt-client/get-file-for-sign", HttpMethod.POST,
				root);
		if (null == fileInfo || null == fileInfo.getContentFile()) {
			dto.setErrorCode(999);
			dto.setResponseData("Không tìm thấy dữ liệu hóa đơn.");
			return dto;
		}

		/* THONG TIN TEN FILE */
		token = commons.convertLocalDateTimeToString(LocalDateTime.now(), Constants.FORMAT_DATE.FORMAT_DATETIME_DB_FULL)
				+ "-" + commons.csRandomAlphaNumbericString(5);
		token += ".xml";
		File file = new File(SystemParams.DIR_TMP_SAVE_FILES);
		file.mkdirs();
		FileUtils.writeByteArrayToFile(new File(SystemParams.DIR_TMP_SAVE_FILES, token), fileInfo.getContentFile());
		/* END - LAY THONG TIN DU LIEU XML VE SERVER WEB */

		HashMap<String, String> hInfo = new HashMap<String, String>();
		hInfo.put("TOKEN", token);

		dto.setResponseData(hInfo);
		dto.setErrorCode(0);
		return dto;
	}

	@RequestMapping(value = "/signFile", produces = MediaType.APPLICATION_JSON_VALUE, method = RequestMethod.POST, consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	@ResponseBody
	public BaseDTO processSignFile(HttpServletRequest req, HttpSession session,
			@RequestAttribute(name = "transaction", value = "", required = false) String transaction,
			@RequestParam("XMLFileSigned") MultipartFile multipartFile, @RequestParam("certificate") String certificate,
			@RequestParam("_id") String _id) throws Exception {

		BaseDTO dtoRes = new BaseDTO(req);
		Msg msg = dtoRes.createMsgPass();
		HashMap<String, Object> hData = new HashMap<>();
		msg.setObjData(hData);
		JSONRoot root = new JSONRoot(msg);

		/* CONNECT TO API */
		HttpHeaders headers = new HttpHeaders();
		headers.setAccept(Arrays.asList(new MediaType[] { MediaType.APPLICATION_JSON }));
		headers.setContentType(MediaType.MULTIPART_FORM_DATA);
		headers.add(APIParams.API_LICENSE_KEY_NAME, APIParams.HTTP_LICENSEKEY);
		MultiValueMap<String, String> fileMap = null;
		HttpEntity<byte[]> fileEntity = null;
		ContentDisposition contentDisposition = null;

		MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
		body.add("Base64JsonRoot", commons.encodeStringBase64(Json.serializer().toString(root)));
		body.add("_id", _id);

		/* ADD DU LIEU XML DA KY */
		fileMap = new LinkedMultiValueMap<String, String>();
		contentDisposition = ContentDisposition.builder("form-data").name("XMLFileSigned").filename("bbdctt-signed.xml")
				.build();
		fileMap.add(HttpHeaders.CONTENT_DISPOSITION, contentDisposition.toString());
		fileEntity = new HttpEntity<byte[]>(multipartFile.getBytes(), fileMap);
		body.add("XMLFileSigned", fileEntity);

		HttpEntity<MultiValueMap<String, Object>> requestBody = new HttpEntity<>(body, headers);
		String url = "/lbbdctt-client/sign-single";
		ResponseEntity<MsgRsp> result = restTemplate.exchange(APIParams.HTTP_URI + url, HttpMethod.POST, requestBody,
				MsgRsp.class);
		/* END - CONNECT TO API */
		if (result.getStatusCode() == org.springframework.http.HttpStatus.OK) {
			MsgRsp rsp = result.getBody();
			MspResponseStatus rspStatus = rsp.getResponseStatus();
			if (rspStatus.getErrorCode() == 0) {
				dtoRes.setErrorCode(0);
				dtoRes.setResponseData(rsp.getObjData());
			} else {
				dtoRes = new BaseDTO(rspStatus.getErrorCode(), rspStatus.getErrorDesc());
			}
		} else {
			dtoRes = new BaseDTO(result.getStatusCode().value(), "Thực hiện ký thông báo hđ sai sót không thành công.");
		}
		return dtoRes;
	}

	@RequestMapping(value = {"/check-certcks" }, produces = MediaType.APPLICATION_JSON_VALUE, method = RequestMethod.POST)
	@ResponseBody
	public BaseDTO execCheckCertcks(Locale locale, HttpServletRequest req, HttpSession session,
			@RequestParam(value = "cert", required = false, defaultValue = "") String cert) throws Exception {
		BaseDTO dtoRes = new BaseDTO();

		if ("".equals(cert)) {
			dtoRes.setErrorCode(1);
			dtoRes.setResponseData("Chứng thư số không hợp lệ.");
			return dtoRes;
		}

		// LAY THONG TIN CERT - KIEM TRA SAU
		CertificateFactory certificateFactory = null;
		InputStream in = null;
		X509Certificate x509Cert = null;

		certificateFactory = CertificateFactory.getInstance("X.509");
		cert = cert.replaceAll("@", "+");
		in = new ByteArrayInputStream(DatatypeConverter.parseBase64Binary(cert));
		x509Cert = (X509Certificate) certificateFactory.generateCertificate(in);

		String serialNumber = x509Cert.getSerialNumber().toString(16);

		if (serialNumber.length() % 2 == 1)
			serialNumber = "0" + serialNumber;

		dtoRes.setErrorCode(0);
		HashMap<String, String> hR = new LinkedHashMap<String, String>();
		hR.put("TTChuc", commons.getAttributeCertificate(x509Cert.getIssuerDN().toString(), "O"));
		hR.put("Seri", serialNumber);
		hR.put("TNgay",
				commons.convertLocalDateTimeToString(commons.convertDateGMT7ToLocalDateTime(x509Cert.getNotBefore()),
						Constants.FORMAT_DATE.FORMAT_DATE_TIME_WEB));
		hR.put("DNgay",
				commons.convertLocalDateTimeToString(commons.convertDateGMT7ToLocalDateTime(x509Cert.getNotAfter()),
						Constants.FORMAT_DATE.FORMAT_DATE_TIME_WEB));
		dtoRes.setResponseData(hR);
		return dtoRes;
	}

	@RequestMapping(value = "/get-file-to-sign/{file-name:.+}", method = RequestMethod.GET)
	public void getFileToSign(HttpServletRequest req, HttpServletResponse resp, HttpSession session,
			@PathVariable(name = "file-name", required = true) String fileName) throws Exception {
		PrintWriter writer = null;

		String[] info = fileName.split("-");
		if (info.length != 2) {
			resp.setContentType("text/html; charset=utf-8");
			resp.setCharacterEncoding("UTF-8");
			resp.setHeader("success", "yes");
			resp.setStatus(HttpStatus.NOT_FOUND.value());
			writer = resp.getWriter();
			writer.write(HttpStatus.NOT_FOUND.getReasonPhrase());
			writer.close();
			return;
		}

		LocalDateTime now = LocalDateTime.now();
		LocalDateTime time = null;
		try {
			time = commons.convertStringToLocalDateTime(info[0], Constants.FORMAT_DATE.FORMAT_DATETIME_DB_FULL);
		} catch (Exception e) {
		}
		long diffTime = ChronoUnit.SECONDS.between(time, now);
		if (diffTime > SystemParams.TIME_OUT_DOWNLOAD_FILE) {
			resp.setContentType("text/html; charset=utf-8");
			resp.setCharacterEncoding("UTF-8");
			resp.setHeader("success", "yes");
			resp.setStatus(HttpStatus.FORBIDDEN.value());
			writer = resp.getWriter();
			writer.write(HttpStatus.FORBIDDEN.getReasonPhrase());
			writer.close();
			return;
		}

		Path path = Paths.get(SystemParams.DIR_TMP_SAVE_FILES, fileName);
		File file = path.toFile();
		if (!file.exists() || !file.isFile()) {
			resp.setContentType("text/html; charset=utf-8");
			resp.setCharacterEncoding("UTF-8");
			resp.setHeader("success", "yes");
			resp.setStatus(HttpStatus.NOT_FOUND.value());
			writer = resp.getWriter();
			writer.write(HttpStatus.NOT_FOUND.getReasonPhrase());
			writer.close();
			return;
		}

		InputStream is = new FileInputStream(file);
		resp.setContentType("application/force-download");
		resp.setHeader("Content-Disposition", "attachment; filename=" + fileName + "");
		int read = 0;
		byte[] bytes = new byte[SystemParams.BUFFER_SIZE];
		OutputStream os = resp.getOutputStream();

		while ((read = is.read(bytes)) != -1) {
			os.write(bytes, 0, read);
		}
		os.flush();
		os.close();
		is.close();

		return;
	}

	@RequestMapping(value = "/send-mail-check", produces = MediaType.APPLICATION_JSON_VALUE, method = RequestMethod.POST)
	@ResponseBody
	public BaseDTO execCheckDataToSave(Locale locale, HttpServletRequest req, HttpSession session) throws Exception {
		String token = "";
		if (null != session.getAttribute(Constants.SESSION_TYPE.SESSION_TOKEN_EXECUTE)) {
			token = session.getAttribute(Constants.SESSION_TYPE.SESSION_TOKEN_EXECUTE).toString();
			session.removeAttribute(token);
		}
		session.removeAttribute(Constants.SESSION_TYPE.SESSION_TOKEN_EXECUTE);

		BaseDTO dto = new BaseDTO();
		String id = commons.getParameterFromRequest(req, "_id").replaceAll("\\s", "");
		if (id.equals("")) {
			dto.setErrorCode(998);
			dto.setResponseData("Không tìm thấy chức năng giao dịch.");
			return dto;
		}

		token = commons.csRandomAlphaNumbericString(30);
		session.setAttribute(Constants.SESSION_TYPE.SESSION_TOKEN_EXECUTE, token);

		HashMap<String, String> hInfo = new HashMap<String, String>();
		hInfo.put("TOKEN", token);

		dto.setResponseData(hInfo);
		dto.setErrorCode(0);
		return dto;
	}

	@RequestMapping(value = "/send-mail-execute", produces = MediaType.APPLICATION_JSON_VALUE, method = RequestMethod.POST)
	@ResponseBody
	public BaseDTO execSaveData(HttpServletRequest req, HttpSession session,
			@RequestParam(value = "tokenTransaction", required = false, defaultValue = "") String tokenTransaction)
			throws Exception {
		BaseDTO dtoRes = new BaseDTO(req);
		String token = session.getAttribute(Constants.SESSION_TYPE.SESSION_TOKEN_EXECUTE) == null ? ""
				: session.getAttribute(Constants.SESSION_TYPE.SESSION_TOKEN_EXECUTE).toString();
		session.removeAttribute(Constants.SESSION_TYPE.SESSION_TOKEN_EXECUTE);
		if ("".equals(token) || !tokenTransaction.equals(token)) {
			dtoRes.setErrorCode(1);
			dtoRes.setResponseData("Token giao dịch không hợp lệ.");
			return dtoRes;
		}
		String id = commons.getParameterFromRequest(req, "_id").replaceAll("\\s", "");
		Msg msg = dtoRes.createMsgPass();
		HashMap<String, Object> hData = new HashMap<>();
		hData.put("_id", id);
		msg.setObjData(hData);
		JSONRoot root = new JSONRoot(msg);

		MsgRsp rsp = restAPI.callAPIPNotAuth("/lbbdctt-client/send-mail", HttpMethod.POST, root);
		MspResponseStatus rspStatus = rsp.getResponseStatus();
		if (rspStatus.getErrorCode() == 0) {
			dtoRes.setErrorCode(0);
			dtoRes.setResponseData("Gửi mail thành công.");
		} else {
			dtoRes.setErrorCode(rspStatus.getErrorCode());
			dtoRes.setResponseData(rspStatus.getErrorDesc());
		}
		return dtoRes;
	}
	
	@RequestMapping(value = "/download-plugin", method = RequestMethod.GET)
	public void getFile(HttpServletRequest req, HttpServletResponse resp, HttpSession session) throws Exception {
		JSONRoot root = new JSONRoot();
		BaseDTO dtoRes = new BaseDTO();
		Msg msg = dtoRes.createMsgPass();
		HashMap<String, String> hInput = new HashMap<>();
		msg.setObjData(hInput);
		root = new JSONRoot(msg);
		MsgRsp rsp = restAPI.callAPIPass("/support/getList", HttpMethod.POST, root);
		MspResponseStatus rspStatus = rsp.getResponseStatus();

		JsonNode row = null;
		if (rspStatus.getErrorCode() == 0) {
			JsonNode jsonData = Json.serializer().nodeFromObject(rsp.getObjData());
			if (!jsonData.at("/rows").isMissingNode()) {
				row = jsonData.at("/rows").get(0);
			}
		}

		String name = commons.getTextJsonNode(row.at("/ImageLogoOriginalFilename"));
		String fileName = commons.getTextJsonNode(row.at("/ImageLogo"));

		PrintWriter writer = null;
		Path path = Paths.get(SystemParams.DIR_E_INVOICE_DATA + "/support", fileName);
		File file = path.toFile();
		if (!file.exists() || !file.isFile()) {
			resp.setContentType("text/html; charset=utf-8");
			resp.setCharacterEncoding("UTF-8");
			resp.setHeader("success", "yes");
			resp.setStatus(HttpStatus.NOT_FOUND.value());
			writer = resp.getWriter();
			writer.write(HttpStatus.NOT_FOUND.getReasonPhrase());
			writer.close();
			return;
		}

		InputStream is = new FileInputStream(file);
		resp.setContentType("application/force-download");
		resp.setHeader("Content-Disposition", "attachment; filename=" + name + "");
		int read = 0;
		byte[] bytes = new byte[SystemParams.BUFFER_SIZE];
		OutputStream os = resp.getOutputStream();

		while ((read = is.read(bytes)) != -1) {
			os.write(bytes, 0, read);
		}
		os.flush();
		os.close();
		is.close();
		File file_delete = new File(SystemParams.DIR_E_INVOICE_TEMPORARY + "/" + fileName);
		file_delete.delete();

		return;
	}
}
