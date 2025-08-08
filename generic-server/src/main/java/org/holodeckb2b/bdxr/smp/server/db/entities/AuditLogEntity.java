/*
 * Copyright (C) 2025 The Holodeck B2B Team
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
package org.holodeckb2b.bdxr.smp.server.db.entities;

import java.time.Instant;

import org.holodeckb2b.bdxr.smp.server.auditlogging.AuditLogRecord;

import jakarta.persistence.Basic;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import lombok.NoArgsConstructor;

/**
 * Is the JPA entity for storing the audit log records in the database. As audit log records should be read-only, all
 * fields of the entity are marked as not updatable.
 * 
 * @author Sander Fieten (sander at holodeck-b2b.org)
 * @since 3.0.0
 */
@Entity(name = "AuditLog")
@NoArgsConstructor
public class AuditLogEntity {

	@Id
	@GeneratedValue
	private Long oid;	
	
	@Column(nullable = false, updatable = false)
	private Instant	timestamp;
	
	@Column(nullable = false, updatable = false)
	private String	username;
	
	@Column(nullable = false, updatable = false)
	private String	action;
	
	@Column(updatable = false)
	private String	subject;
	
	@Column(updatable = false)
	@Lob 
	@Basic(fetch = FetchType.LAZY)
	private String	details;
	
	/**
	 * Creates a new audit log record entity using the given data.
	 * 
	 * @param r	the data for the new audit log record, represented a an {@link AuditLogRecord} instance 
	 */
	public AuditLogEntity(AuditLogRecord r) {
		this.timestamp = r.timestamp();
		this.username = r.username();
		this.action = r.action();
		this.subject = r.subject();
		this.details = r.details();
	}
	
	/**
	 * @return	the {@link AuditLogRecord} representation of the audit log record 
	 */
	public AuditLogRecord toAuditLogRecord() {
		return new AuditLogRecord(timestamp, username, action, subject, details);
	}
}
