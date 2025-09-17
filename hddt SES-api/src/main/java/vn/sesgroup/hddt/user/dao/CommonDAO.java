package vn.sesgroup.hddt.user.dao;

import com.api.message.JSONRoot;
import com.api.message.MsgRsp;

import vn.sesgroup.hddt.dto.FileInfo;

public interface CommonDAO {
	public MsgRsp getFullParams(JSONRoot jsonRoot) throws Exception;
	public FileInfo printEInvoice(JSONRoot jsonRoot) throws Exception;
	public MsgRsp getAutoCompleteProducts(JSONRoot jsonRoot) throws Exception;
	
	public MsgRsp listSearchCustomer(JSONRoot jsonRoot) throws Exception;
	
	public MsgRsp listSearchCustomerUpdate(JSONRoot jsonRoot) throws Exception;
	
	public MsgRsp listEInvoiceSigned(JSONRoot jsonRoot) throws Exception;
	public MsgRsp listEInvoicesSigned(JSONRoot jsonRoot) throws Exception;
	FileInfo printExport(JSONRoot jsonRoot) throws Exception;
	public FileInfo viewpdf(JSONRoot jsonRoot)throws Exception;
	
	
	FileInfo printAgent(JSONRoot jsonRoot) throws Exception;
	FileInfo printEInvoiceBH(JSONRoot jsonRoot) throws Exception;
	public FileInfo printEinvoiceAll(JSONRoot jsonRoot)throws Exception;
	public FileInfo einvoiceXml(JSONRoot jsonRoot)throws Exception;
	
	public FileInfo einvoice1Xml(JSONRoot jsonRoot)throws Exception;
	public FileInfo printEinvoice1All(JSONRoot jsonRoot)throws Exception;
	public FileInfo print04(JSONRoot jsonRoot)throws Exception;
	public FileInfo print04CTDT(JSONRoot jsonRoot)throws Exception;
	public FileInfo viewpdftncn(JSONRoot jsonRoot)throws Exception;
	public FileInfo viewpdfcttncn(JSONRoot jsonRoot)throws Exception;
	public FileInfo getXml(JSONRoot jsonRoot)throws Exception;
	

	
	public FileInfo downLoadFile(JSONRoot jsonRoot)throws Exception;
	public FileInfo downLoadTemplate(JSONRoot jsonRoot)throws Exception;
	public FileInfo getXmlThue(JSONRoot jsonRoot)throws Exception;
	public MsgRsp getFullRightAdmin(JSONRoot jsonRoot) throws Exception;
	
	public FileInfo exportXml(JSONRoot jsonRoot)throws Exception;
	public FileInfo exportPDF(JSONRoot jsonRoot)throws Exception;
	public FileInfo printExportAll(JSONRoot jsonRoot)throws Exception;
	
	public MsgRsp getFullRightAdminManager(JSONRoot jsonRoot) throws Exception;
	
	public MsgRsp detailMaSoThue(JSONRoot jsonRoot) throws Exception;
	
	public FileInfo viewPdfTiepnhan(JSONRoot jsonRoot)throws Exception;
	
	
	public FileInfo cttncnXml(JSONRoot jsonRoot)throws Exception;
	public FileInfo cttncnXmlV1(JSONRoot jsonRoot)throws Exception;
	public MsgRsp saveDataToBase64(JSONRoot jsonRoot) throws Exception;
	
	public FileInfo printEinvoiceAllDB(JSONRoot jsonRoot)throws Exception;
	
	FileInfo printEInvoiceMTT(JSONRoot jsonRoot) throws Exception;
	
	public MsgRsp listEInvoiceMTTSigned(JSONRoot jsonRoot) throws Exception;
	
	public FileInfo print04_mtt(JSONRoot jsonRoot)throws Exception;
	
	public MsgRsp scratchingTaxCode(JSONRoot jsonRoot) throws Exception;
	public MsgRsp scratchingTaxCodeV2(JSONRoot jsonRoot) throws Exception;
	public FileInfo printbb(JSONRoot jsonRoot)throws Exception;
	public MsgRsp listCTTNCNSigned(JSONRoot jsonRoot) throws Exception;
	public FileInfo viewpdfcttncnV1(JSONRoot jsonRoot)throws Exception;
	public MsgRsp getNotification() throws Exception;
	public FileInfo einvoiceMTTXml(JSONRoot jsonRoot)throws Exception;

}
