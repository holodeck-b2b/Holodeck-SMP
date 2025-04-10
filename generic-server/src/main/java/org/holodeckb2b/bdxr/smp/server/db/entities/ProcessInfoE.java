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

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.holodeckb2b.bdxr.smp.datamodel.Extension;
import org.holodeckb2b.bdxr.smp.datamodel.ProcessIdentifier;
import org.holodeckb2b.bdxr.smp.datamodel.ProcessInfo;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Is the JPA entity for {@link ProcessInfo}.
 *
 * @author Sander Fieten (sander at holodeck-b2b.org)
 */
@Entity(name = "ProcessInfo")
@Getter
@NoArgsConstructor
public class ProcessInfoE implements ProcessInfo {

	@Id
	@GeneratedValue
	protected long	oid;

	@ManyToOne
	protected ProcessGroupE	procgroup;

	@ManyToOne
	@Setter
	protected ProcessE	process;

	@ElementCollection(fetch = FetchType.EAGER)
	@CollectionTable(name = "PI_ROLES")
	@Setter
	protected Set<IdentifierE>	roles = new HashSet<>();

	public ProcessInfoE(ProcessGroupE pg) {
		this.procgroup = pg;
	}

	@Override
	public ProcessIdentifier getProcessId() {
		return process != null ? process.getId() : null;
	}

	@Override
	public List<Extension> getExtensions() {
		return null;
	}

	@Override
	public boolean equals(Object o) {
		if (o == null || !(o instanceof ProcessInfoE))
			return false;
		else
			return this.oid == ((ProcessInfoE) o).getOid();
	}
}
