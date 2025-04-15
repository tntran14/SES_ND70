package vn.sesgroup.hddt.user.dao;

import com.api.message.JSONRoot;
import com.api.message.MsgRsp;

public interface XuLyNghiepVuDAO {
	public MsgRsp getInvoiceByMTDiep(JSONRoot jsonRoot) throws Exception;
	public MsgRsp updateMCQT(JSONRoot jsonRoot) throws Exception;
}