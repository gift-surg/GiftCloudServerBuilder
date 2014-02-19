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
import org.nrg.xft.generators.JavaBeanGenerator;
import org.nrg.xft.generators.JavaFileGenerator;
import org.nrg.xft.schema.Wrappers.GenericWrapper.GenericWrapperElement;

/*
 * GenerateJavaBeans
 * XNAT http://www.xnat.org
 * Copyright (c) 2014, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 *
 * Last modified 7/1/13 9:12 AM
 */



public class GenerateJavaBeans extends CommandPromptTool{
//  java -Xmx256M -classpath .;XFT.jar;commons-collections-2.0.jar;commons-configuration-1.0-dev.jar;commons-dbcp-1.0-dev-20020806.jar;commons-lang-1.0.jar;commons-pool-1.0.jar;jakarta-regexp-1.2.jar;log4j-1.2.6.jar;parser.jar;pg74.214.jdbc2.jar;stratum-1.0-b3.jar;xerces-J_1.4.0.jar XFTApp
    public GenerateJavaBeans(String[] args)
    {
        super(args);
    }
    
    public static void main(String[] args) {
        GenerateJavaBeans b = new GenerateJavaBeans(args);    
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
        this.addPossibleVariable("element","schema data type to generate.","e",true);
        this.addPossibleVariable("dir","Root directory of Java Source.","javaDir",false);
        this.addPossibleVariable("skipXDAT","Skip the xdat data types.",false);
        this.addPossibleVariable("package","base java package.",false);
        this.addPossibleVariable("allow1.5","allow JAVA Version 1.5+.",false);
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
        return "GenerateJavaBeans";
    }
    
    public void process()
    {
        Hashtable hash = variables;
        try {
            //System.out.print(elementName + ":" + selectType + ":" + output);
                        
            String dir = directory;

            String packag = "";
            if (hash.get("package") != null)
                packag = (String)hash.get("package");
            
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
            
            boolean allow15 = false;
            if (hash.get("allow1.5") != null)
            {
                if (hash.get("allow1.5").toString().equals("true"))
                {
                    allow15 = true;
                }else{
                    allow15 = false;
                }
            }
            
            JavaBeanGenerator.VERSION5=allow15;
                        
            String elementName = (String)hash.get("element");
            if (elementName.equalsIgnoreCase("all"))
            {
                if (XFT.VERBOSE)
                    System.out.println("Generating files...");
                JavaBeanGenerator.GenerateJavaFiles(dir,skipXDAT,packag);
                if (XFT.VERBOSE)
                    System.out.println("Files generated in: " + dir);
            }else{

                boolean valid = XFTTool.ValidateElementName(elementName);
                if (! valid)
                {
                    System.out.println("\nERROR:  Invalid Element '" + elementName + "'");
                    System.exit(0);
                }
                
                JavaBeanGenerator jfg = new JavaBeanGenerator();
                jfg.setProject(packag);
                GenericWrapperElement e=GenericWrapperElement.GetElement(elementName);
                
                jfg.generateJavaBeanFile(e,dir);
                jfg.generateJavaInterface(e,dir);
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