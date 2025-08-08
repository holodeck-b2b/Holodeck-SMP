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
package org.holodeckb2b.bdxr.smp.server.services;

import java.util.Collection;
import java.util.stream.Collectors;

import org.holodeckb2b.bdxr.smp.server.datamodel.Endpoint;
import org.holodeckb2b.bdxr.smp.server.datamodel.Process;
import org.holodeckb2b.bdxr.smp.server.datamodel.ProcessGroup;
import org.holodeckb2b.bdxr.smp.server.datamodel.Service;
import org.holodeckb2b.bdxr.smp.server.datamodel.ServiceMetadataTemplate;
import org.holodeckb2b.bdxr.smp.server.db.entities.EndpointEntity;
import org.holodeckb2b.bdxr.smp.server.db.entities.ProcessEntity;
import org.holodeckb2b.bdxr.smp.server.db.entities.ServiceMetadataTemplateEntity;
import org.holodeckb2b.bdxr.smp.server.db.entities.ServiceEntity;
import org.holodeckb2b.bdxr.smp.server.db.repos.ParticipantRepository;
import org.holodeckb2b.bdxr.smp.server.db.repos.ServiceMetadataTemplateRepository;
import org.holodeckb2b.bdxr.smp.server.db.repos.ServiceRepository;
import org.holodeckb2b.bdxr.smp.server.services.core.ConstraintViolationException;
import org.holodeckb2b.bdxr.smp.server.services.core.ConstraintViolationException.ViolationType;
import org.holodeckb2b.bdxr.smp.server.services.core.PersistenceException;
import org.holodeckb2b.bdxr.smp.server.services.core.SMTMgmtService;
import org.holodeckb2b.bdxr.smp.server.utils.IdUtils;
import org.holodeckb2b.commons.util.Utils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.userdetails.UserDetails;

import lombok.extern.slf4j.Slf4j;

/**
 * Implements the {@link SMTMgmtService}
 * 
 * @author Sander Fieten (sander at holodeck-b2b.org)
 */
@Slf4j
@org.springframework.stereotype.Service
public class SMTMgmtServiceImpl extends BaseMgmtServiceImpl<Long, ServiceMetadataTemplateEntity, ServiceMetadataTemplateRepository> 
										implements SMTMgmtService {

	@Autowired
	protected IdUtils	idUtils;
	
	@Autowired
	protected ParticipantRepository participants;

	@Autowired
	protected ServiceRepository services;
	
	@Override
	protected String getAuditDetails(ServiceMetadataTemplateEntity entity) {
		StringBuilder sb = new StringBuilder("name=").append(entity.getName()).append(",svcID=")
											.append(entity.getService().getId().toString())
											.append(",procGroups=[");
		for (ProcessGroup pg : entity.getProcessMetadata()) {
			sb.append("{[")
			  .append(pg.getProcessInfo().parallelStream()
					  	.map(pi -> pi.getProcessId().toString()).collect(Collectors.joining(",")))
			  .append(']');
			if (!Utils.isNullOrEmpty(pg.getEndpoints()))
				sb.append(",ep=[")
				  .append(pg.getEndpoints().parallelStream()
						  		.map(ep -> "{" + ((Endpoint) ep).getId().toString() 
						  					+ "," + (ep.getEndpointURL() != null ? ep.getEndpointURL().toString() : "") 
						  					+ "," + ep.getTransportProfileId().toString()
						  					+ "}").collect(Collectors.joining(",")))
				  .append("]");
			else if (pg.getRedirection() != null) {
				sb.append(",r=").append(pg.getRedirection().getNewSMPURL().toString());
			}
			sb.append('}');
		}
		sb.append(']');
		return sb.toString();
	}

	@Override
	public ServiceMetadataTemplate addTemplate(UserDetails user, ServiceMetadataTemplate smt)
			throws PersistenceException {
		if (smt.getService() == null) {
			log.warn("New SMT meta-data does not contain Service");
			throw new ConstraintViolationException(ViolationType.MISSING_MANDATORY_FIELD, smt, "Service");
		}
		ServiceMetadataTemplateEntity entity = new ServiceMetadataTemplateEntity();
		try {
			entity.setService(smt.getService());
		} catch (IllegalArgumentException unmanagedSvc) {
			log.warn("New SMT meta-data does contain unmanaged Service instance");
			throw new ConstraintViolationException(ViolationType.INVALID_FIELD, smt, "Service");
		}
		entity.setName(smt.getName());
		try {
			for(ProcessGroup pg : smt.getProcessMetadata()) 
				entity.addProcessGroup(pg);			
		} catch (IllegalArgumentException invalidPg) {
			log.warn("New SMT meta-data does contain invalid ProcessGroup instance : {}", invalidPg.getMessage());
			throw new ConstraintViolationException(ViolationType.INVALID_FIELD, smt, "ProcessGroup");
		}		
		return executeCRUD(CrudOps.Add, user, entity);
	}

	@Override
	public ServiceMetadataTemplate updateTemplate(UserDetails user, ServiceMetadataTemplate smt)
			throws PersistenceException {
		return executeCRUD(CrudOps.Update, user, checkManaged(smt));
	}

	@Override
	public void deleteTemplate(UserDetails user, ServiceMetadataTemplate smt) throws PersistenceException {
		ServiceMetadataTemplateEntity entity = checkManaged(smt);
		log.trace("Check SMT with ID {} isn't bound to any Participant before deletion", smt.getId());
		if (participants.countParticipantsSupporting(entity.getOid()) > 0) {
			log.warn("SMT with ID {} is bound to Participant(s) and can't be deleted", smt.getId());
			throw new PersistenceException("SMT still bound to Participant(s)");
		} else
			executeCRUD(CrudOps.Delete, user, entity);
	}

	@Override
	public Collection<? extends ServiceMetadataTemplate> getTemplates() throws PersistenceException {
		return getAll();
	}
	
	@Override
	public Page<? extends ServiceMetadataTemplate> getTemplates(PageRequest request) throws PersistenceException {
		return retrieveSubset(request);
	}

	@Override
	public Collection<? extends ServiceMetadataTemplate> findTemplatesForService(Service svc) throws PersistenceException {		
		log.trace("Retrieving all SMT for Service with ID {}", svc.getId().toString());
		if (!(svc instanceof ServiceEntity) || ((ServiceEntity) svc).getOid() == null) {
			log.debug("Given Service ({}) is not managed => no SMT to retrieve", svc.getId().toString());
			return null;
		} else
			return repo.findByService((ServiceEntity) svc);
	}
	
	@Override
	public Collection<? extends ServiceMetadataTemplate> findTemplatesForProcess(Process proc) throws PersistenceException {		
		log.trace("Retrieving all SMT for Process with ID {}", proc.getId().toString());
		if (!(proc instanceof ProcessEntity) || ((ProcessEntity) proc).getOid() == null) {
			log.debug("Given Process ({}) is not managed => no SMT to retrieve", proc.getId().toString());
			return null;
		} else
			return repo.findByProcess((ProcessEntity) proc);
	}

	@Override
	public Collection<? extends ServiceMetadataTemplate> findTemplatesUsingEndpoint(Endpoint ep) throws PersistenceException {		
		log.trace("Retrieving all SMT including Endpoint {}-{}", ep.getId(), ep.getName());
		if (!(ep instanceof EndpointEntity) || ((EndpointEntity) ep).getOid() == null) {
			log.debug("Given Endpoint ({}-{}) is not managed => no SMT to retrieve", ep.getId(), ep.getName());
			return null;
		} else
			return repo.findByEndpoint((EndpointEntity) ep);
	}
	
	@Override
	public ServiceMetadataTemplate getTemplate(Long id) throws PersistenceException {
		log.trace("Retrieving SMT with ID {}", id);
		return repo.findById(id).orElse(null);
	}
}
