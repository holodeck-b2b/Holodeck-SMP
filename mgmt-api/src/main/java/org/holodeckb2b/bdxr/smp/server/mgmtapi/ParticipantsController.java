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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Random;

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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
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

	@Value("${sml.autoregistration:true}")
	protected boolean	autoRegisterInSML;
	
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
	public void addParticipant(@PathVariable("partID") String partID, 
							   @RequestParam(required = false) String migrationCode) {		
		log.debug("Request to {} Participant with ID={}", Utils.isNullOrEmpty(migrationCode) ? "add" : "migrate", partID);
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
			p = participants.save(p);
			log.info("Added new Participant with ID {} to database", pid.toString());
		} catch (Exception e) {
			log.error("Could not save new Participant (PID={}) to database : {}", pid.toString(), Utils.getExceptionTrace(e));
			throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR);
		}
		if (autoRegisterInSML && smlService.isSMLIntegrationAvailable())
			registerInSML(p, migrationCode);			
	}	
	
	@DeleteMapping("/{partID}")
	@ResponseStatus(HttpStatus.ACCEPTED)
	public void removeParticipant(@PathVariable("partID") String partID) {
		log.debug("Request to remove Participant with ID={}", partID);
		ParticipantE p = findParticipant(partID);
		if (p == null) {
			log.info("Participant with PartID ({}) not found, nothing to remove", partID);
			return;
		}
		
		// If the Participant is migrated to another SP, no action towards SML and/or directory is needed
		if (p.registeredInSML() && Utils.isNullOrEmpty(p.getMigrationCode())) 
			removeFromSML(p);
		
		log.trace("Removing Participant (ID={}) from database", p.getId().toString());
		participants.delete(p);
		log.info("Removed Participant (ID={}) from database", p.getId().toString());
	}
	
	@PutMapping("/{partID}/sml")
	@ResponseStatus(HttpStatus.CREATED)
	public void registerParticipantInSML(@PathVariable("partID") String partID, 
										 @RequestParam(required = false) String migrationCode) {
		log.debug("Request to register Participant ({}) in SML", partID);
		ParticipantE p = findParticipant(partID);
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
		ParticipantE p = findParticipant(partID);
		if (p == null) {
			log.info("Participant with PartID ({}) not found, cannot prepare migration", partID);
			throw new ResponseStatusException(HttpStatus.NOT_FOUND);
		}
		
		log.trace("Generate a migration code");
	    String upperCase = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
	    String lowerCase = "abcdefghijklmnopqrstuvwxyz";
	    String numbers = "0123456789";
	    String specialChars = "@#$%()[]{}*^-!~|+=";
	    String allChars = upperCase + lowerCase + numbers + specialChars;
	    
	    List<Character> buf = new ArrayList<>();	    
	    Random random = new Random();	    
        // Add required minimum characters
        for (int i = 0; i < 2; i++) {
            buf.add(upperCase.charAt(random.nextInt(upperCase.length())));
            buf.add(lowerCase.charAt(random.nextInt(lowerCase.length())));
            buf.add(numbers.charAt(random.nextInt(numbers.length())));
            buf.add(specialChars.charAt(random.nextInt(specialChars.length())));
        }
        // Fill the rest of the string
        while (buf.size() < 24) {
            buf.add(allChars.charAt(random.nextInt(allChars.length())));
        }
        // Shuffle to ensure randomness
        Collections.shuffle(buf, random);

        StringBuilder codeBldr = new StringBuilder();
        buf.forEach(c -> codeBldr.append(c));
        String migCode = codeBldr.toString();

        log.trace("Register migrationcode for Participant in SML");
        try {
        	p.setMigrationCode(migCode);        
        	smlService.registerMigrationCode(p);
            participants.save(p);
            log.info("Saved migration code ({}) for Participant (ID={})", migCode, p.getId().toString());
        } catch (SMLException e) {
	        log.error("Could not register migration code for Participant ({}) in SML : {}", p.getId().toString(), 
	        		  Utils.getExceptionTrace(e));
	        throw new ResponseStatusException(HttpStatus.FAILED_DEPENDENCY);
        }
        
        return migCode;
	}
	
	@DeleteMapping("/{partID}/sml")
	@ResponseStatus(HttpStatus.OK)
	public void removeParticipantFromSML(@PathVariable("partID") String partID) {
		log.debug("Request to remove Participant ({}) from SML", partID);
		ParticipantE p = findParticipant(partID);
		if (p == null) {
			log.info("Participant with PartID ({}) not found, cannot remove from SML", partID);
			throw new ResponseStatusException(HttpStatus.NOT_FOUND);
		}
		
		if (!Utils.isNullOrEmpty(p.getMigrationCode())) {
			log.warn("Cannot remove Participant ({}) as it's being migrated", partID);
			throw new ResponseStatusException(HttpStatus.PRECONDITION_FAILED);
		}		
		removeFromSML(p);
	}
		
	/**
	 * Helper method to retrieve the Participant registration from the database.
	 * 
	 * @param partID	identifier of the Participant
	 * @return	the Participant's registration in the database if it exists, otherwise <code>null</code>
	 * @throws ResponseStatusException when the given string does not represent a valid Participant ID
	 */
	private ParticipantE findParticipant(String partID) throws ResponseStatusException {
		IdentifierE pid;
		try {
			pid = idUtils.parseIDString(partID);
		} catch (NoSuchElementException unknownScheme) {
			log.warn("ID Scheme of given Participant ID ({}) not found!", partID);
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
		}
		
		return participants.findByIdentifier(pid);		
	}
	
	/**
	 * Helper method to register or migrate the Participant in the SML.
	 *  
	 * @param p					Participant to be registered or migrated 
	 * @param migrationCode 	migration code to use for migrating the Participant
	 * @throws ResponseStatusException when the update in the SML fails
	 */
	private void registerInSML(ParticipantE p, String migrationCode) throws ResponseStatusException {
		try {
			if (Utils.isNullOrEmpty(migrationCode)) {
				log.trace("Register the Participant in the SML");
				smlService.registerParticipant(p);
			} else {
				log.trace("Migrate the Participant in the SML");
				smlService.migrateParticipant(p, migrationCode);
			}
			log.info("Registered new Participant in SML");
			p.setIsRegisteredSML(true);
			participants.save(p);
		} catch (SMLException smlRegFailed) {
			log.error("Error during registration of Participant (ID={}) in SML : {}", p.getId().toString(), 
						Utils.getExceptionTrace(smlRegFailed));
			throw new ResponseStatusException(HttpStatus.FAILED_DEPENDENCY);
		}				
	}

	/**
	 * Helper method to remove the Participant from the SML and if published in the directory from the directory.
	 *  
	 * @param p					Participant to be removed 
	 * @throws ResponseStatusException when an error occurs removing the Participant from either the SML or directory
	 */
	private void removeFromSML(ParticipantE p) throws ResponseStatusException {
		if (p.publishedInDirectory()) {
			try {
				log.trace("Remove Participant from the directory");
				directoryService.removeParticipantInfo(p);
			} catch (DirectoryException dirRemoveFailed) {
				log.error("An error occurred removing the Participant (ID={}) from the directory", p.getId().toString());
				throw new ResponseStatusException(HttpStatus.FAILED_DEPENDENCY);
			}
		}
		try {
			log.trace("Remove Participant from the SML");
			smlService.unregisterParticipant(p);
			p.setIsRegisteredSML(false);
			p.setPublishedInDirectory(false);
			participants.save(p);
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
}
