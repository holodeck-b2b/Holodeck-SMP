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

import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import lombok.extern.slf4j.Slf4j;
import org.holodeckb2b.bdxr.smp.datamodel.Certificate;
import org.holodeckb2b.bdxr.smp.datamodel.EndpointInfo;
import org.holodeckb2b.bdxr.smp.datamodel.Identifier;
import org.holodeckb2b.bdxr.smp.datamodel.ProcessGroup;
import org.holodeckb2b.bdxr.smp.datamodel.ProcessInfo;
import org.holodeckb2b.bdxr.smp.datamodel.Redirection;
import org.holodeckb2b.bdxr.smp.datamodel.impl.CertificateImpl;
import org.holodeckb2b.bdxr.smp.server.datamodel.ServiceMetadataBinding;
import org.holodeckb2b.bdxr.smp.server.datamodel.ServiceMetadataTemplate;
import org.oasis_open.docs.bdxr.ns.smp._2.aggregatecomponents.CertificateType;
import org.oasis_open.docs.bdxr.ns.smp._2.aggregatecomponents.EndpointType;
import org.oasis_open.docs.bdxr.ns.smp._2.aggregatecomponents.ProcessMetadataType;
import org.oasis_open.docs.bdxr.ns.smp._2.aggregatecomponents.ProcessType;
import org.oasis_open.docs.bdxr.ns.smp._2.aggregatecomponents.RedirectType;
import org.oasis_open.docs.bdxr.ns.smp._2.basiccomponents.ActivationDateType;
import org.oasis_open.docs.bdxr.ns.smp._2.basiccomponents.AddressURIType;
import org.oasis_open.docs.bdxr.ns.smp._2.basiccomponents.ContactType;
import org.oasis_open.docs.bdxr.ns.smp._2.basiccomponents.ContentBinaryObjectType;
import org.oasis_open.docs.bdxr.ns.smp._2.basiccomponents.DescriptionType;
import org.oasis_open.docs.bdxr.ns.smp._2.basiccomponents.ExpirationDateType;
import org.oasis_open.docs.bdxr.ns.smp._2.basiccomponents.IDType;
import org.oasis_open.docs.bdxr.ns.smp._2.basiccomponents.ParticipantIDType;
import org.oasis_open.docs.bdxr.ns.smp._2.basiccomponents.PublisherURIType;
import org.oasis_open.docs.bdxr.ns.smp._2.basiccomponents.RoleIDType;
import org.oasis_open.docs.bdxr.ns.smp._2.basiccomponents.TransportProfileIDType;
import org.oasis_open.docs.bdxr.ns.smp._2.basiccomponents.TypeCodeType;
import org.oasis_open.docs.bdxr.ns.smp._2.servicemetadata.ServiceMetadataType;
import org.w3c.dom.Document;

/**
 * Is a factory for <code>ServiceMetadata</code> XML documents as specified by the OASIS SMP V2 Specification.
 *
 * @author Sander Fieten (sander at holodeck-b2b.org)
 */
@Slf4j
public class ServiceMetadataFactory extends AbstractResponseFactory {
	/**
	 * The Mime Type that should be set on the <code>mimeCode</code> attribute of the <code>ContentBinaryObject</code>
	 * element.
	 */
	private String certMimeType;

	ServiceMetadataFactory(String certMimeType) {
		this.certMimeType = certMimeType;
	}

	/**
	 * Creates a XML Document with <code>ServiceMetadata</code> root element as defined by the OASIS SMP2 Specification
	 * using the metadata from the given ServiceMetadata Binding.
	 *
	 * @param smb	the Servicemetadata Binding to use
	 * @return	new XML Document containing the <code>ServiceMetadata</code>
	 */
	Document newResponse(ServiceMetadataBinding smb) throws InstantiationException {
		ServiceMetadataTemplate smt = smb.getTemplate();
		ServiceMetadataType smd = new ServiceMetadataType();
		smd.setSMPVersionID(SMP_VERSION_ID);
		smd.setID(convertID(smt.getServiceId(), IDType.class));
		smd.setParticipantID(convertID(smb.getParticipantId(), ParticipantIDType.class));
		for(ProcessGroup pg : smt.getProcessMetadata())
			smd.getProcessMetadata().add(createProcessMetadata(pg));

		return jaxb2dom(smd);
	}

	private ProcessMetadataType createProcessMetadata(ProcessGroup pg) throws InstantiationException {
		ProcessMetadataType pmd = new ProcessMetadataType();
		for(ProcessInfo p : pg.getProcessInfo())
			pmd.getProcess().add(createProcess(p));
		for(EndpointInfo ep : pg.getEndpoints())
			pmd.getEndpoint().add(createEndpoint(ep));
		pmd.setRedirect(createRedirect(pg.getRedirection()));
		return pmd;
	}

	private ProcessType createProcess(ProcessInfo pi) throws InstantiationException {
		ProcessType p = new ProcessType();
		IDType pid;
		if (pi.getProcessId().isNoProcess()) {
			pid = new IDType();
			pid.setValue("bdx:noprocess");
		} else
			pid = convertID(pi.getProcessId(), IDType.class);
		p.setID(pid);
		for(Identifier r : pi.getRoles())
			p.getRoleID().add(convertID(r, RoleIDType.class));
		return p;
	}

	private EndpointType createEndpoint(EndpointInfo ep) throws InstantiationException {
		EndpointType e = new EndpointType();
		TransportProfileIDType tp = new TransportProfileIDType();
		tp.setValue(ep.getTransportProfile());
		e.setTransportProfileID(tp);
		e.setDescription(createTextContent(ep.getDescription(), DescriptionType.class));
		e.setContact(createTextContent(ep.getContactInfo(), ContactType.class));
		e.setAddressURI(createTextContent(ep.getEndpointURL().toString(), AddressURIType.class));
		e.setActivationDate(createDateContent(ep.getServiceActivationDate(), ActivationDateType.class));
		e.setExpirationDate(createDateContent(ep.getServiceExpirationDate(), ExpirationDateType.class));
		for(Certificate c : ep.getCertificates())
			e.getCertificate().add(createCertificate(c));
		return e;
	}

	private CertificateType createCertificate(Certificate cert) throws InstantiationException {
		CertificateType c = new CertificateType();
		c.setTypeCode(createTextContent(cert.getUsage(), TypeCodeType.class));
		c.setDescription(createTextContent(cert.getDescription(), DescriptionType.class));
		c.setActivationDate(createDateContent(cert.getActivationDate(), ActivationDateType.class));
		c.setExpirationDate(createDateContent(cert.getExpirationDate(), ExpirationDateType.class));
		try {
			ContentBinaryObjectType ec = new ContentBinaryObjectType();
			ec.setMimeCode(certMimeType);
			ec.setValue(cert.getX509Cert().getEncoded());
			c.setContentBinaryObject(ec);
		} catch (CertificateEncodingException ex) {
			throw new InstantiationException("Could not encode Certificate");
		}
		return c;
	}

	private RedirectType createRedirect(Redirection redirection) throws InstantiationException {
		if (redirection == null)
			return null;
		RedirectType r = new RedirectType();
		r.setPublisherURI(createTextContent(redirection.getNewSMPURL().toString(), PublisherURIType.class));
		X509Certificate c = redirection.getSMPCertificate();
		if (c != null)
			r.getCertificate().add(createCertificate(new CertificateImpl(c)));
		return r;
	}
}
