package vn.sesgroup.hddt.controller.einvoice_mtt;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.ss.util.NumberToTextConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.context.WebApplicationContext;

import com.api.message.JSONRoot;
import com.api.message.Msg;
import com.api.message.MsgParam;
import com.api.message.MsgParams;
import com.api.message.MsgRsp;
import com.api.message.MspResponseStatus;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;

import vn.sesgroup.hddt.controller.AbstractController;
import vn.sesgroup.hddt.controller.einvoice.EInvoiceImportAutoController;
import vn.sesgroup.hddt.dto.BaseDTO;
import vn.sesgroup.hddt.dto.CurrentUserProfile;
import vn.sesgroup.hddt.dto.LoginRes;
import vn.sesgroup.hddt.resources.RestAPIUtility;
import vn.sesgroup.hddt.utils.Constants;
import vn.sesgroup.hddt.utils.Json;

@Controller
@RequestMapping({ "/einvoice_mtt-import-auto"})

@Scope(value = WebApplicationContext.SCOPE_REQUEST)
public class EInvoiceMTTImportAutoController extends AbstractController {
	private static final Logger log = LogManager.getLogger(EInvoiceImportAutoController.class);
	@Autowired
	RestAPIUtility restAPI;
	private String loaiTienTt;

	private void LoadParameter(CurrentUserProfile cup, Locale locale, HttpServletRequest req, String action) {
		try {
			BaseDTO baseDTO = new BaseDTO(req);
			Msg msg = baseDTO.createMsg(cup, Constants.MSG_ACTION_CODE.LOAD_PARAMS);

			/* DANH SACH THAM SO */
			HashMap<String, String> hashConds = null;
			ArrayList<HashMap<String, String>> conds = null;
			MsgParam msgParam = null;
			MsgParams msgParams = new MsgParams();

			msgParam = new MsgParam();
			msgParam.setId("param01");
			msgParam.setParam("DMPaymentType");
			msgParams.getParams().add(msgParam);

			msgParam = new MsgParam();
			msgParam.setId("param02");
			msgParam.setParam("DMMauSoKyHieuForCreate");
			msgParams.getParams().add(msgParam);

			msgParam = new MsgParam();
			msgParam.setId("param03");
			msgParam.setParam("DMCurrencies");
			msgParams.getParams().add(msgParam);

			/* END: DANH SACH THAM SO */
			msg.setObjData(msgParams);

			JSONRoot root = new JSONRoot(msg);
			MsgRsp rsp = restAPI.callAPINormal("/commons/get-full-params", cup.getLoginRes().getToken(),
					HttpMethod.POST, root);
			MspResponseStatus rspStatus = rsp.getResponseStatus();
			String KHHDon = "";
			if (rspStatus.getErrorCode() == 0 && rsp.getObjData() != null) {
				LinkedHashMap<String, String> hItem = null;

				JsonNode jsonData = Json.serializer().nodeFromObject(rsp.getObjData());
				if (null != jsonData.at("/param01") && jsonData.at("/param01") instanceof ArrayNode) {
					hItem = new LinkedHashMap<String, String>();
					for (JsonNode o : jsonData.at("/param01")) {
						hItem.put(commons.getTextJsonNode(o.get("code")), commons.getTextJsonNode(o.get("name")));
					}
					req.setAttribute("map_paymenttype", hItem);
				}
				if (null != jsonData.at("/param02") && jsonData.at("/param02") instanceof ArrayNode) {
					hItem = new LinkedHashMap<String, String>();
					for (JsonNode o : jsonData.at("/param02")) {
						KHHDon = commons.getTextJsonNode(o.get("KHHDon"));
						char words = KHHDon.charAt(KHHDon.length() - 3);
						String s = String.valueOf(words);
						if ("M".equals(s)) {
							hItem.put(commons.getTextJsonNode(o.get("_id")), commons.getTextJsonNode(o.get("KHMSHDon"))
									+ commons.getTextJsonNode(o.get("KHHDon")));
						}
					}
					req.setAttribute("map_mausokyhieu", hItem);
				}
				if (null != jsonData.at("/param03") && jsonData.at("/param03") instanceof ArrayNode) {
					hItem = new LinkedHashMap<String, String>();
					for (JsonNode o : jsonData.at("/param03")) {
						hItem.put(commons.getTextJsonNode(o.get("code")), commons.getTextJsonNode(o.get("code")));
						if (action.equals("CREATE") && null != o.get("IsDefault")
								&& o.get("IsDefault").asBoolean(false)) {
							loaiTienTt = commons.getTextJsonNode(o.get("code"));
							req.setAttribute("DVTTe", loaiTienTt);
						}
					}
					req.setAttribute("map_currencies", hItem);
				}
			}

		} catch (Exception e) {
		}
	}
	
	@RequestMapping(value = "/init", method = {RequestMethod.POST})
	public String init(Locale locale, HttpServletRequest req, HttpSession session
			, @RequestAttribute(name = "transaction", value = "", required = false) String transaction, String action) throws Exception{
		CurrentUserProfile cup = getCurrentlyAuthenticatedPrincipal();
		req.setAttribute("_header_", "Import hóa đơn Máy tính tiền");
		LoadParameter(cup, locale, req, action);
		return "einvoice_mtt/einvoice_mtt-import-auto";
	}
	
	private String mauSoHdon;
	private String dataFileName;
	
	public BaseDTO checkDataToImport(HttpServletRequest req, HttpSession session, String transaction
			, CurrentUserProfile cup) throws Exception{
		BaseDTO dto = new BaseDTO();
		dto.setErrorCode(0);
		mauSoHdon = commons.getParameterFromRequest(req, "mau-so-hdon").replaceAll("\\s", "");
		dataFileName = commons.getParameterFromRequest(req, "dataFileName").replaceAll("\\s", "");
		if("".equals(dataFileName)) {
			dto.setErrorCode(1);
			dto.getErrorMessages().add("Vui lòng chọn tập tin chứa dữ liệu.");
		}if("".equals(mauSoHdon)) {
			dto.setErrorCode(1);
			dto.getErrorMessages().add("Vui lòng chọn mẫu số hóa đơn.");
		}
		
		return dto;
	}
	
	@RequestMapping(value = "/check-data-import",  produces = MediaType.APPLICATION_JSON_VALUE, method = RequestMethod.POST)
	@ResponseBody
	public BaseDTO execCheckDataToImport(Locale locale, HttpServletRequest req, HttpSession session
			, @RequestParam(value = "transaction", required = false, defaultValue = "") String transaction) throws Exception {
		String token = "";
		if (null != session.getAttribute(Constants.SESSION_TYPE.SESSION_TOKEN_EXECUTE)) {
			token = session.getAttribute(Constants.SESSION_TYPE.SESSION_TOKEN_EXECUTE).toString();
			session.removeAttribute(token);
		}
		session.removeAttribute(Constants.SESSION_TYPE.SESSION_TOKEN_EXECUTE);

		BaseDTO dto = new BaseDTO();	
		String messageConfirm = "Bạn có muốn thực hiện import không?";
		CurrentUserProfile cup = getCurrentlyAuthenticatedPrincipal();
		dto = checkDataToImport(req, session, transaction, cup);
		if(0 != dto.getErrorCode()) {
			dto.setErrorCode(999);
			dto.setResponseData(Constants.MAP_ERROR.get(999));
			return dto;
		}
		
		token = commons.csRandomAlphaNumbericString(30);
		session.setAttribute(Constants.SESSION_TYPE.SESSION_TOKEN_EXECUTE, token);
		
		HashMap<String, String> hInfo = new HashMap<String, String>();
		hInfo.put("CONFIRM", messageConfirm);
		hInfo.put("TOKEN", token);

		dto.setResponseData(hInfo);
		dto.setErrorCode(0);
		return dto;
	}
	
	@RequestMapping(value = "/import-auto",  produces = MediaType.APPLICATION_JSON_VALUE, method = RequestMethod.POST)
	@ResponseBody
	public BaseDTO execImportData(HttpServletRequest req, HttpSession session
			, @RequestParam(value = "transaction", required = false, defaultValue = "") String transaction
			, @RequestParam(value = "tokenTransaction", required = false, defaultValue = "") String tokenTransaction) throws Exception{
		BaseDTO dtoRes = new BaseDTO();
		
		CurrentUserProfile cup = getCurrentlyAuthenticatedPrincipal();
		dtoRes = checkDataToImport(req, session, transaction, cup);
		if(0 != dtoRes.getErrorCode()) {
			dtoRes.setErrorCode(999);
			dtoRes.setResponseData(Constants.MAP_ERROR.get(999));
			return dtoRes;
		}
		
		/*CHECK TOKEN*/
		String token = session.getAttribute(Constants.SESSION_TYPE.SESSION_TOKEN_EXECUTE) == null ? ""
				: session.getAttribute(Constants.SESSION_TYPE.SESSION_TOKEN_EXECUTE).toString();
		session.removeAttribute(Constants.SESSION_TYPE.SESSION_TOKEN_EXECUTE);
		if ("".equals(token) || !tokenTransaction.equals(token)) {
			dtoRes.setErrorCode(997);
			dtoRes.setResponseData(Constants.MAP_ERROR.get(997));
			return dtoRes;
		}
		/*END: CHECK TOKEN*/
		
		dtoRes = new BaseDTO(req);
		Msg msg = dtoRes.createMsg(cup, Constants.MSG_ACTION_CODE.CREATED);
		HashMap<String, Object> hData = new HashMap<>();
		hData.put("DataFileName", dataFileName);
		hData.put("MauSoHdon", mauSoHdon);
		msg.setObjData(hData);
		JSONRoot root = new JSONRoot(msg);
		//-----------
		
		
		MsgRsp rsp = restAPI.callAPINormal("/einvoice_mtt/import-data-auto", cup.getLoginRes().getToken(), HttpMethod.POST, root);
		
		//---------
		
		MspResponseStatus rspStatus = rsp.getResponseStatus();
		if(rspStatus.getErrorCode() == 0) {
			dtoRes.setErrorCode(0);
			dtoRes.setResponseData("Import hóa đơn thành công.");
		}else {
			dtoRes.setErrorCode(rspStatus.getErrorCode());
			dtoRes.setResponseData(rspStatus.getErrorDesc());
		}
		return dtoRes;
	}
	

	@RequestMapping(value = "/init-products", method = { RequestMethod.POST })
	public String importProdusts(HttpServletRequest req) throws Exception {
		String invoiceType = commons.getParameterFromRequest(req, "invoiceType");
		req.setAttribute("_header_", "Import danh sách hàng hóa, dịch vụ");
		req.setAttribute("_invoiceType_", invoiceType);
		return "einvoice_mtt/einvoice_mtt-import-products";
	}
	
	public BaseDTO checkDataToImportProducts(HttpServletRequest req) throws Exception {
		BaseDTO dto = new BaseDTO();
		dto.setErrorCode(0);
		dataFileName = commons.getParameterFromRequest(req, "dataFileName").replaceAll("\\s", "");
		if ("".equals(dataFileName)) {
			dto.setErrorCode(1);
			dto.getErrorMessages().add("Vui lòng chọn tập tin chứa dữ liệu.");
		}
		return dto;
	}
	
	@RequestMapping(value = "/import-products",  produces = MediaType.APPLICATION_JSON_VALUE, method = RequestMethod.POST)
	@ResponseBody
	public BaseDTO importProducts(HttpServletRequest req) throws Exception{		
		CurrentUserProfile cup = getCurrentlyAuthenticatedPrincipal();
		String issuerId = cup.getLoginRes().getIssuerId();
		BaseDTO dtoRes = checkDataToImportProducts(req);
		if(0 != dtoRes.getErrorCode()) {
			dtoRes.setErrorCode(999);
			dtoRes.setResponseData(Constants.MAP_ERROR.get(999));
			return dtoRes;
		}
		
		Path path = Paths.get(vn.sesgroup.hddt.utils.SystemParams.DIR_E_INVOICE_TEMPORARY, issuerId, dataFileName);
		File file = path.toFile();
		if (!(file.exists() && file.isFile())) {
			dtoRes.setErrorCode(999);
			dtoRes.setResponseData("Tập tin import dữ liệu không tồn tại.");
			return dtoRes;
		}
		
		List<Map<String, String>> products = new ArrayList<>();
		try (Workbook wb = WorkbookFactory.create(file)) {
			Sheet sheet = wb.getSheetAt(0);
			boolean skipHeader = true;
			List<Cell> columnNames = new ArrayList<Cell>();
			List<Cell> cells = null;
			Cell cell = null;
			for (Row row1 : sheet) {
				int lastColumn =row1.getLastCellNum();
				if (skipHeader) {
					for (int i = 0; i < lastColumn; i++) {
						cell = row1.getCell(i, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
						columnNames.add(cell);
					}
					skipHeader = false;
					continue;
				}

				Cell firstCell = row1.getCell(0, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
				if (firstCell == null || "".equals(commons.getCellValueAsString(firstCell).trim())) {
					break;
				}

				cells = new ArrayList<Cell>();
				for (int i = 0; i < lastColumn; i++) {
					cell = row1.getCell(i, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
					cells.add(cell);
				}

				Map<String, String> product = extractInfoProductFromCells(columnNames, cells);
				products.add(product);
			}
		} catch (Exception e) {
			dtoRes.setErrorCode(999);
			dtoRes.setResponseData("Lỗi khi import dữ liệu.");
			return dtoRes;
		}

		dtoRes.setResponseData(products);
		return dtoRes;
	}
	
	private Map<String, String> extractInfoProductFromCells(List<Cell> columnNames, List<Cell> cells) throws Exception {
		Map<String, String> map = new LinkedHashMap<String, String>();

		for (int i = 0; i < columnNames.size(); i++) {
			String key = "";
			Cell cellColumnName = columnNames.get(i);
			Cell cell = cells.get(i);

			if (cellColumnName != null) {
				String value = cellColumnName.getStringCellValue().trim();
				switch (value) {
				case "TenHangHoa(*)":
					key = "ProductName";
					break;
				case "MaHangHoa":
					key = "ProductCode";
					break;
				case "DonViTinh":
					key = "Unit";
					break;
				case "SoLuong":
					key = "Quantity";
					break;
				case "DonGia":
					key = "Price";
					break;
				case "ChietKhau(%)":
					key = "DiscountRate";
					break;
				case "ThanhTien(*)":
					key = "Total";
					break;
				case "ThueSuat(*)":
					key = "VATRate";
					break;
				case "TienThue(*)":
					key = "VATAmount";
					break;
				case "TongTien(*)":
					key = "Amount";
					break;
				case "TinhChat(*)":
					key = "Feature";
					break;
				}
			}

			if (cell != null) {
				CellType cellType = null;
				if (cell.getCellType() == CellType.FORMULA) {
					cellType = cell.getCachedFormulaResultType();
				} else {
					cellType = cell.getCellType();
				}
				
				switch (cellType) {
				case STRING:
					map.put(key, cell.getStringCellValue());
					break;
				case NUMERIC:
					map.put(key, (NumberToTextConverter.toText(cell.getNumericCellValue())));
					break;
				case BLANK:
					break;
				default:
					break;
				}
			}
		}

		return map;
	}
}
