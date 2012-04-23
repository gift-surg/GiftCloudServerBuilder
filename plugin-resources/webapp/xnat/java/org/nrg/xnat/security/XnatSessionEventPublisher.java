package org.nrg.xnat.security;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.nrg.framework.services.ContextService;
import org.nrg.xdat.XDAT;
import org.nrg.xdat.entities.XDATUserDetails;
import org.nrg.xft.XFTItem;
import org.springframework.context.ApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;

import org.springframework.security.web.session.HttpSessionCreatedEvent;
import org.springframework.security.web.session.HttpSessionDestroyedEvent;
import org.springframework.security.web.session.HttpSessionEventPublisher;
import org.springframework.web.context.support.WebApplicationContextUtils;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.http.HttpSessionEvent;
import javax.servlet.http.HttpSessionListener;


/**
 * This is very much a copy/paste of Spring Security's HttpSessionEventPublisher.
 * I needed it to accept the contextAttribute. I couldn't simply override that
 * class because the thing I needed to override is getContext(ServletContext) which 
 * is private and anyway it only has 1 method I wasn't going to override.
 */

/**
 * Declared in web.xml as
 * <pre>
 * &lt;listener&gt;
 *     &lt;listener-class&gt;org.springframework.security.web.session.HttpSessionEventPublisher&lt;/listener-class&gt;
 * &lt;/listener&gt;
 * </pre>
 *
 * Publishes <code>HttpSessionApplicationEvent</code>s to the Spring Root WebApplicationContext. Maps
 * javax.servlet.http.HttpSessionListener.sessionCreated() to {@link HttpSessionCreatedEvent}. Maps
 * javax.servlet.http.HttpSessionListener.sessionDestroyed() to {@link HttpSessionDestroyedEvent}.
 *
 * @author Ray Krueger
 */


public class XnatSessionEventPublisher implements HttpSessionListener, ServletContextListener{
    //~ Static fields/initializers =====================================================================================

    private String contextAttribute = null;

    private static final String LOGGER_NAME = XnatSessionEventPublisher.class.getName();
    //~ Methods ========================================================================================================

    ApplicationContext getContext(ServletContext servletContext) {
        return WebApplicationContextUtils.getWebApplicationContext(servletContext,contextAttribute);  // contextAttribute in xnat's case will always be "org.springframework.web.servlet.FrameworkServlet.CONTEXT.spring-mvc");
    }

    /**
     * Handles the HttpSessionEvent by publishing a {@link HttpSessionCreatedEvent} to the application
     * appContext.
     *
     * @param event HttpSessionEvent passed in by the container
     */
    public void sessionCreated(HttpSessionEvent event) {
        HttpSessionCreatedEvent e = new HttpSessionCreatedEvent(event.getSession());
        Log log = LogFactory.getLog(LOGGER_NAME);

        if (log.isDebugEnabled()) {
            log.debug("Publishing event: " + e);
        }
        
        getContext(event.getSession().getServletContext()).publishEvent(e);
    }

    /**
     * Handles the HttpSessionEvent by publishing a {@link HttpSessionDestroyedEvent} to the application
     * appContext.
     *
     * @param event The HttpSessionEvent pass in by the container
     */
    public void sessionDestroyed(HttpSessionEvent event) {
    	
    	String sessionId = event.getSession().getId();
        
    	Log log = LogFactory.getLog(LOGGER_NAME);
        
    	//Remember, you will often get a sessionID for anonymous or guest. skip those. right?
      	java.util.Date today = java.util.Calendar.getInstance(java.util.TimeZone.getDefault()).getTime();
     	try {
	      	JdbcTemplate template = new JdbcTemplate(XDAT.getDataSource());
	      	java.sql.Timestamp stamp = new java.sql.Timestamp(today.getTime());		
			template.execute("UPDATE xdat_user_login SET logout_date='" + stamp +"' WHERE session_id='" + sessionId + "';");
      	} catch (Exception e){
      		//remember, you get session ID's for guest sessions. Those won't be in the table. Fail silently.
      	}
        HttpSessionDestroyedEvent e = new HttpSessionDestroyedEvent(event.getSession());
        if (log.isDebugEnabled()) {
            log.debug("Publishing event: " + e);
        }
        getContext(event.getSession().getServletContext()).publishEvent(e);
    }

	@Override
	public void contextDestroyed(ServletContextEvent arg0) {
		//nothing to do here.
		return;
	}

	@Override
	public void contextInitialized(ServletContextEvent event) {
		ServletContext ctx=event.getServletContext();
		contextAttribute=ctx.getInitParameter("contextAttribute");
	}
}