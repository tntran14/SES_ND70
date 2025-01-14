package vn.sesgroup.hddt.interceptor;

import java.util.ArrayList;
import java.util.HashMap;

import javax.servlet.RequestDispatcher;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.servlet.ModelAndView;

import vn.sesgroup.hddt.dto.BaseDTO;
import vn.sesgroup.hddt.dto.CurrentUserProfile;
import vn.sesgroup.hddt.dto.JsonGridDTO;
import vn.sesgroup.hddt.utils.Constants;
import vn.sesgroup.hddt.utils.SendMsgUtil;

@Component
public class CheckRightsInterceptor implements HandlerInterceptor{
	private static final Logger log = LogManager.getLogger(CheckRightsInterceptor.class);
	
	@Override
	public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
			throws Exception {
		String uri = (String) request.getAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE);
		
		CurrentUserProfile cu = null;
		if(null != SecurityContextHolder.getContext().getAuthentication() 
				&& SecurityContextHolder.getContext().getAuthentication().isAuthenticated()
				&& !(SecurityContextHolder.getContext().getAuthentication() instanceof AnonymousAuthenticationToken)) {
			if(SecurityContextHolder.getContext().getAuthentication().getPrincipal() instanceof CurrentUserProfile)
				cu = (CurrentUserProfile) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
		}
		
		String transaction = "|" + (null == request.getAttribute("transaction")? "": request.getAttribute("transaction")) + "|";
		StringBuilder sbPathRight = new StringBuilder(cu.getAllRights() == null? "": cu.getAllRights());
		
		boolean isRight = sbPathRight.toString().contains(transaction);
		if(!isRight) {
			request.setAttribute("_isRight_", false);
			log.error(cu.getUsername() + " không có quyền thực hiện chức năng " + uri);
			
			String RESULT_TYPE = (null == request.getHeader(Constants.HEADER_RESULT_TYPE_NAME) ? "" : request.getHeader(Constants.HEADER_RESULT_TYPE_NAME));
			int codeError = 403;
			String headerError = "USER-NOT-RIGHT";
			String messageError = "NGƯỜI DÙNG KHÔNG CÓ QUYỀN THỰC HIỆN CHỨC NĂNG NÀY...";
			
			RequestDispatcher dispatcher = null;
			BaseDTO baseDTO = null;
			switch (RESULT_TYPE) {
			case Constants.HEADER_RESULT_TYPES.PAGE_POPUP:
				request.setAttribute("statusCode", headerError);
				request.setAttribute("headerError", headerError);
				request.setAttribute("errorMessage", messageError);
				dispatcher = request.getRequestDispatcher("/error-popup");
	            dispatcher.forward(request, response);
				return false;
			case Constants.HEADER_RESULT_TYPES.PAGE_AREA:
				request.setAttribute("typeError", Constants.HEADER_RESULT_TYPES.PAGE_AREA);
				request.setAttribute("statusCode", codeError);
				request.setAttribute("errorMessage", messageError);
				dispatcher = request.getRequestDispatcher("/error-area");
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
