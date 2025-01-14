package vn.sesgroup.hddt.scheduling;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import vn.sesgroup.hddt.user.dao.ScheduledTasksDAO;

//http://www.quartz-scheduler.org/documentation/quartz-2.3.0/tutorials/crontrigger.html
@Component
public class ScheduledTasks {
	@Autowired private ScheduledTasksDAO dao;
	
	/*GET TOKEN MOI 1,9,17 GIO*/
//	@Scheduled(cron = "0/30 * * * * ?")
//	@Scheduled(cron = "0 0 1,9,17 * * ?")
	@Scheduled(cron = "0 7 7,19 * * ?")
	public void getAccessTokenVISNAM() throws Exception{
		dao.getAccessTokenVISNAM();
	}
	
	/*GUI DU LIEU DEN TCTN MOI 10 GIAY*/
//	@Scheduled(cron = "0/10 * * * * ?")
//	public void setTotalRoomEmptyByDate() throws Exception{
////		dao.callSignEInvoiceToTCTN();
//	}
}
