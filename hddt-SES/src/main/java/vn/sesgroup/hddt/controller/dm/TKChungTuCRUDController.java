package vn.sesgroup.hddt.controller.dm;

import java.io.File;
import java.time.LocalDate;
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
import com.api.message.MsgParam;
import com.api.message.MsgParams;
import com.api.message.MsgRsp;
import com.api.message.MspResponseStatus;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;

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
@RequestMapping({ 
	"/tkctu-cre",
	"/tkctu-edit",
	"/tkctu-detail",
	"/tkctu-copy",
	"/tkctu-sign",
	"/tkctu-del" 
})
@Scope(value = WebApplicationContext.SCOPE_REQUEST)
public class TKChungTuCRUDController extends AbstractController {
	private static final Logger log = LogManager.getLogger(TKChungTuCRUDController.class);
	@Autowired
	RestAPIUtility restAPI;
	@Autowired
	RestTemplate restTemplate;

	private String errorCode;
	private String errorDesc;
	private String _id;
	private String mst;
	private String tennnt;
	private String nlhe;
	private String dclhe;
	private String dctdtu;
	private String dtlhe;

	private String mauSo;
	private String ten;
	private String tinhThanh;
	private String cqtqly;
	private String nlap;
	private String hthuc;

	private String tccnnphanh;
	private String cqtphanh;

	private String pthuc;

	private String lctsdung_TNCNhan;
	private String lctsdung_TMDTu;
	private String lctsdung_BLKIMGia;
	private String lctsdung_BLIMGia;
	private String lctsdung_BLCTT50;
	private String dsctsSDung;

	private void LoadParameter(CurrentUserProfile cup, Locale locale, HttpServletRequest req, String action) {
		try {
			BaseDTO baseDTO = new BaseDTO(req);
			Msg msg = baseDTO.createMsg(cup, Constants.MSG_ACTION_CODE.LOAD_PARAMS);

			/* DANH SACH THAM SO */
			HashMap<String, String> hashConds = null;
			ArrayList<HashMap<String, String>> conds = null;
			MsgParam msgParam = null;
			MsgParams msgParams = new MsgParams();

			msgParam = new MsgParam();
			msgParam.setId("param01");
			msgParam.setParam("DMTinhThanh");
			msgParams.getParams().add(msgParam);

			msgParam = new MsgParam();
			msgParam.setId("param02");
			msgParam.setParam("DMChiCucThue");
			conds = new ArrayList<>();
			hashConds = new HashMap<>();
			hashConds.put("cond", "tinhthanh_ma");
			hashConds.put("condval", null == tinhThanh ? "" : tinhThanh);
			conds.add(hashConds);
			msgParam.setConds(conds);
			msgParams.getParams().add(msgParam);

			msgParam = new MsgParam();
			msgParam.setId("param03");
			msgParam.setParam("DMMSTKhai");
			msgParams.getParams().add(msgParam);

			msgParam = new MsgParam();
			msgParam.setId("param04");
			msgParam.setParam("DMHTTKhai");
			msgParams.getParams().add(msgParam);

			/* END: DANH SACH THAM SO */
			msg.setObjData(msgParams);

			JSONRoot root = new JSONRoot(msg);
			MsgRsp rsp = restAPI.callAPINormal("/commons/get-full-params", cup.getLoginRes().getToken(),
					HttpMethod.POST, root);
			MspResponseStatus rspStatus = rsp.getResponseStatus();

			if (rspStatus.getErrorCode() == 0 && rsp.getObjData() != null) {
				LinkedHashMap<String, String> hItem = null;

				JsonNode jsonData = Json.serializer().nodeFromObject(rsp.getObjData());
				if (null != jsonData.at("/param01") && jsonData.at("/param01") instanceof ArrayNode) {
					hItem = new LinkedHashMap<String, String>();
					for (JsonNode o : jsonData.at("/param01")) {
						hItem.put(commons.getTextJsonNode(o.get("code")), commons.getTextJsonNode(o.get("name")));
					}
					req.setAttribute("map_dmtinhthanh", hItem);
				}
				if (null != jsonData.at("/param02") && jsonData.at("/param02") instanceof ArrayNode) {
					hItem = new LinkedHashMap<String, String>();
					for (JsonNode o : jsonData.at("/param02")) {
                        String name = commons.getTextJsonNode(o.get("name")) + " ("+commons.getTextJsonNode(o.at("/note"))+")";
						hItem.put(commons.getTextJsonNode(o.get("code")), name);
					}
					req.setAttribute("map_cucthue", hItem);
				}

				if (null != jsonData.at("/param03") && jsonData.at("/param03") instanceof ArrayNode) {
					hItem = new LinkedHashMap<String, String>();
					for (JsonNode o : jsonData.at("/param03")) {
						hItem.put(commons.getTextJsonNode(o.get("code")), commons.getTextJsonNode(o.get("name")));
					}
					req.setAttribute("map_mstkhai", hItem);
				}
				if (null != jsonData.at("/param04") && jsonData.at("/param04") instanceof ArrayNode) {
					hItem = new LinkedHashMap<String, String>();
					for (JsonNode o : jsonData.at("/param04")) {
						hItem.put(commons.getTextJsonNode(o.get("code")), commons.getTextJsonNode(o.get("name")));
					}
					req.setAttribute("map_httkhai", hItem);
				}

			}
		} catch (Exception e) {
		}
	}

	@RequestMapping(value = "/init", method = { RequestMethod.POST })
	public String init(Locale locale, HttpServletRequest req, HttpSession session,
			@RequestAttribute(name = "transaction", value = "", required = false) String transaction) throws Exception {
		errorCode = "";
		errorDesc = "";

		CurrentUserProfile cup = getCurrentlyAuthenticatedPrincipal();
		IssuerInfo ii = cup.getLoginRes().getIssuerInfo();

		LocalDate now = LocalDate.now();
		req.setAttribute("tenNnt", ii.getName());
		req.setAttribute("ten", "Tờ khai đăng ký/thay đổi thông tin sử dụng chứng từ điện tử");
		req.setAttribute("nLap", commons.convertLocalDateTimeToString(now, Constants.FORMAT_DATE.FORMAT_DATE_WEB));
		req.setAttribute("mst", ii.getTaxCode());

		req.setAttribute("optPThuc", "HTGDLieu_TNCN");
		req.setAttribute("LHDSDung_HDGTGT", true);
		req.setAttribute("TCCNPHanh", true);

		_id = commons.getParameterFromRequest(req, "_id");
		String header = "Đăng ký tờ khai";
		String action = "CREATE";
		boolean isEdit = false;

		switch (transaction) {
		case "tkctu-cre":
			header = "Đăng ký tờ khai chứng từ";
			action = "CREATE";
			isEdit = true;
			req.setAttribute("MSCode", "01/ĐKTĐ-CTĐT");
			break;
		case "tkctu-edit":
			header = "Thay đổi thông tin tờ khai";
			action = "EDIT";
			isEdit = true;
			break;
		case "tkctu-detail":
			header = "Chi tiết tờ khai";
			action = "DETAIL";
			isEdit = false;
			break;
		case "tkctu-copy":
			header = "Copy tờ khai";
			action = "COPY";
			isEdit = true;
			req.setAttribute("HThuc", "2");
			break;
		case "tkctu-sign":
			header = "Ký tờ khai";
			action = "SIGN";
			isEdit = false;
			break;
		default:
			break;
		}

		if ("|tkctu-detail|tkctu-copy|tkctu-sign|tkctu-edit|".indexOf(transaction) != -1)
			inquiry(cup, locale, req, session, _id, action);

		req.setAttribute("_header_", header);
		req.setAttribute("_action_", action);
		req.setAttribute("_isedit_", isEdit);
		req.setAttribute("_id", _id);

		LoadParameter(cup, locale, req, action);

		if (!"".equals(errorDesc))
			req.setAttribute("messageError", errorDesc);

		return "dm/tkchungtu-crud";
	}

	private void inquiry(CurrentUserProfile cup, Locale locale, HttpServletRequest req, HttpSession session, String _id,
			String action) throws Exception {
		if ("".equals(_id)) {
			errorCode = "NOT FOUND";
			errorDesc = "Không tìm thấy thông tin tờ khai.";
			return;
		}
		BaseDTO baseDTO = new BaseDTO(req);
		Msg msg = baseDTO.createMsg(cup, Constants.MSG_ACTION_CODE.INQUIRY);
		HashMap<String, String> hData = new HashMap<>();
		msg.setObjData(hData);

		JSONRoot root = new JSONRoot(msg);
		MsgRsp rsp = restAPI.callAPINormal("/tkctu/detail/" + _id, cup.getLoginRes().getToken(), HttpMethod.POST, root);
		MspResponseStatus rspStatus = rsp.getResponseStatus();
		if (rspStatus.getErrorCode() == 0) {
			JsonNode jsonData = Json.serializer().nodeFromObject(rsp.getObjData());

			if ("EDIT|SIGN".indexOf(action) != -1
					&& !"CREATED".equals(commons.getTextJsonNode(jsonData.at("/Status")))) {
				errorCode = "NOT FOUND";
				errorDesc = "Trạng thái tờ khai không hợp lệ.";
				return;
			}

			req.setAttribute("tenNnt", commons.getTextJsonNode(jsonData.at("/TNNT")));
			req.setAttribute("MSo", commons.getTextJsonNode(jsonData.at("/MSo")));
			req.setAttribute("MSCode", commons.getTextJsonNode(jsonData.at("/MSo")));
			req.setAttribute("ten", commons.getTextJsonNode(jsonData.at("/Ten")));
			req.setAttribute("mst", commons.getTextJsonNode(jsonData.at("/MST")));
			String ht = commons.getTextJsonNode(jsonData.at("/HThuc"));
			if (ht.equals("1")) {
				ht = "Đăng ký mới";
			}
			if (ht.equals("2")) {
				ht = "Thay đổi thông tin";
			}

			if (action.equals("COPY")) {
				req.setAttribute("HThuc", "Thay đổi thông tin");
				req.setAttribute("HTCode", "2");
			} else {
				req.setAttribute("HThuc", ht);
				req.setAttribute("HTCode", commons.getTextJsonNode(jsonData.at("/HThuc")));
			}

			tinhThanh = commons.getTextJsonNode(jsonData.at("/TinhThanhInfo/code"));
			req.setAttribute("TThanhCode", tinhThanh);
			req.setAttribute("TThanhName", commons.getTextJsonNode(jsonData.at("/TinhThanhInfo/name")));

			String cqt = commons.getTextJsonNode(jsonData.at("/ChiCucThueInfo/code"));
			req.setAttribute("CQThueCode", cqt);
            String name = commons.getTextJsonNode(jsonData.at("/ChiCucThueInfo/name")) + " ("+commons.getTextJsonNode(jsonData.at("/ChiCucThueInfo/note"))+")";
			req.setAttribute("CQThueName", name);

			req.setAttribute("NLHe", commons.getTextJsonNode(jsonData.at("/NLHe")));
			req.setAttribute("DCLHe", commons.getTextJsonNode(jsonData.at("/DCLHe")));
			req.setAttribute("DCTDTu", commons.getTextJsonNode(jsonData.at("/DCTDTu")));
			req.setAttribute("DTLHe", commons.getTextJsonNode(jsonData.at("/DTLHe")));
			if (!action.equals("COPY")) {
				req.setAttribute("nLap",
						commons.convertLocalDateTimeToString(
								commons.convertLongToLocalDate(jsonData.at("/NLap").asLong()),
								Constants.FORMAT_DATE.FORMAT_DATE_WEB));
			}

			req.setAttribute("TCCNPHanh", "1".equals(commons.getTextJsonNode(jsonData.at("/DTPHanh/TCCNPHanh"))));
			req.setAttribute("CQTPHanh", "1".equals(commons.getTextJsonNode(jsonData.at("/DTPHanh/CQTPHanh"))));

			req.setAttribute("optPThuc", commons.getTextJsonNode(jsonData.at("/PThuc")));
			req.setAttribute("LCTSDung_TNCNhan",
					"1".equals(commons.getTextJsonNode(jsonData.at("/LHSDung/CTTNCNhan"))));
			req.setAttribute("LCTSDung_TMDTu", "1".equals(commons.getTextJsonNode(jsonData.at("/LHSDung/CTKTTTMDTu"))));
			req.setAttribute("LCTSDung_BLKIMGia",
					"1".equals(commons.getTextJsonNode(jsonData.at("/LHSDung/BLTPLPKIn"))));
			req.setAttribute("LCTSDung_BLIMGia", "1".equals(commons.getTextJsonNode(jsonData.at("/LHSDung/BLTPLPIn"))));
			req.setAttribute("LCTSDung_BLCTT50",
					"1".equals(commons.getTextJsonNode(jsonData.at("/LHSDung/BLTTPLPhi"))));

			HashMap<String, String> hItem = null;
			List<Object> prds = new ArrayList<Object>();
			if (!jsonData.at("/DSCTSSDung").isMissingNode()) {
				for (JsonNode o : jsonData.at("/DSCTSSDung")) {
					hItem = new LinkedHashMap<String, String>();
					hItem.put("TTChuc", commons.getTextJsonNode(o.at("/TTChuc")));
					hItem.put("Seri", commons.getTextJsonNode(o.at("/Seri")));
					hItem.put("TNgay",
							commons.convertLocalDateTimeToString(
									commons.convertLongToLocalDateTime(o.at("/TNgay").asLong()),
									Constants.FORMAT_DATE.FORMAT_DATE_TIME_WEB));
					hItem.put("DNgay",
							commons.convertLocalDateTimeToString(
									commons.convertLongToLocalDateTime(o.at("/DNgay").asLong()),
									Constants.FORMAT_DATE.FORMAT_DATE_TIME_WEB));
					if ("DETAIL".equals(action) || "SIGN".equals(action)) {
						hItem.put("HThuc", Constants.MAP_TKHAI_HTHUC.get(commons.getTextJsonNode(o.at("/HThuc"))));
					} else {
						hItem.put("HThuc", commons.getTextJsonNode(o.at("/HThuc")));
					}
					prds.add(hItem);
				}
			}
			req.setAttribute("DSCTSSDung", Json.serializer().toString(prds));

			if (!action.equals("COPY"))
				return;
			msg = baseDTO.createMsg(cup, Constants.MSG_ACTION_CODE.INQUIRY);
			root = new JSONRoot(msg);
			String id = cup.getLoginRes().getIssuerInfo().get_id();
			rsp = restAPI.callAPINormal("/main/profile/" + id, cup.getLoginRes().getToken(), HttpMethod.POST, root);
			rspStatus = rsp.getResponseStatus();
			if (rspStatus.getErrorCode() == 0) {
				jsonData = Json.serializer().nodeFromObject(rsp.getObjData());
				tinhThanh = commons.getTextJsonNode(jsonData.at("/TinhThanhInfo/code"));
				req.setAttribute("TThanhCode", tinhThanh);
				req.setAttribute("TThanhName", commons.getTextJsonNode(jsonData.at("/TinhThanhInfo/name")));

				req.setAttribute("CQThueCode", commons.getTextJsonNode(jsonData.at("/ChiCucThueInfo/code")));
				req.setAttribute("CQThueName", commons.getTextJsonNode(jsonData.at("/ChiCucThueInfo/name")));
			}
		} else {
			errorDesc = rspStatus.getErrorDesc();
		}
	}

	private void validateEmpty(String value, String message, BaseDTO dto) {
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
		mst = commons.getParameterFromRequest(req, "mst").replaceAll("\\s", "");
		tennnt = commons.getParameterFromRequest(req, "ten-nnt").trim().replaceAll("\\s+", " ");
		nlhe = commons.getParameterFromRequest(req, "NLHe").trim().replaceAll("\\s+", " ");
		dclhe = commons.getParameterFromRequest(req, "DCLHe").trim().replaceAll("\\s+", " ");
		dctdtu = commons.getParameterFromRequest(req, "DCTDTu").trim().replaceAll("\\s+", " ");
		dtlhe = commons.getParameterFromRequest(req, "DTLHe").trim().replaceAll("\\s+", " ");

		mauSo = commons.getParameterFromRequest(req, "mau-so").replaceAll("\\s", "");
		ten = commons.getParameterFromRequest(req, "ten").trim().replaceAll("\\s+", " ");
		tinhThanh = commons.getParameterFromRequest(req, "tinh-thanh").replaceAll("\\s", "");
		cqtqly = commons.getParameterFromRequest(req, "CQTQLy").replaceAll("\\s", "");
		nlap = commons.getParameterFromRequest(req, "NLap").replaceAll("\\s", "");
		hthuc = commons.getParameterFromRequest(req, "HThuc").replaceAll("\\s", "");

		tccnnphanh = commons.getParameterFromRequest(req, "TCCNPHanh").replaceAll("\\s", "");
		cqtphanh = commons.getParameterFromRequest(req, "CQTPHanh").replaceAll("\\s", "");

		pthuc = commons.getParameterFromRequest(req, "PThuc").replaceAll("\\s", "");

		lctsdung_TNCNhan = commons.getParameterFromRequest(req, "LCTSDung_TNCNhan").replaceAll("\\s", "");
		lctsdung_TMDTu = commons.getParameterFromRequest(req, "LCTSDung_TMDTu").replaceAll("\\s", "");
		lctsdung_BLKIMGia = commons.getParameterFromRequest(req, "LCTSDung_BLKIMGia").replaceAll("\\s", "");
		lctsdung_BLIMGia = commons.getParameterFromRequest(req, "LCTSDung_BLIMGia").replaceAll("\\s", "");
		lctsdung_BLCTT50 = commons.getParameterFromRequest(req, "LCTSDung_BLCTT50").replaceAll("\\s", "");

		dsctsSDung = commons.getParameterFromRequest(req, "DSCTSSDung").replaceAll("\\s", "");
		switch (transaction) {
		case "tkctu-cre":
		case "tkctu-edit":
		case "tkctu-copy":
			validateEmpty(dclhe, "Vui lòng nhập địa chỉ liên hệ.", dto);
			validateEmpty(nlhe, "Vui lòng nhập người liên hệ.", dto);
			validateEmpty(dctdtu, "Vui lòng nhập email liên hệ.", dto);
			validateEmpty(dclhe, "Vui lòng nhập diện thoại liên hệ", dto);

			validateEmpty(mauSo, "Vui lòng chọn mẫu số.", dto);
			validateEmpty(nlap, "Vui lòng chọn ngày lập.", dto);
			validateEmpty(hthuc, "Vui lòng chọn hình thức đăng ký.", dto);
			validateEmpty(tinhThanh, "Vui lòng chọn tỉnh/thành phố.", dto);
			validateEmpty(cqtqly, "Vui lòng chọn cơ quan thuế.", dto);

			if (tccnnphanh.equals("") && cqtphanh.equals("")) {
				dto.setErrorCode(1);
				dto.getErrorMessages().add("Vui lòng chọn đối tượng phát hành chứng từ điện tử.");
			}
			if (lctsdung_TNCNhan.equals("") && lctsdung_TMDTu.equals("") && lctsdung_BLKIMGia.equals("")
					&& lctsdung_BLIMGia.equals("") && lctsdung_BLCTT50.equals("")) {
				dto.setErrorCode(1);
				dto.getErrorMessages().add("Vui lòng chọn loại chứng từ sử dụng.");
			}
			break;
		case "tkctu-del":
			if ("".equals(_id)) {
				dto.setErrorCode(1);
				dto.getErrorMessages().add("Không tìm thấy thông tin tờ khai.");
			}
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
		String messageConfirm = "Bạn có muốn đăng ký tờ khai không?";
		switch (transaction) {
		case "tkctu-cre":
			messageConfirm = "Bạn có muốn đăng ký tờ khai không?";
			break;
		case "tkctu-edit":
			messageConfirm = "Bạn có muốn thay đổi tờ khai không?";
			break;
		case "tkctu-copy":
			messageConfirm = "Bạn có muốn copy tờ khai không?";
			break;
		case "tkctu-del":
			messageConfirm = "Bạn có muốn xóa tờ khai này không?";
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
		/* END: CHECK TOKEN */
		JsonNode jsonNodeTmp = null;
		try {
			jsonNodeTmp = Json.serializer().nodeFromJson(commons.decodeBase64ToString(dsctsSDung));
		} catch (Exception e) {
			log.error(" >>>>> An exception occurred!", e);
		}

		String actionCode = Constants.MSG_ACTION_CODE.CREATED;
		switch (transaction) {
		case "tkctu-cre":
			actionCode = Constants.MSG_ACTION_CODE.CREATED;
			break;
		case "tkctu-edit":
			actionCode = Constants.MSG_ACTION_CODE.MODIFY;
			break;
		case "tkctu-copy":
			actionCode = Constants.MSG_ACTION_CODE.COPY;
			break;
		case "tkctu-del":
			actionCode = Constants.MSG_ACTION_CODE.DELETE;
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
		hData.put("Mst", mst);
		hData.put("TenNNT", tennnt);
		hData.put("NLHe", nlhe);
		hData.put("DCLHe", dclhe);
		hData.put("DCTDTu", dctdtu);
		hData.put("DTLHe", dtlhe);

		hData.put("MauSo", mauSo);
		hData.put("Ten", ten);
		hData.put("TinhThanh", tinhThanh);
		hData.put("CQTQLy", cqtqly);
		hData.put("NLap", nlap);
		hData.put("HThuc", hthuc);

		hData.put("TCCNPHanh", tccnnphanh);
		hData.put("CQTPHanh", cqtphanh);

		hData.put("PThuc", pthuc);

		hData.put("LCTSDung_TNCNhan", lctsdung_TNCNhan);
		hData.put("LCTSDung_TMDTu", lctsdung_TMDTu);
		hData.put("LCTSDung_BLKIMGia", lctsdung_BLKIMGia);
		hData.put("LCTSDung_BLIMGia", lctsdung_BLIMGia);
		hData.put("LCTSDung_BLCTT50", lctsdung_BLCTT50);
		hData.put("DSCTSSDung", jsonNodeTmp);

		msg.setObjData(hData);

		JSONRoot root = new JSONRoot(msg);
		MsgRsp rsp = restAPI.callAPINormal("/tkctu/crud", cup.getLoginRes().getToken(), HttpMethod.POST, root);
		MspResponseStatus rspStatus = rsp.getResponseStatus();
		if (rspStatus.getErrorCode() == 0) {
			dtoRes.setErrorCode(0);
			switch (transaction) {
			case "tkctu-cre":
				dtoRes.setResponseData("Đăng ký tờ khai chứng từ thành công.");
				break;
			case "tkctu-edit":
				dtoRes.setResponseData("Thay đổi tờ khai chứng từ thành công.");
				break;
			case "tkctu-copy":
				dtoRes.setResponseData("Copy tờ khai chứng từ thành công.");
				break;
			case "tkctu-del":
				dtoRes.setResponseData("Xóa tờ khai chứng từ thành công.");
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

		if ("".equals(_id)) {
			dto.setErrorCode(1);
			dto.setResponseData("Không tìm thấy tờ khai cần ký.");
			return dto;
		}

		/* LAY THONG TIN DU LIEU XML VE SERVER WEB */
		dto = new BaseDTO(req);
		Msg msg = dto.createMsg(cup, Constants.MSG_ACTION_CODE.CREATED);
		HashMap<String, Object> hData = new HashMap<>();
		hData.put("_id", _id);
		msg.setObjData(hData);
		JSONRoot root = new JSONRoot(msg);

		FileInfo fileInfo = restAPI.callAPIGetFileInfo("/tkctu/get-file-for-sign", cup.getLoginRes().getToken(),
				HttpMethod.POST, root);
		if (null == fileInfo.getContentFile()) {
			dto.setErrorCode(999);
			dto.setResponseData("Không tìm thấy dữ liệu tờ khai.");
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

	public BaseDTO checkDataToSign(HttpServletRequest req, HttpSession session, String transaction,
			CurrentUserProfile cup) throws Exception {
		BaseDTO dto = new BaseDTO();
		dto.setErrorCode(0);

		return dto;
	}

	@RequestMapping(value = "/signFile", produces = MediaType.APPLICATION_JSON_VALUE, method = RequestMethod.POST, consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	@ResponseBody
	public BaseDTO processSignFile(HttpServletRequest req, HttpSession session,
			@RequestAttribute(name = "transaction", value = "", required = false) String transaction,
			@RequestParam("XMLFileSigned") MultipartFile multipartFile, @RequestParam("certificate") String certificate,
			@RequestParam("_id") String _id) throws Exception {
		BaseDTO dtoRes = new BaseDTO();

		CurrentUserProfile cup = getCurrentlyAuthenticatedPrincipal();
		dtoRes = checkDataToSign(req, session, transaction, cup);
		if (0 != dtoRes.getErrorCode()) {
			dtoRes.setErrorCode(999);
			dtoRes.setResponseData(Constants.MAP_ERROR.get(999));
			return dtoRes;
		}

		dtoRes = new BaseDTO(req);
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
		contentDisposition = ContentDisposition.builder("form-data").name("XMLFileSigned")
				.filename("tkhai.xml").build();
		fileMap.add(HttpHeaders.CONTENT_DISPOSITION, contentDisposition.toString());
		fileEntity = new HttpEntity<byte[]>(multipartFile.getBytes(), fileMap);
		body.add("XMLFileSigned", fileEntity);

		HttpEntity<MultiValueMap<String, Object>> requestBody = new HttpEntity<>(body, headers);
		String url = "/tkctu/sign-single";
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
			dtoRes = new BaseDTO(result.getStatusCode().value(), "Thực hiện tờ khai không thành công.");
		}
		return dtoRes;
	}

}
