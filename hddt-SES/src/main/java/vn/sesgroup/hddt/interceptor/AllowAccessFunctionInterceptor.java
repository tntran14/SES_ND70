package vn.sesgroup.hddt.interceptor;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;

import javax.servlet.RequestDispatcher;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import vn.sesgroup.hddt.dto.BaseDTO;
import vn.sesgroup.hddt.dto.CurrentUserProfile;
import vn.sesgroup.hddt.dto.JsonGridDTO;
import vn.sesgroup.hddt.utils.Constants;
import vn.sesgroup.hddt.utils.SendMsgUtil;

@Component
public class AllowAccessFunctionInterceptor implements HandlerInterceptor{

	@Override
	public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
			throws Exception {
		int codeError = 403;
		String headerError = "FORBIDDEN";
		
		CurrentUserProfile cu = null;
		if(null != SecurityContextHolder.getContext().getAuthentication() 
				&& SecurityContextHolder.getContext().getAuthentication().isAuthenticated()
				&& !(SecurityContextHolder.getContext().getAuthentication() instanceof AnonymousAuthenticationToken)) {
			if(SecurityContextHolder.getContext().getAuthentication().getPrincipal() instanceof CurrentUserProfile) {
				cu = (CurrentUserProfile) SecurityContextHolder.getContext().getAuthentication().getPrincipal();	
			}
		}
		BaseDTO baseDTO = null;
		RequestDispatcher dispatcher = null;
		
		String checkAllow = null == request.getAttribute(Constants.HAVE_ACTION_NAME)? "": request.getAttribute(Constants.HAVE_ACTION_NAME).toString();
		String RESULT_TYPE = (null == request.getHeader(Constants.HEADER_RESULT_TYPE_NAME) ? "" : request.getHeader(Constants.HEADER_RESULT_TYPE_NAME));
		if(!"OK".equals(checkAllow)) {
			String messageError = MessageFormat.format("You don’t have permission to access {0} on this server", request.getRequestURI());
			messageError = "Người dùng không được phép truy cập chức năng theo cách này.";
			request.setAttribute("isAllow", "NOT-ALLOW");
			switch (RESULT_TYPE) {
			case Constants.HEADER_RESULT_TYPES.PAGE_POPUP:
				request.setAttribute("statusCode", headerError);
				request.setAttribute("headerError", headerError);
				request.setAttribute("errorMessage", messageError);
				dispatcher = request.getRequestDispatcher("/error-popup-admin");
	            dispatcher.forward(request, response);
				return false;
			case Constants.HEADER_RESULT_TYPES.PAGE_AREA:
				request.setAttribute("typeError", Constants.HEADER_RESULT_TYPES.PAGE_AREA);
				request.setAttribute("statusCode", codeError);
				request.setAttribute("errorMessage", messageError);
				if("tab".equals(request.getParameter("layout"))) {
					dispatcher = request.getRequestDispatcher("/error-admin-tab-area");
				}else {
					dispatcher = request.getRequestDispatcher("/error-admin-area");	
				}
				dispatcher.forward(request, response);
				return false;
			case Constants.HEADER_RESULT_TYPES.JSON_GRID:
				JsonGridDTO grid = new JsonGridDTO();
				grid.setErrorCode(codeError);
				grid.setResponseData(messageError);
				SendMsgUtil.sendJsonMessage(response, grid);
				return false;
			case Constants.HEADER_RESULT_TYPES.JSON:	
				baseDTO = new BaseDTO();
				baseDTO.setErrorCode(1);
				baseDTO.setResponseData(messageError);
				SendMsgUtil.sendJsonMessage(response, baseDTO);
				return false;
			case Constants.HEADER_RESULT_TYPES.JSON_ARRAY:
				ArrayList<HashMap<String, String>> arrayResult = new ArrayList<>();
				SendMsgUtil.sendJsonMessage(response, arrayResult);
				return false;
			default:
				request.setAttribute("typeError", "FLAT_FORM");
				request.setAttribute("statusCode", codeError);
				request.setAttribute("errorMessage", messageError);
				dispatcher = request.getRequestDispatcher("/error-flat-form");
				dispatcher.forward(request, response);
				return false;
			}
		}
		
		return HandlerInterceptor.super.preHandle(request, response, handler);
	}

	@Override
	public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler,
			ModelAndView modelAndView) throws Exception {
		// TODO Auto-generated method stub
		HandlerInterceptor.super.postHandle(request, response, handler, modelAndView);
	}

	@Override
	public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex)
			throws Exception {
		// TODO Auto-generated method stub
		HandlerInterceptor.super.afterCompletion(request, response, handler, ex);
	}
	
}
