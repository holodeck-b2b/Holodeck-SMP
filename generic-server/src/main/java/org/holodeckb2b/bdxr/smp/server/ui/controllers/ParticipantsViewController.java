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
import javax.servlet.http.HttpSession;
import javax.validation.Valid;
import org.holodeckb2b.bdxr.smp.server.db.entities.IDSchemeE;
import org.holodeckb2b.bdxr.smp.server.db.entities.ParticipantE;
import org.holodeckb2b.bdxr.smp.server.db.entities.ServiceMetadataBindingE;
import org.holodeckb2b.bdxr.smp.server.db.repos.IDSchemeRepository;
import org.holodeckb2b.bdxr.smp.server.db.repos.ParticipantRepository;
import org.holodeckb2b.bdxr.smp.server.db.repos.ServiceMetadataBindingRepository;
import org.holodeckb2b.bdxr.smp.server.db.repos.ServiceMetadataTemplateRepository;
import org.holodeckb2b.bdxr.smp.server.svc.SMLIntegrationService;
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

@Controller
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


	@ModelAttribute("idSchemes")
	public Collection<IDSchemeE> populdateSchemes() {
		return idschemes.findAll();
	}

	@ModelAttribute("smlAvailable")
	public boolean setSMLAvailability() {
		return smlService.isSMLIntegrationAvailable();
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
		try {
			ParticipantE p = participants.getReferenceById(oid);
			if (smlService.isSMLIntegrationAvailable() && p.isRegisteredSML())
				smlService.unregisterParticipant(p);
			participants.delete(p);
			mv = new ModelAndView("redirect:/participants");
		} catch (Exception removeFailed) {
			mv = new ModelAndView("admin-ui/participants", "participants", participants.findAll());
			mv.addObject("errorMessage", "An error occurred while removing the participant registration: " +
										 removeFailed.getMessage());
		}
		return mv;
	}

	@PostMapping(value = "/update", params = { "save" })
	public String saveParticipant(@ModelAttribute(P_ATTR) @Valid ParticipantE input, BindingResult br, Model m, HttpSession s) {
		ParticipantE update = updateBasics(input, s);

		if (!br.hasErrors() && !participants.findByIdentifier(input.getId()).stream().allMatch(p -> p.equals(update))) {
			br.rejectValue("id.value", "ID_EXISTS", "There already exists another Participant with the same Identifier");
			br.rejectValue("id.scheme", "ID_EXISTS");
		}

		ParticipantE saved;
		boolean smlUpdRequired = false;
		if (!br.hasErrors()) {
			if (smlService.isSMLIntegrationAvailable() && update.getOid() != 0) {
				// Update to an existing registration, check if the SML registration changed
				ParticipantE current = participants.getReferenceById(update.getOid());
				smlUpdRequired = current.isRegisteredSML() != update.isRegisteredSML();
			} else
				smlUpdRequired = smlService.isSMLIntegrationAvailable() && update.isRegisteredSML();

			// Save data
			saved = participants.save(update);
			// Update SML registration if needed
			if (smlUpdRequired)
				try {
					if (update.isRegisteredSML())
						smlService.registerParticipant(update);
					else
						smlService.unregisterParticipant(update);
				} catch (Exception smlUpdateFailed) {
					saved.setIsRegisteredSML(!update.isRegisteredSML());
					saved = participants.save(saved);
					s.setAttribute(P_ATTR, saved);
					m.addAttribute(P_ATTR, saved);
					m.addAttribute("errorMessage", "There was an error updating the Participant's registration in the SML ("
									+ smlUpdateFailed.getMessage() + ")");
					return toForm(m, s);
				}
		}

		if (!br.hasErrors())
			return "redirect:/participants";
		else
			return toForm(m, s);
	}

	@PostMapping(value = "/update", params = {"addBinding", "template2add"})
	public String addService(@ModelAttribute(P_ATTR) ParticipantE input, Model m, HttpSession s, @RequestParam("template2add") Long svc2add) {
		ParticipantE p = updateBasics(input, s);
		p.getBindings().add(new ServiceMetadataBindingE(p, templates.getById(svc2add)));
		return toForm(m, s);
	}

	@PostMapping(value = "/update", params = {"removeBinding"})
	public String removeService(@ModelAttribute(P_ATTR) ParticipantE input, Model m, HttpSession s, @RequestParam("removeBinding") Long row) {
		updateBasics(input, s).getBindings().remove(row.intValue());
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

	private ParticipantE updateBasics(ParticipantE input, HttpSession s) {
		ParticipantE stored = (ParticipantE) s.getAttribute(P_ATTR);
		if (input != null) {
			stored.setId(input.getId());
			stored.setName(input.getName());
			stored.setContactInfo(input.getContactInfo());
			stored.setIsRegisteredSML(input.getIsRegisteredSML());
		}
		return stored;
	}
}

