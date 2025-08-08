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
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collection;
import java.util.Random;

import org.holodeckb2b.bdxr.smp.server.CommonServerConfig;
import org.holodeckb2b.bdxr.smp.server.db.entities.EmbeddedIdentifier;
import org.holodeckb2b.bdxr.smp.server.db.entities.IDSchemeEntity;
import org.holodeckb2b.bdxr.smp.server.db.entities.ParticipantEntity;
import org.holodeckb2b.bdxr.smp.server.db.entities.ServiceMetadataTemplateEntity;
import org.holodeckb2b.bdxr.smp.server.db.entities.ServiceEntity;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ContextConfiguration;

import jakarta.persistence.EntityManager;

@DataJpaTest
@ContextConfiguration(classes = { CommonServerConfig.class })
class ParticipantRepositoryTest {

	@Autowired
	ParticipantRepository 	repo;

	@Autowired
	EntityManager		em;
	
	@Test
	void testCountParticipantsSupporting() {
		ServiceEntity svc = new ServiceEntity();
		svc.setId(new EmbeddedIdentifier("SvcId-T-1"));
		em.persist(svc);		
		ServiceMetadataTemplateEntity smt = new ServiceMetadataTemplateEntity();
		smt.setService(svc);		
		em.persist(smt);
		
		for(int i = 0; i < 10; i++) {
			ParticipantEntity p = new ParticipantEntity();
			p.setId(new EmbeddedIdentifier("PartId-T-"+i));
			if (i % 2 == 0)
				p.addBinding(smt);			
			repo.save(p);
		}
		
		assertEquals(5, assertDoesNotThrow(() -> repo.countParticipantsSupporting(smt.getId())));
	}
	
	@Test
	void testFindParticipantsSupporting() {
		ServiceEntity svc = new ServiceEntity();
		svc.setId(new EmbeddedIdentifier("SvcId-T-1"));
		em.persist(svc);		
		ServiceMetadataTemplateEntity smt1 = new ServiceMetadataTemplateEntity();
		smt1.setService(svc);		
		em.persist(smt1);
		ServiceMetadataTemplateEntity smt2 = new ServiceMetadataTemplateEntity();
		smt2.setService(svc);		
		em.persist(smt2);
		
		Random r = new Random();
		int smt1Count = 0;
		for(int i = 0; i < 100; i++) {
			ParticipantEntity p = new ParticipantEntity();
			p.setId(new EmbeddedIdentifier("PartId-T-"+i));
			switch (r.nextInt(3)) {
			case 0 :
				break;
			case 1 :
				p.addBinding(smt1);
				smt1Count++;
				break;
			case 2 :
				p.addBinding(smt2);
				break;
			case 3 :
				p.addBinding(smt1);
				p.addBinding(smt2);
				break;
			}
			repo.save(p);
		}
		
		Page<ParticipantEntity> result = repo.findByBindingsContains(smt1, PageRequest.of(0, 100));
		assertEquals(smt1Count, result.getTotalElements());
		assertTrue(result.stream().allMatch(p -> p.getBoundSMT().contains(smt1)));
	}
	
	@Test
	void testFindByName() {
		for(int i = 0; i < 20; i++) {
			ParticipantEntity p = new ParticipantEntity();
			p.setId(new EmbeddedIdentifier("PartId-T-"+i));
			p.setName("Participant " + i);
			repo.save(p);
		}
		
		// No result because argument should be in lower case
		assertTrue(repo.findByLcNameStartsWith("Participant").isEmpty());
		
		Collection<ParticipantEntity> result = repo.findByLcNameStartsWith("participant 4");
		assertEquals(1, result.size());
		assertEquals(new EmbeddedIdentifier("PartId-T-4"), result.iterator().next().getId());
		
		result = repo.findByLcNameStartsWith("participant 1");
		assertEquals(11, result.size());
		assertTrue(result.stream().allMatch(p -> p.getId().toString().startsWith("partid-t-1")));
	}
	
	@Test
	void testFindByAdditionalId() {
		IDSchemeEntity ids1 = new IDSchemeEntity("T-IDS-1", true, null, null, null); 
		IDSchemeEntity ids2 = new IDSchemeEntity("T-IDS-2", false, null, null, null);
		em.persist(ids1);
		em.persist(ids2);
			
		ParticipantEntity p1 = new ParticipantEntity();
		p1.setId(new EmbeddedIdentifier("PartId-T-1"));
		p1.addAdditionalId(new EmbeddedIdentifier(ids1, "ADDID-1"));
		p1.addAdditionalId(new EmbeddedIdentifier(ids1, "ADDID-2"));
		p1.addAdditionalId(new EmbeddedIdentifier(ids2, "ADDID-1"));
		repo.save(p1);

		ParticipantEntity p2 = new ParticipantEntity();
		p2.setId(new EmbeddedIdentifier("PartId-T-2"));
		p2.addAdditionalId(new EmbeddedIdentifier(ids1, "addid-1"));
		p2.addAdditionalId(new EmbeddedIdentifier(ids2, "ADDID-2"));
		repo.save(p2);
		
		ParticipantEntity p3 = new ParticipantEntity();
		p3.setId(new EmbeddedIdentifier("PartId-T-3"));
		p3.addAdditionalId(new EmbeddedIdentifier(ids2, "addid-2"));
		repo.save(p3);

		Collection<ParticipantEntity> result = repo.findByAdditionalId(new EmbeddedIdentifier(ids1, "addid-1"));
		assertEquals(1, result.size());
		assertTrue(result.contains(p2));
	
		result = repo.findByAdditionalId(new EmbeddedIdentifier(ids2, "addid-1"));
		assertEquals(1, result.size());
		assertTrue(result.contains(p1));
		
		result = repo.findByAdditionalId(new EmbeddedIdentifier(ids2, "addid-2"));
		assertEquals(2, result.size());
		assertTrue(result.contains(p2));
		assertTrue(result.contains(p3));
	}
	
	@Test
	void testFindBySMLRegState() {
		for(int i = 0; i < 20; i++) {
			ParticipantEntity p = new ParticipantEntity();
			p.setId(new EmbeddedIdentifier("PartId-T-"+i));
			p.setName("Participant " + i);
			p.setRegisteredInSML(null);
			repo.save(p);
		}
		
		Page<ParticipantEntity> result = repo.findByRegisteredInSML(true, PageRequest.of(0, 20));
		assertEquals(10, result.getTotalElements());
		
		result = repo.findByRegisteredInSML(false, PageRequest.of(0, 5));
		assertEquals(10, result.getTotalElements());
		assertEquals(2, result.getTotalPages());
	}
	
	@Test
	void testFindByDirectoryState() {
		for(int i = 0; i < 20; i++) {
			ParticipantEntity p = new ParticipantEntity();
			p.setId(new EmbeddedIdentifier("PartId-T-"+i));
			p.setName("Participant " + i);
			p.setPublishedInDirectory(i % 2 == 0);
			repo.save(p);
		}
		
		Page<ParticipantEntity> result = repo.findByPublishedInDirectory(false, PageRequest.of(0, 20));
		assertEquals(10, result.getTotalElements());
		
		result = repo.findByPublishedInDirectory(true, PageRequest.of(0, 5));
		assertEquals(10, result.getTotalElements());
		assertEquals(2, result.getTotalPages());
	}
	
	@Test
	void testExistsPublished() {
		for(int i = 0; i < 10; i++) {
			ParticipantEntity p = new ParticipantEntity();
			p.setId(new EmbeddedIdentifier("PartId-T-"+i));
			p.setName("Participant " + i);
			p.setPublishedInDirectory(false);
			repo.save(p);
		}
		
		assertFalse(repo.existsByPublishedInDirectory(true));
		
		ParticipantEntity p = new ParticipantEntity();
		p.setId(new EmbeddedIdentifier("PartId-T-10"));
		p.setName("Participant 10");
		p.setPublishedInDirectory(true);
		repo.save(p);
		
		assertTrue(repo.existsByPublishedInDirectory(true));		
	}
	
	@Test	
	void testUnregisterAllFromSML() {
		for(int i = 0; i < 10; i++) {
			ParticipantEntity p = new ParticipantEntity();
			p.setId(new EmbeddedIdentifier("PartId-T-"+i));
			p.setRegisteredInSML(true);
			repo.save(p);
		}
		
		assertDoesNotThrow(() -> repo.unregisterAllFromSML());
		assertTrue(repo.findAll().stream().noneMatch(p -> p.isRegisteredInSML()));
	}

}
