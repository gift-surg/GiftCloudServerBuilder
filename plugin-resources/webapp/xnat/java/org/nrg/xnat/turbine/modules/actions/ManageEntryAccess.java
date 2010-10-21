//Copyright 2007 Washington University School of Medicine All Rights Reserved
/*
 * Created on Aug 28, 2007
 *
 */
package org.nrg.xnat.turbine.modules.actions;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Hashtable;

import org.apache.log4j.Logger;
import org.apache.turbine.modules.ScreenLoader;
import org.apache.turbine.util.RunData;
import org.apache.velocity.context.Context;
import org.nrg.xdat.schema.SchemaElement;
import org.nrg.xdat.turbine.modules.actions.ModifyItem;
import org.nrg.xdat.turbine.modules.actions.ModifyItem.CriticalException;
import org.nrg.xdat.turbine.modules.screens.EditScreenA;
import org.nrg.xdat.turbine.utils.PopulateItem;
import org.nrg.xdat.turbine.utils.TurbineUtils;
import org.nrg.xft.ItemI;
import org.nrg.xft.XFTItem;
import org.nrg.xft.collections.ItemCollection;
import org.nrg.xft.db.DBAction;
import org.nrg.xft.exception.ElementNotFoundException;
import org.nrg.xft.exception.FieldNotFoundException;
import org.nrg.xft.exception.InvalidItemException;
import org.nrg.xft.exception.InvalidValueException;
import org.nrg.xft.exception.XFTInitException;
import org.nrg.xft.search.ItemSearch;
import org.nrg.xft.utils.ValidationUtils.ValidationResults;

public class ManageEntryAccess extends ModifyItem {
    static Logger logger = Logger.getLogger(ManageEntryAccess.class);

    
    public void save(XFTItem first,RunData data, Context context) throws InvalidItemException,Exception{
        ArrayList<XFTItem>al = first.getChildItems("sharing/share");
        for (XFTItem i : al){
            if (first.getGenericSchemaElement().instanceOf("xnat:subjectData")){
                i.setProperty("subject_id", first.getProperty("ID"));
            }else{
                i.setProperty("sharing_share_xnat_experimentda_id", first.getProperty("ID"));
            }
            i.save(TurbineUtils.getUser(data),false,false);
        }
    }
    
    public void doPerform(RunData data, Context context) throws Exception
    {
        XFTItem first =null;
        preserveVariables(data,context);
        //TurbineUtils.OutputPassedParameters(data,context,this.getClass().getName());
        //parameter specifying elementAliass and elementNames
        try {
            String header = "ELEMENT_";
            int counter = 0;
            Hashtable hash = new Hashtable();
            while (data.getParameters().get(header + counter) != null)
            {
                String elementToLoad = data.getParameters().getString(header + counter++);
                Integer numberOfInstances = data.getParameters().getIntObject(elementToLoad);
                if (numberOfInstances != null && numberOfInstances.intValue()!=0)
                {
                    int subCount = 0;
                    while (subCount != numberOfInstances.intValue())
                    {
                        hash.put(elementToLoad + (subCount++),elementToLoad);
                    }
                }else{
                    hash.put(elementToLoad,elementToLoad);
                }
            }
            
            if (hash.size()==0){
                throw new Exception("Missing ELEMENT_0 property.");
            }
            
            String screenName = null;
            if (data.getParameters().getString("edit_screen") !=null && !data.getParameters().getString("edit_screen").equals(""))
            {
                screenName = data.getParameters().getString("edit_screen").substring(0,data.getParameters().getString("edit_screen").lastIndexOf(".vm"));
            }
            
            InvalidValueException error = null;
            ArrayList al = new ArrayList();
            Enumeration keys = hash.keys();
            while(keys.hasMoreElements())
            {
                String key = (String)keys.nextElement();
                String element = (String)hash.get(key);
                SchemaElement e = SchemaElement.GetElement(element);
                
                PopulateItem populater = null;
                if (screenName == null)
                {
                    populater = PopulateItem.Populate(data,element,true);
                }else{
                    if (screenName.equals("XDATScreen_edit_" + e.getFormattedName()))
                    {
                        EditScreenA screen = (EditScreenA) ScreenLoader.getInstance().getInstance(screenName);
                        XFTItem newItem = (XFTItem)screen.getEmptyItem(data);
                        populater = PopulateItem.Populate(data,element,true,newItem);
                    }else{
                        populater = PopulateItem.Populate(data,element,true);
                    }
                }
                
                if (populater.hasError())
                {
                    error = populater.getError();
                }
                
                al.add(populater.getItem());
            }
            first = (XFTItem)al.get(0);
            try {
                preProcess(first,data,context);
            } catch (RuntimeException e1) {
                logger.error("",e1);
            }
            
            if (error!=null)
            {
                handleException(data,first,error);
                return;
            }

//            if (removedReference)
//            {
//                data.getSession().setAttribute(this.getReturnEditItemIdentifier(),first);
//                if (data.getParameters().getString("edit_screen") !=null)
//                {
//                    data.setScreenTemplate(data.getParameters().getString("edit_screen"));
//                }
//                return;
//            }

            ValidationResults vr = null;
            
            if (vr != null)
            {
                data.getSession().setAttribute(this.getReturnEditItemIdentifier(),first);
                context.put("vr",vr);
                if (data.getParameters().getString("edit_screen") !=null)
                {
                    data.setScreenTemplate(data.getParameters().getString("edit_screen"));
                }
            }else{
                try {
                    try {
                        preSave(first.getItem(),data,context);
                    } catch (CriticalException e) {
                        throw e;
                    } catch (RuntimeException e) {
                        logger.error("",e);
                    }
                    save(first,data,context);
                } catch (Exception e) {
                    handleException(data,first,error);
                    return;
                }
                try {
                    postProcessing(first,data,context);
                } catch (Exception e) {
                    logger.error("",e);
                    data.setMessage(e.getMessage());
                }
            }
        } catch (XFTInitException e) {
            handleException(data,first,e);
            return;
        } catch (ElementNotFoundException e) {
            handleException(data,first,e);
            return;
        } catch (FieldNotFoundException e) {
            handleException(data,first,e);
            return;
        } catch (Exception e) {
            handleException(data,first,e);
            return;
        }
    }
}
