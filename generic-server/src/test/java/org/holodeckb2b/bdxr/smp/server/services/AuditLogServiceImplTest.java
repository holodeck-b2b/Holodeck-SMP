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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collections;

import org.holodeckb2b.bdxr.smp.server.CommonServerConfig;
import org.holodeckb2b.bdxr.smp.server.auditlogging.AuditLogRecord;
import org.holodeckb2b.bdxr.smp.server.auditlogging.AuditLogSearchCriteria;
import org.holodeckb2b.bdxr.smp.server.auditlogging.AuditLogService;
import org.holodeckb2b.bdxr.smp.server.db.entities.AuditLogEntity;
import org.holodeckb2b.bdxr.smp.server.db.repos.AuditLogRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

@SuppressWarnings("unchecked")
@SpringBootTest(classes = { CommonServerConfig.class })
class AuditLogServiceImplTest {

	@MockitoBean
	AuditLogRepository repo;
	
	@Autowired
	AuditLogService auditLogService;
	
	@Test
	void testAddRecord() {
		final AuditLogRecord newRecord = new AuditLogRecord(Instant.now(), "Tester", "Testing", "AuditLog", null);
		
		assertDoesNotThrow(() -> auditLogService.log(newRecord));
		
		ArgumentCaptor<AuditLogEntity> captor = ArgumentCaptor.forClass(AuditLogEntity.class);
		verify(repo).save(captor.capture());
		
		assertEquals(newRecord, captor.getValue().toAuditLogRecord());
	}

	@Test
	void testFindAll() {
		ArgumentCaptor<Specification<AuditLogEntity>> captor = ArgumentCaptor.forClass(Specification.class);
        
        Page<AuditLogEntity> result = mock(Page.class);
        when(result.getContent()).thenReturn(Collections.emptyList());
        when(repo.findAll(any(Specification.class), any(PageRequest.class))).thenReturn(result);
        
		assertDoesNotThrow(() -> 
					auditLogService.getAuditLogRecords(new AuditLogSearchCriteria(null, null, null, null, null),
													   PageRequest.of(0, 100)));
				
		verify(repo).findAll(captor.capture(), any(PageRequest.class));
		
		CriteriaBuilder cb = mock(CriteriaBuilder.class);
        CriteriaQuery<AuditLogEntity> query = mock(CriteriaQuery.class);
        Root<AuditLogEntity> root = mock(Root.class);
        
        Predicate predicate = captor.getValue().toPredicate(root, query, cb);
		
        verify(cb).isTrue(any());
		verify(cb).literal(true);
		verify(cb).desc(root.get("timestamp"));
		verifyNoMoreInteractions(cb);
	}

	@Test
	void testFindByCriteria() {
		AuditLogSearchCriteria criteria = new AuditLogSearchCriteria(new TestUser().getUsername(), 
																	"AuditLog", "Testing", 
																	Instant.now().minus(4, ChronoUnit.HOURS),
																	Instant.now().minus(2, ChronoUnit.HOURS));
		
		CriteriaBuilder cb = mock(CriteriaBuilder.class);
        CriteriaQuery<AuditLogEntity> query = mock(CriteriaQuery.class);
        Root<AuditLogEntity> root = mock(Root.class);
        ArgumentCaptor<Specification<AuditLogEntity>> captorSpec = ArgumentCaptor.forClass(Specification.class);
        ArgumentCaptor<PageRequest> captorPageReq = ArgumentCaptor.forClass(PageRequest.class);
        
        Page<AuditLogEntity> result = mock(Page.class);
        when(result.getContent()).thenReturn(Collections.emptyList());
        when(repo.findAll(any(Specification.class), any(PageRequest.class))).thenReturn(result);
        
        PageRequest subsetRequest = PageRequest.of(3, 40);
		assertDoesNotThrow(() -> auditLogService.getAuditLogRecords(criteria, subsetRequest));
		
		verify(repo).findAll(captorSpec.capture(), captorPageReq.capture());
		
		Predicate P = AuditLogRepository.all()
										.and(AuditLogRepository.forUser(criteria.username()))
										.and(AuditLogRepository.ofAction(criteria.action()))
										.and(AuditLogRepository.onSubject(criteria.subject()))
										.and(AuditLogRepository.after(criteria.from()))
										.and(AuditLogRepository.before(criteria.to())).toPredicate(root, query, cb);
		
		assertEquals(P, captorSpec.getValue().toPredicate(root, query, cb));
		
		assertEquals(subsetRequest, captorPageReq.getValue());
	}
}
