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
 * Defines the interface of the component that implements the functionality to manage the data registered in the network's directory.
 * As currently only Peppol has a directory service this interface is based on the specifications of that interface.
 *
 * @author Sander Fieten (sander at holodeck-b2b.org)
 */
public interface IDirectoryIntegrator {

	/**
	 * Publishes the particpant's business card information to the directory. 
	 *
	 * @param p	the meta-data of the Participant whose business card needs to be published
	 * @throws DirectoryException when the Participant's business card could not be published in the directory
	 */
	void publishParticipantInfo(Participant p) throws DirectoryException;

	/**
	 * Remove the Participant's business card from the directory.
	 *
	 * @param p	the meta-data of the Participant to remove from the directory
	 * @throws DirectoryException when the Participant's business card could not be removed from the directory
	 */
	void removeParticipantInfo(Participant p) throws DirectoryException;	
}
