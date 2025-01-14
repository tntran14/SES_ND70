package vn.sesgroup.hddt.controller.user;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Locale;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.context.WebApplicationContext;

import vn.sesgroup.hddt.controller.AbstractController;
import vn.sesgroup.hddt.resources.RestAPIUtility;
import vn.sesgroup.hddt.utils.Commons;
import vn.sesgroup.hddt.utils.Constants;
import vn.sesgroup.hddt.utils.SystemParams;

@Controller
@Scope(value = WebApplicationContext.SCOPE_REQUEST)
@RequestMapping("/InvoiceUtility")
public class DownloadFileForSignMulti extends AbstractController{
	@Autowired RestAPIUtility restAPI;
	private static final Logger log = LogManager.getLogger(DownloadFileForSignMulti.class);
	private Commons commons = new Commons();

	@RequestMapping(
			value = "/downloadFileForSignMulti/{t1}" , method = RequestMethod.GET
		)
		public void execDownloadFileForSignMulti(
				Locale locale
				, HttpServletRequest req
				, HttpServletResponse resp
				, HttpSession session
				, @PathVariable(name = "t1", required = false) String t1			//TAXCODE
				) throws Exception{
			PrintWriter writer = null;
			 String[] words = t1.split(",");
			 String taxcode = words[0];
			 String tg =  words[1];
			 String token = words[2];
			/*KIEM TRA THONG TIN*/
			LocalDateTime now = LocalDateTime.now();
			LocalDateTime time = null;
			try {
				time = commons.formatStringToLocalDateTime(tg, Constants.FORMAT_DATE.FORMAT_DATETIME_DB_FULL);
			}catch(Exception e) {}
			long diffTime = ChronoUnit.SECONDS.between(time, now);
			if(Math.abs(diffTime) > 600 
			/* || !t1.equals(cup.getLoginRes().getCompany().getTaxCode()) */) {
				resp.setContentType("text/html; charset=utf-8");
		        resp.setCharacterEncoding("UTF-8");
		        resp.setHeader("success", "yes");
		        resp.setStatus(HttpStatus.FORBIDDEN.value());
		        writer = resp.getWriter();
		        writer.close();
		        return;
			}
			
			Path path = Paths.get(SystemParams.DIR_TMP_SAVE_FILES);
			String urlPath = path.toString();
			String zipFile =  token + ".zip";
			
			File file = new File(urlPath, zipFile);
			if(!file.exists() || !file.isFile()) {
				resp.setContentType("text/html; charset=utf-8");
		        resp.setCharacterEncoding("UTF-8");
		        resp.setHeader("success", "yes");
		        resp.setStatus(HttpStatus.NOT_FOUND.value());
		        writer = resp.getWriter();
		        writer.write("Tập tin không tồn tại.");
		        writer.close();
		        return;
			}
			
			InputStream is = new FileInputStream(file);
			resp.setContentType("application/force-download");
		    resp.setHeader("Content-Disposition", "attachment; filename=" + zipFile + "");
		    int read=0;
		    byte[] bytes = new byte[SystemParams.BUFFER_SIZE];
		    OutputStream os = resp.getOutputStream();

		    while((read = is.read(bytes))!= -1){
		        os.write(bytes, 0, read);
		    }
		    os.flush();
		    os.close(); 
			is.close();
			return;
		}	
	
	
	
}

