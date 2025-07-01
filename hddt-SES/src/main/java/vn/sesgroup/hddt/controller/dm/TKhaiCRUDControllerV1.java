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
	"/tkhai-creV1"
	, "/tkhai-editV1"
	, "/tkhai-detailV1"
	, "/tkhai-copyV1"
	, "/tkhai-signV1"
	, "/tkhai-delV1"
})
@Scope(value = WebApplicationContext.SCOPE_REQUEST)
public class TKhaiCRUDControllerV1 extends AbstractController{
	private static final Logger log = LogManager.getLogger(TKhaiCRUDControllerV1.class);
	@Autowired RestAPIUtility restAPI;
	@Autowired RestTemplate restTemplate;
	
	private String errorCode;
	private String errorDesc;
	private String _id;
	private String tenNnt;
	private String mauSo;
	private String ten;
	private String mst;
	private String tinhThanh;
	private String cqtQLy;
	private String nlHe;
	private String dcLHe;
	private String dcCTDTu;
	private String dtLHe;
	
	private String tnddpLuat;
	private String dtddpLuat;
	private String cccDan;
	private String shChieu;
	private String nsddpLuat;
	private String gTinh;
	
	private String nLap;
	private String htHDon;
	private String pthuc;
	private String lhdSDung_HDGTGT;
	private String CMMTT;
	private String lhdsDung_HDBHang;
	private String lhdsDung_HDBTSCong;
	private String lhdsDung_HDBHDTQGia;
	private String lhdsDung_HDKhac;
	private String lhdsDung_CTu;
	
	private String lhdsDung_HDGTGTTHBLai;
	private String lhdsDung_HDBHTHBLai;
	private String lhdsDung_HDTMai;
	private String dsctsSDung;
	
	private String hThuc;
	
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
			msgParam.setParam("DMTinhThanh");
			msgParams.getParams().add(msgParam);
			
			msgParam = new MsgParam();
			msgParam.setId("param02");
			msgParam.setParam("DMChiCucThue");			
			conds = new ArrayList<>();
			hashConds = new HashMap<>();
			hashConds.put("cond", "tinhthanh_ma");
			hashConds.put("condval", null == tinhThanh? "": tinhThanh);
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
			
			/*END: DANH SACH THAM SO*/
			msg.setObjData(msgParams);
			
			JSONRoot root = new JSONRoot(msg);
			MsgRsp rsp = restAPI.callAPINormal("/commons/get-full-params", cup.getLoginRes().getToken(), HttpMethod.POST, root);
			MspResponseStatus rspStatus = rsp.getResponseStatus();
			
			if(rspStatus.getErrorCode() == 0 &&
					rsp.getObjData() != null) {
				LinkedHashMap<String, String> hItem = null;
				
				JsonNode jsonData = Json.serializer().nodeFromObject(rsp.getObjData());
				if(null != jsonData.at("/param01") && jsonData.at("/param01") instanceof ArrayNode) {
					hItem = new LinkedHashMap<String, String>();
					for(JsonNode o: jsonData.at("/param01")) {
						hItem.put(commons.getTextJsonNode(o.get("code")), commons.getTextJsonNode(o.get("name")));
					}
					req.setAttribute("map_dmtinhthanh", hItem);
				}
				if(null != jsonData.at("/param02") && jsonData.at("/param02") instanceof ArrayNode) {
					hItem = new LinkedHashMap<String, String>();
					for(JsonNode o: jsonData.at("/param02")) {
						hItem.put(commons.getTextJsonNode(o.get("code")), commons.getTextJsonNode(o.get("name")));
					}
					req.setAttribute("map_cucthue", hItem);
				}
				
				
				if(null != jsonData.at("/param03") && jsonData.at("/param03") instanceof ArrayNode) {
					hItem = new LinkedHashMap<String, String>();
					for(JsonNode o: jsonData.at("/param03")) {
						hItem.put(commons.getTextJsonNode(o.get("code")), commons.getTextJsonNode(o.get("name")));
					}
					req.setAttribute("map_mstkhai", hItem);
				}
				if(null != jsonData.at("/param04") && jsonData.at("/param04") instanceof ArrayNode) {
					hItem = new LinkedHashMap<String, String>();
					for(JsonNode o: jsonData.at("/param04")) {
						hItem.put(commons.getTextJsonNode(o.get("code")), commons.getTextJsonNode(o.get("name")));
					}
					req.setAttribute("map_httkhai", hItem);
				}
				
			}
		}catch(Exception e) {}
	}
	
	@RequestMapping(value = "/init", method = {RequestMethod.POST})
	public String init(Locale locale, HttpServletRequest req, HttpSession session
			, @RequestAttribute(name = "transaction", value = "", required = false) String transaction) throws Exception{
		errorCode = "";
		errorDesc = "";
		
		CurrentUserProfile cup = getCurrentlyAuthenticatedPrincipal();
		IssuerInfo ii = cup.getLoginRes().getIssuerInfo();
		
		LocalDate now = LocalDate.now();
		ten = "Tờ khai đăng ký/thay đổi thông tin sử dụng hóa đơn điện tử";
		nLap = commons.convertLocalDateTimeToString(now, Constants.FORMAT_DATE.FORMAT_DATE_WEB);
		
		req.setAttribute("tenNnt", ii.getName());
		req.setAttribute("ten", ten);
		req.setAttribute("nLap", nLap);
		req.setAttribute("mst", ii.getTaxCode());
		
		req.setAttribute("optHTHDon", "CMa");
		req.setAttribute("optPThuc", "CDDu");
		req.setAttribute("LHDSDung_HDGTGT", true);
		
		
		_id = commons.getParameterFromRequest(req, "_id");
		String header = "Đăng ký tờ khai";
		String action = "CREATE";
		boolean isEdit = false;
		
		switch (transaction) {
		case "tkhai-creV1":
			header = "Đăng ký tờ khai";
			action = "CREATE";
			isEdit = true;
			req.setAttribute( "MSCode", "01/ĐKTĐ-HĐĐT");
			req.setAttribute("HThuc", "1");
			break;
		case "tkhai-editV1":
			header = "Thay đổi thông tin tờ khai";
			action = "EDIT";
			isEdit = true;
			break;
		case "tkhai-detailV1":
			header = "Chi tiết tờ khai";
			action = "DETAIL";
			isEdit = false;
			break;
		case "tkhai-copyV1":
			header = "Copy tờ khai";
			action = "COPY";
			isEdit = true;
			req.setAttribute("HThuc", "2");
			break;
		case "tkhai-signV1":
			header = "Ký tờ khai";
			action = "SIGN";
			isEdit = false;
			break;
		default:
			break;
		}
		
		if("|tkhai-detailV1|tkhai-copyV1|tkhai-signV1|tkhai-editV1|".indexOf(transaction) != -1)
			inquiry(cup, locale, req, session, _id, action);
		
		req.setAttribute("_header_", header);
		req.setAttribute("_action_", action);
		req.setAttribute("_isedit_", isEdit);
		req.setAttribute("_id", _id);
		
		LoadParameter(cup, locale, req, action);
		
		if(!"".equals(errorDesc))
			req.setAttribute("messageError", errorDesc);
		
		return "dm/tkhai-crudV1";
	}
	
	private void inquiry(CurrentUserProfile cup, Locale locale, HttpServletRequest req, HttpSession session, String _id , String action) throws Exception{
		if("".equals(_id)) {
			errorCode = "NOT FOUND";
			errorDesc = "Không tìm thấy thông tin tờ khai.";
			return;
		}
		BaseDTO baseDTO = new BaseDTO(req);
		Msg msg = baseDTO.createMsg(cup, Constants.MSG_ACTION_CODE.INQUIRY);
		HashMap<String, String> hData = new HashMap<>();
		msg.setObjData(hData);
		
		JSONRoot root = new JSONRoot(msg);
		MsgRsp rsp = restAPI.callAPINormal("/tkhai/detailV1/" + _id, cup.getLoginRes().getToken(), HttpMethod.POST, root);
		MspResponseStatus rspStatus = rsp.getResponseStatus();
		if(rspStatus.getErrorCode() == 0) {
			JsonNode jsonData = Json.serializer().nodeFromObject(rsp.getObjData());
			
			if("EDIT|SIGN".indexOf(action) != -1 &&
					!"CREATED".equals(commons.getTextJsonNode(jsonData.at("/Status")))) {
				errorCode = "NOT FOUND";
				errorDesc = "Trạng thái tờ khai không hợp lệ.";
				return;
			}
			
			req.setAttribute("tenNnt", commons.getTextJsonNode(jsonData.at("/TenNnt")));
			req.setAttribute("MSo", commons.getTextJsonNode(jsonData.at("/MSo")));
			req.setAttribute("MSCode", commons.getTextJsonNode(jsonData.at("/MSo")));
			req.setAttribute("ten", commons.getTextJsonNode(jsonData.at("/Ten")));
			req.setAttribute("TNDDPLuat", commons.getTextJsonNode(jsonData.at("/TNDDPLuat")));
			req.setAttribute("DTDDPLuat", commons.getTextJsonNode(jsonData.at("/DTDDPLuat")));
			req.setAttribute("CCCDan", commons.getTextJsonNode(jsonData.at("/CCCDan")));
			req.setAttribute("SHChieu", commons.getTextJsonNode(jsonData.at("/SHChieu")));
			req.setAttribute("NSDDPLuat", commons.convertLocalDateTimeToString(commons.convertLongToLocalDate(jsonData.at("/NSDDPLuat").asLong()), Constants.FORMAT_DATE.FORMAT_DATE_WEB));
			String sex = commons.getTextJsonNode(jsonData.at("/GTinh"));
			if (sex.equals("0")) req.setAttribute("GTinhText", "Nữ");
			if (sex.equals("1")) req.setAttribute("GTinhText", "Nam");
			req.setAttribute("GTinh", sex);
			
			String ht = commons.getTextJsonNode(jsonData.at("/HThuc"));
			if(ht.equals("1")) {
				ht = "Đăng ký mới";
			}
			if(ht.equals("2")) {
				ht = "Thay đổi thông tin";
			}

			req.setAttribute("HThuc", ht);
			req.setAttribute("HTCode", commons.getTextJsonNode(jsonData.at("/HThuc")));
		
			tinhThanh = commons.getTextJsonNode(jsonData.at("/TinhThanhInfo/code"));
			req.setAttribute("TThanhCode", tinhThanh);
			req.setAttribute("TThanhName", commons.getTextJsonNode(jsonData.at("/TinhThanhInfo/name")));
			
			cqtQLy = commons.getTextJsonNode(jsonData.at("/ChiCucThueInfo/code"));
			req.setAttribute("CQThueCode", cqtQLy);
			req.setAttribute("CQThueName", commons.getTextJsonNode(jsonData.at("/ChiCucThueInfo/name")));
			
			req.setAttribute("NLHe", commons.getTextJsonNode(jsonData.at("/NLHe")));
			req.setAttribute("DCLHe", commons.getTextJsonNode(jsonData.at("/DCLHe")));
			req.setAttribute("DCTDTu", commons.getTextJsonNode(jsonData.at("/DCTDTu")));
			req.setAttribute("DTLHe", commons.getTextJsonNode(jsonData.at("/DTLHe")));
			if (!action.equals("COPY")) {
				req.setAttribute("nLap", commons.convertLocalDateTimeToString(commons.convertLongToLocalDate(jsonData.at("/NLap").asLong()), Constants.FORMAT_DATE.FORMAT_DATE_WEB));				
			}
			
			req.setAttribute("optHTHDon", commons.getTextJsonNode(jsonData.at("/HTHDon")));
			
			req.setAttribute("optPThuc", commons.getTextJsonNode(jsonData.at("/PThuc")));
			req.setAttribute("LHDSDung_HDGTGT", "1".equals(commons.getTextJsonNode(jsonData.at("/LHDSDung/HDGTGT"))));
			req.setAttribute("CMMTT", "1".equals(commons.getTextJsonNode(jsonData.at("/CMMTTien"))));
			req.setAttribute("LHDSDung_HDBHang", "1".equals(commons.getTextJsonNode(jsonData.at("/LHDSDung/HDBHang"))));
			
			req.setAttribute("LHDSDung_HDGTGTTHBLai", "1".equals(commons.getTextJsonNode(jsonData.at("/LHDSDung/HDGTGTTHBLai"))));
			req.setAttribute("LHDSDung_HDBHTHBLai", "1".equals(commons.getTextJsonNode(jsonData.at("/LHDSDung/HDBHTHBLai"))));
			req.setAttribute("LHDSDung_HDTMai", "1".equals(commons.getTextJsonNode(jsonData.at("/LHDSDung/HDTMai"))));
			
			req.setAttribute("LHDSDung_HDBTSCong", "1".equals(commons.getTextJsonNode(jsonData.at("/LHDSDung/HDBTSCong"))));
			req.setAttribute("LHDSDung_HDBHDTQGia", "1".equals(commons.getTextJsonNode(jsonData.at("/LHDSDung/HDBHDTQGia"))));
			req.setAttribute("LHDSDung_HDKhac", "1".equals(commons.getTextJsonNode(jsonData.at("/LHDSDung/HDKhac"))));
			req.setAttribute("LHDSDung_CTu", "1".equals(commons.getTextJsonNode(jsonData.at("/LHDSDung/CTu"))));
			
			HashMap<String, String> hItem = null;
			List<Object> prds = new ArrayList<Object>();
			if(!jsonData.at("/DSCTSSDung").isMissingNode()) {
				for(JsonNode o: jsonData.at("/DSCTSSDung")) {
					hItem = new LinkedHashMap<String, String>();
					hItem.put("TTChuc", commons.getTextJsonNode(o.at("/TTChuc")));
					hItem.put("Seri", commons.getTextJsonNode(o.at("/Seri")));
					hItem.put("TNgay", commons.convertLocalDateTimeToString(commons.convertLongToLocalDateTime(o.at("/TNgay").asLong()), Constants.FORMAT_DATE.FORMAT_DATE_TIME_WEB));
					hItem.put("DNgay", commons.convertLocalDateTimeToString(commons.convertLongToLocalDateTime(o.at("/DNgay").asLong()), Constants.FORMAT_DATE.FORMAT_DATE_TIME_WEB));
//					if("EDIT".equals(action))
						hItem.put("HThuc", commons.getTextJsonNode(o.at("/HThuc")));
//					else
//						hItem.put("HThuc", Constants.MAP_TKHAI_HTHUC.get(commons.getTextJsonNode(o.at("/HThuc"))));
//					
					prds.add(hItem);
				}
			}
			req.setAttribute("DSCTSSDung", Json.serializer().toString(prds));
			
			if (!action.equals("COPY")) return;
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
		}else {
			errorDesc = rspStatus.getErrorDesc();
		}
	}
	
	private void validateEmpty(String value, String message, BaseDTO dto) {
		if (value == null || value.trim().isEmpty()) {
			dto.setErrorCode(1);
			dto.getErrorMessages().add(message);
		}
	}
	
	public BaseDTO checkDataToAccept(HttpServletRequest req, HttpSession session, String transaction
			, CurrentUserProfile cup) throws Exception{
		BaseDTO dto = new BaseDTO();
		dto.setErrorCode(0);
		
		_id = commons.getParameterFromRequest(req, "_id").replaceAll("\\s", "");
		tenNnt = commons.getParameterFromRequest(req, "ten-nnt").trim().replaceAll("\\s+", " ");
		mauSo = commons.getParameterFromRequest(req, "mau-so").replaceAll("\\s", "");
		ten = commons.getParameterFromRequest(req, "ten").trim().replaceAll("\\s+", " ");
		mst = commons.getParameterFromRequest(req, "mst").replaceAll("\\s", "");
		tinhThanh = commons.getParameterFromRequest(req, "tinh-thanh").replaceAll("\\s", "");
		cqtQLy = commons.getParameterFromRequest(req, "CQTQLy").replaceAll("\\s", "");
		nlHe = commons.getParameterFromRequest(req, "NLHe").trim().replaceAll("\\s+", " ");
		dcLHe = commons.getParameterFromRequest(req, "DCLHe").trim().replaceAll("\\s+", " ");
		dcCTDTu = commons.getParameterFromRequest(req, "DCTDTu").trim().replaceAll("\\s+", " ");
		dtLHe = commons.getParameterFromRequest(req, "DTLHe").trim().replaceAll("\\s+", " ");
		nLap = commons.getParameterFromRequest(req, "NLap").replaceAll("\\s", "");
		
		tnddpLuat = commons.getParameterFromRequest(req, "TNDDPLuat").replaceAll("\\s+", " ");
		dtddpLuat = commons.getParameterFromRequest(req, "DTDDPLuat").replaceAll("\\s+", "");
		cccDan = commons.getParameterFromRequest(req, "CCCDan").replaceAll("\\s+", "");
		shChieu = commons.getParameterFromRequest(req, "SHChieu").replaceAll("\\s+", "");
		nsddpLuat = commons.getParameterFromRequest(req, "NSDDPLuat").replaceAll("\\s", "");
		gTinh = commons.getParameterFromRequest(req, "GTinh").replaceAll("\\s", "");
		
		String hthDon_KCM = commons.getParameterFromRequest(req, "HTHDon_KCM").replaceAll("\\s", "");
		htHDon = !hthDon_KCM.equals("") ? hthDon_KCM: "";
		String hthDon_CM = commons.getParameterFromRequest(req, "HTHDon_CM").replaceAll("\\s", "");
		htHDon = !hthDon_CM.equals("") ? hthDon_CM: htHDon;
		pthuc = commons.getParameterFromRequest(req, "PThuc").replaceAll("\\s", "");
		lhdSDung_HDGTGT = commons.getParameterFromRequest(req, "LHDSDung_HDGTGT").replaceAll("\\s", "");
		CMMTT = commons.getParameterFromRequest(req, "CMMTT").replaceAll("\\s", "");
		lhdsDung_HDBHang = commons.getParameterFromRequest(req, "LHDSDung_HDBHang").replaceAll("\\s", "");
		lhdsDung_HDBTSCong = commons.getParameterFromRequest(req, "LHDSDung_HDBTSCong").replaceAll("\\s", "");
		lhdsDung_HDBHDTQGia = commons.getParameterFromRequest(req, "LHDSDung_HDBHDTQGia").replaceAll("\\s", "");
		lhdsDung_HDKhac = commons.getParameterFromRequest(req, "LHDSDung_HDKhac").replaceAll("\\s", "");
		lhdsDung_CTu = commons.getParameterFromRequest(req, "LHDSDung_CTu").replaceAll("\\s", "");
		
		lhdsDung_HDGTGTTHBLai = commons.getParameterFromRequest(req, "LHDSDung_HDGTGTTHBLai").replaceAll("\\s", "");
		lhdsDung_HDBHTHBLai = commons.getParameterFromRequest(req, "LHDSDung_HDBHTHBLai").replaceAll("\\s", "");
		lhdsDung_HDTMai = commons.getParameterFromRequest(req, "LHDSDung_HDTMai").replaceAll("\\s", "");
		
		dsctsSDung = commons.getParameterFromRequest(req, "DSCTSSDung").replaceAll("\\s", "");
		hThuc = commons.getParameterFromRequest(req, "HThuc").replaceAll("\\s", "");
		
		switch (transaction) {
		case "tkhai-creV1":
		case "tkhai-editV1":
		case "tkhai-copyV1":
			if (!hthDon_KCM.equals("") && !hthDon_CM.equals("")) {
				dto.setErrorCode(1);
				dto.getErrorMessages().add("Không được chọn đồng thời 2 hình thức hóa đơn.");
			}
			validateEmpty(tenNnt, "Vui lòng nhập tên người nộp thuế.", dto);
			validateEmpty(mst, "Vui lòng nhập mã số thuế.", dto);
			validateEmpty(mauSo, "Vui lòng chọn mẫu số.", dto);
			validateEmpty(ten, "Vui lòng nhập tên tờ khai.", dto);
			validateEmpty(cqtQLy, "Vui lòng chọn cơ quan thuế quản lý.", dto);
			validateEmpty(dcLHe, "Vui lòng nhập địa chỉ liên hệ.", dto);
			validateEmpty(dcCTDTu, "Vui lòng nhập email liên hệ.", dto);
			validateEmpty(nLap, "Vui lòng nhập ngày lập.", dto);
			
			validateEmpty(tnddpLuat, "Vui lòng nhập tên người đại diện pháp luật.", dto);
			validateEmpty(dtddpLuat, "Vui lòng nhập diện thoại người đại diện pháp luật.", dto);
			validateEmpty(nsddpLuat, "Vui lòng nhập ngày sinh đại diện pháp luật.", dto);

			if (!"".equals(dcCTDTu) && !commons.isValidEmailAddress(dcCTDTu)) {
				dto.setErrorCode(1);
				dto.getErrorMessages().add("Địa chỉ email không đúng.");
			}
			
			if (!commons.checkLocalDate(nLap, Constants.FORMAT_DATE.FORMAT_DATE_WEB)) {
				dto.setErrorCode(1);
				dto.getErrorMessages().add("Ngày lập không đúng định dạng.");
			}
			
			if (!commons.checkLocalDate(nsddpLuat, Constants.FORMAT_DATE.FORMAT_DATE_WEB)) {
				dto.setErrorCode(1);
				dto.getErrorMessages().add("Ngày sinh đại diện pháp luật không đúng định dạng.");
			}
			
			if ("W10=".equals(dsctsSDung)) {
				dto.setErrorCode(1);
				dto.getErrorMessages().add("Vui lòng chọn chứng thư số.");
			}
			
			if ("|1|2|".indexOf("|" + hThuc + "|") == -1) {
				dto.setErrorCode(1);
				dto.getErrorMessages().add("Vui lòng kiểm tra lại hình thức đăng ký.");
			}

			if ("".equals(htHDon) && !"on".equals(CMMTT)) {
				dto.setErrorCode(1);
				dto.getErrorMessages().add("Vui lòng kiểm tra lại hình thức hóa đơn.");
			}

			if ("".equals(pthuc) || "|CDDu|CBTHop|".indexOf("|" + pthuc + "|") == -1) {
				dto.setErrorCode(1);
				dto.getErrorMessages().add("Vui lòng kiểm tra lại phương thức chuyển dữ liệu hóa đơn điện tử.");
			}

			if (!"on".equals(lhdSDung_HDGTGT) && !"on".equals(lhdsDung_HDBHang) && !"on".equals(lhdsDung_HDBTSCong)
					&& !"on".equals(lhdsDung_HDBHDTQGia) && !"on".equals(lhdsDung_HDKhac) && !"on".equals(lhdsDung_CTu)
					&& !"on".equals(lhdsDung_HDGTGTTHBLai) && !"on".equals(lhdsDung_HDBHTHBLai)
					&& !"on".equals(lhdsDung_HDTMai)) {
				dto.setErrorCode(1);
				dto.getErrorMessages().add("Vui lòng chọn ít nhất 1 loại hóa đơn sử dụng.");
			}
			break;
		case "tkhai-delV1":
			if ("".equals(_id)) {
				dto.setErrorCode(1);
				dto.getErrorMessages().add("Không tìm thấy thông tin tờ khai.");
			}
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
		String messageConfirm = "Bạn có muốn đăng ký tờ khai không?";
		switch (transaction) {
		case "tkhai-creV1":
			messageConfirm = "Bạn có muốn đăng ký tờ khai không?";
			break;
		case "tkhai-editV1":
			messageConfirm = "Bạn có muốn thay đổi tờ khai không?";
			break;
		case "tkhai-copyV1":
			messageConfirm = "Bạn có muốn copy tờ khai không?";
			break;
		case "tkhai-delV1":
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
		if(0 != dto.getErrorCode()) {
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
		try {
			jsonNodeTmp = Json.serializer().nodeFromJson(commons.decodeBase64ToString(dsctsSDung));
		}catch(Exception e) {
			log.error(" >>>>> An exception occurred!", e);
		}
		
		String actionCode = Constants.MSG_ACTION_CODE.CREATED;
		switch (transaction) {
		case "tkhai-creV1": actionCode = Constants.MSG_ACTION_CODE.CREATED; break;
		case "tkhai-editV1": actionCode = Constants.MSG_ACTION_CODE.MODIFY; break;
		case "tkhai-copyV1": actionCode = Constants.MSG_ACTION_CODE.COPY; break;
		case "tkhai-delV1": actionCode = Constants.MSG_ACTION_CODE.DELETE; break;
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
		hData.put("TenNnt", tenNnt);
		hData.put("MauSo", mauSo);
		hData.put("Ten", ten);
		hData.put("Mst", mst);
		hData.put("TinhThanh", tinhThanh);
		hData.put("CqtQLy", cqtQLy);
		hData.put("NLHe", nlHe);
		hData.put("DCLHe", dcLHe);
		hData.put("DCCTDTu", dcCTDTu);
		hData.put("DTLHe", dtLHe);
		hData.put("NLap", nLap);
		
		hData.put("TNDDPLuat", tnddpLuat);
		hData.put("DTDDPLuat", dtddpLuat);
		hData.put("CCCDan", cccDan);
		hData.put("SHChieu", shChieu);
		hData.put("NSDDPLuat", nsddpLuat);
		hData.put("GTinh", gTinh);
		
		hData.put("HTHDon", htHDon);
		hData.put("PThuc", pthuc);
		hData.put("CMMTT", CMMTT);
		hData.put("LHDSDung_HDGTGT", lhdSDung_HDGTGT);
		hData.put("LHDSDung_HDBHang", lhdsDung_HDBHang);
		hData.put("LHDSDung_HDBTSCong", lhdsDung_HDBTSCong);
		hData.put("LHDSDung_HDBHDTQGia", lhdsDung_HDBHDTQGia);
		hData.put("LHDSDung_HDKhac", lhdsDung_HDKhac);
		hData.put("LHDSDung_CTu", lhdsDung_CTu);
		
		hData.put("LHDSDung_HDGTGTTHBLai", lhdsDung_HDGTGTTHBLai);
		hData.put("LHDSDung_HDBHTHBLai", lhdsDung_HDBHTHBLai);
		hData.put("LHDSDung_HDTMai", lhdsDung_HDTMai);
		
		hData.put("DSCTSSDung", jsonNodeTmp);
		
		hData.put("HThuc", hThuc);
		msg.setObjData(hData);
		
		JSONRoot root = new JSONRoot(msg);
		MsgRsp rsp = restAPI.callAPINormal("/tkhai/crudV1", cup.getLoginRes().getToken(), HttpMethod.POST, root);
		MspResponseStatus rspStatus = rsp.getResponseStatus();
		if(rspStatus.getErrorCode() == 0) {
			dtoRes.setErrorCode(0);
			switch (transaction) {
			case "tkhai-creV1": dtoRes.setResponseData("Đăng ký tờ khai thành công.");break;
			case "tkhai-editV1": dtoRes.setResponseData("Thay đổi tờ khai thành công.");break;
			case "tkhai-copyV1": dtoRes.setResponseData("Copy tờ khai thành công.");break;
			case "tkhai-delV1": dtoRes.setResponseData("Xóa tờ khai thành công.");break;
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
			dto.setResponseData("Không tìm thấy tờ khai cần ký.");
			return dto;
		}
		
		/*LAY THONG TIN DU LIEU XML VE SERVER WEB*/
		dto = new BaseDTO(req);
		Msg msg = dto.createMsg(cup, Constants.MSG_ACTION_CODE.CREATED);
		HashMap<String, Object> hData = new HashMap<>();
		hData.put("_id", _id);
		msg.setObjData(hData);
		JSONRoot root = new JSONRoot(msg);
		
		FileInfo fileInfo = restAPI.callAPIGetFileInfo("/tkhai/get-file-for-sign", cup.getLoginRes().getToken(), HttpMethod.POST, root);
		if(null == fileInfo.getContentFile()) {
			dto.setErrorCode(999);
			dto.setResponseData("Không tìm thấy dữ liệu tờ khai.");
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
			, @RequestParam("_id") String _id
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
		body.add("_id", _id);
		
		/*ADD DU LIEU XML DA KY*/
		fileMap = new LinkedMultiValueMap<String, String>();
		contentDisposition = ContentDisposition.builder("form-data").name("XMLFileSigned").filename("einvoice-signed.xml").build();
		fileMap.add(HttpHeaders.CONTENT_DISPOSITION, contentDisposition.toString());
		fileEntity = new HttpEntity<byte[]>(multipartFile.getBytes(), fileMap);
		body.add("XMLFileSigned", fileEntity);
		
		HttpEntity<MultiValueMap<String, Object>> requestBody = new HttpEntity<>(body, headers);
		String url = "/tkhai/sign-singleV1";
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
			dtoRes = new BaseDTO(result.getStatusCode().value(), "Thực hiện tờ khai không thành công.");
		}
		return dtoRes;
	}
	
}
