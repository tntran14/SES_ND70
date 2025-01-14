package vn.sesgroup.hddt.controller.einvoice_mtt;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;

import javax.net.ssl.HttpsURLConnection;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
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
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;

import vn.sesgroup.hddt.controller.AbstractController;
import vn.sesgroup.hddt.dto.BaseDTO;
import vn.sesgroup.hddt.dto.CurrentUserProfile;
import vn.sesgroup.hddt.dto.FileInfo;
import vn.sesgroup.hddt.dto.IssuerInfo;
import vn.sesgroup.hddt.dto.JsonGridDTO;
import vn.sesgroup.hddt.dto.LoginRes;
import vn.sesgroup.hddt.resources.APIParams;
import vn.sesgroup.hddt.resources.RestAPIUtility;
import vn.sesgroup.hddt.utils.Constants;
import vn.sesgroup.hddt.utils.Json;
import vn.sesgroup.hddt.utils.SystemParams;

@Controller
@RequestMapping({

	 "/einvoice_mtt_cqt-detail"

})
@Scope(value = WebApplicationContext.SCOPE_REQUEST)
public class EInvoiceMTTResponseCQTCRUDController extends AbstractController{
	private static final Logger log = LogManager.getLogger(EInvoiceMTTResponseCQTCRUDController.class);
	@Autowired RestAPIUtility restAPI;
	@Autowired RestTemplate restTemplate;
	
	private String errorCode;
	private String errorDesc;
	private String _id;
	private String _id_tt_dc;
	private String mauSoHdon;
	private String loaiHdon;
	private String loaiHdonText;
	private String maHoaDon;
	private String tenLoaiHd;
	private String ngayLap;
	private String hinhThucThanhToan;
	private String hinhThucThanhToanText;
//	private String chkXuatTheoLoaiTienTt;
	private String khMst;
	private String khMKHang;
	private String khCCCDan;
	private String khHoTenNguoiMua;
	private String khTenDonVi;
	private String khDiaChi;
	private String khEmail;
	private String khEmailCC;
	private String khSoDt;
	private String khSoTk;
	private String khTkTaiNganHang;
	private String tongTienTruocThue;
	private String loaiTienTt;
	private String tyGia;
	private String tongTienThueGtgt;
	private String tongTienDaCoThue;
	private String tongTienQuyDoi;
	private String tienBangChu;
	private String Param;
	private String dsSanPham;
	private String _token;
	private String checkProductExtension;
	private String checkProductExtension1;
	private List<String> ids = null;

	
	private void LoadParameter(CurrentUserProfile cup, Locale locale, HttpServletRequest req, String action) {
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
			msgParam.setParam("DMPaymentType");
			msgParams.getParams().add(msgParam);
			
			msgParam = new MsgParam();
			msgParam.setId("param02");
			msgParam.setParam("DMMauSoKyHieuForCreate");
			msgParams.getParams().add(msgParam);
			
			msgParam = new MsgParam();
			msgParam.setId("param03");
			msgParam.setParam("DMCurrencies");
			msgParams.getParams().add(msgParam);
			
			
			msgParam = new MsgParam();
			msgParam.setId("param04");
			msgParam.setParam("UserConFig");
			msgParams.getParams().add(msgParam);
			
			
			msgParam = new MsgParam();
			msgParam.setId("param05");
			msgParam.setParam("DMLoaiHD");
			msgParams.getParams().add(msgParam);
			/*END: DANH SACH THAM SO*/
			msg.setObjData(msgParams);
			
			JSONRoot root = new JSONRoot(msg);
			MsgRsp rsp = restAPI.callAPINormal("/commons/get-full-params", cup.getLoginRes().getToken(), HttpMethod.POST, root);
			MspResponseStatus rspStatus = rsp.getResponseStatus();
			String KHHDon = "";
			if(rspStatus.getErrorCode() == 0 && rsp.getObjData() != null) {
				LinkedHashMap<String, String> hItem = null;
				
				JsonNode jsonData = Json.serializer().nodeFromObject(rsp.getObjData());
				if(null != jsonData.at("/param01") && jsonData.at("/param01") instanceof ArrayNode) {
					hItem = new LinkedHashMap<String, String>();
					for(JsonNode o: jsonData.at("/param01")) {
						hItem.put(commons.getTextJsonNode(o.get("code")), commons.getTextJsonNode(o.get("name")));
					}
					req.setAttribute("map_paymenttype", hItem);
				}
				if(null != jsonData.at("/param02") && jsonData.at("/param02") instanceof ArrayNode) {
					hItem = new LinkedHashMap<String, String>();
					for(JsonNode o: jsonData.at("/param02")) {													
						KHHDon = commons.getTextJsonNode(o.get("KHHDon"));	
						char words=KHHDon.charAt(KHHDon.length() - 3);
						String s=String.valueOf(words);  
						if("M".equals(s)) {
							hItem.put(commons.getTextJsonNode(o.get("_id")), commons.getTextJsonNode(o.get("KHMSHDon")) + commons.getTextJsonNode(o.get("KHHDon")));	
						}						
				}
					req.setAttribute("map_mausokyhieu", hItem);
				}
				if(null != jsonData.at("/param03") && jsonData.at("/param03") instanceof ArrayNode) {
					hItem = new LinkedHashMap<String, String>();
					for(JsonNode o: jsonData.at("/param03")) {
						hItem.put(commons.getTextJsonNode(o.get("code")), commons.getTextJsonNode(o.get("code")));
						if(action.equals("CREATE") && null != o.get("IsDefault") && o.get("IsDefault").asBoolean(false)) {
							loaiTienTt = commons.getTextJsonNode(o.get("code"));
							req.setAttribute("DVTTe", loaiTienTt);
						}
					}
					req.setAttribute("map_currencies", hItem);
				}
				
				if(null != jsonData.at("/param04") && jsonData.at("/param04") instanceof ArrayNode) {
					hItem = new LinkedHashMap<String, String>();
					for(JsonNode o: jsonData.at("/param04")) {
//					hItem.put(commons.getTextJsonNode(o.get("VND")), commons.getTextJsonNode(o.get("USD")));	
					req.setAttribute("userconfig_vnd", commons.getTextJsonNode(o.get("VND")));
						req.setAttribute("userconfig_usd", commons.getTextJsonNode(o.get("USD")));
					}
				}else {
					req.setAttribute("userconfig_vnd", "2");
					req.setAttribute("userconfig_usd", "2");
				}
				
				if(null != jsonData.at("/param05") && jsonData.at("/param05") instanceof ArrayNode) {
					hItem = new LinkedHashMap<String, String>();
					for(JsonNode o: jsonData.at("/param05")) {
						hItem.put(commons.getTextJsonNode(o.get("Code")), commons.getTextJsonNode(o.get("Name")));
					}
					req.setAttribute("map_loai_hd", hItem);
				}
			}
			
		}catch(Exception e) {}
	}
	
	@RequestMapping(value = "/reset",  method = RequestMethod.POST)
	public void execResetProducts(Locale locale, HttpServletRequest req, HttpSession session) {
	session.removeAttribute(Constants.SESSION_TYPE.SESSION_FORM_ACTION);
	}
	@RequestMapping(value = {"/init", "/init-dc-tt"}, method = {RequestMethod.POST})
	public String init(Locale locale, HttpServletRequest req, HttpSession session
			, @RequestAttribute(name = "transaction", value = "", required = false) String transaction
			, @RequestAttribute(name = "method", value = "", required = false) String method
			) throws Exception{
		errorCode = "";
		errorDesc = "";
		
		CurrentUserProfile cup = getCurrentlyAuthenticatedPrincipal();
		IssuerInfo ii = cup.getLoginRes().getIssuerInfo();
	
		LoginRes issu = cup.getLoginRes();
		String _idsu = issu.getIssuerId();

		BaseDTO baseDTO = new BaseDTO(req);
		Msg msg = baseDTO.createMsg(cup, Constants.MSG_ACTION_CODE.INQUIRY);		
		HashMap<String, String> hData = new HashMap<>();
		msg.setObjData(hData);

		JSONRoot root = new JSONRoot(msg);
		MsgRsp rsp = restAPI.callAPINormal("/einvoice/getMS", cup.getLoginRes().getToken(), HttpMethod.POST, root);
		MspResponseStatus rspStatus = rsp.getResponseStatus();

		
		req.setAttribute("NbanMst", ii.getTaxCode());
		req.setAttribute("NbanTen", ii.getName());
		req.setAttribute("NbanDchi", ii.getAddress());
		
		_id = commons.getParameterFromRequest(req, "_id");
		String header = "Thêm mới hóa đơn điện tử từ máy tính tiền";
		String action = "CREATE";
		boolean isEdit = false;
		
		req.setAttribute("NLap", commons.convertLocalDateTimeToString(LocalDate.now(), Constants.FORMAT_DATE.FORMAT_DATE_WEB));
		req.setAttribute("TGia", "1");
		req.setAttribute("DVTTe", "VND");
				
		switch (transaction) {
		case "einvoice_mtt_cqt-detail":
			header = "Chi tiết dữ liệu hóa đơn điện tử từ máy tính tiền gửi cơ quan thuế";
			action = "DETAIL";
			isEdit = false;
			break;

		default:
			break;
		}
		
		if("|einvoice_mtt-edit|einvoice_mtt-copy|einvoice_mtt_cqt-detail|einvoice_mtt-sign|".indexOf(transaction) != -1
				|| "init-dc-tt".equals(method))
			inquiry(cup, locale, req, session, _id, action, transaction, method);
		if("einvoice_mtt-cre".equals(transaction) && "init-dc-tt".equals(method)) {
			req.setAttribute("NLap", commons.convertLocalDateTimeToString(LocalDate.now(), Constants.FORMAT_DATE.FORMAT_DATE_WEB));
		}
		
		req.setAttribute("_header_", header);
		req.setAttribute("_action_", action);
		req.setAttribute("_isedit_", isEdit);
		req.setAttribute("_id", _id);
		
		if("|einvoice_mtt-cre|einvoice_mtt-copy|einvoice_mtt-edit|".indexOf(transaction) != -1)
			LoadParameter(cup, locale, req, action);
		req.setAttribute("Param", Param);
		if(!"".equals(errorDesc))
			req.setAttribute("messageError", errorDesc);
		
		return "einvoice_mtt/einvoice_mtt_cqt-crud";
	}

	private void inquiry(CurrentUserProfile cup, Locale locale, HttpServletRequest req, HttpSession session, String _id , String action, String transaction, String method) throws Exception{
		if("".equals(_id)) {
			errorCode = "NOT FOUND";
			errorDesc = "Không tìm thấy thông tin hóa đơn.";
			return;
		}
		BaseDTO baseDTO = new BaseDTO(req);
		Msg msg = baseDTO.createMsg(cup, Constants.MSG_ACTION_CODE.INQUIRY);
		HashMap<String, String> hData = new HashMap<>();
		msg.setObjData(hData);
		
		JSONRoot root = new JSONRoot(msg);
		MsgRsp rsp = restAPI.callAPINormal("/einvoice_mtt_cqt/detail/" + _id, cup.getLoginRes().getToken(), HttpMethod.POST, root);
		MspResponseStatus rspStatus = rsp.getResponseStatus();
		if(rspStatus.getErrorCode() == 0) {
			JsonNode jsonData = Json.serializer().nodeFromObject(rsp.getObjData());
			JsonNode rows = null;
			HashMap<String, String> hItem = null;
			List<Object> prds = new ArrayList<Object>();
			if(!jsonData.at("/rows").isMissingNode()) {
				rows = jsonData.at("/rows");
				int stt= 1;
				for(JsonNode row: rows) {				
					hItem = new LinkedHashMap<String, String>();	
					String STT = String.valueOf(stt);
					hItem.put("STT", STT); 
					hItem.put("MSHDon", commons.getTextJsonNode(row.at("/KHMSHDon"))+ commons.getTextJsonNode(row.at("/KHHDon")));
					hItem.put("SHDon", commons.getTextJsonNode(row.at("/SHDon")));
					hItem.put("NLap", commons.convertLocalDateTimeToString(commons.convertLongToLocalDate(row.at("/NLap").asLong()), Constants.FORMAT_DATE.FORMAT_DATE_WEB)
							);
					hItem.put("MaCQT", commons.getTextJsonNode(row.at("/MaCQT")));
					hItem.put("TTTToan", row.at("/TTTTSo").isMissingNode()? "":
						commons.formatNumberReal(row.at("/TTTTSo").doubleValue())
					);		
					
					String status = commons.getTextJsonNode(row.at("/EInvoiceStatus"));
					if(status.equals("PROCESSING")) {
						hItem.put("Status", "Đang xử lý từ CQT");	
					}else if(status.equals("COMPLETE")) {
						hItem.put("Status", "CQT chấp nhận");
					}else  if(status.equals("ERROR_CQT")) {
						hItem.put("Status", "Lỗi từ CQT");
					}	
					prds.add(hItem);
					stt++;
				}
			}
			req.setAttribute("DSHHDVu", commons.encodeStringBase64(Json.serializer().toString(prds)));
	
		}else {
			errorDesc = rspStatus.getErrorDesc();
		}
	}
	
	public BaseDTO checkDataToAccept(HttpServletRequest req, HttpSession session, String transaction
			, CurrentUserProfile cup) throws Exception{
		BaseDTO dto = new BaseDTO();
		dto.setErrorCode(0);
		
		_id = commons.getParameterFromRequest(req, "_id").replaceAll("\\s", "");
		_id_tt_dc = commons.getParameterFromRequest(req, "_id").replaceAll("\\s", "");
		mauSoHdon = commons.getParameterFromRequest(req, "mau-so-hdon").replaceAll("\\s", "");
		
		loaiHdon = commons.getParameterFromRequest(req, "loai-hd").replaceAll("\\s", "");
		loaiHdonText = commons.getParameterFromRequest(req, "loai-hd-text").trim().replaceAll("\\s+", " ");
		
		maHoaDon = commons.getParameterFromRequest(req, "ma-hd").trim().replaceAll("\\s+", " ");
		tenLoaiHd = commons.getParameterFromRequest(req, "ten-loai-hd").trim().replaceAll("\\s+", " ");
		ngayLap = commons.getParameterFromRequest(req, "ngay-lap").replaceAll("\\s", "");
		hinhThucThanhToan = commons.getParameterFromRequest(req, "hinh-thuc-thanh-toan").replaceAll("\\s", "");
		hinhThucThanhToanText = commons.getParameterFromRequest(req, "hinh-thuc-thanh-toan-text").trim().replaceAll("\\s+", " ");
//		chkXuatTheoLoaiTienTt = commons.getParameterFromRequest(req, "chk-xuat-theo-loai-tien-tt").replaceAll("\\s", "");
		khMst = commons.getParameterFromRequest(req, "kh-mst").replaceAll("\\s", "");
		khMKHang = commons.getParameterFromRequest(req, "kh-makhachhang").trim().replaceAll("\\s+", " ");
		
		khCCCDan = commons.getParameterFromRequest(req, "kh-cancuoccongdan").trim().replaceAll("\\s+", " ");
		
		khHoTenNguoiMua = commons.getParameterFromRequest(req, "kh-ho-ten-nguoi-mua").trim().replaceAll("\\s+", " ");
		khTenDonVi = commons.getParameterFromRequest(req, "kh-ten-don-vi").trim().replaceAll("\\s+", " ");
		khDiaChi = commons.getParameterFromRequest(req, "kh-dia-chi").trim().replaceAll("\\s+", " ");
		khEmail = commons.getParameterFromRequest(req, "kh-email").trim().replaceAll("null", " ").replaceAll("\\s+", " ");
		khEmailCC = commons.getParameterFromRequest(req, "kh-emailcc").trim().replaceAll("null", " ").replaceAll("\\s+", " ");
		khSoDt = commons.getParameterFromRequest(req, "kh-so-dt").trim().replaceAll("\\s+", " ");
		khSoTk = commons.getParameterFromRequest(req, "kh-so-tk").trim().replaceAll("\\s+", " ");
		khTkTaiNganHang = commons.getParameterFromRequest(req, "kh-tk-tai-ngan-hang").trim().replaceAll("\\s+", " ");
		tongTienTruocThue = commons.getParameterFromRequest(req, "tong-tien-truoc-thue").replaceAll("\\s", "");
		loaiTienTt = commons.getParameterFromRequest(req, "loai-tien-tt").replaceAll("\\s", "");
//		tyGia = commons.getParameterFromRequest(req, "ty-gia").replaceAll("\\s", "").replaceAll(",", "");
		tyGia = commons.getParameterFromRequest(req, "ty-gia").replaceAll("(\\s|,)", "");
		tongTienThueGtgt = commons.getParameterFromRequest(req, "tong-tien-thue-gtgt").replaceAll("\\s", "");
		tongTienDaCoThue = commons.getParameterFromRequest(req, "tong-tien-da-co-thue").replaceAll("\\s", "");
		tongTienQuyDoi = commons.getParameterFromRequest(req, "tong-tien-quy-doi").replaceAll("\\s", "");
		tienBangChu = commons.getParameterFromRequest(req, "tien-bang-chu").trim().replaceAll("\\s+", " ");
		dsSanPham = commons.getParameterFromRequest(req, "ds-san-pham").replaceAll("\\s", "");
		checkProductExtension = commons.getParameterFromRequest(req, "checkProductExtension").trim().replaceAll("\\s+", " ");
		checkProductExtension1 = "".equals(checkProductExtension)? "false": "true";
		if("einvoice_mtt-edit".equals(transaction)) {
			if("".equals(_id)) {
				dto.setErrorCode(1);
				dto.getErrorMessages().add("Không tìm thấy thông tin hóa đơn.");
			}
		}
	
		switch (transaction) {
		case "einvoice_mtt-cre":
		case "einvoice_mtt-copy":
		case "einvoice_mtt-edit":
			if("".equals(mauSoHdon)) {
				dto.setErrorCode(1);
				dto.getErrorMessages().add("Vui lòng chọn mẫu số hóa đơn.");
			}
			if("".equals(ngayLap)) {
				dto.setErrorCode(1);
				dto.getErrorMessages().add("Vui lòng chọn ngày lập hóa đơn.");
			}else if(!commons.checkLocalDate(ngayLap, Constants.FORMAT_DATE.FORMAT_DATE_WEB)) {
				dto.setErrorCode(1);
				dto.getErrorMessages().add("Ngày lập hóa đơn không đúng định dạng.");
			}
//			else if(commons.compareLocalDate(commons.convertStringToLocalDate(ngayLap, Constants.FORMAT_DATE.FORMAT_DATE_WEB), LocalDate.now()) > 0) {
//				dto.setErrorCode(1);
//				dto.getErrorMessages().add("Ngày lập hóa đơn không được lớn hơn ngày hiện tại.");
//			}
			if("".equals(loaiTienTt)) {
				dto.setErrorCode(1);
				dto.getErrorMessages().add("Vui lòng chọn loại tiền thanh toán.");
			}
			
			if("".equals(loaiHdon)) {
				dto.setErrorCode(1);
				dto.getErrorMessages().add("Vui lòng chọn loại hóa đơn.");
			}
			
			if("".equals(hinhThucThanhToanText)) {
				dto.setErrorCode(1);
				dto.getErrorMessages().add("Vui lòng chọn hình thức thanh toán thanh toán.");
			}
			if(!"".equals(khEmail) && !commons.isValidEmailAddress(khEmail)) {
				dto.setErrorCode(1);
				dto.getErrorMessages().add("Địa chỉ email người mua không đúng.");
			}
			if(!commons.checkStringIsInt(tyGia) || commons.ToNumber(tyGia) < 1) {
				dto.setErrorCode(1);
				dto.getErrorMessages().add("Tỷ giá không được nhỏ hơn 1 và phải là số nguyên.");
			}
			if("".equals(tienBangChu)) {
				dto.setErrorCode(1);
				dto.getErrorMessages().add("Tiền bằng chữ không được rỗng.");
			}
			
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
		String messageConfirm = "Bạn có muốn thêm mới hóa đơn điện tử này không?";
		switch (transaction) {
		case "einvoice_mtt-cre":
			messageConfirm = "Bạn có muốn thêm mới hóa đơn điện tử này không?";
			break;
		case "einvoice_mtt-copy":
			messageConfirm = "Bạn có muốn thêm mới hóa đơn điện tử này không?";
			break;
		case "einvoice_mtt-edit":
			messageConfirm = "Bạn có muốn thay đổi thông tin hóa đơn điện tử này không?";
			break;
		case "einvoice_mtt-deleteAll":
			messageConfirm = "Bạn có muốn xóa danh sách hóa đơn điện tử này không?";
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
		
		
		if(!"einvoice_mtt-deleteAll".equals(transaction)) {	
	JsonNode jsonNodeTmp = null;
		
		jsonNodeTmp = null;
		try {
			jsonNodeTmp = Json.serializer().nodeFromJson(commons.decodeBase64ToString(dsSanPham));
		}catch(Exception e) {
			log.error(" >>>>> An exception occurred!", e);
		}
		
		if(null == jsonNodeTmp || jsonNodeTmp.size() == 0) {
			dto.setErrorCode(999);
			dto.setResponseData("Hóa đơn chưa có dữ liệu hàng hóa.");
			return dto;
		}
		
		/*KIEM TRA THONG TIN SAN PHAM*/
		boolean check = true;
		int count = 0;
		JsonNode jsonNode = null;
		while(count < jsonNodeTmp.size() && check) {
			jsonNode = jsonNodeTmp.get(count);
			if("".equals(commons.getTextJsonNode(jsonNode.at("/Feature"))) 
					|| "".equals(commons.getTextJsonNode(jsonNode.at("/ProductName"))) 
//					|| commons.ToNumber(commons.getTextJsonNode(jsonNode.at("/Total"))) < 0
//					|| (
//							(!"4".equals(commons.getTextJsonNode(jsonNode.at("/Feature"))) && !"2".equals(commons.getTextJsonNode(jsonNode.at("/Feature"))))
//							 && commons.ToNumber(commons.getTextJsonNode(jsonNode.at("/Total"))) <= 0 
//						)
					) {
				check = false;
				break;
			}
			count++;
		}
		if(!check) {
			dto.setErrorCode(999);
			dto.setResponseData("Vui lòng kiểm tra lại dữ liệu hàng hóa.");
			return dto;
		}
		/*END - KIEM TRA THONG TIN SAN PHAM*/
		token = commons.csRandomAlphaNumbericString(30);
		session.setAttribute(Constants.SESSION_TYPE.SESSION_TOKEN_EXECUTE, token);
		
		HashMap<String, String> hInfo = new HashMap<String, String>();
		hInfo.put("CONFIRM", messageConfirm);
		hInfo.put("TOKEN", token);
		
		dto.setResponseData(hInfo);
		dto.setErrorCode(0);
		return dto;
	}
	else {
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
		

		
		JsonNode jsonNodeTmp = null;
		
		if(!"einvoice_mtt-deleteAll".equals(transaction)) {
		try {
			jsonNodeTmp = Json.serializer().nodeFromJson(commons.decodeBase64ToString(dsSanPham));
		}catch(Exception e) {
			log.error(" >>>>> An exception occurred!", e);
		}
		if(null == jsonNodeTmp || jsonNodeTmp.size() == 0) {
			dtoRes.setErrorCode(999);
			dtoRes.setResponseData("Hóa đơn chưa có dữ liệu hàng hóa.");
			return dtoRes;
		}
		
		boolean checkPrd = true;
		/*KIEM TRA TEN SP PHAI KHAC RONG*/
		for(JsonNode o: jsonNodeTmp) {
			if("".equals(commons.getTextJsonNode(o.at("/ProductName")))) {
				checkPrd = false;
				break;
			}
		}
		if(!checkPrd) {
			dtoRes.setErrorCode(999);
			dtoRes.setResponseData("Vui lòng kiểm tra lại dữ liệu hàng hóa.");
			return dtoRes;
		}
		}
		String actionCode = Constants.MSG_ACTION_CODE.CREATED;
		switch (transaction) {
		case "einvoice_mtt-cre": actionCode = Constants.MSG_ACTION_CODE.CREATED; break;
		case "einvoice_mtt-edit": actionCode = Constants.MSG_ACTION_CODE.MODIFY; break;
		case "einvoice_mtt-copy": actionCode = Constants.MSG_ACTION_CODE.COPY; break;
		case "einvoice_mtt-deleteAll": actionCode = Constants.MSG_ACTION_CODE.DELETEALL; break;
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
		case "einvoice_mtt-deleteAll":
			hData.put("ids", ids);
			break;
		default:
		hData.put("_id", _id);
		hData.put("MaHoaDon", maHoaDon);
		hData.put("_id_tt_dc", _id_tt_dc);
		hData.put("LoaiHoaDon", loaiHdon);
		hData.put("LoaiHoaDonText", loaiHdonText);
		hData.put("MauSoHdon", mauSoHdon);
		hData.put("TenLoaiHd", tenLoaiHd);
		hData.put("NgayLap", ngayLap);
		hData.put("HinhThucThanhToan", hinhThucThanhToan);
		hData.put("HinhThucThanhToanText", hinhThucThanhToanText);
		hData.put("KhMst", khMst);
		hData.put("KhMKHang", khMKHang);
		hData.put("KhCCCDan", khCCCDan);
		hData.put("KhHoTenNguoiMua", khHoTenNguoiMua);
		hData.put("KhTenDonVi", khTenDonVi);
		hData.put("KhDiaChi", khDiaChi);
		hData.put("KhEmail", khEmail);
		hData.put("KhEmailCC", khEmailCC);
		hData.put("KhSoDt", khSoDt);
		hData.put("KhSoTk", khSoTk);
		hData.put("KhTkTaiNganHang", khTkTaiNganHang);
		hData.put("TongTienTruocThue", tongTienTruocThue) ;
		hData.put("LoaiTienTt", loaiTienTt);
		hData.put("TyGia", "VND".equals(loaiTienTt)? "1": tyGia);
		if(!"VND".equals(loaiTienTt)) {
			hData.put("TongTienQuyDoi", tongTienQuyDoi);
		}
		hData.put("TongTienThueGtgt", tongTienThueGtgt);
		hData.put("TongTienDaCoThue", tongTienDaCoThue);
		hData.put("TienBangChu", tienBangChu);
		hData.put("DSSanPham", jsonNodeTmp);

		break;
		}
		msg.setObjData(hData);
		JSONRoot root = new JSONRoot(msg);
		MsgRsp rsp = restAPI.callAPINormal("/einvoice_mtt/crud", cup.getLoginRes().getToken(), HttpMethod.POST, root);
		MspResponseStatus rspStatus = rsp.getResponseStatus();
		if(rspStatus.getErrorCode() == 0) {
			dtoRes.setErrorCode(0);
			switch (transaction) {
			case "einvoice_mtt-cre":
				dtoRes.setResponseData("Thêm mới thông tin hóa đơn thành công.");
				break;
			case "einvoice_mtt-edit":
				dtoRes.setResponseData("Cập nhật thông tin hóa đơn thành công.");
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
			dto.setResponseData("Không tìm thấy hóa đơn cần ký.");
			return dto;
		}
		
		/*LAY THONG TIN DU LIEU XML VE SERVER WEB*/
		dto = new BaseDTO(req);
		Msg msg = dto.createMsg(cup, Constants.MSG_ACTION_CODE.CREATED);
		HashMap<String, Object> hData = new HashMap<>();
		hData.put("_id", _id);
		msg.setObjData(hData);
		JSONRoot root = new JSONRoot(msg);
		
		FileInfo fileInfo = restAPI.callAPIGetFileInfo("/einvoice_mtt/get-file-for-sign", cup.getLoginRes().getToken(), HttpMethod.POST, root);
		
		if(fileInfo.getCheck()!=null) {
			dto.setErrorCode(999);			
			dto.setResponseData(fileInfo.getCheck());
			return dto;
		}
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
		contentDisposition = ContentDisposition.builder("form-data").name("XMLFileSigned").filename("einvoice_mtt-signed.xml").build();
		fileMap.add(HttpHeaders.CONTENT_DISPOSITION, contentDisposition.toString());
		fileEntity = new HttpEntity<byte[]>(multipartFile.getBytes(), fileMap);
		body.add("XMLFileSigned", fileEntity);
		
		HttpEntity<MultiValueMap<String, Object>> requestBody = new HttpEntity<>(body, headers);
		String url = "/einvoice_mtt/sign-single";
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
	
	
	
	@RequestMapping(value = "/history", method = {RequestMethod.POST})
	public String history(Locale locale, HttpServletRequest req, HttpSession session
			, @RequestAttribute(name = "transaction", value = "", required = false) String transaction) throws Exception{
		errorCode = "";
		errorDesc = "";
		
		CurrentUserProfile cup = getCurrentlyAuthenticatedPrincipal();
		_id = commons.getParameterFromRequest(req, "_id");
		String header = "Tra cứu lịch sử mã CQT";
		String action = "HISTORY";
		boolean isEdit = false;
		if("|einvoice_mtt-history|".indexOf(transaction) != -1)
			inquiryhistory(cup, locale, req, session, _id, action);
		
		req.setAttribute("_header_", header);
		req.setAttribute("_action_", action);
		req.setAttribute("_isedit_", isEdit);
		req.setAttribute("_id", _id);


		return "einvoice_mtt/einvoice_mtt-history";
	}
	@RequestMapping(value = "/change", method = {RequestMethod.POST})
	public String change(Locale locale, HttpServletRequest req, HttpSession session
			, @RequestAttribute(name = "transaction", value = "", required = false) String transaction) throws Exception{
		errorCode = "";
		errorDesc = "";
		
		CurrentUserProfile cup = getCurrentlyAuthenticatedPrincipal();
		_id = commons.getParameterFromRequest(req, "_id");
		String header = "Đổi mã thông điệp";
		String action = "CHANGE";
		boolean isEdit = false;
		if("|einvoice_mtt-change|".indexOf(transaction) != -1)
			inquirychange(cup, locale, req, session, _id, action);
		
		req.setAttribute("_header_", header);
		req.setAttribute("_action_", action);
		req.setAttribute("_isedit_", isEdit);
		req.setAttribute("_id", _id);


		return "einvoice_mtt/einvoice-change";
	}
	private void inquirychange(CurrentUserProfile cup, Locale locale, HttpServletRequest req, HttpSession session,
			String _id2, String action)throws Exception{
		if("".equals(_id)) {
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
		MsgRsp rsp = restAPI.callAPINormal("/einvoice/change/" + _id, cup.getLoginRes().getToken(), HttpMethod.POST, root);
		MspResponseStatus rspStatus = rsp.getResponseStatus();
		if(rspStatus.getErrorCode() == 0) {
			MsgPage page = rsp.getMsgPage();
			grid.setTotal(1);
			StringBuilder sb = new StringBuilder();
			JsonNode jsonData = Json.serializer().nodeFromObject(rsp.getObjData());
			JsonNode rows = null;
			HashMap<String, String> hItem = null;
			List<Object> dshdon = new ArrayList<Object>();
			if(!jsonData.at("/rows").isMissingNode()) {
				rows = jsonData.at("/rows");
				for(JsonNode row: rows) {
					hItem = new HashMap<String, String>();
					hItem.put("SHD", commons.getTextJsonNode(row.at("/SHD")));
					hItem.put("MS", commons.getTextJsonNode(row.at("/MS")));
					hItem.put("MTDiep", commons.getTextJsonNode(row.at("/MTDiep")));
					hItem.put("MTDiepCU", commons.getTextJsonNode(row.at("/MTDiepCU")));
					dshdon.add(hItem);
				}
				req.setAttribute("DSHDon", commons.encodeStringBase64(Json.serializer().toString(dshdon)));
			}
			
		}else {
			errorDesc = rspStatus.getErrorDesc();
		}
	
	}

	private void inquiryhistory(CurrentUserProfile cup, Locale locale, HttpServletRequest req, HttpSession session, String _id , String action) throws Exception{
		if("".equals(_id)) {
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
		MsgRsp rsp = restAPI.callAPINormal("/einvoice_mtt/history/" + _id, cup.getLoginRes().getToken(), HttpMethod.POST, root);
		MspResponseStatus rspStatus = rsp.getResponseStatus();
		if(rspStatus.getErrorCode() == 0) {
			MsgPage page = rsp.getMsgPage();
			grid.setTotal(page.getTotalRows());
			StringBuilder sb = new StringBuilder();
			JsonNode jsonData = Json.serializer().nodeFromObject(rsp.getObjData());
			JsonNode rows = null;
			HashMap<String, String> hItem = null;
			LocalDateTime localdatetime = null;
			LocalDate localdate = null;
			List<Object> dshdon = new ArrayList<Object>();
			if(!jsonData.at("/rows").isMissingNode()) {
				rows = jsonData.at("/rows");
				for(JsonNode row: rows) {
					hItem = new HashMap<String, String>();
					hItem.put("STT", commons.getTextJsonNode(row.at("/STT")));
					 localdatetime = null;
					String ngay = commons.getTextJsonNode(row.at("/Date"));
					int nngay = ngay.length();
					if(nngay == 19) {
						 localdatetime = LocalDateTime.parse(ngay);
							ngay = commons.convertLocalDateTimeToString(localdatetime, Constants.FORMAT_DATE.FORMAT_DATE_TIME_WEB);
					}
					else {
						localdate = LocalDate.parse(ngay);
						ngay = commons.convertLocalDateTimeToString(localdate, Constants.FORMAT_DATE.FORMAT_DATE_WEB);
					}
				
				
					hItem.put("Date", 
							ngay
					);
					hItem.put("MLoi", commons.getTextJsonNode(row.at("/MLoi")));
					hItem.put("MTLoi", commons.getTextJsonNode(row.at("/MTLoi")));
					dshdon.add(hItem);
				}
				req.setAttribute("DSHDon", commons.encodeStringBase64(Json.serializer().toString(dshdon)));
			}
			
		}else {
			errorDesc = rspStatus.getErrorDesc();
		}
	
	}

	

	

	@RequestMapping(value = "/check-mst",  produces = MediaType.APPLICATION_JSON_VALUE, method = RequestMethod.POST)
	@ResponseBody
	public BaseDTO execCheckMST(Locale locale, HttpServletRequest req, HttpSession session
			, @RequestAttribute(name = "transaction", required = false, value = "") String transaction) throws Exception {
		String token = "";
		if (null != session.getAttribute(Constants.SESSION_TYPE.SESSION_TOKEN_EXECUTE)) {
			token = session.getAttribute(Constants.SESSION_TYPE.SESSION_TOKEN_EXECUTE).toString();
			session.removeAttribute(token);
		}
		session.removeAttribute(Constants.SESSION_TYPE.SESSION_TOKEN_EXECUTE);
		
		BaseDTO dto = new BaseDTO();
		String messageConfirm = "Bạn có muốn thêm mới hóa đơn không?";
		
		CurrentUserProfile cup = getCurrentlyAuthenticatedPrincipal();
		dto = checkDataToAccept(req, session, transaction, cup);
		if(0 != dto.getErrorCode()) {
			dto.setErrorCode(999);
			dto.setResponseData(Constants.MAP_ERROR.get(999));
			return dto;
		}
		String mst = commons.getParameterFromRequest(req, "mst").replaceAll("\\s", "");

		BaseDTO baseDTO = new BaseDTO(req);
		JsonGridDTO grid = new JsonGridDTO();
		Msg msg = baseDTO.createMsg(cup, Constants.MSG_ACTION_CODE.INQUIRY);
		HashMap<String, String> hData = new HashMap<>();
		hData.put("MST", mst);
		msg.setObjData(hData);
		JSONRoot root = new JSONRoot(msg);
		MsgRsp rsp = restAPI.callAPINormal("/einvoice/check_mst", cup.getLoginRes().getToken(), HttpMethod.POST, root);
		MspResponseStatus rspStatus = rsp.getResponseStatus();
		if(rspStatus.getErrorCode() == 0) {
			String TH = rsp.getResponseStatus().getErrorDesc();
			
			String[] split = TH.split(";");
			int dem = split.length;
			
			String ma_kh = split[0];
			String hvtnmh =split[1];
			String tendv = split[2];		
			String emailcc = split[3];
			String email = split[4];
			String sdt = split[5];
			String dchi = split[6];
			
			dto.setErrorCode(0);
			HashMap<String, Object> hR = new HashMap<String, Object>();
			hR.put("ma_kh", ma_kh);
			hR.put("hvtnmh", hvtnmh);
			hR.put("tendv",tendv);
			hR.put("dchi",dchi);
			hR.put("email",email);
			hR.put("sdt", sdt);
			hR.put("emailcc", emailcc);		
			dto.setResponseData(hR);
			return dto;
		}
		
		dto.setErrorCode(999);
		dto.setResponseData(rsp.getResponseStatus().getErrorDesc());
		return dto;
	}		
	
	@RequestMapping(value = "/save-nmua",  produces = MediaType.APPLICATION_JSON_VALUE, method = RequestMethod.POST)
	@ResponseBody
	public BaseDTO execSaveNMua(Locale locale, HttpServletRequest req, HttpSession session
			, @RequestAttribute(name = "transaction", required = false, value = "") String transaction) throws Exception {
		String token = "";
		if (null != session.getAttribute(Constants.SESSION_TYPE.SESSION_TOKEN_EXECUTE)) {
			token = session.getAttribute(Constants.SESSION_TYPE.SESSION_TOKEN_EXECUTE).toString();
			session.removeAttribute(token);
		}
		session.removeAttribute(Constants.SESSION_TYPE.SESSION_TOKEN_EXECUTE);
		
		BaseDTO dto = new BaseDTO();
		String messageConfirm = "Bạn có muốn thêm mới hóa đơn không?";
		
		CurrentUserProfile cup = getCurrentlyAuthenticatedPrincipal();
		dto = checkDataToAccept(req, session, transaction, cup);
		if(0 != dto.getErrorCode()) {
			dto.setErrorCode(999);
			dto.setResponseData(Constants.MAP_ERROR.get(999));
			return dto;
		}
		String mst = commons.getParameterFromRequest(req, "mst").replaceAll("\\s", "");
		String mkh = commons.getParameterFromRequest(req, "kh-makhachhang").replaceAll("\\s", "");
		String hvtnm = commons.getParameterFromRequest(req, "kh-ho-ten-nguoi-mua").replaceAll("\\s", " ");
		String tdv = commons.getParameterFromRequest(req, "kh-ten-don-vi").replaceAll("\\s", " ");
		String dchi = commons.getParameterFromRequest(req, "kh-dia-chi").replaceAll("\\s", " ");
		String email = commons.getParameterFromRequest(req, "kh-email").replaceAll("\\s", "");
		String emailcc = commons.getParameterFromRequest(req, "kh-emailcc").replaceAll("\\s", "");
		String sdt = commons.getParameterFromRequest(req, "kh-so-dt").replaceAll("\\s", "");


		BaseDTO baseDTO = new BaseDTO(req);
		JsonGridDTO grid = new JsonGridDTO();
		Msg msg = baseDTO.createMsg(cup, Constants.MSG_ACTION_CODE.INQUIRY);
		HashMap<String, String> hData = new HashMap<>();
		hData.put("MST", mst);
		hData.put("MKH", mkh);
		hData.put("HVTNM", hvtnm);
		hData.put("TDV", tdv);
		hData.put("DCHI", dchi);
		hData.put("EMAIL", email);
		hData.put("EMAILCC", emailcc);
		hData.put("SDT", sdt);
		msg.setObjData(hData);
		JSONRoot root = new JSONRoot(msg);
		MsgRsp rsp = restAPI.callAPINormal("/einvoice/save_nmua", cup.getLoginRes().getToken(), HttpMethod.POST, root);
		MspResponseStatus rspStatus = rsp.getResponseStatus();
		if(rspStatus.getErrorCode() == 0) {					
			dto.setErrorCode(0);	
			return dto;
		}
		
		dto.setErrorCode(999);
		dto.setResponseData(rsp.getResponseStatus().getErrorDesc());
		return dto;
	}	
	@RequestMapping(value = "/online-mst",  produces = MediaType.APPLICATION_JSON_VALUE, method = RequestMethod.POST)
	@ResponseBody
	public BaseDTO execOnlineMST(Locale locale, HttpServletRequest req, HttpSession session
			, @RequestAttribute(name = "transaction", required = false, value = "") String transaction) throws Exception {
		String token = "";
		if (null != session.getAttribute(Constants.SESSION_TYPE.SESSION_TOKEN_EXECUTE)) {
			token = session.getAttribute(Constants.SESSION_TYPE.SESSION_TOKEN_EXECUTE).toString();
			session.removeAttribute(token);
		}
		session.removeAttribute(Constants.SESSION_TYPE.SESSION_TOKEN_EXECUTE);
		
		BaseDTO dto = new BaseDTO();
		CurrentUserProfile cup = getCurrentlyAuthenticatedPrincipal();
		dto = checkDataToAccept(req, session, transaction, cup);
		if(0 != dto.getErrorCode()) {
			dto.setErrorCode(999);
			dto.setResponseData(Constants.MAP_ERROR.get(999));
			return dto;
		}
		String mst = commons.getParameterFromRequest(req, "mst").replaceAll("\\s", "");
		

//		BaseDTO baseDTO = new BaseDTO(req);
//		JsonGridDTO grid = new JsonGridDTO();
//		Msg msg = baseDTO.createMsg(cup, Constants.MSG_ACTION_CODE.INQUIRY);
//		HashMap<String, String> hData = new HashMap<>();
//		hData.put("MST", mst);
//		
//		
//		/*LAY TOKEN*/
//		String request = "https://masothue.com/Ajax/Token/";
//		URL url = new URL(request);
//		HttpURLConnection conn = (HttpURLConnection) url.openConnection();
//		conn.setDoOutput(true);
//		conn.setInstanceFollowRedirects(false);
//		conn.setRequestMethod("POST");
////		conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/90.0.4430.93 Safari/537.36");
//		conn.setRequestProperty("Content-type", "application/x-www-form-urlencoded; charset=UTF-8");
//		conn.setUseCaches( false );
//		
//		OutputStreamWriter writer = new OutputStreamWriter(conn.getOutputStream());
//		writer.write("");
//		writer.flush();
//		
//		StringBuilder sb = new StringBuilder();
//		String line;
//		BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
//		while ((line = reader.readLine()) != null) {
//		    sb.append(line);
//		}
//		writer.close();
//		reader.close();
//		
//		JsonNode jsonNode = Json.serializer().nodeFromJson(sb.toString());
//		String token_ = jsonNode.at("/token").asText();
//		
//		/*LAY THONG TIN MST*/
//		String urlSearch = String.format("https://masothue.com/Search/?q=%s&type=auto&token=%s&force-search=1", mst, token_);
//		Document doc = Jsoup.connect(urlSearch).get();
//		Element element = doc.select("table.table-taxinfo").first();
//		Element elemTmp = null;
//		if(null == element) {
//			//NEU CO NHIEU CHI NHANH; THI LAY DONG DAU TIEN
//			element = doc.select("div.tax-listing div[data-prefetch] a").first();
//			if(null != element && null != element.attr("href") && !"".equals(element.attr("href"))) {
//				urlSearch = "https://masothue.com" + element.attr("href");
//				doc = Jsoup.connect(urlSearch).get();
//				element = doc.select("table.table-taxinfo").first();
//			}
//		}		
//		if(null == element) {
//			dto.setErrorCode(1);
//			dto.setResponseData("Không tìm thấy thông tin mã số thuế.");
//			return dto;
//		}
//		
//		HashMap<String, String> hR = parserInfoCompany(doc);
//		
//		elemTmp = element.selectFirst("thead tr th span");
//		if(null != elemTmp) {
//			hR.put("ten_cong_ty", elemTmp.text());
//		}
//		
//		Elements elements = element.select("tbody tr");
//		Iterator<Element> iterator = elements.iterator();
//		while (iterator.hasNext()) {
//			elemTmp = iterator.next();
//			if(elemTmp.select("td").size() == 2) {
//				hR.put(
//					commons.deAccent(elemTmp.select("td:eq(0)").text().toLowerCase().replaceAll("\\s", "_"))
//					, elemTmp.select("td:eq(1)").text().trim()
//				);
//				if(elemTmp.select("td:eq(1) span[itemprop='name'] a") != null
//						&& elemTmp.select("td:eq(1) span[itemprop='name'] a").size() > 0
//						) {
//					hR.put("dai_dien_phap_luat", elemTmp.selectFirst("td:eq(1) span[itemprop='name'] a").text());
//				}
//			}
//			
////			if(elemTmp.selectFirst("td:eq(0)") != null) {}
////			hR.put("", elemTmp.select(cssQuery))
//		}
//		
//		if(hR.get("ngay_hoat_dong") != null
//				&& commons.checkLocalDate(hR.get("ngay_hoat_dong"), "yyyy-MM-dd")
//				) {
//			hR.put("ngay_hoat_dong", commons.convertLocalDateTimeStringToString(hR.get("ngay_hoat_dong"), "yyyy-MM-dd", Constants.FORMAT_DATE.FORMAT_DATE_WEB));
//		}
//		
//		elemTmp = element.nextElementSibling();
//		
//		if(elemTmp.hasAttr("role")) {
//			hR.put("remark", elemTmp.text());
//		}
//		
//		dto.setErrorCode(0);
//		dto.setResponseData(hR);
//		return dto;
		

		String urlString = String.format("https://api.vietqr.io/v2/business/%s", mst);
		URL url = new URL(urlString);
		HttpsURLConnection con = (HttpsURLConnection) url.openConnection();
		con.setRequestMethod("GET");
		
		if(con.getResponseCode() == 200) {
			BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
			String inputLine = "";
			StringBuilder content = new StringBuilder();
			while((inputLine = in.readLine()) != null) {
				content.append(inputLine);
			}
			in.close();
			con.disconnect();
			
			JsonNode jsonNode = Json.serializer().nodeFromJson(content.toString());
			if("00".equals(commons.getTextJsonNode(jsonNode.at("/code")))) {
				HashMap<String, String> hR = new HashMap<String, String>();
				
				hR.put("ten_cong_ty", commons.getTextJsonNode(jsonNode.at("/data/name")));
				hR.put("dia_chi", commons.getTextJsonNode(jsonNode.at("/data/address")));
				dto.setErrorCode(0);
				dto.setResponseData(hR);
				return dto;
			}else {
				dto.setErrorCode(1);
				dto.setResponseData(commons.getTextJsonNode(jsonNode.at("/desc")));
				return dto;
			}			
		}else {
			dto.setErrorCode(1);
			dto.setResponseData("Không tìm thấy thông tin khách hàng.");
			return dto;
		}	
	}
	
	private HashMap<String, String> parserInfoCompany(Document doc) throws Exception{
		HashMap<String, String> hR = new HashMap<String, String>(); 
		
		Element element = doc.select("table.table-taxinfo").first();
		Element elemTmp = null;
		
		elemTmp = element.selectFirst("thead tr th span");
		if(null != elemTmp) {
			hR.put("ten_cong_ty", elemTmp.text());
		}
		
		Elements elements = element.select("tbody tr");
		Iterator<Element> iterator = elements.iterator();
		while (iterator.hasNext()) {
			elemTmp = iterator.next();
			if(elemTmp.select("td").size() == 2) {
				hR.put(commons.deAccent(elemTmp.select("td:eq(0)").text().toLowerCase().replaceAll("\\s", "_")), elemTmp.select("td:eq(1)").text().trim());
			}
			
//			if(elemTmp.selectFirst("td:eq(0)") != null) {}
//			hR.put("", elemTmp.select(cssQuery))
		}
		
		if(hR.get("ngay_hoat_dong") != null
				&& commons.checkLocalDate(hR.get("ngay_hoat_dong"), "yyyy-MM-dd")
				) {
			hR.put("ngay_hoat_dong", commons.convertLocalDateTimeStringToString(hR.get("ngay_hoat_dong"), "yyyy-MM-dd", Constants.FORMAT_DATE.FORMAT_DATE_WEB));
		}
		
		elemTmp = element.nextElementSibling();
		
		if(elemTmp.hasAttr("role")) {
			hR.put("remark", elemTmp.text());
		}
		
		return hR;
	}

}
	