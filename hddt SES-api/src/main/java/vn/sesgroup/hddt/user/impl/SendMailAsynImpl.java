package vn.sesgroup.hddt.user.impl;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import org.bson.Document;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import vn.sesgroup.hddt.dto.MailConfig;
import vn.sesgroup.hddt.user.dao.AbstractDAO;
import vn.sesgroup.hddt.user.dao.SendMailAsyncDAO;
import vn.sesgroup.hddt.user.service.JPUtils;
import vn.sesgroup.hddt.user.service.TCTNService;
import vn.sesgroup.hddt.utility.Constants;
import vn.sesgroup.hddt.utility.MailUtils;
import vn.sesgroup.hddt.utility.SystemParams;


@Repository
@Transactional
public class SendMailAsynImpl extends AbstractDAO implements SendMailAsyncDAO {
	@Autowired
	MongoTemplate mongoTemplate;
	@Autowired
	TCTNService tctnService;
	private MailUtils mailUtils = new MailUtils();
	@Autowired
	JPUtils jpUtils;
	@Override
	public void sendMailInvoice(MailConfig mailConfig, List<String> listIDSendMail) throws Exception {
		
		for(int h = 0; h< listIDSendMail.size(); h++)
		{
			Document docTmp =null;
			String _title = "";
			String _content = "";
			String _email = "";
			List<String> listFiles = new ArrayList<>();
			List<String> listNames = new ArrayList<>();
			String _id = listIDSendMail.get(h);
			ObjectId objectId = null;
			try {
				objectId = new ObjectId(_id);
			}catch(Exception e) {}
		Document docFind = new Document("_id", objectId);
		Iterable<Document>	cursor = mongoTemplate.getCollection("EInvoice").find(docFind);
		Iterator<Document> iter	 = cursor.iterator();
		if(iter.hasNext()) {
			docTmp = iter.next();
		}
		String mailNhan = "";
		 mailNhan = docTmp.getEmbedded(Arrays.asList("EInvoiceDetail", "NDHDon", "NMua","DCTDTu"),"");
		if(!mailNhan.equals("")) {
		String Taxcode = docTmp.getEmbedded(Arrays.asList("EInvoiceDetail", "NDHDon", "NBan","MST"),"");
		String Name = docTmp.getEmbedded(Arrays.asList("EInvoiceDetail", "NDHDon", "NBan","Ten"),"");
		int shd = docTmp.getEmbedded(Arrays.asList("EInvoiceDetail", "TTChung", "SHDon"),0);
			_title = Taxcode+ " " + Name + " Thông báo phát hành Hóa đơn điện tử" +  " - Số HĐ " +shd;
			String nban = docTmp.getEmbedded(Arrays.asList("EInvoiceDetail", "NDHDon", "NBan","Ten"),"");
			String mailNhanCC = docTmp.getEmbedded(Arrays.asList("EInvoiceDetail", "NDHDon", "NMua","DCTDTuCC"),"");
			String nmua = docTmp.getEmbedded(Arrays.asList("EInvoiceDetail", "NDHDon", "NMua","Ten"),"");
			String ms = docTmp.getEmbedded(Arrays.asList("EInvoiceDetail", "TTChung", "KHMSHDon"),"")+docTmp.getEmbedded(Arrays.asList("EInvoiceDetail", "TTChung", "KHHDon"),"");
		
			String url = docTmp.getEmbedded(Arrays.asList("PramLink", "LinkPortal"), "");
			String secretCode = docTmp.getEmbedded(Arrays.asList("SecureKey"),"");
	
			StringBuilder sb = new StringBuilder();
			sb.setLength(0);
			sb.append("<p><span style='font-family: Times New Roman;font-size: 13px;'>Kính gửi: <label style='font-weight: bold;'>" + ("".equals(nmua)? "Quý khách hàng": nmua) + "</label><o:p></o:p></span></p>\n");
			sb.append("<p><span style='font-family: Times New Roman;font-size: 13px;'><label style='font-weight: bold;'>" + nban + "</label> xin chân thành cảm ơn Quý đơn vị đã tin tưởng và sử dụng sản phẩm/dịch vụ của chúng tôi!</span></p>\n");
			sb.append("<p><span style='font-family: Times New Roman;font-size: 13px;'>Chúng tôi vừa phát hành hóa đơn điện tử đến Quý đơn vị với thông tin như sau:</span></p>\n");
			sb.append("<p><span style='font-family: Times New Roman;font-size: 13px;'>1.  Số hoá đơn:  " + shd + "</span></p>\n");
			sb.append("<p><span style='font-family: Times New Roman;font-size: 13px;'>2.  Mẫu hoá đơn: " + ms + "</span></p>\n");
			sb.append("<p><span style='font-family: Times New Roman;font-size: 13px;'>3.  Link truy cập: <a target='_blank' href='" + url + "'>" + url + "</a></span></p>\n");
			sb.append("<p><span style='font-family: Times New Roman;font-size: 13px;'>4.  Mã bảo mật: <a target='_blank' href='" + url + "'>" + secretCode + "</a></span></p>\n");
			sb.append("<p><span style='font-family: Times New Roman;font-size: 13px;'>Quý đơn vị  vui lòng kiểm tra lại thông tin và lưu trữ hoá đơn.</span></p>\n");
			sb.append("<p><span style='font-family: Times New Roman;font-size: 13px;'>Trân trọng kính chào!</span></p>");
			sb.append("<hr style='margin: 5px 0 5px 0;'>");
			sb.append("<p style='margin-bottom: 3px;'><span style='font-family: Times New Roman;font-size: 13px;color:red;font-weight: bold;'>QUÝ CÔNG TY VUI LÒNG KHÔNG REPLY EMAIL NÀY!</span></p>");
			sb.append("<p style='margin-bottom: 0px;'><span style='font-family: Times New Roman;font-size: 13px;'><label style='font-weight: bold;'>" + nban+ "</label><o:p></o:p></span></p>");
			sb.append("<p><span style='font-family: Times New Roman;font-size: 13px;'>" + docTmp.getEmbedded(Arrays.asList("EInvoiceDetail", "NDHDon", "NBan","DChi"),"") + "</span></p>\n");
			
			
			 _email=mailNhan ;
			 if(mailNhanCC.equals("")) {
				 _email += ","+mailNhanCC;
			 }
			_content = sb.toString();
	
		String dir = docTmp.getString("Dir");
		String signStatusCode = docTmp.get("SignStatusCode", "");
		String eInvoiceStatus = docTmp.get("EInvoiceStatus", "");
		String secureKey = docTmp.get("SecureKey", "");
		String MCCQT = docTmp.get("MCCQT", "");
		String fileName = _id + ".xml";
		File file = null;
		String CheckView = "";
		String fileNameXML = _id + "_signed.xml";
		String fileNamePDF = _id + ".pdf";
		if (Constants.INVOICE_STATUS.DELETED.equals(eInvoiceStatus))
			fileNamePDF = _id + "-deleted.pdf";
		file = new File(dir, fileNamePDF);
		/* KIEM TRA XEM CO FILE PDF CHUA; NEU CHUA CO THI TAO FILE PDF */
		String mauHD = docTmp.getEmbedded(Arrays.asList("EInvoiceDetail", "TTChung", "KHMSHDon"), "")
				+ docTmp.getEmbedded(Arrays.asList("EInvoiceDetail", "TTChung", "KHHDon"), "");
		String soHD = commons
				.formatNumberBillInvoice(docTmp.getEmbedded(Arrays.asList("EInvoiceDetail", "TTChung", "SHDon"), 0));
		
		String mausohd = docTmp.getEmbedded(Arrays.asList("EInvoiceDetail", "TTChung", "MauSoHD"), "");
		try {
			objectId = new ObjectId(mausohd);
		}catch(Exception e) {}
		List<Document> pipeline = null;
		pipeline = new ArrayList<Document>();
		pipeline.add(new Document("$lookup", new Document("from", "DMMauSoKyHieu")
				.append("pipeline", Arrays.asList(new Document("$match", new Document("_id", objectId))))
				.append("as", "DMMauSoKyHieu")));
		pipeline.add(new Document("$unwind",
				new Document("path", "$DMMauSoKyHieu").append("preserveNullAndEmptyArrays", true)));
		pipeline.add(
				new Document("$lookup", 
					new Document("from", "PramLink")
					.append("pipeline", 
						Arrays.asList(
							new Document("$match", 
								new Document("$expr", 
										new Document("IsDelete", false)
								)
							)
						)	
					)
					.append("as", "PramLink")
				)
			);
			pipeline.add(
				new Document("$unwind", new Document("path", "$PramLink").append("preserveNullAndEmptyArrays", true))
			);	
		cursor = mongoTemplate.getCollection("EInvoice").aggregate(pipeline);
		iter = cursor.iterator();
		if (iter.hasNext()) {
			docTmp = iter.next();
		}
		String 	link = docTmp.getEmbedded(Arrays.asList("PramLink", "LinkPortal"), "");
		
		if (!file.exists() && docTmp.get("DMMauSoKyHieu") != null) {
			String MST = docTmp.getEmbedded(Arrays.asList("EInvoiceDetail", "NDHDon", "NBan", "MST"), "");
			String ImgLogo = docTmp.getEmbedded(Arrays.asList("DMMauSoKyHieu", "Templates", "ImgLogo"), "");
			String ImgBackground = docTmp.getEmbedded(Arrays.asList("DMMauSoKyHieu", "Templates", "ImgBackground"), "");
			String ImgQA = docTmp.getEmbedded(Arrays.asList("DMMauSoKyHieu", "Templates", "ImgQA"), "");
			String ImgVien = docTmp.getEmbedded(Arrays.asList("DMMauSoKyHieu", "Templates", "ImgVien"), "");
			String ParamUSD = docTmp.getEmbedded(Arrays.asList("EInvoiceDetail", "TToan", "ParamUSD"), "");

			fileName = _id + ".xml";
			if ("SIGNED".equals(signStatusCode) && "COMPLETE".equals(eInvoiceStatus) && !"".equals(MCCQT)) {
				fileName = _id + "_" + MCCQT + ".xml";
			} else {
				if ("SIGNED".equals(signStatusCode)) {
					fileName = _id + "_signed.xml";
				}
			}
			file = new File(dir, fileName);
			if (file.exists() && file.isFile()) {
				org.w3c.dom.Document doc = commons.fileToDocument(file);
				String fileNameJP = docTmp.getEmbedded(Arrays.asList("DMMauSoKyHieu", "Templates", "FileName"), "");
				int numberRowInPage = docTmp.getEmbedded(Arrays.asList("DMMauSoKyHieu", "Templates", "RowsInPage"), 20);
				int numberRowInPageMultiPage = docTmp
						.getEmbedded(Arrays.asList("DMMauSoKyHieu", "Templates", "RowInPageMultiPage"), 26);
				int numberCharsInRow = docTmp.getEmbedded(Arrays.asList("DMMauSoKyHieu", "Templates", "CharsInRow"),
						50);
				boolean isDieuChinh = "2".equals(docTmp.getEmbedded(Arrays.asList("HDSS", "TCTBao"), ""));
				boolean isThayThe = false;
				String check_status = docTmp.get("EInvoiceStatus", "");
				if(check_status.equals("REPLACED"))	{
					isThayThe = "3".equals(docTmp.getEmbedded(Arrays.asList("HDSS", "TCTBao"), ""));
				}
				
				File fileJP = new File(SystemParams.DIR_E_INVOICE_TEMPLATE, fileNameJP);

				ByteArrayOutputStream baosPDF = null;

				baosPDF = jpUtils.createFinalInvoice(fileJP, doc,CheckView, secureKey, numberRowInPage, numberRowInPageMultiPage,
						numberCharsInRow, MST,link, ParamUSD,
						Paths.get(SystemParams.DIR_E_INVOICE_TEMPLATE, "images", MST, ImgLogo).toString(),
						Paths.get(SystemParams.DIR_E_INVOICE_TEMPLATE, "images", MST, ImgBackground).toString(),
						Paths.get(SystemParams.DIR_E_INVOICE_TEMPLATE, "images", MST, ImgQA ).toString(),
						Paths.get(SystemParams.DIR_E_INVOICE_TEMPLATE, "images", MST, ImgVien ).toString(),

						false,Constants.INVOICE_STATUS.DELETED.equals(eInvoiceStatus), isThayThe, isDieuChinh);
				/* LUU TAP TIN PDF */
				if (null != baosPDF) {
					try (OutputStream fileOuputStream = new FileOutputStream(new File(dir, fileNamePDF))) {
						baosPDF.writeTo(fileOuputStream);
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}
		}
		/* END - KIEM TRA XEM CO FILE PDF CHUA; NEU CHUA CO THI TAO FILE PDF */

		file = null;
		file = new File(dir, fileNameXML);
		if (file.exists() && file.isFile()) {
			listFiles.add(file.toString());
			listNames.add(mauHD + "-" + soHD + ".xml");
		}
		file = new File(dir, fileNamePDF);
		if (file.exists() && file.isFile()) {
			listFiles.add(file.toString());
			listNames.add(mauHD + "-" + soHD + ".pdf");
		}
		
		boolean boo = false;
		boo = mailUtils.sendMail(mailConfig, _title, _content, _email, listFiles, listNames, true);	
		}
		}
	}
	
	
}
