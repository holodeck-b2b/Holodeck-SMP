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

import java.io.FileWriter;
import java.io.IOException;

import org.holodeckb2b.bdxr.smp.server.utils.DataEncryptor;
import org.holodeckb2b.commons.util.Utils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.annotation.PropertySources;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import lombok.extern.slf4j.Slf4j;

/**
 * Configures the common components of the SMP server used by both the query responder and the administration UI.
 *
 * @author Sander Fieten (sander at holodeck-b2b.org)
 */
@Slf4j
@Configuration
@ComponentScan({"org.holodeckb2b.bdxr.smp.server.db", "org.holodeckb2b.bdxr.smp.server.services",
				"org.holodeckb2b.bdxr.smp.server.utils"})
@PropertySources({
	@PropertySource("classpath:/common-defaults.properties"),
	@PropertySource(value = "file:${smp.home:.}/common.properties", ignoreResourceNotFound = true)})
@EnableAutoConfiguration
@EnableScheduling
public class CommonServerConfig {

	/**
	 * Password encoder to hash user passwords and ensure they cannot be retrieved or decoded
	 */
    private PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
	
	/**
	 * Utility class to encrypt and decrypt data using a password derived key
	 */
    private DataEncryptor dataEncryptor;
    
	/**
	 * The master password used to encrypt and decrypt data
	 */
    @Value("${smp.masterpwd:}")
    private String masterPwd;
    
    /**
     * The home directory of the SMP server, used to write the generated master password to the configuration file
     */
    @Value("${smp.home:.}")
    private String smpHome;
    
    /**
	 * @return the {@link PasswordEncoder} instance to use for hashing of passwords  
	 */
	@Bean
    PasswordEncoder passwordEncoder() {
        return passwordEncoder;
    }    
	
	@Bean
	DataEncryptor dataEncryptor() {
		if (dataEncryptor == null) 
			initDataEncryptor();
		
		return dataEncryptor;
	}

	private void initDataEncryptor() {
		if (Utils.isNullOrEmpty(masterPwd)) {
			final String mpwd  = Long.toHexString(Double.doubleToLongBits(Math.random())) 
									+ Long.toHexString(Double.doubleToLongBits(Math.random()));
			try (FileWriter fw = new FileWriter(smpHome + "/common.properties", true)) {
				fw.write("smp.masterpwd=");
				fw.write(mpwd);
				fw.write("\n");
			} catch (IOException e) {
				log.warn("Could not write the master password ({}) to the configuration file! Add it manually!", mpwd);
			}
			masterPwd = mpwd;
		}
		
		dataEncryptor = new DataEncryptor(masterPwd);		
		masterPwd = null;
	}
	
	
}
