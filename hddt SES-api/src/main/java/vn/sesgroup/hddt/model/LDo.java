package vn.sesgroup.hddt.model;

import lombok.Data;

@Data
public class LDo {
    private String MLoi;
    private String MTLoi;

    public LDo(){}

    public LDo(String MLoi, String MTLoi) {
        this.MLoi = MLoi;
        this.MTLoi = MTLoi;
    }

    public String getMLoi() {
        return MLoi;
    }

    public void setMLoi(String MLoi) {
        this.MLoi = MLoi;
    }

    public String getMTLoi() {
        return MTLoi;
    }

    public void setMTLoi(String MTLoi) {
        this.MTLoi = MTLoi;
    }
}
