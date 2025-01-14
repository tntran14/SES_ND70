package vn.sesgroup.hddt.model;

import java.util.Date;

import lombok.Data;

@Data
public class InfoSigned {

    private Date SignedDate;
    private String SignedUserID;
    private String SignedUserName;
    private String SignedUserFullName;

    public InfoSigned(){}

    public InfoSigned(Date signedDate, String signedUserID, String signedUserName, String signedUserFullName) {
        SignedDate = signedDate;
        SignedUserID = signedUserID;
        SignedUserName = signedUserName;
        SignedUserFullName = signedUserFullName;
    }

    public Date getSignedDate() {
        return SignedDate;
    }

    public void setSignedDate(Date signedDate) {
        SignedDate = signedDate;
    }

    public String getSignedUserID() {
        return SignedUserID;
    }

    public void setSignedUserID(String signedUserID) {
        SignedUserID = signedUserID;
    }

    public String getSignedUserName() {
        return SignedUserName;
    }

    public void setSignedUserName(String signedUserName) {
        SignedUserName = signedUserName;
    }

    public String getSignedUserFullName() {
        return SignedUserFullName;
    }

    public void setSignedUserFullName(String signedUserFullName) {
        SignedUserFullName = signedUserFullName;
    }
}
