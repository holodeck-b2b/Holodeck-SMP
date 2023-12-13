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

import org.apache.commons.logging.LogFactory;
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
		boolean queryApi = false, adminUI = false, mgmtApi = false;
		if (args.length >= 0) {			
			queryApi = adminUI = mgmtApi = false;
			for (String a : args) {
				queryApi |= "-query".equals(a);
				adminUI |= "-admin".equals(a);
				mgmtApi |= "-api".equals(a);
			}
		}	
		// If no specific server mode(s) have been specified, run all 
		if (!queryApi && !adminUI && !mgmtApi) {
			queryApi = true; 
			adminUI = true;
			mgmtApi = true;
		}
			
		SpringApplicationBuilder app = new SpringApplicationBuilder(CommonServerConfig.class)
																.properties("spring.config.name=common");
																	 
		if (queryApi)
			app = app.child(QueryAppConfig.class).web(WebApplicationType.SERVLET);
		if (adminUI) {
			if (queryApi)
				app = app.sibling(AdminUIConfig.class);
			else
				app = app.child(AdminUIConfig.class);
			app = app.web(WebApplicationType.SERVLET).bannerMode(Banner.Mode.OFF);
		}		
		if (mgmtApi) {
			try {
				Class mgmtAppClass = Class.forName("org.holodeckb2b.bdxr.smp.server.mgmtapi.MgmtAppConfig");
				if (queryApi || adminUI)
					app = app.sibling(mgmtAppClass);
				else
					app = app.child(mgmtAppClass);
				app = app.web(WebApplicationType.SERVLET).bannerMode(Banner.Mode.OFF);
			} catch (ClassNotFoundException noMgmtApi) {
				if (mgmtApi) {
					LogFactory.getLog(SMPServerApplication.class).fatal("The required management API module cannot be loaded!");
					System.exit(-1);
				}
			}					
		}
			
		app.run(args);
	}
}
