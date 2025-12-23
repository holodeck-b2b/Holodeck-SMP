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

import java.util.Collection;

/**
 * Defines the <i>Service Metadata Template</i> concept used by Holodeck SMP to create a pre-defined <i>Service 
 * Metadata</i> that can be re-used by multiple <i>Participants</i>. 
 * 
 * @author Sander Fieten (sander at holodeck-b2b.org)
 */
public interface ServiceMetadataTemplate extends MetadataRegistration<Long> {	
	
	/**
	 * Gets the <i>Service</i> for which this Service Metadata Template is created.
	 * 
	 * @return	meta-data on the <i>Service</i> for which this template is created
	 */
	Service getService();
	
	/**
	 * Sets the <i>Service</i> for which this Service Metadata Template is created. The provided argument MUST already 
	 * be managed by the SMP server.
	 * 
	 * @param svc	the <i>Service</i> for which this template is created
	 */
	void setService(Service svc);
	
	/**
	 * Gets the <i>Process Groups</i> containing information on the processes for which this Service Metadata Template 
	 * is created. This includes information on the <i>Endpoints</i> or <i>Redirects</i> that can be used or must be 
	 * followed by other Access Point for accessing the <i>Service</i> (or sending Documents). 
	 * 
	 * @return	collection of {@link ProcessGroup} instances containing meta-data on the processes
	 */
	Collection<? extends ProcessGroup> getProcessMetadata();
	
	/**
	 * Adds a new <i>Process Group</i> to this Service Metadata Template. The argument passed to this method only needs
	 * to implement {@link org.holodeckb2b.bdxr.smp.datamodel.ProcessGroup}, i.e. it may be a read-only object. This is
	 * because the Process Group is not yet managed by the SMP server. 
	 * <p>
	 * NOTES:<ol>
	 * <li>The <i>Endpoints</i> and <i>Processes</i> that may be indirectly included in the Process Group MUST already 
	 * be managed by the SMP server. Therefore the result of {@link ProcessGroup#getProcessInfo()} MUST contain 
	 * instances of {@link ProcessInfo org.holodeckb2b.bdxr.smp.server.datamodel.ProcessInfo}.</li>
	 * <li>When the Process Group contains a <i>Redirection</i>, its meta-data MUST be provided using an 
	 * instance of {@link org.holodeckb2b.bdxr.smp.datamodel.RedirectionV2}.</li>
	 * </ol>
	 * 	
	 * @param pg 	the Process Group to be added
	 */
	void addProcessGroup(org.holodeckb2b.bdxr.smp.datamodel.ProcessGroup pg);
	
	/**
	 * Removes the given Process Group from this Service Metadata Template.
	 * 
	 * @param pg	a {@link ProcessGroup} instance from the {@link #getProcessMetadata()} collection
	 */
	void removeProcessGroup(ProcessGroup pg);
}
