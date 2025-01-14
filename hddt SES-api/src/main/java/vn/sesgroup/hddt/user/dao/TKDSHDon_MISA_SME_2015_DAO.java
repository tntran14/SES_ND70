package vn.sesgroup.hddt.user.dao;

import com.api.message.JSONRoot;
import com.api.message.MsgRsp;

import vn.sesgroup.hddt.dto.FileInfo;

public interface TKDSHDon_MISA_SME_2015_DAO {
	public MsgRsp list(JSONRoot jsonRoot) throws Exception;

	public FileInfo exportExcelDSHDVAT(JSONRoot jsonRoot) throws Exception;

}
