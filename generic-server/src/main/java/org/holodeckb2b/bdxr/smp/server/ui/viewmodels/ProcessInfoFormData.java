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
package org.holodeckb2b.bdxr.smp.server.ui.viewmodels;

import java.util.Set;

import org.holodeckb2b.bdxr.smp.server.db.entities.IdentifierE;
import org.holodeckb2b.bdxr.smp.server.db.entities.ProcessInfoE;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * UI model for editing the meta-data of Process info included in a Process Group.
 *
 * @author Sander Fieten (sander at holodeck-b2b.org)
 */
@Getter
@Setter
@NoArgsConstructor
public class ProcessInfoFormData {

	/**
	 * Is the list index of the Process Group the edited Process Info is part of
	 */
	private int	pgIndex;
	/**
	 * Is the list index of the Process Info within the Process Group, will be -1
	 * when this is a new Process Info element
	 */
	private int procIndex;

	@NotNull(message = "A process must be selected")
	private Long	processOID;

	private Set<IdentifierE>	roles;

	/**
	 * When the form is submitted the roles will be serialised as a comma separated list of the Role identifier string
	 * representations
	 */
	private String rolesAsArray;

	public ProcessInfoFormData(int pg, int p) {
		this.pgIndex = pg;
		this.procIndex = p;
	}

	public ProcessInfoFormData(int pg, int p, ProcessInfoE pi) {
		this.pgIndex = pg;
		this.procIndex = p;
		this.processOID = pi.getProcess().getOid();
		this.roles = pi.getRoles();
	}
}
