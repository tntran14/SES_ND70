package vn.sesgroup.hddt.controller.tncn;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
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
import org.springframework.web.client.RestTemplate;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;

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
import vn.sesgroup.hddt.dto.LoginRes;
import vn.sesgroup.hddt.resources.RestAPIUtility;
import vn.sesgroup.hddt.utils.Constants;
import vn.sesgroup.hddt.utils.Json;
import vn.sesgroup.hddt.utils.SystemParams;

@Controller
@RequestMapping({
	"/mstncn-cre"
	, "/mstncn-edit"
	, "/mstncn-detail"
	
})
@Scope(value = WebApplicationContext.SCOPE_REQUEST)
public class MauSoTNCNCRUDController extends AbstractController{
	private static final Logger log = LogManager.getLogger(MauSoTNCNCRUDController.class);
	@Autowired RestAPIUtility restAPI;
	@Autowired RestTemplate restTemplate;

	private String errorCode;
	private String errorDesc;
	private String _id;
	private String	khhd ; 
	private String	macqt ;
	private String	yearCreated ;
	private String macty;
	private String sl;
	private String phoict;
	private String phoicttext;
	private String logoFileNameSystem;


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
			msgParam.setParam("LoaiHD");
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
					req.setAttribute("map_loaihd", hItem);
				}
				
				
			}
		}catch(Exception e) {}
	}
	
	
	
	
	
	
	@RequestMapping(value = "/viewimg", method = {RequestMethod.POST})
	public String viewimg(Locale locale, HttpServletRequest req, HttpSession session
			, @RequestAttribute(name = "transaction", value = "", required = false) String transaction, String action) throws Exception{
		CurrentUserProfile cup = getCurrentlyAuthenticatedPrincipal();
		String phoi = null == req.getParameter("phoict")? "": req.getParameter("phoict");

		req.setAttribute("_header_", "Xem phôi chứng từ");
		
		JSONRoot root = new JSONRoot();
		MsgRsp rsp = restAPI.callAPINormal("/mstncn/viewimg/" + phoi, cup.getLoginRes().getToken(), HttpMethod.POST, root);
		 Object anh = rsp.getObjData();
		req.setAttribute("invoiceTemplate", anh);
		return "tncn/viewimg";
	}

	@RequestMapping(value = "/init", method = {RequestMethod.POST})
	public String init(Locale locale, HttpServletRequest req, HttpSession session
			, @RequestAttribute(name = "transaction", value = "", required = false) String transaction
			, @RequestAttribute(name = "method", value = "", required = false) String method
			) throws Exception{
		errorCode = "";
		errorDesc = "";
		CurrentUserProfile cup = getCurrentlyAuthenticatedPrincipal();
		_id = commons.getParameterFromRequest(req, "_id");
		String header = "Thêm mới mẫu hóa đơn";
		String action = "CREATE";
		boolean isEdit = false;
		
		
		switch (transaction) {
		case "mstncn-cre":
			header = "Thêm mới mẫu hóa đơn";
			action = "CREATE";
			isEdit = true;
		
			
		
			break;
		case "mstncn-edit":
			header = "Thay đổi thông tin mẫu hóa đơn";
			action = "EDIT";
			isEdit = true;
			break;
		case "mstncn-detail":
			header = "Chi tiết mẫu hóa đơn";
			action = "DETAIL";
			isEdit = false;
			break;
		

		default:
			break;
		}
		
		if("|mstncn-edit|mstncn-detail".indexOf(transaction) != -1)
			inquiry(cup, locale, req, session, action);
			req.setAttribute("_header_", header);
			req.setAttribute("_action_", action);
			req.setAttribute("_isedit_", isEdit);
			req.setAttribute("_id", _id);
			LoadParameter(cup, locale, req, action);
			if(!"".equals(errorDesc))
				req.setAttribute("messageError", errorDesc);
			
		return "tncn/mstncn-crud";
	}
	private void inquiry(CurrentUserProfile cup, Locale locale, HttpServletRequest req, HttpSession session, String action) throws Exception{
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
		MsgRsp rsp = restAPI.callAPINormal("/mstncn/detail/" + _id, cup.getLoginRes().getToken(), HttpMethod.POST, root);
		MspResponseStatus rspStatus = rsp.getResponseStatus();
		if(rspStatus.getErrorCode() == 0) {
			JsonNode jsonData = Json.serializer().nodeFromObject(rsp.getObjData());
		
			req.setAttribute("KyHieu", commons.getTextJsonNode(jsonData.at("/KyHieu")));
			req.setAttribute("SoLuong", commons.getTextJsonNode(jsonData.at("/SoLuong")));
			req.setAttribute("LoGo", commons.getTextJsonNode(jsonData.at("/LoGo")));
		
			}else {
			errorDesc = rspStatus.getErrorDesc();
		}
	}
	
	
	public BaseDTO checkDataToAccept(HttpServletRequest req, HttpSession session, String transaction
			, CurrentUserProfile cup) throws Exception{
		BaseDTO dto = new BaseDTO();
		dto.setErrorCode(0);	
		_id = commons.getParameterFromRequest(req, "_id").replaceAll("\\s", "");
		khhd = commons.getParameterFromRequest(req, "khhd");
		macqt = commons.getParameterFromRequest(req, "macqt");
		sl = commons.getParameterFromRequest(req, "sl");
		yearCreated = commons.getParameterFromRequest(req, "yearCreated");
		macty = commons.getParameterFromRequest(req, "macty");
		logoFileNameSystem = commons.getParameterFromRequest(req, "logoFileNameSystem");
		phoict = commons.getParameterFromRequest(req, "phoict");
		phoicttext = commons.getParameterFromRequest(req, "phoict-text");
			switch (transaction) {
			case "mstncn-cre":
			case "mstncn-edit":			
				if("".equals(macty)) {
					dto.setErrorCode(1);
					dto.getErrorMessages().add("Kí hiệu không được để trống.");
				}
				if("".equals(yearCreated)) {
					dto.setErrorCode(1);
					dto.getErrorMessages().add("Năm chưa xác định. Vui lòng load lại trang.");
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
		String messageConfirm = "Bạn có muốn mẫu hóa đơn?";
		switch (transaction) {
		case "mstncn-cre":
			messageConfirm = "Bạn có muốn thêm mới mẫu hóa đơn không?";
			break;
		case "mstncn-edit":
			messageConfirm = "Bạn có muốn thay đổi thông tin mẫu hóa đơn không?";
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

		String actionCode = Constants.MSG_ACTION_CODE.CREATED;
		switch (transaction) {
		case "mstncn-cre": actionCode = Constants.MSG_ACTION_CODE.CREATED; break;
		case "mstncn-edit": actionCode = Constants.MSG_ACTION_CODE.MODIFY; break;
		default:
			dtoRes = new BaseDTO();
			dtoRes.setErrorCode(998);
			dtoRes.setResponseData(Constants.MAP_ERROR.get(998));
			return dtoRes;
		}
		
		dtoRes = new BaseDTO(req);
		Msg msg = dtoRes.createMsg(cup, actionCode);
		HashMap<String, Object> hData = new HashMap<>();
		msg.setObjData(hData);
		JSONRoot root = new JSONRoot(msg);
		hData.put("_id", _id);
		hData.put("KiHieuHD", khhd);
		hData.put("MaCQT", macqt);
		hData.put("SOLUONG", sl);
		hData.put("PhoiCT", phoict);
		hData.put("PhoiCTText", phoicttext);
		hData.put("NamPhatHanh", yearCreated);
		hData.put("MaCty", macty);
		hData.put("Logo", logoFileNameSystem);
		MsgRsp rsp = restAPI.callAPINormal("/mstncn/crud", cup.getLoginRes().getToken(), HttpMethod.POST, root);
		MspResponseStatus rspStatus = rsp.getResponseStatus();
		if(rspStatus.getErrorCode() == 0) {
			dtoRes.setErrorCode(0);
			switch (transaction) {
			case "mstncn-cre":
				dtoRes.setResponseData("Thêm mới thông tin khách hàng thành công.");
				break;
			case "mstncn-edit":
				dtoRes.setResponseData("Cập nhật thông tin khách hàng thành công.");
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
	

	
	
	
	
	@RequestMapping(value = "/processUploadFile", produces = MediaType.APPLICATION_JSON_VALUE,
			method = RequestMethod.POST, consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	@ResponseBody
	public BaseDTO execProcessUploadFile(Locale locale, HttpServletRequest req, 
			HttpSession session, MultipartHttpServletRequest multipartHttpServletRequest) {
		BaseDTO dto = new BaseDTO();
		CurrentUserProfile cup = getCurrentlyAuthenticatedPrincipal();
		LoginRes a = cup.getLoginRes();
		String DIR_TEMPORARY_STORE_FILES = "C:/hddt-ses/server/template/MauSoTNCN/" + a.getUserName()+ "/";
		try {
			File file = new File(DIR_TEMPORARY_STORE_FILES);
			if(!file.exists())
				file.mkdirs();
			
			Iterator<String> itrator = multipartHttpServletRequest.getFileNames();
			MultipartFile multiFile = null;
			StringBuilder keyFileName = new StringBuilder();
			
			String fileNameOriginal = "";
			String fileName = "";
			if(itrator.hasNext()) {
				keyFileName.setLength(0);
				keyFileName.append(itrator.next());
				multiFile = multipartHttpServletRequest.getFile(keyFileName.toString());
				fileNameOriginal = multiFile.getOriginalFilename();
				if(multiFile.isEmpty()) {
					
				}else {
					double sizeFile = multiFile.getSize()/1024D;
					if(sizeFile > SystemParams.MAX_SIZE_IMAGE_LOGO_KB) {
						dto.setErrorCode(1);
						return dto;
					}
					
					fileName =fileNameOriginal;
					if(saveUploadFiles(multiFile, fileName, DIR_TEMPORARY_STORE_FILES )) {
						if("logoFile".equals(keyFileName.toString()))
							session.setAttribute("FILE_NAME_LOGO", fileName);
						else if("backgroundFile".equals(keyFileName.toString()))
								session.setAttribute("FILE_NAME_BACKGROUND", fileName);
					}
				}
			}
			dto.setErrorCode(0);
			HashMap<String, String> hResult = new HashMap<>();
			hResult.put("id", keyFileName.toString() + "Name");
			hResult.put("value", fileNameOriginal);
			hResult.put("valueSystem", fileName);
			dto.setResponseData(hResult);
		}catch(Exception e) {
			e.printStackTrace();
			dto.setErrorCode(1);
			return dto;
		}
		return dto;
	}

	@RequestMapping(value = "/deleteImage",  produces = MediaType.APPLICATION_JSON_VALUE, method = RequestMethod.POST)
	@ResponseBody
	public BaseDTO execDeleteImage(Locale locale, HttpServletRequest req, HttpSession session) {
		BaseDTO dto = new BaseDTO();
		try {
			String typeImage = null == req.getParameter("typeImage")? "": req.getParameter("typeImage");
			String fileName = "";
			switch (typeImage) {
			case "logoFile":
				fileName = null == session.getAttribute("FILE_NAME_LOGO")? "": session.getAttribute("FILE_NAME_LOGO").toString();
				session.removeAttribute("FILE_NAME_LOGO");
				break;
			case "backgroundFile":
				fileName = null == session.getAttribute("FILE_NAME_BACKGROUND")? "": session.getAttribute("FILE_NAME_BACKGROUND").toString();
				session.removeAttribute("FILE_NAME_BACKGROUND");
				break;
			default:
				break;
			}
			CurrentUserProfile cup = getCurrentlyAuthenticatedPrincipal();
			LoginRes a = cup.getLoginRes();
			String DIR_TEMPORARY_STORE_FILES = "C:/hddt-ses/server/template/MauSoTNCN/" + a.getUserName()+ "/";
			File file = new File(DIR_TEMPORARY_STORE_FILES, fileName);
			if(file.exists() && !file.isDirectory()){
				file.delete();
			}
			
			dto.setErrorCode(0);
		}catch(Exception e) {
			dto.setErrorCode(1);
			return dto;
		}
		return dto;
	}

	private boolean saveUploadFiles(MultipartFile multiFile, String fileNameTarget, String dir) {
		try {
			if(!multiFile.isEmpty()) {
				byte[] bytes = multiFile.getBytes();
	            Path path = Paths.get(dir + fileNameTarget);
	            Files.write(path, bytes);
	            return true;
			}
		}catch(Exception e) {
			e.printStackTrace();
		}
		return false;
	}
	
	
	
	
	
	

}
