package vn.sesgroup.hddt.user.impl;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

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
			Collections.sort(listpool, (d1, d2) -> Integer.compare(d1.getNumber(), d2.getNumber()));
			List<String> listFileNamePdfFinalAll = listpool.stream().map(PoolData::getString)
					.collect(Collectors.toList());
			// Đóng ExecutorService và đợi tất cả các nhiệm vụ con hoàn thành
			executorService.shutdown();
			executorService.shutdown();
			executorService.awaitTermination(2_000, TimeUnit.MILLISECONDS);
			completableFutures.clear();
			listpool.clear();
		
			if (listFileNamePdfFinalAll.size() == 0) {
				return fileInfo;
			} else {
				ByteArrayOutputStream out = commons.doMergeMultiPdf(listFileNamePdfFinalAll);
				fileInfo.setContentFile(out.toByteArray());
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
					isThayThe = "3".equals(docTmp.getEmbedded(Arrays.asList("HDSS", "TCTBao"), ""));
				}

				String CheckView = docTmp.getEmbedded(Arrays.asList("UserConFig", "viewshd"), "");
				String MST = docTmp.getEmbedded(Arrays.asList("EInvoiceDetail", "NDHDon", "NBan", "MST"), "");
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
						isDieuChinh);

				// baosPDF = jpUtils.createFinalInvoicetest(fileJP,
				// docTmp,"Y".equals(isConvert));
				if (baosPDF != null) {
					file = new File(dir, docTmp.get("_id") + "_final.pdf");
					try (OutputStream fileOuputStream = new FileOutputStream(file)) {
						baosPDF.writeTo(fileOuputStream);
						String threadName = Thread.currentThread().getName();
						// Tìm vị trí của dấu gạch ngang cuối cùng trong chuỗi
						int dashIndex = threadName.lastIndexOf("-");
						// Trích xuất phần tử sau dấu gạch ngang cuối cùng
						String numberString = threadName.substring(dashIndex + 1);
						// Chuyển đổi chuỗi thành số nguyên
						int number = Integer.parseInt(numberString);

						listpool.add(new PoolData(number, file.getAbsolutePath()));
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

}
