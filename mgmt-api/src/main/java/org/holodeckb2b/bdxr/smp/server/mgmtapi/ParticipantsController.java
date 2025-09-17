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
import org.holodeckb2b.bdxr.smp.server.db.entities.EmbeddedIdentifier;
import org.holodeckb2b.bdxr.smp.server.db.entities.ParticipantEntity;
import org.holodeckb2b.bdxr.smp.server.services.core.ParticipantsService;
import org.holodeckb2b.bdxr.smp.server.services.core.PersistenceException;
import org.holodeckb2b.bdxr.smp.server.services.network.SMLException;
import org.holodeckb2b.bdxr.smp.server.utils.IdUtils;
import org.holodeckb2b.commons.util.Utils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.User;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/participants")
@Slf4j
public class ParticipantsController {

	@Autowired
	protected User		mgmtAPIUser;
	
	@Value("${sml.autoregistration:true}")
	protected boolean	autoRegisterInSML;
	
	@Autowired
	protected ParticipantsService	participantsSvc;
	
	@Autowired
	protected IdUtils idUtils;
	
	@PutMapping("/{partID}")
	@ResponseStatus(HttpStatus.CREATED)
	public void addParticipant(@PathVariable("partID") String partID, 
							   @RequestParam(required = false) String migrationCode) {		
		log.debug("Request to {} Participant with ID={}", Utils.isNullOrEmpty(migrationCode) ? "add" : "migrate", partID);
		EmbeddedIdentifier pid;
		try {
			pid = idUtils.toEmbeddedIdentifier(idUtils.parseIDString(partID));
		} catch (NoSuchElementException unknownScheme) {
			log.warn("ID Scheme of given Participant ID ({}) not found!", partID);
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
		}		
		try {
			if (participantsSvc.getParticipant(pid) != null) {
				log.warn("Cannot add Participant as the PartID ({}) already exists", pid.toString());
				throw new ResponseStatusException(HttpStatus.CONFLICT);
			}		
			log.trace("Adding new Participant");
			ParticipantEntity p = new ParticipantEntity();
			p.setId(pid);
			p = (ParticipantEntity) participantsSvc.addParticipant(mgmtAPIUser, p);
			log.info("Added new Participant with ID {} to database", pid.toString());
			if (autoRegisterInSML)
				registerInSML(p, migrationCode);			
		} catch (PersistenceException e) {
			log.error("Could not save new Participant (PID={}) to database : {}", pid.toString(), Utils.getExceptionTrace(e));
			throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}	
	
	@DeleteMapping("/{partID}")
	@ResponseStatus(HttpStatus.ACCEPTED)
	public void removeParticipant(@PathVariable("partID") String partID) {
		log.debug("Request to remove Participant with ID={}", partID);
		Participant p = findParticipant(partID);
		if (p == null) {
			log.info("Participant with PartID ({}) not found, nothing to remove", partID);
		} else {
			try {
				log.trace("Removing Participant (ID={}) from database", p.getId().toString());
				participantsSvc.deleteParticipant(mgmtAPIUser, p);
				log.info("Removed Participant (ID={}) from database", p.getId().toString());
			} catch (PersistenceException e) {
				log.error("Error occurred removing Participant (ID={}) : {}", p.getId().toString(), 
						  Utils.getExceptionTrace(e));
				throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR);
			}
		}
	}
	
	@GetMapping("/{partID}/sml")
	@ResponseStatus(HttpStatus.OK)
	public void checkParticipantSMLRegistration(@PathVariable("partID") String partID) {
		log.debug("Request to check SML registration of Participant with ID={}", partID);
		Participant p = findParticipant(partID);
		if (p == null || !p.isRegisteredInSML()) {
			log.debug("Participant not found or not registered in SML");
			throw new ResponseStatusException(HttpStatus.NOT_FOUND);
		} else if (!Utils.isNullOrEmpty(p.getSMLMigrationCode())) {
			log.debug("Participant is being migrated");
			throw new ResponseStatusException(HttpStatus.MOVED_TEMPORARILY);
		} else
			log.trace("Participant is registered in SML", partID);
	}
	
	@PutMapping("/{partID}/sml")
	@ResponseStatus(HttpStatus.CREATED)
	public void registerParticipantInSML(@PathVariable("partID") String partID, 
										 @RequestParam(required = false) String migrationCode) {
		log.debug("Request to register Participant ({}) in SML", partID);
		Participant p = findParticipant(partID);
		if (p == null) {
			log.info("Participant with PartID ({}) not found, cannot register in SML", partID);
			throw new ResponseStatusException(HttpStatus.NOT_FOUND);
		}
		registerInSML(p, migrationCode);
	}
	
	@GetMapping(value = "/{partID}/sml/prepareMigration", produces = MediaType.TEXT_PLAIN_VALUE)
	@ResponseStatus(HttpStatus.OK)
	public String prepareMigration(@PathVariable("partID") String partID, 
								 @RequestParam(required = false) String migrationCode) {
		
		log.debug("Request to prepare migration of Participant with ID={}", partID);
		Participant p = findParticipant(partID);
		if (p == null) {
			log.info("Participant with PartID ({}) not found, cannot prepare migration", partID);
			throw new ResponseStatusException(HttpStatus.NOT_FOUND);
		}
        try {
        	p = participantsSvc.prepareForSMLMigration(mgmtAPIUser, p, migrationCode);
            log.info("Prepared Participant (ID={}) for migration (code={})", p.getId().toString(), 
            		 p.getSMLMigrationCode());
            return p.getSMLMigrationCode();
        } catch (PersistenceException e) {
	        log.error("Could prepare Participant ({}) for migration in SML : {}", p.getId().toString(), 
	        		  Utils.getExceptionTrace(e));
	        throw new ResponseStatusException(HttpStatus.FAILED_DEPENDENCY);
        }
        
	}
	
	@DeleteMapping("/{partID}/sml")
	@ResponseStatus(HttpStatus.OK)
	public void removeParticipantFromSML(@PathVariable("partID") String partID) {
		log.debug("Request to remove Participant ({}) from SML", partID);
		Participant p = findParticipant(partID);
		if (p == null) {
			log.info("Participant with PartID ({}) not found, cannot remove from SML", partID);
			throw new ResponseStatusException(HttpStatus.NOT_FOUND);
		}		
		try {
			participantsSvc.removeFromSML(mgmtAPIUser, p);
		} catch (SMLException smlRemovalFailed) {
			log.error("Error occurred removing Participant (ID={}) from SML : {}", p.getId().toString(),
					  Utils.getExceptionTrace(smlRemovalFailed));
			throw new ResponseStatusException(HttpStatus.FAILED_DEPENDENCY);
		} catch (PersistenceException pe) {
			log.error("Error occurred updating Participant (ID={}) meta-data : {}", p.getId().toString(),
					Utils.getExceptionTrace(pe));
			throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}
		
	/**
	 * Helper method to retrieve the Participant registration from the database.
	 * 
	 * @param partID	the identifier of the Participant
	 * @return	the Participant's registration in the database if it exists, otherwise <code>null</code>
	 * @throws ResponseStatusException when the given string does not represent a valid Participant ID (BAD_REQUEST) or
	 * 								   an error occurs checking the Participant registrations (INTERNAL_SERVER_ERROR) 
	 */
	private Participant findParticipant(String partID) throws ResponseStatusException {
		EmbeddedIdentifier pid;
		try {
			pid = idUtils.toEmbeddedIdentifier(idUtils.parseIDString(partID));
			return participantsSvc.getParticipant(pid);		
		} catch (NoSuchElementException unknownScheme) {
			log.warn("ID Scheme of given Participant ID ({}) not found!", partID);
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
		} catch (PersistenceException pe) {
			log.error("Unexpected error checking for Participant (PID={}) : {}", partID, Utils.getExceptionTrace(pe));
			throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR);
		}		
	}
	
	/**
	 * Helper method to register or migrate the Participant in the SML.
	 *  
	 * @param p					Participant to be registered or migrated 
	 * @param migrationCode 	migration code to use for migrating the Participant
	 * @throws ResponseStatusException when the update in the SML fails
	 */
	private void registerInSML(Participant p, String migrationCode) throws ResponseStatusException {
		try {
			if (Utils.isNullOrEmpty(migrationCode)) {
				log.trace("Register the Participant in the SML");
				participantsSvc.registerInSML(mgmtAPIUser, p);
			} else {
				log.trace("Migrate the Participant in the SML");
				participantsSvc.migrateInSML(mgmtAPIUser, p, migrationCode);
			}
			log.info("Registered new Participant in SML");
		} catch (PersistenceException smlRegFailed) {
			log.error("Error during registration of Participant (ID={}) in SML : {}", p.getId().toString(), 
						Utils.getExceptionTrace(smlRegFailed));
			throw new ResponseStatusException(HttpStatus.FAILED_DEPENDENCY);
		}				
	}
}
