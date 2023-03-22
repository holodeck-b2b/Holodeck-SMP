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
	 * @throws Exception when the Participant could not be registered in the SML
	 */
	void registerParticipant(Participant p) throws Exception;

	/**
	 * Remove the registration of a Participant from the SML.
	 *
	 * @param p	the meta-data of the Participant to remove from the SML
	 * @throws Exception when the Participant could not be removed in the SML
	 */
	void unregisterParticipant(Participant p) throws Exception;

	/**
	 * Registers the new SMP certificate in the SML.
	 *
	 * @param newCert	the new certificate used by the SMP
	 * @param newCert	the new certificate used by the SMP
	 * @throws Exception when the new certificate could not be registered in the SML
	 */
//	void registerSMPCertChange(X509Certificate oldCert, X509Certificate newCert) throws Exception;
}
