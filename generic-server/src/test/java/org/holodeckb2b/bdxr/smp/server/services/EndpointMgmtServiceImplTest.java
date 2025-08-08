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
package org.holodeckb2b.bdxr.smp.server.services;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.net.URL;
import java.security.cert.X509Certificate;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Collection;
import java.util.Set;

import org.holodeckb2b.bdxr.smp.server.CommonServerConfig;
import org.holodeckb2b.bdxr.smp.server.auditlogging.AuditLogRecord;
import org.holodeckb2b.bdxr.smp.server.auditlogging.AuditLogService;
import org.holodeckb2b.bdxr.smp.server.datamodel.Certificate;
import org.holodeckb2b.bdxr.smp.server.datamodel.Endpoint;
import org.holodeckb2b.bdxr.smp.server.db.entities.EmbeddedCertificate;
import org.holodeckb2b.bdxr.smp.server.db.entities.EmbeddedIdentifier;
import org.holodeckb2b.bdxr.smp.server.db.entities.EndpointEntity;
import org.holodeckb2b.bdxr.smp.server.db.entities.TransportProfileEntity;
import org.holodeckb2b.bdxr.smp.server.db.repos.EndpointRepository;
import org.holodeckb2b.bdxr.smp.server.db.repos.TransportProfileRepository;
import org.holodeckb2b.bdxr.smp.server.services.core.ConstraintViolationException;
import org.holodeckb2b.bdxr.smp.server.services.core.ConstraintViolationException.ViolationType;
import org.holodeckb2b.commons.security.CertificateUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest(classes = { CommonServerConfig.class })
class EndpointMgmtServiceImplTest {

	static final X509Certificate T_CERT = assertDoesNotThrow(() -> CertificateUtils.getCertificate(
			"""
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
			"""
			));
	
	@MockitoBean
	private AuditLogService 		auditService;
	
	@Autowired
	private EndpointRepository		endpoints;
	
	@Autowired
	private TransportProfileRepository profiles;
	
	@Autowired
	private EndpointMgmtServiceImpl epMgmtService;
	
	private static final TestUser T_USER = new TestUser();
	
	@BeforeEach
	void resetAuditLog() {
		reset(auditService);
		endpoints.deleteAll();
		profiles.deleteAll();
	}
	
	@Test
	void testAddEndpoint() {
		TransportProfileEntity tp = profiles.save(
										new TransportProfileEntity(new EmbeddedIdentifier("test-transport-profile-1")));
		
		Endpoint ep = mock(Endpoint.class);
		when(ep.getTransportProfile()).thenReturn(tp);
		when(ep.getEndpointURL()).thenReturn(assertDoesNotThrow(() -> new URL("http://test.holodeck-smp.org/")));
		when(ep.getName()).thenReturn("Test Endpoint");
		when(ep.getDescription()).thenReturn("A test endpoint for testing purposes");
		when(ep.getContactInfo()).thenReturn("http://help.test.holodeck-smp.org/");
		ZonedDateTime actDate = ZonedDateTime.now();
		when(ep.getServiceActivationDate()).thenReturn(actDate);
		ZonedDateTime expDate = ZonedDateTime.now();
		when(ep.getServiceExpirationDate()).thenReturn(expDate);
		
		Certificate c = new EmbeddedCertificate();
		c.setX509Cert(T_CERT);
		c.setUsage("test");
		c.setDescription("Test Certificate");
		c.setActivationDate(ZonedDateTime.from(T_CERT.getNotBefore().toInstant().atZone(ZoneOffset.UTC)).plusDays(1));
		c.setExpirationDate(ZonedDateTime.from(T_CERT.getNotAfter().toInstant().atZone(ZoneOffset.UTC)).minusDays(1));
		when(ep.getCertificates()).then(i -> Set.of(c));
		
		Endpoint saved = assertDoesNotThrow(() -> epMgmtService.addEndpoint(T_USER, ep));
		
		assertTrue(saved instanceof EndpointEntity);
		assertNotNull(saved.getId());
		assertEquals(ep.getTransportProfile(), saved.getTransportProfile());
		assertEquals(ep.getEndpointURL(), saved.getEndpointURL());
		assertEquals(ep.getName(), saved.getName());
		assertEquals(ep.getDescription(), saved.getDescription());
		assertEquals(ep.getContactInfo(), saved.getContactInfo());
		assertEquals(ep.getServiceActivationDate(), saved.getServiceActivationDate());
		assertEquals(ep.getServiceExpirationDate(), saved.getServiceExpirationDate());
		
		Collection<? extends org.holodeckb2b.bdxr.smp.datamodel.Certificate> savedCerts = saved.getCertificates();
		assertEquals(1, savedCerts.size());
		
		org.holodeckb2b.bdxr.smp.datamodel.Certificate cert = savedCerts.iterator().next();
		assertEquals(c.getX509Cert(), cert.getX509Cert());
		assertEquals(c.getUsage(), cert.getUsage());
		assertEquals(c.getDescription(), cert.getDescription());
		assertEquals(c.getActivationDate(), cert.getActivationDate());
		assertEquals(c.getExpirationDate(), cert.getExpirationDate());
		
		ArgumentCaptor<AuditLogRecord> captor = ArgumentCaptor.forClass(AuditLogRecord.class);
		verify(auditService).log(captor.capture());
		
		AuditLogRecord ar = captor.getValue();
		assertNotNull(ar.timestamp());
		assertEquals(T_USER.getUsername(), ar.username());
		assertEquals("Add Endpoint", ar.action());
		assertEquals(saved.getId().toString(), ar.subject());
	}

	@Test
	void testUpdateEndpoint() {
		TransportProfileEntity tp = profiles.save(
				new TransportProfileEntity(new EmbeddedIdentifier("test-transport-profile-1")));

		Endpoint ep = mock(Endpoint.class);
		when(ep.getTransportProfile()).thenReturn(tp);
		when(ep.getEndpointURL()).thenReturn(assertDoesNotThrow(() -> new URL("http://test.holodeck-smp.org/")));

		Endpoint saved = assertDoesNotThrow(() -> epMgmtService.addEndpoint(T_USER, ep));
		
		saved.setEndpointURL(assertDoesNotThrow(() -> new URL("http://test2.holodeck-smp.org/")));
		
		Endpoint updated = assertDoesNotThrow(() -> epMgmtService.updateEndpoint(T_USER, saved));
		
		assertEquals(saved.getEndpointURL(), updated.getEndpointURL());
		
		ArgumentCaptor<AuditLogRecord> captor = ArgumentCaptor.forClass(AuditLogRecord.class);
		verify(auditService, times(2)).log(captor.capture());
		
		AuditLogRecord ar = captor.getValue();
		assertNotNull(ar.timestamp());
		assertEquals(T_USER.getUsername(), ar.username());
		assertEquals("Update Endpoint", ar.action());
		assertEquals(saved.getId().toString(), ar.subject());
	}
	
	@Test
	void testDeleteEndpoint() {
		TransportProfileEntity tp = profiles.save(
				new TransportProfileEntity(new EmbeddedIdentifier("test-transport-profile-1")));

		Endpoint ep = mock(Endpoint.class);
		when(ep.getTransportProfile()).thenReturn(tp);
		
		Endpoint saved = assertDoesNotThrow(() -> epMgmtService.addEndpoint(T_USER, ep));
		
		assertDoesNotThrow(() -> epMgmtService.deleteEndpoint(T_USER, saved));
		
		ArgumentCaptor<AuditLogRecord> captor = ArgumentCaptor.forClass(AuditLogRecord.class);
		verify(auditService, times(2)).log(captor.capture());
		
		AuditLogRecord ar = captor.getValue();
		assertNotNull(ar.timestamp());
		assertEquals(T_USER.getUsername(), ar.username());
		assertEquals("Delete Endpoint", ar.action());
		assertEquals(saved.getId().toString(), ar.subject());
	}
	
	@Test	
	void testConstraints() {
		Endpoint ep = mock(Endpoint.class);
		when(ep.getTransportProfile()).thenReturn(null);
		
		ConstraintViolationException cve = 
				assertThrows(ConstraintViolationException.class, () -> epMgmtService.addEndpoint(T_USER, ep));		
		assertEquals(ViolationType.MISSING_MANDATORY_FIELD, cve.getViolation());
		assertEquals(ep, cve.getSubject());
		
		TransportProfileEntity tp = new TransportProfileEntity(new EmbeddedIdentifier("test-transport-profile-1"));

		when(ep.getTransportProfile()).thenReturn(tp);
		
		cve = assertThrows(ConstraintViolationException.class, () -> epMgmtService.addEndpoint(T_USER, ep));
		assertEquals(ViolationType.INVALID_FIELD, cve.getViolation());
		assertEquals(ep, cve.getSubject());

		when(ep.getServiceActivationDate()).thenReturn(ZonedDateTime.now().plusDays(1));
		when(ep.getServiceExpirationDate()).thenReturn(ZonedDateTime.now().minusDays(1));
		
		cve = assertThrows(ConstraintViolationException.class, () -> epMgmtService.addEndpoint(T_USER, ep));
		assertEquals(ViolationType.INVALID_FIELD, cve.getViolation());
		assertEquals(ep, cve.getSubject());
		
		Certificate c = new EmbeddedCertificate();
		c.setX509Cert(T_CERT);
		c.setActivationDate(ZonedDateTime.from(T_CERT.getNotBefore().toInstant().atZone(ZoneOffset.UTC)).plusDays(1));
		c.setExpirationDate(ZonedDateTime.from(T_CERT.getNotAfter().toInstant().atZone(ZoneOffset.UTC)).minusDays(1));
		when(ep.getCertificates()).then(i -> Set.of(c));
		
		cve = assertThrows(ConstraintViolationException.class, () -> epMgmtService.addEndpoint(T_USER, ep));
		assertEquals(ViolationType.INVALID_FIELD, cve.getViolation());
		assertEquals(ep, cve.getSubject());
		
		c.setActivationDate(ZonedDateTime.from(T_CERT.getNotAfter().toInstant().atZone(ZoneOffset.UTC)).plusDays(1));
		c.setExpirationDate(null);
		
		cve = assertThrows(ConstraintViolationException.class, () -> epMgmtService.addEndpoint(T_USER, ep));		
		assertEquals(ViolationType.INVALID_FIELD, cve.getViolation());
		assertEquals(ep, cve.getSubject());
		
		c.setActivationDate(null);
		c.setExpirationDate(ZonedDateTime.from(T_CERT.getNotBefore().toInstant().atZone(ZoneOffset.UTC)).minusDays(1));
	
		cve = assertThrows(ConstraintViolationException.class, () -> epMgmtService.addEndpoint(T_USER, ep));		
		assertEquals(ViolationType.INVALID_FIELD, cve.getViolation());
		assertEquals(ep, cve.getSubject());
		
		c.setActivationDate(ZonedDateTime.from(T_CERT.getNotBefore().toInstant().atZone(ZoneOffset.UTC)).plusDays(10));
		c.setExpirationDate(ZonedDateTime.from(T_CERT.getNotBefore().toInstant().atZone(ZoneOffset.UTC)).plusDays(5));

		assertEquals(ViolationType.INVALID_FIELD, cve.getViolation());
		assertEquals(ep, cve.getSubject());
	}
	
	@Test
	void testGetEndpoints() {
		TransportProfileEntity tp = profiles.save(
				new TransportProfileEntity(new EmbeddedIdentifier("test-transport-profile-1")));

		Endpoint ep = mock(Endpoint.class);
		when(ep.getTransportProfile()).thenReturn(tp);		
		for (int i = 0; i < 200; i++) 
			assertDoesNotThrow(() -> epMgmtService.addEndpoint(T_USER, ep));
		
		Page<EndpointEntity> p3 = assertDoesNotThrow(() -> epMgmtService.getEndpoints(PageRequest.of(2, 50)));
		
		assertEquals(4, p3.getTotalPages());
		assertEquals(2, p3.getNumber());
	}
	
	@Test
	void testCountAndFindByProfile() {
		TransportProfileEntity tp1 = profiles.save(
				new TransportProfileEntity(new EmbeddedIdentifier("test-transport-profile-1")));
		TransportProfileEntity tp2 = profiles.save(
				new TransportProfileEntity(new EmbeddedIdentifier("test-transport-profile-2")));

		Endpoint ep = mock(Endpoint.class);
		for (int i = 0; i < 200; i++) {
			when(ep.getTransportProfile()).thenReturn(i % 2 == 0 ? tp1 : tp2);
			assertDoesNotThrow(() -> epMgmtService.addEndpoint(T_USER, ep));
		}
		
		assertEquals(100, assertDoesNotThrow(() -> epMgmtService.countEndpointsUsingProfile(tp1)));
		assertEquals(100, assertDoesNotThrow(() -> epMgmtService.findEndpointsUsingProfile(tp1)).size());
		assertEquals(100, assertDoesNotThrow(() -> epMgmtService.countEndpointsUsingProfile(tp2)));
		assertEquals(100, assertDoesNotThrow(() -> epMgmtService.findEndpointsUsingProfile(tp2)).size());
	}
}
