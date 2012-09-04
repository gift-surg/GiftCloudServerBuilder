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
            if (_log.isDebugEnabled()) {
                _log.debug("Found a FilterSecurityInterceptor bean, doing the needful");
                displayExistingMetadataSource(interceptor.getSecurityMetadataSource());
            }
            interceptor.setSecurityMetadataSource(getMetadataSource(isRequiredLogin()));
        }

        return bean;
    }

    public ExpressionBasedFilterInvocationSecurityMetadataSource getMetadataSource(boolean requiredLogin) {
        UrlMatcher urlMatcher = new AntUrlPathMatcher();
        WebSecurityExpressionHandler handler = new DefaultWebSecurityExpressionHandler();
        LinkedHashMap<RequestKey, Collection<ConfigAttribute>> map = new LinkedHashMap<RequestKey, Collection<ConfigAttribute>>();

        for (String openUrl : _openUrls) {
            map.put(new RequestKey(openUrl), SecurityConfig.createList(PERMIT_ALL));
        }

        map.put(new RequestKey(DEFAULT_PATTERN), SecurityConfig.createList(requiredLogin ? DEFAULT_EXPRESSION : PERMIT_ALL));
        return new ExpressionBasedFilterInvocationSecurityMetadataSource(urlMatcher, map, handler);
    }

    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
        return bean;
    }

    private ArcArchivespecification getArcSpecInstance() {
        if (_arcSpec == null) {
            initializeArcSpecInstance();
        }
        return _arcSpec;
    }

    private synchronized void initializeArcSpecInstance() {
        if (_arcSpec == null) {
            _arcSpec = ArcSpecManager.GetInstance();
        }
    }

    private boolean isRequiredLogin() {
        return getArcSpecInstance().getRequireLogin();
    }

    private void displayExistingMetadataSource(final SecurityMetadataSource metadataSource) {
        if (metadataSource != null) {
            _log.debug("Found existing configuration, now iterating.");

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
