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
	public FileInfo getFileForSign(JSONRoot jsonRoot)throws Exception;
	public Object signSingle(InputStream is, JSONRoot jsonRoot)throws Exception;
	MsgRsp importExcel(JSONRoot jsonRoot) throws Exception;
	public FileInfo getFileForSignAll(JSONRoot jsonRoot)throws Exception;
	public Object signAll(UpdateSignedMultiBillReq input, JSONRoot jsonRoot)throws Exception;
}
