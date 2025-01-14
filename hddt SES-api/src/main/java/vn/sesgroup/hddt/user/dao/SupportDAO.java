package vn.sesgroup.hddt.user.dao;

import com.api.message.JSONRoot;
import com.api.message.MsgRsp;

public interface SupportDAO {
	public MsgRsp getList(JSONRoot jsonRoot) throws Exception;
}
