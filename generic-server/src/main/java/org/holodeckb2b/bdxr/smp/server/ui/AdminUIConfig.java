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
package org.holodeckb2b.bdxr.smp.server.ui;

import org.holodeckb2b.bdxr.smp.server.SMPServerApplication;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 *
 * @author Sander Fieten (sander at holodeck-b2b.org)
 */
@Configuration
@ComponentScan("org.holodeckb2b.bdxr.smp.server.ui")
@PropertySource(value = "file:${smp.home:.}/admin-ui.properties", ignoreResourceNotFound = true)
@EnableAutoConfiguration
public class AdminUIConfig {
	private static final String DEFAULT_UID = "admin@smp.holodeck-b2b.org";
	private static final String DEFAULT_PWD = "Ch@nge1tN0w!";

	@Bean
	@Autowired
	InitializingBean initAdminUser(UserAccountRepository users) {
		return () -> {
			if (users.countAdmins() == 0) {
				Logger log = LoggerFactory.getLogger(SMPServerApplication.class);
				UserAccount admin = new UserAccount();
				admin.setEmailAddress(DEFAULT_UID);
				admin.setFullName("Default admin account");
				admin.setPassword(new BCryptPasswordEncoder().encode(DEFAULT_PWD));
				admin.getRoles().add(UserRole.ADMIN);
				try {
					users.save(admin);
					log.info("Added default admin account: ({}/{})", DEFAULT_UID, DEFAULT_PWD);
				} catch (Throwable t) {
					log.error("Could not add a default admin account. Administration UI may not be available!");
				}
			}
		};
	}
}
