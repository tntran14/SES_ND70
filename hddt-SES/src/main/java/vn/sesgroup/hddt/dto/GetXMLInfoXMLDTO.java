package vn.sesgroup.hddt.dto;

import java.io.Serializable;

public class GetXMLInfoXMLDTO implements Serializable{
	private static final long serialVersionUID = 8727379364315528690L;
	
	private String fileName;
	private byte[] fileData;
	private int shd;
	private String IDMSHDon;
	
	public String getFileName() {
		return fileName;
	}
	public void setFileName(String fileName) {
		this.fileName = fileName;
	}
	public byte[] getFileData() {
		return fileData;
	}
	public void setFileData(byte[] fileData) {
		this.fileData = fileData;
	}
	public int getShd() {
		return shd;
	}
	public void setShd(int shd) {
		this.shd = shd;
	}
	public String getIDMSHDon() {
		return IDMSHDon;
	}
	public void setIDMSHDon(String iDMSHDon) {
		IDMSHDon = iDMSHDon;
	}
	
}
