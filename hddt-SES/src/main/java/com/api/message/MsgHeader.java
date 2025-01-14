package com.api.message;

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonRootName;

@JsonRootName(value = "header")
public class MsgHeader implements Serializable{
	private static final long serialVersionUID = 7711980040246651963L;

	@JsonProperty(value = "Type")
	private UserType type;
	@JsonProperty(value = "UserId")
	private String userId;
	@JsonProperty(value = "UserName")
	private String userName;
	@JsonProperty(value = "UserFullName")
	private String userFullName;
	@JsonProperty(value = "IssuerId")
	private String issuerId;
	@JsonProperty(value = "IsRoot")
	private boolean isRoot;
	@JsonProperty(value = "IPAddress")
	private String ipAddress;
	@JsonProperty(value = "ActionCode")
	private String actionCode;
	@JsonProperty(value = "RqId")
	private String rqId;
	
	public UserType getType() {
		return type;
	}
	public void setType(UserType type) {
		this.type = type;
	}
	public String getUserId() {
		return userId;
	}
	public void setUserId(String userId) {
		this.userId = userId;
	}
	public String getUserName() {
		return userName;
	}
	public void setUserName(String userName) {
		this.userName = userName;
	}
	public String getUserFullName() {
		return userFullName;
	}
	public void setUserFullName(String userFullName) {
		this.userFullName = userFullName;
	}
	public String getIssuerId() {
		return issuerId;
	}
	public void setIssuerId(String issuerId) {
		this.issuerId = issuerId;
	}
	public boolean isRoot() {
		return isRoot;
	}
	public void setRoot(boolean isRoot) {
		this.isRoot = isRoot;
	}
	public String getIpAddress() {
		return ipAddress;
	}
	public void setIpAddress(String ipAddress) {
		this.ipAddress = ipAddress;
	}
	public String getActionCode() {
		return actionCode;
	}
	public void setActionCode(String actionCode) {
		this.actionCode = actionCode;
	}
	public String getRqId() {
		return rqId;
	}
	public void setRqId(String rqId) {
		this.rqId = rqId;
	}
	
}
