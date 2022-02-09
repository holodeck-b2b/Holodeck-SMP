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
package org.holodeckb2b.bdxr.smp.server.queryapi;

import org.springframework.http.HttpHeaders;

/**
 * Defines the interface of the component responsible for creating the response to a SMP query.
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
