package vn.sesgroup.hddt.dto;

import java.io.Serializable;

public class BaseResDTO implements Serializable{
	private static final long serialVersionUID = -1491559379571266813L;

	private int statusCode;
	private String statusText;
	
	public int getStatusCode() {
		return statusCode;
	}
	public void setStatusCode(int statusCode) {
		this.statusCode = statusCode;
	}
	public String getStatusText() {
		return statusText;
	}
	public void setStatusText(String statusText) {
		this.statusText = statusText;
	}
	public BaseResDTO() {
		super();
	}
	
	public BaseResDTO(int statusCode, String statusText) {
		super();
		this.statusCode = statusCode;
		this.statusText = statusText;
	}
}
