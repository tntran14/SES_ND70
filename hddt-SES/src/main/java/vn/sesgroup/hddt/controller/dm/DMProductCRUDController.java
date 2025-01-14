package vn.sesgroup.hddt.controller.dm;

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
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
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
import vn.sesgroup.hddt.resources.RestAPIUtility;
import vn.sesgroup.hddt.utils.Constants;
import vn.sesgroup.hddt.utils.Json;

@Controller
@RequestMapping({
	"/dmproduct-cre",
	"/dmproduct-detail",
	"/dmproduct-edit",
	"/dmproduct-del"
})
@Scope(value = WebApplicationContext.SCOPE_REQUEST)
public class DMProductCRUDController extends AbstractController{
	private static final Logger log = LogManager.getLogger(DMProductCRUDController.class);
	@Autowired RestAPIUtility restAPI;
	
	private String errorCode;
	private String errorDesc;
	private String _id;
	private String code;
	private String name;
	private String stock;
	private String unit;
	private String price;
	private String vatRate;
	private String description;
	private String thdoiTonkho;
	private String remark;
	
	private String tkvt;
	private String tkgv;
	private String tkdt;
	private String loaivt;
	private String nh_vt1;
	private String nh_vt2;
	private String nh_vt3;
	private String sua_tk_tonkho;
	private String cach_tinh_gia_ton;
	private String tk_cl_vt;
	private String tk_dtnb;
	
	private String _token;
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
			msgParam.setParam("DMStock");
			msgParams.getParams().add(msgParam);
			
			msgParam = new MsgParam();
			msgParam.setId("param02");
			msgParam.setParam("DMProductGroup");
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
						hItem.put(commons.getTextJsonNode(o.get("Code")), commons.getTextJsonNode(o.get("Name")));
					}
					req.setAttribute("map_stock", hItem);
				}
				if(null != jsonData.at("/param02") && jsonData.at("/param02") instanceof ArrayNode) {
					hItem = new LinkedHashMap<String, String>();
					for(JsonNode o: jsonData.at("/param02")) {
						hItem.put(commons.getTextJsonNode(o.get("Code")), commons.getTextJsonNode(o.get("Name")));
					}
					req.setAttribute("map_productgroup", hItem);
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
		
		req.setAttribute("cach_tinh_gia_ton", "1 - Trung bình tháng");
		req.setAttribute("VatRate", "8");
		
		String header = "Thêm mới sản phẩm";
		String action = "CREATE";
		boolean isEdit = false;
		
		switch (transaction) {
		case "dmproduct-cre":
			header = "Thêm mới sản phẩm";
			action = "CREATE";
			isEdit = true;
			break;
		case "dmproduct-detail":
			header = "Chi tiết sản phẩm";
			action = "DETAIL";
			isEdit = false;
			break;
		case "dmproduct-edit":
			header = "Thay đổi thông tin sản phẩm";
			action = "EDIT";
			isEdit = true;
			break;
		default:
			break;
		}
		
		if("|dmproduct-detail|dmproduct-edit|".indexOf(transaction) != -1
			|| (!"".equals(_id) && "dmproduct-cre".equals(transaction))
		) {
			inquiry(cup, locale, req, session, _id, action);
		}
		
		req.setAttribute("_header_", header);
		req.setAttribute("_action_", action);
		req.setAttribute("_id", _id);
		req.setAttribute("_isedit_", isEdit);
		
		if("dmproduct-cre".equals(transaction)) {
			req.setAttribute("_id", "");
			req.setAttribute("Code", "");
		}
		
//		if("|dmproduct-cre|dmproduct-edit|".indexOf(transaction) != -1) {
//			LoadParameter(cup, locale, req, action);
//			req.setAttribute("map_vat", Constants.MAP_VAT);
//		}
		LoadParameter(cup, locale, req, action);
		req.setAttribute("map_vat", Constants.MAP_VAT);
		
		if(!"".equals(errorDesc))
			req.setAttribute("messageError", errorDesc);
		
		return "dm/product-crud";
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
		MsgRsp rsp = restAPI.callAPINormal("/dmproduct/detail/" + _id, cup.getLoginRes().getToken(), HttpMethod.POST, root);
		MspResponseStatus rspStatus = rsp.getResponseStatus();
		if(rspStatus.getErrorCode() == 0) {
			JsonNode jsonData = Json.serializer().nodeFromObject(rsp.getObjData());
			
			req.setAttribute("Code", commons.getTextJsonNode(jsonData.at("/Code")));
			req.setAttribute("Name", commons.getTextJsonNode(jsonData.at("/Name")));
			req.setAttribute("Stock", commons.getTextJsonNode(jsonData.at("/Stock")));
			req.setAttribute("Unit", commons.getTextJsonNode(jsonData.at("/Unit")));
			if(!jsonData.at("/Price").isMissingNode())
				req.setAttribute("Price", commons.formatNumberReal(jsonData.at("/Price").doubleValue()));
			if(!jsonData.at("/VatRate").isMissingNode())
				req.setAttribute("VatRate", commons.formatNumberReal(jsonData.at("/VatRate").doubleValue()));
			req.setAttribute("Description", commons.getTextJsonNode(jsonData.at("/Description")));
			if(!jsonData.at("/thdoi_tonkho").isMissingNode())
				req.setAttribute("thdoi_tonkho", jsonData.at("/thdoi_tonkho").asBoolean(false));
			req.setAttribute("Remark", commons.getTextJsonNode(jsonData.at("/Remark")));
			
			req.setAttribute("tkvt", commons.getTextJsonNode(jsonData.at("/tkvt")));
			req.setAttribute("tkgv", commons.getTextJsonNode(jsonData.at("/tkgv")));
			req.setAttribute("tkdt", commons.getTextJsonNode(jsonData.at("/tkdt")));
			req.setAttribute("loaivt", commons.getTextJsonNode(jsonData.at("/loaivt")));
			req.setAttribute("nh_vt1", commons.getTextJsonNode(jsonData.at("/nh_vt1")));
			req.setAttribute("nh_vt2", commons.getTextJsonNode(jsonData.at("/nh_vt2")));
			req.setAttribute("nh_vt3", commons.getTextJsonNode(jsonData.at("/nh_vt3")));
			if(!jsonData.at("/sua_tk_tonkho").isMissingNode())
				req.setAttribute("sua_tk_tonkho", jsonData.at("/sua_tk_tonkho").asBoolean(false));
			req.setAttribute("cach_tinh_gia_ton", "1 - Trung bình tháng");
			req.setAttribute("tk_cl_vt", commons.getTextJsonNode(jsonData.at("/tk_cl_vt")));
			req.setAttribute("tk_dtnb", commons.getTextJsonNode(jsonData.at("/tk_dtnb")));
		}else {
			errorDesc = rspStatus.getErrorDesc();
		}
	}
	
	public BaseDTO checkDataToAccept(HttpServletRequest req, HttpSession session, String transaction
			, CurrentUserProfile cup) throws Exception{
		BaseDTO dto = new BaseDTO();
		dto.setErrorCode(0);
		
		_id = commons.getParameterFromRequest(req, "_id").replaceAll("\\s", "");
		code = commons.getParameterFromRequest(req, "code").replaceAll("\\s", "").toUpperCase();
		name = commons.getParameterFromRequest(req, "name").trim().replaceAll("\\s+", " ");
		stock = commons.getParameterFromRequest(req, "stock").trim().replaceAll("\\s+", " ");
		unit = commons.getParameterFromRequest(req, "unit").trim().replaceAll("\\s+", " ");
		price = commons.getParameterFromRequest(req, "price").trim().replaceAll("\\s+", " ");
		vatRate = commons.getParameterFromRequest(req, "vat-rate").trim().replaceAll("\\s+", " ");
		description = commons.getParameterFromRequest(req, "description").trim().replaceAll("\\s+", " ");
		thdoiTonkho = commons.getParameterFromRequest(req, "thdoi_tonkho").trim().replaceAll("\\s+", " ");
		remark = commons.getParameterFromRequest(req, "remark").trim().replaceAll("\\s+", " ");
		
		tkvt = commons.getParameterFromRequest(req, "tkvt").trim().replaceAll("\\s+", " ");
		tkgv = commons.getParameterFromRequest(req, "tkgv").trim().replaceAll("\\s+", " ");
		tkdt = commons.getParameterFromRequest(req, "tkdt").trim().replaceAll("\\s+", " ");
		loaivt = commons.getParameterFromRequest(req, "loaivt").trim().replaceAll("\\s+", " ");
		nh_vt1 = commons.getParameterFromRequest(req, "nh_vt1").trim().replaceAll("\\s+", " ");
		nh_vt2 = commons.getParameterFromRequest(req, "nh_vt2").trim().replaceAll("\\s+", " ");
		nh_vt3 = commons.getParameterFromRequest(req, "nh_vt3").trim().replaceAll("\\s+", " ");
		sua_tk_tonkho = commons.getParameterFromRequest(req, "sua_tk_tonkho").trim().replaceAll("\\s+", " ");
		cach_tinh_gia_ton = commons.getParameterFromRequest(req, "cach_tinh_gia_ton").trim().replaceAll("\\s+", " ");
		cach_tinh_gia_ton = "1";
		tk_cl_vt = commons.getParameterFromRequest(req, "tk_cl_vt").trim().replaceAll("\\s+", " ");
		tk_dtnb = commons.getParameterFromRequest(req, "tk_dtnb").trim().replaceAll("\\s+", " ");
		
		if("dmproduct-edit".equals(transaction)) {
			if("".equals(_id)) {
				dto.setErrorCode(1);
				dto.getErrorMessages().add("Không tìm thấy thông tin sản phẩm.");
			}
		}
		
		switch (transaction) {
		case "dmproduct-cre":
		case "dmproduct-edit":
			if("".equals(code)) {
				dto.setErrorCode(1);
				dto.getErrorMessages().add("Vui lòng nhập vào mã sản phẩm.");
			}else if(!commons.checkStringWithRegex(Constants.REGEX_CHECK.STRING_CODE, code)) {
				dto.setErrorCode(1);
				dto.getErrorMessages().add("Mã sản phẩm không đúng định dạng.");
			}
			
			if("".equals(name)) {
				dto.setErrorCode(1);
				dto.getErrorMessages().add("Vui lòng nhập vào tên sản phẩm.");
			}
//			if("".equals(stock)) {
//				dto.setErrorCode(1);
//				dto.getErrorMessages().add("Vui lòng chọn kho hàng.");
//			}
			if("".equals(unit)) {
				dto.setErrorCode(1);
				dto.getErrorMessages().add("Vui lòng nhập vào đơn vị tính.");
			}
			if(commons.ToNumber(price) < 0) {
				dto.setErrorCode(1);
				dto.getErrorMessages().add("Vui lòng nhập đơn giá lớn hơn 0.");
			}
			if("".equals(vatRate) || Constants.MAP_VAT.get(vatRate) == null) {
				dto.setErrorCode(1);
				dto.getErrorMessages().add("Vui lòng kiểm tra lại thuế VAT.");
			}
			break;
		case "dmproduct-del":
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
		String messageConfirm = "Bạn có muốn thêm mới sản phẩm không?";
		switch (transaction) {
		case "dmproduct-cre":
			messageConfirm = "Bạn có muốn thêm mới sản phẩm không?";
			break;
		case "dmproduct-edit":
			messageConfirm = "Bạn có muốn thay đổi thông tin sản phẩm không?";
			break;
		case "dmproduct-del":
			messageConfirm = "Bạn có muốn xóa danh sách sản phẩm không?";
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
		
		if("dmproduct-del".equals(transaction)) {
			if(null == ids || ids.size() == 0) {
				dto.setErrorCode(999);
				dto.setResponseData("Không tìm thấy danh sách sản phẩm cần xóa.");
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
		
		if("dmproduct-del".equals(transaction)) {
			if(null == ids || ids.size() == 0) {
				dtoRes.setErrorCode(999);
				dtoRes.setResponseData("Không tìm thấy danh sách sản phẩm cần xóa.");
				return dtoRes;
			}
		}
		
		String actionCode = Constants.MSG_ACTION_CODE.CREATED;
		switch (transaction) {
		case "dmproduct-cre": actionCode = Constants.MSG_ACTION_CODE.CREATED; break;
		case "dmproduct-edit": actionCode = Constants.MSG_ACTION_CODE.MODIFY; break;
		case "dmproduct-del": actionCode = Constants.MSG_ACTION_CODE.DELETE; break;
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
		case "dmproduct-del":
			hData.put("ids", ids);
			break;
		default:
			hData.put("_id", _id);
			hData.put("code", code);
			hData.put("name", name);
			hData.put("stock", stock);
			hData.put("unit", unit);
			hData.put("price", price);
			hData.put("vatRate", vatRate);
			hData.put("description", description);
			hData.put("thdoiTonkho", thdoiTonkho);
			hData.put("remark", remark);
			
			hData.put("tkvt", tkvt);
			hData.put("tkgv", tkgv);
			hData.put("tkdt", tkdt);
			hData.put("loaivt", loaivt);
			hData.put("nh_vt1", nh_vt1);
			hData.put("nh_vt2", nh_vt2);
			hData.put("nh_vt3", nh_vt3);
			hData.put("sua_tk_tonkho", sua_tk_tonkho);
			hData.put("cach_tinh_gia_ton", cach_tinh_gia_ton);
			hData.put("tk_cl_vt", tk_cl_vt);
			hData.put("tk_dtnb", tk_dtnb);
			break;
		}
		
		msg.setObjData(hData);
		JSONRoot root = new JSONRoot(msg);
		MsgRsp rsp = restAPI.callAPINormal("/dmproduct/crud", cup.getLoginRes().getToken(), HttpMethod.POST, root);
		MspResponseStatus rspStatus = rsp.getResponseStatus();
		if(rspStatus.getErrorCode() == 0) {
			dtoRes.setErrorCode(0);
			switch (transaction) {
			case "dmproduct-cre":
				dtoRes.setResponseData("Thêm mới thông tin sản phẩm thành công.");
				break;
			case "dmproduct-edit":
				dtoRes.setResponseData("Cập nhật thông tin sản phẩm thành công.");
				break;
			case "dmproduct-del":
				dtoRes.setResponseData("Xóa danh sách sản phẩm thành công.");
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
	
}
