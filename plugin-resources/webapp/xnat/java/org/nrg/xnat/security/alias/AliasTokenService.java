/**
 * AliasTokenService
 * (C) 2012 Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD License
 *
 * Created on 4/17/12 by rherri01
 */
package org.nrg.xnat.security.alias;

import org.nrg.framework.orm.hibernate.BaseHibernateService;
import org.nrg.xdat.om.XdatUser;
import org.nrg.xnat.security.alias.entities.AliasToken;

import java.util.Set;

public interface AliasTokenService extends BaseHibernateService<AliasToken> {
    /**
     * Issues a token to the user with the indicated name.
     *
     * @param xdatUserId    The user ID from the XdatUser table.
     * @return An {@link AliasToken} issued to the indicated user.
     * @throws Exception When something goes wrong.
     */
    abstract public AliasToken issueTokenForUser(String xdatUserId) throws Exception;
    /**
     * Issues a token to the indicated user. This calls the {@link #issueTokenForUser(XdatUser, boolean)} version of
     * this method, passing <b>false</b> by default for the boolean parameter.
     *
     * @param xdatUser    The user requesting a token.
     * @return An {@link AliasToken} issued to the indicated user.
     */
    abstract public AliasToken issueTokenForUser(XdatUser xdatUser);
    /**
     * Issues a token to the indicated user. The <b>isSingleUse</b> parameter indicates whether the issued token should
     * be disposed of when the token is used.
     *
     * @param xdatUser    The user requesting a token.
     * @param isSingleUse Indicates whether the token should be disposed of once the token is used once.
     * @return An {@link AliasToken} issued to the indicated user.
     */
    abstract public AliasToken issueTokenForUser(XdatUser xdatUser, boolean isSingleUse);
    /**
     * Issues a token to the indicated user. The <b>validIPAddresses</b> parameter indicates which originating IPs
     * should be permitted to offer the returned alias tokens. Note that there is nothing in the issued token that
     * indicates the acceptable IP addresses.
     *
     * @param xdatUser    The user requesting a token.
     * @param validIPAddresses    The list of IP addresses from which the alias token will be accepted.
     * @return An {@link AliasToken} issued to the indicated user.
     */
    abstract public AliasToken issueTokenForUser(XdatUser xdatUser, Set<String> validIPAddresses);
    /**
     * Issues a token to the indicated user.  The <b>isSingleUse</b> parameter indicates whether the issued token should
     * be disposed of when the token is used.The <b>validIPAddresses</b> parameter indicates which originating IPs
     * should be permitted to offer the returned alias tokens. Note that there is nothing in the issued token that
     * indicates the acceptable IP addresses.
     *
     * @param xdatUser    The user requesting a token.
     * @param isSingleUse Indicates whether the token should be disposed of once the token is used once.
     * @param validIPAddresses    The list of IP addresses from which the alias token will be accepted.
     * @return An {@link AliasToken} issued to the indicated user.
     */
    abstract public AliasToken issueTokenForUser(XdatUser xdatUser, boolean isSingleUse, Set<String> validIPAddresses);

    /**
     * Locates and returns the token indicated by the alias string. The returned token should not be considered fully
     * validated until the {@link AliasToken#getSecret() token secret} and {@link AliasToken#getValidIPAddresses() IP
     * addresses} have also been checked and validated against the requesting client.
     *
     * @param alias    The alias for the requested token.
     * @return The token matching the indicated alias if one exists; otherwise this returns null.
     */
    abstract public AliasToken locateToken(String alias);

    /**
     * Checks whether a token exists with the indicated alias and secret and no IP address restrictions. If so, this
     * method returns the {@link XdatUser#getLogin() corresponding XDAT user login ID}. Otherwise, this method returns
     * <b>null</b>.
     *
     * @param alias     The alias to check.
     * @param secret    The secret to validate the indicated alias.
     * @return The {@link XdatUser#getLogin() XDAT user login ID} of the matching token exists, or <b>null</b> if not.
     */
    abstract public String validateToken(String alias, long secret);

    /**
     * Checks whether a token exists with the indicated alias and secret and an IP address matching one of the defined
     * IP addresses (if there are no IP address restrictions in the token, any given IP address will match). If so, this
     * method returns the {@link XdatUser#getLogin() corresponding XDAT user login ID}. Otherwise, this method returns
     * <b>null</b>.
     *
     * @param alias     The alias to check.
     * @param secret    The secret to validate the indicated alias.
     * @param address   The IP address to validate.
     * @return The {@link XdatUser#getLogin() XDAT user login ID} of the matching token exists, or <b>null</b> if not.
     */
    abstract public String validateToken(String alias, long secret, String address);

    /**
     * Invalidates the token with the given alias. No supporting validation is required for this operation.
     *
     * @param alias    The alias of the token to be invalidated.
     */
    abstract public void invalidateToken(String alias);
}
