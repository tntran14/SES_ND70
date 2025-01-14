package vn.sesgroup.hddt.user.dao;

import java.util.List;

import vn.sesgroup.hddt.dto.MailConfig;

public interface SendMailAsyncDAO {

	void sendMailInvoice(MailConfig mailConfig, List<String> listIDSendMail)throws Exception;
}
