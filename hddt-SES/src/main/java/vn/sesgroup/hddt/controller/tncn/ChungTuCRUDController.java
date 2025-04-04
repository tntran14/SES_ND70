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
import vn.sesgroup.hddt.resources.APIParams;
import vn.sesgroup.hddt.resources.RestAPIUtility;
import vn.sesgroup.hddt.utils.Constants;
import vn.sesgroup.hddt.utils.Json;
import vn.sesgroup.hddt.utils.SystemParams;
@Controller
@RequestMapping({
	"/cttncn-cre",
	"/cttncn-detail",
	"/cttncn-edit",
	"/cttncn-sign",
	"/cttncn-signAll",
	"/cttncn-del"
	
})
@Scope(value = WebApplicationContext.SCOPE_REQUEST)
public class ChungTuCRUDController extends AbstractController{
	private static final Logger log = LogManager.getLogger(ChungTuCRUDController.class);
	@Autowired RestAPIUtility restAPI;
	@Autowired RestTemplate restTemplate;
	private String errorCode;
	private String errorDesc;
	private String _id;
	private String name;
	private String code;
	private String address;
	private String sdtlh;
	private String contactEmail;
	private String emailcc;
	private String taxcode;
	private String optHTHDon;
	private String cccd;
	private String qt;
	private String cccddate;
	private String cccdaddress;
	private String kibc;
	private String tungay;
	private String denngay;
	
	private String ktn;
	private String tdtn;
	private String kdtnndkh;
	private String kbh;
	private String ttnkt;
	private String ttntt;
	private String sttndkt;
	private String mstncn;

	private String _token;
	private List<String> ids = null;
	private String fromDate;
	private String toDate;
	
	private void LoadParameter(CurrentUserProfile cup, HttpServletRequest req) {
		try {
			BaseDTO baseDTO = new BaseDTO(req);
			Msg msg = baseDTO.createMsg(cup, Constants.MSG_ACTION_CODE.LOAD_PARAMS);
			
			/*DANH SACH THAM SO*/
			HashMap<String, String> hashConds = null;
			ArrayList<HashMap<String, String>> conds = null;
			MsgParam msgParam = null;
			MsgParams msgParams = new MsgParams();
			
			msgParam = new MsgParam();
			msgParam.setId("param01");
			msgParam.setParam("DMMSTNCN");
			msgParams.getParams().add(msgParam);
			
			/*END: DANH SACH THAM SO*/
			msg.setObjData(msgParams);
			
			JSONRoot root = new JSONRoot(msg);
			MsgRsp rsp = restAPI.callAPINormal("/commons/get-full-params", cup.getLoginRes().getToken(), HttpMethod.POST, root);
			MspResponseStatus rspStatus = rsp.getResponseStatus();
			
			if(rspStatus.getErrorCode() == 0 && rsp.getObjData() != null) {
				LinkedHashMap<String, String> hItem = null;
				
				JsonNode jsonData = Json.serializer().nodeFromObject(rsp.getObjData());
			
				if(null != jsonData.at("/param01") && jsonData.at("/param01") instanceof ArrayNode) {
					hItem = new LinkedHashMap<String, String>();
					for(JsonNode o: jsonData.at("/param01")) {
						hItem.put(commons.getTextJsonNode(o.get("_id")), 
										commons.getTextJsonNode(o.get("KyHieu")) + "/" + 
										commons.getTextJsonNode(o.get("Nam")) + "/"+ 
										commons.getTextJsonNode(o.get("ChungTu")));
				}		
					req.setAttribute("map_mausotncn", hItem);
				}
			}
			
		}catch(Exception e) {}
	}
	
	@RequestMapping(value = "/init", method = {RequestMethod.POST})
	public String init(Locale locale, HttpServletRequest req, HttpSession session
			, @RequestAttribute(name = "transaction", value = "", required = false) String transaction) throws Exception{
		errorCode = "";
		errorDesc = "";
		
		_id = commons.getParameterFromRequest(req, "_id");
		CurrentUserProfile cup = getCurrentlyAuthenticatedPrincipal();
		
		String header = "Thêm mới chứng từ";
		String action = "CREATE";
		boolean isEdit = false;
		
		switch (transaction) {
		case "cttncn-cre":
			LoadParameter(cup, req);
			req.setAttribute("KyBaoCao",  LocalDate.now().get(ChronoField.YEAR));
			header = "Thêm mới chứng từ";
			action = "CREATE";
			isEdit = true;
			req.setAttribute("optHTHDon", "KCT");
			break;
		case "cttncn-detail":
			LoadParameter(cup, req);
			header = "Chi tiết thông tin chứng từ";
			action = "DETAIL";
			isEdit = false;
			break;
		case "cttncn-edit":
			LoadParameter(cup, req);
			header = "Thay đổi thông tin chứng từ";
			action = "EDIT";
			isEdit = true;
			break;
		case "cttncn-sign":
			header = "Ký";
			action = "SIGN";
			isEdit = false;
			break;
		default:
			break;
		}
		
		if("|cttncn-detail|cttncn-edit|".indexOf(transaction) != -1) {
			inquiry(cup, locale, req, session, _id, action);
		}
		LocalDate now = LocalDate.now();
		req.setAttribute("FromDate", commons.convertLocalDateTimeToString(now.with(ChronoField.DAY_OF_MONTH, 1), Constants.FORMAT_DATE.FORMAT_DATE_WEB));
		req.setAttribute("ToDate", commons.convertLocalDateTimeToString(now, Constants.FORMAT_DATE.FORMAT_DATE_WEB));
	
		
	
		
		req.setAttribute("_header_", header);
		req.setAttribute("_action_", action);
		req.setAttribute("_id", _id);
		req.setAttribute("_isedit_", isEdit);
		
		return "tncn/cttncn-crud";
	}
	
	private void inquiry(CurrentUserProfile cup, Locale locale, HttpServletRequest req, HttpSession session, String _id , String action) throws Exception{
		if("".equals(_id)) {
			errorCode = "NOT FOUND";
			errorDesc = "Không tìm thấy thông tin sản phẩm.";
			return;
		}
		BaseDTO baseDTO = new BaseDTO(req);
		Msg msg = baseDTO.createMsg(cup, Constants.MSG_ACTION_CODE.INQUIRY);
		HashMap<String, String> hData = new HashMap<>();
		msg.setObjData(hData);
		
		JSONRoot root = new JSONRoot(msg);
		MsgRsp rsp = restAPI.callAPINormal("/cttncn/detail/" + _id, cup.getLoginRes().getToken(), HttpMethod.POST, root);
		MspResponseStatus rspStatus = rsp.getResponseStatus();
		if(rspStatus.getErrorCode() == 0) {
			JsonNode jsonData = Json.serializer().nodeFromObject(rsp.getObjData());
			
			req.setAttribute("TaxCode", commons.getTextJsonNode(jsonData.at("/TaxCode")));
		
			req.setAttribute("Code", commons.getTextJsonNode(jsonData.at("/Code")));
			req.setAttribute("Name", commons.getTextJsonNode(jsonData.at("/Name")));
			req.setAttribute("ContactPhone", commons.getTextJsonNode(jsonData.at("/ContactPhone")));
			req.setAttribute("ContactEmail", commons.getTextJsonNode(jsonData.at("/ContactEmail")));
			req.setAttribute("EmailCC", commons.getTextJsonNode(jsonData.at("/EmailCC")));

			req.setAttribute("Address", commons.getTextJsonNode(jsonData.at("/Address")));
			req.setAttribute("TuNgay", commons.getTextJsonNode(jsonData.at("/TuNgay")));
			req.setAttribute("DenNgay", commons.getTextJsonNode(jsonData.at("/DenNgay")));
			req.setAttribute("KyBaoCao", commons.getTextJsonNode(jsonData.at("/KyBaoCao")));
			
			
			req.setAttribute("CCCD", commons.getTextJsonNode(jsonData.at("/CMND-CCCD/CCCD")));
			req.setAttribute("CCCDDATE", commons.getTextJsonNode(jsonData.at("/CMND-CCCD/CCCDDATE")));
			req.setAttribute("CCCDADDRESS", commons.getTextJsonNode(jsonData.at("/CMND-CCCD/CCCDADDRESS")));
			req.setAttribute("QuocTich", commons.getTextJsonNode(jsonData.at("/CMND-CCCD/QuocTich")));
			req.setAttribute("optHTHDon", commons.getTextJsonNode(jsonData.at("/CuTru")));
			
			
			req.setAttribute("KhoanThuNhap", commons.getTextJsonNode(jsonData.at("/TNCNKhauTru/KhoanThuNhap")));
		
			req.setAttribute("KhoanBaoHiem", commons.getTextJsonNode(jsonData.at("/TNCNKhauTru/KhoanBaoHiem")));
			req.setAttribute("KhoanTuThienNhanDaoKhuyenHoc", commons.getTextJsonNode(jsonData.at("/TNCNKhauTru/KhoanTuThienNhanDaoKhuyenHoc")));
			req.setAttribute("TongTNKhauTru", commons.getTextJsonNode(jsonData.at("/TNCNKhauTru/TongTNKhauTru")));
			req.setAttribute("TongTNTinhThue", commons.getTextJsonNode(jsonData.at("/TNCNKhauTru/TongTNTinhThue")));
			req.setAttribute("SoTienCaNhanKhauTru", commons.getTextJsonNode(jsonData.at("/TNCNKhauTru/SoTienCaNhanKhauTru")));
			req.setAttribute("MauSoTNCN", commons.getTextJsonNode(jsonData.at("/MauSoHD")));
			
			req.setAttribute("Department", commons.getTextJsonNode(jsonData.at("/Department")));
			
		}else {
			errorDesc = rspStatus.getErrorDesc();
		}
	}
	
	public BaseDTO checkDataToAccept(HttpServletRequest req, HttpSession session, String transaction
			, CurrentUserProfile cup) throws Exception{
		BaseDTO dto = new BaseDTO();
		dto.setErrorCode(0);
		
		_id = commons.getParameterFromRequest(req, "_id").replaceAll("\\s", "");
		name = commons.getParameterFromRequest(req, "name");
		code = commons.getParameterFromRequest(req, "code").trim().replaceAll("\\s+", " ");
		address = commons.getParameterFromRequest(req, "address").trim().replaceAll("\\s+", " ");
		taxcode = commons.getParameterFromRequest(req, "taxcode").trim().replaceAll("\\s+", " ");
		optHTHDon = commons.getParameterFromRequest(req, "optHTHDon").trim().replaceAll("\\s+", " ");
		cccd = commons.getParameterFromRequest(req, "cccd").trim().replaceAll("\\s+", " ");
		qt = commons.getParameterFromRequest(req, "qt").trim().replaceAll("\\s+", " ");
		cccddate = commons.getParameterFromRequest(req, "cccddate").trim().replaceAll("\\s+", " ");
		cccdaddress = commons.getParameterFromRequest(req, "cccdaddress").trim().replaceAll("\\s+", " ");
		kibc = commons.getParameterFromRequest(req, "kibc").trim().replaceAll("\\s+", " ");
		tungay= commons.getParameterFromRequest(req, "from-date").trim().replaceAll("\\s+", " ");
		denngay = commons.getParameterFromRequest(req, "to-date").trim().replaceAll("\\s+", " ");
		ktn = commons.getParameterFromRequest(req, "ktn").trim().replaceAll("\\s+", " ");
		tdtn = commons.getParameterFromRequest(req, "tdtn").trim().replaceAll("\\s+", " ");
		kbh = commons.getParameterFromRequest(req, "kbh").trim().replaceAll("\\s+", " ");
		ttnkt = commons.getParameterFromRequest(req, "ttnkt").trim().replaceAll("\\s+", " ");
		ttntt = commons.getParameterFromRequest(req, "ttntt").trim().replaceAll("\\s+", " ");
		sttndkt = commons.getParameterFromRequest(req, "sttndkt").trim().replaceAll("\\s+", " ");
		mstncn = commons.getParameterFromRequest(req, "mau-so-tncn").trim().replaceAll("\\s+", " ");
		sdtlh = commons.getParameterFromRequest(req, "sdtlh").trim().replaceAll("\\s+", " ");
		contactEmail = commons.getParameterFromRequest(req, "contactEmail").trim().replaceAll("\\s+", " ");
		emailcc = commons.getParameterFromRequest(req, "emailcc").trim().replaceAll("\\s+", " ");
		kdtnndkh = commons.getParameterFromRequest(req, "kdtnndkh").trim().replaceAll("\\s+", " ");

		if("cttncn-edit".equals(transaction)) {
			if("".equals(_id)) {
				dto.setErrorCode(1);
				dto.getErrorMessages().add("Không tìm thấy thông tin chứng từ.");
			}
		}
		switch (transaction) {
		case "cttncn-cre":
		case "cttncn-edit":
		
			if("".equals(name)) {
				dto.setErrorCode(1);
				dto.getErrorMessages().add("Vui lòng nhập tên.");
			}	
			if("".equals(kibc)) {
				dto.setErrorCode(1);
				dto.getErrorMessages().add("Vui lòng nhập Kì báo cáo.");
			}

			if("".equals(ktn)) {
				dto.setErrorCode(1);
				dto.getErrorMessages().add("Vui lòng nhập khoản thu nhập.");
			}
			
			if("".equals(ttnkt)) {
				dto.setErrorCode(1);
				dto.getErrorMessages().add("Vui lòng nhập tổng thu nhập chịu thuế phải khấu trừ.");
			}
			if("".equals(ttntt)) {
				dto.setErrorCode(1);
				dto.getErrorMessages().add("Vui lòng nhập tổng thu nhập tính thuế.");
			}	
			if("".equals(sttndkt)) {
				dto.setErrorCode(1);
				dto.getErrorMessages().add("Vui lòng nhập số thuế thu nhập cá nhân đã khấu trừ.");
			}
			if("".equals(mstncn)) {
				dto.setErrorCode(1);
				dto.getErrorMessages().add("Vui lòng chọn mẫu số thu nhập cá nhân.");
			}
			break;
		case "cttncn-del":
			_token = commons.getParameterFromRequest(req, "_token").replaceAll("\\s", "");
			ids = null;
			try {
				ids = Json.serializer().fromJson(commons.decodeBase64ToString(_token), new TypeReference<List<String>>() {
				});
			}catch(Exception e) {}
			break;
		case "cttncn-signAll":
			_token = commons.getParameterFromRequest(req, "_token").replaceAll("\\s", "");
			ids = null;
			try {
				ids = Json.serializer().fromJson(commons.decodeBase64ToString(_token), new TypeReference<List<String>>() {
				});
			}catch(Exception e) {}
			break;
		default:
			break;
		}
		
		return dto;
	}
	
	@RequestMapping(value = "/check-data-save",  produces = MediaType.APPLICATION_JSON_VALUE, method = RequestMethod.POST)
	@ResponseBody
	public BaseDTO execCheckDataToSave(Locale locale, HttpServletRequest req, HttpSession session
			, @RequestAttribute(name = "transaction", required = false, value = "") String transaction) throws Exception {
		String token = "";
		if (null != session.getAttribute(Constants.SESSION_TYPE.SESSION_TOKEN_EXECUTE)) {
			token = session.getAttribute(Constants.SESSION_TYPE.SESSION_TOKEN_EXECUTE).toString();
			session.removeAttribute(token);
		}
		session.removeAttribute(Constants.SESSION_TYPE.SESSION_TOKEN_EXECUTE);
		
		BaseDTO dto = new BaseDTO();
		String messageConfirm = "Bạn có muốn thêm mới chứng từ không?";
		switch (transaction) {
		case "cttncn-cre":
			messageConfirm = "Bạn có muốn thêm mới chứng từ không?";
			break;
		case "cttncn-edit":
			messageConfirm = "Bạn có muốn thay đổi thông tin chứng từ không?";
			break;
		case "cttncn-del":
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
		if(0 != dto.getErrorCode()) {
			dto.setErrorCode(999);
			dto.setResponseData(Constants.MAP_ERROR.get(999));
			return dto;
		}
		
		if("cttncn-del".equals(transaction)) {
			if(null == ids || ids.size() == 0) {
				dto.setErrorCode(999);
				dto.setResponseData("Không tìm thấy danh sách chứng từ cần xóa.");
				return dto;
			}
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
	
	@RequestMapping(value = "/save-data",  produces = MediaType.APPLICATION_JSON_VALUE, method = RequestMethod.POST)
	@ResponseBody
	public BaseDTO execSaveData(HttpServletRequest req, HttpSession session
			, @RequestAttribute(name = "transaction", value = "", required = false) String transaction
			, @RequestParam(value = "tokenTransaction", required = false, defaultValue = "") String tokenTransaction) throws Exception{
		BaseDTO dtoRes = new BaseDTO();
	
		CurrentUserProfile cup = getCurrentlyAuthenticatedPrincipal();
		dtoRes = checkDataToAccept(req, session, transaction, cup);
		if(0 != dtoRes.getErrorCode()) {
			dtoRes.setErrorCode(999);
			dtoRes.setResponseData(Constants.MAP_ERROR.get(999));
			return dtoRes;
		}
		
		/*CHECK TOKEN*/
		String token = session.getAttribute(Constants.SESSION_TYPE.SESSION_TOKEN_EXECUTE) == null ? ""
				: session.getAttribute(Constants.SESSION_TYPE.SESSION_TOKEN_EXECUTE).toString();
		session.removeAttribute(Constants.SESSION_TYPE.SESSION_TOKEN_EXECUTE);
		if ("".equals(token) || !tokenTransaction.equals(token)) {
			dtoRes.setErrorCode(1);
			dtoRes.setResponseData("Token giao dịch không hợp lệ.");
			return dtoRes;
		}
		/*END: CHECK TOKEN*/
		
		String actionCode = Constants.MSG_ACTION_CODE.CREATED;
		switch (transaction) {
		case "cttncn-cre": actionCode = Constants.MSG_ACTION_CODE.CREATED; break;
		case "cttncn-edit": actionCode = Constants.MSG_ACTION_CODE.MODIFY; break;
		case "cttncn-del": actionCode = Constants.MSG_ACTION_CODE.DELETE; break;
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
		case "cttncn-del":
			hData.put("ids", ids);
			break;
		default:
			hData.put("_id", _id);
			hData.put("Name", name);
			hData.put("Code", code);
			hData.put("Address", address);
			hData.put("ContactPhone", sdtlh);
			hData.put("ContactEmail", contactEmail);
			hData.put("EmailCC", emailcc);
			hData.put("Taxcode", taxcode);
			hData.put("CuTru", optHTHDon);
			hData.put("CCCD", cccd);
			hData.put("QuocTich", qt);
			hData.put("CCCDDATE", cccddate);
			hData.put("CCCDADDRESS", cccdaddress);
			hData.put("KyBaoCao", kibc);
			hData.put("TuNgay", tungay);
			hData.put("DenNgay", denngay);
			
			
			hData.put("KhoanThuNhap", ktn);
			hData.put("KhoanTuThienNhanDaoKhuyenHoc", kdtnndkh);
			hData.put("DateThuNhap", tdtn);
			hData.put("KhoanBaoHiem", kbh);
			hData.put("TongTNKhauTru", ttnkt);
			hData.put("TongTNTinhThue", ttntt);
			hData.put("SoTienCaNhanKhauTru", sttndkt);
			hData.put("MauSoTNCN", mstncn);
			break;
		}
		
		msg.setObjData(hData);
		JSONRoot root = new JSONRoot(msg);
		MsgRsp rsp = restAPI.callAPINormal("/cttncn/crud", cup.getLoginRes().getToken(), HttpMethod.POST, root);
		MspResponseStatus rspStatus = rsp.getResponseStatus();
		if(rspStatus.getErrorCode() == 0) {
		
			dtoRes.setErrorCode(0);
			switch (transaction) {
			case "cttncn-cre":
				dtoRes.setResponseData("Thêm mới thông tin chứng từ thành công.");
				break;
			case "cttncn-edit":
				dtoRes.setResponseData("Cập nhật thông tin chứng từ thành công.");
				break;
			case "cttncn-del":
				dtoRes.setResponseData("Xóa danh sách chứng từ thành công.");
				break;
			default:
				dtoRes.setResponseData("Giao dịch thành công.");
				break;
			}
		}else {
			dtoRes.setErrorCode(rspStatus.getErrorCode());
			dtoRes.setResponseData(rspStatus.getErrorDesc());
		}
		
		return dtoRes;
	}
	
	
	
	
	
	
	
	
	
	@RequestMapping(value = "/check-data-sign",  produces = MediaType.APPLICATION_JSON_VALUE, method = RequestMethod.POST)
	@ResponseBody
	public BaseDTO execCheckDataToSign(Locale locale, HttpServletRequest req, HttpSession session
			, @RequestAttribute(name = "transaction", value = "", required = false) String transaction) throws Exception {
		String token = "";
		if (null != session.getAttribute(Constants.SESSION_TYPE.SESSION_TOKEN_EXECUTE)) {
			token = session.getAttribute(Constants.SESSION_TYPE.SESSION_TOKEN_EXECUTE).toString();
			session.removeAttribute(token);
		}
		session.removeAttribute(Constants.SESSION_TYPE.SESSION_TOKEN_EXECUTE);
		
		CurrentUserProfile cup = getCurrentlyAuthenticatedPrincipal();
		BaseDTO dto = new BaseDTO();
		_id = commons.getParameterFromRequest(req, "_id").replaceAll("\\s", "");
		
		if("".equals(_id)) {
			dto.setErrorCode(1);
			dto.setResponseData("Không tìm chứng từ cần ký.");
			return dto;
		}
		
		/*LAY THONG TIN DU LIEU XML VE SERVER WEB*/
		dto = new BaseDTO(req);
		Msg msg = dto.createMsg(cup, Constants.MSG_ACTION_CODE.CREATED);
		HashMap<String, Object> hData = new HashMap<>();
		hData.put("_id", _id);
		msg.setObjData(hData);
		JSONRoot root = new JSONRoot(msg);
		
		FileInfo fileInfo = restAPI.callAPIGetFileInfo("/cttncn/get-file-for-sign", cup.getLoginRes().getToken(), HttpMethod.POST, root);
		if(null == fileInfo || null == fileInfo.getContentFile()) {
			dto.setErrorCode(999);
			dto.setResponseData("Không tìm thấy dữ liệu hóa đơn.");
			return dto;
		}
		
		/*THONG TIN TEN FILE*/
		token = commons.convertLocalDateTimeToString(LocalDateTime.now(), Constants.FORMAT_DATE.FORMAT_DATETIME_DB_FULL) + "-" + commons.csRandomAlphaNumbericString(5);
		token += ".xml";
		File file = new File(SystemParams.DIR_TMP_SAVE_FILES);
		file.mkdirs();
		FileUtils.writeByteArrayToFile(new File(SystemParams.DIR_TMP_SAVE_FILES, token), fileInfo.getContentFile());
		/*END - LAY THONG TIN DU LIEU XML VE SERVER WEB*/
		
		HashMap<String, String> hInfo = new HashMap<String, String>();
		hInfo.put("TOKEN", token);
		
		dto.setResponseData(hInfo);
		dto.setErrorCode(0);
		return dto;
	}

	
	
	
	
	
	
	public BaseDTO checkDataToSign(HttpServletRequest req, HttpSession session
			, String transaction, CurrentUserProfile cup) throws Exception{
		BaseDTO dto = new BaseDTO();
		dto.setErrorCode(0);
		
		return dto;
	}
	
	@RequestMapping(
			value = "/signFile"
			, produces = MediaType.APPLICATION_JSON_VALUE
			, method = RequestMethod.POST
			, consumes = MediaType.MULTIPART_FORM_DATA_VALUE
		)
	@ResponseBody
	public BaseDTO processSignFile(HttpServletRequest req, HttpSession session
			, @RequestAttribute(name = "transaction", value = "", required = false) String transaction
			, @RequestParam("XMLFileSigned") MultipartFile multipartFile
			, @RequestParam("certificate") String certificate
			) throws Exception{
		BaseDTO dtoRes = new BaseDTO();
		
		CurrentUserProfile cup = getCurrentlyAuthenticatedPrincipal();
		dtoRes = checkDataToSign(req, session, transaction, cup);
		if(0 != dtoRes.getErrorCode()) {
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
		
		/*CONNECT TO API*/
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
		
		/*ADD DU LIEU XML DA KY*/
		fileMap = new LinkedMultiValueMap<String, String>();
		contentDisposition = ContentDisposition.builder("form-data").name("XMLFileSigned").filename("cttncn-signed.xml").build();
		fileMap.add(HttpHeaders.CONTENT_DISPOSITION, contentDisposition.toString());
		fileEntity = new HttpEntity<byte[]>(multipartFile.getBytes(), fileMap);
		body.add("XMLFileSigned", fileEntity);
		
		HttpEntity<MultiValueMap<String, Object>> requestBody = new HttpEntity<>(body, headers);
		String url = "/cttncn/sign-single";
		ResponseEntity<MsgRsp> result = restTemplate.exchange(APIParams.HTTP_URI + url, HttpMethod.POST, requestBody, MsgRsp.class);
		/*END - CONNECT TO API*/		
		if (result.getStatusCode() == org.springframework.http.HttpStatus.OK) {
			MsgRsp rsp = result.getBody();
			MspResponseStatus rspStatus = rsp.getResponseStatus();
			if(rspStatus.getErrorCode() == 0) {
				dtoRes.setErrorCode(0);
				dtoRes.setResponseData(rsp.getObjData());
			}else {
				dtoRes = new BaseDTO(rspStatus.getErrorCode(), rspStatus.getErrorDesc());
			}
		}else {
			dtoRes = new BaseDTO(result.getStatusCode().value(), "Thực hiện ký hóa đơn không thành công.");
		}
		return dtoRes;
	}
	
	
	
	
	@RequestMapping(value = "/check-data-signAll", produces = MediaType.APPLICATION_JSON_VALUE, method = RequestMethod.POST)
	@ResponseBody
	public BaseDTO signAll(Locale locale, HttpServletRequest req, HttpSession session,
			@RequestAttribute(name = "transaction", value = "", required = false) String transaction) throws Exception {
		String token = "";
		if (null != session.getAttribute(Constants.SESSION_TYPE.SESSION_TOKEN_EXECUTE)) {
			token = session.getAttribute(Constants.SESSION_TYPE.SESSION_TOKEN_EXECUTE).toString();
			session.removeAttribute(token);
		}
		token = commons.csRandomAlphaNumbericString(5);
		session.removeAttribute(Constants.SESSION_TYPE.SESSION_TOKEN_EXECUTE);
		CurrentUserProfile cup = getCurrentlyAuthenticatedPrincipal();
		BaseDTO dto = new BaseDTO();
		HashMap<String, String> hInfo = new HashMap<String, String>();
		FileInfo fileInfo = null;

		switch (transaction) {
		case "cttncn-signAll":
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
		int dem = 0;
		String tokens = "";
		ZipOutputStream zout = null;
		FileOutputStream fos = null;

		if ("".equals(_id)) {
			dto.setErrorCode(1);
			dto.setResponseData("Không tìm thấy hóa đơn cần ký.");
			return dto;
		}
		
		dem = ids.size();
		
//		if(dem>20) {
//			dto.setErrorCode(1);
//			dto.setResponseData("Ký hàng loạt chỉ hỗ trợ ký tối đa 20 hóa đơn.");
//			return dto;
//		}

		/* LAY THONG TIN DU LIEU XML VE SERVER WEB */
		dto = new BaseDTO(req);
		Msg msg = dto.createMsg(cup, Constants.MSG_ACTION_CODE.CREATED);
		HashMap<String, Object> hData = new HashMap<>();
		hData.put("id", ids);
		hData.put("soLuong", dem);
		msg.setObjData(hData);
		JSONRoot root = new JSONRoot(msg);

		fileInfo = restAPI.callAPIGetFileInfo("/cttncn/get-file-for-signAll", cup.getLoginRes().getToken(),
				HttpMethod.POST, root);
		String check = "error";
		String enoughSL = "Not Enough";
		String MS = "Not MS";
		String CT = "Not CT";
	
		if(fileInfo.getCheck().equals(enoughSL)) {
			dto.setErrorCode(999);
			dto.setResponseData("Số chứng từ còn lại không đủ để ký.");
			return dto;
		}
		if(fileInfo.getCheck().equals(MS)) {
			dto.setErrorCode(999);
			dto.setResponseData("Không tìm thấy thông tin mẫu số.");
			return dto;
		}
		if(fileInfo.getCheck().equals(CT)) {
			dto.setErrorCode(999);
			dto.setResponseData("Chứng từ đã được ký!!!");
			return dto;
		}
	
		if (null == fileInfo || null == fileInfo.getContentFile()) {
			dto.setErrorCode(999);
			dto.setResponseData("Số hóa đơn còn lại không đủ để ký.");
			return dto;
		}
		
		if(fileInfo.getFormIssueInvoiceID() != "" && fileInfo.getCheck().equals(check))
		{
			dto.setErrorCode(999);
			dto.setResponseData("Ký hàng loạt chỉ hỗ trợ kí cùng 1 mãu số.");
			return dto;
		}
		
		
		/*THONG TIN TEN FILE*/
		token = commons.convertLocalDateTimeToString(LocalDateTime.now(), Constants.FORMAT_DATE.FORMAT_DATETIME_DB_FULL) + "-" + commons.csRandomAlphaNumbericString(5);
		token += ".xml";
		File file = new File(SystemParams.DIR_TMP_SAVE_FILES);
		file.mkdirs();
		/*END - LAY THONG TIN DU LIEU XML VE SERVER WEB*/
	

		String token1 = "";
		
		String t = commons.convertLocalDateTimeToString(LocalDateTime.now(),
				Constants.FORMAT_DATE.FORMAT_DATETIME_DB_FULL);
		// VONG LAP XML TRONG ZIP
		List<GetXMLInfoXMLDTO> arrFileInfos = fileInfo.getArrFileInfos();
	
		String SHDon = "";
		int SHDon_1 = 0;
		if(arrFileInfos!=null) {
			arrFileInfos = fileInfo.getArrFileInfos();
			fos = new FileOutputStream(new File(file, token  + ".zip"));
			zout = new ZipOutputStream(fos);
			for (GetXMLInfoXMLDTO o : arrFileInfos) {
			SHDon_1 = o.getShd();
				SHDon = SHDon + SHDon_1+ ","; 
				zout.putNextEntry(new ZipEntry(o.getFileName()));
				zout.write(o.getFileData());
				zout.closeEntry();
			}
			zout.close();
			fos.close();
			// END VONG LAP
			
			String SHD =  SHDon.substring(0, SHDon.length() - 1);
		
		
			
			HashMap<String, Object> hR = new HashMap<String, Object>();
			hR.put("Token", token);
			hR.put("Time", t);
			hR.put("TaxCode",cup.getUsername());
			hR.put("Numbers",fileInfo.getNumbers());
			hR.put("FormIssueInvoiceID",fileInfo.getFormIssueInvoiceID());
			dto.setResponseData(hR);
			
			
			
			
		}else {
			dto.setErrorCode(999);
			dto.setResponseData("Không tìm thấy dữ liệu hóa đơn.");
			return dto;
		}		
		return dto;
	}


	
	@RequestMapping(
			value = "/signFileAll"
			, produces = MediaType.APPLICATION_JSON_VALUE
			, method = RequestMethod.POST
			, consumes = MediaType.MULTIPART_FORM_DATA_VALUE
		)
	@ResponseBody
	public BaseDTO processSignFileAll(HttpServletRequest req, HttpSession session
			, @RequestAttribute(name = "transaction", value = "", required = false) String transaction
			, @RequestParam(value = "zipFile", required = false) MultipartFile multipartFile
			, @RequestParam(value = "Numbers", required = true) String numbers
			, @RequestParam(value = "FormIssueInvoiceID", required = true) String formIssueInvoiceID
			, @RequestParam(value = "Certificate", required = true) String certificate
			) throws Exception{
		BaseDTO dtoRes = new BaseDTO();
		
		CurrentUserProfile cup = getCurrentlyAuthenticatedPrincipal();
		dtoRes = checkDataToSign(req, session, transaction, cup);
		if(0 != dtoRes.getErrorCode()) {
			dtoRes.setErrorCode(999);
			dtoRes.setResponseData(Constants.MAP_ERROR.get(999));
			return dtoRes;
		}
		String luu = cup.getUsername() +"/"+ formIssueInvoiceID;
		dtoRes = new BaseDTO(req);
		Msg msg = dtoRes.createMsg(cup, Constants.MSG_ACTION_CODE.SIGNED);
		HashMap<String, Object> hData = new HashMap<>();
		
		msg.setObjData(hData);
		JSONRoot root = new JSONRoot(msg);
		
//		FileCopyUtils.copy(multipartFile.getBytes(), Paths.get(SystemParams.DIR_TMP_SAVE_FILES, "tmp.xml").toFile());
		
		/*CONNECT TO API*/
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
		
		/*ADD DU LIEU XML DA KY*/
		fileMap = new LinkedMultiValueMap<String, String>();
		contentDisposition = ContentDisposition.builder("form-data").name("zipFile").filename(luu).build();
		fileMap.add(HttpHeaders.CONTENT_DISPOSITION, contentDisposition.toString());
		fileEntity = new HttpEntity<byte[]>(multipartFile.getBytes(), fileMap);
		body.add("zipFile", fileEntity);
		body.add("Ten", luu);
		HttpEntity<MultiValueMap<String, Object>> requestBody = new HttpEntity<>(body, headers);
		String url = "/cttncn/signAll";
		ResponseEntity<MsgRsp> result = restTemplate.exchange(APIParams.HTTP_URI + url, HttpMethod.POST, requestBody, MsgRsp.class);
		/*END - CONNECT TO API*/		
		if (result.getStatusCode() == org.springframework.http.HttpStatus.OK) {
			MsgRsp rsp = result.getBody();
			MspResponseStatus rspStatus = rsp.getResponseStatus();
			if(rspStatus.getErrorCode() == 0) {
				dtoRes.setErrorCode(0);
				dtoRes.setResponseData("Thực hiện ký hóa đơn không thành công.");
			}else {
				dtoRes = new BaseDTO(rspStatus.getErrorCode(), rspStatus.getErrorDesc());
			}
		}else {
			dtoRes = new BaseDTO(result.getStatusCode().value(), "Thực hiện ký hóa đơn không thành công.");
		}
		return dtoRes;
	}
	

	
	
	
}
