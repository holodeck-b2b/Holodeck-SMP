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
 * Is a base interface to specify that each meta-data registration in the SMP instance needs to have unique identifier 
 * and descriptive name. 
 *  
 * @param <I>  the class used as identifier of the meta-data registrations
 * @author Sander Fieten (sander at holodeck-b2b.org)
 */
public interface MetadataRegistration<I> {
		
	/**
	 * Gets the unique identifier of the meta-data registration.  
	 * 
	 * @return unique identifier of the registered meta-data
	 */
	I getId();
	
	/**
	 * Sets the unique identifier of the meta-data registration. Please note that when the identifier class 
	 * <code>&ltl;I&gt;</code> is a complex type, e.g. {@link Identifier}, it may be required that the passed argument 
	 * or related objects is/are already a managed instance.  
	 * 
	 * @param id unique identifier of the registered meta-data, SHALL NOT be <code>null</code>
	 */
	void setId(I id);
	
	/**
	 * Gets the descriptive name of the registered meta-data.  
	 * 
	 * @return descriptive name of the registered meta-data
	 */
	String getName();
	
	/**
	 * Sets the descriptive name of the registered meta-data.
	 * 
	 * @param name descriptive name of the registered meta-data
	 */
	void setName(String name);
}
