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

import vn.sesgroup.hddt.dto.FileInfo;
import vn.sesgroup.hddt.user.dao.AbstractDAO;
import vn.sesgroup.hddt.user.dao.TaxCodeDAO;
import vn.sesgroup.hddt.user.service.JPUtils;

@Repository
@Transactional
public class TaxCodeImpl extends AbstractDAO implements TaxCodeDAO{
	private static final Logger log = LogManager.getLogger(TaxCodeImpl.class);
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
	public FileInfo getTaxCode(HashMap<String, String> mapInput) throws Exception {
		FileInfo fileInfo = new FileInfo();
		
		/*LAY THONG TIN DE IN HD*/
		String issuerId = null == mapInput.get("IssuerId")? "": mapInput.get("IssuerId");
		ObjectId objectId = null;
		objectId = new ObjectId(issuerId);
		
		List<Document> pipeline = null;

		Document docFind = null;
		docFind = new Document("_id", objectId).append("IsActive", true).append("IsDelete", new Document("$ne", true));
		pipeline = new ArrayList<Document>();
		pipeline.add(new Document("$match", docFind));	
		Document docTmp = null;
		Iterable<Document> cursor = mongoTemplate.getCollection("Issuer").aggregate(pipeline);
		Iterator<Document> iter = cursor.iterator();
		if(iter.hasNext()) {
			docTmp = iter.next();
		}
		
		String Taxcode = docTmp.getString("TaxCode");
				
		fileInfo.setTaxcode(Taxcode);		
		return fileInfo;
	}

}
