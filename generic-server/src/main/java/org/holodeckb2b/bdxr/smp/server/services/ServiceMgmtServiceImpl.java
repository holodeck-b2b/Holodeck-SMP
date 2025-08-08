/*
 * Copyright (C) 2025 The Holodeck B2B Team
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Affero GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.holodeckb2b.bdxr.smp.server.services;

import java.util.Collection;
import java.util.NoSuchElementException;

import org.holodeckb2b.bdxr.smp.server.datamodel.Identifier;
import org.holodeckb2b.bdxr.smp.server.datamodel.Service;
import org.holodeckb2b.bdxr.smp.server.db.entities.ServiceEntity;
import org.holodeckb2b.bdxr.smp.server.db.repos.ServiceRepository;
import org.holodeckb2b.bdxr.smp.server.services.core.ConstraintViolationException;
import org.holodeckb2b.bdxr.smp.server.services.core.ConstraintViolationException.ViolationType;
import org.holodeckb2b.bdxr.smp.server.services.core.PersistenceException;
import org.holodeckb2b.bdxr.smp.server.services.core.ServiceMgmtService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.userdetails.UserDetails;

import lombok.extern.slf4j.Slf4j;

/**
 * Implements the {@link ServiceMgmtService}.
 * 
 * @author Sander Fieten (sander at holodeck-b2b.org)
 */
@Slf4j
@org.springframework.stereotype.Service
public class ServiceMgmtServiceImpl 
				extends IdBasedEntityMgmtServiceImpl<Identifier, ServiceEntity, ServiceRepository> 
				implements ServiceMgmtService {

	@Override
	protected String getAuditDetails(ServiceEntity entity) {
		return String.format("ID=%s,specRef=%s", entity.getId().toString(), entity.getSpecificationRef());			
	}
	
	@Override
	public Service addService(UserDetails user, Service service) throws PersistenceException {
		try {
			return executeCRUD(CrudOps.Add, user, 
					new ServiceEntity(idUtils.toEmbeddedIdentifier(service.getId()), 
										service.getName(), service.getSpecificationRef()));
		} catch (NoSuchElementException unknownScheme) {
			log.error("Unknown ID scheme {} used in Identifier", service.getId().getScheme().getSchemeId());
			throw new ConstraintViolationException(ViolationType.INVALID_FIELD, service, "Identifier.IDScheme");
		}
	}

	@Override
	public Service updateService(UserDetails user, Service service) throws PersistenceException {
		return executeCRUD(CrudOps.Update, user, checkManaged(service));
	}

	@Override
	public void deleteService(UserDetails user, Service service) throws PersistenceException {
		executeCRUD(CrudOps.Delete, user, checkManaged(service));
	}

	@Override
	public Collection<? extends Service> getServices() throws PersistenceException {
		return getAll();
	}
	
	@Override
	public Page<ServiceEntity> getServices(PageRequest request) throws PersistenceException {
		return retrieveSubset(request);
	}

	@Override
	public Service getService(org.holodeckb2b.bdxr.smp.datamodel.Identifier svcId) throws PersistenceException {
		try {
			return getById(idUtils.toEmbeddedIdentifier(svcId));
		} catch (NoSuchElementException unknownScheme) {
			return null;
		}
	}
}
