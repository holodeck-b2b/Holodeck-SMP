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

import javax.persistence.EntityNotFoundException;
import javax.servlet.http.HttpSession;
import javax.validation.Valid;
import org.holodeckb2b.bdxr.smp.server.db.entities.IDSchemeE;
import org.holodeckb2b.bdxr.smp.server.db.entities.ParticipantE;
import org.holodeckb2b.bdxr.smp.server.db.entities.ServiceMetadataBindingE;
import org.holodeckb2b.bdxr.smp.server.db.repos.IDSchemeRepository;
import org.holodeckb2b.bdxr.smp.server.db.repos.ParticipantRepository;
import org.holodeckb2b.bdxr.smp.server.db.repos.ServiceMetadataBindingRepository;
import org.holodeckb2b.bdxr.smp.server.db.repos.ServiceMetadataTemplateRepository;
import org.holodeckb2b.bdxr.smp.server.svc.DirectoryIntegrationService;
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

import lombok.extern.log4j.Log4j2;

@Controller
@Log4j2
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

	@GetMapping()
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
			if (smlService.isSMLIntegrationAvailable() && p.isRegisteredSML()) {
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
		
		ParticipantE prev = (ParticipantE) s.getAttribute(P_ATTR);
		ParticipantE current = participants.findByIdentifier(input.getId()); 
		if (!br.hasErrors() && current != null && current.getOid() != prev.getOid()) {
			br.rejectValue("id.value", "ID_EXISTS", "There already exists another Participant with the same Identifier");
			br.rejectValue("id.scheme", "ID_EXISTS");
		}
		if (input.publishedInDirectory()) {
			if (!input.isRegisteredSML()) {
				br.rejectValue("publishedInDirectory", "SML_REG_REQD", 
								"To publish the business information in the directory, the participant must also be registered in the SML");
				input.setIsRegisteredSML(prev.isRegisteredSML());
			}
			if (Utils.isNullOrEmpty(input.getName()))
				br.rejectValue("name", "NAME_REQD", "Name is required when publishing to the directory");
			if (Utils.isNullOrEmpty(input.getCountry()))
					br.rejectValue("country", "COUNTRY_REQD", "Country is required when publishing to the directory");
		}

		if (br.hasErrors()) 
			return toForm(m, s);

		boolean smlUpdRequired = smlService.isSMLIntegrationAvailable() &&
								( (current != null && current.isRegisteredSML() != input.isRegisteredSML())
								|| (current == null && input.isRegisteredSML()) );
		ParticipantE updated = updateSessionBasics(input, s); 
		try {
			// Save data
			updated = participants.save(updated);
			
			// Update SML registration if needed
			if (smlUpdRequired)
				try {
					if (updated.isRegisteredSML())
						smlService.registerParticipant(updated);
					else
						smlService.unregisterParticipant(updated);
				} catch (Exception smlUpdateFailed) {
					updated.setIsRegisteredSML(!updated.isRegisteredSML());
					updated = participants.save(updated);
					m.addAttribute("errorMessage", "There was an error updating the Participant's registration in the SML ("
									+ smlUpdateFailed.getMessage() + ")");
				}		
			if (updated.publishedInDirectory())
				try {
					directoryService.publishParticipantInfo(updated);
				} catch (Exception dirUpdatedFailed) {
					m.addAttribute("errorMessage", "There was an error publishing the Participant's info to the directory ("
									+ dirUpdatedFailed.getMessage() + ")");
				}
			else if (current.publishedInDirectory()) 
				try {
					directoryService.removeParticipantInfo(updated);
				} catch (Exception dirUpdatedFailed) {
					updated.setPublishedInDirectory(true);
					updated = participants.save(updated);				
					m.addAttribute("errorMessage", "There was an error removing the Participant's info from the directory ("
							+ dirUpdatedFailed.getMessage() + ")");
					return toForm(m, s);					
				}			
		} catch (Exception updateFailure) {
			log.error("Failed to update the Participant (PID={}) meta-data : {}", prev.getOid(), Utils.getExceptionTrace(updateFailure));
			m.addAttribute("errorMessage", "There was an error updating the Participant's info");
		}
		
		if (m.getAttribute("errorMessage") != null) {
			m.addAttribute(P_ATTR, updated);
			s.setAttribute(P_ATTR, updated);
			return toForm(m, s);
		} else		
			return "redirect:/participants";
	}

	@PostMapping(value = "/update", params = {"addBinding", "template2add"})
	public String addService(@ModelAttribute(P_ATTR) ParticipantE input, Model m, HttpSession s, @RequestParam("template2add") Long svc2add) {
		ParticipantE p = updateSessionBasics(input, s);
		p.getBindings().add(new ServiceMetadataBindingE(p, templates.getById(svc2add)));
		return toForm(m, s);
	}

	@PostMapping(value = "/update", params = {"removeBinding"})
	public String removeService(@ModelAttribute(P_ATTR) ParticipantE input, Model m, HttpSession s, @RequestParam("removeBinding") Long row) {
		updateSessionBasics(input, s).getBindings().remove(row.intValue());
		return toForm(m, s);
	}

	private String toForm(Model m, HttpSession s) {
		ParticipantE mp = (ParticipantE) m.getAttribute(P_ATTR);
		ParticipantE sp = (ParticipantE) s.getAttribute(P_ATTR);
		if (mp != null)
			mp.setBindings(sp.getBindings());
		else
			m.addAttribute(P_ATTR, sp);

		m.addAttribute("availableSMT",  templates.findAll().stream().filter(smt -> sp.getBindings().parallelStream()
												.noneMatch(b -> b.getTemplate().equals(smt)))
												.toList());
 		return "admin-ui/participant_form";
	}

	private ParticipantE updateSessionBasics(ParticipantE input, HttpSession s) {
		ParticipantE stored = (ParticipantE) s.getAttribute(P_ATTR);
		if (input != null) {
			stored.setId(input.getId());
			stored.setName(input.getName());
			stored.setCountry(input.getCountry());
			stored.setAddressInfo(input.getAddressInfo());
			stored.setContactInfo(input.getContactInfo());
			stored.setIsRegisteredSML(input.getIsRegisteredSML());
			stored.setPublishedInDirectory(input.getPublishedInDirectory());
		}
		return stored;
	}
}

