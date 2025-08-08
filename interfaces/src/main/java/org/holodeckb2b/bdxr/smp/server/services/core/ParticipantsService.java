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

import org.holodeckb2b.bdxr.smp.datamodel.Identifier;
import org.holodeckb2b.bdxr.smp.server.datamodel.Participant;
import org.holodeckb2b.bdxr.smp.server.datamodel.ServiceMetadataTemplate;
import org.holodeckb2b.bdxr.smp.server.services.network.DirectoryException;
import org.holodeckb2b.bdxr.smp.server.services.network.DirectoryIntegrationService;
import org.holodeckb2b.bdxr.smp.server.services.network.SMLException;
import org.holodeckb2b.bdxr.smp.server.services.network.SMLIntegrationService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.userdetails.UserDetails;

/**
 * Defines the interface of the central service for the management of <i>Participant</i> registrations in the 
 * Holodeck SMP instance.
 * 
 * @author Sander Fieten (sander at holodeck-b2b.org)
 */
public interface ParticipantsService {
	
	/**
	 * Adds a new Participant to the data set managed by this Holodeck SMP instance and returns the registered instance 
	 * which must be used for further processing. The provided Participant meta-data SHOULD NOT include any bindings to
	 * Service Meta-data Templates. If provided they will be ignored. 
	 * 
	 * @param user	the User registering the Participant, required for audit logging
	 * @param p	the meta-data of the new Participant
	 * @return	object representing the persisted meta-data, this object MUST be used for further processing of the 
	 * 			Participant 
	 * @throws PersistenceException	when an error occurs saving the Participant's meta-data
	 */
	Participant addParticipant(UserDetails user, Participant p) throws PersistenceException;

	/**
	 * Updates the meta-data of an existing Participant. If the updated Participant is registered in the SML, the 
	 * identifier of the Participant CAN NOT be changed as this would break the SML registration. When the Participant 
	 * is published in the network's directory the directory it will be notified of the change.  
	 * <p>
	 * NOTE: This method CAN NOT be used to update the bindings to Service Meta-data Templates! Use the {@link 
	 * #bindToSMT()} and {@link #removeSMTBinding()} methods instead.  
	 * 
	 * @param user	the User updating the Participant, required for audit logging
	 * @param p	the updated meta-data of the Participant, MUST be an instance that was obtained from this service
	 * @return	object representing the persisted meta-data, this object MUST be used for further processing of the 
	 * 			Participant 
	 * @throws PersistenceException	when an error occurs saving the Participant's meta-data, for example because no 
	 * 								Participant registration exists with the specified internal identifier
	 */
	Participant updateParticipant(UserDetails user, Participant p) throws PersistenceException;
	
	/**
	 * Removes the Participant registration. This includes removal from the network's SML and Directory if the 
	 * Participant was registered/published there.
	 * 
	 * @param user			the User removing the Participant, required for audit logging
	 * @param participant	the Participant to delete, SHALL NOT be <code>null</code>
	 * @throws PersistenceException	when an error occurs deleting the Participant's meta-data
	 */
	void deleteParticipant(UserDetails user, Participant p) throws PersistenceException;

	/**
	 * Gets a subset of registered Participants.
	 * 
	 * @param request	a {@link PageRequest} specifying the requested subset
	 * @return collection of all registered Participants
	 * @throws PersistenceException	when an error occurs retrieving the meta-data of the participants
	 */
	Page<? extends Participant> getParticipants(PageRequest request) throws PersistenceException;
	
	/**
	 * Gets the meta-data of the Participant with the specified Participant Identifier.
	 * 
	 * @param pid	the Participant Identifier of the registration to be retrieved
	 * @return 	the meta-data of the Participant with the specified identifier if it exists or <code>null</code> if no
	 * 			such registration exists
	 * @throws PersistenceException	when an error occurs retrieving the meta-data of the Participant
	 */
	Participant getParticipant(Identifier pid) throws PersistenceException;

	/**
	 * Finds the Participant registrations that use the given additional Identifier. 
	 * 
	 * @param id	the additional Identifier to search on
	 * @return	the Participant registrations that use the given additional Identifier
	 * @throws PersistenceException when an error occurs retrieving the meta-data of the Participants
	 */
	Collection<? extends Participant> findParticipantsByAdditionalId(Identifier id) throws PersistenceException;	
	
	/**
	 * Finds the Participant registrations which business entity name starts with the given name, ignoring case. 
	 * 
	 * @param startsWith	string the business entity name of the Participant to find should start with
	 * @return	the Participant registrations which have the specified business entity name
	 * @throws PersistenceException when an error occurs retrieving the meta-data of the Participants
	 */
	Collection<? extends Participant> findParticipantsByName(String startsWith) throws PersistenceException;
	
	/**
	 * Finds <i>Participants</i> in the given SML registration state. Because the result set can be quit large, this
	 * method uses pagination to prevent server overloading. Therefore the caller should specify which subset of the
	 * complete result set is to be retrieved. 
	 * 
	 * @param registered	<code>true</code> when Participant registered in the SML should be retrieved,<br/>
	 * 						<code>false</code> when unregistered Participants should be retrieved
	 * @param request		a {@link PageRequest} specifying the requested subset 
	 * @return the requested sub set of Participants in the given SML registration state
	 */	
	Page<? extends Participant> findParticipantsBySMLRegistration(boolean registered, Pageable request)
																							throws PersistenceException;
	
	/**
	 * Finds <i>Participants</i> which are/are not published to the directory. Because the result set can be quit large, 
	 * this method uses pagination to prevent server overloading. Therefore the caller should specify which subset of 
	 * the complete result set is to be retrieved. 
	 * 
	 * @param published		<code>true</code> when Participant published to the directory should be retrieved,<br/>
	 * 						<code>false</code> when Participants not published to the directory should be retrieved
	 * @param request		a {@link PageRequest} specifying the requested subset 
	 * @return the requested sub set of Participants in the given publication state
	 */	
	Page<? extends Participant> findParticipantsByDirectoryPublication(boolean published, Pageable request) 
																							throws PersistenceException;
	
	/**
	 * Finds <i>Participants</i> which are/are not register in the SML service and are/are not published to the 
	 * directory. Because the result set can be quit large, this method uses pagination to prevent server overloading. 
	 * Therefore the caller should specify which subset of the complete result set is to be retrieved. 
	 * 
	 * @param registered	<code>true</code> when Participant registered in the SML should be retrieved,<br/>
	 * 						<code>false</code> when unregistered Participants should be retrieved
	 * @param published		<code>true</code> when Participant published to the directory should be retrieved,<br/>
	 * 						<code>false</code> when Participants not published to the directory should be retrieved
	 * @param request		a {@link PageRequest} specifying the requested subset 
	 * @return the requested sub set of Participants in the given publication state
	 */		
	Page<? extends Participant> findParticipantsBySMLRegistrationAndDirectoryPublication(boolean registered,
													boolean published, Pageable request) throws PersistenceException;

	/**
	 * Finds <i>Participants</i> supporting the given <i>Service Metadata Template</i>. Because the result set can be 
	 * quit large, this method uses pagination to prevent server overloading. Therefore the caller should specify which 
	 * subset of the complete result set is to be retrieved. 
	 * 
	 * @param smt	the <i>Service Metadata Template</i> to select the <i>Participants</i>
	 * @param request		a {@link PageRequest} specifying the requested subset 
	 * @return the requested sub set of Participants supporting the given <i>Service Metadata Template</i>
	 */	
	Page<? extends Participant> findParticipantsSupporting(ServiceMetadataTemplate smt, Pageable request) 
																							throws PersistenceException;
		
	/**
	 * Binds the given <i>Service Metadata Template</i> to the given <i>Participant</i>.
	 *  
	 * @param user		User adding the SMT to the Participant, required for audit logging
	 * @param p			Participant to bind the SMT to
	 * @param smt		SMT to bind to the Participant
	 * @return	the updated Participant registration
	 * @throws PersistenceException	when an error occurs binding the SMT to the Participant
	 */
	Participant bindToSMT(UserDetails user, Participant p, ServiceMetadataTemplate smt) throws PersistenceException;
	
	/**
	 * Removes the given <i>Service Metadata Template</i> from the given <i>Participant</i> registration.
	 *  
	 * @param user		User removing the SMT from the Participant, required for audit logging
	 * @param p			Participant from which the SMT should be removed
	 * @param smt		the SMT to remove from the Participant
	 * @return	the updated Participant registration
	 * @throws PersistenceException	when an error occurs removing the SMT from the Participant
	 */
	Participant removeSMTBinding(UserDetails user, Participant p, ServiceMetadataTemplate smt) throws PersistenceException;
	
	/**
	 * Indicates whether an SML service is available in the network and the SMP can register Participants in it. This
	 * requires that a {@link SMLIntegrationService} implementation is available and ready for registration 
	 * Participants. 
	 * 
	 * @return	<code>true</code> when the SMP can register Participants in the SML, <code>false</code> otherwise
	 */
	boolean isSMLRegistrationAvailable();
	
	/**
	 * Registers the Participant in the network's SML. This method should only be called when the integration with SML
	 * is available and ready for registration of Participants, i.e. {@link #isSMLRegistrationAvailable()} returns
	 * <code>true</code>.
	 * 
	 * @param user	the User registering the Participant in the SML, required for audit logging
	 * @param p		the Participant to register in the SML
	 * @return		the updated Participant registration
	 * @throws SMLException	when there is an error registering the Participant in the SML
	 * @throws PersistenceException when the given Participant instance is not managed 
	 */
	Participant registerInSML(UserDetails user, Participant p) throws SMLException, PersistenceException;
	
	/**
	 * Migrates the existing Participant registration in the network's SML from another SMP to the current SMP. This 
	 * method should only be called when the integration with SML is available and ready for registration of 
	 * Participants, i.e. {@link #isSMLRegistrationAvailable()} returns <code>true</code>.
	 * 
	 * @param user			the User performing migration of the Participant in the SML, required for audit logging
	 * @param p				the Participant to register in the SML
	 * @param migrationCode	the migration code to use
	 * @return		the updated Participant registration
	 * @throws SMLException	when there is an error registering the Participant in the SML
	 * @throws PersistenceException when the given Participant instance is not managed
	 */
	Participant migrateInSML(UserDetails user, Participant p, String migrationCode) throws SMLException,
																						   PersistenceException;
	
	/**
	 * Prepares the migration of the Participant to another SMP by, if not provided, generating and registering a 
	 * migration code in the network's SML service. This method should only be called when the Participant is already 
	 * registered in the SML.
	 * 
	 * @param user	the User preparing the migration of the Participant in the SML, required for audit logging
	 * @param p		the Participant to be migrated
	 * @param migrationCode	optional, the migration code to use. Note that the network's SML service may define 
	 * 						constraints on the migration code.
	 * @return	the updated Participant registration
	 * @throws SMLException when there is an error preparing the migration of the Participant
	 * @throws PersistenceException when the given Participant instance is not managed 
	 */
	Participant prepareForSMLMigration(UserDetails user, Participant p, String... migrationCode) throws SMLException, 
																								PersistenceException;
	
	/**
	 * Cancels the migration of the Participant to another SMP by removing the migration code from the network's SML 
	 * service.
	 * 
	 * @param user	the User canceling the migration of the Participant in the SML, required for audit logging
	 * @param p		the Participant registration which migration should be cancelled
	 * @return		the updated Participant registration
	 * @throws SMLException	when there is an error cancelling the migration of the Participant, for example because the
	 * 						Participant has already been migrated to the other SMP
	 * @throws PersistenceException when the given Participant instance is not managed
	 */
	Participant cancelSMLMigration(UserDetails user, Participant p) throws SMLException, PersistenceException;
	
	/**
	 * Removes the Participant from the network's SML. This method SHOULD NOT be called if the Participant has been 
	 * prepared for migration to another SMP.
	 * 
	 * @param user	the User removing the Participant from the SML, required for audit logging
	 * @param p		the Participant to remove from the SML
	 * @return		the updated Participant registration
	 * @throws SMLException	when there is an error removing the Participant from the SML. For example, if the 
	 * 						Participant is published in the directory and directory publication requires SML 
	 * 						registration
	 * @throws PersistenceException when the given Participant instance is not managed
	 */
	Participant removeFromSML(UserDetails user, Participant p) throws SMLException, PersistenceException;	
		
	/**
	 * Indicates whether a directory service is available in the network and the SMP can publish Participants in it. 
	 * This requires that a {@link DirectoryIntegrationService} implementation is available and ready for publishing of
	 * Participants.
	 * 
	 * @return <code>true</code> when the SMP can publish Participants in the directory, <code>false</code> otherwise
	 */
	boolean isDirectoryPublicationAvailable();
	
	/**
	 * Publishes the Participant's business card information to the directory. This method should only be called when 
	 * the integration with the directory is available and ready for publication of Participants, i.e. {@link 
	 * #isDirectoryPublicationAvailable()} returns <code>true</code>.
	 * 
	 * @param user	the User publishing the Participant in the directory, required for audit logging
	 * @param p		the meta-data of the Participant whose business card needs to be published
	 * @return		the updated Participant registration
	 * @throws DirectoryException	when the Participant's business card could not be published in the directory
	 * @throws PersistenceException when the given Participant instance is not managed
	 */
	Participant publishInDirectory(UserDetails user, Participant p) throws DirectoryException, PersistenceException;	

	/**
	 * Removes the Participant's business card from the directory.
	 * 
	 * @param user	the User removing the Participant from the directory, required for audit logging
	 * @param p	the Participant to remove from the directory
	 * @return	the updated Participant registration	
	 * @throws DirectoryException when an error occurs removing the Participant's business card from the directory
	 * @throws PersistenceException when the given Participant instance is not managed
	 */
	Participant removeFromDirectory(UserDetails user, Participant p) throws DirectoryException, PersistenceException;
}
