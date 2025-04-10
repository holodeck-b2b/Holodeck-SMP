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
package org.holodeckb2b.bdxr.smp.server.ui.controllers;

import java.util.Collection;

import org.holodeckb2b.bdxr.smp.server.db.entities.IDSchemeE;
import org.holodeckb2b.bdxr.smp.server.db.entities.ParticipantE;
import org.holodeckb2b.bdxr.smp.server.db.entities.ServiceMetadataBindingE;
import org.holodeckb2b.bdxr.smp.server.db.repos.IDSchemeRepository;
import org.holodeckb2b.bdxr.smp.server.db.repos.ParticipantRepository;
import org.holodeckb2b.bdxr.smp.server.db.repos.ServiceMetadataBindingRepository;
import org.holodeckb2b.bdxr.smp.server.db.repos.ServiceMetadataTemplateRepository;
import org.holodeckb2b.bdxr.smp.server.svc.DirectoryIntegrationService;
import org.holodeckb2b.bdxr.smp.server.svc.SMLException;
import org.holodeckb2b.bdxr.smp.server.svc.SMLIntegrationService;
import org.holodeckb2b.commons.util.Utils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;

@Controller
@Slf4j
@RequestMapping("participants")
public class ParticipantsViewController {
	private static final String P_ATTR = "participant";

	@Autowired
	protected ParticipantRepository participants;
	@Autowired
	protected IDSchemeRepository idschemes;
	@Autowired
	protected ServiceMetadataTemplateRepository templates;
	@Autowired
	protected ServiceMetadataBindingRepository bindings;
	@Autowired
	protected SMLIntegrationService	smlService;
	@Autowired
	protected DirectoryIntegrationService directoryService;
	

	@ModelAttribute("idSchemes")
	public Collection<IDSchemeE> populdateSchemes() {
		return idschemes.findAll();
	}

	@ModelAttribute("smlAvailable")
	public boolean setSMLAvailability() {
		return smlService.isSMLIntegrationAvailable();
	}

	@ModelAttribute("directoryAvailable")
	public boolean setDirectoryAvailability() {
		return directoryService.isDirectoryIntegrationAvailable();
	}

	@GetMapping(value = {"","/"})
    public String getOverview(Model m) {
		m.addAttribute("participants", participants.findAll());
        return "admin-ui/participants";
    }

	@GetMapping(value = "/edit/{oid}")
	public String editParticipant(@PathVariable("oid") Long oid, Model m, HttpSession s) {
		s.setAttribute(P_ATTR, oid > 0 ? participants.getById(oid) : new ParticipantE());
		return toForm(m, s);
	}

	@GetMapping(value = "/delete/{oid}")
	public ModelAndView deleteParticipant(@PathVariable("oid") Long oid) {
		ModelAndView mv;
		ParticipantE p = null;
		try {
			p = participants.getReferenceById(oid);
			if (smlService.isSMLIntegrationAvailable() && p.registeredInSML() 
				&& Utils.isNullOrEmpty(p.getMigrationCode())) {
				if (p.publishedInDirectory())
					directoryService.removeParticipantInfo(p);
				smlService.unregisterParticipant(p);
			}
			participants.delete(p);
			log.info("Participant (PID={}) removed", p.getId().toString());
			mv = new ModelAndView("redirect:/participants");
		} catch (EntityNotFoundException alreadyRemoved) {
			mv = new ModelAndView("redirect:/participants");
		} catch (Exception removeFailed) {
			log.error("Failed to remove Participant (PID={}) : {}", p.getId().toString(), Utils.getExceptionTrace(removeFailed));
			mv = new ModelAndView("admin-ui/participants", "participants", participants.findAll());
			mv.addObject("errorMessage", "An error occurred while removing the participant registration: " +
										 removeFailed.getMessage());
		}
		return mv;
	}

	@PostMapping(value = "/update", params = { "save" })
	public String saveParticipant(@ModelAttribute(P_ATTR) @Valid ParticipantE input, BindingResult br, Model m, HttpSession s) {		
		ParticipantE stored = participants.findByIdentifier(input.getId()); 
		if (!br.hasErrors() && stored != null && stored.getOid() != input.getOid()) {
			br.rejectValue("id.value", "ID_EXISTS", "There already exists another Participant with the same Identifier");
			br.rejectValue("id.scheme", "ID_EXISTS");
		}
		if (input.publishedInDirectory()) {
			if (input.getIsRegisteredSML() != null && !input.getIsRegisteredSML()) 
				br.rejectValue("publishedInDirectory", "SML_REG_REQD", 
								"To publish the business information in the directory, the participant must also be registered in the SML");
			if (Utils.isNullOrEmpty(input.getName()))
				br.rejectValue("name", "NAME_REQD", "Name is required when publishing to the directory");
			if (Utils.isNullOrEmpty(input.getCountry()))
				br.rejectValue("country", "COUNTRY_REQD", "Country is required when publishing to the directory");
		}

		if (br.hasErrors()) 
			return toForm(m, s);

		boolean wasSMLRegistered = stored != null && stored.registeredInSML();
		boolean wasPublished = stored != null && stored.publishedInDirectory();
		String storedMigrationCode = stored != null ? stored.getMigrationCode() : null;
		try {
			// First update the Participant data in database,			
			stored = participants.save(getUpdatedParticipant(input, s));
			
			// If SML integration is enabled, check if an update in the SML is needed
			if (smlService.isSMLIntegrationAvailable()) {
				try {
					if (wasSMLRegistered && !stored.registeredInSML()) 
						smlService.unregisterParticipant(stored);
					else if (!wasSMLRegistered && stored.registeredInSML()) {
						if (Utils.isNullOrEmpty(stored.getMigrationCode()))
							smlService.registerParticipant(stored);
						else {
							smlService.migrateParticipant(stored, stored.getMigrationCode());
							stored.setMigrationCode(null);
							stored = participants.save(stored);
						}								
					} else if (wasSMLRegistered && Utils.isNullOrEmpty(storedMigrationCode) 
							&& !Utils.isNullOrEmpty(stored.getMigrationCode())) {
						// A migration code has been set and therefore needs to be registered in the SML
						smlService.registerMigrationCode(stored);
					}
				} catch (Exception smlUpdateFailed) {
					stored.setIsRegisteredSML(wasSMLRegistered);
					stored.setMigrationCode(storedMigrationCode);
					stored = participants.save(stored);
					m.addAttribute("errorMessage", "There was an error updating the Participant's registration in the SML ("
									+ Utils.getRootCause(smlUpdateFailed).getMessage() + ")");
				}		
			}
			// If directory integration is enabled, check if an update in the directory is needed
			if (directoryService.isDirectoryIntegrationAvailable()) {
				try {
					if (stored.publishedInDirectory())
						directoryService.publishParticipantInfo(stored);
					else if (wasPublished)
						directoryService.removeParticipantInfo(stored);					
				} catch (Exception dirUpdatedFailed) {
					m.addAttribute("errorMessage", "There was an error " 
									+ (stored.publishedInDirectory() ? "publishing" : "removing") 
									+ " the Participant's info to the directory ("
									+ Utils.getRootCause(dirUpdatedFailed).getMessage() + ")");

					stored.setPublishedInDirectory(!stored.publishedInDirectory());
					stored = participants.save(stored);				
					
					return toForm(m, s);					
				}			
			}
		} catch (Exception updateFailure) {
			log.error("Failed to update the Participant (PID={}) meta-data : {}", input.getId().toString(),
						Utils.getExceptionTrace(updateFailure));
			m.addAttribute("errorMessage", "There was an error updating the Participant's info");
		}
		
		if (m.getAttribute("errorMessage") != null) {
			m.addAttribute(P_ATTR, stored);
			s.setAttribute(P_ATTR, stored);
			return toForm(m, s);
		} else		
			return "redirect:/participants";
	}

	@PostMapping(value = "/update", params = {"addBinding", "template2add"})
	public String addService(@ModelAttribute(P_ATTR) ParticipantE input, Model m, HttpSession s, @RequestParam("template2add") Long svc2add) {
		ParticipantE p = (ParticipantE) s.getAttribute(P_ATTR);
		p.getBindings().add(new ServiceMetadataBindingE(p, templates.getById(svc2add)));
		return toForm(m, s);
	}

	@PostMapping(value = "/update", params = {"removeBinding"})
	public String removeService(@ModelAttribute(P_ATTR) ParticipantE input, Model m, HttpSession s, @RequestParam("removeBinding") Long row) {
		((ParticipantE) s.getAttribute(P_ATTR)).getBindings().remove(row.intValue());
		return toForm(m, s);
	}
	
	@PostMapping(value = "/update", params = {"cancelMigration"})
	public String cancelMigration(@ModelAttribute(P_ATTR) ParticipantE input, Model m, HttpSession s) {
		ParticipantE stored = participants.findByIdentifier(input.getId());
		try {
			smlService.migrateParticipant(stored, stored.getMigrationCode());
			stored.setMigrationCode(null);
			stored = participants.save(stored);	
			input.setMigrationCode(null);
			((ParticipantE) s.getAttribute(P_ATTR)).setMigrationCode(null);
		} catch (SMLException migrateFailed) {
			m.addAttribute("errorMessage", 
					"Could not cancel migration, probably because registration has already moved to other SMP. Error: " 
					+ Utils.getRootCause(migrateFailed).getMessage());
		}
		// Because the checkbox for SML registration is disabled, its value is not set in the model and we need to set
		// it again
		input.setIsRegisteredSML(true);
		return toForm(m, s);
	}

	private String toForm(Model m, HttpSession s) {
		ParticipantE mp = (ParticipantE) m.getAttribute(P_ATTR);
		ParticipantE sp = (ParticipantE) s.getAttribute(P_ATTR);
		if (mp != null)
			mp.setBindings(sp.getBindings());
		else
			m.addAttribute(P_ATTR, sp);

		m.addAttribute("isMigrating", !Utils.isNullOrEmpty(sp.getMigrationCode()));
		m.addAttribute("availableSMT",  templates.findAll().stream().filter(smt -> sp.getBindings().parallelStream()
												.noneMatch(b -> b.getTemplate().equals(smt)))
												.toList());
 		return "admin-ui/participant_form";
	}

	/**
	 * Gets the updated data on the Particiant by combining the basic fields from the Model and the bound SMT from the
	 * session object.
	 * 
	 * @param input	the meta-data from the model
	 * @param s		the meta-data from the session
	 * @return	the combined data
	 */
	private ParticipantE getUpdatedParticipant(ParticipantE input, HttpSession s) {
		ParticipantE updated = (ParticipantE) s.getAttribute(P_ATTR);
		if (input != null) {
			updated.setId(input.getId());
			updated.setName(input.getName());
			updated.setCountry(input.getCountry());
			updated.setAddressInfo(input.getAddressInfo());
			updated.setContactInfo(input.getContactInfo());
			updated.setPublishedInDirectory(input.getPublishedInDirectory());
			updated.setFirstRegistration(input.getFirstRegistration());
			updated.setAdditionalIds(input.getAdditionalIds());
			if (Utils.isNullOrEmpty(updated.getMigrationCode())) {
				updated.setIsRegisteredSML(input.getIsRegisteredSML());
				updated.setMigrationCode(input.getMigrationCode());
			}
		}
		return updated;
	}
}

