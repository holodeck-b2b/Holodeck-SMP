/*
 * Copyright (C) 2025 The Holodeck B2B Team
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
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.holodeckb2b.bdxr.smp.server.ui.auth;

import java.time.Instant;
import java.util.NoSuchElementException;

import org.bouncycastle.crypto.CryptoException;
import org.holodeckb2b.bdxr.smp.server.auditlogging.AuditLogRecord;
import org.holodeckb2b.bdxr.smp.server.auditlogging.AuditLogService;
import org.holodeckb2b.bdxr.smp.server.utils.DataEncryptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import dev.samstevens.totp.code.CodeVerifier;
import dev.samstevens.totp.code.DefaultCodeGenerator;
import dev.samstevens.totp.code.DefaultCodeVerifier;
import dev.samstevens.totp.time.SystemTimeProvider;
import lombok.extern.slf4j.Slf4j;

/**
 * Is the Spring service to verify the presented login credentials.
 * 
 * @author Sander Fieten (sander at holodeck-b2b.org)
 */
@Slf4j
@Service
public class UserLoginService {
	
	@Value("${smp.auth.lockAfter:5}")
	protected int 	 lockAfter;
	
	@Autowired
	protected UserAccountRepository	users;
	
	@Autowired
	private PasswordEncoder		pwdEncoder;
	
	@Autowired
	private DataEncryptor		encryptor;	
	
	@Autowired
	private AuditLogService		auditSvc;
	
	private CodeVerifier 		totpVerifier = new DefaultCodeVerifier(new DefaultCodeGenerator(), 
																		new SystemTimeProvider());
		
	/**
	 * Verifies the first factor login. 
	 * <p>
	 * The user is logged in when there exists an User account with entered username that is not locked and the entered
	 * password matches the stored one. If the account is locked the login immediately fails. If the entered password 
	 * does not match the stored one, the number of failed login attempts is increased and if the maximum is reached, 
	 * the account locked. The number of failed attempts is reset on a successful login.  
	 *
	 * @param username	the username entered by the user
	 * @param password	the password entered by the user
	 * @return the User when the login is successfully verified
	 * @throws UsernameNotFoundException when there exists no account with the entered username
	 * @throws LockedException when the account with the entered username is locked
	 * @throws BadCredentialsException when the entered username and password do not match to the registered account 
	 */
	public UserAccount verifyFFLogin(final String username, final String password) {
		UserAccount user;			
		try {
			user = users.findByEmailAddress(username.toLowerCase()).get();
		} catch (NoSuchElementException notFound) {
			log.warn("Login attempt for unknown user : {}", username);
			throw new UsernameNotFoundException(username);
		}		
			
		if (user.isLocked()) {
			log.warn("Login attempt for locked user account : {}", username);
			auditSvc.log(
					new AuditLogRecord(Instant.now(), username, "Failed authentication", null, "Account is locked"));
			throw new LockedException(username);
		}
			
		if (!pwdEncoder.matches(password, user.getPassword())) {
			log.warn("Failed login attempt for user account : {}", username);
			auditSvc.log(new AuditLogRecord(Instant.now(), username, "Failed authentication", null, "Failed 1FA"));

			Integer failedAttempts = user.getFailedAttempts();				
			failedAttempts = failedAttempts == null ? 1 : failedAttempts + 1;
			user.setFailedAttempts(failedAttempts);
			if (failedAttempts >= lockAfter) {
				log.warn("User account {} locked after {} failed login attempts", username, lockAfter);
				auditSvc.log(new AuditLogRecord(Instant.now(), username, "Account locked", null, 
												lockAfter + " failed 1FA attempts"));
				user.setLocked(true);
			}
			users.save(user);			
			throw new BadCredentialsException(username);
		} else {
			user.setFailedAttempts(0);
			users.save(user);
			auditSvc.log(
					new AuditLogRecord(Instant.now(), username, "Successful authentication", null, "Successful 1FA"));
			log.debug("Verified login for user {}", username);
			return user;
		}		
	}
	
	/**
	 * Verifies the TOTP based authentication attempt.
	 * 
	 * @param user	the User account being authenticated
	 * @param code  the TOTP entered by the user
	 * @throws CryptoException when the stored TOTP secret could not be decrypted
	 * @throws LockedException when the User account is locked
	 * @throws BadCredentialsException when the entered code is not valid
	 */
	public void verifyTOTPAuthentication(final UserAccount user, final String code) throws CryptoException {
		final String username = user.getUsername();
		UserAccount stored;			
		try {
			stored = users.findByEmailAddress(username).get();
		} catch (NoSuchElementException notFound) {
			log.warn("Login attempt for unknown user : {}", username);
			throw new UsernameNotFoundException(username);
		}		
		
		if (stored.isLocked()) {
			log.warn("TOTP attempted for locked user : {}", username);
			auditSvc.log(
					new AuditLogRecord(Instant.now(), username, "Failed authentication", null, "Account is locked"));
			throw new LockedException(username);
		}
		
		if (totpVerifier.isValidCode(new String(encryptor.decrypt(stored.getTotpSecret())), code)) { 
			stored.setFailed2faAttempts(0);
			auditSvc.log(
					new AuditLogRecord(Instant.now(), username, "Successful authentication", null, "Successful 2FA"));
			log.debug("Verified TOTP for user {}", username);
		} else {
			log.warn("Failed TOTP verification for user account : {}", username);
			auditSvc.log(new AuditLogRecord(Instant.now(), username, "Failed authentication", null, "Failed 2FA"));

			Integer failedAttempts = stored.getFailed2faAttempts();				
			failedAttempts = failedAttempts == null ? 1 : failedAttempts + 1;
			stored.setFailed2faAttempts(failedAttempts);
			AuthenticationException ex;
			if (failedAttempts >= lockAfter) {
				log.warn("User account {} locked after {} failed TOTP verifications", username, lockAfter);
				auditSvc.log(new AuditLogRecord(Instant.now(), username, "Account locked", null, 
												lockAfter + " failed 2FA attempts")); 						  				
				stored.setLocked(true);
				ex = new LockedException(username);
			} else {
				ex = new BadCredentialsException(username);
			}
			users.save(stored);
			throw ex;
		}		
	}	
}
