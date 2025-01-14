package vn.sesgroup.hddt.user.dao;

import com.api.message.JSONRoot;
import com.api.message.MsgRsp;

import vn.sesgroup.hddt.dto.FileInfo;

public interface MauHD23UpdateAdminDAO {
	public MsgRsp list(JSONRoot jsonRoot) throws Exception;
	public MsgRsp checkdb(JSONRoot jsonRoot)throws Exception;
	public MsgRsp updatedb(JSONRoot jsonRoot)throws Exception;
	public FileInfo exportExcel(JSONRoot jsonRoot) throws Exception;
}
