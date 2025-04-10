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

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

@Configuration
@EnableWebSecurity
public class WebSecurityConfiguration {

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

	@Bean
    PasswordEncoder passwordEncoder(){
        return passwordEncoder;
    }

    @Bean
    SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
		 
        return http.authorizeHttpRequests((requests) -> requests
						.requestMatchers("/favicon.ico",
									"/js/**",
									"/css/**",
									"/img/**",
									"/webjars/**").permitAll()
						.requestMatchers("/settings/users/**", "/settings/smpcert/**").hasRole(UserRole.ADMIN.name())
						.anyRequest().authenticated())
				    .formLogin(login -> login
			            .loginPage("/login")
						.usernameParameter("email")
						.permitAll())
			        .logout(logout -> logout
			            .invalidateHttpSession(true)
			            .clearAuthentication(true)
						.logoutRequestMatcher(new AntPathRequestMatcher("/logout"))
			            .logoutSuccessUrl("/login?logout")
			            .permitAll())
			        .csrf(csrf -> csrf.disable())         
	                .headers(headers -> headers.frameOptions().disable())
			        .build();
	}
}
