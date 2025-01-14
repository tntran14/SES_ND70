package vn.sesgroup.hddt.configuration;

import javax.servlet.ServletContext;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.web.context.ServletContextAware;

import vn.sesgroup.hddt.resources.APIParams;
import vn.sesgroup.hddt.utils.SystemParams;

@Configuration
public class InitializationParameter implements InitializingBean, DisposableBean, ServletContextAware{
	private ServletContext context;
	@Autowired private Environment env;
	@Override
	public void setServletContext(ServletContext servletContext) {
		this.context = servletContext;
	}
	@Override
	public void destroy() throws Exception {
		System.out.println("hddt web admin shutting down...");
	}
	@Override
	public void afterPropertiesSet() throws Exception {
		System.out.println("Web Starting Initialization Parameters...");
		APIParams.HTTP_URIPDF = env.getProperty("api.hddt.uripdf", "");
		APIParams.HTTP_URI = env.getProperty("api.hddt.uri", "");
		APIParams.HTTP_LICENSEKEY = env.getProperty("api.hddt.license.key", "");
		
		SystemParams.DIR_TMP_SAVE_FILES = env.getProperty("dir.tmp.save.files", "");
		SystemParams.DIR_TEMPLATE_FILES = env.getProperty("dir.template.files", "");
		SystemParams.DIR_E_INVOICE_TEMPORARY = env.getProperty("dir.temporary", "");
		SystemParams.DIR_E_INVOICE_DATA = env.getProperty("dir.einvoice.data", "");
	}

}
