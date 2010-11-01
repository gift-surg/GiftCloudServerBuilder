// Copyright 2010 Washington University School of Medicine All Rights Reserved
/*
 * GENERATED FILE
 * Created on Wed Aug 08 11:56:15 CDT 2007
 *
 */
package org.nrg.xdat.om.base;
import java.util.Hashtable;
import java.util.List;

import org.nrg.xdat.model.ArcPathinfoI;
import org.nrg.xdat.model.ArcProjectI;
import org.nrg.xdat.om.ArcProject;
import org.nrg.xdat.om.base.auto.AutoArcArchivespecification;
import org.nrg.xft.ItemI;
import org.nrg.xft.security.UserI;

/**
 * @author XDAT
 *
 */
@SuppressWarnings({"unchecked","rawtypes"})
public abstract class BaseArcArchivespecification extends AutoArcArchivespecification {

	public BaseArcArchivespecification(ItemI item)
	{
		super(item);
	}

	public BaseArcArchivespecification(UserI user)
	{
		super(user);
	}

	/*
	 * @deprecated Use BaseArcArchivespecification(UserI user)
	 **/
	public BaseArcArchivespecification()
	{}

	public BaseArcArchivespecification(Hashtable properties, UserI user)
	{
		super(properties,user);
	}

    public  String getGlobalArchivePath(){
        String path = null;
        ArcPathinfoI pathInfo= this.getGlobalpaths();
        if (pathInfo!=null){
            path=pathInfo.getArchivepath();
        }
        if (path==null){
            path =".";
        }
        path = path.replace('\\', '/');
        if (!path.endsWith("/")){
            path = path +"/";
        }
        return path;
    }

    public String getGlobalPrearchivePath(){
        String path = null;
            ArcPathinfoI pathInfo= this.getGlobalpaths();
            if (pathInfo!=null){
                path=pathInfo.getPrearchivepath();
            }
        if (path==null){
            path =".";
        }
        path = path.replace('\\', '/');
        if (!path.endsWith("/")){
            path = path +"/";
        }
        return path;
    }

    public String getGlobalCachePath(){
        String path = null;
            ArcPathinfoI pathInfo= this.getGlobalpaths();
            if (pathInfo!=null){
                path=pathInfo.getCachepath();
            }
        if (path==null){
            path =".";
        }
        path = path.replace('\\', '/');
        if (!path.endsWith("/")){
            path = path +"/";
        }
        return path;
    }

    public  String getGlobalBuildPath(){
        String path = null;
            ArcPathinfoI pathInfo= this.getGlobalpaths();
            if (pathInfo!=null){
                path=pathInfo.getBuildpath();
            }
        if (path==null){
            path =".";
        }
        path = path.replace('\\', '/');
        if (!path.endsWith("/")){
            path = path +"/";
        }
        return path;
    }

    public String getArchivePathForProject(String id){
        String path = null;
            List<ArcProjectI> projects=this.getProjects_project();
            for (ArcProjectI p : projects){
                if (p.getId().equals(id)){
                    ArcPathinfoI pathInfo= p.getPaths();
                    if (pathInfo!=null){
                        path=pathInfo.getArchivepath();
                    }
                    break;
                }
            }
            if (path==null || path.trim().equals("")){
                ArcPathinfoI pathInfo= this.getGlobalpaths();
                if (pathInfo!=null){
                    path=pathInfo.getArchivepath();
                }
            }
        if (path==null){
            path =".";
        }
        path = path.replace('\\', '/');
        if (!path.endsWith("/")){
            path = path +"/";
        }
        return path;
    }

    public String getCachePathForProject(String id){
        String path = null;
            List<ArcProjectI> projects=this.getProjects_project();
            for (ArcProjectI p : projects){
                if (p.getId().equals(id)){
                    ArcPathinfoI pathInfo= p.getPaths();
                    if (pathInfo!=null){
                        path=pathInfo.getCachepath();
                    }
                    break;
                }
            }
            if (path==null || path.trim().equals("")){
                ArcPathinfoI pathInfo= this.getGlobalpaths();
                if (pathInfo!=null){
                    path=pathInfo.getCachepath();
                }
            }
        if (path==null){
            path =".";
        }
        path = path.replace('\\', '/');
        if (!path.endsWith("/")){
            path = path +"/";
        }
        return path;
    }

    public  Integer getPrearchiveCodeForProject(String id){
        List<ArcProjectI> projects=this.getProjects_project();
        for (ArcProjectI p : projects){
            if (p.getId().equals(id)){
            	return p.getPrearchiveCode();
            }
        }
        return null;
    }

    public  Integer getAutoQuarantineCodeForProject(String id){
        List<ArcProjectI> projects=this.getProjects_project();
        for (ArcProjectI p : projects){
            if (p.getId().equals(id)){
            	return p.getQuarantineCode();
            }
        }
        return null;
    }

    public  String getPrearchivePathForProject(String id){
        String path = null;
            List<ArcProjectI> projects=this.getProjects_project();
            for (ArcProjectI p : projects){
                if (p.getId().equals(id)){
                    ArcPathinfoI pathInfo= p.getPaths();
                    if (pathInfo!=null){
                        path=pathInfo.getPrearchivepath();
                    }
                    break;
                }
            }
            if (path==null || path.trim().equals("")){
                ArcPathinfoI pathInfo= this.getGlobalpaths();
                if (pathInfo!=null){
                    path=pathInfo.getPrearchivepath();
                }
            }
        if (path==null){
            path =".";
        }
        path = path.replace('\\', '/');
        if (!path.endsWith("/")){
            path = path +"/";
        }
        return path;
    }

    public String getBuildPathForProject(String id){
        String path = null;
            List<ArcProjectI> projects=this.getProjects_project();
            for (ArcProjectI p : projects){
                if (p.getId().equals(id)){
                    ArcPathinfoI pathInfo= p.getPaths();
                    if (pathInfo!=null){
                        path=pathInfo.getBuildpath();
                    }
                    break;
                }
            }
            if (path==null || path.trim().equals("")){
                ArcPathinfoI pathInfo= this.getGlobalpaths();
                if (pathInfo!=null){
                    path=pathInfo.getBuildpath();
                }
            }
        if (path==null){
            path =".";
        }
        path = path.replace('\\', '/');
        if (!path.endsWith("/")){
            path = path +"/";
        }
        return path;
    }

    public  ArcProject getProjectArc(String id){
            List<ArcProjectI> projects=getProjects_project();
            for (ArcProjectI p : projects){
                if (p.getId().equals(id)){
                    return (ArcProject)p;
                }
            }
        return null;
    }

    public boolean isComplete(){
        if (this.getSiteId()==null || this.getSiteId().equals("")){
            return false;
        }

        if (this.getSiteAdminEmail()==null || this.getSiteAdminEmail().equals("")){
            return false;
        }

        if (this.getGlobalpaths()==null){
            return false;
        }

        if (this.getGlobalpaths().getArchivepath()==null || this.getGlobalpaths().getArchivepath().equals("")){
            return false;
        }

        if (this.getGlobalpaths().getPrearchivepath()==null || this.getGlobalpaths().getPrearchivepath().equals("")){
            return false;
        }

        if (this.getGlobalpaths().getCachepath()==null || this.getGlobalpaths().getCachepath().equals("")){
            return false;
        }

        if (this.getGlobalpaths().getBuildpath()==null || this.getGlobalpaths().getBuildpath().equals("")){
            return false;
        }

        if (this.getGlobalpaths().getFtppath()==null || this.getGlobalpaths().getFtppath().equals("")){
            return false;
        }

        return true;
    }
}
