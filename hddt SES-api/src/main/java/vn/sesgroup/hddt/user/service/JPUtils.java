package vn.sesgroup.hddt.user.service;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.text.DecimalFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoField;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import org.apache.commons.lang3.StringUtils;
import org.bson.types.ObjectId;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import net.sf.jasperreports.engine.JRDataSource;
import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;
import net.sf.jasperreports.engine.export.JRPdfExporter;
import net.sf.jasperreports.export.Exporter;
import net.sf.jasperreports.export.SimpleExporterInput;
import net.sf.jasperreports.export.SimpleOutputStreamExporterOutput;
import net.sf.jasperreports.export.SimplePdfExporterConfiguration;
import vn.sesgroup.hddt.dto.SignTypeInfo;
import vn.sesgroup.hddt.utility.Commons;
import vn.sesgroup.hddt.utility.Constants;
import vn.sesgroup.hddt.utility.SystemParams;

@Service
public class JPUtils {
	private static final String IMAGE_FILENAME_BLANK = "BLANK_ICON.png";
//	private static final String IMAGE_FILENAME_INV_CONVERT = "BLANK_ICON.png";
	private Commons commons = new Commons();
	
	public ByteArrayOutputStream createFinalInvoice(File fileJP, Document doc, String secureKey, String CheckView
			, int numberRowInPage, int numberRowInPageMultiPage, int numberCharsInRow
			, String MST,String link, String ParamUSD, String pathLogo, String pathBackground,String pathQA, String pathVien
			, boolean isConvert, boolean isDeleted
			, boolean isThayThe, boolean isDieuChinh
		) throws Exception{
			Map<String, Object> reportParams = new HashMap<String, Object>();
			 String Checknamecd = "";
			
			
			/*KIEM TRA FILE LOG & BACKGROUND CO TON TAI KHONG*/
			File f = new File(pathLogo);
			if(f.exists() && f.isFile())
				reportParams.put("URL_IMG_LOGO", pathLogo);
			else
				reportParams.put("URL_IMG_LOGO", Paths.get(SystemParams.DIR_E_INVOICE_TEMPLATE, IMAGE_FILENAME_BLANK).toString());
			
			f = new File(pathBackground);
			if(f.exists() && f.isFile())
				reportParams.put("URL_IMG_BACKGROUND", pathBackground);
			else
				reportParams.put("URL_IMG_BACKGROUND", Paths.get(SystemParams.DIR_E_INVOICE_TEMPLATE, IMAGE_FILENAME_BLANK).toString());
		
			f = new File(pathQA);
			if(f.exists() && f.isFile())
				reportParams.put("URL_QR_INFO", pathQA);
			else
				reportParams.put("URL_QR_INFO", Paths.get(SystemParams.DIR_E_INVOICE_TEMPLATE, IMAGE_FILENAME_BLANK).toString());
		
			f = new File(pathVien);
			if(f.exists() && f.isFile())
				reportParams.put("URL_IMG_FRAME", pathVien);
			else
				reportParams.put("URL_IMG_FRAME", Paths.get(SystemParams.DIR_E_INVOICE_TEMPLATE, IMAGE_FILENAME_BLANK).toString());
		
		
			
			
			reportParams.put("IsConvert", isConvert);
//			if(isConvert) {
//				reportParams.put("IsConvert", isConvert);
//				reportParams.put("URL_IMG_CONVERT", Paths.get(SystemParams.DIR_E_INVOICE_TEMPLATE, IMAGE_FILENAME_INV_CONVERT).toString());
//			}

			String NLap = "";
			String KDLieu = "";
			Node nodeTmp = null;
			Node nodeSubTmp = null;
			NodeList nodeListTTKhac = null;
			int countPrd = 0;
			
			XPath xPath = XPathFactory.newInstance().newXPath();
//			Node nodeHDon = (Node) xPath.evaluate("/KetQuaTraCuu/DuLieu/TDiep[last()]/DLieu/HDon", doc, XPathConstants.NODE);
			Node nodeHDon = null;
			for(int i = 1; i<= 20; i++) {
				nodeHDon = (Node) xPath.evaluate("/KetQuaTraCuu/DuLieu/TDiep[" + i + "]/DLieu/HDon", doc, XPathConstants.NODE);
				if(nodeHDon != null) break;
			}
			
			
			
			/*FILE CHUA DUOC DUI DEN CO QUAN THUE*/
			if(null == nodeHDon) {
				nodeHDon = (Node) xPath.evaluate("/HDon", doc, XPathConstants.NODE);
			}
			
			Node nodeTTChung = (Node) xPath.evaluate("DLHDon/TTChung", nodeHDon, XPathConstants.NODE);
			Node nodeNBan = (Node) xPath.evaluate("DLHDon/NDHDon/NBan", nodeHDon, XPathConstants.NODE);
			Node nodeNMua = (Node) xPath.evaluate("DLHDon/NDHDon/NMua", nodeHDon, XPathConstants.NODE);
			Node nodeTToan = (Node) xPath.evaluate("DLHDon/NDHDon/TToan", nodeHDon, XPathConstants.NODE);
			NodeList nodeListHHDVu = (NodeList) xPath.evaluate("DLHDon/NDHDon/DSHHDVu/HHDVu", nodeHDon, XPathConstants.NODESET);
			
			LocalDate localDateNLap = null;
			NLap = commons.getTextFromNodeXML((Element) xPath.evaluate("NLap", nodeTTChung, XPathConstants.NODE));
			if(!"".equals(NLap)) {
				try {
					localDateNLap = commons.convertStringToLocalDate(NLap, "yyyy-MM-dd");
				}catch(Exception e) {}
			}
			
			if(null != localDateNLap) {
				reportParams.put("InvoiceDate", StringUtils.leftPad(String.valueOf(localDateNLap.get(ChronoField.DAY_OF_MONTH)), 2, "0"));
				reportParams.put("InvoiceMonth", StringUtils.leftPad(String.valueOf(localDateNLap.get(ChronoField.MONTH_OF_YEAR)), 2, "0"));
				reportParams.put("InvoiceYear", StringUtils.leftPad(String.valueOf(localDateNLap.get(ChronoField.YEAR)), 4, "0"));
			}
			
			reportParams.put("KHHDon", 
				commons.getTextFromNodeXML((Element) xPath.evaluate("KHMSHDon", nodeTTChung, XPathConstants.NODE))
				+ commons.getTextFromNodeXML((Element) xPath.evaluate("KHHDon", nodeTTChung, XPathConstants.NODE))
			);
			
		//	LocalDate localDateCD= LocalDate.now();
		//	localDateCD = commons.convertStringToLocalDate(localDateCD.toString(), "yyyy-MM-dd");
			LocalDate localDateCD= null;
			localDateCD = commons.convertStringToLocalDate(NLap, "yyyy-MM-dd");
				reportParams.put("InvoiceCovertDate", StringUtils.leftPad(String.valueOf(localDateCD.get(ChronoField.DAY_OF_MONTH)), 2, "0"));
				reportParams.put("InvoiceCovertMonth", StringUtils.leftPad(String.valueOf(localDateCD.get(ChronoField.MONTH_OF_YEAR)), 2, "0"));
				reportParams.put("InvoiceCovertYear", StringUtils.leftPad(String.valueOf(localDateCD.get(ChronoField.YEAR)), 4, "0"));
				try {
					String[] words = CheckView.split(",");
					  CheckView = words[0];
					  Checknamecd =  words[1];
				
				} catch (Exception e) {
					// TODO: handle exception
				}
		
				if(!Checknamecd.equals("")) {
					reportParams.put("SignNameCovert",Checknamecd);			
				}
if(CheckView.equals("Y"))
{
			reportParams.put("SHDon", commons.getTextFromNodeXML((Element) xPath.evaluate("SHDon", nodeTTChung, XPathConstants.NODE)));
			reportParams.put("SHDonOrigin", commons.getTextFromNodeXML((Element) xPath.evaluate("SHDon", nodeTTChung, XPathConstants.NODE)));
			
}
else {
	reportParams.put("SHDon",
			commons.addSpacingAfterLeter(
				commons.formatNumberBillInvoice(commons.getTextFromNodeXML((Element) xPath.evaluate("SHDon", nodeTTChung, XPathConstants.NODE)))
			)
		);
	//	reportParams.put("SHDonOrigin", commons.getTextFromNodeXML((Element) xPath.evaluate("SHDon", nodeTTChung, XPathConstants.NODE)));
		reportParams.put("SHDonOrigin",
				commons.addSpacingAfterLeter(
					commons.formatNumberBillInvoice(commons.getTextFromNodeXML((Element) xPath.evaluate("SHDon", nodeTTChung, XPathConstants.NODE)))
				)
			);
}
			
			
			reportParams.put("MCCQT", 
				commons.getTextFromNodeXML((Element) xPath.evaluate("MCCQT", nodeHDon, XPathConstants.NODE))
			);
			//TTChung/TTKhac
			nodeListTTKhac = (NodeList) xPath.evaluate("TTKhac/TTin", nodeTTChung, XPathConstants.NODESET);
			if(nodeListTTKhac != null) {
				for(int j = 0; j < nodeListTTKhac.getLength(); j++) {
					nodeSubTmp = nodeListTTKhac.item(j);
					
					KDLieu = commons.getTextFromNodeXML((Element) xPath.evaluate("KDLieu", nodeSubTmp, XPathConstants.NODE));
					switch (KDLieu.toUpperCase()) {
					case "DECIMAL":
						reportParams.put(
							commons.getTextFromNodeXML((Element) xPath.evaluate("TTruong", nodeSubTmp, XPathConstants.NODE)),
							commons.ToNumber(commons.getTextFromNodeXML((Element) xPath.evaluate("DLieu", nodeSubTmp, XPathConstants.NODE)))
						);
						break;
					default:
						reportParams.put(
							commons.getTextFromNodeXML((Element) xPath.evaluate("TTruong", nodeSubTmp, XPathConstants.NODE)),
							commons.getTextFromNodeXML((Element) xPath.evaluate("DLieu", nodeSubTmp, XPathConstants.NODE))
						);
						break;
					}
				}
			}
			
			//CHECK SECUREKEY									
			String checkSecureKey = (String) reportParams.get("SecureKey");
			
			if(checkSecureKey == null || checkSecureKey.equals("")) {
				reportParams.put("SecureKey", secureKey);
			}			
			//END CHECK SECUREKEY
			
			reportParams.put("SignName", commons.getTextFromNodeXML((Element) xPath.evaluate("Ten", nodeNBan, XPathConstants.NODE)));
			reportParams.put("NBanTen", 
				commons.getTextFromNodeXML((Element) xPath.evaluate("Ten", nodeNBan, XPathConstants.NODE))
			);
			reportParams.put("NBanMST", 
				commons.getTextFromNodeXML((Element) xPath.evaluate("MST", nodeNBan, XPathConstants.NODE))
			);
			reportParams.put("NBanDChi", 
				commons.getTextFromNodeXML((Element) xPath.evaluate("DChi", nodeNBan, XPathConstants.NODE))
			);
			reportParams.put("NBanSDThoai", 
				commons.getTextFromNodeXML((Element) xPath.evaluate("SDThoai", nodeNBan, XPathConstants.NODE))
			);
			reportParams.put("NBanDCTDTu", 
				commons.getTextFromNodeXML((Element) xPath.evaluate("DCTDTu", nodeNBan, XPathConstants.NODE))
			);
			reportParams.put("NBanSTKNHang", 
				commons.getTextFromNodeXML((Element) xPath.evaluate("STKNHang", nodeNBan, XPathConstants.NODE))
			);
			reportParams.put("NBanTNHang", 
				commons.getTextFromNodeXML((Element) xPath.evaluate("TNHang", nodeNBan, XPathConstants.NODE))
			);
			reportParams.put("NBanFax", 
				commons.getTextFromNodeXML((Element) xPath.evaluate("Fax", nodeNBan, XPathConstants.NODE))
			);
			reportParams.put("NBanWebsite", 
				commons.getTextFromNodeXML((Element) xPath.evaluate("Website", nodeNBan, XPathConstants.NODE))
			);
			
			/*THONG TIN KHAC (NEU CO)*/
			nodeListTTKhac = (NodeList) xPath.evaluate("TTKhac/TTin", nodeNBan, XPathConstants.NODESET);
			if(nodeListTTKhac != null) {
				for(int j = 0; j < nodeListTTKhac.getLength(); j++) {
					nodeSubTmp = nodeListTTKhac.item(j);
					
					KDLieu = commons.getTextFromNodeXML((Element) xPath.evaluate("KDLieu", nodeSubTmp, XPathConstants.NODE));
					switch (KDLieu.toUpperCase()) {
					case "DECIMAL":
						reportParams.put(
							"NBan" + commons.getTextFromNodeXML((Element) xPath.evaluate("TTruong", nodeSubTmp, XPathConstants.NODE)),
							commons.ToNumber(commons.getTextFromNodeXML((Element) xPath.evaluate("DLieu", nodeSubTmp, XPathConstants.NODE)))
						);
						break;
					default:
						reportParams.put(
							"NBan" + commons.getTextFromNodeXML((Element) xPath.evaluate("TTruong", nodeSubTmp, XPathConstants.NODE)),
							commons.getTextFromNodeXML((Element) xPath.evaluate("DLieu", nodeSubTmp, XPathConstants.NODE))
						);
						break;
					}
				}
			}
			
			reportParams.put("NMuaTen", 
				commons.getTextFromNodeXML((Element) xPath.evaluate("Ten", nodeNMua, XPathConstants.NODE))
			);
			reportParams.put("NMuaMST", 
				commons.getTextFromNodeXML((Element) xPath.evaluate("MST", nodeNMua, XPathConstants.NODE))
			);
			reportParams.put("NMuaDChi", 
				commons.getTextFromNodeXML((Element) xPath.evaluate("DChi", nodeNMua, XPathConstants.NODE))
			);
			reportParams.put("NMuaMKHang", 
				commons.getTextFromNodeXML((Element) xPath.evaluate("MKHang", nodeNMua, XPathConstants.NODE))
			);
			reportParams.put("NMuaSDThoai", 
				commons.getTextFromNodeXML((Element) xPath.evaluate("SDThoai", nodeNMua, XPathConstants.NODE))
			);
			reportParams.put("NMuaDCTDTu", 
				commons.getTextFromNodeXML((Element) xPath.evaluate("DCTDTu", nodeNMua, XPathConstants.NODE))
			);
			reportParams.put("NMuaHVTNMHang", 
				commons.getTextFromNodeXML((Element) xPath.evaluate("HVTNMHang", nodeNMua, XPathConstants.NODE))
			);
			reportParams.put("NMuaSTKNHang", 
				commons.getTextFromNodeXML((Element) xPath.evaluate("STKNHang", nodeNMua, XPathConstants.NODE))
			);
			reportParams.put("NMuaTNHang", 
				commons.getTextFromNodeXML((Element) xPath.evaluate("TNHang", nodeNMua, XPathConstants.NODE))
			);
			
			reportParams.put("TToanTGia", commons.ToNumber(commons.getTextFromNodeXML((Element) xPath.evaluate("TGia", nodeTTChung, XPathConstants.NODE))));
			reportParams.put("TTChungHTTToan", commons.getTextFromNodeXML((Element) xPath.evaluate("HTTToan", nodeTTChung, XPathConstants.NODE)));
			
			String TCHDon = commons.getTextFromNodeXML((Element) xPath.evaluate("TTHDLQuan/TCHDon", nodeTTChung, XPathConstants.NODE));
			String noticeTTDC = "";
			switch (TCHDon) {
			case "1":
				if(CheckView.equals("Y"))
				{
					noticeTTDC = String.format("(Thay thế cho hóa đơn Mẫu số %s, Ký hiệu %s, số %s, ngày %s)", 
							commons.getTextFromNodeXML((Element) xPath.evaluate("TTHDLQuan/KHMSHDCLQuan", nodeTTChung, XPathConstants.NODE)),
							commons.getTextFromNodeXML((Element) xPath.evaluate("TTHDLQuan/KHHDCLQuan", nodeTTChung, XPathConstants.NODE)),
							commons.getTextFromNodeXML((Element) xPath.evaluate("TTHDLQuan/SHDCLQuan", nodeTTChung, XPathConstants.NODE)),
							commons.convertLocalDateTimeToString(commons.convertStringToLocalDate(commons.getTextFromNodeXML((Element) xPath.evaluate("TTHDLQuan/NLHDCLQuan", nodeTTChung, XPathConstants.NODE)), "yyyy-MM-dd"), Constants.FORMAT_DATE.FORMAT_DATE_WEB)
						);
				}
				else {
					noticeTTDC = String.format("(Thay thế cho hóa đơn Mẫu số %s, Ký hiệu %s, số %s, ngày %s)", 
							commons.getTextFromNodeXML((Element) xPath.evaluate("TTHDLQuan/KHMSHDCLQuan", nodeTTChung, XPathConstants.NODE)),
							commons.getTextFromNodeXML((Element) xPath.evaluate("TTHDLQuan/KHHDCLQuan", nodeTTChung, XPathConstants.NODE)),
							commons.formatNumberBillInvoice(commons.getTextFromNodeXML((Element) xPath.evaluate("TTHDLQuan/SHDCLQuan", nodeTTChung, XPathConstants.NODE))),
							commons.convertLocalDateTimeToString(commons.convertStringToLocalDate(commons.getTextFromNodeXML((Element) xPath.evaluate("TTHDLQuan/NLHDCLQuan", nodeTTChung, XPathConstants.NODE)), "yyyy-MM-dd"), Constants.FORMAT_DATE.FORMAT_DATE_WEB)
						);
				}
				
				break;
			case "2":
				if(CheckView.equals("Y"))
				{
					noticeTTDC = String.format("(Điều chỉnh cho hóa đơn Mẫu số %s, Ký hiệu %s, số %s, ngày %s)", 
							commons.getTextFromNodeXML((Element) xPath.evaluate("TTHDLQuan/KHMSHDCLQuan", nodeTTChung, XPathConstants.NODE)),
							commons.getTextFromNodeXML((Element) xPath.evaluate("TTHDLQuan/KHHDCLQuan", nodeTTChung, XPathConstants.NODE)),
							commons.getTextFromNodeXML((Element) xPath.evaluate("TTHDLQuan/SHDCLQuan", nodeTTChung, XPathConstants.NODE)),
							commons.convertLocalDateTimeToString(commons.convertStringToLocalDate(commons.getTextFromNodeXML((Element) xPath.evaluate("TTHDLQuan/NLHDCLQuan", nodeTTChung, XPathConstants.NODE)), "yyyy-MM-dd"), Constants.FORMAT_DATE.FORMAT_DATE_WEB)
						);
				}
				else {
					noticeTTDC = String.format("(Điều chỉnh cho hóa đơn Mẫu số %s, Ký hiệu %s, số %s, ngày %s)", 
							commons.getTextFromNodeXML((Element) xPath.evaluate("TTHDLQuan/KHMSHDCLQuan", nodeTTChung, XPathConstants.NODE)),
							commons.getTextFromNodeXML((Element) xPath.evaluate("TTHDLQuan/KHHDCLQuan", nodeTTChung, XPathConstants.NODE)),
							commons.formatNumberBillInvoice(commons.getTextFromNodeXML((Element) xPath.evaluate("TTHDLQuan/SHDCLQuan", nodeTTChung, XPathConstants.NODE))),
							commons.convertLocalDateTimeToString(commons.convertStringToLocalDate(commons.getTextFromNodeXML((Element) xPath.evaluate("TTHDLQuan/NLHDCLQuan", nodeTTChung, XPathConstants.NODE)), "yyyy-MM-dd"), Constants.FORMAT_DATE.FORMAT_DATE_WEB)
						);
				}
				
				break;
			default:
				break;
			}
			reportParams.put("NoticeTTDC", noticeTTDC);
			reportParams.put("TTChungDVTTe", commons.getTextFromNodeXML((Element) xPath.evaluate("DVTTe", nodeTTChung, XPathConstants.NODE)));
		
			/* TEST THUE */
			String DVTTe = commons.getTextFromNodeXML((Element) xPath.evaluate("DVTTe", nodeTTChung, XPathConstants.NODE));
			
			if(!DVTTe.equals("VND") && !ParamUSD.equals("")) {
			String TgTCThue = commons.getTextFromNodeXML((Element) xPath.evaluate("TgTCThue", nodeTToan, XPathConstants.NODE));
			String TgTThue = commons.getTextFromNodeXML((Element) xPath.evaluate("TgTCThue", nodeTToan, XPathConstants.NODE));
			String TgTTTBSo = commons.getTextFromNodeXML((Element) xPath.evaluate("TgTCThue", nodeTToan, XPathConstants.NODE));
		
			double DBTgTCThue = Double.parseDouble(TgTCThue); 
			double DBTgTThue = Double.parseDouble(TgTThue); 
			double DBTgTTTBSo = Double.parseDouble(TgTTTBSo); 
			
			int checkParamUSD = Integer.parseInt(ParamUSD);
			DecimalFormat decimalFormat = null;
			String formattedNumber = "";
			switch(checkParamUSD) {
			case 1:
				decimalFormat = new DecimalFormat("#,##0.0");
				break;
			case 2:
				decimalFormat = new DecimalFormat("#,##0.00");
				break;
			case 3:
				decimalFormat = new DecimalFormat("#,##0.000");
				break;
			case 4:
				decimalFormat = new DecimalFormat("#,##0.0000");
				break;
			case 5:
				decimalFormat = new DecimalFormat("#,##0.00000");
				break;
			case 6:
				decimalFormat = new DecimalFormat("#,##0.000000");
				break;
			case 7:
				decimalFormat = new DecimalFormat("#,##0.0000000");
				break;
			case 8:
				decimalFormat = new DecimalFormat("#,##0.00000000");
				break;
				
			default:
				decimalFormat = new DecimalFormat("#,##0.00");
				break;
			}
			
	        formattedNumber = decimalFormat.format(DBTgTCThue);						
			reportParams.put("TToanTgTCThueNgoaiTe", formattedNumber);
			
			formattedNumber = decimalFormat.format(DBTgTThue);	
			reportParams.put("TgTThueNgoaiTe", formattedNumber);
			
			formattedNumber = decimalFormat.format(DBTgTTTBSo);	
			reportParams.put("TgTTTBSoNgoaiTe", formattedNumber);
			
			}
			reportParams.put("TToanTgTCThue", 
				commons.ToNumber(commons.getTextFromNodeXML((Element) xPath.evaluate("TgTCThue", nodeTToan, XPathConstants.NODE)))
			);
			reportParams.put("TToanTgTThue", 
				commons.ToNumber(commons.getTextFromNodeXML((Element) xPath.evaluate("TgTThue", nodeTToan, XPathConstants.NODE)))
			);
			reportParams.put("TToanTgTTTBSo", 
				commons.ToNumber(commons.getTextFromNodeXML((Element) xPath.evaluate("TgTTTBSo", nodeTToan, XPathConstants.NODE)))
			);
			/*THONG TIN KHAC (NEU CO)*/
			String TTruong = "";
			String tienQuyDoi = "";
			
			nodeListTTKhac = (NodeList) xPath.evaluate("TTKhac/TTin", nodeTToan, XPathConstants.NODESET);
			if(nodeListTTKhac != null) {
				for(int j = 0; j < nodeListTTKhac.getLength(); j++) {
					nodeSubTmp = nodeListTTKhac.item(j);
					TTruong = commons.getTextFromNodeXML((Element) xPath.evaluate("TTruong", nodeSubTmp, XPathConstants.NODE));
					
					if("TgTQDoi".equals(TTruong)) {
						tienQuyDoi =commons.getTextFromNodeXML((Element) xPath.evaluate("DLieu", nodeSubTmp, XPathConstants.NODE));
					}
					
					KDLieu = commons.getTextFromNodeXML((Element) xPath.evaluate("KDLieu", nodeSubTmp, XPathConstants.NODE));
					switch (KDLieu.toUpperCase()) {
					case "DECIMAL":
						reportParams.put(
							"TToan" + commons.getTextFromNodeXML((Element) xPath.evaluate("TTruong", nodeSubTmp, XPathConstants.NODE)),
							commons.ToNumber(commons.getTextFromNodeXML((Element) xPath.evaluate("DLieu", nodeSubTmp, XPathConstants.NODE)))
						);
						break;
					default:
						reportParams.put(
							"TToan" + commons.getTextFromNodeXML((Element) xPath.evaluate("TTruong", nodeSubTmp, XPathConstants.NODE)),
							commons.getTextFromNodeXML((Element) xPath.evaluate("DLieu", nodeSubTmp, XPathConstants.NODE))
						);
						break;
					}
				}
			}
			//TIEN QUY DOI
			if(!tienQuyDoi.equals("")) {
				double QuyDoiTien = Double.parseDouble(tienQuyDoi);
				reportParams.put("TToanTgTTTQDoi", QuyDoiTien);
			}
			reportParams.put("TToanTgTTTBChu", 
				commons.getTextFromNodeXML((Element) xPath.evaluate("TgTTTBChu", nodeTToan, XPathConstants.NODE))
			);
			
			/*LAY DANH SACH LAI SUAT*/
			String tsuatTmp = "";
			String check_ts = "";
			NodeList nodeListLTSuat = null;
			try {
				nodeListLTSuat = (NodeList) xPath.evaluate("THTTLTSuat/LTSuat", nodeTToan, XPathConstants.NODESET);
				if(nodeListLTSuat != null && nodeListLTSuat.getLength() > 0) {
					for(int i = 0; i < nodeListLTSuat.getLength(); i++) {
						nodeTmp = nodeListLTSuat.item(i);
						
						tsuatTmp = commons.getTextFromNodeXML((Element) xPath.evaluate("TSuat", nodeTmp, XPathConstants.NODE)).replaceAll("%", "");
						reportParams.put("Tax_TSuat_" + tsuatTmp, tsuatTmp);
						reportParams.put("Tax_ThTien_" + tsuatTmp, 
							commons.ToNumber(commons.getTextFromNodeXML((Element) xPath.evaluate("ThTien", nodeTmp, XPathConstants.NODE)))
						);
						reportParams.put("Tax_TThue_" + tsuatTmp, 
							commons.ToNumber(commons.getTextFromNodeXML((Element) xPath.evaluate("TThue", nodeTmp, XPathConstants.NODE)))
						);
						
						
						
						
						
						if(reportParams.get("Tax_TSuat_Default") == null) {						
							reportParams.put("Tax_TSuat_Default", tsuatTmp);
							reportParams.put("Tax_ThTien_Default", 
								commons.ToNumber(commons.getTextFromNodeXML((Element) xPath.evaluate("ThTien", nodeTmp, XPathConstants.NODE)))
							);
//							reportParams.put("Tax_TThue_Default", 
//								commons.ToNumber(commons.getTextFromNodeXML((Element) xPath.evaluate("TThue", nodeTmp, XPathConstants.NODE)))
//							);
							check_ts = tsuatTmp;
						}
						
						if(reportParams.get("Tax_TSuat_Default") != null) {	
							if(check_ts.equals("0")) {							
							reportParams.put("Tax_TSuat_Default", tsuatTmp);
							reportParams.put("Tax_ThTien_Default", 
								commons.ToNumber(commons.getTextFromNodeXML((Element) xPath.evaluate("ThTien", nodeTmp, XPathConstants.NODE)))
							);
						}
						}
						
					}
				}
			}catch(Exception e) {}
			/*END - LAY DANH SACH LAI SUAT*/
			
			reportParams.put("Tax_TThue_Default", 
					commons.ToNumber(commons.getTextFromNodeXML((Element) xPath.evaluate("TgTThue", nodeTToan, XPathConstants.NODE)))
				);
			
			
			/*KIEM TRA THONG TIN NGUOI BAN KY CHUA*/
			Node nodeDSCKS = (Node) xPath.evaluate("DSCKS", nodeHDon, XPathConstants.NODE);
			Node nodeSignature = null;
			if(null != nodeDSCKS)
				nodeSignature = (Node) xPath.evaluate("NBan/Signature", nodeDSCKS, XPathConstants.NODE);
			LocalDateTime ldt = null;
			if(null == nodeSignature) {
				reportParams.put("IsSigned", false);
				reportParams.put("SignDesc", "Chưa ký");
				reportParams.put("UrlImageVerify", Paths.get(SystemParams.DIR_E_INVOICE_TEMPLATE, Constants.TEMPLATE_FILE_NAME.IMG_SIGNATURE_INVALID).toString());
			}else {
				String x509Certificate = commons.getTextFromNodeXML((Element) xPath.evaluate("KeyInfo/X509Data/X509Certificate", nodeSignature, XPathConstants.NODE));
				String signingTime = commons.getTextFromNodeXML((Element) xPath.evaluate("Object[@Id='SigningTime']/SignatureProperties/SignatureProperty/SigningTime", nodeSignature, XPathConstants.NODE));
				
				reportParams.put("IsSigned", true);
				reportParams.put("SignDesc", "Đã ký");
				SignTypeInfo signTypeInfo = commons.parserCert(x509Certificate);
				reportParams.put("SignName", null == signTypeInfo? "": signTypeInfo.getName());
				try {
					ldt = commons.convertStringToLocalDateTime(signingTime, "yyyy-MM-dd'T'HH:mm:ss'Z'");
					reportParams.put("SignDate", commons.convertLocalDateTimeToString(ldt, Constants.FORMAT_DATE.FORMAT_DATE_WEB));
				}catch(Exception e) {}
				reportParams.put("UrlImageVerify", Paths.get(SystemParams.DIR_E_INVOICE_TEMPLATE, Constants.TEMPLATE_FILE_NAME.IMG_SIGNATURE_VALID).toString());
			}
			reportParams.put("UrlImageInvDeleted", Paths.get(SystemParams.DIR_E_INVOICE_TEMPLATE, Constants.TEMPLATE_FILE_NAME.IMG_INV_DELETED).toString());
			reportParams.put("IsDeleted", isDeleted);
			reportParams.put("UrlImageInvThayThe", Paths.get(SystemParams.DIR_E_INVOICE_TEMPLATE, Constants.TEMPLATE_FILE_NAME.IMG_INV_THAYTHE).toString());
			reportParams.put("UrlImageInvDieuChinh", Paths.get(SystemParams.DIR_E_INVOICE_TEMPLATE, Constants.TEMPLATE_FILE_NAME.IMG_INV_DIEUCHINH).toString());
			reportParams.put("IsThayThe", isThayThe);
			reportParams.put("IsDieuChinh", isDieuChinh);
			List<HashMap<String, Object>> arrayData = new ArrayList<>();
			HashMap<String, Object> hItem = null;
			
			int lenProName = 0;
			String productName = "";
			String sLo = "";
			String hanSD = "";
			int startRowGroup = 0;
			
			/*XEM CO IN NHIEU DONG KHONG*/
			int groupPageIDX = 1;
			boolean isUsingMultiPage = false;
			if(nodeListHHDVu != null) {
				for(int i = 0; i < nodeListHHDVu.getLength(); i++) {
					nodeTmp = nodeListHHDVu.item(i);
					
					productName = commons.getTextFromNodeXML((Element) xPath.evaluate("THHDVu", nodeTmp, XPathConstants.NODE));
					lenProName = productName.getBytes(StandardCharsets.UTF_8).length;
					startRowGroup += Math.ceil((double) lenProName / numberCharsInRow);
					
					if(startRowGroup >= numberRowInPage) {
						isUsingMultiPage = true;
						break;
					}
				}	
			}
			/*END - XEM CO IN NHIEU DONG KHONG*/
			
			startRowGroup = 0;
			KDLieu = "";
			nodeTmp = null;
			nodeSubTmp = null;
			nodeListTTKhac = null;
			countPrd = 0;
			if(nodeListHHDVu != null) {
				if(isUsingMultiPage) {
					for(int i = 0; i < nodeListHHDVu.getLength(); i++) {
						nodeTmp = nodeListHHDVu.item(i);
						
						productName = commons.getTextFromNodeXML((Element) xPath.evaluate("THHDVu", nodeTmp, XPathConstants.NODE));
						sLo = commons.getTextFromNodeXML((Element) xPath.evaluate("SLo", nodeTmp, XPathConstants.NODE));
						hanSD = commons.getTextFromNodeXML((Element) xPath.evaluate("HanSD", nodeTmp, XPathConstants.NODE));
						lenProName = productName.getBytes(StandardCharsets.UTF_8).length;
						startRowGroup += Math.ceil((double) lenProName / numberCharsInRow);
//						countRowForPrd = (int) Math.ceil(lenProName / numberCharsInRow);
//						if(countRowForPrd > 1)
//							i = i + countRowForPrd;
						
						hItem = new HashMap<>();
						hItem.put("GroupPageIDX", groupPageIDX);
						
						hItem.put("TChat", commons.getTextFromNodeXML((Element) xPath.evaluate("TChat", nodeTmp, XPathConstants.NODE)));
						hItem.put("STT", commons.getTextFromNodeXML((Element) xPath.evaluate("STT", nodeTmp, XPathConstants.NODE)));
						hItem.put("MHHDVu", commons.getTextFromNodeXML((Element) xPath.evaluate("MHHDVu", nodeTmp, XPathConstants.NODE)));
						
						hItem.put("THHDVu", productName);
						
						if(!"".equals(sLo)){
							hItem.put("SLo", commons.getTextFromNodeXML((Element) xPath.evaluate("SLo", nodeTmp, XPathConstants.NODE)));
						}
						if(!"".equals(hanSD)){
							hItem.put("HanSD", commons.getTextFromNodeXML((Element) xPath.evaluate("HanSD", nodeTmp, XPathConstants.NODE)));
						}
						
						hItem.put("DVTinh", commons.getTextFromNodeXML((Element) xPath.evaluate("DVTinh", nodeTmp, XPathConstants.NODE)));
						hItem.put("SLuong",
							commons.ToNumber(commons.getTextFromNodeXML((Element) xPath.evaluate("SLuong", nodeTmp, XPathConstants.NODE)))
						);
						hItem.put("DGia",
							commons.ToNumber(commons.getTextFromNodeXML((Element) xPath.evaluate("DGia", nodeTmp, XPathConstants.NODE)))
						);
						hItem.put("ThTien",
							commons.ToNumber(commons.getTextFromNodeXML((Element) xPath.evaluate("ThTien", nodeTmp, XPathConstants.NODE)))
						);
						hItem.put("TSuat",
							commons.getTextFromNodeXML((Element) xPath.evaluate("TSuat", nodeTmp, XPathConstants.NODE))
						);
						
						/*LAY NOI DUNG TTKHAC*/
						nodeListTTKhac = (NodeList) xPath.evaluate("TTKhac/TTin", nodeTmp, XPathConstants.NODESET);
						if(nodeListTTKhac != null) {
							for(int j = 0; j < nodeListTTKhac.getLength(); j++) {
								nodeSubTmp = nodeListTTKhac.item(j);
								
								KDLieu = commons.getTextFromNodeXML((Element) xPath.evaluate("KDLieu", nodeSubTmp, XPathConstants.NODE));
								switch (KDLieu.toUpperCase()) {
								case "DECIMAL":
									hItem.put(
										commons.getTextFromNodeXML((Element) xPath.evaluate("TTruong", nodeSubTmp, XPathConstants.NODE)),
										commons.ToNumber(commons.getTextFromNodeXML((Element) xPath.evaluate("DLieu", nodeSubTmp, XPathConstants.NODE)))
									);
									break;
								default:
									hItem.put(
										commons.getTextFromNodeXML((Element) xPath.evaluate("TTruong", nodeSubTmp, XPathConstants.NODE)),
										commons.getTextFromNodeXML((Element) xPath.evaluate("DLieu", nodeSubTmp, XPathConstants.NODE))
									);
									break;
								}
							}
						}
						
						arrayData.add(hItem);
						countPrd++;	
						
						if(startRowGroup >= numberRowInPageMultiPage) {
							startRowGroup = 0;
							groupPageIDX++;
						}
					}
					if(0 == startRowGroup && nodeListHHDVu.getLength() > 0) groupPageIDX--;		//BO BOT TRANG CUOI CUNG VI KHONG CO DU LIEU
					
					if(0 == startRowGroup) {
						groupPageIDX++;
						for(int i = 0; i < numberRowInPage; i++) {
							hItem = new HashMap<>();
							hItem.put("ProdName", " ");
							hItem.put("GroupPageIDX", groupPageIDX);
							arrayData.add(hItem);
						}
					}else if(startRowGroup <= numberRowInPage) {
						for(int i = startRowGroup; i < numberRowInPage; i++) {
							hItem = new HashMap<>();
							hItem.put("ProdName", " ");
							hItem.put("GroupPageIDX", groupPageIDX);
							arrayData.add(hItem);
						}
					}else {
						for(int i = startRowGroup; i < numberRowInPageMultiPage; i++) {
							hItem = new HashMap<>();
							hItem.put("ProdName", " ");
							hItem.put("GroupPageIDX", groupPageIDX);
							arrayData.add(hItem);
						}
						groupPageIDX++;
						for(int i = 0; i < numberRowInPage; i++) {
							hItem = new HashMap<>();
							hItem.put("ProdName", " ");
							hItem.put("GroupPageIDX", groupPageIDX);
							arrayData.add(hItem);
						}
					}
				}else {
					for(int i = 0; i < nodeListHHDVu.getLength(); i++) {
						nodeTmp = nodeListHHDVu.item(i);
						
						productName = commons.getTextFromNodeXML((Element) xPath.evaluate("THHDVu", nodeTmp, XPathConstants.NODE));
						sLo = commons.getTextFromNodeXML((Element) xPath.evaluate("SLo", nodeTmp, XPathConstants.NODE));
						hanSD = commons.getTextFromNodeXML((Element) xPath.evaluate("HanSD", nodeTmp, XPathConstants.NODE));
						lenProName = productName.getBytes(StandardCharsets.UTF_8).length;
						startRowGroup += Math.ceil((double) lenProName / numberCharsInRow);
//						countRowForPrd = (int) Math.ceil(lenProName / numberCharsInRow);
//						if(countRowForPrd > 1)
//							i = i + countRowForPrd;
						
						hItem = new HashMap<>();
						hItem.put("GroupPageIDX", groupPageIDX);
						
						hItem.put("TChat", commons.getTextFromNodeXML((Element) xPath.evaluate("TChat", nodeTmp, XPathConstants.NODE)));
						hItem.put("STT", commons.getTextFromNodeXML((Element) xPath.evaluate("STT", nodeTmp, XPathConstants.NODE)));
						hItem.put("MHHDVu", commons.getTextFromNodeXML((Element) xPath.evaluate("MHHDVu", nodeTmp, XPathConstants.NODE)));
						
						hItem.put("THHDVu", productName);
						if(!"".equals(sLo)){
							hItem.put("SLo", commons.getTextFromNodeXML((Element) xPath.evaluate("SLo", nodeTmp, XPathConstants.NODE)));
						}
						if(!"".equals(hanSD)){
							hItem.put("HanSD", commons.getTextFromNodeXML((Element) xPath.evaluate("HanSD", nodeTmp, XPathConstants.NODE)));
						}
						hItem.put("DVTinh", commons.getTextFromNodeXML((Element) xPath.evaluate("DVTinh", nodeTmp, XPathConstants.NODE)));
						hItem.put("SLuong",
							commons.ToNumber(commons.getTextFromNodeXML((Element) xPath.evaluate("SLuong", nodeTmp, XPathConstants.NODE)))
						);
						hItem.put("DGia",
							commons.ToNumber(commons.getTextFromNodeXML((Element) xPath.evaluate("DGia", nodeTmp, XPathConstants.NODE)))
						);
						hItem.put("ThTien",
							commons.ToNumber(commons.getTextFromNodeXML((Element) xPath.evaluate("ThTien", nodeTmp, XPathConstants.NODE)))
						);
						hItem.put("TSuat",
							commons.getTextFromNodeXML((Element) xPath.evaluate("TSuat", nodeTmp, XPathConstants.NODE))
						);
						
						/*LAY NOI DUNG TTKHAC*/
						nodeListTTKhac = (NodeList) xPath.evaluate("TTKhac/TTin", nodeTmp, XPathConstants.NODESET);
						if(nodeListTTKhac != null) {
							for(int j = 0; j < nodeListTTKhac.getLength(); j++) {
								nodeSubTmp = nodeListTTKhac.item(j);
								
								KDLieu = commons.getTextFromNodeXML((Element) xPath.evaluate("KDLieu", nodeSubTmp, XPathConstants.NODE));
								switch (KDLieu.toUpperCase()) {
								case "DECIMAL":
									hItem.put(
										commons.getTextFromNodeXML((Element) xPath.evaluate("TTruong", nodeSubTmp, XPathConstants.NODE)),
										commons.ToNumber(commons.getTextFromNodeXML((Element) xPath.evaluate("DLieu", nodeSubTmp, XPathConstants.NODE)))
									);
									break;
								default:
									hItem.put(
										commons.getTextFromNodeXML((Element) xPath.evaluate("TTruong", nodeSubTmp, XPathConstants.NODE)),
										commons.getTextFromNodeXML((Element) xPath.evaluate("DLieu", nodeSubTmp, XPathConstants.NODE))
									);
									break;
								}
							}
						}
						
						arrayData.add(hItem);
						countPrd++;	
						
						if(startRowGroup >= numberRowInPage) {
							startRowGroup = 0;
							groupPageIDX++;
						}
					}
					if(0 == startRowGroup && nodeListHHDVu.getLength() > 0) groupPageIDX--;		//BO BOT TRANG CUOI CUNG VI KHONG CO DU LIEU
					
					/*ADD THEM DU LIEU CHO DU TRANG*/
					if(startRowGroup > 0 || nodeListHHDVu.getLength() == 0) {
						for(int i = startRowGroup; i < numberRowInPage; i++) {
							hItem = new HashMap<>();
							hItem.put("ProdName", " ");
							hItem.put("GroupPageIDX", groupPageIDX);
							arrayData.add(hItem);
						}
					}
				}
				
			}		
			reportParams.put("PortalLink", 
					link
			);
			
			reportParams.put("TOTAL_PAGE", (int) groupPageIDX);
			
			JRDataSource jds = null;
			jds = new JRBeanCollectionDataSource(arrayData);
			
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			JasperReport jr = JasperCompileManager.compileReport(new FileInputStream(fileJP));
			JasperPrint jp = JasperFillManager.fillReport(jr, reportParams, jds);
			Exporter exporter = null;
			
			exporter = new JRPdfExporter();
			exporter.setExporterInput(new SimpleExporterInput(jp));
			exporter.setExporterOutput(new SimpleOutputStreamExporterOutput(out));
	        SimplePdfExporterConfiguration configuration = new SimplePdfExporterConfiguration();
	        configuration.setCreatingBatchModeBookmarks(true);
	        exporter.setConfiguration(configuration);
	        exporter.exportReport();
	        return out;
		}



	public ByteArrayOutputStream createFinalInvoice2(File fileJP, Document doc
			,org.bson.Document docTmp,String CheckView, int numberRowInPage, int numberRowInPageMultiPage, int numberCharsInRow
			, String MST, String link, String pathLogo, String pathBackground,String pathQA, String pathVien
			, boolean isConvert, boolean isDeleted
			, boolean isThayThe, boolean isDieuChinh
		) throws Exception{
			Map<String, Object> reportParams = new HashMap<String, Object>();
			
			/*KIEM TRA FILE LOG & BACKGROUND CO TON TAI KHONG*/
			File f = new File(pathLogo);
			if(f.exists() && f.isFile())
				reportParams.put("URL_IMG_LOGO", pathLogo);
			else
				reportParams.put("URL_IMG_LOGO", Paths.get(SystemParams.DIR_E_INVOICE_TEMPLATE, IMAGE_FILENAME_BLANK).toString());
			
			f = new File(pathBackground);
			if(f.exists() && f.isFile())
				reportParams.put("URL_IMG_BACKGROUND", pathBackground);
			else
				reportParams.put("URL_IMG_BACKGROUND", Paths.get(SystemParams.DIR_E_INVOICE_TEMPLATE, IMAGE_FILENAME_BLANK).toString());
			f = new File(pathQA);
			if(f.exists() && f.isFile())
				reportParams.put("URL_QR_INFO", pathQA);
			else
				reportParams.put("URL_QR_INFO", Paths.get(SystemParams.DIR_E_INVOICE_TEMPLATE, IMAGE_FILENAME_BLANK).toString());
		
			f = new File(pathVien);
			if(f.exists() && f.isFile())
				reportParams.put("URL_IMG_FRAME", pathVien);
			else
				reportParams.put("URL_IMG_FRAME", Paths.get(SystemParams.DIR_E_INVOICE_TEMPLATE, IMAGE_FILENAME_BLANK).toString());
		
			
			
			
			reportParams.put("IsConvert", isConvert);

			String NLap = "";
			String KDLieu = "";
			
			Node nodeTmp = null;
			Node nodeSubTmp = null;
			NodeList nodeListTTKhac = null;
			NodeList nodeListNBanTTKhac = null;
			int countPrd = 0;
			
			XPath xPath = XPathFactory.newInstance().newXPath();
//			Node nodeHDon = (Node) xPath.evaluate("/KetQuaTraCuu/DuLieu/TDiep[last()]/DLieu/HDon", doc, XPathConstants.NODE);
			Node nodeHDon = null;
			for(int i = 1; i<= 20; i++) {
				nodeHDon = (Node) xPath.evaluate("/KetQuaTraCuu/DuLieu/TDiep[" + i + "]/DLieu/HDon", doc, XPathConstants.NODE);
				if(nodeHDon != null) break;
			}
			/*FILE CHUA DUOC DUI DEN CO QUAN THUE*/
			if(null == nodeHDon) {
				nodeHDon = (Node) xPath.evaluate("/HDon", doc, XPathConstants.NODE);
			}
			
			Node nodeTTChung = (Node) xPath.evaluate("DLHDon/TTChung", nodeHDon, XPathConstants.NODE);
			Node nodeNBan = (Node) xPath.evaluate("DLHDon/NDHDon/NBan", nodeHDon, XPathConstants.NODE);
			Node nodeNMua = (Node) xPath.evaluate("DLHDon/NDHDon/NMua", nodeHDon, XPathConstants.NODE);
			Node nodeTToan = (Node) xPath.evaluate("DLHDon/NDHDon/TToan", nodeHDon, XPathConstants.NODE);
			NodeList nodeListHHDVu = (NodeList) xPath.evaluate("DLHDon/NDHDon/DSHHDVu/HHDVu", nodeHDon, XPathConstants.NODESET);
			
			LocalDate localDateNLap = null;
			NLap = commons.getTextFromNodeXML((Element) xPath.evaluate("NLap", nodeTTChung, XPathConstants.NODE));
			if(!"".equals(NLap)) {
				try {
					localDateNLap = commons.convertStringToLocalDate(NLap, "yyyy-MM-dd");
				}catch(Exception e) {}
			}
			
			if(null != localDateNLap) {
				reportParams.put("InvoiceDate", StringUtils.leftPad(String.valueOf(localDateNLap.get(ChronoField.DAY_OF_MONTH)), 2, "0"));
				reportParams.put("InvoiceMonth", StringUtils.leftPad(String.valueOf(localDateNLap.get(ChronoField.MONTH_OF_YEAR)), 2, "0"));
				reportParams.put("InvoiceYear", StringUtils.leftPad(String.valueOf(localDateNLap.get(ChronoField.YEAR)), 4, "0"));
			}
			if(null != localDateNLap) {
				reportParams.put("LDieuDongDate", StringUtils.leftPad(String.valueOf(localDateNLap.get(ChronoField.DAY_OF_MONTH)), 2, "0"));
				reportParams.put("LDieuDongMonth", StringUtils.leftPad(String.valueOf(localDateNLap.get(ChronoField.MONTH_OF_YEAR)), 2, "0"));
				reportParams.put("LDieuDongYear", StringUtils.leftPad(String.valueOf(localDateNLap.get(ChronoField.YEAR)), 4, "0"));
			}
			
			LocalDate localDateCD= null;
			localDateCD = commons.convertStringToLocalDate(NLap, "yyyy-MM-dd");
				reportParams.put("InvoiceCovertDate", StringUtils.leftPad(String.valueOf(localDateCD.get(ChronoField.DAY_OF_MONTH)), 2, "0"));
				reportParams.put("InvoiceCovertMonth", StringUtils.leftPad(String.valueOf(localDateCD.get(ChronoField.MONTH_OF_YEAR)), 2, "0"));
				reportParams.put("InvoiceCovertYear", StringUtils.leftPad(String.valueOf(localDateCD.get(ChronoField.YEAR)), 4, "0"));
			
			reportParams.put("KHHDon", 
				commons.getTextFromNodeXML((Element) xPath.evaluate("KHMSHDon", nodeTTChung, XPathConstants.NODE))
				+ commons.getTextFromNodeXML((Element) xPath.evaluate("KHHDon", nodeTTChung, XPathConstants.NODE))
			);
			
			 String Checknamecd = "";
			
			try {
				String[] words = CheckView.split(",");
				  CheckView = words[0];
				  Checknamecd =  words[1];
			
			} catch (Exception e) {
				// TODO: handle exception
			}
	
			if(!Checknamecd.equals("")) {
				reportParams.put("SignNameCovert",Checknamecd);			
			}
			
//			reportParams.put("SHDon",
//				commons.addSpacingAfterLeter(
//					commons.formatNumberBillInvoice(commons.getTextFromNodeXML((Element) xPath.evaluate("SHDon", nodeTTChung, XPathConstants.NODE)))
//				)
//			);
//			reportParams.put("SHDonOrigin", commons.getTextFromNodeXML((Element) xPath.evaluate("SHDon", nodeTTChung, XPathConstants.NODE)));
//			
			
			if(CheckView.equals("Y"))
			{
						reportParams.put("SHDon", commons.getTextFromNodeXML((Element) xPath.evaluate("SHDon", nodeTTChung, XPathConstants.NODE)));
						reportParams.put("SHDonOrigin", commons.getTextFromNodeXML((Element) xPath.evaluate("SHDon", nodeTTChung, XPathConstants.NODE)));
						
			}
			else {
				reportParams.put("SHDon",
						commons.addSpacingAfterLeter(
							commons.formatNumberBillInvoice(commons.getTextFromNodeXML((Element) xPath.evaluate("SHDon", nodeTTChung, XPathConstants.NODE)))
						)
					);
				reportParams.put("SHDonOrigin",
							commons.addSpacingAfterLeter(
								commons.formatNumberBillInvoice(commons.getTextFromNodeXML((Element) xPath.evaluate("SHDon", nodeTTChung, XPathConstants.NODE)))
							)
						);
			}	
			
			
			
			
			
			
			
			
			
			
			
			
			reportParams.put("MCCQT", 
				commons.getTextFromNodeXML((Element) xPath.evaluate("MCCQT", nodeHDon, XPathConstants.NODE))
			);
			//TTChung/TTKhac
			nodeListTTKhac = (NodeList) xPath.evaluate("TTKhac/TTin", nodeTTChung, XPathConstants.NODESET);
			if(nodeListTTKhac != null) {
				for(int j = 0; j < nodeListTTKhac.getLength(); j++) {
					nodeSubTmp = nodeListTTKhac.item(j);
					
					KDLieu = commons.getTextFromNodeXML((Element) xPath.evaluate("KDLieu", nodeSubTmp, XPathConstants.NODE));
					switch (KDLieu.toUpperCase()) {
					case "DECIMAL":
						reportParams.put(
							commons.getTextFromNodeXML((Element) xPath.evaluate("TTruong", nodeSubTmp, XPathConstants.NODE)),
							commons.ToNumber(commons.getTextFromNodeXML((Element) xPath.evaluate("DLieu", nodeSubTmp, XPathConstants.NODE)))
						);
						break;
					default:
						reportParams.put(
							commons.getTextFromNodeXML((Element) xPath.evaluate("TTruong", nodeSubTmp, XPathConstants.NODE)),
							commons.getTextFromNodeXML((Element) xPath.evaluate("DLieu", nodeSubTmp, XPathConstants.NODE))
						);
						break;
					}
				}
			}
			
			//CHECK SECUREKEY									
			String checkSecureKey = (String) reportParams.get("SecureKey");
			
			if(checkSecureKey == null || checkSecureKey.equals("")) {
				reportParams.put("SecureKey", docTmp.get("SecureKey", ""));
			}			
			//END CHECK SECUREKEY
			NodeList nodeListNBan = null;	
			String DLieu = "";
			String TTruong = "";
			String fax = "";
			String dc = "";
			String ten = "";
			String stk = "";
			String tnh= "";
			String phone = "";
			String PostDescription = "";
			nodeListNBan = (NodeList) xPath.evaluate("DLHDon/NDHDon/NBan/TTKhac/TTin", nodeHDon, XPathConstants.NODESET);
			if(nodeListNBan != null) {
				for(int j = 0; j < nodeListNBan.getLength(); j++) {
					nodeSubTmp = nodeListNBan.item(j);
					TTruong = commons.getTextFromNodeXML((Element) xPath.evaluate("TTruong", nodeSubTmp, XPathConstants.NODE));
					
					if("ComFax".equals(TTruong)) {
						fax =commons.getTextFromNodeXML((Element) xPath.evaluate("DLieu", nodeSubTmp, XPathConstants.NODE));
					}
					if("ComAddress".equals(TTruong)) {
						dc =commons.getTextFromNodeXML((Element) xPath.evaluate("DLieu", nodeSubTmp, XPathConstants.NODE));
					}
					if("ComPhone".equals(TTruong)) {
						phone =commons.getTextFromNodeXML((Element) xPath.evaluate("DLieu", nodeSubTmp, XPathConstants.NODE));
					}
					if("STKNHang1".equals(TTruong)) {
						stk =commons.getTextFromNodeXML((Element) xPath.evaluate("DLieu", nodeSubTmp, XPathConstants.NODE));
					}
					if("TNHang1".equals(TTruong)) {
						tnh =commons.getTextFromNodeXML((Element) xPath.evaluate("DLieu", nodeSubTmp, XPathConstants.NODE));
					}
					if("PostBy".equals(TTruong)) {
						ten =commons.getTextFromNodeXML((Element) xPath.evaluate("DLieu", nodeSubTmp, XPathConstants.NODE));
					}
					if("PostDescription".equals(TTruong)) {
						PostDescription =commons.getTextFromNodeXML((Element) xPath.evaluate("DLieu", nodeSubTmp, XPathConstants.NODE));
					}
					DLieu = commons.getTextFromNodeXML((Element) xPath.evaluate("DLieu", nodeSubTmp, XPathConstants.NODE));
					switch (KDLieu.toUpperCase()) {
					case "DECIMAL":
						reportParams.put(
							commons.getTextFromNodeXML((Element) xPath.evaluate("TTruong", nodeSubTmp, XPathConstants.NODE)),
							commons.ToNumber(commons.getTextFromNodeXML((Element) xPath.evaluate("DLieu", nodeSubTmp, XPathConstants.NODE)))
						);
						break;
					default:
						reportParams.put(
							commons.getTextFromNodeXML((Element) xPath.evaluate("TTruong", nodeSubTmp, XPathConstants.NODE)),
							commons.getTextFromNodeXML((Element) xPath.evaluate("DLieu", nodeSubTmp, XPathConstants.NODE))
						
						);
						break;
					}
				}
			}
			String abc = DLieu;

			reportParams.put("SignName", commons.getTextFromNodeXML((Element) xPath.evaluate("Ten", nodeNBan, XPathConstants.NODE)));
			reportParams.put("NRaLenhTen",ten);

					reportParams.put("LDieuDongSo", 
					commons.getTextFromNodeXML((Element) xPath.evaluate("LDDNBo", nodeNBan, XPathConstants.NODE))
				);
			
					
					
			reportParams.put("NBanMST", 
				commons.getTextFromNodeXML((Element) xPath.evaluate("MST", nodeNBan, XPathConstants.NODE))
			);
			reportParams.put("NBanTen", 
					commons.getTextFromNodeXML((Element) xPath.evaluate("Ten", nodeNBan, XPathConstants.NODE))
				);
			
			reportParams.put("HopDongSo", 
					commons.getTextFromNodeXML((Element) xPath.evaluate("HDSo", nodeNBan, XPathConstants.NODE))
				);
					
			reportParams.put("NBanDChi",commons.getTextFromNodeXML((Element) xPath.evaluate("DChi", nodeNBan, XPathConstants.NODE))
					);
			reportParams.put("NVanChuyenTen", 
					commons.getTextFromNodeXML((Element) xPath.evaluate("TNVChuyen", nodeNBan, XPathConstants.NODE))
				);
			reportParams.put("PTienVanChuyen", 
					commons.getTextFromNodeXML((Element) xPath.evaluate("PTVChuyen", nodeNBan, XPathConstants.NODE))
				);
					
			reportParams.put("NBanSDThoai",phone);
					
			reportParams.put("NBanFax",fax);
			reportParams.put("NBanDCTDTu", 
					docTmp.getEmbedded(Arrays.asList("Issuer","Email"), "")
			);
			
	
			
			reportParams.put("NBanSTKNHang", 
					docTmp.getEmbedded(Arrays.asList("Issuer","BankAccount","AccountNumber"), "")
			);
			reportParams.put("NBanTNHang", 
					docTmp.getEmbedded(Arrays.asList("Issuer","BankAccount","BankName"), "")
			);
			
			
			
			
			
			
			
			
//			reportParams.put("NBanSTKNHang", 
//				commons.getTextFromNodeXML((Element) xPath.evaluate("STKNHang", nodeNBan, XPathConstants.NODE))
//			);
//			reportParams.put("NBanTNHang", 
//				commons.getTextFromNodeXML((Element) xPath.evaluate("TNHang", nodeNBan, XPathConstants.NODE))
//			);
			
			reportParams.put("NBanWebsite", 
				commons.getTextFromNodeXML((Element) xPath.evaluate("Website", nodeNBan, XPathConstants.NODE))
			);
			
			/*THONG TIN KHAC (NEU CO)*/
			nodeListTTKhac = (NodeList) xPath.evaluate("TTKhac/TTin", nodeNBan, XPathConstants.NODESET);
			if(nodeListTTKhac != null) {
				for(int j = 0; j < nodeListTTKhac.getLength(); j++) {
					nodeSubTmp = nodeListTTKhac.item(j);
					
					KDLieu = commons.getTextFromNodeXML((Element) xPath.evaluate("KDLieu", nodeSubTmp, XPathConstants.NODE));
					switch (KDLieu.toUpperCase()) {
					case "DECIMAL":
						reportParams.put(
							"NBan" + commons.getTextFromNodeXML((Element) xPath.evaluate("TTruong", nodeSubTmp, XPathConstants.NODE)),
							commons.ToNumber(commons.getTextFromNodeXML((Element) xPath.evaluate("DLieu", nodeSubTmp, XPathConstants.NODE)))
						);
						break;
					default:
						reportParams.put(
							"NBan" + commons.getTextFromNodeXML((Element) xPath.evaluate("TTruong", nodeSubTmp, XPathConstants.NODE)),
							commons.getTextFromNodeXML((Element) xPath.evaluate("DLieu", nodeSubTmp, XPathConstants.NODE))
						);
						break;
					}
				}
			}
			
			
			reportParams.put("DViNHangMST", 
					commons.getTextFromNodeXML((Element) xPath.evaluate("MST", nodeNMua, XPathConstants.NODE))
				);
			
			reportParams.put("DViNHangTen", 
					commons.getTextFromNodeXML((Element) xPath.evaluate("Ten", nodeNMua, XPathConstants.NODE))
				);
			reportParams.put("DViNHangDChi", 
					commons.getTextFromNodeXML((Element) xPath.evaluate("DChi", nodeNMua, XPathConstants.NODE))
				);
			
			reportParams.put("NNHangTen", 
					commons.getTextFromNodeXML((Element) xPath.evaluate("HVTNMHang", nodeNMua, XPathConstants.NODE))
				);
			
			
			
			reportParams.put("NMuaTen", 
				commons.getTextFromNodeXML((Element) xPath.evaluate("Ten", nodeNMua, XPathConstants.NODE))
			);
			
			reportParams.put("NhapTaiKho", 
					commons.getTextFromNodeXML((Element) xPath.evaluate("DChi", nodeNMua, XPathConstants.NODE))
				);
			reportParams.put("XuatTaiKho",dc);
			reportParams.put("NMuaMST", 
				commons.getTextFromNodeXML((Element) xPath.evaluate("MST", nodeNMua, XPathConstants.NODE))
			);
			
			reportParams.put("NMuaDChi", 
				commons.getTextFromNodeXML((Element) xPath.evaluate("DChi", nodeNMua, XPathConstants.NODE))
			);
			reportParams.put("NDungCV",abc);
			reportParams.put("NMuaMKHang", 
				commons.getTextFromNodeXML((Element) xPath.evaluate("MKHang", nodeNMua, XPathConstants.NODE))
			);
			reportParams.put("NMuaSDThoai", 
				commons.getTextFromNodeXML((Element) xPath.evaluate("SDThoai", nodeNMua, XPathConstants.NODE))
			);
			reportParams.put("NMuaDCTDTu", 
				commons.getTextFromNodeXML((Element) xPath.evaluate("DCTDTu", nodeNMua, XPathConstants.NODE))
			);
			reportParams.put("NMuaHVTNMHang", 
				commons.getTextFromNodeXML((Element) xPath.evaluate("HVTNMHang", nodeNMua, XPathConstants.NODE))
			);
			reportParams.put("NMuaSTKNHang", 
				commons.getTextFromNodeXML((Element) xPath.evaluate("STKNHang", nodeNMua, XPathConstants.NODE))
			);
			reportParams.put("NMuaTNHang", 
				commons.getTextFromNodeXML((Element) xPath.evaluate("TNHang", nodeNMua, XPathConstants.NODE))
			);
			
			reportParams.put("TTChungHTTToan", commons.getTextFromNodeXML((Element) xPath.evaluate("HTTToan", nodeTTChung, XPathConstants.NODE)));
			
			String TCHDon = commons.getTextFromNodeXML((Element) xPath.evaluate("TTHDLQuan/TCHDon", nodeTTChung, XPathConstants.NODE));
			String noticeTTDC = "";
			switch (TCHDon) {
			case "1":
				noticeTTDC = String.format("(Thay thế cho hóa đơn Mẫu số %s, Ký hiệu %s, số %s, ngày %s)", 
						commons.getTextFromNodeXML((Element) xPath.evaluate("TTHDLQuan/KHMSHDCLQuan", nodeTTChung, XPathConstants.NODE)),
						commons.getTextFromNodeXML((Element) xPath.evaluate("TTHDLQuan/KHHDCLQuan", nodeTTChung, XPathConstants.NODE)),
						commons.getTextFromNodeXML((Element) xPath.evaluate("TTHDLQuan/SHDCLQuan", nodeTTChung, XPathConstants.NODE)),
						commons.convertLocalDateTimeToString(commons.convertStringToLocalDate(commons.getTextFromNodeXML((Element) xPath.evaluate("TTHDLQuan/NLHDCLQuan", nodeTTChung, XPathConstants.NODE)), "yyyy-MM-dd"), Constants.FORMAT_DATE.FORMAT_DATE_WEB)
					);
				break;
			case "2":
				noticeTTDC = String.format("(Điều chỉnh cho hóa đơn Mẫu số %s, Ký hiệu %s, số %s, ngày %s)", 
						commons.getTextFromNodeXML((Element) xPath.evaluate("TTHDLQuan/KHMSHDCLQuan", nodeTTChung, XPathConstants.NODE)),
						commons.getTextFromNodeXML((Element) xPath.evaluate("TTHDLQuan/KHHDCLQuan", nodeTTChung, XPathConstants.NODE)),
						commons.getTextFromNodeXML((Element) xPath.evaluate("TTHDLQuan/SHDCLQuan", nodeTTChung, XPathConstants.NODE)),
						commons.convertLocalDateTimeToString(commons.convertStringToLocalDate(commons.getTextFromNodeXML((Element) xPath.evaluate("TTHDLQuan/NLHDCLQuan", nodeTTChung, XPathConstants.NODE)), "yyyy-MM-dd"), Constants.FORMAT_DATE.FORMAT_DATE_WEB)
					);
				break;
			default:
				break;
			}
			reportParams.put("NoticeTTDC", noticeTTDC);
			
			reportParams.put("TTChungDVTTe", commons.getTextFromNodeXML((Element) xPath.evaluate("DVTTe", nodeTTChung, XPathConstants.NODE)));
			reportParams.put("TToanTgTCThue", 
				commons.ToNumber(commons.getTextFromNodeXML((Element) xPath.evaluate("TgTCThue", nodeTToan, XPathConstants.NODE)))
			);
			reportParams.put("TToanTgTThue", 
				commons.ToNumber(commons.getTextFromNodeXML((Element) xPath.evaluate("TgTThue", nodeTToan, XPathConstants.NODE)))
			);
			reportParams.put("TToanTgTTTBSo", 
				commons.ToNumber(commons.getTextFromNodeXML((Element) xPath.evaluate("TgTTTBSo", nodeTToan, XPathConstants.NODE)))
			);
			reportParams.put("TToanTgTTTBChu", 
				commons.getTextFromNodeXML((Element) xPath.evaluate("TgTTTBChu", nodeTToan, XPathConstants.NODE))
			);
			
			/*LAY DANH SACH LAI SUAT*/
			String tsuatTmp = "";
			NodeList nodeListLTSuat = null;
			try {
				nodeListLTSuat = (NodeList) xPath.evaluate("THTTLTSuat/LTSuat", nodeTToan, XPathConstants.NODESET);
				if(nodeListLTSuat != null && nodeListLTSuat.getLength() > 0) {
					for(int i = 0; i < nodeListLTSuat.getLength(); i++) {
						nodeTmp = nodeListLTSuat.item(i);
						
						tsuatTmp = commons.getTextFromNodeXML((Element) xPath.evaluate("TSuat", nodeTmp, XPathConstants.NODE)).replaceAll("%", "");
						reportParams.put("Tax_TSuat_" + tsuatTmp, tsuatTmp);
						reportParams.put("Tax_ThTien_" + tsuatTmp, 
							commons.ToNumber(commons.getTextFromNodeXML((Element) xPath.evaluate("ThTien", nodeTmp, XPathConstants.NODE)))
						);
						reportParams.put("Tax_TThue_" + tsuatTmp, 
							commons.ToNumber(commons.getTextFromNodeXML((Element) xPath.evaluate("TThue", nodeTmp, XPathConstants.NODE)))
						);
						
						if(reportParams.get("Tax_TSuat_Default") == null) {
							reportParams.put("Tax_TSuat_Default", tsuatTmp);
							reportParams.put("Tax_ThTien_Default", 
								commons.ToNumber(commons.getTextFromNodeXML((Element) xPath.evaluate("ThTien", nodeTmp, XPathConstants.NODE)))
							);
							reportParams.put("Tax_TThue_Default", 
								commons.ToNumber(commons.getTextFromNodeXML((Element) xPath.evaluate("TThue", nodeTmp, XPathConstants.NODE)))
							);
						}
					}
				}
			}catch(Exception e) {}
			/*END - LAY DANH SACH LAI SUAT*/
			
			/*KIEM TRA THONG TIN NGUOI BAN KY CHUA*/
			Node nodeDSCKS = (Node) xPath.evaluate("DSCKS", nodeHDon, XPathConstants.NODE);
			Node nodeSignature = null;
			if(null != nodeDSCKS)
				nodeSignature = (Node) xPath.evaluate("NBan/Signature", nodeDSCKS, XPathConstants.NODE);
			LocalDateTime ldt = null;
			if(null == nodeSignature) {
				reportParams.put("IsSigned", false);
				reportParams.put("SignDesc", "Chưa ký");
				reportParams.put("UrlImageVerify", Paths.get(SystemParams.DIR_E_INVOICE_TEMPLATE, Constants.TEMPLATE_FILE_NAME.IMG_SIGNATURE_INVALID).toString());
			}else {
				String x509Certificate = commons.getTextFromNodeXML((Element) xPath.evaluate("KeyInfo/X509Data/X509Certificate", nodeSignature, XPathConstants.NODE));
				String signingTime = commons.getTextFromNodeXML((Element) xPath.evaluate("Object[@Id='SigningTime']/SignatureProperties/SignatureProperty/SigningTime", nodeSignature, XPathConstants.NODE));
				
				reportParams.put("IsSigned", true);
				reportParams.put("SignDesc", "Đã ký");
				SignTypeInfo signTypeInfo = commons.parserCert(x509Certificate);
				reportParams.put("SignName", null == signTypeInfo? "": signTypeInfo.getName());
				try {
					ldt = commons.convertStringToLocalDateTime(signingTime, "yyyy-MM-dd'T'HH:mm:ss'Z'");
					reportParams.put("SignDate", commons.convertLocalDateTimeToString(ldt, Constants.FORMAT_DATE.FORMAT_DATE_WEB));
				}catch(Exception e) {}
				reportParams.put("UrlImageVerify", Paths.get(SystemParams.DIR_E_INVOICE_TEMPLATE, Constants.TEMPLATE_FILE_NAME.IMG_SIGNATURE_VALID).toString());
			}
			reportParams.put("UrlImageInvDeleted", Paths.get(SystemParams.DIR_E_INVOICE_TEMPLATE, Constants.TEMPLATE_FILE_NAME.IMG_INV_DELETED).toString());
			reportParams.put("IsDeleted", isDeleted);
			reportParams.put("UrlImageInvThayThe", Paths.get(SystemParams.DIR_E_INVOICE_TEMPLATE, Constants.TEMPLATE_FILE_NAME.IMG_INV_THAYTHE).toString());
			reportParams.put("IsThayThe", isThayThe);
			
			List<HashMap<String, Object>> arrayData = new ArrayList<>();
			HashMap<String, Object> hItem = null;
			
			int lenProName = 0;
			String productName = "";
			int startRowGroup = 0;
			
			/*XEM CO IN NHIEU DONG KHONG*/
			int groupPageIDX = 1;
			boolean isUsingMultiPage = false;
			if(nodeListHHDVu != null) {
				for(int i = 0; i < nodeListHHDVu.getLength(); i++) {
					nodeTmp = nodeListHHDVu.item(i);
					
					productName = commons.getTextFromNodeXML((Element) xPath.evaluate("THHDVu", nodeTmp, XPathConstants.NODE));
					lenProName = productName.getBytes(StandardCharsets.UTF_8).length;
					startRowGroup += Math.ceil((double) lenProName / numberCharsInRow);
					
					if(startRowGroup >= numberRowInPage) {
						isUsingMultiPage = true;
						break;
					}
				}	
			}
			/*END - XEM CO IN NHIEU DONG KHONG*/
			
			startRowGroup = 0;
			KDLieu = "";
			nodeTmp = null;
			nodeSubTmp = null;
			nodeListTTKhac = null;
			countPrd = 0;
			
			if(nodeListHHDVu != null) {
				if(isUsingMultiPage) {
					for(int i = 0; i < nodeListHHDVu.getLength(); i++) {
						nodeTmp = nodeListHHDVu.item(i);
						String SLNhap = "";
						productName = commons.getTextFromNodeXML((Element) xPath.evaluate("THHDVu", nodeTmp, XPathConstants.NODE));
						lenProName = productName.getBytes(StandardCharsets.UTF_8).length;
						startRowGroup += Math.ceil((double) lenProName / numberCharsInRow);
//						countRowForPrd = (int) Math.ceil(lenProName / numberCharsInRow);
//						if(countRowForPrd > 1)
//							i = i + countRowForPrd;
						
						hItem = new HashMap<>();
						hItem.put("GroupPageIDX", groupPageIDX);
						
						hItem.put("TChat", commons.getTextFromNodeXML((Element) xPath.evaluate("TChat", nodeTmp, XPathConstants.NODE)));
						hItem.put("STT", commons.getTextFromNodeXML((Element) xPath.evaluate("STT", nodeTmp, XPathConstants.NODE)));
						hItem.put("MSo", commons.getTextFromNodeXML((Element) xPath.evaluate("MHHDVu", nodeTmp, XPathConstants.NODE)));
						
						hItem.put("THHDVu", productName);
						
						hItem.put("DVTinh", commons.getTextFromNodeXML((Element) xPath.evaluate("DVTinh", nodeTmp, XPathConstants.NODE)));
						hItem.put("SLThucXuat",
								commons.ToNumber(commons.getTextFromNodeXML((Element) xPath.evaluate("SLuong", nodeTmp, XPathConstants.NODE)))
							);
						hItem.put("DGia",
							commons.ToNumber(commons.getTextFromNodeXML((Element) xPath.evaluate("DGia", nodeTmp, XPathConstants.NODE)))
						);
						hItem.put("ThTien",
							commons.ToNumber(commons.getTextFromNodeXML((Element) xPath.evaluate("ThTien", nodeTmp, XPathConstants.NODE)))
						);
						hItem.put("TSuat",
							commons.getTextFromNodeXML((Element) xPath.evaluate("TSuat", nodeTmp, XPathConstants.NODE))
						);
						
						/*LAY NOI DUNG TTKHAC*/
						nodeListTTKhac = (NodeList) xPath.evaluate("TTKhac/TTin", nodeTmp, XPathConstants.NODESET);
						if(nodeListTTKhac != null) {
							for(int j = 0; j < nodeListTTKhac.getLength(); j++) {
								nodeSubTmp = nodeListTTKhac.item(j);
								
								TTruong = commons.getTextFromNodeXML((Element) xPath.evaluate("TTruong", nodeSubTmp, XPathConstants.NODE));
		                        if("SLNhap".equals(TTruong)) {
		                  SLNhap =commons.getTextFromNodeXML((Element) xPath.evaluate("DLieu", nodeSubTmp, XPathConstants.NODE));
		            
		                      }
								
								KDLieu = commons.getTextFromNodeXML((Element) xPath.evaluate("KDLieu", nodeSubTmp, XPathConstants.NODE));
								switch (KDLieu.toUpperCase()) {
								case "DECIMAL":
									hItem.put(
										commons.getTextFromNodeXML((Element) xPath.evaluate("TTruong", nodeSubTmp, XPathConstants.NODE)),
										commons.ToNumber(commons.getTextFromNodeXML((Element) xPath.evaluate("DLieu", nodeSubTmp, XPathConstants.NODE)))
									);
									break;
								default:
									hItem.put(
										commons.getTextFromNodeXML((Element) xPath.evaluate("TTruong", nodeSubTmp, XPathConstants.NODE)),
										commons.getTextFromNodeXML((Element) xPath.evaluate("DLieu", nodeSubTmp, XPathConstants.NODE))
									);
									break;
								}
							}
						}
						
						arrayData.add(hItem);
						countPrd++;	
					
						if(startRowGroup >= numberRowInPageMultiPage) {
							startRowGroup = 0;
							groupPageIDX++;
						}
						hItem.put("SLThucNhap",
				                commons.ToNumber(SLNhap)
				              );
					}
					if(0 == startRowGroup && nodeListHHDVu.getLength() > 0) groupPageIDX--;		//BO BOT TRANG CUOI CUNG VI KHONG CO DU LIEU
					
					if(0 == startRowGroup) {
						groupPageIDX++;
						for(int i = 0; i < numberRowInPage; i++) {
							hItem = new HashMap<>();
							hItem.put("ProdName", " ");
							hItem.put("GroupPageIDX", groupPageIDX);
							arrayData.add(hItem);
						}
					}else if(startRowGroup <= numberRowInPage) {
						for(int i = startRowGroup; i < numberRowInPage; i++) {
							hItem = new HashMap<>();
							hItem.put("ProdName", " ");
							hItem.put("GroupPageIDX", groupPageIDX);
							arrayData.add(hItem);
						}
					}else {
						for(int i = startRowGroup; i < numberRowInPageMultiPage; i++) {
							hItem = new HashMap<>();
							hItem.put("ProdName", " ");
							hItem.put("GroupPageIDX", groupPageIDX);
							arrayData.add(hItem);
						}
						groupPageIDX++;
						for(int i = 0; i < numberRowInPage; i++) {
							hItem = new HashMap<>();
							hItem.put("ProdName", " ");
							hItem.put("GroupPageIDX", groupPageIDX);
							arrayData.add(hItem);
						}
					}
				}else {
					for(int i = 0; i < nodeListHHDVu.getLength(); i++) {
						nodeTmp = nodeListHHDVu.item(i);
					  String SLNhap = "";
						productName = commons.getTextFromNodeXML((Element) xPath.evaluate("THHDVu", nodeTmp, XPathConstants.NODE));
						lenProName = productName.getBytes(StandardCharsets.UTF_8).length;
						startRowGroup += Math.ceil((double) lenProName / numberCharsInRow);
//						countRowForPrd = (int) Math.ceil(lenProName / numberCharsInRow);
//						if(countRowForPrd > 1)
//							i = i + countRowForPrd;
						
						hItem = new HashMap<>();
						hItem.put("GroupPageIDX", groupPageIDX);
						
						hItem.put("TChat", commons.getTextFromNodeXML((Element) xPath.evaluate("TChat", nodeTmp, XPathConstants.NODE)));
						hItem.put("STT", commons.getTextFromNodeXML((Element) xPath.evaluate("STT", nodeTmp, XPathConstants.NODE)));
						hItem.put("MSo", commons.getTextFromNodeXML((Element) xPath.evaluate("MHHDVu", nodeTmp, XPathConstants.NODE)));
						
						hItem.put("THHDVu", productName);
						
						hItem.put("DVTinh", commons.getTextFromNodeXML((Element) xPath.evaluate("DVTinh", nodeTmp, XPathConstants.NODE)));
						hItem.put("SLThucXuat",
							commons.ToNumber(commons.getTextFromNodeXML((Element) xPath.evaluate("SLuong", nodeTmp, XPathConstants.NODE)))
						);
						hItem.put("DGia",
							commons.ToNumber(commons.getTextFromNodeXML((Element) xPath.evaluate("DGia", nodeTmp, XPathConstants.NODE)))
						);
						hItem.put("ThTien",
							commons.ToNumber(commons.getTextFromNodeXML((Element) xPath.evaluate("ThTien", nodeTmp, XPathConstants.NODE)))
						);
						hItem.put("TSuat",
							commons.getTextFromNodeXML((Element) xPath.evaluate("TSuat", nodeTmp, XPathConstants.NODE))
						);
						
						/*LAY NOI DUNG TTKHAC*/
						nodeListTTKhac = (NodeList) xPath.evaluate("TTKhac/TTin", nodeTmp, XPathConstants.NODESET);
						if(nodeListTTKhac != null) {
							for(int j = 0; j < nodeListTTKhac.getLength(); j++) {
								nodeSubTmp = nodeListTTKhac.item(j);
								
								
								TTruong = commons.getTextFromNodeXML((Element) xPath.evaluate("TTruong", nodeSubTmp, XPathConstants.NODE));
				                if("SLNhap".equals(TTruong)) {
		              SLNhap =commons.getTextFromNodeXML((Element) xPath.evaluate("DLieu", nodeSubTmp, XPathConstants.NODE));
			      
			                }
				                
								KDLieu = commons.getTextFromNodeXML((Element) xPath.evaluate("KDLieu", nodeSubTmp, XPathConstants.NODE));
								switch (KDLieu.toUpperCase()) {
								case "DECIMAL":
									hItem.put(
										commons.getTextFromNodeXML((Element) xPath.evaluate("TTruong", nodeSubTmp, XPathConstants.NODE)),
										commons.ToNumber(commons.getTextFromNodeXML((Element) xPath.evaluate("DLieu", nodeSubTmp, XPathConstants.NODE)))
									);
									break;
								default:
									hItem.put(
										commons.getTextFromNodeXML((Element) xPath.evaluate("TTruong", nodeSubTmp, XPathConstants.NODE)),
										commons.getTextFromNodeXML((Element) xPath.evaluate("DLieu", nodeSubTmp, XPathConstants.NODE))
									);
									break;
								}
							}
						}
						
						arrayData.add(hItem);
						countPrd++;	
						
						if(startRowGroup >= numberRowInPage) {
							startRowGroup = 0;
							groupPageIDX++;
						}
						
						hItem.put("SLThucNhap",
								commons.ToNumber(SLNhap)
							);
					}
					if(0 == startRowGroup && nodeListHHDVu.getLength() > 0) groupPageIDX--;		//BO BOT TRANG CUOI CUNG VI KHONG CO DU LIEU
					
					/*ADD THEM DU LIEU CHO DU TRANG*/
					if(startRowGroup > 0 || nodeListHHDVu.getLength() == 0) {
						for(int i = startRowGroup; i < numberRowInPage; i++) {
							hItem = new HashMap<>();
							hItem.put("ProdName", " ");
							hItem.put("GroupPageIDX", groupPageIDX);
							arrayData.add(hItem);
						}
					}
				}
				
//				for(int i = 0; i < nodeListHHDVu.getLength(); i++) {
//					nodeTmp = nodeListHHDVu.item(i);
//					
//					productName = commons.getTextFromNodeXML((Element) xPath.evaluate("THHDVu", nodeTmp, XPathConstants.NODE));
//					lenProName = productName.getBytes(StandardCharsets.UTF_8).length;
//					startRowGroup += Math.ceil((double) lenProName / numberCharsInRow);
////					countRowForPrd = (int) Math.ceil(lenProName / numberCharsInRow);
////					if(countRowForPrd > 1)
////						i = i + countRowForPrd;
//					
//					hItem = new HashMap<>();
//					hItem.put("GroupPageIDX", 0);
//					
//					hItem.put("TChat", commons.getTextFromNodeXML((Element) xPath.evaluate("TChat", nodeTmp, XPathConstants.NODE)));
//					hItem.put("STT", commons.getTextFromNodeXML((Element) xPath.evaluate("STT", nodeTmp, XPathConstants.NODE)));
//					hItem.put("MHHDVu", commons.getTextFromNodeXML((Element) xPath.evaluate("MHHDVu", nodeTmp, XPathConstants.NODE)));
//					
//					hItem.put("THHDVu", productName);
//					
//					hItem.put("DVTinh", commons.getTextFromNodeXML((Element) xPath.evaluate("DVTinh", nodeTmp, XPathConstants.NODE)));
//					hItem.put("SLuong",
//						commons.ToNumber(commons.getTextFromNodeXML((Element) xPath.evaluate("SLuong", nodeTmp, XPathConstants.NODE)))
//					);
//					hItem.put("DGia",
//						commons.ToNumber(commons.getTextFromNodeXML((Element) xPath.evaluate("DGia", nodeTmp, XPathConstants.NODE)))
//					);
//					hItem.put("ThTien",
//						commons.ToNumber(commons.getTextFromNodeXML((Element) xPath.evaluate("ThTien", nodeTmp, XPathConstants.NODE)))
//					);
//					hItem.put("TSuat",
//						commons.getTextFromNodeXML((Element) xPath.evaluate("TSuat", nodeTmp, XPathConstants.NODE))
//					);
//					
//					/*LAY NOI DUNG TTKHAC*/
//					nodeListTTKhac = (NodeList) xPath.evaluate("TTKhac/TTin", nodeTmp, XPathConstants.NODESET);
//					if(nodeListTTKhac != null) {
//						for(int j = 0; j < nodeListTTKhac.getLength(); j++) {
//							nodeSubTmp = nodeListTTKhac.item(j);
//							
//							KDLieu = commons.getTextFromNodeXML((Element) xPath.evaluate("KDLieu", nodeSubTmp, XPathConstants.NODE));
//							switch (KDLieu.toUpperCase()) {
//							case "DECIMAL":
//								hItem.put(
//									commons.getTextFromNodeXML((Element) xPath.evaluate("TTruong", nodeSubTmp, XPathConstants.NODE)),
//									commons.ToNumber(commons.getTextFromNodeXML((Element) xPath.evaluate("DLieu", nodeSubTmp, XPathConstants.NODE)))
//								);
//								break;
//							default:
//								hItem.put(
//									commons.getTextFromNodeXML((Element) xPath.evaluate("TTruong", nodeSubTmp, XPathConstants.NODE)),
//									commons.getTextFromNodeXML((Element) xPath.evaluate("DLieu", nodeSubTmp, XPathConstants.NODE))
//								);
//								break;
//							}
//						}
//					}
//					
//					arrayData.add(hItem);
//					countPrd++;	
//				}
			}		
//			if(startRowGroup < numberRowInPage) {
//				for(int i = startRowGroup; i< numberRowInPage; i++) {
//					hItem = new HashMap<>();
//					hItem.put("GroupPageIDX", 0);
//					arrayData.add(hItem);
//				}
//				
//			}
			reportParams.put("PortalLink", 
					link
			);
			
			reportParams.put("TOTAL_PAGE", (int) groupPageIDX);
			
			JRDataSource jds = null;
			jds = new JRBeanCollectionDataSource(arrayData);
			
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			JasperReport jr = JasperCompileManager.compileReport(new FileInputStream(fileJP));
			JasperPrint jp = JasperFillManager.fillReport(jr, reportParams, jds);
			Exporter exporter = null;
			
			exporter = new JRPdfExporter();
			exporter.setExporterInput(new SimpleExporterInput(jp));
			exporter.setExporterOutput(new SimpleOutputStreamExporterOutput(out));
	        SimplePdfExporterConfiguration configuration = new SimplePdfExporterConfiguration();
	        configuration.setCreatingBatchModeBookmarks(true);
	        exporter.setConfiguration(configuration);
	        exporter.exportReport();
	        return out;
		}


	

	
	public ByteArrayOutputStream viewpdf(File fileJP, org.bson.Document docTmp, int numberRowInPage,
			int numberRowInPageMultiPage, int numberCharsInRow, String pathLogo, String pathBackground,String pathQA, String pathVien, boolean isConvert) throws Exception{
			Map<String, Object> reportParams = new HashMap<String, Object>();
			
			/*KIEM TRA FILE LOG & BACKGROUND CO TON TAI KHONG*/
			File f = new File(pathLogo);
			if(f.exists() && f.isFile())
				reportParams.put("URL_IMG_LOGO", pathLogo);
			else
				reportParams.put("URL_IMG_LOGO", Paths.get(SystemParams.DIR_E_INVOICE_TEMPLATE, IMAGE_FILENAME_BLANK).toString());
			
			f = new File(pathBackground);
			if(f.exists() && f.isFile())
				reportParams.put("URL_IMG_BACKGROUND", pathBackground);
			else
				reportParams.put("URL_IMG_BACKGROUND", Paths.get(SystemParams.DIR_E_INVOICE_TEMPLATE, IMAGE_FILENAME_BLANK).toString());
			f = new File(pathQA);
			if(f.exists() && f.isFile())
				reportParams.put("URL_QR_INFO", pathQA);
			else
				reportParams.put("URL_QR_INFO", Paths.get(SystemParams.DIR_E_INVOICE_TEMPLATE, IMAGE_FILENAME_BLANK).toString());
		
			f = new File(pathVien);
			if(f.exists() && f.isFile())
				reportParams.put("URL_IMG_FRAME", pathVien);
			else
				reportParams.put("URL_IMG_FRAME", Paths.get(SystemParams.DIR_E_INVOICE_TEMPLATE, IMAGE_FILENAME_BLANK).toString());
		
			
			
			
			reportParams.put("IsConvert", isConvert);


			reportParams.put("KHHDon", docTmp.getEmbedded(Arrays.asList( "KHMSHDon"), "") + docTmp.getEmbedded(Arrays.asList( "KHHDon"), "")
						);
		
			reportParams.put("NBanTen", 
					docTmp.getEmbedded(Arrays.asList("Issuer","Name"), "")
			);
			reportParams.put("NBanFax", 
					docTmp.getEmbedded(Arrays.asList("Issuer","Fax"), "")
			);
			reportParams.put("NBanSTKNHang", 
					docTmp.getEmbedded(Arrays.asList("Issuer","BankAccount","AccountNumber"), "")
			);
			reportParams.put("NBanTNHang", 
					docTmp.getEmbedded(Arrays.asList("Issuer","BankAccount","BankName"), "")
			);
			reportParams.put("NBanWebsite", 
					docTmp.getEmbedded(Arrays.asList("Issuer","Website"), "")
			);
			reportParams.put("NBanDCTDTu", 
					docTmp.getEmbedded(Arrays.asList("Issuer","Email"), "")
			);
			reportParams.put("NBanMST",	docTmp.getEmbedded(Arrays.asList("Issuer","TaxCode"), ""));
			reportParams.put("NBanDChi",	docTmp.getEmbedded(Arrays.asList("Issuer","Address"), ""));
			reportParams.put("NBanSDThoai",docTmp.getEmbedded(Arrays.asList("Issuer","Phone"), "")	);
			
			
			List<HashMap<String, Object>> arrayData = new ArrayList<>();
			HashMap<String, Object> hItem = null;
			
			int lenProName = 0;
			String productName = "";
			int startRowGroup = 0;
			
			
			startRowGroup = 0;
		
			
			JRDataSource jds = null;
			jds = new JRBeanCollectionDataSource(arrayData);
			
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			JasperReport jr = JasperCompileManager.compileReport(new FileInputStream(fileJP));
			JasperPrint jp = JasperFillManager.fillReport(jr, reportParams, jds);
			Exporter exporter = null;
			
			exporter = new JRPdfExporter();
			exporter.setExporterInput(new SimpleExporterInput(jp));
			exporter.setExporterOutput(new SimpleOutputStreamExporterOutput(out));
	        SimplePdfExporterConfiguration configuration = new SimplePdfExporterConfiguration();
	        configuration.setCreatingBatchModeBookmarks(true);
	        exporter.setConfiguration(configuration);
	        exporter.exportReport();
	        return out;
	}	


	
	
	 public ByteArrayOutputStream print04(File fileJP, Document doc, org.bson.Document docTmp, int numberRowInPage, int numberRowInPageMultiPage, int numberCharsInRow, String pathLogo, String pathBackground, boolean isConvert) throws Exception {
	      Map<String, Object> reportParams = new HashMap();
	      File f = new File(pathLogo);
	      if (f.exists() && f.isFile()) {
	         reportParams.put("URL_IMG_LOGO", pathLogo);
	      } else {
	         reportParams.put("URL_IMG_LOGO", Paths.get(SystemParams.DIR_E_INVOICE_TEMPLATE, "BLANK_ICON.png").toString());
	      }
	     // reportParams.put("UrlImageConfirmed", Paths.get(SystemParams.DIR_E_INVOICE_TEMPLATE, "tick.png").toString());
	     // reportParams.put("IsTaxConfirmed", true);
	      f = new File(pathBackground);
	      if (f.exists() && f.isFile()) {
	         reportParams.put("URL_IMG_BACKGROUND", pathBackground);
	      } else {
	         reportParams.put("URL_IMG_BACKGROUND", Paths.get(SystemParams.DIR_E_INVOICE_TEMPLATE, "BLANK_ICON.png").toString());
	      }

	      reportParams.put("IsConvert", isConvert);
	      String NLap = "";
	      String KDLieu = "";
	      Node nodeTmp = null;
	      Node nodeSubTmp = null;
	      NodeList nodeListTTKhac = null;
	      int countPrd = 0;
	      XPath xPath = XPathFactory.newInstance().newXPath();
	      Node nodeHDon = null;

	      for(int i = 1; i <= 20; ++i) {
	         nodeHDon = (Node)xPath.evaluate("/KetQuaTraCuu/DuLieu/TDiep[" + i + "]/DLieu/HDon", doc, XPathConstants.NODE);
	         if (nodeHDon != null) {
	            break;
	         }
	      }

	      if (null == nodeHDon) {
	         nodeHDon = (Node)xPath.evaluate("/TBao", doc, XPathConstants.NODE);
	      }

	      NodeList nodeListHHDVu = (NodeList)xPath.evaluate("DLTBao/DSHDon/HDon", nodeHDon, XPathConstants.NODESET);
	      LocalDate localDateNLap = null;
	      NLap = this.commons.getTextFromNodeXML((Element)xPath.evaluate("DLTBao/DSHDon/NTBao", nodeHDon, XPathConstants.NODE));
	      if (!"".equals(NLap)) {
	         try {
	            localDateNLap = this.commons.convertStringToLocalDate(NLap, "yyyy-MM-dd");
	         } catch (Exception var40) {
	         }
	      }
	      String TNNT = "";
	      String MST = "";
	      String check_TNNT = docTmp.getEmbedded(Arrays.asList("Issuer", "Name"), "");
	      String check_TNNT1 = docTmp.get("TNNT","");
	      if(!check_TNNT.equals("")) {
	    	  TNNT = check_TNNT;
	      }else {
	    	  TNNT =  check_TNNT1;
	      }
	      
	      String check_MST = docTmp.getEmbedded(Arrays.asList("Issuer", "TaxCode"), "");
	      String check_MST1 = docTmp.get("MST","");
	      if(!check_MST.equals("")) {
	    	  MST = check_MST;
	      }else {
	    	  MST =  check_MST1;
	      }
	    
	      reportParams.put("CQTQLy", (String)docTmp.getEmbedded(Arrays.asList("DDanh"), "") + "-" + (String)docTmp.getEmbedded(Arrays.asList("TCQT"), ""));
	      reportParams.put("NNTTen", TNNT);
	      reportParams.put("NNTMST", MST);
	      List<HashMap<String, Object>> arrayData = new ArrayList();
	      HashMap<String, Object> hItem = null;
	      int lenProName = 0;
	      String productName = "";
	      String sLo = "";
	      String hanSD = "";
	      int startRowGroup = 0;
	      int groupPageIDX = 1;
	      boolean isUsingMultiPage = false;
	      int i;
	      lenProName = 0;
	      if (nodeListHHDVu != null) {
	         for(i = 0; i < nodeListHHDVu.getLength(); ++i) {
	            nodeTmp = nodeListHHDVu.item(i);
	            productName = this.commons.getTextFromNodeXML((Element)xPath.evaluate("MCQTCap", nodeTmp, XPathConstants.NODE));
	            if(productName.equals("")) {
	            	   productName = this.commons.getTextFromNodeXML((Element)xPath.evaluate("MCCQT", nodeTmp, XPathConstants.NODE));	     	          
	            }
	           	            
	            lenProName = productName.getBytes(StandardCharsets.UTF_8).length;
	            startRowGroup = (int)((double)startRowGroup + Math.ceil((double)lenProName / (double)numberCharsInRow));
	            if (startRowGroup >= numberRowInPage) {
	               isUsingMultiPage = true;
	               break;
	            }
	         }
	      }

	      startRowGroup = 0;
	      KDLieu = "";
	      nodeTmp = null;
	      nodeSubTmp = null;
	      nodeListTTKhac = null;
	       countPrd = 0;
	      String thang;
	      
	      
	      
	      
	      
	      
	      
			/*KIEM TRA THONG TIN NGUOI BAN KY CHUA*/
			Node nodeDSCKS = (Node) xPath.evaluate("DSCKS", nodeHDon, XPathConstants.NODE);
			Node nodeSignature = null;
			if(null != nodeDSCKS)
				nodeSignature = (Node) xPath.evaluate("NNT/Signature", nodeDSCKS, XPathConstants.NODE);
			LocalDateTime ldt = null;
				String x509Certificate = commons.getTextFromNodeXML((Element) xPath.evaluate("KeyInfo/X509Data/X509Certificate", nodeSignature, XPathConstants.NODE));
				String signingTime = commons.getTextFromNodeXML((Element) xPath.evaluate("Object[@Id='SigningTime']/SignatureProperties/SignatureProperty/SigningTime", nodeSignature, XPathConstants.NODE));
				reportParams.put("UrlImageVerify", Paths.get(SystemParams.DIR_E_INVOICE_TEMPLATE, Constants.TEMPLATE_FILE_NAME.IMG_SIGNATURE_VALID).toString());
				reportParams.put("IsSigned", true);
				reportParams.put("SignDesc", "Đã ký");
				SignTypeInfo signTypeInfo = commons.parserCert(x509Certificate);
				reportParams.put("SignName", null == signTypeInfo? "": signTypeInfo.getName());



	      
	      
	      
	      
	      
	      if (nodeListHHDVu != null) {
	         String ngayc;
	         String[] words;
	         String nam;
	         String ngay;
	         String ngayview;
	         String ladhddt;
	         String tctb;
	         if (isUsingMultiPage) {
	            for(i = 0; i < nodeListHHDVu.getLength(); ++i) {
	               nodeTmp = nodeListHHDVu.item(i);
	               productName = this.commons.getTextFromNodeXML((Element)xPath.evaluate("MCQTCap", nodeTmp, XPathConstants.NODE));
	               if(productName.equals("")) {
	            	   productName = this.commons.getTextFromNodeXML((Element)xPath.evaluate("MCCQT", nodeTmp, XPathConstants.NODE));	     	          
	            }
	               lenProName = productName.getBytes(StandardCharsets.UTF_8).length;
	               startRowGroup = (int)((double)startRowGroup + Math.ceil((double)lenProName / (double)numberCharsInRow));
	               hItem = new HashMap();
	               hItem.put("GroupPageIDX", groupPageIDX);
	               hItem.put("STT", this.commons.getTextFromNodeXML((Element)xPath.evaluate("STT", nodeTmp, XPathConstants.NODE)));
	               hItem.put("MCQTCap", productName);
	               hItem.put("MSHDon", this.commons.getTextFromNodeXML((Element)xPath.evaluate("KHMSHDon", nodeTmp, XPathConstants.NODE)) + this.commons.getTextFromNodeXML((Element)xPath.evaluate("KHHDon", nodeTmp, XPathConstants.NODE)));
	               hItem.put("SHDon", this.commons.getTextFromNodeXML((Element)xPath.evaluate("SHDon", nodeTmp, XPathConstants.NODE)));
	               ngayc = this.commons.getTextFromNodeXML((Element)xPath.evaluate("Ngay", nodeTmp, XPathConstants.NODE));
	               String ngayky = (String) docTmp.get("NTBao");
	               words = ngayc.split("-");
	               nam = words[0];
	               thang = words[1];
	               ngay = words[2];
	               ngayview = ngay + "-" + thang + "-" + nam;
	                             
	               words.clone();
	               nam = "";
	               thang = "";
	               ngay ="";
	               words = ngayky.split("-");
	               nam = words[0];
	               thang = words[1];
	               ngay = words[2];
	              String  ngaykyview = ngay + "-" + thang + "-" + nam;
	             
	               	               
	               hItem.put("NLHDon", ngayview);
	               hItem.put("SignDate", ngayview);
		           	reportParams.put("SignDate", ngaykyview);
	               
	               reportParams.put("InvoiceDate",ngay);
		  	         reportParams.put("InvoiceMonth", thang);
		  	         reportParams.put("InvoiceYear", nam);
	           
	               ladhddt = this.commons.getTextFromNodeXML((Element)xPath.evaluate("LADHDDT", nodeTmp, XPathConstants.NODE));
	               if ("1".equals(ladhddt)) {
	                  hItem.put("LADHDDT", "Thông báo hủy/giải trình của NNT");
	               }

	               if ("2".equals(ladhddt)) {
	                  hItem.put("LADHDDT", "Thông báo hủy/giải trình của NNT theo thông báo của CQT");
	               }

	               tctb = this.commons.getTextFromNodeXML((Element)xPath.evaluate("TCTBao", nodeTmp, XPathConstants.NODE));
	               if ("1".equals(tctb)) {
	                  hItem.put("TCTBao", "Hủy");
	               }

	               if ("2".equals(tctb)) {
	                  hItem.put("TCTBao", "Điều chỉnh");
	               }

	               if ("3".equals(tctb)) {
	                  hItem.put("TCTBao", "Thay thế");
	               }

	               if ("4".equals(tctb)) {
	                  hItem.put("TCTBao", "Giải trình");
	               }

	               hItem.put("LDo", this.commons.getTextFromNodeXML((Element)xPath.evaluate("LDo", nodeTmp, XPathConstants.NODE)));
	               arrayData.add(hItem);
	               ++countPrd;
	               if (startRowGroup >= numberRowInPageMultiPage) {
	                  startRowGroup = 0;
	                  ++groupPageIDX;
	               }
	            }

	            if (0 == startRowGroup && nodeListHHDVu.getLength() > 0) {
	               --groupPageIDX;
	            }

	            if (0 == startRowGroup) {
	               ++groupPageIDX;

	               for(i = 0; i < numberRowInPage; ++i) {
	                  hItem = new HashMap();
	                  hItem.put("ProdName", " ");
	                  hItem.put("GroupPageIDX", groupPageIDX);
	                  arrayData.add(hItem);
	               }
	            } else if (startRowGroup <= numberRowInPage) {
	               for(i = startRowGroup; i < numberRowInPage; ++i) {
	                  hItem = new HashMap();
	                  hItem.put("ProdName", " ");
	                  hItem.put("GroupPageIDX", groupPageIDX);
	                  arrayData.add(hItem);
	               }
	            } else {
	               for(i = startRowGroup; i < numberRowInPageMultiPage; ++i) {
	                  hItem = new HashMap();
	                  hItem.put("ProdName", " ");
	                  hItem.put("GroupPageIDX", groupPageIDX);
	                  arrayData.add(hItem);
	               }

	               ++groupPageIDX;

	               for(i = 0; i < numberRowInPage; ++i) {
	                  hItem = new HashMap();
	                  hItem.put("ProdName", " ");
	                  hItem.put("GroupPageIDX", groupPageIDX);
	                  arrayData.add(hItem);
	               }
	            }
	         } else {
	            for(i = 0; i < nodeListHHDVu.getLength(); ++i) {
	               nodeTmp = nodeListHHDVu.item(i);
	               productName = this.commons.getTextFromNodeXML((Element)xPath.evaluate("MCQTCap", nodeTmp, XPathConstants.NODE));
	               if(productName.equals("")) {
	            	   productName = this.commons.getTextFromNodeXML((Element)xPath.evaluate("MCCQT", nodeTmp, XPathConstants.NODE));	     	          
	            }
	               lenProName = productName.getBytes(StandardCharsets.UTF_8).length;
	               startRowGroup = (int)((double)startRowGroup + Math.ceil((double)lenProName / (double)numberCharsInRow));
	               hItem = new HashMap();
	               hItem.put("GroupPageIDX", groupPageIDX);
	               hItem = new HashMap();
	               hItem.put("GroupPageIDX", groupPageIDX);
	               hItem.put("STT", this.commons.getTextFromNodeXML((Element)xPath.evaluate("STT", nodeTmp, XPathConstants.NODE)));
	               hItem.put("MCQTCap", productName);
	               hItem.put("MSHDon", this.commons.getTextFromNodeXML((Element)xPath.evaluate("KHMSHDon", nodeTmp, XPathConstants.NODE)) + this.commons.getTextFromNodeXML((Element)xPath.evaluate("KHHDon", nodeTmp, XPathConstants.NODE)));
	               hItem.put("SHDon", this.commons.getTextFromNodeXML((Element)xPath.evaluate("SHDon", nodeTmp, XPathConstants.NODE)));
	               ngayc = this.commons.getTextFromNodeXML((Element)xPath.evaluate("Ngay", nodeTmp, XPathConstants.NODE));
	            String ngayky = (String) docTmp.get("NTBao");
	               
	               words = ngayc.split("-");
	               nam = words[0];
	               thang = words[1];
	               ngay = words[2];
	               ngayview = ngay + "-" + thang + "-" + nam;
	               
	               words.clone();
	               nam = "";
	               thang = "";
	               ngay ="";
	               words = ngayky.split("-");
	               nam = words[0];
	               thang = words[1];
	               ngay = words[2];
	              String  ngaykyview = ngay + "-" + thang + "-" + nam;
	               
	               hItem.put("NLHDon", ngayview);
	               hItem.put("SignDate", ngayview);
	           	reportParams.put("SignDate", ngaykyview);
	           	
	            reportParams.put("InvoiceDate",ngay);
	  	         reportParams.put("InvoiceMonth", thang);
	  	         reportParams.put("InvoiceYear", nam);
	  	    
	           	
	               ladhddt = this.commons.getTextFromNodeXML((Element)xPath.evaluate("LADHDDT", nodeTmp, XPathConstants.NODE));
	               if ("1".equals(ladhddt)) {
	                  hItem.put("LADHDDT", "Thông báo hủy/giải trình của NNT");
	               }

	               if ("2".equals(ladhddt)) {
	                  hItem.put("LADHDDT", "Thông báo hủy/giải trình của NNT theo thông báo của CQT");
	               }

	               tctb = this.commons.getTextFromNodeXML((Element)xPath.evaluate("TCTBao", nodeTmp, XPathConstants.NODE));
	               if ("1".equals(tctb)) {
	                  hItem.put("TCTBao", "Hủy");
	               }

	               if ("2".equals(tctb)) {
	                  hItem.put("TCTBao", "Điều chỉnh");
	               }

	               if ("3".equals(tctb)) {
	                  hItem.put("TCTBao", "Thay thế");
	               }

	               if ("4".equals(tctb)) {
	                  hItem.put("TCTBao", "Giải trình");
	               }

	               hItem.put("LDo", this.commons.getTextFromNodeXML((Element)xPath.evaluate("LDo", nodeTmp, XPathConstants.NODE)));
	               arrayData.add(hItem);
	               ++countPrd;
	               if (startRowGroup >= numberRowInPage) {
	                  startRowGroup = 0;
	                  ++groupPageIDX;
	               }
	            }

	            if (0 == startRowGroup && nodeListHHDVu.getLength() > 0) {
	               --groupPageIDX;
	            }

	            if (startRowGroup > 0 || nodeListHHDVu.getLength() == 0) {
	               for(i = startRowGroup; i < numberRowInPage; ++i) {
	                  hItem = new HashMap();
	                  hItem.put("ProdName", " ");
	                  hItem.put("GroupPageIDX", groupPageIDX);
	                  arrayData.add(hItem);
	               }
	            }
	         }
	      }

	      reportParams.put("TOTAL_PAGE", groupPageIDX);
	      JRDataSource jds = null;
	      jds = new JRBeanCollectionDataSource(arrayData);
	      ByteArrayOutputStream out = new ByteArrayOutputStream();
	      JasperReport jr = JasperCompileManager.compileReport(new FileInputStream(fileJP));
	      JasperPrint jp = JasperFillManager.fillReport(jr, reportParams, jds);
	      thang = null;
	      Exporter exporter = new JRPdfExporter();
	      exporter.setExporterInput(new SimpleExporterInput(jp));
	      exporter.setExporterOutput(new SimpleOutputStreamExporterOutput(out));
	      SimplePdfExporterConfiguration configuration = new SimplePdfExporterConfiguration();
	      configuration.setCreatingBatchModeBookmarks(true);
	      exporter.setConfiguration(configuration);
	      exporter.exportReport();
	      return out;
	   }


		public ByteArrayOutputStream createFinalInvoiceDL(File fileJP, Document doc, String secureKey, String CheckView
				, int numberRowInPage, int numberRowInPageMultiPage, int numberCharsInRow
				, String MST, String link, String pathLogo, String pathBackground,String pathQA, String pathVien
				, boolean isConvert, boolean isDeleted
				, boolean isThayThe, boolean isDieuChinh
			) throws Exception{
				Map<String, Object> reportParams = new HashMap<String, Object>();
				
				/*KIEM TRA FILE LOG & BACKGROUND CO TON TAI KHONG*/
				File f = new File(pathLogo);
				if(f.exists() && f.isFile())
					reportParams.put("URL_IMG_LOGO", pathLogo);
				else
					reportParams.put("URL_IMG_LOGO", Paths.get(SystemParams.DIR_E_INVOICE_TEMPLATE, IMAGE_FILENAME_BLANK).toString());
				reportParams.put("IsConvert", isConvert);
				f = new File(pathBackground);
				if(f.exists() && f.isFile())
					reportParams.put("URL_IMG_BACKGROUND", pathBackground);
				else
					reportParams.put("URL_IMG_BACKGROUND", Paths.get(SystemParams.DIR_E_INVOICE_TEMPLATE, IMAGE_FILENAME_BLANK).toString());
				f = new File(pathQA);
				if(f.exists() && f.isFile())
					reportParams.put("URL_QR_INFO", pathQA);
				else
					reportParams.put("URL_QR_INFO", Paths.get(SystemParams.DIR_E_INVOICE_TEMPLATE, IMAGE_FILENAME_BLANK).toString());
			
				f = new File(pathVien);
				if(f.exists() && f.isFile())
					reportParams.put("URL_IMG_FRAME", pathVien);
				else
					reportParams.put("URL_IMG_FRAME", Paths.get(SystemParams.DIR_E_INVOICE_TEMPLATE, IMAGE_FILENAME_BLANK).toString());
			
				
				String NLap = "";
				String KDLieu = "";
				
				Node nodeTmp = null;
				Node nodeSubTmp = null;
				NodeList nodeListTTKhac = null;
				NodeList nodeListNBanTTKhac = null;
				int countPrd = 0;
				
				XPath xPath = XPathFactory.newInstance().newXPath();
//				Node nodeHDon = (Node) xPath.evaluate("/KetQuaTraCuu/DuLieu/TDiep[last()]/DLieu/HDon", doc, XPathConstants.NODE);
				Node nodeHDon = null;
				for(int i = 1; i<= 20; i++) {
					nodeHDon = (Node) xPath.evaluate("/KetQuaTraCuu/DuLieu/TDiep[" + i + "]/DLieu/HDon", doc, XPathConstants.NODE);
					if(nodeHDon != null) break;
				}
				/*FILE CHUA DUOC DUI DEN CO QUAN THUE*/
				if(null == nodeHDon) {
					nodeHDon = (Node) xPath.evaluate("/HDon", doc, XPathConstants.NODE);
				}
				
				Node nodeTTChung = (Node) xPath.evaluate("DLHDon/TTChung", nodeHDon, XPathConstants.NODE);
				Node nodeNBan = (Node) xPath.evaluate("DLHDon/NDHDon/NBan", nodeHDon, XPathConstants.NODE);
				Node nodeNMua = (Node) xPath.evaluate("DLHDon/NDHDon/NMua", nodeHDon, XPathConstants.NODE);
				Node nodeTToan = (Node) xPath.evaluate("DLHDon/NDHDon/TToan", nodeHDon, XPathConstants.NODE);
				NodeList nodeListHHDVu = (NodeList) xPath.evaluate("DLHDon/NDHDon/DSHHDVu/HHDVu", nodeHDon, XPathConstants.NODESET);
				
				LocalDate localDateNLap = null;
				NLap = commons.getTextFromNodeXML((Element) xPath.evaluate("NLap", nodeTTChung, XPathConstants.NODE));
				if(!"".equals(NLap)) {
					try {
						localDateNLap = commons.convertStringToLocalDate(NLap, "yyyy-MM-dd");
					}catch(Exception e) {}
				}
				
				if(null != localDateNLap) {
					reportParams.put("InvoiceDate", StringUtils.leftPad(String.valueOf(localDateNLap.get(ChronoField.DAY_OF_MONTH)), 2, "0"));
					reportParams.put("InvoiceMonth", StringUtils.leftPad(String.valueOf(localDateNLap.get(ChronoField.MONTH_OF_YEAR)), 2, "0"));
					reportParams.put("InvoiceYear", StringUtils.leftPad(String.valueOf(localDateNLap.get(ChronoField.YEAR)), 4, "0"));
				}
				LocalDate localDateNLap1 = null;
		        String NLap2= commons.getTextFromNodeXML((Element) xPath.evaluate("HDKTNgay", nodeNBan, XPathConstants.NODE));
		        if(!"".equals(NLap2)) {
		          try {
		            localDateNLap1 = commons.convertStringToLocalDate(NLap2, "yyyy-MM-dd");
		          }catch(Exception e) {}
		        }
		        
		        if(null != localDateNLap1) {
		          reportParams.put("HDongKinhTeNgayDate", StringUtils.leftPad(String.valueOf(localDateNLap1.get(ChronoField.DAY_OF_MONTH)), 2, "0"));
		          reportParams.put("HDongKinhTeNgayMonth", StringUtils.leftPad(String.valueOf(localDateNLap1.get(ChronoField.MONTH_OF_YEAR)), 2, "0"));
		          reportParams.put("HDongKinhTeNgayYear", StringUtils.leftPad(String.valueOf(localDateNLap1.get(ChronoField.YEAR)), 4, "0"));
		        }
				reportParams.put("KHHDon", 
					commons.getTextFromNodeXML((Element) xPath.evaluate("KHMSHDon", nodeTTChung, XPathConstants.NODE))
					+ commons.getTextFromNodeXML((Element) xPath.evaluate("KHHDon", nodeTTChung, XPathConstants.NODE))
				);
				
				LocalDate localDateCD= null;
				localDateCD = commons.convertStringToLocalDate(NLap, "yyyy-MM-dd");
					reportParams.put("InvoiceCovertDate", StringUtils.leftPad(String.valueOf(localDateCD.get(ChronoField.DAY_OF_MONTH)), 2, "0"));
					reportParams.put("InvoiceCovertMonth", StringUtils.leftPad(String.valueOf(localDateCD.get(ChronoField.MONTH_OF_YEAR)), 2, "0"));
					reportParams.put("InvoiceCovertYear", StringUtils.leftPad(String.valueOf(localDateCD.get(ChronoField.YEAR)), 4, "0"));
				
				 String Checknamecd = "";
					try {
								String[] words = CheckView.split(",");
								  CheckView = words[0];
								  Checknamecd =  words[1];
							
							} catch (Exception e) {
								// TODO: handle exception
							}
					
							if(!Checknamecd.equals("")) {
								reportParams.put("SignNameCovert",Checknamecd);			
							}
				
				
//				reportParams.put("SHDon",
//					commons.addSpacingAfterLeter(
//						commons.formatNumberBillInvoice(commons.getTextFromNodeXML((Element) xPath.evaluate("SHDon", nodeTTChung, XPathConstants.NODE)))
//					)
//				);
//				reportParams.put("SHDonOrigin", commons.getTextFromNodeXML((Element) xPath.evaluate("SHDon", nodeTTChung, XPathConstants.NODE)));
//				
				if(CheckView.equals("Y"))
				{
							reportParams.put("SHDon", commons.getTextFromNodeXML((Element) xPath.evaluate("SHDon", nodeTTChung, XPathConstants.NODE)));
							reportParams.put("SHDonOrigin", commons.getTextFromNodeXML((Element) xPath.evaluate("SHDon", nodeTTChung, XPathConstants.NODE)));
							
				}
				else {
					reportParams.put("SHDon",
							commons.addSpacingAfterLeter(
								commons.formatNumberBillInvoice(commons.getTextFromNodeXML((Element) xPath.evaluate("SHDon", nodeTTChung, XPathConstants.NODE)))
							)
						);
					reportParams.put("SHDonOrigin",
								commons.addSpacingAfterLeter(
									commons.formatNumberBillInvoice(commons.getTextFromNodeXML((Element) xPath.evaluate("SHDon", nodeTTChung, XPathConstants.NODE)))
								)
							);
				}	
				reportParams.put("MCCQT", 
					commons.getTextFromNodeXML((Element) xPath.evaluate("MCCQT", nodeHDon, XPathConstants.NODE))
				);
				//TTChung/TTKhac
				nodeListTTKhac = (NodeList) xPath.evaluate("TTKhac/TTin", nodeTTChung, XPathConstants.NODESET);
				if(nodeListTTKhac != null) {
					for(int j = 0; j < nodeListTTKhac.getLength(); j++) {
						nodeSubTmp = nodeListTTKhac.item(j);
						
						KDLieu = commons.getTextFromNodeXML((Element) xPath.evaluate("KDLieu", nodeSubTmp, XPathConstants.NODE));
						switch (KDLieu.toUpperCase()) {
						case "DECIMAL":
							reportParams.put(
								commons.getTextFromNodeXML((Element) xPath.evaluate("TTruong", nodeSubTmp, XPathConstants.NODE)),
								commons.ToNumber(commons.getTextFromNodeXML((Element) xPath.evaluate("DLieu", nodeSubTmp, XPathConstants.NODE)))
							);
							break;
						default:
							reportParams.put(
								commons.getTextFromNodeXML((Element) xPath.evaluate("TTruong", nodeSubTmp, XPathConstants.NODE)),
								commons.getTextFromNodeXML((Element) xPath.evaluate("DLieu", nodeSubTmp, XPathConstants.NODE))
							);
							break;
						}
					}
				}
				
				//CHECK SECUREKEY									
				String checkSecureKey = (String) reportParams.get("SecureKey");
				
				if(checkSecureKey == null || checkSecureKey.equals("")) {
					reportParams.put("SecureKey", secureKey);
				}			
				//END CHECK SECUREKEY
				NodeList nodeListNBan = null;	
				String DLieu = "";
				String TTruong = "";
				String fax = "";
				String dc = "";
				String ten = "";
				String stk = "";
				String tnh= "";
				String phone = "";
				String email="";
				String PostDescription = "";
				nodeListNBan = (NodeList) xPath.evaluate("DLHDon/NDHDon/NBan/TTKhac/TTin", nodeHDon, XPathConstants.NODESET);
				if(nodeListNBan != null) {
					for(int j = 0; j < nodeListNBan.getLength(); j++) {
						nodeSubTmp = nodeListNBan.item(j);
						TTruong = commons.getTextFromNodeXML((Element) xPath.evaluate("TTruong", nodeSubTmp, XPathConstants.NODE));
						
						if("ComFax".equals(TTruong)) {
							fax =commons.getTextFromNodeXML((Element) xPath.evaluate("DLieu", nodeSubTmp, XPathConstants.NODE));
						}
						if("ComAddress".equals(TTruong)) {
							dc =commons.getTextFromNodeXML((Element) xPath.evaluate("DLieu", nodeSubTmp, XPathConstants.NODE));
						}
						if("ComPhone".equals(TTruong)) {
							phone =commons.getTextFromNodeXML((Element) xPath.evaluate("DLieu", nodeSubTmp, XPathConstants.NODE));
						}
						if("ComEmail".equals(TTruong)) {
							email =commons.getTextFromNodeXML((Element) xPath.evaluate("DLieu", nodeSubTmp, XPathConstants.NODE));
						}
						if("STKNHang1".equals(TTruong)) {
							stk =commons.getTextFromNodeXML((Element) xPath.evaluate("DLieu", nodeSubTmp, XPathConstants.NODE));
						}
						if("TNHang1".equals(TTruong)) {
							tnh =commons.getTextFromNodeXML((Element) xPath.evaluate("DLieu", nodeSubTmp, XPathConstants.NODE));
						}
						if("PostBy".equals(TTruong)) {
							ten =commons.getTextFromNodeXML((Element) xPath.evaluate("DLieu", nodeSubTmp, XPathConstants.NODE));
						}
						if("PostDescription".equals(TTruong)) {
							PostDescription =commons.getTextFromNodeXML((Element) xPath.evaluate("DLieu", nodeSubTmp, XPathConstants.NODE));
						}
						DLieu = commons.getTextFromNodeXML((Element) xPath.evaluate("DLieu", nodeSubTmp, XPathConstants.NODE));
						switch (KDLieu.toUpperCase()) {
						case "DECIMAL":
							reportParams.put(
								commons.getTextFromNodeXML((Element) xPath.evaluate("TTruong", nodeSubTmp, XPathConstants.NODE)),
								commons.ToNumber(commons.getTextFromNodeXML((Element) xPath.evaluate("DLieu", nodeSubTmp, XPathConstants.NODE)))
							);
							break;
						default:
							reportParams.put(
								commons.getTextFromNodeXML((Element) xPath.evaluate("TTruong", nodeSubTmp, XPathConstants.NODE)),
								commons.getTextFromNodeXML((Element) xPath.evaluate("DLieu", nodeSubTmp, XPathConstants.NODE))
							
							);
							break;
						}
					}
				}
				String abc = DLieu;

				
				//NGUOI MUA
				reportParams.put("TNCNTen", commons.getTextFromNodeXML((Element) xPath.evaluate("Ten", nodeNMua, XPathConstants.NODE)));
				reportParams.put("TCCNMST", commons.getTextFromNodeXML((Element) xPath.evaluate("MST", nodeNMua, XPathConstants.NODE)));
				
				//NGUOI BAN
				reportParams.put("SignName", commons.getTextFromNodeXML((Element) xPath.evaluate("Ten", nodeNBan, XPathConstants.NODE)));
				reportParams.put("NRaLenhTen",ten);

						reportParams.put("LDieuDongSo", 
						commons.getTextFromNodeXML((Element) xPath.evaluate("HDKTSo", nodeNBan, XPathConstants.NODE))
					);
				
						
						
				reportParams.put("NBanMST", 
					commons.getTextFromNodeXML((Element) xPath.evaluate("MST", nodeNBan, XPathConstants.NODE))
				);
				reportParams.put("NBanTen", 
						commons.getTextFromNodeXML((Element) xPath.evaluate("Ten", nodeNBan, XPathConstants.NODE))
					);
				
				reportParams.put("HopDongSo", 
						commons.getTextFromNodeXML((Element) xPath.evaluate("HDSo", nodeNBan, XPathConstants.NODE))
					);
						
				reportParams.put("NBanDChi",commons.getTextFromNodeXML((Element) xPath.evaluate("DChi", nodeNBan, XPathConstants.NODE))
						);
				reportParams.put("NVanChuyenTen", 
						commons.getTextFromNodeXML((Element) xPath.evaluate("TNVChuyen", nodeNBan, XPathConstants.NODE))
					);
				reportParams.put("PTienVanChuyen", 
						commons.getTextFromNodeXML((Element) xPath.evaluate("PTVChuyen", nodeNBan, XPathConstants.NODE))
					);
						
				reportParams.put("NBanSDThoai",phone);
						
				reportParams.put("NBanFax",fax);
				reportParams.put("NBanDCTDTu", email);
				
				reportParams.put("NBanSTKNHang",stk);
				reportParams.put("NBanTNHang", tnh);
//				reportParams.put("NBanSTKNHang", 
//					commons.getTextFromNodeXML((Element) xPath.evaluate("STKNHang", nodeNBan, XPathConstants.NODE))
//				);
//				reportParams.put("NBanTNHang", 
//					commons.getTextFromNodeXML((Element) xPath.evaluate("TNHang", nodeNBan, XPathConstants.NODE))
//				);
				
				reportParams.put("NBanWebsite", 
					commons.getTextFromNodeXML((Element) xPath.evaluate("Website", nodeNBan, XPathConstants.NODE))
				);
				
				/*THONG TIN KHAC (NEU CO)*/
				nodeListTTKhac = (NodeList) xPath.evaluate("TTKhac/TTin", nodeNBan, XPathConstants.NODESET);
				if(nodeListTTKhac != null) {
					for(int j = 0; j < nodeListTTKhac.getLength(); j++) {
						nodeSubTmp = nodeListTTKhac.item(j);
						
						KDLieu = commons.getTextFromNodeXML((Element) xPath.evaluate("KDLieu", nodeSubTmp, XPathConstants.NODE));
						switch (KDLieu.toUpperCase()) {
						case "DECIMAL":
							reportParams.put(
								"NBan" + commons.getTextFromNodeXML((Element) xPath.evaluate("TTruong", nodeSubTmp, XPathConstants.NODE)),
								commons.ToNumber(commons.getTextFromNodeXML((Element) xPath.evaluate("DLieu", nodeSubTmp, XPathConstants.NODE)))
							);
							break;
						default:
							reportParams.put(
								"NBan" + commons.getTextFromNodeXML((Element) xPath.evaluate("TTruong", nodeSubTmp, XPathConstants.NODE)),
								commons.getTextFromNodeXML((Element) xPath.evaluate("DLieu", nodeSubTmp, XPathConstants.NODE))
							);
							break;
						}
					}
				}
				
				reportParams.put("NMuaTen", 
					commons.getTextFromNodeXML((Element) xPath.evaluate("Ten", nodeNMua, XPathConstants.NODE))
				);
				
				reportParams.put("NhapTaiKho", 
						commons.getTextFromNodeXML((Element) xPath.evaluate("DChi", nodeNMua, XPathConstants.NODE))
					);
				reportParams.put("XuatTaiKho",dc);
				reportParams.put("NMuaMST", 
					commons.getTextFromNodeXML((Element) xPath.evaluate("MST", nodeNMua, XPathConstants.NODE))
				);
				reportParams.put("NMuaDChi", 
					commons.getTextFromNodeXML((Element) xPath.evaluate("DChi", nodeNMua, XPathConstants.NODE))
				);
				reportParams.put("NDungCV",abc);
				reportParams.put("NMuaMKHang", 
					commons.getTextFromNodeXML((Element) xPath.evaluate("MKHang", nodeNMua, XPathConstants.NODE))
				);
				reportParams.put("NMuaSDThoai", 
					commons.getTextFromNodeXML((Element) xPath.evaluate("SDThoai", nodeNMua, XPathConstants.NODE))
				);
				reportParams.put("NMuaDCTDTu", 
					commons.getTextFromNodeXML((Element) xPath.evaluate("DCTDTu", nodeNMua, XPathConstants.NODE))
				);
				reportParams.put("NMuaHVTNMHang", 
					commons.getTextFromNodeXML((Element) xPath.evaluate("HVTNMHang", nodeNMua, XPathConstants.NODE))
				);
				reportParams.put("NMuaSTKNHang", 
					commons.getTextFromNodeXML((Element) xPath.evaluate("STKNHang", nodeNMua, XPathConstants.NODE))
				);
				reportParams.put("NMuaTNHang", 
					commons.getTextFromNodeXML((Element) xPath.evaluate("TNHang", nodeNMua, XPathConstants.NODE))
				);
				
				reportParams.put("TTChungHTTToan", commons.getTextFromNodeXML((Element) xPath.evaluate("HTTToan", nodeTTChung, XPathConstants.NODE)));
				
				String TCHDon = commons.getTextFromNodeXML((Element) xPath.evaluate("TTHDLQuan/TCHDon", nodeTTChung, XPathConstants.NODE));
				String noticeTTDC = "";
				switch (TCHDon) {
				case "1":
					noticeTTDC = String.format("(Thay thế cho hóa đơn Mẫu số %s, Ký hiệu %s, số %s, ngày %s)", 
							commons.getTextFromNodeXML((Element) xPath.evaluate("TTHDLQuan/KHMSHDCLQuan", nodeTTChung, XPathConstants.NODE)),
							commons.getTextFromNodeXML((Element) xPath.evaluate("TTHDLQuan/KHHDCLQuan", nodeTTChung, XPathConstants.NODE)),
							commons.getTextFromNodeXML((Element) xPath.evaluate("TTHDLQuan/SHDCLQuan", nodeTTChung, XPathConstants.NODE)),
							commons.convertLocalDateTimeToString(commons.convertStringToLocalDate(commons.getTextFromNodeXML((Element) xPath.evaluate("TTHDLQuan/NLHDCLQuan", nodeTTChung, XPathConstants.NODE)), "yyyy-MM-dd"), Constants.FORMAT_DATE.FORMAT_DATE_WEB)
						);
					break;
				case "2":
					noticeTTDC = String.format("(Điều chỉnh cho hóa đơn Mẫu số %s, Ký hiệu %s, số %s, ngày %s)", 
							commons.getTextFromNodeXML((Element) xPath.evaluate("TTHDLQuan/KHMSHDCLQuan", nodeTTChung, XPathConstants.NODE)),
							commons.getTextFromNodeXML((Element) xPath.evaluate("TTHDLQuan/KHHDCLQuan", nodeTTChung, XPathConstants.NODE)),
							commons.getTextFromNodeXML((Element) xPath.evaluate("TTHDLQuan/SHDCLQuan", nodeTTChung, XPathConstants.NODE)),
							commons.convertLocalDateTimeToString(commons.convertStringToLocalDate(commons.getTextFromNodeXML((Element) xPath.evaluate("TTHDLQuan/NLHDCLQuan", nodeTTChung, XPathConstants.NODE)), "yyyy-MM-dd"), Constants.FORMAT_DATE.FORMAT_DATE_WEB)
						);
					break;
				default:
					break;
				}
				reportParams.put("NoticeTTDC", noticeTTDC);
				
				reportParams.put("TTChungDVTTe", commons.getTextFromNodeXML((Element) xPath.evaluate("DVTTe", nodeTTChung, XPathConstants.NODE)));
				reportParams.put("TToanTgTCThue", 
					commons.ToNumber(commons.getTextFromNodeXML((Element) xPath.evaluate("TgTCThue", nodeTToan, XPathConstants.NODE)))
				);
				reportParams.put("TToanTgTThue", 
					commons.ToNumber(commons.getTextFromNodeXML((Element) xPath.evaluate("TgTThue", nodeTToan, XPathConstants.NODE)))
				);
				reportParams.put("TToanTgTTTBSo", 
					commons.ToNumber(commons.getTextFromNodeXML((Element) xPath.evaluate("TgTTTBSo", nodeTToan, XPathConstants.NODE)))
				);
				reportParams.put("TToanTgTTTBChu", 
					commons.getTextFromNodeXML((Element) xPath.evaluate("TgTTTBChu", nodeTToan, XPathConstants.NODE))
				);
				
				/*LAY DANH SACH LAI SUAT*/
				String tsuatTmp = "";
				NodeList nodeListLTSuat = null;
				try {
					nodeListLTSuat = (NodeList) xPath.evaluate("THTTLTSuat/LTSuat", nodeTToan, XPathConstants.NODESET);
					if(nodeListLTSuat != null && nodeListLTSuat.getLength() > 0) {
						for(int i = 0; i < nodeListLTSuat.getLength(); i++) {
							nodeTmp = nodeListLTSuat.item(i);
							
							tsuatTmp = commons.getTextFromNodeXML((Element) xPath.evaluate("TSuat", nodeTmp, XPathConstants.NODE)).replaceAll("%", "");
							reportParams.put("Tax_TSuat_" + tsuatTmp, tsuatTmp);
							reportParams.put("Tax_ThTien_" + tsuatTmp, 
								commons.ToNumber(commons.getTextFromNodeXML((Element) xPath.evaluate("ThTien", nodeTmp, XPathConstants.NODE)))
							);
							reportParams.put("Tax_TThue_" + tsuatTmp, 
								commons.ToNumber(commons.getTextFromNodeXML((Element) xPath.evaluate("TThue", nodeTmp, XPathConstants.NODE)))
							);
							
							if(reportParams.get("Tax_TSuat_Default") == null) {
								reportParams.put("Tax_TSuat_Default", tsuatTmp);
								reportParams.put("Tax_ThTien_Default", 
									commons.ToNumber(commons.getTextFromNodeXML((Element) xPath.evaluate("ThTien", nodeTmp, XPathConstants.NODE)))
								);
								reportParams.put("Tax_TThue_Default", 
									commons.ToNumber(commons.getTextFromNodeXML((Element) xPath.evaluate("TThue", nodeTmp, XPathConstants.NODE)))
								);
							}
						}
					}
				}catch(Exception e) {}
				/*END - LAY DANH SACH LAI SUAT*/
				
				/*KIEM TRA THONG TIN NGUOI BAN KY CHUA*/
				Node nodeDSCKS = (Node) xPath.evaluate("DSCKS", nodeHDon, XPathConstants.NODE);
				Node nodeSignature = null;
				if(null != nodeDSCKS)
					nodeSignature = (Node) xPath.evaluate("NBan/Signature", nodeDSCKS, XPathConstants.NODE);
				LocalDateTime ldt = null;
				if(null == nodeSignature) {
					reportParams.put("IsSigned", false);
					reportParams.put("SignDesc", "Chưa ký");
					reportParams.put("UrlImageVerify", Paths.get(SystemParams.DIR_E_INVOICE_TEMPLATE, Constants.TEMPLATE_FILE_NAME.IMG_SIGNATURE_INVALID).toString());
				}else {
					String x509Certificate = commons.getTextFromNodeXML((Element) xPath.evaluate("KeyInfo/X509Data/X509Certificate", nodeSignature, XPathConstants.NODE));
					String signingTime = commons.getTextFromNodeXML((Element) xPath.evaluate("Object[@Id='SigningTime']/SignatureProperties/SignatureProperty/SigningTime", nodeSignature, XPathConstants.NODE));
					
					reportParams.put("IsSigned", true);
					reportParams.put("SignDesc", "Đã ký");
					SignTypeInfo signTypeInfo = commons.parserCert(x509Certificate);
					reportParams.put("SignName", null == signTypeInfo? "": signTypeInfo.getName());
					try {
						ldt = commons.convertStringToLocalDateTime(signingTime, "yyyy-MM-dd'T'HH:mm:ss'Z'");
						reportParams.put("SignDate", commons.convertLocalDateTimeToString(ldt, Constants.FORMAT_DATE.FORMAT_DATE_WEB));
					}catch(Exception e) {}
					reportParams.put("UrlImageVerify", Paths.get(SystemParams.DIR_E_INVOICE_TEMPLATE, Constants.TEMPLATE_FILE_NAME.IMG_SIGNATURE_VALID).toString());
				}
				reportParams.put("UrlImageInvDeleted", Paths.get(SystemParams.DIR_E_INVOICE_TEMPLATE, Constants.TEMPLATE_FILE_NAME.IMG_INV_DELETED).toString());
				reportParams.put("IsDeleted", isDeleted);
				reportParams.put("UrlImageInvThayThe", Paths.get(SystemParams.DIR_E_INVOICE_TEMPLATE, Constants.TEMPLATE_FILE_NAME.IMG_INV_THAYTHE).toString());
				reportParams.put("IsThayThe", isThayThe);
				
				List<HashMap<String, Object>> arrayData = new ArrayList<>();
				HashMap<String, Object> hItem = null;
				
				int lenProName = 0;
				String productName = "";
				int startRowGroup = 0;
				
				/*XEM CO IN NHIEU DONG KHONG*/
				int groupPageIDX = 1;
				boolean isUsingMultiPage = false;
				if(nodeListHHDVu != null) {
					for(int i = 0; i < nodeListHHDVu.getLength(); i++) {
						nodeTmp = nodeListHHDVu.item(i);
						
						productName = commons.getTextFromNodeXML((Element) xPath.evaluate("THHDVu", nodeTmp, XPathConstants.NODE));
						lenProName = productName.getBytes(StandardCharsets.UTF_8).length;
						startRowGroup += Math.ceil((double) lenProName / numberCharsInRow);
						
						if(startRowGroup >= numberRowInPage) {
							isUsingMultiPage = true;
							break;
						}
					}	
				}
				/*END - XEM CO IN NHIEU DONG KHONG*/
				
				startRowGroup = 0;
				KDLieu = "";
				nodeTmp = null;
				nodeSubTmp = null;
				nodeListTTKhac = null;
				countPrd = 0;
				if(nodeListHHDVu != null) {
					if(isUsingMultiPage) {
						for(int i = 0; i < nodeListHHDVu.getLength(); i++) {
							nodeTmp = nodeListHHDVu.item(i);
							
							productName = commons.getTextFromNodeXML((Element) xPath.evaluate("THHDVu", nodeTmp, XPathConstants.NODE));
							lenProName = productName.getBytes(StandardCharsets.UTF_8).length;
							startRowGroup += Math.ceil((double) lenProName / numberCharsInRow);
//							countRowForPrd = (int) Math.ceil(lenProName / numberCharsInRow);
//							if(countRowForPrd > 1)
//								i = i + countRowForPrd;
							
							hItem = new HashMap<>();
							hItem.put("GroupPageIDX", groupPageIDX);
							
							hItem.put("TChat", commons.getTextFromNodeXML((Element) xPath.evaluate("TChat", nodeTmp, XPathConstants.NODE)));
							hItem.put("STT", commons.getTextFromNodeXML((Element) xPath.evaluate("STT", nodeTmp, XPathConstants.NODE)));
							hItem.put("MSo", commons.getTextFromNodeXML((Element) xPath.evaluate("MHHDVu", nodeTmp, XPathConstants.NODE)));
							
							hItem.put("THHDVu", productName);
							
							hItem.put("DVTinh", commons.getTextFromNodeXML((Element) xPath.evaluate("DVTinh", nodeTmp, XPathConstants.NODE)));
							hItem.put("SLuong",
									commons.ToNumber(commons.getTextFromNodeXML((Element) xPath.evaluate("SLuong", nodeTmp, XPathConstants.NODE)))
								);
							hItem.put("DGia",
								commons.ToNumber(commons.getTextFromNodeXML((Element) xPath.evaluate("DGia", nodeTmp, XPathConstants.NODE)))
							);
							hItem.put("ThTien",
								commons.ToNumber(commons.getTextFromNodeXML((Element) xPath.evaluate("ThTien", nodeTmp, XPathConstants.NODE)))
							);
							hItem.put("TSuat",
								commons.getTextFromNodeXML((Element) xPath.evaluate("TSuat", nodeTmp, XPathConstants.NODE))
							);
							
							/*LAY NOI DUNG TTKHAC*/
							nodeListTTKhac = (NodeList) xPath.evaluate("TTKhac/TTin", nodeTmp, XPathConstants.NODESET);
							if(nodeListTTKhac != null) {
								for(int j = 0; j < nodeListTTKhac.getLength(); j++) {
									nodeSubTmp = nodeListTTKhac.item(j);
									
									KDLieu = commons.getTextFromNodeXML((Element) xPath.evaluate("KDLieu", nodeSubTmp, XPathConstants.NODE));
									switch (KDLieu.toUpperCase()) {
									case "DECIMAL":
										hItem.put(
											commons.getTextFromNodeXML((Element) xPath.evaluate("TTruong", nodeSubTmp, XPathConstants.NODE)),
											commons.ToNumber(commons.getTextFromNodeXML((Element) xPath.evaluate("DLieu", nodeSubTmp, XPathConstants.NODE)))
										);
										break;
									default:
										hItem.put(
											commons.getTextFromNodeXML((Element) xPath.evaluate("TTruong", nodeSubTmp, XPathConstants.NODE)),
											commons.getTextFromNodeXML((Element) xPath.evaluate("DLieu", nodeSubTmp, XPathConstants.NODE))
										);
										break;
									}
								}
							}
							
							arrayData.add(hItem);
							countPrd++;	
							
							if(startRowGroup >= numberRowInPageMultiPage) {
								startRowGroup = 0;
								groupPageIDX++;
							}
						}
						if(0 == startRowGroup && nodeListHHDVu.getLength() > 0) groupPageIDX--;		//BO BOT TRANG CUOI CUNG VI KHONG CO DU LIEU
						
						if(0 == startRowGroup) {
							groupPageIDX++;
							for(int i = 0; i < numberRowInPage; i++) {
								hItem = new HashMap<>();
								hItem.put("ProdName", " ");
								hItem.put("GroupPageIDX", groupPageIDX);
								arrayData.add(hItem);
							}
						}else if(startRowGroup <= numberRowInPage) {
							for(int i = startRowGroup; i < numberRowInPage; i++) {
								hItem = new HashMap<>();
								hItem.put("ProdName", " ");
								hItem.put("GroupPageIDX", groupPageIDX);
								arrayData.add(hItem);
							}
						}else {
							for(int i = startRowGroup; i < numberRowInPageMultiPage; i++) {
								hItem = new HashMap<>();
								hItem.put("ProdName", " ");
								hItem.put("GroupPageIDX", groupPageIDX);
								arrayData.add(hItem);
							}
							groupPageIDX++;
							for(int i = 0; i < numberRowInPage; i++) {
								hItem = new HashMap<>();
								hItem.put("ProdName", " ");
								hItem.put("GroupPageIDX", groupPageIDX);
								arrayData.add(hItem);
							}
						}
					}else {
						for(int i = 0; i < nodeListHHDVu.getLength(); i++) {
							nodeTmp = nodeListHHDVu.item(i);
							
							productName = commons.getTextFromNodeXML((Element) xPath.evaluate("THHDVu", nodeTmp, XPathConstants.NODE));
							lenProName = productName.getBytes(StandardCharsets.UTF_8).length;
							startRowGroup += Math.ceil((double) lenProName / numberCharsInRow);
//							countRowForPrd = (int) Math.ceil(lenProName / numberCharsInRow);
//							if(countRowForPrd > 1)
//								i = i + countRowForPrd;
							
							hItem = new HashMap<>();
							hItem.put("GroupPageIDX", groupPageIDX);
							
							hItem.put("TChat", commons.getTextFromNodeXML((Element) xPath.evaluate("TChat", nodeTmp, XPathConstants.NODE)));
							hItem.put("STT", commons.getTextFromNodeXML((Element) xPath.evaluate("STT", nodeTmp, XPathConstants.NODE)));
							hItem.put("MSo", commons.getTextFromNodeXML((Element) xPath.evaluate("MHHDVu", nodeTmp, XPathConstants.NODE)));
							
							hItem.put("THHDVu", productName);
							
							hItem.put("DVTinh", commons.getTextFromNodeXML((Element) xPath.evaluate("DVTinh", nodeTmp, XPathConstants.NODE)));
							hItem.put("SLuong",
								commons.ToNumber(commons.getTextFromNodeXML((Element) xPath.evaluate("SLuong", nodeTmp, XPathConstants.NODE)))
							);
							hItem.put("DGia",
								commons.ToNumber(commons.getTextFromNodeXML((Element) xPath.evaluate("DGia", nodeTmp, XPathConstants.NODE)))
							);
							hItem.put("ThTien",
								commons.ToNumber(commons.getTextFromNodeXML((Element) xPath.evaluate("ThTien", nodeTmp, XPathConstants.NODE)))
							);
							hItem.put("TSuat",
								commons.getTextFromNodeXML((Element) xPath.evaluate("TSuat", nodeTmp, XPathConstants.NODE))
							);
							
							/*LAY NOI DUNG TTKHAC*/
							nodeListTTKhac = (NodeList) xPath.evaluate("TTKhac/TTin", nodeTmp, XPathConstants.NODESET);
							if(nodeListTTKhac != null) {
								for(int j = 0; j < nodeListTTKhac.getLength(); j++) {
									nodeSubTmp = nodeListTTKhac.item(j);
									
									KDLieu = commons.getTextFromNodeXML((Element) xPath.evaluate("KDLieu", nodeSubTmp, XPathConstants.NODE));
									switch (KDLieu.toUpperCase()) {
									case "DECIMAL":
										hItem.put(
											commons.getTextFromNodeXML((Element) xPath.evaluate("TTruong", nodeSubTmp, XPathConstants.NODE)),
											commons.ToNumber(commons.getTextFromNodeXML((Element) xPath.evaluate("DLieu", nodeSubTmp, XPathConstants.NODE)))
										);
										break;
									default:
										hItem.put(
											commons.getTextFromNodeXML((Element) xPath.evaluate("TTruong", nodeSubTmp, XPathConstants.NODE)),
											commons.getTextFromNodeXML((Element) xPath.evaluate("DLieu", nodeSubTmp, XPathConstants.NODE))
										);
										break;
									}
								}
							}
							
							arrayData.add(hItem);
							countPrd++;	
							
							if(startRowGroup >= numberRowInPage) {
								startRowGroup = 0;
								groupPageIDX++;
							}
						}
						if(0 == startRowGroup && nodeListHHDVu.getLength() > 0) groupPageIDX--;		//BO BOT TRANG CUOI CUNG VI KHONG CO DU LIEU
						
						/*ADD THEM DU LIEU CHO DU TRANG*/
						if(startRowGroup > 0 || nodeListHHDVu.getLength() == 0) {
							for(int i = startRowGroup; i < numberRowInPage; i++) {
								hItem = new HashMap<>();
								hItem.put("ProdName", " ");
								hItem.put("GroupPageIDX", groupPageIDX);
								arrayData.add(hItem);
							}
						}
					}
					
//					for(int i = 0; i < nodeListHHDVu.getLength(); i++) {
//						nodeTmp = nodeListHHDVu.item(i);
//						
//						productName = commons.getTextFromNodeXML((Element) xPath.evaluate("THHDVu", nodeTmp, XPathConstants.NODE));
//						lenProName = productName.getBytes(StandardCharsets.UTF_8).length;
//						startRowGroup += Math.ceil((double) lenProName / numberCharsInRow);
////						countRowForPrd = (int) Math.ceil(lenProName / numberCharsInRow);
////						if(countRowForPrd > 1)
////							i = i + countRowForPrd;
//						
//						hItem = new HashMap<>();
//						hItem.put("GroupPageIDX", 0);
//						
//						hItem.put("TChat", commons.getTextFromNodeXML((Element) xPath.evaluate("TChat", nodeTmp, XPathConstants.NODE)));
//						hItem.put("STT", commons.getTextFromNodeXML((Element) xPath.evaluate("STT", nodeTmp, XPathConstants.NODE)));
//						hItem.put("MHHDVu", commons.getTextFromNodeXML((Element) xPath.evaluate("MHHDVu", nodeTmp, XPathConstants.NODE)));
//						
//						hItem.put("THHDVu", productName);
//						
//						hItem.put("DVTinh", commons.getTextFromNodeXML((Element) xPath.evaluate("DVTinh", nodeTmp, XPathConstants.NODE)));
//						hItem.put("SLuong",
//							commons.ToNumber(commons.getTextFromNodeXML((Element) xPath.evaluate("SLuong", nodeTmp, XPathConstants.NODE)))
//						);
//						hItem.put("DGia",
//							commons.ToNumber(commons.getTextFromNodeXML((Element) xPath.evaluate("DGia", nodeTmp, XPathConstants.NODE)))
//						);
//						hItem.put("ThTien",
//							commons.ToNumber(commons.getTextFromNodeXML((Element) xPath.evaluate("ThTien", nodeTmp, XPathConstants.NODE)))
//						);
//						hItem.put("TSuat",
//							commons.getTextFromNodeXML((Element) xPath.evaluate("TSuat", nodeTmp, XPathConstants.NODE))
//						);
//						
//						/*LAY NOI DUNG TTKHAC*/
//						nodeListTTKhac = (NodeList) xPath.evaluate("TTKhac/TTin", nodeTmp, XPathConstants.NODESET);
//						if(nodeListTTKhac != null) {
//							for(int j = 0; j < nodeListTTKhac.getLength(); j++) {
//								nodeSubTmp = nodeListTTKhac.item(j);
//								
//								KDLieu = commons.getTextFromNodeXML((Element) xPath.evaluate("KDLieu", nodeSubTmp, XPathConstants.NODE));
//								switch (KDLieu.toUpperCase()) {
//								case "DECIMAL":
//									hItem.put(
//										commons.getTextFromNodeXML((Element) xPath.evaluate("TTruong", nodeSubTmp, XPathConstants.NODE)),
//										commons.ToNumber(commons.getTextFromNodeXML((Element) xPath.evaluate("DLieu", nodeSubTmp, XPathConstants.NODE)))
//									);
//									break;
//								default:
//									hItem.put(
//										commons.getTextFromNodeXML((Element) xPath.evaluate("TTruong", nodeSubTmp, XPathConstants.NODE)),
//										commons.getTextFromNodeXML((Element) xPath.evaluate("DLieu", nodeSubTmp, XPathConstants.NODE))
//									);
//									break;
//								}
//							}
//						}
//						
//						arrayData.add(hItem);
//						countPrd++;	
//					}
				}		
//				if(startRowGroup < numberRowInPage) {
//					for(int i = startRowGroup; i< numberRowInPage; i++) {
//						hItem = new HashMap<>();
//						hItem.put("GroupPageIDX", 0);
//						arrayData.add(hItem);
//					}
//					
//				}
				
				
				reportParams.put("PortalLink", 
						link
				);
				
				reportParams.put("TOTAL_PAGE", (int) groupPageIDX);
				
				JRDataSource jds = null;
				jds = new JRBeanCollectionDataSource(arrayData);
				
				ByteArrayOutputStream out = new ByteArrayOutputStream();
				JasperReport jr = JasperCompileManager.compileReport(new FileInputStream(fileJP));
				JasperPrint jp = JasperFillManager.fillReport(jr, reportParams, jds);
				Exporter exporter = null;
				
				exporter = new JRPdfExporter();
				exporter.setExporterInput(new SimpleExporterInput(jp));
				exporter.setExporterOutput(new SimpleOutputStreamExporterOutput(out));
				SimplePdfExporterConfiguration configuration = new SimplePdfExporterConfiguration();
				configuration.setCreatingBatchModeBookmarks(true);
				exporter.setConfiguration(configuration);
				exporter.exportReport();
				return out;
			}

	
	public ByteArrayOutputStream createFinalInvoiceBH(File fileJP, Document doc, String secureKey, String CheckView
		, int numberRowInPage, int numberRowInPageMultiPage, int numberCharsInRow
		, String MST,String link, String pathLogo, String pathBackground,String pathQA, String pathVien
		, boolean isConvert, boolean isDeleted
		, boolean isThayThe, boolean isDieuChinh
	) throws Exception{
		Map<String, Object> reportParams = new HashMap<String, Object>();
		
		/*KIEM TRA FILE LOG & BACKGROUND CO TON TAI KHONG*/
		File f = new File(pathLogo);
		if(f.exists() && f.isFile())
			reportParams.put("URL_IMG_LOGO", pathLogo);
		else
			reportParams.put("URL_IMG_LOGO", Paths.get(SystemParams.DIR_E_INVOICE_TEMPLATE, IMAGE_FILENAME_BLANK).toString());
		
		f = new File(pathBackground);
		if(f.exists() && f.isFile())
			reportParams.put("URL_IMG_BACKGROUND", pathBackground);
		else
			reportParams.put("URL_IMG_BACKGROUND", Paths.get(SystemParams.DIR_E_INVOICE_TEMPLATE, IMAGE_FILENAME_BLANK).toString());
		reportParams.put("IsConvert", isConvert);
//		if(isConvert) {
//			reportParams.put("IsConvert", isConvert);
//			reportParams.put("URL_IMG_CONVERT", Paths.get(SystemParams.DIR_E_INVOICE_TEMPLATE, IMAGE_FILENAME_INV_CONVERT).toString());
//		}

		String NLap = "";
		String KDLieu = "";
		Node nodeTmp = null;
		Node nodeSubTmp = null;
		NodeList nodeListTTKhac = null;
		int countPrd = 0;
		
		XPath xPath = XPathFactory.newInstance().newXPath();
//		Node nodeHDon = (Node) xPath.evaluate("/KetQuaTraCuu/DuLieu/TDiep[last()]/DLieu/HDon", doc, XPathConstants.NODE);
		Node nodeHDon = null;
		for(int i = 1; i<= 20; i++) {
			nodeHDon = (Node) xPath.evaluate("/KetQuaTraCuu/DuLieu/TDiep[" + i + "]/DLieu/HDon", doc, XPathConstants.NODE);
			if(nodeHDon != null) break;
		}
		/*FILE CHUA DUOC DUI DEN CO QUAN THUE*/
		if(null == nodeHDon) {
			nodeHDon = (Node) xPath.evaluate("/HDon", doc, XPathConstants.NODE);
		}
		
		Node nodeTTChung = (Node) xPath.evaluate("DLHDon/TTChung", nodeHDon, XPathConstants.NODE);
		Node nodeNBan = (Node) xPath.evaluate("DLHDon/NDHDon/NBan", nodeHDon, XPathConstants.NODE);
		Node nodeNMua = (Node) xPath.evaluate("DLHDon/NDHDon/NMua", nodeHDon, XPathConstants.NODE);
		Node nodeTToan = (Node) xPath.evaluate("DLHDon/NDHDon/TToan", nodeHDon, XPathConstants.NODE);
		NodeList nodeListHHDVu = (NodeList) xPath.evaluate("DLHDon/NDHDon/DSHHDVu/HHDVu", nodeHDon, XPathConstants.NODESET);
		
		LocalDate localDateNLap = null;
		NLap = commons.getTextFromNodeXML((Element) xPath.evaluate("NLap", nodeTTChung, XPathConstants.NODE));
		if(!"".equals(NLap)) {
			try {
				localDateNLap = commons.convertStringToLocalDate(NLap, "yyyy-MM-dd");
			}catch(Exception e) {}
		}
		
		if(null != localDateNLap) {
			reportParams.put("InvoiceDate", StringUtils.leftPad(String.valueOf(localDateNLap.get(ChronoField.DAY_OF_MONTH)), 2, "0"));
			reportParams.put("InvoiceMonth", StringUtils.leftPad(String.valueOf(localDateNLap.get(ChronoField.MONTH_OF_YEAR)), 2, "0"));
			reportParams.put("InvoiceYear", StringUtils.leftPad(String.valueOf(localDateNLap.get(ChronoField.YEAR)), 4, "0"));
		}
		LocalDate localDateCD= null;
		localDateCD = commons.convertStringToLocalDate(NLap, "yyyy-MM-dd");
			reportParams.put("InvoiceCovertDate", StringUtils.leftPad(String.valueOf(localDateCD.get(ChronoField.DAY_OF_MONTH)), 2, "0"));
			reportParams.put("InvoiceCovertMonth", StringUtils.leftPad(String.valueOf(localDateCD.get(ChronoField.MONTH_OF_YEAR)), 2, "0"));
			reportParams.put("InvoiceCovertYear", StringUtils.leftPad(String.valueOf(localDateCD.get(ChronoField.YEAR)), 4, "0"));
		
		reportParams.put("KHHDon", 
			commons.getTextFromNodeXML((Element) xPath.evaluate("KHMSHDon", nodeTTChung, XPathConstants.NODE))
			+ commons.getTextFromNodeXML((Element) xPath.evaluate("KHHDon", nodeTTChung, XPathConstants.NODE))
		);
//		reportParams.put("SHDon",
//			commons.addSpacingAfterLeter(
//				commons.formatNumberBillInvoice(commons.getTextFromNodeXML((Element) xPath.evaluate("SHDon", nodeTTChung, XPathConstants.NODE)))
//			)
//		);
//		reportParams.put("SHDonOrigin", commons.getTextFromNodeXML((Element) xPath.evaluate("SHDon", nodeTTChung, XPathConstants.NODE)));
//		
		
		if(CheckView.equals("Y"))
		{
					reportParams.put("SHDon", commons.getTextFromNodeXML((Element) xPath.evaluate("SHDon", nodeTTChung, XPathConstants.NODE)));
					reportParams.put("SHDonOrigin", commons.getTextFromNodeXML((Element) xPath.evaluate("SHDon", nodeTTChung, XPathConstants.NODE)));
					
		}
		else {
			reportParams.put("SHDon",
					commons.addSpacingAfterLeter(
						commons.formatNumberBillInvoice(commons.getTextFromNodeXML((Element) xPath.evaluate("SHDon", nodeTTChung, XPathConstants.NODE)))
					)
				);
			reportParams.put("SHDonOrigin",
						commons.addSpacingAfterLeter(
							commons.formatNumberBillInvoice(commons.getTextFromNodeXML((Element) xPath.evaluate("SHDon", nodeTTChung, XPathConstants.NODE)))
						)
					);
		}	
		
		
		
		
		
		
		
		
		reportParams.put("MCCQT", 
			commons.getTextFromNodeXML((Element) xPath.evaluate("MCCQT", nodeHDon, XPathConstants.NODE))
		);
		//TTChung/TTKhac
		nodeListTTKhac = (NodeList) xPath.evaluate("TTKhac/TTin", nodeTTChung, XPathConstants.NODESET);
		if(nodeListTTKhac != null) {
			for(int j = 0; j < nodeListTTKhac.getLength(); j++) {
				nodeSubTmp = nodeListTTKhac.item(j);
				
				KDLieu = commons.getTextFromNodeXML((Element) xPath.evaluate("KDLieu", nodeSubTmp, XPathConstants.NODE));
				switch (KDLieu.toUpperCase()) {
				case "DECIMAL":
					reportParams.put(
						commons.getTextFromNodeXML((Element) xPath.evaluate("TTruong", nodeSubTmp, XPathConstants.NODE)),
						commons.ToNumber(commons.getTextFromNodeXML((Element) xPath.evaluate("DLieu", nodeSubTmp, XPathConstants.NODE)))
					);
					break;
				default:
					reportParams.put(
						commons.getTextFromNodeXML((Element) xPath.evaluate("TTruong", nodeSubTmp, XPathConstants.NODE)),
						commons.getTextFromNodeXML((Element) xPath.evaluate("DLieu", nodeSubTmp, XPathConstants.NODE))
					);
					break;
				}
			}
		}
		
		//CHECK SECUREKEY									
				String checkSecureKey = (String) reportParams.get("SecureKey");
				
				if(checkSecureKey == null || checkSecureKey.equals("")) {
					reportParams.put("SecureKey", secureKey);
				}			
				//END CHECK SECUREKEY
		
		reportParams.put("SignName", commons.getTextFromNodeXML((Element) xPath.evaluate("Ten", nodeNBan, XPathConstants.NODE)));
		reportParams.put("NBanTen", 
			commons.getTextFromNodeXML((Element) xPath.evaluate("Ten", nodeNBan, XPathConstants.NODE))
		);
		reportParams.put("NBanMST", 
			commons.getTextFromNodeXML((Element) xPath.evaluate("MST", nodeNBan, XPathConstants.NODE))
		);
		reportParams.put("NBanDChi", 
			commons.getTextFromNodeXML((Element) xPath.evaluate("DChi", nodeNBan, XPathConstants.NODE))
		);
		reportParams.put("NBanSDThoai", 
			commons.getTextFromNodeXML((Element) xPath.evaluate("SDThoai", nodeNBan, XPathConstants.NODE))
		);
		reportParams.put("NBanDCTDTu", 
			commons.getTextFromNodeXML((Element) xPath.evaluate("DCTDTu", nodeNBan, XPathConstants.NODE))
		);
		reportParams.put("NBanSTKNHang", 
			commons.getTextFromNodeXML((Element) xPath.evaluate("STKNHang", nodeNBan, XPathConstants.NODE))
		);
		reportParams.put("NBanTNHang", 
			commons.getTextFromNodeXML((Element) xPath.evaluate("TNHang", nodeNBan, XPathConstants.NODE))
		);
		reportParams.put("NBanFax", 
			commons.getTextFromNodeXML((Element) xPath.evaluate("Fax", nodeNBan, XPathConstants.NODE))
		);
		reportParams.put("NBanWebsite", 
			commons.getTextFromNodeXML((Element) xPath.evaluate("Website", nodeNBan, XPathConstants.NODE))
		);
		
		/*THONG TIN KHAC (NEU CO)*/
		nodeListTTKhac = (NodeList) xPath.evaluate("TTKhac/TTin", nodeNBan, XPathConstants.NODESET);
		if(nodeListTTKhac != null) {
			for(int j = 0; j < nodeListTTKhac.getLength(); j++) {
				nodeSubTmp = nodeListTTKhac.item(j);
				
				KDLieu = commons.getTextFromNodeXML((Element) xPath.evaluate("KDLieu", nodeSubTmp, XPathConstants.NODE));
				switch (KDLieu.toUpperCase()) {
				case "DECIMAL":
					reportParams.put(
						"NBan" + commons.getTextFromNodeXML((Element) xPath.evaluate("TTruong", nodeSubTmp, XPathConstants.NODE)),
						commons.ToNumber(commons.getTextFromNodeXML((Element) xPath.evaluate("DLieu", nodeSubTmp, XPathConstants.NODE)))
					);
					break;
				default:
					reportParams.put(
						"NBan" + commons.getTextFromNodeXML((Element) xPath.evaluate("TTruong", nodeSubTmp, XPathConstants.NODE)),
						commons.getTextFromNodeXML((Element) xPath.evaluate("DLieu", nodeSubTmp, XPathConstants.NODE))
					);
					break;
				}
			}
		}
		
		reportParams.put("NMuaTen", 
			commons.getTextFromNodeXML((Element) xPath.evaluate("Ten", nodeNMua, XPathConstants.NODE))
		);
		reportParams.put("NMuaMST", 
			commons.getTextFromNodeXML((Element) xPath.evaluate("MST", nodeNMua, XPathConstants.NODE))
		);
		reportParams.put("NMuaDChi", 
			commons.getTextFromNodeXML((Element) xPath.evaluate("DChi", nodeNMua, XPathConstants.NODE))
		);
		reportParams.put("NMuaMKHang", 
			commons.getTextFromNodeXML((Element) xPath.evaluate("MKHang", nodeNMua, XPathConstants.NODE))
		);
		reportParams.put("NMuaSDThoai", 
			commons.getTextFromNodeXML((Element) xPath.evaluate("SDThoai", nodeNMua, XPathConstants.NODE))
		);
		reportParams.put("NMuaDCTDTu", 
			commons.getTextFromNodeXML((Element) xPath.evaluate("DCTDTu", nodeNMua, XPathConstants.NODE))
		);
		reportParams.put("NMuaHVTNMHang", 
			commons.getTextFromNodeXML((Element) xPath.evaluate("HVTNMHang", nodeNMua, XPathConstants.NODE))
		);
		reportParams.put("NMuaSTKNHang", 
			commons.getTextFromNodeXML((Element) xPath.evaluate("STKNHang", nodeNMua, XPathConstants.NODE))
		);
		reportParams.put("NMuaTNHang", 
			commons.getTextFromNodeXML((Element) xPath.evaluate("TNHang", nodeNMua, XPathConstants.NODE))
		);
		
		reportParams.put("TToanTGia", commons.ToNumber(commons.getTextFromNodeXML((Element) xPath.evaluate("TGia", nodeTTChung, XPathConstants.NODE))));
		reportParams.put("TTChungHTTToan", commons.getTextFromNodeXML((Element) xPath.evaluate("HTTToan", nodeTTChung, XPathConstants.NODE)));
		
		String TCHDon = commons.getTextFromNodeXML((Element) xPath.evaluate("TTHDLQuan/TCHDon", nodeTTChung, XPathConstants.NODE));
		String noticeTTDC = "";
		switch (TCHDon) {
		case "1":
			noticeTTDC = String.format("(Thay thế cho hóa đơn Mẫu số %s, Ký hiệu %s, số %s, ngày %s)", 
					commons.getTextFromNodeXML((Element) xPath.evaluate("TTHDLQuan/KHMSHDCLQuan", nodeTTChung, XPathConstants.NODE)),
					commons.getTextFromNodeXML((Element) xPath.evaluate("TTHDLQuan/KHHDCLQuan", nodeTTChung, XPathConstants.NODE)),
					commons.formatNumberBillInvoice(commons.getTextFromNodeXML((Element) xPath.evaluate("TTHDLQuan/SHDCLQuan", nodeTTChung, XPathConstants.NODE))),
					commons.convertLocalDateTimeToString(commons.convertStringToLocalDate(commons.getTextFromNodeXML((Element) xPath.evaluate("TTHDLQuan/NLHDCLQuan", nodeTTChung, XPathConstants.NODE)), "yyyy-MM-dd"), Constants.FORMAT_DATE.FORMAT_DATE_WEB)
				);
			break;
		case "2":
			noticeTTDC = String.format("(Điều chỉnh cho hóa đơn Mẫu số %s, Ký hiệu %s, số %s, ngày %s)", 
					commons.getTextFromNodeXML((Element) xPath.evaluate("TTHDLQuan/KHMSHDCLQuan", nodeTTChung, XPathConstants.NODE)),
					commons.getTextFromNodeXML((Element) xPath.evaluate("TTHDLQuan/KHHDCLQuan", nodeTTChung, XPathConstants.NODE)),
					commons.formatNumberBillInvoice(commons.getTextFromNodeXML((Element) xPath.evaluate("TTHDLQuan/SHDCLQuan", nodeTTChung, XPathConstants.NODE))),
					commons.convertLocalDateTimeToString(commons.convertStringToLocalDate(commons.getTextFromNodeXML((Element) xPath.evaluate("TTHDLQuan/NLHDCLQuan", nodeTTChung, XPathConstants.NODE)), "yyyy-MM-dd"), Constants.FORMAT_DATE.FORMAT_DATE_WEB)
				);
			break;
		default:
			break;
		}
		reportParams.put("NoticeTTDC", noticeTTDC);
		
		reportParams.put("TTChungDVTTe", commons.getTextFromNodeXML((Element) xPath.evaluate("DVTTe", nodeTTChung, XPathConstants.NODE)));
		reportParams.put("TToanTgTCThue", 
			commons.ToNumber(commons.getTextFromNodeXML((Element) xPath.evaluate("TgTCThue", nodeTToan, XPathConstants.NODE)))
		);
		reportParams.put("TToanTgTThue", 
			commons.ToNumber(commons.getTextFromNodeXML((Element) xPath.evaluate("TgTThue", nodeTToan, XPathConstants.NODE)))
		);
		reportParams.put("TToanTgTTTBSo", 
			commons.ToNumber(commons.getTextFromNodeXML((Element) xPath.evaluate("TgTTTBSo", nodeTToan, XPathConstants.NODE)))
		);
		/*THONG TIN KHAC (NEU CO)*/
		nodeListTTKhac = (NodeList) xPath.evaluate("TTKhac/TTin", nodeTToan, XPathConstants.NODESET);
		if(nodeListTTKhac != null) {
			for(int j = 0; j < nodeListTTKhac.getLength(); j++) {
				nodeSubTmp = nodeListTTKhac.item(j);
				
				KDLieu = commons.getTextFromNodeXML((Element) xPath.evaluate("KDLieu", nodeSubTmp, XPathConstants.NODE));
				switch (KDLieu.toUpperCase()) {
				case "DECIMAL":
					reportParams.put(
						"TToan" + commons.getTextFromNodeXML((Element) xPath.evaluate("TTruong", nodeSubTmp, XPathConstants.NODE)),
						commons.ToNumber(commons.getTextFromNodeXML((Element) xPath.evaluate("DLieu", nodeSubTmp, XPathConstants.NODE)))
					);
					break;
				default:
					reportParams.put(
						"TToan" + commons.getTextFromNodeXML((Element) xPath.evaluate("TTruong", nodeSubTmp, XPathConstants.NODE)),
						commons.getTextFromNodeXML((Element) xPath.evaluate("DLieu", nodeSubTmp, XPathConstants.NODE))
					);
					break;
				}
			}
		}
//		reportParams.put("TToanTgTTTQDoi", 
//			commons.ToNumber(commons.getTextFromNodeXML((Element) xPath.evaluate("TgTTTBSo", nodeTToan, XPathConstants.NODE)))
//		);
		reportParams.put("TToanTgTTTBChu", 
			commons.getTextFromNodeXML((Element) xPath.evaluate("TgTTTBChu", nodeTToan, XPathConstants.NODE))
		);
		
		/*LAY DANH SACH LAI SUAT*/
		String tsuatTmp = "";
		NodeList nodeListLTSuat = null;
		try {
			nodeListLTSuat = (NodeList) xPath.evaluate("THTTLTSuat/LTSuat", nodeTToan, XPathConstants.NODESET);
			if(nodeListLTSuat != null && nodeListLTSuat.getLength() > 0) {
				for(int i = 0; i < nodeListLTSuat.getLength(); i++) {
					nodeTmp = nodeListLTSuat.item(i);
					
					tsuatTmp = commons.getTextFromNodeXML((Element) xPath.evaluate("TSuat", nodeTmp, XPathConstants.NODE)).replaceAll("%", "");
					reportParams.put("Tax_TSuat_" + tsuatTmp, tsuatTmp);
					reportParams.put("Tax_ThTien_" + tsuatTmp, 
						commons.ToNumber(commons.getTextFromNodeXML((Element) xPath.evaluate("ThTien", nodeTmp, XPathConstants.NODE)))
					);
					reportParams.put("Tax_TThue_" + tsuatTmp, 
						commons.ToNumber(commons.getTextFromNodeXML((Element) xPath.evaluate("TThue", nodeTmp, XPathConstants.NODE)))
					);
					
					if(reportParams.get("Tax_TSuat_Default") == null) {
						reportParams.put("Tax_TSuat_Default", tsuatTmp);
						reportParams.put("Tax_ThTien_Default", 
							commons.ToNumber(commons.getTextFromNodeXML((Element) xPath.evaluate("ThTien", nodeTmp, XPathConstants.NODE)))
						);
						reportParams.put("Tax_TThue_Default", 
							commons.ToNumber(commons.getTextFromNodeXML((Element) xPath.evaluate("TThue", nodeTmp, XPathConstants.NODE)))
						);
					}
				}
			}
		}catch(Exception e) {}
		/*END - LAY DANH SACH LAI SUAT*/
		
		/*KIEM TRA THONG TIN NGUOI BAN KY CHUA*/
		Node nodeDSCKS = (Node) xPath.evaluate("DSCKS", nodeHDon, XPathConstants.NODE);
		Node nodeSignature = null;
		if(null != nodeDSCKS)
			nodeSignature = (Node) xPath.evaluate("NBan/Signature", nodeDSCKS, XPathConstants.NODE);
		LocalDateTime ldt = null;
		if(null == nodeSignature) {
			reportParams.put("IsSigned", false);
			reportParams.put("SignDesc", "Chưa ký");
			reportParams.put("UrlImageVerify", Paths.get(SystemParams.DIR_E_INVOICE_TEMPLATE, Constants.TEMPLATE_FILE_NAME.IMG_SIGNATURE_INVALID).toString());
		}else {
			String x509Certificate = commons.getTextFromNodeXML((Element) xPath.evaluate("KeyInfo/X509Data/X509Certificate", nodeSignature, XPathConstants.NODE));
			String signingTime = commons.getTextFromNodeXML((Element) xPath.evaluate("Object[@Id='SigningTime']/SignatureProperties/SignatureProperty/SigningTime", nodeSignature, XPathConstants.NODE));
			
			reportParams.put("IsSigned", true);
			reportParams.put("SignDesc", "Đã ký");
			SignTypeInfo signTypeInfo = commons.parserCert(x509Certificate);
			reportParams.put("SignName", null == signTypeInfo? "": signTypeInfo.getName());
			try {
				ldt = commons.convertStringToLocalDateTime(signingTime, "yyyy-MM-dd'T'HH:mm:ss'Z'");
				reportParams.put("SignDate", commons.convertLocalDateTimeToString(ldt, Constants.FORMAT_DATE.FORMAT_DATE_WEB));
			}catch(Exception e) {}
			reportParams.put("UrlImageVerify", Paths.get(SystemParams.DIR_E_INVOICE_TEMPLATE, Constants.TEMPLATE_FILE_NAME.IMG_SIGNATURE_VALID).toString());
		}
		reportParams.put("UrlImageInvDeleted", Paths.get(SystemParams.DIR_E_INVOICE_TEMPLATE, Constants.TEMPLATE_FILE_NAME.IMG_INV_DELETED).toString());
		reportParams.put("IsDeleted", isDeleted);
		reportParams.put("UrlImageInvThayThe", Paths.get(SystemParams.DIR_E_INVOICE_TEMPLATE, Constants.TEMPLATE_FILE_NAME.IMG_INV_THAYTHE).toString());
		reportParams.put("IsThayThe", isThayThe);
		
		List<HashMap<String, Object>> arrayData = new ArrayList<>();
		HashMap<String, Object> hItem = null;
		
		int lenProName = 0;
		String productName = "";
		int startRowGroup = 0;
		
		/*XEM CO IN NHIEU DONG KHONG*/
		int groupPageIDX = 1;
		boolean isUsingMultiPage = false;
		if(nodeListHHDVu != null) {
			for(int i = 0; i < nodeListHHDVu.getLength(); i++) {
				nodeTmp = nodeListHHDVu.item(i);
				
				productName = commons.getTextFromNodeXML((Element) xPath.evaluate("THHDVu", nodeTmp, XPathConstants.NODE));
				lenProName = productName.getBytes(StandardCharsets.UTF_8).length;
				startRowGroup += Math.ceil((double) lenProName / numberCharsInRow);
				
				if(startRowGroup >= numberRowInPage) {
					isUsingMultiPage = true;
					break;
				}
			}	
		}
		/*END - XEM CO IN NHIEU DONG KHONG*/
		
		startRowGroup = 0;
		KDLieu = "";
		nodeTmp = null;
		nodeSubTmp = null;
		nodeListTTKhac = null;
		countPrd = 0;
		if(nodeListHHDVu != null) {
			if(isUsingMultiPage) {
				for(int i = 0; i < nodeListHHDVu.getLength(); i++) {
					nodeTmp = nodeListHHDVu.item(i);
					
					productName = commons.getTextFromNodeXML((Element) xPath.evaluate("THHDVu", nodeTmp, XPathConstants.NODE));
					lenProName = productName.getBytes(StandardCharsets.UTF_8).length;
					startRowGroup += Math.ceil((double) lenProName / numberCharsInRow);
//					countRowForPrd = (int) Math.ceil(lenProName / numberCharsInRow);
//					if(countRowForPrd > 1)
//						i = i + countRowForPrd;
					
					hItem = new HashMap<>();
					hItem.put("GroupPageIDX", groupPageIDX);
					
					hItem.put("TChat", commons.getTextFromNodeXML((Element) xPath.evaluate("TChat", nodeTmp, XPathConstants.NODE)));
					hItem.put("STT", commons.getTextFromNodeXML((Element) xPath.evaluate("STT", nodeTmp, XPathConstants.NODE)));
					hItem.put("MHHDVu", commons.getTextFromNodeXML((Element) xPath.evaluate("MHHDVu", nodeTmp, XPathConstants.NODE)));
					
					hItem.put("THHDVu", productName);
					
					hItem.put("DVTinh", commons.getTextFromNodeXML((Element) xPath.evaluate("DVTinh", nodeTmp, XPathConstants.NODE)));
					hItem.put("SLuong",
						commons.ToNumber(commons.getTextFromNodeXML((Element) xPath.evaluate("SLuong", nodeTmp, XPathConstants.NODE)))
					);
					hItem.put("DGia",
						commons.ToNumber(commons.getTextFromNodeXML((Element) xPath.evaluate("DGia", nodeTmp, XPathConstants.NODE)))
					);
					hItem.put("ThTien",
						commons.ToNumber(commons.getTextFromNodeXML((Element) xPath.evaluate("ThTien", nodeTmp, XPathConstants.NODE)))
					);
					hItem.put("TSuat",
						commons.getTextFromNodeXML((Element) xPath.evaluate("TSuat", nodeTmp, XPathConstants.NODE))
					);
					
					/*LAY NOI DUNG TTKHAC*/
					nodeListTTKhac = (NodeList) xPath.evaluate("TTKhac/TTin", nodeTmp, XPathConstants.NODESET);
					if(nodeListTTKhac != null) {
						for(int j = 0; j < nodeListTTKhac.getLength(); j++) {
							nodeSubTmp = nodeListTTKhac.item(j);
							
							KDLieu = commons.getTextFromNodeXML((Element) xPath.evaluate("KDLieu", nodeSubTmp, XPathConstants.NODE));
							switch (KDLieu.toUpperCase()) {
							case "DECIMAL":
								hItem.put(
									commons.getTextFromNodeXML((Element) xPath.evaluate("TTruong", nodeSubTmp, XPathConstants.NODE)),
									commons.ToNumber(commons.getTextFromNodeXML((Element) xPath.evaluate("DLieu", nodeSubTmp, XPathConstants.NODE)))
								);
								break;
							default:
								hItem.put(
									commons.getTextFromNodeXML((Element) xPath.evaluate("TTruong", nodeSubTmp, XPathConstants.NODE)),
									commons.getTextFromNodeXML((Element) xPath.evaluate("DLieu", nodeSubTmp, XPathConstants.NODE))
								);
								break;
							}
						}
					}
					
					arrayData.add(hItem);
					countPrd++;	
					
					if(startRowGroup >= numberRowInPageMultiPage) {
						startRowGroup = 0;
						groupPageIDX++;
					}
				}
				if(0 == startRowGroup && nodeListHHDVu.getLength() > 0) groupPageIDX--;		//BO BOT TRANG CUOI CUNG VI KHONG CO DU LIEU
				
				if(0 == startRowGroup) {
					groupPageIDX++;
					for(int i = 0; i < numberRowInPage; i++) {
						hItem = new HashMap<>();
						hItem.put("ProdName", " ");
						hItem.put("GroupPageIDX", groupPageIDX);
						arrayData.add(hItem);
					}
				}else if(startRowGroup <= numberRowInPage) {
					for(int i = startRowGroup; i < numberRowInPage; i++) {
						hItem = new HashMap<>();
						hItem.put("ProdName", " ");
						hItem.put("GroupPageIDX", groupPageIDX);
						arrayData.add(hItem);
					}
				}else {
					for(int i = startRowGroup; i < numberRowInPageMultiPage; i++) {
						hItem = new HashMap<>();
						hItem.put("ProdName", " ");
						hItem.put("GroupPageIDX", groupPageIDX);
						arrayData.add(hItem);
					}
					groupPageIDX++;
					for(int i = 0; i < numberRowInPage; i++) {
						hItem = new HashMap<>();
						hItem.put("ProdName", " ");
						hItem.put("GroupPageIDX", groupPageIDX);
						arrayData.add(hItem);
					}
				}
			}else {
				for(int i = 0; i < nodeListHHDVu.getLength(); i++) {
					nodeTmp = nodeListHHDVu.item(i);
					
					productName = commons.getTextFromNodeXML((Element) xPath.evaluate("THHDVu", nodeTmp, XPathConstants.NODE));
					lenProName = productName.getBytes(StandardCharsets.UTF_8).length;
					startRowGroup += Math.ceil((double) lenProName / numberCharsInRow);
//					countRowForPrd = (int) Math.ceil(lenProName / numberCharsInRow);
//					if(countRowForPrd > 1)
//						i = i + countRowForPrd;
					
					hItem = new HashMap<>();
					hItem.put("GroupPageIDX", groupPageIDX);
					
					hItem.put("TChat", commons.getTextFromNodeXML((Element) xPath.evaluate("TChat", nodeTmp, XPathConstants.NODE)));
					hItem.put("STT", commons.getTextFromNodeXML((Element) xPath.evaluate("STT", nodeTmp, XPathConstants.NODE)));
					hItem.put("MHHDVu", commons.getTextFromNodeXML((Element) xPath.evaluate("MHHDVu", nodeTmp, XPathConstants.NODE)));
					
					hItem.put("THHDVu", productName);
					
					hItem.put("DVTinh", commons.getTextFromNodeXML((Element) xPath.evaluate("DVTinh", nodeTmp, XPathConstants.NODE)));
					hItem.put("SLuong",
						commons.ToNumber(commons.getTextFromNodeXML((Element) xPath.evaluate("SLuong", nodeTmp, XPathConstants.NODE)))
					);
					hItem.put("DGia",
						commons.ToNumber(commons.getTextFromNodeXML((Element) xPath.evaluate("DGia", nodeTmp, XPathConstants.NODE)))
					);
					hItem.put("ThTien",
						commons.ToNumber(commons.getTextFromNodeXML((Element) xPath.evaluate("ThTien", nodeTmp, XPathConstants.NODE)))
					);
					hItem.put("TSuat",
						commons.getTextFromNodeXML((Element) xPath.evaluate("TSuat", nodeTmp, XPathConstants.NODE))
					);
					
					/*LAY NOI DUNG TTKHAC*/
					nodeListTTKhac = (NodeList) xPath.evaluate("TTKhac/TTin", nodeTmp, XPathConstants.NODESET);
					if(nodeListTTKhac != null) {
						for(int j = 0; j < nodeListTTKhac.getLength(); j++) {
							nodeSubTmp = nodeListTTKhac.item(j);
							
							KDLieu = commons.getTextFromNodeXML((Element) xPath.evaluate("KDLieu", nodeSubTmp, XPathConstants.NODE));
							switch (KDLieu.toUpperCase()) {
							case "DECIMAL":
								hItem.put(
									commons.getTextFromNodeXML((Element) xPath.evaluate("TTruong", nodeSubTmp, XPathConstants.NODE)),
									commons.ToNumber(commons.getTextFromNodeXML((Element) xPath.evaluate("DLieu", nodeSubTmp, XPathConstants.NODE)))
								);
								break;
							default:
								hItem.put(
									commons.getTextFromNodeXML((Element) xPath.evaluate("TTruong", nodeSubTmp, XPathConstants.NODE)),
									commons.getTextFromNodeXML((Element) xPath.evaluate("DLieu", nodeSubTmp, XPathConstants.NODE))
								);
								break;
							}
						}
					}
					
					arrayData.add(hItem);
					countPrd++;	
					
					if(startRowGroup >= numberRowInPage) {
						startRowGroup = 0;
						groupPageIDX++;
					}
				}
				if(0 == startRowGroup && nodeListHHDVu.getLength() > 0) groupPageIDX--;		//BO BOT TRANG CUOI CUNG VI KHONG CO DU LIEU
				
				/*ADD THEM DU LIEU CHO DU TRANG*/
				if(startRowGroup > 0 || nodeListHHDVu.getLength() == 0) {
					for(int i = startRowGroup; i < numberRowInPage; i++) {
						hItem = new HashMap<>();
						hItem.put("ProdName", " ");
						hItem.put("GroupPageIDX", groupPageIDX);
						arrayData.add(hItem);
					}
				}
			}
			
//			for(int i = 0; i < nodeListHHDVu.getLength(); i++) {
//				nodeTmp = nodeListHHDVu.item(i);
//				
//				productName = commons.getTextFromNodeXML((Element) xPath.evaluate("THHDVu", nodeTmp, XPathConstants.NODE));
//				lenProName = productName.getBytes(StandardCharsets.UTF_8).length;
//				startRowGroup += Math.ceil((double) lenProName / numberCharsInRow);
////				countRowForPrd = (int) Math.ceil(lenProName / numberCharsInRow);
////				if(countRowForPrd > 1)
////					i = i + countRowForPrd;
//				
//				hItem = new HashMap<>();
//				hItem.put("GroupPageIDX", 0);
//				
//				hItem.put("TChat", commons.getTextFromNodeXML((Element) xPath.evaluate("TChat", nodeTmp, XPathConstants.NODE)));
//				hItem.put("STT", commons.getTextFromNodeXML((Element) xPath.evaluate("STT", nodeTmp, XPathConstants.NODE)));
//				hItem.put("MHHDVu", commons.getTextFromNodeXML((Element) xPath.evaluate("MHHDVu", nodeTmp, XPathConstants.NODE)));
//				
//				hItem.put("THHDVu", productName);
//				
//				hItem.put("DVTinh", commons.getTextFromNodeXML((Element) xPath.evaluate("DVTinh", nodeTmp, XPathConstants.NODE)));
//				hItem.put("SLuong",
//					commons.ToNumber(commons.getTextFromNodeXML((Element) xPath.evaluate("SLuong", nodeTmp, XPathConstants.NODE)))
//				);
//				hItem.put("DGia",
//					commons.ToNumber(commons.getTextFromNodeXML((Element) xPath.evaluate("DGia", nodeTmp, XPathConstants.NODE)))
//				);
//				hItem.put("ThTien",
//					commons.ToNumber(commons.getTextFromNodeXML((Element) xPath.evaluate("ThTien", nodeTmp, XPathConstants.NODE)))
//				);
//				hItem.put("TSuat",
//					commons.getTextFromNodeXML((Element) xPath.evaluate("TSuat", nodeTmp, XPathConstants.NODE))
//				);
//				
//				/*LAY NOI DUNG TTKHAC*/
//				nodeListTTKhac = (NodeList) xPath.evaluate("TTKhac/TTin", nodeTmp, XPathConstants.NODESET);
//				if(nodeListTTKhac != null) {
//					for(int j = 0; j < nodeListTTKhac.getLength(); j++) {
//						nodeSubTmp = nodeListTTKhac.item(j);
//						
//						KDLieu = commons.getTextFromNodeXML((Element) xPath.evaluate("KDLieu", nodeSubTmp, XPathConstants.NODE));
//						switch (KDLieu.toUpperCase()) {
//						case "DECIMAL":
//							hItem.put(
//								commons.getTextFromNodeXML((Element) xPath.evaluate("TTruong", nodeSubTmp, XPathConstants.NODE)),
//								commons.ToNumber(commons.getTextFromNodeXML((Element) xPath.evaluate("DLieu", nodeSubTmp, XPathConstants.NODE)))
//							);
//							break;
//						default:
//							hItem.put(
//								commons.getTextFromNodeXML((Element) xPath.evaluate("TTruong", nodeSubTmp, XPathConstants.NODE)),
//								commons.getTextFromNodeXML((Element) xPath.evaluate("DLieu", nodeSubTmp, XPathConstants.NODE))
//							);
//							break;
//						}
//					}
//				}
//				
//				arrayData.add(hItem);
//				countPrd++;	
//			}
		}		
//		if(startRowGroup < numberRowInPage) {
//			for(int i = startRowGroup; i< numberRowInPage; i++) {
//				hItem = new HashMap<>();
//				hItem.put("GroupPageIDX", 0);
//				arrayData.add(hItem);
//			}
//			
//		}
		
		reportParams.put("PortalLink", 
				link
		);
		
		
		reportParams.put("TOTAL_PAGE", (int) groupPageIDX);
		
		JRDataSource jds = null;
		jds = new JRBeanCollectionDataSource(arrayData);
		
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		JasperReport jr = JasperCompileManager.compileReport(new FileInputStream(fileJP));
		JasperPrint jp = JasperFillManager.fillReport(jr, reportParams, jds);
		Exporter exporter = null;
		
		exporter = new JRPdfExporter();
		exporter.setExporterInput(new SimpleExporterInput(jp));
		exporter.setExporterOutput(new SimpleOutputStreamExporterOutput(out));
        SimplePdfExporterConfiguration configuration = new SimplePdfExporterConfiguration();
        configuration.setCreatingBatchModeBookmarks(true);
        exporter.setConfiguration(configuration);
        exporter.exportReport();
        return out;
	}



	public ByteArrayOutputStream viewpdftncn(File fileJP, org.bson.Document docTmp, String pathLogo, String KH,boolean isConvert) throws Exception{
				Map<String, Object> reportParams = new HashMap<String, Object>();
				
				/*KIEM TRA FILE LOG & BACKGROUND CO TON TAI KHONG*/
				File f = new File(pathLogo);
				if(f.exists() && f.isFile())
					reportParams.put("URL_IMG_LOGO", pathLogo);
				else
					reportParams.put("URL_IMG_LOGO", Paths.get(SystemParams.DIR_E_INVOICE_TEMPLATE, IMAGE_FILENAME_BLANK).toString());			
			reportParams.put("IsConvert", isConvert);

			String KYHieu = docTmp.getEmbedded(Arrays.asList("KyHieu"), "");
			String MauSo = docTmp.getEmbedded(Arrays.asList("MauSo"), "");
			
				reportParams.put("TChucCNTen", 
						docTmp.getEmbedded(Arrays.asList("Issuer","Name"), "")
				);
				reportParams.put("TChucCNMST", 
						docTmp.getEmbedded(Arrays.asList("Issuer","TaxCode"), "")
				);
				reportParams.put("TChucCNDChi", 
						docTmp.getEmbedded(Arrays.asList("Issuer","Address"), "")
				);
				reportParams.put("TChucCNSDThoai", 
						docTmp.getEmbedded(Arrays.asList("Issuer","Phone"), "")
				);
				reportParams.put("KHHDon", 
						KYHieu
				);
				reportParams.put("MauSo", 
						MauSo
				);

				
				List<HashMap<String, Object>> arrayData = new ArrayList<>();
				HashMap<String, Object> hItem = null;
				
				int lenProName = 0;
				String productName = "";
				int startRowGroup = 0;
				
				
				startRowGroup = 0;
			
				
				JRDataSource jds = null;
				jds = new JRBeanCollectionDataSource(arrayData);
				
				ByteArrayOutputStream out = new ByteArrayOutputStream();
				JasperReport jr = JasperCompileManager.compileReport(new FileInputStream(fileJP));
				JasperPrint jp = JasperFillManager.fillReport(jr, reportParams, jds);
				Exporter exporter = null;
				
				exporter = new JRPdfExporter();
				exporter.setExporterInput(new SimpleExporterInput(jp));
				exporter.setExporterOutput(new SimpleOutputStreamExporterOutput(out));
		        SimplePdfExporterConfiguration configuration = new SimplePdfExporterConfiguration();
		        configuration.setCreatingBatchModeBookmarks(true);
		        exporter.setConfiguration(configuration);
		        exporter.exportReport();
		        return out;
		}




	public ByteArrayOutputStream viewpdfcttncn(File fileJP,Document doc, org.bson.Document docTmp, String pathLogo, String KH,String MS,String link,boolean isConvert, boolean isXoaBo) throws Exception{
				Map<String, Object> reportParams = new HashMap<String, Object>();
				
				/*KIEM TRA FILE LOG & BACKGROUND CO TON TAI KHONG*/
				File f = new File(pathLogo);
				if(f.exists() && f.isFile())
					reportParams.put("URL_IMG_LOGO", pathLogo);
				else
					reportParams.put("URL_IMG_LOGO", Paths.get(SystemParams.DIR_E_INVOICE_TEMPLATE, IMAGE_FILENAME_BLANK).toString());			
			reportParams.put("IsConvert", isConvert);

			
			
				reportParams.put("TChucCNTen", 
						docTmp.getEmbedded(Arrays.asList("Issuer","Name"), "")
				);
				reportParams.put("TChucCNMST", 
						docTmp.getEmbedded(Arrays.asList("Issuer","TaxCode"), "")
				);
				reportParams.put("TChucCNDChi", 
						docTmp.getEmbedded(Arrays.asList("Issuer","Address"), "")
				);
				reportParams.put("TChucCNSDThoai", 
						docTmp.getEmbedded(Arrays.asList("Issuer","Phone"), "")
				);
				reportParams.put("KHHDon", 
						KH
				);
				reportParams.put("MauSo", 
						MS
				);

				String checkky = docTmp.getEmbedded(Arrays.asList("SignStatus"), "");
				
				
				reportParams.put("PortalLink", 
						link
				);
				reportParams.put("SecureKey", 
					docTmp.get("SecureKey", "")
				);
				
				if(checkky.equals("NOSIGN")) {
					reportParams.put("IsSigned", false);
					reportParams.put("SignDesc", "Chưa ký");
					reportParams.put("UrlImageVerify", Paths.get(SystemParams.DIR_E_INVOICE_TEMPLATE, Constants.TEMPLATE_FILE_NAME.IMG_SIGNATURE_INVALID).toString());
				}else {			
					Node nodeSignature = null;
					
					XPath xPath = XPathFactory.newInstance().newXPath();
					LocalDateTime ldt = null;
					Node nodeHDon = null;
					if(null == nodeHDon) {
						nodeHDon = (Node) xPath.evaluate("/HDon", doc, XPathConstants.NODE);
					}
					Node nodeDSCKS = (Node) xPath.evaluate("DSCKS", nodeHDon, XPathConstants.NODE);
					nodeSignature = (Node) xPath.evaluate("NBan/Signature", nodeDSCKS, XPathConstants.NODE);
					Node nodeTTChung = (Node) xPath.evaluate("DLHDon/TTChung", nodeHDon, XPathConstants.NODE);
					reportParams.put("SHDonOrigin", commons.getTextFromNodeXML((Element) xPath.evaluate("SHDon", nodeTTChung, XPathConstants.NODE)));
					String signingTime = commons.getTextFromNodeXML((Element) xPath.evaluate("Object[@Id='SigningTime']/SignatureProperties/SignatureProperty/SigningTime", nodeSignature, XPathConstants.NODE));
					ldt = commons.convertStringToLocalDateTime(signingTime, "yyyy-MM-dd'T'HH:mm:ss'Z'");
						reportParams.put("IsSigned", true);
						reportParams.put("SignDesc", "Đã ký");
						reportParams.put("SignName", docTmp.getEmbedded(Arrays.asList("InfoSigned","SignedUserFullName"), ""));
		
							reportParams.put("SignDate", commons.convertLocalDateTimeToString(ldt, Constants.FORMAT_DATE.FORMAT_DATE_WEB));

					reportParams.put("SignName", docTmp.getEmbedded(Arrays.asList("InfoSigned","SignedUserFullName"), ""));
					reportParams.put("UrlImageVerify", Paths.get(SystemParams.DIR_E_INVOICE_TEMPLATE, Constants.TEMPLATE_FILE_NAME.IMG_SIGNATURE_VALID).toString());
				
				
				}
				
				reportParams.put("UrlImageInvDeleted", Paths.get(SystemParams.DIR_E_INVOICE_TEMPLATE, Constants.TEMPLATE_FILE_NAME.IMG_INV_XoaBo).toString());
				reportParams.put("IsDeleted", isXoaBo);
				
				reportParams.put("NNThueTen", 
						docTmp.getEmbedded(Arrays.asList("Name"), "")
				);
				reportParams.put("NNThueMST", 
						docTmp.getEmbedded(Arrays.asList("TaxCode"), "")
				);
				reportParams.put("NNThueQTich", 
						docTmp.getEmbedded(Arrays.asList("CMND-CCCD","QuocTich"), "")
				);
			
				 String cutru = 	docTmp.getEmbedded(Arrays.asList("CuTru"), "");
				 if(cutru.equals("CCT")) {
					 reportParams.put("isNNThueCuTru", 
								true
						);
						reportParams.put("UrlImageChecked", 
								 Paths.get(SystemParams.DIR_E_INVOICE_TEMPLATE, Constants.TEMPLATE_FILE_NAME.IMG_SIGNATURE_VALID).toString());
				 }
				 else {
					 reportParams.put("isNNThueKCuTru", 
								true
						);
						reportParams.put("UrlImageChecked", 
								 Paths.get(SystemParams.DIR_E_INVOICE_TEMPLATE, Constants.TEMPLATE_FILE_NAME.IMG_SIGNATURE_VALID).toString());
				 }
				
				reportParams.put("NNThueDChiORSDThoai", 
						docTmp.getEmbedded(Arrays.asList("Address"), "")
				);
				reportParams.put("NNThueCMND", 
						docTmp.getEmbedded(Arrays.asList("CMND-CCCD","CCCD"), "")
				);
				reportParams.put("NNThueCMNDNoiCap", 
						docTmp.getEmbedded(Arrays.asList("CMND-CCCD","CCCDADDRESS"), "")
				);
				reportParams.put("NNThueCMNDNgayCap", 
						docTmp.getEmbedded(Arrays.asList("CMND-CCCD","CCCDDATE"), "")
				);
				
				
				String CNhanTNhap = docTmp.getEmbedded(Arrays.asList("TNCNKhauTru","KhoanThuNhap"), "").replaceAll("\\₫", "");
				String CNhanBHiem = docTmp.getEmbedded(Arrays.asList("TNCNKhauTru","KhoanBaoHiem"), "").replaceAll("\\₫", "");
				String CNTongTNhapChiuThue = docTmp.getEmbedded(Arrays.asList("TNCNKhauTru","TongTNKhauTru"), "").replaceAll("\\₫", "");
				String CNTongTNhapTinhThue = docTmp.getEmbedded(Arrays.asList("TNCNKhauTru","TongTNTinhThue"), "").replaceAll("\\₫", "");
				String CNTNhapDaKhauTru = docTmp.getEmbedded(Arrays.asList("TNCNKhauTru","SoTienCaNhanKhauTru"), "").replaceAll("\\₫", "");
				
				String date = docTmp.getEmbedded(Arrays.asList("DateSave"), "");
				
				reportParams.put("CNhanTNhap", CNhanTNhap);
				reportParams.put("CNhanBHiem", CNhanBHiem);
				reportParams.put("CNTongTNhapChiuThue", CNTongTNhapChiuThue);
				reportParams.put("CNTongTNhapTinhThue", CNTongTNhapTinhThue);
				reportParams.put("CNTNhapDaKhauTru", CNTNhapDaKhauTru);
				
				String[] words;
				words = date.split("/");
				String thangnv ="";
		      
				int check = words.length;
			  
				if(check > 1) {														
		         String nam = words[1];		      
		         reportParams.put("CNTraTNhapYear", nam);				
	      
				String TuNgay = 	docTmp.getEmbedded(Arrays.asList("TuNgay"), "");
				String DenNgay = 	docTmp.getEmbedded(Arrays.asList("DenNgay"), "");
				String thang = "";
				  words = TuNgay.split("/");
			             nam = words[2];
			           thang = words[1];
			      int    tuthang = Integer.parseInt(thang);
			   
			      words = DenNgay.split("/");
		             nam = words[2];
		           thang = words[1];
		      int    denthang = Integer.parseInt(thang);

		      for(int i = tuthang; i<=denthang; i++) {  	
		    	  if(i == denthang)
		    	  {
		    		  thangnv += i;
		    	  }
		    	  else {
		    		  thangnv += i +",";
		    	  }
		    	  
		      }
		      reportParams.put("CNTraTNhapMonth", thangnv);
		            
			  }else {
				   String nam = words[0];		      
			         reportParams.put("CNTraTNhapYear", nam);		
			         reportParams.put("CNTraTNhapMonth", "1,2,3,4,5,6,7,8,9,10,11,12");
			  }
		      
		      
		      
		      
				LocalDate localDateNLap = LocalDate.now();
				String NLap = 	docTmp.getEmbedded(Arrays.asList("Date"), "");
						localDateNLap = commons.convertStringToLocalDate(NLap, "yyyy-MM-dd");
					
				
				if(null != localDateNLap) {
					reportParams.put("LicenseDate", StringUtils.leftPad(String.valueOf(localDateNLap.get(ChronoField.DAY_OF_MONTH)), 2, "0"));
					reportParams.put("LicenseMonth", StringUtils.leftPad(String.valueOf(localDateNLap.get(ChronoField.MONTH_OF_YEAR)), 2, "0"));
					reportParams.put("LicenseYear", StringUtils.leftPad(String.valueOf(localDateNLap.get(ChronoField.YEAR)), 4, "0"));
				}
				
				
				List<HashMap<String, Object>> arrayData = new ArrayList<>();
				HashMap<String, Object> hItem = null;
				
				int lenProName = 0;
				String productName = "";
				int startRowGroup = 0;
				
				
				startRowGroup = 0;
			
				
				JRDataSource jds = null;
				jds = new JRBeanCollectionDataSource(arrayData);
				
				ByteArrayOutputStream out = new ByteArrayOutputStream();
				JasperReport jr = JasperCompileManager.compileReport(new FileInputStream(fileJP));
				JasperPrint jp = JasperFillManager.fillReport(jr, reportParams, jds);
				Exporter exporter = null;
				
				exporter = new JRPdfExporter();
				exporter.setExporterInput(new SimpleExporterInput(jp));
				exporter.setExporterOutput(new SimpleOutputStreamExporterOutput(out));
		        SimplePdfExporterConfiguration configuration = new SimplePdfExporterConfiguration();
		        configuration.setCreatingBatchModeBookmarks(true);
		        exporter.setConfiguration(configuration);
		        exporter.exportReport();
		        return out;
		}	

	 public ByteArrayOutputStream viewPdfTiepnhan(File fileJP, Document doc, org.bson.Document docTmp, int numberRowInPage, int numberRowInPageMultiPage, int numberCharsInRow, String pathLogo, String pathBackground, boolean isConvert) throws Exception {
	      Map<String, Object> reportParams = new HashMap();
	      File f = new File(pathLogo);
	      if (f.exists() && f.isFile()) {
	         reportParams.put("URL_IMG_LOGO", pathLogo);
	      } else {
	         reportParams.put("URL_IMG_LOGO", Paths.get(SystemParams.DIR_E_INVOICE_TEMPLATE, "BLANK_ICON.png").toString());
	      }

	      f = new File(pathBackground);
	      if (f.exists() && f.isFile()) {
	         reportParams.put("URL_IMG_BACKGROUND", pathBackground);
	      } else {
	         reportParams.put("URL_IMG_BACKGROUND", Paths.get(SystemParams.DIR_E_INVOICE_TEMPLATE, "BLANK_ICON.png").toString());
	      }

	      reportParams.put("IsConvert", isConvert);
	      String NLap = "";
	      Node nodeTmp = null;
	      int countPrd = 0;
	      XPath xPath = XPathFactory.newInstance().newXPath();
	      Node nodeHDon = null;
			Node nodeTDiep = null;
	      String CQT_MLTDiep = "";
	      	      
	      for(int i = 1; i <= 20; ++i) {
	    	  nodeTDiep = (Node) xPath.evaluate("/KetQuaTraCuu/DuLieu/TDiep[" + i + "]", doc, XPathConstants.NODE);
	    	  CQT_MLTDiep = commons.getTextFromNodeXML((Element) xPath.evaluate("TTChung/MLTDiep", nodeTDiep, XPathConstants.NODE));
	         nodeHDon = (Node)xPath.evaluate("/KetQuaTraCuu/DuLieu/TDiep[" + i + "]/DLieu/TBao", doc, XPathConstants.NODE);
	         if("301".equals(CQT_MLTDiep)) {    	 
		          break;
		       }
	        
	      }


	      NodeList nodeListHHDVu = (NodeList)xPath.evaluate("DLTBao/DSHDon/HDon", nodeHDon, XPathConstants.NODESET);
	     
	      
	      String SoTB =  this.commons.getTextFromNodeXML((Element)xPath.evaluate("STBao/So", nodeHDon, XPathConstants.NODE)); 
	     
	      String MATC = docTmp.get("MTDTChieu", ""); 
	      
	      reportParams.put("TBHDDTSo",  SoTB); 
	    
	      reportParams.put("MGDDienTu",  MATC); 
	      
	      int SLHHDVu = nodeListHHDVu.getLength();
	      String CQTTNhanTotalHD = String.valueOf(SLHHDVu);
	      reportParams.put("CQTTNhanTotalHD",  CQTTNhanTotalHD); 
	      
	      
	      String TNNT = "";
	      String MST = "";
	      String check_TNNT = docTmp.getEmbedded(Arrays.asList("Issuer", "Name"), "");
	      String check_TNNT1 = docTmp.get("TNNT","");
	      if(!check_TNNT.equals("")) {
	    	  TNNT = check_TNNT;
	      }else {
	    	  TNNT =  check_TNNT1;
	      }
	      
	      String check_MST = docTmp.getEmbedded(Arrays.asList("Issuer", "TaxCode"), "");
	      String check_MST1 = docTmp.get("MST","");
	      if(!check_MST.equals("")) {
	    	  MST = check_MST;
	      }else {
	    	  MST =  check_MST1;
	      }
	    
	      reportParams.put("CQTQLy", (String)docTmp.getEmbedded(Arrays.asList("DDanh"), "") + "-" + (String)docTmp.getEmbedded(Arrays.asList("TCQT"), ""));
	      reportParams.put("NNTTen", TNNT);
	      reportParams.put("GNNTTen", TNNT);
	      
	      reportParams.put("NNTMST", MST);
	      List<HashMap<String, Object>> arrayData = new ArrayList();
	      HashMap<String, Object> hItem = null;
	      int lenProName = 0;
	      String productName = "";
	      int startRowGroup = 0;
	      int groupPageIDX = 1;
	      boolean isUsingMultiPage = false;
	      int i;
	      lenProName = 0;
	      if (nodeListHHDVu != null) {
	         for(i = 0; i < nodeListHHDVu.getLength(); ++i) {
	            nodeTmp = nodeListHHDVu.item(i);
	            productName = this.commons.getTextFromNodeXML((Element)xPath.evaluate("MCQTCap", nodeTmp, XPathConstants.NODE));
	            lenProName = productName.getBytes(StandardCharsets.UTF_8).length;
	            startRowGroup = (int)((double)startRowGroup + Math.ceil((double)lenProName / (double)numberCharsInRow));
	            if (startRowGroup >= numberRowInPage) {
	               isUsingMultiPage = true;
	               break;
	            }
	         }
	      }

	      startRowGroup = 0;
	      nodeTmp = null;
	       countPrd = 0;
	      String thang;
	      
	      
	      
			/*KIEM TRA THONG TIN NGUOI BAN KY CHUA*/
			Node nodeDSCKS = (Node) xPath.evaluate("DSCKS", nodeHDon, XPathConstants.NODE);
			Node nodeSignature = null;
			if(null != nodeDSCKS)
				nodeSignature = (Node) xPath.evaluate("CQT/Signature", nodeDSCKS, XPathConstants.NODE);
			LocalDateTime ldt = null;
				String x509Certificate = commons.getTextFromNodeXML((Element) xPath.evaluate("KeyInfo/X509Data/X509Certificate", nodeSignature, XPathConstants.NODE));
				String signingTime = commons.getTextFromNodeXML((Element) xPath.evaluate("Object[@Id='SigningTime']/SignatureProperties/SignatureProperty/SigningTime", nodeSignature, XPathConstants.NODE));
				reportParams.put("UrlImageVerify", Paths.get(SystemParams.DIR_E_INVOICE_TEMPLATE, Constants.TEMPLATE_FILE_NAME.IMG_SIGNATURE_VALID).toString());
				reportParams.put("IsSigned", true);
				reportParams.put("SignDesc", "Đã ký");
				SignTypeInfo signTypeInfo = commons.parserCert(x509Certificate);
				reportParams.put("SignName", null == signTypeInfo? "": signTypeInfo.getName());



	      
	      
	      
	      
		int CQTTNhan = 0;
	      if (nodeListHHDVu != null) {
	         String ngayc;
	         String[] words;
	         String nam;
	         String ngay;
	         String ngayview;
	         String TTTNCQT;
	         String tctb;
	         
	       
	         if (isUsingMultiPage) {
	            for(i = 0; i < nodeListHHDVu.getLength(); ++i) {
	               nodeTmp = nodeListHHDVu.item(i);
	               productName = this.commons.getTextFromNodeXML((Element)xPath.evaluate("MCQTCap", nodeTmp, XPathConstants.NODE));
	               lenProName = productName.getBytes(StandardCharsets.UTF_8).length;
	               startRowGroup = (int)((double)startRowGroup + Math.ceil((double)lenProName / (double)numberCharsInRow));
	               hItem = new HashMap();
	               hItem.put("GroupPageIDX", groupPageIDX);
	               hItem.put("STT", this.commons.getTextFromNodeXML((Element)xPath.evaluate("STT", nodeTmp, XPathConstants.NODE)));
	               hItem.put("MCQTCap", productName);
	               hItem.put("MSHDon", this.commons.getTextFromNodeXML((Element)xPath.evaluate("KHMSHDon", nodeTmp, XPathConstants.NODE)) + this.commons.getTextFromNodeXML((Element)xPath.evaluate("KHHDon", nodeTmp, XPathConstants.NODE)));
	               hItem.put("SHDon", this.commons.getTextFromNodeXML((Element)xPath.evaluate("SHDon", nodeTmp, XPathConstants.NODE)));
	               ngayc = this.commons.getTextFromNodeXML((Element)xPath.evaluate("NLap", nodeTmp, XPathConstants.NODE));
	               String ngayky = (String) docTmp.get("NTBao");
	               words = ngayc.split("-");
	               nam = words[0];
	               thang = words[1];
	               ngay = words[2];
	               ngayview = ngay + "-" + thang + "-" + nam;
	               ///////////////////
	               words.clone();
	               nam = "";
	               thang = "";
	               ngay ="";
	               words = ngayky.split("-");
	               nam = words[0];
	               thang = words[1];
	               ngay = words[2];
	               String  ngaykyview = ngay + "-" + thang + "-" + nam;
	               ///////////////
	               
	               
	               hItem.put("NLHDon", ngayview);
	               //////////////////////
	               hItem.put("SignDate", ngayview);
		           	reportParams.put("SignDate", ngaykyview);
		           	
		            reportParams.put("InvoiceDate",ngay);
		  	         reportParams.put("InvoiceMonth", thang);
		  	         reportParams.put("InvoiceYear", nam);
		  	    
	               ///////////////////////
	           
	               TTTNCQT = this.commons.getTextFromNodeXML((Element)xPath.evaluate("TTTNCCQT", nodeTmp, XPathConstants.NODE));
	               if ("1".equals(TTTNCQT)) {
	                  hItem.put("TTTNCQT", "Tiếp nhận");
	                  CQTTNhan++;
	               }

	               if ("2".equals(TTTNCQT)) {
	                  hItem.put("TTTNCQT", "Không tiếp nhận");
	               }

	               tctb = this.commons.getTextFromNodeXML((Element)xPath.evaluate("TCTBao", nodeTmp, XPathConstants.NODE));
	               if ("1".equals(tctb)) {
	                  hItem.put("TCTBao", "Hủy");
	               }

	               if ("2".equals(tctb)) {
	                  hItem.put("TCTBao", "Điều chỉnh");
	               }

	               if ("3".equals(tctb)) {
	                  hItem.put("TCTBao", "Thay thế");
	               }

	               if ("4".equals(tctb)) {
	                  hItem.put("TCTBao", "Giải trình");
	               }

	         
	               
	               hItem.put("LDo", this.commons.getTextFromNodeXML((Element)xPath.evaluate("LDo", nodeTmp, XPathConstants.NODE)));
	               arrayData.add(hItem);
	               ++countPrd;
	               if (startRowGroup >= numberRowInPageMultiPage) {
	                  startRowGroup = 0;
	                  ++groupPageIDX;
	               }
	            }

	            if (0 == startRowGroup && nodeListHHDVu.getLength() > 0) {
	               --groupPageIDX;
	            }

	            if (0 == startRowGroup) {
	               ++groupPageIDX;

	               for(i = 0; i < numberRowInPage; ++i) {
	                  hItem = new HashMap();
	                  hItem.put("ProdName", " ");
	                  hItem.put("GroupPageIDX", groupPageIDX);
	                  arrayData.add(hItem);
	               }
	            } else if (startRowGroup <= numberRowInPage) {
	               for(i = startRowGroup; i < numberRowInPage; ++i) {
	                  hItem = new HashMap();
	                  hItem.put("ProdName", " ");
	                  hItem.put("GroupPageIDX", groupPageIDX);
	                  arrayData.add(hItem);
	               }
	            } else {
	               for(i = startRowGroup; i < numberRowInPageMultiPage; ++i) {
	                  hItem = new HashMap();
	                  hItem.put("ProdName", " ");
	                  hItem.put("GroupPageIDX", groupPageIDX);
	                  arrayData.add(hItem);
	               }

	               ++groupPageIDX;

	               for(i = 0; i < numberRowInPage; ++i) {
	                  hItem = new HashMap();
	                  hItem.put("ProdName", " ");
	                  hItem.put("GroupPageIDX", groupPageIDX);
	                  arrayData.add(hItem);
	               }
	            }
	         } else {
	            for(i = 0; i < nodeListHHDVu.getLength(); ++i) {
	               nodeTmp = nodeListHHDVu.item(i);
	               productName = this.commons.getTextFromNodeXML((Element)xPath.evaluate("MCQTCap", nodeTmp, XPathConstants.NODE));
	               lenProName = productName.getBytes(StandardCharsets.UTF_8).length;
	               startRowGroup = (int)((double)startRowGroup + Math.ceil((double)lenProName / (double)numberCharsInRow));
	               hItem = new HashMap();
	               hItem.put("GroupPageIDX", groupPageIDX);
	               hItem = new HashMap();
	               hItem.put("GroupPageIDX", groupPageIDX);
	               hItem.put("STT", this.commons.getTextFromNodeXML((Element)xPath.evaluate("STT", nodeTmp, XPathConstants.NODE)));
	               hItem.put("MCQTCap", productName);
	               hItem.put("MSHDon", this.commons.getTextFromNodeXML((Element)xPath.evaluate("KHMSHDon", nodeTmp, XPathConstants.NODE)) + this.commons.getTextFromNodeXML((Element)xPath.evaluate("KHHDon", nodeTmp, XPathConstants.NODE)));
	               hItem.put("SHDon", this.commons.getTextFromNodeXML((Element)xPath.evaluate("SHDon", nodeTmp, XPathConstants.NODE)));
	               ngayc = this.commons.getTextFromNodeXML((Element)xPath.evaluate("NLap", nodeTmp, XPathConstants.NODE));
	               String ngayky = (String) docTmp.get("NTBao");
	               
	               words = ngayc.split("-");
	               nam = words[0];
	               thang = words[1];
	               ngay = words[2];
	               ngayview = ngay + "-" + thang + "-" + nam;
	               
	               words.clone();
	               nam = "";
	               thang = "";
	               ngay ="";
	               words = ngayky.split("-");
	               nam = words[0];
	               thang = words[1];
	               ngay = words[2];
	               String  ngaykyview = ngay + "-" + thang + "-" + nam;
	               
	               hItem.put("NLHDon", ngayview);
	               hItem.put("SignDate", ngayview);
	           	reportParams.put("SignDate", ngaykyview);
	           	
	            reportParams.put("InvoiceDate",ngay);
	  	         reportParams.put("InvoiceMonth", thang);
	  	         reportParams.put("InvoiceYear", nam);
	  	    
	           	
	  	       TTTNCQT = this.commons.getTextFromNodeXML((Element)xPath.evaluate("TTTNCCQT", nodeTmp, XPathConstants.NODE));
	               if ("1".equals(TTTNCQT)) {
	                  hItem.put("TTTNCQT", "Tiếp nhận");
	                  CQTTNhan++;
	               }

	               if ("2".equals(TTTNCQT)) {
	                  hItem.put("TTTNCQT", "Không tiếp nhận");
	               }

	               tctb = this.commons.getTextFromNodeXML((Element)xPath.evaluate("TCTBao", nodeTmp, XPathConstants.NODE));
	               if ("1".equals(tctb)) {
	                  hItem.put("TCTBao", "Hủy");
	               }

	               if ("2".equals(tctb)) {
	                  hItem.put("TCTBao", "Điều chỉnh");
	               }

	               if ("3".equals(tctb)) {
	                  hItem.put("TCTBao", "Thay thế");
	               }

	               if ("4".equals(tctb)) {
	                  hItem.put("TCTBao", "Giải trình");
	               }

//	               hItem.put("LDo", this.commons.getTextFromNodeXML((Element)xPath.evaluate("LDo", nodeTmp, XPathConstants.NODE)));
	              
	               arrayData.add(hItem);
	               ++countPrd;
	               if (startRowGroup >= numberRowInPage) {
	                  startRowGroup = 0;
	                  ++groupPageIDX;
	               }
	            }

	            if (0 == startRowGroup && nodeListHHDVu.getLength() > 0) {
	               --groupPageIDX;
	            }

	            if (startRowGroup > 0 || nodeListHHDVu.getLength() == 0) {
	               for(i = startRowGroup; i < numberRowInPage; ++i) {
	                  hItem = new HashMap();
	                  hItem.put("ProdName", " ");
	                  hItem.put("GroupPageIDX", groupPageIDX);
	                  arrayData.add(hItem);
	               }
	            }
	         }
	      }
	      
	      String CQTTNhanSoHD = String.valueOf(CQTTNhan);
	      
	      reportParams.put("CQTTNhanSoHD",  CQTTNhanSoHD); 

	      reportParams.put("TOTAL_PAGE", groupPageIDX);
	      JRDataSource jds = null;
	      jds = new JRBeanCollectionDataSource(arrayData);
	      ByteArrayOutputStream out = new ByteArrayOutputStream();
	      JasperReport jr = JasperCompileManager.compileReport(new FileInputStream(fileJP));
	      JasperPrint jp = JasperFillManager.fillReport(jr, reportParams, jds);
	      thang = null;
	      Exporter exporter = new JRPdfExporter();
	      exporter.setExporterInput(new SimpleExporterInput(jp));
	      exporter.setExporterOutput(new SimpleOutputStreamExporterOutput(out));
	      SimplePdfExporterConfiguration configuration = new SimplePdfExporterConfiguration();
	      configuration.setCreatingBatchModeBookmarks(true);
	      exporter.setConfiguration(configuration);
	      exporter.exportReport();
	      return out;
	   }



		public ByteArrayOutputStream createFinalInvoiceDB(File fileJP, org.bson.Document doc, String CheckView
				, int numberRowInPage, int numberRowInPageMultiPage, int numberCharsInRow
				, String MST,String link, String pathLogo, String pathBackground,String pathQA, String pathVien
				, boolean isConvert, boolean isDeleted
				, boolean isThayThe, boolean isDieuChinh
			) throws Exception{
				Map<String, Object> reportParams = new HashMap<String, Object>();
				 String Checknamecd = "";
				 
				
				/*KIEM TRA FILE LOG & BACKGROUND CO TON TAI KHONG*/
				File f = new File(pathLogo);
				if(f.exists() && f.isFile())
					reportParams.put("URL_IMG_LOGO", pathLogo);
				else
					reportParams.put("URL_IMG_LOGO", Paths.get(SystemParams.DIR_E_INVOICE_TEMPLATE, IMAGE_FILENAME_BLANK).toString());
				
				f = new File(pathBackground);
				if(f.exists() && f.isFile())
					reportParams.put("URL_IMG_BACKGROUND", pathBackground);
				else
					reportParams.put("URL_IMG_BACKGROUND", Paths.get(SystemParams.DIR_E_INVOICE_TEMPLATE, IMAGE_FILENAME_BLANK).toString());
			
				f = new File(pathQA);
				if(f.exists() && f.isFile())
					reportParams.put("URL_QR_INFO", pathQA);
				else
					reportParams.put("URL_QR_INFO", Paths.get(SystemParams.DIR_E_INVOICE_TEMPLATE, IMAGE_FILENAME_BLANK).toString());
			
				f = new File(pathVien);
				if(f.exists() && f.isFile())
					reportParams.put("URL_IMG_FRAME", pathVien);
				else
					reportParams.put("URL_IMG_FRAME", Paths.get(SystemParams.DIR_E_INVOICE_TEMPLATE, IMAGE_FILENAME_BLANK).toString());
			
				reportParams.put("IsConvert", isConvert);

				
				//LIST EINVOICE
				
				String NLap = "";
				NLap = commons.convertLocalDateTimeToString(commons.convertDateToLocalDateTime(doc.getEmbedded(Arrays.asList("EInvoiceDetail", "TTChung", "NLap"), Date.class)),"yyyy-MM-dd");
				
				String KDLieu = "";				
				int countPrd = 0;							
				LocalDate localDateNLap = null;
				if(!"".equals(NLap)) {
					try {
						localDateNLap = commons.convertStringToLocalDate(NLap, "yyyy-MM-dd");
					}catch(Exception e) {}
				}
				
				if(null != localDateNLap) {
					reportParams.put("InvoiceDate", StringUtils.leftPad(String.valueOf(localDateNLap.get(ChronoField.DAY_OF_MONTH)), 2, "0"));
					reportParams.put("InvoiceMonth", StringUtils.leftPad(String.valueOf(localDateNLap.get(ChronoField.MONTH_OF_YEAR)), 2, "0"));
					reportParams.put("InvoiceYear", StringUtils.leftPad(String.valueOf(localDateNLap.get(ChronoField.YEAR)), 4, "0"));
				}
								
				String KHMSHDon = doc.getEmbedded(Arrays.asList("EInvoiceDetail", "TTChung", "KHMSHDon"), "");
				String KHHDon = doc.getEmbedded(Arrays.asList("EInvoiceDetail", "TTChung", "KHHDon"), "");
				reportParams.put("KHHDon", KHMSHDon + KHHDon);
				
				String SHDon =  doc.getEmbedded(Arrays.asList("EInvoiceDetail", "TTChung", "SHDon"), 0).toString();
				
				LocalDate localDateCD= null;
				localDateCD = commons.convertStringToLocalDate(NLap, "yyyy-MM-dd");
					reportParams.put("InvoiceCovertDate", StringUtils.leftPad(String.valueOf(localDateCD.get(ChronoField.DAY_OF_MONTH)), 2, "0"));
					reportParams.put("InvoiceCovertMonth", StringUtils.leftPad(String.valueOf(localDateCD.get(ChronoField.MONTH_OF_YEAR)), 2, "0"));
					reportParams.put("InvoiceCovertYear", StringUtils.leftPad(String.valueOf(localDateCD.get(ChronoField.YEAR)), 4, "0"));
					try {
						String[] words = CheckView.split(",");
						  CheckView = words[0];
						  Checknamecd =  words[1];
					
					} catch (Exception e) {
						// TODO: handle exception
					}
			
					if(!Checknamecd.equals("")) {
						reportParams.put("SignNameCovert",Checknamecd);			
					}
					if(CheckView.equals("Y"))
					{
						reportParams.put("SHDon", SHDon);
						reportParams.put("SHDonOrigin",SHDon);								
					}
					else {
						reportParams.put("SHDon",commons.addSpacingAfterLeter(commons.formatNumberBillInvoice(SHDon)));
						reportParams.put("SHDonOrigin",commons.addSpacingAfterLeter(commons.formatNumberBillInvoice(SHDon)));
					}
				
				
				reportParams.put("MCCQT", doc.get("MCCQT", ""));
				
				reportParams.put("SignName", doc.getEmbedded(Arrays.asList("EInvoiceDetail", "NDHDon", "NBan", "Ten"), ""));
				reportParams.put("NBanTen", doc.getEmbedded(Arrays.asList("EInvoiceDetail", "NDHDon", "NBan", "Ten"), ""));
				reportParams.put("NBanMST", doc.getEmbedded(Arrays.asList("EInvoiceDetail", "NDHDon", "NBan", "MST"), ""));
				reportParams.put("NBanDChi", doc.getEmbedded(Arrays.asList("EInvoiceDetail", "NDHDon", "NBan", "DChi"), ""));
				reportParams.put("NBanSDThoai", doc.getEmbedded(Arrays.asList("EInvoiceDetail", "NDHDon", "NBan", "SDThoai"), ""));
				reportParams.put("NBanDCTDTu", doc.getEmbedded(Arrays.asList("EInvoiceDetail", "NDHDon", "NBan", "DCTDTu"), ""));
				reportParams.put("NBanSTKNHang", doc.getEmbedded(Arrays.asList("EInvoiceDetail", "NDHDon", "NBan", "STKNHang"), ""));
				reportParams.put("NBanTNHang", doc.getEmbedded(Arrays.asList("EInvoiceDetail", "NDHDon", "NBan", "TNHang"), ""));
				reportParams.put("NBanFax", doc.getEmbedded(Arrays.asList("EInvoiceDetail", "NDHDon", "NBan", "Fax"), ""));
				reportParams.put("NBanWebsite", doc.getEmbedded(Arrays.asList("EInvoiceDetail", "NDHDon", "NBan", "Website"), ""));
				
				
				reportParams.put("NMuaTen", doc.getEmbedded(Arrays.asList("EInvoiceDetail", "NDHDon", "NMua", "Ten"), ""));
				reportParams.put("NMuaMST", doc.getEmbedded(Arrays.asList("EInvoiceDetail", "NDHDon", "NMua", "MST"), ""));
				reportParams.put("NMuaDChi", doc.getEmbedded(Arrays.asList("EInvoiceDetail", "NDHDon", "NMua", "DChi"), ""));
				reportParams.put("NMuaMKHang", doc.getEmbedded(Arrays.asList("EInvoiceDetail", "NDHDon", "NMua", "MKHang"), ""));
				reportParams.put("NMuaSDThoai", doc.getEmbedded(Arrays.asList("EInvoiceDetail", "NDHDon", "NMua", "SDThoai"), ""));
				reportParams.put("NMuaDCTDTu",doc.getEmbedded(Arrays.asList("EInvoiceDetail", "NDHDon", "NMua", "DCTDTu"), ""));
				reportParams.put("NMuaHVTNMHang", doc.getEmbedded(Arrays.asList("EInvoiceDetail", "NDHDon", "NMua", "HVTNMHang"), ""));
				reportParams.put("NMuaSTKNHang", doc.getEmbedded(Arrays.asList("EInvoiceDetail", "NDHDon", "NMua", "STKNHang"), ""));
				reportParams.put("NMuaTNHang", doc.getEmbedded(Arrays.asList("EInvoiceDetail", "NDHDon", "NMua", "TNHang"), ""));
								
				reportParams.put("TToanTGia", commons.ToNumber(doc.getEmbedded(Arrays.asList("EInvoiceDetail", "TTChung", "TGia"), "")));
				reportParams.put("TTChungHTTToan", doc.getEmbedded(Arrays.asList("EInvoiceDetail", "TTChung", "HTTToan"), ""));
				
				String TCHDon = doc.getEmbedded(Arrays.asList("EInvoiceDetail", "TTChung", "TTHDLQuan", "TCHDon"), "");
				String noticeTTDC = "";
				switch (TCHDon) {
				case "1":
					if(CheckView.equals("Y"))
					{
						noticeTTDC = String.format("(Thay thế cho hóa đơn Mẫu số %s, Ký hiệu %s, số %s, ngày %s)", 
								
								doc.getEmbedded(Arrays.asList("EInvoiceDetail", "TTChung", "TTHDLQuan", "KHMSHDCLQuan"), ""),
								doc.getEmbedded(Arrays.asList("EInvoiceDetail", "TTChung", "TTHDLQuan", "KHHDCLQuan"), ""),
								doc.getEmbedded(Arrays.asList("EInvoiceDetail", "TTChung", "TTHDLQuan", "SHDCLQuan"), ""),
								doc.getEmbedded(Arrays.asList("EInvoiceDetail", "TTChung", "TTHDLQuan", "NLHDCLQuan"), "")
							);
					}
					else {
						noticeTTDC = String.format("(Thay thế cho hóa đơn Mẫu số %s, Ký hiệu %s, số %s, ngày %s)", 
								doc.getEmbedded(Arrays.asList("EInvoiceDetail", "TTChung", "TTHDLQuan", "KHMSHDCLQuan"), ""),
								doc.getEmbedded(Arrays.asList("EInvoiceDetail", "TTChung", "TTHDLQuan", "KHHDCLQuan"), ""),
								commons.formatNumberBillInvoice(doc.getEmbedded(Arrays.asList("EInvoiceDetail", "TTChung", "TTHDLQuan", "SHDCLQuan"), "")),
								doc.getEmbedded(Arrays.asList("EInvoiceDetail", "TTChung", "TTHDLQuan", "NLHDCLQuan"), "")							);
					}
					
					break;
				case "2":
					if(CheckView.equals("Y"))
					{
						noticeTTDC = String.format("(Điều chỉnh cho hóa đơn Mẫu số %s, Ký hiệu %s, số %s, ngày %s)", 
								doc.getEmbedded(Arrays.asList("EInvoiceDetail", "TTChung", "TTHDLQuan", "KHMSHDCLQuan"), ""),
								doc.getEmbedded(Arrays.asList("EInvoiceDetail", "TTChung", "TTHDLQuan", "KHHDCLQuan"), ""),
								doc.getEmbedded(Arrays.asList("EInvoiceDetail", "TTChung", "TTHDLQuan", "SHDCLQuan"), ""),
								doc.getEmbedded(Arrays.asList("EInvoiceDetail", "TTChung", "TTHDLQuan", "NLHDCLQuan"), "")
							);
					}
					else {
						noticeTTDC = String.format("(Điều chỉnh cho hóa đơn Mẫu số %s, Ký hiệu %s, số %s, ngày %s)", 
								doc.getEmbedded(Arrays.asList("EInvoiceDetail", "TTChung", "TTHDLQuan", "KHMSHDCLQuan"), ""),
								doc.getEmbedded(Arrays.asList("EInvoiceDetail", "TTChung", "TTHDLQuan", "KHHDCLQuan"), ""),
								commons.formatNumberBillInvoice(doc.getEmbedded(Arrays.asList("EInvoiceDetail", "TTChung", "TTHDLQuan", "SHDCLQuan"), "")),
								doc.getEmbedded(Arrays.asList("EInvoiceDetail", "TTChung", "TTHDLQuan", "NLHDCLQuan"), "")
							);
					}
					
					break;
				default:
					break;
				}
				reportParams.put("NoticeTTDC", noticeTTDC);
								
				String TgTCThue =  doc.getEmbedded(Arrays.asList("EInvoiceDetail", "TToan", "TgTCThue"), 0.0).toString();
				String TgTThue = doc.getEmbedded(Arrays.asList("EInvoiceDetail", "TToan", "TgTThue"), 0.0).toString();
				String TgTTTBSo = doc.getEmbedded(Arrays.asList("EInvoiceDetail", "TToan", "TgTTTBSo"), 0.0).toString();
				
				reportParams.put("TTChungDVTTe", doc.getEmbedded(Arrays.asList("EInvoiceDetail", "TTChung", "DVTTe"), ""));
				reportParams.put("TToanTgTCThue", commons.ToNumber(TgTCThue));
				reportParams.put("TToanTgTThue", commons.ToNumber(TgTThue));
				reportParams.put("TToanTgTTTBSo", commons.ToNumber(TgTTTBSo));
			
				//TIEN QUY DOI
				Double tienQuyDoi = doc.getEmbedded(Arrays.asList("EInvoiceDetail", "TToan", "TgTQDoi"), 0.0);
				if(tienQuyDoi > 0.0) {
					reportParams.put("TToanTgTTTQDoi", tienQuyDoi);
				}
				reportParams.put("TToanTgTTTBChu", 
						doc.getEmbedded(Arrays.asList("EInvoiceDetail", "TToan", "TgTTTBChu"), "")
				);
								
				reportParams.put("SecureKey", doc.get("SecureKey", ""));
				reportParams.put("SystemKey", doc.get("_id", ObjectId.class).toString());
				
				/*KIEM TRA THONG TIN NGUOI BAN KY CHUA*/
		
				String SignStatusCode = doc.get("SignStatusCode", "");
				String SignName = doc.getEmbedded(Arrays.asList("EInvoiceDetail", "NDHDon", "NBan", "Ten"), "");
				
				LocalDateTime ldt = null;
				if(SignStatusCode.equals("NOSIGN")) {
					reportParams.put("IsSigned", false);
					reportParams.put("SignDesc", "Chưa ký");
					reportParams.put("UrlImageVerify", Paths.get(SystemParams.DIR_E_INVOICE_TEMPLATE, Constants.TEMPLATE_FILE_NAME.IMG_SIGNATURE_INVALID).toString());
				}else {
					
					String SignDate = commons.convertLocalDateTimeToString(commons.convertDateToLocalDateTime(doc.getEmbedded(Arrays.asList("InfoSigned", "SignedDate"), Date.class)),"dd/MM/yyyy");
			
					reportParams.put("IsSigned", true);
					reportParams.put("SignDesc", "Đã ký");
					reportParams.put("SignName", SignName);			
					reportParams.put("SignDate", SignDate);		
					reportParams.put("UrlImageVerify", Paths.get(SystemParams.DIR_E_INVOICE_TEMPLATE, Constants.TEMPLATE_FILE_NAME.IMG_SIGNATURE_VALID).toString());
				}
				
				
				reportParams.put("UrlImageInvDeleted", Paths.get(SystemParams.DIR_E_INVOICE_TEMPLATE, Constants.TEMPLATE_FILE_NAME.IMG_INV_DELETED).toString());
				reportParams.put("IsDeleted", isDeleted);
				reportParams.put("UrlImageInvThayThe", Paths.get(SystemParams.DIR_E_INVOICE_TEMPLATE, Constants.TEMPLATE_FILE_NAME.IMG_INV_THAYTHE).toString());
				reportParams.put("UrlImageInvDieuChinh", Paths.get(SystemParams.DIR_E_INVOICE_TEMPLATE, Constants.TEMPLATE_FILE_NAME.IMG_INV_DIEUCHINH).toString());
				reportParams.put("IsThayThe", isThayThe);
				reportParams.put("IsDieuChinh", isDieuChinh);
				List<HashMap<String, Object>> arrayData = new ArrayList<>();
				HashMap<String, Object> hItem = null;
				
				int lenProName = 0;
				String productName = "";
				String sLo = "";
				String hanSD = "";
				int startRowGroup = 0;
				
				/*XEM CO IN NHIEU DONG KHONG*/
				int groupPageIDX = 1;
				boolean isUsingMultiPage = false;
				org.bson.Document docEInvoiceDetail = null;
				docEInvoiceDetail = doc.get("EInvoiceDetail", org.bson.Document.class);
				if(docEInvoiceDetail.get("DSHHDVu") != null && docEInvoiceDetail.getList("DSHHDVu", org.bson.Document.class).size() > 0) {
					for(org.bson.Document o: docEInvoiceDetail.getList("DSHHDVu", org.bson.Document.class)) {
						productName= o.get("ProductName", "");
						lenProName = productName.getBytes(StandardCharsets.UTF_8).length;
						startRowGroup += Math.ceil((double) lenProName / numberCharsInRow);
						
						if(startRowGroup >= numberRowInPage) {
							isUsingMultiPage = true;
							break;
						}
					}
				}
				
				/*LAY DANH SACH LAI SUAT*/
				HashMap<String, Double> mapVATAmount = null;
				HashMap<String, Double> mapAmount = null;
				mapVATAmount = new LinkedHashMap<String, Double>();
				mapAmount = new LinkedHashMap<String, Double>();
				String tmp= "";
				if(docEInvoiceDetail.get("DSHHDVu") != null && docEInvoiceDetail.getList("DSHHDVu", org.bson.Document.class).size() > 0) {
					for(org.bson.Document o: docEInvoiceDetail.getList("DSHHDVu", org.bson.Document.class)) {
						productName= o.get("ProductName", "");
						if(!productName.equals("")) {
							tmp = commons.formatNumberReal(o.get("VATRate", 0.0).toString());
							
							switch (tmp) {
							case "0":
								tmp = "-1";
								break;
							case "1":
								tmp = "0";
								break;
							case "5":
							case "8":
							case "10":								
								break;
							case "-1":
								tmp = "KCT";
								break;
							case "-2":
								tmp = "KKKNT";
								break;
							default:
								break;
							}
							
							if ("1".equals(o.get("Feature")) || "3".equals(o.get("Feature")) || "4".equals(o.get("Feature"))) {
								mapAmount.compute(tmp, (k, v) -> {
									return (v == null ? o.get("Total", 0.0) * ("3".equals(o.get("Feature")) ? -1 : 1): v + o.get("Total", 0.0) * ("3".equals(o.get("Feature")) ? -1 : 1));
								});
								mapVATAmount.compute(tmp, (k, v) -> {
									return (v == null ? o.get("VATAmount", 0.0) * ("3".equals(o.get("Feature")) ? -1 : 1) : v + o.get("VATAmount", 0.0) * ("3".equals(o.get("Feature")) ? -1 : 1));
								});
							}
						}
					}
				}
				
				String check_default = "";
				String DVTTe = doc.getEmbedded(Arrays.asList("EInvoiceDetail", "TTChung", "DVTTe"),  ""); 
				
				for (Map.Entry<String, Double> pair : mapVATAmount.entrySet()) {
					if (null != pair.getKey() && !"".equals(pair.getKey())) {
						if(!pair.getKey().equals("-1")) {

							String Tax_ThTien = "Tax_ThTien_" + pair.getKey();
							reportParams.put(Tax_ThTien, mapAmount.get(pair.getKey()));
																									
							String Tax_TSuat = "Tax_TSuat_" + pair.getKey();
							reportParams.put(Tax_TSuat, pair.getKey());
																		
							String Tax_TThue = "Tax_TThue_" + pair.getKey();																					
							
							if(DVTTe.equals("VND")) {	
								String TThue = String.format("%.0f", pair.getValue());
								Double TThue_double = Double.valueOf(TThue);
								reportParams.put(Tax_TThue, TThue_double);
								
							}else {
								String TThue = String.format("%.2f", pair.getValue());
								Double TThue_double = Double.valueOf(TThue);
								reportParams.put(Tax_TThue, TThue_double);								
							}
							
							if(reportParams.get("Tax_TSuat_Default") == null) {		
								reportParams.put("Tax_ThTien_Default", mapAmount.get(pair.getKey()));
								
								if(DVTTe.equals("VND")) {	
									
									String TThue = String.format("%.0f", pair.getValue());
									Double TThue_double = Double.valueOf(TThue);			
									reportParams.put("Tax_TThue_Default", TThue_double);
									
								}else {

									String TThue = String.format("%.2f", pair.getValue());
									Double TThue_double = Double.valueOf(TThue);		
									reportParams.put("Tax_TThue_Default", TThue_double);								
								}
				
								reportParams.put("Tax_TSuat_Default", pair.getKey());
								check_default = pair.getKey();
							}
							
							if(reportParams.get("Tax_TSuat_Default") != null) {	
								if(check_default.equals("0")) {							
								reportParams.put("Tax_TSuat_Default", check_default);
								reportParams.put("Tax_ThTien_Default", mapAmount.get(pair.getKey()));
							}
							}
							
					}
				}
			}
				
				
				/*END - LAY DANH SACH LAI SUAT*/

				/*END - XEM CO IN NHIEU DONG KHONG*/			
				startRowGroup = 0;
				countPrd = 0;
				if(docEInvoiceDetail.getList("DSHHDVu", org.bson.Document.class).size() > 0) {
					if(isUsingMultiPage) {
							for(org.bson.Document o: docEInvoiceDetail.getList("DSHHDVu", org.bson.Document.class)) {
							
							productName =  o.get("ProductName", "");
							sLo =  o.get("SLo", "");
							hanSD =  o.get("HanSD", "");
							lenProName = productName.getBytes(StandardCharsets.UTF_8).length;
							startRowGroup += Math.ceil((double) lenProName / numberCharsInRow);
							
							hItem = new HashMap<>();
							hItem.put("GroupPageIDX", groupPageIDX);							
							hItem.put("TChat", o.get("Feature", ""));
							hItem.put("STT", o.get("STT", ""));
							hItem.put("MHHDVu", o.get("ProductCode", ""));		
							hItem.put("THHDVu", productName);
							
							if(!"".equals(sLo)){
								hItem.put("SLo", sLo);
							}
							if(!"".equals(hanSD)){
								hItem.put("HanSD", hanSD);
							}
							
							hItem.put("DVTinh",  o.get("Unit", ""));		
							hItem.put("SLuong", o.get("Quantity", Double.class));
							hItem.put("DGia", o.get("Price", Double.class));
							hItem.put("ThTien", o.get("Total", Double.class));
							hItem.put("TSuat",commons.formatNumberReal(o.get("VATRate", 0.0).toString()) + "%"); 
							hItem.put("VATAmount", o.get("VATAmount", Double.class));
							hItem.put("Amount", o.get("Amount", Double.class)); 							
							
							arrayData.add(hItem);
							countPrd++;	
							
							if(startRowGroup >= numberRowInPageMultiPage) {
								startRowGroup = 0;
								groupPageIDX++;
							}
						}
						if(0 == startRowGroup && docEInvoiceDetail.getList("DSHHDVu", org.bson.Document.class).size() > 0) groupPageIDX--;		//BO BOT TRANG CUOI CUNG VI KHONG CO DU LIEU
						
						if(0 == startRowGroup) {
							groupPageIDX++;
							for(int i = 0; i < numberRowInPage; i++) {
								hItem = new HashMap<>();
								hItem.put("ProdName", " ");
								hItem.put("GroupPageIDX", groupPageIDX);
								arrayData.add(hItem);
							}
						}else if(startRowGroup <= numberRowInPage) {
							for(int i = startRowGroup; i < numberRowInPage; i++) {
								hItem = new HashMap<>();
								hItem.put("ProdName", " ");
								hItem.put("GroupPageIDX", groupPageIDX);
								arrayData.add(hItem);
							}
						}else {
							for(int i = startRowGroup; i < numberRowInPageMultiPage; i++) {
								hItem = new HashMap<>();
								hItem.put("ProdName", " ");
								hItem.put("GroupPageIDX", groupPageIDX);
								arrayData.add(hItem);
							}
							groupPageIDX++;
							for(int i = 0; i < numberRowInPage; i++) {
								hItem = new HashMap<>();
								hItem.put("ProdName", " ");
								hItem.put("GroupPageIDX", groupPageIDX);
								arrayData.add(hItem);
							}
						}
					}else {
						for(org.bson.Document o: docEInvoiceDetail.getList("DSHHDVu", org.bson.Document.class)) {
							
							productName =  o.get("ProductName", "");
							sLo =  o.get("SLo", "");
							hanSD =  o.get("HanSD", "");
							lenProName = productName.getBytes(StandardCharsets.UTF_8).length;
							startRowGroup += Math.ceil((double) lenProName / numberCharsInRow);
													
							hItem = new HashMap<>();
							hItem.put("GroupPageIDX", groupPageIDX);							
							hItem.put("TChat", o.get("Feature", ""));
							hItem.put("STT", o.get("STT", ""));
							hItem.put("MHHDVu", o.get("ProductCode", ""));		
							hItem.put("THHDVu", productName);
							
							if(!"".equals(sLo)){
								hItem.put("SLo", sLo);
							}
							if(!"".equals(hanSD)){
								hItem.put("HanSD", hanSD);
							}
							
							hItem.put("DVTinh",  o.get("Unit", ""));		
							hItem.put("SLuong", o.get("Quantity", Double.class));
							hItem.put("DGia", o.get("Price", Double.class));
							hItem.put("ThTien", o.get("Total", Double.class));
							hItem.put("TSuat",commons.formatNumberReal(o.get("VATRate", 0.0).toString()) + "%"); 
							
							hItem.put("VATAmount", o.get("VATAmount", Double.class));
							hItem.put("Amount", o.get("Amount", Double.class)); 
														
							arrayData.add(hItem);
							countPrd++;	
							
							if(startRowGroup >= numberRowInPage) {
								startRowGroup = 0;
								groupPageIDX++;
							}
						}
						if(0 == startRowGroup && docEInvoiceDetail.getList("DSHHDVu", org.bson.Document.class).size() > 0) groupPageIDX--;		//BO BOT TRANG CUOI CUNG VI KHONG CO DU LIEU
						
						/*ADD THEM DU LIEU CHO DU TRANG*/
						if(startRowGroup > 0 || docEInvoiceDetail.getList("DSHHDVu", org.bson.Document.class).size() == 0) {
							for(int i = startRowGroup; i < numberRowInPage; i++) {
								hItem = new HashMap<>();
								hItem.put("ProdName", " ");
								hItem.put("GroupPageIDX", groupPageIDX);
								arrayData.add(hItem);
							}
						}
					}
					
				}		
				reportParams.put("PortalLink", link);
				
				reportParams.put("TOTAL_PAGE", (int) groupPageIDX);
				
				JRDataSource jds = null;
				jds = new JRBeanCollectionDataSource(arrayData);
				
				ByteArrayOutputStream out = new ByteArrayOutputStream();
				JasperReport jr = JasperCompileManager.compileReport(new FileInputStream(fileJP));
				JasperPrint jp = JasperFillManager.fillReport(jr, reportParams, jds);
				Exporter exporter = null;
				
				exporter = new JRPdfExporter();
				exporter.setExporterInput(new SimpleExporterInput(jp));
				exporter.setExporterOutput(new SimpleOutputStreamExporterOutput(out));
		        SimplePdfExporterConfiguration configuration = new SimplePdfExporterConfiguration();
		        configuration.setCreatingBatchModeBookmarks(true);
		        exporter.setConfiguration(configuration);
		        exporter.exportReport();
		        return out;
			}

		//PRINT EINVOICE MTT
		public ByteArrayOutputStream createFinalInvoiceMTT(File fileJP, Document doc, String CheckView,String link, String einvoiceStatus, String signStatusCode
				, int numberRowInPage, int numberRowInPageMultiPage, int numberCharsInRow
				, String MST, String pathLogo, String pathBackground,String pathQA, String pathVien
				, boolean isConvert, boolean isDeleted
				, boolean isThayThe, boolean isDieuChinh
			) throws Exception{
				Map<String, Object> reportParams = new HashMap<String, Object>();
				 String Checknamecd = "";
				
				
				/*KIEM TRA FILE LOG & BACKGROUND CO TON TAI KHONG*/
				File f = new File(pathLogo);
				if(f.exists() && f.isFile())
					reportParams.put("URL_IMG_LOGO", pathLogo);
				else
					reportParams.put("URL_IMG_LOGO", Paths.get(SystemParams.DIR_E_INVOICE_TEMPLATE, IMAGE_FILENAME_BLANK).toString());
				
				f = new File(pathBackground);
				if(f.exists() && f.isFile())
					reportParams.put("URL_IMG_BACKGROUND", pathBackground);
				else
					reportParams.put("URL_IMG_BACKGROUND", Paths.get(SystemParams.DIR_E_INVOICE_TEMPLATE, IMAGE_FILENAME_BLANK).toString());
			
				f = new File(pathQA);
				if(f.exists() && f.isFile())
					reportParams.put("URL_QR_INFO", pathQA);
				else
					reportParams.put("URL_QR_INFO", Paths.get(SystemParams.DIR_E_INVOICE_TEMPLATE, IMAGE_FILENAME_BLANK).toString());
			
				f = new File(pathVien);
				if(f.exists() && f.isFile())
					reportParams.put("URL_IMG_FRAME", pathVien);
				else
					reportParams.put("URL_IMG_FRAME", Paths.get(SystemParams.DIR_E_INVOICE_TEMPLATE, IMAGE_FILENAME_BLANK).toString());
			
			
				
				
				reportParams.put("IsConvert", isConvert);
//				if(isConvert) {
//					reportParams.put("IsConvert", isConvert);
//					reportParams.put("URL_IMG_CONVERT", Paths.get(SystemParams.DIR_E_INVOICE_TEMPLATE, IMAGE_FILENAME_INV_CONVERT).toString());
//				}

				String NLap = "";
				String KDLieu = "";
				Node nodeTmp = null;
				Node nodeSubTmp = null;
				NodeList nodeListTTKhac = null;
				int countPrd = 0;
				
				XPath xPath = XPathFactory.newInstance().newXPath();
//				Node nodeHDon = (Node) xPath.evaluate("/KetQuaTraCuu/DuLieu/TDiep[last()]/DLieu/HDon", doc, XPathConstants.NODE);
				Node nodeHDon = null;
				Node nodeHDonTDiep = null;
				for(int i = 1; i<= 20; i++) {
					nodeHDon = (Node) xPath.evaluate("/KetQuaTraCuu/DuLieu/TDiep[" + i + "]/DLieu/HDon", doc, XPathConstants.NODE);
					nodeHDonTDiep = (Node) xPath.evaluate("/TDiep", doc, XPathConstants.NODE);
					if(nodeHDon != null) break;
				}
				
				
				
				/*FILE CHUA DUOC DUI DEN CO QUAN THUE*/
				if(null == nodeHDon) {
					nodeHDon = (Node) xPath.evaluate("/TDiep/DLieu/HDon", doc, XPathConstants.NODE);
					nodeHDonTDiep = (Node) xPath.evaluate("/TDiep", doc, XPathConstants.NODE);
				}
				if(null == nodeHDon) {
					nodeHDon = (Node) xPath.evaluate("/HDon", doc, XPathConstants.NODE);
					nodeHDonTDiep = (Node) xPath.evaluate("/TDiep", doc, XPathConstants.NODE);
				}
				Node nodeTTChung = (Node) xPath.evaluate("DLHDon/TTChung", nodeHDon, XPathConstants.NODE);
				Node nodeNBan = (Node) xPath.evaluate("DLHDon/NDHDon/NBan", nodeHDon, XPathConstants.NODE);
				Node nodeNMua = (Node) xPath.evaluate("DLHDon/NDHDon/NMua", nodeHDon, XPathConstants.NODE);
				Node nodeTToan = (Node) xPath.evaluate("DLHDon/NDHDon/TToan", nodeHDon, XPathConstants.NODE);
				NodeList nodeListHHDVu = (NodeList) xPath.evaluate("DLHDon/NDHDon/DSHHDVu/HHDVu", nodeHDon, XPathConstants.NODESET);
				
				LocalDate localDateNLap = null;
				NLap = commons.getTextFromNodeXML((Element) xPath.evaluate("NLap", nodeTTChung, XPathConstants.NODE));
				if(!"".equals(NLap)) {
					try {
						localDateNLap = commons.convertStringToLocalDate(NLap, "yyyy-MM-dd");
					}catch(Exception e) {}
				}
				
				if(null != localDateNLap) {
					reportParams.put("InvoiceDate", StringUtils.leftPad(String.valueOf(localDateNLap.get(ChronoField.DAY_OF_MONTH)), 2, "0"));
					reportParams.put("InvoiceMonth", StringUtils.leftPad(String.valueOf(localDateNLap.get(ChronoField.MONTH_OF_YEAR)), 2, "0"));
					reportParams.put("InvoiceYear", StringUtils.leftPad(String.valueOf(localDateNLap.get(ChronoField.YEAR)), 4, "0"));
				}
				
				
				
				reportParams.put("HDonTen", 
						commons.getTextFromNodeXML((Element) xPath.evaluate("THDon", nodeTTChung, XPathConstants.NODE))
						
					);
				
				reportParams.put("KHHDon", 
					commons.getTextFromNodeXML((Element) xPath.evaluate("KHMSHDon", nodeTTChung, XPathConstants.NODE))
					+ commons.getTextFromNodeXML((Element) xPath.evaluate("KHHDon", nodeTTChung, XPathConstants.NODE))
				);
				
				LocalDate localDateCD= LocalDate.now();
				localDateCD = commons.convertStringToLocalDate(localDateCD.toString(), "yyyy-MM-dd");
					reportParams.put("InvoiceCovertDate", StringUtils.leftPad(String.valueOf(localDateCD.get(ChronoField.DAY_OF_MONTH)), 2, "0"));
					reportParams.put("InvoiceCovertMonth", StringUtils.leftPad(String.valueOf(localDateCD.get(ChronoField.MONTH_OF_YEAR)), 2, "0"));
					reportParams.put("InvoiceCovertYear", StringUtils.leftPad(String.valueOf(localDateCD.get(ChronoField.YEAR)), 4, "0"));
					try {
						String[] words = CheckView.split(",");
						  CheckView = words[0];
						  Checknamecd =  words[1];
					
					} catch (Exception e) {
						// TODO: handle exception
					}
			
					if(!Checknamecd.equals("")) {
						reportParams.put("SignNameCovert",Checknamecd);			
					}
	if(CheckView.equals("Y"))
	{
				reportParams.put("SHDon", commons.getTextFromNodeXML((Element) xPath.evaluate("SHDon", nodeTTChung, XPathConstants.NODE)));
				reportParams.put("SHDonOrigin", commons.getTextFromNodeXML((Element) xPath.evaluate("SHDon", nodeTTChung, XPathConstants.NODE)));
				
	}
	else {
		reportParams.put("SHDon",
				commons.addSpacingAfterLeter(
					commons.formatNumberBillInvoice(commons.getTextFromNodeXML((Element) xPath.evaluate("SHDon", nodeTTChung, XPathConstants.NODE)))
				)
			);
		//	reportParams.put("SHDonOrigin", commons.getTextFromNodeXML((Element) xPath.evaluate("SHDon", nodeTTChung, XPathConstants.NODE)));
			reportParams.put("SHDonOrigin",
					commons.addSpacingAfterLeter(
						commons.formatNumberBillInvoice(commons.getTextFromNodeXML((Element) xPath.evaluate("SHDon", nodeTTChung, XPathConstants.NODE)))
					)
				);
	}
				
				
				reportParams.put("MCCQT", 
					commons.getTextFromNodeXML((Element) xPath.evaluate("MCCQT", nodeHDon, XPathConstants.NODE))
				);
				//TTChung/TTKhac
				nodeListTTKhac = (NodeList) xPath.evaluate("TTKhac/TTin", nodeTTChung, XPathConstants.NODESET);
				if(nodeListTTKhac != null) {
					for(int j = 0; j < nodeListTTKhac.getLength(); j++) {
						nodeSubTmp = nodeListTTKhac.item(j);
						
						KDLieu = commons.getTextFromNodeXML((Element) xPath.evaluate("KDLieu", nodeSubTmp, XPathConstants.NODE));
						switch (KDLieu.toUpperCase()) {
						case "DECIMAL":
							reportParams.put(
								commons.getTextFromNodeXML((Element) xPath.evaluate("TTruong", nodeSubTmp, XPathConstants.NODE)),
								commons.ToNumber(commons.getTextFromNodeXML((Element) xPath.evaluate("DLieu", nodeSubTmp, XPathConstants.NODE)))
							);
							break;
						default:
							reportParams.put(
								commons.getTextFromNodeXML((Element) xPath.evaluate("TTruong", nodeSubTmp, XPathConstants.NODE)),
								commons.getTextFromNodeXML((Element) xPath.evaluate("DLieu", nodeSubTmp, XPathConstants.NODE))
							);
							break;
						}
					}
				}
				
				reportParams.put("SignName", commons.getTextFromNodeXML((Element) xPath.evaluate("Ten", nodeNBan, XPathConstants.NODE)));
				reportParams.put("NBanTen", 
					commons.getTextFromNodeXML((Element) xPath.evaluate("Ten", nodeNBan, XPathConstants.NODE))
				);
				reportParams.put("NBanMST", 
					commons.getTextFromNodeXML((Element) xPath.evaluate("MST", nodeNBan, XPathConstants.NODE))
				);
				reportParams.put("NBanDChi", 
					commons.getTextFromNodeXML((Element) xPath.evaluate("DChi", nodeNBan, XPathConstants.NODE))
				);
				reportParams.put("NBanSDThoai", 
					commons.getTextFromNodeXML((Element) xPath.evaluate("SDThoai", nodeNBan, XPathConstants.NODE))
				);
				reportParams.put("NBanDCTDTu", 
					commons.getTextFromNodeXML((Element) xPath.evaluate("DCTDTu", nodeNBan, XPathConstants.NODE))
				);
				reportParams.put("NBanSTKNHang", 
					commons.getTextFromNodeXML((Element) xPath.evaluate("STKNHang", nodeNBan, XPathConstants.NODE))
				);
				reportParams.put("NBanTNHang", 
					commons.getTextFromNodeXML((Element) xPath.evaluate("TNHang", nodeNBan, XPathConstants.NODE))
				);
				reportParams.put("NBanFax", 
					commons.getTextFromNodeXML((Element) xPath.evaluate("Fax", nodeNBan, XPathConstants.NODE))
				);
				reportParams.put("NBanWebsite", 
					commons.getTextFromNodeXML((Element) xPath.evaluate("Website", nodeNBan, XPathConstants.NODE))
				);
				
				/*THONG TIN KHAC (NEU CO)*/
				nodeListTTKhac = (NodeList) xPath.evaluate("TTKhac/TTin", nodeNBan, XPathConstants.NODESET);
				if(nodeListTTKhac != null) {
					for(int j = 0; j < nodeListTTKhac.getLength(); j++) {
						nodeSubTmp = nodeListTTKhac.item(j);
						
						KDLieu = commons.getTextFromNodeXML((Element) xPath.evaluate("KDLieu", nodeSubTmp, XPathConstants.NODE));
						switch (KDLieu.toUpperCase()) {
						case "DECIMAL":
							reportParams.put(
								"NBan" + commons.getTextFromNodeXML((Element) xPath.evaluate("TTruong", nodeSubTmp, XPathConstants.NODE)),
								commons.ToNumber(commons.getTextFromNodeXML((Element) xPath.evaluate("DLieu", nodeSubTmp, XPathConstants.NODE)))
							);
							break;
						default:
							reportParams.put(
								"NBan" + commons.getTextFromNodeXML((Element) xPath.evaluate("TTruong", nodeSubTmp, XPathConstants.NODE)),
								commons.getTextFromNodeXML((Element) xPath.evaluate("DLieu", nodeSubTmp, XPathConstants.NODE))
							);
							break;
						}
					}
				}
				
				reportParams.put("NMuaTen", 
					commons.getTextFromNodeXML((Element) xPath.evaluate("Ten", nodeNMua, XPathConstants.NODE))
				);
				reportParams.put("NMuaMST", 
					commons.getTextFromNodeXML((Element) xPath.evaluate("MST", nodeNMua, XPathConstants.NODE))
				);
				reportParams.put("NMuaDChi", 
					commons.getTextFromNodeXML((Element) xPath.evaluate("DChi", nodeNMua, XPathConstants.NODE))
				);
				reportParams.put("NMuaMKHang", 
					commons.getTextFromNodeXML((Element) xPath.evaluate("MKHang", nodeNMua, XPathConstants.NODE))
				);
				
				reportParams.put("CCCD", 
						commons.getTextFromNodeXML((Element) xPath.evaluate("CCCDan", nodeNMua, XPathConstants.NODE))
					);
				
				reportParams.put("NMuaSDThoai", 
					commons.getTextFromNodeXML((Element) xPath.evaluate("SDThoai", nodeNMua, XPathConstants.NODE))
				);
				reportParams.put("NMuaDCTDTu", 
					commons.getTextFromNodeXML((Element) xPath.evaluate("DCTDTu", nodeNMua, XPathConstants.NODE))
				);
				reportParams.put("NMuaHVTNMHang", 
					commons.getTextFromNodeXML((Element) xPath.evaluate("HVTNMHang", nodeNMua, XPathConstants.NODE))
				);
				reportParams.put("NMuaSTKNHang", 
					commons.getTextFromNodeXML((Element) xPath.evaluate("STKNHang", nodeNMua, XPathConstants.NODE))
				);
				reportParams.put("NMuaTNHang", 
					commons.getTextFromNodeXML((Element) xPath.evaluate("TNHang", nodeNMua, XPathConstants.NODE))
				);
				
				reportParams.put("TToanTGia", commons.ToNumber(commons.getTextFromNodeXML((Element) xPath.evaluate("TGia", nodeTTChung, XPathConstants.NODE))));
				reportParams.put("TTChungHTTToan", commons.getTextFromNodeXML((Element) xPath.evaluate("HTTToan", nodeTTChung, XPathConstants.NODE)));
				
				String TCHDon = commons.getTextFromNodeXML((Element) xPath.evaluate("TTHDLQuan/TCHDon", nodeTTChung, XPathConstants.NODE));
				String noticeTTDC = "";
				switch (TCHDon) {
				case "1":
					if(CheckView.equals("Y"))
					{
						noticeTTDC = String.format("(Thay thế cho hóa đơn Mẫu số %s, Ký hiệu %s, số %s, ngày %s)", 
								commons.getTextFromNodeXML((Element) xPath.evaluate("TTHDLQuan/KHMSHDCLQuan", nodeTTChung, XPathConstants.NODE)),
								commons.getTextFromNodeXML((Element) xPath.evaluate("TTHDLQuan/KHHDCLQuan", nodeTTChung, XPathConstants.NODE)),
								commons.getTextFromNodeXML((Element) xPath.evaluate("TTHDLQuan/SHDCLQuan", nodeTTChung, XPathConstants.NODE)),
								commons.convertLocalDateTimeToString(commons.convertStringToLocalDate(commons.getTextFromNodeXML((Element) xPath.evaluate("TTHDLQuan/NLHDCLQuan", nodeTTChung, XPathConstants.NODE)), "yyyy-MM-dd"), Constants.FORMAT_DATE.FORMAT_DATE_WEB)
							);
					}
					else {
						noticeTTDC = String.format("(Thay thế cho hóa đơn Mẫu số %s, Ký hiệu %s, số %s, ngày %s)", 
								commons.getTextFromNodeXML((Element) xPath.evaluate("TTHDLQuan/KHMSHDCLQuan", nodeTTChung, XPathConstants.NODE)),
								commons.getTextFromNodeXML((Element) xPath.evaluate("TTHDLQuan/KHHDCLQuan", nodeTTChung, XPathConstants.NODE)),
								commons.formatNumberBillInvoice(commons.getTextFromNodeXML((Element) xPath.evaluate("TTHDLQuan/SHDCLQuan", nodeTTChung, XPathConstants.NODE))),
								commons.convertLocalDateTimeToString(commons.convertStringToLocalDate(commons.getTextFromNodeXML((Element) xPath.evaluate("TTHDLQuan/NLHDCLQuan", nodeTTChung, XPathConstants.NODE)), "yyyy-MM-dd"), Constants.FORMAT_DATE.FORMAT_DATE_WEB)
							);
					}
					
					break;
				case "2":
					if(CheckView.equals("Y"))
					{
						noticeTTDC = String.format("(Điều chỉnh cho hóa đơn Mẫu số %s, Ký hiệu %s, số %s, ngày %s)", 
								commons.getTextFromNodeXML((Element) xPath.evaluate("TTHDLQuan/KHMSHDCLQuan", nodeTTChung, XPathConstants.NODE)),
								commons.getTextFromNodeXML((Element) xPath.evaluate("TTHDLQuan/KHHDCLQuan", nodeTTChung, XPathConstants.NODE)),
								commons.getTextFromNodeXML((Element) xPath.evaluate("TTHDLQuan/SHDCLQuan", nodeTTChung, XPathConstants.NODE)),
								commons.convertLocalDateTimeToString(commons.convertStringToLocalDate(commons.getTextFromNodeXML((Element) xPath.evaluate("TTHDLQuan/NLHDCLQuan", nodeTTChung, XPathConstants.NODE)), "yyyy-MM-dd"), Constants.FORMAT_DATE.FORMAT_DATE_WEB)
							);
					}
					else {
						noticeTTDC = String.format("(Điều chỉnh cho hóa đơn Mẫu số %s, Ký hiệu %s, số %s, ngày %s)", 
								commons.getTextFromNodeXML((Element) xPath.evaluate("TTHDLQuan/KHMSHDCLQuan", nodeTTChung, XPathConstants.NODE)),
								commons.getTextFromNodeXML((Element) xPath.evaluate("TTHDLQuan/KHHDCLQuan", nodeTTChung, XPathConstants.NODE)),
								commons.formatNumberBillInvoice(commons.getTextFromNodeXML((Element) xPath.evaluate("TTHDLQuan/SHDCLQuan", nodeTTChung, XPathConstants.NODE))),
								commons.convertLocalDateTimeToString(commons.convertStringToLocalDate(commons.getTextFromNodeXML((Element) xPath.evaluate("TTHDLQuan/NLHDCLQuan", nodeTTChung, XPathConstants.NODE)), "yyyy-MM-dd"), Constants.FORMAT_DATE.FORMAT_DATE_WEB)
							);
					}
					
					break;
				default:
					break;
				}
				reportParams.put("NoticeTTDC", noticeTTDC);
				
				reportParams.put("TTChungDVTTe", commons.getTextFromNodeXML((Element) xPath.evaluate("DVTTe", nodeTTChung, XPathConstants.NODE)));
				reportParams.put("TToanTgTCThue", 
					commons.ToNumber(commons.getTextFromNodeXML((Element) xPath.evaluate("TgTCThue", nodeTToan, XPathConstants.NODE)))
				);
				reportParams.put("TToanTgTThue", 
					commons.ToNumber(commons.getTextFromNodeXML((Element) xPath.evaluate("TgTThue", nodeTToan, XPathConstants.NODE)))
				);
				reportParams.put("TToanTgTTTBSo", 
					commons.ToNumber(commons.getTextFromNodeXML((Element) xPath.evaluate("TgTTTBSo", nodeTToan, XPathConstants.NODE)))
				);
				/*THONG TIN KHAC (NEU CO)*/
				String TTruong = "";
				String tienQuyDoi = "";
				
				nodeListTTKhac = (NodeList) xPath.evaluate("TTKhac/TTin", nodeTToan, XPathConstants.NODESET);
				if(nodeListTTKhac != null) {
					for(int j = 0; j < nodeListTTKhac.getLength(); j++) {
						nodeSubTmp = nodeListTTKhac.item(j);
						TTruong = commons.getTextFromNodeXML((Element) xPath.evaluate("TTruong", nodeSubTmp, XPathConstants.NODE));
						
						if("TgTQDoi".equals(TTruong)) {
							tienQuyDoi =commons.getTextFromNodeXML((Element) xPath.evaluate("DLieu", nodeSubTmp, XPathConstants.NODE));
						}
						
						KDLieu = commons.getTextFromNodeXML((Element) xPath.evaluate("KDLieu", nodeSubTmp, XPathConstants.NODE));
						switch (KDLieu.toUpperCase()) {
						case "DECIMAL":
							reportParams.put(
								"TToan" + commons.getTextFromNodeXML((Element) xPath.evaluate("TTruong", nodeSubTmp, XPathConstants.NODE)),
								commons.ToNumber(commons.getTextFromNodeXML((Element) xPath.evaluate("DLieu", nodeSubTmp, XPathConstants.NODE)))
							);
							break;
						default:
							reportParams.put(
								"TToan" + commons.getTextFromNodeXML((Element) xPath.evaluate("TTruong", nodeSubTmp, XPathConstants.NODE)),
								commons.getTextFromNodeXML((Element) xPath.evaluate("DLieu", nodeSubTmp, XPathConstants.NODE))
							);
							break;
						}
					}
				}
				//TIEN QUY DOI
				if(!tienQuyDoi.equals("")) {
					double QuyDoiTien = Double.parseDouble(tienQuyDoi);
					reportParams.put("TToanTgTTTQDoi", QuyDoiTien);
				}
				reportParams.put("TToanTgTTTBChu", 
					commons.getTextFromNodeXML((Element) xPath.evaluate("TgTTTBChu", nodeTToan, XPathConstants.NODE))
				);
				
				/*LAY DANH SACH LAI SUAT*/
				String tsuatTmp = "";
				NodeList nodeListLTSuat = null;
				try {
					nodeListLTSuat = (NodeList) xPath.evaluate("THTTLTSuat/LTSuat", nodeTToan, XPathConstants.NODESET);
					if(nodeListLTSuat != null && nodeListLTSuat.getLength() > 0) {
						for(int i = 0; i < nodeListLTSuat.getLength(); i++) {
							nodeTmp = nodeListLTSuat.item(i);
							
							tsuatTmp = commons.getTextFromNodeXML((Element) xPath.evaluate("TSuat", nodeTmp, XPathConstants.NODE)).replaceAll("%", "");
							reportParams.put("Tax_TSuat_" + tsuatTmp, tsuatTmp);
							reportParams.put("Tax_ThTien_" + tsuatTmp, 
								commons.ToNumber(commons.getTextFromNodeXML((Element) xPath.evaluate("ThTien", nodeTmp, XPathConstants.NODE)))
							);
							reportParams.put("Tax_TThue_" + tsuatTmp, 
								commons.ToNumber(commons.getTextFromNodeXML((Element) xPath.evaluate("TThue", nodeTmp, XPathConstants.NODE)))
							);
							
							if(reportParams.get("Tax_TSuat_Default") == null) {
								reportParams.put("Tax_TSuat_Default", tsuatTmp);
								reportParams.put("Tax_ThTien_Default", 
									commons.ToNumber(commons.getTextFromNodeXML((Element) xPath.evaluate("ThTien", nodeTmp, XPathConstants.NODE)))
								);
//								reportParams.put("Tax_TThue_Default", 
//									commons.ToNumber(commons.getTextFromNodeXML((Element) xPath.evaluate("TThue", nodeTmp, XPathConstants.NODE)))
//								);
							}
						}
					}
				}catch(Exception e) {}
				/*END - LAY DANH SACH LAI SUAT*/
				
				reportParams.put("Tax_TThue_Default", 
						commons.ToNumber(commons.getTextFromNodeXML((Element) xPath.evaluate("TgTThue", nodeTToan, XPathConstants.NODE)))
					);
				
				
				/*KIEM TRA THONG TIN NGUOI BAN KY CHUA*/
				Node nodeDSCKS =null;
				if(nodeHDonTDiep != null) {
					 nodeDSCKS = (Node) xPath.evaluate("CKSNNT", nodeHDonTDiep, XPathConstants.NODE);
				}
				Node nodeSignature = null;
				if(null != nodeDSCKS)
					nodeSignature = (Node) xPath.evaluate("Signature", nodeDSCKS, XPathConstants.NODE);
				LocalDateTime ldt = null;
				if(null == nodeSignature) {
					reportParams.put("IsSigned", false);
					reportParams.put("SignDesc", "Chưa ký");
					reportParams.put("UrlImageVerify", Paths.get(SystemParams.DIR_E_INVOICE_TEMPLATE, Constants.TEMPLATE_FILE_NAME.IMG_SIGNATURE_INVALID).toString());
				}else {
					String x509Certificate = commons.getTextFromNodeXML((Element) xPath.evaluate("KeyInfo/X509Data/X509Certificate", nodeSignature, XPathConstants.NODE));
					String signingTime = commons.getTextFromNodeXML((Element) xPath.evaluate("Object[@Id='SigningTime']/SignatureProperties/SignatureProperty/SigningTime", nodeSignature, XPathConstants.NODE));
					if(signingTime.equals("")) {
						signingTime = commons.getTextFromNodeXML((Element) xPath.evaluate("Object[@Id='HDonDSCKSNBandataSigningTime']/SignatureProperties/SignatureProperty/SigningTime", nodeSignature, XPathConstants.NODE));
					}
					
					reportParams.put("IsSigned", true);
					reportParams.put("SignDesc", "Đã ký");
					SignTypeInfo signTypeInfo = commons.parserCert(x509Certificate);
					reportParams.put("SignName", null == signTypeInfo? "": signTypeInfo.getName());
					try {
					    if (signingTime.endsWith("Z")) {
					        ldt = commons.convertStringToLocalDateTime(signingTime, "yyyy-MM-dd'T'HH:mm:ss'Z'");
					    } else {
					        ldt = commons.convertStringToLocalDateTime(signingTime, "yyyy-MM-dd'T'HH:mm:ss");
					    }

						reportParams.put("SignDate", commons.convertLocalDateTimeToString(ldt, Constants.FORMAT_DATE.FORMAT_DATE_WEB));
					}catch(Exception e) {}
					reportParams.put("UrlImageVerify", Paths.get(SystemParams.DIR_E_INVOICE_TEMPLATE, Constants.TEMPLATE_FILE_NAME.IMG_SIGNATURE_VALID).toString());
				}
				reportParams.put("UrlImageInvDeleted", Paths.get(SystemParams.DIR_E_INVOICE_TEMPLATE, Constants.TEMPLATE_FILE_NAME.IMG_INV_DELETED).toString());
				reportParams.put("IsDeleted", isDeleted);
				reportParams.put("UrlImageInvThayThe", Paths.get(SystemParams.DIR_E_INVOICE_TEMPLATE, Constants.TEMPLATE_FILE_NAME.IMG_INV_THAYTHE).toString());
				reportParams.put("IsThayThe", isThayThe);
				
				
				
				if(!einvoiceStatus.equals("ERROR_CQT") && signStatusCode.equals("SIGNED")) {
					reportParams.put("IsSignedMTT", true);
					reportParams.put("UrlImageMTTVerify", Paths.get(SystemParams.DIR_E_INVOICE_TEMPLATE, Constants.TEMPLATE_FILE_NAME.IMG_SIGNATURE_VALID).toString());

				}
				else {
					
					reportParams.put("IsSignedMTT", false);
					reportParams.put("UrlImageMTTVerify", Paths.get(SystemParams.DIR_E_INVOICE_TEMPLATE, Constants.TEMPLATE_FILE_NAME.IMG_SIGNATURE_INVALID).toString());

				}
				List<HashMap<String, Object>> arrayData = new ArrayList<>();
				HashMap<String, Object> hItem = null;
				
				int lenProName = 0;
				String productName = "";
				String sLo = "";
				String hanSD = "";
				int startRowGroup = 0;
				
				/*XEM CO IN NHIEU DONG KHONG*/
				int groupPageIDX = 1;
				boolean isUsingMultiPage = false;
				if(nodeListHHDVu != null) {
					for(int i = 0; i < nodeListHHDVu.getLength(); i++) {
						nodeTmp = nodeListHHDVu.item(i);
						
						productName = commons.getTextFromNodeXML((Element) xPath.evaluate("THHDVu", nodeTmp, XPathConstants.NODE));
						lenProName = productName.getBytes(StandardCharsets.UTF_8).length;
						startRowGroup += Math.ceil((double) lenProName / numberCharsInRow);
						
						if(startRowGroup >= numberRowInPage) {
							isUsingMultiPage = true;
							break;
						}
					}	
				}
				/*END - XEM CO IN NHIEU DONG KHONG*/
				
				startRowGroup = 0;
				KDLieu = "";
				nodeTmp = null;
				nodeSubTmp = null;
				nodeListTTKhac = null;
				countPrd = 0;
				if(nodeListHHDVu != null) {
					if(isUsingMultiPage) {
						for(int i = 0; i < nodeListHHDVu.getLength(); i++) {
							nodeTmp = nodeListHHDVu.item(i);
							
							productName = commons.getTextFromNodeXML((Element) xPath.evaluate("THHDVu", nodeTmp, XPathConstants.NODE));
							sLo = commons.getTextFromNodeXML((Element) xPath.evaluate("SLo", nodeTmp, XPathConstants.NODE));
							hanSD = commons.getTextFromNodeXML((Element) xPath.evaluate("HanSD", nodeTmp, XPathConstants.NODE));
							lenProName = productName.getBytes(StandardCharsets.UTF_8).length;
							startRowGroup += Math.ceil((double) lenProName / numberCharsInRow);
//							countRowForPrd = (int) Math.ceil(lenProName / numberCharsInRow);
//							if(countRowForPrd > 1)
//								i = i + countRowForPrd;
							
							hItem = new HashMap<>();
							hItem.put("GroupPageIDX", groupPageIDX);
							
							hItem.put("TChat", commons.getTextFromNodeXML((Element) xPath.evaluate("TChat", nodeTmp, XPathConstants.NODE)));
							hItem.put("STT", commons.getTextFromNodeXML((Element) xPath.evaluate("STT", nodeTmp, XPathConstants.NODE)));
							hItem.put("MHHDVu", commons.getTextFromNodeXML((Element) xPath.evaluate("MHHDVu", nodeTmp, XPathConstants.NODE)));
							
							hItem.put("THHDVu", productName);
							
							if(!"".equals(sLo)){
								hItem.put("SLo", commons.getTextFromNodeXML((Element) xPath.evaluate("SLo", nodeTmp, XPathConstants.NODE)));
							}
							if(!"".equals(hanSD)){
								hItem.put("HanSD", commons.getTextFromNodeXML((Element) xPath.evaluate("HanSD", nodeTmp, XPathConstants.NODE)));
							}
							
							hItem.put("DVTinh", commons.getTextFromNodeXML((Element) xPath.evaluate("DVTinh", nodeTmp, XPathConstants.NODE)));
							hItem.put("SLuong",
								commons.ToNumber(commons.getTextFromNodeXML((Element) xPath.evaluate("SLuong", nodeTmp, XPathConstants.NODE)))
							);
							hItem.put("DGia",
								commons.ToNumber(commons.getTextFromNodeXML((Element) xPath.evaluate("DGia", nodeTmp, XPathConstants.NODE)))
							);
							hItem.put("ThTien",
								commons.ToNumber(commons.getTextFromNodeXML((Element) xPath.evaluate("ThTien", nodeTmp, XPathConstants.NODE)))
							);
							hItem.put("TSuat",
								commons.getTextFromNodeXML((Element) xPath.evaluate("TSuat", nodeTmp, XPathConstants.NODE))
							);
							
							/*LAY NOI DUNG TTKHAC*/
							nodeListTTKhac = (NodeList) xPath.evaluate("TTKhac/TTin", nodeTmp, XPathConstants.NODESET);
							if(nodeListTTKhac != null) {
								for(int j = 0; j < nodeListTTKhac.getLength(); j++) {
									nodeSubTmp = nodeListTTKhac.item(j);
									
									KDLieu = commons.getTextFromNodeXML((Element) xPath.evaluate("KDLieu", nodeSubTmp, XPathConstants.NODE));
									switch (KDLieu.toUpperCase()) {
									case "DECIMAL":
										hItem.put(
											commons.getTextFromNodeXML((Element) xPath.evaluate("TTruong", nodeSubTmp, XPathConstants.NODE)),
											commons.ToNumber(commons.getTextFromNodeXML((Element) xPath.evaluate("DLieu", nodeSubTmp, XPathConstants.NODE)))
										);
										break;
									default:
										hItem.put(
											commons.getTextFromNodeXML((Element) xPath.evaluate("TTruong", nodeSubTmp, XPathConstants.NODE)),
											commons.getTextFromNodeXML((Element) xPath.evaluate("DLieu", nodeSubTmp, XPathConstants.NODE))
										);
										break;
									}
								}
							}
							
							arrayData.add(hItem);
							countPrd++;	
							
							if(startRowGroup >= numberRowInPageMultiPage) {
								startRowGroup = 0;
								groupPageIDX++;
							}
						}
						if(0 == startRowGroup && nodeListHHDVu.getLength() > 0) groupPageIDX--;		//BO BOT TRANG CUOI CUNG VI KHONG CO DU LIEU
						
						if(0 == startRowGroup) {
							groupPageIDX++;
							for(int i = 0; i < numberRowInPage; i++) {
								hItem = new HashMap<>();
								hItem.put("ProdName", " ");
								hItem.put("GroupPageIDX", groupPageIDX);
								arrayData.add(hItem);
							}
						}else if(startRowGroup <= numberRowInPage) {
							for(int i = startRowGroup; i < numberRowInPage; i++) {
								hItem = new HashMap<>();
								hItem.put("ProdName", " ");
								hItem.put("GroupPageIDX", groupPageIDX);
								arrayData.add(hItem);
							}
						}else {
							for(int i = startRowGroup; i < numberRowInPageMultiPage; i++) {
								hItem = new HashMap<>();
								hItem.put("ProdName", " ");
								hItem.put("GroupPageIDX", groupPageIDX);
								arrayData.add(hItem);
							}
							groupPageIDX++;
							for(int i = 0; i < numberRowInPage; i++) {
								hItem = new HashMap<>();
								hItem.put("ProdName", " ");
								hItem.put("GroupPageIDX", groupPageIDX);
								arrayData.add(hItem);
							}
						}
					}else {
						for(int i = 0; i < nodeListHHDVu.getLength(); i++) {
							nodeTmp = nodeListHHDVu.item(i);
							
							productName = commons.getTextFromNodeXML((Element) xPath.evaluate("THHDVu", nodeTmp, XPathConstants.NODE));
							sLo = commons.getTextFromNodeXML((Element) xPath.evaluate("SLo", nodeTmp, XPathConstants.NODE));
							hanSD = commons.getTextFromNodeXML((Element) xPath.evaluate("HanSD", nodeTmp, XPathConstants.NODE));
							lenProName = productName.getBytes(StandardCharsets.UTF_8).length;
							startRowGroup += Math.ceil((double) lenProName / numberCharsInRow);
//							countRowForPrd = (int) Math.ceil(lenProName / numberCharsInRow);
//							if(countRowForPrd > 1)
//								i = i + countRowForPrd;
							
							hItem = new HashMap<>();
							hItem.put("GroupPageIDX", groupPageIDX);
							
							hItem.put("TChat", commons.getTextFromNodeXML((Element) xPath.evaluate("TChat", nodeTmp, XPathConstants.NODE)));
							hItem.put("STT", commons.getTextFromNodeXML((Element) xPath.evaluate("STT", nodeTmp, XPathConstants.NODE)));
							hItem.put("MHHDVu", commons.getTextFromNodeXML((Element) xPath.evaluate("MHHDVu", nodeTmp, XPathConstants.NODE)));
							
							hItem.put("THHDVu", productName);
							if(!"".equals(sLo)){
								hItem.put("SLo", commons.getTextFromNodeXML((Element) xPath.evaluate("SLo", nodeTmp, XPathConstants.NODE)));
							}
							if(!"".equals(hanSD)){
								hItem.put("HanSD", commons.getTextFromNodeXML((Element) xPath.evaluate("HanSD", nodeTmp, XPathConstants.NODE)));
							}
							hItem.put("DVTinh", commons.getTextFromNodeXML((Element) xPath.evaluate("DVTinh", nodeTmp, XPathConstants.NODE)));
							hItem.put("SLuong",
								commons.ToNumber(commons.getTextFromNodeXML((Element) xPath.evaluate("SLuong", nodeTmp, XPathConstants.NODE)))
							);
							hItem.put("DGia",
								commons.ToNumber(commons.getTextFromNodeXML((Element) xPath.evaluate("DGia", nodeTmp, XPathConstants.NODE)))
							);
							hItem.put("ThTien",
								commons.ToNumber(commons.getTextFromNodeXML((Element) xPath.evaluate("ThTien", nodeTmp, XPathConstants.NODE)))
							);
							hItem.put("TSuat",
								commons.getTextFromNodeXML((Element) xPath.evaluate("TSuat", nodeTmp, XPathConstants.NODE))
							);
							
							/*LAY NOI DUNG TTKHAC*/
							nodeListTTKhac = (NodeList) xPath.evaluate("TTKhac/TTin", nodeTmp, XPathConstants.NODESET);
							if(nodeListTTKhac != null) {
								for(int j = 0; j < nodeListTTKhac.getLength(); j++) {
									nodeSubTmp = nodeListTTKhac.item(j);
									
									KDLieu = commons.getTextFromNodeXML((Element) xPath.evaluate("KDLieu", nodeSubTmp, XPathConstants.NODE));
									switch (KDLieu.toUpperCase()) {
									case "DECIMAL":
										hItem.put(
											commons.getTextFromNodeXML((Element) xPath.evaluate("TTruong", nodeSubTmp, XPathConstants.NODE)),
											commons.ToNumber(commons.getTextFromNodeXML((Element) xPath.evaluate("DLieu", nodeSubTmp, XPathConstants.NODE)))
										);
										break;
									default:
										hItem.put(
											commons.getTextFromNodeXML((Element) xPath.evaluate("TTruong", nodeSubTmp, XPathConstants.NODE)),
											commons.getTextFromNodeXML((Element) xPath.evaluate("DLieu", nodeSubTmp, XPathConstants.NODE))
										);
										break;
									}
								}
							}
							
							arrayData.add(hItem);
							countPrd++;	
							
							if(startRowGroup >= numberRowInPage) {
								startRowGroup = 0;
								groupPageIDX++;
							}
						}
						if(0 == startRowGroup && nodeListHHDVu.getLength() > 0) groupPageIDX--;		//BO BOT TRANG CUOI CUNG VI KHONG CO DU LIEU
						
						/*ADD THEM DU LIEU CHO DU TRANG*/
						if(startRowGroup > 0 || nodeListHHDVu.getLength() == 0) {
							for(int i = startRowGroup; i < numberRowInPage; i++) {
								hItem = new HashMap<>();
								hItem.put("ProdName", " ");
								hItem.put("GroupPageIDX", groupPageIDX);
								arrayData.add(hItem);
							}
						}
					}
					
				}		
				reportParams.put("PortalLink",  link);
				
				reportParams.put("TOTAL_PAGE", (int) groupPageIDX);
				
				JRDataSource jds = null;
				jds = new JRBeanCollectionDataSource(arrayData);
				
				ByteArrayOutputStream out = new ByteArrayOutputStream();
				JasperReport jr = JasperCompileManager.compileReport(new FileInputStream(fileJP));
				JasperPrint jp = JasperFillManager.fillReport(jr, reportParams, jds);
				Exporter exporter = null;
				
				exporter = new JRPdfExporter();
				exporter.setExporterInput(new SimpleExporterInput(jp));
				exporter.setExporterOutput(new SimpleOutputStreamExporterOutput(out));
		        SimplePdfExporterConfiguration configuration = new SimplePdfExporterConfiguration();
		        configuration.setCreatingBatchModeBookmarks(true);
		        exporter.setConfiguration(configuration);
		        exporter.exportReport();
		        return out;
			}


}
