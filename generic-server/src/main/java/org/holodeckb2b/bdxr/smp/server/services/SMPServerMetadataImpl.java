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
package org.holodeckb2b.bdxr.smp.server.services;

import java.net.MalformedURLException;
import java.net.URL;
import java.security.cert.X509Certificate;

import org.holodeckb2b.bdxr.smp.datamodel.Certificate;
import org.holodeckb2b.bdxr.smp.server.datamodel.SMPServerMetadata;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

/**
 * Is the Core implementation of the {@link SMPServerMetadata} interface.
 * 
 * @author Sander Fieten (sander at holodeck-b2b.org)
 */
@AllArgsConstructor
@Getter
public class SMPServerMetadataImpl implements SMPServerMetadata {

	@Setter
	@NotBlank(message = "A SMP ID must be provided")
	String 	SMPId;

	@NotNull(message = "An absolute base URL for SMP queries must be provided")
	URL 	baseUrl;

	@Setter
	String 	IPv4Address;
	
	@Setter
	String 	IPv6Address;
	
	X509Certificate certificate;
	
	Certificate 	pendingCertificateUpdate;
	
	public void setBaseUrl(String url) {
		try {
			baseUrl = new URL(url);
		} catch (MalformedURLException invalidURL) {
			baseUrl = null;
		}
	}
	
	public void setBaseUrl(URL url) {
		baseUrl = url;
	}
}
