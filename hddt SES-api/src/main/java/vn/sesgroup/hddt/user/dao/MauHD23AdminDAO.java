package vn.sesgroup.hddt.user.dao;

import com.api.message.JSONRoot;
import com.api.message.MsgRsp;

public interface MauHD23AdminDAO {
	public MsgRsp crud(JSONRoot jsonRoot) throws Exception;
	public MsgRsp list(JSONRoot jsonRoot) throws Exception;
	public MsgRsp detail(JSONRoot jsonRoot, String _id) throws Exception;
	public MsgRsp checkdb(JSONRoot jsonRoot)throws Exception;
	public MsgRsp updatedb(JSONRoot jsonRoot)throws Exception;
	public MsgRsp updateDBTheoNam(JSONRoot jsonRoot)throws Exception;
}
