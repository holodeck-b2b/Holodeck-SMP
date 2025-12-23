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

import java.util.Collection;

import org.holodeckb2b.bdxr.smp.server.datamodel.Identifier;
import org.holodeckb2b.bdxr.smp.server.db.entities.ParticipantEntity;

/**
 * Defines the Identifier based search and duplicate check capabilities for the {@link ParticipantEntity} based 
 * repository.
 *
 * @author Sander Fieten (sander at holodeck-b2b.org)
 */
public interface ParticipantIdCapableRepo extends UniqueIdMDRRepo<Identifier, ParticipantEntity> {
	
	/**
	 * Finds the Participants that have the given additional identifier.  
	 * 
	 * @param id	the identifier to search for
	 * @return	a collection of Participants for which used the given identifier as an additional identifier
	 */
	Collection<ParticipantEntity> findByAdditionalId(org.holodeckb2b.bdxr.common.datamodel.Identifier id);
}
