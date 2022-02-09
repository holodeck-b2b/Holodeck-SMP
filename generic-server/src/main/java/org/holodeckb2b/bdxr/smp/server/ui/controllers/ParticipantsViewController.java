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

	@ModelAttribute("idSchemes")
	public Collection<IDSchemeE> populdateSchemes() {
		return idschemes.findAll();
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
	public String deleteParticipant(@PathVariable("oid") Long oid) {
		participants.deleteById(oid);
		return "redirect:/participants";
	}

	@PostMapping(value = "/update", params = { "save" })
	public String saveParticipant(@ModelAttribute(P_ATTR) @Valid ParticipantE input, BindingResult br, Model m, HttpSession s) {
		ParticipantE updated = updateBasics(input, s);

		if (!br.hasErrors() && !participants.findByIdentifier(input.getId()).stream().allMatch(p -> p.equals(updated))) {
			br.rejectValue("id.value", "ID_EXISTS", "There already exists another Participant with the same Identifier");
			br.rejectValue("id.scheme", "ID_EXISTS");
		}

		if (!br.hasErrors()) {
			participants.save(updated);
			return "redirect:/participants";
		} else {
			return toForm(m, s);
		}
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
		}
		return stored;
	}
}

