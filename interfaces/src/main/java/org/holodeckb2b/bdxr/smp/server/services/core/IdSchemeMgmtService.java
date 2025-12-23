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

import org.holodeckb2b.bdxr.smp.server.datamodel.IDScheme;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.userdetails.UserDetails;

/**
 * Defines the interface of the central service for the management of <i>Identifier Scheme</i> registrations in the 
 * Holodeck SMP instance.
 * 
 * @author Sander Fieten (sander at holodeck-b2b.org)
 */
public interface IdSchemeMgmtService {

	/**
	 * Adds a new Identifier Scheme to the data set managed by this Holodeck SMP instance and returns the registered  
	 * instance which must be used for further processing. 
	 * 
	 * @param user		the User registering the ID Scheme, required for audit logging
	 * @param scheme 	the meta-data on the new ID Scheme
	 * @return	the registered ID Scheme, this object MUST be used for further processing of the ID Scheme
	 * @throws PersistenceException	when an error occurs saving the ID Scheme's meta-data
	 */
	IDScheme addIDScheme(UserDetails user, IDScheme scheme) throws PersistenceException;
	
	/**
	 * Updates the meta-data of an existing Identifier Scheme.
	 * 
	 * @param user		the User updating the ID Scheme, required for audit logging
	 * @param scheme	the updated meta-data of the ID Scheme, MUST be an instance that was obtained from this service
	 * @return	the updated ID Scheme registration, this object MUST be used for further processing of the ID Scheme
	 * @throws PersistenceException	when an error occurs saving the ID Scheme's meta-data
	 */
	IDScheme updateIDScheme(UserDetails user, IDScheme scheme) throws PersistenceException;
	
	/**
	 * Removes the Identifier Scheme registration.
	 * 
	 * @param user		the User removing the ID Scheme, required for audit logging
	 * @param scheme	the ID Scheme to remove
	 * @throws PersistenceException	when an error occurs removing the ID Scheme
	 */
	void deleteIDScheme(UserDetails user, IDScheme scheme) throws PersistenceException;
	
	/**
	 * Gets all registered Identifier Schemes.
	 * 
	 * @return	collection with the meta-data of all registered ID schemes, empty collection if no ID schemes are 
	 * 			registered
	 * @throws PersistenceException	when an error occurs retrieving the meta-data of the ID schemes
	 */
	Collection<? extends IDScheme> getIDSchemes() throws PersistenceException;
	
	/**
	 * Gets a subset of registered Identifier Schemes.
	 * 
	 * @param request	a {@link PageRequest} specifying the requested subset
	 * @return	collection with the meta-data of all registered ID schemes, empty collection if no ID schemes are 
	 * 			registered
	 * @throws PersistenceException	when an error occurs retrieving the meta-data of the ID schemes
	 */
	Page<? extends IDScheme> getIDSchemes(PageRequest request) throws PersistenceException;
	
	/**
	 * Gets the registered Identifier Scheme with the given scheme identifier.
	 * 
	 * @param schemeId the scheme identifier of the ID scheme to retrieve
	 * @return	the meta-data of the registered ID scheme with the given scheme identifier if it exists, 
	 * 			<code>null</code> otherwise
	 * @throws PersistenceException	when an error occurs retrieving the meta-data of the ID scheme
	 */
	IDScheme getIDScheme(String schemeId) throws PersistenceException;
}
