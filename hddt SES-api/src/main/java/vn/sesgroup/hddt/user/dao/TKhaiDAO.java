package vn.sesgroup.hddt.user.dao;

import java.io.InputStream;

import com.api.message.JSONRoot;
import com.api.message.MsgRsp;

import vn.sesgroup.hddt.dto.FileInfo;

public interface TKhaiDAO {
	public MsgRsp crud(JSONRoot jsonRoot) throws Exception;
	public MsgRsp crudV1(JSONRoot jsonRoot) throws Exception;
	public MsgRsp list(JSONRoot jsonRoot) throws Exception;
	public MsgRsp detail(JSONRoot jsonRoot, String _id) throws Exception;
	public MsgRsp detailV1(JSONRoot jsonRoot, String _id) throws Exception;
	public FileInfo getFileForSign(JSONRoot jsonRoot) throws Exception;
	public MsgRsp signSingle(InputStream is, JSONRoot jsonRoot, String _id) throws Exception;
	public MsgRsp signSingleV1(InputStream is, JSONRoot jsonRoot, String _id) throws Exception;
	public MsgRsp refreshStatusCQT(JSONRoot jsonRoot) throws Exception;
	public MsgRsp refreshStatusCQTV1(JSONRoot jsonRoot) throws Exception;
}
