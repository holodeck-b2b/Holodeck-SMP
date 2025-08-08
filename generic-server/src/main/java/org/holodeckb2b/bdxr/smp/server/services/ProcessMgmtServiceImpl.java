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
import java.util.NoSuchElementException;

import org.holodeckb2b.bdxr.smp.server.datamodel.Process;
import org.holodeckb2b.bdxr.smp.server.datamodel.ProcessIdentifier;
import org.holodeckb2b.bdxr.smp.server.db.entities.ProcessEntity;
import org.holodeckb2b.bdxr.smp.server.db.repos.ProcessRepository;
import org.holodeckb2b.bdxr.smp.server.services.core.ConstraintViolationException;
import org.holodeckb2b.bdxr.smp.server.services.core.ConstraintViolationException.ViolationType;
import org.holodeckb2b.bdxr.smp.server.services.core.PersistenceException;
import org.holodeckb2b.bdxr.smp.server.services.core.ProcessMgmtService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;

/**
 * Implements the {@link ProcessMgmtService}.
 * 
 * @author Sander Fieten (sander at holodeck-b2b.org)
 */
@Slf4j
@Service
public class ProcessMgmtServiceImpl 
			extends IdBasedEntityMgmtServiceImpl<ProcessIdentifier, ProcessEntity, ProcessRepository> 
			implements ProcessMgmtService {

	@Override
	protected String getAuditDetails(ProcessEntity entity) {
		return String.format("Name=%s,specRef=%s", entity.getName(), entity.getSpecificationRef());			
	}

	@Override
	public Process addProccess(UserDetails user, Process process) throws PersistenceException {
		try {
			return executeCRUD(CrudOps.Add, user, 
							new ProcessEntity(idUtils.toEmbeddedProcessIdentifier(process.getId()), 
												process.getName(), process.getSpecificationRef()));
		} catch (NoSuchElementException unknownScheme) {
			log.error("Unknown ID scheme {} used in Identifier", process.getId().getScheme().getSchemeId());
			throw new ConstraintViolationException(ViolationType.INVALID_FIELD, process, "Identifier.IDScheme");
		}
	}

	@Override
	public Process updateProcess(UserDetails user, Process process) throws PersistenceException {
		return executeCRUD(CrudOps.Update, user, checkManaged(process));
	}

	@Override
	public void deleteProcess(UserDetails user, Process process) throws PersistenceException {
		executeCRUD(CrudOps.Delete, user, checkManaged(process));
	}

	@Override
	public Collection<? extends Process> getProcesses() throws PersistenceException {
		return getAll();
	}
	
	@Override
	public Page<ProcessEntity> getProcesses(PageRequest request) throws PersistenceException {
		return retrieveSubset(request);
	}

	@Override
	public Process getProcess(org.holodeckb2b.bdxr.smp.datamodel.ProcessIdentifier processId) throws PersistenceException {		
		try {
			return getById(idUtils.toEmbeddedProcessIdentifier(processId));
		} catch (NoSuchElementException unknownScheme) {
			return null;
		}
	}
}
