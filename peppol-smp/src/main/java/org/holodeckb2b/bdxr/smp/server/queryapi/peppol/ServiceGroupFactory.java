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
package org.holodeckb2b.bdxr.smp.server.queryapi.peppol;

import java.net.URL;
import java.util.Collection;
import java.util.List;

import org.busdox.servicemetadata.publishing._1.ServiceGroupType;
import org.busdox.servicemetadata.publishing._1.ServiceMetadataReferenceCollectionType;
import org.busdox.servicemetadata.publishing._1.ServiceMetadataReferenceType;
import org.busdox.transport.identifiers._1.ParticipantIdentifierType;
import org.holodeckb2b.bdxr.common.datamodel.Identifier;
import org.holodeckb2b.bdxr.smp.server.datamodel.ServiceMetadataTemplate;
import org.w3c.dom.Document;

/**
 * Is a factory for <code>ServiceGroup</code> XML documents as specified by the PEPPOL SMP Specification.
 *
 * @author Sander Fieten (sander at holodeck-b2b.org)
 */
public class ServiceGroupFactory extends AbstractResponseFactory {


	/**
	 * Creates a XML Document with <code>ServiceGroup</code> root element as defined by the PEPPOL SMP Specification
	 * using the metadata from the given ServiceMetadata Bindings.
	 *
	 * @param partId	the Participant identifier
	 * @param smt		the collection of Servicemetadata Bindings to use
	 * @param smpURL	the URL at which the SMP is registered
	 * @return	new XML Document containing the <code>ServiceGroup</code>
	 */
	Document newResponse(Identifier partId, Collection<? extends ServiceMetadataTemplate> smt, URL smpURL)
																						throws InstantiationException {
		ServiceGroupType sg = new ServiceGroupType();
		ParticipantIdentifierType partID = new ParticipantIdentifierType();
		partID.setValue(partId.getValue());
		partID.setScheme(partId.getScheme() != null ? partId.getScheme().getSchemeId() : null);
		sg.setParticipantIdentifier(partID);

		ServiceMetadataReferenceCollectionType svcRefs = new ServiceMetadataReferenceCollectionType();
		sg.setServiceMetadataReferenceCollection(svcRefs);

		List<ServiceMetadataReferenceType> refs = svcRefs.getServiceMetadataReference();
		for(ServiceMetadataTemplate t : smt) {
			ServiceMetadataReferenceType r = new ServiceMetadataReferenceType();
			r.setHref(String.format("%s/%s/services/%s", smpURL.toString(),
									partId.getURLEncoded(), t.getService().getId().getURLEncoded()));
			refs.add(r);
		}

		return jaxb2dom(sg);
	}
}
