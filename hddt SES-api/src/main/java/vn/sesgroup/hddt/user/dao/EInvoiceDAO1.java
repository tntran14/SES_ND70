package vn.sesgroup.hddt.user.dao;

import java.io.InputStream;

import com.api.message.JSONRoot;
import com.api.message.MsgRsp;

import vn.sesgroup.hddt.dto.FileInfo;

public interface EInvoiceDAO1 {
	public MsgRsp crud(JSONRoot jsonRoot) throws Exception;
	public MsgRsp list(JSONRoot jsonRoot) throws Exception;
	public MsgRsp detail(JSONRoot jsonRoot, String _id) throws Exception;
	
	/*LAY THONG TIN TAP TIN XML*/
	public FileInfo getFileForSign(JSONRoot jsonRoot) throws Exception;
	
//	public FileInfo getFilesForSign(JSONRoot jsonRoot) throws Exception;
	public MsgRsp signSingle(InputStream is, JSONRoot jsonRoot) throws Exception;
	public MsgRsp refreshStatusCQT(JSONRoot jsonRoot) throws Exception;
	MsgRsp importExcel(JSONRoot jsonRoot) throws Exception;
	public MsgRsp sendMail(JSONRoot jsonRoot)throws Exception;
	public MsgRsp history(JSONRoot jsonRoot, String _id)throws Exception;
	public MsgRsp change(JSONRoot jsonRoot, String _id)throws Exception;
	public MsgRsp importExcelAuto(JSONRoot jsonRoot) throws Exception;
	public MsgRsp checkSHD(JSONRoot jsonRoot) throws Exception;
	public MsgRsp checkMST(JSONRoot jsonRoot) throws Exception;
	public MsgRsp saveNMua(JSONRoot jsonRoot) throws Exception;
	public MsgRsp getMS(JSONRoot jsonRoot) throws Exception;
	public MsgRsp checkHistoryMST(JSONRoot jsonRoot) throws Exception;
}
