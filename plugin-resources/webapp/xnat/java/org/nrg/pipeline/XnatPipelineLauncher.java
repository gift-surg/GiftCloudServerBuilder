/*
 *	Copyright Washington University in St Louis 2006
 *	All rights reserved
 *
 * 	@author Mohana Ramaratnam (Email: mramarat@wustl.edu)

*/

package org.nrg.pipeline;

import java.io.File;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Hashtable;

import org.apache.log4j.Logger;
import org.apache.turbine.util.RunData;
import org.apache.velocity.context.Context;
import org.nrg.viewer.QCImageCreator;
import org.nrg.xdat.om.WrkWorkflowdata;
import org.nrg.xdat.om.XnatExperimentdata;
import org.nrg.xdat.om.XnatImagesessiondata;
import org.nrg.xdat.security.XDATUser;
import org.nrg.xdat.turbine.utils.AdminUtils;
import org.nrg.xdat.turbine.utils.TurbineUtils;
import org.nrg.xft.XFT;
import org.nrg.xft.security.UserI;
import org.nrg.xnat.turbine.utils.ArcSpecManager;

public class XnatPipelineLauncher {
    static org.apache.log4j.Logger logger = Logger.getLogger(XnatPipelineLauncher.class);

    public static final String SCHEDULE = "schedule";

    String pipelineName;
    String id, label = null;
    String externalId; //Workflows External Id
    XDATUser user;
    String dataType;
    String builddir;
    String host;
    ArrayList notificationEmailIds;
    Hashtable parameters;
    String startAt;
    boolean waitFor;
    boolean isWindows;
    boolean needsBuildDir;
    boolean supressNotification;
    String parameterFile;
    String admin_email;
    boolean alwaysEmailAdmin=true;
	boolean useAlias = false;

    /**
     * @return the useAlias
     */
    public boolean useAlias() {
        return useAlias;
    }

    /**
     * @set useAlias
     */
    public void useAlias(boolean u) {
        useAlias = u;
    }

    /**
     * @return the admin_email
     */
    public String getAdmin_email() {
        return admin_email;
    }

    /**
     * @param admin_email the admin_email to set
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
     * @param alwaysEmailAdmin the alwaysEmailAdmin to set
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
     * @param needsBuildDir The needsBuildDir to set.
     */
    public void setNeedsBuildDir(boolean needsBuildDir) {
        this.needsBuildDir = needsBuildDir;
    }

    public void setBuildDir(String path) {
    	if (path==null) return;
    	if (path.endsWith(File.separator)) path = path.substring(0,path.length()-1);
        if (needsBuildDir) {
        	 ArrayList temp = new ArrayList();
        	 temp.add(path );

            parameters.put("builddir",temp);
        }
    	setNeedsBuildDir(false);

    }

    public XnatPipelineLauncher(RunData data, Context context) {
        notificationEmailIds = new ArrayList();
        parameters = new Hashtable();
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
        notificationEmailIds = new ArrayList();
        parameters = new Hashtable();
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



    public boolean launch() {
    	return launch(XFT.GetPipelinePath() + "bin" + File.separator + SCHEDULE);
    }

    public boolean launch (String cmdPrefix) {
        String command = " ";
        String pcommand = "";
        if (cmdPrefix != null) {
            command += cmdPrefix + " ";
        }
        command += XFT.GetPipelinePath() + "bin" + File.separator + "XnatPipelineLauncher";

        String osName = System.getProperty("os.name");
        if (osName.toUpperCase().startsWith("WINDOWS")) {
            command += ".bat";
        }
        command += " -pipeline " +  pipelineName;
        command += " -id " + id;
        if (label != null)
        	command += " -label " + label;
        command += " -host " + host;
   		if (useAlias())
            command += " -useAlias ";

        if (isSupressNotification())
            command += " -supressNotification ";
        command += " -u " + user.getUsername();

        //MODIFIED BY TO 09/22/09
        String pwd=user.getPrimaryPassword();
        if(pwd!=null){
          pcommand += " -pwd " + escapeSpecialShellCharacters(user.getPrimaryPassword());
		}else{
		  org.nrg.xnat.security.LDAPAuthenticator.AuthenticationAttempt attempt = org.nrg.xnat.security.LDAPAuthenticator.RetrieveCachedAttempt(new org.nrg.xdat.security.Authenticator.Credentials(user.getUsername(), ""));
		  if(attempt!=null){
          	pcommand += " -pwd " + escapeSpecialShellCharacters(attempt.cred.getPassword());
		  }
		}

        command += " -dataType " + dataType;
        if (externalId != null)
        	command += " -project \"" + externalId + "\"";


        if (startAt != null) {
            command += " -startAt " + startAt;
        }
        if (parameterFile != null) {
            command += " -parameterFile " + parameterFile;
        }

        //command += " -parameter host=" + host;
        //command += " -parameter u=" + user.getUsername();
        //pcommand += " -parameter pwd=" + escapeSpecialShellCharacters(user.getPrimaryPassword());
        for (int i = 0 ; i < notificationEmailIds.size(); i++)
            command += " -notify " + notificationEmailIds.get(i);
        setBuildDir();

        Enumeration enumeration = parameters.keys();
        while (enumeration.hasMoreElements()) {
            String paramName = (String)enumeration.nextElement();
            ArrayList values = (ArrayList)parameters.get(paramName);
            String paramArg = " -parameter " + paramName + "=";
            for (int i = 0; i < values.size(); i++) {
                paramArg += escapeSpecialShellCharacters((String)values.get(i)) + ",";
            }
            if (paramArg.endsWith(",")) paramArg = paramArg.substring(0,paramArg.length()-1);
            command += paramArg + " ";
        }
        boolean success = true;
        try {
            logger.debug("Launching command: " + command + " -pwd ****** -parameter pwd=******" );
            WrkWorkflowdata wrk = new WrkWorkflowdata();
            wrk.setDataType(this.getDataType());
            wrk.setId(this.getId());
            wrk.setExternalid(this.getExternalId());
            wrk.setPipelineName(this.getPipelineName());
            wrk.setLaunchTime(java.util.Calendar.getInstance().getTime());
            wrk.setStatus("Queued");
            wrk.save(user,false,true);
            ProcessLauncher processLauncher = new ProcessLauncher();
            processLauncher.setCommand(command + " " + pcommand);
            processLauncher.start();
            if (waitFor) {
              while (processLauncher.isAlive()) { } //wait for the thread to end
              success = processLauncher.getExitStatus();
            }
            if (!success) {
              logger.error("Couldnt launch " + command + " -pwd ******" );
            }
        }catch (Exception e) {
            logger.error(e.getMessage() + " for command " + command + " -pwd ******* " ,e);
            success = false;
        }
        return success;
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
     * @param waitFor The waitFor to set.
     */
    public void setWaitFor(boolean waitFor) {
        this.waitFor = waitFor;
    }

    public static String getUserName(UserI user) {
        String rtn = "";
        try {
            if (user.getFirstname() != null && user.getLastname() != null)
            rtn = user.getFirstname().substring(0,1) + "." + user.getLastname();
        }catch (Exception e) {}
        return rtn;
    }

    /**
     * @param startAt The startAt to set.
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
     * @param supressNotification The supressNotification to set.
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
           ((ArrayList)parameters.get(name)).add(value);
        }else {
           ArrayList values = new ArrayList();
           values.add(value);
           parameters.put(name,values);
        }

    }

    private void setBuildDir() {
        //TODO Set this to be the buildDir for the project
        String tdir = ArcSpecManager.GetInstance().getGlobalBuildPath() ;
        if (tdir.endsWith(File.separator))
            tdir = tdir.substring(0,tdir.length()-1);
        ArrayList temp = new ArrayList();
        temp.add(tdir + File.separator+  "Pipeline" );
        if (needsBuildDir)
            parameters.put("builddir",temp);
    }



    /**
     * @return Returns the dataType.
     */
    public String getDataType() {
        return dataType;
    }

    /**
     * @param dataType The dataType to set.
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
     * @param id The id to set.
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
     * @param pipelineName The pipelineName to set.
     */
    public void setPipelineName(String pipelineName) {
        this.pipelineName = pipelineName;
    }

    private String escapeSpecialShellCharacters(String input) {
         String rtn=input;
         if (input == null) return rtn;
         if (!System.getProperty("os.name").toUpperCase().startsWith("WINDOWS")) {
         	String[] pieces = input.split("'");
         	rtn = "";
            for (int i=0; i < pieces.length; i++) {
         	   rtn += "'" + pieces[i] + "'" + "\\'";
            }
            if (rtn.endsWith("\\'") && !input.endsWith("'") ) {
         	   int indexOfLastQuote = rtn.lastIndexOf("\\'");
         	   if (indexOfLastQuote != -1)
         	     rtn = rtn.substring(0,indexOfLastQuote);
            }
         }
         return rtn;
    }

    private String escapeComma(String input) {
        String rtn=input;
        if (input == null) return rtn;
        rtn = rtn.replace("\\", "\\\\");
        rtn = rtn.replace("\"", "\\\"");
        rtn = "\"" + rtn + "\"";
        return rtn;
   }


	/**
	 * @return the externalId
	 */
	public String getExternalId() {
		return externalId;
	}

	/**
	 * @param externalId the externalId to set
	 */
	public void setExternalId(String externalId) {
		this.externalId = externalId;
	}

    public void setParameter(String name, ArrayList<String> values) {
    	if (values != null && values.size() > 0) {
	        if (parameters.containsKey(name)) {
	           ((ArrayList)parameters.get(name)).addAll(values);
	        }else {
	           parameters.put(name,values);
	        }
    	}
    }

    public static  XnatPipelineLauncher GetLauncherForExperiment(RunData data, Context context, XnatExperimentdata imageSession) throws Exception  {
	       XnatPipelineLauncher xnatPipelineLauncher = new XnatPipelineLauncher(data,context);
	       xnatPipelineLauncher.setSupressNotification(true);
	       UserI user = TurbineUtils.getUser(data);
	       xnatPipelineLauncher.setParameter("useremail", user.getEmail());
	       xnatPipelineLauncher.setParameter("userfullname", XnatPipelineLauncher.getUserName(user));
	       xnatPipelineLauncher.setParameter("adminemail", AdminUtils.getAdminEmailId());
	       xnatPipelineLauncher.setParameter("mailhost", AdminUtils.getMailServer());
		    xnatPipelineLauncher.setParameter("xnatserver", TurbineUtils.GetSystemName());

	       //xnatPipelineLauncher.setParameter("xnatserver", TurbineUtils.GetSystemName());

			xnatPipelineLauncher.setId(imageSession.getId());
			xnatPipelineLauncher.setLabel(imageSession.getLabel());
			xnatPipelineLauncher.setDataType(imageSession.getXSIType());
			xnatPipelineLauncher.setExternalId(imageSession.getProject());
			xnatPipelineLauncher.setParameter("xnat_id", imageSession.getId());
			xnatPipelineLauncher.setParameter("project", imageSession.getProject());
	        xnatPipelineLauncher.setParameter("cachepath",QCImageCreator.getQCCachePathForSession(imageSession.getProject()));

	       String emailsStr = TurbineUtils.getUser(data).getEmail() + "," + data.getParameters().get("emailField");
	       String[] emails = emailsStr.trim().split(",");
	       for (int i = 0 ; i < emails.length; i++) {
	           xnatPipelineLauncher.notify(emails[i]);
	       }
	       return xnatPipelineLauncher;
	   }

    
    public static  XnatPipelineLauncher GetLauncher(RunData data, Context context, XnatImagesessiondata imageSession) throws Exception  {
	       XnatPipelineLauncher xnatPipelineLauncher = GetLauncherForExperiment(data,context, imageSession);
	       String path = imageSession.getArchivePath();
	       if (path.endsWith(File.separator)) path = path.substring(0, path.length()-1);
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
