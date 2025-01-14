package vn.sesgroup.hddt.controller;

//@Controller
//public class CustomizedErrorController implements ErrorController  {
//
//    @RequestMapping("/error")
//public String handleError(HttpServletRequest request) {
//    	return "/error/error-flat-form.html";
//    	
////    Object status = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);
//
////    if (status != null) {
////        Integer statusCode = Integer.valueOf(status.toString());
////
////        if(statusCode == HttpStatus.NOT_FOUND.value()) {
//////            return "error404";
////        }
////        else if(statusCode == HttpStatus.INTERNAL_SERVER_ERROR.value()) {
//////            return "error500";
////        }
////    }
////    return "/error/error-flat-form.html";
//}
//
////    @Override
////    public String getErrorPath() {
////        return "/error";
////    }
//}



//@CrossOrigin
//@Controller
//public class CustomizedErrorController implements ErrorController{
//	@Autowired private ErrorAttributes errorAttributes;
//	
//	 public CustomizedErrorController(ErrorAttributes errorAttributes) {
//		 this.errorAttributes = errorAttributes;
//	 }
//	
////	@RequestMapping("/error")
//    public String error(WebRequest webRequest, HttpServletRequest req, HttpServletResponse resp) throws Exception{
//		resp.setStatus(HttpStatus.OK.value());
//		
//		Integer statusCode = -1;
//		Throwable throwable = null;
//		
//		BaseDTO baseDTO = null;
//		RequestDispatcher dispatcher = null;
//		
//		if(req.getAttribute("javax.servlet.error.status_code") instanceof Integer)
//			statusCode = (Integer) req.getAttribute("javax.servlet.error.status_code");
//		if(req.getAttribute("javax.servlet.error.exception") != null
//				&& req.getAttribute("javax.servlet.error.exception") instanceof Throwable)
//			throwable = (Throwable) req.getAttribute("javax.servlet.error.exception");
//		
//		String exceptionMessage = getExceptionMessage(throwable, statusCode);
//
//		String requestUri = (String) req.getAttribute("javax.servlet.error.request_uri");
//		if (requestUri == null) {
//			requestUri = "Unknown";
//		}
//		
//		String message = MessageFormat.format("{0} returned for {1} with message {2}", statusCode, requestUri, exceptionMessage);
//		if(statusCode == HttpStatus.NOT_FOUND.value()) {
//			message = statusCode + " - Not Found...";
//		}else {
//			message = statusCode + " - Lỗi xử lý dữ liệu...";
//		}
//		String RESULT_TYPE = (null == req.getHeader(Constants.HEADER_RESULT_TYPE_NAME) ? "" : req.getHeader(Constants.HEADER_RESULT_TYPE_NAME));
//		resp.setStatus(HttpStatus.OK.value());
//		
//		
//		
//		return "/error/error-flat-form";
//	}
//	
//	private String getExceptionMessage(Throwable throwable, Integer statusCode) {
//		if (throwable != null) {
//			return Throwables.getRootCause(throwable).getMessage();
//		}
//		HttpStatus httpStatus = HttpStatus.valueOf(statusCode);
//		return httpStatus.getReasonPhrase();
//	}
//}


