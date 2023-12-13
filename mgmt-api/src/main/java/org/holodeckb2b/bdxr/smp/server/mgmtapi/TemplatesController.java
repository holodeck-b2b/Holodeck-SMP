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

import java.math.BigInteger;

import org.holodeckb2b.bdxr.smp.server.db.entities.IdentifierE;
import org.holodeckb2b.bdxr.smp.server.db.entities.ProcessGroupE;
import org.holodeckb2b.bdxr.smp.server.db.entities.ServiceMetadataTemplateE;
import org.holodeckb2b.bdxr.smp.server.db.repos.ServiceMetadataTemplateRepository;
import org.holodeckb2b.bdxr.smp.server.mgmtapi.xml.ServiceMetadataTemplate;
import org.holodeckb2b.bdxr.smp.server.mgmtapi.xml.ServiceMetadataTemplates;
import org.holodeckb2b.bdxr.smp.server.queryapi.oasisv2.ServiceMetadataFactory;
import org.holodeckb2b.bdxr.smp.server.svc.IdUtils;
import org.holodeckb2b.commons.util.Utils;
import org.oasis_open.docs.bdxr.ns.smp._2.basiccomponents.IDType;
import org.oasis_open.docs.bdxr.ns.smp._2.extensioncomponents.NameType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
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

	@Value("${smp.smp2_cert_mime-type:application/pkix-cert}")
	protected String certMimeType;
	
	@Autowired
	protected ServiceMetadataTemplateRepository templates;
	
	@Autowired
	protected IdUtils idUtils;

	
	@GetMapping(produces = MediaType.APPLICATION_XML_VALUE)
	public ServiceMetadataTemplates getAll() {
		log.debug("Request for all SMT");
		
		ServiceMetadataTemplates tempList = new ServiceMetadataTemplates();		
		ServiceMetadataFactory factory = new ServiceMetadataFactory(certMimeType);
				
		for(ServiceMetadataTemplateE t : templates.findAll()) {
			ServiceMetadataTemplate tempElem = new ServiceMetadataTemplate();
			tempElem.setTemplateId(BigInteger.valueOf(t.getOid()));
			NameType name = new NameType();
			name.setValue(t.getName());
			tempElem.setName(name);
			IdentifierE svcID = t.getService().getId();
			IDType sidElem = new IDType();
			if (svcID.getScheme() != null)
				sidElem.setSchemeID(svcID.getScheme().getSchemeId());
			sidElem.setValue(svcID.getValue());
			tempElem.setID(sidElem);
			
			for (ProcessGroupE pg : t.getProcessMetadata())
				try {
					tempElem.getProcessMetadatas().add(factory.createProcessMetadata(pg));
				} catch (InstantiationException e) {
					log.error("An error occurred while constructing the ProcessMetadata of SMT ({}-{}) : {}",
							 t.getOid(), t.getName(), Utils.getExceptionTrace(e));
					throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR);
				}
			tempList.getServiceMetadataTemplates().add(tempElem);
		}
		
		return tempList;
	}
}
