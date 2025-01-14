package vn.sesgroup.hddt.dto.req;

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonRootName;

@JsonRootName(value = "UserLoginReq")
public class UserLoginReq implements Serializable{
	private static final long serialVersionUID = -2977599625646035016L;

	@JsonProperty(value = "UserName")
	private String userName;
	@JsonProperty(value = "Password")
	private String password;
	@JsonProperty(value = "RemoteAddr")
	private String remoteAddr;
	@JsonProperty(value = "UserAgent")
	private String userAgent;
	
	public String getUserName() {
		return userName;
	}
	public void setUserName(String userName) {
		this.userName = userName;
	}
	public String getPassword() {
		return password;
	}
	public void setPassword(String password) {
		this.password = password;
	}
	public String getRemoteAddr() {
		return remoteAddr;
	}
	public void setRemoteAddr(String remoteAddr) {
		this.remoteAddr = remoteAddr;
	}
	public String getUserAgent() {
		return userAgent;
	}
	public void setUserAgent(String userAgent) {
		this.userAgent = userAgent;
	}
	
	public UserLoginReq() {
		super();
	}
	
	public UserLoginReq(String userName, String password) {
		super();
		this.userName = userName;
		this.password = password;
	}
	
	public UserLoginReq(String userName, String password, String remoteAddr, String userAgent) {
		super();
		this.userName = userName;
		this.password = password;
		this.remoteAddr = remoteAddr;
		this.userAgent = userAgent;
	}
	
}
