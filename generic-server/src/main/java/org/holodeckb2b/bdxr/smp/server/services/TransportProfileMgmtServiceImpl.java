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
import org.holodeckb2b.bdxr.smp.server.datamodel.TransportProfile;
import org.holodeckb2b.bdxr.smp.server.db.entities.TransportProfileEntity;
import org.holodeckb2b.bdxr.smp.server.db.repos.TransportProfileRepository;
import org.holodeckb2b.bdxr.smp.server.services.core.ConstraintViolationException;
import org.holodeckb2b.bdxr.smp.server.services.core.ConstraintViolationException.ViolationType;
import org.holodeckb2b.bdxr.smp.server.services.core.PersistenceException;
import org.holodeckb2b.bdxr.smp.server.services.core.TransportProfileMgmtService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;

/**
 * Implements the {@link TransportProfileMgmtService}.
 * 
 * @author Sander Fieten (sander at holodeck-b2b.org)
 */
@Slf4j
@Service
public class TransportProfileMgmtServiceImpl 
			extends IdBasedEntityMgmtServiceImpl<Identifier, TransportProfileEntity, TransportProfileRepository> 
			implements TransportProfileMgmtService {

	@Override
	protected String getAuditDetails(TransportProfileEntity entity) {
		return String.format("ID=%s,specRef=%s", entity.getId().toString(), entity.getSpecificationRef());			
	}

	@Override
	public TransportProfile addTransportProfile(UserDetails user, TransportProfile profile) throws PersistenceException {
		try {
			return executeCRUD(CrudOps.Add, user, 
							new TransportProfileEntity(idUtils.toEmbeddedIdentifier(profile.getId()), 
												profile.getName(), profile.getSpecificationRef()));
		} catch (NoSuchElementException unknownScheme) {
			log.error("Unknown ID scheme {} used in Identifier", profile.getId().getScheme().getSchemeId());
			throw new ConstraintViolationException(ViolationType.INVALID_FIELD, profile, "Identifier.IDScheme");
		}
	}

	@Override
	public TransportProfile updateTransportProfile(UserDetails user, TransportProfile profile) throws PersistenceException {
		return executeCRUD(CrudOps.Update, user, checkManaged(profile));
	}

	@Override
	public void deleteTransportProfile(UserDetails user, TransportProfile profile) throws PersistenceException {
		executeCRUD(CrudOps.Delete, user, checkManaged(profile));
	}

	@Override
	public Collection<? extends TransportProfile> getTransportProfiles() throws PersistenceException {
		return getAll();
	}
	
	@Override
	public Page<TransportProfileEntity> getTransportProfiles(PageRequest request) throws PersistenceException {
		return retrieveSubset(request);
	}

	@Override
	public TransportProfile getTransportProfile(org.holodeckb2b.bdxr.common.datamodel.Identifier profileId) throws PersistenceException {		
		try {
			return getById(idUtils.toEmbeddedIdentifier(profileId));
		} catch (NoSuchElementException unknownScheme) {
			return null;
		}
	}
}
