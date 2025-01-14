package vn.sesgroup.hddt.user.dao;

import com.api.message.JSONRoot;

import vn.sesgroup.hddt.dto.FileInfo;

public interface CommonDAO {

	public FileInfo printEinvoiceAll(JSONRoot jsonRoot)throws Exception;
}
