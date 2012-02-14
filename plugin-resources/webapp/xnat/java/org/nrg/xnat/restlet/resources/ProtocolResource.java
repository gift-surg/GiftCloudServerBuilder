// Copyright 2010 Washington University School of Medicine All Rights Reserved
package org.nrg.xnat.restlet.resources;

import java.sql.SQLException;
import java.util.ArrayList;

import org.nrg.xdat.om.XnatAbstractprotocol;
import org.nrg.xdat.om.XnatDatatypeprotocol;
import org.nrg.xdat.om.XnatProjectdata;
import org.nrg.xdat.security.ElementSecurity;
import org.nrg.xft.XFTItem;
import org.nrg.xft.db.DBAction;
import org.nrg.xft.db.MaterializedView;
import org.nrg.xft.schema.Wrappers.GenericWrapper.GenericWrapperElement;
import org.nrg.xft.security.UserI;
import org.nrg.xft.utils.SaveItemHelper;
import org.restlet.Context;
import org.restlet.data.MediaType;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.data.Status;
import org.restlet.resource.Representation;
import org.restlet.resource.Variant;
import org.xml.sax.SAXParseException;

public class ProtocolResource extends ItemResource {
	XnatProjectdata proj=null;

	XnatDatatypeprotocol protocol = null;
	String protID=null;
	
	XnatDatatypeprotocol existing =null;
	
	public ProtocolResource(Context context, Request request, Response response) {
		super(context, request, response);
		
		String pID = (String) request.getAttributes().get("PROJECT_ID");
		if (pID != null) {
			proj = XnatProjectdata.getProjectByIDorAlias(pID, user, false);
		}

		protID = (String) request.getAttributes().get("PROTOCOL_ID");

		if (proj != null)
			existing = (XnatDatatypeprotocol) XnatAbstractprotocol
					.getXnatAbstractprotocolsById(protID, user, true);

		this.getVariants().add(new Variant(MediaType.TEXT_XML));
	}
	
	@Override
	public boolean allowPut() {
		return true;
	}

	@Override
	public void handlePut() {
		try {
			XFTItem template=null;
			if (existing!=null){
				template=existing.getItem();
			}
			
			XFTItem item=this.loadItem("xnat:datatypeProtocol",true,template);
			
			if(item.instanceOf("xnat:datatypeProtocol")){
				protocol = new XnatDatatypeprotocol(item);
					
					if(this.proj==null && protocol.getProject()!=null){
						proj = XnatProjectdata.getXnatProjectdatasById(protocol.getProject(), user, false);
					}
					
					if(this.proj!=null){
						if(protocol.getProject()==null || protocol.getProject().equals("")){
							protocol.setProperty("xnat:projectdata_id", proj.getId());
						}
					}else{
						this.getResponse().setStatus(Status.CLIENT_ERROR_UNPROCESSABLE_ENTITY,"Submitted subject record must include the project attribute.");
						return;
					}
					
					if(existing==null){
						if(!user.canEdit(proj)){
							this.getResponse().setStatus(Status.CLIENT_ERROR_FORBIDDEN,"Specified user account has insufficient edit priviledges for this project.");
							return;
						}
						//IS NEW
						if(protocol.getId()==null || protocol.getId().equals("")){
							protocol.setId(protocol.getDataType());
						}
					}else{							
						if(!user.canEdit(proj)){
							this.getResponse().setStatus(Status.CLIENT_ERROR_FORBIDDEN,"Specified user account has insufficient edit priviledges for this project.");
							return;
						}
						if(protocol.getId()==null || protocol.getId().equals("")){
							protocol.setId(existing.getId());
						}
					}
					
					if(this.getQueryVariable("gender")!=null){
						protocol.setProperty("xnat:subjectData/demographics[@xsi:type=xnat:demographicData]/gender",this.getQueryVariable("gender"));
					}
											
					if(SaveItemHelper.authorizedSave(protocol,user,false,true)){
						MaterializedView.DeleteByUser(user);
					}
					
					this.returnXML(protocol.getItem());
				}
		} catch (SAXParseException e) {
			this.getResponse().setStatus(Status.CLIENT_ERROR_UNPROCESSABLE_ENTITY,e.getMessage());
		} catch (Exception e) {
			this.getResponse().setStatus(Status.SERVER_ERROR_INTERNAL);
			e.printStackTrace();
		}
	}

	@Override
	public boolean allowDelete() {
		return true;
	}

	@Override
	public void handleDelete(){
		if(existing!=null){
			protocol=existing;
		}
		
		try {
		
			if(!user.canEdit(proj)){
				this.getResponse().setStatus(Status.CLIENT_ERROR_FORBIDDEN,"User account doesn't have permission to delete this subject.");
				return;
			}
		
			if(protocol!=null){
				if (protocol!=null){				        
					SaveItemHelper.authorizedDelete(protocol.getItem().getCurrentDBVersion(), user);
			    }
			    user.clearLocalCache();
				MaterializedView.DeleteByUser(user);
			}
		} catch (SQLException e) {
			e.printStackTrace();
			this.getResponse().setStatus(Status.SERVER_ERROR_INTERNAL,e);
		} catch (Exception e) {
			e.printStackTrace();
			this.getResponse().setStatus(Status.SERVER_ERROR_INTERNAL,e);
		}
	}

	@Override
	public Representation getRepresentation(Variant variant) {	
		MediaType mt = overrideVariant(variant);
			
		if(protocol!=null){
			return this.representItem(protocol.getItem(),mt);
		}else{
			if(this.getQueryVariable("dataType")!=null && proj!=null){
				String dataType=this.getQueryVariable("dataType");
				XnatDatatypeprotocol temp = (XnatDatatypeprotocol)proj.getProtocolByDataType(dataType);
				
				try {
					ElementSecurity ess = ElementSecurity.GetElementSecurity(dataType);
					
					if(temp==null && ess!=null){
						GenericWrapperElement e=GenericWrapperElement.GetElement(dataType);
						temp=new XnatDatatypeprotocol((UserI)user);
						temp.setProperty("xnat_projectdata_id", proj.getId());
						temp.setDataType(e.getXSIType());
						
						temp.setId(proj.getId() + "_" + e.getSQLName());
					    if (temp.getProperty("name")==null){
					    	temp.setProperty("name",ess.getPluralDescription());
					    }
					    
					    if(temp.getXSIType().equals("xnat:datatypeProtocol")){
					    	temp.setProperty("xnat:datatypeProtocol/definitions/definition[ID=default]/data-type", temp.getProperty("data-type"));
					    	temp.setProperty("xnat:datatypeProtocol/definitions/definition[ID=default]/project-specific", "false");
					    }
					    SaveItemHelper.authorizedSave(temp,user, false, false);
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
				
				if(temp==null){
					this.getResponse().setStatus(Status.CLIENT_ERROR_NOT_FOUND,"Unable to find the specified protocol.");
					return null;
				}else{
					return this.representItem(temp.getItem(),mt);
				}
			}else{
				this.getResponse().setStatus(Status.CLIENT_ERROR_NOT_FOUND,"Unable to find the specified protocol.");
				return null;
			}
		}

	}
}
