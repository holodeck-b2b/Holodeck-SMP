/*
 * Copyright (C) 2022 The Holodeck B2B Team
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

import org.holodeckb2b.bdxr.smp.server.db.entities.EndpointEntity;
import org.holodeckb2b.bdxr.smp.server.db.entities.TransportProfileEntity;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * The Spring JPA repository for Endpoint meta-data.
 *
 * @author Sander Fieten (sander at holodeck-b2b.org)
 */
public interface EndpointRepository extends JpaRepository<EndpointEntity, Long> {

	/**
	 * Counts all Endpoints that use the given Transport Profile.
	 * 
	 * @param tp	the Transport Profile to select the Endpoints
	 * @return		number of Endpoints that support the given profile
	 */
	int countByTransportProfile(TransportProfileEntity tp);

	/**
	 * Finds all Endpoints that use the given Transport Profile.
	 * 
	 * @param tp	the Transport Profile to select the Endpoints
	 * @return		Collection with all Endpoints that support the given profile
	 */
	Collection<EndpointEntity> findByTransportProfile(TransportProfileEntity tp);
}
