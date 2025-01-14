package vn.sesgroup.hddt.user.dao;

import java.io.InputStream;

import com.api.message.JSONRoot;
import com.api.message.MsgRsp;

import vn.sesgroup.hddt.dto.FileInfo;

public interface EInvoiceMTTDAO {
	public MsgRsp crud(JSONRoot jsonRoot) throws Exception;
	public MsgRsp list(JSONRoot jsonRoot) throws Exception;
	public MsgRsp detail(JSONRoot jsonRoot, String _id) throws Exception;
	public FileInfo getFileForSign(JSONRoot jsonRoot) throws Exception;
	public MsgRsp signSingle(InputStream is, JSONRoot jsonRoot) throws Exception;
	public MsgRsp refreshStatusCQT(JSONRoot jsonRoot) throws Exception;
	public MsgRsp history(JSONRoot jsonRoot, String _id) throws Exception;
	public MsgRsp publishHD(JSONRoot jsonRoot) throws Exception;
	public MsgRsp sendMail(JSONRoot jsonRoot) throws Exception;
	public MsgRsp createTDSendTax(JSONRoot jsonRoot) throws Exception;
//	public MsgRsp list_send_cqt(JSONRoot jsonRoot) throws Exception;
//	public MsgRsp sendListCQT(JSONRoot jsonRoot) throws Exception;
//	public FileInfo getFileForSignMTT(JSONRoot jsonRoot) throws Exception;
//	public MsgRsp signSingleAndSendCQTMTT(InputStream is, JSONRoot jsonRoot) throws Exception;
//	public FileInfo getFileForSignALLMTT(JSONRoot jsonRoot) throws Exception;
//	public MsgRsp signSingleAndSendALLCQTMTT(InputStream is, JSONRoot jsonRoot) throws Exception;
//	public MsgRsp sendCQTMTT(JSONRoot jsonRoot) throws Exception;	
//	public MsgRsp list_einvoice_mtt_send(JSONRoot jsonRoot) throws Exception;
//	public MsgRsp einvoice_mtt_list_sendAll(JSONRoot jsonRoot) throws Exception;

}
