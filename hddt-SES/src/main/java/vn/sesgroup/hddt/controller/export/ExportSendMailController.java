package vn.sesgroup.hddt.controller.export;

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
import vn.sesgroup.hddt.resources.RestAPIUtility;
import vn.sesgroup.hddt.utils.Constants;
import vn.sesgroup.hddt.utils.Json;
import vn.sesgroup.hddt.utils.SystemParams;

@Controller
@RequestMapping({
	"/export-send-mail"
})
@Scope(value = WebApplicationContext.SCOPE_REQUEST)
public class ExportSendMailController extends AbstractController{
	private static final Logger log = LogManager.getLogger(ExportSendMailController.class);
	@Autowired RestAPIUtility restAPI;
	
	private String errorCode;
	private String errorDesc;
	private String _id;
	private String _title;
	private String _email;
	private String _content;
	
	@RequestMapping(value = "/init", method = {RequestMethod.POST})
	public String init(Locale locale, HttpServletRequest req, HttpSession session
			, @RequestAttribute(name = "transaction", value = "", required = false) String transaction) throws Exception{
		req.setAttribute("_header_", "Gửi thông tin hóa đơn");
		
		errorCode = "";
		errorDesc = "";
		
		CurrentUserProfile cup = getCurrentlyAuthenticatedPrincipal();
		_id = commons.getParameterFromRequest(req, "_id");
		inquiry(cup, locale, req, session, _id, "DETAIL");
		
		if(!"".equals(errorDesc))
			req.setAttribute("messageError", errorDesc);
		
		return "export/export-send-mail-export";
	}
	
	private void inquiry(CurrentUserProfile cup, Locale locale, HttpServletRequest req, HttpSession session, String _id , String action) throws Exception{
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
		MsgRsp rsp = restAPI.callAPINormal("/export/detail/" + _id, cup.getLoginRes().getToken(), HttpMethod.POST, root);
		MspResponseStatus rspStatus = rsp.getResponseStatus();
		if(rspStatus.getErrorCode() == 0) {
			IssuerInfo ii = cup.getLoginRes().getIssuerInfo();
			
			JsonNode jsonData = Json.serializer().nodeFromObject(rsp.getObjData());
			
			String title = "";
			String emailReceive = "";
			
			String _tmp = "";
			title = ii.getTaxCode() + " " + ii.getName() + " Thông báo phát hành phiếu xuất kho kiêm vận chuyển nội bộ";
			
			_tmp = commons.getTextJsonNode(jsonData.at("/EInvoiceDetail/NDHDon/NMua/MST"));
			if(!"".equals(_tmp))
				title += " " + _tmp;
			_tmp = commons.getTextJsonNode(jsonData.at("/EInvoiceDetail/NDHDon/NMua/Ten"));
			if("".equals(_tmp))
				_tmp = commons.getTextJsonNode(jsonData.at("/EInvoiceDetail/NDHDon/NMua/HVTNMHang"));
			if(!"".equals(_tmp))
				title += " " + _tmp;
			_tmp = " - Số HĐ ";
			if(!jsonData.at("/EInvoiceDetail/TTChung/SHDon").isMissingNode())
				_tmp += commons.formatNumberBillInvoice(jsonData.at("/EInvoiceDetail/TTChung/SHDon").doubleValue());
			_tmp += " (No reply)";
			title += _tmp;
			
		String 	emailReceivecc = commons.getTextJsonNode(jsonData.at("/EInvoiceDetail/NDHDon/NMua/DCTDTuCC"));
			
		String	emailReceivemain = commons.getTextJsonNode(jsonData.at("/EInvoiceDetail/NDHDon/NMua/DCTDTu"));
if(emailReceivemain!="" || emailReceivecc !="") {
			
			if(emailReceivemain!="" && emailReceivecc =="") {
				
				emailReceive = emailReceivemain; 	
			}
			else if(emailReceivemain == "" && emailReceivecc!="") {
				emailReceive = emailReceivecc;	
			}
			else {
				emailReceive = emailReceivemain + "," + emailReceivecc;
			}	
		}
			else {
				emailReceive = "";
			}
		
			_tmp = commons.getTextJsonNode(jsonData.at("/EInvoiceDetail/NDHDon/NMua/Ten")).toUpperCase();
			if("".equals(_tmp))
				_tmp = commons.getTextJsonNode(jsonData.at("/EInvoiceDetail/NDHDon/NMua/HVTNMHang")).toUpperCase();
			
			String url =commons.getTextJsonNode(jsonData.at("/PramLink/LinkPortal"));
			String secretCode = commons.getTextJsonNode(jsonData.at("/SecureKey"));
			
			StringBuilder sb = new StringBuilder();
			sb.setLength(0);
			sb.append("<p><span style='font-family: Times New Roman;font-size: 13px;'>Kính gửi: <label style='font-weight: bold;'>" + ("".equals(_tmp)? "Quý khách hàng": _tmp) + "</label><o:p></o:p></span></p>\n");
			sb.append("<p><span style='font-family: Times New Roman;font-size: 13px;'><label style='font-weight: bold;'>" + ii.getName() + "</label> xin chân thành cảm ơn Quý đơn vị đã tin tưởng và sử dụng sản phẩm/dịch vụ của chúng tôi!</span></p>\n");
			sb.append("<p><span style='font-family: Times New Roman;font-size: 13px;'>Chúng tôi vừa phát hành hóa đơn điện tử đến Quý đơn vị với thông tin như sau:</span></p>\n");
			
			_tmp = "";
			if(!jsonData.at("/EInvoiceDetail/TTChung/SHDon").isMissingNode())
				_tmp += commons.formatNumberBillInvoice(jsonData.at("/EInvoiceDetail/TTChung/SHDon").doubleValue());
			
			sb.append("<p><span style='font-family: Times New Roman;font-size: 13px;'>1.  Số hoá đơn:  " + _tmp + "</span></p>\n");
			sb.append("<p><span style='font-family: Times New Roman;font-size: 13px;'>2.  Mẫu hoá đơn: " + (commons.getTextJsonNode(jsonData.at("/EInvoiceDetail/TTChung/KHMSHDon")) + commons.getTextJsonNode(jsonData.at("/EInvoiceDetail/TTChung/KHHDon"))) + "</span></p>\n");
			sb.append("<p><span style='font-family: Times New Roman;font-size: 13px;'>3.  Link truy cập: <a target='_blank' href='" + url + "'>" + url + "</a></span></p>\n");
			sb.append("<p><span style='font-family: Times New Roman;font-size: 13px;'>4.  Mã bảo mật: <a target='_blank' href='" + url + "'>" + secretCode + "</a></span></p>\n");
			sb.append("<p><span style='font-family: Times New Roman;font-size: 13px;'>Quý đơn vị  vui lòng kiểm tra lại thông tin và lưu trữ hoá đơn.</span></p>\n");
			sb.append("<p><span style='font-family: Times New Roman;font-size: 13px;'>Trân trọng kính chào!</span></p>");

			sb.append("<hr style='margin: 5px 0 5px 0;'>");
			sb.append("<p style='margin-bottom: 3px;'><span style='font-family: Times New Roman;font-size: 13px;color:red;font-weight: bold;'>QUÝ CÔNG TY VUI LÒNG KHÔNG REPLY EMAIL NÀY!</span></p>");
			sb.append("<p style='margin-bottom: 0px;'><span style='font-family: Times New Roman;font-size: 13px;'><label style='font-weight: bold;'>" + ii.getName().toUpperCase() + "</label><o:p></o:p></span></p>");
			sb.append("<p><span style='font-family: Times New Roman;font-size: 13px;'>" + ii.getAddress() + "</span></p>\n");
			
			req.setAttribute("_id", _id);
			req.setAttribute("Title", title);
			req.setAttribute("EmailReceive", emailReceive);
			req.setAttribute("EmailContent", sb.toString());
		}else {
			errorDesc = rspStatus.getErrorDesc();
		}
	}
	
	public BaseDTO checkDataToSend(HttpServletRequest req, HttpSession session, String transaction
			, CurrentUserProfile cup) throws Exception{
		BaseDTO dto = new BaseDTO();
		dto.setErrorCode(0);
		
		_id = commons.getParameterFromRequest(req, "_id").replaceAll("\\s", "");
		_title = commons.getParameterFromRequest(req, "_title").trim().replaceAll("\\s+", " ").toUpperCase();
		_email = commons.getParameterFromRequest(req, "_email").trim().replaceAll("\\s+", " ");
		_content = commons.getParameterFromRequest(req, "_content").trim().replaceAll("\\s+", " ");
		
		if("".equals(_id)) {
			dto.setErrorCode(1);
			dto.getErrorMessages().add("Không tìm thấy thông tin hóa đơn.");
		}
		if("".equals(_title)) {
			dto.setErrorCode(1);
			dto.getErrorMessages().add("Vui lòng nhập vào tiêu đề email.");
		}
		if("".equals(_email)) {
			dto.setErrorCode(1);
			dto.getErrorMessages().add("Vui lòng nhập vào email nhận.");
		}
		if("".equals(_content)) {
			dto.setErrorCode(1);
			dto.getErrorMessages().add("Vui lòng nhập vào nội dung gửi mail.");
		}
		return dto;
	}
	
	@RequestMapping(value = "/check-data-send",  produces = MediaType.APPLICATION_JSON_VALUE, method = RequestMethod.POST)
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
		String messageConfirm = "Bạn có muốn thực hiện gửi mail không?";
				
		CurrentUserProfile cup = getCurrentlyAuthenticatedPrincipal();
		dto = checkDataToSend(req, session, transaction, cup);
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
	
	@RequestMapping(value = "/send-mail",  produces = MediaType.APPLICATION_JSON_VALUE, method = RequestMethod.POST)
	@ResponseBody
	public BaseDTO execSendMail(HttpServletRequest req, HttpSession session
			, @RequestAttribute(name = "transaction", value = "", required = false) String transaction
			, @RequestParam(value = "tokenTransaction", required = false, defaultValue = "") String tokenTransaction) throws Exception{
		BaseDTO dtoRes = new BaseDTO();
		
		CurrentUserProfile cup = getCurrentlyAuthenticatedPrincipal();
		dtoRes = checkDataToSend(req, session, transaction, cup);
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
		
		dtoRes = new BaseDTO(req);
		Msg msg = dtoRes.createMsg(cup, actionCode);
		HashMap<String, Object> hData = new HashMap<>();
		hData.put("_id", _id);
		hData.put("_title", _title);
		hData.put("_email", _email);
		hData.put("_content", _content);
		
		msg.setObjData(hData);
		JSONRoot root = new JSONRoot(msg);
		MsgRsp rsp = restAPI.callAPINormal("/export/send-mail", cup.getLoginRes().getToken(), HttpMethod.POST, root);
		MspResponseStatus rspStatus = rsp.getResponseStatus();
		if(rspStatus.getErrorCode() == 0) {
			dtoRes.setErrorCode(0);
			dtoRes.setResponseData("Gửi email hóa đơn thành công.");
		}else {
			dtoRes.setErrorCode(rspStatus.getErrorCode());
			dtoRes.setResponseData(rspStatus.getErrorDesc());
		}
		
		return dtoRes;
	}
	
}
