/*
 * Copyright (C) 2023 The Holodeck B2B Team
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
import java.security.cert.CertificateExpiredException;
import java.security.cert.CertificateNotYetValidException;
import java.security.cert.X509Certificate;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.UUID;

import javax.net.ssl.SSLException;

import org.holodeckb2b.bdxr.smp.server.db.CertificateUpdate;
import org.holodeckb2b.bdxr.smp.server.db.SMLRegistration;
import org.holodeckb2b.bdxr.smp.server.svc.SMPCertificateService;
import org.holodeckb2b.bdxr.smp.server.svc.peppol.SMLClient;
import org.holodeckb2b.bdxr.smp.server.ui.viewmodels.X509CertificateData;
import org.holodeckb2b.commons.security.CertificateUtils;
import org.holodeckb2b.commons.security.KeystoreUtils;
import org.holodeckb2b.commons.util.Utils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.ws.soap.client.SoapFaultClientException;

import jakarta.servlet.http.HttpSession;

@Controller
@RequestMapping("settings/sml/smpcert")
public class CertUpdateViewController {
	private static final String M_KP_ERROR_ATTR = "keyFileError";
	private static final String M_ACTIVATION_ERROR_ATTR = "activationError";
	private static final String S_KEYPAIR = "newKeyPair";
	private static final String REVERTID = "revertId";
	
	@Autowired
	protected SMLClient smlClient;
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

	@GetMapping()
	public ModelAndView prepareCertUpdate() throws CertificateException {
		SMLRegistration smlReg = smlClient.getSMLRegistration();
		CertificateUpdate pu = smlReg != null ? smlReg.getPendingCertUpdate() : null;
		if (pu != null) {
			ModelAndView mv = new ModelAndView("peppol/crt_upd_pending", "newCert",
										new X509CertificateData((X509Certificate) pu.getKeyPair().getCertificate()));
			mv.addObject("activationDate", pu.getActivation());
			return mv;
		} else
			return new ModelAndView("peppol/cert_update");
	}

	@PostMapping(value = "/upload")
	public ModelAndView uploadKeypair(@RequestParam("keypair") MultipartFile kpFile, @RequestParam("password") String pwd,
								HttpSession s) throws CertificateException {
		ModelAndView mv = new ModelAndView("peppol/cert_update");

		if (kpFile.isEmpty())
			mv.addObject(M_KP_ERROR_ATTR, "You must provide a file containing the key pair");
		else {
			SMLRegistration smlReg = smlClient.getSMLRegistration();
			try {
				KeyStore.PrivateKeyEntry kp = KeystoreUtils.readKeyPairFromKeystore(kpFile.getInputStream(), pwd);
				X509Certificate cert = (X509Certificate) kp.getCertificate();

				String issuerName = CertificateUtils.getIssuerName(cert).toLowerCase();
				if (!issuerName.contains("peppol service metadata publisher"))
					mv.addObject(M_KP_ERROR_ATTR, "The uploaded certificate is not a Peppol SMP certificate");
				else if (("SMK".equals(smlReg.getEnvironment()) && !issuerName.contains("test"))
						|| ("SML".equals(smlReg.getEnvironment()) && issuerName.contains("test")))
					mv.addObject(M_KP_ERROR_ATTR, "The uploaded SMP certificate cannot be used with the " + smlReg.getEnvironment());
				else {
					mv.addObject("newCert", new X509CertificateData(cert));
					LocalDate certStart = LocalDate.ofInstant(((X509Certificate) kp.getCertificate()).getNotBefore()
																									 .toInstant(), ZoneOffset.UTC);
					LocalDate today = LocalDate.now();
					mv.addObject("activationDate", certStart.isAfter(today) ? certStart : today.plusDays(1));
					s.setAttribute(S_KEYPAIR, kp);
				}
			} catch (CertificateNotYetValidException | CertificateExpiredException invalid) {
				mv.addObject(M_KP_ERROR_ATTR, "The uploaded key pair is not valid on the activation date");
			} catch (IOException | CertificateException ex) {
				mv.addObject(M_KP_ERROR_ATTR, "Could not read the key pair from the uploaded file");
			}
		}
		return mv;
	}

	@PostMapping("/activate")
	public ModelAndView activateKeypair(@RequestParam("activationDate") String activation, HttpSession s) {
		ModelAndView mv = new ModelAndView("peppol/cert_update");

		KeyStore.PrivateKeyEntry kp = (KeyStore.PrivateKeyEntry) s.getAttribute(S_KEYPAIR);
		LocalDate certStart = LocalDate.ofInstant(((X509Certificate) kp.getCertificate()).getNotBefore()
																						 .toInstant(), ZoneOffset.UTC);
		LocalDate activationDate = !Utils.isNullOrEmpty(activation) ? LocalDate.parse(activation) : certStart;
		LocalDate tomorrow = LocalDate.now().plusDays(1);
		if (activationDate.isBefore(tomorrow))
			mv.addObject(M_ACTIVATION_ERROR_ATTR, "You must specify an activation date after today");
		else if (certStart.isAfter(activationDate))
			mv.addObject(M_ACTIVATION_ERROR_ATTR, "The activation date cannot be before the certificate's start date");
		else {
			try {
				smlClient.registerNewSMPCertificate(kp, activationDate);
				mv.addObject("newCert", new X509CertificateData((X509Certificate) kp.getCertificate()));
				mv.addObject("activationDate", activationDate);
				mv.setViewName("peppol/crt_upd_pending");
			} catch (SoapFaultClientException smlError) {
				mv.addObject("errorMessage", "There was an error registering the certificate update in the SML : "
											 + smlError.getMessage());
			} catch (SSLException | CertificateException ex) {
				throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR);
			}
		}
		return mv;
	}

	@GetMapping("/revert")
	public ModelAndView confirmRevertChange(HttpSession s) throws CertificateException {
		SMLRegistration registration = smlClient.getSMLRegistration();
		CertificateUpdate update = registration.getPendingCertUpdate();
		if (update == null)
			return new ModelAndView("redirect:/settings/sml/smpcert");
		else {
			final String revertId = UUID.randomUUID().toString();
			s.setAttribute(REVERTID, revertId);
			return new ModelAndView("peppol/crt_upd_revert", REVERTID, revertId);
		}
	}
	
	@PostMapping("/revert")
	public String revertChange(@RequestParam("revertId") String revertId, HttpSession s) throws CertificateException {
		SMLRegistration registration = smlClient.getSMLRegistration();
		CertificateUpdate update = registration.getPendingCertUpdate();
		if (update == null)
			return "redirect:/settings/sml/smpcert";
		
		final String expRevertId = (String) s.getAttribute(REVERTID);
		
		if (Utils.isNullOrEmpty(expRevertId) || !expRevertId.equals(revertId)) 		
			throw new ResponseStatusException(HttpStatus.PRECONDITION_FAILED);
		
		try {
			smlClient.clearPendingUpdate();
			return "redirect:/settings/sml/smpcert";
		} catch (SoapFaultClientException e) {
			throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}
}
