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

import org.holodeckb2b.bdxr.smp.server.ui.auth.FFAuthenticationHandler;
import org.holodeckb2b.bdxr.smp.server.ui.auth.MFAAuthToken;
import org.holodeckb2b.bdxr.smp.server.ui.auth.MFATrustResolver;
import org.holodeckb2b.bdxr.smp.server.ui.auth.UserRole;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.ObjectPostProcessor;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.ExceptionTranslationFilter;
import org.springframework.security.web.access.intercept.RequestAuthorizationContext;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

@Configuration
@EnableWebSecurity
public class WebSecurityConfiguration {

	/**
	 * Indicates whether MFA is required. If so, the user will be redirected to the MFA registration page after the
	 * first successful user name and password authentication.
	 */
	@Value("${smp.auth.mfa-required:false}")
	protected boolean mfaRequired;
	
    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

	@Bean
    PasswordEncoder passwordEncoder(){
        return passwordEncoder;
    }
	
	@Bean
	AuthorizationManager<RequestAuthorizationContext> getAuthorizationManager() {
		return (authentication,
				context) -> {
					Authentication auth = authentication.get();
					return new AuthorizationDecision(auth.isAuthenticated() || auth instanceof MFAAuthToken);
				};
	}
	
    @Bean
    protected SecurityFilterChain filterChain(final HttpSecurity http) throws Exception {
        http.authorizeHttpRequests((authorize) -> authorize
                .requestMatchers("/favicon.ico",
                		"/totp_logo.png",
                        "/js/**",
                        "/css/**",
                        "/img/**",
                        "/webjars/**",
                        "/reset/*",
                        "/login*"
                        ).permitAll()
                .requestMatchers("/verify/*", "/mfa/register/*").access(getAuthorizationManager())
                .requestMatchers("/settings/users/**", "/settings/smpcert/**", "/auditlog/**").hasRole(UserRole.ADMIN.name())
                .anyRequest().authenticated())
        		// The custom AuthenticationTrustResolver is used to ensure that users are redirected to the login page
        		// when they try to access a page that requires authentication before completing the 2FA 
				.exceptionHandling((exceptions) -> exceptions
						.withObjectPostProcessor(new ObjectPostProcessor<ExceptionTranslationFilter>() {
							@Override
							public <O extends ExceptionTranslationFilter> O postProcess(O filter) {
								filter.setAuthenticationTrustResolver(new MFATrustResolver());
								return filter;
							}
						}))        
                .formLogin(login -> login
                        .loginPage("/login")                        
                        .usernameParameter("email")
                        .successHandler(new FFAuthenticationHandler("/verify/totp", mfaRequired ? "/mfa/register/" : null)))
                .logout(logout -> logout
                        .invalidateHttpSession(true)
                        .clearAuthentication(true)
                        .logoutRequestMatcher(new AntPathRequestMatcher("/logout*"))
                        .logoutSuccessUrl("/login?logout"))
                .csrf(csrf -> csrf.disable())         
                .headers(headers -> headers.frameOptions(Customizer.withDefaults()).disable());
        return http.build();
	}
	
}
