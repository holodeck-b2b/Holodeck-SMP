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
import org.holodeckb2b.bdxr.smp.server.db.entities.ProcessE;
import org.holodeckb2b.bdxr.smp.server.db.repos.IDSchemeRepository;
import org.holodeckb2b.bdxr.smp.server.db.repos.ProcessRepository;
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
@RequestMapping("smd/processes")
public class ProcessesViewController {
	private static final String P_ATTR = "process";

	@Autowired
	protected ProcessRepository		procesess;
	@Autowired
	protected IDSchemeRepository	idschemes;

	@ModelAttribute("idSchemes")
	public Collection<IDSchemeE> populateSchemes() {
		return idschemes.findAll();
	}

	@GetMapping(value = {"","/"})
    public String getOverview(Model m) {
		m.addAttribute("processes", procesess.findAll().parallelStream()
									.map(svc -> new Pair<ProcessE, Integer>(svc, procesess.getNumberOfTemplates(svc)))
									.toList());
	    return "admin-ui/processes";
    }

	@GetMapping(value = "/edit/{oid}")
	public String editProcess(@PathVariable("oid") Long oid, Model m) {
		m.addAttribute(P_ATTR, oid > 0 ? procesess.findById(oid).get() : new ProcessE());
		return "admin-ui/proc_form";
	}

	@PostMapping(value = "/update")
	public String saveProcess(@ModelAttribute(P_ATTR) @Valid ProcessE input, BindingResult br) {
		ProcessE existing = procesess.findByIdentifier(input.getId());
		if (!br.hasErrors() && existing != null && existing.getOid() != input.getOid()) {
			br.rejectValue("id.value", "ID_EXISTS", "There already exists another Process with the same Identifier");
			br.rejectValue("id.scheme", "ID_EXISTS");
		}

		if (br.hasErrors())
			return "admin-ui/proc_form";
		else {
			procesess.save(input);
			return "redirect:/smd/processes";
		}
	}

	@GetMapping(value = "/delete/{oid}")
	public String removeProcess(@PathVariable("oid") Long oid) {
		procesess.deleteById(oid);
		return "redirect:/smd/processes";
	}
}
