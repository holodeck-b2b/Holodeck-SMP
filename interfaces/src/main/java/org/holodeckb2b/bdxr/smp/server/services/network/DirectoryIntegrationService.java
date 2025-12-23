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

import org.holodeckb2b.bdxr.smp.server.datamodel.Participant;

/**
 * Defines the interface of the Spring Bean that implements the functionality to manage the data registered in the 
 * network's directory. Since there currently does not exist an open standard for directory service's (specifically for
 * use in four corner networks) this interface provides just two functions to publish or remove a Participant to/from
 * the directory.  
 * 
 * @author Sander Fieten (sander at holodeck-b2b.org)
 */
public interface DirectoryIntegrationService {
	
	/**
	 * Gets the name of the network's Directory service the server is connected to. This name is used in the UI and 
	 * therefore the name should be descriptive.
	 *    
	 * @return name of the network's Directory service the server is connected to
	 */
	String getDirectoryName();
	
	/**
	 * Indicates whether a Participant must be registered in the network's SML service before it can be published to
	 * the directory.
	 * 
	 * @return	<code>true</code> if the Participant must be registered in the SML before publication,
	 * 			<code>false</code> otherwise
	 */
	boolean isSMLRegistrationRequired();
	
	/**
	 * Publishes the particpant's business card information to the directory. 
	 *
	 * @param p	the meta-data of the Participant whose business card needs to be published
	 * @throws DirectoryException when the Participant's business card could not be published in the directory
	 */
	void publishParticipantInfo(Participant p) throws DirectoryException;

	/**
	 * Remove the business card of a Participant from the directory.
	 *
	 * @param p	the Participant meta-data registration for which the business cards should be removed from the
	 * 			directory 
	 * @throws DirectoryException when the Participants' business cards could not be removed from the directory
	 */
	void removeParticipantInfo(Participant p) throws DirectoryException;	
}
