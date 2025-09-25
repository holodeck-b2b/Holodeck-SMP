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
package org.holodeckb2b.bdxr.smp.server.ui.controllers;

import org.holodeckb2b.bdxr.smp.server.services.core.SMPServerAdminService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Is a Spring {@link ControllerAdvice} that ensures that the request URI is always available when processing the 
 * Thymeleaf templates. 
 * 
 * @author Sander Fieten (sander at holodeck-b2b.org)
 */
@ControllerAdvice
public class StandardAttributesProvider {

	@Autowired
	protected SMPServerAdminService configSvc;
	
	@ModelAttribute("requestURI")
    String getRequestRequestURI(HttpServletRequest request) {
        return request.getRequestURI();
    }
	
}
