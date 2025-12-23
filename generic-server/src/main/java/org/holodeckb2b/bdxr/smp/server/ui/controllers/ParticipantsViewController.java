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
import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Stream;

import org.holodeckb2b.bdxr.smp.server.datamodel.IDScheme;
import org.holodeckb2b.bdxr.smp.server.datamodel.Participant;
import org.holodeckb2b.bdxr.smp.server.datamodel.ServiceMetadataTemplate;
import org.holodeckb2b.bdxr.smp.server.db.entities.IDSchemeEntity;
import org.holodeckb2b.bdxr.smp.server.db.entities.ParticipantEntity;
import org.holodeckb2b.bdxr.smp.server.db.entities.ServiceMetadataTemplateEntity;
import org.holodeckb2b.bdxr.smp.server.services.core.IdSchemeMgmtService;
import org.holodeckb2b.bdxr.smp.server.services.core.ParticipantsService;
import org.holodeckb2b.bdxr.smp.server.services.core.PersistenceException;
import org.holodeckb2b.bdxr.smp.server.services.core.SMTMgmtService;
import org.holodeckb2b.bdxr.smp.server.services.network.DirectoryException;
import org.holodeckb2b.bdxr.smp.server.services.network.SMLException;
import org.holodeckb2b.bdxr.smp.server.ui.auth.UserAccount;
import org.holodeckb2b.bdxr.smp.server.ui.viewmodels.ParticipantFormData;
import org.holodeckb2b.bdxr.smp.server.ui.viewmodels.ParticipantSearchCriteria;
import org.holodeckb2b.bdxr.smp.server.utils.IDSchemeConvertor;
import org.holodeckb2b.bdxr.smp.server.utils.IdUtils;
import org.holodeckb2b.commons.util.Utils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
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

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;

@Controller
@Slf4j
@RequestMapping("participants")
public class ParticipantsViewController {
	private static final String P_ATTR = "participant";
	private static final String SMT_ATTR = "boundSMT";	
	private static final String CRITERIA_ATTR = "criteria";
	
	@Value("${smp.ui.maxitems_per_page:50}")
	private int maxItemsPerPage;

	@Autowired
	protected IdSchemeMgmtService	idsSvc;
	
	@Autowired
	protected SMTMgmtService 		smtSvc;
	
	@Autowired
	protected IdUtils	idUtils;
	
	@Autowired
	protected ParticipantsService	participantsSvc;

	@ModelAttribute("idSchemes")
	public Collection<? extends IDScheme> populdateSchemes() {
		try {
			return idsSvc.getIDSchemes();
		} catch (PersistenceException e) {
			return new ArrayList<>();
		} 
	}	
	
	@ModelAttribute("smlAvailable")
	public boolean provideSMLAvailability() {
		return participantsSvc.isSMLRegistrationAvailable();
	}
	
	@ModelAttribute("directoryAvailable")
	public boolean provideDirectoryAvailability() {
		return participantsSvc.isDirectoryPublicationAvailable();
	}
	
	@InitBinder
	public void registerIdSchemeConvertor(WebDataBinder binder) {
		binder.registerCustomEditor(IDSchemeEntity.class, new IDSchemeConvertor(idsSvc));
	}
	
	@GetMapping({"","/"})
    public String getOverview(@RequestParam(name = "page", required = false) Integer page, Model m, HttpSession s) throws PersistenceException {
		ParticipantSearchCriteria criteria;
		if (page == null) {			
			criteria = new ParticipantSearchCriteria();
			s.setAttribute(CRITERIA_ATTR, criteria);
		} else {
			criteria = (ParticipantSearchCriteria) s.getAttribute(CRITERIA_ATTR);
		}		
		m.addAttribute("participants", searchParticipants(criteria, page == null ? 0 : page));
		m.addAttribute(CRITERIA_ATTR, criteria);
		return "participants";
    }
	
	@PostMapping("/search")
	public String searchParticipants(@ModelAttribute(CRITERIA_ATTR) ParticipantSearchCriteria criteria, Model m, HttpSession s) throws PersistenceException {
		m.addAttribute("participants", searchParticipants(criteria, 0));
		s.setAttribute(CRITERIA_ATTR, criteria);		
		return "participants";
	}
	
	private Page<? extends Participant> searchParticipants(ParticipantSearchCriteria criteria, int page) throws PersistenceException {
		Stream<? extends Participant> result = null;
		if (criteria.getId() != null && !Utils.isNullOrEmpty(criteria.getId().getValue())) {
			Participant p = participantsSvc.getParticipant(criteria.getId());
			result = p == null ? Stream.empty() : Stream.of(p);
		} 		
		if (!Utils.isNullOrEmpty(criteria.getEntityName())) 
			result = result == null ? participantsSvc.findParticipantsByName(criteria.getEntityName()).stream() 
						: result.filter(p -> p.getName() != null && !p.getName().startsWith(criteria.getEntityName()));
				
		if (result != null) {
			// Check if found participant(s) match(es) SML and Directory search criteria
			result = result.filter(p -> (criteria.getRegisteredInSML() == null 
								|| criteria.getRegisteredInSML().equals(p.isRegisteredInSML()))
							 && (criteria.getPublishedInDirectory() == null 
							 	|| criteria.getPublishedInDirectory().equals(p.isPublishedInDirectory())));			
			return new PageImpl<Participant>((List<Participant>) result.toList());
		} 
		
		if (criteria.getRegisteredInSML() != null && criteria.getPublishedInDirectory() != null)
			return participantsSvc.findParticipantsBySMLRegistrationAndDirectoryPublication(
													criteria.getRegisteredInSML(), criteria.getPublishedInDirectory(), 
													PageRequest.of(page, maxItemsPerPage));
		else if (criteria.getRegisteredInSML() != null)
			return participantsSvc.findParticipantsBySMLRegistration(criteria.getRegisteredInSML(), 
													PageRequest.of(page, maxItemsPerPage));
		else if (criteria.getPublishedInDirectory() != null)
			return participantsSvc.findParticipantsByDirectoryPublication(criteria.getPublishedInDirectory(), 
													PageRequest.of(page, maxItemsPerPage));
		else
			return participantsSvc.getParticipants(PageRequest.of(page, maxItemsPerPage));
	}

	@GetMapping({"/add", "/edit/{id}"})
	public ModelAndView editParticipant(@PathVariable(name = "id", required = false) String id, HttpSession s) throws NoSuchElementException, PersistenceException {
		ParticipantFormData p = new ParticipantFormData(
				!Utils.isNullOrEmpty(id) ? (ParticipantEntity) participantsSvc.getParticipant(idUtils.parseIDString(id))
										 : new ParticipantEntity());
		
		Collection<ServiceMetadataTemplate> boundSMT = p.getBindings();
		s.setAttribute(SMT_ATTR, boundSMT);
		return new ModelAndView("participant_form", P_ATTR, p)
								.addObject("availableSMT", smtSvc.getTemplates().stream()
																  .filter(smt -> !boundSMT.contains(smt))
																  .toList());		
	}


	@GetMapping(value = "/delete/{id}")
	public String removeParticipant(@AuthenticationPrincipal UserAccount user, @PathVariable("id") String id) throws NoSuchElementException, PersistenceException {
		participantsSvc.deleteParticipant(user, participantsSvc.getParticipant(idUtils.parseIDString(id)));
		return "redirect:/participants";
	}
		
	@PostMapping(value = "/update", params = { "save" })
	public ModelAndView saveParticipant(@AuthenticationPrincipal UserAccount user,
								  @ModelAttribute(P_ATTR) @Valid ParticipantFormData input, BindingResult br, 
								  Model m, HttpSession s) throws PersistenceException {		
		ParticipantEntity existing = (ParticipantEntity) participantsSvc.getParticipant(input.getId()); 
		if (!br.hasErrors() && existing != null && !existing.equals(input)) {
			br.rejectValue("id.value", "ID_EXISTS", "There already exists another Participant with the same Identifier");
			br.rejectValue("id.scheme", "ID_EXISTS");
		}

		if (input.isPublishedInDirectory()) {
			if (!input.isRegisteredInSML())
				br.rejectValue("publishedInDirectory", "SML_REG_REQD",
						"To publish the business information in the directory, the participant must also be registered in the SML");
			if (Utils.isNullOrEmpty(input.getName()))
				br.rejectValue("name", "NAME_REQD", "Name is required when publishing to the directory");
			if (Utils.isNullOrEmpty(input.getRegistrationCountry()))
				br.rejectValue("registrationCountry", "COUNTRY_REQD", "Country is required when publishing to the directory");
		}
		
		Collection<ServiceMetadataTemplate> boundSMT = (Collection<ServiceMetadataTemplate>) s.getAttribute(SMT_ATTR);
		if (br.hasErrors()) {
			input.setBindings(boundSMT);
			return new ModelAndView("participant_form").addAllObjects(m.asMap())
									.addObject("availableSMT", smtSvc.getTemplates().stream()
																	.filter(smt -> !boundSMT.contains(smt)).toList());									
		}
		
		if (existing == null) {
			existing = (ParticipantEntity) participantsSvc.addParticipant(user, input);
		} else {
			input.setBindings(existing.getBindings());
			existing = (ParticipantEntity) participantsSvc.updateParticipant(user, input.asEntity());
		}
		try {
			Collection<ServiceMetadataTemplate> existingSMT = existing.getBoundSMT();
			for (ServiceMetadataTemplate smt : boundSMT) {
				if (!existingSMT.contains(smt))
					participantsSvc.bindToSMT(user, existing, smt);
			}
			for (ServiceMetadataTemplate smt : existingSMT) {
				if (!boundSMT.contains(smt))
					participantsSvc.removeSMTBinding(user, existing, smt);
			}
		} catch (PersistenceException smtUpdateFailure) {
			return editParticipant(IdUtils.toIDString(existing.getId()), s)
						.addObject("errorMessage", 
									"Could not update Service Metadata Template bindings of the Participant.");
		}
		
		try {
			if (input.isRegisteredInSML() != existing.isRegisteredInSML())
				if (input.isRegisteredInSML() && Utils.isNullOrEmpty(input.getSMLMigrationCode()))
					existing = (ParticipantEntity) participantsSvc.registerInSML(user, existing);
				else if (input.isRegisteredInSML())
					existing = (ParticipantEntity) participantsSvc.migrateInSML(user, existing, input.getSMLMigrationCode());
				else 
					existing = (ParticipantEntity) participantsSvc.removeFromSML(user, existing);
		} catch (SMLException smlUpdateFailure) {
			return editParticipant(IdUtils.toIDString(existing.getId()), s)
					.addObject("errorMessage",
						(input.isRegisteredInSML() ? "Could not register or migrate the Particpant in the SML."
												   : "Could not remove the Particpant from the SML.")
										+ "<p>SML error details: " + Utils.getRootCause(smlUpdateFailure).getMessage());
		}
		
		try {
			if (input.isRegisteredInSML() && !Utils.isNullOrEmpty(input.getSMLMigrationCode())
					&& Utils.isNullOrEmpty(existing.getSMLMigrationCode())) 
				participantsSvc.prepareForSMLMigration(user, existing, input.getSMLMigrationCode());
		} catch (SMLException migrationPrepFailed) {
			return editParticipant(IdUtils.toIDString(existing.getId()), s)
					.addObject("errorMessage", 
								"Could not prepare the Particpant for migration in the SML.<p>"
								+ "SML error details: " + Utils.getRootCause(migrationPrepFailed).getMessage());
		}
			
		try {
			if (input.isPublishedInDirectory() != existing.isPublishedInDirectory())
				existing = input.isPublishedInDirectory() ? 
												(ParticipantEntity) participantsSvc.publishInDirectory(user, existing)
											: (ParticipantEntity) participantsSvc.removeFromDirectory(user, existing);
		} catch (DirectoryException dirUpdateFailure) {
			return editParticipant(IdUtils.toIDString(existing.getId()), s)
					.addObject("errorMessage", 
							(input.isPublishedInDirectory() ? "Could not publish the Particpant to the Directory."
															: "Could not remove the Particpant from the Directory.")
							+ "<p>Directory error details: " + Utils.getRootCause(dirUpdateFailure).getMessage());
		}			
		
		return new ModelAndView("redirect:/participants"); 
	}

	@PostMapping(value = "/update", params = {"addBinding", "template2add"})
	public String addSMT(@ModelAttribute(P_ATTR) ParticipantFormData input,
						  @RequestParam("template2add") Long smt2add,
						  Model m, HttpSession s) throws PersistenceException {
		Collection<ServiceMetadataTemplate> boundSMT = (Collection<ServiceMetadataTemplate>) s.getAttribute(SMT_ATTR);
		boundSMT.add(smtSvc.getTemplate(smt2add));
		input.setBindings(boundSMT);
		m.addAttribute("availableSMT", smtSvc.getTemplates().stream().filter(smt -> !boundSMT.contains(smt)).toList());						
		return "participant_form";
	}

	@PostMapping(value = "/update", params = {"removeBinding" })
	public String removeSMT(@ModelAttribute(P_ATTR) ParticipantFormData input,
						  	@RequestParam("removeBinding") Long smt2rm,
						  	Model m, HttpSession s) throws PersistenceException {
		Collection<ServiceMetadataTemplate> boundSMT = (Collection<ServiceMetadataTemplate>) s.getAttribute(SMT_ATTR);
		boundSMT.remove(smtSvc.getTemplate(smt2rm));
		input.setBindings(boundSMT);		
		m.addAttribute("availableSMT", smtSvc.getTemplates().stream().filter(smt -> !boundSMT.contains(smt)).toList());						
		return "participant_form";
	}
	
	@PostMapping(value = "/update", params = {"cancelMigration"})
	public ModelAndView cancelMigration(@AuthenticationPrincipal UserAccount user,
								  @ModelAttribute(P_ATTR) ParticipantFormData input, HttpSession s) throws PersistenceException {
		
		Collection<ServiceMetadataTemplate> boundSMT = (Collection<ServiceMetadataTemplate>) s.getAttribute(SMT_ATTR);
		input.setBindings(boundSMT);
		ModelAndView mv = new ModelAndView("participant_form").addObject("availableSMT", 
										smtSvc.getTemplates().stream().filter(smt -> !boundSMT.contains(smt)).toList());	
		
		try {
			participantsSvc.cancelSMLMigration(user, input.asEntity());
			input.setRegisteredInSML(true);
			input.setSMLMigrationCode(null);
			input.setMigrating(false);
		} catch (SMLException cancelFailed) {
			mv.addObject("errorMessage", "Failed to cancel migration in SML<p>SML error details: " + 
										Utils.getRootCause(cancelFailed).getMessage());
		}		
		return mv;
	}
}

