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

import java.security.cert.X509Certificate;
import java.time.ZonedDateTime;

/**
 * Defines the meta-data of a <i>Certificate</i> maintained by Holodeck SMP. This MUST at least contain the X509.v3
 * certificate itself and may contain other information further specifying the usage of the certificate.
 * 
 * @author Sander Fieten (sander at holodeck-b2b.org)
 */
public interface Certificate extends org.holodeckb2b.bdxr.smp.datamodel.Certificate {

	/**
	 * Sets the actual X509.v3 certificate.
	 * 
	 * @param certificate 	the actual X509 certificate, SHALL NOT be <code>null</code>
	 */
	void setX509Cert(X509Certificate certificate);
	
	/**
	 * Sets the usage code of the certificate which indicates how this Certificate may be used. These usage codes are
	 * not specified in the SMP Specifications and are defined by the network in which the SMP is deployed.
	 * 
	 * @param usage		usage code indicating how the certificate may be used
	 */
	void setUsage(String usage);
	
	/**
	 * Sets the date (and time) from when the Certificate is active and can be used. This date SHOULD be the same or
	 * after the <i>not before</i> date of the certificate itself and MUST be before the <i>not after</i> date of the 
	 * certificate itself and, in case set, the expiration date.
	 * 
	 * @param activationDate	the date from when the Certificate is active	 
	 */
	void setActivationDate(ZonedDateTime activationDate);

	/**
	 * Sets the date (and time) when the Certificate will be deactivated and should no longer be used. This date should
	 * be before the <i>not after</i> date of the certificate itself and MUST be after the <i>not before</i> date of the 
	 * certificate itself and, in case set, the activation date.
	 * 
	 * @param expirationDate	the date from when the Certificate isn't active anymore and should no longer be used
	 */
	void setExpirationDate(ZonedDateTime expirationDate);
	
	/**
	 * Sets the description of the Certificate.
	 * 
	 * @param description 	description of the Certificate
	 */
	void setDescription(String description);
}
