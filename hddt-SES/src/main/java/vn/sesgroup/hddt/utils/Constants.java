package vn.sesgroup.hddt.utils;

import java.util.HashMap;
import java.util.LinkedHashMap;

public class Constants {
	public static final char[] VALID_CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456879".toCharArray();
	public static final char[] VALID_NUMBERS = "0123456879".toCharArray();
	
	public static final String PREFIX_TITLE = "HĐĐT";
	public static final String TOKEN_HEADER = "Authorization";
	public static final String SESSION_CAPTCHA = "SESSION_CAPTCHA";
	
	public static final String ADD_TRANSACTION = "transaction";
	public static final String ADD_METHOD = "method";
	public static final String HAVE_ACTION_NAME = "HAVE_ACTION_NAME";
	
	public static final String HEADER_RESULT_TYPE_NAME = "RESULT_TYPE";
	public static final class HEADER_RESULT_TYPES{
		public static final String JSON = "JSON";
		public static final String NORMAL = "PAGE";
		public static final String JSON_ARRAY = "JSON_ARRAY";		//USING FOR COMBOBOX
		public static final String JSON_GRID = "JSON_GRID";			//USING FOR GRID
		public static final String PAGE_POPUP = "PAGE-POPUP";		//USING FOR PAGE POPUP
		public static final String PAGE_AREA = "PAGE-AREA";		//USING FOR PAGE POPUP
	}
	
	public static final class SESSION_TYPE {
		public static final String SESSION_TOKEN_EXECUTE = "SESSION_TOKEN_EXECUTE";
		public static final String SESSION_FORM_ACTION = "SESSION_FORM_ACTION";
		public static final String SESSION_FULL_MENU_RIGHTS = "SESSION_FULL_MENU_RIGHTS";
	}
	
	public static final class MSG_ACTION_CODE {
		public static final String LOAD_PARAMS = "LOAD-PARAMETERS";
		public static final String SEARCH = "SEARCH";
		public static final String MTD = "MTD";
		public static final String INQUIRY = "INQUIRY";
		public static final String CREATED = "CREATED";
		public static final String MODIFY = "MODIFY";
		public static final String MODIFY_CONTRACT = "MODIFY_CONTRACT";
		public static final String ACTIVE = "ACTIVE";
		public static final String DEACTIVE = "DEACTIVE";
		public static final String ACTIVE_OR_DEACTIVE = "ACTIVE_OR_DEACTIVE";
		public static final String DELETE = "DELETE";
		public static final String XoaBo = "XoaBo";
		public static final String SEND = "SEND";
		public static final String UPDATE_INFO = "UPDATE_INFO";
		public static final String RESET_PASSWORD = "RESET_PASSWORD";
		public static final String COPY = "COPY";
		public static final String DELETE_ATTACH_FILES = "DELETE_ATTACH_FILES";
		public static final String SAVE_ATTACH_FILES = "SAVE_ATTACH_FILES";
		
		public static final String APPROVE = "APPROVE";
		public static final String CANCEL = "CANCEL";
		public static final String CREATED_AND_APPROVE = "CREATED_AND_APPROVE";
		public static final String SIGNED = "SIGNED";
		public static final String SEND_CQT = "SEND_CQT";
		public static final String CHECK = "CHECK";
		public static final String DELETEALL = "DELETEALL";
		public static final String SIGNALL = "SIGNALL";
		public static final String CREATEDPARAM = "CREATEDPARAM";
		public static final String REPORT = "REPORT";
		public static final String SEND_CQTALL = "SEND_CQTALL";
	}
	
	public static final class FORMAT_DATE {
		public static final String DEFAULT_DATE = "01/01/1900";
		public static final String FORMAT_DATE_WEB = "dd/MM/yyyy";
		public static final String FORMAT_DATE_TIME_WEB = "dd/MM/yyyy HH:mm:ss";
		public static final String FORMAT_DATETIME_DB_FULL = "yyyyMMddHHmmss";
		public static final String FORMAT_DATE_DB = "yyyyMMdd";
		public static final int RANGE_DATE_EXPORT = 365;
	}
	
	public static final HashMap<Integer, String> MAP_ERROR = new HashMap<Integer, String>(){
		private static final long serialVersionUID = 6987923652719538171L;
		{put(0, "SUCCESS");}
		{put(1, "Vui lòng nhập vào đầy đủ thông tin đăng nhập.");}
		{put(2, "Mã kiểm tra không đúng.");}
		
		{put(997, "Token giao dịch không hợp lệ.");}
		{put(998, "Không tìm thấy chức năng giao dịch.");}
		{put(999, "Thông tin nhập vào chưa đúng. Vui lòng kiểm tra lại.");}
	};
	
	public static final HashMap<String, String> MAP_EINVOICE_SIGN_STATUS = new HashMap<String, String>(){
		private static final long serialVersionUID = -4829089663504096703L;
		{put("NOSIGN", "Chưa ký");}
		{put("SIGNED", "Đã ký");}
	};
	public static final HashMap<String, String> MAP_STATUS = new HashMap<String, String>(){
		private static final long serialVersionUID = -4829089663504096703L;
		{put("true", "Hoạt động");}
		{put("false", "Chưa Hoạt động");}
		{put("DELETE", "Đã xóa");}
	};
	public static final HashMap<String, String> MAP_STATUSLoaiHD = new HashMap<String, String>(){
		private static final long serialVersionUID = -4829089663504096703L;
		{put("1", "Hóa đơn giá trị gia tăng");}
		{put("2", "Hóa đơn bán hàng");}
		{put("3", "Hóa đơn bán tài sản công");}
		{put("4", "Hóa đơn bán hàng dự trữ quốc gia");}
		{put("5", "Hóa đơn theo nghị định số 123/2020/NĐ-CP");}
		{put("6", "Hóa đơn Phiếu xuất kho(Kiêm vận chuyển nội bộ - Hàng gửi bán đại lý điện tử");}
	};
	public static final LinkedHashMap<String, String> MAP_EINVOICE_STATUS = new LinkedHashMap<String, String>(){
		private static final long serialVersionUID = -4829089663504096703L;
		{put("CREATED", "Mới tạo");}
		{put("PENDING", "Chưa được ký");}
		{put("PROCESSING", "Đang xử lý từ CQT");}
		{put("COMPLETE", "Đã phát hành");}
		{put("ERROR_CQT", "Lỗi từ CQT");}
		{put("REPLACED", "Đã thay thế");}
		{put("ADJUSTED", "Đã điều chỉnh");}
		{put("XOABO", "Đã xóa bỏ");}
		{put("DELETED", "Đã hủy");}
	};
	
	public static final LinkedHashMap<String, String> MAP_HDSS_STATUS = new LinkedHashMap<String, String>(){
		private static final long serialVersionUID = -4829089663504096703L;
		{put("CREATED", "Mới tạo");}
		{put("PENDING", "Đang xử lý");}
		{put("PROCESSING", "Đang xử lý từ CQT");}
		{put("COMPLETE", "CQT tiếp nhận");}
		{put("ERROR_CQT", "Lỗi từ CQT");}
		{put("REPLACED", "Đã thay thế");}
		{put("ADJUSTED", "Đã điều chỉnh");}
		{put("DELETED", "Đã xóa bỏ");}
	};
	
	public static final LinkedHashMap<String, String> MAP_TKHAI_STATUS = new LinkedHashMap<String, String>(){
		private static final long serialVersionUID = -4829089663504096703L;
		{put("CREATED", "Mới tạo");}
		{put("PROCESSING", "Đang xử lý");}
		{put("COMPLETE", "Hoàn tất");}
		{put("ERROR_CQT", "Lỗi từ CQT");}
	};
	
	public static final LinkedHashMap<String, String> MAP_VAT = new LinkedHashMap<String, String>(){
		private static final long serialVersionUID = -4829089663504096703L;
		{put("0", "0%");}
		{put("5", "5%");}
		{put("8", "8%");}
		{put("10", "10%");}
		{put("-1", "KCT");}
		{put("-2", "KKKNT");}
	};
	
	public static final HashMap<String, String> MAP_TKHAI_STATUS_CQT = new LinkedHashMap<String, String>(){
		private static final long serialVersionUID = -4829089663504096703L;
		{put("102", "Đã tiếp nhận");}
		{put("103", "Đã chấp nhận");}
	};
	
	public static final LinkedHashMap<String, String> MAP_LOAITB_HDSS = new LinkedHashMap<String, String>(){
		private static final long serialVersionUID = -4829089663504096703L;
		{put("1", "Thông báo hủy/giải trình của NNT");}
		{put("2", "Thông báo hủy/giải trình của NNT theo thông báo của CQT");}
	};
	
//	public static final HashMap<String, String> MAP_PRD_VAT = new LinkedHashMap<String, String>(){
//		private static final long serialVersionUID = 356826940981839047L;
//		{put("0", "0%");}
//		{put("5", "5%");}
//		{put("10", "10%");}
//		{put("-1", "KCT");}
//		{put("-2", "KKKNT");}
//	};
	
	public static final HashMap<String, String> MAP_PRD_FEATURE = new LinkedHashMap<String, String>(){
		private static final long serialVersionUID = 1L;
		{put("1", "HH, DV");}
		{put("2", "KM");}
		{put("3", "CK");}
		{put("4", "Ghi chú");}
	};
	
	public static final HashMap<String, String> MAP_TKHAI_HTHUC = new LinkedHashMap<String, String>(){
		private static final long serialVersionUID = 1L;
		{put("1", "Thêm mới");}
		{put("2", "Gia hạn");}
		{put("3", "Ngừng sử dụng");}
	};
	
	public static final HashMap<String, String> MAP_HDSS_LOAI_AD_HDDT = new LinkedHashMap<String, String>(){
		private static final long serialVersionUID = 1L;
		{put("1", "Theo Nghị định số 123/2020/NĐ-CP");}
	};
	public static final HashMap<String, String> MAP_HDSS_TCTBAO = new LinkedHashMap<String, String>(){
		private static final long serialVersionUID = 1L;
		{put("1", "Hủy");}
		{put("2", "Điều chỉnh");}
		{put("3", "Thay thế");}
		{put("4", "Giải trình");}
	};
	
	
	public static final HashMap<String, String> MAP_HD = new LinkedHashMap<String, String>(){
		private static final long serialVersionUID = 1L;
		{put("1", "Hóa đơn giá trị gia tăng");}
		{put("2", "Hóa đơn bán hàng");}
		{put("3", "Hóa đơn bán tài sản công");}
		{put("4", "Hóa đơn bán hàng dự trữ quốc gia");}
		{put("5", "Hóa đơn theo nghị định số 123/2020/NĐ-CP");}
		{put("6", "Hóa đơn Phiếu xuất kho(Kiêm vận chuyển nội bộ - Hàng gửi bán đại lý điện tử");}
		{put("7", "Hóa đơn chứng từ");}
	};
	public static final class REGEX_CHECK{
		public static final String STRING_CODE = "^[A-Z0-9][A-Z0-9_-]*$";
		public static final String STRING_IS_USERNAME = "^([A-Z]{1})[A-Z0-9_\\-\\.]*$";
		public static final String STRING_IS_POSITIONS = "^[A-Z][A-Z0-9_\\-\\.]*$";
	}
	
	public static final class FILE_DOWNLOAD_TEMPLATE{
		public static final String SIGN_PLUGIN = "SES-Invoice-Sign.zip";
	}
	
	public static final String ERROR_DESCRIPTION_EXPORT_EXCEL = "Lỗi trong quá trình xuất dữ liệu. Vui lòng liên hệ Admin để xử lý.";

	public static final HashMap<String, String> MAP_STATUSEMAIL = new HashMap<String, String>(){
		private static final long serialVersionUID = -4829089663504096703L;
		{put("true", "Đã Gửi");}
		{put("false", "Bị Lỗi");}
	};
	public static final HashMap<String, String> MAP_STATUSEMAILCHECK = new HashMap<String, String>(){
		private static final long serialVersionUID = -4829089663504096703L;
		{put("true", "");}
		{put("false", "Mail không tồn tại");}
	};

	
	
	public static final HashMap<String, String> MAP_MS_STATUS = new HashMap<String, String>(){
		private static final long serialVersionUID = -4829089663504096703L;
		{put("true", "Đã gia hạn");}
		{put("false", "Chưa gia hạn");}
	};
	
	public static final HashMap<String, String> MAP_STATUS_APPROVE = new HashMap<String, String>(){
		private static final long serialVersionUID = -4829089663504096703L;
		{put("true", "Đã duyệt");}
		{put("false", "Chưa duyệt");}
	};
	
	public static final HashMap<String, String> MAP_EINVOICE_PUBLISH_STATUS = new HashMap<String, String>(){
		private static final long serialVersionUID = -4829089663504096703L;
		{put("true", "Đã phát hành");}
		{put("false", "Chưa phát hành");}
	};
	
	
	public static final HashMap<String, String> MAP_ISSUER_STATUS = new HashMap<String, String>(){
		private static final long serialVersionUID = -4829089663504096703L;
		{put("true", "Đã kích hoạt");}
		{put("false", "Chưa kích hoạt");}
	};
	
	public static final HashMap<String, String> MAP_EINVOICE_EMAIL_STATUS = new HashMap<String, String>(){
		private static final long serialVersionUID = -4829089663504096703L;
		{put("true", "Đã gửi");}
		{put("false", "Chưa gửi");}
	};
	
	public static final HashMap<String, String> MAP_EINVOICE_SENDCQT_STATUS = new HashMap<String, String>(){
		private static final long serialVersionUID = -4829089663504096703L;
		{put("true", "Đã gửi");}
		{put("false", "Chưa gửi");}
	};
	public static final LinkedHashMap<String, String> MAP_STATUS_SENDCQT = new LinkedHashMap<String, String>(){
		private static final long serialVersionUID = -4829089663504096703L;
		{put("CREATED", "Mới tạo");}
		{put("PENDING", "Chưa gửi CQT");}
		{put("PROCESSING", "Gửi CQT thành công");}
		{put("COMPLETE", "CQT chấp nhận");}
		{put("ERROR_CQT", "Lỗi từ CQT");}
	
	};
	
	public static final LinkedHashMap<String, String> MAP_EINVOICE_RESPONSE_STATUS = new LinkedHashMap<String, String>(){
		private static final long serialVersionUID = -4829089663504096703L;
		{put("PROCESSING", "Đang xử lý từ CQT");}
		{put("COMPLETE", "CQT chấp nhận");}
		{put("ERROR_CQT", "Lỗi từ CQT");}
	
	};
	
	public static final LinkedHashMap<String, String> MAP_UPDATE_STATUS = new LinkedHashMap<String, String>(){
		private static final long serialVersionUID = -4829089663504096703L;
		{put("true", "Đã cập nhật");}
		{put("false", "Chưa cập nhật");}	
	};
	
	public static final class NEXT_SEQUENCE{
		public static final String EINVOICEMTT = "EINVOICEMTT";
	}
}
