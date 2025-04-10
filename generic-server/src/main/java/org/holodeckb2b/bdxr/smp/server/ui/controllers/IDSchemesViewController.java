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

import org.holodeckb2b.bdxr.smp.server.db.entities.IDSchemeE;
import org.holodeckb2b.bdxr.smp.server.db.repos.IDSchemeRepository;
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
@RequestMapping("settings/ids")
public class IDSchemesViewController {
	private static final String S_ATTR = "scheme";

	@Autowired
	protected IDSchemeRepository		idschemes;


	@GetMapping(value = {"","/"})
    public String getOverview(Model m) {
		m.addAttribute("schemes", idschemes.findAll());
	    return "admin-ui/idschemes";
    }

	@GetMapping(value = {"/edit", "/edit/{schemeId}" })
	public String editScheme(@PathVariable(name = "schemeId", required = false) String schemeId, Model m) {
		m.addAttribute(S_ATTR, !Utils.isNullOrEmpty(schemeId) ? idschemes.findById(schemeId).get() : new IDSchemeE());
		return "admin-ui/idscheme_form";
	}

	@PostMapping(value = "/update")
	public String saveScheme(@ModelAttribute(S_ATTR) @Valid IDSchemeE input, BindingResult br, @RequestParam("action") String action) {
		if (!br.hasErrors() && "add".equals(action) && !idschemes.findById(input.getSchemeId()).isEmpty()) {
			br.rejectValue("schemeId", "ID_EXISTS", "There already exists another identifier scheme with the same identifier");
			br.rejectValue("schemeId", "ID_EXISTS");
		}

		if (br.hasErrors())
			return "admin-ui/idscheme_form";
		else {
			idschemes.save(input);
			return "redirect:/settings/ids";
		}
	}

	@GetMapping(value = "/delete/{schemeId}")
	public String removeScheme(@PathVariable("schemeId") String schemeId) {
		idschemes.deleteById(schemeId);
		return "redirect:/settings/ids";
	}
}
