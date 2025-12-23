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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atMostOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.net.URL;
import java.time.LocalDate;
import java.util.Collection;
import java.util.Objects;
import java.util.Set;

import org.holodeckb2b.bdxr.smp.server.CommonServerConfig;
import org.holodeckb2b.bdxr.smp.server.auditlogging.AuditLogRecord;
import org.holodeckb2b.bdxr.smp.server.auditlogging.AuditLogService;
import org.holodeckb2b.bdxr.smp.server.datamodel.Contact;
import org.holodeckb2b.bdxr.smp.server.datamodel.Participant;
import org.holodeckb2b.bdxr.smp.server.db.entities.EmbeddedIdentifier;
import org.holodeckb2b.bdxr.smp.server.db.entities.IDSchemeEntity;
import org.holodeckb2b.bdxr.smp.server.db.entities.ParticipantEntity;
import org.holodeckb2b.bdxr.smp.server.db.entities.ServiceEntity;
import org.holodeckb2b.bdxr.smp.server.db.entities.ServiceMetadataTemplateEntity;
import org.holodeckb2b.bdxr.smp.server.db.repos.IDSchemeRepository;
import org.holodeckb2b.bdxr.smp.server.db.repos.ParticipantRepository;
import org.holodeckb2b.bdxr.smp.server.db.repos.ServiceMetadataTemplateRepository;
import org.holodeckb2b.bdxr.smp.server.db.repos.ServiceRepository;
import org.holodeckb2b.bdxr.smp.server.services.core.ConstraintViolationException;
import org.holodeckb2b.bdxr.smp.server.services.core.ConstraintViolationException.ViolationType;
import org.holodeckb2b.bdxr.smp.server.services.core.PersistenceException;
import org.holodeckb2b.bdxr.smp.server.services.network.SMLException;
import org.holodeckb2b.commons.util.Utils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest(classes = { CommonServerConfig.class })
class ParticipantsServiceImplTest {

	@MockitoBean
	private AuditLogService 		auditService;
	
	@Autowired
	private ParticipantRepository	participants;
	
	@Autowired
	private ServiceRepository		services;

	@Autowired
	private ServiceMetadataTemplateRepository smtRepo;
	
	@Autowired
	private IDSchemeRepository		idschemes;
	
	@Autowired
	private TestSMLIntegrator smlIntegrator;

	@Autowired
	private TestDirectoryIntegrator dirIntegrator;
	
	@Autowired
	private ParticipantsServiceImpl partMgmtService;
		
	private static final TestUser T_USER = new TestUser();
	
	@BeforeEach
	void addIDSchemes() {
		reset(auditService);
		participants.deleteAll();
		smtRepo.deleteAll();
		services.deleteAll();
		idschemes.deleteAll();
		smlIntegrator.reset();
		dirIntegrator.reset();
	}
		
	@Test
	void testAddParticipant() {
		IDSchemeEntity ids1 = idschemes.save(new IDSchemeEntity("TestIDScheme", false));
		IDSchemeEntity ids2 = idschemes.save(new IDSchemeEntity("TestIDScheme2", true));
		
		Participant p = mock(Participant.class);
		when(p.getId()).thenReturn(new EmbeddedIdentifier(ids1, "9999:test1"));
		when(p.getName()).thenReturn("Test Participant");
		when(p.getLocationInfo()).thenReturn("Dev Environment");
		when(p.getRegistrationCountry()).thenReturn("NL");
		LocalDate regDate = LocalDate.now();
		when(p.getFirstRegistrationDate()).thenReturn(regDate);
		when(p.getWebsites()).thenReturn(Set.of(assertDoesNotThrow(() -> new URL("http://test.holodeck-smp.org"))));
		Contact contact = mock(Contact.class);
		when(contact.getName()).thenReturn("John Doe");
		when(contact.getJobTitle()).thenReturn("Test Director");
		when(contact.getDepartment()).thenReturn("Quality Management");
		when(contact.getEmailAddress()).thenReturn("jdoe@test.business");
		when(contact.getTelephone()).thenReturn("+311234567890");
		when(p.getContactInfo()).thenReturn(Set.of(contact));		
		when(p.getAdditionalIds()).thenReturn(Set.of(new EmbeddedIdentifier(ids2, "99xx:test2")));
		
		Participant saved = assertDoesNotThrow(() -> partMgmtService.addParticipant(T_USER, p));
		
		assertTrue(saved instanceof ParticipantEntity);
		assertEquals(p.getId(), saved.getId());
		assertEquals(p.getName(), saved.getName());
		assertEquals(p.getLocationInfo(), saved.getLocationInfo());
		assertEquals(p.getRegistrationCountry(), saved.getRegistrationCountry());
		assertEquals(p.getFirstRegistrationDate(), saved.getFirstRegistrationDate());
		assertTrue(Utils.areEqual(p.getWebsites(), saved.getWebsites()));
		
		Contact savedContact = saved.getContactInfo() != null ? saved.getContactInfo().iterator().next() : null;
		assertNotNull(savedContact);
		assertEquals(contact.getName(), savedContact.getName());
		assertEquals(contact.getJobTitle(), savedContact.getJobTitle());
		assertEquals(contact.getDepartment(), savedContact.getDepartment());
		assertEquals(contact.getEmailAddress(), savedContact.getEmailAddress());
		assertEquals(contact.getTelephone(), savedContact.getTelephone());
		
		assertTrue(p.getAdditionalIds().parallelStream().allMatch(ai -> 
									saved.getAdditionalIds().parallelStream().anyMatch(savedAi -> ai.equals(savedAi))));
		
		ArgumentCaptor<AuditLogRecord> captor = ArgumentCaptor.forClass(AuditLogRecord.class);
		verify(auditService).log(captor.capture());
		
		AuditLogRecord ar = captor.getValue();
		assertNotNull(ar.timestamp());
		assertEquals(T_USER.getUsername(), ar.username());
		assertEquals("Add Participant", ar.action());
		assertEquals(saved.getId().toString(), ar.subject());
	}

	@Test
	void testRejectDuplicateID() {	
		IDSchemeEntity ids1 = idschemes.save(new IDSchemeEntity("TestIDScheme", false));
				
		Participant p1 = mock(Participant.class);
		when(p1.getId()).thenReturn(new EmbeddedIdentifier(ids1, "9999:test1"));		
		Participant p2 = mock(Participant.class);
		when(p2.getId()).thenReturn(new EmbeddedIdentifier(ids1, "9999:TEST1"));
		
		assertDoesNotThrow(() -> partMgmtService.addParticipant(T_USER, p1));
		ConstraintViolationException cve = assertThrows(ConstraintViolationException.class, 
														() -> partMgmtService.addParticipant(T_USER, p2));
		
		assertEquals(ViolationType.DUPLICATE_ID, cve.getViolation());
		assertEquals(p2.getId().toString(), cve.getSubject().getId().toString());
		
		verify(auditService, atMostOnce()).log(any(AuditLogRecord.class));
	}
	
	@Test
	void testUpdateParticipant() {
		IDSchemeEntity ids1 = idschemes.save(new IDSchemeEntity("TestIDScheme", false));
		IDSchemeEntity ids2 = idschemes.save(new IDSchemeEntity("TestIDScheme2", true));
		
		ServiceEntity svc = new ServiceEntity();
		svc.setId(new EmbeddedIdentifier("SvcId-T-1"));
		svc = services.save(svc);				
		ServiceMetadataTemplateEntity smt = new ServiceMetadataTemplateEntity();
		smt.setService(svc);				
		final ServiceMetadataTemplateEntity storedSMT = smtRepo.save(smt);

		Participant p = mock(Participant.class);
		when(p.getId()).thenReturn(new EmbeddedIdentifier(ids1, "9999:test1"));
		when(p.getName()).thenReturn("Test Participant");

		Participant saved = assertDoesNotThrow(() -> 
							 partMgmtService.bindToSMT(T_USER, partMgmtService.addParticipant(T_USER, p), storedSMT));
		
		saved.setId(new EmbeddedIdentifier(ids1, "9999:test2"));
		saved.setName("Test Participant 2");
		saved.setLocationInfo("Dev Environment");
		saved.setRegistrationCountry("NL");
		saved.setFirstRegistrationDate(LocalDate.now());
		saved.addWebsite(assertDoesNotThrow(() -> new URL("http://test.holodeck-smp.org")));
		Contact contact = mock(Contact.class);		
		when(contact.getName()).thenReturn("John Doe");
		when(contact.getJobTitle()).thenReturn("Test Director");
		when(contact.getDepartment()).thenReturn("Quality Management");
		when(contact.getEmailAddress()).thenReturn("jdoe@test.business");
		when(contact.getTelephone()).thenReturn("+311234567890");
		saved.addContactInfo(contact);		
		saved.addAdditionalId(new EmbeddedIdentifier(ids2, "99xx:test2"));
		
		Participant updated = assertDoesNotThrow(() -> partMgmtService.updateParticipant(T_USER, saved));
		
		assertEquals(saved.getId(), updated.getId());
		assertEquals(saved.getName(), updated.getName());
		assertEquals(saved.getLocationInfo(), updated.getLocationInfo());
		assertEquals(saved.getRegistrationCountry(), updated.getRegistrationCountry());
		assertEquals(saved.getFirstRegistrationDate(), updated.getFirstRegistrationDate());
		assertTrue(Utils.areEqual(saved.getWebsites(), updated.getWebsites()));
		
		Contact savedContact = updated.getContactInfo() != null ? updated.getContactInfo().iterator().next() : null;
		assertNotNull(updated);
		assertEquals(contact.getName(), savedContact.getName());
		assertEquals(contact.getJobTitle(), savedContact.getJobTitle());
		assertEquals(contact.getDepartment(), savedContact.getDepartment());
		assertEquals(contact.getEmailAddress(), savedContact.getEmailAddress());
		assertEquals(contact.getTelephone(), savedContact.getTelephone());
		
		assertTrue(Utils.areEqual(saved.getAdditionalIds(), updated.getAdditionalIds()));
		assertTrue(Utils.areEqual(saved.getBoundSMT(), updated.getBoundSMT()));
		
		ArgumentCaptor<AuditLogRecord> captor = ArgumentCaptor.forClass(AuditLogRecord.class);
		verify(auditService, times(3)).log(captor.capture());
		
		AuditLogRecord ar = captor.getValue();
		assertNotNull(ar.timestamp());
		assertEquals(T_USER.getUsername(), ar.username());
		assertEquals("Update Participant", ar.action());
		assertEquals(updated.getId().toString(), ar.subject());
	}	

	@Test
	void testRejectBindingsUpdateParticipant() {
		ServiceEntity svc = new ServiceEntity();
		svc.setId(new EmbeddedIdentifier("SvcId-T-1"));
		svc = services.save(svc);				
		ServiceMetadataTemplateEntity smt = new ServiceMetadataTemplateEntity();
		smt.setService(svc);				
		final ServiceMetadataTemplateEntity storedSMT = smtRepo.save(smt);
		
		IDSchemeEntity ids1 = idschemes.save(new IDSchemeEntity("TestIDScheme", false));		
		Participant p = mock(Participant.class);
		when(p.getId()).thenReturn(new EmbeddedIdentifier(ids1, "9999:test1"));
		when(p.getName()).thenReturn("Test Participant");

		Participant saved = assertDoesNotThrow(() -> partMgmtService.addParticipant(T_USER, p));
		((ParticipantEntity) saved).addBinding(storedSMT);
		
		assertThrows(PersistenceException.class, () -> partMgmtService.updateParticipant(T_USER, saved));
		
		assertTrue(participants.findById(((ParticipantEntity) saved).getOid()).get().getBoundSMT().isEmpty());		
	}
	
	
	@Test
	void testDeleteParticipant() {
		IDSchemeEntity ids1 = idschemes.save(new IDSchemeEntity("TestIDScheme", false));
		
		Participant p = mock(Participant.class);
		when(p.getId()).thenReturn(new EmbeddedIdentifier(ids1, "9999:test1"));
		when(p.getName()).thenReturn("Test Participant");
		
		Participant saved = assertDoesNotThrow(() -> partMgmtService.addParticipant(T_USER, p));
		
		assertDoesNotThrow(() -> partMgmtService.deleteParticipant(T_USER, saved));
		
		ArgumentCaptor<AuditLogRecord> captor = ArgumentCaptor.forClass(AuditLogRecord.class);
		verify(auditService, times(2)).log(captor.capture());
		
		AuditLogRecord ar = captor.getValue();
		assertNotNull(ar.timestamp());
		assertEquals(T_USER.getUsername(), ar.username());
		assertEquals("Delete Participant", ar.action());
		assertEquals(saved.getId().toString(), ar.subject());
	}
	
	@Test
	void testSMLRegistration() {
		IDSchemeEntity ids1 = idschemes.save(new IDSchemeEntity("TestIDScheme", false));

		Participant p = mock(Participant.class);
		when(p.getId()).thenReturn(new EmbeddedIdentifier(ids1, "9999:test1"));
		when(p.getName()).thenReturn("Test Participant");

		Participant saved = assertDoesNotThrow(() -> partMgmtService.addParticipant(T_USER, p));
		Participant registered = assertDoesNotThrow(() -> partMgmtService.registerInSML(T_USER, saved));
		
		assertTrue(registered.isRegisteredInSML());
		assertTrue(assertDoesNotThrow(() -> smlIntegrator.isRegistered(registered)));
		
		ArgumentCaptor<AuditLogRecord> captor = ArgumentCaptor.forClass(AuditLogRecord.class);
		verify(auditService, times(2)).log(captor.capture());
		AuditLogRecord ar = captor.getValue();
		assertNotNull(ar.timestamp());
		assertEquals(T_USER.getUsername(), ar.username());
		assertEquals("Register in SML", ar.action());
		assertEquals(saved.getId().toString(), ar.subject());				

		Participant removed = assertDoesNotThrow(() -> partMgmtService.removeFromSML(T_USER, registered));
		
		assertFalse(removed.isRegisteredInSML());
		assertFalse(assertDoesNotThrow(() -> smlIntegrator.isRegistered(removed)));
				
		verify(auditService, times(3)).log(captor.capture());
		ar = captor.getValue();
		assertNotNull(ar.timestamp());
		assertEquals(T_USER.getUsername(), ar.username());
		assertEquals("Remove from SML", ar.action());
		assertEquals(saved.getId().toString(), ar.subject());				
	}
	
	@Test
	void testSMLMigration() {
		IDSchemeEntity ids1 = idschemes.save(new IDSchemeEntity("TestIDScheme", false));

		Participant p = mock(Participant.class);
		when(p.getId()).thenReturn(new EmbeddedIdentifier(ids1, "9999:test1"));
		when(p.getName()).thenReturn("Test Participant");

		Participant saved = assertDoesNotThrow(() -> partMgmtService.addParticipant(T_USER, p));
		
		assertDoesNotThrow(() -> smlIntegrator.registerParticipant(saved));
		final String migrationCode = "MIGRATIONCODE";
		assertDoesNotThrow(() -> smlIntegrator.registerMigrationCode(p, migrationCode));
		
		// Migration to SMP
		//
		Participant migrated = assertDoesNotThrow(() -> partMgmtService.migrateInSML(T_USER, saved, migrationCode));

		assertTrue(migrated.isRegisteredInSML());		
		assertNull(migrated.getSMLMigrationCode());
		
		ArgumentCaptor<AuditLogRecord> captor = ArgumentCaptor.forClass(AuditLogRecord.class);
		verify(auditService, times(2)).log(captor.capture());
		AuditLogRecord ar = captor.getValue();
		assertNotNull(ar.timestamp());
		assertEquals(T_USER.getUsername(), ar.username());
		assertEquals("Migrate in SML", ar.action());
		assertEquals(saved.getId().toString(), ar.subject());
		assertTrue(ar.details().contains(migrationCode));
		
		// Prepare for migration to SML (generate migration code)
		//
		Participant prepared = assertDoesNotThrow(() -> partMgmtService.prepareForSMLMigration(T_USER, migrated));
		
		assertNotNull(prepared.getSMLMigrationCode());
		assertEquals(prepared.getSMLMigrationCode(), smlIntegrator.migrations.get(prepared.getId()));
		
		verify(auditService, times(3)).log(captor.capture());
		ar = captor.getValue();
		assertNotNull(ar.timestamp());
		assertEquals(T_USER.getUsername(), ar.username());
		assertEquals("Prepare SML migration", ar.action());
		assertEquals(saved.getId().toString(), ar.subject());
		assertTrue(ar.details().contains(prepared.getSMLMigrationCode()));
		
		// Cancel migration
		//
		Participant cancelled = assertDoesNotThrow(() -> partMgmtService.cancelSMLMigration(T_USER, prepared));
		
		assertNull(cancelled.getSMLMigrationCode());
		assertFalse(smlIntegrator.migrations.containsKey(cancelled.getId()));
		
		verify(auditService, times(4)).log(captor.capture());
		ar = captor.getValue();
		assertNotNull(ar.timestamp());
		assertEquals(T_USER.getUsername(), ar.username());
		assertEquals("Cancel SML migration", ar.action());
		assertEquals(saved.getId().toString(), ar.subject());
		
		// Prepare for migration to SML (provide migration code)
		//
		Participant prepared2 = assertDoesNotThrow(() -> 
											partMgmtService.prepareForSMLMigration(T_USER, migrated, migrationCode));
				
		assertEquals(migrationCode, prepared2.getSMLMigrationCode());
		assertEquals(migrationCode, smlIntegrator.migrations.get(prepared.getId()));			
	}
	
	@Test
	void testSMLRegistrationFailure() {
		IDSchemeEntity ids1 = idschemes.save(new IDSchemeEntity("TestIDScheme", false));
		Participant p = mock(Participant.class);
		when(p.getId()).thenReturn(new EmbeddedIdentifier(ids1, "9999:test1"));
		when(p.getName()).thenReturn("Test Participant");
		
		Participant saved = assertDoesNotThrow(() -> partMgmtService.addParticipant(T_USER, p));
		
		reset(auditService);
		
		SMLException smlException = new SMLException("Fail");
		smlIntegrator.rejectNextWith(smlException);

		SMLException failure = assertThrows(SMLException.class, () -> partMgmtService.registerInSML(T_USER, saved));
		
		assertEquals(smlException, failure);
		
		ParticipantEntity current = participants.findById(((ParticipantEntity) saved).getOid()).orElse(null);
		
		assertFalse(current.isRegisteredInSML());
		assertFalse(assertDoesNotThrow(() -> smlIntegrator.isRegistered(current)));
		
		verify(auditService, never()).log(any(AuditLogRecord.class));			
	}
	
	@Test
	void testSMLMigrationFailure() {
		IDSchemeEntity ids1 = idschemes.save(new IDSchemeEntity("TestIDScheme", false));
		Participant p = mock(Participant.class);
		when(p.getId()).thenReturn(new EmbeddedIdentifier(ids1, "9999:test1"));
		when(p.getName()).thenReturn("Test Participant");
		Participant saved = assertDoesNotThrow(() -> partMgmtService.addParticipant(T_USER, p));
		
		assertDoesNotThrow(() -> smlIntegrator.registerParticipant(saved));
		final String migrationCode = "MIGRATIONCODE";
		assertDoesNotThrow(() -> smlIntegrator.registerMigrationCode(p, migrationCode));
		
		reset(auditService);
		
		SMLException smlException = new SMLException("Fail");		
		smlIntegrator.rejectNextWith(smlException);

		SMLException failure = assertThrows(SMLException.class, () -> 
												partMgmtService.migrateInSML(T_USER, saved, migrationCode));
		
		assertEquals(smlException, failure);
		
		ParticipantEntity current = participants.findById(((ParticipantEntity) saved).getOid()).orElse(null);
		
		assertFalse(current.isRegisteredInSML());	
		
		verify(auditService, never()).log(any(AuditLogRecord.class));
	}
	
	@Test
	void testSMLPrepareMigrationFailure() {
		IDSchemeEntity ids1 = idschemes.save(new IDSchemeEntity("TestIDScheme", false));
		Participant p = mock(Participant.class);
		when(p.getId()).thenReturn(new EmbeddedIdentifier(ids1, "9999:test1"));
		when(p.getName()).thenReturn("Test Participant");
		Participant saved = assertDoesNotThrow(() -> partMgmtService.addParticipant(T_USER, p));
		
		assertDoesNotThrow(() -> partMgmtService.registerInSML(T_USER, saved));
		
		reset(auditService);

		SMLException smlException = new SMLException("Fail");		
		smlIntegrator.rejectNextWith(smlException);

		SMLException failure = assertThrows(SMLException.class, () -> 
												partMgmtService.prepareForSMLMigration(T_USER, saved, "MIGRATIONCODE"));
		
		assertEquals(smlException, failure);
		assertNull(saved.getSMLMigrationCode());
		
		ParticipantEntity current = participants.findById(((ParticipantEntity) saved).getOid()).orElse(null);
		
		assertNull(current.getSMLMigrationCode());		
		
		verify(auditService, never()).log(any(AuditLogRecord.class));
	}
	
	@Test
	void testSMLCancelMigrationFailure() {
		IDSchemeEntity ids1 = idschemes.save(new IDSchemeEntity("TestIDScheme", false));
		Participant p = mock(Participant.class);
		when(p.getId()).thenReturn(new EmbeddedIdentifier(ids1, "9999:test1"));
		when(p.getName()).thenReturn("Test Participant");
		Participant saved = assertDoesNotThrow(() -> partMgmtService.addParticipant(T_USER, p));
		
		assertDoesNotThrow(() -> partMgmtService.registerInSML(T_USER, saved));
		final String migrationCode = "MIGRATIONCODE";
		assertDoesNotThrow(() -> partMgmtService.prepareForSMLMigration(T_USER, saved, migrationCode));
		
		reset(auditService);
		
		SMLException smlException = new SMLException("Fail");		
		smlIntegrator.rejectNextWith(smlException);
		
		SMLException failure = assertThrows(SMLException.class, () -> partMgmtService.cancelSMLMigration(T_USER, saved));
		
		assertEquals(smlException, failure);
		
		ParticipantEntity current = participants.findById(((ParticipantEntity) saved).getOid()).orElse(null);
		
		assertEquals(migrationCode, current.getSMLMigrationCode());		
		
		verify(auditService, never()).log(any(AuditLogRecord.class));
	}
	
	@Test
	void testDirectoryActions() {
		IDSchemeEntity ids1 = idschemes.save(new IDSchemeEntity("TestIDScheme", false));

		Participant p = mock(Participant.class);
		when(p.getId()).thenReturn(new EmbeddedIdentifier(ids1, "9999:test1"));
		when(p.getName()).thenReturn("Test Participant");

		Participant saved = assertDoesNotThrow(() -> partMgmtService.addParticipant(T_USER, p));
		
		Participant published = assertDoesNotThrow(() -> partMgmtService.publishInDirectory(T_USER, saved));
		
		assertTrue(published.isPublishedInDirectory());
		assertTrue(dirIntegrator.publications.contains(saved.getId()));
		
		ArgumentCaptor<AuditLogRecord> captor = ArgumentCaptor.forClass(AuditLogRecord.class);
		assertDoesNotThrow(() -> verify(auditService, times(2)).log(captor.capture()));
		AuditLogRecord ar = captor.getValue();
		assertNotNull(ar.timestamp());
		assertEquals(T_USER.getUsername(), ar.username());
		assertEquals("Publish in directory", ar.action());
		assertEquals(saved.getId().toString(), ar.subject());
		
		Participant removed = assertDoesNotThrow(() -> partMgmtService.removeFromDirectory(T_USER, published));
		
		assertFalse(removed.isPublishedInDirectory());
		assertFalse(dirIntegrator.publications.contains(saved.getId()));
		
		verify(auditService, times(3)).log(captor.capture());
		ar = captor.getValue();
		assertNotNull(ar.timestamp());
		assertEquals(T_USER.getUsername(), ar.username());
		assertEquals("Remove from directory", ar.action());
		assertEquals(saved.getId().toString(), ar.subject());
	}
	
	@Test
	void testRejectIDChangeWhenInSML() {
		IDSchemeEntity ids1 = idschemes.save(new IDSchemeEntity("TestIDScheme", false));
		
		Participant p = mock(Participant.class);
		when(p.getId()).thenReturn(new EmbeddedIdentifier(ids1, "9999:test1"));
		when(p.getName()).thenReturn("Test Participant");

		Participant saved = assertDoesNotThrow(() -> partMgmtService.addParticipant(T_USER, p));
		assertDoesNotThrow(() -> partMgmtService.registerInSML(T_USER, saved));
		
		saved.setId(new EmbeddedIdentifier(ids1, "9999:test2"));
		
		assertThrows(PersistenceException.class, () -> partMgmtService.updateParticipant(T_USER, saved));		
	}
	
	@Test
	void testNotifyDirectory() {
		IDSchemeEntity ids1 = idschemes.save(new IDSchemeEntity("TestIDScheme", false));
		
		Participant p = mock(Participant.class);
		when(p.getId()).thenReturn(new EmbeddedIdentifier(ids1, "9999:test1"));
		when(p.getName()).thenReturn("Test Participant");

		Participant saved = assertDoesNotThrow(() -> partMgmtService.addParticipant(T_USER, p));
		assertDoesNotThrow(() -> partMgmtService.publishInDirectory(T_USER, saved));
		
		saved.setName("The new name");
		
		Participant updated = assertDoesNotThrow(() -> partMgmtService.updateParticipant(T_USER, saved));
		
		assertTrue(updated.isPublishedInDirectory());		
		assertEquals(2, dirIntegrator.publications.size());
	}
	
	@Test
	void testBindSMT() {
		ServiceEntity svc = new ServiceEntity();
		svc.setId(new EmbeddedIdentifier("SvcId-T-1"));
		svc = services.save(svc);				
		ServiceMetadataTemplateEntity smt = new ServiceMetadataTemplateEntity();
		smt.setService(svc);				
		final ServiceMetadataTemplateEntity storedSMT = smtRepo.save(smt);
		
		ParticipantEntity p = new ParticipantEntity();
		p.setId(new EmbeddedIdentifier("PartId-T-1"));
		final ParticipantEntity stored = participants.save(p);
		
		Participant updated = assertDoesNotThrow(() -> partMgmtService.bindToSMT(T_USER, stored, storedSMT));
		
		assertEquals(1, updated.getBoundSMT().size());
		
		ArgumentCaptor<AuditLogRecord> captor = ArgumentCaptor.forClass(AuditLogRecord.class);
		verify(auditService).log(captor.capture());
		AuditLogRecord ar = captor.getValue();
		assertNotNull(ar.timestamp());
		assertEquals(T_USER.getUsername(), ar.username());
		assertEquals("Add Service to Participant", ar.action());
		assertEquals(updated.getId().toString(), ar.subject());
		assertTrue(ar.details().contains(storedSMT.getId().toString()));
		assertTrue(ar.details().contains(storedSMT.getService().getId().toString()));		
	}
	
	@Test
	void testUnbindSMT() {
		ServiceEntity svc = new ServiceEntity();
		svc.setId(new EmbeddedIdentifier("SvcId-T-1"));
		svc = services.save(svc);				
		ServiceMetadataTemplateEntity smt = new ServiceMetadataTemplateEntity();
		smt.setService(svc);				
		final ServiceMetadataTemplateEntity storedSMT = smtRepo.save(smt);
		
		ParticipantEntity p = new ParticipantEntity();
		p.setId(new EmbeddedIdentifier("PartId-T-1"));
		p.addBinding(storedSMT);
		final ParticipantEntity stored = participants.save(p);
		
		Participant updated = assertDoesNotThrow(() -> partMgmtService.removeSMTBinding(T_USER, stored, storedSMT));
		
		assertEquals(0, updated.getBoundSMT().size());
		
		ArgumentCaptor<AuditLogRecord> captor = ArgumentCaptor.forClass(AuditLogRecord.class);
		verify(auditService).log(captor.capture());
		AuditLogRecord ar = captor.getValue();
		assertNotNull(ar.timestamp());
		assertEquals(T_USER.getUsername(), ar.username());
		assertEquals("Remove Service from Participant", ar.action());
		assertEquals(updated.getId().toString(), ar.subject());
		assertTrue(ar.details().contains(storedSMT.getId().toString()));
		assertTrue(ar.details().contains(storedSMT.getService().getId().toString()));
	}
	
	@Test
	void testFindByName() {
		for(int i = 0; i < 20; i++) {
			ParticipantEntity p = new ParticipantEntity();
			p.setId(new EmbeddedIdentifier("PartId-T-"+i));
			p.setName("Participant " + i);
			participants.save(p);
		}
	
		assertTrue(assertDoesNotThrow(() -> partMgmtService.findParticipantsByName("articpant")).isEmpty());
		Collection<? extends Participant> result = assertDoesNotThrow(() -> 
												partMgmtService.findParticipantsByName("PARTI"));
		
		assertEquals(20, result.size());
		assertEquals(11, assertDoesNotThrow(() -> partMgmtService.findParticipantsByName("PARTIcipant 1")).size());		
	}
	
	@Test
	void testFindByAdditionalId() {
		IDSchemeEntity ids1 = new IDSchemeEntity("T-IDS-1", true, null, null, null); 
		IDSchemeEntity ids2 = new IDSchemeEntity("T-IDS-2", false, null, null, null);
		idschemes.save(ids1);
		idschemes.save(ids2);
			
		ParticipantEntity p1 = new ParticipantEntity();
		p1.setId(new EmbeddedIdentifier("PartId-T-1"));
		p1.addAdditionalId(new EmbeddedIdentifier(ids1, "ADDID-1"));
		p1.addAdditionalId(new EmbeddedIdentifier(ids1, "ADDID-2"));
		p1.addAdditionalId(new EmbeddedIdentifier(ids2, "ADDID-1"));
		participants.save(p1);

		ParticipantEntity p2 = new ParticipantEntity();
		p2.setId(new EmbeddedIdentifier("PartId-T-2"));
		p2.addAdditionalId(new EmbeddedIdentifier(ids1, "addid-1"));
		p2.addAdditionalId(new EmbeddedIdentifier(ids2, "ADDID-2"));
		participants.save(p2);
		
		ParticipantEntity p3 = new ParticipantEntity();
		p3.setId(new EmbeddedIdentifier("PartId-T-3"));
		p3.addAdditionalId(new EmbeddedIdentifier(ids2, "addid-2"));
		participants.save(p3);

		Collection<? extends Participant> result = assertDoesNotThrow(() ->
					partMgmtService.findParticipantsByAdditionalId(
													new EmbeddedIdentifier(ids1, "addid-1")));
		assertEquals(1, result.size());
		assertTrue(result.contains(p2));
	
		result = assertDoesNotThrow(() ->  partMgmtService.findParticipantsByAdditionalId(
													new EmbeddedIdentifier(ids2, "addid-1")));
		assertEquals(1, result.size());
		assertTrue(result.contains(p1));
		
		result = assertDoesNotThrow(() -> partMgmtService.findParticipantsByAdditionalId(
														new EmbeddedIdentifier(ids2, "addid-2")));
		assertEquals(2, result.size());
		assertTrue(result.contains(p2));
		assertTrue(result.contains(p3));
	}

}
