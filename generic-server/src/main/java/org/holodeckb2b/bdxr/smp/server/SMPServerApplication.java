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

import org.holodeckb2b.bdxr.smp.server.queryapi.QueryAppConfig;
import org.holodeckb2b.bdxr.smp.server.ui.AdminUIConfig;
import org.springframework.boot.Banner;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;

/**
 * Is the main application class responsible for starting the both the Admin UI and Query servers.
 *
 * @author Sander Fieten (sander at holodeck-b2b.org)
 */
public class SMPServerApplication {

	public static void main(String[] args) {
		new SpringApplicationBuilder().properties("spring.config.name=common")
									.sources(CommonServerConfig.class)
						            .child(QueryAppConfig.class)
						            .web(WebApplicationType.SERVLET)
									.sibling(AdminUIConfig.class)
									.web(WebApplicationType.SERVLET)
									.bannerMode(Banner.Mode.OFF)
									.run(args);
	}
}
