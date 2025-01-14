package vn.sesgroup.hddt.model;

import java.util.Date;

import lombok.Data;

@Data
public class InfoDeleted {
    private Date DeletedDate;
    private String DeletedUserID;
    private String DeletedUserName;
    private String DeletedUserFullName;

    public InfoDeleted(){}

    public InfoDeleted(Date deletedDate, String deletedUserID, String deletedUserName, String deletedUserFullName) {
        DeletedDate = deletedDate;
        DeletedUserID = deletedUserID;
        DeletedUserName = deletedUserName;
        DeletedUserFullName = deletedUserFullName;
    }

    public Date getDeletedDate() {
        return DeletedDate;
    }

    public void setDeletedDate(Date deletedDate) {
        DeletedDate = deletedDate;
    }

    public String getDeletedUserID() {
        return DeletedUserID;
    }

    public void setDeletedUserID(String deletedUserID) {
        DeletedUserID = deletedUserID;
    }

    public String getDeletedUserName() {
        return DeletedUserName;
    }

    public void setDeletedUserName(String deletedUserName) {
        DeletedUserName = deletedUserName;
    }

    public String getDeletedUserFullName() {
        return DeletedUserFullName;
    }

    public void setDeletedUserFullName(String deletedUserFullName) {
        DeletedUserFullName = deletedUserFullName;
    }
}


