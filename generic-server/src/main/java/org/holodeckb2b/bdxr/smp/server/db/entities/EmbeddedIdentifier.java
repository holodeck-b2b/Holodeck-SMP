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

import org.holodeckb2b.bdxr.smp.server.datamodel.IDScheme;
import org.holodeckb2b.bdxr.smp.server.datamodel.Identifier;
import org.holodeckb2b.commons.util.Utils;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Defines the JPA class for storing an {@link Identifier}. As identifiers always belong to a specific meta-data 
 * registration, such as a <i>Participant</i> or <i>Service</i>, it is defined as {@link Embeddable}.
 *
 * @author Sander Fieten (sander at holodeck-b2b.org)
 */
@Embeddable
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class EmbeddedIdentifier implements Identifier {
	private static final long serialVersionUID = 1042891282124928495L;

	@ManyToOne(optional = true)	
	@JoinColumn(name = "idscheme")
	protected IDSchemeEntity		scheme;

	@Setter
	@Column(name="idvalue", nullable = false)
	@NotBlank(message = "An identifier value must be specified")
	protected String		value;

	/**
	 * Creates a new Identifier with the given value and no id scheme assigned.
	 *
	 * @param value		the actual value of the identifier
	 */
	public EmbeddedIdentifier(String value) {
		this(null, value);
	}
	
	@Override
	public void setScheme(IDScheme scheme) {
		if (scheme != null && !(scheme instanceof IDSchemeEntity))
			throw new IllegalArgumentException("Unmanaged IDScheme object");
		this.scheme = (IDSchemeEntity) scheme;
	}
	
    /**
     * Gets a string representation of the identifier which is constructed by concatenating the scheme and value
     * separated by a "::".
     * <p>
     * NOTE: This method takes the case-sensivity of the identifier scheme to which the identifier belongs into account,
     * i.e. will return the lower-case version of the identifier if no scheme is specified or the scheme is defined as
     * being case-insensitive.
     *
     * @return String representation of the identifier
     * @see #getURLEncoded()
     */
    @Override
    public String toString() {
    	if (scheme == null)
    		return value.toLowerCase();
    	else 
    		return scheme.getSchemeId() + "::" + (scheme.isCaseSensitive() ? value : value.toLowerCase());
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
		if (o == null || !(o instanceof org.holodeckb2b.bdxr.common.datamodel.Identifier))
			return false;
		else {
			org.holodeckb2b.bdxr.common.datamodel.Identifier other = (org.holodeckb2b.bdxr.common.datamodel.Identifier) o;
			if (this.scheme == null )
				return other.getScheme() == null && Utils.nullSafeEqual(this.value, other.getValue());
			else
				return other.getScheme() != null && this.scheme.getSchemeId().equals(other.getScheme().getSchemeId())
							&& !this.scheme.isCaseSensitive() ? this.value.equalsIgnoreCase(other.getValue())
												 				: this.value.equals(other.getValue());
		}
	}
	
	@Override
	public int hashCode() {
		return toString().hashCode();
	}
}
