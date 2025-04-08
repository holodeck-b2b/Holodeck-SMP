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
package org.holodeckb2b.bdxr.smp.server.svc;

import org.holodeckb2b.bdxr.smp.server.datamodel.Participant;

/**
 * Defines the interface of the component that implements the functionality to manage the data registered in the SML.
 * As currently only Peppol and the EC offer SML interfaces this interface is based on those.
 *
 * @author Sander Fieten (sander at holodeck-b2b.org)
 */
public interface ISMLIntegrator {

	/**
	 * Checks if the SMP is registered in the SML
	 *
	 * @return <code>true</code> if the SMP is registered in the SML, <code>false</code> otherwise
	 */
	boolean isSMPRegistered();

	/**
	 * Registers a Participant in the SML.
	 *
	 * @param p	the meta-data of the Participant to register
	 * @throws SMLException when the Participant could not be registered in the SML
	 */
	void registerParticipant(Participant p) throws SMLException;

	/**
	 * Remove the registration of a Participant from the SML.
	 *
	 * @param p	the meta-data of the Participant to remove from the SML
	 * @throws SMLException when the Participant could not be removed in the SML
	 */
	void unregisterParticipant(Participant p) throws SMLException;
	
	/**
	 * Prepares the migration of the Participant by registering the migration code in the SML.
	 * 
	 * @param p		the meta-data on the Participant
	 * @param code	the migration code
	 * @throws SMLException when there is an error executing the update to the SML
	 */
	void registerMigrationCode(Participant p, String code) throws SMLException;
	
	/**
	 * Migrates the Participant to this SMP using the provided migration code.
	 * 
	 * @param p		the meta-data on the Participant being migrated
	 * @param code	the migration code
	 * @throws SMLException when there is an error executing the update to the SML
	 */
	void migrateParticipant(Participant p, String code) throws SMLException; 

	/**
	 * Indicates whether the SMP Certificate needs to be registered in the SML
	 *
	 * @return <code>true</code> if the certificate needs to be registered in the SML, <code>false</code> if not
	 */
	boolean requiresSMPCertRegistration();
}
