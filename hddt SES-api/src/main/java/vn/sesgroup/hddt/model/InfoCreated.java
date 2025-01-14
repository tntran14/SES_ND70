package vn.sesgroup.hddt.model;

import java.time.LocalDateTime;

import lombok.Data;

@Data
public class InfoCreated {

    private LocalDateTime CreateDate;
    private String CreateUserID;
    private String CreateUserName;
    private String CreateUserFullName;

    public InfoCreated(){}

    public InfoCreated(LocalDateTime createDate, String createUserID, String createUserName, String createUserFullName) {
        CreateDate = createDate;
        CreateUserID = createUserID;
        CreateUserName = createUserName;
        CreateUserFullName = createUserFullName;
    }

    public LocalDateTime getCreateDate() {
        return CreateDate;
    }

    public void setCreateDate(LocalDateTime createDate) {
        CreateDate = createDate;
    }

    public String getCreateUserID() {
        return CreateUserID;
    }

    public void setCreateUserID(String createUserID) {
        CreateUserID = createUserID;
    }

    public String getCreateUserName() {
        return CreateUserName;
    }

    public void setCreateUserName(String createUserName) {
        CreateUserName = createUserName;
    }

    public String getCreateUserFullName() {
        return CreateUserFullName;
    }

    public void setCreateUserFullName(String createUserFullName) {
        CreateUserFullName = createUserFullName;
    }
}
