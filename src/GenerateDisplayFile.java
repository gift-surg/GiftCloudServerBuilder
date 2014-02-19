/*
 * GenerateDisplayFile
 * XNAT http://www.xnat.org
 * Copyright (c) 2014, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 *
 * Last modified 7/1/13 9:12 AM
 */

import java.sql.SQLException;
import java.util.Hashtable;

import org.nrg.xft.XFT;
import org.nrg.xft.XFTTool;
import org.nrg.xft.commandPrompt.CommandPromptTool;
import org.nrg.xft.exception.DBPoolException;
import org.nrg.xft.exception.ElementNotFoundException;
import org.nrg.xft.exception.FieldNotFoundException;
import org.nrg.xft.exception.XFTInitException;
import org.nrg.xft.generators.JavaFileGenerator;
import org.nrg.xft.schema.Wrappers.GenericWrapper.GenericWrapperElement;

/**
 * @author Tim
 *
 */
public class GenerateDisplayFile extends CommandPromptTool{
//	java -Xmx256M -classpath .;XFT.jar;commons-collections-2.0.jar;commons-configuration-1.0-dev.jar;commons-dbcp-1.0-dev-20020806.jar;commons-lang-1.0.jar;commons-pool-1.0.jar;jakarta-regexp-1.2.jar;log4j-1.2.6.jar;parser.jar;pg74.214.jdbc2.jar;stratum-1.0-b3.jar;xerces-J_1.4.0.jar XFTApp
	public GenerateDisplayFile(String[] args)
	{
	    super(args);
	}
    
    public static void main(String[] args) {
        GenerateDisplayFile b = new GenerateDisplayFile(args);	
		return;
	}	
	
	
	
    /* (non-Javadoc)
     * @see org.nrg.xft.commandPrompt.CommandPromptTool#definePossibleVariables()
     */
    public void definePossibleVariables() {
        this.addPossibleVariable("element","schema data type to generate.","e",true);
//        this.addPossibleVariable("dir","Root directory of Java Source.","javaDir",false);
//        this.addPossibleVariable("templateDir","Root directory of Templates.","templateDir",false);
//        this.addPossibleVariable("skipXDAT","Skip the xdat data types.","skipXDAT",false);
//        this.addPossibleVariable("dir","Root directory of Java Source.","javaDir",false);

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
        return "Function used to generate Display Documents which allow for easy creation and customization of Listings.\n";
    }
    /* (non-Javadoc)
     * @see org.nrg.xft.commandPrompt.CommandPromptTool#getName()
     */
    public String getName() {
        return "GenerateDisplayFile";
    }
	
	public void process()
	{
	    Hashtable hash = variables;
		try {
		    			
			String elementName = (String)hash.get("element");
				boolean valid = XFTTool.ValidateElementName(elementName);
				if (! valid)
				{
					System.out.println("\nERROR:  Invalid Element '" + elementName + "'");
					System.exit(0);
				}
				
				JavaFileGenerator jfg = new JavaFileGenerator();
				GenericWrapperElement e=GenericWrapperElement.GetElement(elementName);
				
				jfg.generateDisplayFile(e);

		} catch (ElementNotFoundException e) {
			e.printStackTrace();
		} catch (XFTInitException e) {
			e.printStackTrace();
		} catch (SQLException e) {
			e.printStackTrace();
		} catch (DBPoolException e) {
			e.printStackTrace();
		} catch (FieldNotFoundException e) {
			e.printStackTrace();
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
	
	public boolean requireLogin()
    {
        return false;
    }
}
