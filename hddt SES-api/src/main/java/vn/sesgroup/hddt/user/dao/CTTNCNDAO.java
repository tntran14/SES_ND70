package vn.sesgroup.hddt.user.dao;

import java.io.InputStream;

import com.api.message.JSONRoot;
import com.api.message.MsgRsp;

import vn.sesgroup.hddt.dto.FileInfo;
import vn.sesgroup.hddt.utility.UpdateSignedMultiBillReq;

public interface CTTNCNDAO {
	public MsgRsp list(JSONRoot jsonRoot) throws Exception;

	public MsgRsp crud(JSONRoot jsonRoot) throws Exception;

	public MsgRsp detail(JSONRoot jsonRoot, String _id) throws Exception;

	public FileInfo getFileForSign(JSONRoot jsonRoot) throws Exception;

	public Object signSingle(InputStream is, JSONRoot jsonRoot) throws Exception;

	MsgRsp importExcel(JSONRoot jsonRoot) throws Exception;
	
	MsgRsp importExcelV1(JSONRoot jsonRoot) throws Exception;

	public FileInfo getFileForSignAll(JSONRoot jsonRoot) throws Exception;

	public Object signAll(UpdateSignedMultiBillReq input, JSONRoot jsonRoot) throws Exception;

	public MsgRsp sendMail(JSONRoot jsonRoot) throws Exception;

	public MsgRsp sendMailAll(JSONRoot jsonRoot) throws Exception;

	public MsgRsp crudV1(JSONRoot jsonRoot) throws Exception;

	public MsgRsp listV1(JSONRoot jsonRoot) throws Exception;

	public MsgRsp detailV1(JSONRoot jsonRoot, String _id) throws Exception;

	public FileInfo getFileForSignV1(JSONRoot jsonRoot) throws Exception;

	public Object signSingleV1(InputStream is, JSONRoot jsonRoot, String _id) throws Exception;

	public MsgRsp history(JSONRoot jsonRoot, String _id) throws Exception;

	public MsgRsp sendMailV1(JSONRoot jsonRoot) throws Exception;
}
