package vn.sesgroup.hddt.user.controller;

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

import com.api.message.JSONRoot;
import com.api.message.MsgRsp;

import vn.sesgroup.hddt.user.dao.CAInvoiceDAO;
import vn.sesgroup.hddt.utility.Commons;

@RestController
@RequestMapping(value = "/ca_invoice")
@Scope(value = WebApplicationContext.SCOPE_REQUEST)
public class CAInvoiceController {
	Commons commons = new Commons();
	@Autowired private CAInvoiceDAO dao;
	
	
	@RequestMapping(value = "/list", method = RequestMethod.POST,
			consumes = {MediaType.APPLICATION_JSON_VALUE},		//MediaType.TEXT_PLAIN_VALUE, 
			produces = {MediaType.APPLICATION_JSON_VALUE})
	public ResponseEntity<?> list(@RequestBody JSONRoot jsonRoot) throws Exception{
		MsgRsp rsp = dao.list(jsonRoot);
		HttpHeaders headers = new HttpHeaders();
		headers.add(HttpHeaders.CONTENT_TYPE, "application/json; charset=UTF-8");
		return ResponseEntity.ok()
				.headers(headers)
				.cacheControl(CacheControl.noCache())
				.body(rsp);
	}
	@RequestMapping(value = "/export-excel", method = RequestMethod.POST,
			consumes = {MediaType.APPLICATION_JSON_VALUE},		//MediaType.TEXT_PLAIN_VALUE, 
			produces = {MediaType.APPLICATION_OCTET_STREAM_VALUE})
	public ResponseEntity<?> exportExcelToFAST(@RequestBody JSONRoot jsonRoot) throws Exception{
		vn.sesgroup.hddt.dto.FileInfo fileInfo = dao.exportExcel(jsonRoot);
		
		HttpHeaders headers = new HttpHeaders();
		headers.add("content-disposition", "attachment; filename=" + "data.xlsx");
		headers.add("Content-Type", MediaType.APPLICATION_OCTET_STREAM_VALUE);
        
		return ResponseEntity.ok()
				.headers(headers)
				.cacheControl(CacheControl.noCache())
				.body(SerializationUtils.serialize(fileInfo));
	}
	
}
