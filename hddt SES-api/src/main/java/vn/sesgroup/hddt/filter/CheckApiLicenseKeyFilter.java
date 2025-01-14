package vn.sesgroup.hddt.filter;

import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.web.filter.OncePerRequestFilter;

import vn.sesgroup.hddt.exception.ApiErrorResponse;
import vn.sesgroup.hddt.utility.Constants;
import vn.sesgroup.hddt.utility.Json;
import vn.sesgroup.hddt.utility.SystemParams;

public class CheckApiLicenseKeyFilter extends OncePerRequestFilter{

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {
		String apiLicenseKey = null == request.getHeader(Constants.REQ_API_LICENSE_KEY_NAME)? "": request.getHeader(Constants.REQ_API_LICENSE_KEY_NAME);
		if(!HttpMethod.GET.toString().equals(request.getMethod()) && !SystemParams.LIST_APILICENSEKEY.contains(apiLicenseKey)) {
			ApiErrorResponse apiResponse = new ApiErrorResponse
		            .ApiErrorResponseBuilder()
		            .withDetail("API License key không hợp lệ. Vui lòng liên hệ Admin để biết thêm chi tiết.")
		            .withMessage("Invalid api license key.")
		            .withErrorCode(String.valueOf(HttpStatus.FORBIDDEN.value()))
		            .withStatus(HttpStatus.FORBIDDEN)
		            .atTime(LocalDateTime.now(ZoneOffset.UTC))
		            .build();
			
			response.setStatus(HttpStatus.FORBIDDEN.value());
			response.setContentType("application/json; charset=UTF-8");
			response.setCharacterEncoding("UTF-8");
			response.setHeader("Cache-Control", "no-cache");
			PrintWriter pw = response.getWriter();
			pw.print(Json.serializer().toString(apiResponse));
			pw.flush();
			pw.close();
			return;
		}
		
		filterChain.doFilter(request, response);
	}

}
