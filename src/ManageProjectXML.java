/*
 * ManageProjectXML
 * XNAT http://www.xnat.org
 * Copyright (c) 2014, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 *
 * Last modified 7/1/13 9:12 AM
 */
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.Task;

import java.io.*;

public class ManageProjectXML extends Task {
    public static final String FILE_SEPARATOR = System.getProperty("file.separator");
	private String src = null;
	private String dest = null;
    private String version = null;
    private String basedir = null;
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
		handleOutput("Creating/Replacing " + dest + " with merged version.");
		try {
			outputToFile(sbSRC.toString().replaceAll("%PROJECT%", getProjectName()).replaceAll("%VERSION%", getVersion()), dest);
		} catch (IOException ioe) {
			throw new BuildException(ioe);
		}
	}

    private void addProjectSpecificResources(StringBuilder sbDEST) {
		// Add project-specific resources if defined
		if (projdepsrc == null) {
			return;
		}
        if (projdepsrc.trim().isEmpty()) {
            handleOutput("No value set for projdepsrc attribute, exiting without merging.");
			return;
		}

        DirectoryScanner scanner = new DirectoryScanner();
        if (basedir != null && basedir.trim().length() > 0) {
            handleOutput("Looking for project dependencies in base directory: " + basedir);
            scanner.setBasedir(basedir);
        }
        scanner.setIncludes(projdepsrc.split("\\s*,\\s*"));
        scanner.setCaseSensitive(false);
        scanner.scan();
        String[] files = scanner.getIncludedFiles();
        if (files == null || files.length == 0) {
            handleOutput("No files matching indicated patterns found, exiting without merging.");
            return;
        }

        final StringBuilder sbPDEPSRC = new StringBuilder();
        for (String file : files) {
            File pDepSrcF = getFileFromBasedir(file);
            if (!pDepSrcF.exists()) {
                handleOutput("Couldn't find file indicated by " + file + ", skipping.");
                continue;
            }
            handleOutput("Processing contents of dependency file: "+ pDepSrcF.getName());
            sbPDEPSRC.append(readFileToSB(pDepSrcF));
        }

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
	 * @return the version
	 * 
	 */
	public String getVersion() {
		return version;
	}
	/**
	 * 
	 * @param version
	 *            the version to set
	 * 
	 */
	public void setVersion(String version) {
		this.version = version;
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
     * @return The base directory for the project dependency sources.
     */
    public String getBasedir() {
        return basedir;
    }
    /**
     * @param basedir    The base directory from which project dependencies should be pulled.
     */
    public void setBasedir(String basedir) {
        this.basedir = basedir;
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
			_outFileStream.close();
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
	
    private File getFileFromBasedir(final String file) {
        return new File((basedir != null && basedir.trim().length() > 0) ? basedir + FILE_SEPARATOR + file : file);
}
}
