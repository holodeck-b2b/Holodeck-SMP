/*
 * Copyright (C) 2023 The Holodeck B2B Team
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
package org.holodeckb2b.bdxr.smp.server.db.repos;

import org.holodeckb2b.bdxr.smp.server.datamodel.MetadataRegistration;
import org.holodeckb2b.bdxr.smp.server.db.entities.BaseMetadataRegistrationEntity;
import org.holodeckb2b.bdxr.smp.server.services.core.ConstraintViolationException;
import org.holodeckb2b.bdxr.smp.server.services.core.ConstraintViolationException.ViolationType;
import org.springframework.beans.factory.annotation.Autowired;

import jakarta.persistence.EntityManager;
import jakarta.persistence.NonUniqueResultException;
import lombok.extern.slf4j.Slf4j;

/**
 * Base implementation of {@link UniqueIdMDRRepo} that provides a default implementation of the {@link 
 * UniqueIdMDRRepo#save()))} method. It uses the {@link #findByIdentifier()} method to find an existing registration 
 * with the same identifier and then compares it to the registration being saved using the {@link equals()}
 * method. As the {@link BaseMetadataRegistrationEntity} implementation uses the primary key for this comparison this
 * works correctly.  
 *
 * @param <I> the class of the identifier used by <code>&lt;E&gt;</code>
 * @param <E> the entity class subject of the repository
 * @author Sander Fieten (sander at holodeck-b2b.org)
 */
@Slf4j
abstract class DuplicateIdCheckRepoImpl<I, E extends BaseMetadataRegistrationEntity<I>> 
																	implements UniqueIdMDRRepo<I, E> {

	@Autowired
	protected EntityManager	em;

	@SuppressWarnings("rawtypes")
	@Override
	public E save(E r) throws ConstraintViolationException {	
		E existing = findByIdentifier(r.getId());		
		if (existing != null && !existing.getOid().equals(r.getOid())) 
			throw new ConstraintViolationException(ViolationType.DUPLICATE_ID, (MetadataRegistration) r);
		try {
			if (r.getOid() == null)
				em.persist(r);
			else 
				r = em.merge(r);			
			em.flush();
		} catch (jakarta.persistence.PersistenceException saveFailure) {
			// Probably caused by a parallel transaction. Check that it didn't add the same identifier
			// as the entity we tried to save now
			try {
				existing = findByIdentifier(r.getId());		
				if (existing != null && !r.equals(existing)) 
					throw new ConstraintViolationException(ViolationType.DUPLICATE_ID, (MetadataRegistration) r);				
			} catch (NonUniqueResultException hasDuplicateId) {
				// This shouldn't happen as their should be one registration with this identifier. It could be that the
				// check also found the record we just saved, but it could also be that the database now contains 
				// multiple registrations with the same identifier. Therefore we log a warning and throw the exception
				// to indicate the duplicate.
				log.warn("Possible inconsistency. Found multiple {} registrations with the identifier {}!",
						r.getClass().getSimpleName(), r.getId().toString());
				throw new ConstraintViolationException(ViolationType.DUPLICATE_ID, (MetadataRegistration) r);				
			}
		}		
		return r;
	}

}
