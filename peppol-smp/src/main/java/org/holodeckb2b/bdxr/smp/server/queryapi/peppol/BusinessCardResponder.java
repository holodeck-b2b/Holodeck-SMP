/*
 * Copyright (C) 2023 The Holodeck B2B Team
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

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.NoSuchElementException;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.namespace.QName;
import javax.xml.transform.dom.DOMResult;

import org.holodeckb2b.bdxr.smp.datamodel.Identifier;
import org.holodeckb2b.bdxr.smp.server.db.entities.ParticipantE;
import org.holodeckb2b.bdxr.smp.server.db.repos.ParticipantRepository;
import org.holodeckb2b.bdxr.smp.server.queryapi.IQueryResponder;
import org.holodeckb2b.bdxr.smp.server.queryapi.QueryResponse;
import org.holodeckb2b.bdxr.smp.server.svc.IdUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;

import eu.peppol.schema.pd.businesscard._20161123.BusinessCardType;
import eu.peppol.schema.pd.businesscard._20161123.BusinessEntityType;
import eu.peppol.schema.pd.businesscard._20161123.IdentifierType;
import lombok.extern.slf4j.Slf4j;

/**
 * Is the component responsible for responding to Business Card queries by the Peppol Directory indexer. 
 * 
 * @author Sander Fieten (sander at holodeck-b2b.org)
 */
@Service("PEPPOLBCResponder")
@Slf4j
public class BusinessCardResponder implements IQueryResponder {
	
	@Autowired
	protected IdUtils	queryUtils;
	@Autowired
	protected ParticipantRepository participants;
		
	protected static final QName BC_QNAME = new QName("http://www.peppol.eu/schema/pd/businesscard/20161123/", "BusinessCard");
	protected static final JAXBContext JAXB_CTX;
	static {
		try {
			JAXB_CTX = JAXBContext.newInstance(BusinessCardType.class);
		} catch (JAXBException ex) {
			throw new RuntimeException("Failed to initialise JAXBContext", ex);
		}
	}
	
	@Override
	public QueryResponse processQuery(String query, HttpHeaders headers) {
		log.trace("Process a BusinessCard query");
		Identifier partID;
		// The query must always start with "/businesscard/", so we can start looking for the participant identifier
		// after 14 characters
		if (query.length() < 14) {
			log.warn("Missing ParticipantID");
			return new QueryResponse(HttpStatus.BAD_REQUEST, null, null);
		}		
		String pidString = URLDecoder.decode(query.substring(14), StandardCharsets.UTF_8);
		try {
			partID = queryUtils.parseIDString(pidString);
		} catch (NoSuchElementException unknownScheme) {
			log.warn("ID Scheme of queried Participant ID ({}) not found!", pidString);
			return new QueryResponse(HttpStatus.NOT_FOUND, null, null);
		}
		
		log.trace("Business Card requested of Participant={}", partID.toString());
		ParticipantE participant = participants.findByIdentifier(partID);
		
		if (participant == null || !participant.publishedInDirectory()) {
			log.warn("Got Business Card request for non-existing or not published Participant ID ({})", partID.toString());
			return new QueryResponse(HttpStatus.NOT_FOUND, null, null);
		}
		
		log.trace("Create BusinessCard for Participant ({})", partID.toString());
		BusinessCardType bc = new BusinessCardType();
		IdentifierType pid = new IdentifierType();
		pid.setScheme(partID.getScheme().getSchemeId());
		pid.setValue(partID.getValue());
		bc.setParticipantIdentifier(pid);
		BusinessEntityType busInfo = new BusinessEntityType();
		busInfo.setName(participant.getName());
		busInfo.setCountryCode(participant.getCountry());
		busInfo.setGeographicalInformation(participant.getAddressInfo());
		bc.getBusinessEntity().add(busInfo);				 
		try {
			DOMResult res = new DOMResult();
			JAXB_CTX.createMarshaller().marshal(new JAXBElement<BusinessCardType>(BC_QNAME, BusinessCardType.class, bc), res);
			log.debug("Return BusinessCard of Participant ({}) to Peppol Directory indexer", partID.toString());
			return new QueryResponse(HttpStatus.OK, null, (Document) res.getNode());
		} catch (JAXBException ex) {
			log.error("Error in conversion of BusinessCard XML for Participant ({}) : {}", partID.toString(), ex.getMessage());
			return new QueryResponse(HttpStatus.INTERNAL_SERVER_ERROR, null, null);
		}
	}
}
