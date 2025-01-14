package vn.sesgroup.hddt.utils;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.net.URLDecoder;
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
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAccessor;
import java.util.Date;
import java.util.Locale;
import java.util.Optional;
import java.util.Random;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletRequest;
import javax.xml.bind.DatatypeConverter;

import org.apache.commons.validator.routines.EmailValidator;

import com.fasterxml.jackson.databind.JsonNode;

import vn.sesgroup.hddt.dto.SignTypeInfo;


public class Commons {
//	private static char[] VALID_CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456879".toCharArray();
	// cs = cryptographically secure
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
	
//	private static char[] VALID_NUMBERS = "0123456879".toCharArray();
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
	public String getTextJsonNode(JsonNode jsonNode) {
		try {
			return (jsonNode == null || jsonNode.isMissingNode() || jsonNode.isNull() /* || !jsonNode.isTextual() */)? "": jsonNode.asText();
		}catch(Exception e) {
			e.printStackTrace();
			return null;
		}
	}
	
	public String getParameterFromRequest(HttpServletRequest request, String paramName) {
		return request.getParameter(paramName) == null? "": request.getParameter(paramName);
	}
	
	public int randomInt(int bound) {
		Random rn = new Random();
		return rn.nextInt(bound) + 1;
	}
	
	/*DATE TIME*/
	public LocalDate convertDateToLocalDate(Date date) {
//		Instant.ofEpochMilli(date.getTime()).atZone(ZoneId.systemDefault()).toLocalDate();
//		return date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
		return date.toInstant().atZone(ZoneId.of("UTC")).toLocalDate();
	}
	public LocalDateTime convertDateToLocalDateTime(Date date) {
//		return date.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
		return date.toInstant().atZone(ZoneId.of("UTC")).toLocalDateTime();
	}
	
	public LocalDate convertDateGMT7ToLocalDate(Date date) {
		return date.toInstant().atZone(ZoneId.of("GMT+7")).toLocalDate();
	}
	public LocalDateTime convertDateGMT7ToLocalDateTime(Date date) {
		return date.toInstant().atZone(ZoneId.of("GMT+7")).toLocalDateTime();
	}
	
	public Date convertLocalDateToDate(LocalDate localDate) {
//		ZoneId zoneId = ZoneId.systemDefault();
		ZoneId zoneId = ZoneId.of("UTC");
		return Date.from(localDate.atStartOfDay(zoneId).toInstant());
	}
	
	public Date convertLocalDateTimeToDate(LocalDateTime localDateTime) {
//		ZoneId zoneId = ZoneId.systemDefault();
		ZoneId zoneId = ZoneId.of("UTC");
		return Date.from(localDateTime.atZone(zoneId).toInstant());
	}
	
	public boolean checkLocalDate(String input, String format) {
		try {
			DateTimeFormatter dtf = new DateTimeFormatterBuilder()
					.appendPattern(format)
					.parseStrict()		//NGHIEM NGAT
//					.parseLenient()		//KHOAN DUNG
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
					.parseStrict()		//NGHIEM NGAT
//					.parseLenient()		//KHOAN DUNG
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
				.parseStrict()		//NGHIEM NGAT
//				.parseLenient()		//KHOAN DUNG
				.toFormatter();
		LocalDateTime ldt = LocalDateTime.parse(input, dtf);
		return ldt;
	}
	
	public LocalDate convertStringToLocalDate(String input, String format) {
		DateTimeFormatter dtf = new DateTimeFormatterBuilder()
				.appendPattern(format)
				.parseStrict()		//NGHIEM NGAT
//				.parseLenient()		//KHOAN DUNG
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
	
//	public Date convertStringToDateByFormat(String strDate,String format){
//		if(strDate.equals("")){return null;}
//		Date date = new Date();
//		try{
//			DateFormat formatter = new SimpleDateFormat(format);
//			if(strDate.equals("")) return null;
//			date = (Date)formatter.parse(strDate);
//		}catch(Exception e){			
//		}
//		return date;
//	}
	
	public LocalTime convertStringToLocalTime(String input, String format) {
		DateTimeFormatter dtf = new DateTimeFormatterBuilder()
				.appendPattern(format)
				.parseStrict()		//NGHIEM NGAT
//				.parseLenient()		//KHOAN DUNG
				.toFormatter();
		LocalTime lt = LocalTime.parse(input, dtf);
		return lt;
	}
	
	public LocalDateTime convertLongToLocalDateTime(long longValue) {
//		LocalDate date = Instant.ofEpochMilli(longValue).atZone(ZoneId.systemDefault()).toLocalDate();
//		LocalDate date =
//				  Instant.ofEpochMilli(startDateLong)
//				  .atZone(ZoneId.systemDefault())
//				  .toLocalDate();
		try {
			LocalDateTime dateTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(longValue), ZoneId.of("UTC"));
			return dateTime;
		}catch(Exception e) {
			e.printStackTrace();
			return null;
		}
	}
	
	public LocalDateTime convertLongToLocalDateTime(long longValue, String zoneId) {
//		LocalDate date = Instant.ofEpochMilli(longValue).atZone(ZoneId.systemDefault()).toLocalDate();
//		LocalDate date =
//				  Instant.ofEpochMilli(startDateLong)
//				  .atZone(ZoneId.systemDefault())
//				  .toLocalDate();
		try {
			LocalDateTime dateTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(longValue), ZoneId.of(zoneId));
			return dateTime;
		}catch(Exception e) {
			e.printStackTrace();
			return null;
		}
	}
	
	public LocalDate convertLongToLocalDate(long longValue) {
//		ZoneId zoneId = ZoneId.of("UTC");
//		return Date.from(localDateTime.atZone(zoneId).toInstant());
		try {
			return Instant.ofEpochMilli(longValue).atZone(ZoneId.of("UTC")).toLocalDate();
		}catch(Exception e) {
			e.printStackTrace();
			return null;
		}
	}
	
	/**
	 * -1: from > to
	 * 0: from = to
	 * >0: from < to
	 * */
	public long calcDateBetween2Date(LocalDate from, LocalDate to) {
		return ChronoUnit.DAYS.between(from, to);
	}
	
	/*END DATE TIME*/
	
	public boolean checkStringWithRegex(String regex, String input) {
		Pattern pattern = Pattern.compile(regex);
		return pattern.matcher(input).matches();
	}
	
	public String encodeStringBase64(String input) {
		return DatatypeConverter.printBase64Binary(input.getBytes(StandardCharsets.UTF_8));
	}
	
	
	public String decodeBase64ToString(String stringBase64){
//		stringBase64 = stringBase64.replaceAll(" ", "+");
		stringBase64 = stringBase64.replaceAll("@", "+");
		return new String(DatatypeConverter.parseBase64Binary(stringBase64), StandardCharsets.UTF_8);
	}
	
	public String decodeURIComponent(String val){
		try {
			return URLDecoder.decode(val, StandardCharsets.UTF_8.toString());
		}catch(Exception e) {
			return val;
		}
		
	}
	
	public boolean isValidEmailAddress(String email) {
//        String ePattern = "^[a-zA-Z0-9.!#$%&'*+/=?^_`{|}~-]+@((\\[[0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3}\\])|(([a-zA-Z\\-0-9]+\\.)+[a-zA-Z]{2,}))$";
//        java.util.regex.Pattern p = java.util.regex.Pattern.compile(ePattern);
//        java.util.regex.Matcher m = p.matcher(email);
//        return m.matches();
        
        EmailValidator emailvalidator = EmailValidator.getInstance();
        return emailvalidator.isValid(email);
	}
	
	public String getAttributeCertificate(String dn, String key) {
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
	
	public boolean checkStringIsInt(String input) {
		return input.chars().allMatch(Character::isDigit);
	}
	public Integer stringToInteger(String input) {
//		Integer value = Optional.of(input).map(Integer::valueOf).get();
		try {
			Integer value = Optional.ofNullable(input).map(Integer::valueOf).orElse(0);
			return value;
		}catch(Exception e) {
//			e.printStackTrace();
			return 0;
		}
	}
	
	public Long stringToLong(String input) {
		try {
			Long value = Optional.ofNullable(input).map(Long::valueOf).orElse(0L);
			return value;
		}catch(Exception e) {
			e.printStackTrace();
			return 0L;
		}
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
	
	public String formatNumberBillInvoice(Number billNumber) {
		NumberFormat numberFormat = new DecimalFormat("00000000");
		//NumberFormat numberFormat = new DecimalFormat("");
		return numberFormat.format(billNumber);
	}
	
	public String formatNumberBillInvoice(String billNumber) {
		return formatNumberBillInvoice(ToNumber(billNumber));
	}
	
	public String formatNumberReal(String num) {
		return formatNumberReal(ToNumber(num));
	}
	
	public String formatNumberReal(Number num) {
		try {
//			NumberFormat nf = new DecimalFormat("#,##0.##########");
			NumberFormat nf = NumberFormat.getNumberInstance(Locale.US);
			DecimalFormat df = (DecimalFormat) nf;
			df.applyPattern("#,##0.##########");
			return df.format(num);
		}catch(Exception e) {
			return "";
		}
	}
	
	public static void main(String[] args) throws Exception{
		Commons commons = new Commons();
//		System.out.println(commons.csRandomAlphaNumbericString(50));
		
		String cert = "MIIEkTCCA3mgAwIBAgIQVAEBCSaoM/JUI1GGbtQ6VjANBgkqhkiG9w0BAQUFADBgMQswCQYDVQQGEwJWTjE6MDgGA1UECgwxQ8OUTkcgVFkgQ+G7lCBQSOG6pk4gVknhu4ROIFRIw5RORyBORVdURUwtVEVMRUNPTTEVMBMGA1UEAxMMTkVXVEVMLUNBIHYyMB4XDTIwMTExNzAxMjkwOVoXDTIyMTAyNTAxMTgyN1owbDEeMBwGCgmSJomT8ixkAQEMDk1TVDowMzA0MjA2MjM1MT0wOwYDVQQDDDRDw5RORyBUWSBUTkhIIFRISeG6vlQgS+G6viBWw4AgWMOCWSBE4buwTkcg4bqkTiBUw41OMQswCQYDVQQGEwJWTjCBnzANBgkqhkiG9w0BAQEFAAOBjQAwgYkCgYEA0K1Oyrd+LL92NQR/SZCNqvyO4Q37jw4woD/B60Mrp39jrrx215DW7ZdAkTRf4SixDjifSfvMaVBXdTFT02lLW5Qqfj5urzo9udGoaujErZKqr762HyEHEMwIIcLb/qOZLbL0iFajhI5nL6+dndjW22iC/hoPDbYgIM4GRN1MetcCAwEAAaOCAb0wggG5MAwGA1UdEwEB/wQCMAAwHwYDVR0jBBgwFoAUgvDxIe/+Tlg0cW/5jEIPPyHdVZswawYIKwYBBQUHAQEEXzBdMC4GCCsGAQUFBzAChiJodHRwOi8vcHViMi5uZXdjYS52bi9uZXd0ZWwtY2EuY3J0MCsGCCsGAQUFBzABhh9odHRwOi8vb2NzcDIubmV3Y2Eudm4vcmVzcG9uZGVyMCAGA1UdEQQZMBeBFWR1eXF1YW5nY3BhQGdtYWlsLmNvbTBfBgNVHSAEWDBWMFQGDCsGAQQBge0DAQkDATBEMCMGCCsGAQUFBwIBFhdodHRwOi8vcHViLm5ld2NhLnZuL3JwYTAdBggrBgEFBQcCAjARDA9PU19SZW5ld18zWV9LTTYwNAYDVR0lBC0wKwYIKwYBBQUHAwIGCCsGAQUFBwMEBgorBgEEAYI3CgMMBgkqhkiG9y8BAQUwMwYDVR0fBCwwKjAooCagJIYiaHR0cDovL2NybDIubmV3Y2Eudm4vbmV3dGVsLWNhLmNybDAdBgNVHQ4EFgQUh4e4+VVcNNw2iByGiqAYvjoIxkswDgYDVR0PAQH/BAQDAgTwMA0GCSqGSIb3DQEBBQUAA4IBAQAK8MCDdcFXs4bW0tbigtfmn6iq+HcAE/Hz11m37QW9PCKumZd7jVX7HGEQMLo+K8IAD19OBilwWI7TiRuuPHIU7cjdDaDn1wRCoq0IXRDe/QMBW+4J6E4H+UFT2FVppM1LoOjprngVFPBIDRnclQibtBhpVgQmlHuEmao3MJYmsVQQPi3asE2TrEAzPifgIvQ/U2oiM8mu5vFPK+Tt+XgCgyhReUEy4Ll0w8ZW7CHecXsg6isWdXu9mty87YR0XjcbDBm8eVFJXvK/vXjlmZwDfqQtyKWevtp9NjgI99tq5G0JDUy1fM+9j2CwdSAXNykZunhy+Dvk/aN+e0toRsyV";
		cert = "MIIEhjCCA26gAwIBAgIQVAEBCaSWnMiJg5EqxTF2eDANBgkqhkiG9w0BAQUFADBgMQswCQYDVQQGEwJWTjE6MDgGA1UECgwxQ8OUTkcgVFkgQ+G7lCBQSOG6pk4gVknhu4ROIFRIw5RORyBORVdURUwtVEVMRUNPTTEVMBMGA1UEAxMMTkVXVEVMLUNBIHYyMB4XDTE5MDcxNjA2NDQzMVoXDTIyMDcxNTA2NDQzMVowZTELMAkGA1UEBhMCVk4xNjA0BgNVBAMMLUPDlE5HIFRZIFROSEggU+G6ok4gWFXhuqRUIFRIxq/GoE5HIE3huqBJIE1UQTEeMBwGCgmSJomT8ixkAQEMDk1TVDowMzE1NzgzNTE2MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQCo2DIopzERrtAhVhgsi10qJEMa7AWmwIeM4kO/uKHKIkCq/sTWu4vb81jEOawTvcAy8BWvcRE2inxe/HlXJwb1csWbbbI3dj/wS/tX6CD76rvU8dAjT189UK55YoTYl6ZYRvVJ6z37msDw1UA/geyv/3OQaSBq0wwhvErx0TvaIQIDAQABo4IBuTCCAbUwDAYDVR0TAQH/BAIwADAfBgNVHSMEGDAWgBSC8PEh7/5OWDRxb/mMQg8/Id1VmzBrBggrBgEFBQcBAQRfMF0wLgYIKwYBBQUHMAKGImh0dHA6Ly9wdWIyLm5ld2NhLnZuL25ld3RlbC1jYS5jcnQwKwYIKwYBBQUHMAGGH2h0dHA6Ly9vY3NwMi5uZXdjYS52bi9yZXNwb25kZXIwIgYDVR0RBBswGYEXY3VuZ2NhcGtldG9hbkBnbWFpbC5jb20wWQYDVR0gBFIwUDBOBgwrBgEEAYHtAwEJAwEwPjAjBggrBgEFBQcCARYXaHR0cDovL3B1Yi5uZXdjYS52bi9ycGEwFwYIKwYBBQUHAgIwCwwJT1NfTmV3XzNZMDQGA1UdJQQtMCsGCCsGAQUFBwMCBggrBgEFBQcDBAYKKwYBBAGCNwoDDAYJKoZIhvcvAQEFMDMGA1UdHwQsMCowKKAmoCSGImh0dHA6Ly9jcmwyLm5ld2NhLnZuL25ld3RlbC1jYS5jcmwwHQYDVR0OBBYEFOyJfoa1Rkzzq/2KsDiMBSGh2OpoMA4GA1UdDwEB/wQEAwIE8DANBgkqhkiG9w0BAQUFAAOCAQEAV8E64KDRgGwy7fxO0LTe8Dp+ChFvDRXlo8irxRNHhEV3yrsi0/rS6bsg5aQcnHU94nA9wz8dyiehifnZ90mNP1ruv8D7xjDZP3r3cy6VIKLKdRoFXxGXPugI4k3RdTbu/lyjUryZLmM4AEM2GspoXVAW1VbwcZQeHL/pkO3q8EmXp8lyJ/4mpxKJOzMH9Xr/MXCxLS57ByYZOoaPortBk/bCQvCXjgwbjNgYzPtLa6GHZJguljuZizhfl5G/D/SZlddpI1PrGGe28D58A8LfdI1upDuOi0bGp4NLm8mp6blioyYkuenYrgHCIkdOTlElYv08GR48i8WlLb8I/ujXTw==";
		cert = "MIIEjTCCA3WgAwIBAgIQVAEOABTsu0UCUCb2HKIcZzANBgkqhkiG9w0BAQsFADBWMQswCQYDVQQGEwJWTjEvMC0GA1UECgwmSE4gSU5WRVNUTUVOVCBBTkQgQlVTSU5FU1MgU1VQUE9SVCBKU0MxFjAUBgNVBAMMDU5DLUNBIFNIQS0yNTYwHhcNMjAwNjE2MDk0NTUwWhcNMjQwNjE1MDk0NTUwWjCBvzELMAkGA1UEBhMCVk4xGjAYBgNVBAgMEVRQIEjhu5MgQ2jDrSBNaW5oMTkwNwYDVQQKDDBDw5RORyBUWSBUTkhIIFRIxq/GoE5HIE3huqBJIFbDgCBE4buKQ0ggVuG7pCBUSFkxOTA3BgNVBAMMMEPDlE5HIFRZIFROSEggVEjGr8agTkcgTeG6oEkgVsOAIEThu4pDSCBW4bukIFRIWTEeMBwGCgmSJomT8ixkAQEMDk1TVDowMzAxNTAyNzk3MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAui4JqT6pinKG3zqp+vbVy0g1LuVccC07jHWfd6C/iBxKdr3cU39KVwqGFKh7t3alqNrHDi0nRy7nxBQS2VSFxRlOYP5w3gHdb5OwJDDcO/Nqc9UYHVhJbrXlIsn2LH7JS0v4prCm6q9jZ7D82v+WPTKPZ2lB7XmbbZ9otWcHcsEsKHPNtkXqmyhsS+VW+O1x82i+kaRlc/JNHOfSMxJJvwKaPI20MyxYOlFH503ExK/g33XHZN36Iwu4/q9i3SFMaas0zUDEExXhf1m0wrV/yhOuFuFepT6drnryEX1Q2Oj/zS0ye0m8zoYXrcbvyq5CQynhn2BkOKb8WMvqn9JMEwIDAQABo4HsMIHpMAwGA1UdEwEB/wQCMAAwHwYDVR0jBBgwFoAU92R6QX/5zOohws0TNWcSuMN6QZwwOgYIKwYBBQUHAQEELjAsMCoGCCsGAQUFBzABhh5odHRwOi8vb2NzcC5uYy1jYS52bi9yZXNwb25kZXIwHQYDVR0lBBYwFAYIKwYBBQUHAwIGCCsGAQUFBwMEMC4GA1UdHwQnMCUwI6AhoB+GHWh0dHA6Ly9jcmwubmMtY2Eudm4vbmMtY2EuY3JsMB0GA1UdDgQWBBTWv/lL58BkId3lk3A6tZkJlbAiQDAOBgNVHQ8BAf8EBAMCBeAwDQYJKoZIhvcNAQELBQADggEBAFOJ1gl8qcBrVEiwqzd6TEnvVwfL61W8FdEOeuIRrZKQ4bFduXTYfIDagfZHWPIzqIYiiDjLWACHtzPAAj970kNTrAxHqL2xU46OFOJgT2S0r5hRFyrzyKo2dFP75k3nkdtxC8qS4F8XYNrnbJmxi+2PSaEEErDHIYKMroEFiQc0Y3El1mjNaMtAjjHu1WniRfa1GTMwrwjQewpOOURZlGESZXfOy8Txjf6VM6Y5sStWjONdMZ32V+iyKiVyyDIjr+oLQwKKAp0BH55gnLSszR5RYDQGCrYVQWH9bDZ5DoIYzBCYqt7s9C+IQjfs9xlkzXNFeSkdGNSDiuvBKAKfkPk=";
		
		cert = "MIIEvDCCA6SgAwIBAgIQVAEBEBuZOKe9Y/YFyVctoTANBgkqhkiG9w0BAQUFADBwMQswCQYDVQQGEwJWTjE/MD0GA1UEChM2VmlldG5hbSBFRlkgSW5mb3JtYXRpY3MgVGVjaG5vbG9neSBKb2ludCBTdG9jayBDb21wYW55MQ8wDQYDVQQLEwZFRlktQ0ExDzANBgNVBAMTBkVGWS1DQTAeFw0yMDAzMTkwNjQ0MTNaFw0yMzAzMTYwOTEzMDBaMIGVMQswCQYDVQQGEwJWTjEaMBgGA1UECAwRVFAgSOG7kyBDaMOtIE1pbmgxGjAYBgNVBAcMEVF14bqtbiBUw6JuIELDrG5oMS4wLAYDVQQDDCVDw5RORyBUWSBD4buUIFBI4bqmTiBL4bu4IFRIVeG6rFQgQlROMR4wHAYKCZImiZPyLGQBAQwOTVNUOjAzMTU1MzAwMDAwggEiMA0GCSqGSIb3DQEBAQUAA4IBDwAwggEKAoIBAQDcu7K4l8PalXPGSdd0jXWyzCGfzcIJTkA/BA62wojaO4WTZXPZgTIYK66EJA6Yb+wRJi1GsijYNaMf0TjY8sG1RpT//UvDqJnapVoD/moXtM2aCGh7CKtnXVcMftMqMCNXtO5aDMC0rIQbvk9ndf9MALSeeteDSp8rWNeXkPh6ACiCzpbZhJnStiDngA8CY+abWqFtECWLmVbWv4TzsaFWzAwULMjTfnzUlOLCgUrK2fYyoRONsLknfZyCh9VHPHR7k6nKE4EDC0v0mdOLiRscB6JRk+fFAe8PI0OGjnwamqLvv5ZfiFx80O9mgjTO3VYcy82+j0NpYUVVYPtVPb2XAgMBAAGjggEqMIIBJjAMBgNVHRMBAf8EAjAAMB8GA1UdIwQYMBaAFKUb1CpA5eA37ZRWyaKT/Eg187nTMGAGCCsGAQUFBwEBBFQwUjAuBggrBgEFBQcwAoYiaHR0cDovL3ZhLmVmeWNhLnZuL2NlcnRzL2VmeWNhLmNydDAgBggrBgEFBQcwAYYUaHR0cDovL29jc3AuZWZ5Y2Eudm4wGAYDVR0gBBEwDzANBgsrBgEEAYHtAwEKATAdBgNVHSUEFjAUBggrBgEFBQcDAgYIKwYBBQUHAwQwKwYDVR0fBCQwIjAgoB6gHIYaaHR0cDovL3ZhLmVmeWNhLnZuL3B1Yi9DUkwwHQYDVR0OBBYEFJxKF/fawMAkg9E3PXv5H0YFB3ccMA4GA1UdDwEB/wQEAwIF4DANBgkqhkiG9w0BAQUFAAOCAQEAISlbqhhd/W6iXnUbIlYI6pT0U7BniwWslCBJNG7ZrTF+B/4bUjAcI+T6qv2fMCg/4i6EzRMYQZxjCc1ARHtGc7ik51wLJ5GdrKUx9K/c1gjxNo/LJW3Ia6vUs14g/H8G152/F6gclwv2HYRWmvERdzNeqMoPuNRsKRazYeGfxfQPDqEzGf2FQizJGjdZ1k9Ao7vA2zfZY7uLo5ZICI3cApHgVeTrk/flOymFFG1R4jGziWXebAXZu+Fk3zQTdlzOLA7fMhpvHTQugGitEbHOAnVyAE6vT1OIF0/kt6WSMqzZS8QEmtGgGROBQMdTmXyi1VkP/nkpI1057Tf9+fWimA==";
		
		CertificateFactory certificateFactory = null;
		InputStream in = null;
		X509Certificate x509Cert = null;
		
		certificateFactory = CertificateFactory.getInstance("X.509");
		cert = cert.replaceAll("@", "+");
		in = new ByteArrayInputStream(DatatypeConverter.parseBase64Binary(cert));
		x509Cert = (X509Certificate) certificateFactory.generateCertificate(in);
		
		String serialNumber = x509Cert.getSerialNumber().toString(16);
		
		System.out.println(serialNumber);
		System.out.println(commons.getAttributeCertificate(x509Cert.getIssuerDN().toString(), "O"));
		System.out.println(commons.convertLocalDateTimeToString(commons.convertDateGMT7ToLocalDateTime(x509Cert.getNotBefore()), Constants.FORMAT_DATE.FORMAT_DATE_TIME_WEB));
		System.out.println(commons.convertLocalDateTimeToString(commons.convertDateGMT7ToLocalDateTime(x509Cert.getNotAfter()), Constants.FORMAT_DATE.FORMAT_DATE_TIME_WEB));
		
		String SOLUTION_TAXCODE = "0315382923";
		String MNNhan = "0401486901";
		String MTDiep = SOLUTION_TAXCODE + commons.csRandomAlphaNumbericString(46 - SOLUTION_TAXCODE.length()).toUpperCase();
		System.out.println(MTDiep);
	}
	/*SHA encode*/
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
	/*SHA decode*/
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

		public String encodeImageToBase64(InputStream fis) {
		String base64Image = "";
		try {
			byte imageData[] = new byte[fis.available()];
			fis.read(imageData);
			base64Image = java.util.Base64.getEncoder().encodeToString(imageData);
		}catch(Exception e) {
			e.printStackTrace();
		}
		return base64Image;
	}

		public LocalDateTime formatStringToLocalDateTime(String input, String format) {
			DateTimeFormatter dtf = new DateTimeFormatterBuilder()
					.appendPattern(format)
					.parseStrict()		//NGHIEM NGAT
//					.parseLenient()		//KHOAN DUNG
					.toFormatter();
			LocalDateTime ldt = LocalDateTime.parse(input, dtf);
			return ldt;
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
