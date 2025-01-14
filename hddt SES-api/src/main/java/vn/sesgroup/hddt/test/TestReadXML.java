package vn.sesgroup.hddt.test;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.URL;
import java.nio.charset.Charset;
import java.security.cert.X509Certificate;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import vn.sesgroup.hddt.utility.Commons;
import vn.sesgroup.hddt.utility.Constants;

public class TestReadXML {
	public static void main(String[] args) throws Exception{
		Commons commons = new Commons();
		String path = "C:\\hddt-ses\\server\\tkhai\\0106323762";
		String fileName = "61c2a660436cda025bef74f5_test.xml";
		
		File f = new File(path, fileName);
		org.w3c.dom.Document doc = commons.fileToDocument(f);
		XPath xPath = XPathFactory.newInstance().newXPath();
		Node nodeKetQuaTraCuu = (Node) xPath.evaluate("/KetQuaTraCuu", doc, XPathConstants.NODE);
		
		Node nodeTmp = (Node) xPath.evaluate("DuLieu/TDiep[last()]", nodeKetQuaTraCuu, XPathConstants.NODE);
		System.out.println(nodeTmp == null);
//		System.out.println(commons.getTextFromNodeXML((Element) xPath.evaluate("TTChung/MLTDiep", nodeTmp, XPathConstants.NODE)));
		
		String SOLUTION_TAXCODE = "0315382923";
		String MNNhan = "0401486901";
		String MTDiep = SOLUTION_TAXCODE + commons.csRandomAlphaNumbericString(46 - SOLUTION_TAXCODE.length()).toUpperCase();
		System.out.println(MTDiep);
	}
	
	public static void main02(String[] args) throws Exception{
		Commons commons = new Commons();
		String path = "C:\\";
		String fileName = "xml-sign.xml";
		
		File f = new File(path, fileName);
		org.w3c.dom.Document doc = commons.fileToDocument(f);
		XPath xPath = XPathFactory.newInstance().newXPath();
		Node nodeDLHDon = (Node) xPath.evaluate("/HDon/DLHDon[@Id='data']", doc, XPathConstants.NODE);
		Node nodeTmp = null;
		nodeTmp = (Node) xPath.evaluate("TTChung", nodeDLHDon, XPathConstants.NODE);
		
		Element elementSub = (Element) xPath.evaluate("SHDon", nodeTmp, XPathConstants.NODE);
		System.out.println(elementSub.getTextContent());
		
		String keySystem = "";
		String key = "";
		NodeList nodeList = (NodeList) xPath.evaluate("TTKhac/TTin", nodeTmp, XPathConstants.NODESET);
		for(int i = 0; i < nodeList.getLength(); i++) {
			key = commons.getTextFromNodeXML((Element) xPath.evaluate("TTruong", nodeList.item(i), XPathConstants.NODE));
			if("SystemKey".equals(key)) {
				keySystem = commons.getTextFromNodeXML((Element) xPath.evaluate("DLieu", nodeList.item(i), XPathConstants.NODE));
				break;
			}
		}
		
		System.out.println(keySystem);
		
	}
	public static void main01(String[] args) throws Exception{
		Commons commons = new Commons();
		
		String SOLUTION_TAXCODE = "0315382923";
		String MNNhan = "0401486901";
		String MTDiep = SOLUTION_TAXCODE + commons.csRandomAlphaNumbericString(46 - SOLUTION_TAXCODE.length()).toUpperCase();
		
		String path = "C:\\hddt-ses\\web\\tmp-files";
		String fileName = "tmp.xml";
		
		path = "C:\\";
		fileName = "xml-sign.xml";
		
		Document doc01 = commons.fileToDocument(new File(path, fileName), true);
		Element elem01 = doc01.getDocumentElement();
//		System.out.println(commons.docW3cToString(doc01));
		
		/*TAO THONG DIEP GUI DI*/
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        DocumentBuilder db = dbf.newDocumentBuilder();
        
        Document doc = db.newDocument();
		doc.setXmlStandalone(true);
		
		Element root = doc.createElement("TDiep");
		doc.appendChild(root);
		
		Element elementContent = doc.createElement("TTChung");
		elementContent.appendChild(commons.createElementWithValue(doc, "PBan", Constants.TDiep_TTChung_TCTN_VISNAM.PBan));
		elementContent.appendChild(commons.createElementWithValue(doc, "MNGui", SOLUTION_TAXCODE));
		elementContent.appendChild(commons.createElementWithValue(doc, "MNNhan", MNNhan));
		elementContent.appendChild(commons.createElementWithValue(doc, "MLTDiep", "200"));
		elementContent.appendChild(commons.createElementWithValue(doc, "MTDiep", MTDiep));
		elementContent.appendChild(commons.createElementWithValue(doc, "MTDTChieu", ""));
		elementContent.appendChild(commons.createElementWithValue(doc, "MST", "0106323762"));
		elementContent.appendChild(commons.createElementWithValue(doc, "SLuong", "1"));		
		root.appendChild(elementContent);
		
		elementContent = doc.createElement("DLieu");
		Node copiedRoot = doc.importNode(elem01, true);
		elementContent.appendChild(copiedRoot);
		root.appendChild(elementContent);
		
		
		
		// Create a trust manager that does not validate certificate chains
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
        
//        Install the all-trusting trust manager
        SSLContext sc = SSLContext.getInstance("SSL");
        sc.init(null, trustAllCerts, new java.security.SecureRandom());
        HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
        
//      Create all-trusting host name verifier
        HostnameVerifier allHostsValid = new HostnameVerifier() {
        	public boolean verify(String hostname, SSLSession session) {
        		return true;
        	}
        };
		
        HttpsURLConnection.setDefaultHostnameVerifier(allHostsValid);
        
        URL url = new URL("https://demows.vin-hoadon.com/api/services/hddtws/TiepNhanThongDiep/TiepNhan");
        HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();
		conn.setDoOutput(true); 
		conn.setDoInput(true);
		conn.setRequestProperty("charset", "utf-8");
		conn.setRequestMethod("POST");
		conn.setRequestProperty("Content-Type", "application/xml");
		conn.setRequestProperty("Authorization", "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJodHRwOi8vc2NoZW1hcy54bWxzb2FwLm9yZy93cy8yMDA1LzA1L2lkZW50aXR5L2NsYWltcy9uYW1laWRlbnRpZmllciI6IjE5IiwiaHR0cDovL3NjaGVtYXMueG1sc29hcC5vcmcvd3MvMjAwNS8wNS9pZGVudGl0eS9jbGFpbXMvbmFtZSI6InNlc2dyb3VwIiwiQXNwTmV0LklkZW50aXR5LlNlY3VyaXR5U3RhbXAiOiJMTUlRVTdKWDZDV0pRRkFYVFEzS0syQ0pZS0I1M0tZRyIsInN1YiI6IjE5IiwianRpIjoiNjkxYjgxZjEtZDliZC00NTM2LWJmOTktYzAzOTA5Yzc5MTRlIiwiaWF0IjoxNjM5MTA0NzMwLCJ0b2tlbl92YWxpZGl0eV9rZXkiOiIyNmIwMGY2Zi0xYWMwLTQxMmYtYmU1OC1hZDMxNThmM2ZmMTQiLCJ1c2VyX2lkZW50aWZpZXIiOiIxOSIsIm5iZiI6MTYzOTEwNDczMCwiZXhwIjoxNjM5MTkxMTMwLCJpc3MiOiJWUyIsImF1ZCI6IlZTIn0.YI7V8uOjPzFdDDXAHswQqI7n2geKRk0N0BWl9lq9RKc");
		
		conn.setReadTimeout(60000);
		conn.setConnectTimeout(60000);
		conn.setRequestProperty("User-Agent", "Mozilla/5.0");
		
//		String data = "<TDiep><TTChung><PBan>2.0.0</PBan><MNGui>V738373933</MNGui><MNNhan>V0401486901</MNNhan><MLTDiep>203</MLTDiep><MTDiep>V0401486901150D1B8E3D45451C91854D7ê51ACDA101</MTDiep><MTDTChieu/><MST>0106323762</MST><SLuong>1</SLuong></TTChung><DLieu><HDon><DLHDon Id=\"SignData\"><TTChung><PBan>2.0.0</PBan><THDon>HÓA ĐƠN GIÁ TRỊ GIA TĂNG</THDon><KHMSHDon>1</KHMSHDon><KHHDon>K21TCC</KHHDon><SHDon>6</SHDon><MHSo /><NLap>2021-11-20</NLap><SBKe /><NBKe /><DVTTe>VND</DVTTe><TGia>1</TGia><HTTToan>TM/CK</HTTToan><MSTTCGP /><MSTDVNUNLHDon /><TDVNUNLHDon /><DCDVNUNLHDon /><TTKhac><TTin><TTruong>HoaDon_Loai</TTruong><KDLieu>string</KDLieu><DLieu>1</DLieu></TTin><TTin><TTruong>QuanLy_SoBaoMat</TTruong><KDLieu>string</KDLieu><DLieu>2LCLFYCJ4CFJ</DLieu></TTin></TTKhac></TTChung><NDHDon><NBan><Ten>Mã số Thuế Test 97</Ten><MST>0106323762</MST><DChi>Ba Đình</DChi><SDThoai>0325467895</SDThoai><DCTDTu>hoabt01091994@gmail.com</DCTDTu><STKNHang /><TNHang /><Fax /><Website /><TTKhac><TTin><TTruong>DNBan_NguoiDaiDien</TTruong><KDLieu>string</KDLieu><DLieu>Trần Hớn</DLieu></TTin></TTKhac></NBan><NMua><Ten /><MST /><DChi /><MKHang /><SDThoai /><DCTDTu /><HVTNMHang>Khách lẻ jjj</HVTNMHang><STKNHang /><TNHang /></NMua><DSHHDVu><HHDVu><TChat>1</TChat><STT>1</STT><THHDVu>Hàng hóa 01</THHDVu><DVTinh>cái</DVTinh><SLuong>100</SLuong><DGia>1000000</DGia><TLCKhau>0</TLCKhau><ThTien>100000000</ThTien><TSuat>10%</TSuat><TTKhac><TTin><TTruong>TongTien_CoThue</TTruong><KDLieu>string</KDLieu><DLieu>110000000.00</DLieu></TTin><TTin><TTruong>TongTien_Thue</TTruong><KDLieu>string</KDLieu><DLieu>10000000.00000</DLieu></TTin></TTKhac></HHDVu></DSHHDVu><TToan><THTTLTSuat><LTSuat><TSuat>10%</TSuat><ThTien>100000000</ThTien><TThue>10000000</TThue></LTSuat></THTTLTSuat><TgTCThue>100000000</TgTCThue><TgTThue>10000000</TgTThue><DSLPhi><LPhi><TLPhi /><TPhi /></LPhi></DSLPhi><TTCKTMai /><TgTTTBSo>110000000</TgTTTBSo><TgTTTBChu>Một trăm mười triệu đồng</TgTTTBChu><TTKhac><TTin><TTruong>TongHop_TongTien_KhoanKhac</TTruong><KDLieu>string</KDLieu><DLieu>0.00000</DLieu></TTin></TTKhac></TToan></NDHDon><TTKhac><TTin><TTruong>QuanLy_ChuyenDoi</TTruong><KDLieu>string</KDLieu><DLieu>False</DLieu></TTin><TTin><TTruong>DNBan_NgayKy</TTruong><KDLieu>string</KDLieu><DLieu>2021-11-20</DLieu></TTin><TTin><TTruong>IdHoaDon</TTruong><KDLieu>string</KDLieu><DLieu>403428</DLieu></TTin><TTin><TTruong>FinalPage</TTruong><KDLieu>string</KDLieu><DLieu>1</DLieu></TTin><TTin><TTruong>IsShowArea</TTruong><KDLieu>string</KDLieu><DLieu>0</DLieu></TTin><TTin><TTruong>EmptyLineNumber</TTruong><KDLieu>string</KDLieu><DLieu>9</DLieu></TTin></TTKhac></DLHDon><DLQRCode>000201010212261401100106323762520400005303VND54091100000005802VN62870032CA787448099E41438FA6692E39F8A96D020110306K21TCC040700000060508202111200609110000000633282267FE15586360182CE685EBCC4F28B</DLQRCode><MCCQT /><DSCKS><NBan><Signature Id=\"proid\" xmlns=\"http://www.w3.org/2000/09/xmldsig#\"><SignedInfo><CanonicalizationMethod Algorithm=\"http://www.w3.org/TR/2001/REC-xml-c14n-20010315\" /><SignatureMethod Algorithm=\"http://www.w3.org/2000/09/xmldsig#rsa-sha1\" /><Reference URI=\"#SignData\"><Transforms><Transform Algorithm=\"http://www.w3.org/2000/09/xmldsig#enveloped-signature\" /></Transforms><DigestMethod Algorithm=\"http://www.w3.org/2000/09/xmldsig#sha1\" /><DigestValue>FoZVZbSEzDPvD3iM0UvwB1rwtRk=</DigestValue></Reference><Reference URI=\"#SigningTime\"><DigestMethod Algorithm=\"http://www.w3.org/2000/09/xmldsig#sha1\" /><DigestValue>7NbB23g7xfFOZp+/wF4/bl0fivM=</DigestValue></Reference></SignedInfo><SignatureValue>OKCd44hXRcltbP/l2x8qloCg6QlnjxkZzCVyat/thwOKrl0H28HOCpZPTpqSKibyBVcUYjmYHKqFcg9r6O+gVvF5EriaUqbXUXGB7OuXTE9RooMXXzkyfVbQU8paBAPrfEtN5voPwTlriS9h1EHM3Eh7jiHZtokDIWyX73zXpASWwEvDhNrnL1ofIm7FKmvC8+ICvZmpWRN1U53dktB7bZt5gUWzRpfhuMMrDR0fKUg0g9/lgYSbwOCCuts6XcDGXjqpx/BAdzOsgnmz+pFDepXDQLKGXHaBJV9gYtSfeUBq99mkvs3fTSVrz5lLvZ7w3eEjZ/erfIYs16LK/0XWxw==</SignatureValue><KeyInfo><KeyValue><RSAKeyValue><Modulus>k0i7dlmtzCMqQm/QoKbnPRK0c7im1CiIJpAk+xRnPOZE1afDvAb5qRCV2XJm9hQMjwuuwleKOJo8XJJ5PPg0hnQb9ZYOFtxZuEbV0KU98Nyn3zG1w5ScRues49WItz32s9JuO6h/dxXIHk7qaRoAmsb6RlWEznfMiZl27RWHuUk/uwPdsdeD2YvCl16lIeb+tbLX2vafTxKk2+M+EcOfn9AOzkVvs7EKlG5eUmIgrImBd7bWwmc88ZbMc//55OczoYTDIeADg0/4X0IANL9DjbewzJ3SdN0ikKzY69Od2/2QzMVtFTsbmgNpri6PnyULa+PlN/uB8zgr/eWris78SQ==</Modulus><Exponent>AQAB</Exponent></RSAKeyValue></KeyValue><X509Data><X509SubjectName>OID.0.9.2342.19200300.100.1.1=MST:0106323762, CN=MÃ SỐ THUẾ TEST 97</X509SubjectName><X509Certificate>MIIEdTCCA12gAwIBAgIQVAEBDz5oOWps6UZRr0TCgTANBgkqhkiG9w0BAQsFADBUMRIwEAYDVQQDDAlMQ1MtQ0EgRzExFTATBgNVBAoMDEwuQy5TIENPLkxURDEaMBgGA1UECAwRVFAuSOG7kyBDaMOtIE1pbmgxCzAJBgNVBAYTAlZOMB4XDTIxMTEwMzE3MDAwMFoXDTIyMTEwNDE2NTkwMFowQjEgMB4GA1UEAwwXTcODIFPhu5AgVEhV4bq+IFRFU1QgOTcxHjAcBgoJkiaJk/IsZAEBDA5NU1Q6MDEwNjMyMzc2MjCCASIwDQYJKoZIhvcNAQEBBQADggEPADCCAQoCggEBAJNIu3ZZrcwjKkJv0KCm5z0StHO4ptQoiCaQJPsUZzzmRNWnw7wG+akQldlyZvYUDI8LrsJXijiaPFySeTz4NIZ0G/WWDhbcWbhG1dClPfDcp98xtcOUnEbnrOPViLc99rPSbjuof3cVyB5O6mkaAJrG+kZVhM53zImZdu0Vh7lJP7sD3bHXg9mLwpdepSHm/rWy19r2n08SpNvjPhHDn5/QDs5Fb7OxCpRuXlJiIKyJgXe21sJnPPGWzHP/+eTnM6GEwyHgA4NP+F9CADS/Q423sMyd0nTdIpCs2OvTndv9kMzFbRU7G5oDaa4uj58lC2vj5Tf7gfM4K/3lq4rO/EkCAwEAAaOCAVMwggFPMAwGA1UdEwEB/wQCMAAwHwYDVR0jBBgwFoAUXYADrGJokVVOCpFA4VHLepzi14gwXgYIKwYBBQUHAQEEUjBQMCsGCCsGAQUFBzAChh9odHRwOi8vY3JsLmxjcy1jYS52bi9sY3MtY2EuY3J0MCEGCCsGAQUFBzABhhVodHRwOi8vb2NzcC5sY3MtY2Eudm4wGwYDVR0RBBQwEoEQZHV5cGNAdmlzbmFtLmNvbTBABgNVHSUEOTA3BggrBgEFBQcDAgYIKwYBBQUHAwQGCisGAQQBgjcKAwwGCisGAQQBgjcUAgIGCSqGSIb3LwEBBTAwBgNVHR8EKTAnMCWgI6Ahhh9odHRwOi8vY3JsLmxjcy1jYS52bi9sY3MtY2EuY3JsMB0GA1UdDgQWBBTt0dgvnsQ5PUE6RzJREZlXk6aDLTAOBgNVHQ8BAf8EBAMCBeAwDQYJKoZIhvcNAQELBQADggEBAItPk5ux4RT4O2BowpC0vfRZnDvxkt7G6HGfBC8cxLmVjqUGtR8+G6ScuR8J9OfZdF5naDnvUBqMqjcxwOiP+lZ2uBmoVe1diebRzqjZFttyDWQf0Hcjg0EOaSP89CB1Hk1PEs4sf8RWT7OA/97212tEPOkW3CiLFSOlIC8P6yPTuWyi0wIGoCJRSB/IcmD2djEv9/HkiuYSFBTuiaxaJ9WUrRCHuuaMFv3LBvtnCbog8pMOXK7qwaFlf35x6g2kPHdS72Jn0L7Q5V90iReJI2pyKyeeQ27gcViI/9IXatM95SbCCktIGhxk/vNGaC+1+uPmBM1auUDhcRmmJWNlkm8=</X509Certificate></X509Data></KeyInfo><Object Id=\"SigningTime\"><SignatureProperties Id=\"SigProps\" xmlns=\"\"><SignatureProperty Target=\"#proid\"><SigningTime xmlns=\"http://example.org/#signatureProperties\">2021-11-20T19:20:19Z</SigningTime></SignatureProperty></SignatureProperties></Object></Signature></NBan><NMua /><CQT /><CCKSKhac /></DSCKS></HDon></DLieu></TDiep>";
		String data = commons.docW3cToString(doc);
		
		System.out.println(data);
		
		OutputStream os = conn.getOutputStream();
		os.write(data.getBytes(Charset.forName("UTF-8")));
		os.flush();
		
//		System.out.println(conn.getResponseCode());
		
		BufferedReader br = new BufferedReader(new InputStreamReader((conn.getInputStream())));

		String output;
		System.out.println("Output from Server .... \n");
		while ((output = br.readLine()) != null) {
			System.out.println(output);
		}

		conn.disconnect();
		
//		System.out.println(commons.docW3cToString(doc));
		
	}
}
