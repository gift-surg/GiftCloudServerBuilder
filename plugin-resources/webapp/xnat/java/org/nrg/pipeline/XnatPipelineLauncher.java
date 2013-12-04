/*
 * org.nrg.pipeline.XnatPipelineLauncher
 * XNAT http://www.xnat.org
 * Copyright (c) 2013, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 *
 * Last modified 7/10/13 8:47 PM
 */

package org.nrg.pipeline;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.apache.turbine.util.RunData;
import org.apache.velocity.context.Context;
import org.nrg.pipeline.client.XNATPipelineLauncher;
import org.nrg.viewer.QCImageCreator;
import org.nrg.xdat.XDAT;
import org.nrg.xdat.entities.AliasToken;
import org.nrg.xdat.om.WrkWorkflowdata;
import org.nrg.xdat.om.XnatExperimentdata;
import org.nrg.xdat.om.XnatImagesessiondata;
import org.nrg.xdat.security.XDATUser;
import org.nrg.xdat.services.AliasTokenService;
import org.nrg.xdat.turbine.utils.AdminUtils;
import org.nrg.xdat.turbine.utils.TurbineUtils;
import org.nrg.xft.XFT;
import org.nrg.xft.event.EventUtils;
import org.nrg.xft.security.UserI;
import org.nrg.xnat.turbine.utils.ArcSpecManager;
import org.nrg.xnat.utils.WorkflowUtils;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class XnatPipelineLauncher {
    static org.apache.log4j.Logger logger = Logger.getLogger(XnatPipelineLauncher.class);

    public static final String SCHEDULE = "schedule";
    public static final boolean DEFAULT_RUN_PIPELINE_IN_PROCESS = false;
    public static final boolean DEFAULT_RECORD_WORKFLOW_ENTRIES = true;

    private String pipelineName;
    private String id, label = null;
    private String externalId; // Workflows External Id
    private XDATUser user;
    private String dataType;
    private String host;
    private Set<String> notificationEmailIds = new HashSet<String>();
    private Map<String, List<String>> parameters = new Hashtable<String, List<String>>();
    private String startAt;
    private boolean waitFor;
    private boolean needsBuildDir;
    private boolean supressNotification;
    private String parameterFile;
    private String admin_email;
    private boolean alwaysEmailAdmin = true;
    private boolean useAlias = false;

    private boolean runPipelineInProcess = DEFAULT_RUN_PIPELINE_IN_PROCESS;
    private boolean recordWorkflowEntries = DEFAULT_RECORD_WORKFLOW_ENTRIES;

    /**
     * @return the useAlias
     */
    public boolean useAlias() {
        return useAlias;
    }

    /**
     * @param useAlias Indicates whether the alias should be used.
     */
    public void useAlias(boolean useAlias) {
        this.useAlias = useAlias;
    }

    /**
     * @return the admin_email
     */
    public String getAdmin_email() {
        return admin_email;
    }

    /**
     * @param admin_email
     *            the admin_email to set
     */
    public void setAdmin_email(String admin_email) {
        this.admin_email = admin_email;
    }

    /**
     * @return the alwaysEmailAdmin
     */
    public boolean alwaysEmailAdmin() {
        return alwaysEmailAdmin;
    }

    /**
     * @param alwaysEmailAdmin
     *            the alwaysEmailAdmin to set
     */
    public void setAlwaysEmailAdmin(boolean alwaysEmailAdmin) {
        this.alwaysEmailAdmin = alwaysEmailAdmin;
    }

    /**
     * @return Returns the needsBuildDir.
     */
    public boolean isNeedsBuildDir() {
        return needsBuildDir;
    }

    /**
     * @param needsBuildDir
     *            The needsBuildDir to set.
     */
    public void setNeedsBuildDir(boolean needsBuildDir) {
        this.needsBuildDir = needsBuildDir;
    }

    public void setBuildDir(String path) {
        if (StringUtils.isBlank(path)) {
            return;
        }
        if (path.endsWith(File.separator)) {
            path = path.substring(0, path.length() - 1);
        }

        if (needsBuildDir) {
            parameters.put("builddir", Arrays.asList(path));
        }

        setNeedsBuildDir(false);
    }

    public boolean getRunPipelineInProcess() {
        return runPipelineInProcess;
    }

    public void setRunPipelineInProcess(boolean runPipelineInProcess) {
        this.runPipelineInProcess = runPipelineInProcess;
    }

    public boolean getRecordWorkflowEntries() {
        return recordWorkflowEntries;
    }

    public void setRecordWorkflowEntries(boolean recordWorkflowEntries) {
        this.recordWorkflowEntries = recordWorkflowEntries;
    }

    public XnatPipelineLauncher(RunData data, Context context) {
        user = TurbineUtils.getUser(data);
        host = TurbineUtils.GetFullServerPath();
        startAt = null;
        supressNotification = false;
        this.needsBuildDir = true;
        waitFor = false;
        addUserEmailForNotification();
        parameterFile = null;
    }

    public XnatPipelineLauncher(XDATUser user) {
        this.user = user;
        host = TurbineUtils.GetFullServerPath();
        startAt = null;
        supressNotification = false;
        waitFor = false;
        needsBuildDir = true;
        addUserEmailForNotification();
        parameterFile = null;
    }

    private void addUserEmailForNotification() {
        notify(user.getEmail());
        notify(AdminUtils.getAdminEmailId());
    }

    /*
     * Use this method when you want the job to be executed after schedule
     * command gets hold of the command string. Schedule could log the string
     * into a file and/or submit to a GRID
     */

    public boolean launch() {
        return launch(XFT.GetPipelinePath() + "bin" + File.separator + SCHEDULE);
    }

    /*
     * Setting cmdPrefix to null will launch the job directly.
     */

    public boolean launch(String cmdPrefix) {
        return runPipelineInProcess ? launchInProcessPipelineExecution() : launchExternalPipelineExecution(cmdPrefix);
    }

    private boolean launchInProcessPipelineExecution() {
        boolean success;
        try {
        	Integer workflowPrimaryKey = null;
            if (recordWorkflowEntries) {
            	workflowPrimaryKey = initiateWorkflowEntry();
            }

            List<String> parameters = getPipelineConfigurationArguments();
            parameters.addAll(getCommandLineArguments());
            if (recordWorkflowEntries && workflowPrimaryKey !=null) {
            	parameters.add("-workFlowPrimaryKey "+workflowPrimaryKey);
            }
            XNATPipelineLauncher launcher = new XNATPipelineLauncher(parameters);
            success = launcher.run();
        } catch (Exception exception) {
            logger.error(exception.getMessage() + " for in-process execution of pipeline " + pipelineName + " -pwd ******* ", exception);
            success = false;
        }
        return success;
    }

    private boolean launchExternalPipelineExecution(String cmdPrefix) {

        String command = buildPipelineLauncherScriptCommand(cmdPrefix) + " " + convertArgumentListToCommandLine(getCommandLineArguments());

        boolean success = true;

        try {
            logger.debug("Launching command: " + command + " -pwd ****** -parameter pwd=******");
        	Integer workflowPrimaryKey = null;
            if (recordWorkflowEntries) {
            	workflowPrimaryKey=initiateWorkflowEntry();
            }
            if (recordWorkflowEntries && workflowPrimaryKey !=null) {
            	command += " -workFlowPrimaryKey "+workflowPrimaryKey+" ";
            }

            ProcessLauncher processLauncher = new ProcessLauncher();
            processLauncher.setCommand(command);
            processLauncher.start();
            if (waitFor) {
                while (processLauncher.isAlive()) {
                } // wait for the thread to end
                success = processLauncher.getExitStatus();
            }
            if (!success) {
                logger.error("Couldn't launch " + command + " -pwd ******");
            }
        } catch (Exception e) {
            logger.error(e.getMessage() + " for command " + command + " -pwd ******* ", e);
            success = false;
        }

        return success;
    }

    private String buildPipelineLauncherScriptCommand(String cmdPrefix) {
        String command;
        if (!StringUtils.isBlank(cmdPrefix)) {
            command = cmdPrefix + " ";
        } else {
            command = "";
        }

        command += XFT.GetPipelinePath() + "bin" + File.separator + "XnatPipelineLauncher";

        if (System.getProperty("os.name").toUpperCase().startsWith("WINDOWS")) {
            command += ".bat";
        }
        return command;
    }

    private String convertArgumentListToCommandLine(List<String> arguments) {
        StringBuilder command = new StringBuilder();
        for (String argument : arguments) {
            command.append(argument).append(" ");
        }
        return command.toString().trim();
    }

    /**
     * This builds the arguments for {@link XNATPipelineLauncher} that are
     * contained in the script when launched externally. These are passed in
     * directly to the {@link XNATPipelineLauncher#main(String[])} instead of
     * implicitly through the launcher script.
     *
     * @return The pipeline configuration arguments.
     */
    private List<String> getPipelineConfigurationArguments() {
        List<String> arguments = new ArrayList<String>();
        try {
            String pipelinePath = new File(XFT.GetPipelinePath()).getCanonicalPath();
            boolean requiresQuotes = pipelinePath.contains(" ");
            arguments.add("-config");
            String configPath = pipelinePath + File.separator + "pipeline.config";
            arguments.add(requiresQuotes ? "\"" + configPath + "\"" : configPath);
            arguments.add("-log");
            String logConfigPath = pipelinePath + File.separator + "log4j.properties";
            arguments.add(requiresQuotes ? "\"" + logConfigPath + "\"" : logConfigPath);
            arguments.add("-catalogPath");
            String catalogPath = pipelinePath + File.separator + "catalog";
            arguments.add(requiresQuotes ? "\"" + catalogPath + "\"" : catalogPath);
        } catch (IOException e) {
            // TODO: Do something useful in here
            e.printStackTrace();
        }

        return arguments;
    }

    /**
     * This builds all of the command-line arguments that are standard between
     * in-process and external launch mode. Use the {@link #convertArgumentListToCommandLine(List)}
     * method to convert the returned list to a command line.
     *
     * @return A list of the submitted command-line arguments.
     */
    private List<String> getCommandLineArguments() {
        AliasToken token = XDAT.getContextService().getBean(AliasTokenService.class).issueTokenForUser(user);

        List<String> arguments = new ArrayList<String>();
        arguments.add("-pipeline");
        arguments.add(pipelineName);
        arguments.add("-id");
        arguments.add(id);
        arguments.add("-host");
        arguments.add(host);
        arguments.add("-u");
        arguments.add(token.getAlias());
        arguments.add("-pwd");
        arguments.add(Long.toString(token.getSecret()));
        arguments.add("-dataType");
        arguments.add(dataType);

        if (!recordWorkflowEntries) {
            arguments.add("-recordWorkflow");
            arguments.add("false");
        }

        if (label != null) {
            arguments.add("-label");
            arguments.add(label);
        }

        if (useAlias()) {
            arguments.add("-useAlias");
        }

        if (isSupressNotification()) {
            arguments.add("-supressNotification");
        }

        if (externalId != null) {
            arguments.add("-project");
            arguments.add("\"" + externalId + "\"");
        }

        if (startAt != null) {
            arguments.add("-startAt");
            arguments.add(startAt);
        }

        if (parameterFile != null) {
            arguments.add("-parameterFile");
            arguments.add(parameterFile);
        }

        for (String notificationEmailId : notificationEmailIds) {
            if (!StringUtils.isBlank(notificationEmailId)) {
                arguments.add("-notify");
                arguments.add(notificationEmailId);
            }
        }

        setBuildDir();

        Set<String> params = parameters.keySet();
        for (String param : params) {
            arguments.add("-parameter");
            List<String> values = parameters.get(param);
            StringBuilder paramArg = new StringBuilder(param).append("=");
            for (String value : values) {
                if (!StringUtils.isBlank(value)) {
                    paramArg.append(escapeSpecialShellCharacters(value)).append(",");
                }
            }
            if (paramArg.toString().endsWith(",")) {
                paramArg.deleteCharAt(paramArg.length() - 1);
            }
            arguments.add(paramArg.toString());
        }

        return arguments;
    }

    private Integer initiateWorkflowEntry() throws Exception {
        WrkWorkflowdata wrk = new WrkWorkflowdata((UserI)user);
        wrk.setDataType(this.getDataType());
        wrk.setId(this.getId());
        wrk.setExternalid(this.getExternalId());
        wrk.setCategory(EventUtils.CATEGORY.DATA);
        wrk.setType(EventUtils.TYPE.PROCESS);
        wrk.setPipelineName(this.getPipelineName());
        wrk.setLaunchTime(java.util.Calendar.getInstance().getTime());
        wrk.setStatus("Queued");
        WorkflowUtils.save(wrk, wrk.buildEvent());
        return wrk.getWorkflowId();
    }

    /**
     * @return Returns the startAt.
     */
    public String getStartAt() {
        return startAt;
    }

    /**
     * @return Returns the waitFor.
     */
    public boolean isWaitFor() {
        return waitFor;
    }

    /**
     * @param waitFor
     *            The waitFor to set.
     */
    public void setWaitFor(boolean waitFor) {
        this.waitFor = waitFor;
    }

    public static String getUserName(UserI user) {
        String rtn = "";
        try {
            if (user.getFirstname() != null && user.getLastname() != null) rtn = user.getFirstname().substring(0, 1) + "." + user.getLastname();
        } catch (Exception ignored) {
        }
        return rtn;
    }

    /**
     * @param startAt
     *            The startAt to set.
     */
    public void setStartAt(String startAt) {
        this.startAt = startAt;
    }

    /**
     * @return Returns the supressNotification.
     */
    public boolean isSupressNotification() {
        return supressNotification;
    }

    /**
     * @param supressNotification
     *            The supressNotification to set.
     */
    public void setSupressNotification(boolean supressNotification) {
        this.supressNotification = supressNotification;
    }

    public void setParameterFile(String pathToParameterFile) {
        parameterFile = pathToParameterFile;
    }

    public void notify(String emailId) {
        notificationEmailIds.add(emailId);
    }

    public void setParameter(String name, String value) {
        if (parameters.containsKey(name)) {
            parameters.get(name).add(value);
        } else {
            parameters.put(name, Arrays.asList(value));
        }

    }

    private void setBuildDir() {
        // TODO Set this to be the buildDir for the project
        String buildPath = ArcSpecManager.GetFreshInstance().getGlobalBuildPath() ;
        if (buildPath.endsWith(File.separator)) {
            buildPath = buildPath.substring(0, buildPath.length() - 1);
        }
        if (needsBuildDir) {
            parameters.put("builddir", Arrays.asList(buildPath + File.separator + "Pipeline"));
        }
    }

    /**
     * @return Returns the dataType.
     */
    public String getDataType() {
        return dataType;
    }

    /**
     * @param dataType
     *            The dataType to set.
     */
    public void setDataType(String dataType) {
        this.dataType = dataType;
    }

    /**
     * @return Returns the id.
     */
    public String getId() {
        return id;
    }

    /**
     * @param id
     *            The id to set.
     */
    public void setId(String id) {
        this.id = id;
    }

    /**
     * @return Returns the pipelineName.
     */
    public String getPipelineName() {
        return pipelineName;
    }

    /**
     * @param pipelineName
     *            The pipelineName to set.
     */
    public void setPipelineName(String pipelineName) {
        this.pipelineName = pipelineName;
    }

    private String escapeSpecialShellCharacters(String input) {
        String rtn = input;
        if (input == null) return rtn;
        if (!System.getProperty("os.name").toUpperCase().startsWith("WINDOWS")) {
            String[] pieces = input.split("'");
            rtn = "";
            for (final String piece : pieces) {
                rtn += "'" + piece + "'" + "\\'";
            }
            if (rtn.endsWith("\\'") && !input.endsWith("'")) {
                int indexOfLastQuote = rtn.lastIndexOf("\\'");
                if (indexOfLastQuote != -1) rtn = rtn.substring(0, indexOfLastQuote);
            }
        }
        return rtn;
    }

    /**
     * @return the externalId
     */
    public String getExternalId() {
        return externalId;
    }

    /**
     * @param externalId
     *            the externalId to set
     */
    public void setExternalId(String externalId) {
        this.externalId = externalId;
    }

    public void setParameter(String name, ArrayList<String> values) {
        if (values != null && values.size() > 0) {
            if (parameters.containsKey(name)) {
                parameters.get(name).addAll(values);
            } else {
                parameters.put(name, values);
            }
        }
    }

    public static XnatPipelineLauncher GetLauncherForExperiment(RunData data, Context context, XnatExperimentdata imageSession) throws Exception {
        XnatPipelineLauncher xnatPipelineLauncher = new XnatPipelineLauncher(data, context);
        xnatPipelineLauncher.setSupressNotification(true);
        UserI user = TurbineUtils.getUser(data);
        xnatPipelineLauncher.setParameter("useremail", user.getEmail());
        xnatPipelineLauncher.setParameter("userfullname", XnatPipelineLauncher.getUserName(user));
        xnatPipelineLauncher.setParameter("adminemail", AdminUtils.getAdminEmailId());
        xnatPipelineLauncher.setParameter("mailhost", AdminUtils.getMailServer());
        xnatPipelineLauncher.setParameter("xnatserver", TurbineUtils.GetSystemName());

        xnatPipelineLauncher.setId(imageSession.getId());
        xnatPipelineLauncher.setLabel(imageSession.getLabel());
        xnatPipelineLauncher.setDataType(imageSession.getXSIType());
        xnatPipelineLauncher.setExternalId(imageSession.getProject());
        xnatPipelineLauncher.setParameter("xnat_id", imageSession.getId());
        xnatPipelineLauncher.setParameter("project", imageSession.getProject());
        xnatPipelineLauncher.setParameter("cachepath", QCImageCreator.getQCCachePathForSession(imageSession.getProject()));

        List<String> emails = new ArrayList<String>();
        emails.add(TurbineUtils.getUser(data).getEmail());

        String extraEmails = (String) TurbineUtils.GetPassedParameter("emailField", data);
        if (!StringUtils.isBlank(extraEmails)) {
            for (String extraEmail : extraEmails.split("[\\s]*,[\\s]*")) {
                if (!StringUtils.isBlank(extraEmail)) {
                    emails.add(extraEmail);
                }
            }
        }
        for (final String email : emails) {
            if (!StringUtils.isBlank(email)) {
                xnatPipelineLauncher.notify(email);
            }
        }
        return xnatPipelineLauncher;
    }

    public static XnatPipelineLauncher GetBareLauncherForExperiment(RunData data, Context context, XnatExperimentdata imageSession) throws Exception {
        XnatPipelineLauncher xnatPipelineLauncher = new XnatPipelineLauncher(data, context);
        xnatPipelineLauncher.setSupressNotification(true);
        UserI user = TurbineUtils.getUser(data);
        xnatPipelineLauncher.setParameter("useremail", user.getEmail());
        xnatPipelineLauncher.setParameter("userfullname", XnatPipelineLauncher.getUserName(user));
        xnatPipelineLauncher.setParameter("adminemail", AdminUtils.getAdminEmailId());
        xnatPipelineLauncher.setParameter("mailhost", AdminUtils.getMailServer());
        xnatPipelineLauncher.setParameter("xnatserver", TurbineUtils.GetSystemName());

        xnatPipelineLauncher.setId(imageSession.getId());
        xnatPipelineLauncher.setLabel(imageSession.getLabel());
        xnatPipelineLauncher.setDataType(imageSession.getXSIType());
        xnatPipelineLauncher.setExternalId(imageSession.getProject());

        return xnatPipelineLauncher;
    }

    public static XnatPipelineLauncher GetLauncher(RunData data, Context context, XnatImagesessiondata imageSession) throws Exception {
        XnatPipelineLauncher xnatPipelineLauncher = GetLauncherForExperiment(data, context, imageSession);
        String path = imageSession.getArchivePath();
        if (path.endsWith(File.separator)) {
            path = path.substring(0, path.length() - 1);
        }
        xnatPipelineLauncher.setParameter("archivedir", path);
        return xnatPipelineLauncher;
    }

    /**
     * @return the label
     */
    public String getLabel() {
        return label;
    }

    /**
     * @param label the label to set
     */
    public void setLabel(String label) {
        this.label = label;
    }
}
