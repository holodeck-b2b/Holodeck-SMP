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

import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.holodeckb2b.commons.util.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;
import org.w3c.dom.Document;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;

/**
 *
 * @author Sander Fieten (sander at holodeck-b2b.org)
 */
@RestController
@Slf4j
public class QueryController  {
	private static final Logger queryLog = LoggerFactory.getLogger("org.holodeckb2b.bdxr.smp.server.queries");

	@Autowired
	protected QueryMapper	queryMapper;

	@Value("${server.servlet.context-path:}")
	protected String contextPath;

	private static final TransformerFactory TF_FACTORY = TransformerFactory.newInstance();

	@RequestMapping(path = {"/**"}, method = RequestMethod.GET)
	public ResponseEntity<StreamingResponseBody> streamData(HttpServletRequest req, @RequestHeader HttpHeaders hdrs) {
		String queryPath = req.getRequestURI().substring(contextPath.length());

		log.trace("Get query responder for query path: {}", queryPath);
		IQueryResponder responder = queryMapper.getResponderFor(queryPath);

		QueryResponse r;
		if (responder == null) {
			log.warn("No responder available for query: {}", queryPath);
			r = new QueryResponse(HttpStatus.NOT_IMPLEMENTED, null, null);
		} else {
			log.trace("Get response from responder: {}", responder.getClass().getSimpleName());
			try {
				r = responder.processQuery(queryPath, hdrs);
			} catch (Throwable t) {
				log.error("An exception occurred handling the query: {}", Utils.getExceptionTrace(t));
				r = new QueryResponse(HttpStatus.INTERNAL_SERVER_ERROR, null, null);
			}
		}
		log.trace("Create response entity body");
		Document doc = r.response();
		StreamingResponseBody responseBody = response -> {
			if (doc != null)
				try {
					TF_FACTORY.newTransformer().transform(new DOMSource(doc), new StreamResult(response));
				} catch (TransformerException ex) {
					log.error("Could not write the response document to the HTTP entity body! Error details: {}",
								Utils.getExceptionTrace(ex));
				}
		};
		log.debug("Complete processing of query request: {}", queryPath);
		queryLog.info("{} - {}", r.status(), queryPath);

		return ResponseEntity.status(r.status())
					.headers(r.headers())
					.contentType(MediaType.APPLICATION_XML)
					.body(responseBody);
  }
}
