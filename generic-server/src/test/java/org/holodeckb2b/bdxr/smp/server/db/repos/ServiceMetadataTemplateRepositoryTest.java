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
package org.holodeckb2b.bdxr.smp.server.db.repos;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.holodeckb2b.bdxr.smp.server.CommonServerConfig;
import org.holodeckb2b.bdxr.smp.server.datamodel.ProcessGroup;
import org.holodeckb2b.bdxr.smp.server.db.entities.EmbeddedIdentifier;
import org.holodeckb2b.bdxr.smp.server.db.entities.EmbeddedProcessIdentifier;
import org.holodeckb2b.bdxr.smp.server.db.entities.EndpointEntity;
import org.holodeckb2b.bdxr.smp.server.db.entities.ProcessEntity;
import org.holodeckb2b.bdxr.smp.server.db.entities.ProcessGroupEntity;
import org.holodeckb2b.bdxr.smp.server.db.entities.ProcessInfoEntity;
import org.holodeckb2b.bdxr.smp.server.db.entities.ServiceEntity;
import org.holodeckb2b.bdxr.smp.server.db.entities.ServiceMetadataTemplateEntity;
import org.holodeckb2b.bdxr.smp.server.db.entities.TransportProfileEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ContextConfiguration;

@DataJpaTest
@ContextConfiguration(classes = { CommonServerConfig.class })
class ServiceMetadataTemplateRepositoryTest {

	@Autowired
	ServiceRepository	svcRepo;
	
	@Autowired
	ProcessRepository	procRepo;
	
	@Autowired
	EndpointRepository	epRepo;
	
	@Autowired
	TransportProfileRepository tpRepo;
	
	@Autowired
	ServiceMetadataTemplateRepository	repo;
		
	
	private ProcessEntity 	PROC_1;
	private ProcessEntity 	PROC_2;
	private ProcessEntity 	PROC_3;
	private EndpointEntity 	EP_1;
	private EndpointEntity 	EP_2;
	private EndpointEntity 	EP_3;
	private ServiceEntity 	SVC_1;
	private ServiceEntity 	SVC_2;
	private ServiceMetadataTemplateEntity 		SMT_1;
	private ServiceMetadataTemplateEntity 		SMT_2;
	private ServiceMetadataTemplateEntity 		SMT_3;
	
	@BeforeEach
	void createTestData() {
		PROC_1 = new ProcessEntity();
		PROC_1.setId(new EmbeddedProcessIdentifier("ProcId-T-1"));
		procRepo.save(PROC_1);
		PROC_2 = new ProcessEntity();
		PROC_2.setId(new EmbeddedProcessIdentifier("ProcId-T-2"));	
		procRepo.save(PROC_2);	
		PROC_3 = new ProcessEntity();
		PROC_3.setId(new EmbeddedProcessIdentifier("ProcId-T-3"));	
		procRepo.save(PROC_3);	
		
		TransportProfileEntity tp = new TransportProfileEntity();
		tp.setId(new EmbeddedIdentifier("transport-T-1"));
		tpRepo.save(tp);		
		EP_1 = new EndpointEntity();
		EP_1.setTransportProfile(tp);
		EP_1.setUrl("http://test.holodeck-smp.org/1");
		epRepo.save(EP_1);		
		EP_2 = new EndpointEntity();
		EP_2.setTransportProfile(tp);
		EP_2.setUrl("http://test.holodeck-smp.org/2");
		epRepo.save(EP_2);		
		EP_3 = new EndpointEntity();
		EP_3.setTransportProfile(tp);
		EP_3.setUrl("http://test.holodeck-smp.org/3");
		epRepo.save(EP_3);		
		
		SVC_1 = new ServiceEntity();
		SVC_1.setId(new EmbeddedIdentifier("SvcId-T-1"));
		SVC_1 = svcRepo.save(SVC_1);		
		SVC_2 = new ServiceEntity();
		SVC_2.setId(new EmbeddedIdentifier("SvcId-T-2"));
		SVC_2 = svcRepo.save(SVC_2);		
		
		SMT_1 = new ServiceMetadataTemplateEntity();
		SMT_1.setService(SVC_1);		
		SMT_1.addProcessGroup(createPg(SMT_1, Set.of(PROC_1, PROC_2), Set.of(EP_1)));
		repo.save(SMT_1);

		SMT_2 = new ServiceMetadataTemplateEntity();
		SMT_2.setService(SVC_1);		
		SMT_2.addProcessGroup(createPg(SMT_2, Set.of(PROC_2), Set.of(EP_2)));
		repo.save(SMT_2);
		
		SMT_3 = new ServiceMetadataTemplateEntity();
		SMT_3.setService(SVC_2);
		SMT_3.addProcessGroup(createPg(SMT_3, Set.of(PROC_3), Set.of(EP_1, EP_3)));
		SMT_3.addProcessGroup(createPg(SMT_3, Set.of(PROC_2), Set.of(EP_2)));
		repo.save(SMT_3);
	}
	
	ProcessGroup createPg(ServiceMetadataTemplateEntity smt, Collection<ProcessEntity> procs, Collection<EndpointEntity> eps) {
		ProcessGroup pg = mock(ProcessGroup.class);		
		when(pg.getEndpoints()).then(invocation -> eps);
		Collection<ProcessInfoEntity> piList = new ArrayList<>();
		procs.forEach(p -> {
			ProcessInfoEntity pi = new ProcessInfoEntity();
			pi.setProcess(p);		
			piList.add(pi);
		});
		when(pg.getProcessInfo()).then(invocation -> piList);
		
		return pg;
	}
	
	@Test
	void testSaveSMT() {
		ServiceMetadataTemplateEntity saved = repo.findById(SMT_1.getId()).orElse(null);
		
		assertNotNull(saved);
		assertEquals(SVC_1, saved.getService());
		
		List<ProcessGroupEntity> pgs = saved.getProcessMetadata();
		assertEquals(1, pgs.size());
		
		ProcessGroupEntity pg = pgs.get(0);
		
		assertEquals(2, pg.getProcessInfo().size());
		assertTrue(pg.getProcessInfo().parallelStream().anyMatch(pi -> pi.getProcess().equals(PROC_1)));
		assertTrue(pg.getProcessInfo().parallelStream().anyMatch(pi -> pi.getProcess().equals(PROC_2)));
		
		assertEquals(1, pg.getEndpoints().size());
		assertEquals(EP_1, pg.getEndpoints().iterator().next());
	}
	
	@Test
	void testAddPg() {
		ServiceMetadataTemplateEntity saved = repo.findById(SMT_1.getId()).orElse(null);
		
		saved.addProcessGroup(createPg(saved, Set.of(PROC_3), Set.of(EP_3)));
		
		ServiceMetadataTemplateEntity updated = repo.save(saved);
		
		assertEquals(2, updated.getProcessMetadata().size());
		assertTrue(updated.getProcessMetadata().parallelStream().anyMatch(pg -> 
								pg.getProcessInfo().parallelStream().anyMatch(pi -> pi.getProcess().equals(PROC_3))));
	}
	
	@Test
	void testRemovePg() {
		ServiceMetadataTemplateEntity saved = repo.findById(SMT_3.getId()).orElse(null);
		assertEquals(2, saved.getProcessMetadata().size());
		
		ProcessGroupEntity pg2rm = saved.getProcessMetadata().get(0);
		saved.removeProcessGroup(pg2rm);
		
		ServiceMetadataTemplateEntity updated = repo.save(saved);
		
		assertEquals(1, updated.getProcessMetadata().size());
		assertFalse(updated.getProcessMetadata().contains(pg2rm));
	}
	
	@Test
	void testfindByService() {
		Collection<ServiceMetadataTemplateEntity> r = assertDoesNotThrow(() -> repo.findByService(SVC_1));
		assertEquals(2, r.size());
		assertTrue(r.contains(SMT_1));
		assertTrue(r.contains(SMT_2));		
		
		r = assertDoesNotThrow(() -> repo.findByService(SVC_2));
		assertEquals(1, r.size());
		assertTrue(r.contains(SMT_3));		
	}

	@Test
	void testfindByProcess() {
		Collection<ServiceMetadataTemplateEntity> r = assertDoesNotThrow(() -> repo.findByProcess(PROC_1));
		assertEquals(1, r.size());
		assertTrue(r.contains(SMT_1));
		
		r = assertDoesNotThrow(() -> repo.findByProcess(PROC_2));
		assertEquals(3, r.size());
		assertTrue(r.contains(SMT_1));		
		assertTrue(r.contains(SMT_2));		
		assertTrue(r.contains(SMT_3));		
	}

	@Test
	void testfindByEndpoint() {
		Collection<ServiceMetadataTemplateEntity> r = assertDoesNotThrow(() -> repo.findByEndpoint(EP_2));
		assertEquals(2, r.size());
		assertTrue(r.contains(SMT_2));
		assertTrue(r.contains(SMT_3));

		r = assertDoesNotThrow(() -> repo.findByEndpoint(EP_3));
		assertEquals(1, r.size());
		assertTrue(r.contains(SMT_3));
	}
	

}