package vn.sesgroup.hddt.user.dao;

import vn.sesgroup.hddt.user.dto.LoginRes;
import vn.sesgroup.hddt.user.dto.UserLoginReq;

public interface LoginDAO {
	public LoginRes doAuth(UserLoginReq req) throws Exception;
}
