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
package org.holodeckb2b.bdxr.smp.server.svc;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyStore;
import java.security.cert.CertificateException;
import java.util.Scanner;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.holodeckb2b.commons.security.KeystoreUtils;
import org.holodeckb2b.commons.util.Utils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 *
 * @author Sander Fieten (sander at holodeck-b2b.org)
 */
@Service
public class SMPCertificateService {
	private static final Logger	log	= LogManager.getLogger();

	@Value( "${smp.keylocation:${smp.home:.}/signkeypair.p12}" )
	protected String ksLocation;

	private static KeyStore.PrivateKeyEntry	current;

	/**
	 * Gets the key pair the SMP server uses to sign the query responses.
	 *
	 * @return key pair used for signing responses if it can be loaded, or
	 *		   <code>null</code> if no key pair has been configured yet
	 * @throws CertificateException when a key pair is configured, but could not be loaded
	 */
	public KeyStore.PrivateKeyEntry getKeyPair() throws CertificateException {
		if (current == null)
			readKeyPair();
		return current;
	}

	/**
	 * Reads the key pair from the file system.
	 *
	 * @throws CertificateException	when there is a problem retrieving the key pair from the file system.
	 */
	private synchronized void readKeyPair() throws CertificateException {
		final Path ksPath = Paths.get(ksLocation);
		if (!Files.exists(ksPath))
			return;

		String password = null;
		Path pwdPath = ksPath.resolveSibling(ksPath.getFileName().toString() + ".pwd");
		try (Scanner s = new Scanner(pwdPath.toFile())) {
			password = s.hasNextLine() ? s.nextLine() : null;
		} catch (IOException ex) {
			log.error("Could not read the password file ({}) : {}", pwdPath.toString(), ex.getMessage());
			throw new CertificateException("Error reading password file", ex);
		}
		try (FileInputStream fis = new FileInputStream(ksPath.toFile())) {
			current =  KeystoreUtils.readKeyPairFromKeystore(fis, password);
		} catch (IOException | CertificateException ex) {
			log.error("An error occured reading the key pair from file ({}). Error trace: {}", ksPath.toString(),
						Utils.getExceptionTrace(ex, true));
			throw new CertificateException("Error reading key pair", ex);
		}
	}

	/**
	 * Saves a new key pair to be used by the SMP server for signing of the query responses.
	 *
	 * @param kp		the new key pair
	 * @throws CertificateException when the key pair could not be saved.
	 */
	public synchronized void setKeyPair(KeyStore.PrivateKeyEntry kp) throws CertificateException {
		final Path ksPath = Paths.get(ksLocation);
		Path pwdPath = ksPath.resolveSibling(ksPath.getFileName().toString() + ".pwd");
		FileOutputStream fos = null;
		try {
			log.trace("Write new key pair and password to temp files");
			fos = new FileOutputStream(ksPath.toFile());
			String password = "kpp-" + Long.toHexString(Double.doubleToLongBits(Math.random()));
			KeystoreUtils.saveKeyPairToPKCS12(kp, fos, password);
			fos.close();
			fos = new FileOutputStream(pwdPath.toFile());
			fos.write(password.getBytes());
		} catch (IOException ex) {
			log.error("Error saving the new key pair. Error message: {}", ex.getMessage());
			throw new CertificateException("Error saving key pair", ex);
		}
		current = kp;
		log.debug("Saved key pair");
	}
}
