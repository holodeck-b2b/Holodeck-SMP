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

import org.holodeckb2b.bdxr.common.datamodel.Identifier;
import org.holodeckb2b.bdxr.smp.server.datamodel.Service;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.userdetails.UserDetails;

/**
 * Defines the interface of the central service for the management of <i>Service</i> registrations in the Holodeck SMP 
 * instance.
 * 
 * @author Sander Fieten (sander at holodeck-b2b.org)
 */
public interface ServiceMgmtService {

	/**
	 * Adds a new Service to the data set managed by this Holodeck SMP instance and returns the registered instance 
	 * which must be used for further processing. If the Service Identifier is assigned an ID Scheme the scheme MUST 
	 * already be registered in the SMP instance.
	 * 
	 * @param user		the User registering the Service, required for audit logging
	 * @param service 	the meta-data on the new Service
	 * @return	the registered Service, this object MUST be used for further processing of the Service
	 * @throws PersistenceException	when an error occurs saving the Service meta-data
	 */
	Service addService(UserDetails user, Service service) throws PersistenceException;
	
	/**
	 * Updates the meta-data of an existing Service.
	 * 
	 * @param user	the User updating the Service, required for audit logging
	 * @param service	the updated meta-data of the Service, MUST be an instance that was obtained from this service
	 * @return	the registered Service, this object MUST be used for further processing of the Service
	 * @throws PersistenceException	when an error occurs saving the Service meta-data
	 */
	Service updateService(UserDetails user, Service service) throws PersistenceException;
	
	/**
	 * Removes the Service registration.
	 * 
	 * @param user		the User removing the Service, required for audit logging
	 * @param service	the Service to remove
	 * @throws PersistenceException	when an error occurs removing the Service
	 */
	void deleteService(UserDetails user, Service service) throws PersistenceException;
	
	/**
	 * Gets all registered Services.
	 * 
	 * @return	collection with the meta-data of all registered Services, empty collection if no Services are 
	 * 			registered
	 * @throws PersistenceException	when an error occurs retrieving the meta-data of the Services
	 */
	Collection<? extends Service> getServices() throws PersistenceException;
	
	/**
	 * Gets a subset of registered Services.
	 * 
	 * @param request	a {@link PageRequest} specifying the requested subset
	 * @return	collection with the meta-data of all registered Services, empty collection if no Services are 
	 * 			registered
	 * @throws PersistenceException	when an error occurs retrieving the meta-data of the Services
	 */
	Page<? extends Service> getServices(PageRequest request) throws PersistenceException;
	
	/**
	 * Gets the registered Service with the given Service identifier.
	 * 
	 * @param svcId the Identifier of the Service to retrieve
	 * @return	the meta-data of the registered Service with the given identifier if it exists, 
	 * 			<code>null</code> otherwise
	 * @throws PersistenceException	when an error occurs retrieving the meta-data of the Service
	 */
	Service getService(Identifier svcId) throws PersistenceException;
}
