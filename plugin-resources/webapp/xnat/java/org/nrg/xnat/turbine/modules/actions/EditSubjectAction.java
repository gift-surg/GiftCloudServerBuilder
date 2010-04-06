//Copyright 2005 Harvard University / Howard Hughes Medical Institute (HHMI) All Rights Reserved
/*
 * Created on Mar 25, 2005
 *
 */
package org.nrg.xnat.turbine.modules.actions;


import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;

import org.apache.log4j.Logger;
import org.apache.turbine.modules.ScreenLoader;
import org.apache.turbine.util.RunData;
import org.apache.velocity.context.Context;
import org.nrg.xdat.base.BaseElement;
import org.nrg.xdat.om.XnatProjectdata;
import org.nrg.xdat.om.XnatProjectparticipant;
import org.nrg.xdat.om.XnatSubjectdata;
import org.nrg.xdat.om.XnatSubjectdataAddid;
import org.nrg.xdat.schema.SchemaElement;
import org.nrg.xdat.security.XDATUser;
import org.nrg.xdat.turbine.modules.actions.DisplayItemAction;
import org.nrg.xdat.turbine.modules.actions.SecureAction;
import org.nrg.xdat.turbine.modules.screens.EditScreenA;
import org.nrg.xdat.turbine.utils.PopulateItem;
import org.nrg.xdat.turbine.utils.TurbineUtils;
import org.nrg.xft.ItemI;
import org.nrg.xft.XFT;
import org.nrg.xft.XFTItem;
import org.nrg.xft.collections.ItemCollection;
import org.nrg.xft.db.DBAction;
import org.nrg.xft.db.MaterializedView;
import org.nrg.xft.search.CriteriaCollection;
import org.nrg.xft.search.ItemSearch;
import org.nrg.xft.utils.ValidationUtils.ValidationResults;

/**
 * @author Tim
 *
 */
public class EditSubjectAction extends SecureAction {
    static Logger logger = Logger.getLogger(EditSubjectAction.class);

    /* (non-Javadoc)
     * @see org.apache.turbine.modules.actions.VelocityAction#doPerform(org.apache.turbine.util.RunData, org.apache.velocity.context.Context)
     */
    @SuppressWarnings("deprecation")
    public void doPerform(RunData data, Context context) throws Exception {
        final XDATUser user = TurbineUtils.getUser(data);
        ItemI found = null;

        if (TurbineUtils.HasPassedParameter("tag", data)){
            context.put("tag", TurbineUtils.GetPassedParameter("tag", data));
        }
        try {
            final EditScreenA screen = (EditScreenA) ScreenLoader.getInstance().getInstance("XDATScreen_edit_xnat_subjectData");
            
            final XFTItem newItem = (XFTItem)screen.getEmptyItem(data);
            
           // TurbineUtils.OutputDataParameters(data);
            
            final PopulateItem populater = PopulateItem.Populate(data,"xnat:subjectData",true,newItem);
            boolean hasError = false;
            String message = null;
            
            if (populater.hasError())
            {
                hasError = true;
                message = populater.getError().getMessage();
            }
            
            found = populater.getItem();

            if (hasError)
            {
                TurbineUtils.SetEditItem(found,data);
                System.out.println("'" + message + "'");
                if (! message.endsWith("/addID : Required Field"))
                {
            	    data.addMessage(message);
            	    if (data.getParameters().getString("edit_screen") !=null)
            	    {
            	        data.setScreenTemplate(data.getParameters().getString("edit_screen"));
            	    }
                    if(TurbineUtils.HasPassedParameter("destination", data))data.getParameters().add("destination", (String)TurbineUtils.GetPassedParameter("destination", data));
            	    return;
                }
            }
            
            final Iterator addIDS = found.getChildItems("xnat:subjectData/addID").iterator();
            while (addIDS.hasNext())
            {
                final ItemI addID = (ItemI)addIDS.next();
                if (addID.getProperty("addID") == null)
                {
                    addID.setProperty("name",null);
                }
            }
            
            final Iterator addFields = found.getChildItems("xnat:subjectData/fields/field").iterator();
            while (addFields.hasNext())
            {
                final ItemI addID = (ItemI)addFields.next();
                if (addID.getProperty("field") == null)
                {
                    addID.setProperty("name",null);
                }
            }
            
        
            ((XFTItem)found).removeEmptyItems();
            
            logger.debug("EditSubjectAction: \n" + found.toString());
      // System.out.println("EditSubjectAction: PRE DOB:" + found.getProperty("dob"));
            //System.out.println("EditSubjectAction: PRE YOB:" + found.getProperty("yob"));
            
            if (data.getParameters().get("dob_estimated")!=null)
            {
            	final Date year = (Date)found.getProperty("demographics/dob");
                found.setProperty("demographics/yob",new Integer(year.getYear() + 1900));
                found.setProperty("demographics/dob",null);
            }
            
            if((found.getProperty("demographics/yob")!=null && !found.getProperty("demographics/yob").equals("NULL"))
            		|| (found.getProperty("demographics/age")!=null && !found.getProperty("demographics/age").equals("NULL"))){
                found.setProperty("demographics/dob",null);
            }
            
            //System.out.println("EditSubjectAction: POST DOB:" + found.getProperty("dob"));
            //System.out.println("EditSubjectAction: POST YOB:" + found.getProperty("yob"));
            
            final XnatSubjectdata subject = new XnatSubjectdata(found);

            final CriteriaCollection cc = new CriteriaCollection("OR");
            
//          --BEGIN CHANGE
            if (subject.getLabel()!=null && subject.getProject()!=null){
                final CriteriaCollection subcc = new CriteriaCollection("AND");
                subcc.addClause("xnat:subjectData/project",subject.getProject());
                subcc.addClause("xnat:subjectData/label",subject.getLabel());
                cc.add(subcc);
            }
            //--END CHANGE
            
            if (subject.getAddid().size()>0)
            {
                for (final XnatSubjectdataAddid addID:subject.getAddid())
                {
                    final CriteriaCollection subCC = new CriteriaCollection("AND");
                    subCC.addClause("xnat:subjectData/addID/name",addID.getName());
                    subCC.addClause("xnat:subjectData/addID/addID",addID.getAddid());
                    cc.add(subCC);
                }
            }
            
            if (!subject.getSharing_share().isEmpty())
            {
                for (final XnatProjectparticipant addID: subject.getSharing_share())
                {
                    final CriteriaCollection subCC = new CriteriaCollection("AND");
                    if (addID.getLabel()!=null){
                        subCC.addClause("xnat:subjectData/sharing/share/project",addID.getProject());
                        subCC.addClause("xnat:subjectData/sharing/share/label",addID.getLabel());
                        cc.add(subCC);
                    }
                }
            }
            
            if (cc.size()>0)
            {
                final ItemCollection items = ItemSearch.GetItems("xnat:subjectData",cc,TurbineUtils.getUser(data),false);
                
                if (subject.getId()==null)
                {
                    //new item
                    if (items.size() >0)
                    {
                        final ArrayList matches = BaseElement.WrapItems(items.getItems());
                                                
                        context.put("matches",matches);
                        TurbineUtils.SetEditItem(found,data);
                        if (data.getParameters().getString("edit_screen") !=null)
                        {
                            data.setScreenTemplate(data.getParameters().getString("edit_screen"));
                        }
                        if(TurbineUtils.HasPassedParameter("destination", data))data.getParameters().add("destination", (String)TurbineUtils.GetPassedParameter("destination", data));
                        data.addMessage("Matched previous subject. Save aborted.");
                        return;
                    }
                }else{
                    
                    if (items.size() >0)
                    {
                        if (items.size() > 1)
                        {
                            final ArrayList matches = BaseElement.WrapItems(items.getItems());
                            context.put("matches",matches);
                            TurbineUtils.SetEditItem(found,data);
                            if (data.getParameters().getString("edit_screen") !=null)
                            {
                                data.setScreenTemplate(data.getParameters().getString("edit_screen"));
                            }
                            if(TurbineUtils.HasPassedParameter("destination", data))data.getParameters().add("destination", (String)TurbineUtils.GetPassedParameter("destination", data));
                            data.addMessage("Matched previous subject. Save aborted.");
                            return;
                        }else{
                            final ItemI match = (ItemI)items.getFirst();
                            
                            if (! match.getStringProperty("xnat:subjectData.ID").equalsIgnoreCase(found.getStringProperty("xnat:subjectData.ID")))
                            {
                                final ArrayList matches = new ArrayList();
                                
                                matches.add(BaseElement.GetGeneratedItem(match));
                                
                                context.put("matches",matches);
                                TurbineUtils.SetEditItem(found,data);
                                if (data.getParameters().getString("edit_screen") !=null)
                                {
                                    data.setScreenTemplate(data.getParameters().getString("edit_screen"));
                                }
                                if(TurbineUtils.HasPassedParameter("destination", data))data.getParameters().add("destination", (String)TurbineUtils.GetPassedParameter("destination", data));
                                data.addMessage("Matched previous subject. Save aborted.");
                                return;
                            }
                        }
                    }
                }
            }
           
            if (! TurbineUtils.getUser(data).canCreate(found))
            {
                TurbineUtils.SetEditItem(found,data);
                if(TurbineUtils.HasPassedParameter("destination", data))data.getParameters().add("destination", (String)TurbineUtils.GetPassedParameter("destination", data));
                data.addMessage("Invalid create permissions for this item.");
                if (data.getParameters().getString("edit_screen") !=null)
                {
                    data.setScreenTemplate(data.getParameters().getString("edit_screen"));
                }
                return;
            }
            
            
            if (subject.getId()==null)
            {
                //ASSIGN A PARTICIPANT ID
                String s = XnatSubjectdata.CreateNewID();
                found.setProperty("xnat:subjectData.ID",s);
            }
            
            boolean removedReference = false;
            XFTItem first = (XFTItem)found;

            Object[] keysArray = data.getParameters().getKeys();
            for (int i=0;i<keysArray.length;i++)
            {
                final String key = (String)keysArray[i];
                if (key.toLowerCase().startsWith("remove_"))
                {
                    final int index = key.indexOf("=");
                    final String field = key.substring(index+1);
                    final Object value = data.getParameters().getObject(key);
                    logger.debug("FOUND REMOVE: " + field + " " + value);
                    final ItemCollection items =ItemSearch.GetItems(field,value,TurbineUtils.getUser(data),false);
                    if (items.size() > 0)
                    {
                    	final ItemI toRemove = items.getFirst();
                        DBAction.RemoveItemReference(first.getItem(),null,toRemove.getItem(),TurbineUtils.getUser(data));
                        first.removeItem(toRemove);
                        removedReference = true;
                    }else{
                        logger.debug("ITEM NOT FOUND:" + key + "="+ value);
                    }
                }
            }

            if (removedReference)
            {
                data.getSession().setAttribute("edit_item",first);
                if(TurbineUtils.HasPassedParameter("destination", data))data.getParameters().add("destination", (String)TurbineUtils.GetPassedParameter("destination", data));
                if (data.getParameters().getString("edit_screen") !=null)
                {
                    data.setScreenTemplate(data.getParameters().getString("edit_screen"));
                }
                return;
            }
            
            final ValidationResults vr = found.validate();
            
            if (vr != null && !vr.isValid())
            {
                TurbineUtils.SetEditItem(first,data);
                context.put("vr",vr);
                if(TurbineUtils.HasPassedParameter("destination", data))data.getParameters().add("destination", (String)TurbineUtils.GetPassedParameter("destination", data));
                if (data.getParameters().getString("edit_screen") !=null)
                {
                    data.setScreenTemplate(data.getParameters().getString("edit_screen"));
                }
            }else{
            	try {
            		found.save(TurbineUtils.getUser(data),false,false);
            		
					MaterializedView.DeleteByUser(user);
					
            		ItemI temp1 =found.getCurrentDBVersion(false);
            		if (temp1 != null)
            		{
            		    first = (XFTItem)temp1;
            		}
            	} catch (Exception e) {
            		logger.error("Error Storing " + found.getXSIType(),e);
            		
            		data.setMessage("Error Saving item.");
                    TurbineUtils.SetEditItem(found,data);
                    if(TurbineUtils.HasPassedParameter("destination", data))data.getParameters().add("destination", (String)TurbineUtils.GetPassedParameter("destination", data));
                    
                    if (data.getParameters().getString("edit_screen") !=null)
                    {
                        data.setScreenTemplate(data.getParameters().getString("edit_screen"));
                    }
                    return;
            	}
            	final SchemaElement se = SchemaElement.GetElement(first.getXSIType());
            	data = TurbineUtils.setDataItem(data,first);
            	data = TurbineUtils.SetSearchProperties(data,first);
            	if (TurbineUtils.HasPassedParameter("source", data))
            	{
                	data.setScreenTemplate((String)TurbineUtils.GetPassedParameter("source", data));
                	data.getParameters().add("confirmed","true");
            	}else if(TurbineUtils.HasPassedParameter("destination", data)){
                    this.redirectToReportScreen((String)TurbineUtils.GetPassedParameter("destination", data), first, data);
            	}else{
                    this.redirectToReportScreen(first, data);
            	}
            }
        } catch (Exception e) {
            logger.error("",e);
            data.setMessage("Unknown Error.");
            if(TurbineUtils.HasPassedParameter("destination", data))data.getParameters().add("destination", (String)TurbineUtils.GetPassedParameter("destination", data));
            TurbineUtils.SetEditItem(found,data);
            if (data.getParameters().getString("edit_screen") !=null)
            {
                data.setScreenTemplate(data.getParameters().getString("edit_screen"));
            }
        }
    }

}
