package vn.sesgroup.hddt.user.impl;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.mail.MessagingException;

import org.bson.Document;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.api.message.JSONRoot;
import com.api.message.Msg;
import com.api.message.MsgHeader;
import com.api.message.MsgPage;
import com.fasterxml.jackson.databind.JsonNode;

import vn.sesgroup.hddt.dto.FileInfo;
import vn.sesgroup.hddt.user.dao.AbstractDAO;
import vn.sesgroup.hddt.user.dao.CommonDAO;
import vn.sesgroup.hddt.user.service.JPUtils;
import vn.sesgroup.hddt.utility.Commons;
import vn.sesgroup.hddt.utility.Constants;
import vn.sesgroup.hddt.utility.Json;
import vn.sesgroup.hddt.utility.SystemParams;

@Repository
@Transactional
public class CommonImpl extends AbstractDAO implements CommonDAO {
//	@Autowired ConfigConnectMongo cfg;
	@Autowired
	MongoTemplate mongoTemplate;
	@Autowired
	JPUtils jpUtils;

	@Override
	public FileInfo printEinvoiceAll(JSONRoot jsonRoot) throws Exception {
		FileInfo fileInfo = new FileInfo();
		Msg msg = jsonRoot.getMsg();
		MsgHeader header = msg.getMsgHeader();
		Object objData = msg.getObjData();

		try {

			JsonNode jsonData = null;
			if (objData != null) {
				jsonData = Json.serializer().nodeFromObject(msg.getObjData());
			}
			String _token = commons.getTextJsonNode(jsonData.at("/_token")).replaceAll("\\s", "");
			String isConvert = commons.getTextJsonNode(jsonData.at("/IsConvert")).replaceAll("\\s", "");

			ObjectId id__ = new ObjectId(_token);
			Document docTmp1 = null;
			Document findTmp = new Document("_id", id__).append("IsDelete", false);
			
			Iterable<Document> cursor = null;
			Iterator<Document> iter = null;
			cursor = mongoTemplate.getCollection("EInvoiceTmp").find(findTmp);
			iter = cursor.iterator();
			if (iter.hasNext()) {
				docTmp1 = iter.next();
			}
	
			if (docTmp1 == null) {
				return new FileInfo();
			}

			List<Object> rows = null;
			rows = docTmp1.getList("Arrays", Object.class);
			// Tạo một ExecutorService với 10 luồng			
			ExecutorService executorService = Executors.newFixedThreadPool(Integer.parseInt(SystemParams.PoolThread));
			// Tính toán số lượng phần tử trong mỗi nhóm
			int groupSize = rows.size() / Integer.valueOf(SystemParams.PoolThread); // Chia đều số lượng phần tử cho 10 luồng
			if (groupSize == 0) {
				groupSize = rows.size();
			}

			List<PoolData> listpool = new ArrayList<>();
			// Tạo danh sách các CompletableFuture
			List<CompletableFuture<?>> completableFutures = new ArrayList<>();
			// Chia các giá trị thành các nhóm nhỏ và gửi từng nhóm vào các luồng khác nhau
			for (int i = 0; i < rows.size(); i += groupSize) {
				int endIndex = Math.min(i + groupSize, rows.size()); // Xác định chỉ số cuối cùng của nhóm
				List<Object> group = rows.subList(i, endIndex); // Lấy nhóm phần tử
				CompletableFuture<?> completableFuture = CompletableFuture.runAsync(() -> {
					// Tiếp tục xử lý nhóm phần tử
					processGroup(group,  header, commons, isConvert, jpUtils, listpool);

				}, executorService);

				completableFutures.add(completableFuture);
			}
			// Chờ tất cả các CompletableFuture hoàn thành
			CompletableFuture.allOf(completableFutures.toArray(new CompletableFuture[0])).join();
			// Sắp xếp lại danh sách dataList theo thứ tự số number
			Collections.sort(listpool, (d1, d2) -> Integer.compare(d2.getNumber(), d1.getNumber()));
			
			List<String> listFileNamePdfFinalAll = listpool.stream().map(PoolData::getString)
					.collect(Collectors.toList());
			String listInvoiceNumber = listpool.stream()
				    .map(p -> String.valueOf(p.getNumber()))
				    .collect(Collectors.joining(", "));
			// Đóng ExecutorService và đợi tất cả các nhiệm vụ con hoàn thành
			executorService.shutdown();
			executorService.awaitTermination(2_000, TimeUnit.MILLISECONDS);
			completableFutures.clear();
			listpool.clear();
		
			if (listFileNamePdfFinalAll.size() == 0) {
				return fileInfo;
			} else {
				ByteArrayOutputStream out = commons.doMergeMultiPdf(listFileNamePdfFinalAll);
				fileInfo.setContentFile(out.toByteArray());
				fileInfo.setFileName(listInvoiceNumber);
				//giai phong
				out.flush();
				out.close();
				listFileNamePdfFinalAll.clear();
				return fileInfo;
			}
		} catch (NullPointerException | MessagingException | UnsupportedEncodingException e) {
			return new FileInfo();
		}
		

	}

	private void processGroup(List<Object> group, MsgHeader header, Commons commons,
			String isConvert, JPUtils jpUtils2, List<PoolData> listpool) {
		ByteArrayOutputStream baosPDF = null;
		for (Object _idIntoArray : group) {
			String _id = _idIntoArray.toString();
			ObjectId objectId = null;
			try {
				objectId = new ObjectId(_id);
				Document docFind = new Document("IssuerId", header.getIssuerId()).append("_id", objectId)
						.append("IsDelete", new Document("$ne", true));
				List<Document> pipeline = new ArrayList<Document>();
				pipeline.add(new Document("$match", docFind));
			
				pipeline.add(new Document("$lookup", new Document("from", "DMMauSoKyHieu")
						.append("let",
								new Document("vIssuerId", "$IssuerId").append("vMauSoHD",
										"$EInvoiceDetail.TTChung.MauSoHD"))
						.append("pipeline", Arrays.asList(new Document("$match", new Document("$expr", new Document(
								"$and",
								Arrays.asList(new Document("$eq", Arrays.asList("$$vIssuerId", "$IssuerId")),
										new Document("$eq",
												Arrays.asList(new Document("$toString", "$_id"), "$$vMauSoHD"))))))))
						.append("as", "DMMauSoKyHieu")));
				pipeline.add(new Document("$unwind",
						new Document("path", "$DMMauSoKyHieu").append("preserveNullAndEmptyArrays", true)));
			
				pipeline.add(new Document("$lookup",
						new Document("from", "UserConFig")
								.append("pipeline",
										Arrays.asList(new Document("$match",
												new Document("viewshd", "Y").append("IssuerId", header.getIssuerId()))))
								.append("as", "UserConFig")));
				pipeline.add(new Document("$unwind",
						new Document("path", "$UserConFig").append("preserveNullAndEmptyArrays", true)));
			
				pipeline.add(new Document("$lookup",
						new Document("from", "PramLink")
								.append("pipeline",
										Arrays.asList(new Document("$match",
												new Document("$expr", new Document("IsDelete", false)))))
								.append("as", "PramLink")));
				pipeline.add(new Document("$unwind",
						new Document("path", "$PramLink").append("preserveNullAndEmptyArrays", true)));
			
				Document docTmp = null;

				Iterable<Document> cursor = null;
				Iterator<Document> iter = null;
				cursor = mongoTemplate.getCollection("EInvoice").aggregate(pipeline);
				iter = cursor.iterator();
				if (iter.hasNext()) {
					docTmp = iter.next();
				}
				pipeline.clear();
				if (null == docTmp) {
					docFind = new Document("_id", objectId).append("IsDelete", new Document("$ne", true));
					pipeline = new ArrayList<Document>();
					pipeline.add(new Document("$match", docFind));
					pipeline.add(new Document("$lookup", new Document("from", "DMMauSoKyHieu")
							.append("let",
									new Document("vIssuerId", "$IssuerId").append("vMauSoHD",
											"$EInvoiceDetail.TTChung.MauSoHD"))
							.append("pipeline",
									Arrays.asList(new Document("$match",
											new Document("$expr", new Document("$and", Arrays.asList(
													new Document("$eq", Arrays.asList("$$vIssuerId", "$IssuerId")),
													new Document("$eq",
															Arrays.asList(new Document("$toString", "$_id"),
																	"$$vMauSoHD"))))))))
							.append("as", "DMMauSoKyHieu")));
					pipeline.add(new Document("$unwind",
							new Document("path", "$DMMauSoKyHieu").append("preserveNullAndEmptyArrays", true)));

					pipeline.add(new Document("$lookup", new Document("from", "UserConFig")
							.append("let", new Document("vIssuerId", "$IssuerId"))
							.append("pipeline", Arrays.asList(new Document("$match",
									new Document("$expr", new Document("$and",
											Arrays.asList(
													new Document("$eq", Arrays.asList("$$vIssuerId", "$IssuerId"))))))))
							.append("as", "UserConFig")));
					pipeline.add(new Document("$unwind",
							new Document("path", "$UserConFig").append("preserveNullAndEmptyArrays", true)));
					pipeline.add(new Document("$lookup",
							new Document("from", "PramLink")
									.append("pipeline",
											Arrays.asList(new Document("$match",
													new Document("$expr", new Document("IsDelete", false)))))
									.append("as", "PramLink")));
					pipeline.add(new Document("$unwind",
							new Document("path", "$PramLink").append("preserveNullAndEmptyArrays", true)));

					docTmp = null;
					 try {
							cursor = mongoTemplate.getCollection("EInvoice").aggregate(pipeline);
							iter = cursor.iterator();
							if (iter.hasNext()) {
								docTmp = iter.next();
							}
					 } catch (Exception e) {
						// TODO: handle exception
					}
					 pipeline.clear();
				}

				if (null == docTmp || docTmp.get("DMMauSoKyHieu") == null) {
					break;
				}
				String link = docTmp.getEmbedded(Arrays.asList("PramLink", "LinkPortal"), "");
				boolean isDieuChinh = "2".equals(docTmp.getEmbedded(Arrays.asList("HDSS", "TCTBao"), ""));
				boolean isThayThe = false;
				String check_status = docTmp.get("EInvoiceStatus", "");
				if (check_status.equals("REPLACED")) {
					isThayThe = true;
				}
				if (check_status.equals("ADJUSTED")) {
					isDieuChinh = true;
				}

				String CheckView = docTmp.getEmbedded(Arrays.asList("UserConFig", "viewshd"), "");
				String MST = docTmp.getEmbedded(Arrays.asList("EInvoiceDetail", "NDHDon", "NBan", "MST"), "");
				int invoiceNumber = docTmp.getEmbedded(Arrays.asList("EInvoiceDetail", "TTChung", "SHDon"), 0);
				String ImgLogo = docTmp.getEmbedded(Arrays.asList("DMMauSoKyHieu", "Templates", "ImgLogo"), "");
				String ImgBackground = docTmp.getEmbedded(Arrays.asList("DMMauSoKyHieu", "Templates", "ImgBackground"),
						"");
				String ImgQA = docTmp.getEmbedded(Arrays.asList("DMMauSoKyHieu", "Templates", "ImgQA"), "");
				String ImgVien = docTmp.getEmbedded(Arrays.asList("DMMauSoKyHieu", "Templates", "ImgVien"), "");
				String dir = docTmp.get("Dir", "");
				String signStatusCode = docTmp.get("SignStatusCode", "");
				String eInvoiceStatus = docTmp.get("EInvoiceStatus", "");
				String MCCQT = docTmp.get("MCCQT", "");
				String fileName = _id + ".xml";
				if ("SIGNED".equals(signStatusCode) && !"".equals(MCCQT)) {
					fileName = _id + "_" + MCCQT + ".xml";
				} else {
					if ("SIGNED".equals(signStatusCode)) {
						fileName = _id + "_signed.xml";
					}
				}

				File file = new File(dir, fileName);
				if (!file.exists() || !file.isFile()) {
					break;
				}

				org.w3c.dom.Document doc = commons.fileToDocument(file);
				/* TEST REPORT TO PDF */
				String fileNameJP = docTmp.getEmbedded(Arrays.asList("DMMauSoKyHieu", "Templates", "FileName"), "");
				int numberRowInPage = docTmp.getEmbedded(Arrays.asList("DMMauSoKyHieu", "Templates", "RowsInPage"), 20);
				int numberRowInPageMultiPage = docTmp
						.getEmbedded(Arrays.asList("DMMauSoKyHieu", "Templates", "RowInPageMultiPage"), 26);
				int numberCharsInRow = docTmp.getEmbedded(Arrays.asList("DMMauSoKyHieu", "Templates", "CharsInRow"),
						50);

				File fileJP = new File(SystemParams.DIR_E_INVOICE_TEMPLATE, fileNameJP);
				baosPDF = jpUtils2.createFinalInvoice(fileJP, doc, CheckView, numberRowInPage, numberRowInPageMultiPage,
						numberCharsInRow, MST, link,
						Paths.get(SystemParams.DIR_E_INVOICE_TEMPLATE, "images", MST, ImgLogo).toString(),
						Paths.get(SystemParams.DIR_E_INVOICE_TEMPLATE, "images", MST, ImgBackground).toString(),
						Paths.get(SystemParams.DIR_E_INVOICE_TEMPLATE, "images", MST, ImgQA).toString(),
						Paths.get(SystemParams.DIR_E_INVOICE_TEMPLATE, "images", MST, ImgVien).toString(),
						"Y".equals(isConvert), Constants.INVOICE_STATUS.DELETED.equals(eInvoiceStatus), isThayThe,
						isDieuChinh, Constants.INVOICE_STATUS.ERROR_CQT.equals(eInvoiceStatus));

				// baosPDF = jpUtils.createFinalInvoicetest(fileJP,
				// docTmp,"Y".equals(isConvert));
				if (baosPDF != null) {
					file = new File(dir, docTmp.get("_id") + "_final.pdf");
					try (OutputStream fileOuputStream = new FileOutputStream(file)) {
						baosPDF.writeTo(fileOuputStream);
//						String threadName = Thread.currentThread().getName();
						// Tìm vị trí của dấu gạch ngang cuối cùng trong chuỗi
//						int dashIndex = threadName.lastIndexOf("-");
						// Trích xuất phần tử sau dấu gạch ngang cuối cùng
//						String numberString = threadName.substring(dashIndex + 1);
						// Chuyển đổi chuỗi thành số nguyên
//						int number = Integer.parseInt(numberString);

						listpool.add(new PoolData(invoiceNumber, file.getAbsolutePath()));
						//giai phong du lieu
						fileOuputStream.close();
						  baosPDF.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
				  baosPDF.reset();
			} catch (Exception e) {
				// Xử lý ngoại lệ (nếu cần)
			}
		}
	}

	@Override
	public FileInfo printCttncnAll(JSONRoot jsonRoot) throws Exception {
		FileInfo fileInfo = new FileInfo();
		Msg msg = jsonRoot.getMsg();
		MsgHeader header = msg.getMsgHeader();
		Object objData = msg.getObjData();

		try {
			JsonNode jsonData = null;
			if (objData != null) {
				jsonData = Json.serializer().nodeFromObject(msg.getObjData());
			}
			String _token = commons.getTextJsonNode(jsonData.at("/_token")).replaceAll("\\s", "");

			ObjectId _id = new ObjectId(_token);
			Document docTmp1 = null;
			Document findTmp = new Document("_id", _id).append("IsDelete", false);

			Iterable<Document> cursor = null;
			Iterator<Document> iter = null;
			cursor = mongoTemplate.getCollection("EInvoiceTmp").find(findTmp);
			iter = cursor.iterator();
			if (iter.hasNext()) {
				docTmp1 = iter.next();
			}

			if (docTmp1 == null) {
				return new FileInfo();
			}

			List<Object> rows = null;
			rows = docTmp1.getList("Arrays", Object.class);
			// Tạo một ExecutorService với 10 luồng
			ExecutorService executorService = Executors.newFixedThreadPool(Integer.parseInt(SystemParams.PoolThread));
			// Tính toán số lượng phần tử trong mỗi nhóm
			int groupSize = rows.size() / Integer.valueOf(SystemParams.PoolThread); // Chia đều số lượng phần tử cho 10
																					// luồng
			if (groupSize == 0) {
				groupSize = rows.size();
			}

			List<PoolData> listpool = new ArrayList<>();
			// Tạo danh sách các CompletableFuture
			List<CompletableFuture<?>> completableFutures = new ArrayList<>();
			// Chia các giá trị thành các nhóm nhỏ và gửi từng nhóm vào các luồng khác nhau
			for (int i = 0; i < rows.size(); i += groupSize) {
				int endIndex = Math.min(i + groupSize, rows.size()); // Xác định chỉ số cuối cùng của nhóm
				List<Object> group = rows.subList(i, endIndex); // Lấy nhóm phần tử
				CompletableFuture<?> completableFuture = CompletableFuture.runAsync(() -> {
					// Tiếp tục xử lý nhóm phần tử
					processGroupCttncn(group, header, commons, "Y", jpUtils, listpool);

				}, executorService);

				completableFutures.add(completableFuture);
			}
			// Chờ tất cả các CompletableFuture hoàn thành
			CompletableFuture.allOf(completableFutures.toArray(new CompletableFuture[0])).join();
			// Sắp xếp lại danh sách dataList theo thứ tự số number
			Collections.sort(listpool, (d1, d2) -> Integer.compare(d2.getNumber(), d1.getNumber()));
			List<String> listFileNamePdfFinalAll = listpool.stream().map(PoolData::getString)
					.collect(Collectors.toList());
			// Đóng ExecutorService và đợi tất cả các nhiệm vụ con hoàn thành
			executorService.shutdown();
			executorService.awaitTermination(2_000, TimeUnit.MILLISECONDS);
			completableFutures.clear();
			listpool.clear();

			if (listFileNamePdfFinalAll.size() == 0) {
				return fileInfo;
			} else {
				ByteArrayOutputStream out = commons.doMergeMultiPdf(listFileNamePdfFinalAll);
				fileInfo.setContentFile(out.toByteArray());
				// giai phong
				out.flush();
				out.close();
				listFileNamePdfFinalAll.clear();
				return fileInfo;
			}
		} catch (NullPointerException | MessagingException | UnsupportedEncodingException e) {
			return new FileInfo();
		}
	}

	private void processGroupCttncn(List<Object> group, MsgHeader header, Commons commons, String isConvert,
			JPUtils jpUtils, List<PoolData> listpool) {
		ByteArrayOutputStream baosPDF = null;
		for (Object _idIntoArray : group) {
			String _id = _idIntoArray.toString();
			ObjectId objectId = null;
			ObjectId objectIdIssu = null;
			try {
				objectId = new ObjectId(_id);
				objectIdIssu = new ObjectId(header.getIssuerId());
				List<Document> pipeline = new ArrayList<Document>();
				pipeline.add(new Document("$match",
						new Document("_id", objectId).append("IsDelete", new Document("$ne", true))));
				pipeline.add(new Document("$lookup",
						new Document("from", "Issuer")
								.append("pipeline",
										Arrays.asList(new Document("$match", new Document("_id", objectIdIssu)
												.append("IsDelete", new Document("$ne", true)))))
								.append("as", "Issuer")));
				pipeline.add(new Document("$unwind",
						new Document("path", "$Issuer").append("preserveNullAndEmptyArrays", true)));

				pipeline.add(new Document("$lookup",
						new Document("from", "DMMSTNCN")
								.append("let",
										new Document("vMauSoHD", "$MauSoHD"))
								.append("pipeline",
										Arrays.asList(
												new Document("$match",
														new Document("$expr",
																new Document("$and", Arrays.asList(
																		new Document("$eq",
																				Arrays.asList("$_id",
																						new Document("$toObjectId",
																								"$$vMauSoHD"))),
																		new Document("$eq",
																				Arrays.asList("$IssuerId",
																						objectIdIssu.toString())),
																		new Document(
																				"$eq",
																				Arrays.asList("$IsDelete", false)),
																		new Document("$eq",
																				Arrays.asList("$IsActive", true))))))))
								.append("as", "DMMSTNCN")));
				pipeline.add(new Document("$unwind",
						new Document("path", "$DMMSTNCN").append("preserveNullAndEmptyArrays", true)));
				pipeline.add(new Document("$lookup",
						new Document("from", "PramLink")
								.append("pipeline",
										Arrays.asList(new Document("$match",
												new Document("$expr", new Document("IsDelete", false)))))
								.append("as", "PramLink")));
				pipeline.add(new Document("$unwind",
						new Document("path", "$PramLink").append("preserveNullAndEmptyArrays", true)));

				Document docTmp = null;

				Iterable<Document> cursor = null;
				Iterator<Document> iter = null;
				cursor = mongoTemplate.getCollection("ChungTuTNCN").aggregate(pipeline);
				iter = cursor.iterator();
				if (iter.hasNext()) {
					docTmp = iter.next();
				}
				if (null == docTmp || docTmp.get("DMMSTNCN") == null) {
					break;
				}

				String ImgLogo = docTmp.getEmbedded(Arrays.asList("DMMSTNCN", "LoGo"), "");

				String link = docTmp.getEmbedded(Arrays.asList("PramLink", "LinkPortal"), "");
				String KH = docTmp.getEmbedded(Arrays.asList("DMMSTNCN", "KyHieu"), "") + "/"
						+ docTmp.getEmbedded(Arrays.asList("DMMSTNCN", "Nam"), "") + "/"
						+ docTmp.getEmbedded(Arrays.asList("DMMSTNCN", "ChungTu"), "");
				String MS = docTmp.getEmbedded(Arrays.asList("DMMSTNCN", "MauSo"), "");;
				int invoiceNumber = docTmp.get("SHDon", 0);
				String Status = docTmp.get("Status", "");

				/* TEST REPORT TO PDF */

				String dir = docTmp.get("Dir", "");
				String SignStatus = docTmp.get("SignStatus", "");
				String fileName = _id + ".xml";
				if ("SIGNED".equals(SignStatus)) {
					fileName = _id + "_signed.xml";
				}

				File file = new File(dir, fileName);
				if (!file.exists() || !file.isFile()) {
					break;
				}

				org.w3c.dom.Document doc = commons.fileToDocument(file);
				/* TEST REPORT TO PDF */
				String fileNameJP = docTmp.getEmbedded(Arrays.asList("DMMSTNCN", "FileName"), "");
				File fileJP = new File(SystemParams.DIR_E_INVOICE_TEMPLATE, fileNameJP);
				baosPDF = jpUtils.viewpdfcttncn(fileJP, doc, docTmp,
						Paths.get(SystemParams.DIR_E_INVOICE_TEMPLATE, "MauSoTNCN",
								docTmp.getEmbedded(Arrays.asList("Issuer", "TaxCode"), ""), ImgLogo).toString(),
						KH, MS, link, "Y".equals(isConvert), Constants.INVOICE_STATUS.XOABO.equals(Status));

				if (baosPDF != null) {
					file = new File(dir, docTmp.get("_id") + "_final.pdf");
					try (OutputStream fileOuputStream = new FileOutputStream(file)) {
						baosPDF.writeTo(fileOuputStream);
						listpool.add(new PoolData(invoiceNumber, file.getAbsolutePath()));
						// giai phong du lieu
						fileOuputStream.close();
						baosPDF.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
				baosPDF.reset();
			} catch (Exception e) {

			}
		}
	}

	@Override
	public FileInfo printEinvoiceAll1(JSONRoot jsonRoot) throws Exception {
		FileInfo fileInfo = new FileInfo();
		Msg msg = jsonRoot.getMsg();
		MsgHeader header = msg.getMsgHeader();
		ByteArrayOutputStream baosPDF = null;
		List<String> listFileNamePdfFinal = new ArrayList<>();
		HashMap<String, String> hItem = null;
		ArrayList<HashMap<String, String>> arrayInfoInvoice = new ArrayList<>();
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		File file_zip = null;
		Object objData = msg.getObjData();
		JsonNode jsonData = null;

		try {
			String _id = "";
			String _token = "";
			String isConvert = "";
			if (objData != null) {
				jsonData = Json.serializer().nodeFromObject(msg.getObjData());
				_token = commons.getTextJsonNode(jsonData.at("/_token")).replaceAll("\\s", "");
				isConvert = commons.getTextJsonNode(jsonData.at("/IsConvert")).replaceAll("\\s", "");
			}

			ObjectId id__ = new ObjectId(_token);
			Document findTmp = new Document("_id", id__).append("IsDelete", false);
			Iterable<Document> cursor1 = mongoTemplate.getCollection("EInvoiceTmp").find(findTmp);
			Iterator<Document> iter1 = cursor1.iterator();
			Document docTmp1 = null;
			if (iter1.hasNext()) {
				docTmp1 = iter1.next();
			}
			if (docTmp1 == null) {
				return new FileInfo();
			}
			List<Object> rows = null;
			rows = docTmp1.getList("Arrays", Object.class);
			for (Object _idIntoArray : rows) {
				_id = _idIntoArray.toString();
				ObjectId objectId = null;
				try {
					objectId = new ObjectId(_id);
				} catch (Exception e) {
				}

				Document docFind = new Document("IssuerId", header.getIssuerId()).append("_id", objectId)
						.append("IsDelete", new Document("$ne", true));
				List<Document> pipeline = new ArrayList<Document>();
				pipeline.add(new Document("$match", docFind));
				pipeline.add(new Document("$lookup", new Document("from", "DMMauSoKyHieu")
						.append("let",
								new Document("vIssuerId", "$IssuerId").append("vMauSoHD",
										"$EInvoiceDetail.TTChung.MauSoHD"))
						.append("pipeline", Arrays.asList(new Document("$match", new Document("$expr", new Document(
								"$and",
								Arrays.asList(new Document("$eq", Arrays.asList("$$vIssuerId", "$IssuerId")),
										new Document("$eq",
												Arrays.asList(new Document("$toString", "$_id"), "$$vMauSoHD"))))))))
						.append("as", "DMMauSoKyHieu")));
				pipeline.add(new Document("$unwind",
						new Document("path", "$DMMauSoKyHieu").append("preserveNullAndEmptyArrays", true)));
				pipeline.add(new Document("$lookup",
						new Document("from", "UserConFig")
								.append("pipeline",
										Arrays.asList(new Document("$match",
												new Document("viewshd", "Y").append("IssuerId", header.getIssuerId()))))
								.append("as", "UserConFig")));
				pipeline.add(new Document("$unwind",
						new Document("path", "$UserConFig").append("preserveNullAndEmptyArrays", true)));
				pipeline.add(new Document("$lookup",
						new Document("from", "PramLink")
								.append("pipeline",
										Arrays.asList(new Document("$match",
												new Document("$expr", new Document("IsDelete", false)))))
								.append("as", "PramLink")));
				pipeline.add(new Document("$unwind",
						new Document("path", "$PramLink").append("preserveNullAndEmptyArrays", true)));
				Document docTmp = null;
				Iterable<Document> cursor = mongoTemplate.getCollection("EInvoice").aggregate(pipeline)
						.allowDiskUse(true);
				Iterator<Document> iter = cursor.iterator();
				if (iter.hasNext()) {
					docTmp = iter.next();
				}
				if (null == docTmp || docTmp.get("DMMauSoKyHieu") == null) {
					return new FileInfo();
				}
				String link = docTmp.getEmbedded(Arrays.asList("PramLink", "LinkPortal"), "");
				boolean isDieuChinh = "2".equals(docTmp.getEmbedded(Arrays.asList("HDSS", "TCTBao"), ""));
				boolean isThayThe = "3".equals(docTmp.getEmbedded(Arrays.asList("HDSS", "TCTBao"), ""));
				String check_status = docTmp.get("EInvoiceStatus", "");
				if (check_status.equals("REPLACED")) {
					isThayThe = true;
				}
				if (check_status.equals("ADJUSTED")) {
					isDieuChinh = true;
				}
				String CheckView = docTmp.getEmbedded(Arrays.asList("UserConFig", "viewshd"), "");
				String MST = docTmp.getEmbedded(Arrays.asList("EInvoiceDetail", "NDHDon", "NBan", "MST"), "");
				String ImgLogo = docTmp.getEmbedded(Arrays.asList("DMMauSoKyHieu", "Templates", "ImgLogo"), "");
				String ImgBackground = docTmp.getEmbedded(Arrays.asList("DMMauSoKyHieu", "Templates", "ImgBackground"),
						"");
				String ImgQA = docTmp.getEmbedded(Arrays.asList("DMMauSoKyHieu", "Templates", "ImgQA"), "");
				String ImgVien = docTmp.getEmbedded(Arrays.asList("DMMauSoKyHieu", "Templates", "ImgVien"), "");

				String mskh = docTmp.getEmbedded(Arrays.asList("EInvoiceDetail", "TTChung", "KHMSHDon"), "")
						+ docTmp.getEmbedded(Arrays.asList("EInvoiceDetail", "TTChung", "KHHDon"), "");
				Integer shd = docTmp.getEmbedded(Arrays.asList("EInvoiceDetail", "TTChung", "SHDon"), 0);

				String dir = docTmp.get("Dir", "");
				String signStatusCode = docTmp.get("SignStatusCode", "");
				String eInvoiceStatus = docTmp.get("EInvoiceStatus", "");
				String MCCQT = docTmp.get("MCCQT", "");
				String fileName = _id + ".xml";
				if ("SIGNED".equals(signStatusCode) && !"".equals(MCCQT)) {
					fileName = _id + "_" + MCCQT + ".xml";
				} else {
					if ("SIGNED".equals(signStatusCode)) {
						fileName = _id + "_signed.xml";
					}
				}

				File file = new File(dir, fileName);
				if (!file.exists() || !file.isFile()) {
					return new FileInfo();
				}

				org.w3c.dom.Document doc = commons.fileToDocument(file);
				/* TEST REPORT TO PDF */
				String fileNameJP = docTmp.getEmbedded(Arrays.asList("DMMauSoKyHieu", "Templates", "FileName"), "");
				int numberRowInPage = docTmp.getEmbedded(Arrays.asList("DMMauSoKyHieu", "Templates", "RowsInPage"), 20);
				int numberRowInPageMultiPage = docTmp
						.getEmbedded(Arrays.asList("DMMauSoKyHieu", "Templates", "RowInPageMultiPage"), 26);
				int numberCharsInRow = docTmp.getEmbedded(Arrays.asList("DMMauSoKyHieu", "Templates", "CharsInRow"),
						50);
				File fileJP = new File(SystemParams.DIR_E_INVOICE_TEMPLATE, fileNameJP);
				baosPDF = jpUtils.createFinalInvoice(fileJP, doc, CheckView, numberRowInPage, numberRowInPageMultiPage,
						numberCharsInRow, MST, link,
						Paths.get(SystemParams.DIR_E_INVOICE_TEMPLATE, "images", MST, ImgLogo).toString(),
						Paths.get(SystemParams.DIR_E_INVOICE_TEMPLATE, "images", MST, ImgBackground).toString(),
						Paths.get(SystemParams.DIR_E_INVOICE_TEMPLATE, "images", MST, ImgQA).toString(),
						Paths.get(SystemParams.DIR_E_INVOICE_TEMPLATE, "images", MST, ImgVien).toString(),
						"Y".equals(isConvert), Constants.INVOICE_STATUS.DELETED.equals(eInvoiceStatus), isThayThe,
						isDieuChinh, Constants.INVOICE_STATUS.ERROR_CQT.equals(eInvoiceStatus));

				if (null != baosPDF) {
					file = new File(dir, docTmp.get("_id") + "_final.pdf");
					try (OutputStream fileOuputStream = new FileOutputStream(file)) {
						baosPDF.writeTo(fileOuputStream);
						listFileNamePdfFinal.add(file.getAbsolutePath());
					} catch (IOException e) {
						e.printStackTrace();
					}
				}

				String namepdf = mskh + "_" + shd + ".pdf";
				File tam = new File(dir, namepdf);
				copyFileUsingStream(file, tam);

				hItem = new HashMap<>();
				hItem.put("UrlFile", tam.getAbsolutePath());
				arrayInfoInvoice.add(hItem);

			}
			if (listFileNamePdfFinal.size() == 0) {
				return fileInfo;
			} else {

				/* NEN DANH SACH FILE XML */
				FileInputStream fis = null;
				int length;
				byte[] buffer = new byte[1024];
				bos = new ByteArrayOutputStream();
				ZipOutputStream zout = new ZipOutputStream(bos);
				for (int i = 0; i < arrayInfoInvoice.size(); i++) {
					hItem = arrayInfoInvoice.get(i);
					file_zip = new File(hItem.get("UrlFile"));
					fis = new FileInputStream(file_zip);
					zout.putNextEntry(new ZipEntry(file_zip.getName()));
					while ((length = fis.read(buffer)) > 0)
						zout.write(buffer, 0, length);

					zout.closeEntry();
					fis.close();

				}
				zout.close();
				fileInfo.setFileName("EINVOICE.zip");
				fileInfo.setContentFile(bos.toByteArray());

				DateTimeFormatter format_time = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
				LocalDateTime time_dem = LocalDateTime.now();
				String time = time_dem.format(format_time);
				String name_company = removeAccent(header.getUserFullName());
				System.out.println(time + " " + name_company + " vua xuat PDF hang loat hoa don GTGT.");
				return fileInfo;
			}

		} catch (NullPointerException | MessagingException | UnsupportedEncodingException e) {
			return new FileInfo();
		}
	}

	private void copyFileUsingStream(File file, File tam) throws IOException {
		InputStream is = null;
		OutputStream os = null;
		try {
			is = new FileInputStream(file);
			os = new FileOutputStream(tam);
			byte[] buffer = new byte[1024];
			int length;
			while ((length = is.read(buffer)) > 0) {
				os.write(buffer, 0, length);
			}
		} finally {
			is.close();
			os.close();
		}
	}

	@Override
	public FileInfo printCttncnAllV1(JSONRoot jsonRoot) throws Exception {
		FileInfo fileInfo = new FileInfo();
		Msg msg = jsonRoot.getMsg();
		MsgHeader header = msg.getMsgHeader();
		Object objData = msg.getObjData();
		try {
			JsonNode jsonData = null;
			String _token = "";
			if (objData != null) {
				jsonData = Json.serializer().nodeFromObject(msg.getObjData());
			}
			if (jsonData != null) {
				_token = commons.getTextJsonNode(jsonData.at("/_token")).replaceAll("\\s", "");
			}

			Iterable<Document> cursor = null;
			Iterator<Document> iter = null;
			Document docTmp = null;
			
			ObjectId _id = new ObjectId(_token);
			Document findTmp = new Document("_id", _id).append("IsDelete", false);
			cursor = mongoTemplate.getCollection("EInvoiceTmp").find(findTmp);
			iter = cursor.iterator();
			if (iter.hasNext()) {
				docTmp = iter.next();
			}
			if (docTmp == null) {
				return new FileInfo();
			}

			List<Object> rows = new ArrayList<Object>();
			rows = docTmp.getList("Arrays", Object.class);
			
			List<Document> pipeline = new ArrayList<Document>();
			List<String> listFileNamePDF = new ArrayList<>();
			String fileNamePDF = "";
			File file_zip = null;
			ByteArrayOutputStream bos = new ByteArrayOutputStream();
			
			for (Object id : rows) {
				String stringId = id.toString();
				ObjectId objectId = new ObjectId(stringId);
				ObjectId objectIdIssu = new ObjectId(header.getIssuerId());
				
				pipeline.clear();
				
				pipeline.add(new Document("$match",
						new Document("_id", objectId).append("IsDelete", new Document("$ne", true))));
				pipeline.add(
						new Document("$lookup",
								new Document("from", "Issuer")
										.append("pipeline",
												Arrays.asList(
														new Document("$match",
																new Document("_id", objectIdIssu).append("IsDelete",
																		new Document("$ne", true)))))
										.append("as", "Issuer")));
				pipeline.add(new Document("$unwind",
						new Document("path", "$Issuer").append("preserveNullAndEmptyArrays", true)));

				pipeline.add(new Document("$lookup",
						new Document("from", "DMMSTNCN").append("let", new Document("vMauSo", "$MauSo"))
								.append("pipeline", Arrays.asList(new Document("$match",
										new Document("$expr",
												new Document("$and", Arrays.asList(
														new Document("$eq",
																Arrays.asList("$_id",
																		new Document("$toObjectId", "$$vMauSo"))),
														new Document("$eq",
																Arrays.asList("$IssuerId", objectIdIssu.toString())),
														new Document("$eq", Arrays.asList("$IsDelete", false)),
														new Document("$eq", Arrays.asList("$IsActive", true))))))))
								.append("as", "DMMSTNCN")));
				pipeline.add(new Document("$unwind",
						new Document("path", "$DMMSTNCN").append("preserveNullAndEmptyArrays", true)));
				pipeline.add(new Document("$lookup",
						new Document("from", "PramLink")
								.append("pipeline",
										Arrays.asList(new Document("$match",
												new Document("$expr", new Document("IsDelete", false)))))
								.append("as", "PramLink")));
				pipeline.add(new Document("$unwind",
						new Document("path", "$PramLink").append("preserveNullAndEmptyArrays", true)));

				docTmp = null;
				cursor = null;
				iter = null;
				cursor = mongoTemplate.getCollection("CTTNCNhan").aggregate(pipeline).allowDiskUse(true);
				iter = cursor.iterator();
				if (iter.hasNext()) {
					docTmp = iter.next();
				}
				if (null == docTmp || docTmp.get("DMMSTNCN") == null) {
					break;
				}
				String ImgLogo = docTmp.getEmbedded(Arrays.asList("DMMSTNCN", "LoGo"), "");
				String link = docTmp.getEmbedded(Arrays.asList("PramLink", "LinkPortal"), "");
				String msctu = docTmp.getEmbedded(Arrays.asList("MSCTu"), "");
				String khctu = docTmp.get("KHCTu", "");
				String mtdiep = docTmp.get("MTDiep", "");
				int sctu = docTmp.get("SCTu", 0);
				String fileNameJP = docTmp.getEmbedded(Arrays.asList("DMMSTNCN", "FileName"), "");
				String dir = docTmp.get("Dir", "");
				String signStatus = docTmp.get("SignStatus", "");
				String pathLogo = Paths.get(SystemParams.DIR_E_INVOICE_TEMPLATE, "MauSoTNCN",
						docTmp.getEmbedded(Arrays.asList("Issuer", "TaxCode"), ""), ImgLogo).toString();
				String fileName = stringId + ".xml";
				if ("SIGNED".equals(signStatus)) {
					fileName = stringId + "_signed.xml";
				}

				File file = new File(dir, fileName);
				if (!file.exists() || !file.isFile()) {
					break;
				}

				org.w3c.dom.Document doc = commons.fileToDocument(file);
				File fileJP = new File(SystemParams.DIR_E_INVOICE_TEMPLATE, fileNameJP);

				ByteArrayOutputStream baosPDF = null;
				baosPDF = jpUtils.viewpdfcttncnV1(fileJP, doc, docTmp, pathLogo, khctu, msctu, link, false);

				if (null != baosPDF) {
					if (sctu == 0) {
						fileNamePDF = khctu.replaceAll("/", "-") + "_" + mtdiep + ".pdf";
					} else {
						fileNamePDF = khctu.replaceAll("/", "-") + "_" + sctu + ".pdf";
					}

					file = new File(dir, fileNamePDF);
					try (OutputStream fileOuputStream = new FileOutputStream(file)) {
						baosPDF.writeTo(fileOuputStream);
						listFileNamePDF.add(file.getAbsolutePath());
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}
			if (listFileNamePDF.size() == 0) {
				return fileInfo;
			}
			if (listFileNamePDF.size() == 1) {
				file_zip = new File(listFileNamePDF.get(0));
				fileInfo.setContentFile(Files.readAllBytes(file_zip.toPath()));
				fileInfo.setFileName(fileNamePDF);
				return fileInfo;
			} else {

				byte[] buffer = new byte[1024];
				bos = new ByteArrayOutputStream();
				ZipOutputStream zout = new ZipOutputStream(bos);
				for (String fileName : listFileNamePDF) {
					File fileZip = new File(fileName);
					try (FileInputStream fis = new FileInputStream(fileZip)) {
						zout.putNextEntry(new ZipEntry(fileZip.getName()));
						int length;
						while ((length = fis.read(buffer)) > 0) {
							zout.write(buffer, 0, length);
						}
						zout.closeEntry();
					}
				}
				zout.close();
				fileInfo.setFileName("ChungTu_TNCN.zip");
				fileInfo.setContentFile(bos.toByteArray());
				return fileInfo;
			}

		} catch (Exception e) {
			return new FileInfo();
		}
	}

	@Override
	public FileInfo printEinvoiceMTTAll(JSONRoot jsonRoot) throws Exception {
		FileInfo fileInfo = new FileInfo();
		Msg msg = jsonRoot.getMsg();
		MsgHeader header = msg.getMsgHeader();
		ByteArrayOutputStream baosPDF = null;
		List<String> listFileNamePdfFinal = new ArrayList<>();
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		File file_zip = null;
		Object objData = msg.getObjData();
		JsonNode jsonData = null;

		try {
			String _id = "";
			String _token = "";
			String isConvert = "";
			String fileNamePDF = "";
			if (objData != null) {
				jsonData = Json.serializer().nodeFromObject(msg.getObjData());
				_token = commons.getTextJsonNode(jsonData.at("/_token")).replaceAll("\\s", "");
				isConvert = commons.getTextJsonNode(jsonData.at("/IsConvert")).replaceAll("\\s", "");
			}

			ObjectId id__ = new ObjectId(_token);
			Document findTmp = new Document("_id", id__).append("IsDelete", false);
			Iterable<Document> cursor1 = mongoTemplate.getCollection("EInvoiceTmp").find(findTmp);
			Iterator<Document> iter1 = cursor1.iterator();
			Document docTmp1 = null;
			if (iter1.hasNext()) {
				docTmp1 = iter1.next();
			}
			if (docTmp1 == null) {
				return new FileInfo();
			}
			List<Object> rows = null;
			rows = docTmp1.getList("Arrays", Object.class);
			for (Object _idIntoArray : rows) {
				_id = _idIntoArray.toString();
				ObjectId objectId = null;
				try {
					objectId = new ObjectId(_id);
				} catch (Exception e) {
				}

				Document docFind = new Document("IssuerId", header.getIssuerId()).append("_id", objectId)
						.append("IsDelete", new Document("$ne", true));
				List<Document> pipeline = new ArrayList<Document>();
				pipeline.add(new Document("$match", docFind));
				pipeline.add(new Document("$lookup", new Document("from", "DMMauSoKyHieu")
						.append("let",
								new Document("vIssuerId", "$IssuerId").append("vMauSoHD",
										"$EInvoiceDetail.TTChung.MauSoHD"))
						.append("pipeline", Arrays.asList(new Document("$match", new Document("$expr", new Document(
								"$and",
								Arrays.asList(new Document("$eq", Arrays.asList("$$vIssuerId", "$IssuerId")),
										new Document("$eq",
												Arrays.asList(new Document("$toString", "$_id"), "$$vMauSoHD"))))))))
						.append("as", "DMMauSoKyHieu")));
				pipeline.add(new Document("$unwind",
						new Document("path", "$DMMauSoKyHieu").append("preserveNullAndEmptyArrays", true)));
				
				pipeline.add(new Document("$lookup",
						new Document("from", "UserConFig")
								.append("pipeline",
										Arrays.asList(new Document("$match",
												new Document("viewshd", "Y").append("IssuerId", header.getIssuerId()))))
								.append("as", "UserConFig")));
				pipeline.add(new Document("$unwind",
						new Document("path", "$UserConFig").append("preserveNullAndEmptyArrays", true)));
				
				pipeline.add(new Document("$lookup",
						new Document("from", "PramLink")
								.append("pipeline",
										Arrays.asList(new Document("$match",
												new Document("$expr", new Document("IsDelete", false)))))
								.append("as", "PramLink")));
				pipeline.add(new Document("$unwind",
						new Document("path", "$PramLink").append("preserveNullAndEmptyArrays", true)));
				Document docTmp = null;
				Iterable<Document> cursor = mongoTemplate.getCollection("EInvoiceMTT").aggregate(pipeline)
						.allowDiskUse(true);
				Iterator<Document> iter = cursor.iterator();
				if (iter.hasNext()) {
					docTmp = iter.next();
				}
				if (null == docTmp || docTmp.get("DMMauSoKyHieu") == null) {
					return new FileInfo();
				}
				
				String link = docTmp.getEmbedded(Arrays.asList("PramLink", "LinkPortal"), "");
				boolean isDieuChinh = "2".equals(docTmp.getEmbedded(Arrays.asList("HDSS", "TCTBao"), ""));
				boolean isThayThe = "3".equals(docTmp.getEmbedded(Arrays.asList("HDSS", "TCTBao"), ""));
				String check_status = docTmp.get("EInvoiceStatus", "");
				if (check_status.equals("REPLACED")) {
					isThayThe = true;
				}
				if (check_status.equals("ADJUSTED")) {
					isDieuChinh = true;
				}
				String CheckView = docTmp.getEmbedded(Arrays.asList("UserConFig", "viewshd"), "");
				String MST = docTmp.getEmbedded(Arrays.asList("EInvoiceDetail", "NDHDon", "NBan", "MST"), "");
				String ImgLogo = docTmp.getEmbedded(Arrays.asList("DMMauSoKyHieu", "Templates", "ImgLogo"), "");
				String ImgBackground = docTmp.getEmbedded(Arrays.asList("DMMauSoKyHieu", "Templates", "ImgBackground"),
						"");
				String ImgQA = docTmp.getEmbedded(Arrays.asList("DMMauSoKyHieu", "Templates", "ImgQA"), "");
				String ImgVien = docTmp.getEmbedded(Arrays.asList("DMMauSoKyHieu", "Templates", "ImgVien"), "");

				String mskh = docTmp.getEmbedded(Arrays.asList("EInvoiceDetail", "TTChung", "KHMSHDon"), "")
						+ docTmp.getEmbedded(Arrays.asList("EInvoiceDetail", "TTChung", "KHHDon"), "");
				Integer shd = docTmp.getEmbedded(Arrays.asList("EInvoiceDetail", "TTChung", "SHDon"), 0);

				String dir = docTmp.get("Dir", "");
				String signStatusCode = docTmp.get("SignStatusCode", "");
				String eInvoiceStatus = docTmp.get("EInvoiceStatus", "");
				String mtdiep = docTmp.get("MTDiep", "");
				String fileName = _id + ".xml";
				if ("SIGNED".equals(signStatusCode)) {
					fileName = _id + "_signed" + ".xml";
				}
				else if ("NOSIGN".equals(signStatusCode) && eInvoiceStatus.equals("PENDING")) {
					fileName = _id + "_pending.xml";
				}
				
				else {
					fileName = _id + ".xml";
				}

				File file = new File(dir, fileName);
				if (!file.exists() || !file.isFile()) {
					return new FileInfo();
				}

				org.w3c.dom.Document doc = commons.fileToDocument(file);
				/* TEST REPORT TO PDF */
				String fileNameJP = docTmp.getEmbedded(Arrays.asList("DMMauSoKyHieu", "Templates", "FileName"), "");
				int numberRowInPage = docTmp.getEmbedded(Arrays.asList("DMMauSoKyHieu", "Templates", "RowsInPage"), 20);
				int numberRowInPageMultiPage = docTmp
						.getEmbedded(Arrays.asList("DMMauSoKyHieu", "Templates", "RowInPageMultiPage"), 26);
				int numberCharsInRow = docTmp.getEmbedded(Arrays.asList("DMMauSoKyHieu", "Templates", "CharsInRow"),
						50);
				File fileJP = new File(SystemParams.DIR_E_INVOICE_TEMPLATE, fileNameJP);
				baosPDF = jpUtils.createFinalInvoiceMTT(fileJP, doc,CheckView, link, eInvoiceStatus, signStatusCode, numberRowInPage, numberRowInPageMultiPage, numberCharsInRow,
						MST, Paths.get(SystemParams.DIR_E_INVOICE_TEMPLATE, "images", MST, ImgLogo).toString(),
						Paths.get(SystemParams.DIR_E_INVOICE_TEMPLATE, "images", MST, ImgBackground).toString(),
						Paths.get(SystemParams.DIR_E_INVOICE_TEMPLATE, "images", MST, ImgQA ).toString(),
						Paths.get(SystemParams.DIR_E_INVOICE_TEMPLATE, "images", MST, ImgVien ).toString(),

						"Y".equals(isConvert), Constants.INVOICE_STATUS.DELETED.equals(eInvoiceStatus), isThayThe, isDieuChinh);

				if (null != baosPDF) {
					fileNamePDF = mskh + "_" + shd + ".pdf";
					if (shd == 0) {
						fileNamePDF = mskh + "_" + mtdiep + ".pdf";
					} 
					file = new File(dir, fileNamePDF);
					try (OutputStream fileOuputStream = new FileOutputStream(file)) {
						baosPDF.writeTo(fileOuputStream);
						listFileNamePdfFinal.add(file.getAbsolutePath());
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}

			if (listFileNamePdfFinal.size() == 0) {
				return fileInfo;
			}
			if (listFileNamePdfFinal.size() == 1) {
				file_zip = new File(listFileNamePdfFinal.get(0));
				fileInfo.setContentFile(Files.readAllBytes(file_zip.toPath()));
				fileInfo.setFileName(fileNamePDF);
				return fileInfo;
			}
			else {
				byte[] buffer = new byte[1024];
				bos = new ByteArrayOutputStream();
				ZipOutputStream zout = new ZipOutputStream(bos);
				for (String fileName : listFileNamePdfFinal) {
					File fileZip = new File(fileName);
					try (FileInputStream fis = new FileInputStream(fileZip)) {
						zout.putNextEntry(new ZipEntry(fileZip.getName()));
						int length;
						while ((length = fis.read(buffer)) > 0) {
							zout.write(buffer, 0, length);
						}
						zout.closeEntry();
					}
				}
				zout.close();
				fileInfo.setFileName("EINVOICE_MTT.zip");
				fileInfo.setContentFile(bos.toByteArray());
				return fileInfo;
			}
		} catch (NullPointerException | MessagingException | UnsupportedEncodingException e) {
			return new FileInfo();
		}
	}
}
