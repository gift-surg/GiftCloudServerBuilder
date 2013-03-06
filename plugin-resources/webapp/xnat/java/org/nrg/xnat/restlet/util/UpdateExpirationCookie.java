package org.nrg.xnat.restlet.util;

import java.io.IOException;
import java.util.Date;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class UpdateExpirationCookie implements Filter {
	public static String name = "SESSION_EXPIRATION_TIME";
	

	@Override
	public void destroy() {}
	@Override
	public void doFilter(ServletRequest req, 
						   ServletResponse resp,
						   FilterChain chain) throws IOException, ServletException {
		HttpServletRequest hq = (HttpServletRequest) req;
		HttpServletResponse hr = (HttpServletResponse) resp;
		int sessionIdleTime = hq.getSession().getMaxInactiveInterval();

		Cookie c=new Cookie(name, ""+(new Date()).getTime()+","+((sessionIdleTime *1000))); 
		c.setPath("/");
		hr.addCookie(c);
		
		chain.doFilter(req,resp);
	}
	
	@Override
	public void init(FilterConfig fg) throws ServletException {}
}
