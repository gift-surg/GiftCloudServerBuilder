/*
 * (C) 2011 Washington University School of Medicine
 * All Rights Reserved
 * Released under the Simplified BSD License
 */

/**
 * XnatRestlet
 * Created on 11/8/11 by rherri01
 */
package org.nrg.xnat.restlet;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * This indicates that a class is an XNAT restlet. The annotation value
 * must be specified and indicates the URL path (relative to the RESTlet
 * root path) for the RESTful service. Duplicating an already configured
 * URL path will raise an exception.
 */
@Documented
@Target(TYPE)
@Retention(RUNTIME)
public @interface XnatRestlet {
    String[] value();
    boolean required() default false;
    boolean secure() default true;
}
