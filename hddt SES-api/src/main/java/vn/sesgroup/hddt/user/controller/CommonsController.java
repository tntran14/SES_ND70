package vn.sesgroup.hddt.user.controller;

import java.io.File;
import java.io.FileOutputStream;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.SerializationUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.FileCopyUtils;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.multipart.MultipartHttpServletRequest;

import com.api.message.JSONRoot;
import com.api.message.MsgRsp;
import com.api.message.MspResponseStatus;

import vn.sesgroup.hddt.dto.FileInfo;
import vn.sesgroup.hddt.user.dao.CommonDAO;
import vn.sesgroup.hddt.utility.Commons;
import vn.sesgroup.hddt.utility.SystemParams;

@RestController
@RequestMapping(value = "/commons")
@Scope(value = WebApplicationContext.SCOPE_REQUEST)
public class CommonsController {
	@Autowired private CommonDAO dao;
	private Commons commons = new Commons();
	
	@RequestMapping(
		value = "/get-full-params", method = RequestMethod.POST
		, consumes = {MediaType.APPLICATION_JSON_VALUE } // MediaType.TEXT_PLAIN_VALUE,
		, produces = { MediaType.APPLICATION_JSON_VALUE }
	)
	public ResponseEntity<?> getFullParams(@RequestBody JSONRoot jsonRoot) throws Exception {
		HttpHeaders headers = new HttpHeaders();
		headers.add(HttpHeaders.CONTENT_TYPE, "application/json; charset=UTF-8");
		return ResponseEntity.ok().headers(headers).cacheControl(CacheControl.noCache()).body(dao.getFullParams(jsonRoot));
	}
	
	@RequestMapping(value = "/print-einvoice", method = RequestMethod.POST,
			consumes = {MediaType.APPLICATION_JSON_VALUE},		//MediaType.TEXT_PLAIN_VALUE, 
			produces = {MediaType.APPLICATION_OCTET_STREAM_VALUE})
	public ResponseEntity<?> printEinvoice(@RequestBody JSONRoot jsonRoot) throws Exception{
		FileInfo fileInfo = dao.printEInvoice(jsonRoot);
		
		HttpHeaders headers = new HttpHeaders();
		headers.add("content-disposition", "attachment; filename=" + "einvoice.pdf");
		headers.add("Content-Type", MediaType.APPLICATION_OCTET_STREAM_VALUE);
        
		return ResponseEntity.ok()
				.headers(headers)
				.cacheControl(CacheControl.noCache())
				.body(SerializationUtils.serialize(fileInfo));
	}
	@RequestMapping(value = "/print-einvoiceAll", method = RequestMethod.POST,
			consumes = {MediaType.APPLICATION_JSON_VALUE},		//MediaType.TEXT_PLAIN_VALUE, 
			produces = {MediaType.APPLICATION_OCTET_STREAM_VALUE})
	public ResponseEntity<?> printEinvoiceAll(@RequestBody JSONRoot jsonRoot) throws Exception{
		FileInfo fileInfo = dao.printEinvoiceAll(jsonRoot);
		
		HttpHeaders headers = new HttpHeaders();
		headers.add("content-disposition", "attachment; filename=" + "einvoice.pdf");
		headers.add("Content-Type", MediaType.APPLICATION_OCTET_STREAM_VALUE);
		
		return ResponseEntity.ok()
				.headers(headers)
				.cacheControl(CacheControl.noCache())
				.body(SerializationUtils.serialize(fileInfo));
	}
	
	@RequestMapping(value = "/print-einvoiceAllDB", method = RequestMethod.POST,
			consumes = {MediaType.APPLICATION_JSON_VALUE},		//MediaType.TEXT_PLAIN_VALUE, 
			produces = {MediaType.APPLICATION_OCTET_STREAM_VALUE})
	public ResponseEntity<?> printEinvoiceAllDB(@RequestBody JSONRoot jsonRoot) throws Exception{
		FileInfo fileInfo = dao.printEinvoiceAllDB(jsonRoot);
		
		HttpHeaders headers = new HttpHeaders();
		headers.add("content-disposition", "attachment; filename=" + "einvoice.pdf");
		headers.add("Content-Type", MediaType.APPLICATION_OCTET_STREAM_VALUE);
		
		return ResponseEntity.ok()
				.headers(headers)
				.cacheControl(CacheControl.noCache())
				.body(SerializationUtils.serialize(fileInfo));
	}
	
	@RequestMapping(value = "/einvoiceXml", method = RequestMethod.POST,
			consumes = {MediaType.APPLICATION_JSON_VALUE},		//MediaType.TEXT_PLAIN_VALUE, 
			produces = {MediaType.APPLICATION_OCTET_STREAM_VALUE})
	public ResponseEntity<?> einvoiceXml(@RequestBody JSONRoot jsonRoot) throws Exception{
		FileInfo fileInfo = dao.einvoiceXml(jsonRoot);
		
		HttpHeaders headers = new HttpHeaders();
		headers.add("content-disposition", "attachment; filename=" + "bill.data");
		headers.add("Content-Type", MediaType.APPLICATION_OCTET_STREAM_VALUE);
        
		return ResponseEntity.ok()
				.headers(headers)
				.cacheControl(CacheControl.noCache())
				.body(SerializationUtils.serialize(fileInfo));
	}
	//Print PXK
	@RequestMapping(value = "/print-export", method = RequestMethod.POST,
			consumes = {MediaType.APPLICATION_JSON_VALUE},		//MediaType.TEXT_PLAIN_VALUE, 
			produces = {MediaType.APPLICATION_OCTET_STREAM_VALUE})
	public ResponseEntity<?> printExport(@RequestBody JSONRoot jsonRoot) throws Exception{
		FileInfo fileInfo = dao.printExport(jsonRoot);
		
		HttpHeaders headers = new HttpHeaders();
		headers.add("content-disposition", "attachment; filename=" + "export.pdf");
		headers.add("Content-Type", MediaType.APPLICATION_OCTET_STREAM_VALUE);
        
		return ResponseEntity.ok()
				.headers(headers)
				.cacheControl(CacheControl.noCache())
				.body(SerializationUtils.serialize(fileInfo));
	}
	
	@RequestMapping(
		value = "/auto-complete-products", method = RequestMethod.POST
		, consumes = {MediaType.APPLICATION_JSON_VALUE } // MediaType.TEXT_PLAIN_VALUE,
		, produces = { MediaType.APPLICATION_JSON_VALUE }
	)
	public ResponseEntity<?> getAutoCompleteProducts(@RequestBody JSONRoot jsonRoot) throws Exception {
		HttpHeaders headers = new HttpHeaders();
		headers.add(HttpHeaders.CONTENT_TYPE, "application/json; charset=UTF-8");
		return ResponseEntity.ok().headers(headers).cacheControl(CacheControl.noCache()).body(dao.getAutoCompleteProducts(jsonRoot));
	}
	
	@RequestMapping(value = "/processUploadTmpFile", method = RequestMethod.POST, consumes = {
			MediaType.MULTIPART_FORM_DATA_VALUE }, produces = { MediaType.APPLICATION_JSON_VALUE })
	public ResponseEntity<?> processUploadTmpFile(HttpServletRequest req,
			MultipartHttpServletRequest multipartHttpServletRequest,
			@RequestParam(name = "IssuerId", defaultValue = "") String issuerId) throws Exception {
		MsgRsp rsp = new MsgRsp();

		/* TAO THU MUC NEU CHUA TON TAI */
		File file = new File(SystemParams.DIR_TEMPORARY, issuerId);
		if (!file.exists())
			file.mkdirs();

		ArrayList<HashMap<String, String>> files = new ArrayList<HashMap<String, String>>();
		HashMap<String, String> hFile = null;

		String originalFilename = "";
		String fileNameStore = "";
		String fileNameExt = "";
		List<String> requestKeys = new ArrayList<String>();
		multipartHttpServletRequest.getFileNames().forEachRemaining(requestKeys::add);

		for (String multiPartFile : requestKeys) {
			originalFilename = multipartHttpServletRequest.getFile(multiPartFile).getOriginalFilename();
			fileNameExt = FilenameUtils.getExtension(originalFilename);
			fileNameExt = "".equals(fileNameExt) ? "" : "." + fileNameExt;
			fileNameStore = commons.convertLocalDateTimeToString(LocalDateTime.now(), "yyyyMMddHHmmss") + "-" + commons.csRandomAlphaNumbericString(5) + fileNameExt;

			FileCopyUtils.copy(multipartHttpServletRequest.getFile(multiPartFile).getBytes(),
					new FileOutputStream(new File(file.getAbsolutePath(), fileNameStore)));

			hFile = new HashMap<String, String>();
			hFile.put("OriginalFilename", originalFilename);
			hFile.put("SystemFilename", fileNameStore);
			files.add(hFile);
		}

		MspResponseStatus responseStatus = new MspResponseStatus(0, "SUCCESS");
		rsp.setResponseStatus(responseStatus);
		rsp.setObjData(files);

		HttpHeaders headers = new HttpHeaders();
		headers.add(HttpHeaders.CONTENT_TYPE, "application/json; charset=UTF-8");
		return ResponseEntity.ok().headers(headers).cacheControl(CacheControl.noCache()).body(rsp);
	}
	
	@RequestMapping(value = "/list-search-customer", method = RequestMethod.POST,
			consumes = {MediaType.APPLICATION_JSON_VALUE},		//MediaType.TEXT_PLAIN_VALUE, 
			produces = {MediaType.APPLICATION_JSON_VALUE})
	public ResponseEntity<?> listSearchCustomer(@RequestBody JSONRoot jsonRoot) throws Exception{
		MsgRsp rsp = dao.listSearchCustomer(jsonRoot);
		HttpHeaders headers = new HttpHeaders();
		headers.add(HttpHeaders.CONTENT_TYPE, "application/json; charset=UTF-8");
		return ResponseEntity.ok()
				.headers(headers)
				.cacheControl(CacheControl.noCache())
				.body(rsp);
	}
	
	@RequestMapping(value = "/list-search-customer-update", method = RequestMethod.POST,
			consumes = {MediaType.APPLICATION_JSON_VALUE},		//MediaType.TEXT_PLAIN_VALUE, 
			produces = {MediaType.APPLICATION_JSON_VALUE})
	public ResponseEntity<?> listSearchCustomerUpdate(@RequestBody JSONRoot jsonRoot) throws Exception{
		MsgRsp rsp = dao.listSearchCustomerUpdate(jsonRoot);
		HttpHeaders headers = new HttpHeaders();
		headers.add(HttpHeaders.CONTENT_TYPE, "application/json; charset=UTF-8");
		return ResponseEntity.ok()
				.headers(headers)
				.cacheControl(CacheControl.noCache())
				.body(rsp);
	}
	
	@RequestMapping(value = "/list-einvoice-signed", method = RequestMethod.POST,
			consumes = {MediaType.APPLICATION_JSON_VALUE},		//MediaType.TEXT_PLAIN_VALUE, 
			produces = {MediaType.APPLICATION_JSON_VALUE})
	public ResponseEntity<?> listEInvoiceSigned(@RequestBody JSONRoot jsonRoot) throws Exception{
		MsgRsp rsp = dao.listEInvoiceSigned(jsonRoot);
		HttpHeaders headers = new HttpHeaders();
		headers.add(HttpHeaders.CONTENT_TYPE, "application/json; charset=UTF-8");
		return ResponseEntity.ok()
				.headers(headers)
				.cacheControl(CacheControl.noCache())
				.body(rsp);
	}
	@RequestMapping(value = "/viewpdf", method = RequestMethod.POST,
			consumes = {MediaType.APPLICATION_JSON_VALUE},		//MediaType.TEXT_PLAIN_VALUE, 
			produces = {MediaType.APPLICATION_OCTET_STREAM_VALUE})
	public ResponseEntity<?> viewpdf(@RequestBody JSONRoot jsonRoot) throws Exception{
		FileInfo fileInfo = dao.viewpdf(jsonRoot);
		
		HttpHeaders headers = new HttpHeaders();
		headers.add("content-disposition", "attachment; filename=" + "viewpdf.pdf");
		headers.add("Content-Type", MediaType.APPLICATION_OCTET_STREAM_VALUE);
        
		return ResponseEntity.ok()
				.headers(headers)
				.cacheControl(CacheControl.noCache())
				.body(SerializationUtils.serialize(fileInfo));
	}
	
	@RequestMapping(value = "/viewpdftncn", method = RequestMethod.POST,
			consumes = {MediaType.APPLICATION_JSON_VALUE},		//MediaType.TEXT_PLAIN_VALUE, 
			produces = {MediaType.APPLICATION_OCTET_STREAM_VALUE})
	public ResponseEntity<?> viewpdftncn(@RequestBody JSONRoot jsonRoot) throws Exception{
		FileInfo fileInfo = dao.viewpdftncn(jsonRoot);
		
		HttpHeaders headers = new HttpHeaders();
		headers.add("content-disposition", "attachment; filename=" + "viewpdf.pdf");
		headers.add("Content-Type", MediaType.APPLICATION_OCTET_STREAM_VALUE);
        
		return ResponseEntity.ok()
				.headers(headers)
				.cacheControl(CacheControl.noCache())
				.body(SerializationUtils.serialize(fileInfo));
	}
	@RequestMapping(value = "/viewpdfcttncn", method = RequestMethod.POST,
			consumes = {MediaType.APPLICATION_JSON_VALUE},		//MediaType.TEXT_PLAIN_VALUE, 
			produces = {MediaType.APPLICATION_OCTET_STREAM_VALUE})
	public ResponseEntity<?> viewpdfcttncn(@RequestBody JSONRoot jsonRoot) throws Exception{
		FileInfo fileInfo = dao.viewpdfcttncn(jsonRoot);
		
		HttpHeaders headers = new HttpHeaders();
		headers.add("content-disposition", "attachment; filename=" + "viewpdf.pdf");
		headers.add("Content-Type", MediaType.APPLICATION_OCTET_STREAM_VALUE);
        
		return ResponseEntity.ok()
				.headers(headers)
				.cacheControl(CacheControl.noCache())
				.body(SerializationUtils.serialize(fileInfo));
	}
	@RequestMapping(value = "/print04", method = RequestMethod.POST,
			consumes = {MediaType.APPLICATION_JSON_VALUE},		//MediaType.TEXT_PLAIN_VALUE, 
			produces = {MediaType.APPLICATION_OCTET_STREAM_VALUE})
	public ResponseEntity<?> print04(@RequestBody JSONRoot jsonRoot) throws Exception{
		FileInfo fileInfo = dao.print04(jsonRoot);
		
		HttpHeaders headers = new HttpHeaders();
		headers.add("content-disposition", "attachment; filename=" + "print04.pdf");
		headers.add("Content-Type", MediaType.APPLICATION_OCTET_STREAM_VALUE);
        
		return ResponseEntity.ok()
				.headers(headers)
				.cacheControl(CacheControl.noCache())
				.body(SerializationUtils.serialize(fileInfo));
	}
	//Print PXKDL
	@RequestMapping(value = "/print-agent", method = RequestMethod.POST,
			consumes = {MediaType.APPLICATION_JSON_VALUE},		//MediaType.TEXT_PLAIN_VALUE, 
			produces = {MediaType.APPLICATION_OCTET_STREAM_VALUE})
	public ResponseEntity<?> printAgent(@RequestBody JSONRoot jsonRoot) throws Exception{
		FileInfo fileInfo = dao.printAgent(jsonRoot);
		
		HttpHeaders headers = new HttpHeaders();
		headers.add("content-disposition", "attachment; filename=" + "agent.pdf");
		headers.add("Content-Type", MediaType.APPLICATION_OCTET_STREAM_VALUE);
        
		return ResponseEntity.ok()
				.headers(headers)
				.cacheControl(CacheControl.noCache())
				.body(SerializationUtils.serialize(fileInfo));
	}
	//Print Hóa đơn bán hàng
	@RequestMapping(value = "/print-einvoice1", method = RequestMethod.POST,
			consumes = {MediaType.APPLICATION_JSON_VALUE},		//MediaType.TEXT_PLAIN_VALUE, 
			produces = {MediaType.APPLICATION_OCTET_STREAM_VALUE})
	public ResponseEntity<?> printEInvoiceBH(@RequestBody JSONRoot jsonRoot) throws Exception{
		FileInfo fileInfo = dao.printEInvoiceBH(jsonRoot);
		
		HttpHeaders headers = new HttpHeaders();
		headers.add("content-disposition", "attachment; filename=" + "einvoiceBH.pdf");
		headers.add("Content-Type", MediaType.APPLICATION_OCTET_STREAM_VALUE);
        
		return ResponseEntity.ok()
				.headers(headers)
				.cacheControl(CacheControl.noCache())
				.body(SerializationUtils.serialize(fileInfo));
	}
	
	@RequestMapping(value = "/getXml", method = RequestMethod.POST,
			consumes = {MediaType.APPLICATION_JSON_VALUE},		//MediaType.TEXT_PLAIN_VALUE, 
			produces = {MediaType.APPLICATION_OCTET_STREAM_VALUE})
	public ResponseEntity<?> getXml(@RequestBody JSONRoot jsonRoot) throws Exception{
		FileInfo fileInfo = dao.getXml(jsonRoot);
		
		HttpHeaders headers = new HttpHeaders();
		headers.add("content-disposition", "attachment; filename=" + "bill.data");
		headers.add("Content-Type", MediaType.APPLICATION_OCTET_STREAM_VALUE);
        
		return ResponseEntity.ok()
				.headers(headers)
				.cacheControl(CacheControl.noCache())
				.body(SerializationUtils.serialize(fileInfo));
	}
	
	
	
	@RequestMapping(value = "/einvoice1Xml", method = RequestMethod.POST,
			consumes = {MediaType.APPLICATION_JSON_VALUE},		//MediaType.TEXT_PLAIN_VALUE, 
			produces = {MediaType.APPLICATION_OCTET_STREAM_VALUE})
	public ResponseEntity<?> einvoice1Xml(@RequestBody JSONRoot jsonRoot) throws Exception{
		FileInfo fileInfo = dao.einvoice1Xml(jsonRoot);
		
		HttpHeaders headers = new HttpHeaders();
		headers.add("content-disposition", "attachment; filename=" + "bill.data");
		headers.add("Content-Type", MediaType.APPLICATION_OCTET_STREAM_VALUE);
        
		return ResponseEntity.ok()
				.headers(headers)
				.cacheControl(CacheControl.noCache())
				.body(SerializationUtils.serialize(fileInfo));
	}
	
	
	@RequestMapping(value = "/print-einvoice1All", method = RequestMethod.POST,
			consumes = {MediaType.APPLICATION_JSON_VALUE},		//MediaType.TEXT_PLAIN_VALUE, 
			produces = {MediaType.APPLICATION_OCTET_STREAM_VALUE})
	public ResponseEntity<?> printEinvoice1All(@RequestBody JSONRoot jsonRoot) throws Exception{
		FileInfo fileInfo = dao.printEinvoice1All(jsonRoot);
		
		HttpHeaders headers = new HttpHeaders();
		headers.add("content-disposition", "attachment; filename=" + "einvoice.pdf");
		headers.add("Content-Type", MediaType.APPLICATION_OCTET_STREAM_VALUE);
		
		return ResponseEntity.ok()
				.headers(headers)
				.cacheControl(CacheControl.noCache())
				.body(SerializationUtils.serialize(fileInfo));
	}
	//
	
	@RequestMapping(value = "/downLoadFile", method = RequestMethod.POST,
			consumes = {MediaType.APPLICATION_JSON_VALUE},		//MediaType.TEXT_PLAIN_VALUE, 
			produces = {MediaType.APPLICATION_OCTET_STREAM_VALUE})
	public ResponseEntity<?> downLoadFile(@RequestBody JSONRoot jsonRoot) throws Exception{
		FileInfo fileInfo = dao.downLoadFile(jsonRoot);
		
		HttpHeaders headers = new HttpHeaders();
		headers.add("content-disposition", "attachment; filename=" + "bill.data");
		headers.add("Content-Type", MediaType.APPLICATION_OCTET_STREAM_VALUE);
        
		return ResponseEntity.ok()
				.headers(headers)
				.cacheControl(CacheControl.noCache())
				.body(SerializationUtils.serialize(fileInfo));
	}
	
	@RequestMapping(value = "/getXmlThue", method = RequestMethod.POST,
			consumes = {MediaType.APPLICATION_JSON_VALUE},		//MediaType.TEXT_PLAIN_VALUE, 
			produces = {MediaType.APPLICATION_OCTET_STREAM_VALUE})
	public ResponseEntity<?> getXmlThue(@RequestBody JSONRoot jsonRoot) throws Exception{
		FileInfo fileInfo = dao.getXmlThue(jsonRoot);
		
		HttpHeaders headers = new HttpHeaders();
		headers.add("content-disposition", "attachment; filename=" + "bill.data");
		headers.add("Content-Type", MediaType.APPLICATION_OCTET_STREAM_VALUE);
        
		return ResponseEntity.ok()
				.headers(headers)
				.cacheControl(CacheControl.noCache())
				.body(SerializationUtils.serialize(fileInfo));
	}

	@RequestMapping(value = "/getFullRightAdmin", method = RequestMethod.POST, consumes = {
			MediaType.APPLICATION_JSON_VALUE }, // MediaType.TEXT_PLAIN_VALUE,
			produces = { MediaType.APPLICATION_JSON_VALUE })
	public ResponseEntity<?> getFullRightAdmin(@RequestBody JSONRoot jsonRoot) throws Exception {
		MsgRsp rsp = dao.getFullRightAdmin(jsonRoot);

		HttpHeaders headers = new HttpHeaders();
		headers.add(HttpHeaders.CONTENT_TYPE, "application/json; charset=UTF-8");
		return ResponseEntity.ok().headers(headers).cacheControl(CacheControl.noCache()).body(rsp);
	}
	
	@RequestMapping(value = "/exportXml", method = RequestMethod.POST,
			consumes = {MediaType.APPLICATION_JSON_VALUE},		//MediaType.TEXT_PLAIN_VALUE, 
			produces = {MediaType.APPLICATION_OCTET_STREAM_VALUE})
	public ResponseEntity<?> exportXml(@RequestBody JSONRoot jsonRoot) throws Exception{
		FileInfo fileInfo = dao.exportXml(jsonRoot);
		
		HttpHeaders headers = new HttpHeaders();
		headers.add("content-disposition", "attachment; filename=" + "bill.data");
		headers.add("Content-Type", MediaType.APPLICATION_OCTET_STREAM_VALUE);
        
		return ResponseEntity.ok()
				.headers(headers)
				.cacheControl(CacheControl.noCache())
				.body(SerializationUtils.serialize(fileInfo));
	}
		//IN PDF HANG LOAT
	@RequestMapping(value = "/exportPDF", method = RequestMethod.POST,
			consumes = {MediaType.APPLICATION_JSON_VALUE},		//MediaType.TEXT_PLAIN_VALUE, 
			produces = {MediaType.APPLICATION_OCTET_STREAM_VALUE})
	public ResponseEntity<?> exportPDF(@RequestBody JSONRoot jsonRoot) throws Exception{
		FileInfo fileInfo = dao.exportPDF(jsonRoot);
		
		HttpHeaders headers = new HttpHeaders();
		headers.add("content-disposition", "attachment; filename=" + "bill.data");
		headers.add("Content-Type", MediaType.APPLICATION_OCTET_STREAM_VALUE);
        
		return ResponseEntity.ok()
				.headers(headers)
				.cacheControl(CacheControl.noCache())
				.body(SerializationUtils.serialize(fileInfo));
	}
	
	@RequestMapping(value = "/print-exportAll", method = RequestMethod.POST,
			consumes = {MediaType.APPLICATION_JSON_VALUE},		//MediaType.TEXT_PLAIN_VALUE, 
			produces = {MediaType.APPLICATION_OCTET_STREAM_VALUE})
	public ResponseEntity<?> printExportAll(@RequestBody JSONRoot jsonRoot) throws Exception{
		FileInfo fileInfo = dao.printExportAll(jsonRoot);
		
		HttpHeaders headers = new HttpHeaders();
		headers.add("content-disposition", "attachment; filename=" + "einvoice.pdf");
		headers.add("Content-Type", MediaType.APPLICATION_OCTET_STREAM_VALUE);
		
		return ResponseEntity.ok()
				.headers(headers)
				.cacheControl(CacheControl.noCache())
				.body(SerializationUtils.serialize(fileInfo));
	}
	
	//PHAN QUYEN ADMIN
	@RequestMapping(value = "/getFullRightAdminManager", method = RequestMethod.POST, consumes = {
			MediaType.APPLICATION_JSON_VALUE }, // MediaType.TEXT_PLAIN_VALUE,
			produces = { MediaType.APPLICATION_JSON_VALUE })
	public ResponseEntity<?> getFullRightAdminManager(@RequestBody JSONRoot jsonRoot) throws Exception {
		MsgRsp rsp = dao.getFullRightAdminManager(jsonRoot);

		HttpHeaders headers = new HttpHeaders();
		headers.add(HttpHeaders.CONTENT_TYPE, "application/json; charset=UTF-8");
		return ResponseEntity.ok().headers(headers).cacheControl(CacheControl.noCache()).body(rsp);
	}
	
	
	//PHAN QUYEN ADMIN
		@RequestMapping(value = "/detailMaSoThue", method = RequestMethod.POST, consumes = {
				MediaType.APPLICATION_JSON_VALUE }, // MediaType.TEXT_PLAIN_VALUE,
				produces = { MediaType.APPLICATION_JSON_VALUE })
		public ResponseEntity<?> detailMaSoThue(@RequestBody JSONRoot jsonRoot) throws Exception {
			MsgRsp rsp = dao.detailMaSoThue(jsonRoot);

			HttpHeaders headers = new HttpHeaders();
			headers.add(HttpHeaders.CONTENT_TYPE, "application/json; charset=UTF-8");
			return ResponseEntity.ok().headers(headers).cacheControl(CacheControl.noCache()).body(rsp);
		}
		
		@RequestMapping(value = "/view-pdf-tiepnhan", method = RequestMethod.POST,
				consumes = {MediaType.APPLICATION_JSON_VALUE},		//MediaType.TEXT_PLAIN_VALUE, 
				produces = {MediaType.APPLICATION_OCTET_STREAM_VALUE})
		public ResponseEntity<?> viewPdfTiepnhan(@RequestBody JSONRoot jsonRoot) throws Exception{
			FileInfo fileInfo = dao.viewPdfTiepnhan(jsonRoot);
			
			HttpHeaders headers = new HttpHeaders();
			headers.add("content-disposition", "attachment; filename=" + "print04.pdf");
			headers.add("Content-Type", MediaType.APPLICATION_OCTET_STREAM_VALUE);
	        
			return ResponseEntity.ok()
					.headers(headers)
					.cacheControl(CacheControl.noCache())
					.body(SerializationUtils.serialize(fileInfo));
		}
		
		@RequestMapping(value = "/cttncnXml", method = RequestMethod.POST,
				consumes = {MediaType.APPLICATION_JSON_VALUE},		//MediaType.TEXT_PLAIN_VALUE, 
				produces = {MediaType.APPLICATION_OCTET_STREAM_VALUE})
		public ResponseEntity<?> cttncnXml(@RequestBody JSONRoot jsonRoot) throws Exception{
			FileInfo fileInfo = dao.cttncnXml(jsonRoot);
			
			HttpHeaders headers = new HttpHeaders();
			headers.add("content-disposition", "attachment; filename=" + "bill.data");
			headers.add("Content-Type", MediaType.APPLICATION_OCTET_STREAM_VALUE);
	        
			return ResponseEntity.ok()
					.headers(headers)
					.cacheControl(CacheControl.noCache())
					.body(SerializationUtils.serialize(fileInfo));
		}
		
		@RequestMapping(
				value = "/saveDataToBase64", method = RequestMethod.POST
				, consumes = {MediaType.APPLICATION_JSON_VALUE } // MediaType.TEXT_PLAIN_VALUE,
				, produces = { MediaType.APPLICATION_JSON_VALUE }
			)
			public ResponseEntity<?> saveDataToBase64(@RequestBody JSONRoot jsonRoot) throws Exception {
				HttpHeaders headers = new HttpHeaders();
				headers.add(HttpHeaders.CONTENT_TYPE, "application/json; charset=UTF-8");
				return ResponseEntity.ok().headers(headers).cacheControl(CacheControl.noCache()).body(dao.saveDataToBase64(jsonRoot));
			}
			
		//print einvoice mtt
		
		@RequestMapping(value = "/print-einvoice_mtt", method = RequestMethod.POST,
				consumes = {MediaType.APPLICATION_JSON_VALUE},		//MediaType.TEXT_PLAIN_VALUE, 
				produces = {MediaType.APPLICATION_OCTET_STREAM_VALUE})
		public ResponseEntity<?> printEinvoiceMTT(@RequestBody JSONRoot jsonRoot) throws Exception{
			FileInfo fileInfo = dao.printEInvoiceMTT(jsonRoot);
			
			HttpHeaders headers = new HttpHeaders();
			headers.add("content-disposition", "attachment; filename=" + "einvoice.pdf");
			headers.add("Content-Type", MediaType.APPLICATION_OCTET_STREAM_VALUE);
	        
			return ResponseEntity.ok()
					.headers(headers)
					.cacheControl(CacheControl.noCache())
					.body(SerializationUtils.serialize(fileInfo));
		}
		
		@RequestMapping(value = "/list-einvoice_mtt-signed", method = RequestMethod.POST,
				consumes = {MediaType.APPLICATION_JSON_VALUE},		//MediaType.TEXT_PLAIN_VALUE, 
				produces = {MediaType.APPLICATION_JSON_VALUE})
		public ResponseEntity<?> listEInvoiceMTTSigned(@RequestBody JSONRoot jsonRoot) throws Exception{
			MsgRsp rsp = dao.listEInvoiceMTTSigned(jsonRoot);
			HttpHeaders headers = new HttpHeaders();
			headers.add(HttpHeaders.CONTENT_TYPE, "application/json; charset=UTF-8");
			return ResponseEntity.ok()
					.headers(headers)
					.cacheControl(CacheControl.noCache())
					.body(rsp);
		}
		
		@RequestMapping(value = "/print04_mtt", method = RequestMethod.POST,
				consumes = {MediaType.APPLICATION_JSON_VALUE},		//MediaType.TEXT_PLAIN_VALUE, 
				produces = {MediaType.APPLICATION_OCTET_STREAM_VALUE})
		public ResponseEntity<?> print04_mtt(@RequestBody JSONRoot jsonRoot) throws Exception{
			FileInfo fileInfo = dao.print04_mtt(jsonRoot);
			
			HttpHeaders headers = new HttpHeaders();
			headers.add("content-disposition", "attachment; filename=" + "print04.pdf");
			headers.add("Content-Type", MediaType.APPLICATION_OCTET_STREAM_VALUE);
	        
			return ResponseEntity.ok()
					.headers(headers)
					.cacheControl(CacheControl.noCache())
					.body(SerializationUtils.serialize(fileInfo));
		}
}
