/*
 * Copyright (C) 2023 The Holodeck B2B Team
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
 * This package contains the definitions and implementations of the Spring JPA repositories used to manage the storage
 * of the SMP server's meta-data and audit logging in the database.
 * <p>
 * With the exception of {@link org.holodeckb2b.bdxr.smp.server.datamodel.Endpoint Endpoint} and {@link 
 * org.holodeckb2b.bdxr.smp.server.datamodel.ServiceMetadataTemplate ServiceMetadataTemplate} all meta-data 
 * registrations must have a unique business identifier. Therefore we define a {@link 
 * org.holodeckb2b.bdxr.smp.server.db.repos.UniqueIdMDRRepo generic repository interface} that requires the 
 * <code>save()</code> to check for duplicate identifiers. Also this interface defines a {@link 
 * org.holodeckb2b.bdxr.smp.server.db.repos.UniqueIdMDRRepo#findByIdentifier(Object) search method} based on the 
 * business identifier.<br/>
 * Due to the way Spring binds repository implementations, for each entity with a unique identifier two repository 
 * interfaces are needed, one that defines the identifier based duplicate check and search capability and one that adds
 * the general JPA repository operations. 
 * <p>
 * To facilitate implementation and prevent code duplication, two base implementations are used; {@link 
 * org.holodeckb2b.bdxr.smp.server.db.repos.DuplicateIdCheckRepoImpl one} that provide the <code>save()</code> method 
 * with duplicate check and {@link org.holodeckb2b.bdxr.smp.server.db.repos.IdentifierSearchRepoImpl another} one that 
 * adds {@link Identifier} based search capability. 
 */
package org.holodeckb2b.bdxr.smp.server.db.repos;

