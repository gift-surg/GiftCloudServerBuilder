package org.nrg.xnat.security;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nrg.xdat.XDAT;
import org.nrg.xdat.entities.XDATUserDetails;
import org.nrg.xdat.security.XDATUser;
import org.nrg.xdat.services.XdatUserAuthService;
import org.springframework.ldap.core.DirContextOperations;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.ldap.userdetails.LdapUserDetailsMapper;
import org.springframework.util.Assert;

public class XnatLdapUserDetailsMapper extends LdapUserDetailsMapper {
	
    public static final String PROPERTY_PREFIX = "attributes.";
    public static final String PROPERTY_EMAIL = PROPERTY_PREFIX + "email";
    public static final String PROPERTY_FIRST = PROPERTY_PREFIX + "firstname";
    public static final String PROPERTY_LAST = PROPERTY_PREFIX + "lastname";
	
    public XnatLdapUserDetailsMapper(final String authMethodId, final Map<String, String> properties) {
		super();
        Assert.hasText(authMethodId, "You must provide an authentication method ID.");
        Assert.notEmpty(properties, "You must provide the authentication provider properties.");
        if (_log.isInfoEnabled()) {
            _log.info("Creating user details mapper with the auth method ID [" + authMethodId + "] and " + (properties != null && properties.size() > 0 ? "mapping properties: " + properties.toString() : "default mapping properties"));
        }
        _authMethodId = authMethodId;
        if (properties == null || properties.size() == 0) {
            _properties = new HashMap<String, String>(3) {{ put(PROPERTY_EMAIL, "mail"); put(PROPERTY_FIRST, "givenName"); put(PROPERTY_LAST, "sn"); }};
        } else {
            if (!properties.containsKey(PROPERTY_EMAIL)) {
                properties.put(PROPERTY_EMAIL, "mail");
            }
            if (!properties.containsKey(PROPERTY_FIRST)) {
                properties.put(PROPERTY_FIRST, "givenName");
            }
            if (!properties.containsKey(PROPERTY_LAST)) {
                properties.put(PROPERTY_LAST, "sn");
            }
            _properties = properties;
        }
	}
	
    public XDATUserDetails mapUserFromContext(DirContextOperations ctx, String username, Collection<GrantedAuthority> authorities) {
        UserDetails user = super.mapUserFromContext(ctx, username, authorities);

        String email = (String) ctx.getObjectAttribute(_properties.get(PROPERTY_EMAIL));
        String firstname = (String) ctx.getObjectAttribute(_properties.get(PROPERTY_FIRST));
        String lastname = (String) ctx.getObjectAttribute(_properties.get(PROPERTY_LAST));

        XDATUserDetails userDetails = XDAT.getXdatUserAuthService().getUserDetailsByNameAndAuth(user.getUsername(), XdatUserAuthService.LDAP, _authMethodId, email, lastname, firstname);
        
        try{
        	XDATUser xdatUser = new XDATUser(user.getUsername());
	        if( ((!(XDAT.verificationOn()))||xdatUser.isVerified()) && userDetails.getAuthorization().isEnabled() )
	        {
	        	return userDetails;
	        }
	        else
	        {
	        	throw new NewLdapAccountNotAutoEnabledException(
	        			"Successful first-time authentication via LDAP, but accounts are not auto-enabled or email verification required.  We'll treat this the same as we would a user registration"
	        			, userDetails
	        	); 
	        }
        }
        catch(Exception e){
        	throw new NewLdapAccountNotAutoEnabledException(
        			"Successful first-time authentication via LDAP, but accounts are not auto-enabled or email verification required.  We'll treat this the same as we would a user registration"
        			, userDetails
        	); 
        }
    }

    private static final Log _log = LogFactory.getLog(XnatLdapUserDetailsMapper.class);

    private final String _authMethodId;
    private final Map<String, String> _properties;
}
