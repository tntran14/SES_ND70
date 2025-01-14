package vn.sesgroup.hddt.user.dao;

import com.api.message.JSONRoot;
import com.api.message.MsgRsp;

public interface UserCheckDAO {
	public MsgRsp save_user_check(JSONRoot jsonRoot) throws Exception;
	
}
