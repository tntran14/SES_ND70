package vn.sesgroup.hddt.dto;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

public class JsonGridDTO extends BaseDTO{
	private static final long serialVersionUID = -969294957700099916L;

	private List<HashMap<String, String>> rows = new ArrayList<>();
	private Object objectData;
	
	public List<HashMap<String, String>> getRows() {
		return rows;
	}
	public void setRows(List<HashMap<String, String>> rows) {
		this.rows = rows;
	}
	public Object getObjectData() {
		return objectData;
	}
	public void setObjectData(Object objectData) {
		this.objectData = objectData;
	}
	public JsonGridDTO() {}
	
	public JsonGridDTO(HttpServletRequest req) {
		super(req);
	}
}
