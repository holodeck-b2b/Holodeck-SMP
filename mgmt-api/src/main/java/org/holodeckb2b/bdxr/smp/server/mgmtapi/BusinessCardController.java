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

import org.holodeckb2b.bdxr.common.datamodel.impl.IdentifierImpl;
import org.holodeckb2b.bdxr.smp.server.datamodel.Participant;
import org.holodeckb2b.bdxr.smp.server.db.entities.EmbeddedIdentifier;
import org.holodeckb2b.bdxr.smp.server.mgmtapi.xml.BusinessEntity;
import org.holodeckb2b.bdxr.smp.server.services.core.ParticipantsService;
import org.holodeckb2b.bdxr.smp.server.services.core.PersistenceException;
import org.holodeckb2b.bdxr.smp.server.services.network.DirectoryException;
import org.holodeckb2b.bdxr.smp.server.services.network.SMLException;
import org.holodeckb2b.bdxr.smp.server.utils.IdUtils;
import org.holodeckb2b.commons.util.Utils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.userdetails.User;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/participants/{partID}/businesscard")
@Slf4j
public class BusinessCardController {

	@Autowired
	protected User	mgmtAPIUser;
	
	@Autowired
	protected IdUtils idUtils;
	
	@Autowired
	protected ParticipantsService participantsSvc;

	@PutMapping
	@ResponseStatus(HttpStatus.ACCEPTED)
	public void updateBusinessCard(@PathVariable("partID") String partID, @RequestBody BusinessEntity bc) {
		log.debug("Request to update the Business Card info for Participant {}", partID);
		if (Utils.isNullOrEmpty(bc.getName()) || Utils.isNullOrEmpty(bc.getCountryCode())) {
			log.warn("Missing required information in BC update request for Participant ({})", partID);
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
		}
		Participant p = findParticipant(partID);
		
		p.setName(bc.getName());
		p.setRegistrationCountry(bc.getCountryCode());
		p.setLocationInfo(bc.getGeographicalInformation());
		if (bc.getRegistrationDate() != null)
			p.setFirstRegistrationDate(bc.getRegistrationDate().toGregorianCalendar().toZonedDateTime().toLocalDate());
		
		bc.getIdentifier().forEach(id -> p.addAdditionalId(new IdentifierImpl(id.getValue(), id.getScheme())));
		log.trace("Updating business card info");
		try {
			Participant updated = participantsSvc.updateParticipant(mgmtAPIUser, p);
			log.info("Updated business card info of Participant {}", p.getId().toString());
			if (!p.isPublishedInDirectory() && participantsSvc.isDirectoryPublicationAvailable()) {
				log.debug("Publish Participant to Directory");
				participantsSvc.publishInDirectory(mgmtAPIUser, updated);			
			}
		} catch (DirectoryException directoryError) {
			log.error("Error occurred publishing updated Participant (ID={}) to Directory : {}", partID,
					  Utils.getExceptionTrace(directoryError));
			throw new ResponseStatusException(HttpStatus.PARTIAL_CONTENT);
		} catch (PersistenceException e) {
			log.error("Error in updating Participant info ({}) : {}", p.getId().toString(), Utils.getExceptionTrace(e));
			throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}
	
	@DeleteMapping
	@ResponseStatus(HttpStatus.ACCEPTED)
	public void deleteBusinessCard(@PathVariable("partID") String partID) {
		log.debug("Request to remove Participant ({}) from network directory", partID);		
		try {
			participantsSvc.removeFromDirectory(mgmtAPIUser, findParticipant(partID));
			log.info("Removed Participant {} from directory", partID);
		} catch (DirectoryException directoryError) {
			log.error("Error occurred removing Participant (ID={}) from Directory : {}", partID,
					  Utils.getExceptionTrace(directoryError));
			throw new ResponseStatusException(HttpStatus.FAILED_DEPENDENCY);
		} catch (PersistenceException pe) {
			log.error("Error occurred updating Participant (ID={}) meta-data : {}", partID,
					Utils.getExceptionTrace(pe));
			throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	/**
	 * Helper method to retrieve the Participant registration from the database.
	 * 
	 * @param partID	the identifier of the Participant
	 * @return	the Participant's registration in the database if it exists
	 * @throws ResponseStatusException when the given string does not represent a valid Participant ID (BAD_REQUEST),
	 * 								   no Participant with the given ID is found (NOT_FOUND) or
	 * 								   an error occurs checking the Participant registrations (INTERNAL_SERVER_ERROR) 
	 */
	private Participant findParticipant(String partID) throws ResponseStatusException {
		try {
			EmbeddedIdentifier pid = idUtils.toEmbeddedIdentifier(idUtils.parseIDString(partID));
			Participant p = participantsSvc.getParticipant(pid);
			if (p == null) {
				log.warn("No Participant with ID ({}) is found", pid.toString());
				throw new ResponseStatusException(HttpStatus.NOT_FOUND);
			} else
				return p;
		} catch (NoSuchElementException unknownScheme) {
			log.warn("ID Scheme of given Participant ID ({}) not found!", partID);
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
		} catch (PersistenceException pe) {
			log.error("Unexpected error checking for Participant (PID={}) : {}", partID, Utils.getExceptionTrace(pe));
			throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR);
		}		
	}	
}
