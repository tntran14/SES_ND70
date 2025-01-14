package vn.sesgroup.hddt.user.impl;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import org.bson.Document;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import com.api.message.JSONRoot;
import com.api.message.Msg;
import com.api.message.MsgHeader;
import com.api.message.MsgPage;
import com.api.message.MsgRsp;
import com.api.message.MspResponseStatus;
import com.fasterxml.jackson.databind.JsonNode;
import com.mongodb.client.model.FindOneAndUpdateOptions;
import com.mongodb.client.model.ReturnDocument;

import vn.sesgroup.hddt.user.dao.AbstractDAO;
import vn.sesgroup.hddt.user.dao.EInvoiceDAO;
import vn.sesgroup.hddt.user.service.TCTNService;
import vn.sesgroup.hddt.utility.Constants;
import vn.sesgroup.hddt.utility.Json;

public abstract  class CreateThread  extends AbstractDAO implements EInvoiceDAO , Runnable{
	@Autowired MongoTemplate mongoTemplate;
	@Autowired TCTNService tctnService;
	@Override
	public MsgRsp refreshStatusCQT(JSONRoot jsonRoot) throws Exception {
		 MsgRsp rsp = new MsgRsp();
		Msg msg = jsonRoot.getMsg();
		MsgHeader header = msg.getMsgHeader();
		MsgPage page = msg.getMsgPage();
		String idxl = "";
		Object objData = msg.getObjData();
		
		JsonNode jsonData = null;
		if(objData != null) {
			jsonData = Json.serializer().nodeFromObject(msg.getObjData());
		}else{
			throw new Exception("Lỗi dữ liệu đầu vào");
		}
		String _id = commons.getTextJsonNode(jsonData.at("/_id")).replaceAll("\\s", "");

		   List<String> ids = new ArrayList<>();
		   ids.add(_id);
		for(int i = 0; i<10; i++) {
			 idxl = ids.get(i);
			  rsp = new MsgRsp(header);
				rsp.setMsgPage(page);
				MspResponseStatus responseStatus = null;
				
				ObjectId objectId = null;
				try {
					objectId = new ObjectId(_id);
				}catch(Exception e) {}
				
				FindOneAndUpdateOptions options = null;
				/*KIEM TRA XEM THONG TIN TKHAI CO TON TAI KHONG*/
				Document docFind = new Document("IssuerId", header.getIssuerId())
						.append("_id", objectId).append("IsDelete", new Document("$ne", true))
						.append("SignStatusCode", "SIGNED").append("EInvoiceStatus", "PROCESSING");
				
				Document docTmp = null;
				Iterable<Document> cursor = mongoTemplate.getCollection("EInvoice").find(docFind);
				Iterator<Document> iter = cursor.iterator();
				if(iter.hasNext()) {
					docTmp = iter.next();
				} 
				if(null == docTmp) {
					responseStatus = new MspResponseStatus(9999, "Không tìm thấy thông tin hóa đơn.");
					rsp.setResponseStatus(responseStatus);
					return rsp;
				}
				String MTDiep = docTmp.get("MTDiep", "");
				org.w3c.dom.Document rTCTN = tctnService.callTraCuuThongDiep(MTDiep);
				if(rTCTN == null) {
					responseStatus = new MspResponseStatus(9999, "Kết nối với TCTN không thành công.");
					rsp.setResponseStatus(responseStatus);
					return rsp;
				}
				
				/*DO DU LIEU TRA VE - CAP NHAT LAI KET QUA*/
				XPath xPath = XPathFactory.newInstance().newXPath();
				Node nodeKetQuaTraCuu = (Node) xPath.evaluate("/KetQuaTraCuu", rTCTN, XPathConstants.NODE);
				String MaKetQua = commons.getTextFromNodeXML((Element) xPath.evaluate("MaKetQua", nodeKetQuaTraCuu, XPathConstants.NODE));
				String MoTaKetQua = commons.getTextFromNodeXML((Element) xPath.evaluate("MoTaKetQua", nodeKetQuaTraCuu, XPathConstants.NODE));
				
				if(!"0".equals(MaKetQua)) {
					responseStatus = new MspResponseStatus(9999, MoTaKetQua);
					rsp.setResponseStatus(responseStatus);
					return rsp;
				}
				
//				Node nodeTDiep = (Node) xPath.evaluate("DuLieu/TDiep[last()]", nodeKetQuaTraCuu, XPathConstants.NODE);
				Node nodeTDiep = null;
				for(int i1 = 1; i1<=5; i1++) {
					if(xPath.evaluate("DuLieu/TDiep[" + i1 + "]", nodeKetQuaTraCuu, XPathConstants.NODE) == null) break;
					nodeTDiep = (Node) xPath.evaluate("DuLieu/TDiep[" + i1 + "]", nodeKetQuaTraCuu, XPathConstants.NODE);
//					if(xPath.evaluate("DLieu/*/DSCKS", nodeTDiep, XPathConstants.NODE) != null)
					if(xPath.evaluate("DLieu/*/MCCQT", nodeTDiep, XPathConstants.NODE) != null)
						break;
				}
				

				
				
				if(nodeTDiep == null) {
					responseStatus = new MspResponseStatus(9999, "Không đọc được kết quả tra cứu.");
					rsp.setResponseStatus(responseStatus);
					return rsp;
				}
				String CQT_MLTDiep = commons.getTextFromNodeXML((Element) xPath.evaluate("TTChung/MLTDiep", nodeTDiep, XPathConstants.NODE));
				String MLoi = "";
				String MTLoi = "";
				if("204".equals(CQT_MLTDiep)) {
					MLoi = commons.getTextFromNodeXML((Element) xPath.evaluate("DLieu/TBao/DLTBao/LCMa/DSLDo/LDo/MLoi", nodeTDiep, XPathConstants.NODE));
					MTLoi = commons.getTextFromNodeXML((Element) xPath.evaluate("DLieu/TBao/DLTBao/LCMa/DSLDo/LDo/MTLoi", nodeTDiep, XPathConstants.NODE));
					
					/*LUU LAI FILE XML LOI*/
					String dir = docTmp.get("Dir", "");
					String fileName = _id + "_" + CQT_MLTDiep + ".xml";
					boolean boo = false;
					try {
						boo = commons.docW3cToFile(rTCTN, dir, fileName);
					}catch(Exception e) {}
					if(!boo) {
						responseStatus = new MspResponseStatus(9999, "Lưu tập tin trả về từ CQT không thành công.");
						rsp.setResponseStatus(responseStatus);
						return rsp;
					}
					
					/*CAP NHAT TRANG THAI COMPLETE - TRANG THAI CQT*/
					options = new FindOneAndUpdateOptions();
					options.upsert(false);
					options.maxTime(5000, TimeUnit.MILLISECONDS);
					options.returnDocument(ReturnDocument.AFTER);
					
					mongoTemplate.getCollection("EInvoice").findOneAndUpdate(
						docFind, 
						new Document("$set", 
							new Document("EInvoiceStatus", Constants.INVOICE_STATUS.ERROR_CQT)
							.append("LDo", 
								new Document("MLoi", MLoi).append("MTLoi", MTLoi)
							)
						), 
						options
					);
					
					responseStatus = new MspResponseStatus(0, "".equals(MTLoi)? "CQT chưa có thông báo kết quả trả về.": MTLoi);
					rsp.setResponseStatus(responseStatus);
					return rsp;
				}
				if("|202|".indexOf("|" + CQT_MLTDiep + "|") == -1) {
					responseStatus = new MspResponseStatus(9999, "CQT chưa có thông báo kết quả trả về.");
					rsp.setResponseStatus(responseStatus);
					return rsp;
				}
				
				String MCCQT = commons.getTextFromNodeXML((Element) xPath.evaluate("DLieu/HDon/MCCQT", nodeTDiep, XPathConstants.NODE));
				
				String dir = docTmp.get("Dir", "");
				String fileName = _id + "_" + MCCQT + ".xml";
				boolean boo = false;
				try {
					boo = commons.docW3cToFile(rTCTN, dir, fileName);
				}catch(Exception e) {}
				if(!boo) {
					responseStatus = new MspResponseStatus(9999, "Lưu tập tin trả về từ CQT không thành công.");
					rsp.setResponseStatus(responseStatus);
					return rsp;
				}
				
				/*CAP NHAT TRANG THAI COMPLETE - TRANG THAI CQT*/
				options = new FindOneAndUpdateOptions();
				options.upsert(false);
				options.maxTime(5000, TimeUnit.MILLISECONDS);
				options.returnDocument(ReturnDocument.AFTER);
				
				mongoTemplate.getCollection("EInvoice").findOneAndUpdate(
					docFind, 
					new Document("$set", 
						new Document("EInvoiceStatus", Constants.INVOICE_STATUS.COMPLETE)
						.append("MCCQT", MCCQT)
					), 
					options
				);
				responseStatus = new MspResponseStatus(0, Constants.MAP_ERROR.get(0));
				rsp.setResponseStatus(responseStatus);
				return rsp;
	 
		}
		try {
			Thread.sleep(10000);
		} catch (Exception e) {
			// TODO: handle exception
		}
		return rsp;


	}


}
