//package vn.sesgroup.hddt.user.service;
//
//import java.time.LocalDate;
//import java.util.ArrayList;
//import java.util.Arrays;
//import java.util.Iterator;
//import java.util.List;
//import java.util.concurrent.TimeUnit;
//
//import javax.xml.xpath.XPath;
//import javax.xml.xpath.XPathConstants;
//import javax.xml.xpath.XPathFactory;
//
//import org.bson.Document;
//import org.bson.types.ObjectId;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.data.mongodb.core.MongoTemplate;
//import org.springframework.scheduling.annotation.Scheduled;
//import org.springframework.stereotype.Component;
//import org.w3c.dom.Element;
//import org.w3c.dom.Node;
//
//import com.mongodb.client.model.FindOneAndDeleteOptions;
//import com.mongodb.client.model.FindOneAndUpdateOptions;
//import com.mongodb.client.model.ReturnDocument;
//import com.mongodb.client.result.DeleteResult;
//
//import vn.sesgroup.hddt.user.dao.AbstractDAO;
//import vn.sesgroup.hddt.utility.Constants;
//
//@Component
//public class ScheduleInvoiceVATService extends AbstractDAO {
//
//	@Autowired
//	MongoTemplate mongoTemplate;
//	@Autowired
//	TCTNService tctnService;

//	@Scheduled(cron = "0 0/30 * * * *")
////	@Scheduled(fixedRate = 1800000, initialDelay = 3000)
//	public void scheduleByInvoice() throws Exception {
//		Document docTmp = null;
//		Iterable<Document> cursor = null;
//		Iterator<Document> iter = null;
//
//		FindOneAndUpdateOptions options = null;
//
//		Document docFind = new Document("IsDelete", new Document("$ne", true)).append("SignStatusCode", "SIGNED")
//				.append("EInvoiceStatus", "PROCESSING");
//
//		List<Document> pipeline = new ArrayList<Document>();
//		pipeline.add(new Document("$match", docFind));
//		pipeline.add(new Document("$sort",
//				new Document("EInvoiceDetail.TTChung.NLap", 1).append("EInvoiceDetail.TTChung.SHDon", 1)));
//		pipeline.add(new Document("$project", new Document("_id", new Document("$toString", "$_id")).append("MTDiep", 1)
//				.append("Dir", 1).append("IssuerId", 1).append("EInvoiceDetail.TTChung", 1)
//
//		));
//		cursor = mongoTemplate.getCollection("EInvoice").aggregate(pipeline).allowDiskUse(true);
//		iter = cursor.iterator();
//		// HANDLE FOR SEQUENCE
//
//		while (iter.hasNext()) {
//			docTmp = iter.next();
//
//			try {
//
//				String MaKetQua = "";
//				String _id = docTmp.getString("_id");
//				String MTDiep = docTmp.getString("MTDiep");
//				String IssuerId = docTmp.getString("IssuerId");
//				org.w3c.dom.Document rTCTN = null;
//
//				try {
//					rTCTN = tctnService.callTraCuuThongDiep(MTDiep);
//				} catch (Exception e) {
//				}
//
//				if (rTCTN == null) {
//					continue;
//				}
//
//				/* DO DU LIEU TRA VE - CAP NHAT LAI KET QUA */
//				XPath xPath = null;
//				xPath = XPathFactory.newInstance().newXPath();
//				Node nodeKetQuaTraCuu = null;
//				nodeKetQuaTraCuu = (Node) xPath.evaluate("/KetQuaTraCuu", rTCTN, XPathConstants.NODE);
//				MaKetQua = commons.getTextFromNodeXML(
//						(Element) xPath.evaluate("MaKetQua", nodeKetQuaTraCuu, XPathConstants.NODE));
//
//				if ("2".equals(MaKetQua)) {
//					continue;
//				}
//
//				Node nodeTDiep = null;
//				String checkMLTDiep = "";
//				boolean check_ = false;
//				String MLoi = "";
//				String MTLoi = "";
//				String MTDTChieu = "";
//				String CQT_MLTDiep = "";
//				String MLoi1 = "";
//				String MTLoi1 = "";
//				String CQT_MLTDiep1 = "";
//				for (int i = 1; i <= 20; i++) {
//
//					if (xPath.evaluate("DuLieu/TDiep[" + i + "]", nodeKetQuaTraCuu, XPathConstants.NODE) == null)
//						break;
//					nodeTDiep = (Node) xPath.evaluate("DuLieu/TDiep[" + i + "]", nodeKetQuaTraCuu, XPathConstants.NODE);
//					checkMLTDiep = commons.getTextFromNodeXML(
//							(Element) xPath.evaluate("TTChung/MLTDiep", nodeTDiep, XPathConstants.NODE));
//					if (checkMLTDiep.equals("202"))
//						break;
//					if (checkMLTDiep.equals("204")) {
//						check_ = true;
//						MLoi1 = commons.getTextFromNodeXML((Element) xPath
//								.evaluate("DLieu/TBao/DLTBao/LCMa/DSLDo/LDo/MLoi", nodeTDiep, XPathConstants.NODE));
//						MTLoi1 = commons.getTextFromNodeXML((Element) xPath
//								.evaluate("DLieu/TBao/DLTBao/LCMa/DSLDo/LDo/MTLoi", nodeTDiep, XPathConstants.NODE));
//						CQT_MLTDiep1 = checkMLTDiep;
//					}
//				}
//
//				if (nodeTDiep == null) {
//					continue;
//				}
//
//				CQT_MLTDiep = commons.getTextFromNodeXML(
//						(Element) xPath.evaluate("TTChung/MLTDiep", nodeTDiep, XPathConstants.NODE));
//				MTDTChieu = commons.getTextFromNodeXML(
//						(Element) xPath.evaluate("TTChung/MTDTChieu", nodeTDiep, XPathConstants.NODE));
//
//				Document findEinvoice = new Document("_id", new ObjectId(_id)).append("IsDelete",
//						new Document("$ne", true));
//
//				if (!CQT_MLTDiep.equals("202")) {
//					if (check_ == true) {
//						MLoi = MLoi1;
//						MTLoi = MTLoi1;
//						/* LUU LAI FILE XML LOI */
//						String dir = docTmp.get("Dir", "");
//						String fileName = _id + "_" + CQT_MLTDiep1 + ".xml";
//						boolean boo = false;
//						try {
//							boo = commons.docW3cToFile(rTCTN, dir, fileName);
//						} catch (Exception e) {
//						}
//						if (!boo) {
//							continue;
//						}
//
//						/* CAP NHAT TRANG THAI COMPLETE - TRANG THAI CQT */
//						options = new FindOneAndUpdateOptions();
//						options.upsert(false);
//						options.maxTime(5000, TimeUnit.MILLISECONDS);
//						options.returnDocument(ReturnDocument.AFTER);
//						mongoTemplate.getCollection("EInvoice").findOneAndUpdate(findEinvoice,
//								new Document("$set",
//										new Document("EInvoiceStatus", Constants.INVOICE_STATUS.ERROR_CQT)
//												.append("CQT_Date", LocalDate.now())
//												.append("LDo", new Document("MLoi", MLoi).append("MTLoi", MTLoi))),
//								options);
//					}
//				}
//
//				if ("|202|".indexOf("|" + CQT_MLTDiep + "|") == -1) {
//					continue;
//				}
//
//				String MCCQT = commons.getTextFromNodeXML(
//						(Element) xPath.evaluate("DLieu/HDon/MCCQT", nodeTDiep, XPathConstants.NODE));
//
//				String dir = docTmp.get("Dir", "");
//				String fileName = _id + "_" + MCCQT + ".xml";
//
//				boolean boo = false;
//				try {
//					boo = commons.docW3cToFile(rTCTN, dir, fileName);
//				} catch (Exception e) {
//					continue;
//				}
//				if (!boo) {
//					continue;
//				}
//
//				/* CAP NHAT TRANG THAI COMPLETE - TRANG THAI CQT */
//				options = new FindOneAndUpdateOptions();
//				options.upsert(false);
//				options.maxTime(5000, TimeUnit.MILLISECONDS);
//				options.returnDocument(ReturnDocument.AFTER);
//
//				mongoTemplate.getCollection("EInvoice").findOneAndUpdate(findEinvoice,
//						new Document("$set",
//								new Document("EInvoiceStatus", Constants.INVOICE_STATUS.COMPLETE).append("MCCQT", MCCQT)
//										.append("MTDTChieu", MTDTChieu).append("CQT_Date", LocalDate.now())
//										.append("LDo", new Document("MLoi", "").append("MTLoi", ""))),
//						options);
//
//				String iddc = "";
//				try {
//					iddc = docTmp.getEmbedded(Arrays.asList("EInvoiceDetail", "TTChung", "TTHDLQuan", "_id"), "");
//				} catch (Exception e) {
//					iddc = docTmp
//							.getEmbedded(Arrays.asList("EInvoiceDetail", "TTChung", "TTHDLQuan", "_id"), ObjectId.class)
//							.toString();
//				}
//
//				if (!iddc.equals("")) {
//					ObjectId objectIddc = null;
//					try {
//						objectIddc = new ObjectId(iddc);
//					} catch (Exception e) {
//						continue;
//					}
//
//					Document docFind1 = new Document("IssuerId", IssuerId).append("IsDelete", new Document("$ne", true))
//							.append("_id", objectIddc).append("SignStatusCode", Constants.INVOICE_SIGN_STATUS.SIGNED)
//							.append("EInvoiceStatus",
//									new Document("$in", Arrays.asList(Constants.INVOICE_STATUS.COMPLETE,
//											Constants.INVOICE_STATUS.ADJUSTED, Constants.INVOICE_STATUS.REPLACED)));
//					options = new FindOneAndUpdateOptions();
//					options.upsert(true);
//					options.maxTime(5000, TimeUnit.MILLISECONDS);
//					options.returnDocument(ReturnDocument.AFTER);
//
//					if ("1".equals(docTmp.getEmbedded(Arrays.asList("EInvoiceDetail", "TTChung", "TTHDLQuan", "TCHDon"),
//							""))) {
//						mongoTemplate.getCollection("EInvoice").findOneAndUpdate(docFind1,
//								new Document("$set", new Document("EInvoiceStatus", "REPLACED")), options);
//					} else if ("2".equals(docTmp
//							.getEmbedded(Arrays.asList("EInvoiceDetail", "TTChung", "TTHDLQuan", "TCHDon"), ""))) {
//						mongoTemplate.getCollection("EInvoice").findOneAndUpdate(docFind1,
//								new Document("$set", new Document("EInvoiceStatus", "ADJUSTED")), options);
//					}
//				}
//
//			} catch (Exception e) {
//				continue;
//			}
//		}
//	}

//	@Scheduled(cron = "0 0 12,0 * * *")
//	public void scheduleClearInvoiceTmp() throws Exception {
//
//		FindOneAndDeleteOptions options = null;
//		Document docFind = new Document("IsDelete", new Document("$ne", true));
//		/* CAP NHAT TRANG THAI COMPLETE - TRANG THAI CQT */
//		options = new FindOneAndDeleteOptions();
//		options.maxTime(5000, TimeUnit.MILLISECONDS);
//		DeleteResult clearData = mongoTemplate.getCollection("EInvoiceTmp").deleteMany(docFind);
//
//	}
//}
