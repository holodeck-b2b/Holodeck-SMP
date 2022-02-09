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

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import lombok.extern.slf4j.Slf4j;
import org.holodeckb2b.commons.util.Utils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

/**
 * Maps a request URL to a {@link IQueryResponder} that is responsible for handling the SMP query that is represented by
 * the given URL.
 * <p>The mapping consists of pairs of regular expressions and the Spring bean name of the query responder that should
 * handle a request. The regular expression is matched against the URL path of a request without the application context
 * path (as may configured by <code>server.servlet.context-path</code>). Note that this implies the regexp should always
 * start with a "/" as the context path does not end with one.<br/>
 * It is read from a configuration file which location is specified by the <i>smp.querymap</i> property in
 * <code>query-api.properties</code> or if not specified in there from <code>querymap.conf</code> in the SMP's home
 * directory as specified by the <code>smp.home</code> system property. For each mapping the file must contain the
 * following line: <code>«regexp»;;«bean name»</code>.<br/>
 * If no mapping file is provided a default mapping will be used that supports only OASIS SMP V2 queries.
 *
 * @author Sander Fieten (sander at holodeck-b2b.org)
 */
@Service
@Slf4j
public class QueryMapper {

	@Autowired
    protected BeanFactory responderFactory;

	@Value( "${smp.querymap:${smp.home:.}/querymap.conf}" )
	protected String cfgFilePath;

	/**
	 * Contains the mapping of URL pattern to the QueryResponder responsible for answering the query represented by a
	 * URL that matches the pattern.
	 */
	private static Map<Pattern, IQueryResponder>	queryMap;

	/**
	 * Gets the {@link IQueryResponder} that should handle the request.
	 *
	 * @param urlPath	the path of part of the request URL
	 * @return	the {@link IQueryResponder} that should handle the request or <code>null</code> if no mapping is defined
	 *			for the given URL.
	 */
	public IQueryResponder getResponderFor(String urlPath) {
		if (Utils.isNullOrEmpty(queryMap))
			initMapping();

		return queryMap.entrySet().parallelStream().filter(m -> m.getKey().matcher(urlPath).matches())
												   .findFirst()
												  .map(m -> m.getValue())
												  .orElse(null);
	}

	private synchronized void initMapping() {
		if (!Utils.isNullOrEmpty(queryMap))
			return;

		queryMap = new HashMap<>();

		InputStream is = null;
		if (Files.isReadable(Path.of(cfgFilePath))) {
			log.trace("Reading query mapping from {}", cfgFilePath);
			try {
				is = new FileInputStream(cfgFilePath);
			} catch (FileNotFoundException ex) {
			}
		}
		if (is == null) {
			log.warn("Mapping file ({}) not available, using default mapping", cfgFilePath);
			try {
				 is = new ClassPathResource("/querymap-default.conf").getInputStream();
			} catch (IOException ex) {
				log.error("Default query mapping not available, server will be unable to process queries!");
				return;
			}
		}

		try (Scanner mappings = new Scanner(is)) {
			while (mappings.hasNextLine()) {
				String l = mappings.nextLine();
				String[] mapping = l.split(";;");
				if (mapping.length == 2) {
					try {
						Pattern p = Pattern.compile(mapping[0]);
						IQueryResponder r = responderFactory.getBean(mapping[1], IQueryResponder.class);
						log.info("Registering mapping {} -> {}", mapping[0], mapping[1]);
						queryMap.put(p, r);
					} catch (PatternSyntaxException invalidPattern) {
						log.error("URL regexp of mapping for {} responder {} is invalid", mapping[0], mapping[1]);
					} catch (BeansException invalidResponder) {
						log.error("Specified responder {} of mapping for URL {} is invalid", mapping[1], mapping[0]);
					}
				} else
					log.error("Query mapping configuration contains invalid mapping : {}", l);
			}
			log.debug("Registered {} query mappings", queryMap.size());
		}
	}
}
