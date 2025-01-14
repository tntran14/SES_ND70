package vn.sesgroup.hddt.controller.einvoice;

import java.io.Serializable;

public class ResponseBase implements Serializable{
	private static final long serialVersionUID = -2389102003970631397L;
	
	private String resCode;
	private String resDesVN;
	private String resDesEN;
	
	private int totalRow;
	
	public String getResCode() {
		return resCode;
	}
	public void setResCode(String resCode) {
		this.resCode = resCode;
	}
	public String getResDesVN() {
		return resDesVN;
	}
	public void setResDesVN(String resDesVN) {
		this.resDesVN = resDesVN;
	}
	public String getResDesEN() {
		return resDesEN;
	}
	public void setResDesEN(String resDesEN) {
		this.resDesEN = resDesEN;
	}
	public int getTotalRow() {
		return totalRow;
	}
	public void setTotalRow(int totalRow) {
		this.totalRow = totalRow;
	}
	
}
