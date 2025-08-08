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

import java.io.IOException;

import org.holodeckb2b.commons.util.Utils;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Handles the redirection to the next page after a successful first authentication. The next step can be the second 
 * authentication step if the user has enabled 2FA, the requested page, or the 2FA registration page if 2FA is required
 * and the user has not yet enabled 2FA.
 * 
 * @author Sander Fieten (sander at holodeck-b2b.org)
 */
public class FFAuthenticationHandler extends SavedRequestAwareAuthenticationSuccessHandler {
	
	// The URL of the second factor authentication page
	private String secondAuthUrl;
	// The URL of the second factor registration page
	private String secondFactorRegUrl;
	
	public FFAuthenticationHandler(String secondAuthUrl, String registrationUrl) {
		this.secondAuthUrl = secondAuthUrl;
		this.secondFactorRegUrl = registrationUrl;
	}

	/**
	 * Decides which page the user will be redirected to after a successful first authentication.
	 */
	@Override
	public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
										Authentication authentication) throws IOException, ServletException {

		UserAccount user = (UserAccount) authentication.getPrincipal();
		if (user.getTotpSecret() != null || !Utils.isNullOrEmpty(secondFactorRegUrl)) {
			SecurityContextHolder.getContext().setAuthentication(new MFAAuthToken(authentication));
			getRedirectStrategy().sendRedirect(request, response, user.getTotpSecret() != null ? secondAuthUrl
																							   : secondFactorRegUrl);
		} else
			super.onAuthenticationSuccess(request, response, authentication);
	}

}
