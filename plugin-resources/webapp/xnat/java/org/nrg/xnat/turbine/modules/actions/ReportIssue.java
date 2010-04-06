//Copyright 2007 Washington University School of Medicine All Rights Reserved
/*
 * Created on Aug 7, 2007
 *
 */
package org.nrg.xnat.turbine.modules.actions;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.Date;

import javax.mail.internet.InternetAddress;
import javax.servlet.ServletContext;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.apache.turbine.util.RunData;
import org.apache.turbine.util.parser.ParameterParser;
import org.apache.velocity.app.Velocity;
import org.apache.velocity.context.Context;
import org.nrg.xdat.security.XDATUser;
import org.nrg.xdat.turbine.modules.actions.SecureAction;
import org.nrg.xdat.turbine.utils.TurbineUtils;
import org.nrg.xft.XFT;
import org.nrg.xft.db.PoolDBUtils;
import org.nrg.xft.email.EmailUtils;
import org.nrg.xft.email.EmailerI;
import org.nrg.xnat.turbine.utils.ArcSpecManager;

public class ReportIssue extends SecureAction {
	private static final Logger logger = Logger.getLogger(ReportIssue.class);

	private static final String HTTP_USER_AGENT = "User-Agent";
	private static final String HTTP_HOST = "Host";
	private static final String JAVA_VENDOR = "java.vendor";
	private static final String JAVA_VERSION = "java.version";
	private static final String JAVA_OS_VERSION = "os.version";
	private static final String JAVA_OS_ARCH = "os.arch";
	private static final String JAVA_OS_NAME = "os.name";

	@Override
	public void doPerform(RunData data, Context context) throws Exception {

		final String adminEmail = XFT.GetAdminEmail();
		final XDATUser user = TurbineUtils.getUser(data);
		final ParameterParser parameters = data.getParameters();
		final String body = emailBody(user, parameters, data, context);

		try {
			final EmailerI mailer = EmailUtils.getEmailer();

			mailer.setFrom(adminEmail);
			mailer.setTo(Arrays.asList(new InternetAddress[] { new InternetAddress(adminEmail) }));
			mailer.setSubject(TurbineUtils.GetSystemName() + " Issue Report from " + user.getLogin());
			mailer.setMsg(body);

			attachment(data.getSession().getId(), parameters, mailer);

			mailer.send();
		} catch (Exception e) {
			logger.error("Unable to send mail", e);
		}
	}

	private void attachment(String sessionId, ParameterParser parameters, EmailerI mailer) {
		final FileItem fi = parameters.getFileItem("upload");
		if (fi != null) {
			final String cachePath = location(ArcSpecManager.GetInstance().getGlobalCachePath(), "issuereports", sessionId);
			checkFolder(cachePath);

			final File f = new File(location(cachePath, fi.getName()));
			final String path = f.getAbsolutePath();
			try {
				fi.write(f);
				mailer.addAttachment(path);
			} catch (Exception e) {
				logger.warn("Could not attach file, " + path, e);
			}
		}
	}

	private String emailBody(XDATUser user, ParameterParser parameters, RunData data, Context context) throws Exception {
		context.put("summary", parameters.get("summary"));
		context.put("description", parameters.get("description"));

		context.put("time", (new Date()).toString());

		context.put("user_agent", data.getRequest().getHeader(HTTP_USER_AGENT));
		context.put("xnat_host", data.getRequest().getHeader(HTTP_HOST));
		context.put("remote_addr", data.getRequest().getRemoteAddr());
		context.put("server_info", data.getServletContext().getServerInfo());

		context.put("os_name", System.getProperty(JAVA_OS_NAME));
		context.put("os_arch", System.getProperty(JAVA_OS_ARCH));
		context.put("os_version", System.getProperty(JAVA_OS_VERSION));
		context.put("java_version", System.getProperty(JAVA_VERSION));
		context.put("java_vendor", System.getProperty(JAVA_VENDOR));

		context.put("xnat_version", getXNATVersion(data.getServletContext()));

		context.put("user", user);
		context.put("postgres_version", (String) PoolDBUtils.ReturnStatisticQuery("SELECT version();", "version", user.getDBName(), user
				.getLogin()));

		final StringWriter sw = new StringWriter();
		Velocity.getTemplate("/screens/email/issue_report.vm").merge(context, sw);
		return sw.toString();
	}

	private String getXNATVersion(ServletContext servletContext) {
		final String path = servletContext.getRealPath(location("WEB-INF", "conf", "VERSION"));
		FileReader fr = null;
		try {
			fr = new FileReader(path);
			return (new BufferedReader(fr)).readLine();
		} catch (Exception e) {
			logger.warn("Issue reading VERSION file", e);
			return "could not retrieve";
		} finally {
			if (fr != null) {
				try {
					fr.close();
				} catch (Exception e) {
					// ignore it
				}
			}
		}
	}

	private void checkFolder(String path) {
		final File f = new File(path);
		if (!f.exists()) {
			f.mkdirs();
		}
	}

	private String location(String... pathParts) {
		return StringUtils.join(pathParts, File.separator);
	}

}
