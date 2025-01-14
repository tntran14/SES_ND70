package vn.sesgroup.hddt.controller.user;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bson.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.context.WebApplicationContext;

import com.api.message.JSONRoot;
import com.api.message.Msg;
import com.api.message.MsgRsp;
import com.api.message.MspResponseStatus;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;

import vn.sesgroup.hddt.controller.AbstractController;
import vn.sesgroup.hddt.dto.BaseDTO;
import vn.sesgroup.hddt.dto.FileInfo;
import vn.sesgroup.hddt.resources.RestAPIUtility;
import vn.sesgroup.hddt.utils.Commons;
import vn.sesgroup.hddt.utils.Constants;
import vn.sesgroup.hddt.utils.Json;

@Controller
@Scope(value = WebApplicationContext.SCOPE_REQUEST)
@RequestMapping("/")
public class TraCuuHDController extends AbstractController{
	@Autowired RestAPIUtility restAPI;
	private static final Logger log = LogManager.getLogger(TraCuuHDController.class);
	private Commons commons = new Commons();
	
	@RequestMapping(value = {"/tracuuhd"}, method = {RequestMethod.GET})
	public String tracuuhd(Model model, HttpServletRequest request) throws Exception {
		request.setAttribute("_header", Constants.PREFIX_TITLE + " - Tra cứu hóa đơn");
		BaseDTO dtoRes = new BaseDTO();
		Msg msg = dtoRes.createMsgPass();
		HashMap<String, String> hInput = new HashMap<>();
		HashMap<String, Object> hTmp = null;
		List<HashMap<String, Object>> rowsL = null;
		Document r10 = null;
		msg.setObjData(hInput);
		JSONRoot root = new JSONRoot(msg);
		MsgRsp rsp = restAPI.callAPIPass("/forgotpass/dl", HttpMethod.POST, root);
		MspResponseStatus rspStatus = rsp.getResponseStatus();
		JsonNode rows = null;
		if(rspStatus.getErrorCode() == 0) {
			JsonNode jsonData = Json.serializer().nodeFromObject(rsp.getObjData());
			if(!jsonData.at("/rows").isMissingNode()) {
				rows = jsonData.at("/rows");
			for(JsonNode row: rows) {
				request.setAttribute("LOGO", commons.getTextJsonNode(row.at("/LOGO")));
				request.setAttribute("PHONE", commons.getTextJsonNode(row.at("/PHONE")));
				request.setAttribute("EMAIL", commons.getTextJsonNode(row.at("/EMAIL")));
			}
			}
		}
		 dtoRes = new BaseDTO(request);
		 msg = dtoRes.createMsgPass();
		 hInput = new HashMap<>();
		msg.setObjData(hInput);
		 root = new JSONRoot(msg);
		 rsp = restAPI.callAPIPass("/forgotpass/left", HttpMethod.POST, root);
		 rspStatus = rsp.getResponseStatus();
		 rows = null;
		
		
		if(rspStatus.getErrorCode() == 0) {
			JsonNode jsonData = Json.serializer().nodeFromObject(rsp.getObjData());
			if(!jsonData.at("/rows").isMissingNode()) {
				rows = jsonData.at("/rows");
			rowsL = new ArrayList<HashMap<String,Object>>();
			for(JsonNode row: rows) {
			hTmp = new HashMap<String, Object>();
			hTmp.put("Chude", commons.getTextJsonNode(row.at("/Chude")));
			hTmp.put("Tieude", commons.getTextJsonNode(row.at("/Tieude")));
			hTmp.put("Link", commons.getTextJsonNode(row.at("/Link")));
			hTmp.put("Noidung", commons.getTextJsonNode(row.at("/Noidung")));
			hTmp.put("_id", commons.getTextJsonNode(row.at("/_id")));
			rowsL.add(hTmp);
			}
			request.setAttribute("TinWebL", rowsL);
			}
		}
		
		
		 dtoRes = new BaseDTO(request);
		 msg = dtoRes.createMsgPass();
		 hInput = new HashMap<>();
		msg.setObjData(hInput);
		 root = new JSONRoot(msg);
		 rsp = restAPI.callAPIPass("/forgotpass/right", HttpMethod.POST, root);
		 rspStatus = rsp.getResponseStatus();
		 rows = null;
		if(rspStatus.getErrorCode() == 0) {
			JsonNode jsonData = Json.serializer().nodeFromObject(rsp.getObjData());
			if(!jsonData.at("/rows").isMissingNode()) {
				rows = jsonData.at("/rows");
			rowsL = new ArrayList<HashMap<String,Object>>();
			for(JsonNode row: rows) {
			hTmp = new HashMap<String, Object>();
			hTmp.put("Chude", commons.getTextJsonNode(row.at("/Chude")));
			hTmp.put("Tieude", commons.getTextJsonNode(row.at("/Tieude")));
			hTmp.put("Noidung", commons.getTextJsonNode(row.at("/Noidung")));
			request.setAttribute("Content",commons.getTextJsonNode(row.at("/Noidung")));
			hTmp.put("_id", commons.getTextJsonNode(row.at("/_id")));
			rowsL.add(hTmp);
			}
			request.setAttribute("TinWebR", rowsL);
			}
		}
		return "user/tracuuhd";
	}
	
	@RequestMapping(value = {"/tracuuhd/{token}"}, method = {RequestMethod.GET})
	public void viewPDF(Model model, HttpServletRequest req, HttpServletResponse resp
			, @PathVariable(value = "token") String token) throws Exception{
		PrintWriter writer = null;
		
		HashMap<String, String> hInput = null;
		try {
			hInput = Json.serializer().fromJson(commons.decodeBase64ToString(token), new TypeReference<HashMap<String, String>>() {
			});
		}catch(Exception e) {
			log.error(">>>>> An exception occurred!", e);
		}
		
		if(hInput == null) {
			resp.setContentType("text/html; charset=utf-8");
	        resp.setCharacterEncoding("UTF-8");
	        resp.setHeader("success", "yes");
	        writer = resp.getWriter();
	        writer.write("Dữ liệu xem hóa đơn không hợp lệ.");
	        writer.flush();
	        writer.close();
	        return;
		}
		
		FileInfo fileInfo = restAPI.callAPITracuuHD("/tracuuhd/print-einvoice", HttpMethod.POST, hInput);
		if(null == fileInfo || null == fileInfo.getContentFile()) {
			resp.setContentType("text/html; charset=utf-8");
	        resp.setCharacterEncoding("UTF-8");
	        resp.setHeader("success", "yes");
	        writer = resp.getWriter();
	        writer.write("In hóa đơn không thành công.");
	        writer.flush();
	        writer.close();
	        return;
		}
		
		
		String fileName = "Hoa-Don.pdf";
		String type = "application/pdf";
		if("download-xml".equals(hInput.get("action"))) {
			fileName = "Hoa-Don.xml";
			type = "application/octet-stream";
		}
		
		InputStream inputStream = new ByteArrayInputStream(fileInfo.getContentFile());
		resp.setHeader("Content-Type", type);
		
		resp.setHeader("Cache-Control", "no-cache, no-store, must-revalidate"); // HTTP 1.1.
		resp.setHeader("Pragma", "no-cache"); // HTTP 1.0.
		resp.setHeader("Expires", "0"); // Proxies.
		resp.setHeader("Content-Disposition", "inline; filename=" + URLEncoder.encode(null == fileInfo.getFileName()? fileName: fileInfo.getFileName(), "UTF-8").replaceAll("\\+", "%20"));
		
		int bufferSize = 1024;
//		String mimetype = "application/pdf";
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
        
//		resp.setContentType("text/html; charset=utf-8");
//        resp.setCharacterEncoding("UTF-8");
//        resp.setHeader("success", "yes");
//        writer = resp.getWriter();
//        writer.write("Không tìm thấy thông tin hóa đơn." + token);
//        writer.flush();
//        writer.close();
//        return;
	}
}
