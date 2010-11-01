//Copyright 2007 Washington University School of Medicine All Rights Reserved
/*
 * Created on Sep 26, 2007
 *
 */
package org.nrg.xnat.turbine.utils;

import java.util.Hashtable;

import org.nrg.xdat.bean.CatCatalogBean;

public class CatalogSet {
    public CatCatalogBean catalog = null;
    public Hashtable<String,Object> hash = null;
    
    public CatalogSet(CatCatalogBean c, Hashtable<String,Object> h){
        catalog=c;
        hash=h;
    }
}
