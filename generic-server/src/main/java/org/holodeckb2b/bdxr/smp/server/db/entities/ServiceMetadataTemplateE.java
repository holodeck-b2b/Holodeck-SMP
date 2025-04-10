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

import org.holodeckb2b.bdxr.smp.datamodel.Extension;
import org.holodeckb2b.bdxr.smp.datamodel.Identifier;
import org.holodeckb2b.bdxr.smp.server.datamodel.ServiceMetadataTemplate;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Is the JPA entity for {@link ServiceMetadataTemplate}.
 *
 * @author Sander Fieten (sander at holodeck-b2b.org)
 */
@Entity(name = "ServiceMetadataTemplate")
@Getter
@NoArgsConstructor
public class ServiceMetadataTemplateE implements ServiceMetadataTemplate {

	@Id
	@GeneratedValue
	protected long oid;

	@Column
	@Setter
	protected String	name;

	@ManyToOne
	@Setter
	@NotNull(message = "A Service must be selected")
	protected ServiceE	service;

	@OneToMany(mappedBy = "template", cascade = CascadeType.ALL, orphanRemoval = true)
	@Setter
	protected List<ProcessGroupE>	processMetadata = new ArrayList<>();

	@Override
	public Identifier getServiceId() {
		return service != null ? service.getId() : null;
	}

	@Override
	public List<Extension> getExtensions() {
		return null;
	}

	@Override
	public boolean equals(Object o) {
		if (o == null || !(o instanceof ServiceMetadataTemplateE))
			return false;
		else
			return this.oid == ((ServiceMetadataTemplateE) o).getOid();
	}
}
