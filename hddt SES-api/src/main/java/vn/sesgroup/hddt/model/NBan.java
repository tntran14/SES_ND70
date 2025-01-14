package vn.sesgroup.hddt.model;

import lombok.Data;

@Data
public class NBan {
    private String Ten;
    private String MST;
    private String LDDNBo;
    private String DChi;
    private String SDThoai;
    private String HVTNVChuyen;
    private String PTVChuyen;
    private String TNDDien;
    private String DCTDTu;
    private String STKNHang;
    private String TNHang;
    private String Fax;
    private String Website;

    public NBan(){}


    public NBan(String ten, String MST, String DChi, String SDThoai, String DCTDTu, String STKNHang, String TNHang, String fax, String website) {
        Ten = ten;
        this.MST = MST;
        this.DChi = DChi;
        this.SDThoai = SDThoai;
        this.DCTDTu = DCTDTu;
        this.STKNHang = STKNHang;
        this.TNHang = TNHang;
        Fax = fax;
        Website = website;
    }

    public String getTen() {
        return Ten;
    }

    public void setTen(String ten) {
        Ten = ten;
    }

    public String getMST() {
        return MST;
    }

    public void setMST(String MST) {
        this.MST = MST;
    }

    public String getDChi() {
        return DChi;
    }

    public void setDChi(String DChi) {
        this.DChi = DChi;
    }

    public String getSDThoai() {
        return SDThoai;
    }

    public void setSDThoai(String SDThoai) {
        this.SDThoai = SDThoai;
    }

    public String getDCTDTu() {
        return DCTDTu;
    }

    public void setDCTDTu(String DCTDTu) {
        this.DCTDTu = DCTDTu;
    }

    public String getSTKNHang() {
        return STKNHang;
    }

    public void setSTKNHang(String STKNHang) {
        this.STKNHang = STKNHang;
    }

    public String getTNHang() {
        return TNHang;
    }

    public void setTNHang(String TNHang) {
        this.TNHang = TNHang;
    }

    public String getFax() {
        return Fax;
    }

    public void setFax(String fax) {
        Fax = fax;
    }

    public String getWebsite() {
        return Website;
    }

    public void setWebsite(String website) {
        Website = website;
    }


	public String getLDDNBo() {
		return LDDNBo;
	}


	public void setLDDNBo(String lDDNBo) {
		LDDNBo = lDDNBo;
	}


	public String getHVTNVChuyen() {
		return HVTNVChuyen;
	}


	public void setHVTNVChuyen(String hVTNVChuyen) {
		HVTNVChuyen = hVTNVChuyen;
	}


	public String getPTVChuyen() {
		return PTVChuyen;
	}


	public void setPTVChuyen(String pTVChuyen) {
		PTVChuyen = pTVChuyen;
	}


	public String getTNDDien() {
		return TNDDien;
	}


	public void setTNDDien(String tNDDien) {
		TNDDien = tNDDien;
	}


	public NBan(String ten, String mST, String lDDNBo, String dChi, String sDThoai, String hVTNVChuyen,
			String pTVChuyen, String tNDDien, String dCTDTu, String sTKNHang, String tNHang, String fax,
			String website) {
		super();
		Ten = ten;
		MST = mST;
		LDDNBo = lDDNBo;
		DChi = dChi;
		SDThoai = sDThoai;
		HVTNVChuyen = hVTNVChuyen;
		PTVChuyen = pTVChuyen;
		TNDDien = tNDDien;
		DCTDTu = dCTDTu;
		STKNHang = sTKNHang;
		TNHang = tNHang;
		Fax = fax;
		Website = website;
	}
    
}
