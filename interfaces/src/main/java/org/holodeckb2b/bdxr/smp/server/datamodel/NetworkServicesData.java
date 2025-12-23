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
package org.holodeckb2b.bdxr.smp.server.datamodel;

/**
 * Is a record that holds information about the network service the SMP Server is (or is not) connected to.  
 *
 * @author Sander Fieten (sander at holodeck-b2b.org)
 */
public record NetworkServicesData(
	/**
	 * Indicates whether the SMP server is connected to a network SML service
	 */
	boolean smlServiceAvailable,
	/**
	 * The descriptive name of the network's SML service. Again the value of this element only has meaning when 
	 * {@link #smlServiceAvailable} is <code>true</code>.
	 */
	String smlName,
	/**
	 * Indicates whether the SMP server must be registered in the network's SML service.
	 */
	Boolean smlRegistrationRequired,
	/**
	 * Indicates whether the SMP server must also register its certificate in the network's SML service. The value
	 * of this indicator only has meaning when {@link #smlServiceAvailable} is <code>true</code>.
	 */
	Boolean smlCertificateRegistrationRequired,
	/**
	 * Indicates whether the SMP server is connected to a network Directory service
	 */
	boolean directoryServiceAvailable,
	/**
	 * The name of the network's Directory service. Will be <code>null</code> if the SMP server is not connected to a
	 * directory service.
	 */
	String directoryName) 
{}