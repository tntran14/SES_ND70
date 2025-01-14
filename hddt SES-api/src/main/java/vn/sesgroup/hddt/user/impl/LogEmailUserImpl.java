package vn.sesgroup.hddt.user.impl;

import java.util.ArrayList;
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
import vn.sesgroup.hddt.user.dao.LogEmailUserDAO;
import vn.sesgroup.hddt.user.service.TCTNService;
import vn.sesgroup.hddt.utility.Constants;

@Repository
@Transactional
public class LogEmailUserImpl extends AbstractDAO implements LogEmailUserDAO{
	private static final Logger log = LogManager.getLogger(LogEmailUserImpl.class);
	@Autowired MongoTemplate mongoTemplate;
	@Autowired TCTNService tctnService;
	
	
	///
	
	@Override
	public MsgRsp list(JSONRoot jsonRoot) throws Exception {
			Msg msg = jsonRoot.getMsg();
			MsgHeader header = msg.getMsgHeader();
			MsgPage page = msg.getMsgPage();
			Object objData = msg.getObjData();
			
			
			MsgRsp rsp = new MsgRsp(header);
			MspResponseStatus responseStatus = null;
			
			ObjectId objectId = null;
			Document docTmp = null;
			Iterable<Document> cursor = null;
			Iterator<Document> iter = null;
			List<Document> pipeline = new ArrayList<Document>();
			

		
			Document docMatch = new Document("IsDelete",new Document("$ne", true))
					.append("IsActive", true);
			pipeline = new ArrayList<Document>();
			pipeline.add(new Document("$match", docMatch));
			pipeline.add(
					new Document("$sort", 
						new Document("_id", -1)
					)
				);
			pipeline.addAll(createFacetForSearchNotSort(page));
			cursor = mongoTemplate.getCollection("LogEmailUser").aggregate(pipeline).allowDiskUse(true);
			iter = cursor.iterator();
			if(iter.hasNext()) {
				docTmp = iter.next();
			}
			

			
			
			rsp = new MsgRsp(header);
			responseStatus = null;
			if(null == docTmp) {
				responseStatus = new MspResponseStatus(9999, Constants.MAP_ERROR.get(9999));
				rsp.setResponseStatus(responseStatus);
				return rsp;
			}
			
			page.setTotalRows(docTmp.getInteger("total", 0));
			rsp.setMsgPage(page);
			
			List<Document> rows = null;
			if(docTmp.get("data") != null && docTmp.get("data") instanceof List) {
				rows = docTmp.getList("data", Document.class);
			}
			
			ArrayList<HashMap<String, Object>> rowsReturn = new ArrayList<HashMap<String, Object>>();
			HashMap<String, Object> hItem = null;
			if(null != rows) {
				for(Document doc: rows) {
					Document docTmp1 = null;
					objectId = (ObjectId) doc.get("_id");					
					String IssuerId = doc.get("IssuerId", "");					
					ObjectId objectId_issu= new ObjectId(IssuerId);					
					
					Document findIssuer = new Document("_id", objectId_issu).append("IsDelete", new Document("$ne", true));
					Iterable<Document> cursor1 = mongoTemplate.getCollection("Issuer").find(findIssuer).limit(1);					
					Iterator<Document> iter1 = cursor1.iterator();
					if (iter1.hasNext()) {
						docTmp1 = iter1.next();
					}
					
					
					String NameIssuer = "";
					if(docTmp1!=null) {
						NameIssuer = docTmp1.get("Name", "");
					}
					
					hItem = new HashMap<String, Object>();
					hItem.put("_id", objectId.toString());
					hItem.put("Name", NameIssuer);
					hItem.put("Title", doc.get("Title"));
					hItem.put("Email", doc.get("Email"));
					hItem.put("EmailContent", doc.get("EmailContent"));
					
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
}
