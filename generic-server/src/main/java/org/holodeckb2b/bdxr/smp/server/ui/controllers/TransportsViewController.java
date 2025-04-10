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

import org.holodeckb2b.bdxr.smp.server.db.entities.TransportProfileE;
import org.holodeckb2b.bdxr.smp.server.db.repos.TransportProfileRepository;
import org.holodeckb2b.commons.Pair;
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

import jakarta.validation.Valid;

@Controller
@RequestMapping("settings/transports")
public class TransportsViewController {
	private static final String P_ATTR = "profile";

	@Autowired
	protected TransportProfileRepository		profiles;


	@GetMapping(value = {"","/"})
    public String getOverview(Model m) {
		m.addAttribute("profiles", profiles.findAll().parallelStream()
									.map(p -> new Pair<TransportProfileE, Integer>(p, profiles.getNumberOfEndpoints(p)))
									.toList());
	    return "admin-ui/transports";
    }

	@GetMapping(value = {"/edit", "/edit/{id}" })
	public String editProfile(@PathVariable(name = "id", required = false) String id, Model m) {
		m.addAttribute(P_ATTR, !Utils.isNullOrEmpty(id) ? profiles.findById(id).get() : new TransportProfileE());
		return "admin-ui/transport_form";
	}

	@PostMapping(value = "/update")
	public String saveProfile(@ModelAttribute(P_ATTR) @Valid TransportProfileE input, BindingResult br, @RequestParam("action") String action) {
		if (!br.hasErrors() && "add".equals(action) && !profiles.findById(input.getId()).isEmpty())
			br.rejectValue("id", "ID_EXISTS", "There already exists another Transport Profile with the same identifier");

		if (br.hasErrors())
			return "admin-ui/transport_form";
		else {
			profiles.save(input);
			return "redirect:/settings/transports";
		}
	}

	@GetMapping(value = "/delete/{id}")
	public String removeProfile(@PathVariable("id") String id) {
		profiles.deleteById(id);
		return "redirect:/settings/transports";
	}
}
