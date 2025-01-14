package vn.sesgroup.hddt.controller.qlyphoihd;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Locale;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
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
	"/qly-phoihd-cre"
	, "/qly-phoihd-detail"
	, "/qly-phoihd-delete"
	, "/qly-phoihd-edit"
	,"/qly-phoihd-view"
})
@Scope(value = WebApplicationContext.SCOPE_REQUEST)
public class QLPhoiHDonCRUDController extends AbstractController{
	private static final Logger log = LogManager.getLogger(QLPhoiHDonCRUDController.class);
	@Autowired RestAPIUtility restAPI;
	@Autowired RestTemplate restTemplate;
	@Autowired
	ServletContext app;
	
	private String errorCode;
	private String errorDesc;
	private String _id;
	private String	loaihd ; 
	private String	name ; 
	private String	FileNameSystem ; 
	private String	ImgFileNameSystem ;
	private String phanloai;
	private String dactinh;
	private String mota;
	private String ghichu;
	

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
		case "qly-phoihd-cre":
			header = "Thêm mới mẫu hóa đơn";
			action = "CREATE";
			isEdit = true;
			break;
		case "qly-phoihd-edit":
			header = "Thay đổi thông tin phôi hóa đơn";
			action = "EDIT";
			isEdit = true;
			break;
		case "qly-phoihd-detail":
			header = "Chi tiết phôi hóa đơn";
			action = "DETAIL";
			isEdit = false;
			break;
		

		default:
			break;
		}
		
		if("|qly-phoihd-edit|qly-phoihd-detail".indexOf(transaction) != -1)
			req.setAttribute("_header_", header);
			req.setAttribute("_action_", action);
			req.setAttribute("_isedit_", isEdit);
			req.setAttribute("_id", _id);
			LoadParameter(cup, locale, req, action);
			inquiry(cup, locale, req, session, action);
			if(!"".equals(errorDesc))
				req.setAttribute("messageError", errorDesc);
		return "ad_qlyphoihd/qly-phoihd-crud";
	}
	private void inquiry(CurrentUserProfile cup, Locale locale, HttpServletRequest req, HttpSession session, String action) throws Exception{
		if("".equals(_id)) {
			errorCode = "NOT FOUND";
			errorDesc = "Không tìm thấy thông tin mẫu hóa đơn.";
			return;
		}
		BaseDTO baseDTO = new BaseDTO(req);
		Msg msg = baseDTO.createMsg(cup, Constants.MSG_ACTION_CODE.INQUIRY);
		HashMap<String, String> hData = new HashMap<>();
		msg.setObjData(hData);
		JSONRoot root = new JSONRoot(msg);
		MsgRsp rsp = restAPI.callAPINormal("/phoihd/detail/" + _id, cup.getLoginRes().getToken(), HttpMethod.POST, root);
		MspResponseStatus rspStatus = rsp.getResponseStatus();
		if(rspStatus.getErrorCode() == 0) {
			JsonNode jsonData = Json.serializer().nodeFromObject(rsp.getObjData());
		
			req.setAttribute("LoaiHD", commons.getTextJsonNode(jsonData.at("/loaihd_ma")));
			req.setAttribute("Name", commons.getTextJsonNode(jsonData.at("/Name")));
			req.setAttribute("FileName", commons.getTextJsonNode(jsonData.at("/FileName")));
			req.setAttribute("Images", commons.getTextJsonNode(jsonData.at("/Images")));
			req.setAttribute("PhanLoai", commons.getTextJsonNode(jsonData.at("/PhanLoai")));
			req.setAttribute("DacTinh", commons.getTextJsonNode(jsonData.at("/DacTinh")));
			req.setAttribute("MoTa", commons.getTextJsonNode(jsonData.at("/MoTa")));
			req.setAttribute("GhiChu", commons.getTextJsonNode(jsonData.at("/GhiChu")));
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
		name = commons.getParameterFromRequest(req, "name");
		FileNameSystem = commons.getParameterFromRequest(req, "logoFileNameSystem");
		ImgFileNameSystem = commons.getParameterFromRequest(req, "backgroundFileNameSystem");
		phanloai = commons.getParameterFromRequest(req, "phan-loai");
		dactinh = commons.getParameterFromRequest(req, "dac-tinh-phoi");
		mota = commons.getParameterFromRequest(req, "mo-ta");
		ghichu = commons.getParameterFromRequest(req, "ghi-chu");

			switch (transaction) {
			case "qly-phoihd-cre":
//				if("".equals(loaihd)) {
//					dto.setErrorCode(1);
//					dto.getErrorMessages().add("Loại hóa đơn không được để trống.");
//				}
				if("".equals(FileNameSystem)) {
					dto.setErrorCode(1);
					dto.getErrorMessages().add("Mẫu hóa đơn không được để trống.");
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
		case "qly-phoihd-cre":
			messageConfirm = "Bạn có muốn thêm mới mẫu hóa đơn không?";
			break;
		case "qly-phoihd-edit":
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
		case "qly-phoihd-cre": actionCode = Constants.MSG_ACTION_CODE.CREATED; break;
		case "qly-phoihd-edit": actionCode = Constants.MSG_ACTION_CODE.MODIFY; break;
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
		hData.put("Name", name);
		hData.put("FileNameSystem", FileNameSystem);
		hData.put("ImgFileNameSystem", ImgFileNameSystem);
		hData.put("PhanLoai", phanloai);
		hData.put("DacTinh", dactinh);
		hData.put("MoTa", mota);
		hData.put("GhiChu", ghichu);
		
		MsgRsp rsp = restAPI.callAPINormal("/phoihd/crud", cup.getLoginRes().getToken(), HttpMethod.POST, root);
		MspResponseStatus rspStatus = rsp.getResponseStatus();
		if(rspStatus.getErrorCode() == 0) {
			dtoRes.setErrorCode(0);
			switch (transaction) {
			case "qly-phoihd-cre":
				dtoRes.setResponseData("Thêm mới thông tin khách hàng thành công.");
				break;
			case "qly-phoihd-edit":
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
		String DIR_TEMPORARY_STORE_FILES = "C:/hddt-ses/server/template/";
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
					
					String imageDirectory = System.getProperty("user.dir") + "/src/main/resources/static/images/mauhd/";
			
					if("backgroundFile".equals(keyFileName.toString()))
					 {
					makeDirectoryIfNotExist(imageDirectory);
					Path fileNamePath = Paths.get(imageDirectory, fileName);
					try {
						Files.write(fileNamePath, multiFile.getBytes());	
					
					} catch (Exception e) {
					}
					 }

					if(saveUploadFiles(multiFile, fileName, DIR_TEMPORARY_STORE_FILES )) {
						if("logoFile".equals(keyFileName.toString()))
							session.setAttribute("FILE_NAME_LOGO", fileName);
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

	private void makeDirectoryIfNotExist(String imageDirectory) {
		File directory = new File(imageDirectory);
		if (!directory.exists()) {
			directory.mkdir();
		}
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
			String DIR_TEMPORARY_STORE_FILES = "C:/hddt-ses/server/template/";
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


	
	@RequestMapping(value = "/viewimg", method = {RequestMethod.POST})
	public String viewimg(Locale locale, HttpServletRequest req, HttpSession session
			, @RequestAttribute(name = "transaction", value = "", required = false) String transaction, String action) throws Exception{
		CurrentUserProfile cup = getCurrentlyAuthenticatedPrincipal();
		_id = commons.getParameterFromRequest(req, "_id").replaceAll("\\s", "");

		req.setAttribute("_header_", "Xem phôi hóa đơn");
		
		JSONRoot root = new JSONRoot();
		MsgRsp rsp = restAPI.callAPINormal("/phoihd/viewimg/" + _id, cup.getLoginRes().getToken(), HttpMethod.POST, root);
		 Object anh = rsp.getObjData();
		req.setAttribute("invoiceTemplate", anh);
		LoadParameter(cup, locale, req, action);
		return "/ad_qlyphoihd/viewimg";
	}
	
	
}
