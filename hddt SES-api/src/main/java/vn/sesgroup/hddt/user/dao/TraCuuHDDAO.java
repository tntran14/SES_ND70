package vn.sesgroup.hddt.user.dao;

import java.util.HashMap;

import vn.sesgroup.hddt.dto.FileInfo;

public interface TraCuuHDDAO {
	public FileInfo printEInvoice(HashMap<String, String> mapInput) throws Exception;
}
