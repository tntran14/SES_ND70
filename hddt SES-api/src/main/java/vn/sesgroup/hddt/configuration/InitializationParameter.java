package vn.sesgroup.hddt.configuration;

import java.util.Arrays;
import java.util.Iterator;

import javax.servlet.ServletContext;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bson.Document;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.web.context.ServletContextAware;

import vn.sesgroup.hddt.resources.JmsParams;
import vn.sesgroup.hddt.utility.SystemParams;

@Configuration
public class InitializationParameter implements InitializingBean, DisposableBean, ServletContextAware{
	private static final Logger log = LogManager.getLogger(InitializationParameter.class);
	private ServletContext context;
	@Autowired private Environment env;
	@Autowired MongoTemplate mongoTemplate;
	
	@Override
	public void setServletContext(ServletContext servletContext) {
		this.context = servletContext;
	}

	@Override
	public void destroy() throws Exception {
		System.out.println("hddt-api shutting down...");
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		System.out.println("API: Starting Initialization Parameters...");
		SystemParams.VERSION = env.getProperty("api.version", "1.0");
		SystemParams.VERSION_XML = env.getProperty("version.xml", "2.0.0");
		SystemParams.VERSION_XML_PXK = env.getProperty("version.xml_pxk", "2.0.0");
		SystemParams.VERSION_XML_HDSS = env.getProperty("version.xml_hdss", "2.0.0");
		SystemParams.VERSION_XML_TOKHAI = env.getProperty("version.xml_tokhai", "2.0.0");
		SystemParams.DIR_E_INVOICE_DATA = env.getProperty("dir.einvoice.data", "");
		SystemParams.DIR_E_INVOICE_TKHAI = env.getProperty("dir.einvoice.tkhai", "");
		SystemParams.DIR_E_INVOICE_HDSS = env.getProperty("dir.einvoice.hdss", "");
		SystemParams.DIR_E_INVOICE_TEMPLATE = env.getProperty("dir.einvoice.template", "");
		SystemParams.DIR_TEMPORARY = env.getProperty("dir.temporary", "");
		
		SystemParams.MSTTCGP = env.getProperty("mngui.taxcode", "");
		SystemParams.MSTDVTN = env.getProperty("mnnhan.taxcode", "2.0.0");
	
		SystemParams.MaxViewPdf = env.getProperty("MaxViewPdf", "50");
		
		JmsParams.BROKER_URL = env.getProperty("jms.activemq.broker.url", "tcp://127.0.0.1:61616");
		JmsParams.BROKER_USERNAME = env.getProperty("jms.activemq.borker.username", "");
		JmsParams.BROKER_PASSWORD = env.getProperty("jms.activemq.borker.password", "");	    
		JmsParams.QUEUE_BULK_MAIL = env.getProperty("jms.activemq.borker.queue.bulk.mail", "QueueBulkMail");
		
		try {
			StringBuilder sb = new StringBuilder();
			
			Document docTmp = null;
			Iterable<Document> cursor = mongoTemplate.getCollection("ApiLicenseKey").find(new Document("ActiveFlag", true));
			Iterator<Document> iter = cursor.iterator();
			while(iter.hasNext()) {
				docTmp = iter.next();
				SystemParams.LIST_APILICENSEKEY.add(docTmp.get("LicenseKey", ""));
			}
			
			/*LAY DU LIEU TOKEN*/
			cursor = mongoTemplate.getCollection("SystemAccessToken").find();
			iter = cursor.iterator();
			while(iter.hasNext()) {
				docTmp = iter.next();
				
				sb.setLength(0);
				sb.append(docTmp.get("key", ""));
				switch (sb.toString()) {
				case "ACCESS_TOKEN_VISNAM":
					SystemParams.VISNAM_ACCESSTOKEN = docTmp.get("AccessToken", "");
					SystemParams.VISNAM_URL_GETTOKENXML = docTmp.getEmbedded(Arrays.asList("URLS", "GetTokenXML"), "");
					SystemParams.VISNAM_URL_TIEPNHANTHONGDIEP = docTmp.getEmbedded(Arrays.asList("URLS", "TiepNhanThongDiep"), "");
					SystemParams.VISNAM_URL_TRACUUTHONGDIEP = docTmp.getEmbedded(Arrays.asList("URLS", "TraCuuThongDiep"), "");
					break;
				default:
					break;
				}
			}
			
		}catch(Exception e) {
			log.error(" >>>>> An exception occurred!", e);
		}finally {
			
		}
		
	}

}
