package vn.sesgroup.hddt.dto;

import java.io.Serializable;
import java.util.List;

public class FileInfo implements Serializable{
	private static final long serialVersionUID = 2542485207869285810L;
	
	private byte[] contentFile;
	private String fileName;
	private String taxcode;
	private String check;
	public String getCheck() {
		return check;
	}

	public void setCheck(String check) {
		this.check = check;
	}
	private List<Long> numbers;
	private String formIssueInvoiceID;
	private List<GetXMLInfoXMLDTO> arrFileInfos;
	public byte[] getContentFile() {
		return contentFile;
	}

	public List<GetXMLInfoXMLDTO> getArrFileInfos() {
		return arrFileInfos;
	}
	public List<Long> getNumbers() {
		return numbers;
	}
	public void setNumbers(List<Long> numbers) {
		this.numbers = numbers;
	}
	public String getFormIssueInvoiceID() {
		return formIssueInvoiceID;
	}
	public void setFormIssueInvoiceID(String formIssueInvoiceID) {
		this.formIssueInvoiceID = formIssueInvoiceID;
	}
	public void setArrFileInfos(List<GetXMLInfoXMLDTO> arrFileInfos) {
		this.arrFileInfos = arrFileInfos;
	}
	public void setContentFile(byte[] contentFile) {
		this.contentFile = contentFile;
	}
	public String getFileName() {
		return fileName;
	}
	public void setFileName(String fileName) {
		this.fileName = fileName;
	}
	public String getTaxcode() {
		return taxcode;
	}
	public void setTaxcode(String taxcode) {
		this.taxcode = taxcode;
	}
	
	
}
