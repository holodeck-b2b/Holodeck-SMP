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
import org.holodeckb2b.bdxr.smp.server.db.entities.ServiceMetadataBindingE;
import org.holodeckb2b.bdxr.smp.server.db.entities.ServiceMetadataTemplateE;
import org.holodeckb2b.bdxr.smp.server.db.repos.ParticipantRepository;
import org.holodeckb2b.bdxr.smp.server.db.repos.ServiceMetadataBindingRepository;
import org.holodeckb2b.bdxr.smp.server.db.repos.ServiceMetadataTemplateRepository;
import org.holodeckb2b.bdxr.smp.server.mgmtapi.xml.ServiceMetadataBindings;
import org.holodeckb2b.bdxr.smp.server.svc.IdUtils;
import org.holodeckb2b.commons.util.Utils;
import org.oasis_open.docs.bdxr.ns.smp._2.basiccomponents.ParticipantIDType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import jakarta.persistence.PersistenceException;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/participants/{partID}/bindings")
@Slf4j
public class BindingsController {

	@Autowired
	protected ParticipantRepository	participants;

	@Autowired
	protected ServiceMetadataTemplateRepository templates;
	
	@Autowired
	protected ServiceMetadataBindingRepository	bindings;
	
	@Autowired
	protected IdUtils idUtils;
	
	@GetMapping(produces = MediaType.APPLICATION_XML_VALUE )
	public ServiceMetadataBindings getBindings(@PathVariable("partID") String partID) {
		log.debug("Request to list all bindings for Participant {}", partID);
		IdentifierE pid;
		try {
			pid = idUtils.parseIDString(partID);
		} catch (NoSuchElementException unknownScheme) {
			log.warn("ID Scheme of given Participant ID ({}) not found!", partID);
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
		}		
		ParticipantE participant = participants.findByIdentifier(pid);
		if (participant == null) {
			log.warn("Unable to list bindings as no Participant with ID ({}) is found", pid.toString());
			throw new ResponseStatusException(HttpStatus.NOT_FOUND);
		}		
				
		ServiceMetadataBindings bindings = new ServiceMetadataBindings();
		ParticipantIDType pidElem = new ParticipantIDType();
		if (pid.getScheme() != null)
			pid.setValue(pid.getScheme().getSchemeId());
		pid.setValue(pid.getValue());		
		bindings.setParticipantID(pidElem);		
		
		return bindings;
	}
	
	@PutMapping("/{smtID}")
	@ResponseStatus(HttpStatus.CREATED)	
	public void addBinding(@PathVariable("partID") String partID, @PathVariable("smtID") String templateId) {
		log.debug("Request to bind SMT {} to Participant {}", templateId, partID);
		IdentifierE pid;
		try {
			pid = idUtils.parseIDString(partID);
		} catch (NoSuchElementException unknownScheme) {
			log.warn("ID Scheme of given Participant ID ({}) not found!", partID);
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
		}
		long smtOid;
		try {
			smtOid = Long.parseLong(templateId);
		} catch (NumberFormatException invalidOid) {
			log.warn("Invalid value provided for template ID : {}", templateId);
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
		}
		
		ParticipantE participant = participants.findByIdentifier(pid);
		if (participant == null) {
			log.warn("Unable to add binding as no Participant with ID ({}) is found", pid.toString());
			throw new ResponseStatusException(HttpStatus.NOT_FOUND);
		}		
		ServiceMetadataTemplateE template = templates.getReferenceById(smtOid);
		if (template == null) {
			log.warn("Unable to add binding as no template with ID ({}) is found", pid.toString());
			throw new ResponseStatusException(HttpStatus.NOT_FOUND);
		}
		
		try {
			bindings.save(new ServiceMetadataBindingE(participant, template));
			log.info("Added new binding ({},{}-{})", participant.getId().toString(), template.getOid(), template.getName());
		} catch (PersistenceException dbError) {
			log.error("Failed to save new binding ({},{}-{}) : {}", participant.getId().toString(), template.getOid(), template.getName(),
					Utils.getExceptionTrace(dbError));
			throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}
	
	@DeleteMapping("/{smtID}")
	@ResponseStatus(HttpStatus.ACCEPTED)
	public void removeBinding(@PathVariable("partID") String partID, @PathVariable("smtID") String templateId) {
		log.debug("Request to remove binding of SMT {} to Participant {}", templateId, partID);
		IdentifierE pid;
		try {
			pid = idUtils.parseIDString(partID);
		} catch (NoSuchElementException unknownScheme) {
			log.warn("ID Scheme of given Participant ID ({}) not found!", partID);
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
		}
		long smtOid;
		try {
			smtOid = Long.parseLong(templateId);
		} catch (NumberFormatException invalidOid) {
			log.warn("Invalid value provided for template ID : {}", templateId);
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
		}
		
		ParticipantE participant = participants.findByIdentifier(pid);
		if (participant == null) {
			log.warn("Unable to remove binding as no Participant with ID ({}) is found", pid.toString());
			throw new ResponseStatusException(HttpStatus.NOT_FOUND);
		}
		
		ServiceMetadataBindingE smb = bindings.findByParticipantId(pid).stream()
											  .filter(b -> b.getTemplate().getOid() == smtOid).findFirst().orElse(null);
		if (smb == null) {
			log.info("No binding found of template (OID={}) to Participant ({}), nothing to remove", smtOid, pid.toString());
			return;			
		}
		
		try {
			bindings.delete(smb);
			log.info("Removed binding ({},{}-{})", participant.getId().toString(), smb.getTemplate().getOid(), smb.getTemplate().getName());
		} catch (PersistenceException dbError) {
			log.error("Failed to save new binding ({},{}-{}) : {}", participant.getId().toString(), smb.getTemplate().getOid(), 
						smb.getTemplate().getName(), Utils.getExceptionTrace(dbError));
			throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR);
		}		
	}
}
