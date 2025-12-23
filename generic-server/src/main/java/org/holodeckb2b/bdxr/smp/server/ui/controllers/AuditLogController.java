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

import java.beans.PropertyEditorSupport;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

import org.holodeckb2b.bdxr.smp.server.auditlogging.AuditLogSearchCriteria;
import org.holodeckb2b.bdxr.smp.server.auditlogging.AuditLogService;
import org.holodeckb2b.bdxr.smp.server.db.entities.IDSchemeEntity;
import org.holodeckb2b.bdxr.smp.server.services.core.PersistenceException;
import org.holodeckb2b.bdxr.smp.server.utils.IDSchemeConvertor;
import org.holodeckb2b.commons.util.Utils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;

@Controller
@Slf4j
@RequestMapping("/auditlog")
public class AuditLogController {
	private static final String CRITERIA_ATTR = "criteria";

	@Value("${smp.ui.maxitems_per_page:50}")
	private int maxItemsPerPage;

	@Autowired
	private AuditLogService	auditSvc;

	@InitBinder
	public void registerTimestampConvertor(WebDataBinder binder) {
		binder.registerCustomEditor(Instant.class, new InstantConvertor());
	}
	
	@GetMapping({"","/"})
    public String getLog(@RequestParam(name = "page", required = false) Integer page, Model m, HttpSession s) throws PersistenceException {
		AuditLogSearchCriteria criteria;
		if (page == null) {			
			page = 0;
			criteria = new AuditLogSearchCriteria();
			s.setAttribute(CRITERIA_ATTR, criteria);			
		} else {
			criteria = (AuditLogSearchCriteria) s.getAttribute(CRITERIA_ATTR);
		}		
		m.addAttribute("records", auditSvc.getAuditLogRecords(criteria, PageRequest.of(page, maxItemsPerPage)));
		m.addAttribute(CRITERIA_ATTR, criteria);
		m.addAttribute("users", auditSvc.getAvailableUsernames());
		m.addAttribute("actions", auditSvc.getAvailableActions());
		m.addAttribute("subjects", auditSvc.getAvailableSubjects());
		return "auditlog";
    }
	
	@PostMapping("/search")
	public String searchLog(@ModelAttribute(CRITERIA_ATTR) AuditLogSearchCriteria criteria, Model m, HttpSession s) throws PersistenceException {
		s.setAttribute(CRITERIA_ATTR, criteria);		
		return getLog(0, m, s);
	}
	
	class InstantConvertor extends PropertyEditorSupport {
		
		@Override
		public String getAsText() {
			Instant instant = (Instant) getValue();
			return instant != null ? LocalDateTime.ofInstant(instant, ZoneOffset.UTC).toString() : null;
		}
		
		@Override
		public void setAsText(String text) {
			if (Utils.isNullOrEmpty(text))
				setValue(null);
			else
				setValue(LocalDateTime.parse(text).toInstant(ZoneOffset.UTC));
		}
	}
}
