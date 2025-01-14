package vn.sesgroup.hddt.user.dao;

import com.api.message.JSONRoot;
import com.api.message.MsgRsp;

public interface EInvoiceMTTResponseCQTDAO {
	
	public MsgRsp list(JSONRoot jsonRoot) throws Exception;
	public MsgRsp detail(JSONRoot jsonRoot, String _id) throws Exception;
	public MsgRsp refreshStatusCQT(JSONRoot jsonRoot) throws Exception;
}
