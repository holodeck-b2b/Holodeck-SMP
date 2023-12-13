/*
 * Copyright (C) 2023 The Holodeck B2B Team
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
package org.holodeckb2b.bdxr.smp.server.mgmtapi;

import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.annotation.PropertySources;

/**
 * Configuration for the REST management API server part.
 * 
 * @author Sander Fieten (sander at holodeck-b2b.org)
 */
@Configuration
@ComponentScan("org.holodeckb2b.bdxr.smp.server.mgmtapi")
@PropertySources({
	@PropertySource("classpath:/mgmt-api-defaults.properties"),
	@PropertySource(value = "file:${smp.home:.}/mgmt-api.properties", ignoreResourceNotFound = true)})
@EnableAutoConfiguration(exclude = { SecurityAutoConfiguration.class })
public class MgmtAppConfig {

}
