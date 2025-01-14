package vn.sesgroup.hddt.utils;

public class ComboboxItem {
	private String id;
	private String desc;
	
	public String getId() {
		return id;
	}
	public void setId(String id) {
		this.id = id;
	}
	public String getDesc() {
		return desc;
	}
	public void setDesc(String desc) {
		this.desc = desc;
	}
	
	public ComboboxItem(){}
	
	public ComboboxItem(String id, String desc){
		this.id = id;
		this.desc = desc;
	}
	
}
