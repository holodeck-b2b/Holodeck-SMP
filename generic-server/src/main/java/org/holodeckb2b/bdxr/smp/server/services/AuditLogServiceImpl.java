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

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.Set;

import org.holodeckb2b.bdxr.smp.server.auditlogging.AuditLogRecord;
import org.holodeckb2b.bdxr.smp.server.auditlogging.AuditLogSearchCriteria;
import org.holodeckb2b.bdxr.smp.server.auditlogging.AuditLogService;
import org.holodeckb2b.bdxr.smp.server.db.entities.AuditLogEntity;
import org.holodeckb2b.bdxr.smp.server.db.repos.AuditLogRepository;
import org.holodeckb2b.bdxr.smp.server.services.core.PersistenceException;
import org.holodeckb2b.commons.util.Utils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.extern.slf4j.Slf4j;

/**
 * Implements the {@link AuditLogService}
 * 
 * @author Sander Fieten (sander at holodeck-b2b.org)
 */
@Service
@Slf4j
public class AuditLogServiceImpl implements AuditLogService {

	@Autowired
	private AuditLogRepository	auditLog;
	
	/**
	 * The retention period for audit log records (in days)
	 */
	@Value("${smp.auditlog.retention:30}")
	private int retentionPeriod;
	
	@Override
	public void log(AuditLogRecord record) {
		log.trace("Adding audit log record"); 
		auditLog.save(new AuditLogEntity(record));
		log.debug("Added audit log record : t={}, u={}, a={}, s={})", record.timestamp(), record.username(), 
				record.action(), record.subject());			
	}

	@Override
	public Set<String> getAvailableUsernames() throws PersistenceException {
		try {
			return auditLog.getAvailableUsers();
		} catch (Throwable t) {
			log.error("Error while retrieving available usernames: {}", Utils.getExceptionTrace(t));
			throw new PersistenceException("Failed to retrieve available usernames", t);
		}
	}
	
	@Override
	public Set<String> getAvailableActions() throws PersistenceException {
		try {
			return auditLog.getAvailableActions();
		} catch (Throwable t) {
			log.error("Error while retrieving available actions: {}", Utils.getExceptionTrace(t));
			throw new PersistenceException("Failed to retrieve available actions", t);
		}
	}

	@Override
	public Set<String> getAvailableSubjects() throws PersistenceException {
		try {
			return auditLog.getAvailableSubjects();
		} catch (Throwable t) {
			log.error("Error while retrieving available subjects: {}", Utils.getExceptionTrace(t));
			throw new PersistenceException("Failed to retrieve available subjects", t);
		}
	}

	@Override
	public Page<AuditLogRecord> getAuditLogRecords(AuditLogSearchCriteria searchCriteria, PageRequest request) 
																						throws PersistenceException {
		Specification<AuditLogEntity> querySpec = AuditLogRepository.all();
		
		StringBuilder logMsg = new StringBuilder("Retrieving page {} with {} audit log records matching u=");
		if (searchCriteria != null && !Utils.isNullOrEmpty(searchCriteria.username())) {
			querySpec = querySpec.and(AuditLogRepository.forUser(searchCriteria.username()));
			logMsg.append(searchCriteria.username());
		} else 
			logMsg.append("ALL");
		logMsg.append(", a=");
		if (searchCriteria != null && !Utils.isNullOrEmpty(searchCriteria.action())) { 
			querySpec = querySpec.and(AuditLogRepository.ofAction(searchCriteria.action()));
			logMsg.append(searchCriteria.action());
		} else
			logMsg.append("ALL");
		logMsg.append(", s=");
		if (searchCriteria != null && !Utils.isNullOrEmpty(searchCriteria.subject())) {
			querySpec = querySpec.and(AuditLogRepository.onSubject(searchCriteria.subject()));
			logMsg.append(searchCriteria.subject());
		} else
			logMsg.append("ALL");
		logMsg.append(", from=");
		if (searchCriteria != null && searchCriteria.from() != null) {
			querySpec = querySpec.and(AuditLogRepository.after(searchCriteria.from()));
			logMsg.append(String.format("%1$tFT%1$tT", searchCriteria.from().atZone(ZoneOffset.UTC)));
		} else
			logMsg.append("ANYTIME");
		logMsg.append(", to=");
		if (searchCriteria != null && searchCriteria.to() != null) {
			querySpec = querySpec.and(AuditLogRepository.before(searchCriteria.to()));
			logMsg.append(String.format("%1$tFT%1$tT", searchCriteria.to().atZone(ZoneOffset.UTC)));
		} else
			logMsg.append("ANYTIME");
		 
		try {
			log.trace(logMsg.toString(), request.getPageNumber(), request.getPageSize());
			return auditLog.findAll(AuditLogRepository.orderByTimestamp(querySpec), request)
							.map(auditLogRecord -> auditLogRecord.toAuditLogRecord());			 
		} catch (Throwable t) {
			log.error("Error while retrieving audit log records: {}", Utils.getExceptionTrace(t));
			throw new PersistenceException("Failed to retrieve audit log records", t);
		}
	}
	
	/**
	 * Cleans up the audit log, removing entries older than the configured retention period. This is a scheduled action
	 * that runs once a day.
	 */
	@Scheduled(cron = "@daily")
	@Transactional
	public void cleanUp() {
		log.trace("Cleaning up audit log");
		try {
			auditLog.deleteByTimestampBefore(Instant.now().minus(retentionPeriod, ChronoUnit.DAYS));
			log.debug("Audit log cleaned up");
		} catch (Exception e) {
			log.error("An error occurred cleaning up the audit log : {}", Utils.getExceptionTrace(e));
		}
	}
}
