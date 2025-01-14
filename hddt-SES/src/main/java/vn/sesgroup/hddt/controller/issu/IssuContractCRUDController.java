package vn.sesgroup.hddt.controller.issu;

import java.security.Principal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
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
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.context.WebApplicationContext;

import com.api.message.JSONRoot;
import com.api.message.Msg;
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
import vn.sesgroup.hddt.dto.LoginRes;
import vn.sesgroup.hddt.resources.RestAPIUtility;
import vn.sesgroup.hddt.utils.Constants;
import vn.sesgroup.hddt.utils.Json;

@Controller
@RequestMapping({ "/issu-contract-cre", 
	"/issu-contract-detail",
	"/issu-contract-del",
	"/issu-contract-edit",
	"/issu-contract-approve", 
	"/issu-contract-active",
	"/issu-contract-deactive",
	"/issu-contract-db" })
@Scope(value = WebApplicationContext.SCOPE_REQUEST)
public class IssuContractCRUDController extends AbstractController {
	private static final Logger log = LogManager.getLogger(IssuContractCRUDController.class);
	@Autowired
	RestAPIUtility restAPI;
	@Autowired
	RestTemplate restTemplate;
	private String errorDesc;
	private String _id;
	private String ma_taxcode;

	private String n;
	private String t;
	private String boss;
	private String p;
	private String a;

	private String e;
	private String w;
	private String cv;
	private String ac;
	private String an;
	private String bn;

	private String shd;
	private String slhd;
	private String ngayky;
	private String ngaytt;
	private String gc;
	private String hinhThucThanhToan;
	private String hinhThucThanhToanText;
	private String km;
	private String tongTienTruocThue;
	private String tongTienThueGtgt;
	private String tongTienDaCoThue;
	private String tienBangChu;
	private String dsSanPham;
	private String _token;
	private List<String> ids = null;
	private String _title;
	private String _content;

	private void LoadParameter(CurrentUserProfile cup, Locale locale, HttpServletRequest req, String action) {
		try {
			BaseDTO baseDTO = new BaseDTO(req);
			Msg msg = baseDTO.createMsg(cup, Constants.MSG_ACTION_CODE.LOAD_PARAMS);

			/* DANH SACH THAM SO */
			MsgParam msgParam = null;
			MsgParams msgParams = new MsgParams();

			msgParam = new MsgParam();
			msgParam.setId("param01");
			msgParam.setParam("DMPaymentType");
			msgParams.getParams().add(msgParam);

			msgParam = new MsgParam();
			msgParam.setId("param02");
			msgParam.setParam("DMPTax");
			msgParams.getParams().add(msgParam);
			/* END: DANH SACH THAM SO */
			msg.setObjData(msgParams);

			msgParam = new MsgParam();
			msgParam.setId("param03");
			msgParam.setParam("Issuer");

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
					req.setAttribute("map_paymenttype", hItem);
				}
				if (null != jsonData.at("/param02") && jsonData.at("/param02") instanceof ArrayNode) {
					hItem = new LinkedHashMap<String, String>();
					for (JsonNode o : jsonData.at("/param02")) {
						hItem.put(commons.getTextJsonNode(o.get("code")), commons.getTextJsonNode(o.get("name")));
					}
					req.setAttribute("map_tax", hItem);
				}
				if (null != jsonData.at("/param03") && jsonData.at("/param03") instanceof ArrayNode) {
					hItem = new LinkedHashMap<String, String>();
					for (JsonNode o : jsonData.at("/param03")) {
						hItem.put(commons.getTextJsonNode(o.get("TaxCode")), commons.getTextJsonNode(o.get("TaxCode")));

					}
					req.setAttribute("map_taxcode", hItem);
				}
			}

		} catch (Exception e) {
		}
	}

	private void inquiry(CurrentUserProfile cup, Locale locale, HttpServletRequest req, HttpSession session,
			String action) throws Exception {
		cup = getCurrentlyAuthenticatedPrincipal();
		_id = commons.getParameterFromRequest(req, "_id");
		ma_taxcode = commons.getParameterFromRequest(req, "TaxCode");
		BaseDTO baseDTO = new BaseDTO(req);
		Msg msg = baseDTO.createMsg(cup, Constants.MSG_ACTION_CODE.INQUIRY);
		HashMap<String, String> hData = new HashMap<>();
		msg.setObjData(hData);
		JSONRoot root = new JSONRoot(msg);

		MsgRsp rspinfo = restAPI.callAPINormal("/main/infoConfig", cup.getLoginRes().getToken(), HttpMethod.POST, root);
		MspResponseStatus rspStatusinfo = rspinfo.getResponseStatus();
		if (rspStatusinfo.getErrorCode() == 0) {
			JsonNode jsonData = Json.serializer().nodeFromObject(rspinfo.getObjData());
			req.setAttribute("NameAD", commons.getTextJsonNode(jsonData.at("/Name")));
			req.setAttribute("TaxCodeAD", commons.getTextJsonNode(jsonData.at("/TaxCode")));
			req.setAttribute("MainUserAD", commons.getTextJsonNode(jsonData.at("/MainUser")));
			req.setAttribute("PositionAD", commons.getTextJsonNode(jsonData.at("/Position")));
			req.setAttribute("AccountNumberAD", commons.getTextJsonNode(jsonData.at("/BankAccount/AccountNumber")));
			req.setAttribute("AccountNameAD", commons.getTextJsonNode(jsonData.at("/BankAccount/AccountName")));
			req.setAttribute("BankNameAD", commons.getTextJsonNode(jsonData.at("/BankAccount/BankName")));
			req.setAttribute("WebsiteAD", commons.getTextJsonNode(jsonData.at("/Website")));
			req.setAttribute("AddressAD", commons.getTextJsonNode(jsonData.at("/Address")));
			req.setAttribute("PhoneAD", commons.getTextJsonNode(jsonData.at("/Phone")));
			req.setAttribute("EmailAD", commons.getTextJsonNode(jsonData.at("/Email")));
		} else {
			rspStatusinfo.getErrorDesc();
		}

		if (_id != "") {
			MsgRsp rsp = restAPI.callAPINormal("/issu-contract/detail/" + _id, cup.getLoginRes().getToken(),
					HttpMethod.POST, root);

			MspResponseStatus rspStatus = rsp.getResponseStatus();
			if (rspStatus.getErrorCode() == 0) {
				JsonNode jsonData = Json.serializer().nodeFromObject(rsp.getObjData());

				req.setAttribute("SHDon", commons.getTextJsonNode(jsonData.at("/Contract/SHDon")));
				req.setAttribute("SLHDon", commons.getTextJsonNode(jsonData.at("/Contract/SLHDon")));
				req.setAttribute("KhuyenMai", commons.getTextJsonNode(jsonData.at("/Contract/KhuyenMai")));
				req.setAttribute("GhiChu", commons.getTextJsonNode(jsonData.at("/Contract/GhiChu")));
				req.setAttribute("NgayThanhToan",
						commons.convertLocalDateTimeToString(
								commons.convertLongToLocalDate(jsonData.at("/Contract/NgayThanhToan").asLong()),
								Constants.FORMAT_DATE.FORMAT_DATE_WEB));
				req.setAttribute("NgayKy",
						commons.convertLocalDateTimeToString(
								commons.convertLongToLocalDate(jsonData.at("/Contract/NgayKy").asLong()),
								Constants.FORMAT_DATE.FORMAT_DATE_WEB));
				req.setAttribute("HTTToanCode", commons.getTextJsonNode(jsonData.at("/Contract/HTTToanCode")));
				req.setAttribute("HTTToan", commons.getTextJsonNode(jsonData.at("/Contract/HTTToan")));

				req.setAttribute("_id", _id);
				req.setAttribute("Name", commons.getTextJsonNode(jsonData.at("/NMUA/Name")));
				req.setAttribute("TaxCode", commons.getTextJsonNode(jsonData.at("/NMUA/TaxCode")));
				req.setAttribute("MainUser", commons.getTextJsonNode(jsonData.at("/NMUA/MainUser")));
				req.setAttribute("Position", commons.getTextJsonNode(jsonData.at("/NMUA/Position")));
				req.setAttribute("AccountNumber",
						commons.getTextJsonNode(jsonData.at("/NMUA/BankAccount/AccountNumber")));
				req.setAttribute("AccountName", commons.getTextJsonNode(jsonData.at("/NMUA/BankAccount/AccountName")));
				req.setAttribute("BankName", commons.getTextJsonNode(jsonData.at("/NMUA/BankAccount/BankName")));
				req.setAttribute("Website", commons.getTextJsonNode(jsonData.at("/NMUA/Website")));
				req.setAttribute("Address", commons.getTextJsonNode(jsonData.at("/NMUA/Address")));
				req.setAttribute("Phone", commons.getTextJsonNode(jsonData.at("/NMUA/Phone")));
				req.setAttribute("Email", commons.getTextJsonNode(jsonData.at("/NMUA/Email")));

				HashMap<String, String> hItem = null;
				List<Object> prds = new ArrayList<Object>();
				if (!jsonData.at("/DSHHDVu").isMissingNode()) {
					for (JsonNode o : jsonData.at("/DSHHDVu")) {
						hItem = new LinkedHashMap<String, String>();

						hItem.put("STT", commons.getTextJsonNode(o.at("/STT")));
						hItem.put("ProductName", commons.getTextJsonNode(o.at("/ProductName")));
						hItem.put("Quantity", commons.getTextJsonNode(o.at("/Quantity")));
						hItem.put("Price", commons.getTextJsonNode(o.at("/Price")));
						hItem.put("Total", commons.getTextJsonNode(o.at("/Total")));
						hItem.put("VATRate", commons.getTextJsonNode(o.at("/VATRate")));
						hItem.put("VATAmount", commons.getTextJsonNode(o.at("/VATAmount")));
						hItem.put("Amount", commons.getTextJsonNode(o.at("/Amount")));
						hItem.put("Note", commons.getTextJsonNode(o.at("/Note")));

						switch (commons.getTextJsonNode(o.at("/Feature"))) {
						case "2":
							break;
						default:
							hItem.put("Quantity", o.at("/Quantity").isMissingNode() ? ""
									: commons.formatNumberReal(o.at("/Quantity").doubleValue()));
							hItem.put("Price", o.at("/Price").isMissingNode() ? ""
									: commons.formatNumberReal(o.at("/Price").doubleValue()));
							hItem.put("Total", o.at("/Total").isMissingNode() ? ""
									: commons.formatNumberReal(o.at("/Total").doubleValue()));
							if ("DETAIL".equals(action))
								hItem.put("VATRate",
										o.at("/VATRate").isMissingNode() ? ""
												: Constants.MAP_VAT
														.get(commons.formatNumberReal(o.at("/VATRate").doubleValue())));
							else
								hItem.put("VATRate", o.at("/VATRate").isMissingNode() ? ""
										: commons.formatNumberReal(o.at("/VATRate").doubleValue()));
							hItem.put("VATAmount", o.at("/VATAmount").isMissingNode() ? ""
									: commons.formatNumberReal(o.at("/VATAmount").doubleValue()));
							hItem.put("Amount", o.at("/Amount").isMissingNode() ? ""
									: commons.formatNumberReal(o.at("/Amount").doubleValue()));
							break;
						}
						if ("DETAIL".equals(action))
							hItem.put("Feature",
									Constants.MAP_PRD_FEATURE.get(commons.getTextJsonNode(o.at("/Feature"))));
						else
							hItem.put("Feature", commons.getTextJsonNode(o.at("/Feature")));
						prds.add(hItem);
					}
				}
				req.setAttribute("DSHHDVu", commons.encodeStringBase64(Json.serializer().toString(prds)));

				req.setAttribute("TgTCThue", jsonData.at("/TToan/TgTCThue").isMissingNode() ? ""
						: commons.formatNumberReal(jsonData.at("/TToan/TgTCThue").doubleValue()));
				req.setAttribute("TgTThue", jsonData.at("/TToan/TgTThue").isMissingNode() ? ""
						: commons.formatNumberReal(jsonData.at("/TToan/TgTThue").doubleValue()));
				req.setAttribute("TgTTTBSo", jsonData.at("/TToan/TgTTTBSo").isMissingNode() ? ""
						: commons.formatNumberReal(jsonData.at("/TToan/TgTTTBSo").doubleValue()));
				req.setAttribute("TgTTTBChu", commons.getTextJsonNode(jsonData.at("/TToan/TgTTTBChu")));

			} else {
				rspStatus.getErrorDesc();
			}
		} else {
			MsgRsp rsp = restAPI.callAPINormal("/main/issu/" + ma_taxcode, cup.getLoginRes().getToken(),
					HttpMethod.POST, root);
			MspResponseStatus rspStatus = rsp.getResponseStatus();
			if (rspStatus.getErrorCode() == 0) {
				JsonNode jsonData = Json.serializer().nodeFromObject(rsp.getObjData());

				req.setAttribute("Name", commons.getTextJsonNode(jsonData.at("/Name")));
				req.setAttribute("TaxCode", commons.getTextJsonNode(jsonData.at("/TaxCode")));
				req.setAttribute("MainUser", commons.getTextJsonNode(jsonData.at("/MainUser")));
				req.setAttribute("Position", commons.getTextJsonNode(jsonData.at("/Position")));
				req.setAttribute("AccountNumber", commons.getTextJsonNode(jsonData.at("/BankAccount/AccountNumber")));
				req.setAttribute("AccountName", commons.getTextJsonNode(jsonData.at("/BankAccount/AccountName")));
				req.setAttribute("BankName", commons.getTextJsonNode(jsonData.at("/BankAccount/BankName")));
				req.setAttribute("Website", commons.getTextJsonNode(jsonData.at("/Website")));
				req.setAttribute("Address", commons.getTextJsonNode(jsonData.at("/Address")));
				req.setAttribute("Phone", commons.getTextJsonNode(jsonData.at("/Phone")));
				req.setAttribute("Email", commons.getTextJsonNode(jsonData.at("/Email")));

			} else {
				rspStatus.getErrorDesc();
			}
		}

	}

	@RequestMapping(value = "/check", method = { RequestMethod.POST, RequestMethod.GET })
	public String check(Locale locale, Principal principal, HttpServletRequest req) throws Exception {
		req.setAttribute("_TitleView_", Constants.PREFIX_TITLE + " - Danh sách khách hàng");
		req.setAttribute("map_status", Constants.MAP_STATUS);
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

		if ("true".equals(isroot) && issu.isRoot() == true) {
			return "/common/search-issuer";
		} else {
			return "/user/main";
		}

	}

	@RequestMapping(value = "/checkdb", method = { RequestMethod.POST, RequestMethod.GET })
	public String checkdb(Locale locale, Principal principal, HttpServletRequest req) throws Exception {
		CurrentUserProfile cup = getCurrentlyAuthenticatedPrincipal();

		BaseDTO baseDTO = new BaseDTO(req);
		Msg msg = baseDTO.createMsg(cup, Constants.MSG_ACTION_CODE.INQUIRY);
		HashMap<String, String> hData = new HashMap<>();
		msg.setObjData(hData);
		JSONRoot root = new JSONRoot(msg);
		MsgRsp rsp = restAPI.callAPINormal("/issu-contract/checkdb", cup.getLoginRes().getToken(), HttpMethod.POST,
				root);
		MspResponseStatus rspStatus = rsp.getResponseStatus();
		if (rspStatus.getErrorCode() == 0) {
			JsonNode jsonData = Json.serializer().nodeFromObject(rsp.getObjData());

			req.setAttribute("SLHDTT", jsonData.at("/sltt").size());
			req.setAttribute("SLHDQL", jsonData.at("/slql").size());

		} else {
			rspStatus.getErrorDesc();
		}

		return "/issu/issu-contract-db";
	}

	@PostMapping(value = "/updatedb", produces = MediaType.APPLICATION_JSON_VALUE)
	@ResponseBody
	public BaseDTO execImportData(HttpServletRequest req, HttpSession session,
			@RequestParam(value = "transaction", required = false, defaultValue = "") String transaction,
			@RequestParam(value = "tokenTransaction", required = false, defaultValue = "") String tokenTransaction)
			throws Exception {
		BaseDTO dtoRes = new BaseDTO();

		CurrentUserProfile cup = getCurrentlyAuthenticatedPrincipal();
		dtoRes = new BaseDTO(req);
		Msg msg = dtoRes.createMsg(cup, Constants.MSG_ACTION_CODE.CREATED);
		JSONRoot root = new JSONRoot(msg);
		MsgRsp rsp = restAPI.callAPINormal("/issu-contract/updatedb", cup.getLoginRes().getToken(), HttpMethod.POST,
				root);

		MspResponseStatus rspStatus = rsp.getResponseStatus();
		if (rspStatus.getErrorCode() == 0) {
			dtoRes.setErrorCode(0);
			dtoRes.setResponseData("Import thông tin sản phẩm thành công.");
		} else {
			dtoRes.setErrorCode(rspStatus.getErrorCode());
			dtoRes.setResponseData(rspStatus.getErrorDesc());
		}
		return dtoRes;
	}

	@PostMapping(value = "/init")
	public String init(LoginRes us, Locale locale, HttpServletRequest req, HttpSession session,
			@RequestAttribute(name = "transaction", value = "", required = false) String transaction) throws Exception {
		CurrentUserProfile cup = getCurrentlyAuthenticatedPrincipal();
		us = cup.getLoginRes();

		String header = "Thêm mới hợp đồng";
		String action = "CREATE";
		boolean isEdit = false;

		switch (transaction) {
		case "issu-contract-cre":
			req.setAttribute("NgayKy",
					commons.convertLocalDateTimeToString(LocalDate.now(), Constants.FORMAT_DATE.FORMAT_DATE_WEB));
			header = "Thêm mới hợp đồng";
			action = "CREATE";
			isEdit = true;
			break;
		case "issu-contract-edit":
			header = "Thay đổi thông tin hợp đồng";
			action = "EDIT";
			isEdit = true;
			break;
		case "issu-contract-detail":
			header = "Chi tiết hợp đồng";
			action = "DETAIL";
			isEdit = false;
			break;
		default:
			break;
		}

		if ("|issu-contract-edit|issu-contract-detail".indexOf(transaction) != -1)
			req.setAttribute("_header_", header);
		req.setAttribute("_action_", action);
		req.setAttribute("_isedit_", isEdit);
		req.setAttribute("_id", _id);
		LoadParameter(cup, locale, req, action);
		inquiry(cup, locale, req, session, action);
		if (!"".equals(errorDesc))
			req.setAttribute("messageError", errorDesc);

		return "/issu/issu-contract-crud";
	}

	public BaseDTO checkDataToAccept(HttpServletRequest req, HttpSession session, String transaction,
			CurrentUserProfile cup) throws Exception {
		BaseDTO dto = new BaseDTO();
		dto.setErrorCode(0);

		_id = commons.getParameterFromRequest(req, "_id").replaceAll("\\s", "");
		cv = commons.getParameterFromRequest(req, "cv");
		e = commons.getParameterFromRequest(req, "e");
		w = commons.getParameterFromRequest(req, "w");
		ac = commons.getParameterFromRequest(req, "ac");
		an = commons.getParameterFromRequest(req, "an");
		bn = commons.getParameterFromRequest(req, "bn");

		n = commons.getParameterFromRequest(req, "n");
		boss = commons.getParameterFromRequest(req, "boss").replaceAll("\\s", "");
		t = commons.getParameterFromRequest(req, "t").trim().replaceAll("\\s+", " ");
		p = commons.getParameterFromRequest(req, "p").replaceAll("\\s", "");
		a = commons.getParameterFromRequest(req, "a");
		shd = commons.getParameterFromRequest(req, "shd").replaceAll("\\s", "");
		slhd = commons.getParameterFromRequest(req, "slhd").replaceAll("\\s", "");
		ngayky = commons.getParameterFromRequest(req, "ngay-ky").replaceAll("\\s", "");
		ngaytt = commons.getParameterFromRequest(req, "ngay-tt").replaceAll("\\s", "");
		gc = commons.getParameterFromRequest(req, "gc").replaceAll("\\s", "");
		hinhThucThanhToan = commons.getParameterFromRequest(req, "hinh-thuc-thanh-toan").replaceAll("\\s", "");
		hinhThucThanhToanText = commons.getParameterFromRequest(req, "hinh-thuc-thanh-toan-text").trim()
				.replaceAll("\\s+", " ");
		km = commons.getParameterFromRequest(req, "km").replaceAll("\\s", "");
		tongTienTruocThue = commons.getParameterFromRequest(req, "tong-tien-truoc-thue").replaceAll("\\s", "");
		tongTienThueGtgt = commons.getParameterFromRequest(req, "tong-tien-thue-gtgt").replaceAll("\\s", "");
		tongTienDaCoThue = commons.getParameterFromRequest(req, "tong-tien-da-co-thue").replaceAll("\\s", "");
		tienBangChu = commons.getParameterFromRequest(req, "tien-bang-chu").trim().replaceAll("\\s+", " ");
		dsSanPham = commons.getParameterFromRequest(req, "ds-san-pham").replaceAll("\\s", "");

		switch (transaction) {
		case "issu-contract-cre":
		case "issu-contract-edit":
			if ("".equals(shd)) {
				dto.setErrorCode(1);
				dto.getErrorMessages().add("Vui lòng chọn số hóa đơn.");
			}
			if ("".equals(slhd)) {
				dto.setErrorCode(1);
				dto.getErrorMessages().add("Vui lòng chọn số lượng hóa đơn.");
			}
			if ("".equals(hinhThucThanhToan)) {
				dto.setErrorCode(1);
				dto.getErrorMessages().add("Vui lòng chọn hình thức thanh toán.");
			}
			if ("".equals(ngayky)) {
				dto.setErrorCode(1);
				dto.getErrorMessages().add("Vui lòng chọn ngày ký.");
			} else if (!commons.checkLocalDate(ngayky, Constants.FORMAT_DATE.FORMAT_DATE_WEB)) {
				dto.setErrorCode(1);
				dto.getErrorMessages().add("Ngày ký hợp đồng không đúng định dạng.");
			} else if (commons.compareLocalDate(
					commons.convertStringToLocalDate(ngayky, Constants.FORMAT_DATE.FORMAT_DATE_WEB),
					LocalDate.now()) < 0) {
				dto.setErrorCode(1);
				dto.getErrorMessages().add("Ngày ký hợp đồng không được nhỏ hơn ngày hiện tại.");
			}

			if (commons.ToNumber(tongTienTruocThue) < 0) {
				dto.setErrorCode(1);
				dto.getErrorMessages().add("Tổng tiền trước thuế phải lớn hơn hoặc bằng 0.");
			}
			if (commons.ToNumber(tongTienThueGtgt) < 0) {
				dto.setErrorCode(1);
				dto.getErrorMessages().add("Tổng tiền không được nhỏ hơn 0.");
			}
			if (commons.ToNumber(tongTienDaCoThue) < 0) {
				dto.setErrorCode(1);
				dto.getErrorMessages().add("Tổng tiền sau thuế phải lớn hơn hoặc bằng 0.");
			}
			if ("".equals(tienBangChu)) {
				dto.setErrorCode(1);
				dto.getErrorMessages().add("Tiền bằng chữ không được rỗng.");
			}

			break;
		case "issu-contract-del":
			_token = commons.getParameterFromRequest(req, "_token").replaceAll("\\s", "");
			ids = null;
			try {
				ids = Json.serializer().fromJson(commons.decodeBase64ToString(_token),
						new TypeReference<List<String>>() {

						});
			} catch (Exception e) {
			}
			break;
		case "issu-contract-approve":
		case "issu-contract-active":
		case "issu-contract-deactive":
			_token = commons.getParameterFromRequest(req, "_token").replaceAll("\\s", "");
			ids = null;
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

		return dto;
	}

	@PostMapping(value = "/check-data-save", produces = MediaType.APPLICATION_JSON_VALUE)
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
		String messageConfirm = "Bạn có muốn thêm mới hợp đồng không?";
		switch (transaction) {
		case "issu-contract-cre":
			messageConfirm = "Bạn có muốn thêm mới hợp đồng không?";
			break;
		case "issu-contract-edit":
			messageConfirm = "Bạn có muốn thay đổi thông tin hợp đồng không?";
			break;
		case "issu-contract-approve":
			messageConfirm = "Bạn có muốn hủy/duyệt hợp đồng này không?";
			break;
		case "issu-contract-del":
			messageConfirm = "Bạn có muốn xóa thông tin hợp đồng không?";
			break;
		case "issu-contract-active":
			messageConfirm = "Bạn có muốn kích hoạt hợp đồng không?";
			break;
		case "issu-contract-deactive":
			messageConfirm = "Bạn có muốn hủy kích hoạt hợp đồng không?";
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

		if ("issu-contract-del".equals(transaction) || "issu-contract-approve".equals(transaction) || "issu-contract-active".equals(transaction) || "issu-contract-deactive".equals(transaction)) {
			if (null == ids || ids.size() == 0) {
				dto.setErrorCode(999);
				dto.setResponseData("Không tìm thấy danh sách hợp đồng.");
				return dto;
			}
		}

		JsonNode jsonNodeTmp = null;

		jsonNodeTmp = null;

		if ("issu-contract-del".equals(transaction) || "issu-contract-approve".equals(transaction) || "issu-contract-active".equals(transaction) || "issu-contract-deactive".equals(transaction)) {
			token = commons.csRandomAlphaNumbericString(30);
			session.setAttribute(Constants.SESSION_TYPE.SESSION_TOKEN_EXECUTE, token);

			HashMap<String, String> hInfo = new HashMap<String, String>();
			hInfo.put("CONFIRM", messageConfirm);
			hInfo.put("TOKEN", token);

			dto.setResponseData(hInfo);
			dto.setErrorCode(0);
			return dto;
		} else {
			try {
				jsonNodeTmp = Json.serializer().nodeFromJson(commons.decodeBase64ToString(dsSanPham));
			} catch (Exception e) {
				log.error(" >>>>> An exception occurred!", e);
			}

			if (null == jsonNodeTmp || jsonNodeTmp.size() == 0) {
				dto.setErrorCode(999);
				dto.setResponseData("Hợp đồng chưa có dữ liệu hàng hóa.");
				return dto;
			}

			/* KIEM TRA THONG TIN SAN PHAM */
			boolean check = true;
			int count = 0;
			JsonNode jsonNode = null;
			while (count < jsonNodeTmp.size() && check) {
				jsonNode = jsonNodeTmp.get(count);
				if ("".equals(commons.getTextJsonNode(jsonNode.at("/ProductName")))) {
					check = false;
					break;
				}
				count++;
			}
			if (!check) {
				dto.setErrorCode(999);
				dto.setResponseData("Vui lòng kiểm tra lại dữ liệu hàng hóa.");
				return dto;
			}
			/* END - KIEM TRA THONG TIN SAN PHAM */

			token = commons.csRandomAlphaNumbericString(30);
			session.setAttribute(Constants.SESSION_TYPE.SESSION_TOKEN_EXECUTE, token);

			HashMap<String, String> hInfo = new HashMap<String, String>();
			hInfo.put("CONFIRM", messageConfirm);
			hInfo.put("TOKEN", token);

			dto.setResponseData(hInfo);
			dto.setErrorCode(0);
			return dto;
		}
	}

	@PostMapping(value = "/save-data", produces = MediaType.APPLICATION_JSON_VALUE)
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

		if ("issu-contract-del".equals(transaction) || "issu-contract-approve".equals(transaction) || "issu-contract-active".equals(transaction) || "issu-contract-deactive".equals(transaction)) {
			if (null == ids || ids.size() == 0) {
				dtoRes.setErrorCode(999);
				dtoRes.setResponseData("Không tìm thấy danh sách hợp đồng.");
				return dtoRes;
			}
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

		if ("issu-contract-del".equals(transaction) || "issu-contract-approve".equals(transaction) || "issu-contract-active".equals(transaction) || "issu-contract-deactive".equals(transaction)) {
		
		}else {
			try {
				jsonNodeTmp = Json.serializer().nodeFromJson(commons.decodeBase64ToString(dsSanPham));
			} catch (Exception e) {
				log.error(" >>>>> An exception occurred!", e);
			}
			if (null == jsonNodeTmp || jsonNodeTmp.size() == 0) {
				dtoRes.setErrorCode(999);
				dtoRes.setResponseData("Hóa đơn chưa có dữ liệu hàng hóa.");
				return dtoRes;
			}

			boolean checkPrd = true;
			/* KIEM TRA TEN SP PHAI KHAC RONG */
			for (JsonNode o : jsonNodeTmp) {
				if ("".equals(commons.getTextJsonNode(o.at("/ProductName")))) {
					checkPrd = false;
					break;
				}
			}
			if (!checkPrd) {
				dtoRes.setErrorCode(999);
				dtoRes.setResponseData("Vui lòng kiểm tra lại dữ liệu hàng hóa.");
				return dtoRes;
			}

		}

		String actionCode = Constants.MSG_ACTION_CODE.CREATED;
		switch (transaction) {
		case "issu-contract-cre":
			actionCode = Constants.MSG_ACTION_CODE.CREATED;
			break;
		case "issu-contract-edit":
			actionCode = Constants.MSG_ACTION_CODE.MODIFY;
			break;
		case "issu-contract-approve":
			actionCode = Constants.MSG_ACTION_CODE.APPROVE;
			break;
		case "issu-contract-del":
			actionCode = Constants.MSG_ACTION_CODE.DELETE;
			break;
		case "issu-contract-active":
			actionCode = Constants.MSG_ACTION_CODE.ACTIVE;
			break;
		case "issu-contract-deactive":
			actionCode = Constants.MSG_ACTION_CODE.DEACTIVE;
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
		case "issu-contract-del":
		case "issu-contract-approve":
		case "issu-contract-active":
		case "issu-contract-deactive":
			hData.put("ids", ids);
			break;
		default:

			hData.put("_id", _id);
			hData.put("TaxCode", t);
			hData.put("Name", n);
			hData.put("Address", a);
			hData.put("Phone", p);
			hData.put("MainUser", boss);
			hData.put("Email", e);
			hData.put("Website", w);
			hData.put("AccountNumber", ac);
			hData.put("AccountName", an);
			hData.put("BankName", bn);
			hData.put("Position", cv);
			hData.put("SoHoaDon", shd);
			hData.put("SoLuongHoaDon", slhd);
			hData.put("NgayKy", ngayky);
			hData.put("NgayThanhToan", ngaytt);
			hData.put("GhiChu", gc);
			hData.put("KhuyenMai", km);
			hData.put("HinhThucThanhToan", hinhThucThanhToan);
			hData.put("HinhThucThanhToanText", hinhThucThanhToanText);
			hData.put("TongTienTruocThue", tongTienTruocThue);
			hData.put("TongTienThueGtgt", tongTienThueGtgt);
			hData.put("TongTienDaCoThue", tongTienDaCoThue);
			hData.put("TienBangChu", tienBangChu);
			hData.put("DSSanPham", jsonNodeTmp);
			hData.put("_title", _title);
			hData.put("_content", _content);
			break;
		}

		msg.setObjData(hData);
		JSONRoot root = new JSONRoot(msg);
		MsgRsp rsp = restAPI.callAPINormal("/issu-contract/crud", cup.getLoginRes().getToken(), HttpMethod.POST, root);
		MspResponseStatus rspStatus = rsp.getResponseStatus();
		if (rspStatus.getErrorCode() == 0) {
			dtoRes.setErrorCode(0);
			switch (transaction) {
			case "issu-contract-cre":
				dtoRes.setResponseData("Thêm mới thông tin hợp đồng thành công.");
				break;
			case "issu-contract-edit":
				dtoRes.setResponseData("Cập nhật thông tin hợp đồng thành công.");
				break;
			case "issu-contract-approve":
				dtoRes.setResponseData("Duyệt thông tin hợp đồng thành công.");
				break;
			case "issu-contract-del":
				dtoRes.setResponseData("Xóa thông tin hợp đồng thành công.");
				break;
			case "issu-contract-active":
				dtoRes.setResponseData("Kích hoạt hợp đồng thành công.");
				break;
			case "issu-contract-deactive":
				dtoRes.setResponseData("Hủy kích hoạt hợp đồng thành công.");
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

}
