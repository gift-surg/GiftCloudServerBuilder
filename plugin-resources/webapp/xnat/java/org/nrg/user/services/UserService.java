/**
 * UserService
 * (C) 2011 Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD License
 *
 * Created on Oct 10, 2011 by Rick Herrick <rick.herrick@wustl.edu>
 */
package org.nrg.user.services;

import org.nrg.framework.orm.hibernate.BaseHibernateService;
import org.nrg.user.entities.User;

/**
 * 
 * @author Rick Herrick <rick.herrick@wustl.edu>
 */
public interface UserService extends BaseHibernateService<User> {
    abstract public User getUserByName(String name);
}
