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
package org.holodeckb2b.bdxr.smp.server.queryapi;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.when;

import java.io.StringReader;
import java.security.KeyStore.PrivateKeyEntry;

import javax.xml.crypto.MarshalException;
import javax.xml.crypto.dsig.XMLSignature;
import javax.xml.crypto.dsig.XMLSignatureException;
import javax.xml.crypto.dsig.XMLSignatureFactory;
import javax.xml.crypto.dsig.dom.DOMValidateContext;
import javax.xml.parsers.DocumentBuilderFactory;

import org.holodeckb2b.bdxr.smp.server.services.core.SMPServerAdminService;
import org.holodeckb2b.commons.security.KeystoreUtils;
import org.holodeckb2b.commons.testing.TestUtils;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

@SpringBootTest(classes = { QueryAppConfig.class })
class ResponseSignerTest {

	@MockitoBean
	private SMPServerAdminService 		adminService;
	
	@Autowired
	private ResponseSigner 	signer;
	
	private static PrivateKeyEntry T_KEYPAIR_1;
	private static Document T_RESPONSE_XML; 
	
	@BeforeAll
	static void setup() {
	    try {
	    	T_RESPONSE_XML = DocumentBuilderFactory.newInstance()
	    							.newDocumentBuilder()
	    							.parse(new InputSource(new StringReader(
	    								"<Response><Status>OK</Status><Data>Just a simple example</Data></Response>")));
	    	T_KEYPAIR_1 = KeystoreUtils.readKeyPairFromPKCS12(TestUtils.getTestResource("testkey1.p12"), null);
	    } catch (Exception e) {
	        fail(e);
	    }
	}	
	
	
	@Test
	void testSignResponse() {
		when(adminService.getActiveKeyPair()).thenReturn(T_KEYPAIR_1);
		
		final String signingAlgorithm = "http://www.w3.org/2001/04/xmldsig-more#rsa-sha256";
		final String digestAlgorithm = "http://www.w3.org/2001/04/xmlenc#sha256";
		final String c14nAlgorithm = "http://www.w3.org/TR/2001/REC-xml-c14n-20010315";
		
		Document signed = assertDoesNotThrow(() -> 
								signer.signResponse(T_RESPONSE_XML, signingAlgorithm, digestAlgorithm, c14nAlgorithm));
		
		NodeList signatures = signed.getElementsByTagNameNS("http://www.w3.org/2000/09/xmldsig#", "Signature");		
		assertNotNull(signatures);
		assertEquals(1, signatures.getLength());
		
		assertTrue(verifySignature((Element) signatures.item(0)));
	}

	
	
	private boolean verifySignature(Element sigElement) {
		try {
            DOMValidateContext valCtx = new DOMValidateContext(T_KEYPAIR_1.getCertificate().getPublicKey(), sigElement);
//            valContext.setProperty("org.jcp.xml.dsig.secureValidation", clientConfig.useSecureSignatureValidation());
//            valContext.setProperty("org.apache.jcp.xml.dsig.secureValidation", clientConfig.useSecureSignatureValidation());
            XMLSignatureFactory xmlSignatureFactory = XMLSignatureFactory.getInstance("DOM");
            XMLSignature signature = xmlSignatureFactory.unmarshalXMLSignature(valCtx);            
            return signature.validate(valCtx);
        } catch (XMLSignatureException | MarshalException verificationFailed) {
            return false;
        }
	}
}
