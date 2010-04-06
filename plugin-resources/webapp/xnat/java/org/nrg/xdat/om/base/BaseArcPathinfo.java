// Copyright 2010 Washington University School of Medicine All Rights Reserved
/*
 * GENERATED FILE
 * Created on Tue Aug 07 11:23:27 CDT 2007
 *
 */
package org.nrg.xdat.om.base;
import org.nrg.xdat.om.base.auto.*;
import org.nrg.xft.*;
import org.nrg.xft.security.UserI;

import java.util.*;

/**
 * @author XDAT
 *
 */
@SuppressWarnings("serial")
public abstract class BaseArcPathinfo extends AutoArcPathinfo {

	public BaseArcPathinfo(ItemI item)
	{
		super(item);
	}

	public BaseArcPathinfo(UserI user)
	{
		super(user);
	}

	/*
	 * @deprecated Use BaseArcPathinfo(UserI user)
	 **/
	public BaseArcPathinfo()
	{}

	public BaseArcPathinfo(Hashtable properties, UserI user)
	{
		super(properties,user);
	}

    public String getArchivepath(){
        try{
            String path = super.getArchivepath();

            path = path.replace('\\', '/');
            if (!path.endsWith("/")){
                path = path +"/";
            }
            return path;
        } catch (Throwable e1) {logger.error(e1);return null;}
    }
    /**
     * @return Returns the prearchivePath.
     */
    public String getPrearchivepath(){
        try{
            String path = super.getPrearchivepath();

            path = path.replace('\\', '/');
            if (!path.endsWith("/")){
                path = path +"/";
            }
            return path;
        } catch (Throwable e1) {logger.error(e1);return null;}
    }

    /**
     * @return Returns the cachePath.
     */
    public String getCachepath(){
        try{
            String path = super.getCachepath();

            path = path.replace('\\', '/');
            if (!path.endsWith("/")){
                path = path +"/";
            }
            return path;
        } catch (Throwable e1) {logger.error(e1);return null;}
    }

    /**
     * @return Returns the buildPath.
     */
    public String getBuildpath(){
        try{
            String path = super.getBuildpath();

            path = path.replace('\\', '/');
            if (!path.endsWith("/")){
                path = path +"/";
            }
            return path;
        } catch (Throwable e1) {logger.error(e1);return null;}
    }
}
