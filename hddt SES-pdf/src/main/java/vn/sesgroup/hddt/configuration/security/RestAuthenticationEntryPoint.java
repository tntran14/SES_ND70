package vn.sesgroup.hddt.configuration.security;

import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;

import vn.sesgroup.hddt.exception.ApiErrorResponse;
import vn.sesgroup.hddt.utility.Json;

public class RestAuthenticationEntryPoint implements AuthenticationEntryPoint{
	@Override
	public void commence(HttpServletRequest request, HttpServletResponse response,
			AuthenticationException authException) throws IOException, ServletException {

		ApiErrorResponse apiResponse = new ApiErrorResponse
	            .ApiErrorResponseBuilder()
	            .withDetail("The request has not been applied because it lacks valid authentication credentials for the target resource.")
	            .withMessage("401 Unauthorized")
	            .withErrorCode(String.valueOf(HttpStatus.UNAUTHORIZED.value()))
	            .withStatus(HttpStatus.UNAUTHORIZED)
	            .atTime(LocalDateTime.now(ZoneOffset.UTC))
	            .build();
//		httpStatus = HttpStatus.METHOD_NOT_ALLOWED;
		response.setStatus(HttpStatus.UNAUTHORIZED.value());
		response.setContentType("application/json; charset=UTF-8");
		response.setCharacterEncoding("UTF-8");
		response.setHeader("Cache-Control", "no-cache");
		PrintWriter pw = response.getWriter();
		pw.print(Json.serializer().toString(apiResponse));
		pw.flush();
		pw.close();
		
//		response.sendError(HttpServletResponse.SC_UNAUTHORIZED, Json.serializer().toPrettyString(apiResponse));
		return;
	}
}
