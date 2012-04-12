/**
 * UserDAO
 * (C) 2011 Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD License
 *
 * Created on Oct 10, 2011 by Rick Herrick <rick.herrick@wustl.edu>
 */
package org.nrg.user.daos;

import org.hibernate.Criteria;
import org.hibernate.criterion.Restrictions;
import org.nrg.framework.orm.hibernate.AbstractHibernateDAO;
import org.nrg.user.entities.User;
import org.springframework.stereotype.Repository;

/**
 * 
 * @author Rick Herrick <rick.herrick@wustl.edu>
 */
@Repository
public class UserDAO extends AbstractHibernateDAO<User> {

    /**
     * @param name
     * @return
     */
    public User getUserByName(String name) {
        Criteria criteria = getSession().createCriteria(getParameterizedType());
        criteria.add(Restrictions.eq("name", name));
        criteria.add(Restrictions.eq("enabled", true));
        return (User) criteria.list().get(0);
    }

}
