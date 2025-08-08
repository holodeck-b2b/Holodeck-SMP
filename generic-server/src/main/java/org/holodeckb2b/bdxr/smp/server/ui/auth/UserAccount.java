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
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.holodeckb2b.bdxr.smp.server.ui.auth;

import java.util.ArrayList;
import java.util.Collection;

import org.holodeckb2b.commons.util.Utils;
import org.springframework.security.core.CredentialsContainer;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Is the JPA entity object representing a User of the SMP server's administration UI.
 *
 * @author Sander Fieten (sander at holodeck-b2b.org)
 */
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Setter
@Getter
@EqualsAndHashCode
public class UserAccount implements UserDetails, CredentialsContainer {
	private static final long serialVersionUID = 2973178004601378341L;

	@Id
	@GeneratedValue
	private Long oid;

	/**
	 * Hash of the user's password.
	 */
	@Column
	private String password;
	/**
	 * The secret for the TOTP based second authentication factor 
	 */
	@Column
	private byte[] totpSecret;
	/**
	 * The name of the TOTP device used by the User 
	 */
	@Column
	private String	totpDevice;
	/**
	 * The roles assigned to the user. Each user much have been assigned at least one role.
	 */
	@ElementCollection(fetch = FetchType.EAGER)
	private Collection<UserRole>	roles = new ArrayList<>();
	/**
	 * The email address of this user. Also used to identify the user.
	 */
	@Email
	@NotBlank(message = "An email address must be provided")
	@Column(nullable = false, unique = true)
	private String emailAddress;
	/**
	 * Name of the user.
	 */
	@NotBlank(message = "The user's name must be provided")
	@Column(nullable = false)
	private String fullName;
	/**
	 * The number of consecutive failed login attempts
	 */
	@Column
	private Integer failedAttempts;	
	/**
	 * The number of consecutive failed 2FA verification attempts
	 */
	@Column
	private Integer failed2faAttempts;	
	/**
	 * Indicates whether the account is locked because there were too many login attempts using an incorrect password.
	 */
	@Column
	private boolean locked;
	/**
	 * Contains the password reset when it has been requested for this user
	 */
	@Embedded
	private PasswordResetRequest	passwordResetRequest;
	
	@Override
	public void eraseCredentials() {
		password = null;
	}
	
	public void setRoles(Collection<UserRole> roles) {
		this.roles = !Utils.isNullOrEmpty(roles) ? new ArrayList<>(roles) : new ArrayList<>(); 
	}	

	@Override
	public Collection<? extends GrantedAuthority> getAuthorities() {
		return roles.stream().map(r -> new SimpleGrantedAuthority("ROLE_" + r.name())).toList();
	}

	@Override
	public String getUsername() {
		return emailAddress;
	}

	@Override
	public boolean isAccountNonExpired() {
		return true;
	}

	@Override
	public boolean isAccountNonLocked() {
		return !locked;
	}

	@Override
	public boolean isCredentialsNonExpired() {
		return true;
	}

	@Override
	public boolean isEnabled() {
		return true;
	}

	/**
	 * Checks if this User has been assigned the requested role.
	 *
	 * @param r		Role to check for assignment
	 * @return		<code>true</code> iff the user has been assigned the requested role, <code>false</code> otherwise
	 */
	public boolean hasRole(UserRole r) {
		return !this.roles.isEmpty() && this.roles.contains(r);
	}
}
