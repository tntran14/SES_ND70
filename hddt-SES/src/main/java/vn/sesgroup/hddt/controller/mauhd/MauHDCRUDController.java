package vn.sesgroup.hddt.controller.mauhd;

import java.io.File;
import java.io.InputStream;
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
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;

import vn.sesgroup.hddt.controller.AbstractController;
import vn.sesgroup.hddt.dto.BaseDTO;
import vn.sesgroup.hddt.dto.CurrentUserProfile;
import vn.sesgroup.hddt.dto.FileInfo;
import vn.sesgroup.hddt.dto.LoginRes;
import vn.sesgroup.hddt.resources.RestAPIUtility;
import vn.sesgroup.hddt.utils.Constants;
import vn.sesgroup.hddt.utils.Json;
import vn.sesgroup.hddt.utils.SystemParams;

@Controller
@RequestMapping({
	"/mauhd-cre"
	, "/mauhd-edit"
	, "/mauhd-detail"
})
@Scope(value = WebApplicationContext.SCOPE_REQUEST)
public class MauHDCRUDController extends AbstractController{
	private static final Logger log = LogManager.getLogger(MauHDCRUDController.class);
	@Autowired RestAPIUtility restAPI;
	@Autowired RestTemplate restTemplate;

	private String errorCode;
	private String errorDesc;
	private String _id;
	private String	loaihd ; 
	private String	phoi ; 
	private String	phoitest ;
	private String	khhd ; 
	private String	macqt ;
	private String	yearCreated ;
	private String	kh ; 
	private String macty;
	private String r1p;
	private String rnp;
	private String CharsInRow;
	private String logoFileNameSystem;
	private String backgroundFileNameSystem;
	private String qaFileNameSystem;
	private String vienFileNameSystem;

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
			
			msgParam = new MsgParam();
			msgParam.setId("param02");
			msgParam.setParam("DMTemplates");			
			conds = new ArrayList<>();
			hashConds = new HashMap<>();
			hashConds.put("cond", "loaihd_ma");
			hashConds.put("condval", null == loaihd? "": loaihd);
			conds.add(hashConds);
			msgParam.setConds(conds);			
			msgParams.getParams().add(msgParam);
			
			
			
			msgParam = new MsgParam();
			msgParam.setId("param03");
			msgParam.setParam("KyHieuThongTu");
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
				if(null != jsonData.at("/param02") && jsonData.at("/param02") instanceof ArrayNode) {
					hItem = new LinkedHashMap<String, String>();
					for(JsonNode o: jsonData.at("/param02")) {
						hItem.put(commons.getTextJsonNode(o.get("Images")), commons.getTextJsonNode(o.get("Name")));
					}
					req.setAttribute("map_phoi", hItem);
				}
				if(null != jsonData.at("/param03") && jsonData.at("/param03") instanceof ArrayNode) {
					hItem = new LinkedHashMap<String, String>();
					for(JsonNode o: jsonData.at("/param03")) {
						hItem.put(commons.getTextJsonNode(o.get("code")), commons.getTextJsonNode(o.get("name")));
					}
					req.setAttribute("map_kh", hItem);
				}
				
			}
		}catch(Exception e) {}
	}
	
	@RequestMapping(value = "/viewimg", method = {RequestMethod.POST})
	public String viewimg(Locale locale, HttpServletRequest req, HttpSession session
			, @RequestAttribute(name = "transaction", value = "", required = false) String transaction, String action) throws Exception{
		CurrentUserProfile cup = getCurrentlyAuthenticatedPrincipal();
		String phoi = null == req.getParameter("phoi")? "": req.getParameter("phoi");

		req.setAttribute("_header_", "Xem phôi hóa đơn");
		
		JSONRoot root = new JSONRoot();
		MsgRsp rsp = restAPI.callAPINormal("/mauhd/viewimg/" + phoi, cup.getLoginRes().getToken(), HttpMethod.POST, root);
		 Object anh = rsp.getObjData();
		req.setAttribute("invoiceTemplate", anh);
		LoadParameter(cup, locale, req, action);
		return "mauhd/viewimg";
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
		case "mauhd-cre":
			header = "Thêm mới mẫu hóa đơn";
			action = "CREATE";
			isEdit = true;
			req.setAttribute("MaCTY", "YY");
			req.setAttribute("RowsInPage", "12");
			req.setAttribute("RowInPageMultiPage", "15");
			req.setAttribute("CharsInRow", "50");
		
			break;
		case "mauhd-edit":
			header = "Thay đổi thông tin mẫu hóa đơn";
			action = "EDIT";
			isEdit = true;
			break;
		case "mauhd-detail":
			header = "Chi tiết mẫu hóa đơn";
			action = "DETAIL";
			isEdit = false;
			break;
		

		default:
			break;
		}
		
		if("|mauhd-edit|mauhd-detail".indexOf(transaction) != -1)
			inquiry(cup, locale, req, session, action);
			req.setAttribute("_header_", header);
			req.setAttribute("_action_", action);
			req.setAttribute("_isedit_", isEdit);
			req.setAttribute("_id", _id);
			LoadParameter(cup, locale, req, action);
		
			if(!"".equals(errorDesc))
				req.setAttribute("messageError", errorDesc);
		return "mauhd/mauhd-crud";
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
		MsgRsp rsp = restAPI.callAPINormal("/mauhd/detail/" + _id, cup.getLoginRes().getToken(), HttpMethod.POST, root);
		MspResponseStatus rspStatus = rsp.getResponseStatus();
		if(rspStatus.getErrorCode() == 0) {
			JsonNode jsonData = Json.serializer().nodeFromObject(rsp.getObjData());
			String str = commons.getTextJsonNode(jsonData.at("/KHHDon"));
			char id = str.charAt(1);			
			char id1 = str.charAt(2);
			char index1 = str.charAt(3);			
			char index2 = str.charAt(4);
			char index3 = str.charAt(5);
			String macty = Character.toString(index2) + Character.toString(index3);
			String kh = Character.toString(index1);
			
			
			String nam = Character.toString(id) + Character.toString(id1);
			req.setAttribute("LoaiHD", commons.getTextJsonNode(jsonData.at("/KHMSHDon")));
			req.setAttribute("KH",kh);
		
			req.setAttribute("PhoiHD",commons.getTextJsonNode(jsonData.at("/Templates/Name")));
			req.setAttribute("PhoiHDCode",commons.getTextJsonNode(jsonData.at("/Templates/Images")));
			req.setAttribute("MaCTY",macty);
			req.setAttribute("Nam",nam);
			req.setAttribute("Logo", commons.getTextJsonNode(jsonData.at("/Templates/ImgLogo")));
			req.setAttribute("Bg", commons.getTextJsonNode(jsonData.at("/Templates/ImgBackground")));
			req.setAttribute("QA", commons.getTextJsonNode(jsonData.at("/Templates/ImgQA")));
			req.setAttribute("Vien", commons.getTextJsonNode(jsonData.at("/Templates/ImgVien")));
			req.setAttribute("RowsInPage", commons.getTextJsonNode(jsonData.at("/Templates/RowsInPage")));
			req.setAttribute("RowInPageMultiPage", commons.getTextJsonNode(jsonData.at("/Templates/RowInPageMultiPage")));
			req.setAttribute("CharsInRow", commons.getTextJsonNode(jsonData.at("/Templates/CharsInRow")));

			}else {
			errorDesc = rspStatus.getErrorDesc();
		}
	}
	
	
	public BaseDTO checkDataToAccept(HttpServletRequest req, HttpSession session, String transaction
			, CurrentUserProfile cup) throws Exception{
		BaseDTO dto = new BaseDTO();
		dto.setErrorCode(0);	
		_id = commons.getParameterFromRequest(req, "_id").replaceAll("\\s", "");
		loaihd = commons.getParameterFromRequest(req, "loaihd");
		phoi = commons.getParameterFromRequest(req, "phoi");
		phoitest = commons.getParameterFromRequest(req, "phoi-text");
		khhd = commons.getParameterFromRequest(req, "khhd");
		macqt = commons.getParameterFromRequest(req, "macqt");
		r1p = commons.getParameterFromRequest(req, "r1p");
		rnp = commons.getParameterFromRequest(req, "rnp");
		CharsInRow = commons.getParameterFromRequest(req, "CharsInRow");
		
		yearCreated = commons.getParameterFromRequest(req, "yearCreated");
		kh = commons.getParameterFromRequest(req, "kh");	
		macty = commons.getParameterFromRequest(req, "macty");
		logoFileNameSystem = commons.getParameterFromRequest(req, "logoFileNameSystem");
		backgroundFileNameSystem = commons.getParameterFromRequest(req, "backgroundFileNameSystem");
		qaFileNameSystem = commons.getParameterFromRequest(req, "qaFileNameSystem");
		vienFileNameSystem = commons.getParameterFromRequest(req, "vienFileNameSystem");

			switch (transaction) {
			case "mauhd-cre":
			case "mauhd-edit":	
				if("".equals(loaihd)) {
					dto.setErrorCode(1);
					dto.getErrorMessages().add("Loại hóa đơn không được để trống.");
				}
				if("".equals(phoitest)) {
					dto.setErrorCode(1);
					dto.getErrorMessages().add("Phôi hóa đơn không được để trống.");
				}
				if("".equals(kh)) {
					dto.setErrorCode(1);
					dto.getErrorMessages().add("Kí hiệu không được để trống.");
				}
				if("".equals(macty)) {
					dto.setErrorCode(1);
					dto.getErrorMessages().add("Kí hiệu không được để trống.");
				}
				break;
//			case "mauhd-edit":	
//				if("".equals(kh)) {
//					dto.setErrorCode(1);
//					dto.getErrorMessages().add("Kí hiệu không được để trống.");
//				}
//				if("".equals(macty)) {
//					dto.setErrorCode(1);
//					dto.getErrorMessages().add("Kí hiệu Công Ty không được để trống.");
//				}
//				break;

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
		case "mauhd-cre":
			messageConfirm = "Bạn có muốn thêm mới mẫu hóa đơn không?";
			break;
		case "mauhd-edit":
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
		case "mauhd-cre": actionCode = Constants.MSG_ACTION_CODE.CREATED; break;
		case "mauhd-edit": actionCode = Constants.MSG_ACTION_CODE.MODIFY; break;
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
		hData.put("LoaiHD", loaihd);
		hData.put("Phoi", phoi);
		hData.put("PhoiTest", phoitest);
		hData.put("KiHieuHD", khhd);
		hData.put("MaCQT", macqt);
		hData.put("NamPhatHanh", yearCreated);
		hData.put("KiHieu", kh);
		hData.put("MaCty", macty);
		hData.put("RowsInPage", r1p);
		hData.put("RowInPageMultiPage", rnp);
		hData.put("CharsInRow", CharsInRow);
		hData.put("Logo", logoFileNameSystem);
		hData.put("Nen", backgroundFileNameSystem);
		hData.put("QA", qaFileNameSystem);
		hData.put("Vien", vienFileNameSystem);
		MsgRsp rsp = restAPI.callAPINormal("/mauhd/crud", cup.getLoginRes().getToken(), HttpMethod.POST, root);
		MspResponseStatus rspStatus = rsp.getResponseStatus();
		if(rspStatus.getErrorCode() == 0) {
			dtoRes.setErrorCode(0);
			switch (transaction) {
			case "mauhd-cre":
				dtoRes.setResponseData("Thêm mới thông tin khách hàng thành công.");
				break;
			case "mauhd-edit":
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
			HttpSession session, MultipartHttpServletRequest multipartHttpServletRequest) throws Exception {
		BaseDTO dto = new BaseDTO();
		CurrentUserProfile cup = getCurrentlyAuthenticatedPrincipal();
	
		String a = cup.getLoginRes().getIssuerId();
		 String s1 = "{\"IssuerId\":\"";	
		 String s2 = "\"}";
		String s3 = s1+a+s2;
		
		///
		/*LAY THONG TIN DU LIEU XML VE SERVER WEB*/
		HashMap<String, String> hInput = null;
		try {
			hInput = Json.serializer().fromJson((s3), new TypeReference<HashMap<String, String>>() {});
				
//			hInput = a, new TypeReference<HashMap<String, String>>() {});
		}catch(Exception e) {
			log.error(">>>>> An exception occurred!", e);
		}
		
		FileInfo fileInfo = restAPI.callAPIGetTaxCode("/taxcode/get-taxcode", HttpMethod.POST, hInput);
		if(null == fileInfo || null == fileInfo.getTaxcode()) {
			dto.setErrorCode(999);
			dto.setResponseData("Không tìm thấy dữ liệu hóa đơn.");
			return dto;
		}
				
		///
		String DIR_TEMPORARY_STORE_FILES = "C:/hddt-ses/server/template/images/"+fileInfo.getTaxcode() + "/";
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
						{
							session.setAttribute("FILE_NAME_LOGO", fileName);
						}
						else if("backgroundFile".equals(keyFileName.toString()))
						{
								session.setAttribute("FILE_NAME_BACKGROUND", fileName);
						}
						else if("qaFile".equals(keyFileName.toString()))
						{
								session.setAttribute("FILE_NAME_QA", fileName);
						}
						else if("vienFile".equals(keyFileName.toString()))
						{
								session.setAttribute("FILE_NAME_VIEN", fileName);
						}
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
			case "qaFile":
				fileName = null == session.getAttribute("FILE_NAME_QA")? "": session.getAttribute("FILE_NAME_QA").toString();
				session.removeAttribute("FILE_NAME_QA");
				break;
			case "vienFile":
				fileName = null == session.getAttribute("FILE_NAME_VIEN")? "": session.getAttribute("FILE_NAME_VIEN").toString();
				session.removeAttribute("FILE_NAME_VIEN");
				break;
			default:
				break;
			}
			CurrentUserProfile cup = getCurrentlyAuthenticatedPrincipal();
			String a = cup.getLoginRes().getIssuerId();
			 String s1 = "{\"IssuerId\":\"";	
			 String s2 = "\"}";
			String s3 = s1+a+s2;
			///
			/*LAY THONG TIN DU LIEU XML VE SERVER WEB*/
			HashMap<String, String> hInput = null;
			try {
				hInput = Json.serializer().fromJson((s3), new TypeReference<HashMap<String, String>>() {});
					
//				hInput = a, new TypeReference<HashMap<String, String>>() {});
			}catch(Exception e) {
				log.error(">>>>> An exception occurred!", e);
			}
			
			FileInfo fileInfo = restAPI.callAPIGetTaxCode("/taxcode/get-taxcode", HttpMethod.POST, hInput);
			if(null == fileInfo || null == fileInfo.getTaxcode()) {
				dto.setErrorCode(999);
				dto.setResponseData("Không tìm thấy dữ liệu hóa đơn.");
				return dto;
			}
			
			
			String DIR_TEMPORARY_STORE_FILES = "C:/hddt-ses/server/template/images/" + fileInfo.getTaxcode()+ "/";
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
