// Copyright 2010 Washington University School of Medicine All Rights Reserved
package org.nrg.xnat.restlet.resources.files;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;

import org.nrg.xdat.om.XnatAbstractresource;
import org.nrg.xdat.om.XnatExperimentdata;
import org.nrg.xdat.om.XnatImageassessordata;
import org.nrg.xdat.om.XnatImagescandata;
import org.nrg.xdat.om.XnatImagesessiondata;
import org.nrg.xdat.om.XnatReconstructedimagedata;
import org.nrg.xdat.om.XnatResource;
import org.nrg.xft.XFTTable;
import org.nrg.xft.exception.ElementNotFoundException;
import org.nrg.xft.utils.StringUtils;
import org.nrg.xnat.helpers.FileWriterWrapper;
import org.nrg.xnat.helpers.resource.XnatResourceInfo;
import org.nrg.xnat.helpers.resource.direct.DirectAssessResourceImpl;
import org.nrg.xnat.helpers.resource.direct.DirectExptResourceImpl;
import org.nrg.xnat.helpers.resource.direct.DirectProjResourceImpl;
import org.nrg.xnat.helpers.resource.direct.DirectReconResourceImpl;
import org.nrg.xnat.helpers.resource.direct.DirectResourceModifierA;
import org.nrg.xnat.helpers.resource.direct.DirectScanResourceImpl;
import org.nrg.xnat.helpers.resource.direct.DirectSubjResourceImpl;
import org.restlet.Context;
import org.restlet.data.Request;
import org.restlet.data.Response;

public class XNATCatalogTemplate extends XNATTemplate {
	XFTTable catalogs=null;
	
	ArrayList<String> resource_ids=null;
	ArrayList<XnatAbstractresource> resources=new ArrayList<XnatAbstractresource>();
	
	public XNATCatalogTemplate(Context context, Request request,
			Response response,boolean allowAll) {
		super(context, request, response);
		
		String resourceID= (String)request.getAttributes().get("RESOURCE_ID");

		if(resourceID!=null){
			resource_ids=new ArrayList<String>();
			for(String s:StringUtils.CommaDelimitedStringToArrayList(resourceID, true)){
				resource_ids.add(s);
				}
			}
			
			try {
			catalogs=this.loadCatalogs(resource_ids,true,allowAll);
			} catch (Exception e) {
	            logger.error("",e);
			}
	}
	
	
	public String getBaseURI() throws ElementNotFoundException{
		StringBuffer sb =new StringBuffer("/data");
		if(proj!=null && sub!=null){
			sb.append("/projects/");
			sb.append(proj.getId());
			sb.append("/subjects/");
			sb.append(sub.getId());
		}
		if(recons.size()>0){
			sb.append("/experiments/");
			int aC=0;
			for(XnatExperimentdata assessed:this.assesseds){
				if(aC++>0)sb.append(",");
			sb.append(assessed.getId());
			}
			sb.append("/reconstructions/");
			int sC=0;
			for(XnatReconstructedimagedata recon:recons){
				if(sC++>0)sb.append(",");
			sb.append(recon.getId());
			}
			if(type!=null){
				sb.append("/" + type);
			}
		}else if(scans.size()>0){
			sb.append("/experiments/");
			int aC=0;
			for(XnatExperimentdata assessed:this.assesseds){
				if(aC++>0)sb.append(",");
			sb.append(assessed.getId());
			}
			sb.append("/scans/");
			int sC=0;
			for(XnatImagescandata scan:scans){
				if(sC++>0)sb.append(",");
			sb.append(scan.getId());
			}
		}else if(expts.size()>0){
			if(assesseds.size()>0){
				sb.append("/experiments/");
				int aC=0;
				for(XnatExperimentdata assessed:this.assesseds){
					if(aC++>0)sb.append(",");
				sb.append(assessed.getId());
				}
				sb.append("/assessors/");
				int eC=0;
				for(XnatExperimentdata expt:this.expts){
					if(eC++>0)sb.append(",");
				sb.append(expt.getId());
				}
				if(type!=null){
					sb.append("/" + type);
				}
			}else{
				sb.append("/experiments/");
				int eC=0;
				for(XnatExperimentdata expt:this.expts){
					if(eC++>0)sb.append(",");
				sb.append(expt.getId());
			}
			}
		}else if(sub!=null){
			
		}else if(proj!=null){
			sb.append("/projects/");
			sb.append(proj.getId());
		}
		return sb.toString();
	}



    protected File getFileOnLocalFileSystem(String fullPath) {
        File f = new File(fullPath);
        if (!f.exists()){
            if (!fullPath.endsWith(".gz")){
            	f= new File(fullPath + ".gz");
            	if (!f.exists()){
            		return null;
            	}
            }else{
                return null;
            }
        }
        
        return f;
    }
	
    public XnatResourceInfo buildResourceInfo(){
		XnatResourceInfo info = new XnatResourceInfo();
        
	    if(this.getQueryVariable("description")!=null){
	    	info.setDescription(this.getQueryVariable("description"));
	    }
	    if(this.getQueryVariable("format")!=null){
	    	info.setFormat(this.getQueryVariable("format"));
	    }
	    if(this.getQueryVariable("content")!=null){
	    	info.setContent(this.getQueryVariable("content"));
	    }
	    
	    if(this.getQueryVariables("tags")!=null){
	    	String[] tags = this.getQueryVariables("tags");
	    	for(String tag: tags){
	    		tag = tag.trim();
	    		if(!tag.equals("")){
	    			for(String s:StringUtils.CommaDelimitedStringToArrayList(tag)){
	    				s=s.trim();
	    				if(!s.equals("")){
	    		    		if(s.indexOf("=")>-1){
	    		    			info.addMeta(s.substring(0,s.indexOf("=")),s.substring(s.indexOf("=")+1));
	    		    		}else{
	    		    			if(s.indexOf(":")>-1){
		    		    			info.addMeta(s.substring(0,s.indexOf(":")),s.substring(s.indexOf(":")+1));
		    		    		}else{
		    		    			info.addTag(s);
		    		    		}
	    		    		}
	    				}
	    			}
	    			
	    		}
	    	}
	    }
        
		return info;
			}
			
	protected DirectResourceModifierA buildResourceModifier() throws Exception{
		XnatImagesessiondata assessed=null;
			
		if(this.assesseds.size()==1)assessed=(XnatImagesessiondata)assesseds.get(0);
			
		if(recons.size()>0){
			//reconstruction			
			if(assessed==null){
				throw new Exception("Invalid session id");
			}
			
			return new DirectReconResourceImpl(recons.get(0), assessed, type);
		}else if(scans.size()>0){
			//scan
			if(assessed==null){
				throw new Exception("Invalid session id");
			}
			
			return new DirectScanResourceImpl(scans.get(0), assessed);
		}else if(expts.size()>0){
			final XnatExperimentdata expt=this.expts.get(0);
//			experiment
			
			if(expt.getItem().instanceOf("xnat:imageAssessorData")){
				if(assessed==null){
					throw new Exception("Invalid session id");
			}

				return new DirectAssessResourceImpl((XnatImageassessordata)expt,(XnatImagesessiondata)assessed,type);
			}else{
				return new DirectExptResourceImpl(proj,expt);
			}
			
		}else if(sub!=null){
			return new DirectSubjResourceImpl(proj, sub);
		}else if(proj!=null){
			return new DirectProjResourceImpl(proj);
			}else{
			throw new Exception("Unknown resource");
		}
	}
}
