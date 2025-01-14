package vn.sesgroup.hddt.model;

import java.util.Date;

import lombok.Data;

@Data
public class InfoUpdated {
    private Date UpdatedDate;
    private String UpdatedUserID;
    private String UpdatedUserName;
    private String UpdatedUserFullName;

    public InfoUpdated(){}

    public InfoUpdated(Date updatedDate, String updatedUserID, String updatedUserName, String updatedUserFullName) {
        UpdatedDate = updatedDate;
        UpdatedUserID = updatedUserID;
        UpdatedUserName = updatedUserName;
        UpdatedUserFullName = updatedUserFullName;
    }

    public Date getUpdatedDate() {
        return UpdatedDate;
    }

    public void setUpdatedDate(Date updatedDate) {
        UpdatedDate = updatedDate;
    }

    public String getUpdatedUserID() {
        return UpdatedUserID;
    }

    public void setUpdatedUserID(String updatedUserID) {
        UpdatedUserID = updatedUserID;
    }

    public String getUpdatedUserName() {
        return UpdatedUserName;
    }

    public void setUpdatedUserName(String updatedUserName) {
        UpdatedUserName = updatedUserName;
    }

    public String getUpdatedUserFullName() {
        return UpdatedUserFullName;
    }

    public void setUpdatedUserFullName(String updatedUserFullName) {
        UpdatedUserFullName = updatedUserFullName;
    }
}
