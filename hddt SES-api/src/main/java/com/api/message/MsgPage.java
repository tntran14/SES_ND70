package com.api.message;

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonRootName;

@JsonRootName(value = "page")
public class MsgPage implements Serializable{
	private static final long serialVersionUID = 3493373302076725644L;
	
	@JsonProperty(value = "size", required = false, defaultValue = "0")
	private int size;
	@JsonProperty(value = "page_no", required = false, defaultValue = "0")
	private int pageNo;
	@JsonProperty(value = "field_sort")
	private String fieldSort;
	@JsonProperty(value = "type_sort")
	private int typeSort;
	@JsonProperty(value = "total_rows", required = false, defaultValue = "0")
	private int totalRows;
	
	public int getSize() {
		return size;
	}
	public void setSize(int size) {
		this.size = size;
	}
	public int getPageNo() {
		return pageNo;
	}
	public void setPageNo(int pageNo) {
		this.pageNo = pageNo;
	}
	public String getFieldSort() {
		return fieldSort;
	}
	public void setFieldSort(String fieldSort) {
		this.fieldSort = fieldSort;
	}
	public int getTypeSort() {
		return typeSort;
	}
	public void setTypeSort(int typeSort) {
		this.typeSort = typeSort;
	}
	public int getTotalRows() {
		return totalRows;
	}
	public void setTotalRows(int totalRows) {
		this.totalRows = totalRows;
	}
	
}
