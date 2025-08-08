/*
 * Copyright (C) 2025 The Holodeck B2B Team
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

import org.holodeckb2b.bdxr.smp.server.datamodel.Contact;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Defines the JPA class for storing an {@link Contact} meta-data. As the contact information is always associated with
 * one Participant this class is defined as an <code>Embeddable</code>.
 * 
 * @author Sander Fieten (sander at holodeck-b2b.org)
 * @since 3.0.0
 */
@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class EmbeddedContact implements Contact {
	
	@Column
	protected String		name;
	
	@Column
	protected String		jobTitle;
	
	@Column
	protected String		department;
	
	@Column
	protected String		emailAddress;

	@Column
	protected String		telephone;
	
}
