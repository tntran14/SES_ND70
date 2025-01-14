package vn.sesgroup.hddt.user.impl;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.time.LocalDate;
import java.time.temporal.ChronoField;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import org.w3c.dom.Attr;
import org.w3c.dom.Element;

import com.api.message.JSONRoot;
import com.api.message.Msg;
import com.api.message.MsgHeader;
import com.api.message.MsgPage;
import com.fasterxml.jackson.databind.JsonNode;

import net.sf.jasperreports.engine.JRDataSource;
import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;
import net.sf.jasperreports.engine.export.HtmlExporter;
import net.sf.jasperreports.engine.export.JRPdfExporter;
import net.sf.jasperreports.export.Exporter;
import net.sf.jasperreports.export.SimpleExporterInput;
import net.sf.jasperreports.export.SimpleHtmlExporterOutput;
import net.sf.jasperreports.export.SimpleOutputStreamExporterOutput;
import net.sf.jasperreports.export.SimplePdfExporterConfiguration;
import vn.sesgroup.hddt.dto.FileInfo;
import vn.sesgroup.hddt.user.dao.AbstractDAO;
import vn.sesgroup.hddt.user.dao.ReportSituationDAO;
import vn.sesgroup.hddt.user.service.JPUtils;
import vn.sesgroup.hddt.user.service.TCTNService;
import vn.sesgroup.hddt.utility.Constants;
import vn.sesgroup.hddt.utility.Json;
import vn.sesgroup.hddt.utility.MailUtils;
import vn.sesgroup.hddt.utility.SystemParams;

@Repository
@Transactional
public class ReportSituationImpl extends AbstractDAO implements ReportSituationDAO {
	private static final Logger log = LogManager.getLogger(EInvoiceImpl.class);
	@Autowired
	MongoTemplate mongoTemplate;
	@Autowired
	TCTNService tctnService;
	private MailUtils mailUtils = new MailUtils();
	@Autowired
	JPUtils jpUtils;
	
	
	
	@SuppressWarnings({ "unchecked" })
	@Override
	public FileInfo viewReport(JSONRoot jsonRoot) throws Exception {
		FileInfo fileInfo = new FileInfo();
		
		Msg msg = jsonRoot.getMsg();
		MsgHeader header = msg.getMsgHeader();
		MsgPage page = msg.getMsgPage();
		Object objData = msg.getObjData();
		JsonNode jsonData = null;
		if (objData != null) {
			jsonData = Json.serializer().nodeFromObject(msg.getObjData());
		} else {
			throw new Exception("Lỗi dữ liệu đầu vào");
		}
				
			HashMap<String, String> hInput = (HashMap<String, String>) msg.getObjData();
			String type = null == hInput.get("type")? "": hInput.get("type");
			String typeExport = null == hInput.get("typeExport")? "html": hInput.get("typeExport");
			
						
			String loginRes = header.getUserName();
			switch (type) {
			case "ReportSituationUseInvoice":
				switch (typeExport) {
				case "xml":
					fileInfo = processReportSituationUseInvoiceXML(loginRes, jsonRoot);
					break;
				default:
					fileInfo = processReportSituationUseInvoice(loginRes, jsonRoot);
					break;
				}				
				break;
			default:
				return fileInfo;			
			}
			return fileInfo;
		}
	

	


	private FileInfo processReportSituationUseInvoiceXML(String loginRes, JSONRoot jsonRoot) throws Exception{
		FileInfo fileInfo = new FileInfo();
		Iterable<Document> cursor = null;
		Iterator<Document> iter = null;
		List<Document> pipeline = new ArrayList<Document>();
			Document docTmp = null;
		try {
			Msg msg = jsonRoot.getMsg();
			MsgHeader header = msg.getMsgHeader();
			MsgPage page = msg.getMsgPage();
			Object objData = msg.getObjData();
			@SuppressWarnings("unchecked")
			HashMap<String, String> hInput = (HashMap<String, String>) msg.getObjData();									
					try {
						int row = 0;
						/*KIEM TRA XEM MAU REPORT CO TON TAI KHONG*/
						File f = new File(SystemParams.DIR_E_INVOICE_TEMPLATE, "ReportSituationUseInvoice.jrxml");
						if(!(f.exists() && f.isFile())) {
							return fileInfo;
						}
						
						String quarterMonth = null == hInput.get("quarterMonth")? "": hInput.get("quarterMonth");
						String year = null == hInput.get("year")? "": hInput.get("year");
						String typeExport = null == hInput.get("typeExport")? "html": hInput.get("typeExport");
						
						String pQuarterMonth = "";
						LocalDate reportDateFrom = LocalDate.now();
						LocalDate reportDateTo = LocalDate.now();
						switch (quarterMonth) {
						case "Q1": 
							pQuarterMonth = "Quý 1";
							reportDateFrom = LocalDate.of(commons.stringToInteger(year), 1, 1);
							reportDateTo = LocalDate.of(commons.stringToInteger(year), 3, 1);
							reportDateTo = reportDateTo.with(TemporalAdjusters.lastDayOfMonth());
							break;
						case "Q2": 
							pQuarterMonth = "Quý 2";
							reportDateFrom = LocalDate.of(commons.stringToInteger(year), 4, 1);
							reportDateTo = LocalDate.of(commons.stringToInteger(year), 6, 1);
							reportDateTo = reportDateTo.with(TemporalAdjusters.lastDayOfMonth());
							break;
						case "Q3": 
							pQuarterMonth = "Quý 3";
							reportDateFrom = LocalDate.of(commons.stringToInteger(year), 7, 1);
							reportDateTo = LocalDate.of(commons.stringToInteger(year), 9, 1);
							reportDateTo = reportDateTo.with(TemporalAdjusters.lastDayOfMonth());
							break;
						case "Q4":
							pQuarterMonth = "Quý 4";
							reportDateFrom = LocalDate.of(commons.stringToInteger(year), 10, 1);
							reportDateTo = LocalDate.of(commons.stringToInteger(year), 12, 1);
							reportDateTo = reportDateTo.with(TemporalAdjusters.lastDayOfMonth());
							break;
						case "M1": 
							pQuarterMonth = "Tháng 1";
							reportDateFrom = LocalDate.of(commons.stringToInteger(year), 1, 1);
							reportDateTo = LocalDate.of(commons.stringToInteger(year), 1, 1);
							reportDateTo = reportDateTo.with(TemporalAdjusters.lastDayOfMonth());
							break;
						case "M2": 
							pQuarterMonth = "Tháng 2";
							reportDateFrom = LocalDate.of(commons.stringToInteger(year), 2, 1);
							reportDateTo = LocalDate.of(commons.stringToInteger(year), 2, 1);
							reportDateTo = reportDateTo.with(TemporalAdjusters.lastDayOfMonth());
							break;
						case "M3": 
							pQuarterMonth = "Tháng 3";
							reportDateFrom = LocalDate.of(commons.stringToInteger(year), 3, 1);
							reportDateTo = LocalDate.of(commons.stringToInteger(year), 3, 1);
							reportDateTo = reportDateTo.with(TemporalAdjusters.lastDayOfMonth());
							break;
						case "M4": 
							pQuarterMonth = "Tháng 4";
							reportDateFrom = LocalDate.of(commons.stringToInteger(year), 4, 1);
							reportDateTo = LocalDate.of(commons.stringToInteger(year), 4, 1);
							reportDateTo = reportDateTo.with(TemporalAdjusters.lastDayOfMonth());
							break;
						case "M5": 
							pQuarterMonth = "Tháng 5";
							reportDateFrom = LocalDate.of(commons.stringToInteger(year), 5, 1);
							reportDateTo = LocalDate.of(commons.stringToInteger(year), 5, 1);
							reportDateTo = reportDateTo.with(TemporalAdjusters.lastDayOfMonth());
							break;
						case "M6": 
							pQuarterMonth = "Tháng 6";
							reportDateFrom = LocalDate.of(commons.stringToInteger(year), 6, 1);
							reportDateTo = LocalDate.of(commons.stringToInteger(year), 6, 1);
							reportDateTo = reportDateTo.with(TemporalAdjusters.lastDayOfMonth());
							break;
						case "M7": 
							pQuarterMonth = "Tháng 7";
							reportDateFrom = LocalDate.of(commons.stringToInteger(year), 7, 1);
							reportDateTo = LocalDate.of(commons.stringToInteger(year), 7, 1);
							reportDateTo = reportDateTo.with(TemporalAdjusters.lastDayOfMonth());
							break;
						case "M8": 
							pQuarterMonth = "Tháng 8";
							reportDateFrom = LocalDate.of(commons.stringToInteger(year), 8, 1);
							reportDateTo = LocalDate.of(commons.stringToInteger(year), 8, 1);
							reportDateTo = reportDateTo.with(TemporalAdjusters.lastDayOfMonth());
							break;
						case "M9": 
							pQuarterMonth = "Tháng 9";
							reportDateFrom = LocalDate.of(commons.stringToInteger(year), 9, 1);
							reportDateTo = LocalDate.of(commons.stringToInteger(year), 9, 1);
							reportDateTo = reportDateTo.with(TemporalAdjusters.lastDayOfMonth());
							break;
						case "M10": 
							pQuarterMonth = "Tháng 10";
							reportDateFrom = LocalDate.of(commons.stringToInteger(year), 10, 1);
							reportDateTo = LocalDate.of(commons.stringToInteger(year), 10, 1);
							reportDateTo = reportDateTo.with(TemporalAdjusters.lastDayOfMonth());
							break;
						case "M11": 
							pQuarterMonth = "Tháng 11";
							reportDateFrom = LocalDate.of(commons.stringToInteger(year), 11, 1);
							reportDateTo = LocalDate.of(commons.stringToInteger(year), 11, 1);
							reportDateTo = reportDateTo.with(TemporalAdjusters.lastDayOfMonth());
							break;
						case "M12": 
							pQuarterMonth = "Tháng 12";
							reportDateFrom = LocalDate.of(commons.stringToInteger(year), 12, 1);
							reportDateTo = LocalDate.of(commons.stringToInteger(year), 12, 1);
							reportDateTo = reportDateTo.with(TemporalAdjusters.lastDayOfMonth());
							break;
						default:
							break;
						}
						
					//GET DATA TO DB		

						ObjectId objectId = null;
						ObjectId objectIdUser = null;			
						ObjectId objectIdEInvoice = null;
						objectId = null;
						try {
							objectId = new ObjectId(header.getIssuerId());
						} catch (Exception e) {
						}
						try {
							objectIdUser = new ObjectId(header.getUserId());
						} catch (Exception e) {
						}

				
						pipeline = new ArrayList<Document>();	
						pipeline.add(new Document("$match", new Document("_id", objectId).append("IsActive", true)
								.append("IsDelete", new Document("$ne", true))));		
						
						//USER INFO 
						pipeline.add(new Document("$match", new Document("_id", objectId).append("IsActive", true)
								.append("IsDelete", new Document("$ne", true))));
						pipeline.add(new Document("$lookup", new Document("from", "Users").append("pipeline", Arrays.asList(
								new Document("$match",
										new Document("IssuerId", header.getIssuerId()).append("_id", objectIdUser)
												.append("IsActive", true).append("IsDelete", new Document("$ne", true))),
							
								new Document("$limit", 1))).append("as", "UserInfo")));
						pipeline.add(new Document("$unwind",
								new Document("path", "$UserInfo").append("preserveNullAndEmptyArrays", true)));
						//DM Depot
						pipeline.add(new Document("$lookup", new Document("from", "DMDepot").append("pipeline", Arrays.asList(
								new Document("$match",
										new Document("TaxCode", header.getUserName())
										.append("IsDelete", new Document("$ne", true)))
								
							)).append("as", "DMDepot")));
						pipeline.add(new Document("$unwind",
								new Document("path", "$DMDepot").append("preserveNullAndEmptyArrays", true)));
										
						//DM Quantity
						pipeline.add(new Document("$lookup",
						new Document("from", "DMQuantity")
							.append("pipeline",
							Arrays.asList(new Document("$match", new Document("IssuerId", header.getIssuerId())
							.append("IsDelete", new Document("$ne", true))
							.append("$and", Arrays.asList(new Document("$and", Arrays.asList(new Document("NLap", new Document("$gte", reportDateFrom)
									.append("$lt", reportDateTo))							
											))))
									),																
							new Document("$project", new Document("_id", 1).append("SoLuong", 1).append("TuSo", 1).append("DenSo", 1).append("NLap", 1).append("KHMSHDon", 1).append("KHHDon", 1))
							))
							.append("as", "DMQuantity")));
							// EInvoice
						
						//DM EIvoice
						pipeline.add(new Document("$lookup",
						new Document("from", "EInvoice")
							.append("pipeline",
							Arrays.asList(new Document("$match", new Document("IssuerId", header.getIssuerId())
							.append("IsDelete", new Document("$ne", true))
							.append("SignStatusCode", Constants.INVOICE_SIGN_STATUS.SIGNED)
							.append("$and", Arrays.asList(new Document("$and", Arrays.asList(new Document("EInvoiceDetail.TTChung.NLap", new Document("$gte", reportDateFrom)
									.append("$lt", reportDateTo))								
											))))
									),
									new Document("$sort", new Document("EInvoiceDetail.TTChung.SHDon", 1))
									
//								new Document("$project", new Document("_id", 1).append("TTChung", "$EInvoiceDetail.TTChung").append("SoLuong", 1).append("NLap", 1))
							)									
						)							
							.append("as", "EInvoice")));		
							
						//DMMauSoKyHieu
						pipeline.add(new Document("$lookup",
								new Document("from", "DMMauSoKyHieu")
									.append("pipeline",
									Arrays.asList(new Document("$match", new Document("IssuerId", header.getIssuerId())
											.append("IsDelete", new Document("$ne", true))
											.append("IsActive", true))									
									))
									.append("as", "DMMauSoKyHieu")));
						
						cursor = mongoTemplate.getCollection("Issuer").aggregate(pipeline);
						iter = cursor.iterator();
						if (iter.hasNext()) {
							docTmp = iter.next();
						}
						
						
						///GET INFOR SOFT BY NUMBER ON THE FILE XML
						//DATA USER INFOR
						String taxCode = docTmp.getString("TaxCode");
						String Name = docTmp.getString("Name");
						String Address = docTmp.getString("Address");
						String Phone = docTmp.getString("Phone");
						String Fax = docTmp.getString("Fax");
						String Email = docTmp.getString("Email");
						
						//DATA MAU SO KY HIEU
						 String KHMSHDMSKHieu = "";
						 List<Document> MauSoKHieu = null;
						    if (docTmp.get("DMMauSoKyHieu") != null ) {
						    	MauSoKHieu = docTmp.getList("DMMauSoKyHieu", Document.class);
						    }
						//DATA EINVOICE
						    List<Document> EInvoice = null;
						    if (docTmp.get("EInvoice") != null ) {
						    	EInvoice = docTmp.getList("EInvoice", Document.class);
						    }   
						    
						//DATA DEPOT
						int SLDeport = docTmp.getEmbedded(Arrays.asList("DMDepot", "SLHDon"), 0);
						int SLHDonDDDepot = docTmp.getEmbedded(Arrays.asList("DMDepot", "SLHDonDD"), 0); 
						int SLHDonCLDepot =	docTmp.getEmbedded(Arrays.asList("DMDepot", "SLHDonCL"), 0);
						//DATA QUANTITY
						List<Document> Quantity = null;
					    if (docTmp.get("DMQuantity") != null ) {
					    	Quantity = docTmp.getList("DMQuantity", Document.class);
					    }
						int SLQuantity = 0;	
						int SLQuantity1 = 0;	
						int SLQuantity2 = 0;	
						String KHMSHDonQuantity = "";
						String TLHDon = "";
						int tongQuantity= 0;
						int tongQuantity1= 0;
						int tongQuantity2= 0;
						
						for(int i=0; i<Quantity.size();i++) {
							
							KHMSHDonQuantity = (String) Quantity.get(i).get("KHMSHDon");
							
							if(KHMSHDonQuantity.equals("1")) {
								SLQuantity = (int) Quantity.get(i).get("SoLuong");
								tongQuantity = tongQuantity + SLQuantity;
								
							}if(KHMSHDonQuantity.equals("2")) {
								SLQuantity1 = (int) Quantity.get(i).get("SoLuong");
								tongQuantity1 = tongQuantity1 + SLQuantity1;
								
							}if(KHMSHDonQuantity.equals("6")) {
								SLQuantity2 = (int) Quantity.get(i).get("SoLuong");
								tongQuantity2 = tongQuantity2 + SLQuantity2;								
							}														
						}
						//1: NAME EINVOICE
						
						// TONG SO HOA DON VAT
						int TongVAT = SLDeport - tongQuantity;
						
						//4: SO TON DAU KY: TONG SO, TU SO, DEN SO, MUA PHAT HANH TRONG KY: TU SO, DEN SO	
						
					////MAP DATA TO XML	
						String companyInfo = loginRes;
						DocumentBuilderFactory dbf = null;
						DocumentBuilder db = null;
						org.w3c.dom.Document doc = null;
						Element root = null;

						Element elementContent = null;

						Element elementSubTmp = null;
						Element elementSubTmp01 = null;
						Element elementSubContent = null;
//						Element elementTmp = null;
						dbf = DocumentBuilderFactory.newInstance();
						db = dbf.newDocumentBuilder();
						doc = db.newDocument();
						doc.setXmlStandalone(true);

						root = doc.createElement("HSoThueDTu");
						doc.appendChild(root);						
						
						Attr attr = doc.createAttribute("xmlns");
						attr.setValue("http://kekhaithue.gdt.gov.vn/TKhaiThue");
						root.setAttributeNode(attr);
						
						attr = doc.createAttribute("xmlns:xsi");
						attr.setValue("http://www.w3.org/2001/XMLSchema-instance");
						root.setAttributeNode(attr);
						
						Element rootElement = doc.createElement("HSoKhaiThue");
				        root.appendChild(rootElement);
				        
				        Element elementTTinChung = doc.createElement("TTinChung");
				        rootElement.appendChild(elementTTinChung);
	
				        Element elementTTinDVu = doc.createElement("TTinDVu");
				        elementTTinChung.appendChild(elementTTinDVu);
				        
				        Element elementTmp = doc.createElement("maDVu");
				        elementTmp.appendChild(doc.createTextNode("HTKK"));
				        elementTTinDVu.appendChild(elementTmp);
				        elementTmp = doc.createElement("tenDVu");
				        elementTmp.appendChild(doc.createTextNode("HỖ TRỢ KÊ KHAI THUẾ"));
				        elementTTinDVu.appendChild(elementTmp);
				        elementTmp = doc.createElement("pbanDVu");
				        elementTmp.appendChild(doc.createTextNode("4.5.9"));
				        elementTTinDVu.appendChild(elementTmp);
				        elementTmp = doc.createElement("ttinNhaCCapDVu");
				        elementTmp.appendChild(doc.createTextNode(""));
				        elementTTinDVu.appendChild(elementTmp);
				        
				        Element elementTTinTKhaiThue = doc.createElement("TTinTKhaiThue");
				        elementTTinChung.appendChild(elementTTinTKhaiThue);
				        
				        Element elementTKhaiThue = doc.createElement("TKhaiThue");
				        elementTTinTKhaiThue.appendChild(elementTKhaiThue);
				        
				        elementTmp = doc.createElement("maTKhai");
				        elementTmp.appendChild(doc.createTextNode("102"));
				        elementTKhaiThue.appendChild(elementTmp);
				        elementTmp = doc.createElement("tenTKhai");
				        elementTmp.appendChild(doc.createTextNode("Báo cáo tình hình sử dụng hóa đơn (BC26/AC)"));
				        elementTKhaiThue.appendChild(elementTmp);				        
				        elementTmp = doc.createElement("moTaBMau");
				        elementTmp.appendChild(doc.createTextNode("(Ban hành kèm theo Thông tư số 39/2014/TT-BTC  ngày 31/3/2014 của Bộ Tài chính)"));
				        elementTKhaiThue.appendChild(elementTmp);
				        elementTmp = doc.createElement("pbanTKhaiXML");
				        elementTmp.appendChild(doc.createTextNode("2.0.8"));
				        elementTKhaiThue.appendChild(elementTmp);
				        elementTmp = doc.createElement("loaiTKhai");
				        elementTmp.appendChild(doc.createTextNode("C"));
				        elementTKhaiThue.appendChild(elementTmp);
				        elementTmp = doc.createElement("soLan");
				        elementTmp.appendChild(doc.createTextNode("0"));
				        elementTKhaiThue.appendChild(elementTmp);
	
				        Element elementKyKKhaiThue = doc.createElement("KyKKhaiThue");
				        elementTKhaiThue.appendChild(elementKyKKhaiThue);
				        
				        String kieuKy = quarterMonth.substring(0, 1);
				        elementTmp = doc.createElement("kieuKy");
				        elementTmp.appendChild(doc.createTextNode(kieuKy));
				        elementKyKKhaiThue.appendChild(elementTmp);
				        elementTmp = doc.createElement("kyKKhai");
				        elementTmp.appendChild(doc.createTextNode(quarterMonth.substring(1) + "/" + year));
				        elementKyKKhaiThue.appendChild(elementTmp);				        
				        elementTmp = doc.createElement("kyKKhaiTuNgay");
				        elementTmp.appendChild(doc.createTextNode(commons.formatLocalDateTimeToString(reportDateFrom, Constants.FORMAT_DATE.FORMAT_DATE_WEB)));
				        elementKyKKhaiThue.appendChild(elementTmp);
				        elementTmp = doc.createElement("kyKKhaiDenNgay");
				        elementTmp.appendChild(doc.createTextNode(commons.formatLocalDateTimeToString(reportDateTo, Constants.FORMAT_DATE.FORMAT_DATE_WEB)));
				        elementKyKKhaiThue.appendChild(elementTmp);
				        elementTmp = doc.createElement("kyKKhaiTuThang");
//				        elementTmp.appendChild(doc.createTextNode(" "));
				        elementKyKKhaiThue.appendChild(elementTmp);
				        elementTmp = doc.createElement("kyKKhaiDenThang");
//				        elementTmp.appendChild(doc.createTextNode(" "));
				        elementKyKKhaiThue.appendChild(elementTmp);
				        
				        elementTmp = doc.createElement("maCQTNoiNop");
				        elementTmp.appendChild(doc.createTextNode(""));
				        elementTKhaiThue.appendChild(elementTmp);				        
				        elementTmp = doc.createElement("tenCQTNoiNop");
				        elementTmp.appendChild(doc.createTextNode(""));
				        elementTKhaiThue.appendChild(elementTmp);
				        elementTmp = doc.createElement("ngayLapTKhai");
				        elementTmp.appendChild(doc.createTextNode(commons.formatLocalDateTimeToString(LocalDate.now(), "yyyy-MM-dd")));
				        elementTKhaiThue.appendChild(elementTmp);
				        
				        Element elementGiaHan = doc.createElement("GiaHan");
				        elementTKhaiThue.appendChild(elementGiaHan);
				        
				        elementTmp = doc.createElement("maLyDoGiaHan");
				        elementTmp.appendChild(doc.createTextNode(""));
				        elementGiaHan.appendChild(elementTmp);
				        elementTmp = doc.createElement("lyDoGiaHan");
				        elementTmp.appendChild(doc.createTextNode(""));
				        elementGiaHan.appendChild(elementTmp);
				        elementTmp = doc.createElement("nguoiKy");
				        elementTmp.setTextContent(("".equals(Name) || null == Name) ? " ": Name);
			            elementTKhaiThue.appendChild(elementTmp);
			            elementTmp = doc.createElement("ngayKy");
			            elementTmp.setTextContent(commons.formatLocalDateTimeToString(LocalDate.now(), "yyyy-MM-dd"));
			            elementTKhaiThue.appendChild(elementTmp);
			            elementTmp = doc.createElement("nganhNgheKD");
			            elementTmp.setTextContent("");
			            elementTKhaiThue.appendChild(elementTmp);
				        
				        
				        
				        Element elementNNT = doc.createElement("NNT");
				        elementTTinTKhaiThue.appendChild(elementNNT);
				        
			            elementTmp = doc.createElement("mst");
			            elementTmp.setTextContent((null == taxCode || "".equals(taxCode))? " ": taxCode);
			            elementNNT.appendChild(elementTmp);
			            elementTmp = doc.createElement("tenNNT");
			            elementTmp.setTextContent((null == Name || "".equals(Name))? " ": Name);
			            elementNNT.appendChild(elementTmp);
			            elementTmp = doc.createElement("dchiNNT");
			            elementTmp.setTextContent((null == Address || "".equals(Address))? " ": Address);
			            elementNNT.appendChild(elementTmp);
			            elementTmp = doc.createElement("phuongXa");
			            elementTmp.appendChild(doc.createTextNode(""));
			            elementNNT.appendChild(elementTmp);
			            elementTmp = doc.createElement("maHuyenNNT");
			            elementTmp.appendChild(doc.createTextNode(""));
			            elementNNT.appendChild(elementTmp);
			            
			            String sDchiNNT = companyInfo;
			            String diachi_Huyen = "";
			            String diachi_tinh = "";
			            if (!sDchiNNT.isEmpty()){
			                String[] _DC = null;
			                if (sDchiNNT.contains(",")){
			                    _DC = sDchiNNT.split(",");
			                    diachi_Huyen = _DC[_DC.length-2];
			                    diachi_tinh = _DC[_DC.length-1];
			                }else if (sDchiNNT.contains("-")){
			                    _DC = sDchiNNT.split("-");
			                    diachi_Huyen = _DC[_DC.length-2];
			                    diachi_tinh = _DC[_DC.length-1];
			                }
			            }
			            elementTmp = doc.createElement("tenHuyenNNT");
			            elementTmp.setTextContent(diachi_Huyen);
			            elementNNT.appendChild(elementTmp);
			            elementTmp = doc.createElement("maTinhNNT");
			            elementTmp.setTextContent("");
			            elementNNT.appendChild(elementTmp);
			            elementTmp = doc.createElement("tenTinhNNT");
			            elementTmp.setTextContent(diachi_tinh);
			            elementNNT.appendChild(elementTmp);
			            elementTmp = doc.createElement("dthoaiNNT");
			            elementTmp.appendChild(doc.createTextNode(""));
			            elementNNT.appendChild(elementTmp);
			            elementTmp = doc.createElement("faxNNT");
			            elementTmp.appendChild(doc.createTextNode(""));
			            elementNNT.appendChild(elementTmp);
			            elementTmp = doc.createElement("emailNNT");
			            elementTmp.appendChild(doc.createTextNode(""));
			            elementNNT.appendChild(elementTmp);
			            
			            Element elementCTieuTKhaiChinh = doc.createElement("CTieuTKhaiChinh");
			            rootElement.appendChild(elementCTieuTKhaiChinh);
			            
			            elementTmp = doc.createElement("kyBCaoCuoi");
			            elementTmp.appendChild(doc.createTextNode("0"));
			            elementCTieuTKhaiChinh.appendChild(elementTmp);
			            elementTmp = doc.createElement("chuyenDiaDiem");
			            elementTmp.appendChild(doc.createTextNode("0"));
			            elementCTieuTKhaiChinh.appendChild(elementTmp);
			            elementTmp = doc.createElement("ngayDauKyBC");
			            elementTmp.setTextContent(commons.formatLocalDateTimeToString(reportDateFrom, "yyyy-MM-dd"));
			            elementCTieuTKhaiChinh.appendChild(elementTmp);
			            elementTmp = doc.createElement("ngayCuoiKyBC");
			            elementTmp.setTextContent(commons.formatLocalDateTimeToString(reportDateTo, "yyyy-MM-dd"));
			            elementCTieuTKhaiChinh.appendChild(elementTmp);
			            
			            Element elementHoaDon = doc.createElement("HoaDon");
			            elementCTieuTKhaiChinh.appendChild(elementHoaDon);
			            
			            String sTongCongSoTonDKy = "";
			            String sTongCongSDung = "";
			            int _tongsotonDK = 0;
			            int _tongcongSD = 0;
			            
			            Element elementChiTiet = null;
			            Attr attrTmp = null;
			            List<Document> DMQuantity = null;
					    if (docTmp.get("DMQuantity") != null ) {
					    	DMQuantity = docTmp.getList("DMQuantity", Document.class);
					    }
						
					    for(int i=0; i<DMQuantity.size();i++) {
					    	
					    
					    	String KHMSHDon = "";
					    	String KHHDon = "";
					    	int SLQUANTITY = 0;
					    	int TSQUANTITY = 0;
					    	int DSQUANTITY = 0;
					    	String muaTrongKy_tuSo= "";
					    	String muaTrongKy_denSo = "";
					    	int soTonMuaTrKy_ts = 0;
					    	String soTonMuaTrKy_tongSo = "";
					    	KHMSHDon = (String) DMQuantity.get(i).getEmbedded(Arrays.asList("KHMSHDon"), "");	
					    	if("1".equals(KHMSHDon)) {
					    		
					    	
					    	KHHDon =  (String) DMQuantity.get(i).getEmbedded(Arrays.asList("KHHDon"), "");	
					    	SLQUANTITY = (int) DMQuantity.get(i).getEmbedded(Arrays.asList("SoLuong"), 0);	
					    	TSQUANTITY = (int) DMQuantity.get(i).getEmbedded(Arrays.asList("TuSo"), 0);	
					    	DSQUANTITY = (int) DMQuantity.get(i).getEmbedded(Arrays.asList("DenSo"), 0);	
					    	
					    	
					    	muaTrongKy_tuSo = String.valueOf(TSQUANTITY);
					    	muaTrongKy_denSo = String.valueOf(DSQUANTITY);
					    	soTonMuaTrKy_ts = (DSQUANTITY - TSQUANTITY) + 1;					    					    
					    	soTonMuaTrKy_tongSo = String.valueOf(soTonMuaTrKy_ts);
					    	
			            	elementChiTiet = doc.createElement("ChiTiet");
			                elementHoaDon.appendChild(elementChiTiet);			                
			                attrTmp = doc.createAttribute("id");
			                attrTmp.setValue("ID_" + (i+1));
			                elementChiTiet.setAttributeNode(attrTmp);
			                
			                // Tên loại hóa đơn
			                String sMaHoaDon = "";			     			     			               
			                if (KHMSHDon.isEmpty()) {
			                    elementTmp = doc.createElement("maHoaDon");
			                    elementTmp.appendChild(doc.createTextNode(" "));
			                    elementChiTiet.appendChild(elementTmp);
			                }else{
			                	elementTmp = doc.createElement("maHoaDon");
			                	elementTmp.appendChild(doc.createTextNode(KHMSHDon));
			                    elementChiTiet.appendChild(elementTmp);
			                }
			                
			                // Tên loại hóa đơn
			                String sTenLoaiHoaDon = "Hóa đơn giá trị gia tăng";
			                if (sTenLoaiHoaDon.isEmpty()) {
			                    elementTmp = doc.createElement("tenHDon");
			                    elementTmp.appendChild(doc.createTextNode(" "));
			                    elementChiTiet.appendChild(elementTmp);
			                }else{
			                	elementTmp = doc.createElement("tenHDon");
			                	elementTmp.appendChild(doc.createTextNode(sTenLoaiHoaDon));
			                    elementChiTiet.appendChild(elementTmp);
			                }
	
			                // Ký hiệu mẫu hóa đơn
			                String sKyHieuMauHoaDon = KHMSHDon;
			                if (sKyHieuMauHoaDon.isEmpty()){
			                    elementTmp = doc.createElement("kHieuMauHDon");
			                    elementTmp.appendChild(doc.createTextNode(" "));
			                    elementChiTiet.appendChild(elementTmp);
			                }else{
			                	elementTmp = doc.createElement("kHieuMauHDon");
			                	elementTmp.appendChild(doc.createTextNode(sKyHieuMauHoaDon));
			                    elementChiTiet.appendChild(elementTmp);
			                }
	
			                // Ký hiệu Hóa đơn
			                String sKyHieuHoaDon = KHHDon;
			                if (sKyHieuHoaDon.isEmpty()){
			                	elementTmp = doc.createElement("kHieuHDon");
			                	elementTmp.appendChild(doc.createTextNode(" "));
			                    elementChiTiet.appendChild(elementTmp);
			                }else{
			                	elementTmp = doc.createElement("kHieuHDon");
			                	elementTmp.appendChild(doc.createTextNode(sKyHieuHoaDon));
			                	elementChiTiet.appendChild(elementTmp);
			                }
			                
			                // Tổng số
			                elementTmp = doc.createElement("soTonMuaTrKy_tongSo");
		                	elementTmp.appendChild(doc.createTextNode(soTonMuaTrKy_tongSo));
		                	elementChiTiet.appendChild(elementTmp);
			                
			                // Số tồn đầu kỳ => Từ số
			                elementTmp = doc.createElement("soTonDauKy_tuSo");
		                	elementTmp.appendChild(doc.createTextNode(""));
		                    elementChiTiet.appendChild(elementTmp);
			                
			                // Số tồn đầu kỳ => Đến số
			                elementTmp = doc.createElement("soTonDauKy_denSo");
		                    elementTmp.appendChild(doc.createTextNode(""));
		                    elementChiTiet.appendChild(elementTmp);
			                
			                // Số mua/ phát hành trong kỳ => Từ số
			                elementTmp = doc.createElement("muaTrongKy_tuSo");
		                	elementTmp.appendChild(doc.createTextNode(muaTrongKy_tuSo));
		                	elementChiTiet.appendChild(elementTmp);
			                
			                // Số mua/ phát hành trong kỳ => Từ số
			                elementTmp = doc.createElement("muaTrongKy_denSo");
		                	elementTmp.appendChild(doc.createTextNode(muaTrongKy_denSo));
		                	elementChiTiet.appendChild(elementTmp);
			                
		                	int SLMSKH = 0;
		                	int CLMSKH = 0;
							for(int j=0;j<MauSoKHieu.size();j++) {
								
								String KHMSHDonMSKH = (String) MauSoKHieu.get(j).getEmbedded(Arrays.asList("KHMSHDon"), "");	
						    	String KHHDonMSKH =  (String) MauSoKHieu.get(j).getEmbedded(Arrays.asList("KHHDon"), "");	
						    	if(KHMSHDonMSKH.equals(KHMSHDon)&& KHHDonMSKH.equals(KHHDon)) {
						    		SLMSKH = (int) MauSoKHieu.get(j).getEmbedded(Arrays.asList("SoLuong"), 0);
						    		CLMSKH = (int) MauSoKHieu.get(j).getEmbedded(Arrays.asList("ConLai"), 0);
						    		break;
						    	}
							}
							
							int demEInvoice = 0;
							int demDelete = 0;
							String SHDCancel = "";
							int SHDonSD = 0;
							int SHDon = 0;
							int tamSHD = 0;
							int SHDonMax = 0;
							for(int k=0;k<EInvoice.size();k++) {
								
								String KHMSHDonEInvoice = (String) EInvoice.get(k).getEmbedded(Arrays.asList("EInvoiceDetail", "TTChung", "KHMSHDon"), "");	
						    	String KHHDonEinvoice =  (String) EInvoice.get(k).getEmbedded(Arrays.asList("EInvoiceDetail", "TTChung","KHHDon"), "");	
						    	String EIvoiceStatus = (String) EInvoice.get(k).getEmbedded(Arrays.asList("EInvoiceStatus"), "");	
						    	int SHDonTam = (int) EInvoice.get(k).getEmbedded(Arrays.asList("EInvoiceDetail", "TTChung","SHDon"), 0);
						    	if(KHMSHDonEInvoice.equals(KHMSHDon)&& KHHDonEinvoice.equals(KHHDon)) {
//						    		SHDonSD = (int) EInvoice.get(k).getEmbedded(Arrays.asList("EInvoiceDetail", "TTChung","SHDon"), 0);
						    		
						    		if(SHDonMax>SHDonTam) {
						    			SHDonMax = SHDonMax;
						    		}if(SHDonMax<SHDonTam) {
						    			SHDonMax = SHDonTam;
						    		}
						    		demEInvoice++;						    	
						    	}if(KHMSHDonEInvoice.equals(KHMSHDon)&& KHHDonEinvoice.equals(KHHDon) && "DELETED".equals(EIvoiceStatus)) {
//						    		SHDon = (int) EInvoice.get(k).getEmbedded(Arrays.asList("EInvoiceDetail", "TTChung", "SHDon"), 0);	
						    		SHDCancel = SHDCancel+ SHDonTam + ";";	
						    		demDelete++;
						    	}
							}
							String tongSoSuDung_denSo = String.valueOf(SHDonMax);
							String demEInvoices = "";
							String demSDSD = "";
							int dem = 0;
							String denDele = String.valueOf(demDelete);
							if(demDelete>0) {
								dem = demEInvoice - demDelete;
								demEInvoices = String.valueOf(dem);
							}else {
								demEInvoices = String.valueOf(demEInvoice);	
							}
							demSDSD = String.valueOf(demEInvoice);
//							int demSHDon = demEInvoice - SHDonSD;
//							String TSSHD = String.valueOf(SHDonSD);
//		                	String demSHDON = String.valueOf(demSHDon);
		                	int CongSSD = (SHDonMax - TSQUANTITY)+ 1;
		                	String tongSoSuDung_cong = String.valueOf(CongSSD);
		                	int tonCKyTS = CongSSD+ 1;
		                	int tonCuoiKy_sl = (DSQUANTITY -tonCKyTS)+ 1;
		                	String SHD_Delete = "";
		                	String TStonCKy = String.valueOf(tonCKyTS);
		                	String tonCuoiKy_soLuong = String.valueOf(tonCuoiKy_sl);
		                	if(SHDCancel!="") {
		                		SHD_Delete =  SHDCancel.substring(0, SHDCancel.length() - 1);
		                	}else {
		                		SHD_Delete = "";
		                	}
		                	
			                // Tổng số sử dụng, xóa bỏ, mất, hủy => Từ số
			                elementTmp = doc.createElement("tongSoSuDung_tuSo");
		                    elementTmp.appendChild(doc.createTextNode(muaTrongKy_tuSo));
		                    elementChiTiet.appendChild(elementTmp);
			                
			                // Tổng số sử dụng, xóa bỏ, mất, hủy => Đến số
			                elementTmp = doc.createElement("tongSoSuDung_denSo");
		                	elementTmp.appendChild(doc.createTextNode(tongSoSuDung_denSo));
		                	elementChiTiet.appendChild(elementTmp);
			                
			                // Tổng số sử dụng, xóa bỏ, mất, hủy => Cộng
			                elementTmp = doc.createElement("tongSoSuDung_cong");
		                	elementTmp.appendChild(doc.createTextNode(tongSoSuDung_cong));
		                	elementChiTiet.appendChild(elementTmp);
			                
			                // Số lượng đã sử dụng
			                elementTmp = doc.createElement("soDaSDung");
		                	elementTmp.appendChild(doc.createTextNode(demSDSD));
		                	elementChiTiet.appendChild(elementTmp);
			                
			                // Xóa bỏ => Số lượng
			                elementTmp = doc.createElement("xoaBo_soLuong");
		                	elementTmp.appendChild(doc.createTextNode(""));
		                    elementChiTiet.appendChild(elementTmp);
			                
			                // Xóa bỏ => Số
			                elementTmp = doc.createElement("xoaBo_so");
		                	elementTmp.appendChild(doc.createTextNode(""));
		                	elementChiTiet.appendChild(elementTmp);
			                
			                // Mất => Số lượng
			                elementTmp = doc.createElement("mat_soLuong");
		                	elementTmp.appendChild(doc.createTextNode(""));
		                	elementChiTiet.appendChild(elementTmp);
			                
			                // Mất => Số
			                elementTmp = doc.createElement("mat_so");
		                	elementTmp.appendChild(doc.createTextNode(""));
		                	elementChiTiet.appendChild(elementTmp);
			                
			                // Hủy => Số lượng
		                	elementTmp = doc.createElement("huy_soLuong");
		                    elementTmp.appendChild(doc.createTextNode(denDele));
		                    elementChiTiet.appendChild(elementTmp);
			                
			                // Hủy => Số
		                    elementTmp = doc.createElement("huy_so");
		                	elementTmp.appendChild(doc.createTextNode(SHD_Delete));
		                	elementChiTiet.appendChild(elementTmp);
			                
			                // Tồn cuối kỳ => Từ số
		                	elementTmp = doc.createElement("tonCuoiKy_tuSo");
		                	elementTmp.appendChild(doc.createTextNode(TStonCKy));
		                	elementChiTiet.appendChild(elementTmp);
			                
			                // Tồn cuối kỳ => Đến số
		                	elementTmp = doc.createElement("tonCuoiKy_denSo");
		                	elementTmp.appendChild(doc.createTextNode(muaTrongKy_denSo));
		                	elementChiTiet.appendChild(elementTmp);
			                
			                // Tồn cuối kỳ => Số lượng
		                	elementTmp = doc.createElement("tonCuoiKy_soLuong");
		                	elementTmp.appendChild(doc.createTextNode(tonCuoiKy_soLuong));
		                	elementChiTiet.appendChild(elementTmp);
					    	  
			                sTongCongSoTonDKy = "";
			                sTongCongSoTonDKy = soTonMuaTrKy_tongSo;
			                sTongCongSDung = demSDSD;
					    	} 
			                if (!sTongCongSoTonDKy.isEmpty()){
			                    _tongsotonDK += Integer.parseInt(sTongCongSoTonDKy);
			                }else{
			                    _tongsotonDK += 0;
			                }
			                
			                if (!sTongCongSDung.isEmpty()){
			                    _tongcongSD += Integer.parseInt(sTongCongSDung);
			                }else{
			                    _tongcongSD += 0;
			                }
			            }
			            
			            String _sTongCongSoTonDKy = _tongsotonDK + "";
			            String _sTongCongSDung = _tongcongSD + "";
			                
			            // tongCongSoTonDKy
			            if (_sTongCongSoTonDKy.isEmpty()){
			                elementTmp = doc.createElement("tongCongSoTonDKy");
			                elementTmp.appendChild(doc.createTextNode("0"));
			                elementCTieuTKhaiChinh.appendChild(elementTmp);
			            }else{
			            	elementTmp = doc.createElement("tongCongSoTonDKy");
			            	elementTmp.appendChild(doc.createTextNode(_sTongCongSoTonDKy));
			                elementCTieuTKhaiChinh.appendChild(elementTmp);
			            }
	
			            // tongCongSDung
			            if (_sTongCongSDung.isEmpty()){
			            	elementTmp = doc.createElement("tongCongSDung");
			            	elementTmp.appendChild(doc.createTextNode("0"));
			                elementCTieuTKhaiChinh.appendChild(elementTmp);
			            }else{
			            	elementTmp = doc.createElement("tongCongSDung");
			            	elementTmp.appendChild(doc.createTextNode(_sTongCongSDung));
			                elementCTieuTKhaiChinh.appendChild(elementTmp);
			            }
	
			            // tongCongSoTonCKy
			            String sTongCongSoTonCKy = "";
			            if (!_sTongCongSoTonDKy.isEmpty() || !_sTongCongSDung.isEmpty()){
			                int so1 = 0;
			                int so2 = 0;
			                if (!_sTongCongSoTonDKy.isEmpty()){
			                    so1 = Integer.parseInt(_sTongCongSoTonDKy);
			                }
			                if (!_sTongCongSDung.isEmpty()){
			                    so2 = Integer.parseInt(_sTongCongSDung);
			                }
	
			                int tong = so1 - so2;
			                sTongCongSoTonCKy = tong + "";
			            }
			            
			            if (sTongCongSoTonCKy.isEmpty()){
			            	elementTmp = doc.createElement("tongCongSoTonCKy");
			            	elementTmp.appendChild(doc.createTextNode("0"));
			                elementCTieuTKhaiChinh.appendChild(elementTmp);
			            }else{
			            	elementTmp = doc.createElement("tongCongSoTonCKy");
			            	elementTmp.appendChild(doc.createTextNode(sTongCongSoTonCKy));
			                elementCTieuTKhaiChinh.appendChild(elementTmp);
			            }
	
			            // nguoiLapBieu
			            String sNguoiLapBieu = Name;
			            if (sNguoiLapBieu.isEmpty()){
			            	elementTmp = doc.createElement("nguoiLapBieu");
			            	elementTmp.appendChild(doc.createTextNode(""));
			                elementCTieuTKhaiChinh.appendChild(elementTmp);
			            }else{
			            	elementTmp = doc.createElement("nguoiLapBieu");
			            	elementTmp.appendChild(doc.createTextNode(""));
			                elementCTieuTKhaiChinh.appendChild(elementTmp);
			            }
	
			            // nguoiDaiDien
			            String sNguoiDaiDien = Name;
			            if (sNguoiDaiDien.isEmpty()){
			            	elementTmp = doc.createElement("nguoiDaiDien");
			            	elementTmp.appendChild(doc.createTextNode(""));
			                elementCTieuTKhaiChinh.appendChild(elementTmp);
			            }
			            else{
			            	elementTmp = doc.createElement("nguoiDaiDien");
			            	elementTmp.appendChild(doc.createTextNode(""));
			            	elementCTieuTKhaiChinh.appendChild(elementTmp);
			            }
			            
			            elementTmp = doc.createElement("ngayBCao");
			            elementTmp.appendChild(doc.createTextNode(commons.formatLocalDateTimeToString(LocalDate.now(), "yyyy-MM-dd")));
			            elementCTieuTKhaiChinh.appendChild(elementTmp);
						
						ByteArrayOutputStream out = new ByteArrayOutputStream();
						TransformerFactory tf = TransformerFactory.newInstance();
						Transformer trans = tf.newTransformer();
						trans.transform(new DOMSource(doc), new StreamResult(out));
						
						fileInfo.setContentFile(out.toByteArray());
						
						return fileInfo;
					  
					}catch(Exception e) {
						e.printStackTrace();
						
						return fileInfo;
					}								
			
		}catch(Exception e) {
			throw e;
		}
	}
	
	@SuppressWarnings("unchecked")
	private FileInfo processReportSituationUseInvoice(String loginRes, JSONRoot jsonRoot) throws Exception{
		FileInfo fileInfo = new FileInfo();
		Iterable<Document> cursor = null;
		Iterator<Document> iter = null;
		List<Document> pipeline = new ArrayList<Document>();
			Document docTmp = null;
			Msg msg = jsonRoot.getMsg();
			MsgHeader header = msg.getMsgHeader();
			MsgPage page = msg.getMsgPage();
			Object objData = msg.getObjData();
			JsonNode jsonData = null;
			if (objData != null) {
				jsonData = Json.serializer().nodeFromObject(msg.getObjData());
			} else {
				throw new Exception("Lỗi dữ liệu đầu vào");
			}
			
			HashMap<String, String> hInput = (HashMap<String, String>) msg.getObjData();						
				try {
						int row = 0;
						/*KIEM TRA XEM MAU REPORT CO TON TAI KHONG*/
						File f = new File(SystemParams.DIR_E_INVOICE_TEMPLATE, "ReportSituationUseInvoice.jrxml");
						if(!(f.exists() && f.isFile())) {
							return fileInfo;
						}						
						String quarterMonth = null == hInput.get("quarterMonth")? "": hInput.get("quarterMonth");
						String year = null == hInput.get("year")? "": hInput.get("year");
						String typeExport = null == hInput.get("typeExport")? "html": hInput.get("typeExport");
						
						String pQuarterMonth = "";
						LocalDate reportDateFrom = LocalDate.now();
						LocalDate reportDateTo = LocalDate.now();
						switch (quarterMonth) {
						case "Q1": 
							pQuarterMonth = "Quý 1";
							reportDateFrom = LocalDate.of(commons.stringToInteger(year), 1, 1);
							reportDateTo = LocalDate.of(commons.stringToInteger(year), 3, 1);
							reportDateTo = reportDateTo.with(TemporalAdjusters.lastDayOfMonth());
							break;
						case "Q2": 
							pQuarterMonth = "Quý 2";
							reportDateFrom = LocalDate.of(commons.stringToInteger(year), 4, 1);
							reportDateTo = LocalDate.of(commons.stringToInteger(year), 6, 1);
							reportDateTo = reportDateTo.with(TemporalAdjusters.lastDayOfMonth());
							break;
						case "Q3": 
							pQuarterMonth = "Quý 3";
							reportDateFrom = LocalDate.of(commons.stringToInteger(year), 7, 1);
							reportDateTo = LocalDate.of(commons.stringToInteger(year), 9, 1);
							reportDateTo = reportDateTo.with(TemporalAdjusters.lastDayOfMonth());
							break;
						case "Q4":
							pQuarterMonth = "Quý 4";
							reportDateFrom = LocalDate.of(commons.stringToInteger(year), 10, 1);
							reportDateTo = LocalDate.of(commons.stringToInteger(year), 12, 1);
							reportDateTo = reportDateTo.with(TemporalAdjusters.lastDayOfMonth());
							break;
						case "M1": 
							pQuarterMonth = "Tháng 1";
							reportDateFrom = LocalDate.of(commons.stringToInteger(year), 1, 1);
							reportDateTo = LocalDate.of(commons.stringToInteger(year), 1, 1);
							reportDateTo = reportDateTo.with(TemporalAdjusters.lastDayOfMonth());
							break;
						case "M2": 
							pQuarterMonth = "Tháng 2";
							reportDateFrom = LocalDate.of(commons.stringToInteger(year), 2, 1);
							reportDateTo = LocalDate.of(commons.stringToInteger(year), 2, 1);
							reportDateTo = reportDateTo.with(TemporalAdjusters.lastDayOfMonth());
							break;
						case "M3": 
							pQuarterMonth = "Tháng 3";
							reportDateFrom = LocalDate.of(commons.stringToInteger(year), 3, 1);
							reportDateTo = LocalDate.of(commons.stringToInteger(year), 3, 1);
							reportDateTo = reportDateTo.with(TemporalAdjusters.lastDayOfMonth());
							break;
						case "M4": 
							pQuarterMonth = "Tháng 4";
							reportDateFrom = LocalDate.of(commons.stringToInteger(year), 4, 1);
							reportDateTo = LocalDate.of(commons.stringToInteger(year), 4, 1);
							reportDateTo = reportDateTo.with(TemporalAdjusters.lastDayOfMonth());
							break;
						case "M5": 
							pQuarterMonth = "Tháng 5";
							reportDateFrom = LocalDate.of(commons.stringToInteger(year), 5, 1);
							reportDateTo = LocalDate.of(commons.stringToInteger(year), 5, 1);
							reportDateTo = reportDateTo.with(TemporalAdjusters.lastDayOfMonth());
							break;
						case "M6": 
							pQuarterMonth = "Tháng 6";
							reportDateFrom = LocalDate.of(commons.stringToInteger(year), 6, 1);
							reportDateTo = LocalDate.of(commons.stringToInteger(year), 6, 1);
							reportDateTo = reportDateTo.with(TemporalAdjusters.lastDayOfMonth());
							break;
						case "M7": 
							pQuarterMonth = "Tháng 7";
							reportDateFrom = LocalDate.of(commons.stringToInteger(year), 7, 1);
							reportDateTo = LocalDate.of(commons.stringToInteger(year), 7, 1);
							reportDateTo = reportDateTo.with(TemporalAdjusters.lastDayOfMonth());
							break;
						case "M8": 
							pQuarterMonth = "Tháng 8";
							reportDateFrom = LocalDate.of(commons.stringToInteger(year), 8, 1);
							reportDateTo = LocalDate.of(commons.stringToInteger(year), 8, 1);
							reportDateTo = reportDateTo.with(TemporalAdjusters.lastDayOfMonth());
							break;
						case "M9": 
							pQuarterMonth = "Tháng 9";
							reportDateFrom = LocalDate.of(commons.stringToInteger(year), 9, 1);
							reportDateTo = LocalDate.of(commons.stringToInteger(year), 9, 1);
							reportDateTo = reportDateTo.with(TemporalAdjusters.lastDayOfMonth());
							break;
						case "M10": 
							pQuarterMonth = "Tháng 10";
							reportDateFrom = LocalDate.of(commons.stringToInteger(year), 10, 1);
							reportDateTo = LocalDate.of(commons.stringToInteger(year), 10, 1);
							reportDateTo = reportDateTo.with(TemporalAdjusters.lastDayOfMonth());
							break;
						case "M11": 
							pQuarterMonth = "Tháng 11";
							reportDateFrom = LocalDate.of(commons.stringToInteger(year), 11, 1);
							reportDateTo = LocalDate.of(commons.stringToInteger(year), 11, 1);
							reportDateTo = reportDateTo.with(TemporalAdjusters.lastDayOfMonth());
							break;
						case "M12": 
							pQuarterMonth = "Tháng 12";
							reportDateFrom = LocalDate.of(commons.stringToInteger(year), 12, 1);
							reportDateTo = LocalDate.of(commons.stringToInteger(year), 12, 1);
							reportDateTo = reportDateTo.with(TemporalAdjusters.lastDayOfMonth());
							break;
						default:
							break;
						}
						ObjectId objectId = null;
						ObjectId objectIdUser = null;			
						ObjectId objectIdEInvoice = null;
						objectId = null;
						try {
							objectId = new ObjectId(header.getIssuerId());
						} catch (Exception e) {
						}
						try {
							objectIdUser = new ObjectId(header.getUserId());
						} catch (Exception e) {
						}

				
						pipeline = new ArrayList<Document>();	
						pipeline.add(new Document("$match", new Document("_id", objectId).append("IsActive", true)
								.append("IsDelete", new Document("$ne", true))));		
						
						//USER INFO 
						pipeline.add(new Document("$match", new Document("_id", objectId).append("IsActive", true)
								.append("IsDelete", new Document("$ne", true))));
						pipeline.add(new Document("$lookup", new Document("from", "Users").append("pipeline", Arrays.asList(
								new Document("$match",
										new Document("IssuerId", header.getIssuerId()).append("_id", objectIdUser)
												.append("IsActive", true).append("IsDelete", new Document("$ne", true))),
							
								new Document("$limit", 1))).append("as", "UserInfo")));
						pipeline.add(new Document("$unwind",
								new Document("path", "$UserInfo").append("preserveNullAndEmptyArrays", true)));
						//DM Depot
						pipeline.add(new Document("$lookup", new Document("from", "DMDepot").append("pipeline", Arrays.asList(
								new Document("$match",
										new Document("TaxCode", header.getUserName())
										.append("IsDelete", new Document("$ne", true)))
								
							)).append("as", "DMDepot")));
						pipeline.add(new Document("$unwind",
								new Document("path", "$DMDepot").append("preserveNullAndEmptyArrays", true)));
										
						//DM Quantity
						pipeline.add(new Document("$lookup",
						new Document("from", "DMQuantity")
							.append("pipeline",
							Arrays.asList(new Document("$match", new Document("IssuerId", header.getIssuerId())
							.append("IsDelete", new Document("$ne", true))
							.append("$and", Arrays.asList(new Document("$and", Arrays.asList(new Document("NLap", new Document("$gte", reportDateFrom)
									.append("$lt", reportDateTo))							
											))))
									),																
							new Document("$project", new Document("_id", 1).append("SoLuong", 1).append("TuSo", 1).append("DenSo", 1).append("NLap", 1).append("KHMSHDon", 1).append("KHHDon", 1))
							))
							.append("as", "DMQuantity")));
							// EInvoice
						
						//DM EIvoice
						pipeline.add(new Document("$lookup",
						new Document("from", "EInvoice")
							.append("pipeline",
							Arrays.asList(new Document("$match", new Document("IssuerId", header.getIssuerId())
							.append("IsDelete", new Document("$ne", true))
							.append("SignStatusCode", Constants.INVOICE_SIGN_STATUS.SIGNED)
							.append("$and", Arrays.asList(new Document("$and", Arrays.asList(new Document("EInvoiceDetail.TTChung.NLap", new Document("$gte", reportDateFrom)
									.append("$lt", reportDateTo))								
											))))
					
									),
									new Document("$sort", new Document("EInvoiceDetail.TTChung.SHDon", 1))
//								new Document("$project", new Document("_id", 1).append("TTChung", "$EInvoiceDetail.TTChung").append("SoLuong", 1).append("NLap", 1))
							))
							.append("as", "EInvoice")));
					
						//DMMauSoKyHieu
						pipeline.add(new Document("$lookup",
								new Document("from", "DMMauSoKyHieu")
									.append("pipeline",
									Arrays.asList(new Document("$match", new Document("IssuerId", header.getIssuerId())
											.append("IsDelete", new Document("$ne", true))
											.append("IsActive", true))									
									))
									.append("as", "DMMauSoKyHieu")));
						
						cursor = mongoTemplate.getCollection("Issuer").aggregate(pipeline);
						iter = cursor.iterator();
						if (iter.hasNext()) {
							docTmp = iter.next();
						}
						
						
						///GET INFOR SOFT BY NUMBER ON THE FILE XML
						//DATA USER INFOR
						String taxCode = docTmp.getString("TaxCode");
						String Name = docTmp.getString("Name");
						String Address = docTmp.getString("Address");
						String Phone = docTmp.getString("Phone");
						String Fax = docTmp.getString("Fax");
						String Email = docTmp.getString("Email");
						
						//DATA MAU SO KY HIEU
						 String KHMSHDMSKHieu = "";
						 List<Document> MauSoKHieu = null;
						    if (docTmp.get("DMMauSoKyHieu") != null ) {
						    	MauSoKHieu = docTmp.getList("DMMauSoKyHieu", Document.class);
						    }
						//DATA EINVOICE
						    List<Document> EInvoice = null;
						    if (docTmp.get("EInvoice") != null ) {
						    	EInvoice = docTmp.getList("EInvoice", Document.class);
						    }   
						    
						//DATA DEPOT
						int SLDeport = docTmp.getEmbedded(Arrays.asList("DMDepot", "SLHDon"), 0);
						int SLHDonDDDepot = docTmp.getEmbedded(Arrays.asList("DMDepot", "SLHDonDD"), 0); 
						int SLHDonCLDepot =	docTmp.getEmbedded(Arrays.asList("DMDepot", "SLHDonCL"), 0);
						//DATA QUANTITY
						List<Document> Quantity = null;
					    if (docTmp.get("DMQuantity") != null ) {
					    	Quantity = docTmp.getList("DMQuantity", Document.class);
					    }
						int SLQuantity = 0;	
						int SLQuantity1 = 0;	
						int SLQuantity2 = 0;	
						String KHMSHDonQuantity = "";
						String TLHDon = "";
						int tongQuantity= 0;
						int tongQuantity1= 0;
						int tongQuantity2= 0;
						
						for(int i=0; i<Quantity.size();i++) {
							
							KHMSHDonQuantity = (String) Quantity.get(i).get("KHMSHDon");
							
							if(KHMSHDonQuantity.equals("1")) {
								SLQuantity = (int) Quantity.get(i).get("SoLuong");
								tongQuantity = tongQuantity + SLQuantity;
								
							}if(KHMSHDonQuantity.equals("2")) {
								SLQuantity1 = (int) Quantity.get(i).get("SoLuong");
								tongQuantity1 = tongQuantity1 + SLQuantity1;
								
							}if(KHMSHDonQuantity.equals("6")) {
								SLQuantity2 = (int) Quantity.get(i).get("SoLuong");
								tongQuantity2 = tongQuantity2 + SLQuantity2;								
							}														
						}
						
						
						///GET INFOR SOFT BY NUMBER ON THE FILE JRXML				
						//1: NAME EINVOICE
						
						
						//2: KI HIEU MAU SO HOA DON
						//3: KI HIEU HOA DON
						//4: SO TON DAU KY: TONG SO, TU SO, DEN SO, MUA PHAT HANH TRONG KY: TU SO, DEN SO
						Map<String, Object> reportParams = new HashMap<String, Object>();		
						List<HashMap<String, Object>> arrayData = new ArrayList<>();
						LocalDate localDateNLap = LocalDate.now();
						HashMap<String, Object> hItem = null;
						reportParams.put("KTTQuy", pQuarterMonth);
						reportParams.put("KTTYear", year);
						reportParams.put("TCCNTen", Name);
						reportParams.put("TCCNMST", taxCode);
						reportParams.put("TCCNDChi", Address);
						reportParams.put("BCDauKyDate", commons.formatLocalDateTimeToString(reportDateFrom, "dd/MM/yyyy"));
						reportParams.put("BCCuoiKyDate", commons.formatLocalDateTimeToString(reportDateTo, "dd/MM/yyyy"));
						reportParams.put("InvoiceDate", StringUtils.leftPad(String.valueOf(localDateNLap.get(ChronoField.DAY_OF_MONTH)), 2, "0"));
						reportParams.put("InvoiceMonth", StringUtils.leftPad(String.valueOf(localDateNLap.get(ChronoField.MONTH_OF_YEAR)), 2, "0"));
						reportParams.put("InvoiceYear", StringUtils.leftPad(String.valueOf(localDateNLap.get(ChronoField.YEAR)), 4, "0"));

						
						   for(int i=0; i<Quantity.size();i++) {
						   
						   String KHMSHDon = "";
					    	String KHHDon = "";
					    	int SLQUANTITY = 0;
					    	int TSQUANTITY = 0;
					    	int DSQUANTITY = 0;
					    	String muaTrongKy_tuSo= "";
					    	String muaTrongKy_denSo = "";
					    	int soTonMuaTrKy_ts = 0;
					    	String soTonMuaTrKy_tongSo = "";
					    	KHMSHDon = (String) Quantity.get(i).getEmbedded(Arrays.asList("KHMSHDon"), "");	
					    	if("1".equals(KHMSHDon)) {
					    		
					    	
					    	KHHDon =  (String) Quantity.get(i).getEmbedded(Arrays.asList("KHHDon"), "");	
					    	SLQUANTITY = (int) Quantity.get(i).getEmbedded(Arrays.asList("SoLuong"), 0);	
					    	TSQUANTITY = (int) Quantity.get(i).getEmbedded(Arrays.asList("TuSo"), 0);	
					    	DSQUANTITY = (int) Quantity.get(i).getEmbedded(Arrays.asList("DenSo"), 0);	
					    	
					    	
					    	muaTrongKy_tuSo = String.valueOf(TSQUANTITY);
					    	muaTrongKy_denSo = String.valueOf(DSQUANTITY);
					    	soTonMuaTrKy_ts = (DSQUANTITY - TSQUANTITY) + 1;					    					    
					    	soTonMuaTrKy_tongSo = String.valueOf(soTonMuaTrKy_ts);
							
							
								int demEInvoice = 0;
							int demDelete = 0;
							String SHDCancel = "";
							int SHDonSD = 0;
							int SHDon = 0;
							int tamSHD = 0;
							int SHDonMax = 0;
							for(int k=0;k<EInvoice.size();k++) {
								
								String KHMSHDonEInvoice = (String) EInvoice.get(k).getEmbedded(Arrays.asList("EInvoiceDetail", "TTChung", "KHMSHDon"), "");	
						    	String KHHDonEinvoice =  (String) EInvoice.get(k).getEmbedded(Arrays.asList("EInvoiceDetail", "TTChung","KHHDon"), "");	
						    	String EIvoiceStatus = (String) EInvoice.get(k).getEmbedded(Arrays.asList("EInvoiceStatus"), "");	
						    	int SHDonTam = (int) EInvoice.get(k).getEmbedded(Arrays.asList("EInvoiceDetail", "TTChung","SHDon"), 0);
						    	if(KHMSHDonEInvoice.equals(KHMSHDon)&& KHHDonEinvoice.equals(KHHDon)) {
//						    		SHDonSD = (int) EInvoice.get(k).getEmbedded(Arrays.asList("EInvoiceDetail", "TTChung","SHDon"), 0);
						    		
						    		if(SHDonMax>SHDonTam) {
						    			SHDonMax = SHDonMax;
						    		}if(SHDonMax<SHDonTam) {
						    			SHDonMax = SHDonTam;
						    		}
						    		demEInvoice++;						    	
						    	}if(KHMSHDonEInvoice.equals(KHMSHDon)&& KHHDonEinvoice.equals(KHHDon) && "DELETED".equals(EIvoiceStatus)) {
//						    		SHDon = (int) EInvoice.get(k).getEmbedded(Arrays.asList("EInvoiceDetail", "TTChung", "SHDon"), 0);	
						    		SHDCancel = SHDCancel+ SHDonTam + ";";	
						    		demDelete++;
						    	}
							}
							String tongSoSuDung_denSo = String.valueOf(SHDonMax);
							String demEInvoices = "";
							String demSDSD = "";
							int dem = 0;
							String denDele = String.valueOf(demDelete);
							if(demDelete>0) {
								dem = demEInvoice - demDelete;
								demEInvoices = String.valueOf(dem);
							}else {
								demEInvoices = String.valueOf(demEInvoice);	
							}
							demSDSD = String.valueOf(demEInvoice);
//							int demSHDon = demEInvoice - SHDonSD;
//							String TSSHD = String.valueOf(SHDonSD);
//		                	String demSHDON = String.valueOf(demSHDon);
		                	int CongSSD = (SHDonMax - TSQUANTITY)+ 1;
		                	String tongSoSuDung_cong = String.valueOf(CongSSD);
		                	int tonCKyTS = CongSSD+ 1;
		                	int tonCuoiKy_sl = (DSQUANTITY -tonCKyTS)+ 1;
		                	String SHD_Delete = "";
		                	String TStonCKy = String.valueOf(tonCKyTS);
		                	String tonCuoiKy_soLuong = String.valueOf(tonCuoiKy_sl);
		                	if(SHDCancel!="") {
		                		SHD_Delete =  SHDCancel.substring(0, SHDCancel.length() - 1);
		                	}else {
		                		SHD_Delete = "";
		                	}
		                	hItem = new HashMap<>();
		                	String STT = String.valueOf(i+1); 
							  String sTenLoaiHoaDon = "Hóa đơn giá trị gia tăng";	
							  hItem.put("STT", STT);
							  hItem.put("LHDTen",sTenLoaiHoaDon);
							  hItem.put("KHMauHDon",KHMSHDon);
							  hItem.put("KHHDon",KHHDon);
							  hItem.put("TDKMPHTongSo",soTonMuaTrKy_tongSo);
							  hItem.put("TDKTuSo","");		
							  hItem.put("TDKDenSo", "");
							  hItem.put("MPHTuSo",muaTrongKy_tuSo);
							  hItem.put("MPHDenSo",muaTrongKy_denSo);
							  hItem.put("SDXBMHTuSo",muaTrongKy_tuSo);
							  hItem.put("SDXBMHDenSo",tongSoSuDung_denSo);
							  hItem.put("SDXBMHCong",tongSoSuDung_cong);		
							  hItem.put("SLSuDung", demSDSD);
							  hItem.put("XBSLuong","");
							  hItem.put("XBSo","");
							  hItem.put("MSLuong","");
							  hItem.put("MSo","");
							  hItem.put("HSLuong",denDele);		
							  hItem.put("HSo", SHD_Delete);
							  hItem.put("TCKTuSo",TStonCKy);
							  hItem.put("TCKDenSo",muaTrongKy_denSo);
							  hItem.put("TCKSLuong",tonCuoiKy_soLuong);								
							  arrayData.add(hItem);
						   }
		}
						JRDataSource jds = null;
						jds = new JRBeanCollectionDataSource(arrayData);
						
						ByteArrayOutputStream out = new ByteArrayOutputStream();
						JasperReport jr = JasperCompileManager.compileReport(new FileInputStream(f));
						JasperPrint jp = JasperFillManager.fillReport(jr, reportParams, jds);
						Exporter exporter = null;
						switch (typeExport) {
						case "pdf":
							exporter = new JRPdfExporter();
							exporter.setExporterInput(new SimpleExporterInput(jp));
							exporter.setExporterOutput(new SimpleOutputStreamExporterOutput(out));
					        SimplePdfExporterConfiguration configuration = new SimplePdfExporterConfiguration();
					        configuration.setCreatingBatchModeBookmarks(true);
					        exporter.setConfiguration(configuration);
					        exporter.exportReport();
							break;
						default:
							exporter = new HtmlExporter();						
							exporter.setExporterOutput(new SimpleHtmlExporterOutput(out));						
							exporter.setExporterInput(new SimpleExporterInput(jp));
							exporter.exportReport();
							break;
						}
						
						fileInfo.setContentFile(out.toByteArray());										
						return fileInfo;
						  
			}catch(Exception e) {
				e.printStackTrace();
				
				return fileInfo;
			}										
		}	
}	
	
	
	
