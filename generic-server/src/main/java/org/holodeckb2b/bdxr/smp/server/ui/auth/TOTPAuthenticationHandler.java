/*
 * Copyright (C) 2025 The Holodeck B2B Team
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
package org.holodeckb2b.bdxr.smp.server.ui.auth;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;


import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Handles the TOTP based second factor authentication step.
 * 
 * @author Sander Fieten (sander at holodeck-b2b.org)
 */
@Controller
@RequestMapping("/verify/totp")
public class TOTPAuthenticationHandler {

	private AuthenticationSuccessHandler successHandler = new SavedRequestAwareAuthenticationSuccessHandler();

	private AuthenticationFailureHandler failureHandler = new SimpleUrlAuthenticationFailureHandler("/verify/totp?error");

	@Autowired
	private UserLoginService loginSvc;
	
	@GetMapping
	public ModelAndView requestVerification(@AuthenticationPrincipal UserAccount user) {
		if (user.isAccountNonLocked())
			return new ModelAndView("authentication/totp_verification", "device", user.getTotpDevice());
		else
			return new ModelAndView("authentication/locked");
	}
	
	@PostMapping
	public void checkVerification(@RequestParam("code") String code, 
								  HttpServletRequest request, HttpServletResponse response) throws Exception {
		try {
			Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
			loginSvc.verifyTOTPAuthentication((UserAccount) authentication.getPrincipal(), code);
			SecurityContextHolder.getContext().setAuthentication(((MFAAuthToken) authentication).getFirst());
			successHandler.onAuthenticationSuccess(request, response, ((MFAAuthToken) authentication).getFirst());
		} catch (LockedException locked) {
			response.sendRedirect("/verify/locked");
		} catch (BadCredentialsException invalid) {
			failureHandler.onAuthenticationFailure(request, response, invalid);
		}		
	}
	
}
