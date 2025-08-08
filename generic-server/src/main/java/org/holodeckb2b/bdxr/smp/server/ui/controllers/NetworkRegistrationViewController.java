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
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.holodeckb2b.bdxr.smp.server.datamodel.NetworkServicesData;
import org.holodeckb2b.bdxr.smp.server.services.SMPServerAdminServiceImpl;
import org.holodeckb2b.bdxr.smp.server.services.SMPServerMetadataImpl;
import org.holodeckb2b.bdxr.smp.server.services.core.PersistenceException;
import org.holodeckb2b.bdxr.smp.server.services.core.SMPServerAdminService;
import org.holodeckb2b.bdxr.smp.server.services.network.DirectoryIntegrationService;
import org.holodeckb2b.bdxr.smp.server.services.network.SMLException;
import org.holodeckb2b.bdxr.smp.server.services.network.SMLIntegrationService;
import org.holodeckb2b.bdxr.smp.server.ui.auth.UserAccount;
import org.holodeckb2b.commons.util.Utils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

import jakarta.validation.Valid;

@Controller
@RequestMapping("settings/network")
public class NetworkRegistrationViewController {
	private static final String R_ATTR = "registration";

	@Value("${smp.ui.network.require_ipv4addr:true}")
	protected boolean requireIPv4;
	@Value("${smp.ui.network.validate_ipv4addr:true}")
	protected boolean validateIPv4;
	
	@Value("${smp.ui.network.require_ipv6addr:false}")
	protected boolean requireIPv6;
	@Value("${smp.ui.network.validate_ipv6addr:false}")
	protected boolean validateIPv6;
	
	@Autowired
	protected SMPServerAdminService	configSvc;

	@ModelAttribute("netInfo")
	public NetworkServicesData populateNetworkInfo() {
//		return new NetworkServicesData(true, "UITest SML", true, true, false, "DevTest Dir");		
		return configSvc.getNetworkServicesInfo();										   
	}
		
	@ModelAttribute("certRegistered")
	public boolean populateCertRegistration() {
		return configSvc.getServerMetadata().getCertificate() != null;
	}

	@ModelAttribute("smpRegistered")
	public boolean populateSMPRegistration() {
		return configSvc.isRegisteredInSML();
	}
	
	@GetMapping(value = {"","/"})    
    public ModelAndView getOverview() {
		return new ModelAndView("networkregistration", R_ATTR, configSvc.getServerMetadata());
    }

	@PostMapping("/update")
	public String saveRegistration(@AuthenticationPrincipal UserAccount user, 
								   @ModelAttribute(R_ATTR) @Valid SMPServerMetadataImpl input, BindingResult br,
								   Model m) throws PersistenceException {
		
		if (!Utils.isNullOrEmpty(input.getSMPId()) && !input.getSMPId().matches("[\\w-]+"))
			br.rejectValue("SMPId", "INVALID_FORMAT", "The identifier contains invalid characters");
		
		String ipv4 = input.getIPv4Address();
		InetAddress ip4addr = null;
		if (requireIPv4 && Utils.isNullOrEmpty(ipv4)) 
			br.rejectValue("IPv4Address", "IPV4_REQUIRED", "The external IPv4 address of the SMP must be provided");
		else if (!Utils.isNullOrEmpty(ipv4) && !ipv4.matches("\\d{1,3}.\\d{1,3}.\\d{1,3}.\\d{1,3}"))
			br.rejectValue("IPv4Address", "INVALID_FORMAT", "The format of the IPv4 address must be x.x.x.x");
		else if (!Utils.isNullOrEmpty(ipv4))
			try {
				ip4addr =  Inet4Address.getByName(ipv4);
			} catch (UnknownHostException invalidAddr) {
				br.rejectValue("IPv4Address", "INVALID", "Please provide a valid IPv4 address");				
			}
		
		String ipv6 = input.getIPv6Address();
		InetAddress ip6addr = null;
		if (requireIPv6 && Utils.isNullOrEmpty(ipv6)) 
			br.rejectValue("IPv6Address", "IPV6_REQUIRED", "The external IPv6 address of the SMP must be provided");
		else if (!Utils.isNullOrEmpty(ipv6)) 
			try {
				ip6addr =  Inet4Address.getByName(ipv6);
			} catch (UnknownHostException invalidAddr) {
				br.rejectValue("IPv6Address", "INVALID", "Please provide a valid IPv6 address");				
			}
		
		List<InetAddress> allAddrs = null;
		try {
			allAddrs = Arrays.asList(Inet6Address.getAllByName(input.getBaseUrl().getHost()));
		} catch (UnknownHostException e) {
			br.rejectValue("baseUrl", "UNKNOWN_HOST", "The specified hostname is not registered in the DNS");
		}
		
		if (ip4addr != null && validateIPv4 && !verifyHostAddr(allAddrs, ip4addr)) {
			br.rejectValue("IPv4Address", "NO_MATCH", "The specified IPv4 address is not registered in DNS for the given hostname");
			br.rejectValue("baseUrl", "NO_MATCH", "");			
		}
		if (ip6addr != null && validateIPv6 && !verifyHostAddr(allAddrs, ip6addr)) {
			br.rejectValue("IPv6Address", "NO_MATCH", "The specified IPv6 address is not registered in DNS for the given hostname");
			br.rejectValue("baseUrl", "NO_MATCH", "");
		}
				
		if (!br.hasErrors()) {
			try {				
				configSvc.updateServerMetadata(user, input);
				if (configSvc.getNetworkServicesInfo().smlServiceAvailable())
					configSvc.registerServerInSML(user);
				return "redirect:/settings/network";
			} catch (SMLException smlError) {
				m.addAttribute("errorMessage", "There was an error updating the registration in the SML : "
											 + smlError.getMessage());
			}
		}

		return "networkregistration";
	}

	private boolean verifyHostAddr(List<InetAddress> hostAddrs, InetAddress input) {
		return hostAddrs.parallelStream().anyMatch(a -> a.equals(input));
	}
	
	@GetMapping("/remove")
	public ModelAndView removeFromSML(@AuthenticationPrincipal UserAccount user) {
		ModelAndView mv = new ModelAndView("networkregistration", R_ATTR, configSvc.getServerMetadata());
		try {
			configSvc.removeServerFromSML(user);
		} catch (SMLException smlError) {
			mv.addObject("errorMessage", "There was an error removing the registration from the SML : "
										+ smlError.getMessage());
		}
		return mv;
	}
}
