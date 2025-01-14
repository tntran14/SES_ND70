package vn.sesgroup.hddt.model;

import java.util.Date;

import lombok.Data;

@Data
public class InfoSendCQT {
    private Date Date;
    private String UserID;
    private String UserName;
    private String UserFullName;

    public InfoSendCQT(){}

    public InfoSendCQT(java.util.Date date, String userID, String userName, String userFullName) {
        Date = date;
        UserID = userID;
        UserName = userName;
        UserFullName = userFullName;
    }


    public java.util.Date getDate() {
        return Date;
    }

    public void setDate(java.util.Date date) {
        Date = date;
    }

    public String getUserID() {
        return UserID;
    }

    public void setUserID(String userID) {
        UserID = userID;
    }

    public String getUserName() {
        return UserName;
    }

    public void setUserName(String userName) {
        UserName = userName;
    }

    public String getUserFullName() {
        return UserFullName;
    }

    public void setUserFullName(String userFullName) {
        UserFullName = userFullName;
    }
}
