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

import org.holodeckb2b.bdxr.smp.datamodel.ProcessIdentifier;

import jakarta.persistence.Embeddable;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Is the JPA entity for {@link ProcessIdentifier}. Uses the "normal" {@link IdentifierE} entity as base class and add
 * the "no process" indicator.
 *
 * @author Sander Fieten (sander at holodeck-b2b.org)
 */
@Embeddable
@Getter
@NoArgsConstructor
public class ProcessIdentifierE extends IdentifierE implements ProcessIdentifier {

	public static final String NO_PROCESS = "hb2b:no-process";

	/**
	 * Creates a new Process Identifier with the given value and no id scheme assigned
	 *
	 * @param value		the actual value of the identifier
	 */
	public ProcessIdentifierE(String value) {
		this(value, null);
	}

	/**
	 * Creates a new Process Identifier with the given value and id scheme.
	 *
	 * @param value		the actual value of the identifier
	 * @param idScheme	the identifier scheme the identifier is part of
	 */
	public ProcessIdentifierE(String value, IDSchemeE idScheme) {
		super(value, idScheme);
	}

	@Override
	public boolean isNoProcess() {
		return NO_PROCESS.equals(value);
	}

	/**
	 * Indicates that this identifier is the special "no-process" id.
	 */
	public void setNoProcess() {
		this.scheme = null;
		this.value = NO_PROCESS;
	}

    @Override
    public String toString() {
		return isNoProcess() ? "{{No-Process}}" : super.toString();
    }

	@Override
	public boolean equals(Object o) {
		if (o == null || !(o instanceof ProcessIdentifier))
			return false;
		else {
			ProcessIdentifier other = (ProcessIdentifier) o;
			return (this.isNoProcess() && other.isNoProcess()) || super.equals(other);
		}
	}
}
