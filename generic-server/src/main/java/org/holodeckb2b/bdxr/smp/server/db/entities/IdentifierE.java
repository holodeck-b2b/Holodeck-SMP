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

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import org.holodeckb2b.bdxr.smp.datamodel.Identifier;
import org.holodeckb2b.commons.util.Utils;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MappedSuperclass;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Is the JPA entity for {@link Identifier}. As identifiers always belong to a specific <i>Participant</i>, <i>Process
 * </i>, <i>Service</i>, etc. the JPA object is defined as embeddable.
 *
 * @author Sander Fieten (sander at holodeck-b2b.org)
 */
@Embeddable
@MappedSuperclass
@Getter
@NoArgsConstructor
public class IdentifierE implements Identifier {

	@ManyToOne(optional = true)
	@Setter
	protected IDSchemeE		scheme;

	@Column(name="idvalue", nullable = false)
	@NotBlank(message = "An identifier value must be specified")
	protected String		value;

	/**
	 * Creates a new Identifier with the given value and no id scheme assigned.
	 *
	 * @param value		the actual value of the identifier
	 */
	public IdentifierE(String value) {
		this(value, null);
	}

	/**
	 * Creates a new Identifier with the given value and id scheme.
	 *
	 * @param value		the actual value of the identifier
	 * @param idScheme	the identifier scheme the identifier is part of
	 */
	public IdentifierE(String value, IDSchemeE idScheme) {
		this.scheme = idScheme;
		this.value = value;
	}

	/**
	 * Sets a new identifier value.
	 *
	 * @param value the new id
	 */
	public void setValue(String value) {
		this.value = value;
	}

    /**
     * Gets a string representation of the identifier which is constructed by concatenating the scheme and value
     * separated by a "::".
     *
     * @return String representation of the identifier
     * @see #getURLEncoded()
     */
    @Override
    public String toString() {
		return (scheme == null ? "" : scheme.getSchemeId() + "::") + getValue();
    }

	@Override
	public String getURLEncoded() {
		try {
            return URLEncoder.encode(toString(), "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException("UTF-8 not supported.");
        }
	}

	@Override
	public boolean equals(Object o) {
		if (o == null || !(o instanceof Identifier))
			return false;
		else {
			Identifier other = (Identifier) o;
			return this.scheme.equals(other.getScheme())
				&& this.scheme.isCaseSensitive() ? Utils.nullSafeEqual(this.value.toLowerCase(),
																	   other.getValue().toLowerCase())
												 : Utils.nullSafeEqual(this.value, other.getValue());
		}
	}
}
