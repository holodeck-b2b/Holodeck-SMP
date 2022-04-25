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

import java.net.MalformedURLException;
import java.net.URL;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.holodeckb2b.bdxr.smp.datamodel.EndpointInfo;
import org.holodeckb2b.bdxr.smp.datamodel.Extension;

/**
 * Is the JPA entity for {@link EndpointInfo}.
 *
 * @author Sander Fieten (sander at holodeck-b2b.org)
 */
@Entity(name = "Endpoint")
@Getter
@NoArgsConstructor
public class EndpointInfoE implements EndpointInfo {

	@Id
	@GeneratedValue
	protected long		oid;

	@Column
	@Setter
	protected String	name;

	@ManyToOne
	@NotNull
	@Setter
	protected TransportProfileE	profile;

	@Column
	@NotNull
	protected	String	url;

	@OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
	@Setter
	protected List<CertificateE>	certificates = new ArrayList<>();

	@Column
	@Setter
	protected ZonedDateTime	serviceActivationDate;

	@Column
	@Setter
	protected ZonedDateTime	serviceExpirationDate;

	@Column
	@Setter
	protected String	description;

	@Column
	@Setter
	protected String	contactInfo;

	@Override
	public String getTransportProfile() {
		return profile.getId();
	}

	@Override
	public URL getEndpointURL() {
		try {
			return new URL(url);
		} catch (MalformedURLException ex) {
			throw new IllegalStateException("Invalid URL stored in database");
		}
	}

	/**
	 * Sets a new URL for this endpoint
	 *
	 * @param url URL of the endpoint
	 */
	public void setEndpointURL(URL url) {
		if (url == null)
			throw new IllegalArgumentException("Endpoint must have a URL");
		this.url = url.toString();
	}

	@Override
	public List<Extension> getExtensions() {
		return null;
	}

	@Override
	public boolean equals(Object o) {
		if (o == null || !(o instanceof EndpointInfoE))
			return false;
		else
			return this.oid == ((EndpointInfoE) o).getOid();
	}
}
