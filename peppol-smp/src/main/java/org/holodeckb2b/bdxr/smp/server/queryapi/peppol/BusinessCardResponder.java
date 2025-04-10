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

import eu.peppol.schema.pd.businesscard._20180621.BusinessCardType;
import eu.peppol.schema.pd.businesscard._20180621.BusinessEntityType;
import eu.peppol.schema.pd.businesscard._20180621.IdentifierType;
import eu.peppol.schema.pd.businesscard._20180621.MultilingualNameType;
import lombok.extern.slf4j.Slf4j;

/**
 * Is the component responsible for responding to Business Card queries by the Peppol Directory indexer. 
 * 
 * @author Sander Fieten (sander at holodeck-b2b.org)
 */
@Service("PEPPOLBCResponder")
@Slf4j
public class BusinessCardResponder extends AbstractResponseFactory implements IQueryResponder {
	
	@Autowired
	protected IdUtils	queryUtils;
	@Autowired
	protected ParticipantRepository participants;
			
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
		pid.setScheme(partID.getScheme() == null ? null : partID.getScheme().getSchemeId());
		pid.setValue(partID.getValue());
		bc.setParticipantIdentifier(pid);
		BusinessEntityType busInfo = new BusinessEntityType();
		MultilingualNameType name = new MultilingualNameType();
		name.setValue(participant.getName());
		busInfo.getName().add(name);
		busInfo.setCountryCode(participant.getCountry());
		busInfo.setGeographicalInformation(participant.getAddressInfo());
		busInfo.setRegistrationDate(createDateContent(participant.getFirstRegistration()));
		String additionalIds = participant.getAdditionalIds();
		if (additionalIds != null && additionalIds.length() > 1) {
			for(String id : additionalIds.split(",")) {
				int schemeEnd = Math.max(0, id.indexOf("::"));
				IdentifierType idType = new IdentifierType();
				idType.setScheme(id.substring(0, schemeEnd));
				idType.setValue(id.substring(schemeEnd > 0 ? schemeEnd + 2 : 0));
				busInfo.getIdentifier().add(idType);
			}
		}		
		bc.getBusinessEntity().add(busInfo);			
		
		try {
			log.debug("Return BusinessCard of Participant ({}) to Peppol Directory indexer", partID.toString());
			return new QueryResponse(HttpStatus.OK, null, jaxb2dom(bc));
		} catch (InstantiationException ex) {
			log.error("Error in conversion of BusinessCard XML for Participant ({}) : {}", partID.toString(), ex.getMessage());
			return new QueryResponse(HttpStatus.INTERNAL_SERVER_ERROR, null, null);
		}
	}
}
