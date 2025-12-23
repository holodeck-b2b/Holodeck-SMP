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

import java.time.Instant;

/**
 * Defines the search criteria of the audit log. All criteria are optional. When more than one criterion is specified,
 * only the records that match all specified criteria will be returned. 
 * 
 * @author Sander Fieten (sander at holodeck-b2b.org)
 */
public record AuditLogSearchCriteria(
	/**
	 * The username of the account for which the audit log records should be returned.
	 */
	String	 	username,
	/**
	 * The subject of the audit log records to be returned. 
	 * <p>
	 * NOTE: The {@link AuditLogService#getAvailableSubjects()} method can be used to obtain a list of available subjects.
	 */
	String 		subject,
	/**
	 * The action of the audit log records to be returned. 
	 * <p>
	 * NOTE: The {@link AuditLogService#getAvailableActions()} method can be used to obtain a list of available actions.
	 */
	String 		action,
	/**
	 * The time from which audit log records should be included in the search result. Audit log records with a time 
	 * stamp equal or after this value will be included in the search result. 
	 */
	Instant		from,
	/**
	 * The time until which audit log records should be included in the search result. Audit log records with a time 
	 * stamp less than but not equal to this value will be included in the search result. 
	 */
	Instant		to) {	
	
	/**
	 * Creates search criteria to find all audit log records.
	 */
	public AuditLogSearchCriteria() {
		this(null, null, null, null, null);
	}
}
