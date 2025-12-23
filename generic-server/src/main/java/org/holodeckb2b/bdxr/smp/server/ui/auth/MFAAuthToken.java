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

import java.util.Collections;

import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.CredentialsContainer;

/**
 * Is the authentication token that is used during the multi-factor authentication process.
 * It includes a reference to the first authentication token that holds the actual identity of the user.  
 * 
 * @author Sander Fieten (sander at holodeck-b2b.org)
 */
public class MFAAuthToken extends AbstractAuthenticationToken {
	
	private final Authentication first;

	public MFAAuthToken(Authentication first) {
		super(Collections.emptyList());
		this.first = first;
	}

	@Override
	public Object getPrincipal() {
		return this.first.getPrincipal();
	}

	@Override
	public Object getCredentials() {
		return this.first.getCredentials();
	}

	@Override
	public void eraseCredentials() {
		if (this.first instanceof CredentialsContainer) {
			((CredentialsContainer) this.first).eraseCredentials();
		}
	}

	@Override
	public boolean isAuthenticated() {
		return false;
	}

	public Authentication getFirst() {
		return this.first;
	}
}
