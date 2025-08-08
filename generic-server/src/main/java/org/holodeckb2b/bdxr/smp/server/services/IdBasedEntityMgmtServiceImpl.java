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

import org.holodeckb2b.bdxr.smp.server.db.entities.BaseMetadataRegistrationEntity;
import org.holodeckb2b.bdxr.smp.server.db.repos.UniqueIdMDRRepo;
import org.holodeckb2b.bdxr.smp.server.services.core.PersistenceException;
import org.holodeckb2b.bdxr.smp.server.utils.IdUtils;
import org.holodeckb2b.commons.util.Utils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.repository.JpaRepository;

import lombok.extern.slf4j.Slf4j;

/**
 * Extends {@link BaseMgmtServiceImpl} to provide a method for retrieving a meta-data registration based on its 
 * identifier.
 * 
 * @author Sander Fieten (sander at holodeck-b2b.org)
 */
@Slf4j
abstract class IdBasedEntityMgmtServiceImpl<I, E extends BaseMetadataRegistrationEntity<I>,
											R extends UniqueIdMDRRepo<I, E> & JpaRepository<E, Long>> 
				extends BaseMgmtServiceImpl<I, E, R> {

	@Autowired
	protected IdUtils idUtils;
	
	/**
	 * Retrieves the entity object representing the registered meta-data with the specified identifier.
	 * 
	 * @param id	identifier of the meta-data registration to retrieve
	 * @return	entity object representing the meta-data with the specified identifier
	 * @throws PersistenceException	when an error occurs retrieving the meta-data
	 */
	protected E getById(I id) throws PersistenceException {
		try {
			log.trace("Retrieving {} with id={}", mdrName, id);			
			return repo.findByIdentifier(id);
		} catch (Throwable t) {
			log.error("An error occurred retrieving the {} with id={} : {}", mdrName, id, 
						Utils.getExceptionTrace(t));
			throw new PersistenceException("Failed to retrieve " + mdrName, t);
		}
	}	
}
