package vn.sesgroup.hddt.model;

import lombok.Data;

@Data
public class TToan {

    private Double TgTCThue;
    private Double TgTThue;
    private Double TgTTTBSo;
    private String TgTTTBChu;

    public TToan(){}

    public TToan(Double tgTCThue, Double tgTThue, Double tgTTTBSo, String tgTTTBChu) {
        TgTCThue = tgTCThue;
        TgTThue = tgTThue;
        TgTTTBSo = tgTTTBSo;
        TgTTTBChu = tgTTTBChu;
    }

    public Double getTgTCThue() {
        return TgTCThue;
    }

    public void setTgTCThue(Double tgTCThue) {
        TgTCThue = tgTCThue;
    }

    public Double getTgTThue() {
        return TgTThue;
    }

    public void setTgTThue(Double tgTThue) {
        TgTThue = tgTThue;
    }

    public Double getTgTTTBSo() {
        return TgTTTBSo;
    }

    public void setTgTTTBSo(Double tgTTTBSo) {
        TgTTTBSo = tgTTTBSo;
    }

    public String getTgTTTBChu() {
        return TgTTTBChu;
    }

    public void setTgTTTBChu(String tgTTTBChu) {
        TgTTTBChu = tgTTTBChu;
    }
}
