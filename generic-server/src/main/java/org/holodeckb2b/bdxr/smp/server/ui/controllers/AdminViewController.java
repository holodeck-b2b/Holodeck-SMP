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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import org.holodeckb2b.commons.util.Utils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.info.BuildProperties;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.server.ResponseStatusException;

@Controller
public class AdminViewController {

	@Autowired
	BuildProperties		buildProperties;

	@ModelAttribute("appinfo")
	public BuildProperties appInfo() {
		return buildProperties;
	}

    @GetMapping({"", "/"})
    public String getMainView() {
        return "redirect:/participants";
    }

	@GetMapping("about")
    public String getAboutView() {
        return "admin-ui/about";
    }

	@GetMapping(value = "/favicon.ico", produces = "image/x-icon")	
	@ResponseBody
	public byte[] getFavIcon() throws ResponseStatusException {
		try(InputStream is = new ClassPathResource("/static/img/favicon.ico").getInputStream()) {
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			Utils.copyStream(is, baos);			
			return baos.toByteArray();
		} catch (IOException e) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND);
		}
	}

}
