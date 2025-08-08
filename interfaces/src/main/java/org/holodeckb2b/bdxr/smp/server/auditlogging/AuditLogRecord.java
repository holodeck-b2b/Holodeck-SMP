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

import org.holodeckb2b.commons.util.Utils;

/**
 * Defines the content of an audit log record. Audit log records always relate to an action that is executed by, or on 
 * behalf of (if executed via an API), a user at a certain point in time. Therefore {@link #timestamp}, {@link #username} 
 * and {@link #action} are mandatory fields. But since actions can apply to the user itself, for example when logging
 * in or out of the application, the {@link #subject} field is optional. Also, the {@link #details} field is optional. 
 * 
 * @author Sander Fieten (sander at holodeck-b2b.org)
 */
public record AuditLogRecord(
	/**
	 * The timestamp when the action was executed 
	 */
	Instant timestamp, 
	/**
	 * The username of the User who executed the action
	 */
	String  username, 
	/**
	 * The action that was executed. Currently this is a free text field and the component that executes the action 
	 * should set a short description of the action. 
	 */
	String 	action, 
	/**
	 * Identifies the subject of the action. If the value of this field is <code>null</code> it implies that the subject 
	 * of the action was the user who executed the action.  
	 */
	String 	subject,
	/**
	 * Further details about the action. May be <code>null</code> if no details are available.
	 */
	String 	details) {

	/**
	 * Creates a new audit log record. Checks that at least the username and action are specified, sets the timestamp if
	 * not provided and trims all strings.
	 * 
	 * @param timestamp	timestamp when the action was executed, if <code>null</code> the current timestamp is used
	 * @param username	username of the user who executed the action
	 * @param action    the action that was executed
	 * @param subject	the subject of the action, if <code>null</code> the user who executed the action is the subject
	 * @param details   details about the action, may be <code>null</code>
	 */
	public AuditLogRecord {
		Utils.requireNotNullOrEmpty(username);
		Utils.requireNotNullOrEmpty(action);
		if (timestamp == null) 
			timestamp = Instant.now();
		
		username = username.trim();
		action = action.trim();
		subject = subject == null ? null : subject.trim();
		details = details == null ? null : details.trim();
	}
}
