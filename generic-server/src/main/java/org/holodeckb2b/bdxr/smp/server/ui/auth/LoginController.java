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
package org.holodeckb2b.bdxr.smp.server.ui.auth;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

/**
 * Handles the first pages of the login process which includes presenting the login page and handling of forgotten
 * passwords and password resets.
 * 
 * @author Sander Fieten (sander at holodeck-b2b.org)
 */
@Controller
public class LoginController {

	@Autowired
	protected UserAccountMgmtService userMgmtSvc;
	
	@GetMapping("login")
	public String getLogin(Authentication authToken) {
		/*
		 * If user is already authenticated, go to the User's default page. 
		 * Otherwise go to the login page, or if initial login has been completed the 2FA verification
		 */
		if (authToken != null && !(authToken instanceof MFAAuthToken)) {
			return "redirect:/";
		} else if (authToken == null)
			return "authentication/login";
		else
			return "redirect:/verify/totp";
	}

	@GetMapping("verify/locked")
	public String getLockedOut() {
		return "authentication/locked";
	}

	@GetMapping("reset/forgotten")
	public String getForgotten() {
		return userMgmtSvc.isMailServiceAvailable() ? "authentication/req_rst_by_email" 
													: "authentication/req_rst_by_admin";
	}

	@PostMapping("reset/forgotten")
	public ModelAndView sendResetLink(@RequestParam("emailAddress") String email) {
		try {
			UserAccount user = userMgmtSvc.findUserAccount(email);
			if (user != null)
				userMgmtSvc.requestPasswordReset(user, user.getOid());
		} catch (Exception e) {
			// We don't care about errors here as the error has already been logged and we won't provide info on this 
			// to the user
		}
		return new ModelAndView("authentication/reset_sent");
	}

	@GetMapping("reset/{id}")
	public ModelAndView getPasswordReset(@PathVariable("id") String requestId) {
		if (userMgmtSvc.isValidRequest(requestId))
			return new ModelAndView("authentication/pwd_reset", "requestId", requestId);
		else
			return new ModelAndView("authentication/reset_unavailable");
	}

	@PostMapping("reset/{requestId}")
	public ModelAndView resetPassword(@PathVariable("requestId") String requestId,
			@RequestParam("password") String password) {
		try {
			userMgmtSvc.resetPassword(requestId, password);
			return new ModelAndView("authentication/reset_done");
		} catch (Exception resetFailure) {
			return new ModelAndView("authentication/reset_unavailable");
		}
	}

}
