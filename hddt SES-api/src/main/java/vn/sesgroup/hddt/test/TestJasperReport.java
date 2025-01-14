package vn.sesgroup.hddt.test;

import java.io.File;
import java.io.FileInputStream;
import java.time.LocalDate;
import java.time.temporal.ChronoField;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import org.apache.commons.lang3.StringUtils;
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
import vn.sesgroup.hddt.utility.Commons;
import vn.sesgroup.hddt.utility.Json;

public class TestJasperReport {
	public static void main(String[] args) throws Exception{
		Commons commons = new Commons();
		/*DOC DU LIEU XML*/
		String dir = "C:\\hddt-ses\\server\\xml\\0106323762";
		String fileNameXML = "61c7cad6f8b593616ce03cc7_006D249999BD7A4A2FA3689987BFE9C0F7.xml";
		fileNameXML = "61c7cad6f8b593616ce03cc7_signed.xml";
		
		File file = new File(dir, fileNameXML);
		org.w3c.dom.Document doc = commons.fileToDocument(file);
		
		String NLap = "";
		String KDLieu = "";
		Node nodeTmp = null;
		Node nodeSubTmp = null;
		NodeList nodeListTTKhac = null;
		int countPrd = 0;
		
		XPath xPath = XPathFactory.newInstance().newXPath();
		Node nodeHDon = (Node) xPath.evaluate("/KetQuaTraCuu/DuLieu/TDiep[last()]/DLieu/HDon", doc, XPathConstants.NODE);
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
		
		Map<String, Object> reportParams = new HashMap<String, Object>();
		if(null != localDateNLap) {
			reportParams.put("InvoiceDate", StringUtils.leftPad(String.valueOf(localDateNLap.get(ChronoField.DAY_OF_MONTH)), 2, "0"));
			reportParams.put("InvoiceMonth", StringUtils.leftPad(String.valueOf(localDateNLap.get(ChronoField.MONTH_OF_YEAR)), 2, "0"));
			reportParams.put("InvoiceYear", StringUtils.leftPad(String.valueOf(localDateNLap.get(ChronoField.YEAR)), 4, "0"));
		}
		
		reportParams.put("KHHDon", 
			commons.getTextFromNodeXML((Element) xPath.evaluate("KHMSHDon", nodeTTChung, XPathConstants.NODE))
			+ commons.getTextFromNodeXML((Element) xPath.evaluate("KHHDon", nodeTTChung, XPathConstants.NODE))
		);
		reportParams.put("SHDon",
			commons.addSpacingAfterLeter(
				commons.formatNumberBillInvoice(commons.getTextFromNodeXML((Element) xPath.evaluate("SHDon", nodeTTChung, XPathConstants.NODE)))
			)
		);
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
		
		reportParams.put("TTChungHTTToan", commons.getTextFromNodeXML((Element) xPath.evaluate("HTTToan", nodeTTChung, XPathConstants.NODE)));
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
		
		List<HashMap<String, Object>> arrayData = new ArrayList<>();
		HashMap<String, Object> hItem = null;
		
		KDLieu = "";
		nodeTmp = null;
		nodeSubTmp = null;
		nodeListTTKhac = null;
		countPrd = 0;
		if(nodeListHHDVu != null) {
			for(int i = 0; i < nodeListHHDVu.getLength(); i++) {
				nodeTmp = nodeListHHDVu.item(i);
				
				hItem = new HashMap<>();
				hItem.put("GroupPageIDX", 0);
				
				hItem.put("TChat", commons.getTextFromNodeXML((Element) xPath.evaluate("TChat", nodeTmp, XPathConstants.NODE)));
				hItem.put("STT", commons.getTextFromNodeXML((Element) xPath.evaluate("STT", nodeTmp, XPathConstants.NODE)));
				hItem.put("MHHDVu", commons.getTextFromNodeXML((Element) xPath.evaluate("MHHDVu", nodeTmp, XPathConstants.NODE)));
				hItem.put("THHDVu", commons.getTextFromNodeXML((Element) xPath.evaluate("THHDVu", nodeTmp, XPathConstants.NODE)));
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
				System.out.println(Json.serializer().toPrettyString(hItem));
				countPrd++;	
			}
		}
		
		if(countPrd < 20) {
			for(int i = countPrd; i< 20;i++) {
				hItem = new HashMap<>();
				hItem.put("GroupPageIDX", 0);
				arrayData.add(hItem);
			}
			
		}
		
		String path = "C:\\Users\\SB-NANG.NH\\JaspersoftWorkspace\\MyReports\\A-HDDT-NEW";
		String fileName = "INV-VAT-001.jrxml";
		
		File fileJP = new File(path, fileName);
		
		JRDataSource jds = null;
		jds = new JRBeanCollectionDataSource(arrayData);
		
//		Map<String, Object> reportParams = new HashMap<String, Object>();
		JasperReport jr = JasperCompileManager.compileReport(new FileInputStream(fileJP));
		JasperPrint jp = JasperFillManager.fillReport(jr, reportParams, jds);
		
		Exporter exporter = null;
		
		exporter = new JRPdfExporter();
		exporter.setExporterInput(new SimpleExporterInput(jp));
		exporter.setExporterOutput(new SimpleOutputStreamExporterOutput(new File("C:\\aaa.pdf")));
        SimplePdfExporterConfiguration configuration = new SimplePdfExporterConfiguration();
        configuration.setCreatingBatchModeBookmarks(true);
        exporter.setConfiguration(configuration);
        exporter.exportReport();
	}
}
