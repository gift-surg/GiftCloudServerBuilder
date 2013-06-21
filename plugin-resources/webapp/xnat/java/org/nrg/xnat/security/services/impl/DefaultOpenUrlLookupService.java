/**
 * OpenUrlLookupService
 * (C) 2013 Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD License
 *
 * Created on 6/21/13 by rherri01
 */
package org.nrg.xnat.security.services.impl;

import com.google.common.collect.ImmutableSet;
import org.apache.commons.lang.StringUtils;
import org.nrg.xnat.restlet.util.RequestUtil;
import org.nrg.xnat.security.services.OpenUrlLookupService;
import org.restlet.data.Request;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.web.util.AntUrlPathMatcher;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Set;

/**
 * OpenUrlLookupService class.
 *
 * @author rherri01
 */
@Service
public class DefaultOpenUrlLookupService implements OpenUrlLookupService {

    @Override
    public Set<String> getOpenUrls() {
        return _openUrlPatterns;
    }

    @Resource(name = "openUrls")
    public void setOpenUrls(List<String> openUrls) {
        _openUrlPatterns = ImmutableSet.copyOf(openUrls);
    }

    @Override
    public boolean isOpenUrl(final Request request) {
        return isOpenUrl(_requestUtil.getHttpServletRequest(request));
    }

    @Override
    public boolean isOpenUrl(final ServletRequest request) {
        if (!(request instanceof HttpServletRequest)) {
            if (_log.isInfoEnabled()) {
                _log.info("Got a weird request object of type: {}, can't process this" + request.getClass().getName());
            }
            return false;
        }
        HttpServletRequest servletRequest = (HttpServletRequest) request;
        final String strippedUri = servletRequest.getRequestURI().substring(servletRequest.getContextPath().length());
        return isOpenUrl(strippedUri);
    }

    @Override
    public boolean isOpenUrl(final String strippedUri) {
        if (StringUtils.isEmpty(strippedUri)) {
            if (_log.isDebugEnabled()) {
                _log.debug("Found an empty URI, can't call that open");
            }
            return false;
        }
        if (_log.isDebugEnabled()) {
            _log.debug("Processing the URI to check for open URI: {}", strippedUri);
        }
        for (String openUrlPattern : _openUrlPatterns) {
            if (_matcher.pathMatchesUrl(openUrlPattern, strippedUri)) {
                if (_log.isDebugEnabled()) {
                    _log.debug("Found an open URI {} matching the pattern {}", strippedUri, openUrlPattern);
                }
                return true;
            }
        }
        if (_log.isDebugEnabled()) {
            _log.debug("Didn't find an open URI match for the URI {}", strippedUri);
        }
        return false;
    }

    private static final Logger _log = LoggerFactory.getLogger(DefaultOpenUrlLookupService.class);
    private final RequestUtil _requestUtil = new RequestUtil();
    private final AntUrlPathMatcher _matcher = new AntUrlPathMatcher();
    private Set<String> _openUrlPatterns;
}
