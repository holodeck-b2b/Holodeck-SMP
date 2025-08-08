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
import org.holodeckb2b.bdxr.smp.server.services.core.IdSchemeMgmtService;
import org.holodeckb2b.bdxr.smp.server.services.core.PersistenceException;
import org.holodeckb2b.bdxr.smp.server.services.core.ProcessMgmtService;
import org.holodeckb2b.bdxr.smp.server.services.core.SMTMgmtService;
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
@RequestMapping("smd/processes")
public class ProcessesViewController {
	private static final String P_ATTR = "process";

	@Value("${smp.ui.maxitems_per_page:50}")
	private int maxItemsPerPage;
	
	@Autowired
	protected IdUtils	idUtils;
	
	@Autowired
	protected ProcessMgmtService	procSvc;
	
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
		return new ModelAndView("processes", "processes", 
			procSvc.getProcesses(PageRequest.of(page == null ? 0 : page, maxItemsPerPage))
			.map(p -> {
				try {
					return new Pair<ProcessEntity, Integer>((ProcessEntity) p, smtSvc.findTemplatesForProcess(p).size());
				} catch (PersistenceException e) {
					log.error("Could not count templates for process: {}", Utils.getExceptionTrace(e));
					return new Pair<ProcessEntity, Integer>((ProcessEntity) p, 0);
				}
			}));		
    }

	@GetMapping({"/add", "/edit/{id}"})
	public ModelAndView editProcess(@PathVariable(name = "id", required = false) String id) throws NoSuchElementException, PersistenceException {
		ProcessEntity proc = null;
		if (!Utils.isNullOrEmpty(id))
			proc = (ProcessEntity) procSvc.getProcess(idUtils.parseProcessIDString(id,
																				EmbeddedProcessIdentifier.NO_PROCESS));
		else 
			proc = new ProcessEntity();
		return new ModelAndView("proc_form", P_ATTR, proc);		
	}

	@PostMapping(value = "/update")
	public String saveProcess(@AuthenticationPrincipal UserAccount user,
			@ModelAttribute(P_ATTR) @Valid ProcessEntity input, BindingResult br) throws PersistenceException {
		ProcessEntity existing = (ProcessEntity) procSvc.getProcess(input.getId());
		if (!br.hasErrors() && existing != null && !existing.equals(input)) {
			br.rejectValue("id.value", "ID_EXISTS", "There already exists another Process with the same Identifier");
			br.rejectValue("id.scheme", "ID_EXISTS");
		}

		if (br.hasErrors())
			return "proc_form";
		else if (input.getOid() == null) 
			procSvc.addProccess(user, input);
		else
			procSvc.updateProcess(user, input);
		
		return "redirect:/smd/processes";	
	}

	@GetMapping(value = "/delete/{id}")
	public String removeProcess(@AuthenticationPrincipal UserAccount user, @PathVariable("id") String id) throws NoSuchElementException, PersistenceException {
		procSvc.deleteProcess(user, 
				 			procSvc.getProcess(idUtils.parseProcessIDString(id, EmbeddedProcessIdentifier.NO_PROCESS)));
		return "redirect:/smd/processes";
	}
}
