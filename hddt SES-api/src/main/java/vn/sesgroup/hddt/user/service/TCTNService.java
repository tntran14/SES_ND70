package vn.sesgroup.hddt.user.service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.URL;
import java.nio.charset.Charset;
import java.security.cert.X509Certificate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import vn.sesgroup.hddt.user.dao.ScheduledTasksDAO;
import vn.sesgroup.hddt.utility.Commons;
import vn.sesgroup.hddt.utility.Constants;
import vn.sesgroup.hddt.utility.SystemParams;

@Service
public class TCTNService {
	private static final Logger log = LogManager.getLogger(TCTNService.class);
	Commons commons = new Commons();
	@Autowired private ScheduledTasksDAO dao;
	DateTimeFormatter format_time = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
	LocalDateTime time_dem  = LocalDateTime.now();
	String time = time_dem.format(format_time);
	
	
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
	

	
	public Document callTiepNhanThongDiep(String MLTDiep, String MTDiep, String MSTKHang, String SLuong, Document docDLieu) throws Exception{
		Document r = null;
		
			System.out.println(time +" "+"call callTiepNhanThongDiep "+MTDiep);
		
		
		
		Element elem01 = docDLieu.getDocumentElement();
		
		/*TAO XML THONG DIEP GUI DI*/
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        DocumentBuilder db = dbf.newDocumentBuilder();
        
        Document doc = db.newDocument();
		doc.setXmlStandalone(true);
		
		Element root = doc.createElement("TDiep");
		doc.appendChild(root);
		
		Element elementContent = doc.createElement("TTChung");
		
		if(MLTDiep.equals("100")) {
			elementContent.appendChild(commons.createElementWithValue(doc, "PBan", Constants.TDiep_TTChung_TCTN_VISNAM.PBan1));
		}else {
			elementContent.appendChild(commons.createElementWithValue(doc, "PBan", Constants.TDiep_TTChung_TCTN_VISNAM.PBan));
		}

		elementContent.appendChild(commons.createElementWithValue(doc, "MNGui", SystemParams.MSTTCGP));
		elementContent.appendChild(commons.createElementWithValue(doc, "MNNhan", SystemParams.MSTDVTN));
		elementContent.appendChild(commons.createElementWithValue(doc, "MLTDiep", MLTDiep));
		elementContent.appendChild(commons.createElementWithValue(doc, "MTDiep", MTDiep));
		elementContent.appendChild(commons.createElementWithValue(doc, "MTDTChieu", ""));
		elementContent.appendChild(commons.createElementWithValue(doc, "MST", MSTKHang));		//MA SO THUE NGUOI NOP THUE
		elementContent.appendChild(commons.createElementWithValue(doc, "SLuong", SLuong));		
		root.appendChild(elementContent);
		
		elementContent = doc.createElement("DLieu");
		Node copiedRoot = doc.importNode(elem01, true);
		elementContent.appendChild(copiedRoot);
		root.appendChild(elementContent);
		
		String data = commons.docW3cToString(doc);
        
		SSLContext sc = SSLContext.getInstance("SSL");
        sc.init(null, trustAllCerts, new java.security.SecureRandom());
        HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
		
        HostnameVerifier allHostsValid = new HostnameVerifier() {
        	public boolean verify(String hostname, SSLSession session) {
        		return true;
        	}
        };
		
        HttpsURLConnection.setDefaultHostnameVerifier(allHostsValid);
        System.out.println(time +" "+"Lay dl hoa don bat dau callTiepNhanThongDiep  "+MTDiep);
        URL url = new URL(SystemParams.VISNAM_URL_TIEPNHANTHONGDIEP);
        HttpsURLConnection conn = null;
        try {
        	conn = (HttpsURLConnection) url.openConnection();
     		conn.setDoOutput(true); 
     		conn.setDoInput(true);
     		conn.setRequestProperty("charset", "utf-8");
     		conn.setRequestMethod("POST");
     		conn.setRequestProperty("Content-Type", "application/xml");
     		conn.setRequestProperty("Authorization", "Bearer " + SystemParams.VISNAM_ACCESSTOKEN);
     		
     		conn.setReadTimeout(60000);
     		conn.setConnectTimeout(60000);
     		conn.setRequestProperty("User-Agent", "Mozilla/5.0");
     		
     		OutputStream os = conn.getOutputStream();
    		os.write(data.getBytes(Charset.forName("UTF-8")));
    		os.flush();
    		
    		if(conn.getResponseCode() != 200) {
    			dao.getAccessTokenVISNAM();  	
    			return null;
    		}
    	      System.out.println(time +" "+"callTiepNhanThongDiep thanh cong "+MTDiep);
    		
 
    			BufferedReader br = new BufferedReader(new InputStreamReader((conn.getInputStream())));
        		StringBuilder sbReceive = new StringBuilder();
        		String output;
        		while ((output = br.readLine()) != null) {
        			sbReceive.append(output);
        		}
        		/*READ DATA XML*/
        		
    			r = commons.stringToDocument(sbReceive.toString());

    		
        }catch(Exception ex) {
        	log.error(" >>>>> An exception occurred!", ex);
        	  System.out.println(time +" "+"callTiepNhanThongDiep that bai"+" "+MTDiep);
        }finally {
        	try {conn.disconnect();}catch(Exception e) {}
        }
		return r;
	}
	
	public Document callTraCuuThongDiep(String MTDiep) throws Exception{
		Document r = null;
		System.out.println(time +" "+"call callTraCuuThongDiep"+" "+MTDiep);
		/*TAO XML THONG DIEP GUI DI*/
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        DocumentBuilder db = dbf.newDocumentBuilder();
        
        Document doc = db.newDocument();
		doc.setXmlStandalone(true);
		doc.appendChild(commons.createElementWithValue(doc, "MTDiep", MTDiep));
		
		String data = commons.docW3cToString(doc);
        
		SSLContext sc = SSLContext.getInstance("SSL");
        sc.init(null, trustAllCerts, new java.security.SecureRandom());
        HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
		
        HostnameVerifier allHostsValid = new HostnameVerifier() {
        	public boolean verify(String hostname, SSLSession session) {
        		return true;
        	}
        };
		
        HttpsURLConnection.setDefaultHostnameVerifier(allHostsValid);
        
        URL url = new URL(SystemParams.VISNAM_URL_TRACUUTHONGDIEP);
        HttpsURLConnection conn = null;
        try {
        	conn = (HttpsURLConnection) url.openConnection();
     		conn.setDoOutput(true); 
     		conn.setDoInput(true);
     		conn.setRequestProperty("charset", "utf-8");
     		conn.setRequestMethod("POST");
     		conn.setRequestProperty("Content-Type", "application/xml");
     		conn.setRequestProperty("Authorization", "Bearer " + SystemParams.VISNAM_ACCESSTOKEN);
     		
     		conn.setReadTimeout(60000);
     		conn.setConnectTimeout(60000);
     		conn.setRequestProperty("User-Agent", "Mozilla/5.0");
     		
     		OutputStream os = conn.getOutputStream();
    		os.write(data.getBytes(Charset.forName("UTF-8")));
    		os.flush();
    		
    	
    		if(conn.getResponseCode() != 200) {
    	          dao.getAccessTokenVISNAM();    
    	          return null;
    	        }
    		System.out.println(time +" "+"call callTraCuuThongDiep THANH CONG"+" "+MTDiep);
    		BufferedReader br = new BufferedReader(new InputStreamReader((conn.getInputStream())));
    		StringBuilder sbReceive = new StringBuilder();
    		String output;
    		while ((output = br.readLine()) != null) {
    			sbReceive.append(output);
    		}

    		
    		r = commons.stringToDocument(sbReceive.toString()); 
			
        }catch(Exception ex) {
        	log.error(" >>>>> An exception occurred!", ex);
        	System.out.println(time +" "+"call callTraCuuThongDiep THAT BAI"+" "+MTDiep);
        }finally {
        	try {conn.disconnect();}catch(Exception e) {}
        }
		return r;
	}
	
	
	
	//GET STRING XML ADMIN
	public String callTraCuuThongDiepString(String MTDiep) throws Exception{
		String r = null;
		
		/*TAO XML THONG DIEP GUI DI*/
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        DocumentBuilder db = dbf.newDocumentBuilder();
        
        Document doc = db.newDocument();
		doc.setXmlStandalone(true);
		doc.appendChild(commons.createElementWithValue(doc, "MTDiep", MTDiep));
		
		String data = commons.docW3cToString(doc);
        
		SSLContext sc = SSLContext.getInstance("SSL");
        sc.init(null, trustAllCerts, new java.security.SecureRandom());
        HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
		
        HostnameVerifier allHostsValid = new HostnameVerifier() {
        	public boolean verify(String hostname, SSLSession session) {
        		return true;
        	}
        };
		
        HttpsURLConnection.setDefaultHostnameVerifier(allHostsValid);
        
        URL url = new URL(SystemParams.VISNAM_URL_TRACUUTHONGDIEP);
        HttpsURLConnection conn = null;
        try {
        	conn = (HttpsURLConnection) url.openConnection();
     		conn.setDoOutput(true); 
     		conn.setDoInput(true);
     		conn.setRequestProperty("charset", "utf-8");
     		conn.setRequestMethod("POST");
     		conn.setRequestProperty("Content-Type", "application/xml");
     		conn.setRequestProperty("Authorization", "Bearer " + SystemParams.VISNAM_ACCESSTOKEN);
     		
     		conn.setReadTimeout(60000);
     		conn.setConnectTimeout(60000);
     		conn.setRequestProperty("User-Agent", "Mozilla/5.0");
     		
     		OutputStream os = conn.getOutputStream();
    		os.write(data.getBytes(Charset.forName("UTF-8")));
    		os.flush();
    		
    	
    		if(conn.getResponseCode() != 200) {
    	          dao.getAccessTokenVISNAM();    
    	          return null;
    	        }
    		BufferedReader br = new BufferedReader(new InputStreamReader((conn.getInputStream())));
    		StringBuilder sbReceive = new StringBuilder();
    		String output;
    		while ((output = br.readLine()) != null) {
    			sbReceive.append(output);
    		}

    		/*READ DATA XML*/
			 r = sbReceive.toString(); 
    		

        }catch(Exception ex) {
        	log.error(" >>>>> An exception occurred!", ex);
        }finally {
        	try {conn.disconnect();}catch(Exception e) {}
        }
		return r;
	}
	
	
	/* MAY TINH TIEN */
	
	public Document callTiepNhanListThongDiep(String MLTDiep, String MTDiep, String MSTKHang, String SLuong, Document docDLieu) throws Exception{
		Document r = null;
		
		String data = commons.docW3cToString(docDLieu);
		
		SSLContext sc = SSLContext.getInstance("SSL");
        sc.init(null, trustAllCerts, new java.security.SecureRandom());
        HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
		
        HostnameVerifier allHostsValid = new HostnameVerifier() {
        	public boolean verify(String hostname, SSLSession session) {
        		return true;
        	}
        };
		
        HttpsURLConnection.setDefaultHostnameVerifier(allHostsValid);
        URL url = new URL(SystemParams.VISNAM_URL_TIEPNHANTHONGDIEP);
        HttpsURLConnection conn = null;
        try {
        	conn = (HttpsURLConnection) url.openConnection();
     		conn.setDoOutput(true); 
     		conn.setDoInput(true);
     		conn.setRequestProperty("charset", "utf-8");
     		conn.setRequestMethod("POST");
     		conn.setRequestProperty("Content-Type", "application/xml");
     		conn.setRequestProperty("Authorization", "Bearer " + SystemParams.VISNAM_ACCESSTOKEN);
     		
     		conn.setReadTimeout(60000);
     		conn.setConnectTimeout(60000);
     		conn.setRequestProperty("User-Agent", "Mozilla/5.0");
     		
     		OutputStream os = conn.getOutputStream();
    		os.write(data.getBytes(Charset.forName("UTF-8")));
    		os.flush();
    		
    		if(conn.getResponseCode() != 200) {
    			dao.getAccessTokenVISNAM();  	
    			return null;
    		}
    		
    			BufferedReader br = new BufferedReader(new InputStreamReader((conn.getInputStream())));
        		StringBuilder sbReceive = new StringBuilder();
        		String output;
        		while ((output = br.readLine()) != null) {
        			sbReceive.append(output);
        		}
        		/*READ DATA XML*/
        		
    			r = commons.stringToDocument(sbReceive.toString());

    		
        }catch(Exception ex) {
        	log.error(" >>>>> An exception occurred!", ex);
        	  System.out.println(time +" "+"callTiepNhanThongDiep that bai"+" "+MTDiep);
        }finally {
        	try {conn.disconnect();}catch(Exception e) {}
        }
		return r;
	}

	
	
	public Document callTiepNhanThongDiepMTT(Document docDLieu) throws Exception{
				
		Document r = null;
		String data = commons.docW3cToString(docDLieu);
        
		SSLContext sc = SSLContext.getInstance("SSL");
        sc.init(null, trustAllCerts, new java.security.SecureRandom());
        HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
		
        HostnameVerifier allHostsValid = new HostnameVerifier() {
        	public boolean verify(String hostname, SSLSession session) {
        		return true;
        	}
        };
		
        HttpsURLConnection.setDefaultHostnameVerifier(allHostsValid);
        URL url = new URL(SystemParams.VISNAM_URL_TIEPNHANTHONGDIEP);
        HttpsURLConnection conn = null;
        try {
        	conn = (HttpsURLConnection) url.openConnection();
     		conn.setDoOutput(true); 
     		conn.setDoInput(true);
     		conn.setRequestProperty("charset", "utf-8");
     		conn.setRequestMethod("POST");
     		conn.setRequestProperty("Content-Type", "application/xml");
     		conn.setRequestProperty("Authorization", "Bearer " + SystemParams.VISNAM_ACCESSTOKEN);
     		
     		conn.setReadTimeout(60000);
     		conn.setConnectTimeout(60000);
     		conn.setRequestProperty("User-Agent", "Mozilla/5.0");
     		
     		OutputStream os = conn.getOutputStream();
    		os.write(data.getBytes(Charset.forName("UTF-8")));
    		os.flush();
    		
    		if(conn.getResponseCode() != 200) {
    			dao.getAccessTokenVISNAM();  	
    			return null;
    		}    		
 
    			BufferedReader br = new BufferedReader(new InputStreamReader((conn.getInputStream())));
        		StringBuilder sbReceive = new StringBuilder();
        		String output;
        		while ((output = br.readLine()) != null) {
        			sbReceive.append(output);
        		}
        		/*READ DATA XML*/
        		
    			r = commons.stringToDocument(sbReceive.toString());

    		
        }catch(Exception ex) {
        	log.error(" >>>>> An exception occurred!", ex);
        }finally {
        	try {conn.disconnect();}catch(Exception e) {}
        }
		return r;
	}
	
}
