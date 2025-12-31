package vn.sesgroup.hddt.controller.tncn;

import java.io.File;
import java.io.FileOutputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoField;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

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
import com.api.message.MsgPage;
import com.api.message.MsgParam;
import com.api.message.MsgParams;
import com.api.message.MsgRsp;
import com.api.message.MspResponseStatus;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;

import vn.sesgroup.hddt.controller.AbstractController;
import vn.sesgroup.hddt.dto.BaseDTO;
import vn.sesgroup.hddt.dto.CurrentUserProfile;
import vn.sesgroup.hddt.dto.FileInfo;
import vn.sesgroup.hddt.dto.GetXMLInfoXMLDTO;
import vn.sesgroup.hddt.dto.JsonGridDTO;
import vn.sesgroup.hddt.resources.APIParams;
import vn.sesgroup.hddt.resources.RestAPIUtility;
import vn.sesgroup.hddt.utils.Constants;
import vn.sesgroup.hddt.utils.Json;
import vn.sesgroup.hddt.utils.SystemParams;

@Controller
@RequestMapping({ "/cttncn-creV1", "/cttncn-detailV1", "/cttncn-editV1", "/cttncn-signV1", "/cttncn-signAllV1",
		"/cttncn-delV1", "/cttncn-historyV1", "/cttncn-cre-dc-tt"

})
@Scope(value = WebApplicationContext.SCOPE_REQUEST)
public class ChungTuCRUDControllerV1 extends AbstractController {
	private static final Logger log = LogManager.getLogger(ChungTuCRUDControllerV1.class);
	@Autowired
	RestAPIUtility restAPI;
	@Autowired
	RestTemplate restTemplate;
	private String errorCode;
	private String errorDesc;
	private String _id;
	private String _id_tt_dc;
	private String _tcctu;
	private String name;
	private String code;
	private String taxcode;

	private String dchi;
	private String qtich;
	private String optHTHDon;
	private String cccdan;
	private String sdthoai;
	private String dctdtu;
	private String dctdtucc;

	private String msctu;
	private String nam;
	private String tthang;
	private String dthang;

	private String ktnhap;
	private String bhiem;
	private String tthien;
	private String ttncthue;
	private String ttntthue;
	private String sthue;

	private void LoadParameter(CurrentUserProfile cup, HttpServletRequest req) {
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
			msgParam.setParam("DMMSTNCN");
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
						if (commons.getTextJsonNode(o.get("KyHieu")).equalsIgnoreCase("CT")) {
							hItem.put(commons.getTextJsonNode(o.get("_id")),
									commons.getTextJsonNode(o.get("KyHieu")) + "/"
											+ String.valueOf(commons.getTextJsonNode(o.get("Nam"))).substring(2)
											+ commons.getTextJsonNode(o.get("ChungTu")));
						}
					}

					req.setAttribute("map_mausotncn", hItem);
				}
			}

		} catch (Exception e) {
		}
	}

	@RequestMapping(value = { "/init", "/init-dc", "/init-tt" }, method = { RequestMethod.POST })
	public String init(Locale locale, HttpServletRequest req, HttpSession session,
			@RequestAttribute(name = "transaction", value = "", required = false) String transaction,
			@RequestAttribute(name = "method", value = "", required = false) String method) throws Exception {
		errorCode = "";
		errorDesc = "";

		_id = commons.getParameterFromRequest(req, "_id");
		CurrentUserProfile cup = getCurrentlyAuthenticatedPrincipal();

		String header = "Thêm mới chứng từ";
		String action = "CREATE";
		boolean isEdit = false;

		switch (transaction) {
		case "cttncn-creV1":
		case "cttncn-cre-dc-tt":
			LoadParameter(cup, req);
			header = "Thêm mới chứng từ TNCN";
			action = "CREATE";
			isEdit = true;
			req.setAttribute("optHTHDon", "0");
			break;
		case "cttncn-detailV1":
			LoadParameter(cup, req);
			header = "Chi tiết thông tin chứng từ TNCN";
			action = "DETAIL";
			isEdit = false;
			break;
		case "cttncn-editV1":
			LoadParameter(cup, req);
			header = "Thay đổi thông tin chứng từ TNCN";
			action = "EDIT";
			isEdit = true;
			break;
		case "cttncn-signV1":
			header = "Ký";
			action = "SIGN";
			isEdit = false;
			break;
		default:
			break;
		}

		if ("|cttncn-detailV1|cttncn-editV1|".indexOf(transaction) != -1 || "init-dc".equals(method)
				|| "init-tt".equals(method)) {
			inquiry(cup, locale, req, session, _id, action, method);
		}
		req.setAttribute("_header_", header);
		req.setAttribute("_action_", action);
		req.setAttribute("_id", _id);
		req.setAttribute("_isedit_", isEdit);

		return "tncn/cttncn-crudV1";
	}

	private void inquiry(CurrentUserProfile cup, Locale locale, HttpServletRequest req, HttpSession session, String _id,
			String action, String method) throws Exception {
		if ("".equals(_id)) {
			errorCode = "NOT FOUND";
			errorDesc = "Không tìm thấy thông tin sản phẩm.";
			return;
		}
		BaseDTO baseDTO = new BaseDTO(req);
		Msg msg = baseDTO.createMsg(cup, Constants.MSG_ACTION_CODE.INQUIRY);
		HashMap<String, String> hData = new HashMap<>();
		msg.setObjData(hData);

		JSONRoot root = new JSONRoot(msg);
		MsgRsp rsp = restAPI.callAPINormal("/cttncn/detailV1/" + _id, cup.getLoginRes().getToken(), HttpMethod.POST,
				root);
		MspResponseStatus rspStatus = rsp.getResponseStatus();
		if (rspStatus.getErrorCode() == 0) {
			JsonNode jsonData = Json.serializer().nodeFromObject(rsp.getObjData());
			req.setAttribute("Ten", commons.getTextJsonNode(jsonData.at("/NNT/Ten")));
			req.setAttribute("Code", commons.getTextJsonNode(jsonData.at("/NNT/Code")));
			req.setAttribute("MST", commons.getTextJsonNode(jsonData.at("/NNT/MST")));
			req.setAttribute("DChi", commons.getTextJsonNode(jsonData.at("/NNT/DChi")));
			req.setAttribute("QTich", commons.getTextJsonNode(jsonData.at("/NNT/QTich")));
			req.setAttribute("optHTHDon", commons.getTextJsonNode(jsonData.at("/NNT/CTru")));
			req.setAttribute("CCCDan", commons.getTextJsonNode(jsonData.at("/NNT/CCCDan")));
			req.setAttribute("SDThoai", commons.getTextJsonNode(jsonData.at("/NNT/SDThoai")));
			req.setAttribute("DCTDTu", commons.getTextJsonNode(jsonData.at("/NNT/DCTDTu")));
			req.setAttribute("DCTDTuCC", commons.getTextJsonNode(jsonData.at("/NNT/DCTDTuCC")));

			req.setAttribute("MSCTu", commons.getTextJsonNode(jsonData.at("/MauSo")));
			req.setAttribute("Nam", commons.getTextJsonNode(jsonData.at("/TTNCNKTru/Nam")));
			req.setAttribute("TThang", commons.getTextJsonNode(jsonData.at("/TTNCNKTru/TThang")));
			req.setAttribute("DThang", commons.getTextJsonNode(jsonData.at("/TTNCNKTru/DThang")));

			req.setAttribute("KTNhap", commons.getTextJsonNode(jsonData.at("/TTNCNKTru/KTNhap")));
			req.setAttribute("BHiem", commons.getTextJsonNode(jsonData.at("/TTNCNKTru/BHiem")));
			req.setAttribute("TThien", commons.getTextJsonNode(jsonData.at("/TTNCNKTru/TThien")));
			req.setAttribute("TTNCThue", commons.getTextJsonNode(jsonData.at("/TTNCNKTru/TTNCThue")));
			req.setAttribute("TTNTThue", commons.getTextJsonNode(jsonData.at("/TTNCNKTru/TTNTThue")));
			req.setAttribute("SThue", commons.getTextJsonNode(jsonData.at("/TTNCNKTru/SThue")));

			String notice = "";
			if (("init-dc".equals(method) || "init-tt".equals(method))) {
				String title = "init-dc".equals(method) ? "Điều chỉnh" : "Thay thế";

				notice = String.format("(%s cho chứng từ Ký hiệu %s, số %s, ngày %s)", title,
						commons.getTextJsonNode(jsonData.at("/KHCTu")), commons.getTextJsonNode(jsonData.at("/SCTu")),
						commons.convertLocalDateTimeToString(
								commons.convertLongToLocalDate(jsonData.at("/NLap").asLong()),
								Constants.FORMAT_DATE.FORMAT_DATE_WEB));

				req.setAttribute("_notice", notice);
				req.setAttribute("_id_tt_dc", _id);
				req.setAttribute("_tcctu", "init-tt".equals(method) ? "1" : "2");
			} else {
				if (!jsonData.at("/TTCTLQuan").isMissingNode()) {
					String title = commons.getTextJsonNode(jsonData.at("/TTCTLQuan/TCCTu")).equals("1") ? "Thay thế"
							: "Điều chỉnh";
					notice = String.format("(%s cho chứng từ Ký hiệu %s, số %s, ngày %s)", title,
							commons.getTextJsonNode(jsonData.at("/TTCTLQuan/KHCTCLQuan")),
							commons.getTextJsonNode(jsonData.at("/TTCTLQuan/SCTCLQuan")),
							commons.convertLocalDateTimeToString(commons.convertStringToLocalDate(
									commons.getTextJsonNode(jsonData.at("/TTCTLQuan/NLCTCLQuan")), "yyyy-MM-dd"),
									Constants.FORMAT_DATE.FORMAT_DATE_WEB)

					);
					req.setAttribute("_notice", notice);
				}
			}
		} else {
			errorDesc = rspStatus.getErrorDesc();
		}
	}

	public BaseDTO checkDataToAccept(HttpServletRequest req, HttpSession session, String transaction,
			CurrentUserProfile cup) throws Exception {
		BaseDTO dto = new BaseDTO();
		dto.setErrorCode(0);

		_id = commons.getParameterFromRequest(req, "_id").replaceAll("\\s", "");
		_id_tt_dc = commons.getParameterFromRequest(req, "_id_tt_dc").replaceAll("\\s", "");
		_tcctu = commons.getParameterFromRequest(req, "_tcctu").replaceAll("\\s", "");
		name = commons.getParameterFromRequest(req, "name");
		code = commons.getParameterFromRequest(req, "code").trim().replaceAll("\\s+", " ");
		taxcode = commons.getParameterFromRequest(req, "taxcode").trim().replaceAll("\\s+", " ");
		dchi = commons.getParameterFromRequest(req, "dchi").trim().replaceAll("\\s+", " ");
		qtich = commons.getParameterFromRequest(req, "qtich").trim().replaceAll("\\s+", " ");
		optHTHDon = commons.getParameterFromRequest(req, "optHTHDon").trim().replaceAll("\\s+", " ");
		cccdan = commons.getParameterFromRequest(req, "cccdan").trim().replaceAll("\\s+", " ");
		sdthoai = commons.getParameterFromRequest(req, "sdthoai").trim().replaceAll("\\s+", " ");
		dctdtu = commons.getParameterFromRequest(req, "dctdtu").trim().replaceAll("\\s+", " ");
		dctdtucc = commons.getParameterFromRequest(req, "dctdtucc").trim().replaceAll("\\s+", " ");

		msctu = commons.getParameterFromRequest(req, "msctu").trim().replaceAll("\\s+", " ");
		nam = commons.getParameterFromRequest(req, "nam").trim().replaceAll("\\s+", " ");
		tthang = commons.getParameterFromRequest(req, "tthang").trim().replaceAll("\\s+", " ");
		dthang = commons.getParameterFromRequest(req, "dthang").trim().replaceAll("\\s+", " ");

		ktnhap = commons.getParameterFromRequest(req, "ktnhap").trim().replaceAll("\\s+", " ");
		bhiem = commons.getParameterFromRequest(req, "bhiem").trim().replaceAll("\\s+", " ");
		tthien = commons.getParameterFromRequest(req, "tthien").trim().replaceAll("\\s+", " ");
		ttncthue = commons.getParameterFromRequest(req, "ttncthue").trim().replaceAll("\\s+", " ");
		ttntthue = commons.getParameterFromRequest(req, "ttntthue").trim().replaceAll("\\s+", " ");
		sthue = commons.getParameterFromRequest(req, "sthue").trim().replaceAll("\\s+", " ");

		if ("cttncn-edit".equals(transaction)) {
			if ("".equals(_id)) {
				dto.setErrorCode(1);
				dto.getErrorMessages().add("Không tìm thấy thông tin chứng từ.");
			}
		}
		switch (transaction) {
		case "cttncn-creV1":
		case "cttncn-editV1":

			if ("".equals(name)) {
				dto.setErrorCode(1);
				dto.getErrorMessages().add("Vui lòng nhập Họ và Tên Nhân viên.");
			}
			if ("".equals(dchi)) {
				dto.setErrorCode(1);
				dto.getErrorMessages().add("Vui lòng nhập Địa chỉ.");
			}

			if ("".equals(cccdan)) {
				dto.setErrorCode(1);
				dto.getErrorMessages().add("Vui lòng nhập Căn cước công dân.");
			}

			if ("".equals(sdthoai)) {
				dto.setErrorCode(1);
				dto.getErrorMessages().add("Vui lòng nhập Số điện thoại.");
			}
			if ("".equals(msctu)) {
				dto.setErrorCode(1);
				dto.getErrorMessages().add("Vui lòng chọn Mã số chứng từ.");
			}
			if ("".equals(nam)) {
				dto.setErrorCode(1);
				dto.getErrorMessages().add("Vui lòng nhập Năm (Thời điểm tra thu nhập).");
			}
			if ("".equals(tthang)) {
				dto.setErrorCode(1);
				dto.getErrorMessages().add("Vui lòng nhập Từ tháng (Tháng bắt đầu trả thu nhập).");
			}
			if ("".equals(dthang)) {
				dto.setErrorCode(1);
				dto.getErrorMessages().add("Vui lòng nhập Đến Tháng (Tháng cuối cùng trả thu nhập).");
			}
			if (!tthang.matches("\\d{2}") || !dthang.matches("\\d{2}")) {
				dto.setErrorCode(1);
				dto.getErrorMessages().add("Từ tháng và đến tháng chưa đúng format MM.");
			}
			if ("".equals(ktnhap)) {
				dto.setErrorCode(1);
				dto.getErrorMessages().add("Vui lòng nhập Khoản thu nhập.");
			}
			if ("".equals(bhiem)) {
				dto.setErrorCode(1);
				dto.getErrorMessages().add("Vui lòng nhập Bảo hiểm (Khoản đóng bảo hiểm bắt buộc).");
			}
			if ("".equals(tthien)) {
				dto.setErrorCode(1);
				dto.getErrorMessages().add("Vui lòng nhập Khoản từ thiện, nhân đạo, khuyến học.");
			}
			if ("".equals(ttncthue)) {
				dto.setErrorCode(1);
				dto.getErrorMessages()
						.add("Vui lòng nhập Tổng thu nhập chịu thuế (Tổng thu nhập chịu thuế phải khấu trừ).");
			}
			if ("".equals(ttntthue)) {
				dto.setErrorCode(1);
				dto.getErrorMessages().add("Vui lòng nhập Tổng thu nhập tính thuế.");
			}
			if ("".equals(sthue)) {
				dto.setErrorCode(1);
				dto.getErrorMessages().add("Vui lòng nhập Số thuế (Số thuế thu nhập cá nhân đã khấu trừ).");
			}
			break;
		case "cttncn-delV1":
			if ("".equals(_id)) {
				dto.setErrorCode(1);
				dto.getErrorMessages().add("Vui lòng chọn chứng từ cần xóa.");
			}
			break;
		case "cttncn-signAllV1":
//			_token = commons.getParameterFromRequest(req, "_token").replaceAll("\\s", "");
//			ids = null;
//			try {
//				ids = Json.serializer().fromJson(commons.decodeBase64ToString(_token), new TypeReference<List<String>>() {
//				});
//			}catch(Exception e) {}
//			break;
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
		String messageConfirm = "Bạn có muốn thêm mới chứng từ không?";
		switch (transaction) {
		case "cttncn-creV1":
		case "cttncn-cre-dc-tt":
			messageConfirm = "Bạn có muốn thêm mới chứng từ không?";
			break;
		case "cttncn-editV1":
			messageConfirm = "Bạn có muốn thay đổi thông tin chứng từ không?";
			break;
		case "cttncn-delV1":
			messageConfirm = "Bạn có muốn xóa danh sách chứng từ không?";
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

		String actionCode = Constants.MSG_ACTION_CODE.CREATED;
		switch (transaction) {
		case "cttncn-creV1":
		case "cttncn-cre-dc-tt":
			actionCode = Constants.MSG_ACTION_CODE.CREATED;
			break;
		case "cttncn-editV1":
			actionCode = Constants.MSG_ACTION_CODE.MODIFY;
			break;
		case "cttncn-delV1":
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
		switch (transaction) {
		case "cttncn-delV1":
			hData.put("_id", _id);
			break;
		default:
			hData.put("_id", _id);
			hData.put("_id_tt_dc", _id_tt_dc);
			hData.put("_tcctu", _tcctu);
			hData.put("Ten", name);
			hData.put("Code", code);
			hData.put("MSThue", taxcode);
			hData.put("DChi", dchi);
			hData.put("QTich", qtich);
			hData.put("CTru", optHTHDon);
			hData.put("CCCDan", cccdan);
			hData.put("SDThoai", sdthoai);
			hData.put("DCTDTu", dctdtu);
			hData.put("DCTDTuCC", dctdtucc);

			hData.put("MSCTu", msctu);
			hData.put("Nam", nam);
			hData.put("TThang", tthang);
			hData.put("DThang", dthang);

			hData.put("KTNhap", ktnhap);
			hData.put("BHiem", bhiem);
			hData.put("TThien", tthien);
			hData.put("TTNCThue", ttncthue);
			hData.put("TTNTThue", ttntthue);
			hData.put("SThue", sthue);
			break;
		}

		msg.setObjData(hData);
		JSONRoot root = new JSONRoot(msg);
		MsgRsp rsp = restAPI.callAPINormal("/cttncn/crudV1", cup.getLoginRes().getToken(), HttpMethod.POST, root);
		MspResponseStatus rspStatus = rsp.getResponseStatus();
		if (rspStatus.getErrorCode() == 0) {

			dtoRes.setErrorCode(0);
			switch (transaction) {
			case "cttncn-creV1":
				dtoRes.setResponseData("Thêm mới thông tin chứng từ thành công.");
				break;
			case "cttncn-editV1":
				dtoRes.setResponseData("Cập nhật thông tin chứng từ thành công.");
				break;
			case "cttncn-delV1":
				dtoRes.setResponseData("Xóa danh sách chứng từ thành công.");
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
			dto.setResponseData("Không tìm chứng từ cần ký.");
			return dto;
		}

		/* LAY THONG TIN DU LIEU XML VE SERVER WEB */
		dto = new BaseDTO(req);
		Msg msg = dto.createMsg(cup, Constants.MSG_ACTION_CODE.CREATED);
		HashMap<String, Object> hData = new HashMap<>();
		hData.put("_id", _id);
		msg.setObjData(hData);
		JSONRoot root = new JSONRoot(msg);

		FileInfo fileInfo = restAPI.callAPIGetFileInfo("/cttncn/get-file-for-signV1", cup.getLoginRes().getToken(),
				HttpMethod.POST, root);
		if (null == fileInfo || null == fileInfo.getContentFile()) {
			dto.setErrorCode(999);
			dto.setResponseData(
					fileInfo.getCheck().isEmpty() ? fileInfo.getCheck() : "Không tìm thấy dữ liệu hóa đơn.");
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
		contentDisposition = ContentDisposition.builder("form-data").name("XMLFileSigned").filename("cttncn-signed.xml")
				.build();
		fileMap.add(HttpHeaders.CONTENT_DISPOSITION, contentDisposition.toString());
		fileEntity = new HttpEntity<byte[]>(multipartFile.getBytes(), fileMap);
		body.add("XMLFileSigned", fileEntity);

		HttpEntity<MultiValueMap<String, Object>> requestBody = new HttpEntity<>(body, headers);
		String url = "/cttncn/sign-singleV1";
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
			dtoRes = new BaseDTO(result.getStatusCode().value(), "Thực hiện ký hóa đơn không thành công.");
		}
		return dtoRes;
	}

	@RequestMapping(value = "/check-data-signAll", produces = MediaType.APPLICATION_JSON_VALUE, method = RequestMethod.POST)
	@ResponseBody
	public BaseDTO signAll(Locale locale, HttpServletRequest req, HttpSession session,
			@RequestAttribute(name = "transaction", value = "", required = false) String transaction) throws Exception {
		String token = "";
		List<String> ids = new ArrayList<String>();
		BaseDTO dto = new BaseDTO(req);

		if (null != session.getAttribute(Constants.SESSION_TYPE.SESSION_TOKEN_EXECUTE)) {
			token = session.getAttribute(Constants.SESSION_TYPE.SESSION_TOKEN_EXECUTE).toString();
			session.removeAttribute(token);
		}
		session.removeAttribute(Constants.SESSION_TYPE.SESSION_TOKEN_EXECUTE);

		switch (transaction) {
		case "cttncn-signAllV1":
			String _token = commons.getParameterFromRequest(req, "_token").replaceAll("\\s", "");
			try {
				ids = Json.serializer().fromJson(commons.decodeBase64ToString(_token),
						new TypeReference<List<String>>() {
						});
			} catch (Exception e) {
			}
			break;
		default:
			break;
		}

		if (ids.size() < 1) {
			dto.setErrorCode(1);
			dto.setResponseData("Không tìm thấy hóa đơn cần ký.");
			return dto;
		}

		CurrentUserProfile cup = getCurrentlyAuthenticatedPrincipal();
		Msg msg = dto.createMsg(cup, Constants.MSG_ACTION_CODE.CREATED);
		HashMap<String, Object> hData = new HashMap<>();
		hData.put("ids", ids);
		hData.put("soLuong", ids.size());
		msg.setObjData(hData);
		JSONRoot root = new JSONRoot(msg);

		FileInfo fileInfo = restAPI.callAPIGetFileInfo("/cttncn/get-file-for-signAllV1", cup.getLoginRes().getToken(),
				HttpMethod.POST, root);
		if (!"".equals(fileInfo.getCheck())) {
			dto.setErrorCode(999);
			dto.setResponseData(fileInfo.getCheck());
			return dto;
		}

		// CHECK SCTu
		MsgRsp rsp = restAPI.callAPINormal("/cttncn/check-sctu-list", cup.getLoginRes().getToken(), HttpMethod.POST,
				root);
		MspResponseStatus rspStatus = rsp.getResponseStatus();
		if (rspStatus.getErrorCode() != 0) {
			dto.setErrorCode(999);
			dto.setResponseData(rsp.getResponseStatus().getErrorDesc());
			return dto;
		}

		/* THONG TIN TEN FILE */
		token = commons.convertLocalDateTimeToString(LocalDateTime.now(), Constants.FORMAT_DATE.FORMAT_DATETIME_DB_FULL)
				+ "-" + commons.csRandomAlphaNumbericString(5);
		token += ".xml";
		File file = new File(SystemParams.DIR_TMP_SAVE_FILES);
		file.mkdirs();
		/* END - LAY THONG TIN DU LIEU XML VE SERVER WEB */

		// VONG LAP XML TRONG ZIP
		List<GetXMLInfoXMLDTO> arrFileInfos = fileInfo.getArrFileInfos();

		ZipOutputStream zout = null;
		FileOutputStream fos = null;
		if (arrFileInfos != null) {
			arrFileInfos = fileInfo.getArrFileInfos();
			fos = new FileOutputStream(new File(file, token + ".zip"));
			zout = new ZipOutputStream(fos);
			for (GetXMLInfoXMLDTO o : arrFileInfos) {
				zout.putNextEntry(new ZipEntry(o.getFileName()));
				zout.write(o.getFileData());
				zout.closeEntry();
			}
			zout.close();
			fos.close();
			// END VONG LAP

			HashMap<String, Object> hR = new HashMap<String, Object>();
			hR.put("Token", token);
			hR.put("Time", commons.convertLocalDateTimeToString(LocalDateTime.now(),
					Constants.FORMAT_DATE.FORMAT_DATETIME_DB_FULL));
			hR.put("TaxCode", cup.getUsername());
			hR.put("Numbers", fileInfo.getNumbers());
			hR.put("FormIssueInvoiceID", fileInfo.getFormIssueInvoiceID());
			dto.setResponseData(hR);
		} else {
			dto.setErrorCode(999);
			dto.setResponseData("Không tìm thấy dữ liệu hóa đơn.");
			return dto;
		}
		return dto;
	}

	@RequestMapping(value = "/signFileAll", produces = MediaType.APPLICATION_JSON_VALUE, method = RequestMethod.POST, consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	@ResponseBody
	public BaseDTO processSignFileAll(HttpServletRequest req, HttpSession session,
			@RequestAttribute(name = "transaction", value = "", required = false) String transaction,
			@RequestParam(value = "zipFile", required = false) MultipartFile multipartFile,
			@RequestParam(value = "Numbers", required = true) String numbers,
			@RequestParam(value = "FormIssueInvoiceID", required = true) String formIssueInvoiceID,
			@RequestParam(value = "Certificate", required = true) String certificate) throws Exception {
		BaseDTO dtoRes = new BaseDTO();

		CurrentUserProfile cup = getCurrentlyAuthenticatedPrincipal();
		dtoRes = checkDataToSign(req, session, transaction, cup);
		if (0 != dtoRes.getErrorCode()) {
			dtoRes.setErrorCode(999);
			dtoRes.setResponseData(Constants.MAP_ERROR.get(999));
			return dtoRes;
		}
		String luu = cup.getUsername() + "/" + formIssueInvoiceID;
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

		/* ADD DU LIEU XML DA KY */
		fileMap = new LinkedMultiValueMap<String, String>();
		contentDisposition = ContentDisposition.builder("form-data").name("zipFile").filename(luu).build();
		fileMap.add(HttpHeaders.CONTENT_DISPOSITION, contentDisposition.toString());
		fileEntity = new HttpEntity<byte[]>(multipartFile.getBytes(), fileMap);
		body.add("zipFile", fileEntity);
		body.add("Ten", luu);
		HttpEntity<MultiValueMap<String, Object>> requestBody = new HttpEntity<>(body, headers);
		String url = "/cttncn/signAllV1";
		ResponseEntity<MsgRsp> result = restTemplate.exchange(APIParams.HTTP_URI + url, HttpMethod.POST, requestBody,
				MsgRsp.class);
		/* END - CONNECT TO API */
		if (result.getStatusCode() == org.springframework.http.HttpStatus.OK) {
			MsgRsp rsp = result.getBody();
			MspResponseStatus rspStatus = rsp.getResponseStatus();
			if (rspStatus.getErrorCode() == 0) {
				dtoRes.setErrorCode(0);
				dtoRes.setResponseData("Thực hiện ký hóa đơn không thành công.");
			} else {
				dtoRes = new BaseDTO(rspStatus.getErrorCode(), rspStatus.getErrorDesc());
			}
		} else {
			dtoRes = new BaseDTO(result.getStatusCode().value(), "Thực hiện ký hóa đơn không thành công.");
		}
		return dtoRes;
	}

	@RequestMapping(value = "/check-data-send-cqtV1", produces = MediaType.APPLICATION_JSON_VALUE, method = RequestMethod.POST)
	@ResponseBody
	public BaseDTO checkDataToSendCQTV1(Locale locale, HttpServletRequest req, HttpSession session,
			@RequestAttribute(name = "transaction", value = "", required = false) String transaction) throws Exception {
		String token = "";
		if (null != session.getAttribute(Constants.SESSION_TYPE.SESSION_TOKEN_EXECUTE)) {
			token = session.getAttribute(Constants.SESSION_TYPE.SESSION_TOKEN_EXECUTE).toString();
			session.removeAttribute(token);
		}
		session.removeAttribute(Constants.SESSION_TYPE.SESSION_TOKEN_EXECUTE);

		BaseDTO dto = new BaseDTO(req);
		_id = commons.getParameterFromRequest(req, "_id").replaceAll("\\s", "");

		if ("".equals(_id)) {
			dto.setErrorCode(1);
			dto.setResponseData("Không tìm chứng từ cần ký.");
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

	@RequestMapping(value = "/send-cqtV1", produces = MediaType.APPLICATION_JSON_VALUE, method = RequestMethod.POST)
	@ResponseBody
	public BaseDTO execData(HttpServletRequest req, HttpSession session,
			@RequestAttribute(name = "transaction", value = "", required = false) String transaction,
			@RequestParam(value = "tokenTransaction", required = false, defaultValue = "") String tokenTransaction)
			throws Exception {
		BaseDTO dto = new BaseDTO(req);
		_id = commons.getParameterFromRequest(req, "_id").replaceAll("\\s", "");

		if ("".equals(_id)) {
			dto.setErrorCode(1);
			dto.setResponseData("Không tìm chứng từ cần ký.");
			return dto;
		}

		/* CHECK TOKEN */
		String token = session.getAttribute(Constants.SESSION_TYPE.SESSION_TOKEN_EXECUTE) == null ? ""
				: session.getAttribute(Constants.SESSION_TYPE.SESSION_TOKEN_EXECUTE).toString();
		session.removeAttribute(Constants.SESSION_TYPE.SESSION_TOKEN_EXECUTE);
		if ("".equals(token) || !tokenTransaction.equals(token)) {
			dto.setErrorCode(1);
			dto.setResponseData("Token giao dịch không hợp lệ.");
			return dto;
		}
		String actionCode = Constants.MSG_ACTION_CODE.SEND_CQT;

		CurrentUserProfile cup = getCurrentlyAuthenticatedPrincipal();
		Msg msg = dto.createMsg(cup, actionCode);
		HashMap<String, Object> hData = new HashMap<>();
		hData.put("_id", _id);
		msg.setObjData(hData);

		JSONRoot root = new JSONRoot(msg);
		MsgRsp rsp = restAPI.callAPINormal("/cttncn/crudV1", cup.getLoginRes().getToken(), HttpMethod.POST, root);
		MspResponseStatus rspStatus = rsp.getResponseStatus();
		if (rspStatus.getErrorCode() == 0) {
			dto.setErrorCode(0);
			dto.setResponseData("Gửi Chứng từ đến CQT thành công.");
		} else {
			dto.setErrorCode(rspStatus.getErrorCode());
			dto.setResponseData(rspStatus.getErrorDesc());
		}
		return dto;
	}

	@RequestMapping(value = "/check-data-refresh-cqtV1", produces = MediaType.APPLICATION_JSON_VALUE, method = RequestMethod.POST)
	@ResponseBody
	public BaseDTO checkDataToRefreshCQTV1(Locale locale, HttpServletRequest req, HttpSession session,
			@RequestAttribute(name = "transaction", value = "", required = false) String transaction) throws Exception {
		BaseDTO dto = new BaseDTO(req);
		_id = commons.getParameterFromRequest(req, "_id").replaceAll("\\s", "");
		
		if ("".equals(_id)) {
			dto.setErrorCode(1);
			dto.setResponseData("Không tìm chứng từ cần ký.");
			return dto;
		}
		
		String token = "";
		if (null != session.getAttribute(Constants.SESSION_TYPE.SESSION_TOKEN_EXECUTE + _id)) {
			token = session.getAttribute(Constants.SESSION_TYPE.SESSION_TOKEN_EXECUTE + _id).toString();
			session.removeAttribute(token);
		}
		session.removeAttribute(Constants.SESSION_TYPE.SESSION_TOKEN_EXECUTE + _id);

		token = commons.csRandomAlphaNumbericString(30);
		session.setAttribute(Constants.SESSION_TYPE.SESSION_TOKEN_EXECUTE + _id, token);

		HashMap<String, String> hInfo = new HashMap<String, String>();
		hInfo.put("TOKEN", token);

		dto.setResponseData(hInfo);
		dto.setErrorCode(0);
		return dto;
	}

	@RequestMapping(value = "/refresh-cqtV1", produces = MediaType.APPLICATION_JSON_VALUE, method = RequestMethod.POST)
	@ResponseBody
	public BaseDTO refreshData(HttpServletRequest req, HttpSession session,
			@RequestAttribute(name = "transaction", value = "", required = false) String transaction,
			@RequestParam(value = "tokenTransaction", required = false, defaultValue = "") String tokenTransaction)
			throws Exception {
		BaseDTO dto = new BaseDTO(req);
		_id = commons.getParameterFromRequest(req, "_id").replaceAll("\\s", "");

		if ("".equals(_id)) {
			dto.setErrorCode(1);
			dto.setResponseData("Không tìm chứng từ cần ký.");
			return dto;
		}

		/* CHECK TOKEN */
		String token = session.getAttribute(Constants.SESSION_TYPE.SESSION_TOKEN_EXECUTE + _id) == null ? ""
				: session.getAttribute(Constants.SESSION_TYPE.SESSION_TOKEN_EXECUTE + _id).toString();
		session.removeAttribute(Constants.SESSION_TYPE.SESSION_TOKEN_EXECUTE + _id);
		if ("".equals(token) || !tokenTransaction.equals(token)) {
			dto.setErrorCode(1);
			dto.setResponseData("Token giao dịch không hợp lệ.");
			return dto;
		}

		String actionCode = Constants.MSG_ACTION_CODE.CHECK;
		CurrentUserProfile cup = getCurrentlyAuthenticatedPrincipal();
		Msg msg = dto.createMsg(cup, actionCode);
		HashMap<String, Object> hData = new HashMap<>();
		hData.put("_id", _id);
		msg.setObjData(hData);

		JSONRoot root = new JSONRoot(msg);
		MsgRsp rsp = restAPI.callAPINormal("/cttncn/crudV1", cup.getLoginRes().getToken(), HttpMethod.POST, root);
		MspResponseStatus rspStatus = rsp.getResponseStatus();
		if (rspStatus.getErrorCode() == 0) {
			dto.setErrorCode(0);
			dto.setResponseData("Lấy kết quả Chứng từ từ CQT thành công.");
		} else {
			dto.setErrorCode(rspStatus.getErrorCode());
			dto.setResponseData(rspStatus.getErrorDesc());
		}
		return dto;
	}

	@RequestMapping(value = "/history", method = { RequestMethod.POST })
	public String history(Locale locale, HttpServletRequest req, HttpSession session,
			@RequestAttribute(name = "transaction", value = "", required = false) String transaction) throws Exception {
		errorCode = "";
		errorDesc = "";

		CurrentUserProfile cup = getCurrentlyAuthenticatedPrincipal();
		_id = commons.getParameterFromRequest(req, "_id");
		String header = "Tra cứu lịch sử mã CQT";
		String action = "HISTORY";
		boolean isEdit = false;
		if ("|cttncn-historyV1|".indexOf(transaction) != -1)
			inquiryhistory(cup, locale, req, session, _id, action);

		req.setAttribute("_header_", header);
		req.setAttribute("_action_", action);
		req.setAttribute("_isedit_", isEdit);
		req.setAttribute("_id", _id);

		return "tncn/cttncn-historyV1";
	}

	private void inquiryhistory(CurrentUserProfile cup, Locale locale, HttpServletRequest req, HttpSession session,
			String _id, String action) throws Exception {
		if ("".equals(_id)) {
			errorCode = "NOT FOUND";
			errorDesc = "Không tìm thấy thông tin thông báo.";
			return;
		}
		BaseDTO baseDTO = new BaseDTO(req);
		JsonGridDTO grid = new JsonGridDTO();
		Msg msg = baseDTO.createMsg(cup, Constants.MSG_ACTION_CODE.INQUIRY);
		HashMap<String, String> hData = new HashMap<>();
		msg.setObjData(hData);
		JSONRoot root = new JSONRoot(msg);
		MsgRsp rsp = restAPI.callAPINormal("/cttncn/history/" + _id, cup.getLoginRes().getToken(), HttpMethod.POST,
				root);
		MspResponseStatus rspStatus = rsp.getResponseStatus();
		if (rspStatus.getErrorCode() == 0) {
			MsgPage page = rsp.getMsgPage();
			grid.setTotal(page.getTotalRows());
			StringBuilder sb = new StringBuilder();
			JsonNode jsonData = Json.serializer().nodeFromObject(rsp.getObjData());
			JsonNode rows = null;
			HashMap<String, String> hItem = null;
			LocalDateTime localdatetime = null;
			LocalDate localdate = null;
			List<Object> dshdon = new ArrayList<Object>();
			if (!jsonData.at("/rows").isMissingNode()) {
				rows = jsonData.at("/rows");
				for (JsonNode row : rows) {
					hItem = new HashMap<String, String>();
					hItem.put("STT", commons.getTextJsonNode(row.at("/STT")));
					localdatetime = null;
					String ngay = commons.getTextJsonNode(row.at("/Date"));
					int nngay = ngay.length();
					if (nngay == 19) {
						localdatetime = LocalDateTime.parse(ngay);
						ngay = commons.convertLocalDateTimeToString(localdatetime,
								Constants.FORMAT_DATE.FORMAT_DATE_TIME_WEB);
					} else {
						localdate = LocalDate.parse(ngay);
						ngay = commons.convertLocalDateTimeToString(localdate, Constants.FORMAT_DATE.FORMAT_DATE_WEB);
					}

					hItem.put("Date", ngay);
					hItem.put("MLoi", commons.getTextJsonNode(row.at("/MLoi")));
					hItem.put("MTLoi", commons.getTextJsonNode(row.at("/MTLoi")));
					dshdon.add(hItem);
				}
				req.setAttribute("DSHDon", commons.encodeStringBase64(Json.serializer().toString(dshdon)));
			}

		} else {
			errorDesc = rspStatus.getErrorDesc();
		}
	}

	@RequestMapping(value = "/check-data-send-all-cqtV1", produces = MediaType.APPLICATION_JSON_VALUE, method = RequestMethod.POST)
	@ResponseBody
	public BaseDTO checkDataSendCQTAll(Locale locale, HttpServletRequest req, HttpSession session,
			@RequestAttribute(name = "transaction", required = false, value = "") String transaction) throws Exception {
		String token = "";
		if (null != session.getAttribute(Constants.SESSION_TYPE.SESSION_TOKEN_EXECUTE)) {
			token = session.getAttribute(Constants.SESSION_TYPE.SESSION_TOKEN_EXECUTE).toString();
			session.removeAttribute(token);
		}

		session.removeAttribute(Constants.SESSION_TYPE.SESSION_TOKEN_EXECUTE);
		BaseDTO dto = new BaseDTO();
		String _token = commons.getParameterFromRequest(req, "_token").replaceAll("\\s", "");
		if ("".equals(_token)) {
			dto.setErrorCode(999);
			dto.setResponseData(Constants.MAP_ERROR.get(999));
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

	@RequestMapping(value = "/send-all-cqtV1", produces = MediaType.APPLICATION_JSON_VALUE, method = RequestMethod.POST)
	@ResponseBody
	public BaseDTO execDatAll(HttpServletRequest req, HttpSession session,
			@RequestAttribute(name = "transaction", value = "", required = false) String transaction,
			@RequestParam(value = "tokenTransaction", required = false, defaultValue = "") String tokenTransaction)
			throws Exception {
		BaseDTO dtoRes = new BaseDTO();

		CurrentUserProfile cup = getCurrentlyAuthenticatedPrincipal();

		/* CHECK TOKEN */
		String token = session.getAttribute(Constants.SESSION_TYPE.SESSION_TOKEN_EXECUTE) == null ? ""
				: session.getAttribute(Constants.SESSION_TYPE.SESSION_TOKEN_EXECUTE).toString();
		session.removeAttribute(Constants.SESSION_TYPE.SESSION_TOKEN_EXECUTE);
		if ("".equals(token) || !tokenTransaction.equals(token)) {
			dtoRes.setErrorCode(1);
			dtoRes.setResponseData("Token giao dịch không hợp lệ.");
			return dtoRes;
		}

		String _token = commons.getParameterFromRequest(req, "_token").replaceAll("\\s", "");
		if ("".equals(_token)) {
			dtoRes.setErrorCode(999);
			dtoRes.setResponseData(Constants.MAP_ERROR.get(999));
			return dtoRes;
		}

		String actionCode = Constants.MSG_ACTION_CODE.SEND_CQTALL;
		dtoRes = new BaseDTO(req);
		Msg msg = dtoRes.createMsg(cup, actionCode);
		HashMap<String, Object> hData = new HashMap<>();
		hData.put("_token", _token);
		msg.setObjData(hData);

		JSONRoot root = new JSONRoot(msg);
		MsgRsp rsp = restAPI.callAPINormal("/cttncn/crudV1", cup.getLoginRes().getToken(), HttpMethod.POST, root);
		MspResponseStatus rspStatus = rsp.getResponseStatus();
		if (rspStatus.getErrorCode() == 0) {
			dtoRes.setErrorCode(0);
			dtoRes.setResponseData("Gửi HĐ đến CQT thành công.");
		} else {
			dtoRes.setErrorCode(rspStatus.getErrorCode());
			dtoRes.setResponseData(rspStatus.getErrorDesc());
		}
		return dtoRes;
	}
	
	@RequestMapping(value = "/refresh-all-status-from-cqt",  produces = MediaType.APPLICATION_JSON_VALUE, method = RequestMethod.POST)
	@ResponseBody
	public BaseDTO refreshAllStatusFromCQT(Locale locale, HttpServletRequest req, HttpSession session,
			@RequestAttribute(name = "transaction", value = "", required = false) String transaction) throws Exception {
		BaseDTO dto = new BaseDTO();

		CurrentUserProfile cup = getCurrentlyAuthenticatedPrincipal();
		String _token = commons.getParameterFromRequest(req, "_token").replaceAll("\\s", "");
		if ("".equals(_id)) {
			dto.setErrorCode(1);
			dto.setErrorDesc(Constants.MAP_ERROR.get(999));
			return dto;
		}

		String actionCode = Constants.MSG_ACTION_CODE.CHECK_ALL;
		dto = new BaseDTO(req);
		Msg msg = dto.createMsg(cup, actionCode);
		HashMap<String, Object> hData = new HashMap<>();
		hData.put("_token", _token);

		msg.setObjData(hData);

		JSONRoot root = new JSONRoot(msg);
		MsgRsp rsp = restAPI.callAPINormal("/cttncn/crudV1", cup.getLoginRes().getToken(), HttpMethod.POST, root);
		MspResponseStatus rspStatus = rsp.getResponseStatus();
		if (rspStatus.getErrorCode() == 0) {
			dto.setErrorCode(0);
			dto.setResponseData("Lấy kết quả từ cqt thành công.");
		} else {
			dto.setErrorCode(rspStatus.getErrorCode());
			dto.setResponseData(rspStatus.getErrorDesc());
		}
		return dto;
	}
}
