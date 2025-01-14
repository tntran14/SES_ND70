package com.api.message;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;

import com.fasterxml.jackson.annotation.JsonProperty;

public class MsgParam implements Serializable{
	private static final long serialVersionUID = -116397857653721483L;
	
	@JsonProperty(value = "id")
	private String id;
	@JsonProperty(value = "param")
	private String param;
	@JsonProperty(value = "conds")
	private ArrayList<HashMap<String, String>> conds = new ArrayList<>();
	public String getId() {
		return id;
	}
	public String getParam() {
		return param;
	}
	public ArrayList<HashMap<String, String>> getConds() {
		return conds;
	}
	public void setId(String id) {
		this.id = id;
	}
	public void setParam(String param) {
		this.param = param;
	}
	public void setConds(ArrayList<HashMap<String, String>> conds) {
		this.conds = conds;
	}
	
}
