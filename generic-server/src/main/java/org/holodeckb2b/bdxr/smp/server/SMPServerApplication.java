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
import org.springframework.boot.ResourceBanner;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.core.io.ClassPathResource;

/**
 * Is the main application class responsible for starting the both the Admin UI and Query servers.
 *
 * @author Sander Fieten (sander at holodeck-b2b.org)
 */
public class SMPServerApplication {
	
	public static boolean isMgmtAPILoaded = false;
	
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
		boolean runAll = (!queryApi && !adminUI && !mgmtApi);
		
		SpringApplicationBuilder root = new SpringApplicationBuilder(CommonServerConfig.class)
													.properties("spring.config.name=common");
		SpringApplicationBuilder app = root;
		if (queryApi || runAll)
			app = app.child(QueryAppConfig.class).web(WebApplicationType.SERVLET)
					 .banner(new ResourceBanner(new ClassPathResource("banners/query_banner.txt")));
		root.banner(new ResourceBanner(new ClassPathResource("banners/main_banner.txt")))
					.bannerMode(Banner.Mode.CONSOLE);
		if (adminUI || runAll) {
			if (queryApi || runAll)
				app = app.sibling(AdminUIConfig.class);
			else
				app = app.child(AdminUIConfig.class);
			app = app.web(WebApplicationType.SERVLET)
					 .banner(new ResourceBanner(new ClassPathResource("banners/adminui_banner.txt")));
		}		
		if (mgmtApi || runAll) {
			try {
				Class mgmtAppClass = Class.forName("org.holodeckb2b.bdxr.smp.server.mgmtapi.MgmtAppConfig");
				isMgmtAPILoaded = true;
				if (queryApi || adminUI || runAll)
					app = app.sibling(mgmtAppClass);
				else
					app = app.child(mgmtAppClass);
				app = app.web(WebApplicationType.SERVLET)
						 .banner(new ResourceBanner(new ClassPathResource("banners/mgmtapi_banner.txt")));
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
