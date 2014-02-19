/*
 * Search
 * XNAT http://www.xnat.org
 * Copyright (c) 2014, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 *
 * Last modified 7/1/13 9:12 AM
 */


import java.io.File;
import java.net.URL;
import java.sql.SQLException;
import java.util.Calendar;
import java.util.Hashtable;

import org.nrg.xdat.security.XDATUser.FailedLoginException;
import org.nrg.xft.XFT;
import org.nrg.xft.XFTTool;
import org.nrg.xft.commandPrompt.CommandPromptTool;
import org.nrg.xft.exception.DBPoolException;
import org.nrg.xft.exception.ElementNotFoundException;
import org.nrg.xft.exception.FieldNotFoundException;
import org.nrg.xft.exception.XFTInitException;
import org.nrg.xft.utils.StringUtils;
public class Search  extends CommandPromptTool{
	
//	java -Xmx256M -classpath .;XFT.jar;commons-collections-2.0.jar;commons-configuration-1.0-dev.jar;commons-dbcp-1.0-dev-20020806.jar;commons-lang-1.0.jar;commons-pool-1.0.jar;jakarta-regexp-1.2.jar;log4j-1.2.6.jar;parser.jar;pg74.214.jdbc2.jar;stratum-1.0-b3.jar;xerces-J_1.4.0.jar XFTApp
    public Search(String[] args)
	{
	    super(args);
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
        return "Function used to access particular sub-sets of the data in the database.\n";
    }
    /* (non-Javadoc)
     * @see org.nrg.xft.commandPrompt.CommandPromptTool#getName()
     */
    public String getName() {
        return "Search";
    }
	
    /* (non-Javadoc)
     * @see org.nrg.xft.commandPrompt.CommandPromptTool#definePossibleVariables()
     */
    public void definePossibleVariables() {
        addPossibleVariable("field","field to search on - Must be specified using dot syntax from the parent element. (i.e. ClinicalAssessment.neuro.CDR.memory",true);
        addPossibleVariable("value","value to search for.",new String[]{"v","value"},true);
        addPossibleVariable("comparison","(i.e. '=','<','<=','>','>=', or 'LIKE').",new String[]{"c","comparison"},false);
        addPossibleVariable("dataType","Root level data type to return.",new String[]{"e","dataType"},false);
        addPossibleVariable("format","output Format (i.e. 'xml','csv', or 'html').",new String[]{"f","format"},false);
        addPossibleVariable("dir","Directory to store created files.",false);
        addPossibleVariable("limited"," (true or false) Whether or not to limit the content of the returned document.",false);

        addPossibleVariable("pp"," (true or false) Whether or not to 'pretty-print' the xml (not recommended for large files).",false);
    }
    
    public static void main(String[] args) {
        Search b = new Search(args);	
		return;
	}	
    
    public String getService(){
        return "axis/XMLSearch.jws";
    }
    
    public void run() throws DBPoolException,SQLException,FailedLoginException,Exception{
        if (getService()==null)
        {
            _process();
        }else{
            
            boolean URLExists = true;
            try {
                url =new URL(getServiceURL());
                url.getContent();
            } catch (Exception e1) {
                URLExists = false;
    		    if (XFT.VERBOSE)System.out.println("Unable to connect to web service.");
            }
            
            if (URLExists)
            {
                String output = "xml";
    			if (variables.get("format") != null)
    				output = (String)variables.get("format");
    			
    			if (output.equalsIgnoreCase("xml"))
    			{
    			    XMLSearch search = new XMLSearch(_args);
    			}else if (output.equalsIgnoreCase("velocity"))
    			{
    			    VelocitySearch search = new VelocitySearch(_args);
    			}else{
    			    _process();
    			}
            }else{
                _process();
            }
        }
    }
    
	
	public  void process()
	{
	    Hashtable hash = variables;
	    int _return = 0;
		try {
		    File f = new File("");
		    String dir = f.getAbsolutePath();
			
			if (hash.get("dir") != null)
				dir = (String)hash.get("dir");
			
			if (! dir.endsWith(File.separator))
				dir += File.separator;
			
			f = new File(dir);
			if (!f.exists())
			{
			    f.mkdir();
			}
            String pp = (String)variables.get("pp");

            Boolean pretty = Boolean.FALSE;
            if (pp!=null)
            {
                if (pp.equalsIgnoreCase("true"))
                {
                    pretty = Boolean.TRUE;
                }
            }
			
			String comparison = "=";
			if (hash.get("comparison")!=null)
			{
			    comparison = (String)hash.get("comparison");
			}
			
			String elementName = StringUtils.GetRootElementName((String)hash.get("field"));
			
			boolean valid = XFTTool.ValidateElementName(elementName);
			if (! valid)
			{
				System.out.println("\nERROR:  Invalid Element '" + elementName + "'");
				tool.close();
				System.exit(3);
			}

			
			if (hash.get("dataType")!=null)
			{
			    elementName = (String)hash.get("dataType");
			}
			
			valid = XFTTool.ValidateElementName(elementName);
			if (! valid)
			{
				System.out.println("\nERROR:  Invalid Element '" + elementName + "'");
				tool.close();
				System.exit(3);
			}

			String field = (String)hash.get("field");

			String fieldElementName = StringUtils.GetRootElementName((String)hash.get("field"));
			String validElementName = XFTTool.GetValidElementName(fieldElementName);
			if (!validElementName.equals(fieldElementName))
			{
			    field = validElementName + XFT.PATH_SEPERATOR + StringUtils.GetFieldText(field);
			}
			
			Object o = hash.get("value");
	
			if (XFT.VERBOSE)System.out.println("DATA TYPE:" + elementName);
			if (XFT.VERBOSE)System.out.println("FIELD:" + field);
			if (XFT.VERBOSE)System.out.println("VALUE:" + o);
			if (XFT.VERBOSE)System.out.println("COMPARISON:" + comparison);
			
			tool.info("\n\nNEW SEARCH (" + tool.getUser().getUsername() + ")");
			tool.info("DATA TYPE:" + elementName);
			tool.info("FIELD:" + field);
			tool.info("VALUE:" + o);
			tool.info("COMPARISON:" + comparison);
			
			String output = "xml";
			if (hash.get("format") != null)
				output = (String)hash.get("format");
			
			if (output.equalsIgnoreCase("csv"))
			{
			    tool.CSVSearch(field, comparison,o);
			}else if (output.equalsIgnoreCase("xml")){
			    long start = Calendar.getInstance().getTimeInMillis();
			    int i= tool.XMLSearch(elementName,field, comparison,o,dir,false,pretty.booleanValue());
			    if (XFT.VERBOSE)System.out.println("Search Time: " + (Calendar.getInstance().getTimeInMillis()-start) + " ms");
			    try {
		            XFT.closeConnections();
		        } catch (SQLException e1) {
		        }
			    System.exit(i);
			}else if (output.equalsIgnoreCase("velocity")){
			    int i= tool.VelocitySearch(elementName,field, comparison,o);
			    System.exit(i);
			}else
			{
			    
			    tool.HTMLSearch(field, comparison,o);
			}
		} catch (ElementNotFoundException e) {
			e.printStackTrace();
			_return= 4;
		} catch (XFTInitException e) {
			e.printStackTrace();
			_return= 5;
		} catch (SQLException e) {
			e.printStackTrace();
			_return= 6;
		} catch (DBPoolException e) {
			e.printStackTrace();
			_return= 7;
		} catch (FieldNotFoundException e) {
			e.printStackTrace();
			_return= 8;
		} catch (Exception e) {
			e.printStackTrace();
			_return= 9;
		}finally{
		    try {
	            XFT.closeConnections();
	        } catch (SQLException e1) {
	        }
		}
		System.exit(_return);
	}
}
