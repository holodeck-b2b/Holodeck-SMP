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

import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZonedDateTime;

import org.holodeckb2b.bdxr.smp.server.db.entities.CertificateE;
import org.holodeckb2b.commons.security.CertificateUtils;
import org.springframework.format.annotation.DateTimeFormat;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * UI model for editing the meta-data of a Certificate.
 *
 * @author Sander Fieten (sander at holodeck-b2b.org)
 */
@Getter
@Setter
@NoArgsConstructor
public class CertificateFormData {

	private Long    oid;
	@NotBlank(message = "A PEM encoded certificate must be set")
	private String	pemText;
	private String	subjectName;
	private String	usage;
	@DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
	private LocalDate	activationDate;
	@DateTimeFormat(pattern = "HH:mm")
	private LocalTime	activationTime;
	@DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
	private LocalDate	expirationDate;
	@DateTimeFormat(pattern = "HH:mm")
	private LocalTime	expirationTime;

	public CertificateFormData(CertificateE c) {
		oid = c.getOid();
		X509Certificate x509Cert = c.getX509Cert();
		if (x509Cert != null) {
			try {
				pemText = CertificateUtils.getPEMEncoded(x509Cert);
			} catch (CertificateException ex) {
				pemText = null;
			}
			subjectName = x509Cert.getSubjectX500Principal().getName();
		}
		ZonedDateTime activation = c.getActivationDate();
		activationDate = activation != null ? activation.toLocalDate() : null;
		activationTime = activation != null ? activation.toLocalTime() : null;
		ZonedDateTime expiration = c.getExpirationDate();
		expirationDate = expiration != null ? expiration.toLocalDate() : null;
		expirationTime = expiration != null ? expiration.toLocalTime() : null;
		usage = c.getUsage();
	}
}
