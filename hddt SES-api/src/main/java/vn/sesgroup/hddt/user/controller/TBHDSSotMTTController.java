package vn.sesgroup.hddt.user.controller;

import java.io.InputStream;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.SerializationUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.multipart.MultipartHttpServletRequest;

import com.api.message.JSONRoot;
import com.api.message.MsgRsp;
import com.fasterxml.jackson.core.type.TypeReference;

import vn.sesgroup.hddt.dto.FileInfo;
import vn.sesgroup.hddt.user.dao.TBHDSSotMTTDAO;
import vn.sesgroup.hddt.utility.Commons;
import vn.sesgroup.hddt.utility.Json;

@RestController
@RequestMapping(value = "/tbhdssot_mtt")
@Scope(value = WebApplicationContext.SCOPE_REQUEST)
public class TBHDSSotMTTController {
	Commons commons = new Commons();
	@Autowired TBHDSSotMTTDAO dao;
	
	@PostMapping(value = "/crud",
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
	
	@PostMapping(value = "/list",
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
	
	@PostMapping(value = "/detail/{_id}",
			consumes = {MediaType.APPLICATION_JSON_VALUE},		//MediaType.TEXT_PLAIN_VALUE, 
			produces = {MediaType.APPLICATION_JSON_VALUE})
	public ResponseEntity<?> detail(@RequestBody JSONRoot jsonRoot
			, @PathVariable(name = "_id", required = false) String _id) throws Exception{
		MsgRsp rsp = dao.detail(jsonRoot, _id);
		
		HttpHeaders headers = new HttpHeaders();
		headers.add(HttpHeaders.CONTENT_TYPE, "application/json; charset=UTF-8");
		return ResponseEntity.ok()
				.headers(headers)
				.cacheControl(CacheControl.noCache())
				.body(rsp);
	}
	@PostMapping(value = "/history/{_id}",
			consumes = {MediaType.APPLICATION_JSON_VALUE},		//MediaType.TEXT_PLAIN_VALUE, 
			produces = {MediaType.APPLICATION_JSON_VALUE})
	public ResponseEntity<?> history(@RequestBody JSONRoot jsonRoot
			, @PathVariable(name = "_id", required = false) String _id) throws Exception{
		MsgRsp rsp = dao.history(jsonRoot, _id);
		
		HttpHeaders headers = new HttpHeaders();
		headers.add(HttpHeaders.CONTENT_TYPE, "application/json; charset=UTF-8");
		return ResponseEntity.ok()
				.headers(headers)
				.cacheControl(CacheControl.noCache())
				.body(rsp);
	}
	@PostMapping(value = "/get-file-for-sign", consumes = {
			MediaType.APPLICATION_JSON_VALUE }, // MediaType.TEXT_PLAIN_VALUE,
			produces = { MediaType.APPLICATION_OCTET_STREAM_VALUE })
	public ResponseEntity<?> getFilesForSign(@RequestBody JSONRoot jsonRoot) throws Exception {
		FileInfo fileInfo = dao.getFileForSign(jsonRoot);

		HttpHeaders headers = new HttpHeaders();
		headers.add("content-disposition", "attachment; filename=" + "template.data");
		headers.add("Content-Type", MediaType.APPLICATION_OCTET_STREAM_VALUE);

		headers.add(HttpHeaders.CONTENT_TYPE, "application/json; charset=UTF-8");
		return ResponseEntity.ok().headers(headers).cacheControl(CacheControl.noCache())
				.body(SerializationUtils.serialize(fileInfo));
	}

	@PostMapping(value = "/send-mail",
			consumes = {MediaType.APPLICATION_JSON_VALUE},		//MediaType.TEXT_PLAIN_VALUE, 
			produces = {MediaType.APPLICATION_JSON_VALUE})
	public ResponseEntity<?> sendMail(@RequestBody JSONRoot jsonRoot) throws Exception{
		MsgRsp rsp = dao.sendMail(jsonRoot);
		HttpHeaders headers = new HttpHeaders();
		headers.add(HttpHeaders.CONTENT_TYPE, "application/json; charset=UTF-8");
		return ResponseEntity.ok()
				.headers(headers)
				.cacheControl(CacheControl.noCache())
				.body(rsp);
	}
	@PostMapping(value = "/sign-single", consumes = {
			MediaType.MULTIPART_FORM_DATA_VALUE }, produces = { MediaType.APPLICATION_JSON_VALUE })
	public ResponseEntity<?> signSingle(HttpServletRequest req,
			MultipartHttpServletRequest multipartHttpServletRequest,
			@RequestParam(name = "Base64JsonRoot", defaultValue = "") String _Base64JsonRoot,
			@RequestParam(name = "_id", defaultValue = "") String _id
		) throws Exception {
		HttpHeaders headers = new HttpHeaders();
		headers.add(HttpHeaders.CONTENT_TYPE, "application/json; charset=UTF-8");
		
		JSONRoot jsonRoot = null;		
		try {
			jsonRoot = Json.serializer().fromJson(commons.decodeBase64ToString(_Base64JsonRoot), new TypeReference<JSONRoot>() {
			});
		}catch(Exception e) {
			throw new Exception("Lỗi dữ liệu đầu vào");
		}
		
		InputStream is = multipartHttpServletRequest.getFile("XMLFileSigned").getInputStream();
		
		return ResponseEntity.ok()
				.headers(headers)
				.cacheControl(CacheControl.noCache())
				.body(dao.signSingle(is, jsonRoot, _id));
	}
	
	@PostMapping(value = "/refresh-status-cqt",
			consumes = {MediaType.APPLICATION_JSON_VALUE},		//MediaType.TEXT_PLAIN_VALUE, 
			produces = {MediaType.APPLICATION_JSON_VALUE})
	public ResponseEntity<?> refreshStatusCQT(@RequestBody JSONRoot jsonRoot) throws Exception{
		MsgRsp rsp = dao.refreshStatusCQT(jsonRoot);
		HttpHeaders headers = new HttpHeaders();
		headers.add(HttpHeaders.CONTENT_TYPE, "application/json; charset=UTF-8");
		return ResponseEntity.ok()
				.headers(headers)
				.cacheControl(CacheControl.noCache())
				.body(rsp);
	}
	
}
