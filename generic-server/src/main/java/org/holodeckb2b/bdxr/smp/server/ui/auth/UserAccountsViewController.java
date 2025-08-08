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
package org.holodeckb2b.bdxr.smp.server.ui.auth;

import java.util.Collection;
import java.util.Optional;

import org.bouncycastle.crypto.CryptoException;
import org.holodeckb2b.bdxr.smp.server.NotAuthorizedException;
import org.holodeckb2b.bdxr.smp.server.utils.DataEncryptor;
import org.holodeckb2b.commons.Pair;
import org.holodeckb2b.commons.util.Utils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.MailSendException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;

/**
 * Handles the pages for managing user accounts.
 * 
 * @author Sander Fieten (sander at holodeck-b2b.org)
 */
@Controller
@Slf4j
public class UserAccountsViewController {
	private static final String USR_ATTR = "user";
	private static final String RETURN_TO_ATTR = "returnTo";

	@Autowired
	private DataEncryptor	encryptor;
	
	@Autowired
	protected UserAccountMgmtService	userMgmtSvc;
	
	@ModelAttribute("mailEnabled")
	public boolean mailEnabled() {
		return userMgmtSvc.isMailServiceAvailable();
	}

	@GetMapping(value = {"settings/users", "settings/users/"})
    public String getOverview(Model m) {
		Collection<UserAccount> allUsers = userMgmtSvc.getAll();
		m.addAttribute("accounts", allUsers);
		m.addAttribute("numAdm", allUsers.stream().filter(u -> u.hasRole(UserRole.ADMIN)).count());
	    return "authentication/users";
    }

	@GetMapping(value = {"settings/users/edit", "settings/users/edit/{id}" })
	public String editAccount(@AuthenticationPrincipal UserAccount admin, 
							  @PathVariable(name = "id", required = false) Long id, Model m) throws EntityNotFoundException, NotAuthorizedException {		
		m.addAttribute(USR_ATTR, id != null ? userMgmtSvc.getUserInfo(admin, id) : new UserAccount());
		return "authentication/edit_user";
	}
	
	@ResponseBody
	@GetMapping("settings/users/requestpwdreset")
	public String requestPwdReset(@AuthenticationPrincipal UserAccount admin, @RequestParam("uid") Long oid) throws Exception {
		Pair<String, Integer> linkInfo = 
			userMgmtSvc.getPwdResetLink(userMgmtSvc.requestPasswordReset(admin, oid).getPasswordResetRequest().getId());
		return linkInfo.value1() + "," + linkInfo.value2(); 
	}
	
	@PostMapping("settings/users/update")
	public String saveAccount(@AuthenticationPrincipal UserAccount admUser, Model m,
							  @RequestParam(name = "has2FA", defaultValue = "false") boolean has2FA,
							  @ModelAttribute(USR_ATTR) @Valid UserAccount input, BindingResult br) 
									  						throws EntityNotFoundException, NotAuthorizedException {
		UserAccount existing = null;
		if (!br.hasErrors()) {
			existing = userMgmtSvc.findUserAccount(input.getEmailAddress());
			if (existing != null && existing.getOid() != input.getOid())
				br.rejectValue("emailAddress", "EMAIL_EXISTS", "There already exists another user account with the same email address");
		}

		if (br.hasErrors()) {
			input.setTotpSecret(existing != null ? existing.getTotpSecret() : null);
			input.setTotpDevice(existing != null ? existing.getTotpDevice() : null);
			return "authentication/edit_user";
		} else {
			UserAccount updated = userMgmtSvc.updateUser(admUser, input);
			if ((existing == null) && !userMgmtSvc.isMailServiceAvailable()) {
				Pair<String, Integer> resetLinkInfo = 
						userMgmtSvc.getPwdResetLink(updated.getPasswordResetRequest().getId());
				m.addAttribute("resetLink", resetLinkInfo.value1());
				m.addAttribute("linkExpires", resetLinkInfo.value2());
				m.addAttribute(USR_ATTR, updated);
				return "authentication/edit_user";
			} else if (existing != null) {
				if (existing.getTotpSecret() != null && !has2FA) 
					userMgmtSvc.disable2FA(admUser, updated);
				if (existing.isLocked() && !input.isLocked())
					userMgmtSvc.unlockUser(admUser, updated.getOid());
				else if (!existing.isLocked() && input.isLocked())
					userMgmtSvc.lockUser(admUser, updated);					
			}
			return "redirect:/settings/users";
		}
	}

	@GetMapping(value = "settings/users/delete/{id}")
	public String removeAccount(@AuthenticationPrincipal UserAccount admin, @PathVariable("id") Long id) throws NotAuthorizedException {
		userMgmtSvc.removeUser(admin, id);
		return "redirect:/settings/users";
	}
	
	@GetMapping("account/update")
    public String editCurrentUser(HttpServletRequest req, @AuthenticationPrincipal UserAccount user, Model m) throws EntityNotFoundException, NotAuthorizedException {
		String backURL = Optional.ofNullable(req.getHeader("Referer")).orElse("/");
		m.addAttribute(RETURN_TO_ATTR, backURL);
		m.addAttribute(USR_ATTR, userMgmtSvc.getUserInfo(user, user.getOid()));
	    return "authentication/edit_self";
    }

	@PostMapping("account/save")
	public String saveCurrentUser(@AuthenticationPrincipal UserAccount user, HttpSession s,
				@ModelAttribute(RETURN_TO_ATTR) String returnTo, 
				@ModelAttribute("passwordConfirm") String passwordConfirm, 
				@ModelAttribute("newTotpDevice") String newTotpDevice,
				@ModelAttribute(USR_ATTR) @Valid UserAccount input,
				BindingResult br) throws EntityNotFoundException, NotAuthorizedException, CryptoException {
		
		UserAccount existing = null;
		if (!br.hasErrors()) {
			existing = userMgmtSvc.findUserAccount(input.getEmailAddress());
			if (existing != null && existing.getOid() != input.getOid())
				br.rejectValue("emailAddress", "EMAIL_EXISTS", "There already exists another user account with the same email address");
			if (!Utils.isNullOrEmpty(input.getPassword()) && !Utils.nullSafeEqual(input.getPassword(), passwordConfirm))
				br.rejectValue("password", "PWD_MISMATCH", "Entered passwords do not match");
		}

		if (br.hasErrors()) {
			input.setTotpSecret(existing != null ? existing.getTotpSecret() : null);
			input.setTotpDevice(existing != null ? existing.getTotpDevice() : null);
			return "authentication/edit_self";
		} else {
			String totpSecret = (String) s.getAttribute("totp-secret");
			if (!Utils.isNullOrEmpty(totpSecret)) {
				try {
					input.setTotpSecret(encryptor.encrypt(totpSecret.getBytes()));
					input.setTotpDevice(newTotpDevice);
				} catch (CryptoException encFailure) {
					log.error("Could not encrypt the TOTP secret key : {}", encFailure.getMessage());
					throw encFailure;
				}
			}
			userMgmtSvc.updateUser(user, input);
			
			return "redirect:" + returnTo;
		}
	}
}
