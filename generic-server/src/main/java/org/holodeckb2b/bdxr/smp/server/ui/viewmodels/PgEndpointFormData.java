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

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * UI model for adding/editing an Endpoint in a Process Group.
 *
 * @author Sander Fieten (sander at holodeck-b2b.org)
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class PgEndpointFormData {

	/**
	 * Is the list index of the Process Group the edited Process Info is part of
	 */
	private int	pgIndex;

	/**
	 * The id of the Endpoint releated to the Process Group
	 */
	@NotNull(message = "An endpoint must be selected")
	private Long	epId;
}
