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

import org.holodeckb2b.bdxr.smp.server.db.entities.ParticipantEntity;
import org.holodeckb2b.bdxr.smp.server.db.entities.ServiceMetadataTemplateEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.NativeQuery;
import org.springframework.data.jpa.repository.Query;

/**
 * The Spring JPA repository for participant meta-data.
 *
 * @author Sander Fieten (sander at holodeck-b2b.org)
 */
public interface ParticipantRepository extends JpaRepository<ParticipantEntity, Long>, ParticipantIdCapableRepo {

	/**
	 * Find <i>Participants</i> whose lower case name starts with the given string.
	 * 
	 * @param startsWith	the <b>lower case</b> string the name of the <i>Participant</i> to find should start with
	 * @return		all <i>Participants</i> with the given name
	 */
	Collection<ParticipantEntity> findByLcNameStartsWith(String startsWith);
		
	/**
	 * Find <i>Participants</i> in the given SML registration state. Because the result set can be quit large, it uses 
	 * pagination to prevent server overloading. 
	 * 
	 * @param registered	<code>true</code> when Participant registered in the SML should be retrieved,<br/>
	 * 						<code>false</code> when unregistered Participants should be retrieved
	 * @param requestSpec	a {@link PageRequest} specifying the requested subset 
	 * @return the requested sub set of Participants in the given SML registration state
	 */
	Page<ParticipantEntity> findByRegisteredInSML(boolean registered, Pageable requestSpec);
	
	/**
	 * Find <i>Participants</i> in the given directory publication state. Because the result set can be quit large, it 
	 * uses pagination to prevent server overloading. 
	 * 
	 * @param registered	<code>true</code> when Participant published to the directory should be retrieved,<br/>
	 * 						<code>false</code> when not published Participants should be retrieved
	 * @param requestSpec	a {@link PageRequest} specifying the requested subset 
	 * @return the requested sub set of Participants in the given directory publication state
	 */
	Page<ParticipantEntity> findByPublishedInDirectory(boolean published, Pageable requestSpec);

	
	Page<ParticipantEntity> findByRegisteredInSMLAndPublishedInDirectory(boolean registered, boolean published, Pageable requestSpec);
	
	/**
	 * Find <i>Participants</i> supporting the given <i>Service Metadata Template</i>. Because the result set can be 
	 * quit large, it uses pagination to prevent server overloading. 
	 * 
	 * @param smt	the <i>Service Metadata Template</i> to select the <i>Participants</i>
	 * @param requestSpec	a {@link PageRequest} specifying the requested subset 
	 * @return the requested sub set of Participants supporting the given <i>Service Metadata Template</i>
	 */
	Page<ParticipantEntity> findByBindingsContains(ServiceMetadataTemplateEntity smt, Pageable requestSpec);
	
	/**
	 * Checks if there exists at least one  <i>Participant</i> that is/is not published to the directory. This query is
	 * only used to check if any Participant is still published when a request is done to remove all Participants from
	 * the SML and SML registration is a pre-requisite for publication in the directory.
	 * 
	 * @param published		will always be set to <code>true</code> 
	 * @return	<code>true</code> when a Participant registration exists which is published in the directory,
	 * 			<code>false</code> otherwise
	 */
	boolean existsByPublishedInDirectory(boolean published);
	
	/**
	 * Count the number of Participants supporting the given <i>Service Metadata Template</i>.
	 * 
	 * @param templateId	the identifier of the Service Metadata Template registration to count
	 * @return			the number of Participants supporting the given <i>Service Metadata Template</i>
	 */
	@NativeQuery("select count(smb.PARTICIPANT_OID) from SERVICE_METADATA_BINDING smb where smb.TEMPLATE_OID = :templateOid")
	int countParticipantsSupporting(Long templateOid);		
	
	/**
	 * Set the SML registration indication to <code>false</code> for all Participants.
	 */
	@Modifying(clearAutomatically = true)
	@Query("update Participant p set p.registeredInSML = false")
	void unregisterAllFromSML();
}
