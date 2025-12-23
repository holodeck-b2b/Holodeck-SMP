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
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.holodeckb2b.bdxr.smp.server.ui.auth;

import java.time.LocalDateTime;

import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Contains the meta-data of a request to reset the password of a User.
 * 
 * @author Sander Fieten (sander at holodeck-b2b.org)
 */
@Embeddable
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class PasswordResetRequest {
	/**
	 * Contains the identifier of the password reset request
	 */
	private String id;
	/**
	 * Contains the time stamp when the request was done 
	 */
	private LocalDateTime	timestamp;
}
