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

import java.util.Collection;
import java.util.NoSuchElementException;
import java.util.regex.Pattern;

import javax.xml.crypto.dsig.XMLSignatureException;

import org.holodeckb2b.bdxr.common.datamodel.Identifier;
import org.holodeckb2b.bdxr.smp.server.datamodel.Participant;
import org.holodeckb2b.bdxr.smp.server.datamodel.ServiceMetadataTemplate;
import org.holodeckb2b.bdxr.smp.server.queryapi.ResponseSigner;
import org.holodeckb2b.bdxr.smp.server.services.core.ParticipantsService;
import org.holodeckb2b.bdxr.smp.server.services.core.PersistenceException;
import org.holodeckb2b.bdxr.smp.server.services.core.SMPServerAdminService;
import org.holodeckb2b.bdxr.smp.server.services.query.IQueryResponder;
import org.holodeckb2b.bdxr.smp.server.services.query.QueryResponse;
import org.holodeckb2b.bdxr.smp.server.utils.IdUtils;
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
	protected ResponseSigner	signer;
	@Autowired
	protected ParticipantsService  participantsSvc;
	@Autowired
	protected SMPServerAdminService	adminSvc;
	
	private ServiceMetadataFactory smdFactory;
	private ServiceGroupFactory sgFactory;

	@Override
	public QueryResponse processQuery(String query, HttpHeaders headers) {
		try {
			if (Pattern.matches(".+/services/.+", query))
				return processServiceMetadataQuery(query.substring(1));
			else
				return processServiceGroupQuery(query.substring(1));
		} catch (Throwable t) {
			log.error("Error during processing query ({}): {}", query, Utils.getExceptionTrace(t));
			return new QueryResponse(HttpStatus.INTERNAL_SERVER_ERROR, null, null);
		}
	}

	private QueryResponse processServiceMetadataQuery(String query) throws PersistenceException, InstantiationException,
			 																				XMLSignatureException {
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
		log.trace("Retrieve Participant={}", partID.toString());
		Participant p = participantsSvc.getParticipant(partID);
		ServiceMetadataTemplate smt = p == null ? null : 
					p.getBoundSMT().stream().filter(t -> t.getService().getId().equals(svcID)).findFirst().orElse(null);
		if (smt == null) {
			log.debug("No template found for Participant={} and Service={}", partID.toString(), svcID.toString());
			return new QueryResponse(HttpStatus.NOT_FOUND, null, null);
		}
		log.trace("Create ServiceMetadata response document");
		Document response = getSmdFactory().newResponse(p.getId(), smt);
		log.trace("Sign the response document");
		response = signer.signResponse(response, signingAlgorithm, digestMethod, c14nAlgorithm);
		log.info("Completed ServiceMetadata query for Participant={} and Service={}",
				partID.toString(), svcID.toString());
		return new QueryResponse(HttpStatus.OK, null, response);
	}

	private QueryResponse processServiceGroupQuery(String query) throws PersistenceException, InstantiationException {
		log.trace("Process a ServiceGroup query");
		Identifier partID;
		try {
			partID = queryUtils.parseIDString(query);
		} catch (NoSuchElementException unknownScheme) {
			log.debug("ID Scheme of queried Participant ID ({}) not found!", query);
			return new QueryResponse(HttpStatus.NOT_FOUND, null, null);
		}
		log.trace("Check if Participant with ID={} exists", partID.toString());
		Participant p = participantsSvc.getParticipant(partID);
		if (p  == null) {
			log.debug("Queried Participant ID ({}) not found!", query);
			return new QueryResponse(HttpStatus.NOT_FOUND, null, null);
		}
		Collection<ServiceMetadataTemplate> boundSMT = p.getBoundSMT();
		log.trace("Create ServiceGroup response document");
		Document response = getSvcGrpFactory().newResponse(partID, boundSMT, adminSvc.getServerMetadata().getBaseUrl());
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
}
