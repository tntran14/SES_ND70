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

import vn.sesgroup.hddt.dto.FileInfo;
import vn.sesgroup.hddt.user.dao.CommonDAO;

@RestController
@RequestMapping(value = "/commons")
@Scope(value = WebApplicationContext.SCOPE_REQUEST)
public class CommonsController {
	@Autowired private CommonDAO dao;
	@RequestMapping(value = "/print-einvoiceAll", method = RequestMethod.POST,
			consumes = {MediaType.APPLICATION_JSON_VALUE},		//MediaType.TEXT_PLAIN_VALUE, 
			produces = {MediaType.APPLICATION_OCTET_STREAM_VALUE})
	public ResponseEntity<?> printEinvoiceAll(@RequestBody JSONRoot jsonRoot) throws Exception{
		FileInfo fileInfo = dao.printEinvoiceAll(jsonRoot);
		
		HttpHeaders headers = new HttpHeaders();
		headers.add("content-disposition", "attachment; filename=" + "einvoice.pdf");
		headers.add("Content-Type", MediaType.APPLICATION_OCTET_STREAM_VALUE);
		
		return ResponseEntity.ok()
				.headers(headers)
				.cacheControl(CacheControl.noCache())
				.body(SerializationUtils.serialize(fileInfo));
	}
	
	@RequestMapping(value = "/print-cttncnAll", method = RequestMethod.POST,
			consumes = {MediaType.APPLICATION_JSON_VALUE},		//MediaType.TEXT_PLAIN_VALUE, 
			produces = {MediaType.APPLICATION_OCTET_STREAM_VALUE})
	public ResponseEntity<?> printCttncnAll(@RequestBody JSONRoot jsonRoot) throws Exception{
		FileInfo fileInfo = dao.printCttncnAll(jsonRoot);
		
		HttpHeaders headers = new HttpHeaders();
		headers.add("content-disposition", "attachment; filename=" + "einvoice.pdf");
		headers.add("Content-Type", MediaType.APPLICATION_OCTET_STREAM_VALUE);
		
		return ResponseEntity.ok()
				.headers(headers)
				.cacheControl(CacheControl.noCache())
				.body(SerializationUtils.serialize(fileInfo));
	}	
	
	@RequestMapping(value = "/print-einvoiceAll1", method = RequestMethod.POST,
			consumes = {MediaType.APPLICATION_JSON_VALUE},		//MediaType.TEXT_PLAIN_VALUE, 
			produces = {MediaType.APPLICATION_OCTET_STREAM_VALUE})
	public ResponseEntity<?> printEinvoiceAll1(@RequestBody JSONRoot jsonRoot) throws Exception{
		FileInfo fileInfo = dao.printEinvoiceAll1(jsonRoot);
		
		HttpHeaders headers = new HttpHeaders();
		headers.add("content-disposition", "attachment; filename=" + "einvoice.pdf");
		headers.add("Content-Type", MediaType.APPLICATION_OCTET_STREAM_VALUE);
		
		return ResponseEntity.ok()
				.headers(headers)
				.cacheControl(CacheControl.noCache())
				.body(SerializationUtils.serialize(fileInfo));
	}
	
	@RequestMapping(value = "/print-einvoice_mttAll", method = RequestMethod.POST,
			consumes = {MediaType.APPLICATION_JSON_VALUE},		//MediaType.TEXT_PLAIN_VALUE, 
			produces = {MediaType.APPLICATION_OCTET_STREAM_VALUE})
	public ResponseEntity<?> printEinvoiceMTTAll(@RequestBody JSONRoot jsonRoot) throws Exception{
		FileInfo fileInfo = dao.printEinvoiceMTTAll(jsonRoot);
		
		HttpHeaders headers = new HttpHeaders();
		headers.add("content-disposition", "attachment; filename=" + "einvoice_mtt.pdf");
		headers.add("Content-Type", MediaType.APPLICATION_OCTET_STREAM_VALUE);
		
		return ResponseEntity.ok()
				.headers(headers)
				.cacheControl(CacheControl.noCache())
				.body(SerializationUtils.serialize(fileInfo));
	}
	
	@RequestMapping(value = "/print-cttncnAllV1", method = RequestMethod.POST,
			consumes = {MediaType.APPLICATION_JSON_VALUE},		//MediaType.TEXT_PLAIN_VALUE, 
			produces = {MediaType.APPLICATION_OCTET_STREAM_VALUE})
	public ResponseEntity<?> printCttncnAllV1(@RequestBody JSONRoot jsonRoot) throws Exception{
		FileInfo fileInfo = dao.printCttncnAllV1(jsonRoot);
		
		HttpHeaders headers = new HttpHeaders();
		headers.add("content-disposition", "attachment; filename=" + "CTTNCN.pdf");
		headers.add("Content-Type", MediaType.APPLICATION_OCTET_STREAM_VALUE);
		
		return ResponseEntity.ok()
				.headers(headers)
				.cacheControl(CacheControl.noCache())
				.body(SerializationUtils.serialize(fileInfo));
	}	
}
