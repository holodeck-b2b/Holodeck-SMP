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

import java.util.List;
import org.holodeckb2b.bdxr.smp.datamodel.Identifier;
import org.holodeckb2b.bdxr.smp.server.db.entities.ServiceMetadataBindingE;
import org.springframework.data.repository.query.Param;

/**
 * Specifies the queries for getting the <i>Service Metadata Template</i>(s) of a Participant. Because the queries need
 * to observe the case sensitivity specified by the identifier schemes a custom repository implementation is used.
 *
 * @author Sander Fieten (sander at holodeck-b2b.org)
 */
public interface SMBSchemeBasedQueryingRepository {

	/**
	 * Gets all <i>Service Metadata Templates</i> supported for the given <i>Participant</i>.
	 * This query respects the case sensitivity specified by the Participant identifier's scheme.
	 *
	 * @param partID	the identifier of the Participant
	 * @return	all {@link ServiceMetadataBindingE}s of the Participant with the specified identifier.
	 */
	List<ServiceMetadataBindingE> findByParticipantId(@Param("id") Identifier partID);

	/**
	 * Get the <i>Service Metadata Template</i> for the given <i>Participant</i> and <i>Service</i>.
	 * This query respects the case sensitivity specified by the Participant and Service identifier's schemes.
	 *
	 * @param partID		the identifier of the Participant
	 * @param serviceID		the identifier of the Service
	 * @return the {@link ServiceMetadataBindingE} instance for the specified Participant and Service,
	 *		   or <code>null</code> if no such binding exists
	 */
	ServiceMetadataBindingE findByParticipantAndServiceId(@Param("pid") Identifier partID, @Param("sid") Identifier serviceID);
}
