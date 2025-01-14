package vn.sesgroup.hddt.user.impl;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.URL;
import java.nio.charset.Charset;
import java.security.cert.X509Certificate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
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
import org.w3c.dom.Element;

import com.mongodb.client.model.FindOneAndUpdateOptions;

import vn.sesgroup.hddt.user.dao.AbstractDAO;
import vn.sesgroup.hddt.user.dao.ScheduledTasksDAO;
import vn.sesgroup.hddt.utility.SystemParams;

@Repository
@Transactional
public class ScheduledTasksImpl extends AbstractDAO implements ScheduledTasksDAO{
	private static final Logger log = LogManager.getLogger(ScheduledTasksImpl.class);
	@Autowired MongoTemplate mongoTemplate;
	
	TrustManager[] trustAllCerts = new TrustManager[] { 
		new X509TrustManager() {
			public java.security.cert.X509Certificate[] getAcceptedIssuers() {
				return null;
			}
			public void checkClientTrusted(X509Certificate[] certs, String authType) {
			}
			public void checkServerTrusted(X509Certificate[] certs, String authType) {
			}
		} 
	};
	
	@Override
	public void getAccessTokenVISNAM() throws Exception {
		Iterable<Document> cursor = null;
		Iterator<Document> iter = null;
		Document docTmp = null;
		/*GET USER - PASS TO GET TOKEN*/
		cursor = mongoTemplate.getCollection("SystemAccessToken").find(
			new Document("key", "ACCESS_TOKEN_VISNAM")
		);
		iter = cursor.iterator();
		if(iter.hasNext()) {
			docTmp = iter.next();
		}else {
			return;
		}
		
		/*END - GET USER - PASS TO GET TOKEN*/
		
//		Install the all-trusting trust manager
		SSLContext sc = SSLContext.getInstance("SSL");
		sc.init(null, trustAllCerts, new java.security.SecureRandom());
		HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
      
//		Create all-trusting host name verifier
		HostnameVerifier allHostsValid = new HostnameVerifier() {
			public boolean verify(String hostname, SSLSession session) {
				return true;
			}
		};
//		Install the all-trusting host verifier
		HttpsURLConnection.setDefaultHostnameVerifier(allHostsValid);
		
		URL url = null;
		HttpsURLConnection conn = null;
		try {
//			url = new URL("https://ws.vin-hoadon.com/api/services/hddtws/Authentication/GetTokenXML");
			url = new URL(SystemParams.VISNAM_URL_GETTOKENXML);
	        conn = (HttpsURLConnection) url.openConnection();
			conn.setDoOutput(true); 
			conn.setRequestMethod("POST");
			conn.setRequestProperty("Content-Type", "application/xml");
			
			conn.setReadTimeout(60000);
			conn.setConnectTimeout(60000);
			conn.setRequestProperty("User-Agent", "Mozilla/5.0");
			
			String data = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?><GetTokenInput><UserName>" + docTmp.getEmbedded(Arrays.asList("InfoGetToken", "UserName"), "") + "</UserName><Password>" + docTmp.getEmbedded(Arrays.asList("InfoGetToken", "Password"), "") + "</Password></GetTokenInput>";
//			String data = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?><GetTokenInput><UserName>dvgp_sesgroup</UserName><Password>sesgroup@2021</Password></GetTokenInput>";
			OutputStream os = conn.getOutputStream();
			os.write(data.getBytes(Charset.forName("UTF-8")));
			os.flush();
			
			if(conn.getResponseCode() != 200) return;
			
			BufferedReader br = new BufferedReader(new InputStreamReader((conn.getInputStream())));
			StringBuilder sbReceive = new StringBuilder();
			String output;
			while ((output = br.readLine()) != null) {
				sbReceive.append(output);
			}
			
			/*READ DATA XML*/
			org.w3c.dom.Document doc = commons.stringToDocument(sbReceive.toString());
			XPath xPath = XPathFactory.newInstance().newXPath();
			Element elemContent = (Element) xPath.evaluate("/GetTokenOutput/Result/Access_Token", doc, XPathConstants.NODE);
			
			String accessToken = commons.getTextFromNodeXML(elemContent);
			if(!"".equals(accessToken)) {
				SystemParams.VISNAM_ACCESSTOKEN = accessToken;
				
				/*UPDATE ACCESS TOKEN DB*/
				mongoTemplate.getCollection("SystemAccessToken").findOneAndUpdate(
					new Document("key", "ACCESS_TOKEN_VISNAM"),
					new Document("$set", 
						new Document("AccessToken", accessToken)
						.append("AccessTokenLastTimeUpdate", LocalDateTime.now())
					),
					new FindOneAndUpdateOptions().upsert(false)
				);
				/*END - UPDATE ACCESS TOKEN DB*/
			}
			/*END - READ DATA XML*/

			
		}catch(Exception e) {
			
		}finally {
			try {conn.disconnect();}catch(Exception e) {}
		}
		
	}
	
/*
db.getCollection('EInvoice').aggregate([
    {$match: {
            IsDelete: {$ne: true}, EInvoiceStatus: 'PENDING', SignStatusCode: 'PROCESSING'
        }
    },
    {$sort: {
        'EInvoiceDetail.TTChung.MauSoHD': 1, 'EInvoiceDetail.TTChung.SHDon': 1
        }
    },
    {$group: {
            _id: '$EInvoiceDetail.TTChung.MauSoHD',
            Detail: {$first: '$$ROOT'}
        }
    }
])
 * */
	
	@Override
	public void callSignEInvoiceToTCTN() throws Exception {
		Document docTmp = null;
		Iterable<Document> cursor = null;
		Iterator<Document> iter = null;
		ObjectId objectId = null;
		
		Document docFind = new Document("IsDelete", new Document("$ne", true))
				.append("EInvoiceStatus", "PENDING").append("SignStatusCode", "PROCESSING");
		
		List<Document> pipeline = new ArrayList<Document>();
		pipeline.add(new Document("$match", docFind));
		pipeline.add(
			new Document("$sort", 
				new Document("EInvoiceDetail.TTChung.MauSoHD", 1).append("EInvoiceDetail.TTChung.SHDon", 1)
			)
		);
		pipeline.add(
			new Document("$group", 
				new Document("_id", "$EInvoiceDetail.TTChung.MauSoHD")
				.append("Detail", new Document("$first", "$$ROOT"))
			)
		);
		
		cursor = mongoTemplate.getCollection("EInvoice").aggregate(pipeline).allowDiskUse(true);
		iter = cursor.iterator();
		while(iter.hasNext()) {
			docTmp = iter.next();
			
			System.out.println(docTmp);
		}
		System.out.println("*******");
		
	}

	
}
