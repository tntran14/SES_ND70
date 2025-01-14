package vn.sesgroup.hddt.controller.reportsituation;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.security.Principal;
import java.time.LocalDate;
import java.time.temporal.ChronoField;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import com.api.message.JSONRoot;
import com.api.message.Msg;
import com.api.message.MsgParam;
import com.api.message.MsgParams;
import com.api.message.MsgRsp;
import com.api.message.MspResponseStatus;
import com.fasterxml.jackson.core.type.TypeReference;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.context.WebApplicationContext;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.stream.IntStream;

import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;

import vn.sesgroup.hddt.controller.AbstractController;
import vn.sesgroup.hddt.controller.einvoice.EInvoicesController;
import vn.sesgroup.hddt.dto.BaseDTO;
import vn.sesgroup.hddt.dto.CurrentUserProfile;
import vn.sesgroup.hddt.dto.FileInfo;
import vn.sesgroup.hddt.resources.RestAPIUtility;
import vn.sesgroup.hddt.utils.ComboboxItem;
import vn.sesgroup.hddt.utils.Constants;
import vn.sesgroup.hddt.utils.Json;

@Controller
@RequestMapping("/report_situation")
@Scope(value = WebApplicationContext.SCOPE_REQUEST)
public class ReportSituationController extends AbstractController{
	private static final Logger log = LogManager.getLogger(EInvoicesController.class);
	@Autowired RestAPIUtility restAPI; 
	
	@SuppressWarnings("unused")
	private void loadCombobox(CurrentUserProfile cup, Locale locale, HttpServletRequest req, String action) {		
		try {
		List<ComboboxItem> comboboxItems = new ArrayList<>();
		comboboxItems.add(new ComboboxItem("", ""));
		IntStream.rangeClosed(1, 4).forEach(i -> {
			comboboxItems.add(new ComboboxItem("Q" + String.valueOf(i), "Quý " + i));	
		});
		IntStream.rangeClosed(1, 12).forEach(i -> {
			comboboxItems.add(new ComboboxItem("M" + String.valueOf(i), "Tháng " + i));	
		});
		req.setAttribute("PARAM_QUARTERMONTH", comboboxItems);
		
		List<ComboboxItem> years = new ArrayList<>();
		years.add(new ComboboxItem("", ""));
		int year = LocalDate.now().get(ChronoField.YEAR);
		IntStream.rangeClosed(year - 10, year).forEach(i -> {
			years.add(new ComboboxItem(String.valueOf(i), String.valueOf(i)));
		});
		req.setAttribute("PARAM_YEARS", years);
		req.setAttribute("year", year);		
		
		}catch(Exception e) {}
	}
	
	
	@RequestMapping(value = "/init", method = {RequestMethod.POST, RequestMethod.GET})
	  public String init(Locale locale, Principal principal,HttpServletRequest req) throws Exception {
	    req.setAttribute("_TitleView_", Constants.PREFIX_TITLE + " - Báo cáo tình hình sử dụng hóa đơn");
	    CurrentUserProfile cup = getCurrentlyAuthenticatedPrincipal();
	    LocalDate localDateNLap = LocalDate.now();
	    loadCombobox(cup, locale, req, null);
	    req.setAttribute("YEAR", StringUtils.leftPad(String.valueOf(localDateNLap.get(ChronoField.YEAR)), 4, "0"));
	    return "report_situation/report_situation";
	  }
	
	
	@RequestMapping(value = "/checkDataToExport",  produces = MediaType.APPLICATION_JSON_VALUE, method = RequestMethod.POST)
	@ResponseBody
	public BaseDTO execCheckDataToExport(Locale locale, HttpServletRequest req, HttpSession session) {
		BaseDTO dto = new BaseDTO();
		try {		
			List<String> quarterMonths = new ArrayList<>();
			IntStream.rangeClosed(1, 4).forEach(i -> {
				quarterMonths.add("Q" + i);	
			});
			IntStream.rangeClosed(1, 12).forEach(i -> {
				quarterMonths.add("M" + i);	
			});
			int yearCurrent = LocalDate.now().get(ChronoField.YEAR);
			
			String quarterMonth = null == req.getParameter("quarterMonth")? "": req.getParameter("quarterMonth").trim();
			String year = null == req.getParameter("year")? "": req.getParameter("year").trim();
			
			if("".equals(quarterMonth)) {
				dto.setErrorCode(1);
				dto.getErrorMessages().add("Vui lòng chọn Quý/Tháng xuất báo cáo.");
			
			}else if(quarterMonths.indexOf(quarterMonth) == -1) {
				dto.setErrorCode(1);
				dto.getErrorMessages().add("Quý/Tháng xuất báo cáo không hợp lệ.");
			
			}
			
			if("".equals(year) || !commons.checkStringIsInt(year) 
					|| commons.stringToInteger(year) < yearCurrent - 10 
					|| commons.stringToInteger(year) > yearCurrent + 10) {
				dto.setErrorCode(1);
				dto.getErrorMessages().add("Năm xuất báo cáo không hợp lệ.");
		
			}
			
				ObjectNode jsonData = null;
				jsonData = Json.serializer().mapper().createObjectNode();
				jsonData.put("QUARTER-MONTH", quarterMonth);
				jsonData.put("YEAR", year);				
				dto.setResponseData(commons.encodeStringBase64(jsonData.toString()));
				dto.setErrorCode(0);
		}catch(Exception e) {
			dto.setErrorCode(1);
			dto.getErrorMessages().add("Exception.");	
			return dto;
		}					
		return dto;
	}
	
	
	/////
	
	
	@SuppressWarnings("unused")
	@RequestMapping(value = {"/viewReportPdf/{idata1}", "/exportDataXml/{idata1}"},  method = RequestMethod.GET)
	public void viewReportPdf(Locale locale, HttpServletRequest req, HttpServletResponse resp, HttpSession session
			, @PathVariable(value = "idata1") String idata1) throws IOException {
		PrintWriter writer = null;
		try {
			
			
			String[] words = idata1.split("-");
	         String data = words[0];
	         String method =  words[1];
	
				boolean isXml = "exportDataXml".equals(method);
			ObjectNode objectNode = null;
			try {
				String decode = commons.decodeBase64ToString(data);
				objectNode = Json.serializer().fromJson(decode, new TypeReference<ObjectNode>() {
				});
			}catch(Exception e) {
				e.printStackTrace();
			}
			
			if(null == objectNode) {
				resp.setContentType("text/html; charset=utf-8");
		        resp.setCharacterEncoding("UTF-8");
		        resp.setHeader("success", "yes");
		        writer = resp.getWriter();
		        writer.write("Lỗi trong quá trình đọc dữ liệu báo cáo.");
		        writer.close();
				return;
			}
			
			String quarterMonth = null == objectNode.get("QUARTER-MONTH")? "": objectNode.get("QUARTER-MONTH").asText("");
			String year = null == objectNode.get("YEAR")? "": objectNode.get("YEAR").asText("");
			
			List<String> quarterMonths = new ArrayList<>();
			IntStream.rangeClosed(1, 4).forEach(i -> {
				quarterMonths.add("Q" + i);	
			});
			IntStream.rangeClosed(1, 12).forEach(i -> {
				quarterMonths.add("M" + i);	
			});
			int yearCurrent = LocalDate.now().get(ChronoField.YEAR);
			
			if("".equals(quarterMonth) 
					|| quarterMonths.indexOf(quarterMonth) == -1
					|| "".equals(year) || !commons.checkStringIsInt(year) 
					|| commons.stringToInteger(year) < yearCurrent - 10 
					|| commons.stringToInteger(year) > yearCurrent + 10) {
				resp.setContentType("text/html; charset=utf-8");
		        resp.setCharacterEncoding("UTF-8");
		        resp.setHeader("success", "yes");
		        writer = resp.getWriter();
		        writer.write("Dữ liệu xem báo cáo không hợp lệ.");
		        writer.close();
				return;
			}
			
			BaseDTO dto = new BaseDTO(req);
			CurrentUserProfile cup = getCurrentlyAuthenticatedPrincipal();
			Msg msg = dto.createMsg(cup, Constants.MSG_ACTION_CODE.REPORT);	
			HashMap<String, String> hDataDetail = new HashMap<>();
			
			hDataDetail.put("type", "ReportSituationUseInvoice");
			hDataDetail.put("quarterMonth", quarterMonth);
			hDataDetail.put("year", year);
			if(isXml) {
				hDataDetail.put("typeExport", "xml");	
			}else {
				hDataDetail.put("typeExport", "pdf");
			}
			msg.setObjData(hDataDetail);
			dto.setResponseData(hDataDetail);
			dto.setErrorCode(0);
			
			
			JSONRoot root = new JSONRoot(msg);
			FileInfo fileInfo = restAPI.callAPIGetFileInfo("/ReportSituaion/viewReport", cup.getLoginRes().getToken(), HttpMethod.POST, root);
			if(null == fileInfo || null == fileInfo.getContentFile()) {
				dto.setErrorCode(999);
				dto.setResponseData("Không tìm thấy dữ liệu hóa đơn.");		
				return;
			}
			if(fileInfo!=null) {
//				req.setAttribute("reportContent", common.byteArrayToString(res.getFileData()));
				String type = "application/pdf";
				if(isXml) {
					type = "application/xml";
					resp.setHeader("Content-Disposition", "attachment; filename=" + "BC26_AC.xml");
				}else {
					resp.setHeader("Content-Disposition", "inline; filename=" + "bc-tinh-hinh-su-dung-hoa-don.pdf");
				}
				InputStream inputStream = new ByteArrayInputStream(fileInfo.getContentFile());
				resp.setHeader("Content-Type", type);

				
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
		        writer.write("Tập tin không tồn tại.");
		        writer.close();
				return;
			}
		}catch(Exception e) {
			e.printStackTrace();
			resp.setContentType("text/html; charset=utf-8");
	        resp.setCharacterEncoding("UTF-8");
	        resp.setHeader("success", "yes");
	        writer = resp.getWriter();
	        writer.write("Lỗi trong quá trinh xem báo cáo. [EX]");
	        writer.close();
			return;
		}
	}
}
