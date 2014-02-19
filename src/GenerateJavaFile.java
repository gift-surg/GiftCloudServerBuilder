/*
 * GenerateJavaFile
 * XNAT http://www.xnat.org
 * Copyright (c) 2014, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 *
 * Last modified 7/1/13 9:12 AM
 */

import java.io.File;
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
public class GenerateJavaFile extends CommandPromptTool{
//	java -Xmx256M -classpath .;XFT.jar;commons-collections-2.0.jar;commons-configuration-1.0-dev.jar;commons-dbcp-1.0-dev-20020806.jar;commons-lang-1.0.jar;commons-pool-1.0.jar;jakarta-regexp-1.2.jar;log4j-1.2.6.jar;parser.jar;pg74.214.jdbc2.jar;stratum-1.0-b3.jar;xerces-J_1.4.0.jar XFTApp
    public GenerateJavaFile(String[] args)
	{
	    super(args);
	}
    
    public static void main(String[] args) {
        GenerateJavaFile b = new GenerateJavaFile(args);	
		return;
	}
	
	public boolean requireLogin()
    {
        return false;
    }
    
//    public static void main(String[] args) {
//			Hashtable hash = new Hashtable();
//	
//			if (args.length <1){
//				showUsage();
//				return;
//			}
//			
//			for(int i=0; i<args.length; i++){		
//				if (args[i].equalsIgnoreCase("-instance")) {
//					if (i+1 < args.length) 
//					    hash.put("instance",args[i+1]);
//				}		
//				if (args[i].equalsIgnoreCase("-project")) {
//					if (i+1 < args.length) 
//					    hash.put("project",args[i+1]);
//				}			
//				if (args[i].equalsIgnoreCase("-xdir")) {
//					if (i+1 < args.length) 
//					    hash.put("xdir",args[i+1]);
//				}	
//				
//				if (args[i].equalsIgnoreCase("-javaDir") ) {
//					if (i+1 < args.length) 
//						hash.put("dir",args[i+1]);
//				}
//			
//				if (args[i].equalsIgnoreCase("-e") ) {
//					if (i+1 < args.length) 
//						hash.put("element",args[i+1]);
//				}
//				
//				if (args[i].equalsIgnoreCase("-skipXDAT") ) {
//					if (i+1 < args.length) 
//						hash.put("skipXDAT",args[i+1]);
//				}
//				
//				if (args[i].equalsIgnoreCase("-displayDocs") ) {
//					if (i+1 < args.length) 
//						hash.put("displayDocs",args[i+1]);
//				}
//				
//				if (args[i].equalsIgnoreCase("-templateDir") ) {
//					if (i+1 < args.length) 
//						hash.put("templateDir",args[i+1]);
//				}
//				
//				if (args[i].equalsIgnoreCase("-quiet") ) {
//					XFT.VERBOSE=false;
//				}	
//				
//
//				if (args[i].equalsIgnoreCase("-h") || args[i].equalsIgnoreCase("-help") ) {
//					showUsage();
//					return;
//				}		
//			}
//			
//			Process(hash);
//			return;
//	}	
	
    /* (non-Javadoc)
     * @see org.nrg.xft.commandPrompt.CommandPromptTool#definePossibleVariables()
     */
    public void definePossibleVariables() {
        this.addPossibleVariable("element","schema data type to generate.","e",true);
        this.addPossibleVariable("dir","Root directory of Java Source.","javaDir",false);
        this.addPossibleVariable("templateDir","Root directory of Templates.",false);
        this.addPossibleVariable("skipXDAT","Skip the xdat data types.",false);
        this.addPossibleVariable("displayDocs","if (true), XDAT will generate default display.xml files for each root level data type.",false);
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
        return "Function used to generate item wrapper objects which allow for easy access and customization of XFTItem usage.\n";
    }
    /* (non-Javadoc)
     * @see org.nrg.xft.commandPrompt.CommandPromptTool#getName()
     */
    public String getName() {
        return "GenerateJavaFile";
    }
    
	public void process()
	{
	    Hashtable hash = variables;
		try {
			//System.out.print(elementName + ":" + selectType + ":" + output);
		    			
			String dir = directory;
			
			if (hash.get("dir") != null)
				dir = (String)hash.get("dir");
			
			if (! dir.endsWith(File.separator))
				dir += File.separator;
			
			boolean skipXDAT = false;
			if (hash.get("skipXDAT") != null)
			{
			    if (hash.get("skipXDAT").toString().equals("true"))
			    {
				    skipXDAT = true;
			    }else{
				    skipXDAT = false;
			    }
			}
			

			boolean generateDisplayDocs = false;
			if (hash.get("displayDocs") != null)
			{
			    if (hash.get("displayDocs").toString().equals("true"))
			    {
			        generateDisplayDocs = true;
			    }else{
			        generateDisplayDocs = false;
			    }
			}
			
			String templateDir = directory;
			if (hash.get("templateDir") != null)
			    templateDir = (String)hash.get("templateDir");
			
			if (! templateDir.endsWith(File.separator))
			    templateDir += File.separator;
			
			String elementName = (String)hash.get("element");
			if (elementName.equalsIgnoreCase("all"))
			{
			    if (XFT.VERBOSE)
                    System.out.println("Generating files...");
			    JavaFileGenerator.GenerateJavaFiles(dir,templateDir,skipXDAT,generateDisplayDocs);
			    if (XFT.VERBOSE)
                    System.out.println("Files generated in: " + dir);
			}else{

				boolean valid = XFTTool.ValidateElementName(elementName);
				if (! valid)
				{
					System.out.println("\nERROR:  Invalid Element '" + elementName + "'");
					System.exit(0);
				}
				
				JavaFileGenerator jfg = new JavaFileGenerator();
				GenericWrapperElement e=GenericWrapperElement.GetElement(elementName);
				
				jfg.generateJavaFile(e,dir);
				if (XFT.VERBOSE)
                    System.out.println("Files generated in: " + dir);
			}
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
}
