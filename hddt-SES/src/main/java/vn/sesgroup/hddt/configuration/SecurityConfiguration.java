package vn.sesgroup.hddt.configuration;

import java.util.regex.Pattern;

import javax.servlet.http.HttpServletRequest;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.security.web.csrf.CsrfTokenRepository;
import org.springframework.security.web.csrf.HttpSessionCsrfTokenRepository;
import org.springframework.security.web.firewall.HttpFirewall;
import org.springframework.security.web.firewall.StrictHttpFirewall;
import org.springframework.security.web.session.SessionManagementFilter;
import org.springframework.security.web.util.matcher.RequestMatcher;

import vn.sesgroup.authentication.entry.point.CustomAuthenticationEntryPoint;
import vn.sesgroup.hddt.exception.MyAccessDeniedHandler;
import vn.sesgroup.hddt.filter.CORSFilter;
import vn.sesgroup.hddt.filter.XsrfFilter;

@Configuration
@EnableWebSecurity
@Order(1)
public class SecurityConfiguration extends WebSecurityConfigurerAdapter{
	
	@Override
	protected void configure(HttpSecurity http) throws Exception {
		http
			.logout()
				.deleteCookies("JSESSIONID")
				.logoutUrl("/logout")
				.logoutSuccessHandler((req, res, auth) -> {
					res.sendRedirect("/login?logout");
				})
				.permitAll()
			.and()
				.authorizeRequests()
					.antMatchers(
						"/static", "/static/**", "/favicon.ico"
						, "/", "/login", "/index", "/authenticate"
						, "/common/generatingCaptcha"
						, "/common/get-file-to-sign/**"
						, "/common/**"
						, "/tracuuhd", "/tracuuhd/**"
						, "/InvoiceUtility/**"
						, "/forgotpass", "/forgotpass/**"
						,"/taxcode","/taxcode/**"
						, "/issu-contract", "/issu-contract/**" 
						,"/support", "/support/**"
						, "/ContractExpies", "/ContractExpies/**" 
						,"/logEmail", "/logEmail/**"
					).permitAll()
					.antMatchers("/**").authenticated()
			.and()
				.exceptionHandling()
					.accessDeniedHandler(new MyAccessDeniedHandler())
					.authenticationEntryPoint(new CustomAuthenticationEntryPoint())
				.and()
					.headers()
						.frameOptions().disable();
			
		http.addFilterAfter(new CORSFilter(), BasicAuthenticationFilter.class)
			.addFilterAfter(new XsrfFilter(), SessionManagementFilter.class);
		
		http.csrf().csrfTokenRepository(csrfTokenRepository()).requireCsrfProtectionMatcher(csrfRequestMatcher);
	}
	
	RequestMatcher csrfRequestMatcher = new RequestMatcher() {
		private Pattern allowedMethods = Pattern.compile("^GET$");

		@Override
		public boolean matches(HttpServletRequest request) {
			if (allowedMethods.matcher(request.getMethod()).matches()) {
				return false;
			}
			
			return true;
		}
		
	};
	
	private CsrfTokenRepository csrfTokenRepository() {
		HttpSessionCsrfTokenRepository repository = new HttpSessionCsrfTokenRepository();
		repository.setHeaderName("X-CSRF-TOKEN");
		return repository;
	}
	
	@Bean
	public HttpFirewall allowUrlEncodedSlashHttpFirewall() {
	    StrictHttpFirewall firewall = new StrictHttpFirewall();
	    firewall.setAllowUrlEncodedSlash(true);    
	    return firewall;
	}
	
	@Override
	public void configure(WebSecurity web) throws Exception {
	    //@formatter:off
//	    web.ignoring().antMatchers("/generalError", "/forbidden", "/popup-error", "/error-area", "/error**");
		web.ignoring().antMatchers("/common**", "/error**");
	    web.httpFirewall(allowUrlEncodedSlashHttpFirewall());
	}
}
