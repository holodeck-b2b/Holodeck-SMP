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
package org.holodeckb2b.bdxr.smp.server.mgmtapi;

import java.util.NoSuchElementException;

import org.holodeckb2b.bdxr.smp.server.db.entities.IdentifierE;
import org.holodeckb2b.bdxr.smp.server.db.entities.ParticipantE;
import org.holodeckb2b.bdxr.smp.server.db.repos.ParticipantRepository;
import org.holodeckb2b.bdxr.smp.server.mgmtapi.xml.BusinessEntity;
import org.holodeckb2b.bdxr.smp.server.svc.DirectoryException;
import org.holodeckb2b.bdxr.smp.server.svc.DirectoryIntegrationService;
import org.holodeckb2b.bdxr.smp.server.svc.IdUtils;
import org.holodeckb2b.bdxr.smp.server.svc.SMLIntegrationService;
import org.holodeckb2b.commons.util.Utils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import eu.peppol.schema.pd.businesscard._20161123.IdentifierType;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/participants/{partID}/businesscard")
@Slf4j
public class BusinessCardController {

	@Autowired
	protected ParticipantRepository	participants;
	
	@Autowired
	protected SMLIntegrationService smlService;	
	@Autowired
	protected DirectoryIntegrationService directoryService;
	@Autowired
	protected IdUtils idUtils;

	@PutMapping
	@ResponseStatus(HttpStatus.ACCEPTED)
	public void updateBusinessCard(@PathVariable("partID") String partID, @RequestBody BusinessEntity bc) {
		log.debug("Request to update the Business Card info for Participant {}", partID);
		IdentifierE pid;
		try {
			pid = idUtils.parseIDString(partID);
		} catch (NoSuchElementException unknownScheme) {
			log.warn("ID Scheme of given Participant ID ({}) not found!", partID);
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
		}
		
		ParticipantE p = participants.findByIdentifier(pid);
		if (p == null) {
			log.warn("Cannot update business card info as Participant with PartID ({}) is not found", pid.toString());
			throw new ResponseStatusException(HttpStatus.NOT_FOUND);
		}

		if (Utils.isNullOrEmpty(bc.getName()) || Utils.isNullOrEmpty(bc.getCountryCode())) {
			log.warn("Missing required information in update request for Participant ({})", pid.toString());
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
		}
		
		p.setName(bc.getName());
		p.setCountry(bc.getCountryCode());
		p.setAddressInfo(bc.getGeographicalInformation());
		if (bc.getRegistrationDate() != null)
			p.setFirstRegistration(bc.getRegistrationDate().toGregorianCalendar().toZonedDateTime().toLocalDate());
		StringBuilder additionalIds = new StringBuilder();
		for(IdentifierType id : bc.getIdentifier()) {
			if (!Utils.isNullOrEmpty(id.getScheme()))
				additionalIds.append(id.getScheme()).append("::");
			additionalIds.append(id.getValue()).append(',');
		}
		p.setAdditionalIds(additionalIds.length() > 0 ? additionalIds.toString() : null);
		log.trace("Updating business card info");
		p = participants.save(p);
		log.debug("Saved participant data");
		
		if (directoryService.isDirectoryIntegrationAvailable()) 
			try {
				log.trace("Publish participant to directory");
				directoryService.publishParticipantInfo(p);
				p.setPublishedInDirectory(true);
				participants.save(p);
				log.debug("Published participant to directory");
			} catch (DirectoryException publishFailure) {
				log.error("Error in publishing Participant ({}) to directoty : {}", pid.toString(), publishFailure.getMessage());
				throw new ResponseStatusException(HttpStatus.PARTIAL_CONTENT);
			}
		
		log.info("Updated business card info of Participant {}", pid.toString());
	}
	
	@DeleteMapping
	@ResponseStatus(HttpStatus.ACCEPTED)
	public void deleteBusinessCard(@PathVariable("partID") String partID) {
		log.debug("Request to update the Business Card info for Participant {}", partID);
		IdentifierE pid;
		try {
			pid = idUtils.parseIDString(partID);
		} catch (NoSuchElementException unknownScheme) {
			log.warn("ID Scheme of given Participant ID ({}) not found!", partID);
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
		}
		
		ParticipantE p = participants.findByIdentifier(pid);
		if (p == null) {
			log.warn("Cannot update business card info as Participant with PartID ({}) is not found", pid.toString());
			throw new ResponseStatusException(HttpStatus.NOT_FOUND);
		}
		
		if (p.publishedInDirectory()) 
			try {
				log.trace("Remove participant from directory");
				directoryService.removeParticipantInfo(p);
				p.setPublishedInDirectory(false);
				participants.save(p);
				log.debug("Removed participant from directory");
			} catch (DirectoryException publishFailure) {
				log.error("Error in publishing Participant ({}) to directoty : {}", pid.toString(), publishFailure.getMessage());
				throw new ResponseStatusException(HttpStatus.FAILED_DEPENDENCY);
			}
		
		log.info("Removed Participant {} from directory", pid.toString());
	}
}
