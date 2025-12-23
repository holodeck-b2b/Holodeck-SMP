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
import java.time.LocalDateTime;
import java.time.ZonedDateTime;

import org.hibernate.annotations.UpdateTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 
 * 
 * @author Sander Fieten (sander at holodeck-b2b.org)
 */
@Entity
@Setter
@Getter
@NoArgsConstructor
public class ServerConfigEntity {

	@Id
	@GeneratedValue
	protected Long		oid;
	
	@UpdateTimestamp
	protected LocalDateTime lastModified;
	
	@Column
	protected String	smpId;
	
	@Column
	protected String	baseUrl;
	
	@Column
	protected String	ipv4Address;
	
	@Column
	protected String	ipv6Address;
	
	@Column(length = 10240)
	protected byte[]	currentKeyPair;
	
	@Column(length = 10240)
	protected byte[]	nextKeyPair;

	@Column
	protected ZonedDateTime	activationDate;	
	
	@Column
	protected boolean	registeredSML = false;
	
	public URL getBaseUrl() {
		try {
			return baseUrl == null ? null : new URL(baseUrl);
		} catch (MalformedURLException e) {
			throw new IllegalStateException("Invalid SMP base URL (" + baseUrl + ") in database");
		}
	}

	public void setBaseUrl(URL url) {
		this.baseUrl = url.toString();
	}
}
