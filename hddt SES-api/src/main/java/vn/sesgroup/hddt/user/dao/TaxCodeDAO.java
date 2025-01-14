package vn.sesgroup.hddt.user.dao;

import java.util.HashMap;

import vn.sesgroup.hddt.dto.FileInfo;

public interface TaxCodeDAO {
	public FileInfo getTaxCode(HashMap<String, String> mapInput) throws Exception;
}
