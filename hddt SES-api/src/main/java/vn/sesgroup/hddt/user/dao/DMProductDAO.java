package vn.sesgroup.hddt.user.dao;

import com.api.message.JSONRoot;
import com.api.message.MsgRsp;

import vn.sesgroup.hddt.dto.FileInfo;

public interface DMProductDAO {
	public MsgRsp list(JSONRoot jsonRoot) throws Exception;
	public MsgRsp crud(JSONRoot jsonRoot) throws Exception;
	public MsgRsp detail(JSONRoot jsonRoot, String _id) throws Exception;
	public MsgRsp importExcel(JSONRoot jsonRoot) throws Exception;
	public FileInfo exportExcel(JSONRoot jsonRoot) throws Exception;
}
