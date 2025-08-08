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

import java.time.LocalDate;

import org.hibernate.annotations.UpdateTimestamp;
import org.holodeckb2b.bdxr.smp.server.datamodel.MetadataRegistration;

import jakarta.persistence.Column;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Is the abstract base class for all entities that represent a meta-data registration from the SMP server's data model.
 * This base class implements the <i>name</i> field part of all meta-data registrations and includes a generated primary 
 * key (i.e. JPA <code>@Id</code>) and automatically set <code>lastModified</code>. It also implements basic 
 * <code>equals</code> and <code>hashCode</code> methods that only use the primary key to calculate the hash or compare 
 * two entity objects for equality.
 * 
 * @param <I> the class used as identifier of the meta-data registrations
 * @author Sander Fieten (sander at holodeck-b2b.org)
 * @since 3.0.0
 */
@MappedSuperclass
@Getter
@NoArgsConstructor
public abstract class BaseMetadataRegistrationEntity<I> implements MetadataRegistration<I> {

	@Id
	@GeneratedValue
	@Setter
	protected Long		oid;
	
	@UpdateTimestamp
	protected LocalDate lastModified;
	
	@Setter
	@Column
	protected String	name;		
				
	/**
	 * Gets the string that should be used in the audit log to identify the meta-data registration.
	 * <p>
	 * By default the <i>name</i> is used as this is assigned by the User to describe the meta-data registration. 
	 * However this can be overridden by subclasses, as for example Participants will be better identified by their 
	 * actual identifier.
	 * 
	 * @return	identifcation of the meta-data registration to be used in the audit log
	 */
	public String getAuditLogId() {
		return name;
	}
	
	@Override
	public int hashCode() {
		return this.oid != null ? this.oid.hashCode() : super.hashCode();
	}
	
	@SuppressWarnings("rawtypes")
	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		else if (o == null || !this.getClass().isAssignableFrom(o.getClass()))
			return false;
		else 
			return this.oid != null && this.oid.equals(((BaseMetadataRegistrationEntity) o).oid);
	}
}
