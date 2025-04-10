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
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.holodeckb2b.bdxr.smp.server.db.entities.CertificateE;
import org.holodeckb2b.bdxr.smp.server.db.entities.EndpointInfoE;
import org.holodeckb2b.bdxr.smp.server.db.entities.TransportProfileE;
import org.holodeckb2b.bdxr.smp.server.db.repos.EndpointRepository;
import org.holodeckb2b.bdxr.smp.server.db.repos.TransportProfileRepository;
import org.holodeckb2b.bdxr.smp.server.ui.viewmodels.CertificateFormData;
import org.holodeckb2b.bdxr.smp.server.ui.viewmodels.EndpointFormData;
import org.holodeckb2b.commons.security.CertificateUtils;
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

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;

@Controller
@RequestMapping("endpoints")
public class EndpointsViewController {
	private static final String P_ATTR = "endpoint";

	@Autowired
	protected EndpointRepository		endpoints;
	@Autowired
	protected TransportProfileRepository profiles;

	@ModelAttribute("tpProfiles")
	public List<TransportProfileE> populateProfiles() {
		return profiles.findAll();
	}

	@GetMapping(value = {"","/"})
    public String getOverview(Model m) {
		m.addAttribute("endpoints", endpoints.findAll());
        return "admin-ui/endpoints";
    }

	@GetMapping("/add")
	public String addEndpoint(Model m, HttpSession s) {
		m.addAttribute(new EndpointFormData());
		s.setAttribute("certs", new ArrayList<CertificateFormData>());
		m.addAttribute("certs", s.getAttribute("certs"));
		return "admin-ui/endpoint_form";
	}

	@GetMapping(value = "/edit/{oid}")
	public String editEndpoint(@PathVariable("oid") Long oid, Model m, HttpSession s) {
		EndpointInfoE ep = endpoints.findById(oid).get();
		m.addAttribute(new EndpointFormData(ep));
		s.setAttribute("certs", ep.getCertificates().stream().map(c -> new CertificateFormData(c)).collect(Collectors.toList()));
		m.addAttribute("certs", s.getAttribute("certs"));
		return "admin-ui/endpoint_form";
	}

	@GetMapping(value = "/delete/{oid}")
	public String deleteEndpoint(@PathVariable("oid") Long oid, Model m, HttpSession s) {
		endpoints.deleteById(oid);
		return "redirect:/endpoints";
	}

	@PostMapping(value = "/save")
	public String saveEndpoint(@ModelAttribute @Valid EndpointFormData input, BindingResult br, Model m, HttpSession s) {
		URL epURL = null;
		try {
			epURL = new URL(input.getUrl());
		} catch (MalformedURLException invalidURL) {
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
		if (activation != null && expiration != null && expiration.isBefore(activation))
			br.rejectValue("expirationDate", "expiresBeforeActive", "Expiration must be after activation");

		if (!br.hasErrors()) {
			EndpointInfoE epEntity = input.getOid() == null ? new EndpointInfoE() :
															endpoints.findById(input.getOid()).get();
			epEntity.setName(input.getName());
			epEntity.setProfile(profiles.findById(input.getProfileId()).get());
			epEntity.setEndpointURL(epURL);
			epEntity.setServiceActivationDate(activation);
			epEntity.setServiceExpirationDate(expiration);
			epEntity.setContactInfo(input.getContactInfo());
			epEntity.setDescription(input.getDescription());

			List<CertificateE> storedCerts = epEntity.getCertificates();
			List<CertificateFormData> updCerts = (List<CertificateFormData>) s.getAttribute("certs");
			for(CertificateFormData c : updCerts) {
				boolean isNew = c.getOid() == null;
				CertificateE cEntity = isNew ? new CertificateE() : storedCerts.stream()
																			.filter(sc -> sc.getOid() == c.getOid())
																			.findFirst().get();
				try {
					cEntity.setCertificate(c.getPemText());
				} catch (CertificateException ex) {
					// Should never happen as cert if already checked on form submission
				}
				cEntity.setUsage(c.getUsage());
				if (c.getActivationDate() != null)
					cEntity.setActivationDate(c.getActivationDate().atTime(c.getActivationTime()));
				else 
					cEntity.setActivationDate(null);
				if (c.getExpirationDate()!= null)
					cEntity.setExpirationDate(c.getExpirationDate().atTime(c.getExpirationTime()));
				else 
					cEntity.setExpirationDate(null);
				
				if (isNew) {
					epEntity.getCertificates().add(cEntity);
					c.setOid(cEntity.getOid());
				}
			}
			List<Long> updCertOIDs = updCerts.parallelStream().filter(c -> c.getOid() != null)
															  .map(c -> c.getOid()).toList();
			storedCerts.removeAll(storedCerts.parallelStream().filter(c -> !updCertOIDs.contains(c.getOid())).toList());

			endpoints.save(epEntity);

			s.removeAttribute("endpointFormData");
			s.removeAttribute("certs");
			return "redirect:/endpoints";
		}

		return "admin-ui/endpoint_form";
	}

	@PostMapping(value = "/save", params = { "addCertificate" })
	public String addCertificate(@ModelAttribute EndpointFormData input, Model m, HttpSession s) {
		s.setAttribute("endpointFormData", input);
		m.addAttribute("certificateFormData", new CertificateFormData());
		return "admin-ui/endpoint_form";
	}

	@PostMapping(value = "/save", params = { "editCertificate" })
	public String editCertificate(@ModelAttribute EndpointFormData input, Model m, HttpSession s, @RequestParam("editCertificate") Long row) {
		s.setAttribute("endpointFormData", input);
		m.addAttribute("certificateFormData", ((List<CertificateFormData>) s.getAttribute("certs")).get(row.intValue()));
		m.addAttribute("certIndex", row);
		m.addAttribute("certs", s.getAttribute("certs"));
		return "admin-ui/endpoint_form";
	}

	@PostMapping(value = "/save", params = { "removeCertificate" })
	public String removeCertificate(@ModelAttribute EndpointFormData input, Model m, HttpSession s, @RequestParam("removeCertificate") Long row) {
		((List<CertificateFormData>) s.getAttribute("certs")).remove(row.intValue());
		m.addAttribute("certs", s.getAttribute("certs"));
		return "admin-ui/endpoint_form";
	}

	@PostMapping(value = "/save", params = { "saveCertificate" })
	public String saveCertificate(@ModelAttribute @Valid CertificateFormData input, BindingResult br, Model m, HttpSession s, @RequestParam("saveCertificate") Long row) {
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
					activation.toInstant(ZoneOffset.UTC).isBefore(x509Cert.getNotBefore().toInstant()))
					br.rejectValue("activationDate", "activeBeforeValid", "Activation cannot be before certificate validity period");
				if (expiration != null) {
					if (activation != null && expiration.isBefore(activation))
						br.rejectValue("expirationDate", "expiresBeforeActive", "Expiration must be after activation");
					if (expiration.toInstant(ZoneOffset.UTC).isAfter(x509Cert.getNotAfter().toInstant()))
						br.rejectValue("expirationDate", "activeAfterValid", "Expiration cannot be after certificate validity period");
				}
			} catch (CertificateException ex) {
				br.rejectValue("pemText", "invalidCert", "Provided text does not contain PEM encoded certificate");
			}
		}
		if (!br.hasErrors()) {
			input.setSubjectName(x509Cert.getSubjectX500Principal().getName());
			if (row < 0)
				((List<CertificateFormData>) s.getAttribute("certs")).add(input);
			else
				((List<CertificateFormData>) s.getAttribute("certs")).set(row.intValue(), input);
			m.addAttribute("certificateFormData", null);
		}

		m.addAttribute("endpointFormData", s.getAttribute("endpointFormData"));
		m.addAttribute("certs", s.getAttribute("certs"));
		if (row > 0)
			m.addAttribute("certIndex", row);
		return "admin-ui/endpoint_form";
	}

}
