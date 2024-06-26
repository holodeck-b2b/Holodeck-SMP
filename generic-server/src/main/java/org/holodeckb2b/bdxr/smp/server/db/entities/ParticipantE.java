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

import java.util.ArrayList;
import java.util.List;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.validation.Valid;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.holodeckb2b.bdxr.smp.server.datamodel.Participant;

/**
 * Is the JPA entity for {@link Participant}.
 *
 * @author Sander Fieten (sander at holodeck-b2b.org)
 */
@Entity(name = "Participant")
@Table(indexes = {@Index(columnList = "name")})
@Getter
@NoArgsConstructor
public class ParticipantE implements Participant {

	@Id
	@GeneratedValue
	protected long		oid;

	@Embedded
	@Setter
	@Valid
	protected IdentifierE	id;

	@Column
	@Setter
	protected String	name;
	
	@Column(length=2)
	@Setter
	protected String	country;

	@Column
	@Setter
	protected String	contactInfo;

	@Column(length=1024)
	@Setter
	protected String	addressInfo;

	@Column
	@Setter
	protected Boolean	isRegisteredSML;

	@Column
	@Setter
	protected Boolean	publishedInDirectory;

	@OneToMany(mappedBy = "participant", cascade = CascadeType.ALL, orphanRemoval = true)
	@Setter
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
