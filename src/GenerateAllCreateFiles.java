import java.io.File;
import java.sql.SQLException;
import java.util.Hashtable;

import org.nrg.xdat.XDAT;
import org.nrg.xft.XFT;
import org.nrg.xft.commandPrompt.CommandPromptTool;
import org.nrg.xft.exception.DBPoolException;
import org.nrg.xft.exception.ElementNotFoundException;
import org.nrg.xft.exception.FieldNotFoundException;
import org.nrg.xft.exception.XFTInitException;
import org.nrg.xft.generators.JavaFileGenerator;
import org.nrg.xft.generators.JavaScriptGenerator;

/*
 * GenerateAllCreateFiles
 * XNAT http://www.xnat.org
 * Copyright (c) 2014, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 *
 * Last modified 7/1/13 9:12 AM
 */



public class GenerateAllCreateFiles extends CommandPromptTool {
    public GenerateAllCreateFiles(String[] args)
    {
        super(args);
    }
    
    public static void main(String[] args) {
        GenerateAllCreateFiles b = new GenerateAllCreateFiles(args);    
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
        this.addPossibleVariable("javadir","Root directory of Java Source.",false);
        this.addPossibleVariable("sqlfile","SQL Output.",false);
        this.addPossibleVariable("templateDir","Root directory of Templates.",false);
        this.addPossibleVariable("javascriptDir","Root directory of Java script Source.",false);
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
        return "Function used to generate files objects which allow for easy access and customization of data.\n";
    }
    /* (non-Javadoc)
     * @see org.nrg.xft.commandPrompt.CommandPromptTool#getName()
     */
    public String getName() {
        return "GenerateAllCreateFiles";
    }
    
    public void process()
    {
        Hashtable hash = variables;
        try {
            //System.out.print(elementName + ":" + selectType + ":" + output);
                        
            String javaDir = directory;

            if (hash.get("javadir") != null)
                javaDir = (String)hash.get("javadir");
            
            if (! javaDir.endsWith(File.separator))
                javaDir += File.separator;
            

            String sqlfile = directory;

            if (hash.get("sqlfile") != null)
                sqlfile = (String)hash.get("sqlfile");
            
            if (! sqlfile.endsWith(File.separator))
                sqlfile += File.separator;
            

            String javascriptdir = directory;

            if (hash.get("javascriptDir") != null)
                javascriptdir = (String)hash.get("javascriptDir");
            
            if (! sqlfile.endsWith(File.separator))
                javascriptdir += File.separator;

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
                        

            if (XFT.VERBOSE)
                System.out.println("Generating files...");
            JavaFileGenerator.GenerateJavaFiles(javaDir,templateDir,true,generateDisplayDocs);
            JavaScriptGenerator.GenerateJSFiles(javascriptdir,false);
            tool.generateSQL(sqlfile);

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
