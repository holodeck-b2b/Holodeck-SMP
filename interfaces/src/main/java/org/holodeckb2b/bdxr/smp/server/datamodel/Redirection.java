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

/**
 * Defines the meta-data maintained by Holodeck SMP on a <i>Redirection</i>. This is based on the {@link 
 * org.holodeckb2b.bdxr.smp.datamodel.RedirectionV2} as the <i>Subject Unique Identifier</i> used in the earlier SMP
 * specification can be derived from the certificate used in the later specs.
 * 
 * @author Sander Fieten (sander at holodeck-b2b.org)
 */
public interface Redirection extends org.holodeckb2b.bdxr.smp.datamodel.RedirectionV2 {
	
	/**
	 * Sets the URL to which query requests should be redirected. 
	 * 
	 * @param url 	the URL which should be used for query requests 
	 */
	void setNewSMPURL(URL url);
	
	/**
	 * Sets the certificate of the SMP server to which requests are redirected.
	 * 
	 * @param cert	X509.v3 certificate of the SMP server
	 */
	void setSMPCertificate(X509Certificate cert);
}
