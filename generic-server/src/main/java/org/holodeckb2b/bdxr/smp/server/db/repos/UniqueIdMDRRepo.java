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

import org.holodeckb2b.bdxr.smp.server.db.entities.BaseMetadataRegistrationEntity;
import org.holodeckb2b.bdxr.smp.server.services.core.ConstraintViolationException;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;


/**
 * Definition of a repository that handles entity classes for meta-data registrations that must have unique business 
 * identifiers. It defines an identifier based search and duplicate check capability. 
 *
 * @param <I> the class of the identifier used by <code>&lt;E&gt;</code>
 * @param <E> the entity class subject of the repository
 * @author Sander Fieten (sander at holodeck-b2b.org)
 */
public interface UniqueIdMDRRepo<I, E extends BaseMetadataRegistrationEntity<I>> {

	/**
	 * Finds the meta-data registration with the specified identifier.
	 * 
	 * @param id	the identifier
	 * @return	the meta-data registration if one exists with the specified identifier, or <code>null</code> if no such
	 * 			registration exists
	 */
	E findByIdentifier(I id);
	
	/**
	 * Saves the meta-data registration to the database, checking no other registration exists with the same identifier. 
	 * 
	 * @param r	entity object representing the meta-data registration to be saved to the database 
	 * @return	the updated entity object with the meta-data as stored in the database
	 * @throws ConstraintViolationException when there already exists another registration in the database with the same
	 * 										identifier
	 */
	@Transactional(isolation = Isolation.SERIALIZABLE, rollbackFor = ConstraintViolationException.class)
	E saveWithDuplicateCheck(E r) throws ConstraintViolationException;
}
