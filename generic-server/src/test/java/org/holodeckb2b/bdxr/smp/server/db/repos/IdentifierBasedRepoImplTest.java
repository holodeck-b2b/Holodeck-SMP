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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.holodeckb2b.bdxr.smp.server.CommonServerConfig;
import org.holodeckb2b.bdxr.smp.server.db.entities.EmbeddedProcessIdentifier;
import org.holodeckb2b.bdxr.smp.server.db.entities.IDSchemeEntity;
import org.holodeckb2b.bdxr.smp.server.db.entities.ProcessEntity;
import org.holodeckb2b.bdxr.smp.server.services.core.ConstraintViolationException;
import org.holodeckb2b.bdxr.smp.server.services.core.ConstraintViolationException.ViolationType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ContextConfiguration;

import jakarta.persistence.EntityManager;


@DataJpaTest
@ContextConfiguration(classes = { CommonServerConfig.class })
class IdentifierBasedRepoImplTest {

	@Autowired
	EntityManager 	em;
	
	@Autowired
	ProcessRepository repo;
	
	@Test
	void testSave() {
		ProcessEntity p = new ProcessEntity();
		EmbeddedProcessIdentifier id = new EmbeddedProcessIdentifier("local-test");
		p.setId(id);
		
		assertDoesNotThrow(() -> repo.save(p));
		assertEquals(1, repo.count());
		assertEquals(p, repo.findByIdentifier(id));
	}

	@Test
	void testSaveWithCaseSensitiveScheme() {
		final IDSchemeEntity scheme = new IDSchemeEntity("casesensitive-scheme", true);
		em.persist(scheme);
		
		ProcessEntity p = new ProcessEntity();
		EmbeddedProcessIdentifier id = new EmbeddedProcessIdentifier(scheme, "local-test");
		p.setId(id);
		
		assertDoesNotThrow(() -> repo.save(p));
		assertEquals(1, repo.count());
		assertEquals(p, repo.findByIdentifier(id));
		
		ProcessEntity p2 = new ProcessEntity();
		EmbeddedProcessIdentifier id2 = new EmbeddedProcessIdentifier(scheme, "Local-Test");
		p2.setId(id2);
		
		assertDoesNotThrow(() -> repo.save(p2));		
		assertEquals(2, repo.count());
		assertEquals(p2, repo.findByIdentifier(id2));
	}
		
	@Test
	void testRejectDuplicateNoScheme() {
		ProcessEntity p = new ProcessEntity();
		EmbeddedProcessIdentifier id = new EmbeddedProcessIdentifier("local-test");
		p.setId(id);
		
		assertDoesNotThrow(() -> repo.save(p));
		assertEquals(1, repo.count());
		assertEquals(p, repo.findByIdentifier(id));
		
		ProcessEntity p2 = new ProcessEntity();
		EmbeddedProcessIdentifier id2 = new EmbeddedProcessIdentifier("local-test");
		p2.setId(id2);
		
		ConstraintViolationException cve = assertThrows(ConstraintViolationException.class, () -> repo.save(p2));
		assertEquals(ViolationType.DUPLICATE_ID, cve.getViolation());
		assertEquals(p2, cve.getSubject());
		
		assertEquals(1, repo.count());		
	}
	
	@Test
	void testRejectDuplicateCaseInsensitiveScheme() {
		final IDSchemeEntity scheme = new IDSchemeEntity("casesensitive-scheme", false);
		em.persist(scheme);
		
		ProcessEntity p = new ProcessEntity();
		EmbeddedProcessIdentifier id = new EmbeddedProcessIdentifier("local-test");
		p.setId(id);
		
		assertDoesNotThrow(() -> repo.save(p));
		assertEquals(1, repo.count());
		assertEquals(p, repo.findByIdentifier(id));
		
		ProcessEntity p2 = new ProcessEntity();
		EmbeddedProcessIdentifier id2 = new EmbeddedProcessIdentifier("Local-Test");
		p2.setId(id2);
		
		ConstraintViolationException cve = assertThrows(ConstraintViolationException.class, () -> repo.save(p2));
		assertEquals(ViolationType.DUPLICATE_ID, cve.getViolation());
		assertEquals(p2, cve.getSubject());
		
		assertEquals(1, repo.count());		
	}
	
	@Test
	void testFindByIdentifier() {
		IDSchemeEntity ids1 = new IDSchemeEntity("T-IDS-1", true); 
		IDSchemeEntity ids2 = new IDSchemeEntity("T-IDS-2", false);
		em.persist(ids1);
		em.persist(ids2);
		
		for(int i = 0; i < 10; i++) {
			ProcessEntity p = new ProcessEntity();
			p.setId(new EmbeddedProcessIdentifier(ids1, "T-PID-"+i));
			repo.save(p);
			p = new ProcessEntity();
			p.setId(new EmbeddedProcessIdentifier(ids2, "T-PID-"+i));
			repo.save(p);
			p = new ProcessEntity();
			p.setId(new EmbeddedProcessIdentifier("T-PID-"+i));
			repo.save(p);			
		}
		
		assertNotNull(repo.findByIdentifier(new EmbeddedProcessIdentifier(ids1, "T-PID-4")));
		assertNull(repo.findByIdentifier(new EmbeddedProcessIdentifier(ids1, "T-pid-4")));
		assertNotNull(repo.findByIdentifier(new EmbeddedProcessIdentifier(ids2, "T-pid-4")));
		// Without scheme default is to threat identifier case insensitive
		assertNotNull(repo.findByIdentifier(new EmbeddedProcessIdentifier("T-pid-4")));		
	}
	
}
