package vn.sesgroup.hddt.model;

public class EInvoiceExcelForm {

    private String MaHD;
    private String MaSoThue;
    private String TenNguoiMua;
    private String TenDonVi;
    private String DiaChiKhachHang;
    private String MailKhachHang;
    private String SDTKhachHang;
    private String SoTaiKhoan;
    private String TenNganHang;
    private String HinhThucThanhToan;
    private String LoaiTien;
    private String STT;
    private String TenHangHoa;
    private String MaHangHoa;
    private String SLo;
    private String HanSD;
    private String DonViTinh;
    private Double SoLuong;
    private Double DonGia;
    private Double ThanhTien;
    private Double ThueSuat;
    private Double TienThue;
    private Double TongTien;
    private String TinhChat;
    private Double TongTienTruocThue;
    private String TyGia;
    private Double TTTGTGT;
    private Double TTDCTGTGT;
    private String TTBangChu;
    private String MaKH;

    public EInvoiceExcelForm(){
        super();
    }

    //Import Normal
    public EInvoiceExcelForm(String maHD, String maSoThue, String tenNguoiMua, String tenDonVi, String diaChiKhachHang,
			String mailKhachHang, String sDTKhachHang, String soTaiKhoan, String tenNganHang, String hinhThucThanhToan,
			String loaiTien, String sTT, String tenHangHoa, String maHangHoa, String sLo, String hanSD,
			String donViTinh, Double soLuong, Double donGia, Double thanhTien, Double thueSuat, Double tienThue,
			Double tongTien, String tinhChat, Double tongTienTruocThue, String tyGia, Double tTTGTGT, Double tTDCTGTGT,
			String tTBangChu, String maKH) {
		super();
		MaHD = maHD;
		MaSoThue = maSoThue;
		TenNguoiMua = tenNguoiMua;
		TenDonVi = tenDonVi;
		DiaChiKhachHang = diaChiKhachHang;
		MailKhachHang = mailKhachHang;
		SDTKhachHang = sDTKhachHang;
		SoTaiKhoan = soTaiKhoan;
		TenNganHang = tenNganHang;
		HinhThucThanhToan = hinhThucThanhToan;
		LoaiTien = loaiTien;
		STT = sTT;
		TenHangHoa = tenHangHoa;
		MaHangHoa = maHangHoa;
		SLo = sLo;
		HanSD = hanSD;
		DonViTinh = donViTinh;
		SoLuong = soLuong;
		DonGia = donGia;
		ThanhTien = thanhTien;
		ThueSuat = thueSuat;
		TienThue = tienThue;
		TongTien = tongTien;
		TinhChat = tinhChat;
		TongTienTruocThue = tongTienTruocThue;
		TyGia = tyGia;
		TTTGTGT = tTTGTGT;
		TTDCTGTGT = tTDCTGTGT;
		TTBangChu = tTBangChu;
		MaKH = maKH;
	}


	//Import Excel Auto get STT, ALL TOTAL ABOUT AMOUNT

	public EInvoiceExcelForm(String maHD, String maSoThue, String tenNguoiMua, String tenDonVi, String diaChiKhachHang,
			String mailKhachHang, String sDTKhachHang, String soTaiKhoan, String tenNganHang, String hinhThucThanhToan,
			String loaiTien, String tenHangHoa, String maHangHoa, String sLo, String HanSD, String donViTinh,
			Double soLuong, Double donGia, Double thanhTien, Double thueSuat, Double tienThue, String tinhChat,
			String tyGia) {
		super();
		MaHD = maHD;
		MaSoThue = maSoThue;
		TenNguoiMua = tenNguoiMua;
		TenDonVi = tenDonVi;
		DiaChiKhachHang = diaChiKhachHang;
		MailKhachHang = mailKhachHang;
		SDTKhachHang = sDTKhachHang;
		SoTaiKhoan = soTaiKhoan;
		TenNganHang = tenNganHang;
		HinhThucThanhToan = hinhThucThanhToan;
		LoaiTien = loaiTien;
		TenHangHoa = tenHangHoa;
		MaHangHoa = maHangHoa;
		SLo = sLo;
		HanSD = HanSD;
		DonViTinh = donViTinh;
		SoLuong = soLuong;
		DonGia = donGia;
		ThanhTien = thanhTien;
		ThueSuat = thueSuat;
		TienThue = tienThue;
		TinhChat = tinhChat;
		TyGia = tyGia;
	}





	public String getMaHD() {
        return MaHD;
    }
	public void setMaHD(String maHD) {
        MaHD = maHD;
    }

    public String getMaSoThue() {
        return MaSoThue;
    }

    public void setMaSoThue(String maSoThue) {
        MaSoThue = maSoThue;
    }

    public String getTenNguoiMua() {
        return TenNguoiMua;
    }

    public void setTenNguoiMua(String tenNguoiMua) {
        TenNguoiMua = tenNguoiMua;
    }

    public String getTenDonVi() {
        return TenDonVi;
    }

    public void setTenDonVi(String tenDonVi) {
        TenDonVi = tenDonVi;
    }

    public String getDiaChiKhachHang() {
        return DiaChiKhachHang;
    }

    public void setDiaChiKhachHang(String diaChiKhachHang) {
        DiaChiKhachHang = diaChiKhachHang;
    }

    public String getMailKhachHang() {
        return MailKhachHang;
    }

    public void setMailKhachHang(String mailKhachHang) {
        MailKhachHang = mailKhachHang;
    }

    public String getSDTKhachHang() {
        return SDTKhachHang;
    }

    public void setSDTKhachHang(String SDTKhachHang) {
        this.SDTKhachHang = SDTKhachHang;
    }

    public String getSoTaiKhoan() {
        return SoTaiKhoan;
    }

    public void setSoTaiKhoan(String soTaiKhoan) {
        SoTaiKhoan = soTaiKhoan;
    }

    public String getTenNganHang() {
        return TenNganHang;
    }

    public void setTenNganHang(String tenNganHang) {
        TenNganHang = tenNganHang;
    }

    public String getHinhThucThanhToan() {
        return HinhThucThanhToan;
    }

    public void setHinhThucThanhToan(String hinhThucThanhToan) {
        HinhThucThanhToan = hinhThucThanhToan;
    }

    public String getLoaiTien() {
        return LoaiTien;
    }

    public void setLoaiTien(String loaiTien) {
        LoaiTien = loaiTien;
    }

    public String getSTT() {
        return STT;
    }

    public void setSTT(String STT) {
        this.STT = STT;
    }

    public String getTenHangHoa() {
        return TenHangHoa;
    }

    public void setTenHangHoa(String tenHangHoa) {
        TenHangHoa = tenHangHoa;
    }

    public String getDonViTinh() {
        return DonViTinh;
    }

    public void setDonViTinh(String donViTinh) {
        DonViTinh = donViTinh;
    }

    public Double getSoLuong() {
        return SoLuong;
    }

    public void setSoLuong(Double soLuong) {
        SoLuong = soLuong;
    }

    public Double getDonGia() {
        return DonGia;
    }

    public void setDonGia(Double donGia) {
        DonGia = donGia;
    }

    public Double getThanhTien() {
        return ThanhTien;
    }

    public void setThanhTien(Double thanhTien) {
        ThanhTien = thanhTien;
    }

    public Double getThueSuat() {
        return ThueSuat;
    }

    public void setThueSuat(Double thueSuat) {
        ThueSuat = thueSuat;
    }

    public Double getTienThue() {
        return TienThue;
    }

    public void setTienThue(Double tienThue) {
        TienThue = tienThue;
    }

    public Double getTongTien() {
        return TongTien;
    }

    public void setTongTien(Double tongTien) {
        TongTien = tongTien;
    }

    public String getTinhChat() {
        return TinhChat;
    }

    public void setTinhChat(String tinhChat) {
        TinhChat = tinhChat;
    }

    public Double getTongTienTruocThue() {
        return TongTienTruocThue;
    }

    public void setTongTienTruocThue(Double tongTienTruocThue) {
        TongTienTruocThue = tongTienTruocThue;
    }

    public String getTyGia() {
        return TyGia;
    }

    public void setTyGia(String tyGia) {
        TyGia = tyGia;
    }

    public Double getTTTGTGT() {
        return TTTGTGT;
    }

    public void setTTTGTGT(Double TTTGTGT) {
        this.TTTGTGT = TTTGTGT;
    }

    public Double getTTDCTGTGT() {
        return TTDCTGTGT;
    }

    public void setTTDCTGTGT(Double TTDCTGTGT) {
        this.TTDCTGTGT = TTDCTGTGT;
    }

    public String getTTBangChu() {
        return TTBangChu;
    }

    public void setTTBangChu(String TTBangChu) {
        this.TTBangChu = TTBangChu;
    }

    public String getMaKH() {
		return MaKH;
	}

	public void setMaKH(String maKH) {
		MaKH = maKH;
	}

	
	public String getMaHangHoa() {
		return MaHangHoa;
	}





	public void setMaHangHoa(String maHangHoa) {
		MaHangHoa = maHangHoa;
	}





	public String getSLo() {
		return SLo;
	}





	public void setSLo(String sLo) {
		SLo = sLo;
	}





	public String getHanSD() {
		return HanSD;
	}





	public void setHanSD(String hanSD) {
		HanSD = hanSD;
	}





	@Override
	public String toString() {
		return "EInvoiceExcelForm [MaHD=" + MaHD + ", MaSoThue=" + MaSoThue + ", TenNguoiMua=" + TenNguoiMua
				+ ", TenDonVi=" + TenDonVi + ", DiaChiKhachHang=" + DiaChiKhachHang + ", MailKhachHang=" + MailKhachHang
				+ ", SDTKhachHang=" + SDTKhachHang + ", SoTaiKhoan=" + SoTaiKhoan + ", TenNganHang=" + TenNganHang
				+ ", HinhThucThanhToan=" + HinhThucThanhToan + ", LoaiTien=" + LoaiTien + ", STT=" + STT
				+ ", TenHangHoa=" + TenHangHoa + ", MaHangHoa=" + MaHangHoa + ", SLo=" + SLo + ", HanSD=" + HanSD
				+ ", DonViTinh=" + DonViTinh + ", SoLuong=" + SoLuong + ", DonGia=" + DonGia + ", ThanhTien="
				+ ThanhTien + ", ThueSuat=" + ThueSuat + ", TienThue=" + TienThue + ", TongTien=" + TongTien
				+ ", TinhChat=" + TinhChat + ", TongTienTruocThue=" + TongTienTruocThue + ", TyGia=" + TyGia
				+ ", TTTGTGT=" + TTTGTGT + ", TTDCTGTGT=" + TTDCTGTGT + ", TTBangChu=" + TTBangChu + ", MaKH=" + MaKH
				+ "]";
	}





}
