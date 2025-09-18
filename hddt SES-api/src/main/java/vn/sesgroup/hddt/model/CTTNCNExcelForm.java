package vn.sesgroup.hddt.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@AllArgsConstructor
@Builder
public class CTTNCNExcelForm {
	private String hovatennhanvien;
	private String mstnhanvien;
	private String manhanvien;
	private String diachi;
	private String cccd;
	private String quoctich;
	private String sodienthoai;
	private String email;
	private String emailcc;
	private String canhancutru;
	private String nam;
	private String tuthang;
	private String denthang;
	private String khoanthunhap;
	private String baohiem;
	private String khoantuthien;
	private String tongthunhapchiuthue;
	private String tongthunhaptinhthue;
	private String sothue;

	public CTTNCNExcelForm() {
	}
}
