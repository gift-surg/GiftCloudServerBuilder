/*
 * org.nrg.xnat.security.XnatExpiredPasswordFilter
 * XNAT http://www.xnat.org
 * Copyright (c) 2014, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 *
 * Last modified 12/11/13 3:33 PM
 */
package org.nrg.xnat.security;

import org.apache.commons.lang.StringUtils;
import org.nrg.config.exceptions.ConfigServiceException;
import org.nrg.xdat.XDAT;
import org.nrg.xdat.entities.AliasToken;
import org.nrg.xdat.entities.UserRole;
import org.nrg.xdat.entities.XDATUserDetails;
import org.nrg.xdat.om.ArcArchivespecification;
import org.nrg.xdat.security.XDATUser;
import org.nrg.xdat.services.AliasTokenService;
import org.nrg.xdat.services.XdatUserAuthService;
import org.nrg.xdat.turbine.utils.TurbineUtils;
import org.nrg.xnat.turbine.utils.ArcSpecManager;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.security.core.codec.Base64;
import org.springframework.web.filter.GenericFilterBean;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

public class XnatExpiredPasswordFilter extends GenericFilterBean {
	private String changePasswordPath = "";
	private String changePasswordDestination = "";
	private String logoutDestination = "";
	private String loginPath = "";
	private String loginDestination = "";
	private String inactiveAccountPath;
	private String inactiveAccountDestination;
	private String emailVerificationDestination;
	private String emailVerificationPath;
	private boolean passwordExpirationDirtied = true;
	private boolean passwordExpirationDisabled;
	private boolean passwordExpirationInterval;
	private String passwordExpirationSetting;

	@Override
	public void doFilter(ServletRequest req, ServletResponse res,
			FilterChain chain) throws IOException, ServletException {
		final HttpServletRequest request = (HttpServletRequest) req;
		final HttpServletResponse response = (HttpServletResponse) res;
		XDATUser user = (XDATUser) request.getSession().getAttribute("user");
		Object passwordExpired = request.getSession().getAttribute("expired");
		ArcArchivespecification _arcSpec = ArcSpecManager.GetInstance();
		final String referer = request.getHeader("Referer");
		if (request.getSession() != null
				&& request.getSession().getAttribute("forcePasswordChange") != null
				&& (Boolean) request.getSession().getAttribute(
						"forcePasswordChange")) {
			try {
				String refererPath = null;
				String uri = new URI(request.getRequestURI()).getPath();
				if (!StringUtils.isBlank(referer)) {
					refererPath = new URI(referer).getPath();
				}
				if (uri.endsWith(changePasswordPath)
						|| uri.endsWith(changePasswordDestination)
						|| uri.endsWith(logoutDestination)
						|| uri.endsWith(loginPath)
						|| uri.endsWith(loginDestination)) {
					// If you're already on the change password page, continue
					// on without redirect.
					chain.doFilter(req, res);
				} else if (!StringUtils.isBlank(refererPath)
						&& (changePasswordPath.equals(refererPath)
								|| changePasswordDestination
										.equals(refererPath) || logoutDestination
									.equals(refererPath))) {
					// If you're on a request within the change password page,
					// continue on without redirect.
					chain.doFilter(req, res);
				} else {
					response.sendRedirect(TurbineUtils.GetFullServerPath()
							+ changePasswordPath);
				}
			} catch (URISyntaxException ignored) {
				//
			}
		} else if (passwordExpired != null && !(Boolean) passwordExpired) {
			// If the date of password change was checked earlier in the session
			// and found to be not expired, do not send them to the expired
			// password page.
			chain.doFilter(request, response);
		} else if (_arcSpec == null || !_arcSpec.isComplete()) {
			// If the arc spec has not yet been set, have the user configure the
			// arc spec before changing their password. This prevents a negative
			// interaction with the arc spec filter.
			chain.doFilter(request, response);
		} else if (user == null) {
			// If the user is not logged in, do not send them to the expired
			// password page.

			String header = request.getHeader("Authorization");
			if (header != null && header.startsWith("Basic ")) {
				// For users that authenticated using basic authentication,
				// check whether their password is expired, and if so give them
				// a 403 and a message that they need to change their password.

				String token = new String(Base64.decode(header.substring(6)
						.getBytes("UTF-8")), "UTF-8");
				String username = "";
				int delim = token.indexOf(":");
				if (delim != -1) {
					username = token.substring(0, delim);
				}
				if (AliasToken.isAliasFormat(username)) {
					AliasTokenService service = XDAT.getContextService()
							.getBean(AliasTokenService.class);
					AliasToken alias = service.locateToken(username);
					if (alias == null) {
						response.sendError(
								HttpServletResponse.SC_FORBIDDEN,
								"Your security token has expired. Please try again after updating your session.");
						return;
					}
					username = alias.getXdatUserId();
				}
				// Check whether the user is connected to an active role for
				// non_expiring.
				try {
					List<Integer> roles = (new JdbcTemplate(
							XDAT.getDataSource()))
							.query("SELECT COUNT(*) FROM xhbm_user_role where username = ? and role = ? and enabled = 't'",
									new String[] { username,
											UserRole.ROLE_NON_EXPIRING },
									new RowMapper<Integer>() {
										public Integer mapRow(ResultSet rs,
												int rowNum) throws SQLException {
											return rs.getInt(1);
										}
									});
					if (roles.get(0) > 0) {
						chain.doFilter(request, response);
					}
				} catch (Exception e) {
					logger.error(e);
				}

				if (isPasswordExpirationDisabled()) {
					chain.doFilter(request, response);
				} else {
					final boolean isExpired = checkForExpiredPassword(username);
					request.getSession().setAttribute("expired", isExpired);
					if (username != null && isExpired
							&& !username.equals("guest")) {
						response.sendError(HttpServletResponse.SC_FORBIDDEN,
								"Your password has expired. Please try again after changing your password.");
					} else {
						chain.doFilter(request, response);
					}
				}
			} else {
				checkUserChangePassword(request, response);
				// User is not authenticated through basic authentication
				// either.
				chain.doFilter(req, res);
			}
		} else {
			String uri = request.getRequestURI();

			if (user.getUsername().equals("guest")) {
				// If you're a guest and you try to access the change password
				// page, you get sent to the login page since there's no
				// password on the guest account to change.
				checkUserChangePassword(request, response);
			}
			if (user.getUsername().equals("guest") ||
			// If you're logging in or out, or going to the login page
			// itself
					(uri.endsWith(logoutDestination) || uri.endsWith(loginPath) || uri
							.endsWith(loginDestination)) ||
					// If you're already on the change password page, continue
					// on without redirect.
					(user.isEnabled() && (uri.endsWith(changePasswordPath) || uri
							.endsWith(changePasswordDestination))) ||
					// If you're already on the inactive account page or
					// reactivating an account, continue on without redirect.
					(!user.isEnabled() && (uri.endsWith(inactiveAccountPath)
							|| uri.endsWith(inactiveAccountDestination)
							|| uri.endsWith(emailVerificationPath)
							|| uri.endsWith(emailVerificationDestination) || (referer != null && (referer
							.endsWith(inactiveAccountPath) || referer
							.endsWith(inactiveAccountDestination))))) ||
					// If you're on a request within the change password page,
					// continue on without redirect.
					(referer != null && (referer.endsWith(changePasswordPath)
							|| referer.endsWith(changePasswordDestination) || referer
								.endsWith(logoutDestination)))) {
				chain.doFilter(req, res);
			} else if (user instanceof XDATUserDetails
					&& ((XDATUserDetails) user).getAuthorization() != null
					&& ((XDATUserDetails) user).getAuthorization()
							.getAuthMethod().equals(XdatUserAuthService.LDAP)) {
				// Shouldn't check for a localdb expired password if user is
				// coming in through LDAP
				chain.doFilter(req, res);
			} else if (user.isEnabled()) {
				boolean isExpired = checkForExpiredPassword(user);

				if ((!isUserNonExpiring(user) && isExpired)
						|| (XDAT.getBoolSiteConfigurationProperty(
								"requireSaltedPasswords", true) && user
								.getSalt() == null)) {
					request.getSession().setAttribute("expired", isExpired);
					response.sendRedirect(TurbineUtils.GetFullServerPath()
							+ changePasswordPath);
				} else {
					chain.doFilter(request, response);
				}
			} else {
				response.sendRedirect(TurbineUtils.GetFullServerPath()
						+ inactiveAccountPath);
			}
		}
	}

	public void setChangePasswordPath(String path) {
		this.changePasswordPath = path;
	}

	public void setChangePasswordDestination(String path) {
		this.changePasswordDestination = path;
	}

	public void setLogoutDestination(String path) {
		this.logoutDestination = path;
	}

	public void setLoginPath(String path) {
		this.loginPath = path;
	}

	public void setLoginDestination(String loginDestination) {
		this.loginDestination = loginDestination;
	}

	public void setInactiveAccountPath(String inactiveAccountPath) {
		this.inactiveAccountPath = inactiveAccountPath;
	}

	public String getInactiveAccountPath() {
		return inactiveAccountPath;
	}

	public void setInactiveAccountDestination(String inactiveAccountDestination) {
		this.inactiveAccountDestination = inactiveAccountDestination;
	}

	public String getInactiveAccountDestination() {
		return inactiveAccountDestination;
	}

	public void setEmailVerificationDestination(
			String emailVerificationDestination) {
		this.emailVerificationDestination = emailVerificationDestination;
	}

	public String getEmailVerificationDestination() {
		return emailVerificationDestination;
	}

	public void setEmailVerificationPath(String emailVerificationPath) {
		this.emailVerificationPath = emailVerificationPath;
	}

	public String getEmailVerificationPath() {
		return emailVerificationPath;
	}

	public void setPasswordExpirationDirtied(
			final boolean passwordExpirationDirtied) {
		this.passwordExpirationDirtied = passwordExpirationDirtied;
	}

	private boolean checkForExpiredPassword(final XDATUser user) {
		return checkForExpiredPassword(user.getUsername());
	}

	private boolean checkForExpiredPassword(final String username) {
		try {
			if (isPasswordExpirationDisabled()) {
				return false;
			}
			if (isPasswordExpirationInterval()) {
				List<Boolean> expired = (new JdbcTemplate(XDAT.getDataSource()))
						.query("SELECT ((now()-password_updated)> (Interval '"
								+ getPasswordExpirationSetting()
								+ " days')) AS expired FROM xhbm_xdat_user_auth WHERE auth_user = ? AND auth_method = 'localdb'",
								new String[] { username },
								new RowMapper<Boolean>() {
									public Boolean mapRow(ResultSet rs,
											int rowNum) throws SQLException {
										return rs.getBoolean(1);
									}
								});
				return expired.get(0);
			} else {
				List<Boolean> expired = (new JdbcTemplate(XDAT.getDataSource()))
						.query("SELECT (to_date('"
								+ getPasswordExpirationSetting()
								+ "', 'MM/DD/YYYY') BETWEEN password_updated AND now()) AS expired FROM xhbm_xdat_user_auth WHERE auth_user = ? AND auth_method = 'localdb'",
								new String[] { username },
								new RowMapper<Boolean>() {
									public Boolean mapRow(ResultSet rs,
											int rowNum) throws SQLException {
										return rs.getBoolean(1);
									}
								});
				return expired.get(0);
			}
		} catch (Throwable e) { // ldap authentication can throw an exception
								// during these queries
			logger.error(e.getMessage(), e);
		}
		return false;
	}

	private boolean isPasswordExpirationDisabled() {
		if (!passwordExpirationDirtied) {
			return passwordExpirationDisabled;
		}
		try {
			final String type = XDAT
					.getSiteConfigurationProperty("passwordExpirationType");
			if (StringUtils.isBlank(type)) {
				passwordExpirationDisabled = true;
			} else if (type.equals("Interval")) {
				passwordExpirationInterval = true;
				passwordExpirationSetting = validatePasswordExpirationInterval(XDAT
						.getSiteConfigurationProperty("passwordExpirationInterval"));
				passwordExpirationDisabled = passwordExpirationSetting
						.equals("0");
			} else if (type.equals("Date")) {
				passwordExpirationInterval = false;
				passwordExpirationSetting = validatePasswordExpirationDate(XDAT
						.getSiteConfigurationProperty("passwordExpirationDate"));
				passwordExpirationDisabled = passwordExpirationSetting
						.equals("0");
			} else {
				passwordExpirationDisabled = true;
			}
			passwordExpirationDirtied = false;
			return passwordExpirationDisabled;
		} catch (ConfigServiceException e) {
			logger.error("Error accessing the configuration service", e);
			return true;
		}
	}

	private String validatePasswordExpirationInterval(
			final String passwordExpirationInterval) {
		// overly long intervals break the query; this limit allows intervals up
		// to approximately 2700 years, which should be sufficient for most
		// purposes
		return StringUtils.isNotBlank(passwordExpirationInterval)
				&& !passwordExpirationInterval.equals("0")
				&& passwordExpirationInterval.length() <= 6
				&& passwordExpirationInterval.matches("\\d+") ? passwordExpirationInterval
				: "0";
	}

	private String validatePasswordExpirationDate(
			final String passwordExpirationDate) {
		return StringUtils.isNotBlank(passwordExpirationDate)
				&& passwordExpirationDate.matches("\\d\\d/\\d\\d/\\d\\d\\d\\d") ? passwordExpirationDate
				: "0";
	}

	private boolean isPasswordExpirationInterval() {
		return passwordExpirationInterval;
	}

	private String getPasswordExpirationSetting() {
		return passwordExpirationSetting;
	}

	private boolean isUserNonExpiring(XDATUser user) {
		try {
			return user.checkRole(UserRole.ROLE_NON_EXPIRING);
		} catch (Exception e) {
			return false;
		}
	}

	private void checkUserChangePassword(HttpServletRequest request,
			HttpServletResponse response) throws IOException {
		try {
			String uri = new URI(request.getRequestURI()).getPath();
			if (uri.endsWith("ChangePassword.vm")
					&& request.getParameterMap().isEmpty()) {
				response.sendRedirect(TurbineUtils.GetFullServerPath()
						+ "/app/template/Login.vm");
			}
		} catch (URISyntaxException ignored) {
		}
	}

}
