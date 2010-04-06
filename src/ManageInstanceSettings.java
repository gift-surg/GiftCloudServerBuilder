import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Iterator;
import java.util.Properties;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;

//Copyright 2007 Washington University School of Medicine All Rights Reserved
/*
 * Created on May 9, 2007
 *
 */

public class ManageInstanceSettings extends Task {
    private String projectDir = null;
    private String originalsDir = null;
    /* (non-Javadoc)
     * @see org.apache.tools.ant.Task#execute()
     */
    @SuppressWarnings("deprecation")
    public void execute() throws BuildException {
        File projectF = new File(projectDir);
        File origF = new File(originalsDir);

        if (projectF.exists() && projectF.isDirectory())
        {
            StringBuffer isTEXT = new StringBuffer();
            try {
                FileInputStream inputS = new FileInputStream(new File(origF,"instanceSettingsProps.properties"));
                Properties props = new Properties();
                props.load(inputS);
                File is = new File(projectF,"InstanceSettings.xml");
                isTEXT = new StringBuffer();
                
                FileInputStream in = new FileInputStream(is);
                DataInputStream dis = new DataInputStream(in);
                while (dis.available() !=0)
                {
                                        // Print file line to screen
                    isTEXT.append(dis.readLine()).append("\n");
                }

                dis.close();

                boolean manipulated = false;
                Iterator iter = props.keySet().iterator();
                while(iter.hasNext())
                {
                    String key = (String)iter.next();
                    if (isTEXT.indexOf(key)==-1){
                        manipulated=true;
                        int xsiIndex = isTEXT.indexOf("xmlns:xsi");
                        isTEXT.insert(xsiIndex, " " + key + "=\"" + props.getProperty(key) + "\" ");
                    }
                }
                
                if (manipulated){
                    System.out.println("Updating " + is.getAbsolutePath());
                    OutputToFile(isTEXT.toString(), is.getAbsolutePath());
                }
            } catch (Throwable e) {
                throw new BuildException(e);
            }
        }
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
     * @return the originalsDir
     */
    public String getOriginalsDir() {
        return originalsDir;
    }


    /**
     * @param originalsDir the originalsDir to set
     */
    public void setOriginalsDir(String originalsDir) {
        this.originalsDir = originalsDir;
    }


    /**
     * @return the projectDir
     */
    public String getProjectDir() {
        return projectDir;
    }


    /**
     * @param projectDir the projectDir to set
     */
    public void setProjectDir(String projectDir) {
        this.projectDir = projectDir;
    }
    
    public void main(String[] args){
        ManageInstanceSettings m = new ManageInstanceSettings();
    }
}
