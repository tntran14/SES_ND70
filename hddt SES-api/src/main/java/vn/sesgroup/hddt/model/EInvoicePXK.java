package vn.sesgroup.hddt.model;


import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.Data;

@Data
@Document(collection = "EInvoicePXK")
public class EInvoicePXK {

    @Id
    private String id;
    private String IssuerId;
    private String MTDiep;
    private EInvoiceDetail EInvoiceDetail;
    private String SignStatusCode;
    private String EInvoiceStatus;
    private Boolean IsDelete;
    private String SecureKey;
    private String Dir;
    private String FileNameXML;
    private InfoCreated InfoCreated;
    private InfoSigned InfoSigned;
    private String MCCQT;
    private InfoDeleted InfoDeleted;
    private InfoSendCQT InfoSendCQT;
    private LDo LDo;
    private InfoUpdated InfoUpdated;

    public EInvoicePXK(){}

    public EInvoicePXK(String MTDiep, vn.sesgroup.hddt.model.EInvoiceDetail EInvoiceDetail, String signStatusCode, String EInvoiceStatus, Boolean isDelete, String secureKey, String dir, String fileNameXML, vn.sesgroup.hddt.model.InfoCreated infoCreated, vn.sesgroup.hddt.model.InfoSigned infoSigned, String MCCQT, vn.sesgroup.hddt.model.InfoDeleted infoDeleted, vn.sesgroup.hddt.model.InfoSendCQT infoSendCQT, vn.sesgroup.hddt.model.LDo LDo, vn.sesgroup.hddt.model.InfoUpdated infoUpdated) {
        this.MTDiep = MTDiep;
        this.EInvoiceDetail = EInvoiceDetail;
        SignStatusCode = signStatusCode;
        this.EInvoiceStatus = EInvoiceStatus;
        IsDelete = isDelete;
        SecureKey = secureKey;
        Dir = dir;
        FileNameXML = fileNameXML;
        InfoCreated = infoCreated;
        InfoSigned = infoSigned;
        this.MCCQT = MCCQT;
        InfoDeleted = infoDeleted;
        InfoSendCQT = infoSendCQT;
        this.LDo = LDo;
        InfoUpdated = infoUpdated;
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

    public String getMTDiep() {
        return MTDiep;
    }

    public void setMTDiep(String MTDiep) {
        this.MTDiep = MTDiep;
    }

    public vn.sesgroup.hddt.model.EInvoiceDetail getEInvoiceDetail() {
        return EInvoiceDetail;
    }

    public void setEInvoiceDetail(vn.sesgroup.hddt.model.EInvoiceDetail EInvoiceDetail) {
        this.EInvoiceDetail = EInvoiceDetail;
    }

    public String getSignStatusCode() {
        return SignStatusCode;
    }

    public void setSignStatusCode(String signStatusCode) {
        SignStatusCode = signStatusCode;
    }

    public String getEInvoiceStatus() {
        return EInvoiceStatus;
    }

    public void setEInvoiceStatus(String EInvoiceStatus) {
        this.EInvoiceStatus = EInvoiceStatus;
    }

    public Boolean getDelete() {
        return IsDelete;
    }

    public void setDelete(Boolean delete) {
        IsDelete = delete;
    }

    public String getSecureKey() {
        return SecureKey;
    }

    public void setSecureKey(String secureKey) {
        SecureKey = secureKey;
    }

    public String getDir() {
        return Dir;
    }

    public void setDir(String dir) {
        Dir = dir;
    }

    public String getFileNameXML() {
        return FileNameXML;
    }

    public void setFileNameXML(String fileNameXML) {
        FileNameXML = fileNameXML;
    }

    public vn.sesgroup.hddt.model.InfoCreated getInfoCreated() {
        return InfoCreated;
    }

    public void setInfoCreated(vn.sesgroup.hddt.model.InfoCreated infoCreated) {
        InfoCreated = infoCreated;
    }

    public vn.sesgroup.hddt.model.InfoSigned getInfoSigned() {
        return InfoSigned;
    }

    public void setInfoSigned(vn.sesgroup.hddt.model.InfoSigned infoSigned) {
        InfoSigned = infoSigned;
    }

    public String getMCCQT() {
        return MCCQT;
    }

    public void setMCCQT(String MCCQT) {
        this.MCCQT = MCCQT;
    }

    public vn.sesgroup.hddt.model.InfoDeleted getInfoDeleted() {
        return InfoDeleted;
    }

    public void setInfoDeleted(vn.sesgroup.hddt.model.InfoDeleted infoDeleted) {
        InfoDeleted = infoDeleted;
    }

    public vn.sesgroup.hddt.model.InfoSendCQT getInfoSendCQT() {
        return InfoSendCQT;
    }

    public void setInfoSendCQT(vn.sesgroup.hddt.model.InfoSendCQT infoSendCQT) {
        InfoSendCQT = infoSendCQT;
    }

    public vn.sesgroup.hddt.model.LDo getLDo() {
        return LDo;
    }

    public void setLDo(vn.sesgroup.hddt.model.LDo LDo) {
        this.LDo = LDo;
    }

    public vn.sesgroup.hddt.model.InfoUpdated getInfoUpdated() {
        return InfoUpdated;
    }

    public void setInfoUpdated(vn.sesgroup.hddt.model.InfoUpdated infoUpdated) {
        InfoUpdated = infoUpdated;
    }

    @Override
    public String toString() {
        return "EInvoice{" +
                "id='" + id + '\'' +
                ", IssuerId='" + IssuerId + '\'' +
                ", MTDiep='" + MTDiep + '\'' +
                ", EInvoiceDetail=" + EInvoiceDetail +
                ", SignStatusCode='" + SignStatusCode + '\'' +
                ", EInvoiceStatus='" + EInvoiceStatus + '\'' +
                ", IsDelete=" + IsDelete +
                ", SecureKey='" + SecureKey + '\'' +
                ", Dir='" + Dir + '\'' +
                ", FileNameXML='" + FileNameXML + '\'' +
                ", InfoCreated=" + InfoCreated +
                ", InfoSigned=" + InfoSigned +
                ", MCCQT='" + MCCQT + '\'' +
                ", InfoDeleted=" + InfoDeleted +
                ", InfoSendCQT=" + InfoSendCQT +
                ", LDo=" + LDo +
                ", InfoUpdated=" + InfoUpdated +
                '}';
    }
}
