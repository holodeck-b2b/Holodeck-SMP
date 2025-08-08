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

import org.holodeckb2b.bdxr.smp.server.datamodel.IDScheme;
import org.holodeckb2b.bdxr.smp.server.db.entities.IDSchemeEntity;
import org.holodeckb2b.bdxr.smp.server.db.repos.IDSchemeRepository;
import org.holodeckb2b.bdxr.smp.server.services.core.IdSchemeMgmtService;
import org.holodeckb2b.bdxr.smp.server.services.core.PersistenceException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;

/**
 * Implements the {@link IdSchemeMgmtService}
 * 
 * @author Sander Fieten (sander at holodeck-b2b.org)
 */
@Service
@Slf4j
public class IdSchemeMgmtServiceImpl extends IdBasedEntityMgmtServiceImpl<String, IDSchemeEntity, IDSchemeRepository>
										implements IdSchemeMgmtService {

	@Override
	protected String getAuditDetails(IDSchemeEntity entity) {
		return String.format("Name=%s,agency=%s,schemeRef=%s", 
							entity.getName(), entity.getAgency(), entity.getSchemeSpecificationRef());			
	}

	@Override
	public IDScheme addIDScheme(UserDetails user, IDScheme scheme) throws PersistenceException {
		return executeCRUD(CrudOps.Add, user, new IDSchemeEntity(scheme.getSchemeId(), 
																 scheme.isCaseSensitive(),
																 scheme.getName(), 
																 scheme.getAgency(), 
																 scheme.getSchemeSpecificationRef()));	
	}

	@Override
	public IDScheme updateIDScheme(UserDetails user, IDScheme scheme) throws PersistenceException {
		return executeCRUD(CrudOps.Update, user, checkManaged(scheme));
	}

	@Override
	public void deleteIDScheme(UserDetails user, IDScheme scheme) throws PersistenceException {
		executeCRUD(CrudOps.Delete, user, checkManaged(scheme));
	}
	
	@Override
	public Collection<? extends IDScheme> getIDSchemes() throws PersistenceException {
		return getAll();
	}

	@Override
	public Page<IDSchemeEntity> getIDSchemes(PageRequest request) throws PersistenceException {
		return retrieveSubset(request);
	}

	@Override
	public IDScheme getIDScheme(String schemeId) throws PersistenceException {
		return getById(schemeId);
	}	
}
