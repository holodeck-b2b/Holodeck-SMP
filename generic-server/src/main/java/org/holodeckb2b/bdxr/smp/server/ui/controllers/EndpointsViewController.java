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
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

import org.holodeckb2b.bdxr.smp.server.datamodel.Endpoint;
import org.holodeckb2b.bdxr.smp.server.datamodel.TransportProfile;
import org.holodeckb2b.bdxr.smp.server.db.entities.EndpointEntity;
import org.holodeckb2b.bdxr.smp.server.services.core.EndpointMgmtService;
import org.holodeckb2b.bdxr.smp.server.services.core.PersistenceException;
import org.holodeckb2b.bdxr.smp.server.services.core.SMTMgmtService;
import org.holodeckb2b.bdxr.smp.server.services.core.TransportProfileMgmtService;
import org.holodeckb2b.bdxr.smp.server.ui.auth.UserAccount;
import org.holodeckb2b.bdxr.smp.server.ui.viewmodels.CertificateFormData;
import org.holodeckb2b.bdxr.smp.server.ui.viewmodels.EndpointFormData;
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
 * Adding/editing a certificate of the Endpoint is done in a popup dialog. To keep the, possibly edited, endpoint data 
 * available for when the dialog is closed, the endpoint data is both kept in the model and stored in the session.  
 */
@Controller
@Slf4j
@RequestMapping("endpoints")
public class EndpointsViewController {
	private static final String EP_ATTR = "endpoint";
	private static final String CERT_ATTR = "cert2edit";
	
	@Value("${smp.ui.maxitems_per_page:50}")
	private int maxItemsPerPage;

	@Autowired
	protected EndpointMgmtService	epSvc;
	
	@Autowired
	protected TransportProfileMgmtService tpSvc;

	@Autowired
	protected SMTMgmtService smtSvc;

	@ModelAttribute("tpProfiles")
	public Collection<? extends TransportProfile> provideTransportProfiles() {
		try {
			return tpSvc.getTransportProfiles();
		} catch (PersistenceException e) {
			log.error("Could not retrieve Transport Profiles: {}", Utils.getExceptionTrace(e));
			return new ArrayList<>();
		}
	}
		
	@GetMapping(value = {"","/"})
    public ModelAndView getOverview(@RequestParam(name = "page", required = false) Integer page) throws PersistenceException {
		return new ModelAndView("endpoints", "endpoints", 
				epSvc.getEndpoints(PageRequest.of(page == null ? 0 : page, maxItemsPerPage))
				.map(ep -> {
					try {
						return new Pair<Endpoint, Integer>((Endpoint) ep, smtSvc.findTemplatesUsingEndpoint(ep).size());
					} catch (PersistenceException e) {
						log.error("Could not count templates for service: {}", Utils.getExceptionTrace(e));
						return new Pair<Endpoint, Integer>((Endpoint) ep, 0);
					}
				}));		
    }

	@GetMapping(value = "/delete/{id}")
	public String deleteEndpoint(@AuthenticationPrincipal UserAccount user, @PathVariable("id") Long id) throws PersistenceException {
		epSvc.deleteEndpoint(user, epSvc.getEndpoint(id));
		return "redirect:/endpoints";
	}

	@GetMapping({"/add", "/edit/{id}"})
	public ModelAndView editEndpoint(@PathVariable(name = "id", required = false) String id, HttpSession s) throws NoSuchElementException, PersistenceException {
		EndpointEntity ep = null;
		if (!Utils.isNullOrEmpty(id))
			ep = (EndpointEntity) epSvc.getEndpoint(Long.valueOf(id));
		else 
			ep = new EndpointEntity();
		
		s.setAttribute(EP_ATTR, new EndpointFormData(ep));
		return new ModelAndView("endpoint_form", EP_ATTR, s.getAttribute(EP_ATTR));		
	}
	
	@PostMapping(value = "/edit")
	public String saveEndpoint(@AuthenticationPrincipal UserAccount user, @ModelAttribute(name = EP_ATTR) @Valid EndpointFormData input, 
								BindingResult br, HttpSession s) throws PersistenceException {
		URL epURL = null;
		try {
			epURL = new URL(input.getUrl());
			if (!epURL.toURI().isAbsolute())
				br.rejectValue("url", "invalidURL", "A valid URL must be provided");
		} catch (MalformedURLException | URISyntaxException invalidURL) {
			br.rejectValue("url", "invalidURL", "A valid URL must be provided");
		}

		LocalDateTime activation = null, expiration = null;
		if (input.getActivationTime() != null && input.getActivationDate() == null) 
			br.rejectValue("activationDate", "missingDate", "A valid date must be provided");
		else if (input.getActivationDate() != null) {
			input.setActivationTime(input.getActivationTime() != null ? input.getActivationTime()
																		  : LocalTime.MIDNIGHT);
			activation = input.getActivationDate().atTime(input.getActivationTime());
		}
		if (input.getExpirationTime() != null && input.getExpirationDate() == null)
			br.rejectValue("expirationDate", "missingDate", "A valid date must be provided");
		else if (input.getExpirationDate() != null) {
			input.setExpirationTime(input.getExpirationTime() != null ? input.getExpirationTime()
																		  : LocalTime.MIDNIGHT);
			expiration = input.getExpirationDate().atTime(input.getExpirationTime());
		}
		if (activation != null && expiration != null && expiration.isBefore(activation)) {
			br.rejectValue("expirationDate", "expiresBeforeActive", "Expiration must be after activation");
			br.rejectValue("expirationTime", "expiresBeforeActive", "");
		}
		
		if (br.hasErrors())
			return "endpoint_form";
		
		EndpointEntity epEntity = input.getOid() == null ? new EndpointEntity()  
														 : (EndpointEntity) epSvc.getEndpoint(input.getOid());
		epEntity.setName(input.getName());
		epEntity.setTransportProfile(tpSvc.getTransportProfile(input.getProfileId()));
		epEntity.setEndpointURL(epURL);
		epEntity.setServiceActivationDate(activation != null ? activation.atZone(ZoneOffset.UTC) : null);
		epEntity.setServiceExpirationDate(expiration != null ? expiration.atZone(ZoneOffset.UTC) : null);
		epEntity.setContactInfo(input.getContactInfo());
		epEntity.setDescription(input.getDescription());
		epEntity.setCertificates(((EndpointFormData) s.getAttribute(EP_ATTR)).getCerts().parallelStream()
									.map(c -> c.toEmbeddedCertificate()).collect(Collectors.toList()));

		if (epEntity.getOid() == null)
			epSvc.addEndpoint(user, epEntity);
		else
			epSvc.updateEndpoint(user, epEntity);
		
		s.removeAttribute(EP_ATTR);
		return "redirect:/endpoints";
	}

	@PostMapping(value = "/edit", params = { "editCertificate" })
	public ModelAndView editCertificate(@ModelAttribute EndpointFormData input, HttpSession s, @RequestParam("editCertificate") Long row) {
		input.setCerts(((EndpointFormData) s.getAttribute(EP_ATTR)).getCerts());
		s.setAttribute(EP_ATTR, input);	
		return new ModelAndView("endpoint_form", EP_ATTR, s.getAttribute(EP_ATTR))
					.addObject(CERT_ATTR, row < 0 ? new CertificateFormData() : input.getCerts().get(row.intValue())); 
	}
		
	@PostMapping(value = "/edit", params = { "saveCertificate" })
	public String saveCertificate(@ModelAttribute("cert2edit") @Valid CertificateFormData input, BindingResult br, HttpSession s, 
			Model m, @RequestParam("saveCertificate") Long row) {		
		X509Certificate x509Cert = null;
		if (!Utils.isNullOrEmpty(input.getPemText())) {
			try {
				x509Cert = CertificateUtils.getCertificate(input.getPemText());
				LocalDateTime activation = null, expiration = null;
				if (input.getActivationTime() != null && input.getActivationDate() == null)
					br.rejectValue("activationDate", "missingDate", "A valid date must be provided");
				else if (input.getActivationDate() != null) {
					input.setActivationTime(input.getActivationTime() != null ? input.getActivationTime()
																				  : LocalTime.MIDNIGHT);
					activation = input.getActivationDate().atTime(input.getActivationTime());
				}
				if (input.getExpirationTime() != null && input.getExpirationDate() == null)
					br.rejectValue("expirationDate", "missingDate", "A valid date must be provided");
				else if (input.getExpirationDate() != null) {
					input.setExpirationTime(input.getExpirationTime() != null ? input.getExpirationTime()
																				  : LocalTime.MIDNIGHT);
					expiration = input.getExpirationDate().atTime(input.getExpirationTime());
				}
				if (activation != null &&
					activation.toInstant(ZoneOffset.UTC).isBefore(x509Cert.getNotBefore().toInstant())) {
					br.rejectValue("activationDate", "activeBeforeValid", "Activation cannot be before certificate validity period");
					br.rejectValue("activationTime", "activeBeforeValid", "");
				}
				if (expiration != null) {
					if (activation != null && expiration.isBefore(activation)) {
						br.rejectValue("expirationDate", "expiresBeforeActive", "Expiration must be after activation");
						br.rejectValue("expirationTime", "activeAfterValid", "");
					} if (expiration.toInstant(ZoneOffset.UTC).isAfter(x509Cert.getNotAfter().toInstant())) {
						br.rejectValue("expirationDate", "activeAfterValid", "Expiration cannot be after certificate validity period");
						br.rejectValue("expirationTime", "activeAfterValid", "");
					}
				}
			} catch (CertificateException ex) {
				br.rejectValue("pemText", "invalidCert", "Provided text does not contain PEM encoded certificate");
			}
		}
		m.addAttribute(EP_ATTR, s.getAttribute(EP_ATTR));
		if (!br.hasErrors()) {
			input.setSubjectName(x509Cert.getSubjectX500Principal().getName());
			if (row < 0)
				((EndpointFormData) s.getAttribute(EP_ATTR)).getCerts().add(input);
			else
				((EndpointFormData) s.getAttribute(EP_ATTR)).getCerts().set(row.intValue(), input);
			m.addAttribute(CERT_ATTR, null);
		} else {
			m.addAttribute(CERT_ATTR, input);
			m.addAttribute("certIndex", row);
		}
		
		return "endpoint_form";
	}

	@PostMapping(value = "/edit", params = { "removeCertificate" })
	public ModelAndView removeCertificate(@ModelAttribute EndpointFormData input, HttpSession s, @RequestParam("removeCertificate") Long row) {
		((EndpointFormData) s.getAttribute(EP_ATTR)).getCerts().remove(row.intValue());
		return new ModelAndView("endpoint_form", EP_ATTR, s.getAttribute(EP_ATTR));
	}

}
