package org.nrg.xnat.security.alias;

import org.nrg.xdat.XDAT;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import java.lang.Long;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;

/**
 * This class supports translating between {@link org.nrg.xdat.entities.AliasToken alias token credentials} and standard username/password
 * credentials. Initialization of this should usually set the principal and credentials to the token's alias and secret
 * respectively. As the alias is translated into a specific user on the server side, the principal and credentials can
 * be converted to that user's, while the alias and secret remain the same. To accomplish this, this class overrides the
 * {@link org.springframework.security.authentication.UsernamePasswordAuthenticationToken#getPrincipal()} and
 * {@link org.springframework.security.authentication.UsernamePasswordAuthenticationToken#getCredentials()} methods to
 * return its own version of these data (the base class doesn't provide setters for the properties and the principal
 * data store is actually declared <b>final</b>).
 */
public class AliasTokenAuthenticationToken extends UsernamePasswordAuthenticationToken{
	public AliasTokenAuthenticationToken(Object principal, Object credentials) {
		super(principal, credentials);
        _principal = principal;
        _credentials = credentials;
        _alias = (String) principal;
        _secret = (Long) credentials;
	}

    @Override
    public Object getPrincipal() {
        return _principal;
    }

    public void setPrincipal(Object principal) {
        _principal = principal;
    }

    @Override
    public Object getCredentials() {
        return _credentials;
    }

    public void setCredentials(Object credentials) {
        _credentials = credentials;
    }

    public String getAlias() {
        return _alias;
    }

    public long getSecret() {
        return _secret;
    }

    @Override
	public String toString(){
		return getPrincipal().toString();
	}

    private Object _principal;
    private Object _credentials;
    private String _alias;
    private long _secret;
}
