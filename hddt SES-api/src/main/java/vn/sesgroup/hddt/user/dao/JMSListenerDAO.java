package vn.sesgroup.hddt.user.dao;

public interface JMSListenerDAO {
//	public void sendMailWithQueueBulkMail(HashMap<String, Object> hInput) throws Exception;
//	public void sendMailWithQueueBulkMail(ArrayList<String> _ids) throws Exception;
	public void sendMailWithQueueBulkMail(String infoServerID) throws Exception;
	public void sendMailWithQueueBulkMailOnCttncn(String infoServerID) throws Exception;
	public void createBBDCTT(String infoServerID, String collection, String issuerId, String userId) throws Exception;
}
