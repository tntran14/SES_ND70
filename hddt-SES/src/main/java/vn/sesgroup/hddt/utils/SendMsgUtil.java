package vn.sesgroup.hddt.utils;

import java.io.PrintWriter;

import javax.servlet.http.HttpServletResponse;

import org.springframework.http.HttpStatus;

import com.fasterxml.jackson.databind.ObjectMapper;

public class SendMsgUtil {
	public static void sendMessage(HttpServletResponse response, String str) throws Exception {
        response.setContentType("text/html; charset=utf-8");
        PrintWriter writer = response.getWriter();
        writer.print(str);
        writer.close();
        response.flushBuffer(); 
    }

    public static void sendJsonMessage(HttpServletResponse response, Object obj) throws Exception {
    	ObjectMapper mapper = new ObjectMapper();    	
        response.setContentType("application/json; charset=utf-8");
		response.setCharacterEncoding("UTF-8");
		response.setHeader("Cache-Control", "no-cache");
		response.setStatus(HttpStatus.OK.value());
        PrintWriter writer = response.getWriter();
        writer.print(mapper.writeValueAsString(obj));
        writer.close();
        response.flushBuffer();
    }
}
