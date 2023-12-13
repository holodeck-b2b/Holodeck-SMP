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

import java.util.List;
import java.util.NoSuchElementException;
import java.util.regex.Pattern;
import javax.xml.crypto.dsig.XMLSignatureException;
import lombok.extern.slf4j.Slf4j;
import org.holodeckb2b.bdxr.smp.datamodel.Identifier;
import org.holodeckb2b.bdxr.smp.server.db.entities.ServiceMetadataBindingE;
import org.holodeckb2b.bdxr.smp.server.db.repos.ParticipantRepository;
import org.holodeckb2b.bdxr.smp.server.db.repos.ServiceMetadataBindingRepository;
import org.holodeckb2b.bdxr.smp.server.queryapi.IQueryResponder;
import org.holodeckb2b.bdxr.smp.server.queryapi.QueryResponse;
import org.holodeckb2b.bdxr.smp.server.queryapi.XMLResponseSigner;
import org.holodeckb2b.bdxr.smp.server.svc.IdUtils;
import org.holodeckb2b.commons.util.Utils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;

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

	@Autowired
	protected IdUtils	queryUtils;
	@Autowired
	protected XMLResponseSigner	signer;
	@Autowired
	protected ParticipantRepository participants;
	@Autowired
	protected ServiceMetadataBindingRepository bindings;
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
		if (Pattern.matches(".+/services/.+", query))
			return processServiceMetadataQuery(query);
		else
			return processServiceGroupQuery(query);
	}

	private QueryResponse processServiceMetadataQuery(String query) {
		log.trace("Process a ServiceMetadata query");
		Identifier partID, svcID;
		int pidEnd = query.indexOf('/');
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
			signed = signer.signResponse(response, SIGNING_ALG, DIGEST_ALG);
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
		if (smb.isEmpty()) {
			log.debug("No SMB found for Participant={}", partID.toString());
			return new QueryResponse(HttpStatus.NOT_FOUND, null, null);
		}
		log.trace("Create ServiceGroup response document");
		Document response, signed;
		try {
			response = getSvcGrpFactory().newResponse(smb);
		} catch (InstantiationException ex) {
			log.error("Error occurred creating the ServiceGroup response: {}", ex.getMessage());
			return new QueryResponse(HttpStatus.INTERNAL_SERVER_ERROR, null, null);
		}
		log.trace("Sign the response document");
		try {
			signed = signer.signResponse(response, SIGNING_ALG, DIGEST_ALG);
		} catch (XMLSignatureException ex) {
			log.error("Error occurred signing the response document. Error details: {}", Utils.getExceptionTrace(ex));
			return new QueryResponse(HttpStatus.INTERNAL_SERVER_ERROR, null, null);
		}
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
