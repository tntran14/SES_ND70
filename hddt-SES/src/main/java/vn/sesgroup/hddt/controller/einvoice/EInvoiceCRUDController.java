package vn.sesgroup.hddt.controller.einvoice;

import java.io.File;
import java.io.FileOutputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.apache.commons.text.StringEscapeUtils;
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
	"/einvoice-cre"
	, "/einvoice-detail"
	, "/einvoice-copy"
	, "/einvoice-sign"
	, "/einvoice-edit"
	, "/einvoice-deleteAll"
	, "/einvoice-history"
	, "/einvoice-change"
	, "/einvoice-signAll"
	,"/einvoice_check_mst"
	,"/einvoice_save_nmua"
})
@Scope(value = WebApplicationContext.SCOPE_REQUEST)
public class EInvoiceCRUDController extends AbstractController{
	private static final Logger log = LogManager.getLogger(EInvoiceCRUDController.class);
	@Autowired RestAPIUtility restAPI;
	@Autowired RestTemplate restTemplate;
	
	private String errorCode;
	private String errorDesc;
	private String _id;
	private String _id_tt_dc;
	private String mauSoHdon;
	private String maHoaDon;
	private String tenLoaiHd;
	private String ngayLap;
	private String hinhThucThanhToan;
	private String hinhThucThanhToanText;
//	private String chkXuatTheoLoaiTienTt;
	private String khMst;
	private String khMKHang;
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
	private String paramUSD;
	
//	@RequestMapping(value = "/checkSignListInvoice",  produces = MediaType.APPLICATION_JSON_VALUE, method = RequestMethod.POST)
//	@ResponseBody
//	public BaseDTO checkSignListInvoice(Locale locale, HttpServletRequest req, HttpSession session) {
//		BaseDTO dtoRes = new BaseDTO();
//		
//		try {
//			String dataApprove = commons.getParameterFromRequest(req, "dataApprove");
//			JsonNode jsonNode = null;
//			try {
//				jsonNode = Json.serializer().nodeFromJson(commons.decodeBase64ToString(dataApprove));
//			}catch(Exception e) {}
//			
//			if(null == jsonNode) {
//				dtoRes.setErrorCode(1);
//				dtoRes.setResponseData("<span class='text-error'>Vui lòng kiểm tra lại dữ liệu hóa đơn.</span>");
//				return dtoRes;
//			}
//	
//	}

	
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
			msgParam.setParam("TaxInvoice");
			msgParams.getParams().add(msgParam);
			
			
			/*END: DANH SACH THAM SO*/
			msg.setObjData(msgParams);
			
			JSONRoot root = new JSONRoot(msg);
			MsgRsp rsp = restAPI.callAPINormal("/commons/get-full-params", cup.getLoginRes().getToken(), HttpMethod.POST, root);
			MspResponseStatus rspStatus = rsp.getResponseStatus();
			
			
			if(rspStatus.getErrorCode() == 0 && rsp.getObjData() != null) {
				LinkedHashMap<String, String> hItem = null;
				LinkedHashMap<String, String> hItem1 = null;
				ArrayList<HashMap<String, String>> rows = null;
				ArrayList<HashMap<String, String>> rows1 = null;
				
				
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
						if("1".equals(commons.getTextJsonNode(o.get("KHMSHDon")))){
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
						req.setAttribute("viewmoney", commons.getTextJsonNode(o.get("viewmoney")));
						req.setAttribute("userconfig_vnd", commons.getTextJsonNode(o.get("VND")));
						req.setAttribute("userconfig_usd", commons.getTextJsonNode(o.get("USD")));
					}
				}else {
					req.setAttribute("userconfig_vnd", "2");
					req.setAttribute("userconfig_usd", "2");
				}
				
				String TaxInvoice = "";
				String defaultTaxAdmin = "";
				String _taxInvoice = "";
				//boolean check_tax = false;
				if(null != jsonData.at("/param05")) {
					rows = new ArrayList<HashMap<String,String>>();
					rows1 = new ArrayList<HashMap<String,String>>();
					for(JsonNode o: jsonData.at("/param05")) {						
						hItem = new LinkedHashMap<String, String>();
					//	hItem.put("_id", commons.getTextJsonNode(o.at("/_id")));
						hItem.put("Name", commons.getTextJsonNode(o.at("/Name")));
						hItem.put("Code", commons.getTextJsonNode(o.at("/Code")));
						
						defaultTaxAdmin =  commons.getTextJsonNode(o.at("/DefaultAdmin"));
						if(defaultTaxAdmin.equals("true")) {
							_taxInvoice = commons.getTextJsonNode(o.at("/Code"));
						}
						
						rows.add(hItem);
						
						hItem1 = new LinkedHashMap<String, String>();
						hItem1.put(commons.getTextJsonNode(o.at("/Code")), commons.getTextJsonNode(o.at("/Name")));
						rows1.add(hItem1);
						
						if(TaxInvoice.equals("")) {
							
							TaxInvoice = "{" + "0" + ":" +" " + "," ;
							TaxInvoice += commons.getTextJsonNode(o.at("/Code")) + ":" + commons.getTextJsonNode(o.at("/Name")).toString();
						}else {
							TaxInvoice += "," + commons.getTextJsonNode(o.at("/Code")) + ":" + commons.getTextJsonNode(o.at("/Name")).toString();
						}						
					}
					req.setAttribute("ParamWareHouse", rows);
					TaxInvoice = TaxInvoice + "}";
					
					req.setAttribute("ParamTaxInvoiceSelect", TaxInvoice);
										
				}
											
				
				if(null != jsonData.at("/param04") && jsonData.at("/param04") instanceof ArrayNode) {
					hItem = new LinkedHashMap<String, String>();
					for(JsonNode o: jsonData.at("/param04")) {
	
					String check_Tax = commons.getTextJsonNode(o.get("TaxInvoice"));
						if(check_Tax.equals("")) {
							req.setAttribute("DefaultTaxAdmin", _taxInvoice);
						}else {
							req.setAttribute("DefaultTaxAdmin", check_Tax);
						}
					}
				}else {
					req.setAttribute("DefaultTaxAdmin", _taxInvoice);
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
		String mauso = "";
		if(rspStatus.getErrorCode() == 0) {		
			mauso =  rsp.getResponseStatus().getErrorDesc();
		}
		
		
		if("einvoice-cre".equals(transaction) && !"init-dc-tt".equals(method)) {
			if(!mauso.equals("")) {
				req.setAttribute("MauSoHD", mauso);
			}
		}
		
		req.setAttribute("HTTToanCode", "3");
		req.setAttribute("NbanMst", ii.getTaxCode());
		req.setAttribute("NbanTen", ii.getName());
		req.setAttribute("NbanDchi", ii.getAddress());
		req.setAttribute("THDon", "Hóa đơn giá trị gia tăng TT 78");
		
		_id = commons.getParameterFromRequest(req, "_id");
		String header = "Thêm mới hóa đơn";
		String action = "CREATE";
		boolean isEdit = false;
		
		req.setAttribute("NLap", commons.convertLocalDateTimeToString(LocalDate.now(), Constants.FORMAT_DATE.FORMAT_DATE_WEB));
		req.setAttribute("TGia", "1");
		req.setAttribute("DVTTe", "VND");
				
		switch (transaction) {
		case "einvoice-cre":
			header = "Thêm mới hóa đơn";
			action = "CREATE";
			isEdit = true;
			break;
		case "einvoice-edit":
			header = "Thay đổi thông tin hóa đơn";
			action = "EDIT";
			isEdit = true;
			break;
		case "einvoice-detail":
			header = "Chi tiết hóa đơn";
			action = "DETAIL";
			isEdit = false;
			break;
		case "einvoice-copy":
			header = "Copy hóa đơn";
			action = "COPY";
			isEdit = true;
			break;
		case "einvoice-sign":
			header = "Ký hóa đơn";
			action = "SIGN";
			isEdit = false;
			break;

		default:
			break;
		}
		
		if("|einvoice-edit|einvoice-copy|einvoice-detail|einvoice-sign|".indexOf(transaction) != -1
				|| "init-dc-tt".equals(method))
			inquiry(cup, locale, req, session, _id, action, transaction, method);
		if("einvoice-cre".equals(transaction) && "init-dc-tt".equals(method)) {
			req.setAttribute("NLap", commons.convertLocalDateTimeToString(LocalDate.now(), Constants.FORMAT_DATE.FORMAT_DATE_WEB));
		}
		
		req.setAttribute("_header_", header);
		req.setAttribute("_action_", action);
		req.setAttribute("_isedit_", isEdit);
		req.setAttribute("_id", _id);
		
		if("|einvoice-cre|einvoice-copy|einvoice-edit|".indexOf(transaction) != -1)
			LoadParameter(cup, locale, req, action);
		req.setAttribute("Param", Param);
		if(!"".equals(errorDesc))
			req.setAttribute("messageError", errorDesc);
		
		return "einvoice/einvoice-crud";
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
		MsgRsp rsp = restAPI.callAPINormal("/einvoice/detail/" + _id, cup.getLoginRes().getToken(), HttpMethod.POST, root);
		MspResponseStatus rspStatus = rsp.getResponseStatus();
		if(rspStatus.getErrorCode() == 0) {
			JsonNode jsonData = Json.serializer().nodeFromObject(rsp.getObjData());
			
			String TCTBao = "";
			String notice = "";
			if("init-dc-tt".equals(method) && "einvoice-cre".equals(transaction)) {
				TCTBao = commons.getTextJsonNode(jsonData.at("/HDSS/TCTBao"));
				if("|2|3|".indexOf("|" + TCTBao + "|") == -1) {
					errorDesc = "Không tìm thấy thông tin hóa đơn điều chỉnh hoặc thay thế.";
					return;
				}
				
				if("3".equals(TCTBao)) {
					notice = String.format("(Thay thế cho hóa đơn Mẫu số %s, Ký hiệu %s, số %s, ngày %s)", 
							commons.getTextJsonNode(jsonData.at("/EInvoiceDetail/TTChung/KHMSHDon")),
							commons.getTextJsonNode(jsonData.at("/EInvoiceDetail/TTChung/KHHDon")),
							commons.formatNumberBillInvoice(jsonData.at("/EInvoiceDetail/TTChung/SHDon").asInt()),
							commons.convertLocalDateTimeToString(commons.convertLongToLocalDate(jsonData.at("/EInvoiceDetail/TTChung/NLap").asLong()), Constants.FORMAT_DATE.FORMAT_DATE_WEB)
						);
				}else if("2".equals(TCTBao)) {
					notice = String.format("(Điều chỉnh cho hóa đơn Mẫu số %s, Ký hiệu %s, số %s, ngày %s)", 
							commons.getTextJsonNode(jsonData.at("/EInvoiceDetail/TTChung/KHMSHDon")),
							commons.getTextJsonNode(jsonData.at("/EInvoiceDetail/TTChung/KHHDon")),
							commons.formatNumberBillInvoice(jsonData.at("/EInvoiceDetail/TTChung/SHDon").asInt()),
							commons.convertLocalDateTimeToString(commons.convertLongToLocalDate(jsonData.at("/EInvoiceDetail/TTChung/NLap").asLong()), Constants.FORMAT_DATE.FORMAT_DATE_WEB)
						);
				}
				req.setAttribute("_notice", notice);
				req.setAttribute("_id_tt_dc", _id);
			}else {
				if(!jsonData.at("/EInvoiceDetail/TTChung/TTHDLQuan").isMissingNode()) {
					TCTBao = commons.getTextJsonNode(jsonData.at("/EInvoiceDetail/TTChung/TTHDLQuan/TCHDon"));
					if("1".equals(TCTBao)) {
						notice = String.format("(Thay thế cho hóa đơn Mẫu số %s, Ký hiệu %s, số %s, ngày %s)", 
								commons.getTextJsonNode(jsonData.at("/EInvoiceDetail/TTChung/TTHDLQuan/KHMSHDCLQuan")),
								commons.getTextJsonNode(jsonData.at("/EInvoiceDetail/TTChung/TTHDLQuan/KHHDCLQuan")),
								commons.getTextJsonNode(jsonData.at("/EInvoiceDetail/TTChung/TTHDLQuan/SHDCLQuan")),
								commons.convertLocalDateTimeToString(commons.convertStringToLocalDate(commons.getTextJsonNode(jsonData.at("/EInvoiceDetail/TTChung/TTHDLQuan/NLHDCLQuan")), "yyyy-MM-dd"), Constants.FORMAT_DATE.FORMAT_DATE_WEB)
							);
					}else if("2".equals(TCTBao)) {
						notice = String.format("(Điều chỉnh cho hóa đơn Mẫu số %s, Ký hiệu %s, số %s, ngày %s)", 
								commons.getTextJsonNode(jsonData.at("/EInvoiceDetail/TTChung/TTHDLQuan/KHMSHDCLQuan")),
								commons.getTextJsonNode(jsonData.at("/EInvoiceDetail/TTChung/TTHDLQuan/KHHDCLQuan")),
								commons.getTextJsonNode(jsonData.at("/EInvoiceDetail/TTChung/TTHDLQuan/SHDCLQuan")),
								commons.convertLocalDateTimeToString(commons.convertStringToLocalDate(commons.getTextJsonNode(jsonData.at("/EInvoiceDetail/TTChung/TTHDLQuan/NLHDCLQuan")), "yyyy-MM-dd"), Constants.FORMAT_DATE.FORMAT_DATE_WEB)
							);
					}
					req.setAttribute("_notice", notice);
				}
			}
			if("SIDED".equals(commons.getTextJsonNode(jsonData.at("/SignStatusCode")))){
				
			}
			else {
				req.setAttribute("MCCQT", commons.getTextJsonNode(jsonData.at("/MCCQT")));
				if(!jsonData.at("/EInvoiceDetail/TTChung/SHDon").isMissingNode())
					req.setAttribute("SHDon", commons.formatNumberBillInvoice(jsonData.at("/EInvoiceDetail/TTChung/SHDon").asInt()));	
				
				req.setAttribute("NbanMst", commons.getTextJsonNode(jsonData.at("/EInvoiceDetail/NDHDon/NBan/MST")));
				req.setAttribute("NbanTen", commons.getTextJsonNode(jsonData.at("/EInvoiceDetail/NDHDon/NBan/Ten")));
				req.setAttribute("NbanDchi", commons.getTextJsonNode(jsonData.at("/EInvoiceDetail/NDHDon/NBan/DChi")));
				req.setAttribute("MauSoHdon",
					commons.getTextJsonNode(jsonData.at("/EInvoiceDetail/TTChung/KHMSHDon"))
					+ commons.getTextJsonNode(jsonData.at("/EInvoiceDetail/TTChung/KHHDon"))
				);
				req.setAttribute("MauSoHD", commons.getTextJsonNode(jsonData.at("/EInvoiceDetail/TTChung/MauSoHD")));
				req.setAttribute("THDon", commons.getTextJsonNode(jsonData.at("/EInvoiceDetail/TTChung/THDon")));
				req.setAttribute("HTTToanCode", commons.getTextJsonNode(jsonData.at("/EInvoiceDetail/TTChung/HTTToanCode")));
				req.setAttribute("HTTToan", commons.getTextJsonNode(jsonData.at("/EInvoiceDetail/TTChung/HTTToan")));
				req.setAttribute("HTTToanCode", commons.getTextJsonNode(jsonData.at("/EInvoiceDetail/TTChung/HTTToanCode")));
				req.setAttribute("NLap", commons.convertLocalDateTimeToString(commons.convertLongToLocalDate(jsonData.at("/EInvoiceDetail/TTChung/NLap").asLong()), Constants.FORMAT_DATE.FORMAT_DATE_WEB));
				
				
				if (action != "COPY" && TCTBao.equals("")) {
					req.setAttribute("MaHD", commons.getTextJsonNode(jsonData.at("/EInvoiceDetail/TTChung/MaHD")));
				}		
				if(!TCTBao.equals("") && !action.equals("CREATE") && action != "COPY") {
					req.setAttribute("MaHD", commons.getTextJsonNode(jsonData.at("/EInvoiceDetail/TTChung/MaHD")));
				}
				
				
				req.setAttribute("NMuaMST", commons.getTextJsonNode(jsonData.at("/EInvoiceDetail/NDHDon/NMua/MST")));
				req.setAttribute("NMuaMKHang", commons.getTextJsonNode(jsonData.at("/EInvoiceDetail/NDHDon/NMua/MKHang")));
				req.setAttribute("NMuaHVTNMHang", commons.getTextJsonNode(jsonData.at("/EInvoiceDetail/NDHDon/NMua/HVTNMHang")));
				req.setAttribute("NMuaTen", commons.getTextJsonNode(jsonData.at("/EInvoiceDetail/NDHDon/NMua/Ten")));
				req.setAttribute("NMuaDChi", commons.getTextJsonNode(jsonData.at("/EInvoiceDetail/NDHDon/NMua/DChi")));
				req.setAttribute("NMuaDCTDTu", commons.getTextJsonNode(jsonData.at("/EInvoiceDetail/NDHDon/NMua/DCTDTu")));
				req.setAttribute("NMuaDCTDTuCC", commons.getTextJsonNode(jsonData.at("/EInvoiceDetail/NDHDon/NMua/DCTDTuCC")));
				req.setAttribute("NMuaSDThoai", commons.getTextJsonNode(jsonData.at("/EInvoiceDetail/NDHDon/NMua/SDThoai")));
				req.setAttribute("NMuaSTKNHang", commons.getTextJsonNode(jsonData.at("/EInvoiceDetail/NDHDon/NMua/STKNHang")));
				req.setAttribute("NMuaTNHang", commons.getTextJsonNode(jsonData.at("/EInvoiceDetail/NDHDon/NMua/TNHang")));
				
				HashMap<String, String> hItem = null;
				List<Object> prds = new ArrayList<Object>();
				if(!jsonData.at("/EInvoiceDetail/DSHHDVu").isMissingNode()) {
					for(JsonNode o: jsonData.at("/EInvoiceDetail/DSHHDVu")) {
						hItem = new LinkedHashMap<String, String>();
						
						hItem.put("STT", commons.getTextJsonNode(o.at("/STT")));
						if("DETAIL".equals(action)){
							hItem.put("ProductName", commons.getTextJsonNode(o.at("/ProductName")));
						}else {
							String productName = commons.getTextJsonNode(o.at("/ProductName"));
							productName = StringEscapeUtils.escapeHtml4(productName);
							hItem.put("ProductName",productName);
						}
						hItem.put("ProductCode", commons.getTextJsonNode(o.at("/ProductCode")));
						hItem.put("SLo", commons.getTextJsonNode(o.at("/SLo")));
						hItem.put("HanSD", commons.getTextJsonNode(o.at("/HanSD")));
						hItem.put("Unit", commons.getTextJsonNode(o.at("/Unit")));
						switch (commons.getTextJsonNode(o.at("/Feature"))) {
//						case "2":
//							break;
//						case "4":
//							if("DETAIL".equals(action))
//								hItem.put("VATRate", 
//									o.at("/VATRate").isMissingNode()? "":
//									Constants.MAP_VAT.get(commons.formatNumberReal(o.at("/VATRate").doubleValue()))
//								);
//							else
//								hItem.put("VATRate", 
//									o.at("/VATRate").isMissingNode()? "":
//									commons.formatNumberReal(o.at("/VATRate").doubleValue())
//								);
//							break;
						default:
							hItem.put("Quantity", 
								o.at("/Quantity").isMissingNode()? "":
								commons.formatNumberReal(o.at("/Quantity").doubleValue())
							);
							hItem.put("Price", 
								o.at("/Price").isMissingNode()? "":
								commons.formatNumberReal(o.at("/Price").doubleValue())
							);
							hItem.put("Total", 
								o.at("/Total").isMissingNode()? "":
								commons.formatNumberReal(o.at("/Total").doubleValue())
							);
							if("DETAIL".equals(action))
								hItem.put("VATRate", 
									o.at("/VATRate").isMissingNode()? "":
									Constants.MAP_VAT.get(commons.formatNumberReal(o.at("/VATRate").doubleValue()))
								);
							else if((o.at("/VATRate").toString().equals(""))){
								hItem.put("VATRate", "0");
							}
							else if((o.at("/VATRate").toString().equals("0.0"))){
								hItem.put("VATRate", "1");	
								
							}	else	{							
								hItem.put("VATRate", 
										o.at("/VATRate").isMissingNode()? "":
										commons.formatNumberReal(o.at("/VATRate").doubleValue()));
							}						
								
								
							hItem.put("VATAmount", 
								o.at("/VATAmount").isMissingNode()? "":
								commons.formatNumberReal(o.at("/VATAmount").doubleValue())
							);
							hItem.put("Amount", 
								o.at("/Amount").isMissingNode()? "":
								commons.formatNumberReal(o.at("/Amount").doubleValue())
							);
							break;
						}
						if("DETAIL".equals(action))
							hItem.put("Feature", Constants.MAP_PRD_FEATURE.get(commons.getTextJsonNode(o.at("/Feature"))));
						else
							hItem.put("Feature", commons.getTextJsonNode(o.at("/Feature")));
						
						prds.add(hItem);
					}
				}

				req.setAttribute("DSHHDVu", commons.encodeStringBase64(Json.serializer().toString(prds)));
				loaiTienTt = commons.getTextJsonNode(jsonData.at("/EInvoiceDetail/TTChung/DVTTe"));
				req.setAttribute("DVTTe", loaiTienTt);
				req.setAttribute("TGia", commons.formatNumberReal(commons.getTextJsonNode(jsonData.at("/EInvoiceDetail/TTChung/TGia"))));
				
				String ParamUSD = commons.getTextJsonNode(jsonData.at("/EInvoiceDetail/TToan/ParamUSD"));
							
				if(!loaiTienTt.equals("VND") && !ParamUSD.equals("")) {				
					int CheckParamUSD = Integer.parseInt(ParamUSD);
					String TgTCThue = commons.formatNumberReal(jsonData.at("/EInvoiceDetail/TToan/TgTCThue").doubleValue());
//					if (TgTCThue.contains(".")) {
//					    String[] parts = TgTCThue.split("\\.");
//					    if (parts.length == 2 && parts[1].length() < CheckParamUSD) {
//					        StringBuilder sb = new StringBuilder(TgTCThue);
//					       while (sb.charAt(sb.length() - 1) != '.' && sb.charAt(sb.length() - 1) != '0') {
//					            sb.append("0");
//					        }
//					        TgTCThue = sb.toString();
//					    }
//					}
					
					if (TgTCThue.contains(".")) {
					    String[] parts = TgTCThue.split("\\.");
					    if (parts.length == 2 && parts[1].length() < CheckParamUSD) {
					        StringBuilder sb = new StringBuilder(TgTCThue);
					        while (parts[1].length() < CheckParamUSD) {
					            sb.append("0");
					            parts[1] += "0";
					        }
					        TgTCThue = sb.toString();
					    }
					}
					
					req.setAttribute("TgTCThue", TgTCThue);
				
					
					String TgTThue = commons.formatNumberReal(jsonData.at("/EInvoiceDetail/TToan/TgTThue").doubleValue());
//					if (TgTThue.contains(".")) {
//					    String[] parts = TgTThue.split("\\.");
//					    if (parts.length == 2 && parts[1].length() < CheckParamUSD) {
//					        StringBuilder sb = new StringBuilder(TgTThue);
//					       while (sb.charAt(sb.length() - 1) != '.' && sb.charAt(sb.length() - 1) != '0') {
//					            sb.append("0");
//					        }
//					        TgTThue = sb.toString();
//					    }
//					}
					
					if (TgTThue.contains(".")) {
					    String[] parts = TgTThue.split("\\.");
					    if (parts.length == 2 && parts[1].length() < CheckParamUSD) {
					        StringBuilder sb = new StringBuilder(TgTThue);
					        while (parts[1].length() < CheckParamUSD) {
					            sb.append("0");
					            parts[1] += "0";
					        }
					        TgTThue = sb.toString();
					    }
					}
					
					req.setAttribute("TgTThue", TgTThue);
					
					String TgTTTBSo = commons.formatNumberReal(jsonData.at("/EInvoiceDetail/TToan/TgTTTBSo").doubleValue());
//					if (TgTTTBSo.contains(".")) {
//					    String[] parts = TgTTTBSo.split("\\.");
//					    if (parts.length == 2 && parts[1].length() < CheckParamUSD) {
//					        StringBuilder sb = new StringBuilder(TgTTTBSo);
//					       while (sb.charAt(sb.length() - 1) != '.' && sb.charAt(sb.length() - 1) != '0') {			        
//					            sb.append("0");
//					        }
//					        TgTTTBSo = sb.toString();
//					    }
//					}		
					
					if (TgTTTBSo.contains(".")) {
					    String[] parts = TgTTTBSo.split("\\.");
					    if (parts.length == 2 && parts[1].length() < CheckParamUSD) {
					        StringBuilder sb = new StringBuilder(TgTTTBSo);
					        while (parts[1].length() < CheckParamUSD) {
					            sb.append("0");
					            parts[1] += "0";
					        }
					        TgTTTBSo = sb.toString();
					    }
					}
					
					req.setAttribute("TgTTTBSo", TgTTTBSo);
					
				}else {
				req.setAttribute("TgTCThue", 
					jsonData.at("/EInvoiceDetail/TToan/TgTCThue").isMissingNode()? "":
					commons.formatNumberReal(jsonData.at("/EInvoiceDetail/TToan/TgTCThue").doubleValue())
				);
				
				req.setAttribute("TgTThue", 
						jsonData.at("/EInvoiceDetail/TToan/TgTThue").isMissingNode()? "":
						commons.formatNumberReal(jsonData.at("/EInvoiceDetail/TToan/TgTThue").doubleValue())
					);
				
				req.setAttribute("TgTTTBSo", 
						jsonData.at("/EInvoiceDetail/TToan/TgTTTBSo").isMissingNode()? "":
						commons.formatNumberReal(jsonData.at("/EInvoiceDetail/TToan/TgTTTBSo").doubleValue())
					);		
			}
					
				req.setAttribute("TgTTTBSoQuyDoi", 
					jsonData.at("/EInvoiceDetail/TToan/TgTQDoi").isMissingNode()? "":
					commons.formatNumberReal(jsonData.at("/EInvoiceDetail/TToan/TgTQDoi").doubleValue())
				);
				req.setAttribute("TgTTTBChu", commons.getTextJsonNode(jsonData.at("/EInvoiceDetail/TToan/TgTTTBChu")));
				req.setAttribute("checkProductExtension", commons.getTextJsonNode(jsonData.at("/checkProductExtension")));
		
			}
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
		maHoaDon = commons.getParameterFromRequest(req, "ma-hd").trim().replaceAll("\\s+", " ");
		tenLoaiHd = commons.getParameterFromRequest(req, "ten-loai-hd").trim().replaceAll("\\s+", " ");
		ngayLap = commons.getParameterFromRequest(req, "ngay-lap").replaceAll("\\s", "");
		hinhThucThanhToan = commons.getParameterFromRequest(req, "hinh-thuc-thanh-toan").replaceAll("\\s", "");
		hinhThucThanhToanText = commons.getParameterFromRequest(req, "hinh-thuc-thanh-toan-text").trim().replaceAll("\\s+", " ");
//		chkXuatTheoLoaiTienTt = commons.getParameterFromRequest(req, "chk-xuat-theo-loai-tien-tt").replaceAll("\\s", "");
		khMst = commons.getParameterFromRequest(req, "kh-mst").replaceAll("\\s+", "");
		khMKHang = commons.getParameterFromRequest(req, "kh-makhachhang").trim().replaceAll("\\s+", " ");
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
		paramUSD = commons.getParameterFromRequest(req, "paramUSD").replaceAll("\\s", "");
		if("einvoice-edit".equals(transaction)) {
			if("".equals(_id)) {
				dto.setErrorCode(1);
				dto.getErrorMessages().add("Không tìm thấy thông tin hóa đơn.");
			}
		}
	
		switch (transaction) {
		case "einvoice-cre":
		case "einvoice-copy":
		case "einvoice-edit":
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
			
			if(!khMst.equals("")) {
				if(khMst.length() < 10 || khMst.length() > 14 || khMst.length() == 11 || khMst.length() ==12 || khMst.length() ==13) {
					dto.setErrorCode(1);
					dto.getErrorMessages().add("Mã số thuế người mua không đúng định dạng.");
				}else {					
					if(khMst.length() == 14) {
						if(!khMst.contains("-")){
						dto.setErrorCode(1);
						dto.getErrorMessages().add("Mã số thuế người mua không đúng định dạng.");
						}else {
							String split[] = khMst.split("-");
							if(split[1].length() != 3) {
								dto.setErrorCode(1);
								dto.getErrorMessages().add("Mã số thuế người mua không đúng định dạng.");
							}
							
						}
					}
				}
			}
			
			
			break;
		case "einvoice-deleteAll":
			_token = commons.getParameterFromRequest(req, "_token").replaceAll("\\s", "");
			ids = null;
			try {
				ids = Json.serializer().fromJson(commons.decodeBase64ToString(_token), new TypeReference<List<String>>() {
				   
				});
			}catch(Exception e) {}
			break;
		case "einvoice-signAll":
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
		String messageConfirm = "Bạn có muốn thêm mới hóa đơn không?";
		switch (transaction) {
		case "einvoice-cre":
			messageConfirm = "Bạn có muốn thêm mới hóa đơn không?";
			break;
		case "einvoice-copy":
			messageConfirm = "Bạn có muốn thêm mới hóa đơn không?";
			break;
		case "einvoice-edit":
			messageConfirm = "Bạn có muốn thay đổi thông tin hóa đơn không?";
			break;
		case "einvoice-deleteAll":
			messageConfirm = "Bạn có muốn xóa danh sách hóa đơn không?";
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
		
		if("einvoice-deleteAll".equals(transaction)) {
			if(null == ids || ids.size() == 0) {
				dto.setErrorCode(999);
				dto.setResponseData("Không tìm thấy danh sách hóa đơn cần xóa.");
				return dto;
			}
		}
	
		if(!"einvoice-deleteAll".equals(transaction)) {	
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
		
		if("einvoice-deleteAll".equals(transaction)) {
			if(null == ids || ids.size() == 0) {
				dtoRes.setErrorCode(999);
				dtoRes.setResponseData("Không tìm thấy danh sách hóa đơn cần xóa.");
				return dtoRes;
			}
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
		
		if(!"einvoice-deleteAll".equals(transaction)) {
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
		case "einvoice-cre": actionCode = Constants.MSG_ACTION_CODE.CREATED; break;
		case "einvoice-edit": actionCode = Constants.MSG_ACTION_CODE.MODIFY; break;
		case "einvoice-copy": actionCode = Constants.MSG_ACTION_CODE.COPY; break;
		case "einvoice-deleteAll": actionCode = Constants.MSG_ACTION_CODE.DELETEALL; break;
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
		case "einvoice-deleteAll":
			hData.put("ids", ids);
			break;
		default:
		hData.put("_id", _id);
		hData.put("MaHoaDon", maHoaDon);
		hData.put("_id_tt_dc", _id_tt_dc);
		hData.put("MauSoHdon", mauSoHdon);
		hData.put("TenLoaiHd", tenLoaiHd);
		hData.put("NgayLap", ngayLap);
		hData.put("HinhThucThanhToan", hinhThucThanhToan);
		hData.put("HinhThucThanhToanText", hinhThucThanhToanText);
		hData.put("KhMst", khMst);
		hData.put("KhMKHang", khMKHang);
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
		hData.put("checkProductExtension", checkProductExtension1);
		hData.put("ParamUSD", paramUSD);
		break;
		}
		msg.setObjData(hData);
		JSONRoot root = new JSONRoot(msg);
		MsgRsp rsp = restAPI.callAPINormal("/einvoice/crud", cup.getLoginRes().getToken(), HttpMethod.POST, root);
		MspResponseStatus rspStatus = rsp.getResponseStatus();
		if(rspStatus.getErrorCode() == 0) {
			dtoRes.setErrorCode(0);
			switch (transaction) {
			case "einvoice-cre":
				dtoRes.setResponseData("Thêm mới thông tin hóa đơn thành công.");
				break;
			case "einvoice-edit":
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
		
		FileInfo fileInfo = restAPI.callAPIGetFileInfo("/einvoice/get-file-for-sign", cup.getLoginRes().getToken(), HttpMethod.POST, root);
		
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
		
		
		
		//CHECK SHDON CO HOP LE HAY KHONG
		MsgRsp rsp = restAPI.callAPINormal("/einvoice/check_shd", cup.getLoginRes().getToken(), HttpMethod.POST, root);
		MspResponseStatus rspStatus = rsp.getResponseStatus();
		if(rspStatus.getErrorCode() != 0) {			
			dto.setErrorCode(999);
			dto.setResponseData(rsp.getResponseStatus().getErrorDesc());
			return dto;
			
		}
		//END CHECK SHDON CO HOP LE HAY KHONG
		
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
		contentDisposition = ContentDisposition.builder("form-data").name("XMLFileSigned").filename("einvoice-signed.xml").build();
		fileMap.add(HttpHeaders.CONTENT_DISPOSITION, contentDisposition.toString());
		fileEntity = new HttpEntity<byte[]>(multipartFile.getBytes(), fileMap);
		body.add("XMLFileSigned", fileEntity);
		
		HttpEntity<MultiValueMap<String, Object>> requestBody = new HttpEntity<>(body, headers);
		String url = "/einvoice/sign-single";
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
		if("|einvoice-history|".indexOf(transaction) != -1)
			inquiryhistory(cup, locale, req, session, _id, action);
		
		req.setAttribute("_header_", header);
		req.setAttribute("_action_", action);
		req.setAttribute("_isedit_", isEdit);
		req.setAttribute("_id", _id);


		return "einvoice/einvoice-history";
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
		if("|einvoice-change|".indexOf(transaction) != -1)
			inquirychange(cup, locale, req, session, _id, action);
		
		req.setAttribute("_header_", header);
		req.setAttribute("_action_", action);
		req.setAttribute("_isedit_", isEdit);
		req.setAttribute("_id", _id);


		return "einvoice/einvoice-change";
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
		MsgRsp rsp = restAPI.callAPINormal("/einvoice/history/" + _id, cup.getLoginRes().getToken(), HttpMethod.POST, root);
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
		case "einvoice-signAll":
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

		fileInfo = restAPI.callAPIGetFileInfo("/einvoice/get-file-for-signAll", cup.getLoginRes().getToken(),
				HttpMethod.POST, root);
		String check = "error";
		String enoughSL = "Not Enough";
		
		if (null == fileInfo || null == fileInfo.getContentFile()) {
			dto.setErrorCode(999);
			dto.setResponseData("Số hóa đơn còn lại không đủ để ký.");
			return dto;
		}
		if(fileInfo.getCheck().equals(enoughSL)) {
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
			hR.put("DateTime", LocalDateTime.now().toString());
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
		String url = "/einvoice/signAll";
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
		
		try {
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
		}catch (Exception e) {
			dto.setErrorCode(999);
			return dto;
		}
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
	
	
	
	@RequestMapping(value = "/check-history-mst",  produces = MediaType.APPLICATION_JSON_VALUE, method = RequestMethod.POST)
	@ResponseBody
	public BaseDTO execCheckHistoryMST(Locale locale, HttpServletRequest req, HttpSession session
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

		BaseDTO baseDTO = new BaseDTO(req);
		Msg msg = baseDTO.createMsg(cup, Constants.MSG_ACTION_CODE.INQUIRY);
		HashMap<String, String> hData = new HashMap<>();
		hData.put("MST", mst);
		msg.setObjData(hData);
		JSONRoot root = new JSONRoot(msg);
		MsgRsp rsp = restAPI.callAPINormal("/einvoice/check_history_mst", cup.getLoginRes().getToken(), HttpMethod.POST, root);
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
}
	