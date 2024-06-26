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
package org.holodeckb2b.bdxr.smp.server;

import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.annotation.PropertySources;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Configures the common components of the SMP server used by both the query responder and the administration UI.
 *
 * @author Sander Fieten (sander at holodeck-b2b.org)
 */
@Configuration
@ComponentScan({"org.holodeckb2b.bdxr.smp.server.db", "org.holodeckb2b.bdxr.smp.server.svc"})
@PropertySources({
	@PropertySource("classpath:/common-defaults.properties"),
	@PropertySource(value = "file:${smp.home:.}/common.properties", ignoreResourceNotFound = true)})
@EnableAutoConfiguration
@EnableScheduling
public class CommonServerConfig {

}
