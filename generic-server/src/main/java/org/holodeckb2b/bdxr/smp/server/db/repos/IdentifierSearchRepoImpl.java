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
import org.holodeckb2b.bdxr.smp.server.datamodel.Identifier;
import org.holodeckb2b.bdxr.smp.server.db.entities.BaseMetadataRegistrationEntity;

import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

/**
 * Base implementation of {@link UniqueIdMDRRepo} to manage meta-data registration entities that use an {@link
 * Identifier} based identifier.
 *
 * @param <I> the actual implementation of {@link Identifier} used by <E>
 * @param <E> the entity class subject of the repository
 * @author Sander Fieten (sander at holodeck-b2b.org)
 */
abstract class IdentifierSearchRepoImpl<I extends Identifier, E extends BaseMetadataRegistrationEntity<I>> 
																	extends DuplicateIdCheckRepoImpl<I, E> {

	@Override
	public E findByIdentifier(I id) {
		final IDScheme scheme = id.getScheme();

		CriteriaBuilder cb = em.getCriteriaBuilder();
		CriteriaQuery<E> q = cb.createQuery(getResultClass());
		Root<E> r = q.from(getResultClass());
		q.select(r);
		List<Predicate> p = new ArrayList<>();
		if (scheme != null && scheme.isCaseSensitive())
			p.add(cb.equal(r.get("id").get("value"), id.getValue()));
		else
			p.add(cb.equal(cb.lower(r.get("id").get("value")), id.getValue().toLowerCase()));
		if (scheme != null)
			p.add(cb.equal(r.get("id").get("scheme"), scheme));
		else
			p.add(cb.isNull(r.get("id").get("scheme")));

		q.where(p.toArray(new Predicate[p.size()]));

		TypedQuery<E> tq = em.createQuery(q);
		return tq.getResultList().stream().findFirst().orElse(null);
	}

	/**
	 * Implementations should return the actual class of <R>, i.e. the entity to be searched.
	 * 
	 * @return	class of <R>
	 */
	abstract protected Class<E> getResultClass();
}
