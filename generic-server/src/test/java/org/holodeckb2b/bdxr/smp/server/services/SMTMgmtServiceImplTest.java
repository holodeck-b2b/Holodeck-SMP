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

import java.util.Collection;
import java.util.Set;
import java.util.UUID;

import org.holodeckb2b.bdxr.smp.server.CommonServerConfig;
import org.holodeckb2b.bdxr.smp.server.auditlogging.AuditLogRecord;
import org.holodeckb2b.bdxr.smp.server.auditlogging.AuditLogService;
import org.holodeckb2b.bdxr.smp.server.datamodel.Endpoint;
import org.holodeckb2b.bdxr.smp.server.datamodel.Process;
import org.holodeckb2b.bdxr.smp.server.datamodel.ProcessGroup;
import org.holodeckb2b.bdxr.smp.server.datamodel.ProcessInfo;
import org.holodeckb2b.bdxr.smp.server.datamodel.Service;
import org.holodeckb2b.bdxr.smp.datamodel.EndpointInfo;
import org.holodeckb2b.bdxr.smp.server.datamodel.ServiceMetadataTemplate;
import org.holodeckb2b.bdxr.smp.server.db.entities.EmbeddedIdentifier;
import org.holodeckb2b.bdxr.smp.server.db.entities.EmbeddedProcessIdentifier;
import org.holodeckb2b.bdxr.smp.server.db.entities.EndpointEntity;
import org.holodeckb2b.bdxr.smp.server.db.entities.ProcessEntity;
import org.holodeckb2b.bdxr.smp.server.db.entities.ProcessInfoEntity;
import org.holodeckb2b.bdxr.smp.server.db.entities.ServiceMetadataTemplateEntity;
import org.holodeckb2b.bdxr.smp.server.db.entities.ServiceEntity;
import org.holodeckb2b.bdxr.smp.server.db.entities.TransportProfileEntity;
import org.holodeckb2b.bdxr.smp.server.db.repos.EndpointRepository;
import org.holodeckb2b.bdxr.smp.server.db.repos.ProcessRepository;
import org.holodeckb2b.bdxr.smp.server.db.repos.ServiceMetadataTemplateRepository;
import org.holodeckb2b.bdxr.smp.server.db.repos.ServiceRepository;
import org.holodeckb2b.bdxr.smp.server.db.repos.TransportProfileRepository;
import org.holodeckb2b.bdxr.smp.server.services.core.ConstraintViolationException;
import org.holodeckb2b.bdxr.smp.server.services.core.ConstraintViolationException.ViolationType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest(classes = { CommonServerConfig.class })
class SMTMgmtServiceImplTest {

	@MockitoBean
	private AuditLogService 		auditService;
	
	@Autowired
	private TransportProfileRepository profiles;
	
	@Autowired
	private EndpointRepository		endpoints;
	
	@Autowired
	private ServiceRepository		services;
	
	@Autowired
	private ProcessRepository		procs;
	
	@Autowired
	private ServiceMetadataTemplateRepository smtRepo;
	
	@Autowired
	private SMTMgmtServiceImpl 	smtMgmtService;
	
	private static final TestUser T_USER = new TestUser();
	
	@AfterEach
	void cleanup() {
		reset(auditService);
		smtRepo.deleteAll();
		services.deleteAll();
		procs.deleteAll();
		endpoints.deleteAll();
	}	
	
	@Test
	void testAdd() {
		ServiceMetadataTemplate smt = createTemplate();
		ServiceMetadataTemplate saved = assertDoesNotThrow(() -> smtMgmtService.addTemplate(T_USER, smt));
		
		assertTrue(saved instanceof ServiceMetadataTemplateEntity);
		assertEquals(smt.getService(), saved.getService());
		assertEquals(1, saved.getProcessMetadata().size());
		
		ProcessGroup pg = saved.getProcessMetadata().iterator().next();
		
		assertEquals(1, pg.getProcessInfo().size());
		org.holodeckb2b.bdxr.smp.datamodel.ProcessInfo pi = pg.getProcessInfo().iterator().next();
		assertTrue(pi instanceof ProcessInfoEntity);
		assertEquals(smt.getProcessMetadata().iterator().next().getProcessInfo().iterator().next().getProcessId(), 
						((ProcessInfoEntity) pi).getProcess().getId());
		
		assertEquals(1, pg.getEndpoints().size());
		assertEquals(smt.getProcessMetadata().iterator().next().getEndpoints().iterator().next(),
					 pg.getEndpoints().iterator().next());
		
		ArgumentCaptor<AuditLogRecord> captor = ArgumentCaptor.forClass(AuditLogRecord.class);
		verify(auditService).log(captor.capture());
		
		AuditLogRecord ar = captor.getValue();
		assertNotNull(ar.timestamp());
		assertEquals(T_USER.getUsername(), ar.username());
		assertEquals("Add SMT", ar.action());
		assertEquals(saved.getId().toString(), ar.subject());	
	}
	
	@Test
	void testRejectAddUnmanagedService() {
		ServiceMetadataTemplate smt = mock(ServiceMetadataTemplate.class);
		ServiceEntity svc = new ServiceEntity();
		svc.setId(new EmbeddedIdentifier("unmanaged-service-" + System.currentTimeMillis()));
		when(smt.getService()).thenReturn(svc);
		
		ConstraintViolationException cve = assertThrows(ConstraintViolationException.class,
														() -> smtMgmtService.addTemplate(T_USER, smt));
		
		assertEquals(ViolationType.INVALID_FIELD, cve.getViolation());
		assertEquals(smt, cve.getSubject());				
	}
	
	@Test
	void testAddProcToPg() {
		ServiceMetadataTemplate smt = assertDoesNotThrow(() -> smtMgmtService.addTemplate(T_USER, createTemplate()));
		
		ProcessEntity proc = new ProcessEntity();
		proc.setId(new EmbeddedProcessIdentifier("test-process-" + System.currentTimeMillis()));
		procs.save(proc);

		org.holodeckb2b.bdxr.smp.server.datamodel.ProcessInfo pi = mock(org.holodeckb2b.bdxr.smp.server.datamodel.ProcessInfo.class);
		when(pi.getProcess()).thenReturn(proc);
		
		((ProcessGroup) smt.getProcessMetadata().iterator().next()).addProcessInfo(pi);
		
		ServiceMetadataTemplate updated = assertDoesNotThrow(() -> smtMgmtService.updateTemplate(T_USER, smt));
		
		Collection<? extends org.holodeckb2b.bdxr.smp.datamodel.ProcessInfo> processInfo =
														updated.getProcessMetadata().iterator().next().getProcessInfo();
		assertEquals(2, processInfo.size());
		assertTrue(processInfo.parallelStream()
				.anyMatch(p -> ((org.holodeckb2b.bdxr.smp.server.datamodel.ProcessInfo) p).getProcess().equals(proc)));
		
		ArgumentCaptor<AuditLogRecord> captor = ArgumentCaptor.forClass(AuditLogRecord.class);
		verify(auditService, times(2)).log(captor.capture());
		
		AuditLogRecord ar = captor.getValue();
		assertNotNull(ar.timestamp());
		assertEquals(T_USER.getUsername(), ar.username());
		assertEquals("Update SMT", ar.action());
		assertEquals(smt.getId().toString(), ar.subject());	
	}	
	
	@Test
	void testAddEPToPg() {
		ServiceMetadataTemplate smt = assertDoesNotThrow(() -> smtMgmtService.addTemplate(T_USER, createTemplate()));
		
		TransportProfileEntity tp = new TransportProfileEntity();
		tp.setId(new EmbeddedIdentifier("test-transport-profile-" + System.currentTimeMillis()));
		profiles.save(tp);
		
		EndpointEntity ep = new EndpointEntity();
		ep.setTransportProfile(tp);
		ep.setUrl("https://test.send.to.me/");
		endpoints.save(ep);
		
		((ProcessGroup) smt.getProcessMetadata().iterator().next()).addEndpoint(ep);
		
		ServiceMetadataTemplate updated = assertDoesNotThrow(() -> smtMgmtService.updateTemplate(T_USER, smt));
		
		Collection<? extends EndpointInfo> endpoints = updated.getProcessMetadata().iterator().next().getEndpoints();
		assertEquals(2, endpoints.size());
		assertTrue(endpoints.contains(ep));
		
		ArgumentCaptor<AuditLogRecord> captor = ArgumentCaptor.forClass(AuditLogRecord.class);
		verify(auditService, times(2)).log(captor.capture());
		
		AuditLogRecord ar = captor.getValue();
		assertNotNull(ar.timestamp());
		assertEquals(T_USER.getUsername(), ar.username());
		assertEquals("Update SMT", ar.action());
		assertEquals(smt.getId().toString(), ar.subject());	
	}
	
	@Test
	void testAddPg() {
		ServiceMetadataTemplate smt = assertDoesNotThrow(() -> smtMgmtService.addTemplate(T_USER, createTemplate()));
		
		ProcessEntity proc = new ProcessEntity();
		proc.setId(new EmbeddedProcessIdentifier("test-process-" + System.currentTimeMillis()));
		procs.save(proc);
		org.holodeckb2b.bdxr.smp.server.datamodel.ProcessInfo pi = mock(org.holodeckb2b.bdxr.smp.server.datamodel.ProcessInfo.class);
		when(pi.getProcess()).thenReturn(proc);		
		ProcessGroup pg = mock(ProcessGroup.class);
		when(pg.getProcessInfo()).then(i -> Set.of(pi));
		
		smt.addProcessGroup(pg);
		
		ServiceMetadataTemplate updated = assertDoesNotThrow(() -> smtMgmtService.updateTemplate(T_USER, smt));
		
		assertEquals(2, updated.getProcessMetadata().size());
		assertTrue(updated.getProcessMetadata().parallelStream().anyMatch(pgrp -> pgrp.getEndpoints().isEmpty()));
		
		ArgumentCaptor<AuditLogRecord> captor = ArgumentCaptor.forClass(AuditLogRecord.class);
		verify(auditService, times(2)).log(captor.capture());
		
		AuditLogRecord ar = captor.getValue();
		assertNotNull(ar.timestamp());
		assertEquals(T_USER.getUsername(), ar.username());
		assertEquals("Update SMT", ar.action());
		assertEquals(smt.getId().toString(), ar.subject());	
	}	
	
	@Test
	void testRemovePg() {
		ServiceMetadataTemplate smt = assertDoesNotThrow(() -> smtMgmtService.addTemplate(T_USER, createTemplate()));
				
		smt.removeProcessGroup(smt.getProcessMetadata().iterator().next());
		
		ServiceMetadataTemplate updated = assertDoesNotThrow(() -> smtMgmtService.updateTemplate(T_USER, smt));
		
		assertTrue(updated.getProcessMetadata().isEmpty());

		ArgumentCaptor<AuditLogRecord> captor = ArgumentCaptor.forClass(AuditLogRecord.class);
		verify(auditService, times(2)).log(captor.capture());
		
		AuditLogRecord ar = captor.getValue();
		assertNotNull(ar.timestamp());
		assertEquals(T_USER.getUsername(), ar.username());
		assertEquals("Update SMT", ar.action());
		assertEquals(smt.getId().toString(), ar.subject());	
	}
	
	@Test
	void testFindByService() {
		ServiceMetadataTemplate smt1 = createTemplate();
		ServiceMetadataTemplate smt2 = createTemplate();		
		for(int i = 0; i < 100; i++) {
			ServiceMetadataTemplate smt = i % 2 == 0 ? smt1 : smt2;
			assertDoesNotThrow(() -> smtMgmtService.addTemplate(T_USER, smt));
		}
		
		Service svc = smt2.getService();
		Collection<? extends ServiceMetadataTemplate> result = 
												assertDoesNotThrow(() -> smtMgmtService.findTemplatesForService(svc));
		
		assertEquals(50, result.size());
		assertTrue(result.parallelStream().allMatch(smt -> smt.getService().equals(svc)));
	}
	
	@Test
	void testFindByProcess() {
		ServiceMetadataTemplate smt1 = createTemplate();
		ServiceMetadataTemplate smt2 = createTemplate();		
		for(int i = 0; i < 100; i++) {
			ServiceMetadataTemplate smt = i % 2 == 0 ? smt1 : smt2;
			assertDoesNotThrow(() -> smtMgmtService.addTemplate(T_USER, smt));
		}
		
		Process proc = ((ProcessInfo) smt1.getProcessMetadata().iterator().next().getProcessInfo().iterator().next()).getProcess();
		Collection<? extends ServiceMetadataTemplate> result = 
												assertDoesNotThrow(() -> smtMgmtService.findTemplatesForProcess(proc));
		
		assertEquals(50, result.size());
		assertTrue(result.parallelStream().allMatch(smt ->  ((ProcessInfo) smt1.getProcessMetadata().iterator().next()
													.getProcessInfo().iterator().next()).getProcess().equals(proc)));
	}
	

	@Test
	void testFindByEndpoint() {
		ServiceMetadataTemplate smt1 = createTemplate();
		ServiceMetadataTemplate smt2 = createTemplate();		
		for(int i = 0; i < 100; i++) {
			ServiceMetadataTemplate smt = i % 2 == 0 ? smt1 : smt2;
			assertDoesNotThrow(() -> smtMgmtService.addTemplate(T_USER, smt));
		}
		
		Endpoint ep = (Endpoint) smt1.getProcessMetadata().iterator().next().getEndpoints().iterator().next();
		Collection<? extends ServiceMetadataTemplate> result = 
												assertDoesNotThrow(() -> smtMgmtService.findTemplatesUsingEndpoint(ep));
		
		assertEquals(50, result.size());
		assertTrue(result.parallelStream().allMatch(smt ->  ((Endpoint) smt1.getProcessMetadata().iterator().next()
													.getEndpoints().iterator().next()).equals(ep)));
	}
	

	
	ServiceMetadataTemplate createTemplate() {
		ServiceMetadataTemplate smt = mock(ServiceMetadataTemplate.class);
		
		TransportProfileEntity tp = new TransportProfileEntity();
		tp.setId(new EmbeddedIdentifier("test-transport-profile-" + System.currentTimeMillis()));
		profiles.save(tp);
		
		EndpointEntity ep = new EndpointEntity();
		ep.setTransportProfile(tp);
		ep.setUrl("https://test.send.to.me/");
		endpoints.save(ep);
		
		ProcessEntity proc = new ProcessEntity();
		proc.setId(new EmbeddedProcessIdentifier("test-process-" + UUID.randomUUID().toString()));
		procs.save(proc);
		
		ServiceEntity svc = new ServiceEntity();
		svc.setId(new EmbeddedIdentifier("test-service-" + UUID.randomUUID().toString()));
		services.save(svc);
		
		ProcessInfoEntity pi = new ProcessInfoEntity();
		pi.setProcess(proc);
		
		ProcessGroup pg = mock(ProcessGroup.class);
		when(pg.getProcessInfo()).then(i -> Set.of(pi));
		when(pg.getEndpoints()).then(i -> Set.of(ep));
		
		when(smt.getService()).thenReturn(svc);
		when(smt.getProcessMetadata()).then(i -> Set.of(pg));
		
		return smt;
	}

}
