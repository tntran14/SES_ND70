package vn.sesgroup.hddt.user.dao;

import com.api.message.JSONRoot;
import com.api.message.MsgRsp;

public interface PHSLMauHDAdminDAO {
	public MsgRsp getMSHD(JSONRoot jsonRoot, String taxCode) throws Exception;
	public MsgRsp detail(JSONRoot jsonRoot, String _id) throws Exception;
	public MsgRsp list(JSONRoot jsonRoot) throws Exception;
	MsgRsp crud(JSONRoot jsonRoot) throws Exception;	
}
