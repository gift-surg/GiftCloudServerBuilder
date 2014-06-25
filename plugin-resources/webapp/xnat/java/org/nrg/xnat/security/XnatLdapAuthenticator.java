/*
 * org.nrg.xnat.security.XnatLdapAuthenticator
 * XNAT http://www.xnat.org
 * Copyright (c) 2014, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 *
 * Last modified 7/10/13 9:04 PM
 */
package org.nrg.xnat.security;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.nrg.xdat.security.Authenticator;
import org.nrg.xdat.security.XDATUser;
import org.nrg.xdat.security.XDATUser.ActivationException;
import org.nrg.xdat.security.XDATUser.EnabledException;
import org.nrg.xdat.security.XDATUser.PasswordAuthenticationException;
import org.nrg.xdat.security.XDATUser.UserNotFoundException;
import org.nrg.xft.XFT;
import org.nrg.xft.XFTItem;
import org.nrg.xft.event.EventUtils;
import org.nrg.xft.exception.ElementNotFoundException;
import org.nrg.xft.exception.FieldNotFoundException;
import org.nrg.xft.exception.XFTInitException;
import org.nrg.xft.utils.SaveItemHelper;

import javax.naming.Context;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;
import javax.naming.ldap.InitialLdapContext;
import javax.naming.ldap.LdapContext;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.*;

public class XnatLdapAuthenticator extends Authenticator {

	static org.apache.log4j.Logger logger = Logger
			.getLogger(XnatLdapAuthenticator.class);

	//LDAP server to connect to
	private static String LDAP_HOST = "ldap://localhost";
	
	//account to use when connecting to LDAP server to verify account details
	private  static String LDAP_USER = "CN=admin,OU=Users,DC=xnat,DC=com";
	private  static String SEARCHBASE = "dc=xnat,dc=com";
	private  static String LDAP_PASS = "admin";
	
	private  static String INITIAL_CONTEXT_FACTORY = "com.sun.jndi.ldap.LdapCtxFactory";
	private  static String SECURITY_AUTHENTICATION = "simple";
	private  static String REFERRAL = "follow";
	
	//search used to query for the user's distinguishedName
	private  static String SEARCH_TEMPLATE = "(&(objectClass=user)(CN=%USER%))";
	private  static String LDAP_USER_PK = "distinguishedName";
	
	public static String LDAP_TO_XNAT_PK_CACHE = "xdat:user/quarantine_path";
	
	public static int AUTHENTICATION_EXPIRATION = 3600; //seconds

	public static boolean IGNORE_MULTIPLE_MATCHES = false;
	public static boolean CREATE_MISSING_ACCOUNTS = true;

	public final static Map<String, String> params = new HashMap<String, String>();


	private LdapContext ctx;
	private Hashtable env;

	public static boolean ENABLED=false;

	static File AUTH_PROPS=null;

	public XnatLdapAuthenticator() {
		try {
			if(AUTH_PROPS==null){
				AUTH_PROPS=new File(XFT.GetConfDir(),"authentication.properties");
				if(AUTH_PROPS.exists()){
					ENABLED=true;
				}
				else{
					logger.info("No authentication.properties file found in conf directory. Skipping enhanced authentication method.");
					ENABLED=false;
					return;
				}
				InputStream inputs = new FileInputStream(AUTH_PROPS);
				 Properties properties = new Properties();
				 properties.load(inputs);

				if(properties.containsKey("LDAP_HOST"))
				 LDAP_HOST = properties.getProperty("LDAP_HOST");
				else
					throw new Exception("Missing LDAP_HOST");

				if(properties.containsKey("LDAP_USER"))
					LDAP_USER = properties.getProperty("LDAP_USER");
				else
					throw new Exception("Missing LDAP_USER");

				if(properties.containsKey("SEARCHBASE"))
					SEARCHBASE = properties.getProperty("SEARCHBASE");
				else
					throw new Exception("Missing SEARCHBASE");

				if(properties.containsKey("LDAP_PASS"))
					LDAP_PASS = properties.getProperty("LDAP_PASS");
				else
					throw new Exception("Missing LDAP_PASS");

				if(properties.containsKey("INITIAL_CONTEXT_FACTORY"))
					INITIAL_CONTEXT_FACTORY = properties.getProperty("INITIAL_CONTEXT_FACTORY");
				else
					throw new Exception("Missing INITIAL_CONTEXT_FACTORY");

				if(properties.containsKey("SECURITY_AUTHENTICATION"))
					SECURITY_AUTHENTICATION = properties.getProperty("SECURITY_AUTHENTICATION");
				else
					throw new Exception("Missing SECURITY_AUTHENTICATION");

				if(properties.containsKey("REFERRAL"))
					REFERRAL = properties.getProperty("REFERRAL");
				else
					throw new Exception("Missing REFERRAL");

				if(properties.containsKey("SEARCH_TEMPLATE"))
					SEARCH_TEMPLATE = properties.getProperty("SEARCH_TEMPLATE");
				else
					throw new Exception("Missing SEARCH_TEMPLATE");

				if(properties.containsKey("LDAP_USER_PK"))
					LDAP_USER_PK = properties.getProperty("LDAP_USER_PK");
				else
					throw new Exception("Missing LDAP_USER_PK");

				if(properties.containsKey("IGNORE_MULTIPLE_MATCHES"))
					IGNORE_MULTIPLE_MATCHES = Boolean.parseBoolean(properties.getProperty("IGNORE_MULTIPLE_MATCHES"));

				if(properties.containsKey("CREATE_MISSING_ACCOUNTS"))
					CREATE_MISSING_ACCOUNTS = Boolean.parseBoolean(properties.getProperty("CREATE_MISSING_ACCOUNTS"));

				if(properties.containsKey("AUTHENTICATION_EXPIRATION"))
					AUTHENTICATION_EXPIRATION = Integer.parseInt(properties.getProperty("AUTHENTICATION_EXPIRATION"));
				else
					throw new Exception("Missing AUTHENTICATION_EXPIRATION");


				params.put("CN", "xdat:user/login");
				params.put("givenName", "xdat:user/firstname");
				params.put("sn", "xdat:user/lastname");
				params.put("mail", "xdat:user/email");
				params.put(LDAP_USER_PK, LDAP_TO_XNAT_PK_CACHE);
			}

		} catch (Exception e) {
			logger.info(e.getMessage());
			ENABLED=false;
			logger.error("Problem during init", e);
		}
	}

	public void closeContext() {
		try {
			if(ctx!=null)ctx.close();
			ctx=null;
		} catch (NamingException e) {
			logger.error("Problem during close", e);

		}
	}

	public void openContext(String user, String pass) throws NamingException{
		try {
			env = new Hashtable();
			env.put(Context.SECURITY_PRINCIPAL, user);
			env.put(Context.SECURITY_CREDENTIALS, pass);
			env.put(Context.INITIAL_CONTEXT_FACTORY, INITIAL_CONTEXT_FACTORY);
			env.put(Context.SECURITY_AUTHENTICATION, SECURITY_AUTHENTICATION);
			env.put(Context.PROVIDER_URL, LDAP_HOST);
			env.put(Context.REFERRAL, REFERRAL);

			// Create the initial directory context
			ctx = new InitialLdapContext(env, null);
		} catch (NamingException e) {
			throw e;
		}
	}

	/**
	 * Gets LDAP information for users based on search filter
	 *
	 * Step 1: query the server for a list of matching users (based on search filter and submitted cred)
	 * Step 2: use the results to populate XDATUser objects
	 * 
	 * @param searchFilter
	 *            The JNDI search filter you want to use
	 * @return Array of LoginBean objects for users found.
	 */
	public XDATUser getUsers(Credentials cred) {
		logger.debug("\n\ngetUsers:" + cred.getUsername());
		String searchFilter = buildSearchFilter(cred);
		XDATUser loginBean = null;
		ArrayList<XDATUser> users = new ArrayList<XDATUser>();
		// Create the search controls
		SearchControls searchCtls = new SearchControls();
		searchCtls.setSearchScope(SearchControls.SUBTREE_SCOPE);

		logger.debug(params.toString());
		String[] returnedAtts = new String[params.size()];
		int c=0;
		for(Map.Entry<String,String> e:params.entrySet()){
			returnedAtts[c++]=e.getKey();
		}

		// String returnedAtts[] = {"displayName", "sn", "givenName",
		// "sAMAccountName", "mail", "department", "telephoneNumber", "company",
		// "operations", "memberOf"};
		searchCtls.setReturningAttributes(returnedAtts);
		try {
			this.openContext(LDAP_USER, LDAP_PASS);
			logger.debug(ctx.getEnvironment().toString());
			logger.debug("SEARCHBASE:" + SEARCHBASE);
			logger.debug("FILTER:" + searchFilter);
			logger.debug("searchCtls:" + searchCtls.toString());
			for(String s:returnedAtts){
				logger.debug("returnedAtts:" + s);
			}
			// Search for objects using the filter
			NamingEnumeration answer = ctx.search(SEARCHBASE, searchFilter,
					searchCtls);
			int match=0;
			// Loop through the search results
			while (answer.hasMoreElements()) {
				SearchResult sr = (SearchResult) answer.next();
				Attributes attrs = sr.getAttributes();
				String cn = sr.getClassName();
				logger.debug("getUsers:answer[" + match++ +"]=" + sr.getName() + " "+sr.getClassName() + " " +attrs.size());

				try {
					//Create new user account for missing user
					XFTItem i = XFTItem.NewItem("xdat:user", null);

					if (attrs != null) {
						try {
							// multiple attributes returned from server need to be mapped to a single xdat:user
							// this is the point when the XNAT-user's properties are set, it is generic to allow for different parameter mapping logic.
							for (NamingEnumeration ae = attrs.getAll(); ae
									.hasMore();) {
								Attribute attr = (Attribute) ae.next();

								logger.debug("getUsers:answer:row:attr:" + attr.getID() + "="+ attr.get());
								// System.out.println("Attribute: " +
								// attr.getID());
								for(Map.Entry<String,String> e: params.entrySet()){
									if(e.getKey().equalsIgnoreCase(attr.getID())){
										i.setProperty(e.getValue(),
												attr.get());
										break;
									}

								}
//								for (NamingEnumeration e = attr.getAll(); e
//										.hasMore();) {
//									if (params.containsKey(attr.getID())) {
//										i.setProperty(params.get(attr.getID()),
//												e.next());
//									}
//								}
							}
						} catch (NamingException e) {
							logger.error(cred.getUsername()
									+ ":getUser:Error retrieving data for "
									+ " from results", e);
						}
					}
					loginBean=new XDATUser(i);
					if(loginBean.getUsername()==null){
						logger.error(cred.getUsername()
								+ ":getUser:Missing login");
					}else{
						users.add(loginBean);
					}
				} catch (XFTInitException e) {
					logger.error("Problem during getUsers", e);
				} catch (ElementNotFoundException e) {
					logger.error("Problem during getUsers", e);
				} catch (FieldNotFoundException e) {
					logger.error("Problem during getUsers", e);
				} catch (Exception e) {
					logger.error("Problem during getUsers", e);
				}
			}

		} catch (NamingException e) {
			logger.error("Problem during getUsers", e);
		}finally{
			this.closeContext();
		}

		if (users.size() == 1) {
			logger.info(cred.getUsername() + ":getUser:Matched user account:"+users.get(0));
			return users.get(0);
		} else if (users.size() == 0) {
			logger.error(cred.getUsername() + ":getUser:No Results Found");
			return null;
		} else {
			if(IGNORE_MULTIPLE_MATCHES){
				logger.error(cred.getUsername() + ":getUser:Multiple matches ignored- returning first:"+users.get(0));
				return users.get(0);
			}else{
				logger.error(cred.getUsername()
						+ ":getUser:Multiple Records Found for "
						+ cred.getUsername() + " Account");
				return null;
			}
		}
	}

	public String buildSearchFilter(Credentials cred) {
		return StringUtils.replace(SEARCH_TEMPLATE, "%USER%", cred
				.getUsername());
	}

	
	/**
	 * Query server for the DN for this cred
	 * 
	 * @param cred
	 * @return String DN
	 */
	public String getDN(Credentials cred) {
		logger.debug("\n\ngetDN:" + cred.getUsername());
		String searchFilter = buildSearchFilter(cred);
		ArrayList<String> dns = new ArrayList<String>();
		// Create the search controls
		SearchControls searchCtls = new SearchControls();
		searchCtls.setSearchScope(SearchControls.SUBTREE_SCOPE);
		String returnedAtts[] = { LDAP_USER_PK };
		searchCtls.setReturningAttributes(returnedAtts);
		try {
			this.openContext(LDAP_USER, LDAP_PASS);
			logger.debug(ctx.getEnvironment().toString());
			logger.debug("SEARCHBASE:" + SEARCHBASE);
			logger.debug("FILTER:" + searchFilter);
			logger.debug("searchCtls:" + searchCtls.toString());
			// Search for objects using the filter
			NamingEnumeration answer = ctx.search(SEARCHBASE, searchFilter,
					searchCtls);

			// Loop through the search results
			while (answer.hasMoreElements()) {
				SearchResult sr = (SearchResult) answer.next();
				String cn = sr.getClassName();

				Attributes attrs = sr.getAttributes();
				if (attrs != null) {
					try {
						for (NamingEnumeration ae = attrs.getAll(); ae
								.hasMore();) {
							Attribute attr = (Attribute) ae.next();
							// System.out.println("Attribute: " + attr.getID());
							for (NamingEnumeration e = attr.getAll(); e
									.hasMore();) {
								if (attr.getID().equalsIgnoreCase(LDAP_USER_PK)) {
									dns.add((String) e.next());
								}
							}
						}
					} catch (NamingException e) {
						logger.error(
								cred.getUsername()
										+ ":Error retrieving DN for "
										+ " from results", e);
					}
				}
			}

		} catch (NamingException e) {
			logger.error(cred.getUsername() + ":Error retrieving DN for "
					+ cred.getUsername() + " from server", e);
		}finally{
			this.closeContext();
		}

		if (dns.size() == 1) {
			return dns.get(0);
		} else if (dns.size() == 0) {
			logger.info(cred.getUsername() + ":No DN Found for "
					+ cred.getUsername() + " Account");
			return null;
		} else {
			logger.info(cred.getUsername() + ":Multiple DNs Found for "
					+ cred.getUsername() + " Account");
			return null;
		}
	}

	/**
	 * Attempt to login using the specified DN and password in cred.
	 * @param dn
	 * @param cred
	 * @return
	 * @throws NamingException (means authentication failure)
	 */
	public boolean attemptLogin(final String dn, final Credentials cred){
		final String searchFilter = buildSearchFilter(cred);
		
		// Create the search controls
		final SearchControls searchCtls = new SearchControls();
		searchCtls.setSearchScope(SearchControls.SUBTREE_SCOPE);
		final String returnedAtts[] = { LDAP_USER_PK };
		searchCtls.setReturningAttributes(returnedAtts);
		int count = 0;

		try{
			this.openContext(dn, cred
					.getPassword());
			logger.debug(ctx.getEnvironment().toString());
			logger.debug("SEARCHBASE:" + SEARCHBASE);
			logger.debug("FILTER:" + searchFilter);
			logger.debug("searchCtls:" + searchCtls.toString());
			// Search for objects using the filter
			NamingEnumeration answer = ctx.search(SEARCHBASE, searchFilter,
					searchCtls);
	
			// Loop through the search results
			while (answer.hasMoreElements()) {
				SearchResult sr = (SearchResult) answer.next();
				String cn = sr.getClassName();
	
				Attributes attrs = sr.getAttributes();
				if (attrs != null) {
					try {
						for (NamingEnumeration ae = attrs.getAll(); ae
								.hasMore();) {
							Attribute attr = (Attribute) ae.next();
							// System.out.println("Attribute: " + attr.getID());
							for (NamingEnumeration e = attr.getAll(); e
									.hasMore();) {
								if (attr.getID().equalsIgnoreCase(LDAP_USER_PK)) {
									count++;
									e.next();
								}
							}
						}
					} catch (NamingException e) {
						logger.error(cred.getUsername()
								+ ":Error retrieving DN for "
								+ cred.getUsername() + " from results", e);
					}
				}
			}
		}catch (NamingException e) {
			logger.error(cred.getUsername()
					+ ":Unable to authenticate "
					+ cred.getUsername() + " with given password using DN: " + dn, e);
		}finally{
			this.closeContext();
		}
		
		if (count > 0) {
			return true;
		} else {
			return false;
		}
	}
	
	/**
	 * Step 1: Query server as user
	 * Step 2: Confirm results returned
	 * 
	 * @param cred
	 * @return
	 */
	public boolean verifyLogin(XDATUser u,Credentials cred) {
		logger.debug("\n\nverifyLogin:" + cred.getUsername());
		String dn = (String) cred.OTHER.get(LDAP_USER_PK);
		if (dn == null) {
			logger.info(cred.getUsername() + ":failed to populate DN for "
					+ cred.getUsername() + " Account");
			return false;
		}

		if(this.attemptLogin(dn, cred)){
			return true;
		}else{
			//check for updated DN
			final String newDN = this.getDN(cred);
			if(!dn.equals(newDN)){
				logger.info(cred.getUsername() + ":LDAP Server has a new DN for this user.  Attempting authentication with new DN " + newDN);
				cred.OTHER.put(LDAP_USER_PK, dn);
				if(this.attemptLogin(newDN, cred)){
					logger.info(cred.getUsername() + ":LDAP authentication succeeded with updated DN " + newDN + ". Updating stored DN.");
					updateStoredDN(newDN,u);
					return true;
				}else{
					return false;
				}
			}else{
				//don't update stored DN when authentication fails.
				return false;
			}
		}
	}

	
	public String retrieveStoredDN(XDATUser u){
		//this version stores the DN in a dead field (quarantine_path).  This prevented having to modify the user schema, but really seems in-appropriate.
		return u.getQuarantinePath();
	}
	
	public void updateStoredDN(final String newDN,XDATUser u){
		try {
			u.setQuarantinePath(newDN);
			SaveItemHelper.authorizedSave(u,null, true, false, true, false,EventUtils.newEventInstance(EventUtils.CATEGORY.SIDE_ADMIN, EventUtils.TYPE.PROCESS, "Created user from LDAP"));
		} catch (Exception e) {
			logger.error(u.getUsername() + ":Failed to update stored DN for user. Proceeding...",e);
		}
	}

	/* (non-Javadoc)
	 * @see org.nrg.xdat.security.Authenticator#authenticate(org.nrg.xdat.security.Authenticator.Credentials)
	 * 
	 * This is one of two possible starting points in this class.  It tries to authenticate a user, without knowing in advance the corresponding xnat-user object.
	 */
	public XDATUser authenticate(Credentials cred)
			throws PasswordAuthenticationException, EnabledException,
			ActivationException, Exception {
		if(!ENABLED)return super.authenticate(cred);
		try {
			XDATUser u = null;
			try {
				u = new XDATUser(cred.getUsername());
			} catch (UserNotFoundException e) {
				// ignore missing account for now. Need to check against LDAP
			}

			if (u == null && CREATE_MISSING_ACCOUNTS) {
				logger.info(cred.getUsername() + ": unknown user account");
				// SEE IF USER ACCOUNT exists in LDAP, and create
				u = getUsers(cred);

				if(u==null){
					throw new XDATUser.FailedLoginException("Unknown user account.", cred.getUsername());
				}

				u.setProperty("enabled", "true");
				//we have everything we need to create our user account here, but we haven't actually checked the password against LDAP yet.  The user will be saved, if that succeeds.
			}

			if (u != null) {
				if (authenticate(u, cred)){
					if(u.getXdatUserId()==null){

						SaveItemHelper.authorizedSave(u,null, true, false, true, false,EventUtils.newEventInstance(EventUtils.CATEGORY.SIDE_ADMIN, EventUtils.TYPE.PROCESS, "Created user from LDAP"));

						u = new XDATUser(cred.getUsername());
						u.setLoggedIn(true);
						if (!u.isExtended()) {
							u.init();
							if (!u.getItem().isPreLoaded())
								u.extend(true);
							u.setExtended(true);
						}
					}
					return u;
				}
				else
					throw new PasswordAuthenticationException(u.getLogin());
			}else{
				throw new XDATUser.FailedLoginException("Unknown Account", cred.getUsername());
			}
		} catch (Exception t) {
			throw t;
		} finally {
			this.closeContext();
		}
	}

	/* (non-Javadoc)
	 * @see org.nrg.xdat.security.Authenticator#authenticate(org.nrg.xdat.security.XDATUser, org.nrg.xdat.security.Authenticator.Credentials)
	 * 
	 * This performs the actual user authentication (confirms if account is enabled, active, and has proper credentials).
	 */
	public boolean authenticate(XDATUser u, Credentials cred)
			throws PasswordAuthenticationException, EnabledException,
			ActivationException, Exception {
		if(!ENABLED)return super.authenticate(u,cred);
		try {
			if(u.getXdatUserId()!=null){
				if (!u.isEnabled()) {
					logger.info(u.getFirstname() + ": disabled");
					throw new XDATUser.EnabledException(u.getLogin());
				}
				if (!u.isVerified()) {
					logger.info(u.getFirstname() + ": unverified");
					throw new XDATUser.VerifiedException(u.getLogin());
				}
				if ((!u.isActive()) && (!u.checkRole("Administrator"))) {
					logger.info(u.getFirstname() + ": needs activation");
					throw new XDATUser.ActivationException(u.getLogin());
				}
			}

			//if the user account has a stored password, then it is a LOCAL account and shouldn't be managed by LDAP.  Otherwise, use LDAP.
			if (StringUtils.isEmpty(u.getPrimaryPassword())) {
				//uses hand rolled credential caching to prevent LDAP from being flooded with duplicate authentication attempts, this is not required but nice.
				AuthenticationAttempt attempt = XnatLdapAuthenticator
						.RetrieveCachedAttempt(cred);
				if (attempt == null) {
					if (!cred.OTHER.containsKey(LDAP_USER_PK)) {
						//DN is cached here to prevent an additional query.
						String dn = retrieveStoredDN(u);
						logger.debug(u.getUsername()
								+ ": CACHED quarantine path.");
						if (StringUtils.isEmpty(dn)) {
							// BUILD FIRST ATTEMPT, retrieve DN
							logger.info(u.getUsername()
									+ ": requesting DN from server.");
							dn = this.getDN(cred);
						}
						cred.OTHER.put(LDAP_USER_PK, dn);
					}

					attempt = new AuthenticationAttempt(cred);
					attempt.expire();
					RecordAttempt(attempt);
				} else if (!attempt.isExpired()) {
					//the in-memory passwords should be SHA2'd
					if (attempt.cred.getPassword().equals(cred.getPassword())) {
						logger.info(u.getUsername()
								+ ": verified versus cache password. EXPIRES:"+ attempt.expires.getTime());
						return true;
					}else{
						attempt.expire();
						logger.info(u.getUsername()
								+ ": failed to verify against cached password. MIS-MATCH.");

					}
				}
				//failed to authenticate vs a cached password...

				//the in-memory passwords should be SHA2'd
				if (!attempt.cred.getPassword().equals(cred.getPassword())) {
					attempt.cred.setPassword(cred.getPassword());
				}

				// authenticate
				logger.info(u.getUsername()
						+ ": attempting to authenticate account from server.");
				if (this.verifyLogin(u,attempt.cred)) {
					logger
							.info(u.getUsername()
									+ ": validated against server.");
					attempt.setTimeout();
					return true;
				} else {
					logger.info(u.getUsername()
							+ ": validation FAILED against server.");
					return false;
				}

			} else {
				try {
					boolean auth = super.authenticate(u, cred);
					if (auth) {
						logger.info(u.getUsername()
								+ ": validated against db value.");
						return true;
					} else {
						logger.info(u.getUsername()
								+ ": Wrong Password against db value.");
						return false;
					}
				} catch (PasswordAuthenticationException e) {
					logger.info(u.getUsername()
							+ ": Wrong Password against db value.");
					throw e;
				}
			}
		} catch (Exception t) {
			throw t;
		} finally {
			this.closeContext();
		}
	}

	private static Hashtable<String, AuthenticationAttempt> cache = new Hashtable<String, AuthenticationAttempt>();

	public synchronized static AuthenticationAttempt RetrieveCachedAttempt(
			Credentials cred) {
		if (cache.containsKey(cred.getUsername())) {
			AuthenticationAttempt attempt = cache.get(cred.getUsername());
			return attempt;
		} else {
			return null;
		}
	}

	public synchronized static void RecordAttempt(AuthenticationAttempt attempt) {
		if (!cache.containsKey(attempt.cred.getUsername())) {
			cache.put(attempt.cred.getUsername(), attempt);
		}
	}

	public static class AuthenticationAttempt {
		public Credentials cred = null;
		int attempts = 0;
		Calendar expires = null;

		public AuthenticationAttempt(Credentials c) {
			this.cred = c;
			setTimeout();
		}

		public void setTimeout() {
			expires = Calendar.getInstance();
			expires.add(Calendar.SECOND, AUTHENTICATION_EXPIRATION);
		}

		public int getAttempts() {
			return attempts;
		}

		public void expire() {
			expires = Calendar.getInstance();
		}

		public void setAttempts(int attempts) {
			this.attempts = attempts;
		}

		public boolean isExpired() {
			Calendar cp = Calendar.getInstance();
			if (cp.before(expires)) {
				return false;
			} else
				return true;
		}
	}
}
