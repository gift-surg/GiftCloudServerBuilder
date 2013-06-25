/**
 * OpenUrlLookupService
 * (C) 2013 Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD License
 *
 * Created on 6/21/13 by rherri01
 */
package org.nrg.xnat.security.services;

import org.restlet.data.Request;

import javax.servlet.ServletRequest;
import java.util.Set;

/**
 * OpenUrlLookupService interface.
 *
 * @author rherri01
 */
public interface OpenUrlLookupService {
    public abstract Set<String> getOpenUrls();
    public abstract boolean isOpenUrl(Request request);
    public abstract boolean isOpenUrl(ServletRequest request);
    public abstract boolean isOpenUrl(String strippedUri);
}
