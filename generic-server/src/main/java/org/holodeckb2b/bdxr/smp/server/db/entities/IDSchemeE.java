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

import org.holodeckb2b.bdxr.smp.datamodel.IDScheme;
import org.holodeckb2b.commons.util.Utils;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Is the JPA entity for {@link IDScheme}. To simplify handling of identifiers the schemes are immutable so identifier
 * values can be set once to correct casing and can be compared directly without the need to check the case sensitivity
 * policy of the scheme.
 *
 * @author Sander Fieten (sander at holodeck-b2b.org)
 */
@Entity(name = "IDScheme")
@Getter
@Setter
@NoArgsConstructor
public class IDSchemeE implements IDScheme {

	@Id
	@Column(nullable = false, unique = true, updatable = false)
	@NotBlank(message = "A scheme identifier must be provided")
	protected String	schemeId;

	@Column
	protected Boolean	caseSensitive = Boolean.FALSE;

	@Column
	protected String	name;

	@Column
	protected String	agency;

	@Column
	protected String	locationURI;

	@Override
	public boolean isCaseSensitive() {
		return caseSensitive != null ? caseSensitive.booleanValue() : false;
	}

	@Override
	public boolean equals(Object o) {
		if (o == null || !(o instanceof IDScheme))
			return false;
		else {
			IDScheme other = (IDScheme) o;
			return this.isCaseSensitive() == other.isCaseSensitive()
				&& Utils.nullSafeEqual(this.getSchemeId(), other.getSchemeId());
		}
	}
}
