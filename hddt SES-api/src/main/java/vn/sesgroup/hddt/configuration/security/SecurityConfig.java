package vn.sesgroup.hddt.configuration.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.firewall.HttpFirewall;
import org.springframework.security.web.firewall.StrictHttpFirewall;

import vn.sesgroup.hddt.filter.AuthenticationFilter;
import vn.sesgroup.hddt.filter.CheckApiLicenseKeyFilter;

@Configuration
@EnableWebSecurity
@EnableGlobalMethodSecurity(prePostEnabled = true)
public class SecurityConfig extends WebSecurityConfigurerAdapter{
	
	@Bean
    public AuthenticationFilter authenticationFilterBean() throws Exception {
        return new AuthenticationFilter();
    }
	
	@Override
	protected void configure(HttpSecurity http) throws Exception {
		http
			.csrf().disable()
			.sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS)
			.and()
				.authorizeRequests()
				.antMatchers(
						"/", "/version"
						,"/auth"
						,"/tracuuhd/print-einvoice"
						,"/forgotpass/**"
						,"/taxcode/get-taxcode"
						,"/issu-contract/list"
						,"/issu-contract/listcks"
						,"/issu-contract/listnguoimua"
						,"/support/getList"
						,"/ContractExpies/list"
						,"/logEmail/list"
						,"/getAndHandleInvoiceVAT/handleVAT"
						,"/mauso-expires/list"
						,"/ca_invoice/list"
						
						
					).permitAll()
					.anyRequest().authenticated()
			.and()
				.exceptionHandling()
					.authenticationEntryPoint(new RestAuthenticationEntryPoint());
		
		http.addFilterBefore(new CheckApiLicenseKeyFilter(), UsernamePasswordAuthenticationFilter.class);
		// custom JWT based security filter
		http.addFilterBefore(authenticationFilterBean(), UsernamePasswordAuthenticationFilter.class);
		// disable page caching
		http.headers().cacheControl();
	}
	
	@Bean
	public HttpFirewall allowUrlEncodedSlashHttpFirewall() {
		StrictHttpFirewall firewall = new StrictHttpFirewall();
		firewall.setAllowUrlEncodedSlash(true);
		return firewall;
	}
	
}
