/*
 * Copyright (C) 2023 The Holodeck B2B Team
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
package org.holodeckb2b.bdxr.smp.server.mgmtapi;

import org.holodeckb2b.bdxr.smp.server.mgmtapi.xml.ResponseFactory;
import org.holodeckb2b.bdxr.smp.server.mgmtapi.xml.v2025.ServiceMetadataTemplatesElement;
import org.holodeckb2b.bdxr.smp.server.services.core.PersistenceException;
import org.holodeckb2b.bdxr.smp.server.services.core.SMTMgmtService;
import org.holodeckb2b.commons.util.Utils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/templates")
@Slf4j
public class TemplatesController {

	@Autowired
	protected SMTMgmtService  smtSvc;
	
	@GetMapping(produces = MediaType.APPLICATION_XML_VALUE)
	public ServiceMetadataTemplatesElement getAll() {
		log.debug("Request for all SMT");
		
		try {
			return ResponseFactory.createSMTResponse(smtSvc.getTemplates());
		} catch (InstantiationException | PersistenceException e) {
			log.error("Error while creating ServiceMetadataTemplates XML document : {}", Utils.getExceptionTrace(e));
			throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR); 
		}			
	}
	
	
}
