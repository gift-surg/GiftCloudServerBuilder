/**
 * Copyright 2006,2008,2010 Harvard University / Washington University School of Medicine All Rights Reserved
 */
package org.nrg.xnat.turbine.modules.actions;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.apache.turbine.modules.ScreenLoader;
import org.apache.turbine.util.RunData;
import org.apache.velocity.context.Context;
import org.nrg.xdat.base.BaseElement;
import org.nrg.xdat.model.XnatAbstractresourceI;
import org.nrg.xdat.model.XnatImagescandataI;
import org.nrg.xdat.om.WrkWorkflowdata;
import org.nrg.xdat.om.XnatAbstractresource;
import org.nrg.xdat.om.XnatExperimentdata;
import org.nrg.xdat.om.XnatImagesessiondata;
import org.nrg.xdat.om.XnatProjectdata;
import org.nrg.xdat.om.XnatResource;
import org.nrg.xdat.om.XnatSubjectdata;
import org.nrg.xdat.schema.SchemaElement;
import org.nrg.xdat.security.ElementSecurity;
import org.nrg.xdat.security.XDATUser;
import org.nrg.xdat.turbine.modules.actions.ModifyItem;
import org.nrg.xdat.turbine.modules.screens.EditScreenA;
import org.nrg.xdat.turbine.utils.AdminUtils;
import org.nrg.xdat.turbine.utils.PopulateItem;
import org.nrg.xdat.turbine.utils.TurbineUtils;
import org.nrg.xft.ItemI;
import org.nrg.xft.XFTItem;
import org.nrg.xft.db.MaterializedView;
import org.nrg.xft.exception.ElementNotFoundException;
import org.nrg.xft.exception.FieldNotFoundException;
import org.nrg.xft.exception.InvalidValueException;
import org.nrg.xft.exception.XFTInitException;
import org.nrg.xft.schema.design.SchemaElementI;
import org.nrg.xft.search.CriteriaCollection;
import org.nrg.xft.security.UserI;
import org.nrg.xft.utils.FileUtils;
import org.nrg.xft.utils.ValidationUtils.ValidationResults;
import org.nrg.xnat.archive.Transfer;
import org.xml.sax.SAXException;

public class StoreImageSession extends ModifyItem {
    private final Logger logger = Logger.getLogger(ModifyItem.class);

    private String prearcSessionPath = null;

    /* (non-Javadoc)
     * @see org.nrg.xdat.turbine.modules.actions.ModifyItem#allowDataDeletion()
     */
    @Override
    public boolean allowDataDeletion(){
        return true;
    }

    public XnatImagesessiondata template = null;
    public XnatImagesessiondata populateItem(final RunData data, final Context context) throws Exception{
    	if(template==null){
            final String tag= (String)TurbineUtils.GetPassedParameter("tag", data);
            final String header = "ELEMENT_";
            final Map<String,String> hash = new HashMap<String,String>();
            for (int counter = 0; null != data.getParameters().get(header + counter); counter++) {
                final String elementToLoad = data.getParameters().getString(header + counter);
                final Integer numberOfInstances = data.getParameters().getIntObject(elementToLoad);
                if (numberOfInstances != null && numberOfInstances.intValue()!=0) {
                    for (int subCount = 0; subCount < numberOfInstances; subCount++) {
                          hash.put(elementToLoad + subCount, elementToLoad);
                    }
                } else {
                    hash.put(elementToLoad,elementToLoad);
                }
            }
            
            final String screenName;
            if (null == data.getParameters().getString("edit_screen")) {
        	screenName = null;
            } else {
                screenName = data.getParameters().getString("edit_screen").substring(0,data.getParameters().getString("edit_screen").lastIndexOf(".vm"));
            }
            
            if (hash.isEmpty()) {
                data.setMessage("Missing ELEMENT_0 indicator.");
                return null;
            }
            
            InvalidValueException error = null;
            final List<ItemI> al = new ArrayList<ItemI>(hash.size());
            for (final String element : hash.values()) {
                final SchemaElement e = SchemaElement.GetElement(element);
                
                final PopulateItem populator;
                if (null == screenName) {
                    populator = PopulateItem.Populate(data,element,true);
                } else {
                    if (screenName.equals("XDATScreen_edit_" + e.getFormattedName()))
                    {
                        EditScreenA screen = (EditScreenA) ScreenLoader.getInstance().getInstance(screenName);
                        XFTItem newItem = (XFTItem)screen.getEmptyItem(data);
                        populator = PopulateItem.Populate(data,element,true,newItem);
                    } else {
                        populator = PopulateItem.Populate(data,element,true);
                    }
                }
                
                if (populator.hasError()) {
                    error = populator.getError();
                }
                
                al.add(populator.getItem());
            }
            final XFTItem first = (XFTItem)al.get(0);
            try {
                preProcess(first,data,context);
            } catch (RuntimeException e1) {
                logger.error("",e1);
            }
            
            if (error!=null)
            {
                data.getSession().setAttribute(this.getReturnEditItemIdentifier(),first);
                data.addMessage(error.getMessage());
                if (data.getParameters().getString("edit_screen") != null) {
                    data.setScreenTemplate(data.getParameters().getString("edit_screen"));
                }
                return null;
            }
            
            final ItemI found = al.get(0);
            
            template = (XnatImagesessiondata) data.getSession().getAttribute(tag);
            data.getSession().removeAttribute(tag);
            if (null == template.getUser()) {
            	template.getItem().setUser(TurbineUtils.getUser(data));
                    }
            
            final String sessionType = template.getXSIType();
            template.copyValuesFrom((XnatImagesessiondata)BaseElement.GetGeneratedItem(found));
                }
    	
    	XnatExperimentdata expt=null;
    	if(template.getLabel()==null || template.getLabel().equals("")){
    		expt=XnatExperimentdata.GetExptByProjectIdentifier(template.getProject(), template.getLabel(), TurbineUtils.getUser(data), false);
            }

    	if(expt==null){
    		template.setId(XnatExperimentdata.CreateNewID());
        }else{
        	template.setId(expt.getId());
                }
    	
    	return template;
            }
            
    public XnatImagesessiondata process(RunData data,Context context) throws Exception{
			XnatImagesessiondata imageSession=this.populateItem(data, context);
            
			try {
			    preProcess(imageSession.getItem(),data,context);
			} catch (RuntimeException e1) {
			    logger.error("",e1);
			}
			try { 
			    preSave(imageSession.getItem(),data,context);
			} catch (CriticalException e) {
			    throw e;
			} catch (RuntimeException e) {
			    logger.error("",e);
			}
			XDATUser user=TurbineUtils.getUser(data);
			
			if(imageSession.save(user,false,allowDataDeletion())){
				user.clearLocalCache();
				MaterializedView.DeleteByUser(user);
				
				boolean quarantine=false;
				if(TurbineUtils.GetPassedParameter("quarantine", data)!=null){
					quarantine=true;
				}else{
					XnatProjectdata proj=imageSession.getPrimaryProject(false);
					if(proj.getArcSpecification().getQuarantineCode()!=null && proj.getArcSpecification().getQuarantineCode()>0){
						quarantine=true;
					}
            }
            
				if(quarantine){
					imageSession.quarantine(user);
				}
			}
            
			try {
				postProcessing(imageSession.getItem(),data,context);
			} catch (Throwable e) {
				logger.error("",e);
			}

			return imageSession;
    }

    public void doPerform(final RunData data, final Context context) throws Exception
    {
        //TurbineUtils.OutputPassedParameters(data,context,this.getClass().getName());
        //parameter specifying elementAliass and elementNames
        try {
            XnatImagesessiondata imageSession=this.populateItem(data, context);
            
            if(imageSession==null){
            	return;
            }
            try {
                preProcess(imageSession.getItem(),data,context);
            } catch (RuntimeException e1) {
                logger.error("",e1);
            }
            
            final ValidationResults vr = imageSession.validate();
            
            if (vr != null && !vr.isValid()) {
                data.getSession().setAttribute(this.getReturnEditItemIdentifier(),imageSession);
                context.put("vr",vr);
                if (data.getParameters().getString("edit_screen") != null) {
                    data.setScreenTemplate(data.getParameters().getString("edit_screen"));
                }
            }else {
                try {
                    try { 
                        preSave(imageSession.getItem(),data,context);
                    } catch (CriticalException e) {
                        throw e;
                    } catch (RuntimeException e) {
                        logger.error("",e);
                    }
					XDATUser user=TurbineUtils.getUser(data);
					
					if(imageSession.save(user,false,allowDataDeletion())){
						MaterializedView.DeleteByUser(user);
                    
						XnatProjectdata proj=imageSession.getPrimaryProject(false);
						if(proj.getArcSpecification().getQuarantineCode()!=null && proj.getArcSpecification().getQuarantineCode().equals(1)){
							imageSession.quarantine(user);
						}
					}
                } catch (Exception e) {
                    logger.error("Error Storing " + imageSession.getXSIType(),e);
                    data.setMessage(e.getMessage());

                    data.getSession().setAttribute(this.getReturnEditItemIdentifier(),imageSession);
                    context.put("vr",vr);
                    if (data.getParameters().getString("edit_screen") !=null) {
                        data.setScreenTemplate(data.getParameters().getString("edit_screen"));
                    }
                    return;
                }
                
                try {
                    postProcessing(imageSession.getItem(),data,context);
                    this.sendToReport(data, imageSession.getItem());
                } catch (Exception e) {
                    logger.error("",e);
                    data.setMessage(e.getMessage());
                }
            }
        } catch (XFTInitException e) {
            logger.error("",e);
            data.setMessage(e.getMessage());
            if (data.getParameters().getString("edit_screen") !=null) {
                data.setScreenTemplate(data.getParameters().getString("edit_screen"));
            } else {
                data.setScreenTemplate("Index.vm");
            }
        } catch (ElementNotFoundException e) {
            logger.error("",e);
            data.setMessage(e.getMessage());
            if (data.getParameters().getString("edit_screen") !=null) {
                data.setScreenTemplate(data.getParameters().getString("edit_screen"));
            }else {
                data.setScreenTemplate("Index.vm");
            }
        } catch (FieldNotFoundException e) {
            logger.error("",e);
            data.setMessage("Error: Unknown field '" + e.FIELD + "'.");
            if (data.getParameters().getString("edit_screen") !=null) {
                data.setScreenTemplate(data.getParameters().getString("edit_screen"));
            } else {
                data.setScreenTemplate("Index.vm");
            }
        } catch (Exception e) {
            logger.error("",e);
            data.setMessage(e.getMessage());
            if (data.getParameters().getString("edit_screen") !=null) {
                data.setScreenTemplate(data.getParameters().getString("edit_screen"));
            } else {
                data.setScreenTemplate("Index.vm");
            }
        }
    }
    
    @Override
    public void preProcess(final XFTItem item, final RunData data, final Context context) {
	final XnatImagesessiondata session = (XnatImagesessiondata)BaseElement.GetGeneratedItem(item);
	prearcSessionPath = session.getPrearchivepath();
    }
    
    /* (non-Javadoc)
     * @see org.nrg.xdat.turbine.modules.actions.ModifyItem#postProcessing(org.nrg.xft.XFTItem)
     */
    @Override
    public void postProcessing(XFTItem item,RunData data, Context context)
    throws FileNotFoundException,SAXException,IllegalArgumentException,XFTInitException,ElementNotFoundException {
	final XnatImagesessiondata session = (XnatImagesessiondata)BaseElement.GetGeneratedItem(item);
	if (prearcSessionPath.endsWith("/")) {
	    prearcSessionPath = prearcSessionPath.substring(0,prearcSessionPath.length()-1);
	}
	if (prearcSessionPath.endsWith("\\")) {
	    prearcSessionPath = prearcSessionPath.substring(0,prearcSessionPath.length()-1);
	}
	//save the prearchive xml with the updated IDs set
	final File xml = new File(prearcSessionPath + ".xml");
	if (xml.exists()){
	    final FileOutputStream fos;
	    try {
		fos = new FileOutputStream(xml);
	    } catch (FileNotFoundException e) {
		data.setMessage("Unable to open missing session XML");
		logger.error("XML file passed exists() test, but now is missing", e);
		throw e;
	    }

	    try {
		session.toXML(fos,true);
	    } catch (IllegalArgumentException e) {
		data.setMessage("Error updating prearchive xml file.");
		logger.error("unable to update session XML", e);
	    } catch (SAXException e) {
		data.setMessage("Error updating prearchive xml file.");
		logger.error("unable to update session XML", e);
	    } finally {
		try { fos.close(); } catch (IOException ignore) {}
	    }
	} else {
	    logger.error("Error updating prearchive xml file:\n"+ xml.getAbsolutePath() + " does not exist.");
	    data.setMessage("Error updating prearchive xml file.");
	}

	//spin off transfer process
	final Transfer transfer = new Transfer(TurbineUtils.GetFullServerPath(),TurbineUtils.GetSystemName(),AdminUtils.getAdminEmailId());
	if (null == session.getUser()) {
	    session.getItem().setUser(TurbineUtils.getUser(data));
	}
	transfer.setImageSession(session);
	transfer.setPlaceInRaw(false);
	transfer.setPrearc(prearcSessionPath);
	transfer.execute();

	//next chunk should probably be deleted.
	final SchemaElementI se = SchemaElement.GetElement(item.getXSIType());
	if (se.getGenericXFTElement().getType().getLocalPrefix().equalsIgnoreCase("xdat")) {
	    ElementSecurity.refresh();
	}

	//this is maintained for project scoping in display
	if (session.getProject()!=null){
	    data.getParameters().setString("project", session.getProject());
	}

    }
    
    public void sendToReport(RunData data, XFTItem item){
	data.setMessage("Session Successfully Stored.  The Session files are being transfered into the permanent archive.");
	if (TurbineUtils.HasPassedParameter("destination", data)){
	    this.redirectToReportScreen((String)TurbineUtils.GetPassedParameter("destination", data), item, data);
	} else {
	    this.redirectToReportScreen(item, data);
	}
    }

    public String getArcSessionPath(XnatImagesessiondata session) throws Exception{
    	final String currentarc = session.getCurrentArchiveFolder();
        String arcSessionPath;
        if (null == currentarc){
            arcSessionPath = session.getArchiveDirectoryName() + "/";
        } else {
            arcSessionPath = currentarc.replace('\\', '/') + session.getArchiveDirectoryName() + "/";
        }
        
        return FileUtils.AppendRootPath(session.getPrimaryProject(false).getRootArchivePath(), arcSessionPath); 
        
    }

    /* (non-Javadoc)
     * @see org.nrg.xdat.turbine.modules.actions.ModifyItem#preSave(org.nrg.xft.XFTItem)
     */
    @Override
    public void preSave(XFTItem item,RunData data, Context context) throws Exception {
    	
    	final XnatImagesessiondata session = (XnatImagesessiondata)BaseElement.GetGeneratedItem(item);
	final XDATUser user = (XDATUser) session.getUser();
	
	//check to see if a transfer is already running.
	final CriteriaCollection cc= new CriteriaCollection("AND");
	cc.addClause("wrk:workFlowData.ID",session.getId());
	cc.addClause("wrk:workFlowData.pipeline_name","Transfer");
	cc.addClause("wrk:workFlowData.status","In Progress");
	final Collection al = WrkWorkflowdata.getWrkWorkflowdatasByField(cc, user, false);
	if (!al.isEmpty()){
	    throw new org.nrg.xdat.turbine.modules.actions.ModifyItem.CriticalException("Transfer in progress.  Try again later.");
	}
	
	//if a subject doesn't exist, create one
		XnatSubjectdata subj=session.getSubjectData();
		
		if(subj==null  && LoadImageData.hasValue(session.getSubjectId())){
			String cleaned=XnatSubjectdata.cleanValue(session.getSubjectId());
			if(!cleaned.equals(session.getSubjectId())){
				session.setSubjectId(cleaned);
				subj=session.getSubjectData();
			}
		}
		
		if(subj==null){
			XnatSubjectdata sub=new XnatSubjectdata((UserI)user);
			sub.setProject(session.getProject());
			if(LoadImageData.hasValue(session.getSubjectId())){
				sub.setLabel(XnatSubjectdata.cleanValue(session.getSubjectId()));
        }
			sub.setId(XnatSubjectdata.CreateNewID());
			sub.save(user, false, false);
			
			session.setSubjectId(sub.getId());
		}
		
		//fix the scan paths using the destination session path (they are probably relative paths before this)
        String arcSessionPath=this.getArcSessionPath(session);
        
        for (final XnatImagescandataI scan : session.getScans_scan()) {
            for (final XnatAbstractresourceI file : scan.getFile()) {
        	((XnatAbstractresource)file).appendToPaths(arcSessionPath);
                 
        	
        	try {
	        	    if (null != scan.getType() && !"".equals(scan.getType())){
	        	    	if(((XnatResource)file).getContent()==null || ((XnatResource)file).getContent().equals("")){
            				((XnatResource)file).setContent("RAW");
            			}
	        	    }
        	} catch (Throwable e) {
        	    logger.error("error setting scan content type",e);
        	}
            }
        }
    }
}

