package vn.sesgroup.hddt.dto;

import java.io.Serializable;

public class BaseDTO implements Serializable{
	private static final long serialVersionUID = -2502433701323708846L;

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
	
	public BaseDTO() {}
	
	public BaseDTO(int statusCode, String statusText) {
		super();
		this.statusCode = statusCode;
		this.statusText = statusText;
	}
	
}
