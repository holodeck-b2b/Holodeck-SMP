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
import java.net.URL;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

import org.holodeckb2b.bdxr.smp.server.SMPServerApplication;
import org.holodeckb2b.bdxr.smp.server.db.entities.IdentifierE;
import org.holodeckb2b.bdxr.smp.server.db.entities.ProcessGroupE;
import org.holodeckb2b.bdxr.smp.server.db.entities.ProcessInfoE;
import org.holodeckb2b.bdxr.smp.server.db.entities.RedirectionE;
import org.holodeckb2b.bdxr.smp.server.db.entities.ServiceE;
import org.holodeckb2b.bdxr.smp.server.db.entities.ServiceMetadataTemplateE;
import org.holodeckb2b.bdxr.smp.server.db.repos.EndpointRepository;
import org.holodeckb2b.bdxr.smp.server.db.repos.ProcessRepository;
import org.holodeckb2b.bdxr.smp.server.db.repos.ServiceMetadataTemplateRepository;
import org.holodeckb2b.bdxr.smp.server.db.repos.ServiceRepository;
import org.holodeckb2b.bdxr.smp.server.ui.viewmodels.PgAddEndpointFormData;
import org.holodeckb2b.bdxr.smp.server.ui.viewmodels.ProcessInfoFormData;
import org.holodeckb2b.bdxr.smp.server.ui.viewmodels.RedirectionFormData;
import org.holodeckb2b.commons.Pair;
import org.holodeckb2b.commons.security.CertificateUtils;
import org.holodeckb2b.commons.util.Utils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;

@Controller
@RequestMapping("smd/smt")
public class SMTViewController {
	private static final String SMT_ATTR = "serviceMetadataTemplateE";

	@Value("${api.enabled:false}")
	protected boolean	mgmtAPIenabled;
	
	@Autowired
	protected ServiceMetadataTemplateRepository	templates;
	@Autowired
	protected ServiceRepository		services;
	@Autowired
	protected ProcessRepository		processes;
	@Autowired
	protected EndpointRepository	endpoints;

	private boolean isMgmtAPIenabled() {
		return mgmtAPIenabled || SMPServerApplication.isMgmtAPILoaded;
	}
	
	@ModelAttribute("allServices")
	public List<ServiceE> populateServices() {
		return services.findAll();
	}

	@GetMapping(value = {"","/"})
    public String getOverview(Model m) {
		m.addAttribute("templates", templates.findAll().parallelStream()
							.map(t -> new Pair<ServiceMetadataTemplateE, Integer>(t, templates.getNumberOfBindings(t)))
							.toList());
		m.addAttribute("apiEnabled", isMgmtAPIenabled());
	    return "admin-ui/smt";
    }

	@GetMapping("/edit/{oid}")
	public String editTemplate(@PathVariable("oid") Long oid, Model m, HttpSession s) {
		ServiceMetadataTemplateE smt;
		if (oid > 0)
			smt = templates.getById(oid);
		else {
			smt = new ServiceMetadataTemplateE();
			smt.getProcessMetadata().add(new ProcessGroupE(smt));
		}
		s.setAttribute(SMT_ATTR, smt);
		m.addAttribute(SMT_ATTR, smt);
		m.addAttribute("apiEnabled", isMgmtAPIenabled());
		return "admin-ui/smt_form";
	}

	@GetMapping("/delete/{oid}")
	public String removeTemplate(@PathVariable("oid") Long oid, Model m, HttpSession s) {
		templates.deleteById(oid);
	    return "redirect:/smd/smt";
	}


	@PostMapping(value = "/update")
	public String saveSMDTemplate(@ModelAttribute(SMT_ATTR) @Valid ServiceMetadataTemplateE input, BindingResult br, Model m, HttpSession s) {
		ServiceMetadataTemplateE smt = updateBasics(m, s);

		List<ProcessGroupE> procGroups = smt.getProcessMetadata();
		if (procGroups.isEmpty())
			br.rejectValue("processMetadata", "NoProcGroups", "At least one process group must be specified");
		else {
			for(int i = 0; i < procGroups.size(); i++) {
				ProcessGroupE pg = procGroups.get(i);
				if (pg.getEndpoints().isEmpty() && pg.getRedirection() == null)
					br.rejectValue("processMetadata[" + i + "]", "NoEndpoint", "Either an Endpoint or a Redirection must be specified");
			}
		}
		if (br.hasErrors()) {
			((ServiceMetadataTemplateE) br.getTarget()).setProcessMetadata(smt.getProcessMetadata());
			return "admin-ui/smt_form";
		} else {
			templates.save(smt);
			return "redirect:/smd/smt";
		}
	}

	@PostMapping(value = "/update", params = { "addProcGroup" })
	public String addProcGroup(@ModelAttribute ServiceMetadataTemplateE input, Model m, HttpSession s) {
		return performUpdate(m, s, smt -> smt.getProcessMetadata().add(new ProcessGroupE(smt)));
	}

	@PostMapping(value = "/update", params = { "removeProcGroup" })
	public String removeProcGroup(@ModelAttribute ServiceMetadataTemplateE input, Model m, HttpSession s, @RequestParam("removeProcGroup") Long row) {
		return performUpdate(m, s, smt -> smt.getProcessMetadata().remove(row.intValue()));
	}

	@PostMapping(value = "/update", params = { "addProcess" })
	public String addProcessInfo(@ModelAttribute ServiceMetadataTemplateE input, Model m, HttpSession s, @RequestParam("addProcess") Long row) {
		return performUpdate(m, s, smt -> {
			m.addAttribute(new ProcessInfoFormData(row.intValue(), -1));
			m.addAttribute("allProcesses", processes.findAll());
		});
	}

	@PostMapping(value = "/update", params = { "editProcess" })
	public String editProcessInfo(@ModelAttribute ServiceMetadataTemplateE input, Model m, HttpSession s, @RequestParam("editProcess") String idx) {
		return performUpdate(m, s, smt -> {
			int pgIdx = Integer.parseInt(idx.substring(0, idx.indexOf(',')));
			int procIdx = Integer.parseInt(idx.substring(idx.indexOf(',') + 1));
			m.addAttribute(new ProcessInfoFormData(pgIdx, procIdx,
											   smt.getProcessMetadata().get(pgIdx).getProcessInfo().get(procIdx)));
			m.addAttribute("allProcesses", processes.findAll());
		});
	}

	@PostMapping(value = "/update", params = { "removeProcess" })
	public String removeProcessInfo(@ModelAttribute ServiceMetadataTemplateE input, Model m, HttpSession s, @RequestParam("removeProcess") String idx) {
		return performUpdate(m, s, smt -> {
			int pgIdx = Integer.parseInt(idx.substring(0, idx.indexOf(',')));
			int procIdx = Integer.parseInt(idx.substring(idx.indexOf(',') + 1));
			smt.getProcessMetadata().get(pgIdx).getProcessInfo().remove(procIdx);
		});
	}

	@PostMapping(value = "/pg/update/procinfo")
	public String updateProcessInfo(@ModelAttribute @Valid ProcessInfoFormData input, BindingResult br, Model m, HttpSession s) {
		return performUpdate(m, s, smt -> {
			if (!br.hasErrors()) {
				m.addAttribute("processInfoFormData", null);
				ProcessGroupE pg = smt.getProcessMetadata().get(input.getPgIndex());
				ProcessInfoE procInfo;
				if (input.getProcIndex() < 0) {
					procInfo = new ProcessInfoE(pg);
					pg.getProcessInfo().add(procInfo);
				} else
					procInfo = pg.getProcessInfo().get(input.getProcIndex());
				procInfo.setProcess(processes.getById(input.getProcessOID()));
				Set<IdentifierE> roles = new HashSet<>();
				if (!Utils.isNullOrEmpty(input.getRolesAsArray())) {
					for(String role : input.getRolesAsArray().split(","))
						roles.add(new IdentifierE(role));
				}
				procInfo.setRoles(roles);
			}
		});
	}


	@PostMapping(value = "/update", params = { "addEndpoint" })
	public String addEndpoint(@ModelAttribute ServiceMetadataTemplateE input, Model m, HttpSession s, @RequestParam("addEndpoint") Long row) {
		return performUpdate(m, s, smt -> {
			int pgIdx = row.intValue();
			m.addAttribute("endpoint", new PgAddEndpointFormData(pgIdx));

			List<Long> regdEPs = smt.getProcessMetadata().get(pgIdx).getEndpoints().stream()
															.map(ep -> ep.getOid()).toList();
			m.addAttribute("availableEndpoints", endpoints.findAll().stream()
												.filter(ep -> !regdEPs.contains(ep.getOid())).toList());
		});
	}

	@PostMapping(value = "/update", params = { "removeEndpoint" })
	public String removeEndpoint(@ModelAttribute ServiceMetadataTemplateE input, Model m, HttpSession s, @RequestParam("removeEndpoint") String idx) {
		return performUpdate(m, s, smt -> {
			int pgIdx = Integer.parseInt(idx.substring(0, idx.indexOf(',')));
			int epIdx = Integer.parseInt(idx.substring(idx.indexOf(',') + 1));
			smt.getProcessMetadata().get(pgIdx).getEndpoints().remove(epIdx);
		});
	}

	@PostMapping(value = "/pg/update/endpoint")
	public String updateEndpoint(@ModelAttribute("endpoint") @Valid PgAddEndpointFormData input, BindingResult br, Model m, HttpSession s) {
		return performUpdate(m, s, smt -> {
			int pgIdx = input.getPgIndex();
			if (!br.hasErrors()) {
				m.addAttribute("endpoint", null);
				smt.getProcessMetadata().get(pgIdx).getEndpoints().add(endpoints.getById(input.getEpOID()));
			} else {
				m.addAttribute("endpoint", input);
				List<Long> regdEPs = smt.getProcessMetadata().get(pgIdx).getEndpoints().stream()
																.map(ep -> ep.getOid()).toList();
				m.addAttribute("availableEndpoints", endpoints.findAll().stream()
													.filter(ep -> !regdEPs.contains(ep.getOid())).toList());
			}
		});
	}

	@PostMapping(value = "/update", params = { "addRedirect" })
	public String addRedirection(@ModelAttribute ServiceMetadataTemplateE input, Model m, HttpSession s, @RequestParam("addRedirect") Long row) {
		return performUpdate(m, s, smt -> m.addAttribute("redirection", new RedirectionFormData(row.intValue())));
	}

	@PostMapping(value = "/update", params = { "removeRedirect" })
	public String removeRedirection(@ModelAttribute ServiceMetadataTemplateE input, Model m, HttpSession s, @RequestParam("removeRedirect") Long idx) {
		return performUpdate(m, s, smt -> smt.getProcessMetadata().get(idx.intValue()).setRedirection(null));
	}

	@PostMapping(value = "/pg/update/redirect")
	public String updateRedirection(@ModelAttribute("redirection") @Valid RedirectionFormData input, BindingResult br, Model m, HttpSession s) {
		return performUpdate(m, s, smt -> {
			int pgIdx = input.getPgIndex();

			X509Certificate x509Cert = null;
			if (!Utils.isNullOrEmpty(input.getPemText())) {
				try {
					x509Cert = CertificateUtils.getCertificate(input.getPemText());
				} catch (CertificateException ex) {
					br.rejectValue("pemText", "invalidCert", "Provided text does not contain PEM encoded certificate");
				}
			}
			URL targetURL = null;
			if (!Utils.isNullOrEmpty(input.getTargetURL())) {
				try {
					targetURL = new URL(input.getTargetURL());
				} catch (MalformedURLException invalidURL) {
					br.rejectValue("targetURL", "invalidURL", "A valid http(s) URL must be specified");
				}
				if (!targetURL.getProtocol().toLowerCase().startsWith("http"))
					br.rejectValue("targetURL", "invalidURL", "A valid http(s) URL must be specified");
			}
			if (!br.hasErrors()) {
				m.addAttribute("redirection", null);
				RedirectionE redirection = input.getOid() != null ? smt.getProcessMetadata().get(pgIdx).getRedirection()
																  : new RedirectionE();
				redirection.setRedirectionURL(targetURL);
				try {
					redirection.setSMPCertificate(x509Cert);
				} catch (CertificateEncodingException ex) {
					// Should never happen as cert is already checked above
				}
				smt.getProcessMetadata().get(pgIdx).setRedirection(redirection);
			} else
				m.addAttribute("redirection", input);
		});
	}

	private String performUpdate(Model m, HttpSession s, Consumer<ServiceMetadataTemplateE> update) {
		ServiceMetadataTemplateE updated = updateBasics(m, s);
		update.accept(updated);
		m.addAttribute(SMT_ATTR, updated);
		return "admin-ui/smt_form";
	}

	private ServiceMetadataTemplateE updateBasics(Model m, HttpSession s) {
		ServiceMetadataTemplateE input = (ServiceMetadataTemplateE) m.getAttribute(SMT_ATTR);
		ServiceMetadataTemplateE stored = (ServiceMetadataTemplateE) s.getAttribute(SMT_ATTR);
		if (input != null) {
			stored.setService(input.getService());
			stored.setName(!Utils.isNullOrEmpty(input.getName()) ? input.getName() :
													input.getService() != null ? input.getService().getName() : null);
		}
		return stored;
	}
}
