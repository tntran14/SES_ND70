package vn.sesgroup.hddt.utility;

public class UpdateSignedMultiBillReq {
private static final long serialVersionUID = -4547581500009366335L;
	
	private String jsonRoot;
	private byte[] fileData;
	private String taxcode;
	private String formIssueInvoiceID;
	public String getJsonRoot() {
		return jsonRoot;
	}
	public void setJsonRoot(String jsonRoot) {
		this.jsonRoot = jsonRoot;
	}
	public byte[] getFileData() {
		return fileData;
	}
	public void setFileData(byte[] fileData) {
		this.fileData = fileData;
	}
	
	public String getTaxcode() {
		return taxcode;
	}
	public void setTaxcode(String taxcode) {
		this.taxcode = taxcode;
	}
	public String getFormIssueInvoiceID() {
		return formIssueInvoiceID;
	}
	public void setFormIssueInvoiceID(String formIssueInvoiceID) {
		this.formIssueInvoiceID = formIssueInvoiceID;
	}
	public static long getSerialversionuid() {
		return serialVersionUID;
	}

}
