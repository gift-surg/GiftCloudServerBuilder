/**
 * User
 * (C) 2011 Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD License
 *
 * Created on Oct 10, 2011 by Rick Herrick <rick.herrick@wustl.edu>
 */
package org.nrg.user.entities;

import javax.persistence.Entity;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

import org.nrg.framework.orm.hibernate.AbstractHibernateEntity;
import org.nrg.framework.orm.hibernate.annotations.Auditable;

/**
 * 
 * @author Rick Herrick <rick.herrick@wustl.edu>
 */
@Auditable
@Entity
@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"name"}))
public class User extends AbstractHibernateEntity {
    /**
     * @param name Sets the name property.
     */
    public void setName(String name) {
        _name = name;
    }

    /**
     * @return Returns the name property.
     */
    public String getName() {
        return _name;
    }

    private String _name;
}
