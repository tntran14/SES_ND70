package vn.sesgroup.hddt.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.stereotype.Component;
@Component
@Document(collection = "Users")
public class Users {

    @Id
    private String id;
    private String IssuerId;
    private String UserName;
    private String Password;
    private String FullName;
    private String Phone;
    private String Email;
    private Boolean IsRoot;
    private Boolean IsActive;
    private Boolean IsDelete;
    private Boolean AdIssu;
    public Boolean getAdIssu() {
		return AdIssu;
	}

	public void setAdIssu(Boolean adIssu) {
		AdIssu = adIssu;
	}

	private String Roles;
    public String getRoles() {
		return Roles;
	}

	public void setRoles(String roles) {
		Roles = roles;
	}

	@Transient
	private String captcha;
	
	@Transient
	private String hiddenCaptcha;
	
	@Transient
	private String realCaptcha;

    
    
    public Boolean getIsRoot() {
		return IsRoot;
	}

	public void setIsRoot(Boolean isRoot) {
		IsRoot = isRoot;
	}

	public Boolean getIsActive() {
		return IsActive;
	}

	public void setIsActive(Boolean isActive) {
		IsActive = isActive;
	}

	public Boolean getIsDelete() {
		return IsDelete;
	}

	public void setIsDelete(Boolean isDelete) {
		IsDelete = isDelete;
	}

	public String getCaptcha() {
		return captcha;
	}

	public void setCaptcha(String captcha) {
		this.captcha = captcha;
	}

	public String getHiddenCaptcha() {
		return hiddenCaptcha;
	}

	public void setHiddenCaptcha(String hiddenCaptcha) {
		this.hiddenCaptcha = hiddenCaptcha;
	}

	public String getRealCaptcha() {
		return realCaptcha;
	}

	public void setRealCaptcha(String realCaptcha) {
		this.realCaptcha = realCaptcha;
	}

	public Users() {
    }

    public Users(String issuerId, String userName, String password, String fullName, String phone, String email, Boolean isRoot, Boolean isActive, Boolean isDelete) {
        IssuerId = issuerId;
        UserName = userName;
        Password = password;
        FullName = fullName;
        Phone = phone;
        Email = email;
        IsRoot = isRoot;
        IsActive = isActive;
        IsDelete = isDelete;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getIssuerId() {
        return IssuerId;
    }

    public void setIssuerId(String issuerId) {
        IssuerId = issuerId;
    }

    public String getUserName() {
        return UserName;
    }

    public void setUserName(String userName) {
        UserName = userName;
    }

    public String getPassword() {
        return Password;
    }

    public void setPassword(String password) {
        Password = password;
    }

    public String getFullName() {
        return FullName;
    }

    public void setFullName(String fullName) {
        FullName = fullName;
    }

    public String getPhone() {
        return Phone;
    }

    public void setPhone(String phone) {
        Phone = phone;
    }

    public String getEmail() {
        return Email;
    }

    public void setEmail(String email) {
        Email = email;
    }

    public Boolean getRoot() {
        return IsRoot;
    }

    public void setRoot(Boolean root) {
        IsRoot = root;
    }

    public Boolean getActive() {
        return IsActive;
    }

    public void setActive(Boolean active) {
        IsActive = active;
    }

    public Boolean getDelete() {
        return IsDelete;
    }

    public void setDelete(Boolean delete) {
        IsDelete = delete;
    }

    @Override
    public String toString() {
        return "Users{" +
                "id='" + id + '\'' +
                ", IssuerId='" + IssuerId + '\'' +
                ", UserName='" + UserName + '\'' +
                ", Password='" + Password + '\'' +
                ", FullName='" + FullName + '\'' +
                ", Phone='" + Phone + '\'' +
                ", Email='" + Email + '\'' +
                ", IsRoot='" + IsRoot + '\'' +
                ", IsActive='" + IsActive + '\'' +
                ", IsDelete='" + IsDelete + '\'' +
                '}';
    }
}
