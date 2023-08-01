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

import org.holodeckb2b.bdxr.smp.server.db.entities.ProcessE;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * The Spring JPA repository for process meta-data.
 *
 * @author Sander Fieten (sander at holodeck-b2b.org)
 */
public interface ProcessRepository extends JpaRepository<ProcessE, Long>, ProcessSearchByIdRepo {

	@Query("""
			select count(*)
			from ServiceMetadataTemplate smt
			where smt in (select pg.template
						  from ProcessGroup pg
						  where pg in (select pi.procgroup from ProcessInfo pi where pi.process = :proc)
						 )
		""")
	Integer getNumberOfTemplates(@Param("proc") ProcessE proc);
}
