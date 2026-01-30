package vn.sesgroup.hddt.user.impl;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoField;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.mail.MessagingException;
import javax.net.ssl.HttpsURLConnection;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.w3c.dom.Node;

import com.api.message.JSONRoot;
import com.api.message.Msg;
import com.api.message.MsgHeader;
import com.api.message.MsgPage;
import com.api.message.MsgParam;
import com.api.message.MsgParams;
import com.api.message.MsgRsp;
import com.api.message.MspResponseStatus;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;

import vn.sesgroup.hddt.configuration.ConfigConnectMongo;
import vn.sesgroup.hddt.dto.FileInfo;
import vn.sesgroup.hddt.user.dao.AbstractDAO;
import vn.sesgroup.hddt.user.dao.CommonDAO;
import vn.sesgroup.hddt.user.service.JPUtils;
import vn.sesgroup.hddt.user.service.TCTNService;
import vn.sesgroup.hddt.utility.Constants;
import vn.sesgroup.hddt.utility.Json;
import vn.sesgroup.hddt.utility.SystemParams;

@Repository
@Transactional
public class CommonImpl extends AbstractDAO implements CommonDAO {
	private static final Logger log = LogManager.getLogger(CommonImpl.class);
	@Autowired
	MongoTemplate mongoTemplate;
	@Autowired
	ConfigConnectMongo cfg;
	@Autowired
	JPUtils jpUtils;
	@Autowired
	TCTNService tctnService;
	
	@Autowired RestTemplate restTemplate;

	@Override
	public MsgRsp getFullParams(JSONRoot jsonRoot) throws Exception {
		Msg msg = jsonRoot.getMsg();
		MsgHeader header = msg.getMsgHeader();
		MsgPage page = msg.getMsgPage();
		Object objData = msg.getObjData();

		MsgRsp rsp = new MsgRsp(header);
		MspResponseStatus responseStatus = null;

		MsgParams msgParams = null;
		try {
			msgParams = Json.serializer().fromJson(Json.serializer().toString(msg.getObjData()),
					new TypeReference<MsgParams>() {
					});
		} catch (Exception e) {
			log.error(" >>>>> An exception occurred!", e);
		}

		if (null == msgParams || 0 == msgParams.getParams().size()) {
			responseStatus = new MspResponseStatus(9999, "Không tìm thấy dữ liệu.");
			rsp.setResponseStatus(responseStatus);
			return rsp;
		}

		try {
			String code01 = "";
			String code02 = "";
			int currentYear = LocalDate.now().get(ChronoField.YEAR);

			HashMap<String, String> hashConds = null;
			Document docMatch = null;
			String IssuerId = msg.getMsgHeader().getIssuerId();
			List<Document> pipeline = new ArrayList<Document>();
			pipeline.add(new Document("$limit", 1));
			pipeline.add(new Document("$project", new Document("_id", 1)));
			for (MsgParam msgParam : msgParams.getParams()) {
				switch (msgParam.getParam()) {
				case "HuongDanSD":
					pipeline.add(new Document("$lookup",
							new Document("from", "HuongDanSD")
									.append("pipeline",
											Arrays.asList(new Document("$match", new Document("IsActive", true)),
													new Document("$project", new Document("_id", 0)),
													new Document("$sort", new Document("_id", 1))))
									.append("as", msgParam.getId())));
					break;
				case "DMMSTKhai":
					pipeline.add(
							new Document("$lookup",
									new Document("from", "DMMSTKhai")
											.append("pipeline",
													Arrays.asList(
															new Document("$match",
																	new Document("IsDelete",
																			new Document("$ne", true))),
															new Document("$project", new Document("_id", 0)),
															new Document("$sort", new Document("code", 1))))
											.append("as", msgParam.getId())));
					break;
				case "DMHTTKhai":
					pipeline.add(
							new Document("$lookup",
									new Document("from", "DMHTTKhai")
											.append("pipeline",
													Arrays.asList(
															new Document("$match",
																	new Document("IsDelete",
																			new Document("$ne", true))),
															new Document("$project", new Document("_id", 0)),
															new Document("$sort", new Document("code", 1))))
											.append("as", msgParam.getId())));
					break;
				case "DMTinhThanh":
					pipeline.add(
							new Document("$lookup",
									new Document("from", "DMTinhThanh")
											.append("pipeline",
													Arrays.asList(
															new Document("$match",
																	new Document("IsDelete",
																			new Document("$ne", true))),
															new Document("$project", new Document("_id", 0)),
															new Document("$sort", new Document("code", 1))))
											.append("as", msgParam.getId())));
					break;
				case "DMChiCucThue":
					if (null != msgParam.getConds() && msgParam.getConds().size() > 0) {
						for (HashMap<String, String> o : msgParam.getConds()) {
							if ("tinhthanh_ma".equals(o.get("cond"))) {
								code01 = o.get("condval");
							}
						}
					}
					pipeline.add(
							new Document("$lookup",
									new Document("from", "DMChiCucThue")
											.append("pipeline",
													Arrays.asList(
															new Document("$match",
																	new Document("IsDelete", new Document("$ne", true))
																			.append("tinhthanh_ma",
																					commons.regexEscapeForMongoQuery(
																							code01))),
															new Document("$project", new Document("_id", 0)),
															new Document("$sort", new Document("code", 1))))
											.append("as", msgParam.getId())));
					break;
				case "LoaiHD":
					pipeline.add(
							new Document("$lookup",
									new Document("from", "LoaiHD")
											.append("pipeline",
													Arrays.asList(
															new Document("$match",
																	new Document("IsDelete",
																			new Document("$ne", true))),
															new Document("$project", new Document("_id", 0)),
															new Document("$sort", new Document("code", 1))))
											.append("as", msgParam.getId())));
					break;
				case "PhoiCT":
					pipeline.add(
							new Document("$lookup",
									new Document("from", "DMTemplateCT")
											.append("pipeline",
													Arrays.asList(
															new Document("$match",
																	new Document("IsDelete",
																			new Document("$ne", true))),
															new Document("$project", new Document("_id", 0)),
															new Document("$sort", new Document("name", 1))))
											.append("as", msgParam.getId())));
					break;
				case "UserConFig":
					pipeline.add(new Document("$lookup",
							new Document("from", "UserConFig")
									.append("pipeline",
											Arrays.asList(
													new Document("$match",
															new Document("IsDelete", new Document("$ne", true))
																	.append("IssuerId", IssuerId)),
													new Document("$project", new Document("_id", 0))))
									.append("as", msgParam.getId())));
					break;
				case "KyHieuThongTu":
					pipeline.add(
							new Document("$lookup",
									new Document("from", "KyHieuThongTu")
											.append("pipeline",
													Arrays.asList(
															new Document("$match",
																	new Document("IsDelete",
																			new Document("$ne", true))),
															new Document("$project", new Document("_id", 0)),
															new Document("$sort", new Document("code", 1))))
											.append("as", msgParam.getId())));
					break;
				case "DMTemplates":
					if (null != msgParam.getConds() && msgParam.getConds().size() > 0) {
						for (HashMap<String, String> o : msgParam.getConds()) {
							if ("loaihd_ma".equals(o.get("cond"))) {
								code01 = o.get("condval");
							}
						}
					}
					pipeline.add(
							new Document("$lookup",
									new Document("from", "DMTemplates")
											.append("pipeline",
													Arrays.asList(
															new Document("$match",
																	new Document("IsDelete", new Document("$ne", true))
																			.append("loaihd_ma",
																					commons.regexEscapeForMongoQuery(
																							code01))),
															new Document("$project", new Document("_id", 0)),
															new Document("$sort", new Document("name", 1))))
											.append("as", msgParam.getId())));
					break;
				case "DMPaymentType":
					pipeline.add(
							new Document("$lookup",
									new Document("from", "DMPaymentType")
											.append("pipeline",
													Arrays.asList(
															new Document("$match",
																	new Document("IsDelete",
																			new Document("$ne", true))),
															new Document("$sort", new Document("order", 1)),
															new Document("$project",
																	new Document("_id", 0).append("code", 1)
																			.append("name", 1))))
											.append("as", msgParam.getId())));
					break;
				case "DMPTax":
					pipeline.add(
							new Document("$lookup",
									new Document("from", "DMPTax")
											.append("pipeline",
													Arrays.asList(
															new Document("$match",
																	new Document("IsDelete",
																			new Document("$ne", true))),
															new Document("$project",
																	new Document("_id", 0).append("code", 1)
																			.append("name", 1))))
											.append("as", msgParam.getId())));
					break;
				case "Issuer":
					pipeline.add(
							new Document("$lookup",
									new Document("from", "Issuer")
											.append("pipeline",
													Arrays.asList(
															new Document("$match",
																	new Document("IsDelete",
																			new Document("$ne", true))),
															new Document("$project",
																	new Document("_id", 0).append("TaxCode", 1))))
											.append("as", msgParam.getId())));
					break;
				case "DMMauSoKyHieu":
					code01 = header.getIssuerId();
					pipeline.add(new Document("$lookup",
							new Document("from", "DMMauSoKyHieu")
									.append("pipeline",
											Arrays.asList(
													new Document("$match",
															new Document("NamPhatHanh",
																	new Document("$lte", currentYear))
																	.append("IsDelete", new Document("$ne", true))
																	.append("IssuerId", code01)),
													new Document("$sort",
															new Document("NamPhatHanh", -1).append("_id", 1)),
													new Document("$project",
															new Document("_id", new Document("$toString", "$_id"))
																	.append("KHMSHDon", 1).append("KHHDon", 1))))
									.append("as", msgParam.getId())));
					break;
				case "DMMauSoKyHieuForCreate":
					code01 = header.getIssuerId();
					pipeline.add(
							new Document("$lookup",
									new Document("from", "DMMauSoKyHieu")
											.append("pipeline", Arrays.asList(
													new Document("$match",
															new Document("NamPhatHanh", currentYear)
																	.append("IsDelete", new Document("$ne", true))
																	.append("IssuerId", code01)),
													new Document("$sort", new Document("_id", 1)),
													new Document("$project",
															new Document("_id", new Document("$toString", "$_id"))
																	.append("KHMSHDon", 1).append("KHHDon", 1))))
											.append("as", msgParam.getId())));
					break;
				case "DMCurrencies":
					pipeline.add(
							new Document("$lookup",
									new Document("from", "DMCurrencies")
											.append("pipeline",
													Arrays.asList(
															new Document("$match",
																	new Document("IsDelete",
																			new Document("$ne", true))),
															new Document("$project", new Document("_id", 0)),
															new Document("$sort", new Document("order", 1))))
											.append("as", msgParam.getId())));
					break;
				case "DMStock":
					pipeline.add(new Document("$lookup",
							new Document("from", "DMStock")
									.append("pipeline", Arrays.asList(
											new Document("$match",
													new Document("IsDelete", new Document("$ne", true))
															.append("IssuerId", header.getIssuerId())),
											new Document("$addFields",
													new Document("Order",
															new Document("$ifNull", Arrays.asList("$Order", 999)))),
											new Document("$project", new Document("_id", 0)),
											new Document("$sort", new Document("Order", 1).append("Code", 1))))
									.append("as", msgParam.getId())));
					break;
				case "DMProductGroup":
					pipeline.add(new Document("$lookup",
							new Document("from", "DMProductGroup")
									.append("pipeline", Arrays.asList(
											new Document("$match",
													new Document("IsDelete", new Document("$ne", true))
															.append("IssuerId", header.getIssuerId())),
											new Document("$addFields",
													new Document("Order",
															new Document("$ifNull", Arrays.asList("$Order", 999)))),
											new Document("$project", new Document("_id", 0)),
											new Document("$sort", new Document("Order", 1).append("Code", 1))))
									.append("as", msgParam.getId())));
					break;
				case "DMCustomerGroup":
					pipeline.add(
							new Document("$lookup",
									new Document("from", "DMCustomerGroup")
											.append("pipeline",
													Arrays.asList(
															new Document("$match",
																	new Document("IsDelete",
																			new Document("$ne", true))),
															new Document("$project",
																	new Document("_id", 0).append("Description", 0)),
															new Document("$sort", new Document("Code", 1))))
											.append("as", msgParam.getId())));
					break;
				case "DMButton":
					pipeline.add(
							new Document("$lookup",
									new Document("from", "DMButton")
											.append("pipeline",
													Arrays.asList(
															new Document("$match",
																	new Document("IsDelete",
																			new Document("$ne", true))),
															new Document("$project", new Document("_id", 0)),
															new Document("$sort", new Document("Code", 1))))
											.append("as", msgParam.getId())));
					break;
				case "RolesRightManage":
					pipeline.add(
							new Document("$lookup", new Document("from", "RolesRightManage")
									.append("pipeline", Arrays.asList(
											new Document("$match",
													new Document("IssuerId", header.getIssuerId())
													.append("IsActive", true)
													.append("InfoCreated.CreateUserID", header.getUserId())),
											new Document("$addFields",
													new Document("_id", new Document("$toString", "$_id"))),
											new Document("$project",
													new Document("_id", 1).append("RoleId", 1).append("RoleName", 1)),
											new Document("$sort", new Document("Code", 1))))
									.append("as", msgParam.getId())));
					break;
				case "DMUserCheck":
					pipeline.add(new Document("$lookup",
							new Document("from", "Users")
									.append("pipeline",
											Arrays.asList(
													new Document("$match",
															new Document("IsDelete", new Document("$ne", true))
																	.append("IsActive", true).append("IsCheck", true)),
													new Document("$project", new Document("_id", 0)),
													new Document("$sort", new Document("UserName", 1))))
									.append("as", msgParam.getId())));
					break;

				case "TaxInvoice":
					pipeline.add(new Document("$lookup",
							new Document("from", "TaxInvoice")
									.append("pipeline",
											Arrays.asList(
													new Document("$match",
															new Document("IsDelete", new Document("$ne", true))
																	.append("IsActive", true)),
													new Document("$project", new Document("_id", 0)),
													new Document("$sort", new Document("STT", 1))))
									.append("as", msgParam.getId())));
					break;
				case "DMLoaiHD":
					pipeline.add(
						new Document("$lookup", 
							new Document("from", "DMLoaiHD")
							.append("pipeline", 
								Arrays.asList(
									new Document("$match", new Document("IsDelete", new Document("$ne", true))),
									new Document("$project", new Document("_id", 0)),
									new Document("$sort", new Document("Code", 1))
								)
							)
							.append("as", msgParam.getId())
						)
					);
					break;
				case "DMTaxRate":
					pipeline.add(
							new Document("$lookup",
									new Document("from", "DMTaxRate")
											.append("pipeline",
													Arrays.asList(
															new Document("$match",
																	new Document("IsDelete",
																			new Document("$ne", true))),
															new Document("$project",
																	new Document("_id", 0).append("code", 1)
																			.append("name", 1))))
											.append("as", msgParam.getId())));
					break;
				case "DMMSTNCN":
					pipeline.add(new Document("$lookup",
							new Document("from", "DMMSTNCN")
							.append("pipeline", 
									Arrays.asList(
									new Document("$match", 
											new Document("IsDelete", 
													new Document("$ne", true))
													.append("IssuerId", header.getIssuerId())
													.append("IsActive", true)
													.append("ConLai", new Document("$gt", 0))),
									new Document("$project", 
											new Document("_id", new Document("$toString", "$_id"))
											.append("KyHieu", 1)
											.append("Nam", 1)
											.append("ChungTu", 1))
									))
							.append("as", msgParam.getId())));
					break;
				case "DMSubAdmin":
					code01 = header.getIssuerId();
					pipeline.add(
							new Document("$lookup",
									new Document("from", "Users")
											.append("pipeline", Arrays.asList(
													new Document("$match",
															new Document("IsActive", true)
																	.append("IsDelete", false)
																	.append("IsRole", true)
																	.append("IssuerId", code01)),
													new Document("$sort", new Document("_id", 1)),
													new Document("$project",
															new Document("_id", new Document("$toString", "$_id"))
																	.append("UserName", 1).append("FullName", 1))))
											.append("as", msgParam.getId())));
					break;
				default:
					break;
				}
			}
			Document docTmp = null;
			Iterable<Document> cursor = mongoTemplate.getCollection("ApiLicenseKey").aggregate(pipeline)
					.allowDiskUse(true);
			Iterator<Document> iter = cursor.iterator();
			if (iter.hasNext()) {
				docTmp = iter.next();
			}

			if (null == docTmp) {
				responseStatus = new MspResponseStatus(9999, "Không tìm thấy dữ liệu.");
				rsp.setResponseStatus(responseStatus);
				return rsp;
			}

			responseStatus = new MspResponseStatus(0, "SUCCESS");
			rsp.setResponseStatus(responseStatus);
			rsp.setObjData(docTmp);
			return rsp;
		} catch (Exception e) {
			throw e;
		} finally {
		}
	}

	@Override
	public FileInfo printEInvoice(JSONRoot jsonRoot) throws Exception {
		FileInfo fileInfo = new FileInfo();

		Msg msg = jsonRoot.getMsg();
		MsgHeader header = msg.getMsgHeader();
		MsgPage page = msg.getMsgPage();

		Object objData = msg.getObjData();

		JsonNode jsonData = null;
		if (objData != null) {
			jsonData = Json.serializer().nodeFromObject(msg.getObjData());
		} else {
			return new FileInfo();
		}

		String _id = commons.getTextJsonNode(jsonData.at("/_id")).replaceAll("\\s", "");
		String isConvert = commons.getTextJsonNode(jsonData.at("/IsConvert")).replaceAll("\\s", "");
		/*
		 * db.getCollection('EInvoice').find( { IssuerId: '61b851ebb0228bba71fca2ec',
		 * _id: ObjectId("61c7cad6f8b593616ce03cc7"),IsDelete: {$ne: true} } )
		 * 
		 * db.getCollection('EInvoice').aggregate([ {$match: { IssuerId:
		 * '61b851ebb0228bba71fca2ec', _id:
		 * ObjectId("61c7cad6f8b593616ce03cc7"),IsDelete: {$ne: true} } }, {$lookup: {
		 * from: 'DMMauSoKyHieu', let: {vIssuerId: '$IssuerId', vMauSoHD:
		 * '$EInvoiceDetail.TTChung.MauSoHD'}, pipeline: [ {$match: { $expr: { $and: [
		 * {$eq: ['$$vIssuerId', '$IssuerId']}, {$eq: [{$toString: '$_id'},
		 * '$$vMauSoHD']} ] } } } ], as: 'DMMauSoKyHieu' } }, {$unwind: {path:
		 * '$DMMauSoKyHieu', preserveNullAndEmptyArrays: true}} ])
		 * 
		 */

		ObjectId objectId = null;
		try {
			objectId = new ObjectId(_id);

			Document docFind = new Document("IssuerId", header.getIssuerId()).append("_id", objectId).append("IsDelete",
					new Document("$ne", true));
			List<Document> pipeline = new ArrayList<Document>();
			pipeline.add(new Document("$match", docFind));
			pipeline.add(new Document("$lookup", new Document("from", "DMMauSoKyHieu")
					.append("let",
							new Document("vIssuerId", "$IssuerId").append("vMauSoHD",
									"$EInvoiceDetail.TTChung.MauSoHD"))
					.append("pipeline", Arrays.asList(new Document("$match", new Document("$expr", new Document("$and",
							Arrays.asList(new Document("$eq", Arrays.asList("$$vIssuerId", "$IssuerId")),
									new Document("$eq",
											Arrays.asList(new Document("$toString", "$_id"), "$$vMauSoHD"))))))))
					.append("as", "DMMauSoKyHieu")));
			pipeline.add(new Document("$unwind",
					new Document("path", "$DMMauSoKyHieu").append("preserveNullAndEmptyArrays", true)));
			pipeline.add(
					new Document("$lookup",
							new Document("from", "UserConFig")
									.append("pipeline",
											Arrays.asList(new Document("$match",
													new Document("IssuerId", header.getIssuerId()))))
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
			Iterable<Document> cursor = mongoTemplate.getCollection("EInvoice").aggregate(pipeline).allowDiskUse(true);
			Iterator<Document> iter = cursor.iterator();
			if (iter.hasNext()) {
				docTmp = iter.next();
			}

			if (null == docTmp) {
				docFind = new Document("_id", objectId).append("IsDelete", new Document("$ne", true));
				pipeline = new ArrayList<Document>();
				pipeline.add(new Document("$match", docFind));
				pipeline.add(
						new Document("$lookup",
								new Document("from", "UserConFig")
										.append("pipeline",
												Arrays.asList(new Document("$match",
														new Document("IssuerId", header.getIssuerId()))))
										.append("as", "UserConFig")));
				pipeline.add(new Document("$unwind",
						new Document("path", "$UserConFig").append("preserveNullAndEmptyArrays", true)));
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
						new Document("from", "PramLink")
								.append("pipeline",
										Arrays.asList(new Document("$match",
												new Document("$expr", new Document("IsDelete", false)))))
								.append("as", "PramLink")));
				pipeline.add(new Document("$unwind",
						new Document("path", "$PramLink").append("preserveNullAndEmptyArrays", true)));
				docTmp = null;
				cursor = mongoTemplate.getCollection("EInvoice").aggregate(pipeline).allowDiskUse(true);
				iter = cursor.iterator();
				if (iter.hasNext()) {
					docTmp = iter.next();
				}
			}

			if (null == docTmp || docTmp.get("DMMauSoKyHieu") == null) {
				return new FileInfo();
			}

			boolean isDieuChinh = false;
			boolean isThayThe = false;
			String check_status = docTmp.get("EInvoiceStatus", "");
			if (check_status.equals("REPLACED")) {
				isThayThe = true;
			}
			if (check_status.equals("ADJUSTED")) {
				isDieuChinh = true;
			}

			String ParamUSD = docTmp.getEmbedded(Arrays.asList("EInvoiceDetail", "TToan", "ParamUSD"), "");
			String link = docTmp.getEmbedded(Arrays.asList("PramLink", "LinkPortal"), "");
			String MST = docTmp.getEmbedded(Arrays.asList("EInvoiceDetail", "NDHDon", "NBan", "MST"), "");
			String ImgLogo = docTmp.getEmbedded(Arrays.asList("DMMauSoKyHieu", "Templates", "ImgLogo"), "");
			String ImgBackground = docTmp.getEmbedded(Arrays.asList("DMMauSoKyHieu", "Templates", "ImgBackground"), "");
			String ImgQA = docTmp.getEmbedded(Arrays.asList("DMMauSoKyHieu", "Templates", "ImgQA"), "");
			String ImgVien = docTmp.getEmbedded(Arrays.asList("DMMauSoKyHieu", "Templates", "ImgVien"), "");
			String Checknamecd = docTmp.getEmbedded(Arrays.asList("UserConFig", "NameCD"), "");
			String readMoneyInEnglish = docTmp.getEmbedded(Arrays.asList("UserConFig", "ReadMoneyInEnglish"), "");
			String CheckView = docTmp.getEmbedded(Arrays.asList("UserConFig", "viewshd"), "") + "," + Checknamecd;
			String dir = docTmp.get("Dir", "");
			String signStatusCode = docTmp.get("SignStatusCode", "");
			String eInvoiceStatus = docTmp.get("EInvoiceStatus", "");
			String secureKey = docTmp.get("SecureKey", "");
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
			int numberCharsInRow = docTmp.getEmbedded(Arrays.asList("DMMauSoKyHieu", "Templates", "CharsInRow"), 50);

			File fileJP = new File(SystemParams.DIR_E_INVOICE_TEMPLATE, fileNameJP);

			ByteArrayOutputStream baosPDF = null;

			baosPDF = jpUtils.createFinalInvoice(fileJP, doc, secureKey, CheckView, readMoneyInEnglish, numberRowInPage, numberRowInPageMultiPage,
					numberCharsInRow, MST, link, ParamUSD,
					Paths.get(SystemParams.DIR_E_INVOICE_TEMPLATE, "images", MST, ImgLogo).toString(),
					Paths.get(SystemParams.DIR_E_INVOICE_TEMPLATE, "images", MST, ImgBackground).toString(),
					Paths.get(SystemParams.DIR_E_INVOICE_TEMPLATE, "images", MST, ImgQA).toString(),
					Paths.get(SystemParams.DIR_E_INVOICE_TEMPLATE, "images", MST, ImgVien).toString(),

					"Y".equals(isConvert), Constants.INVOICE_STATUS.DELETED.equals(eInvoiceStatus), isThayThe,
					isDieuChinh
					, Constants.INVOICE_STATUS.ERROR_CQT.equals(eInvoiceStatus)
					);

			fileInfo.setFileName("EInvoice.pdf");
			fileInfo.setContentFile(baosPDF.toByteArray());

		} catch (NullPointerException | MessagingException e) {

			return new FileInfo();
		}

		return fileInfo;
	}

	@Override
	public FileInfo printEinvoiceAll(JSONRoot jsonRoot) throws Exception {
		FileInfo fileInfo = new FileInfo();
		List<String> ids = null;
		Msg msg = jsonRoot.getMsg();
		MsgHeader header = msg.getMsgHeader();
		MsgPage page = msg.getMsgPage();
		ByteArrayOutputStream baosPDF = null;
		List<String> listFileNamePdfFinal = new ArrayList<>();
		HashMap<String, String> hItem = null;
		ArrayList<HashMap<String, String>> arrayInfoInvoice = new ArrayList<>();
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		File file_zip = null;

		Object objData = msg.getObjData();

		try {

			JsonNode jsonData = null;
			if (objData != null) {
				jsonData = Json.serializer().nodeFromObject(msg.getObjData());
			}
			String _id = "";
			String _token = commons.getTextJsonNode(jsonData.at("/_token")).replaceAll("\\s", "");
			String isConvert = commons.getTextJsonNode(jsonData.at("/IsConvert")).replaceAll("\\s", "");

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
					cursor = mongoTemplate.getCollection("EInvoice").aggregate(pipeline).allowDiskUse(true);
					iter = cursor.iterator();
					if (iter.hasNext()) {
						docTmp = iter.next();
					}
				}

				if (null == docTmp || docTmp.get("DMMauSoKyHieu") == null) {
					return new FileInfo();
				}
				String link = docTmp.getEmbedded(Arrays.asList("PramLink", "LinkPortal"), "");
				boolean isDieuChinh = "2".equals(docTmp.getEmbedded(Arrays.asList("HDSS", "TCTBao"), ""));
				boolean isThayThe = false;
				String check_status = docTmp.get("EInvoiceStatus", "");
				if (check_status.equals("REPLACED")) {
					isThayThe = "3".equals(docTmp.getEmbedded(Arrays.asList("HDSS", "TCTBao"), ""));
				}

				String CheckView = docTmp.getEmbedded(Arrays.asList("UserConFig", "viewshd"), "");
				String readMoneyInEnglish = docTmp.getEmbedded(Arrays.asList("UserConFig", "ReadMoneyInEnglish"), "");
				String MST = docTmp.getEmbedded(Arrays.asList("EInvoiceDetail", "NDHDon", "NBan", "MST"), "");
				String ImgLogo = docTmp.getEmbedded(Arrays.asList("DMMauSoKyHieu", "Templates", "ImgLogo"), "");
				String ImgBackground = docTmp.getEmbedded(Arrays.asList("DMMauSoKyHieu", "Templates", "ImgBackground"),
						"");
				String ImgQA = docTmp.getEmbedded(Arrays.asList("DMMauSoKyHieu", "Templates", "ImgQA"), "");
				String ImgVien = docTmp.getEmbedded(Arrays.asList("DMMauSoKyHieu", "Templates", "ImgVien"), "");

				String mskh = docTmp.getEmbedded(Arrays.asList("EInvoiceDetail", "TTChung", "KHMSHDon"), "")
						+ docTmp.getEmbedded(Arrays.asList("EInvoiceDetail", "TTChung", "KHHDon"), "");
				Integer shd = docTmp.getEmbedded(Arrays.asList("EInvoiceDetail", "TTChung", "SHDon"), 0);
				String ParamUSD = docTmp.getEmbedded(Arrays.asList("EInvoiceDetail", "TToan", "ParamUSD"), "");

				String dir = docTmp.get("Dir", "");
				String signStatusCode = docTmp.get("SignStatusCode", "");
				String eInvoiceStatus = docTmp.get("EInvoiceStatus", "");
				String secureKey = docTmp.get("SecureKey", "");
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
				baosPDF = jpUtils.createFinalInvoice(fileJP, doc, secureKey, CheckView, readMoneyInEnglish, numberRowInPage, numberRowInPageMultiPage,
						numberCharsInRow, MST, link, ParamUSD,
						Paths.get(SystemParams.DIR_E_INVOICE_TEMPLATE, "images", MST, ImgLogo).toString(),
						Paths.get(SystemParams.DIR_E_INVOICE_TEMPLATE, "images", MST, ImgBackground).toString(),
						Paths.get(SystemParams.DIR_E_INVOICE_TEMPLATE, "images", MST, ImgQA).toString(),
						Paths.get(SystemParams.DIR_E_INVOICE_TEMPLATE, "images", MST, ImgVien).toString(),

						"Y".equals(isConvert), Constants.INVOICE_STATUS.DELETED.equals(eInvoiceStatus), isThayThe,
						isDieuChinh, false);

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
				LocalDateTime time_dem  = LocalDateTime.now();
				String time = time_dem.format(format_time);
				String name_company = removeAccent(header.getUserFullName());
				System.out.println(time +" "+name_company+" vua xuat PDF hang loat hoa don VAT.");
				
				return fileInfo;
			}
		} catch (NullPointerException | MessagingException | UnsupportedEncodingException e) {
			return new FileInfo();
		}

	}

	@Override
	public FileInfo einvoiceXml(JSONRoot jsonRoot) throws Exception {
		FileInfo fileInfo = new FileInfo();
		ArrayList<HashMap<String, String>> arrayInfoInvoice = new ArrayList<>();
		HashMap<String, String> hItem = null;
		File file_zip = null;
		String fileName = "";
		List<String> ids = null;
		Msg msg = jsonRoot.getMsg();
		MsgHeader header = msg.getMsgHeader();
		MsgPage page = msg.getMsgPage();
		ByteArrayOutputStream baosPDF = null;
		List<String> listFileNamePdfFinal = new ArrayList<>();
		Object objData = msg.getObjData();
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		JsonNode jsonData = null;

		try {

			if (objData != null) {
				jsonData = Json.serializer().nodeFromObject(msg.getObjData());
			}
			String _id = "";
			String _token = commons.getTextJsonNode(jsonData.at("/_token")).replaceAll("\\s", "");
//		ids = null;
//		try {
//			ids = Json.serializer().fromJson(commons.decodeBase64ToString(_token), new TypeReference<List<String>>() {
//			});
//		}catch(Exception e) {}

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

//		for(int i = 0; i< ids.size(); i++ ) {
//			_id = ids.get(i) ;
				String isConvert = commons.getTextJsonNode(jsonData.at("/IsConvert")).replaceAll("\\s", "");
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

				Document docTmp = null;
				Iterable<Document> cursor = mongoTemplate.getCollection("EInvoice").aggregate(pipeline)
						.allowDiskUse(true);
				Iterator<Document> iter = cursor.iterator();
				if (iter.hasNext()) {
					docTmp = iter.next();
				}
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

					docTmp = null;
					cursor = mongoTemplate.getCollection("EInvoice").aggregate(pipeline).allowDiskUse(true);
					iter = cursor.iterator();
					if (iter.hasNext()) {
						docTmp = iter.next();
					}
				}
				if (null == docTmp || docTmp.get("DMMauSoKyHieu") == null) {
					return new FileInfo();
				}
				String mskh = docTmp.getEmbedded(Arrays.asList("EInvoiceDetail", "TTChung", "KHMSHDon"), "")
						+ docTmp.getEmbedded(Arrays.asList("EInvoiceDetail", "TTChung", "KHHDon"), "");
				Integer shd = docTmp.getEmbedded(Arrays.asList("EInvoiceDetail", "TTChung", "SHDon"), 0);
				String namexml = mskh + "_" + shd + ".xml";
				boolean isDieuChinh = "2".equals(docTmp.getEmbedded(Arrays.asList("HDSS", "TCTBao"), ""));
				boolean isThayThe = "3".equals(docTmp.getEmbedded(Arrays.asList("HDSS", "TCTBao"), ""));

				String MST = docTmp.getEmbedded(Arrays.asList("EInvoiceDetail", "NDHDon", "NBan", "MST"), "");
				String ImgLogo = docTmp.getEmbedded(Arrays.asList("DMMauSoKyHieu", "Templates", "ImgLogo"), "");
				String ImgBackground = docTmp.getEmbedded(Arrays.asList("DMMauSoKyHieu", "Templates", "ImgBackground"),
						"");

				String dir = docTmp.get("Dir", "");
				String signStatusCode = docTmp.get("SignStatusCode", "");
				String eInvoiceStatus = docTmp.get("EInvoiceStatus", "");
				String MCCQT = docTmp.get("MCCQT", "");
				fileName = _id + ".xml";
				if ("SIGNED".equals(signStatusCode) && !"".equals(MCCQT)) {
					fileName = _id + "_" + MCCQT + ".xml";
					String fileName_ = _id + "_" + MCCQT + "_" + shd + ".xml";
					/* CHECK MCCQT GET DATA XML */

					File file_xml = new File(dir, fileName);

					org.w3c.dom.Document doc_xml = commons.fileToDocument(file_xml);

					XPath xPath_xml = XPathFactory.newInstance().newXPath();
					Node nodeHDon = null;
					for (int J = 1; J <= 20; J++) {
						nodeHDon = (Node) xPath_xml.evaluate("/KetQuaTraCuu/DuLieu/TDiep[" + J + "]/DLieu/HDon",
								doc_xml, XPathConstants.NODE);
						if (nodeHDon != null)
							break;
					}
					/* FILE CHUA DUOC DUI DEN CO QUAN THUE */
					if (null == nodeHDon) {
						nodeHDon = (Node) xPath_xml.evaluate("/HDon", doc_xml, XPathConstants.NODE);
					}

					DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
					DocumentBuilder builder = factory.newDocumentBuilder();
					org.w3c.dom.Document rTCTN_HDON = builder.newDocument();
					rTCTN_HDON.appendChild(rTCTN_HDON.importNode(nodeHDon, true));
					/* END CHECK GET DATA IN RESULT IN FILE XML TO TAX */
					boolean boo_ = false;
					boo_ = commons.docW3cToFile(rTCTN_HDON, dir, fileName_);

					if (boo_ == true) {
						fileName = fileName_;
					}
					/* END CHECK MCCQT GET DATA XML */
				} else {
					if ("SIGNED".equals(signStatusCode)) {
						fileName = _id + "_signed.xml";
					}
				}
				// namexml

				File file = new File(dir, fileName);
				File tam = new File(dir, namexml);
				copyFileUsingStream(file, tam);

				hItem = new HashMap<>();
				hItem.put("UrlFile", tam.getAbsolutePath());
				arrayInfoInvoice.add(hItem);
			}
			if (arrayInfoInvoice.size() == 0) {
				return fileInfo;
			}
			if (arrayInfoInvoice.size() == 1) {
				hItem = arrayInfoInvoice.get(0);
				file_zip = new File(hItem.get("UrlFile"));
				fileInfo.setContentFile(Files.readAllBytes(file_zip.toPath()));
				fileInfo.setFileName(fileName);
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
			}

			return fileInfo;

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

	/*
	 * db.getCollection('DMProduct').find({ IssuerId: '61b851ebb0228bba71fca2ec',
	 * IsDelete: {$ne: true}, $or: [ {Name: {$regex: '', $options: 'i'}}, {Code:
	 * {$regex: '', $options: 'i'}} ] }).sort({Name: 1, Code: 1, _id: 1}) .limit(20)
	 */

	@Override
	public MsgRsp getAutoCompleteProducts(JSONRoot jsonRoot) throws Exception {
		Msg msg = jsonRoot.getMsg();
		MsgHeader header = msg.getMsgHeader();
		MsgPage page = msg.getMsgPage();
		Object objData = msg.getObjData();

		JsonNode jsonData = null;
		String dataInput = "";
		if (objData != null) {
			jsonData = Json.serializer().nodeFromObject(msg.getObjData());
			dataInput = commons.getTextJsonNode(jsonData.at("/DataInput")).trim().replaceAll("\\s+", " ");
		}

		dataInput = commons.regexEscapeForMongoQuery(dataInput);
		Document docFind = new Document("IssuerId", header.getIssuerId()).append("IsDelete", new Document("$ne", true))
				.append("$or",
						Arrays.asList(new Document("Name", new Document("$regex", dataInput).append("$options", "i")),
								new Document("Code", new Document("$regex", dataInput).append("$options", "i"))));

//		 docFind = new Document("IsDelete", new Document("$ne", true))
//				.append("$or", 
//					Arrays.asList(
//						new Document("Name", new Document("$regex", dataInput).append("$options", "i")),
//						new Document("Code", new Document("$regex", dataInput).append("$options", "i"))
//					)
//				);

		MsgRsp rsp = new MsgRsp(header);
		MspResponseStatus responseStatus = null;

		ArrayList<HashMap<String, Object>> datasReturn = new ArrayList<HashMap<String, Object>>();
		HashMap<String, Object> hItem = null;

		Document docTmp = null;
		Iterable<Document> cursor = mongoTemplate.getCollection("DMProduct").find(docFind)
				.sort(new Document("Name", 1).append("Code", 1).append("_id", 1)).limit(20);
		Iterator<Document> iter = cursor.iterator();
		while (iter.hasNext()) {
			docTmp = iter.next();

			hItem = new HashMap<String, Object>();
			hItem.put("Code", docTmp.get("Code"));
			hItem.put("Name", docTmp.get("Name"));
			hItem.put("Price", docTmp.get("Price"));
			hItem.put("Unit", docTmp.get("Unit"));
			hItem.put("VatRate", docTmp.get("VatRate"));
			hItem.put("Stock", docTmp.get("Stock"));

			datasReturn.add(hItem);
		}
		responseStatus = new MspResponseStatus(0, "SUCCESS");
		rsp.setResponseStatus(responseStatus);
		rsp.setObjData(datasReturn);
		return rsp;
	}

	@Override
	public MsgRsp listSearchCustomer(JSONRoot jsonRoot) throws Exception {
		Msg msg = jsonRoot.getMsg();
		MsgHeader header = msg.getMsgHeader();
		MsgPage page = msg.getMsgPage();
		Object objData = msg.getObjData();

		String taxCode = "";
		String companyName = "";
		String customerName = "";

		JsonNode jsonData = null;
		if (objData != null) {
			jsonData = Json.serializer().nodeFromObject(objData);
			taxCode = commons.getTextJsonNode(jsonData.at("/TaxCode")).trim().replaceAll("\\s+", " ");
			companyName = commons.getTextJsonNode(jsonData.at("/CompanyName")).trim().replaceAll("\\s+", " ");
			customerName = commons.getTextJsonNode(jsonData.at("/CustomerName")).trim().replaceAll("\\s+", " ");
		}

		MsgRsp rsp = new MsgRsp(header);
		MspResponseStatus responseStatus = null;

		ObjectId objectId = null;
		Document docTmp = null;
		Iterable<Document> cursor = null;
		Iterator<Document> iter = null;
		List<Document> pipeline = new ArrayList<Document>();

		Document docMatch = new Document("IssuerId", header.getIssuerId()).append("IsDelete",
				new Document("$ne", true));
		if (!"".equals(taxCode))
			docMatch.append("TaxCode",
					new Document("$regex", commons.regexEscapeForMongoQuery(taxCode)).append("$options", "i"));
		if (!"".equals(companyName))
			docMatch.append("CompanyName",
					new Document("$regex", commons.regexEscapeForMongoQuery(companyName)).append("$options", "i"));
		if (!"".equals(customerName))
			docMatch.append("CustomerName",
					new Document("$regex", commons.regexEscapeForMongoQuery(customerName)).append("$options", "i"));

		pipeline = new ArrayList<Document>();
		pipeline.add(new Document("$match", docMatch));
		pipeline.add(new Document("$sort", new Document("Stock", 1).append("Code", 1).append("_id", 1)));
		pipeline.addAll(createFacetForSearchNotSort(page));

		cursor = mongoTemplate.getCollection("DMCustomer").aggregate(pipeline).allowDiskUse(true);
		iter = cursor.iterator();
		if (iter.hasNext()) {
			docTmp = iter.next();
		}

		rsp = new MsgRsp(header);
		responseStatus = null;
		if (null == docTmp) {
			responseStatus = new MspResponseStatus(9999, Constants.MAP_ERROR.get(9999));
			rsp.setResponseStatus(responseStatus);
			return rsp;
		}

		page.setTotalRows(docTmp.getInteger("total", 0));
		rsp.setMsgPage(page);

		List<Document> rows = null;
		if (docTmp.get("data") != null && docTmp.get("data") instanceof List) {
			rows = docTmp.getList("data", Document.class);
		}

		ArrayList<HashMap<String, Object>> rowsReturn = new ArrayList<HashMap<String, Object>>();
		HashMap<String, Object> hItem = null;
		if (null != rows) {
			for (Document doc : rows) {
				objectId = (ObjectId) doc.get("_id");
				hItem = new HashMap<String, Object>();
				hItem.put("_id", objectId.toString());
				hItem.put("TaxCode", doc.get("TaxCode"));
				hItem.put("CustomerCode", doc.get("CustomerCode"));
				hItem.put("CompanyName", doc.get("CompanyName"));
				hItem.put("CustomerName", doc.get("CustomerName"));
				hItem.put("Address", doc.get("Address"));
				hItem.put("Email", doc.get("Email"));
				hItem.put("EmailCC", doc.get("EmailCC"));
				hItem.put("Phone", doc.get("Phone"));
				hItem.put("AccountNumber", doc.get("AccountNumber"));
				hItem.put("AccountBankName", doc.get("AccountBankName"));

				hItem.put("Province", doc.get("Province"));
				hItem.put("CustomerGroup1", doc.get("CustomerGroup1"));
				hItem.put("CustomerGroup2", doc.get("CustomerGroup2"));
				hItem.put("CustomerGroup3", doc.get("CustomerGroup3"));

				hItem.put("InfoCreated", doc.get("InfoCreated"));
				rowsReturn.add(hItem);
			}
		}

		responseStatus = new MspResponseStatus(0, "SUCCESS");
		rsp.setResponseStatus(responseStatus);

		HashMap<String, Object> mapDataR = new HashMap<String, Object>();
		mapDataR.put("rows", rowsReturn);
		rsp.setObjData(mapDataR);
		return rsp;
	}

	/*
	 * db.getCollection('EInvoice').aggregate([ {$match: { 'IssuerId' :
	 * '61b851ebb0228bba71fca2ec', IsDelete: {$ne: true}, SignStatusCode: 'SIGNED',
	 * EInvoiceStatus: 'COMPLETE', MCCQT: {$exists: true, $ne: null},
	 * 'EInvoiceDetail.TTChung.MauSoHD': '61bf1c62b0228bba7101b8c0',
	 * 'EInvoiceDetail.TTChung.SHDon': 1, 'EInvoiceDetail.TTChung.NLap': {$gte:
	 * ISODate("2021-12-26T00:00:00.000Z"), $lt:
	 * ISODate("2021-12-30T00:00:00.000Z")}, 'EInvoiceDetail.NDHDon.NMua.MST':
	 * '11111111', $or: [ {'EInvoiceDetail.NDHDon.NMua.Ten': {$regex: '',
	 * '$options': 'i'}}, {'EInvoiceDetail.NDHDon.NMua.HVTNMHang': {$regex: '',
	 * '$options': 'i'}} ] } } , {$sort: {'EInvoiceDetail.TTChung.MauSoHD':-1,
	 * 'SHDon': -1, _id: -1}} , {$facet: { meta: [{$count: 'total'}], data: [{$skip:
	 * 0}, {$limit: 10}] } } , {$unwind: '$meta'} ,
	 * {$project:{'total':'$meta.total',data:1}} ]);
	 */
	@Override
	public MsgRsp listEInvoiceSigned(JSONRoot jsonRoot) throws Exception {
		Msg msg = jsonRoot.getMsg();
		MsgHeader header = msg.getMsgHeader();
		MsgPage page = msg.getMsgPage();
		Object objData = msg.getObjData();

		String mauSoHdon = "";
		String soHoaDon = "";
		String fromDate = "";
		String toDate = "";
		String nbanMst = "";
		String nbanTen = "";

		JsonNode jsonData = null;
		if (objData != null) {
			jsonData = Json.serializer().nodeFromObject(objData);

			mauSoHdon = commons.getTextJsonNode(jsonData.at("/MauSoHdon")).replaceAll("\\s", "");
			soHoaDon = commons.getTextJsonNode(jsonData.at("/SoHoaDon")).replaceAll("\\s", "");
			fromDate = commons.getTextJsonNode(jsonData.at("/FromDate")).replaceAll("\\s", "");
			toDate = commons.getTextJsonNode(jsonData.at("/ToDate")).replaceAll("\\s", "");
			nbanMst = commons.getTextJsonNode(jsonData.at("/NbanMst")).trim().replaceAll("\\s+", " ");
			nbanTen = commons.getTextJsonNode(jsonData.at("/NbanTen")).trim().replaceAll("\\s+", " ");
		}

		MsgRsp rsp = new MsgRsp(header);
		MspResponseStatus responseStatus = null;

		ObjectId objectId = null;
		Document docTmp = null;
		Document docTmp1 = null;
		Document docTmp2 = null;
		Document docTmp3 = null;
		Iterable<Document> cursor = null;
		Iterable<Document> cursor1 = null;
		Iterator<Document> iter = null;
		Iterator<Document> iter1 = null;
		Iterable<Document> cursor2 = null;
		Iterable<Document> cursor3 = null;
		Iterator<Document> iter2 = null;
		Iterator<Document> iter3 = null;
		List<Document> pipeline = new ArrayList<Document>();

		LocalDate dateFrom = null;
		LocalDate dateTo = null;
		Document docMatchDate = null;

		dateFrom = "".equals(fromDate) || !commons.checkLocalDate(fromDate, Constants.FORMAT_DATE.FORMAT_DATE_WEB)
				? null
				: commons.convertStringToLocalDate(fromDate, Constants.FORMAT_DATE.FORMAT_DATE_WEB);
		dateTo = "".equals(toDate) || !commons.checkLocalDate(toDate, Constants.FORMAT_DATE.FORMAT_DATE_WEB) ? null
				: commons.convertStringToLocalDate(toDate, Constants.FORMAT_DATE.FORMAT_DATE_WEB);
		if (null != dateTo)
			dateTo = dateTo.plus(1, ChronoUnit.DAYS);
		if (null != dateFrom || null != dateTo) {
			docMatchDate = new Document();
			if (null != dateFrom)
				docMatchDate.append("$gte", dateFrom);
			if (null != dateTo)
				docMatchDate.append("$lt", dateTo);
		}

		Document docMatch = new Document("IssuerId", header.getIssuerId()).append("IsDelete", new Document("$ne", true))
				.append("SignStatusCode", "SIGNED")
				.append("EInvoiceStatus",
						new Document("$in",
								Arrays.asList(Constants.INVOICE_STATUS.COMPLETE, Constants.INVOICE_STATUS.ADJUSTED, Constants.INVOICE_STATUS.REPLACED)))

				.append("MCCQT", new Document("$exists", true).append("$ne", null));
		if (!"".equals(mauSoHdon))
			docMatch.append("EInvoiceDetail.TTChung.MauSoHD", commons.regexEscapeForMongoQuery(mauSoHdon));
		if (!"".equals(soHoaDon))
			docMatch.append("EInvoiceDetail.TTChung.SHDon", commons.stringToInteger(soHoaDon));
		if (null != docMatchDate)
			docMatch.append("EInvoiceDetail.TTChung.NLap", docMatchDate);
		if (!"".equals(nbanMst))
			docMatch.append("EInvoiceDetail.NDHDon.NMua.MST",
					new Document("$regex", commons.regexEscapeForMongoQuery(nbanMst)).append("$options", "i"));
		if (!"".equals(nbanTen)) {
			docMatch.append("$or",
					Arrays.asList(
							new Document("EInvoiceDetail.NDHDon.NMua.Ten",
									new Document("$regex", commons.regexEscapeForMongoQuery(nbanTen)).append("$options",
											"i")),
							new Document("EInvoiceDetail.NDHDon.NMua.HVTNMHang",
									new Document("$regex", commons.regexEscapeForMongoQuery(nbanTen)).append("$options",
											"i"))));
		}
		pipeline = new ArrayList<Document>();
		pipeline.add(new Document("$match", docMatch));

		pipeline.add(new Document("$sort",
				new Document("EInvoiceDetail.TTChung.MauSoHD", -1).append("SHDon", -1).append("_id", -1)));
		pipeline.addAll(createFacetForSearchNotSort(page));

		cursor = mongoTemplate.getCollection("EInvoice").aggregate(pipeline).allowDiskUse(true);
		cursor1 = mongoTemplate.getCollection("EInvoicePXK").aggregate(pipeline).allowDiskUse(true);
		cursor2 = mongoTemplate.getCollection("EInvoiceBH").aggregate(pipeline).allowDiskUse(true);
		cursor3 = mongoTemplate.getCollection("EInvoicePXKDL").aggregate(pipeline).allowDiskUse(true);
		iter = cursor.iterator();
		iter1 = cursor1.iterator();
		iter2 = cursor2.iterator();
		iter3 = cursor3.iterator();
		if (iter.hasNext()) {
			docTmp = iter.next();
		}
		if (iter1.hasNext()) {
			docTmp1 = iter1.next();

		}
		if (iter2.hasNext()) {
			docTmp2 = iter2.next();
		}
		if (iter3.hasNext()) {
			docTmp3 = iter3.next();

		}
		rsp = new MsgRsp(header);
		responseStatus = null;
		ArrayList<HashMap<String, Object>> rowsReturn = new ArrayList<HashMap<String, Object>>();
		HashMap<String, Object> hItem = null;
		if (null != docTmp) {
			page.setTotalRows(docTmp.getInteger("total", 0));
			rsp.setMsgPage(page);
			List<Document> rows = null;
			if (docTmp.get("data") != null && docTmp.get("data") instanceof List) {
				rows = docTmp.getList("data", Document.class);
			}
			if (null != rows) {
				for (Document doc : rows) {
					objectId = (ObjectId) doc.get("_id");

					hItem = new HashMap<String, Object>();
					hItem.put("_id", objectId.toString());
					hItem.put("EInvoiceStatus", doc.get("EInvoiceStatus"));
					hItem.put("SignStatusCode", doc.get("SignStatusCode"));
					hItem.put("MCCQT", doc.get("MCCQT"));

					hItem.put("EInvoiceDetail", doc.get("EInvoiceDetail"));
					hItem.put("InfoCreated", doc.get("InfoCreated"));
					hItem.put("LDo", doc.get("LDo"));

					rowsReturn.add(hItem);
				}
			}
		}
		if (null != docTmp1) {
			page.setTotalRows(docTmp1.getInteger("total", 0));
			rsp.setMsgPage(page);
			List<Document> rows1 = null;
			if (docTmp1.get("data") != null && docTmp1.get("data") instanceof List) {
				rows1 = docTmp1.getList("data", Document.class);
			}

			if (null != rows1) {
				for (Document doc : rows1) {
					objectId = (ObjectId) doc.get("_id");

					hItem = new HashMap<String, Object>();
					hItem.put("_id", objectId.toString());
					hItem.put("EInvoiceStatus", doc.get("EInvoiceStatus"));
					hItem.put("SignStatusCode", doc.get("SignStatusCode"));
					hItem.put("MCCQT", doc.get("MCCQT"));

					hItem.put("EInvoiceDetail", doc.get("EInvoiceDetail"));
					hItem.put("InfoCreated", doc.get("InfoCreated"));
					hItem.put("LDo", doc.get("LDo"));

					rowsReturn.add(hItem);
				}
			}
		}
		if (null != docTmp2) {
			page.setTotalRows(docTmp2.getInteger("total", 0));
			rsp.setMsgPage(page);
			List<Document> rows1 = null;
			if (docTmp2.get("data") != null && docTmp2.get("data") instanceof List) {
				rows1 = docTmp2.getList("data", Document.class);
			}

			if (null != rows1) {
				for (Document doc : rows1) {
					objectId = (ObjectId) doc.get("_id");

					hItem = new HashMap<String, Object>();
					hItem.put("_id", objectId.toString());
					hItem.put("EInvoiceStatus", doc.get("EInvoiceStatus"));
					hItem.put("SignStatusCode", doc.get("SignStatusCode"));
					hItem.put("MCCQT", doc.get("MCCQT"));

					hItem.put("EInvoiceDetail", doc.get("EInvoiceDetail"));
					hItem.put("InfoCreated", doc.get("InfoCreated"));
					hItem.put("LDo", doc.get("LDo"));

					rowsReturn.add(hItem);
				}
			}
		}
		if (null != docTmp3) {
			page.setTotalRows(docTmp3.getInteger("total", 0));
			rsp.setMsgPage(page);
			List<Document> rows1 = null;
			if (docTmp3.get("data") != null && docTmp3.get("data") instanceof List) {
				rows1 = docTmp3.getList("data", Document.class);
			}

			if (null != rows1) {
				for (Document doc : rows1) {
					objectId = (ObjectId) doc.get("_id");

					hItem = new HashMap<String, Object>();
					hItem.put("_id", objectId.toString());
					hItem.put("EInvoiceStatus", doc.get("EInvoiceStatus"));
					hItem.put("SignStatusCode", doc.get("SignStatusCode"));
					hItem.put("MCCQT", doc.get("MCCQT"));

					hItem.put("EInvoiceDetail", doc.get("EInvoiceDetail"));
					hItem.put("InfoCreated", doc.get("InfoCreated"));
					hItem.put("LDo", doc.get("LDo"));

					rowsReturn.add(hItem);
				}
			}
		}
		responseStatus = new MspResponseStatus(0, "SUCCESS");
		rsp.setResponseStatus(responseStatus);

		HashMap<String, Object> mapDataR = new HashMap<String, Object>();
		mapDataR.put("rows", rowsReturn);
		rsp.setObjData(mapDataR);
		return rsp;
	}

	@Override
	public FileInfo printExport(JSONRoot jsonRoot) throws Exception {
		FileInfo fileInfo = new FileInfo();

		Msg msg = jsonRoot.getMsg();
		MsgHeader header = msg.getMsgHeader();
		MsgPage page = msg.getMsgPage();

		Object objData = msg.getObjData();

		JsonNode jsonData = null;

		try {
			if (objData != null) {
				jsonData = Json.serializer().nodeFromObject(msg.getObjData());
			} else {
				return new FileInfo();
			}
			ObjectId objectIdIssu = null;
			try {
				objectIdIssu = new ObjectId(header.getIssuerId());
			} catch (Exception e) {
			}
			String _id = commons.getTextJsonNode(jsonData.at("/_id")).replaceAll("\\s", "");
			String isConvert = commons.getTextJsonNode(jsonData.at("/IsConvert")).replaceAll("\\s", "");
			ObjectId objectId = null;
			try {
				objectId = new ObjectId(_id);
			} catch (Exception e) {
			}

			Document docFind = new Document("IssuerId", header.getIssuerId()).append("_id", objectId).append("IsDelete",
					new Document("$ne", true));
			List<Document> pipeline = new ArrayList<Document>();
			pipeline.add(new Document("$match", docFind));
			pipeline.add(new Document("$lookup", new Document("from", "DMMauSoKyHieu")
					.append("let",
							new Document("vIssuerId", "$IssuerId").append("vMauSoHD",
									"$EInvoiceDetail.TTChung.MauSoHD"))
					.append("pipeline", Arrays.asList(new Document("$match", new Document("$expr", new Document("$and",
							Arrays.asList(new Document("$eq", Arrays.asList("$$vIssuerId", "$IssuerId")),
									new Document("$eq",
											Arrays.asList(new Document("$toString", "$_id"), "$$vMauSoHD"))))))))
					.append("as", "DMMauSoKyHieu")));
			pipeline.add(new Document("$unwind",
					new Document("path", "$DMMauSoKyHieu").append("preserveNullAndEmptyArrays", true)));
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
			pipeline.add(
					new Document("$lookup",
							new Document("from", "UserConFig")
									.append("pipeline",
											Arrays.asList(new Document("$match",
													new Document("IssuerId", header.getIssuerId()))))
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
			Iterable<Document> cursor = mongoTemplate.getCollection("EInvoicePXK").aggregate(pipeline)
					.allowDiskUse(true);
			Iterator<Document> iter = cursor.iterator();
			if (iter.hasNext()) {
				docTmp = iter.next();
			}
			if (null == docTmp || docTmp.get("DMMauSoKyHieu") == null) {
				return new FileInfo();
			}

			boolean isDieuChinh = "2".equals(docTmp.getEmbedded(Arrays.asList("HDSS", "TCTBao"), ""));
			boolean isThayThe = "3".equals(docTmp.getEmbedded(Arrays.asList("HDSS", "TCTBao"), ""));
			String check_status = docTmp.get("EInvoiceStatus", "");
			if (check_status.equals("REPLACED")) {
				isThayThe = true;
			}
			if (check_status.equals("ADJUSTED")) {
				isDieuChinh = true;
			}
			String MST = docTmp.getEmbedded(Arrays.asList("EInvoiceDetail", "NDHDon", "NBan", "MST"), "");
			String ImgLogo = docTmp.getEmbedded(Arrays.asList("DMMauSoKyHieu", "Templates", "ImgLogo"), "");
			String ImgBackground = docTmp.getEmbedded(Arrays.asList("DMMauSoKyHieu", "Templates", "ImgBackground"), "");
			String ImgQA = docTmp.getEmbedded(Arrays.asList("DMMauSoKyHieu", "Templates", "ImgQA"), "");
			String ImgVien = docTmp.getEmbedded(Arrays.asList("DMMauSoKyHieu", "Templates", "ImgVien"), "");
			String link = docTmp.getEmbedded(Arrays.asList("PramLink", "LinkPortal"), "");
			String dir = docTmp.get("Dir", "");
			String signStatusCode = docTmp.get("SignStatusCode", "");
			String eInvoiceStatus = docTmp.get("EInvoiceStatus", "");
			String MCCQT = docTmp.get("MCCQT", "");
			String fileName = _id + ".xml";
			if ("SIGNED".equals(signStatusCode) && "COMPLETE".equals(eInvoiceStatus) && !"".equals(MCCQT)) {
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

//				file = new File("D:\\WORKING\\WEB\\HDDT-NEW\\_TTP\\_temp", "61d058d11b9b8d400da33da7_00276F25B531ED4541B4D1C69B06E60AE2.xml");

			org.w3c.dom.Document doc = commons.fileToDocument(file);
			/* TEST REPORT TO PDF */
			String fileNameJP = docTmp.getEmbedded(Arrays.asList("DMMauSoKyHieu", "Templates", "FileName"), "");
			int numberRowInPage = docTmp.getEmbedded(Arrays.asList("DMMauSoKyHieu", "Templates", "RowsInPage"), 20);
			int numberRowInPageMultiPage = docTmp
					.getEmbedded(Arrays.asList("DMMauSoKyHieu", "Templates", "RowInPageMultiPage"), 26);
			int numberCharsInRow = docTmp.getEmbedded(Arrays.asList("DMMauSoKyHieu", "Templates", "CharsInRow"), 50);
//		String CheckView = docTmp.getEmbedded(Arrays.asList("UserConFig", "viewshd"), "");

			String Checknamecd = docTmp.getEmbedded(Arrays.asList("UserConFig", "NameCD"), "");
			String CheckView = docTmp.getEmbedded(Arrays.asList("UserConFig", "viewshd"), "") + "," + Checknamecd;
			File fileJP = new File(SystemParams.DIR_E_INVOICE_TEMPLATE, fileNameJP);

			ByteArrayOutputStream baosPDF = null;

			baosPDF = jpUtils.createFinalInvoice2(fileJP, doc, docTmp, CheckView, numberRowInPage,
					numberRowInPageMultiPage, numberCharsInRow, MST, link,
					Paths.get(SystemParams.DIR_E_INVOICE_TEMPLATE, "images", MST, ImgLogo).toString(),
					Paths.get(SystemParams.DIR_E_INVOICE_TEMPLATE, "images", MST, ImgBackground).toString(),
					Paths.get(SystemParams.DIR_E_INVOICE_TEMPLATE, "images", MST, ImgQA).toString(),
					Paths.get(SystemParams.DIR_E_INVOICE_TEMPLATE, "images", MST, ImgVien).toString(),
					"Y".equals(isConvert), Constants.INVOICE_STATUS.DELETED.equals(eInvoiceStatus), isThayThe,
					isDieuChinh);
			fileInfo.setFileName("Export.pdf");
			fileInfo.setContentFile(baosPDF.toByteArray());

			return fileInfo;
		} catch (NullPointerException | MessagingException | UnsupportedEncodingException e) {
			return new FileInfo();
		}
	}

	@Override
	public FileInfo viewpdf(JSONRoot jsonRoot) throws Exception {
		FileInfo fileInfo = new FileInfo();

		Msg msg = jsonRoot.getMsg();
		MsgHeader header = msg.getMsgHeader();

		Object objData = msg.getObjData();

		JsonNode jsonData = null;
		if (objData != null) {
			jsonData = Json.serializer().nodeFromObject(msg.getObjData());
		} else {
			return new FileInfo();
		}

		String _id = commons.getTextJsonNode(jsonData.at("/_id")).replaceAll("\\s", "");
		ObjectId objectId = null;
		try {
			objectId = new ObjectId(_id);
		} catch (Exception e) {
		}
		ObjectId objectIdIssu = null;
		try {
			objectIdIssu = new ObjectId(header.getIssuerId());
		} catch (Exception e) {
		}

		List<Document> pipeline = new ArrayList<Document>();
		pipeline.add(
				new Document("$match", new Document("_id", objectId).append("IsDelete", new Document("$ne", true))));
		pipeline.add(
				new Document("$lookup",
						new Document("from", "Issuer")
								.append("pipeline",
										Arrays.asList(new Document("$match", new Document("_id", objectIdIssu)
												.append("IsDelete", new Document("$ne", true)))))
								.append("as", "Issuer")));
		pipeline.add(
				new Document("$unwind", new Document("path", "$Issuer").append("preserveNullAndEmptyArrays", true)));

		Document docTmp = null;
		Iterable<Document> cursor = mongoTemplate.getCollection("DMMauSoKyHieu").aggregate(pipeline).allowDiskUse(true);
		Iterator<Document> iter = cursor.iterator();
		if (iter.hasNext()) {
			docTmp = iter.next();
		}

		String ImgLogo = docTmp.getEmbedded(Arrays.asList("Templates", "ImgLogo"), "");
		String ImgBackground = docTmp.getEmbedded(Arrays.asList("Templates", "ImgBackground"), "");
		String ImgQA = docTmp.getEmbedded(Arrays.asList("Templates", "ImgQA"), "");
		String ImgVien = docTmp.getEmbedded(Arrays.asList("Templates", "ImgVien"), "");

		// org.w3c.dom.Document doc = (org.w3c.dom.Document) isu;
		/* TEST REPORT TO PDF */
		String fileNameJP = docTmp.getEmbedded(Arrays.asList("Templates", "FileName"), "");
		int numberRowInPage = docTmp.getEmbedded(Arrays.asList("Templates", "RowsInPage"), 20);
		File fileJP = new File(SystemParams.DIR_E_INVOICE_TEMPLATE, fileNameJP);

		ByteArrayOutputStream baosPDF = null;
		try {
			baosPDF = jpUtils.viewpdf(fileJP, docTmp, numberRowInPage,
					Paths.get(SystemParams.DIR_E_INVOICE_TEMPLATE, "images",
							docTmp.getEmbedded(Arrays.asList("Issuer", "TaxCode"), ""), ImgLogo).toString(),
					Paths.get(SystemParams.DIR_E_INVOICE_TEMPLATE, "images",
							docTmp.getEmbedded(Arrays.asList("Issuer", "TaxCode"), ""), ImgBackground).toString(),
					Paths.get(SystemParams.DIR_E_INVOICE_TEMPLATE, "images",
							docTmp.getEmbedded(Arrays.asList("Issuer", "TaxCode"), ""), ImgQA).toString(),
					Paths.get(SystemParams.DIR_E_INVOICE_TEMPLATE, "images",
							docTmp.getEmbedded(Arrays.asList("Issuer", "TaxCode"), ""), ImgVien).toString());
			fileInfo.setFileName("Viewpdf.pdf");
			fileInfo.setContentFile(baosPDF.toByteArray());
			return fileInfo;
		} catch (Exception e) {
			return new FileInfo();
		}

	}

	@Override
	public FileInfo viewpdftncn(JSONRoot jsonRoot) throws Exception {
		FileInfo fileInfo = new FileInfo();

		Msg msg = jsonRoot.getMsg();
		MsgHeader header = msg.getMsgHeader();
		MsgPage page = msg.getMsgPage();

		Object objData = msg.getObjData();

		JsonNode jsonData = null;
		if (objData != null) {
			jsonData = Json.serializer().nodeFromObject(msg.getObjData());
		} else {
			return new FileInfo();
		}

		String _id = commons.getTextJsonNode(jsonData.at("/_id")).replaceAll("\\s", "");
		String isConvert = commons.getTextJsonNode(jsonData.at("/IsConvert")).replaceAll("\\s", "");
		ObjectId objectId = null;
		try {
			objectId = new ObjectId(_id);
		} catch (Exception e) {
		}
		ObjectId objectIdIssu = null;
		try {
			objectIdIssu = new ObjectId(header.getIssuerId());
		} catch (Exception e) {
		}

		List<Document> pipeline = new ArrayList<Document>();
		pipeline.add(
				new Document("$match", new Document("_id", objectId).append("IsDelete", new Document("$ne", true))));
		pipeline.add(
				new Document("$lookup",
						new Document("from", "Issuer")
								.append("pipeline",
										Arrays.asList(new Document("$match", new Document("_id", objectIdIssu)
												.append("IsDelete", new Document("$ne", true)))))
								.append("as", "Issuer")));
		pipeline.add(
				new Document("$unwind", new Document("path", "$Issuer").append("preserveNullAndEmptyArrays", true)));

		Document docTmp = null;
		Iterable<Document> cursor = mongoTemplate.getCollection("DMMSTNCN").aggregate(pipeline).allowDiskUse(true);
		Iterator<Document> iter = cursor.iterator();
		if (iter.hasNext()) {
			docTmp = iter.next();
		}

		String ImgLogo = docTmp.getEmbedded(Arrays.asList("LoGo"), "");

		String KYHieu = docTmp.getEmbedded(Arrays.asList("KyHieu"), "");
		String MauSo = docTmp.getEmbedded(Arrays.asList("MauSo"), "");
		String Nam = docTmp.getEmbedded(Arrays.asList("Nam"), "");
		String ChungTu = docTmp.getEmbedded(Arrays.asList("ChungTu"), "");

		String KH = KYHieu + MauSo + Nam + ChungTu;
		// org.w3c.dom.Document doc = (org.w3c.dom.Document) isu;
		/* TEST REPORT TO PDF */
		String fileNameJP = docTmp.getEmbedded(Arrays.asList("FileName"), "");

		File fileJP = new File(SystemParams.DIR_E_INVOICE_TEMPLATE, fileNameJP);

		ByteArrayOutputStream baosPDF = null;

		baosPDF = jpUtils.viewpdftncn(fileJP, docTmp,
				Paths.get(SystemParams.DIR_E_INVOICE_TEMPLATE, "MauSoTNCN",
						docTmp.getEmbedded(Arrays.asList("Issuer", "TaxCode"), ""), ImgLogo).toString(),
				KH, "Y".equals(isConvert));
		fileInfo.setFileName("viewpdftncn.pdf");
		fileInfo.setContentFile(baosPDF.toByteArray());

		return fileInfo;
	}

	public FileInfo print04(JSONRoot jsonRoot) throws Exception {
		FileInfo fileInfo = new FileInfo();
		Msg msg = jsonRoot.getMsg();
		MsgHeader header = msg.getMsgHeader();
		MsgPage page = msg.getMsgPage();
		Object objData = msg.getObjData();
		JsonNode jsonData = null;
		if (objData != null) {
			jsonData = Json.serializer().nodeFromObject(msg.getObjData());
			String _id = this.commons.getTextJsonNode(jsonData.at("/_id")).replaceAll("\\s", "");
			String isConvert = this.commons.getTextJsonNode(jsonData.at("/IsConvert")).replaceAll("\\s", "");
			ObjectId objectId = null;

			try {
				objectId = new ObjectId(_id);
			} catch (Exception var29) {
			}

			ObjectId objectIdIssu = null;

			try {
				objectIdIssu = new ObjectId(header.getIssuerId());
			} catch (Exception var28) {
			}

			List<Document> pipeline = new ArrayList();
			pipeline.add(new Document("$match",
					(new Document("_id", objectId)).append("IsDelete", new Document("$ne", true))));
			pipeline.add(
					new Document("$lookup",
							(new Document("from", "Issuer"))
									.append("pipeline",
											Arrays.asList(
													new Document("$match",
															(new Document("_id", objectIdIssu)).append("IsDelete",
																	new Document("$ne", true)))))
									.append("as", "Issuer")));
			pipeline.add(new Document("$unwind",
					(new Document("path", "$Issuer")).append("preserveNullAndEmptyArrays", true)));
			Document docTmp = null;
			Iterable<Document> cursor = this.mongoTemplate.getCollection("EInvoiceHDSS").aggregate(pipeline)
					.allowDiskUse(true);
			Iterator<Document> iter = cursor.iterator();
			if (iter.hasNext()) {
				docTmp = (Document) iter.next();
			}

			String ImgLogo = "";
			String ImgBackground = "";
			String fileName = _id + "_signed.xml";
			String dir = (String) docTmp.get("Dir", "");
			File file = new File(dir, fileName);
			if (file.exists() && file.isFile()) {
				org.w3c.dom.Document doc = this.commons.fileToDocument(file);
				String fileNameJP = "04SS.jrxml";
				int numberRowInPage = 5;
				int numberRowInPageMultiPage = 15;
				int numberCharsInRow = 50;
				File fileJP = new File(SystemParams.DIR_E_INVOICE_TEMPLATE, fileNameJP);
				ByteArrayOutputStream baosPDF = null;
				baosPDF = this.jpUtils.print04(fileJP, doc, docTmp, numberRowInPage, numberRowInPageMultiPage,
						numberCharsInRow,
						Paths.get(SystemParams.DIR_E_INVOICE_TEMPLATE, "images",
								(String) docTmp.getEmbedded(Arrays.asList("Issuer", "TaxCode"), ""), ImgLogo)
								.toString(),
						Paths.get(SystemParams.DIR_E_INVOICE_TEMPLATE, "images",
								(String) docTmp.getEmbedded(Arrays.asList("Issuer", "TaxCode"), ""), ImgBackground)
								.toString(),
						"Y".equals(isConvert));
				fileInfo.setFileName("print04.pdf");
				fileInfo.setContentFile(baosPDF.toByteArray());
				return fileInfo;
			} else {
				return new FileInfo();
			}
		} else {
			return new FileInfo();
		}
	}

	// Print Agent
	@Override
	public FileInfo printAgent(JSONRoot jsonRoot) throws Exception {
		FileInfo fileInfo = new FileInfo();

		Msg msg = jsonRoot.getMsg();
		MsgHeader header = msg.getMsgHeader();
		MsgPage page = msg.getMsgPage();

		Object objData = msg.getObjData();

		JsonNode jsonData = null;
		if (objData != null) {
			jsonData = Json.serializer().nodeFromObject(msg.getObjData());
		} else {
			return new FileInfo();
		}

		String _id = commons.getTextJsonNode(jsonData.at("/_id")).replaceAll("\\s", "");
		String isConvert = commons.getTextJsonNode(jsonData.at("/IsConvert")).replaceAll("\\s", "");
		ObjectId objectId = null;
		try {
			objectId = new ObjectId(_id);
		} catch (Exception e) {
		}

		Document docFind = new Document("IssuerId", header.getIssuerId()).append("_id", objectId).append("IsDelete",
				new Document("$ne", true));
		List<Document> pipeline = new ArrayList<Document>();
		pipeline.add(new Document("$match", docFind));
		pipeline.add(new Document("$lookup", new Document("from", "DMMauSoKyHieu")
				.append("let",
						new Document("vIssuerId", "$IssuerId").append("vMauSoHD", "$EInvoiceDetail.TTChung.MauSoHD"))
				.append("pipeline",
						Arrays.asList(new Document("$match", new Document("$expr", new Document("$and",
								Arrays.asList(new Document("$eq", Arrays.asList("$$vIssuerId", "$IssuerId")),
										new Document("$eq",
												Arrays.asList(new Document("$toString", "$_id"), "$$vMauSoHD"))))))))
				.append("as", "DMMauSoKyHieu")));
		pipeline.add(new Document("$unwind",
				new Document("path", "$DMMauSoKyHieu").append("preserveNullAndEmptyArrays", true)));
		pipeline.add(
				new Document("$lookup",
						new Document("from", "UserConFig")
								.append("pipeline",
										Arrays.asList(
												new Document("$match", new Document("IssuerId", header.getIssuerId()))))
								.append("as", "UserConFig")));
		pipeline.add(new Document("$unwind",
				new Document("path", "$UserConFig").append("preserveNullAndEmptyArrays", true)));

		pipeline.add(
				new Document("$lookup",
						new Document("from", "PramLink")
								.append("pipeline",
										Arrays.asList(new Document("$match",
												new Document("$expr", new Document("IsDelete", false)))))
								.append("as", "PramLink")));
		pipeline.add(
				new Document("$unwind", new Document("path", "$PramLink").append("preserveNullAndEmptyArrays", true)));
		Document docTmp = null;
		Iterable<Document> cursor = mongoTemplate.getCollection("EInvoicePXKDL").aggregate(pipeline).allowDiskUse(true);
		Iterator<Document> iter = cursor.iterator();
		if (iter.hasNext()) {
			docTmp = iter.next();
		}
		if (null == docTmp || docTmp.get("DMMauSoKyHieu") == null) {
			return new FileInfo();
		}

		boolean isDieuChinh = "2".equals(docTmp.getEmbedded(Arrays.asList("HDSS", "TCTBao"), ""));
		boolean isThayThe = "3".equals(docTmp.getEmbedded(Arrays.asList("HDSS", "TCTBao"), ""));
		String check_status = docTmp.get("EInvoiceStatus", "");
		if (check_status.equals("REPLACED")) {
			isThayThe = true;
		}
		if (check_status.equals("ADJUSTED")) {
			isDieuChinh = true;
		}
		String MST = docTmp.getEmbedded(Arrays.asList("EInvoiceDetail", "NDHDon", "NBan", "MST"), "");
		String ImgLogo = docTmp.getEmbedded(Arrays.asList("DMMauSoKyHieu", "Templates", "ImgLogo"), "");
		String ImgBackground = docTmp.getEmbedded(Arrays.asList("DMMauSoKyHieu", "Templates", "ImgBackground"), "");
		String ImgQA = docTmp.getEmbedded(Arrays.asList("DMMauSoKyHieu", "Templates", "ImgQA"), "");
		String ImgVien = docTmp.getEmbedded(Arrays.asList("DMMauSoKyHieu", "Templates", "ImgVien"), "");
//			String CheckView = docTmp.getEmbedded(Arrays.asList("UserConFig", "viewshd"), "");
		String link = docTmp.getEmbedded(Arrays.asList("PramLink", "LinkPortal"), "");
		String Checknamecd = docTmp.getEmbedded(Arrays.asList("UserConFig", "NameCD"), "");
		String CheckView = docTmp.getEmbedded(Arrays.asList("UserConFig", "viewshd"), "") + "," + Checknamecd;

		String dir = docTmp.get("Dir", "");
		String signStatusCode = docTmp.get("SignStatusCode", "");
		String eInvoiceStatus = docTmp.get("EInvoiceStatus", "");
		String secureKey = docTmp.get("SecureKey", "");
		String MCCQT = docTmp.get("MCCQT", "");
		String fileName = _id + ".xml";
		if ("SIGNED".equals(signStatusCode) && "COMPLETE".equals(eInvoiceStatus) && !"".equals(MCCQT)) {
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

//					file = new File("D:\\WORKING\\WEB\\HDDT-NEW\\_TTP\\_temp", "61d058d11b9b8d400da33da7_00276F25B531ED4541B4D1C69B06E60AE2.xml");

		org.w3c.dom.Document doc = commons.fileToDocument(file);
		/* TEST REPORT TO PDF */
		String fileNameJP = docTmp.getEmbedded(Arrays.asList("DMMauSoKyHieu", "Templates", "FileName"), "");
		int numberRowInPage = docTmp.getEmbedded(Arrays.asList("DMMauSoKyHieu", "Templates", "RowsInPage"), 20);
		int numberRowInPageMultiPage = docTmp
				.getEmbedded(Arrays.asList("DMMauSoKyHieu", "Templates", "RowInPageMultiPage"), 26);
		int numberCharsInRow = docTmp.getEmbedded(Arrays.asList("DMMauSoKyHieu", "Templates", "CharsInRow"), 50);

		File fileJP = new File(SystemParams.DIR_E_INVOICE_TEMPLATE, fileNameJP);

		ByteArrayOutputStream baosPDF = null;

		baosPDF = jpUtils.createFinalInvoiceDL(fileJP, doc, secureKey, CheckView, numberRowInPage, numberRowInPageMultiPage,
				numberCharsInRow, MST, link,
				Paths.get(SystemParams.DIR_E_INVOICE_TEMPLATE, "images", MST, ImgLogo).toString(),
				Paths.get(SystemParams.DIR_E_INVOICE_TEMPLATE, "images", MST, ImgBackground).toString(),
				Paths.get(SystemParams.DIR_E_INVOICE_TEMPLATE, "images", MST, ImgQA).toString(),
				Paths.get(SystemParams.DIR_E_INVOICE_TEMPLATE, "images", MST, ImgVien).toString(),

				"Y".equals(isConvert), Constants.INVOICE_STATUS.DELETED.equals(eInvoiceStatus), isThayThe, isDieuChinh);
		fileInfo.setFileName("Agent.pdf");
		fileInfo.setContentFile(baosPDF.toByteArray());

		return fileInfo;
	}

	// Print EInvoice BH
	@Override
	public FileInfo printEInvoiceBH(JSONRoot jsonRoot) throws Exception {
		FileInfo fileInfo = new FileInfo();

		Msg msg = jsonRoot.getMsg();
		MsgHeader header = msg.getMsgHeader();
		MsgPage page = msg.getMsgPage();

		Object objData = msg.getObjData();

		JsonNode jsonData = null;
		if (objData != null) {
			jsonData = Json.serializer().nodeFromObject(msg.getObjData());
		} else {
			return new FileInfo();
		}

		String _id = commons.getTextJsonNode(jsonData.at("/_id")).replaceAll("\\s", "");
		String isConvert = commons.getTextJsonNode(jsonData.at("/IsConvert")).replaceAll("\\s", "");
		/*
		 * db.getCollection('EInvoice').find( { IssuerId: '61b851ebb0228bba71fca2ec',
		 * _id: ObjectId("61c7cad6f8b593616ce03cc7"),IsDelete: {$ne: true} } )
		 * 
		 * db.getCollection('EInvoice').aggregate([ {$match: { IssuerId:
		 * '61b851ebb0228bba71fca2ec', _id:
		 * ObjectId("61c7cad6f8b593616ce03cc7"),IsDelete: {$ne: true} } }, {$lookup: {
		 * from: 'DMMauSoKyHieu', let: {vIssuerId: '$IssuerId', vMauSoHD:
		 * '$EInvoiceDetail.TTChung.MauSoHD'}, pipeline: [ {$match: { $expr: { $and: [
		 * {$eq: ['$$vIssuerId', '$IssuerId']}, {$eq: [{$toString: '$_id'},
		 * '$$vMauSoHD']} ] } } } ], as: 'DMMauSoKyHieu' } }, {$unwind: {path:
		 * '$DMMauSoKyHieu', preserveNullAndEmptyArrays: true}} ])
		 * 
		 */

		ObjectId objectId = null;
		try {
			objectId = new ObjectId(_id);
		} catch (Exception e) {
		}

		Document docFind = new Document("IssuerId", header.getIssuerId()).append("_id", objectId).append("IsDelete",
				new Document("$ne", true));
		List<Document> pipeline = new ArrayList<Document>();
		pipeline.add(new Document("$match", docFind));
		pipeline.add(new Document("$lookup", new Document("from", "DMMauSoKyHieu")
				.append("let",
						new Document("vIssuerId", "$IssuerId").append("vMauSoHD", "$EInvoiceDetail.TTChung.MauSoHD"))
				.append("pipeline",
						Arrays.asList(new Document("$match", new Document("$expr", new Document("$and",
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
		pipeline.add(
				new Document("$lookup",
						new Document("from", "PramLink")
								.append("pipeline",
										Arrays.asList(new Document("$match",
												new Document("$expr", new Document("IsDelete", false)))))
								.append("as", "PramLink")));
		pipeline.add(
				new Document("$unwind", new Document("path", "$PramLink").append("preserveNullAndEmptyArrays", true)));
		Document docTmp = null;
		Iterable<Document> cursor = mongoTemplate.getCollection("EInvoiceBH").aggregate(pipeline).allowDiskUse(true);
		Iterator<Document> iter = cursor.iterator();
		if (iter.hasNext()) {
			docTmp = iter.next();
		}
		if (null == docTmp || docTmp.get("DMMauSoKyHieu") == null) {
			return new FileInfo();
		}

		boolean isDieuChinh = false;
		boolean isThayThe = false;
		String check_status = docTmp.get("EInvoiceStatus", "");
		if (check_status.equals("REPLACED")) {
			isThayThe = true;
		}
		if (check_status.equals("ADJUSTED")) {
			isDieuChinh = true;
		}

		String link = docTmp.getEmbedded(Arrays.asList("PramLink", "LinkPortal"), "");
		String MST = docTmp.getEmbedded(Arrays.asList("EInvoiceDetail", "NDHDon", "NBan", "MST"), "");
		String ImgLogo = docTmp.getEmbedded(Arrays.asList("DMMauSoKyHieu", "Templates", "ImgLogo"), "");
		String ImgQA = docTmp.getEmbedded(Arrays.asList("DMMauSoKyHieu", "Templates", "ImgQA"), "");
		String ImgVien = docTmp.getEmbedded(Arrays.asList("DMMauSoKyHieu", "Templates", "ImgVien"), "");

		String ImgBackground = docTmp.getEmbedded(Arrays.asList("DMMauSoKyHieu", "Templates", "ImgBackground"), "");
		String CheckView = docTmp.getEmbedded(Arrays.asList("UserConFig", "viewshd"), "");
		String dir = docTmp.get("Dir", "");
		String signStatusCode = docTmp.get("SignStatusCode", "");
		String eInvoiceStatus = docTmp.get("EInvoiceStatus", "");
		String secureKey = docTmp.get("SecureKey", "");
		String MCCQT = docTmp.get("MCCQT", "");
		String fileName = _id + ".xml";
		if ("SIGNED".equals(signStatusCode) && "COMPLETE".equals(eInvoiceStatus) && !"".equals(MCCQT)) {
			fileName = _id + "_" + MCCQT + ".xml";
		} else {
			if ("SIGNED".equals(signStatusCode)) {
				fileName = _id + "_signed.xml";
			}
		}
//				else if("PENDING".equals(eInvoiceStatus))
//					fileName = _id + "_signed.xml";

		File file = new File(dir, fileName);
		if (!file.exists() || !file.isFile()) {
			return new FileInfo();
		}

//				file = new File("D:\\WORKING\\WEB\\HDDT-NEW\\_TTP\\_temp", "61d058d11b9b8d400da33da7_00276F25B531ED4541B4D1C69B06E60AE2.xml");

		org.w3c.dom.Document doc = commons.fileToDocument(file);
		/* TEST REPORT TO PDF */
		String fileNameJP = docTmp.getEmbedded(Arrays.asList("DMMauSoKyHieu", "Templates", "FileName"), "");
		int numberRowInPage = docTmp.getEmbedded(Arrays.asList("DMMauSoKyHieu", "Templates", "RowsInPage"), 20);
		int numberRowInPageMultiPage = docTmp
				.getEmbedded(Arrays.asList("DMMauSoKyHieu", "Templates", "RowInPageMultiPage"), 26);
		int numberCharsInRow = docTmp.getEmbedded(Arrays.asList("DMMauSoKyHieu", "Templates", "CharsInRow"), 50);

		File fileJP = new File(SystemParams.DIR_E_INVOICE_TEMPLATE, fileNameJP);

		ByteArrayOutputStream baosPDF = null;

		baosPDF = jpUtils.createFinalInvoiceBH(fileJP, doc, secureKey, CheckView, numberRowInPage, numberRowInPageMultiPage,
				numberCharsInRow, MST, link,
				Paths.get(SystemParams.DIR_E_INVOICE_TEMPLATE, "images", MST, ImgLogo).toString(),
				Paths.get(SystemParams.DIR_E_INVOICE_TEMPLATE, "images", MST, ImgBackground).toString(),
				Paths.get(SystemParams.DIR_E_INVOICE_TEMPLATE, "images", MST, ImgQA).toString(),
				Paths.get(SystemParams.DIR_E_INVOICE_TEMPLATE, "images", MST, ImgVien).toString(),

				"Y".equals(isConvert), Constants.INVOICE_STATUS.DELETED.equals(eInvoiceStatus), isThayThe, isDieuChinh);

//				Map<String, Object> reportParams = new HashMap<String, Object>();
//				
//				ByteArrayOutputStream out = new ByteArrayOutputStream();
//				JasperReport jr = JasperCompileManager.compileReport(new FileInputStream(fileJP));
//				JasperPrint jp = JasperFillManager.fillReport(jr, reportParams);
//				
//				Exporter exporter = null;
//				
//				exporter = new JRPdfExporter();
//				exporter.setExporterInput(new SimpleExporterInput(jp));
//				exporter.setExporterOutput(new SimpleOutputStreamExporterOutput(out));
//		        SimplePdfExporterConfiguration configuration = new SimplePdfExporterConfiguration();
//		        configuration.setCreatingBatchModeBookmarks(true);
//		        exporter.setConfiguration(configuration);
//		        exporter.exportReport();
//				
		fileInfo.setFileName("EInvoiceBH.pdf");
		fileInfo.setContentFile(baosPDF.toByteArray());

		return fileInfo;
	}

	@Override
	public FileInfo viewpdfcttncn(JSONRoot jsonRoot) throws Exception {
		FileInfo fileInfo = new FileInfo();

		Msg msg = jsonRoot.getMsg();
		MsgHeader header = msg.getMsgHeader();
		MsgPage page = msg.getMsgPage();

		Object objData = msg.getObjData();

		JsonNode jsonData = null;
		if (objData != null) {
			jsonData = Json.serializer().nodeFromObject(msg.getObjData());
		} else {
			return new FileInfo();
		}

		String _id = commons.getTextJsonNode(jsonData.at("/_id")).replaceAll("\\s", "");
		String isConvert = commons.getTextJsonNode(jsonData.at("/IsConvert")).replaceAll("\\s", "");
		ObjectId objectId = null;
		try {
			objectId = new ObjectId(_id);
		} catch (Exception e) {
		}
		ObjectId objectIdIssu = null;
		try {
			objectIdIssu = new ObjectId(header.getIssuerId());
		} catch (Exception e) {
		}

		List<Document> pipeline = new ArrayList<Document>();
		pipeline.add(
				new Document("$match", new Document("_id", objectId).append("IsDelete", new Document("$ne", true))));
		pipeline.add(
				new Document("$lookup",
						new Document("from", "Issuer")
								.append("pipeline",
										Arrays.asList(new Document("$match", new Document("_id", objectIdIssu)
												.append("IsDelete", new Document("$ne", true)))))
								.append("as", "Issuer")));
		pipeline.add(
				new Document("$unwind", new Document("path", "$Issuer").append("preserveNullAndEmptyArrays", true)));

		pipeline.add(new Document("$lookup",
				new Document("from", "DMMSTNCN")
				.append("let",
						new Document("vMauSoHD", "$MauSoHD"))
						.append("pipeline",
								Arrays.asList(new Document("$match",
//										new Document("IssuerId", objectIdIssu.toString()).append("IsDelete", false).append("IsActive", true))
										  new Document("$expr",
			                                        new Document("$and", Arrays.asList(
			                                                new Document("$eq", Arrays.asList("$_id", new Document("$toObjectId", "$$vMauSoHD"))),
			                                                new Document("$eq", Arrays.asList("$IssuerId", objectIdIssu.toString())),
			                                                new Document("$eq", Arrays.asList("$IsDelete", false)),
			                                                new Document("$eq", Arrays.asList("$IsActive", true))
			                                        )))
			                                )
										))
						.append("as", "DMMSTNCN")));
		pipeline.add(
				new Document("$unwind", new Document("path", "$DMMSTNCN").append("preserveNullAndEmptyArrays", true)));
		pipeline.add(
				new Document("$lookup",
						new Document("from", "PramLink")
								.append("pipeline",
										Arrays.asList(new Document("$match",
												new Document("$expr", new Document("IsDelete", false)))))
								.append("as", "PramLink")));
		pipeline.add(
				new Document("$unwind", new Document("path", "$PramLink").append("preserveNullAndEmptyArrays", true)));
		Document docTmp = null;
		Iterable<Document> cursor = mongoTemplate.getCollection("ChungTuTNCN").aggregate(pipeline).allowDiskUse(true);
		Iterator<Document> iter = cursor.iterator();
		if (iter.hasNext()) {
			docTmp = iter.next();
		}
		
		if (docTmp == null) {
			return fileInfo;
		}

		String ImgLogo = docTmp.getEmbedded(Arrays.asList("DMMSTNCN", "LoGo"), "");

		String link = docTmp.getEmbedded(Arrays.asList("PramLink", "LinkPortal"), "");
		String MauSo = docTmp.getEmbedded(Arrays.asList("DMMSTNCN", "MauSo"), "");

		String KH = docTmp.getEmbedded(Arrays.asList("DMMSTNCN", "KyHieu"), "") + "/"
				+ docTmp.getEmbedded(Arrays.asList("DMMSTNCN", "Nam"), "") + "/"
				+ docTmp.getEmbedded(Arrays.asList("DMMSTNCN", "ChungTu"), "");
		String MS = MauSo;
		// org.w3c.dom.Document doc = (org.w3c.dom.Document) isu;
		/* TEST REPORT TO PDF */

		String dir = docTmp.get("Dir", "");
		String SignStatus = docTmp.get("SignStatus", "");
		String fileName = _id + ".xml";
		if ("SIGNED".equals(SignStatus)) {
			fileName = _id + "_signed.xml";
		}

		File file = new File(dir, fileName);
		if (!file.exists() || !file.isFile()) {
			return new FileInfo();
		}

		org.w3c.dom.Document doc = commons.fileToDocument(file);

		String Status = docTmp.get("Status", "");

		String fileNameJP = docTmp.getEmbedded(Arrays.asList("DMMSTNCN", "FileName"), "");

		File fileJP = new File(SystemParams.DIR_E_INVOICE_TEMPLATE, fileNameJP);

		ByteArrayOutputStream baosPDF = null;

		baosPDF = jpUtils.viewpdfcttncn(fileJP, doc, docTmp,
				Paths.get(SystemParams.DIR_E_INVOICE_TEMPLATE, "MauSoTNCN",
						docTmp.getEmbedded(Arrays.asList("Issuer", "TaxCode"), ""), ImgLogo).toString(),
				KH, MS, link, "Y".equals(isConvert), Constants.INVOICE_STATUS.XOABO.equals(Status));
		fileInfo.setFileName("viewpdftncn.pdf");
		fileInfo.setContentFile(baosPDF.toByteArray());

		return fileInfo;
	}

	public FileInfo getXml(JSONRoot jsonRoot) throws Exception {
		FileInfo fileInfo = new FileInfo();
		ArrayList<HashMap<String, String>> arrayInfoInvoice = new ArrayList<>();
		HashMap<String, String> hItem = null;
		File file_zip = null;
		String fileName = "";
		List<String> data = null;
		Msg msg = jsonRoot.getMsg();
		MsgHeader header = msg.getMsgHeader();
		MsgPage page = msg.getMsgPage();
		Object objData = msg.getObjData();
		JsonNode jsonData = null;

		try {
			if (objData != null) {
				jsonData = Json.serializer().nodeFromObject(msg.getObjData());
			}
			String mst = "";
			String khhd = "";
			String shoad = "";
			String mtd = "";
			String _token = commons.getTextJsonNode(jsonData.at("/_token")).replaceAll("\\s", "");
			data = null;
			try {
				data = Json.serializer().fromJson(commons.decodeBase64ToString(_token),
						new TypeReference<List<String>>() {
						});
			} catch (Exception e) {
			}

			mst = data.get(0);
			khhd = data.get(1);
			shoad = data.get(2);
			mtd = data.get(3);

			int shdon = Integer.parseInt(shoad);
			List<Document> pipeline = new ArrayList<Document>();
			if (mtd != null) {
				Document docFind = new Document("EInvoiceDetail.TTChung.KHHDon", khhd)
						.append("EInvoiceDetail.TTChung.SHDon", shdon).append("EInvoiceDetail.NDHDon.NBan.MST", mst)
						.append("MTDiep", mtd).append("IsDelete", new Document("$ne", true));
				pipeline.add(new Document("$match", docFind));
				pipeline.add(new Document("$project", new Document("_id", 1).append("EInvoiceDetail", 1)
						.append("Dir", 1).append("SignStatusCode", 1).append("MCCQT", 1)
						
						));
			} else {
				Document docFind = new Document("EInvoiceDetail.TTChung.KHHDon", khhd)
						.append("EInvoiceDetail.TTChung.SHDon", shdon).append("EInvoiceDetail.NDHDon.NBan.MST", mst)
						.append("IsDelete", new Document("$ne", true));
				pipeline.add(new Document("$match", docFind));
				pipeline.add(new Document("$project", new Document("_id", 1).append("EInvoiceDetail", 1)
						.append("Dir", 1).append("SignStatusCode", 1).append("MCCQT", 1)
						
						));
			}
			Document docTmp = null;
			Document docTmp1 = null;
			Document docTmp2 = null;
			Document docTmp3 = null;
			
			MongoClient mongoClient = cfg.mongoClient();
			MongoCollection<Document> collection = mongoClient.getDatabase(cfg.dbName).getCollection("EInvoice");
			      try {
			        docTmp = collection.aggregate(pipeline).allowDiskUse(true).iterator().next();    
			      } catch (Exception e) {
			        
			      }		        
			mongoClient.close();
			
			
			mongoClient = cfg.mongoClient();
			collection = mongoClient.getDatabase(cfg.dbName).getCollection("EInvoiceBH");
			  try {
			         docTmp1 = collection.aggregate(pipeline).allowDiskUse(true).iterator().next();      
			     } catch (Exception e) {
			        
			    }
			mongoClient.close();
			
			mongoClient = cfg.mongoClient();
			collection = mongoClient.getDatabase(cfg.dbName).getCollection("EInvoicePXK");
			  try {
			         docTmp2 = collection.aggregate(pipeline).allowDiskUse(true).iterator().next();      
			     } catch (Exception e) {
			        
			    }
			mongoClient.close();
			
			mongoClient = cfg.mongoClient();
			collection = mongoClient.getDatabase(cfg.dbName).getCollection("EInvoicePXKDL");
			  try {
			         docTmp3 = collection.aggregate(pipeline).allowDiskUse(true).iterator().next();      
			     } catch (Exception e) {
			        
			    }
			mongoClient.close();
			
			
			if (null != docTmp) {
				String mskh = docTmp.getEmbedded(Arrays.asList("EInvoiceDetail", "TTChung", "KHMSHDon"), "")
						+ docTmp.getEmbedded(Arrays.asList("EInvoiceDetail", "TTChung", "KHHDon"), "");
				Integer shd = docTmp.getEmbedded(Arrays.asList("EInvoiceDetail", "TTChung", "SHDon"), 0);
				String namexml = mskh + "_" + shd + ".xml";				
			
				String _id = docTmp.get("_id").toString();
				String dir = docTmp.get("Dir", "");
				String signStatusCode = docTmp.get("SignStatusCode", "");
			
				String MCCQT = docTmp.get("MCCQT", "");
				fileName = _id + ".xml";
				if ("SIGNED".equals(signStatusCode) && !"".equals(MCCQT)) {
					fileName = _id + "_" + MCCQT + ".xml";
				} else {
					if ("SIGNED".equals(signStatusCode)) {
						fileName = _id + "_signed.xml";
					}
				}
				File file = new File(dir, fileName);
				File tam = new File(dir, namexml);
				copyFileUsingStream(file, tam);

				hItem = new HashMap<>();
				hItem.put("UrlFile", tam.getAbsolutePath());
				arrayInfoInvoice.add(hItem);

				if (arrayInfoInvoice.size() == 0) {
					return fileInfo;
				}
				if (arrayInfoInvoice.size() == 1) {
					hItem = arrayInfoInvoice.get(0);
					file_zip = new File(hItem.get("UrlFile"));
					fileInfo.setContentFile(Files.readAllBytes(file_zip.toPath()));
					fileInfo.setFileName(fileName);
					return fileInfo;
				}
			} else if (null != docTmp1) {
				String mskh = docTmp1.getEmbedded(Arrays.asList("EInvoiceDetail", "TTChung", "KHMSHDon"), "")
						+ docTmp1.getEmbedded(Arrays.asList("EInvoiceDetail", "TTChung", "KHHDon"), "");
				Integer shd = docTmp1.getEmbedded(Arrays.asList("EInvoiceDetail", "TTChung", "SHDon"), 0);
				String namexml = mskh + "_" + shd + ".xml";						

				String _id = docTmp1.get("_id").toString();
				String dir = docTmp1.get("Dir", "");
				String signStatusCode = docTmp1.get("SignStatusCode", "");				
				String MCCQT = docTmp1.get("MCCQT", "");
				fileName = _id + ".xml";
				if ("SIGNED".equals(signStatusCode) && !"".equals(MCCQT)) {
					fileName = _id + "_" + MCCQT + ".xml";
				} else {
					if ("SIGNED".equals(signStatusCode)) {
						fileName = _id + "_signed.xml";
					}
				}
				File file = new File(dir, fileName);
				File tam = new File(dir, namexml);
				copyFileUsingStream(file, tam);

				hItem = new HashMap<>();
				hItem.put("UrlFile", tam.getAbsolutePath());
				arrayInfoInvoice.add(hItem);

				if (arrayInfoInvoice.size() == 0) {
					return fileInfo;
				}
				if (arrayInfoInvoice.size() == 1) {
					hItem = arrayInfoInvoice.get(0);
					file_zip = new File(hItem.get("UrlFile"));
					fileInfo.setContentFile(Files.readAllBytes(file_zip.toPath()));
					fileInfo.setFileName(fileName);
					return fileInfo;
				}
			} else if (null != docTmp2) {
				String mskh = docTmp2.getEmbedded(Arrays.asList("EInvoiceDetail", "TTChung", "KHMSHDon"), "")
						+ docTmp2.getEmbedded(Arrays.asList("EInvoiceDetail", "TTChung", "KHHDon"), "");
				Integer shd = docTmp2.getEmbedded(Arrays.asList("EInvoiceDetail", "TTChung", "SHDon"), 0);
				String namexml = mskh + "_" + shd + ".xml";			

				String _id = docTmp2.get("_id").toString();
				String dir = docTmp2.get("Dir", "");
				String signStatusCode = docTmp2.get("SignStatusCode", "");
				String MCCQT = docTmp2.get("MCCQT", "");
				fileName = _id + ".xml";
				if ("SIGNED".equals(signStatusCode) && !"".equals(MCCQT)) {
					fileName = _id + "_" + MCCQT + ".xml";
				} else {
					if ("SIGNED".equals(signStatusCode)) {
						fileName = _id + "_signed.xml";
					}
				}
				File file = new File(dir, fileName);
				File tam = new File(dir, namexml);
				copyFileUsingStream(file, tam);

				hItem = new HashMap<>();
				hItem.put("UrlFile", tam.getAbsolutePath());
				arrayInfoInvoice.add(hItem);

				if (arrayInfoInvoice.size() == 0) {
					return fileInfo;
				}
				if (arrayInfoInvoice.size() == 1) {
					hItem = arrayInfoInvoice.get(0);
					file_zip = new File(hItem.get("UrlFile"));
					fileInfo.setContentFile(Files.readAllBytes(file_zip.toPath()));
					fileInfo.setFileName(fileName);
					return fileInfo;
				}
			} else if (null != docTmp3) {
				String mskh = docTmp3.getEmbedded(Arrays.asList("EInvoiceDetail", "TTChung", "KHMSHDon"), "")
						+ docTmp3.getEmbedded(Arrays.asList("EInvoiceDetail", "TTChung", "KHHDon"), "");
				Integer shd = docTmp3.getEmbedded(Arrays.asList("EInvoiceDetail", "TTChung", "SHDon"), 0);
				String namexml = mskh + "_" + shd + ".xml";				
				
				String _id = docTmp3.get("_id").toString();
				String dir = docTmp3.get("Dir", "");
				String signStatusCode = docTmp3.get("SignStatusCode", "");
			
				String MCCQT = docTmp3.get("MCCQT", "");
				fileName = _id + ".xml";
				if ("SIGNED".equals(signStatusCode) && !"".equals(MCCQT)) {
					fileName = _id + "_" + MCCQT + ".xml";
				} else {
					if ("SIGNED".equals(signStatusCode)) {
						fileName = _id + "_signed.xml";
					}
				}
				File file = new File(dir, fileName);
				File tam = new File(dir, namexml);
				copyFileUsingStream(file, tam);

				hItem = new HashMap<>();
				hItem.put("UrlFile", tam.getAbsolutePath());
				arrayInfoInvoice.add(hItem);

				if (arrayInfoInvoice.size() == 0) {
					return fileInfo;
				}
				if (arrayInfoInvoice.size() == 1) {
					hItem = arrayInfoInvoice.get(0);
					file_zip = new File(hItem.get("UrlFile"));
					fileInfo.setContentFile(Files.readAllBytes(file_zip.toPath()));
					fileInfo.setFileName(fileName);
					return fileInfo;
				}
			} else {
				return new FileInfo();
			}

			return fileInfo;

		} catch (NullPointerException | UnsupportedEncodingException e) {
			return new FileInfo();
		}
	}


	@Override
	public FileInfo einvoice1Xml(JSONRoot jsonRoot) throws Exception {
		FileInfo fileInfo = new FileInfo();
		ArrayList<HashMap<String, String>> arrayInfoInvoice = new ArrayList<>();
		HashMap<String, String> hItem = null;
		File file_zip = null;
		String fileName = "";
		List<String> ids = null;
		Msg msg = jsonRoot.getMsg();
		MsgHeader header = msg.getMsgHeader();
		MsgPage page = msg.getMsgPage();
		ByteArrayOutputStream baosPDF = null;
		List<String> listFileNamePdfFinal = new ArrayList<>();
		Object objData = msg.getObjData();
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		JsonNode jsonData = null;

		try {
			if (objData != null) {
				jsonData = Json.serializer().nodeFromObject(msg.getObjData());
			}
			String _id = "";
			String _token = commons.getTextJsonNode(jsonData.at("/_token")).replaceAll("\\s", "");
//				ids = null;
//				try {
//					ids = Json.serializer().fromJson(commons.decodeBase64ToString(_token), new TypeReference<List<String>>() {
//					});
//				}catch(Exception e) {}
//				
//				
//				
//				for(int i = 0; i< ids.size(); i++ ) {
//				_id = ids.get(i) ;

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

				String isConvert = commons.getTextJsonNode(jsonData.at("/IsConvert")).replaceAll("\\s", "");
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

				Document docTmp = null;
				Iterable<Document> cursor = mongoTemplate.getCollection("EInvoiceBH").aggregate(pipeline)
						.allowDiskUse(true);
				Iterator<Document> iter = cursor.iterator();
				if (iter.hasNext()) {
					docTmp = iter.next();
				}
				if (null == docTmp || docTmp.get("DMMauSoKyHieu") == null) {
					return new FileInfo();
				}
				String mskh = docTmp.getEmbedded(Arrays.asList("EInvoiceDetail", "TTChung", "KHMSHDon"), "")
						+ docTmp.getEmbedded(Arrays.asList("EInvoiceDetail", "TTChung", "KHHDon"), "");
				Integer shd = docTmp.getEmbedded(Arrays.asList("EInvoiceDetail", "TTChung", "SHDon"), 0);
				String namexml = mskh + "_" + shd + ".xml";
				boolean isDieuChinh = "2".equals(docTmp.getEmbedded(Arrays.asList("HDSS", "TCTBao"), ""));
				boolean isThayThe = "3".equals(docTmp.getEmbedded(Arrays.asList("HDSS", "TCTBao"), ""));

				String MST = docTmp.getEmbedded(Arrays.asList("EInvoiceDetail", "NDHDon", "NBan", "MST"), "");
				String ImgLogo = docTmp.getEmbedded(Arrays.asList("DMMauSoKyHieu", "Templates", "ImgLogo"), "");
				String ImgBackground = docTmp.getEmbedded(Arrays.asList("DMMauSoKyHieu", "Templates", "ImgBackground"),
						"");

				String dir = docTmp.get("Dir", "");
				String signStatusCode = docTmp.get("SignStatusCode", "");
				String eInvoiceStatus = docTmp.get("EInvoiceStatus", "");
				String MCCQT = docTmp.get("MCCQT", "");
				fileName = _id + ".xml";
				if ("SIGNED".equals(signStatusCode) && !"".equals(MCCQT)) {
					fileName = _id + "_" + MCCQT + ".xml";
					String fileName_ = _id + "_" + MCCQT + "_" + shd + ".xml";
					/* CHECK MCCQT GET DATA XML */

					File file_xml = new File(dir, fileName);

					org.w3c.dom.Document doc_xml = commons.fileToDocument(file_xml);

					XPath xPath_xml = XPathFactory.newInstance().newXPath();
					Node nodeHDon = null;
					for (int J = 1; J <= 20; J++) {
						nodeHDon = (Node) xPath_xml.evaluate("/KetQuaTraCuu/DuLieu/TDiep[" + J + "]/DLieu/HDon",
								doc_xml, XPathConstants.NODE);
						if (nodeHDon != null)
							break;
					}
					/* FILE CHUA DUOC DUI DEN CO QUAN THUE */
					if (null == nodeHDon) {
						nodeHDon = (Node) xPath_xml.evaluate("/HDon", doc_xml, XPathConstants.NODE);
					}

					DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
					DocumentBuilder builder = factory.newDocumentBuilder();
					org.w3c.dom.Document rTCTN_HDON = builder.newDocument();
					rTCTN_HDON.appendChild(rTCTN_HDON.importNode(nodeHDon, true));
					/* END CHECK GET DATA IN RESULT IN FILE XML TO TAX */
					boolean boo_ = false;
					boo_ = commons.docW3cToFile(rTCTN_HDON, dir, fileName_);

					if (boo_ == true) {
						fileName = fileName_;
					}
					/* END CHECK MCCQT GET DATA XML */
				} else {
					if ("SIGNED".equals(signStatusCode)) {
						fileName = _id + "_signed.xml";
					}
				}
				// namexml

				File file = new File(dir, fileName);
				File tam = new File(dir, namexml);
				copyFileUsingStream(file, tam);

				hItem = new HashMap<>();
				hItem.put("UrlFile", tam.getAbsolutePath());
				arrayInfoInvoice.add(hItem);
			}
			if (arrayInfoInvoice.size() == 0) {
				return fileInfo;
			}
			if (arrayInfoInvoice.size() == 1) {
				hItem = arrayInfoInvoice.get(0);
				file_zip = new File(hItem.get("UrlFile"));
				fileInfo.setContentFile(Files.readAllBytes(file_zip.toPath()));
				fileInfo.setFileName(fileName);
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
				fileInfo.setFileName("EINVOICE-BH.zip");
				fileInfo.setContentFile(bos.toByteArray());
			}
			return fileInfo;
		} catch (NullPointerException | MessagingException | UnsupportedEncodingException e) {
			return new FileInfo();
		}
	}

	@Override
	public FileInfo printEinvoice1All(JSONRoot jsonRoot) throws Exception {
		FileInfo fileInfo = new FileInfo();
		List<String> ids = null;
		Msg msg = jsonRoot.getMsg();
		MsgHeader header = msg.getMsgHeader();
		MsgPage page = msg.getMsgPage();
		ByteArrayOutputStream baosPDF = null;
		List<String> listFileNamePdfFinal = new ArrayList<>();
		HashMap<String, String> hItem = null;
		ArrayList<HashMap<String, String>> arrayInfoInvoice = new ArrayList<>();
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		File file_zip = null;

		Object objData = msg.getObjData();

		JsonNode jsonData = null;

		try {
			if (objData != null) {
				jsonData = Json.serializer().nodeFromObject(msg.getObjData());
			}
			String _id = "";
			String _token = commons.getTextJsonNode(jsonData.at("/_token")).replaceAll("\\s", "");
			String isConvert = commons.getTextJsonNode(jsonData.at("/IsConvert")).replaceAll("\\s", "");
//				ids = null;
//				try {
//					ids = Json.serializer().fromJson(commons.decodeBase64ToString(_token), new TypeReference<List<String>>() {
//					});
//				}catch(Exception e) {}
//				
//				
//				
//				for(int i = 0; i< ids.size(); i++ ) {
//					_id = ids.get(i) ;				

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
				Iterable<Document> cursor = mongoTemplate.getCollection("EInvoiceBH").aggregate(pipeline)
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
				String readMoneyInEnglish = docTmp.getEmbedded(Arrays.asList("UserConFig", "ReadMoneyInEnglish"), "");
				String MST = docTmp.getEmbedded(Arrays.asList("EInvoiceDetail", "NDHDon", "NBan", "MST"), "");
				String ImgLogo = docTmp.getEmbedded(Arrays.asList("DMMauSoKyHieu", "Templates", "ImgLogo"), "");
				String ImgBackground = docTmp.getEmbedded(Arrays.asList("DMMauSoKyHieu", "Templates", "ImgBackground"),
						"");
				String ImgQA = docTmp.getEmbedded(Arrays.asList("DMMauSoKyHieu", "Templates", "ImgQA"), "");
				String ImgVien = docTmp.getEmbedded(Arrays.asList("DMMauSoKyHieu", "Templates", "ImgVien"), "");

				String mskh = docTmp.getEmbedded(Arrays.asList("EInvoiceDetail", "TTChung", "KHMSHDon"), "")
						+ docTmp.getEmbedded(Arrays.asList("EInvoiceDetail", "TTChung", "KHHDon"), "");
				Integer shd = docTmp.getEmbedded(Arrays.asList("EInvoiceDetail", "TTChung", "SHDon"), 0);
				String ParamUSD = docTmp.getEmbedded(Arrays.asList("EInvoiceDetail", "TToan", "ParamUSD"), "");

				String dir = docTmp.get("Dir", "");
				String signStatusCode = docTmp.get("SignStatusCode", "");
				String eInvoiceStatus = docTmp.get("EInvoiceStatus", "");
				String secureKey = docTmp.get("SecureKey", "");
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
				baosPDF = jpUtils.createFinalInvoice(fileJP, doc, secureKey, CheckView, readMoneyInEnglish, numberRowInPage, numberRowInPageMultiPage,
						numberCharsInRow, MST, link, ParamUSD,
						Paths.get(SystemParams.DIR_E_INVOICE_TEMPLATE, "images", MST, ImgLogo).toString(),
						Paths.get(SystemParams.DIR_E_INVOICE_TEMPLATE, "images", MST, ImgBackground).toString(),
						Paths.get(SystemParams.DIR_E_INVOICE_TEMPLATE, "images", MST, ImgQA).toString(),
						Paths.get(SystemParams.DIR_E_INVOICE_TEMPLATE, "images", MST, ImgVien).toString(),

						"Y".equals(isConvert), Constants.INVOICE_STATUS.DELETED.equals(eInvoiceStatus), isThayThe,
						isDieuChinh, false);

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
				LocalDateTime time_dem  = LocalDateTime.now();
				String time = time_dem.format(format_time);
				String name_company = removeAccent(header.getUserFullName());
				System.out.println(time +" "+name_company+" vua xuat PDF hang loat hoa don BH.");
				
//				ByteArrayOutputStream out = commons.doMergeMultiPdf(listFileNamePdfFinal);
//				fileInfo.setContentFile(out.toByteArray());
				return fileInfo;
			}

		} catch (NullPointerException | MessagingException | UnsupportedEncodingException e) {
			return new FileInfo();
		}
	}

	//
	public FileInfo downLoadFile(JSONRoot jsonRoot) throws Exception {
		FileInfo fileInfo = new FileInfo();
		ArrayList<HashMap<String, String>> arrayInfoInvoice = new ArrayList<>();
		HashMap<String, String> hItem = null;
		File file_zip = null;
		String fileName = "";
		List<String> ids = null;
		Msg msg = jsonRoot.getMsg();
		MsgHeader header = msg.getMsgHeader();
		MsgPage page = msg.getMsgPage();
		ByteArrayOutputStream baosPDF = null;
		List<String> listFileNamePdfFinal = new ArrayList<>();
		Object objData = msg.getObjData();
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		JsonNode jsonData = null;
		if (objData != null) {
			jsonData = Json.serializer().nodeFromObject(msg.getObjData());
		}
		String _id = "";
		String _token = commons.getTextJsonNode(jsonData.at("/_token")).replaceAll("\\s", "");
		ids = null;
		try {
			ids = Json.serializer().fromJson(commons.decodeBase64ToString(_token), new TypeReference<List<String>>() {
			});
		} catch (Exception e) {
		}

		String FileName = "";
		for (int i = 0; i < ids.size(); i++) {
			_id = ids.get(i);
			ObjectId objectId = null;
			try {
				objectId = new ObjectId(_id);
			} catch (Exception e) {
			}

			Document docFind = new Document("_id", objectId).append("IsDelete", new Document("$ne", true));
			Document docTmp = null;
			Iterable<Document> cursor = mongoTemplate.getCollection("DMTemplates").find(docFind);
			Iterator<Document> iter = cursor.iterator();
			if (iter.hasNext()) {
				docTmp = iter.next();
			}

			if (null == docTmp) {
				return new FileInfo();
			}
			FileName = docTmp.get("FileName", "");

			File file = new File(SystemParams.DIR_E_INVOICE_TEMPLATE, FileName);
			File tam = new File(SystemParams.DIR_TEMPORARY, FileName);
			copyFileUsingStream(file, tam);

			hItem = new HashMap<>();
			hItem.put("UrlFile", tam.getAbsolutePath());
			arrayInfoInvoice.add(hItem);
		}
		if (arrayInfoInvoice.size() == 0) {
			return fileInfo;
		}

		if (arrayInfoInvoice.size() == 1) {
			hItem = arrayInfoInvoice.get(0);
			file_zip = new File(hItem.get("UrlFile"));
			fileInfo.setContentFile(Files.readAllBytes(file_zip.toPath()));
			fileInfo.setFileName(FileName);
			return fileInfo;
		}

		return fileInfo;
	}

	// GET XML THUE
	public FileInfo getXmlThue(JSONRoot jsonRoot) throws Exception {
		FileInfo fileInfo = new FileInfo();
		ArrayList<HashMap<String, String>> arrayInfoInvoice = new ArrayList<>();
		HashMap<String, String> hItem = null;
		File file_zip = null;
		List<String> ids = null;
		Msg msg = jsonRoot.getMsg();
		MsgHeader header = msg.getMsgHeader();
		MsgPage page = msg.getMsgPage();
		Object objData = msg.getObjData();
		JsonNode jsonData = null;
		if (objData != null) {
			jsonData = Json.serializer().nodeFromObject(msg.getObjData());
		}
		String mtd = "";
		String _token = commons.getTextJsonNode(jsonData.at("/_token")).replaceAll("\\s", "");
		ids = null;
		try {
			ids = Json.serializer().fromJson(commons.decodeBase64ToString(_token), new TypeReference<List<String>>() {
			});
		} catch (Exception e) {
		}

		String FileName = "";
		for (int i = 0; i < ids.size(); i++) {
			mtd = ids.get(i);

			String tracuu = tctnService.callTraCuuThongDiepString(mtd);

			FileName = mtd + ".xml";
			String dir_file = SystemParams.DIR_E_INVOICE_DATA;
			String save_file = dir_file + "/" + FileName;
			// SAVE FILE XML
			FileWriter fw = new FileWriter(save_file);
			fw.write(tracuu);
			fw.close();

			// END SAVE FILE XML

			File file = new File(save_file);
			File tam = new File(SystemParams.DIR_TEMPORARY, FileName);
			copyFileUsingStream(file, tam);

			hItem = new HashMap<>();
			hItem.put("UrlFile", tam.getAbsolutePath());
			arrayInfoInvoice.add(hItem);
		}
		if (arrayInfoInvoice.size() == 0) {
			return fileInfo;
		}

		if (arrayInfoInvoice.size() == 1) {
			hItem = arrayInfoInvoice.get(0);
			file_zip = new File(hItem.get("UrlFile"));
			fileInfo.setContentFile(Files.readAllBytes(file_zip.toPath()));
			fileInfo.setFileName(FileName);
			return fileInfo;
		}

		return fileInfo;
	}

	// PHAN QUYEN

	@Transactional(rollbackFor = { Exception.class })
	@Override
	public MsgRsp getFullRightAdmin(JSONRoot jsonRoot) throws Exception {
		Msg msg = jsonRoot.getMsg();
		MsgHeader header = msg.getMsgHeader();
		MsgPage page = msg.getMsgPage();
		List<Document> pipeline = null;
		try {

			pipeline = new ArrayList<Document>();
			pipeline.add(new Document("$match", new Document("ActiveFlag", true)));

			pipeline.add(new Document("$lookup", new Document("from", "FullParamsSystems")
					// .append("let", new Document("varAgentParentID", "$AgentParentID"))
					.append("pipeline", Arrays.asList(new Document("$match", new Document("$expr", new Document("$and",
							Arrays.asList(new Document("$eq", Arrays.asList("$k", "FULL-RIGHT-ADMIN"))
							// , new Document("$eq", Arrays.asList("$$varAgentParentID", null))
							)))), new Document("$limit", 1))).append("as", "FullRightAdmin")));

			pipeline.add(new Document("$unwind",
					new Document("path", "$FullRightAdmin").append("preserveNullAndEmptyArrays", true)));
			pipeline.add(new Document("$lookup", new Document("from", "SystemServicePackage")
					// .append("let", new Document("varServicePackageInfo",
					// "$ServicePackageInfo").append("varAgentParentID", "$AgentParentID"))
					.append("pipeline", Arrays.asList(new Document("$match", new Document("$expr",
							new Document("$and", Arrays.asList(new Document("$ne", Arrays.asList("$IsDelete", true))
							// new Document("$eq", Arrays.asList("$$varServicePackageInfo._id", new
							// Document("$toString", "$_id")))
							// , new Document("$ne", Arrays.asList("$$varAgentParentID", null))
							)))))).append("as", "ServicePackage")));
			pipeline.add(new Document("$unwind",
					new Document("path", "$ServicePackage").append("preserveNullAndEmptyArrays", true)));
//					pipeline.add(new Document("$unset", Arrays.asList("ServicePackageInfo"))
//						);

			Document docTmp = null;
			Document docTmpSub = null;
			Iterable<Document> cursor = mongoTemplate.getCollection("ApiLicenseKey").aggregate(pipeline)
					.allowDiskUse(true);
			Iterator<Document> iter = cursor.iterator();
			if (iter.hasNext()) {
				docTmp = iter.next();
			}

			MsgRsp rsp = new MsgRsp(header);
			MspResponseStatus responseStatus = null;
			if (null == docTmp) {
				responseStatus = new MspResponseStatus(9999, Constants.MAP_ERROR.get(9999));
				rsp.setResponseStatus(responseStatus);
				return rsp;
			}

			responseStatus = new MspResponseStatus(0, "SUCCESS");
			rsp.setResponseStatus(responseStatus);
			if (null != docTmp.get("FullRightAdmin")) {
				docTmpSub = (Document) docTmp.get("FullRightAdmin");
				rsp.setObjData(docTmpSub.get("v"));
			} else if (null != docTmp.get("ServicePackage")) {
				docTmpSub = (Document) docTmp.get("ServicePackage");
				rsp.setObjData(docTmpSub.get("Functions"));
			}
//					rsp.setObjData(docTmp.get("v"));
			return rsp;
		} catch (Exception e) {
			throw e;
		} finally {
		}
	}

	// IN PHIEU XUAT KHO
	public FileInfo exportXml(JSONRoot jsonRoot) throws Exception {
		FileInfo fileInfo = new FileInfo();
		ArrayList<HashMap<String, String>> arrayInfoInvoice = new ArrayList<>();
		HashMap<String, String> hItem = null;
		File file_zip = null;
		String fileName = "";
		List<String> ids = null;
		Msg msg = jsonRoot.getMsg();
		MsgHeader header = msg.getMsgHeader();
		MsgPage page = msg.getMsgPage();
		ByteArrayOutputStream baosPDF = null;
		List<String> listFileNamePdfFinal = new ArrayList<>();
		Object objData = msg.getObjData();
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		JsonNode jsonData = null;
		if (objData != null) {
			jsonData = Json.serializer().nodeFromObject(msg.getObjData());
		}
		String _id = "";
		String _token = commons.getTextJsonNode(jsonData.at("/_token")).replaceAll("\\s", "");
//					ids = null;
//					try {
//						ids = Json.serializer().fromJson(commons.decodeBase64ToString(_token), new TypeReference<List<String>>() {
//						});
//					}catch(Exception e) {}
//					
//					
//					
//					for(int i = 0; i< ids.size(); i++ ) {					
//					_id = ids.get(i) ;

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
			String isConvert = commons.getTextJsonNode(jsonData.at("/IsConvert")).replaceAll("\\s", "");
			ObjectId objectId = null;
			try {
				objectId = new ObjectId(_id);
			} catch (Exception e) {
			}

			Document docFind = new Document("IssuerId", header.getIssuerId()).append("_id", objectId).append("IsDelete",
					new Document("$ne", true));
			List<Document> pipeline = new ArrayList<Document>();
			pipeline.add(new Document("$match", docFind));
			pipeline.add(new Document("$lookup", new Document("from", "DMMauSoKyHieu")
					.append("let",
							new Document("vIssuerId", "$IssuerId").append("vMauSoHD",
									"$EInvoiceDetail.TTChung.MauSoHD"))
					.append("pipeline", Arrays.asList(new Document("$match", new Document("$expr", new Document("$and",
							Arrays.asList(new Document("$eq", Arrays.asList("$$vIssuerId", "$IssuerId")),
									new Document("$eq",
											Arrays.asList(new Document("$toString", "$_id"), "$$vMauSoHD"))))))))
					.append("as", "DMMauSoKyHieu")));
			pipeline.add(new Document("$unwind",
					new Document("path", "$DMMauSoKyHieu").append("preserveNullAndEmptyArrays", true)));

			Document docTmp = null;
			Iterable<Document> cursor = mongoTemplate.getCollection("EInvoicePXK").aggregate(pipeline)
					.allowDiskUse(true);
			Iterator<Document> iter = cursor.iterator();
			if (iter.hasNext()) {
				docTmp = iter.next();
			}
			if (null == docTmp || docTmp.get("DMMauSoKyHieu") == null) {
				return new FileInfo();
			}
			String mskh = docTmp.getEmbedded(Arrays.asList("EInvoiceDetail", "TTChung", "KHMSHDon"), "")
					+ docTmp.getEmbedded(Arrays.asList("EInvoiceDetail", "TTChung", "KHHDon"), "");
			Integer shd = docTmp.getEmbedded(Arrays.asList("EInvoiceDetail", "TTChung", "SHDon"), 0);
			String namexml = mskh + "_" + shd + ".xml";
			boolean isDieuChinh = "2".equals(docTmp.getEmbedded(Arrays.asList("HDSS", "TCTBao"), ""));
			boolean isThayThe = "3".equals(docTmp.getEmbedded(Arrays.asList("HDSS", "TCTBao"), ""));

			String MST = docTmp.getEmbedded(Arrays.asList("EInvoiceDetail", "NDHDon", "NBan", "MST"), "");
			String ImgLogo = docTmp.getEmbedded(Arrays.asList("DMMauSoKyHieu", "Templates", "ImgLogo"), "");
			String ImgBackground = docTmp.getEmbedded(Arrays.asList("DMMauSoKyHieu", "Templates", "ImgBackground"), "");

			String dir = docTmp.get("Dir", "");
			String signStatusCode = docTmp.get("SignStatusCode", "");
			String eInvoiceStatus = docTmp.get("EInvoiceStatus", "");
			String MCCQT = docTmp.get("MCCQT", "");
			fileName = _id + ".xml";
			if ("SIGNED".equals(signStatusCode) && !"".equals(MCCQT)) {
				fileName = _id + "_" + MCCQT + ".xml";
				String fileName_ = _id + "_" + MCCQT + "_" + shd + ".xml";
				/* CHECK MCCQT GET DATA XML */

				File file_xml = new File(dir, fileName);

				org.w3c.dom.Document doc_xml = commons.fileToDocument(file_xml);

				XPath xPath_xml = XPathFactory.newInstance().newXPath();
				Node nodeHDon = null;
				for (int J = 1; J <= 20; J++) {
					nodeHDon = (Node) xPath_xml.evaluate("/KetQuaTraCuu/DuLieu/TDiep[" + J + "]/DLieu/HDon", doc_xml,
							XPathConstants.NODE);
					if (nodeHDon != null)
						break;
				}
				/* FILE CHUA DUOC DUI DEN CO QUAN THUE */
				if (null == nodeHDon) {
					nodeHDon = (Node) xPath_xml.evaluate("/HDon", doc_xml, XPathConstants.NODE);
				}

				DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
				DocumentBuilder builder = factory.newDocumentBuilder();
				org.w3c.dom.Document rTCTN_HDON = builder.newDocument();
				rTCTN_HDON.appendChild(rTCTN_HDON.importNode(nodeHDon, true));
				/* END CHECK GET DATA IN RESULT IN FILE XML TO TAX */
				boolean boo_ = false;
				boo_ = commons.docW3cToFile(rTCTN_HDON, dir, fileName_);

				if (boo_ == true) {
					fileName = fileName_;
				}
				/* END CHECK MCCQT GET DATA XML */
			} else {
				if ("SIGNED".equals(signStatusCode)) {
					fileName = _id + "_signed.xml";
				}
			}
			// namexml

			File file = new File(dir, fileName);
			File tam = new File(dir, namexml);
			copyFileUsingStream(file, tam);

			hItem = new HashMap<>();
			hItem.put("UrlFile", tam.getAbsolutePath());
			arrayInfoInvoice.add(hItem);
		}
		if (arrayInfoInvoice.size() == 0) {
			return fileInfo;
		}
		if (arrayInfoInvoice.size() == 1) {
			hItem = arrayInfoInvoice.get(0);
			file_zip = new File(hItem.get("UrlFile"));
			fileInfo.setContentFile(Files.readAllBytes(file_zip.toPath()));
			fileInfo.setFileName(fileName);
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
			fileInfo.setFileName("EINVOICE_PXK.zip");
			fileInfo.setContentFile(bos.toByteArray());
		}
		return fileInfo;
	}

	// IN PDF HANG LOAT
	public FileInfo exportPDF(JSONRoot jsonRoot) throws Exception {
		FileInfo fileInfo = new FileInfo();
		ArrayList<HashMap<String, String>> arrayInfoInvoice = new ArrayList<>();
		HashMap<String, String> hItem = null;
		File file_zip = null;
		String fileName = "";
		List<String> ids = null;
		Msg msg = jsonRoot.getMsg();
		MsgHeader header = msg.getMsgHeader();
		MsgPage page = msg.getMsgPage();
		ByteArrayOutputStream baosPDF = null;
		List<String> listFileNamePdfFinal = new ArrayList<>();
		Object objData = msg.getObjData();
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		JsonNode jsonData = null;
		if (objData != null) {
			jsonData = Json.serializer().nodeFromObject(msg.getObjData());
		}
		String _id = "";
		String _token = commons.getTextJsonNode(jsonData.at("/_token")).replaceAll("\\s", "");
		ids = null;
		try {
			ids = Json.serializer().fromJson(commons.decodeBase64ToString(_token), new TypeReference<List<String>>() {
			});
		} catch (Exception e) {
		}

		// VONG LAP FOR CHO CHUOI ID
		for (int i = 0; i < ids.size(); i++) {
			_id = ids.get(i);
			String isConvert = commons.getTextJsonNode(jsonData.at("/IsConvert")).replaceAll("\\s", "");
			ObjectId objectId = null;
			try {
				objectId = new ObjectId(_id);
			} catch (Exception e) {
			}

			Document docFind = new Document("IssuerId", header.getIssuerId()).append("_id", objectId).append("IsDelete",
					new Document("$ne", true));
			List<Document> pipeline = new ArrayList<Document>();
			pipeline.add(new Document("$match", docFind));
			pipeline.add(new Document("$lookup", new Document("from", "DMMauSoKyHieu")
					.append("let",
							new Document("vIssuerId", "$IssuerId").append("vMauSoHD",
									"$EInvoiceDetail.TTChung.MauSoHD"))
					.append("pipeline", Arrays.asList(new Document("$match", new Document("$expr", new Document("$and",
							Arrays.asList(new Document("$eq", Arrays.asList("$$vIssuerId", "$IssuerId")),
									new Document("$eq",
											Arrays.asList(new Document("$toString", "$_id"), "$$vMauSoHD"))))))))
					.append("as", "DMMauSoKyHieu")));
			pipeline.add(new Document("$unwind",
					new Document("path", "$DMMauSoKyHieu").append("preserveNullAndEmptyArrays", true)));
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
			Iterable<Document> cursor = mongoTemplate.getCollection("EInvoicePXK").aggregate(pipeline)
					.allowDiskUse(true);
			Iterator<Document> iter = cursor.iterator();
			if (iter.hasNext()) {
				docTmp = iter.next();
			}
			if (null == docTmp || docTmp.get("DMMauSoKyHieu") == null) {
				return new FileInfo();
			}
			String mskh = docTmp.getEmbedded(Arrays.asList("EInvoiceDetail", "TTChung", "KHMSHDon"), "")
					+ docTmp.getEmbedded(Arrays.asList("EInvoiceDetail", "TTChung", "KHHDon"), "");
			Integer shd = docTmp.getEmbedded(Arrays.asList("EInvoiceDetail", "TTChung", "SHDon"), 0);
			String namePDF = mskh + "_" + shd + ".pdf";
			boolean isDieuChinh = "2".equals(docTmp.getEmbedded(Arrays.asList("HDSS", "TCTBao"), ""));
			boolean isThayThe = "3".equals(docTmp.getEmbedded(Arrays.asList("HDSS", "TCTBao"), ""));

			String MST = docTmp.getEmbedded(Arrays.asList("EInvoiceDetail", "NDHDon", "NBan", "MST"), "");
			String ImgLogo = docTmp.getEmbedded(Arrays.asList("DMMauSoKyHieu", "Templates", "ImgLogo"), "");
			String ImgBackground = docTmp.getEmbedded(Arrays.asList("DMMauSoKyHieu", "Templates", "ImgBackground"), "");
			String ImgQA = docTmp.getEmbedded(Arrays.asList("DMMauSoKyHieu", "Templates", "ImgQA"), "");
			String ImgVien = docTmp.getEmbedded(Arrays.asList("DMMauSoKyHieu", "Templates", "ImgVien"), "");
			String link = docTmp.getEmbedded(Arrays.asList("PramLink", "LinkPortal"), "");
			String dir = docTmp.get("Dir", "");
			String signStatusCode = docTmp.get("SignStatusCode", "");
			String eInvoiceStatus = docTmp.get("EInvoiceStatus", "");
			String MCCQT = docTmp.get("MCCQT", "");
			fileName = _id + ".xml";
			if ("SIGNED".equals(signStatusCode) && !"".equals(MCCQT)) {
				fileName = _id + "_" + MCCQT + ".xml";
			} else {
				if ("SIGNED".equals(signStatusCode)) {
					fileName = _id + "_signed.xml";
				}
			}
			// namePDF
			String fileNamePDF = _id + ".pdf";
			File file1 = new File(dir, fileNamePDF);
			File file2 = new File(dir, fileName);
			if (!file1.exists()) {
				org.w3c.dom.Document doc = commons.fileToDocument(file2);

				String fileNameJP = docTmp.getEmbedded(Arrays.asList("DMMauSoKyHieu", "Templates", "FileName"), "");
				int numberRowInPage = docTmp.getEmbedded(Arrays.asList("DMMauSoKyHieu", "Templates", "RowsInPage"), 20);
				int numberRowInPageMultiPage = docTmp
						.getEmbedded(Arrays.asList("DMMauSoKyHieu", "Templates", "RowInPageMultiPage"), 26);
				int numberCharsInRow = docTmp.getEmbedded(Arrays.asList("DMMauSoKyHieu", "Templates", "CharsInRow"),
						50);
				String CheckView = docTmp.getEmbedded(Arrays.asList("UserConFig", "viewshd"), "");
				File fileJP = new File(SystemParams.DIR_E_INVOICE_TEMPLATE, fileNameJP);

				ByteArrayOutputStream baosPDF1 = null;
				baosPDF1 = jpUtils.createFinalInvoice2(fileJP, doc, docTmp, CheckView, numberRowInPage,
						numberRowInPageMultiPage, numberCharsInRow, MST, link,
						Paths.get(SystemParams.DIR_E_INVOICE_TEMPLATE, "images", MST, ImgLogo).toString(),
						Paths.get(SystemParams.DIR_E_INVOICE_TEMPLATE, "images", MST, ImgBackground).toString(),
						Paths.get(SystemParams.DIR_E_INVOICE_TEMPLATE, "images", MST, ImgQA).toString(),
						Paths.get(SystemParams.DIR_E_INVOICE_TEMPLATE, "images", MST, ImgVien).toString(),
						"Y".equals(isConvert), Constants.INVOICE_STATUS.DELETED.equals(eInvoiceStatus), isThayThe,
						isDieuChinh);
				/* LUU TAP TIN PDF */
				if (null != baosPDF1) {
					try (OutputStream fileOuputStream = new FileOutputStream(new File(dir, fileNamePDF))) {
						baosPDF1.writeTo(fileOuputStream);
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}

			File file = new File(dir, fileNamePDF);
			File tam = new File(dir, namePDF);
			copyFileUsingStream(file, tam);

			hItem = new HashMap<>();
			hItem.put("UrlFile", tam.getAbsolutePath());
			arrayInfoInvoice.add(hItem);
		}

		// KET THUC VONG LAP ID

		if (arrayInfoInvoice.size() == 0) {
			return fileInfo;
		}
		if (arrayInfoInvoice.size() == 1) {
			hItem = arrayInfoInvoice.get(0);
			file_zip = new File(hItem.get("UrlFile"));
			fileInfo.setContentFile(Files.readAllBytes(file_zip.toPath()));
			fileInfo.setFileName(fileName);
			return fileInfo;
		} else {
			/* NEN DANH SACH FILE PDF */
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
			fileInfo.setFileName("EINVOICE_PXK.zip");
			fileInfo.setContentFile(bos.toByteArray());
		}
		return fileInfo;
	}

	@Override
	public FileInfo printExportAll(JSONRoot jsonRoot) throws Exception {
		FileInfo fileInfo = new FileInfo();
		List<String> ids = null;
		Msg msg = jsonRoot.getMsg();
		MsgHeader header = msg.getMsgHeader();
		MsgPage page = msg.getMsgPage();
		ByteArrayOutputStream baosPDF = null;
		List<String> listFileNamePdfFinal = new ArrayList<>();
		
		HashMap<String, String> hItem = null;
		ArrayList<HashMap<String, String>> arrayInfoInvoice = new ArrayList<>();
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		File file_zip = null;
		
		Object objData = msg.getObjData();

		JsonNode jsonData = null;
		if (objData != null) {
			jsonData = Json.serializer().nodeFromObject(msg.getObjData());
		}
		String _id = "";
		String _token = commons.getTextJsonNode(jsonData.at("/_token")).replaceAll("\\s", "");
		String isConvert = commons.getTextJsonNode(jsonData.at("/IsConvert")).replaceAll("\\s", "");
		ids = null;
//						try {
//							ids = Json.serializer().fromJson(commons.decodeBase64ToString(_token), new TypeReference<List<String>>() {
//							});
//						}catch(Exception e) {}
//						
//						
//						
//						for(int i = 0; i< ids.size(); i++ ) {
//							_id = ids.get(i) ;

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

			Document docFind = new Document("IssuerId", header.getIssuerId()).append("_id", objectId).append("IsDelete",
					new Document("$ne", true));
			List<Document> pipeline = new ArrayList<Document>();
			pipeline.add(new Document("$match", docFind));
			pipeline.add(new Document("$lookup", new Document("from", "DMMauSoKyHieu")
					.append("let",
							new Document("vIssuerId", "$IssuerId").append("vMauSoHD",
									"$EInvoiceDetail.TTChung.MauSoHD"))
					.append("pipeline", Arrays.asList(new Document("$match", new Document("$expr", new Document("$and",
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
			Iterable<Document> cursor = mongoTemplate.getCollection("EInvoicePXK").aggregate(pipeline)
					.allowDiskUse(true);
			Iterator<Document> iter = cursor.iterator();
			if (iter.hasNext()) {
				docTmp = iter.next();
			}
			if (null == docTmp || docTmp.get("DMMauSoKyHieu") == null) {
				return new FileInfo();
			}

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
			String ImgBackground = docTmp.getEmbedded(Arrays.asList("DMMauSoKyHieu", "Templates", "ImgBackground"), "");
			String ImgQA = docTmp.getEmbedded(Arrays.asList("DMMauSoKyHieu", "Templates", "ImgQA"), "");
			String ImgVien = docTmp.getEmbedded(Arrays.asList("DMMauSoKyHieu", "Templates", "ImgVien"), "");
			String link = docTmp.getEmbedded(Arrays.asList("PramLink", "LinkPortal"), "");
			String dir = docTmp.get("Dir", "");
			String signStatusCode = docTmp.get("SignStatusCode", "");
			String eInvoiceStatus = docTmp.get("EInvoiceStatus", "");
			
			String mskh = docTmp.getEmbedded(Arrays.asList("EInvoiceDetail", "TTChung", "KHMSHDon"), "")
					+ docTmp.getEmbedded(Arrays.asList("EInvoiceDetail", "TTChung", "KHHDon"), "");
			Integer shd = docTmp.getEmbedded(Arrays.asList("EInvoiceDetail", "TTChung", "SHDon"), 0);
			
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
			int numberCharsInRow = docTmp.getEmbedded(Arrays.asList("DMMauSoKyHieu", "Templates", "CharsInRow"), 50);
			File fileJP = new File(SystemParams.DIR_E_INVOICE_TEMPLATE, fileNameJP);
			baosPDF = jpUtils.createFinalInvoice2(fileJP, doc, docTmp, CheckView, numberRowInPage,
					numberRowInPageMultiPage, numberCharsInRow, MST, link,
					Paths.get(SystemParams.DIR_E_INVOICE_TEMPLATE, "images", MST, ImgLogo).toString(),
					Paths.get(SystemParams.DIR_E_INVOICE_TEMPLATE, "images", MST, ImgBackground).toString(),
					Paths.get(SystemParams.DIR_E_INVOICE_TEMPLATE, "images", MST, ImgQA).toString(),
					Paths.get(SystemParams.DIR_E_INVOICE_TEMPLATE, "images", MST, ImgVien).toString(),

					"Y".equals(isConvert), Constants.INVOICE_STATUS.DELETED.equals(eInvoiceStatus), isThayThe,
					isDieuChinh);

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
			LocalDateTime time_dem  = LocalDateTime.now();
			String time = time_dem.format(format_time);
			String name_company = removeAccent(header.getUserFullName());
			System.out.println(time +" "+name_company+" vua xuat PDF hang loat PXK NOI BO.");
			
//			ByteArrayOutputStream out = commons.doMergeMultiPdf(listFileNamePdfFinal);
//			fileInfo.setContentFile(out.toByteArray());
			return fileInfo;
		}

	}

	// PHÂN QUYỀN ADMIN

	// PHAN QUYEN ADMIN

	@Transactional(rollbackFor = { Exception.class })
	@Override
	public MsgRsp getFullRightAdminManager(JSONRoot jsonRoot) throws Exception {
		Msg msg = jsonRoot.getMsg();
		MsgHeader header = msg.getMsgHeader();
		MsgPage page = msg.getMsgPage();
		List<Document> pipeline = null;
		try {

			pipeline = new ArrayList<Document>();
			pipeline.add(new Document("$match", new Document("ActiveFlag", true)));

			pipeline.add(new Document("$lookup", new Document("from", "FullParamsSystems")
					// .append("let", new Document("varAgentParentID", "$AgentParentID"))
					.append("pipeline", Arrays.asList(new Document("$match", new Document("$expr", new Document("$and",
							Arrays.asList(new Document("$eq", Arrays.asList("$k", "FULL-RIGHT-ADMIN-MANAGER"))
							// , new Document("$eq", Arrays.asList("$$varAgentParentID", null))
							)))), new Document("$limit", 1))).append("as", "FullRightAdminManager")));

			pipeline.add(new Document("$unwind",
					new Document("path", "$FullRightAdminManager").append("preserveNullAndEmptyArrays", true)));
			pipeline.add(new Document("$lookup", new Document("from", "SystemServicePackage")
					// .append("let", new Document("varServicePackageInfo",
					// "$ServicePackageInfo").append("varAgentParentID", "$AgentParentID"))
					.append("pipeline", Arrays.asList(new Document("$match", new Document("$expr",
							new Document("$and", Arrays.asList(new Document("$ne", Arrays.asList("$IsDelete", true))
							// new Document("$eq", Arrays.asList("$$varServicePackageInfo._id", new
							// Document("$toString", "$_id")))
							// , new Document("$ne", Arrays.asList("$$varAgentParentID", null))
							)))))).append("as", "ServicePackage")));
			pipeline.add(new Document("$unwind",
					new Document("path", "$ServicePackage").append("preserveNullAndEmptyArrays", true)));
//						pipeline.add(new Document("$unset", Arrays.asList("ServicePackageInfo"))
//							);

			Document docTmp = null;
			Document docTmpSub = null;
			Iterable<Document> cursor = mongoTemplate.getCollection("ApiLicenseKey").aggregate(pipeline)
					.allowDiskUse(true);
			Iterator<Document> iter = cursor.iterator();
			if (iter.hasNext()) {
				docTmp = iter.next();
			}

			MsgRsp rsp = new MsgRsp(header);
			MspResponseStatus responseStatus = null;
			if (null == docTmp) {
				responseStatus = new MspResponseStatus(9999, Constants.MAP_ERROR.get(9999));
				rsp.setResponseStatus(responseStatus);
				return rsp;
			}

			responseStatus = new MspResponseStatus(0, "SUCCESS");
			rsp.setResponseStatus(responseStatus);
			if (null != docTmp.get("FullRightAdminManager")) {
				docTmpSub = (Document) docTmp.get("FullRightAdminManager");
				rsp.setObjData(docTmpSub.get("v"));
			} else if (null != docTmp.get("ServicePackage")) {
				docTmpSub = (Document) docTmp.get("ServicePackage");
				rsp.setObjData(docTmpSub.get("Functions"));
			}
//						rsp.setObjData(docTmp.get("v"));
			return rsp;
		} catch (Exception e) {
			throw e;
		} finally {
		}
	}

	@Transactional(rollbackFor = { Exception.class })
	public MsgRsp detailMaSoThue(JSONRoot jsonRoot) throws Exception {
		Msg msg = jsonRoot.getMsg();
		MsgHeader header = msg.getMsgHeader();
		MsgPage page = msg.getMsgPage();
		Object objData = msg.getObjData();

		MsgRsp rsp = new MsgRsp(header);
		MspResponseStatus responseStatus = null;

		JsonNode jsonData = null;
		String taxCode = "";
		if (objData != null) {
			jsonData = Json.serializer().nodeFromObject(msg.getObjData());
			taxCode = commons.getTextJsonNode(jsonData.at("/MST")).trim().replaceAll("\\s+", " ");
		}

		try {
			String urlString = String.format("https://api.vietqr.io/v2/business/%s", taxCode);
			URL url = new URL(urlString);
			HttpsURLConnection con = (HttpsURLConnection) url.openConnection();
			con.setRequestMethod("GET");
			
//			System.out.println(con.getResponseCode());
			
			if(con.getResponseCode() == 200) {
				BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
				String inputLine = "";
				StringBuilder content = new StringBuilder();
				while((inputLine = in.readLine()) != null) {
					content.append(inputLine);
				}
				in.close();
				con.disconnect();
//				System.out.println(content.toString());
				
				JsonNode jsonNode = Json.serializer().nodeFromJson(content.toString());
				if("00".equals(commons.getTextJsonNode(jsonNode.at("/code")))) {
					HashMap<String, String> hR = new HashMap<String, String>();
					
					hR.put("ten_cong_ty", commons.getTextJsonNode(jsonNode.at("/data/name")));
					hR.put("dia_chi", commons.getTextJsonNode(jsonNode.at("/data/address")));
					responseStatus = new MspResponseStatus(0, "SUCCESS");
					rsp.setResponseStatus(responseStatus);
					rsp.setObjData(hR);
					return rsp;
				}else {
					responseStatus = new MspResponseStatus(1, commons.getTextJsonNode(jsonNode.at("/desc")));
					rsp.setResponseStatus(responseStatus);
					return rsp;
				}
				
			}else {
				responseStatus = new MspResponseStatus(1, "Không tìm thấy thông tin khách hàng.");
				rsp.setResponseStatus(responseStatus);
				return rsp;
			}
			
			
			
//			/* LAY TOKEN */
//			String request = "https://masothue.com/Ajax/Token/";
//			URL url = new URL(request);
//			HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();
//			conn.setDoOutput(true);
//			conn.setInstanceFollowRedirects(false);
//			conn.setRequestMethod("POST");
//			conn.setRequestProperty("User-Agent",
//					"Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/90.0.4430.93 Safari/537.36");
//			conn.setRequestProperty("Content-type", "application/x-www-form-urlencoded; charset=UTF-8");
//			conn.setUseCaches(false);
//
//			OutputStreamWriter writer = new OutputStreamWriter(conn.getOutputStream());
//			writer.write("");
//			writer.flush();
//
//			StringBuilder sb = new StringBuilder();
//			String line;
//			BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
//			while ((line = reader.readLine()) != null) {
//				sb.append(line);
//			}
//			writer.close();
//			reader.close();
//
//			JsonNode jsonNode = Json.serializer().nodeFromJson(sb.toString());
//			String token_ = jsonNode.at("/token").asText();
//
//			/* LAY THONG TIN MST */
//			String urlSearch = String.format("https://masothue.com/Search/?q=%s&type=auto&token=%s&force-search=1",
//					taxCode, token_);
//			org.jsoup.nodes.Document doc = Jsoup.connect(urlSearch).get();
//			Element element = doc.select("table.table-taxinfo").first();
//			Element elemTmp = null;
//			if (null == element) {
//				// NEU CO NHIEU CHI NHANH; THI LAY DONG DAU TIEN
//				element = doc.select("div.tax-listing div[data-prefetch] a").first();
//				if (null != element && null != element.attr("href") && !"".equals(element.attr("href"))) {
//					urlSearch = "https://masothue.com" + element.attr("href");
//					doc = Jsoup.connect(urlSearch).get();
//					element = doc.select("table.table-taxinfo").first();
//				}
//			}
//			if (null == element) {
//				responseStatus = new MspResponseStatus(999, "error");
//				rsp.setResponseStatus(responseStatus);
//				return rsp;
//			}
//
//			HashMap<String, String> hR = parserInfoCompany(doc);
//
//			elemTmp = element.selectFirst("thead tr th span");
//			if (null != elemTmp) {
//				hR.put("ten_cong_ty", elemTmp.text());
//			}
//
//			Elements elements = element.select("tbody tr");
//			Iterator<Element> iterator = elements.iterator();
//			while (iterator.hasNext()) {
//				elemTmp = iterator.next();
//				if (elemTmp.select("td").size() == 2) {
//					hR.put(commons.deAccent(elemTmp.select("td:eq(0)").text().toLowerCase().replaceAll("\\s", "_")),
//							elemTmp.select("td:eq(1)").text().trim());
//					if (elemTmp.select("td:eq(1) span[itemprop='name'] a") != null
//							&& elemTmp.select("td:eq(1) span[itemprop='name'] a").size() > 0) {
//						hR.put("dai_dien_phap_luat", elemTmp.selectFirst("td:eq(1) span[itemprop='name'] a").text());
//					}
//				}
//
////							if(elemTmp.selectFirst("td:eq(0)") != null) {}
////							hR.put("", elemTmp.select(cssQuery))
//			}
//
//			if (hR.get("ngay_hoat_dong") != null && commons.checkLocalDate(hR.get("ngay_hoat_dong"), "yyyy-MM-dd")) {
//				hR.put("ngay_hoat_dong", commons.convertLocalDateTimeStringToString(hR.get("ngay_hoat_dong"),
//						"yyyy-MM-dd", Constants.FORMAT_DATE.FORMAT_DATE_WEB));
//			}
//
//			elemTmp = element.nextElementSibling();
//
//			if (elemTmp.hasAttr("role")) {
//				hR.put("remark", elemTmp.text());
//			}

//			responseStatus = new MspResponseStatus(0, "SUCCESS");
//			rsp.setResponseStatus(responseStatus);
//			rsp.setObjData(hR);
//			return rsp;
		} catch (Exception e) {
			throw e;
		} finally {
		}
	}

	private HashMap<String, String> parserInfoCompany(org.jsoup.nodes.Document doc) throws Exception {
		HashMap<String, String> hR = new HashMap<String, String>();

		Element element = doc.select("table.table-taxinfo").first();
		Element elemTmp = null;

		elemTmp = element.selectFirst("thead tr th span");
		if (null != elemTmp) {
			hR.put("ten_cong_ty", elemTmp.text());
		}

		Elements elements = element.select("tbody tr");
		Iterator<Element> iterator = elements.iterator();
		while (iterator.hasNext()) {
			elemTmp = iterator.next();
			if (elemTmp.select("td").size() == 2) {
				hR.put(commons.deAccent(elemTmp.select("td:eq(0)").text().toLowerCase().replaceAll("\\s", "_")),
						elemTmp.select("td:eq(1)").text().trim());
			}

//						if(elemTmp.selectFirst("td:eq(0)") != null) {}
//						hR.put("", elemTmp.select(cssQuery))
		}

		if (hR.get("ngay_hoat_dong") != null && commons.checkLocalDate(hR.get("ngay_hoat_dong"), "yyyy-MM-dd")) {
			hR.put("ngay_hoat_dong", commons.convertLocalDateTimeStringToString(hR.get("ngay_hoat_dong"), "yyyy-MM-dd",
					Constants.FORMAT_DATE.FORMAT_DATE_WEB));
		}

		elemTmp = element.nextElementSibling();

		if (elemTmp.hasAttr("role")) {
			hR.put("remark", elemTmp.text());
		}

		return hR;
	}

	@Override
	public MsgRsp listSearchCustomerUpdate(JSONRoot jsonRoot) throws Exception {
		Msg msg = jsonRoot.getMsg();
		MsgHeader header = msg.getMsgHeader();
		MsgPage page = msg.getMsgPage();
		Object objData = msg.getObjData();

		String taxCode = "";
		String companyName = "";
		String customerName = "";

		JsonNode jsonData = null;
		if (objData != null) {
			jsonData = Json.serializer().nodeFromObject(objData);
			taxCode = commons.getTextJsonNode(jsonData.at("/TaxCode")).trim().replaceAll("\\s+", " ");
			companyName = commons.getTextJsonNode(jsonData.at("/CompanyName")).trim().replaceAll("\\s+", " ");
			customerName = commons.getTextJsonNode(jsonData.at("/CustomerName")).trim().replaceAll("\\s+", " ");
		}

		MsgRsp rsp = new MsgRsp(header);
		MspResponseStatus responseStatus = null;

		ObjectId objectId = null;
		Document docTmp = null;
		Iterable<Document> cursor = null;
		Iterator<Document> iter = null;
		List<Document> pipeline = new ArrayList<Document>();

		Document docMatch = new Document("IssuerId", header.getIssuerId()).append("IsDelete",
				new Document("$ne", true));
		if (!"".equals(taxCode))
			docMatch.append("TaxCode",
					new Document("$regex", commons.regexEscapeForMongoQuery(taxCode)).append("$options", "i"));
		if (!"".equals(companyName))
			docMatch.append("CompanyName",
					new Document("$regex", commons.regexEscapeForMongoQuery(companyName)).append("$options", "i"));
		if (!"".equals(customerName))
			docMatch.append("CustomerName",
					new Document("$regex", commons.regexEscapeForMongoQuery(customerName)).append("$options", "i"));

		pipeline = new ArrayList<Document>();
		pipeline.add(new Document("$match", docMatch));
		pipeline.add(new Document("$sort", new Document("Stock", 1).append("Code", 1).append("_id", 1)));
		pipeline.addAll(createFacetForSearchNotSort(page));

		cursor = mongoTemplate.getCollection("DMHistoryCustomer").aggregate(pipeline).allowDiskUse(true);
		iter = cursor.iterator();
		if (iter.hasNext()) {
			docTmp = iter.next();
		}

		rsp = new MsgRsp(header);
		responseStatus = null;
		if (null == docTmp) {
			responseStatus = new MspResponseStatus(9999, Constants.MAP_ERROR.get(9999));
			rsp.setResponseStatus(responseStatus);
			return rsp;
		}

		page.setTotalRows(docTmp.getInteger("total", 0));
		rsp.setMsgPage(page);

		List<Document> rows = null;
		if (docTmp.get("data") != null && docTmp.get("data") instanceof List) {
			rows = docTmp.getList("data", Document.class);
		}

		ArrayList<HashMap<String, Object>> rowsReturn = new ArrayList<HashMap<String, Object>>();
		HashMap<String, Object> hItem = null;
		if (null != rows) {
			for (Document doc : rows) {
				objectId = (ObjectId) doc.get("_id");
				hItem = new HashMap<String, Object>();
				hItem.put("_id", objectId.toString());
				hItem.put("TaxCode", doc.get("TaxCode"));
				hItem.put("CustomerCode", doc.get("CustomerCode"));
				hItem.put("CompanyName", doc.get("CompanyName"));
				hItem.put("CustomerName", doc.get("CustomerName"));
				hItem.put("Address", doc.get("Address"));
				hItem.put("Email", doc.get("Email"));
				hItem.put("EmailCC", doc.get("EmailCC"));
				hItem.put("Phone", doc.get("Phone"));
				hItem.put("AccountNumber", doc.get("AccountNumber"));
				hItem.put("AccountBankName", doc.get("AccountBankName"));

				hItem.put("Province", doc.get("Province"));
				hItem.put("CustomerGroup1", doc.get("CustomerGroup1"));
				hItem.put("CustomerGroup2", doc.get("CustomerGroup2"));
				hItem.put("CustomerGroup3", doc.get("CustomerGroup3"));

				hItem.put("InfoCreated", doc.get("InfoCreated"));
				rowsReturn.add(hItem);
			}
		}

		responseStatus = new MspResponseStatus(0, "SUCCESS");
		rsp.setResponseStatus(responseStatus);

		HashMap<String, Object> mapDataR = new HashMap<String, Object>();
		mapDataR.put("rows", rowsReturn);
		rsp.setObjData(mapDataR);
		return rsp;
	}

	public FileInfo viewPdfTiepnhan(JSONRoot jsonRoot) throws Exception {
		FileInfo fileInfo = new FileInfo();
		Msg msg = jsonRoot.getMsg();
		MsgHeader header = msg.getMsgHeader();
		Object objData = msg.getObjData();
		JsonNode jsonData = null;
		if (objData != null) {
			jsonData = Json.serializer().nodeFromObject(msg.getObjData());
			String _id = this.commons.getTextJsonNode(jsonData.at("/_id")).replaceAll("\\s", "");
			String isConvert = this.commons.getTextJsonNode(jsonData.at("/IsConvert")).replaceAll("\\s", "");
			ObjectId objectId = null;

			try {
				objectId = new ObjectId(_id);
			} catch (Exception var29) {
			}

			ObjectId objectIdIssu = null;

			try {
				objectIdIssu = new ObjectId(header.getIssuerId());
			} catch (Exception var28) {
			}

			List<Document> pipeline = new ArrayList<Document>();
			pipeline.add(new Document("$match",
					(new Document("_id", objectId)).append("IsDelete", new Document("$ne", true))));
//			pipeline.add(
//					new Document("$lookup",
//							(new Document("from", "Issuer"))
//									.append("pipeline",
//											Arrays.asList(
//													new Document("$match",
//															(new Document("_id", objectIdIssu)).append("IsDelete",
//																	new Document("$ne", true)))))
//									.append("as", "Issuer")));
//			pipeline.add(new Document("$unwind",
//					(new Document("path", "$Issuer")).append("preserveNullAndEmptyArrays", true)));
			Document docTmp = null;
			Iterable<Document> cursor = this.mongoTemplate.getCollection("EInvoiceHDSS").aggregate(pipeline)
					.allowDiskUse(true);
			Iterator<Document> iter = cursor.iterator();
			if (iter.hasNext()) {
				docTmp = (Document) iter.next();
			}

			String MTDiep = docTmp.get("MTDiep", "");
			String fileName = _id + "_" + MTDiep + ".xml";
//			String fileName = "688ae33895f24f2b4b4f2677_0315382923202507311030333789F2F213CDCB9497F804.xml";
			String dir = (String) docTmp.get("Dir", "");
			File file = new File(dir, fileName);
			if (file.exists() && file.isFile()) {
				org.w3c.dom.Document doc = this.commons.fileToDocument(file);
				String fileNameJP = "Mau-TB-01-TB-SSĐT.jrxml";
				File fileJP = new File(SystemParams.DIR_E_INVOICE_TEMPLATE, fileNameJP);
				ByteArrayOutputStream baosPDF = null;
				baosPDF = this.jpUtils.viewPdfTiepnhanV1(fileJP, doc, docTmp.get("TNNT",""), docTmp.get("MST",""));
				fileInfo.setFileName("Mau-TB-01-TB-SSĐT.pdf");
				fileInfo.setContentFile(baosPDF.toByteArray());
				return fileInfo;
			} else {
				return new FileInfo();
			}
		} else {
			return new FileInfo();
		}
	}

	public FileInfo cttncnXml(JSONRoot jsonRoot) throws Exception {
		FileInfo fileInfo = new FileInfo();
		ArrayList<HashMap<String, String>> arrayInfoInvoice = new ArrayList<>();
		HashMap<String, String> hItem = null;
		File file_zip = null;
		String fileName = "";
		List<String> ids = null;
		Msg msg = jsonRoot.getMsg();
		MsgHeader header = msg.getMsgHeader();
		MsgPage page = msg.getMsgPage();

		Object objData = msg.getObjData();
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		JsonNode jsonData = null;
		if (objData != null) {
			jsonData = Json.serializer().nodeFromObject(msg.getObjData());
		}
		String _id = "";
		String _token = commons.getTextJsonNode(jsonData.at("/_token")).replaceAll("\\s", "");
		ids = null;

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

			Document docFind = new Document("IssuerId", header.getIssuerId()).append("_id", objectId).append("IsDelete",
					new Document("$ne", true));
			List<Document> pipeline = new ArrayList<Document>();
			pipeline.add(new Document("$match", docFind));
			pipeline.add(new Document("$lookup", new Document("from", "DMMSTNCN")
					.append("let", new Document("vIssuerId", "$IssuerId").append("vMauSoHD", "$MauSoHD"))
					.append("pipeline", Arrays.asList(new Document("$match", new Document("$expr", new Document("$and",
							Arrays.asList(new Document("$eq", Arrays.asList("$$vIssuerId", "$IssuerId")),
									new Document("$eq",
											Arrays.asList(new Document("$toString", "$_id"), "$$vMauSoHD"))))))))
					.append("as", "DMMSTNCN")));
			pipeline.add(new Document("$unwind",
					new Document("path", "$DMMSTNCN").append("preserveNullAndEmptyArrays", true)));

			Document docTmp = null;
			Iterable<Document> cursor = mongoTemplate.getCollection("ChungTuTNCN").aggregate(pipeline)
					.allowDiskUse(true);
			Iterator<Document> iter = cursor.iterator();
			if (iter.hasNext()) {
				docTmp = iter.next();
			}
			if (null == docTmp) {
				docFind = new Document("_id", objectId).append("IsDelete", new Document("$ne", true));
				pipeline = new ArrayList<Document>();
				pipeline.add(new Document("$match", docFind));
				pipeline.add(new Document("$lookup", new Document("from", "DMMSTNCN")
						.append("let", new Document("vIssuerId", "$IssuerId").append("vMauSoHD", "$MauSoHD"))
						.append("pipeline", Arrays.asList(new Document("$match", new Document("$expr", new Document(
								"$and",
								Arrays.asList(new Document("$eq", Arrays.asList("$$vIssuerId", "$IssuerId")),
										new Document("$eq",
												Arrays.asList(new Document("$toString", "$_id"), "$$vMauSoHD"))))))))
						.append("as", "DMMSTNCN")));
				pipeline.add(new Document("$unwind",
						new Document("path", "$DMMSTNCN").append("preserveNullAndEmptyArrays", true)));

				docTmp = null;
				cursor = mongoTemplate.getCollection("ChungTuTNCN").aggregate(pipeline).allowDiskUse(true);
				iter = cursor.iterator();
				if (iter.hasNext()) {
					docTmp = iter.next();
				}
			}
			if (null == docTmp || docTmp.get("DMMSTNCN") == null) {
				return new FileInfo();
			}
			String mskh = docTmp.get("KyHieu", "");

			String KiHieu = mskh.replaceAll("/", "_");
			Integer shd = docTmp.get("SHDon", 0);
			String namexml = KiHieu + "_" + shd + ".xml";

			String dir = docTmp.get("Dir", "");
			String signStatus = docTmp.get("SignStatus", "");
			fileName = _id + ".xml";
			if ("SIGNED".equals(signStatus)) {
				fileName = _id + "_signed.xml";
			} else {
				fileName = _id + ".xml";

			}
			// namexml

			File file = new File(dir, fileName);
			File tam = new File(dir, namexml);
			copyFileUsingStream(file, tam);

			hItem = new HashMap<>();
			hItem.put("UrlFile", tam.getAbsolutePath());
			arrayInfoInvoice.add(hItem);
		}
		if (arrayInfoInvoice.size() == 0) {
			return fileInfo;
		}
		if (arrayInfoInvoice.size() == 1) {
			hItem = arrayInfoInvoice.get(0);
			file_zip = new File(hItem.get("UrlFile"));
			fileInfo.setContentFile(Files.readAllBytes(file_zip.toPath()));
			fileInfo.setFileName(fileName);
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
			fileInfo.setFileName("ChungTu_TNCN.zip");
			fileInfo.setContentFile(bos.toByteArray());
		}
		return fileInfo;
	}

	@Override
	public MsgRsp saveDataToBase64(JSONRoot jsonRoot) throws Exception {
		Msg msg = jsonRoot.getMsg();
		MsgHeader header = msg.getMsgHeader();
		MsgPage page = msg.getMsgPage();
		Object objData = msg.getObjData();

		JsonNode jsonData = null;
		String dataInput = "";
		if (objData != null) {
			jsonData = Json.serializer().nodeFromObject(msg.getObjData());
			dataInput = commons.getTextJsonNode(jsonData.at("/ArrayData")).trim().replaceAll("\\s+", " ");
		}

		List<String> ids = null;
		ids = null;
		try {
			ids = Json.serializer().fromJson(commons.decodeBase64ToString(dataInput),
					new TypeReference<List<String>>() {
					});
		} catch (Exception e) {
		}
		
		int dem = ids.size();
		MsgRsp rsp = new MsgRsp(header);
		MspResponseStatus responseStatus = null;
	
		
		if(dem > Integer.parseInt(SystemParams.MaxViewPdf) ) {
			responseStatus = new MspResponseStatus(999, "Tối đa là "+SystemParams.MaxViewPdf+" hóa đơn. Vui lòng chọn lại !!!");
			rsp.setResponseStatus(responseStatus);
			return rsp;
		}
		

		ObjectId _id = new ObjectId();
		Document docUpsert1 = new Document("_id", _id).append("Arrays", ids).append("IsDelete", false);
		mongoTemplate.getCollection("EInvoiceTmp").insertOne(docUpsert1);

	

		responseStatus = new MspResponseStatus(0, _id.toString());
		rsp.setResponseStatus(responseStatus);
		return rsp;
	}
	
	
	
	@Override
	public FileInfo printEinvoiceAllDB(JSONRoot jsonRoot) throws Exception {
		FileInfo fileInfo = new FileInfo();
		List<String> ids = null;
		Msg msg = jsonRoot.getMsg();
		MsgHeader header = msg.getMsgHeader();
		MsgPage page = msg.getMsgPage();
		ByteArrayOutputStream baosPDF = null;
		List<String> listFileNamePdfFinal = new ArrayList<>();
		HashMap<String, String> hItem = null;
		ArrayList<HashMap<String, String>> arrayInfoInvoice = new ArrayList<>();
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		File file_zip = null;

		Object objData = msg.getObjData();

		try {

			JsonNode jsonData = null;
			if (objData != null) {
				jsonData = Json.serializer().nodeFromObject(msg.getObjData());
			}
			String _id = "";
			String _token = commons.getTextJsonNode(jsonData.at("/_token")).replaceAll("\\s", "");
			String isConvert = commons.getTextJsonNode(jsonData.at("/IsConvert")).replaceAll("\\s", "");

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
					cursor = mongoTemplate.getCollection("EInvoice").aggregate(pipeline).allowDiskUse(true);
					iter = cursor.iterator();
					if (iter.hasNext()) {
						docTmp = iter.next();
					}
				}

				if (null == docTmp || docTmp.get("DMMauSoKyHieu") == null) {
					return new FileInfo();
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

//				org.w3c.dom.Document doc = commons.fileToDocument(file);
				/* TEST REPORT TO PDF */
				String fileNameJP = docTmp.getEmbedded(Arrays.asList("DMMauSoKyHieu", "Templates", "FileName"), "");
				int numberRowInPage = docTmp.getEmbedded(Arrays.asList("DMMauSoKyHieu", "Templates", "RowsInPage"), 20);
				int numberRowInPageMultiPage = docTmp
						.getEmbedded(Arrays.asList("DMMauSoKyHieu", "Templates", "RowInPageMultiPage"), 26);
				int numberCharsInRow = docTmp.getEmbedded(Arrays.asList("DMMauSoKyHieu", "Templates", "CharsInRow"),
						50);
				File fileJP = new File(SystemParams.DIR_E_INVOICE_TEMPLATE, fileNameJP);
				baosPDF = jpUtils.createFinalInvoiceDB(fileJP, docTmp, CheckView, numberRowInPage, numberRowInPageMultiPage,
						numberCharsInRow, MST, link,
						Paths.get(SystemParams.DIR_E_INVOICE_TEMPLATE, "images", MST, ImgLogo).toString(),
						Paths.get(SystemParams.DIR_E_INVOICE_TEMPLATE, "images", MST, ImgBackground).toString(),
						Paths.get(SystemParams.DIR_E_INVOICE_TEMPLATE, "images", MST, ImgQA).toString(),
						Paths.get(SystemParams.DIR_E_INVOICE_TEMPLATE, "images", MST, ImgVien).toString(),

						"Y".equals(isConvert), Constants.INVOICE_STATUS.DELETED.equals(eInvoiceStatus), isThayThe,
						isDieuChinh);

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
				LocalDateTime time_dem  = LocalDateTime.now();
				String time = time_dem.format(format_time);
				String name_company = removeAccent(header.getUserFullName());
				System.out.println(time +" "+name_company+" vua xuat PDF hang loat hoa don VAT.");
				
				return fileInfo;
			}
		} catch (NullPointerException | MessagingException | UnsupportedEncodingException e) {
			return new FileInfo();
		}

	}
	
	
	/* MAY TINH TIEN */
	

	 public FileInfo print04_mtt(JSONRoot jsonRoot) throws Exception {
	      FileInfo fileInfo = new FileInfo();
	      Msg msg = jsonRoot.getMsg();
	      MsgHeader header = msg.getMsgHeader();
	      MsgPage page = msg.getMsgPage();
	      Object objData = msg.getObjData();
	      JsonNode jsonData = null;
	      if (objData != null) {
	         jsonData = Json.serializer().nodeFromObject(msg.getObjData());
	         String _id = this.commons.getTextJsonNode(jsonData.at("/_id")).replaceAll("\\s", "");
	         String isConvert = this.commons.getTextJsonNode(jsonData.at("/IsConvert")).replaceAll("\\s", "");
	         ObjectId objectId = null;

	         try {
	            objectId = new ObjectId(_id);
	         } catch (Exception var29) {
	         }

	         ObjectId objectIdIssu = null;

	         try {
	            objectIdIssu = new ObjectId(header.getIssuerId());
	         } catch (Exception var28) {
	         }

	         List<Document> pipeline = new ArrayList<Document>();
	         pipeline.add(new Document("$match", (new Document("_id", objectId)).append("IsDelete", new Document("$ne", true))));
	         pipeline.add(new Document("$lookup", (new Document("from", "Issuer")).append("pipeline", Arrays.asList(new Document("$match", (new Document("_id", objectIdIssu)).append("IsDelete", new Document("$ne", true))))).append("as", "Issuer")));
	         pipeline.add(new Document("$unwind", (new Document("path", "$Issuer")).append("preserveNullAndEmptyArrays", true)));
	         Document docTmp = null;
	         Iterable<Document> cursor = this.mongoTemplate.getCollection("EInvoiceHDSSMTT").aggregate(pipeline).allowDiskUse(true);
	         Iterator<Document> iter = cursor.iterator();
	         if (iter.hasNext()) {
	            docTmp = (Document)iter.next();
	         }

	         String ImgLogo = "";
	         String ImgBackground = "";
	         String fileName = _id +"_signed.xml";
	         String dir = (String)docTmp.get("Dir", "");
	         File file = new File(dir, fileName);
	         if (file.exists() && file.isFile()) {
	            org.w3c.dom.Document doc = this.commons.fileToDocument(file);
	            String fileNameJP = "04SS.jrxml";
	            int numberRowInPage = 5;
	            int numberRowInPageMultiPage = 15;
	            int numberCharsInRow = 50;
	            File fileJP = new File(SystemParams.DIR_E_INVOICE_TEMPLATE, fileNameJP);
	            ByteArrayOutputStream baosPDF = null;
	            baosPDF = this.jpUtils.print04(fileJP, doc, docTmp, numberRowInPage, numberRowInPageMultiPage, numberCharsInRow, Paths.get(SystemParams.DIR_E_INVOICE_TEMPLATE, "images", (String)docTmp.getEmbedded(Arrays.asList("Issuer", "TaxCode"), ""), ImgLogo).toString(), Paths.get(SystemParams.DIR_E_INVOICE_TEMPLATE, "images", (String)docTmp.getEmbedded(Arrays.asList("Issuer", "TaxCode"), ""), ImgBackground).toString(), "Y".equals(isConvert));
	            fileInfo.setFileName("print04.pdf");
	            fileInfo.setContentFile(baosPDF.toByteArray());
	            return fileInfo;
	         } else {
	            return new FileInfo();
	         }
	      } else {
	         return new FileInfo();
	      }
	   }


		@Override
		public FileInfo printEInvoiceMTT(JSONRoot jsonRoot) throws Exception {
			FileInfo fileInfo = new FileInfo();

			Msg msg = jsonRoot.getMsg();
			MsgHeader header = msg.getMsgHeader();
			MsgPage page = msg.getMsgPage();

			Object objData = msg.getObjData();

			JsonNode jsonData = null;
			if (objData != null) {
				jsonData = Json.serializer().nodeFromObject(msg.getObjData());
			} else {
				return new FileInfo();
			}

			String _id = commons.getTextJsonNode(jsonData.at("/_id")).replaceAll("\\s", "");
			String isConvert = commons.getTextJsonNode(jsonData.at("/IsConvert")).replaceAll("\\s", "");		

			ObjectId objectId = null;
			try {
				objectId = new ObjectId(_id);
			} catch (Exception e) {
			}

			Document docFind = new Document("IssuerId", header.getIssuerId()).append("_id", objectId).append("IsDelete",
					new Document("$ne", true));
			List<Document> pipeline = new ArrayList<Document>();
			pipeline.add(new Document("$match", docFind));
			pipeline.add(new Document("$lookup", new Document("from", "DMMauSoKyHieu")
					.append("let",
							new Document("vIssuerId", "$IssuerId").append("vMauSoHD", "$EInvoiceDetail.TTChung.MauSoHD"))
					.append("pipeline",
							Arrays.asList(new Document("$match", new Document("$expr", new Document("$and",
									Arrays.asList(new Document("$eq", Arrays.asList("$$vIssuerId", "$IssuerId")),
											new Document("$eq",
													Arrays.asList(new Document("$toString", "$_id"), "$$vMauSoHD"))))))))
					.append("as", "DMMauSoKyHieu")));
			pipeline.add(new Document("$unwind",
					new Document("path", "$DMMauSoKyHieu").append("preserveNullAndEmptyArrays", true)));
			pipeline.add(new Document("$lookup", new Document("from", "UserConFig")
					.append("pipeline", Arrays.asList(new Document("$match", new Document("IssuerId", header.getIssuerId()))))
					.append("as", "UserConFig")));
			pipeline.add(new Document("$unwind",
					new Document("path", "$UserConFig").append("preserveNullAndEmptyArrays", true)));
			
			pipeline.add(new Document("$lookup",
					new Document("from", "PramLink")
							.append("pipeline",
									Arrays.asList(
											new Document("$match",
													new Document("$expr", new Document("IsDelete", false))),
											new Document("$project", new Document("_id", 1).append("LinkPortal", 1))))
							.append("as", "PramLink")));
			pipeline.add(new Document("$unwind",
					new Document("path", "$PramLink").append("preserveNullAndEmptyArrays", true)));
			Document docTmp = null;
			Iterable<Document> cursor = mongoTemplate.getCollection("EInvoiceMTT").aggregate(pipeline).allowDiskUse(true);
			Iterator<Document> iter = cursor.iterator();
			if (iter.hasNext()) {
				docTmp = iter.next();
			}
			
			if (null == docTmp) {
				 docFind = new Document("_id", objectId).append("IsDelete",
						new Document("$ne", true));
				pipeline = new ArrayList<Document>();
				pipeline.add(new Document("$match", docFind));
				pipeline.add(
						new Document("$lookup", 
							new Document("from", "UserConFig")
							.append("pipeline", 
								Arrays.asList(
									new Document("$match", 
										new Document("IssuerId", header.getIssuerId())
									)
								)
							)
							.append("as", "UserConFig")
						)
					);
				pipeline.add(new Document("$unwind",
						new Document("path", "$UserConFig").append("preserveNullAndEmptyArrays", true)));
				
				pipeline.add(new Document("$lookup",
						new Document("from", "PramLink")
								.append("pipeline",
										Arrays.asList(
												new Document("$match",
														new Document("$expr", new Document("IsDelete", false))),
												new Document("$project", new Document("_id", 1).append("LinkPortal", 1))))
								.append("as", "PramLink")));
				pipeline.add(new Document("$unwind",
						new Document("path", "$PramLink").append("preserveNullAndEmptyArrays", true)));
			
				
				pipeline.add(new Document("$lookup", new Document("from", "DMMauSoKyHieu")
						.append("let",
								new Document("vIssuerId", "$IssuerId").append("vMauSoHD", "$EInvoiceDetail.TTChung.MauSoHD"))
						.append("pipeline",
								Arrays.asList(new Document("$match", new Document("$expr", new Document("$and",
										Arrays.asList(new Document("$eq", Arrays.asList("$$vIssuerId", "$IssuerId")),
												new Document("$eq",
														Arrays.asList(new Document("$toString", "$_id"), "$$vMauSoHD"))))))))
						.append("as", "DMMauSoKyHieu")));
				pipeline.add(new Document("$unwind",
						new Document("path", "$DMMauSoKyHieu").append("preserveNullAndEmptyArrays", true)));		
			
				 docTmp = null;
			 cursor = mongoTemplate.getCollection("EInvoiceMTT").aggregate(pipeline).allowDiskUse(true);
			 iter = cursor.iterator();
				if (iter.hasNext()) {
					docTmp = iter.next();
				}
			}
			
			if (null == docTmp || docTmp.get("DMMauSoKyHieu") == null) {
				return new FileInfo();
			}

			boolean isDieuChinh = "2".equals(docTmp.getEmbedded(Arrays.asList("HDSS", "TCTBao"), ""));
			boolean isThayThe = "3".equals(docTmp.getEmbedded(Arrays.asList("HDSS", "TCTBao"), ""));
            String check_status = docTmp.get("EInvoiceStatus", "");
            if (check_status.equals("REPLACED")) {
                isThayThe = true;
            }
            if (check_status.equals("ADJUSTED")) {
                isDieuChinh = true;
            }
			String link = docTmp.getEmbedded(Arrays.asList("PramLink", "LinkPortal"), "");
			String MST = docTmp.getEmbedded(Arrays.asList("EInvoiceDetail", "NDHDon", "NBan", "MST"), "");
			String ImgLogo = docTmp.getEmbedded(Arrays.asList("DMMauSoKyHieu", "Templates", "ImgLogo"), "");
			String ImgBackground = docTmp.getEmbedded(Arrays.asList("DMMauSoKyHieu", "Templates", "ImgBackground"), "");
			String ImgQA = docTmp.getEmbedded(Arrays.asList("DMMauSoKyHieu", "Templates", "ImgQA"), "");
			String ImgVien = docTmp.getEmbedded(Arrays.asList("DMMauSoKyHieu", "Templates", "ImgVien"), "");
			String Checknamecd = docTmp.getEmbedded(Arrays.asList("UserConFig", "NameCD"), "");
			String CheckView = docTmp.getEmbedded(Arrays.asList("UserConFig", "viewshd"), "")+","+Checknamecd;
			String dir = docTmp.get("Dir", "");
			String signStatusCode = docTmp.get("SignStatusCode", "");
			String eInvoiceStatus = docTmp.get("EInvoiceStatus", "");
			String MCCQT = docTmp.get("MCCQT", "");
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
			int numberCharsInRow = docTmp.getEmbedded(Arrays.asList("DMMauSoKyHieu", "Templates", "CharsInRow"), 50);

			File fileJP = new File(SystemParams.DIR_E_INVOICE_TEMPLATE, fileNameJP);

			ByteArrayOutputStream baosPDF = null;

			baosPDF = jpUtils.createFinalInvoiceMTT(fileJP, doc,CheckView, link, eInvoiceStatus, signStatusCode, numberRowInPage, numberRowInPageMultiPage, numberCharsInRow,
					MST, Paths.get(SystemParams.DIR_E_INVOICE_TEMPLATE, "images", MST, ImgLogo).toString(),
					Paths.get(SystemParams.DIR_E_INVOICE_TEMPLATE, "images", MST, ImgBackground).toString(),
					Paths.get(SystemParams.DIR_E_INVOICE_TEMPLATE, "images", MST, ImgQA ).toString(),
					Paths.get(SystemParams.DIR_E_INVOICE_TEMPLATE, "images", MST, ImgVien ).toString(),

					"Y".equals(isConvert), Constants.INVOICE_STATUS.DELETED.equals(eInvoiceStatus), isThayThe, isDieuChinh);
		
			fileInfo.setFileName("EInvoiceMTT.pdf");
			fileInfo.setContentFile(baosPDF.toByteArray());

			return fileInfo;
		}

		public MsgRsp listEInvoiceMTTSigned(JSONRoot jsonRoot) throws Exception {
			Msg msg = jsonRoot.getMsg();
			MsgHeader header = msg.getMsgHeader();
			MsgPage page = msg.getMsgPage();
			Object objData = msg.getObjData();

			String mauSoHdon = "";
			String soHoaDon = "";
			String fromDate = "";
			String toDate = "";
			String nbanMst = "";
			String nbanTen = "";

			JsonNode jsonData = null;
			if (objData != null) {
				jsonData = Json.serializer().nodeFromObject(objData);

				mauSoHdon = commons.getTextJsonNode(jsonData.at("/MauSoHdon")).replaceAll("\\s", "");
				soHoaDon = commons.getTextJsonNode(jsonData.at("/SoHoaDon")).replaceAll("\\s", "");
				fromDate = commons.getTextJsonNode(jsonData.at("/FromDate")).replaceAll("\\s", "");
				toDate = commons.getTextJsonNode(jsonData.at("/ToDate")).replaceAll("\\s", "");
				nbanMst = commons.getTextJsonNode(jsonData.at("/NbanMst")).trim().replaceAll("\\s+", " ");
				nbanTen = commons.getTextJsonNode(jsonData.at("/NbanTen")).trim().replaceAll("\\s+", " ");
			}

			MsgRsp rsp = new MsgRsp(header);
			MspResponseStatus responseStatus = null;

			ObjectId objectId = null;
			Document docTmp = null;
			Iterable<Document> cursor = null;
			Iterator<Document> iter = null;

			List<Document> pipeline = new ArrayList<Document>();

			LocalDate dateFrom = null;
			LocalDate dateTo = null;
			Document docMatchDate = null;

			dateFrom = "".equals(fromDate) || !commons.checkLocalDate(fromDate, Constants.FORMAT_DATE.FORMAT_DATE_WEB)
					? null
					: commons.convertStringToLocalDate(fromDate, Constants.FORMAT_DATE.FORMAT_DATE_WEB);
			dateTo = "".equals(toDate) || !commons.checkLocalDate(toDate, Constants.FORMAT_DATE.FORMAT_DATE_WEB) ? null
					: commons.convertStringToLocalDate(toDate, Constants.FORMAT_DATE.FORMAT_DATE_WEB);
			if (null != dateTo)
				dateTo = dateTo.plus(1, ChronoUnit.DAYS);
			if (null != dateFrom || null != dateTo) {
				docMatchDate = new Document();
				if (null != dateFrom)
					docMatchDate.append("$gte", dateFrom);
				if (null != dateTo)
					docMatchDate.append("$lt", dateTo);
			}

			Document docMatch = new Document("IssuerId", header.getIssuerId()).append("IsDelete", new Document("$ne", true))
					.append("SignStatusCode", "SIGNED")
					.append("EInvoiceStatus",
							new Document("$in",
									Arrays.asList(Constants.INVOICE_STATUS.COMPLETE, Constants.INVOICE_STATUS.ADJUSTED)))

					.append("MCCQT", new Document("$exists", true).append("$ne", null));
			if (!"".equals(mauSoHdon))
				docMatch.append("EInvoiceDetail.TTChung.MauSoHD", commons.regexEscapeForMongoQuery(mauSoHdon));
			if (!"".equals(soHoaDon))
				docMatch.append("EInvoiceDetail.TTChung.SHDon", commons.stringToInteger(soHoaDon));
			if (null != docMatchDate)
				docMatch.append("EInvoiceDetail.TTChung.NLap", docMatchDate);
			if (!"".equals(nbanMst))
				docMatch.append("EInvoiceDetail.NDHDon.NMua.MST",
						new Document("$regex", commons.regexEscapeForMongoQuery(nbanMst)).append("$options", "i"));
			if (!"".equals(nbanTen)) {
				docMatch.append("$or",
						Arrays.asList(
								new Document("EInvoiceDetail.NDHDon.NMua.Ten",
										new Document("$regex", commons.regexEscapeForMongoQuery(nbanTen)).append("$options",
												"i")),
								new Document("EInvoiceDetail.NDHDon.NMua.HVTNMHang",
										new Document("$regex", commons.regexEscapeForMongoQuery(nbanTen)).append("$options",
												"i"))));
			}
			pipeline = new ArrayList<Document>();
			pipeline.add(new Document("$match", docMatch));

			pipeline.add(new Document("$sort",
					new Document("EInvoiceDetail.TTChung.MauSoHD", -1).append("SHDon", -1).append("_id", -1)));
			pipeline.addAll(createFacetForSearchNotSort(page));

			cursor = mongoTemplate.getCollection("EInvoiceMTT").aggregate(pipeline).allowDiskUse(true);

			iter = cursor.iterator();
			
			if (iter.hasNext()) {
				docTmp = iter.next();
			}
			
			if(docTmp == null) {
				responseStatus = new MspResponseStatus(9999, "Không tìm thấy dữ liệu.");
				rsp.setResponseStatus(responseStatus);
				return rsp;
			}
			
			rsp = new MsgRsp(header);
			responseStatus = null;
			ArrayList<HashMap<String, Object>> rowsReturn = new ArrayList<HashMap<String, Object>>();
			HashMap<String, Object> hItem = null;
			if (null != docTmp) {
				page.setTotalRows(docTmp.getInteger("total", 0));
				rsp.setMsgPage(page);
				List<Document> rows = null;
				if (docTmp.get("data") != null && docTmp.get("data") instanceof List) {
					rows = docTmp.getList("data", Document.class);
				}
				if (null != rows) {
					for (Document doc : rows) {
						objectId = (ObjectId) doc.get("_id");

						hItem = new HashMap<String, Object>();
						hItem.put("_id", objectId.toString());
						hItem.put("EInvoiceStatus", doc.get("EInvoiceStatus"));
						hItem.put("SignStatusCode", doc.get("SignStatusCode"));
						hItem.put("MCCQT", doc.get("MCCQT"));

						hItem.put("EInvoiceDetail", doc.get("EInvoiceDetail"));
						hItem.put("InfoCreated", doc.get("InfoCreated"));
						hItem.put("LDo", doc.get("LDo"));

						rowsReturn.add(hItem);
					}
				}
			}

			responseStatus = new MspResponseStatus(0, "SUCCESS");
			rsp.setResponseStatus(responseStatus);

			HashMap<String, Object> mapDataR = new HashMap<String, Object>();
			mapDataR.put("rows", rowsReturn);
			rsp.setObjData(mapDataR);
			return rsp;
		}
		
		private boolean compareTaxCodeInfo(Map<String, Object> doc1, Document doc2) {
			if (!Objects.equals(doc1.get("Name"), doc2.get("CompanyName"))) return false;
			if (!Objects.equals(doc1.get("Address"), doc2.get("Address"))) return false;
			if (!Objects.equals(doc1.get("Phone"), doc2.get("Phone"))) return false;
			if (!Objects.equals(doc1.get("Email"), doc2.get("Email"))) return false;
			if (!Objects.equals(doc1.get("TaxAuthority"), doc2.get("TaxAuthority"))) return false;
			if (!Objects.equals(doc1.get("TinhTrang"), doc2.get("Status"))) return false;
			System.out.println();
			return true;
		}
		
		private void saveTaxCodeInfo(MsgHeader header, Map<String, Object> bodyMap, String taxCode, int statusCode) {
			Document insertDoc = new Document();
			try (MongoClient mongoClient = cfg.mongoClient()) {
				MongoDatabase mongoDatabase = mongoClient.getDatabase(cfg.dbName);
				Map<String, Object> data = null;
				if (bodyMap.get("data") != null && bodyMap.get("data") instanceof Map) {
					data = (Map<String, Object>) bodyMap.get("data");
				}
				
				insertDoc.append("IssuerId", header.getUserId())
						.append("TaxCode", taxCode)
						.append("CompanyName", data !=null ? data.get("Name") : null)
						.append("Address", data !=null ? data.get("Address"): null)
						.append("Phone", data !=null && data.get("Phone") != null ? data.get("Phone") : null)
						.append("Email", data !=null && data.get("Email") != null ? data.get("Email") : null)
						.append("TaxAuthority", data !=null ? data.get("TaxAuthority") : null)
						.append("Status", data !=null ? data.get("TinhTrang") : null)
						.append("IsDelete", false)
						.append("InfoCreated",
								new Document("CreateDate", LocalDateTime.now())
										.append("CreateUserID", header.getUserId())
										.append("CreateUserName", header.getUserName())
										.append("CreateUserFullName", header.getUserFullName()));
				
				if (Boolean.TRUE.equals(bodyMap.get("success"))) {
					Document filter = new Document("TaxCode", taxCode).append("IsDelete", false);

					MongoCollection<Document> collection = mongoDatabase.getCollection("TaxCodeInfomations");
					Document doc = collection.find(filter).first();
					if (doc == null) {
						collection.insertOne(insertDoc);
					} else if (!compareTaxCodeInfo(data, doc)) {
						Document updateFields = new Document("IsDelete", true).append("InfoUpdated",
								new Document("UpdateDate", LocalDateTime.now())
										.append("CreateUserID", header.getUserId())
										.append("CreateUserName", header.getUserName())
										.append("CreateUserFullName", header.getUserFullName()));

						Document update = new Document("$set", updateFields);
						collection.updateOne(filter, update);
						collection.insertOne(insertDoc);
					}
				}
				
				insertDoc
				.append("TaxCode", taxCode)
				.append("IssuerTaxCode", header.getUserName())
				.append("IssuerName", header.getUserFullName())
				.append("SearchDate", LocalDateTime.now())
				.append("response", 
						new Document("code", statusCode)
							.append("success", bodyMap.get("success"))
							.append("error",bodyMap.get("error") != null ? bodyMap.get("error").toString() : null)
							.append("message", bodyMap.get("message") != null ? bodyMap.get("message").toString():null))
				.append("InfoCreated",
								new Document("CreateDate", LocalDateTime.now())
									.append("CreateUserID", header.getUserId())
									.append("CreateUserName", header.getUserName())
									.append("CreateUserFullName", header.getUserFullName()));

				MongoCollection<Document> collection = mongoDatabase.getCollection("TaxCodeSearchStatistics");
				collection.insertOne(insertDoc);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		
		@Override
		public MsgRsp scratchingTaxCode(JSONRoot jsonRoot) throws Exception {
			Msg msg = jsonRoot.getMsg();
			MsgHeader header = msg.getMsgHeader();
			Object objData = msg.getObjData();

			MsgRsp rsp = new MsgRsp(header);
			MspResponseStatus responseStatus = null;

			JsonNode jsonData = null;
			String taxCode = "";
			if (objData != null) {
				jsonData = Json.serializer().nodeFromObject(msg.getObjData());
				taxCode = commons.getTextJsonNode(jsonData.at("/MST")).trim().replaceAll("\\s+", " ");
			}

			HashMap<String, String> hR = new HashMap<String, String>();
			try {
				String url = "https://dauvao-api.vinvoice.vn/api/public/doanhnghiep/tracuu/{code}";
				HttpHeaders headers = new HttpHeaders();
				headers.setAccept(Arrays.asList(new MediaType[] { MediaType.APPLICATION_JSON }));
				headers.setContentType(MediaType.APPLICATION_JSON);
				HttpEntity<Void> requestBody = new HttpEntity<>(headers);
				ResponseEntity<Map> result = restTemplate.exchange(url, HttpMethod.GET, requestBody, Map.class,
						taxCode);

				Map<String, Object> bodyMap = result.getBody();
				saveTaxCodeInfo(header, bodyMap, taxCode, result.getStatusCodeValue());
				
				if (result.getStatusCode() == HttpStatus.OK) {
					if (bodyMap.get("data") != null && bodyMap.get("data") instanceof Map) {
						Map<String, Object> data = (Map<String, Object>) bodyMap.get("data");
						hR.put("ten_cong_ty", data.get("Name").toString());
						hR.put("dia_chi", data.get("Address").toString());
					}
					responseStatus = new MspResponseStatus(0, "SUCCESS");
					rsp.setResponseStatus(responseStatus);
					rsp.setObjData(hR);
					return rsp;
				}

				responseStatus = new MspResponseStatus(999, bodyMap.get("message").toString());
				rsp.setResponseStatus(responseStatus);
				rsp.setObjData(hR);
				return rsp;

			} catch (HttpClientErrorException e) {
			    String responseBody = e.getResponseBodyAsString();
			    int statusCode = e.getRawStatusCode();
			    ObjectMapper mapper = new ObjectMapper();
			    Map<String, Object> errorMap = mapper.readValue(responseBody, Map.class);
				saveTaxCodeInfo(header, errorMap, taxCode, statusCode);

			    responseStatus = new MspResponseStatus(statusCode, errorMap.get("message").toString());
			    rsp.setResponseStatus(responseStatus);
			    rsp.setObjData(hR);
			    return rsp;

			}  catch (Exception e) {
				e.printStackTrace();
				System.out.println("ERROR WHEN GET MST " + e);
				responseStatus = new MspResponseStatus(999, "Scratching information from tax code fail.");
				rsp.setResponseStatus(responseStatus);
				rsp.setObjData(hR);
				return rsp;
			} 
		}

		@Override
		public MsgRsp scratchingTaxCodeV2(JSONRoot jsonRoot) throws Exception {
			Msg msg = jsonRoot.getMsg();
			MsgHeader header = msg.getMsgHeader();
			Object objData = msg.getObjData();

			MsgRsp rsp = new MsgRsp(header);
			MspResponseStatus responseStatus = null;

			JsonNode jsonData = null;
			String taxCode = "";
			if (objData != null) {
				jsonData = Json.serializer().nodeFromObject(msg.getObjData());
				taxCode = commons.getTextJsonNode(jsonData.at("/MST")).trim().replaceAll("\\s+", " ");
			}

			HashMap<String, String> hR = new HashMap<String, String>();
			String url = "https://hsctvn.com/search?key="+taxCode+"&opt=0&p=0&d=0";
			try {
				String address = "";
				String compayName = "";
				org.jsoup.nodes.Document doc = Jsoup.connect(url)
						.userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/121.0.0.0 Safari/537.36")
						.referrer("https://www.google.com/")
						.header("Accept-Language", "en-US,en;q=0.9")
						.header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8")
						.header("Connection", "keep-alive")
						.timeout(10 * 1000).get();

	            Elements elements = doc.select("ul.hsdn li h3 a");
	            if (elements.size() != 0) {
	            	compayName= elements.get(0).text();
	            }
	            elements = doc.select("ul.hsdn li div");
	            if (elements.size() != 0) {
	                address = elements.get(0).text().replace("Địa chỉ:", "").split("Mã số thuế:")[0].trim();
	            }
	            
	            hR.put("ten_cong_ty", compayName);
	            hR.put("dia_chi", address);
	            
	            
			} catch (Exception e) {
				e.printStackTrace();
				System.out.println("ERROR WHEN GET MST "+e);
				responseStatus = new MspResponseStatus(999, "Scratching information from tax code fail.");
				rsp.setResponseStatus(responseStatus);
				rsp.setObjData(hR);
				return rsp;
			}

			responseStatus = new MspResponseStatus(0, "SUCCESS");
			rsp.setResponseStatus(responseStatus);
			rsp.setObjData(hR);
			return rsp;
		}

		@Override
		public MsgRsp listEInvoicesSigned(JSONRoot jsonRoot) throws Exception {
			Msg msg = jsonRoot.getMsg();
			MsgHeader header = msg.getMsgHeader();
			MsgPage page = msg.getMsgPage();
			Object objData = msg.getObjData();

			String mauSoHdon = "";
			String soHoaDon = "";
			String fromDate = "";
			String toDate = "";
			String nbanMst = "";
			String nbanTen = "";

			JsonNode jsonData = null;
			if (objData != null) {
				jsonData = Json.serializer().nodeFromObject(objData);

				mauSoHdon = commons.getTextJsonNode(jsonData.at("/MauSoHdon")).replaceAll("\\s", "");
				soHoaDon = commons.getTextJsonNode(jsonData.at("/SoHoaDon")).replaceAll("\\s", "");
				fromDate = commons.getTextJsonNode(jsonData.at("/FromDate")).replaceAll("\\s", "");
				toDate = commons.getTextJsonNode(jsonData.at("/ToDate")).replaceAll("\\s", "");
				nbanMst = commons.getTextJsonNode(jsonData.at("/NbanMst")).trim().replaceAll("\\s+", " ");
				nbanTen = commons.getTextJsonNode(jsonData.at("/NbanTen")).trim().replaceAll("\\s+", " ");
			}

			MsgRsp rsp = new MsgRsp(header);
			MspResponseStatus responseStatus = null;

			ObjectId objectId = null;
			Iterable<Document> cursor = null;
			Iterable<Document> cursor1 = null;
			Iterable<Document> cursor2 = null;
			Iterable<Document> cursor3 = null;
			Iterable<Document> cursor4 = null;
			Iterator<Document> iter = null;
			Iterator<Document> iter1 = null;	
			Iterator<Document> iter2 = null;
			Iterator<Document> iter3 = null;
			Iterator<Document> iter4 = null;
			List<Document> docsTmp = new ArrayList<Document>();
			List<Document> pipeline = new ArrayList<Document>();

			LocalDate dateFrom = null;
			LocalDate dateTo = null;
			Document docMatchDate = null;

			dateFrom = "".equals(fromDate) || !commons.checkLocalDate(fromDate, Constants.FORMAT_DATE.FORMAT_DATE_WEB)
					? null
					: commons.convertStringToLocalDate(fromDate, Constants.FORMAT_DATE.FORMAT_DATE_WEB);
			dateTo = "".equals(toDate) || !commons.checkLocalDate(toDate, Constants.FORMAT_DATE.FORMAT_DATE_WEB) ? null
					: commons.convertStringToLocalDate(toDate, Constants.FORMAT_DATE.FORMAT_DATE_WEB);
			if (null != dateTo)
				dateTo = dateTo.plus(1, ChronoUnit.DAYS);
			if (null != dateFrom || null != dateTo) {
				docMatchDate = new Document();
				if (null != dateFrom)
					docMatchDate.append("$gte", dateFrom);
				if (null != dateTo)
					docMatchDate.append("$lt", dateTo);
			}

			Document docMatch = new Document("IssuerId", header.getIssuerId()).append("IsDelete", new Document("$ne", true))
					.append("SignStatusCode", "SIGNED")
					.append("EInvoiceStatus",
							new Document("$in",
									Arrays.asList(Constants.INVOICE_STATUS.COMPLETE, Constants.INVOICE_STATUS.ADJUSTED, Constants.INVOICE_STATUS.REPLACED)))

					.append("MCCQT", new Document("$exists", true).append("$ne", null));
			if (!"".equals(mauSoHdon))
				docMatch.append("EInvoiceDetail.TTChung.MauSoHD", commons.regexEscapeForMongoQuery(mauSoHdon));
			if (!"".equals(soHoaDon))
				docMatch.append("EInvoiceDetail.TTChung.SHDon", commons.stringToInteger(soHoaDon));
			if (null != docMatchDate)
				docMatch.append("EInvoiceDetail.TTChung.NLap", docMatchDate);
			if (!"".equals(nbanMst))
				docMatch.append("EInvoiceDetail.NDHDon.NMua.MST",
						new Document("$regex", commons.regexEscapeForMongoQuery(nbanMst)).append("$options", "i"));
			if (!"".equals(nbanTen)) {
				docMatch.append("$or",
						Arrays.asList(
								new Document("EInvoiceDetail.NDHDon.NMua.Ten",
										new Document("$regex", commons.regexEscapeForMongoQuery(nbanTen)).append("$options",
												"i")),
								new Document("EInvoiceDetail.NDHDon.NMua.HVTNMHang",
										new Document("$regex", commons.regexEscapeForMongoQuery(nbanTen)).append("$options",
												"i"))));
			}
			pipeline = new ArrayList<Document>();
			pipeline.add(new Document("$match", docMatch));

			pipeline.add(new Document("$sort",
					new Document("EInvoiceDetail.TTChung.MauSoHD", -1).append("SHDon", -1).append("_id", -1)));
//			pipeline.addAll(createFacetForSearchNotSort(page));

			cursor = mongoTemplate.getCollection("EInvoice").aggregate(pipeline).allowDiskUse(true);
			cursor1 = mongoTemplate.getCollection("EInvoicePXK").aggregate(pipeline).allowDiskUse(true);
			cursor2 = mongoTemplate.getCollection("EInvoiceBH").aggregate(pipeline).allowDiskUse(true);
			cursor3 = mongoTemplate.getCollection("EInvoicePXKDL").aggregate(pipeline).allowDiskUse(true);
			cursor4 = mongoTemplate.getCollection("EInvoiceMTT").aggregate(pipeline).allowDiskUse(true);
			iter = cursor.iterator();
			iter1 = cursor1.iterator();
			iter2 = cursor2.iterator();
			iter3 = cursor3.iterator();
			iter4 = cursor4.iterator();
			while (iter.hasNext()) {
				docsTmp.add(iter.next());
			}
			while (iter1.hasNext()) {
				docsTmp.add(iter1.next());
			}
			while (iter2.hasNext()) {
				docsTmp.add(iter2.next());
			}
			while (iter3.hasNext()) {
				docsTmp.add(iter3.next());
			}
			while (iter4.hasNext()) {
				docsTmp.add(iter4.next());
			}
			rsp = new MsgRsp(header);
			responseStatus = null;
			ArrayList<HashMap<String, Object>> rowsReturn = new ArrayList<HashMap<String, Object>>();
			HashMap<String, Object> hItem = null;
			int totalRows = docsTmp.size();
			if (docsTmp.size() > 0) {
					for (Document doc : docsTmp) {
						objectId = (ObjectId) doc.get("_id");

						hItem = new HashMap<String, Object>();
						hItem.put("_id", objectId.toString());
						hItem.put("EInvoiceStatus", doc.get("EInvoiceStatus"));
						hItem.put("SignStatusCode", doc.get("SignStatusCode"));
						hItem.put("MCCQT", doc.get("MCCQT"));
						hItem.put("EInvoiceDetail", doc.get("EInvoiceDetail"));
						rowsReturn.add(hItem);
					}
			}
	
			page.setTotalRows(totalRows);
			rsp.setMsgPage(page);
			responseStatus = new MspResponseStatus(0, "SUCCESS");
			rsp.setResponseStatus(responseStatus);
			
			int pageNo = page.getPageNo();     
			int pageSize = page.getSize();

			// Calculate start and end indexes
			int fromIndex = (pageNo - 1) * pageSize;
			int toIndex = Math.min(fromIndex + pageSize, totalRows);
			
			List<HashMap<String, Object>> results = new ArrayList<>();
			if (fromIndex < totalRows) {
				results = rowsReturn.subList(fromIndex, toIndex);
			}
			
			HashMap<String, Object> mapDataR = new HashMap<String, Object>();
			mapDataR.put("rows", results);
			rsp.setObjData(mapDataR);
			return rsp;
		}

		@Override
		public FileInfo printbb(JSONRoot jsonRoot) throws Exception {
			FileInfo fileInfo = new FileInfo();
			Msg msg = jsonRoot.getMsg();
			MsgHeader header = msg.getMsgHeader();
			Object objData = msg.getObjData();
			
			if (objData == null) {
				return new FileInfo();
			}
			
			JsonNode jsonData = Json.serializer().nodeFromObject(msg.getObjData());
			String _id = this.commons.getTextJsonNode(jsonData.at("/_id")).replaceAll("\\s", "");
			
			ObjectId objectId = null;
			try {
				objectId = new ObjectId(_id);
			} catch (Exception e) {
			}

			Document docFind =  new Document("_id", objectId).append("IsDelete", false).append("IssuerId", header.getIssuerId());
			Document docTmp = null;
			Iterable<Document> cursor = this.mongoTemplate.getCollection("EInvoiceBBDCTT").find(docFind).allowDiskUse(true);
			Iterator<Document> iter = cursor.iterator();
			if (iter.hasNext()) {
				docTmp = (Document) iter.next();
			}
			if (docTmp == null) {
				return fileInfo;
			}
			
			String signStatusCode = docTmp.get("SignStatusCode", "");
			String status = docTmp.get("Status", "");
			String mtdiep = docTmp.get("MTDiep", "");
			String loai = docTmp.get("Loai", "");
			String fileName = _id + ".xml";
			String dir = docTmp.get("Dir", "");
			boolean isMultiInvoice = docTmp.getBoolean("IsMultiInvoice", false);
			if ("SIGNED".equals(signStatusCode)) {
				fileName = _id + "_signed.xml";
				if ("COMPLETE".equals(status)) {
					fileName = _id + "_" + mtdiep + "_signed.xml";
				}
			}
			
			String fileNameJP = isMultiInvoice ? "BIEN-BAN-DIEU-CHINH-THAY-THE-MULTI.jrxml" : "BIEN-BAN-DIEU-CHINH-THAY-THE.jrxml";
			List<Document> invoicesReturn = new ArrayList<Document>();
			if (isMultiInvoice && 
					Constants.TAXCODE_USING_RETURN_REPORT.contains(docTmp.getEmbedded(Arrays.asList("TTNBan", "MSThue"), ""))) {
				fileNameJP ="BIEN-BAN-DIEU-CHINH-THAY-THE-MULTI-101.jrxml";

				List<ObjectId> ids = new ArrayList<ObjectId>();
				List<Document> dshDon = docTmp.getList("DSHDon", Document.class);
				for (Document hdPair: dshDon) {
						String id = hdPair.getEmbedded(Arrays.asList("HDDCTThe","_id"),"");
						ids.add(new ObjectId(id));		
				}
				
				docFind = new Document("_id", new Document("$in", ids)).append("IsDelete", false).append("IssuerId", header.getIssuerId());
				Document projection = new Document("EInvoiceDetail.DSHHDVu", 1).append("EInvoiceDetail.TTChung", 1);
				docTmp = null;
				cursor = this.mongoTemplate.getCollection("EInvoice").find(docFind).projection(projection).allowDiskUse(true);
				iter = cursor.iterator();
				while (iter.hasNext()) {
					invoicesReturn.add(iter.next());
				}
				if (invoicesReturn.size() == 0) {
					cursor = this.mongoTemplate.getCollection("EInvoice").find(docFind).allowDiskUse(true);
					iter = cursor.iterator();
					while (iter.hasNext()) {
						invoicesReturn.add(iter.next());
					}
				}
				if (invoicesReturn.size() == 0) {
					cursor = this.mongoTemplate.getCollection("EInvoiceBH").find(docFind).allowDiskUse(true);
					iter = cursor.iterator();
					while (iter.hasNext()) {
						invoicesReturn.add(iter.next());
					}
				}
				if (invoicesReturn.size() == 0) {
					cursor = this.mongoTemplate.getCollection("EInvoiceMTT").find(docFind).allowDiskUse(true);
					iter = cursor.iterator();
					while (iter.hasNext()) {
						invoicesReturn.add(iter.next());
					}
				}
			}
			File file = new File(dir, fileName);
			if (file.exists() && file.isFile()) {
				org.w3c.dom.Document doc = this.commons.fileToDocument(file);
				File fileJP = new File(SystemParams.DIR_E_INVOICE_TEMPLATE, fileNameJP);
				ByteArrayOutputStream baosPDF = isMultiInvoice
						? this.jpUtils.printbb_multi(fileJP, doc, "1".equals(loai), invoicesReturn)
						: this.jpUtils.printbb(fileJP, doc, "1".equals(loai));
				fileInfo.setFileName("printbb.pdf");
				fileInfo.setContentFile(baosPDF.toByteArray());
				return fileInfo;
			} else {
				return new FileInfo();
			}
		}

		@Override
		public MsgRsp listCTTNCNSigned(JSONRoot jsonRoot) throws Exception {
			Msg msg = jsonRoot.getMsg();
			MsgHeader header = msg.getMsgHeader();
			MsgPage page = msg.getMsgPage();
			Object objData = msg.getObjData();

			String soChungTu = "";
			String fromDate = "";
			String toDate = "";
			String nvMst = "";

			JsonNode jsonData = null;
			if (objData != null) {
				jsonData = Json.serializer().nodeFromObject(objData);
				soChungTu = commons.getTextJsonNode(jsonData.at("/SoHoaDon")).replaceAll("\\s", "");
				fromDate = commons.getTextJsonNode(jsonData.at("/FromDate")).replaceAll("\\s", "");
				toDate = commons.getTextJsonNode(jsonData.at("/ToDate")).replaceAll("\\s", "");
				nvMst = commons.getTextJsonNode(jsonData.at("/NbanMst")).trim().replaceAll("\\s+", " ");
			}

			MsgRsp rsp = new MsgRsp(header);
			MspResponseStatus responseStatus = null;

			ObjectId objectId = null;
			Document docTmp = null;

			Iterable<Document> cursor = null;
			Iterator<Document> iter = null;


			LocalDate dateFrom = null;
			LocalDate dateTo = null;
			Document docMatchDate = null;

			dateFrom = "".equals(fromDate) || !commons.checkLocalDate(fromDate, Constants.FORMAT_DATE.FORMAT_DATE_WEB)
					? null
					: commons.convertStringToLocalDate(fromDate, Constants.FORMAT_DATE.FORMAT_DATE_WEB);
			dateTo = "".equals(toDate) || !commons.checkLocalDate(toDate, Constants.FORMAT_DATE.FORMAT_DATE_WEB) ? null
					: commons.convertStringToLocalDate(toDate, Constants.FORMAT_DATE.FORMAT_DATE_WEB);
			if (null != dateTo)
				dateTo = dateTo.plus(1, ChronoUnit.DAYS);
			if (null != dateFrom || null != dateTo) {
				docMatchDate = new Document();
				if (null != dateFrom)
					docMatchDate.append("$gte", dateFrom);
				if (null != dateTo)
					docMatchDate.append("$lt", dateTo);
			}

			Document docMatch = new Document("IssuerId", header.getIssuerId()).append("IsDelete", new Document("$ne", true))
					.append("SignStatus", "SIGNED")
					.append("Status",
							new Document("$in",
									Arrays.asList(Constants.INVOICE_STATUS.COMPLETE)))
					;
			if (!"".equals(soChungTu))
				docMatch.append("SCTu", commons.stringToInteger(soChungTu));
			if (null != docMatchDate)
				docMatch.append("NLap", docMatchDate);
			if (!"".equals(nvMst))
				docMatch.append("NNT.MST",
						new Document("$regex", commons.regexEscapeForMongoQuery(nvMst)).append("$options", "i"));
			
			List<Document> pipeline = new ArrayList<Document>();
			pipeline.add(new Document("$match", docMatch));

			pipeline.add(new Document("$sort",
					new Document("SCTu", -1).append("_id", -1)));
			pipeline.addAll(createFacetForSearchNotSort(page));

			cursor = mongoTemplate.getCollection("CTTNCNhan").aggregate(pipeline).allowDiskUse(true);
			iter = cursor.iterator();
			if (iter.hasNext()) {
				docTmp = iter.next();
			}
			rsp = new MsgRsp(header);
			responseStatus = null;
			ArrayList<HashMap<String, Object>> rowsReturn = new ArrayList<HashMap<String, Object>>();
			HashMap<String, Object> hItem = null;
			if (null != docTmp) {
				page.setTotalRows(docTmp.getInteger("total", 0));
				rsp.setMsgPage(page);
				List<Document> rows = null;
				if (docTmp.get("data") != null && docTmp.get("data") instanceof List) {
					rows = docTmp.getList("data", Document.class);
				}
				if (null != rows) {
					for (Document doc : rows) {
						objectId = (ObjectId) doc.get("_id");
						hItem = new HashMap<String, Object>();
						hItem.put("_id", objectId.toString());
						hItem.put("Status", doc.get("Status"));
						hItem.put("SignStatus", doc.get("SignStatus"));
						hItem.put("MSCTu", doc.get("MSCTu"));
						hItem.put("KHCTu", doc.get("KHCTu"));
						hItem.put("NLap", doc.get("NLap"));
						hItem.put("SCTu", doc.get("SCTu"));
						hItem.put("NNT", doc.get("NNT"));
						rowsReturn.add(hItem);
					}
				}
			}
			
			responseStatus = new MspResponseStatus(0, "SUCCESS");
			rsp.setResponseStatus(responseStatus);

			HashMap<String, Object> mapDataR = new HashMap<String, Object>();
			mapDataR.put("rows", rowsReturn);
			rsp.setObjData(mapDataR);
			return rsp;
		}

		@Override
		public FileInfo viewpdfcttncnV1(JSONRoot jsonRoot) throws Exception {
			FileInfo fileInfo = new FileInfo();

			Msg msg = jsonRoot.getMsg();
			MsgHeader header = msg.getMsgHeader();
			Object objData = msg.getObjData();

			JsonNode jsonData = null;
			if (objData != null) {
				jsonData = Json.serializer().nodeFromObject(msg.getObjData());
			} else {
				return new FileInfo();
			}

			String _id = commons.getTextJsonNode(jsonData.at("/_id")).replaceAll("\\s", "");
			String isConvert = commons.getTextJsonNode(jsonData.at("/IsConvert")).replaceAll("\\s", "");
			ObjectId objectId = null;
			try {
				objectId = new ObjectId(_id);
			} catch (Exception e) {
			}
			ObjectId objectIdIssu = null;
			try {
				objectIdIssu = new ObjectId(header.getIssuerId());
			} catch (Exception e) {
			}

			List<Document> pipeline = new ArrayList<Document>();
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
			Document docTmp = null;
			Iterable<Document> cursor = mongoTemplate.getCollection("CTTNCNhan").aggregate(pipeline).allowDiskUse(true);
			Iterator<Document> iter = cursor.iterator();
			if (iter.hasNext()) {
				docTmp = iter.next();
			}

			if (docTmp == null) {
				return fileInfo;
			}

			String ImgLogo = docTmp.getEmbedded(Arrays.asList("DMMSTNCN", "LoGo"), "");
			String link = docTmp.getEmbedded(Arrays.asList("PramLink", "LinkPortal"), "");
			String KH = docTmp.getEmbedded(Arrays.asList("KHCTu"), "");
			String MS = docTmp.getEmbedded(Arrays.asList("MSCTu"), "");
			String fileNameJP = docTmp.getEmbedded(Arrays.asList("DMMSTNCN", "FileName"), "");
			String dir = docTmp.get("Dir", "");
			String SignStatus = docTmp.get("SignStatus", "");
			String pathLogo = Paths.get(SystemParams.DIR_E_INVOICE_TEMPLATE, "MauSoTNCN",
					docTmp.getEmbedded(Arrays.asList("Issuer", "TaxCode"), ""), ImgLogo).toString();
			String fileName = _id + ".xml";
			if ("SIGNED".equals(SignStatus)) {
				fileName = _id + "_signed.xml";
			}

			File file = new File(dir, fileName);
			if (!file.exists() || !file.isFile()) {
				return new FileInfo();
			}

			org.w3c.dom.Document doc = commons.fileToDocument(file);
			File fileJP = new File(SystemParams.DIR_E_INVOICE_TEMPLATE, fileNameJP);

			ByteArrayOutputStream baosPDF = null;
			baosPDF = jpUtils.viewpdfcttncnV1(fileJP, doc, docTmp, pathLogo, KH, MS, link, "Y".equals(isConvert));
			fileInfo.setFileName("viewpdftncn.pdf");
			fileInfo.setContentFile(baosPDF.toByteArray());

			return fileInfo;
		}

		@Override
		public FileInfo print04CTDT(JSONRoot jsonRoot) throws Exception {
			FileInfo fileInfo = new FileInfo();
			Msg msg = jsonRoot.getMsg();
			Object objData = msg.getObjData();
			JsonNode jsonData = null;
			if (objData != null) {
				jsonData = Json.serializer().nodeFromObject(msg.getObjData());
				String _id = this.commons.getTextJsonNode(jsonData.at("/_id")).replaceAll("\\s", "");
				ObjectId objectId = null;

				try {
					objectId = new ObjectId(_id);
				} catch (Exception e) {
				}

				List<Document> pipeline = new ArrayList<Document>();
				pipeline.add(new Document("$match",
						(new Document("_id", objectId)).append("IsDelete", new Document("$ne", true))));
				Document docTmp = null;
				Iterable<Document> cursor = this.mongoTemplate.getCollection("CTTNCNhanSS").aggregate(pipeline)
						.allowDiskUse(true);
				Iterator<Document> iter = cursor.iterator();
				if (iter.hasNext()) {
					docTmp = (Document) iter.next();
				}

				String fileName = _id + "_signed.xml";
				String dir = (String) docTmp.get("Dir", "");
				File file = new File(dir, fileName);
				if (file.exists() && file.isFile()) {
					org.w3c.dom.Document doc = this.commons.fileToDocument(file);
					String fileNameJP = "04SS-CTDT.jrxml";
					File fileJP = new File(SystemParams.DIR_E_INVOICE_TEMPLATE, fileNameJP);
					ByteArrayOutputStream baosPDF = null;
					baosPDF = this.jpUtils.print04CTDT(fileJP, doc);
					fileInfo.setFileName("print04-ctdt.pdf");
					fileInfo.setContentFile(baosPDF.toByteArray());
					return fileInfo;
				} else {
					return new FileInfo();
				}
			} else {
				return new FileInfo();
			}
		}

		@Override
		public MsgRsp getNotification() throws Exception {
			MsgRsp rsp = new MsgRsp();
			MspResponseStatus responseStatus = null;
			
			Document docTmp = null;
			List<Document> pipeline = new ArrayList<Document>();

			Document docMatch = new Document("IsDelete", false).append("IsActive", true);

			pipeline = new ArrayList<Document>();
			pipeline.add(new Document("$match", docMatch));
			pipeline.add(new Document("$sort", new Document("_id", -1)));
			pipeline.add(new Document("$limit", 1));
			pipeline.add(new Document("$project", 
					new Document("_id", 0)
					.append("InfoCreated", 0)
					.append("InfoUpdate", 0)
					.append("InfoDeleted", 0)
					.append("Date", 0)
					));

			MongoClient mongoClient = cfg.mongoClient();
			MongoCollection<Document> collection = mongoClient.getDatabase(cfg.dbName).getCollection("DMNotification");
			try {
				docTmp = collection.aggregate(pipeline).allowDiskUse(true).first();
			} catch (Exception e) {

			}

			mongoClient.close();

			if (null == docTmp) {
				responseStatus = new MspResponseStatus(9999, Constants.MAP_ERROR.get(9999));
				rsp.setResponseStatus(responseStatus);
				return rsp;
			}
			
			responseStatus = new MspResponseStatus(0, "SUCCESS");
			rsp.setResponseStatus(responseStatus);
			rsp.setObjData(docTmp);
			return rsp;
		}

		@Override
		public FileInfo downLoadTemplate(JSONRoot jsonRoot) throws Exception {
			FileInfo fileInfo = new FileInfo();
			File file_zip = null;
			Msg msg = jsonRoot.getMsg();
			Object objData = msg.getObjData();
			JsonNode jsonData = null;
			if (objData != null) {
				jsonData = Json.serializer().nodeFromObject(msg.getObjData());
			}
			String _id = commons.getTextJsonNode(jsonData.at("/_id")).replaceAll("\\s", "");

			ObjectId objectId = null;
			try {
				objectId = new ObjectId(_id);
			} catch (Exception e) {
			}

			Document docFind = new Document("_id", objectId).append("IsDelete", new Document("$ne", true));
			Document docTmp = null;
			Iterable<Document> cursor = mongoTemplate.getCollection("DMMauSoKyHieu").find(docFind);
			Iterator<Document> iter = cursor.iterator();
			if (iter.hasNext()) {
				docTmp = iter.next();
			}

			if (null == docTmp) {
				return new FileInfo();
			}
			String fileName = docTmp.getEmbedded(Arrays.asList("Templates","FileName"), "");

			File file = new File(SystemParams.DIR_E_INVOICE_TEMPLATE, fileName);
			File tam = new File(SystemParams.DIR_TEMPORARY, fileName);
			copyFileUsingStream(file, tam);

			file_zip = new File(tam.getAbsolutePath());
			fileInfo.setContentFile(Files.readAllBytes(file_zip.toPath()));
			fileInfo.setFileName(fileName);
			return fileInfo;
		}

		@Override
		public FileInfo einvoiceMTTXml(JSONRoot jsonRoot) throws Exception {
			FileInfo fileInfo = new FileInfo();
			ArrayList<HashMap<String, String>> arrayInfoInvoice = new ArrayList<>();
			HashMap<String, String> hItem = null;
			File file_zip = null;
			String fileName = "";
			Msg msg = jsonRoot.getMsg();
			MsgHeader header = msg.getMsgHeader();
			Object objData = msg.getObjData();
			ByteArrayOutputStream bos = new ByteArrayOutputStream();
			JsonNode jsonData = null;
			try {

				if (objData != null) {
					jsonData = Json.serializer().nodeFromObject(msg.getObjData());
				}
				String _id = "";
				String _token = commons.getTextJsonNode(jsonData.at("/_token")).replaceAll("\\s", "");

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

					Document docTmp = null;
					Iterable<Document> cursor = mongoTemplate.getCollection("EInvoiceMTT").aggregate(pipeline)
							.allowDiskUse(true);
					Iterator<Document> iter = cursor.iterator();
					if (iter.hasNext()) {
						docTmp = iter.next();
					}
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

						docTmp = null;
						cursor = mongoTemplate.getCollection("EInvoiceMTT").aggregate(pipeline).allowDiskUse(true);
						iter = cursor.iterator();
						if (iter.hasNext()) {
							docTmp = iter.next();
						}
					}
					if (null == docTmp || docTmp.get("DMMauSoKyHieu") == null) {
						return new FileInfo();
					}
					String mskh = docTmp.getEmbedded(Arrays.asList("EInvoiceDetail", "TTChung", "KHMSHDon"), "")
							+ docTmp.getEmbedded(Arrays.asList("EInvoiceDetail", "TTChung", "KHHDon"), "");
					Integer shd = docTmp.getEmbedded(Arrays.asList("EInvoiceDetail", "TTChung", "SHDon"), 0);
					String namexml = mskh + "_" + shd + ".xml";

					String dir = docTmp.get("Dir", "");
					String signStatusCode = docTmp.get("SignStatusCode", "");
					String eInvoiceStatus = docTmp.get("EInvoiceStatus", "");
					fileName = _id + ".xml";

					if ("SIGNED".equals(signStatusCode)) {
						fileName = _id + "_signed.xml";
					} else if ("PENDING".equals(eInvoiceStatus)) {
						fileName = _id + "_pending.xml";
					}

					File file = new File(dir, fileName);
					File tam = new File(dir, namexml);
					copyFileUsingStream(file, tam);

					hItem = new HashMap<>();
					hItem.put("UrlFile", tam.getAbsolutePath());
					arrayInfoInvoice.add(hItem);
				}
				if (arrayInfoInvoice.size() == 0) {
					return fileInfo;
				}
				if (arrayInfoInvoice.size() == 1) {
					hItem = arrayInfoInvoice.get(0);
					file_zip = new File(hItem.get("UrlFile"));
					fileInfo.setContentFile(Files.readAllBytes(file_zip.toPath()));
					fileInfo.setFileName(fileName);
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
				}
				return fileInfo;

			} catch (NullPointerException | UnsupportedEncodingException e) {
				return new FileInfo();
			}
		}

		@Override
		public FileInfo cttncnXmlV1(JSONRoot jsonRoot) throws Exception {
			FileInfo fileInfo = new FileInfo();
			Msg msg = jsonRoot.getMsg();

			Object objData = msg.getObjData();
			ByteArrayOutputStream bos = new ByteArrayOutputStream();
			JsonNode jsonData = null;
			if (objData != null) {
				jsonData = Json.serializer().nodeFromObject(msg.getObjData());
			}

			String _token = commons.getTextJsonNode(jsonData.at("/_token")).replaceAll("\\s", "");

			ObjectId objectId = new ObjectId(_token);

			Document findTmp = new Document("_id", objectId).append("IsDelete", false);
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

			String _id = "";
			String fileName = "";
			String namexml = "";
			File file_zip = null;
			ArrayList<HashMap<String, String>> arrayInfoInvoice = new ArrayList<>();
			HashMap<String, String> hItem = null;

			for (Object id : rows) {
				_id = id.toString();
				try {
					objectId = new ObjectId(_id);
				} catch (Exception e) {
				}

				Document docTmp = null;
				Document docFind = new Document("_id", objectId).append("IsDelete", new Document("$ne", true));
				List<Document> pipeline = new ArrayList<Document>();
				pipeline.add(new Document("$match", docFind));

				Iterable<Document> cursor = mongoTemplate.getCollection("CTTNCNhan").aggregate(pipeline)
						.allowDiskUse(true);
				Iterator<Document> iter = cursor.iterator();
				if (iter.hasNext()) {
					docTmp = iter.next();
				}

				if (null == docTmp) {
					continue;
				}

				String khctu = docTmp.get("KHCTu", "");
				String mtdiep = docTmp.get("MTDiep", "");
				Integer sctu = docTmp.get("SCTu", 0);
				if (sctu == 0) {
					namexml = khctu.replaceAll("/", "-") + "_" + mtdiep + ".xml";
				} else {
					namexml = khctu.replaceAll("/", "-") + "_" + sctu + ".xml";
				}

				String dir = docTmp.get("Dir", "");
				String signStatus = docTmp.get("SignStatus", "");

				fileName = _id + ".xml";
				if ("SIGNED".equals(signStatus)) {
					fileName = _id + "_signed.xml";
				} else {
					fileName = _id + ".xml";
				}

				File file = new File(dir, fileName);
				File tam = new File(dir, namexml);
				copyFileUsingStream(file, tam);

				hItem = new HashMap<>();
				hItem.put("UrlFile", tam.getAbsolutePath());
				arrayInfoInvoice.add(hItem);
			}

			if (arrayInfoInvoice.size() == 0) {
				return fileInfo;
			}
			if (arrayInfoInvoice.size() == 1) {
				hItem = arrayInfoInvoice.get(0);
				file_zip = new File(hItem.get("UrlFile"));
				fileInfo.setContentFile(Files.readAllBytes(file_zip.toPath()));
				fileInfo.setFileName(namexml);
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
				fileInfo.setFileName("ChungTu_TNCN.zip");
				fileInfo.setContentFile(bos.toByteArray());
			}
			return fileInfo;
		}
}
