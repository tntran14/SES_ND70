package vn.sesgroup.hddt.exception;


import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.access.AccessDeniedHandler;

import vn.sesgroup.hddt.dto.BaseDTO;
import vn.sesgroup.hddt.dto.CurrentUserProfile;
import vn.sesgroup.hddt.dto.JsonGridDTO;
import vn.sesgroup.hddt.utils.Constants;
import vn.sesgroup.hddt.utils.SendMsgUtil;

public class MyAccessDeniedHandler implements AccessDeniedHandler{

	@Override
	public void handle(HttpServletRequest request, HttpServletResponse response,
			AccessDeniedException accessDeniedException) throws IOException, ServletException {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		boolean isLogged = null != authentication && authentication.getPrincipal() instanceof UserDetails;
		
		String RESULT_TYPE = (request.getHeader(Constants.HEADER_RESULT_TYPE_NAME) == null ? "" : request.getHeader(Constants.HEADER_RESULT_TYPE_NAME));
		
		int codeError = 401;
		String headerError = "Unauthorized";
		String messageError = "The request has not been applied because it lacks valid authentication credentials for the target resource.";
		messageError = "Phiên làm việc của bạn đã hết. Vui lòng đăng nhập lại để tiếp tục...";
//		String message = "";
		
		RequestDispatcher dispatcher = null;
		if(isLogged == false) {
			switch (RESULT_TYPE) {
			case Constants.HEADER_RESULT_TYPES.JSON_GRID:
				JsonGridDTO grid = new JsonGridDTO();
				grid.setErrorCode(codeError);
				grid.setResponseData(messageError);
				try {
					SendMsgUtil.sendJsonMessage(response, grid);
				} catch (Exception e) {
					e.printStackTrace();
				}
				break;
			case Constants.HEADER_RESULT_TYPES.PAGE_AREA:
				request.setAttribute("_IsRelogin", "Y");
				request.setAttribute("typeError", Constants.HEADER_RESULT_TYPES.PAGE_AREA);
				request.setAttribute("statusCode", codeError + " - " + headerError);
				request.setAttribute("errorMessage", messageError);
				if("tab".equals(request.getParameter("layout"))) {
					dispatcher = request.getRequestDispatcher("/error-user-tab-area");
				}else {
					dispatcher = request.getRequestDispatcher("/error-area");	
				}
				dispatcher.forward(request, response);
				break;
			case Constants.HEADER_RESULT_TYPES.JSON:	
				BaseDTO baseDTO = new BaseDTO();
				baseDTO.setErrorCode(codeError);
				baseDTO.setResponseData(messageError);
				try {
					SendMsgUtil.sendJsonMessage(response, baseDTO);	
				}catch(Exception e) {
				}
				return;
			case Constants.HEADER_RESULT_TYPES.PAGE_POPUP:
				request.setAttribute("_IsRelogin", "Y");
				request.setAttribute("typeError", Constants.HEADER_RESULT_TYPES.PAGE_POPUP);
				request.setAttribute("statusCode", codeError + " - " + headerError);
				request.setAttribute("errorMessage", messageError);
				request.getRequestDispatcher("/error-popup").forward(request, response);
				return;
			case Constants.HEADER_RESULT_TYPES.JSON_ARRAY:
				ArrayList<HashMap<String, String>> arrayResult = new ArrayList<>();
				try {
					SendMsgUtil.sendJsonMessage(response, arrayResult);	
				}catch(Exception e) {
				}
				return;
			default:
				response.sendRedirect(request.getContextPath() + "/login");
				break;
			}
		}else {
			CurrentUserProfile cu = null;
			boolean isAdmin = false;		
			if(null != SecurityContextHolder.getContext().getAuthentication() 
					&& SecurityContextHolder.getContext().getAuthentication().isAuthenticated()
					&& !(SecurityContextHolder.getContext().getAuthentication() instanceof AnonymousAuthenticationToken)) {
				if(SecurityContextHolder.getContext().getAuthentication().getPrincipal() instanceof CurrentUserProfile)
					cu = (CurrentUserProfile) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
			}
			
			if(null != cu) {
				isAdmin = cu.isAdmin();
			}
			
			//DUNG LUON CHO NAY
			isAdmin = true;
			
			codeError = 403;
			headerError = "FORBIDDEN";
//			messageError = "The server understood the request but refuses to authorize it";
			messageError = "TOKEN GIAO DỊCH KHÔNG HỢP LỆ.";
			switch (RESULT_TYPE) {
			case Constants.HEADER_RESULT_TYPES.JSON_GRID:
				JsonGridDTO grid = new JsonGridDTO();
				grid.setErrorCode(codeError);
				grid.setResponseData(messageError);
				try {
					SendMsgUtil.sendJsonMessage(response, grid);
				} catch (Exception e) {
					e.printStackTrace();
				}
				break;
			case Constants.HEADER_RESULT_TYPES.PAGE_AREA:
				request.setAttribute("typeError", Constants.HEADER_RESULT_TYPES.PAGE_AREA);
				request.setAttribute("statusCode", codeError + " - " + headerError);
				request.setAttribute("errorMessage", messageError);
				if("tab".equals(request.getParameter("layout"))) {
					dispatcher = request.getRequestDispatcher("/error-user-tab-area");
				}else {
					dispatcher = request.getRequestDispatcher("/error-area");	
				}
				dispatcher.forward(request, response);
//				request.getRequestDispatcher("/forbidden").forward(request, response);
				break;
			case Constants.HEADER_RESULT_TYPES.JSON:	
				BaseDTO baseDTO = new BaseDTO();
				baseDTO.setErrorCode(1);
				baseDTO.setResponseData(messageError);
				try {
					SendMsgUtil.sendJsonMessage(response, baseDTO);	
				}catch(Exception e) {
//					log.error(request.getRemoteAddr() + " >>>>> An exception occurred!", e);
				}
				return;
			case Constants.HEADER_RESULT_TYPES.PAGE_POPUP:
				request.setAttribute("typeError", Constants.HEADER_RESULT_TYPES.PAGE_POPUP);
				request.setAttribute("statusCode", codeError + " - " + headerError);
				request.setAttribute("errorMessage", messageError);
				request.getRequestDispatcher("/error-popup").forward(request, response);
				return;
			case Constants.HEADER_RESULT_TYPES.JSON_ARRAY:
				ArrayList<HashMap<String, String>> arrayResult = new ArrayList<>();
				try {
					SendMsgUtil.sendJsonMessage(response, arrayResult);	
				}catch(Exception e) {
//					log.error(request.getRemoteAddr() + " >>>>> An exception occurred!", e);
				}
				return;
			default:
//				message = MessageFormat.format("{0}: {1} with message {2}", codeError, messageError);
//				request.setAttribute("statusCode", codeError + " - " + headerError);
//				request.setAttribute("errorMessage", messageError);
//				request.getRequestDispatcher(isAdmin? "/error-general-admin": "/error-general-user").forward(request, response);
				response.sendRedirect(request.getContextPath() + "/login");
				return;
			}
		}
	}

}