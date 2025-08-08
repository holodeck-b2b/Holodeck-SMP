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
 * Defines the meta-data of an identifier scheme maintained by Holodeck SMP. 
 * 
 * @author Sander Fieten (sander at holodeck-b2b.org)
 */
public interface IDScheme extends org.holodeckb2b.bdxr.smp.datamodel.IDScheme, MetadataRegistration<String> {

	/**
	 * Sets the scheme identifier of the identifier scheme.
	 * 
	 * @param schemeId	the scheme identifier
	 */
	void setSchemeId(String schemeId);
	
	/**
	 * Sets the indication whether the identifiers belonging to this identifier scheme should be treated case 
	 * sensitively or not.
	 * 
	 * @param caseSensitive	<code>true</code> when the identifiers should be treated case sensitively,<br/>
	 * 						<code>false</code> when the identifiers should be treated case insensitively
	 */
	void setCaseSensitive(boolean caseSensitive);
	
	/**
	 * Gets a reference to the specification of the identifier scheme. Although the reference is probably a URI this 
	 * element is defined in the data model as a simple string so any kind of reference is allowed. 
	 * 
	 * @return	reference to the specification of the identifier scheme
	 */
	String getSchemeSpecificationRef();
	
	/**
	 * Sets the reference to the specification of the identifier scheme. It is RECOMMENDED to use a URI, but this 
	 * element is defined in the data model as a simple string so any kind of reference is allowed. 
	 * 
	 * @param schemeSpecificationRef	reference to the specification of the identifier scheme
	 */
	void setSchemeSpecificationRef(String schemeSpecificationRef);
	
	/**
	 * Gets the name of the agency that maintains the identifier scheme.
	 * 
	 * @return name of the identifier scheme's agency
	 */
	String getAgency();
	
	/**
	 * Sets the name of the agency that maintains the identifier scheme.
	 * 
	 * @param agency	name of the identifier scheme's agency
	 */
	void setAgency(String agency);
}
