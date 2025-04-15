package vn.sesgroup.hddt.user.impl;

import java.io.File;
import org.w3c.dom.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.bson.Document;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.api.message.JSONRoot;
import com.api.message.Msg;
import com.api.message.MsgHeader;
import com.api.message.MsgPage;
import com.api.message.MsgRsp;
import com.api.message.MspResponseStatus;
import com.fasterxml.jackson.databind.JsonNode;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.FindOneAndUpdateOptions;
import com.mongodb.client.model.ReturnDocument;

import vn.sesgroup.hddt.configuration.ConfigConnectMongo;
import vn.sesgroup.hddt.user.dao.AbstractDAO;
import vn.sesgroup.hddt.user.dao.XuLyNghiepVuDAO;
import vn.sesgroup.hddt.utility.Constants;
import vn.sesgroup.hddt.utility.Json;

@Repository
@Transactional
public class XuLyNghiepVuImpl extends AbstractDAO implements XuLyNghiepVuDAO {
	@Autowired
	ConfigConnectMongo cfg;

	@Override
	public MsgRsp getInvoiceByMTDiep(JSONRoot jsonRoot) throws Exception {
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

		MsgRsp rsp = new MsgRsp(header);
		rsp.setMsgPage(page);
		MspResponseStatus responseStatus = null;

		String mtdiep = commons.getTextJsonNode(jsonData.at("/MTDiep")).replaceAll("\\s", "");
		ObjectId objectId = null;

		List<Document> pipeline = new ArrayList<Document>();

		Document docMatch = new Document("MTDiep", mtdiep).append("IsDelete", new Document("$ne", true));

		pipeline = new ArrayList<Document>();
		pipeline.add(new Document("$match", docMatch));
		pipeline.addAll(createFacetForSearchNotSort(page));

		Document docTmp = null;
		MongoClient mongoClient = cfg.mongoClient();
		MongoCollection<Document> collection = mongoClient.getDatabase(cfg.dbName).getCollection("EInvoice");

		try {
			// EINVOICE
			docTmp = collection.aggregate(pipeline).allowDiskUse(true).iterator().next();

			if (docTmp == null) {
				// EINVOICE BH
				collection = mongoClient.getDatabase(cfg.dbName).getCollection("EInvoiceBH");

				docTmp = collection.aggregate(pipeline).allowDiskUse(true).iterator().next();
			}

			if (docTmp == null) {
				// EINVOICE PXK
				collection = mongoClient.getDatabase(cfg.dbName).getCollection("EInvoicePXK");

				docTmp = collection.aggregate(pipeline).allowDiskUse(true).iterator().next();
			}

			if (docTmp == null) {
				// EINVOICE PXK DL
				collection = mongoClient.getDatabase(cfg.dbName).getCollection("EInvoicePXKDL");

				docTmp = collection.aggregate(pipeline).allowDiskUse(true).iterator().next();
			}

		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (mongoClient != null)
				mongoClient.close();
		}

		if (docTmp != null) {
			page.setTotalRows(docTmp.getInteger("total", 0));
			rsp.setMsgPage(page);
			List<Document> rows = null;
			if (docTmp.get("data") != null && docTmp.get("data") instanceof List) {
				rows = docTmp.getList("data", Document.class);
			}

			ArrayList<HashMap<String, Object>> rowsReturn = new ArrayList<HashMap<String, Object>>();
			HashMap<String, Object> hItem = null;
			if (null != rows) {
				for (Document doc : rows) {
					objectId = (ObjectId) doc.get("_id");

					hItem = new HashMap<String, Object>();
					hItem.put("_id", objectId.toString());
					hItem.put("EInvoiceStatus", doc.get("EInvoiceStatus"));
					hItem.put("SignStatusCode", doc.get("SignStatusCode"));
					hItem.put("MCCQT", doc.get("MCCQT"));
					hItem.put("MTDiep", doc.get("MTDiep"));
					hItem.put("EInvoiceDetail", doc.get("EInvoiceDetail"));
					hItem.put("InfoCreated", doc.get("InfoCreated"));
					hItem.put("LDo", doc.get("LDo"));
					hItem.put("HDSS", doc.get("HDSS"));
					hItem.put("MTDiep", doc.get("MTDiep"));
					hItem.put("MTDTChieu", doc.get("MTDTChieu"));
					hItem.put("SendCQT_Date", doc.get("SendCQT_Date"));
					hItem.put("CQT_Date", doc.get("CQT_Date"));

					rowsReturn.add(hItem);
				}
			}
			responseStatus = new MspResponseStatus(0, "SUCCESS");
			rsp.setResponseStatus(responseStatus);

			HashMap<String, Object> mapDataR = new HashMap<String, Object>();
			mapDataR.put("rows", rowsReturn);
			rsp.setObjData(mapDataR);
			return rsp;
		}
		responseStatus = new MspResponseStatus(999, "Không tìm thấy hóa đơn.");
		rsp.setResponseStatus(responseStatus);
		return rsp;
	}

	@Override
	public MsgRsp updateMCQT(JSONRoot jsonRoot) throws Exception {
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

		MsgRsp rsp = new MsgRsp(header);
		rsp.setMsgPage(page);
		MspResponseStatus responseStatus = null;

		String collectionName = "";
		String mtdiep = commons.getTextJsonNode(jsonData.at("/MTDiep")).replaceAll("\\s", "");
		String mccqt = commons.getTextJsonNode(jsonData.at("/MCCQT")).replaceAll("\\s", "");
		String mtdtchieu = commons.getTextJsonNode(jsonData.at("/MTDTChieu")).replaceAll("\\s", "");

		List<Document> pipeline = new ArrayList<Document>();
		Document docMatch = new Document("MTDiep", mtdiep).append("IsDelete", new Document("$ne", true));

		pipeline = new ArrayList<Document>();
		pipeline.add(new Document("$match", docMatch));
		pipeline.addAll(createFacetForSearchNotSort(page));

		Document docTmp = null;
		MongoClient mongoClient = cfg.mongoClient();
		MongoCollection<Document> collection = mongoClient.getDatabase(cfg.dbName).getCollection("EInvoice");

		try {
			// EINVOICE
			docTmp = collection.aggregate(pipeline).allowDiskUse(true).iterator().next();
			collectionName = "EInvoice";

			if (docTmp == null) {
				// EINVOICE BH
				collection = mongoClient.getDatabase(cfg.dbName).getCollection("EInvoiceBH");

				docTmp = collection.aggregate(pipeline).allowDiskUse(true).iterator().next();
				collectionName = "EInvoiceBH";
			}

			if (docTmp == null) {
				// EINVOICE PXK
				collection = mongoClient.getDatabase(cfg.dbName).getCollection("EInvoicePXK");

				docTmp = collection.aggregate(pipeline).allowDiskUse(true).iterator().next();
				collectionName = "EInvoicePXK";
			}

			if (docTmp == null) {
				// EINVOICE PXK DL
				collection = mongoClient.getDatabase(cfg.dbName).getCollection("EInvoicePXKDL");

				docTmp = collection.aggregate(pipeline).allowDiskUse(true).iterator().next();
				collectionName = "EInvoicePXKDL";
			}

		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (mongoClient != null)
				mongoClient.close();
		}

		if (docTmp == null) {
			responseStatus = new MspResponseStatus(999, "Không tìm thấy hóa đơn.");
			rsp.setResponseStatus(responseStatus);
			return rsp;
		}
		List<Document> listDocTmp = docTmp.getList("data", Document.class);
		Document doct = (Document) listDocTmp.get(0);

		// get cur document
		File file = null;
		String dir = doct.get("Dir", "");
		String _id = doct.getObjectId("_id").toString();
		String fileName = _id + "_signed.xml";
		file = new File(dir, fileName);
		if (!file.exists()) {
			responseStatus = new MspResponseStatus(999, "Không có thông tin hóa đơn.");
			rsp.setResponseStatus(responseStatus);
			return rsp;
		}
		org.w3c.dom.Document curDoc = commons.fileToDocument(file, true);
		Element root = curDoc.getDocumentElement();
		Element mccqtElement = curDoc.createElement("MCCQT");
		mccqtElement.setTextContent(mccqt);
		root.appendChild(mccqtElement);

		// create new document
		Element temp = null;
		DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
		org.w3c.dom.Document newDoc = dBuilder.newDocument();
		newDoc.setXmlStandalone(true);

		Element newRoot = newDoc.createElement("KetQuaTraCuu");
		temp = newDoc.createElement("MaGD");
		newRoot.appendChild(temp);

		temp = newDoc.createElement("MaKetQua");
		temp.setTextContent("0");
		newRoot.appendChild(temp);

		temp = newDoc.createElement("MoTaKetQua");
		temp.setTextContent("CQT đã trả kết quả xử lý thông điệp");
		newRoot.appendChild(temp);
		// KetQuaTraCuu / DuLieu
		Element dulieu = newDoc.createElement("DuLieu");

		// KetQuaTraCuu / DuLieu / TDiep1
		Element tdiep1 = newDoc.createElement("TDiep");
		// KetQuaTraCuu / DuLieu / TDiep1 / TTChung
		Element ttchung = newDoc.createElement("TTChung");
		temp = newDoc.createElement("PBan");
		ttchung.appendChild(temp);
		temp = newDoc.createElement("MNGui");
		temp.setTextContent("TCT");
		ttchung.appendChild(temp);
		temp = newDoc.createElement("MNNhan");
		ttchung.appendChild(temp);
		temp = newDoc.createElement("MLTDiep");
		temp.setTextContent("999");
		ttchung.appendChild(temp);
		temp = newDoc.createElement("MTDiep");
		temp.setTextContent(mtdiep);
		ttchung.appendChild(temp);
		temp = newDoc.createElement("MTDTChieu");
		temp.setTextContent(mtdtchieu);
		ttchung.appendChild(temp);

		// KetQuaTraCuu / DuLieu / TDiep1 / DLieu
		Element dlieu = newDoc.createElement("DLieu");
		Element tbao = newDoc.createElement("TBao");
		temp = newDoc.createElement("MTDiep");
		temp.setTextContent(mtdiep);
		tbao.appendChild(temp);
		temp = newDoc.createElement("MNGui");
		tbao.appendChild(temp);
		temp = newDoc.createElement("NNhan");
		temp.setTextContent(LocalDate.now().toString());
		tbao.appendChild(temp);
		temp = newDoc.createElement("TTTNhan");
		temp.setTextContent("0");
		tbao.appendChild(temp);
		dlieu.appendChild(tbao);

		tdiep1.appendChild(ttchung);
		tdiep1.appendChild(dlieu);

		// KetQuaTraCuu / DuLieu / TDiep2
		Element tdiep2 = newDoc.createElement("TDiep");
		// KetQuaTraCuu / DuLieu / TDiep2 / TTChung
		ttchung = newDoc.createElement("TTChung");
		temp = newDoc.createElement("PBan");
		ttchung.appendChild(temp);
		temp = newDoc.createElement("MNGui");
		temp.setTextContent("TCT");
		ttchung.appendChild(temp);
		temp = newDoc.createElement("MNNhan");
		ttchung.appendChild(temp);
		temp = newDoc.createElement("MLTDiep");
		temp.setTextContent("202");
		ttchung.appendChild(temp);
		temp = newDoc.createElement("MTDiep");
		temp.setTextContent(mtdiep);
		ttchung.appendChild(temp);
		temp = newDoc.createElement("MTDTChieu");
		temp.setTextContent(mtdtchieu);
		ttchung.appendChild(temp);
		temp = newDoc.createElement("MST");
		temp.setTextContent(doct.getEmbedded(Arrays.asList("InfoCreated", "CreateUserName"), String.class));
		ttchung.appendChild(temp);
		temp = newDoc.createElement("SLuong");
		ttchung.appendChild(temp);

		// KetQuaTraCuu / DuLieu / TDiep2 / DLieu
		dlieu = newDoc.createElement("DLieu");
		Node importedRoot = newDoc.importNode(root, true);
		dlieu.appendChild(importedRoot);

		tdiep2.appendChild(ttchung);
		tdiep2.appendChild(dlieu);

		dulieu.appendChild(tdiep1);
		dulieu.appendChild(tdiep2);

		newRoot.appendChild(dulieu);
		newDoc.appendChild(newRoot);

		String newFileName = _id + "_" + mccqt + ".xml";
		file = new File(dir, newFileName);
		boolean boo = false;
		try {
			boo = commons.docW3cToFile(newDoc, dir, newFileName);
		} catch (Exception e) {
		}
		if (!boo) {
			responseStatus = new MspResponseStatus(9999, "Lưu tập tin mới khi update CQT không thành công.");
			rsp.setResponseStatus(responseStatus);
			return rsp;
		}

		FindOneAndUpdateOptions options = new FindOneAndUpdateOptions();
		options.upsert(false);
		options.maxTime(5000, TimeUnit.MILLISECONDS);
		options.returnDocument(ReturnDocument.AFTER);

		mongoClient = cfg.mongoClient();
		try {
			collection = mongoClient.getDatabase(cfg.dbName).getCollection(collectionName);
			collection.findOneAndUpdate(docMatch,
					new Document("$set",
							new Document("EInvoiceStatus", Constants.INVOICE_STATUS.COMPLETE).append("MCCQT", mccqt)
									.append("MTDTChieu", mtdtchieu).append("CQT_Date", LocalDate.now())
									.append("LDo", new Document("MLoi", "").append("MTLoi", ""))),
					options);
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (mongoClient != null)
				mongoClient.close();
		}

		responseStatus = new MspResponseStatus(0, "Update mã cqt thành công.");
		rsp.setResponseStatus(responseStatus);
		return rsp;
	}
}
