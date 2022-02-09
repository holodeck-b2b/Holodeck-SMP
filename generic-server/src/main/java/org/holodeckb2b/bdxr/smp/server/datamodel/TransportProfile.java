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
package org.holodeckb2b.bdxr.smp.server.datamodel;

/**
 * Represents the meta-data stored in an SMP server about a <i>Transport Profile</i>.
 *
 * @author Sander Fieten (sander at holodeck-b2b.org)
 */
public interface TransportProfile {

	/**
	 * Gets the identifier of the transport profile.
	 *
	 * @return	the transport profile identifier. Note that this is just a string and not a {@link Identifier} as
	 *			used for other SMP meta-data like Participant
	 */
	String getId();

	/**
	 * Gets a reference to the specification of the transport profile
	 *
	 * @return	a reference to the specification of the profile
	 */
	String getSpecificationRef();
}
