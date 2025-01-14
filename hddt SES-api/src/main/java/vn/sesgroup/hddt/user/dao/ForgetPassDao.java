package vn.sesgroup.hddt.user.dao;


import com.api.message.JSONRoot;
import com.api.message.MsgRsp;


public interface ForgetPassDao {
	public MsgRsp crud(JSONRoot jsonRoot) throws Exception;
	public MsgRsp check_token(JSONRoot jsonRoot) throws Exception;
	public MsgRsp using_token(JSONRoot jsonRoot) throws Exception;
	public MsgRsp list(JSONRoot jsonRoot)throws Exception;
	public MsgRsp left(JSONRoot jsonRoot)throws Exception;
	public MsgRsp right(JSONRoot jsonRoot)throws Exception;
}
