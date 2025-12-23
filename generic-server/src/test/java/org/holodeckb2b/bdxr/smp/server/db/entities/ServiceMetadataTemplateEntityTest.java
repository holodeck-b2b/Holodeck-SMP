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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Set;

import org.holodeckb2b.bdxr.smp.datamodel.ProcessGroup;
import org.holodeckb2b.bdxr.smp.datamodel.impl.ProcessGroupImpl;
import org.holodeckb2b.bdxr.smp.datamodel.impl.RedirectionV2Impl;
import org.holodeckb2b.bdxr.smp.server.datamodel.ProcessInfo;
import org.junit.jupiter.api.Test;

class ServiceMetadataTemplateEntityTest extends BaseEntityTest<ServiceMetadataTemplateEntity>{

	@Test
	void testSingleEmptyPg() {
		ServiceEntity svc = new ServiceEntity();
		svc.setId(new EmbeddedIdentifier("SvcId-T-1"));
		em.persist(svc);
		
		ServiceMetadataTemplateEntity smt = new ServiceMetadataTemplateEntity();
		smt.setService(svc);		
		smt.addProcessGroup(new ProcessGroupImpl());
		
		assertDoesNotThrow(() -> save(smt));
		
		ServiceMetadataTemplateEntity found = reload(smt);		
		assertNotNull(found);
		assertEquals(svc, found.getService());
		assertEquals(smt.getProcessMetadata().size(), found.getProcessMetadata().size());
		
		ProcessGroup pg = found.getProcessMetadata().get(0);
		assertTrue(pg.getProcessInfo().isEmpty());
		assertTrue(pg.getEndpoints().isEmpty());
		assertNull(pg.getRedirection());
	}
	
	@Test
	void testAllRedirection() {
		ServiceEntity svc = new ServiceEntity();
		svc.setId(new EmbeddedIdentifier("SvcId-T-1"));
		em.persist(svc);
		
		ServiceMetadataTemplateEntity smt = new ServiceMetadataTemplateEntity();
		smt.setService(svc);
		
		ProcessGroupImpl pg = new ProcessGroupImpl();
		RedirectionV2Impl redirect = new RedirectionV2Impl(assertDoesNotThrow(() -> new URL("http://go.to.old/smp")),
															EndpointEntityTest.T_CERT);
		pg.setRedirection(redirect);		
		smt.addProcessGroup(pg);
		
		assertDoesNotThrow(() -> save(smt));
		
		ServiceMetadataTemplateEntity found = reload(smt);		
		assertNotNull(found);
		assertEquals(svc, found.getService());
		EmbeddedRedirection savedRedirect = found.getProcessMetadata().get(0).getRedirection();
		
		assertEquals(redirect.getNewSMPURL(), savedRedirect.getNewSMPURL());
		assertEquals(redirect.getSMPCertificate(), savedRedirect.getSMPCertificate());
	}
	
	@Test
	void testFullProcGroup() {		
		ServiceMetadataTemplateEntity smt = new ServiceMetadataTemplateEntity();
		smt.setService(createService());				
		smt.addProcessGroup(createPg(Set.of(createProcess(), createProcess()), Set.of(createEndpoint())));
		
		assertDoesNotThrow(() -> save(smt));
		
		ServiceMetadataTemplateEntity found = reload(smt);
		
		assertNotNull(found);
		assertEquals(smt.getService(), found.getService());
		assertEquals(smt.getProcessMetadata().size(), found.getProcessMetadata().size());
		
		assertTrue(smt.getProcessMetadata().parallelStream().allMatch(spg -> 
									found.getProcessMetadata().parallelStream().anyMatch(fpg -> equalPg(spg, fpg))));
		
	}
	
	@Test
	void testRemoveProcGroup() {
		ServiceMetadataTemplateEntity smt = new ServiceMetadataTemplateEntity();
		smt.setService(createService());				
		smt.addProcessGroup(createPg(Set.of(createProcess(), createProcess()), Set.of(createEndpoint())));
		smt.addProcessGroup(createPg(Set.of(createProcess()), Set.of(createEndpoint())));
		
		assertDoesNotThrow(() -> save(smt));
		assertEquals(2, smt.getProcessMetadata().size());
		
		smt.removeProcessGroup(smt.getProcessMetadata().get(1));
		
		assertDoesNotThrow(() -> save(smt));
		ServiceMetadataTemplateEntity updated = reload(smt);
		
		assertEquals(1, updated.getProcessMetadata().size());		
	}
	
	@Test
	void testAddProcInfo() {
		ServiceMetadataTemplateEntity smt = new ServiceMetadataTemplateEntity();
		smt.setService(createService());				
		smt.addProcessGroup(createPg(Set.of(createProcess()), Set.of(createEndpoint())));
		
		assertDoesNotThrow(() -> save(smt));
		
		ProcessInfo pi = mock(ProcessInfo.class);
		ProcessEntity p = createProcess();
		when(pi.getProcess()).then(invocation -> p);		
		assertDoesNotThrow(() -> smt.getProcessMetadata().get(0).addProcessInfo(pi));
		
		assertDoesNotThrow(() -> save(smt));
		
		ServiceMetadataTemplateEntity updated = reload(smt);
		
		assertEquals(2, updated.getProcessMetadata().get(0).getProcessInfo().size());
		assertEquals(pi.getProcess(), updated.getProcessMetadata().get(0).getProcessInfo().get(1).getProcess());
	}
	
	@Test
	void testRemoveProcInfo() {
		ServiceMetadataTemplateEntity smt = new ServiceMetadataTemplateEntity();
		smt.setService(createService());				
		smt.addProcessGroup(createPg(Set.of(createProcess(), createProcess()), Set.of(createEndpoint())));
		
		assertDoesNotThrow(() -> save(smt));
		
		ProcessGroupEntity pg = smt.getProcessMetadata().get(0);
		assertEquals(2, pg.getProcessInfo().size());
		
		pg.removeProcessInfo(pg.getProcessInfo().get(0));
		
		assertDoesNotThrow(() -> save(smt));
		ServiceMetadataTemplateEntity updated = reload(smt);
		
		assertEquals(1, updated.getProcessMetadata().get(0).getProcessInfo().size());		
	}
	
	
	private ServiceEntity createService() {
		ServiceEntity svc = new ServiceEntity();
		svc.setId(new EmbeddedIdentifier("SvcId-T-" + System.nanoTime()));
		em.persist(svc);
		return svc;
	}
	
	private ProcessEntity createProcess() {
		ProcessEntity proc = new ProcessEntity();
		proc.setId(new EmbeddedProcessIdentifier("ProcId-T-" + System.nanoTime()));
		em.persist(proc);
		return proc;
	}
	
	private EndpointEntity createEndpoint() {
		TransportProfileEntity tp = new TransportProfileEntity();
		tp.setId(new EmbeddedIdentifier("Transport-T-1"));
		em.persist(tp);
		EndpointEntity ep = new EndpointEntity();
		ep.setUrl("http://test.holodeck-smp.org/" + System.nanoTime());
		ep.setTransportProfile(tp);
		em.persist(ep);
		return ep;
	}

	private ProcessGroup createPg(Collection<ProcessEntity> procs, Collection<EndpointEntity> eps) {
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
	
	boolean equalPg(ProcessGroup pg1, ProcessGroup pg2) {
		boolean equal = pg1.getProcessInfo().size() == pg2.getProcessInfo().size();
		for(org.holodeckb2b.bdxr.smp.datamodel.ProcessInfo pi1 : pg1.getProcessInfo()) 
			equal &= pg2.getProcessInfo().parallelStream().anyMatch(pi2 -> pi1.getProcessId().toString().equals(pi2.getProcessId().toString()));
		
		equal &= pg1.getEndpoints().size() == pg2.getEndpoints().size();
		for(org.holodeckb2b.bdxr.smp.datamodel.EndpointInfo ei1 : pg1.getEndpoints()) 
			equal &= pg2.getEndpoints().parallelStream().anyMatch(ei2 -> ei1.equals(ei2));
		
		return equal;
	}
	
}