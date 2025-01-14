package vn.sesgroup.hddt.controller.issu;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.URLEncoder;
import java.security.Principal;
import java.util.HashMap;
import java.util.Locale;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.context.WebApplicationContext;

import com.api.message.JSONRoot;
import com.api.message.Msg;
import com.api.message.MsgPage;
import com.api.message.MsgRsp;
import com.api.message.MspResponseStatus;
import com.fasterxml.jackson.databind.JsonNode;

import vn.sesgroup.hddt.controller.AbstractController;
import vn.sesgroup.hddt.dto.BaseDTO;
import vn.sesgroup.hddt.dto.CurrentUserProfile;
import vn.sesgroup.hddt.dto.FileInfo;
import vn.sesgroup.hddt.dto.JsonGridDTO;
import vn.sesgroup.hddt.dto.LoginRes;
import vn.sesgroup.hddt.resources.RestAPIUtility;
import vn.sesgroup.hddt.utils.Constants;
import vn.sesgroup.hddt.utils.Json;

@Controller
@RequestMapping("/mauso-expires")
@Scope(value = WebApplicationContext.SCOPE_REQUEST)
public class MauSoExpiresController extends AbstractController {
	private static final Logger log = LogManager.getLogger(MauSoExpiresController.class);
	@Autowired
	RestAPIUtility restAPI;

	private String mst;
	private String tyle;
	private String status;


	@RequestMapping(value = "/init", method = { RequestMethod.POST, RequestMethod.GET })
	public String init(Locale locale, Principal principal, HttpServletRequest req) throws Exception {
		req.setAttribute("_TitleView_", Constants.PREFIX_TITLE + " - Danh sách mẫu số khách hàng");
		req.setAttribute("map_status", Constants.MAP_STATUS);
		CurrentUserProfile cup = getCurrentlyAuthenticatedPrincipal();
		LoginRes issu = cup.getLoginRes();
		String _id = issu.getIssuerId();
		
		BaseDTO baseDTO = new BaseDTO(req);
		Msg msg = baseDTO.createMsg(cup, Constants.MSG_ACTION_CODE.INQUIRY);
		HashMap<String, String> hData = new HashMap<>();
		msg.setObjData(hData);
		JSONRoot root = new JSONRoot(msg);
		MsgRsp rsp = restAPI.callAPINormal("/main/profile/" + _id, cup.getLoginRes().getToken(), HttpMethod.POST, root);
		JsonNode jsonData = Json.serializer().nodeFromObject(rsp.getObjData());
		String isroot = commons.getTextJsonNode(jsonData.at("/IsRoot"));
		
		req.setAttribute("map_ms_status", Constants.MAP_MS_STATUS);
		
		String MS_EXPIRES = "30";
		rsp = restAPI.callAPINormal("/param-admin/detail", cup.getLoginRes().getToken(), HttpMethod.POST, root);
		MspResponseStatus rspStatus = rsp.getResponseStatus();
		if (rspStatus.getErrorCode() == 0) {	
			 jsonData = Json.serializer().nodeFromObject(rsp.getObjData());			
			MS_EXPIRES = commons.getTextJsonNode(jsonData.at("/MS_EXPIRES"));	
		}
		
		req.setAttribute("TyLe", MS_EXPIRES);
		
		if("true".equals(isroot) && issu.isRoot() == true && issu.isAdmin() == true)
		{return "/issu/mauso-expires";}
		else {
			return "/admin/admin";
		}
		
	}

	private BaseDTO checkDataSearch(Locale locale, HttpServletRequest req, HttpSession session) {
		BaseDTO dto = new BaseDTO();
		dto.setErrorCode(0);
	
		mst = commons.getParameterFromRequest(req, "mst").replaceAll("\\s", "");
		tyle = commons.getParameterFromRequest(req, "tyle").replaceAll("\\s", "");
		status = commons.getParameterFromRequest(req, "status").replaceAll("\\s", "");
	
		return dto;
	}
	
	
	@RequestMapping(value = "/search",  produces = MediaType.APPLICATION_JSON_VALUE, method = RequestMethod.POST)
	@ResponseBody
	public BaseDTO execSearch(Locale locale, HttpServletRequest req, HttpSession session) throws Exception{
		JsonGridDTO grid = new JsonGridDTO();
		
		BaseDTO baseDTO = checkDataSearch(locale, req, session);
		if(0 != baseDTO.getErrorCode()) {
			grid.setErrorCode(baseDTO.getErrorCode());
			grid.setErrorMessages(baseDTO.getErrorMessages());
			grid.setResponseData(Constants.MAP_ERROR.get(999));
			return grid;
		}
		
		CurrentUserProfile cup = getCurrentlyAuthenticatedPrincipal();		
		baseDTO = new BaseDTO(req);
		Msg msg = baseDTO.createMsg(cup, Constants.MSG_ACTION_CODE.SEARCH);
		
		HashMap<String, Object> hData = new HashMap<>();
		hData.put("TaxCode", mst);
		hData.put("TyLe", tyle);
		hData.put("Status", status);
		
		msg.setObjData(hData);
		
		JSONRoot root = new JSONRoot(msg);
		MsgRsp rsp = restAPI.callAPINormal("/mauso-expires/list", cup.getLoginRes().getToken(), HttpMethod.POST, root);
		MspResponseStatus rspStatus = rsp.getResponseStatus();
		if(rspStatus.getErrorCode() == 0) {
			MsgPage page = rsp.getMsgPage();
			grid.setTotal(page.getTotalRows());
			
			JsonNode jsonData = Json.serializer().nodeFromObject(rsp.getObjData());
			JsonNode rows = null;
			HashMap<String, String> hItem = null;
			if(!jsonData.at("/rows").isMissingNode()) {
				rows = jsonData.at("/rows");
				for(JsonNode row: rows) {
					hItem = new HashMap<String, String>();
					
					hItem.put("_id", commons.getTextJsonNode(row.at("/_id")));
					hItem.put("TaxCode", commons.getTextJsonNode(row.at("/TaxCode")));
					hItem.put("Name", commons.getTextJsonNode(row.at("/Name")));
					hItem.put("MSHDon", commons.getTextJsonNode(row.at("/MSHDon")));				
					hItem.put("SoLuong", commons.getTextJsonNode(row.at("/SoLuong")));
					hItem.put("SoLuongConLai", commons.getTextJsonNode(row.at("/SoLuongConLai")));
					hItem.put("SoLuongDaDung", commons.getTextJsonNode(row.at("/SoLuongDaDung")));
					hItem.put("TiLe", commons.getTextJsonNode(row.at("/TiLe")));
									
					hItem.put("Status", commons.getTextJsonNode(row.at("/Status")));
										
					grid.getRows().add(hItem);
				}
			}
			
		}else {
			grid = new JsonGridDTO();
			grid.setErrorCode(rspStatus.getErrorCode());
			grid.setResponseData(rspStatus.getErrorDesc());
		}
		
		return grid;
	}
	
	
	
	@RequestMapping(value = {"/check-data-export"},  produces = MediaType.APPLICATION_JSON_VALUE, method = RequestMethod.POST)
	@ResponseBody
	public BaseDTO execCheckDataToExport(Locale locale, HttpServletRequest req, HttpSession session
			, @RequestParam(value = "transaction", required = false, defaultValue = "") String transaction
			, @RequestParam(value = "method", required = false, defaultValue = "") String method
		) throws Exception {
		String token = "";
		if (null != session.getAttribute(Constants.SESSION_TYPE.SESSION_TOKEN_EXECUTE)) {
			token = session.getAttribute(Constants.SESSION_TYPE.SESSION_TOKEN_EXECUTE).toString();
			session.removeAttribute(token);
		}
		session.removeAttribute(Constants.SESSION_TYPE.SESSION_TOKEN_EXECUTE);
		
		BaseDTO dto = new BaseDTO();
		
		dto = checkDataSearch(locale, req, session);
		if(0 != dto.getErrorCode()) {
			dto.setErrorCode(999);
			dto.setResponseData(Constants.MAP_ERROR.get(999));
			return dto;
		}
		
		token = commons.csRandomAlphaNumbericString(30);
		session.setAttribute(Constants.SESSION_TYPE.SESSION_TOKEN_EXECUTE, token);
		
		HashMap<String, Object> hData = new HashMap<>();
		hData = new HashMap<>();
		hData.put("TaxCode", mst);
		hData.put("Status", status);
		hData.put("TyLe", tyle);
		

		
		HashMap<String, String> hInfo = new HashMap<String, String>();
		hInfo.put("TOKEN", token);
		session.setAttribute(token, hData);
		
		dto.setResponseData(hInfo);
		dto.setErrorCode(0);
		return dto;
	}
	
	@SuppressWarnings("unchecked")
	@RequestMapping(
			value = {
				"/export-excel/{token-transaction}"
			}, 
			method = {RequestMethod.GET }
	)
	public void exportExcel(Locale locale, HttpServletRequest req, HttpServletResponse resp, HttpSession session
			, @RequestAttribute(name = "method", required = false, value = "") String method
			, @PathVariable(name = "token-transaction", required = false, value = "") String tokenTransaction) throws Exception {
		PrintWriter writer = null;		
		BaseDTO baseDTO = null;

		try {
			/*CHECK TOKEN*/
			String token = session.getAttribute(Constants.SESSION_TYPE.SESSION_TOKEN_EXECUTE) == null ? ""
					: session.getAttribute(Constants.SESSION_TYPE.SESSION_TOKEN_EXECUTE).toString();
			session.removeAttribute(Constants.SESSION_TYPE.SESSION_TOKEN_EXECUTE);
			if ("".equals(token) || !tokenTransaction.equals(token)) {
				resp.setContentType("text/html; charset=utf-8");
		        resp.setCharacterEncoding("UTF-8");
		        resp.setHeader("success", "yes");
		        writer = resp.getWriter();
		        writer.write("Token giao dịch không hợp lệ.");
		        writer.flush();
		        writer.close();
		        return;
			}
			/*END: CHECK TOKEN*/
			HashMap<String, Object> hData = new HashMap<>();
			if(null != session.getAttribute(token) && session.getAttribute(token) instanceof HashMap)
				hData = (HashMap<String, Object>) session.getAttribute(token);
			session.removeAttribute(token);
			
			CurrentUserProfile cup = getCurrentlyAuthenticatedPrincipal();
			baseDTO = new BaseDTO(req);
			Msg msg = baseDTO.createMsg(cup, Constants.MSG_ACTION_CODE.SEARCH);

			msg.setObjData(hData);
			
			JSONRoot root = new JSONRoot(msg);
			
			String url = "/mauso-expires/export-excel";		
			FileInfo fileInfo = restAPI.callAPIGetFileInfo(url, cup.getLoginRes().getToken(), HttpMethod.POST, root);
			if(null != fileInfo) {
				String fileNameOut = "DANH-SACH-MAU-SO-HET-HAN.xlsx";
				
				String type = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet;charset=UTF-8";
				resp.setHeader("Content-Disposition", "inline; filename=" + URLEncoder.encode(null == fileInfo.getFileName()? fileNameOut: fileInfo.getFileName(), "UTF-8").replaceAll("\\+", "%20"));
				InputStream inputStream = new ByteArrayInputStream(fileInfo.getContentFile());
				resp.setHeader("Content-Type", type);
				
				resp.setHeader("Cache-Control", "no-cache, no-store, must-revalidate"); // HTTP 1.1.
				resp.setHeader("Pragma", "no-cache"); // HTTP 1.0.
				resp.setHeader("Expires", "0"); // Proxies.
				
				int bufferSize = 1024;
//				String mimetype = "application/pdf";
				resp.setContentType(type);
		        final byte[] buffer = new byte[bufferSize];
		        int bytesRead = 0;
		        OutputStream out = null;
		        try {
		            out = resp.getOutputStream();
		            long totalWritten = 0;
		            while ((bytesRead = inputStream.read(buffer)) > 0) {
		                out.write(buffer, 0, bytesRead);
		                totalWritten += bytesRead;
		                if (totalWritten >= buffer.length) {
		                    out.flush();
		                }
		            }
		        } finally {
		            tryToCloseStream(out);
		            tryToCloseStream(inputStream);
		        }
			}else {
				resp.setContentType("text/html; charset=utf-8");
		        resp.setCharacterEncoding("UTF-8");
		        resp.setHeader("success", "yes");
		        writer = resp.getWriter();
		        writer.write("[NULL] - " + Constants.ERROR_DESCRIPTION_EXPORT_EXCEL);
		        writer.close();
			}
		}catch(Exception e) {
			log.error(">>>>> An exception occurred!", e);
			
			resp.setContentType("text/html; charset=utf-8");
	        resp.setCharacterEncoding("UTF-8");
	        resp.setHeader("success", "yes");
	        writer = resp.getWriter();
	        writer.write("Exception - " + Constants.ERROR_DESCRIPTION_EXPORT_EXCEL);
	        writer.flush();
	        writer.close();
		}
	}

}
