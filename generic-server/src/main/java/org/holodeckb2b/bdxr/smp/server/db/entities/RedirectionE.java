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
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.List;

import org.holodeckb2b.bdxr.smp.datamodel.Extension;
import org.holodeckb2b.bdxr.smp.datamodel.Redirection;
import org.holodeckb2b.bdxr.smp.datamodel.RedirectionV2;
import org.holodeckb2b.commons.security.CertificateUtils;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;

/**
 * Is the JPA entity for {@link Redirection}.
 *
 * @author Sander Fieten (sander at holodeck-b2b.org)
 */
@Entity(name = "Redirection")
public class RedirectionE implements RedirectionV2 {

	@Id
	@GeneratedValue
	protected long			oid;

	@Column(nullable = false)
	protected	String		url;

	@Column(length = 6144)
	protected byte[]		cert;

	public long	getOid() {
		return oid;
	}

	@Override
	public URL getNewSMPURL() {
		try {
			return new URL(url);
		} catch (MalformedURLException ex) {
			throw new IllegalStateException("Invalid URL stored in database");
		}
	}

	/**
	 * Sets a new URL for this redirection
	 *
	 * @param url URL of the SMP to which queries must be redirected
	 */
	public void setRedirectionURL(URL url) {
		if (url == null)
			throw new IllegalArgumentException("Redirection must have a URL");
		this.url = url.toString();
	}

	@Override
	public X509Certificate getSMPCertificate() {
		try {
			return CertificateUtils.getCertificate(cert);
		} catch (CertificateException ex) {
			throw new IllegalStateException("Invalid cert data", ex);
		}
	}

	/**
	 * Sets a new X509 certificate for the SMP to which queries are redirected.
	 *
	 * @param cert	the X.509v3 Certificate to use
	 * @throws CertificateEncodingException when the X509 certificate cannot be encoded
	 */
	public void setSMPCertificate(X509Certificate cert) throws CertificateEncodingException {
		this.cert = cert != null ? cert.getEncoded() : null;
	}

	@Override
	public List<Extension> getExtensions() {
		return null;
	}

	@Override
	public boolean equals(Object o) {
		if (o == null || !(o instanceof RedirectionE))
			return false;
		else
			return this.oid == ((RedirectionE) o).getOid();
	}
}
