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
 * Is the server based interface definition for <i>Process Group</i> adding setter methods for the contained meta-data.
 * 
 * @author Sander Fieten (sander at holodeck-b2b.org)
 * @see org.holodeckb2b.bdxr.smp.datamodel.ProcessGroup
 */
public interface ProcessGroup extends org.holodeckb2b.bdxr.smp.datamodel.ProcessGroup {

	/**
	 * Adds the meta-data of one process to this Process Group. Although the given <code>ProcessInfo</code> does not to
	 * be a managed object and this method will only read information from it, it still must be an instance of {@link 
	 * ProcessInfo} because the read-only version as defined in the BDXR Commmons project does not define a method to 
	 * get the related {@link Process} instance. 
	 * <p>
	 * Note that the <code>Process</code> contained in the <code>ProcessInfo</code> MUST already be managed by the SMP 
	 * server, as well as any <code>IDScheme</code>s referenced by the <code>Roles</code> part of the <code>ProcessInfo
	 * </code>. 
	 * 
	 * @param pi	the process meta-data to be added to this process group
	 */
	void addProcessInfo(ProcessInfo pi);
	
	/**
	 * Removes the given process information from the Process Group.
	 * 
	 * @param pi	a {@link ProcessInfo} instance from the {@link #getProcessInfo()} collection
	 */
	void removeProcessInfo(ProcessInfo pi);
	
	/**
	 * Adds the given Endpoint meta-data to this Process Group. The provided argument MUST already be managed by the SMP
	 * server. 
	 * <p>
	 * NOTE: A Process Group can either contain one ore more <i>Endpoints</i> or a <i>Redirection</i> but not both. 
	 * Therefore this method SHOULD only be called when no Redirection is set, i.e. <code>{@link #getRedirection()} == 
	 * null</code>. Calling this method when a Redirection is already set MAY result in an exception.
	 * 
	 * @param ep	the Endpoint to be added
	 */
	void addEndpoint(Endpoint ep);
	
	/**
	 * Removes the given Endpoint from the Process Group.
	 * 
	 * @param ep	a {@link Endpoint} instance from the {@link #getEndpoints()} collection
	 */
	void removeEndpoint(Endpoint ep);
	
	/**
	 * Sets the Redirection meta-data for this Process Group. The argument passed to this method only needs to 
	 * implement {@link org.holodeckb2b.bdxr.smp.datamodel.RedirectionV2}, i.e. it may be a read-only object. This is 
	 * because the redirection information is not yet managed by the SMP server.
	 * <p>
	 * NOTE: A Process Group can either contain one ore more <i>Endpoints</i> or a <i>Redirection</i> but not both. 
	 * Therefore this method SHOULD only be called when no Endpoints are set, i.e. <code>{@link 
	 * #getEndpoints()}.isEmpty() == true</code>. Calling this method when Endpoints have already been set MAY result in 
	 * an exception.
	 * 
	 * @param r		the Redirection to be set
	 */
	void setRedirection(org.holodeckb2b.bdxr.smp.datamodel.RedirectionV2 r);

	/**
	 * Removes the Redirection meta-data from the Process Group.
	 */
	void removeRedirection();	
}
