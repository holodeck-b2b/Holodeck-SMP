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

import org.springframework.security.authentication.AuthenticationTrustResolver;
import org.springframework.security.authentication.AuthenticationTrustResolverImpl;
import org.springframework.security.core.Authentication;

/**
 * Is a custom {@link AuthenticationTrustResolver} that is used to ensure that users are redirected to the login page
 * when they try to access a resource that requires authentication before completing the 2FA. 
 * 
 * @author Sander Fieten (sander at holodeck-b2b.org)
 */
public class MFATrustResolver implements AuthenticationTrustResolver {

	private final AuthenticationTrustResolver delegate = new AuthenticationTrustResolverImpl();

	@Override
	public boolean isAnonymous(Authentication authentication) {
		return this.delegate.isAnonymous(authentication) || authentication instanceof MFAAuthToken;
	}

	@Override
	public boolean isRememberMe(Authentication authentication) {
		return this.delegate.isRememberMe(authentication);
	}
}
