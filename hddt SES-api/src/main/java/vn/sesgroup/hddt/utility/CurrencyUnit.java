package vn.sesgroup.hddt.utility;

public enum CurrencyUnit {
	USD("dollar", "cent", 2),
	EUR("euro", "cent", 2),
	GBP("pound", "pence", 2), 
	JPY("yen", null, 0),
	VND("dong", null, 0);

	private final String major;
	private final String minor;
	private final int scale;

	CurrencyUnit(String major, String minor, int scale) {
		this.major = major;
		this.minor = minor;
		this.scale = scale;
	}

	public String major(long value) {
		return value == 1 ? major : major + "s";
	}

	public String minor(int value) {
		if (minor == null)
			return "";
		return value == 1 ? minor : minor + "s";
	}

	public int getScale() {
		return scale;
	}
}
