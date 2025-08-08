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

import java.util.Collection;

import org.holodeckb2b.bdxr.smp.server.db.entities.EndpointEntity;
import org.holodeckb2b.bdxr.smp.server.db.entities.ProcessEntity;
import org.holodeckb2b.bdxr.smp.server.db.entities.ServiceMetadataTemplateEntity;
import org.holodeckb2b.bdxr.smp.server.db.entities.ServiceEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * The Spring JPA repository for service meta-data templates.
 *
 * @author Sander Fieten (sander at holodeck-b2b.org)
 */
public interface ServiceMetadataTemplateRepository extends JpaRepository<ServiceMetadataTemplateEntity, Long> {

	/**
	 * Find the Service Metadata Templates for the given <i>Service</i>.
	 * 
	 * @param service	the entity object representing the Service
	 * @return			the collection of Service Metadata Templates for the given <i>Service</i>
	 */
	Collection<ServiceMetadataTemplateEntity> findByService(ServiceEntity service);
	
	/**
	 * Finds the Service Metadata Templates that are bound to the given Process.
	 * 
	 * @param process	the entity object representing the Process
	 * @return			the collection of Service Metadata Templates that are bound to the given Process
	 */
	@Query("""
			select smt
			from ServiceMetadataTemplate smt
			where smt in (select pg.template
						  from ProcessGroup pg
						  where pg in (select pi.procgroup from ProcessInfo pi where pi.process = :proc)
						 )
		""")
	Collection<ServiceMetadataTemplateEntity> findByProcess(@Param("proc") ProcessEntity process);
	
	/**
	 * Finds the Service Metadata Templates that use to the given Endpoint.
	 * 
	 * @param endpoint	the entity object representing the Endpoint
	 * @return			the collection of Service Metadata Templates that use the given Endpoint
	 */
	@Query("""
			select smt
			from ServiceMetadataTemplate smt
			where smt in (select pg.template
						  from ProcessGroup pg
						  where :ep member of pg.endpoints)
		""")
	Collection<ServiceMetadataTemplateEntity> findByEndpoint(@Param("ep") EndpointEntity endpoint);
}
