/*
 * Copyright (C) 2025 The Holodeck B2B Team
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

/**
 * This package contains the JPA entities (and embeddables) used by the Holodeck SMP server to store the meta-data in 
 * the database. The data model is defined in the {@link org.holodeckb2b.bdxr.smp.server.datamodel datamodel} package of 
 * the <code>interfaces</code> module of the project.
 * <p>
 * Each {@link org.holodeckb2b.bdxr.smp.server.datamodel.MetadataRegistration MetadataRegistration} from the data model 
 * is mapped to a JPA <code>Entity</code> in this package. These entities all extend {@link 
 * BaseMetadataRegistrationEntity} to ensure they have a primary key independent of their business identifier, which may 
 * be case insensitive, and also have a <code>lastModified</code> field which can be used for cache control.<br/>
 * With the exception of {@link org.holodeckb2b.bdxr.smp.datamodel.ProcessGroup ProcessGroup} and {@link 
 * org.holodeckb2b.bdxr.smp.datamodel.ProcessInfo ProcessInfo}, all other interfaces are mapped to <code>Embeddables</code>
 * because they are always contained in one instance of a meta-data registration. <code>ProccessGroup</code> and <code>
 * ProcessInfo</code> are mapped to <code>Entities</code> because it allows for easier querying of {@link ServiceMetadataTemplateEntity}s.    
 */
package org.holodeckb2b.bdxr.smp.server.db.entities;
