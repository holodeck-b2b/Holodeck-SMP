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
package org.holodeckb2b.bdxr.smp.server.queryapi;

import java.security.InvalidAlgorithmParameterException;
import java.security.KeyStore;
import java.security.KeyStore.PrivateKeyEntry;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.cert.CertificateException;
import java.util.Arrays;
import java.util.Collections;

import javax.xml.crypto.NoSuchMechanismException;
import javax.xml.crypto.dsig.DigestMethod;
import javax.xml.crypto.dsig.Reference;
import javax.xml.crypto.dsig.SignatureMethod;
import javax.xml.crypto.dsig.SignedInfo;
import javax.xml.crypto.dsig.Transform;
import javax.xml.crypto.dsig.XMLSignature;
import javax.xml.crypto.dsig.XMLSignatureException;
import javax.xml.crypto.dsig.XMLSignatureFactory;
import javax.xml.crypto.dsig.dom.DOMSignContext;
import javax.xml.crypto.dsig.keyinfo.KeyInfo;
import javax.xml.crypto.dsig.keyinfo.KeyInfoFactory;
import javax.xml.crypto.dsig.spec.C14NMethodParameterSpec;
import javax.xml.crypto.dsig.spec.TransformParameterSpec;

import org.holodeckb2b.bdxr.smp.server.services.core.SMPServerAdminService;
import org.holodeckb2b.bdxr.smp.server.services.query.ResponseSigningService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;

import lombok.extern.slf4j.Slf4j;

/**
 * Implements the {@link ResponseSigningService}.
 *
 * @author Sander Fieten (sander at holodeck-b2b.org)
 */
@Service
public class ResponseSigner implements ResponseSigningService {

	@Autowired
	protected SMPServerAdminService		adminSvc;

	@Override
	public Document signResponse(Document response, String signingAlg, String digestAlg, String c14nAlg)
																						throws XMLSignatureException {
		PrivateKeyEntry keyPair = adminSvc.getActiveKeyPair();
		if (keyPair == null) 
			throw new XMLSignatureException("Signing certificate not available");
		
		XMLSignatureFactory f;
		try {
			f = XMLSignatureFactory.getInstance("DOM", "ApacheXMLDSig");
		} catch (NoSuchProviderException noSantuario) {
			try {
				f = XMLSignatureFactory.getInstance();
			} catch (NoSuchMechanismException nsm) {
				throw new XMLSignatureException("No XML signature library available!");
			}
		}
		DigestMethod dm;
		try {
			dm = f.newDigestMethod(digestAlg, null);
		} catch (NoSuchAlgorithmException | InvalidAlgorithmParameterException unsupportedDigestAlg) {
			throw new XMLSignatureException("Unsupported digest algorithm");
		}
		Transform c14n;
		try {
			c14n = f.newTransform(c14nAlg, (TransformParameterSpec) null);
		} catch (NoSuchAlgorithmException | InvalidAlgorithmParameterException unsupportedC14nAlg) {
			throw new XMLSignatureException("Unsupported canonicalisation algorithm");			
		}		
		Reference r;
		try {
			r = f.newReference("", dm, Arrays.asList(new Transform[] {
													f.newTransform(Transform.ENVELOPED, (TransformParameterSpec) null)
												}), null, null);
		} catch (NoSuchAlgorithmException | InvalidAlgorithmParameterException unsupportedTransform) {
			throw new XMLSignatureException("Enveloped signature not supported");
		}
		
		// Create the KeyInfo containing the X509Data with the SMP server's certificate
		KeyInfoFactory kif = f.getKeyInfoFactory();
		KeyInfo ki = kif.newKeyInfo(Collections.singletonList(
										kif.newX509Data(Collections.singletonList(keyPair.getCertificate()))));
		SignatureMethod sm;
		try {
			sm = f.newSignatureMethod(signingAlg, null);
		} catch (NoSuchAlgorithmException  | InvalidAlgorithmParameterException unsupportedSignAlg) {
			throw new XMLSignatureException("Unsupported signing algorithm");
		}

		SignedInfo si;
		try {
			si = f.newSignedInfo(f.newCanonicalizationMethod(c14nAlg, (C14NMethodParameterSpec) null),
								 sm, Collections.singletonList(r));
		} catch (NoSuchAlgorithmException | InvalidAlgorithmParameterException ex) {
			throw new XMLSignatureException("Error creating SignedInfo element", ex);
		}
		XMLSignature signature = f.newXMLSignature(si, ki);

		try {
			signature.sign(new DOMSignContext(keyPair.getPrivateKey(), response.getDocumentElement()));
		} catch (Exception signatureFailure) {
			throw new XMLSignatureException("Error signing the response", signatureFailure);
		}

		return response;
	}
}
