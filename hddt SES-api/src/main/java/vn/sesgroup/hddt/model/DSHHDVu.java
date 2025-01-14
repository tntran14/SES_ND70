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

    public DSHHDVu(){}



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



	public String getSTT() {
        return STT;
    }

    public void setSTT(String STT) {
        this.STT = STT;
    }

    public String getProductName() {
        return ProductName;
    }

    public void setProductName(String productName) {
        ProductName = productName;
    }

    public String getUnit() {
        return Unit;
    }

    public void setUnit(String unit) {
        Unit = unit;
    }

    public Double getQuantity() {
        return Quantity;
    }

    public void setQuantity(Double quantity) {
        Quantity = quantity;
    }

    
    
    public Double getTNhap() {
		return TNhap;
	}

	public void setTNhap(Double tNhap) {
		TNhap = tNhap;
	}



	public Double getPrice() {
        return Price;
    }

    public void setPrice(Double price) {
        Price = price;
    }

    public Double getTotal() {
        return Total;
    }

    public void setTotal(Double total) {
        Total = total;
    }

    public Double getVATRate() {
        return VATRate;
    }

    public void setVATRate(Double VATRate) {
        this.VATRate = VATRate;
    }

    public Double getVATAmount() {
        return VATAmount;
    }

    public void setVATAmount(Double VATAmount) {
        this.VATAmount = VATAmount;
    }

    public Double getAmount() {
        return Amount;
    }

    public void setAmount(Double amount) {
        Amount = amount;
    }

    public String getFeature() {
        return Feature;
    }

    public void setFeature(String feature) {
        Feature = feature;
    }

	public String getProductCode() {
		return ProductCode;
	}

	public void setProductCode(String productCode) {
		ProductCode = productCode;
	}

	
	public Double getTTien() {
		return TTien;
	}

	public void setTTien(Double tTien) {
		TTien = tTien;
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
      
}
