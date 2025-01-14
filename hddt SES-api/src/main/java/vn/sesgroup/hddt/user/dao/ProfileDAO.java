package vn.sesgroup.hddt.user.dao;

import com.api.message.JSONRoot;
import com.api.message.MsgRsp;

public interface ProfileDAO {

	public MsgRsp detail(JSONRoot jsonRoot, String _id) throws Exception;
	public MsgRsp issu(JSONRoot jsonRoot, String _id) throws Exception;
	MsgRsp crud(JSONRoot jsonRoot) throws Exception;
	MsgRsp infoConfig(JSONRoot jsonRoot) throws Exception;
	MsgRsp reset_issu(JSONRoot jsonRoot) throws Exception;

	
}
