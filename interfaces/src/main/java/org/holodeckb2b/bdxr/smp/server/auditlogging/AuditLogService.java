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
package org.holodeckb2b.bdxr.smp.server.auditlogging;

import java.util.Set;

import org.holodeckb2b.bdxr.smp.server.services.core.PersistenceException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

/**
 * Defines the interface of the central audit log service.
 *   
 * @author Sander Fieten (sander at holodeck-b2b.org)
 */
public interface AuditLogService {

	/**
	 * Adds the given record to the audit log.
	 *  
	 * @param record	the record to add to the audit log
	 */
	void log(final AuditLogRecord record);

	/**
	 * Gets all usernames that are currently registered in the audit log.
	 * 
	 * @return	the set of all usernames that are currently registered
	 * @throws PersistenceException	when an error occurs retrieving the usernames
	 */
	Set<String> getAvailableUsernames() throws PersistenceException;

	/**
	 * Gets all actions that are currently registered in the audit log. As the actions are currently free text, this 
	 * method is intended to assist in searching the audit log to first get an overview of all actions.
	 * 
	 * @return	the set of all actions that are currently registered
	 * @throws PersistenceException	when an error occurs retrieving the actions
	 */
	Set<String> getAvailableActions() throws PersistenceException;
	
	/**
	 * Gets all subjects that are currently registered in the audit log. As the subjects are currently free text, this 
	 * method is intended to assist in searching the audit log to first get an overview of all subjects.
	 * 
	 * @return	the set of all subjects that are currently registered
	 * @throws PersistenceException	when an error occurs retrieving the subjects
	 */
	Set<String> getAvailableSubjects() throws PersistenceException;

	/**
	 * Gets the specified subset of the audit log records that match the given search criteria.
	 * 
	 * @param searchCriteria	the search criteria
	 * @param request			a {@link PageRequest} specifying the requested subset
	 * @return	the audit log records that match the search criteria
	 * @throws PersistenceException	when an error occurs retrieving the audit log records
	 */
	Page<AuditLogRecord> getAuditLogRecords(AuditLogSearchCriteria searchCriteria, PageRequest request) 
																							throws PersistenceException;
}
