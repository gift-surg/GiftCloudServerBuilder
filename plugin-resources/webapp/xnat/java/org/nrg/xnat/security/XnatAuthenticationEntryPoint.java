/**
 * XnatAuthenticationEntryPoint
 * (C) 2012 Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD License
 *
 * Created on 10/4/12 by rherri01
 */
package org.nrg.xnat.security;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.util.AntUrlPathMatcher;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Handles distinguishing calls to data paths. Data paths are URIs, usually for REST services, that may be called by
 * automated agents to retrieve data, but may also be called by the user interface to populate user controls. These two
 * cases require different responses when unauthenticated requests are received:
 * <p/>
 * <ul>
 * <li>When the request comes from a browser, the response should have a status of HTTP 302 redirecting the browser
 * to the application login page.</li>
 * <li>When the request comes from a data agent, such as the XnatDataClient, curl, or other command-line
 * application, the response should have a status of HTTP 401 indicating the request was not authenticated.</li>
 * </ul>
 * <p/>
 * To determine is a request is a data path, this class performs Ant-style pattern matching of the request URI against
 * the list of data paths configured via the {@link #setDataPaths(java.util.List)} method.
 * <p/>
 * To determine is a user agent is interactive (e.g. a browser), this class performs a regular-expression match against
 * the user-agent specifiers set using the {@link #setInteractiveAgents(java.util.List)} method. These are specified as
 * Java-style regular expressions.
 */
public class XnatAuthenticationEntryPoint extends LoginUrlAuthenticationEntryPoint {
    /**
     * Overrides {@link LoginUrlAuthenticationEntryPoint#commence(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse, org.springframework.security.core.AuthenticationException)}
     * to test for data path and user agent. If this request is for a data path by an non-interactive agent, the
     * response status is set to HTTP 302, i.e. unauthorized. Otherwise the base implementation is used, which redirects
     * the request to the configured login page.
     *
     * @param request       HTTP request object.
     * @param response      HTTP response object.
     * @param authException An authentication exception that may have redirected the agent to re-authenticate.
     * @throws IOException
     * @throws ServletException
     */
    @Override
    public void commence(final HttpServletRequest request, final HttpServletResponse response, final AuthenticationException authException) throws IOException, ServletException {
        final String strippedUri = request.getRequestURI().substring(request.getContextPath().length());
        final String userAgent = request.getHeader("User-Agent");

        if (_log.isDebugEnabled()) {
            _log.debug("Evaluating data path request: " + strippedUri + ", user agent: " + userAgent);
            }

        if(strippedUri!=null && strippedUri.indexOf("/action/AcceptProjectAccess/par/")>-1){
        	int index=strippedUri.indexOf("/par/")+5;
        	if(strippedUri.length()>index){//par number included?
        		String parS=strippedUri.substring(index);
        		if(parS.indexOf("/")>-1){
        			parS=parS.substring(0,parS.indexOf("/"));
        		}
        		
        		request.getSession().setAttribute("par", parS);
        	}
        }
        
        if (isDataPath(strippedUri) && !isInteractiveAgent(userAgent)) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }

            super.commence(request, response, authException);
        }

    /**
     * Sets the data paths, i.e. those paths which require a user-agent interactivity test to determine whether the user
     * should be denied as unauthorized or redirected to the login page. Each data path should be a valid Ant-style
     * pattern matching the URL(s) to be designated as data paths.
     *
     * @param dataPaths A list of strings in Ant-style patterns indicating data paths.
     */
    public void setDataPaths(final List<String> dataPaths) {
        if (_log.isDebugEnabled()) {
            _log.debug("Adding " + dataPaths + " data paths");
    }

        for (final String dataPath : dataPaths) {
            if (_log.isDebugEnabled()) {
                _log.debug("Adding data path: " + dataPath);
            }
            _dataPaths.add(_pathMatcher.compile(dataPath));
        }
    }

    /**
     * Sets the list of interactive agents to redirect to the login page even on data paths.
     *
     * @param interactiveAgents A list of interactive agents to be directed to the login page even on data paths.
     */
    public void setInteractiveAgents(final List<String> interactiveAgents) {
        for (final String interactiveAgent : interactiveAgents) {
            if (_log.isDebugEnabled()) {
                _log.debug("Adding interactive agent specifier: " + interactiveAgent);
}
            final Pattern pattern = Pattern.compile(interactiveAgent);
            _agentPatterns.add(pattern);
        }
    }

    private boolean isDataPath(final String strippedUri) {
        if (_log.isDebugEnabled()) {
            _log.debug("Testing URI as data path: " + strippedUri);
        }
        for (final Object dataPath : _dataPaths) {
            if (_pathMatcher.pathMatchesUrl(dataPath, strippedUri)) {
                if (_log.isDebugEnabled()) {
                    _log.debug("URI " + strippedUri + "is a data path.");
                }
                return true;
            }
        }
        if (_log.isDebugEnabled()) {
            _log.debug("URI " + strippedUri + "is not a data path.");
        }
        return false;
    }

    private boolean isInteractiveAgent(final String userAgent) {
        if (_log.isDebugEnabled()) {
            _log.debug("Testing user agent as interactive: " + userAgent);
        }
        if (!StringUtils.isBlank(userAgent)) {
            for (Pattern interactiveAgent : _agentPatterns) {
                if (interactiveAgent.matcher(userAgent).matches()) {
                    if (_log.isDebugEnabled()) {
                        _log.debug("User agent " + userAgent + " is interactive, matched simple regex pattern: " + interactiveAgent);
                    }
                    return true;
                }
            }
        }
        if (_log.isDebugEnabled()) {
            _log.debug("User agent " + userAgent + " is not interactive, failed to match any patterns");
        }
        return false;
    }

    private static final Log _log = LogFactory.getLog(XnatAuthenticationEntryPoint.class);

    private final AntUrlPathMatcher _pathMatcher = new AntUrlPathMatcher();
    private final List<Object> _dataPaths = new ArrayList<Object>();
    private final List<Pattern> _agentPatterns = new ArrayList<Pattern>();
}
