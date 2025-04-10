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
package org.holodeckb2b.bdxr.smp.server.ui.controllers;

import java.util.Optional;

import org.holodeckb2b.bdxr.smp.server.ui.UserAccount;
import org.holodeckb2b.bdxr.smp.server.ui.UserAccountRepository;
import org.holodeckb2b.bdxr.smp.server.ui.UserAccountService.UserDetails;
import org.holodeckb2b.bdxr.smp.server.ui.viewmodels.UserAccountFormData;
import org.holodeckb2b.commons.util.Utils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;

@Controller
@RequestMapping("account/update")
public class CurrentUserAccountViewController {
	private static final String U_ATTR = "account";

	@Autowired
	protected UserAccountRepository		users;

	private static final BCryptPasswordEncoder	pwdEncoder = new BCryptPasswordEncoder();

	@GetMapping
    public String editCurrentUser(HttpServletRequest req, @AuthenticationPrincipal UserDetails user,
								 HttpSession s, Model m) {
		String backURL = Optional.ofNullable(req.getHeader("Referer")).map(requestUrl -> "redirect:" + requestUrl)
																	  .orElse("/");
		s.setAttribute("backURL", backURL);
		m.addAttribute(U_ATTR, new UserAccountFormData(users.findByEmailAddress(user.getUsername()).get()));
	    return "admin-ui/currentuser_form";
    }

	@PostMapping
	public String saveCurrentUser(@ModelAttribute(U_ATTR) @Valid UserAccountFormData input, BindingResult br, HttpSession s) {
		if (!br.hasErrors()) {
			Optional<UserAccount> existing = users.findByEmailAddress(input.getEmailAddress());
			if (existing.isPresent() && existing.get().getOid() != input.getOid())
				br.rejectValue("emailAddress", "EMAIL_EXISTS", "There already exists another user with the same email address");
		}

		if (br.hasErrors())
			return "admin-ui/currentuser_form";
		else {
			UserAccount u = users.getById(input.getOid());
			u.setEmailAddress(input.getEmailAddress().toLowerCase());
			u.setFullName(input.getFullName());
			if (!Utils.isNullOrEmpty(input.getPassword()))
				u.setPassword(pwdEncoder.encode(input.getPassword()));
			users.save(u);
			return (String) s.getAttribute("backURL");
		}
	}
}
