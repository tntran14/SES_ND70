package vn.sesgroup;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.annotation.PropertySources;

@Configuration
@PropertySources({ 
	@PropertySource("classpath:application.properties")
	, @PropertySource("classpath:mongodb.properties")
	,	@PropertySource("classpath:activemq.properties") 
})
//@ComponentScan(basePackageClasses = Application.class)
@ComponentScan(basePackages = {"vn.nhatrovn"})
public class ApplicationConfig {

}
