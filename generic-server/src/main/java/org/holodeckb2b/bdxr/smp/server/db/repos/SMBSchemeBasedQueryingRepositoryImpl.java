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

import java.util.ArrayList;
import java.util.List;

import org.holodeckb2b.bdxr.smp.datamodel.IDScheme;
import org.holodeckb2b.bdxr.smp.datamodel.Identifier;
import org.holodeckb2b.bdxr.smp.server.db.entities.ServiceMetadataBindingE;
import org.holodeckb2b.commons.Pair;
import org.springframework.beans.factory.annotation.Autowired;

import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

/**
 * Implements the queries for getting the <i>Service Metadata Template</i>(s) of a Participant. Because the queries need
 * to observe the case sensitivity specified by the identifier schemes a custom repository implementation is used.
 *
 * @author Sander Fieten (sander at holodeck-b2b.org)
 */
public class SMBSchemeBasedQueryingRepositoryImpl implements SMBSchemeBasedQueryingRepository {

	@Autowired
    private EntityManager	em;

	@Override
	public List<ServiceMetadataBindingE> findByParticipantId(Identifier partID) {
		return executeQuery(new Pair<String, Identifier>("participant.id", partID));
	}

	@Override
	public ServiceMetadataBindingE findByParticipantAndServiceId(Identifier partID, Identifier serviceID) {

		// The result of the query is either one SMB or none at all
		List<ServiceMetadataBindingE> smb = executeQuery(new Pair<String, Identifier>("participant.id", partID),
														 new Pair<String, Identifier>("template.service.id", serviceID));
		return smb.isEmpty() ? null : smb.get(0);
	}

	private List<ServiceMetadataBindingE> executeQuery(Pair<String, Identifier> ...ids) {
		CriteriaBuilder cb = em.getCriteriaBuilder();
		CriteriaQuery<ServiceMetadataBindingE> q = cb.createQuery(ServiceMetadataBindingE.class);
		Root<ServiceMetadataBindingE> smb = q.from(ServiceMetadataBindingE.class);
		q.select(smb);

		List<Predicate> p = new ArrayList<>();
		for(Pair<String, Identifier> id : ids) {
			Path idField = smb;
			for(String f : id.value1().split("\\."))
				idField	= idField.get(f);
			String value = id.value2().getValue();
			IDScheme scheme = id.value2().getScheme();
			if (scheme != null && scheme.isCaseSensitive())
				p.add(cb.equal(idField.get("value"), value));
			else
				p.add(cb.equal(cb.lower(idField.get("value")), value.toLowerCase()));
			if (scheme != null)
				p.add(cb.equal(idField.get("scheme"), scheme));
			else
				p.add(cb.isNull(idField.get("scheme")));
		}
		q.where(p.toArray(new Predicate[p.size()]));

		return em.createQuery(q).getResultList();
	}
}
