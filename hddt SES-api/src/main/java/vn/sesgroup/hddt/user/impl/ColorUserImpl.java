package vn.sesgroup.hddt.user.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
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
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;

import vn.sesgroup.hddt.configuration.ConfigConnectMongo;
import vn.sesgroup.hddt.user.dao.AbstractDAO;
import vn.sesgroup.hddt.user.dao.ColorUserDAO;
import vn.sesgroup.hddt.user.service.JPUtils;
import vn.sesgroup.hddt.user.service.TCTNService;
import vn.sesgroup.hddt.utility.Constants;

@Repository
@Transactional
public class ColorUserImpl extends AbstractDAO implements ColorUserDAO {
	private static final Logger log = LogManager.getLogger(ColorUserImpl.class);
	@Autowired
	TCTNService tctnService;
	@Autowired
	JPUtils jpUtils;
	Document docUpsert = null;
	@Autowired ConfigConnectMongo cfg;
	@Override
	public MsgRsp list(JSONRoot jsonRoot) throws Exception {
		Msg msg = jsonRoot.getMsg();
		MsgHeader header = msg.getMsgHeader();
		MsgPage page = msg.getMsgPage();
		page.setSize(100);	

		MsgRsp rsp = new MsgRsp(header);
		MspResponseStatus responseStatus = null;

		ObjectId objectId = null;
		Document docTmp = null;
	
		List<Document> pipeline = new ArrayList<Document>();

		Document docMatch = new Document("IsDelete", new Document("$ne", true)).append("Status", new Document("$ne", false));
				
		pipeline = new ArrayList<Document>();
		pipeline.add(new Document("$match", docMatch));
		pipeline.addAll(createFacetForSearchNotSort(page));

		
		MongoClient mongoClient = cfg.mongoClient();
		MongoDatabase db = mongoClient.getDatabase(cfg.dbName);
		MongoCollection<Document> collection = db.getCollection("DMColor");
		docTmp =   collection.aggregate(pipeline).allowDiskUse(true).iterator().next();		
		mongoClient.close();
		
		rsp = new MsgRsp(header);
		responseStatus = null;
		
		if (null == docTmp) {
			responseStatus = new MspResponseStatus(9999, Constants.MAP_ERROR.get(9999));
			rsp.setResponseStatus(responseStatus);
			return rsp;
		}
		
		page.setTotalRows(docTmp.getInteger("total", 0));
		rsp.setMsgPage(page);

		ArrayList<HashMap<String, Object>> rowsReturn = new ArrayList<HashMap<String, Object>>();
		
		List<Document> rows = null;
		
		if(docTmp!=null) {
		if (docTmp.get("data") != null && docTmp.get("data") instanceof List) {
			rows = docTmp.getList("data", Document.class);
		}


		HashMap<String, Object> hItem = null;
		if (null != rows) {
			for (Document doc : rows) {
				objectId = (ObjectId) doc.get("_id");
				hItem = new HashMap<String, Object>();
				hItem.put("_id", objectId.toString());
				hItem.put("Name", doc.get("Name"));
				hItem.put("Color", doc.get("Color"));
				hItem.put("LBT", doc.get("LBT"));
				hItem.put("LBTCode", doc.get("LBTCode"));

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
