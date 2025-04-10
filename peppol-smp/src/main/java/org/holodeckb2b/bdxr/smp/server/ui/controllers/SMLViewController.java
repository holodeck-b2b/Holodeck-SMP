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

import java.net.Inet4Address;
import java.net.UnknownHostException;
import java.security.cert.CertificateException;

import javax.net.ssl.SSLException;

import org.holodeckb2b.bdxr.smp.server.db.SMLRegistration;
import org.holodeckb2b.bdxr.smp.server.svc.peppol.SMLClient;
import org.holodeckb2b.commons.util.Utils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.ws.soap.client.SoapFaultClientException;

import jakarta.validation.Valid;

@Controller
@RequestMapping("settings/sml")
public class SMLViewController {

	@Autowired
	protected SMLClient smlClient;

	@GetMapping(value = {"","/"})    
    private ModelAndView getOverview(String infoMsg) {
		return createOverview(null);
	}

    private ModelAndView createOverview(String infoMsg) {
		try {
			ModelAndView mv = new ModelAndView("peppol/sml", "registration", smlClient.getSMLRegistration());
			if (!Utils.isNullOrEmpty(infoMsg))
				mv.addObject("infoMessage", infoMsg);
			return mv;
		} catch (CertificateException certIssue) {
			return new ModelAndView("peppol/unavailable", "errorTxt", certIssue.getMessage());
		}
    }

	@PostMapping("/update")
	public ModelAndView saveRegistration(@ModelAttribute("registration") @Valid SMLRegistration input, BindingResult br) {
		ModelAndView mv = new ModelAndView("peppol/sml");
		try {
			if (!input.getIpAddress().equals(Inet4Address.getByName(input.getHostname()).getHostAddress())) {
				br.rejectValue("ipAddress", "NO_MATCH", "");
				br.rejectValue("hostname", "NO_MATCH", "The hostname is not registered with the specified IP address");
			}
		} catch (UnknownHostException unknownHost) {
			br.rejectValue("hostname", "UNKNOWN_HOST", "The specified hostname is not registered in the DNS");
		}

		if (!br.hasErrors()) {
			try {
				smlClient.saveSMPRegistration(input, input.getOid() != 0);
				mv.addObject("infoMessage", "Successfully updated the registration in the SML");
			} catch (Exception smlError) {
				mv.addObject("errorMessage", "There was an error updating the registration in the SML : "
											 + smlError.getMessage());
			}
		}

		return mv;
	}

	@GetMapping("/remove")
	public ModelAndView removeRegistration(Model m) {
		try {
			smlClient.removeSMPRegistration();
			return createOverview("Successfully removed the registration from the SML");
		} catch (SoapFaultClientException smlError) {
			ModelAndView mv = createOverview(null);
			mv.addObject("errorMessage", "There was an error removing the registration from the SML : "
										+ smlError.getMessage());
			return mv;
		} catch (SSLException smpKeyIssue) {
			throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}
}
