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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atMost;
import static org.mockito.Mockito.atMostOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.holodeckb2b.bdxr.smp.server.CommonServerConfig;
import org.holodeckb2b.bdxr.smp.server.auditlogging.AuditLogRecord;
import org.holodeckb2b.bdxr.smp.server.auditlogging.AuditLogService;
import org.holodeckb2b.bdxr.smp.server.datamodel.Service;
import org.holodeckb2b.bdxr.smp.server.db.entities.EmbeddedIdentifier;
import org.holodeckb2b.bdxr.smp.server.db.entities.IDSchemeEntity;
import org.holodeckb2b.bdxr.smp.server.db.entities.ServiceEntity;
import org.holodeckb2b.bdxr.smp.server.db.repos.IDSchemeRepository;
import org.holodeckb2b.bdxr.smp.server.db.repos.ServiceRepository;
import org.holodeckb2b.bdxr.smp.server.services.core.ConstraintViolationException;
import org.holodeckb2b.bdxr.smp.server.services.core.ConstraintViolationException.ViolationType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest(classes = { CommonServerConfig.class })
public class ServiceMgmtServiceImplTest {

	@MockitoBean
	private AuditLogService 		auditService;
	
	@Autowired
	private ServiceRepository		services;
	
	@Autowired
	private IDSchemeRepository		idschemes;
	
	@Autowired
	private ServiceMgmtServiceImpl 	svcMgmtService;
	
	private static final TestUser T_USER = new TestUser();
	
	@AfterEach
	void cleanup() {
		reset(auditService);
		services.deleteAll();
		idschemes.deleteAll();
	}
	
	@Test	
	void testAdd() {
		IDSchemeEntity ids = assertDoesNotThrow(() -> idschemes.save(new IDSchemeEntity("TestIDScheme")));
		Service svc = createTestService(ids);
		
		long count = services.count();
		Service saved = assertDoesNotThrow(() -> svcMgmtService.addService(T_USER, svc));
	
		assertTrue(saved instanceof ServiceEntity);
		
		assertEquals(svc.getId(), saved.getId());
		assertEquals(svc.getName(), saved.getName());
		assertEquals(svc.getSpecificationRef(), saved.getSpecificationRef());
		
		assertEquals(count + 1, services.count());
		assertEquals(saved, services.findByIdentifier((EmbeddedIdentifier) saved.getId()));
		
		ArgumentCaptor<AuditLogRecord> captor = ArgumentCaptor.forClass(AuditLogRecord.class);
		assertDoesNotThrow(() -> verify(auditService).log(captor.capture()));
		
		AuditLogRecord ar = captor.getValue();
		assertNotNull(ar.timestamp());
		assertEquals(T_USER.getUsername(), ar.username());
		assertEquals("Add Service", ar.action());
		assertEquals(svc.getId().toString(), ar.subject());		
	}

	@Test	
	void testRejectAddDuplicate() {
		Service svc = createTestService();
		
		long count = services.count();
		assertDoesNotThrow(() -> svcMgmtService.addService(T_USER, svc));			
		assertEquals(count + 1, services.count());
		
		ConstraintViolationException cve = assertThrows(ConstraintViolationException.class, 
														() -> svcMgmtService.addService(T_USER, svc));		
		assertEquals(count + 1, services.count());

		assertEquals(ViolationType.DUPLICATE_ID, cve.getViolation());
		assertTrue(cve.getSubject() instanceof Service);
		assertEquals(svc.getId(), cve.getSubject().getId());	
		
		assertDoesNotThrow(() -> verify(auditService, atMostOnce()).log(any(AuditLogRecord.class)));
	}

	@Test	
	void testUpdate() {
		Service svc = createTestService();
		
		ServiceEntity saved = assertDoesNotThrow(() -> (ServiceEntity) svcMgmtService.addService(T_USER, svc));
		long count = services.count();
		
		saved.setName("New Service Name");		
		
		Service updated = assertDoesNotThrow(() -> svcMgmtService.updateService(T_USER, saved));		
		assertEquals(count, services.count());
		assertEquals(svc.getId(), updated.getId());
		assertEquals(saved.getName(), updated.getName());
		
		assertEquals(updated, services.findByIdentifier(saved.getId()));
		
		ArgumentCaptor<AuditLogRecord> captor = ArgumentCaptor.forClass(AuditLogRecord.class);
		assertDoesNotThrow(() -> verify(auditService, times(2)).log(captor.capture()));
		
		AuditLogRecord ar = captor.getValue();
		assertNotNull(ar.timestamp());
		assertEquals(T_USER.getUsername(), ar.username());
		assertEquals("Update Service", ar.action());
		assertEquals(svc.getId().toString(), ar.subject());	
	}
	
	@Test	
	void testRejectUpdateDup() {
		Service svc1 = createTestService();
		Service svc2 = createTestService();
		
		ServiceEntity saved1 = assertDoesNotThrow(() -> (ServiceEntity) svcMgmtService.addService(T_USER, svc1));
		ServiceEntity saved2 = assertDoesNotThrow(() -> (ServiceEntity) svcMgmtService.addService(T_USER, svc2));
		
		saved2.setId(saved1.getId());		

		ConstraintViolationException cve = assertThrows(ConstraintViolationException.class, 
												() -> svcMgmtService.updateService(T_USER, saved2));		
		
		assertEquals(ViolationType.DUPLICATE_ID, cve.getViolation());
		assertEquals(saved2, cve.getSubject());		
		
		assertDoesNotThrow(() -> verify(auditService, atMost(2)).log(any(AuditLogRecord.class)));
	}
	
	@Test	
	void testDelete() {
		Service svc = createTestService();
		
		ServiceEntity saved = assertDoesNotThrow(() -> (ServiceEntity) svcMgmtService.addService(T_USER, svc));
		long count = services.count();
		
		assertDoesNotThrow(() -> svcMgmtService.deleteService(T_USER, saved));		
		assertEquals(count - 1, services.count());
		
		assertNull(services.findByIdentifier(saved.getId()));	
		
		ArgumentCaptor<AuditLogRecord> captor = ArgumentCaptor.forClass(AuditLogRecord.class);
		assertDoesNotThrow(() -> verify(auditService, times(2)).log(captor.capture()));
		
		AuditLogRecord ar = captor.getValue();
		assertNotNull(ar.timestamp());
		assertEquals(T_USER.getUsername(), ar.username());
		assertEquals("Delete Service", ar.action());
		assertEquals(svc.getId().toString(), ar.subject());	
	}

	@Test
	void testGetService() {
		IDSchemeEntity ids = assertDoesNotThrow(() -> idschemes.save(new IDSchemeEntity("TestIDScheme", true)));
		Service svc1 = mock(Service.class);
		when(svc1.getId()).thenReturn(new EmbeddedIdentifier(ids, "SvcId-T-1"));
		when(svc1.getName()).thenReturn("T-SVC-1");
		assertDoesNotThrow(() -> svcMgmtService.addService(T_USER, svc1));
		Service svc2 = mock(Service.class);
		when(svc2.getId()).thenReturn(new EmbeddedIdentifier(ids, "SVCID-T-1"));		
		when(svc2.getName()).thenReturn("T-SVC-2");
		assertDoesNotThrow(() -> svcMgmtService.addService(T_USER, svc2));
		Service svc3 = mock(Service.class);
		when(svc3.getId()).thenReturn(new EmbeddedIdentifier("SVCID-T-1"));
		when(svc3.getName()).thenReturn("T-SVC-3");
		assertDoesNotThrow(() -> svcMgmtService.addService(T_USER, svc3));
		
		assertEquals(svc1.getName(), assertDoesNotThrow(() -> svcMgmtService.getById(svc1.getId())).getName());
		assertEquals(svc3.getName(), assertDoesNotThrow(() -> 
									svcMgmtService.getById(new EmbeddedIdentifier(svc3.getId().getValue())).getName()));		
	}
	
	@Test
	void testGetServices() {
		for (int i = 0; i < 200; i++) 
			assertDoesNotThrow(() -> svcMgmtService.addService(T_USER, createTestService()));
		
		Page<ServiceEntity> firstPage = assertDoesNotThrow(() -> svcMgmtService.getServices(PageRequest.of(0, 50)));
		
		assertEquals(4, firstPage.getTotalPages());
	}
	
	Service createTestService(IDSchemeEntity ...ids) {
		Service svc = mock(Service.class);
		EmbeddedIdentifier id = new EmbeddedIdentifier(ids.length == 0 ? null : ids[0], "SvcId-T-" + System.nanoTime());
		when(svc.getId()).thenReturn(id);
		when(svc.getSpecificationRef()).thenReturn("http://test.holodeck-smp.org/svc/1");
		when(svc.getName()).thenReturn("Test-Service");
		return svc;
	}
}
