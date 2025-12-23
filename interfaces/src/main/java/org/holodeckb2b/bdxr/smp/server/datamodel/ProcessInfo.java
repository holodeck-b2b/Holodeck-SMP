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
 * Is the server based interface definition for <i>Process Info</i> adding setter methods for the contained meta-data 
 * and a direct reference to the managed Process meta-data registration.
 * 
 * @author Sander Fieten (sander at holodeck-b2b.org)
 * @see org.holodeckb2b.bdxr.smp.datamodel.ProcessInfo
 */
public interface ProcessInfo extends org.holodeckb2b.bdxr.smp.datamodel.ProcessInfo {

	/**
	 * Gets the meta-data on the Process.
	 * 
	 * @return	the meta-data on the Process contained in this Process Information element 
	 */
	Process getProcess();
	
	/**
	 * Sets the Process for this Process Information element. The provided argument MUST already be managed by the SMP
	 * server. 
	 * 
	 * @param process	the Process this Process Information element applies to
	 */
	void setProcess(Process process);
	
	/**
	 * Adds the Identifier of a Role the Participant acts in. The argument passed to this method only needs to implement 
	 * {@link org.holodeckb2b.bdxr.common.datamodel.Identifier}, i.e. it may be a read-only object. This is because the 
	 * identifier is not yet managed by the SMP server. However if the Identifier has an identifier scheme, the scheme 
	 * MUST be already registered in the SMP server.
	 * 
	 * @param roleId	identifier of the Role. If <code>Identifier#getScheme()} != null</code> the returned scheme MUST
	 * 					be a <b>managed instance</b> of {@link IDScheme}  
	 */
	void addRole(org.holodeckb2b.bdxr.common.datamodel.Identifier roleId);
	
	/**
	 * Removes the Role with the given identifier.
	 *  
	 * @param roleId	a {@link Identifier} instance from the {@link #getRoles()} collection
	 */
	void removeRole(Identifier roleId);
}
