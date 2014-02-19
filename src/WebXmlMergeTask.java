 /* 
 * ========================================================================
 * 
 * This program is a modification of:
 *    org.apache.cactus.integration.ant.WebXmlMergeTask 
 * That code was licensed under the Apache License, Version 2.0
 * 
 * ========================================================================
 */

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.types.XMLCatalog;
import org.codehaus.cargo.module.webapp.WebXml;
import org.codehaus.cargo.module.webapp.WebXmlIo;
import org.codehaus.cargo.module.webapp.merge.WebXmlMerger;
import org.codehaus.cargo.util.log.AntLogger;
import org.jdom.JDOMException;

public class WebXmlMergeTask extends Task
{
    
    // Instance Variables ------------------------------------------------------
    
    /**
     * Location of the original <code>web.xml</code>.
     */
    private File srcFile;  

    /**
     * Location of the overriding <code>web.xml</code>.
     */
    private File mergeFile;  

    /**
     * Location of the resulting <code>web.xml</code>.
     */
    private File destFile;

    /**
     * Whether the merge should be performed even when the destination file is
     * up to date.
     */
    private boolean force = false;
    
    /**
     * Whether the resulting XML file should be indented.
     */
    private boolean indent = false;
    
    /**
     * The encoding of the resulting XML file.
     */
    private String encoding;

    /**
     * For resolving entities such as DTDs.
     */
    private XMLCatalog xmlCatalog = null;

    // Public Methods ----------------------------------------------------------
    
    /**
     * {@inheritDoc}
     * @see Task#execute()
     */
    public void execute() throws BuildException
    {

        // Modification to return rather than throw build exceptions when files are missing.  We don't
        // expect there to always be a project-specific merge file for an XNAT build.
        if ((this.srcFile == null) || !this.srcFile.isFile() || this.destFile == null) {
            return;
        }
        
        try {

            // The cactus program expected the merging of two valid web.xml files.  While this might 
            // be the case for XNAT, it likely we would rather just specify additions to the plugin-resources
            // web.xml rather than a full file.  Here we read file and add surrounding webapp elements
            // and xml declaration to allow the merge to take place
        	final StringBuilder sbSrc = readFileToSB(this.srcFile);
        	
            if (force || !this.destFile.exists() 
             || (srcFile.lastModified() > destFile.lastModified())
             || (mergeFile!=null && mergeFile.lastModified() > destFile.lastModified())) {
            	// If no mergeFile, just output source to Destination and return
                if (mergeFile == null || !(mergeFile.exists() && mergeFile.isFile())) {
                	final StringBuilder sbMerge = readFileToSB(this.srcFile);
                	outputToFile(sbMerge,destFile);
                	return;
                }
                final StringBuilder sbMerge = readFileToSB(this.mergeFile);
	        	if (sbMerge.indexOf("<web-app>")<0 && sbMerge.indexOf("</web-app>")<0) {
	        		createFromFragments(sbMerge,sbSrc);
	        	}
                WebXml srcWebXml;
                try  {
                    srcWebXml = WebXmlIo.parseWebXml(
                         new ByteArrayInputStream(sbSrc.toString().getBytes()), this.xmlCatalog);
                } 
                catch (JDOMException e)  {
                    throw new BuildException("Unable to get the web.xml " 
                       + "from the specified archive", e);
                }
                WebXml mergeWebXml = null;
                try  {
                    mergeWebXml = WebXmlIo.parseWebXml(
                         new ByteArrayInputStream(sbMerge.toString().getBytes()), this.xmlCatalog);
                } 
                catch (JDOMException e)  {
                    throw new BuildException("Unable to parse the " 
                        + "web.xml from the specified file.", e);
                }
                WebXmlMerger merger = new WebXmlMerger(srcWebXml);
                merger.setLogger(new AntLogger(this));
                merger.merge(mergeWebXml);
                WebXmlIo.writeDescriptor(srcWebXml, this.destFile,
                    this.encoding, this.indent);
            }
            else {
                log("The destination file is up to date",
                    Project.MSG_VERBOSE);
            }
        }
        catch (IOException ioe) {
            throw new BuildException("An I/O error occurred: "
                + ioe.getMessage(), ioe);
        }
    }

	/**
     * Adds an XML catalog to the internal catalog.
     *
     * @param theXmlCatalog the XMLCatalog instance to use to look up DTDs
     */
    public final void addConfiguredXMLCatalog(XMLCatalog theXmlCatalog)
    {
        if (this.xmlCatalog == null)
        {
            this.xmlCatalog = new XMLCatalog();
            this.xmlCatalog.setProject(getProject());
        }
        this.xmlCatalog.addConfiguredXMLCatalog(theXmlCatalog);
    }

    /**
     * The original web deployment descriptor into which the new elements will
     * be merged.
     * 
     * @param theSrcFile the original <code>web.xml</code>
     */
    public final void setSrcFile(File theSrcFile)
    {
        this.srcFile = theSrcFile;
    }

    /**
     * The descriptor to merge into the original file.
     * 
     * @param theMergeFile the <code>web.xml</code> to merge
     */
    public final void setMergeFile(File theMergeFile)
    {
        this.mergeFile = theMergeFile;
    }

    /**
     * The destination file where the result of the merge are stored.
     * 
     * @param theDestFile the resulting <code>web.xml</code>
     */
    public final void setDestFile(File theDestFile)
    {
        this.destFile = theDestFile;
    }
    
    /**
     * Sets whether the merge should be performed even when the destination 
     * file is up to date.
     * 
     * @param isForce Whether the merge should be forced
     */
    public final void setForce(boolean isForce)
    {
        this.force = isForce;
    }

    /**
     * Sets the encoding of the resulting XML file. Default is 'UTF-8'.
     * 
     * @param theEncoding The encoding to set
     */
    public final void setEncoding(String theEncoding)
    {
        this.encoding = theEncoding;
    }

    /**
     * Whether the result XML file should be indented for better readability.
     * Default is 'false'.
     *  
     * @param isIndent Whether the result should be indented
     */
    public final void setIndent(boolean isIndent)
    {
        this.indent = isIndent;
    }

    private static void createFromFragments(StringBuilder sbMerge, StringBuilder sbSrc) {
    	sbMerge.insert(0,sbSrc.substring(0,sbSrc.indexOf("<web-app>")+9));
    	sbMerge.append("</web-app>");
	}
    
	private static StringBuilder readFileToSB(File inFile) {
		StringBuilder sb = new StringBuilder();
		try {
			final FileInputStream in = new FileInputStream(inFile);
			final BufferedReader dis = new BufferedReader(new InputStreamReader(in));
			while (dis.ready()) {
					// Print file line to screen
					sb.append(dis.readLine()).append("\n");
			}
			dis.close();
			return sb;
		} catch (Exception e) {
			throw new BuildException(e);
		}
	}
	

    public static void outputToFile(CharSequence content, File outFile) throws IOException
    {
        FileOutputStream _outFileStream;
        PrintWriter _outPrintWriter;

       _outFileStream = new FileOutputStream ( outFile );

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

}
