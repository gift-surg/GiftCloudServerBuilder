/*
 * org.nrg.user.entities.User
 * XNAT http://www.xnat.org
 * Copyright (c) 2013, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 *
 * Last modified 7/10/13 8:16 PM
 */
package org.nrg.user.entities;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.nrg.framework.orm.hibernate.AbstractHibernateEntity;
import org.nrg.framework.orm.hibernate.annotations.Auditable;

import javax.persistence.Entity;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

@Auditable
@Entity
@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"name"}))
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE, region = "xnat")
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
