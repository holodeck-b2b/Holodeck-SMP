/*
 * Copyright (C) 2023 The Holodeck B2B Team
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

import org.holodeckb2b.bdxr.smp.server.datamodel.Identifier;
import org.holodeckb2b.bdxr.smp.server.db.entities.ParticipantEntity;

/**
 * Implements the Identifier based search and duplicate check capabilities for the {@link ParticipantEntity} based 
 * repository.
 * 
 * @author Sander Fieten (sander at holodeck-b2b.org)
 */
public class ParticipantIdCapableRepoImpl extends IdentifierSearchRepoImpl<Identifier, ParticipantEntity>
											implements ParticipantIdCapableRepo {
	@Override
	protected Class<ParticipantEntity> getResultClass() {
		return ParticipantEntity.class;
	}
	
	public Collection<ParticipantEntity> findByAdditionalId(org.holodeckb2b.bdxr.smp.datamodel.Identifier id) {
		return em.createNamedQuery("Participant.findByAdditionalId", ParticipantEntity.class)
									.setParameter("additionalId", id.toString())
									.getResultList();
//		long total = em.createNamedQuery("Participant.countByAdditionalId", Long.class)
//										.setParameter("additionalId", id.toString()).getSingleResult();
//		return new PageImpl<ParticipantEntity>(em.createNamedQuery("Participant.findByAdditionalId", ParticipantEntity.class)
//									.setParameter("additionalId", id.toString())
//									.setFirstResult(requestSpec.getPageNumber() * requestSpec.getPageSize())
//									.setMaxResults(requestSpec.getPageSize())
//									.getResultList(), requestSpec, total);
	}
}
