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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.security.KeyStore;
import java.time.LocalDate;

import org.holodeckb2b.commons.security.KeystoreUtils;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Lob;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Is the JPA entity that contains the data on a pending certificate update.
 *
 * @author Sander Fieten (sander at holodeck-b2b.org)
 * @since 1.1.0
 */
@Embeddable
@NoArgsConstructor
public class CertificateUpdate {

	@Lob
	@Column
	protected byte[]	keypair;

	@Setter
	@Getter
	@Column
	protected LocalDate	activation;

	public CertificateUpdate(KeyStore.PrivateKeyEntry kp, LocalDate activation) {
		this.activation = activation;
		setKeyPair(kp);
	}

	public KeyStore.PrivateKeyEntry getKeyPair() {
		try (ByteArrayInputStream bais = new ByteArrayInputStream(keypair)) {
			return KeystoreUtils.readKeyPairFromKeystore(bais, null);
		} catch (Exception ex) {
			return null;
		}
	}

	public void setKeyPair(KeyStore.PrivateKeyEntry kp) {
		try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
			KeystoreUtils.saveKeyPairToPKCS12(kp, baos, null);
			this.keypair = baos.toByteArray();
		} catch (Exception ex) {
			this.keypair = null;
		}
	}
}
