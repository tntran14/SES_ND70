package vn.sesgroup.hddt.model;

public class NMua {
    private String Ten;
    private String MST;
    private String DChi;
    private String NDXkho;
    private String SDThoai;
    private String DCTDTu;
    private String STKNHang;
    private String TNHang;
    private String MaKH;
    private String HVTNMHang;

    public NMua(){}

    public NMua(String ten, String MST, String DChi, String SDThoai, String DCTDTu, String STKNHang, String TNHang, String MaKH, String HVTNMHang) {
        Ten = ten;
        this.MST = MST;
        this.DChi = DChi;
        this.SDThoai = SDThoai;
        this.DCTDTu = DCTDTu;
        this.STKNHang = STKNHang;
        this.TNHang = TNHang;
        this.MaKH = MaKH;
        this.HVTNMHang = HVTNMHang;
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
    public String getMaKH() {
        return MaKH;
    }

    public void setMaKH(String MaKH) {
        this.MaKH = MaKH;
    }
    public String getHVTNMHang() {
        return HVTNMHang;
    }

    public void setHVTNMHang(String HVTNMHang) {
        this.HVTNMHang = HVTNMHang;
    }

	public String getNDXkho() {
		return NDXkho;
	}

	public void setNDXkho(String nDXkho) {
		NDXkho = nDXkho;
	}

	public NMua(String ten, String mST, String dChi, String nDXkho, String sDThoai, String dCTDTu, String sTKNHang,
			String tNHang, String maKH, String hVTNMHang) {
		super();
		Ten = ten;
		MST = mST;
		DChi = dChi;
		NDXkho = nDXkho;
		SDThoai = sDThoai;
		DCTDTu = dCTDTu;
		STKNHang = sTKNHang;
		TNHang = tNHang;
		MaKH = maKH;
		HVTNMHang = hVTNMHang;
	}


    
	
}
