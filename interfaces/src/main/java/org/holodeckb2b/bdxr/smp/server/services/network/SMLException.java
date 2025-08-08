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
package org.holodeckb2b.bdxr.smp.server.services.network;

import org.holodeckb2b.bdxr.smp.server.services.core.PersistenceException;

/**
 * Indicates that an SML operation failed.
 * 
 * @author Sander Fieten (sander at holodeck-b2b.org)
 */
public class SMLException extends PersistenceException {
	private static final long serialVersionUID = 4038224633328245110L;

	public SMLException(String msg) {
		super(msg);
	}
	
	public SMLException(Throwable cause) {
		super(cause);
	}
	
	public SMLException(String msg, Throwable cause) {
		super(msg, cause);
	}
}
