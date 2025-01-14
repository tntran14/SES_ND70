package vn.sesgroup.hddt.utility;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.text.DecimalFormat;
import java.text.Normalizer;
import java.text.NumberFormat;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoField;
import java.time.temporal.TemporalAccessor;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.StringJoiner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collector;

import javax.servlet.http.HttpServletRequest;
import javax.xml.bind.DatatypeConverter;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.validator.routines.EmailValidator;
import org.apache.pdfbox.io.MemoryUsageSetting;
import org.apache.pdfbox.multipdf.PDFMergerUtility;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.FormulaEvaluator;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.fasterxml.jackson.databind.JsonNode;

import vn.sesgroup.hddt.dto.SignTypeInfo;

public class Commons {
	public String csRandomAlphaNumbericString(int numChars) {
		char buff[] = new char[numChars];
		SecureRandom srand = new SecureRandom();
		Random rand = new Random();
		for (int i = 0; i < numChars; i++) {
			if ((i % 10) == 0) {
				rand.setSeed(srand.nextLong()); // // 64 bits of random!
			}
			buff[i] = Constants.VALID_CHARACTERS[rand.nextInt(Constants.VALID_CHARACTERS.length)];
		}
		return new String(buff);
	}
	
	public String csRandomNumbericString(int numChars) {
		char buff[] = new char[numChars];
		SecureRandom srand = new SecureRandom();
		Random rand = new Random();
		for (int i = 0; i < numChars; i++) {
			if ((i % 10) == 0) {
				rand.setSeed(srand.nextLong()); // // 64 bits of random!
			}
			buff[i] = Constants.VALID_NUMBERS[rand.nextInt(Constants.VALID_NUMBERS.length)];
		}
		return new String(buff);
	}
	
	public int randomInt(int bound) {
		SecureRandom rn = new SecureRandom();
		return rn.nextInt(bound) + 1;
	}
	
	/*Base64*/
	public String encodeStringBase64(String input) {
		return DatatypeConverter.printBase64Binary(input.getBytes(StandardCharsets.UTF_8));
	}
	
	public String decodeBase64ToString(String stringBase64){
		stringBase64 = stringBase64.replaceAll(" ", "+");
		return new String(DatatypeConverter.parseBase64Binary(stringBase64), StandardCharsets.UTF_8);
	}
	/*End Base64*/
	
	public String encodeURIComponent(String val){
		try {
			return URLEncoder.encode(val, StandardCharsets.UTF_8.toString()).replaceAll("\\+", "%20");
		}catch(Exception e) {
			return val;
		}
		
	}
	
	public String decodeURIComponent(String val){
		try {
			return URLDecoder.decode(val, StandardCharsets.UTF_8.toString());
		}catch(Exception e) {
			return val;
		}
		
	}
	
	/*SHA*/
	public String generateSHA(String content, boolean isSHA256) {
		MessageDigest md = null;
		byte[] hash = null;
		try {
			if(isSHA256)
	    		md = MessageDigest.getInstance("SHA-256");
	    	else
	    		md = MessageDigest.getInstance("SHA-512");
			hash = md.digest(content.getBytes("UTF-8"));
		}catch(NoSuchAlgorithmException e) {
			return "";
		}catch(UnsupportedEncodingException e) {
			return "";
		}
		return convertToHex(hash);
	}
	
	private String convertToHex(byte[] raw) {
	    StringBuffer sb = new StringBuffer();
	    for (int i = 0; i < raw.length; i++) {
	        sb.append(Integer.toString((raw[i] & 0xff) + 0x100, 16).substring(1));
	    }
	    return sb.toString();
	}
//	System.err.println(sha.generateSHA("AAA_SUPERADMIN" + "admin@123", false).toUpperCase());
	/*END SHA*/
	
	public String getParameterFromRequest(HttpServletRequest request, String paramName) {
		return request.getParameter(paramName) == null? "": request.getParameter(paramName);
	}
	
	public String getTextJsonNode(JsonNode jsonNode) {
		try {
			return (jsonNode == null || jsonNode.isMissingNode() || jsonNode.isNull() /* || !jsonNode.isTextual() */)? "": jsonNode.asText();
		}catch(Exception e) {
			e.printStackTrace();
			return null;
		}
	}
	
	public int calcPageNo(int pageNo) {
		return pageNo < 1? 1: pageNo;
	}
	
	public int calcPageSize(int pageSize) {
		int size = pageSize > 100? 100: pageSize;
		if(size < 1) size = 1;
		return size;		
	}
	
	public String regexEscapeForMongoQuery(String input) {
		try {
			Pattern p = Pattern.compile("[\\*\\+\\?\\^\\${}\\(\\)|\\]\\[\\\\]");
			Matcher m = p.matcher(input);
			input = m.replaceAll("\\\\$0");
			return input;
		}catch(Exception e) {
			return input;
		}
	}
	
	/*DATE TIME*/
	public LocalDate convertDateToLocalDate(Date date) {
		return date.toInstant().atZone(ZoneId.of("UTC")).toLocalDate();
	}
	public LocalDateTime convertDateToLocalDateTime(Date date) {
		return date.toInstant().atZone(ZoneId.of("UTC")).toLocalDateTime();
	}
	
	public Date convertLocalDateToDate(LocalDate localDate) {
		ZoneId zoneId = ZoneId.of("UTC");
		return Date.from(localDate.atStartOfDay(zoneId).toInstant());
	}
	
	public Date convertLocalDateTimeToDate(LocalDateTime localDateTime) {
		ZoneId zoneId = ZoneId.of("UTC");
		return Date.from(localDateTime.atZone(zoneId).toInstant());
	}
	
	//EXCEL USING ZoneId.systemDefault()
	public LocalDate convertDoubleExcelToLocalDate(double dDate) {
		try {
			Date javaDate= DateUtil.getJavaDate(dDate);			
			return Instant.ofEpochMilli(javaDate.getTime()).atZone(ZoneId.systemDefault()).toLocalDate();
		}catch(Exception e) {
			return null;
		}
	}
	
	public boolean checkLocalDate(String input, String format) {
		try {
			DateTimeFormatter dtf = new DateTimeFormatterBuilder()
					.appendPattern(format)
					.parseStrict()
					.toFormatter();
			LocalDate.parse(input, dtf);
			return true;
		}catch(Exception e) {
			return false;
		}
	}
	
	public boolean checkLocalDateTime(String input, String format) {
		try {
			DateTimeFormatter dtf = new DateTimeFormatterBuilder()
					.appendPattern(format)
					.parseStrict()
					.toFormatter();
			LocalDateTime.parse(input, dtf);
			return true;
		}catch(Exception e) {
			return false;
		}
	}
	
	public String convertLocalDateTimeToString(TemporalAccessor temporalAccessor, String patternFormat) {
		DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern(patternFormat);
		return dateTimeFormatter.format(temporalAccessor);
	}
	
	public String convertLocalDateTimeStringToString(String input, String inputFormat, String outputFormat) {
		try {
			DateTimeFormatter formatter = DateTimeFormatter.ofPattern(inputFormat);
			LocalDate localDate = LocalDate.parse(input, formatter);
			return convertLocalDateTimeToString(localDate, outputFormat);
		}catch(Exception e) {
			return "";
		}
	}
	
	public String convertLocalDateTimeStringToString(String input, String inputFormat, String outputFormat, boolean hasTime) {
		try {
			DateTimeFormatter formatter = DateTimeFormatter.ofPattern(inputFormat);
			if(hasTime) {
				LocalDateTime localDate = LocalDateTime.parse(input, formatter);
				return convertLocalDateTimeToString(localDate, outputFormat);
			}else {
				LocalDate localDate = LocalDate.parse(input, formatter);
				return convertLocalDateTimeToString(localDate, outputFormat);
			}			
		}catch(Exception e) {
			return "";
		}
	}
	
	public LocalDateTime convertStringToLocalDateTime(String input, String format) {
		DateTimeFormatter dtf = new DateTimeFormatterBuilder()
				.appendPattern(format)
				.parseStrict()
				.toFormatter();
		LocalDateTime ldt = LocalDateTime.parse(input, dtf);
		return ldt;
	}
	
	public LocalDate convertStringToLocalDate(String input, String format) {
		DateTimeFormatter dtf = new DateTimeFormatterBuilder()
				.appendPattern(format)
				.parseStrict()
				.toFormatter();
		LocalDate ld = LocalDate.parse(input, dtf);
		return ld;
	}
	
	public int compareLocalDate(LocalDate t1, LocalDate t2) {
		return t1.compareTo(t2);
	}
	
	public int compareLocalDateTime(LocalDateTime t1, LocalDateTime t2) {
		return t1.compareTo(t2);
	}
		
	public LocalTime convertStringToLocalTime(String input, String format) {
		DateTimeFormatter dtf = new DateTimeFormatterBuilder()
				.appendPattern(format)
				.parseStrict()
				.toFormatter();
		LocalTime lt = LocalTime.parse(input, dtf);
		return lt;
	}
	/*END DATE TIME*/
	
	public boolean checkStringIsInt(String input) {
		return !"".equals(input) && input.chars().allMatch(Character::isDigit);
	}
	public Integer stringToInteger(String input) {
//		Integer value = Optional.of(input).map(Integer::valueOf).get();
		input = input.replaceAll(",", "");
		try {
//			Integer value = Optional.ofNullable(input).map(Integer::valueOf).orElse(0);
			Integer value = (int) ToNumber(input);
			return value;
		}catch(Exception e) {
//			e.printStackTrace();
			return 0;
		}
	}
	
	public boolean CheckNumber(String strDouble){
		if(null == strDouble) strDouble = "0";
		strDouble = strDouble.replaceAll(",", "");
		boolean boo = false;
		try{
			Double.parseDouble(strDouble);
			boo = true;
		}catch(Exception e){			
		}
		return boo;
	}
	
	public double ToNumber(String strDouble){
		if(strDouble==null) strDouble = "0";
		strDouble = strDouble.replaceAll(",", "");
		if(CheckNumber(strDouble)==false){
			return 0;
		}else{
			return Double.parseDouble(strDouble);
		}
	}
	
	public float ToNumberFloat(String strFloat) {
	    if (strFloat == null) strFloat = "0";
	    strFloat = strFloat.replaceAll(",", "");
	    if (!CheckNumber(strFloat)) {
	        return 0;
	    } else {
	        return Float.parseFloat(strFloat);
	    }
	}
	
	public int ToNumberInt(String strInt){
		if(strInt==null) strInt = "0";
		strInt = strInt.replaceAll(",", "");
		if(CheckNumber(strInt)==false){
			return 0;
		}else{
			return Integer.parseInt(strInt);
		}
	}
	
	public String formatNumberReal(String num) {
		return formatNumberReal(ToNumber(num));
	}
	
	public String formatNumberReal(Number num) {
		try {
//			NumberFormat nf = new DecimalFormat("#,##0.##########");
			NumberFormat nf = NumberFormat.getNumberInstance(Locale.US);
			DecimalFormat df = (DecimalFormat) nf;
			df.applyPattern("#,##0.#####");
			return df.format(num);
		}catch(Exception e) {
			return "";
		}
	}
	
	public String FormatCurrency(String strNumber, String currencyCode){
		if(null == strNumber || "".equals(strNumber)) return "";
		if(null == currencyCode) currencyCode = "VND";
		return FormatCurrency(ToNumber(strNumber), currencyCode);
	}
	
	public String FormatCurrency(double dblNumber, String currencyCode){
		String strReturn = "0";
		if(null == currencyCode) currencyCode = "VND";
		try{
			BigDecimal number = new BigDecimal(-1);
			NumberFormat numberFormat = null;
			number = new BigDecimal(dblNumber);
			if(currencyCode.equals("VND") || currencyCode.equals("JPY") || currencyCode.equals("CCC")){
				number.setScale(0, BigDecimal.ROUND_HALF_UP);
				numberFormat = new DecimalFormat("#,###");
			}else{
				number.setScale(2, BigDecimal.ROUND_HALF_UP);
				numberFormat = new DecimalFormat("#,##0.00");
			}
			strReturn = numberFormat.format(number);
		}catch(Exception e){
			strReturn = "";
		}
		return strReturn;
	}
	
	public String formatNumberPercent(Number num) {
		try {
			NumberFormat nf = NumberFormat.getNumberInstance(Locale.US);
			DecimalFormat df = (DecimalFormat) nf;
			df.applyPattern("#,##0.0000");
			return df.format(num);
		}catch(Exception e) {
			return "";
		}
	}
	
	public String formatNumberPercent(String input) {
		return formatNumberPercent(ToNumber(input));
	}
	
//https://www.java67.com/2014/11/how-to-convert-double-to-long-in-java-example.html
	public double div(double db1, double db2){
		return (db1 - db1%db2)/db2;
	}
	
	public int getNumberDaysInMonth(LocalDate date) {
		try {
			return date.withDayOfMonth(1).plusMonths(1).minusDays(1).get(ChronoField.DAY_OF_MONTH);
		}catch(Exception e) {
			return 30;
		}
	}
	
	public boolean docW3cToFile(Document doc, String pathName, String fileName) throws Exception{
		TransformerFactory tf = TransformerFactory.newInstance();
		Transformer t = tf.newTransformer();
//		t.setOutputProperty(OutputKeys.INDENT, "yes");
		t.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
		t.setOutputProperty(OutputKeys.DOCTYPE_PUBLIC, "yes");
		t.setOutputProperty(OutputKeys.METHOD, "xml");
//		t.setOutputProperty("{http://xml.apache.org/xslt}indent-amount","2");
		
		DOMSource source = new DOMSource(doc);
		StreamResult result = new StreamResult(new File(pathName, fileName));
		t.transform(source, result);
		return true;
	}
	
	public String docW3cToString(Document doc) throws Exception{
		Transformer tf = TransformerFactory.newInstance().newTransformer();
		tf.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
		tf.setOutputProperty(OutputKeys.INDENT, "yes");
		tf.setOutputProperty(OutputKeys.METHOD, "xml");
		tf.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no");
		tf.setOutputProperty(OutputKeys.STANDALONE, "yes");
		
//		tf.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
//		tf.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");
//		tf.setOutputProperty(OutputKeys.INDENT, "yes");
		
		Writer writer = new StringWriter();
		tf.transform(new DOMSource(doc), new StreamResult(writer));
		
//		Transformer txformer = TransformerFactory.newInstance().newTransformer();
//	      txformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no");
//	      txformer.setOutputProperty(OutputKeys.METHOD, "xml");
//	      txformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
//	      txformer.setOutputProperty(OutputKeys.INDENT, "yes");
//	      txformer.setOutputProperty(OutputKeys.STANDALONE, "yes");
//	      txformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
		
//		Writer writer = new StringWriter();
//		txformer.transform(new DOMSource(doc), new StreamResult(writer));
		
		
//		StreamResult out = new StreamResult("howto.xml");
//	    DOMSource domSource = new DOMSource(xmldoc);
//	    TransformerFactory tf = TransformerFactory.newInstance();
//	    Transformer transformer = tf.newTransformer();
//	    transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no");
//	    transformer.setOutputProperty(OutputKeys.METHOD, "xml");
//	    transformer.setOutputProperty(OutputKeys.ENCODING, "ISO-8859-1");
//	    transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");
//	    transformer.setOutputProperty(OutputKeys.INDENT, "yes");
//	    transformer.transform(domSource, out);
		
		return writer.toString();
	}
	
	public byte[] docW3cToByte(Document doc) throws Exception{
		TransformerFactory transformerFactory = TransformerFactory.newInstance();
		Transformer transformer = transformerFactory.newTransformer();
		DOMSource source = new DOMSource(doc);
		
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		StreamResult result=new StreamResult(bos);
		transformer.transform(source, result);
//		 byte []array = bos.toByteArray();
		return bos.toByteArray();
		
//		ByteArrayOutputStream baos = new ByteArrayOutputStream();
//	    org.apache.xml.security.utils.XMLUtils.outputDOM(doc, baos, true);
//	    return baos.toByteArray();
	}
	
	public Document stringToDocument(String input) throws Exception{
		DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder builder = builderFactory.newDocumentBuilder();
		return builder.parse(new ByteArrayInputStream(input.getBytes("UTF-8")));
	}
	
	public Document fileToDocument(String fileName) throws Exception{
		File file = new File(fileName);
		DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
//		documentBuilderFactory.setNamespaceAware(true);
		DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
		Document document = documentBuilder.parse(file);
		return document;
	}
	
	public Document fileToDocument(File file) throws Exception{
		DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
//		documentBuilderFactory.setNamespaceAware(true);
		DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
		Document document = documentBuilder.parse(file);
		return document;
	}
	
	public Document fileToDocument(File file, boolean isNamespaceAware) throws Exception{
		DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
		documentBuilderFactory.setNamespaceAware(isNamespaceAware);
		DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
		Document document = documentBuilder.parse(file);
		return document;
	}
	
	public Document inputStreamToDocument(InputStream is, boolean isNamespaceAware) throws Exception{
		try {
			DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
			documentBuilderFactory.setNamespaceAware(isNamespaceAware);
			DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
			Document document = documentBuilder.parse(is);
			return document;
		}catch(Exception e) {
			return null;
		}
	}
	
	public Document byteArrayToDocument(byte[] bytes) throws Exception{
		try {
			DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
			documentBuilderFactory.setNamespaceAware(true);
			DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
			Document document = documentBuilder.parse(new ByteArrayInputStream(bytes));
			return document;
		}catch(Exception e) {
			return null;
		}
	}
	
	public Element createElementWithValue(Document doc, String nodeName, String value) throws Exception{
		Element ele = doc.createElement(nodeName);
		ele.setTextContent(value);
		return ele;
	}
	
	public Element createElementTTKhac(Document doc, String TTruong, String KDLieu, String DLieu) throws Exception{
		Element ele = doc.createElement("TTin");
		
		Element eleTmp = doc.createElement("TTruong");
		eleTmp.setTextContent(TTruong);
		ele.appendChild(eleTmp);
		eleTmp = doc.createElement("KDLieu");
		eleTmp.setTextContent(KDLieu);
		ele.appendChild(eleTmp);
		eleTmp = doc.createElement("DLieu");
		eleTmp.setTextContent(DLieu);
		ele.appendChild(eleTmp);
		return ele;
	}
	
	public String getTextFromNodeXML(Element ele) {
		if(null == ele) return "";
		return ele.getTextContent();
	}
	
	public byte[] getBytesDataFromFile(String pathFileName) {
		try {
			File file = new File(pathFileName);
			if(!file.exists()) return null;
			byte[] bytes = null;
			FileInputStream fis = new FileInputStream(file);
			BufferedInputStream bis = new BufferedInputStream(fis);
			bytes = new byte[(int)file.length()];
			bis.read(bytes);
			bis.close();
			return bytes;
		}catch(Exception e) {
			e.printStackTrace();
			return null;
		}
	}
	
	public byte[] getBytesDataFromFile(File file) {
		try {
			if(!file.exists()) return null;
			byte[] bytes = null;
			FileInputStream fis = new FileInputStream(file);
			BufferedInputStream bis = new BufferedInputStream(fis);
			bytes = new byte[(int)file.length()];
			bis.read(bytes);
			bis.close();
			return bytes;
		}catch(Exception e) {
			e.printStackTrace();
			return null;
		}
	}
	
	public byte[] getFileBytes(File file) throws IOException {
	    ByteArrayOutputStream ous = null;
	    InputStream ios = null;
	    try {
	        byte[] buffer = new byte[4096];
	        ous = new ByteArrayOutputStream();
	        ios = new FileInputStream(file);
	        int read = 0;
	        while ((read = ios.read(buffer)) != -1)
	            ous.write(buffer, 0, read);
	    } finally {
	        try {
	            if (ous != null)
	                ous.close();
	        } catch (IOException e) {
	            // swallow, since not that important
	        }
	        try {
	            if (ios != null)
	                ios.close();
	        } catch (IOException e) {
	            // swallow, since not that important
	        }
	    }
	    return ous.toByteArray();
	}
	
	public String formatNumberBillInvoice(Number billNumber) {
		NumberFormat numberFormat = new DecimalFormat("00000000");
	
		return numberFormat.format(billNumber);
	}
	
	public String formatNumberBillInvoice(String billNumber) {
		return formatNumberBillInvoice(ToNumber(billNumber));
	}
	
	public String addSpacingAfterLeter(String input) {
		if(null == input || "".equals(input.trim())) return "";
		
		Collector<Character, StringJoiner, String> collectorAddSpacingAfterLeter = 
				Collector.of(
						() -> new StringJoiner(" "), 
						(sj, chr) -> sj.add(String.valueOf(chr)), 
						(j1, j2) -> j1.merge(j2), 
						j -> j.toString());
		return input.chars().mapToObj(chr -> (char) chr).collect(collectorAddSpacingAfterLeter);
	}
	
	public String formatLocalDateTimeToString(TemporalAccessor temporalAccessor, String patternFormat) {
		DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern(patternFormat);
		return dateTimeFormatter.format(temporalAccessor);
	}
	
	public LocalDate dateToLocalDate(Date d) {
		Instant instant = d.toInstant();
		ZonedDateTime zdt = instant.atZone(ZoneId.systemDefault());
		return zdt.toLocalDate();
	}
	
	public SignTypeInfo parserCert(String cert) {
    	try {
    		SignTypeInfo signTypeInfo = new SignTypeInfo();
    		
			CertificateFactory certificateFactory = CertificateFactory.getInstance("X.509");
			InputStream in = new ByteArrayInputStream(DatatypeConverter.parseBase64Binary(cert));
			X509Certificate x509Cert = (X509Certificate) certificateFactory.generateCertificate(in);
			signTypeInfo.setSerial(x509Cert.getSerialNumber().toString(16));
			if(signTypeInfo.getSerial().length() % 2 == 1)
				signTypeInfo.setSerial("0" + signTypeInfo.getSerial());
			signTypeInfo.setName(getAttributeCertificate(x509Cert.getSubjectDN().toString(), "CN"));
			signTypeInfo.setIssuedDate(formatLocalDateTimeToString(dateToLocalDate(x509Cert.getNotBefore()), Constants.FORMAT_DATE.FORMAT_DATE_DB));
			signTypeInfo.setExpireDate(formatLocalDateTimeToString(dateToLocalDate(x509Cert.getNotAfter()), Constants.FORMAT_DATE.FORMAT_DATE_DB));
			signTypeInfo.setCertificate(cert);
			String tempTaxCode = x509Cert.getSubjectDN().toString();
			int i = 0;
			if ((i = tempTaxCode.indexOf("MST", i)) != -1) {
				String mst = tempTaxCode.substring(i + 4);
				i = mst.indexOf(44);
				if (i == -1)
					tempTaxCode = mst;
				else
					tempTaxCode = mst.substring(0, i);
			}
			signTypeInfo.setTaxCode(tempTaxCode);
			in.close();
    		
    		return signTypeInfo;
    	}catch(Exception e) {
    		e.printStackTrace();
    		return null;
    	}
    }
	
	public static String getAttributeCertificate(String dn, String key) {
		String[] parts = dn.split(",");
		for (int i = 0; i < parts.length; i++) {
			String[] part = parts[i].split("=");
			part[0] = part[0].trim();
			if (part[0].compareTo(key) == 0) {
				return part[1];
			}
		}
		return null;
	}
	
	public String getCellValue(FormulaEvaluator formulaEvaluator, Cell cell){
		if(cell == null) return "";
		
		NumberFormat numberFormat = new DecimalFormat("#.##########");
		if(cell.getCellType() == CellType.FORMULA){
			if(cell.getCachedFormulaResultType() == CellType.NUMERIC)
				return numberFormat.format(cell.getNumericCellValue());
			else if(cell.getCachedFormulaResultType() == CellType.STRING)
				return cell.getRichStringCellValue().toString();
		}
		
		String result = "";
		
		CellType cellType = formulaEvaluator.evaluateInCell(cell).getCellType();
		if(CellType.BOOLEAN == cellType) {
			result = (cell.getBooleanCellValue() ? "1" : "0");
		}else if(CellType.NUMERIC == cellType) {
			if(DateUtil.isCellDateFormatted(cell)){
//				result = formatLocalDateTimeToString(cell.getDateCellValue().toInstant().atZone(ZoneId.systemDefault()).toLocalDate(), Constants.FORMAT_DATE.FORMAT_DATE_WEB);
				result = convertLocalDateTimeToString(cell.getDateCellValue().toInstant().atZone(ZoneId.systemDefault()).toLocalDate(), Constants.FORMAT_DATE.FORMAT_DATE_WEB);
			}else{
				result = numberFormat.format(formulaEvaluator.evaluate(cell).getNumberValue());
			}
		}else if(CellType.STRING == cellType) {
			result = formulaEvaluator.evaluate(cell).getStringValue();
		}else if(CellType.BLANK == cellType) {
			result = "";
		}else if(CellType.ERROR == cellType) {
			result = "";
		}else {
			result = "";
		}
		return result;
	}
	
	public static void main(String[] args) {
		Commons c = new Commons();
//		System.out.println(c.csRandomAlphaNumbericString(50));
		System.out.println(c.generateSHA("123123" + "123123", false));
		System.out.println(c.encryptThisString("123123" + "123123" ));
//		File file = new File("d:/abc.txt");
//		System.out.println(file.toString());
		
		
//		String MSTTCGP = "0315382923";
//		String MTDiep = MSTTCGP + c.csRandomAlphaNumbericString(46 - MSTTCGP.length()).toUpperCase();
//		System.out.println(MTDiep);
	}


	
	
	
	
	
	
	
	public  String encryptThisString(String input)
    {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-512");
            byte[] messageDigest = md.digest(input.getBytes());

            // Convert byte array into signum representation
            BigInteger no = new BigInteger(1, messageDigest);

            // Convert message digest into hex value
            String hashtext = no.toString(16);

            // Add preceding 0s to make it 32 bit
            while (hashtext.length() < 32) {
                hashtext = "0" + hashtext;
            }
            // return the HashText
            return hashtext;
        }

        // For specifying wrong message digest algorithms
        catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

	//	private static char[] VALID_NUMBERS = "0123456879".toCharArray();
	public String csRandomString(int numChars) {
		char buff[] = new char[numChars];
		SecureRandom srand = new SecureRandom();
		Random rand = new Random();
		for (int i = 0; i < numChars; i++) {
			if ((i % 10) == 0) {
				rand.setSeed(srand.nextLong()); // // 64 bits of random!
			}
			buff[i] = Constants.VALID_CHARACTERS[rand.nextInt(Constants.VALID_CHARACTERS.length)];
		}
		return new String(buff);
	}

	public String encodeImageToBase64(File f) {
		   String encodedfile = null;
           try {
               FileInputStream fileInputStreamReader = new FileInputStream(f);
               byte[] bytes = new byte[(int)f.length()];
               fileInputStreamReader.read(bytes);
               encodedfile = new String(Base64.encodeBase64(bytes), "UTF-8");
           } catch (FileNotFoundException e) {
               // TODO Auto-generated catch block
               e.printStackTrace();
           } catch (IOException e) {
               // TODO Auto-generated catch block
               e.printStackTrace();
           }

           return encodedfile;
	}

	public ByteArrayOutputStream doMergeMultiPdf(List<String> filePaths) throws Exception{
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		
		PDFMergerUtility pdfMerger = new PDFMergerUtility();
		pdfMerger.setDestinationStream(out);
		for(String path: filePaths) {
			pdfMerger.addSource(new File(path));
		}
		pdfMerger.mergeDocuments(MemoryUsageSetting.setupMainMemoryOnly());
		
		return out;
	}

		public boolean isValidEmailAddress(String email) {
//        String ePattern = "^[a-zA-Z0-9.!#$%&'*+/=?^_`{|}~-]+@((\\[[0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3}\\])|(([a-zA-Z\\-0-9]+\\.)+[a-zA-Z]{2,}))$";
//        java.util.regex.Pattern p = java.util.regex.Pattern.compile(ePattern);
//        java.util.regex.Matcher m = p.matcher(email);
//        return m.matches();
        
        EmailValidator emailvalidator = EmailValidator.getInstance();
        return emailvalidator.isValid(email);
	}

		public LocalDate formatStringToLocalDate(String input, String format) {
		      DateTimeFormatter dtf = new DateTimeFormatterBuilder()
		          .appendPattern(format)
		          .parseStrict()    //NGHIEM NGAT
//		          .parseLenient()    //KHOAN DUNG
		          .toFormatter();
		      LocalDate ld = LocalDate.parse(input, dtf);
		      return ld;
		    }
		
		
		public boolean copyFileUsingStream(File source, File dest) throws IOException {
			boolean r = false;
			InputStream is = null;
			OutputStream os = null;
			try {
				is = new FileInputStream(source);
				os = new FileOutputStream(dest);
				byte[] buffer = new byte[1024];
				int length;
				while ((length = is.read(buffer)) > 0) {
					os.write(buffer, 0, length);
				}
				r = true;
			} finally {
				is.close();
				os.close();
			}
			return r;
		}
		
		
		//CHUYEN TIENG VIET SANG KHONG DAU
		  public String deAccent(String input) {
		    try {
		      input = input.trim().replaceAll("\\s+", " ");
		      String temp = Normalizer.normalize(input, Normalizer.Form.NFD);
		      Pattern pattern = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");
		          return pattern.matcher(temp).replaceAll("").replaceAll(" ", "-")
		              .replaceAll("đ", "d").replaceAll("Đ", "D");
		    }catch(Exception e) {
		      e.printStackTrace();
		    }
		    return "";
		  }
}
