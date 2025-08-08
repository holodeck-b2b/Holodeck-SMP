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
package org.holodeckb2b.bdxr.smp.server.utils;

import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.NoSuchElementException;

import org.holodeckb2b.bdxr.smp.datamodel.IDScheme;
import org.holodeckb2b.bdxr.smp.datamodel.Identifier;
import org.holodeckb2b.bdxr.smp.datamodel.ProcessIdentifier;
import org.holodeckb2b.bdxr.smp.datamodel.impl.IdentifierImpl;
import org.holodeckb2b.bdxr.smp.datamodel.impl.ProcessIdentifierImpl;
import org.holodeckb2b.bdxr.smp.server.db.entities.EmbeddedIdentifier;
import org.holodeckb2b.bdxr.smp.server.db.entities.EmbeddedProcessIdentifier;
import org.holodeckb2b.bdxr.smp.server.db.entities.IDSchemeEntity;
import org.holodeckb2b.bdxr.smp.server.db.repos.IDSchemeRepository;
import org.holodeckb2b.commons.Pair;
import org.holodeckb2b.commons.util.Utils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Contains some helper functions for processing identifiers.
 *
 * @author Sander Fieten (sander at holodeck-b2b.org)
 */
@Component
public class IdUtils {

	@Autowired
	protected IDSchemeRepository 	idschemes;
	
	/**
	 * Checks that the given IDScheme is already managed by the server.
	 *   
	 * @param s	the IDScheme to check
	 * @throws IllegalArgumentException when the IDScheme is not managed
	 */
	public static void checkManagedScheme(IDScheme s) {
		if (s != null && (!(s instanceof IDSchemeEntity) || ((IDSchemeEntity) s).getOid() == null))
			throw new IllegalArgumentException("Unmanaged IDScheme object");		
	}
	
	/**
	 * Returns the string representation of the given Identifier always including the "::" separator between ID scheme
	 * and identifier value. If the Identifier is not assigned a ID Scheme the returned String starts with "::". This 
	 * ensures that the returned string can be successfully parsed if the identifier value includes a "::".
	 * 
	 * @param id	the Identifier to convert 
	 * @return	string representation of the Identifier, always including the "::" separator between ID Scheme and value
	 */
	public static String toIDString(Identifier id) {
		IDScheme ids = id.getScheme();
		return (ids != null ? ids.getSchemeId() : "") + "::" + id.getValue();		
	}
	
	/**
	 * Parses the string representation of an Identifier and returns it as {@link Identifier} instance. It handles the 
	 * URL encoding of the identifier.
	 *
	 * @param idString	the string representation of the Identifier, i.e.: [«schemeID»::]«identifier»
	 * @return	the object representation of the Identifier
	 * @throws NoSuchElementException  when there is no ID Scheme registered in this SMP with the given scheme ID
	 */
	public Identifier parseIDString(String idString) throws NoSuchElementException {
		Pair<String, IDSchemeEntity> parsedId = _parseIDString(idString);
		return new IdentifierImpl(parsedId.value1(), parsedId.value2());
	}
	
	/**
	 * Parses the string representation of a Process Identifier and returns it as {@link ProcessIdentifier} instance.
	 * It handles the URL encoding of the identifier.
	 * <p>
	 * Because different SMP specifications have different representations of the <i>No-Process</i> Identifier, this 
	 * method has a second argument to specify the <i>No-Process</i> string representation. If the identifier string is
	 * equal to this argument, the <i>No-Process</i> Identifier is returned.  
	 *
	 * @param idString	the string representation of the Process Identifier, i.e.: [«schemeID»::]«identifier»
	 * @param noProcString	the string representation of the <i>No-Process</i> Identifier
	 * @return	the object representation of the Process Identifier
	 * @throws NoSuchElementException  when there is no ID Scheme registered in this SMP with the given scheme ID
	 */
	public ProcessIdentifier parseProcessIDString(String idString, String noProcString) throws NoSuchElementException {
		if (Utils.nullSafeEqual(noProcString, idString))
			return new ProcessIdentifierImpl();
		
		Pair<String, IDSchemeEntity> parsedId = _parseIDString(idString);
		return new ProcessIdentifierImpl(parsedId.value1(), parsedId.value2());
	}
		
	/**
	 * Converts the given {@link Identifier} instance to an {@link EmbeddedIdentifier} instance. This includes a check
	 * on the IDScheme to make sure it is managed by this SMP.
	 * 
	 * @param id	the identifier to convert
	 * @return	an {@link EmbeddedIdentifier} instance representing the identifier
	 * @throws NoSuchElementException	if the IDScheme of the given identifier is not managed by this SMP
	 */
	public EmbeddedIdentifier toEmbeddedIdentifier(Identifier id) throws NoSuchElementException {
		return new EmbeddedIdentifier(getIDScheme(id.getScheme()), id.getValue());
	}

	/**
	 * Converts the given {@link ProcessIdentifier} instance to an {@link EmbeddedProcessIdentifier} instance. This 
	 * includes a check on the IDScheme to make sure it is managed by this SMP.
	 * 
	 * @param id	the identifier to convert
	 * @return	an {@link EmbeddedProcessIdentifier} instance representing the identifier
	 * @throws NoSuchElementException	if the IDScheme of the given identifier is not managed by this SMP
	 */
	public EmbeddedProcessIdentifier toEmbeddedProcessIdentifier(ProcessIdentifier id) throws NoSuchElementException {
		if (id.isNoProcess())
			return new EmbeddedProcessIdentifier(EmbeddedProcessIdentifier.NO_PROCESS);
		else 
			return new EmbeddedProcessIdentifier(getIDScheme(id.getScheme()), id.getValue());
	}

	/**
	 * Helper method to do the actual parsing of an identifier string. Instead of returning an Identifier object, it 
	 * returns a Pair containing the identifier value and, if applicable, the IDScheme entity. This can be used by the
	 * public methods to return to correct type of Identifier object.
	 * 
	 * @param idString	the string representation of the identifier
	 * @return	a {@link Pair} containing the identifier value and, if applicable, the IDScheme entity
	 * @throws NoSuchElementException
	 */
	private Pair<String, IDSchemeEntity> _parseIDString(String idString) throws NoSuchElementException {
		final String decoded = URLDecoder.decode(idString, StandardCharsets.UTF_8);
		int sep = decoded.indexOf("::");
		String ids = decoded.substring(0, Math.max(0, sep));
		String idv = sep < 0 ? decoded : decoded.substring(sep + 2);
		if (!Utils.isNullOrEmpty(ids)) {
			// Find the ID scheme with the specified id
			IDSchemeEntity scheme = idschemes.findByIdentifier(ids);
			if (scheme == null)
				throw new NoSuchElementException("No IDScheme registered with the scheme ID=" + ids);
			return new Pair<String, IDSchemeEntity>(idv, scheme);
		} else
			return new Pair<String, IDSchemeEntity>(idv, null);
	}	
	
	/**
	 * Helper method to get the {@link IDSchemeEntity} representation for the given {@link IDScheme}.
	 * 
	 * @param ids	the IDScheme instance
	 * @return	the {@link IDSchemeEntity} instance representating the same ID {@link IDSchemeEntity}
	 * @throws NoSuchElementException when the IDScheme is not managed by this SMP
	 */
	private IDSchemeEntity getIDScheme(IDScheme ids) throws NoSuchElementException {
		if (ids == null)
			return null;
		else if (ids instanceof IDSchemeEntity && ((IDSchemeEntity) ids).getOid() != null)
			return (IDSchemeEntity) ids;
		else if (!Utils.isNullOrEmpty(ids.getSchemeId())) {
			IDSchemeEntity registeredScheme = idschemes.findByIdentifier(ids.getSchemeId());
			if (registeredScheme == null)
				throw new NoSuchElementException("Missing ID Scheme registration for schemeID=" + ids.getSchemeId());
			
			return registeredScheme;
		} else
			throw new IllegalArgumentException("IDScheme must have a scheme identifier");
	}
}
