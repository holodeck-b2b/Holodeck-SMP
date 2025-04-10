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

import org.holodeckb2b.bdxr.smp.server.db.entities.EndpointInfoE;
import org.springframework.format.annotation.DateTimeFormat;

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
	@NotBlank(message = "A transport profile must be selected")
	private String	profileId;
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

	public EndpointFormData(EndpointInfoE ep) {
		oid = ep.getOid();
		name = ep.getName();
		profileId = ep.getTransportProfile();
		url = ep.getEndpointURL().toString();
		ZonedDateTime activation = ep.getServiceActivationDate();
		activationDate = activation != null ? activation.toLocalDate() : null;
		activationTime = activation != null ? activation.toLocalTime() : null;
		ZonedDateTime expiration = ep.getServiceExpirationDate();
		expirationDate = expiration != null ? expiration.toLocalDate() : null;
		expirationTime = expiration != null ? expiration.toLocalTime() : null;
		contactInfo = ep.getContactInfo();
		description = ep.getDescription();
	}
}
