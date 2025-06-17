package vn.sesgroup.hddt.controller.xulyhoadon;

import java.io.File;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
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
import com.api.message.MsgRsp;
import com.api.message.MspResponseStatus;
import com.fasterxml.jackson.databind.JsonNode;

import vn.sesgroup.hddt.controller.AbstractController;
import vn.sesgroup.hddt.dto.BaseDTO;
import vn.sesgroup.hddt.dto.CurrentUserProfile;
import vn.sesgroup.hddt.dto.FileInfo;
import vn.sesgroup.hddt.dto.IssuerInfo;
import vn.sesgroup.hddt.resources.APIParams;
import vn.sesgroup.hddt.resources.RestAPIUtility;
import vn.sesgroup.hddt.utils.Constants;
import vn.sesgroup.hddt.utils.Json;
import vn.sesgroup.hddt.utils.SystemParams;

@Controller
@RequestMapping({ "/lbbdctt-cre", "/lbbdctt-detail", "/lbbdctt-edit", "/lbbdctt-sign", "/lbbdctt-del" })
@Scope(value = WebApplicationContext.SCOPE_REQUEST)
public class LBBDCTTheCRUDController extends AbstractController {
	private static final Logger log = LogManager.getLogger(LBBDCTTheCRUDController.class);
	@Autowired
	RestAPIUtility restAPI;
	@Autowired
	RestTemplate restTemplate;

	private String errorCode;
	private String errorDesc;
	private String _id;

	private String nb_mst;
	private String nb_dvbh;
	private String nb_dc;
	private String nb_dd;
	private String nb_cv;
	private String nb_email_receive;

	private String nm_mst;
	private String nm_dvmh;
	private String nm_dc;
	private String nm_dd;
	private String nm_cv;
	private String nm_email_send;

	private String loaibb;
	private String sbban;
	private String ndsai;
	private String nddung;
	private String ndtnhat;

	private String dsHD;
	private String dsHDNew;

	@RequestMapping(value = "/init", method = { RequestMethod.POST })
	public String init(Locale locale, HttpServletRequest req, HttpSession session,
			@RequestAttribute(name = "transaction", value = "", required = false) String transaction) throws Exception {
		errorCode = "";
		errorDesc = "";

		CurrentUserProfile cup = getCurrentlyAuthenticatedPrincipal();
		IssuerInfo ii = cup.getLoginRes().getIssuerInfo();

		req.setAttribute("NB_MSThue", ii.getTaxCode());

		_id = commons.getParameterFromRequest(req, "_id");
		String header = "Tạo biên bản điều chỉnh/thay thế";
		String action = "CREATE";
		boolean isEdit = false;

		switch (transaction) {
		case "lbbdctt-cre":
			header = "Tạo biên bản điều chỉnh/thay thế";
			action = "CREATE";
			isEdit = true;
			break;
		case "lbbdctt-edit":
			header = "Thay đổi biên bản điều chỉnh/thay thế";
			action = "EDIT";
			isEdit = true;
			break;
		case "lbbdctt-detail":
			header = "Chi tiết biên bản điều chỉnh/thay thế";
			action = "DETAIL";
			isEdit = false;
			break;
		case "lbbdctt-sign":
			header = "Ký thông biên bản điều chỉnh/thay thế";
			action = "SIGN";
			isEdit = false;
			break;
		default:
			break;
		}

		if ("|lbbdctt-edit|lbbdctt-sign|lbbdctt-detail|".indexOf(transaction) != -1)
			inquiry(cup, locale, req, session, _id, action);

		req.setAttribute("_header_", header);
		req.setAttribute("_action_", action);
		req.setAttribute("_isedit_", isEdit);
		req.setAttribute("_id", _id);

		req.setAttribute("map_loaibb", Constants.MAP_LOAIBB_LBBDCTT);
		if (!"".equals(errorDesc))
			req.setAttribute("messageError", errorDesc);

		return "bbdctthe/bbdctthe-crud";
	}

	private void inquiry(CurrentUserProfile cup, Locale locale, HttpServletRequest req, HttpSession session, String _id,
			String action) throws Exception {
		if ("".equals(_id)) {
			errorCode = "NOT FOUND";
			errorDesc = "Không tìm thấy thông tin thông báo.";
			return;
		}

		BaseDTO baseDTO = new BaseDTO(req);
		Msg msg = baseDTO.createMsg(cup, Constants.MSG_ACTION_CODE.INQUIRY);
		HashMap<String, String> hData = new HashMap<>();
		msg.setObjData(hData);

		JSONRoot root = new JSONRoot(msg);
		MsgRsp rsp = restAPI.callAPINormal("/lbbdctt/detail/" + _id, cup.getLoginRes().getToken(), HttpMethod.POST,
				root);
		MspResponseStatus rspStatus = rsp.getResponseStatus();
		if (rspStatus.getErrorCode() == 0) {
			JsonNode jsonData = Json.serializer().nodeFromObject(rsp.getObjData());

			req.setAttribute("NB_MSThue", commons.getTextJsonNode(jsonData.at("/TTNBan/MSThue")));
			req.setAttribute("NB_DVBHang", commons.getTextJsonNode(jsonData.at("/TTNBan/DVBHang")));
			req.setAttribute("NB_DChi", commons.getTextJsonNode(jsonData.at("/TTNBan/DChi")));
			req.setAttribute("NB_DDien", commons.getTextJsonNode(jsonData.at("/TTNBan/DDien")));
			req.setAttribute("NB_CVu", commons.getTextJsonNode(jsonData.at("/TTNBan/CVu")));
			req.setAttribute("Email_Receive", commons.getTextJsonNode(jsonData.at("/TTNBan/EReceive")));

			req.setAttribute("NM_MSThue", commons.getTextJsonNode(jsonData.at("/TTNMua/MSThue")));
			req.setAttribute("NM_DVMHang", commons.getTextJsonNode(jsonData.at("/TTNMua/DVMHang")));
			req.setAttribute("NM_DChi", commons.getTextJsonNode(jsonData.at("/TTNMua/DChi")));
			req.setAttribute("NM_DDien", commons.getTextJsonNode(jsonData.at("/TTNMua/DDien")));
			req.setAttribute("NM_CVu", commons.getTextJsonNode(jsonData.at("/TTNMua/CVu")));
			req.setAttribute("Email_Send", commons.getTextJsonNode(jsonData.at("/TTNMua/ESend")));

			req.setAttribute("loai_bb", commons.getTextJsonNode(jsonData.at("/Loai")));
			req.setAttribute("SBBan", commons.getTextJsonNode(jsonData.at("/SBBan")));
			req.setAttribute("NDSai", commons.getTextJsonNode(jsonData.at("/NDSai")));
			req.setAttribute("NDDung", commons.getTextJsonNode(jsonData.at("/NDDung")));
			req.setAttribute("NDTNhat", commons.getTextJsonNode(jsonData.at("/NDTNhat")));

			List<Object> dshdon = null;
			HashMap<String, String> hItem = null;
			if (!jsonData.at("/HDSSot").isMissingNode()) {
				dshdon = new ArrayList<Object>();
				hItem = new LinkedHashMap<String, String>();

				hItem.put("MSHDon", commons.getTextJsonNode(jsonData.at("/HDSSot/KHMSHDon"))
						+ commons.getTextJsonNode(jsonData.at("/HDSSot/KHHDon")));
				hItem.put("SHDon",
						commons.formatNumberBillInvoice(commons.getTextJsonNode(jsonData.at("/HDSSot/SHDon"))));
				hItem.put("Ngay",
						commons.convertLocalDateTimeToString(
								commons.convertLongToLocalDate(jsonData.at("/HDSSot/NLap").asLong()),
								Constants.FORMAT_DATE.FORMAT_DATE_WEB));
				hItem.put("MCQTCap", commons.getTextJsonNode(jsonData.at("/HDSSot/MCCQT")));
				hItem.put("TNMHang", commons.getTextJsonNode(jsonData.at("/HDSSot/TNMua")));
				hItem.put("TgTCThue", commons.getTextJsonNode(jsonData.at("/HDSSot/TgTCThue")));
				hItem.put("TgTThue", commons.getTextJsonNode(jsonData.at("/HDSSot/TgTThue")));
				hItem.put("TgTTTBSo", commons.getTextJsonNode(jsonData.at("/HDSSot/TgTTTBSo")));
				dshdon.add(hItem);
				req.setAttribute("DSHDon", commons.encodeStringBase64(Json.serializer().toString(dshdon)));
			}
			if (!jsonData.at("/HDDCTThe").isMissingNode()) {
				dshdon = new ArrayList<Object>();
				hItem = new LinkedHashMap<String, String>();

				hItem.put("MSHDon_N", commons.getTextJsonNode(jsonData.at("/HDDCTThe/KHMSHDon"))
						+ commons.getTextJsonNode(jsonData.at("/HDDCTThe/KHHDon")));
				hItem.put("SHDon_N",
						commons.formatNumberBillInvoice(commons.getTextJsonNode(jsonData.at("/HDDCTThe/SHDon"))));
				hItem.put("Ngay_N",
						commons.convertLocalDateTimeToString(
								commons.convertLongToLocalDate(jsonData.at("/HDDCTThe/NLap").asLong()),
								Constants.FORMAT_DATE.FORMAT_DATE_WEB));
				hItem.put("MCQTCap_N", commons.getTextJsonNode(jsonData.at("/HDDCTThe/MCCQT")));
				hItem.put("TNMHang_N", commons.getTextJsonNode(jsonData.at("/HDDCTThe/TNMua")));
				hItem.put("TgTCThue_N", commons.getTextJsonNode(jsonData.at("/HDDCTThe/TgTCThue")));
				hItem.put("TgTThue_N", commons.getTextJsonNode(jsonData.at("/HDDCTThe/TgTThue")));
				hItem.put("TgTTTBSo_N", commons.getTextJsonNode(jsonData.at("/HDDCTThe/TgTTTBSo")));
				dshdon.add(hItem);
				req.setAttribute("DSHDon1", commons.encodeStringBase64(Json.serializer().toString(dshdon)));
			}
		} else {
			errorDesc = rspStatus.getErrorDesc();
		}
	}

	private void validateRequired(BaseDTO dto, String value, String message) {
		if (value == null || value.trim().isEmpty()) {
			dto.setErrorCode(1);
			dto.getErrorMessages().add(message);
		}
	}

	public BaseDTO checkDataToAccept(HttpServletRequest req, HttpSession session, String transaction,
			CurrentUserProfile cup) throws Exception {
		BaseDTO dto = new BaseDTO();
		dto.setErrorCode(0);

		_id = commons.getParameterFromRequest(req, "_id").replaceAll("\\s", "");
		nb_mst = commons.getParameterFromRequest(req, "nb_mst").replaceAll("\\s", "");
		nb_dvbh = commons.getParameterFromRequest(req, "nb_dvbh").replaceAll("\\s+", " ");
		nb_dc = commons.getParameterFromRequest(req, "nb_dc").replaceAll("\\s+", " ");
		nb_dd = commons.getParameterFromRequest(req, "nb_dd").replaceAll("\\s+", " ");
		nb_cv = commons.getParameterFromRequest(req, "nb_cv").replaceAll("\\s+", " ");
		nb_email_receive = commons.getParameterFromRequest(req, "email_receive").replaceAll("\\s", "");

		nm_mst = commons.getParameterFromRequest(req, "nm_mst").replaceAll("\\s", "");
		nm_dvmh = commons.getParameterFromRequest(req, "nm_dvmh").replaceAll("\\s+", " ");
		nm_dc = commons.getParameterFromRequest(req, "nm_dc").replaceAll("\\s+", " ");
		nm_dd = commons.getParameterFromRequest(req, "nm_dd").replaceAll("\\s+", " ");
		nm_cv = commons.getParameterFromRequest(req, "nm_cv").replaceAll("\\s+", " ");
		nm_email_send = commons.getParameterFromRequest(req, "email_send").replaceAll("\\s", "");

		loaibb = commons.getParameterFromRequest(req, "loaibb").replaceAll("\\s", "");
		sbban = commons.getParameterFromRequest(req, "sbban").replaceAll("\\s", "");
		ndsai = commons.getParameterFromRequest(req, "ndsai").replaceAll("\\s+", " ");
		nddung = commons.getParameterFromRequest(req, "nddung").replaceAll("\\s+", " ");
		ndtnhat = commons.getParameterFromRequest(req, "ndtnhat").replaceAll("\\s+", " ");

		dsHD = commons.getParameterFromRequest(req, "ds-hd").replaceAll("\\s", "");
		dsHDNew = commons.getParameterFromRequest(req, "ds-hd-new").replaceAll("\\s", "");
		switch (transaction) {
		case "lbbdctt-cre":
		case "lbbdctt-edit":
			validateRequired(dto, nb_mst, "Vui lòng nhập MST người bán.");
			validateRequired(dto, nb_dvbh, "Vui lòng nhập đơn vị bán hàng người bán.");
			validateRequired(dto, nb_dc, "Vui lòng nhập địa chỉ người bán.");
			validateRequired(dto, nb_dd, "Vui lòng nhập đại diện người bán.");
			validateRequired(dto, nb_cv, "Vui lòng nhập chức vụ người bán.");
			validateRequired(dto, nb_email_receive, "Vui lòng nhập email nhận.");

			validateRequired(dto, nm_mst, "Vui lòng nhập MST người mua.");
			validateRequired(dto, nm_dvmh, "Vui lòng nhập đơn vị mua hàng người mua.");
			validateRequired(dto, nm_dc, "Vui lòng nhập địa chỉ người mua.");
			validateRequired(dto, nm_dd, "Vui lòng nhập đại diện người mua.");
			validateRequired(dto, nm_cv, "Vui lòng nhập chức vụ người mua.");
			validateRequired(dto, nm_email_send, "Vui lòng nhập email gửi.");

			validateRequired(dto, loaibb, "Vui lòng chọn loại biên bản.");
			validateRequired(dto, sbban, "Vui lòng chọn loại biên bản.");
			validateRequired(dto, ndsai, "Vui lòng nhập nội dung sai.");
			validateRequired(dto, nddung, "Vui lòng nhập nội dung đúng.");
			validateRequired(dto, ndtnhat, "Vui lòng nhập nội dung thống nhất.");
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
		String messageConfirm = "Bạn có muốn tạo biên bản điều chỉnh/thay thế không?";
		switch (transaction) {
		case "lbbdctt-cre":
			messageConfirm = "Bạn có muốn tạo biên bản điều chỉnh/thay thế không?";
			break;
		case "lbbdctt-edit":
			messageConfirm = "Bạn có muốn thay đổi biên bản điều chỉnh/thay thế không?";
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

		JsonNode jsonNodeTmp = null;
		try {
			jsonNodeTmp = Json.serializer().nodeFromJson(commons.decodeBase64ToString(dsHD));
		} catch (Exception e) {
			log.error(" >>>>> An exception occurred!", e);
		}

		if (null == jsonNodeTmp || jsonNodeTmp.size() == 0) {
			dto.setErrorCode(999);
			dto.setResponseData("Vui lòng chọn danh sách hóa đơn để thực hiện.");
			return dto;
		}

		/* KIEM TRA THONG TIN */
		boolean check = true;
		JsonNode jsonNode = jsonNodeTmp.get(0);
		if ("".equals(commons.getTextJsonNode(jsonNode.at("/MCQTCap")))
				|| "".equals(commons.getTextJsonNode(jsonNode.at("/SHDon")))
				|| "".equals(commons.getTextJsonNode(jsonNode.at("/MSHDon")))) {
			check = false;
		}

		if (!check) {
			dto.setErrorCode(999);
			dto.setResponseData("Vui lòng kiểm tra lại dữ liệu hóa đơn cần điều chỉnh/thay thế.");
			return dto;
		}

		jsonNodeTmp = null;
		jsonNode = null;
		check = true;
		try {
			jsonNodeTmp = Json.serializer().nodeFromJson(commons.decodeBase64ToString(dsHDNew));
		} catch (Exception e) {
			log.error(" >>>>> An exception occurred!", e);
		}

		if (null == jsonNodeTmp || jsonNodeTmp.size() == 0) {
			dto.setErrorCode(999);
			dto.setResponseData("Vui lòng chọn danh sách hóa đơn để thực hiện.");
			return dto;
		}

		jsonNode = jsonNodeTmp.get(0);
		if ("".equals(commons.getTextJsonNode(jsonNode.at("/MCQTCap")))
				|| "".equals(commons.getTextJsonNode(jsonNode.at("/SHDon")))
				|| "".equals(commons.getTextJsonNode(jsonNode.at("/MSHDon")))) {
			check = false;
		}

		if (!check) {
			dto.setErrorCode(999);
			dto.setResponseData("Vui lòng kiểm tra lại dữ liệu hóa đơn mới.");
			return dto;
		}
		/* END - KIEM TRA THONG TIN */

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
		/* END: CHECK TOKEN */
		JsonNode jsonNodeTmp = null;
		try {
			jsonNodeTmp = Json.serializer().nodeFromJson(commons.decodeBase64ToString(dsHD));
		} catch (Exception e) {
			log.error(" >>>>> An exception occurred!", e);
		}
		if (null == jsonNodeTmp || jsonNodeTmp.size() == 0) {
			dtoRes.setErrorCode(999);
			dtoRes.setResponseData("Vui lòng chọn hóa đơn để thực hiện.");
			return dtoRes;
		}

		JsonNode jsonNodeTmp1 = null;
		try {
			jsonNodeTmp1 = Json.serializer().nodeFromJson(commons.decodeBase64ToString(dsHDNew));
		} catch (Exception e) {
			log.error(" >>>>> An exception occurred!", e);
		}
		if (null == jsonNodeTmp1 || jsonNodeTmp1.size() == 0) {
			dtoRes.setErrorCode(999);
			dtoRes.setResponseData("Vui lòng chọn hóa đơn để thực hiện.");
			return dtoRes;
		}

		String actionCode = Constants.MSG_ACTION_CODE.CREATED;
		switch (transaction) {
		case "lbbdctt-cre":
			actionCode = Constants.MSG_ACTION_CODE.CREATED;
			break;
		case "lbbdctt-edit":
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
		hData.put("NB_MSThue", nb_mst);
		hData.put("NB_DVBHang", nb_dvbh);
		hData.put("NB_DChi", nb_dc);
		hData.put("NB_DDien", nb_dd);
		hData.put("NB_CVu", nb_cv);
		hData.put("Email_Recive", nb_email_receive);

		hData.put("NM_MSThue", nm_mst);
		hData.put("NM_DVMHang", nm_dvmh);
		hData.put("NM_DChi", nm_dc);
		hData.put("NM_DDien", nm_dd);
		hData.put("NM_CVu", nm_cv);
		hData.put("Email_Send", nm_email_send);

		hData.put("LBBan", loaibb);
		hData.put("SBBan", sbban);
		hData.put("NDSai", ndsai);
		hData.put("NDDung", nddung);
		hData.put("NDTNhat", ndtnhat);

		hData.put("HDon", jsonNodeTmp.get(0));
		hData.put("HDNew", jsonNodeTmp1.get(0));
		msg.setObjData(hData);
		JSONRoot root = new JSONRoot(msg);
		MsgRsp rsp = restAPI.callAPINormal("/lbbdctt/crud", cup.getLoginRes().getToken(), HttpMethod.POST, root);
		MspResponseStatus rspStatus = rsp.getResponseStatus();
		if (rspStatus.getErrorCode() == 0) {
			dtoRes.setErrorCode(0);
			switch (transaction) {
			case "lbbdctt-cre":
				dtoRes.setResponseData("Tạo biên bản điều chỉnh/thay thế thành công.");
				break;
			case "lbbdctt-edit":
				dtoRes.setResponseData("Cập nhật biên bản điều chỉnh/thay thế thành công.");
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

	@RequestMapping(value = "/check-data-sign", produces = MediaType.APPLICATION_JSON_VALUE, method = RequestMethod.POST)
	@ResponseBody
	public BaseDTO execCheckDataToSign(Locale locale, HttpServletRequest req, HttpSession session,
			@RequestAttribute(name = "transaction", value = "", required = false) String transaction) throws Exception {
		String token = "";
		if (null != session.getAttribute(Constants.SESSION_TYPE.SESSION_TOKEN_EXECUTE)) {
			token = session.getAttribute(Constants.SESSION_TYPE.SESSION_TOKEN_EXECUTE).toString();
			session.removeAttribute(token);
		}
		session.removeAttribute(Constants.SESSION_TYPE.SESSION_TOKEN_EXECUTE);

		CurrentUserProfile cup = getCurrentlyAuthenticatedPrincipal();
		BaseDTO dto = new BaseDTO();
		_id = commons.getParameterFromRequest(req, "_id").replaceAll("\\s", "");

		if (!"lbbdctt-sign".equals(transaction)) {
			dto = new BaseDTO();
			dto.setErrorCode(998);
			dto.setResponseData("Chức năng giao dịch không hợp lệ.");
			return dto;
		}

		if ("".equals(_id)) {
			dto.setErrorCode(1);
			dto.setResponseData("Không tìm thấy thông báo hđ sai sót.");
			return dto;
		}

		/* LAY THONG TIN DU LIEU XML VE SERVER WEB */
		dto = new BaseDTO(req);
		Msg msg = dto.createMsg(cup, Constants.MSG_ACTION_CODE.CREATED);
		HashMap<String, Object> hData = new HashMap<>();
		hData.put("_id", _id);
		msg.setObjData(hData);
		JSONRoot root = new JSONRoot(msg);

		FileInfo fileInfo = restAPI.callAPIGetFileInfo("/lbbdctt/get-file-for-sign", cup.getLoginRes().getToken(),
				HttpMethod.POST, root);
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

		CurrentUserProfile cup = getCurrentlyAuthenticatedPrincipal();
		BaseDTO dtoRes = new BaseDTO(req);
		Msg msg = dtoRes.createMsg(cup, Constants.MSG_ACTION_CODE.SIGNED);
		HashMap<String, Object> hData = new HashMap<>();

		msg.setObjData(hData);
		JSONRoot root = new JSONRoot(msg);

//		FileCopyUtils.copy(multipartFile.getBytes(), Paths.get(SystemParams.DIR_TMP_SAVE_FILES, "tmp.xml").toFile());

		/* CONNECT TO API */
		HttpHeaders headers = new HttpHeaders();
		headers.setAccept(Arrays.asList(new MediaType[] { MediaType.APPLICATION_JSON }));
		headers.setContentType(MediaType.MULTIPART_FORM_DATA);
		headers.add(APIParams.API_LICENSE_KEY_NAME, APIParams.HTTP_LICENSEKEY);
		headers.add(Constants.TOKEN_HEADER, cup.getLoginRes().getToken());

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
		String url = "/lbbdctt/sign-single";
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

	@RequestMapping(value = "/check-data-delete", produces = MediaType.APPLICATION_JSON_VALUE, method = RequestMethod.POST)
	@ResponseBody
	public BaseDTO checkDataToDelete(Locale locale, HttpServletRequest req, HttpSession session,
			@RequestAttribute(name = "transaction", required = false, value = "") String transaction) throws Exception {
		String token = "";
		if (null != session.getAttribute(Constants.SESSION_TYPE.SESSION_TOKEN_EXECUTE)) {
			token = session.getAttribute(Constants.SESSION_TYPE.SESSION_TOKEN_EXECUTE).toString();
			session.removeAttribute(token);
		}
		session.removeAttribute(Constants.SESSION_TYPE.SESSION_TOKEN_EXECUTE);
		BaseDTO dto = new BaseDTO();

		_id = commons.getParameterFromRequest(req, "_id").replaceAll("\\s", "");

		if ("".equals(_id)) {
			dto.setErrorCode(999);
			dto.getErrorMessages().add("Không tìm thấy thông tin biên bản điều chỉnh thay thế.");
			return dto;
		}

		String messageConfirm = "";
		switch (transaction) {
		case "lbbdctt-del":
			messageConfirm = "Bạn có muốn tạo xóa biên bản điều chỉnh/thay thế không?";
			break;
		default:
			dto = new BaseDTO();
			dto.setErrorCode(998);
			dto.setResponseData("Không tìm thấy chức năng giao dịch.");
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

	@RequestMapping(value = "/delete-data", produces = MediaType.APPLICATION_JSON_VALUE, method = RequestMethod.POST)
	@ResponseBody
	public BaseDTO deleteData(Locale locale, HttpServletRequest req, HttpSession session,
			@RequestAttribute(name = "transaction", value = "", required = false) String transaction,
			@RequestParam(value = "tokenTransaction", required = false, defaultValue = "") String tokenTransaction)
			throws Exception {
		BaseDTO dto = new BaseDTO();

		CurrentUserProfile cup = getCurrentlyAuthenticatedPrincipal();
		_id = commons.getParameterFromRequest(req, "_id").replaceAll("\\s", "");
		/* CHECK TOKEN */
		String token = session.getAttribute(Constants.SESSION_TYPE.SESSION_TOKEN_EXECUTE) == null ? ""
				: session.getAttribute(Constants.SESSION_TYPE.SESSION_TOKEN_EXECUTE).toString();
		session.removeAttribute(Constants.SESSION_TYPE.SESSION_TOKEN_EXECUTE);
		if ("".equals(token) || !tokenTransaction.equals(token)) {
			dto.setErrorCode(1);
			dto.setResponseData("Token giao dịch không hợp lệ.");
			return dto;
		}

		String actionCode = Constants.MSG_ACTION_CODE.DELETE;
		dto = new BaseDTO(req);
		Msg msg = dto.createMsg(cup, actionCode);
		HashMap<String, Object> hData = new HashMap<>();
		hData.put("_id", _id);

		msg.setObjData(hData);

		JSONRoot root = new JSONRoot(msg);
		MsgRsp rsp = restAPI.callAPINormal("/lbbdctt/crud", cup.getLoginRes().getToken(), HttpMethod.POST, root);
		MspResponseStatus rspStatus = rsp.getResponseStatus();
		if (rspStatus.getErrorCode() == 0) {
			dto.setErrorCode(0);
			dto.setResponseData("Xóa biên bản thành công.");
		} else {
			dto.setErrorCode(rspStatus.getErrorCode());
			dto.setResponseData(rspStatus.getErrorDesc());
		}
		return dto;
	}

}
