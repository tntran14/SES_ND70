package vn.sesgroup.hddt.dto;

import org.springframework.security.core.GrantedAuthority;

public class CurrentUserRole implements GrantedAuthority{
	private static final long serialVersionUID = 2856677693245884068L;
	
	private String name;
	
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	@Override
	public String getAuthority() {
		return this.name;
	}
}
