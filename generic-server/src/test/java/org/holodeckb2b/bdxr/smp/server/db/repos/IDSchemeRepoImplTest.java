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
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.holodeckb2b.bdxr.smp.server.CommonServerConfig;
import org.holodeckb2b.bdxr.smp.server.db.entities.IDSchemeEntity;
import org.holodeckb2b.bdxr.smp.server.services.core.ConstraintViolationException;
import org.holodeckb2b.bdxr.smp.server.services.core.ConstraintViolationException.ViolationType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ContextConfiguration;


@DataJpaTest
@ContextConfiguration(classes = { CommonServerConfig.class })
class IDSchemeRepoImplTest {

	@Autowired
	IDSchemeRepository repo;	
	
	@Test
	void testSave() {
		IDSchemeEntity scheme = new IDSchemeEntity("test-scheme-caseinsensitive", false, null, null, null);
		
		assertDoesNotThrow(() -> repo.save(scheme));
		assertEquals(1, repo.count());
		assertEquals(scheme, repo.findByIdentifier(scheme.getId()));

		IDSchemeEntity scheme2 = new IDSchemeEntity("Test-Scheme-CaseInsensitive", false, null, null, null);
		
		assertDoesNotThrow(() -> repo.save(scheme2));
		assertEquals(2, repo.count());
		assertEquals(scheme2, repo.findByIdentifier(scheme2.getId()));
	}
	
	@Test
	void testRejectDuplicates() {
		final IDSchemeEntity scheme = new IDSchemeEntity("casesensitive-scheme", false, null, null, null);
		
		assertDoesNotThrow(() -> repo.save(scheme));
		assertEquals(1, repo.count());
		assertEquals(scheme, repo.findByIdentifier(scheme.getId()));

		IDSchemeEntity scheme2 = new IDSchemeEntity(scheme.getSchemeId(), true, null, null, null);
		
		ConstraintViolationException cve = assertThrows(ConstraintViolationException.class, () -> repo.save(scheme2));
		assertEquals(ViolationType.DUPLICATE_ID, cve.getViolation());
		assertEquals(scheme2, cve.getSubject());
		
		assertEquals(1, repo.count());		
	}
}
