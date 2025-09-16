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

import org.holodeckb2b.bdxr.common.datamodel.ProcessIdentifier;
import org.holodeckb2b.bdxr.smp.server.datamodel.Process;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.userdetails.UserDetails;

/**
 * Defines the interface of the central service for the management of <i>Process</i> registrations in the Holodeck SMP 
 * instance.
 * 
 * @author Sander Fieten (sander at holodeck-b2b.org)
 */
public interface ProcessMgmtService {

	/**
	 * Adds a new Process to the data set managed by this Holodeck SMP instance and returns the registered instance 
	 * which must be used for further processing. If the Process Identifier is assigned an ID Scheme the scheme MUST 
	 * already be registered in the SMP instance.
	 * 
	 * @param user		the User registering the Process, required for audit logging
	 * @param process 	the meta-data on the new Process
	 * @return	the registered Process, this object MUST be used for further processing of the Process
	 * @throws PersistenceException	when an error occurs saving the Process' meta-data
	 */
	Process addProccess(UserDetails user, Process process) throws PersistenceException;
	
	/**
	 * Updates the meta-data of an existing Process.
	 * 
	 * @param user	the User updating the Process, required for audit logging
	 * @param process	the updated meta-data of the Process, MUST be an instance that was obtained from this service
	 * @return	the registered Process, this object MUST be used for further processing of the Process
	 * @throws PersistenceException	when an error occurs saving the Process' meta-data
	 */
	Process updateProcess(UserDetails user, Process process) throws PersistenceException;
	
	/**
	 * Removes the Process registration.
	 * 
	 * @param user		the User removing the Process, required for audit logging
	 * @param process	the Process to remove
	 * @throws PersistenceException	when an error occurs removing the Process
	 */
	void deleteProcess(UserDetails user, Process process) throws PersistenceException;
	
	/**
	 * Gets all registered Processes.
	 * 
	 * @return	collection with the meta-data of all registered Processes, empty collection if no Processes are 
	 * 			registered
	 * @throws PersistenceException	when an error occurs retrieving the meta-data of the Processes
	 */
	Collection<? extends Process> getProcesses() throws PersistenceException;
	
	/**
	 * Gets a subset of registered Processes.
	 * 
	 * @param request	a {@link PageRequest} specifying the requested subset
	 * @return	collection with the meta-data of all registered Processes in the requested subset, empty collection if 
	 * 			the specified set does not contain any Processes
	 * @throws PersistenceException	when an error occurs retrieving the meta-data of the Processes
	 */
	Page<? extends Process> getProcesses(PageRequest request) throws PersistenceException;
	
	/**
	 * Gets the registered Process with the given process identifier.
	 * 
	 * @param processId the Process Identifier of the Process to retrieve
	 * @return	the meta-data of the registered Process with the given Process Identifier if it exists, 
	 * 			<code>null</code> otherwise
	 * @throws PersistenceException	when an error occurs retrieving the meta-data of the Process
	 */
	Process getProcess(ProcessIdentifier processId) throws PersistenceException;
}
