// Copyright 2010 Washington University School of Medicine All Rights Reserved
package org.nrg.xnat.restlet.resources;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Map;

import org.nrg.xdat.model.XnatProjectdataI;
import org.nrg.xdat.om.XnatExperimentdata;
import org.nrg.xdat.om.XnatProjectdata;
import org.nrg.xdat.om.XnatSubjectassessordata;
import org.nrg.xft.XFTTable;
import org.restlet.Context;
import org.restlet.data.MediaType;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.data.Status;
import org.restlet.resource.Representation;
import org.restlet.resource.Variant;

public class ExperimentResource extends ItemResource {
	XnatProjectdata proj=null;

	XnatExperimentdata expt = null;
	String exptID=null;
	
	public ExperimentResource(Context context, Request request, Response response) {
		super(context, request, response);
		
			String pID= (String)getParameter(request,"PROJECT_ID");
			if(pID!=null){
				proj = XnatProjectdata.getProjectByIDorAlias(pID, user, false);
			}

			exptID= (String)getParameter(request,"EXPT_ID");
			if(exptID!=null){
				this.getVariants().add(new Variant(MediaType.TEXT_XML));
			}else{
			response.setStatus(Status.CLIENT_ERROR_NOT_FOUND);
		}
	}
	

	@Override
	public Representation getRepresentation(Variant variant) {	
		MediaType mt = overrideVariant(variant);

		if(expt==null&& exptID!=null){
			expt=XnatExperimentdata.getXnatExperimentdatasById(exptID, user, false);
			

			if(proj!=null){
				if(expt==null){
					expt=(XnatSubjectassessordata)XnatExperimentdata.GetExptByProjectIdentifier(proj.getId(), exptID,user, false);
				}
			}
		}
		
		if(expt!=null){
			if(filepath!=null && !filepath.equals("") && filepath.equals("status")){

				return returnStatus(expt,mt);
			}else if(filepath!=null && !filepath.equals("") && filepath.equals("history")){
				try {
					return buildChangesets(expt.getItem(), mt);
				} catch (Exception e) {
					logger.error("",e);
					this.getResponse().setStatus(Status.SERVER_ERROR_INTERNAL,e.getMessage());
					return null;
				}
			}else if(filepath!=null && !filepath.equals("") && filepath.startsWith("projects")){
				XFTTable t = new XFTTable();
				ArrayList al = new ArrayList();
				al.add("label");
				al.add("ID");
				al.add("Secondary_ID");
				al.add("Name");
				t.initTable(al);

				Object[] row=new Object[4];
				row[0]=expt.getLabel();
				XnatProjectdata primary = expt.getPrimaryProject(false);
				row[1]=primary.getId();
				row[2]=primary.getSecondaryId();
				row[3]=primary.getName();
				t.rows().add(row);

				for(Map.Entry<XnatProjectdataI, String> entry: expt.getProjectDatas().entrySet()){
					row=new Object[4];
					row[0]=entry.getValue();
					row[1]=entry.getKey().getId();
					row[2]=entry.getKey().getSecondaryId();
					row[3]=entry.getKey().getName();
					t.rows().add(row);
				}

				return representTable(t, mt, new Hashtable<String,Object>());
			}else{
				return this.representItem(expt.getItem(),mt);
			}
		}else{
			this.getResponse().setStatus(Status.CLIENT_ERROR_NOT_FOUND,
					"Unable to find the specified experiment.");
			return null;
		}

	}

}
