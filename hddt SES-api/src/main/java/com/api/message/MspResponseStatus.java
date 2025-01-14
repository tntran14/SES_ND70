package com.api.message;

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonRootName;

@JsonRootName(value = "response_status")
public class MspResponseStatus implements Serializable{
	private static final long serialVersionUID = -3017955408460097171L;
	
	@JsonProperty(value = "error_code", required = false, defaultValue = "0")
	private int errorCode;
	@JsonProperty(value = "error_desc")
	private String errorDesc;
	
	public int getErrorCode() {
		return errorCode;
	}
	public void setErrorCode(int errorCode) {
		this.errorCode = errorCode;
	}
	public String getErrorDesc() {
		return errorDesc;
	}
	public void setErrorDesc(String errorDesc) {
		this.errorDesc = errorDesc;
	}
	
	public MspResponseStatus() {
		super();
	}
	
	public MspResponseStatus(int errorCode, String errorDesc) {
		super();
		this.errorCode = errorCode;
		this.errorDesc = errorDesc;
	}
}