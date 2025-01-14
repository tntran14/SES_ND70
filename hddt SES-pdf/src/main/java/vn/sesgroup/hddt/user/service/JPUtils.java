package vn.sesgroup.hddt.user.service;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoField;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
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
	private Commons commons = new Commons();
	
	public ByteArrayOutputStream createFinalInvoice(File fileJP, Document doc, String CheckView
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

	
	
	
	
	public ByteArrayOutputStream createFinalInvoicetest(File fileJP, org.bson.Document docTmp, boolean isConvert
		) throws Exception{
			Map<String, Object> reportParams = new HashMap<String, Object>();
			 String Checknamecd = "";
				String MST = docTmp.getEmbedded(Arrays.asList("EInvoiceDetail", "NDHDon", "NBan", "MST"), "");
				String ImgLogo = docTmp.getEmbedded(Arrays.asList("DMMauSoKyHieu", "Templates", "ImgLogo"), "");
				String ImgBackground = docTmp.getEmbedded(Arrays.asList("DMMauSoKyHieu", "Templates", "ImgBackground"),"");
				String ImgQA = docTmp.getEmbedded(Arrays.asList("DMMauSoKyHieu", "Templates", "ImgQA"), "");
				String ImgVien = docTmp.getEmbedded(Arrays.asList("DMMauSoKyHieu", "Templates", "ImgVien"), "");
				String pathLogo = Paths.get(SystemParams.DIR_E_INVOICE_TEMPLATE, "images", MST, ImgLogo).toString();
				String pathBackground =	Paths.get(SystemParams.DIR_E_INVOICE_TEMPLATE, "images", MST, ImgBackground).toString();
				String pathQA =Paths.get(SystemParams.DIR_E_INVOICE_TEMPLATE, "images", MST, ImgQA).toString();
				String pathVien =	Paths.get(SystemParams.DIR_E_INVOICE_TEMPLATE, "images", MST, ImgVien).toString();
				String CheckView = docTmp.getEmbedded(Arrays.asList("UserConFig", "viewshd"), "");
				String SHD =	docTmp.getEmbedded(Arrays.asList("EInvoiceDetail", "TTChung", "SHDon"),0 ).toString();
				String SignStatusCode =	docTmp.get("SignStatusCode", "");
				String link = docTmp.getEmbedded(Arrays.asList("PramLink", "LinkPortal"), "");
				int numberRowInPage = docTmp.getEmbedded(Arrays.asList("DMMauSoKyHieu", "Templates", "RowsInPage"), 20);
				int numberRowInPageMultiPage = docTmp.getEmbedded(Arrays.asList("DMMauSoKyHieu", "Templates", "RowInPageMultiPage"), 26);
				int numberCharsInRow = docTmp.getEmbedded(Arrays.asList("DMMauSoKyHieu", "Templates", "CharsInRow"),50);
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
			LocalDate localDateNLap = null;
	        NLap = commons.convertLocalDateTimeToString(commons.convertDateToLocalDateTime((Date)docTmp.getEmbedded(Arrays.asList("EInvoiceDetail", "TTChung", "NLap"), Date.class)), "yyyy-MM-dd");
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
					docTmp.getEmbedded(Arrays.asList("EInvoiceDetail", "TTChung", "KHMSHDon"), "")+docTmp.getEmbedded(Arrays.asList("EInvoiceDetail", "TTChung", "KHHDon"), "")
			);
			
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
							reportParams.put("SHDon",SHD);
							reportParams.put("SHDonOrigin",SHD);	
				}
				else {
					reportParams.put("SHDon",SHD);
					reportParams.put("SHDonOrigin",SHD);
				}
			
			
			reportParams.put("MCCQT", 
				 docTmp.get("MCCQT", "")
			);
//			//TTChung/TTKhac
//			nodeListTTKhac = (NodeList) xPath.evaluate("TTKhac/TTin", nodeTTChung, XPathConstants.NODESET);
//			if(nodeListTTKhac != null) {
//				for(int j = 0; j < nodeListTTKhac.getLength(); j++) {
//					nodeSubTmp = nodeListTTKhac.item(j);
//					
//					KDLieu = commons.getTextFromNodeXML((Element) xPath.evaluate("KDLieu", nodeSubTmp, XPathConstants.NODE));
//					switch (KDLieu.toUpperCase()) {
//					case "DECIMAL":
//						reportParams.put(
//							commons.getTextFromNodeXML((Element) xPath.evaluate("TTruong", nodeSubTmp, XPathConstants.NODE)),
//							commons.ToNumber(commons.getTextFromNodeXML((Element) xPath.evaluate("DLieu", nodeSubTmp, XPathConstants.NODE)))
//						);
//						break;
//					default:
//						reportParams.put(
//							commons.getTextFromNodeXML((Element) xPath.evaluate("TTruong", nodeSubTmp, XPathConstants.NODE)),
//							commons.getTextFromNodeXML((Element) xPath.evaluate("DLieu", nodeSubTmp, XPathConstants.NODE))
//						);
//						break;
//					}
//				}
//			}
			
			reportParams.put("SignName", docTmp.getEmbedded(Arrays.asList("InfoSignature", "SignatureName"), ""));
			reportParams.put("NBanTen", 
					docTmp.getEmbedded(Arrays.asList("EInvoiceDetail", "NDHDon", "NBan", "Ten"), "")
			);
			reportParams.put("NBanMST", 
					docTmp.getEmbedded(Arrays.asList("EInvoiceDetail", "NDHDon", "NBan", "MST"), "")
			);
			reportParams.put("NBanDChi", 
					docTmp.getEmbedded(Arrays.asList("EInvoiceDetail", "NDHDon", "NBan", "DChi"), "")
			);
			reportParams.put("NBanSDThoai", 
					docTmp.getEmbedded(Arrays.asList("EInvoiceDetail", "NDHDon", "NBan", "SDThoai"), "")
			);
			reportParams.put("NBanDCTDTu", 
					docTmp.getEmbedded(Arrays.asList("EInvoiceDetail", "NDHDon", "NBan", "DCTDTu"), "")
			);
			reportParams.put("NBanSTKNHang", 
					docTmp.getEmbedded(Arrays.asList("EInvoiceDetail", "NDHDon", "NBan", "STKNHang"), "")
			);
			reportParams.put("NBanTNHang", 
					docTmp.getEmbedded(Arrays.asList("EInvoiceDetail", "NDHDon", "NBan", "TNHang"), "")
			);
			reportParams.put("NBanFax", 
					docTmp.getEmbedded(Arrays.asList("EInvoiceDetail", "NDHDon", "NBan", "Fax"), "")
			);
			reportParams.put("NBanWebsite", 
					docTmp.getEmbedded(Arrays.asList("EInvoiceDetail", "NDHDon", "NBan", "Website"), "")
			);
			
//			/*THONG TIN KHAC (NEU CO)*/
//			nodeListTTKhac = (NodeList) xPath.evaluate("TTKhac/TTin", nodeNBan, XPathConstants.NODESET);
//			if(nodeListTTKhac != null) {
//				for(int j = 0; j < nodeListTTKhac.getLength(); j++) {
//					nodeSubTmp = nodeListTTKhac.item(j);
//					
//					KDLieu = commons.getTextFromNodeXML((Element) xPath.evaluate("KDLieu", nodeSubTmp, XPathConstants.NODE));
//					switch (KDLieu.toUpperCase()) {
//					case "DECIMAL":
//						reportParams.put(
//							"NBan" + commons.getTextFromNodeXML((Element) xPath.evaluate("TTruong", nodeSubTmp, XPathConstants.NODE)),
//							commons.ToNumber(commons.getTextFromNodeXML((Element) xPath.evaluate("DLieu", nodeSubTmp, XPathConstants.NODE)))
//						);
//						break;
//					default:
//						reportParams.put(
//							"NBan" + commons.getTextFromNodeXML((Element) xPath.evaluate("TTruong", nodeSubTmp, XPathConstants.NODE)),
//							commons.getTextFromNodeXML((Element) xPath.evaluate("DLieu", nodeSubTmp, XPathConstants.NODE))
//						);
//						break;
//					}
//				}
//			}
			
			reportParams.put("NMuaTen", 
					docTmp.getEmbedded(Arrays.asList("EInvoiceDetail", "NDHDon", "NMua", "Ten"), "")
			);
			reportParams.put("NMuaMST", 
					docTmp.getEmbedded(Arrays.asList("EInvoiceDetail", "NDHDon", "NMua", "MST"), "")
			);
			reportParams.put("NMuaDChi", 
					docTmp.getEmbedded(Arrays.asList("EInvoiceDetail", "NDHDon", "NMua", "DChi"), "")
			);
			reportParams.put("NMuaMKHang", 
					docTmp.getEmbedded(Arrays.asList("EInvoiceDetail", "NDHDon", "NMua", "MKHang"), "")
			);
			reportParams.put("NMuaSDThoai", 
					docTmp.getEmbedded(Arrays.asList("EInvoiceDetail", "NDHDon", "NMua", "SDThoai"), "")
			);
			reportParams.put("NMuaDCTDTu", 
					docTmp.getEmbedded(Arrays.asList("EInvoiceDetail", "NDHDon", "NMua", "DCTDTu"), "")
			);
			reportParams.put("NMuaHVTNMHang", 
					docTmp.getEmbedded(Arrays.asList("EInvoiceDetail", "NDHDon", "NMua", "HVTNMHang"), "")
			);
			reportParams.put("NMuaSTKNHang", 
					docTmp.getEmbedded(Arrays.asList("EInvoiceDetail", "NDHDon", "NMua", "STKNHang"), "")
			);
			reportParams.put("NMuaTNHang", 
					docTmp.getEmbedded(Arrays.asList("EInvoiceDetail", "NDHDon", "NMua", "TNHang"), "")
			);
			
			reportParams.put("TToanTGia", 	docTmp.getEmbedded(Arrays.asList("EInvoiceDetail", "TTChung", "TGia"), ""));
			reportParams.put("TTChungHTTToan",docTmp.getEmbedded(Arrays.asList("EInvoiceDetail", "TTChung", "HTTToan"), ""));
			
			
			 String TCHDon = docTmp.getEmbedded(Arrays.asList("EInvoiceDetail", "TTChung", "TTHDLQuan", "TCHDon"),"");
			 String noticeTTDC = "";
		     String s = TCHDon;
		     switch (s) {
	            case "1": {
	                if (CheckView.equals("Y")) {
	                	
						noticeTTDC = String.format("(Thay thế cho hóa đơn Mẫu số %s, Ký hiệu %s, số %s, ngày %s)", 
								docTmp.getEmbedded(Arrays.asList("EInvoiceDetail", "TTChung", "TTHDLQuan", "KHMSHDCLQuan"), ""), 
								docTmp.getEmbedded(Arrays.asList("EInvoiceDetail", "TTChung", "TTHDLQuan", "KHHDCLQuan"), ""), 
								docTmp.getEmbedded(Arrays.asList("EInvoiceDetail", "TTChung", "TTHDLQuan", "SHDCLQuan"), ""), 
								docTmp.getEmbedded(Arrays.asList("EInvoiceDetail", "TTChung", "TTHDLQuan", "NLHDCLQuan"), ""));
	                    break;
	                }

					noticeTTDC = String.format("(Thay thế cho hóa đơn Mẫu số %s, Ký hiệu %s, số %s, ngày %s)", 
							docTmp.getEmbedded(Arrays.asList("EInvoiceDetail", "TTChung", "TTHDLQuan", "KHMSHDCLQuan"), ""), 
							docTmp.getEmbedded(Arrays.asList("EInvoiceDetail", "TTChung", "TTHDLQuan", "KHHDCLQuan"), ""), 
							commons.formatNumberBillInvoice(docTmp.getEmbedded(Arrays.asList("EInvoiceDetail", "TTChung", "TTHDLQuan", "SHDCLQuan"), "")), 
							docTmp.getEmbedded(Arrays.asList("EInvoiceDetail", "TTChung", "TTHDLQuan", "NLHDCLQuan"), ""));      
	        break;
	            }
	            case "2": {
	                if (CheckView.equals("Y")) {
	                	noticeTTDC = String.format("(Điều chỉnh cho hóa đơn Mẫu số %s, Ký hiệu %s, số %s, ngày %s)",
	                    		docTmp.getEmbedded(Arrays.asList("EInvoiceDetail", "TTChung", "TTHDLQuan", "KHMSHDCLQuan"), ""),
	                      		docTmp.getEmbedded(Arrays.asList("EInvoiceDetail", "TTChung", "TTHDLQuan", "KHHDCLQuan"), ""),
	                      		docTmp.getEmbedded(Arrays.asList("EInvoiceDetail", "TTChung", "TTHDLQuan", "SHDCLQuan"), ""),
	                      		docTmp.getEmbedded(Arrays.asList("EInvoiceDetail", "TTChung", "TTHDLQuan", "NLHDCLQuan"), "")
	                					);
	                    break;
	                }
					String bc =	docTmp.getEmbedded(Arrays.asList("EInvoiceDetail", "TTChung", "TTHDLQuan", "NLHDCLQuan"), "");
					
					
					noticeTTDC = String.format("(Điều chỉnh cho hóa đơn Mẫu số %s, Ký hiệu %s, số %s, ngày %s)", 
							docTmp.getEmbedded(Arrays.asList("EInvoiceDetail", "TTChung", "TTHDLQuan", "KHMSHDCLQuan"), ""), 
							docTmp.getEmbedded(Arrays.asList("EInvoiceDetail", "TTChung", "TTHDLQuan", "KHHDCLQuan"), ""), 
							commons.formatNumberBillInvoice(docTmp.getEmbedded(Arrays.asList("EInvoiceDetail", "TTChung", "TTHDLQuan", "SHDCLQuan"), "")), 
							docTmp.getEmbedded(Arrays.asList("EInvoiceDetail", "TTChung", "TTHDLQuan", "NLHDCLQuan"), ""));	
	                break;
	            }
	        }
			 
			reportParams.put("NoticeTTDC", noticeTTDC);
			
			
			    String TgTCThue = ((Double)docTmp.getEmbedded(Arrays.asList("EInvoiceDetail", "TToan", "TgTCThue"), 0.0)).toString();
		         String TgTThue = ((Double)docTmp.getEmbedded(Arrays.asList("EInvoiceDetail", "TToan", "TgTThue"), 0.0)).toString();
		         String TgTTTBSo = ((Double)docTmp.getEmbedded(Arrays.asList("EInvoiceDetail", "TToan", "TgTTTBSo"), 0.0)).toString();
		        reportParams.put("TTChungDVTTe", docTmp.getEmbedded(Arrays.asList("EInvoiceDetail", "TTChung", "DVTTe"), ""));
		        reportParams.put("TToanTgTCThue", commons.ToNumber(TgTCThue));
		        reportParams.put("TToanTgTThue", commons.ToNumber(TgTThue));
		        reportParams.put("TToanTgTTTBSo", commons.ToNumber(TgTTTBSo));
		        
		        
//			reportParams.put("TTChungDVTTe", 	docTmp.getEmbedded(Arrays.asList("EInvoiceDetail", "TTChung", "DVTTe"), ""));
//			reportParams.put("TToanTgTCThue", 
//					docTmp.getEmbedded(Arrays.asList("EInvoiceDetail", "TTChung", "TgTCThue"), "")
//			);
//			reportParams.put("TToanTgTThue", docTmp.getEmbedded(Arrays.asList("EInvoiceDetail", "TTChung", "TgTThue"), ""));
//			reportParams.put("TToanTgTTTBSo",docTmp.getEmbedded(Arrays.asList("EInvoiceDetail", "TTChung", "TgTTTBSo"), ""));
		
			
			/*THONG TIN KHAC (NEU CO)*/
//			String TTruong = "";
//			String tienQuyDoi = "";
			
//			nodeListTTKhac = (NodeList) xPath.evaluate("TTKhac/TTin", nodeTToan, XPathConstants.NODESET);
//			if(nodeListTTKhac != null) {
//				for(int j = 0; j < nodeListTTKhac.getLength(); j++) {
//					nodeSubTmp = nodeListTTKhac.item(j);
//					TTruong = commons.getTextFromNodeXML((Element) xPath.evaluate("TTruong", nodeSubTmp, XPathConstants.NODE));
//					
//					if("TgTQDoi".equals(TTruong)) {
//						tienQuyDoi =commons.getTextFromNodeXML((Element) xPath.evaluate("DLieu", nodeSubTmp, XPathConstants.NODE));
//					}
//					
//					KDLieu = commons.getTextFromNodeXML((Element) xPath.evaluate("KDLieu", nodeSubTmp, XPathConstants.NODE));
//					switch (KDLieu.toUpperCase()) {
//					case "DECIMAL":
//						reportParams.put(
//							"TToan" + commons.getTextFromNodeXML((Element) xPath.evaluate("TTruong", nodeSubTmp, XPathConstants.NODE)),
//							commons.ToNumber(commons.getTextFromNodeXML((Element) xPath.evaluate("DLieu", nodeSubTmp, XPathConstants.NODE)))
//						);
//						break;
//					default:
//						reportParams.put(
//							"TToan" + commons.getTextFromNodeXML((Element) xPath.evaluate("TTruong", nodeSubTmp, XPathConstants.NODE)),
//							commons.getTextFromNodeXML((Element) xPath.evaluate("DLieu", nodeSubTmp, XPathConstants.NODE))
//						);
//						break;
//					}
//				}
//			}
			//TIEN QUY DOI
			
//			if(!tienQuyDoi.equals("")) {
//				double QuyDoiTien = Double.parseDouble(tienQuyDoi);
//				reportParams.put("TToanTgTTTQDoi", QuyDoiTien);
//			}
//			reportParams.put("TToanTgTTTBChu", docTmp.getEmbedded(Arrays.asList("EInvoiceDetail", "TTChung", "TgTTTBChu"), ""));
		
		        Double  tienQuyDoi = (Double)docTmp.getEmbedded(Arrays.asList("EInvoiceDetail", "TToan", "TgTQDoi"), 0.0);
		        if (tienQuyDoi > 0.0) {
		            reportParams.put("TToanTgTTTQDoi", tienQuyDoi);
		        }
		        reportParams.put("TToanTgTTTBChu", docTmp.getEmbedded(Arrays.asList("EInvoiceDetail", "TToan", "TgTTTBChu"), ""));

		      
		        reportParams.put("SecureKey",  docTmp.get("SecureKey", ""));
		        reportParams.put("SystemKey",  docTmp.get("_id", ObjectId.class).toString());
		     
			
//			/*LAY DANH SACH LAI SUAT*/
//			String tsuatTmp = "";
//			String check_ts = "";
//			NodeList nodeListLTSuat = null;
//			try {
//				nodeListLTSuat = (NodeList) xPath.evaluate("THTTLTSuat/LTSuat", nodeTToan, XPathConstants.NODESET);
//				if(nodeListLTSuat != null && nodeListLTSuat.getLength() > 0) {
//					for(int i = 0; i < nodeListLTSuat.getLength(); i++) {
//						nodeTmp = nodeListLTSuat.item(i);
//						
//						tsuatTmp = commons.getTextFromNodeXML((Element) xPath.evaluate("TSuat", nodeTmp, XPathConstants.NODE)).replaceAll("%", "");
//						reportParams.put("Tax_TSuat_" + tsuatTmp, tsuatTmp);
//						reportParams.put("Tax_ThTien_" + tsuatTmp, 
//							commons.ToNumber(commons.getTextFromNodeXML((Element) xPath.evaluate("ThTien", nodeTmp, XPathConstants.NODE)))
//						);
//						reportParams.put("Tax_TThue_" + tsuatTmp, 
//							commons.ToNumber(commons.getTextFromNodeXML((Element) xPath.evaluate("TThue", nodeTmp, XPathConstants.NODE)))
//						);
//						
//						
//						
//						
//						
//						if(reportParams.get("Tax_TSuat_Default") == null) {						
//							reportParams.put("Tax_TSuat_Default", tsuatTmp);
//							reportParams.put("Tax_ThTien_Default", 
//								commons.ToNumber(commons.getTextFromNodeXML((Element) xPath.evaluate("ThTien", nodeTmp, XPathConstants.NODE)))
//							);
//						}
//						
//						if(reportParams.get("Tax_TSuat_Default") != null) {	
//							if(check_ts.equals("0")) {							
//							reportParams.put("Tax_TSuat_Default", tsuatTmp);
//							reportParams.put("Tax_ThTien_Default", 
//								commons.ToNumber(commons.getTextFromNodeXML((Element) xPath.evaluate("ThTien", nodeTmp, XPathConstants.NODE)))
//							);
//						}
//						}
//						
//					}
//				}
//			}catch(Exception e) {}
		        
		        
		        
		        String VatName = "";
	            String VATAmount = "";
	            String Amount = "";
	            String check_default = "";
	            if (docTmp.get((Object)"DSVAT") != null && docTmp.getList((Object)"DSVAT", org.bson.Document.class).size() > 0) {
	                for (final org.bson.Document o : docTmp.getList((Object)"DSVAT", org.bson.Document.class)) {
	                    VatName = (String)o.get((Object)"VatName", "");
	                    VATAmount = (String)o.get((Object)"VATAmount", "");
	                    Amount = (String)o.get((Object)"Amount", "");
	                     String Tax_ThTien = "Tax_ThTien_" + VatName;
	                    reportParams.put(Tax_ThTien, commons.ToNumber(Amount));
	                     String Tax_TSuat = "Tax_TSuat_" + VatName;
	                    reportParams.put(Tax_TSuat, VatName);
	                     String Tax_TThue = "Tax_TThue_" + VatName;
	                    reportParams.put(Tax_TThue, commons.ToNumber(VATAmount));
	                    if (reportParams.get("Tax_TSuat_Default") == null) {
	                        reportParams.put("Tax_TSuat_Default", VatName);
	                        reportParams.put("Tax_TThue_Default",commons.ToNumber(VATAmount));
	                        reportParams.put("Tax_ThTien_Default", commons.ToNumber(Amount));
	                        check_default = VatName;
	                    }
	                    if (reportParams.get("Tax_TSuat_Default") != null && check_default.equals("0")) {
	                        reportParams.put("Tax_TSuat_Default", check_default);
	                        reportParams.put("Tax_ThTien_Default", commons.ToNumber(Amount));
	                    }
	                }
	            }
		        
		        
		        
		        
//			/*END - LAY DANH SACH LAI SUAT*/
			
			reportParams.put("Tax_TThue_Default", docTmp.getEmbedded(Arrays.asList("EInvoiceDetail", "TTChung", "TgTThue"), ""));
			
			
			/*KIEM TRA THONG TIN NGUOI BAN KY CHUA*/
		
			
			if(!SignStatusCode.equals("SIGNED")) {
				reportParams.put("IsSigned", false);
				reportParams.put("SignDesc", "Chưa ký");
				reportParams.put("UrlImageVerify", Paths.get(SystemParams.DIR_E_INVOICE_TEMPLATE, Constants.TEMPLATE_FILE_NAME.IMG_SIGNATURE_INVALID).toString());
			}else {
				reportParams.put("IsSigned", true);
				reportParams.put("SignDesc", "Đã ký");
				reportParams.put("SignName",docTmp.getEmbedded(Arrays.asList("InfoSignature", "SignatureName"), ""));
				reportParams.put("SignDate",docTmp.getEmbedded(Arrays.asList("InfoSignature", "SignatureDate"), ""));
				reportParams.put("UrlImageVerify", Paths.get(SystemParams.DIR_E_INVOICE_TEMPLATE, Constants.TEMPLATE_FILE_NAME.IMG_SIGNATURE_VALID).toString());
			}
			reportParams.put("UrlImageInvDeleted", Paths.get(SystemParams.DIR_E_INVOICE_TEMPLATE, Constants.TEMPLATE_FILE_NAME.IMG_INV_DELETED).toString());
			reportParams.put("UrlImageInvThayThe", Paths.get(SystemParams.DIR_E_INVOICE_TEMPLATE, Constants.TEMPLATE_FILE_NAME.IMG_INV_THAYTHE).toString());
			reportParams.put("UrlImageInvDieuChinh", Paths.get(SystemParams.DIR_E_INVOICE_TEMPLATE, Constants.TEMPLATE_FILE_NAME.IMG_INV_DIEUCHINH).toString());
		
			boolean isThayThe = false;
			String check_status = docTmp.get("EInvoiceStatus", "");
			if (check_status.equals("REPLACED")) {
				isThayThe = "3".equals(docTmp.getEmbedded(Arrays.asList("HDSS", "TCTBao"), ""));
			}
			boolean isDieuChinh = "2".equals(docTmp.getEmbedded(Arrays.asList("HDSS", "TCTBao"), ""));
			reportParams.put("IsDeleted",  Constants.INVOICE_STATUS.DELETED.equals(docTmp.get("EInvoiceStatus", "")));
			reportParams.put("IsThayThe", isThayThe);
			reportParams.put("IsDieuChinh", isDieuChinh);
			List<HashMap<String, Object>> arrayData = new ArrayList<>();


			   HashMap<String, Object> hItem2 = null;
		        int lenProName = 0;
		        String productName = "";
		        String sLo = "";
		        String hanSD = "";
		        int startRowGroup = 0;
		        int countPrd = 0;
		        int groupPageIDX = 1;
		        boolean isUsingMultiPage = false;
		        org.bson.Document docEInvoiceDetail = null;
		        docEInvoiceDetail = docTmp.get("EInvoiceDetail", org.bson.Document.class);
		        
		        if (docEInvoiceDetail.get((Object)"DSHHDVu") != null && docEInvoiceDetail.getList((Object)"DSHHDVu", org.bson.Document.class).size() > 0) {
		            for (org.bson.Document o2 : docEInvoiceDetail.getList((Object)"DSHHDVu", org.bson.Document.class)) {
		                productName = (String)o2.get((Object)"ProductName", "");
		                lenProName = productName.getBytes(StandardCharsets.UTF_8).length;
		                startRowGroup += (int)Math.ceil(lenProName / (double)numberCharsInRow);
		                if (startRowGroup >= numberRowInPage) {
		                    isUsingMultiPage = true;
		                    break;
		                }
		            }
		        }

		        startRowGroup = 0;
		        countPrd = 0;
		        
		        if (docEInvoiceDetail.getList((Object)"DSHHDVu", (Class)org.bson.Document.class).size() > 0) {
		            if (isUsingMultiPage) {
		                for ( org.bson.Document o2 : docEInvoiceDetail.getList((Object)"DSHHDVu",org.bson.Document.class)) {
		                    productName = (String)o2.get((Object)"ProductName", "");
		                    sLo = (String)o2.get((Object)"SLo", "");
		                    hanSD = (String)o2.get((Object)"HanSD", "");
		                    lenProName = productName.getBytes(StandardCharsets.UTF_8).length;
		                    startRowGroup += (int)Math.ceil(lenProName / (double)numberCharsInRow);
		                    hItem2 = new HashMap<String, Object>();
		                    hItem2.put("GroupPageIDX", groupPageIDX);
		                    hItem2.put("TChat", o2.get((Object)"Feature", ""));
		                    hItem2.put("STT", o2.get((Object)"STT", ""));
		                    hItem2.put("MHHDVu", o2.get((Object)"ProductCode", ""));
		                    hItem2.put("THHDVu", productName);
		                    if (!"".equals(sLo)) {
		                        hItem2.put("SLo", sLo);
		                    }
		                    if (!"".equals(hanSD)) {
		                        hItem2.put("HanSD", hanSD);
		                    }
		                    hItem2.put("DVTinh", o2.get((Object)"Unit", ""));
		                    hItem2.put("SLuong", o2.get((Object)"Quantity", Double.class));
		                    hItem2.put("DGia", o2.get((Object)"Price", Double.class));
		                    hItem2.put("ThTien", o2.get((Object)"Total", Double.class));
		                    hItem2.put("TSuat", this.commons.formatNumberReal((o2.get((Object)"VATRate", 0.0)).toString()) + "%");
		                    hItem2.put("VATAmount", o2.get((Object)"VATAmount", Double.class));
		                    hItem2.put("Amount", o2.get((Object)"Amount", Double.class));
		                    arrayData.add(hItem2);
		                    ++countPrd;
		                    if (startRowGroup >= numberRowInPageMultiPage) {
		                        startRowGroup = 0;
		                        ++groupPageIDX;
		                    }
		                }
		                if (0 == startRowGroup && docEInvoiceDetail.getList((Object)"DSHHDVu", org.bson.Document.class).size() > 0) {
		                    --groupPageIDX;
		                }
		                if (0 == startRowGroup) {
		                    ++groupPageIDX;
		                    for (int k = 0; k < numberRowInPage; ++k) {
		                        hItem2 = new HashMap<String, Object>();
		                        hItem2.put("ProdName", " ");
		                        hItem2.put("GroupPageIDX", groupPageIDX);
		                        arrayData.add(hItem2);
		                    }
		                }
		                else if (startRowGroup <= numberRowInPage) {
		                    for (int k = startRowGroup; k < numberRowInPage; ++k) {
		                        hItem2 = new HashMap<String, Object>();
		                        hItem2.put("ProdName", " ");
		                        hItem2.put("GroupPageIDX", groupPageIDX);
		                        arrayData.add(hItem2);
		                    }
		                }
		                else {
		                    for (int k = startRowGroup; k < numberRowInPageMultiPage; ++k) {
		                        hItem2 = new HashMap<String, Object>();
		                        hItem2.put("ProdName", " ");
		                        hItem2.put("GroupPageIDX", groupPageIDX);
		                        arrayData.add(hItem2);
		                    }
		                    ++groupPageIDX;
		                    for (int k = 0; k < numberRowInPage; ++k) {
		                        hItem2 = new HashMap<String, Object>();
		                        hItem2.put("ProdName", " ");
		                        hItem2.put("GroupPageIDX", groupPageIDX);
		                        arrayData.add(hItem2);
		                    }
		                }
		            }
		            else {
		                for (final org.bson.Document o2 : docEInvoiceDetail.getList((Object)"DSHHDVu", org.bson.Document.class)) {
		                    productName = (String)o2.get((Object)"ProductName", "");
		                    sLo = (String)o2.get((Object)"SLo", "");
		                    hanSD = (String)o2.get((Object)"HanSD", "");
		                    lenProName = productName.getBytes(StandardCharsets.UTF_8).length;
		                    startRowGroup += (int)Math.ceil(lenProName / (double)numberCharsInRow);
		                    hItem2 = new HashMap<String, Object>();
		                    hItem2.put("GroupPageIDX", groupPageIDX);
		                    hItem2.put("TChat", o2.get((Object)"Feature", ""));
		                    hItem2.put("STT", o2.get((Object)"STT", ""));
		                    hItem2.put("MHHDVu", o2.get((Object)"ProductCode", ""));
		                    hItem2.put("THHDVu", productName);
		                    if (!"".equals(sLo)) {
		                        hItem2.put("SLo", sLo);
		                    }
		                    if (!"".equals(hanSD)) {
		                        hItem2.put("HanSD", hanSD);
		                    }
		                    hItem2.put("DVTinh", o2.get((Object)"Unit", ""));
		                    hItem2.put("SLuong", o2.get((Object)"Quantity", Double.class));
		                    hItem2.put("DGia", o2.get((Object)"Price", Double.class));
		                    hItem2.put("ThTien", o2.get((Object)"Total", Double.class));
		                    hItem2.put("TSuat", this.commons.formatNumberReal(((Double)o2.get((Object)"VATRate", 0.0)).toString()) + "%");
		                    hItem2.put("VATAmount", o2.get((Object)"VATAmount", Double.class));
		                    hItem2.put("Amount", o2.get((Object)"Amount", Double.class));
		                    arrayData.add(hItem2);
		                    ++countPrd;
		                    if (startRowGroup >= numberRowInPage) {
		                        startRowGroup = 0;
		                        ++groupPageIDX;
		                    }
		                }
		                if (0 == startRowGroup && docEInvoiceDetail.getList((Object)"DSHHDVu", org.bson.Document.class).size() > 0) {
		                    --groupPageIDX;
		                }
		                if (startRowGroup > 0 || docEInvoiceDetail.getList((Object)"DSHHDVu", org.bson.Document.class).size() == 0) {
		                    for (int k = startRowGroup; k < numberRowInPage; ++k) {
		                        hItem2 = new HashMap<String, Object>();
		                        hItem2.put("ProdName", " ");
		                        hItem2.put("GroupPageIDX", groupPageIDX);
		                        arrayData.add(hItem2);
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






	
	
	
}
