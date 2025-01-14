package vn.sesgroup.hddt.controller.user;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.context.WebApplicationContext;

import com.api.message.JSONRoot;
import com.api.message.Msg;
import com.api.message.MsgRsp;
import com.api.message.MspResponseStatus;
import com.fasterxml.jackson.databind.JsonNode;

import vn.sesgroup.hddt.controller.AbstractController;
import vn.sesgroup.hddt.dto.BaseDTO;
import vn.sesgroup.hddt.dto.CurrentUserProfile;
import vn.sesgroup.hddt.dto.LoginRes;
import vn.sesgroup.hddt.resources.RestAPIUtility;
import vn.sesgroup.hddt.utils.Constants;
import vn.sesgroup.hddt.utils.Json;

@Controller
@Scope(value = WebApplicationContext.SCOPE_REQUEST)
public class MainController extends AbstractController {
	@Autowired
	RestAPIUtility restAPI;

	@RequestMapping(value = { "/main", "/main/{transaction}/{method}",
			"/main/{transaction}/{method}/{param1}" }, method = { RequestMethod.GET, RequestMethod.POST })
	public String main(@PathVariable(name = "transaction", required = false, value = "") String transaction,
			@PathVariable(name = "method", required = false, value = "") String method,
			@PathVariable(name = "param1", required = false, value = "") String param1, HttpServletRequest request)
			throws Exception {

		if (null == transaction)
			transaction = "";
		if (null == method)
			method = "";
		if (null == param1)
			param1 = "";

		CurrentUserProfile cup = getCurrentlyAuthenticatedPrincipal();
		boolean admin = true;
		LoginRes issu = cup.getLoginRes();
		String CompanyName = issu.getIssuerInfo().getName();
		String UserName = issu.getUserName();
			
			JSONRoot root = new JSONRoot();
			
			BaseDTO dtoRes = new BaseDTO();
			Msg msg = dtoRes.createMsgPass();
			HashMap<String, String> hInput = new HashMap<>();
			msg.setObjData(hInput);
			root = new JSONRoot(msg);
			MsgRsp rsp = restAPI.callAPIPass("/forgotpass/dl", HttpMethod.POST, root);
			MspResponseStatus rspStatus = rsp.getResponseStatus();
			JsonNode rows = null;
			if (rspStatus.getErrorCode() == 0) {
				JsonNode jsonData = Json.serializer().nodeFromObject(rsp.getObjData());
				if (!jsonData.at("/rows").isMissingNode()) {
					rows = jsonData.at("/rows");
					for (JsonNode row : rows) {
						request.setAttribute("LOGO", commons.getTextJsonNode(row.at("/LOGO")));
						request.setAttribute("PHONE", commons.getTextJsonNode(row.at("/PHONE")));
						request.setAttribute("EMAIL", commons.getTextJsonNode(row.at("/EMAIL")));
					}
				}
			}
			
			if (admin == issu.isAdmin()) {
			/* GET LIST PARAM ADMIN */
			
			dtoRes = new BaseDTO();
			msg = dtoRes.createMsgMain(cup, Constants.MSG_ACTION_CODE.SEARCH);
			hInput = new HashMap<>();
			msg.setObjData(hInput);		
			root = new JSONRoot(msg);
			
			String MS_EXPIRES = "30";
			String CKS_EXPIRES = "3";
			rsp = restAPI.callAPINormal("/param-admin/detail", cup.getLoginRes().getToken(), HttpMethod.POST, root);
			rspStatus = rsp.getResponseStatus();
			rows = null;
			if (rspStatus.getErrorCode() == 0) {	
				JsonNode jsonData = Json.serializer().nodeFromObject(rsp.getObjData());			
				MS_EXPIRES = commons.getTextJsonNode(jsonData.at("/MS_EXPIRES"));	
				CKS_EXPIRES = commons.getTextJsonNode(jsonData.at("/CKS_EXPIRES"));
			}
			int CKS_EXPIRES_ = Integer.parseInt(CKS_EXPIRES);
			
			
			/* GET LIST MAU SO GAN HET HAN */
							
			dtoRes = new BaseDTO();
			msg = dtoRes.createMsgMain(cup, Constants.MSG_ACTION_CODE.SEARCH);
			hInput = new HashMap<>();
		
			hInput.put("TaxCode", "");
			hInput.put("TyLe", MS_EXPIRES);
			hInput.put("Status", "");
			
			msg.setObjData(hInput);
			
			root = new JSONRoot(msg);
			rsp = restAPI.callAPIPass("/mauso-expires/list", HttpMethod.POST, root);
			rspStatus = rsp.getResponseStatus();
			rows = null;
			if (rspStatus.getErrorCode() == 0) {		
				request.setAttribute("SOLUONG_EXPIRES", rspStatus.getErrorDesc());					
			}else {
				request.setAttribute("SOLUONG_EXPIRES", "0");	
			}
			
			
			/* GET LIST CKS GAN HET HAN */
			
			dtoRes = new BaseDTO();
			msg = dtoRes.createMsgMain(cup, Constants.MSG_ACTION_CODE.SEARCH);
			hInput = new HashMap<>();
			LocalDate now = LocalDate.now();
			
			hInput.put("TaxCode", "");
			hInput.put("Name", "");
			hInput.put("FromDate", commons.convertLocalDateTimeToString(now, Constants.FORMAT_DATE.FORMAT_DATE_WEB));
			hInput.put("ToDate", commons.convertLocalDateTimeToString(now.plusMonths(CKS_EXPIRES_), Constants.FORMAT_DATE.FORMAT_DATE_WEB));
			hInput.put("Status", "");
			
			msg.setObjData(hInput);
			
			root = new JSONRoot(msg);
			rsp = restAPI.callAPIPass("/ca_invoice/list", HttpMethod.POST, root);
			rspStatus = rsp.getResponseStatus();
			rows = null;
			if (rspStatus.getErrorCode() == 0) {		
				request.setAttribute("CKS_EXPIRES", rspStatus.getErrorDesc());					
			}else {
				request.setAttribute("CKS_EXPIRES", "0");	
			}
						
			request.setAttribute("transaction", transaction);
			request.setAttribute("method", method);
			request.setAttribute("UserFullPathRight", cup.getAllRights());
			request.setAttribute("COMPANYNAME", CompanyName);
			request.setAttribute("TAXCODE", UserName);
			if (!("".equals(transaction) || "".equals(method))) {
				request.setAttribute(Constants.HAVE_ACTION_NAME, "OK");
				if (!"".equals(param1))
					return "forward:/" + transaction + "/" + method + "/" + param1;
				return "forward:/" + transaction + "/" + method;
			}
			
			return "/admin/admin";
		}else {
						
		root = new JSONRoot();
		rsp = restAPI.callAPINormal("/issu/mskh/" + issu.getIssuerId(), cup.getLoginRes().getToken(),
				HttpMethod.POST, root);
		JsonNode jsonData = Json.serializer().nodeFromObject(rsp.getObjData());
		rows = null;

		List<HashMap<String, Object>> rowsMSKH = null;
		HashMap<String, Object> hTmp = null;

		String mskh = null;
		String mshd = null;
		int SL = 0;
		int CL = 0;
		int DD = 0;
		int PercentCL = 0;
		int PercentDD = 0;
		if (!jsonData.at("/DMMauSoKyHieu").isMissingNode()) {
			rows = jsonData.at("/DMMauSoKyHieu");
			rowsMSKH = new ArrayList<HashMap<String, Object>>();
			int dem = 0;
			for (JsonNode row : rows) {
				String KHMSHDon = commons.getTextJsonNode(row.at("/KHMSHDon"));
				String KHHDon = commons.getTextJsonNode(row.at("/KHHDon"));
				String SoLuongs = commons.getTextJsonNode(row.at("/SoLuong"));
				String ConLais = commons.getTextJsonNode(row.at("/ConLai"));

				String LoaiHD = "";
				char index1 = KHHDon.charAt(3);
				String check6 = Character.toString(index1);
				if (SoLuongs != "") {
					String namekh = "";
					if ("1".equals(KHMSHDon)) {
						namekh = "Hóa đơn giá trị gia tăng";
						LoaiHD = "1";
					}
					if ("2".equals(KHMSHDon)) {
						namekh = "Hóa đơn bán hàng";
						LoaiHD = "2";
					}
					if ("3".equals(KHMSHDon)) {
						namekh = "Hóa đơn bán tài sản công";
						LoaiHD = "3";
					}
					if ("4".equals(KHMSHDon)) {
						namekh = "Hóa đơn bán hàng dự trữ quốc gia";
						LoaiHD = "4";
					}
					if ("5".equals(KHMSHDon)) {
						namekh = "Hóa đơn theo nghị định số 123/2020/NĐ-CP";
						LoaiHD = "5";
					}
					if ("6".equals(KHMSHDon) && "N".equals(check6)) {
						namekh = "Hóa đơn Phiếu xuất kho(Kiêm vận chuyển nội bộ)";
						LoaiHD = "6";
					}
					if ("6".equals(KHMSHDon) && "B".equals(check6)) {
						namekh = "Hóa đơn Hàng gửi bán đại lý điện tử";
						LoaiHD = "7";
					}

					hTmp = new HashMap<String, Object>();
					mskh = "Mẫu số" + " " + namekh + " :" + KHMSHDon + KHHDon;
					SL = Integer.parseInt(SoLuongs);
					CL = Integer.parseInt(ConLais);
					DD = SL - CL;
					mshd = KHMSHDon + KHHDon;
					
					if (CL == 0 && SL == 0) {
						PercentCL = 0;
						PercentDD = 0;
					} else if (CL == 0 && SL > 0) {
						PercentCL = 0;
						PercentDD = 100;
					} else {
						PercentCL = (CL * 100) / SL;
						PercentDD = 100 - PercentCL;
					}	
					
					hTmp.put("MSKH", mskh);
					hTmp.put("SL", SL);
					hTmp.put("CL", CL);
					hTmp.put("DD", DD);
					hTmp.put("MSHD", mshd);
					hTmp.put("PERCENT_CL", PercentCL);
					hTmp.put("PERCENT_DD", PercentDD);
					hTmp.put("LoaiHD", LoaiHD);
					rowsMSKH.add(hTmp);

					dem++;

					if (dem == 6) {
						break;
					}
				}

			}

			/* GET DATA ADD LIST ARRAY INVOICE TYPE */

			List<HashMap<String, Object>> rowsVAT = null;
			List<HashMap<String, Object>> rowsBH = null;
			List<HashMap<String, Object>> rowsPXKNB = null;
			List<HashMap<String, Object>> rowsPXKDL = null;

			rowsVAT = new ArrayList<HashMap<String, Object>>();
			rowsBH = new ArrayList<HashMap<String, Object>>();
			rowsPXKNB = new ArrayList<HashMap<String, Object>>();
			rowsPXKDL = new ArrayList<HashMap<String, Object>>();

			for (HashMap<String, Object> hashMap : rowsMSKH) {
				hTmp = new HashMap<String, Object>();

				String MSKH_ = hashMap.get("MSKH").toString();
				String SL_ = hashMap.get("SL").toString();
				String CL_ = hashMap.get("CL").toString();
				String DD_ = hashMap.get("DD").toString();
				String MSHD_ = hashMap.get("MSHD").toString();
				String PERCENT_CL_ = hashMap.get("PERCENT_CL").toString();
				String PERCENT_DD_ = hashMap.get("PERCENT_DD").toString();
				
				String LoaiHD_ = hashMap.get("LoaiHD").toString();

				hTmp.put("MSKH", MSKH_);
				hTmp.put("SL", SL_);
				hTmp.put("CL", CL_);
				hTmp.put("DD", DD_);
				hTmp.put("MSHD", MSHD_);
				hTmp.put("PERCENT_CL", PERCENT_CL_);
				hTmp.put("PERCENT_DD", PERCENT_DD_);
				hTmp.put("LoaiHD", LoaiHD_);

				if (LoaiHD_.equals("1")) {
					rowsVAT.add(hTmp);
				} else if (LoaiHD_.equals("2")) {
					rowsBH.add(hTmp);
				} else if (LoaiHD_.equals("6")) {
					rowsPXKNB.add(hTmp);
				} else if (LoaiHD_.equals("7")) {
					rowsPXKDL.add(hTmp);
				}

			}

			request.setAttribute("MauSoVAT", rowsVAT);
			request.setAttribute("MauSoBH", rowsBH);
			request.setAttribute("MauSoPXK", rowsPXKNB);
			request.setAttribute("MauSoPXKDL", rowsPXKDL);

			/* END GET DATA ADD LIST ARRAY INVOICE TYPE */
		}

		// LAY MAU SAC CAC NUT

		dtoRes = new BaseDTO();
		msg = dtoRes.createMsgPass();
		hInput = new HashMap<>();
		msg.setObjData(hInput);
		root = new JSONRoot(msg);
		rsp = restAPI.callAPINormal("/getColor/list", cup.getLoginRes().getToken(), HttpMethod.POST, root);
		rspStatus = rsp.getResponseStatus();
		rows = null;
		if (rspStatus.getErrorCode() == 0) {
			jsonData = Json.serializer().nodeFromObject(rsp.getObjData());
			if (!jsonData.at("/rows").isMissingNode()) {
				rows = jsonData.at("/rows");
				for (JsonNode row : rows) {
					String code = commons.getTextJsonNode(row.at("/LBTCode"));
					if (code.equals("1")) {
						request.setAttribute("ip_hdon", commons.getTextJsonNode(row.at("/Color")));
					}
					if (code.equals("2")) {
						request.setAttribute("ip_ds", commons.getTextJsonNode(row.at("/Color")));
					}
					if (code.equals("3")) {
						request.setAttribute("them_moi", commons.getTextJsonNode(row.at("/Color")));
					}
					if (code.equals("4")) {
						request.setAttribute("chi_tiet", commons.getTextJsonNode(row.at("/Color")));
					}
					if (code.equals("5")) {
						request.setAttribute("thay_doi", commons.getTextJsonNode(row.at("/Color")));
					}
					if (code.equals("6")) {
						request.setAttribute("dc_tt", commons.getTextJsonNode(row.at("/Color")));
					}
					if (code.equals("7")) {
						request.setAttribute("copy", commons.getTextJsonNode(row.at("/Color")));
					}
					if (code.equals("8")) {
						request.setAttribute("xuat_xml", commons.getTextJsonNode(row.at("/Color")));
					}
					if (code.equals("9")) {
						request.setAttribute("in", commons.getTextJsonNode(row.at("/Color")));
					}
					if (code.equals("10")) {
						request.setAttribute("in_cd", commons.getTextJsonNode(row.at("/Color")));
					}
					if (code.equals("11")) {
						request.setAttribute("ky", commons.getTextJsonNode(row.at("/Color")));
					}
					if (code.equals("12")) {
						request.setAttribute("xoa", commons.getTextJsonNode(row.at("/Color")));

					}
					if (code.equals("13")) {
						request.setAttribute("ky_hl", commons.getTextJsonNode(row.at("/Color")));

					}
					if (code.equals("14")) {
						request.setAttribute("send_cqt_hd", commons.getTextJsonNode(row.at("/Color")));

					}
					if (code.equals("15")) {
						request.setAttribute("lay_ma_hl", commons.getTextJsonNode(row.at("/Color")));

					}
					if (code.equals("16")) {
						request.setAttribute("send_mail_hl", commons.getTextJsonNode(row.at("/Color")));
					}
				}
			}
		}

		// END LAY MAU SAC CAC NUT

		request.setAttribute("transaction", transaction);
		request.setAttribute("method", method);
		request.setAttribute("UserFullPathRight", cup.getAllRights());
		request.setAttribute("COMPANYNAME", CompanyName);
		request.setAttribute("TAXCODE", UserName);
		if (!("".equals(transaction) || "".equals(method))) {
			request.setAttribute(Constants.HAVE_ACTION_NAME, "OK");
			if (!"".equals(param1))
				return "forward:/" + transaction + "/" + method + "/" + param1;
			return "forward:/" + transaction + "/" + method;
		}
			return "/user/main";
		}
	}
}
