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

import com.api.message.JSONRoot;
import com.api.message.MsgRsp;

import vn.sesgroup.hddt.user.dao.TTHDonDAO;
import vn.sesgroup.hddt.utility.Commons;

@RestController
@RequestMapping(value = "/tthdon")
@Scope(value = WebApplicationContext.SCOPE_REQUEST)
public class TTHDonController {
	Commons commons = new Commons();
	@Autowired private TTHDonDAO dao;
	
	@RequestMapping(value = "/crud", method = RequestMethod.POST,
			consumes = {MediaType.APPLICATION_JSON_VALUE},		//MediaType.TEXT_PLAIN_VALUE, 
			produces = {MediaType.APPLICATION_JSON_VALUE})
	public ResponseEntity<?> crud(@RequestBody JSONRoot jsonRoot) throws Exception{
		MsgRsp rsp = dao.crud(jsonRoot);
		HttpHeaders headers = new HttpHeaders();
		headers.add(HttpHeaders.CONTENT_TYPE, "application/json; charset=UTF-8");
		return ResponseEntity.ok()
				.headers(headers)
				.cacheControl(CacheControl.noCache())
				.body(rsp);
	}
	
	
	@RequestMapping(value = "/check", method = RequestMethod.POST,
	consumes = {MediaType.APPLICATION_JSON_VALUE},		//MediaType.TEXT_PLAIN_VALUE, 
	produces = {MediaType.APPLICATION_JSON_VALUE})
public ResponseEntity<?> check(@RequestBody JSONRoot jsonRoot) throws Exception{
MsgRsp rsp = dao.check(jsonRoot);
HttpHeaders headers = new HttpHeaders();
headers.add(HttpHeaders.CONTENT_TYPE, "application/json; charset=UTF-8");
return ResponseEntity.ok()
		.headers(headers)
		.cacheControl(CacheControl.noCache())
		.body(rsp);
}

	
}

