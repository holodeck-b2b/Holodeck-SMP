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

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.function.Consumer;

import org.holodeckb2b.bdxr.common.datamodel.ProcessIdentifier;
import org.holodeckb2b.bdxr.smp.server.SMPServerApplication;
import org.holodeckb2b.bdxr.smp.server.datamodel.Process;
import org.holodeckb2b.bdxr.smp.server.datamodel.Service;
import org.holodeckb2b.bdxr.smp.server.db.entities.EmbeddedIdentifier;
import org.holodeckb2b.bdxr.smp.server.db.entities.EmbeddedProcessIdentifier;
import org.holodeckb2b.bdxr.smp.server.db.entities.EmbeddedRedirection;
import org.holodeckb2b.bdxr.smp.server.db.entities.ProcessGroupEntity;
import org.holodeckb2b.bdxr.smp.server.db.entities.ProcessInfoEntity;
import org.holodeckb2b.bdxr.smp.server.db.entities.ServiceMetadataTemplateEntity;
import org.holodeckb2b.bdxr.smp.server.services.ParticipantsServiceImpl;
import org.holodeckb2b.bdxr.smp.server.services.core.EndpointMgmtService;
import org.holodeckb2b.bdxr.smp.server.services.core.PersistenceException;
import org.holodeckb2b.bdxr.smp.server.services.core.ProcessMgmtService;
import org.holodeckb2b.bdxr.smp.server.services.core.SMTMgmtService;
import org.holodeckb2b.bdxr.smp.server.services.core.ServiceMgmtService;
import org.holodeckb2b.bdxr.smp.server.ui.auth.UserAccount;
import org.holodeckb2b.bdxr.smp.server.ui.viewmodels.PgEndpointFormData;
import org.holodeckb2b.bdxr.smp.server.ui.viewmodels.PgProcInfoFormData;
import org.holodeckb2b.bdxr.smp.server.ui.viewmodels.PgRedirectionFormData;
import org.holodeckb2b.bdxr.smp.server.utils.IdUtils;
import org.holodeckb2b.commons.Pair;
import org.holodeckb2b.commons.security.CertificateUtils;
import org.holodeckb2b.commons.util.Utils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;

/*
 * Adding/editing the Process Groups and Endpoints of a Service Metadata Template is done using popup dialogs. To keep 
 * the, possibly edited, SMT data available for when a dialog is closed, it's both kept in the model and stored in the 
 * session.  
 */
@Controller
@Slf4j
@RequestMapping("smd/smt")
public class SMTViewController {
	private static final String SMT_ATTR = "smt";
	private static final String PI_ATTR = "procinfo";
	private static final String EP_ATTR = "endpoint";
	private static final String R_ATTR = "redirection";

	@Value("${smp.ui.maxitems_per_page:50}")
	private int maxItemsPerPage;

	@Value("${smp.mgmt_api_enabled:false}")
	private boolean apiEnabled;
	
	@Autowired
	protected SMTMgmtService		smtMgmtSvc;
	@Autowired
	protected ServiceMgmtService	svcsMgmtSvc;
	@Autowired
	protected ProcessMgmtService	procMgmtSvc;
	@Autowired
	protected EndpointMgmtService	epMgmtSvc;
	@Autowired
	protected ParticipantsServiceImpl 	partMgmtSvc;	
	@Autowired
	protected IdUtils idUtils;
	
	@ModelAttribute("allServices")
	public Collection<? extends Service> populateServices() throws PersistenceException {
		return svcsMgmtSvc.getServices();
	}

	@GetMapping(value = {"","/"})
    public ModelAndView getOverview(@RequestParam(name = "page", required = false) Integer page) throws PersistenceException {
		return new ModelAndView("smt", "templates", 
				smtMgmtSvc.getTemplates(PageRequest.of(page == null ? 0 : page, maxItemsPerPage))
				.map(s -> {
					try {
						return new Pair<ServiceMetadataTemplateEntity, Long>((ServiceMetadataTemplateEntity) s, partMgmtSvc.countParticipantsSupporting(s));
					} catch (PersistenceException e) {
						log.error("Could not count templates for service: {}", Utils.getExceptionTrace(e));
						return new Pair<ServiceMetadataTemplateEntity, Integer>((ServiceMetadataTemplateEntity) s, 0);
					}
				})).addObject("apiEnabled", SMPServerApplication.isMgmtAPILoaded() || apiEnabled);	    
    }

	@GetMapping(value = "/delete/{id}")
	public String deleteSMT(@AuthenticationPrincipal UserAccount user, @PathVariable("id") Long id) throws PersistenceException {
		smtMgmtSvc.deleteTemplate(user, smtMgmtSvc.getTemplate(id));
		return "redirect:/smd/smt";
	}

	@GetMapping({"/add", "/edit/{id}"})
	public ModelAndView editSMT(@PathVariable(name = "id", required = false) String id, HttpSession s) throws NoSuchElementException, PersistenceException {
		ServiceMetadataTemplateEntity smt = null;
		if (!Utils.isNullOrEmpty(id))
			smt = (ServiceMetadataTemplateEntity) smtMgmtSvc.getTemplate(Long.valueOf(id));
		else {
			smt = new ServiceMetadataTemplateEntity();
			smt.addProcessGroup(new ProcessGroupEntity());
		}
		s.setAttribute(SMT_ATTR, smt);
		return new ModelAndView("smt_form", SMT_ATTR, s.getAttribute(SMT_ATTR));		
	}
	
	@PostMapping(value = "/update")
	public String saveSMT(@AuthenticationPrincipal UserAccount user, @ModelAttribute(SMT_ATTR) @Valid ServiceMetadataTemplateEntity input, 
						  BindingResult br, Model m, HttpSession s) throws PersistenceException {
		ServiceMetadataTemplateEntity smt = updateBasicInfo(m, s);
		List<ProcessGroupEntity> procGroups = smt.getProcessMetadata();
		if (procGroups.isEmpty())
			br.rejectValue("processMetadata", "NoProcGroups", "At least one process group must be specified");
		else {
			for(int i = 0; i < procGroups.size(); i++) {
				ProcessGroupEntity pg = procGroups.get(i);
				if (pg.getEndpoints().isEmpty() && pg.getRedirection() == null)
					br.rejectValue("processMetadata[" + i + "]", "NoEndpoint", "Either an Endpoint or a Redirection must be specified");
			}
		}
		if (br.hasErrors()) 
			return "smt_form";
		else if (smt.getOid() == null) 
			smtMgmtSvc.addTemplate(user, smt);
		else 
			smtMgmtSvc.updateTemplate(user, smt);
		
		return "redirect:/smd/smt";
	}

	@PostMapping(value = "/update", params = { "addProcGroup" })
	public String addProcGroup(@ModelAttribute(SMT_ATTR) ServiceMetadataTemplateEntity input, Model m, HttpSession s) {
		return performUpdate(m, s, smt -> smt.addProcessGroup(new ProcessGroupEntity()));
	}

	@PostMapping(value = "/update", params = { "removeProcGroup" })
	public String removeProcGroup(@ModelAttribute(SMT_ATTR) ServiceMetadataTemplateEntity input, Model m, HttpSession s, 
								  @RequestParam("removeProcGroup") Long row) {
		return performUpdate(m, s, smt -> smt.getProcessMetadata().remove(row.intValue()));
	}

	@PostMapping(value = "/update", params = { "addProcessInfo" })
	public String addProcessInfo(@ModelAttribute(SMT_ATTR) ServiceMetadataTemplateEntity smtInput, Model m, HttpSession s, 
								 @RequestParam("addProcessInfo") Long row) {
		return performUpdate(m, s, smt -> {
			m.addAttribute(PI_ATTR, new PgProcInfoFormData(row.intValue()));
			try {
				m.addAttribute("allProcesses", procMgmtSvc.getProcesses());
			} catch (PersistenceException e) {
				log.error("Could not get available Processes : {}", Utils.getExceptionTrace(e));
			}
		});
	}

	@PostMapping(value = "/update", params = { "editProcessInfo" })
	public String editProcessInfo(@ModelAttribute(SMT_ATTR) ServiceMetadataTemplateEntity smtInput, Model m, HttpSession s,
								  @RequestParam("editProcessInfo") String idx) {
		return performUpdate(m, s, smt -> {
			int pgIdx = Integer.parseInt(idx.substring(0, idx.indexOf(',')));
			int procIdx = Integer.parseInt(idx.substring(idx.indexOf(',') + 1));
			m.addAttribute(PI_ATTR, new PgProcInfoFormData(pgIdx, procIdx,
											   smt.getProcessMetadata().get(pgIdx).getProcessInfo().get(procIdx)));
			try {
				m.addAttribute("allProcesses", procMgmtSvc.getProcesses());
			} catch (PersistenceException e) {
				log.error("Could not get available Processes : {}", Utils.getExceptionTrace(e));
			}
		});
	}

	@PostMapping(value = "/update", params = { "removeProcessInfo" })
	public String removeProcessInfo(@ModelAttribute(SMT_ATTR) ServiceMetadataTemplateEntity smtInput, Model m, HttpSession s, 
									@RequestParam("removeProcessInfo") String idx) {
		return performUpdate(m, s, smt -> {
			int pgIdx = Integer.parseInt(idx.substring(0, idx.indexOf(',')));
			int procIdx = Integer.parseInt(idx.substring(idx.indexOf(',') + 1));
			smt.getProcessMetadata().get(pgIdx).getProcessInfo().remove(procIdx);
		});
	}

	@PostMapping(value = "/pg/update/procinfo")
	public String updateProcessInfo(@ModelAttribute(PI_ATTR) @Valid PgProcInfoFormData piData, BindingResult br, 
									Model m, HttpSession s) {
		return performUpdate(m, s, smt -> {
			if (!br.hasErrors()) {
				m.addAttribute(PI_ATTR, null);
				ProcessIdentifier pid = idUtils.parseProcessIDString(piData.getProcID(), 
																	"::" + EmbeddedProcessIdentifier.NO_PROCESS);
				Process p;
				try {
					p = procMgmtSvc.getProcess(pid);
				} catch (PersistenceException e) {
					log.error("Could not get Process (ProcID={}) : {}", piData.getProcID(), Utils.getExceptionTrace(e));
					throw new IllegalStateException(e);
				}
				ProcessGroupEntity pg = smt.getProcessMetadata().get(piData.getPgIndex());
				Set<EmbeddedIdentifier> roles = new HashSet<>();
				if (!Utils.isNullOrEmpty(piData.getRolesCSL())) {
					for(String role : piData.getRolesCSL().split(","))
						roles.add(new EmbeddedIdentifier(role));
				}				
				if (piData.getProcIndex() < 0) {
					ProcessInfoEntity procInfo = new ProcessInfoEntity();
					procInfo.setProcess(p);
					procInfo.setRoles(roles);				
					pg.addProcessInfo(procInfo);
				} else { 
					ProcessInfoEntity procInfo = pg.getProcessInfo().get(piData.getProcIndex());
					procInfo.setProcess(p);
					procInfo.setRoles(roles);				
				}
			}
		});
	}

	@PostMapping(value = "/update", params = { "addEndpoint" })
	public String addEndpoint(@ModelAttribute(SMT_ATTR) ServiceMetadataTemplateEntity smtInput, Model m, HttpSession s,
							  @RequestParam("addEndpoint") Long row) {
		return performUpdate(m, s, smt -> {
			int pgIdx = row.intValue();
			m.addAttribute(EP_ATTR, new PgEndpointFormData(pgIdx, null));
			List<Long> regdEPs = smt.getProcessMetadata().get(pgIdx).getEndpoints().stream()
															.map(ep -> ep.getId()).toList();
			try {
				m.addAttribute("availableEndpoints", epMgmtSvc.getEndpoints().stream()
													.filter(ep -> !regdEPs.contains(ep.getId())).toList());
			} catch (PersistenceException e) {
				log.error("Could not get available Endpoints : {}", Utils.getExceptionTrace(e));
			}
		});
	}

	@PostMapping(value = "/update", params = { "removeEndpoint" })
	public String removeEndpoint(@ModelAttribute(SMT_ATTR) ServiceMetadataTemplateEntity smtInput, Model m, HttpSession s, 
								 @RequestParam("removeEndpoint") String idx) {
		return performUpdate(m, s, smt -> {
			int pgIdx = Integer.parseInt(idx.substring(0, idx.indexOf(',')));
			int epIdx = Integer.parseInt(idx.substring(idx.indexOf(',') + 1));
			smt.getProcessMetadata().get(pgIdx).getEndpoints().remove(epIdx);
		});
	}

	@PostMapping(value = "/pg/update/endpoint")
	public String updateEndpoint(@ModelAttribute(EP_ATTR) @Valid PgEndpointFormData epData, BindingResult br, 
								Model m, HttpSession s) {
		return performUpdate(m, s, smt -> {
			int pgIdx = epData.getPgIndex();
			try {
				if (!br.hasErrors()) {
					m.addAttribute(EP_ATTR, null);
					smt.getProcessMetadata().get(pgIdx).addEndpoint(epMgmtSvc.getEndpoint(epData.getEpId()));
				} else {
					List<Long> regdEPs = smt.getProcessMetadata().get(pgIdx).getEndpoints().stream()
																	.map(ep -> ep.getOid()).toList();
					m.addAttribute("availableEndpoints", epMgmtSvc.getEndpoints().stream()
															.filter(ep -> !regdEPs.contains(ep.getId())).toList());
				}
			} catch (PersistenceException e) {
				log.error("Could not get Endpoint data : {}", Utils.getExceptionTrace(e));
			}
		});
	}

	@PostMapping(value = "/update", params = { "addRedirect" })
	public String addRedirection(@ModelAttribute(SMT_ATTR) ServiceMetadataTemplateEntity smtInput,Model m, HttpSession s, 
								 @RequestParam("addRedirect") Long row) {
		return performUpdate(m, s, smt -> m.addAttribute(R_ATTR, new PgRedirectionFormData(row.intValue())));
	}

	@PostMapping(value = "/update", params = { "editRedirect" })
	public String editRedirection(@ModelAttribute(SMT_ATTR) ServiceMetadataTemplateEntity smtInput, Model m, HttpSession s, 
								  @RequestParam("editRedirect") Long row) {
		return performUpdate(m, s, smt -> m.addAttribute(R_ATTR, 
			new PgRedirectionFormData(row.intValue(), smt.getProcessMetadata().get(row.intValue()).getRedirection())));
	}

	@PostMapping(value = "/update", params = { "removeRedirect" })
	public String removeRedirection(@ModelAttribute(SMT_ATTR) ServiceMetadataTemplateEntity smtInput, Model m, HttpSession s, 
									@RequestParam("removeRedirect") Long idx) {
		return performUpdate(m, s, smt -> smt.getProcessMetadata().get(idx.intValue()).removeRedirection());
	}
	
	@PostMapping(value = "/pg/update/redirect")
	public String updateRedirection(@ModelAttribute(R_ATTR) @Valid PgRedirectionFormData redirection, BindingResult br, 
									Model m, HttpSession s) {
		return performUpdate(m, s, smt -> {
			int pgIdx = redirection.getPgIndex();

			X509Certificate x509Cert = null;
			if (!Utils.isNullOrEmpty(redirection.getPemText())) {
				try {
					x509Cert = CertificateUtils.getCertificate(redirection.getPemText());
				} catch (CertificateException ex) {
					br.rejectValue("pemText", "invalidCert", "Provided text does not contain PEM encoded certificate");
				}
			}
			URL targetURL = null;
			if (!Utils.isNullOrEmpty(redirection.getTargetURL())) {
				try {
					targetURL = new URL(redirection.getTargetURL());
					if (!targetURL.toURI().isAbsolute())
						br.rejectValue("targetURL", "invalidURL", "A valid http(s) URL must be specified");						
					else if (!targetURL.getProtocol().toLowerCase().startsWith("http"))
						br.rejectValue("targetURL", "invalidURL", "A valid http(s) URL must be specified");
				} catch (MalformedURLException | URISyntaxException invalidURL) {
					br.rejectValue("targetURL", "invalidURL", "A valid http(s) URL must be specified");
				}
			}
			if (!br.hasErrors()) {
				m.addAttribute(R_ATTR, null);
				EmbeddedRedirection updRedirect = new EmbeddedRedirection();
				updRedirect.setRedirectionURL(targetURL);
				try {
					updRedirect.setSMPCertificate(x509Cert);
				} catch (CertificateEncodingException ex) {
					// Should never happen as cert is already checked above
				}
				smt.getProcessMetadata().get(pgIdx).setRedirection(updRedirect);
			} else
				m.addAttribute(R_ATTR, redirection);
		});
	}

	private String performUpdate(Model m, HttpSession s, Consumer<ServiceMetadataTemplateEntity> update) {
		ServiceMetadataTemplateEntity sSMT = updateBasicInfo(m, s);
		update.accept(sSMT);
		m.addAttribute(SMT_ATTR, sSMT);
		return "smt_form";
	}

	private ServiceMetadataTemplateEntity updateBasicInfo(Model m, HttpSession s) {
		ServiceMetadataTemplateEntity sSMT = (ServiceMetadataTemplateEntity) s.getAttribute(SMT_ATTR);
		ServiceMetadataTemplateEntity mSMT = (ServiceMetadataTemplateEntity) m.getAttribute(SMT_ATTR);
		if (mSMT != null) {
			sSMT.setService(mSMT.getService());
			sSMT.setName(!Utils.isNullOrEmpty(mSMT.getName()) ? mSMT.getName() :
													mSMT.getService() != null ? mSMT.getService().getName() : null);
		}
		return sSMT;
	}
}
