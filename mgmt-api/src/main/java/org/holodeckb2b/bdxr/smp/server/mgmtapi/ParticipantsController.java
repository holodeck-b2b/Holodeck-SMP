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
import org.holodeckb2b.bdxr.smp.server.svc.DirectoryException;
import org.holodeckb2b.bdxr.smp.server.svc.DirectoryIntegrationService;
import org.holodeckb2b.bdxr.smp.server.svc.IdUtils;
import org.holodeckb2b.bdxr.smp.server.svc.SMLException;
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

import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/participants")
@Slf4j
public class ParticipantsController {

	@Autowired
	protected ParticipantRepository	participants;
	
	@Autowired
	protected SMLIntegrationService smlService;	
	@Autowired
	protected DirectoryIntegrationService directoryService;
	@Autowired
	protected IdUtils idUtils;
	
	@PutMapping("/{partID}")
	@ResponseStatus(HttpStatus.CREATED)
	public void addParticipant(@PathVariable("partID") String partID) {
		log.debug("Request to add Participant with ID={}", partID);
		IdentifierE pid;
		try {
			pid = idUtils.parseIDString(partID);
		} catch (NoSuchElementException unknownScheme) {
			log.warn("ID Scheme of given Participant ID ({}) not found!", partID);
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
		}
		
		if (participants.findByIdentifier(pid) != null) {
			log.warn("Cannot add Participant as the PartID ({}) already exists", pid.toString());
			throw new ResponseStatusException(HttpStatus.CONFLICT);
		}
		
		ParticipantE p = new ParticipantE();
		try {
			log.trace("Adding new Participant");
			p.setId(pid);
			p.setIsRegisteredSML(smlService.isSMLIntegrationAvailable());
			p = participants.save(p);
			log.info("Added new Participant with ID {} to database", pid.toString());
		} catch (Exception e) {
			log.error("Could not save new Participant (PID={}) to database : {}", pid.toString(), Utils.getExceptionTrace(e));
			throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR);
		}
		if (p.registeredInSML())
			try {
				log.trace("Register the Participant in the SML");
				smlService.registerParticipant(p);
				log.info("Registered new Participant in SML");			
			} catch (SMLException smlRegFailed) {
				log.error("Revert Participant (ID={}) registration because it could not be registered in SML", pid.toString());
				participants.delete(p);
				throw new ResponseStatusException(HttpStatus.FAILED_DEPENDENCY);
			}
	}
	
	@DeleteMapping("/{partID}")
	@ResponseStatus(HttpStatus.ACCEPTED)
	public void removeParticipant(@PathVariable("partID") String partID) {
		log.debug("Request to remove Participant with ID={}", partID);
		IdentifierE pid;
		try {
			pid = idUtils.parseIDString(partID);
		} catch (NoSuchElementException unknownScheme) {
			log.warn("ID Scheme of given Participant ID ({}) not found!", partID);
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
		}
		
		ParticipantE p = participants.findByIdentifier(pid);
		if (p == null) {
			log.info("Participant with PartID ({}) not found, nothing to remove", pid.toString());
			return;
		}
		
		if (p.publishedInDirectory()) {
			try {
				log.trace("Remove Participant from the directory");
				directoryService.removeParticipantInfo(p);
			} catch (DirectoryException dirRemoveFailed) {
				log.error("An error occurred removing the Participant (ID={}) from the directory", p.getId().toString());
				throw new ResponseStatusException(HttpStatus.FAILED_DEPENDENCY);
			}
		}
		if (p.registeredInSML()) {
			try {
				log.trace("Remove Participant from the SML");
				smlService.unregisterParticipant(p);
			} catch (SMLException smlRemoveFailed) {
				log.error("An error occurred removing the Participant (ID={}) from the SML", p.getId().toString());
				if (p.publishedInDirectory()) {
					log.trace("Republish Participant in directory");
					try {
						directoryService.publishParticipantInfo(p);
					} catch (DirectoryException republishFailed) {
						log.error("Could not republish Participant to directory. Updating info");
						p.setPublishedInDirectory(false);
						p = participants.save(p);
					}
				}
				throw new ResponseStatusException(HttpStatus.FAILED_DEPENDENCY);
			}			
		}
		log.trace("Removing Participant (ID={}) from database", p.getId().toString());
		participants.delete(p);
		log.info("Removed Participant (ID={}) from database", p.getId().toString());
	}
	
}
