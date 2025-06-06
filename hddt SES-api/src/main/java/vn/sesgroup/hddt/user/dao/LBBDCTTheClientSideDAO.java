package vn.sesgroup.hddt.user.dao;

import java.io.InputStream;

import com.api.message.JSONRoot;
import com.api.message.MsgRsp;

import vn.sesgroup.hddt.dto.FileInfo;

public interface LBBDCTTheClientSideDAO {
	public MsgRsp detail(JSONRoot jsonRoot) throws Exception;
	public FileInfo getFileForSign(JSONRoot jsonRoot) throws Exception;
	public MsgRsp signSingle(InputStream is, JSONRoot jsonRoot, String _id) throws Exception;
	public FileInfo printbb(JSONRoot jsonRoot)throws Exception;
	public MsgRsp sendMail(JSONRoot jsonRoot) throws Exception;
}
