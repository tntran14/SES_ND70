package vn.sesgroup.hddt.user.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.WebApplicationContext;

import com.api.message.MsgRsp;

import vn.sesgroup.hddt.user.dao.TraCuuThongDiepDAO;

@RestController
@RequestMapping(value = "/tracuuthongdiep")
@Scope(value = WebApplicationContext.SCOPE_REQUEST)
public class TraCuuThongDiepController {
	@Autowired
	private TraCuuThongDiepDAO dao;

	@RequestMapping(value = "/{mtdiep}", method = RequestMethod.GET, produces = { MediaType.APPLICATION_JSON_VALUE })
	public ResponseEntity<?> check(@PathVariable String mtdiep) throws Exception {
		MsgRsp rsp = dao.tracuuthongdiep(mtdiep);
		HttpHeaders headers = new HttpHeaders();
		headers.add(HttpHeaders.CONTENT_TYPE, "application/json; charset=UTF-8");
		return ResponseEntity.ok().headers(headers).cacheControl(CacheControl.noCache()).body(rsp);
	}
}
