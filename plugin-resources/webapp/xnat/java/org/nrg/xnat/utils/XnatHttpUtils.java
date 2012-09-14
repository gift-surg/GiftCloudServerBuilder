package org.nrg.xnat.utils;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;

import org.apache.turbine.util.RunData;

public class XnatHttpUtils {
	
	public static String getJSESSIONID(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (cookie.getName().equalsIgnoreCase("jsessionid")) {
                	return cookie.getValue();
                }
            }
        }
        throw new JSESSIONIDCookieNotFoundException();
	}

	public static String getJSESSIONID(RunData runData) {
		return getJSESSIONID(runData.getRequest());
	}
}
