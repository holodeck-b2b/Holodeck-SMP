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
package org.holodeckb2b.bdxr.smp.server.mgmtapi.xml;

import java.math.BigInteger;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.util.Collection;

import org.holodeckb2b.bdxr.common.datamodel.Identifier;
import org.holodeckb2b.bdxr.smp.datamodel.Certificate;
import org.holodeckb2b.bdxr.smp.datamodel.EndpointInfo;
import org.holodeckb2b.bdxr.smp.datamodel.ProcessGroup;
import org.holodeckb2b.bdxr.smp.datamodel.ProcessInfo;
import org.holodeckb2b.bdxr.smp.datamodel.RedirectionV2;
import org.holodeckb2b.bdxr.smp.datamodel.impl.CertificateImpl;
import org.holodeckb2b.bdxr.smp.server.datamodel.Participant;
import org.holodeckb2b.bdxr.smp.server.datamodel.ServiceMetadataTemplate;
import org.holodeckb2b.bdxr.smp.server.mgmtapi.xml.v2025.CertificateElement;
import org.holodeckb2b.bdxr.smp.server.mgmtapi.xml.v2025.EndpointElement;
import org.holodeckb2b.bdxr.smp.server.mgmtapi.xml.v2025.ProcessMetadataElement;
import org.holodeckb2b.bdxr.smp.server.mgmtapi.xml.v2025.ServiceMetadataBindingsElement;
import org.holodeckb2b.bdxr.smp.server.mgmtapi.xml.v2025.ServiceMetadataTemplateElement;
import org.holodeckb2b.bdxr.smp.server.mgmtapi.xml.v2025.ServiceMetadataTemplatesElement;
import org.holodeckb2b.bdxr.smp.server.queryapi.oasisv2.AbstractResponseFactory;
import org.holodeckb2b.commons.util.Utils;
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
import org.oasis_open.docs.bdxr.ns.smp._2.extensioncomponents.NameType;

import lombok.extern.slf4j.Slf4j;

/**
 * Is a factory for the XML response documents used in the SMP server's REST API and as specified in the XML schema with
 * namespace <i>http://holodeck-smp.org/schemas/2025/05/server/api/metadata</i> and which can be found in 
 * <a href="../../../../../../../../xsd/metadata.xsd">/src/main/xsd/metadata.xsd</a>.
 * 
 * @author Sander Fieten (sander at holodeck-b2b.org)
 * @since  3.0.0
 */
@Slf4j
public class ResponseFactory extends AbstractResponseFactory {
	/**
	 * The Mime Type that will be set on the <code>mimeCode</code> attribute of the <code>ContentBinaryObject</code>
	 * element that includes the PEM encoded version of an Endpoint certificate.
	 */
	private static final String CERT_MIME_TYPE = "application/pkix-cert";

	private static class SMTFactoryHolder {
		static ResponseFactory factory = new ResponseFactory();
	}
	
	private ResponseFactory() {}
		
	/**
	 * Creates a new {@link ServiceMetadataTemplatesElement} instance which is the XML representation of given set of
	 * {@link ServiceMetadataTemplateEntity Service Metadata Templates}.   
	 * 
	 * @param collection		collection of Service Metadata Templates  
	 * @return	JAXB object containing the XML representation of the Service Metadata Templates 
	 * @throws InstantiationException	when the XML could not be created
	 */
	public static ServiceMetadataTemplatesElement createSMTResponse(
							Collection<? extends ServiceMetadataTemplate> collection) throws InstantiationException {
		return SMTFactoryHolder.factory.createSMTElement(collection);
	}
		
	/**
	 * Creates a new {@link ServiceMetadataBindingsElement} instance which represents the XML document that contains the
	 * overview of which Service Metadata Templates are supported for the Participant with the given identifier.   
	 * 
	 * @param p	the Participant meta-data registration
	 * @return	JAXB object containing the XML representation of the Service Metadata Templates bindings  
	 * @throws InstantiationException	when the XML could not be created
	 */
	public static ServiceMetadataBindingsElement createBindingsResponse(Participant p) throws InstantiationException {		
		return SMTFactoryHolder.factory.createSMBElement(p);
	}
	
	private ServiceMetadataBindingsElement createSMBElement(Participant p) throws InstantiationException {
		ServiceMetadataBindingsElement smb = new ServiceMetadataBindingsElement();		
		smb.setParticipantID(convertID(p.getId(), ParticipantIDType.class));	
		
		if (!Utils.isNullOrEmpty(p.getBoundSMT())) {
			for(ServiceMetadataTemplate t : p.getBoundSMT()) 
				smb.getTemplateIds().add(BigInteger.valueOf(t.getId()));			
		}		
		return smb;
	}
	
	private ServiceMetadataTemplatesElement createSMTElement(Collection<? extends ServiceMetadataTemplate> collection) 
																						throws InstantiationException {
		ServiceMetadataTemplatesElement smt = new ServiceMetadataTemplatesElement();
		if (!Utils.isNullOrEmpty(collection))
			for (ServiceMetadataTemplate t : collection)
				smt.getServiceMetadataTemplates().add(createServiceMetadataTemplate(t));
		
		return smt;
	}
		
	private ServiceMetadataTemplateElement createServiceMetadataTemplate(ServiceMetadataTemplate t) 
																						throws InstantiationException {		
		ServiceMetadataTemplateElement smt = new ServiceMetadataTemplateElement();
		smt.setID(convertID(t.getService().getId(), IDType.class));
		smt.setName(createTextContent(t.getName(), NameType.class));
		smt.setTemplateId(BigInteger.valueOf(t.getId()));
		
		for (ProcessGroup pg : t.getProcessMetadata())
			smt.getProcessMetadatas().add(createProcessMetadata(pg));
		
		return smt;
	}
	
	private ProcessMetadataElement createProcessMetadata(ProcessGroup pg) throws InstantiationException {
		ProcessMetadataElement pmd = new ProcessMetadataElement();
		for(ProcessInfo p : pg.getProcessInfo())
			pmd.getProcesses().add(createProcess(p));
		for(EndpointInfo ep : pg.getEndpoints())
			pmd.getEndpoints().add(createEndpoint(ep));
		pmd.setRedirect(createRedirect((RedirectionV2) pg.getRedirection()));
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
	
	private EndpointElement createEndpoint(EndpointInfo ep) throws InstantiationException {
		EndpointElement e = new EndpointElement();
//		e.setEndpointId(BigInteger.valueOf(ep.getOid()));
		e.setTransportProfileID(convertID(ep.getTransportProfileId(), TransportProfileIDType.class));
		e.setDescription(createTextContent(ep.getDescription(), DescriptionType.class));
		e.setContact(createTextContent(ep.getContactInfo(), ContactType.class));
		e.setAddressURI(createTextContent(ep.getEndpointURL().toString(), AddressURIType.class));
		e.setActivationDate(createDateContent(ep.getServiceActivationDate(), ActivationDateType.class));
		e.setExpirationDate(createDateContent(ep.getServiceExpirationDate(), ExpirationDateType.class));
		for(Certificate c : ep.getCertificates())
			e.getCertificates().add(createCertificate(c));
		return e;
	}

	private CertificateElement createCertificate(Certificate cert) throws InstantiationException {
		CertificateElement c = new CertificateElement();
		c.setTypeCode(createTextContent(cert.getUsage(), TypeCodeType.class));
		c.setDescription(createTextContent(cert.getDescription(), DescriptionType.class));
		c.setActivationDate(createDateContent(cert.getActivationDate(), ActivationDateType.class));
		c.setExpirationDate(createDateContent(cert.getExpirationDate(), ExpirationDateType.class));
		try {
			ContentBinaryObjectType ec = new ContentBinaryObjectType();
			ec.setMimeCode(CERT_MIME_TYPE);
			ec.setValue(cert.getX509Cert().getEncoded());
			c.setContentBinaryObject(ec);
		} catch (CertificateEncodingException ex) {
			throw new InstantiationException("Could not encode Certificate");
		}
		return c;
	}

	private RedirectType createRedirect(RedirectionV2 redirection) throws InstantiationException {
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
