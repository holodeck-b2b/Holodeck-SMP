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

import java.util.ArrayList;
import java.util.Collection;
import java.util.NoSuchElementException;

import org.holodeckb2b.bdxr.smp.datamodel.IDScheme;
import org.holodeckb2b.bdxr.smp.server.db.entities.EmbeddedProcessIdentifier;
import org.holodeckb2b.bdxr.smp.server.db.entities.IDSchemeEntity;
import org.holodeckb2b.bdxr.smp.server.db.entities.ProcessEntity;
import org.holodeckb2b.bdxr.smp.server.db.entities.ServiceEntity;
import org.holodeckb2b.bdxr.smp.server.services.core.IdSchemeMgmtService;
import org.holodeckb2b.bdxr.smp.server.services.core.PersistenceException;
import org.holodeckb2b.bdxr.smp.server.services.core.ProcessMgmtService;
import org.holodeckb2b.bdxr.smp.server.services.core.SMTMgmtService;
import org.holodeckb2b.bdxr.smp.server.services.core.ServiceMgmtService;
import org.holodeckb2b.bdxr.smp.server.ui.auth.UserAccount;
import org.holodeckb2b.bdxr.smp.server.utils.IDSchemeConvertor;
import org.holodeckb2b.bdxr.smp.server.utils.IdUtils;
import org.holodeckb2b.commons.Pair;
import org.holodeckb2b.commons.util.Utils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.InitBinder;
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
@RequestMapping("smd/services")
public class ServicesViewController {
	private static final String S_ATTR = "service";

	@Value("${smp.ui.maxitems_per_page:50}")
	private int maxItemsPerPage;
	
	@Autowired
	protected IdUtils	idUtils;
	
	@Autowired
	protected ServiceMgmtService	servicesSvc;
	
	@Autowired
	protected IdSchemeMgmtService 	idsSvc;
	
	@Autowired
	protected SMTMgmtService smtSvc;

	@InitBinder
	public void registerIdSchemeConvertor(WebDataBinder binder) {
		binder.registerCustomEditor(IDSchemeEntity.class, new IDSchemeConvertor(idsSvc));
	}
	
	@ModelAttribute("idSchemes")
	public Collection<? extends IDScheme> provideIdSchemes() { 
		try {
			return idsSvc.getIDSchemes();
		} catch (PersistenceException e) {
			return new ArrayList<>();
		} 
	}
	
	@GetMapping({"","/"})
    public ModelAndView getOverview(@RequestParam(name = "page", required = false) Integer page) throws PersistenceException {
		return new ModelAndView("services", "services", 
			servicesSvc.getServices(PageRequest.of(page == null ? 0 : page, maxItemsPerPage))
			.map(s -> {
				try {
					return new Pair<ServiceEntity, Integer>((ServiceEntity) s, smtSvc.findTemplatesForService(s).size());
				} catch (PersistenceException e) {
					log.error("Could not count templates for service: {}", Utils.getExceptionTrace(e));
					return new Pair<ServiceEntity, Integer>((ServiceEntity) s, 0);
				}
			}));		
    }	

	@GetMapping({"/add", "/edit/{id}"})
	public ModelAndView editService(@PathVariable(name = "id", required = false) String id) throws NoSuchElementException, PersistenceException {
		ServiceEntity svc = null;
		if (!Utils.isNullOrEmpty(id))
			svc = (ServiceEntity) servicesSvc.getService(idUtils.parseIDString(id));
		else 
			svc = new ServiceEntity();
		return new ModelAndView("svc_form", S_ATTR, svc);		
	}
	
	@PostMapping(value = "/update")
	public String saveService(@AuthenticationPrincipal UserAccount user,
			@ModelAttribute(S_ATTR) @Valid ServiceEntity input, BindingResult br) throws PersistenceException {
		ServiceEntity existing = (ServiceEntity) servicesSvc.getService(input.getId());
		if (!br.hasErrors() && existing != null && !existing.equals(input)) {
			br.rejectValue("id.value", "ID_EXISTS", "There already exists another Service with the same Identifier");
			br.rejectValue("id.scheme", "ID_EXISTS");
		}

		if (br.hasErrors())
			return "svc_form";
		else if (input.getOid() == null) 
			servicesSvc.addService(user, input);
		else
			servicesSvc.updateService(user, input);
		
		return "redirect:/smd/services";		
	}

	@GetMapping(value = "/delete/{id}")
	public String removeService(@AuthenticationPrincipal UserAccount user, @PathVariable("id") String id) throws NoSuchElementException, PersistenceException {
		servicesSvc.deleteService(user, servicesSvc.getService(idUtils.parseIDString(id)));
		return "redirect:/smd/services";
	}
}
