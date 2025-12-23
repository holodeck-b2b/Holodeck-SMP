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

import org.holodeckb2b.bdxr.smp.server.db.entities.IDSchemeEntity;
import org.holodeckb2b.bdxr.smp.server.services.core.IdSchemeMgmtService;
import org.holodeckb2b.bdxr.smp.server.services.core.PersistenceException;
import org.holodeckb2b.commons.util.Utils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
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
	private static final String SCHEME_ATTR = "scheme";

	@Autowired
	protected IdSchemeMgmtService	idsMgmtSvc;

	@GetMapping(value = {"","/"})
    public String getOverview(Model m) throws PersistenceException {
		m.addAttribute("schemes", idsMgmtSvc.getIDSchemes(PageRequest.ofSize(100)));
	    return "idschemes";
    }

	@GetMapping(value = {"/edit", "/edit/{schemeId}" })
	public String editScheme(@PathVariable(name = "schemeId", required = false) String schemeId, Model m) throws PersistenceException {
		m.addAttribute(SCHEME_ATTR, Utils.isNullOrEmpty(schemeId) ? new IDSchemeEntity() : idsMgmtSvc.getIDScheme(schemeId));
		return "idscheme_form";
	}

	@PostMapping(value = "/update")
	public String saveScheme(@ModelAttribute(SCHEME_ATTR) @Valid IDSchemeEntity input, BindingResult br, 
							 @RequestParam("action") String action, @AuthenticationPrincipal UserDetails user) throws PersistenceException {
		if (!br.hasErrors() && "add".equals(action) && idsMgmtSvc.getIDScheme(input.getSchemeId()) != null) {
			br.rejectValue("schemeId", "ID_EXISTS", "There already exists another identifier scheme with the same identifier");
			br.rejectValue("schemeId", "ID_EXISTS");
		}

		if (br.hasErrors())
			return "idscheme_form";
		else if ("add".equals(action)) 
			idsMgmtSvc.addIDScheme(user, input);
		else 
			idsMgmtSvc.updateIDScheme(user, input);
		
		return "redirect:/settings/ids";
	}

	@GetMapping(value = "/delete/{schemeId}")
	public String removeScheme(@PathVariable("schemeId") String schemeId, @AuthenticationPrincipal UserDetails user) throws PersistenceException {
		idsMgmtSvc.deleteIDScheme(user, idsMgmtSvc.getIDScheme(schemeId));
		return "redirect:/settings/ids";
	}
}
