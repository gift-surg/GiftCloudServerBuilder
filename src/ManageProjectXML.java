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
//Copyright 2007 Washington University School of Medicine All Rights Reserved
/*
 * Created on May 8, 2007
 *
 */
public class ManageProjectXML extends Task {
	private String src = null;
	private String dest = null;
	private String projectName = null;
	/*
	 * (non-Javadoc)
	 * 
	 * @see org.apache.tools.ant.Task#execute()
	 * 
	 */
	@SuppressWarnings("deprecation")
	public void execute() throws BuildException {
		final File srcF = new File(src);
		final File destF = new File(dest);
		
		final StringBuffer sbSRC = new StringBuffer();
		final StringBuffer sbDEST = new StringBuffer();
		try {
			final FileInputStream in = new FileInputStream(srcF);
			final DataInputStream dis = new DataInputStream(in);
			while (dis.available() != 0)
			{
				// Print file line to screen
				sbSRC.append(dis.readLine()).append("\n");
			}
			dis.close();
		} catch (Exception e) {
			throw new BuildException(e);
		}
		try {
			final FileInputStream in = new FileInputStream(destF);
			final DataInputStream dis = new DataInputStream(in);
			while (dis.available() != 0)
			{
				// Print file line to screen
				sbDEST.append(dis.readLine()).append("\n");
			}
			dis.close();
		} catch (Exception e) {
			throw new BuildException(e);
		}
		final int destEnd = sbDEST.indexOf("<!-- END XDAT Dependencies -->");
		final int destStart = sbDEST.indexOf("<!-- = JDBC Drivers");
		if (destEnd == -1 || destStart == -1)
		{
			System.out.println("Replacing " + dest + " with new version.");
			destF.delete();
			final Project project = new Project();
			project.init();
			final Target target = new Target();
			final Copy move = new Copy();
			move.setProject(project);
			move.setOwningTarget(target);
			move.setFile(srcF);
			move.setTofile(destF);
			move.execute();
			final Replace replace = new Replace();
			replace.setProject(project);
			replace.setOwningTarget(target);
			replace.setFile(destF);
			replace.setToken("%PROJECT%");
			replace.setValue(projectName);
			replace.execute();
		} else {
			final int srcEnd = sbSRC.indexOf("<!-- END XDAT Dependencies -->");
			final int srcStart = sbSRC.indexOf("<!-- = JDBC Drivers");
			final String newDependencies = sbSRC.substring(srcStart, srcEnd);
			sbDEST.replace(destStart, destEnd, newDependencies);
			try {
				OutputToFile(sbDEST.toString(), dest);
			} catch (IOException e) {
				throw new BuildException(e);
			}
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
	public static void OutputToFile(String content, String filePath)
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
}
