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

/**
 * This package contains the interfaces that define the data model used by the Holodeck SMP server. The data model
 * extends the basic SMP data model defined in the <a href="https://github.com/holodeck-b2b/bdxr-common">BDXR Commons 
 * project</a>. There are additional interfaces to define maintained meta-data by the SMP server and extensions to the
 * interfaces in that project with additional fields and <code>setter</code> methods for all information elements. 
 * <p>
 * Because the meta-data of the Participants by an SMP server is typically the same for all Participants, the Holodeck 
 * SMP server's data model uses the concept of <i>Service Metadata Templates</i> which are used to define the
 * <code>ServiceMeta</code> element as specified by the OASIS SMP V2 Standard (and which can be mapped to the OASIS SMP 
 * V1 and Peppol SMP specifications) just once and then be bound to Participants. For more information see the 
 * <a href="https://holodeck-smp.org/documentation/smp-background-and-architecture/">documentation</a> on our website.
 */
package org.holodeckb2b.bdxr.smp.server.datamodel;
