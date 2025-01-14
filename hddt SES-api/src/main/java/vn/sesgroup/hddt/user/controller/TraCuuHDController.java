package vn.sesgroup.hddt.user.controller;

import java.util.HashMap;

import org.apache.commons.lang3.SerializationUtils;
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

import vn.sesgroup.hddt.dto.FileInfo;
import vn.sesgroup.hddt.user.dao.TraCuuHDDAO;

@RestController
@RequestMapping(value = "/tracuuhd")
@Scope(value = WebApplicationContext.SCOPE_REQUEST)
public class TraCuuHDController {
	@Autowired private TraCuuHDDAO dao;
	
	@RequestMapping(value = "/print-einvoice", method = RequestMethod.POST,
			consumes = {MediaType.APPLICATION_JSON_VALUE},		//MediaType.TEXT_PLAIN_VALUE, 
			produces = {MediaType.APPLICATION_OCTET_STREAM_VALUE})
	public ResponseEntity<?> printEinvoice(@RequestBody HashMap<String, String> mapInput) throws Exception{
		FileInfo fileInfo = dao.printEInvoice(mapInput);
		
		HttpHeaders headers = new HttpHeaders();
		headers.add("content-disposition", "attachment; filename=" + "einvoice.pdf");
		headers.add("Content-Type", MediaType.APPLICATION_OCTET_STREAM_VALUE);
        
		return ResponseEntity.ok()
				.headers(headers)
				.cacheControl(CacheControl.noCache())
				.body(SerializationUtils.serialize(fileInfo));
	}
}
