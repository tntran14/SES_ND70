package vn.sesgroup.hddt.filter;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.security.SignatureException;
import vn.sesgroup.hddt.dto.User;
import vn.sesgroup.hddt.utility.Constants;

public class AuthenticationFilter extends OncePerRequestFilter{
	private static final Logger log = LogManager.getLogger(AuthenticationFilter.class);
	
	private static List<String> excludedUrls = new ArrayList<String>() {
		private static final long serialVersionUID = -4405970029806745758L;
		{
			add("/version");
			add("/auth");
			add("/tracuuhd/print-einvoice");
			add("/taxcode/get-taxcode");
			add("/forgotpass/**");
			add("/issu-contract/**");
			add("/support/**");
			add("/ContractExpies/**");
			add("/logEmail/**");
		}
	};
	
	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {
		response.addHeader("Access-Control-Allow-Origin", "*");
		response.addHeader("Access-Control-Allow-Methods", "*");
		response.addHeader("Access-Control-Max-Age", "3600");
		response.addHeader("Access-Control-Allow-Credentials", "true");		//allow cookie
		response.addHeader("Access-Control-Expose-Headers", "Content-Length");
		response.addHeader("Access-Control-Allow-Headers", "x-requested-with, Content-Type, Accept-Encoding, Accept-Language, Cookie, Referer, Authorization, Accept");
//		response.setHeader("Access-Control-Allow-Headers", "x-requested-with");
//		response.setHeader("Access-Control-Allow-Headers", "Content-Type, x-requested-with, Authorization, refresh-token");

		String servletPath = request.getServletPath();
		if(excludedUrls.contains(request.getServletPath())
//				|| servletPath.startsWith("/Commons/downloadFileImageForMail")
//				|| servletPath.startsWith("/einvoice")
//				|| servletPath.startsWith("/guest-sign-file")
//				|| servletPath.startsWith("/guest-img")
				) { 
			filterChain.doFilter(request, response);
			return;
		}

		final String authHeader = request.getHeader(Constants.TOKEN_HEADER);
		
		if (authHeader != null) {
			String token = authHeader;	/*.substring(7);*/
			try {
//				String base64Key = Base64.getEncoder().encodeToString(Constants.JWT_SECRET.getBytes());
//	        	Jws<Claims> parsedToken = Jwts.parser()
//	                    .setSigningKey(base64Key)
////	                    .parseClaimsJws(token);
//	                    .parseClaimsJws(token.replaceAll("\\s", "").replace(Constants.TOKEN_PREFIX, ""));
				Jws<Claims> parsedToken = Jwts.parserBuilder().setSigningKey(Constants.JWT_SECRET.getBytes()).build().parseClaimsJws(token.replaceAll("\\s", "").replace(Constants.TOKEN_PREFIX, ""));
	        	
	        	Claims claims = parsedToken.getBody();
	        	
	        	String userName = claims.get("data02").toString();
//	        	Boolean enabled = Boolean.valueOf(true);
	        	List<String> roles = new ArrayList<>();
	        	roles.add("ADMIN");
	        	
//	        	Commons commons = new Commons();
//	    		String serverMacAddress = null == claims.get("ServerMacAddress")? "": claims.get("ServerMacAddress").toString();
//	    		String remoteAddress = null == claims.get("RemoteAddress")? "": claims.get("RemoteAddress").toString();
	        	
//	        	String sessionApi = claims.get("Pswd").toString();
	        	/*KIEM TRA SESSION API*/
	        	User user = new User(userName, null, true, roles); 
	        	UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());
	        	authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
	        	SecurityContextHolder.getContext().setAuthentication(authentication);
			}catch (ExpiredJwtException exception) {
                log.error("Request to parse expired JWT : {} failed : {}", token, exception.getMessage());
//				throw exception;
                response.setContentType("text/html;charset=UTF-8");
        		response.setCharacterEncoding("UTF-8");
        		response.setHeader("Cache-Control", "no-cache");
        		response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        		PrintWriter pw = response.getWriter();					
				pw.print("Token đang sử dụng đã hết thời gian hiệu lực.");
				pw.flush();
				pw.close();
				return;
            } catch (UnsupportedJwtException exception) {
                log.error("Request to parse unsupported JWT : {} failed : {}", token, exception.getMessage());
            } catch (MalformedJwtException exception) {
                log.error("Request to parse invalid JWT : {} failed : {}", token, exception.getMessage());
            } catch (SignatureException exception) {
                log.error("Request to parse JWT with invalid signature : {} failed : {}", token, exception.getMessage());
            } catch (IllegalArgumentException exception) {
                log.error("Request to parse empty or null JWT : {} failed : {}", token, exception.getMessage());
            }
		}
		if (!request.getMethod().equalsIgnoreCase("OPTIONS")) {
			filterChain.doFilter(request, response);
		}
	}
}
