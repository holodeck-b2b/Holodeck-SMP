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
package org.holodeckb2b.bdxr.smp.server.queryapi.oasisv2;

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
 * Is the component responsible for processing SMP queries as specified in the OASIS SMP V2 Specification.
 *
 * @author Sander Fieten (sander at holodeck-b2b.org)
 */
@Service
@Slf4j
public class OASISv2QueryResponder implements IQueryResponder {

	private static final String URL_PREFIX = "/bdxr-smp-2/";
	
	private static final String SIGNING_ALG = "http://www.w3.org/2001/04/xmldsig-more#rsa-sha256";
	private static final String DIGEST_ALG = "http://www.w3.org/2001/04/xmlenc#sha256";
	private static final String C14N_ALG = "http://www.w3.org/2006/12/xml-c14n11";

	@Autowired
	protected IdUtils	idUtils;
	@Autowired
	protected ResponseSigner	signer;
	@Autowired
	protected ParticipantsService participantsSvc;
	
	@Value("${smp.smp2_cert_mime-type:application/pkix-cert}")
	protected String certMimeType;

	private ServiceMetadataFactory smdFactory;
	private ServiceGroupFactory sgFactory;

	@Override
	public QueryResponse processQuery(String queryPath, HttpHeaders headers) {
		if (!queryPath.startsWith(URL_PREFIX)) {
			log.error("Invalid query path: {}", queryPath);
			return new QueryResponse(HttpStatus.BAD_REQUEST, null, null);
		}
		String query = queryPath.substring(12);
		try {
			if (Pattern.matches(".+/services/.+", query))
				return processServiceMetadataQuery(query);
			else
				return processServiceGroupQuery(query);
		} catch (Throwable t) {
			log.error("Error occurred processing the query ({}): {}", query,Utils.getExceptionTrace(t));
			return new QueryResponse(HttpStatus.INTERNAL_SERVER_ERROR, null, null);
		}
	}

	private QueryResponse processServiceMetadataQuery(String query) throws PersistenceException, InstantiationException, 
			 																				XMLSignatureException {
		log.trace("Process a ServiceMetadata query");
		Identifier partID, svcID;
		int pidEnd = query.indexOf('/');
		String pidString = pidEnd < 0 ? query : query.substring(0, pidEnd);
		String sidString = query.substring(query.indexOf("/services/") + 10);
		try {
		    svcID = idUtils.parseIDString(sidString);
		} catch (NoSuchElementException unknownScheme) {
			log.debug("ID Scheme of queried Service ID ({}) not found!", pidString);
			return new QueryResponse(HttpStatus.NOT_FOUND, null, null);
		}
		try {
			partID = idUtils.parseIDString(pidString);
		} catch (NoSuchElementException unknownScheme) {
			log.debug("ID Scheme of queried Participant ID ({}) not found!", pidString);
			return new QueryResponse(HttpStatus.NOT_FOUND, null, null);
		}	
		log.trace("Retrieve bound templates for Participant={} ", partID.toString());
		Participant p = participantsSvc.getParticipant(partID);
		ServiceMetadataTemplate smt = p == null ? null : 
					p.getBoundSMT().stream().filter(t -> t.getService().getId().equals(svcID)).findFirst().orElse(null);
		if (smt == null) {
			log.debug("No template found for Participant={} and Service={}", partID.toString(), svcID.toString());
			return new QueryResponse(HttpStatus.NOT_FOUND, null, null);
		}
		log.trace("Create ServiceMetadata response document");
		Document response, signed;
		response = getSmdFactory().newResponse(partID, smt);
		log.trace("Sign the response document");
		signed = signer.signResponse(response, SIGNING_ALG, DIGEST_ALG, C14N_ALG);
		log.info("Completed ServiceMetadata query for Participant={} and Service={}",
				partID.toString(), svcID.toString());
		return new QueryResponse(HttpStatus.OK, null, signed);
	}

	private QueryResponse processServiceGroupQuery(String query) throws PersistenceException, InstantiationException,
																								XMLSignatureException {
		log.trace("Process a ServiceGroup query");
		Identifier partID;
		try {
			partID = idUtils.parseIDString(query);
		} catch (NoSuchElementException unknownScheme) {
			log.debug("ID Scheme of queried Participant ID ({}) not found!", query);
			return new QueryResponse(HttpStatus.NOT_FOUND, null, null);
		}
		log.trace("Check if Participant with ID={} exists", partID.toString());
		Participant p = participantsSvc.getParticipant(partID);
		if (p == null) {
			log.debug("Queried Participant ID ({}) not found!", query);
			return new QueryResponse(HttpStatus.NOT_FOUND, null, null);
		}
		Collection<ServiceMetadataTemplate> boundSMT = p.getBoundSMT();
		if (boundSMT.isEmpty()) {
			log.debug("No templates bound to Participant={}", partID.toString());
			return new QueryResponse(HttpStatus.NOT_FOUND, null, null);
		}
		log.trace("Create ServiceGroup response document");
		Document response, signed;
		response = getSvcGrpFactory().newResponse(p.getId(), boundSMT);
		log.trace("Sign the response document");
		signed = signer.signResponse(response, SIGNING_ALG, DIGEST_ALG, C14N_ALG);
		log.info("Completed ServiceGroup query for Participant={}", partID.toString());
		return new QueryResponse(HttpStatus.OK, null, signed);
	}
	
	private ServiceMetadataFactory getSmdFactory() {
		if (smdFactory == null)
			smdFactory = new ServiceMetadataFactory(certMimeType);
		return smdFactory;
	}

	private ServiceGroupFactory getSvcGrpFactory() {
		if (sgFactory == null)
			sgFactory = new ServiceGroupFactory();
		return sgFactory;
	}
}
