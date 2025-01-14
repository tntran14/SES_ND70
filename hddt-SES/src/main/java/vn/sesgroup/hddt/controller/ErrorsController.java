package vn.sesgroup.hddt.controller;

import java.util.Locale;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.context.WebApplicationContext;

import vn.sesgroup.hddt.dto.BaseDTO;

@Controller
@RequestMapping(value = {"/"})
@Scope(value = WebApplicationContext.SCOPE_REQUEST)
public class ErrorsController {
	
	@RequestMapping(value = "/{path-error}")
	public String errorPopup(Locale locale
			, @PathVariable(name = "path-error", required = true, value = "") String pathError
			, HttpServletRequest req, HttpSession session) throws Exception{
		return "/error/" + pathError;
	}
	
	@RequestMapping(value = "/errorJSON")
	@ResponseBody
	public BaseDTO errorJSON(Locale locale, HttpServletRequest req, HttpSession session) throws Exception{
		BaseDTO errDTO = new BaseDTO(1, null == req.getAttribute("errorMessage")? "Lỗi trong quá trình xử lý...": req.getAttribute("errorMessage"));
		return errDTO;
	}
	
	
}
