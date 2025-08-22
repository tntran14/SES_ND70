package vn.sesgroup.hddt.model;


import lombok.Data;

@Data
public class DSHHDVu {
	private String STT;
	private String ProductName;
	private String ProductCode;
	private String SLo;
	private String HanSD;
	private String Unit;
	private Double Quantity;
	private Double TNhap;
	private Double Price;
	private Double TTien;
	private Double Total;
	private Double VATRate;
	private Double VATAmount;
	private Double Amount;
	private String Feature;
	private String LHHDTrung;
	private String SKhung;
	private String SMay;
	private String BKSPTVChuyen;
	private String TNGHang;
	private String DCNGHang;
	private String MSTNGHang;
	private String MDDNGHang;

	public DSHHDVu() {
	}

	public DSHHDVu(String sTT, String productName, String productCode, String sLo, String hanSD, String unit,
			Double quantity, Double price, Double total, Double vATRate, Double vATAmount, Double amount,
			String feature) {
		super();
		STT = sTT;
		ProductName = productName;
		ProductCode = productCode;
		SLo = sLo;
		HanSD = hanSD;
		Unit = unit;
		Quantity = quantity;
		Price = price;
		Total = total;
		VATRate = vATRate;
		VATAmount = vATAmount;
		Amount = amount;
		Feature = feature;
	}
	public DSHHDVu(String sTT, String productName, String productCode, String sLo, String hanSD, String unit,
			Double quantity, Double price, Double tTien, Double total, String feature) {
		super();
		STT = sTT;
		ProductName = productName;
		ProductCode = productCode;
		SLo = sLo;
		HanSD = hanSD;
		Unit = unit;
		Quantity = quantity;
		Price = price;
		TTien = tTien;
		Total = total;
		Feature = feature;
	}

	public DSHHDVu(String sTT, String productName, String productCode, String sLo, String hanSD, String unit,
			Double quantity, Double tNhap, Double price, Double tTien, Double total, Double vATRate, Double vATAmount,
			Double amount, String feature) {
		super();
		STT = sTT;
		ProductName = productName;
		ProductCode = productCode;
		SLo = sLo;
		HanSD = hanSD;
		Unit = unit;
		Quantity = quantity;
		TNhap = tNhap;
		Price = price;
		TTien = tTien;
		Total = total;
		VATRate = vATRate;
		VATAmount = vATAmount;
		Amount = amount;
		Feature = feature;
	}

	public DSHHDVu(String sTT, String productName, String productCode, String sLo, String hanSD, String unit,
			Double quantity, Double tNhap, Double price, Double tTien, Double total, Double vATRate, Double vATAmount,
			Double amount, String feature, String tTHHDTrung, String sKhung, String sMay, String bKSPTVChuyen,
			String tNGHang, String dCNGHang, String mSTNGHang, String mDDNGHang) {
		super();
		STT = sTT;
		ProductName = productName;
		ProductCode = productCode;
		SLo = sLo;
		HanSD = hanSD;
		Unit = unit;
		Quantity = quantity;
		TNhap = tNhap;
		Price = price;
		TTien = tTien;
		Total = total;
		VATRate = vATRate;
		VATAmount = vATAmount;
		Amount = amount;
		Feature = feature;
		LHHDTrung = tTHHDTrung;
		SKhung = sKhung;
		SMay = sMay;
		BKSPTVChuyen = bKSPTVChuyen;
		TNGHang = tNGHang;
		DCNGHang = dCNGHang;
		MSTNGHang = mSTNGHang;
		MDDNGHang = mDDNGHang;
	}

	

}
