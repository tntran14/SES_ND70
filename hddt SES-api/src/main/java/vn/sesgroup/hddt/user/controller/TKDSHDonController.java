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

import vn.sesgroup.hddt.user.dao.TKDSHDonDAO;

@RestController
@RequestMapping(value = "/tkdshdon")
@Scope(value = WebApplicationContext.SCOPE_REQUEST)
public class TKDSHDonController {
	@Autowired private TKDSHDonDAO dao;
	
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
	
	@RequestMapping(value = "/export-excel-to-fast", method = RequestMethod.POST,
			consumes = {MediaType.APPLICATION_JSON_VALUE},		//MediaType.TEXT_PLAIN_VALUE, 
			produces = {MediaType.APPLICATION_OCTET_STREAM_VALUE})
	public ResponseEntity<?> exportExcelToFAST(@RequestBody JSONRoot jsonRoot) throws Exception{
		vn.sesgroup.hddt.dto.FileInfo fileInfo = dao.exportExcelToFAST(jsonRoot);
		
		HttpHeaders headers = new HttpHeaders();
		headers.add("content-disposition", "attachment; filename=" + "data.xlsx");
		headers.add("Content-Type", MediaType.APPLICATION_OCTET_STREAM_VALUE);
        
		return ResponseEntity.ok()
				.headers(headers)
				.cacheControl(CacheControl.noCache())
				.body(SerializationUtils.serialize(fileInfo));
	}
	
	@RequestMapping(value = "/export-excel-dshdon-ctiet", method = RequestMethod.POST,
			consumes = {MediaType.APPLICATION_JSON_VALUE},		//MediaType.TEXT_PLAIN_VALUE, 
			produces = {MediaType.APPLICATION_OCTET_STREAM_VALUE})
	public ResponseEntity<?> exportExcelDSHDCTiet(@RequestBody JSONRoot jsonRoot) throws Exception{
		vn.sesgroup.hddt.dto.FileInfo fileInfo = dao.exportExcelDSHDCTiet(jsonRoot);
		
		HttpHeaders headers = new HttpHeaders();
		headers.add("content-disposition", "attachment; filename=" + "data.xlsx");
		headers.add("Content-Type", MediaType.APPLICATION_OCTET_STREAM_VALUE);
        
		return ResponseEntity.ok()
				.headers(headers)
				.cacheControl(CacheControl.noCache())
				.body(SerializationUtils.serialize(fileInfo));
	}

	@RequestMapping(value = "/export-excel-general", method = RequestMethod.POST,
			consumes = {MediaType.APPLICATION_JSON_VALUE},		//MediaType.TEXT_PLAIN_VALUE, 
			produces = {MediaType.APPLICATION_OCTET_STREAM_VALUE})
	public ResponseEntity<?> exportExceGeneral(@RequestBody JSONRoot jsonRoot) throws Exception{
		vn.sesgroup.hddt.dto.FileInfo fileInfo = dao.exportExceGeneral(jsonRoot);
		
		HttpHeaders headers = new HttpHeaders();
		headers.add("content-disposition", "attachment; filename=" + "data.xlsx");
		headers.add("Content-Type", MediaType.APPLICATION_OCTET_STREAM_VALUE);
        
		return ResponseEntity.ok()
				.headers(headers)
				.cacheControl(CacheControl.noCache())
				.body(SerializationUtils.serialize(fileInfo));
	}
	
}
