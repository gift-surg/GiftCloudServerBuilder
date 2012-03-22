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
	static class RemoveCookieFromRequest extends HttpServletRequestWrapper{
		private final int sessionIdleTime;
		public RemoveCookieFromRequest (HttpServletRequest req, int sessionIdleTime) {
			super(req);
			this.sessionIdleTime = sessionIdleTime;
		}
		public Cookie[] getCookies() {
			Cookie[] cookies = super.getCookies();
			List<Cookie> newCookies = new ArrayList<Cookie>();
			// cookies only come from a HTTP session
			if (cookies != null) {
				for (int i =0; i < cookies.length; i++) {
					Cookie c = cookies[i];
					if (c.getName().equals(UpdateExpirationCookie.name)){
						Cookie d = UpdateExpirationCookie.makeCookie(sessionIdleTime);
						c = d;
					}
				}
			}
			return newCookies.toArray(new Cookie[newCookies.size()]);
		}
	}
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
		RemoveCookieFromRequest rq = new RemoveCookieFromRequest(hq, sessionIdleTime);
		Cookie[] cookies = rq.getCookies();
//		for (int i = 0; i < cookies.length; i++) {
//			hr.addCookie(cookies[i]);
//		}
		hr.addCookie(cookie);
		chain.doFilter(req,resp);
	}
	@Override
	public void init(FilterConfig fg) throws ServletException {}
}
