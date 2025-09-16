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
import org.holodeckb2b.bdxr.smp.server.datamodel.TransportProfile;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.userdetails.UserDetails;

/**
 * Defines the interface of the central service for the management of <i>Transport Profile</i> registrations in the 
 * Holodeck SMP instance.
 * 
 * @author Sander Fieten (sander at holodeck-b2b.org)
 */
public interface TransportProfileMgmtService {

	/**
	 * Adds a new Transport Profile to the data set managed by this Holodeck SMP instance and returns the registered 
	 * instance which must be used for further processing. If the Transport Profile Identifier is assigned an ID Scheme 
	 * the scheme MUST already be registered in the SMP instance.
	 * 
	 * @param user		the User registering the Transport Profile, required for audit logging
	 * @param profile 	the meta-data on the new Transport Profile
	 * @return	the registered Transport Profile, this object MUST be used for further processing
	 * @throws PersistenceException	when an error occurs saving the Transport Profile meta-data
	 */
	TransportProfile addTransportProfile(UserDetails user, TransportProfile profile) throws PersistenceException;
	
	/**
	 * Updates the meta-data of an existing Transport Profile.
	 * 
	 * @param user		the User updating the Transport Profile, required for audit logging
	 * @param profile	the updated meta-data of the Transport Profile, MUST be an instance that was obtained from this 
	 * 					service
	 * @return	the updated Transport Profile, this object MUST be used for further processing of the Transport Profile
	 * @throws PersistenceException	when an error occurs saving the Transport Profile meta-data
	 */
	TransportProfile updateTransportProfile(UserDetails user, TransportProfile profile) throws PersistenceException;
	
	/**
	 * Removes the Transport Profile registration.
	 * 
	 * @param user		the User removing the Transport Profile, required for audit logging
	 * @param profile	the Transport Profile to remove
	 * @throws PersistenceException	when an error occurs removing the Transport Profile
	 */
	void deleteTransportProfile(UserDetails user, TransportProfile profile) throws PersistenceException;
	
	/**
	 * Gets all registered Transport Profiles.
	 * 
	 * @return	collection with the meta-data of all registered Transport Profiles, empty collection if no Profiles are 
	 * 			registered
	 * @throws PersistenceException	when an error occurs retrieving the meta-data of the Transport Profiles
	 */
	Collection<? extends TransportProfile> getTransportProfiles() throws PersistenceException;
	
	/**
	 * Gets a subset of registered Transport Profiles.
	 * 
	 * @param request	a {@link PageRequest} specifying the requested subset
	 * @return	collection with the meta-data of all registered Profiles in the requested subset, empty collection if 
	 * 			the specified set does not contain any Transport Profiles
	 * @throws PersistenceException	when an error occurs retrieving the meta-data of the Transport Profiles
	 */
	Page<? extends TransportProfile> getTransportProfiles(PageRequest request) throws PersistenceException;
	
	/**
	 * Gets the registered Transport Profile with the given Transport Profile identifier.
	 * 
	 * @param profileId the Identifier of the Transport Profile to retrieve
	 * @return	the meta-data of the registered ID scheme with the given identifier if it exists, 
	 * 			<code>null</code> otherwise
	 * @throws PersistenceException	when an error occurs retrieving the meta-data of the Transport Profile
	 */
	TransportProfile getTransportProfile(Identifier profileId) throws PersistenceException;
}
