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

import org.holodeckb2b.bdxr.smp.datamodel.Identifier;
import org.holodeckb2b.bdxr.smp.server.datamodel.ServiceMetadataBinding;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Is the JPA entity for {@link ServiceMetadataBinding}.
 *
 * @author Sander Fieten (sander at holodeck-b2b.org)
 */
@Entity(name = "ServiceMetadataBinding")
@Getter
@NoArgsConstructor
public class ServiceMetadataBindingE implements ServiceMetadataBinding {

	@Id
	@GeneratedValue
	protected long oid;

	@ManyToOne
	protected ParticipantE	participant;

	@ManyToOne
	protected ServiceMetadataTemplateE	template;

	public ServiceMetadataBindingE(ParticipantE p, ServiceMetadataTemplateE svc) {
		participant = p;
		template = svc;
	}

	@Override
	public Identifier getParticipantId() {
		return participant.getId();
	}

	@Override
	public boolean equals(Object o) {
		if (o == null || !(o instanceof ServiceMetadataBindingE))
			return false;
		else
			return this.oid == ((ServiceMetadataBindingE) o).getOid();
	}
}
