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
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.holodeckb2b.bdxr.smp.server.ui.auth;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.holodeckb2b.bdxr.smp.server.NotAuthorizedException;
import org.holodeckb2b.bdxr.smp.server.auditlogging.AuditLogRecord;
import org.holodeckb2b.bdxr.smp.server.auditlogging.AuditLogService;
import org.holodeckb2b.bdxr.smp.server.utils.MailSenderUtil;
import org.holodeckb2b.commons.Pair;
import org.holodeckb2b.commons.util.Utils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.MailSendException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import jakarta.persistence.EntityNotFoundException;
import lombok.extern.slf4j.Slf4j;

/**
 * Spring service for the management of the admin UI's user accounts.
 *
 * @author Sander Fieten (sander at holodeck-b2b.org)
 */
@Service
@Slf4j
public class UserAccountMgmtService {
	
	@Autowired
	protected UserAccountRepository		users;
	@Autowired
	protected MailSenderUtil	mailer;
	@Autowired
	protected PasswordEncoder	pwdEncoder;
	@Autowired
	protected AuditLogService	auditSvc;
	
	private static final String PWD_RST_SUBJECT = "Password reset link";

	/**
	 * The base URL to include in the password reset messages 
	 */
	@Value("${smp.ui.external-url:http://localhost:${server.port:8080}}")
	private String portalUrl;
	
	/**
	 * The context path that is configured for the Admin UI
	 */
	@Value("${server.servlet.context-path:}")
	private String contextPath;
	
	/**
	 * The number of hours after which a password reset request expires.
	 */
	@Value("${smp.auth.pwd-reset-expiration:24}")
	private int pwdResetExpiration;
	
	/**
	 * Indicates whether the SMP server is connected to an email server and can send password reset links to users. If
	 * this service is not available the administrator will need to send the password reset links manually to users.
	 * 
	 * @return <code>true</code> when the email service is available, <code>false</code> otherwise
	 */
	public boolean isMailServiceAvailable() {
		return mailer.isAvailable();
	}

	/**
	 * Gets, for the specified request id, the complete password reset link and the number of hours the user can use it 
	 * to reset their password.
	 *  
	 * @param requestId	the id of the password reset request
	 * @return	a {@link Pair} containing the complete password reset link and the number of hours the link can be used
	 */	
	public Pair<String, Integer> getPwdResetLink(String requestId) {
		return new Pair<String, Integer>(portalUrl + contextPath + "/reset/" + requestId, pwdResetExpiration);
	}	
	
	/**
	 * Retrieves the information of all user accounts.
	 *
	 * @return the information of all user accounts
	 */
	public Collection<UserAccount> getAll() { 
		return users.findAll(); 
	}
	
	/**
	 * Finds the user account with the given email address.
	 *
	 * @param email	user's email address
	 * @return	the user information if an account with the specified email address exists, <code>null</code> otherwise
	 */
	public UserAccount findUserAccount(String email) throws EntityNotFoundException {
		return users.findByEmailAddress(email.toLowerCase()).orElse(null);
    }

	/**
	 * Retrieves the information of the specified user.
	 *
	 * @param admin account of the administrator requesting the user data
	 * @param oid	the id of the user account
	 * @return the user information
	 * @throws EntityNotFoundException	when the given id does not exist
	 * @throws NotAuthorizedException	when the administrator tries to retrieve data from a user working for another customer
	 */
	public UserAccount getUserInfo(UserAccount admin, Long oid) throws EntityNotFoundException, NotAuthorizedException {
		try {
			UserAccount user = users.getById(oid);
			if (!isAuthorised(admin, user)) {
				log.error("Unauthorised attempt to retrieve user info by {} for user {}", admin.getUsername(),
							user.getUsername());
				throw new NotAuthorizedException();
			}
			user.eraseCredentials();
			return user;
		} catch (EntityNotFoundException notFound) {
			log.error("Request for data of an unknown user: {}", oid.toString());
			throw notFound;
		}
	}

	/**
	 * Checks if there exists a valid password reset request with the given id.
	 *
	 * @param pwdResetReqId		the id of the password reset request
	 * @return	<code>true</code> if there exists a non expired password request with the given id,<br/>
	 * 			<code>false</code> otherwise
	 */
	public boolean isValidRequest(String pwdResetReqId) {
		UserAccount user = users.findByPasswordResetRequestId(pwdResetReqId).orElse(null);
		return user != null
				&& LocalDateTime.now().isBefore(user.getPasswordResetRequest().getTimestamp().plusHours(pwdResetExpiration));
	}

	/**
	 * Resets to locked status of the user account to unlocked.
	 *
	 * @param admin account of the administrator requesting the unlocking
	 * @param oid	the id of the user account
	 * @throws EntityNotFoundException	when the given id does not exist
	 * @throws NotAuthorizedException	when the administrator tries to unlock a user working for another customer
	 */
	public void unlockUser(UserAccount admin, Long oid) throws EntityNotFoundException, NotAuthorizedException {
		try {
			UserAccount user = users.getById(oid);
			if (!isAuthorised(admin, user)) {
				log.error("Unauthorised attempt to unlock by {} for user {}", admin.getUsername(), user.getUsername());
				throw new NotAuthorizedException();
			}
			user.setLocked(false);
			users.save(user);
			auditSvc.log(
					new AuditLogRecord(Instant.now(), admin.getUsername(), "Unlock user", user.getUsername(), null));
		} catch (EntityNotFoundException notFound) {
			log.error("Received unlock request for unknown user: {}", oid.toString());
			throw notFound;
		}
	}
	
	/**
	 * Locks the user account to prevent further login attempts.
	 *
	 * @param admin 			account of the administrator setting the locking
	 * @param accountToLock		the user account to lock
	 */
	public void lockUser(UserAccount admin, UserAccount accountToLock) {
		accountToLock.setLocked(true);
		users.save(accountToLock);
		auditSvc.log(
				new AuditLogRecord(Instant.now(), admin.getUsername(), "Lock user", accountToLock.getUsername(), null));
	}

	/**
	 * Generates a password reset link for the specified user and, if the mail service is available, sends it to the 
	 * user. If the mail service is not available the administrator will need to send the password reset link manually.
	 *
	 * @param requestor user requesting the sending of the link
	 * @param oid		the id of the user account for which reset link should be send
	 * @return the updated user information including the password reset meta-data
	 * @throws EntityNotFoundException	when the given id does not exist
	 * @throws NotAuthorizedException	when the requestor is not authorised to request the reset, for example when
	 * 								    the administrator and user work for different customers
	 * @throws LockedException 			when the user account is locked
	 * @throws MailSendException 		when there is an error sending the email
	 */
	public UserAccount requestPasswordReset(UserAccount requestor, Long oid) throws EntityNotFoundException, 
												NotAuthorizedException, LockedException, MailSendException {
		UserAccount user = null;
		try {
			user = users.getById(oid);
			if (!isAuthorised(requestor, user)) {
				log.error("Unauthorised password reset request by {} for user {}", requestor.getUsername(),
							user.getUsername());
				throw new NotAuthorizedException();
			}
		} catch (EntityNotFoundException notFound) {
			log.error("Received password reset request for unknown user: {}", oid.toString());
			throw notFound;
		}		
		if (user.isLocked()) {
			log.error("Password reset request for locked user : {}", user.getUsername());
			throw new LockedException("Cannot request password reset for locked user");
		}
		log.trace("Generate password reset request for {}", user.getEmailAddress());
		// Generate and store a new password reset request
		PasswordResetRequest pr = new PasswordResetRequest(UUID.randomUUID().toString(), LocalDateTime.now());
		user.setPasswordResetRequest(pr);
		users.save(user);
		log.debug("Password reset request generated for {}", user.getEmailAddress());		
		if (isMailServiceAvailable()) {
			try {
				log.trace("Prepare email message");
				Map<String, Object> vars = new HashMap<>(2);
				vars.put("name", user.getFullName());
				vars.put("resetLink", getPwdResetLink(pr.getId()).value1());
				vars.put("expiration", pwdResetExpiration);
				log.trace("Send email");
				mailer.sendMail(user.getEmailAddress(), PWD_RST_SUBJECT, "pwd_reset", vars);
				log.trace("Sent email");
				if (requestor.getOid() != user.getOid())
					auditSvc.log(new AuditLogRecord(Instant.now(), 
											requestor.getUsername(), "Request password reset", user.getUsername(), null));
				else
					auditSvc.log(new AuditLogRecord(Instant.now(), requestor.getUsername(), "Request password reset", null, 
													null));
			} catch (MailException mailFailure) {
				log.error("Error sending password reset email to {}. Details: {}", user.getEmailAddress(),
							Utils.getExceptionTrace(mailFailure));
				throw new MailSendException("Error sending password reset email");
			}
		} 
		return user;
	}

	/**
	 * Resets the password of the user account to which the password request with the given id belongs.
	 *
	 * @param requestId		the id of the password reset request
	 * @param newPassword	the new password
	 * @throws EntityNotFoundException	when there is no user account with a password request with the given id.
	 * @throws NotAuthorizedException when the request is for a locked user account
	 */
	public void resetPassword(final String requestId, final String newPassword) throws EntityNotFoundException, NotAuthorizedException {
		UserAccount user = users.findByPasswordResetRequestId(requestId).orElse(null);
		if (user == null) {
			log.error("Received a password reset for unknown request ({})", requestId);
			throw new EntityNotFoundException();
		}
		if (user.isLocked()) {
			log.error("Received password reset for locked account ({})", user.getEmailAddress());
			throw new NotAuthorizedException("Account is locked");
		}
		user.setPassword(pwdEncoder.encode(newPassword));
		user.setPasswordResetRequest(null);
		users.save(user);
	}

	/**
	 * Updates the information of an existing user account or create a new account for a new user.
	 *
	 * @param admin 		account performing the update, note that this can also be the user him/herself
	 * @param updatedUser	the updated data of the user
	 * @return	the updated User record
	 * @throws EntityNotFoundException	when the given data contains a user id that does not exist
	 * @throws NotAuthorizedException	when the administrator tries to update a user working for another customer
	 */
	public UserAccount updateUser(UserAccount admin, UserAccount updatedUser) 
																throws EntityNotFoundException, NotAuthorizedException {

		UserAccount user;
		boolean isNew = updatedUser.getOid() == null;
		if (!isNew) {
			// Update of an existing user, retrieve current record and check that admin is authorised to edit this user
			try {
				user = users.getById(updatedUser.getOid());
				log.trace("Update existing user account {}", user.getEmailAddress());
			} catch (EntityNotFoundException notFound) {
				log.error("Received update request for unknown user: {}", updatedUser.getOid().toString());
				throw notFound;
			}
		} else {
			log.trace("Adding a new user account");
			user = new UserAccount();
		}

		if (!isAuthorised(admin, user)) {
			log.error("Unauthorised attempt to change user info by {} for {}", admin.getUsername(),
					updatedUser.getUsername());
			throw new NotAuthorizedException();
		}

		// Copy the updated info
		user.setFullName(updatedUser.getFullName());
		user.setEmailAddress(updatedUser.getEmailAddress().toLowerCase());
		if (!Utils.isNullOrEmpty(updatedUser.getRoles()))
			user.setRoles(updatedUser.getRoles());
		else if (admin.getOid() != updatedUser.getOid())
			user.setRoles(List.of(UserRole.USER));
		if (!Utils.isNullOrEmpty(updatedUser.getPassword()))
			user.setPassword(pwdEncoder.encode(updatedUser.getPassword()));
		if (!Utils.isNullOrEmpty(updatedUser.getTotpDevice())) {
			user.setTotpDevice(updatedUser.getTotpDevice());
			user.setTotpSecret(updatedUser.getTotpSecret());
		} 
		
		log.trace("Saving updated account info ({})", user.getEmailAddress());
		user = users.save(user);
		auditSvc.log(new AuditLogRecord(Instant.now(), admin.getUsername(), isNew ? "Add user" : "Update user info", 
					updatedUser.getUsername(), String.format("Full name: %s;Pwd changed=%b;2FA device=%s;Roles: {%s};", 
										user.getFullName(), user.getEmailAddress(),
										!Utils.isNullOrEmpty(updatedUser.getPassword()), user.getTotpDevice(), 
										user.getRoles().stream().map(r -> r.name()).collect(Collectors.joining("-")))));
		
		if (isNew)
			requestPasswordReset(admin, user.getOid());

		return user;
	}

	/**
	 * Disables the two-factor authentication (2FA) for the user account.
	 * 
	 * @param admin account of the administrator disabling 2FA
	 * @param user	the user account for which 2FA is to be disabled
	 * @throws NotAuthorizedException when the administrator account is not authorised to disable 2FA for a user
	 */
	public void disable2FA(UserAccount admin, UserAccount user) throws NotAuthorizedException {
		try {
			if (!isAuthorised(admin, user)) {
				log.error("Unauthorised attempt to disable 2FA by {} for user {}", admin.getUsername(),
							user.getUsername());
				throw new NotAuthorizedException();
			}
			user.setTotpDevice(null);
			user.setTotpSecret(null);
			users.save(user);
			auditSvc.log(new AuditLogRecord(Instant.now(), admin.getUsername(), "Disable 2FA", user.getUsername(), null));
		} catch (EntityNotFoundException notFound) {
			log.error("Received disable 2FA request for unknown user: {}", user.getEmailAddress());
		}
	}
	
	/**
	 * Removes the user account.
	 *
	 * @param admin account of the administrator requesting the removal
	 * @param oid	the id of the user account
	 * @throws NotAuthorizedException	when the administrator tries to remove a user working for another customer
	 */
	public void removeUser(UserAccount admin, Long oid) throws NotAuthorizedException {
		if (admin.getOid().equals(oid)) {
			log.error("Admin {} tried to remove own account!", admin.getEmailAddress());
			throw new NotAuthorizedException();
		}

		try {
			final UserAccount user = users.getById(oid);
			if (!isAuthorised(admin, user)) {
				log.error("Unauthorised attempt to remove user by {} for user {}", admin.getUsername(),
							user.getUsername());
				throw new NotAuthorizedException();
			}
			users.delete(user);
			auditSvc.log(new AuditLogRecord(Instant.now(), 
											admin.getUsername(), "Remove user", user.getUsername(), null));
		} catch (EntityNotFoundException notFound) {
			log.warn("Received removal request for unknown user: {}", oid.toString());
		}
	}

	/**
	 * Checks if the user requesting the update of an account is authorised to perform the update.
	 *
	 * @param requestor	user requesting the update
	 * @param subject	the account being updated
	 * @return	<code>true</code> iff the requestor and subject are the same account or the requestor is a provider
	 * 			administrator or the requestor is an administor of the Customer the subject works for.
	 */
	private boolean isAuthorised(final UserAccount requestor, final UserAccount subject) {
		return requestor.getOid().equals(subject.getOid()) || requestor.hasRole(UserRole.ADMIN);
	}
}
