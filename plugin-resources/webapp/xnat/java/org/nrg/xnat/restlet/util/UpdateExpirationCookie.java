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

	static class CookieTuple {
		final boolean flag;
		final int maxIdleTime;
		CookieTuple(Cookie c, int maxIdleTime) throws NumberFormatException, IOException {
			this.flag = parseFlag(c);
			this.maxIdleTime = maxIdleTime;
		}
		boolean parseFlag(Cookie c) throws IOException {
			String[] values = c.getValue().split(",");
			int numberFlag = Integer.parseInt(values[0]);
			if (numberFlag == 0) {
				return false;
			}
			else if (numberFlag == 1) {
				return true;
			}
			else {
				throw new IOException("Expecting the first element of the tuple to be 0 or 1");
			}
		}
		
		static String createCookieTuple (boolean flag, int maxIdleTime) {
			String flagString = flag ? "1" : "0";
			return (flagString + "," + Integer.toString(maxIdleTime * 1000));
 		}
		
		Cookie makeCookie () {
			Cookie c = new Cookie(UpdateExpirationCookie.name, CookieTuple.createCookieTuple(!this.flag, this.maxIdleTime));
			c.setPath("/");
			return c;
		}
		
		static Cookie defaultCookie (int maxIdleTime) {
			Cookie c = new Cookie(UpdateExpirationCookie.name, CookieTuple.createCookieTuple(false, maxIdleTime));
			c.setPath("/");
			return c;
		} 
	}
	
	private Cookie getRequestExpirationCookie (HttpServletRequest hq) {
		Cookie[] requestCookies = hq.getCookies();
		Cookie ret = null;
		for (int i = 0; i < requestCookies.length; i++) {
			Cookie c = requestCookies[i];
			if (c.getName().equals(UpdateExpirationCookie.name)) {
				ret = c;
			}
		}
		return ret;
	}
	
	@Override
	public void destroy() {}
	@Override
	public void doFilter(ServletRequest req, 
						   ServletResponse resp,
						   FilterChain chain) throws IOException, ServletException {
		HttpServletRequest hq = (HttpServletRequest) req;
		HttpServletResponse hr = (HttpServletResponse) resp;
		Cookie oldCookie = getRequestExpirationCookie(hq);
		int sessionIdleTime = hq.getSession().getMaxInactiveInterval();
		CookieTuple cp = null;
		Cookie c = null;
		if (oldCookie == null) {
			c = CookieTuple.defaultCookie(sessionIdleTime);
		}
		else {
			try {
				cp = new CookieTuple(oldCookie, sessionIdleTime);
				c = cp.makeCookie();
			}
			catch (Exception e) {
				c = CookieTuple.defaultCookie(sessionIdleTime);
			}
		}
		hr.addCookie(c);
		chain.doFilter(req,resp);
	}
	
	@Override
	public void init(FilterConfig fg) throws ServletException {}
}
