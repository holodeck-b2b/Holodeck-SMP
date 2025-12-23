/*
 * Copyright (C) 2023 The Holodeck B2B Team
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.holodeckb2b.bdxr.smp.server.services.core;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * Is a generic exception to indicate that a CRUD operation on the meta-data managed by Holodeck SMP failed.   
 * 
 * @author Sander Fieten (sander at holodeck-b2b.org)
 */
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PersistenceException extends Exception {	
	private static final long serialVersionUID = -420247670756243227L;

	public PersistenceException(String msg) {
		super(msg);
	}
	
	public PersistenceException(Throwable cause) {
		super(cause);
	}
	
	public PersistenceException(String msg, Throwable cause) {
		super(msg, cause);
	}
}
