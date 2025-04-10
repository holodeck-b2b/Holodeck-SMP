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

import org.holodeckb2b.bdxr.smp.server.svc.SMLIntegrationService;
import org.holodeckb2b.bdxr.smp.server.svc.SMPCertificateService;
import org.holodeckb2b.bdxr.smp.server.ui.viewmodels.X509CertificateData;
import org.holodeckb2b.commons.security.KeystoreUtils;
import org.springframework.beans.factory.annotation.Autowired;
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
@RequestMapping("settings/smpcert")
public class SMPCertViewController {
	private static final String M_ERROR_ATTR = "keyFileError";
	private static final String S_KEYPAIR = "keypair";

	@Autowired
	protected SMLIntegrationService		smlIntegration;
	@Autowired
	protected SMPCertificateService		certService;

	@ModelAttribute("currentCert")
	public X509CertificateData populateCurrentCert() {
		try {
			KeyStore.PrivateKeyEntry keyPair = certService.getKeyPair();
			if (keyPair != null)
				return new X509CertificateData((X509Certificate) keyPair.getCertificate());
		} catch (CertificateException ex) {
			return new X509CertificateData("There was an error reading the key pair. Please provide a new key pair.");
		}
		return null;
	}

	@GetMapping(value = {"","/"})
    public String getOverview(HttpSession s) throws Exception {
		if (smlIntegration.requiresSMPCertRegistration())
			return "redirect:/settings/sml/smpcert";

		s.removeAttribute(S_KEYPAIR);
	    return "admin-ui/smpcert";
    }

	@PostMapping(value = "/upload")
	public String uploadKeypair(@RequestParam("keypair") MultipartFile kpFile, @RequestParam("password") String pwd, Model ra, HttpSession s) {

		if (kpFile.isEmpty())
			ra.addAttribute(M_ERROR_ATTR, "You must provide a file containing the key pair");
		else {
			try {
				KeyStore.PrivateKeyEntry kp = KeystoreUtils.readKeyPairFromKeystore(kpFile.getInputStream(), pwd);
				s.setAttribute(S_KEYPAIR, kp);
				ra.addAttribute("newCert", new X509CertificateData((X509Certificate) kp.getCertificate()));
			} catch (IOException ex) {
				ra.addAttribute(M_ERROR_ATTR, "An error occured reading the uploaded file");
			} catch (CertificateException ex) {
				ra.addAttribute(M_ERROR_ATTR, "Could not read the key pair from the uploaded file");
			}
		}
		return "admin-ui/smpcert";
	}

	@GetMapping("/change")
	public String changeCert(Model m, HttpSession s) {
		KeyStore.PrivateKeyEntry kp = (KeyStore.PrivateKeyEntry) s.getAttribute(S_KEYPAIR);
		if (kp != null) {
			try {
				certService.setKeyPair(kp);
			} catch (CertificateException e) {
				m.addAttribute(M_ERROR_ATTR, "An error occurred updating the SMP certificate. Try again later.");
				m.addAttribute("newCert", new X509CertificateData((X509Certificate) kp.getCertificate()));
				return "admin-ui/smpcert";
			}
		}
		return "redirect:/settings/smpcert";
	}

}
