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

import org.holodeckb2b.bdxr.smp.server.datamodel.Participant;
import org.holodeckb2b.bdxr.smp.server.datamodel.ServiceMetadataTemplate;
import org.holodeckb2b.bdxr.smp.server.db.entities.EmbeddedIdentifier;
import org.holodeckb2b.bdxr.smp.server.mgmtapi.xml.ResponseFactory;
import org.holodeckb2b.bdxr.smp.server.mgmtapi.xml.v2025.ServiceMetadataBindingsElement;
import org.holodeckb2b.bdxr.smp.server.services.core.ParticipantsService;
import org.holodeckb2b.bdxr.smp.server.services.core.PersistenceException;
import org.holodeckb2b.bdxr.smp.server.services.core.SMTMgmtService;
import org.holodeckb2b.bdxr.smp.server.utils.IdUtils;
import org.holodeckb2b.commons.util.Utils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.User;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/participants/{partID}/bindings")
@Slf4j
public class BindingsController {

	@Autowired
	protected User	mgmtAPIUser;
	
	@Autowired
	protected IdUtils idUtils;
	
	@Autowired
	protected ParticipantsService participantsSvc;

	@Autowired
	protected SMTMgmtService smtSvc;
	
	@GetMapping(produces = MediaType.APPLICATION_XML_VALUE )
	public ServiceMetadataBindingsElement getBindings(@PathVariable("partID") String partID) {
		log.debug("Request to list all bindings for Participant {}", partID);
		try {		
			return ResponseFactory.createBindingsResponse(findParticipant(partID));
		} catch (InstantiationException e) {
			log.error("Error while creating ServiceMetadataBindings XML document : {}", Utils.getExceptionTrace(e));
			throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR); 
		}
	}
	
	@PutMapping("/{smtID}")
	@ResponseStatus(HttpStatus.CREATED)	
	public void addBinding(@PathVariable("partID") String partID, @PathVariable("smtID") String templateId) {
		log.debug("Request to bind SMT {} to Participant {}", templateId, partID);
		long smtId;
		try {
			smtId = Long.parseLong(templateId);
		} catch (NumberFormatException invalidOid) {
			log.warn("Invalid value provided for template ID : {}", templateId);
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
		}		
		try {
			Participant participant = findParticipant(partID);
			ServiceMetadataTemplate template = smtSvc.getTemplate(smtId);
			if (template == null) {
				log.warn("Unable to add binding as no template with ID ({}) is found", smtId);
				throw new ResponseStatusException(HttpStatus.NOT_FOUND);
			}	
			participantsSvc.bindToSMT(mgmtAPIUser, participant, template);
			log.info("Added new binding ({},{}-{})", participant.getId().toString(), template.getId(), 
					template.getName());
		} catch (PersistenceException dbError) {
			log.error("Error during binding of SMT ({}) to Participant ({}) : {}", smtId, partID,
						Utils.getExceptionTrace(dbError));
			throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}
	
	@DeleteMapping("/{smtID}")
	@ResponseStatus(HttpStatus.ACCEPTED)
	public void removeBinding(@PathVariable("partID") String partID, @PathVariable("smtID") String templateId) {
		log.debug("Request to remove binding of SMT {} to Participant {}", templateId, partID);
		long smtId;
		try {
			smtId = Long.parseLong(templateId);
		} catch (NumberFormatException invalidOid) {
			log.warn("Invalid value provided for template ID : {}", templateId);
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
		}		
		try {
			Participant participant = findParticipant(partID);
			ServiceMetadataTemplate template = smtSvc.getTemplate(smtId);
			if (template == null) {
				log.warn("Unable to remove binding as no template with ID ({}) is found", smtId);
				throw new ResponseStatusException(HttpStatus.NOT_FOUND);
			}	
			participantsSvc.removeSMTBinding(mgmtAPIUser, participant, template);
			log.info("Removed binding ({},{}-{})", participant.getId().toString(), template.getId(), 
					template.getName());
		} catch (PersistenceException dbError) {
			log.error("Error during binding of SMT ({}) to Participant ({}) : {}", smtId, partID,
						Utils.getExceptionTrace(dbError));
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
