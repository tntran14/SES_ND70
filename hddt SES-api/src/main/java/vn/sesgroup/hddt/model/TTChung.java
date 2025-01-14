package vn.sesgroup.hddt.model;

import java.time.LocalDateTime;

import lombok.Data;

@Data
public class TTChung {
    private String THDon;
    private String MauSoHD;
    private String KHMSHDon;
    private String MaHD;
    private String KHHDon;
    private LocalDateTime NLap;
    private String DVTTe;
    private String TGia;
    private String HTTToanCode;
    private String HTTToan;


    public TTChung(){}

    public TTChung(String MaHD,String THDon, String mauSoHD, String KHMSHDon, String KHHDon, LocalDateTime NLap, String DVTTe, String TGia, String HTTToanCode, String HTTToan) {
        this.THDon = THDon;
        MauSoHD = mauSoHD;
        this.KHMSHDon = KHMSHDon;
        this.MaHD = MaHD;
        this.KHHDon = KHHDon;
        this.NLap = NLap;
        this.DVTTe = DVTTe;
        this.TGia = TGia;
        this.HTTToanCode = HTTToanCode;
        this.HTTToan = HTTToan;

    }

    
    public TTChung(String tHDon, String mauSoHD, String kHMSHDon, String maHD, String kHHDon, LocalDateTime nLap,
			String dVTTe, String tGia) {
		super();
		THDon = tHDon;
		MauSoHD = mauSoHD;
		KHMSHDon = kHMSHDon;
		MaHD = maHD;
		KHHDon = kHHDon;
		NLap = nLap;
		DVTTe = dVTTe;
		TGia = tGia;
	}

	public String getTHDon() {
        return THDon;
    }

    public void setTHDon(String THDon) {
        this.THDon = THDon;
    }

    public String getMauSoHD() {
        return MauSoHD;
    }

    public void setMauSoHD(String mauSoHD) {
        MauSoHD = mauSoHD;
    }



	public String getKHMSHDon() {
        return KHMSHDon;
    }

    public void setKHMSHDon(String KHMSHDon) {
        this.KHMSHDon = KHMSHDon;
    }

    public String getKHHDon() {
        return KHHDon;
    }

    public void setKHHDon(String KHHDon) {
        this.KHHDon = KHHDon;
    }

    public LocalDateTime getNLap() {
        return NLap;
    }

    public void setNLap(LocalDateTime NLap) {
        this.NLap = NLap;
    }

    public String getDVTTe() {
        return DVTTe;
    }

    public void setDVTTe(String DVTTe) {
        this.DVTTe = DVTTe;
    }

    public String getTGia() {
        return TGia;
    }

    public void setTGia(String TGia) {
        this.TGia = TGia;
    }

    public String getHTTToanCode() {
        return HTTToanCode;
    }

    public void setHTTToanCode(String HTTToanCode) {
        this.HTTToanCode = HTTToanCode;
    }

    public String getHTTToan() {
        return HTTToan;
    }

    public void setHTTToan(String HTTToan) {
        this.HTTToan = HTTToan;
    }

}
