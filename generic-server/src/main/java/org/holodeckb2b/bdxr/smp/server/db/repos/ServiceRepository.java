/*
 * Copyright (C) 2022 The Holodeck B2B Team
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

import java.util.List;
import org.holodeckb2b.bdxr.smp.server.db.entities.IdentifierE;
import org.holodeckb2b.bdxr.smp.server.db.entities.ServiceE;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * The Spring JPA repository for service meta-data.
 *
 * @author Sander Fieten (sander at holodeck-b2b.org)
 */
public interface ServiceRepository extends JpaRepository<ServiceE, Long> {

	@Query("""
        select s from Service s where s.id.value = :#{#id.value}
        		and ( s.id.scheme = :#{#id.scheme} or s.id.scheme is null and :#{#id.scheme} is null )
		""")
	List<ServiceE> findByIdentifier(@Param("id") IdentifierE id);

	@Query("select count(*) from ServiceMetadataTemplate smt where smt.service = :svc")
	Integer getNumberOfTemplates(@Param("svc") ServiceE svc);
}
