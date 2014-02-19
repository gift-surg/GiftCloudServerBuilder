import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Target;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.taskdefs.Copy;
import org.apache.tools.ant.taskdefs.Replace;

/*
 * ManageWebXML
 * XNAT http://www.xnat.org
 * Copyright (c) 2014, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 *
 * Last modified 7/1/13 9:12 AM
 */



public class ManageWebXML extends Task {
    private String specDest = null;
    private String dest = null;
    private String projectName = null;

    
    /* (non-Javadoc)
     * @see org.apache.tools.ant.Task#execute()
     */
    @SuppressWarnings("deprecation")
    public void execute() throws BuildException {
        File destF = new File(dest);

        StringBuffer sbDEST = new StringBuffer();
        
        try {
            FileInputStream in = new FileInputStream(destF);
            DataInputStream dis = new DataInputStream(in);
            while (dis.available() !=0)
            {
                                    // Print file line to screen
                sbDEST.append(dis.readLine()).append("\n");
            }

            dis.close();

        } catch (Exception e) {
            throw new BuildException(e);
        }
        
        int buildSpecIndex = sbDEST.indexOf("BuildSpecServlet");
        if (buildSpecIndex ==-1){
            int index = sbDEST.lastIndexOf("</servlet>") + 10;
            sbDEST.insert(index, getBuildSpecServlet());
            try {
                OutputToFile(sbDEST.toString(), dest);
            } catch (IOException e) {
                throw new BuildException(e);
            }
        }
    }


    /**
     * @return the dest
     */
    public String getDest() {
        return dest;
    }


    /**
     * @param dest the dest to set
     */
    public void setDest(String dest) {
        this.dest = dest;
    }
    

    public static void OutputToFile(String content, String filePath) throws IOException
    {
        File _outFile;
        FileOutputStream _outFileStream;
        PrintWriter _outPrintWriter;

        _outFile = new File(filePath);
        

       _outFileStream = new FileOutputStream ( _outFile );

        // Instantiate and chain the PrintWriter
        _outPrintWriter = new PrintWriter ( _outFileStream );
          
        _outPrintWriter.println(content);
        _outPrintWriter.flush();
          
        _outPrintWriter.close();
        
        try
        {
            _outFileStream.close();
        }
        catch ( IOException except )
        {
        }
    }


    /**
     * @return the projectName
     */
    public String getProjectName() {
        return projectName;
    }


    /**
     * @param projectName the projectName to set
     */
    public void setProjectName(String projectName) {
        this.projectName = projectName;
    }
    
    /**
     * @return the specDest
     */
    public String getBuildSpecDest() {
        return specDest;
    }


    /**
     * @param specDest the specDest to set
     */
    public void setBuildSpecDest(String specDest) {
        this.specDest = specDest;
    }


    public String getBuildSpecServlet(){
        StringBuffer sb = new StringBuffer();
        
        sb.append("\n\t<servlet>");
        sb.append("\n\t\t<servlet-name>BuildSpecServlet</servlet-name>");
        sb.append("\n\t\t<display-name>BuildSpecServlet</display-name>");
        sb.append("\n\t\t<servlet-class>org.nrg.pipeline.servlet.BuildSpecServlet</servlet-class>");
        sb.append("\n\t\t<init-param>");
        sb.append("\n\t\t\t<param-name>instance_settings_directory</param-name>");
        sb.append("\n\t\t\t<param-value>");
        sb.append(getBuildSpecDest());
        sb.append("</param-value>");
        sb.append("\n\t\t</init-param>");
        sb.append("\n\t\t<load-on-startup>1</load-on-startup>");
        sb.append("\n\t</servlet>");
        
        return sb.toString();
    }
}