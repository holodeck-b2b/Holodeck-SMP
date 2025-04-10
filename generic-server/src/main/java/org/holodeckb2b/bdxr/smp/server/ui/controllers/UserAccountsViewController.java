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
import org.holodeckb2b.bdxr.smp.server.ui.UserRole;
import org.holodeckb2b.bdxr.smp.server.ui.viewmodels.UserAccountFormData;
import org.holodeckb2b.commons.util.Utils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import jakarta.validation.Valid;

@Controller
@RequestMapping("settings/users")
public class UserAccountsViewController {
	private static final String U_ATTR = "account";

	@Autowired
	protected UserAccountRepository		users;

	private static final BCryptPasswordEncoder	pwdEncoder = new BCryptPasswordEncoder();

	@GetMapping(value = {"","/"})
    public String getOverview(Model m) {
		m.addAttribute("accounts", users.findAll());
		m.addAttribute("numAdm", users.countAdmins());
	    return "admin-ui/users";
    }

	@GetMapping(value = { "/edit", "/edit/{id}" })
	public String editAccount(@PathVariable(name = "id", required = false) Long id, Model m) {
		m.addAttribute(U_ATTR, new UserAccountFormData(id != null ? users.getById(id) : null));
		return "admin-ui/useraccount_form";
	}

	@PostMapping(value = "/update")
	public String saveAccount(@ModelAttribute(U_ATTR) @Valid UserAccountFormData input, BindingResult br) {
		if (!br.hasErrors()) {
			Optional<UserAccount> existing = users.findByEmailAddress(input.getEmailAddress());
			if (input.getOid() != null && existing.isPresent() && existing.get().getOid() != input.getOid())
				br.rejectValue("emailAddress", "EMAIL_EXISTS", "There already exists another user with the same email address");
			if (input.getOid() == null && Utils.isNullOrEmpty(input.getPassword()))
				br.rejectValue("password", "MISSING_PWD", "A password must be provided for the user");
		}

		if (br.hasErrors())
			return "admin-ui/useraccount_form";
		else {
			UserAccount u = input.getOid() != null ? users.getById(input.getOid()) : new UserAccount();
			u.setEmailAddress(input.getEmailAddress().toLowerCase());
			u.setFullName(input.getFullName());
			if (input.isAdmin())
				u.getRoles().add(UserRole.ADMIN);
			else
				u.getRoles().remove(UserRole.ADMIN);
			if (!Utils.isNullOrEmpty(input.getPassword()))
				u.setPassword(pwdEncoder.encode(input.getPassword()));
			users.save(u);
			return "redirect:/settings/users";
		}
	}

	@GetMapping(value = "/delete/{id}")
	public String removeAccount(@PathVariable("id") Long id) {
		users.deleteById(id);
		return "redirect:/settings/users";
	}
}
