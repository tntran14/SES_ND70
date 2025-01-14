package vn.sesgroup.hddt.user.dao;

import com.api.message.JSONRoot;
import com.api.message.MsgRsp;

public interface IssuContractDao {
	public MsgRsp list(JSONRoot jsonRoot) throws Exception;
	public MsgRsp crud(JSONRoot jsonRoot) throws Exception;
	public MsgRsp detail(JSONRoot jsonRoot, String _id)throws Exception;
	public MsgRsp checkdb(JSONRoot jsonRoot)throws Exception;
	public MsgRsp updatedb(JSONRoot jsonRoot)throws Exception;
	public MsgRsp listcks(JSONRoot jsonRoot) throws Exception;
	public MsgRsp listnguoimua(JSONRoot jsonRoot) throws Exception;
}
