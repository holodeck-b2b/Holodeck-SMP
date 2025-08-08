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

import java.net.URL;
import java.time.ZonedDateTime;

import org.holodeckb2b.bdxr.smp.datamodel.EndpointInfo;

/**
 * Defines the meta-data of an <i>Endpoint</i> maintained by Holodeck SMP. 
 * 
 * @author Sander Fieten (sander at holodeck-b2b.org)
 */
public interface Endpoint extends EndpointInfo, MetadataRegistration<Long> {
	
	/**
	 * Sets the URL where the Endpoint receives messages.
	 * 
	 * @param url The URL used by the Endpoint
	 */
	void setEndpointURL(URL url);

	/**
	 * Gets the Transport Profile that is used by the Endpoint to receive and respond to messages.
	 * 
	 * @returns the meta-data registration of the Transport Profile used by the Endpoint
	 */
	TransportProfile getTransportProfile();
	
	/**
	 * Sets the Transport Profile that is used by the Endpoint to receive and respond to messages. The provided argument
	 * MUST already be managed by the SMP server. 
	 * 
	 * @param transportProfile	the Transport Profile
	 */
	void setTransportProfile(TransportProfile transportProfile);

	/**
	 * Sets the date (and time) from when the Endpoint is active and can be used by other Access Points for sending
	 * messages to it. If an expiration date has been set, the activation date MUST be before it.
	 * 
	 * @param activationDate	the date from when the Endpoint is active
	 */
	void setServiceActivationDate(ZonedDateTime activationDate);

	/**
	 * Sets the date (and time) when the Endpoint will be deactivated and can no longer be used by other Access Points 
	 * for sending messages to it. If an activation date has been set, the expiration date MUST be after it.
	 * 
	 * @param expirationDate	the date from when the Endpoint isn't active anymore and can no longer be used
	 */
	void setServiceExpirationDate(ZonedDateTime expirationDate);

	/**
	 * Adds the given Certificate information to the Endpoint. The argument passed to this method only needs to 
	 * implement {@link org.holodeckb2b.bdxr.smp.datamodel.Certificate}, i.e. it may be a read-only object. This is 
	 * because the certificate is not yet managed by the SMP server.  
	 * 
	 * @param cert	the Certificate meta-data to add
	 */
	void addCertificate(org.holodeckb2b.bdxr.smp.datamodel.Certificate cert);
	
	/**
	 * Removes the given Certificate information from the Endpoint.
	 * 
	 * @param cert	a {@link Certificate} instance from the {@link #getCertificates()} collection
	 */
	void removeCertificate(Certificate cert);	
	
	/**
	 * Sets the contact information of the Endpoint.
	 * 
	 * @param contactInfo	contact information of the Endpoint
	 */
	void setContactInfo(String contactInfo);
	
	/**	
	 * Sets the description of the Endpoint.
	 * 
	 * @param description	description of the Endpoint
	 */
	void setDescription(String description);
}
