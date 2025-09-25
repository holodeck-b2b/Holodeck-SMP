/*
 * Copyright (C) 2025 The Holodeck B2B Team
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
package org.holodeckb2b.bdxr.smp.server.utils;

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.util.Arrays;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

import org.bouncycastle.crypto.CryptoException;
import org.holodeckb2b.commons.util.Utils;

import lombok.extern.slf4j.Slf4j;

/**
 * Is a utility class for the encryption of data using a password derived key.
 *
 * @author Sander Fieten (sander at holodeck-b2b.org)
 */
@Slf4j
public class DataEncryptor {
	private static final byte[] SALT = new byte[]
			{ -123, -5,  75,  88,   4, 13, -27,  37,  87,  11, 121, 117,  92, 111,   7, -71,
			  -57, -91,  67, 102, -39, 38,  69,  39,  88,  66,  81,  84, -16,  81,  35,  44,
			  -23, -67,  69, 47,  67,  74,  22,  70, -17, -20,  90, -66,  55,  39, 108, 100 };

	// The AES key to encrypt/decrypt data
	private final SecretKey	key;
	// Random generator for IV
	private final SecureRandom random = new SecureRandom();
	
	public DataEncryptor(final String masterPwd) {
		try {
		    SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
		    KeySpec spec = new PBEKeySpec(masterPwd.toCharArray(), SALT, 65536, 256);
		    key = new SecretKeySpec(factory.generateSecret(spec).getEncoded(), "AES");
		} catch (NoSuchAlgorithmException | InvalidKeySpecException kdfFailure) {
			log.error("Could not derive key from password : {}", kdfFailure);
			throw new RuntimeException("Could not derive key from password", kdfFailure);
		}
	}

	/**
	 * Encrypts the given plain text using the secret key derived from the master password provided upon initialisation
	 * of the instance.
	 *
	 * @param plaintext	the plain text to encrypt, represented a byte array
	 * @return 	the encrypted text as byte array, prefixed with the IV
	 * @throws CryptoException when an error occurs during the encryption
	 */
	public byte[] encrypt(final byte[] plaintext) throws CryptoException {

	    byte[] iv = new byte[16];
	    random.nextBytes(iv);
	    IvParameterSpec ivSpec = new IvParameterSpec(iv);

		try {
			Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
			cipher.init(Cipher.ENCRYPT_MODE, key, ivSpec);
			byte[] ciphertext = cipher.doFinal(plaintext);

			byte[] result = new byte[16 + ciphertext.length];

			System.arraycopy(iv, 0, result, 0, 16);
			System.arraycopy(ciphertext, 0, result, 16, ciphertext.length);

			return result;
		} catch (Exception encError) {
			log.error("Unexpected error during encryption : {}", Utils.getExceptionTrace(encError));
			throw new CryptoException();
		}
	}

	/**
	 * Decrypts the given cipher text using the included IV and the key derived from the master password provided upon
	 * initialisation of the instance.
	 *
	 * @param ciphertext	a byte array with the cipher text, starting with the IV
	 * @return	the decrypted plain text, represented as a byte array
	 * @throws CryptoException when an error occurs during the decryption
	 */
	public byte[] decrypt(final byte[] ciphertext) throws CryptoException {
		if (ciphertext == null || ciphertext.length == 0)
			return null;

		try {
			IvParameterSpec ivSpec = new IvParameterSpec(Arrays.copyOf(ciphertext, 16));
			Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
			cipher.init(Cipher.DECRYPT_MODE, key, ivSpec);

			byte[] encrypted = new byte[ciphertext.length - 16];
			System.arraycopy(ciphertext, 16, encrypted, 0, encrypted.length);

			return cipher.doFinal(encrypted);
		} catch (Exception decrError) {
			log.error("Unexpected error during encryption : {}", Utils.getExceptionTrace(decrError));
			throw new CryptoException();
		}
	}
}