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

import java.util.NoSuchElementException;

import org.holodeckb2b.bdxr.smp.server.db.entities.TransportProfileEntity;
import org.holodeckb2b.bdxr.smp.server.services.core.EndpointMgmtService;
import org.holodeckb2b.bdxr.smp.server.services.core.PersistenceException;
import org.holodeckb2b.bdxr.smp.server.services.core.TransportProfileMgmtService;
import org.holodeckb2b.bdxr.smp.server.utils.IdUtils;
import org.holodeckb2b.commons.Pair;
import org.holodeckb2b.commons.util.Utils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
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
import org.springframework.web.servlet.ModelAndView;

import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;

@Controller
@Slf4j
@RequestMapping("settings/transports")
public class TransportsViewController {
	private static final String PROFILE_ATTR = "profile";

	@Value("${smp.ui.maxitems_per_page:50}")
	private int maxItemsPerPage;
	
	@Autowired
	protected TransportProfileMgmtService	tpMgmtSvc;

	@Autowired
	protected EndpointMgmtService	epMgmtSvc;
	
	@Autowired
	protected IdUtils idUtils;


	@GetMapping(value = {"","/"})
    public ModelAndView getOverview(@RequestParam(name = "page", required = false) Integer page) throws PersistenceException {
		return new ModelAndView("transports", "profiles", 
				tpMgmtSvc.getTransportProfiles(PageRequest.of(page == null ? 0 : page, maxItemsPerPage))
				.map(p -> {
					try {
						return new Pair<TransportProfileEntity, Integer>((TransportProfileEntity) p, epMgmtSvc.findEndpointsUsingProfile(p).size());
					} catch (PersistenceException e) {
						log.error("Could not count endpoint for profile: {}", Utils.getExceptionTrace(e));
						return new Pair<TransportProfileEntity, Integer>((TransportProfileEntity) p, 0);
					}
				}));		
    }

	@GetMapping(value = {"/edit", "/edit/{id}" })
	public String editProfile(@PathVariable(name = "id", required = false) String id, Model m) throws NoSuchElementException, PersistenceException {
		m.addAttribute(PROFILE_ATTR, !Utils.isNullOrEmpty(id) ? tpMgmtSvc.getTransportProfile(idUtils.parseIDString(id))
															: new TransportProfileEntity());
		return "transport_form";
	}

	@PostMapping(value = "/update")
	public String saveProfile(@ModelAttribute(PROFILE_ATTR) @Valid TransportProfileEntity input, BindingResult br, 
								@RequestParam("action") String action, @AuthenticationPrincipal UserDetails user) throws PersistenceException {
		if (!br.hasErrors() && "add".equals(action) && tpMgmtSvc.getTransportProfile(input.getId()) != null)
			br.rejectValue("id.value", "ID_EXISTS", "There already exists another Transport Profile with the same identifier");

		if (br.hasErrors())
			return "transport_form";
		else if ("add".equals(action)) 
			tpMgmtSvc.addTransportProfile(user, input);
		else 
			tpMgmtSvc.updateTransportProfile(user, input);
			
		return "redirect:/settings/transports";
	
	}

	@GetMapping(value = "/delete/{id}")
	public String removeProfile(@PathVariable("id") String id, @AuthenticationPrincipal UserDetails user) throws PersistenceException {
		tpMgmtSvc.deleteTransportProfile(user, tpMgmtSvc.getTransportProfile(idUtils.parseIDString(id)));
		return "redirect:/settings/transports";
	}
}
