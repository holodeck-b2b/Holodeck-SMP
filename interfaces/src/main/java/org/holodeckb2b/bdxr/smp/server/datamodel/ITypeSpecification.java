/*
 * Copyright (C) 2025 The Holodeck B2B Team
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
package org.holodeckb2b.bdxr.smp.server.datamodel;

/**
 * Is a base interface that specifies the information elements of a type specification, such as a specific 
 * <i>Service</i>. These always use an {@link Identifier} as id and can have a reference to their specification.
 * 
 * @author Sander Fieten (sander at holodeck-b2b.org)
 */
public interface ITypeSpecification<I extends Identifier> extends MetadataRegistration<I> {
	
	/**
	 * Gets a reference to the specification of the type identified by this identifier. Although the reference to the 
	 * specification is probably a URI this element is defined in the data model as a simple string so any kind of 
	 * reference is allowed. 
	 * 
	 * @return	reference to the specification
	 */
	String getSpecificationRef();
	
	/**
	 * Sets the reference to the specification of the type identified by this identifier. It's RECOMMENDED to use a URI,
	 * but any string is allowed.
	 * 
	 * @param ref	reference to the specification
	 */
	void setSpecificationRef(String ref);
}
