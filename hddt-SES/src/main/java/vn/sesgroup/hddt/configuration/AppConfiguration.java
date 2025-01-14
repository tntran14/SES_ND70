package vn.sesgroup.hddt.configuration;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.FileSystemResource;
import org.springframework.format.FormatterRegistry;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.converter.ByteArrayHttpMessageConverter;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.validation.MessageCodesResolver;
import org.springframework.validation.Validator;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.HandlerMethodReturnValueHandler;
import org.springframework.web.multipart.commons.CommonsMultipartResolver;
import org.springframework.web.servlet.HandlerExceptionResolver;
import org.springframework.web.servlet.ViewResolver;
import org.springframework.web.servlet.config.annotation.AsyncSupportConfigurer;
import org.springframework.web.servlet.config.annotation.ContentNegotiationConfigurer;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.DefaultServletHandlerConfigurer;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.PathMatchConfigurer;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.ViewResolverRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter;
import org.springframework.web.servlet.view.BeanNameViewResolver;
import org.springframework.web.servlet.view.json.MappingJackson2JsonView;
import org.thymeleaf.spring5.SpringTemplateEngine;
import org.thymeleaf.spring5.view.ThymeleafViewResolver;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.ObjectMapper;

import nz.net.ultraq.thymeleaf.LayoutDialect;
import vn.sesgroup.hddt.interceptor.AllowAccessFunctionInterceptor;
import vn.sesgroup.hddt.interceptor.CheckRightsInterceptor;
import vn.sesgroup.hddt.interceptor.LogInterceptor;
import vn.sesgroup.hddt.interceptor.LoginInterceptor;
import vn.sesgroup.hddt.interceptor.NoCacheHeaderInterceptor;

//import nz.net.ultraq.thymeleaf.layoutdialect.LayoutDialect;

@Configuration
@EnableWebMvc
@EnableAsync
@ComponentScan("vn.sesgroup.hddt")
public class AppConfiguration implements WebMvcConfigurer {
	@Autowired ApplicationContext applicationContext;
	
	

	
	@Bean
	public ClassLoaderTemplateResolver templateResolver() {
		ClassLoaderTemplateResolver templateResolver = new ClassLoaderTemplateResolver();
		templateResolver.setPrefix("hddt/views/");
		templateResolver.setSuffix(".html");
		templateResolver.setTemplateMode(TemplateMode.HTML);
		templateResolver.setCacheable(false);
		templateResolver.setCharacterEncoding("UTF-8");
		return templateResolver;
	}
	
	@Bean
	public SpringTemplateEngine templateEngine() {
		SpringTemplateEngine templateEngine = new SpringTemplateEngine();
		templateEngine.setTemplateResolver(templateResolver());
		templateEngine.addDialect(new LayoutDialect());
		templateEngine.setEnableSpringELCompiler(true);
		return templateEngine;
	}

	@Bean
	public ViewResolver viewResolver() {
		ThymeleafViewResolver viewResolver = new ThymeleafViewResolver();
		viewResolver.setCharacterEncoding("UTF-8");
		viewResolver.setTemplateEngine(templateEngine());
		viewResolver.setOrder(1);
		return viewResolver;
	}
	
	@Bean
	public BeanNameViewResolver beanNameViewResolver(){
		BeanNameViewResolver view = new BeanNameViewResolver();
		view.setOrder(2);
		return view;
	}
	
	@Bean(name = "jsonView")
	public MappingJackson2JsonView jsonView(){
		MappingJackson2JsonView viewResolver = new MappingJackson2JsonView();
		return viewResolver;
	}
	
	@Bean
	public MappingJackson2HttpMessageConverter mappingJackson2HttpMessageConverter(){
		MappingJackson2HttpMessageConverter converter = new MappingJackson2HttpMessageConverter();
		ObjectMapper mapper = new ObjectMapper();
		mapper.setSerializationInclusion(Include.NON_NULL);
		mapper.setSerializationInclusion(Include.NON_EMPTY);
		converter.setObjectMapper(mapper);
		return converter;
	}
	
	private List<MediaType> getSupportedMediaTypes(){
		List<MediaType> list = new ArrayList<>();
		list.add(MediaType.IMAGE_JPEG);
		list.add(MediaType.IMAGE_PNG);
		list.add(MediaType.valueOf("image/ico"));
		list.add(MediaType.APPLICATION_OCTET_STREAM);
//		list.add(new MediaType("text", "html", Charset.forName("UTF-8")));
		list.add(new MediaType(MediaType.TEXT_PLAIN, Charset.forName("UTF-8")));
//		list.add(new MediaType("text/html;charset=UTF-8"));
		list.add(new MediaType(MediaType.TEXT_HTML, Charset.forName("UTF-8")));
		return list;
	}
	
	@Bean
	public ByteArrayHttpMessageConverter byteArrayHttpMessageConverter(){
		ByteArrayHttpMessageConverter converter = new ByteArrayHttpMessageConverter();
		converter.setDefaultCharset(Charset.forName("UTF-8"));
		converter.setSupportedMediaTypes(getSupportedMediaTypes());
		return converter;
	}
	
	@Bean
	public StringHttpMessageConverter stringHttpMessageConverter(){
		StringHttpMessageConverter converter = new StringHttpMessageConverter(Charset.forName("UTF-8"));
		converter.setSupportedMediaTypes(
				Arrays.asList(
						new MediaType("text", "plain", Charset.forName("UTF-8")),
						new MediaType("text", "html", Charset.forName("UTF-8"))
						)
				);
		converter.setWriteAcceptCharset(false);
		return converter;
	}
	
	@Bean(name = "filterMultipartResolver")
	public CommonsMultipartResolver createMultipartResolver() throws IOException{
		CommonsMultipartResolver resolver = new CommonsMultipartResolver();
		resolver.setDefaultEncoding("utf-8");
		resolver.setMaxUploadSizePerFile(50 * 1024 * 1024);//50MB	~~ 5 * 1024 * 1024
		resolver.setMaxUploadSize(1024*1024*50*5);		//250MB
		resolver.setMaxInMemorySize(1048576);
		resolver.setUploadTempDir(new FileSystemResource("/tmp"));
		
		resolver.setResolveLazily(true);			//exception to @ControllerAdvice
		return resolver;
	}
	
	@Autowired
    private RequestMappingHandlerAdapter requestMappingHandlerAdapter;
	
	@PostConstruct
    public void init() {
		System.out.println("Initialization Web....");
		requestMappingHandlerAdapter.setIgnoreDefaultModelOnRedirect(true);
    }
	
	@Override
	public void configurePathMatch(PathMatchConfigurer configurer) {
		// TODO Auto-generated method stub
		WebMvcConfigurer.super.configurePathMatch(configurer);
	}

	@Override
	public void configureContentNegotiation(ContentNegotiationConfigurer configurer) {
		// TODO Auto-generated method stub
		WebMvcConfigurer.super.configureContentNegotiation(configurer);
	}

	@Override
	public void configureAsyncSupport(AsyncSupportConfigurer configurer) {
		// TODO Auto-generated method stub
		WebMvcConfigurer.super.configureAsyncSupport(configurer);
	}

	@Override
	public void configureDefaultServletHandling(DefaultServletHandlerConfigurer configurer) {
		// TODO Auto-generated method stub
		WebMvcConfigurer.super.configureDefaultServletHandling(configurer);
	}

	@Override
	public void addFormatters(FormatterRegistry registry) {
		// TODO Auto-generated method stub
		WebMvcConfigurer.super.addFormatters(registry);
	}

	@Autowired NoCacheHeaderInterceptor noCacheHeaderInterceptor;
	@Autowired LoginInterceptor loginInterceptor;
	@Autowired AllowAccessFunctionInterceptor allowAccessFunctionInterceptor;
	@Autowired CheckRightsInterceptor checkRightsInterceptor;
	@Autowired LogInterceptor logInterceptor;
	
	@Override
	public void addInterceptors(InterceptorRegistry registry) {		
		registry.addInterceptor(noCacheHeaderInterceptor).excludePathPatterns("/static/**", "/favicon.ico");
		registry.addInterceptor(loginInterceptor).addPathPatterns("/", "/login", "/index");
//		
		List<String> pathChecks = Arrays.asList(
				"/static/**", "/favicon.ico", "/", "/login", "/index"
				, "/error", "/error**"
				, "/authenticate", "/logout"
				, "/common/**"
				, "/main", "/main/**" 
				, "/tracuuhd", "/tracuuhd/**" 
				, "/InvoiceUtility/**"
				, "/forgotpass", "/forgotpass/**" 
				,"/taxcode","/taxcode/**"
				, "/issu-contract", "/issu-contract/**" 
				,"/support", "/support/**"
				, "/ContractExpies", "/ContractExpies/**" 
				,"/logEmail","/logEmail/**"
			);
		registry.addInterceptor(allowAccessFunctionInterceptor).excludePathPatterns(pathChecks);
		registry.addInterceptor(checkRightsInterceptor).excludePathPatterns(pathChecks);
		registry.addInterceptor(logInterceptor).excludePathPatterns(
			Arrays.asList(
				"/static/**", "/favicon.ico"
			)
		);
		
	}

	@Override
	public void addResourceHandlers(ResourceHandlerRegistry registry) {
		registry.addResourceHandler("/static/**").addResourceLocations("classpath:/static/").setCacheControl(CacheControl.maxAge(365, TimeUnit.DAYS));
		registry.addResourceHandler("/static/function/**").addResourceLocations("classpath:/hddt/js/").setCacheControl(CacheControl.maxAge(365, TimeUnit.DAYS));
	}

	@Override
	public void addCorsMappings(CorsRegistry registry) {
// WebMvcConfigurer.super.addCorsMappings(registry); 
		  registry.addMapping("/**")
          .allowedOrigins("*")
          .allowedMethods("POST","GET","OPTIONS")
          .allowedHeaders("*")
          .allowCredentials(false).maxAge(3600);
	}

	@Override
	public void addViewControllers(ViewControllerRegistry registry) {
		// TODO Auto-generated method stub
		WebMvcConfigurer.super.addViewControllers(registry);
	}

	@Override
	public void configureViewResolvers(ViewResolverRegistry registry) {
		// TODO Auto-generated method stub
		WebMvcConfigurer.super.configureViewResolvers(registry);
	}

	@Override
	public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
		// TODO Auto-generated method stub
		WebMvcConfigurer.super.addArgumentResolvers(resolvers);
	}

	@Override
	public void addReturnValueHandlers(List<HandlerMethodReturnValueHandler> handlers) {
		// TODO Auto-generated method stub
		WebMvcConfigurer.super.addReturnValueHandlers(handlers);
	}

	@Override
	public void configureMessageConverters(List<HttpMessageConverter<?>> converters) {
		converters.add(mappingJackson2HttpMessageConverter());
		converters.add(byteArrayHttpMessageConverter());
		converters.add(stringHttpMessageConverter());
	}

	@Override
	public void extendMessageConverters(List<HttpMessageConverter<?>> converters) {
		// TODO Auto-generated method stub
		WebMvcConfigurer.super.extendMessageConverters(converters);
	}

	@Override
	public void configureHandlerExceptionResolvers(List<HandlerExceptionResolver> resolvers) {
		// TODO Auto-generated method stub
		WebMvcConfigurer.super.configureHandlerExceptionResolvers(resolvers);
	}

	@Override
	public void extendHandlerExceptionResolvers(List<HandlerExceptionResolver> resolvers) {
		// TODO Auto-generated method stub
		WebMvcConfigurer.super.extendHandlerExceptionResolvers(resolvers);
	}

	@Override
	public Validator getValidator() {
		// TODO Auto-generated method stub
		return WebMvcConfigurer.super.getValidator();
	}

	@Override
	public MessageCodesResolver getMessageCodesResolver() {
		// TODO Auto-generated method stub
		return WebMvcConfigurer.super.getMessageCodesResolver();
	}

	@Override
	public int hashCode() {
		// TODO Auto-generated method stub
		return super.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		// TODO Auto-generated method stub
		return super.equals(obj);
	}

	@Override
	protected Object clone() throws CloneNotSupportedException {
		// TODO Auto-generated method stub
		return super.clone();
	}

	@Override
	public String toString() {
		// TODO Auto-generated method stub
		return super.toString();
	}

	@Override
	protected void finalize() throws Throwable {
		// TODO Auto-generated method stub
		super.finalize();
	}
	
}
