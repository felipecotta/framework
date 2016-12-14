/*
 * Demoiselle Framework
 *
 * License: GNU Lesser General Public License (LGPL), version 3 or later.
 * See the lgpl.txt file in the root directory or <https://www.gnu.org/licenses/lgpl.html>.
 */
package org.demoiselle.jee.multitenancy.hibernate.dao;

import javax.inject.Inject;
import javax.persistence.EntityManager;

import org.demoiselle.jee.multitenancy.hibernate.context.MultiTenantContext;
import org.demoiselle.jee.multitenancy.hibernate.dao.context.EntityManagerMaster;
import org.demoiselle.jee.multitenancy.hibernate.entity.Tenant;
import org.demoiselle.jee.persistence.crud.AbstractDAO;

/**
 * This class contains the methods required to interact with Tenant entity in
 * database.
 * 
 * @author SERPRO
 *
 */
public class TenantDAO extends AbstractDAO<Tenant, Long> {

	@Inject
	private MultiTenantContext multiTenantContext;

	@Inject
	private EntityManagerMaster entityManagerMaster;

	protected EntityManager getEntityManager() {
		return entityManagerMaster.getEntityManager();
	}

	/**
	 * Constructor for CDI @Inject
	 */
	public TenantDAO() {

	}

	public MultiTenantContext getMultiTenantContext() {
		return multiTenantContext;
	}

	/**
	 * Find Tenant by name
	 * 
	 * @param name
	 *            Tenant name
	 * @return Tenant
	 */
	public Tenant findByName(String name) {
		Tenant tenant = getEntityManager().createNamedQuery("Tenant.findByName", Tenant.class)
				.setParameter("name", name).getSingleResult();

		return tenant;
	}

}
