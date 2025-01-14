package vn.sesgroup.hddt.dto;

import java.io.Serializable;
import java.util.List;

public class CurrentUserProfile implements Serializable{
	private static final long serialVersionUID = 6227321224583118544L;
	
	private String username;
    private String password;
    private List<CurrentUserRole> authorities;
    private boolean accountNonExpired = true;
    private boolean accountNonLocked = true;
    private boolean credentialsNonExpired = true;
    private boolean enabled = true;
    
    private LoginRes loginRes;
    private boolean isAdmin;
    private String allRights;
	public String getUsername() {
		return username;
	}
	public void setUsername(String username) {
		this.username = username;
	}
	public String getPassword() {
		return password;
	}
	public void setPassword(String password) {
		this.password = password;
	}
	public List<CurrentUserRole> getAuthorities() {
		return authorities;
	}
	public void setAuthorities(List<CurrentUserRole> authorities) {
		this.authorities = authorities;
	}
	public boolean isAccountNonExpired() {
		return accountNonExpired;
	}
	public void setAccountNonExpired(boolean accountNonExpired) {
		this.accountNonExpired = accountNonExpired;
	}
	public boolean isAccountNonLocked() {
		return accountNonLocked;
	}
	public void setAccountNonLocked(boolean accountNonLocked) {
		this.accountNonLocked = accountNonLocked;
	}
	public boolean isCredentialsNonExpired() {
		return credentialsNonExpired;
	}
	public void setCredentialsNonExpired(boolean credentialsNonExpired) {
		this.credentialsNonExpired = credentialsNonExpired;
	}
	public boolean isEnabled() {
		return enabled;
	}
	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}
	public LoginRes getLoginRes() {
		return loginRes;
	}
	public void setLoginRes(LoginRes loginRes) {
		this.loginRes = loginRes;
	}
	public boolean isAdmin() {
		return isAdmin;
	}
	public void setAdmin(boolean isAdmin) {
		this.isAdmin = isAdmin;
	}
	public String getAllRights() {
		return allRights;
	}
	public void setAllRights(String allRights) {
		this.allRights = allRights;
	}
    
}
