package com.api.message;

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonProperty;

public class JSONRoot implements Serializable{
	private static final long serialVersionUID = 6374921486833711537L;
	
	@JsonProperty(value = "msg")
	private Msg msg;

	public Msg getMsg() {
		return msg;
	}
	public void setMsg(Msg msg) {
		this.msg = msg;
	}
	
	public JSONRoot() {
	}
	
	public JSONRoot(Msg msg) {
		super();
		this.msg = msg;
	}
}
