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

import org.springframework.http.HttpHeaders;

/**
 * Defines the interface of a Spring bean that can respond to a query, i.e. is able to the response to the query. The 
 * input to the query responder is the <i>"query path"</i> and the HTTP headers of the request. The query path is 
 * the request URL without the servlet context that may have been configured in the instance's server configuration.
 * <p>
 * Which query responder is used for handling a request is managed by the Core's <code>QueryMapper</code> component. It 
 * uses regexp based matching of the request URL to the Spring bean name of the query responder.    
 *
 * @author Sander Fieten (sander at holodeck-b2b.org)
 */
public interface IQueryResponder {

	/**
	 * Processes the query defined by the given URL path and returns the response to provide to the client.
	 *
	 * @param queryPath	path part of the URL without the servlet context path
	 * @param headers	the HTTP request headers
	 * @return	the response to provide to the client, consisting of the HTTP status and code and the response XML
	 */
	QueryResponse processQuery(final String queryPath, final HttpHeaders headers);
}
