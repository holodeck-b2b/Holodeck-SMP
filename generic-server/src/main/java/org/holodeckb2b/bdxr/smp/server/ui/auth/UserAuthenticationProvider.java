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

import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Component;

/**
 * Handles the verification of the first factor authentication. 
 * 
 * @author Sander Fieten (sander at holodeck-b2b.org)
 */
@Component
public class UserAuthenticationProvider implements AuthenticationProvider {

	private UserLoginService	loginSvc;
	
	public UserAuthenticationProvider(final UserLoginService loginSvc) {
		this.loginSvc = loginSvc;
	}
	
	@Override
	public Authentication authenticate(Authentication authentication) throws AuthenticationException {
		final String name = authentication.getName();
        final String password = authentication.getCredentials().toString();
        
        UserAccount user = loginSvc.verifyFFLogin(name, password);

        return new UsernamePasswordAuthenticationToken(user, password, user.getAuthorities()); 	
	}

	@Override
	public boolean supports(Class<?> authentication) {
		return authentication.equals(UsernamePasswordAuthenticationToken.class);
	}

}
