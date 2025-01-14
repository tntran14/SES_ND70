package vn.sesgroup.hddt.utility;

import java.util.HashMap;
import java.util.LinkedHashMap;

import io.jsonwebtoken.Header;

public class Constants {
	public static final char[] VALID_CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456879".toCharArray();
	public static final char[] VALID_NUMBERS = "0123456879".toCharArray();
	
	public static final String TOKEN_HEADER = "Authorization";
	public static final String TOKEN_PREFIX = "Bearer ";
	public static final String TOKEN_TYPE = Header.JWT_TYPE;
	public static final String TOKEN_ISSUER = "secure-api";
	public static final String TOKEN_AUDIENCE = "secure-app";
	public static final String TOKEN_SUBJECT = "Token For API CRM";
	public static final String JWT_SECRET = "sTg3Ge82o78EUYEgAM8RuGj4tZIh3ZqBLXQhcQIzZ0wwtRv694RLRUJHeZK3Nu30eMmULpUiIyNxFQTH42Z7UKRg88hzdF8GIJ4a";
	public static final long EXPIRATION_TIME = 30; // 30 phut
	
	public static final String REQ_API_LICENSE_KEY_NAME = "api-license-key";
	
	public static final HashMap<Integer, String> MAP_ERROR = new HashMap<Integer, String>(){
		private static final long serialVersionUID = 6987923652719538171L;
		{put(0, "SUCCESS");}
		{put(9998, "Không tìm thấy yêu cầu xử lý.");}
		{put(9999, "Không tìm thấy dữ liệu.");}
	};
	
	public static final class FORMAT_DATE {
		public static final String DEFAULT_DATE = "01/01/1900";
		public static final String FORMAT_DATE_WEB = "dd/MM/yyyy";
		public static final String FORMAT_DATE_TIME_WEB = "dd/MM/yyyy HH:mm:ss";
		public static final String FORMAT_DATE_TIME_YYYYMMDD_HHMMSS = "yyyyMMdd'-'HHmmssSSS";
		public static final String FORMAT_DATE_EINVOICE = "yyyy-MM-dd";
		public static final String FORMAT_DATETIME_EINVOICE = "yyyy-MM-dd'T'HH:mm:ss";
		public static final String FORMAT_DATE_DB = "yyyyMMdd";
	}
	
	public static final class MSG_ACTION_CODE {
		public static final String LOAD_PARAMS = "LOAD-PARAMETERS";
		public static final String SEARCH = "SEARCH";
		public static final String INQUIRY = "INQUIRY";
		public static final String CREATED = "CREATED";
		public static final String MODIFY = "MODIFY";
		public static final String MODIFY_CONTRACT = "MODIFY_CONTRACT";
		public static final String ACTIVE = "ACTIVE";
		public static final String DEACTIVE = "DEACTIVE";
		public static final String ACTIVE_OR_DEACTIVE = "ACTIVE_OR_DEACTIVE";
		public static final String DELETE = "DELETE";
		public static final String SEND = "SEND";
		public static final String UPDATE_INFO = "UPDATE_INFO";
		public static final String RESET_PASSWORD = "RESET_PASSWORD";
		public static final String COPY = "COPY";
		public static final String DELETE_ATTACH_FILES = "DELETE_ATTACH_FILES";
		public static final String SAVE_ATTACH_FILES = "SAVE_ATTACH_FILES";
		public static final String XoaBo = "XoaBo";
		public static final String APPROVE = "APPROVE";
		public static final String CANCEL = "CANCEL";
		public static final String CREATED_AND_APPROVE = "CREATED_AND_APPROVE";
		public static final String SEND_CQT = "SEND_CQT";
		public static final String CHECK = "CHECK";
		public static final String DELETEALL = "DELETEALL";
		public static final String SEND_CQTALL = "SEND_CQTALL";
	}
	
	public static final LinkedHashMap<String, String> MAP_VAT = new LinkedHashMap<String, String>(){
		private static final long serialVersionUID = -4829089663504096703L;
		{put("0", "0%");}
		{put("5", "5%");}
		{put("8", "8%");}
		{put("10", "10%");}
		{put("-1", "KCT");}
		{put("-2", "KKKNT");}
	};
	
	public static final class INVOICE_SIGN_STATUS {
		public static final String NOSIGN = "NOSIGN";
		public static final String SIGNED = "SIGNED";
	}
	
	public static final class INVOICE_STATUS {
		public static final String CREATED = "CREATED";
		public static final String USING = "USING";
		public static final String DELETED = "DELETED";
		public static final String XOABO = "XOABO";
		public static final String REPLACED = "REPLACED";
		public static final String ADJUSTED = "ADJUSTED";
		public static final String PENDING = "PENDING";
		public static final String PROCESSING = "PROCESSING";
		public static final String COMPLETE = "COMPLETE";
		public static final String ERROR_CQT = "ERROR_CQT";
		
		public static final String TK_CREATED = "CREATED";
	}
	
	public static final LinkedHashMap<String, String> MAP_HDSS_TCTBAO = new LinkedHashMap<String, String>(){
		private static final long serialVersionUID = 4659557633581867222L;
		
		{put("1", "Hủy");}
		{put("2", "Điều chỉnh");}
		{put("3", "Thay thế");}
		{put("4", "Giải trình");}
		
	};
	
	public static final class TEMPLATE_FILE_NAME {
		public static final String IMG_SIGNATURE_INVALID = "signature-invalid.png";
		public static final String IMG_SIGNATURE_VALID = "signature-valid.png";
		public static final String IMG_INV_DELETED = "img-inv-deleted-no-bg.png";
		public static final String IMG_INV_THAYTHE = "inv-bi-thay-th.png";
		public static final String IMG_INV_DIEUCHINH = "inv-bi-dieu-chinh.png";
		
		public static final String EXCEL_TKDSHD_CTIET = "thong-ke-chi-tiet-danh-sach-hoa-don.xlsx";
		public static final String EXCEL_TKDSHD_GENERAL = "Report-BKHD-General.xlsx";
		
		public static final String EXCEL_TKDSHD_PXKVCNB_CTIET = "Thong ke chi tiet_PXK.xlsx";
		
		public static final String EXCEL_TKDSHDBH_CTIET = "Thong-ke-chi-tiet-danh-sach-hoa-don-ban-hang.xlsx";
		public static final String EXCEL_TKDSHDBH_GENERAL = "Report-BKHDBH-General.xlsx";
		
		public static final String IMG_INV_XoaBo = "img-inv-xoabo-no-bg.png";
		
		public static final String EXCEL_TKDSHD_PXKVCNB_GENERAL = "Report-BKHD-General_1.xlsx";
		
		public static final String EXCEL_TKDSHD_PXKVCDL_CTIET = "Thong ke chi tiet_PXKDL.xlsx";
		public static final String EXCEL_TKCNTNT = "Thong-Ke-Chung-Tu-Khau-Tru-Thue.xlsx";

		public static final String EXCEL_SENDMAILHD_CTIET = "chi-tiet-danh-sach-gui-mai-hoa-don.xlsx";
		
		public static final String EXCEL_TKDSHD_MISA_SME_2015 = "Ban_hang_MISA_SME_NET_2015.xlsx";
	}
	
	public static final class TDiep_TTChung_TCTN_VISNAM{
		public static final String PBan1 = "2.0.1";
		public static final String PBan = "2.0.0";
		public static final String MNGui = "MÃ NƠI GỬI: MST BEN GIẢI PHÁP";
		public static final String MNNhan = "0401486901";
		public static final String MLTDiep = "MA THONG DIEP - THEO QUI DINH";
		public static final String MTDiep = "(46) MST BÊN GIẢI PHÁP + RANDOM";	 //RND(46 - MST.LENGTH)
		public static final String MTDTChieu = "(46) NHAN DU LIEU TU BEN VISNAM";
		public static final String MST = "MST NGUOI NOP THUE";
		public static final String SLuong = "SO LUONG THONG DIEP";
	}
	
	public static final class SendMailFromFunc{
		public static final String Single = "Single";
		public static final String Bulk = "Bulk";
	}
	
}
