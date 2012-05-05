/**
 * HibernateUserService
 * (C) 2011 Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD License
 *
 * Created on Oct 10, 2011 by Rick Herrick <rick.herrick@wustl.edu>
 */
package org.nrg.user.services.impl.hibernate;

import org.nrg.framework.orm.hibernate.AbstractHibernateEntityService;
import org.nrg.user.daos.UserDAO;
import org.nrg.user.entities.User;
import org.nrg.user.services.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 
 * @author Rick Herrick <rick.herrick@wustl.edu>
 */
@Service
public class HibernateUserService extends AbstractHibernateEntityService<User> implements UserService {

    /**
     * @see org.nrg.user.services.UserService#getUserByName(java.lang.String)
     */
    @Override
    @Transactional
    public User getUserByName(String name) {
        return _dao.getUserByName(name);
    }

    /**
     * @see org.nrg.framework.orm.hibernate.AbstractHibernateEntityService#newEntity()
     */
    @Override
    public User newEntity() {
        return new User();
    }

    /**
     * @see org.nrg.framework.orm.hibernate.AbstractHibernateEntityService#getDao()
     */
    @Override
    protected UserDAO getDao() {
        return _dao;
    }

    @Autowired
    private UserDAO _dao;
}