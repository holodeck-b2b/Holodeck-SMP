/*
 * Copyright (C) 2022 The Holodeck B2B Team
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

import org.holodeckb2b.bdxr.smp.server.datamodel.Service;

import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

/**
 * Is the JPA entity for {@link Service}.
 *
 * @author Sander Fieten (sander at holodeck-b2b.org)
 */
@Entity(name = "Service")
@Table(indexes = {@Index(columnList = "name")})
@Getter
@Setter
public class ServiceE implements Service {

	@Id
	@GeneratedValue
	private long		oid;

	@Column
	@NotBlank(message = "A Service name must be provided")
	private String		name;

	@Embedded
	private IdentifierE	id;

	@Column
	private String		specificationRef;

	@Override
	public boolean equals(Object o) {
		if (o == null || !(o instanceof ServiceE))
			return false;
		else
			return this.oid == ((ServiceE) o).getOid();
	}
}
