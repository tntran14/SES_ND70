package vn.sesgroup.hddt.controller.admin.support;

import java.io.File;
import java.time.LocalDate;
import java.util.HashMap;
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

import com.api.message.JSONRoot;
import com.api.message.Msg;
import com.api.message.MsgRsp;
import com.api.message.MspResponseStatus;
import com.fasterxml.jackson.databind.JsonNode;

import vn.sesgroup.hddt.controller.AbstractController;
import vn.sesgroup.hddt.dto.BaseDTO;
import vn.sesgroup.hddt.dto.CurrentUserProfile;
import vn.sesgroup.hddt.dto.IssuerInfo;
import vn.sesgroup.hddt.dto.LoginRes;
import vn.sesgroup.hddt.resources.RestAPIUtility;
import vn.sesgroup.hddt.utils.Constants;
import vn.sesgroup.hddt.utils.Json;
import vn.sesgroup.hddt.utils.SystemParams;

@Controller
@RequestMapping({
	"/support-cre"
	, "/support-detail"
	, "/support-edit"
	, "/support-active"
	, "/support-deactive"
	, "/support-del"
})
@Scope(value = WebApplicationContext.SCOPE_REQUEST)
public class SupportAdminCRUDController extends AbstractController{
	private static final Logger log = LogManager.getLogger(SupportAdminCRUDController.class);
	@Autowired RestAPIUtility restAPI;
	@Autowired RestTemplate restTemplate;
	
	private String errorCode;
	private String errorDesc;
	private String _id;
	private String title;
	private String content;
	private String summaryContent;
	private String attachFileName;
	private String attachFileNameSystem;
	
	
	@RequestMapping(value = {"/init"}, method = {RequestMethod.POST})
	public String init(Locale locale, HttpServletRequest req, HttpSession session
			, @RequestAttribute(name = "transaction", value = "", required = false) String transaction
			, @RequestAttribute(name = "method", value = "", required = false) String method
			, @RequestParam(value = "tokenTransaction", required = false, defaultValue = "") String tokenTransaction
			) throws Exception{
		errorCode = "";
		errorDesc = "";
		
		CurrentUserProfile cup = getCurrentlyAuthenticatedPrincipal();
		IssuerInfo ii = cup.getLoginRes().getIssuerInfo();
		
		String mst = "";
		String name = "";
		if(!tokenTransaction.equals("")) {
			String[] split =tokenTransaction.split(",");
			mst = split[0];
			name = split[1]; 
		}
		
		LoginRes issu = cup.getLoginRes();
		String _idsu = issu.getIssuerId();

		BaseDTO baseDTO = new BaseDTO(req);
		Msg msg = baseDTO.createMsg(cup, Constants.MSG_ACTION_CODE.INQUIRY);
		HashMap<String, String> hData = new HashMap<>();
		msg.setObjData(hData);

		JSONRoot root = new JSONRoot(msg);
		
		_id = commons.getParameterFromRequest(req, "_id");
		String header = "Thêm mới phần mềm hỗ trợ";
		String action = "CREATE";
		boolean isEdit = false;
		
		req.setAttribute("NLap", commons.convertLocalDateTimeToString(LocalDate.now(), Constants.FORMAT_DATE.FORMAT_DATE_WEB));

				
		switch (transaction) {
		case "support-cre":
			header = "Thêm mới phần mềm hỗ trợ";
			action = "CREATE";
			isEdit = true;
			break;
		case "support-detail":
			header = "Chi tiết thông tin phần mềm hỗ trợ";
			action = "DETAIL";
			isEdit = false;
			break;
		case "support-edit":
			header = "Thay đổi thông tin phần mềm hỗ trợ";
			action = "EDIT";
			isEdit = true;
			break;
		case "support-active":
			
			break;
		case "support-deactive":
			
			break;
		case "support-del":
			
			break;
		default:
			break;
		}
		
		if("|support-edit|support-detail|support-active|support-deactive|support-del|".indexOf(transaction) != -1
				|| "init-dc-tt".equals(method))
			inquiry(cup, locale, req, session, _id, action, transaction, method);
		if("support-cre".equals(transaction)) {
	
			
		}
		
		req.setAttribute("_header_", header);
		req.setAttribute("_action_", action);
		req.setAttribute("_isedit_", isEdit);
		req.setAttribute("_id", _id);
		
		if("|support-cre|support-edit|".indexOf(transaction) != -1)
		
		if(!"".equals(errorDesc))
			req.setAttribute("messageError", errorDesc);

		
		return "support-admin/support-crud";
	}

	private void inquiry(CurrentUserProfile cup, Locale locale, HttpServletRequest req, HttpSession session, String _id , String action, String transaction, String method) throws Exception{
		if("".equals(_id)) {
			errorCode = "NOT FOUND";
			errorDesc = "Không tìm thấy thông tin phần mềm hỗ trợ.";
			return;
		}
		BaseDTO baseDTO = new BaseDTO(req);
		Msg msg = baseDTO.createMsg(cup, Constants.MSG_ACTION_CODE.INQUIRY);
		HashMap<String, String> hData = new HashMap<>();
		msg.setObjData(hData);
		
		JSONRoot root = new JSONRoot(msg);
		MsgRsp rsp = restAPI.callAPINormal("/support_admin/detail/" + _id, cup.getLoginRes().getToken(), HttpMethod.POST, root);
		MspResponseStatus rspStatus = rsp.getResponseStatus();
		if(rspStatus.getErrorCode() == 0) {
			JsonNode jsonData = Json.serializer().nodeFromObject(rsp.getObjData());
			
			req.setAttribute("Title", commons.getTextJsonNode(jsonData.at("/Title")));
			req.setAttribute("Content",  commons.decodeURIComponent(commons.getTextJsonNode(jsonData.at("/Content"))));
			req.setAttribute("SummaryContent", commons.getTextJsonNode(jsonData.at("/SummaryContent")));
			req.setAttribute("ImageLogo", commons.getTextJsonNode(jsonData.at("/ImageLogo")));
			req.setAttribute("ImageLogoOriginalFilename",commons.getTextJsonNode(jsonData.at("/ImageLogoOriginalFilename")));
		
	
		}else {
			errorDesc = rspStatus.getErrorDesc();
		}
	}
	
	public BaseDTO checkDataToAccept(HttpServletRequest req, HttpSession session, String transaction
			, CurrentUserProfile cup) throws Exception{
		BaseDTO dto = new BaseDTO();
		dto.setErrorCode(0);
		
		_id = commons.getParameterFromRequest(req, "_id").replaceAll("\\s", "");
		title = commons.getParameterFromRequest(req, "title").trim().replaceAll("\\s+", " ");		
		content = commons.getParameterFromRequest(req, "content").trim().replaceAll("\\s+", " ");
		summaryContent = commons.getParameterFromRequest(req, "summaryContent").trim().replaceAll("\\s+", " ");
		attachFileName = commons.getParameterFromRequest(req, "attachFileName").trim().replaceAll("\\s+", " ");
		attachFileNameSystem = commons.getParameterFromRequest(req, "attachFileNameSystem").trim().replaceAll("\\s+", " ");		
	
	
		
		if("support-edit".equals(transaction)) {
			if("".equals(_id)) {
				dto.setErrorCode(1);
				dto.getErrorMessages().add("Không tìm thấy thông tin phần mềm hỗ trợ.");
			}
		}
		File file = null;
		switch (transaction) {
		case "support-cre":	
			if("".equals(title)) {
				dto.setErrorCode(1);
				dto.getErrorMessages().add("Vui lòng nhập vào tiêu đề.");
			}
			
		
			if("".equals(attachFileName) || "".equals(attachFileNameSystem)) {
				dto.setErrorCode(1);
				dto.getErrorMessages().add("Vui lòng kiểm tra lại file.");
			}else {
				file = new File(SystemParams.DIR_E_INVOICE_TEMPORARY, attachFileNameSystem);
				if(!file.exists() || !file.isFile()) {
					dto.setErrorCode(1);
					dto.getErrorMessages().add("Tập tin không tồn tại.");
				}
			}
			break;
			
		case "support-edit":
			if("".equals(title)) {
				dto.setErrorCode(1);
				dto.getErrorMessages().add("Vui lòng nhập vào tiêu đề.");
			}
			break;
		case "support-actice":
		case "support-deactive":
		case "support-del":
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
		String messageConfirm = "Bạn có muốn thêm mới phần mềm hỗ trợ không?";
		switch (transaction) {
		case "support-cre":
			messageConfirm = "Bạn có muốn thêm mới phần mềm hỗ trợ không?";
			break;
		case "support-edit":
			messageConfirm = "Bạn có muốn thay đổi thông tin phần mềm hỗ trợ không?";
			break;
		case "support-active":
			messageConfirm = "Bạn có muốn kích hoạt thông tin phần mềm hỗ trợ không?";
			break;
		case "support-deactive":
			messageConfirm = "Bạn có muốn hủy kích hoạt thông tin phần mềm hỗ trợ không?";
			break;
		case "support-del":
			messageConfirm = "Bạn có muốn xóa thông tin phần mềm hỗ trợ không?";
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
		String actionCode = Constants.MSG_ACTION_CODE.CREATED;
		switch (transaction) {
		case "support-cre": actionCode = Constants.MSG_ACTION_CODE.CREATED; break;
		case "support-edit": actionCode = Constants.MSG_ACTION_CODE.MODIFY; break;
		case "support-active": actionCode = Constants.MSG_ACTION_CODE.ACTIVE; break;
		case "support-deactive": actionCode = Constants.MSG_ACTION_CODE.DEACTIVE; break;
		case "support-del": actionCode = Constants.MSG_ACTION_CODE.DELETE; break;

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
		hData.put("Title", title);				
		hData.put("Content", content);
		hData.put("SummaryContent", summaryContent);
		hData.put("AttachFileName", attachFileName);
		hData.put("AttachFileNameSystem", attachFileNameSystem);
	
		msg.setObjData(hData);
		JSONRoot root = new JSONRoot(msg);
		MsgRsp rsp = restAPI.callAPINormal("/support_admin/crud", cup.getLoginRes().getToken(), HttpMethod.POST, root);
		MspResponseStatus rspStatus = rsp.getResponseStatus();
		if(rspStatus.getErrorCode() == 0) {
			dtoRes.setErrorCode(0);
			switch (transaction) {
			case "support-cre":
				dtoRes.setResponseData("Thêm mới thông tin phần mềm hỗ trợ thành công.");
				break;
			case "support-edit":
				dtoRes.setResponseData("Cập nhật thông tin phần mềm hỗ trợ thành công.");
				break;
			case "support-active":
				dtoRes.setResponseData("Kích hoạt phần mềm hỗ trợ thành công.");
				break;
			case "support-deactive":
				dtoRes.setResponseData("Hủy kích hoạt thông tin phần mềm hỗ trợ thành công.");
				break;
			case "support-del":
				dtoRes.setResponseData("Xóa thông tin phần mềm hỗ trợ thành công.");
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
	