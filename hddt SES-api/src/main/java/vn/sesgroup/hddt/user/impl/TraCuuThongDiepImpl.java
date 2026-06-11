package vn.sesgroup.hddt.user.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import com.api.message.MsgRsp;
import com.api.message.MspResponseStatus;

import vn.sesgroup.hddt.user.dao.AbstractDAO;
import vn.sesgroup.hddt.user.dao.TraCuuThongDiepDAO;
import vn.sesgroup.hddt.user.service.TCTNService;
import vn.sesgroup.hddt.utility.Commons;

@Repository
public class TraCuuThongDiepImpl extends AbstractDAO implements TraCuuThongDiepDAO {
	@Autowired
	TCTNService tctnService;
	
	public Commons commons = new Commons();
	
	@Override
	public MsgRsp tracuuthongdiep(String mtdiep) throws Exception {
		MsgRsp rsp = new MsgRsp();
		if ("".equals(mtdiep.trim())) {
			rsp.setResponseStatus(new MspResponseStatus(999, "MTDiep rỗng!!!."));
			return rsp;
		}
		org.w3c.dom.Document rTCTN = tctnService.callTraCuuThongDiep(mtdiep);
		String res = commons.docW3cToString(rTCTN);
		rsp.setObjData(res);
		rsp.setResponseStatus(new MspResponseStatus(0, "Tra cứu thông điệp thành công!!!."));
		return rsp;
	}
}
