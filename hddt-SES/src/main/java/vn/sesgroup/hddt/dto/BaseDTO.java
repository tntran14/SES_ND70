package vn.sesgroup.hddt.dto;

import java.io.Serializable;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Optional;
import java.util.Random;

import javax.servlet.http.HttpServletRequest;

import com.api.message.Msg;
import com.api.message.MsgHeader;
import com.api.message.MsgPage;
import com.api.message.UserType;
import com.fasterxml.jackson.annotation.JsonIgnore;

import vn.sesgroup.hddt.utils.Constants;

public class BaseDTO implements Serializable{
	private static final long serialVersionUID = -4247898242169016648L;

	@JsonIgnore public transient HttpServletRequest req;
	@JsonIgnore private transient CurrentUserProfile cup;
	
	private int total;
	private int errorCode;
	private String errorDesc;
	private ArrayList<String> errorMessages = new ArrayList<>();
	private Object responseData;
	
	public int getTotal() {
		return total;
	}
	public void setTotal(int total) {
		this.total = total;
	}
	public int getErrorCode() {
		return errorCode;
	}
	public void setErrorCode(int errorCode) {
		this.errorCode = errorCode;
	}
	public String getErrorDesc() {
		return errorDesc;
	}
	public void setErrorDesc(String errorDesc) {
		this.errorDesc = errorDesc;
	}
	public ArrayList<String> getErrorMessages() {
		return errorMessages;
	}
	public void setErrorMessages(ArrayList<String> errorMessages) {
		this.errorMessages = errorMessages;
	}
	public Object getResponseData() {
		return responseData;
	}
	public void setResponseData(Object responseData) {
		this.responseData = responseData;
	}

	public BaseDTO() {
		this.setErrorCode(0);
	}
	
	public BaseDTO(int errorCode, Object responseData) {
		this.errorCode = errorCode;
		this.responseData = responseData;
	}
	
	public BaseDTO(HttpServletRequest req) {
		this.req = req;
	}
	
	public MsgPage createPage() {
		MsgPage page = new MsgPage();
		try {			
			String tmpPage = null == req.getParameter("page")? "0": req.getParameter("page");
	    	String tmpRows = null == req.getParameter("pageSize")? "0": req.getParameter("pageSize");
	    	String tmpOrder = null == req.getParameter("sort[0][field]")? "": req.getParameter("sort[0][field]");
	    	String tmpSort = null == req.getParameter("sort[0][dir]")? "": req.getParameter("sort[0][dir]");
	    	
	    	page.setSize(stringToInteger(tmpRows));
	    	page.setPageNo(stringToInteger(tmpPage));
	    	page.setFieldSort(tmpOrder);
	    	page.setTypeSort("asc".equals(tmpSort)? 1: -1);	//1: TANG DAN; -1: GIAM DAN
	    	
		} catch (Exception e) {
		}
		return page;
	}
	
	public Msg createMsgMain(CurrentUserProfile cup, String actionCode) {
		Msg msg = new Msg();	
		if(null != cup)
			this.cup = cup;
		
		msg.setMsgHeader(this.createHeaderMain(actionCode));
		msg.setMsgPage(this.createPageMain());
		return msg;
	}
	
	public MsgPage createPageMain() {
		MsgPage page = new MsgPage();
		try {			
	    	String tmpOrder = "";
	    	String tmpSort = "-1";
	    
	    	page.setSize(10);
	    	page.setPageNo(1);
	    	page.setFieldSort(tmpOrder);
	    	page.setTypeSort(stringToInteger(tmpSort));	//1: TANG DAN; -1: GIAM DAN
	    	
		} catch (Exception e) {
		}
		return page;
	}
	
	public MsgHeader createHeaderMain(String actionCode) {
		MsgHeader header = new MsgHeader();
		header.setType(UserType.ADMIN);

		LoginRes loginRes = cup.getLoginRes();
		header.setUserId(loginRes.getUserId());
		header.setUserName(loginRes.getUserName());
		header.setUserFullName(loginRes.getFullName());
		header.setIssuerId(loginRes.getIssuerId());
		header.setRoot(loginRes.isRoot());
		header.setActionCode(actionCode);
		header.setRqId(csRandomAlphaNumbericString(30));
		
		return header;
	}
	
	public MsgHeader createHeader(String actionCode) {
		MsgHeader header = new MsgHeader();
		header.setType(UserType.ADMIN);

		LoginRes loginRes = cup.getLoginRes();
		header.setUserId(loginRes.getUserId());
		header.setUserName(loginRes.getUserName());
		header.setUserFullName(loginRes.getFullName());
		header.setIssuerId(loginRes.getIssuerId());
		header.setRoot(loginRes.isRoot());
		
		header.setIpAddress(req.getRemoteAddr());
		header.setActionCode(actionCode);
		header.setRqId(csRandomAlphaNumbericString(30));
		
		return header;
	}
	
	public Msg createMsg(CurrentUserProfile cup, String actionCode) {
		Msg msg = new Msg();	
		if(null != cup)
			this.cup = cup;
		
		msg.setMsgHeader(this.createHeader(actionCode));
		msg.setMsgPage(this.createPage());
		return msg;
	}
	public Msg createMsgPass() {
		Msg msg = new Msg();	
		msg.setMsgPage(this.createPage());
		return msg;
	}
	public Integer stringToInteger(String input) {
//		Integer value = Optional.of(input).map(Integer::valueOf).get();
		try {
			Integer value = Optional.ofNullable(input).map(Integer::valueOf).orElse(0);
			return value;
		}catch(Exception e) {
//			e.printStackTrace();
			return 0;
		}
	}
	
	
	// cs = cryptographically secure
	public String csRandomAlphaNumbericString(int numChars) {
		char buff[] = new char[numChars];
		SecureRandom srand = new SecureRandom();
		Random rand = new Random();
		for (int i = 0; i < numChars; i++) {
			if ((i % 10) == 0) {
				rand.setSeed(srand.nextLong()); // // 64 bits of random!
			}
			buff[i] = Constants.VALID_CHARACTERS[rand.nextInt(Constants.VALID_CHARACTERS.length)];
		}
		return new String(buff);
	}
	public void setCup(CurrentUserProfile cup) {
		this.cup = cup;
	}

	
}
