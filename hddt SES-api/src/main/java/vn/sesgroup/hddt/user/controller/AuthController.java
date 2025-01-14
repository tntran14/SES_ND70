package vn.sesgroup.hddt.user.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.WebApplicationContext;

import vn.sesgroup.hddt.user.dao.LoginDAO;
import vn.sesgroup.hddt.user.dto.LoginRes;
import vn.sesgroup.hddt.user.dto.UserLoginReq;

@RestController
@RequestMapping(value = "/")
@Scope(value = WebApplicationContext.SCOPE_REQUEST)
public class AuthController {
	@Autowired private LoginDAO dao;
	
	@RequestMapping(value = "/auth", method = RequestMethod.POST,
			consumes = {MediaType.APPLICATION_JSON_VALUE},		//MediaType.TEXT_PLAIN_VALUE, 
			produces = {MediaType.APPLICATION_JSON_VALUE})
	public ResponseEntity<?> doLogin(@RequestBody UserLoginReq req) throws Exception{
		LoginRes res = dao.doAuth(req);
		
		HttpHeaders headers = new HttpHeaders();
		headers.add(HttpHeaders.CONTENT_TYPE, "application/json; charset=UTF-8");
		return ResponseEntity.ok()
				.headers(headers)
				.cacheControl(CacheControl.noCache())
				.body(res);
	}
}
