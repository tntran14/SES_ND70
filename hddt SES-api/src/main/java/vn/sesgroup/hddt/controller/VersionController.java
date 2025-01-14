package vn.sesgroup.hddt.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import vn.sesgroup.hddt.utility.SystemParams;

@RestController
@RequestMapping("/")
public class VersionController {
	
	@RequestMapping(value = {"/", "/version"}, 
			produces = {org.springframework.http.MediaType.TEXT_PLAIN_VALUE, org.springframework.http.MediaType.APPLICATION_JSON_VALUE}, 
			method = {RequestMethod.GET, RequestMethod.POST})
	public String version() {
		return SystemParams.VERSION;
	}
}
