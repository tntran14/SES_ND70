package vn.sesgroup.hddt.user.dao;

import com.api.message.JSONRoot;
import com.api.message.MsgRsp;

import vn.sesgroup.hddt.dto.FileInfo;

public interface TKDSHDonPXKNBDAO {
	public MsgRsp list(JSONRoot jsonRoot) throws Exception;
	public FileInfo exportExcelToFAST(JSONRoot jsonRoot) throws Exception;
	public FileInfo exportExcelDSHDCTiet(JSONRoot jsonRoot) throws Exception;
	public FileInfo exportExceGeneral(JSONRoot jsonRoot) throws Exception;
}
