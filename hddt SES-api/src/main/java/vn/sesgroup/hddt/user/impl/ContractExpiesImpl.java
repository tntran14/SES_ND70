package vn.sesgroup.hddt.user.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
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

import vn.sesgroup.hddt.user.dao.AbstractDAO;
import vn.sesgroup.hddt.user.dao.ContractExpiesDAO;
import vn.sesgroup.hddt.user.service.TCTNService;

@Repository
@Transactional
public class ContractExpiesImpl extends AbstractDAO implements ContractExpiesDAO{
	private static final Logger log = LogManager.getLogger(ContractExpiesImpl.class);
	@Autowired MongoTemplate mongoTemplate;
	@Autowired TCTNService tctnService;
	


	
	@Override
	public MsgRsp list(JSONRoot jsonRoot) throws Exception {
		Msg msg = jsonRoot.getMsg();
		MsgHeader header = msg.getMsgHeader();
		MsgPage page = msg.getMsgPage();		
		Object objData = msg.getObjData();
		
		ObjectId objectId = null;
		Document docTmp = null;
		Iterable<Document> cursor = null;
		Iterator<Document> iter = null;
		
		Iterable<Document> cursor1 = null;
		Iterator<Document> iter1 = null;
		

		
		Iterable<Document> cursor3 = null;
		Iterator<Document> iter3 = null;
		
		List<Document> pipeline = new ArrayList<Document>();
		List<Document> pipeline1 = new ArrayList<Document>();
		List<Document> pipeline2 = new ArrayList<Document>();
		MsgRsp rsp = new MsgRsp(header);
		MspResponseStatus responseStatus = null;
			
		Document docMatch = new Document("ActiveFlag",new Document("$ne", false));
		
		pipeline = new ArrayList<Document>();
		pipeline.add(new Document("$match", docMatch));
		pipeline.add(new Document("$lookup", new Document("from", "DMDepot")
				.append("pipeline", Arrays.asList(
				new Document("$match",
						new Document("IsDelete", new Document("$ne", true)))				
						)).append("as", "DMDepot")));
		
	
		cursor = mongoTemplate.getCollection("ApiLicenseKey").aggregate(pipeline);
		
		iter = cursor.iterator();
		if (iter.hasNext()) {
			docTmp = iter.next();
		}
			
		
		List<Document> DMDepot = null;
		if (docTmp.get("DMDepot") != null ) {
			DMDepot = docTmp.getList("DMDepot", Document.class);
		}
		
		int dem = DMDepot.size();
		
		List<Document> rows = null;
		
		ArrayList<HashMap<String, Object>> rowsReturn = new ArrayList<HashMap<String, Object>>();

		HashMap<String, Object> hItem = null;
		HashMap<String, Object> hItem1 = null;
		Document docTmp1 = null;
		Document docTmp2 = null;
		if(null != DMDepot) {
			
			for(Document doc: DMDepot) {
				
			String TaxCode = doc.get("TaxCode", "");				
			Document findMST = new Document("NMUA.TaxCode", TaxCode).append("IsDelete", new Document("$ne", true)).append("IsActive", true);
			
			Document findIssuer_ = new Document("TaxCode",TaxCode).append("IsDelete", new Document("$ne", true));
			
			pipeline1 = new ArrayList<Document>();
			pipeline1.add(new Document("$match", findIssuer_));
			pipeline1.add(new Document("$lookup", new Document("from", "Contract")
					.append("pipeline", Arrays.asList(
					new Document("$match", new Document("NMUA.TaxCode", TaxCode)
							.append("IsDelete", new Document("$ne", true))
							.append("IsActive", true)
							))).append("as", "Contract")));
			
		
			cursor1 = mongoTemplate.getCollection("Issuer").aggregate(pipeline1);
			
			iter1 = cursor1.iterator();
			if (iter1.hasNext()) {
				docTmp1 = iter1.next();
			}
				
			List<Document> Contract = null;
			if (docTmp1.get("Contract") != null ) {
				Contract = docTmp1.getList("Contract", Document.class);
			}
			
			
			ObjectId IssuerId = docTmp1.get("_id", ObjectId.class);
			String IssuerIdString = IssuerId.toString();
		//	int dem_contract = Contract.size();
	
			int SLPHanh = 0;
			int SLCLai = 0;
			String MST  = "";
			String Ten = "";
			String SoHD = "";
			int SoLuongHD = 0;
			String NgayTao = "";
			String AGENT = "";
			String GhiChu = "";
					
				for(Document doc1: Contract) {
					 MST = doc1.getEmbedded(Arrays.asList("NMUA", "TaxCode"), "");
					 Ten = doc1.getEmbedded(Arrays.asList("NMUA", "Name"), "");
					 SoHD = doc1.getEmbedded(Arrays.asList("Contract", "SHDon"), "");
					 SoLuongHD = doc1.getEmbedded(Arrays.asList("Contract", "SLHDon"), 0);
					 GhiChu = doc1.getEmbedded(Arrays.asList("Contract", "GhiChu"), "");
					 NgayTao = commons.convertLocalDateTimeToString(
								commons.convertDateToLocalDateTime(
										doc.getEmbedded(Arrays.asList("InfoCreated", "CreateDate"), Date.class)),
								"dd/MM/yyyy");
					 AGENT = "CÔNG TY TNHH SES GROUP";
							
					
					Document findIssuer = new Document("_id", IssuerId).append("IsDelete", new Document("$ne", true));
						pipeline2 = new ArrayList<Document>();
						pipeline2.add(new Document("$match", findIssuer));
						pipeline2.add(new Document("$lookup", new Document("from", "DMMauSoKyHieu")
								.append("pipeline", Arrays.asList(
								new Document("$match",
										new Document("IsDelete", new Document("$ne", true))
										.append("IsActive", true)
										.append("IssuerId", IssuerIdString)										
										))).append("as", "DMMauSoKyHieu")));
						
						Iterable<Document> cursor2 = null;
						Iterator<Document> iter2 = null;
						cursor2 = mongoTemplate.getCollection("Issuer").aggregate(pipeline2);						
						iter2 = cursor2.iterator();
						if (iter2.hasNext()) {
							docTmp2 = iter2.next();
						}
						
						List<Document> DMMSKHieu = null;
						if (docTmp2.get("DMMauSoKyHieu") != null ) {
							DMMSKHieu = docTmp2.getList("DMMauSoKyHieu", Document.class);
						}
						
						ArrayList<HashMap<String, Object>> MauSo = new ArrayList<HashMap<String, Object>>();
				//		int check = DMMSKHieu.size();
						int stt = 0;
						for(Document doc2: DMMSKHieu) {				
							stt+=1;
							int slph = 	doc2.get("SoLuong", 0);
							int slcl = doc2.get("ConLai", 0);		
							int dadung = slph - slcl;						
							int TiLe = 	(slcl / slph) * 100;
							
							hItem1 = new HashMap<String, Object>();		
							hItem1.put("STT", stt);
							hItem1.put("KHMSHDon", doc2.get("KHMSHDon", ""));
							hItem1.put("KHHDon", doc2.get("KHHDon", ""));
							hItem1.put("SoLuong", slph);
							hItem1.put("DaDung", dadung);
							hItem1.put("ConLai", slcl);
							hItem1.put("TiLe", TiLe);							
							MauSo.add(hItem1);
				
						}
					
				
						
					hItem = new HashMap<String, Object>();					
					hItem.put("TaxCode", MST);
					hItem.put("Name", Ten);
					hItem.put("SoHDong", SoHD);
					hItem.put("SLDKy", SoLuongHD);
					hItem.put("MauSo", MauSo);
					hItem.put("NgayTao",NgayTao);
					hItem.put("AGENT",AGENT);	
					hItem.put("GhiChu",GhiChu);	
					rowsReturn.add(hItem);		
		
			}
		}
	}			
		responseStatus = new MspResponseStatus(0, "SUCCESS");
		rsp.setResponseStatus(responseStatus);
		
		HashMap<String, Object> mapDataR = new HashMap<String, Object>();
		mapDataR.put("rows", rowsReturn);
		rsp.setObjData(mapDataR);
		return rsp;
	}



}
