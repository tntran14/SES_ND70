package vn.sesgroup.hddt.controller.hdsdung;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.URLEncoder;
import java.security.Principal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.context.WebApplicationContext;

import com.api.message.JSONRoot;
import com.api.message.Msg;
import com.api.message.MsgPage;
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
import vn.sesgroup.hddt.dto.JsonGridDTO;
import vn.sesgroup.hddt.resources.RestAPIUtility;
import vn.sesgroup.hddt.utils.Constants;
import vn.sesgroup.hddt.utils.Json;

@Controller
@RequestMapping("/hdsduser")
@Scope(value = WebApplicationContext.SCOPE_REQUEST)
public class HDSDUserController extends AbstractController {
	@Autowired
	RestAPIUtility restAPI;

	private String _id;
	private String chude;

	
	


	@RequestMapping(value = "/init", method = { RequestMethod.POST, RequestMethod.GET })
	public String init(Locale locale,  HttpSession session,Principal principal, HttpServletRequest req) throws Exception {
		BaseDTO dto = new BaseDTO();
		dto.setErrorCode(0);
		
		BaseDTO baseDTO = new BaseDTO();
		CurrentUserProfile cup = getCurrentlyAuthenticatedPrincipal();		
		baseDTO = new BaseDTO(req);
		Msg msg = baseDTO.createMsg(cup, Constants.MSG_ACTION_CODE.SEARCH);
		HashMap<String, Object> hData = new HashMap<>();
		msg.setObjData(hData);
		List<HashMap<String, Object>> rows123 = null;
		JSONRoot root = new JSONRoot(msg);
		MsgRsp rsp = restAPI.callAPINormal("/hdsduser/list", cup.getLoginRes().getToken(), HttpMethod.POST, root);
		MspResponseStatus rspStatus = rsp.getResponseStatus();
		if(rspStatus.getErrorCode() == 0) {
			rows123 = new ArrayList<HashMap<String,Object>>();
			JsonNode jsonData = Json.serializer().nodeFromObject(rsp.getObjData());
			JsonNode rows = null;
			HashMap<String, Object> hItem = null;
			if(!jsonData.at("/rows").isMissingNode()) {
				rows = jsonData.at("/rows");
				for(JsonNode row: rows) {
					hItem = new HashMap<String, Object>();
					hItem.put("_id", commons.getTextJsonNode(row.at("/_id")));
					hItem.put("Chude", commons.getTextJsonNode(row.at("/Chude")));
					rows123.add(hItem);
				}
			}
			
		}
		req.setAttribute("_listCD", rows123);
		req.setAttribute("Check_", 0);
		return "/hdsd/hdsduser";			
	}

	
	

	@RequestMapping(value = "/search/{token}", method = { RequestMethod.POST, RequestMethod.GET })
	public String search(Locale locale,  HttpSession session,Principal principal, HttpServletRequest req, @PathVariable(value = "token") String token) throws Exception{
		BaseDTO dto = new BaseDTO();
		dto.setErrorCode(0);
		HashMap<String, String> hInput = null;
		try {
			hInput = Json.serializer().fromJson(commons.decodeBase64ToString(token), new TypeReference<HashMap<String, String>>() {
			});
		}catch(Exception e) {
		
		}
		
		BaseDTO baseDTO = new BaseDTO();
		CurrentUserProfile cup = getCurrentlyAuthenticatedPrincipal();		
		baseDTO = new BaseDTO(req);
		Msg msg = baseDTO.createMsg(cup, Constants.MSG_ACTION_CODE.SEARCH);
		HashMap<String, Object> hData = new HashMap<>();
		
		
		String chude = "";
		String _id = "";
		if(hInput == null) {
			_id = token;
			
		}else {
		chude = hInput.get("find");
		}
		
		if(_id.equals("")) {
		hData.put("Name", chude);
		msg.setObjData(hData);
		List<HashMap<String, Object>> rows123 = null;
		JSONRoot root = new JSONRoot(msg);
		MsgRsp rsp = restAPI.callAPINormal("/hdsduser/list", cup.getLoginRes().getToken(), HttpMethod.POST, root);
		MspResponseStatus rspStatus = rsp.getResponseStatus();
		if(rspStatus.getErrorCode() == 0) {
			HashMap<String, Object> hItem = null;
			rows123 = new ArrayList<HashMap<String,Object>>();
			JsonNode jsonData = Json.serializer().nodeFromObject(rsp.getObjData());
			JsonNode rows = null;
			String tieude  ="";
			String file  ="";
			String Content  ="";
			if(!jsonData.at("/rows").isMissingNode()) {
				rows = jsonData.at("/rows");
				for(JsonNode row: rows) {
					_id =  commons.getTextJsonNode(row.at("/_id"));
					chude = commons.getTextJsonNode(row.at("/Chude"));
					tieude =  commons.getTextJsonNode(row.at("/Tieude"));
					file =  commons.getTextJsonNode(row.at("/File"));
					Content = commons.decodeURIComponent(commons.getTextJsonNode(row.at("/Noidung")));
					
					hItem = new HashMap<String, Object>();
					hItem.put("_id", commons.getTextJsonNode(row.at("/_id")));
					hItem.put("Chude", commons.getTextJsonNode(row.at("/Chude")));
					rows123.add(hItem);
				}
			}

			req.setAttribute("List_Search", rows123);
			req.setAttribute("Check_", 0);
			
		}
		}
		else
		{
			msg.setObjData(hData);
			List<HashMap<String, Object>> rows123 = null;
			JSONRoot root = new JSONRoot(msg);
			MsgRsp rsp = restAPI.callAPINormal("/hdsduser/detail/" + _id, cup.getLoginRes().getToken(), HttpMethod.POST, root);
			MspResponseStatus rspStatus = rsp.getResponseStatus();
			if(rspStatus.getErrorCode() == 0) {
				JsonNode jsonData = Json.serializer().nodeFromObject(rsp.getObjData());
				req.setAttribute("File", commons.getTextJsonNode(jsonData.at("/File")));
				req.setAttribute("Tieude", commons.getTextJsonNode(jsonData.at("/Tieude")));
				req.setAttribute("Content", commons.decodeURIComponent(commons.getTextJsonNode(jsonData.at("/Noidung"))));
				
				rows123 = new ArrayList<HashMap<String,Object>>();
				HashMap<String, Object> hItem = null;
				hItem = new HashMap<String, Object>();
				hItem.put("_id",_id);
				hItem.put("Chude", commons.getTextJsonNode(jsonData.at("/Chude")));
				rows123.add(hItem);
				req.setAttribute("List_Search", rows123);
			}			
		}
		return "/hdsd/hdsdusersearch";			
	}
	
	
	
	
	@RequestMapping(value = "/{_id}", method = { RequestMethod.POST, RequestMethod.GET })
	public String id(Locale locale,  HttpSession session,Principal principal, HttpServletRequest req, @PathVariable(name = "_id", required = false) String _id) throws Exception {
		BaseDTO dto = new BaseDTO();
		dto.setErrorCode(0);
		
		BaseDTO baseDTO =new BaseDTO();
		CurrentUserProfile cup = getCurrentlyAuthenticatedPrincipal();		
		baseDTO = new BaseDTO(req);
		Msg msg = baseDTO.createMsg(cup, Constants.MSG_ACTION_CODE.SEARCH);
		
		HashMap<String, Object> hData = new HashMap<>();
		
		msg.setObjData(hData);
		List<HashMap<String, Object>> rows123 = null;
		JSONRoot root = new JSONRoot(msg);
		MsgRsp rsp = restAPI.callAPINormal("/hdsduser/list", cup.getLoginRes().getToken(), HttpMethod.POST, root);
		MspResponseStatus rspStatus = rsp.getResponseStatus();
		if(rspStatus.getErrorCode() == 0) {
			rows123 = new ArrayList<HashMap<String,Object>>();
			JsonNode jsonData = Json.serializer().nodeFromObject(rsp.getObjData());
			JsonNode rows = null;
			HashMap<String, Object> hItem = null;
			if(!jsonData.at("/rows").isMissingNode()) {
				rows = jsonData.at("/rows");
				for(JsonNode row: rows) {
					hItem = new HashMap<String, Object>();
					hItem.put("_id", commons.getTextJsonNode(row.at("/_id")));
					hItem.put("Chude", commons.getTextJsonNode(row.at("/Chude")));
					rows123.add(hItem);
				}
			}
			
		}
		req.setAttribute("_listCD", rows123);
		
		
		 rsp = restAPI.callAPINormal("/hdsduser/detail/" + _id, cup.getLoginRes().getToken(), HttpMethod.POST, root);
		  rspStatus = rsp.getResponseStatus();
			if(rspStatus.getErrorCode() == 0) {
				JsonNode jsonData = Json.serializer().nodeFromObject(rsp.getObjData());
				req.setAttribute("File", commons.getTextJsonNode(jsonData.at("/File")));
				req.setAttribute("Tieude", commons.getTextJsonNode(jsonData.at("/Tieude")));
				req.setAttribute("Content", commons.decodeURIComponent(commons.getTextJsonNode(jsonData.at("/Noidung"))));

			}
		
		
		return "/hdsd/hdsduser";			
	}
	

	
	@RequestMapping(value = {"/dowloadfile/{token}"}, method = {RequestMethod.GET})
	public void viewPDF(Model model, HttpServletRequest req, HttpServletResponse resp
			, @PathVariable(value = "token") String token) throws Exception{
		PrintWriter writer = null;
		CurrentUserProfile cup = getCurrentlyAuthenticatedPrincipal();
		HashMap<String, String> hInput = null;
		try {
			hInput = Json.serializer().fromJson(commons.decodeBase64ToString(token), new TypeReference<HashMap<String, String>>() {
			});
		}catch(Exception e) {
		
		}
		String name = hInput.get("name");
		BaseDTO dto = new BaseDTO();
		dto = new BaseDTO(req);
		Msg msg = dto.createMsg(cup, Constants.MSG_ACTION_CODE.CREATED);
		HashMap<String, Object> hData = new HashMap<>();
		hData.put("name", name);
		msg.setObjData(hData);
		JSONRoot root = new JSONRoot(msg);
	
		

		
		
		FileInfo fileInfo = restAPI.callAPIGetFileInfo("/hdsduser/dowloadfile", cup.getLoginRes().getToken(), HttpMethod.POST, root);
		if(null == fileInfo || null == fileInfo.getContentFile()) {
			resp.setContentType("text/html; charset=utf-8");
	        resp.setCharacterEncoding("UTF-8");
	        resp.setHeader("success", "yes");
	        writer = resp.getWriter();
	        writer.write("Tải file không thành công.");
	        writer.flush();
	        writer.close();
	        return;
		}
		
		
		String fileName = "file.pdf";
		String type = "application/pdf";
			fileName = "file.xml";
			type = "application/octet-stream";
		InputStream inputStream = new ByteArrayInputStream(fileInfo.getContentFile());
		resp.setHeader("Content-Type", type);
		
		resp.setHeader("Cache-Control", "no-cache, no-store, must-revalidate"); // HTTP 1.1.
		resp.setHeader("Pragma", "no-cache"); // HTTP 1.0.
		resp.setHeader("Expires", "0"); // Proxies.
		resp.setHeader("Content-Disposition", "inline; filename=" + URLEncoder.encode(null == fileInfo.getFileName()? fileName: fileInfo.getFileName(), "UTF-8").replaceAll("\\+", "%20"));
		
		int bufferSize = 1024;
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
	}
}
