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

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

/**
 * The Spring JPA repository for the user accounts.
 *
 * @author Sander Fieten (sander at holodeck-b2b.org)
 */
public interface UserAccountRepository extends JpaRepository<UserAccount, Long> {

	/**
	 * Finds the User Account with the given email address (which acts as username).
	 * 
	 * @param username	the email address of the User to retrieve
	 * @return	{@link Optional} that contains the User Account if one exists or an empty one otherwise
	 */
	Optional<UserAccount> findByEmailAddress(String username);
	
	/**
	 * Finds the User Account that contains a password reset request with the given id.
	 * 
	 * @param requestId		id of the password reset request
	 * @return {@link Optional} that contains the User Account if one exists or an empty one otherwise
	 */
	Optional<UserAccount> findByPasswordResetRequestId(String requestId);

	/**
	 * @return the number of User accounts that have the ADMIN role.	
	 */
	@Query("""
        select count(u)
        from UserAccount u
        where org.holodeckb2b.bdxr.smp.server.ui.auth.UserRole.ADMIN member of u.roles
        """)
	long countAdmins();
}
