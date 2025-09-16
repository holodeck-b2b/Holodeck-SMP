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
package org.holodeckb2b.bdxr.smp.server.db.entities;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URL;
import java.security.cert.X509Certificate;

import org.holodeckb2b.bdxr.smp.datamodel.impl.CertificateImpl;
import org.holodeckb2b.commons.security.CertificateUtils;
import org.junit.jupiter.api.Test;

class EndpointEntityTest extends BaseEntityTest<EndpointEntity> {
	
	static final String T_CERT_PEM = """
			-----BEGIN CERTIFICATE-----
			MIIFvjCCA6agAwIBAgICEBQwDQYJKoZIhvcNAQELBQAwZjELMAkGA1UEBhMCTkwx
			ETAPBgNVBAoMCENoYXNxdWlzMR0wGwYDVQQLDBRIb2xvZGVjayBCMkIgU3VwcG9y
			dDElMCMGA1UEAwwcY2EuZXhhbXBsZXMuaG9sb2RlY2stYjJiLm9yZzAeFw0yNDA1
			MjEwODM1MTFaFw0yNzA1MjEwODM1MTFaMGoxCzAJBgNVBAYTAk5MMREwDwYDVQQK
			DAhDaGFzcXVpczEdMBsGA1UECwwUSG9sb2RlY2sgQjJCIFN1cHBvcnQxKTAnBgNV
			BAMMIHBhcnR5YS5leGFtcGxlcy5ob2xvZGVjay1iMmIuY29tMIICIjANBgkqhkiG
			9w0BAQEFAAOCAg8AMIICCgKCAgEA4T98DsywFKLH6UYqV8N9P8gTbdCEPbb5Gm8n
			dnCWUwSFwVX4CCMwHHAIxxy2gdf4lb7XUzOD6WahQsdpM8Fwcj+SX2HJHtpt6JS6
			Cu9QlPxp5MXW0gWyYv7+RLE2Xj+KM2++b/stBC1I6kjUyevtGmea9ufOA3XEJ5jO
			iQ+afk34UAlN9Ta+qpwrtJKxRq6SIB8zaGlU0OsEVZPP2a1QpBVm/1axbG4XRp+Q
			F7mSh0PV1g2ICrE4xXPqqIWdiTKzTWl4xePnLCxdFQkXOjPxo+GAjNnNhXdtaZS+
			KUN2yLIw0Xay3I8HeLMGBHhAIOHBHvwng367RjO3zwbgvt5dcEKWVF57aOBoksGa
			fEfqhN6KNqZM9d8/Aq46GiqHw/2JtEHledKRW8+9S0ri9yAo7vr2RiHQt74Ey+K+
			+NxpHMmAEmnTwK1ki40Lmeih3oKRucUOOWF62K4T++u7X71xkznIeEGxLznSqnPD
			8mwowHN3StQFiMn+Xt66m+a+K3F3NlWYkzeZRPrEA0Wqv6K+z0MbB3JYv1CXuhb5
			kYEGEqsau395/yrn/MbU8+iWU7fNASlHBktwMXHm9NKcuLqiF8TuamZ/5XVBuPIe
			XwuTcdoOh2wxoH9hZDwerkBHJUOgLiUG4Rh6H332uBljkIESqe1eDEWbPNlHlTpt
			Kxjb5YcCAwEAAaNyMHAwCQYDVR0TBAIwADAOBgNVHQ8BAf8EBAMCBeAwEwYDVR0l
			BAwwCgYIKwYBBQUHAwIwHQYDVR0OBBYEFAPf9TzA6vwmsJlWTQY068Zjcks+MB8G
			A1UdIwQYMBaAFGogotBTFmhJkji5a7pAr+ggs75/MA0GCSqGSIb3DQEBCwUAA4IC
			AQAJkTbCLAg034fCCS/D/Frnq4VBlU7ufP/uTPCsXGJ/kea982GvLqQk+Y3SycRr
			N1JRr9WoJewVOXdirnuCWc4bS+KM1lm+DCDXyHCMudBZ0Wk5fZ+HIY2XktWitG11
			itZsXAd5uoed/cjXoQv3rCGQohTHVpVA+QybyXsqDiACmiGr+2ry4Ua+Jj6f57ka
			72O387+Lr/M3uSB/6ADiWnt9t5LF9ZDWTJda11Ue2/8xuSSPrCrUNMoIzoGU8QBy
			sHuSalTz2MfSXetMRWbvPoftmOsfQ9PDo6/x9Yw1n4+fU8/k/8UUrcom/x+8nWei
			yAdo6ScSmu4ui6bmE/WZmgf4TZXsn22BG+aU1r4SKXYQ3pVoS4QRBOSL2v2XIGEd
			NtORYb6w8jeLuQgW+1PP9JQGt41L1VB8u9Sfn35GMq928RA4jw4nKmKkDBX9uIp0
			ugM5o9tWkg/mnWLfwGLr+civZvQAWB4TpzR7EL949n10AZGFCkODK6W2tACVAHg+
			U6JExH3JeiH0CcRCYFNdUszTCWP3ttgRxcEYRKInplxr+iHhic3U2P5LHeoManCM
			oCfJJHA+moLGDI3JpSq73d2j8G1AMhPk8lk677IejwxNYtaVzDj45QymHqMrNjdG
			Phvi78mjs9RpdPANHlN6MgEg5fMrdmj18XBjRsZ/4ez2Ng==
			-----END CERTIFICATE-----
			""";

	static final X509Certificate T_CERT = assertDoesNotThrow(() -> CertificateUtils.getCertificate(T_CERT_PEM));

	
	@Test
	void testSave() {
		TransportProfileEntity tp = new TransportProfileEntity();
		tp.setId(new EmbeddedIdentifier("test-transport-profile-1"));
		em.persist(tp);
	
		EndpointEntity ep = new EndpointEntity();
		ep.setTransportProfile(tp);
		ep.setEndpointURL(assertDoesNotThrow(() -> new URL("http://test.holodeck-smp.org")));
		
		assertDoesNotThrow(() -> save(ep));
		
		EndpointEntity found = reload(ep);
		
		assertEquals(tp, found.getTransportProfile());
		assertEquals(ep.getEndpointURL(), found.getEndpointURL());
	}
	
	@Test
	void testRejectMissingProfile() {
		EndpointEntity ep = new EndpointEntity();
		ep.setEndpointURL(assertDoesNotThrow(() -> new URL("http://test.holodeck-smp.org")));
		
		assertThrows(Exception.class, () -> save(ep));
	}
	
	@Test
	void testAddCertificate() {
		TransportProfileEntity tp = new TransportProfileEntity();
		tp.setId(new EmbeddedIdentifier("test-transport-profile-1"));
		em.persist(tp);

		EndpointEntity ep = new EndpointEntity();
		ep.setTransportProfile(tp);
		ep.setEndpointURL(assertDoesNotThrow(() -> new URL("http://test.holodeck-b2b.org/as4/")));
		
		CertificateImpl cert1 = new CertificateImpl(T_CERT, "sign-and-encrypt");
		ep.addCertificate(cert1);
		CertificateImpl cert2 = new CertificateImpl(T_CERT, "encrypt");
		ep.addCertificate(cert2);
		
		assertDoesNotThrow(() -> save(ep));
		
		EndpointEntity found = reload(ep);

		assertEquals(2, found.getCertificates().size());
		assertTrue(found.getCertificates().parallelStream().anyMatch(sc -> cert1.equals(sc)));
		assertTrue(found.getCertificates().parallelStream().anyMatch(sc -> cert2.equals(sc)));
	}
	
	@Test
	void testRemoveCertificate() {
		TransportProfileEntity tp = new TransportProfileEntity();
		tp.setId(new EmbeddedIdentifier("test-transport-profile-1"));
		em.persist(tp);

		EndpointEntity ep = new EndpointEntity();
		ep.setTransportProfile(tp);
		ep.setEndpointURL(assertDoesNotThrow(() -> new URL("http://test.holodeck-b2b.org/as4/")));
		
		CertificateImpl cert1 = new CertificateImpl(T_CERT, "sign-and-encrypt");
		ep.addCertificate(cert1);
		CertificateImpl cert2 = new CertificateImpl(T_CERT, "encrypt");
		ep.addCertificate(cert2);
		
		assertDoesNotThrow(() -> save(ep));
		
		EndpointEntity found = reload(ep);
		
		assertEquals(2, found.getCertificates().size());		
		
		EmbeddedCertificate c2rm = (EmbeddedCertificate) found.getCertificates().get(0);
		found.removeCertificate(c2rm);
		
		assertDoesNotThrow(() -> save(found));
		
		EndpointEntity updated = reload(ep);		
		assertEquals(1, updated.getCertificates().size());
		assertFalse(found.getCertificates().contains(c2rm));
	}
}
