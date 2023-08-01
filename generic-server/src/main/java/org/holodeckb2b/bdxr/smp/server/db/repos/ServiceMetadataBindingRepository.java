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
package org.holodeckb2b.bdxr.smp.server.db.repos;

import org.holodeckb2b.bdxr.smp.server.db.entities.ServiceMetadataBindingE;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * The Spring JPA repository for service meta-data bindings.
 *
 * @author Sander Fieten (sander at holodeck-b2b.org)
 */
public interface ServiceMetadataBindingRepository extends JpaRepository<ServiceMetadataBindingE, Long>, SMBSchemeBasedQueryingRepository {
}
