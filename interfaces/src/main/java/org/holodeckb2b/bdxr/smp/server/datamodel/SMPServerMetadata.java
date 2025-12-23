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
import java.security.cert.X509Certificate;

import org.holodeckb2b.bdxr.smp.datamodel.Certificate;
import org.holodeckb2b.bdxr.smp.server.services.core.SMPServerAdminService;
import org.holodeckb2b.bdxr.smp.server.services.network.DirectoryIntegrationService;
import org.holodeckb2b.bdxr.smp.server.services.network.SMLIntegrationService;

/**
 * Defines the meta-data of the SMP server itself, like an identifier, hostname, base URL and IP addresses. These can be
 * used for registration of the server in the SML and/or network's directory. As there currently does not exist an open 
 * standard for the SML and/or Directory Registration Service interfaces that specifies the required meta-data this 
 * interface is based on the functionality provided/required by the Peppol SML and directory. This means that is 
 * possible that not all information modeled in this interface are used by the actual implementations.  
 *  
 * @author Sander Fieten (sander at holodeck-b2b.org)
 * @see SMLIntegrationService
 * @see DirectoryIntegrationService
 */
public interface SMPServerMetadata {

	/**
	 * Get the identifier of the SMP.
	 * 
	 * @return SMP identifier
	 */
	String getSMPId();
		
	/**
	 * Sets the identifier of the SMP. When the SMP server is registered in the SML or directory the SMP identifier 
	 * cannot be changed.
	 * 
	 * @param id new SMP identifier
	 */	
	void setSMPId(String id);
		
	/**
	 * Gets the base URL that other Access Points or parties in the network should use to construct the SMP query URLs.
	 * This URL will be prefixed to the URL path specified in the SMP specification to query the meta-data. For example
	 * when the returned value is <i>https://test.holodeck-smp.org/my-test-smp</i>, the OASIS SMP V2 <code>ServiceGroup
	 * </code> query URL would be <i>https://test.holodeck-smp.org/my-test-smp/bdxr-smp-2/«ParticipantID»</i>
	 * 
	 * @return the SMP's base query URL, MUST NOT include the context path specified in the SMP specification  
	 */
	URL getBaseUrl();
	
	/**
	 * Sets the base URL that other Access Points or parties in the network should use to construct the SMP query URLs.
	 * This URL will be prefixed to the URL path specified in the SMP specification to query the meta-data. For example
	 * when the returned value is <i>https://test.holodeck-smp.org/my-test-smp</i>, the OASIS SMP V2 <code>ServiceGroup
	 * </code> query URL would be <i>https://test.holodeck-smp.org/my-test-smp/bdxr-smp-2/«ParticipantID»</i>
	 * 
	 * @param url the SMP's base query URL, MUST NOT include the context path specified in the SMP specification  
	 */
	void setBaseUrl(URL url);
	
	/**
	 * Gets the public IPv4 address used by the SMP server.  
	 * 
	 * @return	the public IPv4 address of the SMP server.
	 */
	String getIPv4Address();
	
	/**
	 * Sets the public IPv4 address used by the SMP server.  
	 * 
	 * @param addr new public IPv4 address of the SMP server.
	 */
	void setIPv4Address(String addr);
	
	/**
	 * Gets the public IPv6 address used by the SMP server.  
	 * 
	 * @return	the public IPv6 address of the SMP server.
	 */	
	String getIPv6Address();
	
	/**
	 * Sets the public IPv6 address used by the SMP server.  
	 * 
	 * @param addr new public IPv6 address of the SMP server.
	 */	
	void setIPv6Address(String addr);
	
	/**
	 * Gets the current certificate the SMP server uses to sign responses. 
	 * <p>
	 * NOTE: To update the certificate used by the SMP server the {@link SMPServerAdminService#registerCertificate()} 
	 * method must be used. 
	 *  
	 * @return	the X.509 certificate used for signing responses, <code>null</code> if no certificate has been 
	 * 			configured 
	 */
	X509Certificate getCertificate();
	
	/**
	 * Gets the pending update of the SMP's certificate. 
	 * 
	 * @return	meta-data on the pending certificate update, <code>null</code> if no update is pending
	 */
	Certificate getPendingCertificateUpdate();
}
