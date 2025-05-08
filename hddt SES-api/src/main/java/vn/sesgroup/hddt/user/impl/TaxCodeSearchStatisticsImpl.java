package vn.sesgroup.hddt.user.impl;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import org.bson.Document;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.api.message.JSONRoot;
import com.api.message.Msg;
import com.api.message.MsgHeader;
import com.api.message.MsgPage;
import com.api.message.MsgRsp;
import com.api.message.MspResponseStatus;
import com.fasterxml.jackson.databind.JsonNode;

import vn.sesgroup.hddt.dto.FileInfo;
import vn.sesgroup.hddt.user.dao.AbstractDAO;
import vn.sesgroup.hddt.user.dao.TaxCodeSearchStatisticsDAO;
import vn.sesgroup.hddt.utility.Constants;
import vn.sesgroup.hddt.utility.Json;

@Repository
@Transactional
public class TaxCodeSearchStatisticsImpl extends AbstractDAO implements TaxCodeSearchStatisticsDAO {
	@Autowired
	MongoTemplate mongoTemplate;

	private void buildDocMatch(String toDate, String fromDate, Document docMatch) {
		Document docMatchDateDN = null;
		LocalDate dateTo = null;
		LocalDate dateFrom = null;
		dateTo = "".equals(toDate) || !commons.checkLocalDate(toDate, Constants.FORMAT_DATE.FORMAT_DATE_WEB) ? null
				: commons.convertStringToLocalDate(toDate, Constants.FORMAT_DATE.FORMAT_DATE_WEB);
		dateFrom = "".equals(fromDate) || !commons.checkLocalDate(fromDate, Constants.FORMAT_DATE.FORMAT_DATE_WEB)
				? null
				: commons.convertStringToLocalDate(fromDate, Constants.FORMAT_DATE.FORMAT_DATE_WEB);
		if (null != dateFrom || null != dateTo) {
			docMatchDateDN = new Document();
			if (null != dateFrom)
				docMatchDateDN.append("$gte", dateFrom);
			if (null != dateTo)
				docMatchDateDN.append("$lte", dateTo);
		}

		if (null != docMatchDateDN)
			docMatch.append("SearchDate", docMatchDateDN);
	}

	@Override
	public MsgRsp list(JSONRoot jsonRoot) throws Exception {
		Msg msg = jsonRoot.getMsg();
		MsgHeader header = msg.getMsgHeader();
		MsgPage page = msg.getMsgPage();
		Object objData = msg.getObjData();

		String toDate = "";
		String fromDate = "";
		JsonNode jsonData = null;
		if (objData != null) {
			jsonData = Json.serializer().nodeFromObject(objData);
			toDate = commons.getTextJsonNode(jsonData.at("/ToDate")).replaceAll("\\s", "");
			fromDate = commons.getTextJsonNode(jsonData.at("/FromDate")).replaceAll("\\s", "");
		}

		MsgRsp rsp = new MsgRsp(header);
		MspResponseStatus responseStatus = null;
		
		Iterator<Document> iter = null;
		Document docTmp = null;
		List<Document> docs = null;

		Document docMatch = new Document("IsDelete", new Document("$ne", true));
		buildDocMatch(toDate, fromDate, docMatch);

		List<Document> pipeline = new ArrayList<Document>();
		pipeline.add(new Document("$match", docMatch));
		
//		pipeline.add(new Document("$sort", new Document("SearchDate", 1)));
		page.setFieldSort("SearchDate");
		page.setTypeSort(1);
		pipeline.addAll(createFacetForSearch(page));
		iter = mongoTemplate.getCollection("TaxCodeSearchStatistics").aggregate(pipeline).allowDiskUse(true).iterator();
		if (iter.hasNext()) {
			docTmp = iter.next();
		}
		
		if (docTmp == null) {
			responseStatus = new MspResponseStatus(999, Constants.MAP_ERROR.get(999));
			rsp.setResponseStatus(responseStatus);
			return rsp;
		}

		page.setTotalRows(docTmp.getInteger("total", 0));
		rsp.setMsgPage(page);

		
		if (docTmp != null && docTmp.get("data") instanceof List) {
			docs = docTmp.getList("data", Document.class);
		}
		List<HashMap<String, Object>> response = new ArrayList<HashMap<String, Object>>();
		
		if (null != docs) {
			HashMap<String, Object> resItem = null;
			ObjectId objectIdCTS = null;
			
			for (Document doc : docs) {
				resItem = new HashMap<String, Object>();
				
				objectIdCTS = (ObjectId) doc.get("_id");
				resItem.put("_id", objectIdCTS.toString());
				resItem.put("IssuerName", doc.get("IssuerName") != null ? doc.get("IssuerName") : "");
				resItem.put("IssuerTaxCode", doc.get("IssuerTaxCode") != null ? doc.get("IssuerTaxCode") : "");
				resItem.put("SearchDate", doc.get("SearchDate") != null ? doc.get("SearchDate") : "");
				resItem.put("TaxCodeSearched", doc.get("TaxCode") != null ? doc.get("TaxCode") : "");
				resItem.put("CompanyNameSearched", doc.get("CompanyName") != null ? doc.get("CompanyName") : "");
				resItem.put("Address", doc.get("Address") != null ? doc.get("Address") : "");
				resItem.put("Email", doc.get("Email") != null ? doc.get("Email") : "");
				resItem.put("Phone", doc.get("Phone") != null ? doc.get("Phone") : "");
				response.add(resItem);
			}
		}

		responseStatus = new MspResponseStatus(0, "Lấy thông tin thành công.");
		rsp.setResponseStatus(responseStatus);
		rsp.setObjData(response);
		return rsp;
	}

	@Override
	public FileInfo exportExcel(JSONRoot jsonRoot) throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

}
