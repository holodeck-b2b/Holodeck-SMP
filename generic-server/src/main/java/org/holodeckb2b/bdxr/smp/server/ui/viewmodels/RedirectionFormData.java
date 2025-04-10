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

import org.holodeckb2b.bdxr.smp.server.db.entities.RedirectionE;
import org.holodeckb2b.commons.security.CertificateUtils;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * UI model for editing the meta-data of a Redirection.
 *
 * @author Sander Fieten (sander at holodeck-b2b.org)
 */
@Getter
@Setter
@NoArgsConstructor
public class RedirectionFormData {
	/**
	 * Is the list index of the Process Group the edited Redirection is part of
	 */
	private int	pgIndex;

	private Long    oid;
	@NotBlank(message = "A redirection URL must be set")
	private String	targetURL;
	private String	pemText;
	private String	subjectName;

	public RedirectionFormData(int pg) {
		pgIndex = pg;
	}

	public RedirectionFormData(int pg, RedirectionE r) {
		pgIndex = pg;
		oid = r.getOid();
		X509Certificate x509Cert = r.getSMPCertificate();
		if (x509Cert != null) {
			try {
				pemText = CertificateUtils.getPEMEncoded(x509Cert);
			} catch (CertificateException ex) {
				pemText = null;
			}
			subjectName = x509Cert.getSubjectX500Principal().getName();
		}
		targetURL = r.getNewSMPURL().toString();
	}
}
