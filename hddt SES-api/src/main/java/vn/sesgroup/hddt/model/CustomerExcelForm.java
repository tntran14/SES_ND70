package vn.sesgroup.hddt.model;

public class CustomerExcelForm {

	  private String Mst;
	  private String TenCty;
    private String TenNguoiMua;
    private String DiaChi;
    private String DienThoai;
    private String MailMain;
    private String MailCC;
	public CustomerExcelForm() {
		super();
	}
	public CustomerExcelForm(String mst, String tenCty, String tenNguoiMua, String diaChi, String dienThoai,
			String mailMain, String mailCC) {
		super();
		Mst = mst;
		TenCty = tenCty;
		TenNguoiMua = tenNguoiMua;
		DiaChi = diaChi;
		DienThoai = dienThoai;
		MailMain = mailMain;
		MailCC = mailCC;
	}
	public String getMst() {
		return Mst;
	}
	public void setMst(String mst) {
		Mst = mst;
	}
	public String getTenCty() {
		return TenCty;
	}
	public void setTenCty(String tenCty) {
		TenCty = tenCty;
	}
	public String getTenNguoiMua() {
		return TenNguoiMua;
	}
	public void setTenNguoiMua(String tenNguoiMua) {
		TenNguoiMua = tenNguoiMua;
	}
	public String getDiaChi() {
		return DiaChi;
	}
	public void setDiaChi(String diaChi) {
		DiaChi = diaChi;
	}
	public String getDienThoai() {
		return DienThoai;
	}
	public void setDienThoai(String dienThoai) {
		DienThoai = dienThoai;
	}
	public String getMailMain() {
		return MailMain;
	}
	public void setMailMain(String mailMain) {
		MailMain = mailMain;
	}
	public String getMailCC() {
		return MailCC;
	}
	public void setMailCC(String mailCC) {
		MailCC = mailCC;
	}
	@Override
	public String toString() {
		return "CustomerExcelForm [Mst=" + Mst + ", TenCty=" + TenCty + ", TenNguoiMua=" + TenNguoiMua + ", DiaChi="
				+ DiaChi + ", DienThoai=" + DienThoai + ", MailMain=" + MailMain + ", MailCC=" + MailCC + "]";
	}
	
}
