/**
 * TranslatingChannelProcessingFilter
 * (C) 2012 Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD License
 *
 * Created on 9/4/12 by rherri01
 */
package org.nrg.xnat.security;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.security.access.ConfigAttribute;
import org.springframework.security.config.http.ChannelAttributeFactory;
import org.springframework.security.web.access.channel.ChannelProcessingFilter;
import org.springframework.security.web.access.intercept.DefaultFilterInvocationSecurityMetadataSource;
import org.springframework.security.web.access.intercept.FilterInvocationSecurityMetadataSource;
import org.springframework.security.web.access.intercept.RequestKey;
import org.springframework.security.web.util.AntUrlPathMatcher;
import org.springframework.security.web.util.UrlMatcher;

import java.util.Collection;
import java.util.LinkedHashMap;

public class TranslatingChannelProcessingFilter extends ChannelProcessingFilter {
    public void setRequiredChannel(String requiredChannel) {
        if (_log.isDebugEnabled()) {
            _log.debug("Setting the default pattern required channel to: " + requiredChannel);
        }

        UrlMatcher urlMatcher = new AntUrlPathMatcher();
        LinkedHashMap<RequestKey, Collection<ConfigAttribute>> map = new LinkedHashMap<RequestKey, Collection<ConfigAttribute>>();
        map.put(new RequestKey("/**"), ChannelAttributeFactory.createChannelAttributes(requiredChannel));
        FilterInvocationSecurityMetadataSource metadataSource = new DefaultFilterInvocationSecurityMetadataSource(urlMatcher, map);
        setSecurityMetadataSource(metadataSource);
    }

    private static final Log _log = LogFactory.getLog(TranslatingChannelProcessingFilter.class);
}
