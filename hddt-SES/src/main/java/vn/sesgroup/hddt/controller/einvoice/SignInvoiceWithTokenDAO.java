package vn.sesgroup.hddt.controller.einvoice;

import java.util.List;

public interface SignInvoiceWithTokenDAO {

	ResponseBaseCheckSignListInvoiceDTO checkSignListInvoice(List<String> ids);

}

