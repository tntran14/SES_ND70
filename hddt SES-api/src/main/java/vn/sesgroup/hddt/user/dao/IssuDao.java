package vn.sesgroup.hddt.user.dao;

import com.api.message.JSONRoot;
import com.api.message.MsgRsp;

public interface IssuDao {
	public MsgRsp list(JSONRoot jsonRoot) throws Exception;
	public MsgRsp crud(JSONRoot jsonRoot) throws Exception;
	public MsgRsp param(JSONRoot jsonRoot, String _id);
	public MsgRsp mskh(JSONRoot jsonRoot, String _id)throws Exception;

}
