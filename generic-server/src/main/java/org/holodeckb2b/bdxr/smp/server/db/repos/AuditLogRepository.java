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

import java.time.Instant;
import java.util.Set;

import org.holodeckb2b.bdxr.smp.server.db.entities.AuditLogEntity;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;

/**
 * The Spring JPA repository for audit log entries. Because of the specific nature of the audit log, this repository 
 * only defines methods to add a record, delete records older than a certain timestamp and query the log. 
 * <p>
 * For querying it uses the {@link JpaSpecificationExecutor} interface to allow easy and flexible creation of queries.
 * The following code snippet shows how this can be used to execute a query to get all audit log entries of a action
 * by a certain user:
 * <pre>{@code
 *    Specification<AuditLogEntity> srchSpec = AuditLogRepository.forUser(username))
 *                                                               .and(AuditLogRepository.ofAction(action));
 *    auditLog.findAll(AuditLogRepository.orderByTimestamp(srchSpec));
 * }</pre>
 *
 * @author Sander Fieten (sander at holodeck-b2b.org)
 */
public interface AuditLogRepository extends Repository<AuditLogEntity, Long>, JpaSpecificationExecutor<AuditLogEntity> {

	/**
	 * Saves the audit log entry to the database.
	 * 
	 * @param auditRecord	the audit information to save
	 * @return	the saved audit log entry
	 */
	AuditLogEntity save(AuditLogEntity auditRecord);
	
	/**
	 * Deletes all audit log entries older than the given timestamp. Note that entries with the same timestamp as 
	 * provided are not deleted.
	 *
	 * @param t	the timestamp before which all audit log entries should be deleted
	 */
	void deleteByTimestampBefore(Instant t);

	/**
	 * @return the list of user(names) that are currently available in the audit log
	 */
	@Query("SELECT DISTINCT(al.username) FROM AuditLog al")
	Set<String> getAvailableUsers();

	/**
	 * @return the list of actions that are currently available in the audit log
	 */
	@Query("SELECT DISTINCT(al.action) FROM AuditLog al")
	Set<String> getAvailableActions();
	
	/**
	 * @return the list of subjects that are currently available in the audit log
	 */
	@Query("SELECT DISTINCT(al.subject) FROM AuditLog al")
	Set<String> getAvailableSubjects();
	
	/**
	 * Returns a query specification that matches all audit log entries. This can be used as a starting point for 
	 * building more complex queries, i.e.:<pre>{@code
	 * Specification<AuditLogEntity> srchSpec = AuditLogRepository.all()
	 *                                                           .and(AuditLogRepository.forUser(username))
	 *                                                           .and(AuditLogRepository.ofAction(action));
	 * }</pre>
	 * 
	 * @return	query specification matching all audit log entries
	 */
	static Specification<AuditLogEntity> all() {
		return (root, query, cb) -> cb.isTrue(cb.literal(true));
	}

	/**
	 * Returns a query specification to match only the audit log entries of the given user
	 * 
	 * @param user	the username to select on
	 * @return	query specification matching only the audit log entries of the given user
	 */
	static Specification<AuditLogEntity> forUser(final String user) {
		return (root, query, cb) -> cb.equal(root.get("username"), user);
	}
	
	/**
	 * Returns a query specification to match only the audit log entries of the given action
	 * 
	 * @param action	the action to select on
	 * @return query specification matching only the audit log entries of the given action
	 */
	static Specification<AuditLogEntity> ofAction(final String action) {
		return (root, query, cb) -> cb.equal(root.get("action"), action);
	}
		
	/**
	 * Returns a query specification to match only the audit log entries of the given subject
	 * 
	 * @param subject the subject to select on
	 * @return query specification matching only the audit log entries of the given subject
	 */
	static Specification<AuditLogEntity> onSubject(final String subject) {
		return (root, query, cb) -> cb.equal(root.get("subject"), subject);
	}
	
	/**
	 * Returns a query specification to match only the audit log entries after or equal to the given timestamp
	 * 
	 * @param t	the timestamp to select on	
	 * @return query specification matching only the audit log entries after or equal to the given timestamp
	 */
	static Specification<AuditLogEntity> after(final Instant t) {
		return (root, query, cb) -> cb.greaterThanOrEqualTo(root.get("timestamp"), t);
	}

	/**
	 * Returns a query specification to match only the audit log entries before the given timestamp
	 * 
	 * @param t the timestamp to select on
	 * @return query specification matching only the audit log entries before the given timestamp
	 */
	static Specification<AuditLogEntity> before(final Instant t) {
		return (root, query, cb) -> cb.lessThan(root.get("timestamp"), t);
	}
	
	/**
	 * Ensures that the query result is ordered on the timestamp of the audit log entries with the newest entries first.
	 * 
	 * @param spec	the current query specification
	 * @return	the query specification with the ordering added
	 */
	static Specification<AuditLogEntity> orderByTimestamp(Specification<AuditLogEntity> spec) {
        return (root, query, builder) -> {
            query.orderBy(builder.desc(root.get("timestamp")));              
            return spec.toPredicate(root, query, builder);
        };
	}
}
