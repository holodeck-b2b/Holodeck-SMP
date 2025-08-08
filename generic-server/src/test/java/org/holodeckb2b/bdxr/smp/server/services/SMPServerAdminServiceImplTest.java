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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.net.URL;
import java.security.KeyStore.PrivateKeyEntry;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.time.ZonedDateTime;
import java.util.UUID;

import org.holodeckb2b.bdxr.smp.datamodel.Certificate;
import org.holodeckb2b.bdxr.smp.server.CommonServerConfig;
import org.holodeckb2b.bdxr.smp.server.auditlogging.AuditLogRecord;
import org.holodeckb2b.bdxr.smp.server.auditlogging.AuditLogService;
import org.holodeckb2b.bdxr.smp.server.datamodel.SMPServerMetadata;
import org.holodeckb2b.bdxr.smp.server.db.entities.EmbeddedIdentifier;
import org.holodeckb2b.bdxr.smp.server.db.entities.ParticipantEntity;
import org.holodeckb2b.bdxr.smp.server.db.entities.ServerConfigEntity;
import org.holodeckb2b.bdxr.smp.server.db.repos.ParticipantRepository;
import org.holodeckb2b.bdxr.smp.server.db.repos.ServerConfigRepository;
import org.holodeckb2b.bdxr.smp.server.services.network.SMLException;
import org.holodeckb2b.bdxr.smp.server.utils.DataEncryptor;
import org.holodeckb2b.commons.security.CertificateUtils;
import org.holodeckb2b.commons.security.KeystoreUtils;
import org.holodeckb2b.commons.testing.TestUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest(classes = { CommonServerConfig.class })
class SMPServerAdminServiceImplTest {

	@MockitoBean
	private AuditLogService 		auditService;
	
	@Autowired
	private TestSMLIntegrator	smlIntegrator;
	
	@Autowired
	private TestDirectoryIntegrator	dirIntegrator;
	
	@Autowired
	private SMPServerAdminServiceImpl 	adminService;
	
	@Autowired
	private ParticipantRepository	participants;
	
	@Autowired
	private ServerConfigRepository 	configRepo;
	
	@Value("${smp.masterpwd}")
	protected String 		masterPwd;
	
	private static PrivateKeyEntry T_KEYPAIR_1;
	private static PrivateKeyEntry T_KEYPAIR_2;
	
	private static final TestUser T_USER = new TestUser();
	
	@BeforeAll
	static void setup() {
		try {
			T_KEYPAIR_1 = KeystoreUtils.readKeyPairFromPKCS12(TestUtils.getTestResource("testkey1.p12"), null);			
			T_KEYPAIR_2 = KeystoreUtils.readKeyPairFromPKCS12(TestUtils.getTestResource("testkey2.p12"), null);			
		} catch (Throwable t) {
			fail(t);
		}
	}
	
	@AfterEach
	void cleanup() {
		configRepo.deleteAll();
		participants.deleteAll();
		reset(auditService);
		smlIntegrator.reset();
	}
		
	@Test
	void testGetters() {
		ServerConfigEntity config = createTestConfig();
		
		config.setNextKeyPair(assertDoesNotThrow(() -> {
			DataEncryptor encryptor = new DataEncryptor(masterPwd);
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			KeystoreUtils.saveKeyPairToPKCS12(T_KEYPAIR_2, baos, null);
			return encryptor.encrypt(baos.toByteArray());
		}));
		config.setActivationDate(ZonedDateTime.now().plusDays(7));
		configRepo.save(config);
	
		SMPServerMetadata read = assertDoesNotThrow(() -> adminService.getServerMetadata());
		
		assertEquals(config.getSmpId(), read.getSMPId());
		assertEquals(config.getBaseUrl(), read.getBaseUrl());
		assertEquals(config.getIpv4Address(), read.getIPv4Address());
		assertEquals(config.getIpv6Address(), read.getIPv6Address());
		assertEquals(T_KEYPAIR_1.getCertificate(), read.getCertificate());
		assertNotNull(read.getPendingCertificateUpdate());
		assertTrue(config.getActivationDate().isEqual(read.getPendingCertificateUpdate().getActivationDate()));
		assertEquals(T_KEYPAIR_2.getCertificate(), read.getPendingCertificateUpdate().getX509Cert());
		
		// Because PrivateKeyEntry only compares objects in equals we need to verify the private key and cert separately
		PrivateKeyEntry kp = assertDoesNotThrow(() -> adminService.getActiveKeyPair());		
		assertEquals(T_KEYPAIR_1.getPrivateKey(), kp.getPrivateKey());
		assertEquals(T_KEYPAIR_1.getCertificate(), kp.getCertificate());			
	}
	
	@Test
	void testRegisterSML() {
		ServerConfigEntity config = createTestConfig();
		
		assertDoesNotThrow(() -> adminService.registerServerInSML(T_USER));
		
		assertTrue(configRepo.findById(config.getOid()).get().isRegisteredSML());
		
		SMPServerMetadata registered = smlIntegrator.smp;
		assertNotNull(registered);
		assertEquals(config.getSmpId(), registered.getSMPId());
		assertEquals(config.getBaseUrl(), registered.getBaseUrl());
		assertEquals(config.getIpv4Address(), registered.getIPv4Address());
		assertEquals(config.getIpv6Address(), registered.getIPv6Address());
		
		ArgumentCaptor<AuditLogRecord> captor = ArgumentCaptor.forClass(AuditLogRecord.class);
		verify(auditService).log(captor.capture());
		
		AuditLogRecord ar = captor.getValue();
		assertNotNull(ar.timestamp());
		assertEquals(T_USER.getUsername(), ar.username());
		assertEquals("Register in SML", ar.action());
		assertEquals("Server", ar.subject());
	}
	
	@Test
	void testRemoveFromSML() {
		ServerConfigEntity config = createTestConfig();
		config.setRegisteredSML(true);
		configRepo.save(config);
		
		smlIntegrator.requireSMPCertRegistration = true;
		smlIntegrator.smp = new SMPServerMetadataImpl(config.getSmpId(), config.getBaseUrl(), config.getIpv4Address(), 
											config.getIpv6Address(), null, null);
		
		assertDoesNotThrow(() -> adminService.removeServerFromSML(T_USER));
		
		assertFalse(configRepo.findById(config.getOid()).get().isRegisteredSML());		
		assertNull(smlIntegrator.smp);
		
		ArgumentCaptor<AuditLogRecord> captor = ArgumentCaptor.forClass(AuditLogRecord.class);
		verify(auditService).log(captor.capture());
		
		AuditLogRecord ar = captor.getValue();
		assertNotNull(ar.timestamp());
		assertEquals(T_USER.getUsername(), ar.username());
		assertEquals("Remove from SML", ar.action());
		assertEquals("Server", ar.subject());
	}
	
	@Test
	void testRejectRemoveFromSML() {
		ServerConfigEntity config = createTestConfig();
		config.setRegisteredSML(true);
		configRepo.save(config);
		
		dirIntegrator.requireSMLRegistration = true;
		
		smlIntegrator.smp = new SMPServerMetadataImpl(config.getSmpId(), 
									config.getBaseUrl(), config.getIpv4Address(), config.getIpv6Address(), null, null);
		
		ParticipantEntity p = new ParticipantEntity();
		p.setId(new EmbeddedIdentifier(UUID.randomUUID().toString()));
		p.setRegisteredInSML(true);
		p.setPublishedInDirectory(true);
		participants.save(p);
		
		assertThrows(SMLException.class, () -> adminService.removeServerFromSML(T_USER));
		
		assertTrue(configRepo.findById(config.getOid()).get().isRegisteredSML());		
		assertNotNull(smlIntegrator.smp);
		
		verify(auditService, never()).log(any(AuditLogRecord.class));				
	}
	
	@Test
	void testRegisterCert() {
		ServerConfigEntity config = createTestConfig();		
		config.setRegisteredSML(true);
		configRepo.save(config);
		smlIntegrator.requireSMPCertRegistration = true;
		
		assertDoesNotThrow(() -> adminService.registerCertificate(T_USER, T_KEYPAIR_2));
		
		ServerConfigEntity updated = configRepo.findById(config.getOid()).get();
		
		assertEquals(T_KEYPAIR_2.getCertificate(), decrypt(updated.getCurrentKeyPair()).getCertificate());
		assertNull(updated.getActivationDate());
		assertNull(updated.getNextKeyPair());
		
		Certificate registered = smlIntegrator.cert;
		
		assertNotNull(registered);
		assertNull(registered.getActivationDate());		
		assertEquals(T_KEYPAIR_2.getCertificate(), registered.getX509Cert());
		
		ArgumentCaptor<AuditLogRecord> captor = ArgumentCaptor.forClass(AuditLogRecord.class);
		verify(auditService).log(captor.capture());
		
		AuditLogRecord ar = captor.getValue();
		assertNotNull(ar.timestamp());
		assertEquals(T_USER.getUsername(), ar.username());
		assertEquals("Update certificate", ar.action());
		assertEquals("Server", ar.subject());
		assertTrue(ar.details().contains(CertificateUtils.getSubjectName((X509Certificate) T_KEYPAIR_2.getCertificate())));
	}
	
	@Test
	void testRegisterCertScheduled() {
		ServerConfigEntity config = createTestConfig();		
		config.setRegisteredSML(true);
		configRepo.save(config);
		smlIntegrator.requireSMPCertRegistration = true;
		
		ZonedDateTime tommorrow = ZonedDateTime.now().plusDays(1);
		
		assertDoesNotThrow(() -> adminService.registerCertificate(T_USER, T_KEYPAIR_2, tommorrow));
		
		ServerConfigEntity updated = configRepo.findById(config.getOid()).get();
		
		assertEquals(T_KEYPAIR_1.getCertificate(), decrypt(updated.getCurrentKeyPair()).getCertificate());
		assertTrue(tommorrow.isEqual(updated.getActivationDate()));
		assertEquals(T_KEYPAIR_2.getCertificate(), decrypt(updated.getNextKeyPair()).getCertificate());
		
		Certificate registered = smlIntegrator.cert;
		
		assertNotNull(registered);
		assertTrue(tommorrow.isEqual(registered.getActivationDate()));		
		assertEquals(T_KEYPAIR_2.getCertificate(), registered.getX509Cert());
		
		ArgumentCaptor<AuditLogRecord> captor = ArgumentCaptor.forClass(AuditLogRecord.class);
		verify(auditService).log(captor.capture());
		
		AuditLogRecord ar = captor.getValue();
		assertNotNull(ar.timestamp());
		assertEquals(T_USER.getUsername(), ar.username());
		assertEquals("Update certificate", ar.action());
		assertEquals("Server", ar.subject());
		assertTrue(ar.details().contains(CertificateUtils.getSubjectName((X509Certificate) T_KEYPAIR_2.getCertificate())));
	}

	@Test
	void testCertSwitch() {
		ServerConfigEntity config = createTestConfig();		
		config.setNextKeyPair(assertDoesNotThrow(() -> {
			DataEncryptor encryptor = new DataEncryptor(masterPwd);
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			KeystoreUtils.saveKeyPairToPKCS12(T_KEYPAIR_2, baos, null);
			return encryptor.encrypt(baos.toByteArray());
		}));		
		config.setActivationDate(ZonedDateTime.now().minusHours(4));		
		configRepo.save(config);
	
		SMPServerMetadata read = assertDoesNotThrow(() -> adminService.getServerMetadata());

		assertEquals(T_KEYPAIR_2.getCertificate(), read.getCertificate());
		assertNull(read.getPendingCertificateUpdate());		
	}
	
	@Test
	void testRegisterCertWithoutSML() {
		ServerConfigEntity config = createTestConfig();		
		config.setRegisteredSML(true);
		configRepo.save(config);
		
		assertDoesNotThrow(() -> adminService.registerCertificate(T_USER, T_KEYPAIR_2));
		
		assertNull(smlIntegrator.cert);
	}
	
	@Test
	void testRemoveCert() {
		ServerConfigEntity config = createTestConfig();		
		config.setRegisteredSML(true);
		configRepo.save(config);
		
		assertDoesNotThrow(() -> adminService.removeCertificate(T_USER));
		
		ServerConfigEntity updated = configRepo.findById(config.getOid()).get();
		
		assertNull(updated.getCurrentKeyPair());
		assertNull(updated.getActivationDate());
		assertNull(updated.getNextKeyPair());
		
		ArgumentCaptor<AuditLogRecord> captor = ArgumentCaptor.forClass(AuditLogRecord.class);
		verify(auditService).log(captor.capture());
		
		AuditLogRecord ar = captor.getValue();
		assertNotNull(ar.timestamp());
		assertEquals(T_USER.getUsername(), ar.username());
		assertEquals("Remove certificate", ar.action());
		assertEquals("Server", ar.subject());
	}
	
	@Test
	void testRejectRemoval() {
		ServerConfigEntity config = createTestConfig();		
		config.setRegisteredSML(true);
		configRepo.save(config);
		smlIntegrator.requireSMPCertRegistration = true;
		smlIntegrator.smp = new SMPServerMetadataImpl(config.getSmpId(), config.getBaseUrl(), config.getIpv4Address(), 
											config.getIpv6Address(), null, null);
		
		assertThrows(CertificateException.class, () -> adminService.removeCertificate(T_USER));
		
		assertNotNull(smlIntegrator.smp);
		
		ServerConfigEntity stored = configRepo.findById(config.getOid()).get();
		
		assertNotNull(stored.getCurrentKeyPair());		
		verify(auditService, never()).log(any(AuditLogRecord.class));
	}	
	
	private ServerConfigEntity createTestConfig() {
		ServerConfigEntity config = new ServerConfigEntity();
		config.setSmpId(UUID.randomUUID().toString());
		config.setBaseUrl(assertDoesNotThrow(() -> new URL("https://test.smp.holodeck-b2b.org")));
		config.setIpv4Address("127.0.0.1");
		config.setIpv6Address("::1");		
		
		assertDoesNotThrow(() -> {
			DataEncryptor encryptor = new DataEncryptor(masterPwd);
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			KeystoreUtils.saveKeyPairToPKCS12(T_KEYPAIR_1, baos, null);
			config.setCurrentKeyPair(encryptor.encrypt(baos.toByteArray()));
			configRepo.save(config);
		});
		
		return config;
	}		
	
	private PrivateKeyEntry decrypt(byte[] encryptedKeyPair) {
		if (encryptedKeyPair == null)
			return null;
		
		return assertDoesNotThrow(() ->{
			DataEncryptor encryptor = new DataEncryptor(masterPwd);
			return KeystoreUtils.readKeyPairFromKeystore(
												new ByteArrayInputStream(encryptor.decrypt(encryptedKeyPair)), null);			
		});		
	}	
}
