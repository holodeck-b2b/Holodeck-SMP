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

import org.holodeckb2b.bdxr.smp.server.datamodel.IDScheme;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.NamedQuery;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Is the JPA entity for storing the {@link IDScheme} meta-data in the database. 
 *
 * @author Sander Fieten (sander at holodeck-b2b.org)
 */
@Entity(name = "IDScheme")
@Getter
@Setter
@NoArgsConstructor
@NamedQuery(name = "IDScheme.findBySchemeId", query = "SELECT s FROM IDScheme s WHERE s.schemeId = :schemeId")
public class IDSchemeEntity extends BaseMetadataRegistrationEntity<String> implements IDScheme {
	private static final long serialVersionUID = 1783194866238166644L;

	@Column(nullable = false, unique = true)
	@NotBlank(message = "A scheme identifier must be provided")
	protected String	schemeId;

	@Column
	protected Boolean	caseSensitive = Boolean.FALSE;

	@Column
	protected String	agency;

	@Column
	protected String	schemeSpecificationRef;
	
	public IDSchemeEntity(String schemeId) {
		this.schemeId = schemeId;
	}

	public IDSchemeEntity(String schemeId, boolean caseSensitive) {
		this.schemeId = schemeId;
		this.caseSensitive = caseSensitive;
	}
	
	public IDSchemeEntity(String schemeId, boolean caseSensitive, String name, String agency, String schemeSpecificationRef) {
		this.schemeId = schemeId;
		this.caseSensitive = caseSensitive;
		this.name = name;
		this.agency = agency;
		this.schemeSpecificationRef = schemeSpecificationRef;
	}
	
	@Override
	public String getId() {
		return schemeId;
	}
	
	@Override
	public void setId(String id) {
		this.schemeId = id;
	}

	@Override
	public boolean isCaseSensitive() {
		return caseSensitive != null ? caseSensitive.booleanValue() : false;
	}

	@Override
	public void setCaseSensitive(boolean caseSensitive) {
		this.caseSensitive = caseSensitive;		
	}
}
