package com.api.message;

import java.io.Serializable;

import vn.sesgroup.hddt.utils.Constants;

public class MsgStreamRsp implements Serializable{
	private static final long serialVersionUID = 2828255872372343236L;
	
	private int statusCode;
	private String statusText;
	private byte[] data;
	private String fileNameDownload;
	
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
	public byte[] getData() {
		return data;
	}
	public void setData(byte[] data) {
		this.data = data;
	}	
	public String getFileNameDownload() {
		return fileNameDownload;
	}
	public void setFileNameDownload(String fileNameDownload) {
		this.fileNameDownload = fileNameDownload;
	}
	
	public MsgStreamRsp() {
		super();
		this.statusCode = 0;
		this.statusText = Constants.MAP_ERROR.get(0);
	}
	public MsgStreamRsp(int statusCode, String statusText) {
		super();
		this.statusCode = statusCode;
		this.statusText = statusText;
	}
	
}
