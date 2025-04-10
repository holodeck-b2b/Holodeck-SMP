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

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.security.cert.CertificateException;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.regex.Pattern;

import javax.xml.crypto.dsig.XMLSignatureException;

import org.holodeckb2b.bdxr.smp.datamodel.Identifier;
import org.holodeckb2b.bdxr.smp.server.db.SMLRegistration;
import org.holodeckb2b.bdxr.smp.server.db.entities.ServiceMetadataBindingE;
import org.holodeckb2b.bdxr.smp.server.db.repos.ParticipantRepository;
import org.holodeckb2b.bdxr.smp.server.db.repos.ServiceMetadataBindingRepository;
import org.holodeckb2b.bdxr.smp.server.queryapi.IQueryResponder;
import org.holodeckb2b.bdxr.smp.server.queryapi.QueryResponse;
import org.holodeckb2b.bdxr.smp.server.queryapi.XMLResponseSigner;
import org.holodeckb2b.bdxr.smp.server.svc.IdUtils;
import org.holodeckb2b.bdxr.smp.server.svc.peppol.SMLClient;
import org.holodeckb2b.commons.util.Utils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;

import lombok.extern.slf4j.Slf4j;

/**
 * Is the component responsible for processing SMP queries as specified in the PEPPOL SMP ßSpecification.
 *
 * @author Sander Fieten (sander at holodeck-b2b.org)
 */
@Service
@Slf4j
public class PEPPOLQueryResponder implements IQueryResponder {

	@Value("${peppol.signing.algorithm:http://www.w3.org/2001/04/xmldsig-more#rsa-sha256}")
	protected String signingAlgorithm;
	@Value("${peppol.signing.c14n:http://www.w3.org/TR/2001/REC-xml-c14n-20010315}")
	protected String c14nAlgorithm;
	@Value("${peppol.signing.digest:http://www.w3.org/2001/04/xmlenc#sha256}")
	protected String digestMethod;	
	
	@Autowired
	protected IdUtils	queryUtils;
	@Autowired
	protected XMLResponseSigner	signer;
	@Autowired
	protected ParticipantRepository participants;
	@Autowired
	protected ServiceMetadataBindingRepository bindings;
	@Autowired
	protected SMLClient		smlSvc;

	private ServiceMetadataFactory smdFactory;
	private ServiceGroupFactory sgFactory;

	@Override
	public QueryResponse processQuery(String query, HttpHeaders headers) {
		if (Pattern.matches(".+/services/.+", query))
			return processServiceMetadataQuery(query.substring(1));
		else
			return processServiceGroupQuery(query.substring(1));
	}

	private QueryResponse processServiceMetadataQuery(String query) {
		log.trace("Process a ServiceMetadata query");
		Identifier partID, svcID;
		int pidEnd = query.indexOf('/', 1);
		String pidString = pidEnd < 0 ? query : query.substring(0, pidEnd);
		String sidString = query.substring(query.indexOf("/services/") + 10);
		try {
			partID = queryUtils.parseIDString(pidString);
		} catch (NoSuchElementException unknownScheme) {
			log.debug("ID Scheme of queried Participant ID ({}) not found!", pidString);
			return new QueryResponse(HttpStatus.NOT_FOUND, null, null);
		}
		try {
		    svcID = queryUtils.parseIDString(sidString);
		} catch (NoSuchElementException unknownScheme) {
			log.debug("ID Scheme of queried Service ID ({}) not found!", pidString);
			return new QueryResponse(HttpStatus.NOT_FOUND, null, null);
		}
		log.trace("Retrieve SMB for Participant={} and Service={}", partID.toString(), svcID.toString());
		ServiceMetadataBindingE smb = bindings.findByParticipantAndServiceId(partID, svcID);
		if (smb == null) {
			log.debug("No SMB found for Participant={} and Service={}", partID.toString(), svcID.toString());
			return new QueryResponse(HttpStatus.NOT_FOUND, null, null);
		}
		log.trace("Create ServiceMetadata response document");
		Document response, signed;
		try {
			response = getSmdFactory().newResponse(smb);
		} catch (InstantiationException ex) {
			log.error("Error occurred creating the ServiceMetadata response: {}", ex.getMessage());
			return new QueryResponse(HttpStatus.INTERNAL_SERVER_ERROR, null, null);
		}
		log.trace("Sign the response document");
		try {
			signed = signer.signResponse(response, signingAlgorithm, digestMethod, c14nAlgorithm);
		} catch (XMLSignatureException ex) {
			log.error("Error occurred signing the response document. Error details: {}", Utils.getExceptionTrace(ex));
			return new QueryResponse(HttpStatus.INTERNAL_SERVER_ERROR, null, null);
		}
		log.info("Completed ServiceMetadata query for Participant={} and Service={}",
				partID.toString(), svcID.toString());
		return new QueryResponse(HttpStatus.OK, null, signed);
	}

	private QueryResponse processServiceGroupQuery(String query) {
		log.trace("Process a ServiceGroup query");
		Identifier partID;
		try {
			partID = queryUtils.parseIDString(query);
		} catch (NoSuchElementException unknownScheme) {
			log.debug("ID Scheme of queried Participant ID ({}) not found!", query);
			return new QueryResponse(HttpStatus.NOT_FOUND, null, null);
		}
		log.trace("Check if Participant with ID={} exists", partID.toString());
		if (participants.findByIdentifier(partID) == null) {
			log.debug("Queried Participant ID ({}) not found!", query);
			return new QueryResponse(HttpStatus.NOT_FOUND, null, null);
		}
		log.trace("Retrieve SMB for Participant={}", partID.toString());
		List<ServiceMetadataBindingE> smb = bindings.findByParticipantId(partID);
		log.trace("Create ServiceGroup response document");
		Document response;
		try {
			response = getSvcGrpFactory().newResponse(partID, smb, getSMPURL());
		} catch (InstantiationException ex) {
			log.error("Error occurred creating the ServiceGroup response: {}", ex.getMessage());
			return new QueryResponse(HttpStatus.INTERNAL_SERVER_ERROR, null, null);
		}
		log.info("Completed ServiceGroup query for Participant={}", partID.toString());
		return new QueryResponse(HttpStatus.OK, null, response);
	}

	private ServiceMetadataFactory getSmdFactory() {
		if (smdFactory == null)
			smdFactory = new ServiceMetadataFactory();
		return smdFactory;
	}

	private ServiceGroupFactory getSvcGrpFactory() {
		if (sgFactory == null)
			sgFactory = new ServiceGroupFactory();
		return sgFactory;
	}

	private String getSMPURL() {
		String hostname;
		try {
			SMLRegistration reg = smlSvc.getSMLRegistration();
			hostname = reg.getHostname();
		} catch (CertificateException ex) {
			hostname = null;
		}
		if (Utils.isNullOrEmpty(hostname))
			try {
				hostname = InetAddress.getLocalHost().getHostName();
			} catch (UnknownHostException ex) {
				hostname = "localhost";
			}
		return hostname;
	}
}
