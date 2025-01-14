package vn.sesgroup.hddt.resources;

import java.net.URI;
import java.util.Arrays;
import java.util.HashMap;

import org.apache.commons.lang3.SerializationUtils;
import org.apache.http.conn.HttpHostConnectException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RequestCallback;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.ResponseExtractor;
import org.springframework.web.client.RestTemplate;

import com.api.message.JSONRoot;
import com.api.message.MsgRsp;
import com.api.message.MspResponseStatus;

import vn.sesgroup.hddt.dto.FileInfo;
import vn.sesgroup.hddt.dto.LoginRes;
import vn.sesgroup.hddt.dto.req.UserLoginReq;
import vn.sesgroup.hddt.utils.Constants;
import vn.sesgroup.hddt.utils.Json;

@Service
public class RestAPIUtility {
	private static final Logger log = LogManager.getLogger(RestAPIUtility.class);
	@Autowired RestTemplate restTemplate;
	
	public LoginRes callAPILogin(String url, HttpMethod httpMethod, UserLoginReq user) throws Exception {
		LoginRes res = new LoginRes();
		try {
			HttpHeaders headers = new HttpHeaders();
			headers.setAccept(Arrays.asList(new MediaType[] { MediaType.APPLICATION_JSON }));
			headers.setContentType(MediaType.APPLICATION_JSON);
			headers.add(APIParams.API_LICENSE_KEY_NAME, APIParams.HTTP_LICENSEKEY);

			HttpEntity<UserLoginReq> requestBody = new HttpEntity<UserLoginReq>(user, headers);
			ResponseEntity<LoginRes> result = restTemplate.exchange(APIParams.HTTP_URI + url, httpMethod,
					requestBody, LoginRes.class);
			if (result.getStatusCode() == HttpStatus.OK) {
				res = result.getBody();
			}
		} catch (HttpClientErrorException e) {
			res = new LoginRes();
			res.setStatusCode(e.getRawStatusCode());
			res.setStatusText("Lỗi kết nối API...");
		} catch (ResourceAccessException e) {
			if(e.getCause() instanceof HttpHostConnectException) {
				res = new LoginRes();
				res.setStatusCode(999);
				res.setStatusText("Connection to api refused");
			}else {
				res = new LoginRes();
				res.setStatusCode(999);
				res.setStatusText("Kết nối đến API bị từ chối...");
			}
		} catch (Exception e) {
			res = new LoginRes();
			res.setStatusCode(999);
			res.setStatusText("Lỗi ngoại lệ...");
		}
		return res;
	}
	
	public MsgRsp callAPINormal(String url, String tokenAuth, HttpMethod httpMethod, JSONRoot jsonRoot)
			throws Exception {
		MsgRsp rsp = null;
		MspResponseStatus responseStatus = null;
		try {
			HttpHeaders headers = new HttpHeaders();
			headers.setAccept(Arrays.asList(new MediaType[] { MediaType.APPLICATION_JSON }));
			headers.setContentType(MediaType.APPLICATION_JSON);
			headers.add(APIParams.API_LICENSE_KEY_NAME, APIParams.HTTP_LICENSEKEY);
			headers.add(Constants.TOKEN_HEADER, tokenAuth);

			HttpEntity<JSONRoot> requestBody = new HttpEntity<>(jsonRoot, headers);
			ResponseEntity<MsgRsp> result = restTemplate.exchange(APIParams.HTTP_URI + url, httpMethod, requestBody, MsgRsp.class);
			if (result.getStatusCode() == HttpStatus.OK) {
				rsp = result.getBody();
			}
		} catch (HttpClientErrorException e) {
			if (e.getRawStatusCode() == HttpStatus.UNAUTHORIZED.value()) {
				responseStatus = new MspResponseStatus(e.getRawStatusCode(),
						"Yêu cầu chưa được áp dụng vì nó thiếu thông tin xác thực hợp lệ cho tài nguyên đích.");
				rsp = new MsgRsp(jsonRoot.getMsg().getMsgHeader());
				rsp.setResponseStatus(responseStatus);
			} else if (e.getRawStatusCode() == HttpStatus.NOT_FOUND.value()) {
				responseStatus = new MspResponseStatus(e.getRawStatusCode(), "Không tìm thấy hàm xử lý dữ liệu.");
				rsp = new MsgRsp(jsonRoot.getMsg().getMsgHeader());
				rsp.setResponseStatus(responseStatus);
			} else if (e.getRawStatusCode() == HttpStatus.FORBIDDEN.value()) {
				responseStatus = new MspResponseStatus(e.getRawStatusCode(), "API License key không hợp lệ. Vui lòng liên hệ Admin để biết thêm chi tiết.");
				rsp = new MsgRsp(jsonRoot.getMsg().getMsgHeader());
				rsp.setResponseStatus(responseStatus);
			}else {
				responseStatus = new MspResponseStatus(e.getRawStatusCode(), "Lỗi kết nối API...");
				rsp = new MsgRsp(jsonRoot.getMsg().getMsgHeader());
				rsp.setResponseStatus(responseStatus);
			}
		} catch (Exception e) {
			responseStatus = new MspResponseStatus(999, "Lỗi ngoại lệ...");
			rsp = new MsgRsp(jsonRoot.getMsg().getMsgHeader());
			rsp.setResponseStatus(responseStatus);
		}
		
		if(null == rsp) {
			responseStatus = new MspResponseStatus(999, "Không tìm thấy nội dung dữ liệu trả về");
			rsp = new MsgRsp(jsonRoot.getMsg().getMsgHeader());
			rsp.setResponseStatus(responseStatus);
		}
		
		return rsp;
	}
	
	public FileInfo callAPIGetFileInfo(String url, String tokenAuth, HttpMethod httpMethod, JSONRoot jsonRoot) throws Exception{
		FileInfo fileInfo = null;
		
		try {
			RequestCallback requestCallback = request -> {
				request.getHeaders().add(APIParams.API_LICENSE_KEY_NAME, APIParams.HTTP_LICENSEKEY);
				request.getHeaders().add(Constants.TOKEN_HEADER, tokenAuth);
				request.getHeaders().add("Content-Type", "application/json");
				request.getHeaders().setAccept(Arrays.asList(MediaType.APPLICATION_OCTET_STREAM, MediaType.ALL));
				request.getBody().write(Json.serializer().toString(jsonRoot).getBytes());
			};
			
			ResponseExtractor<FileInfo> responseExtractor = rsp -> {
				return SerializationUtils.deserialize(rsp.getBody());
			};
			
			fileInfo = restTemplate.execute(URI.create(APIParams.HTTP_URI + url), httpMethod, requestCallback, responseExtractor);
			return fileInfo;
		}catch (HttpClientErrorException e) {
			log.error(">>>>> An exception occurred!", e);
			if (e.getRawStatusCode() == HttpStatus.UNAUTHORIZED.value()) {
				return null;
			} else if (e.getRawStatusCode() == HttpStatus.NOT_FOUND.value()) {
				return null;
			} else {
				return null;
			}
		} catch (Exception e) {
			log.error(">>>>> An exception occurred!", e);
			return null;
		}

	}
	
	public MsgRsp callAPIUploadTmpFile(String url, String tokenAuth, HttpMethod httpMethod,
			String issuerId, String fileName, byte[] fileData) throws Exception {
		MsgRsp rsp = null;
		MspResponseStatus responseStatus = null;
		try {
			HttpHeaders headers = new HttpHeaders();
			headers.setAccept(Arrays.asList(new MediaType[] { MediaType.APPLICATION_JSON }));
			headers.setContentType(MediaType.MULTIPART_FORM_DATA);
			headers.add(APIParams.API_LICENSE_KEY_NAME, APIParams.HTTP_LICENSEKEY);
			headers.add(Constants.TOKEN_HEADER, tokenAuth);

			MultiValueMap<String, String> fileMap = new LinkedMultiValueMap<>();
			ContentDisposition contentDisposition = ContentDisposition.builder("form-data").name("file")
					.filename(fileName).build();
			fileMap.add(HttpHeaders.CONTENT_DISPOSITION, contentDisposition.toString());
			HttpEntity<byte[]> fileEntity = new HttpEntity<>(fileData, fileMap);

			MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
			body.add("file", fileEntity);
			body.add("IssuerId", issuerId);
			HttpEntity<MultiValueMap<String, Object>> requestBody = new HttpEntity<>(body, headers);

			ResponseEntity<MsgRsp> result = restTemplate.exchange(APIParams.HTTP_URI + url, httpMethod, requestBody,
					MsgRsp.class);
			if (result.getStatusCode() == HttpStatus.OK) {
				rsp = result.getBody();
			}
		} catch (HttpClientErrorException e) {
			if (e.getRawStatusCode() == HttpStatus.UNAUTHORIZED.value()) {
				responseStatus = new MspResponseStatus(e.getRawStatusCode(),
						"Yêu cầu chưa được áp dụng vì nó thiếu thông tin xác thực hợp lệ cho tài nguyên đích.");
				rsp.setResponseStatus(responseStatus);
			} else if (e.getRawStatusCode() == HttpStatus.NOT_FOUND.value()) {
				responseStatus = new MspResponseStatus(e.getRawStatusCode(), "Không tìm thấy hàm xử lý dữ liệu.");
				rsp.setResponseStatus(responseStatus);
			} else {
				responseStatus = new MspResponseStatus(e.getRawStatusCode(), "Lỗi kết nối API...");
				rsp.setResponseStatus(responseStatus);
			}
		} catch (Exception e) {
			responseStatus = new MspResponseStatus(999, "Lỗi ngoại lệ...");
			rsp.setResponseStatus(responseStatus);
		}
		return rsp;
	}

	public FileInfo callAPITracuuHD(String url, HttpMethod httpMethod, HashMap<String, String> mapInput) throws Exception{
		FileInfo fileInfo = null;
		
		try {
			RequestCallback requestCallback = request -> {
				request.getHeaders().add(APIParams.API_LICENSE_KEY_NAME, APIParams.HTTP_LICENSEKEY);
				request.getHeaders().add("Content-Type", "application/json");
				request.getHeaders().setAccept(Arrays.asList(MediaType.APPLICATION_OCTET_STREAM, MediaType.ALL));
				request.getBody().write(Json.serializer().toString(mapInput).getBytes());
			};
			
			ResponseExtractor<FileInfo> responseExtractor = rsp -> {
				return SerializationUtils.deserialize(rsp.getBody());
			};
			
			fileInfo = restTemplate.execute(URI.create(APIParams.HTTP_URI + url), httpMethod, requestCallback, responseExtractor);
			return fileInfo;
		}catch (HttpClientErrorException e) {
			log.error(">>>>> An exception occurred!", e);
			if (e.getRawStatusCode() == HttpStatus.UNAUTHORIZED.value()) {
				return null;
			} else if (e.getRawStatusCode() == HttpStatus.NOT_FOUND.value()) {
				return null;
			} else {
				return null;
			}
		} catch (Exception e) {
			log.error(">>>>> An exception occurred!", e);
			return null;
		}

	}
	
	public MsgRsp callAPIPass(String url, HttpMethod httpMethod, JSONRoot jsonRoot) throws Exception {
	
			MsgRsp rsp = null;
			MspResponseStatus responseStatus = null;
			try {
				HttpHeaders headers = new HttpHeaders();
				headers.setAccept(Arrays.asList(new MediaType[] { MediaType.APPLICATION_JSON }));
				headers.setContentType(MediaType.APPLICATION_JSON);
				headers.add(APIParams.API_LICENSE_KEY_NAME, APIParams.HTTP_LICENSEKEY);
				
				HttpEntity<JSONRoot> requestBody = new HttpEntity<>(jsonRoot, headers);
				ResponseEntity<MsgRsp> result = restTemplate.exchange(APIParams.HTTP_URI + url, httpMethod, requestBody, MsgRsp.class);
				if (result.getStatusCode() == HttpStatus.OK) {
					rsp = result.getBody();
				}
			} catch (HttpClientErrorException e) {
				if (e.getRawStatusCode() == HttpStatus.UNAUTHORIZED.value()) {
					responseStatus = new MspResponseStatus(e.getRawStatusCode(),
							"Yêu cầu chưa được áp dụng vì nó thiếu thông tin xác thực hợp lệ cho tài nguyên đích.");
					rsp = new MsgRsp(jsonRoot.getMsg().getMsgHeader());
					rsp.setResponseStatus(responseStatus);
				} else if (e.getRawStatusCode() == HttpStatus.NOT_FOUND.value()) {
					responseStatus = new MspResponseStatus(e.getRawStatusCode(), "Không tìm thấy hàm xử lý dữ liệu.");
					rsp = new MsgRsp(jsonRoot.getMsg().getMsgHeader());
					rsp.setResponseStatus(responseStatus);
				} else if (e.getRawStatusCode() == HttpStatus.FORBIDDEN.value()) {
					responseStatus = new MspResponseStatus(e.getRawStatusCode(), "API License key không hợp lệ. Vui lòng liên hệ Admin để biết thêm chi tiết.");
					rsp = new MsgRsp(jsonRoot.getMsg().getMsgHeader());
					rsp.setResponseStatus(responseStatus);
				}else {
					responseStatus = new MspResponseStatus(e.getRawStatusCode(), "Lỗi kết nối API...");
					rsp = new MsgRsp(jsonRoot.getMsg().getMsgHeader());
					rsp.setResponseStatus(responseStatus);
				}
			} catch (Exception e) {
				responseStatus = new MspResponseStatus(999, "Lỗi ngoại lệ...");
				rsp = new MsgRsp(jsonRoot.getMsg().getMsgHeader());
				rsp.setResponseStatus(responseStatus);
			}
			
			if(null == rsp) {
				responseStatus = new MspResponseStatus(999, "Không tìm thấy nội dung dữ liệu trả về");
				rsp = new MsgRsp(jsonRoot.getMsg().getMsgHeader());
				rsp.setResponseStatus(responseStatus);
			}
			
			return rsp;
		}
	
	//API GET TAXCODE
	public FileInfo callAPIGetTaxCode(String url, HttpMethod httpMethod, HashMap<String, String> mapInput) throws Exception{
		FileInfo fileInfo = null;
		
		try {
			RequestCallback requestCallback = request -> {
				request.getHeaders().add(APIParams.API_LICENSE_KEY_NAME, APIParams.HTTP_LICENSEKEY);
				request.getHeaders().add("Content-Type", "application/json");
				request.getHeaders().setAccept(Arrays.asList(MediaType.APPLICATION_OCTET_STREAM, MediaType.ALL));
				request.getBody().write(Json.serializer().toString(mapInput).getBytes());
			};
			
			ResponseExtractor<FileInfo> responseExtractor = rsp -> {
				return SerializationUtils.deserialize(rsp.getBody());
			};
			
			fileInfo = restTemplate.execute(URI.create(APIParams.HTTP_URI + url), httpMethod, requestCallback, responseExtractor);
			return fileInfo;
		}catch (HttpClientErrorException e) {
			log.error(">>>>> An exception occurred!", e);
			if (e.getRawStatusCode() == HttpStatus.UNAUTHORIZED.value()) {
				return null;
			} else if (e.getRawStatusCode() == HttpStatus.NOT_FOUND.value()) {
				return null;
			} else {
				return null;
			}
		} catch (Exception e) {
			log.error(">>>>> An exception occurred!", e);
			return null;
		}

	}

	public FileInfo callAPIGetFilePDF(String url, String tokenAuth, HttpMethod httpMethod, JSONRoot jsonRoot) throws Exception{
		FileInfo fileInfo = null;
		
		try {
			RequestCallback requestCallback = request -> {
				request.getHeaders().add(APIParams.API_LICENSE_KEY_NAME, APIParams.HTTP_LICENSEKEY);
				request.getHeaders().add(Constants.TOKEN_HEADER, tokenAuth);
				request.getHeaders().add("Content-Type", "application/json");
				request.getHeaders().setAccept(Arrays.asList(MediaType.APPLICATION_OCTET_STREAM, MediaType.ALL));
				request.getBody().write(Json.serializer().toString(jsonRoot).getBytes());
			};
			
			ResponseExtractor<FileInfo> responseExtractor = rsp -> {
				return SerializationUtils.deserialize(rsp.getBody());
			};
			
			fileInfo = restTemplate.execute(URI.create(APIParams.HTTP_URIPDF + url), httpMethod, requestCallback, responseExtractor);
			return fileInfo;
		}catch (HttpClientErrorException e) {
			log.error(">>>>> An exception occurred!", e);
			if (e.getRawStatusCode() == HttpStatus.UNAUTHORIZED.value()) {
				return null;
			} else if (e.getRawStatusCode() == HttpStatus.NOT_FOUND.value()) {
				return null;
			} else {
				return null;
			}
		} catch (Exception e) {
			log.error(">>>>> An exception occurred!", e);
			return null;
		}

	}


}
