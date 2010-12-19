import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;
//Copyright 2007 Washington University School of Medicine All Rights Reserved
/*
 * Created on May 8, 2007
 *
 */
public class ManageProjectXML extends Task {
	private String src = null;
	private String dest = null;
	private String projdepsrc = null;
	private String projectName = null;
	/*
	 * (non-Javadoc)
	 * 
	 * @see org.apache.tools.ant.Task#execute()
	 * 
	 */
	public void execute() throws BuildException {
		final File srcF = new File(src);
		final StringBuilder sbSRC = readFileToSB(srcF);
		addProjectSpecificResources(sbSRC);
		System.out.println("Creating/Replacing " + dest + " with merged version.");
		try {
			outputToFile(sbSRC.toString().replaceAll("%PROJECT%",projectName), dest);
		} catch (IOException ioe) {
			throw new BuildException(ioe);
		}
	}
		
	private void addProjectSpecificResources(StringBuilder sbDEST) {
		// Add project-specific resources if defined
		if (projdepsrc == null) {
			return;
		}
		File pDepSrcF =  new File(projdepsrc);
		if (!pDepSrcF.exists()) {
			return;
		}
		final StringBuilder sbPDEPSRC = readFileToSB(pDepSrcF);
		int depEndTagLoc = sbDEST.lastIndexOf("</dependencies>");
		try {
			while (sbDEST.charAt(depEndTagLoc-1) == '	' || sbDEST.charAt(depEndTagLoc-1) == ' ') {
				depEndTagLoc--;
			}
		} catch (IndexOutOfBoundsException e) {
			// Do nothing
		}
		if (depEndTagLoc>=0) {
			sbDEST.insert(depEndTagLoc, sbPDEPSRC);
				
			}
		}

	/**
	 * 
	 * @return the dest
	 * 
	 */
	public String getDest() {
		return dest;
	}
	/**
	 * 
	 * @param dest
	 *            the dest to set
	 * 
	 */
	public void setDest(String dest) {
		this.dest = dest;
	}
	/**
	 * 
	 * @return the src
	 * 
	 */
	public String getSrc() {
		return src;
	}
	/**
	 * 
	 * @param src
	 *            the src to set
	 * 
	 */
	public void setSrc(String src) {
		this.src = src;
	}
	/**
	 * 
	 * @return the projdepsrc
	 * 
	 */
	public String getProjdepsrc() {
		return projdepsrc;
	}
	/**
	 * 
	 * @param projdepsrc
	 *            the projdepsrc to set
	 * 
	 */
	public void setProjdepsrc(String projdepsrc) {
		this.projdepsrc = projdepsrc;
	}
	/**
	 * 
	 * @return the projectName
	 * 
	 */
	public String getProjectName() {
		return projectName;
	}
	/**
	 * 
	 * @param projectName
	 *            the projectName to set
	 * 
	 */
	public void setProjectName(String projectName) {
		this.projectName = projectName;
	}
	
	public static void outputToFile(String content, String filePath)
			throws IOException
	{
		File _outFile;
		FileOutputStream _outFileStream;
		PrintWriter _outPrintWriter;
		_outFile = new File(filePath);
		_outFileStream = new FileOutputStream(_outFile);
		// Instantiate and chain the PrintWriter
		_outPrintWriter = new PrintWriter(_outFileStream);
		_outPrintWriter.println(content);
		_outPrintWriter.flush();
		_outPrintWriter.close();
		try
		{
			_outFileStream.close();
		}
		catch (IOException except)
		{
		}
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
	
}
