/*
 * Copyright (C) 2023 The Holodeck B2B Team
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
package org.holodeckb2b.bdxr.smp.server.db;

import java.io.Serializable;

import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Is the JPA entity that represent the SMP data registered in the Peppol SML.
 *
 * @author Sander Fieten (sander at holodeck-b2b.org)
 */
@Entity(name = "SMLRegistration")
@Getter
@Setter
@NoArgsConstructor
public class SMLRegistration implements Serializable {

	@Id
	@GeneratedValue
	protected long		oid;

	@Column
	protected String	environment;

	@Column(nullable = false)
	@NotBlank(message = "An identifier value must be specified")
	protected String	identifier;

	@Column(nullable = false)
	@NotBlank(message = "The IP address must be specified")
	@Pattern(regexp = "^(?:[0-9]{1,3}\\.){3}[0-9]{1,3}$", message = "Please provide a valid IPv4 address")
	protected String	ipAddress;

	@Column(nullable = false)
	@NotBlank(message = "The hostname must be specified")
	protected String	hostname;

	@Embedded
	protected CertificateUpdate pendingCertUpdate;
	
	public SMLRegistration(String env) {
		this.environment = env;
	}

	@Override
	public boolean equals(Object o) {
		if (o == null || !(o instanceof SMLRegistration))
			return false;
		else
			return this.oid == ((SMLRegistration) o).getOid();
	}
}
