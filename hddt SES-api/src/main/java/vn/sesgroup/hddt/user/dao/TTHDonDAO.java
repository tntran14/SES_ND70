package vn.sesgroup.hddt.user.dao;

import com.api.message.JSONRoot;
import com.api.message.MsgRsp;

public interface TTHDonDAO {
	public MsgRsp check(JSONRoot jsonRoot) throws Exception;
	public MsgRsp crud(JSONRoot jsonRoot) throws Exception;
}
