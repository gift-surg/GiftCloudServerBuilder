import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;

/*
 * ManageInstanceSettings
 * XNAT http://www.xnat.org
 * Copyright (c) 2014, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 *
 * Last modified 7/1/13 9:12 AM
 */



public class ManageInstanceSettings extends Task {
    private final static String XSD_REFERENCE = "\t\t<Data_Model File_Name=\"%s\" File_Location=\"%%XDAT_PROJECT%%/schemas/%s\" DB=\"%%DB_NAME%%\"/>";
    private String projectDir = null;
    private String originalsDir = null;
    private String modulesDir = null;
    private StringBuilder settings;
    private String settingsPath;
    private boolean manipulated = false;

    public static void main(String[] args) throws IOException {
    }

    /**
     * @return the originalsDir
     */
    public String getOriginalsDir() {
        return originalsDir;
    }

    /**
     * @param originalsDir
     *            the originalsDir to set
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
     * @param projectDir
     *            the projectDir to set
     */
    public void setProjectDir(String projectDir) {
        this.projectDir = projectDir;
    }

    /**
     * Gets the value set for the modules directory. This directory contains the
     * various archived project modules, as well as the custom overlay folder.
     * 
     * @return The modules directory path.
     */
    public String getModulesDir() {
        return modulesDir;
    }

    /**
     * Sets the value set for the modules directory. This directory contains the
     * various archived project modules, as well as the custom overlay folder.
     * 
     * @param modulesDir
     *            The value to set for the modules directory.
     */
    public void setModulesDir(String modulesDir) {
        this.modulesDir = modulesDir;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.tools.ant.Task#execute()
     */
    public void execute() throws BuildException {
        try {
            loadInstanceSettings();
            processSettingsAttributes();
            addSchemasFromModules();

            if (manipulated) {
                console("Updating %s", settingsPath);
                OutputToFile(settings.toString(), settingsPath);
            }
        } catch (IOException exception) {
            throw new BuildException("Error while writing out: " + settingsPath, exception);
        }
    }

    @SuppressWarnings("deprecation")
    private void loadInstanceSettings() {
        console("Loading instance settings from folder: %s", projectDir);
        File projectF = new File(projectDir);

        if (projectF.exists() && projectF.isDirectory()) {
            try {
                File is = new File(projectF, "InstanceSettings.xml");
                settingsPath = is.getAbsolutePath();

                settings = new StringBuilder();

                FileInputStream in = new FileInputStream(is);
                DataInputStream dis = new DataInputStream(in);
                while (dis.available() != 0) {
                    settings.append(dis.readLine()).append("\n");
                }

                dis.close();
            } catch (Throwable e) {
                throw new BuildException(e);
            }
        }
    }

    /**
     * This method validates the attributes for the Instance_Settings element of
     * the InstanceSettings.xml document and ensures that all required
     * attributes are accounted for. This ensure that even settings documents
     * that have been customized will get all of the required attributes set.
     * 
     * @throws IOException
     */
    private void processSettingsAttributes() {
        File file = new File(new File(originalsDir), "instanceSettingsProps.properties");
        console("Processing settings attributes from properties file: %s", file.getName());

        FileInputStream input = null;
        try {
            input = new FileInputStream(file);
            Properties props = new Properties();
            props.load(input);

            for (Object keyObj : props.keySet()) {
                String key = (String) keyObj;
                if (settings.indexOf(key) == -1) {
                    manipulated = true;
                    int xsiIndex = settings.indexOf("xmlns:xsi");
                    settings.insert(xsiIndex, " " + key + "=\"" + props.getProperty(key) + "\" ");
                }
            }
        } catch (FileNotFoundException exception) {
            throw new BuildException("Couldn't find the file: " + file.getAbsolutePath(), exception);
        } catch (IOException exception) {
            throw new BuildException("Error reading the file: " + file.getAbsolutePath(), exception);
        } finally {
            if (input != null) {
                try {
                    input.close();
                } catch (IOException exception) {
                    throw new BuildException("Error closing the file: " + file.getAbsolutePath(), exception);
                }
            }
        }
    }

    private void addSchemasFromModules() throws IOException {
        if (modulesDir == null || modulesDir.length() == 0) {
            console("Modules dir not found, not adding modules to instance settings.");
            return;
        }

        console("Adding schemas from the modules folder: %s", modulesDir);

        List<String[]> references = getSchemaReferences();

        if (references != null && references.size() > 0) {
            manipulated = true;
            BufferedReader reader = new BufferedReader(new StringReader(settings.toString()));
            StringWriter buffer = new StringWriter();
            PrintWriter writer = new PrintWriter(buffer);

            String line;
            boolean inModulesBlock = false;
            while ((line = reader.readLine()) != null) {
                if (!inModulesBlock) {
                    if (line.contains("<!-- Start modules schemas")) {
                        inModulesBlock = true;
                        writer.println(line);
                        for (String[] reference : references) {
                            console(" *** Creating entry for schema %s in folder %s", reference[0], reference[1]);
                            writer.println(String.format(XSD_REFERENCE, reference[0], reference[1]));
                        }
                    } else {
                        writer.println(line);
                    }
                } else {
                    if (line.contains("<!-- End modules schemas")) {
                        inModulesBlock = false;
                        writer.println(line);
                    }
                }
            }
            settings = new StringBuilder(buffer.toString());
        }
    }

    private List<String[]> getSchemaReferences() {
        File dir = new File(modulesDir);

        if (!dir.exists()) {
            System.err.println("The folder " + modulesDir + " does not exist, please check your configuration settings.");
            return null;
        }

        if (!dir.isDirectory()) {
            System.err.println("The location " + modulesDir + " is not a folder, please check your configuration settings.");
            return null;
        }

        StringBuilder buffer = new StringBuilder(modulesDir);
        if (!modulesDir.endsWith(File.separator)) {
            buffer.append(File.separator);
        }
        buffer.append("src").append(File.separatorChar).append("schemas");

        File schemasDir = new File(buffer.toString());

        if (!schemasDir.exists()) {
            System.err.println("The folder " + buffer.toString() + " does not exist, please check your configuration settings.");
            return null;
        }

        if (!schemasDir.isDirectory()) {
            System.err.println("The location " + buffer.toString() + " is not a folder, please check your configuration settings.");
            return null;
        }

        File[] folders = schemasDir.listFiles(new FileFilter() {
            @Override
            public boolean accept(File item) {
                return item.isDirectory();
            }
        });

        List<String[]> references = new ArrayList<String[]>();
        for (File folder : folders) {
            File[] xsds = folder.listFiles(new FilenameFilter() {
                @Override
                public boolean accept(File dir, String name) {
                    return name.matches(".*\\.xsd");
                }
            });

            for (File xsd : xsds) {
                references.add(new String[] { xsd.getName(), folder.getName()});
            }
        }
        return references;
    }

    private void OutputToFile(String content, String filePath) throws IOException {
        PrintWriter writer = null;

        try {
            // Instantiate and chain the PrintWriter
            writer = new PrintWriter(new FileOutputStream(new File(filePath)));
            writer.println(content);
            writer.flush();
        } catch (IOException exception) {
            console("Error occurred while closing output stream: %s", exception);
        } finally {
            if (writer != null) {
                writer.close();
            }
        }
    }

    /**
     * Outputs a message to the designated console. This abstracts the console
     * output process to allow more easily swapping out console output methods,
     * e.g. using standard out or a logging mechanism.
     * 
     * @param message
     *            The message to display.
     * @param parameters
     *            Any parameters that might be used in a format with the
     *            message.
     */
    private void console(String message, Object... parameters) {
        String display;
        if (parameters != null && parameters.length > 0) {
            display = String.format(message, parameters);
        } else {
            display = message;
        }

        handleOutput(display);
    }
}
