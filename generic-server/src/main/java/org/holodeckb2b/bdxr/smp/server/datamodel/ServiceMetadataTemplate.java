/*
 * Copyright (C) 2022 The Holodeck B2B Team
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
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.holodeckb2b.bdxr.smp.server.datamodel;

import java.util.Collection;
import java.util.List;
import org.holodeckb2b.bdxr.smp.datamodel.Extension;
import org.holodeckb2b.bdxr.smp.datamodel.Identifier;
import org.holodeckb2b.bdxr.smp.datamodel.ProcessGroup;

/**
 * Represents a template for <i>ServiceMetadata</i> that defines the Services, Processes and Endpoint/Redirection
 * information but is not bound to a specific Participant. As this is the same for most Participants of an AP Service
 * Provider it makes maintenance of the SMP data easier by defining the template once and then only binding it to the
 * Participants.
 *
 * @author Sander Fieten (sander at holodeck-b2b.org)
 */
public interface ServiceMetadataTemplate {

	/**
	 * Gets a (human readable) name of the ServiceMetadata template.
	 *
	 * @return descriptive name
	 */
	String getName();

	/**
     * Gets the service identifier to which these service meta-data apply
     *
     * @return The service id
     */
	Identifier	getServiceId();

	/**
	 * Gets the meta-data on the processes in which the service is supported by the participant.
	 *
	 * @return collection of {@link ProcessGroup}s
	 */
	Collection<? extends ProcessGroup> getProcessMetadata();

	/**
	 * Gets the additional, non standard, information related to the meta-data object.
	 *
	 * @return The extended meta-data
	 */
	List<Extension> getExtensions();
}
