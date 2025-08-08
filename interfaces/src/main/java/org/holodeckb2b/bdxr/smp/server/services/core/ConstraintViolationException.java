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

import org.holodeckb2b.bdxr.smp.server.datamodel.MetadataRegistration;
import org.holodeckb2b.commons.util.Utils;

import lombok.Getter;

/**
 * Indicates that a CRUD operation on the meta-data managed by Holodeck SMP failed because a constraint on the meta-data
 * registration was violated.    
 * 
 * @author Sander Fieten (sander at holodeck-b2b.org)
 */
@Getter
public class ConstraintViolationException extends PersistenceException {	
	private static final long serialVersionUID = 1852688035579354700L;

	/**
	 * Enumerates the kind of constraint violations.
	 */
	public enum ViolationType {
		/**
		 * Indicates that an attempt was made to add a meta-data registration with a duplicate <i>Identifier</i>.
		 */
		DUPLICATE_ID,
		/**
		 * Indicates that an attempt was made to add or update a meta-data registration without a mandatory field. The
		 * exception's message contains the missing field(s) 
		 */
		MISSING_MANDATORY_FIELD,
		/**
		 * Indicates that an attempt was made to add or update a meta-data registration that contains a field with 
		 * invalid data. For example when the field references another meta-data registration that does not exist. The
		 * exception's message contains the field(s) with the invalid data.
		 */
		INVALID_FIELD,
		/**
		 * Indicates that an attempt was made to remove a meta-data registration that is still in use. For example
		 * a <i>Service</i> for which a <i>Service Metadata Template</i> still exists. The exception's message contains
		 * the information about the related meta-data registration. 
		 */
		IN_USE
	}
	
	/**
	 * Indicates the type of constraint that was violated
	 */
	private final ViolationType			violation;
	/**
	 * The meta-data registration that caused the violation
	 */
	private final MetadataRegistration<?>	subject;	
	
	public ConstraintViolationException(ViolationType v, MetadataRegistration<?> s) {
		this.violation = v;
		this.subject = s;
	}

	public ConstraintViolationException(ViolationType v, MetadataRegistration<?> s, String details) {
		super(details);
		this.violation = v;
		this.subject = s;
	}
	
	/**
	 * Generates the exception message, consisting of the violation type and the details.
	 * 
	 * @return The exception message
	 */
	@Override
	public String getMessage() {
		String details = super.getMessage();
        String msg = "[" + violation.name() + "]";  
		if (!Utils.isNullOrEmpty(details))
			msg += " " + details;
		return msg;
	}
}
