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
package org.holodeckb2b.bdxr.smp.server.ui;

import java.util.Collection;
import java.util.Set;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/**
 * Is the Spring service to provide the user account data to the Spring framework for authentication and authorisation.
 *
 * @author Sander Fieten (sander at holodeck-b2b.org)
 */
@Service
public class UserAccountService implements UserDetailsService {

	@Autowired
	protected UserAccountRepository		users;

	@Override
	public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
		UserAccount user = users.findByEmailAddress(username.toLowerCase()).orElse(null);
        if (user == null) {
            throw new UsernameNotFoundException("Invalid email address or password.");
        }
        return new UserDetails(user);
    }

    private Collection<? extends GrantedAuthority> mapRolesToAuthorities(Set<UserRole> roles) {
        return roles.stream().map(r -> new SimpleGrantedAuthority("ROLE_" + r.name())).toList();
    }

	/**
	 * Extends the default Spring <i>user details</i> with the full name of the logged in user.
	 */
	public class UserDetails extends User {
		private String	fullName;

		UserDetails(UserAccount user) {
			super(user.getEmailAddress(), user.getPassword(), mapRolesToAuthorities(user.getRoles()));
			this.fullName = user.getFullName();
		}

		public String getFullName() {
			return fullName;
		}
	}
}
