package com.api.message;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonRootName;

@JsonRootName(value = "params")
public class MsgParams implements Serializable{
	private static final long serialVersionUID = 6317741529790146935L;

	@JsonProperty(value = "params")
	private List<MsgParam> params = new ArrayList<>();

	public List<MsgParam> getParams() {
		return params;
	}
	public void setParams(List<MsgParam> params) {
		this.params = params;
	}
	
}
