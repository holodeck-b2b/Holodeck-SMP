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

import java.io.IOException;
import java.security.KeyStore;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.time.LocalDate;
import java.time.ZoneOffset;

import org.holodeckb2b.bdxr.smp.datamodel.Certificate;
import org.holodeckb2b.bdxr.smp.server.services.core.SMPServerAdminService;
import org.holodeckb2b.bdxr.smp.server.ui.viewmodels.PendingCertUpdateData;
import org.holodeckb2b.bdxr.smp.server.ui.viewmodels.X509CertificateData;
import org.holodeckb2b.commons.security.KeystoreUtils;
import org.holodeckb2b.commons.util.Utils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import jakarta.servlet.http.HttpSession;

@Controller
@RequestMapping("settings/cert")
public class SMPCertViewController {
	private static final String KP_ERR_ATTR = "keyPairError";
	private static final String NEW_ATTR = "newCert";
	private static final String ACT_ERR_ATTR = "activationError";

	@Autowired
	protected SMPServerAdminService		configSvc;

	@ModelAttribute("currentCert")
	public X509CertificateData populateCurrentCert() {
		X509Certificate currentCert = configSvc.getServerMetadata().getCertificate();
		return currentCert == null ? null : new X509CertificateData(currentCert);
	}
	
	@ModelAttribute("plannedUpdate")
	public PendingCertUpdateData populatePlannedUpdate() {
		Certificate pendingUpd = configSvc.getServerMetadata().getPendingCertificateUpdate();
		return pendingUpd == null ? null : new PendingCertUpdateData(new X509CertificateData(pendingUpd.getX509Cert()), 
															 pendingUpd.getActivationDate().toLocalDate());
	}
		
	@ModelAttribute("smlCertRegRequired")
	public boolean provideSMLCertRequirement() {
		return Boolean.TRUE.equals(configSvc.getNetworkServicesInfo().smlCertificateRegistrationRequired());
	}
	
	@GetMapping(value = {"","/"})
    public String getOverview() {
		return "smpcert";
    }

	@PostMapping(value = "/upload")
	public String uploadKeypair(@RequestParam("keypair") MultipartFile kpFile, @RequestParam("password") String pwd, 
								Model m, HttpSession s) {

		if (kpFile.isEmpty())
			m.addAttribute(KP_ERR_ATTR, "You must provide a file containing the key pair");
		else {
			try {
				KeyStore.PrivateKeyEntry kp = KeystoreUtils.readKeyPairFromKeystore(kpFile.getInputStream(), pwd);
				s.setAttribute(NEW_ATTR, kp);
				m.addAttribute(NEW_ATTR, new X509CertificateData((X509Certificate) kp.getCertificate()));
			} catch (IOException ex) {
				m.addAttribute(KP_ERR_ATTR, "An error occured reading the uploaded file");
			} catch (CertificateException ex) {
				m.addAttribute(KP_ERR_ATTR, "Could not read the key pair from the uploaded file");
			}
		}
		return "smpcert";
	}

	@PostMapping("/apply")
	public String changeCert(@AuthenticationPrincipal UserDetails user, 
							 @RequestParam(name = "activation", required = false) String activation,
							 @RequestParam("activateOn") String activateOn,		
							 Model m, HttpSession s) {
		KeyStore.PrivateKeyEntry kp = (KeyStore.PrivateKeyEntry) s.getAttribute(NEW_ATTR);	
		if (kp != null) {
			m.addAttribute(NEW_ATTR, new X509CertificateData((X509Certificate) kp.getCertificate()));
			m.addAttribute("activation", activation);
			LocalDate activationDate = null;
			if (Utils.isNullOrEmpty(activation))
				m.addAttribute(ACT_ERR_ATTR, "Please select when the certificate should be activated");
			else if ("scheduled".equals(activation)) {				
				if (Utils.isNullOrEmpty(activateOn)) {
					m.addAttribute(ACT_ERR_ATTR, "Please provide the date the certificate should be activated");					
				} else {
					LocalDate certStart = LocalDate.ofInstant(((X509Certificate) kp.getCertificate()).getNotBefore()
																					 .toInstant(), ZoneOffset.UTC);
					activationDate = LocalDate.parse(activateOn);
					m.addAttribute("activateOn", activationDate);
					if (activationDate.isBefore(LocalDate.now()))
						m.addAttribute(ACT_ERR_ATTR, "The activation date cannot be before the current date");
					else if (certStart.isAfter(activationDate))
						m.addAttribute(ACT_ERR_ATTR, "Activation date cannot be before start of certificate validity");
				}
			}
			if (m.getAttribute(ACT_ERR_ATTR) != null) 
				return "smpcert";
			
			try {
				if (activationDate == null) 
					configSvc.registerCertificate(user, kp);
				else
					configSvc.registerCertificate(user, kp, activationDate.atStartOfDay(ZoneOffset.UTC));				
				s.removeAttribute(NEW_ATTR);
			} catch (CertificateException certActivationError) {
				m.addAttribute(ACT_ERR_ATTR, certActivationError.getMessage());
				return "smpcert";
			} catch (SMLException smlRegistrationError) {
				m.addAttribute("errorMessage", "There was an error registering the new certificate in the SML : <p>"
						+ Utils.getRootCause(smlRegistrationError).getMessage());
				return "smpcert";
			}
		}
		return "redirect:/settings/cert";
	}

}
