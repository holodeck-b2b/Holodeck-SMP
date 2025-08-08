/*
 * Copyright (C) 2025 The Holodeck B2B Team
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.holodeckb2b.bdxr.smp.server.services.core;

import java.util.Collection;

import org.holodeckb2b.bdxr.smp.server.datamodel.Endpoint;
import org.holodeckb2b.bdxr.smp.server.datamodel.TransportProfile;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.userdetails.UserDetails;

/**
 * Defines the interface of the central service for the management of <i>Endpoint</i> registrations in the Holodeck SMP 
 * instance.
 * 
 * @author Sander Fieten (sander at holodeck-b2b.org)
 */
public interface EndpointMgmtService {

	/**
	 * Adds a new Endpoint to the data set managed by this Holodeck SMP instance and returns the registered instance 
	 * which must be used for further processing. This method will assign an identifier to the new registration. 
	 * Therefore the provided meta-data SHOULD NOT contain an identifier, but if one is provided it will be ignored.
	 * 
	 * @param user	the User registering the Endpoint, required for audit logging
	 * @param ep 	the meta-data on the new Endpoint
	 * @return	the registered Endpoint, this object MUST be used for further processing of the Endpoint
	 * @throws PersistenceException	when an error occurs saving the Service meta-data
	 */
	Endpoint addEndpoint(UserDetails user, Endpoint ep) throws PersistenceException;
	
	/**
	 * Updates the meta-data of an existing Endpoint.
	 * 
	 * @param user	the User updating the Endpoint, required for audit logging
	 * @param ep	the updated meta-data of the Endpoint, MUST be an instance that was obtained from this service
	 * @return	the updated Endpoint registration, this object MUST be used for further processing of the Endpoint
	 * @throws PersistenceException	when an error occurs saving the Endpoint meta-data
	 */
	Endpoint updateEndpoint(UserDetails user, Endpoint ep) throws PersistenceException;
	
	/**
	 * Removes the Endpoint registration.
	 * 
	 * @param user	the User removing the Endpoint, required for audit logging
	 * @param ep	the Endpoint to remove
	 * @throws PersistenceException	when an error occurs removing the Endpoint
	 */
	void deleteEndpoint(UserDetails user, Endpoint ep) throws PersistenceException;
	
	/**
	 * Gets all registered Endpoints.
	 * 
	 * @return	collection with the meta-data of all registered Endpoints, empty collection if no Endpoints are 
	 * 			registered
	 * @throws PersistenceException	when an error occurs retrieving the meta-data of the Endpoints
	 */
	Collection<? extends Endpoint> getEndpoints() throws PersistenceException;
	
	/**
	 * Gets a subset of registered Endpoints.
	 * 
	 * @param request	a {@link PageRequest} specifying the requested subset
	 * @return	collection with the meta-data of all registered Endpoints in the requested subset, empty collection if 
	 * 			the specified set does not contain any Endpoints
	 * @throws PersistenceException	when an error occurs retrieving the meta-data of the Endpoints
	 */
	Page<? extends Endpoint> getEndpoints(PageRequest request) throws PersistenceException;
	
	/**
	 * Gets the registered Endpoint with the given identifier.
	 * 
	 * @param id 	the identifier of the Endpoint to retrieve
	 * @return	the meta-data of the registered Endpoint with the given identifier if it exists, 
	 * 			<code>null</code> otherwise
	 * @throws PersistenceException	when an error occurs retrieving the meta-data of the Endpoint
	 */
	Endpoint getEndpoint(Long id) throws PersistenceException;

	/**
	 * Counts the number of Endpoints that use the given TransportProfile. Although the number of Endpoint supporting 
	 * a transport profile can also be determined by counting the number of elements in the collection returned by 
	 * {@link #findEndpointsUsingProfile(TransportProfile)}, this method can be optimised as the actual data doesn't 
	 * need to loaded.
	 * 
	 * @param profile	the TransportProfile to search for
	 * @return	the number of Endpoints that use the given TransportProfile
	 * @throws PersistenceException	when an error occurs counting the Endpoints
	 */
	int countEndpointsUsingProfile(TransportProfile profile) throws PersistenceException;

	/**
	 * Finds the Endpoints that use the given TransportProfile.
	 * 
	 * @param profile	the TransportProfile to search for
	 * @return	the Endpoints that use the given TransportProfile
	 * @throws PersistenceException	when an error occurs retrieving the meta-data of the Endpoints
	 */
	Collection<? extends Endpoint> findEndpointsUsingProfile(TransportProfile profile) throws PersistenceException;
}
