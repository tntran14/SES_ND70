package vn.sesgroup.hddt.model;

import lombok.Data;

@Data
public class NDHDon {
    private NBan NBan;
    private NMua NMua;

    public NDHDon(){}

    public NDHDon(vn.sesgroup.hddt.model.NBan NBan, vn.sesgroup.hddt.model.NMua NMua) {
        this.NBan = NBan;
        this.NMua = NMua;
    }
}
