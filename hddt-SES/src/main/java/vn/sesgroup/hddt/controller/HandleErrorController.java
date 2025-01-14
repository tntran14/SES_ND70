package vn.sesgroup.hddt.controller;

import java.text.MessageFormat;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.logging.log4j.core.util.Throwables;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.context.request.WebRequest;

import vn.sesgroup.hddt.utils.Constants;

@Controller
@RequestMapping("/")
public class HandleErrorController implements ErrorController {
//	private final ErrorAttributes errorAttributes;
//
//	public HandleErrorController(ErrorAttributes errorAttributes) {
//		this.errorAttributes = errorAttributes;
//	}

	@RequestMapping("/error")
	public String error(WebRequest webRequest, HttpServletRequest req, HttpServletResponse resp) throws Exception {
		resp.setStatus(HttpStatus.OK.value());
		Integer statusCode = -1;
		Throwable throwable = null;

		if (req.getAttribute("javax.servlet.error.status_code") instanceof Integer)
			statusCode = (Integer) req.getAttribute("javax.servlet.error.status_code");
		if (req.getAttribute("javax.servlet.error.exception") != null
				&& req.getAttribute("javax.servlet.error.exception") instanceof Throwable)
			throwable = (Throwable) req.getAttribute("javax.servlet.error.exception");

		String exceptionMessage = getExceptionMessage(throwable, statusCode);

		String requestUri = (String) req.getAttribute("javax.servlet.error.request_uri");
		if (requestUri == null) {
			requestUri = "Unknown";
		}

		String message = MessageFormat.format("{0} returned for {1} with message {2}", statusCode, requestUri,
				exceptionMessage);
		if (statusCode == HttpStatus.NOT_FOUND.value()) {
			message = statusCode + " - Not Found...";
		} else if (statusCode == HttpStatus.METHOD_NOT_ALLOWED.value()) {
			message = statusCode + " - " + exceptionMessage;
		} else {
			message = statusCode + " - Lỗi xử lý dữ liệu...";
		}
		String RESULT_TYPE = (null == req.getHeader(Constants.HEADER_RESULT_TYPE_NAME) ? ""
				: req.getHeader(Constants.HEADER_RESULT_TYPE_NAME));
		resp.setStatus(HttpStatus.OK.value());

		switch (RESULT_TYPE) {
		case Constants.HEADER_RESULT_TYPES.PAGE_POPUP:
			req.setAttribute("statusCode", statusCode);
			req.setAttribute("headerError", statusCode);
			req.setAttribute("errorMessage", message);
			return "/error/error-popup";
		case Constants.HEADER_RESULT_TYPES.JSON:
			req.setAttribute("statusCode", statusCode);
			req.setAttribute("headerError", statusCode);
			req.setAttribute("errorMessage", message);
			return "forward:/errorJSON";
		case Constants.HEADER_RESULT_TYPES.PAGE_AREA:
			req.setAttribute("typeError", Constants.HEADER_RESULT_TYPES.PAGE_AREA);
			req.setAttribute("statusCode", statusCode);
			req.setAttribute("headerError", statusCode);
			req.setAttribute("errorMessage", message);
			return "/error/error-area";
		default:
			req.setAttribute("typeError", "FLAT_FORM");
			req.setAttribute("statusCode", statusCode);
			req.setAttribute("headerError", statusCode);
			req.setAttribute("errorMessage", message);
			return "/error/error-flat-form";
		}
	}

	private String getExceptionMessage(Throwable throwable, Integer statusCode) {
		if (throwable != null) {
			return Throwables.getRootCause(throwable).getMessage();
		}
		HttpStatus httpStatus = HttpStatus.valueOf(statusCode);
		return httpStatus.getReasonPhrase();
	}
}
