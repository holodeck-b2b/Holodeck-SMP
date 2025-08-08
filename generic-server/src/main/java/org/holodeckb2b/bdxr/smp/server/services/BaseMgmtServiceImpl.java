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

import java.lang.reflect.ParameterizedType;
import java.time.Instant;
import java.util.Collection;

import org.holodeckb2b.bdxr.smp.server.auditlogging.AuditLogRecord;
import org.holodeckb2b.bdxr.smp.server.auditlogging.AuditLogService;
import org.holodeckb2b.bdxr.smp.server.db.entities.BaseMetadataRegistrationEntity;
import org.holodeckb2b.bdxr.smp.server.db.repos.UniqueIdMDRRepo;
import org.holodeckb2b.bdxr.smp.server.services.core.PersistenceException;
import org.holodeckb2b.commons.util.Utils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.security.core.userdetails.UserDetails;

import lombok.extern.slf4j.Slf4j;

/**
 * Provides the base implementation for the management of <i>MetadataRegistration</i> entities. This class provides 
 * implementations for the CRUD and the basic <code>get</code> operations to retrieve all or a subset of all entities.
 * All operations include exception handling and for the CRUD ones also audit logging.
 * 
 * @author Sander Fieten (sander at holodeck-b2b.org)
 */
@Slf4j
abstract class BaseMgmtServiceImpl<I, E extends BaseMetadataRegistrationEntity<I>, R extends JpaRepository<E, Long>> {

	@Autowired
	protected R		repo;
	
	@Autowired
	protected AuditLogService 		auditSvc;
	
	// Entity class being managed
	private final Class entityCls;
	// Name of the meta-data type being managed
	protected final String mdrName; 
		
	public BaseMgmtServiceImpl() {
		String entityClsName = ((ParameterizedType) this.getClass().getGenericSuperclass())
																.getActualTypeArguments()[1].getTypeName();
		try {
			entityCls = Class.forName(entityClsName);
		} catch (ClassNotFoundException e) {
			log.error("The managed entity class ({}) is not available", entityClsName);
			throw new IllegalStateException("Entity Class not available : " + entityClsName);
		}
		int simpleNameStart = entityClsName.lastIndexOf('.') + 1;
		mdrName = entityClsName.substring(simpleNameStart, entityClsName.indexOf("Entity", simpleNameStart));
	}	

	/**
	 * Gets the details to be logged in the audit log when a meta-data registration is added or updated.
	 * 
	 * @param entity	the entity object representing the registered meta-data 
	 * @return	details to be added in the audit log
	 */
	protected abstract String getAuditDetails(E entity);
	
	/**
	 * Enumerates the CRUD operations that can be executed by this class.
	 */
	protected enum CrudOps {
		Add("add"), Update("updat"), Delete("delet");
		
		private String prefix;
		
		private CrudOps(String p) {
			this.prefix = p;
		}
	}
	
	/**
	 * Checks if the given object is a an instance of the entity class managed by this service implementation.
	 * 
	 * @param o		object to check
	 * @return	the object if it is a managed entity
	 * @throws PersistenceException when the given object is not an instance of a managed entity
	 */
	@SuppressWarnings("unchecked")
	protected E checkManaged(Object o) throws PersistenceException {
		if (!entityCls.equals(o.getClass()) || ((E) o).getOid() == null) {
			log.error("Provided object is not a managed {} entity object", mdrName);
			throw new PersistenceException("Not a managed entity object");		
		} else
			return (E) o;		
	}
	
	/**
	 * Executes the specified CRUD operation on the given entity.
	 * 
	 * @param op	CRUD operation to be performed
	 * @param user	User performing the operation, needed for audit logging and therefore SHALL NOT be <code>null</code>
	 * @param entity entity representing the meta-data to be added, updated or deleted	
	 * @return	entity object representing the meta-data as stored in the database, <code>null</code> when the executed 
	 * 			operation is <code>delete</code>
	 * @throws PersistenceException	when an error occurs executing the requested operation
	 */
	@SuppressWarnings("unchecked")
	protected E executeCRUD(CrudOps op, UserDetails user, E entity) throws PersistenceException {
		E saved = null;
		try {
			log.trace("{}ing {} ({})", op.prefix, mdrName, entity.getId());
			if (op == CrudOps.Delete)
				repo.delete(entity);
			else if (repo instanceof UniqueIdMDRRepo)
				saved = ((UniqueIdMDRRepo<I, E>) repo).save(entity);
			else
				saved = repo.save(entity);
			log.info("{}ed {} ({})", op.prefix, mdrName, entity.getId());
			
			auditSvc.log(new AuditLogRecord(Instant.now(), user.getUsername(), op.name() + " " + mdrName, 
											entity.getAuditLogId(),
											op == CrudOps.Delete ? null : getAuditDetails(entity)));
			return saved;			
		} catch (Throwable t) {
			log.error("An error occurred {}ing the {} ({}) : {}", op.prefix, mdrName, entity.getId(), 
						Utils.getExceptionTrace(t));
			if (t instanceof PersistenceException)
				throw (PersistenceException) t;
			else
				throw new PersistenceException("Failed to " + op.name() + " " + mdrName, t);
		}		
	}	
	
	/**
	 * Retrieves all registered entities. Note that this method returns the <code>Entity</code> version of the stored
	 * meta-data and the implementation class may need to convert these to the <code>interface</code> version.
	 * 
	 * @return	collection with the entity object representing all registered meta-data
	 * @throws PersistenceException when an error occurs retrieving the meta-data
	 */
	protected Collection<E> getAll() throws PersistenceException {
		try {
			log.trace("Retrieving all {} registrations", mdrName);
			return repo.findAll();
		} catch (Throwable t) {
			log.error("An error occurred retrieving all {} : {}", mdrName, Utils.getExceptionTrace(t));
			throw new PersistenceException("Failed to retrieve all " + mdrName, t);
		}
	}
	
	/**
	 * Retrieves the requested subset of registered entities. Note that this method returns the <code>Entity</code> 
	 * version of the stored meta-data and the implementation class may need to convert these to the 
	 * <code>interface</code> version.
	 * 
	 * @param request	a {@link PageRequest} specifying the requested subset
	 * @return	collection with the entity object representing all registered meta-data
	 * @throws PersistenceException when an error occurs retrieving the meta-data
	 */
	protected Page<E> retrieveSubset(PageRequest request) throws PersistenceException {
		try {
			log.trace("Retrieving page {} with {} of {} registrations", request.getPageNumber(), request.getPageSize(),
						mdrName);
			return repo.findAll(request);
		} catch (Throwable t) {
			log.error("An error occurred retrieving all {} : {}", mdrName, Utils.getExceptionTrace(t));
			throw new PersistenceException("Failed to retrieve all " + mdrName, t);
		}
	}
}
