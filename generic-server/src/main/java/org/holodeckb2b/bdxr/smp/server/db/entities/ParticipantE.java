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

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.holodeckb2b.bdxr.smp.server.datamodel.Participant;
import org.springframework.format.annotation.DateTimeFormat;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.validation.Valid;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Is the JPA entity for {@link Participant}.
 *
 * @author Sander Fieten (sander at holodeck-b2b.org)
 */
@Entity(name = "Participant")
@Table(indexes = {@Index(columnList = "name")})
@Getter
@Setter
@NoArgsConstructor
public class ParticipantE implements Participant {

	@Id
	@GeneratedValue
	protected long		oid;

	@Embedded
	@Valid
	protected IdentifierE	id;

	@Column
	protected String	name;
	
	@Column(length=2)
	protected String	country;

	@Column
	protected String	contactInfo;

	@Column(length=1024)
	protected String	addressInfo;
	
	@Column
	@DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
	protected LocalDate firstRegistration;
	
	@Column(length=1024)
	protected String	additionalIds;

	@Column
	protected Boolean	isRegisteredSML;

	@Column
	protected Boolean	publishedInDirectory;
	
	@Column
	protected String	migrationCode;

	@OneToMany(mappedBy = "participant", cascade = CascadeType.ALL, orphanRemoval = true)
	protected List<ServiceMetadataBindingE>	bindings = new ArrayList<>();

	@Override
	public boolean registeredInSML() {
		return isRegisteredSML != null ? isRegisteredSML : false;
	}

	@Override
	public boolean publishedInDirectory() {
		return publishedInDirectory != null ? publishedInDirectory : false;
	}

	@Override
	public boolean equals(Object o) {
		if (o == null || !(o instanceof ParticipantE))
			return false;
		else
			return this.oid == ((ParticipantE) o).getOid();
	}
}
