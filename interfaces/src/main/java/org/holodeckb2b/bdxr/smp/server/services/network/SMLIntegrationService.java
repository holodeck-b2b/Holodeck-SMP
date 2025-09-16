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
package org.holodeckb2b.bdxr.smp.server.services.network;

import org.holodeckb2b.bdxr.smp.datamodel.Certificate;
import org.holodeckb2b.bdxr.smp.server.datamodel.Participant;
import org.holodeckb2b.bdxr.smp.server.datamodel.SMPServerMetadata;

/**
 * Defines the interface of the Spring Bean that implements the functionality to manage the data registered in the SML. 
 * As there currently does not exist an open standard for the SML interface this interface is based on the functionality 
 * provided by the Peppol SML. 
 * 
 * @author Sander Fieten (sander at holodeck-b2b.org)
 */
public interface SMLIntegrationService {

	/**
	 * Gets the name of the network's SML service the server is connected to. This name is used in the UI and therefore
	 * the name should be descriptive.
	 *    
	 * @return name of the network's SML service the server is connected to
	 */
	String getSMLName();
	
	/**
	 * Indicates whether the SMP must also be registered in the SML. If this is the case the SMP must be registered
	 * before any Participant is. When SMP registration is not required the methods with regards to the SMP 
	 * registration don't need to implemented and may throw an exception when called.   
	 *  
	 * @return	<code>true</code> when SMP registration is required, <code>false</code> if not
	 */
	boolean requiresSMPRegistration();	

	/**
	 * Indicates whether the SMP Certificate needs to be registered in the SML. If true the SMP certificate must be
	 * provided when registering and updating the SMP registration. Note that a network's SML service may require that
	 * an update of the certificate is announced in advance. 
	 *
	 * @return <code>true</code> if the certificate needs to be registered in the SML, <code>false</code> if not
	 * @see #requiresSMPCertRegistration()
	 */
	boolean requiresSMPCertRegistration();
			
	/**
	 * Registers the SMP in the SML.
	 * 
	 * @param smp	the meta-data of the SMP server
	 * @throws SMLException when the SMP Server could not be registered in the SML
	 * @see #requiresSMPCertRegistration()
	 */
	void registerSMPServer(SMPServerMetadata smp) throws SMLException;
	
	/**
	 * Updates the SMP registration in the SML. 
	 * 
	 * @param smp	the updated meta-data of the SMP server. The <code>SMPId</code> must reference the registration
	 * 				to be updated.
	 * @throws SMLException when the registration of the SMP Server in the SML could not be updated 
	 * @see #requiresSMPCertRegistration()
	 */
	void updateSMPServer(SMPServerMetadata smp) throws SMLException;
	
	/**
	 * Registers the SMP server's certificate update in the SML.
	 *
	 * @param smpId	the identifier of the SMP which certificate is updated
	 * @param cert	the meta-data of the certificate update. Only the X509.v3 certificate is required and optionally the
	 * 				date it will be activated. 
	 * @throws SMLException	when the certificate update could not be registered in the SML
	 */
	void updateSMPCertificate(String smpId, Certificate cert) throws SMLException;
	
	/**
	 * Removes the SMP registration from the SML.
	 * 
	 * @param smpId	the identifier of the SMP to be removed from the SML 
	 * @throws SMLException when the registration of the SMP Server in the SML could not be removed
	 * @see #requiresSMPCertRegistration()
	 */
	void deregisterSMPServer(String smpId) throws SMLException;

	/**
	 * Registers a Participant in the SML.
	 *
	 * @param p	the meta-data of the Participant to register
	 * @throws SMLException when the Participant could not be registered in the SML, for example when the Participant
	 * 						is already registered by another SMP server. 
	 * @see #isAvailable()
	 */
	void registerParticipant(Participant p) throws SMLException;

	/**
	 * Removes the registration of a Participant from the SML.
	 *
	 * @param p	the Participant meta-data registration that should be removed from the SML
	 * @throws SMLException when the Participants could not be removed in the SML
	 * @see #isAvailable()
	 */
	void deregisterParticipant(Participant p) throws SMLException;
	
	/**
	 * Checks if the Participant is registered in the SML by this SMP server. Note that a negative answer does not mean
	 * the Participant is not registered in SML, only that it is not registered by this SMP server. Trying to 
	 * subsequently add the Participant to the SML may therefore still result in an error if the Participant is already 
	 * registered by another SMP.
	 * 
	 * @param p	the meta-data of the Participant to check in the SML
	 * @return	<code>true</code> when the Participant is registered by this SMP server in the SML,
	 * 			<code>false</code> otherwise
	 * @throws SMLException when there is an error executing the query to the SML
	 */
	boolean isRegistered(Participant p) throws SMLException;
	
	/**
	 * Prepares the migration of the Participant by registering the migration code in the SML.
	 * 
	 * @param p		the meta-data on the Participant
	 * @param code	the migration code
	 * @throws SMLException when the provided migration code is invalid or when there is an error executing the update 
	 * 						to the SML
	 * @see #isAvailable()
	 */
	void registerMigrationCode(Participant p, String code) throws SMLException;
	
	/**
	 * Migrates the Participant to this SMP using the provided migration code.
	 * 
	 * @param p		the meta-data on the Participant being migrated
	 * @param code	the migration code
	 * @throws SMLException when there is an error executing the update to the SML
	 * @see #isAvailable()
	 */
	void migrateParticipant(Participant p, String code) throws SMLException; 
}
