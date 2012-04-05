//Copyright 2007 Washington University School of Medicine All Rights Reserved
/*
 * Created on Aug 22, 2007
 *
 */
package org.nrg.xnat.turbine.modules.actions;

import java.sql.SQLException;

import org.apache.log4j.Logger;
import org.apache.turbine.util.RunData;
import org.apache.velocity.context.Context;
import org.nrg.xdat.base.BaseElement;
import org.nrg.xdat.om.XnatExperimentdata;
import org.nrg.xdat.om.XnatSubjectassessordata;
import org.nrg.xdat.om.XnatSubjectdata;
import org.nrg.xdat.schema.SchemaElement;
import org.nrg.xdat.turbine.modules.actions.DisplayItemAction;
import org.nrg.xdat.turbine.modules.actions.ModifyItem;
import org.nrg.xdat.turbine.utils.PopulateItem;
import org.nrg.xdat.turbine.utils.TurbineUtils;
import org.nrg.xft.ItemI;
import org.nrg.xft.XFT;
import org.nrg.xft.XFTItem;
import org.nrg.xft.collections.ItemCollection;
import org.nrg.xft.db.DBAction;
import org.nrg.xft.db.MaterializedView;
import org.nrg.xft.exception.DBPoolException;
import org.nrg.xft.exception.InvalidPermissionException;
import org.nrg.xft.search.ItemSearch;
import org.nrg.xft.utils.SaveItemHelper;
import org.nrg.xft.utils.ValidationUtils.ValidationResults;

public class ModifySubjectAssessorData extends ModifyItem{
    static Logger logger = Logger.getLogger(ModifySubjectAssessorData.class);

    /* (non-Javadoc)
     * @see org.apache.turbine.modules.actions.VelocityAction#doPerform(org.apache.turbine.util.RunData, org.apache.velocity.context.Context)
     */
    public void doPerform(RunData data, Context context) throws Exception {
        XFTItem found = null;
        try {
            String element0 = ((String)org.nrg.xdat.turbine.utils.TurbineUtils.GetPassedParameter("element_0",data));
            if (element0==null){
                this.handleException(data, null, new Exception("Configuration Exception.<br><br> Please create an &lt;input&gt; with name 'ELEMENT_0' and value 'SOME XSI:TYPE' in your form.  This will tell the Submit action, which data type it is looking for."));
            }
            
            PopulateItem populater = PopulateItem.Populate(data,element0,false);
            
            found = populater.getItem();

            if (populater.hasError())
            {
                handleException(data,(XFTItem)found,populater.getError());
                return;
            }
            
            try {
                preProcess(found,data,context);
            } catch (RuntimeException e1) {
                logger.error("",e1);
            }
            
            
            XFTItem dbVersion = null;
            boolean removedReference = false;
            Object[] keysArray = data.getParameters().getKeys();
            for (int i=0;i<keysArray.length;i++)
            {
                String key = (String)keysArray[i];
                if (key.toLowerCase().startsWith("remove_"))
                {
                    if (dbVersion ==null)
                    {
                        dbVersion = found.getCurrentDBVersion();
                    }
                    int index = key.indexOf("=");
                    String field = key.substring(index+1);
                    Object value = org.nrg.xdat.turbine.utils.TurbineUtils.GetPassedParameter(key,data);
                    logger.debug("FOUND REMOVE: " + field + " " + value);
                    ItemCollection items =ItemSearch.GetItems(field,value,TurbineUtils.getUser(data),false);
                    if (items.size() > 0)
                    {
                        ItemI toRemove = items.getFirst();
                        SaveItemHelper.unauthorizedRemoveChild(dbVersion.getItem(),null,toRemove.getItem(),TurbineUtils.getUser(data));
                        found.removeItem(toRemove);
                        removedReference = true;
                    }else{
                        logger.debug("ITEM NOT FOUND:" + key + "="+ value);
                    }
                }
            }

            if (removedReference)
            {
                TurbineUtils.SetEditItem(found,data);
                if (((String)org.nrg.xdat.turbine.utils.TurbineUtils.GetPassedParameter("edit_screen",data)) !=null)
                {
                    data.setScreenTemplate(((String)org.nrg.xdat.turbine.utils.TurbineUtils.GetPassedParameter("edit_screen",data)));
                }
                return;
            }
            
            XnatSubjectassessordata sa = (XnatSubjectassessordata)BaseElement.GetGeneratedItem(found);
            
            XnatSubjectdata s = sa.getSubjectData();
            if ((sa.getId()==null || sa.getId().equals("")) && s!=null){
                sa.setId(XnatExperimentdata.CreateNewID());
            }
            
            if (sa.getProject()!=null){
                data.getParameters().setString("project", sa.getProject());
            }
            
            ValidationResults vr = found.validate();
            
            if (vr.isValid())
            {
                try {
                    if(!TurbineUtils.getUser(data).canEdit(sa)){
                    	error(new InvalidPermissionException("Unable to modify experient "+ sa.getId()),data);
                    	return;
                    }
                	
                    SaveItemHelper.authorizedSave(found,TurbineUtils.getUser(data),false,allowDataDeletion());
                    
					MaterializedView.DeleteByUser(TurbineUtils.getUser(data));

                    
            		try {
            			MaterializedView.DeleteByUser(TurbineUtils.getUser(data));
            		} catch (DBPoolException e) {
            			e.printStackTrace();
            		} catch (SQLException e) {
            			e.printStackTrace();
            		} catch (Exception e) {
            			e.printStackTrace();
            		}

                    found = found.getCurrentDBVersion(false);

                    

                    SchemaElement se = SchemaElement.GetElement(found.getXSIType());
                    if (TurbineUtils.HasPassedParameter("destination", data)){
                        super.redirectToReportScreen((String)TurbineUtils.GetPassedParameter("destination", data), found, data);
                    }else{
                        redirectToReportScreen(DisplayItemAction.GetReportScreen(se), found, data);
                    }
                } catch (Exception e) {
                    handleException(data,(XFTItem)found,e);
                }
            }else{
                TurbineUtils.OutputPassedParameters(data,context,this.getClass().getName());
                logger.error(vr.toString());
                context.put("vr",vr);
                handleException(data,(XFTItem)found,null);
            }
        } catch (Exception e) {
            logger.error("",e);
            data.setMessage("Error: Item save failed.  See log for details.");
            handleException(data,(XFTItem)found,null);
        }
    }

    /* (non-Javadoc)
     * @see org.nrg.xdat.turbine.modules.actions.ModifyItem#preProcess(org.nrg.xft.XFTItem, org.apache.turbine.util.RunData, org.apache.velocity.context.Context)
     */
    @Override
    public void preProcess(XFTItem item, RunData data, Context context) {
        super.preProcess(item, data, context);
    }

    /* (non-Javadoc)
     * @see org.nrg.xdat.turbine.modules.actions.ModifyItem#handleException(org.apache.turbine.util.RunData, org.nrg.xft.XFTItem, java.lang.Throwable)
     */
    @Override
    public void handleException(RunData data, XFTItem first, Throwable error) {

        try {
            if (first!=null){
                String part_id = ((String)org.nrg.xdat.turbine.utils.TurbineUtils.GetPassedParameter("part_id",data));
                if (part_id==null){
                    if (first.getStringProperty("subject_ID")!=null){
                        part_id = first.getStringProperty("subject_ID");
                    }
                }
                if (part_id!=null){
                    ItemI part = ItemSearch.GetItems("xnat:subjectData.ID",part_id,TurbineUtils.getUser(data),false).getFirst();
                    TurbineUtils.SetParticipantItem(part,data);
                }
                TurbineUtils.SetEditItem(first,data);
            }
            if (error!=null)
                data.setMessage(error.getMessage());
            if (first!=null)
                data.setScreenTemplate("XDATScreen_edit_" + first.getGenericSchemaElement().getFormattedName() + ".vm");
            else
                data.setScreenTemplate("Index.vm");
        } catch (Exception e) {
            logger.error("",e);
            super.handleException(data, first, e);
        }
    }
    
    
}
