package vn.sesgroup.hddt.user.impl;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import org.w3c.dom.Node;

import vn.sesgroup.hddt.dto.FileInfo;
import vn.sesgroup.hddt.user.dao.AbstractDAO;
import vn.sesgroup.hddt.user.dao.TraCuuHDDAO;
import vn.sesgroup.hddt.user.service.JPUtils;
import vn.sesgroup.hddt.utility.Constants;
import vn.sesgroup.hddt.utility.SystemParams;

@Repository
@Transactional
public class TraCuuHDImpl extends AbstractDAO implements TraCuuHDDAO{
	private static final Logger log = LogManager.getLogger(TraCuuHDImpl.class);
	@Autowired MongoTemplate mongoTemplate;
	@Autowired JPUtils jpUtils;
	
/*
db.getCollection('EInvoice').aggregate([
    {$match: {
            MCCQT: {$exists: true, $ne: null}, 
            'EInvoiceDetail.TTChung.SHDon': 1,
            'SecureKey': '082223',
            'EInvoiceDetail.NDHDon.NMua.MST': '11111111'
        }
    },
    {$lookup:{
            from: 'DMMauSoKyHieu',
            let: {vMauSoHD: '$EInvoiceDetail.TTChung.MauSoHD'},
            pipeline: [
                {$match: {
                        $expr: {
                            $and: [
                                {$eq: [{$toString: '$_id'}, '$$vMauSoHD']}
                            ]
                        }
                    }
                },
                {$project: {Templates: '$Templates'}}
            ],
            as: 'DMMauSoKyHieu'
        }
    },
    {$unwind: {path: '$DMMauSoKyHieu', preserveNullAndEmptyArrays: true}}
]
)
 * */
	@Override
	public FileInfo printEInvoice(HashMap<String, String> mapInput) throws Exception {
		FileInfo fileInfo = new FileInfo();
		
		/*LAY THONG TIN DE IN HD*/
		String action = null == mapInput.get("action")? "": mapInput.get("action");
		String khmst = null == mapInput.get("khmst")? "": mapInput.get("khmst");
		String shdon = null == mapInput.get("shdon")? "": mapInput.get("shdon");
		String securekey = null == mapInput.get("securekey")? "": mapInput.get("securekey");
		String isconvert = null == mapInput.get("isconvert")? "": mapInput.get("isconvert");
		String isTNCN = null == mapInput.get("isTNCN")? "": mapInput.get("isTNCN");
		if(isTNCN.equals("Y")) {
			String isConvert = "";
			String isXoaBo = "XOABO";
			int intSHDon = 0;
			if(commons.checkStringIsInt(shdon))
				intSHDon = commons.stringToInteger(shdon);
			
			Document docFind = new Document("SHDon", intSHDon)
					.append("SecureKey", commons.regexEscapeForMongoQuery(securekey))
					.append("TaxCode", commons.regexEscapeForMongoQuery(khmst));
			List<Document> pipeline = new ArrayList<Document>();
			pipeline.add(new Document("$match", docFind));
			pipeline.add(
				new Document("$lookup", 
					new Document("from", "DMMSTNCN")
					.append("let", new Document("vMauSoHD", "$MauSoHD"))
					.append("pipeline", 
						Arrays.asList(
							new Document("$match", 
								new Document("$expr", 
									new Document("$and", 
										Arrays.asList(
											new Document("$eq", Arrays.asList(new Document("$toString", "$_id"), "$$vMauSoHD"))
										)
									)
								)
							)
						
						)
					)
					.append("as", "DMMSTNCN")
				)
			);
			pipeline.add(
				new Document("$unwind", new Document("path", "$DMMSTNCN").append("preserveNullAndEmptyArrays", true))
			);
			pipeline.add(
					new Document("$lookup", 
						new Document("from", "Issuer")
						.append("let", new Document("vIssuerId", "$IssuerId"))
						.append("pipeline", 
							Arrays.asList(
								new Document("$match", 
									new Document("$expr", 
										new Document("$and", 
											Arrays.asList(
												new Document("$eq", Arrays.asList(new Document("$toString", "$_id"), "$$vIssuerId"))
											)
										)
									)
								)
							
							)
						)
						.append("as", "Issuer")
					)
				);
				pipeline.add(
					new Document("$unwind", new Document("path", "$Issuer").append("preserveNullAndEmptyArrays", true))
				);
				pipeline.add(
						new Document("$lookup", 
							new Document("from", "PramLink")
							.append("pipeline", 
								Arrays.asList(
									new Document("$match", 
										new Document("$expr", 
												new Document("IsDelete", false)
										)
									)
								)	
							)
							.append("as", "PramLink")
						)
					);
					pipeline.add(
						new Document("$unwind", new Document("path", "$PramLink").append("preserveNullAndEmptyArrays", true))
					);	
			Document docTmp = null;
			Iterable<Document> cursor = mongoTemplate.getCollection("ChungTuTNCN").aggregate(pipeline).allowDiskUse(true);
			Iterator<Document> iter = cursor.iterator();
			if(iter.hasNext()) {
				docTmp = iter.next();
			}
			
			if(docTmp == null) {
				return new FileInfo();
			}
			String ImgLogo = docTmp.getEmbedded(Arrays.asList("DMMSTNCN","LoGo"), "");
			String 	link = docTmp.getEmbedded(Arrays.asList("PramLink", "LinkPortal"), "");
			
			String MauSo = docTmp.getEmbedded(Arrays.asList("DMMSTNCN","MauSo"), "");
		
			
			String KH = docTmp.getEmbedded(Arrays.asList("DMMSTNCN","KyHieu"), "")+"/"+docTmp.getEmbedded(Arrays.asList("DMMSTNCN","Nam"), "")+"/"+docTmp.getEmbedded(Arrays.asList("DMMSTNCN","ChungTu"), "");
			String MS = MauSo;
			//org.w3c.dom.Document doc = (org.w3c.dom.Document) isu;
			/* TEST REPORT TO PDF */
			
			String dir = docTmp.get("Dir", "");
			ObjectId _id = docTmp.get("_id", ObjectId.class);
			
			String SignStatus = docTmp.get("SignStatus", "");
			
			String Status = docTmp.get("Status", "");
			
			String fileName = _id + ".xml";
			if ("SIGNED".equals(SignStatus)) {
				fileName = _id + "_signed.xml";
			} 
			File file123 = null;
			if("download-xml".equals(action)) {
				fileName = _id  + ".xml";
				file123 = new File(dir, fileName);
				if(!file123.exists()) {
					fileName = _id + "_signed.xml";
					file123 = new File(dir, fileName);
				}
				
				fileInfo.setFileName(_id + ".xml");
				fileInfo.setContentFile(commons.getBytesDataFromFile(file123));
				return fileInfo;
			}
			
			File file = new File(dir, fileName);
			if (!file.exists() || !file.isFile()) {
				return new FileInfo();
			}

			org.w3c.dom.Document doc = commons.fileToDocument(file);
			
			
			
			String fileNameJP = docTmp.getEmbedded(Arrays.asList("DMMSTNCN","FileName"), "");
		
			File fileJP = new File(SystemParams.DIR_E_INVOICE_TEMPLATE, fileNameJP);

			ByteArrayOutputStream baosPDF = null;

			baosPDF = jpUtils.viewpdfcttncn(fileJP, doc,docTmp,   
					 Paths.get(SystemParams.DIR_E_INVOICE_TEMPLATE, "MauSoTNCN",  docTmp.getEmbedded(Arrays.asList( "Issuer", "TaxCode"), ""), ImgLogo).toString(),
					 KH,MS,link,"Y".equals(isConvert), Status.equals(isXoaBo));
			fileInfo.setFileName("viewpdftncn.pdf");
			fileInfo.setContentFile(baosPDF.toByteArray());
		}
		else {
		int intSHDon = 0;
		if(commons.checkStringIsInt(shdon))
			intSHDon = commons.stringToInteger(shdon);
		
		Document docFind = new Document("MCCQT", new Document("$exists", true).append("$ne", null))
				.append("EInvoiceDetail.TTChung.SHDon", intSHDon)
				.append("SecureKey", commons.regexEscapeForMongoQuery(securekey))
				.append("EInvoiceDetail.NDHDon.NMua.MST", commons.regexEscapeForMongoQuery(khmst));
		List<Document> pipeline = new ArrayList<Document>();
		pipeline.add(new Document("$match", docFind));
		pipeline.add(
			new Document("$lookup", 
				new Document("from", "DMMauSoKyHieu")
				.append("let", new Document("vMauSoHD", "$EInvoiceDetail.TTChung.MauSoHD"))
				.append("pipeline", 
					Arrays.asList(
						new Document("$match", 
							new Document("$expr", 
								new Document("$and", 
									Arrays.asList(
										new Document("$eq", Arrays.asList(new Document("$toString", "$_id"), "$$vMauSoHD"))
									)
								)
							)
						),
						new Document("$project", new Document("Templates", "$Templates"))
					)
				)
				.append("as", "DMMauSoKyHieu")
			)
		);
		pipeline.add(
			new Document("$unwind", new Document("path", "$DMMauSoKyHieu").append("preserveNullAndEmptyArrays", true))
		);
		pipeline.add(
				new Document("$lookup", 
					new Document("from", "UserConFig")
					.append("let", new Document("vIssuerId", "$IssuerId"))
					.append("pipeline", 
							Arrays.asList(
								new Document("$match", 
									new Document("$expr", 
										new Document("$and", 
											Arrays.asList(
												new Document("$eq", Arrays.asList(new Document("$toString", "$IssuerId"), "$$vIssuerId"))
											)
										)
									)
								)
							
							)
						)
						.append("as", "UserConFig")
				)
			);
			pipeline.add(
				new Document("$unwind", new Document("path", "$UserConFig").append("preserveNullAndEmptyArrays", true))
			);
			pipeline.add(
					new Document("$lookup", 
						new Document("from", "PramLink")
						.append("pipeline", 
							Arrays.asList(
								new Document("$match", 
									new Document("$expr", 
											new Document("IsDelete", false)
									)
								)
							)	
						)
						.append("as", "PramLink")
					)
				);
				pipeline.add(
					new Document("$unwind", new Document("path", "$PramLink").append("preserveNullAndEmptyArrays", true))
				);	
		Document docTmp = null;
		Document docTmp1 = null;
		Document docTmp2 = null;
		Document docTmp3 = null;
		Document docTmp4 = null;
		Iterable<Document> cursor = mongoTemplate.getCollection("EInvoice").aggregate(pipeline).allowDiskUse(true);
		Iterable<Document> cursor1 = mongoTemplate.getCollection("EInvoiceBH").aggregate(pipeline).allowDiskUse(true);
		Iterable<Document> cursor2 = mongoTemplate.getCollection("EInvoicePXK").aggregate(pipeline).allowDiskUse(true);
		Iterable<Document> cursor3 = mongoTemplate.getCollection("EInvoicePXKDL").aggregate(pipeline).allowDiskUse(true);
		Iterable<Document> cursor4 = mongoTemplate.getCollection("EInvoiceMTT").aggregate(pipeline).allowDiskUse(true);
		Iterator<Document> iter = cursor.iterator();
		Iterator<Document> iter1 = cursor1.iterator();
		Iterator<Document> iter2 = cursor2.iterator();
		Iterator<Document> iter3 = cursor3.iterator();
		Iterator<Document> iter4 = cursor4.iterator();
		if(iter.hasNext()) {
			docTmp = iter.next();
		}
		if(iter1.hasNext()) {
			docTmp1 = iter1.next();
		}
		if(iter2.hasNext()) {
			docTmp2 = iter2.next();
		}
		if(iter3.hasNext()) {
			docTmp3 = iter3.next();
		}
		if(iter4.hasNext()) {
			docTmp4 = iter4.next();
		}
		if(null != docTmp) {
			String CheckView = docTmp.getEmbedded(Arrays.asList("UserConFig", "viewshd"), "");
			String _id = docTmp.getObjectId("_id").toString();
			String MST = docTmp.getEmbedded(Arrays.asList("EInvoiceDetail", "NDHDon", "NBan", "MST"), "");
			String ImgLogo = docTmp.getEmbedded(Arrays.asList("DMMauSoKyHieu", "Templates", "ImgLogo"), "");
			String ImgBackground = docTmp.getEmbedded(Arrays.asList("DMMauSoKyHieu", "Templates", "ImgBackground"), "");
			String ImgQA = docTmp.getEmbedded(Arrays.asList("DMMauSoKyHieu", "Templates", "ImgQA"), "");
			String ImgVien = docTmp.getEmbedded(Arrays.asList("DMMauSoKyHieu", "Templates", "ImgVien"), "");
			Integer shd = 	docTmp.getEmbedded(Arrays.asList("EInvoiceDetail", "TTChung","SHDon"), 0);
			String dir = docTmp.get("Dir", "");
			String signStatusCode = docTmp.get("SignStatusCode", "");
			String eInvoiceStatus = docTmp.get("EInvoiceStatus", "");
			String secureKey = docTmp.get("SecureKey", "");
			String MCCQT = docTmp.get("MCCQT", "");
			String fileName = _id + ".xml";
			String 	link = docTmp.getEmbedded(Arrays.asList("PramLink", "LinkPortal"), "");
			boolean isDieuChinh = "2".equals(docTmp.getEmbedded(Arrays.asList("HDSS", "TCTBao"), ""));
			boolean isThayThe = false;
			String check_status = docTmp.get("EInvoiceStatus", "");
			if(check_status.equals("REPLACED"))	{
				isThayThe = "3".equals(docTmp.getEmbedded(Arrays.asList("HDSS", "TCTBao"), ""));
			}
			String ParamUSD = docTmp.getEmbedded(Arrays.asList("EInvoiceDetail", "TToan", "ParamUSD"), "");

			
			File file = null;
			if("download-xml".equals(action)) {
				fileName = _id + "_" + MCCQT + ".xml";
				
				String fileName_ = _id + "_" + MCCQT + "_" + shd + ".xml";
				/*	CHECK MCCQT GET DATA XML */
					
					File file_xml = new File(dir, fileName);
					
					org.w3c.dom.Document doc_xml = commons.fileToDocument(file_xml);
					
					XPath xPath_xml = XPathFactory.newInstance().newXPath();
					Node nodeHDon = null;
					for(int J = 1; J<= 20; J++) {
						nodeHDon = (Node) xPath_xml.evaluate("/KetQuaTraCuu/DuLieu/TDiep[" + J + "]/DLieu/HDon", doc_xml, XPathConstants.NODE);
						if(nodeHDon != null) break;
					}
					/*FILE CHUA DUOC DUI DEN CO QUAN THUE*/
					if(null == nodeHDon) {
						nodeHDon = (Node) xPath_xml.evaluate("/HDon", doc_xml, XPathConstants.NODE);
					}							
					
					DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
					DocumentBuilder builder = factory.newDocumentBuilder();
					org.w3c.dom.Document rTCTN_HDON = builder.newDocument();
					rTCTN_HDON.appendChild(rTCTN_HDON.importNode(nodeHDon, true));
					/* END CHECK GET DATA IN RESULT IN FILE XML TO TAX */
					boolean boo_ = false;
					boo_ = commons.docW3cToFile(rTCTN_HDON, dir, fileName_);
					
					if(boo_ == true) {
						fileName = fileName_;
					}
				/* END CHECK MCCQT GET DATA XML */
					
				file = new File(dir, fileName);
				if(!file.exists()) {
					fileName = _id + "_signed.xml";
					file = new File(dir, fileName);
				}
				
				fileInfo.setFileName(_id + ".xml");
				fileInfo.setContentFile(commons.getBytesDataFromFile(file));
				return fileInfo;
			}
			
			if("SIGNED".equals(signStatusCode) && "COMPLETE".equals(eInvoiceStatus) && !"".equals(MCCQT)) {
				
				fileName = _id + "_" + MCCQT + ".xml";
				String fileName_ = _id + "_" + MCCQT + "_" + shd + ".xml";
				/*	CHECK MCCQT GET DATA XML */
					
					File file_xml = new File(dir, fileName);
					
					org.w3c.dom.Document doc_xml = commons.fileToDocument(file_xml);
					
					XPath xPath_xml = XPathFactory.newInstance().newXPath();
					Node nodeHDon = null;
					for(int J = 1; J<= 20; J++) {
						nodeHDon = (Node) xPath_xml.evaluate("/KetQuaTraCuu/DuLieu/TDiep[" + J + "]/DLieu/HDon", doc_xml, XPathConstants.NODE);
						if(nodeHDon != null) break;
					}
					/*FILE CHUA DUOC DUI DEN CO QUAN THUE*/
					if(null == nodeHDon) {
						nodeHDon = (Node) xPath_xml.evaluate("/HDon", doc_xml, XPathConstants.NODE);
					}							
					
					DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
					DocumentBuilder builder = factory.newDocumentBuilder();
					org.w3c.dom.Document rTCTN_HDON = builder.newDocument();
					rTCTN_HDON.appendChild(rTCTN_HDON.importNode(nodeHDon, true));
					/* END CHECK GET DATA IN RESULT IN FILE XML TO TAX */
					boolean boo_ = false;
					boo_ = commons.docW3cToFile(rTCTN_HDON, dir, fileName_);
					
					if(boo_ == true) {
						fileName = fileName_;
					}
				/* END CHECK MCCQT GET DATA XML */
			}else {
				if("SIGNED".equals(signStatusCode)) {
					fileName = _id + "_signed.xml";	
				}
			}
			file = new File(dir, fileName);
			if(!file.exists() || !file.isFile()) {
				return new FileInfo();
			}
			org.w3c.dom.Document doc = commons.fileToDocument(file);		
			/*TEST REPORT TO PDF*/
			String fileNameJP = docTmp.getEmbedded(Arrays.asList("DMMauSoKyHieu", "Templates", "FileName"), "");
			int numberRowInPage = docTmp.getEmbedded(Arrays.asList("DMMauSoKyHieu", "Templates", "RowsInPage"), 20);
			int numberRowInPageMultiPage = docTmp.getEmbedded(Arrays.asList("DMMauSoKyHieu", "Templates", "RowInPageMultiPage"), 26);
			int numberCharsInRow = docTmp.getEmbedded(Arrays.asList("DMMauSoKyHieu", "Templates", "CharsInRow"), 50);
			
			File fileJP = new File(SystemParams.DIR_E_INVOICE_TEMPLATE, fileNameJP);
			
			ByteArrayOutputStream baosPDF = null;
			
			baosPDF = jpUtils.createFinalInvoice(fileJP, doc, secureKey, CheckView
					, numberRowInPage, numberRowInPageMultiPage, numberCharsInRow, MST, link, ParamUSD,
					Paths.get(SystemParams.DIR_E_INVOICE_TEMPLATE, "images", MST, ImgLogo).toString(), 
					Paths.get(SystemParams.DIR_E_INVOICE_TEMPLATE, "images", MST, ImgBackground).toString(),
					Paths.get(SystemParams.DIR_E_INVOICE_TEMPLATE, "images", MST, ImgQA ).toString(),
					Paths.get(SystemParams.DIR_E_INVOICE_TEMPLATE, "images", MST, ImgVien ).toString(),

					"Y".equals(isconvert), Constants.INVOICE_STATUS.DELETED.equals(eInvoiceStatus),
					isThayThe, isDieuChinh
				);
			
			fileInfo.setFileName("EInvoice.pdf");
			fileInfo.setContentFile(baosPDF.toByteArray());
		}else if(null != docTmp1) {
			
			String CheckView = docTmp1.getEmbedded(Arrays.asList("UserConFig", "viewshd"), "");
			String _id = docTmp1.getObjectId("_id").toString();
			String MST = docTmp1.getEmbedded(Arrays.asList("EInvoiceDetail", "NDHDon", "NBan", "MST"), "");
			String ImgLogo = docTmp1.getEmbedded(Arrays.asList("DMMauSoKyHieu", "Templates", "ImgLogo"), "");
			String ImgBackground = docTmp1.getEmbedded(Arrays.asList("DMMauSoKyHieu", "Templates", "ImgBackground"), "");
			String ImgQA = docTmp1.getEmbedded(Arrays.asList("DMMauSoKyHieu", "Templates", "ImgQA"), "");
			String ImgVien = docTmp1.getEmbedded(Arrays.asList("DMMauSoKyHieu", "Templates", "ImgVien"), "");
			String 	link = docTmp1.getEmbedded(Arrays.asList("PramLink", "LinkPortal"), "");
			String dir = docTmp1.get("Dir", "");
			String signStatusCode = docTmp1.get("SignStatusCode", "");
			String eInvoiceStatus = docTmp1.get("EInvoiceStatus", "");
			String secureKey = docTmp1.get("SecureKey", "");
			String MCCQT = docTmp1.get("MCCQT", "");
			String fileName = _id + ".xml";
			Integer shd = 	docTmp1.getEmbedded(Arrays.asList("EInvoiceDetail", "TTChung","SHDon"), 0);
			
			boolean isDieuChinh = "2".equals(docTmp1.getEmbedded(Arrays.asList("HDSS", "TCTBao"), ""));
			boolean isThayThe = "3".equals(docTmp1.getEmbedded(Arrays.asList("HDSS", "TCTBao"), ""));
			
			File file = null;
			if("download-xml".equals(action)) {
				fileName = _id + "_" + MCCQT + ".xml";
				String fileName_ = _id + "_" + MCCQT + "_" + shd + ".xml";
				/*	CHECK MCCQT GET DATA XML */
					
					File file_xml = new File(dir, fileName);
					
					org.w3c.dom.Document doc_xml = commons.fileToDocument(file_xml);
					
					XPath xPath_xml = XPathFactory.newInstance().newXPath();
					Node nodeHDon = null;
					for(int J = 1; J<= 20; J++) {
						nodeHDon = (Node) xPath_xml.evaluate("/KetQuaTraCuu/DuLieu/TDiep[" + J + "]/DLieu/HDon", doc_xml, XPathConstants.NODE);
						if(nodeHDon != null) break;
					}
					/*FILE CHUA DUOC DUI DEN CO QUAN THUE*/
					if(null == nodeHDon) {
						nodeHDon = (Node) xPath_xml.evaluate("/HDon", doc_xml, XPathConstants.NODE);
					}							
					
					DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
					DocumentBuilder builder = factory.newDocumentBuilder();
					org.w3c.dom.Document rTCTN_HDON = builder.newDocument();
					rTCTN_HDON.appendChild(rTCTN_HDON.importNode(nodeHDon, true));
					/* END CHECK GET DATA IN RESULT IN FILE XML TO TAX */
					boolean boo_ = false;
					boo_ = commons.docW3cToFile(rTCTN_HDON, dir, fileName_);
					
					if(boo_ == true) {
						fileName = fileName_;
					}
				/* END CHECK MCCQT GET DATA XML */
				file = new File(dir, fileName);
				if(!file.exists()) {
					fileName = _id + "_signed.xml";
					file = new File(dir, fileName);
				}
				
				fileInfo.setFileName(_id + ".xml");
				fileInfo.setContentFile(commons.getBytesDataFromFile(file));
				return fileInfo;
			}
			
			if("SIGNED".equals(signStatusCode) && "COMPLETE".equals(eInvoiceStatus) && !"".equals(MCCQT)) {
				fileName = _id + "_" + MCCQT + ".xml";
				String fileName_ = _id + "_" + MCCQT + "_" + shd + ".xml";
				/*	CHECK MCCQT GET DATA XML */
					
					File file_xml = new File(dir, fileName);
					
					org.w3c.dom.Document doc_xml = commons.fileToDocument(file_xml);
					
					XPath xPath_xml = XPathFactory.newInstance().newXPath();
					Node nodeHDon = null;
					for(int J = 1; J<= 20; J++) {
						nodeHDon = (Node) xPath_xml.evaluate("/KetQuaTraCuu/DuLieu/TDiep[" + J + "]/DLieu/HDon", doc_xml, XPathConstants.NODE);
						if(nodeHDon != null) break;
					}
					/*FILE CHUA DUOC DUI DEN CO QUAN THUE*/
					if(null == nodeHDon) {
						nodeHDon = (Node) xPath_xml.evaluate("/HDon", doc_xml, XPathConstants.NODE);
					}							
					
					DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
					DocumentBuilder builder = factory.newDocumentBuilder();
					org.w3c.dom.Document rTCTN_HDON = builder.newDocument();
					rTCTN_HDON.appendChild(rTCTN_HDON.importNode(nodeHDon, true));
					/* END CHECK GET DATA IN RESULT IN FILE XML TO TAX */
					boolean boo_ = false;
					boo_ = commons.docW3cToFile(rTCTN_HDON, dir, fileName_);
					
					if(boo_ == true) {
						fileName = fileName_;
					}
				/* END CHECK MCCQT GET DATA XML */
			}else {
				if("SIGNED".equals(signStatusCode)) {
					fileName = _id + "_signed.xml";	
				}
			}
			file = new File(dir, fileName);
			if(!file.exists() || !file.isFile()) {
				return new FileInfo();
			}
			org.w3c.dom.Document doc = commons.fileToDocument(file);		
			/*TEST REPORT TO PDF*/
			String fileNameJP = docTmp1.getEmbedded(Arrays.asList("DMMauSoKyHieu", "Templates", "FileName"), "");
			int numberRowInPage = docTmp1.getEmbedded(Arrays.asList("DMMauSoKyHieu", "Templates", "RowsInPage"), 20);
			int numberRowInPageMultiPage = docTmp1.getEmbedded(Arrays.asList("DMMauSoKyHieu", "Templates", "RowInPageMultiPage"), 26);
			int numberCharsInRow = docTmp1.getEmbedded(Arrays.asList("DMMauSoKyHieu", "Templates", "CharsInRow"), 50);
			
			File fileJP = new File(SystemParams.DIR_E_INVOICE_TEMPLATE, fileNameJP);
			
			ByteArrayOutputStream baosPDF = null;
			
			baosPDF = jpUtils.createFinalInvoiceBH(fileJP, doc, secureKey, CheckView
					, numberRowInPage, numberRowInPageMultiPage, numberCharsInRow, MST, link,
					Paths.get(SystemParams.DIR_E_INVOICE_TEMPLATE, "images", MST, ImgLogo).toString(), 
					Paths.get(SystemParams.DIR_E_INVOICE_TEMPLATE, "images", MST, ImgBackground).toString(),
					Paths.get(SystemParams.DIR_E_INVOICE_TEMPLATE, "images", MST, ImgQA ).toString(),
					Paths.get(SystemParams.DIR_E_INVOICE_TEMPLATE, "images", MST, ImgVien ).toString(),

					"Y".equals(isconvert), Constants.INVOICE_STATUS.DELETED.equals(eInvoiceStatus),
					isThayThe, isDieuChinh
				);
			
			fileInfo.setFileName("EInvoiceBH.pdf");
			fileInfo.setContentFile(baosPDF.toByteArray());	
		}else if(null != docTmp2) {
			String CheckView = docTmp2.getEmbedded(Arrays.asList("UserConFig", "viewshd"), "");
			String _id = docTmp2.getObjectId("_id").toString();
			String MST = docTmp2.getEmbedded(Arrays.asList("EInvoiceDetail", "NDHDon", "NBan", "MST"), "");
			String ImgLogo = docTmp2.getEmbedded(Arrays.asList("DMMauSoKyHieu", "Templates", "ImgLogo"), "");
			String ImgBackground = docTmp2.getEmbedded(Arrays.asList("DMMauSoKyHieu", "Templates", "ImgBackground"), "");
			String ImgQA = docTmp2.getEmbedded(Arrays.asList("DMMauSoKyHieu", "Templates", "ImgQA"), "");
			String ImgVien = docTmp2.getEmbedded(Arrays.asList("DMMauSoKyHieu", "Templates", "ImgVien"), "");
			String 	link = docTmp2.getEmbedded(Arrays.asList("PramLink", "LinkPortal"), "");
			String dir = docTmp2.get("Dir", "");
			String signStatusCode = docTmp2.get("SignStatusCode", "");
			String eInvoiceStatus = docTmp2.get("EInvoiceStatus", "");
			String MCCQT = docTmp2.get("MCCQT", "");
			String fileName = _id + ".xml";
			Integer shd = 	docTmp2.getEmbedded(Arrays.asList("EInvoiceDetail", "TTChung","SHDon"), 0);
			boolean isDieuChinh = "2".equals(docTmp2.getEmbedded(Arrays.asList("HDSS", "TCTBao"), ""));
			boolean isThayThe = "3".equals(docTmp2.getEmbedded(Arrays.asList("HDSS", "TCTBao"), ""));
			
			File file = null;
			if("download-xml".equals(action)) {
				fileName = _id + "_" + MCCQT + ".xml";
				String fileName_ = _id + "_" + MCCQT + "_" + shd + ".xml";
				/*	CHECK MCCQT GET DATA XML */
					
					File file_xml = new File(dir, fileName);
					
					org.w3c.dom.Document doc_xml = commons.fileToDocument(file_xml);
					
					XPath xPath_xml = XPathFactory.newInstance().newXPath();
					Node nodeHDon = null;
					for(int J = 1; J<= 20; J++) {
						nodeHDon = (Node) xPath_xml.evaluate("/KetQuaTraCuu/DuLieu/TDiep[" + J + "]/DLieu/HDon", doc_xml, XPathConstants.NODE);
						if(nodeHDon != null) break;
					}
					/*FILE CHUA DUOC DUI DEN CO QUAN THUE*/
					if(null == nodeHDon) {
						nodeHDon = (Node) xPath_xml.evaluate("/HDon", doc_xml, XPathConstants.NODE);
					}							
					
					DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
					DocumentBuilder builder = factory.newDocumentBuilder();
					org.w3c.dom.Document rTCTN_HDON = builder.newDocument();
					rTCTN_HDON.appendChild(rTCTN_HDON.importNode(nodeHDon, true));
					/* END CHECK GET DATA IN RESULT IN FILE XML TO TAX */
					boolean boo_ = false;
					boo_ = commons.docW3cToFile(rTCTN_HDON, dir, fileName_);
					
					if(boo_ == true) {
						fileName = fileName_;
					}
				/* END CHECK MCCQT GET DATA XML */
				file = new File(dir, fileName);
				if(!file.exists()) {
					fileName = _id + "_signed.xml";
					file = new File(dir, fileName);
				}
				
				fileInfo.setFileName(_id + ".xml");
				fileInfo.setContentFile(commons.getBytesDataFromFile(file));
				return fileInfo;
			}
			
			if("SIGNED".equals(signStatusCode) && "COMPLETE".equals(eInvoiceStatus) && !"".equals(MCCQT)) {
				fileName = _id + "_" + MCCQT + ".xml";

				String fileName_ = _id + "_" + MCCQT + "_" + shd + ".xml";
		/*	CHECK MCCQT GET DATA XML */
			
			File file_xml = new File(dir, fileName);
			
			org.w3c.dom.Document doc_xml = commons.fileToDocument(file_xml);
			
			XPath xPath_xml = XPathFactory.newInstance().newXPath();
			Node nodeHDon = null;
			for(int J = 1; J<= 20; J++) {
				nodeHDon = (Node) xPath_xml.evaluate("/KetQuaTraCuu/DuLieu/TDiep[" + J + "]/DLieu/HDon", doc_xml, XPathConstants.NODE);
				if(nodeHDon != null) break;
			}
			/*FILE CHUA DUOC DUI DEN CO QUAN THUE*/
			if(null == nodeHDon) {
				nodeHDon = (Node) xPath_xml.evaluate("/HDon", doc_xml, XPathConstants.NODE);
			}							
			
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			DocumentBuilder builder = factory.newDocumentBuilder();
			org.w3c.dom.Document rTCTN_HDON = builder.newDocument();
			rTCTN_HDON.appendChild(rTCTN_HDON.importNode(nodeHDon, true));
			/* END CHECK GET DATA IN RESULT IN FILE XML TO TAX */
			boolean boo_ = false;
			boo_ = commons.docW3cToFile(rTCTN_HDON, dir, fileName_);
			
			if(boo_ == true) {
				fileName = fileName_;
			}
		/* END CHECK MCCQT GET DATA XML */
			}else {
				if("SIGNED".equals(signStatusCode)) {
					fileName = _id + "_signed.xml";	
				}
			}
			file = new File(dir, fileName);
			if(!file.exists() || !file.isFile()) {
				return new FileInfo();
			}
			org.w3c.dom.Document doc = commons.fileToDocument(file);		
			/*TEST REPORT TO PDF*/
			String fileNameJP = docTmp2.getEmbedded(Arrays.asList("DMMauSoKyHieu", "Templates", "FileName"), "");
			int numberRowInPage = docTmp2.getEmbedded(Arrays.asList("DMMauSoKyHieu", "Templates", "RowsInPage"), 20);
			int numberRowInPageMultiPage = docTmp2.getEmbedded(Arrays.asList("DMMauSoKyHieu", "Templates", "RowInPageMultiPage"), 26);
			int numberCharsInRow = docTmp2.getEmbedded(Arrays.asList("DMMauSoKyHieu", "Templates", "CharsInRow"), 50);
			
			File fileJP = new File(SystemParams.DIR_E_INVOICE_TEMPLATE, fileNameJP);
			
			ByteArrayOutputStream baosPDF = null;
			
			baosPDF = jpUtils.createFinalInvoice2(fileJP, doc,docTmp2, CheckView
					, numberRowInPage, numberRowInPageMultiPage, numberCharsInRow, MST, link,
					Paths.get(SystemParams.DIR_E_INVOICE_TEMPLATE, "images", MST, ImgLogo).toString(), 
					Paths.get(SystemParams.DIR_E_INVOICE_TEMPLATE, "images", MST, ImgBackground).toString(),
					Paths.get(SystemParams.DIR_E_INVOICE_TEMPLATE, "images", MST, ImgQA ).toString(),
					Paths.get(SystemParams.DIR_E_INVOICE_TEMPLATE, "images", MST, ImgVien ).toString(),

					"Y".equals(isconvert), Constants.INVOICE_STATUS.DELETED.equals(eInvoiceStatus),
					isThayThe, isDieuChinh
				);
			
			fileInfo.setFileName("EInvoicePXK.pdf");
			fileInfo.setContentFile(baosPDF.toByteArray());	
		}else if(null != docTmp3) {
			String CheckView = docTmp3.getEmbedded(Arrays.asList("UserConFig", "viewshd"), "");
			String _id = docTmp3.getObjectId("_id").toString();
			String MST = docTmp3.getEmbedded(Arrays.asList("EInvoiceDetail", "NDHDon", "NBan", "MST"), "");
			String ImgLogo = docTmp3.getEmbedded(Arrays.asList("DMMauSoKyHieu", "Templates", "ImgLogo"), "");
			String ImgBackground = docTmp3.getEmbedded(Arrays.asList("DMMauSoKyHieu", "Templates", "ImgBackground"), "");
			String ImgQA = docTmp3.getEmbedded(Arrays.asList("DMMauSoKyHieu", "Templates", "ImgQA"), "");
			String ImgVien = docTmp3.getEmbedded(Arrays.asList("DMMauSoKyHieu", "Templates", "ImgVien"), "");
			String 	link = docTmp3.getEmbedded(Arrays.asList("PramLink", "LinkPortal"), "");
			String dir = docTmp3.get("Dir", "");
			String signStatusCode = docTmp3.get("SignStatusCode", "");
			String eInvoiceStatus = docTmp3.get("EInvoiceStatus", "");
			String MCCQT = docTmp3.get("MCCQT", "");
			String secureKey = docTmp3.get("SecureKey", "");
			String fileName = _id + ".xml";
			Integer shd = 	docTmp3.getEmbedded(Arrays.asList("EInvoiceDetail", "TTChung","SHDon"), 0);
			boolean isDieuChinh = "2".equals(docTmp3.getEmbedded(Arrays.asList("HDSS", "TCTBao"), ""));
			boolean isThayThe = "3".equals(docTmp3.getEmbedded(Arrays.asList("HDSS", "TCTBao"), ""));
			
			File file = null;
			if("download-xml".equals(action)) {
				fileName = _id + "_" + MCCQT + ".xml";
				String fileName_ = _id + "_" + MCCQT + "_" + shd + ".xml";
				/*	CHECK MCCQT GET DATA XML */
					
					File file_xml = new File(dir, fileName);
					
					org.w3c.dom.Document doc_xml = commons.fileToDocument(file_xml);
					
					XPath xPath_xml = XPathFactory.newInstance().newXPath();
					Node nodeHDon = null;
					for(int J = 1; J<= 20; J++) {
						nodeHDon = (Node) xPath_xml.evaluate("/KetQuaTraCuu/DuLieu/TDiep[" + J + "]/DLieu/HDon", doc_xml, XPathConstants.NODE);
						if(nodeHDon != null) break;
					}
					/*FILE CHUA DUOC DUI DEN CO QUAN THUE*/
					if(null == nodeHDon) {
						nodeHDon = (Node) xPath_xml.evaluate("/HDon", doc_xml, XPathConstants.NODE);
					}							
					
					DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
					DocumentBuilder builder = factory.newDocumentBuilder();
					org.w3c.dom.Document rTCTN_HDON = builder.newDocument();
					rTCTN_HDON.appendChild(rTCTN_HDON.importNode(nodeHDon, true));
					/* END CHECK GET DATA IN RESULT IN FILE XML TO TAX */
					boolean boo_ = false;
					boo_ = commons.docW3cToFile(rTCTN_HDON, dir, fileName_);
					
					if(boo_ == true) {
						fileName = fileName_;
					}
				/* END CHECK MCCQT GET DATA XML */
				file = new File(dir, fileName);
				if(!file.exists()) {
					fileName = _id + "_signed.xml";
					file = new File(dir, fileName);
				}
				
				fileInfo.setFileName(_id + ".xml");
				fileInfo.setContentFile(commons.getBytesDataFromFile(file));
				return fileInfo;
			}
			
			if("SIGNED".equals(signStatusCode) && "COMPLETE".equals(eInvoiceStatus) && !"".equals(MCCQT)) {
				fileName = _id + "_" + MCCQT + ".xml";
				String fileName_ = _id + "_" + MCCQT + "_" + shd + ".xml";
				/*	CHECK MCCQT GET DATA XML */
					
					File file_xml = new File(dir, fileName);
					
					org.w3c.dom.Document doc_xml = commons.fileToDocument(file_xml);
					
					XPath xPath_xml = XPathFactory.newInstance().newXPath();
					Node nodeHDon = null;
					for(int J = 1; J<= 20; J++) {
						nodeHDon = (Node) xPath_xml.evaluate("/KetQuaTraCuu/DuLieu/TDiep[" + J + "]/DLieu/HDon", doc_xml, XPathConstants.NODE);
						if(nodeHDon != null) break;
					}
					/*FILE CHUA DUOC DUI DEN CO QUAN THUE*/
					if(null == nodeHDon) {
						nodeHDon = (Node) xPath_xml.evaluate("/HDon", doc_xml, XPathConstants.NODE);
					}							
					
					DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
					DocumentBuilder builder = factory.newDocumentBuilder();
					org.w3c.dom.Document rTCTN_HDON = builder.newDocument();
					rTCTN_HDON.appendChild(rTCTN_HDON.importNode(nodeHDon, true));
					/* END CHECK GET DATA IN RESULT IN FILE XML TO TAX */
					boolean boo_ = false;
					boo_ = commons.docW3cToFile(rTCTN_HDON, dir, fileName_);
					
					if(boo_ == true) {
						fileName = fileName_;
					}
				/* END CHECK MCCQT GET DATA XML */
			}else {
				if("SIGNED".equals(signStatusCode)) {
					fileName = _id + "_signed.xml";	
				}
			}
			file = new File(dir, fileName);
			if(!file.exists() || !file.isFile()) {
				return new FileInfo();
			}
			org.w3c.dom.Document doc = commons.fileToDocument(file);		
			/*TEST REPORT TO PDF*/
			String fileNameJP = docTmp3.getEmbedded(Arrays.asList("DMMauSoKyHieu", "Templates", "FileName"), "");
			int numberRowInPage = docTmp3.getEmbedded(Arrays.asList("DMMauSoKyHieu", "Templates", "RowsInPage"), 20);
			int numberRowInPageMultiPage = docTmp3.getEmbedded(Arrays.asList("DMMauSoKyHieu", "Templates", "RowInPageMultiPage"), 26);
			int numberCharsInRow = docTmp3.getEmbedded(Arrays.asList("DMMauSoKyHieu", "Templates", "CharsInRow"), 50);
			
			File fileJP = new File(SystemParams.DIR_E_INVOICE_TEMPLATE, fileNameJP);
			
			ByteArrayOutputStream baosPDF = null;
			
			baosPDF = jpUtils.createFinalInvoiceDL(fileJP, doc, secureKey, CheckView
					, numberRowInPage, numberRowInPageMultiPage, numberCharsInRow, MST, link,
					Paths.get(SystemParams.DIR_E_INVOICE_TEMPLATE, "images", MST, ImgLogo).toString(), 
					Paths.get(SystemParams.DIR_E_INVOICE_TEMPLATE, "images", MST, ImgBackground).toString(),
					Paths.get(SystemParams.DIR_E_INVOICE_TEMPLATE, "images", MST, ImgQA ).toString(),
					Paths.get(SystemParams.DIR_E_INVOICE_TEMPLATE, "images", MST, ImgVien ).toString(),

					"Y".equals(isconvert), Constants.INVOICE_STATUS.DELETED.equals(eInvoiceStatus),
					isThayThe, isDieuChinh
				);
			
			fileInfo.setFileName("EInvoicePXKDL.pdf");
			fileInfo.setContentFile(baosPDF.toByteArray());	
		}	
		else if(null != docTmp4) {
			String CheckView = docTmp4.getEmbedded(Arrays.asList("UserConFig", "viewshd"), "");
			String _id = docTmp4.getObjectId("_id").toString();
			String MST = docTmp4.getEmbedded(Arrays.asList("EInvoiceDetail", "NDHDon", "NBan", "MST"), "");
			String ImgLogo = docTmp4.getEmbedded(Arrays.asList("DMMauSoKyHieu", "Templates", "ImgLogo"), "");
			String ImgBackground = docTmp4.getEmbedded(Arrays.asList("DMMauSoKyHieu", "Templates", "ImgBackground"), "");
			String ImgQA = docTmp4.getEmbedded(Arrays.asList("DMMauSoKyHieu", "Templates", "ImgQA"), "");
			String ImgVien = docTmp4.getEmbedded(Arrays.asList("DMMauSoKyHieu", "Templates", "ImgVien"), "");
			String 	link = docTmp4.getEmbedded(Arrays.asList("PramLink", "LinkPortal"), "");
			String dir = docTmp4.get("Dir", "");
			String signStatusCode = docTmp4.get("SignStatusCode", "");
			String eInvoiceStatus = docTmp4.get("EInvoiceStatus", "");
			String MCCQT = docTmp4.get("MCCQT", "");
			String secureKey = docTmp4.get("SecureKey", "");
			String fileName = _id + ".xml";
			Integer shd = docTmp4.getEmbedded(Arrays.asList("EInvoiceDetail", "TTChung","SHDon"), 0);
			boolean isDieuChinh = "2".equals(docTmp4.getEmbedded(Arrays.asList("HDSS", "TCTBao"), ""));
			boolean isThayThe = "3".equals(docTmp4.getEmbedded(Arrays.asList("HDSS", "TCTBao"), ""));
			
			File file = null;
			if("download-xml".equals(action)) {
				fileName = _id + "_pending"+ ".xml";			
				file = new File(dir, fileName);
				if(!file.exists()) {
					fileName = _id + ".xml";
					file = new File(dir, fileName);
				}
				
				fileInfo.setFileName(_id + ".xml");
				fileInfo.setContentFile(commons.getBytesDataFromFile(file));
				return fileInfo;
			}
			
			if ("SIGNED".equals(signStatusCode)) {
				fileName = _id + "_signed" + ".xml";
			}
			else if ("NOSIGN".equals(signStatusCode) && eInvoiceStatus.equals("PENDING")) {
				fileName = _id + "_pending.xml";
			}
			
			else {
				fileName = _id + ".xml";
			}
			file = new File(dir, fileName);
			if(!file.exists() || !file.isFile()) {
				return new FileInfo();
			}
			org.w3c.dom.Document doc = commons.fileToDocument(file);		
			/*TEST REPORT TO PDF*/
			String fileNameJP = docTmp4.getEmbedded(Arrays.asList("DMMauSoKyHieu", "Templates", "FileName"), "");
			int numberRowInPage = docTmp4.getEmbedded(Arrays.asList("DMMauSoKyHieu", "Templates", "RowsInPage"), 20);
			int numberRowInPageMultiPage = docTmp4.getEmbedded(Arrays.asList("DMMauSoKyHieu", "Templates", "RowInPageMultiPage"), 26);
			int numberCharsInRow = docTmp4.getEmbedded(Arrays.asList("DMMauSoKyHieu", "Templates", "CharsInRow"), 50);
			
			File fileJP = new File(SystemParams.DIR_E_INVOICE_TEMPLATE, fileNameJP);
			
			ByteArrayOutputStream baosPDF = null;
			
			baosPDF = jpUtils.createFinalInvoiceMTT(fileJP, doc, CheckView, link, eInvoiceStatus, signStatusCode
					, numberRowInPage, numberRowInPageMultiPage, numberCharsInRow, MST,
					Paths.get(SystemParams.DIR_E_INVOICE_TEMPLATE, "images", MST, ImgLogo).toString(), 
					Paths.get(SystemParams.DIR_E_INVOICE_TEMPLATE, "images", MST, ImgBackground).toString(),
					Paths.get(SystemParams.DIR_E_INVOICE_TEMPLATE, "images", MST, ImgQA ).toString(),
					Paths.get(SystemParams.DIR_E_INVOICE_TEMPLATE, "images", MST, ImgVien ).toString(),

					"Y".equals(isconvert), Constants.INVOICE_STATUS.DELETED.equals(eInvoiceStatus),
					isThayThe, isDieuChinh
				);
			
			fileInfo.setFileName("EInvoicePXKDL.pdf");
			fileInfo.setContentFile(baosPDF.toByteArray());	
		}	
		else {
			return new FileInfo();
		}			
	}
		return fileInfo;
	
}

	
}
