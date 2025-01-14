package vn.sesgroup.hddt.user.dto;

import java.io.Serializable;

public class IssuerInfo implements Serializable{
	private static final long serialVersionUID = 2469360773321442718L;

	private String _id;
	private String name;
	private String address;
	private String phone;
	private String fax;
	private String email;
	private String website;
	private String taxCode;
	private Object bankAccount;
	
	public String getWebsite() {
		return website;
	}
	public void setWebsite(String website) {
		this.website = website;
	}
	public String get_id() {
		return _id;
	}
	public void set_id(String _id) {
		this._id = _id;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public String getAddress() {
		return address;
	}
	public void setAddress(String address) {
		this.address = address;
	}
	public String getPhone() {
		return phone;
	}
	public void setPhone(String phone) {
		this.phone = phone;
	}
	public String getFax() {
		return fax;
	}
	public void setFax(String fax) {
		this.fax = fax;
	}
	public String getEmail() {
		return email;
	}
	public void setEmail(String email) {
		this.email = email;
	}
	public String getTaxCode() {
		return taxCode;
	}
	public void setTaxCode(String taxCode) {
		this.taxCode = taxCode;
	}
	public Object getBankAccount() {
		return bankAccount;
	}
	public void setBankAccount(Object bankAccount) {
		this.bankAccount = bankAccount;
	}
}
