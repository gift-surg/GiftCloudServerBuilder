import java.io.File;
import java.sql.SQLException;
import java.util.Hashtable;

import org.nrg.xft.XFT;
import org.nrg.xft.XFTTool;
import org.nrg.xft.commandPrompt.CommandPromptTool;
import org.nrg.xft.db.DBAction;
import org.nrg.xft.exception.DBPoolException;
import org.nrg.xft.exception.ElementNotFoundException;
import org.nrg.xft.exception.FieldNotFoundException;
import org.nrg.xft.exception.XFTInitException;
import org.nrg.xft.generators.JavaBeanGenerator;
import org.nrg.xft.schema.Wrappers.GenericWrapper.GenericWrapperElement;

/*
 * InsertMetaDatas
 * XNAT http://www.xnat.org
 * Copyright (c) 2014, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 *
 * Last modified 7/1/13 9:12 AM
 */



public class InsertMetaDatas  extends CommandPromptTool{
//  java -Xmx256M -classpath .;XFT.jar;commons-collections-2.0.jar;commons-configuration-1.0-dev.jar;commons-dbcp-1.0-dev-20020806.jar;commons-lang-1.0.jar;commons-pool-1.0.jar;jakarta-regexp-1.2.jar;log4j-1.2.6.jar;parser.jar;pg74.214.jdbc2.jar;stratum-1.0-b3.jar;xerces-J_1.4.0.jar XFTApp
    public InsertMetaDatas(String[] args)
    {
        super(args);
    }
    
    public static void main(String[] args) {
        InsertMetaDatas b = new InsertMetaDatas(args);    
        return;
    }
    
    public boolean requireLogin()
    {
        return false;
    }
    
    
    /* (non-Javadoc)
     * @see org.nrg.xft.commandPrompt.CommandPromptTool#definePossibleVariables()
     */
    public void definePossibleVariables() {
        this.addPossibleVariable("element","schema data type to generate.","e",false);
    }
    /* (non-Javadoc)
     * @see org.nrg.xft.commandPrompt.CommandPromptTool#getAdditionalUsageInfo()
     */
    public String getAdditionalUsageInfo() {
        return "";
    }
    /* (non-Javadoc)
     * @see org.nrg.xft.commandPrompt.CommandPromptTool#getDescription()
     */
    public String getDescription() {
        return "Function used to insert meta data rows for any table entries which may be missing them.\n";
    }
    /* (non-Javadoc)
     * @see org.nrg.xft.commandPrompt.CommandPromptTool#getName()
     */
    public String getName() {
        return "InsertMetaDatas";
    }
    
    public void process()
    {
        Hashtable hash = variables;
        try {
            //System.out.print(elementName + ":" + selectType + ":" + output);
                        
                        
            String elementName = (String)hash.get("element");
            if (elementName==null || elementName.equalsIgnoreCase("all"))
            {
            if (XFT.VERBOSE)
                System.out.println("Insert meta datas...");
                 DBAction.InsertMetaDatas();
            }else{
                if (XFT.VERBOSE)
                    System.out.println("Insert meta datas...");
                     DBAction.InsertMetaDatas(elementName);
            }
        } catch (Exception e) {
        e.printStackTrace();
    }finally{
        try {
            XFT.closeConnections();
        } catch (SQLException e1) {
        }
    }
        return;
    }
}
