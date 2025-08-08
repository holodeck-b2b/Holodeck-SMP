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
package org.holodeckb2b.bdxr.smp.server.services.query;

import javax.xml.crypto.dsig.XMLSignatureException;

import org.w3c.dom.Document;

/**
 * Defines the Holodeck SMP Spring Service that provides the signing function for query responses. As all SMP 
 * specifications use the same enveloped signature with the signing certificate included in the <code>KeyInfo</code> 
 * element and exclusive canonicalisation method. However the algorithms for the signing and digest functions can be
 * different, so these need to be provided as arguments when calling the signing service.
 * 
 * @author Sander Fieten (sander at holodeck-b2b.org)
 */
public interface ResponseSigningService {

	/**
	 * Signs the response XML document using an enveloped XML signature, the specified algorithms and the active SMP 
	 * certificate. The algorithms should be specified using the URI's defined in the <a 
	 * href="https://www.w3.org/TR/xmldsig-core1/">XML Signature Syntax and Processing standard</a>.
	 * 
	 * @param response		XML response document to sign
	 * @param signingAlg	signing algorithm to use 
	 * @param digestAlg		digest algorithm to use
	 * @param c14nAlg		canonicalisation algorithm to use
	 * @return	the signed XML document
	 * @throws XMLSignatureException when an error occurs signing the response document. May be caused by a missing or
	 * 								 expired SMP certificate	
	 */
	Document signResponse(Document response, String signingAlg, String digestAlg, String c14nAlg) 
																						throws XMLSignatureException;
}
