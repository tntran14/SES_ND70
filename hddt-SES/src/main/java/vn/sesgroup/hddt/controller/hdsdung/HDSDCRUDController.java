package vn.sesgroup.hddt.controller.hdsdung;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Iterator;
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
import com.api.message.MsgRsp;
import com.api.message.MspResponseStatus;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;

import vn.sesgroup.hddt.controller.AbstractController;
import vn.sesgroup.hddt.dto.BaseDTO;
import vn.sesgroup.hddt.dto.CurrentUserProfile;
import vn.sesgroup.hddt.dto.FileInfo;
import vn.sesgroup.hddt.dto.IssuerInfo;
import vn.sesgroup.hddt.dto.LoginRes;
import vn.sesgroup.hddt.resources.RestAPIUtility;
import vn.sesgroup.hddt.utils.Constants;
import vn.sesgroup.hddt.utils.Json;
import vn.sesgroup.hddt.utils.SystemParams;

@Controller
@RequestMapping({
	"/hdsd-cre"
	, "/hdsd-detail"
	, "/hdsd-edit"
	, "/hdsd-delete"
})
@Scope(value = WebApplicationContext.SCOPE_REQUEST)
public class HDSDCRUDController extends AbstractController{
	private static final Logger log = LogManager.getLogger(HDSDCRUDController.class);
	@Autowired RestAPIUtility restAPI;
	@Autowired RestTemplate restTemplate;
	private String errorDesc;
	private String errorCode;
	private IssuerInfo isu;
	private String _id;
	private String	chude ; 
	private String	file ; 
	private String	tieude ; 
	private String	noidung ; 
	private String	File ; 
	

	
	private void inquiry(CurrentUserProfile cup, Locale locale, HttpServletRequest req, HttpSession session, String action) throws Exception{
		cup = getCurrentlyAuthenticatedPrincipal();
		_id = commons.getParameterFromRequest(req, "_id");
		BaseDTO baseDTO = new BaseDTO(req);
		Msg msg = baseDTO.createMsg(cup, Constants.MSG_ACTION_CODE.INQUIRY);
		HashMap<String, String> hData = new HashMap<>();
		msg.setObjData(hData);
		JSONRoot root = new JSONRoot(msg);
		MsgRsp rsp = restAPI.callAPINormal("/hdsd/detail/" + _id, cup.getLoginRes().getToken(), HttpMethod.POST, root);
		
		MspResponseStatus rspStatus = rsp.getResponseStatus();
		if (rspStatus.getErrorCode() == 0) {
			JsonNode jsonData = Json.serializer().nodeFromObject(rsp.getObjData());
			req.setAttribute("_id", commons.getTextJsonNode(jsonData.at("/_id")));
			req.setAttribute("File", commons.getTextJsonNode(jsonData.at("/File")));
			req.setAttribute("Tieude", commons.getTextJsonNode(jsonData.at("/Tieude")));
			req.setAttribute("Chude", commons.getTextJsonNode(jsonData.at("/Chude")));
			req.setAttribute("Content", commons.decodeURIComponent(commons.getTextJsonNode(jsonData.at("/Noidung"))));
	
			} else {
				rspStatus.getErrorDesc();
			}
		}
		


	
	
	@RequestMapping(value = "/init", method = {RequestMethod.POST})
	public String init(LoginRes us,Locale locale, HttpServletRequest req, HttpSession session
			, @RequestAttribute(name = "transaction", value = "", required = false) String transaction) throws Exception{
		CurrentUserProfile cup = getCurrentlyAuthenticatedPrincipal();
		us = cup.getLoginRes();
		isu = us.getIssuerInfo();
	
		String header = "Thêm mới hướng dẫn sử dụng";
		String action = "CREATE";
		boolean isEdit = false;

		switch (transaction) {
		case "hdsd-cre":
			header = "Thêm mới hướng dẫn sử dụng";
			action = "CREATE";
			isEdit = true;
			break;
		case "hdsd-edit":
			header = "Thay đổi thông tin hướng dẫn sử dụng";
			action = "EDIT";
			isEdit = true;
			break;
		case "hdsd-detail":
			header = "Chi tiết hướng dẫn sử dụng";
			action = "DETAIL";
			isEdit = false;
			break;		
		default:
			break;
		}
		
		if("|hdsd-edit|hdsd-detail".indexOf(transaction) != -1)
			inquiry(cup, locale, req, session, action);
		req.setAttribute("_header_", header);
		req.setAttribute("_action_", action);
		req.setAttribute("_isedit_", isEdit);
		req.setAttribute("_id",_id);
	
		if(!"".equals(errorDesc))
			req.setAttribute("messageError", errorDesc);

		
		return "/hdsd/hdsd-crud";
	}
	
	
	
	
	public BaseDTO checkDataToAccept(HttpServletRequest req, HttpSession session, String transaction
			, CurrentUserProfile cup) throws Exception{
		BaseDTO dto = new BaseDTO();
		dto.setErrorCode(0);
		_id = commons.getParameterFromRequest(req, "_id");
		chude = commons.getParameterFromRequest(req, "title");
		tieude = commons.getParameterFromRequest(req, "summaryContent");
		noidung = commons.getParameterFromRequest(req, "content");
		File = commons.getParameterFromRequest(req, "attachFileNameSystem");
		
	
		switch (transaction) {
		case "hdsd-cre":
		case "hdsd-edit":
			if("".equals(chude)) {
				dto.setErrorCode(1);
				dto.getErrorMessages().add("Vui lòng  nhập Chủ đề.");
			}
			if("".equals(tieude)) {
				dto.setErrorCode(1);
				dto.getErrorMessages().add("Vui lòng nhập Tiêu đề.");
			}
			if("".equals(noidung)) {
				dto.setErrorCode(1);
				dto.getErrorMessages().add("Vui lòng nhập Nội dung.");
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
		String messageConfirm = "Bạn có muốn thêm mới hướng dẫn sử dụng không?";
		switch (transaction) {
		case "hdsd-cre":
			messageConfirm = "Bạn có muốn thêm mới hướng dẫn sử dụng không?";
			break;
		case "hdsd-edit":
			messageConfirm = "Bạn có muốn thay đổi thông tin hướng dẫn sử dụng không?";
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
		case "hdsd-cre": actionCode = Constants.MSG_ACTION_CODE.CREATED; break;
		case "hdsd-edit": actionCode = Constants.MSG_ACTION_CODE.MODIFY; break;
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
		hData.put("File", File);
		hData.put("Chude", chude);
		hData.put("Tieude", tieude);
		hData.put("Noidung", noidung);


		msg.setObjData(hData);
		JSONRoot root = new JSONRoot(msg);
		MsgRsp rsp = restAPI.callAPINormal("/hdsd/crud", cup.getLoginRes().getToken(), HttpMethod.POST, root);
		MspResponseStatus rspStatus = rsp.getResponseStatus();
		if(rspStatus.getErrorCode() == 0) {
			dtoRes.setErrorCode(0);
			switch (transaction) {
			case "hdsd-cre":
				dtoRes.setResponseData("Thêm mới thông tin hướng dẫn sử dụng thành công.");
				break;
			case "hdsd-edit":
				dtoRes.setResponseData("Cập nhật thông tin hướng dẫn sử dụng thành công.");
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
		String DIR_TEMPORARY_STORE_FILES = "C:/hddt-ses/server/template/file/";
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
					
					fileName =fileNameOriginal;
					if(saveUploadFiles(multiFile, fileName, DIR_TEMPORARY_STORE_FILES )) {
							session.setAttribute("FILE_NAME_LOGO", fileName);
					}
				}
			}
			dto.setErrorCode(0);
			HashMap<String, String> hResult = new HashMap<>();
			hResult.put("id", keyFileName.toString() + "Name");
			hResult.put("SystemFilename", fileNameOriginal);
			hResult.put("OriginalFilename", fileName);
			dto.setResponseData(hResult);
		}catch(Exception e) {
			e.printStackTrace();
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
