package vn.sesgroup.hddt.controller.tncn;

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
	"/qlnvtncn-cre",
	"/qlnvtncn-detail",
	"/qlnvtncn-edit",
	"/qlnvtncn-del"
})
@Scope(value = WebApplicationContext.SCOPE_REQUEST)
public class QLNhanVienTNCNCRUDController extends AbstractController{
	private static final Logger log = LogManager.getLogger(QLNhanVienTNCNCRUDController.class);
	@Autowired RestAPIUtility restAPI;
	
	private String errorCode;
	private String errorDesc;
	private String _id;
	private String taxCode;
	private String code;
	private String name;
	private String address;
	private String phone;
	private String department;
	private String cccd;
	private String cccddate;
	private String cccdaddress;
	private String qt;
	private String cutru;
	private String _token;
	private List<String> ids = null;
	

	@RequestMapping(value = "/init", method = {RequestMethod.POST})
	public String init(Locale locale, HttpServletRequest req, HttpSession session
			, @RequestAttribute(name = "transaction", value = "", required = false) String transaction) throws Exception{
		errorCode = "";
		errorDesc = "";
		
		_id = commons.getParameterFromRequest(req, "_id");
		CurrentUserProfile cup = getCurrentlyAuthenticatedPrincipal();
		
		String header = "Thêm mới nhân viên";
		String action = "CREATE";
		boolean isEdit = false;
		
		switch (transaction) {
		case "qlnvtncn-cre":
			header = "Thêm mới nhân viên";
			action = "CREATE";
			req.setAttribute("QuocTich", "VIỆT NAM");
			isEdit = true;
			break;
		case "qlnvtncn-detail":
			header = "Chi tiết thông tin nhân viên";
			action = "DETAIL";
			isEdit = false;
			break;
		case "qlnvtncn-edit":
			header = "Thay đổi thông tin nhân viên";
			action = "EDIT";
			isEdit = true;
			break;
		default:
			break;
		}
		
		if("|qlnvtncn-detail|qlnvtncn-edit|".indexOf(transaction) != -1) {
			
			inquiry(cup, locale, req, session, _id, action);
		}
	
		req.setAttribute("_header_", header);
		req.setAttribute("_action_", action);
		req.setAttribute("_id", _id);
		req.setAttribute("_isedit_", isEdit);
		
		return "tncn/qlnvtncn-crud";
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
		MsgRsp rsp = restAPI.callAPINormal("/qlnvtncn/detail/" + _id, cup.getLoginRes().getToken(), HttpMethod.POST, root);
		MspResponseStatus rspStatus = rsp.getResponseStatus();
		if(rspStatus.getErrorCode() == 0) {
			JsonNode jsonData = Json.serializer().nodeFromObject(rsp.getObjData());
			
			req.setAttribute("TaxCode", commons.getTextJsonNode(jsonData.at("/TaxCode")));
			req.setAttribute("Code", commons.getTextJsonNode(jsonData.at("/Code")));
			req.setAttribute("Name", commons.getTextJsonNode(jsonData.at("/Name")));
			req.setAttribute("Phone", commons.getTextJsonNode(jsonData.at("/Phone")));
			req.setAttribute("Address", commons.getTextJsonNode(jsonData.at("/Address")));
			req.setAttribute("CCCD", commons.getTextJsonNode(jsonData.at("/CMND-CCCD/CCCD")));
			req.setAttribute("CCCDDATE", commons.getTextJsonNode(jsonData.at("/CMND-CCCD/CCCDDATE")));
			req.setAttribute("CCCDADDRESS", commons.getTextJsonNode(jsonData.at("/CMND-CCCD/CCCDADDRESS")));
			req.setAttribute("QuocTich", commons.getTextJsonNode(jsonData.at("/CMND-CCCD/QuocTich")));
			req.setAttribute("QuocTich", commons.getTextJsonNode(jsonData.at("/CMND-CCCD/QuocTich")));
			req.setAttribute("optHTHDon", commons.getTextJsonNode(jsonData.at("/CuTru")));
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
		taxCode = commons.getParameterFromRequest(req, "tax-code").replaceAll("\\s", "").toUpperCase();
		code = commons.getParameterFromRequest(req, "code").replaceAll("\\s", "").toUpperCase();
		name = commons.getParameterFromRequest(req, "name");
		address = commons.getParameterFromRequest(req, "address").trim().replaceAll("\\s+", " ");
		department = commons.getParameterFromRequest(req, "department").trim().replaceAll("\\s+", " ");
		phone = commons.getParameterFromRequest(req, "phone").trim().replaceAll("\\s+", " ");
		cccd = commons.getParameterFromRequest(req, "cccd").trim().replaceAll("\\s+", " ");
		cccddate = commons.getParameterFromRequest(req, "cccddate").trim().replaceAll("\\s+", " ");
		cccdaddress = commons.getParameterFromRequest(req, "cccdaddress").trim().replaceAll("\\s+", " ");
		qt = commons.getParameterFromRequest(req, "qt").trim().replaceAll("\\s+", " ");
		cutru = commons.getParameterFromRequest(req, "cutru").trim().replaceAll("\\s+", " ");
		
		if("qlnvtncn-edit".equals(transaction)) {
			if("".equals(_id)) {
				dto.setErrorCode(1);
				dto.getErrorMessages().add("Không tìm thấy thông tin nhân viên.");
			}
		}
		switch (transaction) {
		case "qlnvtncn-cre":
		case "qlnvtncn-edit":
		
			if("".equals(name)) {
				dto.setErrorCode(1);
				dto.getErrorMessages().add("Vui lòng điền đầy đủ thông tin CMND-CCCD.");
			}	
			if("".equals(cccd)) {
				dto.setErrorCode(1);
				dto.getErrorMessages().add("Vui lòng điền đầy đủ thông tin CMND-CCCD.");
			}	
			if("".equals(cccddate)) {
				dto.setErrorCode(1);
				dto.getErrorMessages().add("Vui lòng điền đầy đủ thông tin CMND-CCCD.");
			}	
			if("".equals(cccdaddress)) {
				dto.setErrorCode(1);
				dto.getErrorMessages().add("Vui lòng điền đầy đủ thông tin CMND-CCCD.");
			}	
			if("".equals(address)) {
				dto.setErrorCode(1);
				dto.getErrorMessages().add("Vui lòng nhập vào địa chỉ nhân viên.");
			}	
			break;
		case "qlnvtncn-del":
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
		String messageConfirm = "Bạn có muốn thêm mới nhân viên không?";
		switch (transaction) {
		case "qlnvtncn-cre":
			messageConfirm = "Bạn có muốn thêm mới nhân viên không?";
			break;
		case "qlnvtncn-edit":
			messageConfirm = "Bạn có muốn thay đổi thông tin nhân viên không?";
			break;
		case "qlnvtncn-del":
			messageConfirm = "Bạn có muốn xóa danh sách nhân viên không?";
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
		
		if("qlnvtncn-del".equals(transaction)) {
			if(null == ids || ids.size() == 0) {
				dto.setErrorCode(999);
				dto.setResponseData("Không tìm thấy danh sách nhân viên cần xóa.");
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
		case "qlnvtncn-cre": actionCode = Constants.MSG_ACTION_CODE.CREATED; break;
		case "qlnvtncn-edit": actionCode = Constants.MSG_ACTION_CODE.MODIFY; break;
		case "qlnvtncn-del": actionCode = Constants.MSG_ACTION_CODE.DELETE; break;
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
		case "qlnvtncn-del":
			hData.put("ids", ids);
			break;
		default:
			hData.put("_id", _id);
			hData.put("TaxCode", taxCode);
			hData.put("Code", code);
			hData.put("Name", name);
			hData.put("Address", address);
			hData.put("Phone", phone);
			hData.put("Department", department);
			hData.put("CCCD", cccd);
			hData.put("CCCDDTE", cccddate);
			hData.put("CCCDADDRESS", cccdaddress);
			hData.put("QuocTich", qt);
			hData.put("CuTru", cutru);
			break;
		}
		
		msg.setObjData(hData);
		JSONRoot root = new JSONRoot(msg);
		MsgRsp rsp = restAPI.callAPINormal("/qlnvtncn/crud", cup.getLoginRes().getToken(), HttpMethod.POST, root);
		MspResponseStatus rspStatus = rsp.getResponseStatus();
		if(rspStatus.getErrorCode() == 0) {
			JsonNode jsonData = Json.serializer().nodeFromObject(rsp.getObjData());
			HashMap<String, Object> hR = new HashMap<String, Object>();
			
			dtoRes.setErrorCode(0);
			switch (transaction) {
			case "qlnvtncn-cre":
				hR.put("info", "Thêm mới thông tin nhân viên thành công.");
				hR.put("Code", commons.getTextJsonNode(jsonData.at("/Code")));
				dtoRes.setResponseData(hR);
				break;
			case "qlnvtncn-edit":
				dtoRes.setResponseData("Cập nhật thông tin nhân viên thành công.");
				break;
			case "qlnvtncn-del":
				dtoRes.setResponseData("Xóa danh sách nhân viên thành công.");
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

	
	
	
	
	
	@RequestMapping(value = "/save-nv",  produces = MediaType.APPLICATION_JSON_VALUE, method = RequestMethod.POST)
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
		taxCode = commons.getParameterFromRequest(req, "taxcode").replaceAll("\\s", "").toUpperCase();
		code = commons.getParameterFromRequest(req, "code").replaceAll("\\s", "").toUpperCase();
		name = commons.getParameterFromRequest(req, "name");
		address = commons.getParameterFromRequest(req, "address").trim().replaceAll("\\s+", " ");
		department = commons.getParameterFromRequest(req, "department").trim().replaceAll("\\s+", " ");
		phone = commons.getParameterFromRequest(req, "phone").trim().replaceAll("\\s+", " ");
		cccd = commons.getParameterFromRequest(req, "cccd").trim().replaceAll("\\s+", " ");
		cccddate = commons.getParameterFromRequest(req, "cccddate").trim().replaceAll("\\s+", " ");
		cccdaddress = commons.getParameterFromRequest(req, "cccdaddress").trim().replaceAll("\\s+", " ");
		qt = commons.getParameterFromRequest(req, "qt").trim().replaceAll("\\s+", " ");
		cutru = commons.getParameterFromRequest(req, "cutru").trim().replaceAll("\\s+", " ");


		BaseDTO baseDTO = new BaseDTO(req);
		Msg msg = baseDTO.createMsg(cup,  Constants.MSG_ACTION_CODE.CREATED);
		HashMap<String, String> hData = new HashMap<>();
		hData.put("TaxCode", taxCode);
		hData.put("Code", code);
		hData.put("Name", name);
		hData.put("Address", address);
		hData.put("Phone", phone);
		hData.put("Department", department);
		hData.put("CCCD", cccd);
		hData.put("CCCDDTE", cccddate);
		hData.put("CCCDADDRESS", cccdaddress);
		hData.put("QuocTich", qt);
		hData.put("CuTru", cutru);
		msg.setObjData(hData);
		JSONRoot root = new JSONRoot(msg);
		MsgRsp rsp = restAPI.callAPINormal("/qlnvtncn/crud", cup.getLoginRes().getToken(), HttpMethod.POST, root);
		MspResponseStatus rspStatus = rsp.getResponseStatus();
		if(rspStatus.getErrorCode() == 0) {					
			dto.setErrorCode(0);	
			return dto;
		}
		
		dto.setErrorCode(999);
		dto.setResponseData(rsp.getResponseStatus().getErrorDesc());
		return dto;
	}	
}
