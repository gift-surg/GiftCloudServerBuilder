package org.nrg.xnat.restlet.util;

import java.io.IOException;
import java.net.CookieManager;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletResponse;

public class UpdateExpirationCookie implements Filter {
	public static String name = "SESSION_EXPIRATION_TIME";
	public static Cookie makeCookie (int sessionIdleTime) {
		long start = System.currentTimeMillis();
		Long expirationTimeMillis = Long.valueOf(start + (sessionIdleTime * 1000));
		Cookie c = new Cookie(UpdateExpirationCookie.name, expirationTimeMillis.toString()); 
		c.setPath("/");
		return c;
	}
	@Override
	public void destroy() {}
	@Override
	public void doFilter(ServletRequest req, 
						   ServletResponse resp,
						   FilterChain chain) throws IOException, ServletException {
		HttpServletRequest hq = (HttpServletRequest) req;
		HttpServletResponse hr = (HttpServletResponse) resp;
		int sessionIdleTime = hq.getSession().getMaxInactiveInterval();
		Cookie cookie = UpdateExpirationCookie.makeCookie(sessionIdleTime);
		hr.addCookie(cookie);
		chain.doFilter(req,resp);
	}
	@Override
	public void init(FilterConfig fg) throws ServletException {}
}
