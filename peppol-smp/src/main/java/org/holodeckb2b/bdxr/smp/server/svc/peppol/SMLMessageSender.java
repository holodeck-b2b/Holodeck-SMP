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
package org.holodeckb2b.bdxr.smp.server.svc.peppol;

import java.io.IOException;
import java.net.URI;
import java.util.Iterator;

import org.apache.hc.client5.http.classic.HttpClient;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.core5.http.protocol.HttpContext;
import org.springframework.ws.transport.WebServiceConnection;
import org.springframework.ws.transport.http.HttpComponents5Connection;
import org.springframework.ws.transport.http.HttpComponents5MessageSender;
import org.springframework.ws.transport.http.HttpTransportConstants;

/**
 * Is a customised implementation of {@link HttpComponentsConnection} to work around a bug in the Peppol SML
 * implementation as it returns HTTP status code 4xx instead of the required 500 when the response is a SOAP Fault.
 * Because the Spring web service framework classes expect the 500 code for SOAP 1.1 this class converts a received 4xx
 * code into a 500 when the response contains a SOAP message.
 *
 * @author Sander Fieten (sander at holodeck-b2b.org)
 */
public class SMLMessageSender extends HttpComponents5MessageSender {

	public SMLMessageSender(HttpClient httpClient) {
		super(httpClient);
	}

	@Override
	public WebServiceConnection createConnection(URI uri) throws IOException {
		HttpPost httpPost = new HttpPost(uri);
		if (isAcceptGzipEncoding()) {
			httpPost.addHeader(HttpTransportConstants.HEADER_ACCEPT_ENCODING, HttpTransportConstants.CONTENT_ENCODING_GZIP);
		}
		HttpContext httpContext = createContext(uri);
		return new SMLConnection(getHttpClient(), httpPost, httpContext);
	}

	private class SMLConnection extends HttpComponents5Connection {

		SMLConnection(HttpClient httpClient, HttpPost httpPost, HttpContext httpContext) {
			super(httpClient, httpPost, httpContext);
		}

		/**
		 * Checks if the response code is 4xx and the response contains a SOAP message and if so converts it into 500.
		 *
		 * @return	500 iff the SML server returned a SOAP message with status code 4xx, otherwise the original status code
		 */
		@Override
		protected int getResponseCode() throws IOException {
			int sc = super.getResponseCode();
			return (sc / 100 == 4 && isSoap11Response()) ? 500 : sc;
		}

		/** Determine whether the response is a SOAP 1.1 message. */
		private boolean isSoap11Response() throws IOException {
			Iterator<String> iterator = getResponseHeaders(HttpTransportConstants.HEADER_CONTENT_TYPE);
			if (iterator.hasNext()) {
				String contentType = iterator.next().toLowerCase();
				return contentType.contains("text/xml");
			}
			return false;
		}
	}
}
