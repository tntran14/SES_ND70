package com.api.message;

import java.io.Serializable;
import java.util.concurrent.TimeUnit;

import com.codahale.metrics.json.MetricsModule;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonRootName;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

@JsonRootName(value = "msg")
public class Msg implements Serializable{
	private static final long serialVersionUID = 2093658478596332990L;

	@JsonProperty(value = "header")
	private MsgHeader msgHeader;
	@JsonProperty(value = "page")
	private MsgPage msgPage;
	@JsonProperty(value = "data")
	private Object objData;
	
	public MsgHeader getMsgHeader() {
		return msgHeader;
	}
	public void setMsgHeader(MsgHeader msgHeader) {
		this.msgHeader = msgHeader;
	}
	public MsgPage getMsgPage() {
		return msgPage;
	}
	public void setMsgPage(MsgPage msgPage) {
		this.msgPage = msgPage;
	}
	public Object getObjData() {
		return objData;
	}
	public void setObjData(Object objData) {
		this.objData = objData;
	}
	
	public Msg() {}
	
	public Msg(MsgHeader msgHeader) {
		this.msgHeader = msgHeader;
	}
	
	public Msg(MsgHeader msgHeader, Object objData) {
		this.msgHeader = msgHeader;
		this.objData = objData;
	}
	
	@Override
	public String toString() {
		ObjectMapper mapper = new ObjectMapper();
		mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
		mapper.setSerializationInclusion(Include.NON_NULL);
		mapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
		mapper.registerModule(new JavaTimeModule());
		mapper.registerModule(new MetricsModule(TimeUnit.MINUTES, TimeUnit.MILLISECONDS, false));
		mapper.enable(SerializationFeature.WRAP_ROOT_VALUE);
		
		ObjectWriter writer = mapper.writer();
		try {
			return writer.writeValueAsString(this);
		} catch (JsonProcessingException e) {
			e.printStackTrace();
			return null;
		}
	}
	
}
