/*
 * CreateSQL
 * XNAT http://www.xnat.org
 * Copyright (c) 2014, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 *
 * Last modified 7/1/13 9:12 AM
 */


import java.sql.SQLException;

import org.nrg.xft.XFT;
import org.nrg.xft.commandPrompt.CommandPromptTool;
public class CreateSQL  extends CommandPromptTool{
//	java -Xmx256M -classpath .;XFT.jar;commons-collections-2.0.jar;commons-configuration-1.0-dev.jar;commons-dbcp-1.0-dev-20020806.jar;commons-lang-1.0.jar;commons-pool-1.0.jar;jakarta-regexp-1.2.jar;log4j-1.2.6.jar;parser.jar;pg74.214.jdbc2.jar;stratum-1.0-b3.jar;xerces-J_1.4.0.jar XFTApp
    public CreateSQL(String[] args)
	{
	    super(args);
	}
    
    public static void main(String[] args) {
        CreateSQL b = new CreateSQL(args);	
		return;
	}		
	
    /* (non-Javadoc)
     * @see org.nrg.xft.commandPrompt.CommandPromptTool#definePossibleVariables()
     */
    public void definePossibleVariables() {
        this.addPossibleVariable("output","specify file location for data output 'C:\\Temp\\test.sql'","f",true);
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
        return "Function used to generate the sql create statements for all elements in the specified schemas.\n";
    }
    
    /* (non-Javadoc)
     * @see org.nrg.xft.commandPrompt.CommandPromptTool#getName()
     */
    public String getName() {
        return "CreateSQL";
    }
	
	public void process()
	{
		try {
			//System.out.print(elementName + ":" + selectType + ":" + output);
		    String output = (String)variables.get("output");	
			tool.generateSQL(output);
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
