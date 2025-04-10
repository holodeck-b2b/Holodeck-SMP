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

import java.security.cert.CertificateEncodingException;
import java.util.Base64;
import java.util.Collection;
import java.util.Iterator;

import org.busdox.servicemetadata.publishing._1.EndpointType;
import org.busdox.servicemetadata.publishing._1.ProcessListType;
import org.busdox.servicemetadata.publishing._1.ProcessType;
import org.busdox.servicemetadata.publishing._1.RedirectType;
import org.busdox.servicemetadata.publishing._1.ServiceEndpointList;
import org.busdox.servicemetadata.publishing._1.ServiceInformationType;
import org.busdox.servicemetadata.publishing._1.ServiceMetadataType;
import org.busdox.servicemetadata.publishing._1.SignedServiceMetadataType;
import org.busdox.transport.identifiers._1.DocumentIdentifierType;
import org.busdox.transport.identifiers._1.ParticipantIdentifierType;
import org.busdox.transport.identifiers._1.ProcessIdentifierType;
import org.holodeckb2b.bdxr.smp.datamodel.Certificate;
import org.holodeckb2b.bdxr.smp.datamodel.EndpointInfo;
import org.holodeckb2b.bdxr.smp.datamodel.Identifier;
import org.holodeckb2b.bdxr.smp.datamodel.ProcessGroup;
import org.holodeckb2b.bdxr.smp.datamodel.ProcessIdentifier;
import org.holodeckb2b.bdxr.smp.datamodel.ProcessInfo;
import org.holodeckb2b.bdxr.smp.datamodel.RedirectionV2;
import org.holodeckb2b.bdxr.smp.server.datamodel.ServiceMetadataBinding;
import org.holodeckb2b.bdxr.smp.server.datamodel.ServiceMetadataTemplate;
import org.w3._2005._08.addressing.AttributedURIType;
import org.w3._2005._08.addressing.EndpointReferenceType;
import org.w3c.dom.Document;

import lombok.extern.slf4j.Slf4j;

/**
 * Is a factory for <code>SignedServiceMetadata</code> XML documents as specified by the PEPPOL SMP Specification.
 *
 * @author Sander Fieten (sander at holodeck-b2b.org)
 */
@Slf4j
public class ServiceMetadataFactory extends AbstractResponseFactory {

	/**
	 * Creates a XML Document with <code>SignedServiceMetadata</code> root element as defined by the PEPPOL SMP
	 * Specification using the metadata from the given ServiceMetadata Binding.
	 *
	 * @param smb	the Servicemetadata Binding to use
	 * @return	new XML Document containing the <code>ServiceMetadata</code>
	 */
	Document newResponse(ServiceMetadataBinding smb) throws InstantiationException {
		ServiceMetadataType smd = new ServiceMetadataType();

		ServiceMetadataTemplate smt = smb.getTemplate();
		Collection<? extends ProcessGroup> pmd = smt.getProcessMetadata();
		long redirections = pmd.stream().filter(pg -> pg.getRedirection() != null).count();
		if (redirections != 0 && pmd.size() > 1) {
			log.error("Incompatible Service Metadata Template [{}]: more than one Redirection(s) or combination of Redirection and Endpoints",
						smt.getName());
			throw new InstantiationException("Incompatible Service Metadata Template");
		} else if (redirections == 1)
			smd.setRedirect(createRedirection(smb));
		else
			smd.setServiceInformation(createServiceInformation(smb));

		SignedServiceMetadataType ssmd = new SignedServiceMetadataType();
		ssmd.setServiceMetadata(smd);

		return jaxb2dom(ssmd);
	}

	private RedirectType createRedirection(ServiceMetadataBinding smb) {
		RedirectType redirection = new RedirectType();

		RedirectionV2 rmd = (RedirectionV2) smb.getTemplate().getProcessMetadata().stream()
										  .filter(pg -> pg.getRedirection() != null).findFirst().get().getRedirection();

		String baseURL = rmd.getNewSMPURL().toString();
		redirection.setHref(String.format("%s%s%s/services/%s", baseURL, baseURL.endsWith("/") ? "" : "/",
											smb.getParticipantId().getURLEncoded(),
											smb.getTemplate().getServiceId().getURLEncoded()));

		// NOTE: The Subject UID is set to the empty string as the use of this certificate field is not included in
		// certificates issued in the Peppol network but the element is required by the XSD
		redirection.setCertificateUID("");

		return redirection;
	}

	private ServiceInformationType createServiceInformation(ServiceMetadataBinding smb) throws InstantiationException {
		ServiceInformationType si = new ServiceInformationType();

		Identifier participantId = smb.getParticipantId();
		ParticipantIdentifierType pid = new ParticipantIdentifierType();
		if (participantId.getScheme() != null)
			pid.setScheme(participantId.getScheme().getSchemeId());
		pid.setValue(participantId.getValue());
		si.setParticipantIdentifier(pid);

		ServiceMetadataTemplate smt = smb.getTemplate();

		DocumentIdentifierType did = new DocumentIdentifierType();
		Identifier serviceId = smt.getServiceId();
		if (serviceId.getScheme() != null)
			did.setScheme(serviceId.getScheme().getSchemeId());
		did.setValue(serviceId.getValue());
		si.setDocumentIdentifier(did);

		ProcessListType procList = new ProcessListType();
		for(ProcessGroup pg : smt.getProcessMetadata()) {
			ServiceEndpointList epList = new ServiceEndpointList();
			for(EndpointInfo ep : pg.getEndpoints())
				epList.getEndpoint().add(createEndpoint(ep));

			for (ProcessInfo p : pg.getProcessInfo()) {
				ProcessType pi = new ProcessType();
				ProcessIdentifierType procid = new ProcessIdentifierType();
				ProcessIdentifier processId = p.getProcessId();
				if (processId.getScheme() != null)
					procid.setScheme(processId.getScheme().getSchemeId());
				procid.setValue(processId.getValue());
				pi.setProcessIdentifier(procid);
				pi.setServiceEndpointList(epList);
				procList.getProcess().add(pi);
			}
		}
		si.setProcessList(procList);

		return si;
	}

	private EndpointType createEndpoint(EndpointInfo ep) throws InstantiationException {
		EndpointType e = new EndpointType();
		e.setTransportProfile(ep.getTransportProfile());

		EndpointReferenceType epr = new EndpointReferenceType();
		AttributedURIType epURL = new AttributedURIType();
		epURL.setValue(ep.getEndpointURL().toString());
		epr.setAddress(epURL);
		e.setEndpointReference(epr);

		e.setServiceActivationDate(createDateTimeContent(ep.getServiceActivationDate()));
		e.setServiceExpirationDate(createDateTimeContent(ep.getServiceExpirationDate()));
		e.setRequireBusinessLevelSignature(false);
		e.setServiceDescription(ep.getDescription());
		e.setTechnicalContactUrl(ep.getContactInfo());
		Iterator<? extends Certificate> certs = ep.getCertificates().iterator();
		if (!certs.hasNext()) {
			log.error("Missing required certificate for endpoint : {}", ep.getEndpointURL().toString());
			throw new InstantiationException("Missing required certificate");
		}
		try {
			e.setCertificate(Base64.getMimeEncoder(64, "\r\n".getBytes())
													.encodeToString(certs.next().getX509Cert().getEncoded()));
		} catch (CertificateEncodingException encodingError) {
			log.error("Error encoding the endpoint certificate : {}", encodingError.getMessage());
			throw new InstantiationException("Could not encode certificate");
		}
		if (certs.hasNext())
			log.warn("Additional certificates are configured for endpoint ({}), but cannot be included in PEPPOL response",
					ep.getEndpointURL().toString());

		return e;
	}
}
