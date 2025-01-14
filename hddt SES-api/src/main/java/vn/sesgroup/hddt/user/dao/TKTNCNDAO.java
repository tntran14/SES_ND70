package vn.sesgroup.hddt.user.dao;

import com.api.message.JSONRoot;

import vn.sesgroup.hddt.dto.FileInfo;

public interface TKTNCNDAO {

	public FileInfo viewReport(JSONRoot jsonRoot)throws Exception;
}
