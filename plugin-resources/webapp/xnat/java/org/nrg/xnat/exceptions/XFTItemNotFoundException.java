/*
 * org.nrg.xnat.exceptions.XFTItemNotFoundException
 * XNAT http://www.xnat.org
 * Copyright (c) 2014, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 *
 * Last modified 10/29/13 3:21 PM
 */
package org.nrg.xnat.exceptions;

import org.nrg.xdat.om.XdatUser;

public class XFTItemNotFoundException extends XNATException {

    public static final String XSI_TYPE = "XSI Type";
    public static final String OBJECT_ID = "Object ID";
    public static final String OBJECT_QUERY = "Query";

    public XFTItemNotFoundException() {
        super();
    }

    public XFTItemNotFoundException(final XdatUser user) {
        super(user);
    }

    public XFTItemNotFoundException(final String message, final XdatUser user) {
        super(message, user);
    }

    public String getId() {
        return (String) getParameter(OBJECT_ID);
    }

    public void setId(final String id) {
        setParameter(OBJECT_ID, id);
    }

    public String getXsiType() {
        return (String) getParameter(XSI_TYPE);
    }

    public void setXsiType(final String xsiType) {
        setParameter(XSI_TYPE, xsiType);
    }

    public String getQuery() {
        return (String) getParameter(OBJECT_QUERY);
    }

    public void setQuery(final String query) {
        setParameter(OBJECT_QUERY, query);
    }
}
