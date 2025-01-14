package vn.sesgroup.hddt.exception;

import java.io.FileNotFoundException;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

import com.mongodb.MongoWriteException;

//https://www.bezkoder.com/spring-boot-restcontrolleradvice/
@RestControllerAdvice
public class GlobalRestExceptionHandler {
	private static final Logger log = LogManager.getLogger(GlobalRestExceptionHandler.class);
	
	@ExceptionHandler({
		org.springframework.http.converter.HttpMessageNotReadableException.class
		, HttpRequestMethodNotSupportedException.class
		, Exception.class})
    public ResponseEntity <ApiErrorResponse> processHttpMessageNotReadableException(Exception ex, WebRequest request) {
		log.error(" >>>>> An exception occurred!", ex);
		ApiErrorResponse apiResponse = null;
		HttpStatus httpStatus = null;
		if (ex instanceof org.springframework.http.converter.HttpMessageNotReadableException) {
			apiResponse = new ApiErrorResponse
            .ApiErrorResponseBuilder()
//            .withDetail(ex.getLocalizedMessage())
            .withDetail("Invalid JSON input...")
            .withMessage("Input data is invalid...")
            .withErrorCode(String.valueOf(HttpStatus.BAD_REQUEST.value()))
            .withStatus(HttpStatus.BAD_REQUEST)
            .atTime(LocalDateTime.now(ZoneOffset.UTC))
            .build();
			httpStatus = HttpStatus.BAD_REQUEST;
		}else if (ex instanceof HttpRequestMethodNotSupportedException) {
			apiResponse = new ApiErrorResponse
		            .ApiErrorResponseBuilder()
		            .withDetail(ex.getLocalizedMessage())
//		            .withDetail("Invalid JSON input...")
		            .withMessage("405 Method Not Allowed")
		            .withErrorCode(String.valueOf(HttpStatus.METHOD_NOT_ALLOWED.value()))
		            .withStatus(HttpStatus.METHOD_NOT_ALLOWED)
		            .atTime(LocalDateTime.now(ZoneOffset.UTC))
		            .build();
			httpStatus = HttpStatus.METHOD_NOT_ALLOWED;
		}else if (ex instanceof FileNotFoundException) {
			apiResponse = new ApiErrorResponse
		            .ApiErrorResponseBuilder()
		            .withDetail("The system cannot find the file specified")
//		            .withDetail("Invalid JSON input...")
		            .withMessage("404 FileNotFoundException")
		            .withErrorCode(String.valueOf(HttpStatus.NOT_FOUND.value()))
		            .withStatus(HttpStatus.NOT_FOUND)
		            .atTime(LocalDateTime.now(ZoneOffset.UTC))
		            .build();
			httpStatus = HttpStatus.NOT_FOUND;
		}else if (ex instanceof MongoWriteException) {
			MongoWriteException mongoWriteException = (MongoWriteException) ex;
			apiResponse = new ApiErrorResponse
		            .ApiErrorResponseBuilder()
		            .withDetail(mongoWriteException.getMessage())
//		            .withDetail("Invalid JSON input...")
//		            .withMessage("263 MongoWriteException")
		            .withMessage(mongoWriteException.getCode() + " " + mongoWriteException.getMessage())
		            .withErrorCode(String.valueOf(HttpStatus.NOT_FOUND.value()))
		            .withStatus(HttpStatus.NOT_FOUND)
		            .atTime(LocalDateTime.now(ZoneOffset.UTC))
		            .build();
			httpStatus = HttpStatus.NOT_FOUND;
		}else {			
			apiResponse = new ApiErrorResponse
		            .ApiErrorResponseBuilder()
		            .withDetail(ex.getMessage() == null? "Lỗi hệ thống...": ex.getMessage())
		            .withMessage("400 Bad Request")
		            .withErrorCode(String.valueOf(HttpStatus.BAD_REQUEST.value()))
		            .withStatus(HttpStatus.BAD_REQUEST)
		            .atTime(LocalDateTime.now(ZoneOffset.UTC))
		            .build();
			httpStatus = HttpStatus.BAD_REQUEST;
		}
        
        return new ResponseEntity <ApiErrorResponse> (apiResponse, httpStatus);
    }
}
