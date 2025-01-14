package vn.sesgroup.hddt.user.dao;

import com.api.message.JSONRoot;
import com.api.message.MsgRsp;

import vn.sesgroup.hddt.dto.FileInfo;

public interface HHDSDUserDao {
	public MsgRsp list(JSONRoot jsonRoot) throws Exception;
	public MsgRsp detail(JSONRoot jsonRoot, String _id)throws Exception;
	public FileInfo dowloadfile(JSONRoot jsonRoot)throws Exception;

}
