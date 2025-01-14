package vn.sesgroup.hddt.user.dao;

import com.api.message.JSONRoot;
import com.api.message.MsgRsp;

public interface ChangeMTDiepDAO {
	public MsgRsp list(JSONRoot jsonRoot) throws Exception;
	public MsgRsp change(JSONRoot jsonRoot, String _id)throws Exception;
}
