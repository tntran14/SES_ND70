package vn.sesgroup.hddt.user.dao;

import com.api.message.JSONRoot;

import vn.sesgroup.hddt.dto.FileInfo;

public interface CommonDAO {

	public FileInfo printEinvoiceAll(JSONRoot jsonRoot)throws Exception;
	public FileInfo printCttncnAll(JSONRoot jsonRoot)throws Exception;
	public FileInfo printEinvoiceAll1(JSONRoot jsonRoot)throws Exception;
	public FileInfo printCttncnAllV1(JSONRoot jsonRoot)throws Exception;
	public FileInfo printEinvoiceMTTAll(JSONRoot jsonRoot)throws Exception;


}
