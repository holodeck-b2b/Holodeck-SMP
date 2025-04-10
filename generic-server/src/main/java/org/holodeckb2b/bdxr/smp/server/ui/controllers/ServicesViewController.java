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
import org.holodeckb2b.bdxr.smp.server.db.entities.ServiceE;
import org.holodeckb2b.bdxr.smp.server.db.repos.IDSchemeRepository;
import org.holodeckb2b.bdxr.smp.server.db.repos.ServiceRepository;
import org.holodeckb2b.commons.Pair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import jakarta.validation.Valid;

@Controller
@RequestMapping("smd/services")
public class ServicesViewController {
	private static final String S_ATTR = "service";

	@Autowired
	protected ServiceRepository		services;
	@Autowired
	protected IDSchemeRepository	idschemes;

	@ModelAttribute("idSchemes")
	public Collection<IDSchemeE> populdateSchemes() {
		return idschemes.findAll();
	}

	@GetMapping(value = {"","/"})
    public String getOverview(Model m) {
		m.addAttribute("services", services.findAll().parallelStream()
									.map(svc -> new Pair<ServiceE, Integer>(svc, services.getNumberOfTemplates(svc)))
									.toList());
	    return "admin-ui/services";
    }

	@GetMapping(value = "/edit/{oid}")
	public String editService(@PathVariable("oid") Long oid, Model m) {
		m.addAttribute(S_ATTR, oid > 0 ? services.findById(oid).get() : new ServiceE());
		return "admin-ui/svc_form";
	}

	@PostMapping(value = "/update")
	public String saveService(@ModelAttribute(S_ATTR) @Valid ServiceE input, BindingResult br) {
		ServiceE existing = services.findByIdentifier(input.getId());
		if (!br.hasErrors() && existing != null && existing.getOid() != input.getOid()) {
			br.rejectValue("id.value", "ID_EXISTS", "There already exists another Process with the same Identifier");
			br.rejectValue("id.scheme", "ID_EXISTS");
		}
		if (br.hasErrors())
			return "admin-ui/svc_form";
		else {
			services.save(input);
			return "redirect:/smd/services";
		}
	}

	@GetMapping(value = "/delete/{oid}")
	public String removeService(@PathVariable("oid") Long oid) {
		services.deleteById(oid);
		return "redirect:/smd/services";
	}
}
