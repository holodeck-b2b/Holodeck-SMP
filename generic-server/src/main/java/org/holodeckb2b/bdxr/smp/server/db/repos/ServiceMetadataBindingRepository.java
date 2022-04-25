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
import org.holodeckb2b.bdxr.smp.datamodel.Identifier;
import org.holodeckb2b.bdxr.smp.server.db.entities.ServiceMetadataBindingE;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * The Spring JPA repository for service meta-data bindings.
 *
 * @author Sander Fieten (sander at holodeck-b2b.org)
 */
public interface ServiceMetadataBindingRepository extends JpaRepository<ServiceMetadataBindingE, Long> {

	@Query("""
			select smb
			from ServiceMetadataBinding smb
			where smb.participant.id.value = :#{#id.value}
			and ( smb.participant.id.scheme = :#{#id.scheme}
				or  smb.participant.id.scheme is null and :#{#id.scheme} is null )
			""")
	List<ServiceMetadataBindingE> findByParticipantId(@Param("id") Identifier partID);

	@Query("""
			select smb
			from ServiceMetadataBinding smb
			where smb.participant.id.value = :#{#pid.value}
			and ( smb.participant.id.scheme = :#{#pid.scheme}
				or  smb.participant.id.scheme is null and :#{#pid.scheme} is null )
            and smb.template.service.id.value = :#{#sid.value}
			and ( smb.template.service.id.scheme = :#{#sid.scheme}
				or  smb.template.service.id.scheme is null and :#{#sid.scheme} is null )
		""")
	List<ServiceMetadataBindingE> findByParticipantAndServiceIds(@Param("pid") Identifier partID, @Param("sid") Identifier serviceID);

}
