package vn.sesgroup.hddt.model;

import java.util.List;

import lombok.Data;

@Data
public class EInvoiceDetail {

    private TTChung TTChung;
    private NDHDon NDHDon;
    private List<DSHHDVu> DSHHDVu;
    private TToan TToan;

    public EInvoiceDetail(){
    }

    public EInvoiceDetail(vn.sesgroup.hddt.model.TTChung TTChung, vn.sesgroup.hddt.model.NDHDon NDHDon, List<vn.sesgroup.hddt.model.DSHHDVu> DSHHDVu, vn.sesgroup.hddt.model.TToan TToan) {
        this.TTChung = TTChung;
        this.NDHDon = NDHDon;
        this.DSHHDVu = DSHHDVu;
        this.TToan = TToan;
    }

    public vn.sesgroup.hddt.model.TTChung getTTChung() {
        return TTChung;
    }

    public void setTTChung(vn.sesgroup.hddt.model.TTChung TTChung) {
        this.TTChung = TTChung;
    }

    public vn.sesgroup.hddt.model.NDHDon getNDHDon() {
        return NDHDon;
    }

    public void setNDHDon(vn.sesgroup.hddt.model.NDHDon NDHDon) {
        this.NDHDon = NDHDon;
    }

    public List<vn.sesgroup.hddt.model.DSHHDVu> getDSHHDVu() {
        return DSHHDVu;
    }

    public void setDSHHDVu(List<vn.sesgroup.hddt.model.DSHHDVu> DSHHDVu) {
        this.DSHHDVu = DSHHDVu;
    }

    public vn.sesgroup.hddt.model.TToan getTToan() {
        return TToan;
    }

    public void setTToan(vn.sesgroup.hddt.model.TToan TToan) {
        this.TToan = TToan;
    }
}
