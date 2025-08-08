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
package org.holodeckb2b.bdxr.smp.server.ui.viewmodels;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.util.ArrayList;

import org.holodeckb2b.bdxr.smp.server.db.entities.EmbeddedIdentifier;
import org.holodeckb2b.bdxr.smp.server.db.entities.EndpointEntity;
import org.holodeckb2b.bdxr.smp.server.db.entities.IDSchemeEntity;
import org.springframework.format.annotation.DateTimeFormat;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * UI model for editing the meta-data of a Endpoint.
 *
 * @author Sander Fieten (sander at holodeck-b2b.org)
 */
@Getter
@Setter
@NoArgsConstructor
public class EndpointFormData {

	private Long    oid;
	@NotBlank(message = "A Endpoint name must be provided")
	private String name;
	@Valid
	private EmbeddedIdentifier	profileId;
	@NotBlank(message = "An endpoint URL must be set set")
	private String	url;
	@DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
	private LocalDate	activationDate;
	@DateTimeFormat(pattern = "HH:mm")
	private LocalTime	activationTime;
	@DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
	private LocalDate	expirationDate;
	@DateTimeFormat(pattern = "HH:mm")
	private LocalTime	expirationTime;
	private String		contactInfo;
	private String		description;
	
	private ArrayList<CertificateFormData>	certs = new ArrayList<>();

	public EndpointFormData(EndpointEntity ep) {
		oid = ep.getOid();
		name = ep.getName();
		if (ep.getTransportProfileId() != null)
			profileId = new EmbeddedIdentifier((IDSchemeEntity) ep.getTransportProfileId().getScheme(), 
											ep.getTransportProfileId().getValue());
		else
			profileId = new EmbeddedIdentifier();
		url = ep.getEndpointURL() != null ? ep.getEndpointURL().toString() : null;
		ZonedDateTime activation = ep.getServiceActivationDate();
		activationDate = activation != null ? activation.toLocalDate() : null;
		activationTime = activation != null ? activation.toLocalTime() : null;
		ZonedDateTime expiration = ep.getServiceExpirationDate();
		expirationDate = expiration != null ? expiration.toLocalDate() : null;
		expirationTime = expiration != null ? expiration.toLocalTime() : null;
		contactInfo = ep.getContactInfo();
		description = ep.getDescription();
		
		ep.getCertificates().forEach(c -> certs.add(new CertificateFormData(c)));
	}	
}
