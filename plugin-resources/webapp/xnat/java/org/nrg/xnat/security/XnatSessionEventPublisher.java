/*
 * org.nrg.xnat.security.XnatSessionEventPublisher
 * XNAT http://www.xnat.org
 * Copyright (c) 2014, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 *
 * Last modified 7/10/13 9:04 PM
 */
package org.nrg.xnat.security;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nrg.xdat.XDAT;
import org.nrg.xdat.security.XDATUser;
import org.nrg.xnat.restlet.resources.SecureResource;
import org.springframework.context.ApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;

import org.springframework.security.web.session.HttpSessionCreatedEvent;
import org.springframework.security.web.session.HttpSessionDestroyedEvent;
import org.springframework.web.context.support.WebApplicationContextUtils;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.http.HttpSessionEvent;
import javax.servlet.http.HttpSessionListener;

public class XnatSessionEventPublisher implements HttpSessionListener,
		ServletContextListener {
	// ~ Static fields/initializers
	// =====================================================================================

	private String contextAttribute = null;

	private static final String LOGGER_NAME = XnatSessionEventPublisher.class
			.getName();

	// ~ Methods
	// ========================================================================================================

	ApplicationContext getContext(ServletContext servletContext) {
		return WebApplicationContextUtils.getWebApplicationContext(
				servletContext, contextAttribute); // contextAttribute in xnat's
													// case will always be
													// "org.springframework.web.servlet.FrameworkServlet.CONTEXT.spring-mvc");
	}

	/**
	 * Handles the HttpSessionEvent by publishing a
	 * {@link HttpSessionCreatedEvent} to the application appContext.
	 *
	 * @param event
	 *            HttpSessionEvent passed in by the container
	 */
	public void sessionCreated(HttpSessionEvent event) {
		HttpSessionCreatedEvent e = new HttpSessionCreatedEvent(
				event.getSession());
		Log log = LogFactory.getLog(LOGGER_NAME);

		if (log.isDebugEnabled()) {
			log.debug("Publishing event: " + e);
		}

		getContext(event.getSession().getServletContext()).publishEvent(e);
	}

	/**
	 * Handles the HttpSessionEvent by publishing a
	 * {@link HttpSessionDestroyedEvent} to the application appContext.
	 *
	 * @param event
	 *            The HttpSessionEvent pass in by the container
	 */
	public void sessionDestroyed(HttpSessionEvent event) {

		String sessionId = event.getSession().getId();

		Log log = LogFactory.getLog(LOGGER_NAME);

		java.util.Date today = java.util.Calendar.getInstance(
				java.util.TimeZone.getDefault()).getTime();
		try {

			XDATUser user = (XDATUser) event.getSession().getAttribute(
					SecureResource.USER_ATTRIBUTE);
			if (user != null) {
				String userId = user.getID().toString();
				JdbcTemplate template = new JdbcTemplate(XDAT.getDataSource());
				java.sql.Timestamp stamp = new java.sql.Timestamp(
						today.getTime());
				// sessionId's aren't guaranteed to be unique forever. But, the
				// likelyhood of sessionId and userId not forming a unique combo
				// with a null logout_date is slim.
				template.execute("UPDATE xdat_user_login SET logout_date='"
						+ stamp
						+ "' WHERE logout_date is null and session_id='"
						+ sessionId + "' and user_xdat_user_id='" + userId
						+ "';");
			}
		} catch (Exception e) {
			// remember, anonymous gets a session, too. Those won't be in the
			// table. Fail silently.
		}
		HttpSessionDestroyedEvent e = new HttpSessionDestroyedEvent(
				event.getSession());
		if (log.isDebugEnabled()) {
			log.debug("Publishing event: " + e);
		}
		getContext(event.getSession().getServletContext()).publishEvent(e);
	}

	@Override
	public void contextDestroyed(ServletContextEvent arg0) {
		// nothing to do here.
		return;
	}

	@Override
	public void contextInitialized(ServletContextEvent event) {
		ServletContext ctx = event.getServletContext();
		contextAttribute = ctx.getInitParameter("contextAttribute");
	}
}