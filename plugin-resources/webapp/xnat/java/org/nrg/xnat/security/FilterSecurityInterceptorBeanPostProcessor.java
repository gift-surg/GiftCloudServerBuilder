/**
 * FilterSecurityInterceptorBeanPostProcessor
 * (C) 2012 Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD License
 *
 * Created on 9/4/12 by rherri01
 */
package org.nrg.xnat.security;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nrg.xdat.om.ArcArchivespecification;
import org.nrg.xnat.turbine.utils.ArcSpecManager;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.security.access.ConfigAttribute;
import org.springframework.security.access.SecurityConfig;
import org.springframework.security.access.SecurityMetadataSource;
import org.springframework.security.web.access.expression.DefaultWebSecurityExpressionHandler;
import org.springframework.security.web.access.expression.ExpressionBasedFilterInvocationSecurityMetadataSource;
import org.springframework.security.web.access.expression.WebSecurityExpressionHandler;
import org.springframework.security.web.access.intercept.FilterSecurityInterceptor;
import org.springframework.security.web.access.intercept.RequestKey;
import org.springframework.security.web.util.AntUrlPathMatcher;
import org.springframework.security.web.util.UrlMatcher;

public class FilterSecurityInterceptorBeanPostProcessor implements BeanPostProcessor {
    public void setOpenUrls(List<String> openUrls) {
        _openUrls = openUrls;
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String name) throws BeansException {
        if (_log.isDebugEnabled()) {
            _log.debug("Post-processing bean: " + name);
        }

        if (bean instanceof FilterSecurityInterceptor) {
            FilterSecurityInterceptor interceptor = (FilterSecurityInterceptor) bean;
            final ExpressionBasedFilterInvocationSecurityMetadataSource metadataSource = getMetadataSource(isRequiredLogin());
            if (_log.isDebugEnabled()) {
                _log.debug("Found a FilterSecurityInterceptor bean with the following metadata configuration:");
                displayMetadataSource(interceptor.getSecurityMetadataSource());
                _log.debug("Updating the bean with the following metadata configuration:");
                displayMetadataSource(metadataSource);
            }
            interceptor.setSecurityMetadataSource(metadataSource);
        }

        return bean;
    }

    public ExpressionBasedFilterInvocationSecurityMetadataSource getMetadataSource(boolean requiredLogin) {
        UrlMatcher urlMatcher = new AntUrlPathMatcher();
        WebSecurityExpressionHandler handler = new DefaultWebSecurityExpressionHandler();
        LinkedHashMap<RequestKey, Collection<ConfigAttribute>> map = new LinkedHashMap<RequestKey, Collection<ConfigAttribute>>();

        for (String openUrl : _openUrls) {
            if (_log.isDebugEnabled()) {
                _log.debug("Setting permitAll on the open URL: " + openUrl);
            }

            map.put(new RequestKey(openUrl), SecurityConfig.createList(PERMIT_ALL));
        }

        final String nonopen = requiredLogin ? DEFAULT_EXPRESSION : PERMIT_ALL;
        if (_log.isDebugEnabled()) {
            _log.debug("Setting " + nonopen + " on the default pattern: " + DEFAULT_PATTERN);
        }
        map.put(new RequestKey(DEFAULT_PATTERN), SecurityConfig.createList(nonopen));
        return new ExpressionBasedFilterInvocationSecurityMetadataSource(urlMatcher, map, handler);
    }

    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
        return bean;
    }

    private synchronized void initializeArcSpecInstance() {
        if (_arcSpec == null) {
            _arcSpec = ArcSpecManager.GetInstance();
        }
    }

    private boolean isRequiredLogin() {
        // First check for null arcSpace, initialize if null.
        if (_arcSpec == null) {
            initializeArcSpecInstance();
        }
        // If it's STILL null, then arcSpec hasn't been initialized in the database, so just say false.
        if (_arcSpec == null) {
            return false;
        }
        // If it's not null, see what it's got to say.
        return _arcSpec.getRequireLogin();
    }

    private void displayMetadataSource(final SecurityMetadataSource metadataSource) {
        if (metadataSource != null) {
            _log.debug("Found metadata source configuration, now iterating.");
            for (ConfigAttribute attribute : metadataSource.getAllConfigAttributes()) {
                _log.debug("*** Attribute: " + attribute.getAttribute());
            }
        }
    }

    private static final Log _log = LogFactory.getLog(FilterSecurityInterceptorBeanPostProcessor.class);
    private static final String PERMIT_ALL = "permitAll";
    private static final String DEFAULT_PATTERN = "/**";
    private static final String DEFAULT_EXPRESSION = "hasRole('ROLE_USER')";

    private static ArcArchivespecification _arcSpec;

    private List<String> _openUrls;
}
