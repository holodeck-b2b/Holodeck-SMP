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
import org.holodeckb2b.bdxr.smp.datamodel.ProcessGroup;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Is the JPA entity for {@link ProcessGroup}.
 *
 * @author Sander Fieten (sander at holodeck-b2b.org)
 */
@Entity(name = "ProcessGroup")
@Getter
@NoArgsConstructor
public class ProcessGroupE implements ProcessGroup {

	@Id
	@GeneratedValue
	protected long	oid;

	@ManyToOne
	protected ServiceMetadataTemplateE template;

	@OneToMany(mappedBy = "procgroup", cascade = CascadeType.ALL, orphanRemoval = true)
	@Setter
	protected List<ProcessInfoE>	processInfo = new ArrayList<>();

	@ManyToMany
	@Setter
	protected List<EndpointInfoE>	endpoints = new ArrayList<>();

	@OneToOne(cascade = CascadeType.ALL, orphanRemoval = true)
	@Setter
	protected RedirectionE		redirection;

	public ProcessGroupE(ServiceMetadataTemplateE t) {
		this.template = t;
	}

	@Override
	public List<Extension> getExtensions() {
		return null;
	}

	@Override
	public boolean equals(Object o) {
		if (o == null || !(o instanceof ProcessGroupE))
			return false;
		else
			return this.oid == ((ProcessGroupE) o).getOid();
	}
}
