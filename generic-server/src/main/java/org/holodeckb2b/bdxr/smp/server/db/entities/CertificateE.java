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

import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;

import org.holodeckb2b.bdxr.smp.datamodel.Certificate;
import org.holodeckb2b.bdxr.smp.datamodel.Extension;
import org.holodeckb2b.commons.security.CertificateUtils;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Is the JPA entity for {@link Certificate}.
 *
 * @author Sander Fieten (sander at holodeck-b2b.org)
 */
@Entity(name = "Certificate")
@Getter
@NoArgsConstructor
public class CertificateE implements Certificate {

	@Id
	@GeneratedValue
	protected long		oid;

	@Column(nullable = false, length = 6144)
	protected byte[]	cert;

	@Column
	@Setter
	protected String	usage;

	@Column
	@Setter
	protected LocalDateTime	activationDate;

	@Column
	@Setter
	protected LocalDateTime	expirationDate;

	@Column
	@Setter
	protected String	description;

	@Override
	public X509Certificate getX509Cert() {
		try {
			return CertificateUtils.getCertificate(cert);
		} catch (CertificateException ex) {
			throw new IllegalStateException("Invalid cert data", ex);
		}
	}

	/**
	 * Sets a new X509 certificate.
	 *
	 * @param cert	the X.509v3 Certificate to use
	 * @throws CertificateEncodingException when the X509 certificate cannot be encoded
	 */
	public void setCertificate(X509Certificate cert) throws CertificateEncodingException {
		if (cert == null)
			throw new IllegalArgumentException("X509 Certificate must not be null");
		this.cert = cert.getEncoded();
	}

	/**
	 * Sets a new X509 certificate.
	 *
	 * @param cert	the PEM encoded X.509v3 Certificate to use
	 * @throws CertificateException when the given string cannot be converted to a
	 *								a X509 certificate or if the certificate
	 *								cannot be encoded
	 */
	public void setCertificate(String pemEncodedCert) throws CertificateException {
		this.cert = CertificateUtils.getCertificate(pemEncodedCert).getEncoded();
	}

	@Override
	public ZonedDateTime getActivationDate() {
		return activationDate != null ?  activationDate.atZone(ZoneOffset.UTC) : null;
	}
	
	@Override
	public ZonedDateTime getExpirationDate() {
		return expirationDate != null ? activationDate.atZone(ZoneOffset.UTC) : null;
	}

	@Override
	public List<Extension> getExtensions() {
		return null;
	}

	@Override
	public boolean equals(Object o) {
		if (o == null || !(o instanceof CertificateE))
			return false;
		else
			return this.oid == ((CertificateE) o).getOid();
	}
}
