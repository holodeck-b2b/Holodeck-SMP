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
import org.holodeckb2b.bdxr.smp.server.datamodel.Identifier;
import org.holodeckb2b.bdxr.smp.server.datamodel.Process;
import org.holodeckb2b.bdxr.smp.server.datamodel.ProcessIdentifier;
import org.holodeckb2b.bdxr.smp.server.datamodel.ProcessInfo;
import org.holodeckb2b.bdxr.smp.server.utils.IdUtils;

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
 * Is the JPA entity for storing the {@link ProcessInfo} meta-data in the database.
 *
 * @author Sander Fieten (sander at holodeck-b2b.org)
 */
@Entity(name = "ProcessInfo")
@Getter
@NoArgsConstructor
public class ProcessInfoEntity implements ProcessInfo {
	private static final long serialVersionUID = -2595632878536900690L;

	@Id
	@GeneratedValue
	protected Long		oid;
	
	@ManyToOne
	protected ProcessGroupEntity	procgroup;

	@ManyToOne
	protected ProcessEntity	process;

	@ElementCollection(fetch = FetchType.EAGER)
	@CollectionTable(name = "PI_ROLES")
	@Setter
	protected Set<EmbeddedIdentifier>	roles = new HashSet<>();

	public ProcessInfoEntity(ProcessGroupEntity pg, ProcessInfo src) {
		this.procgroup = pg;
		setProcess(src.getProcess());
		if (src.getRoles() != null)
			src.getRoles().forEach(r -> addRole(r));
	}

	@Override
	public ProcessIdentifier getProcessId() {
		return process != null ? process.getId() : null;
	}

	@Override
	public void setProcess(Process process) {
		if (process == null )
			throw new IllegalArgumentException("Process shall not be null");
		else if (!(process instanceof ProcessEntity) || (((ProcessEntity) process).getOid() == null))
			throw new IllegalArgumentException("Given Process instance is not managed");
		this.process = (ProcessEntity) process;
	}
	
	@Override
	public void addRole(org.holodeckb2b.bdxr.smp.datamodel.Identifier roleId) {
		if (roleId == null )
			throw new IllegalArgumentException("RoleID shall not be null");
		IdUtils.checkManagedScheme(roleId.getScheme());		
		roles.add(new EmbeddedIdentifier((IDSchemeEntity) roleId.getScheme(), roleId.getValue()));
	}
	
	@Override
	public void removeRole(Identifier roleId) {
		roles.remove(roleId);
	}
	
	@Override
	public List<Extension> getExtensions() {
		return null;
	}
}
