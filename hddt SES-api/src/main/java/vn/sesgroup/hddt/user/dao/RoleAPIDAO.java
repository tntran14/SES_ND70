package vn.sesgroup.hddt.user.dao;

import com.api.message.JSONRoot;
import com.api.message.MsgRsp;

public interface RoleAPIDAO {
	public MsgRsp detail(JSONRoot jsonRoot, String _id) throws Exception;
	public MsgRsp list(JSONRoot jsonRoot) throws Exception;
	MsgRsp crud(JSONRoot jsonRoot) throws Exception;	
}
