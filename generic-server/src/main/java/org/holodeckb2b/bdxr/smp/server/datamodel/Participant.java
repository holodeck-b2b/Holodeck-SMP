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

import org.holodeckb2b.bdxr.smp.datamodel.Identifier;

/**
 * Represents the meta-data stored in an SMP server about a <i>Participant</i>. 
 *
 * @author Sander Fieten (sander at holodeck-b2b.org)
 */
public interface Participant {

	/**
	 * Gets the identifier the Participant is identified by
	 *
	 * @return	the Participant ID
	 */
	Identifier	getId();

	/**
	 * Gets the name of the Participant
	 *
	 * @return the Participant's name
	 */
	String getName();

	/**
	 * Gets the country where the Participant is located
	 *
	 * @return the Participant's country 
	 */
	String getCountry();
		
	/**
	 * Gets the additional address info where the Participant is located
	 *
	 * @return the Participant's address information
	 */
	String getAddressInfo();
	
	/**
	 * Gets the information how the Participant can be contacted.
	 *
	 * @return	the contact data of the Participant
	 */
	String getContactInfo();
}
