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
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Random;
import java.util.Set;

import org.holodeckb2b.bdxr.smp.server.CommonServerConfig;
import org.holodeckb2b.bdxr.smp.server.auditlogging.AuditLogRecord;
import org.holodeckb2b.bdxr.smp.server.db.entities.AuditLogEntity;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.test.context.ContextConfiguration;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;

@SuppressWarnings("unchecked")
@DataJpaTest
@ContextConfiguration(classes = { CommonServerConfig.class })
class AuditLogRepositoryTest {

	@Autowired
	AuditLogRepository	repo;
	
	@Test
	void testDeleteBefore() {		
		final Instant now = Instant.now();		
		for(int i = 0; i < 30; i++) 
			repo.save(new AuditLogEntity(
					new AuditLogRecord(now.minus(i, ChronoUnit.DAYS), "TestUser", "DeleteOld", null, null)));
		
		assertEquals(30, repo.count(AuditLogRepository.all()));
		
		assertDoesNotThrow(() -> repo.deleteByTimestampBefore(now.minus(15, ChronoUnit.DAYS)));
		
		// As the method above will not include the entry 15 days old, only 14 entries will be deleted and 16 remain
		assertEquals(16, repo.count(AuditLogRepository.all()));		
	}

	@Test
	void testGetUsers() {					
		final Instant now = Instant.now();
		Random random = new Random();
		for (int i = 0; i < 5; i++) 
			for(int j = 0; j < random.nextInt(10) + 1; j++)
				repo.save(new AuditLogEntity(
							new AuditLogRecord(now.minus(1, ChronoUnit.DAYS), "Tester-" + i, "CountUsers", null, null)));
				
		Set<String> users = assertDoesNotThrow(() -> repo.getAvailableUsers());
		
		assertEquals(5, users.size());
		for (int i = 0; i < 5; i++) 
			assertTrue(users.contains("Tester-" + i));
	}

	@Test
	void testGetActions() {					
		final Instant now = Instant.now();
		Random random = new Random();
		for (int i = 0; i < 5; i++) 
			for(int j = 0; j < random.nextInt(10) + 1; j++)
				repo.save(new AuditLogEntity(
						new AuditLogRecord(now.minus(1, ChronoUnit.DAYS), "Tester", "CountActions-" + i, null, null)));
		
		Set<String> actions = assertDoesNotThrow(() -> repo.getAvailableActions());
		
		assertEquals(5, actions.size());
		for (int i = 0; i < 5; i++) 
			assertTrue(actions.contains("CountActions-" + i));
	}
	
	@Test
	void testGetSubjects() {					
		final Instant now = Instant.now();
		Random random = new Random();
		for (int i = 0; i < 5; i++) 
			for(int j = 0; j < random.nextInt(10) + 1; j++)
				repo.save(new AuditLogEntity(
						new AuditLogRecord(now.minus(1, ChronoUnit.DAYS), "Tester", "CountSubjects", "S-" + i, null)));
		
		Set<String> subjects = assertDoesNotThrow(() -> repo.getAvailableSubjects());
		
		assertEquals(5, subjects.size());
		for (int i = 0; i < 5; i++) 
			assertTrue(subjects.contains("S-" + i));
	}
	
	@Test
	void testForUser() {
		CriteriaBuilder cb = mock(CriteriaBuilder.class);
		CriteriaQuery<AuditLogEntity> query = mock(CriteriaQuery.class);
		Root<AuditLogEntity> root = mock(Root.class);
		
		String user = "Tester";
		
		Specification<AuditLogEntity> spec = AuditLogRepository.forUser(user);
		
		assertEquals(cb.equal(root.get("username"), user), spec.toPredicate(root, query, cb));
	}
	
	@Test
	void testOfAction() {
		CriteriaBuilder cb = mock(CriteriaBuilder.class);
		CriteriaQuery<AuditLogEntity> query = mock(CriteriaQuery.class);
		Root<AuditLogEntity> root = mock(Root.class);
		
		String action = "FindMe";
		
		Specification<AuditLogEntity> spec = AuditLogRepository.ofAction(action);
		
		assertEquals(cb.equal(root.get("action"), action), spec.toPredicate(root, query, cb));
	}

	@Test
    void testOnSubject() {
        CriteriaBuilder cb = mock(CriteriaBuilder.class);
        CriteriaQuery<AuditLogEntity> query = mock(CriteriaQuery.class);
        Root<AuditLogEntity> root = mock(Root.class);

        String subject = "testSubject";

        Specification<AuditLogEntity> spec = AuditLogRepository.onSubject(subject);

        assertEquals(cb.equal(root.get("subject"), subject), spec.toPredicate(root, query, cb));
    }
	
	@Test
	void testAfter() {
		CriteriaBuilder cb = mock(CriteriaBuilder.class);
        CriteriaQuery<AuditLogEntity> query = mock(CriteriaQuery.class);
        Root<AuditLogEntity> root = mock(Root.class);
        
        Instant from = Instant.now().minus(8, ChronoUnit.HOURS);
        
        Specification<AuditLogEntity> spec = AuditLogRepository.after(from);
        
        assertEquals(cb.greaterThanOrEqualTo(root.get("timestamp"), from), spec.toPredicate(root, query, cb));
	}
	
	@Test
	void testBefore() {
		CriteriaBuilder cb = mock(CriteriaBuilder.class);
        CriteriaQuery<AuditLogEntity> query = mock(CriteriaQuery.class);
        Root<AuditLogEntity> root = mock(Root.class);
        
        Instant to = Instant.now().minus(4, ChronoUnit.HOURS);
        
        Specification<AuditLogEntity> spec = AuditLogRepository.before(to);
        
        assertEquals(cb.lessThan(root.get("timestamp"), to), spec.toPredicate(root, query, cb));
	}
	
	@Test
	void testOrderBy() {
		CriteriaBuilder cb = mock(CriteriaBuilder.class);
		CriteriaQuery<AuditLogEntity> query = mock(CriteriaQuery.class);
		Root<AuditLogEntity> root = mock(Root.class);
		
		Specification<AuditLogEntity> spec = AuditLogRepository.orderByTimestamp(AuditLogRepository.all());
		
		assertEquals(cb.desc(root.get("timestamp")), spec.toPredicate(root, query, cb));
	}
	
}
