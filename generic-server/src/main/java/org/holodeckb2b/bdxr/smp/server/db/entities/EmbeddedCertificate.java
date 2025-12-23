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
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Objects;

import org.holodeckb2b.bdxr.common.datamodel.Extension;
import org.holodeckb2b.bdxr.smp.server.datamodel.Certificate;
import org.holodeckb2b.bdxr.smp.server.datamodel.Endpoint;
import org.holodeckb2b.commons.security.CertificateUtils;
import org.holodeckb2b.commons.util.Utils;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Transient;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Defines the JPA class for storing the {@link Certificate} meta-data in the database. As the certificate is always 
 * part of an {@link Endpoint} it is defined as an <code>Embeddable</code>.
 *
 * @author Sander Fieten (sander at holodeck-b2b.org)
 */
@Embeddable
@NoArgsConstructor
public class EmbeddedCertificate implements Certificate {
	private static final long serialVersionUID = 8592912716445061976L;

	/*
	 * The X.509v3 certificate itself is stored as a byte array containing the DER encoded version of the certificate
	 */
	@Column(nullable = false, length = 6144)
	@Getter
	protected byte[]	cert;
	
	@Transient
	private X509Certificate x509Cert;

	@Column(name = "TXT_USAGE")
	@Setter
	@Getter
	protected String	usage;

	@Column
	@Setter
	@Getter
	protected ZonedDateTime	activationDate;

	@Column
	@Setter
	@Getter
	protected ZonedDateTime	expirationDate;

	@Column
	@Setter
	@Getter
	protected String	description;
	
	/**
	 * Creates a new EmbeddedCertificate instance copying the meta-data from the given {@link Certificate} instance.
	 * 
	 * @param cert			the X.509v3 certificate to use
	 */
	EmbeddedCertificate(org.holodeckb2b.bdxr.smp.datamodel.Certificate source) {
		if (source.getX509Cert() == null)
			throw new IllegalArgumentException("X509 Certificate must not be null");
		try {
			this.cert = source.getX509Cert().getEncoded();
			this.x509Cert = source.getX509Cert();
		} catch (CertificateEncodingException e) {
			throw new IllegalArgumentException("Invalid X509 Certificate", e);
		}
		this.usage = source.getUsage();
		this.activationDate = source.getActivationDate();
		this.expirationDate = source.getExpirationDate();
		this.description = source.getDescription();
	}

	@Override
	public X509Certificate getX509Cert() {
		if (x509Cert == null) {
			try {
				 x509Cert = CertificateUtils.getCertificate(cert);
			} catch (CertificateException ex) {
				throw new IllegalStateException("Invalid cert data", ex);
			}
		}
		return x509Cert;
	}

	@Override
	public void setX509Cert(X509Certificate cert) {
		if (cert == null)
			throw new IllegalArgumentException("X509 Certificate must not be null");
		try {
			this.cert = cert.getEncoded();
		} catch (CertificateEncodingException e) {
			throw new IllegalArgumentException("Invalid X509 Certificate", e);
		}
	}

	/**
	 * Sets a new X509 certificate.
	 *
	 * @param cert	the PEM encoded X.509v3 Certificate to use
	 * @throws IllegalArgumentException 	when the given string cannot be converted to a a X509 certificate or if the 
	 * 										certificate cannot be encoded
	 */
	public void setCertificate(String pemEncodedCert) {
		try {
			this.cert = CertificateUtils.getCertificate(pemEncodedCert).getEncoded();
		} catch (CertificateException e) {
			throw new IllegalArgumentException("Invalid X509 Certificate", e);
		}
	}
	
	@Override
	public List<Extension<?>> getExtensions() {
		return null;
	}
	
	@Override
	public boolean equals(Object o) {
		if (!(o instanceof EmbeddedCertificate))
			return false;
		
		EmbeddedCertificate other = (EmbeddedCertificate) o;
		return Utils.nullSafeEqual(this.cert, other.cert)
				&& Utils.nullSafeEqual(this.usage, other.usage)
				&& Utils.nullSafeEqual(this.activationDate, other.activationDate)
				&& Utils.nullSafeEqual(this.expirationDate, other.expirationDate)
				&& Utils.nullSafeEqual(this.description, other.description);
	}
	
	@Override
	public int hashCode() {
		return Objects.hash(cert, usage, activationDate, expirationDate, description);
	}
}
