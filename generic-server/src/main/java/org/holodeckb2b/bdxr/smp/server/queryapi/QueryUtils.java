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
package org.holodeckb2b.bdxr.smp.server.queryapi;

import java.util.NoSuchElementException;
import org.holodeckb2b.bdxr.smp.datamodel.Identifier;
import org.holodeckb2b.bdxr.smp.server.db.entities.IdentifierE;
import org.holodeckb2b.bdxr.smp.server.db.repos.IDSchemeRepository;
import org.holodeckb2b.commons.util.Utils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Contains some helper functions for processing SMP queries.
 *
 * @author Sander Fieten (sander at holodeck-b2b.org)
 */
@Component
public class QueryUtils {

	@Autowired
	protected IDSchemeRepository idschemes;

	/**
	 * Parses the string representation of an Identifier and returns it as {@link Identifier} instance.
	 *
	 * @param idString	the string representation of the Identifier, i.e.: [«schemeID»::]«identifier»
	 * @return	the object representation of the Identifier
	 * @throws NoSuchElementException  when there is no ID Scheme registered in this SMP with the given scheme ID
	 */
	public Identifier parseIDString(String idString) throws NoSuchElementException {
		int sep = idString.indexOf("::");
		String ids = idString.substring(0, Math.max(0, sep));
		String idv = sep < 0 ? idString : idString.substring(sep + 2);
		if (!Utils.isNullOrEmpty(ids)) {
			// Find the ID scheme with the specified id
			return new IdentifierE(idv, idschemes.findById(ids).orElseThrow());
		} else
			return new IdentifierE(idv);
	}
}
